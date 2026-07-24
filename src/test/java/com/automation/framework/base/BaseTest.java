package com.automation.framework.base;

import org.openqa.selenium.WebDriver;
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
 *
 * <p>Screenshot-on-failure is deliberately not handled here. {@code ExtentTestListener}
 * (a TestNG {@code ITestListener}) needs the live driver at the moment a test fails
 * to both attach a screenshot to the HTML report and save it for CI artifact upload,
 * and TestNG guarantees listeners fire before {@code @AfterMethod} teardown runs —
 * so {@link #getCurrentDriver()} exposes the thread's active driver for that listener,
 * and this class stays focused on the create/quit lifecycle only.</p>
 */
public abstract class BaseTest {

    private static final ThreadLocal<WebDriver> DRIVER = new ThreadLocal<>();

    @BeforeMethod(alwaysRun = true)
    public void setUp() {
        DRIVER.set(DriverFactory.createDriver());
    }

    @AfterMethod(alwaysRun = true)
    public void tearDown() {
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

    /** Read-only access for reporting infrastructure (see {@code ExtentTestListener}); returns null outside a running UI test. */
    public static WebDriver getCurrentDriver() {
        return DRIVER.get();
    }
}
