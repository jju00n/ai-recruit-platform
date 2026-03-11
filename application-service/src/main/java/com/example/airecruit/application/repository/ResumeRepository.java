package com.example.airecruit.application.repository;

import com.example.airecruit.application.domain.Resume;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ResumeRepository extends JpaRepository<Resume, Long> {
    List<Resume> findAllByUserIdOrderByCreatedAtDesc(Long userId);
    Optional<Resume> findByIdAndUserId(Long id, Long userId);
}
