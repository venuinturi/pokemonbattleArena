package com.pokemon.automation.stepdefinitions;

import com.pokemon.automation.driver.DriverManager;
import com.pokemon.automation.pages.BattlePage;
import com.pokemon.automation.pages.MapNavigationPage;
import com.pokemon.automation.pages.PokemonCenterPage;
import com.pokemon.automation.pages.PokemonStoragePage;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.ArrayList;
import java.util.List;

public class HuntingSteps {

    private WebDriver driver = DriverManager.getDriver();
    private MapNavigationPage mapPage = new MapNavigationPage(driver);
    private BattlePage battlePage = new BattlePage(driver);

    private PokemonCenterPage centerPage = new PokemonCenterPage(driver);
    private PokemonStoragePage storagePage = new PokemonStoragePage(driver);
    
    private List<String> availableMaps = new ArrayList<>();
    private int currentMapIndex = 0;
    private boolean needsPokeballs = false;

    @Given("I log in to Pokemon Battle Arena")
    public void iLogInToPokemonBattleArena() {
        com.pokemon.automation.pages.LoginPage loginPage = new com.pokemon.automation.pages.LoginPage(driver);
        driver.get(com.pokemon.automation.config.ConfigReader.getProperty("url"));
        loginPage.login(
            com.pokemon.automation.config.ConfigReader.getProperty("email"), 
            com.pokemon.automation.config.ConfigReader.getProperty("password")
        );
        try { Thread.sleep(2000); } catch(Exception e) {}
    }

    @Given("I load the team {string} under Trainer HQ")
    public void iLoadTheTeamUnderTrainerHQ(String teamName) {
        System.out.println("Navigating to Trainer HQ to load team...");
        
        try {
            System.out.println("Navigating to My Pokemon Teams page...");
            driver.get("https://pokemonbattlearena.net/members/team.php");
            try { Thread.sleep(2000); } catch (Exception e) {}
            
            try {
                java.nio.file.Files.write(java.nio.file.Paths.get("target/team_dump.html"), driver.getPageSource().getBytes());
                System.out.println("Dumped team.php to target/team_dump.html");
            } catch (Exception e) {}
            
            storagePage.loadTeam(teamName);
        } catch (Exception e) {
            System.out.println("Could not load team from Trainer HQ: " + e.getMessage());
        }
    }

    @Given("I setup pokemon team slots 5 and 6 with specific level pokemons")
    public void iSetupPokemonTeamSlots5And6() {
        com.pokemon.automation.pages.PokemonCenterPage centerPage = new com.pokemon.automation.pages.PokemonCenterPage(driver);
        centerPage.setupSpecificTeamSlots5And6();
    }

    @Given("I navigate to the map")
    public void iNavigateToTheMap() {
        System.out.println("Scraping available maps from the homescreen...");
        driver.get("https://pokemonbattlearena.net/");
        try { Thread.sleep(2000); } catch (Exception e) {}
        
        List<WebElement> mapLinks = driver.findElements(By.xpath("//a[contains(@href, 'wander.php?M=')]"));
        for (WebElement link : mapLinks) {
            String href = link.getAttribute("href");
            if (href != null && !availableMaps.contains(href)) {
                availableMaps.add(href);
            }
        }
        
        if (availableMaps.isEmpty()) {
            System.out.println("No maps found! Defaulting to map 112.");
            availableMaps.add("https://pokemonbattlearena.net/members/wander.php?M=112");
        } else {
            System.out.println("Found " + availableMaps.size() + " maps to hunt on!");
            // Shuffle maps randomly as requested
            java.util.Collections.shuffle(availableMaps);
            System.out.println("Maps shuffled randomly! Starting with map: " + availableMaps.get(0));
        }

        System.out.println("Healing at Pokemon Center before hunting...");
        driver.get("https://pokemonbattlearena.net/members/pokemoncenter.php");
        try { Thread.sleep(2000); } catch (Exception e) {}
        try {
            centerPage.healTeam();
        } catch (Exception e) {
            System.out.println("Could not heal team initially: " + e.getMessage());
        }

        System.out.println("Navigating to the Map.");
        if (!mapPage.safeNavigate(availableMaps.get(currentMapIndex))) {
            System.out.println("Failed to load map, skipping to next.");
            currentMapIndex++;
            if (currentMapIndex >= availableMaps.size()) currentMapIndex = 0;
            mapPage.safeNavigate(availableMaps.get(currentMapIndex));
        }
        try { Thread.sleep(2000); } catch (Exception e) {}
        mapPage.scrollToMapArea();
    }

