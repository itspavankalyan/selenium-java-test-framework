package com.automation.framework.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

/**
 * Page object for checkout step one (/checkout-step-one.html) — the
 * shipping-details form (first name, last name, postal code).
 *
 * <p>submitting with a field left blank does not navigate away; saucedemo
 * re-renders this same page with an error banner. That mirrors the
 * {@code LoginPage} pattern of not assuming the outcome of a submit — see
 * {@link #continueToOverview()} vs {@link #continueExpectingValidationError()}.</p>
 */
public class CheckoutInformationPage {

    private final WebDriver driver;
    private final BasePage actions;

    private static final By FIRST_NAME_INPUT = By.id("first-name");
    private static final By LAST_NAME_INPUT = By.id("last-name");
    private static final By POSTAL_CODE_INPUT = By.id("postal-code");
    private static final By CONTINUE_BUTTON = By.id("continue");
    private static final By ERROR_MESSAGE = By.cssSelector("[data-test='error']");

    public CheckoutInformationPage(WebDriver driver) {
        this.driver = driver;
        this.actions = new BasePage(driver) {
        };
    }

    public CheckoutInformationPage fillShippingDetails(String firstName, String lastName, String postalCode) {
        actions.type(FIRST_NAME_INPUT, firstName);
        actions.type(LAST_NAME_INPUT, lastName);
        actions.type(POSTAL_CODE_INPUT, postalCode);
        return this;
    }

    /** Happy path: all three fields already filled in, submit and move to the order overview. */
    public CheckoutOverviewPage continueToOverview() {
        actions.click(CONTINUE_BUTTON);
        return new CheckoutOverviewPage(driver);
    }

    /** Negative path: submit with an incomplete form and stay here to assert the validation error. */
    public CheckoutInformationPage continueExpectingValidationError() {
        actions.click(CONTINUE_BUTTON);
        return this;
    }

    public boolean isErrorDisplayed() {
        return actions.isDisplayed(ERROR_MESSAGE);
    }

    public String getErrorMessage() {
        return actions.textOf(ERROR_MESSAGE);
    }
}
