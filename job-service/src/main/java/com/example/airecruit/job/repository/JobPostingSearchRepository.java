package com.example.airecruit.job.repository;

import com.example.airecruit.job.document.JobPostingDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface JobPostingSearchRepository extends ElasticsearchRepository<JobPostingDocument, String> {
}
