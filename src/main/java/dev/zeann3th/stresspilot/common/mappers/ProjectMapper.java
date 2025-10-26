package dev.zeann3th.stresspilot.common.mappers;

import dev.zeann3th.stresspilot.dto.project.ProjectDTO;
import dev.zeann3th.stresspilot.entity.ProjectEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ProjectMapper {
    ProjectDTO toDTO(ProjectEntity projectEntity);
}
