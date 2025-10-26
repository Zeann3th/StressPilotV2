package dev.zeann3th.stresspilot.service;

import dev.zeann3th.stresspilot.dto.project.ProjectRequestDTO;
import dev.zeann3th.stresspilot.dto.project.ProjectDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;

public interface ProjectService {
    ResponseEntity<Page<ProjectDTO>> getListProject(String name, Pageable pageable);

    ResponseEntity<ProjectDTO> getProjectDetail(Long projectId);

    ResponseEntity<ProjectDTO> createProject(ProjectRequestDTO projectRequestDTO);

    ResponseEntity<ProjectDTO> updateProject(Long projectId, ProjectRequestDTO projectRequestDTO);

    ResponseEntity<Void> deleteProject(Long projectId);
}
