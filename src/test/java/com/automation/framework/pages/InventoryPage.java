package com.automation.framework.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.Select;

import java.util.List;

/**
 * Page object for the post-login product listing (/inventory.html).
 *
 * <p>Exposes both the read-only assertions the login tests need (page-loaded
 * check, product count) and the cart/sort interactions the end-to-end
 * checkout tests need — this page is the starting point for every workflow
 * beyond "did login succeed", so it naturally grows alongside the tests that
 * use it.</p>
 */
public class InventoryPage {

    private final WebDriver driver;
    private final BasePage actions;

    private static final By INVENTORY_CONTAINER = By.id("inventory_container");
    private static final By INVENTORY_ITEMS = By.className("inventory_item");
    private static final By PAGE_TITLE = By.className("title");
    private static final By PRODUCT_NAMES = By.className("inventory_item_name");
    private static final By PRODUCT_PRICES = By.className("inventory_item_price");
    private static final By SORT_DROPDOWN = By.className("product_sort_container");
    private static final By CART_BADGE = By.className("shopping_cart_badge");
    private static final By CART_LINK = By.className("shopping_cart_link");

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
        return driver.findElements(INVENTORY_ITEMS).size();
    }

    public List<String> getProductNames() {
        return driver.findElements(PRODUCT_NAMES).stream().map(WebElement::getText).toList();
    }

    /**
     * Reads every displayed price and parses it to a double for numeric
     * assertions (e.g. verifying sort order) — prices render as {@code "$29.99"},
     * so the leading currency symbol has to be stripped before parsing.
     */
    public List<Double> getProductPrices() {
        return driver.findElements(PRODUCT_PRICES).stream()
                .map(element -> Double.parseDouble(element.getText().replace("$", "")))
                .toList();
    }

    /**
     * Selects a sort option from the dropdown. saucedemo's own option values
     * are {@code "az"}, {@code "za"}, {@code "lohi"} (price low-to-high) and
     * {@code "hilo"} (price high-to-low) — passed straight through rather than
     * wrapped in an enum, since this is the one place in the framework that
     * needs them and an enum would just be indirection for four literals.
     */
    public InventoryPage sortBy(String sortOptionValue) {
        Select sortSelect = new Select(actions.waitForVisible(SORT_DROPDOWN));
        sortSelect.selectByValue(sortOptionValue);
        return this;
    }

    /**
     * Adds a product to the cart by its visible name rather than by guessing
     * saucedemo's generated element id (which encodes the product name into
     * a slug, e.g. parentheses and dots for "Test.allTheThings() T-Shirt").
     * Locating by the name actually shown on screen is both more readable in
     * a failure trace and immune to id-encoding quirks.
     *
     * <p>Waits for the button's own label to flip to "Remove" before
     * returning, re-clicking if it hasn't within a few seconds, rather than
     * trusting that a single click means the app has finished processing it
     * — see {@code BasePage.clickAndWaitForText} for why a bare click isn't
     * reliable enough here on a CI runner.</p>
     */
    public InventoryPage addProductToCart(String productName) {
        By button = addToCartButtonFor(productName);
        actions.clickAndWaitForText(button, button, "Remove");
        return this;
    }

    /** Mirror of {@link #addProductToCart(String)} — saucedemo swaps the same button's label back to "Add to cart" once removed; see that method's Javadoc for why this retries until the label actually changes. */
    public InventoryPage removeProductFromCart(String productName) {
        By button = removeButtonFor(productName);
        actions.clickAndWaitForText(button, button, "Add to cart");
        return this;
    }

    /** saucedemo hides the badge entirely at zero items rather than rendering "0", so absence means an empty cart. */
    public int getCartBadgeCount() {
        if (!actions.isDisplayed(CART_BADGE)) {
            return 0;
        }
        return Integer.parseInt(actions.textOf(CART_BADGE));
    }

    public CartPage goToCart() {
        // clickAndWaitFor (not plain click): see BasePage's Javadoc — a bare
        // click here was the first symptom of the CI-only navigation race
        // this framework hit, so the fix belongs at every navigation trigger.
        actions.clickAndWaitFor(CART_LINK, By.id("checkout"));
        return new CartPage(driver);
    }

    private By addToCartButtonFor(String productName) {
        // contains(@class, ...) rather than an exact @class match: saucedemo
        // renders this div's class attribute with a trailing space
        // (`class="inventory_item_name "`), presumably a leftover from a
        // conditional class-name template — confirmed by inspecting the live
        // DOM. normalize-space() on the text guards against the same kind of
        // incidental whitespace in the product name text node.
        return By.xpath(
                ("//div[contains(@class, 'inventory_item_name') and normalize-space(text())='%s']"
                        + "/ancestor::div[@class='inventory_item']//button")
                        .formatted(productName));
    }

    private By removeButtonFor(String productName) {
        // Same button element as add-to-cart — saucedemo re-labels it in place
        // rather than swapping in a different element, so the locator is identical.
        return addToCartButtonFor(productName);
    }
}
