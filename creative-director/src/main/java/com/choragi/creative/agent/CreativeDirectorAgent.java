package com.choragi.creative.agent;

import com.google.genai.Client;
import com.google.genai.types.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

    public String generateTourPoster(String artistName, String theme) {
        log.info("Choragi Creative: Designing poster for {} - Theme: {}", artistName, theme);

        String visualPrompt = String.format(
                "High-resolution concert tour poster for '%s'. Theme: %s. Cinematic style, dynamic lighting. Vertical layout.",
                artistName, theme);


        GenerateContentConfig config = GenerateContentConfig.builder()
                .responseModalities(Arrays.asList("IMAGE"))
                .build();

        int maxRetries = 3;
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                log.info("Generating with Nano Banana (gemini-2.5-flash-image) - Attempt {}...", attempt);

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
                    log.warn("Quota (429) hit. Cooling down for {}ms before retrying...", wait);
                    try { Thread.sleep(wait); } catch (InterruptedException ignored) {}
                } else {
                    log.error("Creative Director: Image generation failed critically", e);
                    break;
                }
            }
        }
        return "IMAGE_ERROR_FALLBACK";
    }
}