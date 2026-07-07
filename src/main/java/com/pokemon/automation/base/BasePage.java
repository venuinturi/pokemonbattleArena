package com.pokemon.automation.base;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.PageFactory;
import org.openqa.selenium.support.ui.WebDriverWait;
import java.time.Duration;

public class BasePage {
    protected WebDriver driver;
    protected WebDriverWait wait;

    public BasePage(WebDriver driver) {
        this.driver = driver;
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        PageFactory.initElements(driver, this);
    }

    public void closeAdIfPresent() {
        try {
            // Wait briefly for ad to potentially appear, but don't block for long
            java.util.List<org.openqa.selenium.WebElement> closeButtons = driver.findElements(org.openqa.selenium.By.xpath("//div[contains(@class, 'ad-close')] | //button[contains(@class, 'close')] | //a[contains(@class, 'close')] | //*[contains(@id, 'close-ad')] | //div[contains(@id, 'ad') and @style='display: block;']//button"));
            for (org.openqa.selenium.WebElement btn : closeButtons) {
                if (btn.isDisplayed()) {
                    btn.click();
                    System.out.println("Closed an ad popup.");
                    break;
                }
            }
        } catch (Exception e) {
            // Ignore
        }
    }
}
