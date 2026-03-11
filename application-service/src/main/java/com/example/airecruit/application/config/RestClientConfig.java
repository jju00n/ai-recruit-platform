package com.example.airecruit.application.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {

    @Value("${job-service.url}")
    private String jobServiceUrl;

    @Value("${claude.api.url}")
    private String claudeApiUrl;

    @Value("${claude.api.key}")
    private String claudeApiKey;

    @Bean
    public RestClient jobServiceRestClient() {
        return RestClient.builder()
                .baseUrl(jobServiceUrl)
                .build();
    }

    @Bean
    public RestClient claudeRestClient() {
        return RestClient.builder()
                .baseUrl(claudeApiUrl)
                .defaultHeader("x-api-key", claudeApiKey)
                .defaultHeader("anthropic-version", "2023-06-01")
                .defaultHeader("anthropic-beta", "prompt-caching-2024-07-31")
                .defaultHeader("content-type", MediaType.APPLICATION_JSON_VALUE)
                .build();
    }
}
