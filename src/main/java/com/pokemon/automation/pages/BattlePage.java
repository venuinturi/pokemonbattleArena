package com.pokemon.automation.pages;

import com.pokemon.automation.base.BasePage;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.ui.ExpectedConditions;

import java.util.List;

public class BattlePage extends BasePage {

    @FindBy(css = ".attack-button, .move-select")
    private List<WebElement> attackButtons;

    @FindBy(css = ".pokemon-team-member")
    private List<WebElement> teamMembers;

    @FindBy(css = ".continue-button, input[value='Continue']")
    private WebElement continueButton;

    public BattlePage(WebDriver driver) {
        super(driver);
    }

    public boolean selectLowestPowerAttack() {
        try {
            boolean attackSelected = false;
            // 1. Try Radio Buttons first (broadened xpath to catch any radio on the battle screen)
            List<WebElement> radios = driver.findElements(By.xpath("//input[@type='radio']"));
            if (!radios.isEmpty()) {
                WebElement weakestRadio = radios.get(0);
                int minPower = Integer.MAX_VALUE;
                for (WebElement radio : radios) {
                    try {
                        String text = radio.findElement(By.xpath("..")).getText(); // Get parent text
                        int power = 999;
                        if (text.contains("Power: ")) {
                            String powerStr = text.substring(text.indexOf("Power: ") + 7).replaceAll("[^0-9]", "");
                            if (!powerStr.isEmpty()) power = Integer.parseInt(powerStr);
                        }
                        if (power > 0 && power < minPower) {
                            minPower = power;
                            weakestRadio = radio;
                        }
                    } catch (Exception e) {}
                }
                new org.openqa.selenium.interactions.Actions(driver).moveToElement(weakestRadio).click().perform();
                attackSelected = true;
                System.out.println("Selected weakest attack via radio button");
            }
            
            // 2. Dropdown Logic (Handles 1v1, 2v2, 3v3)
            if (!attackSelected) {
                List<WebElement> allSelects = driver.findElements(By.tagName("select"));
                int targetSelectionIndex = 0;
                
                for (WebElement selectElement : allSelects) {
                    try {
                        org.openqa.selenium.support.ui.Select dropdown = new org.openqa.selenium.support.ui.Select(selectElement);
                        List<WebElement> options = dropdown.getOptions();
                        if (options.isEmpty()) continue;
                        
                        // Check if it's an attack dropdown
                        boolean isAttackDropdown = false;
                        for (WebElement opt : options) {
                            if (opt.getText().contains("Power: ")) {
                                isAttackDropdown = true;
                                break;
                            }
                        }
                        
                        if (isAttackDropdown) {
                            WebElement weakestOption = null;
                            int minPower = Integer.MAX_VALUE;
                            for (WebElement option : options) {
                                String text = option.getText();
                                int power = 999;
                                if (text.contains("Power: ")) {
                                    try {
                                        String powerStr = text.substring(text.indexOf("Power: ") + 7).replaceAll("[^0-9]", "");
                                        if (!powerStr.isEmpty()) power = Integer.parseInt(powerStr);
                                    } catch (Exception e) {}
                                }
                                if (power > 0 && power < minPower) {
                                    minPower = power;
                                    weakestOption = option;
                                }
                            }
                            if (weakestOption != null) {
                                String valueToSelect = weakestOption.getAttribute("value");
                                ((org.openqa.selenium.JavascriptExecutor) driver).executeScript(
                                    "arguments[0].value = arguments[1];" +
                                    "var opts = arguments[0].options; for(var i=0; i<opts.length; i++) { if(opts[i].value == arguments[1]) { arguments[0].selectedIndex = i; break; } }" +
                                    "if(arguments[0].tomselect) { arguments[0].tomselect.setValue(arguments[1]); arguments[0].tomselect.sync(); }" +
                                    "var evt = document.createEvent('HTMLEvents'); evt.initEvent('change', false, true); arguments[0].dispatchEvent(evt);", 
                                    selectElement, valueToSelect);
                                attackSelected = true;
                            }
                        } else {
                            // Target logic
                            String name = selectElement.getAttribute("name");
                            String id = selectElement.getAttribute("id");
                            boolean isTarget = (name != null && name.toLowerCase().contains("target")) || (id != null && id.toLowerCase().contains("target"));
                            
                            if (isTarget || (!isAttackDropdown && options.size() > 0)) {
                                java.util.List<WebElement> validOptions = new java.util.ArrayList<>();
                                for (WebElement opt : options) {
                                    String text = opt.getText().toLowerCase();
                                    if (!text.trim().isEmpty() && !text.contains("select") && !text.contains("choose")) {
                                        validOptions.add(opt);
                                    }
                                }
                                
                                if (!validOptions.isEmpty()) {
                                    if (validOptions.size() <= 6) {
                                        int optionToSelect = targetSelectionIndex % validOptions.size();
                                        WebElement selectedOption = validOptions.get(optionToSelect);
                                        String valueToSelect = selectedOption.getAttribute("value");
                                        ((org.openqa.selenium.JavascriptExecutor) driver).executeScript(
                                            "var select = arguments[0]; var val = arguments[1];" +
                                            "var nativeSetter = Object.getOwnPropertyDescriptor(window.HTMLSelectElement.prototype, 'value')?.set;" +
                                            "if (nativeSetter) { nativeSetter.call(select, val); } else { select.value = val; }" +
                                            "var opts = select.options; for(var i=0; i<opts.length; i++) { if(opts[i].value == val) { select.selectedIndex = i; break; } }" +
                                            "if(select.tomselect) { select.tomselect.setValue(val); select.tomselect.sync(); }" +
                                            "select.dispatchEvent(new Event('change', { bubbles: true }));" +
                                            "select.dispatchEvent(new Event('input', { bubbles: true }));", 
                                            selectElement, valueToSelect);
                                        targetSelectionIndex++;
                                    }
                                }
                            }
                        }
                    } catch (Exception e) {
                        // ignore issues with a specific select element and move to the next
                    }
                }
            }
            
            // Click Attack button
            List<WebElement> attackBtns = driver.findElements(By.xpath("//input[contains(translate(@value, 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz'), 'attack')] | //button[contains(translate(text(), 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz'), 'attack')] | //*[contains(@class, 'btn-danger') and contains(translate(@value, 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz'), 'attack')]"));
            for (WebElement btn : attackBtns) {
                if (btn.isDisplayed()) {
                    new org.openqa.selenium.interactions.Actions(driver).moveToElement(btn).click().perform();
                    return true;
                }
            }
            return false;
        } catch (Exception ex) {
            System.out.println("Could not find attack element: " + ex.getMessage());
            return false;
        }
    }

