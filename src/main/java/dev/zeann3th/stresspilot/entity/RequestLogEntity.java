package dev.zeann3th.stresspilot.entity;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "request_logs")
public class RequestLogEntity extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "run_id", nullable = false)
    private Long runId;

    @Column(name = "endpoint_id", nullable = false)
    private Long endpointId;

    @Column(name = "status_code", nullable = false)
    private Integer statusCode;

    @Column(name = "response_time", nullable = false)
    private Long responseTime;

    @Column(name = "request", columnDefinition = "TEXT")
    private String request;

    @Column(name = "response", columnDefinition = "TEXT")
    private String response;
}
