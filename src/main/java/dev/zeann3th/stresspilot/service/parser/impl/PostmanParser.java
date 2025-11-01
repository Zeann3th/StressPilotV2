package dev.zeann3th.stresspilot.service.parser.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.zeann3th.stresspilot.common.enums.EndpointType;
import dev.zeann3th.stresspilot.common.enums.ErrorCode;
import dev.zeann3th.stresspilot.dto.endpoint.ParsedEndpointDTO;
import dev.zeann3th.stresspilot.exception.CommandExceptionBuilder;
import dev.zeann3th.stresspilot.service.parser.ParserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
@RequiredArgsConstructor
@SuppressWarnings("unused")
public class PostmanParser implements ParserService {

    private final ObjectMapper objectMapper;

    @Override
    public String getType() {
        return "postman";
    }

    @Override
    public List<ParsedEndpointDTO> parse(String spec) {
        List<ParsedEndpointDTO> endpoints = new ArrayList<>();
        try {
            JsonNode root = objectMapper.readTree(spec);
            JsonNode items = root.path("item");
            extractEndpoints(items, endpoints);
        } catch (Exception e) {
            throw CommandExceptionBuilder.exception(ErrorCode.ENDPOINT_PARSE_ERROR);
        }
        return endpoints;
    }

    private void extractEndpoints(JsonNode items, List<ParsedEndpointDTO> endpoints) {
        if (items.isArray()) {
            for (JsonNode item : items) {
                if (item.has("item")) {
                    // nested folders
                    extractEndpoints(item.get("item"), endpoints);
                } else if (item.has("request")) {
                    endpoints.add(createEndpoint(item));
                }
            }
        }
    }

    private ParsedEndpointDTO createEndpoint(JsonNode item) {
        JsonNode request = item.path("request");

        // Headers
        Map<String, String> headers = new HashMap<>();
        for (JsonNode header : request.path("header")) {
            headers.put(header.path("key").asText(), header.path("value").asText());
        }

        // Query parameters
        Map<String, Object> parameters = new HashMap<>();
        for (JsonNode param : request.path("url").path("query")) {
            parameters.put(param.path("key").asText(), param.path("value").asText());
        }
        Map<String, Object> templatedParameters = parameters.isEmpty() ? null : convertValuesToTemplate(parameters);

        // Body
        Map<String, Object> body = new HashMap<>();
        if ("raw".equals(request.path("body").path("mode").asText())) {
            try {
                String rawBody = request.path("body").path("raw").asText();
                if (!rawBody.isEmpty()) {
                    body = objectMapper.readValue(rawBody, new TypeReference<>() {});
                }
            } catch (Exception ignored) {
                // no-op
            }
        }
        Map<String, Object> templatedBody = body.isEmpty() ? null : convertValuesToTemplate(body);

        return ParsedEndpointDTO.builder()
                .name(item.path("name").asText())
                .description(item.path("description").asText(null))
                .type(EndpointType.HTTP.name())
                .httpMethod(request.path("method").asText())
                .url(request.path("url").path("raw").asText())
                .httpHeaders(headers.isEmpty() ? null : headers)
                .httpParameters(templatedParameters)
                .httpBody(templatedBody)
                .build();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> convertValuesToTemplate(Map<String, Object> input) {
        Map<String, Object> result = new HashMap<>();
        for (Map.Entry<String, Object> entry : input.entrySet()) {
            Object value = entry.getValue();
            String key = entry.getKey();

            if (value instanceof String || value instanceof Number) {
                result.put(key, "{{" + key + "}}");
            } else if (value instanceof Map) {
                result.put(key, convertValuesToTemplate((Map<String, Object>) value));
            } else if (value instanceof List) {
                result.put(key, convertListToTemplate((List<Object>) value, key));
            } else {
                result.put(key, value);
            }
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private List<Object> convertListToTemplate(List<Object> input, String parentKey) {
        List<Object> result = new ArrayList<>();
        for (int i = 0; i < input.size(); i++) {
            Object item = input.get(i);
            String indexedKey = parentKey + "_" + i;

            if (item instanceof String || item instanceof Number) {
                result.add("{{" + indexedKey + "}}");
            } else if (item instanceof Map) {
                result.add(convertValuesToTemplate((Map<String, Object>) item));
            } else if (item instanceof List) {
                result.add(convertListToTemplate((List<Object>) item, indexedKey));
            } else {
                result.add(item);
            }
        }
        return result;
    }
}