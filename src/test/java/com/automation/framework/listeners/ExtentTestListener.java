package com.automation.framework.listeners;

import com.automation.framework.base.BaseTest;
import com.automation.framework.base.ScreenshotUtil;
import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.MediaEntityBuilder;
import com.aventstack.extentreports.Status;
import org.openqa.selenium.WebDriver;
import org.testng.ITestContext;
import org.testng.ITestListener;
import org.testng.ITestResult;

import java.util.Base64;

/**
 * TestNG {@link ITestListener} that drives the ExtentReports HTML report.
 *
 * <p>Registered once per suite XML (see the {@code <listeners>} block in
 * testng.xml / testng-ui.xml / testng-api.xml / testng-db.xml) rather than
 * via a {@code @Listeners} annotation on individual test classes — this way
 * every layer's suite gets reporting for free, and a new test class needs no
 * boilerplate to show up in the report.</p>
 *
 * <p>Uses a {@link ThreadLocal} for the current {@link ExtentTest} node for
 * the same reason {@code BaseTest} uses one for the WebDriver: TestNG can run
 * test methods across threads, and a plain instance field would let two
 * threads' log entries bleed into each other's report node.</p>
 */
public class ExtentTestListener implements ITestListener {

    private static final ThreadLocal<ExtentTest> CURRENT_TEST = new ThreadLocal<>();

    @Override
    public void onTestStart(ITestResult result) {
        String testName = result.getMethod().getMethodName();
        String description = result.getMethod().getDescription();
        ExtentTest test = ExtentManager.getInstance()
                .createTest(testName, description == null ? "" : description);
        CURRENT_TEST.set(test);
    }

    @Override
    public void onTestSuccess(ITestResult result) {
        // Attach a screenshot of the final passing state for UI tests, not
        // just a "Test passed" line — a report meant to be read by someone
        // who didn't watch the run should show what success actually looked
        // like (e.g. the inventory page after login), the same way a
        // failure gets a screenshot of what went wrong.
        attachScreenshotIfAvailable(Status.PASS, "Test passed");
    }

    @Override
    public void onTestFailure(ITestResult result) {
        // Only UI tests (subclasses of BaseTest) have a live browser to screenshot;
        // API/DB test failures fall through to a plain exception log, which is the
        // right evidence for that layer (request/response payload, SQL state, etc.
        // already appear in the exception message and stack trace).
        WebDriver driver = BaseTest.getCurrentDriver();
        byte[] screenshotBytes = driver != null ? ScreenshotUtil.captureAsBytes(driver) : null;

        ExtentTest test = CURRENT_TEST.get();
        if (screenshotBytes != null) {
            ScreenshotUtil.saveToFile(screenshotBytes, result.getMethod().getMethodName());
            String base64Screenshot = Base64.getEncoder().encodeToString(screenshotBytes);
            test.fail(result.getThrowable(),
                    MediaEntityBuilder.createScreenCaptureFromBase64String(base64Screenshot).build());
        } else {
            test.fail(result.getThrowable());
        }
    }

    @Override
    public void onTestSkipped(ITestResult result) {
        ExtentTest test = CURRENT_TEST.get();
        if (test == null) {
            // Can happen if a test is skipped entirely (e.g. a failed dependsOnMethods
            // upstream) before onTestStart ever fires for it — nothing to attach to.
            return;
        }
        String reason = result.getThrowable() != null
                ? result.getThrowable().getMessage()
                : "Skipped due to a failed/skipped upstream dependency";
        test.log(Status.SKIP, reason);
    }

    @Override
    public void onFinish(ITestContext context) {
        ExtentManager.getInstance().flush();
    }

    /**
     * Logs a checkpoint (e.g. "Logged in as standard_user", "Added Sauce
     * Labs Backpack to cart") to the currently-running test's report node,
     * with a screenshot attached if a browser is active.
     *
     * <p>Call this from test code at the moment a meaningful step completes
     * — the pass/fail summary alone doesn't show *what happened along the
     * way*, and a reader of the report (a recruiter, a teammate debugging a
     * failure after the fact) shouldn't have to re-run the suite just to see
     * what the login page or the cart looked like mid-test.</p>
     */
    public static void logStep(String message) {
        WebDriver driver = BaseTest.getCurrentDriver();
        byte[] screenshotBytes = driver != null ? ScreenshotUtil.captureAsBytes(driver) : null;
        ExtentTest test = CURRENT_TEST.get();
        if (test == null) {
            return;
        }
        if (screenshotBytes != null) {
            String base64Screenshot = Base64.getEncoder().encodeToString(screenshotBytes);
            test.log(Status.INFO, message,
                    MediaEntityBuilder.createScreenCaptureFromBase64String(base64Screenshot).build());
        } else {
            test.log(Status.INFO, message);
        }
    }

    private void attachScreenshotIfAvailable(Status status, String message) {
        WebDriver driver = BaseTest.getCurrentDriver();
        byte[] screenshotBytes = driver != null ? ScreenshotUtil.captureAsBytes(driver) : null;
        ExtentTest test = CURRENT_TEST.get();

        if (screenshotBytes != null) {
            String base64Screenshot = Base64.getEncoder().encodeToString(screenshotBytes);
            test.log(status, message,
                    MediaEntityBuilder.createScreenCaptureFromBase64String(base64Screenshot).build());
        } else {
            test.log(status, message);
        }
    }
}
