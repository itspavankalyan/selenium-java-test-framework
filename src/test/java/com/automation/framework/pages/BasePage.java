package com.automation.framework.pages;

import com.automation.framework.config.ConfigReader;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

/**
 * Common plumbing shared by every page object: an explicit-wait helper and
 * the driver reference.
 *
 * <p>Deliberately avoids Selenium's {@code @FindBy}/{@code PageFactory} proxy
 * pattern. Lazily-proxied elements are convenient but they hide *when* the
 * lookup actually happens, which makes StaleElementReferenceException
 * failures much harder to reason about on a page like saucedemo's inventory
 * list where the DOM re-renders after sort/filter actions. Plain
 * {@code driver.findElement(By...)} calls, always paired with an explicit
 * wait, keep failures traceable to one line.</p>
 */
public abstract class BasePage {

    protected final WebDriver driver;
    protected final WebDriverWait wait;

    protected BasePage(WebDriver driver) {
        this.driver = driver;
        this.wait = new WebDriverWait(driver,
                Duration.ofSeconds(ConfigReader.getInt("explicit.wait.seconds")));
    }

    protected WebElement waitForVisible(By locator) {
        return wait.until(ExpectedConditions.visibilityOfElementLocated(locator));
    }

    protected WebElement waitForClickable(By locator) {
        return wait.until(ExpectedConditions.elementToBeClickable(locator));
    }

    /**
     * Clicks via JavaScript ({@code element.click()} executed in-page) rather
     * than Selenium's native {@code WebElement.click()}.
     *
     * <p>This framework's own CI run surfaced why: on GitHub Actions' headless
     * Linux runner, a handful of clicks (the cart link, the Checkout button,
     * a cart Remove button) were accepted by {@link #waitForClickable(By)}
     * without error, yet produced no effect — the browser was still on the
     * exact same page/state afterward, confirmed by inspecting the
     * automatically-captured failure screenshots. That combination — no
     * exception, no state change — points at Selenium's native click
     * dispatching a synthetic mouse event at the element's computed
     * bounding-box center, which can silently miss in headless environments
     * where rendering/scroll positioning differs subtly from a normal
     * desktop browser. It never reproduced locally (including in headless
     * mode on this machine), which is consistent with a rendering-environment
     * difference rather than an application bug.</p>
     *
     * <p>A JS-executed click sidesteps that coordinate-based dispatch
     * entirely by invoking the element's click() method directly in the
     * page's own JS context — it still fires a real {@code click} event that
     * React's event delegation picks up exactly the same way, so this is a
     * more reliable trigger, not a weaker one.</p>
     */
    protected void click(By locator) {
        WebElement element = waitForClickable(locator);
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", element);
    }

    /**
     * Types into a field, verifying the resulting value and falling back to
     * a different mechanism if it doesn't match — rather than trusting that
     * {@code sendKeys()} completing without an exception means the field is
     * actually filled.
     *
     * <p>This method's history is worth reading before changing it again.
     * Native {@code clear()}/{@code sendKeys()} is the first attempt because
     * it's the mechanism proven reliable across dozens of CI runs for the
     * login page's username/password fields — replacing it outright (an
     * earlier version of this method did exactly that, switching everything
     * to JS-set {@code element.value}) broke those previously rock-solid
     * fields instead of fixing the checkout page's flaky one: directly
     * assigning {@code element.value} doesn't go through the setter React
     * overrides on a controlled input to track its own state, so React's
     * internal value can end up out of sync with the DOM even though a
     * plain {@code input} event was dispatched — a well-documented React
     * quirk, not specific to this app.</p>
     *
     * <p>The actual, narrower problem — proven by this same verification
     * catching "Typed 'Ada' into By.id: first-name but its value did not
     * match after 3 attempts" on CI, with the identical native mechanism
     * failing all 3 retries — is that native {@code sendKeys()} isn't
     * reliably registering keystrokes on a field reached via an in-app
     * route change (as opposed to a full page load) in headless Chrome on
     * GitHub Actions' Linux runner. So the fallback here uses the
     * React-safe way to set a controlled input's value from outside React:
     * call the native {@code HTMLInputElement.prototype.value} setter
     * directly (bypassing React's override) before dispatching the
     * {@code input} event, which keeps React's internal tracking correctly
     * in sync. Native {@code sendKeys()} stays the default for every field
     * that doesn't need the fallback.</p>
     */
    protected void type(By locator, String text) {
        final int maxAttempts = 3;

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            WebElement element = waitForVisible(locator);
            if (attempt == 1) {
                element.clear();
                element.sendKeys(text);
            } else {
                setValueReactSafe(element, text);
            }
            if (text.equals(element.getAttribute("value"))) {
                return;
            }
        }
        throw new IllegalStateException(
                "Typed '%s' into %s but its value did not match after %d attempts"
                        .formatted(text, locator, maxAttempts));
    }

    private void setValueReactSafe(WebElement element, String text) {
        ((JavascriptExecutor) driver).executeScript(
                "var nativeSetter = Object.getOwnPropertyDescriptor(window.HTMLInputElement.prototype, 'value').set;"
                        + "nativeSetter.call(arguments[0], arguments[1]);"
                        + "arguments[0].dispatchEvent(new Event('input', { bubbles: true }));",
                element, text);
    }

    protected String textOf(By locator) {
        return waitForVisible(locator).getText();
    }

    protected boolean isDisplayed(By locator) {
        try {
            return waitForVisible(locator).isDisplayed();
        } catch (TimeoutException e) {
            return false;
        }
    }

    /**
     * Clicks {@code trigger} and waits for {@code expectedOutcome} to become
     * visible, re-clicking (up to 3 total attempts) if it doesn't show up
     * within a short per-attempt window.
     *
     * <p>Switching to a JS-executed {@link #click(By)} fixed most, but not
     * all, of the CI-only click flakiness this framework hit on GitHub
     * Actions' headless Linux runner. A second, related failure mode
     * remained: occasionally a click on a "Continue"/checkout-style button
     * fired before React had finished committing state from the
     * {@link #type(By, String)} calls just before it (or before a
     * just-mounted page's event handlers were fully attached after a route
     * change) — same underlying "the app wasn't quite ready yet" class of
     * race, just manifesting as a stale read instead of a dead click. A
     * single retry naturally inserts the extra beat of time React needed;
     * this is the standard, honest fix for that class of problem — retrying
     * the *action*, not lengthening a wait that was never going to help
     * (the first attempt wasn't slow, it was ineffective).</p>
     */
    protected void clickAndWaitFor(By trigger, By expectedOutcome) {
        retryClickUntil(trigger, ExpectedConditions.visibilityOfElementLocated(expectedOutcome));
    }

    /** Same retry rationale as {@link #clickAndWaitFor(By, By)}, for actions confirmed by a text change (e.g. a button's own label flipping) rather than a different element appearing. */
    protected void clickAndWaitForText(By trigger, By target, String expectedText) {
        retryClickUntil(trigger, ExpectedConditions.textToBePresentInElementLocated(target, expectedText));
    }

    private void retryClickUntil(By trigger, ExpectedCondition<?> condition) {
        final int maxAttempts = 3;
        final Duration perAttemptTimeout = Duration.ofSeconds(5);
        TimeoutException lastFailure = null;

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            click(trigger);
            try {
                new WebDriverWait(driver, perAttemptTimeout).until(condition);
                return;
            } catch (TimeoutException e) {
                lastFailure = e;
            }
        }
        throw lastFailure;
    }
}
