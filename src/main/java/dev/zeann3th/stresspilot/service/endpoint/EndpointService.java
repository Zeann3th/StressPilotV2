package dev.zeann3th.stresspilot.service.endpoint;

import dev.zeann3th.stresspilot.dto.endpoint.EndpointDTO;
import dev.zeann3th.stresspilot.dto.endpoint.ExecuteEndpointResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

public interface EndpointService {
    Page<EndpointDTO> getListEndpoint(Long projectId, String name, Pageable pageable);

    EndpointDTO getEndpointDetail(Long endpointId);

    EndpointDTO updateEndpoint(Long endpointId, Map<String, Object> endpointUpdateRequest);

    void deleteEndpoint(Long endpointId);

    void uploadEndpoints(MultipartFile file, Long projectId);

    ExecuteEndpointResponseDTO runEndpoint(Long endpointId, Map<String, Object> variables);
}
