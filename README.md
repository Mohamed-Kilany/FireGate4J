# FireGate4J

## BDD API Testing Guide

This guide explains how to write and use feature files with a Java-based SpecFlow clone for API testing. The system uses
custom step definitions, a shared `Context` object for state across steps, and utility services like `AsyncRestClient`,
`RegexGenerator`, `TypeConverter`, `PathExtractor`, and `SchemaValidator`.

## 🧠 Step Definitions Overview

Each step corresponds to a BDD keyword (`@Given`, `@When`, `@Then`) and enables powerful, composable API tests using
Gherkin syntax.

### 🔧 Setup Steps (`@Given`)

#### `Given set base url to {url}`

Sets the base URL for subsequent API requests.

```gherkin
Given set base url to https://api.example.com
```

#### `Given set endpoint to {endpoint}`

Sets the target endpoint (relative path) to hit.

```gherkin
Given set endpoint to /users/{userId}/posts
```

#### `Given add to headers`

Adds key-value pairs to the HTTP headers.

```gherkin
Given add to headers
| Authorization | Bearer {token} |
| Content-Type  | application/json |
```

Supports context variable interpolation (e.g., `{token}`).

#### `Given add to path parameters`

Used to replace placeholders in the endpoint.

```gherkin
Given add to path parameters
| userId | 42 |
```

#### `Given add to query parameters`

Adds query string parameters.

```gherkin
Given add to query parameters
| sort | desc |
```

#### `Given add to body`

Adds fields to the request body.

```gherkin
Given add to body
| title   | Hello World |
| content | Lorem ipsum |
```

#### `Given generate random values`

Generates dynamic random values using regex.

```gherkin
Given generate random values
| username:string | [a-z]{8} |
| id:integer       | \d{5}    |
```

Generated values are stored in the context and can be reused.

---

### 🚀 Execution Step (`@When`)

#### `When send a {method} request`

Sends the request using the collected info.

```gherkin
When send a POST request
```

Supported methods: `GET`, `POST`, `PUT`, `DELETE`, etc.

---

### ✅ Validation Steps (`@Then`)

#### `Then validate status code of {code}`

Validates the response status code.

```gherkin
Then validate status code of 200
```

#### `Then the response body should match schema: {schemaName}`

Validates the response body against a JSON schema file.

```gherkin
Then the response body should match schema: user_schema.json
```

> Schema files are expected to be in `src/test/resources/schemas/`

#### `Then extract values from response`

Extracts values from the response JSON body and stores them in context.

```gherkin
Then extract values from response
| $.user.id   | userId:integer |
| $.user.name | username      |
```

---

## 🔁 Parameterization & Context

### Context Resolution

All values wrapped in `{}` are automatically replaced by values from the context. For example:

```gherkin
Given add to headers
| Authorization | Bearer {authToken} |
```

This will resolve `{authToken}` from a value previously stored in the context.

### Type Conversion

Supports type annotations via the `key:type` format (e.g., `userId:integer`). Values are automatically type-converted
using `TypeConverter`.

---

## 💡 Best Practices

* Use consistent key casing (lowercase preferred).
* Reuse context variables to chain steps effectively.
* Validate responses early with schema checks and status code assertions.
* Modularize your scenarios for readability.

---

## 🗂 Directory Structure Example

```
project-root/
│
├── src/test/java/.../Steps.java
├── src/test/resources/schemas/
│   └── user_schema.json
├── features/
│   └── user.feature
```

## ✅ Sample Feature File

```gherkin
Feature: User API

  Scenario: Create and validate user
    Given set base url to https://api.example.com
    And set endpoint to /users
    And add to headers
      | Content-Type | application/json |
    And add to body
      | name  | John Doe         |
      | email | john@example.com |
    When send a POST request
    Then validate status code of 201
    And extract values from response
      | $.id | userId:integer |
```

---

## 🛠 Dependencies

Ensure the following helper components exist and are available via context:

* `Context`: State management
* `AsyncRestClient`: For making async HTTP requests
* `RegexGenerator`: For dynamic value generation
* `TypeConverter`: For value type conversion
* `SchemaValidator`: For JSON schema validation
* `PathExtractor`: For extracting fields from JSON response
