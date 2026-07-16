package com.pokemon.automation.pages;

import com.pokemon.automation.base.BasePage;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.support.FindBy;

import java.util.List;

public class TrainerListPage extends BasePage {

    @FindBy(css = ".trainer-row")
    private List<WebElement> trainerRows;

    public TrainerListPage(WebDriver driver) {
        super(driver);
    }

    public int getTrainerCount() {
        try {
            List<WebElement> trainers = driver.findElements(By.xpath("//table[contains(@class, 'table-striped')]//tbody/tr"));
            return trainers.size();
        } catch (Exception e) {
            return 0;
        }
    }

    public java.util.List<String> getAllGymLeaderCategoryValues() {
        java.util.List<String> gymLeaderValues = new java.util.ArrayList<>();
        try {
            JavascriptExecutor js = (JavascriptExecutor) driver;
            long length = (long) js.executeScript("return document.getElementById('dropdown').options.length;");
            
            for (long i = 0; i < length; i++) {
                String text = (String) js.executeScript("return document.getElementById('dropdown').options[" + i + "].text;");
                if (text != null && text.toLowerCase().contains("gym leader")) {
                    String val = (String) js.executeScript("return document.getElementById('dropdown').options[" + i + "].value;");
                    gymLeaderValues.add(val);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return gymLeaderValues;
    }

    public java.util.List<String> getAllConquestCategoryValues() {
        java.util.List<String> conquestValues = new java.util.ArrayList<>();
        try {
            JavascriptExecutor js = (JavascriptExecutor) driver;
            long length = (long) js.executeScript("return document.getElementById('dropdown').options.length;");
            
            for (long i = 0; i < length; i++) {
                String text = (String) js.executeScript("return document.getElementById('dropdown').options[" + i + "].text;");
                if (text != null && text.toLowerCase().contains("conquest")) {
                    String val = (String) js.executeScript("return document.getElementById('dropdown').options[" + i + "].value;");
                    conquestValues.add(val);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return conquestValues;
    }

    public void selectTrainerCategory(String value) {
        try {
            // 1. Try TomSelect visual interaction directly
            try {
                WebElement control = driver.findElement(By.id("dropdown-ts-control"));
                control.click();
                Thread.sleep(500); // Wait for options to render
                
                WebElement optionToClick = driver.findElement(By.xpath("//div[contains(@class, 'ts-dropdown')]//div[contains(@class, 'option') and (@data-value='" + value + "' or @data-selectable)]"));
                // Since data-value might not exactly match if the xpath is strict, let's find it by iterating:
                java.util.List<WebElement> options = driver.findElements(By.cssSelector("div.ts-dropdown div.option, div.ts-dropdown-content div.option"));
                boolean clicked = false;
                for (WebElement opt : options) {
                    if (value.equals(opt.getAttribute("data-value"))) {
                        opt.click();
                        clicked = true;
                        break;
                    }
                }
                
                if (!clicked) {
                    throw new Exception("Option not found in TomSelect");
                }
                
                Thread.sleep(1500); // Wait for potential auto-submit
                
                // If it didn't auto-submit, try to find a submit button in the form
                try {
                    WebElement submitBtn = driver.findElement(By.xpath("//form[@id='TrainersList']//input[@type='submit'] | //form[@id='TrainersList']//button[@type='submit']"));
                    submitBtn.click();
                    Thread.sleep(1500);
                } catch (Exception noSubmitBtn) {}
                
                return; // Successfully used TomSelect
            } catch (Exception tsEx) {
                System.out.println("TomSelect visual click failed, falling back to JS: " + tsEx.getMessage());
            }

            // 2. Fallback to JavaScript
            JavascriptExecutor js = (JavascriptExecutor) driver;
            js.executeScript("if(document.getElementById('dropdown')) { document.getElementById('dropdown').value = arguments[0]; }", value);
            js.executeScript("if(document.getElementById('TrainersList')) { document.getElementById('TrainersList').submit(); }");
            js.executeScript("if(document.querySelector('.tomselected') && document.getElementById('dropdown').tomselect) { " +
                             "  document.getElementById('dropdown').tomselect.setValue('" + value + "');" +
                             "}");
            Thread.sleep(1500);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String getNextTrainerCategoryValue(String currentVal) {
        try {
            java.util.List<WebElement> options = driver.findElements(By.cssSelector("div.ts-dropdown div.option, div.ts-dropdown-content div.option"));
            if (options.isEmpty()) {
                // Fallback to native select
                options = driver.findElements(By.cssSelector("select#dropdown option"));
            }
            
            if (options.isEmpty()) {
                System.out.println("Could not find any category options!");
                return null;
            }
            
            int nextIndex = 1; // Start at 1 to skip "Select a Category" (index 0)
            if (currentVal != null) {
                for (int i = 0; i < options.size(); i++) {
                    String val = options.get(i).getAttribute("data-value");
                    if (val == null) val = options.get(i).getAttribute("value");
                    
                    if (val != null && val.equals(currentVal)) {
                        nextIndex = i + 1;
                        break;
                    }
                }
            }
            
            if (nextIndex >= options.size()) {
                nextIndex = 1; // Loop back around to the first actual category
            }
            
            String nextVal = options.get(nextIndex).getAttribute("data-value");
            if (nextVal == null) nextVal = options.get(nextIndex).getAttribute("value");
            return nextVal;
        } catch (Exception e) {
            System.out.println("Error calculating next category: " + e.getMessage());
            return null;
        }
    }

    public String battleSpecificTrainer(int trainerIndex, int battleType) {
        try {
            List<WebElement> rows = driver.findElements(By.xpath("//tr[.//button[contains(@onclick, 'BT=1')]]"));
            if (trainerIndex < rows.size()) {
                WebElement row = rows.get(trainerIndex);
                String trainerName = "Unknown Trainer";
                try {
                    trainerName = row.findElement(By.xpath(".//td[1]")).getText().trim();
                } catch(Exception e) {}
                
                List<WebElement> btns = row.findElements(By.xpath(".//button[contains(@onclick, 'BT=" + battleType + "')]"));
                if (!btns.isEmpty()) {
                    ((org.openqa.selenium.JavascriptExecutor) driver).executeScript("arguments[0].click();", btns.get(0));
                    System.out.println("Clicked " + battleType + "v" + battleType + " button against " + trainerName);
                    try { Thread.sleep(2000); } catch (Exception e) {}
                    return trainerName;
                } else {
                    System.out.println(trainerName + " does not have a " + battleType + "v" + battleType + " option.");
                }
            }
        } catch (Exception e) {
            System.out.println("Error while finding trainer battle link: " + e.getMessage());
        }
        return null;
    }

    public java.util.List<String> getAllTrainersOnPage() {
        java.util.List<String> trainers = new java.util.ArrayList<>();
        try {
            List<WebElement> rows = driver.findElements(By.xpath("//tr[.//button[contains(@onclick, 'BT=1')]]"));
            for (WebElement row : rows) {
                try {
                    String name = row.findElement(By.xpath(".//td[1]")).getText().trim();
                    if (!name.isEmpty()) {
                        trainers.add(name);
                    }
                } catch (Exception e) {}
            }
        } catch (Exception e) {
            System.out.println("Error reading trainers: " + e.getMessage());
        }
        return trainers;
    }

    public boolean battleSpecificTrainerByName(String trainerName, int battleType) {
        try {
            List<WebElement> rows = driver.findElements(By.xpath("//tr[.//button[contains(@onclick, 'BT=1')]]"));
            for (WebElement row : rows) {
                try {
                    String name = row.findElement(By.xpath(".//td[1]")).getText().trim();
                    if (name.equalsIgnoreCase(trainerName)) {
                        List<WebElement> btns = row.findElements(By.xpath(".//button[contains(@onclick, 'BT=" + battleType + "')]"));
                        if (!btns.isEmpty()) {
                            ((org.openqa.selenium.JavascriptExecutor) driver).executeScript("arguments[0].click();", btns.get(0));
                            System.out.println("Clicked " + battleType + "v" + battleType + " button against " + trainerName);
                            try { Thread.sleep(2000); } catch (Exception e) {}
                            return true;
                        }
                    }
                } catch(Exception e) {}
            }
        } catch (Exception e) {
            System.out.println("Error while finding trainer battle link by name: " + e.getMessage());
        }
        return false;
    }
}
