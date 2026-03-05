package com.choragi.venue.config;

import com.google.genai.Client;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GenAIConfig {

    @Value("${spring.cloud.gcp.project-id}")
    private String projectId;

    @Bean(name = "voiceClient")
    public Client voiceClient() {
        return Client.builder()
                .project(projectId)
                .location("us-central1")
                .vertexAI(true)
                .build();
    }


    @Bean(name = "textClient")
    public Client textClient() {
        return Client.builder()
                .project(projectId)
                .location("global")
                .vertexAI(true)
                .build();
    }
}