package com.choragi.sitebuilder.tools;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.file.Files;

@Component
@Slf4j
public class FirebaseDeployerTool {

    public String deployToFirebase(String htmlContent, String artistName) {
        try {
            log.info("DevOps Tool: Preparing deployment package for Firebase...");


            File baseDir = new File("deploy-stage");
            File publicDir = new File(baseDir, "public");
            publicDir.mkdirs();


            File indexFile = new File(publicDir, "index.html");
            Files.writeString(indexFile.toPath(), htmlContent);


            String firebaseJson = "{\n  \"hosting\": {\n    \"public\": \"public\",\n    \"ignore\": [\"firebase.json\", \"**/.*\", \"**/node_modules/**\"]\n  }\n}";
            Files.writeString(new File(baseDir, "firebase.json").toPath(), firebaseJson);


            String firebaserc = "{\n  \"projects\": {\n    \"default\": \"nixora-web\"\n  }\n}";
            Files.writeString(new File(baseDir, ".firebaserc").toPath(), firebaserc);

            log.info("Files written locally to /deploy-stage/public/index.html");


            log.info("Executing: firebase deploy --only hosting");

            ProcessBuilder processBuilder;
            boolean isWindows = System.getProperty("os.name").toLowerCase().startsWith("windows");

            if (isWindows) {

                processBuilder = new ProcessBuilder("cmd.exe", "/c", "firebase deploy --only hosting");
            } else {

                processBuilder = new ProcessBuilder("firebase", "deploy", "--only", "hosting");
            }

            processBuilder.directory(baseDir);
            processBuilder.redirectErrorStream(true);

            Process process = processBuilder.start();


            java.io.BufferedReader reader = new java.io.BufferedReader(
                    new java.io.InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                log.info("[Firebase CLI] " + line);
            }

            int exitCode = process.waitFor();

            if (exitCode == 0) {
                log.info("Firebase Deployment Successful!");
                return "https://nixora-web.web.app";
            } else {
                log.error("Firebase CLI failed with exit code: " + exitCode);
                return "DEPLOYMENT_FAILED";
            }

        } catch (Exception e) {
            log.error("Failed to deploy to Firebase", e);
            return "ERROR";
        }
    }
}