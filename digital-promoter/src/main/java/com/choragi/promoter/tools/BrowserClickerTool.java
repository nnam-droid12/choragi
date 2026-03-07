package com.choragi.promoter.tools;

import com.microsoft.playwright.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class BrowserClickerTool {

    private final Page page;

    public void navigateTo(String url) {
        log.info("Navigating to: {}", url);
        page.navigate(url);
        page.waitForLoadState();
    }

    public byte[] takeScreenshot() {
        return page.screenshot(new Page.ScreenshotOptions().setFullPage(false));
    }

    public void click(int x, int y) {
        log.info("AI clicked at coordinates: X={}, Y={}", x, y);
        page.mouse().click(x, y);
        page.waitForTimeout(2000);
    }

    public void typeText(String text) {
        log.info("AI typing: {}", text);
        page.keyboard().type(text);
        page.waitForTimeout(1000);
    }

    public void pressKey(String key) {
        log.info("AI pressing key: {}", key);
        page.keyboard().press(key);
        page.waitForTimeout(2000);
    }

    public void clickAndType(int x, int y, String text) {
        log.info("AI clicking at X={}, Y={} and typing: {}", x, y, text);
        page.mouse().click(x, y);
        page.waitForTimeout(500);
        page.keyboard().type(text);
        page.waitForTimeout(1000);
    }

    public void enableGestureScrolling() {
        log.info("Injecting MediaPipe Gesture Recognition for Jedi scrolling...");

        String gestureScript = """
            // 1. Create a hidden video element to capture the webcam stream
            const video = document.createElement('video');
            video.style.display = 'none';
            document.body.appendChild(video);

            // 2. Helper function to load MediaPipe from CDNs
            const loadScript = (src) => new Promise((resolve) => {
                const s = document.createElement('script');
                s.src = src;
                s.onload = resolve;
                document.head.appendChild(s);
            });

            (async function initGestures() {
                // Load the vision models
                await loadScript('https://cdn.jsdelivr.net/npm/@mediapipe/camera_utils/camera_utils.js');
                await loadScript('https://cdn.jsdelivr.net/npm/@mediapipe/hands/hands.js');

                let previousY = null;

                const hands = new Hands({locateFile: (file) => `https://cdn.jsdelivr.net/npm/@mediapipe/hands/${file}`});
                hands.setOptions({
                    maxNumHands: 1,
                    minDetectionConfidence: 0.7,
                    minTrackingConfidence: 0.7
                });

                // 3. The Brain: Translate hand movement to scrolling
                hands.onResults((results) => {
                    if (results.multiHandLandmarks && results.multiHandLandmarks.length > 0) {
                        // Track the Y-coordinate of the tip of the Index Finger (Landmark 8)
                        const currentY = results.multiHandLandmarks[0][8].y;
                        
                        if (previousY !== null) {
                            const deltaY = currentY - previousY;
                            
                            // Threshold to prevent tiny jitters from shaking the screen
                            if (Math.abs(deltaY) > 0.015) {
                                // Multiply the delta to determine scroll speed. 
                                // Hand moves up -> scroll up. Hand moves down -> scroll down.
                                window.scrollBy({ top: deltaY * 4000, behavior: 'auto' }); 
                            }
                        }
                        previousY = currentY;
                    } else {
                        previousY = null; // Reset if hand leaves the camera frame
                    }
                });

                // 4. Start the camera feed
                const camera = new Camera(video, {
                    onFrame: async () => { await hands.send({image: video}); },
                    width: 640, height: 480
                });
                camera.start();
            })();
        """;

        page.evaluate(gestureScript);
    }
}