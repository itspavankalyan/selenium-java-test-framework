package com.automation.framework.listeners;

import com.aventstack.extentreports.ExtentReports;
import com.aventstack.extentreports.reporter.ExtentSparkReporter;
import com.aventstack.extentreports.reporter.configuration.Theme;

/**
 * Owns the single {@link ExtentReports} instance for a test JVM.
 *
 * <p>Eagerly initialised in a static field rather than lazily on first use:
 * every Maven Surefire run forks a fresh JVM per suite execution, so "once
 * per JVM" and "once per test run" are the same thing here — there's no
 * multi-run state to guard against, just a single report writer that every
 * test thread's {@link ExtentTestListener} shares.</p>
 */
final class ExtentManager {

    private static final ExtentReports INSTANCE = createInstance();

    private ExtentManager() {
    }

    static ExtentReports getInstance() {
        return INSTANCE;
    }

    private static ExtentReports createInstance() {
        ExtentSparkReporter sparkReporter = new ExtentSparkReporter("target/extent-reports/ExtentReport.html");
        sparkReporter.config().setTheme(Theme.STANDARD);
        sparkReporter.config().setDocumentTitle("Test Automation Execution Report");
        sparkReporter.config().setReportName("Selenium / API / DB Test Suite");

        ExtentReports extentReports = new ExtentReports();
        extentReports.attachReporter(sparkReporter);
        extentReports.setSystemInfo("Java Version", System.getProperty("java.version"));
        extentReports.setSystemInfo("OS", System.getProperty("os.name"));
        return extentReports;
    }
}
