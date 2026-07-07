@hunt
Feature: Wild Pokemon Hunting Automation

  Scenario: Hunt on the map for a specified duration and battle with the lowest attack
    Given I log in to Pokemon Battle Arena
    And I load the team "huntingteam3" under Trainer HQ
    And I setup pokemon team slots 5 and 6 with specific level pokemons
    And I navigate to the map
    When I hunt on the map for 500 minutes
    Then I should engage any wild Pokemon using the weakest attack available
