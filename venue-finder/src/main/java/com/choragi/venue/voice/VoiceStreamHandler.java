package com.choragi.venue.voice;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.genai.Client;
import com.google.genai.types.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
public class VoiceStreamHandler extends TextWebSocketHandler {

    private final Client client;
    private final ObjectMapper mapper = new ObjectMapper();

    public VoiceStreamHandler(Client client) {
        this.client = client;
    }

    @Override
    public void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        JsonNode json = mapper.readTree(message.getPayload());
        String event = json.get("event").asText();

        switch (event) {
            case "start":
                String streamSid = json.get("start").get("streamSid").asText();
                log.info("Choragi Voice: Connecting to Gemini Live for stream: {}", streamSid);


                String modelId = "gemini-2.5-flash-native-audio-preview-12-2025";

                LiveConnectConfig connectConfig = LiveConnectConfig.builder()
                        .systemInstruction(Content.fromParts(Part.fromText(
                                "You are the Choragi AI Booking Manager. Negotiate professionally with the venue manager.")))
                        .build();


                client.async.live.connect(modelId, connectConfig)
                        .thenAccept(geminiSession -> {
                            session.getAttributes().put("geminiSession", geminiSession);
                            session.getAttributes().put("streamSid", streamSid);


                            try {
                                geminiSession.getClass().getMethod("receive", java.util.function.Consumer.class)
                                        .invoke(geminiSession, (java.util.function.Consumer<Object>) msg -> {
                                            handleGeminiResponse(session, msg);
                                        });
                            } catch (Exception e) {
                                log.error("Failed to register Gemini listener", e);
                            }

                            log.info("Choragi Voice: Gemini Live Session Established.");
                        });
                break;

            case "media":
                String payload = json.get("media").get("payload").asText();
                byte[] audioData = Base64.getDecoder().decode(payload);

                Object activeSession = session.getAttributes().get("geminiSession");
                if (activeSession != null) {
                    Blob audioBlob = Blob.builder()
                            .data(audioData)
                            .mimeType("audio/pcm;rate=16000")
                            .build();
                    try {

                        activeSession.getClass().getMethod("send", Blob.class).invoke(activeSession, audioBlob);
                    } catch (Exception e) {
                        log.debug("Streaming audio...");
                    }
                }
                break;
        }
    }

    private void handleGeminiResponse(WebSocketSession session, Object response) {
        try {

            byte[] audio = (byte[]) response.getClass().getMethod("getAudioData").invoke(response);
            String streamSid = (String) session.getAttributes().get("streamSid");

            if (audio != null && streamSid != null) {
                Map<String, Object> twilioMsg = new HashMap<>();
                twilioMsg.put("event", "media");
                twilioMsg.put("streamSid", streamSid);

                Map<String, String> media = new HashMap<>();
                media.put("payload", Base64.getEncoder().encodeToString(audio));
                twilioMsg.put("media", media);

                session.sendMessage(new TextMessage(mapper.writeValueAsString(twilioMsg)));
            }
        } catch (Exception e) {
            // No audio in this specific message (could be a setup message), ignore
        }
    }
}