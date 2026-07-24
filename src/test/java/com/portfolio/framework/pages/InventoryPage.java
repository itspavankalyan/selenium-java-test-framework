package com.portfolio.framework.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

import java.util.List;

/**
 * Page object for the post-login product listing (/inventory.html).
 *
 * <p>Only exposes what current tests actually need (page-loaded assertion +
 * product count) rather than every possible interaction on this page —
 * YAGNI applies to page objects just as much as to production code. Extend
 * this class when a test genuinely needs sort/cart behaviour, not in
 * anticipation of it.</p>
 */
public class InventoryPage {

    private final WebDriver driver;
    private final BasePage actions;

    private static final By INVENTORY_CONTAINER = By.id("inventory_container");
    private static final By INVENTORY_ITEMS = By.className("inventory_item");
    private static final By PAGE_TITLE = By.className("title");

    public InventoryPage(WebDriver driver) {
        this.driver = driver;
        this.actions = new BasePage(driver) {
        };
    }

    /** True once the inventory grid has rendered — the definitive "login succeeded" signal. */
    public boolean isLoaded() {
        return actions.isDisplayed(INVENTORY_CONTAINER);
    }

    public String getPageTitle() {
        return actions.textOf(PAGE_TITLE);
    }

    public int getProductCount() {
        List<org.openqa.selenium.WebElement> items = driver.findElements(INVENTORY_ITEMS);
        return items.size();
    }
}
