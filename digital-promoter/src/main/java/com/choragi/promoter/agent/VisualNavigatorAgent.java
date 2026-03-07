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

        for (int step = 1; step <= 5; step++) {
            try {
                byte[] screenshotBytes = browser.takeScreenshot();

                String prompt = String.format(
                        "Goal: %s. \nLook at this screenshot. What is the very next action I should take? " +
                                "Respond ONLY in this exact format: \n" +
                                "CLICK: [X, Y] \n" +
                                "TYPE: [text to type] \n" +
                                "DONE: task complete", taskDescription);

                GenerateContentResponse response = client.models.generateContent(
                        "gemini-2.5-computer-use-preview-10-2025",
                        Content.builder().role("user").parts(Arrays.asList(
                                Part.builder().text(prompt).build(),
                                Part.builder().inlineData(Blob.builder().data(screenshotBytes).mimeType("image/png").build()).build()
                        )).build(),
                        GenerateContentConfig.builder().temperature(0.0f).build()
                );

                String aiAction = response.text().trim();
                log.info("Step {}: AI Decision -> {}", step, aiAction);

                // 3. Act (Parse output and execute)
                if (aiAction.startsWith("DONE")) {
                    log.info("Task completed successfully by Visual Agent.");
                    break;
                } else if (aiAction.startsWith("CLICK:")) {
                    // Bulletproof coordinate parsing: strip brackets, split by comma
                    String coords = aiAction.substring(aiAction.indexOf(":") + 1)
                            .replace("[", "")
                            .replace("]", "")
                            .trim();
                    String[] parts = coords.split(",");
                    browser.click(Integer.parseInt(parts[0].trim()), Integer.parseInt(parts[1].trim()));

                } else if (aiAction.startsWith("TYPE:")) {
                    // Bulletproof text parsing: grab everything after the colon and strip brackets
                    String text = aiAction.substring(aiAction.indexOf(":") + 1)
                            .replace("[", "")
                            .replace("]", "")
                            .trim();
                    browser.typeText(text);
                }

            } catch (Exception e) {
                log.error("Visual loop failed", e);
                break;
            }
        }
    }
}