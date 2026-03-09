package com.choragi.promoter.tools;

import com.microsoft.playwright.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class BrowserClickerTool {

    public final Page page;

    public void navigateTo(String url) {
        log.info("Navigating to: {}", url);
        try {

            page.navigate(url, new com.microsoft.playwright.Page.NavigateOptions()
                    .setWaitUntil(com.microsoft.playwright.options.WaitUntilState.DOMCONTENTLOADED)
                    .setTimeout(60000));
            log.info("Navigation command executed successfully.");
        } catch (Exception e) {
            log.error("Failed to navigate to " + url, e);
        }
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
        page.waitForTimeout(400);
        page.mouse().click(x, y);
        page.waitForTimeout(600);
        page.keyboard().type(text);
        page.waitForTimeout(1000);
    }

    public void smartType(int x, int y, String text) {
        log.info("AI using targeted SMART_TYPE at X={}, Y={} to clear old text and inject: {}", x, y, text);

        page.mouse().click(x, y);
        page.waitForTimeout(500);


        page.mouse().click(x, y, new com.microsoft.playwright.Mouse.ClickOptions().setClickCount(3));
        page.waitForTimeout(300);


        page.keyboard().press("Backspace");
        page.waitForTimeout(300);

        page.keyboard().type(text);
        page.waitForTimeout(1000);
    }

    public void fillFormBySelector(String fieldName, String text) {
        log.info("AI using JS Index Locator to safely fill '{}' with: {}", fieldName, text);
        try {
            boolean focused = (boolean) page.evaluate("(args) => { " +
                    "  const inputs = Array.from(document.querySelectorAll('input:not([type=\"hidden\"]):not([type=\"radio\"]):not([type=\"checkbox\"]), textarea'))" +
                    "    .filter(el => el.offsetParent !== null); " +
                    "  let target = null; " +

                    "  if (args.fieldName === 'businessName' && inputs.length > 0) target = inputs[0]; " +

                    "  if (args.fieldName === 'websiteUrl') { " +
                    "    target = Array.from(document.querySelectorAll('input')).find(el => " +
                    "      (el.placeholder && el.placeholder.toLowerCase().includes('url')) || " +
                    "      (el.ariaLabel && el.ariaLabel.toLowerCase().includes('url')) || " +
                    "      el.type === 'url'" +
                    "    ); " +

                    "    if (!target && inputs.length > 1) { target = inputs[1]; } " +
                    "    else if (!target && inputs.length === 1) { target = inputs[0]; } " +
                    "  } " +


                    "  if (args.fieldName === 'businessDescription') { " +
                    "    target = document.querySelector('textarea'); " +
                    "    if (!target && inputs.length > 0) target = inputs[0]; " +
                    "  } " +

                    "  if (args.fieldName === 'goalUrl') { " +
                    "    target = Array.from(document.querySelectorAll('input')).find(el => el.placeholder.includes('subpage') || el.ariaLabel?.toLowerCase().includes('url')); " +
                    "    if (!target && inputs.length > 0) target = inputs[inputs.length - 1]; " +
                    "  } " +

                    "  if (args.fieldName === 'targetCpa') { " +
                    "    target = Array.from(document.querySelectorAll('input')).find(el => el.type === 'text' || el.type === 'number'); " +
                    "    if (!target && inputs.length > 0) target = inputs[inputs.length - 1]; " +
                    "  } " +

                    "  if (target) { " +
                    "    target.value = ''; " +
                    "    target.dispatchEvent(new Event('input', { bubbles: true })); " +
                    "    target.focus(); " +
                    "    return true; " +
                    "  } return false; " +
                    "}", java.util.Map.of("fieldName", fieldName));

            if (focused) {
                log.info("Input box focused. Typing '{}' like a human...", text);
                page.waitForTimeout(500);
                page.keyboard().type(text, new com.microsoft.playwright.Keyboard.TypeOptions().setDelay(100));
                page.waitForTimeout(500);
            } else {
                log.warn("Could not find input box for {}. Scrolling down...", fieldName);
                page.keyboard().press("PageDown");
            }
        } catch (Exception e) {
            log.error("JS Locator failed", e);
        }
    }


    public void addSearchTheme(String text) {
        log.info("AI using Human Typist Macro for Search Theme: {}", text);
        try {
            page.evaluate("() => { " +
                    "  const input = Array.from(document.querySelectorAll('input')).find(el => " +
                    "    (el.placeholder && el.placeholder.toLowerCase().includes('search')) || " +
                    "    (el.ariaLabel && el.ariaLabel.toLowerCase().includes('search'))" +
                    "  ); " +
                    "  if (input) { " +
                    "    input.scrollIntoView({block: 'center'}); " +
                    "    input.setAttribute('data-ai-theme', 'true'); " +
                    "  } " +
                    "}");

            com.microsoft.playwright.Locator box = page.locator("input[data-ai-theme='true']");

            box.clear();
            page.waitForTimeout(500);

            log.info("Typing slowly to trigger React state...");
            box.pressSequentially(text, new com.microsoft.playwright.Locator.PressSequentiallyOptions().setDelay(100));

            log.info("Waiting for Google's dropdown...");
            page.waitForTimeout(2000);

            log.info("Pressing ArrowDown and Enter to select the chip...");
            box.press("ArrowDown");
            page.waitForTimeout(500);
            box.press("Enter");
            page.waitForTimeout(1000);

            log.info("Pressing Tab to lose focus safely...");
            box.press("Tab");
            page.waitForTimeout(1500);

        } catch (Exception e) {
            log.warn("Could not execute Search Theme macro. Scrolling down...");
            page.keyboard().press("PageDown");
        }
    }

    public void clickNext() {
        log.info("AI hunting for the TRUE visible Next button in the sticky footer...");
        try {
            page.keyboard().press("Escape");
            page.waitForTimeout(500);

            boolean clicked = (boolean) page.evaluate("() => { " +
                    "  const elements = Array.from(document.querySelectorAll('button, div[role=\"button\"], a, span')); " +
                    "  const validElements = elements.filter(el => " +
                    "    el.innerText && el.innerText.trim().toLowerCase() === 'next' && " +
                    "    el.getBoundingClientRect().height > 0 && el.getBoundingClientRect().width > 0 " + // MUST be physically visible
                    "  ); " +
                    "  if (validElements.length > 0) { " +
                    "    const realNextBtn = validElements[validElements.length - 1]; " + // The footer is always last in the DOM!
                    "    realNextBtn.click(); " +
                    "    return true; " +
                    "  } " +
                    "  return false; " +
                    "}");

            if (clicked) {
                log.info("Successfully clicked the TRUE Next button via JS Split-Pane Override!");
            } else {

                page.getByRole(com.microsoft.playwright.options.AriaRole.BUTTON,
                        new com.microsoft.playwright.Page.GetByRoleOptions().setName("Next")).last().click();
                log.info("Clicked Next using Native Fallback!");
            }

            log.info("Waiting 8 seconds for Google Ads to finish saving...");
            page.waitForTimeout(8000);

        } catch (Exception e) {
            log.warn("Next button disabled or Google is loading. Waiting 4 seconds...");
            page.waitForTimeout(4000);
        }
    }

    public void clickText(String text) {
        log.info("AI locating and clicking visible text: '{}'", text);
        try {

            com.microsoft.playwright.Locator target = page.locator("text=\"" + text + "\" >> visible=true").first();

            if (target.count() > 0) {
                target.click(new com.microsoft.playwright.Locator.ClickOptions().setTimeout(5000));
                log.info("Successfully clicked the visible text natively!");
                page.waitForTimeout(2000);
                return;
            }

            log.info("Native locator failed to pierce the nav bar. Firing Javascript override...");


            boolean clickedWithJs = (boolean) page.evaluate("(textToFind) => { " +
                    "  const elements = Array.from(document.querySelectorAll('a, button, [role=\"button\"], span')); " +
                    "  const target = elements.find(el => " +
                    "    el.innerText && el.innerText.toLowerCase().includes(textToFind.toLowerCase()) && " +
                    "    el.getBoundingClientRect().height > 0 && el.getBoundingClientRect().width > 0 " + // Must be physically visible on screen
                    "  ); " +
                    "  if (target) { " +
                    "    target.click(); " +
                    "    return true; " +
                    "  } " +
                    "  return false; " +
                    "}", text);

            if (clickedWithJs) {
                log.info("Successfully forced click using Javascript Override!");
                page.waitForTimeout(2000);
            } else {
                throw new Exception("Element absolutely not found on screen.");
            }

        } catch (Exception e) {
            log.warn("Could not find text '{}' to click. Scrolling down...", text);
            page.keyboard().press("PageDown");
        }
    }

    public void clickSuggestedAssets() {
        log.info("AI activating Screenshot-Targeted Asset Picker...");
        try {

            page.keyboard().press("Escape");
            page.keyboard().press("PageDown");
            page.waitForTimeout(1000);


            log.info("Hunting for the 'Select all' button for images...");
            page.evaluate("() => { " +
                    "  const btns = Array.from(document.querySelectorAll('button, div[role=\"button\"], span')); " +
                    "  const selectAll = btns.find(b => b.innerText && b.innerText.trim() === 'Select all'); " +
                    "  if (selectAll) selectAll.click(); " +
                    "}");
            page.waitForTimeout(2000);

            log.info("Hunting for the '+ Logos' button...");
            page.evaluate("() => { " +
                    "  const btns = Array.from(document.querySelectorAll('button, div[role=\"button\"], span, a')); " +
                    "  const addLogo = btns.find(b => b.innerText && b.innerText.trim() === '+ Logos'); " +
                    "  if (addLogo) addLogo.click(); " +
                    "}");
            page.waitForTimeout(4000);

            log.info("Switching to Stock Images tab in the modal...");
            page.evaluate("() => { " +
                    "  const tabs = Array.from(document.querySelectorAll('[role=\"tab\"], button')); " +
                    "  const stockTab = tabs.find(t => t.innerText && t.innerText.toLowerCase().includes('stock')); " +
                    "  if (stockTab) stockTab.click(); " +
                    "}");
            page.waitForTimeout(4000);


            log.info("Selecting a stock logo...");
            page.evaluate("() => { " +
                    "  const modalImages = Array.from(document.querySelectorAll('dialog img, [role=\"dialog\"] img, [aria-modal=\"true\"] img'))" +
                    "    .filter(img => img.getBoundingClientRect().width > 20); " +
                    "  if (modalImages.length > 0) modalImages[0].click(); " +
                    "}");
            page.waitForTimeout(2000);

            log.info("Saving the logo...");
            page.evaluate("() => { " +
                    "  const btns = Array.from(document.querySelectorAll('dialog button, [role=\"dialog\"] button, [aria-modal=\"true\"] button')); " +
                    "  const saveBtn = btns.find(b => b.innerText && b.innerText.trim() === 'Save'); " +
                    "  if (saveBtn) saveBtn.click(); " +
                    "}");
            page.waitForTimeout(3000);

            page.keyboard().press("Escape");

        } catch (Exception e) {
            log.error("Targeted Asset Macro Failed. Scrolling down.", e);
            page.keyboard().press("PageDown");
        }
    }

    public void enableGestureScrolling() {
        log.info("Injecting MediaPipe Gesture Recognition with Visual Tracking Skeleton...");

        String gestureScript = """
            // 1. Create a container to hold BOTH the video and the drawing canvas
            const container = document.createElement('div');
            container.style.position = 'fixed';
            container.style.bottom = '20px';
            container.style.right = '20px';
            // THE FIX: Increased size from 320x240 to 480x360
            container.style.width = '480px';
            container.style.height = '360px';
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
            // THE FIX: Match the new container size
            canvas.width = 480;
            canvas.height = 360;
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
                        canvasCtx.clearRect(0, 0, canvas.width, canvas.height);

                        if (results.multiHandLandmarks && results.multiHandLandmarks.length > 0) {
                            container.style.borderColor = '#ff0000'; // Box turns Red
                            
                            for (const landmarks of results.multiHandLandmarks) {
                                drawConnectors(canvasCtx, landmarks, HAND_CONNECTIONS, {color: '#00FF00', lineWidth: 4});
                                drawLandmarks(canvasCtx, landmarks, {color: '#FF0000', lineWidth: 2, radius: 3});
                            }

                            const currentY = results.multiHandLandmarks[0][8].y; 
                            
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
                        // THE FIX: Ask the webcam for a higher resolution feed
                        width: 480, height: 360
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