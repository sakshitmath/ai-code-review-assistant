package com.aicodereview.backend.repository;

import com.aicodereview.backend.entity.Review;
import com.aicodereview.backend.entity.ReviewFinding;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReviewFindingRepository extends JpaRepository<ReviewFinding, Long> {

    List<ReviewFinding> findByReview(Review review);

    List<ReviewFinding> findByReviewAndSeverity(Review review, String severity);

    List<ReviewFinding> findByReviewAndTool(Review review, String tool);
}