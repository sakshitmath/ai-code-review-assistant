package com.aicodereview.backend.controller;

import com.aicodereview.backend.dto.ComplexityResponse;
import com.aicodereview.backend.service.ComplexityService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/complexity")
@RequiredArgsConstructor
public class ComplexityController {

    private final ComplexityService complexityService;

    @GetMapping("/{projectId}")
    public ResponseEntity<ComplexityResponse> analyze(
            Authentication auth,
            @PathVariable Long projectId) {
        return ResponseEntity.ok(complexityService.analyze(auth.getName(), projectId));
    }
}