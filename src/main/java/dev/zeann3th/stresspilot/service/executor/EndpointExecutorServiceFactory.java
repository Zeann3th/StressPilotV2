package dev.zeann3th.stresspilot.service.executor;

import dev.zeann3th.stresspilot.common.enums.ErrorCode;
import dev.zeann3th.stresspilot.exception.CommandExceptionBuilder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class EndpointExecutorServiceFactory {
    private final List<EndpointExecutorService> executors;

    public EndpointExecutorService getExecutor(String type) {
        return executors.stream()
                .filter(executor -> executor.getType().equalsIgnoreCase(type))
                .findFirst()
                .orElseThrow(() -> CommandExceptionBuilder.exception(ErrorCode.EXECUTOR_UNSUPPORTED_TYPE));
    }
}
