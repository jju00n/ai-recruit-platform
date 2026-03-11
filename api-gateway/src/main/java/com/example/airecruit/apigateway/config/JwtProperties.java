package com.example.airecruit.apigateway.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "jwt.secret")
@Getter
@Setter
public class JwtProperties {
    private String key;
}
