package dev.zeann3th.stresspilot.exception;

import dev.zeann3th.stresspilot.common.enums.ErrorCode;

public class CommandExceptionBuilder {
    public static CommandException exception(ErrorCode errorCode) {
        return new CommandException(errorCode);
    }

    private CommandExceptionBuilder() {
    }
}
