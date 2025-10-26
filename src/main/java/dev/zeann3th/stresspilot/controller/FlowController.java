package dev.zeann3th.stresspilot.controller;

import dev.zeann3th.stresspilot.dto.flow.FlowCreateRequest;
import dev.zeann3th.stresspilot.dto.flow.FlowResponseDTO;
import dev.zeann3th.stresspilot.service.FlowService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/flows")
@RequiredArgsConstructor
public class FlowController {
    private final FlowService flowService;

    @GetMapping
    public ResponseEntity<Page<FlowResponseDTO>> getListFlow(
            @RequestParam(value = "projectId", required = false) Long projectId,
            @RequestParam(value = "name", required = false) String name,
            @ParameterObject @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return flowService.getListFlow(projectId, name, pageable);
    }

    @GetMapping("/{flowId}")
    public ResponseEntity<FlowResponseDTO> getFlowDetail(@PathVariable("flowId") Long flowId) {
        return flowService.getFlowDetail(flowId);
    }

    @PostMapping
    public ResponseEntity<FlowResponseDTO> createFlow(@Valid @RequestBody FlowCreateRequest flowDTO) {
        return flowService.createFlow(flowDTO);
    }

    @DeleteMapping("/{flowId}")
    public ResponseEntity<Void> deleteFlow(@PathVariable("flowId") Long flowId) {
        return flowService.deleteFlow(flowId);
    }

    @PatchMapping("/{flowId}")
    public ResponseEntity<FlowResponseDTO> updateFlow(@PathVariable("flowId") Long flowId, @RequestBody Map<String, Object> flowDTO) {
        return flowService.updateFlow(flowId, flowDTO);
    }
}
