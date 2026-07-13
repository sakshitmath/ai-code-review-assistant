package com.aicodereview.backend.controller;

import com.aicodereview.backend.dto.ProjectResponse;
import com.aicodereview.backend.dto.SnippetRequest;
import com.aicodereview.backend.service.ProjectService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;

    @PostMapping("/upload/files")
    public ResponseEntity<ProjectResponse> uploadFiles(
            Authentication auth,
            @RequestParam("projectName") String projectName,
            @RequestParam("files") MultipartFile[] files) {
        return ResponseEntity.ok(projectService.uploadFiles(auth.getName(), projectName, files));
    }

    @PostMapping("/upload/zip")
    public ResponseEntity<ProjectResponse> uploadZip(
            Authentication auth,
            @RequestParam("projectName") String projectName,
            @RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(projectService.uploadZip(auth.getName(), projectName, file));
    }

    @PostMapping("/snippet")
    public ResponseEntity<ProjectResponse> saveSnippet(
            Authentication auth,
            @Valid @RequestBody SnippetRequest request) {
        return ResponseEntity.ok(projectService.saveSnippet(auth.getName(), request));
    }

    @GetMapping
    public ResponseEntity<List<ProjectResponse>> getAllProjects(Authentication auth) {
        return ResponseEntity.ok(projectService.getAllProjects(auth.getName()));
    }

    @GetMapping("/search")
    public ResponseEntity<List<ProjectResponse>> searchProjects(
            Authentication auth,
            @RequestParam("keyword") String keyword) {
        return ResponseEntity.ok(projectService.searchProjects(auth.getName(), keyword));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProjectResponse> getProject(
            Authentication auth,
            @PathVariable Long id) {
        return ResponseEntity.ok(projectService.getProject(auth.getName(), id));
    }

    @GetMapping("/{id}/file")
    public ResponseEntity<Map<String, String>> getFileContent(
            Authentication auth,
            @PathVariable Long id,
            @RequestParam("fileName") String fileName) {
        String content = projectService.getFileContent(auth.getName(), id, fileName);
        return ResponseEntity.ok(Map.of("fileName", fileName, "content", content));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deleteProject(
            Authentication auth,
            @PathVariable Long id) {
        projectService.deleteProject(auth.getName(), id);
        return ResponseEntity.ok(Map.of("message", "Project deleted successfully"));
    }
}