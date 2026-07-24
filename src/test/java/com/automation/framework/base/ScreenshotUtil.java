package com.automation.framework.base;

import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Screenshot capture used on UI test failure.
 *
 * <p>Captures once, as raw PNG bytes, and hands those bytes to two independent
 * consumers: {@link #saveToFile(byte[], String)} persists them under
 * target/screenshots (picked up as a CI artifact so a red build ships with a
 * picture of exactly what the browser saw), and {@code ExtentTestListener}
 * base64-encodes the same bytes to embed inline in the HTML report. Capturing
 * once and reusing the bytes avoids taking two separate screenshots — the
 * second capture would race with whatever the browser renders next.</p>
 */
public final class ScreenshotUtil {

    private static final Logger log = LoggerFactory.getLogger(ScreenshotUtil.class);
    private static final DateTimeFormatter TIMESTAMP_PATTERN = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    private ScreenshotUtil() {
    }

    /** Returns raw PNG bytes, or null if the driver doesn't support screenshots (e.g. a non-browser test). */
    public static byte[] captureAsBytes(WebDriver driver) {
        if (!(driver instanceof TakesScreenshot)) {
            log.warn("Driver does not implement TakesScreenshot; skipping screenshot capture");
            return null;
        }
        return ((TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES);
    }

    /** Persists PNG bytes under target/screenshots/{testName}_{timestamp}.png; returns the path, or null on write failure. */
    public static Path saveToFile(byte[] pngBytes, String testName) {
        try {
            Path screenshotDir = Path.of("target", "screenshots");
            Files.createDirectories(screenshotDir);

            String fileName = "%s_%s.png".formatted(testName, LocalDateTime.now().format(TIMESTAMP_PATTERN));
            Path destination = screenshotDir.resolve(fileName);
            Files.write(destination, pngBytes);

            log.info("Failure screenshot saved: {}", destination.toAbsolutePath());
            return destination;
        } catch (IOException e) {
            // A screenshot failure should never mask the original test failure —
            // log and move on instead of throwing from a reporting hook.
            log.error("Could not save failure screenshot for '{}'", testName, e);
            return null;
        }
    }
}
