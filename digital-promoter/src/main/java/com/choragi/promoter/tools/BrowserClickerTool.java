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
        log.info("Injecting MediaPipe Gesture Recognition with Visual Tracking Skeleton...");

        String gestureScript = """
            // 1. Create a container to hold BOTH the video and the drawing canvas
            const container = document.createElement('div');
            container.style.position = 'fixed';
            container.style.bottom = '20px';
            container.style.right = '20px';
            container.style.width = '320px';
            container.style.height = '240px';
            container.style.zIndex = '9999';
            container.style.borderRadius = '12px';
            container.style.overflow = 'hidden';
            container.style.boxShadow = '0 10px 20px rgba(0,0,0,0.5)';
            container.style.border = '4px solid #00ff00';
            container.style.transform = 'scaleX(-1)'; // Mirror the whole box
            document.body.appendChild(container);

            // 2. Create the Video Element (Base layer)
            const video = document.createElement('video');
            video.autoplay = true;
            video.playsInline = true;
            video.style.position = 'absolute';
            video.style.top = '0';
            video.style.left = '0';
            video.style.width = '100%';
            video.style.height = '100%';
            video.style.objectFit = 'cover';
            container.appendChild(video);

            // 3. Create the Canvas Element (Top layer for the green skeleton)
            const canvas = document.createElement('canvas');
            canvas.style.position = 'absolute';
            canvas.style.top = '0';
            canvas.style.left = '0';
            canvas.width = 320;
            canvas.height = 240;
            const canvasCtx = canvas.getContext('2d');
            container.appendChild(canvas);

            // 4. Helper function to load MediaPipe
            const loadScript = (src) => new Promise((resolve, reject) => {
                const s = document.createElement('script');
                s.src = src;
                s.onload = resolve;
                s.onerror = reject;
                document.head.appendChild(s);
            });

            (async function initGestures() {
                try {
                    await loadScript('https://cdn.jsdelivr.net/npm/@mediapipe/camera_utils/camera_utils.js');
                    await loadScript('https://cdn.jsdelivr.net/npm/@mediapipe/hands/hands.js');
                    // THE NEW ADDITION: The Drawing utilities!
                    await loadScript('https://cdn.jsdelivr.net/npm/@mediapipe/drawing_utils/drawing_utils.js'); 

                    let previousY = null;

                    const hands = new Hands({locateFile: (file) => `https://cdn.jsdelivr.net/npm/@mediapipe/hands/${file}`});
                    hands.setOptions({
                        maxNumHands: 1,
                        modelComplexity: 1,
                        minDetectionConfidence: 0.65,
                        minTrackingConfidence: 0.65
                    });

                    // 5. The Brain & The Painter
                    hands.onResults((results) => {
                        // Clear the canvas every frame so old lines don't pile up
                        canvasCtx.clearRect(0, 0, canvas.width, canvas.height);

                        if (results.multiHandLandmarks && results.multiHandLandmarks.length > 0) {
                            container.style.borderColor = '#ff0000'; // Box turns Red
                            
                            // DRAW THE SKELETON
                            for (const landmarks of results.multiHandLandmarks) {
                                // Draw the bright green connection lines
                                drawConnectors(canvasCtx, landmarks, HAND_CONNECTIONS, {color: '#00FF00', lineWidth: 4});
                                // Draw the red dots on the knuckles/joints
                                drawLandmarks(canvasCtx, landmarks, {color: '#FF0000', lineWidth: 2, radius: 3});
                            }

                            // SCROLLING LOGIC
                            const currentY = results.multiHandLandmarks[0][8].y; // Index finger tip
                            
                            if (previousY !== null) {
                                const deltaY = currentY - previousY;
                                if (Math.abs(deltaY) > 0.01) {
                                    window.scrollBy({ top: deltaY * 3500, behavior: 'instant' }); 
                                }
                            }
                            previousY = currentY;
                        } else {
                            container.style.borderColor = '#00ff00'; // Box turns Green
                            previousY = null;
                        }
                    });

                    const camera = new Camera(video, {
                        onFrame: async () => { await hands.send({image: video}); },
                        width: 320, height: 240
                    });
                    camera.start();
                } catch (error) {
                    console.error("MediaPipe failed to load", error);
                }
            })();
        """;

        page.evaluate(gestureScript);
    }
}