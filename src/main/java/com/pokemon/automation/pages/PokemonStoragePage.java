package com.pokemon.automation.pages;

import com.pokemon.automation.base.BasePage;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.ui.ExpectedConditions;

import java.util.List;

public class PokemonStoragePage extends BasePage {

    @FindBy(css = ".pc-pokemon-item")
    private List<WebElement> storagePokemons;
    
    @FindBy(css = ".team-pokemon-item")
    private List<WebElement> teamPokemons;
    
    @FindBy(xpath = "//button[contains(text(), 'Withdraw')]")
    private WebElement withdrawButton;
    
    @FindBy(xpath = "//button[contains(text(), 'Deposit')]")
    private WebElement depositButton;

    public PokemonStoragePage(WebDriver driver) {
        super(driver);
    }

    public void loadTeam(String teamName) {
        try {
            org.openqa.selenium.JavascriptExecutor js = (org.openqa.selenium.JavascriptExecutor) driver;
            String lowerTeamName = teamName.toLowerCase();
            
            // The HTML structure is: <b><center><span>Team Name</span></center></b> ... <table ...> ... <img src="...loadTeam.gif"...>
            // We find the span containing the team name, get its following sibling table, and click the loadTeam.gif image inside it.
            try {
                String xpath = "//span[translate(text(), 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz')='" + lowerTeamName + "']" +
                               "/ancestor::b/following-sibling::table[1]//img[contains(@src, 'loadTeam')]";
                WebElement loadBtn = wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath(xpath)));
                js.executeScript("arguments[0].click();", loadBtn);
                System.out.println("Successfully clicked 'Load Team' icon for: " + teamName);
                try { Thread.sleep(2000); } catch (Exception e) {}
                return; // Done!
            } catch (Exception e) {
                System.out.println("Could not find structured team load button for: " + teamName + ". Falling back to generic search...");
            }
            
            // Fallback just in case the HTML structure changes slightly
            boolean found = false;
            try {
                WebElement selectElement = driver.findElement(By.xpath("//select[.//option[contains(translate(text(), 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz'), '" + lowerTeamName + "')]]"));
                new org.openqa.selenium.support.ui.Select(selectElement).selectByVisibleText(teamName);
                found = true;
            } catch (Exception e) {}
            
            if (!found) {
                try {
                    WebElement teamElement = driver.findElement(By.xpath(
                        "//*[contains(translate(text(), 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz'), '" + lowerTeamName + "')] | " +
                        "//option[contains(translate(text(), 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz'), '" + lowerTeamName + "')] | " +
                        "//input[contains(translate(@value, 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz'), '" + lowerTeamName + "')] | " +
                        "//button[contains(translate(text(), 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz'), '" + lowerTeamName + "')]"
                    ));
                    js.executeScript("arguments[0].click();", teamElement);
                    found = true;
                } catch (Exception e) {}
            }
            
            try {
                WebElement loadBtn = driver.findElement(By.xpath("//button[contains(translate(text(), 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz'), 'load')] | //button[contains(translate(text(), 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz'), 'set active')] | //input[contains(translate(@value, 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz'), 'load')] | //input[contains(translate(@value, 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz'), 'set active')]"));
                js.executeScript("arguments[0].click();", loadBtn);
            } catch (Exception e) {}
            
            try { Thread.sleep(2000); } catch (Exception e) {}
        } catch (Exception e) {
            System.out.println("Could not load team " + teamName + ": " + e.getMessage());
            throw new RuntimeException("Failed to load team: " + teamName, e);
        }
    }

    public void swapToLowestLevelPokemon() {
        // Pseudo-logic to deposit current team and withdraw lowest level pokemon
        if (!teamPokemons.isEmpty()) {
            for (WebElement teamPokemon : teamPokemons) {
                wait.until(ExpectedConditions.elementToBeClickable(teamPokemon)).click();
                wait.until(ExpectedConditions.elementToBeClickable(depositButton)).click();
                try { Thread.sleep(500); } catch (Exception e) {}
            }
        }
        
        if (!storagePokemons.isEmpty()) {
            // Very basic approach: sort or just repeatedly find the minimum and withdraw up to 6 times
            for (int i = 0; i < 6 && i < storagePokemons.size(); i++) {
                WebElement lowestLevelStorage = null;
                int minLevel = Integer.MAX_VALUE;
                
                // Re-find elements if DOM changes after withdrawal
                List<WebElement> currentStoragePokemons = driver.findElements(By.cssSelector(".pc-pokemon-item"));
                if (currentStoragePokemons.isEmpty()) break;
                
                for (WebElement storageMon : currentStoragePokemons) {
                    int level = extractLevelFromText(storageMon.getText());
                    if (level < minLevel) {
                        minLevel = level;
                        lowestLevelStorage = storageMon;
                    }
                }
                
                if (lowestLevelStorage != null) {
                    wait.until(ExpectedConditions.elementToBeClickable(lowestLevelStorage)).click();
                    wait.until(ExpectedConditions.elementToBeClickable(withdrawButton)).click();
                    try { Thread.sleep(500); } catch (Exception e) {}
                }
            }
        }
    }
    
    private int extractLevelFromText(String text) {
        try {
            String numStr = text.replaceAll("[^0-9]", "");
            return numStr.isEmpty() ? 999 : Integer.parseInt(numStr);
        } catch (Exception e) {
            return 999;
        }
    }
}
