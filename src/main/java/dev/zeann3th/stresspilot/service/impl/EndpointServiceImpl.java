package dev.zeann3th.stresspilot.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.zeann3th.stresspilot.common.Constants;
import dev.zeann3th.stresspilot.common.enums.ErrorCode;
import dev.zeann3th.stresspilot.common.mappers.EndpointMapper;
import dev.zeann3th.stresspilot.dto.endpoint.EndpointDTO;
import dev.zeann3th.stresspilot.dto.endpoint.ParsedEndpointDTO;
import dev.zeann3th.stresspilot.entity.EndpointEntity;
import dev.zeann3th.stresspilot.exception.CommandExceptionBuilder;
import dev.zeann3th.stresspilot.repository.EndpointRepository;
import dev.zeann3th.stresspilot.service.EndpointService;
import dev.zeann3th.stresspilot.service.parser.ParserService;
import dev.zeann3th.stresspilot.service.parser.ParserServiceFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EndpointServiceImpl implements EndpointService {
    private final EndpointRepository endpointRepository;
    private final EndpointMapper endpointMapper;
    private final ObjectMapper objectMapper;
    private final ParserServiceFactory parserServiceFactory;

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
        if ("application/json".equals(contentType)) {
            parser = parserServiceFactory.getParser("postman");
        } else {
            throw CommandExceptionBuilder.exception(ErrorCode.BAD_REQUEST,
                    Map.of(Constants.REASON, "Unsupported file type: " + contentType));
        }

        try {
            String fileContent = new String(file.getBytes(), StandardCharsets.UTF_8);
            List<ParsedEndpointDTO> parsedEndpoints = parser.parse(fileContent);

            List<EndpointEntity> entities = parsedEndpoints.stream().map(pe -> {
                try {
                    return EndpointEntity.builder()
                            .name(pe.getName())
                            .description(pe.getDescription())
                            .method(pe.getMethod())
                            .url(pe.getUrl())
                            .headers(pe.getHeaders() != null ? objectMapper.writeValueAsString(pe.getHeaders()) : null)
                            .body(pe.getBody() != null ? objectMapper.writeValueAsString(pe.getBody()) : null)
                            .parameters(pe.getParameters() != null ? objectMapper.writeValueAsString(pe.getParameters()) : null)
                            .projectId(projectId)
                            .build();
                } catch (JsonProcessingException e) {
                    throw CommandExceptionBuilder.exception(ErrorCode.BAD_REQUEST,
                            Map.of(Constants.REASON, "Failed to serialize endpoint data"));
                }
            }).toList();

            endpointRepository.saveAll(entities);
        } catch (Exception e) {
            throw CommandExceptionBuilder.exception(ErrorCode.BAD_REQUEST,
                    Map.of(Constants.REASON, "Failed to parse or save endpoints: " + e.getMessage()));
        }
    }
}
