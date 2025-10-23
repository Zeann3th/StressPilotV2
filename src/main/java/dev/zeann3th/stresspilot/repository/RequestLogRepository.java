package dev.zeann3th.stresspilot.repository;

import dev.zeann3th.stresspilot.entity.RequestLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RequestLogRepository extends JpaRepository<RequestLogEntity, Long> {
}
