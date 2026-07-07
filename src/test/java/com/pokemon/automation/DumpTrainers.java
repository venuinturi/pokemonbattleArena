package com.pokemon.automation;

import com.pokemon.automation.driver.DriverManager;
import com.pokemon.automation.config.ConfigReader;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.JavascriptExecutor;
import com.pokemon.automation.pages.LoginPage;

public class DumpTrainers {
    public static void main(String[] args) throws Exception {
        WebDriver driver = DriverManager.getDriver();
        LoginPage loginPage = new LoginPage(driver);
        driver.get(ConfigReader.getProperty("url"));
        loginPage.login(ConfigReader.getProperty("email"), ConfigReader.getProperty("password"));
        Thread.sleep(2000);
        driver.get("https://pokemonbattlearena.net/members/trainers.php");
        Thread.sleep(2000);
        
        JavascriptExecutor js = (JavascriptExecutor) driver;
        String html = (String) js.executeScript("return document.body.innerHTML;");
        java.nio.file.Files.write(java.nio.file.Paths.get("target/trainers_dump.html"), html.getBytes());
        System.out.println("Dumped to target/trainers_dump.html");
        
        DriverManager.quitDriver();
    }
}
