package com.choragi.uiclient.controller;

import com.choragi.uiclient.service.ChoragiOrchestrator;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Controller
@RequiredArgsConstructor
public class WebController {

    private final ChoragiOrchestrator orchestrator;

    @GetMapping("/")
    public String index() {
        return "index";
    }


    @PostMapping("/api/trigger-agents")
    @ResponseBody
    public ResponseEntity<?> triggerAgents(@RequestBody Map<String, String> payload) {
        String location = payload.get("location");
        String date = payload.get("date");

        new Thread(() -> orchestrator.startAutonomousSequence(location, date)).start();

        return ResponseEntity.ok(Map.of("status", "Sequence Initiated"));
    }
}