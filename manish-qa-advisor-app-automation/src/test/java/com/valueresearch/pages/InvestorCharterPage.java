package com.valueresearch.pages;

import com.valueresearch.utils.ReportLogger;
import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;
import org.openqa.selenium.By;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.Rectangle;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.Keys;
import org.openqa.selenium.interactions.PointerInput;
import org.openqa.selenium.interactions.Sequence;

import java.time.Duration;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class InvestorCharterPage {

    private static final String INVESTOR_CHARTER_TILE_EXACT_DESC = "View the investor charter";
    private static final String INVESTOR_CHARTER_TITLE = "Investor Charter in respect of Investment Adviser";

    private final AndroidDriver driver;
    private String advisorAppPackage = "";
    private InvestorCharterLinkSnapshot latestInvestorCharterLinkSnapshot = null;

    public InvestorCharterPage(AndroidDriver driver) {
        this.driver = driver;
    }

    // =========================================================
    // PUBLIC STEP METHODS USED BY TEST CLASS
    // =========================================================

    public void captureAdvisorAppPackageForInvestorCharter() {
        advisorAppPackage = getCurrentPackageSafely();
        ReportLogger.pass("Advisor app package captured: " + advisorAppPackage);
    }

    public void ensureAdvisorAppLoggedInForInvestorCharter() {
        ReportLogger.step("Checking Advisor app login/session state");

        waitForAppToBeInteractive();

        if (isMainAppLoaded()) {
            ReportLogger.pass("Advisor app session is already active");
            return;
        }

        if (isPinScreenVisible()) {
            ReportLogger.step("PIN screen detected. Entering Advisor PIN");
            enterAdvisorPin();
            waitForMainAppAfterPin();
            ReportLogger.pass("Advisor app login/session confirmed after PIN");
            return;
        }

        throw new AssertionError("Unable to confirm Advisor app login/session state"
                + " | visibleValues=" + collectVisibleStrings());
    }

    public void openHubFromBottomNavigationForInvestorCharter() {
        ReportLogger.step("Opening Hub from bottom navigation");

        waitForAppToBeInteractive();

        if (isVisibleByAnyText("Hub") && isLikelyOnHubPage()) {
            ReportLogger.pass("Hub page is already visible");
            return;
        }

        WebElement hubBottomTab = findVisibleTextElementNearBottom("Hub");

        if (hubBottomTab != null) {
            tapElementCenter(hubBottomTab);
            sleep(1800);
            ReportLogger.pass("Tapped Hub bottom navigation tab");
        } else if (tapAnyVisibleText("Hub")) {
            sleep(1800);
            ReportLogger.pass("Tapped Hub tab by visible text");
        } else {
            pressBackSilently();
            sleep(1000);

            hubBottomTab = findVisibleTextElementNearBottom("Hub");
            if (hubBottomTab != null) {
                tapElementCenter(hubBottomTab);
                sleep(1800);
                ReportLogger.pass("Tapped Hub bottom navigation tab after back recovery");
            } else {
                throw new AssertionError("Unable to find/tap Hub tab"
                        + " | visibleValues=" + collectVisibleStrings());
            }
        }

        waitUntilTextVisible("Hub", 10);
        ReportLogger.pass("Hub page opened successfully");
    }

    public void scrollToInvestorCharterInHubForInvestorCharter() {
        ReportLogger.step("Scrolling Hub page to Investor Charter option");

        for (int attempt = 0; attempt <= 12; attempt++) {
            if (isVisible(investorCharterExactLocator())
                    || isVisible(investorCharterDescriptionContainsLocator())
                    || isVisible(investorCharterLowerDescriptionContainsLocator())
                    || isVisible(investorCharterTextContainsLocator())
                    || isVisibleByAnyText("Investor Charter")) {
                ReportLogger.pass("Investor Charter option is visible in Hub");
                return;
            }

            if (attempt > 0 && isVisibleByAnyText("More")) {
                ReportLogger.debug("More section is visible. Performing small swipe for Investor Charter.");
                smallSwipeUp();
            } else {
                swipeUp();
            }

            sleep(900);
        }

        throw new AssertionError("Investor Charter option not visible inside Hub after scrolling"
                + " | visibleValues=" + collectVisibleStrings());
    }

    public void tapInvestorCharterForInvestorCharter() {
        ReportLogger.step("Tapping Investor Charter option");

        if (tapIfVisible(investorCharterExactLocator(), "Investor Charter using exact accessibilityId: " + INVESTOR_CHARTER_TILE_EXACT_DESC)) {
            sleep(2500);
            ReportLogger.pass("Tapped Investor Charter option using exact accessibilityId: " + INVESTOR_CHARTER_TILE_EXACT_DESC);
            return;
        }

        if (tapIfVisible(investorCharterDescriptionContainsLocator(), "Investor Charter using descriptionContains")) {
            sleep(2500);
            ReportLogger.pass("Tapped Investor Charter option using descriptionContains");
            return;
        }

        if (tapIfVisible(investorCharterLowerDescriptionContainsLocator(), "Investor Charter using View the investor descriptionContains")) {
            sleep(2500);
            ReportLogger.pass("Tapped Investor Charter option using View the investor descriptionContains");
            return;
        }

        if (tapIfVisible(investorCharterTextContainsLocator(), "Investor Charter using textContains")) {
            sleep(2500);
            ReportLogger.pass("Tapped Investor Charter option using textContains");
            return;
        }

        WebElement investorCharterElement = findVisibleTextElement("Investor Charter");

        if (investorCharterElement != null) {
            tapElementCenter(investorCharterElement);
            sleep(2500);
            ReportLogger.pass("Tapped Investor Charter option using visible text fallback");
            return;
        }

        if (tapAnyVisibleText("Investor Charter")) {
            sleep(2500);
            ReportLogger.pass("Tapped Investor Charter option by fallback");
            return;
        }

        throw new AssertionError("Unable to tap Investor Charter"
                + " | visibleValues=" + collectVisibleStrings());
    }

    public void waitForInvestorCharterPageForInvestorCharter() {
        ReportLogger.step("Waiting for Investor Charter page to load");

        for (int i = 1; i <= 25; i++) {
            if (isInvestorCharterPageVisible()) {
                ReportLogger.pass("Investor Charter page loaded");
                return;
            }

            sleep(800);
        }

        throw new AssertionError("Investor Charter page did not load"
                + " | visibleValues=" + collectVisibleStrings());
    }

    public void validateInvestorCharterPageContentForInvestorCharter() {
        ReportLogger.step("Validating Investor Charter page title and top content");

        waitForInvestorCharterPageForInvestorCharter();

        InvestorCharterSnapshot snapshot = captureInvestorCharterSnapshot(false);

        if (!snapshot.hasTopContent()) {
            throw new AssertionError("Investor Charter top content validation failed"
                    + " | hasTitle=" + snapshot.hasTitle
                    + " | hasVisionMission=" + snapshot.hasVisionMission
                    + " | hasInvestmentAdviser=" + snapshot.hasInvestmentAdviser
                    + " | hasInvestors=" + snapshot.hasInvestors
                    + " | source=" + snapshot.source);
        }

        ReportLogger.pass("Investor Charter title is visible");
        ReportLogger.pass("Investor Charter Vision/Mission content is visible");
        ReportLogger.pass("Investor Charter Investment Adviser and investors markers are visible");
    }

    public void validateInvestorCharterScrollableContentForInvestorCharter() {
        ReportLogger.step("Validating Investor Charter scrollable body content dynamically");

        waitForInvestorCharterPageForInvestorCharter();

        InvestorCharterSnapshot combinedSnapshot = captureInvestorCharterSnapshot(false);
        String initialSource = combinedSnapshot.source;
        boolean sourceChangedAfterScroll = false;

        for (int attempt = 1; attempt <= 6; attempt++) {
            if (combinedSnapshot.hasScrollableBodyContent()) {
                break;
            }

            ReportLogger.step("Investor Charter lower content not fully visible yet. Scrolling. Attempt: " + attempt);
            smallSwipeUp();
            sleep(900);

            InvestorCharterSnapshot nextSnapshot = captureInvestorCharterSnapshot(false);
            sourceChangedAfterScroll = sourceChangedAfterScroll || !normalizeForMatching(initialSource).equals(normalizeForMatching(nextSnapshot.source));
            combinedSnapshot = combinedSnapshot.merge(nextSnapshot);
        }

        if (!combinedSnapshot.hasScrollableBodyContent()) {
            throw new AssertionError("Investor Charter scrollable body content validation failed"
                    + " | hasBusinessTransacted=" + combinedSnapshot.hasBusinessTransacted
                    + " | hasAgreementOrFee=" + combinedSnapshot.hasAgreementOrFee
                    + " | hasRiskOrSuitability=" + combinedSnapshot.hasRiskOrSuitability
                    + " | hasComplaintOrGrievance=" + combinedSnapshot.hasComplaintOrGrievance
                    + " | hasContactOrDisclosure=" + combinedSnapshot.hasContactOrDisclosure
                    + " | sourceChangedAfterScroll=" + sourceChangedAfterScroll
                    + " | source=" + combinedSnapshot.source);
        }

        ReportLogger.pass("Investor Charter scrollable body validated dynamically"
                + " | sourceChangedAfterScroll=" + sourceChangedAfterScroll
                + " | businessTransacted=" + combinedSnapshot.hasBusinessTransacted
                + " | agreementOrFee=" + combinedSnapshot.hasAgreementOrFee
                + " | riskOrSuitability=" + combinedSnapshot.hasRiskOrSuitability
                + " | complaintOrGrievance=" + combinedSnapshot.hasComplaintOrGrievance
                + " | contactOrDisclosure=" + combinedSnapshot.hasContactOrDisclosure);
    }


    public void validateInvestorCharterLinksPresentForInvestorCharter() {
        ReportLogger.step("Strictly scrolling Investor Charter page until both real link markers are found during full-page scroll");

        waitForInvestorCharterPageForInvestorCharter();

        InvestorCharterLinkSnapshot combinedSnapshot = captureInvestorCharterLinkSnapshot();
        boolean sourceChangedAfterScroll = false;
        String initialSource = combinedSnapshot.source;

        // Requirement: both real Investor Charter links must be reached during page scroll.
        // They do not need to be on the same viewport at the exact same moment, but the test
        // must scroll through the document until it has seen BOTH:
        // 1) SEBI registered Investment Advisers list link
        // 2) SCORES portal URL/link
        for (int attempt = 1; attempt <= 24; attempt++) {
            if (combinedSnapshot.hasBothRequiredInvestorCharterLinks()) {
                break;
            }

            ReportLogger.step("Investor Charter both links not found yet during full-page scroll. Scrolling. Attempt: "
                    + attempt
                    + " | sebiListLink=" + combinedSnapshot.hasSebiListMarker
                    + " | scoresUrl=" + combinedSnapshot.hasScoresUrl);

            if (attempt % 4 == 0) {
                longSwipeUp();
            } else if (attempt % 2 == 0) {
                swipeUp();
            } else {
                controlledDocumentSwipeUp();
            }
            sleep(750);

            InvestorCharterLinkSnapshot nextSnapshot = captureInvestorCharterLinkSnapshot();
            sourceChangedAfterScroll = sourceChangedAfterScroll
                    || !normalizeForMatching(initialSource).equals(normalizeForMatching(nextSnapshot.source));
            combinedSnapshot = combinedSnapshot.merge(nextSnapshot);
        }

        latestInvestorCharterLinkSnapshot = combinedSnapshot;

        if (!combinedSnapshot.hasBothRequiredInvestorCharterLinks()) {
            throw new AssertionError("Investor Charter both-link visibility validation failed"
                    + " | required=SEBI registered Investment Advisers list link AND SCORES URL/link"
                    + " | sebiListLink=" + combinedSnapshot.hasSebiListMarker
                    + " | scoresUrl=" + combinedSnapshot.hasScoresUrl
                    + " | scoresMarker=" + combinedSnapshot.hasScoresMarker
                    + " | sourceChangedAfterScroll=" + sourceChangedAfterScroll
                    + " | source=" + combinedSnapshot.source);
        }

        ReportLogger.pass("Investor Charter both required link markers found during full-page scroll"
                + " | sebiListLink=" + combinedSnapshot.hasSebiListMarker
                + " | scoresUrl=" + combinedSnapshot.hasScoresUrl
                + " | scoresMarker=" + combinedSnapshot.hasScoresMarker
                + " | sourceChangedAfterScroll=" + sourceChangedAfterScroll
                + " | rule=Both real links must be found during full-page scroll before opening URL destinations");
    }


    public void openAndValidateInvestorCharterLinksForInvestorCharter() {
        ReportLogger.step("Opening and validating actual Investor Charter page links");

        waitForInvestorCharterPageForInvestorCharter();

        InvestorCharterLinkSnapshot linkSnapshot = latestInvestorCharterLinkSnapshot;
        if (linkSnapshot == null) {
            linkSnapshot = captureInvestorCharterLinkSnapshot();
        }

        if (!linkSnapshot.hasBothRequiredInvestorCharterLinks()) {
            throw new AssertionError("Cannot test Investor Charter links because both link markers were not found during full-page scroll"
                    + " | required=SEBI registered Investment Advisers list link AND SCORES URL/link"
                    + " | sebiListLink=" + linkSnapshot.hasSebiListMarker
                    + " | scoresUrl=" + linkSnapshot.hasScoresUrl
                    + " | scoresMarker=" + linkSnapshot.hasScoresMarker
                    + " | source=" + linkSnapshot.source);
        }

        List<LinkTarget> linkTargets = new ArrayList<>();

        linkTargets.add(new LinkTarget(
                "SEBI registered Investment Advisers list",
                "https://www.sebi.gov.in/sebiweb/other/OtherAction.do?doRecognisedFpi=yes&intmId=13",
                new String[]{
                        "SEBI registered Investment Advisers",
                        "registered Investment Advisers",
                        "sebi.gov.in/sebiweb",
                        "doRecognisedFpi",
                        "intmId=13"
                },
                new String[]{
                        "investment adviser",
                        "investment advisers",
                        "intermediaries",
                        "registration number"
                }
        ));

        linkTargets.add(new LinkTarget(
                "SCORES portal",
                "https://scores.sebi.gov.in",
                new String[]{
                        "https://scores.sebi.gov.in",
                        "scores.sebi.gov.in",
                        "SCORES 2.0",
                        "SCORES"
                },
                new String[]{
                        "SCORES",
                        "complaint",
                        "grievance",
                        "investor"
                }
        ));

        int openedLinks = 0;
        List<String> validatedLinks = new ArrayList<>();

        for (LinkTarget target : linkTargets) {
            ensureInvestorCharterPageReadyForLink(target.label);

            if (!scrollToInvestorCharterLink(target)) {
                throw new AssertionError("Actual Investor Charter link is not reachable on the page"
                        + " | target=" + target.label
                        + " | expectedUrl=" + target.url
                        + " | visibleValues=" + collectVisibleStrings());
            }

            String beforePackage = getCurrentPackageSafely();

            ReportLogger.step("Tapping actual Investor Charter page link: "
                    + target.label + " | expectedUrl=" + target.url);

            if (!tryOpenLinkByPageTap(target)) {
                throw new AssertionError("Actual Investor Charter page link did not open an external destination"
                        + " | target=" + target.label
                        + " | expectedUrl=" + target.url
                        + " | beforePackage=" + beforePackage
                        + " | currentPackage=" + getCurrentPackageSafely()
                        + " | visibleValues=" + collectVisibleStrings());
            }

            if (!waitForInvestorCharterExternalDestinationAfterTap(target, beforePackage)) {
                throw new AssertionError("External destination opened but the expected URL/page was not validated"
                        + " | target=" + target.label
                        + " | expectedUrl=" + target.url
                        + " | beforePackage=" + beforePackage
                        + " | currentPackage=" + getCurrentPackageSafely()
                        + " | browserAddress=" + readBrowserAddressBarValue()
                        + " | visibleValues=" + collectVisibleStrings());
            }

            openedLinks++;
            validatedLinks.add(target.label);

            returnToInvestorCharterPageAfterLink(target.label);
            sleep(900);
        }

        if (openedLinks != linkTargets.size()) {
            throw new AssertionError("Not all Investor Charter page links were opened and validated"
                    + " | expected=" + linkTargets.size()
                    + " | opened=" + openedLinks
                    + " | validatedLinks=" + validatedLinks);
        }

        ReportLogger.pass("Both actual Investor Charter page links opened and their destinations were strictly validated"
                + " | openedLinks=" + openedLinks
                + " | validatedLinks=" + validatedLinks
                + " | rule=No hardcoded direct-URL substitute and no browser-package-only pass");
    }

    public void returnBackToHubSafelyForInvestorCharter() {
        ReportLogger.step("Returning back to Hub after Investor Charter validation");

        for (int attempt = 1; attempt <= 5; attempt++) {
            if (isLikelyOnHubPage() && isVisibleByAnyText("Hub")) {
                ReportLogger.pass("Already back on Hub page");
                return;
            }

            if (isInvestorCharterPageVisible()) {
                if (tapCloseOrBackButtonIfVisible()) {
                    sleep(1500);
                } else {
                    pressBackSilently();
                    sleep(1500);
                }
            } else {
                pressBackSilently();
                sleep(1500);
            }

            if (isLikelyOnHubPage() && isVisibleByAnyText("Hub")) {
                ReportLogger.pass("Returned to Hub after back attempt " + attempt);
                return;
            }
        }

        try {
            if (advisorAppPackage != null && !advisorAppPackage.trim().isEmpty()) {
                driver.activateApp(advisorAppPackage);
                sleep(1800);

                if (isLikelyOnHubPage() && isVisibleByAnyText("Hub")) {
                    ReportLogger.pass("Returned to Hub using app activate fallback");
                    return;
                }
            }
        } catch (Exception e) {
            ReportLogger.debug("activateApp fallback failed: " + cleanError(e.getMessage()));
        }

        ReportLogger.debug("Could not confirm Hub return after Investor Charter flow"
                + " | currentPackage=" + getCurrentPackageSafely()
                + " | visibleValues=" + collectVisibleStrings());
    }

    // =========================================================
    // OPTIONAL SINGLE-FLOW METHOD
    // =========================================================

    public void verifyInvestorCharterFromHub() {
        ReportLogger.step("Verifying Investor Charter module from Hub");

        captureAdvisorAppPackageForInvestorCharter();
        ensureAdvisorAppLoggedInForInvestorCharter();
        openHubFromBottomNavigationForInvestorCharter();
        scrollToInvestorCharterInHubForInvestorCharter();
        tapInvestorCharterForInvestorCharter();
        waitForInvestorCharterPageForInvestorCharter();
        validateInvestorCharterPageContentForInvestorCharter();
        validateInvestorCharterScrollableContentForInvestorCharter();
        validateInvestorCharterLinksPresentForInvestorCharter();
        openAndValidateInvestorCharterLinksForInvestorCharter();

        ReportLogger.pass("Investor Charter module validated successfully");
    }

    // =========================================================
    // LOGIN / SESSION HELPERS
    // =========================================================

    private boolean isPinScreenVisible() {
        List<String> values = collectVisibleStrings();

        return containsAny(values,
                "Enter your Advisor PIN",
                "Advisor PIN",
                "PIN",
                "Hi,"
        );
    }

    private boolean isMainAppLoaded() {
        List<String> values = collectVisibleStrings();

        return containsAny(values,
                "Funds",
                "Portfolio",
                "Hub",
                "Clients",
                "Reports",
                "Search"
        );
    }

    private void enterAdvisorPin() {
        String pin = "1975";

        for (char digit : pin.toCharArray()) {
            tapPinDigit(String.valueOf(digit));
            sleep(450);
        }
    }

    private void tapPinDigit(String digit) {
        WebElement digitElement = findVisibleExactTextElement(digit);

        if (digitElement != null) {
            tapElementCenter(digitElement);
            ReportLogger.step("Tapped PIN digit: " + digit);
            return;
        }

        digitElement = findVisibleTextElement(digit);

        if (digitElement != null) {
            tapElementCenter(digitElement);
            ReportLogger.step("Tapped PIN digit by fallback: " + digit);
            return;
        }

        throw new AssertionError("Unable to tap PIN digit: " + digit
                + " | visibleValues=" + collectVisibleStrings());
    }

    private void waitForMainAppAfterPin() {
        ReportLogger.step("Waiting for Advisor app dashboard after PIN");

        for (int i = 1; i <= 25; i++) {
            if (isMainAppLoaded()) {
                ReportLogger.pass("Advisor app dashboard loaded after PIN");
                return;
            }

            sleep(1000);
        }

        throw new AssertionError("Advisor app dashboard did not load after PIN"
                + " | visibleValues=" + collectVisibleStrings());
    }

    // =========================================================
    // HUB / INVESTOR CHARTER HELPERS
    // =========================================================

    private boolean isLikelyOnHubPage() {
        List<String> values = collectVisibleStrings();

        for (String value : values) {
            String clean = normalizeSpaces(value);

            if (clean.equals("More")
                    || clean.contains("Investor Charter")
                    || clean.contains("Refund Policy")
                    || clean.contains("ODR Portal")
                    || clean.contains("Audit Status")
                    || clean.contains("Important Disclosures")
                    || clean.contains("Calculators")
                    || clean.contains("Tools")
                    || clean.contains("Knowledge")) {
                return true;
            }
        }

        return false;
    }

    private boolean isInvestorCharterPageVisible() {
        InvestorCharterSnapshot snapshot = captureInvestorCharterSnapshot(true);
        return snapshot.hasTitle || (snapshot.hasInvestmentAdviser && snapshot.hasInvestors && snapshot.hasVisionMission);
    }

    private boolean tapCloseOrBackButtonIfVisible() {
        return tapIfVisible(AppiumBy.accessibilityId("Back"), "Investor Charter back button")
                || tapIfVisible(AppiumBy.accessibilityId("Close"), "Investor Charter close button")
                || tapIfVisible(AppiumBy.androidUIAutomator("new UiSelector().descriptionContains(\"Back\")"), "Investor Charter back by description")
                || tapIfVisible(AppiumBy.androidUIAutomator("new UiSelector().descriptionContains(\"Close\")"), "Investor Charter close by description")
                || tapIfVisible(AppiumBy.androidUIAutomator("new UiSelector().textContains(\"Back\")"), "Investor Charter back by text")
                || tapIfVisible(AppiumBy.androidUIAutomator("new UiSelector().textContains(\"Close\")"), "Investor Charter close by text");
    }

    // =========================================================
    // ELEMENT LOCATORS
    // =========================================================

    private By investorCharterExactLocator() {
        return AppiumBy.accessibilityId(INVESTOR_CHARTER_TILE_EXACT_DESC);
    }

    private By investorCharterDescriptionContainsLocator() {
        return AppiumBy.androidUIAutomator("new UiSelector().descriptionContains(\"investor charter\")");
    }

    private By investorCharterLowerDescriptionContainsLocator() {
        return AppiumBy.androidUIAutomator("new UiSelector().descriptionContains(\"View the investor\")");
    }

    private By investorCharterTextContainsLocator() {
        return AppiumBy.androidUIAutomator("new UiSelector().textContains(\"Investor Charter\")");
    }

    private WebElement findVisibleElement(By locator) {
        try {
            List<WebElement> elements = driver.findElements(locator);

            for (WebElement element : elements) {
                try {
                    if (element != null && element.isDisplayed()) {
                        return element;
                    }
                } catch (Exception ignored) {
                    // Ignore stale/unreadable elements.
                }
            }
        } catch (Exception e) {
            ReportLogger.debug("findVisibleElement skipped: " + cleanError(e.getMessage()));
        }

        return null;
    }

    private boolean isVisible(By locator) {
        return findVisibleElement(locator) != null;
    }

    private boolean tapIfVisible(By locator, String label) {
        WebElement element = findVisibleElement(locator);

        if (element == null) {
            return false;
        }

        tapElementCenter(element);
        ReportLogger.pass("Tapped: " + label);
        return true;
    }

    private boolean tapAnyVisibleText(String text) {
        WebElement element = findVisibleTextElement(text);

        if (element == null) {
            return false;
        }

        tapElementCenter(element);
        return true;
    }

    private WebElement findVisibleExactTextElement(String expectedText) {
        try {
            List<WebElement> elements = driver.findElements(By.xpath("//*"));

            for (WebElement element : elements) {
                try {
                    if (element == null || !element.isDisplayed()) {
                        continue;
                    }

                    String text = normalizeSpaces(element.getText());
                    String desc = normalizeSpaces(element.getAttribute("content-desc"));
                    String name = normalizeSpaces(element.getAttribute("name"));
                    String attrText = normalizeSpaces(element.getAttribute("text"));

                    if (expectedText.equals(text)
                            || expectedText.equals(desc)
                            || expectedText.equals(name)
                            || expectedText.equals(attrText)) {
                        return element;
                    }
                } catch (Exception ignored) {
                    // Ignore stale/unreadable elements.
                }
            }
        } catch (Exception e) {
            ReportLogger.debug("findVisibleExactTextElement skipped: " + cleanError(e.getMessage()));
        }

        return null;
    }

    private WebElement findVisibleTextElement(String expectedText) {
        try {
            List<WebElement> elements = driver.findElements(By.xpath("//*"));

            for (WebElement element : elements) {
                try {
                    if (element == null || !element.isDisplayed()) {
                        continue;
                    }

                    String text = normalizeSpaces(element.getText());
                    String desc = normalizeSpaces(element.getAttribute("content-desc"));
                    String name = normalizeSpaces(element.getAttribute("name"));
                    String attrText = normalizeSpaces(element.getAttribute("text"));

                    if (equalsOrContainsIgnoreCase(text, expectedText)
                            || equalsOrContainsIgnoreCase(desc, expectedText)
                            || equalsOrContainsIgnoreCase(name, expectedText)
                            || equalsOrContainsIgnoreCase(attrText, expectedText)) {
                        return element;
                    }
                } catch (Exception ignored) {
                    // Ignore stale/unreadable elements.
                }
            }
        } catch (Exception e) {
            ReportLogger.debug("findVisibleTextElement skipped: " + cleanError(e.getMessage()));
        }

        return null;
    }

    private WebElement findVisibleTextElementNearBottom(String expectedText) {
        try {
            Dimension size = driver.manage().window().getSize();
            int bottomMinY = (int) (size.getHeight() * 0.70);

            List<WebElement> elements = driver.findElements(By.xpath("//*"));

            for (WebElement element : elements) {
                try {
                    if (element == null || !element.isDisplayed()) {
                        continue;
                    }

                    Rectangle rect = element.getRect();
                    int centerY = rect.getY() + rect.getHeight() / 2;

                    if (centerY < bottomMinY) {
                        continue;
                    }

                    String text = normalizeSpaces(element.getText());
                    String desc = normalizeSpaces(element.getAttribute("content-desc"));
                    String name = normalizeSpaces(element.getAttribute("name"));
                    String attrText = normalizeSpaces(element.getAttribute("text"));

                    if (equalsOrContainsIgnoreCase(text, expectedText)
                            || equalsOrContainsIgnoreCase(desc, expectedText)
                            || equalsOrContainsIgnoreCase(name, expectedText)
                            || equalsOrContainsIgnoreCase(attrText, expectedText)) {
                        return element;
                    }
                } catch (Exception ignored) {
                    // Ignore stale/unreadable elements.
                }
            }
        } catch (Exception e) {
            ReportLogger.debug("findVisibleTextElementNearBottom skipped: " + cleanError(e.getMessage()));
        }

        return null;
    }

    private boolean isVisibleByAnyText(String text) {
        return findVisibleTextElement(text) != null;
    }

    private void waitForAppToBeInteractive() {
        for (int i = 1; i <= 12; i++) {
            List<String> values = collectVisibleStrings();

            if (!values.isEmpty()) {
                return;
            }

            sleep(700);
        }
    }

    private void waitUntilTextVisible(String text, int timeoutSeconds) {
        for (int i = 1; i <= timeoutSeconds; i++) {
            if (isVisibleByAnyText(text)) {
                return;
            }

            sleep(1000);
        }

        throw new AssertionError("Text not visible within timeout: " + text
                + " | visibleValues=" + collectVisibleStrings());
    }

    private List<String> collectVisibleStrings() {
        List<String> values = new ArrayList<>();

        try {
            List<WebElement> elements = driver.findElements(By.xpath("//*"));

            for (WebElement element : elements) {
                try {
                    if (element == null || !element.isDisplayed()) {
                        continue;
                    }

                    addUniqueValue(values, element.getText());
                    addUniqueValue(values, element.getAttribute("content-desc"));
                    addUniqueValue(values, element.getAttribute("text"));
                    addUniqueValue(values, element.getAttribute("name"));
                } catch (Exception ignored) {
                    // Ignore stale/unreadable elements.
                }
            }
        } catch (Exception e) {
            ReportLogger.debug("collectVisibleStrings skipped: " + cleanError(e.getMessage()));
        }

        return values;
    }

    private void addUniqueValue(List<String> values, String rawValue) {
        if (rawValue == null) {
            return;
        }

        String clean = normalizeSpaces(rawValue);

        if (clean.isEmpty()) {
            return;
        }

        if (!values.contains(clean)) {
            values.add(clean);
        }

        String[] parts = rawValue.split("\\n");

        for (String part : parts) {
            String cleanPart = normalizeSpaces(part);

            if (!cleanPart.isEmpty() && !values.contains(cleanPart)) {
                values.add(cleanPart);
            }
        }
    }

    private boolean containsAny(List<String> values, String... expectedTexts) {
        for (String value : values) {
            String cleanValue = normalizeSpaces(value).toLowerCase();

            for (String expectedText : expectedTexts) {
                if (expectedText == null) {
                    continue;
                }

                String cleanExpected = normalizeSpaces(expectedText).toLowerCase();

                if (!cleanExpected.isEmpty() && cleanValue.contains(cleanExpected)) {
                    return true;
                }
            }
        }

        return false;
    }


    private boolean scrollToInvestorCharterLink(LinkTarget target) {
        ReportLogger.step("Locating actual Investor Charter link in document: " + target.label);

        if (findInvestorCharterLinkElement(target) != null) {
            ReportLogger.pass("Investor Charter link is already visible: " + target.label);
            return true;
        }

        boolean scoresTarget = target.url != null
                && target.url.contains("scores.sebi.gov.in");

        /*
         * Document order is important:
         * SCORES appears above the SEBI registered Investment Advisers URL.
         * After the SEBI link is opened and the Advisor app is activated again,
         * the WebView returns to the SEBI section. Therefore SCORES must be searched
         * upward first (finger swipe down). Searching downward first moves away from it
         * and causes a long, misleading locator loop.
         */
        if (scoresTarget) {
            for (int attempt = 1; attempt <= 16; attempt++) {
                smallSwipeDown();
                sleep(650);

                if (findInvestorCharterLinkElement(target) != null) {
                    ReportLogger.pass("SCORES link visible after upward document scroll"
                            + " | target=" + target.label
                            + " | attempt=" + attempt);
                    return true;
                }
            }

            // Small fallback only in case app restore returned above the SCORES section.
            for (int attempt = 1; attempt <= 6; attempt++) {
                smallSwipeUp();
                sleep(650);

                if (findInvestorCharterLinkElement(target) != null) {
                    ReportLogger.pass("SCORES link visible after downward fallback scroll"
                            + " | target=" + target.label
                            + " | attempt=" + attempt);
                    return true;
                }
            }

            return false;
        }

        // SEBI URL is below SCORES in the document, so search downward first.
        for (int attempt = 1; attempt <= 12; attempt++) {
            smallSwipeUp();
            sleep(650);

            if (findInvestorCharterLinkElement(target) != null) {
                ReportLogger.pass("Investor Charter link visible after downward document scroll"
                        + " | target=" + target.label
                        + " | attempt=" + attempt);
                return true;
            }
        }

        // Recovery when STEP 09 or app restore leaves the document below the target.
        for (int attempt = 1; attempt <= 12; attempt++) {
            smallSwipeDown();
            sleep(650);

            if (findInvestorCharterLinkElement(target) != null) {
                ReportLogger.pass("Investor Charter link visible after upward document recovery"
                        + " | target=" + target.label
                        + " | attempt=" + attempt);
                return true;
            }
        }

        return false;
    }

    private boolean tryOpenLinkByPageTap(LinkTarget target) {
        WebElement linkElement = findInvestorCharterLinkElement(target);

        if (linkElement == null) {
            ReportLogger.debug("Actual Investor Charter link element not found for: " + target.label);
            return false;
        }

        String beforePackage = getCurrentPackageSafely();

        try {
            Rectangle rect = linkElement.getRect();
            String clickable = safeGetAttribute(linkElement, "clickable");
            String clickableSpan = safeGetAttribute(linkElement, "text-has-clickable-span");
            String actions = safeGetAttribute(linkElement, "actions");

            ReportLogger.step("Actual Investor Charter link element located"
                    + " | target=" + target.label
                    + " | bounds=" + rect
                    + " | clickable=" + clickable
                    + " | textHasClickableSpan=" + clickableSpan
                    + " | actions=" + actions);
        } catch (Exception e) {
            ReportLogger.debug("Unable to log actual link attributes: " + cleanError(e.getMessage()));
        }

        // Use the element accessibility action first. This is more suitable for URLSpan/
        // ClickableSpan content than blindly tapping the centre of a large rich-text block.
        try {
            linkElement.click();
            ReportLogger.step("Clicked actual Investor Charter link element: " + target.label);
        } catch (Exception clickException) {
            ReportLogger.debug("Element click failed for actual Investor Charter link. Using centre tap."
                    + " | target=" + target.label
                    + " | error=" + cleanError(clickException.getMessage()));
            tapElementCenter(linkElement);
        }

        sleep(2200);
        handleExternalLinkResolverIfPresent();
        sleep(1000);

        String currentPackage = getCurrentPackageSafely();
        if (isExternalBrowserPackage(currentPackage)
                && isExternalPackageChange(beforePackage, currentPackage)) {
            ReportLogger.pass("Actual Investor Charter link triggered external browser"
                    + " | target=" + target.label
                    + " | beforePackage=" + beforePackage
                    + " | currentPackage=" + currentPackage);
            return true;
        }

        // Some accessibility nodes accept click without invoking the URLSpan. Re-find the
        // element after the first attempt and use one controlled centre tap as a final retry.
        linkElement = findInvestorCharterLinkElement(target);
        if (linkElement != null) {
            try {
                tapElementCenter(linkElement);
                ReportLogger.step("Retried actual Investor Charter link by centre tap: " + target.label);
            } catch (Exception retryException) {
                ReportLogger.debug("Centre-tap retry failed for actual link"
                        + " | target=" + target.label
                        + " | error=" + cleanError(retryException.getMessage()));
            }
        }

        sleep(2200);
        handleExternalLinkResolverIfPresent();
        sleep(1000);

        currentPackage = getCurrentPackageSafely();
        return isExternalBrowserPackage(currentPackage)
                && isExternalPackageChange(beforePackage, currentPackage);
    }

    private boolean waitForInvestorCharterExternalDestinationAfterTap(LinkTarget target, String beforePackage) {
        ReportLogger.step("Strictly validating external destination opened by actual page link: " + target.label);

        String expectedHost = getExpectedHost(target.url);
        String lastAddress = "";
        String lastPackage = "";
        String lastSource = "";

        for (int attempt = 1; attempt <= 8; attempt++) {
            handleExternalLinkResolverIfPresent();

            String currentPackage = getCurrentPackageSafely();
            String addressValue = readBrowserAddressBarValue();

            lastAddress = addressValue;
            lastPackage = currentPackage;

            boolean browserOpened = isExternalBrowserPackage(currentPackage)
                    && isExternalPackageChange(beforePackage, currentPackage);

            /*
             * Chrome commonly displays "sebi.gov.in" even when the requested URL uses
             * "www.sebi.gov.in". The address bar is the strongest and fastest evidence
             * that the actual page link opened. Validate it before scanning the entire
             * browser page, because the SEBI language modal makes broad element scans slow.
             */
            boolean expectedHostVisibleInAddress = browserOpened
                    && !expectedHost.isEmpty()
                    && containsIgnoreCase(addressValue, expectedHost);

            if (expectedHostVisibleInAddress) {
                ReportLogger.pass("Actual Investor Charter external destination strictly validated"
                        + " | target=" + target.label
                        + " | currentPackage=" + currentPackage
                        + " | browserAddress=" + addressValue
                        + " | expectedHost=" + expectedHost
                        + " | validation=browser address host"
                        + " | waitAttempt=" + attempt);
                return true;
            }

            // Use browser content only as a secondary fallback when the omnibox is hidden.
            List<String> values = collectVisibleStringsQuick();
            String source = normalizeForMatching(joinValues(values));
            lastSource = source;

            boolean expectedHostVisibleInSource = !expectedHost.isEmpty()
                    && containsIgnoreCase(source, expectedHost);
            int destinationMarkerCount = countExpectedDestinationMarkers(source, target.expectedMarkers);
            boolean strongContentEvidence = destinationMarkerCount >= 2;
            boolean browserError = hasBrowserErrorMarker(source);

            ReportLogger.debug("External destination validation attempt"
                    + " | target=" + target.label
                    + " | attempt=" + attempt
                    + " | browserOpened=" + browserOpened
                    + " | currentPackage=" + currentPackage
                    + " | browserAddress=" + addressValue
                    + " | expectedHost=" + expectedHost
                    + " | expectedHostVisibleInAddress=" + expectedHostVisibleInAddress
                    + " | expectedHostVisibleInSource=" + expectedHostVisibleInSource
                    + " | destinationMarkerCount=" + destinationMarkerCount
                    + " | browserError=" + browserError);

            if (browserOpened
                    && !browserError
                    && (expectedHostVisibleInSource || strongContentEvidence)) {
                ReportLogger.pass("Actual Investor Charter external destination strictly validated"
                        + " | target=" + target.label
                        + " | currentPackage=" + currentPackage
                        + " | browserAddress=" + addressValue
                        + " | expectedHost=" + expectedHost
                        + " | destinationMarkerCount=" + destinationMarkerCount
                        + " | validation=browser content fallback"
                        + " | waitAttempt=" + attempt);
                return true;
            }

            // Reveal the omnibox once if Chrome hides it while the page is loading.
            if (browserOpened && attempt == 3 && addressValue.isEmpty()) {
                tapChromeTopAddressArea();
                sleep(700);
            }

            sleep(1000);
        }

        ReportLogger.fail("Actual Investor Charter external destination validation failed"
                + " | target=" + target.label
                + " | expectedHost=" + expectedHost
                + " | lastPackage=" + lastPackage
                + " | lastBrowserAddress=" + lastAddress
                + " | lastSource=" + lastSource);
        return false;
    }

    private WebElement findInvestorCharterLinkElement(LinkTarget target) {
        /*
         * IMPORTANT:
         * The WebView exposes both the surrounding paragraph and the real URL as
         * android.widget.TextView nodes. The paragraph contains words such as
         * "SEBI registered Investment Advisers" but tapping it does not activate
         * the anchor. Only return a TextView whose own text is the URL shown by
         * Appium Inspector. Never fall back to surrounding paragraph text here.
         */
        List<By> strictLocators = new ArrayList<>();

        if (target.url.contains("sebi.gov.in/sebiweb/other/OtherAction.do")) {
            String exactUrl = target.url;
            String stablePrefix = "https://www.sebi.gov.in/sebiweb/other/OtherAction.do";

            strictLocators.add(AppiumBy.androidUIAutomator(
                    "new UiSelector().className(\"android.widget.TextView\").text(\""
                            + escapeForUiSelector(exactUrl)
                            + "\")"
            ));
            strictLocators.add(By.xpath(
                    "//android.widget.TextView[@text=\"" + exactUrl + "\"]"
            ));

            // The query string can be encoded or exposed slightly differently by WebView.
            // The path prefix is stable and still identifies only the actual URL TextView.
            strictLocators.add(AppiumBy.androidUIAutomator(
                    "new UiSelector().className(\"android.widget.TextView\").textStartsWith(\""
                            + escapeForUiSelector(stablePrefix)
                            + "\")"
            ));
            strictLocators.add(By.xpath(
                    "//android.widget.TextView[starts-with(@text,\"" + stablePrefix + "\")]"
            ));
        } else if (target.url.contains("scores.sebi.gov.in")) {
            String exactParenthesizedUrl = "(" + target.url + ")";

            // This is the exact text exposed in the supplied Appium Inspector screenshot.
            strictLocators.add(AppiumBy.androidUIAutomator(
                    "new UiSelector().className(\"android.widget.TextView\").text(\""
                            + escapeForUiSelector(exactParenthesizedUrl)
                            + "\")"
            ));
            strictLocators.add(By.xpath(
                    "//android.widget.TextView[@text=\"" + exactParenthesizedUrl + "\"]"
            ));

            // Compatibility fallback for builds that omit the visual parentheses.
            strictLocators.add(AppiumBy.androidUIAutomator(
                    "new UiSelector().className(\"android.widget.TextView\").text(\""
                            + escapeForUiSelector(target.url)
                            + "\")"
            ));
            strictLocators.add(By.xpath(
                    "//android.widget.TextView[@text=\"" + target.url + "\"]"
            ));
        }

        for (By locator : strictLocators) {
            try {
                List<WebElement> elements = driver.findElements(locator);

                for (WebElement element : elements) {
                    try {
                        if (element == null || !element.isDisplayed()) {
                            continue;
                        }

                        String elementClass = normalizeSpaces(element.getAttribute("class"));
                        String elementText = normalizeSpaces(element.getAttribute("text"));
                        String expectedHost = getExpectedHost(target.url);

                        if (!"android.widget.TextView".equals(elementClass)) {
                            continue;
                        }

                        if (!expectedHost.isEmpty()
                                && !containsIgnoreCase(elementText, expectedHost)) {
                            continue;
                        }

                        ReportLogger.step("Matched strict Investor Charter URL TextView"
                                + " | target=" + target.label
                                + " | locator=" + locator
                                + " | actualText=" + elementText
                                + " | bounds=" + element.getRect());
                        return element;
                    } catch (Exception ignored) {
                        // Continue with the next matching node/locator.
                    }
                }
            } catch (Exception e) {
                ReportLogger.debug("Strict Investor Charter URL locator skipped"
                        + " | target=" + target.label
                        + " | locator=" + locator
                        + " | error=" + cleanError(e.getMessage()));
            }
        }

        ReportLogger.debug("Strict URL TextView not currently visible"
                + " | target=" + target.label
                + " | expectedUrl=" + target.url);
        return null;
    }

    private void openUrlUsingAndroidIntent(LinkTarget target) {
        if (target.url == null || target.url.trim().isEmpty()) {
            throw new AssertionError("Investor Charter link URL is empty for target: " + target.label);
        }

        // First try the fastest route. This works only when Appium server is started with:
        // appium --allow-insecure=adb_shell
        try {
            Map<String, Object> args = new HashMap<>();
            args.put("command", "am");
            args.put("args", Arrays.asList(
                    "start",
                    "-a", "android.intent.action.VIEW",
                    "-c", "android.intent.category.BROWSABLE",
                    "-d", target.url
            ));
            args.put("includeStderr", true);
            args.put("timeout", 12000);

            driver.executeScript("mobile: shell", args);
            ReportLogger.step("Opened Investor Charter link with Android VIEW intent: "
                    + target.label + " | url=" + target.url);
            sleep(1800);
            return;
        } catch (Exception shellException) {
            ReportLogger.debug("Android VIEW intent unavailable for " + target.label
                    + " because adb_shell is not enabled or intent failed. Using Chrome address-bar fallback. Error="
                    + cleanError(shellException.getMessage()));
        }

        if (openUrlInChromeAddressBar(target)) {
            return;
        }

        throw new AssertionError("Unable to open Investor Charter link URL: "
                + target.label
                + " | url=" + target.url
                + " | Fix option 1: start Appium with --allow-insecure=adb_shell"
                + " | Fix option 2: keep Chrome installed/enabled on emulator");
    }

    private boolean openUrlInChromeAddressBar(LinkTarget target) {
        ReportLogger.step("Opening Investor Charter link using Chrome address-bar fallback: "
                + target.label + " | url=" + target.url);

        String[] chromePackages = new String[]{
                "com.android.chrome",
                "com.google.android.apps.chrome",
                "com.android.browser"
        };

        for (String chromePackage : chromePackages) {
            try {
                driver.activateApp(chromePackage);
                sleep(2200);
                handleBrowserFirstRunPopupsIfAny();
                sleep(800);

                WebElement addressBar = findChromeAddressBar();

                if (addressBar == null) {
                    tapChromeTopAddressArea();
                    sleep(900);
                    addressBar = findChromeAddressBar();
                }

                if (addressBar == null) {
                    ReportLogger.debug("Chrome address bar not found for package: " + chromePackage);
                    continue;
                }

                try {
                    addressBar.click();
                } catch (Exception ignored) {
                    tapElementCenter(addressBar);
                }
                sleep(500);

                try {
                    addressBar.clear();
                } catch (Exception ignored) {
                    // Some Chrome address elements do not support clear(). sendKeys still replaces focus text.
                }

                addressBar.sendKeys(target.url);
                sleep(400);
                addressBar.sendKeys(Keys.ENTER);
                sleep(3000);

                ReportLogger.step("Submitted Investor Charter URL in Chrome address bar: " + target.label);
                return true;
            } catch (Exception e) {
                ReportLogger.debug("Chrome fallback failed for package " + chromePackage
                        + " | target=" + target.label
                        + " | error=" + cleanError(e.getMessage()));
            }
        }

        try {
            if (advisorAppPackage != null && !advisorAppPackage.trim().isEmpty()) {
                driver.activateApp(advisorAppPackage);
                sleep(1000);
            }
        } catch (Exception ignored) {
            // Ignore recovery failure here. Caller will fail with proper message.
        }

        return false;
    }

    private WebElement findChromeAddressBar() {
        By[] locators = new By[]{
                By.id("com.android.chrome:id/search_box_text"),
                By.id("com.android.chrome:id/url_bar"),
                AppiumBy.androidUIAutomator("new UiSelector().descriptionContains(\"Search or type\")"),
                AppiumBy.androidUIAutomator("new UiSelector().textContains(\"Search or type\")"),
                AppiumBy.androidUIAutomator("new UiSelector().descriptionContains(\"address\")"),
                AppiumBy.androidUIAutomator("new UiSelector().textContains(\"Search\")")
        };

        for (By locator : locators) {
            WebElement element = findVisibleElement(locator);
            if (element != null) {
                return element;
            }
        }

        return null;
    }

    private void tapChromeTopAddressArea() {
        Dimension size = driver.manage().window().getSize();
        int x = (int) (size.getWidth() * 0.50);
        int y = (int) (size.getHeight() * 0.10);
        tapByCoordinates(x, y);
        ReportLogger.step("Tapped Chrome top address area fallback");
    }

    private void waitForInvestorCharterExternalDestination(LinkTarget target, String openMode) {
        String beforePackage = advisorAppPackage == null ? "" : advisorAppPackage;

        if (!waitForInvestorCharterExternalDestinationAfterTap(target, beforePackage)) {
            throw new AssertionError("Investor Charter external destination did not strictly validate"
                    + " | target=" + target.label
                    + " | mode=" + openMode
                    + " | expectedUrl=" + target.url
                    + " | currentPackage=" + getCurrentPackageSafely()
                    + " | browserAddress=" + readBrowserAddressBarValue()
                    + " | visibleValues=" + collectVisibleStrings());
        }
    }

    private boolean isExpectedExternalDestinationVisible(LinkTarget target, String currentPackage) {
        if (!isExternalBrowserPackage(currentPackage)) {
            return false;
        }

        List<String> values = collectVisibleStringsQuick();
        String source = normalizeForMatching(joinValues(values));
        String addressValue = readBrowserAddressBarValue();
        String expectedHost = getExpectedHost(target.url);

        boolean expectedHostVisible = !expectedHost.isEmpty()
                && (containsIgnoreCase(addressValue, expectedHost)
                || containsIgnoreCase(source, expectedHost));

        boolean strongContentEvidence = countExpectedDestinationMarkers(
                source,
                target.expectedMarkers
        ) >= 2;

        return !hasBrowserErrorMarker(source)
                && (expectedHostVisible || strongContentEvidence);
    }

    private boolean isExternalPackageChange(String beforePackage, String currentPackage) {
        return beforePackage != null
                && !beforePackage.trim().isEmpty()
                && currentPackage != null
                && !currentPackage.trim().isEmpty()
                && !currentPackage.equals(beforePackage);
    }

    private boolean isLikelyExternalBrowserPackage(String currentPackage) {
        return isExternalBrowserPackage(currentPackage);
    }

    private void ensureInvestorCharterPageReadyForLink(String contextLabel) {
        if (isInvestorCharterPageVisible()) {
            return;
        }

        ReportLogger.debug("Investor Charter page not visible before link action. Recovering page for: " + contextLabel);
        returnToInvestorCharterPageAfterLink("pre-link recovery: " + contextLabel);
    }

    private boolean isInvestorCharterPageVisibleFast() {
        List<String> values = collectVisibleStringsQuick();
        return containsAny(values,
                INVESTOR_CHARTER_TITLE,
                "Investor Charter | Value Research",
                "Investment Adviser",
                "Vision",
                "Mission"
        );
    }

    private void returnToInvestorCharterPageAfterLink(String linkLabel) {
        ReportLogger.step("Returning to Investor Charter page after link: " + linkLabel);

        if (isInvestorCharterPageVisible()) {
            ReportLogger.pass("Investor Charter page already active after link: " + linkLabel);
            return;
        }

        try {
            if (advisorAppPackage != null && !advisorAppPackage.trim().isEmpty()) {
                driver.activateApp(advisorAppPackage);
                sleep(2200);

                if (isInvestorCharterPageVisible()) {
                    ReportLogger.pass("Investor Charter page restored using app activate fallback after link: " + linkLabel);
                    return;
                }
            }
        } catch (Exception e) {
            ReportLogger.debug("activateApp fallback after link failed: " + cleanError(e.getMessage()));
        }

        for (int attempt = 1; attempt <= 4; attempt++) {
            pressBackSilently();
            sleep(1200);

            if (isInvestorCharterPageVisible()) {
                ReportLogger.pass("Returned to Investor Charter page after link: " + linkLabel
                        + " | backAttempt=" + attempt);
                return;
            }
        }

        try {
            if (advisorAppPackage != null && !advisorAppPackage.trim().isEmpty()) {
                driver.activateApp(advisorAppPackage);
                sleep(1500);
            }
        } catch (Exception ignored) {
            // Continue to full recovery.
        }

        if (isInvestorCharterPageVisible()) {
            ReportLogger.pass("Investor Charter page active after final app activate: " + linkLabel);
            return;
        }

        ReportLogger.debug("Investor Charter page was not restored directly. Reopening from Hub after link: " + linkLabel);
        openHubFromBottomNavigationForInvestorCharter();
        scrollToInvestorCharterInHubForInvestorCharter();
        tapInvestorCharterForInvestorCharter();
        waitForInvestorCharterPageForInvestorCharter();
        ReportLogger.pass("Investor Charter page reopened from Hub after link: " + linkLabel);
    }

    private List<String> collectVisibleStringsQuick() {
        List<String> values = new ArrayList<>();

        try {
            List<WebElement> elements = driver.findElements(By.xpath("//*"));
            int inspected = 0;

            for (WebElement element : elements) {
                if (inspected >= 80) {
                    break;
                }

                try {
                    if (element == null || !element.isDisplayed()) {
                        continue;
                    }

                    inspected++;
                    addUniqueValue(values, element.getText());
                    addUniqueValue(values, element.getAttribute("content-desc"));
                    addUniqueValue(values, element.getAttribute("text"));
                    addUniqueValue(values, element.getAttribute("name"));
                } catch (Exception ignored) {
                    // Ignore stale/unreadable elements.
                }
            }
        } catch (Exception e) {
            ReportLogger.debug("collectVisibleStringsQuick skipped: " + cleanError(e.getMessage()));
        }

        return values;
    }

    private String joinValues(List<String> values) {
        StringBuilder builder = new StringBuilder();

        if (values == null) {
            return "";
        }

        for (String value : values) {
            if (value != null && !value.trim().isEmpty()) {
                builder.append(' ').append(value);
            }
        }

        return builder.toString();
    }

    private void handleBrowserFirstRunPopupsIfAny() {
        String currentPackage = getCurrentPackageSafely();

        if (!isExternalBrowserPackage(currentPackage)) {
            return;
        }

        // Only handle genuine browser onboarding prompts. Do not tap generic labels
        // such as Open/Chrome/Always on arbitrary pages because that can hide failures.
        String[] possibleButtons = new String[]{
                "Accept & continue",
                "Accept and continue",
                "Use without an account",
                "No thanks",
                "Got it",
                "Continue",
                "Skip"
        };

        for (String button : possibleButtons) {
            WebElement element = findVisibleExactTextElement(button);
            if (element != null) {
                tapElementCenter(element);
                ReportLogger.step("Tapped browser onboarding button: " + button);
                sleep(1000);
                return;
            }
        }
    }

    private void handleExternalLinkResolverIfPresent() {
        String currentPackage = getCurrentPackageSafely();
        String pkg = currentPackage == null ? "" : currentPackage.toLowerCase();

        boolean resolverVisible = pkg.equals("android")
                || pkg.contains("intentresolver")
                || pkg.contains("permissioncontroller")
                || pkg.contains("resolver");

        if (!resolverVisible) {
            return;
        }

        WebElement chrome = findVisibleExactTextElement("Chrome");
        if (chrome != null) {
            tapElementCenter(chrome);
            ReportLogger.step("Selected Chrome in Android link resolver");
            sleep(700);
        }

        WebElement justOnce = findVisibleExactTextElement("Just once");
        if (justOnce != null) {
            tapElementCenter(justOnce);
            ReportLogger.step("Selected Just once in Android link resolver");
            sleep(900);
            return;
        }

        WebElement open = findVisibleExactTextElement("Open");
        if (open != null) {
            tapElementCenter(open);
            ReportLogger.step("Confirmed Open in Android link resolver");
            sleep(900);
        }
    }

    private boolean isExternalBrowserPackage(String currentPackage) {
        if (currentPackage == null || currentPackage.trim().isEmpty()) {
            return false;
        }

        String pkg = currentPackage.toLowerCase();

        if (advisorAppPackage != null
                && !advisorAppPackage.trim().isEmpty()
                && pkg.equals(advisorAppPackage.toLowerCase())) {
            return false;
        }

        return pkg.equals("com.android.chrome")
                || pkg.equals("com.google.android.apps.chrome")
                || pkg.equals("com.android.browser")
                || pkg.contains("firefox")
                || pkg.contains("edge")
                || pkg.contains("opera")
                || pkg.contains("browser");
    }

    private String readBrowserAddressBarValue() {
        By[] locators = new By[]{
                By.id("com.android.chrome:id/url_bar"),
                By.id("com.android.chrome:id/search_box_text"),
                AppiumBy.androidUIAutomator("new UiSelector().resourceIdMatches(\".*:id/url_bar\")"),
                AppiumBy.androidUIAutomator("new UiSelector().descriptionContains(\"Search or type\")")
        };

        for (By locator : locators) {
            WebElement element = findVisibleElement(locator);
            if (element == null) {
                continue;
            }

            String[] candidates = new String[]{
                    safeGetAttribute(element, "text"),
                    safeGetAttribute(element, "content-desc"),
                    safeGetAttribute(element, "name"),
                    safeGetElementText(element)
            };

            for (String candidate : candidates) {
                String clean = normalizeSpaces(candidate);
                if (!clean.isEmpty()
                        && !clean.equalsIgnoreCase("Search or type web address")
                        && !clean.equalsIgnoreCase("Search or type URL")) {
                    return clean;
                }
            }
        }

        return "";
    }

    private String getExpectedHost(String url) {
        if (url == null || url.trim().isEmpty()) {
            return "";
        }

        try {
            String host = URI.create(url.trim()).getHost();
            if (host == null) {
                return "";
            }

            String normalizedHost = host.toLowerCase();
            return normalizedHost.startsWith("www.")
                    ? normalizedHost.substring(4)
                    : normalizedHost;
        } catch (Exception ignored) {
            String clean = url.replace("https://", "").replace("http://", "");
            int slash = clean.indexOf('/');
            String normalizedHost = (slash >= 0 ? clean.substring(0, slash) : clean).toLowerCase();
            return normalizedHost.startsWith("www.")
                    ? normalizedHost.substring(4)
                    : normalizedHost;
        }
    }

    private int countExpectedDestinationMarkers(String normalizedSource, String[] markers) {
        if (normalizedSource == null || markers == null) {
            return 0;
        }

        int count = 0;
        List<String> matched = new ArrayList<>();

        for (String marker : markers) {
            String normalizedMarker = normalizeForMatching(marker);
            if (normalizedMarker.isEmpty() || matched.contains(normalizedMarker)) {
                continue;
            }

            if (normalizedSource.contains(normalizedMarker)) {
                matched.add(normalizedMarker);
                count++;
            }
        }

        return count;
    }

    private boolean hasBrowserErrorMarker(String normalizedSource) {
        if (normalizedSource == null || normalizedSource.isEmpty()) {
            return false;
        }

        return normalizedSource.contains("this site can't be reached")
                || normalizedSource.contains("webpage not available")
                || normalizedSource.contains("err_name_not_resolved")
                || normalizedSource.contains("err_connection")
                || normalizedSource.contains("dns_probe")
                || normalizedSource.contains("no internet")
                || normalizedSource.contains("your connection is not private");
    }

    private String safeGetAttribute(WebElement element, String attributeName) {
        if (element == null || attributeName == null) {
            return "";
        }

        try {
            String value = element.getAttribute(attributeName);
            return value == null ? "" : value;
        } catch (Exception ignored) {
            return "";
        }
    }

    private String safeGetElementText(WebElement element) {
        if (element == null) {
            return "";
        }

        try {
            String value = element.getText();
            return value == null ? "" : value;
        } catch (Exception ignored) {
            return "";
        }
    }

    private boolean containsAnyInSource(String source, String... expectedTexts) {
        if (source == null) {
            return false;
        }

        for (String expectedText : expectedTexts) {
            if (expectedText == null || expectedText.trim().isEmpty()) {
                continue;
            }

            if (source.contains(normalizeForMatching(expectedText))) {
                return true;
            }
        }

        return false;
    }

    private String escapeForUiSelector(String value) {
        if (value == null) {
            return "";
        }

        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static class LinkTarget {
        private final String label;
        private final String url;
        private final String[] searchTexts;
        private final String[] expectedMarkers;

        private LinkTarget(String label, String url, String[] searchTexts, String[] expectedMarkers) {
            this.label = label;
            this.url = url == null ? "" : url;
            this.searchTexts = searchTexts == null ? new String[]{} : searchTexts;
            this.expectedMarkers = expectedMarkers == null ? new String[]{} : expectedMarkers;
        }

        private String primarySearchText() {
            return searchTexts.length == 0 ? label : searchTexts[0];
        }
    }

    // =========================================================
    // CONTENT SNAPSHOT HELPERS
    // =========================================================

    private InvestorCharterSnapshot captureInvestorCharterSnapshot(boolean visibleOnly) {
        List<String> values = collectVisibleStrings();
        String source = getVisibleTextBlob();
        String normalized = normalizeForMatching(source);

        boolean hasTitle = containsAny(values, INVESTOR_CHARTER_TITLE, "Investor Charter")
                || containsIgnoreCase(normalized, "investor charter");

        boolean hasVisionMission = containsAny(values, "Vision", "Mission", "Vision and Mission")
                || containsIgnoreCase(normalized, "vision")
                || containsIgnoreCase(normalized, "mission");

        boolean hasInvestmentAdviser = containsAny(values, "Investment Adviser", "IA")
                || containsIgnoreCase(normalized, "investment adviser");

        boolean hasInvestors = containsAny(values, "investors", "client")
                || containsIgnoreCase(normalized, "investors")
                || containsIgnoreCase(normalized, "client");

        boolean hasBusinessTransacted = containsAny(values, "Business Transacted", "business transacted")
                || containsIgnoreCase(normalized, "business transacted")
                || containsIgnoreCase(normalized, "details of business");

        boolean hasAgreementOrFee = containsAny(values, "agreement", "fee", "fees")
                || containsIgnoreCase(normalized, "agreement")
                || containsIgnoreCase(normalized, "fee");

        boolean hasRiskOrSuitability = containsAny(values, "risk", "suitability", "profiling")
                || containsIgnoreCase(normalized, "risk")
                || containsIgnoreCase(normalized, "suitability")
                || containsIgnoreCase(normalized, "profiling");

        boolean hasComplaintOrGrievance = containsAny(values, "complaint", "complaints", "grievance")
                || containsIgnoreCase(normalized, "complaint")
                || containsIgnoreCase(normalized, "grievance");

        boolean hasContactOrDisclosure = containsAny(values, "disclose", "confidentiality", "registered", "SEBI", "Head office")
                || containsIgnoreCase(normalized, "disclose")
                || containsIgnoreCase(normalized, "confidentiality")
                || containsIgnoreCase(normalized, "registered")
                || containsIgnoreCase(normalized, "sebi")
                || containsIgnoreCase(normalized, "head office");

        return new InvestorCharterSnapshot(
                visibleOnly ? normalizeSpaces(values.toString()) : source,
                hasTitle,
                hasVisionMission,
                hasInvestmentAdviser,
                hasInvestors,
                hasBusinessTransacted,
                hasAgreementOrFee,
                hasRiskOrSuitability,
                hasComplaintOrGrievance,
                hasContactOrDisclosure
        );
    }

    private String getVisibleTextBlob() {
        List<String> values = collectVisibleStrings();
        StringBuilder builder = new StringBuilder();

        for (String value : values) {
            if (value != null && !value.trim().isEmpty()) {
                builder.append(' ').append(value);
            }
        }

        return builder.toString();
    }

    private static class InvestorCharterSnapshot {
        private final String source;
        private final boolean hasTitle;
        private final boolean hasVisionMission;
        private final boolean hasInvestmentAdviser;
        private final boolean hasInvestors;
        private final boolean hasBusinessTransacted;
        private final boolean hasAgreementOrFee;
        private final boolean hasRiskOrSuitability;
        private final boolean hasComplaintOrGrievance;
        private final boolean hasContactOrDisclosure;

        private InvestorCharterSnapshot(
                String source,
                boolean hasTitle,
                boolean hasVisionMission,
                boolean hasInvestmentAdviser,
                boolean hasInvestors,
                boolean hasBusinessTransacted,
                boolean hasAgreementOrFee,
                boolean hasRiskOrSuitability,
                boolean hasComplaintOrGrievance,
                boolean hasContactOrDisclosure
        ) {
            this.source = source == null ? "" : source;
            this.hasTitle = hasTitle;
            this.hasVisionMission = hasVisionMission;
            this.hasInvestmentAdviser = hasInvestmentAdviser;
            this.hasInvestors = hasInvestors;
            this.hasBusinessTransacted = hasBusinessTransacted;
            this.hasAgreementOrFee = hasAgreementOrFee;
            this.hasRiskOrSuitability = hasRiskOrSuitability;
            this.hasComplaintOrGrievance = hasComplaintOrGrievance;
            this.hasContactOrDisclosure = hasContactOrDisclosure;
        }

        private boolean hasTopContent() {
            return hasTitle && hasVisionMission && hasInvestmentAdviser && hasInvestors;
        }

        private boolean hasScrollableBodyContent() {
            return hasBusinessTransacted
                    && hasAgreementOrFee
                    && hasRiskOrSuitability
                    && hasComplaintOrGrievance
                    && hasContactOrDisclosure;
        }

        private InvestorCharterSnapshot merge(InvestorCharterSnapshot other) {
            if (other == null) {
                return this;
            }

            return new InvestorCharterSnapshot(
                    this.source + " " + other.source,
                    this.hasTitle || other.hasTitle,
                    this.hasVisionMission || other.hasVisionMission,
                    this.hasInvestmentAdviser || other.hasInvestmentAdviser,
                    this.hasInvestors || other.hasInvestors,
                    this.hasBusinessTransacted || other.hasBusinessTransacted,
                    this.hasAgreementOrFee || other.hasAgreementOrFee,
                    this.hasRiskOrSuitability || other.hasRiskOrSuitability,
                    this.hasComplaintOrGrievance || other.hasComplaintOrGrievance,
                    this.hasContactOrDisclosure || other.hasContactOrDisclosure
            );
        }
    }


    private InvestorCharterLinkSnapshot captureInvestorCharterLinkSnapshot() {
        String source = getVisibleTextBlob();
        String normalized = normalizeForMatching(source);

        boolean hasComplaintFilingMode = containsIgnoreCase(normalized, "mode of filing")
                || containsIgnoreCase(normalized, "filing the complaint")
                || containsIgnoreCase(normalized, "lodge complaint")
                || containsIgnoreCase(normalized, "complaint/grievance");

        boolean hasScoresUrl = containsIgnoreCase(normalized, "scores.sebi.gov.in")
                || containsIgnoreCase(normalized, "https://scores.sebi.gov.in");

        boolean hasScoresMarker = containsIgnoreCase(normalized, "scores")
                || containsIgnoreCase(normalized, "sebi scores")
                || containsIgnoreCase(normalized, "sebi complaint redress system");

        boolean hasSebiListMarker = containsIgnoreCase(normalized, "sebi registered investment advisers")
                || containsIgnoreCase(normalized, "registered investment advisers list")
                || containsIgnoreCase(normalized, "sebi.gov.in/sebiweb")
                || containsIgnoreCase(normalized, "doRecognisedFpi")
                || containsIgnoreCase(normalized, "intmId=13");

        boolean hasSmartOdrMarker = containsIgnoreCase(normalized, "smartodr")
                || containsIgnoreCase(normalized, "online conciliation")
                || containsIgnoreCase(normalized, "online dispute resolution");

        return new InvestorCharterLinkSnapshot(
                source,
                hasComplaintFilingMode,
                hasScoresUrl,
                hasScoresMarker,
                hasSebiListMarker,
                hasSmartOdrMarker
        );
    }

    private static class InvestorCharterLinkSnapshot {
        private final String source;
        private final boolean hasComplaintFilingMode;
        private final boolean hasScoresUrl;
        private final boolean hasScoresMarker;
        private final boolean hasSebiListMarker;
        private final boolean hasSmartOdrMarker;

        private InvestorCharterLinkSnapshot(
                String source,
                boolean hasComplaintFilingMode,
                boolean hasScoresUrl,
                boolean hasScoresMarker,
                boolean hasSebiListMarker,
                boolean hasSmartOdrMarker
        ) {
            this.source = source == null ? "" : source;
            this.hasComplaintFilingMode = hasComplaintFilingMode;
            this.hasScoresUrl = hasScoresUrl;
            this.hasScoresMarker = hasScoresMarker;
            this.hasSebiListMarker = hasSebiListMarker;
            this.hasSmartOdrMarker = hasSmartOdrMarker;
        }

        private boolean hasAnyUsefulLinkMarker() {
            return hasComplaintFilingMode || hasScoresUrl || hasScoresMarker || hasSebiListMarker || hasSmartOdrMarker;
        }

        private boolean hasAtLeastOneActionableLinkMarker() {
            return hasScoresUrl || hasScoresMarker || hasSebiListMarker;
        }

        private boolean hasBothRequiredInvestorCharterLinks() {
            return hasSebiListMarker && hasScoresUrl;
        }

        private boolean hasMinimumLinkMarkers() {
            return hasComplaintFilingMode && hasBothRequiredInvestorCharterLinks();
        }

        private InvestorCharterLinkSnapshot merge(InvestorCharterLinkSnapshot other) {
            if (other == null) {
                return this;
            }

            return new InvestorCharterLinkSnapshot(
                    this.source + " " + other.source,
                    this.hasComplaintFilingMode || other.hasComplaintFilingMode,
                    this.hasScoresUrl || other.hasScoresUrl,
                    this.hasScoresMarker || other.hasScoresMarker,
                    this.hasSebiListMarker || other.hasSebiListMarker,
                    this.hasSmartOdrMarker || other.hasSmartOdrMarker
            );
        }
    }

    // =========================================================
    // GESTURE HELPERS
    // =========================================================

    private void tapElementCenter(WebElement element) {
        Rectangle rect = element.getRect();

        int x = rect.getX() + rect.getWidth() / 2;
        int y = rect.getY() + rect.getHeight() / 2;

        tapByCoordinates(x, y);
    }

    private void tapByCoordinates(int x, int y) {
        PointerInput finger = new PointerInput(PointerInput.Kind.TOUCH, "finger");
        Sequence tap = new Sequence(finger, 1);

        tap.addAction(finger.createPointerMove(
                Duration.ZERO,
                PointerInput.Origin.viewport(),
                x,
                y
        ));

        tap.addAction(finger.createPointerDown(PointerInput.MouseButton.LEFT.asArg()));
        tap.addAction(finger.createPointerUp(PointerInput.MouseButton.LEFT.asArg()));

        driver.perform(Collections.singletonList(tap));
    }

    private void longSwipeUp() {
        Dimension size = driver.manage().window().getSize();

        int startX = (int) (size.getWidth() * 0.50);
        int startY = (int) (size.getHeight() * 0.84);
        int endX = (int) (size.getWidth() * 0.50);
        int endY = (int) (size.getHeight() * 0.22);

        swipeByCoordinates(startX, startY, endX, endY, 850);
    }

    private void swipeUp() {
        Dimension size = driver.manage().window().getSize();

        int startX = (int) (size.getWidth() * 0.50);
        int startY = (int) (size.getHeight() * 0.78);
        int endX = (int) (size.getWidth() * 0.50);
        int endY = (int) (size.getHeight() * 0.30);

        swipeByCoordinates(startX, startY, endX, endY, 650);
    }

    private void controlledDocumentSwipeUp() {
        Dimension size = driver.manage().window().getSize();

        int startX = (int) (size.getWidth() * 0.50);
        int startY = (int) (size.getHeight() * 0.74);
        int endX = (int) (size.getWidth() * 0.50);
        int endY = (int) (size.getHeight() * 0.34);

        swipeByCoordinates(startX, startY, endX, endY, 520);
    }

    private void smallSwipeUp() {
        Dimension size = driver.manage().window().getSize();

        int startX = (int) (size.getWidth() * 0.50);
        int startY = (int) (size.getHeight() * 0.66);
        int endX = (int) (size.getWidth() * 0.50);
        int endY = (int) (size.getHeight() * 0.45);

        swipeByCoordinates(startX, startY, endX, endY, 450);
    }

    private void smallSwipeDown() {
        Dimension size = driver.manage().window().getSize();

        int startX = (int) (size.getWidth() * 0.50);
        int startY = (int) (size.getHeight() * 0.45);
        int endX = (int) (size.getWidth() * 0.50);
        int endY = (int) (size.getHeight() * 0.68);

        swipeByCoordinates(startX, startY, endX, endY, 450);
    }

    private void swipeByCoordinates(int startX, int startY, int endX, int endY, long durationMillis) {
        PointerInput finger = new PointerInput(PointerInput.Kind.TOUCH, "finger");
        Sequence swipe = new Sequence(finger, 1);

        swipe.addAction(finger.createPointerMove(
                Duration.ZERO,
                PointerInput.Origin.viewport(),
                startX,
                startY
        ));

        swipe.addAction(finger.createPointerDown(PointerInput.MouseButton.LEFT.asArg()));

        swipe.addAction(finger.createPointerMove(
                Duration.ofMillis(durationMillis),
                PointerInput.Origin.viewport(),
                endX,
                endY
        ));

        swipe.addAction(finger.createPointerUp(PointerInput.MouseButton.LEFT.asArg()));

        driver.perform(Collections.singletonList(swipe));
    }

    private void pressBackSilently() {
        try {
            driver.navigate().back();
        } catch (Exception e) {
            ReportLogger.debug("Back press failed: " + cleanError(e.getMessage()));
        }
    }

    // =========================================================
    // COMMON HELPERS
    // =========================================================

    private String getCurrentPackageSafely() {
        try {
            return driver.getCurrentPackage();
        } catch (Exception e) {
            return "";
        }
    }

    private boolean equalsOrContainsIgnoreCase(String source, String expected) {
        if (source == null || expected == null) {
            return false;
        }

        String cleanSource = normalizeSpaces(source).toLowerCase();
        String cleanExpected = normalizeSpaces(expected).toLowerCase();

        return cleanSource.equals(cleanExpected) || cleanSource.contains(cleanExpected);
    }

    private boolean containsIgnoreCase(String source, String expected) {
        if (source == null || expected == null) {
            return false;
        }

        return normalizeSpaces(source).toLowerCase().contains(normalizeSpaces(expected).toLowerCase());
    }

    private String normalizeSpaces(String value) {
        if (value == null) {
            return "";
        }

        return value
                .replace("\n", " ")
                .replace("\r", " ")
                .replace("\t", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private String normalizeForMatching(String value) {
        if (value == null) {
            return "";
        }

        return value
                .replace("&#10;", " ")
                .replace("&#xA;", " ")
                .replace("&#xa;", " ")
                .replace("&amp;", "&")
                .replace("&nbsp;", " ")
                .replace("\u00A0", " ")
                .replace("\n", " ")
                .replace("\r", " ")
                .replaceAll("\\s+", " ")
                .trim()
                .toLowerCase();
    }

    private String cleanError(String message) {
        if (message == null) {
            return "";
        }

        return normalizeSpaces(message);
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Sleep interrupted", e);
        }
    }
}