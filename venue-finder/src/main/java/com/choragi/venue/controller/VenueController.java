package com.choragi.venue.controller;

import com.choragi.venue.agent.VenueScoutAgent;
import com.choragi.venue.model.VenueLead;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/scout")
@RequiredArgsConstructor
public class VenueController {

    private final VenueScoutAgent scoutAgent;

    @PostMapping
    public List<VenueLead> startScouting(@RequestBody Map<String, Object> tourState) {
        String city = (String) tourState.get("targetCity");


        String artistProfile = "Indie Rock band with a high-energy live show, looking for mid-size venues (300-800 capacity).";

        return scoutAgent.scoutVenues(city, artistProfile);
    }
}