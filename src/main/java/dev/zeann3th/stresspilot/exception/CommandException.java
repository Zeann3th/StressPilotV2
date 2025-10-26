package dev.zeann3th.stresspilot.exception;

import dev.zeann3th.stresspilot.common.enums.ErrorCode;
import lombok.Getter;
import org.springframework.lang.Nullable;

import java.util.Collections;
import java.util.Map;

import java.io.Serializable;

@Getter
public class CommandException extends RuntimeException {
    private final ErrorCode errorCode;
    private final Map<String, Serializable> params;

    public CommandException(ErrorCode errorCode) {
        this(errorCode, null);
    }

    public CommandException(ErrorCode errorCode, @Nullable Map<String, Serializable> params) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
        this.params = (params != null) ? params : Collections.emptyMap();
    }
}