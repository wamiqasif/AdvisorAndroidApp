# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Overview

Appium Mobile Automation Framework for the ValueResearch Advisor Android app (`com.valueresearch.advisor`). Stack: Java 17, TestNG 7.10, Appium Java Client 9.3 + UiAutomator2, Selenium 4.20, Maven, ExtentReports 5.1.

The app is Flutter but is automated via **UiAutomator2** (not the Flutter driver). `content-desc` (accessibility ID) is the primary locator strategy because Flutter exposes semantic labels through the Android accessibility tree.

## Commands

```bash
mvn clean compile                                              # build only
mvn clean test                                                 # run all tests via testng.xml
mvn clean test -Dtest=LoginTest                                # run single test class
mvn clean test -Dtest=LoginTest#verifySuccessfulLogin          # run single test method
mvn clean test -DthreadCount=4                                 # override parallel thread count
```

Appium server must be running before any test execution: `appium` (default `http://127.0.0.1:4723`).

Reports: `reports/ExtentReport.html`. Failure screenshots: `reports/screenshots/`.

`testng.xml` runs `LoginTest`, `PortfolioPlannerTest`, and `PortfolioPlannerInvestorTest` in parallel (thread-count=2, emulator). `PortfolioTest`, `CartingTest`, and `InvestorAccountTest` exist but are not wired into the default suite — add them to `testng.xml` to run. `TaxCalculatorPage` exists as a page object but has no test class yet.

## Architecture

```
tests/  →  pages/  →  base/ + driver/ + utils/  →  config.properties
```

### Two Driver Lifecycle Modes

Every test class extends `BaseTest` and chooses one of two modes by overriding `shouldManageDriverPerMethod()`:

**Per-method mode** (`return true` — the default, used by `LoginTest`):
- `@BeforeClass`: no-ops (skipped by `BaseTest.beforeClass()`)
- `@BeforeMethod` (`setUp`): creates Extent node → ensures driver alive (init if dead) → `ensureAppIsRunning()` → `ensurePinScreenReady()` (because of `@RequiresPinScreen` on `LoginTest`) → `recoverAppState()`
- `@AfterMethod` (`tearDown`): logs result + screenshot → `DriverFactory.quitDriver()`

**Persistent session mode** (`return false` — all other test classes):
- `@BeforeClass` (`BaseTest.beforeClass()`): calls `initializeDriver()`, then `onClassReady()` — subclasses override `onClassReady()` to initialize page objects and optionally do one-time setup
- `@BeforeMethod` (`setUp`): creates Extent node → ensures driver alive (recreates + re-runs `onClassReady()` if session died) → `ensureAppIsRunning()` → `ensureDashboardReady()` → `recoverAppState(method)` — subclasses override `recoverAppState()` to navigate to their test-specific starting screen
- `@AfterMethod` (`tearDown`): logs result + screenshot → `safelyRecoverHomeState()` (presses back up to 4× to unwind Flutter nav stack, then taps Portfolio tab) — driver is **never** quit
- `@AfterClass` (`BaseTest.afterClass()`): not present — driver is quit in `@AfterSuite`

**Critical rule**: `ExtentManager.createTest()` is always the **first statement** in `setUp()`. `getExtentTest()` must never be called from `@BeforeClass`, `@AfterClass`, or `@AfterSuite` — the Extent node does not exist at those phases.

### Persistent Session `onClassReady()` Pattern

All persistent-session classes override `onClassReady()` to initialize page objects. If the class also needs one-time navigation (e.g., discovering investor names), that goes here too. `onClassReady()` is also called by `setUp()` if the driver had to be recreated mid-suite.

```java
@Override
protected void onClassReady() {
    plannerPage = new PortfolioPlannerPage(getDriver());
    // optional one-time navigation, e.g. plannerPage.navigateToHubAndOpenPlanner()
}
```

`recoverAppState(Method)` is called every `@BeforeMethod` and must navigate to the screen the test expects to start from:

```java
@Override
protected void recoverAppState(Method method) {
    plannerPage.navigateToHubAndOpenPlanner();
    Assert.assertTrue(plannerPage.isSelectInvestorScreenDisplayed()
            || plannerPage.isPlannerOptionsScreenDisplayed(), "...");
}
```

### `DashboardPage.recoverToDashboard()`

