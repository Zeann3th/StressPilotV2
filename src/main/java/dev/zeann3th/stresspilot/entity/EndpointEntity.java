package dev.zeann3th.stresspilot.entity;

import jakarta.persistence.*;
import lombok.*;

@EqualsAndHashCode(callSuper = false)
@Data
@Entity
@Table(name = "endpoints")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EndpointEntity extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    // Metadata
    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "type", columnDefinition = "VARCHAR(20)", nullable = false)
    private String type;

    @Column(name = "project_id", nullable = false)
    private Long projectId;

    @Column(name = "url")
    private String url;

    // Http
    @Column(name = "http_method", columnDefinition = "VARCHAR(10)")
    private String httpMethod;

    @Column(name = "http_headers", columnDefinition = "TEXT")
    private String httpHeaders;

    @Column(name = "http_body", columnDefinition = "TEXT")
    private String httpBody;

    @Column(name = "http_parameters", columnDefinition = "TEXT")
    private String httpParameters;

    // gRPC
    @Column(name = "grpc_service_name")
    private String grpcServiceName;

    @Column(name = "grpc_method_name")
    private String grpcMethodName;

    @Column(name = "grpc_stub_path", columnDefinition = "TEXT")
    private String grpcStubPath;

    // GraphQL
    @Column(name = "graphql_operation_type", columnDefinition = "VARCHAR(20)")
    private String graphqlOperationType;

    @Column(name = "graphql_variables", columnDefinition = "TEXT")
    private String graphqlVariables;
}