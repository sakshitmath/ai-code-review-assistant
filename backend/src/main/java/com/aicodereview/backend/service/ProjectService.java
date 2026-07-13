package com.aicodereview.backend.service;

import com.aicodereview.backend.dto.ProjectResponse;
import com.aicodereview.backend.dto.SnippetRequest;
import com.aicodereview.backend.entity.Project;
import com.aicodereview.backend.entity.User;
import com.aicodereview.backend.exception.ApiException;
import com.aicodereview.backend.repository.ProjectRepository;
import com.aicodereview.backend.util.FileStorageUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final UserService userService;
    private final FileStorageUtil fileStorageUtil;

    public ProjectResponse uploadFiles(String email, String projectName, MultipartFile[] files) {
        User user = userService.getByEmail(email);

        if (files == null || files.length == 0) {
            throw new ApiException("No files provided");
        }

        String folderPath = fileStorageUtil.createProjectFolder(user.getId());
        int count = fileStorageUtil.saveJavaFiles(files, folderPath);

        Project project = Project.builder()
                .user(user)
                .projectName(projectName)
                .uploadType("FILE")
                .storagePath(folderPath)
                .fileCount(count)
                .build();

        return toResponse(projectRepository.save(project));
    }

    public ProjectResponse uploadZip(String email, String projectName, MultipartFile zipFile) {
        User user = userService.getByEmail(email);

        if (zipFile == null || zipFile.isEmpty()) {
            throw new ApiException("No ZIP file provided");
        }

        String name = zipFile.getOriginalFilename();
        if (name == null || !name.toLowerCase().endsWith(".zip")) {
            throw new ApiException("File must be a .zip archive");
        }

        String folderPath = fileStorageUtil.createProjectFolder(user.getId());
        int count = fileStorageUtil.extractZip(zipFile, folderPath);

        Project project = Project.builder()
                .user(user)
                .projectName(projectName)
                .uploadType("ZIP")
                .storagePath(folderPath)
                .fileCount(count)
                .build();

        return toResponse(projectRepository.save(project));
    }

    public ProjectResponse saveSnippet(String email, SnippetRequest request) {
        User user = userService.getByEmail(email);

        String folderPath = fileStorageUtil.createProjectFolder(user.getId());
        fileStorageUtil.saveSnippet(request.getCode(), request.getFileName(), folderPath);

        Project project = Project.builder()
                .user(user)
                .projectName(request.getProjectName())
                .uploadType("SNIPPET")
                .storagePath(folderPath)
                .fileCount(1)
                .build();

        return toResponse(projectRepository.save(project));
    }

    public List<ProjectResponse> getAllProjects(String email) {
        User user = userService.getByEmail(email);
        return projectRepository.findByUserOrderByCreatedAtDesc(user)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public List<ProjectResponse> searchProjects(String email, String keyword) {
        User user = userService.getByEmail(email);
        return projectRepository
                .findByUserAndProjectNameContainingIgnoreCaseOrderByCreatedAtDesc(user, keyword)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public ProjectResponse getProject(String email, Long projectId) {
        return toResponse(getOwnedProject(email, projectId));
    }

    public String getFileContent(String email, Long projectId, String fileName) {
        Project project = getOwnedProject(email, projectId);
        return fileStorageUtil.readFile(project.getStoragePath(), fileName);
    }

    public void deleteProject(String email, Long projectId) {
        Project project = getOwnedProject(email, projectId);
        fileStorageUtil.deleteFolder(project.getStoragePath());
        projectRepository.delete(project);
    }

    public Project getOwnedProject(String email, Long projectId) {
        User user = userService.getByEmail(email);
        return projectRepository.findByIdAndUser(projectId, user)
                .orElseThrow(() -> new ApiException("Project not found"));
    }

    private ProjectResponse toResponse(Project project) {
        return ProjectResponse.builder()
                .id(project.getId())
                .projectName(project.getProjectName())
                .uploadType(project.getUploadType())
                .fileCount(project.getFileCount())
                .createdAt(project.getCreatedAt().toString())
                .files(fileStorageUtil.listJavaFiles(project.getStoragePath()))
                .build();
    }
}