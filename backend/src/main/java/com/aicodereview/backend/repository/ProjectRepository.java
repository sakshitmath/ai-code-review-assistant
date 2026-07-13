package com.aicodereview.backend.repository;

import com.aicodereview.backend.entity.Project;
import com.aicodereview.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProjectRepository extends JpaRepository<Project, Long> {

    List<Project> findByUserOrderByCreatedAtDesc(User user);

    Optional<Project> findByIdAndUser(Long id, User user);

    List<Project> findByUserAndProjectNameContainingIgnoreCaseOrderByCreatedAtDesc(User user, String keyword);
}