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

        String prompt = String.format(
                "Write a complete, single-file React application (using React, ReactDOM, and Babel via CDN) " +
                        "styled with Tailwind CSS via CDN. " +
                        "It must be a dark-themed, cinematic concert landing page for '%s'. " +
                        "Include these details: Date: %s, Location: %s. " +
                        "Display this poster image prominently: %s " +
                        "Embed this promotional video using an HTML5 <video> tag (autoplay, muted, loop): %s " +
                        "Include a functional-looking 'Register for Tickets' form (Name, Email, Phone, and a Submit button). " +
                        "RETURN ONLY THE RAW HTML CODE. Do not include markdown formatting like ```html.",
                artistName, date, location, posterUrl, videoUrl != null ? videoUrl : ""
        );

        GenerateContentConfig config = GenerateContentConfig.builder()
                .temperature(0.2f)
                .build();

        try {
            GenerateContentResponse response = client.models.generateContent(
                    "gemini-2.5-pro",
                    prompt,
                    config
            );

            String htmlCode = response.text();
            if (htmlCode.startsWith("```html")) {
                htmlCode = htmlCode.substring(7, htmlCode.length() - 3);
            }
            return htmlCode.trim();

        } catch (Exception e) {
            log.error("Failed to generate React code", e);
            return "<html><body><h1>Error generating site</h1></body></html>";
        }
    }
}