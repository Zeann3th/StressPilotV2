package dev.zeann3th.stresspilot.repository;

import dev.zeann3th.stresspilot.entity.FlowEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FlowRepository extends JpaRepository<FlowEntity, Long> {
}
