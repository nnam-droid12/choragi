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
        String currentDir = System.getProperty("user.dir");
        Path userDataDir = Paths.get(currentDir, "chrome-profile");

        BrowserContext context = playwright.chromium().launchPersistentContext(userDataDir,
                new BrowserType.LaunchPersistentContextOptions()
                        .setHeadless(false)
                        .setViewportSize(1280, 720)
                        .setChannel("chrome")
                        .setArgs(java.util.Arrays.asList(
                                "--use-fake-ui-for-media-stream",
                                "--disable-blink-features=AutomationControlled"
                        ))
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