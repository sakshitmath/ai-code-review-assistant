package com.aicodereview.backend.service;

import com.aicodereview.backend.dto.ComplexityResponse;
import com.aicodereview.backend.entity.Project;
import com.aicodereview.backend.exception.ApiException;
import com.aicodereview.backend.util.FileStorageUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class ComplexityService {

    private final ProjectService projectService;
    private final FileStorageUtil fileStorageUtil;

    private static final Pattern CLASS_PATTERN =
            Pattern.compile("\\b(class|interface|enum)\\s+\\w+");

    private static final Pattern METHOD_PATTERN =
            Pattern.compile("(public|private|protected|static|\\s)+[\\w<>\\[\\],\\s]+\\s+(\\w+)\\s*\\([^)]*\\)\\s*(throws\\s+[\\w,\\s]+)?\\{");

    private static final Pattern DECISION_PATTERN =
            Pattern.compile("\\b(if|else if|for|while|case|catch|&&|\\|\\||\\?)\\b|\\?");

    public ComplexityResponse analyze(String email, Long projectId) {
        Project project = projectService.getOwnedProject(email, projectId);

        List<String> files = fileStorageUtil.listJavaFiles(project.getStoragePath());
        if (files.isEmpty()) {
            throw new ApiException("No Java files found in this project");
        }

        int totalClasses = 0;
        int totalMethods = 0;
        int totalLoc = 0;
        int totalComplexity = 0;

        for (String fileName : files) {
            String content = fileStorageUtil.readFile(project.getStoragePath(), fileName);
            String noComments = stripComments(content);

            totalClasses += countMatches(CLASS_PATTERN, noComments);
            int methods = countMatches(METHOD_PATTERN, noComments);
            totalMethods += methods;
            totalLoc += countLoc(noComments);
            totalComplexity += 1 + countDecisions(noComments);
        }

        double avgMethodLength = totalMethods == 0 ? 0.0 : (double) totalLoc / totalMethods;
        double maintainability = computeMaintainability(totalLoc, totalComplexity);

        return ComplexityResponse.builder()
                .projectId(project.getId())
                .projectName(project.getProjectName())
                .numberOfClasses(totalClasses)
                .numberOfMethods(totalMethods)
                .linesOfCode(totalLoc)
                .cyclomaticComplexity(totalComplexity)
                .averageMethodLength(round(avgMethodLength))
                .maintainabilityIndex(round(maintainability))
                .build();
    }

    private String stripComments(String code) {
        String noBlock = code.replaceAll("(?s)/\\*.*?\\*/", "");
        return noBlock.replaceAll("//.*", "");
    }

    private int countLoc(String code) {
        int count = 0;
        for (String line : code.split("\n")) {
            if (!line.trim().isEmpty()) {
                count++;
            }
        }
        return count;
    }

    private int countMatches(Pattern pattern, String text) {
        Matcher m = pattern.matcher(text);
        int count = 0;
        while (m.find()) {
            count++;
        }
        return count;
    }

    private int countDecisions(String code) {
        Matcher m = DECISION_PATTERN.matcher(code);
        int count = 0;
        while (m.find()) {
            count++;
        }
        return count;
    }

    private double computeMaintainability(int loc, int complexity) {
        if (loc == 0) {
            return 100.0;
        }
        double locLog = Math.log(loc);
        double raw = 171 - 5.2 * locLog - 0.23 * complexity - 16.2 * locLog;
        double normalized = Math.max(0.0, raw) * 100.0 / 171.0;
        return Math.min(100.0, normalized);
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}