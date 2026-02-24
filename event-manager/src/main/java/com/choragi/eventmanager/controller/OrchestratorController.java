package com.choragi.eventmanager.controller;

import com.choragi.eventmanager.model.GlobalTourState;
import com.choragi.eventmanager.service.BigQueryLoggerService; // Added
import lombok.RequiredArgsConstructor; // Added
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate; // Added

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/orchestrator")
@RequiredArgsConstructor
public class OrchestratorController {


    private final RestTemplate restTemplate;
    private final BigQueryLoggerService loggerService;

    @PostMapping("/start")
    public GlobalTourState startTour(@RequestBody Map<String, String> request) {
        GlobalTourState state = new GlobalTourState();
        state.setTourId(UUID.randomUUID().toString());
        state.setArtistId(request.get("artistId"));
        state.setTargetCity(request.get("targetCity"));
        state.setStatus("SCOUTING");
        state.log("Choragi system initialized. Deploying Venue Finder agents...");

        String venueFinderUrl = "http://localhost:8081/api/scout";

        try {

            String venuesJson = restTemplate.postForObject(venueFinderUrl, state, String.class);


            state.setDiscoveredVenues(venuesJson);
            state.setStatus("NEGOTIATING");
            state.log("Venue Finder returned leads. Transitioning to Live Negotiator...");

        } catch (Exception e) {
            state.log("Failed to contact Venue Finder: " + e.getMessage());
        }


        loggerService.saveTourState(state);

        return state;
    }
}