package dev.zeann3th.stresspilot.dto.flow;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FlowStepDTO {
    private String id;
    private String type;
    private Long endpointId;
    private Map<String, Object> preProcessor;
    private Map<String, Object> postProcessor;
    private String nextIfTrue;
    private String nextIfFalse;
    private String condition;
}
