package com.choragi.negotiator.agent;

import com.choragi.venue.model.VenueLead;
import com.google.genai.Client;
import com.google.genai.types.Content;
import com.google.genai.types.LiveConnectConfig;
import com.google.genai.types.Part;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class VoiceNegotiatorAgent {

    private final Client client;

    public void negotiateVenues(List<VenueLead> venues, String artistProfile) {
        log.info("Choragi Negotiator: Preparing voice sessions for {} discovered venues.", venues.size());

        for (VenueLead venue : venues) {
            log.info("Choragi Negotiator: Queueing call for {} at {}", venue.getName(), venue.getAddress());

            String instruction = String.format(
                    "You are the Choragi AI Booking Agent. Call %s. " +
                            "Reasoning for lead: %s. Artist Profile: %s. " +
                            "Goal: Secure a weekend date in June 2026 with a 70/30 door split.",
                    venue.getName(), venue.getReasoning(), artistProfile
            );

            LiveConnectConfig config = LiveConnectConfig.builder()
                    .systemInstruction(Content.fromParts(Part.fromText(instruction)))
                    .build();

            log.info("Choragi Negotiator: Negotiation parameters locked for {}", venue.getName());
        }
    }
}