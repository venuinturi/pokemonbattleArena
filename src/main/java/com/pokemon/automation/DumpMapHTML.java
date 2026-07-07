package com.pokemon.automation;

import com.pokemon.automation.driver.DriverManager;
import com.pokemon.automation.pages.LoginPage;
import org.openqa.selenium.WebDriver;

public class DumpMapHTML {
    public static void main(String[] args) {
        WebDriver driver = DriverManager.getDriver();
        try {
            driver.get("https://pokemonbattlearena.net/members/wander.php?M=2");
            Thread.sleep(2000);
            
            if (driver.getCurrentUrl().contains("login.php")) {
                LoginPage loginPage = new LoginPage(driver);
                loginPage.login("krish1793", "pokemon123");
                Thread.sleep(3000);
            }
            
            driver.get("https://pokemonbattlearena.net/members/wm.php");
            Thread.sleep(3000);
            
            java.nio.file.Files.write(java.nio.file.Paths.get("target/map_iframe_dump.html"), driver.getPageSource().getBytes());
            System.out.println("Dumped HTML to target/map_iframe_dump.html");
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            DriverManager.quitDriver();
        }
    }
}
