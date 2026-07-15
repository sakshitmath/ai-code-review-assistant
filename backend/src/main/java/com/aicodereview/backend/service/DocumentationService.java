package com.aicodereview.backend.service;

import com.aicodereview.backend.dto.DocumentationResponse;
import com.aicodereview.backend.entity.Project;
import com.aicodereview.backend.exception.ApiException;
import com.aicodereview.backend.util.FileStorageUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DocumentationService {

    private final ProjectService projectService;
    private final GeminiService geminiService;
    private final FileStorageUtil fileStorageUtil;

    private static final int MAX_CODE_CHARS = 25000;

    public DocumentationResponse generate(String email, Long projectId) {
        Project project = projectService.getOwnedProject(email, projectId);

        String code = buildCodeBundle(project.getStoragePath());
        String prompt = buildPrompt(code);

        String documentation = geminiService.generate(prompt);

        return DocumentationResponse.builder()
                .projectId(project.getId())
                .projectName(project.getProjectName())
                .documentation(documentation)
                .build();
    }

    private String buildCodeBundle(String folderPath) {
        List<String> fileNames = fileStorageUtil.listJavaFiles(folderPath);

        if (fileNames.isEmpty()) {
            throw new ApiException("No Java files found in this project");
        }

        StringBuilder sb = new StringBuilder();
        for (String name : fileNames) {
            String content = fileStorageUtil.readFile(folderPath, name);
            sb.append("=== FILE: ").append(name).append(" ===\n");
            sb.append(content).append("\n\n");

            if (sb.length() > MAX_CODE_CHARS) {
                sb.setLength(MAX_CODE_CHARS);
                sb.append("\n... [truncated] ...");
                break;
            }
        }
        return sb.toString();
    }

    private String buildPrompt(String code) {
        return """
                You are a senior Java developer writing technical documentation.

                For the Java code below, generate clear, well-structured documentation in Markdown format covering:

                1. **Overview** - a short summary of what the code does
                2. **Class Documentation** - for each class: its purpose and responsibilities
                3. **Method Documentation** - for each method: what it does, its parameters, return value, and any exceptions
                4. **API Documentation** - if there are any public methods that act as an API, describe how to use them

                Write in clean Markdown with headings, bullet points, and code references where helpful.
                Return ONLY the Markdown documentation. No commentary before or after.

                CODE:
                %s
                """.formatted(code);
    }
}