package com.choragi.venue.voice;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/voice")
public class TwilioVoiceController {

    @RequestMapping(value = "/connect", produces = "application/xml")
    public String handleVoiceConnect() {

        String socketUrl = "wss://8dd1-105-119-5-84.ngrok-free.app/voice-stream";

        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<Response>\n" +
                "    <Connect>\n" +
                "        <Stream url=\"" + socketUrl + "\" />\n" +
                "    </Connect>\n" +
                "</Response>";
    }
}