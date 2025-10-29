package dev.zeann3th.stresspilot.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.zeann3th.stresspilot.common.Constants;
import dev.zeann3th.stresspilot.common.enums.ErrorCode;
import dev.zeann3th.stresspilot.common.enums.FlowStepType;
import dev.zeann3th.stresspilot.common.mappers.FlowMapper;
import dev.zeann3th.stresspilot.dto.flow.CreateFlowRequestDTO;
import dev.zeann3th.stresspilot.dto.flow.FlowResponseDTO;
import dev.zeann3th.stresspilot.dto.flow.FlowStepDTO;
import dev.zeann3th.stresspilot.dto.flow.RunFlowRequestDTO;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FlowServiceImpl implements FlowService {
    private static final String STEP_ID = "stepId";
    private static final String START = "START";
    private static final String ENDPOINT = "ENDPOINT";
    private static final String BRANCH = "BRANCH";

    private final FlowRepository flowRepository;
    private final FlowStepRepository flowStepRepository;
    private final EndpointRepository endpointRepository;
    private final ObjectMapper objectMapper;
    private final FlowMapper flowMapper;

    @Override
    public Page<FlowResponseDTO> getListFlow(Long projectId, String name, Pageable pageable) {
        Page<FlowEntity> flowPage = flowRepository.findAllByCondition(projectId, name, pageable);
        return flowPage.map(flowMapper::toListDTO);
    }

    @Override
    public FlowResponseDTO getFlowDetail(Long flowId) {
        FlowEntity flowEntity = flowRepository.findById(flowId)
                .orElseThrow(() -> CommandExceptionBuilder.exception(ErrorCode.FLOW_NOT_FOUND));
        return flowMapper.toDetailDTO(flowEntity);
    }

    @Override
    public FlowResponseDTO createFlow(CreateFlowRequestDTO flowDTO) {
        FlowEntity flowEntity = FlowEntity.builder()
                .projectId(flowDTO.getProjectId())
                .name(flowDTO.getName())
                .description(flowDTO.getDescription())
                .build();
        FlowEntity saved = flowRepository.save(flowEntity);
        return flowMapper.toDetailDTO(saved);
    }

    @Override
    public void deleteFlow(Long flowId) {
        boolean exists = flowRepository.existsById(flowId);
        if (!exists) {
            throw CommandExceptionBuilder.exception(ErrorCode.FLOW_NOT_FOUND);
        }
        flowRepository.deleteById(flowId);
        flowStepRepository.deleteAllByFlowId(flowId);
    }

    @Override
    @Transactional
    public FlowResponseDTO updateFlow(Long flowId, Map<String, Object> flowDTO) {
        FlowEntity flowEntity = flowRepository.findById(flowId)
                .orElseThrow(() -> CommandExceptionBuilder.exception(ErrorCode.FLOW_NOT_FOUND));

        Set<String> forbiddenFields = Set.of("id", "projectId");
        Map<String, Object> sanitized = flowDTO.entrySet().stream()
                .filter(entry -> !forbiddenFields.contains(entry.getKey()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        try {
            objectMapper.updateValue(flowEntity, sanitized);
            FlowEntity updated = flowRepository.save(flowEntity);
            return flowMapper.toDetailDTO(updated);
        } catch (JsonMappingException e) {
            throw CommandExceptionBuilder.exception(ErrorCode.BAD_REQUEST, Map.of(Constants.REASON, "Invalid data format"));
        }
    }

    @Override
    @Transactional
    public List<FlowStepDTO> configureFlow(Long flowId, List<FlowStepDTO> steps) {
        if (!flowRepository.existsById(flowId)) {
            throw CommandExceptionBuilder.exception(ErrorCode.FLOW_NOT_FOUND);
        }

        validateStartStep(steps);

        flowStepRepository.deleteAllByFlowId(flowId);

        Map<String, String> stepIdMap = steps.stream()
                .collect(Collectors.toMap(FlowStepDTO::getId, s -> UUID.randomUUID().toString()));

        List<FlowStepEntity> entities = new ArrayList<>();

        for (FlowStepDTO dto : steps) {
            validateStep(dto, stepIdMap, steps);
        }

        detectInfiniteLoop(steps, stepIdMap);

        for (FlowStepDTO dto : steps) {
            try {
                FlowStepEntity entity = FlowStepEntity.builder()
                        .id(stepIdMap.get(dto.getId()))
                        .flowId(flowId)
                        .type(dto.getType())
                        .endpointId(dto.getEndpointId())
                        .preProcessor(dto.getPreProcessor() != null ? objectMapper.writeValueAsString(dto.getPreProcessor()) : null)
                        .postProcessor(dto.getPostProcessor() != null ? objectMapper.writeValueAsString(dto.getPostProcessor()) : null)
                        .nextIfTrue(dto.getNextIfTrue() != null ? stepIdMap.get(dto.getNextIfTrue()) : null)
                        .nextIfFalse(dto.getNextIfFalse() != null ? stepIdMap.get(dto.getNextIfFalse()) : null)
                        .condition(dto.getCondition())
                        .build();
                entities.add(entity);
            } catch (JsonProcessingException e) {
                throw CommandExceptionBuilder.exception(ErrorCode.FLOW_CONFIGURATION_ERROR);
            }
        }

        flowStepRepository.saveAll(entities);
        return steps;
    }

    private void validateStartStep(List<FlowStepDTO> steps) {
        long startCount = steps.stream()
                .filter(s -> FlowStepType.START.name().equalsIgnoreCase(s.getType()))
                .count();
        if (startCount == 0) {
            throw CommandExceptionBuilder.exception(ErrorCode.BAD_REQUEST,
                    Map.of(Constants.REASON, "Flow must contain exactly one START node (none found)"));
        }
        if (startCount > 1) {
            throw CommandExceptionBuilder.exception(ErrorCode.BAD_REQUEST,
                    Map.of(Constants.REASON, "Flow must contain exactly one START node (found " + startCount + ")"));
        }
    }

    @SuppressWarnings({"java:S3776", "java:S6916", "java:S6541"})
    private void validateStep(FlowStepDTO step, Map<String, String> stepIdMap, List<FlowStepDTO> steps) {
        String type = step.getType().toUpperCase();
        switch (type) {
            case START -> {
                if (step.getEndpointId() != null) {
                    throw CommandExceptionBuilder.exception(ErrorCode.BAD_REQUEST,
                            Map.of(Constants.REASON, "START node cannot have endpointId"));
                }

                if (step.getNextIfTrue() == null || !stepIdMap.containsKey(step.getNextIfTrue())) {
                    throw CommandExceptionBuilder.exception(ErrorCode.BAD_REQUEST,
                            Map.of(Constants.REASON, "START node must have a valid nextIfTrue target"));
                }

                if (step.getNextIfFalse() != null) {
                    throw CommandExceptionBuilder.exception(ErrorCode.BAD_REQUEST,
                            Map.of(Constants.REASON, "START node cannot have nextIfFalse"));
                }
            }
            case ENDPOINT -> {
                if (step.getEndpointId() == null || !endpointRepository.existsById(step.getEndpointId())) {
                    throw CommandExceptionBuilder.exception(ErrorCode.ENDPOINT_NOT_FOUND,
                            Map.of(STEP_ID, step.getId()));
                }

                if (step.getNextIfTrue() != null) {
                    FlowStepDTO target = steps.stream()
                            .filter(s -> s.getId().equals(step.getNextIfTrue()))
                            .findFirst()
                            .orElse(null);
                    if (target != null && START.equalsIgnoreCase(target.getType())) {
                        throw CommandExceptionBuilder.exception(ErrorCode.BAD_REQUEST,
                                Map.of(Constants.REASON, "ENDPOINT node cannot point to START node"));
                    }
                }
            }
            case BRANCH -> {
                if (step.getCondition() == null || step.getCondition().isBlank()) {
                    throw CommandExceptionBuilder.exception(ErrorCode.BAD_REQUEST,
                            Map.of(Constants.REASON, "Branch node must have a non-empty condition"));
                }
                if (step.getNextIfTrue() == null || !stepIdMap.containsKey(step.getNextIfTrue())) {
                    throw CommandExceptionBuilder.exception(ErrorCode.BAD_REQUEST,
                            Map.of(Constants.REASON, "Branch node has invalid nextIfTrue: " + step.getNextIfTrue()));
                }
                if (step.getNextIfFalse() == null || !stepIdMap.containsKey(step.getNextIfFalse())) {
                    throw CommandExceptionBuilder.exception(ErrorCode.BAD_REQUEST,
                            Map.of(Constants.REASON, "Branch node has invalid nextIfFalse: " + step.getNextIfFalse()));
                }

                FlowStepDTO targetTrue = steps.stream()
                        .filter(s -> s.getId().equals(step.getNextIfTrue()))
                        .findFirst()
                        .orElse(null);
                if (targetTrue != null && START.equalsIgnoreCase(targetTrue.getType())) {
                    throw CommandExceptionBuilder.exception(ErrorCode.BAD_REQUEST,
                            Map.of(Constants.REASON, "BRANCH node cannot point to START node in nextIfTrue"));
                }

                FlowStepDTO targetFalse = steps.stream()
                        .filter(s -> s.getId().equals(step.getNextIfFalse()))
                        .findFirst()
                        .orElse(null);
                if (targetFalse != null && START.equalsIgnoreCase(targetFalse.getType())) {
                    throw CommandExceptionBuilder.exception(ErrorCode.BAD_REQUEST,
                            Map.of(Constants.REASON, "BRANCH node cannot point to START node in nextIfFalse"));
                }
            }

            default -> throw CommandExceptionBuilder.exception(ErrorCode.BAD_REQUEST,
                    Map.of(Constants.REASON, "Unknown step type: " + step.getType()));
        }
    }

    private void detectInfiniteLoop(List<FlowStepDTO> steps, Map<String, String> stepIdMap) {
        Map<String, List<String>> graph = new HashMap<>();
        Set<String> terminalNodes = new HashSet<>();

        for (FlowStepDTO dto : steps) {
            String id = stepIdMap.get(dto.getId());

            List<String> nexts = new ArrayList<>();
            if (dto.getNextIfTrue() != null) nexts.add(stepIdMap.get(dto.getNextIfTrue()));
            if (dto.getNextIfFalse() != null) nexts.add(stepIdMap.get(dto.getNextIfFalse()));

            graph.put(id, nexts);

            if (FlowStepType.ENDPOINT.name().equalsIgnoreCase(dto.getType()) && nexts.isEmpty()) {
                terminalNodes.add(id);
            }
        }

        if (terminalNodes.isEmpty()) {
            throw CommandExceptionBuilder.exception(ErrorCode.FLOW_CONFIGURATION_ERROR,
                    Map.of("reason", "No terminal endpoint found — flow would be infinite"));
        }

        Map<String, Boolean> canReachEnd = new HashMap<>();
        for (String node : graph.keySet()) {
            if (!canReachEndpoint(node, graph, terminalNodes, new HashSet<>(), canReachEnd)) {
                throw CommandExceptionBuilder.exception(ErrorCode.FLOW_CONFIGURATION_ERROR,
                        Map.of("reason", "Infinite cycle detected — node " + node + " cannot reach terminal endpoint"));
            }
        }
    }

    private boolean canReachEndpoint(String node,
                                     Map<String, List<String>> graph,
                                     Set<String> endpoints,
                                     Set<String> visiting,
                                     Map<String, Boolean> memo) {
        if (memo.containsKey(node)) return memo.get(node);
        if (endpoints.contains(node)) {
            memo.put(node, true);
            return true;
        }
        if (visiting.contains(node)) {
            return false;
        }
        visiting.add(node);
        for (String next : graph.getOrDefault(node, List.of())) {
            if (next != null && canReachEndpoint(next, graph, endpoints, visiting, memo)) {
                memo.put(node, true);
                visiting.remove(node);
                return true;
            }
        }
        visiting.remove(node);
        memo.put(node, false);
        return false;
    }

    @Override
    public void runFlow(Long flowId, RunFlowRequestDTO runFlowRequestDTO) {
        // Check flow exist, then get environment
        // Merge with variables from request, prioritize request variables if conflict
        // Get all the steps needed
        // init loop of users by request threads (cookie jars, own context var), use CompletableFuture to run in parallel
        // check pre processor, run if any
        // use EndpointRunner to know how to run each step since having multiple types Http, gRPC, etc.
        // start from start node then use map to get to next node then call endpoint runner again until no next node
        // check post processor, run if any
        // log result to db

        // flow done ? update flow run status, must have another endpoint to do inquiry polling
    }
}
