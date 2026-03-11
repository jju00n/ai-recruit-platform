package com.example.airecruit.application.repository;

import com.example.airecruit.application.domain.Application;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ApplicationRepository extends JpaRepository<Application, Long> {
    boolean existsByUserIdAndJobPostingId(Long userId, Long jobPostingId);
    Page<Application> findAllByUserIdOrderByAppliedAtDesc(Long userId, Pageable pageable);
    Optional<Application> findByIdAndUserId(Long id, Long userId);
}
