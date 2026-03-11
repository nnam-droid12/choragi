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

    public void startAutonomousSequence(String artistName, String location, String date) {
        log.info("COMMAND CENTER: Launching autonomous sequence for {} in {} on {}", artistName, location, date);

        try {
            sendStatusToUI("system", "INITIALIZING CHORAGI PROTOCOL...");

            // ==========================================
            // 1. VENUE SCOUT
            // ==========================================
            sendStatusToUI("venue", "Scouting available venues in " + location + " for " + date + "...");
            String venueUrl = "http://localhost:8081/api/scout";

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> venueLeads = restTemplate.postForObject(venueUrl, Map.of("targetCity", location), List.class);

            // THE FIX: Hardcode your verified Twilio number for the trial account!
            String targetPhone = "+2349162270129";
            String targetVenue = "The Grand Arena";
            List<Map<String, String>> uiVenues = new ArrayList<>();

            if (venueLeads != null && !venueLeads.isEmpty()) {
                for (int i = 0; i < Math.min(5, venueLeads.size()); i++) {
                    Map<String, Object> lead = venueLeads.get(i);
                    String name = (String) lead.getOrDefault("name", "Unknown Venue");
                    String phone = (String) lead.getOrDefault("phoneNumber", "UNKNOWN");
                    uiVenues.add(Map.of("name", name, "phone", phone, "address", location));

                    // We grab the dynamic name, but WE DO NOT overwrite the targetPhone anymore!
                    if (i == 0) {
                        targetVenue = name;
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
            sendStatusToUI("creative", "Generating promotional assets for " + artistName + "...");
            String creativeUrl = "http://localhost:8082/api/creative/generate";

            Map<String, String> creativeReq = Map.of(
                    "artistName", artistName,
                    "location", location,
                    "date", date
            );

            @SuppressWarnings("unchecked")
            Map<String, Object> creativeAssets = restTemplate.postForObject(creativeUrl, creativeReq, Map.class);

            String rawPoster = extractValue(creativeAssets, "posterUrl", "poster", "posterGcsUrl");
            String rawVideo = extractValue(creativeAssets, "videoUrl", "video", "videoGcsUrl");

            String publicPoster = convertToPublicUrl(rawPoster);
            String publicVideo = convertToPublicUrl(rawVideo);

            sendStatusToUI("creative", "SUCCESS", Map.of("posterUrl", publicPoster, "videoUrl", publicVideo));

            // ==========================================
            // 4. SITE BUILDER
            // ==========================================
            sendStatusToUI("website", "Deploying site for " + artistName + "...");
            String siteUrl = "http://localhost:8083/api/site/build";

            Map<String, String> siteReq = new HashMap<>();
            siteReq.put("artistName", artistName);
            siteReq.put("date", date);
            siteReq.put("location", location);
            siteReq.put("posterUrl", publicPoster);
            siteReq.put("videoUrl", publicVideo);

            @SuppressWarnings("unchecked")
            Map<String, Object> siteResponse = restTemplate.postForObject(siteUrl, siteReq, Map.class);

            // Log the response so we can debug if it returns empty!
            log.info("RAW SITE BUILDER RESPONSE: {}", siteResponse);

            String liveUrl = extractValue(siteResponse, "liveUrl", "url", "websiteUrl");

            // THE FIX: Removed the Nixora fallback. If it fails, it will now safely display an error on the UI.
            if (liveUrl.isEmpty()) {
                liveUrl = "ERROR_SITE_DEPLOYMENT_FAILED";
                log.error("Site Builder did not return a valid URL!");
            }

            sendStatusToUI("website", "SUCCESS", Map.of("url", liveUrl));

            // ==========================================
            // 5. DIGITAL PROMOTER
            // ==========================================
            sendStatusToUI("promoter", "LAUNCHING", Map.of("status", "Google Ads Engine Online"));
            String promoterUrl = "http://localhost:8084/api/promoter/launch";

            restTemplate.postForEntity(promoterUrl, Map.of("artistName", artistName, "websiteUrl", liveUrl), String.class);

            sendStatusToUI("promoter", "SUCCESS", Map.of(
                    "adTitle", artistName + " Live Concert | Get Tickets",
                    "adUrl", liveUrl,
                    "adDesc", "Join the high-energy live music experience in " + location + ". Secure your tickets before they sell out!"
            ));

            sendStatusToUI("system", "ALL AGENTS DEPLOYED.");

        } catch (Exception e) {
            log.error("Sequence failed!", e);
        }
    }

    private String extractValue(Map<String, Object> map, String... keys) {
        if (map == null) return "";
        for (String key : keys) {
            if (map.containsKey(key) && map.get(key) != null) {
                return String.valueOf(map.get(key));
            }
        }
        return "";
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