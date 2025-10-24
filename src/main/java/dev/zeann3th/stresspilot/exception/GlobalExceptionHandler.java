package dev.zeann3th.stresspilot.exception;

import dev.zeann3th.stresspilot.common.enums.ErrorCode;
import dev.zeann3th.stresspilot.dto.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
@SuppressWarnings("all")
public class GlobalExceptionHandler {
    @ExceptionHandler(CommandException.class)
    public ResponseEntity<Object> handleCommandException(CommandException ex) {
        ErrorCode code = ex.getErrorCode();
        ErrorResponse response = ErrorResponse.builder()
                .code(code.getCode())
                .message(code.getMessage())
                .build();
        return ResponseEntity.status(resolveHttpStatus(code)).body(response);
    }

    private HttpStatus resolveHttpStatus(ErrorCode code) {
        if (code.getCode() / 10000 == 4) return HttpStatus.BAD_REQUEST;
        if (code.getCode() / 10000 == 5) return HttpStatus.INTERNAL_SERVER_ERROR;
        return HttpStatus.OK;
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleException(Exception ex) {
        ErrorResponse response = ErrorResponse.builder()
                .code(ErrorCode.SYSTEM_BUSY.getCode())
                .message(ErrorCode.SYSTEM_BUSY.getMessage())
                .build();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}
