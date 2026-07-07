package com.pokemon.automation.stepdefinitions;

import com.pokemon.automation.driver.DriverManager;
import com.pokemon.automation.mapping.MapGraph;
import com.pokemon.automation.mapping.MapNode;
import com.pokemon.automation.pages.BattlePage;
import com.pokemon.automation.pages.MapNavigationPage;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.openqa.selenium.WebDriver;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;

public class MapExplorationSteps {

    private WebDriver driver = DriverManager.getDriver();
    private MapNavigationPage mapPage = new MapNavigationPage(driver);
    private BattlePage battlePage = new BattlePage(driver);
    
    private MapGraph graph = new MapGraph();
    private Set<Integer> visited = new HashSet<>();
    private Queue<Integer> frontier = new LinkedList<>();

    @Given("I initialize the map cartographer")
    public void iInitializeTheMapCartographer() {
        System.out.println("Initializing Map Cartographer...");
        // Start from a known map, e.g., Route 1 (Map 3) or Hollowbrook Town (Map 2)
        int startMap = 2; // Hollowbrook Town
        frontier.add(startMap);
        graph.addNode(new MapNode(startMap, "Hollowbrook Town", "https://pokemonbattlearena.net/members/wander.php?M=" + startMap));
    }

    @When("I explore all boundaries of known maps")
    public void iExploreAllBoundariesOfKnownMaps() {
        String[] directions = {"North", "East", "South", "West"};
        
        while (!frontier.isEmpty()) {
            int currentMapId = frontier.poll();
            if (visited.contains(currentMapId)) {
                continue;
            }
            
            System.out.println("Exploring Map ID: " + currentMapId);
            visited.add(currentMapId);
            
            for (String dir : directions) {
                // Navigate to the center of the current map
                mapPage.safeNavigate("https://pokemonbattlearena.net/members/wander.php?M=" + currentMapId);
                mapPage.scrollToMapArea();
                
                System.out.println("Walking " + dir + " on Map " + currentMapId);
                
                // Walk in this direction for a while (e.g., 40 steps to reach the edge and wiggle)
                boolean hitEdge = walkInDirection(dir, 40);
                
                if (hitEdge) {
                    int newMapId = mapPage.getMapIdFromUrl();
                    if (newMapId != -1 && newMapId != currentMapId) {
                        System.out.println("Discovered new map connection! " + currentMapId + " -> " + dir + " -> " + newMapId);
                        graph.recordConnection(currentMapId, dir, newMapId);
                        
                        if (!graph.hasNode(newMapId)) {
                            // We don't know the exact name without parsing, just use ID
                            graph.addNode(new MapNode(newMapId, "Map_" + newMapId, "https://pokemonbattlearena.net/members/wander.php?M=" + newMapId));
                            frontier.add(newMapId);
                        }
                    }
                }
            }
        }
    }

    private boolean walkInDirection(String direction, int steps) {
        int initialMapId = mapPage.getMapIdFromUrl();
        
        // Define perpendicular directions for wiggling
        String wiggle1 = direction.equals("North") || direction.equals("South") ? "East" : "West";
        String wiggle2 = direction.equals("North") || direction.equals("South") ? "West" : "East";
        
        int[] lastPos = mapPage.getPlayerLocation();
        
        for (int i = 1; i <= steps; i++) {
            mapPage.moveInDirection(direction);
            
            try { Thread.sleep(400); } catch (Exception e) {}
            
            if (mapPage.isPokemonEncountered()) {
                // Wild encounter detected, but we just ignore it and keep walking!
            }
            
            int currentUrlMapId = mapPage.getMapIdFromUrl();
            if (currentUrlMapId == -1) {
                System.out.println("Redirected away from map! Navigating back to continue.");
                mapPage.safeNavigate("https://pokemonbattlearena.net/members/wander.php?M=" + initialMapId);
                mapPage.scrollToMapArea();
                lastPos = mapPage.getPlayerLocation();
            } else if (currentUrlMapId != initialMapId) {
                return true; // Reached a new map!
            } else {
                // Still on the same map. Did our avatar move?
                int[] currentPos = mapPage.getPlayerLocation();
                if (lastPos != null && currentPos != null) {
                    if (lastPos[0] == currentPos[0] && lastPos[1] == currentPos[1]) {
                        // Avatar didn't move! Hit an obstacle. Wiggle!
                        System.out.println("Avatar is stuck at " + currentPos[0] + "," + currentPos[1] + ". Wiggling...");
                        mapPage.moveInDirection(wiggle1);
                        mapPage.moveInDirection(direction);
                        mapPage.moveInDirection(wiggle2);
                        mapPage.moveInDirection(wiggle2);
                        mapPage.moveInDirection(direction);
                        mapPage.moveInDirection(wiggle1); // reset to original line roughly
                        
                        try { Thread.sleep(800); } catch (Exception e) {}
                        currentPos = mapPage.getPlayerLocation(); // update pos after wiggle
                    }
                }
                lastPos = currentPos;
            }
        }
        return false;
    }

    @Then("the full map graph should be saved to file")
    public void theFullMapGraphShouldBeSavedToFile() {
        System.out.println("Exploration complete! Saving map graph...");
        graph.saveGraphToFile("target/map_graph.csv");
        System.out.println("Saved to target/map_graph.csv");
    }
}
