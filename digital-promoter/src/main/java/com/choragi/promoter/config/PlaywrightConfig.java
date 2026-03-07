package com.choragi.promoter.config;

import com.microsoft.playwright.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

@Configuration
public class PlaywrightConfig {

    @Bean
    public Page playwrightPage() {
        Playwright playwright = Playwright.create();

        Path userDataDir = Paths.get("chrome-profile");

        BrowserContext context = playwright.chromium().launchPersistentContext(userDataDir,
                new BrowserType.LaunchPersistentContextOptions()
                        .setHeadless(false)
                        .setViewportSize(1280, 720)
                        .setArgs(Arrays.asList("--use-fake-ui-for-media-stream"))
        );

        return context.pages().get(0);
    }
}