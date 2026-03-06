package com.choragi.creative.controller;

import com.choragi.creative.agent.AssetGeneratorAgent;
import com.choragi.creative.model.DigitalPressKit;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/creative")
@RequiredArgsConstructor
public class CreativeController {

    private final AssetGeneratorAgent assetGenerator;

    @PostMapping("/generate")
    public DigitalPressKit generateAssets(@RequestBody Map<String, String> request) {
        String artistName = request.getOrDefault("artistName", "Unknown Artist");
        String theme = request.getOrDefault("theme", "Neon cyberpunk night city, highly detailed");

        return assetGenerator.createPressKit(artistName, theme);
    }
}