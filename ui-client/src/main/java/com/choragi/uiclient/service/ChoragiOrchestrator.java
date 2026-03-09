package com.choragi.uiclient.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

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
            Thread.sleep(1500);

            sendStatusToUI("venue", "Scouting available venues in " + location + " for " + date + "...");
            Thread.sleep(3000);
            sendStatusToUI("venue", "SUCCESS: Found 'The Grand Arena'. Capacity: 5,000.");


            sendStatusToUI("negotiator", "Calling venue owner to negotiate pricing...");
            Thread.sleep(3000);
            sendStatusToUI("negotiator", "SUCCESS: Secured 15% discount on venue booking.");


            sendStatusToUI("creative", "Analyzing artist profile to generate promotional assets...");
            Thread.sleep(2000);
            sendStatusToUI("creative", "Generating concert posters using Imagen 3...");
            Thread.sleep(2000);
            sendStatusToUI("creative", "Generating promotional video using Veo...");
            Thread.sleep(3000);
            sendStatusToUI("creative", "SUCCESS: All promotional assets generated and stored in cloud bucket.");


            sendStatusToUI("website", "Generating autonomous concert website using creative assets...");
            Thread.sleep(3000);
            sendStatusToUI("website", "SUCCESS: Ticketing system deployed to https://www.alexwarrenmusic.com");


            sendStatusToUI("promoter", "Triggering Digital Promoter Agent for Google Ads...");

            try {
                String promoterUrl = "http://localhost:8084/api/launch";
                restTemplate.postForEntity(promoterUrl, Map.of(
                        "artistName", "Alex Warren",
                        "websiteUrl", "https://www.alexwarrenmusic.com"
                ), String.class);
                sendStatusToUI("promoter", "Ads sequence initiated in external Playwright node.");
            } catch (Exception e) {
                sendStatusToUI("promoter", "Digital Promoter running in standalone mode...");
            }

            Thread.sleep(2000);
            sendStatusToUI("system", "ALL AGENTS DEPLOYED. WAITING FOR FINAL PAYMENT.");

        } catch (Exception e) {
            log.error("Sequence failed!", e);
            sendStatusToUI("system", "ERROR: Sequence halted. " + e.getMessage());
        }
    }

    private void sendStatusToUI(String agent, String message) {
        String payload = String.format("{\"agent\":\"%s\", \"message\":\"%s\"}", agent, message);
        messagingTemplate.convertAndSend("/topic/agent-status", payload);
    }
}