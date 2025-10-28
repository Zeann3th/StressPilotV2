package dev.zeann3th.stresspilot.dto.environment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EnvironmentVariableDTO {
    private Long id;
    private Long environmentId;
    private String key;
    private String value;
    private Boolean isActive;
}
