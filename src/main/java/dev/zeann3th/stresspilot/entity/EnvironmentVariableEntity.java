package dev.zeann3th.stresspilot.entity;

import jakarta.persistence.*;
import lombok.*;

@EqualsAndHashCode(callSuper = false)
@Data
@Entity
@Table(
    name = "environment_variables",
    uniqueConstraints = {
            @UniqueConstraint(columnNames = {"environment_id", "key"})
    })
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EnvironmentVariableEntity extends BaseEntity {
    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "environment_id", nullable = false)
    private Long environmentId;

    @Column(name = "key", nullable = false)
    private String key;

    @Column(name = "value", columnDefinition = "TEXT")
    private String value;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive;
}
