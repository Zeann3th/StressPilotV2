package dev.zeann3th.stresspilot.controller;

import dev.zeann3th.stresspilot.dto.config.ConfigDTO;
import dev.zeann3th.stresspilot.service.ConfigService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/config")
@RequiredArgsConstructor
@SuppressWarnings("unused")
public class ConfigController {
    private final ConfigService configService;

    @GetMapping
    public ResponseEntity<Map<String, String>> getAllConfigs() {
        var resp = configService.getAllConfigs();
        return ResponseEntity.ok(resp);
    }

    @GetMapping("/value")
    public ResponseEntity<String> getConfigValue(@RequestParam("key") String key) {
        var resp = configService.getValue(key);
        return resp.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Void> setConfigValue(@Valid @RequestBody ConfigDTO configDTO) {
        configService.setValue(configDTO.getKey(), configDTO.getValue());
        return ResponseEntity.ok().build();
    }
}
