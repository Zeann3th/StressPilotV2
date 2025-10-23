package dev.zeann3th.stresspilot.repository;

import dev.zeann3th.stresspilot.entity.FlowStepEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FlowStepRepository extends JpaRepository<FlowStepEntity, Long> {
}
