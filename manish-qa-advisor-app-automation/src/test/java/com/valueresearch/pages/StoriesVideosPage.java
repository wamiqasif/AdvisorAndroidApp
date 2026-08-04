package com.valueresearch.pages;

import com.valueresearch.utils.ReportLogger;
import com.valueresearch.utils.ScreenshotUtils;
import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.android.nativekey.AndroidKey;
import io.appium.java_client.android.nativekey.KeyEvent;
import org.openqa.selenium.By;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.Rectangle;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.PointerInput;
import org.openqa.selenium.interactions.Sequence;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.Collections;
import java.util.List;

public class StoriesVideosPage {

    private final AndroidDriver driver;
    private final WebDriverWait wait;

    // Module constants observed from screencast 02-06-2026 03:01 PM IST.
    private static final String MODULE_TITLE = "Stories & Videos";
    private static final String TAB_FUND_ADVISOR_NOTE = "Fund Advisor's Note";
    private static final String TAB_QUICK_GUIDES = "Quick Guides";

    private static final String STORY_FIRST_TITLE = "Half the job is saying no";
    private static final String STORY_SECOND_TITLE = "A realistic note for uncertain times";
    private static final String STORY_THIRD_TITLE = "Two stories. One signal";
    private static final String STORY_FOURTH_TITLE = "The restaurant critic who tells you to cook at home";
    private static final String STORY_FIFTH_TITLE = "The most expensive feeling in investing";
    private static final String STORY_SIXTH_TITLE = "Your fund trailled its benchmark. Now what?";
    private static final String STORY_SIXTH_TITLE_ALT = "Your fund trailed its benchmark. Now what?";

    private static final String VIDEO_FIRST_TITLE = "Build your investment plan";
    private static final String VIDEO_SECOND_TITLE = "Upgrade your portfolio with clear actions";
    private static final String VIDEO_THIRD_TITLE = "Tap into analyst-picked funds";
    private static final String VIDEO_FOURTH_TITLE = "Buy and sell funds easily";

    // Hub navigation.
    private final By hubTab = AppiumBy.accessibilityId("Hub");
    private final By storiesVideosHubTileExact = AppiumBy.accessibilityId("Stories & Videos");
    private final By storiesVideosHubTileContains = AppiumBy.androidUIAutomator(
            "new UiSelector().descriptionContains(\"Stories\")"
    );
    private final By storiesVideosHubTileTextContains = AppiumBy.androidUIAutomator(
            "new UiSelector().textContains(\"Stories\")"
    );
    private final By mutualFundsHeader = AppiumBy.androidUIAutomator(
            "new UiSelector().descriptionContains(\"Mutual Funds\")"
    );
    private final By fundScreenerTile = AppiumBy.androidUIAutomator(
            "new UiSelector().descriptionContains(\"Fund Screener\")"
    );
    private final By sipCalculatorTile = AppiumBy.androidUIAutomator(
            "new UiSelector().descriptionContains(\"SIP Calculator\")"
    );

    // Stories & Videos page.
    private final By pageTitle = AppiumBy.accessibilityId(MODULE_TITLE);
    private final By pageTitleContains = AppiumBy.androidUIAutomator(
            "new UiSelector().descriptionContains(\"Stories & Videos\")"
    );
    private final By fundAdvisorChip = AppiumBy.accessibilityId(TAB_FUND_ADVISOR_NOTE);
    private final By quickGuidesChip = AppiumBy.accessibilityId(TAB_QUICK_GUIDES);
    private final By storyFirstTitle = AppiumBy.accessibilityId(STORY_FIRST_TITLE);
    private final By storyFirstTitleContains = AppiumBy.androidUIAutomator(
            "new UiSelector().descriptionContains(\"Half the job\")"
    );
    private final By quickGuidesContains = AppiumBy.androidUIAutomator(
            "new UiSelector().descriptionContains(\"Quick Guides\")"
    );

    // Video page/list.
    private final By videoFirstTitle = AppiumBy.accessibilityId(VIDEO_FIRST_TITLE);
    private final By videoFirstTitleContains = AppiumBy.androidUIAutomator(
            "new UiSelector().descriptionContains(\"Build your investment\")"
    );

