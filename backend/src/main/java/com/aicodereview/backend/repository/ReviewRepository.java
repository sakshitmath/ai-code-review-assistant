package com.aicodereview.backend.repository;

import com.aicodereview.backend.entity.Project;
import com.aicodereview.backend.entity.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {

    List<Review> findByProjectOrderByCreatedAtDesc(Project project);

    List<Review> findByProjectAndReviewType(Project project, String reviewType);
}