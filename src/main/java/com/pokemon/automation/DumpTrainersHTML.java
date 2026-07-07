package com.pokemon.automation;

import com.pokemon.automation.driver.DriverManager;
import com.pokemon.automation.pages.LoginPage;
import org.openqa.selenium.WebDriver;

public class DumpTrainersHTML {
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
            
            java.nio.file.Files.write(java.nio.file.Paths.get("target/trainers_dump.html"), driver.getPageSource().getBytes());
            System.out.println("Dumped HTML to target/trainers_dump.html");
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            DriverManager.quitDriver();
        }
    }
}
