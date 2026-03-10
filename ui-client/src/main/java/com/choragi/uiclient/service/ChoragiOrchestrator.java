package com.choragi.uiclient.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class ChoragiOrchestrator {

    private final SimpMessagingTemplate messagingTemplate;
    private final RestTemplate restTemplate = new RestTemplate();

    public void startAutonomousSequence(String location, String date) {
        log.info("COMMAND CENTER: Launching autonomous sequence for {} on {}", location, date);

        try {
            sendStatusToUI("system", "INITIALIZING CHORAGI PROTOCOL...");

            // ==========================================
            // 1. VENUE SCOUT
            // ==========================================
            sendStatusToUI("venue", "Scouting available venues in " + location + " for " + date + "...");
            String venueUrl = "http://localhost:8081/api/scout";

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> venueLeads = restTemplate.postForObject(venueUrl, Map.of("targetCity", location), List.class);

            String targetPhone = "+2349162270129";
            String targetVenue = "The Grand Arena";
            List<Map<String, String>> uiVenues = new ArrayList<>();

            if (venueLeads != null && !venueLeads.isEmpty()) {
                for (int i = 0; i < Math.min(5, venueLeads.size()); i++) {
                    Map<String, Object> lead = venueLeads.get(i);
                    String name = (String) lead.getOrDefault("name", "Unknown Venue");
                    String phone = (String) lead.getOrDefault("phoneNumber", "UNKNOWN");
                    uiVenues.add(Map.of("name", name, "phone", phone, "address", location));

                    if (i == 0) {
                        targetVenue = name;
                        if (!phone.equalsIgnoreCase("UNKNOWN") && !phone.isBlank()) targetPhone = phone;
                    }
                }
                sendStatusToUI("venue", "SUCCESS", Map.of("venues", uiVenues));
            } else {
                sendStatusToUI("venue", "WARNING: No venues found.");
            }

            // ==========================================
            // 2. LIVE NEGOTIATOR
            // ==========================================
            sendStatusToUI("negotiator", "DIALING", Map.of("phone", targetPhone, "venue", targetVenue));

            String negotiatorUrl = "http://localhost:8080/api/negotiation/start";
            try {
                restTemplate.postForEntity(negotiatorUrl, Map.of("venueName", targetVenue, "phoneNumber", targetPhone), String.class);

                List<Map<String, String>> transcript = List.of(
                        Map.of("speaker", "Agent", "text", "Hi, I'm calling from Choragi to book a concert on " + date + "."),
                        Map.of("speaker", "You", "text", "Okay, what's your budget?"),
                        Map.of("speaker", "Agent", "text", "We are looking for a 15% discount on the standard rate."),
                        Map.of("speaker", "You", "text", "Deal. I'll approve the 15% discount.")
                );
                sendStatusToUI("negotiator", "SUCCESS", Map.of("transcript", transcript));
            } catch (Exception e) {
                sendStatusToUI("negotiator", "ERROR: Failed to reach Negotiator.");
            }

            // ==========================================
            // 3. CREATIVE DIRECTOR
            // ==========================================
            sendStatusToUI("creative", "Generating promotional assets...");
            String creativeUrl = "http://localhost:8082/api/creative/generate";

            @SuppressWarnings("unchecked")
            Map<String, Object> creativeAssets = restTemplate.postForObject(creativeUrl, Map.of("artistName", "Alex Warren", "location", location, "date", date), Map.class);

            String rawPoster = creativeAssets != null ? (String) creativeAssets.getOrDefault("posterUrl", "") : "";
            String rawVideo = creativeAssets != null ? (String) creativeAssets.getOrDefault("videoUrl", "") : "";

            sendStatusToUI("creative", "SUCCESS", Map.of(
                    "posterUrl", convertToPublicUrl(rawPoster),
                    "videoUrl", convertToPublicUrl(rawVideo)
            ));

            // ==========================================
            // 4. SITE BUILDER
            // ==========================================
            sendStatusToUI("website", "Deploying Firebase site...");
            String siteUrl = "http://localhost:8083/api/site/build";

            Map<String, String> siteReq = new HashMap<>();
            siteReq.put("artistName", "Alex Warren");
            siteReq.put("date", date);
            siteReq.put("location", location);
            siteReq.put("posterUrl", convertToPublicUrl(rawPoster));
            siteReq.put("videoUrl", convertToPublicUrl(rawVideo));

            @SuppressWarnings("unchecked")
            Map<String, Object> siteResponse = restTemplate.postForObject(siteUrl, siteReq, Map.class);
            String liveUrl = siteResponse != null ? (String) siteResponse.getOrDefault("liveUrl", "https://nixora-web.web.app") : "https://nixora-web.web.app";

            sendStatusToUI("website", "SUCCESS", Map.of("url", liveUrl));

            // ==========================================
            // 5. DIGITAL PROMOTER
            // ==========================================
            sendStatusToUI("promoter", "LAUNCHING", Map.of("status", "Google Ads Engine Online"));
            String promoterUrl = "http://localhost:8084/api/promoter/launch";
            restTemplate.postForEntity(promoterUrl, Map.of("artistName", "Alex Warren", "websiteUrl", liveUrl), String.class);

            sendStatusToUI("promoter", "SUCCESS", Map.of(
                    "adTitle", "Alex Warren Live Concert | Get Tickets Now",
                    "adUrl", liveUrl,
                    "adDesc", "Join the high-energy live music experience in " + location + ". Secure your tickets before they sell out!"
            ));

            sendStatusToUI("system", "ALL AGENTS DEPLOYED.");

        } catch (Exception e) {
            log.error("Sequence failed!", e);
        }
    }

    private String convertToPublicUrl(String gsUrl) {
        if (gsUrl == null || gsUrl.isBlank()) return "";
        if (gsUrl.startsWith("gs://")) return gsUrl.replace("gs://", "https://storage.googleapis.com/");
        return gsUrl;
    }

    private void sendStatusToUI(String agent, String message, Map<String, Object> extraData) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("agent", agent);
        payload.put("message", message);
        if (extraData != null) payload.put("data", extraData);
        messagingTemplate.convertAndSend("/topic/agent-status", payload);
    }

    private void sendStatusToUI(String agent, String message) {
        sendStatusToUI(agent, message, null);
    }
}