package com.portfolio.framework.utils;

import com.opencsv.bean.CsvBindByName;

/**
 * Row-per-record mapping for {@code login_test_data.csv}.
 *
 * <p>OpenCSV populates this via the {@code @CsvBindByName} annotations, matched
 * against the CSV header — column order in the file can change without
 * breaking deserialization, only the header names matter.</p>
 */
public class LoginCredentials {

    @CsvBindByName
    private String testCaseId;

    @CsvBindByName
    private String username;

    @CsvBindByName
    private String password;

    @CsvBindByName
    private String expectedOutcome;

    @CsvBindByName
    private String expectedMessageContains;

    public String getTestCaseId() {
        return testCaseId;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public boolean expectsSuccess() {
        return "SUCCESS".equalsIgnoreCase(expectedOutcome);
    }

    public String getExpectedMessageContains() {
        return expectedMessageContains;
    }

    @Override
    public String toString() {
        // Drives the display name TestNG shows in reports for each data-provider
        // iteration — without this override every row shows up as an opaque
        // "LoginCredentials@1a2b3c", which defeats the point of a readable report.
        return "%s [username='%s']".formatted(testCaseId, username);
    }
}
