package com.aicodereview.backend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "review_findings")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewFinding {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "review_id", nullable = false)
    private Review review;

    @Column(nullable = false)
    private String tool;

    @Column(nullable = false)
    private String severity;

    @Column(length = 1000)
    private String issue;

    @Column(length = 2000)
    private String explanation;

    @Column(length = 2000)
    private String suggestion;

    private String fileName;

    private Integer lineNumber;
}