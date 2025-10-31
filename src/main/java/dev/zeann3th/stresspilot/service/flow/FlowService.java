package dev.zeann3th.stresspilot.service.flow;

import dev.zeann3th.stresspilot.dto.flow.CreateFlowRequestDTO;
import dev.zeann3th.stresspilot.dto.flow.FlowResponseDTO;
import dev.zeann3th.stresspilot.dto.flow.FlowStepDTO;
import dev.zeann3th.stresspilot.dto.flow.RunFlowRequestDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;

public interface FlowService {
    Page<FlowResponseDTO> getListFlow(Long projectId, String name, Pageable pageable);

    FlowResponseDTO getFlowDetail(Long flowId);

    FlowResponseDTO createFlow(CreateFlowRequestDTO flowDTO);

    void deleteFlow(Long flowId);

    FlowResponseDTO updateFlow(Long flowId,  Map<String, Object> flowDTO);

    List<FlowStepDTO> configureFlow(Long flowId, List<FlowStepDTO> steps);

    void runFlow(Long flowId, RunFlowRequestDTO runFlowRequestDTO);
}
