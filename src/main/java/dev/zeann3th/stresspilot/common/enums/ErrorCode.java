package dev.zeann3th.stresspilot.common.enums;

import lombok.Getter;

@Getter
public enum ErrorCode {
    PROJECT_NOT_FOUND(40000, "Project not found"),
    SYSTEM_BUSY(50000, "System is busy, please try again later");

    private final int code;
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
