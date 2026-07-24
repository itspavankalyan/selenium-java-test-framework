package com.automation.framework.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

/**
 * Page object for the final order-confirmation screen (/checkout-complete.html)
 * — the last step of the end-to-end purchase workflow.
 */
public class CheckoutCompletePage {

    private final WebDriver driver;
    private final BasePage actions;

    private static final By COMPLETE_HEADER = By.className("complete-header");
    private static final By BACK_HOME_BUTTON = By.id("back-to-products");

    public CheckoutCompletePage(WebDriver driver) {
        this.driver = driver;
        this.actions = new BasePage(driver) {
        };
        // See CartPage's Javadoc for why every checkout-flow page object
        // waits for one of its own defining elements before doing anything else.
        actions.waitForVisible(COMPLETE_HEADER);
    }

    public String getConfirmationHeader() {
        return actions.textOf(COMPLETE_HEADER);
    }

    public InventoryPage backToProducts() {
        actions.clickAndWaitFor(BACK_HOME_BUTTON, By.id("inventory_container"));
        return new InventoryPage(driver);
    }
}
