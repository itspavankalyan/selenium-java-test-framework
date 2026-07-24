package com.automation.tests.ui;

import com.automation.framework.base.BaseTest;
import com.automation.framework.listeners.ExtentTestListener;
import com.automation.framework.pages.CartPage;
import com.automation.framework.pages.CheckoutCompletePage;
import com.automation.framework.pages.CheckoutInformationPage;
import com.automation.framework.pages.CheckoutOverviewPage;
import com.automation.framework.pages.InventoryPage;
import com.automation.framework.pages.LoginPage;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.List;

/**
 * End-to-end UI workflows on top of the same Page Object Model used by
 * {@link LoginTest} — this class is what actually exercises the multi-page
 * chain (inventory -&gt; cart -&gt; checkout -&gt; confirmation), which a
 * login-only suite can't demonstrate on its own.
 *
 * <p>Every test here starts from a fresh login. That's a deliberate choice,
 * not an oversight: {@code BaseTest} spins up a brand-new {@code WebDriver}
 * per {@code @Test} method (see its Javadoc), so there is no persisted
 * browser session to resume between methods, and no test in this class
 * depends on another having run first. Each test is a complete, independent
 * story a reader can follow top to bottom without needing to know what ran
 * before it — the same reasoning behind why {@code BookingApiChainTest} uses
 * {@code dependsOnMethods} for its chain but this class does not: an API
 * chain is proving one continuous server-side flow, whereas these tests are
 * proving independent user journeys that happen to share a starting page.</p>
 */
public class CheckoutWorkflowTest extends BaseTest {

    // saucedemo's fixed catalog — hardcoded here rather than scraped, since
    // these tests need to name specific, known products to interact with.
    private static final String BACKPACK = "Sauce Labs Backpack";
    private static final String BIKE_LIGHT = "Sauce Labs Bike Light";

    private static final String STANDARD_USER = "standard_user";
    private static final String PASSWORD = "secret_sauce";

    /** Logs in as the one account guaranteed to have a working, non-broken UI (see login_test_data.csv for why the other accounts aren't used here). */
    private InventoryPage loginAsStandardUser() {
        return new LoginPage(getDriver()).open().loginAsValidUser(STANDARD_USER, PASSWORD);
    }

    @Test(description = "Full purchase journey: add an item, check out, confirm the order, and land back on a now-empty cart")
    public void shouldCompleteEndToEndPurchase() {
        InventoryPage inventoryPage = loginAsStandardUser();
        ExtentTestListener.logStep("Logged in as '%s' and reached the inventory page".formatted(STANDARD_USER));

        inventoryPage.addProductToCart(BACKPACK);
        Assert.assertEquals(inventoryPage.getCartBadgeCount(), 1,
                "Expected the cart badge to reflect the one item just added");
        ExtentTestListener.logStep("Added '%s' to the cart".formatted(BACKPACK));

        CartPage cartPage = inventoryPage.goToCart();
        Assert.assertEquals(cartPage.getItemNames(), List.of(BACKPACK),
                "Expected the cart to contain exactly the item added on the inventory page");
        ExtentTestListener.logStep("Opened the cart and confirmed it contains '%s'".formatted(BACKPACK));

        CheckoutInformationPage checkoutInfo = cartPage.proceedToCheckout();
        CheckoutOverviewPage overview = checkoutInfo
                .fillShippingDetails("Ada", "Lovelace", "94105")
                .continueToOverview();

        Assert.assertEquals(overview.getItemCount(), 1, "Expected one item carried through to the order overview");
        Assert.assertTrue(overview.getTotalLabel().contains("Total:"),
                "Expected a 'Total:' summary line before the order is finalised");
        ExtentTestListener.logStep("Filled shipping details and reached the order overview");

        CheckoutCompletePage confirmation = overview.finishOrder();
        Assert.assertTrue(confirmation.getConfirmationHeader().contains("Thank you"),
                "Expected a thank-you confirmation once the order is placed");
        ExtentTestListener.logStep("Order confirmed: " + confirmation.getConfirmationHeader());

        // Round-trip back to the product list and confirm the cart was cleared
        // server-side as part of completing the order — not just that the
        // confirmation page *said* the order succeeded.
        InventoryPage backToShopping = confirmation.backToProducts();
        Assert.assertTrue(backToShopping.isLoaded(), "Expected 'back to products' to land on the inventory page");
        Assert.assertEquals(backToShopping.getCartBadgeCount(), 0, "Expected the cart to be empty after order completion");
    }

    @Test(description = "Add two items, remove one directly from the inventory page, and confirm only the remaining item reaches the cart")
    public void shouldAddAndRemoveItemsFromCart() {
        InventoryPage inventoryPage = loginAsStandardUser();

        inventoryPage.addProductToCart(BACKPACK).addProductToCart(BIKE_LIGHT);
        Assert.assertEquals(inventoryPage.getCartBadgeCount(), 2, "Expected the badge to count both added items");
        ExtentTestListener.logStep("Added '%s' and '%s' to the cart".formatted(BACKPACK, BIKE_LIGHT));

        // saucedemo re-labels the same button "Remove" once an item is in the
        // cart (see InventoryPage.removeProductFromCart) rather than exposing
        // a separate control, so this is still an inventory-page interaction.
        inventoryPage.removeProductFromCart(BACKPACK);
        Assert.assertEquals(inventoryPage.getCartBadgeCount(), 1, "Expected the badge to drop back to 1 after removing an item");

        CartPage cartPage = inventoryPage.goToCart();
        Assert.assertEquals(cartPage.getItemNames(), List.of(BIKE_LIGHT),
                "Expected only the item that was never removed to reach the cart");
    }

    @Test(description = "Sort by price low-to-high and verify the displayed prices are actually in ascending order, not just that the dropdown accepted the selection")
    public void shouldSortProductsByPriceLowToHigh() {
        InventoryPage inventoryPage = loginAsStandardUser();

        inventoryPage.sortBy("lohi");
        List<Double> prices = inventoryPage.getProductPrices();

        List<Double> expectedOrder = prices.stream().sorted().toList();
        Assert.assertEquals(prices, expectedOrder,
                "Expected product prices to be in ascending order after selecting 'Price (low to high)'");
    }

    @Test(description = "Attempt checkout with the postal code left blank and confirm the form blocks progress with a validation error")
    public void shouldBlockCheckoutWhenPostalCodeIsMissing() {
        InventoryPage inventoryPage = loginAsStandardUser();

        CheckoutInformationPage checkoutInfo = inventoryPage
                .addProductToCart(BACKPACK)
                .goToCart()
                .proceedToCheckout();

        // Postal code deliberately left blank — this is the negative-path
        // counterpart to shouldCompleteEndToEndPurchase's happy path.
        checkoutInfo.fillShippingDetails("Ada", "Lovelace", "");
        checkoutInfo.continueExpectingValidationError();

        Assert.assertTrue(checkoutInfo.isErrorDisplayed(), "Expected a validation error when postal code is missing");
        Assert.assertTrue(checkoutInfo.getErrorMessage().toLowerCase().contains("postal code"),
                "Expected the error message to name the missing field, but was: " + checkoutInfo.getErrorMessage());
    }
}
