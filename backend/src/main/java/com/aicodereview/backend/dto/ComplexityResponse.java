package com.aicodereview.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ComplexityResponse {

    private Long projectId;
    private String projectName;

    private int numberOfClasses;
    private int numberOfMethods;
    private int linesOfCode;
    private int cyclomaticComplexity;
    private double averageMethodLength;
    private double maintainabilityIndex;
}