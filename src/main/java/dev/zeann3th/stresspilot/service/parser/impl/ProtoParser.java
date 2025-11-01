package dev.zeann3th.stresspilot.service.parser.impl;

import com.google.protobuf.DescriptorProtos;
import com.google.protobuf.Descriptors;
import dev.zeann3th.stresspilot.common.Constants;
import dev.zeann3th.stresspilot.common.enums.ConfigKey;
import dev.zeann3th.stresspilot.common.enums.EndpointType;
import dev.zeann3th.stresspilot.common.enums.ErrorCode;
import dev.zeann3th.stresspilot.dto.endpoint.ParsedEndpointDTO;
import dev.zeann3th.stresspilot.exception.CommandExceptionBuilder;
import dev.zeann3th.stresspilot.service.ConfigService;
import dev.zeann3th.stresspilot.service.parser.ParserService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.*;
import java.util.*;

@Slf4j(topic = "GRPC_PARSER")
@Component
@RequiredArgsConstructor
@SuppressWarnings("unused")
public class ProtoParser implements ParserService {

    private static final String PROTOC = "protoc";
    private String protocPluginPath;

    private final ConfigService configService;

    @PostConstruct
    public void init() {
        protocPluginPath = configService.getValue(ConfigKey.GRPC_PROTOC_PLUGIN_PATH.name())
                .filter(s -> !s.isBlank())
                .orElse(null);
    }

    @Override
    public String getType() {
        return "proto";
    }

    @Override
    public List<ParsedEndpointDTO> parse(String spec) {
        try {
            Path baseDir = getAppBaseDir();
            Path protoDir = baseDir.resolve(Paths.get("core", "grpc", "proto"));
            Files.createDirectories(protoDir);

            String filename = "service_" + System.currentTimeMillis() + ".proto";
            Path protoFile = protoDir.resolve(filename);
            Files.writeString(protoFile, spec);
            log.info("Saved proto file to: {}", protoFile);

            Path descriptorFile = protoDir.resolve(filename.replace(".proto", ".pb"));
            generateDescriptor(protoFile, descriptorFile);

            List<ParsedEndpointDTO> endpoints = parseDescriptor(descriptorFile, protoFile.toAbsolutePath().toString());

            if (protocPluginPath != null) {
                Path stubPath = generateGrpcStub(protoFile);

                String packageName = null;
                String protoContent = Files.readString(protoFile);
                for (String line : protoContent.split("\\R")) {
                    line = line.trim();
                    if (line.startsWith("package ")) {
                        packageName = line.replace("package ", "").replace(";", "").trim();
                        break;
                    }
                }

                Path fullStubPath = (packageName != null)
                        ? stubPath.resolve(packageName.replace(".", File.separator))
                        : stubPath;

                for (ParsedEndpointDTO endpoint : endpoints) {
                    endpoint.setGrpcStubPath(fullStubPath.toAbsolutePath().toString());
                }
            }

            return endpoints;

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Proto parsing interrupted", e);
            throw CommandExceptionBuilder.exception(ErrorCode.ENDPOINT_PARSE_ERROR, Map.of(
                    Constants.REASON, "Parsing interrupted"
            ));
        } catch (IOException e) {
            log.error("I/O error during proto parsing", e);
            throw CommandExceptionBuilder.exception(ErrorCode.ENDPOINT_PARSE_ERROR, Map.of(
                    Constants.REASON, e.getMessage()
            ));
        } catch (Exception e) {
            log.error("Failed to parse gRPC proto", e);
            throw CommandExceptionBuilder.exception(ErrorCode.ENDPOINT_PARSE_ERROR, Map.of(
                    Constants.REASON, e.getMessage()
            ));
        }
    }

    /** Resolve application base directory */
    private Path getAppBaseDir() {
        String appDir = System.getProperty(Constants.PILOT_HOME);
        if (appDir != null && !appDir.isBlank()) {
            return Paths.get(appDir);
        }
        return Paths.get(System.getProperty(Constants.USER_HOME), Constants.APP_DIR);
    }

    private void generateDescriptor(Path protoFile, Path descriptorFile) throws IOException, InterruptedException {
        Path protoDir = protoFile.getParent();
        List<String> command = List.of(
                PROTOC,
                "--proto_path=" + protoDir.toAbsolutePath(),
                "--descriptor_set_out=" + descriptorFile.toAbsolutePath(),
                "--include_imports",
                protoFile.getFileName().toString()
        );

        runCommand(command, protoDir, "[protoc-descriptor]");
    }

    private Path generateGrpcStub(Path protoFile) throws IOException, InterruptedException {
        Path outputDir = Paths.get(System.getProperty(Constants.USER_HOME), Constants.APP_DIR, "core", "grpc", "stubs");
        Files.createDirectories(outputDir);

        Path protoDir = protoFile.getParent();
        List<String> command = new ArrayList<>();
        command.add(PROTOC);
        command.add("--proto_path=" + protoDir.toAbsolutePath());
        command.add("--java_out=" + outputDir.toAbsolutePath());
        command.add("--grpc-java_out=" + outputDir.toAbsolutePath());
        command.add("--plugin=protoc-gen-grpc-java=" + protocPluginPath);
        command.add(protoFile.getFileName().toString());

        runCommand(command, protoDir, "[protoc-stub]");
        log.info("Generated gRPC stubs in {}", outputDir);
        return outputDir;
    }

    private List<ParsedEndpointDTO> parseDescriptor(Path descriptorFile, String protoFilePath) throws Exception {
        byte[] descriptorBytes = Files.readAllBytes(descriptorFile);
        DescriptorProtos.FileDescriptorSet descriptorSet = DescriptorProtos.FileDescriptorSet.parseFrom(descriptorBytes);

        List<ParsedEndpointDTO> endpoints = new ArrayList<>();
        for (DescriptorProtos.FileDescriptorProto fileProto : descriptorSet.getFileList()) {
            Descriptors.FileDescriptor fileDescriptor =
                    Descriptors.FileDescriptor.buildFrom(fileProto, new Descriptors.FileDescriptor[]{});

            for (Descriptors.ServiceDescriptor service : fileDescriptor.getServices()) {
                for (Descriptors.MethodDescriptor method : service.getMethods()) {
                    ParsedEndpointDTO dto = ParsedEndpointDTO.builder()
                            .name(method.getName())
                            .type(EndpointType.GRPC.name())
                            .url("{{url}}")
                            .grpcServiceName(service.getName())
                            .grpcMethodName(method.getName())
                            .grpcStubPath(protoFilePath)
                            .build();

                    endpoints.add(dto);
                    log.info("Found gRPC method: {}/{}", service.getFullName(), method.getName());
                }
            }
        }
        return endpoints;
    }

    private void runCommand(List<String> command, Path workingDir, String prefix) throws IOException, InterruptedException {
        log.info("{} Running command: {}", prefix, String.join(" ", command));
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.directory(workingDir.toFile());
        pb.redirectErrorStream(true);

        Process process = pb.start();

        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            reader.lines().forEach(line -> {
                log.info("{} {}", prefix, line);
                output.append(line).append("\n");
            });
        }

        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new IOException(prefix + " Command failed with exit code " + exitCode + ". Output:\n" + output);
        }
    }
}