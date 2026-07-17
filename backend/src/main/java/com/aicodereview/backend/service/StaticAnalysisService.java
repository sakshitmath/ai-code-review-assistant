package com.aicodereview.backend.service;

import com.aicodereview.backend.dto.FindingResponse;
import com.aicodereview.backend.dto.ReviewResponse;
import com.aicodereview.backend.entity.Project;
import com.aicodereview.backend.entity.Review;
import com.aicodereview.backend.entity.ReviewFinding;
import com.aicodereview.backend.exception.ApiException;
import com.aicodereview.backend.repository.ReviewFindingRepository;
import com.aicodereview.backend.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StaticAnalysisService {

    private final ProjectService projectService;
    private final CheckstyleService checkstyleService;
    private final PmdService pmdService;
    private final SpotBugsService spotBugsService;
    private final ReviewRepository reviewRepository;
    private final ReviewFindingRepository findingRepository;

    @Transactional
    public ReviewResponse runStaticAnalysis(String email, Long projectId) {
        Project project = projectService.getOwnedProject(email, projectId);

        Review review = Review.builder()
                .project(project)
                .reviewType("STATIC")
                .build();

        Review savedReview = reviewRepository.save(review);

        List<ReviewFinding> allFindings = new ArrayList<>();
        allFindings.addAll(checkstyleService.analyze(project.getStoragePath(), savedReview));
        allFindings.addAll(pmdService.analyze(project.getStoragePath(), savedReview));
        try {
            allFindings.addAll(spotBugsService.analyze(project.getStoragePath(), savedReview));
        } catch (Throwable t) {
            // SpotBugs can fail to initialize in some server environments.
            // Skip it gracefully so Checkstyle and PMD still produce results.
        }
        findingRepository.saveAll(allFindings);

        int score = calculateScore(allFindings);
        savedReview.setReviewScore(score);
        savedReview.setSummary(buildSummary(allFindings, score));
        reviewRepository.save(savedReview);

        return toResponse(savedReview, allFindings);
    }

    @Transactional(readOnly = true)
    public List<ReviewResponse> getReviewsForProject(String email, Long projectId) {
        Project project = projectService.getOwnedProject(email, projectId);

        return reviewRepository.findByProjectOrderByCreatedAtDesc(project)
                .stream()
                .map(r -> toResponse(r, findingRepository.findByReview(r)))
                .toList();
    }

    @Transactional(readOnly = true)
    public ReviewResponse getReview(String email, Long reviewId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ApiException("Review not found"));

        projectService.getOwnedProject(email, review.getProject().getId());

        return toResponse(review, findingRepository.findByReview(review));
    }

    @Transactional
    public void deleteReview(String email, Long reviewId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ApiException("Review not found"));

        projectService.getOwnedProject(email, review.getProject().getId());

        findingRepository.deleteAll(findingRepository.findByReview(review));
        reviewRepository.delete(review);
    }

    private int calculateScore(List<ReviewFinding> findings) {
        int score = 100;
        for (ReviewFinding f : findings) {
            switch (f.getSeverity()) {
                case "HIGH" -> score -= 5;
                case "MEDIUM" -> score -= 2;
                case "LOW" -> score -= 1;
                default -> { }
            }
        }
        return Math.max(score, 0);
    }

    private String buildSummary(List<ReviewFinding> findings, int score) {
        long high = findings.stream().filter(f -> "HIGH".equals(f.getSeverity())).count();
        long medium = findings.stream().filter(f -> "MEDIUM".equals(f.getSeverity())).count();
        long low = findings.stream().filter(f -> "LOW".equals(f.getSeverity())).count();

        return String.format(
                "Static analysis found %d issue(s): %d high, %d medium, %d low. Code quality score: %d/100.",
                findings.size(), high, medium, low, score
        );
    }

    ReviewResponse toResponse(Review review, List<ReviewFinding> findings) {
        Map<String, Long> bySeverity = findings.stream()
                .collect(Collectors.groupingBy(ReviewFinding::getSeverity, Collectors.counting()));

        Map<String, Long> byTool = findings.stream()
                .collect(Collectors.groupingBy(ReviewFinding::getTool, Collectors.counting()));

        List<FindingResponse> findingResponses = findings.stream()
                .map(f -> FindingResponse.builder()
                        .id(f.getId())
                        .tool(f.getTool())
                        .severity(f.getSeverity())
                        .issue(f.getIssue())
                        .explanation(f.getExplanation())
                        .suggestion(f.getSuggestion())
                        .fileName(f.getFileName())
                        .lineNumber(f.getLineNumber())
                        .build())
                .toList();

        return ReviewResponse.builder()
                .id(review.getId())
                .projectId(review.getProject().getId())
                .projectName(review.getProject().getProjectName())
                .reviewType(review.getReviewType())
                .reviewScore(review.getReviewScore())
                .summary(review.getSummary())
                .createdAt(review.getCreatedAt().toString())
                .totalIssues(findings.size())
                .issuesBySeverity(bySeverity)
                .issuesByTool(byTool)
                .findings(findingResponses)
                .build();
    }
}