    public boolean selectHighestPowerAttack() {
        try {
            boolean attackSelected = false;
            // 1. Try Radio Buttons first
            List<WebElement> radios = driver.findElements(By.xpath("//input[@type='radio']"));
            if (!radios.isEmpty()) {
                WebElement strongestRadio = radios.get(0);
                int maxPower = -1;
                for (WebElement radio : radios) {
                    try {
                        String text = radio.findElement(By.xpath("..")).getText(); // Get parent text
                        int power = -1;
                        if (text.contains("Power: ")) {
                            String powerStr = text.substring(text.indexOf("Power: ") + 7).replaceAll("[^0-9]", "");
                            if (!powerStr.isEmpty()) power = Integer.parseInt(powerStr);
                        }
                        if (power > maxPower) {
                            maxPower = power;
                            strongestRadio = radio;
                        }
                    } catch (Exception e) {}
                }
                new org.openqa.selenium.interactions.Actions(driver).moveToElement(strongestRadio).click().perform();
                attackSelected = true;
                System.out.println("Selected highest power attack via radio button");
            }
            
            // 2. Dropdown Logic (Handles 1v1, 2v2, 3v3)
            if (!attackSelected) {
                List<WebElement> allSelects = driver.findElements(By.tagName("select"));
                int targetSelectionIndex = 0;
                
                for (WebElement selectElement : allSelects) {
                    try {
                        org.openqa.selenium.support.ui.Select dropdown = new org.openqa.selenium.support.ui.Select(selectElement);
                        List<WebElement> options = dropdown.getOptions();
                        if (options.isEmpty()) continue;
                        
                        // Check if it's an attack dropdown
                        boolean isAttackDropdown = false;
                        for (WebElement opt : options) {
                            if (opt.getText().contains("Power: ")) {
                                isAttackDropdown = true;
                                break;
                            }
                        }
                        
                        if (isAttackDropdown) {
                            WebElement strongestOption = null;
                            int maxPower = -1;
                            for (WebElement option : options) {
                                String text = option.getText();
                                int power = -1;
                                if (text.contains("Power: ")) {
                                    try {
                                        String powerStr = text.substring(text.indexOf("Power: ") + 7).replaceAll("[^0-9]", "");
                                        if (!powerStr.isEmpty()) power = Integer.parseInt(powerStr);
                                    } catch (Exception e) {}
                                }
                                if (power > maxPower) {
                                    maxPower = power;
                                    strongestOption = option;
                                }
                            }
                            if (strongestOption != null) {
                                String valueToSelect = strongestOption.getAttribute("value");
                                ((org.openqa.selenium.JavascriptExecutor) driver).executeScript(
                                    "arguments[0].value = arguments[1];" +
                                    "var opts = arguments[0].options; for(var i=0; i<opts.length; i++) { if(opts[i].value == arguments[1]) { arguments[0].selectedIndex = i; break; } }" +
                                    "if(arguments[0].tomselect) { arguments[0].tomselect.setValue(arguments[1]); arguments[0].tomselect.sync(); }" +
                                    "var evt = document.createEvent('HTMLEvents'); evt.initEvent('change', false, true); arguments[0].dispatchEvent(evt);", 
                                    selectElement, valueToSelect);
                                attackSelected = true;
                            }
                        } else {
                            // Check if it's a target dropdown (typically named 'target' or similar)
                            String name = selectElement.getAttribute("name");
                            String id = selectElement.getAttribute("id");
                            boolean isTarget = (name != null && name.toLowerCase().contains("target")) || (id != null && id.toLowerCase().contains("target"));
                            
                            if (isTarget || (!isAttackDropdown && options.size() > 0)) {
                                // Filter valid targets (avoid empty or "Select" options)
                                java.util.List<WebElement> validOptions = new java.util.ArrayList<>();
                                for (WebElement opt : options) {
                                    String text = opt.getText().toLowerCase();
                                    if (!text.trim().isEmpty() && !text.contains("select") && !text.contains("choose")) {
                                        validOptions.add(opt);
                                    }
                                }
                                
                                if (!validOptions.isEmpty()) {
                                    // Make sure we only assign targets in the battle area (usually 1-3 valid options)
                                    if (validOptions.size() <= 6) {
                                        int optionToSelect = targetSelectionIndex % validOptions.size();
                                        WebElement selectedOption = validOptions.get(optionToSelect);
                                        String valueToSelect = selectedOption.getAttribute("value");
                                        
                                        ((org.openqa.selenium.JavascriptExecutor) driver).executeScript(
                                            "var select = arguments[0]; var val = arguments[1];" +
                                            "var nativeSetter = Object.getOwnPropertyDescriptor(window.HTMLSelectElement.prototype, 'value')?.set;" +
                                            "if (nativeSetter) { nativeSetter.call(select, val); } else { select.value = val; }" +
                                            "var opts = select.options; for(var i=0; i<opts.length; i++) { if(opts[i].value == val) { select.selectedIndex = i; break; } }" +
                                            "if(select.tomselect) { select.tomselect.setValue(val); select.tomselect.sync(); }" +
                                            "select.dispatchEvent(new Event('change', { bubbles: true }));" +
                                            "select.dispatchEvent(new Event('input', { bubbles: true }));", 
                                            selectElement, valueToSelect);
                                        
                                        targetSelectionIndex++;
                                    }
                                }
                            }
                        }
                    } catch (Exception e) {
                        // ignore issues with a specific select element and move to the next
                    }
                }
            }
            
            List<WebElement> attackBtns = driver.findElements(By.xpath("//input[contains(translate(@value, 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz'), 'attack')] | //button[contains(translate(text(), 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz'), 'attack')] | //*[contains(@class, 'btn-danger') and contains(translate(@value, 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz'), 'attack')]"));
            for (WebElement btn : attackBtns) {
                if (btn.isDisplayed()) {
                    new org.openqa.selenium.interactions.Actions(driver).moveToElement(btn).click().perform();
                    return true;
                }
            }
            return false;
        } catch (Exception ex) {
            System.out.println("Could not find attack element: " + ex.getMessage());
            return false;
        }
    }

