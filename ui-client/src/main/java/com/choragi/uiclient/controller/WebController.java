package com.choragi.uiclient.controller;

import com.choragi.uiclient.service.ChoragiOrchestrator;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Controller
@RequiredArgsConstructor
public class WebController {

    private final ChoragiOrchestrator orchestrator;


    @Value("${gemini.api.key}")
    private String geminiApiKey;

    @GetMapping("/")
    public String getDashboard(Model model) {
        model.addAttribute("geminiApiKey", geminiApiKey);
        return "index";
    }

    @PostMapping("/api/trigger-agents")
    @ResponseBody
    public ResponseEntity<String> triggerAgents(@RequestBody Map<String, String> payload) {
        String artistName = payload.getOrDefault("artistName", "Unknown Artist");
        String location = payload.get("location");
        String date = payload.get("date");

        new Thread(() -> orchestrator.startAutonomousSequence(artistName, location, date)).start();

        return ResponseEntity.ok("Choragi Protocol Initiated");
    }
}