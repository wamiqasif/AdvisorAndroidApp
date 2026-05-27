# Repository Guidelines

## Project Structure & Module Organization
This repository is a Java 17 mobile automation framework using Appium, TestNG, and Maven. Test code lives in `src/test/java`: `tests` contains TestNG classes, `pages` holds page objects, `driver` manages Appium driver setup, `base` contains shared fixtures and listeners, and `utils` covers configuration, reporting, and screenshots. Runtime settings live in `src/test/resources/config.properties`. Build output goes to `target/`; reports and screenshots are generated under `reports/` and `test-output/` and should stay uncommitted.

## Build, Test, and Development Commands
Run commands from the repository root. `mvn clean` removes compiled classes and prior output. `mvn test` runs the default Surefire suite from `testng.xml`. `mvn -Dtest=tests.LoginTest test` runs one test class. `mvn -Dthreadcount=2 test` is the preferred way to tune parallel execution when adjusting suite concurrency and device ports. Start an Appium server at `http://127.0.0.1:4723` before running tests.

## Coding Style & Naming Conventions
Follow the existing Java style: 4-space indentation, same-line braces, and descriptive method names such as `verifySuccessfulLoginWithCorrectPin`. Keep package names lowercase and class names in PascalCase, for example `DashboardPage` or `DriverFactory`. Prefer action-oriented page-object methods like `enterPin()` or `isDashboardDisplayed()`. Use SLF4J for logging rather than ad hoc `System.out` output.

## Testing Guidelines
The framework uses TestNG with page objects and ExtentReports. Add new scenarios under `src/test/java/tests` and keep reusable UI behavior in `src/test/java/pages`. Update `testng.xml` when a new class should run in the default suite; the current default run targets the emulator flow and `tests.LoginTest`. Preserve the `TC_*` traceability comment pattern and make sure failures leave enough context for `reports/ExtentReport.html` and screenshot attachments.

## Commit & Pull Request Guidelines
Git history is not available in this workspace snapshot, so use short imperative commit subjects such as `Add portfolio smoke test`. Keep each commit scoped to one change. Pull requests should describe the affected flow, note the target device mode (`emulator` or `real`), call out any `config.properties` or suite changes, and include report screenshots when UI behavior changes.

## Security & Configuration Tips
`src/test/resources/config.properties` contains environment-specific values such as device identifiers and test credentials. Do not hardcode new secrets in test classes or page objects. Prefer local overrides or secure environment management before sharing branches or reports.

## AI / Codex Instructions (IMPORTANT)

### Locator Strategy

* Prefer resource-id
* Then accessibility id
* Then UiAutomator
* Avoid XPath unless absolutely necessary
* Do not use index-based locators

### Stability Rules

* Do not use Thread.sleep
* Always use explicit waits (visibilityOfElementLocated)
* Add retry for click/type actions
* Avoid hardcoded waits
* Do not assume UI state

### Navigation Rules

* Always verify current screen before action
* Use recovery navigation in @BeforeMethod
* Do not assume tab stays on same screen
* Handle back navigation explicitly

### Page Object Rules

* Use By locators (no raw WebElement in tests)
* Add screen detection method using multiple elements
* Add navigation helper methods
* Avoid duplicate waits in tests

### Test Design Rules

* Tests must be independent
* No dependency on execution order
* Avoid redundant test cases
* Prefer stability over coverage
* Do not generate flaky tests

### Do NOT:

* Add Thread.sleep
* Use absolute XPath
* Rewrite BasePage or DriverFactory without reason
* Generate unnecessary or duplicate test cases
* Hardcode values like usernames or dynamic text

### Output Expectations

* Clean Page Object classes
* Stable and minimal test cases
* Replace unstable locators with reason

### Mobile Stability Rules

* Prefer accessibility id/content-desc when resource-id unavailable
* Avoid exact match on dynamic values
* Composite content-desc elements should be parsed safely
* Use recovery navigation before each test
* Prefer stable business assertions over UI-only assertions
* Avoid brittle scroll validations

