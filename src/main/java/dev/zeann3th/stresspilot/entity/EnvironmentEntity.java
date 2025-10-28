package dev.zeann3th.stresspilot.entity;

import jakarta.persistence.*;
import lombok.*;

@EqualsAndHashCode(callSuper = false)
@Data
@Entity
@Table(name = "environments")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EnvironmentEntity extends BaseEntity{
    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
}
