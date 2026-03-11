package com.choragi.sitebuilder.config;

import com.google.genai.Client;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GenAIConfig {

    @Value("${GEMINI_API_KEY}")
    private String geminiApiKey;

    @Bean
    public Client genAiClient() {
        return Client.builder()
                .apiKey(geminiApiKey)
                .build();
    }
}