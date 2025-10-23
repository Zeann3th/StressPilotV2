package dev.zeann3th.stresspilot.entity;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "runs")
public class RunEntity extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "flow_id", nullable = false)
    private Long flowId;

    @Column(name = "status", nullable = false)
    private String status;

    @Column(name = "threads", nullable = false)
    private Integer threads;

    @Column(name = "duration", nullable = false)
    private Integer duration;

    @Column(name = "ramp_up_duration", nullable = false)
    private Integer rampUpDuration;
}
