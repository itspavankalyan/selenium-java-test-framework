package com.portfolio.framework.base;

import org.openqa.selenium.WebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.ITestResult;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;

/**
 * Base class for every UI test class.
 *
 * <p>The driver is stored in a {@link ThreadLocal} rather than a plain instance
 * field. TestNG can run test classes in parallel threads (configured via
 * testng.xml's {@code parallel="classes"}); a plain field would let two
 * threads silently share — and stomp on — the same WebDriver instance. The
 * ThreadLocal guarantees each test thread gets an isolated browser session.</p>
 */
public abstract class BaseTest {

    private static final Logger log = LoggerFactory.getLogger(BaseTest.class);
    private static final ThreadLocal<WebDriver> DRIVER = new ThreadLocal<>();

    @BeforeMethod(alwaysRun = true)
    public void setUp() {
        DRIVER.set(DriverFactory.createDriver());
    }

    @AfterMethod(alwaysRun = true)
    public void tearDown(ITestResult result) {
        if (result.getStatus() == ITestResult.FAILURE) {
            // Capture evidence before the driver is torn down — a screenshot taken
            // after quit() is an empty/blank image and tells a debugger nothing.
            ScreenshotUtil.capture(getDriver(), result.getName());
        }
        WebDriver driver = DRIVER.get();
        if (driver != null) {
            driver.quit();
            DRIVER.remove();
        }
    }

    protected WebDriver getDriver() {
        WebDriver driver = DRIVER.get();
        if (driver == null) {
            throw new IllegalStateException(
                    "WebDriver has not been initialised for this thread. "
                            + "Was setUp() skipped, or is this method being called outside a test?");
        }
        return driver;
    }
}
