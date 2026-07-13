package com.aicodereview.backend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SnippetRequest {

    @NotBlank(message = "Project name is required")
    private String projectName;

    @NotBlank(message = "File name is required")
    private String fileName;

    @NotBlank(message = "Code is required")
    private String code;
}