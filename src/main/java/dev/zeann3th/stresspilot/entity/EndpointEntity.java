package dev.zeann3th.stresspilot.entity;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "endpoints")
public class EndpointEntity extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description")
    private String description;

    @Column(name = "method", nullable = false)
    private String method;

    @Column(name = "url", nullable = false)
    private String url;

    @Column(name = "headers", columnDefinition = "TEXT")
    private String headers;

    @Column(name = "body", columnDefinition = "TEXT")
    private String body;

    @Column(name = "parameters", columnDefinition = "TEXT")
    private String parameters;

    @Column(name = "project_id", nullable = false)
    private Long projectId;
}
