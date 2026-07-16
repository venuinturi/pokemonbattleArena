Feature: Trainer Farming
  As a Pokemon Battle Arena player
  I want to automatically farm the highest paying trainer on the current trainer page
  So that I can maximize my PokeMoney earnings.

  Scenario: Infinite loop farming of the most profitable trainer
    Given I log into Pokemon Battle Arena
    And I navigate to the trainers list
    And I determine the most profitable trainer on the page
    When I farm the most profitable trainer in an infinite loop
