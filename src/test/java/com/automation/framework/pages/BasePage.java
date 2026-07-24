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
     * Clears the field and types {@code text}, then verifies the field's
     * actual value ended up matching what was sent — retrying the whole
     * clear+type if not (up to 3 attempts).
     *
     * <p>This framework's CI run showed why a bare {@code clear()}/{@code sendKeys()}
     * isn't always enough: a checkout form field occasionally reported as
     * empty in a later validation error despite being typed into moments
     * earlier, even after every navigation click leading up to it had
     * already been made retry-safe. React-controlled inputs only update
     * their internal state in response to a genuine input event reaching an
     * attached listener; typing into a just-mounted field before that
     * listener is wired up can leave keystrokes visually present but
     * functionally not registered, so the very next re-render reverts the
     * field. Verifying the resulting value (not just trusting sendKeys()
     * completed without throwing) and retrying closes that gap the same way
     * {@link #clickAndWaitFor(By, By)} does for clicks.</p>
     */
    protected void type(By locator, String text) {
        final int maxAttempts = 3;

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            WebElement element = waitForVisible(locator);
            element.clear();
            element.sendKeys(text);
            if (text.equals(element.getAttribute("value"))) {
                return;
            }
        }
        throw new IllegalStateException(
                "Typed '%s' into %s but its value did not match after %d attempts"
                        .formatted(text, locator, maxAttempts));
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
