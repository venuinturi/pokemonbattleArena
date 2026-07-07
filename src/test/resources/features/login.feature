Feature: Login Functionality

  Scenario: Successful Login with secure credentials
    Given I am on the home page
    When I login with secure credentials
    Then I should be logged in successfully
