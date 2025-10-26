package dev.zeann3th.stresspilot.common.mappers;

import dev.zeann3th.stresspilot.dto.flow.FlowResponseDTO;
import dev.zeann3th.stresspilot.entity.FlowEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface FlowMapper {
    @Mapping(target = "description", ignore = true)
    FlowResponseDTO toListDTO(FlowEntity flowEntity);

    FlowResponseDTO toDetailDTO(FlowEntity flowEntity);
}
