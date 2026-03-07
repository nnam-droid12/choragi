package com.choragi.promoter.config;

import com.microsoft.playwright.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;

@Configuration
public class PlaywrightConfig {

    @Bean
    public Page playwrightPage() {
        Playwright playwright = Playwright.create();

        Browser browser = playwright.chromium().launch(new BrowserType.LaunchOptions()
                .setHeadless(false)
                .setArgs(Arrays.asList("--use-fake-ui-for-media-stream")));

        BrowserContext context = browser.newContext(new Browser.NewContextOptions().setViewportSize(1280, 720));
        return context.newPage();
    }
}