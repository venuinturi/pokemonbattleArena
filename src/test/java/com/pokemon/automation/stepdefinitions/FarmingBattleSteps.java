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

    @Given("I determine the most profitable trainer on the page")
    public void iDetermineTheMostProfitableTrainerOnThePage() {
        System.out.println("Determining the best trainer to farm...");
        List<String> trainers = trainerPage.getAllTrainersOnPage();
        bestTrainerName = TrainerStatsManager.getBestTrainer(trainers);
        
        if (bestTrainerName != null) {
            System.out.println("Best trainer found: " + bestTrainerName + " with reward $" + TrainerStatsManager.getReward(bestTrainerName));
        } else {
            System.out.println("No known profitable trainers on this page. Defaulting to the first available.");
            if (!trainers.isEmpty()) {
                bestTrainerName = trainers.get(0);
            }
        }
    }

    @When("I farm the most profitable trainer in an infinite loop")
    public void iFarmTheMostProfitableTrainerInAnInfiniteLoop() {
        if (bestTrainerName == null) {
            System.out.println("No trainer selected for farming. Exiting loop.");
            return;
        }

        int totalBattles = 0;
        
        while (true) {
            System.out.println("\n--- Starting Farming Battle #" + (totalBattles + 1) + " against " + bestTrainerName + " ---");
            
            // 1. Heal and Check Team Setup
            System.out.println("Healing team and setting up for battle...");
            centerPage.healTeam();
            centerPage.setupTeamForBattleSession();
            
            // 2. Navigate back to Trainers
            driver.get("https://pokemonbattlearena.net/members/trainers.php");
            try { Thread.sleep(1000); } catch (Exception e) {}
            mapPage.closeAdIfPresent();
            
            // 3. Battle the chosen trainer (defaults to 1v1 for fastest farming)
            boolean clicked = trainerPage.battleSpecificTrainerByName(bestTrainerName, 1);
            if (!clicked) {
                System.out.println("Could not click battle for " + bestTrainerName + ". They might be missing from this page.");
                break; // Exit loop if trainer can't be found
            }
            
            System.out.println("Checking for bot verification after clicking trainer battle...");
            mapPage.handleBotCheckIfPresent();
            battlePage.resetBattleState();
            
            System.out.println("Now battling enemy Pokemon: " + battlePage.getEnemyName());
            
            int loopCount = 0;
            int failedActionCount = 0;
            int lastEnemyHp = -1;
            int zeroDamageCount = 0;
            
            // 4. Battle Logic
            while (loopCount < 40) {
                loopCount++;
                boolean actionTaken = false;
                
                if (battlePage.handleSelectMonsterScreen(1)) {
                    actionTaken = true;
                }
                
                if (battlePage.isBattleComplete()) {
                    int wonMoney = battlePage.getWonPokeMoney();
                    if (wonMoney > 0) {
                        TrainerStatsManager.saveReward(bestTrainerName, wonMoney);
                    }
                    battlePage.clickContinueIfPresent();
                    System.out.println("Farming Battle ended! Clicked Continue.");
                    break;
                }
                
                int currentEnemyHp = battlePage.getEnemyHp();
                if (currentEnemyHp == 0) {
                    if (battlePage.isBattleComplete()) {
                        int wonMoney = battlePage.getWonPokeMoney();
                        if (wonMoney > 0) {
                            TrainerStatsManager.saveReward(bestTrainerName, wonMoney);
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
            
            // Final check
            if (battlePage.isBattleComplete()) {
                int wonMoney = battlePage.getWonPokeMoney();
                if (wonMoney > 0) {
                    TrainerStatsManager.saveReward(bestTrainerName, wonMoney);
                }
                battlePage.clickContinueIfPresent();
            }
            
            totalBattles++;
            if (totalBattles % 20 == 0) {
                System.out.println(totalBattles + " battles completed! Refreshing all attacks for active team...");
                centerPage.configureAllActiveTeamAttacks();
            }
            
            try { Thread.sleep(3000); } catch (Exception e) {}
        }
    }
}
