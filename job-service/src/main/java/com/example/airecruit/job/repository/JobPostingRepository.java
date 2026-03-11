package com.example.airecruit.job.repository;

import com.example.airecruit.job.domain.JobPosting;
import com.example.airecruit.job.domain.enums.JobStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface JobPostingRepository extends JpaRepository<JobPosting, Long> {

    @Query("SELECT jp FROM JobPosting jp JOIN FETCH jp.company WHERE jp.status = :status")
    Page<JobPosting> findAllByStatusWithCompany(JobStatus status, Pageable pageable);

    @Query("SELECT jp FROM JobPosting jp JOIN FETCH jp.company")
    List<JobPosting> findAllWithCompany();

    boolean existsBySourceUrl(String sourceUrl);
}
