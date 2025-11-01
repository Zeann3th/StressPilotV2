package dev.zeann3th.stresspilot.controller;

import dev.zeann3th.stresspilot.entity.RunEntity;
import dev.zeann3th.stresspilot.service.run.RunService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/runs")
@RequiredArgsConstructor
@SuppressWarnings("unused")
public class RunController {
    private final RunService runService;

    @GetMapping
    public ResponseEntity<List<RunEntity>> getAllRuns() {
        var resp = runService.getAllRuns();
        return ResponseEntity.ok().body(resp);
    }

    @GetMapping("/{runId}/export")
    public void exportRun(
            @PathVariable("runId") Long runId,
            @RequestParam(value = "type", required = false) String type,
            HttpServletResponse response
    ) {
        runService.exportRun(response, runId, type);
    }
}
