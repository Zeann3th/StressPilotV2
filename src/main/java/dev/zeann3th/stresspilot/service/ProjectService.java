package dev.zeann3th.stresspilot.service;

import dev.zeann3th.stresspilot.dto.project.ProjectDTO;
import dev.zeann3th.stresspilot.dto.project.GetProjectDetailDTO;
import dev.zeann3th.stresspilot.entity.ProjectEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;

public interface ProjectService {
    ResponseEntity<Page<GetProjectDetailDTO>> getListProject(String name, Pageable pageable);

    ResponseEntity<ProjectEntity> getProjectDetail(Long projectId);

    ResponseEntity<ProjectEntity> createProject(ProjectDTO projectDTO);

    ResponseEntity<ProjectEntity> updateProject(Long projectId, ProjectDTO projectDTO);

    ResponseEntity<Void> deleteProject(Long projectId);
}
