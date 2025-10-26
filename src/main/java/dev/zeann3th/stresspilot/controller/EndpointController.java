package dev.zeann3th.stresspilot.controller;

import dev.zeann3th.stresspilot.dto.endpoint.EndpointDTO;
import dev.zeann3th.stresspilot.service.EndpointService;
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
@RequiredArgsConstructor
public class EndpointController {
    private final EndpointService endpointService;

    @GetMapping
    public ResponseEntity<Page<EndpointDTO>> getListEndpoint(
            @RequestParam(value = "projectId", required = false) Long projectId,
            @RequestParam(value = "name", required = false) String name,
            @ParameterObject @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return endpointService.getListEndpoint(projectId, name, pageable);
    }

    @GetMapping("/{endpointId}")
    public ResponseEntity<EndpointDTO> getEndpointDetail(@PathVariable("endpointId") Long endpointId) {
        return endpointService.getEndpointDetail(endpointId);
    }

    @PatchMapping("/{endpointId}")
    public ResponseEntity<EndpointDTO> updateEndpoint(
            @PathVariable("endpointId") Long endpointId,
            @RequestBody Map<String, Object> endpointDTO
    ) {
        return endpointService.updateEndpoint(endpointId, endpointDTO);
    }

    @DeleteMapping("/{endpointId}")
    public ResponseEntity<Void> deleteEndpoint(@PathVariable("endpointId") Long endpointId) {
        return endpointService.deleteEndpoint(endpointId);
    }

    @PostMapping(value = "/upload", consumes = "multipart/form-data")
    public ResponseEntity<Void> uploadEndpoints(
            @RequestParam("file") MultipartFile file,
            @RequestParam("projectId") Long projectId) {
        return endpointService.uploadEndpoints(file, projectId);
    }
}
