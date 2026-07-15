package com.aicodereview.backend.controller;

import com.aicodereview.backend.dto.DocumentationResponse;
import com.aicodereview.backend.service.DocumentationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/documentation")
@RequiredArgsConstructor
public class DocumentationController {

    private final DocumentationService documentationService;

    @PostMapping("/{projectId}")
    public ResponseEntity<DocumentationResponse> generate(
            Authentication auth,
            @PathVariable Long projectId) {
        return ResponseEntity.ok(documentationService.generate(auth.getName(), projectId));
    }
}