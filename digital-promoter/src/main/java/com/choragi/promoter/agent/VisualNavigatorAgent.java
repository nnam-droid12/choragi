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


        for (int step = 1; step <= 25; step++) {
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
                        "gemini-2.5-computer-use-preview-10-2025",
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
                } else if (aiAction.contains("CLICK_AND_TYPE:")) {
                    String rawArgs = aiAction.substring(aiAction.indexOf("CLICK_AND_TYPE:") + 15).split("\n")[0];
                    String cleanArgs = rawArgs.replace("[", "").replace("]", "").trim();
                    String[] parts = cleanArgs.split(",", 3); // Split into exactly 3 pieces (X, Y, Text)

                    int x = Integer.parseInt(parts[0].trim());
                    int y = Integer.parseInt(parts[1].trim());
                    String text = parts[2].trim();
                    browser.clickAndType(x, y, text);

                } else if (aiAction.contains("CLICK:")) {
                    String rawCoords = aiAction.substring(aiAction.indexOf("CLICK:") + 6).split("\n")[0];
                    String cleanCoords = rawCoords.replaceAll("[^0-9,]", "").trim();
                    String[] parts = cleanCoords.split(",");
                    browser.click(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]));

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