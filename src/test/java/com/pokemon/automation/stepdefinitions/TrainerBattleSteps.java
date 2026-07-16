package com.pokemon.automation.stepdefinitions;

import com.pokemon.automation.driver.DriverManager;
import com.pokemon.automation.pages.BattlePage;
import com.pokemon.automation.pages.MapNavigationPage;
import com.pokemon.automation.pages.TrainerListPage;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.openqa.selenium.WebDriver;

public class TrainerBattleSteps {

    private WebDriver driver = DriverManager.getDriver();
    private TrainerListPage trainerPage = new TrainerListPage(driver);
    private BattlePage battlePage = new BattlePage(driver);
    private int assumedTeamLevel = 15; // Hardcoded default for the script template
    private String currentCategoryValue = null;

    @Given("I navigate to the trainers list")
    public void iNavigateToTheTrainersList() {
        System.out.println("Navigating to Trainers List...");
        driver.get("https://pokemonbattlearena.net/members/trainers.php");
        try { Thread.sleep(2000); } catch(Exception e) {}
        
        if (currentCategoryValue != null) {
            System.out.println("Applying Trainer Category: " + currentCategoryValue);
            trainerPage.selectTrainerCategory(currentCategoryValue);
        }
    }

    @When("I battle appropriate trainers for {int} minutes")
    public void iBattleAppropriateTrainersForMinutes(int minutes) {
        System.out.println("--- Capturing Initial Team Levels ---");
        String initialLevels = battlePage.getTeamLevels();
        System.out.println("Initial Levels: " + initialLevels);

        long endTime = System.currentTimeMillis() + (minutes * 60 * 1000L);

        MapNavigationPage mapPage = new MapNavigationPage(driver);
        com.pokemon.automation.pages.PokemonCenterPage centerPage = new com.pokemon.automation.pages.PokemonCenterPage(driver);
        
        int trainerIndex = 0;
        int battleType = 1;
        
        int loginFailCount = 0;
        int totalBattlesFought = 0;
        
        while (System.currentTimeMillis() < endTime) {
            if (driver.getCurrentUrl().contains("login.php")) {
                if (loginFailCount == 0) {
                    System.out.println("Session expired or redirected to login! Re-logging in...");
                    com.pokemon.automation.pages.LoginPage loginPage = new com.pokemon.automation.pages.LoginPage(driver);
                    loginPage.login(com.pokemon.automation.config.ConfigReader.getProperty("email"), com.pokemon.automation.config.ConfigReader.getProperty("password"));
                    try { Thread.sleep(5000); } catch(Exception e) {}
                }
                
                if (driver.getCurrentUrl().contains("login.php")) {
                    System.out.println("Waiting 20 seconds before skipping to allow potential manual solve or session cooldown...");
                    try { Thread.sleep(20000); } catch(Exception ex) {}
                    System.out.println("⚠️ CLOUDFLARE CAPTCHA DETECTED! EXITING RUN! ⚠️");
                    throw new RuntimeException("CLOUDFLARE CAPTCHA DETECTED");
                } else {
                    loginFailCount = 0; // Reset on success
                }
            }

            System.out.println("Healing team and setting up for battle...");
            centerPage.healTeam();
            centerPage.setupTeamForBattleSession();
            
            iNavigateToTheTrainersList();
            
            mapPage.closeAdIfPresent();
            
            int totalTrainers = trainerPage.getTrainerCount();
            if (trainerIndex >= totalTrainers) {
                if (totalTrainers == 0) {
                    System.out.println("No trainers found in this category.");
                } else {
                    System.out.println("Finished all trainers up to 3v3 options in current category!");
                }
                
                String nextCategory = trainerPage.getNextTrainerCategoryValue(currentCategoryValue);
                if (nextCategory != null) {
                    currentCategoryValue = nextCategory;
                    System.out.println("Moving to next category: " + currentCategoryValue);
                } else {
                    System.out.println("Could not determine next category. Resetting to default.");
                    currentCategoryValue = null;
                }
                
                trainerIndex = 0;
                battleType = 1;
                continue; // Navigates to list and applies new category
            }
            
            String currentTrainerName = trainerPage.battleSpecificTrainer(trainerIndex, battleType);
            
            if (currentTrainerName != null) {
                System.out.println("Checking for bot verification after clicking trainer battle...");
                mapPage.handleBotCheckIfPresent();
                battlePage.resetBattleState();
                
                int loopCount = 0;
                int failedActionCount = 0;
                int lastEnemyHp = -1;
                int zeroDamageCount = 0;
                
                while (loopCount < 40) {
                    loopCount++;
                    boolean actionTaken = false;
                    
                    if (battlePage.handleSelectMonsterScreen(battleType)) {
                        actionTaken = true;
                    }
                    
                    if (battlePage.isContinuePresent()) {
                        int wonMoney = battlePage.getWonPokeMoney();
                        if (wonMoney > 0) {
                            com.pokemon.automation.utils.TrainerStatsManager.saveReward(currentTrainerName, wonMoney);
                        }
                        battlePage.clickContinueIfPresent();
                        System.out.println("Trainer Battle ended! Clicked Continue.");
                        break;
                    }
                    
                    int currentEnemyHp = battlePage.getEnemyHp();
                    if (currentEnemyHp == 0) {
                        System.out.println("Enemy Trainer Pokemon fainted!");
                        zeroDamageCount = 0;
                    } else if (lastEnemyHp != -1 && currentEnemyHp == lastEnemyHp) {
                        zeroDamageCount++;
                    } else if (currentEnemyHp != lastEnemyHp) {
                        zeroDamageCount = 0; 
                        lastEnemyHp = currentEnemyHp;
                    }
                    
                    if (zeroDamageCount >= 2) {
                        System.out.println("Potential 0 damage detected. Using alternative attack...");
                        if (battlePage.selectAlternativeAttack(zeroDamageCount)) {
                            actionTaken = true;
                        }
                    } else {
                        if (battlePage.selectHighestPowerAttack()) {
                            actionTaken = true;
                        }
                    }
                    
                    if (!actionTaken && !battlePage.isContinuePresent()) {
                        failedActionCount++;
                        if (failedActionCount >= 3) {
                            System.out.println("No attacks available for 3 consecutive loops. Assuming battle is over.");
                            break;
                        }
                    } else {
                        failedActionCount = 0;
                    }
                    
                    try { Thread.sleep(2500); } catch (Exception e) {}
                }
                
                // Extra check if loop ended right on battle completion
                if (battlePage.isContinuePresent()) {
                    int wonMoney = battlePage.getWonPokeMoney();
                    if (wonMoney > 0) {
                        com.pokemon.automation.utils.TrainerStatsManager.saveReward(currentTrainerName, wonMoney);
                    }
                    battlePage.clickContinueIfPresent();
                }
                // Wait briefly before next battle (user requested 5 seconds timeout)
                try { Thread.sleep(5000); } catch (InterruptedException e) {}
                
                totalBattlesFought++;
                if (totalBattlesFought % 20 == 0) {
                    System.out.println("20 battles completed! Refreshing all attacks for active team...");
                    centerPage.configureAllActiveTeamAttacks();
                }
            }
            
            battleType++;
            if (battleType > 3) {
                battleType = 1;
                trainerIndex++;
            }
        }
        
        System.out.println("Trainer battle script completed its " + minutes + "-minute run.");
        System.out.println("--- Capturing Final Team Levels ---");
        String finalLevels = battlePage.getTeamLevels();
        System.out.println("Final Levels: " + finalLevels);
    }

