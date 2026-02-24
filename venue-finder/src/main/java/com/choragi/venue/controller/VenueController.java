package com.choragi.venue.controller;

import com.choragi.venue.agent.VenueScoutAgent;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/scout")
@RequiredArgsConstructor
public class VenueController {

    private final VenueScoutAgent scoutAgent;

    @PostMapping
    public String startScouting(@RequestBody Map<String, Object> tourState) {
        String city = (String) tourState.get("targetCity");

        String artistProfile = "Indie Rock band, looking for mid-size venues.";


        return scoutAgent.scoutVenues(city, artistProfile);
    }
}