    public void swapToLowestLevelPokemon() {
        if (!teamMembers.isEmpty()) {
            WebElement lowestLevelPokemon = teamMembers.get(0);
            int minLevel = Integer.MAX_VALUE;

            for (WebElement member : teamMembers) {
                int level = extractPowerFromText(member.getText()); // Reuse extraction for level
                if (level > 0 && level < minLevel) {
                    minLevel = level;
                    lowestLevelPokemon = member;
                }
            }
            wait.until(ExpectedConditions.elementToBeClickable(lowestLevelPokemon)).click();
            // Assuming clicking it swaps, may need to wait for swap confirmation
        }
    }
    
    private int extractPowerFromText(String text) {
        try {
            String numStr = text.replaceAll("[^0-9]", "");
            if (!numStr.isEmpty()) {
                return Integer.parseInt(numStr);
            }
        } catch (Exception e) {
            // Ignored
        }
        return 999; // Default high power if unparseable
    }

    @FindBy(css = ".enemy-level")
    private WebElement enemyLevelElement;
    
    public String getEnemyName() {
        try {
            java.util.List<WebElement> optableCells = driver.findElements(By.cssSelector("#optable td"));
            if (!optableCells.isEmpty()) {
                String text = optableCells.get(0).getText();
                String[] lines = text.split("\n");
                if (lines.length > 0) {
                    return lines[0].trim();
                }
            }
        } catch (Exception e) {}
        return "Unknown Pokemon";
    }

