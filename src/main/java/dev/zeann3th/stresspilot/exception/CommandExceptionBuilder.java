package dev.zeann3th.stresspilot.exception;

import dev.zeann3th.stresspilot.common.enums.ErrorCode;

import java.io.Serializable;
import java.util.Map;

public class CommandExceptionBuilder {
    public static CommandException exception(ErrorCode errorCode) {
        return new CommandException(errorCode);
    }

    public static CommandException exception(ErrorCode errorCode, Map<String, Serializable> params) {
        return new CommandException(errorCode, params);
    }

    private CommandExceptionBuilder() {
    }
}
