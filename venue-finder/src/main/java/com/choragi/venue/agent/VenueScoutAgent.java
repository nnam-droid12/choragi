package com.choragi.venue.agent;

import com.choragi.venue.model.VenueLead;
import com.choragi.venue.tools.GoogleMapsSearchTool;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.genai.Client;
import com.google.genai.types.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class VenueScoutAgent {

    private final GoogleMapsSearchTool mapsTool;
    private final Client client;
    private final ObjectMapper objectMapper = new ObjectMapper();


    public VenueScoutAgent(GoogleMapsSearchTool mapsTool, @Qualifier("textClient") Client client) {
        this.mapsTool = mapsTool;
        this.client = client;
    }

    public List<VenueLead> scoutVenues(String city, String artistProfile) {
        log.info("Choragi Scout: Starting search in {}", city);

        String rawVenueData = mapsTool.searchVenues(city, "live music venues");

        String modelId = "gemini-3.1-pro-preview";

        String systemInstructionText = "You are a venue scouting agent. " +
                "Analyze the provided JSON data and return a JSON ARRAY of the top 5 venues. " +
                "Each object must strictly follow this schema: " +
                "{\"name\": \"string\", \"address\": \"string\", \"phoneNumber\": \"string\", \"reasoning\": \"string\"}. " +
                "If a phone number is missing from the data, output 'UNKNOWN'.";

        GenerateContentConfig config = GenerateContentConfig.builder()
                .systemInstruction(Content.fromParts(Part.fromText(systemInstructionText)))
                .responseMimeType("application/json")
                .build();

        String userPrompt = "Artist Profile: " + artistProfile + "\nRaw Data: " + rawVenueData;

        try {
            var response = client.models.generateContent(modelId, userPrompt, config);

            return objectMapper.readValue(response.text(), new TypeReference<List<VenueLead>>() {});
        } catch (Exception e) {
            log.error("Scout failed to parse response", e);
            return new ArrayList<>();
        }
    }
}