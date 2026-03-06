package com.choragi.creative.agent;

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
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
@RequiredArgsConstructor
public class CreativeDirectorAgent {

    private final Client client;

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
                log.info("Generating with Nano Banana (Attempt {})...", attempt);
                GenerateContentResponse response = client.models.generateContent(
                        "gemini-2.5-flash-image",
                        Content.builder().role("user").parts(Collections.singletonList(
                                Part.builder().text(visualPrompt).build()
                        )).build(),
                        config
                );

                return response.candidates().orElse(Collections.emptyList()).stream()
                        .map(c -> c.content().orElse(null))
                        .filter(content -> content != null)
                        .map(content -> content.parts().orElse(Collections.emptyList()))
                        .flatMap(List::stream)
                        .filter(part -> part.inlineData().isPresent())
                        .map(part -> part.inlineData().get().data().orElse(null))
                        .filter(bytes -> bytes != null && bytes.length > 0)
                        .findFirst()
                        .map(imageBytes -> "data:image/png;base64," + Base64.getEncoder().encodeToString(imageBytes))
                        .orElse("NO_IMAGE_GENERATED");

            } catch (Exception e) {
                if (e.getMessage().contains("429") || e.getMessage().contains("Resource exhausted")) {
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

        String videoPrompt = String.format(
                "A dynamic, high-energy promotional video ad for a live music concert. " +
                        "Visual style: %s. " +
                        "The video should feel like a commercial trailer. " +
                        "Audio MUST include a professional voiceover saying: 'Hurray! %s will be live in town at %s on %s! " +
                        "Bringing the musical magic and the glow of live music. Come and be energized!' " +
                        "Background audio should include a cinematic hype soundtrack and an excited cheering crowd. " +
                        "Visuals: cinematic shots of an excited concert crowd, dramatic stage lighting, and a hyped atmosphere.",
                theme, artistName, location, date);

        String gcsOutput = "gs://" + bucketName + "/videos/";

        try {
            log.info("Sending video ad request to Veo 3.1 Fast...");

            com.google.genai.types.GenerateVideosOperation operation = client.models.generateVideos(
                    "veo-3.1-fast-generate-001",
                    com.google.genai.types.GenerateVideosSource.builder()
                            .prompt(videoPrompt)
                            .build(),
                    com.google.genai.types.GenerateVideosConfig.builder()
                            .aspectRatio("16:9")
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