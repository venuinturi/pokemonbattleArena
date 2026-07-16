package com.pokemon.automation.stepdefinitions;

import com.pokemon.automation.driver.DriverManager;
import com.pokemon.automation.pages.BattlePage;
import com.pokemon.automation.pages.MapNavigationPage;
import com.pokemon.automation.pages.PokemonCenterPage;
import com.pokemon.automation.pages.TrainerListPage;
import com.pokemon.automation.utils.TrainerStatsManager;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.When;
import io.cucumber.java.en.Then;
import org.openqa.selenium.WebDriver;
import java.util.List;

public class FarmingBattleSteps {

    private WebDriver driver;
    private TrainerListPage trainerPage;
    private BattlePage battlePage;
    private PokemonCenterPage centerPage;
    private MapNavigationPage mapPage;
    
    private String bestTrainerName = null;

    public FarmingBattleSteps() {
        this.driver = DriverManager.getDriver();
        this.trainerPage = new TrainerListPage(driver);
        this.battlePage = new BattlePage(driver);
        this.centerPage = new PokemonCenterPage(driver);
        this.mapPage = new MapNavigationPage(driver);
    }

    @When("I identify and farm the highest paying trainer")
    public void iIdentifyAndFarmTheHighestPayingTrainer() {
        boolean failedBattle = false;
        System.out.println("--- PHASE 1: IDENTIFICATION ---");
        List<String> allCategories = trainerPage.getAllConquestCategoryValues();
        
        for (String categoryValue : allCategories) {
            if (failedBattle) break;
            
            System.out.println("\nScanning Trainer Category: " + categoryValue);
            int trainerIndex = 0;
            int battleType = 3; // 3v3 battles
            
            while (!failedBattle) {
                System.out.println("Healing team and setting up for identification battle...");
                centerPage.healTeam();
                centerPage.setupTeamForBattleSession();
                
                driver.get("https://pokemonbattlearena.net/members/trainers.php");
                try { Thread.sleep(1000); } catch (Exception e) {}
                mapPage.closeAdIfPresent();
                
                trainerPage.selectTrainerCategory(categoryValue);
                try { Thread.sleep(1500); } catch (Exception e) {}
                
                int totalTrainers = trainerPage.getTrainerCount();
                if (trainerIndex >= totalTrainers) {
                    System.out.println("Finished all trainers in category " + categoryValue);
                    break; // Move to next category
                }
                
                String currentTrainerName = trainerPage.battleSpecificTrainer(trainerIndex, battleType);
                if (currentTrainerName != null) {
                    System.out.println("Checking for bot verification...");
                    mapPage.handleBotCheckIfPresent();
                    battlePage.resetBattleState();
                    
                    System.out.println("Now battling: " + currentTrainerName);
                    
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
                            break;
                        }
                        
                        int currentEnemyHp = battlePage.getEnemyHp();
                        if (currentEnemyHp == 0) {
                            break;
                        } else if (lastEnemyHp != -1 && currentEnemyHp == lastEnemyHp) {
                            zeroDamageCount++;
                        } else if (currentEnemyHp != lastEnemyHp) {
                            zeroDamageCount = 0; 
                            lastEnemyHp = currentEnemyHp;
                        }
                        
                        if (zeroDamageCount >= 2) {
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
                                break;
                            }
                        } else {
                            failedActionCount = 0;
                        }
                        
                        try { Thread.sleep(2500); } catch (Exception e) {}
                    }
                    
                    if (battlePage.isBattleComplete() || loopCount >= 40 || failedActionCount >= 3) {
                        int wonMoney = battlePage.getWonPokeMoney();
                        if (wonMoney > 0) {
                            System.out.println(currentTrainerName + " defeated! Reward: $" + wonMoney);
                            TrainerStatsManager.saveReward(currentTrainerName, wonMoney);
                            battlePage.clickContinueIfPresent();
                        } else {
                            System.out.println("Failed to win battle against " + currentTrainerName + " (Reward: $0). Stopping identification phase.");
                            failedBattle = true;
                            battlePage.clickContinueIfPresent();
                        }
                    }
                    trainerIndex++;
                } else {
                    trainerIndex++;
                }
            }
        }
        
        System.out.println("\n--- PHASE 2: FARMING ---");
        java.util.Map<String, Integer> allStats = TrainerStatsManager.getAllStats();
        int maxMoney = -1;
        String bestTrainer = null;
        
        for (java.util.Map.Entry<String, Integer> entry : allStats.entrySet()) {
            if (entry.getValue() > maxMoney) {
                maxMoney = entry.getValue();
                bestTrainer = entry.getKey();
            }
        }
        
        bestTrainerName = bestTrainer;
        if (bestTrainerName == null) {
            System.out.println("No profitable trainers found in history. Exiting.");
            return;
        }
        
        System.out.println("Highest paying trainer identified: " + bestTrainerName + " with reward $" + maxMoney);
        int totalBattles = 0;
        
        while (true) {
            System.out.println("\n--- Starting Farming Battle #" + (totalBattles + 1) + " against " + bestTrainerName + " ---");
            
            centerPage.healTeam();
            centerPage.setupTeamForBattleSession();
            
            driver.get("https://pokemonbattlearena.net/members/trainers.php");
            try { Thread.sleep(1000); } catch (Exception e) {}
            mapPage.closeAdIfPresent();
            
            boolean clicked = false;
            for (String cat : allCategories) {
                trainerPage.selectTrainerCategory(cat);
                try { Thread.sleep(1500); } catch (Exception e) {}
                
                clicked = trainerPage.battleSpecificTrainerByName(bestTrainerName, 3);
                if (clicked) {
                    break;
                }
            }
            
            if (!clicked) {
                System.out.println("Could not click battle for " + bestTrainerName + ". They might be missing from this page.");
                break;
            }
            
            mapPage.handleBotCheckIfPresent();
            battlePage.resetBattleState();
            
            int loopCount = 0;
            int failedActionCount = 0;
            int lastEnemyHp = -1;
            int zeroDamageCount = 0;
            
            while (loopCount < 40) {
                loopCount++;
                boolean actionTaken = false;
                
                if (battlePage.handleSelectMonsterScreen(3)) {
                    actionTaken = true;
                }
                
                if (battlePage.isBattleComplete()) {
                    break;
                }
                
                int currentEnemyHp = battlePage.getEnemyHp();
                if (currentEnemyHp == 0) {
                    break;
                } else if (lastEnemyHp != -1 && currentEnemyHp == lastEnemyHp) {
                    zeroDamageCount++;
                } else if (currentEnemyHp != lastEnemyHp) {
                    zeroDamageCount = 0; 
                    lastEnemyHp = currentEnemyHp;
                }
                
                if (zeroDamageCount >= 2) {
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
                    TrainerStatsManager.saveReward(bestTrainerName, wonMoney);
                }
                battlePage.clickContinueIfPresent();
            }
            
            totalBattles++;
            if (totalBattles % 20 == 0) {
                centerPage.configureAllActiveTeamAttacks();
            }
            
            try { Thread.sleep(3000); } catch (Exception e) {}
        }
    }
}
