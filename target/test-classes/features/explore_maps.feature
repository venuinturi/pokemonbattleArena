Feature: Map Exploration and Pathing
  As a player
  I want the bot to systematically explore the game world
  So that I can map all locations and discover unlocked paths

  Scenario: Systematically explore known maps to build a map graph
    Given I am on the home page
    When I login with secure credentials
    Then I should be logged in successfully
    Given I initialize the map cartographer
    When I explore all boundaries of known maps
    Then the full map graph should be saved to file
