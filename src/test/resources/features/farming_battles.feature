Feature: Trainer Farming
  As a Pokemon Battle Arena player
  I want to automatically farm the highest paying trainer on the current trainer page
  So that I can maximize my PokeMoney earnings.

  Scenario: Identify and farm the most profitable trainer
    Given I am on the home page
    When I login with secure credentials
    And I navigate to the trainers list
    When I identify and farm the highest paying trainer
