package dev.zeann3th.stresspilot.dto.endpoint;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EndpointDTO {
    private Long id;
    private String name;
    private String description;
    private String method;
    private String url;
    private String headers;
    private String body;
    private String parameters;
    private Long projectId;
}

