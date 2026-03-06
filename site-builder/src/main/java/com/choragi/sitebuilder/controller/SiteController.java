package com.choragi.sitebuilder.controller;

import com.choragi.sitebuilder.agent.SiteBuilderAgent;
import com.choragi.sitebuilder.model.SiteRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/site")
@RequiredArgsConstructor
public class SiteController {

    private final SiteBuilderAgent siteBuilder;

    @PostMapping("/build")
    public ResponseEntity<Map<String, String>> buildSite(@RequestBody SiteRequest request) {
        String deployedUrl = siteBuilder.buildAndDeploySite(
                request.getArtistName(),
                request.getDate(),
                request.getLocation(),
                request.getPosterUrl(),
                request.getVideoUrl()
        );

        Map<String, String> response = new HashMap<>();
        response.put("status", "SUCCESS");
        response.put("liveUrl", deployedUrl);

        return ResponseEntity.ok(response);
    }
}