package dev.zeann3th.stresspilot.common.mappers;

import dev.zeann3th.stresspilot.dto.endpoint.EndpointDTO;
import dev.zeann3th.stresspilot.entity.EndpointEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface EndpointMapper {
    @Mapping(target = "description", ignore = true)
    EndpointDTO toListDTO(EndpointEntity endpointDTO);

    EndpointDTO toDetailDTO(EndpointEntity endpointDTO);
}
