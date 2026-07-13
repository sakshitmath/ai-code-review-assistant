package com.aicodereview.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewResponse {

    private Long id;
    private Long projectId;
    private String projectName;
    private String reviewType;
    private Integer reviewScore;
    private String summary;
    private String createdAt;

    private Integer totalIssues;
    private Map<String, Long> issuesBySeverity;
    private Map<String, Long> issuesByTool;

    private List<FindingResponse> findings;
}