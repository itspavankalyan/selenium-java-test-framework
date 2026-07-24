package com.automation.framework.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.List;

/**
 * Page object for the shopping cart (/cart.html) — the step between adding
 * products on the inventory page and starting checkout.
 *
 * <p>The constructor waits for {@link #CHECKOUT_BUTTON} (present regardless
 * of how many items are in the cart) before returning. This page is reached
 * via a client-side SPA navigation rather than a full page load, so without
 * an explicit wait here a caller could read cart contents — or saucedemo's
 * inventory grid, which happens to reuse the identical
 * {@code inventory_item_name} CSS class for its own product names — a beat
 * before the cart has actually finished rendering. That exact race is what
 * caused this framework's own CI run to intermittently report the wrong
 * item count on a slower runner even though it never reproduced locally;
 * waiting here, once, up front, is more robust than adding a wait to every
 * read method individually.</p>
 */
public class CartPage {

    private final WebDriver driver;
    private final BasePage actions;

    private static final By CART_ITEMS = By.className("cart_item");
    private static final By CART_ITEM_NAMES = By.className("inventory_item_name");
    private static final By CHECKOUT_BUTTON = By.id("checkout");

    public CartPage(WebDriver driver) {
        this.driver = driver;
        this.actions = new BasePage(driver) {
        };
        actions.waitForVisible(CHECKOUT_BUTTON);
    }

    public int getItemCount() {
        return driver.findElements(CART_ITEMS).size();
    }

    public List<String> getItemNames() {
        return driver.findElements(CART_ITEM_NAMES).stream().map(WebElement::getText).toList();
    }

    public CheckoutInformationPage proceedToCheckout() {
        actions.clickAndWaitFor(CHECKOUT_BUTTON, By.id("first-name"));
        return new CheckoutInformationPage(driver);
    }
}
