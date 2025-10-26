package dev.zeann3th.stresspilot.service;

import dev.zeann3th.stresspilot.dto.flow.FlowCreateRequest;
import dev.zeann3th.stresspilot.dto.flow.FlowResponseDTO;
import dev.zeann3th.stresspilot.dto.flow.FlowStepDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;

public interface FlowService {
    ResponseEntity<Page<FlowResponseDTO>> getListFlow(Long projectId, String name, Pageable pageable);

    ResponseEntity<FlowResponseDTO> getFlowDetail(Long flowId);

    ResponseEntity<FlowResponseDTO> createFlow(FlowCreateRequest flowDTO);

    ResponseEntity<Void> deleteFlow(Long flowId);

    ResponseEntity<FlowResponseDTO> updateFlow(Long flowId,  Map<String, Object> flowDTO);

    ResponseEntity<List<FlowStepDTO>> configureFlow(Long flowId, List<FlowStepDTO> steps);
}
