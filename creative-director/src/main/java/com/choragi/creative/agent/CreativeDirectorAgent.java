package com.choragi.creative.agent;

import com.google.genai.Client;
import com.google.genai.types.*;
import com.google.auth.oauth2.GoogleCredentials;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class CreativeDirectorAgent {

    private final Client client;

    @Value("${choragi.storage.bucket-name:choragi-assets-bucket}")
    private String bucketName;

    @Autowired
    public CreativeDirectorAgent(Client client) {
        this.client = client;
    }

    public String generateTourPoster(String artistName, String theme, String date, String location) {
        log.info("Choragi Creative: Designing poster for {}...", artistName);

        String visualPrompt = String.format(
                "A highly realistic, professional concert tour poster for '%s'. Theme: %s. " +
                        "The poster MUST prominently feature the exact text: '%s' and '%s'. " +
                        "Photographic quality, 8k resolution, raw photography, dramatic lighting, sharp focus. " +
                        "It should look like a genuine, printed live music event poster, not an AI painting.",
                artistName, theme, date, location);

        GenerateContentConfig config = GenerateContentConfig.builder()
                .responseModalities(Arrays.asList("IMAGE"))
                .build();

        int maxRetries = 3;
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                log.info("Generating with Gemini Flash Image (Attempt {})...", attempt);
                GenerateContentResponse response = client.models.generateContent(
                        "gemini-2.5-flash-image",
                        Content.builder().role("user").parts(Collections.singletonList(
                                Part.builder().text(visualPrompt).build()
                        )).build(),
                        config
                );

                byte[] imageBytes = response.candidates().orElse(Collections.emptyList()).stream()
                        .map(c -> c.content().orElse(null))
                        .filter(content -> content != null)
                        .map(content -> content.parts().orElse(Collections.emptyList()))
                        .flatMap(List::stream)
                        .filter(part -> part.inlineData().isPresent())
                        .map(part -> part.inlineData().get().data().orElse(null))
                        .filter(bytes -> bytes != null && bytes.length > 0)
                        .findFirst()
                        .orElse(null);

                if (imageBytes != null) {
                    return "data:image/png;base64," + Base64.getEncoder().encodeToString(imageBytes);
                }

            } catch (Exception e) {
                if (e.getMessage() != null && (e.getMessage().contains("429") || e.getMessage().contains("Resource exhausted"))) {
                    long wait = 2000L * attempt;
                    log.warn("Quota (429) hit. Cooling down for {}ms...", wait);
                    try { Thread.sleep(wait); } catch (InterruptedException ignored) {}
                } else {
                    log.error("Image generation failed critically", e);
                    break;
                }
            }
        }
        return "IMAGE_ERROR_FALLBACK";
    }

    public String generatePromoVideo(String artistName, String theme, String date, String location) {
        log.info("Choragi Creative: Directing promo video ad for {} at {}...", artistName, location);

        String safeTheme = theme != null ? theme.replaceAll("(?i)psychedelic", "retro kaleidoscope") : "cinematic";
        String uniqueId = java.util.UUID.randomUUID().toString().substring(0, 8);

        String videoPrompt = String.format(
                "[Unique Sequence: %s] Cinematic music video of a massive concert stage. " +
                        "Vibrant neon stage lights, smoke machines, and sweeping lasers illuminating empty microphone stands and a glowing drum set. " +
                        "Visual theme: %s. 4k resolution, photorealistic, highly detailed.",
                uniqueId, safeTheme);

        try {
            log.info("Sending video request directly to Vertex AI Veo 3.0 Fast...");

            // 1. Get Google Cloud Auth Token
            GoogleCredentials credentials = GoogleCredentials.getApplicationDefault()
                    .createScoped(Collections.singletonList("https://www.googleapis.com/auth/cloud-platform"));
            credentials.refreshIfExpired();
            String token = credentials.getAccessToken().getTokenValue();

            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(token);
            headers.setContentType(MediaType.APPLICATION_JSON);

            // 2. Dynamically grab your true Project ID (defaults to nixora-project if running locally without env vars)
            String projectId = System.getenv("GOOGLE_CLOUD_PROJECT");
            if (projectId == null || projectId.isEmpty()) {
                projectId = "nixora-project";
            }

            // 3. Use the exact Veo 3.0 Fast URL
            String url = "https://us-central1-aiplatform.googleapis.com/v1/projects/" + projectId + "/locations/us-central1/publishers/google/models/veo-3.0-fast-generate-001:predictLongRunning";
            String gcsUri = "gs://" + bucketName + "/promo_" + uniqueId + ".mp4";

            // 4. Construct the exact JSON Body mandated by the Vertex AI docs
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("instances", Collections.singletonList(Collections.singletonMap("prompt", videoPrompt)));

            Map<String, Object> params = new HashMap<>();
            params.put("storageUri", gcsUri);
            params.put("sampleCount", 1);
            params.put("aspectRatio", "16:9");
            params.put("resolution", "720p");
            params.put("durationSeconds", 4); // Required for Veo 3
            params.put("generateAudio", false); // Required for Veo 3

            requestBody.put("parameters", params);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            // 5. Fire the REST Request
            log.info("Starting Veo 3.0 Fast Operation on Project: {}", projectId);
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, entity, Map.class);

            if (response.getBody() == null || !response.getBody().containsKey("name")) {
                log.error("Failed to get Operation Name from Vertex API.");
                return "VIDEO_ERROR_FALLBACK";
            }

            String operationName = response.getBody().get("name").toString();
            String pollUrl = "https://us-central1-aiplatform.googleapis.com/v1/" + operationName;

            log.info("Operation started: {}. Polling every 15 seconds...", operationName);

            // 6. Poll for completion
            while (true) {
                Thread.sleep(15000);
                ResponseEntity<Map> pollResp = restTemplate.exchange(pollUrl, HttpMethod.GET, new HttpEntity<>(headers), Map.class);

                if (pollResp.getBody() != null) {
                    Boolean done = (Boolean) pollResp.getBody().get("done");
                    if (Boolean.TRUE.equals(done)) {
                        if (pollResp.getBody().containsKey("error")) {
                            log.error("Vertex Veo Failed during processing: {}", pollResp.getBody().get("error"));
                            return "VIDEO_ERROR_FALLBACK";
                        }
                        log.info("Video Generation Complete!");
                        break;
                    }
                }
            }

            return "https://storage.googleapis.com/" + bucketName + "/promo_" + uniqueId + ".mp4";

        } catch (org.springframework.web.client.HttpStatusCodeException e) {
            // THE CRITICAL FIX: If Google rejects it, print the exact reason to the logs!
            log.error("VERTEX AI REJECTED THE REQUEST. Status: {} - Body: {}", e.getStatusCode(), e.getResponseBodyAsString());
            return "VIDEO_ERROR_FALLBACK";
        } catch (Exception e) {
            log.error("Creative Director Error: {}", e.getMessage(), e);
            return "VIDEO_ERROR_FALLBACK";
        }
    }
}