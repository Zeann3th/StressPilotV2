package dev.zeann3th.stresspilot.service.environment;

import dev.zeann3th.stresspilot.dto.environment.EnvironmentVariableDTO;
import dev.zeann3th.stresspilot.dto.environment.UpdateEnvironmentRequestDTO;

import java.util.List;

public interface EnvironmentService {
    List<EnvironmentVariableDTO> getEnvironmentVariables(Long environmentId);

    void updateEnvironmentVariables(Long environmentId, UpdateEnvironmentRequestDTO updateEnvironmentRequestDTO);
}
