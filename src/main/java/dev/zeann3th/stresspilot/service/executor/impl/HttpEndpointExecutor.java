package dev.zeann3th.stresspilot.service.executor.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.zeann3th.stresspilot.common.enums.ConfigKey;
import dev.zeann3th.stresspilot.common.enums.EndpointType;
import dev.zeann3th.stresspilot.dto.endpoint.ExecuteEndpointResponseDTO;
import dev.zeann3th.stresspilot.entity.EndpointEntity;
import dev.zeann3th.stresspilot.service.ConfigService;
import dev.zeann3th.stresspilot.service.executor.EndpointExecutorService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Component
@Slf4j
@RequiredArgsConstructor
@SuppressWarnings("unused")
public class HttpEndpointExecutor implements EndpointExecutorService {
    private final ConfigService configService;
    private final ObjectMapper objectMapper;

    private OkHttpClient baseClient;

    @PostConstruct
    public void init() {
        log.info("Loading HTTP Executor configurations");
        int connectTimeout = configService.getValue(ConfigKey.HTTP_CONNECT_TIMEOUT.name()).map(Integer::parseInt).orElse(10);
        int readTimeout = configService.getValue(ConfigKey.HTTP_READ_TIMEOUT.name()).map(Integer::parseInt).orElse(30);
        int writeTimeout = configService.getValue(ConfigKey.HTTP_WRITE_TIMEOUT.name()).map(Integer::parseInt).orElse(30);
        int maxConnections = configService.getValue(ConfigKey.HTTP_MAX_POOL_SIZE.name()).map(Integer::parseInt).orElse(100);
        int keepAliveDuration = configService.getValue(ConfigKey.HTTP_KEEP_ALIVE_DURATION.name()).map(Integer::parseInt).orElse(5);

        baseClient = new OkHttpClient.Builder()
                .connectTimeout(connectTimeout, TimeUnit.SECONDS)
                .readTimeout(readTimeout, TimeUnit.SECONDS)
                .writeTimeout(writeTimeout, TimeUnit.SECONDS)
                .connectionPool(new ConnectionPool(maxConnections, keepAliveDuration, TimeUnit.MINUTES))
                .followRedirects(true)
                .build();

        log.info("HTTP Executor initialized with timeout: connect={}s, read={}s, write={}s",
                connectTimeout, readTimeout, writeTimeout);
    }

    @Override
    public String getType() {
        return EndpointType.HTTP.name();
    }

    @Override
    public ExecuteEndpointResponseDTO execute(EndpointEntity endpointEntity,
                                              Map<String, Object> environment,
                                              CookieJar cookieJar) {
        try {
            OkHttpClient client = cookieJar != null
                    ? baseClient.newBuilder().cookieJar(cookieJar).build()
                    : baseClient;

            Request request = buildRequest(endpointEntity, environment);

            long startTime = System.currentTimeMillis();
            try (Response response = client.newCall(request).execute()) {
                long responseTimeMs = System.currentTimeMillis() - startTime;

                String rawResponse = response.body() != null ? response.body().string() : "";

                return ExecuteEndpointResponseDTO.builder()
                        .statusCode(response.code())
                        .success(response.isSuccessful())
                        .message(response.message())
                        .responseTimeMs(responseTimeMs)
                        .data(parseResponseData(rawResponse))
                        .rawResponse(rawResponse)
                        .build();
            }

        } catch (IOException e) {
            log.error("Failed to execute HTTP request for endpoint: {}", endpointEntity.getName(), e);
            return ExecuteEndpointResponseDTO.builder()
                    .success(false)
                    .message("IO Error: " + e.getMessage())
                    .build();
        } catch (Exception e) {
            log.error("Unexpected error executing HTTP request", e);
            return ExecuteEndpointResponseDTO.builder()
                    .success(false)
                    .message("Unexpected error: " + e.getMessage())
                    .build();
        }
    }

