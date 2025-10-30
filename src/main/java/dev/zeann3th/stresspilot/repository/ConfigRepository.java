package dev.zeann3th.stresspilot.repository;

import dev.zeann3th.stresspilot.entity.ConfigEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ConfigRepository extends JpaRepository<ConfigEntity, Long> {
    ConfigEntity findByKey(String key);
}
