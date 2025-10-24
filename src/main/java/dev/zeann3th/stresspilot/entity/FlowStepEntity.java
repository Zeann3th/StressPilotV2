package dev.zeann3th.stresspilot.entity;

import jakarta.persistence.*;
import lombok.*;

@EqualsAndHashCode(callSuper = false)
@Data
@Entity
@Table(name = "flow_steps")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FlowStepEntity extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "flow_id", nullable = false)
    private Long flowId;

    @Column(name = "type", columnDefinition = "VARCHAR(10)", nullable = false)
    private String type;

    @Column(name = "endpoint_id")
    private Long endpointId;

    @Column(name = "pre_processor", columnDefinition = "TEXT")
    private String preProcessor;

    @Column(name = "post_processor", columnDefinition = "TEXT")
    private String postProcessor;

    @Column(name = "next_step_if_true")
    private Long nextStepIfTrue;

    @Column(name = "next_step_if_false")
    private Long nextStepIfFalse;
}
