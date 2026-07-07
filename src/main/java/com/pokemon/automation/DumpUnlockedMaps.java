package com.pokemon.automation;

import com.pokemon.automation.driver.DriverManager;
import com.pokemon.automation.pages.LoginPage;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.By;
import java.util.List;

public class DumpUnlockedMaps {
    public static void main(String[] args) {
        WebDriver driver = DriverManager.getDriver();
        try {
            driver.get("https://pokemonbattlearena.net/members/mapguide.php");
            Thread.sleep(2000);
            
            if (driver.getCurrentUrl().contains("login.php")) {
                
                LoginPage loginPage = new LoginPage(driver);
                loginPage.login(com.pokemon.automation.config.ConfigReader.getProperty("email"), 
                                com.pokemon.automation.config.ConfigReader.getProperty("password"));
                Thread.sleep(3000);
            }
            
            driver.get("https://pokemonbattlearena.net/members/");
            Thread.sleep(3000);
            
            System.out.println("--- Unlocked Maps ---");
            List<WebElement> maps = driver.findElements(By.tagName("a"));
            for (WebElement map : maps) {
                String href = map.getAttribute("href");
                if (href != null && href.contains("map.php") || href.contains("wander.php")) {
                    System.out.println(map.getText() + " -> " + href);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            DriverManager.quitDriver();
        }
    }
}
