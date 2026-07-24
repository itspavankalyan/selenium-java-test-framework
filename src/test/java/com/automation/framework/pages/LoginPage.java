package com.automation.framework.pages;

import com.automation.framework.config.ConfigReader;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

/**
 * Page object for saucedemo.com's login screen.
 *
 * <p>Every locator lives here and nowhere else. If saucedemo changes an
 * element id tomorrow, exactly one file needs a fix — every test that logs in
 * (today: login tests; tomorrow: any test that needs an authenticated session
 * as a precondition) keeps working unchanged. That single-responsibility
 * boundary is the entire point of the Page Object Model.</p>
 */
public class LoginPage {

    private final WebDriver driver;
    private final BasePage actions;

    // ---- Locators ----
    private static final By USERNAME_INPUT = By.id("user-name");
    private static final By PASSWORD_INPUT = By.id("password");
    private static final By LOGIN_BUTTON = By.id("login-button");
    private static final By ERROR_MESSAGE = By.cssSelector("[data-test='error']");

    public LoginPage(WebDriver driver) {
        this.driver = driver;
        // Anonymous BasePage subclass: LoginPage doesn't need its own wait
        // configuration, just the shared helpers — this avoids duplicating
        // wait/locator boilerplate in every page object.
        this.actions = new BasePage(driver) {
        };
    }

    /** Navigates directly to the login page rather than assuming a fresh session. */
    public LoginPage open() {
        driver.get(ConfigReader.uiBaseUrl());
        return this;
    }

    public LoginPage enterUsername(String username) {
        actions.type(USERNAME_INPUT, username);
        return this;
    }

    public LoginPage enterPassword(String password) {
        actions.type(PASSWORD_INPUT, password);
        return this;
    }

    /**
     * Submits the form without assuming an outcome. A login attempt can land
     * on either the inventory page (valid credentials) or back on this same
     * page with an error banner (invalid credentials, locked-out user) — the
     * caller decides which page object to assert against next, so this method
     * deliberately returns {@code this} rather than guessing.
     */
    public LoginPage submitLogin() {
        actions.click(LOGIN_BUTTON);
        return this;
    }

    /** Convenience one-shot for the common negative-path case: fill in, submit, stay here. */
    public LoginPage loginAs(String username, String password) {
        return enterUsername(username).enterPassword(password).submitLogin();
    }

    /**
     * Convenience one-shot for the happy path, where the caller already knows
     * the credentials are valid and wants the resulting page object directly.
     */
    public InventoryPage loginAsValidUser(String username, String password) {
        loginAs(username, password);
        return new InventoryPage(driver);
    }

    public boolean isErrorDisplayed() {
        return actions.isDisplayed(ERROR_MESSAGE);
    }

    public String getErrorMessage() {
        return actions.textOf(ERROR_MESSAGE);
    }
}
