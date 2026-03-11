package com.choragi.venue.voice;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/voice")
public class TwilioVoiceController {

    @Value("${choragi.websocket.url}")
    private String websocketUrl;

    @RequestMapping(value = "/connect", produces = "application/xml")
    public String handleVoiceConnect() {

        String socketUrl = websocketUrl;

        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<Response>\n" +
                "    <Connect>\n" +
                "        <Stream url=\"" + socketUrl + "\" />\n" +
                "    </Connect>\n" +
                "</Response>";
    }
}