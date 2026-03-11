package com.choragi.promoter.config;

import com.microsoft.playwright.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Configuration
public class PlaywrightConfig {

    @Bean
    public Page playwrightPage() {
        Playwright playwright = Playwright.create();

        // 1. Detect if we are running in Google Cloud Run
        boolean isCloudRun = System.getenv("K_SERVICE") != null;

        // 2. Route the profile to /tmp in the cloud (read-only bypass), or current dir locally
        String currentDir = isCloudRun ? System.getProperty("java.io.tmpdir") : System.getProperty("user.dir");
        Path userDataDir = Paths.get(currentDir, "chrome-profile");

        // 3. Add the mandatory Docker/Linux sandbox bypass arguments
        List<String> args = new ArrayList<>(Arrays.asList(
                "--use-fake-ui-for-media-stream",
                "--disable-blink-features=AutomationControlled",
                "--no-sandbox",
                "--disable-setuid-sandbox",
                "--disable-dev-shm-usage",
                "--disable-gpu" // Extra stability for headless cloud containers
        ));

        BrowserContext context = playwright.chromium().launchPersistentContext(userDataDir,
                new BrowserType.LaunchPersistentContextOptions()
                        // 4. Force headless mode in the cloud, allow visible mode locally
                        .setHeadless(isCloudRun)
                        .setViewportSize(1280, 720)
                        // Note: Removed .setChannel("chrome") so it uses the safe bundled Chromium
                        .setArgs(args)
        );

        Page activePage;
        if (context.pages().isEmpty()) {
            activePage = context.newPage();
        } else {
            activePage = context.pages().get(0);
        }

        activePage.bringToFront();
        return activePage;
    }
}