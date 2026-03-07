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
}