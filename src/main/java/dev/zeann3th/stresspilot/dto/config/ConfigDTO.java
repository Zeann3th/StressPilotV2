package dev.zeann3th.stresspilot.dto.config;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConfigDTO {
    @NotNull
    private String key;
    @NotNull
    private String value;
}
