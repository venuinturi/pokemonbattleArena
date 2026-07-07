package com.pokemon.automation.debug;

import com.pokemon.automation.driver.DriverManager;
import com.pokemon.automation.config.ConfigReader;
import com.pokemon.automation.pages.LoginPage;
import org.openqa.selenium.WebDriver;

public class MyTeamsDumper {
    public static void main(String[] args) {
        WebDriver driver = DriverManager.getDriver();
        driver.get("https://pokemonbattlearena.net/members/login.php");
        LoginPage loginPage = new LoginPage(driver);
        loginPage.login(ConfigReader.getProperty("email"), ConfigReader.getProperty("password"));
        
        try { Thread.sleep(3000); } catch(Exception e) {}
        
        driver.get("https://pokemonbattlearena.net/members/team.php");
        try { Thread.sleep(2000); } catch(Exception e) {}
        
        System.out.println("=== TEAM HTML DUMP ===");
        System.out.println(driver.getPageSource());
        
        driver.quit();
    }
}
