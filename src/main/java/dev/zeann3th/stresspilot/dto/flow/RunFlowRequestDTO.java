package dev.zeann3th.stresspilot.dto.flow;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RunFlowRequestDTO {
    @NotNull
    private int threads;
    @NotNull
    private int totalDuration;
    @NotNull
    private int rampUpDuration;

    @Builder.Default
    private Map<String, Object> variables = new HashMap<>();
}
