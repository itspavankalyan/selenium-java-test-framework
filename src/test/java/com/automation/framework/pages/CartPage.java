package com.automation.framework.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.List;

/**
 * Page object for the shopping cart (/cart.html) — the step between adding
 * products on the inventory page and starting checkout.
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
    }

    public int getItemCount() {
        return driver.findElements(CART_ITEMS).size();
    }

    public List<String> getItemNames() {
        return driver.findElements(CART_ITEM_NAMES).stream().map(WebElement::getText).toList();
    }

    public CheckoutInformationPage proceedToCheckout() {
        actions.click(CHECKOUT_BUTTON);
        return new CheckoutInformationPage(driver);
    }
}
