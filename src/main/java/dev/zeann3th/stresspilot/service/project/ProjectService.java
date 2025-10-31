package dev.zeann3th.stresspilot.service.project;

import dev.zeann3th.stresspilot.dto.project.ProjectDTO;
import dev.zeann3th.stresspilot.dto.project.ProjectRequestDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ProjectService {
    Page<ProjectDTO> getListProject(String name, Pageable pageable);

    ProjectDTO getProjectDetail(Long projectId);

    ProjectDTO createProject(ProjectRequestDTO projectRequestDTO);

    ProjectDTO updateProject(Long projectId, ProjectRequestDTO projectRequestDTO);

    void deleteProject(Long projectId);
}
