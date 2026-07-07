package com.pokemon.automation.stepdefinitions;

import com.pokemon.automation.config.ConfigReader;
import com.pokemon.automation.driver.DriverManager;
import io.cucumber.java.After;
import io.cucumber.java.Before;
import org.openqa.selenium.WebDriver;

public class Hooks {

    @Before
    public void setUp() {
        WebDriver driver = DriverManager.getDriver();
        driver.manage().deleteAllCookies();
        String url = ConfigReader.getProperty("url");
        driver.get(url);
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @After
    public void tearDown() {
        DriverManager.quitDriver();
    }
}
