package dev.zeann3th.stresspilot.dto.environment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateEnvironmentRequestDTO {
    private List<Add> added;
    private List<Update> updated;
    private List<Long> removed;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Add {
        private String key;
        private String value;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Update {
        private Long id;
        private String key;
        private String value;
        private boolean isActive;
    }
}
