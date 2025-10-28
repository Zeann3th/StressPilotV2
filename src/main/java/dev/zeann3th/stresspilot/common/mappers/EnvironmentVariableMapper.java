package dev.zeann3th.stresspilot.common.mappers;

import dev.zeann3th.stresspilot.dto.environment.EnvironmentVariableDTO;
import dev.zeann3th.stresspilot.entity.EnvironmentVariableEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface EnvironmentVariableMapper {
    EnvironmentVariableDTO toDTO(EnvironmentVariableEntity entity);
}