    public boolean isLegendary(String name) {
        if (name == null) return false;
        name = name.toLowerCase();
        String[] legendaries = {
            "aaron", "acophyte", "alpha appletun", "alpha arceus", "alpha caterpie",
            "alpha charizard", "alpha cosmog", "alpha darkrai", "alpha deoxys", "alpha dialga",
            "alpha espeon (black cat)", "alpha giratina (origin form)", "alpha ho-oh", "alpha hoopa (unbound)", "alpha kyurem (white)",
            "alpha lugia", "alpha mega rayquaza", "alpha mew", "alpha mewtwo", "alpha palkia",
            "alpha typhlosion", "alpha victini", "alpha weedle", "arceus", "armored zygarde (10%)",
            "armored zygarde (complete)", "articuno", "articuno (divine)", "astral deoxys", "azelf",
            "babereap", "baby lugia", "baphomet", "blacephalon", "bloodmoon ursaluna",
            "bobbuffet", "bushil", "buzzwole", "calyrex", "calyrex (ice rider)",
            "calyrex (shadow rider)", "carnivine", "cascoon", "catquaza", "celebi",
            "celesteela", "cherrim (sunshine)", "chi-yu", "chien-pao", "cobalion",
            "cosmog", "cresselia", "crystal blacephalon", "crystal charizard", "crystal chikorita",
            "crystal cyndaquil", "crystal lapras", "crystal litwick", "crystal mudkip", "crystal onix",
            "crystal stakataka", "crystal torchic", "crystal totodile", "crystal treecko", "cyber absol",
            "cyber darkrema", "cyber falinks", "cyber goomy", "cyber kabuto", "dark zekrom",
            "darkrai", "darkrai (divine)", "darkrema", "dawn wings necrozma", "deoxys",
            "deoxys (attack form)", "deoxys (defense form)", "deoxys (speed form)", "dialga", "dialga (origin)",
            "diancie", "ditto (winter)", "dolphini", "drampa", "dratini (electric elemental)",
            "dratini (fire elemental)", "dratini (ice elemental)", "dratini (winter)", "durant", "dusk mane necrozma",
            "elite training champion", "emberoll", "emosys", "enamorus", "enamorus (therian)",
            "enlune", "entei", "entei (amped)", "eternatus", "event (aeg bday)",
            "event (aeg&#039;s bday)", "event (jazzy bday)", "event (osiris bday)", "event (tyrael bday)", "evil celebi",
            "ewok teddiursa", "falinks", "fezandipiti", "florion", "flutter mane",
            "fossil totodile", "fossil tyrunt", "galarian articuno", "galarian moltres", "galarian zapdos",
            "genesect", "ghost", "gigantamax alcremie", "gigantamax appletun", "gigantamax blastoise",
            "gigantamax butterfree", "gigantamax centiskorch", "gigantamax charizard", "gigantamax charizard (winter)", "gigantamax cinderace",
            "gigantamax coalossal", "gigantamax copperajah", "gigantamax corviknight", "gigantamax drednaw", "gigantamax duraludon",
            "gigantamax eevee", "gigantamax flapple", "gigantamax garbodor", "gigantamax gengar", "gigantamax grimmsnarl",
            "gigantamax hatterene", "gigantamax inteleon", "gigantamax kingler", "gigantamax lapras", "gigantamax lapras (pirate)",
            "gigantamax machamp", "gigantamax melmetal", "gigantamax meowth", "gigantamax orbeetle", "gigantamax pikachu",
            "gigantamax rillaboom", "gigantamax sandaconda", "gigantamax snorlax", "gigantamax snorlax (winter)", "gigantamax toxtricity",
            "gigantamax urshifu (rapid-strike style)", "gigantamax urshifu (single-strike style)", "gigantamax venusaur", "gigantamax venusaur (winter)", "giratina",
            "giratina (origin form)", "glastrier", "golden scizor", "gouging fire", "groudon",
            "guzzlord", "halloween blastoise", "halloween bulbasaur", "halloween squirtle", "happy celebi",
            "hatchling ho-oh", "hatmo", "heatmor", "heatran", "hinozerel",
            "ho-oh", "hoopa", "iron boulder", "iron crown", "iron jugulis",
            "iron leaves", "iron thorns", "iron valiant", "jirachi", "kartana",
            "keldeo", "keldeo (resolute)", "koraidon", "koraidon (valentine)", "kubfu",
            "kyogre", "kyurem", "kyurem (black)", "kyurem (white)", "landorus",
            "landorus (therian)", "latias", "latios", "lugia", "lumirema",
            "lunala (full moon)", "lunwere", "magearna", "magquaza", "majora marshadow",
            "manamo", "manaphy", "marshadow", "marshadow (zenith)", "mecha-tyranitar",
            "mega crystal mewtwo x", "mega crystal mewtwo y", "meloetta", "meloetta (pirouette)", "meltan",
            "mesprit", "mew", "mew (armored)", "mewtwo", "mewtwo (armored)",
            "miraidon", "miraidon (valentine)", "missingno", "moltres", "moltres (divine)",
            "munkidori", "necrozma", "neo armored mewtwo", "nihilego", "ninstar",
            "ogerpon", "okidogi", "onyx tyrunt", "palkia", "palkia (origin)",
            "panthulhu", "pecharunt", "pheromosa", "phione", "pichu (spiky-eared)",
            "piplup (ninja)", "poipole", "pokeexpress (conductor)", "pokeexpress (cozy)", "pokeexpress (gift)",
            "pokeexpress (wish)", "poltahaus", "primal dialga", "primal palkia", "psyduck (ninja)",
            "raging bolt", "raikou", "raikou (amped)", "rainbow armored zygarde (10%)", "rainbow growlithe",
            "rainbow ho-oh", "rainbow mewtwo (armored)", "rainbow neo armored mewtwo", "rainbow vulpix", "rainbow zorua",
            "rayquaza", "rayquaza (zenith)", "regice", "regidrago", "regieleki",
            "regigigas", "regirock", "registeel", "reshiram", "roaring moon",
            "rotom", "royal giratina", "royal kabuto", "royal magikarp", "royal manaphy",
            "royal nidoran female", "royal nidoran male", "royal shinx", "sandy shocks", "seviper",
            "shaymin", "shaymin (sky form)", "skalmo", "skuli", "snorlax (winter)",
            "solgaleo (radiant sun)", "spectrier", "stakataka", "stripereor", "suicune",
            "suicune (amped)", "summer charmander", "summer pachirisu", "tapu bulu", "tapu fini",
            "tapu koko", "tapu lele", "terapagos", "terrakion", "thu-fi-zer",
            "thundurus", "thundurus (therian)", "timewarped kyurem", "tinderloin", "ting-lu",
            "tornadus", "tornadus (therian)", "turtonator", "type: null", "tyrogue (ninja)",
            "uxie", "venustoise", "victini", "virizion", "virus cosmog",
            "virus groudon", "virus kyogre", "virus rayquaza", "volcanion", "vouramp",
            "walking wake", "willoel", "wishiwashi (school)", "wo-chien", "xerneas",
            "xurkitree", "yveltal", "zacian (crowned sword)", "zacian (hero)", "zamazenta (crowned shield)",
            "zamazenta (hero)", "zandshrew", "zangoose", "zangoose (ninja)", "zapdos",
            "zapdos (divine)", "zarude", "zekrom", "zemeefin", "zenchen",
            "zeraora", "zorua (stormtrooper)", "zygarde (complete)", "zygarde 10%", "zygarde 50%"
        };
        for (String leg : legendaries) {
            // using word boundary or exact match in case the string is 'Pikachu (Level 5)'
            if (name.contains(leg)) {
                return true;
            }
        }
        return false;
    }

