package dev.zeann3th.stresspilot.controller;

import dev.zeann3th.stresspilot.dto.environment.EnvironmentVariableDTO;
import dev.zeann3th.stresspilot.dto.environment.UpdateEnvironmentRequestDTO;
import dev.zeann3th.stresspilot.service.environment.EnvironmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/environments")
@SuppressWarnings("unused")
@RequiredArgsConstructor
public class EnvironmentController {
    private final EnvironmentService environmentService;

    @GetMapping("/{environmentId}/variables")
    public ResponseEntity<List<EnvironmentVariableDTO>> getEnvironmentVariables(@PathVariable("environmentId") Long environmentId) {
        var resp = environmentService.getEnvironmentVariables(environmentId);
        return ResponseEntity.ok(resp);
    }

    @PatchMapping("/{environmentId}/variables")
    public ResponseEntity<Void> updateEnvironmentVariables(@PathVariable("environmentId") Long environmentId,
                                                           @RequestBody UpdateEnvironmentRequestDTO updateEnvironmentRequestDTO) {
        environmentService.updateEnvironmentVariables(environmentId, updateEnvironmentRequestDTO);
        return ResponseEntity.noContent().build();
    }
}
