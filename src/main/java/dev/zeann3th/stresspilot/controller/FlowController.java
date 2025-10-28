package dev.zeann3th.stresspilot.controller;

import dev.zeann3th.stresspilot.dto.flow.CreateFlowRequestDTO;
import dev.zeann3th.stresspilot.dto.flow.FlowResponseDTO;
import dev.zeann3th.stresspilot.dto.flow.FlowStepDTO;
import dev.zeann3th.stresspilot.service.FlowService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/flows")
@SuppressWarnings("unused")
@RequiredArgsConstructor
public class FlowController {
    private final FlowService flowService;

    @GetMapping
    public ResponseEntity<Page<FlowResponseDTO>> getListFlow(
            @RequestParam(value = "projectId", required = false) Long projectId,
            @RequestParam(value = "name", required = false) String name,
            @ParameterObject @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        var resp = flowService.getListFlow(projectId, name, pageable);
        return ResponseEntity.ok(resp);
    }

    @GetMapping("/{flowId}")
    public ResponseEntity<FlowResponseDTO> getFlowDetail(@PathVariable("flowId") Long flowId) {
        var resp = flowService.getFlowDetail(flowId);
        return ResponseEntity.ok(resp);
    }

    @PostMapping
    public ResponseEntity<FlowResponseDTO> createFlow(@Valid @RequestBody CreateFlowRequestDTO flowDTO) {
        var resp = flowService.createFlow(flowDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(resp);
    }

    @PostMapping("/{flowId}/configuration")
    public ResponseEntity<List<FlowStepDTO>> configureFlow(@PathVariable("flowId") Long flowId, @RequestBody List<FlowStepDTO> steps) {
        var resp = flowService.configureFlow(flowId, steps);
        return ResponseEntity.ok(resp);
    }

    @DeleteMapping("/{flowId}")
    public ResponseEntity<Void> deleteFlow(@PathVariable("flowId") Long flowId) {
        flowService.deleteFlow(flowId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{flowId}")
    public ResponseEntity<FlowResponseDTO> updateFlow(@PathVariable("flowId") Long flowId, @RequestBody Map<String, Object> flowDTO) {
        var resp = flowService.updateFlow(flowId, flowDTO);
        return ResponseEntity.ok(resp);
    }
}
