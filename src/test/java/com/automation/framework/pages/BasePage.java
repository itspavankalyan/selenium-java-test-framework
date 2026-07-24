package com.automation.framework.pages;

import com.automation.framework.config.ConfigReader;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
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

    protected void type(By locator, String text) {
        WebElement element = waitForVisible(locator);
        element.clear();
        element.sendKeys(text);
    }

    protected String textOf(By locator) {
        return waitForVisible(locator).getText();
    }

    protected boolean isDisplayed(By locator) {
        try {
            return waitForVisible(locator).isDisplayed();
        } catch (org.openqa.selenium.TimeoutException e) {
            return false;
        }
    }

    /**
     * Waits until the element at {@code locator} contains {@code expectedText}.
     *
     * <p>Distinct from {@link #waitForVisible(By)} on purpose: an element can
     * be visible with *stale* content the instant after a click triggers a
     * client-side re-render (e.g. a cart badge that still shows the
     * pre-removal count for a few hundred milliseconds). Waiting only for
     * visibility passes immediately in that case and reads the old value —
     * this method instead polls until the actual expected state appears,
     * which is what genuinely confirms the app finished processing the
     * action rather than just rendering *something*.</p>
     */
    protected void waitForTextToContain(By locator, String expectedText) {
        wait.until(ExpectedConditions.textToBePresentInElementLocated(locator, expectedText));
    }
}
