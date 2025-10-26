package dev.zeann3th.stresspilot.service;

import dev.zeann3th.stresspilot.dto.endpoint.EndpointDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

public interface EndpointService {
    ResponseEntity<Page<EndpointDTO>> getListEndpoint(Long projectId, String name, Pageable pageable);

    ResponseEntity<EndpointDTO> getEndpointDetail(Long endpointId);

    ResponseEntity<EndpointDTO> updateEndpoint(Long endpointId, Map<String, Object> endpointUpdateRequest);

    ResponseEntity<Void> deleteEndpoint(Long endpointId);

    ResponseEntity<Void> uploadEndpoints(MultipartFile file, Long projectId);
}
