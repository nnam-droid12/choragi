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

import java.io.ByteArrayOutputStream;
import java.lang.reflect.Method;
import java.util.Arrays;
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

                // Initialize our Voice Activity Detection (VAD) buffers
                session.getAttributes().put("audioBuffer", new ByteArrayOutputStream());
                session.getAttributes().put("silenceCount", 0);
                session.getAttributes().put("hasSpoken", false);

                String modelId = "gemini-2.0-flash-live-preview-04-09";

                LiveConnectConfig connectConfig = LiveConnectConfig.builder()
                        .responseModalities(com.google.genai.types.Modality.Known.AUDIO)
                        .systemInstruction(Content.fromParts(Part.fromText(
                                "You are the Choragi AI Booking Manager. Negotiate professionally with the venue manager. Keep your responses conversational and brief.")))
                        .build();

                client.async.live.connect(modelId, connectConfig)
                        .thenAccept(geminiSession -> {
                            session.getAttributes().put("geminiSession", geminiSession);
                            session.getAttributes().put("streamSid", streamSid);

                            try {
                                Method clientMethod = geminiSession.getClass().getMethod("sendClientContent", LiveSendClientContentParameters.class);
                                session.getAttributes().put("clientContentMethod", clientMethod);

                                // 1. SEND THE GREETING
                                LiveSendClientContentParameters nudgeParams = LiveSendClientContentParameters.builder()
                                        .turns(Arrays.asList(Content.builder()
                                                .role("user")
                                                .parts(Arrays.asList(Part.fromText("Please say: Hello, I am the Booking Manager calling from Choragi.")))
                                                .build()))
                                        .turnComplete(true)
                                        .build();
                                clientMethod.invoke(geminiSession, nudgeParams);
                                log.info("Greeting nudge sent successfully.");
                            } catch (Exception e) {
                                log.warn("Failed to setup connection.");
                            }

                            // 2. LISTEN FOR RESPONSES
                            try {
                                geminiSession.getClass().getMethod("receive", java.util.function.Consumer.class)
                                        .invoke(geminiSession, (java.util.function.Consumer<Object>) msg -> {
                                            handleGeminiResponse(session, msg);
                                        });
                            } catch (Exception e) {
                                log.error("Failed to register choragi listener", e);
                            }

                            log.info("Choragi Voice: Gemini Live Session Established.");
                        });
                break;

            case "media":
                String payload = json.get("media").get("payload").asText();

                // Decode Twilio 8kHz Mu-law directly into 8kHz PCM (No artificial upsampling!)
                byte[] twilioAudio = Base64.getDecoder().decode(payload);
                byte[] pcmAudio = transcodeMuLawToPcm8k(twilioAudio);

                Object activeSession = session.getAttributes().get("geminiSession");
                Method clientContentMethod = (Method) session.getAttributes().get("clientContentMethod");

                if (activeSession != null && clientContentMethod != null) {
                    ByteArrayOutputStream audioBuffer = (ByteArrayOutputStream) session.getAttributes().get("audioBuffer");
                    boolean hasSpoken = (Boolean) session.getAttributes().get("hasSpoken");
                    int silenceCount = (Integer) session.getAttributes().get("silenceCount");

                    try { audioBuffer.write(pcmAudio); } catch (Exception ignore) {}

                    double rms = calculateRMS(pcmAudio);

                    // TUNED: Lowered threshold from 400 to 50 so you don't have to shout!
                    if (rms > 50) {
                        session.getAttributes().put("hasSpoken", true);
                        session.getAttributes().put("silenceCount", 0);
                    } else {
                        silenceCount++;
                        session.getAttributes().put("silenceCount", silenceCount);

                        // TUNED: Increased silence timeout to 75 frames (1.5 seconds).
                        // Gives you time to breathe between words without interrupting you.
                        if (silenceCount > 75) {
                            // 8000 bytes = 0.5 seconds of 8kHz audio (ignores quick throat clears)
                            if (hasSpoken && audioBuffer.size() > 8000) {
                                byte[] sentenceAudio = audioBuffer.toByteArray();

                                try {
                                    LiveSendClientContentParameters audioParams = LiveSendClientContentParameters.builder()
                                            .turns(Arrays.asList(Content.builder()
                                                    .role("user")
                                                    .parts(Arrays.asList(Part.builder()
                                                            .inlineData(Blob.builder()
                                                                    // Explicitly tell Gemini this is 8000Hz audio
                                                                    .mimeType("audio/pcm;rate=8000")
                                                                    .data(sentenceAudio)
                                                                    .build())
                                                            .build()))
                                                    .build()))
                                            .turnComplete(true)
                                            .build();

                                    clientContentMethod.invoke(activeSession, audioParams);
                                    log.info("VAD: User finished speaking. Audio sent to Gemini.");
                                } catch (Exception e) {
                                    log.debug("Failed to send buffered audio.");
                                }
                            }
                            // Reset buffers for the next sentence
                            audioBuffer.reset();
                            session.getAttributes().put("silenceCount", 0);
                            session.getAttributes().put("hasSpoken", false);
                        }
                    }
                }
                break;
        }
    }

    private void handleGeminiResponse(WebSocketSession session, Object response) {
        try {
            if (response instanceof LiveServerMessage) {
                LiveServerMessage msg = (LiveServerMessage) response;

                if (msg.serverContent().isPresent() && msg.serverContent().get().modelTurn().isPresent()) {
                    Content modelTurn = msg.serverContent().get().modelTurn().get();

                    if (modelTurn.parts().isPresent()) {
                        for (Part part : modelTurn.parts().get()) {
                            if (part.inlineData().isPresent() && part.inlineData().get().data().isPresent()) {

                                // Gemini sends 24kHz PCM. Transcode to 8kHz Mu-Law for Twilio's speaker.
                                byte[] geminiPcm = part.inlineData().get().data().get();
                                byte[] twilioMuLaw = transcodePcmToMuLaw(geminiPcm);
                                String streamSid = (String) session.getAttributes().get("streamSid");

                                if (twilioMuLaw != null && streamSid != null) {
                                    Map<String, Object> twilioMsg = new HashMap<>();
                                    twilioMsg.put("event", "media");
                                    twilioMsg.put("streamSid", streamSid);

                                    Map<String, String> media = new HashMap<>();
                                    media.put("payload", Base64.getEncoder().encodeToString(twilioMuLaw));
                                    twilioMsg.put("media", media);

                                    session.sendMessage(new TextMessage(mapper.writeValueAsString(twilioMsg)));
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {}
    }

    // --- REAL-TIME VOLUME DETECTOR ---
    private static double calculateRMS(byte[] pcm) {
        long sum = 0;
        int count = 0;
        for (int i = 0; i < pcm.length - 1; i += 2) {
            int sample = (pcm[i] & 0xFF) | (pcm[i + 1] << 8);
            if ((sample & 0x8000) != 0) sample |= 0xFFFF0000;
            sum += (long) sample * sample;
            count++;
        }
        return count == 0 ? 0 : Math.sqrt((double) sum / count);
    }

    // --- REAL-TIME AUDIO TRANSCODERS ---

    // NEW: Straight 8kHz Mu-law to 8kHz PCM conversion for maximum clarity
    private static byte[] transcodeMuLawToPcm8k(byte[] mulaw8k) {
        byte[] pcm8k = new byte[mulaw8k.length * 2];
        for (int i = 0, j = 0; i < mulaw8k.length; i++) {
            byte ulaw = (byte) ~mulaw8k[i];
            int sign = (ulaw & 0x80);
            int exponent = (ulaw & 0x70) >> 4;
            int data = ulaw & 0x0F;
            int sample = (data << 3) + 132;
            sample <<= exponent;
            sample -= 132;
            if (sign != 0) sample = -sample;

            pcm8k[j++] = (byte) (sample & 0xFF);
            pcm8k[j++] = (byte) ((sample >> 8) & 0xFF);
        }
        return pcm8k;
    }

    private static byte[] transcodePcmToMuLaw(byte[] pcm24k) {
        int outLen = pcm24k.length / 6;
        byte[] mulaw = new byte[outLen];
        for (int i = 0, j = 0; i < pcm24k.length - 1 && j < outLen; i += 6, j++) {
            int pcm = (pcm24k[i] & 0xFF) | (pcm24k[i + 1] << 8);

            int sign = (pcm >> 8) & 0x80;
            if (sign != 0) pcm = -pcm;
            if (pcm > 32635) pcm = 32635;
            pcm += 0x84;
            int exponent = 7;
            for (int expMask = 0x4000; (pcm & expMask) == 0 && exponent > 0; exponent--, expMask >>= 1) {}
            int mantissa = (pcm >> (exponent + 3)) & 0x0F;
            byte ulawByte = (byte) (sign | (exponent << 4) | mantissa);
            mulaw[j] = (byte) ~ulawByte;
        }
        return mulaw;
    }
}