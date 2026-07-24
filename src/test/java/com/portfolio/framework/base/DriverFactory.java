package com.portfolio.framework.base;

import com.portfolio.framework.config.ConfigReader;
import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;

/**
 * Owns WebDriver *creation* only. Lifecycle (quit / thread cleanup) is the
 * caller's responsibility via {@link BaseTest}.
 *
 * <p>Kept as a separate class from {@link BaseTest} on purpose: driver
 * construction is a pure factory concern, whereas BaseTest also wires in
 * TestNG hooks and reporting. Splitting them keeps each class testable and
 * makes it trivial to add a new browser (or a remote Selenium Grid endpoint)
 * without touching test lifecycle code.</p>
 */
public final class DriverFactory {

    private static final Logger log = LoggerFactory.getLogger(DriverFactory.class);

    private DriverFactory() {
    }

    public static WebDriver createDriver() {
        String browser = ConfigReader.get("browser").toLowerCase();
        boolean headless = ConfigReader.getBoolean("headless");

        log.info("Creating '{}' WebDriver instance (headless={})", browser, headless);

        WebDriver driver = switch (browser) {
            case "firefox" -> createFirefoxDriver(headless);
            case "chrome" -> createChromeDriver(headless);
            default -> throw new IllegalArgumentException(
                    "Unsupported browser: '" + browser + "'. Supported values: chrome, firefox.");
        };

        driver.manage().timeouts().implicitlyWait(
                Duration.ofSeconds(ConfigReader.getInt("implicit.wait.seconds")));
        driver.manage().window().maximize();
        return driver;
    }

    private static WebDriver createChromeDriver(boolean headless) {
        WebDriverManager.chromedriver().setup();
        ChromeOptions options = new ChromeOptions();
        if (headless) {
            // "--headless=new" uses Chrome's modern headless mode, which renders much
            // closer to a real headed browser than the legacy --headless flag — matters
            // because this same suite runs both on a dev laptop and in GitHub Actions CI.
            options.addArguments("--headless=new");
        }
        options.addArguments("--window-size=1920,1080");
        options.addArguments("--disable-gpu");
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        return new ChromeDriver(options);
    }

    private static WebDriver createFirefoxDriver(boolean headless) {
        WebDriverManager.firefoxdriver().setup();
        FirefoxOptions options = new FirefoxOptions();
        if (headless) {
            options.addArguments("-headless");
        }
        return new FirefoxDriver(options);
    }
}
