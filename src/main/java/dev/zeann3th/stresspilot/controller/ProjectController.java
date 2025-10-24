package dev.zeann3th.stresspilot.controller;

import dev.zeann3th.stresspilot.dto.project.ProjectDTO;
import dev.zeann3th.stresspilot.dto.project.GetProjectDetailDTO;
import dev.zeann3th.stresspilot.entity.ProjectEntity;
import dev.zeann3th.stresspilot.service.ProjectService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/projects")
@RequiredArgsConstructor
@SuppressWarnings("all")
public class ProjectController {
    private final ProjectService projectService;

    @GetMapping
    ResponseEntity<Page<GetProjectDetailDTO>> getListProjects(
            @RequestParam(value = "name", required = false) String name,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable) {
        return projectService.getListProject(name, pageable);
    }

    @GetMapping("/{projectId}")
    ResponseEntity<ProjectEntity> getProjectDetail(@PathVariable("projectId") Long projectId) {
        return projectService.getProjectDetail(projectId);
    }

    @PostMapping
    ResponseEntity<ProjectEntity> createProject(@RequestBody ProjectDTO projectDTO) {
        return projectService.createProject(projectDTO);
    }

    @PatchMapping("/{projectId}")
    ResponseEntity<ProjectEntity> updateProject(@PathVariable("projectId") Long projectId,
                                                @RequestBody ProjectDTO projectDTO) {
        return projectService.updateProject(projectId, projectDTO);
    }

    @DeleteMapping("/{projectId}")
    ResponseEntity<Void> deleteProject(@PathVariable("projectId") Long projectId) {
        return projectService.deleteProject(projectId);
    }
}
