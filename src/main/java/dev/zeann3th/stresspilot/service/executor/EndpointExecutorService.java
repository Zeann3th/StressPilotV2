package dev.zeann3th.stresspilot.service.executor;

import dev.zeann3th.stresspilot.dto.endpoint.ExecuteEndpointResponseDTO;
import dev.zeann3th.stresspilot.entity.EndpointEntity;
import okhttp3.CookieJar;

import java.util.Map;

public interface EndpointExecutorService {
    String getType();

    ExecuteEndpointResponseDTO execute(EndpointEntity endpointEntity, Map<String, Object> environment, CookieJar cookieJar);
}
