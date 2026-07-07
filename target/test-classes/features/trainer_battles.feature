@trainer
Feature: Trainer Battles Automation

  Scenario: Battle trainers appropriate for my team level
    Given I log in to Pokemon Battle Arena
    And I load the team "battleteam1" under Trainer HQ
    And I navigate to the trainers list
    When I battle appropriate trainers for 399 minutes
