package com.choragi.venue.agent;

import com.choragi.venue.model.VenueLead;
import com.choragi.venue.tools.GoogleMapsSearchTool;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.genai.Client;
import com.google.genai.types.Content;
import com.google.genai.types.GenerateContentConfig;
import com.google.genai.types.Part;
import com.google.genai.types.ThinkingConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class VenueScoutAgent {

    private final GoogleMapsSearchTool mapsTool;
    private final Client client;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public List<VenueLead> scoutVenues(String city, String artistProfile) {
        log.info("Choragi Scout: Searching in {}", city);

        String rawVenueData = mapsTool.searchVenues(city, "live music venues");
        String modelId = "gemini-3.1-pro-preview";

        String systemInstructionText = "You are a venue scouting agent. " +
                "Analyze the provided JSON data and return a JSON ARRAY of the top 5 venues. " +
                "Return ONLY the JSON array. " +
                "Each object must strictly follow this schema: " +
                "{\"name\": \"string\", \"address\": \"string\", \"reasoning\": \"string\"}";

        GenerateContentConfig config = GenerateContentConfig.builder()
                .systemInstruction(Content.fromParts(Part.fromText(systemInstructionText)))
                .thinkingConfig(ThinkingConfig.builder().includeThoughts(false).build())
                .responseMimeType("application/json")
                .build();

        String userPrompt = "Artist Profile: " + artistProfile + "\nRaw Data: " + rawVenueData;

        try {
            log.info("Choragi Scout: Analyzing data with Gemini 3.1 Pro...");
            var response = client.models.generateContent(modelId, userPrompt, config);

            // Convert the AI's JSON string into a List of VenueLead objects
            return objectMapper.readValue(response.text(), new TypeReference<List<VenueLead>>() {});

        } catch (Exception e) {
            log.error("Choragi Scout: Failed to parse AI response into VenueLeads", e);
            return new ArrayList<>(); // Return empty list on failure
        }
    }
}