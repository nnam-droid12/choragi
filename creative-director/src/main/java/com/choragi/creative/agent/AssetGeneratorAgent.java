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

    public DigitalPressKit createPressKit(String artistName, String theme, String date, String location) {
        log.info("Asset Generator: Commissioning assets for {}...", artistName);

        String posterBase64 = creativeDirector.generateTourPoster(artistName, theme, date, location);
        String publicImageUrl = null;


        boolean imageSuccess = !posterBase64.equals("NO_IMAGE_GENERATED") && !posterBase64.equals("IMAGE_ERROR_FALLBACK");
        if (imageSuccess) {
            publicImageUrl = storageUploader.uploadBase64Image(posterBase64, artistName);
        }


        String publicVideoUrl = creativeDirector.generatePromoVideo(artistName, theme, date, location);
        boolean videoSuccess = !publicVideoUrl.equals("NO_VIDEO_GENERATED") && !publicVideoUrl.equals("VIDEO_ERROR_FALLBACK");


        return DigitalPressKit.builder()
                .artistName(artistName)
                .theme(theme)
                .date(date)
                .location(location)
                .posterBase64(posterBase64)
                .posterGcsUrl(publicImageUrl)
                .videoGcsUrl(videoSuccess ? publicVideoUrl : null)
                .status((imageSuccess && videoSuccess) ? "SUCCESS" : "PARTIAL_SUCCESS")
                .build();
    }
}