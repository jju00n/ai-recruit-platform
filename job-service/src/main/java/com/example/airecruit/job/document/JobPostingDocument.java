package com.example.airecruit.job.document;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.*;

import java.time.LocalDateTime;
import java.util.List;

@Document(indexName = "job_postings")
@Mapping(mappingPath = "es-mappings/job_postings.json")
@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobPostingDocument {

    @Id
    private String id;          // jobPosting.id.toString()

    private Long jobPostingId;
    private Long companyId;

    @Field(type = FieldType.Keyword)
    private String companyName;

    @Field(type = FieldType.Text, analyzer = "standard")
    private String title;

    @Field(type = FieldType.Text, analyzer = "standard")
    private String description;

    @Field(type = FieldType.Keyword)
    private String jobCategory;

    @Field(type = FieldType.Keyword)
    private String location;

    @Field(type = FieldType.Keyword)
    private String employmentType;

    @Field(type = FieldType.Keyword)
    private String experienceLevel;

    private Integer minSalary;
    private Integer maxSalary;

    @Field(type = FieldType.Keyword)
    private List<String> skills;

    @Field(type = FieldType.Keyword)
    private String status;

    @Field(type = FieldType.Keyword)
    private String source;      // MANUAL / SARAMIN / WANTED / ...

    @Field(type = FieldType.Keyword)
    private String deadline;    // "yyyy-MM-dd"

    @Field(type = FieldType.Date, format = DateFormat.date_hour_minute_second)
    @JsonSerialize(using = LocalDateTimeSerializer.class)
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    private LocalDateTime createdAt;

    private float[] descriptionVector;
}