    @When("I hunt on the map for {int} minutes")
    public void iHuntOnTheMapForMinutes(int minutes) {
        performHunting(minutes, null);
    }

    @When("I hunt on the map for special type {string} for {int} minutes")
    public void iHuntOnTheMapForSpecialTypeForMinutes(String specialTypes, int minutes) {
        performHunting(minutes, specialTypes);
    }

    private void performHunting(int minutes, String specialTypesFilter) {
        long endTime = System.currentTimeMillis() + (minutes * 60 * 1000L);
        int movementAttemptsWithoutNewPokemon = 0;
        
        while (System.currentTimeMillis() < endTime) {
            // Ad Tab Handling: Check for extraneous window tabs
            java.util.Set<String> windowHandles = driver.getWindowHandles();
            if (windowHandles.size() > 1) {
                String originalWindow = driver.getWindowHandle();
                for (String handle : windowHandles) {
                    if (!handle.equals(originalWindow)) {
                        driver.switchTo().window(handle);
                        driver.close();
                    }
                }
                driver.switchTo().window(originalWindow);
                try {
                    if (driver.getCurrentUrl().contains("map") || driver.getCurrentUrl().contains("wander")) {
                        ((org.openqa.selenium.JavascriptExecutor) driver).executeScript("window.scrollTo(0, document.body.scrollHeight * 0.45)");
                    }
                } catch(Exception e) {}
            }

            // Map URL Check: In case the current tab was hijacked or navigated away
            if (driver.getCurrentUrl().contains("login.php")) {
                System.out.println("Waiting 20 seconds before skipping to allow potential manual solve or session cooldown...");
                try { Thread.sleep(20000); } catch(Exception ex) {}
                System.out.println("⚠️ CLOUDFLARE CAPTCHA DETECTED! EXITING RUN! ⚠️");
                throw new RuntimeException("CLOUDFLARE CAPTCHA DETECTED");
            }
            if (!driver.getCurrentUrl().contains("wander") && !driver.getCurrentUrl().contains("map")) {
                System.out.println("Not on the map page! Currently at (" + driver.getCurrentUrl() + "). Navigating back to map.");
                mapPage.safeNavigate(availableMaps.get(currentMapIndex));
                try { Thread.sleep(2000); } catch(Exception e) {}
                mapPage.scrollToMapArea();
                continue; // Skip this iteration to prevent errors on the wrong page
            }

            mapPage.moveRandomly();
            try {
                Thread.sleep(2000); // Wait 2 seconds for response to avoid skipping pokemon
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            if (mapPage.isPokemonEncountered()) {
                try {
                    java.nio.file.Files.writeString(java.nio.file.Paths.get("target/map_dump.html"), driver.getPageSource());
                } catch (Exception e) {}
                
                boolean shouldBattle = true;
                
                boolean isSpecial = mapPage.isAnySpecialType();
                boolean isMapLegendary = mapPage.isLegendary();
                
                boolean isCaptured = mapPage.isPokemonAlreadyCaptured();
                if (isCaptured) {
                    if (isSpecial || isMapLegendary) {
                        System.out.println("Wild Pokemon is captured, BUT it is legendary/special! Proceeding to battle.");
                        shouldBattle = true;
                    } else {
                        System.out.println("Wild Pokemon on map is already captured. Skipping battle.");
                        shouldBattle = false;
                    }
                } else {
                    System.out.println("Wild Pokemon on map is NOT captured! Proceeding to battle.");
                    shouldBattle = true;
                }
                
                if (!shouldBattle) {
                    movementAttemptsWithoutNewPokemon++;
                } else {
                    movementAttemptsWithoutNewPokemon = 0; // Found a valid one! Reset counter.
                    System.out.println("Clicking battle, then checking for verification!");
                    mapPage.initiateBattle();
                    mapPage.handleBotCheckIfPresent();
                    
                    // Check if we need to select a monster first before entering the actual battle menu
                    battlePage.handleSelectMonsterScreen();

                    int enemyLevel = battlePage.getEnemyLevel();
                    String enemyName = battlePage.getEnemyName();
                    boolean isLegendary = battlePage.isLegendary(enemyName);
                    
                    boolean shouldCapture = true;
                    
                    if (shouldCapture) {
                        System.out.println("In battle! Enemy is " + enemyName + " (Level: " + enemyLevel + "). Attempting capture.");
                    }
                    
                    if (shouldCapture) {
                        battlePage.swapToLowestLevelPokemon();
                        try { Thread.sleep(1000); } catch (Exception e) {}
                    }
                    
                    boolean battleActive = true;
                    int loopCount = 0;
                    int failedActionCount = 0;
                    int pokeballAttemptCount = 0;
                    int greatballAttemptCount = 0;
                    int masterballAttemptCount = 0;
                    int repeatballAttemptCount = 0;
                    int timerballAttemptCount = 0;
                    int faintedPokemonCount = 0;
                    int previousEnemyHp = -1;
                    int zeroDamageCount = 0;
                    boolean lastActionWasAttack = false;
                    while(battleActive) {
                        loopCount++;
                        if (loopCount > 40) {
                            System.out.println("⚠️ BATTLE LOOP TIMEOUT! Breaking after 40 turns as a safety measure.");
                            battleActive = false;
                            break;
                        }
                        
                        boolean actionTaken = false;
                        
                        // Handle case where our pokemon faints mid-battle
                        if (battlePage.handleSelectMonsterScreen()) {
                            actionTaken = true;
                            faintedPokemonCount++;
                        }
                        
                        // Check if continue button is present (meaning battle ended)
                        if (battlePage.isContinuePresent()) {
                            battlePage.clickContinueIfPresent();
                            System.out.println("Battle ended! Clicked Continue.");
                            break;
                        }
                        
                        int enemyHp = battlePage.getEnemyHp();
                        if (enemyHp == 0) {
                            System.out.println("Enemy HP is 0. Battle is over!");
                            battleActive = false;
                            break;
                        }
                        
                        if (previousEnemyHp != -1 && enemyHp == previousEnemyHp && lastActionWasAttack) {
                            zeroDamageCount++;
                        } else {
                            zeroDamageCount = 0;
                        }
                        previousEnemyHp = enemyHp;
                        lastActionWasAttack = false;
                        
                        // Check pokeballs
                        int pbCount = battlePage.getPokeballCount();
                        if (pbCount != 999) {
                            System.out.println("Current Pokeballs: " + pbCount);
                            if (pbCount < 5) {
                                needsPokeballs = true;
                            }
                        } else {
                            System.out.println("Could not parse Pokeball count correctly (returned 999).");
                        }
                        
                        if (shouldCapture) {
                            if (isLegendary && !isCaptured) {
                                if (masterballAttemptCount < 1) {
                                    System.out.println("Legendary appeared! Using Masterball immediately!");
                                    battlePage.useMasterball();
                                    masterballAttemptCount++;
                                    actionTaken = true;
                                } else if (greatballAttemptCount < 2) {
                                    battlePage.useGreatball();
                                    greatballAttemptCount++;
                                    actionTaken = true;
                                } else if (pokeballAttemptCount < 4) {
                                    battlePage.usePokeball();
                                    pokeballAttemptCount++;
                                    actionTaken = true;
                                } else {
                                    System.out.println("Out of balls for Legendary! Have to attack...");
                                    if (zeroDamageCount > 0) {
                                        if (battlePage.selectAlternativeAttack(zeroDamageCount)) {
                                            actionTaken = true;
                                            lastActionWasAttack = true;
                                        }
                                    } else {
                                        if (battlePage.selectLowestPowerAttack()) {
                                            actionTaken = true;
                                            lastActionWasAttack = true;
                                        }
                                    }
                                }
                            } else if (isLegendary && isCaptured) {
                                if (enemyHp < 20 && enemyHp > 0) {
                                    if (greatballAttemptCount < 2) {
                                        battlePage.useGreatball();
                                        greatballAttemptCount++;
                                        actionTaken = true;
                                    } else if (repeatballAttemptCount < 5) {
                                        battlePage.useRepeatBall();
                                        repeatballAttemptCount++;
                                        actionTaken = true;
                                    } else if (timerballAttemptCount < 5) {
                                        battlePage.useTimerBall();
                                        timerballAttemptCount++;
                                        actionTaken = true;
                                    } else {
                                        System.out.println("Max balls thrown! Abandoning capture and attacking to finish it off.");
                                        if (zeroDamageCount > 0) {
                                            if (battlePage.selectAlternativeAttack(zeroDamageCount)) {
                                                actionTaken = true;
                                                lastActionWasAttack = true;
                                            }
                                        } else {
                                            if (battlePage.selectLowestPowerAttack()) {
                                                actionTaken = true;
                                                lastActionWasAttack = true;
                                            }
                                        }
                                    }
                                } else {
                                    // HP is >= 20, we need to weaken it
                                    if (zeroDamageCount > 0) {
                                        if (battlePage.selectAlternativeAttack(zeroDamageCount)) {
                                            actionTaken = true;
                                            lastActionWasAttack = true;
                                        }
                                    } else {
                                        if (battlePage.selectLowestPowerAttack()) {
                                            actionTaken = true;
                                            lastActionWasAttack = true;
                                        }
                                    }
                                }
                            } else {
                                if (enemyHp < 9 && enemyHp > 0) {
                                    if (pokeballAttemptCount < 2) {
                                        battlePage.usePokeball();
                                        pokeballAttemptCount++;
                                        actionTaken = true;
                                    } else if (greatballAttemptCount < 2) {
                                        battlePage.useGreatball();
                                        greatballAttemptCount++;
                                        actionTaken = true;
                                    } else if (repeatballAttemptCount < 1) {
                                        battlePage.useRepeatBall();
                                        repeatballAttemptCount++;
                                        actionTaken = true;
                                    } else if (timerballAttemptCount < 1) {
                                        battlePage.useTimerBall();
                                        timerballAttemptCount++;
                                        actionTaken = true;
                                    } else if (masterballAttemptCount < 1 && enemyLevel >= 60 && (!isLegendary || !isCaptured)) {
                                        battlePage.useMasterball();
                                        masterballAttemptCount++;
                                        actionTaken = true;
                                    } else {
                                        System.out.println("Max balls thrown! Abandoning capture and attacking to finish it off.");
                                        if (zeroDamageCount > 0) {
                                            if (battlePage.selectAlternativeAttack(zeroDamageCount)) {
                                                actionTaken = true;
                                                lastActionWasAttack = true;
                                            }
                                        } else {
                                            if (battlePage.selectLowestPowerAttack()) {
                                                actionTaken = true;
                                                lastActionWasAttack = true;
                                            }
                                        }
                                    }
                                } else {
                                    if (zeroDamageCount > 0) {
                                        if (battlePage.selectAlternativeAttack(zeroDamageCount)) {
                                            actionTaken = true;
                                            lastActionWasAttack = true;
                                        }
                                    } else {
                                        if (battlePage.selectLowestPowerAttack()) {
                                            actionTaken = true;
                                            lastActionWasAttack = true;
                                        }
                                    }
                                }
                            }
                        } else {
                            if (battlePage.selectHighestPowerAttack()) {
                                actionTaken = true;
                                lastActionWasAttack = true;
                            }
                        }
                        
                        if (!actionTaken && !battlePage.isContinuePresent()) {
                            failedActionCount++;
                            if (failedActionCount >= 3) {
                                System.out.println("No actions available for 3 consecutive loops. Assuming battle is over.");
                                break;
                            }
                        } else {
                            failedActionCount = 0;
                        }
                        
                        try { Thread.sleep(2500); } catch (Exception e) {}
                        
                        // Check if we got redirected to map
                        try {
                            if (driver.getCurrentUrl().contains("map") || driver.getCurrentUrl().contains("wander")) {
                                battleActive = false;
                            }
                        } catch (Exception e) {}
                    }
                    
                    battlePage.clickContinueIfPresent();
                    
                    // Mandatory post-battle heal
                    System.out.println("Healing at Pokemon Center...");
                    driver.get("https://pokemonbattlearena.net/members/pokemoncenter.php");
                    try { Thread.sleep(2000); } catch (Exception e) {}
                    try {
                        centerPage.healTeam();
                        // User request: Always replace pokemon with either special type or a legendary
                        centerPage.swapHighLevelForLegendaryOrSpecial(15);
                    } catch (Exception e) {
                        System.out.println("Could not heal team or check levels: " + e.getMessage());
                    }
                    
                    // Check if we need to buy Pokeballs
                    if (needsPokeballs) {
                        System.out.println("Pokeballs are running low! Going to Pokemart to buy 20 more...");
                        driver.get("https://pokemonbattlearena.net/members/itemshop.php");
                        try { Thread.sleep(2000); } catch (Exception e) {}
                        try {
                            // Find the row containing Pokeball, then its input field for amount
                            WebElement pbRow = driver.findElement(By.xpath("//*[contains(translate(text(), 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz'), 'pokeball')]//ancestor::tr | //*[contains(translate(text(), 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz'), 'poké ball')]//ancestor::tr"));
                            WebElement amountInput = pbRow.findElement(By.xpath(".//input[@type='text' or @type='number' or contains(@name, 'amount') or contains(@name, 'quantity')]"));
                            amountInput.clear();
                            amountInput.sendKeys("20");
                            WebElement buyBtn = pbRow.findElement(By.xpath(".//input[@type='submit' or @type='button'] | .//button"));
                            buyBtn.click();
                            System.out.println("Bought 20 Pokeballs.");
                            try { Thread.sleep(2000); } catch (Exception e) {}
                            
                            // Insufficient funds check
                            String pageSource = driver.getPageSource().toLowerCase();
                            if (pageSource.contains("not enough money") || pageSource.contains("insufficient") || pageSource.contains("don't have enough")) {
                                System.out.println("⚠️ Insufficient funds! Switching to Trainer Farming mode for the remainder of the session!");
                                
                                // Load the strong team
                                iLoadTheTeamUnderTrainerHQ("pokemon capture");
                                
                                // Start trainer farming loop
                                TrainerBattleSteps trainerSteps = new TrainerBattleSteps();
                                trainerSteps.iNavigateToTheTrainersList();
                                int remainingMinutes = (int) ((endTime - System.currentTimeMillis()) / 60000);
                                if (remainingMinutes > 0) {
                                    trainerSteps.iBattleAppropriateTrainersForMinutes(remainingMinutes);
                                }
                                System.out.println("Trainer Farming complete. Ending run as per time limit.");
                                return; // End wild hunting
                            }
                        } catch (Exception e) {
                            System.out.println("Could not purchase Pokeballs automatically: " + e.getMessage());
                        }
                        needsPokeballs = false;
                    }
                    
                    System.out.println("Returning to same map: " + availableMaps.get(currentMapIndex));
                    if (!mapPage.safeNavigate(availableMaps.get(currentMapIndex))) {
                        System.out.println("Failed to load map, skipping to next.");
                        currentMapIndex++;
                        if (currentMapIndex >= availableMaps.size()) currentMapIndex = 0;
                        mapPage.safeNavigate(availableMaps.get(currentMapIndex));
                    }
                    try { Thread.sleep(2000); } catch (Exception e) {}
                    mapPage.scrollToMapArea();
                }
            } else {
                movementAttemptsWithoutNewPokemon++;
            }
            
            // Check if we're stuck in a map with no new pokemon
            if (movementAttemptsWithoutNewPokemon >= 20) {
                System.out.println("No new pokemon found after 20 attempts! Moving to a different map.");
                movementAttemptsWithoutNewPokemon = 0; // Reset counter
                currentMapIndex++;
                if (currentMapIndex >= availableMaps.size()) {
                    currentMapIndex = 0;
                }
                if (!mapPage.safeNavigate(availableMaps.get(currentMapIndex))) {
                    System.out.println("Failed to load map, skipping to next.");
                    currentMapIndex++;
                    if (currentMapIndex >= availableMaps.size()) currentMapIndex = 0;
                    mapPage.safeNavigate(availableMaps.get(currentMapIndex));
                }
                try { Thread.sleep(2000); } catch (Exception e) {}
                mapPage.scrollToMapArea();
            }
        }
    }

    @Then("I should engage any wild Pokemon using the weakest attack available")
    public void iShouldEngageAnyWildPokemonUsingTheWeakestAttackAvailable() {
        System.out.println("Hunting cycle completed.");
    }
}
