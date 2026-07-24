package com.portfolio.framework.base;

import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.time.LocalDateTime;

/**
 * Captures a screenshot on test failure and writes it under target/screenshots.
 *
 * <p>Living under target/ (not committed, see .gitignore) keeps failure evidence
 * out of version control while still making it available as a CI artifact —
 * the GitHub Actions workflow uploads this directory whenever a job fails, so
 * a red build comes with a picture of exactly what the browser saw.</p>
 */
final class ScreenshotUtil {

    private static final Logger log = LoggerFactory.getLogger(ScreenshotUtil.class);
    private static final DateTimeFormatter TIMESTAMP_PATTERN = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    private ScreenshotUtil() {
    }

    static void capture(WebDriver driver, String testName) {
        if (!(driver instanceof TakesScreenshot)) {
            log.warn("Driver does not support screenshots; skipping capture for '{}'", testName);
            return;
        }
        try {
            Path screenshotDir = Path.of("target", "screenshots");
            Files.createDirectories(screenshotDir);

            String fileName = "%s_%s.png".formatted(testName, LocalDateTime.now().format(TIMESTAMP_PATTERN));
            Path destination = screenshotDir.resolve(fileName);

            File source = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
            Files.copy(source.toPath(), destination);

            log.info("Failure screenshot saved: {}", destination.toAbsolutePath());
        } catch (IOException e) {
            // A screenshot failure should never mask the original test failure —
            // log and move on instead of throwing from a teardown hook.
            log.error("Could not capture failure screenshot for '{}'", testName, e);
        }
    }
}
