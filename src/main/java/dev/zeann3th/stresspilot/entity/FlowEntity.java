package dev.zeann3th.stresspilot.entity;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "flows")
public class FlowEntity extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description")
    private String description;
}
