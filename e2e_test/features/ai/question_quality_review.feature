@usingMockedOpenAiService
Feature: Question generation by AI using custom model 
  As a developer, I want to use AI and specify a custom model to generate review questions based on my note and its context.
  So that I can review the quality of the custom model by viewing the generated questions.

  Background:
    Given OpenAI by default returns this question from now:
      | question                                            | correct_choice | incorrect_choice_1 | incorrect_choice_2 |
      | What is the most common scuba diving certification? | Rescue Diver   | Divemaster         | Open Water Diver   |

  Scenario: I should be able to use a custom model to generate question
    Given I've logged in as "developer"
    And there are some notes for the current user:
      | topic        | details                                          |
      | Scuba Diving | The most common certification is Rescue Diver.   |
    When I ask to generate a question for note "Scuba Diving" using custom model "gpt-4"
    Then I should be asked "What is the most common scuba diving certification?"

  @ignore
  Scenario: I should not be able to use a custom model to generate question as a learner
    Given I've logged in as an existing user
    And there are some notes for the current user:
      | topic        | details                                          |
      | Scuba Diving | The most common certification is Rescue Diver.   |
    When I ask to generate a question for note "Scuba Diving"
    Then I should not be able to see any input for custom model
