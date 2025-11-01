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
    private String type;
    private String url;

    // Http
    private String httpMethod;
    private Map<String, String> httpHeaders;
    private Map<String, Object> httpBody;
    private Map<String, Object> httpParameters;

    // gRPC
    private String grpcServiceName;
    private String grpcMethodName;
    private String grpcStubPath;

    // GraphQL
    private String graphqlOperationType;
    private Map<String, Object> graphqlVariables;
}