package dev.zeann3th.stresspilot.controller;

import dev.zeann3th.stresspilot.dto.project.ProjectRequestDTO;
import dev.zeann3th.stresspilot.dto.project.ProjectDTO;
import dev.zeann3th.stresspilot.service.ProjectService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/projects")
@RequiredArgsConstructor
public class ProjectController {
    private final ProjectService projectService;

    @GetMapping
    ResponseEntity<Page<ProjectDTO>> getListProjects(
            @RequestParam(value = "name", required = false) String name,
            @ParameterObject @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable) {
        return projectService.getListProject(name, pageable);
    }

    @GetMapping("/{projectId}")
    ResponseEntity<ProjectDTO> getProjectDetail(@PathVariable("projectId") Long projectId) {
        return projectService.getProjectDetail(projectId);
    }

    @PostMapping
    ResponseEntity<ProjectDTO> createProject(@Valid @RequestBody ProjectRequestDTO projectRequestDTO) {
        return projectService.createProject(projectRequestDTO);
    }

    @PatchMapping("/{projectId}")
    ResponseEntity<ProjectDTO> updateProject(@PathVariable("projectId") Long projectId,
                                                @RequestBody ProjectRequestDTO projectRequestDTO) {
        return projectService.updateProject(projectId, projectRequestDTO);
    }

    @DeleteMapping("/{projectId}")
    ResponseEntity<Void> deleteProject(@PathVariable("projectId") Long projectId) {
        return projectService.deleteProject(projectId);
    }
}
