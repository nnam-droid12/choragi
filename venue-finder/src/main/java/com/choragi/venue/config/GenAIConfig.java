package com.choragi.venue.config;

import com.google.genai.Client;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GenAIConfig {

    // 1. Grab the injected GEMINI_API_KEY from the Cloud Run environment
    @Value("${GEMINI_API_KEY}")
    private String geminiApiKey;

    @Bean(name = "voiceClient")
    public Client voiceClient() {
        return Client.builder()
                .apiKey(geminiApiKey)
                .build();
    }

    @Bean(name = "textClient")
    public Client textClient() {
        return Client.builder()
                .apiKey(geminiApiKey)
                .build();
    }
}