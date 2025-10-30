package dev.zeann3th.stresspilot.service;

import dev.zeann3th.stresspilot.entity.ConfigEntity;
import dev.zeann3th.stresspilot.repository.ConfigRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ConfigService {
    private final ConfigRepository configRepository;

    public Optional<String> getValue(String key) {
        return Optional.ofNullable(configRepository.findByKey(key))
                .map(ConfigEntity::getValue);
    }
}
