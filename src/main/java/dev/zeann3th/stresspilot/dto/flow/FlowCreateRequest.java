package dev.zeann3th.stresspilot.dto.flow;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FlowCreateRequest {
    @NotNull(message = "Project ID must not be blank")
    private Long projectId;
    @NotBlank(message = "Flow name must not be blank")
    private String name;
    private String description;
}