Used by `safelyRecoverHomeState()` (post-test) and `ensureDashboardReady()` (pre-test). Flutter hides the bottom navigation bar on deep screens (fund detail, SIP form, payment, OTP, cart). The method handles this with a back-press loop:

1. Already on dashboard → return immediately
2. Bottom nav visible but wrong tab → tap Portfolio tab
3. Bottom nav hidden (deep Flutter screen) → press Android back up to 4× (600 ms apart) until bottom nav reappears, then tap Portfolio tab

### DriverFactory

`ThreadLocal<AndroidDriver>` — one driver per thread, no locking needed. Key points:
- `isDriverAlive()` pings `driver.getCurrentPackage()` to verify the Appium server is actually responsive (a null `SessionId` check alone is insufficient for crashed sessions)
- `AtomicInteger portCounter` allocates sequential `systemPort` values starting from `config.systemPortBase` (8200) to prevent collisions in parallel runs
- `initDriver()` is idempotent — safe to call if a healthy session already exists

### BasePage

All pages extend `BasePage`. It uses `FluentWait` (not `WebDriverWait`) configured with `explicitWaitSeconds` (15 s default) and 250 ms polling, ignoring `NoSuchElementException` and `StaleElementReferenceException`. Key utilities:

- `safeClick(By)` — waits for clickable, falls back to visible on failure
- `safeSendKeys(By, String)` — waits for visible, clears, types, hides keyboard
- `isDisplayed(By)` — instant check via `findElements`, never throws
- `isDisplayed(By, int seconds)` — polls for up to N seconds
- `isAnyDisplayed(By...)` — returns true if any locator has matching elements
- Pages do **not** use `@AndroidFindBy` + `PageFactory`; direct `By` locators are preferred

### Locator Strategy (Flutter via UiAutomator2)

Priority order:
1. `AppiumBy.accessibilityId("exact-content-desc")` — preferred
2. `AppiumBy.androidUIAutomator("new UiSelector().description(\"exact\")")` — when `accessibilityId` matches multiple elements
3. `AppiumBy.androidUIAutomator("new UiSelector().descriptionContains(\"partial\")")` — for dynamic text
4. XPath — last resort only; never use positional XPath

Avoid `instance(n)` positional selectors unless there is no stable alternative.

### Login Flow

The app uses PIN-only login (after first-time OTP setup). PIN is `1454` (from `config.properties`). The PIN screen auto-submits after 4 digits — there is no submit button. `noReset=true` in capabilities preserves the PIN setup across sessions. Never clear app data or call logout — this destroys the PIN setup and forces OTP re-registration.

`LoginTest` uses `@RequiresPinScreen` (class-level annotation) so `setUp()` calls `ensurePinScreenReady()` instead of `ensureDashboardReady()`. Per-method mode means the driver is terminated and restarted for each test, guaranteeing a fresh PIN screen.

### Test Class Conventions

| Class | Mode | `onClassReady()` | `recoverAppState()` |
|---|---|---|---|
| `LoginTest` | per-method + `@RequiresPinScreen` | — | — |
| `PortfolioPlannerTest` | persistent | init page object | navigate to Hub → Planner landing |
| `PortfolioPlannerInvestorTest` | persistent | init page object + navigate + discover investors | navigate to Hub → Planner landing |
| `CartingTest` | persistent | init page objects | ensure planner or cart/OTP screen ready |
| `PortfolioTest` | persistent | init page object | navigate to Portfolio tab, assert visible |
| `InvestorAccountTest` | persistent | init page + dashboard objects | `recoverToDashboard()` → navigate to Basic Details |

`CartingTest` has a multi-step `ensurePlannerReady()` in `recoverAppState()` that can navigate through investor selection → planner options → show plan if the app is not already at a known mid-flow screen (planner, cart, or OTP).

### ExtentManager

Singleton `ExtentReports` (one HTML file) + `ThreadLocal<ExtentTest>` (one node per thread). `createTest()` in `@BeforeMethod`, `removeTest()` in `@AfterMethod`, `flush()` in `@AfterSuite`. `TestListener` (registered in `testng.xml`) provides a secondary flush safety net.

### Configuration

`ConfigReader` is a double-checked-locking singleton. `executionType=emulator|real` selects the device profile (`emulator.*` vs `real.*` property keys). Key properties: `explicitWaitSeconds=15`, `newCommandTimeout=120`, `noReset=true`, `login.pin=1454`.
