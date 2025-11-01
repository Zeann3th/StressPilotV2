package dev.zeann3th.stresspilot.service.endpoint.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.zeann3th.stresspilot.common.Constants;
import dev.zeann3th.stresspilot.common.enums.ErrorCode;
import dev.zeann3th.stresspilot.common.mappers.EndpointMapper;
import dev.zeann3th.stresspilot.dto.endpoint.EndpointDTO;
import dev.zeann3th.stresspilot.dto.endpoint.ExecuteEndpointResponseDTO;
import dev.zeann3th.stresspilot.dto.endpoint.ParsedEndpointDTO;
import dev.zeann3th.stresspilot.entity.EndpointEntity;
import dev.zeann3th.stresspilot.entity.EnvironmentVariableEntity;
import dev.zeann3th.stresspilot.entity.ProjectEntity;
import dev.zeann3th.stresspilot.exception.CommandExceptionBuilder;
import dev.zeann3th.stresspilot.repository.EndpointRepository;
import dev.zeann3th.stresspilot.repository.EnvironmentVariableRepository;
import dev.zeann3th.stresspilot.repository.ProjectRepository;
import dev.zeann3th.stresspilot.service.endpoint.EndpointService;
import dev.zeann3th.stresspilot.service.executor.EndpointExecutorService;
import dev.zeann3th.stresspilot.service.executor.EndpointExecutorServiceFactory;
import dev.zeann3th.stresspilot.service.parser.ParserService;
import dev.zeann3th.stresspilot.service.parser.ParserServiceFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class EndpointServiceImpl implements EndpointService {
    private final EndpointRepository endpointRepository;
    private final ProjectRepository projectRepository;
    private final EnvironmentVariableRepository envVarRepo;
    private final EndpointMapper endpointMapper;
    private final ObjectMapper objectMapper;
    private final ParserServiceFactory parserServiceFactory;
    private final EndpointExecutorServiceFactory endpointExecutorServiceFactory;

    @Override
    public Page<EndpointDTO> getListEndpoint(Long projectId, String name, Pageable pageable) {
        Page<EndpointEntity> entityPage = endpointRepository.findAllByCondition(projectId, name, pageable);
        return entityPage.map(endpointMapper::toListDTO);
    }

    @Override
    public EndpointDTO getEndpointDetail(Long endpointId) {
        EndpointEntity endpointEntity = endpointRepository.findById(endpointId)
                .orElseThrow(() -> CommandExceptionBuilder.exception(ErrorCode.ENDPOINT_NOT_FOUND));
        return endpointMapper.toDetailDTO(endpointEntity);
    }

    @Override
    public EndpointDTO updateEndpoint(Long endpointId, Map<String, Object> endpointUpdateRequest) {
        EndpointEntity endpointEntity = endpointRepository.findById(endpointId)
                .orElseThrow(() -> CommandExceptionBuilder.exception(ErrorCode.ENDPOINT_NOT_FOUND));

        Set<String> forbiddenFields = Set.of("id", "projectId");
        Map<String, Object> sanitized = endpointUpdateRequest.entrySet().stream()
                .filter(entry -> !forbiddenFields.contains(entry.getKey()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        try {
            EndpointEntity updatedEntity = objectMapper.updateValue(endpointEntity, sanitized);
            EndpointEntity savedEntity = endpointRepository.save(updatedEntity);
            return endpointMapper.toDetailDTO(savedEntity);
        } catch (JsonMappingException e) {
            throw CommandExceptionBuilder.exception(ErrorCode.BAD_REQUEST, Map.of(Constants.REASON, "Invalid request data"));
        }
    }

    @Override
    public void deleteEndpoint(Long endpointId) {
        boolean exists = endpointRepository.existsById(endpointId);
        if (!exists) {
            throw CommandExceptionBuilder.exception(ErrorCode.ENDPOINT_NOT_FOUND);
        }
        endpointRepository.deleteById(endpointId);
    }

    @Override
    public void uploadEndpoints(MultipartFile file, Long projectId) {
        if (file == null || file.isEmpty()) {
            throw CommandExceptionBuilder.exception(ErrorCode.BAD_REQUEST,
                    Map.of(Constants.REASON, "File is empty or null"));
        }

        String contentType = file.getContentType();
        if (contentType == null) {
            throw CommandExceptionBuilder.exception(ErrorCode.BAD_REQUEST,
                    Map.of(Constants.REASON, "File content type is missing"));
        }

        ParserService parser;
        String type;
        if ("application/json".equals(contentType) || Objects.requireNonNull(file.getOriginalFilename()).endsWith(".json")) {
            type = "postman";
        } else if (Objects.requireNonNull(file.getOriginalFilename()).endsWith(".proto")
                || Objects.requireNonNull(file.getOriginalFilename()).endsWith(".pb")) {
            type = "proto";
        } else {
            throw CommandExceptionBuilder.exception(ErrorCode.BAD_REQUEST,
                    Map.of(Constants.REASON, "Unsupported file type: " + contentType));
        }

        parser = parserServiceFactory.getParser(type);

        try {
            String fileContent = new String(file.getBytes(), StandardCharsets.UTF_8);
            List<ParsedEndpointDTO> parsedEndpoints = parser.parse(fileContent);

            List<EndpointEntity> entities = parsedEndpoints.stream()
                    .map(parsedEndpointDTO -> buildEndpoint(projectId, parsedEndpointDTO))
                    .toList();

            endpointRepository.saveAll(entities);
        } catch (Exception e) {
            throw CommandExceptionBuilder.exception(ErrorCode.BAD_REQUEST,
                    Map.of(Constants.REASON, "Failed to parse or save endpoints: " + e.getMessage()));
        }
    }

    private EndpointEntity buildEndpoint(Long projectId, ParsedEndpointDTO parsedEndpointDTO) {
        try {
            return EndpointEntity.builder()
                    .name(parsedEndpointDTO.getName())
                    .description(parsedEndpointDTO.getDescription())
                    .type(parsedEndpointDTO.getType())
                    // HTTP
                    .httpMethod(parsedEndpointDTO.getHttpMethod())
                    .url(parsedEndpointDTO.getUrl())
                    .httpHeaders(parsedEndpointDTO.getHttpHeaders() != null ? objectMapper.writeValueAsString(parsedEndpointDTO.getHttpHeaders()) : null)
                    .httpBody(parsedEndpointDTO.getHttpBody() != null ? objectMapper.writeValueAsString(parsedEndpointDTO.getHttpBody()) : null)
                    .httpParameters(parsedEndpointDTO.getHttpParameters() != null ? objectMapper.writeValueAsString(parsedEndpointDTO.getHttpParameters()) : null)
                    // gRPC
                    .grpcServiceName(parsedEndpointDTO.getGrpcServiceName())
                    .grpcMethodName(parsedEndpointDTO.getGrpcMethodName())
                    .grpcStubPath(parsedEndpointDTO.getGrpcStubPath())
                    // GraphQL
                    .graphqlOperationType(parsedEndpointDTO.getGraphqlOperationType())
                    .graphqlVariables(parsedEndpointDTO.getGraphqlVariables() != null ? objectMapper.writeValueAsString(parsedEndpointDTO.getGraphqlVariables()) : null)
                    .projectId(projectId)
                    .build();
        } catch (JsonProcessingException e) {
            throw CommandExceptionBuilder.exception(ErrorCode.BAD_REQUEST,
                    Map.of(Constants.REASON, "Failed to serialize endpoint data"));
        }
    }

    @Override
    public ExecuteEndpointResponseDTO runEndpoint(Long endpointId, Map<String, Object> variables) {
        EndpointEntity endpointEntity = endpointRepository.findById(endpointId)
                .orElseThrow(() -> CommandExceptionBuilder.exception(ErrorCode.ENDPOINT_NOT_FOUND));

        ProjectEntity projectEntity = projectRepository.findById(endpointEntity.getProjectId())
                .orElseThrow(() -> CommandExceptionBuilder.exception(ErrorCode.PROJECT_NOT_FOUND));

        Map<String, Object> environment = envVarRepo
                .findAllByEnvironmentIdAndIsActiveTrue(projectEntity.getEnvironmentId())
                .stream()
                .collect(Collectors.toMap(
                        EnvironmentVariableEntity::getKey,
                        EnvironmentVariableEntity::getValue,
                        (v1, v2) -> v2
                ));

        if (variables != null && !variables.isEmpty()) {
            environment.putAll(variables);
        }

        EndpointExecutorService endpointExecutorService = endpointExecutorServiceFactory.getExecutor(endpointEntity.getType());

        long startTime = System.currentTimeMillis();
        try {
            return endpointExecutorService.execute(endpointEntity, environment, null);
        } catch (Exception e) {
            log.error("Error executing endpoint {}: {}", endpointId, e.getMessage(), e);
            Map<String, Object> data = Map.of("error", e.getMessage());
            return ExecuteEndpointResponseDTO.builder()
                    .responseTimeMs(System.currentTimeMillis() - startTime)
                    .success(false)
                    .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
                    .data(data)
                    .rawResponse(data.toString())
                    .build();
        }
    }
}
