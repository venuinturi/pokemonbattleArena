package com.pokemon.automation;

import com.pokemon.automation.driver.DriverManager;
import com.pokemon.automation.pages.LoginPage;
import org.openqa.selenium.WebDriver;

public class DumpBattleHTML {
    public static void main(String[] args) {
        WebDriver driver = DriverManager.getDriver();
        try {
            driver.get("https://pokemonbattlearena.net/members/trainers.php");
            Thread.sleep(2000);
            
            if (driver.getCurrentUrl().contains("login.php")) {
                LoginPage loginPage = new LoginPage(driver);
                loginPage.login(com.pokemon.automation.config.ConfigReader.getProperty("email"), 
                                com.pokemon.automation.config.ConfigReader.getProperty("password"));
                Thread.sleep(3000);
            }
            
            // Just click the first 2v2 or 3v3 battle button found
            try {
                org.openqa.selenium.WebElement btn = driver.findElement(org.openqa.selenium.By.xpath("//img[contains(@alt, '2v2')]"));
                btn.click();
            } catch (Exception e) {
                 org.openqa.selenium.WebElement btn = driver.findElement(org.openqa.selenium.By.xpath("//img[contains(@alt, '3v3')]"));
                 btn.click();
            }
            
            Thread.sleep(5000);
            
            java.nio.file.Files.write(java.nio.file.Paths.get("target/battle_2v2_dump.html"), driver.getPageSource().getBytes());
            System.out.println("Dumped HTML to target/battle_2v2_dump.html");
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            DriverManager.quitDriver();
        }
    }
}
