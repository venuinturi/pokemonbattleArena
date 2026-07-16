package com.pokemon.automation.stepdefinitions;

import com.pokemon.automation.driver.DriverManager;
import com.pokemon.automation.pages.BattlePage;
import com.pokemon.automation.pages.MapNavigationPage;
import com.pokemon.automation.pages.PokemonCenterPage;
import com.pokemon.automation.pages.TrainerListPage;
import com.pokemon.automation.utils.TrainerStatsManager;
import io.cucumber.java.en.When;
import org.openqa.selenium.WebDriver;
import java.util.List;

public class FarmingBattleSteps {

    private WebDriver driver;
    private TrainerListPage trainerPage;
    private BattlePage battlePage;
    private PokemonCenterPage centerPage;
    private MapNavigationPage mapPage;

    public FarmingBattleSteps() {
        this.driver = DriverManager.getDriver();
        this.trainerPage = new TrainerListPage(driver);
        this.battlePage = new BattlePage(driver);
        this.centerPage = new PokemonCenterPage(driver);
        this.mapPage = new MapNavigationPage(driver);
    }

    @When("I identify and farm the highest paying trainer")
    public void iIdentifyAndFarmTheHighestPayingTrainer() {
        List<String> allCategories = trainerPage.getAllConquestCategoryValues();
        if (allCategories.isEmpty()) {
            System.out.println("No trainer categories found.");
            return;
        }

        int categoryIndex = 0;
        int trainerIndex = 0;

        while (true) {
            if (categoryIndex >= allCategories.size()) {
                categoryIndex = 0; // Wrap around to first category
            }
            
            String currentCategory = allCategories.get(categoryIndex);
            
            driver.get("https://pokemonbattlearena.net/members/trainers.php");
            try { Thread.sleep(1000); } catch (Exception e) {}
            mapPage.closeAdIfPresent();
            
            trainerPage.selectTrainerCategory(currentCategory);
            try { Thread.sleep(1500); } catch (Exception e) {}
            
            int totalTrainers = trainerPage.getTrainerCount();
            if (trainerIndex >= totalTrainers) {
                // Move to next category
                trainerIndex = 0;
                categoryIndex++;
                continue;
            }
            
            String currentTrainerName = trainerPage.battleSpecificTrainer(trainerIndex, 3);
            if (currentTrainerName == null) {
                trainerIndex++;
                continue; // Skip if we couldn't click
            }
            
            // We clicked battle! Now we execute the battle logic
            int wonMoney = executeBattleLogic();
            
            if (wonMoney > 0) {
                System.out.println(currentTrainerName + " defeated! Reward: $" + wonMoney);
                TrainerStatsManager.saveReward(currentTrainerName, wonMoney);
                trainerIndex++; // Move to next trainer
            } else {
                System.out.println("Lost battle against " + currentTrainerName + ". Retrying up to 3 times...");
                
                boolean wonRetry = false;
                for (int retry = 1; retry <= 3; retry++) {
                    System.out.println("--- Retry " + retry + " against " + currentTrainerName + " ---");
                    
                    driver.get("https://pokemonbattlearena.net/members/trainers.php");
                    try { Thread.sleep(1000); } catch (Exception e) {}
                    mapPage.closeAdIfPresent();
                    trainerPage.selectTrainerCategory(currentCategory);
                    try { Thread.sleep(1500); } catch (Exception e) {}
                    
                    if (trainerPage.battleSpecificTrainerByName(currentTrainerName, 3)) {
                        int retryMoney = executeBattleLogic();
                        if (retryMoney > 0) {
                            System.out.println("Won on retry " + retry + "! Reward: $" + retryMoney);
                            TrainerStatsManager.saveReward(currentTrainerName, retryMoney);
                            wonRetry = true;
                            break;
                        }
                    }
                }
                
                if (wonRetry) {
                    trainerIndex++; // Progress forward
                } else {
                    System.out.println("Failed to defeat " + currentTrainerName + " after 3 retries.");
                    fallbackAndFarmBestTrainer(allCategories);
                    // Do NOT increment trainerIndex, so we fight them again next loop
                }
            }
        }
    }
    
    private void fallbackAndFarmBestTrainer(List<String> allCategories) {
        java.util.Map<String, Integer> allStats = TrainerStatsManager.getAllStats();
        int maxMoney = -1;
        String bestTrainer = null;
        
        for (java.util.Map.Entry<String, Integer> entry : allStats.entrySet()) {
            if (entry.getValue() > maxMoney) {
                maxMoney = entry.getValue();
                bestTrainer = entry.getKey();
            }
        }
        
        if (bestTrainer == null) {
            System.out.println("No profitable trainers in history to fall back to! Exiting.");
            System.exit(0);
        }
        
        System.out.println("--- FALLING BACK --- Farming best trainer (" + bestTrainer + ") for 10 battles to level up.");
        
        for (int i = 0; i < 10; i++) {
            System.out.println("\nFallback Farming Battle " + (i + 1) + "/10 against " + bestTrainer);
            
            centerPage.healTeam();
            centerPage.setupTeamForBattleSession();
            
            driver.get("https://pokemonbattlearena.net/members/trainers.php");
            try { Thread.sleep(1000); } catch (Exception e) {}
            mapPage.closeAdIfPresent();
            
            boolean clicked = false;
            for (String cat : allCategories) {
                trainerPage.selectTrainerCategory(cat);
                try { Thread.sleep(1500); } catch (Exception e) {}
                
                if (trainerPage.battleSpecificTrainerByName(bestTrainer, 3)) {
                    clicked = true;
                    break;
                }
            }
            
            if (clicked) {
                int wonMoney = executeBattleLogic();
                if (wonMoney > 0) {
                    TrainerStatsManager.saveReward(bestTrainer, wonMoney);
                }
            } else {
                System.out.println("Could not find " + bestTrainer + " on the map during fallback!");
                break;
            }
            
            try { Thread.sleep(2000); } catch (Exception e) {}
        }
        
        System.out.println("Finished 10 fallback battles. Attempting to progress again...");
    }

    private int executeBattleLogic() {
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
                // Wait a few seconds for the results screen to load since the enemy fainted
                for (int w = 0; w < 5; w++) {
                    if (battlePage.isBattleComplete()) break;
                    try { Thread.sleep(1000); } catch (Exception e) {}
                }
                break;
            } else if (lastEnemyHp != -1 && currentEnemyHp == lastEnemyHp) {
                zeroDamageCount++;
            } else if (currentEnemyHp != lastEnemyHp) {
                zeroDamageCount = 0; 
                lastEnemyHp = currentEnemyHp;
            }
            
            if (battlePage.isContinuePresent()) {
                battlePage.clickContinueIfPresent();
                actionTaken = true;
                zeroDamageCount = 0; // reset
            } else if (zeroDamageCount >= 2) {
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
        
        int wonMoney = 0;
        // Check one last time with a small wait just in case
        for (int w = 0; w < 3; w++) {
            if (battlePage.isBattleComplete()) break;
            try { Thread.sleep(1000); } catch (Exception e) {}
        }
        
        if (battlePage.isBattleComplete() || loopCount >= 40 || failedActionCount >= 3) {
            wonMoney = battlePage.getWonPokeMoney();
            battlePage.clickContinueIfPresent();
        }
        
        return wonMoney;
    }
}
