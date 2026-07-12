package com.pokemon.automation.pages;

import com.pokemon.automation.base.BasePage;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.ui.ExpectedConditions;

import java.util.Random;

public class MapNavigationPage extends BasePage {

    @FindBy(xpath = "//*[contains(translate(text(), 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz'), 'click here to battle')] | //input[@value='Battle'] | //button[contains(text(), 'Battle')] | //*[contains(@class, 'battle-button')]")
    private WebElement battleButton;

    @FindBy(css = "body")
    private WebElement body;

    private Random random = new Random();

    public MapNavigationPage(WebDriver driver) {
        super(driver);
    }

    public boolean safeNavigate(String url) {
        try {
            driver.get(url);
            return true;
        } catch (org.openqa.selenium.TimeoutException e) {
            System.out.println("Timeout loading: " + url + " - Attempting refresh...");
            try {
                driver.navigate().refresh();
                return true;
            } catch (Exception ex) {
                return false;
            }
        } catch (Exception e) {
            return false;
        }
    }

    public void scrollToMapArea() {
        try {
            if (driver.getCurrentUrl().contains("map") || driver.getCurrentUrl().contains("wander")) {
                // Scroll down significantly so the map and arrows are centered
                ((org.openqa.selenium.JavascriptExecutor) driver).executeScript("window.scrollTo({top: document.body.scrollHeight * 0.45, behavior: 'smooth'});");
            }
        } catch (Exception e) {}
    }

    public void moveRandomly() {
        Keys[] directions = {Keys.ARROW_UP, Keys.ARROW_DOWN, Keys.ARROW_LEFT, Keys.ARROW_RIGHT};
        Keys randomDirection = directions[random.nextInt(directions.length)];
        
        Actions actions = new Actions(driver);
        actions.sendKeys(body, randomDirection).perform();
    }

    public void moveInDirection(String direction) {
        Keys key = switch (direction.toLowerCase()) {
            case "north", "up" -> Keys.ARROW_UP;
            case "south", "down" -> Keys.ARROW_DOWN;
            case "west", "left" -> Keys.ARROW_LEFT;
            case "east", "right" -> Keys.ARROW_RIGHT;
            default -> Keys.ARROW_UP;
        };
        new Actions(driver).sendKeys(body, key).perform();
    }

    public int getMapIdFromUrl() {
        String url = driver.getCurrentUrl();
        if (url.contains("M=")) {
            try {
                String idStr = url.substring(url.indexOf("M=") + 2);
                if (idStr.contains("&")) {
                    idStr = idStr.substring(0, idStr.indexOf("&"));
                }
                return Integer.parseInt(idStr);
            } catch (Exception e) {
                return -1;
            }
        }
        return -1;
    }

