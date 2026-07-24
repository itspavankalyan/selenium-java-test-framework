package com.automation.framework.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

/**
 * Page object for checkout step two (/checkout-step-two.html) — the order
 * summary shown just before the customer commits to the purchase.
 */
public class CheckoutOverviewPage {

    private final WebDriver driver;
    private final BasePage actions;

    private static final By CART_ITEMS = By.className("cart_item");
    private static final By ITEM_TOTAL_LABEL = By.className("summary_subtotal_label");
    private static final By TOTAL_LABEL = By.className("summary_total_label");
    private static final By FINISH_BUTTON = By.id("finish");

    public CheckoutOverviewPage(WebDriver driver) {
        this.driver = driver;
        this.actions = new BasePage(driver) {
        };
    }

    public int getItemCount() {
        return driver.findElements(CART_ITEMS).size();
    }

    /** Raw label text, e.g. "Item total: $29.99" — kept as a string since tests only need to assert it's present/non-empty, not parse it. */
    public String getItemTotalLabel() {
        return actions.textOf(ITEM_TOTAL_LABEL);
    }

    /** e.g. "Total: $32.24" (item total + tax) — the figure that matters most to a customer reviewing before confirming. */
    public String getTotalLabel() {
        return actions.textOf(TOTAL_LABEL);
    }

    public CheckoutCompletePage finishOrder() {
        actions.click(FINISH_BUTTON);
        return new CheckoutCompletePage(driver);
    }
}
