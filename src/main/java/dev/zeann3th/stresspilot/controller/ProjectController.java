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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/projects")
@SuppressWarnings("unused")
@RequiredArgsConstructor
public class ProjectController {
    private final ProjectService projectService;

    @GetMapping
    ResponseEntity<Page<ProjectDTO>> getListProjects(
            @RequestParam(value = "name", required = false) String name,
            @ParameterObject @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable) {
        var resp = projectService.getListProject(name, pageable);
        return ResponseEntity.ok(resp);
    }

    @GetMapping("/{projectId}")
    ResponseEntity<ProjectDTO> getProjectDetail(@PathVariable("projectId") Long projectId) {
        var resp = projectService.getProjectDetail(projectId);
        return ResponseEntity.ok(resp);
    }

    @PostMapping
    ResponseEntity<ProjectDTO> createProject(@Valid @RequestBody ProjectRequestDTO projectRequestDTO) {
        var resp = projectService.createProject(projectRequestDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(resp);
    }

    @PatchMapping("/{projectId}")
    ResponseEntity<ProjectDTO> updateProject(@PathVariable("projectId") Long projectId,
                                                @RequestBody ProjectRequestDTO projectRequestDTO) {
        var resp = projectService.updateProject(projectId, projectRequestDTO);
        return ResponseEntity.ok(resp);
    }

    @DeleteMapping("/{projectId}")
    ResponseEntity<Void> deleteProject(@PathVariable("projectId") Long projectId) {
        projectService.deleteProject(projectId);
        return ResponseEntity.noContent().build();
    }
}
