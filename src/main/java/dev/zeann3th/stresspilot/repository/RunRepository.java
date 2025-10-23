package dev.zeann3th.stresspilot.repository;

import dev.zeann3th.stresspilot.entity.RunEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RunRepository extends JpaRepository<RunEntity, Long> {
}
