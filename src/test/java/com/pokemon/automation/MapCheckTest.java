package com.pokemon.automation;

import org.testng.annotations.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import io.github.bonigarcia.wdm.WebDriverManager;
import com.pokemon.automation.config.ConfigReader;

import java.util.*;

public class MapCheckTest {

    @Test
    public void checkMaps() throws Exception {
        WebDriver driver = com.pokemon.automation.driver.DriverManager.getDriver();

        try {
            // 1. Login
            driver.get(com.pokemon.automation.config.ConfigReader.getProperty("url"));
            com.pokemon.automation.pages.LoginPage loginPage = new com.pokemon.automation.pages.LoginPage(driver);
            loginPage.login(ConfigReader.getProperty("email"), ConfigReader.getProperty("password"));
            Thread.sleep(3000);

            // Navigate to tier list
            driver.get("https://pokemonbattlearena.net/members/pokedex_tierlist.php");
            Thread.sleep(3000);
            java.nio.file.Files.writeString(java.nio.file.Paths.get("target/tierlist_dump.html"), driver.getPageSource());
            System.out.println("Dumped tier list to target/tierlist_dump.html");
            
        } finally {
            com.pokemon.automation.driver.DriverManager.quitDriver();
        }
    }
}
