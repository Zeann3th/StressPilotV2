package dev.zeann3th.stresspilot.service;

import dev.zeann3th.stresspilot.entity.ConfigEntity;
import dev.zeann3th.stresspilot.repository.ConfigRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ConfigService {
    private final ConfigRepository configRepository;

    public Map<String, String> getAllConfigs() {
        return configRepository.findAll().stream()
                .collect(
                        java.util.stream.Collectors.toMap(
                                ConfigEntity::getKey,
                                ConfigEntity::getValue
                        )
                );
    }

    public Optional<String> getValue(String key) {
        return Optional.ofNullable(configRepository.findByKey(key))
                .map(ConfigEntity::getValue);
    }

    public void setValue(String key, String value) {
        ConfigEntity configEntity = configRepository.findByKey(key);
        if (configEntity == null) {
            configEntity = ConfigEntity.builder()
                    .key(key)
                    .value(value)
                    .build();
        } else {
            configEntity.setValue(value);
        }
        configRepository.save(configEntity);
    }
}
