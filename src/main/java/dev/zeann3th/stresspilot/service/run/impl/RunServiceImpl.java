package dev.zeann3th.stresspilot.service.run.impl;

import dev.zeann3th.stresspilot.common.Constants;
import dev.zeann3th.stresspilot.common.enums.ErrorCode;
import dev.zeann3th.stresspilot.common.enums.ReportType;
import dev.zeann3th.stresspilot.entity.RequestLogEntity;
import dev.zeann3th.stresspilot.entity.RunEntity;
import dev.zeann3th.stresspilot.exception.CommandExceptionBuilder;
import dev.zeann3th.stresspilot.repository.RequestLogRepository;
import dev.zeann3th.stresspilot.repository.RunRepository;
import dev.zeann3th.stresspilot.service.report.ExcelGenerator;
import dev.zeann3th.stresspilot.service.run.RunService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Slf4j(topic = "[Run Service]")
@Service
@RequiredArgsConstructor
public class RunServiceImpl implements RunService {
    private final RunRepository runRepository;
    private final RequestLogRepository requestLogRepository;

    @Override
    public List<RunEntity> getAllRuns() {
        return runRepository.findAll();
    }

    @Override
    public void exportRun(HttpServletResponse response, Long runId, String type) {
        boolean isExists = runRepository.existsById(runId);
        if (!isExists) {
            throw CommandExceptionBuilder.exception(ErrorCode.RUN_NOT_FOUND);
        }

        String now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String rawFileName;
        String encodedFileName;

        if (type == null || type.isEmpty()) {
            type = ReportType.DETAILED.name();
        }

        if (ReportType.DETAILED.name().equals(type)) {
            rawFileName = "[Stress Pilot] Detailed report of run " + runId + "_" + now + ".xlsx";
            encodedFileName = URLEncoder.encode(rawFileName, StandardCharsets.UTF_8);
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        } else if (ReportType.SUMMARY.name().equals(type)) {
            rawFileName = "[Stress Pilot] Summary report of run " + runId + "_" + now + ".pdf";
            encodedFileName = URLEncoder.encode(rawFileName, StandardCharsets.UTF_8);
            response.setContentType("application/pdf");
        } else {
            throw CommandExceptionBuilder.exception(ErrorCode.BAD_REQUEST,
                    Map.of(Constants.REASON, "Unsupported report type: " + type));
        }

        String contentDisposition = "attachment; filename=\"" + rawFileName + "\"; filename*=UTF-8''" + encodedFileName;
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, contentDisposition);

        try {
            List<RequestLogEntity> logEntities = requestLogRepository.findByRunId(runId);

            if (ReportType.DETAILED.name().equals(type)) {
                String[] headers = {"ID", "Endpoint ID", "Status", "Response Time (ms)", "Request", "Response"};
                String[] fields = {"id", "endpointId", "statusCode", "responseTime", "request", "response"};

                new ExcelGenerator<>(logEntities)
                        .writeHeaderLines(headers)
                        .writeDataLines(fields)
                        .export(response);
            } else {
                // PDF logic later
            }
        } catch (Exception e) {
            log.error("Error exporting report", e);
        }
    }
}
