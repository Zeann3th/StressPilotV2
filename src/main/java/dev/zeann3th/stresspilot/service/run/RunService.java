package dev.zeann3th.stresspilot.service.run;

import dev.zeann3th.stresspilot.entity.RunEntity;
import jakarta.servlet.http.HttpServletResponse;

import java.util.List;

public interface RunService {
    List<RunEntity> getAllRuns();

    void exportRun(HttpServletResponse response, Long runId, String type);
}