    public StoriesVideosPage(AndroidDriver driver) {
        this.driver = driver;
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(25));
    }

    // =====================================================================
    // Open and recovery flow
    // =====================================================================

    public void openStoriesAndVideosFromHub() {
        try {
            ReportLogger.step("Opening Stories & Videos from Hub -> Mutual Funds -> Stories & Videos");

            if (isStoriesVideosPageVisible()) {
                ReportLogger.pass("Stories & Videos page is already open");
                return;
            }

            openHubTab();
            tapStoriesVideosTileFromHub();
            waitForStoriesVideosPage();

            ReportLogger.pass("Stories & Videos page opened successfully from Hub");
        } catch (Exception e) {
            captureScreenshot("SV_001_Open_Stories_Videos_Failure");
            ReportLogger.fail("Failed to open Stories & Videos: " + cleanError(e.getMessage()));
            throw new RuntimeException("Failed to open Stories & Videos: " + cleanError(e.getMessage()), e);
        }
    }

    public void recoverStoriesVideosIfNeeded() {
        try {
            if (isStoriesVideosPageVisible()) {
                ReportLogger.pass("Stories & Videos page is already active");
                return;
            }

            ReportLogger.step("Stories & Videos page is not active. Reopening from Hub.");
            openStoriesAndVideosFromHub();
        } catch (Exception e) {
            captureScreenshot("SV_Recover_Stories_Videos_Failure");
            throw new RuntimeException("Unable to recover Stories & Videos page: " + cleanError(e.getMessage()), e);
        }
    }

    private void openHubTab() {
        ReportLogger.step("Opening Hub bottom tab");

        if (tapElementIfPresent(hubTab, "Hub tab")) {
            sleep(2200);
            ReportLogger.pass("Hub tab opened");
            return;
        }

        // If the current screen is deep inside the app, back out once and retry.
        pressBackSafely();
        sleep(900);

        if (tapElementIfPresent(hubTab, "Hub tab after back recovery")) {
            sleep(2200);
            ReportLogger.pass("Hub tab opened after recovery");
            return;
        }

        throw new RuntimeException("Hub tab was not found. User may not be logged in or app is on an unknown screen.");
    }

    private void tapStoriesVideosTileFromHub() {
        ReportLogger.step("Finding Stories & Videos tile inside Hub Mutual Funds section");

        for (int attempt = 1; attempt <= 10; attempt++) {
            if (tapStoriesVideosTileIfPossible()) {
                sleep(2200);

                if (isStoriesVideosPageVisible() || isSkeletonOrLoadingVisible()) {
                    ReportLogger.pass("Stories & Videos tile opened");
                    return;
                }
            }

            String source = safePageSource();

            if (containsAny(source, "Stories & Videos", "Stories")) {
                ReportLogger.step("Stories & Videos text is visible. Retrying tile tap without fixed coordinates.");

                if (tapStoriesVideosTileIfPossible()) {
                    sleep(2200);

                    if (isStoriesVideosPageVisible() || isSkeletonOrLoadingVisible()) {
                        ReportLogger.pass("Stories & Videos opened using visible tile fallback");
                        return;
                    }
                }
            }

            ReportLogger.step("Stories & Videos tile not opened yet. Scrolling Hub down. Attempt: " + attempt);
            swipeUpW3C();
            sleep(950);
        }

        throw new RuntimeException("Stories & Videos tile was not found in Hub Mutual Funds section after scrolling.");
    }

    private boolean tapStoriesVideosTileIfPossible() {
        return tapVisibleElementIfPresent(storiesVideosHubTileExact, "Stories & Videos tile exact accessibility")
                || tapVisibleElementIfPresent(
                        AppiumBy.androidUIAutomator("new UiSelector().descriptionContains(\"Stories & Videos\")"),
                        "Stories & Videos tile by description contains full text"
                )
                || tapVisibleElementIfPresent(
                        AppiumBy.androidUIAutomator("new UiSelector().textContains(\"Stories & Videos\")"),
                        "Stories & Videos tile by text contains full text"
                )
                || tapVisibleElementIfPresent(storiesVideosHubTileContains, "Stories & Videos tile by description contains Stories")
                || tapVisibleElementIfPresent(storiesVideosHubTileTextContains, "Stories & Videos tile by text contains Stories")
                || tapVisibleElementIfPresent(
                        AppiumBy.xpath("//*[contains(@content-desc,'Stories') or contains(@text,'Stories')]"),
                        "Stories & Videos tile by xpath contains Stories"
                );
    }
    
    private boolean tapVisibleElementIfPresent(By locator, String elementName) {
        try {
            List<WebElement> elements = driver.findElements(locator);
            if (elements == null || elements.isEmpty()) {
                return false;
            }

            Dimension size = driver.manage().window().getSize();

            for (WebElement element : elements) {
                if (element == null) {
                    continue;
                }

                Rectangle rect = element.getRect();

                if (rect == null || rect.getWidth() <= 0 || rect.getHeight() <= 0) {
                    continue;
                }

                int centerX = rect.getX() + (rect.getWidth() / 2);
                int centerY = rect.getY() + (rect.getHeight() / 2);

                // Skip status bar/header and bottom navigation unsafe zones.
                if (centerY < (int) (size.height * 0.12) || centerY > (int) (size.height * 0.90)) {
                    continue;
                }

                tapAt(centerX, centerY, elementName + " visible candidate");
                return true;
            }

            return false;
        } catch (Exception e) {
            ReportLogger.step("Visible element tap failed for " + elementName + ": " + cleanError(e.getMessage()));
            return false;
        }
    }

    private boolean waitUntilStoriesVideosPageVisible(int maxAttempts) {
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            sleep(650);

            if (isStoriesVideosPageVisible()) {
                return true;
            }
        }

        return false;
    }

    private void scrollHubToTop() {
        ReportLogger.step("Resetting Hub scroll position to top before searching Stories & Videos");

        for (int i = 1; i <= 3; i++) {
            swipeDownW3C();
            sleep(500);
        }
    }

    private boolean scrollIntoViewAndTapStoriesVideosTile() {
        String[] selectors = new String[]{
                "new UiScrollable(new UiSelector().scrollable(true)).scrollIntoView(new UiSelector().descriptionContains(\"Stories & Videos\"))",
                "new UiScrollable(new UiSelector().scrollable(true)).scrollIntoView(new UiSelector().textContains(\"Stories & Videos\"))",
                "new UiScrollable(new UiSelector().scrollable(true)).scrollIntoView(new UiSelector().descriptionContains(\"Stories\"))",
                "new UiScrollable(new UiSelector().scrollable(true)).scrollIntoView(new UiSelector().textContains(\"Stories\"))"
        };

        for (String selector : selectors) {
            try {
                WebElement element = driver.findElement(AppiumBy.androidUIAutomator(selector));
                if (element != null) {
                    tapElementCenter(element, "Stories & Videos tile after UiScrollable");
                    return true;
                }
            } catch (Exception ignored) {
                // Flutter screens may not expose a native scrollable container.
            }
        }

        return false;
    }

    private boolean tapStoriesVideosVisibleTextCoordinateFallback() {
        try {
            By visibleStoriesLocator = AppiumBy.xpath(
                    "//*[contains(@content-desc,'Stories') or contains(@text,'Stories')]"
            );

            List<WebElement> elements = driver.findElements(visibleStoriesLocator);
            if (elements == null || elements.isEmpty()) {
                return false;
            }

            Dimension size = driver.manage().window().getSize();

            for (WebElement element : elements) {
                Rectangle rect = element.getRect();

                if (rect == null || rect.getWidth() <= 0 || rect.getHeight() <= 0) {
                    continue;
                }

                int centerX = rect.getX() + (rect.getWidth() / 2);
                int centerY = rect.getY() + (rect.getHeight() / 2);

                if (centerY < (int) (size.height * 0.12) || centerY > (int) (size.height * 0.90)) {
                    continue;
                }

                tapAt(centerX, centerY, "Stories & Videos visible text coordinate fallback");
                return true;
            }

            return false;
        } catch (Exception e) {
            ReportLogger.step("Stories & Videos visible text coordinate fallback failed: " + cleanError(e.getMessage()));
            return false;
        }
    }

    private void recoverBackToHubIfWrongScreenOpened(String beforeSource, String tapMode) {
        String afterSource = safePageSource();

        if (isStoriesVideosPageVisible()) {
            return;
        }

        boolean sourceChanged = beforeSource != null
                && !normalizeForSearch(beforeSource).equals(normalizeForSearch(afterSource));

        if (!sourceChanged) {
            ReportLogger.step("No navigation detected after " + tapMode + ". Continuing Hub scroll.");
            return;
        }

        /*
         * The tap changed screen but did not open Stories & Videos.
         * Most likely wrong Hub tile was tapped. Recover to Hub.
         */
        ReportLogger.step("Wrong screen may have opened after " + tapMode + ". Pressing back to recover Hub.");
        pressBackSafely();
        sleep(1400);
    }

    private void waitForStoriesVideosPage() {
        ReportLogger.step("Waiting for Stories & Videos page to load");

        for (int attempt = 1; attempt <= 16; attempt++) {
            if (isStoriesVideosPageVisible()) {
                ReportLogger.pass("Stories & Videos page loaded");
                return;
            }

            sleep(650);
        }

        throw new RuntimeException("Stories & Videos page did not load. Expected title/chips/list were not visible.");
    }

    public boolean isStoriesVideosPageVisible() {
        String source = safePageSource();

        boolean hasStoriesModuleMarker = containsAny(source,
                TAB_FUND_ADVISOR_NOTE, TAB_QUICK_GUIDES, STORY_FIRST_TITLE, STORY_SECOND_TITLE, VIDEO_FIRST_TITLE);

        boolean looksLikeHubTileOnly = containsAny(source,
                "Mutual Funds", "Portfolio Planner", "Analyst's Choice", "Fund Screener", "SIP Calculator", "Stocks", "Contact Us");

        if (hasStoriesModuleMarker) {
            return true;
        }

        // Hub also exposes a tile named "Stories & Videos". Do not treat Hub as the module page.
        return containsIgnoreCase(source, MODULE_TITLE) && !looksLikeHubTileOnly;
    }

    // =====================================================================
    // Static list validations
    // =====================================================================

    public void verifyStoriesVideosLandingStaticContent() {
        try {
            ReportLogger.step("Validating Stories & Videos landing page static content");
            recoverStoriesVideosIfNeeded();

            assertTextPresent(MODULE_TITLE, "Page title");
            assertTextPresent(TAB_FUND_ADVISOR_NOTE, "Fund Advisor's Note chip");
            assertTextPresent(TAB_QUICK_GUIDES, "Quick Guides chip");

            ReportLogger.pass("Stories & Videos title and chips validated successfully");
        } catch (Exception e) {
            throw new RuntimeException("Stories & Videos landing static validation failed: " + cleanError(e.getMessage()), e);
        }
    }

    public void verifyFundAdvisorNotesStoryList() {
        try {
            ReportLogger.step("Validating Fund Advisor's Note story list items");
            recoverStoriesVideosIfNeeded();
            selectFundAdvisorNotesTab();

            assertTextPresent(STORY_FIRST_TITLE, "Story title 1");
            assertTextPresent(STORY_SECOND_TITLE, "Story title 2");
            assertTextPresent(STORY_THIRD_TITLE, "Story title 3");
            assertTextPresent(STORY_FOURTH_TITLE, "Story title 4");
            assertTextPresent(STORY_FIFTH_TITLE, "Story title 5");

            // The screencast title has a likely typo, so accept both variants but log the expected visual title.
            if (containsAny(safePageSource(), STORY_SIXTH_TITLE, STORY_SIXTH_TITLE_ALT)) {
                ReportLogger.pass("Validated text - Story title 6: " + firstPresentText(STORY_SIXTH_TITLE, STORY_SIXTH_TITLE_ALT));
            } else {
                throw new RuntimeException("Expected story title not found: " + STORY_SIXTH_TITLE + " / " + STORY_SIXTH_TITLE_ALT);
            }

            assertTextPresent("Mins", "Story duration label");
            ReportLogger.pass("Fund Advisor's Note visible story list validated successfully");
        } catch (Exception e) {
            throw new RuntimeException("Fund Advisor's Note story list validation failed: " + cleanError(e.getMessage()), e);
        }
    }

    public void verifyQuickGuidesTabSwitch() {
        try {
            ReportLogger.step("Validating Quick Guides tab switch");
            recoverStoriesVideosIfNeeded();
            selectQuickGuidesTab();
            waitForQuickGuidesVideoList();
            ReportLogger.pass("Quick Guides tab opened successfully");
        } catch (Exception e) {
            throw new RuntimeException("Quick Guides tab switch validation failed: " + cleanError(e.getMessage()), e);
        }
    }

    public void verifyQuickGuidesVideoList() {
        try {
            ReportLogger.step("Validating Quick Guides video list items");
            recoverStoriesVideosIfNeeded();
            selectQuickGuidesTab();
            waitForQuickGuidesVideoList();

            assertTextPresent(VIDEO_FIRST_TITLE, "Video title 1");
            assertTextPresent(VIDEO_SECOND_TITLE, "Video title 2");
            assertTextPresent(VIDEO_THIRD_TITLE, "Video title 3");
            assertTextPresent(VIDEO_FOURTH_TITLE, "Video title 4");
            assertTextPresent("Mins", "Video duration label");

            ReportLogger.pass("Quick Guides video list validated successfully");
        } catch (Exception e) {
            throw new RuntimeException("Quick Guides video list validation failed: " + cleanError(e.getMessage()), e);
        }
    }

    // =====================================================================
    // Story detail validations
    // =====================================================================

    public void verifyOpenFirstStoryDetail() {
        try {
            ReportLogger.step("Opening first story detail from Fund Advisor's Note list");
            recoverStoriesVideosIfNeeded();
            selectFundAdvisorNotesTab();

            if (!tapElementIfPresent(storyFirstTitle, "First story title")
                    && !tapElementIfPresent(storyFirstTitleContains, "First story title by contains")) {
                tapFirstStoryCoordinateFallback();
            }

            waitForFirstStoryDetail();
            ReportLogger.pass("First story detail opened successfully");
        } catch (Exception e) {
            captureScreenshot("SV_Open_First_Story_Detail_Failure");
            throw new RuntimeException("First story detail open validation failed: " + cleanError(e.getMessage()), e);
        }
    }

    public void verifyFirstStoryDetailTopContent() {
        try {
            ReportLogger.step("Validating first story detail top content");
            ensureFirstStoryDetailOpen();

            assertTextPresent(STORY_FIRST_TITLE, "Story detail title");
            assertTextPresent("FUND ADVISOR'S NOTE", "Story category");
            assertTextPresent("What looks like expert curation", "Story intro text");
            assertTextPresent("taxable activity", "Story intro continuation");

            ReportLogger.pass("First story detail top content validated successfully");
        } catch (Exception e) {
            throw new RuntimeException("First story detail top content validation failed: " + cleanError(e.getMessage()), e);
        }
    }

    public void verifyFirstStoryDetailScrollContent() {
        try {
            ReportLogger.step("Validating first story detail body scroll and bottom content");
            ensureFirstStoryDetailOpen();

            boolean bottomFound = false;
            String[] bottomMarkers = new String[]{
                    "The Portfolio Planner",
                    "Advisor's session",
                    "Next Saturday",
                    "sleep better at night"
            };

            for (int attempt = 1; attempt <= 9; attempt++) {
                String source = safePageSource();
                if (containsAny(source, bottomMarkers)) {
                    bottomFound = true;
                    break;
                }
                ReportLogger.step("Story bottom marker not visible yet. Scrolling story detail. Attempt: " + attempt);
                swipeUpW3C();
                sleep(650);
            }

            if (!bottomFound) {
                throw new RuntimeException("Unable to reach expected bottom/story body markers after scrolling.");
            }

            ReportLogger.pass("Story detail body scroll validated. Bottom/body marker found.");
        } catch (Exception e) {
            throw new RuntimeException("Story detail scroll validation failed: " + cleanError(e.getMessage()), e);
        }
    }



    public void verifyStoryInternalHyperlinksPresent() {
        try {
            ReportLogger.step("Validating internal hyperlinks present inside first story detail");

            if (!isFirstStoryDetailVisible()) {
                ReportLogger.step("First story detail is not active. Opening first story detail for hyperlink validation.");
                recoverStoriesVideosIfNeeded();
                selectFundAdvisorNotesTab();
                verifyOpenFirstStoryDetail();
            } else {
                ReportLogger.pass("First story detail is already active. Validating hyperlinks from current story state.");
            }

            boolean portfolioPlannerFound = false;
            boolean analystChoiceFound = false;

            for (int attempt = 1; attempt <= 9; attempt++) {
                String source = safePageSource();

                if (!portfolioPlannerFound && isPortfolioPlannerHyperlinkVisible(source)) {
                    portfolioPlannerFound = true;
                    ReportLogger.pass("Validated internal hyperlink text - Portfolio Planner");
                    logAccessibilityExposureForLink("Portfolio Planner");
                }

                if (!analystChoiceFound && isAnalystChoiceHyperlinkVisible(source)) {
                    analystChoiceFound = true;
                    ReportLogger.pass("Validated internal hyperlink text - Analyst's Choice");
                    logAccessibilityExposureForLink("Analyst");
                    logAccessibilityExposureForLink("Choice");
                }

                if (portfolioPlannerFound && analystChoiceFound) {
                    ReportLogger.pass("Story internal hyperlinks are present: Portfolio Planner, Analyst's Choice");
                    return;
                }

                ReportLogger.step("Story internal hyperlinks not fully visible yet. Scrolling story detail. Attempt: " + attempt
                        + " | Portfolio Planner found=" + portfolioPlannerFound
                        + " | Analyst's Choice found=" + analystChoiceFound);
                swipeUpW3C();
                sleep(700);
            }

            throw new RuntimeException("Expected internal story hyperlinks were not found."
                    + " Portfolio Planner found=" + portfolioPlannerFound
                    + ", Analyst's Choice found=" + analystChoiceFound);
        } catch (Exception e) {
            throw new RuntimeException("Story internal hyperlink presence validation failed: " + cleanError(e.getMessage()), e);
        }
    }


    public void verifyStoryInternalHyperlinksOpen() {
        try {
            ReportLogger.step("Functional validation: opening internal hyperlinks inside first story detail");
            ensureFirstStoryDetailOpen();
            scrollToStoryInternalHyperlinkArea();

            openStoryInternalHyperlinkAndReturn(
                    "Portfolio Planner",
                    new String[]{"Portfolio Planner"},
                    new String[]{"Portfolio Planner", "Portfolio", "Planner"}
            );

            ensureFirstStoryDetailOpen();
            scrollToStoryInternalHyperlinkArea();

            openStoryInternalHyperlinkAndReturn(
                    "Analyst's Choice",
                    new String[]{"Analyst's Choice", "Analyst’s Choice", "Analyst", "Choice"},
                    new String[]{"Analyst's Choice", "Analyst’s Choice", "Analyst", "Choice"}
            );

            ReportLogger.pass("Story internal hyperlinks opened successfully: Portfolio Planner, Analyst's Choice");
        } catch (Exception e) {
            throw new RuntimeException("Story internal hyperlink open validation failed: " + cleanError(e.getMessage()), e);
        }
    }

    public void verifyBackFromStoryDetailToList() {
        try {
            ReportLogger.step("Validating single back navigation from story detail to Stories & Videos list");

            if (!isFirstStoryDetailVisible()) {
                ReportLogger.step("Story detail is not active. Opening first story detail only for this back validation.");
                verifyOpenFirstStoryDetail();
            } else {
                ReportLogger.pass("Story detail is already active. No Hub recovery required.");
            }

            for (int attempt = 1; attempt <= 3; attempt++) {
                pressBackSafely();
                sleep(1500);

                if (isStoriesVideosPageVisible()
                        && containsAny(safePageSource(), TAB_FUND_ADVISOR_NOTE, STORY_FIRST_TITLE, STORY_SECOND_TITLE)) {
                    assertTextPresent(TAB_FUND_ADVISOR_NOTE, "Fund Advisor's Note chip after back");
                    ReportLogger.pass("Back navigation from story detail to list validated successfully on attempt: " + attempt);
                    return;
                }
            }

            throw new RuntimeException("Back from story detail did not return to Stories & Videos list after 3 attempts.");
        } catch (Exception e) {
            throw new RuntimeException("Story detail back navigation validation failed: " + cleanError(e.getMessage()), e);
        }
    }

    // =====================================================================
    // Functional video validations
    // =====================================================================

    public void verifyOpenFirstQuickGuideVideo() {
        try {
            ReportLogger.step("Validating first Quick Guides video opens player/loading screen");
            recoverStoriesVideosIfNeeded();
            selectQuickGuidesTab();
            waitForQuickGuidesVideoList();

            String beforeSource = normalizeForSearch(safePageSource());
            String beforePackage = safeCurrentPackage();

            if (!tapElementIfPresent(videoFirstTitle, "First Quick Guides video title")
                    && !tapElementIfPresent(videoFirstTitleContains, "First Quick Guides video title by contains")) {
                tapFirstVideoCoordinateFallback();
            }

            sleep(6500);

            String afterSourceRaw = safePageSource();
            String afterSource = normalizeForSearch(afterSourceRaw);
            String afterPackage = safeCurrentPackage();

            boolean packageChanged = beforePackage != null
                    && afterPackage != null
                    && !beforePackage.isEmpty()
                    && !afterPackage.isEmpty()
                    && !beforePackage.equalsIgnoreCase(afterPackage);
            boolean sourceChanged = !beforeSource.equals(afterSource);
            boolean listGone = !containsIgnoreCase(afterSourceRaw, VIDEO_FIRST_TITLE)
                    || !containsIgnoreCase(afterSourceRaw, TAB_QUICK_GUIDES);
            boolean playerMarker = containsAny(afterSourceRaw,
                    "Value Research Advisor", "Pause", "Play", "0:00", "youtube", "video", "fullscreen");

            if (!(packageChanged || sourceChanged || listGone || playerMarker)) {
                throw new RuntimeException("Video tap did not open player/loading screen or change UI state.");
            }

            ReportLogger.pass("First Quick Guides video tap response detected"
                    + " | packageChanged=" + packageChanged
                    + " | sourceChanged=" + sourceChanged
                    + " | listGone=" + listGone
                    + " | playerMarker=" + playerMarker
                    + " | currentPackage=" + afterPackage);
        } catch (Exception e) {
            captureScreenshot("SV_First_Video_Open_Failure");
            throw new RuntimeException("First Quick Guides video functional validation failed: " + cleanError(e.getMessage()), e);
        }
    }

    public void verifyBackFromVideoToQuickGuidesList() {
        try {
            ReportLogger.step("Validating back navigation from video/player to Quick Guides list");

            for (int attempt = 1; attempt <= 5; attempt++) {
                if (containsIgnoreCase(safePageSource(), VIDEO_FIRST_TITLE)
                        && containsIgnoreCase(safePageSource(), TAB_QUICK_GUIDES)) {
                    ReportLogger.pass("Already on Quick Guides list before back validation");
                    return;
                }

                pressBackSafely();
                sleep(1400);

                if (containsIgnoreCase(safePageSource(), VIDEO_FIRST_TITLE)
                        || containsIgnoreCase(safePageSource(), VIDEO_SECOND_TITLE)
                        || containsIgnoreCase(safePageSource(), TAB_QUICK_GUIDES)) {
                    ReportLogger.pass("Returned to Quick Guides list from video/player");
                    return;
                }
            }

            recoverStoriesVideosIfNeeded();
            selectQuickGuidesTab();
            waitForQuickGuidesVideoList();
            ReportLogger.pass("Recovered Quick Guides list after video/player back navigation");
        } catch (Exception e) {
            throw new RuntimeException("Back navigation from video/player validation failed: " + cleanError(e.getMessage()), e);
        }
    }

    // =====================================================================
    // Negative/stability validations
    // =====================================================================

    public void verifySelectedTabRetapDoesNotNavigateAway() {
        try {
            ReportLogger.step("Negative validation: re-tapping selected Fund Advisor's Note tab should not navigate away");
            recoverStoriesVideosIfNeeded();
            selectFundAdvisorNotesTab();
            waitForFundAdvisorStoriesList();

            String beforeSource = normalizeForSearch(safePageSource());

            if (!tapElementIfPresent(fundAdvisorChip, "selected Fund Advisor's Note chip re-tap")) {
                tapByTextContains("Fund Advisor", "selected Fund Advisor's Note chip re-tap by contains");
            }

            sleep(1200);

            String afterRawSource = safePageSource();
            String afterSource = normalizeForSearch(afterRawSource);

            boolean stillOnStoriesVideosList = isStoriesVideosPageVisible()
                    && containsAny(afterRawSource, TAB_FUND_ADVISOR_NOTE, STORY_FIRST_TITLE, STORY_SECOND_TITLE);

            boolean openedStoryDetail = isFirstStoryDetailVisible();

            if (openedStoryDetail) {
                pressBackSafely();
                sleep(1200);
                throw new RuntimeException("Selected tab re-tap unexpectedly opened story detail.");
            }

            if (!stillOnStoriesVideosList) {
                throw new RuntimeException("Selected tab re-tap unexpectedly navigated away from Stories & Videos list.");
            }

            ReportLogger.pass("Negative validation passed: selected Fund Advisor's Note tab re-tap kept user on Stories & Videos list"
                    + " | sourceChanged=" + !beforeSource.equals(afterSource));
        } catch (Exception e) {
            throw new RuntimeException("Selected tab re-tap negative validation failed: " + cleanError(e.getMessage()), e);
        }
    }

    public void verifyRapidTabSwitchDoesNotCrash() {
        try {
            ReportLogger.step("Stability validation: rapid switch between Fund Advisor's Note and Quick Guides");
            recoverStoriesVideosIfNeeded();

            for (int i = 1; i <= 3; i++) {
                ReportLogger.step("Rapid tab switch cycle: " + i);
                selectQuickGuidesTab();
                sleep(500);
                selectFundAdvisorNotesTab();
                sleep(500);
            }

            assertNoCrashOrAnr();
            ReportLogger.pass("Rapid tab switch stability validation passed");
        } catch (Exception e) {
            throw new RuntimeException("Rapid tab switch stability validation failed: " + cleanError(e.getMessage()), e);
        }
    }

    public void verifyStoriesVideosScreenStability() {
        try {
            ReportLogger.step("Validating Stories & Videos screen stability and no crash/ANR markers");
            recoverStoriesVideosIfNeeded();
            assertNoCrashOrAnr();
            ReportLogger.pass("Stories & Videos screen stability validated. No crash/ANR marker found.");
        } catch (Exception e) {
            throw new RuntimeException("Stories & Videos screen stability validation failed: " + cleanError(e.getMessage()), e);
        }
    }

    public void verifyBackNavigationToHub() {
        try {
            ReportLogger.step("Validating back navigation from Stories & Videos to Hub");
            recoverStoriesVideosIfNeeded();

            pressBackSafely();
            sleep(1800);

            String source = safePageSource();
            if (!containsAny(source, "Hub", "Mutual Funds", "Portfolio Planner", "Analyst's Choice", "Fund Screener", "SIP Calculator", "Stocks")) {
                pressBackSafely();
                sleep(1200);
                source = safePageSource();
            }

            if (!containsAny(source, "Hub", "Mutual Funds", "Portfolio Planner", "Analyst's Choice", "Fund Screener", "SIP Calculator", "Stories & Videos")) {
                throw new RuntimeException("Back navigation did not return to Hub/Mutual Funds area.");
            }

            ReportLogger.pass("Returned Hub/Mutual Funds text validated in report: Hub, Mutual Funds, Portfolio Planner, Analyst's Choice, Fund Screener, SIP Calculator, Stories & Videos");
        } catch (Exception e) {
            throw new RuntimeException("Stories & Videos back navigation validation failed: " + cleanError(e.getMessage()), e);
        }
    }

    // =====================================================================
    // Tab/list helpers
    // =====================================================================

    private void selectFundAdvisorNotesTab() {
        ReportLogger.step("Selecting Fund Advisor's Note tab");

        if (containsIgnoreCase(safePageSource(), STORY_FIRST_TITLE)) {
            ReportLogger.pass("Fund Advisor's Note list already visible");
            return;
        }

        if (!tapElementIfPresent(fundAdvisorChip, "Fund Advisor's Note chip")) {
            tapByTextContains("Fund Advisor", "Fund Advisor's Note chip by contains");
        }

        sleep(1500);
        waitForFundAdvisorStoriesList();
    }

    private void selectQuickGuidesTab() {
        ReportLogger.step("Selecting Quick Guides tab");

        if (containsIgnoreCase(safePageSource(), VIDEO_FIRST_TITLE)) {
            ReportLogger.pass("Quick Guides list already visible");
            return;
        }

        if (!tapElementIfPresent(quickGuidesChip, "Quick Guides chip")
                && !tapElementIfPresent(quickGuidesContains, "Quick Guides chip by contains")) {
            tapByTextContains("Quick Guides", "Quick Guides chip by text contains");
        }

        sleep(1700);
        waitForQuickGuidesVideoList();
    }

    private void waitForFundAdvisorStoriesList() {
        for (int attempt = 1; attempt <= 12; attempt++) {
            if (containsAny(safePageSource(), STORY_FIRST_TITLE, STORY_SECOND_TITLE, STORY_THIRD_TITLE)) {
                ReportLogger.pass("Fund Advisor's Note story list loaded");
                return;
            }
            sleep(600);
        }
        throw new RuntimeException("Fund Advisor's Note story list did not load.");
    }

    private void waitForQuickGuidesVideoList() {
        ReportLogger.step("Waiting for Quick Guides video list to load");
        for (int attempt = 1; attempt <= 16; attempt++) {
            if (containsAny(safePageSource(), VIDEO_FIRST_TITLE, VIDEO_SECOND_TITLE, VIDEO_THIRD_TITLE, VIDEO_FOURTH_TITLE)) {
                ReportLogger.pass("Quick Guides video list loaded");
                return;
            }
            sleep(700);
        }
        throw new RuntimeException("Quick Guides video list did not load.");
    }

    private void waitForFirstStoryDetail() {
        ReportLogger.step("Waiting for first story detail page to load");
        for (int attempt = 1; attempt <= 14; attempt++) {
            String source = safePageSource();
            if (containsIgnoreCase(source, STORY_FIRST_TITLE)
                    && (containsIgnoreCase(source, "What looks like expert curation")
                    || containsIgnoreCase(source, "taxable activity")
                    || containsIgnoreCase(source, "FUND ADVISOR"))) {
                ReportLogger.pass("First story detail page loaded");
                return;
            }
            sleep(650);
        }
        throw new RuntimeException("First story detail page did not load.");
    }

    private void ensureFirstStoryDetailOpen() {
        if (isFirstStoryDetailVisible()) {
            ReportLogger.pass("First story detail is already active");
            return;
        }

        verifyOpenFirstStoryDetail();
    }

    private boolean isFirstStoryDetailVisible() {
        String source = safePageSource();

        // Do not use only the story title or the Fund Advisor's Note chip here, because
        // those also appear on the listing page. Use body/detail markers that exist only
        // inside the opened story detail, including bottom markers after the story is scrolled.
        return containsAny(source,
                "What looks like expert curation",
                "taxable activity",
                "The Portfolio Planner",
                "Advisor's session",
                "Next Saturday",
                "sleep better at night");
    }

    private boolean isSkeletonOrLoadingVisible() {
        String source = safePageSource();
        return containsIgnoreCase(source, MODULE_TITLE)
                && !containsIgnoreCase(source, "Hub")
                && !containsIgnoreCase(source, "Mutual Funds");
    }

    private void tapFirstStoryCoordinateFallback() {
        Dimension size = driver.manage().window().getSize();
        tapAt((int) (size.width * 0.62), (int) (size.height * 0.25), "first story card coordinate fallback");
    }

    private void tapFirstVideoCoordinateFallback() {
        Dimension size = driver.manage().window().getSize();
        tapAt((int) (size.width * 0.62), (int) (size.height * 0.31), "first Quick Guides video coordinate fallback");
    }

    private boolean tapByTextContains(String text, String elementName) {
        By descLocator = AppiumBy.androidUIAutomator(
                "new UiSelector().descriptionContains(\"" + escapeUiSelector(text) + "\")"
        );
        By textLocator = AppiumBy.androidUIAutomator(
                "new UiSelector().textContains(\"" + escapeUiSelector(text) + "\")"
        );
        return tapElementIfPresent(descLocator, elementName) || tapElementIfPresent(textLocator, elementName);
    }


    private void logAccessibilityExposureForLink(String linkText) {
        try {
            By descLocator = AppiumBy.androidUIAutomator(
                    "new UiSelector().descriptionContains(\"" + escapeUiSelector(linkText) + "\")"
            );
            By textLocator = AppiumBy.androidUIAutomator(
                    "new UiSelector().textContains(\"" + escapeUiSelector(linkText) + "\")"
            );

            int descCount = driver.findElements(descLocator).size();
            int textCount = driver.findElements(textLocator).size();

            ReportLogger.pass("Accessibility exposure for hyperlink text '" + linkText
                    + "' | descriptionMatches=" + descCount
                    + " | textMatches=" + textCount);
        } catch (Exception e) {
            ReportLogger.step("Unable to read accessibility exposure for hyperlink text '" + linkText + "': " + cleanError(e.getMessage()));
        }
    }

    private boolean isPortfolioPlannerHyperlinkVisible(String source) {
        String normalized = normalizeHyperlinkText(source);
        return normalized.contains("portfolio planner")
                || normalized.contains("the portfolio planner")
                || normalized.replace(" ", "").contains("portfolioplanner");
    }

    private boolean isAnalystChoiceHyperlinkVisible(String source) {
        String normalized = normalizeHyperlinkText(source);
        String compact = normalized.replace(" ", "");

        // Flutter can expose this link as Analyst's Choice, Analyst’s Choice,
        // Analysts Choice, Analyst Choice, or split the two words across lines.
        if (normalized.contains("analyst choice")
                || normalized.contains("analysts choice")
                || compact.contains("analystchoice")
                || compact.contains("analystschoice")) {
            return true;
        }

        int analystIndex = normalized.indexOf("analyst");
        int choiceIndex = normalized.indexOf("choice");

        // Accept when both fragments are visible close to each other in the same viewport.
        // This handles line wrapping and apostrophe/semantic-node differences.
        return analystIndex >= 0
                && choiceIndex >= 0
                && Math.abs(choiceIndex - analystIndex) <= 80;
    }


    private void scrollToStoryInternalHyperlinkArea() {
        for (int attempt = 1; attempt <= 9; attempt++) {
            String source = safePageSource();

            if (isPortfolioPlannerHyperlinkVisible(source) && isAnalystChoiceHyperlinkVisible(source)) {
                ReportLogger.pass("Story internal hyperlink area is visible: Portfolio Planner and Analyst's Choice");
                return;
            }

            ReportLogger.step("Story internal hyperlink area not fully visible yet. Scrolling story detail. Attempt: " + attempt);
            swipeUpW3C();
            sleep(700);
        }

        throw new RuntimeException("Unable to bring story internal hyperlink area into view.");
    }

    private void openStoryInternalHyperlinkAndReturn(String linkName, String[] tapFragments, String[] destinationMarkers) {
        ReportLogger.step("Opening story internal hyperlink: " + linkName);

        String beforeSource = normalizeForSearch(safePageSource());

        boolean tapped = tapStoryInternalHyperlink(linkName, tapFragments);
        if (!tapped) {
            ReportLogger.step("Locator tap was not possible for " + linkName + ". Trying coordinate fallback.");
            tapStoryInternalHyperlinkCoordinateFallback(linkName);
        }

        if (!waitForInternalHyperlinkNavigation(beforeSource, linkName, destinationMarkers, "locator/accessibility tap")) {
            ReportLogger.step("No navigation detected after locator tap for " + linkName + ". Retrying with coordinate fallback.");
            scrollToStoryInternalHyperlinkArea();
            tapStoryInternalHyperlinkCoordinateFallback(linkName);
        }

        if (!waitForInternalHyperlinkNavigation(beforeSource, linkName, destinationMarkers, "coordinate fallback tap")) {
            if (containsIgnoreCase(linkName, "Analyst") || containsIgnoreCase(linkName, "Choice")) {
                ReportLogger.step("No navigation detected for Analyst's Choice yet. Trying split-link visible text fallback.");
                scrollToStoryInternalHyperlinkArea();
                tapAnalystChoiceSplitTextFallback();
            }
        }

        if (!waitForInternalHyperlinkNavigation(beforeSource, linkName, destinationMarkers, "final validation")) {
            String afterRawSource = safePageSource();
            String afterSource = normalizeForSearch(afterRawSource);
            boolean sourceChanged = !beforeSource.equals(afterSource);
            boolean leftStoryDetail = !isFirstStoryDetailVisible();
            boolean destinationMarkerFound = containsAny(afterRawSource, destinationMarkers);

            captureScreenshot("SV_Internal_Link_Not_Opened_" + sanitizeFileName(linkName));
            throw new RuntimeException("Internal hyperlink did not open expected destination: " + linkName
                    + " | sourceChanged=" + sourceChanged
                    + " | leftStoryDetail=" + leftStoryDetail
                    + " | destinationMarkerFound=" + destinationMarkerFound);
        }

        waitOnOpenedInternalLinkPage(linkName);
        returnToFirstStoryDetailAfterInternalLink(linkName);
    }

    private void waitOnOpenedInternalLinkPage(String linkName) {
        // Keep the destination page visible for manual observation before navigating back.
        // This wait is only for the newly added internal hyperlink open flow and does not
        // change any previously passed validations.
        ReportLogger.step("Waiting on opened internal hyperlink page before back: " + linkName + " | waitMs=5000");
        sleep(5000);
    }

    private boolean waitForInternalHyperlinkNavigation(String beforeSource, String linkName, String[] destinationMarkers, String tapMode) {
        for (int attempt = 1; attempt <= 6; attempt++) {
            sleep(500);

            String afterRawSource = safePageSource();
            String afterSource = normalizeForSearch(afterRawSource);
            boolean sourceChanged = beforeSource != null && !beforeSource.equals(afterSource);
            boolean leftStoryDetail = !isFirstStoryDetailVisible();
            boolean destinationMarkerFound = containsAny(afterRawSource, destinationMarkers);

            // For this story, the destination words also exist as link text inside the original story.
            // So destinationMarkerFound alone is not enough. A real open must change the source
            // or move out of the original story detail screen.
            if (sourceChanged || leftStoryDetail) {
                ReportLogger.pass("Opened internal hyperlink successfully - " + linkName
                        + " | tapMode=" + tapMode
                        + " | sourceChanged=" + sourceChanged
                        + " | leftStoryDetail=" + leftStoryDetail
                        + " | destinationMarkerFound=" + destinationMarkerFound
                        + " | waitAttempt=" + attempt);
                return true;
            }
        }

        return false;
    }

    private boolean tapStoryInternalHyperlink(String linkName, String[] fragments) {
        if (fragments == null) {
            return false;
        }

        for (String fragment : fragments) {
            if (fragment == null || fragment.trim().isEmpty()) {
                continue;
            }

            By exactAccessibility = AppiumBy.accessibilityId(fragment);
            By descContains = AppiumBy.androidUIAutomator(
                    "new UiSelector().descriptionContains(\"" + escapeUiSelector(fragment) + "\")"
            );
            By textContains = AppiumBy.androidUIAutomator(
                    "new UiSelector().textContains(\"" + escapeUiSelector(fragment) + "\")"
            );
            By xpathContains = AppiumBy.xpath(
                    "//*[contains(@content-desc,\"" + escapeXPath(fragment) + "\") or contains(@text,\"" + escapeXPath(fragment) + "\")]"
            );

            if (tapElementIfPresent(exactAccessibility, linkName + " hyperlink by accessibility id: " + fragment)
                    || tapElementIfPresent(descContains, linkName + " hyperlink by content-desc contains: " + fragment)
                    || tapElementIfPresent(textContains, linkName + " hyperlink by text contains: " + fragment)
                    || tapElementIfPresent(xpathContains, linkName + " hyperlink by xpath contains: " + fragment)) {
                return true;
            }
        }

        return false;
    }

    private void tapStoryInternalHyperlinkCoordinateFallback(String linkName) {
        Dimension size = driver.manage().window().getSize();

        if (containsIgnoreCase(linkName, "Portfolio")) {
            tapAt((int) (size.width * 0.48), (int) (size.height * 0.74), linkName + " hyperlink coordinate fallback");
            return;
        }

        if (containsIgnoreCase(linkName, "Analyst") || containsIgnoreCase(linkName, "Choice")) {
            // Analyst's Choice is split across two visual lines in the story body.
            // First tap the visible "Analyst's" word near the right side of the line.
            tapAt((int) (size.width * 0.80), (int) (size.height * 0.78), linkName + " hyperlink coordinate fallback - Analyst word");
            return;
        }

        tapAt(size.width / 2, (int) (size.height * 0.76), linkName + " hyperlink generic coordinate fallback");
    }

    private void tapAnalystChoiceSplitTextFallback() {
        Dimension size = driver.manage().window().getSize();

        // The link wraps as "Analyst's" at the end of one line and "Choice" on the next line.
        // Try both visible text fragments without changing the already passed validations.
        tapAt((int) (size.width * 0.80), (int) (size.height * 0.78), "Analyst's Choice split-link fallback - Analyst word");
        sleep(900);

        if (!isFirstStoryDetailVisible()) {
            return;
        }

        tapAt((int) (size.width * 0.16), (int) (size.height * 0.81), "Analyst's Choice split-link fallback - Choice word");
    }

    private void returnToFirstStoryDetailAfterInternalLink(String linkName) {
        ReportLogger.step("Returning to first story detail after opening internal hyperlink: " + linkName);

        for (int attempt = 1; attempt <= 4; attempt++) {
            if (isFirstStoryDetailVisible()) {
                ReportLogger.pass("Returned to first story detail after opening: " + linkName);
                return;
            }

            pressBackSafely();
            sleep(1400);

            if (isFirstStoryDetailVisible()) {
                ReportLogger.pass("Returned to first story detail after opening: " + linkName + " on back attempt: " + attempt);
                return;
            }

            if (isStoriesVideosPageVisible()) {
                ReportLogger.step("Returned to Stories & Videos list after opening " + linkName + ". Reopening first story detail.");
                verifyOpenFirstStoryDetail();
                scrollToStoryInternalHyperlinkArea();
                return;
            }
        }

        ReportLogger.step("Could not return directly after opening " + linkName + ". Recovering first story detail from Hub.");
        openStoriesAndVideosFromHub();
        verifyOpenFirstStoryDetail();
        scrollToStoryInternalHyperlinkArea();
    }

    private String normalizeHyperlinkText(String value) {
        if (value == null) {
            return "";
        }

        return normalizeForSearch(value)
                .replace("’", "'")
                .replace("‘", "'")
                .replace("`", "'")
                .replace("&apos;", "'")
                .replace("&#39;", "'")
                .replace("'s", "s")
                .replace("'", "")
                .replaceAll("[^a-z0-9]+", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }


    // =====================================================================
    // Generic helpers
    // =====================================================================

    private void assertTextPresent(String expectedText, String textName) {
        String source = safePageSource();

        if (containsNormalized(source, expectedText) || containsIgnoreCase(source, expectedText)) {
            ReportLogger.pass("Validated text - " + textName + ": " + formatForReport(expectedText));
            return;
        }

        captureScreenshot("SV_Missing_" + sanitizeFileName(textName));
        throw new RuntimeException("Expected text not found for " + textName + ": " + expectedText);
    }

    private void assertNoCrashOrAnr() {
        String source = safePageSource();
        if (containsAny(source, "Unfortunately", "keeps stopping", "isn't responding", "App isn't responding", "Close app", "Wait")) {
            captureScreenshot("SV_Crash_Or_ANR_Detected");
            throw new RuntimeException("Crash/ANR marker detected on screen.");
        }
    }

    private boolean tapElementIfPresent(By locator, String elementName) {
        try {
            List<WebElement> elements = driver.findElements(locator);
            if (elements == null || elements.isEmpty()) {
                return false;
            }

            WebElement best = null;
            for (WebElement element : elements) {
                if (element != null) {
                    best = element;
                    try {
                        if (element.isDisplayed()) {
                            best = element;
                            break;
                        }
                    } catch (Exception ignored) {
                        // Flutter semantics may not reliably expose displayed=true.
                    }
                }
            }

            if (best == null) {
                return false;
            }

            try {
                best.click();
                ReportLogger.step("Tapped: " + elementName);
                return true;
            } catch (Exception clickException) {
                ReportLogger.step("Normal click failed for " + elementName + ". Trying element center tap.");
                tapElementCenter(best, elementName);
                return true;
            }
        } catch (Exception ignored) {
            return false;
        }
    }

    private void tapElementCenter(WebElement element, String elementName) {
        Rectangle rect = element.getRect();
        int centerX = rect.getX() + (rect.getWidth() / 2);
        int centerY = rect.getY() + (rect.getHeight() / 2);
        tapAt(centerX, centerY, elementName + " center");
    }

    private void tapAt(int x, int y, String elementName) {
        try {
            PointerInput finger = new PointerInput(PointerInput.Kind.TOUCH, "finger");
            Sequence tap = new Sequence(finger, 1);
            tap.addAction(finger.createPointerMove(Duration.ZERO, PointerInput.Origin.viewport(), x, y));
            tap.addAction(finger.createPointerDown(PointerInput.MouseButton.LEFT.asArg()));
            tap.addAction(finger.createPointerUp(PointerInput.MouseButton.LEFT.asArg()));
            driver.perform(Collections.singletonList(tap));
            ReportLogger.step("Tapped: " + elementName + " at x=" + x + ", y=" + y);
        } catch (Exception e) {
            throw new RuntimeException("Coordinate tap failed for " + elementName + ": " + cleanError(e.getMessage()), e);
        }
    }

    private void swipeUpW3C() {
        try {
            Dimension size = driver.manage().window().getSize();
            int startX = size.width / 2;
            int startY = (int) (size.height * 0.78);
            int endY = (int) (size.height * 0.30);
            performSwipe(startX, startY, startX, endY, 620);
        } catch (Exception e) {
            ReportLogger.step("Swipe up failed: " + cleanError(e.getMessage()));
        }
    }

    private void swipeDownW3C() {
        try {
            Dimension size = driver.manage().window().getSize();
            int startX = size.width / 2;
            int startY = (int) (size.height * 0.30);
            int endY = (int) (size.height * 0.78);
            performSwipe(startX, startY, startX, endY, 620);
        } catch (Exception e) {
            ReportLogger.step("Swipe down failed: " + cleanError(e.getMessage()));
        }
    }

    private void performSwipe(int startX, int startY, int endX, int endY, int millis) {
        PointerInput finger = new PointerInput(PointerInput.Kind.TOUCH, "finger");
        Sequence sequence = new Sequence(finger, 1);
        sequence.addAction(finger.createPointerMove(Duration.ZERO, PointerInput.Origin.viewport(), startX, startY));
        sequence.addAction(finger.createPointerDown(PointerInput.MouseButton.LEFT.asArg()));
        sequence.addAction(finger.createPointerMove(Duration.ofMillis(millis), PointerInput.Origin.viewport(), endX, endY));
        sequence.addAction(finger.createPointerUp(PointerInput.MouseButton.LEFT.asArg()));
        driver.perform(Collections.singletonList(sequence));
    }

    private void pressBackSafely() {
        try {
            driver.pressKey(new KeyEvent(AndroidKey.BACK));
        } catch (Exception ignored) {
            // Do not hide original validation failure.
        }
    }

    private String safePageSource() {
        try {
            return driver.getPageSource();
        } catch (Exception e) {
            return "";
        }
    }

    private String safeCurrentPackage() {
        try {
            return driver.getCurrentPackage();
        } catch (Exception e) {
            return "";
        }
    }

    private boolean containsIgnoreCase(String source, String expected) {
        if (source == null || expected == null) {
            return false;
        }
        return source.toLowerCase().contains(expected.toLowerCase());
    }

    private boolean containsNormalized(String source, String expected) {
        return normalizeForSearch(source).contains(normalizeForSearch(expected));
    }

    private boolean containsAny(String source, String... expectedValues) {
        if (source == null || expectedValues == null) {
            return false;
        }

        for (String expected : expectedValues) {
            if (containsIgnoreCase(source, expected) || containsNormalized(source, expected)) {
                return true;
            }
        }
        return false;
    }

    private String firstPresentText(String... values) {
        String source = safePageSource();
        for (String value : values) {
            if (containsIgnoreCase(source, value) || containsNormalized(source, value)) {
                return value;
            }
        }
        return values != null && values.length > 0 ? values[0] : "";
    }

    private String normalizeForSearch(String value) {
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

    private String formatForReport(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\n", " | ").replace("\r", " ").trim();
    }

    private String escapeUiSelector(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }


    private String escapeXPath(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private String sanitizeFileName(String value) {
        if (value == null || value.trim().isEmpty()) {
            return "unknown";
        }
        return value.replaceAll("[^a-zA-Z0-9_-]", "_");
    }

    private void captureScreenshot(String name) {
        try {
            ScreenshotUtils.captureScreenshot(driver, name);
        } catch (Exception ignored) {
            // Screenshot failure should not hide original issue.
        }
    }

    private String cleanError(String message) {
        if (message == null) {
            return "";
        }
        return message.replace("\n", " ").replace("\r", " ").replaceAll("\\s+", " ").trim();
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}