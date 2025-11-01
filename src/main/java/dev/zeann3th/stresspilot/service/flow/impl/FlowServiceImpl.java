package dev.zeann3th.stresspilot.service.flow.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.zeann3th.stresspilot.common.Constants;
import dev.zeann3th.stresspilot.common.enums.ErrorCode;
import dev.zeann3th.stresspilot.common.enums.RunStatus;
import dev.zeann3th.stresspilot.common.mappers.FlowMapper;
import dev.zeann3th.stresspilot.common.utils.InMemoryCookieJar;
import dev.zeann3th.stresspilot.dto.flow.*;
import dev.zeann3th.stresspilot.dto.endpoint.ExecuteEndpointResponseDTO;
import dev.zeann3th.stresspilot.entity.*;
import dev.zeann3th.stresspilot.exception.CommandExceptionBuilder;
import dev.zeann3th.stresspilot.repository.*;
import dev.zeann3th.stresspilot.service.executor.EndpointExecutorServiceFactory;
import dev.zeann3th.stresspilot.service.flow.FlowService;
import dev.zeann3th.stresspilot.service.flow.FlowUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

@Slf4j(topic = "[Flow Service]")
@Service
@RequiredArgsConstructor
@SuppressWarnings("java:S3776")
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
    private final RunRepository runRepository;
    private final RequestLogRepository requestLogRepository;
    private final EndpointExecutorServiceFactory endpointExecutorServiceFactory;
    private final ObjectMapper objectMapper;
    private final FlowMapper flowMapper;

    /* ==================== FLOW CRUD ==================== */

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
        if (!flowRepository.existsById(flowId))
            throw CommandExceptionBuilder.exception(ErrorCode.FLOW_NOT_FOUND);

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
            throw CommandExceptionBuilder.exception(ErrorCode.BAD_REQUEST,
                    Map.of(Constants.REASON, "Invalid data format"));
        }
    }

    /* ==================== CONFIGURE FLOW ==================== */

    @Override
    @Transactional
    public List<FlowStepDTO> configureFlow(Long flowId, List<FlowStepDTO> steps) {
        if (!flowRepository.existsById(flowId))
            throw CommandExceptionBuilder.exception(ErrorCode.FLOW_NOT_FOUND);

        FlowUtils.validateStartStep(steps);
        flowStepRepository.deleteAllByFlowId(flowId);

        Map<String, String> stepIdMap = steps.stream()
                .collect(Collectors.toMap(FlowStepDTO::getId, s -> UUID.randomUUID().toString()));

        for (FlowStepDTO dto : steps)
            validateStep(dto, stepIdMap);

        FlowUtils.detectInfiniteLoop(steps, stepIdMap);

        List<FlowStepEntity> entities = new ArrayList<>();
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

    private void validateStep(FlowStepDTO step, Map<String, String> stepIdMap) {
        String type = step.getType().toUpperCase();
        switch (type) {
            case START -> {
                if (step.getEndpointId() != null)
                    throw CommandExceptionBuilder.exception(ErrorCode.BAD_REQUEST,
                            Map.of(Constants.REASON, "START node cannot have endpointId"));
                if (step.getNextIfTrue() == null || !stepIdMap.containsKey(step.getNextIfTrue()))
                    throw CommandExceptionBuilder.exception(ErrorCode.BAD_REQUEST,
                            Map.of(Constants.REASON, "START node must have a valid nextIfTrue target"));
                if (step.getNextIfFalse() != null)
                    throw CommandExceptionBuilder.exception(ErrorCode.BAD_REQUEST,
                            Map.of(Constants.REASON, "START node cannot have nextIfFalse"));
            }
            case ENDPOINT -> {
                boolean invalid = step.getEndpointId() == null || !endpointRepository.existsById(step.getEndpointId());
                if (invalid) {
                    throw CommandExceptionBuilder.exception(
                            ErrorCode.ENDPOINT_NOT_FOUND,
                            Map.of(STEP_ID, step.getId())
                    );
                }
            }
            case BRANCH -> {
                if (step.getCondition() == null || step.getCondition().isBlank())
                    throw CommandExceptionBuilder.exception(ErrorCode.BAD_REQUEST,
                            Map.of(Constants.REASON, "Branch node must have condition"));
                if (step.getNextIfTrue() == null || !stepIdMap.containsKey(step.getNextIfTrue()))
                    throw CommandExceptionBuilder.exception(ErrorCode.BAD_REQUEST,
                            Map.of(Constants.REASON, "Branch node has invalid nextIfTrue"));
                if (step.getNextIfFalse() == null || !stepIdMap.containsKey(step.getNextIfFalse()))
                    throw CommandExceptionBuilder.exception(ErrorCode.BAD_REQUEST,
                            Map.of(Constants.REASON, "Branch node has invalid nextIfFalse"));
            }
            default -> throw CommandExceptionBuilder.exception(ErrorCode.BAD_REQUEST,
                    Map.of(Constants.REASON, "Unknown step type: " + step.getType()));
        }
    }

    private void sortSteps(List<FlowStepEntity> steps) {
        steps.sort((a, b) -> {
            if (START.equalsIgnoreCase(a.getType())) return -1;
            if (START.equalsIgnoreCase(b.getType())) return 1;
            return a.getId().compareTo(b.getId());
        });
    }

    @Override
    @Async
    public void runFlow(Long flowId, RunFlowRequestDTO runFlowRequestDTO) {
        FlowEntity flowEntity = flowRepository.findById(flowId)
                .orElseThrow(() -> CommandExceptionBuilder.exception(ErrorCode.FLOW_NOT_FOUND));

        ProjectEntity projectEntity = projectRepository.findById(flowEntity.getProjectId())
                .orElseThrow(() -> CommandExceptionBuilder.exception(ErrorCode.PROJECT_NOT_FOUND));

        Map<String, Object> environment = envVarRepo
                .findAllByEnvironmentIdAndIsActiveTrue(projectEntity.getEnvironmentId())
                .stream()
                .collect(Collectors.toMap(EnvironmentVariableEntity::getKey, EnvironmentVariableEntity::getValue, (v1, v2) -> v2));

        RunEntity runEntity = RunEntity.builder()
                .flowId(flowId)
                .status(RunStatus.RUNNING.name())
                .threads(runFlowRequestDTO.getThreads())
                .duration(runFlowRequestDTO.getTotalDuration())
                .rampUpDuration(runFlowRequestDTO.getRampUpDuration())
                .build();
        RunEntity savedRun = runRepository.save(runEntity);

        environment.putAll(runFlowRequestDTO.getVariables());

        List<FlowStepEntity> steps = flowStepRepository.findAllByFlowId(flowId);
        Map<String, FlowStepEntity> stepMap = steps.stream()
                .collect(Collectors.toMap(FlowStepEntity::getId, s -> s));

        Map<Long, EndpointEntity> endpointMap = steps.stream()
                .map(FlowStepEntity::getEndpointId)
                .filter(Objects::nonNull)
                .distinct()
                .map(id -> endpointRepository.findById(id).orElse(null))
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(EndpointEntity::getId, e -> e));

        sortSteps(steps);

        executeFlowWithThreads(savedRun.getId(), stepMap, endpointMap, environment, runFlowRequestDTO);

        savedRun.setStatus(RunStatus.COMPLETED.name());
        runRepository.save(savedRun);
    }

    private void executeFlowWithThreads(Long runId,
                                        Map<String, FlowStepEntity> stepMap,
                                        Map<Long, EndpointEntity> endpointMap,
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
        for (int i = 0; i < threads; i++) {
            final int threadIndex = i;
            final long startDelay = i * threadStartDelay;

            Future<?> future = executor.submit(() -> {
                try {
                    if (startDelay > 0) Thread.sleep(startDelay);
                    log.info("Thread {} started", threadIndex);
                    FlowThreadContext context = createThreadContext(threadIndex, baseEnvironment, runId);
                    while (!stopSignal.get() && System.currentTimeMillis() < testEndTime) {
                        try {
                            executeFlowIteration(stepMap, endpointMap, context, stopSignal, testEndTime);
                        } catch (Exception e) {
                            log.error("Thread {} iteration {} error: {}", threadIndex, context.getIterationCount(), e.getMessage(), e);
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
            try { future.get(); }
            catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            catch (ExecutionException e) { log.error("Thread execution error: {}", e.getMessage()); }
        }

        executor.shutdown();
        timeoutScheduler.shutdown();
    }

    private FlowThreadContext createThreadContext(int threadIndex, Map<String, Object> baseEnvironment, Long runId) {
        FlowThreadContext context = new FlowThreadContext();
        context.setThreadId(threadIndex);
        context.setCookieJar(new InMemoryCookieJar());
        context.setVariables(new ConcurrentHashMap<>(baseEnvironment));
        context.setIterationCount(0);
        context.setRunId(runId);
        return context;
    }

    private void executeFlowIteration(Map<String, FlowStepEntity> stepMap,
                                      Map<Long, EndpointEntity> endpointMap,
                                      FlowThreadContext context,
                                      AtomicBoolean stopSignal,
                                      long testEndTime) {

        context.incrementIteration();
        FlowStepEntity currentStep = FlowUtils.findStartNode(stepMap);
        if (currentStep == null) { log.warn("No START node found"); return; }

        String nextStepId = currentStep.getNextIfTrue();
        currentStep = stepMap.get(nextStepId);

        while (currentStep != null && !stopSignal.get() && System.currentTimeMillis() < testEndTime) {
            try {
                String stepType = currentStep.getType().toUpperCase();
                switch (stepType) {
                    case ENDPOINT -> {
                        ExecuteEndpointResponseDTO result = executeEndpointStep(currentStep, endpointMap, context);
                        nextStepId = result.isSuccess() && currentStep.getNextIfTrue() != null ? currentStep.getNextIfTrue() : currentStep.getNextIfFalse();
                    }
                    case BRANCH -> {
                        boolean conditionResult = evaluateCondition(currentStep.getCondition(), context);
                        nextStepId = conditionResult ? currentStep.getNextIfTrue() : currentStep.getNextIfFalse();
                        log.debug("Branch '{}' evaluated {} for thread {}", currentStep.getCondition(), conditionResult, context.getThreadId());
                    }
                    default -> log.error("Unknown step type: {}", stepType);
                }
                currentStep = nextStepId != null ? stepMap.get(nextStepId) : null;
            } catch (Exception e) {
                break;
            }
        }
    }

    private ExecuteEndpointResponseDTO executeEndpointStep(FlowStepEntity step, Map<Long, EndpointEntity> endpointMap, FlowThreadContext context) {
        if (step.getPreProcessor() != null && !step.getPreProcessor().isBlank())
            executeProcessor(step.getPreProcessor(), context, null, "pre-processor");

        EndpointEntity endpointEntity = endpointMap.get(step.getEndpointId());
        var executorService = endpointExecutorServiceFactory.getExecutor(endpointEntity.getType());

        long startTime = System.currentTimeMillis();
        ExecuteEndpointResponseDTO result;
        try {
            result = executorService.execute(endpointEntity, context.getVariables(), context.getCookieJar());
        } catch (Exception e) {
            Map<String, Object> data = Map.of("error", e.getMessage());
            result = ExecuteEndpointResponseDTO.builder()
                    .responseTimeMs(System.currentTimeMillis() - startTime)
                    .success(false)
                    .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
                    .data(data)
                    .rawResponse(data.toString())
                    .build();
        }

        logStepResult(context.getRunId(), endpointEntity, result);

        if (step.getPostProcessor() != null && !step.getPostProcessor().isBlank())
            executeProcessor(step.getPostProcessor(), context, result.getData(), "post-processor");

        return result;
    }

    private void executeProcessor(String processorJson, FlowThreadContext context, Object responseData, String type) {
        try {
            Map<String, Object> processor = objectMapper.readValue(processorJson, new TypeReference<>() {});
            FlowUtils.process(processor, context.getVariables(), responseData);
        } catch (Exception e) {
            log.error("Error executing {} for thread {}: {}", type, context.getThreadId(), e.getMessage(), e);
        }
    }

    private boolean evaluateCondition(String condition, FlowThreadContext context) {
        SpelExpressionParser parser = new SpelExpressionParser();
        StandardEvaluationContext evalContext = new StandardEvaluationContext();
        evalContext.setVariable("variables", context.getVariables());
        try {
            return Boolean.TRUE.equals(parser.parseExpression(condition).getValue(evalContext, Boolean.class));
        } catch (Exception e) {
            log.error("Error evaluating condition '{}': {}", condition, e.getMessage());
            return false;
        }
    }

    private void logStepResult(Long runId, EndpointEntity request, ExecuteEndpointResponseDTO response) {
        RequestLogEntity logEntity = RequestLogEntity.builder()
                .runId(runId)
                .endpointId(request.getId())
                .statusCode(response.getStatusCode())
                .responseTime(response.getResponseTimeMs())
                .request(request.toString())
                .response(response.getRawResponse())
                .build();
        requestLogRepository.save(logEntity);
    }
}