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

        browser.navigateTo("https://business.google.com/en-all/google-ads/");

        String goal = String.format(
                "Goal: Set up Google Ads for '%s'. URL: '%s'. " +
                        "Follow these rules STRICTLY based on the CURRENT screenshot. " +
                        "CRITICAL: You must output EXACTLY ONE command. DO NOT use brackets [].\n\n" +

                        "--- PAGE 0: Google Ads Home ---\n" +
                        "0. If you see the Google Ads homepage with a 'Sign in' button, output EXACTLY: CLICK_TEXT: Sign in\n\n" +

                        "--- PAGE 1: About Your Business (Dynamic Flow) ---\n" +
                        "1. If the screen asks for 'Business name' AND the box is empty, output EXACTLY: FILL_FIELD: businessName, %s\n" +
                        "2. If the screen asks for 'Your website' or 'landing page URL' AND the URL box is empty, output EXACTLY: FILL_FIELD: websiteUrl, %s\n" +
                        "3. If the visible text boxes on the screen are correctly filled, output EXACTLY: CLICK_NEXT\n\n" +

                        "--- PAGE 2: Review Business Info ---\n" +
                        "4. If the business description text box is completely EMPTY, output EXACTLY: FILL_FIELD: businessDescription, High energy live music concerts and tours\n" +
                        "5. If you can see the words 'High energy' already typed inside the box, output EXACTLY: CLICK_NEXT\n\n" +


                        "--- PAGE 3: Link accounts ---\n" +
                        "6. If you see 'Link accounts' or 'Google Business Profile', output EXACTLY: CLICK_NEXT\n\n" +

                        "--- PAGE 4: Choose a goal ---\n" +
                        "7. If you see 'Choose a goal for this campaign' and 'Page views' is NOT selected, output EXACTLY: CLICK_TEXT: Page views\n" +
                        "8. If 'Page views' is selected AND you see an empty 'URL' box or 'enter a subpage path', output EXACTLY: FILL_FIELD: goalUrl, tickets\n" +
                        "9. If 'Page views' is selected AND the subpage box is filled, output EXACTLY: CLICK_NEXT\n\n" +

                        "--- PAGE 5: Search themes ---\n" +
                        "10. If you see 'Search themes' AND there is NO 'live music' chip entered yet, output EXACTLY: ADD_SEARCH_THEME: live music\n" +
                        "11. If you see the 'live music' chip/tag successfully added to the screen, output EXACTLY: CLICK_NEXT\n\n" +

                        "--- PAGE 6: Create an ad ---\n" +
                        "12. If you see 'Images (0)' or 'Logos (0)' or the red text 'Required' anywhere on the ad creation page, output EXACTLY: CLICK_SUGGESTED_ASSETS\n" +
                        "13. If you see 'Create an ad' AND the image/logo requirements are met (you do NOT see red 'Required' text), output EXACTLY: CLICK_NEXT\n\n" +

                        "--- PAGE 7: Set a bid strategy ---\n" +
                        "13. If you see 'Set a target cost per action' AND the checkbox next to it is UNCHECKED, output EXACTLY: CLICK_TEXT: Set a target cost per action\n" +
                        "14. If the 'Target CPA' text box appears and is completely empty, output EXACTLY: FILL_FIELD: targetCpa, 1.50\n" +
                        "15. If the CPA text box is already filled with a number, output EXACTLY: CLICK_NEXT\n\n" +

                        "--- PAGE 8: Set a budget ---\n" +
                        "16. If you see 'Set a budget' or 'Select an amount', output EXACTLY: CLICK_NEXT\n\n" +

                        "--- PAGE 9: Payment details (THE FINISH LINE) ---\n" +
                        "17. If you see 'Enter your account and payment details' or 'European Union political ads', output EXACTLY: DONE\n\n" +

                        "--- GLOBAL RULES & SAFETY VALVES ---\n" +
                        "18. If the 'Next' button has a loading spinner inside it, is pale grey (disabled), or the screen is clearly loading, output EXACTLY: WAIT\n" +
                        "19. If you see an unrecognizable screen, output EXACTLY: WAIT",
                artistName, websiteUrl, artistName, websiteUrl
        );

        navigatorAgent.executeVisualTask(goal);
    }
}