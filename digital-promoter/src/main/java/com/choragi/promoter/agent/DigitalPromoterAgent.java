package com.choragi.promoter.agent;

import com.google.genai.Client;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class DigitalPromoterAgent {

    private final Client client;

    public String generateSocialCampaign(String artistName, String posterUrl) {
        log.info("Choragi Promoter: Creating campaign for {}", artistName);

        String modelId = "gemini-3.1-pro-preview";
        String prompt = String.format(
                "Create a 3-post Instagram launch sequence for the artist '%s'. " +
                        "Reference this tour poster: %s. Use a high-energy tone.", artistName, posterUrl);

        try {
            var response = client.models.generateContent(modelId, prompt, null);
            return response.text();
        } catch (Exception e) {
            log.error("Promoter failed to generate campaign", e);
            return "Campaign generation failed.";
        }
    }
}