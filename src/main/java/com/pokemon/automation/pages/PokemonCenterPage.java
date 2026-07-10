package com.pokemon.automation.pages;

import com.pokemon.automation.base.BasePage;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PokemonCenterPage extends BasePage {

    public PokemonCenterPage(WebDriver driver) {
        super(driver);
    }

    public void healTeam() {
        try {
            if (!driver.getCurrentUrl().contains("pokemoncenter.php")) {
                driver.get("https://pokemonbattlearena.net/members/pokemoncenter.php");
            }
            try {
                org.openqa.selenium.support.ui.WebDriverWait shortWait = new org.openqa.selenium.support.ui.WebDriverWait(driver, java.time.Duration.ofSeconds(2));
                WebElement healBtn = shortWait.until(org.openqa.selenium.support.ui.ExpectedConditions.elementToBeClickable(By.id("btnHeal")));
                new org.openqa.selenium.interactions.Actions(driver).moveToElement(healBtn).click().perform();
                System.out.println("Team healed.");
            } catch (Exception waitEx) {
                System.out.println("Heal button not clickable or not found within 2 seconds. Team is likely healed.");
            }
        } catch (Exception e) {
            System.out.println("Failed to heal team: " + e.getMessage());
        }
    }

    public void swapHighLevelForLowLevel(int maxLevelThreshold, int minTargetLevel, int maxTargetLevel) {
        if (!driver.getCurrentUrl().contains("pokemoncenter.php")) {
            driver.get("https://pokemonbattlearena.net/members/pokemoncenter.php");
            try { Thread.sleep(1000); } catch (Exception e) {}
        }

        // Check if any team member is >= maxLevelThreshold
        System.out.println("Checking team for Pokemon >= Level " + maxLevelThreshold + "...");
        int highLevelIndex = -1;
        try {
            // Options in ddParty are like: <option value="1">[1] Crawdaunt</option>
            // We need to parse the HTML tables above it to see the level
            List<WebElement> teamTables = driver.findElements(By.xpath("//table[contains(@class, 'table-striped')]//tr/td[contains(., 'lvl')]/sup | //table[contains(@class, 'table-striped')]//tr/td[contains(text(), 'lvl')]/sup"));
            if (teamTables.isEmpty()) {
                teamTables = driver.findElements(By.xpath("//td[contains(., 'lvl')]/sup"));
            }
            int numTeamMembers = teamTables.size();
            for (int i = 0; i < numTeamMembers; i++) {
                int slotIndex = i + 1; // 1-indexed for ddParty
                
                if (maxLevelThreshold == 11 && (slotIndex == 5 || slotIndex == 6)) {
                    continue;
                }
                
                // Re-fetch to avoid stale element reference
                List<WebElement> currentTeamTables = driver.findElements(By.xpath("//table[contains(@class, 'table-striped')]//tr/td[contains(., 'lvl')]/sup | //table[contains(@class, 'table-striped')]//tr/td[contains(text(), 'lvl')]/sup"));
                if (currentTeamTables.isEmpty()) {
                    currentTeamTables = driver.findElements(By.xpath("//td[contains(., 'lvl')]/sup"));
                }
                if (i >= currentTeamTables.size()) continue;

                String lvlStr = currentTeamTables.get(i).getText().replaceAll("[^0-9]", "");
                if (!lvlStr.isEmpty() && Integer.parseInt(lvlStr) >= maxLevelThreshold) {
                    highLevelIndex = slotIndex;
                    break;
                }
            }
        } catch (Exception e) {
            System.out.println("Error parsing team levels: " + e.getMessage());
        }

        if (highLevelIndex != -1) {
            System.out.println("Found Pokemon >= Level " + maxLevelThreshold + " in slot " + highLevelIndex + "!");
            // Parse PC box (ddMon)
            String replacementMonValue = null;
            try {
                List<WebElement> pcOptions = (List<WebElement>) ((JavascriptExecutor) driver).executeScript("return document.getElementById('ddMon').options;");
                for (WebElement option : pcOptions) {
                    String text = option.getAttribute("textContent");
                    String value = option.getAttribute("value");
                    if (value == null || value.trim().isEmpty() || text == null || text.contains("Empty Slot")) continue;
                    
                    // Extract any 1-3 digit number from the text.
                    Matcher m = Pattern.compile("\\b(\\d{1,3})\\b").matcher(text);
                    while (m.find()) {
                        int val = Integer.parseInt(m.group(1));
                        // User request: pokemon available from the list with lvl >5 and less than 10
                        if (val > minTargetLevel && val < maxTargetLevel) {
                            replacementMonValue = value;
                            break; // found one!
                        }
                    }
                    if (replacementMonValue != null) break;
                }
            } catch (Exception e) {
                System.out.println("Error parsing PC Pokemon: " + e.getMessage());
            }

            if (replacementMonValue != null) {
                System.out.println("Swapping slot " + highLevelIndex + " with PC Pokemon.");
                try {
                    ((JavascriptExecutor) driver).executeScript("document.getElementById('ddParty').value = arguments[0];", String.valueOf(highLevelIndex));
                    ((JavascriptExecutor) driver).executeScript("document.getElementById('ddMon').value = arguments[0];", replacementMonValue);
                    ((JavascriptExecutor) driver).executeScript("document.getElementById('frmSwap').submit();");
                    Thread.sleep(3000); // Wait for swap to complete
                    System.out.println("Swap executed successfully.");
                    
                    // User Request: Configure the attacks of the newly swapped Pokemon
                    configureNewPokemonAttacks(replacementMonValue);
                    
                } catch (Exception e) {
                    System.out.println("Failed to execute swap: " + e.getMessage());
                }
            } else {
                System.out.println("No suitable lower level Pokemon found in PC to swap with.");
                System.out.println("--- DEBUG: Printing first 10 PC Box options ---");
                try {
                    List<WebElement> pcOptions = (List<WebElement>) ((JavascriptExecutor) driver).executeScript("return document.getElementById('ddMon').options;");
                    for (int i = 0; i < Math.min(pcOptions.size(), 10); i++) {
                        System.out.println("Option " + i + ": '" + pcOptions.get(i).getAttribute("textContent") + "'");
                    }
                } catch (Exception e) {}
                System.out.println("-------------------------------------------------");
            }
        } else {
            System.out.println("No Pokemon >= Level " + maxLevelThreshold + " found in active team. Continuing.");
        }
    }
    
    public void setupSpecificTeamSlots5And6() {
        if (!driver.getCurrentUrl().contains("pokemoncenter.php")) {
            driver.get("https://pokemonbattlearena.net/members/pokemoncenter.php");
            try { Thread.sleep(1000); } catch (Exception e) {}
        }
        
        System.out.println("Setting up team slots 5 and 6 according to specific rules...");
        
        // Slot 5: level between 20-30, whichever is the lowest
        try {
            List<WebElement> pcOptions = (List<WebElement>) ((JavascriptExecutor) driver).executeScript("return document.getElementById('ddMon').options;");
            String slot5ReplacementValue = null;
            int lowestLevel = 999;
            
            for (WebElement option : pcOptions) {
                String text = option.getAttribute("textContent");
                String value = option.getAttribute("value");
                if (value == null || value.trim().isEmpty() || text == null || text.contains("Empty Slot")) continue;
                
                Matcher m = Pattern.compile("\\b(\\d{1,3})\\b").matcher(text);
                while (m.find()) {
                    int val = Integer.parseInt(m.group(1));
                    if (val >= 20 && val <= 30) {
                        if (val < lowestLevel) {
                            lowestLevel = val;
                            slot5ReplacementValue = value;
                        }
                    }
                }
            }
            
            if (slot5ReplacementValue != null) {
                System.out.println("Swapping slot 5 with PC Pokemon of level " + lowestLevel);
                ((JavascriptExecutor) driver).executeScript("document.getElementById('ddParty').value = '5';");
                ((JavascriptExecutor) driver).executeScript("document.getElementById('ddMon').value = arguments[0];", slot5ReplacementValue);
                ((JavascriptExecutor) driver).executeScript("document.getElementById('frmSwap').submit();");
                Thread.sleep(3000); // Wait for swap to complete
                System.out.println("Slot 5 swap executed successfully.");
                configureNewPokemonAttacks(slot5ReplacementValue);
                
                // Return back to center page to do slot 6
                if (!driver.getCurrentUrl().contains("pokemoncenter.php")) {
                    driver.get("https://pokemonbattlearena.net/members/pokemoncenter.php");
                    Thread.sleep(1000);
                }
            } else {
                System.out.println("No suitable Pokemon (Level 20-30) found in PC for slot 5.");
            }
            
            // Slot 6: level >50 and <70, whichever is first available
            pcOptions = (List<WebElement>) ((JavascriptExecutor) driver).executeScript("return document.getElementById('ddMon').options;");
            String slot6ReplacementValue = null;
            int firstValidLevel = -1;
            
            for (WebElement option : pcOptions) {
                String text = option.getAttribute("textContent");
                String value = option.getAttribute("value");
                if (value == null || value.trim().isEmpty() || text == null || text.contains("Empty Slot")) continue;
                
                Matcher m = Pattern.compile("\\b(\\d{1,3})\\b").matcher(text);
                while (m.find()) {
                    int val = Integer.parseInt(m.group(1));
                    if (val > 50 && val < 70) {
                        firstValidLevel = val;
                        slot6ReplacementValue = value;
                        break;
                    }
                }
                if (slot6ReplacementValue != null) break;
            }
            
            if (slot6ReplacementValue != null) {
                System.out.println("Swapping slot 6 with PC Pokemon of level " + firstValidLevel);
                ((JavascriptExecutor) driver).executeScript("document.getElementById('ddParty').value = '6';");
                ((JavascriptExecutor) driver).executeScript("document.getElementById('ddMon').value = arguments[0];", slot6ReplacementValue);
                ((JavascriptExecutor) driver).executeScript("document.getElementById('frmSwap').submit();");
                Thread.sleep(3000); // Wait for swap to complete
                System.out.println("Slot 6 swap executed successfully.");
                configureNewPokemonAttacks(slot6ReplacementValue);
            } else {
                System.out.println("No suitable Pokemon (Level 51-69) found in PC for slot 6.");
            }
            
        } catch (Exception e) {
            System.out.println("Error setting up team slots 5 and 6: " + e.getMessage());
        }
    }
    
    public boolean swapHighLevelForLegendaryOrSpecial(int maxLevelThreshold) {
        if (!driver.getCurrentUrl().contains("pokemoncenter.php")) {
            driver.get("https://pokemonbattlearena.net/members/pokemoncenter.php");
            try { Thread.sleep(1000); } catch (Exception e) {}
        }

        // Check if any team member is >= maxLevelThreshold
        System.out.println("Checking team for Pokemon >= Level " + maxLevelThreshold + "...");
        int highLevelIndex = -1;
        try {
            List<WebElement> teamTables = driver.findElements(By.xpath("//table[contains(@class, 'table-striped')]//tr/td[contains(., 'lvl')]/sup | //table[contains(@class, 'table-striped')]//tr/td[contains(text(), 'lvl')]/sup"));
            if (teamTables.isEmpty()) {
                teamTables = driver.findElements(By.xpath("//td[contains(., 'lvl')]/sup"));
            }
            int numTeamMembers = teamTables.size();
            
            for (int i = 0; i < numTeamMembers; i++) {
                // Re-fetch to avoid stale element reference
                List<WebElement> currentTeamTables = driver.findElements(By.xpath("//table[contains(@class, 'table-striped')]//tr/td[contains(., 'lvl')]/sup | //table[contains(@class, 'table-striped')]//tr/td[contains(text(), 'lvl')]/sup"));
                if (currentTeamTables.isEmpty()) {
                    currentTeamTables = driver.findElements(By.xpath("//td[contains(., 'lvl')]/sup"));
                }
                if (i >= currentTeamTables.size()) continue;

                String lvlStr = currentTeamTables.get(i).getText().replaceAll("[^0-9]", "");
                if (!lvlStr.isEmpty() && Integer.parseInt(lvlStr) >= maxLevelThreshold) {
                    highLevelIndex = i + 1; // 1-indexed for ddParty
                    break;
                }
            }
        } catch (Exception e) {
            System.out.println("Error parsing team levels: " + e.getMessage());
        }

        if (highLevelIndex != -1) {
            System.out.println("Found Pokemon >= Level " + maxLevelThreshold + " in slot " + highLevelIndex + "!");
            String replacementMonValue = null;
            try {
                String[] legendaries = {
                    "articuno", "zapdos", "moltres", "mewtwo", "mew",
                    "raikou", "entei", "suicune", "lugia", "ho-oh", "celebi",
                    "regirock", "regice", "registeel", "latias", "latios", "kyogre", "groudon", "rayquaza", "jirachi", "deoxys",
                    "uxie", "mesprit", "azelf", "dialga", "palkia", "heatran", "regigigas", "giratina", "cresselia", "phione", "manaphy", "darkrai", "shaymin", "arceus",
                    "victini", "cobalion", "terrakion", "virizion", "tornadus", "thundurus", "reshiram", "zekrom", "landorus", "kyurem", "keldeo", "meloetta", "genesect",
                    "xerneas", "yveltal", "zygarde", "diancie", "hoopa", "volcanion",
                    "type: null", "silvally", "tapu koko", "tapu lele", "tapu bulu", "tapu fini", "cosmog", "cosmoem", "solgaleo", "lunala", "nihilego", "buzzwole", "pheromosa", "xurkitree", "celesteela", "kartana", "guzzlord", "necrozma", "magearna", "marshadow", "poipole", "naganadel", "stakataka", "blacephalon", "zeraora", "meltan", "melmetal",
                    "zacian", "zamazenta", "eternatus", "kubfu", "urshifu", "zarude", "regieleki", "regidrago", "glastrier", "spectrier", "calyrex", "enamorus"
                };

                String highestLevelMonValue = null;
                int maxLevelFound = -1;

                List<WebElement> pcOptions = (List<WebElement>) ((JavascriptExecutor) driver).executeScript("return document.getElementById('ddMon').options;");
                for (WebElement option : pcOptions) {
                    String text = option.getAttribute("textContent");
                    String value = option.getAttribute("value");
                    if (value == null || value.trim().isEmpty() || text == null || text.contains("Empty Slot")) continue;
                    
                    int level = -1;
                    java.util.regex.Matcher m = java.util.regex.Pattern.compile("\\b(\\d{1,3})\\b").matcher(text);
                    if (m.find()) {
                        level = Integer.parseInt(m.group(1));
                    }
                    
                    if (level < 90) {
                        if (level > maxLevelFound) {
                            maxLevelFound = level;
                            highestLevelMonValue = value;
                        }
                        
                        String lowerText = text.toLowerCase();
                        boolean isLegendary = false;
                        for (String leg : legendaries) {
                            if (java.util.regex.Pattern.compile("\\b" + leg + "\\b").matcher(lowerText).find()) {
                                isLegendary = true;
                                break;
                            }
                        }
                        
                        boolean isSpecial = java.util.regex.Pattern.compile("\\b(shiny|metallic|mystic|dark|shadow)\\b").matcher(lowerText).find();
                        
                        if (isLegendary || isSpecial) {
                            replacementMonValue = value;
                            break; // found one!
                        }
                    }
                }
                
                if (replacementMonValue == null) {
                    replacementMonValue = highestLevelMonValue;
                    if (replacementMonValue != null) {
                        System.out.println("No Legendary/Special Pokemon < Level 90 found. Falling back to highest level available (< 90): Level " + maxLevelFound);
                    }
                }
            } catch (Exception e) {
                System.out.println("Error parsing PC Pokemon: " + e.getMessage());
            }

            if (replacementMonValue != null) {
                System.out.println("Swapping slot " + highLevelIndex + " with Legendary/Special PC Pokemon.");
                try {
                    ((JavascriptExecutor) driver).executeScript("document.getElementById('ddParty').value = arguments[0];", String.valueOf(highLevelIndex));
                    ((JavascriptExecutor) driver).executeScript("document.getElementById('ddMon').value = arguments[0];", replacementMonValue);
                    ((JavascriptExecutor) driver).executeScript("document.getElementById('frmSwap').submit();");
                    Thread.sleep(3000); // Wait for swap to complete
                    System.out.println("Swap executed successfully.");
                    
                    configureNewPokemonAttacks(replacementMonValue);
                    return true;
                } catch (Exception e) {
                    System.out.println("Failed to execute swap: " + e.getMessage());
                }
            } else {
                System.out.println("No Legendary/Special Pokemon found in PC to swap with.");
            }
        } else {
            System.out.println("No Pokemon >= Level " + maxLevelThreshold + " found in active team. Continuing.");
        }
        return false;
    }

    private void configureNewPokemonAttacks(String pokemonId) {
        try {
            System.out.println("Configuring attacks for newly swapped Pokemon with ID " + pokemonId);
            driver.get("https://pokemonbattlearena.net/members/pokemoncenter.php"); // ensure we are here
            Thread.sleep(1000);
            
            // Try to find the exact link on the page that contains this ID
            List<WebElement> exactLinks = driver.findElements(By.xpath("//a[contains(@href, '" + pokemonId + "')]"));
            
            if (!exactLinks.isEmpty()) {
                String url = exactLinks.get(0).getAttribute("href");
                System.out.println("Found Pokemon details link: " + url);
                driver.get(url);
            } else {
                // Fallback: Guess the URL structure
                System.out.println("Could not find exact link on page. Guessing URL...");
                driver.get("https://pokemonbattlearena.net/members/pokemon.php?id=" + pokemonId);
            }
            Thread.sleep(1500);
            
            configureAttacksOnCurrentPage();
            
        } catch (Exception e) {
            System.out.println("Failed to configure attacks: " + e.getMessage());
        }
    }
    
    public void configureAllActiveTeamAttacks() {
        System.out.println("Configuring attacks for ALL active team members...");
        if (!driver.getCurrentUrl().contains("pokemoncenter.php")) {
            driver.get("https://pokemonbattlearena.net/members/pokemoncenter.php");
            try { Thread.sleep(1000); } catch (Exception e) {}
        }
        
        java.util.List<String> teamUrls = new java.util.ArrayList<>();
        try {
            List<WebElement> links = driver.findElements(By.xpath("//table[contains(@class, 'table-striped')]//a[contains(@href, 'pokemon.php?ID=') or contains(@href, 'pokemon.php?id=')] | //table[contains(@class, 'table')]//a[contains(@href, 'pokemon.php?ID=') or contains(@href, 'pokemon.php?id=')]"));
            for (WebElement link : links) {
                String url = link.getAttribute("href");
                if (url != null && !teamUrls.contains(url)) {
                    teamUrls.add(url);
                }
            }
        } catch (Exception e) {
            System.out.println("Error finding team links: " + e.getMessage());
        }
        
        if (teamUrls.isEmpty()) {
            System.out.println("Could not find any team member links. Falling back to scraping first 6 pokemon.php links.");
            try {
                List<WebElement> allLinks = driver.findElements(By.xpath("//a[contains(@href, 'pokemon.php?ID=') or contains(@href, 'pokemon.php?id=')]"));
                for (WebElement link : allLinks) {
                    String url = link.getAttribute("href");
                    if (url != null && !teamUrls.contains(url)) {
                        teamUrls.add(url);
                        if (teamUrls.size() == 6) break;
                    }
                }
            } catch (Exception e) {}
        }

        for (String url : teamUrls) {
            try {
                System.out.println("Navigating to team member: " + url);
                driver.get(url);
                Thread.sleep(1500);
                
                configureAttacksOnCurrentPage();
                
            } catch (Exception e) {
                System.out.println("Failed to configure attacks for " + url + ": " + e.getMessage());
            }
        }
        
        // Return to pokemon center after done
        driver.get("https://pokemonbattlearena.net/members/pokemoncenter.php");
        try { Thread.sleep(1000); } catch (Exception e) {}
    }

    private void configureAttacksOnCurrentPage() {
        try {
            // Now on pokemon details page
            List<WebElement> allSelects = driver.findElements(By.tagName("select"));
            WebElement slot1 = null;
            org.openqa.selenium.support.ui.Select dropdown = null;
            
            for (WebElement select : allSelects) {
                try {
                    org.openqa.selenium.support.ui.Select tempDropdown = new org.openqa.selenium.support.ui.Select(select);
                    for (WebElement opt : tempDropdown.getOptions()) {
                        String text = opt.getAttribute("textContent");
                        if (text != null && text.contains("Power: ")) {
                            slot1 = select;
                            dropdown = tempDropdown;
                            break;
                        }
                    }
                } catch (Exception e) {}
                if (slot1 != null) break;
            }
            
            if (slot1 != null) {
                System.out.println("Found Attack Dropdown! Name attribute: " + slot1.getAttribute("name"));
                List<WebElement> options = dropdown.getOptions();
                
                java.util.List<Integer> powers = new java.util.ArrayList<>();
                java.util.Map<Integer, String> powerToText = new java.util.HashMap<>();
                java.util.Map<Integer, String> powerToValue = new java.util.HashMap<>();
                
                for (WebElement opt : options) {
                    String text = opt.getAttribute("textContent");
                    String val = opt.getAttribute("value");
                    if (text != null && text.contains("Power: ")) {
                        try {
                            String pwrStr = text.substring(text.indexOf("Power: ") + 7).replaceAll("[^0-9]", "");
                            int pwr = Integer.parseInt(pwrStr);
                            if (!powers.contains(pwr)) {
                                powers.add(pwr);
                                powerToText.put(pwr, text);
                                powerToValue.put(pwr, val);
                            }
                        } catch (Exception e) {}
                    }
                }
                
                if (!powers.isEmpty()) {
                    java.util.Collections.sort(powers, java.util.Collections.reverseOrder());
                    String valueToSelect = powerToValue.get(powers.get(0));
                    
                    ((org.openqa.selenium.JavascriptExecutor) driver).executeScript(
                        "arguments[0].value = arguments[1];" +
                        "if(arguments[0].tomselect) { arguments[0].tomselect.setValue(arguments[1]); arguments[0].tomselect.sync(); }" +
                        "var evt = document.createEvent('HTMLEvents'); evt.initEvent('change', false, true); arguments[0].dispatchEvent(evt);", 
                        slot1, valueToSelect);
                        
                    System.out.println("Selected attack with highest power (" + powers.get(0) + ").");
                    WebElement form = slot1.findElement(By.xpath("./ancestor::form"));
                    try {
                        WebElement submitBtn = form.findElement(By.xpath(".//input[@type='submit' or contains(translate(@value, 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz'), 'change') or contains(translate(@value, 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz'), 'update')] | .//button[@type='submit' or contains(translate(text(), 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz'), 'change') or contains(translate(text(), 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz'), 'update') or contains(translate(text(), 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz'), 'attack')]"));
                        ((org.openqa.selenium.JavascriptExecutor) driver).executeScript("arguments[0].click();", submitBtn);
                    } catch (Exception ex) {
                        form.submit(); // fallback
                    }
                    Thread.sleep(1500);
                    System.out.println("Saved attacks successfully.");
                } else {
                    System.out.println("No attacks with power found in dropdown.");
                }
            } else {
                System.out.println("Could not find attack slot dropdowns on pokemon page.");
            }
        } catch (Exception e) {
            System.out.println("Failed to configure attacks: " + e.getMessage());
        } finally {
            // Always return to the pokemon center so the hunting loop can proceed
            driver.get("https://pokemonbattlearena.net/members/pokemoncenter.php");
            try { Thread.sleep(1000); } catch(Exception e) {}
        }
    }
}
