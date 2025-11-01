package dev.zeann3th.stresspilot.service.flow;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.zeann3th.stresspilot.common.Constants;
import dev.zeann3th.stresspilot.common.enums.ErrorCode;
import dev.zeann3th.stresspilot.common.enums.FlowStepType;
import dev.zeann3th.stresspilot.dto.flow.FlowStepDTO;
import dev.zeann3th.stresspilot.entity.FlowStepEntity;
import dev.zeann3th.stresspilot.exception.CommandExceptionBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;

import java.util.*;

@Slf4j(topic = "[Flow Service Utils]")
@SuppressWarnings("java:S3776")
public class FlowUtils {
    private static final Random RANDOM = new Random();
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private FlowUtils() {}

    public static void process(Map<String, Object> processorMap, Map<String, Object> environment, Object prevResp) {
        if (CollectionUtils.isEmpty(processorMap)) return;

        try {
            if (processorMap.containsKey("sleep")) {
                long baseDelayMs = Long.parseLong(processorMap.get("sleep").toString());
                long randomExtra = 500 + (long) RANDOM.nextInt(501);
                Thread.sleep(baseDelayMs + randomExtra);
            }

            if (processorMap.containsKey("inject")) {
                Object injectObj = processorMap.get("inject");
                if (injectObj != null) {
                    Map<String, Object> injectMap = objectMapper.convertValue(injectObj, new TypeReference<>() {});
                    environment.putAll(injectMap);
                }
            }

            if (processorMap.containsKey("extract") && prevResp != null) {
                Object extractObj = processorMap.get("extract");
                if (extractObj != null) {
                    Map<String, Object> extractMap = objectMapper.convertValue(extractObj, new TypeReference<>() {});
                    for (Map.Entry<String, Object> entry : extractMap.entrySet()) {
                        String targetKey = entry.getKey();
                        String path = String.valueOf(entry.getValue());
                        Object value = resolvePath(prevResp, path);
                        if (value != null) {
                            environment.put(targetKey, value);
                        }
                    }
                }
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            log.error("Error processing processor: {}", e.getMessage(), e);
        }
    }

    private static Object resolvePath(Object obj, String path) {
        if (obj == null || path == null || path.isBlank()) return null;

        String[] parts = path.replaceAll("\\[(\\w+)\\]", ".$1").split("\\.");
        Object current = obj;

        for (String part : parts) {
            if (current == null) return null;

            switch (current) {
                case Map<?, ?> map -> current = map.get(part);
                case List<?> list -> {
                    try {
                        int index = Integer.parseInt(part);
                        if (index >= 0 && index < list.size()) {
                            current = list.get(index);
                        } else {
                            return null;
                        }
                    } catch (NumberFormatException ex) {
                        return null;
                    }
                }
                default -> { return null; }
            }
        }

        return current;
    }

    public static void validateStartStep(List<FlowStepDTO> steps) {
        long startCount = steps.stream().filter(s -> FlowStepType.START.name().equalsIgnoreCase(s.getType())).count();
        if (startCount == 0)
            throw CommandExceptionBuilder.exception(ErrorCode.BAD_REQUEST, Map.of(Constants.REASON, "Flow must contain one START node (none found)"));
        if (startCount > 1)
            throw CommandExceptionBuilder.exception(ErrorCode.BAD_REQUEST, Map.of(Constants.REASON, "Flow must contain one START node (found " + startCount + ")"));
    }

    public static void detectInfiniteLoop(List<FlowStepDTO> steps, Map<String, String> stepIdMap) {
        Map<String, List<String>> graph = new HashMap<>();
        Set<String> terminalNodes = new HashSet<>();
        for (FlowStepDTO dto : steps) {
            String id = stepIdMap.get(dto.getId());
            List<String> nexts = new ArrayList<>();
            if (dto.getNextIfTrue() != null) nexts.add(stepIdMap.get(dto.getNextIfTrue()));
            if (dto.getNextIfFalse() != null) nexts.add(stepIdMap.get(dto.getNextIfFalse()));
            graph.put(id, nexts);
            if (FlowStepType.ENDPOINT.name().equalsIgnoreCase(dto.getType()) && nexts.isEmpty())
                terminalNodes.add(id);
        }
        if (terminalNodes.isEmpty())
            throw CommandExceptionBuilder.exception(ErrorCode.FLOW_CONFIGURATION_ERROR, Map.of("reason", "No terminal endpoint found — flow would be infinite"));
        Map<String, Boolean> memo = new HashMap<>();
        for (String node : graph.keySet()) {
            if (!canReachEndpoint(node, graph, terminalNodes, new HashSet<>(), memo))
                throw CommandExceptionBuilder.exception(ErrorCode.FLOW_CONFIGURATION_ERROR, Map.of("reason", "Infinite cycle detected — node " + node + " cannot reach terminal endpoint"));
        }
    }

    private static boolean canReachEndpoint(String node, Map<String, List<String>> graph, Set<String> endpoints,
                                     Set<String> visiting, Map<String, Boolean> memo) {
        if (memo.containsKey(node)) return memo.get(node);
        if (endpoints.contains(node)) { memo.put(node, true); return true; }
        if (visiting.contains(node)) return false;
        visiting.add(node);
        for (String next : graph.getOrDefault(node, List.of())) {
            if (next != null && canReachEndpoint(next, graph, endpoints, visiting, memo)) {
                memo.put(node, true); visiting.remove(node); return true;
            }
        }
        visiting.remove(node); memo.put(node, false); return false;
    }

    public static FlowStepEntity findStartNode(Map<String, FlowStepEntity> stepMap) {
        return stepMap.values().stream().filter(s -> FlowStepType.START.name().equalsIgnoreCase(s.getType())).findFirst().orElse(null);
    }
}