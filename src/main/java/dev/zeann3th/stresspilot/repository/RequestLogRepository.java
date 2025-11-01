package dev.zeann3th.stresspilot.repository;

import dev.zeann3th.stresspilot.entity.RequestLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RequestLogRepository extends JpaRepository<RequestLogEntity, Long> {
    List<RequestLogEntity> findByRunId(Long runId);
}
