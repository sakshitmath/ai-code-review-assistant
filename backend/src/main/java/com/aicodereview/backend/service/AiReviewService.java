package com.aicodereview.backend.service;

import com.aicodereview.backend.dto.ReviewResponse;
import com.aicodereview.backend.entity.Project;
import com.aicodereview.backend.entity.Review;
import com.aicodereview.backend.entity.ReviewFinding;
import com.aicodereview.backend.exception.ApiException;
import com.aicodereview.backend.repository.ReviewFindingRepository;
import com.aicodereview.backend.repository.ReviewRepository;
import com.aicodereview.backend.util.FileStorageUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AiReviewService {

    private final ProjectService projectService;
    private final GeminiService geminiService;
    private final FileStorageUtil fileStorageUtil;
    private final ReviewRepository reviewRepository;
    private final ReviewFindingRepository findingRepository;
    private final StaticAnalysisService staticAnalysisService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final int MAX_CODE_CHARS = 25000;

    @Transactional
    public ReviewResponse runAiReview(String email, Long projectId) {
        Project project = projectService.getOwnedProject(email, projectId);

        String code = buildCodeBundle(project.getStoragePath());
        String prompt = buildPrompt(code);

        String raw = geminiService.generate(prompt);
        String json = geminiService.cleanJson(raw);

        Review review = Review.builder()
                .project(project)
                .reviewType("AI")
                .build();

        Review savedReview = reviewRepository.save(review);

        List<ReviewFinding> findings = new ArrayList<>();
        int score = 0;
        String summary;

        try {
            JsonNode root = objectMapper.readTree(json);

            score = root.path("codeQualityScore").asInt(0);
            summary = root.path("summary").asText("AI review completed.");

            JsonNode issues = root.path("issues");
            if (issues.isArray()) {
                for (JsonNode node : issues) {
                    findings.add(ReviewFinding.builder()
                            .review(savedReview)
                            .tool("AI")
                            .severity(normalizeSeverity(node.path("severity").asText("MEDIUM")))
                            .issue(truncate(node.path("issue").asText("Unspecified issue"), 1000))
                            .explanation(truncate(node.path("explanation").asText(""), 2000))
                            .suggestion(truncate(node.path("suggestion").asText(""), 2000))
                            .fileName(node.path("fileName").asText("unknown"))
                            .lineNumber(node.path("lineNumber").asInt(1))
                            .build());
                }
            }

        } catch (Exception e) {
            throw new ApiException("Could not parse AI response: " + e.getMessage());
        }

        findingRepository.saveAll(findings);

        savedReview.setReviewScore(score);
        savedReview.setSummary(truncate(summary, 5000));
        reviewRepository.save(savedReview);

        return staticAnalysisService.toResponse(savedReview, findings);
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
                You are a Senior Java Software Engineer performing a code review.

                Review the Java code below and provide:
                1. Bugs found
                2. Security vulnerabilities
                3. Code smells
                4. Performance improvements
                5. Best coding practices
                6. Suggested refactoring
                7. Better variable and method names
                8. Code quality score out of 100
                9. Summary of recommendations

                Return ONLY valid JSON. No markdown, no code fences, no commentary.

                Use exactly this structure:
                {
                  "codeQualityScore": <integer 0-100>,
                  "summary": "<2-4 sentence overall summary>",
                  "issues": [
                    {
                      "severity": "HIGH" | "MEDIUM" | "LOW",
                      "issue": "<short title of the problem>",
                      "explanation": "<why this is a problem>",
                      "suggestion": "<how to fix it, with a concrete example if useful>",
                      "fileName": "<exact file name>",
                      "lineNumber": <integer>
                    }
                  ]
                }

                CODE TO REVIEW:
                %s
                """.formatted(code);
    }

    private String normalizeSeverity(String severity) {
        if (severity == null) return "MEDIUM";
        String s = severity.trim().toUpperCase();
        return switch (s) {
            case "HIGH", "CRITICAL", "SEVERE" -> "HIGH";
            case "LOW", "MINOR", "INFO" -> "LOW";
            default -> "MEDIUM";
        };
    }

    private String truncate(String text, int max) {
        if (text == null) return "";
        return text.length() > max ? text.substring(0, max) : text;
    }
}