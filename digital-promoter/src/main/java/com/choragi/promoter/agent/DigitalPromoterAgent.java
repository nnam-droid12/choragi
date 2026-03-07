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
                "Find the search bar on this page. Type '%s' into the search bar, and hit the search button or press Enter to navigate to their article.",
                artistName
        );

        navigatorAgent.executeVisualTask(goal);
    }
}