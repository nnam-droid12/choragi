package com.choragi.venue.agent;

import com.choragi.venue.tools.GoogleMapsSearchTool;
import com.google.genai.Client;
import com.google.genai.types.Content;
import com.google.genai.types.GenerateContentConfig;
import com.google.genai.types.Part; // Ensure this is imported
import com.google.genai.types.ThinkingConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class VenueScoutAgent {

    private final GoogleMapsSearchTool mapsTool;
    private final Client client;

    public String scoutVenues(String city, String artistProfile) {
        log.info("Choragi Scout: Searching in {}", city);


        String rawVenueData = mapsTool.searchVenues(city, "live music venues");

        String modelId = "gemini-3.1-pro-preview";

        String systemInstructionText = "You are a venue scouting agent. " +
                "Analyze the provided JSON data and return a JSON ARRAY of the top 5 venues. " +
                "Each object must have 'name', 'address', and 'reasoning'. " +
                "OUTPUT ONLY THE JSON ARRAY. NO PROSE. NO EXPLANATIONS.";


        GenerateContentConfig config = GenerateContentConfig.builder()
                .systemInstruction(Content.fromParts(Part.fromText(systemInstructionText)))
                .thinkingConfig(ThinkingConfig.builder()
                        .includeThoughts(false)
                        .build())
                .responseMimeType("application/json")
                .build();

        String userPrompt = "Artist Profile: " + artistProfile + "\nRaw Data: " + rawVenueData;

        try {
            log.info("Choragi Scout: Analyzing data with Gemini 3.1 Pro...");


            var response = client.models.generateContent(modelId, userPrompt, config);

            log.info("Choragi Scout: Selection complete.");
            return response.text();

        } catch (Exception e) {
            log.error("Choragi Scout: Failed to analyze venues", e);
            return "[]";
        }
    }
}