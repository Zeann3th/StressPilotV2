package dev.zeann3th.stresspilot.dto.flow;

import jakarta.validation.constraints.Min;
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
    @Min(value = 1, message = "threads must be greater than 0")
    private Integer threads;

    @NotNull
    @Min(value = 1, message = "totalDuration must be greater than 0")
    private Integer totalDuration;

    @NotNull
    @Min(value = 0, message = "rampUpDuration cannot be negative")
    private Integer rampUpDuration;

    @Builder.Default
    private Map<String, Object> variables = new HashMap<>();
}
