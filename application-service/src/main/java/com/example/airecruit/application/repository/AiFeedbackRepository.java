package com.example.airecruit.application.repository;

import com.example.airecruit.application.domain.AiFeedback;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AiFeedbackRepository extends JpaRepository<AiFeedback, Long> {
    Optional<AiFeedback> findByApplicationId(Long applicationId);
}
