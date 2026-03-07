package com.choragi.promoter.controller;

import com.choragi.promoter.agent.DigitalPromoterAgent;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/promoter")
@RequiredArgsConstructor
public class PromoterController {

    private final DigitalPromoterAgent promoterAgent;

    @PostMapping("/launch")
    public ResponseEntity<Map<String, String>> launchCampaign(@RequestBody Map<String, String> request) {
        String artistName = request.getOrDefault("artistName", "Alex Warren");
        String websiteUrl = request.getOrDefault("websiteUrl", "https://nixora-web.web.app");

        promoterAgent.launchFacebookAd(artistName, websiteUrl);

        return ResponseEntity.ok(Map.of(
                "status", "SUCCESS",
                "message", "UI Navigator agent has taken control of the browser."
        ));
    }
}