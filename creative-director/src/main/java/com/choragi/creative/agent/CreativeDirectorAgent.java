package com.choragi.creative.agent;

import com.google.genai.Client;
import com.google.genai.types.GenerateImagesConfig;
import com.google.genai.types.GenerateImagesResponse;
import com.google.genai.types.GeneratedImage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

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

        String prompt = String.format(
                "High-resolution concert tour poster for '%s'. Theme: %s. Cinematic style.",
                artistName, theme);

        GenerateImagesConfig config = GenerateImagesConfig.builder()
                .numberOfImages(1)
                .aspectRatio("2:3")
                .build();

        try {
            GenerateImagesResponse response = client.models.generateImages("imagen-3.0-generate-001", prompt, config);

            List<GeneratedImage> images = response.generatedImages().orElse(Collections.emptyList());

            if (!images.isEmpty()) {
                GeneratedImage firstImage = images.get(0);


                try {
                    Object imgObj = firstImage.image();

                    if (imgObj != null) {
                        Object bytesData = imgObj.getClass().getMethod("imageBytes").invoke(imgObj);


                        if (bytesData instanceof java.util.Optional) {
                            bytesData = ((java.util.Optional<?>) bytesData).orElse(null);
                        }


                        if (bytesData instanceof byte[]) {
                            return "data:image/png;base64," + Base64.getEncoder().encodeToString((byte[]) bytesData);
                        } else if (bytesData instanceof String) {
                            return "data:image/png;base64," + bytesData;
                        }
                    }
                } catch (Exception ex) {
                    log.warn("Reflection extraction failed, image format may have changed.", ex);
                }
            }
            return "NO_IMAGE_GENERATED";

        } catch (Exception e) {
            log.error("Creative Director: Image generation failed", e);
            return "IMAGE_ERROR_FALLBACK";
        }
    }
}