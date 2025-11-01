package dev.zeann3th.stresspilot.common.mappers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.zeann3th.stresspilot.common.Constants;
import dev.zeann3th.stresspilot.common.enums.ErrorCode;
import dev.zeann3th.stresspilot.dto.endpoint.EndpointDTO;
import dev.zeann3th.stresspilot.entity.EndpointEntity;
import java.util.Collections;

import dev.zeann3th.stresspilot.exception.CommandExceptionBuilder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.Map;

@Mapper(componentModel = "spring")
public interface EndpointMapper {

    ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Mapping(target = "description", ignore = true)
    @Mapping(source = "httpHeaders", target = "httpHeaders", qualifiedByName = "jsonToMap")
    @Mapping(source = "httpBody", target = "httpBody", qualifiedByName = "jsonToObject")
    @Mapping(source = "httpParameters", target = "httpParameters", qualifiedByName = "jsonToMap")
    @Mapping(source = "graphqlVariables", target = "graphqlVariables", qualifiedByName = "jsonToMap")
    EndpointDTO toListDTO(EndpointEntity endpointEntity);

    @Mapping(source = "httpHeaders", target = "httpHeaders", qualifiedByName = "jsonToMap")
    @Mapping(source = "httpBody", target = "httpBody", qualifiedByName = "jsonToObject")
    @Mapping(source = "httpParameters", target = "httpParameters", qualifiedByName = "jsonToMap")
    @Mapping(source = "graphqlVariables", target = "graphqlVariables", qualifiedByName = "jsonToMap")
    EndpointDTO toDetailDTO(EndpointEntity endpointEntity);

    @Named("jsonToMap")
    static Map<String, Object> jsonToMap(String json) {
        if (json == null || json.isBlank()) return Collections.emptyMap();
        try {
            return OBJECT_MAPPER.readValue(json, new TypeReference<>() {
            });
        } catch (JsonProcessingException e) {
            throw CommandExceptionBuilder.exception(ErrorCode.SYSTEM_BUSY,
                    Map.of(Constants.REASON, "Failed to parse JSON to Map: " + json));
        }
    }

    @Named("jsonToObject")
    static Object jsonToObject(String json) {
        if (json == null || json.isBlank()) return null;
        try {
            return OBJECT_MAPPER.readValue(json, Object.class);
        } catch (JsonProcessingException e) {
            throw CommandExceptionBuilder.exception(ErrorCode.SYSTEM_BUSY,
                    Map.of(Constants.REASON, "Failed to parse JSON to Object: " + json));
        }
    }
}

