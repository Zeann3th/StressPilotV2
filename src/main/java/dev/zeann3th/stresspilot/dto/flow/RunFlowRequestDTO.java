package dev.zeann3th.stresspilot.dto.flow;

import jakarta.validation.constraints.NotNull;

import java.util.Map;

public class RunFlowRequestDTO {
    @NotNull
    private int threads;
    @NotNull
    private int totalDuration;
    @NotNull
    private int rampUpDuration;

    private Map<String, Object> variables;
}
