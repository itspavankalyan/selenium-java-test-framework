package com.portfolio.tests.ui;

import com.portfolio.framework.base.BaseTest;
import com.portfolio.framework.pages.InventoryPage;
import com.portfolio.framework.pages.LoginPage;
import com.portfolio.framework.utils.CsvTestDataReader;
import com.portfolio.framework.utils.LoginCredentials;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.List;

/**
 * Data-driven login coverage for saucedemo.com.
 *
 * <p>This single {@code @Test} method replaces what would otherwise be
 * 14 near-identical test methods (one per credential combination) — the
 * behaviour under test never changes, only the input and the expected
 * outcome, which is exactly what a TestNG {@link DataProvider} is for.
 * Adding a 15th scenario is a one-line addition to the CSV file, not a new
 * method.</p>
 */
public class LoginTest extends BaseTest {

    @DataProvider(name = "loginCredentials")
    public Object[][] loginCredentials() {
        List<LoginCredentials> rows = CsvTestDataReader.load("login_test_data.csv", LoginCredentials.class);
        Object[][] data = new Object[rows.size()][1];
        for (int i = 0; i < rows.size(); i++) {
            data[i][0] = rows.get(i);
        }
        return data;
    }

    @Test(dataProvider = "loginCredentials",
            description = "Verifies login outcome (success, lockout, or validation error) "
                    + "for every credential combination defined in login_test_data.csv")
    public void loginWithVariousCredentials(LoginCredentials credentials) {
        LoginPage loginPage = new LoginPage(getDriver()).open();

        if (credentials.expectsSuccess()) {
            InventoryPage inventoryPage = loginPage.loginAsValidUser(
                    credentials.getUsername(), credentials.getPassword());

            Assert.assertTrue(inventoryPage.isLoaded(),
                    credentials.getTestCaseId() + ": expected inventory page to load for a valid login");
            Assert.assertTrue(inventoryPage.getProductCount() > 0,
                    credentials.getTestCaseId() + ": expected product listing to be non-empty");
        } else {
            loginPage.loginAs(credentials.getUsername(), credentials.getPassword());

            Assert.assertTrue(loginPage.isErrorDisplayed(),
                    credentials.getTestCaseId() + ": expected an error banner for invalid credentials");

            String actualMessage = loginPage.getErrorMessage();
            String expectedFragment = credentials.getExpectedMessageContains();
            if (expectedFragment != null && !expectedFragment.isBlank()) {
                Assert.assertTrue(
                        actualMessage.toLowerCase().contains(expectedFragment.toLowerCase()),
                        "%s: expected error message to contain '%s' but was '%s'"
                                .formatted(credentials.getTestCaseId(), expectedFragment, actualMessage));
            }
        }
    }
}
