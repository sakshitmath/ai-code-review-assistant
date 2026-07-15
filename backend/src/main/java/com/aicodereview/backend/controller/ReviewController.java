package com.aicodereview.backend.controller;

import com.aicodereview.backend.dto.ReviewResponse;
import com.aicodereview.backend.service.StaticAnalysisService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import com.aicodereview.backend.service.AiReviewService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final StaticAnalysisService staticAnalysisService;
    private final AiReviewService aiReviewService;

    @PostMapping("/static/{projectId}")
    public ResponseEntity<ReviewResponse> runStaticAnalysis(
            Authentication auth,
            @PathVariable Long projectId) {
        return ResponseEntity.ok(staticAnalysisService.runStaticAnalysis(auth.getName(), projectId));
    }
    @PostMapping("/ai/{projectId}")
    public ResponseEntity<ReviewResponse> runAiReview(
            Authentication auth,
            @PathVariable Long projectId) {
        return ResponseEntity.ok(aiReviewService.runAiReview(auth.getName(), projectId));
    }

    @GetMapping("/project/{projectId}")
    public ResponseEntity<List<ReviewResponse>> getReviewsForProject(
            Authentication auth,
            @PathVariable Long projectId) {
        return ResponseEntity.ok(staticAnalysisService.getReviewsForProject(auth.getName(), projectId));
    }

    @GetMapping("/{reviewId}")
    public ResponseEntity<ReviewResponse> getReview(
            Authentication auth,
            @PathVariable Long reviewId) {
        return ResponseEntity.ok(staticAnalysisService.getReview(auth.getName(), reviewId));
    }

    @DeleteMapping("/{reviewId}")
    public ResponseEntity<Map<String, String>> deleteReview(
            Authentication auth,
            @PathVariable Long reviewId) {
        staticAnalysisService.deleteReview(auth.getName(), reviewId);
        return ResponseEntity.ok(Map.of("message", "Review deleted successfully"));
    }
}