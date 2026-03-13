package com.choragi.venue.controller;

import com.choragi.venue.agent.VenueScoutAgent;
import com.choragi.venue.model.VenueLead;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/scout")
@RequiredArgsConstructor
@Slf4j
public class VenueController {

    private final VenueScoutAgent scoutAgent;
    private final RestTemplate restTemplate = new RestTemplate();

    @PostMapping
    public List<VenueLead> startScouting(@RequestBody Map<String, Object> tourState) {
        String city = (String) tourState.get("targetCity");
        String artistProfile = "Indie Rock band with a high-energy live show, looking for mid-size venues (300-800 capacity).";

        log.info("Initiating Venue Scout for city: {}", city);
        List<VenueLead> leads = scoutAgent.scoutVenues(city, artistProfile);

        log.info("Scouted {} venues. Firing payload to Live-Negotiator...", leads.size());


        for (VenueLead lead : leads) {
            if (lead.getPhoneNumber() != null && !lead.getPhoneNumber().equals("UNKNOWN")) {
//                triggerNegotiator(lead);
                break;
            }
        }

        return leads;
    }

    private void triggerNegotiator(VenueLead lead) {

        String negotiatorUrl = "https://live-negotiator-4j2p5vomta-uc.a.run.app/api/negotiation/start";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, String> payload = new HashMap<>();
        payload.put("venueName", lead.getName());


        log.info("Real venue number is {}, but overriding to +2349162270129 for Twilio Trial testing.", lead.getPhoneNumber());
        payload.put("phoneNumber", "+2349162270129");

        HttpEntity<Map<String, String>> request = new HttpEntity<>(payload, headers);

        try {
            restTemplate.postForObject(negotiatorUrl, request, String.class);
            log.info("Successfully commanded Live-Negotiator to dial.");
        } catch (Exception e) {
            log.error("Failed to reach Live-Negotiator service on 8080: {}", e.getMessage());
        }
    }
}