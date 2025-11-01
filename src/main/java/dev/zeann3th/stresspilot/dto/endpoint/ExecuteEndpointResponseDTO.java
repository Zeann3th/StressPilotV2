package dev.zeann3th.stresspilot.dto.endpoint;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExecuteEndpointResponseDTO {
    private int statusCode;
    private boolean success;
    private String message;
    private long responseTimeMs;
    private Object data;
    private String rawResponse;
}
