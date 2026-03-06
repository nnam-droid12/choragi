package com.choragi.creative.config;

import com.google.genai.Client;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GenAIConfig {

    @Value("${spring.cloud.gcp.project-id}")
    private String projectId;

    @Bean
    public Client genAiClient() {
        return Client.builder()
                .project(projectId)
                .location("global")
                .vertexAI(true)
                .build();
    }
}