    public int getEnemyLevel() {
        try {
            java.util.List<WebElement> optableCells = driver.findElements(By.cssSelector("#optable td"));
            if (!optableCells.isEmpty()) {
                String text = optableCells.get(0).getText();
                String[] lines = text.split("\n");
                for (String line : lines) {
                    java.util.regex.Matcher m = java.util.regex.Pattern.compile("(?:Level|Lvl)[^0-9]*([0-9]+)", java.util.regex.Pattern.CASE_INSENSITIVE).matcher(line);
                    if (m.find()) {
                        return Integer.parseInt(m.group(1));
                    }
                }
            }
        } catch (Exception e) {}
        return 999;
    }

    public int getEnemyHp() {
        try {
            java.util.List<WebElement> optableCells = driver.findElements(By.cssSelector("#optable td"));
            if (!optableCells.isEmpty()) {
                String text = optableCells.get(0).getText();
                String[] lines = text.split("\n");
                for (String line : lines) {
                    if (line.contains("/") && line.matches(".*\\d+/\\d+.*")) {
                        String hpStr = line.split("/")[0].replaceAll("[^0-9]", "");
                        return Integer.parseInt(hpStr);
                    }
                    if (line.toLowerCase().contains("faint")) {
                        return 0;
                    }
                }
            }
        } catch (Exception e) {}
        return 999;
    }

