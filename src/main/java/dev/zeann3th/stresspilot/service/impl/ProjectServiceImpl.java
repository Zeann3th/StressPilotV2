package dev.zeann3th.stresspilot.service.impl;

import dev.zeann3th.stresspilot.common.enums.ErrorCode;
import dev.zeann3th.stresspilot.dto.project.ProjectDTO;
import dev.zeann3th.stresspilot.dto.project.GetProjectDetailDTO;
import dev.zeann3th.stresspilot.entity.ProjectEntity;
import dev.zeann3th.stresspilot.exception.CommandExceptionBuilder;
import dev.zeann3th.stresspilot.repository.ProjectRepository;
import dev.zeann3th.stresspilot.service.ProjectService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProjectServiceImpl implements ProjectService {
    private final ProjectRepository projectRepository;

    @Override
    public ResponseEntity<Page<GetProjectDetailDTO>> getListProject(String name, Pageable pageable) {
        try {
            Page<ProjectEntity> projectPage = projectRepository.findAllByCondition(name, pageable);

            Page<GetProjectDetailDTO> dtoPage = projectPage.map(project -> {
                String description = project.getDescription();
                if (description != null && description.length() > 100) {
                    description = description.substring(0, 100) + "...";
                }
                return GetProjectDetailDTO.builder()
                        .id(project.getId())
                        .name(project.getName())
                        .description(description)
                        .createdAt(project.getCreatedAt())
                        .updatedAt(project.getUpdatedAt())
                        .build();
            });
            return ResponseEntity.ok(dtoPage);
        } catch (Exception e) {
            log.error("Error fetching project list", e);
            throw CommandExceptionBuilder.exception(ErrorCode.SYSTEM_BUSY);
        }
    }

    @Override
    public ResponseEntity<ProjectEntity> getProjectDetail(Long projectId) {
        ProjectEntity project = projectRepository.findById(projectId)
                .orElseThrow(() -> CommandExceptionBuilder.exception(ErrorCode.PROJECT_NOT_FOUND));

        return ResponseEntity.ok(project);
    }

    @Override
    public ResponseEntity<ProjectEntity> createProject(ProjectDTO projectDTO) {
        ProjectEntity project = ProjectEntity.builder()
                .name(projectDTO.getName())
                .description(projectDTO.getDescription())
                .build();
        projectRepository.save(project);
        return ResponseEntity.ok(project);
    }

    @Override
    public ResponseEntity<ProjectEntity> updateProject(Long projectId, ProjectDTO projectDTO) {
        ProjectEntity project = projectRepository.findById(projectId)
                .orElseThrow(() -> CommandExceptionBuilder.exception(ErrorCode.PROJECT_NOT_FOUND));

        Optional.ofNullable(projectDTO.getName()).ifPresent(project::setName);
        Optional.ofNullable(projectDTO.getDescription()).ifPresent(project::setDescription);
        projectRepository.save(project);

        return ResponseEntity.ok(project);
    }

    @Override
    public ResponseEntity<Void> deleteProject(Long projectId) {
        ProjectEntity project = projectRepository.findById(projectId)
                .orElseThrow(() -> CommandExceptionBuilder.exception(ErrorCode.PROJECT_NOT_FOUND));
        projectRepository.delete(project);
        return ResponseEntity.noContent().build();
    }
}
