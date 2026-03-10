package com.choragi.creative.agent;

import com.choragi.creative.tools.CloudStorageUploader;
import com.google.genai.Client;
import com.google.genai.types.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class CreativeDirectorAgent {

    private final Client client;
    private final CloudStorageUploader cloudStorageUploader;

    @Value("${choragi.storage.bucket-name}")
    private String bucketName;

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
                log.info("Generating with Nano Banana 2 (Attempt {})...", attempt);
                GenerateContentResponse response = client.models.generateContent(
                        "gemini-2.5-flash-image",
                        Content.builder().role("user").parts(Collections.singletonList(
                                Part.builder().text(visualPrompt).build()
                        )).build(),
                        config
                );

                // Extract the raw image bytes
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
                    // THE FIX: Convert bytes to Base64 and call your exact method name!
                    String base64Image = Base64.getEncoder().encodeToString(imageBytes);
                    return cloudStorageUploader.uploadBase64Image(base64Image, artistName);
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

        String safeTheme = theme.replaceAll("(?i)psychedelic", "retro kaleidoscope");
        String uniqueId = java.util.UUID.randomUUID().toString().substring(0, 8);

        String videoPrompt = String.format(
                "[Unique Sequence: %s] Cinematic music video of a live rock band performing on a massive concert stage. " +
                        "A lead singer is passionately holding a microphone, a guitarist is playing an electric guitar, and a drummer is playing a drum set. " +
                        "Vibrant neon stage lights and sweeping lasers illuminating a huge, energetic cheering crowd. " +
                        "Visual theme: %s. 4k resolution, photorealistic, highly detailed.",
                uniqueId, safeTheme);

        String gcsOutput = "gs://" + bucketName + "/videos/";

        try {
            log.info("Sending video ad request to Veo...");

            com.google.genai.types.GenerateVideosOperation operation = client.models.generateVideos(
                    "veo-3.1-generate-preview",
                    com.google.genai.types.GenerateVideosSource.builder().prompt(videoPrompt).build(),
                    com.google.genai.types.GenerateVideosConfig.builder()
                            .aspectRatio("16:9")
                            .personGeneration("allow_adult")
                            .outputGcsUri(gcsOutput)
                            .generateAudio(true)
                            .build()
            );

            log.info("Video ad is generating with audio. Polling for completion...");

            while (!operation.done().orElse(false)) {
                java.util.concurrent.TimeUnit.SECONDS.sleep(15);
                operation = client.operations.getVideosOperation(operation, null);
                log.info("Still generating video ad...");
            }

            if (operation.error().isPresent()) {
                Object errorObj = operation.error().get();
                log.error("Veo Render Failed! Google Cloud Reason: {}", errorObj.toString());
                return "VIDEO_ERROR_FALLBACK";
            }

            String generatedVideoUri = operation.response()
                    .flatMap(com.google.genai.types.GenerateVideosResponse::generatedVideos)
                    .flatMap(videos -> videos.stream().findFirst())
                    .flatMap(com.google.genai.types.GeneratedVideo::video)
                    .flatMap(com.google.genai.types.Video::uri)
                    .orElse("NO_VIDEO_GENERATED");

            log.info("Video ad generation complete: {}", generatedVideoUri);

            if (generatedVideoUri.startsWith("gs://")) {
                return generatedVideoUri.replace("gs://", "https://storage.googleapis.com/");
            }
            return generatedVideoUri;

        } catch (Exception e) {
            log.error("Creative Director: Video ad generation failed", e);
            return "VIDEO_ERROR_FALLBACK";
        }
    }
}