    public void runAway() {
        try {
            WebElement runBtn = driver.findElement(By.xpath("//button[contains(text(), 'Run')] | //*[contains(@class, 'run-button')]"));
            wait.until(ExpectedConditions.elementToBeClickable(runBtn)).click();
        } catch (Exception e) {}
    }

    public boolean selectAlternativeAttack(int zeroDamageCount) {
        try {
            boolean attackSelected = false;
            // 1. Try Radio Buttons first
            List<WebElement> radios = driver.findElements(By.xpath("//input[@type='radio']"));
            if (!radios.isEmpty()) {
                // If there are radio buttons, pick one based on the zeroDamageCount (modulo the number of options)
                int index = zeroDamageCount % radios.size();
                new org.openqa.selenium.interactions.Actions(driver).moveToElement(radios.get(index)).click().perform();
                attackSelected = true;
                System.out.println("Selected alternative attack index " + index + " via radio button");
            }
            
            // 2. Dropdown Logic
            if (!attackSelected) {
                List<WebElement> allSelects = driver.findElements(By.tagName("select"));
                for (WebElement selectElement : allSelects) {
                    try {
                        org.openqa.selenium.support.ui.Select dropdown = new org.openqa.selenium.support.ui.Select(selectElement);
                        List<WebElement> options = dropdown.getOptions();
                        if (options.isEmpty()) continue;
                        
                        boolean isAttackDropdown = false;
                        for (WebElement opt : options) {
                            if (opt.getText().contains("Power: ")) {
                                isAttackDropdown = true;
                                break;
                            }
                        }
                        
                        if (isAttackDropdown) {
                            int index = zeroDamageCount % options.size();
                            WebElement selectedOption = options.get(index);
                            String valueToSelect = selectedOption.getAttribute("value");
                            ((org.openqa.selenium.JavascriptExecutor) driver).executeScript(
                                "var select = arguments[0]; var val = arguments[1];" +
                                "var nativeSetter = Object.getOwnPropertyDescriptor(window.HTMLSelectElement.prototype, 'value')?.set;" +
                                "if (nativeSetter) { nativeSetter.call(select, val); } else { select.value = val; }" +
                                "var opts = select.options; for(var i=0; i<opts.length; i++) { if(opts[i].value == val) { select.selectedIndex = i; break; } }" +
                                "if(select.tomselect) { select.tomselect.setValue(val); select.tomselect.sync(); }" +
                                "select.dispatchEvent(new Event('change', { bubbles: true }));" +
                                "select.dispatchEvent(new Event('input', { bubbles: true }));", 
                                selectElement, valueToSelect);
                            attackSelected = true;
                            System.out.println("Selected alternative attack index " + index + " via dropdown");
                        }
                    } catch (Exception e) {}
                }
            }
            
            // Click Attack button
            List<WebElement> attackBtns = driver.findElements(By.xpath("//input[contains(translate(@value, 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz'), 'attack')] | //button[contains(translate(text(), 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz'), 'attack')] | //*[contains(@class, 'btn-danger') and contains(translate(@value, 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz'), 'attack')]"));
            for (WebElement btn : attackBtns) {
                if (btn.isDisplayed()) {
                    new org.openqa.selenium.interactions.Actions(driver).moveToElement(btn).click().perform();
                    return true;
                }
            }
            return false;
        } catch (Exception ex) {
            System.out.println("Could not select alternative attack: " + ex.getMessage());
            return false;
        }
    }
    
    public void usePokeball() {
        try {
            java.util.List<WebElement> itemRows = driver.findElements(By.cssSelector("#myitems tr"));
            for (WebElement row : itemRows) {
                if (row.getText().toLowerCase().contains("poke ball") || row.getText().toLowerCase().contains("pokeball")) {
                    WebElement radio = row.findElement(By.tagName("input"));
                    new org.openqa.selenium.interactions.Actions(driver).moveToElement(radio).click().perform();
                    break;
                }
            }
            WebElement useBtn = driver.findElement(By.xpath("//button[contains(translate(text(), 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz'), 'use item')] | //button[contains(@onclick, 'useitem')] | //input[@value='Use Item']"));
            new org.openqa.selenium.interactions.Actions(driver).moveToElement(useBtn).click().perform();
            System.out.println("Used a Pokeball!");
        } catch (Exception e) {
            System.out.println("Failed to use Pokeball: " + e.getMessage());
        }
    }