    private Request buildRequest(EndpointEntity endpoint, Map<String, Object> environment) {
        String url = replaceVariables(endpoint.getUrl(), environment);

        log.debug("Building request - URL after variable replacement: {}", url);

        Map<String, String> headers = parseHeaders(endpoint.getHttpHeaders());

        Request.Builder builder = new Request.Builder().url(url);

        headers.forEach((key, value) -> {
            String replacedValue = replaceVariables(value, environment);
            builder.addHeader(key, replacedValue);
        });

        String method = endpoint.getHttpMethod().toUpperCase();
        RequestBody requestBody = null;

        if (endpoint.getHttpBody() != null && !endpoint.getHttpBody().isEmpty()) {
            String body = parseBody(endpoint.getHttpBody(), environment);
            log.debug("Request body after variable replacement: {}", body);
            MediaType mediaType = MediaType.parse("application/json; charset=utf-8");
            requestBody = RequestBody.create(body, mediaType);
        }

        switch (method) {
            case "GET":
                builder.get();
                break;
            case "POST":
                builder.post(requestBody != null ? requestBody : RequestBody.create("", null));
                break;
            case "PUT":
                builder.put(requestBody != null ? requestBody : RequestBody.create("", null));
                break;
            case "DELETE":
                builder.delete(requestBody);
                break;
            case "PATCH":
                builder.patch(requestBody != null ? requestBody : RequestBody.create("", null));
                break;
            case "HEAD":
                builder.head();
                break;
            case "OPTIONS":
                builder.method("OPTIONS", null);
                break;
            default:
                builder.method(method, requestBody);
        }

        return builder.build();
    }

    private Map<String, String> parseHeaders(String headersJson) {
        if (headersJson == null || headersJson.isEmpty()) {
            return new HashMap<>();
        }
        try {
            return objectMapper.readValue(headersJson, new TypeReference<>() {});
        } catch (Exception e) {
            log.warn("Failed to parse headers: {}", e.getMessage());
            return new HashMap<>();
        }
    }

    private String parseBody(String bodyJson, Map<String, Object> environment) {
        if (bodyJson == null || bodyJson.isEmpty()) {
            return "";
        }
        try {
            Map<String, Object> bodyMap = objectMapper.readValue(bodyJson, new TypeReference<>() {});
            Map<String, Object> replaced = replaceVariablesInMap(bodyMap, environment);
            return objectMapper.writeValueAsString(replaced);
        } catch (Exception e) {
            log.debug("Body is not valid JSON, treating as plain text");
            return replaceVariables(bodyJson, environment);
        }
    }

    private String replaceVariables(String input, Map<String, Object> environment) {
        if (input == null || environment == null || environment.isEmpty()) {
            return input;
        }
        String result = input;
        for (Map.Entry<String, Object> entry : environment.entrySet()) {
            String placeholder = "{{" + entry.getKey() + "}}";
            String value = entry.getValue() != null ? String.valueOf(entry.getValue()) : "";
            result = result.replace(placeholder, value);
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> replaceVariablesInMap(Map<String, Object> input, Map<String, Object> environment) {
        Map<String, Object> result = new HashMap<>();
        for (Map.Entry<String, Object> entry : input.entrySet()) {
            Object value = entry.getValue();
            switch (value) {
                case String valueStr -> result.put(entry.getKey(), replaceVariables(valueStr, environment));
                case Map<?,?> map -> result.put(entry.getKey(), replaceVariablesInMap((Map<String, Object>) value, environment));
                case List<?> list -> result.put(entry.getKey(), replaceVariablesInList(list, environment));
                case null, default -> result.put(entry.getKey(), value);
            }
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private List<Object> replaceVariablesInList(List<?> input, Map<String, Object> environment) {
        List<Object> result = new java.util.ArrayList<>();
        for (Object item : input) {
            switch (item) {
                case String itemStr -> result.add(replaceVariables(itemStr, environment));
                case Map<?,?> map -> result.add(replaceVariablesInMap((Map<String, Object>) item, environment));
                case List<?> list -> result.add(replaceVariablesInList(list, environment));
                case null, default -> result.add(item);
            }
        }
        return result;
    }

    private Object parseResponseData(String rawResponse) {
        if (rawResponse == null || rawResponse.isEmpty()) {
            return Map.of();
        }
        try {
            if (rawResponse.trim().startsWith("{")) {
                return objectMapper.readValue(rawResponse, new TypeReference<Map<String, Object>>() {});
            } else if (rawResponse.trim().startsWith("[")) {
                return objectMapper.readValue(rawResponse, new TypeReference<List<Object>>() {});
            } else {
                return rawResponse;
            }
        } catch (Exception e) {
            log.debug("Failed to parse response JSON", e);
            return rawResponse;
        }
    }
}