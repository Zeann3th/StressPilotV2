package dev.zeann3th.stresspilot.controller;

import dev.zeann3th.stresspilot.dto.endpoint.EndpointDTO;
import dev.zeann3th.stresspilot.dto.endpoint.ExecuteEndpointResponseDTO;
import dev.zeann3th.stresspilot.service.endpoint.EndpointService;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/endpoints")
@SuppressWarnings("unused")
@RequiredArgsConstructor
public class EndpointController {
    private final EndpointService endpointService;

    @GetMapping
    public ResponseEntity<Page<EndpointDTO>> getListEndpoint(
            @RequestParam(value = "projectId", required = false) Long projectId,
            @RequestParam(value = "name", required = false) String name,
            @ParameterObject @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        var resp = endpointService.getListEndpoint(projectId, name, pageable);
        return ResponseEntity.ok(resp);
    }

    @GetMapping("/{endpointId}")
    public ResponseEntity<EndpointDTO> getEndpointDetail(@PathVariable("endpointId") Long endpointId) {
        var resp = endpointService.getEndpointDetail(endpointId);
        return ResponseEntity.ok(resp);
    }

    @PatchMapping("/{endpointId}")
    public ResponseEntity<EndpointDTO> updateEndpoint(
            @PathVariable("endpointId") Long endpointId,
            @RequestBody Map<String, Object> endpointDTO
    ) {
        var resp = endpointService.updateEndpoint(endpointId, endpointDTO);
        return ResponseEntity.ok(resp);
    }

    @DeleteMapping("/{endpointId}")
    public ResponseEntity<Void> deleteEndpoint(@PathVariable("endpointId") Long endpointId) {
        endpointService.deleteEndpoint(endpointId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping(value = "/upload", consumes = "multipart/form-data")
    public ResponseEntity<Void> uploadEndpoints(
            @RequestParam("file") MultipartFile file,
            @RequestParam("projectId") Long projectId) {
        endpointService.uploadEndpoints(file, projectId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{endpointId}/execute")
    public ResponseEntity<ExecuteEndpointResponseDTO> executeEndpoint(
            @PathVariable("endpointId") Long endpointId,
            @RequestBody Map<String, Object> requestBody
    ) {
        var resp = endpointService.runEndpoint(endpointId, requestBody);
        return ResponseEntity.ok(resp);
    }
}
