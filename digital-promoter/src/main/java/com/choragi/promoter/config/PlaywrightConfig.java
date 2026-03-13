package com.choragi.promoter.config;

import com.microsoft.playwright.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Configuration
public class PlaywrightConfig {

    @Bean
    @Lazy // THE FIX: Starts the server instantly, delays the heavy browser boot until needed
    public Page playwrightPage() {
        System.out.println("🚀 Initializing Playwright Configuration...");

        // 1. Detect if we are in Google Cloud Run
        boolean isCloudRun = System.getenv("K_SERVICE") != null;

        // 2. Use /tmp in the cloud to avoid Read-Only file system crashes
        String currentDir = isCloudRun ? System.getProperty("java.io.tmpdir") : System.getProperty("user.dir");
        Path userDataDir = Paths.get(currentDir, "chrome-profile");

        // 3. Base arguments for both local and cloud
        List<String> args = new ArrayList<>(Arrays.asList(
                "--use-fake-ui-for-media-stream",
                "--disable-blink-features=AutomationControlled"
        ));

        // THE FIX: Only apply the hardcore server flags if we are actually in the cloud!
        if (isCloudRun) {
            args.addAll(Arrays.asList(
                    "--no-sandbox",
                    "--disable-setuid-sandbox",
                    "--disable-dev-shm-usage", // Prevents memory crashes
                    "--disable-gpu",
                    "--single-process" // Forces Chrome to run inside the container limits
            ));
        }

        System.out.println("🌐 Launching Chromium. Headless Mode: " + isCloudRun);

        Playwright playwright = Playwright.create();

        // 4. Launch the browser
        BrowserContext context = playwright.chromium().launchPersistentContext(userDataDir,
                new BrowserType.LaunchPersistentContextOptions()
                        .setChannel("chrome") // Use real Chrome for Google Ads!
                        .setHeadless(isCloudRun)
                        .setViewportSize(1280, 720)
                        .setArgs(args)
        );

        if (context.pages().isEmpty()) {
            return context.newPage();
        }
        return context.pages().get(0);
    }
}