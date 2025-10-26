package dev.zeann3th.stresspilot.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.zeann3th.stresspilot.common.Constants;
import dev.zeann3th.stresspilot.common.enums.ErrorCode;
import dev.zeann3th.stresspilot.common.enums.FlowStepType;
import dev.zeann3th.stresspilot.common.mappers.FlowMapper;
import dev.zeann3th.stresspilot.dto.flow.FlowCreateRequest;
import dev.zeann3th.stresspilot.dto.flow.FlowResponseDTO;
import dev.zeann3th.stresspilot.dto.flow.FlowStepDTO;
import dev.zeann3th.stresspilot.entity.FlowEntity;
import dev.zeann3th.stresspilot.entity.FlowStepEntity;
import dev.zeann3th.stresspilot.exception.CommandExceptionBuilder;
import dev.zeann3th.stresspilot.repository.EndpointRepository;
import dev.zeann3th.stresspilot.repository.FlowRepository;
import dev.zeann3th.stresspilot.repository.FlowStepRepository;
import dev.zeann3th.stresspilot.service.FlowService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FlowServiceImpl implements FlowService {
    private static final String STEP_ID = "stepId";

    private final FlowRepository flowRepository;
    private final FlowStepRepository flowStepRepository;
    private final EndpointRepository endpointRepository;
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
        flowStepRepository.deleteAllByFlowId(flowId);
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

    @Override
    @Transactional
    public ResponseEntity<List<FlowStepDTO>> configureFlow(Long flowId, List<FlowStepDTO> steps) {
        if (!flowRepository.existsById(flowId)) {
            throw CommandExceptionBuilder.exception(ErrorCode.FLOW_NOT_FOUND);
        }

        flowStepRepository.deleteAllByFlowId(flowId);

        Map<String, String> stepIdMap = steps.stream()
                .collect(Collectors.toMap(FlowStepDTO::getId, s -> UUID.randomUUID().toString()));
        List<FlowStepEntity> entities = new ArrayList<>();
        for (FlowStepDTO dto : steps) {
            String newId = stepIdMap.get(dto.getId());

            validateStep(dto, stepIdMap);

            String nextTrue = dto.getNextIfTrue() != null ? stepIdMap.get(dto.getNextIfTrue()) : null;
            String nextFalse = dto.getNextIfFalse() != null ? stepIdMap.get(dto.getNextIfFalse()) : null;

            try {
                FlowStepEntity entity = FlowStepEntity.builder()
                        .id(newId)
                        .flowId(flowId)
                        .type(dto.getType())
                        .endpointId(dto.getEndpointId())
                        .preProcessor(dto.getPreProcessor() != null ? objectMapper.writeValueAsString(dto.getPreProcessor()) : null)
                        .postProcessor(dto.getPostProcessor() != null ? objectMapper.writeValueAsString(dto.getPostProcessor()) : null)
                        .nextIfTrue(nextTrue)
                        .nextIfFalse(nextFalse)
                        .condition(dto.getCondition())
                        .build();

                entities.add(entity);
            } catch (JsonProcessingException e) {
                throw CommandExceptionBuilder.exception(ErrorCode.FLOW_CONFIGURATION_ERROR);
            }
        }
        flowStepRepository.saveAll(entities);
        return ResponseEntity.ok(steps);
    }

    private void validateStep(FlowStepDTO step, Map<String, String> stepIdMap) {
        if (FlowStepType.ENDPOINT.name().equalsIgnoreCase(step.getType())) {
            if (step.getEndpointId() == null || !endpointRepository.existsById(step.getEndpointId())) {
                throw CommandExceptionBuilder.exception(ErrorCode.ENDPOINT_NOT_FOUND,
                        Map.of(STEP_ID, step.getId()));
            }
        } else if (FlowStepType.BRANCH.name().equalsIgnoreCase(step.getType())) {
            if (step.getCondition() == null || step.getCondition().isBlank()) {
                throw CommandExceptionBuilder.exception(ErrorCode.BAD_REQUEST,
                        Map.of(Constants.REASON, "Branch node must have a non-empty condition"));
            }
            if (step.getNextIfTrue() == null || !stepIdMap.containsKey(step.getNextIfTrue())) {
                throw CommandExceptionBuilder.exception(ErrorCode.BAD_REQUEST,
                        Map.of(Constants.REASON, "nextIfTrue id not found: " + step.getNextIfTrue()));
            }
            if (step.getNextIfFalse() == null || !stepIdMap.containsKey(step.getNextIfFalse())) {
                throw CommandExceptionBuilder.exception(ErrorCode.BAD_REQUEST,
                        Map.of(Constants.REASON, "nextIfFalse id not found: " + step.getNextIfFalse()));
            }
        } else {
            throw CommandExceptionBuilder.exception(ErrorCode.BAD_REQUEST,
                    Map.of(Constants.REASON, "Unknown step type: " + step.getType()));
        }
    }
}
