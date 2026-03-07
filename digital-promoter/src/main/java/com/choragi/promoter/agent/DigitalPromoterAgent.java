package com.choragi.promoter.agent;

import com.choragi.promoter.tools.BrowserClickerTool;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class DigitalPromoterAgent {

    private final VisualNavigatorAgent navigatorAgent;
    private final BrowserClickerTool browser;

    public void launchFacebookAd(String artistName, String websiteUrl) {
        log.info("Choragi Promoter: Initiating test UI run for {}", artistName);

        browser.navigateTo("https://en.wikipedia.org/wiki/Main_Page");

        String goal = String.format(
                "Navigate to the Wikipedia article for '%s'. " +
                        "Follow this logic strictly based on what you see in the screenshot: " +
                        "1. If the search bar is empty, use CLICK_AND_TYPE to enter '%s'. " +
                        "2. If '%s' is ALREADY visible inside the search bar, DO NOT type it again. Instead, use PRESS: Enter. " +
                        "3. If you are already looking at the full Wikipedia article page for '%s', output DONE.",
                artistName, artistName, artistName, artistName
        );

        navigatorAgent.executeVisualTask(goal);
    }
}