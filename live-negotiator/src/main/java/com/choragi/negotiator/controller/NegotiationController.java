package com.choragi.negotiator.controller;

import com.choragi.negotiator.agent.VoiceNegotiatorAgent;
import com.choragi.negotiator.model.VenueLeadRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/negotiation")
@RequiredArgsConstructor
@Slf4j
public class NegotiationController {

    private final VoiceNegotiatorAgent negotiatorAgent;

    @PostMapping("/start")
    public ResponseEntity<String> startNegotiation(@RequestBody VenueLeadRequest request) {
        log.info("Received command to negotiate with: {}", request.getVenueName());

        // We now pass BOTH the venue name and the phone number to the agent
        boolean success = negotiatorAgent.startNegotiation(request.getVenueName(), request.getPhoneNumber());

        if (success) {
            return ResponseEntity.ok("Negotiation dispatched for " + request.getVenueName());
        } else {
            return ResponseEntity.internalServerError().body("Failed to dispatch telecom routing.");
        }
    }
}