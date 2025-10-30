package dev.zeann3th.stresspilot.repository;

import dev.zeann3th.stresspilot.entity.EnvironmentVariableEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EnvironmentVariableRepository extends JpaRepository<EnvironmentVariableEntity, Long> {
    List<EnvironmentVariableEntity> findAllByEnvironmentId(Long environmentId);

    List<EnvironmentVariableEntity> findAllByEnvironmentIdAndIsActiveTrue(Long environmentId);
}
