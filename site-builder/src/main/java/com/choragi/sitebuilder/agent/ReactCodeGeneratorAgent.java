package com.choragi.sitebuilder.agent;

import com.google.genai.Client;
import com.google.genai.types.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class ReactCodeGeneratorAgent {

    private final Client client;

    public String generateReactLandingPage(String artistName, String date, String location, String posterUrl, String videoUrl) {
        log.info("Choragi Frontend Agent: Writing React code for {} landing page...", artistName);

        String safeVideo = (videoUrl != null) ? videoUrl : "";
        String safePoster = (posterUrl != null) ? posterUrl : "";

        // THE FIX: Aggressively structured prompt to prevent AI hallucinations
        String prompt = "Write a complete, single-file React application (using React, ReactDOM, and Babel via CDN) " +
                "styled with Tailwind CSS. It must be a dark-themed concert landing page. " +
                "CRITICAL INSTRUCTION 1: The main headline MUST prominently display exactly this artist name: '" + artistName + "'. Do not use any other name. " +
                "CRITICAL INSTRUCTION 2: The event details MUST explicitly state the Date as '" + date + "' and the Location as '" + location + "'. " +
                "CRITICAL INSTRUCTION 3: DO NOT hallucinate placeholder images or stock videos. " +
                "You MUST use EXACTLY these two HTML elements exactly as written below to display the media: " +
                "For the image: <img src=\"" + safePoster + "\" alt=\"Concert Poster\" className=\"w-full max-w-md mx-auto rounded-lg shadow-2xl mb-8\" /> " +
                "For the video: <video src=\"" + safeVideo + "\" autoPlay muted loop playsInline className=\"w-full max-w-2xl mx-auto rounded-lg shadow-2xl mb-8\"></video> " +
                "RETURN ONLY THE RAW HTML CODE. Do not include markdown formatting like ```html.";

        GenerateContentConfig config = GenerateContentConfig.builder()
                .temperature(0.0f) // THE FIX: Temperature 0.0 forces the AI to be strictly literal and stop being "creative" with facts
                .build();

        try {
            GenerateContentResponse response = client.models.generateContent(
                    "gemini-3.1-pro-preview",
                    prompt,
                    config
            );

            String htmlCode = response.text();
            if (htmlCode != null && htmlCode.startsWith("```html")) {
                htmlCode = htmlCode.substring(7, htmlCode.length() - 3);
            }
            return htmlCode != null ? htmlCode.trim() : "<html><body>Error</body></html>";

        } catch (Exception e) {
            log.error("Failed to generate React code", e);
            return "<html><body><h1>Error generating site</h1></body></html>";
        }
    }
}