Feature: User API Validation

  Background:
    Given set base url to https://reqres.in/api/users
    And add to headers
      | x-api-key    | reqres-free-v1  |
      | Content-Type | application/json |

#  Scenario: Get List
#    And set endpoint to /
#    When send a get request
#    Then validate status code of 200
#    And extract values from response
#      | $.data.id | id:list<string> |

#  Scenario: Get by id
#    And set endpoint to /{id}
#    And add to path parameters
#      | id | 1 |
#    When send a GET request
#    Then validate status code of 200
#
  Scenario: Create
    And set endpoint to /
    And generate random values
      | name | [a-z0-9]{10} |
      | job  | [a-z0-9]{10} |
    And add to body
      | name | {name} |
      | job  | {job}  |
    When send a POST request
    Then validate status code of 201