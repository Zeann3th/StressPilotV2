package dev.zeann3th.stresspilot.service.executor;

import dev.zeann3th.stresspilot.common.enums.EndpointType;
import dev.zeann3th.stresspilot.dto.endpoint.ExecuteEndpointResponseDTO;
import dev.zeann3th.stresspilot.entity.EndpointEntity;

import java.util.Map;

public interface ExecutorService {
    EndpointType getType();
    ExecuteEndpointResponseDTO execute(EndpointEntity endpointEntity, Map<String, Object> environment);
}
