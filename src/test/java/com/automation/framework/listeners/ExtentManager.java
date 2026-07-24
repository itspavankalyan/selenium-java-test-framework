package com.automation.framework.listeners;

import com.aventstack.extentreports.ExtentReports;
import com.aventstack.extentreports.reporter.ExtentSparkReporter;
import com.aventstack.extentreports.reporter.configuration.Theme;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

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

    private static final DateTimeFormatter FILE_TIMESTAMP = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");
    private static final ExtentReports INSTANCE = createInstance();

    private ExtentManager() {
    }

    static ExtentReports getInstance() {
        return INSTANCE;
    }

    private static ExtentReports createInstance() {
        // Timestamped per run rather than a fixed "ExtentReport.html": a fixed
        // name gets silently overwritten by the next run, which destroys the
        // one thing a report is for — comparing today's run against
        // yesterday's, or attaching a specific run's evidence. Every suite
        // execution now leaves its own dated file in target/extent-reports/.
        String fileName = "ExtentReport_%s.html".formatted(LocalDateTime.now().format(FILE_TIMESTAMP));
        ExtentSparkReporter sparkReporter = new ExtentSparkReporter("target/extent-reports/" + fileName);
        sparkReporter.config().setTheme(Theme.STANDARD);
        sparkReporter.config().setDocumentTitle("Test Automation Execution Report");
        sparkReporter.config().setReportName("Selenium / API / DB Test Suite");

        ExtentReports extentReports = new ExtentReports();
        extentReports.attachReporter(sparkReporter);
        extentReports.setSystemInfo("Java Version", System.getProperty("java.version"));
        extentReports.setSystemInfo("OS", System.getProperty("os.name"));
        extentReports.setSystemInfo("Run Started", LocalDateTime.now().toString());
        return extentReports;
    }
}
