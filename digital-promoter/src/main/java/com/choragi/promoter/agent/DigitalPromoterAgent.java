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

    public void launchAdsCampaign(String artistName, String websiteUrl) {
        log.info("Choragi Promoter: Initiating Google Ads creation for {}", artistName);

        browser.navigateTo("https://ads.google.com/aw/signup/aboutyourbusiness");

        String goal = String.format(
                "You are setting up a Google Ads campaign for '%s'. Follow this logic strictly based on what is visible on screen:\n" +
                        "1. If you see 'What's your business name?', use CLICK_AND_TYPE to enter '%s', then CLICK the 'Next' button.\n" +
                        "2. If you see 'Where should people go after clicking your ad?' or 'Enter a web page URL', use CLICK_AND_TYPE to enter '%s', then CLICK 'Next'.\n" +
                        "3. If you see 'Describe what makes your business unique', use CLICK_AND_TYPE to enter 'High energy live music concert', then CLICK 'Next'.\n" +
                        "4. If you see 'What specific products or services', CLICK 'Concerts & Music Festivals', then CLICK 'Next'.\n" +
                        "5. If you see 'Choose a goal for this campaign', CLICK 'Page views' or 'Brand awareness', then CLICK 'Next'.\n" +
                        "6. If you see 'Search terms', 'Budget', or 'Create ads', CLICK the 'Next' or 'Continue' button to skip through.\n" +
                        "7. CRITICAL: If you reach a page asking for 'Payment details', 'Billing', or 'Submit', DO NOT click anything. Output DONE.",
                artistName, artistName, websiteUrl
        );

        navigatorAgent.executeVisualTask(goal);
    }
}