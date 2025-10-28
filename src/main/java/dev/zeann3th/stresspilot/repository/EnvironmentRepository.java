package dev.zeann3th.stresspilot.repository;

import dev.zeann3th.stresspilot.entity.EnvironmentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EnvironmentRepository extends JpaRepository<EnvironmentEntity, Long> {

}