    public void useGreatball() {
        try {
            java.util.List<WebElement> itemRows = driver.findElements(By.cssSelector("#myitems tr"));
            for (WebElement row : itemRows) {
                if (row.getText().toLowerCase().contains("great ball") || row.getText().toLowerCase().contains("greatball")) {
                    WebElement radio = row.findElement(By.tagName("input"));
                    new org.openqa.selenium.interactions.Actions(driver).moveToElement(radio).click().perform();
                    break;
                }
            }
            WebElement useBtn = driver.findElement(By.xpath("//button[contains(translate(text(), 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz'), 'use item')] | //button[contains(@onclick, 'useitem')] | //input[@value='Use Item']"));
            new org.openqa.selenium.interactions.Actions(driver).moveToElement(useBtn).click().perform();
            System.out.println("Used a Great Ball!");
        } catch (Exception e) {
            System.out.println("Failed to use Great Ball: " + e.getMessage());
        }
    }

    public void useMasterball() {
        try {
            java.util.List<WebElement> itemRows = driver.findElements(By.cssSelector("#myitems tr"));
            for (WebElement row : itemRows) {
                if (row.getText().toLowerCase().contains("master ball") || row.getText().toLowerCase().contains("masterball")) {
                    WebElement radio = row.findElement(By.tagName("input"));
                    new org.openqa.selenium.interactions.Actions(driver).moveToElement(radio).click().perform();
                    break;
                }
            }
            WebElement useBtn = driver.findElement(By.xpath("//button[contains(translate(text(), 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz'), 'use item')] | //button[contains(@onclick, 'useitem')] | //input[@value='Use Item']"));
            new org.openqa.selenium.interactions.Actions(driver).moveToElement(useBtn).click().perform();
            System.out.println("Used a Master Ball!");
        } catch (Exception e) {
            System.out.println("Failed to use Master Ball: " + e.getMessage());
        }
    }

    public void useRepeatBall() {
        try {
            java.util.List<WebElement> itemRows = driver.findElements(By.cssSelector("#myitems tr"));
            for (WebElement row : itemRows) {
                if (row.getText().toLowerCase().contains("repeat ball") || row.getText().toLowerCase().contains("repeatball")) {
                    WebElement radio = row.findElement(By.tagName("input"));
                    new org.openqa.selenium.interactions.Actions(driver).moveToElement(radio).click().perform();
                    break;
                }
            }
            WebElement useBtn = driver.findElement(By.xpath("//button[contains(translate(text(), 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz'), 'use item')] | //button[contains(@onclick, 'useitem')] | //input[@value='Use Item']"));
            new org.openqa.selenium.interactions.Actions(driver).moveToElement(useBtn).click().perform();
            System.out.println("Used a Repeat Ball!");
        } catch (Exception e) {
            System.out.println("Failed to use Repeat Ball: " + e.getMessage());
        }
    }

    public void useTimerBall() {
        try {
            java.util.List<WebElement> itemRows = driver.findElements(By.cssSelector("#myitems tr"));
            for (WebElement row : itemRows) {
                if (row.getText().toLowerCase().contains("timer ball") || row.getText().toLowerCase().contains("timerball")) {
                    WebElement radio = row.findElement(By.tagName("input"));
                    new org.openqa.selenium.interactions.Actions(driver).moveToElement(radio).click().perform();
                    break;
                }
            }
            WebElement useBtn = driver.findElement(By.xpath("//button[contains(translate(text(), 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz'), 'use item')] | //button[contains(@onclick, 'useitem')] | //input[@value='Use Item']"));
            new org.openqa.selenium.interactions.Actions(driver).moveToElement(useBtn).click().perform();
            System.out.println("Used a Timer Ball!");
        } catch (Exception e) {
            System.out.println("Failed to use Timer Ball: " + e.getMessage());
        }
    }

    public int getPokeballCount() {
        try {
            java.util.List<WebElement> itemRows = driver.findElements(By.cssSelector("#myitems tr"));
            boolean itemsTableExists = !itemRows.isEmpty();
            for (WebElement row : itemRows) {
                String rowText = row.getAttribute("textContent").toLowerCase();
                if (rowText.contains("poke ball") || rowText.contains("pokeball")) {
                    java.util.List<WebElement> tds = row.findElements(By.tagName("td"));
                    if (tds.size() >= 4) {
                        String txt = tds.get(3).getAttribute("textContent").replaceAll("[^0-9]", "");
                        if (!txt.isEmpty()) {
                            return Integer.parseInt(txt);
                        }
                    }
                }
            }
            if (itemsTableExists) {
                // The table exists but no Pokeball row was found. This means we have 0 Pokeballs!
                return 0;
            }
        } catch (Exception e) {
            System.out.println("Could not read pokeball count: " + e.getMessage());
        }
        return 999; // Return a high number only if the table doesn't exist or we hit a real exception
    }

