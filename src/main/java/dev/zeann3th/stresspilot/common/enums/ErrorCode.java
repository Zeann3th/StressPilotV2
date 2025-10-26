package dev.zeann3th.stresspilot.common.enums;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {

    BAD_REQUEST(HttpStatus.BAD_REQUEST, "Bad request"),
    PROJECT_NOT_FOUND(HttpStatus.NOT_FOUND, "Project not found"),
    FLOW_NOT_FOUND(HttpStatus.NOT_FOUND, "Flow not found"),
    ENDPOINT_NOT_FOUND(HttpStatus.NOT_FOUND, "Endpoint not found"),
    ENDPOINT_PARSE_ERROR(HttpStatus.BAD_REQUEST, "Error parsing endpoints from specification"),
    ENDPOINT_UNSUPPORTED_FORMAT(HttpStatus.BAD_REQUEST, "Unsupported endpoint specification format"),
    SYSTEM_BUSY(HttpStatus.INTERNAL_SERVER_ERROR, "System is busy, please try again later");

    private final HttpStatus status;
    private final String message;

    ErrorCode(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }
}