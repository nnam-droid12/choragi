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
        log.info("Choragi Promoter: Initiating ad campaign for {}", artistName);

        browser.navigateTo("https://www.facebook.com/adsmanager/creation");


        String goal = String.format(
                "Create a new traffic ad campaign. Name the campaign '%s Tour'. " +
                        "Set the destination URL to '%s'. Click the publish button.",
                artistName, websiteUrl
        );

        navigatorAgent.executeVisualTask(goal);
    }
}