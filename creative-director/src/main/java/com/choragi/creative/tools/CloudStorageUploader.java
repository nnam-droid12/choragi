package com.choragi.creative.tools;

import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Base64;
import java.util.UUID;

@Component
@Slf4j
public class CloudStorageUploader {

    @Value("${choragi.storage.bucket-name:choragi-assets-bucket}")
    private String bucketName;

    public String uploadBase64Image(String base64Image, String artistName) {
        try {
            String cleanBase64 = base64Image.contains(",") ? base64Image.split(",")[1] : base64Image;
            byte[] imageBytes = Base64.getDecoder().decode(cleanBase64);

            String safeArtistName = artistName.replaceAll("[^a-zA-Z0-9-_\\.]", "_").toLowerCase();
            String fileName = "posters/" + safeArtistName + "-" + UUID.randomUUID().toString() + ".png";

            Storage storage = StorageOptions.getDefaultInstance().getService();
            BlobId blobId = BlobId.of(bucketName, fileName);
            BlobInfo blobInfo = BlobInfo.newBuilder(blobId).setContentType("image/png").build();

            log.info("Uploading poster to Google Cloud Storage: gs://{}/{}", bucketName, fileName);
            storage.create(blobInfo, imageBytes);

            String publicUrl = String.format("https://storage.googleapis.com/%s/%s", bucketName, fileName);
            log.info("Upload complete! Public URL: {}", publicUrl);

            return publicUrl;
        } catch (Exception e) {
            log.error("Failed to upload image to Google Cloud Storage", e);
            return null;
        }
    }
}