    public boolean isBattleComplete() {
        try {
            String bodyText = driver.findElement(org.openqa.selenium.By.tagName("body")).getText().toLowerCase();
            if (bodyText.contains("you have won the match") || bodyText.contains("you lost") || bodyText.contains("you won") || bodyText.contains("defeated")) {
                return true;
            }
            return isContinuePresent();
        } catch (Exception e) {
            return false;
        }
    }

    public boolean isContinuePresent() {
        try {
            String xpath = "//input[@value='Continue'] | //button[contains(translate(text(), 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz'), 'continue')] | //a[contains(translate(text(), 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz'), 'continue')] | //a[contains(translate(text(), 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz'), 'return')] | //button[contains(translate(text(), 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz'), 'return')] | //*[contains(@class, 'continue-button')]";
            List<WebElement> continues = driver.findElements(By.xpath(xpath));
            for (WebElement el : continues) {
                if (el.isDisplayed()) return true;
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    public void clickContinueIfPresent() {
        try {
            String xpath = "//input[@value='Continue'] | //button[contains(translate(text(), 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz'), 'continue')] | //a[contains(translate(text(), 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz'), 'continue')] | //a[contains(translate(text(), 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz'), 'return')] | //button[contains(translate(text(), 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz'), 'return')] | //*[contains(@class, 'continue-button')]";
            List<WebElement> continues = driver.findElements(By.xpath(xpath));
            for (WebElement el : continues) {
                if (el.isDisplayed()) {
                    new org.openqa.selenium.interactions.Actions(driver).moveToElement(el).click().perform();
                    System.out.println("Clicked Continue/Return button.");
                    break;
                }
            }
        } catch (Exception e) {
            // Ignored if not present
        }
    }

    public String getTeamLevels() {
        StringBuilder levels = new StringBuilder();
        if (!teamMembers.isEmpty()) {
            for (WebElement member : teamMembers) {
                int level = extractPowerFromText(member.getText());
                levels.append(level).append(", ");
            }
        } else {
            return "No team found/visible";
        }
        return levels.toString();
    }

    public boolean handleSelectMonsterScreen() {
        try {
            java.util.List<WebElement> pickButtons = new java.util.ArrayList<>();
            List<WebElement> buttons = driver.findElements(org.openqa.selenium.By.xpath("//input[@type='submit' or @type='button'] | //button | //a[contains(@class, 'button')]"));
            for (WebElement btn : buttons) {
                String text = btn.getAttribute("value");
                if (text == null || text.trim().isEmpty()) {
                    text = btn.getText();
                }
                if (text != null) {
                    text = text.toLowerCase();
                    if (text.equals("select") || text.equals("choose") || text.contains("choose pokemon") || text.contains("send out") || text.equals("pick")) {
                        pickButtons.add(btn);
                    }
                }
            }
            
            if (!pickButtons.isEmpty()) {
                WebElement lowestBtn = pickButtons.get(0);
                int minLevel = Integer.MAX_VALUE;
                
                for (WebElement btn : pickButtons) {
                    try {
                        WebElement row = btn.findElement(By.xpath("./ancestor::tr | ./ancestor::div[contains(@class, 'pokemon') or contains(@class, 'team-member')]"));
                        String rowText = row.getText();
                        int level = 999;
                        java.util.regex.Matcher m = java.util.regex.Pattern.compile("(?:Level|Lvl)\\s*[:]?\\s*(\\d+)", java.util.regex.Pattern.CASE_INSENSITIVE).matcher(rowText);
                        if (m.find()) {
                            level = Integer.parseInt(m.group(1));
                        }
                        if (level < minLevel) {
                            minLevel = level;
                            lowestBtn = btn;
                        }
                    } catch (Exception e) {}
                }
                
                new org.openqa.selenium.interactions.Actions(driver).moveToElement(lowestBtn).click().perform();
                System.out.println("Automatically clicked 'Select Monster' button for lowest level (" + minLevel + ")");
                try { Thread.sleep(1500); } catch(Exception e) {}
                return true;
            }
        } catch (Exception e) {
            System.out.println("Error handling select monster screen: " + e.getMessage());
        }
        return false;
    }
}
