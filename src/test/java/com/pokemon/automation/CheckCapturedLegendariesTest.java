package com.pokemon.automation;

import com.pokemon.automation.driver.DriverManager;
import com.pokemon.automation.pages.LoginPage;
import com.pokemon.automation.config.ConfigReader;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Arrays;
import java.io.FileWriter;

public class CheckCapturedLegendariesTest {
    private WebDriver driver;
    private LoginPage loginPage;

    @BeforeMethod
    public void setUp() {
        driver = DriverManager.getDriver();
        driver.get(ConfigReader.getProperty("url"));
        loginPage = new LoginPage(driver);
        loginPage.login(ConfigReader.getProperty("email"), ConfigReader.getProperty("password"));
    }

    @Test
    public void checkCapturedLegendaries() throws Exception {
        try {
            driver.get("https://pokemonbattlearena.net/members/mypokemon.php");
        } catch (Exception e) {
            System.out.println("Timeout or error loading page, proceeding to check source anyway: " + e.getMessage());
        }
        Thread.sleep(5000); // Give it time to render
        
        String pageSource = driver.getPageSource();
        String absPath = "/Users/venugopal/pokemon-battle-automation/target/mypokemon_dump.html";
        try (FileWriter fw = new FileWriter(absPath)) {
            fw.write(pageSource);
        }
        System.out.println("Dumped mypokemon.php to " + absPath);
    }
    @AfterMethod
    public void tearDown() {
        DriverManager.quitDriver();
    }
}
