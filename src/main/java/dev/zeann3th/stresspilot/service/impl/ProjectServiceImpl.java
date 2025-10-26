package dev.zeann3th.stresspilot.service.impl;

import dev.zeann3th.stresspilot.common.enums.ErrorCode;
import dev.zeann3th.stresspilot.common.mappers.ProjectMapper;
import dev.zeann3th.stresspilot.dto.project.ProjectRequestDTO;
import dev.zeann3th.stresspilot.dto.project.ProjectDTO;
import dev.zeann3th.stresspilot.entity.ProjectEntity;
import dev.zeann3th.stresspilot.exception.CommandExceptionBuilder;
import dev.zeann3th.stresspilot.repository.ProjectRepository;
import dev.zeann3th.stresspilot.service.ProjectService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProjectServiceImpl implements ProjectService {
    private final ProjectRepository projectRepository;
    private final ProjectMapper projectMapper;

    @Override
    public ResponseEntity<Page<ProjectDTO>> getListProject(String name, Pageable pageable) {
            Page<ProjectEntity> projectPage = projectRepository.findAllByCondition(name, pageable);
            return ResponseEntity.ok(projectPage.map(projectMapper::toDTO));
    }

    @Override
    public ResponseEntity<ProjectDTO> getProjectDetail(Long projectId) {
        ProjectEntity project = projectRepository.findById(projectId)
                .orElseThrow(() -> CommandExceptionBuilder.exception(ErrorCode.PROJECT_NOT_FOUND));
        return ResponseEntity.ok(projectMapper.toDTO(project));
    }

    @Override
    public ResponseEntity<ProjectDTO> createProject(ProjectRequestDTO projectRequestDTO) {
        ProjectEntity project = ProjectEntity.builder()
                .name(projectRequestDTO.getName())
                .description(projectRequestDTO.getDescription())
                .build();
        ProjectEntity saved = projectRepository.save(project);
        return ResponseEntity.status(HttpStatus.CREATED).body(projectMapper.toDTO(saved));
    }

    @Override
    public ResponseEntity<ProjectDTO> updateProject(Long projectId, ProjectRequestDTO projectRequestDTO) {
        ProjectEntity project = projectRepository.findById(projectId)
                .orElseThrow(() -> CommandExceptionBuilder.exception(ErrorCode.PROJECT_NOT_FOUND));

        Optional.ofNullable(projectRequestDTO.getName()).ifPresent(project::setName);
        Optional.ofNullable(projectRequestDTO.getDescription()).ifPresent(project::setDescription);
        ProjectEntity saved = projectRepository.save(project);

        return ResponseEntity.ok(projectMapper.toDTO(saved));
    }

    @Override
    public ResponseEntity<Void> deleteProject(Long projectId) {
        boolean exists = projectRepository.existsById(projectId);
        if (!exists) {
            throw CommandExceptionBuilder.exception(ErrorCode.PROJECT_NOT_FOUND);
        }
        projectRepository.deleteById(projectId);
        return ResponseEntity.noContent().build();
    }
}
