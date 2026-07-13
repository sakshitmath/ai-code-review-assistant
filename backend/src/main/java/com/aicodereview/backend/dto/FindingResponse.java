package com.aicodereview.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FindingResponse {

    private Long id;
    private String tool;
    private String severity;
    private String issue;
    private String explanation;
    private String suggestion;
    private String fileName;
    private Integer lineNumber;
}