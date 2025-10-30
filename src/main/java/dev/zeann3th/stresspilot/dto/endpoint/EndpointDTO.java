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
    private String type;

    // HTTP
    private String httpMethod;
    private String httpUrl;
    private String httpHeaders;
    private String httpBody;
    private String httpParameters;

    // gRPC
    private String grpcServiceName;
    private String grpcMethodName;
    private String grpcProtoFile;

    // GraphQL
    private String graphqlOperationType;
    private String graphqlVariables;

    private Long projectId;
}