    public boolean isPokemonEncountered() {
        try {
            org.openqa.selenium.support.ui.WebDriverWait shortWait = new org.openqa.selenium.support.ui.WebDriverWait(driver, java.time.Duration.ofSeconds(1));
            return shortWait.until(ExpectedConditions.visibilityOf(battleButton)).isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }
    
    public int getEncounteredPokemonLevel() {
        try {
            String pageText = body.getText();
            java.util.regex.Matcher m = java.util.regex.Pattern.compile("Level\\s*[:]?\\s*(\\d+)", java.util.regex.Pattern.CASE_INSENSITIVE).matcher(pageText);
            if (m.find()) {
                return Integer.parseInt(m.group(1));
            }
            m = java.util.regex.Pattern.compile("Lvl\\s*[:]?\\s*(\\d+)", java.util.regex.Pattern.CASE_INSENSITIVE).matcher(pageText);
            if (m.find()) {
                return Integer.parseInt(m.group(1));
            }
        } catch (Exception e) {}
        return 999; // Default high so we don't accidentally skip a good one if parsing fails
    }
    
    public int[] getPlayerLocation() {
        try {
            org.openqa.selenium.JavascriptExecutor js = (org.openqa.selenium.JavascriptExecutor) driver;
            driver.switchTo().frame("snavi");
            String script = "var p = document.querySelector('img.person[alt=\"Me\"]');" +
                            "if(p) { return p.style.left + ',' + p.style.top; } else { return null; }";
            Object result = js.executeScript(script);
            driver.switchTo().defaultContent();
            
            if (result != null) {
                String[] parts = result.toString().replace("px", "").split(",");
                return new int[]{Integer.parseInt(parts[0].trim()), Integer.parseInt(parts[1].trim())};
            }
        } catch (Exception e) {
            try { driver.switchTo().defaultContent(); } catch(Exception ex) {}
        }
        return null;
    }
    
    public boolean isPokemonAlreadyCaptured() {
        try {
            // 1. Image Check (most reliable)
            java.util.List<WebElement> images = driver.findElements(org.openqa.selenium.By.tagName("img"));
            for (WebElement img : images) {
                String src = img.getAttribute("src");
                String alt = img.getAttribute("alt");
                if (src != null && (src.contains("caught") || src.contains("captured") || src.contains("owned"))) {
                    return true;
                }
                if (alt != null && (alt.toLowerCase().contains("caught") || alt.toLowerCase().contains("captured"))) {
                    return true;
                }
            }
            
            // 2. Text Check (restricted to encounter text area to avoid sidebar)
            WebElement content;
            try {
                content = driver.findElement(org.openqa.selenium.By.id("battleframetext"));
            } catch (Exception ex) {
                content = driver.findElement(org.openqa.selenium.By.cssSelector(".wander_info, .center, #content"));
            }
            
            String text = content.getText().toLowerCase();
            
            if (text.contains("you currently have") || text.contains("already caught") || text.contains("you own") || text.contains("you already have")) {
                if (text.contains("you currently have 0") || text.contains("you have 0") || text.matches(".*you currently have\\s+0\\s+of this pokemon.*")) {
                    return false;
                }
                return true;
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }
    

    public boolean containsSpecialType(String specialTypesCommaSeparated) {
        try {
            String text = body.getText().toLowerCase();
            String[] types = specialTypesCommaSeparated.split(",");
            for (String type : types) {
                if (!type.trim().isEmpty() && text.contains(type.trim().toLowerCase())) {
                    return true;
                }
            }
        } catch (Exception e) {}
        return false;
    }

    public boolean isLegendary() {
        try {
            // Limit scope to the center content to avoid matching team members in the sidebar
            WebElement content;
            try {
                content = driver.findElement(org.openqa.selenium.By.id("battleframetext"));
            } catch (Exception ex) {
                content = driver.findElement(org.openqa.selenium.By.cssSelector(".wander_info, .center, #content"));
            }
            String text = content.getText().toLowerCase();
            
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
                if (java.util.regex.Pattern.compile("\\b" + leg + "\\b").matcher(text).find()) {
                    return true;
                }
            }
        } catch (Exception e) {}
        return false;
    }

    public boolean isAnySpecialType() {
        try {
            WebElement content;
            try {
                content = driver.findElement(org.openqa.selenium.By.id("battleframetext"));
            } catch (Exception ex) {
                content = driver.findElement(org.openqa.selenium.By.cssSelector(".wander_info, .center, #content"));
            }
            String text = content.getText().toLowerCase();
            // In PBA, common special types are shiny, metallic, mystic, dark, shadow
            return java.util.regex.Pattern.compile("\\b(shiny|metallic|mystic|dark|shadow)\\b").matcher(text).find();
        } catch (Exception e) {
            return false;
        }
    }

    public void initiateBattle() {
        WebElement btn = wait.until(ExpectedConditions.elementToBeClickable(battleButton));
        
        // Scroll the button into view so it isn't blocked by top-banner ads
        try {
            ((org.openqa.selenium.JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({behavior: 'smooth', block: 'center'});", btn);
            Thread.sleep(500);
        } catch (Exception e) {}
        
        try {
            btn.click();
        } catch (Exception e) {
            // Ad might be overlaying the button, fallback to JS click
            ((org.openqa.selenium.JavascriptExecutor) driver).executeScript("arguments[0].click();", btn);
        }
        
        System.out.println("Clicked battle button. Waiting 3 seconds for page to load before bot-check...");
        try { Thread.sleep(3000); } catch (Exception e) {}
    }
    
    public void handleBotCheckIfPresent() {
        try {
            WebElement verifyBtn = null;
            try {
                org.openqa.selenium.support.ui.WebDriverWait shortWait = new org.openqa.selenium.support.ui.WebDriverWait(driver, java.time.Duration.ofSeconds(2));
                verifyBtn = shortWait.until(ExpectedConditions.presenceOfElementLocated(org.openqa.selenium.By.xpath("//input[translate(@value, 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz')='verify'] | //button[contains(translate(text(), 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz'), 'verify')] | //input[contains(translate(@value, 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz'), 'verify')]")));
            } catch (Exception waitEx) {
                // Not found within 2 seconds. Assume not present.
                return;
            }
            
            System.out.println("⚠️ BOT VERIFICATION DETECTED! ⚠️");
            
            // Try basic auto-click
            System.out.println("Attempting basic auto-click on CAPTCHA...");
            try {
                boolean clickedCaptcha = false;
                java.util.List<WebElement> cfIframes = driver.findElements(org.openqa.selenium.By.xpath("//iframe[contains(@src, 'cloudflare') or contains(@title, 'Cloudflare')]"));
                if (!cfIframes.isEmpty()) {
                    driver.switchTo().frame(cfIframes.get(0));
                    WebElement cb = new org.openqa.selenium.support.ui.WebDriverWait(driver, java.time.Duration.ofSeconds(3))
                        .until(ExpectedConditions.elementToBeClickable(org.openqa.selenium.By.cssSelector("label, input[type='checkbox']")));
                    cb.click();
                    driver.switchTo().defaultContent();
                    clickedCaptcha = true;
                } else {
                    java.util.List<WebElement> rcIframes = driver.findElements(org.openqa.selenium.By.xpath("//iframe[contains(@src, 'recaptcha') or contains(@title, 'reCAPTCHA')]"));
                    if (!rcIframes.isEmpty()) {
                        driver.switchTo().frame(rcIframes.get(0));
                        WebElement cb = new org.openqa.selenium.support.ui.WebDriverWait(driver, java.time.Duration.ofSeconds(3))
                            .until(ExpectedConditions.elementToBeClickable(org.openqa.selenium.By.cssSelector(".recaptcha-checkbox-border")));
                        cb.click();
                        driver.switchTo().defaultContent();
                        clickedCaptcha = true;
                    }
                }
                
                if (clickedCaptcha) {
                    System.out.println("Clicked CAPTCHA checkbox! Waiting a moment for it to process...");
                    try { Thread.sleep(3000); } catch(Exception ex) {}
                    if (verifyBtn.isDisplayed()) {
                        System.out.println("Clicking 'Verify' button...");
                        verifyBtn.click();
                        try { Thread.sleep(2000); } catch(Exception ex) {}
                    }
                } else {
                    System.out.println("No standard CAPTCHA iframe found. Could not auto-click.");
                }
            } catch (Exception e) {
                driver.switchTo().defaultContent();
                System.out.println("Auto-click attempt failed or was denied: " + e.getMessage());
            }

            try {
                // If the verify button is already gone (success), this will pass immediately.
                // Otherwise, it falls back to waiting for the user to solve it manually.
                org.openqa.selenium.support.ui.WebDriverWait shortCheck = new org.openqa.selenium.support.ui.WebDriverWait(driver, java.time.Duration.ofSeconds(2));
                shortCheck.until(ExpectedConditions.invisibilityOf(verifyBtn));
                System.out.println("CAPTCHA auto-solved successfully!");
            } catch (Exception notSolvedEx) {
                System.out.println("CAPTCHA requires manual intervention (image grid appeared).");
                System.out.println("Waiting up to 30 seconds for manual CAPTCHA solve...");
                try {
                    org.openqa.selenium.support.ui.WebDriverWait manualCheck = new org.openqa.selenium.support.ui.WebDriverWait(driver, java.time.Duration.ofSeconds(30));
                    manualCheck.until(ExpectedConditions.invisibilityOf(verifyBtn));
                    System.out.println("CAPTCHA manually solved successfully! Resuming...");
                } catch (Exception manualEx) {
                    System.out.println("⚠️ CLOUDFLARE CAPTCHA NOT SOLVED IN TIME! EXITING RUN! ⚠️");
                    throw new RuntimeException("CLOUDFLARE CAPTCHA DETECTED");
                }
            }
            try { Thread.sleep(2000); } catch(Exception e) {}
            
        } catch (Exception e) {
            if (e.getMessage() != null && e.getMessage().contains("CLOUDFLARE CAPTCHA DETECTED")) {
                throw new RuntimeException("CLOUDFLARE CAPTCHA DETECTED"); // Rethrow to fail the test!
            }
            // Not present or error, ignore
        }
    }
}
