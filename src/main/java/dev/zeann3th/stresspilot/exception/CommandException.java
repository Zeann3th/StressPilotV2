package dev.zeann3th.stresspilot.exception;

import dev.zeann3th.stresspilot.common.enums.ErrorCode;
import lombok.Getter;

@Getter
public class CommandException extends RuntimeException {
    private final ErrorCode errorCode;

    public CommandException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
}
