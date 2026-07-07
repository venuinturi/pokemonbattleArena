package com.pokemon.automation;

import com.pokemon.automation.driver.DriverManager;
import com.pokemon.automation.pages.LoginPage;
import org.openqa.selenium.WebDriver;
import java.io.FileWriter;
import java.io.IOException;

public class DumpMapGuide {
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
                driver.get("https://pokemonbattlearena.net/members/mapguide.php");
                Thread.sleep(2000);
            }
            
            driver.get("https://pokemonbattlearena.net/members/mapguide.php");
            Thread.sleep(3000);
            
            String pageSource = driver.getPageSource();
            
            try (FileWriter file = new FileWriter("target/mapguide_dump.html")) {
                file.write(pageSource);
                System.out.println("Dumped to target/mapguide_dump.html");
            } catch (IOException e) {
                e.printStackTrace();
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            DriverManager.quitDriver();
        }
    }
}
