package dev.zeann3th.stresspilot.service.flow.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.zeann3th.stresspilot.common.Constants;
import dev.zeann3th.stresspilot.common.enums.ErrorCode;
import dev.zeann3th.stresspilot.common.enums.FlowStepType;
import dev.zeann3th.stresspilot.common.mappers.FlowMapper;
import dev.zeann3th.stresspilot.common.utils.InMemoryCookieJar;
import dev.zeann3th.stresspilot.dto.flow.*;
import dev.zeann3th.stresspilot.dto.endpoint.ExecuteEndpointResponseDTO;
import dev.zeann3th.stresspilot.entity.EnvironmentVariableEntity;
import dev.zeann3th.stresspilot.entity.FlowEntity;
import dev.zeann3th.stresspilot.entity.FlowStepEntity;
import dev.zeann3th.stresspilot.entity.EndpointEntity;
import dev.zeann3th.stresspilot.entity.ProjectEntity;
import dev.zeann3th.stresspilot.exception.CommandExceptionBuilder;
import dev.zeann3th.stresspilot.repository.*;
import dev.zeann3th.stresspilot.service.executor.EndpointExecutorServiceFactory;
import dev.zeann3th.stresspilot.service.flow.FlowService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class FlowServiceImpl implements FlowService {
    private static final String STEP_ID = "stepId";
    private static final String START = "START";
    private static final String ENDPOINT = "ENDPOINT";
    private static final String BRANCH = "BRANCH";

    private final FlowRepository flowRepository;
    private final FlowStepRepository flowStepRepository;
    private final ProjectRepository projectRepository;
    private final EnvironmentVariableRepository envVarRepo;
    private final EndpointRepository endpointRepository;
    private final EndpointExecutorServiceFactory endpointExecutorServiceFactory;
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
        var resp = flowMapper.toDetailDTO(flowEntity);

        List<FlowStepEntity> steps = flowStepRepository.findAllByFlowId(flowId);
        sortSteps(steps);

        List<FlowStepDTO> stepDTOs = steps.stream()
                .map(entity -> FlowStepDTO.builder()
                        .id(entity.getId())
                        .type(entity.getType())
                        .endpointId(entity.getEndpointId())
                        .preProcessor(parseProcessor(entity.getPreProcessor()))
                        .postProcessor(parseProcessor(entity.getPostProcessor()))
                        .nextIfTrue(entity.getNextIfTrue())
                        .nextIfFalse(entity.getNextIfFalse())
                        .condition(entity.getCondition())
                        .build())
                .toList();
        resp.setSteps(stepDTOs);
        return resp;
    }

    private Map<String, Object> parseProcessor(String json) {
        if (json == null || json.isBlank()) return Collections.emptyMap();
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (Exception e) {
            return Collections.emptyMap();
        }
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
        sortSteps(entities);
        return entities.stream()
                .map(entity -> FlowStepDTO.builder()
                        .id(entity.getId())
                        .type(entity.getType())
                        .endpointId(entity.getEndpointId())
                        .preProcessor(parseProcessor(entity.getPreProcessor()))
                        .postProcessor(parseProcessor(entity.getPostProcessor()))
                        .nextIfTrue(entity.getNextIfTrue())
                        .nextIfFalse(entity.getNextIfFalse())
                        .condition(entity.getCondition())
                        .build())
                .toList();
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

    private void sortSteps(List<FlowStepEntity> steps) {
        steps.sort((a, b) -> {
            if (START.equalsIgnoreCase(a.getType())) return -1;
            if (START.equalsIgnoreCase(b.getType())) return 1;
            return a.getId().compareTo(b.getId());
        });
    }

    @Override
    public void runFlow(Long flowId, RunFlowRequestDTO runFlowRequestDTO) {
        FlowEntity flowEntity = flowRepository.findById(flowId)
                .orElseThrow(() -> CommandExceptionBuilder.exception(ErrorCode.FLOW_NOT_FOUND));

        ProjectEntity projectEntity = projectRepository.findById(flowEntity.getProjectId())
                .orElseThrow(() -> CommandExceptionBuilder.exception(ErrorCode.PROJECT_NOT_FOUND));

        Map<String, Object> environment = envVarRepo
                .findAllByEnvironmentIdAndIsActiveTrue(projectEntity.getEnvironmentId())
                .stream()
                .collect(Collectors.toMap(
                        EnvironmentVariableEntity::getKey,
                        EnvironmentVariableEntity::getValue,
                        (v1, v2) -> v2
                ));

        environment.putAll(runFlowRequestDTO.getVariables());

        List<FlowStepEntity> steps = flowStepRepository.findAllByFlowId(flowId);
        Map<String, FlowStepEntity> stepMap = steps.stream()
                .collect(Collectors.toMap(FlowStepEntity::getId, s -> s));

        sortSteps(steps);

        executeFlowWithThreads(flowId, stepMap, environment, runFlowRequestDTO);
    }

    private void executeFlowWithThreads(Long flowId, Map<String, FlowStepEntity> stepMap,
                                        Map<String, Object> baseEnvironment,
                                        RunFlowRequestDTO config) {
        int threads = config.getThreads();
        int totalDuration = config.getTotalDuration();
        int rampUpDuration = config.getRampUpDuration();

        long threadStartDelay = (rampUpDuration * 1000L) / threads;
        AtomicBoolean stopSignal = new AtomicBoolean(false);
        long testStartTime = System.currentTimeMillis();
        long testEndTime = testStartTime + (totalDuration * 1000L);

        ExecutorService executor = Executors.newFixedThreadPool(threads);
        ScheduledExecutorService timeoutScheduler = Executors.newSingleThreadScheduledExecutor();

        timeoutScheduler.schedule(() -> {
            log.info("Total duration reached, signaling all threads to stop");
            stopSignal.set(true);
            executor.shutdownNow();
        }, totalDuration, TimeUnit.SECONDS);

        List<Future<?>> futures = new ArrayList<>();

        try {
            for (int i = 0; i < threads; i++) {
                final int threadIndex = i;
                final long startDelay = i * threadStartDelay;

                Future<?> future = executor.submit(() -> {
                    try {
                        if (startDelay > 0) {
                            Thread.sleep(startDelay);
                        }

                        log.info("Thread {} started", threadIndex);

                        FlowThreadContext context = createThreadContext(threadIndex, baseEnvironment);

                        while (!stopSignal.get() && System.currentTimeMillis() < testEndTime) {
                            try {
                                executeFlowIteration(flowId, stepMap, context, stopSignal, testEndTime);
                            } catch (Exception e) {
                                log.error("Error in thread {} iteration {}: {}",
                                        threadIndex, context.getIterationCount(), e.getMessage(), e);
                            }
                        }

                        log.info("Thread {} completed with {} iterations", threadIndex, context.getIterationCount());
                    } catch (InterruptedException e) {
                        log.info("Thread {} interrupted", threadIndex);
                        Thread.currentThread().interrupt();
                    } catch (Exception e) {
                        log.error("Thread {} fatal error: {}", threadIndex, e.getMessage(), e);
                    }
                });

                futures.add(future);
            }

            for (Future<?> future : futures) {
                try {
                    future.get();
                } catch (InterruptedException | ExecutionException e) {
                    log.error("Thread execution error: {}", e.getMessage());
                }
            }

        } finally {
            executor.shutdown();
            timeoutScheduler.shutdown();
            try {
                if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                    executor.shutdownNow();
                }
                if (!timeoutScheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                    timeoutScheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                executor.shutdownNow();
                timeoutScheduler.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }

    private FlowThreadContext createThreadContext(int threadIndex, Map<String, Object> baseEnvironment) {
        FlowThreadContext context = new FlowThreadContext();
        context.setThreadId(threadIndex);
        context.setCookieJar(new InMemoryCookieJar());
        context.setVariables(new ConcurrentHashMap<>(baseEnvironment));
        context.setIterationCount(0);
        return context;
    }

    private void executeFlowIteration(Long flowId, Map<String, FlowStepEntity> stepMap,
                                      FlowThreadContext context, AtomicBoolean stopSignal,
                                      long testEndTime) {
        context.incrementIteration();

        FlowStepEntity currentStep = findStartNode(stepMap);
        if (currentStep == null) {
            log.warn("No START node found for flow {}", flowId);
            return;
        }

        String nextStepId = currentStep.getNextIfTrue();
        currentStep = stepMap.get(nextStepId);

        while (currentStep != null && !stopSignal.get() && System.currentTimeMillis() < testEndTime) {
            try {
                String stepType = currentStep.getType().toUpperCase();

                switch (stepType) {
                    case ENDPOINT -> {
                        ExecuteEndpointResponseDTO result = executeEndpointStep(currentStep, context);
                        logStepResult(flowId, currentStep.getId(), context.getThreadId(),
                                context.getIterationCount(), result);

                        nextStepId = result.isSuccess() && currentStep.getNextIfTrue() != null
                                ? currentStep.getNextIfTrue()
                                : currentStep.getNextIfFalse();
                    }
                    case BRANCH -> {
                        boolean conditionResult = evaluateCondition(currentStep.getCondition(), context);
                        nextStepId = conditionResult ? currentStep.getNextIfTrue() : currentStep.getNextIfFalse();
                        log.debug("Branch condition '{}' evaluated to {} for thread {}",
                                currentStep.getCondition(), conditionResult, context.getThreadId());
                    }
                    default -> log.error("Unknown step type: {}", stepType);
                }

                if (nextStepId == null) {
                    break;
                }

                currentStep = stepMap.get(nextStepId);

            } catch (Exception e) {
                log.error("Error executing step {} in thread {}: {}",
                        currentStep.getId(), context.getThreadId(), e.getMessage(), e);
                logStepError(flowId, currentStep.getId(), context.getThreadId(),
                        context.getIterationCount(), e);
                break;
            }
        }
    }

    private ExecuteEndpointResponseDTO executeEndpointStep(FlowStepEntity step, FlowThreadContext context) {
        if (step.getPreProcessor() != null && !step.getPreProcessor().isBlank()) {
            executeProcessor(step.getPreProcessor(), context, "pre-processor");
        }

        EndpointEntity endpointEntity = endpointRepository.findById(step.getEndpointId())
                .orElseThrow(() -> CommandExceptionBuilder.exception(ErrorCode.ENDPOINT_NOT_FOUND));

        var executorService = endpointExecutorServiceFactory.getExecutor(endpointEntity.getType());
        long startTime = System.currentTimeMillis();

        ExecuteEndpointResponseDTO result;
        try {
            result = executorService.execute(endpointEntity, context.getVariables(), context.getCookieJar());
        } catch (Exception e) {
            log.error("Error executing endpoint {}: {}", step.getEndpointId(), e.getMessage(), e);
            Map<String, Object> data = Map.of("error", e.getMessage());
            result = ExecuteEndpointResponseDTO.builder()
                    .responseTimeMs(System.currentTimeMillis() - startTime)
                    .success(false)
                    .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
                    .data(data)
                    .rawResponse(data.toString())
                    .build();
        }

        if (step.getPostProcessor() != null && !step.getPostProcessor().isBlank()) {
            executeProcessor(step.getPostProcessor(), context, "post-processor");
        }

        return result;
    }

    private void executeProcessor(String processorJson, FlowThreadContext context, String type) {
        try {
            Map<String, Object> processor = objectMapper.readValue(processorJson, new TypeReference<>() {});
            // TODO: Implement processor logic (variable extraction, assertions, etc.)
            log.debug("Executing {} for thread {}", type, context.getThreadId());
        } catch (Exception e) {
            log.error("Error executing {} for thread {}: {}", type, context.getThreadId(), e.getMessage(), e);
        }
    }

    private boolean evaluateCondition(String condition, FlowThreadContext context) {
        // TODO: Implement condition evaluation logic
        // This could use SpEL, JavaScript engine, or custom expression evaluator
        // For now, return true as placeholder
        return true;
    }

    private FlowStepEntity findStartNode(Map<String, FlowStepEntity> stepMap) {
        return stepMap.values().stream()
                .filter(s -> START.equalsIgnoreCase(s.getType()))
                .findFirst()
                .orElse(null);
    }

    private void logStepResult(Long flowId, String stepId, int threadId, int iteration,
                               ExecuteEndpointResponseDTO result) {
        // TODO: Implement database logging
        log.info("Flow {} - Thread {} - Iteration {} - Step {} - Success: {} - Time: {}ms",
                flowId, threadId, iteration, stepId, result.isSuccess(), result.getResponseTimeMs());
    }

    private void logStepError(Long flowId, String stepId, int threadId, int iteration, Exception error) {
        // TODO: Implement database error logging
        log.error("Flow {} - Thread {} - Iteration {} - Step {} - Error: {}",
                flowId, threadId, iteration, stepId, error.getMessage());
    }
}