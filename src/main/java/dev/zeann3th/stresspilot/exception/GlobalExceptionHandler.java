package dev.zeann3th.stresspilot.exception;

import dev.zeann3th.stresspilot.common.enums.ErrorCode;
import dev.zeann3th.stresspilot.dto.ErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.stream.Collectors;

@Slf4j
@ControllerAdvice
@SuppressWarnings("all")
public class GlobalExceptionHandler {

    @ExceptionHandler(CommandException.class)
    public ResponseEntity<Object> handleCommandException(CommandException ex) {
        ErrorCode error = ex.getErrorCode();
        ErrorResponse response = ErrorResponse.builder()
                .status(error.getStatus().value())
                .message(error.getMessage())
                .params(ex.getParams())
                .build();
        return ResponseEntity.status(error.getStatus()).body(response);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Object> handleValidationException(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(err -> err.getField() + ": " + err.getDefaultMessage())
                .collect(Collectors.joining("; "));

        ErrorResponse response = ErrorResponse.builder()
                .status(400)
                .message(message)
                .build();

        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleUnhandledException(Exception ex) {
        log.error("Unhandled exception occurred", ex);
        ErrorResponse response = ErrorResponse.builder()
                .status(ErrorCode.SYSTEM_BUSY.getStatus().value())
                .message(ErrorCode.SYSTEM_BUSY.getMessage())
                .build();
        return ResponseEntity.status(ErrorCode.SYSTEM_BUSY.getStatus()).body(response);
    }
}