    @When("I battle all gym leaders from the dropdown")
    public void iBattleAllGymLeaders() {
        System.out.println("Gathering all Gym Leader categories...");
        java.util.List<String> gymLeaders = trainerPage.getAllGymLeaderCategoryValues();
        
        if (gymLeaders.isEmpty()) {
            System.out.println("No Gym Leader categories found in the dropdown!");
            return;
        }

        MapNavigationPage mapPage = new MapNavigationPage(driver);
        com.pokemon.automation.pages.PokemonCenterPage centerPage = new com.pokemon.automation.pages.PokemonCenterPage(driver);
        
        for (String categoryValue : gymLeaders) {
            System.out.println("\n--- Starting Gym Leader Category: " + categoryValue + " ---");
            currentCategoryValue = categoryValue;
            int trainerIndex = 0;
            int battleType = 1;
            
            while (true) {
                System.out.println("Healing team and setting up for battle...");
                centerPage.healTeam();
                centerPage.setupTeamForBattleSession();
                
                iNavigateToTheTrainersList();
                mapPage.closeAdIfPresent();
                
                int totalTrainers = trainerPage.getTrainerCount();
                if (trainerIndex >= totalTrainers) {
                    System.out.println("Finished all trainers in Gym Leader category " + categoryValue);
                    break; // Move to next category
                }
                
                String currentTrainerName = trainerPage.battleSpecificTrainer(trainerIndex, battleType);
                
                if (currentTrainerName != null) {
                    System.out.println("Checking for bot verification after clicking trainer battle...");
                    mapPage.handleBotCheckIfPresent();
                    battlePage.resetBattleState();
                    
                    String enemyPoke = battlePage.getEnemyName();
                    System.out.println("Now battling enemy Pokemon: " + enemyPoke);
                    
                    int loopCount = 0;
                    int failedActionCount = 0;
                    int lastEnemyHp = -1;
                    int zeroDamageCount = 0;
                    
                    while (loopCount < 40) {
                        loopCount++;
                        boolean actionTaken = false;
                        
                        if (battlePage.handleSelectMonsterScreen(battleType)) {
                            actionTaken = true;
                        }
                        
                        if (battlePage.isBattleComplete()) {
                            int wonMoney = battlePage.getWonPokeMoney();
                            if (wonMoney > 0) {
                                com.pokemon.automation.utils.TrainerStatsManager.saveReward(currentTrainerName, wonMoney);
                            }
                            battlePage.clickContinueIfPresent();
                            System.out.println("Trainer Battle ended! Clicked Continue.");
                            break;
                        }
                        
                        int currentEnemyHp = battlePage.getEnemyHp();
                        if (currentEnemyHp == 0) {
                            if (battlePage.isBattleComplete()) {
                                int wonMoney = battlePage.getWonPokeMoney();
                                if (wonMoney > 0) {
                                    com.pokemon.automation.utils.TrainerStatsManager.saveReward(currentTrainerName, wonMoney);
                                }
                                battlePage.clickContinueIfPresent();
                            }
                            break;
                        } else if (lastEnemyHp != -1 && currentEnemyHp == lastEnemyHp) {
                            zeroDamageCount++;
                        } else if (currentEnemyHp != lastEnemyHp) {
                            zeroDamageCount = 0; 
                            lastEnemyHp = currentEnemyHp;
                        }
                        
                        if (zeroDamageCount >= 2) {
                            System.out.println("Potential 0 damage detected. Using alternative attack...");
                            if (battlePage.selectAlternativeAttack(zeroDamageCount)) {
                                actionTaken = true;
                            }
                        } else {
                            if (battlePage.selectHighestPowerAttack()) {
                                actionTaken = true;
                            }
                        }
                        
                        if (!actionTaken && !battlePage.isBattleComplete()) {
                            failedActionCount++;
                            if (failedActionCount >= 3) {
                                System.out.println("No attacks available for 3 consecutive loops. Assuming battle is over.");
                                break;
                            }
                        } else {
                            failedActionCount = 0;
                        }
                        
                        try { Thread.sleep(2500); } catch (Exception e) {}
                    }
                    
                    // Final check just in case it ended exactly at the end of the loop
                    if (battlePage.isBattleComplete()) {
                        int wonMoney = battlePage.getWonPokeMoney();
                        if (wonMoney > 0) {
                            com.pokemon.automation.utils.TrainerStatsManager.saveReward(currentTrainerName, wonMoney);
                        }
                        battlePage.clickContinueIfPresent();
                    }
                    
                    battleType++;
                    if (battleType > 3) {
                        battleType = 1;
                        trainerIndex++;
                    }
                } else {
                    System.out.println("Could not click battle button for trainer index " + trainerIndex + ", battle type " + battleType + ". Trying next.");
                    battleType++;
                    if (battleType > 3) {
                        battleType = 1;
                        trainerIndex++;
                    }
                }
            }
        }
        System.out.println("Finished battling all Gym Leaders!");
    }

