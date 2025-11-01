package dev.zeann3th.stresspilot.dto.endpoint;

import dev.zeann3th.stresspilot.common.validators.ValidEndpointRequest;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ValidEndpointRequest
public class EndpointDTO {
    private Long id;
    @NotNull
    private String name;
    private String description;
    @NotNull
    private String type;
    @NotNull
    private String url;

    // HTTP
    private String httpMethod;
    private Map<String, Object> httpHeaders;
    private Object httpBody;
    private Map<String, Object> httpParameters;

    // gRPC
    private String grpcServiceName;
    private String grpcMethodName;
    private String grpcStubPath;

    // GraphQL
    private String graphqlOperationType;
    private Map<String, Object> graphqlVariables;

    @NotNull
    private Long projectId;
}