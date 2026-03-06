package com.choragi.creative.agent;

import com.choragi.creative.model.DigitalPressKit;
import com.choragi.creative.tools.CloudStorageUploader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class AssetGeneratorAgent {

    private final CreativeDirectorAgent creativeDirector;
    private final CloudStorageUploader storageUploader;

    public DigitalPressKit createPressKit(String artistName, String theme) {
        log.info("Asset Generator: Commissioning poster for {}...", artistName);


        String posterBase64 = creativeDirector.generateTourPoster(artistName, theme);
        String publicUrl = null;

        boolean success = !posterBase64.equals("NO_IMAGE_GENERATED") && !posterBase64.equals("IMAGE_ERROR_FALLBACK");

        if (success) {
            publicUrl = storageUploader.uploadBase64Image(posterBase64, artistName);
        }


        return DigitalPressKit.builder()
                .artistName(artistName)
                .theme(theme)
                .posterBase64(posterBase64)
                .posterGcsUrl(publicUrl)
                .status((success && publicUrl != null) ? "SUCCESS" : "FAILED")
                .build();
    }
}