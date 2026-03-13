package com.choragi.promoter.agent;

import com.choragi.promoter.tools.BrowserClickerTool;
import com.google.genai.Client;
import com.google.genai.types.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Arrays;

@Service
@Slf4j
@RequiredArgsConstructor
public class VisualNavigatorAgent {

    private final Client client;
    private final BrowserClickerTool browser;

    public void executeVisualTask(String taskDescription) {
        log.info("Visual Agent starting task: {}", taskDescription);


        for (int step = 1; step <= 50; step++) {
            try {
                byte[] screenshotBytes = browser.takeScreenshot();

                String prompt = String.format(
                        "Goal: %s. \nLook at this screenshot. What is the very next action I should take? " +
                                "Do not explain your reasoning. Output ONLY ONE of these commands:\n" +
                                "CLICK: [X, Y]\n" +
                                "CLICK_AND_TYPE: X, Y, text to type\n" +
                                "PRESS: [KeyName] (e.g., Enter, PageUp)\n" +
                                "DONE: task complete", taskDescription);

                com.google.genai.types.GenerateContentResponse response = client.models.generateContent(
                        "gemini-2.5-pro",
                        com.google.genai.types.Content.builder().role("user").parts(java.util.Arrays.asList(
                                com.google.genai.types.Part.builder().text(prompt).build(),
                                com.google.genai.types.Part.builder().inlineData(
                                        com.google.genai.types.Blob.builder().data(screenshotBytes).mimeType("image/png").build()
                                ).build()
                        )).build(),
                        com.google.genai.types.GenerateContentConfig.builder().temperature(0.0f).build()
                );

                String aiAction = response.text().trim();
                log.info("Step {}: AI Decision -> {}", step, aiAction);

                if (aiAction.contains("DONE")) {
                    log.info("Task completed successfully by Visual Agent.");
                    log.info("Activating Hand Gesture mode...");
                    browser.enableGestureScrolling();
                    break;
                } else if (aiAction.contains("FILL_FIELD:")) {
                    String rawArgs = aiAction.substring(aiAction.indexOf("FILL_FIELD:") + 11).split("\n")[0];
                    String cleanArgs = rawArgs.replace("[", "").replace("]", "").trim();
                    String[] parts = cleanArgs.split(",", 2);

                    String fieldName = parts[0].trim();
                    String text = parts[1].trim();
                    browser.fillFormBySelector(fieldName, text);

                } else if (aiAction.contains("CLICK_SUGGESTED_ASSETS")) {
                    browser.clickSuggestedAssets();

                } else if (aiAction.contains("CLICK_NEXT")) {
                    browser.clickNext();

                } else if (aiAction.contains("ADD_SEARCH_THEME:")) {
                    String theme = aiAction.substring(aiAction.indexOf("ADD_SEARCH_THEME:") + 17).trim();
                    browser.addSearchTheme(theme);

                } else if (aiAction.contains("CLICK:")) {
                    String clickLine = aiAction.lines()
                            .filter(line -> line.contains("CLICK:"))
                            .findFirst()
                            .orElse("");


                    String cleanArgs = clickLine.substring(clickLine.indexOf("CLICK:") + 6)
                            .replace("[", "").replace("]", "").trim();

                    String[] coords = cleanArgs.split(",", 2);

                    try {
                        int x = Integer.parseInt(coords[0].trim());
                        int y = Integer.parseInt(coords[1].trim());
                        browser.click(x, y);
                    } catch (NumberFormatException e) {
                        log.warn("Failed to parse coordinates from AI string: {}. Falling back to PageDown.", aiAction);
                        browser.page.keyboard().press("PageDown");
                    }



                } else if (aiAction.contains("CLICK_TEXT:")) {
                    String targetText = aiAction.substring(aiAction.indexOf("CLICK_TEXT:") + 11).trim();
                    log.info("AI requested to click exact text: {}", targetText);
                    browser.clickText(targetText);

                } else if (aiAction.contains("WAIT")) {
                    log.info("AI requested a WAIT state. Pausing for 5 seconds...");
                    browser.page.waitForTimeout(5000);

                } else if (aiAction.contains("TYPE:")) {
                    String rawText = aiAction.substring(aiAction.indexOf("TYPE:") + 5).split("\n")[0];
                    String cleanText = rawText.replace("[", "").replace("]", "").trim();
                    browser.typeText(cleanText);

                } else if (aiAction.contains("PRESS:")) {
                    String rawKey = aiAction.substring(aiAction.indexOf("PRESS:") + 6).split("\n")[0];
                    String cleanKey = rawKey.replace("[", "").replace("]", "").replace(" ", "").trim();
                    browser.pressKey(cleanKey);
                }

            } catch (Exception e) {
                log.error("Visual loop failed", e);
                break;
            }
        }
    }
}