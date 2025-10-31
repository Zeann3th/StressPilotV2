package dev.zeann3th.stresspilot.service.project.impl;

import dev.zeann3th.stresspilot.common.enums.ErrorCode;
import dev.zeann3th.stresspilot.common.mappers.ProjectMapper;
import dev.zeann3th.stresspilot.dto.project.ProjectDTO;
import dev.zeann3th.stresspilot.dto.project.ProjectRequestDTO;
import dev.zeann3th.stresspilot.entity.EnvironmentEntity;
import dev.zeann3th.stresspilot.entity.ProjectEntity;
import dev.zeann3th.stresspilot.exception.CommandExceptionBuilder;
import dev.zeann3th.stresspilot.repository.EnvironmentRepository;
import dev.zeann3th.stresspilot.repository.ProjectRepository;
import dev.zeann3th.stresspilot.service.project.ProjectService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProjectServiceImpl implements ProjectService {
    private final ProjectRepository projectRepository;
    private final EnvironmentRepository environmentRepository;
    private final ProjectMapper projectMapper;

    @Override
    public Page<ProjectDTO> getListProject(String name, Pageable pageable) {
            Page<ProjectEntity> projectPage = projectRepository.findAllByCondition(name, pageable);
            return projectPage.map(projectMapper::toDTO);
    }

    @Override
    public ProjectDTO getProjectDetail(Long projectId) {
        ProjectEntity project = projectRepository.findById(projectId)
                .orElseThrow(() -> CommandExceptionBuilder.exception(ErrorCode.PROJECT_NOT_FOUND));
        return projectMapper.toDTO(project);
    }

    @Override
    public ProjectDTO createProject(ProjectRequestDTO projectRequestDTO) {
        EnvironmentEntity environmentEntity = EnvironmentEntity.builder()
                .build();
        EnvironmentEntity savedEnvironment = environmentRepository.save(environmentEntity);

        ProjectEntity project = ProjectEntity.builder()
                .name(projectRequestDTO.getName())
                .description(projectRequestDTO.getDescription())
                .environmentId(savedEnvironment.getId())
                .build();
        ProjectEntity savedProject = projectRepository.save(project);

        return projectMapper.toDTO(savedProject);
    }

    @Override
    public ProjectDTO updateProject(Long projectId, ProjectRequestDTO projectRequestDTO) {
        ProjectEntity project = projectRepository.findById(projectId)
                .orElseThrow(() -> CommandExceptionBuilder.exception(ErrorCode.PROJECT_NOT_FOUND));

        Optional.ofNullable(projectRequestDTO.getName()).ifPresent(project::setName);
        Optional.ofNullable(projectRequestDTO.getDescription()).ifPresent(project::setDescription);
        ProjectEntity saved = projectRepository.save(project);

        return projectMapper.toDTO(saved);
    }

    @Override
    public void deleteProject(Long projectId) {
        boolean exists = projectRepository.existsById(projectId);
        if (!exists) {
            throw CommandExceptionBuilder.exception(ErrorCode.PROJECT_NOT_FOUND);
        }
        projectRepository.deleteById(projectId);
    }
}