    @When("I battle all Conquest trainers from the dropdown")
    public void iBattleAllConquestTrainers() {
        System.out.println("Gathering all Conquest categories...");
        java.util.List<String> conquestCats = trainerPage.getAllConquestCategoryValues();
        
        if (conquestCats.isEmpty()) {
            System.out.println("No Conquest categories found in the dropdown!");
            return;
        }

        MapNavigationPage mapPage = new MapNavigationPage(driver);
        com.pokemon.automation.pages.PokemonCenterPage centerPage = new com.pokemon.automation.pages.PokemonCenterPage(driver);
        
        for (String categoryValue : conquestCats) {
            System.out.println("\n--- Starting Conquest Category: " + categoryValue + " ---");
            currentCategoryValue = categoryValue;
            int trainerIndex = 0;
            int battleType = 1; // Default to 1v1 for fastest identification
            
            while (true) {
                System.out.println("Healing team and setting up for battle...");
                centerPage.healTeam();
                centerPage.setupTeamForBattleSession();
                
                iNavigateToTheTrainersList();
                mapPage.closeAdIfPresent();
                
                trainerPage.selectTrainerCategory(categoryValue);
                try { Thread.sleep(1500); } catch (Exception e) {}
                
                int totalTrainers = trainerPage.getTrainerCount();
                if (trainerIndex >= totalTrainers) {
                    System.out.println("Finished all trainers in Conquest category " + categoryValue);
                    break;
                }
                
                String currentTrainerName = trainerPage.battleSpecificTrainer(trainerIndex, battleType);
                
                if (currentTrainerName != null) {
                    System.out.println("Checking for bot verification after clicking trainer battle...");
                    mapPage.handleBotCheckIfPresent();
                    battlePage.resetBattleState();
                    
                    String enemyPoke = battlePage.getEnemyName();
                    System.out.println("Now battling enemy Pokemon: " + enemyPoke);
                    
                    int loopCount = 0;
                    int failedActionCount = 0;
                    int lastEnemyHp = -1;
                    int zeroDamageCount = 0;
                    
                    while (loopCount < 40) {
                        loopCount++;
                        boolean actionTaken = false;
                        
                        if (battlePage.handleSelectMonsterScreen(battleType)) {
                            actionTaken = true;
                        }
                        
                        if (battlePage.isBattleComplete()) {
                            int wonMoney = battlePage.getWonPokeMoney();
                            if (wonMoney > 0) {
                                com.pokemon.automation.utils.TrainerStatsManager.saveReward(currentTrainerName, wonMoney);
                            }
                            battlePage.clickContinueIfPresent();
                            System.out.println("Trainer Battle ended! Clicked Continue.");
                            break;
                        }
                        
                        int currentEnemyHp = battlePage.getEnemyHp();
                        if (currentEnemyHp == 0) {
                            if (battlePage.isBattleComplete()) {
                                int wonMoney = battlePage.getWonPokeMoney();
                                if (wonMoney > 0) {
                                    com.pokemon.automation.utils.TrainerStatsManager.saveReward(currentTrainerName, wonMoney);
                                }
                                battlePage.clickContinueIfPresent();
                            }
                            break;
                        } else if (lastEnemyHp != -1 && currentEnemyHp == lastEnemyHp) {
                            zeroDamageCount++;
                        } else if (currentEnemyHp != lastEnemyHp) {
                            zeroDamageCount = 0; 
                            lastEnemyHp = currentEnemyHp;
                        }
                        
                        if (zeroDamageCount >= 2) {
                            System.out.println("Potential 0 damage detected. Using alternative attack...");
                            if (battlePage.selectAlternativeAttack(zeroDamageCount)) {
                                actionTaken = true;
                            }
                        } else {
                            if (battlePage.selectHighestPowerAttack()) {
                                actionTaken = true;
                            }
                        }
                        
                        if (!actionTaken && !battlePage.isBattleComplete()) {
                            failedActionCount++;
                            if (failedActionCount >= 3) {
                                System.out.println("No attacks available for 3 consecutive loops. Assuming battle is over.");
                                break;
                            }
                        } else {
                            failedActionCount = 0;
                        }
                        
                        try { Thread.sleep(2500); } catch (Exception e) {}
                    }
                    
                    if (battlePage.isBattleComplete()) {
                        int wonMoney = battlePage.getWonPokeMoney();
                        if (wonMoney > 0) {
                            com.pokemon.automation.utils.TrainerStatsManager.saveReward(currentTrainerName, wonMoney);
                        }
                        battlePage.clickContinueIfPresent();
                    }
                    
                    trainerIndex++;
                } else {
                    System.out.println("Could not click battle button for trainer index " + trainerIndex + ". Skipping.");
                    trainerIndex++;
                }
            }
        }
        System.out.println("Finished battling all Conquest Trainers!");
    }
}
