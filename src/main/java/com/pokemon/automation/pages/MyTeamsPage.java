package com.pokemon.automation.pages;

import com.pokemon.automation.base.BasePage;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.List;

public class MyTeamsPage extends BasePage {

    public MyTeamsPage(WebDriver driver) {
        super(driver);
    }

    public void updateBattleTeam(String teamName) {
        System.out.println("Updating battle team: " + teamName);
        if (!driver.getCurrentUrl().contains("team.php")) {
            driver.get("https://pokemonbattlearena.net/members/team.php");
            try { Thread.sleep(2000); } catch (Exception e) {}
        }
        
        // 1. Delete existing team if present
        deleteTeamIfExists(teamName);
        
        // 2. Create new team
        createNewTeam(teamName);
    }
    
    private void deleteTeamIfExists(String teamName) {
        try {
            // Find the team name in the span, go up to its containing row/table, and find the remove link
            List<WebElement> removeLinks = driver.findElements(By.xpath("//table[descendant::span[text()='" + teamName + "']]//a[contains(@href, 'btnRemove')]"));
            if (!removeLinks.isEmpty()) {
                System.out.println("Found existing team '" + teamName + "'. Deleting it...");
                removeLinks.get(0).click();
                try { Thread.sleep(2000); } catch (Exception e) {}
                System.out.println("Team deleted.");
            } else {
                System.out.println("No existing team named '" + teamName + "' found to delete.");
            }
        } catch (Exception e) {
            System.out.println("Failed to delete team '" + teamName + "': " + e.getMessage());
        }
    }
    
    private void createNewTeam(String teamName) {
        try {
            WebElement txtName = driver.findElement(By.id("txtname"));
            txtName.clear();
            txtName.sendKeys(teamName);
            
            WebElement btnSave = driver.findElement(By.id("btnSave"));
            btnSave.click();
            try { Thread.sleep(2000); } catch (Exception e) {}
            
            System.out.println("Created new team: " + teamName);
        } catch (Exception e) {
            System.out.println("Failed to create new team '" + teamName + "': " + e.getMessage());
        }
    }
}
