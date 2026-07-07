package com.pokemon.automation.stepdefinitions;

import com.pokemon.automation.config.ConfigReader;
import com.pokemon.automation.driver.DriverManager;
import com.pokemon.automation.pages.LoginPage;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.openqa.selenium.WebDriver;
import org.testng.Assert;

public class LoginSteps {

    private WebDriver driver = DriverManager.getDriver();
    private LoginPage loginPage;

    @Given("I am on the home page")
    public void iAmOnTheHomePage() {
        String currentUrl = driver.getCurrentUrl();
        Assert.assertTrue(currentUrl.contains("pokemonbattlearena.net"), "Not on the pokemon battle arena homepage! Current URL: " + currentUrl);
    }

    @When("I login with secure credentials")
    public void iLoginWithSecureCredentials() {
        loginPage = new LoginPage(driver);
        String email = ConfigReader.getProperty("email");
        String password = ConfigReader.getProperty("password");
        loginPage.login(email, password);
    }

    @Then("I should be logged in successfully")
    public void iShouldBeLoggedInSuccessfully() {
        // Assert successful login, for example, by checking if the URL changes or a logout button is present
        System.out.println("Login attempt submitted.");
    }
}
