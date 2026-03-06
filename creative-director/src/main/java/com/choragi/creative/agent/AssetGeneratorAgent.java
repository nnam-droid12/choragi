package com.choragi.creative.agent;

import com.choragi.creative.model.DigitalPressKit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class AssetGeneratorAgent {

    private final CreativeDirectorAgent creativeDirector;

    public DigitalPressKit createPressKit(String artistName, String theme) {
        log.info("Asset Generator: Commissioning poster for {}...", artistName);

        String posterBase64 = creativeDirector.generateTourPoster(artistName, theme);

        boolean success = !posterBase64.equals("NO_IMAGE_GENERATED") && !posterBase64.equals("IMAGE_ERROR_FALLBACK");

        return DigitalPressKit.builder()
                .artistName(artistName)
                .theme(theme)
                .posterBase64(posterBase64)
                .status(success ? "SUCCESS" : "FAILED")
                .build();
    }
}