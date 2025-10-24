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

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "method", columnDefinition = "VARCHAR(10)", nullable = false)
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
