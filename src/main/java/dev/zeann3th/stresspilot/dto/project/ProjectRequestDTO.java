package dev.zeann3th.stresspilot.dto.project;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjectRequestDTO {
    @NotBlank(message = "Project name must not be blank")
    private String name;

    private String description;
}
