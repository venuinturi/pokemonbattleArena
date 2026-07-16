Feature: Trainer Farming
  As a Pokemon Battle Arena player
  I want to automatically farm the highest paying trainer on the current trainer page
  So that I can maximize my PokeMoney earnings.

  Scenario: Identify and farm the most profitable Conquest trainer
    Given I am on the home page
    When I login with secure credentials
    And I navigate to the trainers list
    And I battle all Conquest trainers from the dropdown
    And I determine the absolute most profitable known trainer
    When I farm the most profitable trainer in an infinite loop
