package dev.zeann3th.stresspilot.dto.endpoint;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ParsedEndpointDTO {
    private String name;
    private String description;
    private String method;
    private String url;
    private Map<String, String> headers;
    private Map<String, Object> body;
    private Map<String, Object> parameters;
}
