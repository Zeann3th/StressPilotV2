package dev.zeann3th.stresspilot.service.impl;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.zeann3th.stresspilot.common.Constants;
import dev.zeann3th.stresspilot.common.enums.ErrorCode;
import dev.zeann3th.stresspilot.common.mappers.FlowMapper;
import dev.zeann3th.stresspilot.dto.flow.FlowCreateRequest;
import dev.zeann3th.stresspilot.dto.flow.FlowResponseDTO;
import dev.zeann3th.stresspilot.entity.FlowEntity;
import dev.zeann3th.stresspilot.exception.CommandExceptionBuilder;
import dev.zeann3th.stresspilot.repository.FlowRepository;
import dev.zeann3th.stresspilot.service.FlowService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FlowServiceImpl implements FlowService {
    private final FlowRepository flowRepository;
    private final ObjectMapper objectMapper;
    private final FlowMapper flowMapper;

    @Override
    public ResponseEntity<Page<FlowResponseDTO>> getListFlow(Long projectId, String name, Pageable pageable) {
        Page<FlowEntity> flowPage = flowRepository.findAllByCondition(projectId, name, pageable);
        return ResponseEntity.ok(flowPage.map(flowMapper::toListDTO));
    }

    @Override
    public ResponseEntity<FlowResponseDTO> getFlowDetail(Long flowId) {
        FlowEntity flowEntity = flowRepository.findById(flowId)
                .orElseThrow(() -> CommandExceptionBuilder.exception(ErrorCode.FLOW_NOT_FOUND));
        return ResponseEntity.ok(flowMapper.toDetailDTO(flowEntity));
    }

    @Override
    public ResponseEntity<FlowResponseDTO> createFlow(FlowCreateRequest flowDTO) {
        FlowEntity flowEntity = FlowEntity.builder()
                .projectId(flowDTO.getProjectId())
                .name(flowDTO.getName())
                .description(flowDTO.getDescription())
                .build();
        FlowEntity saved = flowRepository.save(flowEntity);
        return ResponseEntity.status(HttpStatus.CREATED).body(flowMapper.toDetailDTO(saved));
    }

    @Override
    public ResponseEntity<Void> deleteFlow(Long flowId) {
        boolean exists = flowRepository.existsById(flowId);
        if (!exists) {
            throw CommandExceptionBuilder.exception(ErrorCode.FLOW_NOT_FOUND);
        }
        flowRepository.deleteById(flowId);
        return ResponseEntity.noContent().build();
    }

    @Override
    @Transactional
    public ResponseEntity<FlowResponseDTO> updateFlow(Long flowId, Map<String, Object> flowDTO) {
        FlowEntity flowEntity = flowRepository.findById(flowId)
                .orElseThrow(() -> CommandExceptionBuilder.exception(ErrorCode.FLOW_NOT_FOUND));

        Set<String> forbiddenFields = Set.of("id", "projectId");
        Map<String, Object> sanitized = flowDTO.entrySet().stream()
                .filter(entry -> !forbiddenFields.contains(entry.getKey()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        try {
            objectMapper.updateValue(flowEntity, sanitized);
            FlowEntity updated = flowRepository.save(flowEntity);
            return ResponseEntity.ok(flowMapper.toDetailDTO(updated));
        } catch (JsonMappingException e) {
            throw CommandExceptionBuilder.exception(ErrorCode.BAD_REQUEST, Map.of(Constants.REASON, "Invalid data format"));
        }
    }
}
