package com.example.airecruit.job.dto;

import com.example.airecruit.job.domain.enums.EmploymentType;
import com.example.airecruit.job.domain.enums.ExperienceLevel;
import com.example.airecruit.job.domain.enums.JobCategory;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class JobSearchDto {

    private String keyword;
    private JobCategory jobCategory;
    private String location;
    private EmploymentType employmentType;
    private ExperienceLevel experienceLevel;
    private Integer minSalary;
    private int page = 0;
    private int size = 10;
}
