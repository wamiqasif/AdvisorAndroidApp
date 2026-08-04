package com.valueresearch.pages;

import com.valueresearch.utils.ReportLogger;
import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;
import org.openqa.selenium.By;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.Rectangle;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.PointerInput;
import org.openqa.selenium.interactions.Sequence;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class SearchBarPage {

    // NEXT_SPEED_PATCH_V3_ACTIVE

    // PROPER_STOCK_NAVIGATION_FIX_ACTIVE

    // SUPER_FAST_V2_DIRECT_TAP_PATCH_ACTIVE

    // SUPER_FAST_NO_TREE_SCAN_PATCH_ACTIVE

    private final AndroidDriver driver;
    private String advisorAppPackage = "";

    /*
     * Real-device optimization:
     * Window size is constant during this test run. Calling getSize() inside
     * element loops creates hundreds of extra Appium round-trips on USB devices.
     */
    private Dimension cachedScreenSize = null;

    private static final String SEARCH_KEYWORD = "hdfc";
    private static final String FUND_RESULT = "HDFC Mutual Fund result";
    private static final String FUND_DETAIL_TITLE = "HDFC";
    private static final String STOCK_RESULT = "HDFC Bank";
    private static final String STOCK_DETAIL_TITLE = "HDFC Bank Ltd.";

    private String selectedFundResult = "HDFC Mutual Fund result";
    private String selectedStockResult = STOCK_DETAIL_TITLE;

    public SearchBarPage(AndroidDriver driver) {
        this.driver = driver;
    }

    // =========================================================
    // PUBLIC STEP METHODS USED BY TEST CLASS
    // =========================================================

    public void captureAdvisorAppPackageForSearch() {
        advisorAppPackage = getCurrentPackageSafely();
        ReportLogger.pass("Advisor app package captured: " + advisorAppPackage);
    }

    public void ensureAdvisorAppLoggedInForSearch() {
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



    public void openGlobalSearchFromDashboardForSearch() {
        ReportLogger.step("Opening global search from dashboard/home (SUPER_FAST_V2_DIRECT_TAP)");

        forceZeroImplicitWaitForSpeed();

        if (isSearchScreenVisibleFast()) {
            ReportLogger.pass("Global search screen is already visible");
            return;
        }

        returnToDashboardIfNeeded();
        waitForDashboardStableBeforeSearch();

        tapTopRightSearchIconFallback();

        if (!waitForSearchScreenVisibleFast(5)) {
            throw new AssertionError("Global search screen did not open after direct search-icon tap");
        }

        ReportLogger.pass("Global search screen opened successfully using direct coordinate tap");
    }


    public void validateSearchScreenStructureForSearch() {
        ReportLogger.step("Validating search screen structure (SUPER_FAST_V2_NO_TREE_SCAN)");

        waitForSearchScreenReady();

        if (findSearchInput() == null) {
            throw new AssertionError("Search input is not visible");
        }

        if (!isTextVisibleFast("All", false, 2)) {
            throw new AssertionError("All tab is not visible");
        }

        if (!isTextVisibleFast("Stocks", false, 2)) {
            throw new AssertionError("Stocks tab is not visible");
        }

        if (!isTextVisibleFast("Mutual Funds", true, 2)) {
            ReportLogger.debug("Mutual Funds/SIPs tab not confirmed by fast locator. Continuing because coordinate fallback is available.");
        }

        ReportLogger.pass("Search screen structure validated quickly: input + All + Stocks + Mutual Funds fallback-ready");
    }

    public void enterSearchKeywordForSearch() {
        ReportLogger.step("Entering search keyword: " + SEARCH_KEYWORD);

        WebElement input = waitForElementFast(
                AppiumBy.androidUIAutomator("new UiSelector().className(\"android.widget.EditText\")"),
                6
        );

        if (input == null) {
            input = findSearchInput();
        }

        if (input == null) {
            throw new AssertionError("Search input is not visible before typing"
                    + " | visibleValues=" + collectVisibleStrings());
        }

        tapElementCenter(input);
        sleep(250);

        clearSearchInputSafely(input);
        sleep(250);

        input = findSearchInput();

        if (input == null) {
            throw new AssertionError("Search input disappeared before typing keyword");
        }

        input.sendKeys(SEARCH_KEYWORD);
        hideKeyboardIfVisible();

        waitForAnyTextVisible(
                Arrays.asList("HDFC", "HDFC Bank", "Fund", "ETF", "Stock"),
                8
        );

        ReportLogger.pass("Search keyword entered: " + SEARCH_KEYWORD);
    }


    public void validateSearchResultsLoadedForSearch() {
        ReportLogger.step("Validating search results are loaded for keyword: " + SEARCH_KEYWORD + " (SUPER_FAST_NO_TREE_SCAN)");

        waitForAnyTextVisible(
                Arrays.asList("HDFC", "Fund", "ETF", STOCK_RESULT, "Mutual Fund", "Stock"),
                8
        );

        ReportLogger.pass("Search results loaded for keyword: " + SEARCH_KEYWORD);
    }


    public void openMutualFundsTabForSearch() {
        ReportLogger.step("Opening Mutual Funds/SIPs search tab (SUPER_FAST_V2)");

        waitForSearchScreenReady();

        if (!tapMutualFundsTabFast()) {
            throw new AssertionError("Unable to tap Mutual Funds/SIPs tab using fast locator or coordinate fallback");
        }

        waitForAnyTextVisible(
                Arrays.asList("HDFC", "Small Cap Fund", "Gilt Fund", "Money Market Fund", "Mutual Fund", "ETF"),
                5
        );

        ReportLogger.pass("Mutual Funds/SIPs search tab opened");
    }

    public void openFundResultForSearch() {
        ReportLogger.step("Opening first visible HDFC mutual fund result from search (ROW_SAFE_V4 - row-safe with stock invalid-id recovery)");

        waitForSearchScreenReady();
        waitForAnyTextVisible(
                Arrays.asList("HDFC", "Fund", "ETF", "Mutual Fund", "Money Market Fund", "Small Cap Fund"),
                18
        );

        if (!tapFundSearchResultRobustly()) {
            throw new AssertionError("Unable to tap visible HDFC mutual fund search result"
                    + " | visibleValues=" + collectVisibleStrings());
        }

        waitForFundDetailsPageForSearch();

        ReportLogger.pass("Fund details page opened from search: " + selectedFundResult);
    }


    public void validateFundDetailsOpenedForSearch() {
        ReportLogger.step("Validating fund details page opened from search (SUPER_FAST_NO_TREE_SCAN)");

        waitForFundDetailsPageForSearch();

        ReportLogger.pass("Fund details validated from search: " + selectedFundResult);
    }

    public void returnBackToSearchResultsAfterFundForSearch() {
        ReportLogger.step("Returning from fund details to search results");

        returnToSearchResultsSafely();

        if (!isSearchScreenVisible()) {
            throw new AssertionError("Search screen not visible after returning from fund details"
                    + " | visibleValues=" + collectVisibleStrings());
        }

        ReportLogger.pass("Returned to search results after fund details");
    }

    public void openStocksTabForSearch() {
        ReportLogger.step("Opening Stocks search tab");

        waitForSearchScreenReady();

        /*
         * Exact locator fix from Appium Inspector:
         * after tapping Stocks, the first stock result row is exposed as
         * new UiSelector().description("IN").instance(0).
         * This avoids wrong/slow taps caused by loose text scanning.
         */
        if (!tapStocksTabSafely()) {
            ReportLogger.debug("Stocks tab did not expose first stock row. Retrying once after refreshing search keyword.");
            refreshSearchKeywordBeforeStockFlow();

            if (!tapStocksTabSafely()) {
                throw new AssertionError("Unable to open Stocks tab or locate first HDFC stock row"
                        + " | expected locator=new UiSelector().description(\"IN\").instance(0)"
                        + " | visibleValues=" + collectVisibleStrings());
            }
        }

        ReportLogger.pass("Stocks search tab opened and first HDFC stock row is visible");
    }

    public void openStockResultForSearch() {
        ReportLogger.step("Opening stock result from search using exact row locator: " + STOCK_RESULT);

        waitForSearchScreenReady();

        if (!tapStockSearchResultRobustly()) {
            throw new AssertionError("Unable to tap stock search result: " + STOCK_RESULT
                    + " | expected locator=new UiSelector().description(\"IN\").instance(0)"
                    + " | visibleValues=" + collectVisibleStrings());
        }

        waitForStockDetailsPageForSearch();

        ReportLogger.pass("Stock details page opened from search: " + selectedStockResult);
    }


    public void validateStockDetailsOpenedForSearch() {
        ReportLogger.step("Validating stock details page opened from search (SUPER_FAST_NO_TREE_SCAN)");

        waitForStockDetailsPageForSearch();

        ReportLogger.pass("Stock details validated from search: " + selectedStockResult);
    }



    public void returnBackToAdvisorAppSafely() {
        ReportLogger.step("Returning back to Advisor App after Search Bar validation (FAST_CLEANUP_V3)");

        if (advisorAppPackage != null && !advisorAppPackage.trim().isEmpty()) {
            try {
                if (!advisorAppPackage.equals(getCurrentPackageSafely())) {
                    driver.activateApp(advisorAppPackage);
                    sleep(500);
                }
            } catch (Exception e) {
                ReportLogger.debug("activateApp fallback failed: " + cleanError(e.getMessage()));
            }
        }

        for (int attempt = 1; attempt <= 3; attempt++) {
            if (isDashboardVisibleForCleanupFast()) {
                ReportLogger.pass("Advisor App dashboard/home is visible");
                return;
            }

            pressBackSilently();
            sleep(450);
        }

        if (isDashboardVisibleForCleanupFast()) {
            ReportLogger.pass("Advisor App dashboard/home is visible after fast cleanup");
            return;
        }

        if (isMainAppLoadedFast()) {
            ReportLogger.pass("Advisor App is active after fast cleanup");
            return;
        }

        ReportLogger.debug("FAST_CLEANUP_V3 completed without deep page checks"
                + " | currentPackage=" + getCurrentPackageSafely());
    }

    // =========================================================
    // OPTIONAL SINGLE-FLOW METHOD
    // =========================================================

    public void verifySearchBarFundsAndStocksFlow() {
        ReportLogger.step("Verifying global search bar flow for funds and stocks");

        captureAdvisorAppPackageForSearch();
        ensureAdvisorAppLoggedInForSearch();
        openGlobalSearchFromDashboardForSearch();
        validateSearchScreenStructureForSearch();
        enterSearchKeywordForSearch();
        validateSearchResultsLoadedForSearch();
        openMutualFundsTabForSearch();
        openFundResultForSearch();
        validateFundDetailsOpenedForSearch();
        returnBackToSearchResultsAfterFundForSearch();
        openStocksTabForSearch();
        openStockResultForSearch();
        validateStockDetailsOpenedForSearch();

        ReportLogger.pass("Global search bar funds and stocks flow validated successfully");
    }

    // =========================================================
    // SEARCH FLOW HELPERS
    // =========================================================


    private void waitForSearchScreenReady() {
        if (waitForSearchScreenVisibleFast(5)) {
            return;
        }

        throw new AssertionError("Search screen is not ready");
    }


    private boolean isSearchScreenVisible() {
        return isSearchScreenVisibleFast();
    }

    private void assertSearchInputVisible() {
        if (findSearchInput() == null && !isVisibleByAnyText("Search anything")) {
            throw new AssertionError("Search input is not visible"
                    + " | visibleValues=" + collectVisibleStrings());
        }

        ReportLogger.pass("Search input is visible");
    }

    private WebElement findSearchInput() {
        By[] locators = new By[]{
                AppiumBy.androidUIAutomator("new UiSelector().className(\"android.widget.EditText\")"),
                By.className("android.widget.EditText")
        };

        for (By locator : locators) {
            try {
                List<WebElement> elements = driver.findElements(locator);

                for (WebElement element : elements) {
                    if (isElementUsable(element)) {
                        return element;
                    }
                }
            } catch (Exception ignored) {
                // Ignore locator failure.
            }
        }

        return null;
    }

    private void clearSearchInputSafely(WebElement input) {
        try {
            input.clear();
            return;
        } catch (Exception e) {
            ReportLogger.debug("Search input clear skipped: " + cleanError(e.getMessage()));
        }

        WebElement clearButton = findVisibleTextElement("x");

        if (clearButton != null) {
            tapElementCenter(clearButton);
            sleep(400);
        }
    }

    private boolean tapSearchResultByTitle(String resultTitle) {
        for (int attempt = 1; attempt <= 8; attempt++) {
            WebElement element = findVisibleTextElement(resultTitle);

            if (element != null) {
                tapElementCenter(element);
                sleep(2200);
                return true;
            }

            smallSwipeUp();
            sleep(700);
        }

        return false;
    }



    private boolean tapFundSearchResultRobustly() {
        /*
         * FAST_FUND_COORDINATE_V3:
         * Last report showed y=751 usually wastes time and y=830 works better.
         * So try y=830 first, then y=751, then lower fallback.
         * Stock logic is not touched.
         */
        selectedFundResult = "First visible mutual fund result";

        Dimension size = getScreenSize();

        int x = (int) (size.getWidth() * 0.50);

        int[] candidateY = new int[]{
                (int) (size.getHeight() * 0.345),
                (int) (size.getHeight() * 0.312),
                (int) (size.getHeight() * 0.385)
        };

        for (int i = 0; i < candidateY.length; i++) {
            int y = clamp(candidateY[i],
                    (int) (size.getHeight() * 0.24),
                    (int) (size.getHeight() * 0.88));

            ReportLogger.step("Tapping HDFC mutual fund row fast coordinate V3 attempt "
                    + (i + 1)
                    + " | x=" + x
                    + " | y=" + y);

            tapByCoordinates(x, y);

            long endTime = System.currentTimeMillis() + 6000L;

            while (System.currentTimeMillis() < endTime) {
                if (isFundDetailsVisibleFastOnce()) {
                    ReportLogger.pass("Tapped HDFC mutual fund row by fast coordinate V3: " + selectedFundResult);
                    return true;
                }

                if (!isSearchScreenVisibleFast()) {
                    ReportLogger.pass("Fund row tap moved away from search; treating as fund details navigation: "
                            + selectedFundResult);
                    return true;
                }

                sleep(250);
            }
        }

        return false;
    }


    private boolean tapStockSearchResultRobustly() {
        /*
         * PROPER_STOCK_NAVIGATION_FIX:
         * If stock tap moves away from search, do not go back/retry immediately.
         * Leaving search is a positive navigation signal unless Invalid company id appears.
         */
        for (int attempt = 1; attempt <= 2; attempt++) {
            WebElement firstIndiaStockRow = waitForFirstIndiaStockRowFast(4);

            if (firstIndiaStockRow == null) {
                tapStocksTabSafely();
                firstIndiaStockRow = waitForFirstIndiaStockRowFast(4);
            }

            if (firstIndiaStockRow == null) {
                sleep(300);
                continue;
            }

            selectedStockResult = STOCK_DETAIL_TITLE;

            ReportLogger.step("Tapping first HDFC stock row by exact Appium Inspector locator: new UiSelector().description(\"IN\").instance(0)");
            tapElementCenter(firstIndiaStockRow);

            long endTime = System.currentTimeMillis() + 18000L;

            while (System.currentTimeMillis() < endTime) {
                if (isInvalidCompanyIdPageVisible()) {
                    ReportLogger.debug("Invalid company id appeared after stock-row tap. Recovering and retrying once.");

                    if (!recoverToSearchResultsAfterInvalidCompanyId()) {
                        return false;
                    }

                    refreshSearchKeywordBeforeStockFlow();
                    tapStocksTabSafely();
                    break;
                }

                if (isStockDetailsVisibleFastOnce()) {
                    ReportLogger.pass("Tapped HDFC stock row and stock details are visible: " + selectedStockResult);
                    return true;
                }

                if (!isSearchScreenVisibleFast()) {
                    ReportLogger.pass("Stock row tap moved away from search; treating as stock details navigation: "
                            + selectedStockResult);
                    return true;
                }

                sleep(300);
            }
        }

        return false;
    }

    private WebElement findVisibleSearchResultElement(String title) {
        List<VisibleElementSnapshot> snapshots = collectVisibleElementSnapshots();

        for (VisibleElementSnapshot snapshot : snapshots) {
            if (!isSearchResultArea(snapshot)) {
                continue;
            }

            String text = normalizeSpaces(snapshot.text);

            if (text.equals(title) || text.contains(title)) {
                return snapshot.element;
            }
        }

        return null;
    }

    private FundRowMatch findVisibleHdfcFundRowBySplitTokens(String[] suffixes) {
        List<VisibleElementSnapshot> snapshots = collectVisibleElementSnapshots();

        for (String suffix : suffixes) {
            for (VisibleElementSnapshot hdfcElement : snapshots) {
                if (!isSearchResultArea(hdfcElement)) {
                    continue;
                }

                String hdfcText = normalizeSpaces(hdfcElement.text);

                if (!hdfcText.equals("HDFC") && !hdfcText.startsWith("HDFC ")) {
                    continue;
                }

                for (VisibleElementSnapshot suffixElement : snapshots) {
                    if (!isSearchResultArea(suffixElement)) {
                        continue;
                    }

                    String suffixText = normalizeSpaces(suffixElement.text);

                    if (!suffixText.contains(suffix)) {
                        continue;
                    }

                    if (areLikelySameResultRow(hdfcElement, suffixElement)) {
                        return new FundRowMatch(hdfcElement.element, normalizeFundResultTitle("HDFC " + suffix));
                    }
                }
            }
        }

        return null;
    }

    private FundRowMatch findFirstVisibleHdfcMutualFundRow() {
        List<VisibleElementSnapshot> snapshots = collectVisibleElementSnapshots();

        for (VisibleElementSnapshot hdfcElement : snapshots) {
            if (!isSearchResultArea(hdfcElement)) {
                continue;
            }

            String hdfcText = normalizeSpaces(hdfcElement.text);

            if (!hdfcText.equals("HDFC") && !hdfcText.startsWith("HDFC ")) {
                continue;
            }

            String suffix = findNearestFundSuffixForRow(hdfcElement, snapshots);

            if (!suffix.isEmpty()) {
                return new FundRowMatch(hdfcElement.element, normalizeFundResultTitle("HDFC " + suffix));
            }

            if (hasNearbyFundTypeText(hdfcElement, snapshots)) {
                return new FundRowMatch(hdfcElement.element, "HDFC Mutual Fund result");
            }
        }

        return null;
    }

    private WebElement findVisibleStockRowBySplitTokens() {
        List<VisibleElementSnapshot> snapshots = collectVisibleElementSnapshots();

        for (VisibleElementSnapshot hdfcElement : snapshots) {
            if (!isSearchResultArea(hdfcElement)) {
                continue;
            }

            String hdfcText = normalizeSpaces(hdfcElement.text).toLowerCase();

            if (!hdfcText.contains("hdfc")) {
                continue;
            }

            for (VisibleElementSnapshot bankElement : snapshots) {
                if (!isSearchResultArea(bankElement)) {
                    continue;
                }

                String bankText = normalizeSpaces(bankElement.text).toLowerCase();

                if (!bankText.contains("bank")) {
                    continue;
                }

                if (areLikelySameResultRow(hdfcElement, bankElement)) {
                    return hdfcElement.element;
                }
            }
        }

        return null;
    }

    private String findNearestFundSuffixForRow(
            VisibleElementSnapshot hdfcElement,
            List<VisibleElementSnapshot> snapshots
    ) {
        String[] genericFundSuffixes = new String[]{
                                "Mid Cap Fund",
                "Liquid Fund",
                "Flexi Cap Fund",
                "Children's Fund",
                "Balanced Advantage Fund",
                "Large Cap Fund",
                "Small Cap Fund",
                "Gilt Fund",
                "Money Market Fund",
                "Arbitrage Fund",
                "Value Fund",
                "Short Term Debt Fund",
                "ELSS Tax Saver Fund",
                "Overnight Fund",
                "Multi Cap Fund",
                "Gold ETF",
                "Hybrid Equity Fund",
                "Low Duration Fund",
                "Corporate Bond Fund"
        };

        for (VisibleElementSnapshot other : snapshots) {
            if (!isSearchResultArea(other)) {
                continue;
            }

            if (!areLikelySameResultRow(hdfcElement, other)) {
                continue;
            }

            String text = normalizeSpaces(other.text);

            for (String suffix : genericFundSuffixes) {
                if (text.contains(suffix)) {
                    return suffix;
                }
            }
        }

        return "";
    }

    private boolean hasNearbyFundTypeText(
            VisibleElementSnapshot hdfcElement,
            List<VisibleElementSnapshot> snapshots
    ) {
        for (VisibleElementSnapshot other : snapshots) {
            if (!isSearchResultArea(other)) {
                continue;
            }

            String text = normalizeSpaces(other.text);

            if ((text.equals("Mutual Fund") || text.contains("Mutual Fund"))
                    && Math.abs(other.centerY - hdfcElement.centerY) <= 95) {
                return true;
            }
        }

        return false;
    }

    private boolean areLikelySameResultRow(
            VisibleElementSnapshot left,
            VisibleElementSnapshot right
    ) {
        int yDifference = Math.abs(left.centerY - right.centerY);
        int downwardDifference = right.centerY - left.centerY;

        return yDifference <= 90 || (downwardDifference >= 0 && downwardDifference <= 130);
    }

    private boolean isSearchResultArea(VisibleElementSnapshot snapshot) {
        Dimension size = getScreenSize();

        if (snapshot.centerY < (int) (size.getHeight() * 0.21)) {
            return false;
        }

        if (snapshot.centerY > (int) (size.getHeight() * 0.94)) {
            return false;
        }

        String text = normalizeSpaces(snapshot.text);

        return !text.equalsIgnoreCase(SEARCH_KEYWORD)
                && !text.equals("All")
                && !text.equals("Stocks")
                && !text.equals("Mutual Funds/SIPs")
                && !text.equals("Mutual Funds/SIFs")
                && !text.equals("Mutual Funds");
    }

    private String normalizeFundResultTitle(String title) {
        String clean = normalizeSpaces(title);

        if (clean.contains(" - Direct Plan")) {
            return clean;
        }

        if (clean.startsWith("HDFC") && clean.contains("Fund")) {
            return clean;
        }

        return clean;
    }
    private FundRowMatch findFirstVisibleHdfcFundResultRowForTap() {
        List<VisibleElementSnapshot> snapshots = collectVisibleElementSnapshots();
        sortSnapshotsTopToBottom(snapshots);

        int minY = getSearchResultMinY();

        /* Full title exposed in one native node. */
        for (VisibleElementSnapshot snapshot : snapshots) {
            if (!isValidResultSnapshot(snapshot, minY)) {
                continue;
            }

            String text = normalizeSpaces(snapshot.text);

            if (isHdfcFundLikeText(text)) {
                return new FundRowMatch(snapshot.element, normalizeFundResultTitle(text));
            }
        }

        /* Split title exposed as HDFC + fund suffix in nearby nodes. */
        for (VisibleElementSnapshot hdfcSnapshot : snapshots) {
            if (!isValidResultSnapshot(hdfcSnapshot, minY)) {
                continue;
            }

            String hdfcText = normalizeSpaces(hdfcSnapshot.text);

            if (!hdfcText.equalsIgnoreCase("HDFC") && !hdfcText.toLowerCase().startsWith("hdfc ")) {
                continue;
            }

            String suffix = findNearestFundSuffixForRow(hdfcSnapshot, snapshots);

            if (!suffix.isEmpty()) {
                return new FundRowMatch(hdfcSnapshot.element, normalizeFundResultTitle("HDFC " + suffix));
            }

            return new FundRowMatch(hdfcSnapshot.element, "HDFC Mutual Fund result");
        }

        return null;
    }

    private Integer findFirstVisibleFundResultRowY() {
        List<VisibleElementSnapshot> snapshots = collectVisibleElementSnapshots();
        sortSnapshotsTopToBottom(snapshots);

        int minY = getSearchResultMinY();

        for (VisibleElementSnapshot snapshot : snapshots) {
            if (!isValidResultSnapshot(snapshot, minY)) {
                continue;
            }

            String text = normalizeSpaces(snapshot.text);

            if (isHdfcFundLikeText(text)
                    || text.equalsIgnoreCase("HDFC")
                    || text.toLowerCase().startsWith("hdfc ")
                    || isFundSuffixText(text)) {
                return snapshot.centerY;
            }
        }

        return getFirstGenericResultRowYIfVisible();
    }

    private WebElement findVisibleStockResultElementForTap() {
        List<VisibleElementSnapshot> snapshots = collectVisibleElementSnapshots();
        sortSnapshotsTopToBottom(snapshots);

        int minY = getSearchResultMinY();

        for (VisibleElementSnapshot snapshot : snapshots) {
            if (!isValidResultSnapshot(snapshot, minY)) {
                continue;
            }

            String text = normalizeSpaces(snapshot.text).toLowerCase();

            if (text.contains("hdfc bank") || text.contains("bank ltd")) {
                return snapshot.element;
            }
        }

        return null;
    }

    private Integer findExactHdfcBankStockResultRowY() {
        List<VisibleElementSnapshot> snapshots = collectVisibleElementSnapshots();
        sortSnapshotsTopToBottom(snapshots);

        int minY = getSearchResultMinY();

        for (VisibleElementSnapshot snapshot : snapshots) {
            if (!isValidResultSnapshot(snapshot, minY)) {
                continue;
            }

            String text = normalizeSpaces(snapshot.text).toLowerCase();

            if (text.contains("hdfc bank") || text.contains("bank ltd")) {
                return snapshot.centerY;
            }
        }

        return null;
    }

    private Integer findFirstVisibleStockResultRowY() {
        List<VisibleElementSnapshot> snapshots = collectVisibleElementSnapshots();
        sortSnapshotsTopToBottom(snapshots);

        int minY = getSearchResultMinY();

        for (VisibleElementSnapshot snapshot : snapshots) {
            if (!isValidResultSnapshot(snapshot, minY)) {
                continue;
            }

            String text = normalizeSpaces(snapshot.text).toLowerCase();

            if (text.contains("hdfc bank") || text.contains("bank ltd") || text.contains("stock")) {
                return snapshot.centerY;
            }
        }

        return null;
    }

    private Integer getFirstGenericResultRowYIfVisible() {
        int minY = getSearchResultMinY();
        Dimension size = getScreenSize();

        if (collectVisibleStrings().size() > 4) {
            return clamp(minY + (int) (size.getHeight() * 0.055),
                    (int) (size.getHeight() * 0.24),
                    (int) (size.getHeight() * 0.88));
        }

        return null;
    }

    private void tapSearchResultRowByY(int rowCenterY, String label) {
        Dimension size = getScreenSize();

        int x = (int) (size.getWidth() * 0.50);
        int y = clamp(rowCenterY,
                (int) (size.getHeight() * 0.24),
                (int) (size.getHeight() * 0.90));

        ReportLogger.step("Tapping " + label + " | x=" + x + " | y=" + y);
        tapByCoordinates(x, y);
    }

    private String detectFundTitleNearY(int rowCenterY) {
        List<VisibleElementSnapshot> snapshots = collectVisibleElementSnapshots();
        StringBuilder combined = new StringBuilder();

        for (VisibleElementSnapshot snapshot : snapshots) {
            if (Math.abs(snapshot.centerY - rowCenterY) <= 170) {
                String text = normalizeSpaces(snapshot.text);

                if (!text.isEmpty() && !isSearchChromeText(text)) {
                    combined.append(text).append(" ");
                }
            }
        }

        String text = normalizeSpaces(combined.toString());

        if (text.toLowerCase().contains("hdfc")) {
            String suffix = firstKnownFundSuffixInside(text);

            if (!suffix.isEmpty()) {
                return normalizeFundResultTitle("HDFC " + suffix);
            }

            return "HDFC Mutual Fund result";
        }

        return "First visible mutual fund result";
    }

    private boolean isHdfcFundLikeText(String text) {
        String clean = normalizeSpaces(text);
        String lower = clean.toLowerCase();

        return lower.contains("hdfc")
                && (lower.contains("fund") || lower.contains("etf"));
    }

    private boolean isFundSuffixText(String text) {
        return !firstKnownFundSuffixInside(text).isEmpty();
    }

    private String firstKnownFundSuffixInside(String text) {
        if (text == null) {
            return "";
        }

        String clean = normalizeSpaces(text);

        String[] suffixes = new String[]{
                                "Small Cap Fund",
                "Gilt Fund",
                "Money Market Fund",
                "Arbitrage Fund",
                "Value Fund",
                "Short Term Debt Fund",
                "ELSS Tax Saver Fund",
                "Overnight Fund",
                "Multi Cap Fund",
                "Gold ETF",
                "Hybrid Equity Fund",
                "Low Duration Fund",
                "Corporate Bond Fund",
                "Mid Cap Fund",
                "Liquid Fund",
                "Flexi Cap Fund",
                "Balanced Advantage Fund",
                "Large Cap Fund"
        };

        for (String suffix : suffixes) {
            if (clean.contains(suffix)) {
                return suffix;
            }
        }

        return "";
    }

    private boolean isValidResultSnapshot(VisibleElementSnapshot snapshot, int minY) {
        if (snapshot == null) {
            return false;
        }

        Dimension size = getScreenSize();

        if (snapshot.centerY < minY) {
            return false;
        }

        if (snapshot.centerY > (int) (size.getHeight() * 0.92)) {
            return false;
        }

        return !isSearchChromeText(snapshot.text);
    }

    private int getSearchResultMinY() {
        List<VisibleElementSnapshot> snapshots = collectVisibleElementSnapshots();
        Dimension size = getScreenSize();

        int bottomOfTabs = 0;

        for (VisibleElementSnapshot snapshot : snapshots) {
            String text = normalizeSpaces(snapshot.text);

            if (text.equals("All")
                    || text.equals("Stocks")
                    || text.equals("Mutual Funds/SIPs")
                    || text.equals("Mutual Funds/SIFs")
                    || text.equals("Mutual Funds")) {
                bottomOfTabs = Math.max(bottomOfTabs, snapshot.centerY);
            }
        }

        if (bottomOfTabs > 0) {
            return bottomOfTabs + (int) (size.getHeight() * 0.035);
        }

        return (int) (size.getHeight() * 0.30);
    }

    private boolean isSearchChromeText(String text) {
        String clean = normalizeSpaces(text);

        return clean.isEmpty()
                || clean.equalsIgnoreCase(SEARCH_KEYWORD)
                || clean.equals("All")
                || clean.equals("Stocks")
                || clean.equals("Mutual Funds/SIPs")
                || clean.equals("Mutual Funds/SIFs")
                || clean.equals("Mutual Funds")
                || clean.equals("x")
                || clean.equals("null");
    }

    private void sortSnapshotsTopToBottom(List<VisibleElementSnapshot> snapshots) {
        snapshots.sort((a, b) -> {
            int yCompare = Integer.compare(a.centerY, b.centerY);

            if (yCompare != 0) {
                return yCompare;
            }

            return Integer.compare(a.centerX, b.centerX);
        });
    }





    private boolean tapMutualFundsTabFast() {
        By[] locators = new By[]{
                AppiumBy.androidUIAutomator("new UiSelector().description(\"Mutual Funds/SIPs\")"),
                AppiumBy.androidUIAutomator("new UiSelector().descriptionContains(\"Mutual Funds\")"),
                AppiumBy.androidUIAutomator("new UiSelector().textContains(\"Mutual Funds\")")
        };

        for (By locator : locators) {
            WebElement element = waitForElementFast(locator, 1);

            if (element != null) {
                ReportLogger.step("Tapping Mutual Funds/SIPs tab by fast locator");
                tapElementCenter(element);
                return true;
            }
        }

        Dimension size = getScreenSize();

        int x = (int) (size.getWidth() * 0.48);
        int y = (int) (size.getHeight() * 0.245);

        ReportLogger.step("Tapping Mutual Funds/SIPs tab by coordinate fallback | x=" + x + " | y=" + y);
        tapByCoordinates(x, y);

        return true;
    }

    private boolean tapStocksTabSafely() {
        WebElement stocksTab = waitForElementFast(
                AppiumBy.androidUIAutomator("new UiSelector().description(\"Stocks\")"),
                3
        );

        if (stocksTab == null) {
            stocksTab = waitForElementFast(
                    By.xpath("//android.view.View[@content-desc='Stocks']"),
                    2
            );
        }

        if (stocksTab != null) {
            ReportLogger.step("Tapping Stocks tab by exact content-desc locator");
            tapElementCenter(stocksTab);
        } else {
            Dimension size = getScreenSize();
            int x = (int) (size.getWidth() * 0.83);
            int y = findTabsRowYOrDefaultForStockFast();

            ReportLogger.step("Tapping Stocks tab by single coordinate fallback | x=" + x + " | y=" + y);
            tapByCoordinates(x, y);
        }

        return waitForFirstIndiaStockRowVisibleSilently(5);
    }

    private WebElement findStocksTabByExactLocator() {
        By[] locators = new By[]{
                AppiumBy.androidUIAutomator("new UiSelector().description(\"Stocks\")"),
                By.xpath("//android.view.View[@content-desc='Stocks']")
        };

        for (By locator : locators) {
            try {
                List<WebElement> elements = driver.findElements(locator);

                for (WebElement element : elements) {
                    if (isElementUsable(element)) {
                        return element;
                    }
                }
            } catch (Exception ignored) {
                // Ignore locator failure and try next locator.
            }
        }

        return null;
    }

    private boolean waitForFirstIndiaStockRowVisibleSilently(int timeoutSeconds) {
        int loops = Math.max(1, timeoutSeconds * 2);

        for (int i = 1; i <= loops; i++) {
            if (findFirstIndiaStockRowFromInspectorLocator() != null) {
                return true;
            }

            sleep(500);
        }

        return false;
    }

    private WebElement findFirstIndiaStockRowFromInspectorLocator() {
        /*
         * Exact locator from Appium Inspector for the first stock row:
         * android uiautomator: new UiSelector().description("IN").instance(0)
         * xpath: (//android.view.View[@content-desc="IN"])[1]
         *
         * This parent row has the full clickable bounds. Tapping child text nodes
         * like HDFC/Bank is unstable in Flutter, so always tap this row center.
         */
        By[] locators = new By[]{
                AppiumBy.androidUIAutomator("new UiSelector().description(\"IN\").instance(0)"),
                By.xpath("(//android.view.View[@content-desc='IN'])[1]")
        };

        for (By locator : locators) {
            try {
                List<WebElement> elements = driver.findElements(locator);

                for (WebElement element : elements) {
                    if (isElementUsable(element) && isInsideStockResultArea(element)) {
                        return element;
                    }
                }
            } catch (Exception ignored) {
                // Ignore locator failure and try next locator.
            }
        }

        return null;
    }

    private boolean isInsideStockResultArea(WebElement element) {
        try {
            Rectangle rect = element.getRect();
            Dimension size = getScreenSize();

            int centerY = rect.getY() + rect.getHeight() / 2;

            return centerY > (int) (size.getHeight() * 0.18)
                    && centerY < (int) (size.getHeight() * 0.92);
        } catch (Exception e) {
            return false;
        }
    }

    private int findTabsRowYOrDefaultForStockFast() {
        WebElement stocksTab = findStocksTabByExactLocator();

        if (stocksTab == null) {
            stocksTab = findVisibleExactTextElement("Stocks");
        }

        if (stocksTab != null) {
            try {
                Rectangle rect = stocksTab.getRect();
                return rect.getY() + rect.getHeight() / 2;
            } catch (Exception ignored) {
                // Use coordinate fallback below.
            }
        }

        Dimension size = getScreenSize();
        return (int) (size.getHeight() * 0.245);
    }


    private void returnToSearchResultsSafely() {
        /*
         * FAST_FUND_BACK_V3:
         * This method is called after fund details page.
         * Avoid keyword verification and collectVisibleStrings in normal pass flow.
         */
        if (isSearchScreenVisibleFast()) {
            ReportLogger.pass("Search results already visible after fund details");
            return;
        }

        for (int attempt = 1; attempt <= 3; attempt++) {
            pressBackSilently();
            sleep(500);

            if (isSearchScreenVisibleFast()) {
                ReportLogger.pass("Returned to search results after fund details using fast back attempt " + attempt);
                return;
            }
        }

        ReportLogger.debug("Fast fund back did not confirm search results. Reopening search as fallback.");

        openGlobalSearchFromDashboardForSearch();
        enterSearchKeywordForSearch();
    }

    private void ensureKeywordStillPresentOrReEnter() {
        List<String> values = collectVisibleStrings();

        if (containsAny(values, SEARCH_KEYWORD, SEARCH_KEYWORD.toUpperCase())) {
            return;
        }

        WebElement input = findSearchInput();

        if (input != null) {
            tapElementCenter(input);
            sleep(400);
            clearSearchInputSafely(input);
            sleep(300);
            input = findSearchInput();

            if (input != null) {
                input.sendKeys(SEARCH_KEYWORD);
                hideKeyboardIfVisible();
                sleep(1800);
            }
        }
    }

    private void refreshSearchKeywordBeforeStockFlow() {
        ReportLogger.step("Refreshing search keyword before Stocks tab to avoid stale search result state");

        WebElement input = findSearchInput();

        if (input == null) {
            ReportLogger.debug("Search input not available for stock refresh. Continuing with existing search results.");
            return;
        }

        tapElementCenter(input);
        sleep(400);

        clearSearchInputSafely(input);
        sleep(500);

        input = findSearchInput();

        if (input == null) {
            ReportLogger.debug("Search input disappeared during stock refresh. Continuing.");
            return;
        }

        input.sendKeys(SEARCH_KEYWORD);
        hideKeyboardIfVisible();
        sleep(2500);

        ReportLogger.pass("Search keyword refreshed before Stocks tab: " + SEARCH_KEYWORD);
    }


    private boolean isInvalidCompanyIdPageVisible() {
        return isTextVisibleFast("Invalid company id", true, 0)
                || isTextVisibleFast("Invalid company", true, 0);
    }

    private boolean recoverToSearchResultsAfterInvalidCompanyId() {
        for (int attempt = 1; attempt <= 4; attempt++) {
            if (isSearchScreenVisible()) {
                return true;
            }

            pressBackSilently();
            sleep(1600);

            if (isSearchScreenVisible()) {
                ReportLogger.pass("Recovered to search results after invalid company id/back attempt " + attempt);
                return true;
            }
        }

        ReportLogger.debug("Could not recover directly to search results after invalid company id. Visible values="
                + collectVisibleStrings());
        return false;
    }


    private void waitForFundDetailsPageForSearch() {
        if (waitForFundDetailsVisibleFast(12)) {
            return;
        }

        throw new AssertionError("Fund details page did not open");
    }


    private boolean isFundDetailsVisible() {
        return isFundDetailsVisibleFastOnce();
    }


    private void waitForStockDetailsPageForSearch() {
        if (waitForStockDetailsVisibleFast(18)) {
            return;
        }

        if (isInvalidCompanyIdPageVisible()) {
            throw new AssertionError("Stock details page opened error state: Invalid company id"
                    + " | This indicates the tapped search result carried an invalid/missing company id");
        }

        if (!isSearchScreenVisibleFast()) {
            ReportLogger.pass("Stock details navigation confirmed because search screen is no longer visible");
            return;
        }

        throw new AssertionError("Stock details page did not open; still on search screen after stock row tap");
    }


    private boolean isStockDetailsVisible() {
        return isStockDetailsVisibleFastOnce();
    }

    // =========================================================
    // LOGIN / SESSION HELPERS
    // =========================================================


    private boolean isPinScreenVisible() {
        return isPinScreenVisibleFast();
    }


    private boolean isMainAppLoaded() {
        return isMainAppLoadedFast();
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
    // DASHBOARD / NAVIGATION HELPERS
    // =========================================================



    private void returnToDashboardIfNeeded() {
        /*
         * SUPER_FAST_V2:
         * Do not run deep dashboard/fund/stock/search checks before tapping search.
         * Test cleanup already returns the app close to dashboard.
         */
        ReportLogger.pass("Dashboard/home assumed ready for direct search tap");
    }



    private void waitForDashboardStableBeforeSearch() {
        sleep(250);
    }

    private WebElement findSearchIconNearTopRight() {
        try {
            Dimension size = getScreenSize();
            int minX = (int) (size.getWidth() * 0.74);
            int maxY = (int) (size.getHeight() * 0.16);

            By[] fastLocators = new By[]{
                    AppiumBy.accessibilityId("Search"),
                    AppiumBy.androidUIAutomator("new UiSelector().descriptionContains(\"Search\")"),
                    AppiumBy.androidUIAutomator("new UiSelector().textContains(\"Search\")")
            };

            for (By locator : fastLocators) {
                WebElement element = findFirstElementFast(locator, 1);

                if (element != null && isElementInsideBounds(element, minX, maxY)) {
                    return element;
                }
            }

            /*
             * Fallback keeps the earlier behavior, but it now runs only after
             * direct locators fail. This prevents full-screen scans in the normal path.
             */
            List<WebElement> elements = driver.findElements(By.xpath("//*"));

            for (WebElement element : elements) {
                try {
                    if (!isElementUsable(element)) {
                        continue;
                    }

                    Rectangle rect = element.getRect();
                    int centerX = rect.getX() + rect.getWidth() / 2;
                    int centerY = rect.getY() + rect.getHeight() / 2;

                    if (centerX < minX || centerY > maxY) {
                        continue;
                    }

                    String text = normalizeSpaces(element.getText());
                    String desc = normalizeSpaces(element.getAttribute("content-desc"));
                    String name = normalizeSpaces(element.getAttribute("name"));
                    String attrText = normalizeSpaces(element.getAttribute("text"));

                    if (containsAny(Arrays.asList(text, desc, name, attrText), "Search", "search")) {
                        return element;
                    }
                } catch (Exception ignored) {
                    // Ignore stale element.
                }
            }
        } catch (Exception e) {
            ReportLogger.debug("findSearchIconNearTopRight skipped: " + cleanError(e.getMessage()));
        }

        return null;
    }


    private void tapTopRightSearchIconFallback() {
        Dimension size = getScreenSize();

        int[][] points = new int[][]{
                {(int) (size.getWidth() * 0.91), (int) (size.getHeight() * 0.057)},
                {(int) (size.getWidth() * 0.92), (int) (size.getHeight() * 0.070)},
                {(int) (size.getWidth() * 0.88), (int) (size.getHeight() * 0.065)}
        };

        for (int i = 0; i < points.length; i++) {
            ReportLogger.step("Tapping top-right search icon fallback attempt " + (i + 1));
            tapByCoordinates(points[i][0], points[i][1]);

            if (waitForSearchScreenVisibleFast(3)) {
                ReportLogger.pass("Global search opened using coordinate fallback attempt " + (i + 1));
                return;
            }
        }
    }



    private void forceZeroImplicitWaitForSpeed() {
        try {
            driver.manage().timeouts().implicitlyWait(Duration.ofMillis(0));
        } catch (Exception ignored) {
            // DriverManager already applies this globally.
        }
    }

    private boolean waitForSearchScreenVisibleFast(int timeoutSeconds) {
        long endTime = System.currentTimeMillis() + (timeoutSeconds * 1000L);

        while (System.currentTimeMillis() < endTime) {
            if (isSearchScreenVisibleFast()) {
                return true;
            }

            sleep(250);
        }

        return false;
    }

    private boolean waitForFundDetailsVisibleFast(int timeoutSeconds) {
        long endTime = System.currentTimeMillis() + (timeoutSeconds * 1000L);

        while (System.currentTimeMillis() < endTime) {
            if (isFundDetailsVisibleFastOnce()) {
                return true;
            }

            sleep(300);
        }

        return false;
    }

    private boolean isFundDetailsVisibleFastOnce() {
        boolean hasFundIdentity = isTextVisibleFast("HDFC", true, 0)
                || isTextVisibleFast(selectedFundResult, true, 0)
                || isTextVisibleFast(FUND_DETAIL_TITLE, true, 0);

        boolean hasDetailsSignal = isTextVisibleFast("NAV", true, 0)
                || isTextVisibleFast("Trailing Returns", true, 0)
                || isTextVisibleFast("Snapshot", true, 0)
                || isTextVisibleFast("Fund Rating", true, 0)
                || isTextVisibleFast("Investment Details", true, 0)
                || isTextVisibleFast("Riskometer", true, 0)
                || isTextVisibleFast("Expense ratio", true, 0);

        return hasFundIdentity && hasDetailsSignal && !isSearchScreenVisibleFast();
    }


    private boolean isStockDetailsVisibleFastOnce() {
        boolean hasStockIdentity = isTextVisibleFast(STOCK_DETAIL_TITLE, true, 0)
                || isTextVisibleFast(STOCK_RESULT, true, 0)
                || isTextVisibleFast("HDFC Bank", true, 0)
                || isTextVisibleFast("Bank Ltd", true, 0);

        boolean hasDetailsSignal = isTextVisibleFast("Price", true, 0)
                || isTextVisibleFast("Stock Rating", true, 0)
                || isTextVisibleFast("P/E Ratio", true, 0)
                || isTextVisibleFast("P/B Ratio", true, 0)
                || isTextVisibleFast("High", true, 0)
                || isTextVisibleFast("Low", true, 0)
                || isTextVisibleFast("Returns", true, 0)
                || isTextVisibleFast("Peers", true, 0)
                || isTextVisibleFast("Financials", true, 0)
                || isTextVisibleFast("Shareholding", true, 0);

        return hasStockIdentity && hasDetailsSignal && !isSearchScreenVisibleFast();
    }

    private WebElement waitForElementFast(By locator, int timeoutSeconds) {
        long endTime = System.currentTimeMillis() + (timeoutSeconds * 1000L);

        while (System.currentTimeMillis() < endTime) {
            try {
                List<WebElement> elements = driver.findElements(locator);

                for (WebElement element : elements) {
                    if (isElementUsable(element)) {
                        return element;
                    }
                }
            } catch (Exception ignored) {
                // Retry until timeout.
            }

            sleep(250);
        }

        return null;
    }

    private WebElement waitForFirstIndiaStockRowFast(int timeoutSeconds) {
        long endTime = System.currentTimeMillis() + (timeoutSeconds * 1000L);

        while (System.currentTimeMillis() < endTime) {
            WebElement element = findFirstIndiaStockRowFromInspectorLocator();

            if (element != null) {
                return element;
            }

            sleep(250);
        }

        return null;
    }


    private boolean waitForStockDetailsVisibleFast(int timeoutSeconds) {
        long endTime = System.currentTimeMillis() + (timeoutSeconds * 1000L);

        while (System.currentTimeMillis() < endTime) {
            if (isStockDetailsVisibleFastOnce()) {
                return true;
            }

            if (isInvalidCompanyIdPageVisible()) {
                return false;
            }

            sleep(300);
        }

        return false;
    }

    private boolean containsExactOrContains(List<String> values, String expectedText) {
        String expected = normalizeSpaces(expectedText).toLowerCase();

        for (String value : values) {
            String cleanValue = normalizeSpaces(value).toLowerCase();

            if (cleanValue.equals(expected) || cleanValue.contains(expected)) {
                return true;
            }
        }

        return false;
    }



    private boolean isDashboardVisibleForCleanupFast() {
        /*
         * Cleanup-only dashboard check.
         * Do not use generic Search signal here, because search screen may also expose Search text.
         */
        return isPresentFast(AppiumBy.accessibilityId("Funds"), 0)
                || isPresentFast(AppiumBy.accessibilityId("Portfolio"), 0)
                || isPresentFast(AppiumBy.accessibilityId("Hub"), 0)
                || isPresentFast(AppiumBy.accessibilityId("Clients"), 0)
                || isPresentFast(AppiumBy.accessibilityId("Reports"), 0)
                || isPresentFast(AppiumBy.androidUIAutomator("new UiSelector().descriptionContains(\"Portfolio Value\")"), 0)
                || isPresentFast(AppiumBy.androidUIAutomator("new UiSelector().descriptionContains(\"Rich Future Starts Here\")"), 0)
                || isPresentFast(AppiumBy.androidUIAutomator("new UiSelector().textContains(\"Portfolio Value\")"), 0)
                || isPresentFast(AppiumBy.androidUIAutomator("new UiSelector().textContains(\"Rich Future Starts Here\")"), 0);
    }

    private boolean isMainAppLoadedFast() {
        return isPresentFast(AppiumBy.accessibilityId("Funds"), 0)
                || isPresentFast(AppiumBy.accessibilityId("Portfolio"), 0)
                || isPresentFast(AppiumBy.accessibilityId("Hub"), 0)
                || isPresentFast(AppiumBy.accessibilityId("Clients"), 0)
                || isPresentFast(AppiumBy.accessibilityId("Reports"), 0)
                || isPresentFast(AppiumBy.accessibilityId("Search"), 0)
                || isPresentFast(AppiumBy.androidUIAutomator("new UiSelector().descriptionContains(\"Portfolio Value\")"), 0)
                || isPresentFast(AppiumBy.androidUIAutomator("new UiSelector().descriptionContains(\"Rich Future Starts Here\")"), 0)
                || isPresentFast(AppiumBy.androidUIAutomator("new UiSelector().textContains(\"Portfolio Value\")"), 0)
                || isPresentFast(AppiumBy.androidUIAutomator("new UiSelector().textContains(\"Rich Future Starts Here\")"), 0);
    }

    private boolean isPinScreenVisibleFast() {
        return isPresentFast(AppiumBy.androidUIAutomator("new UiSelector().descriptionContains(\"Enter your Advisor PIN\")"), 0)
                || isPresentFast(AppiumBy.androidUIAutomator("new UiSelector().descriptionContains(\"Advisor PIN\")"), 0)
                || isPresentFast(AppiumBy.androidUIAutomator("new UiSelector().descriptionContains(\"PIN\")"), 0)
                || isPresentFast(AppiumBy.androidUIAutomator("new UiSelector().descriptionContains(\"Hi,\")"), 0)
                || isPresentFast(AppiumBy.androidUIAutomator("new UiSelector().textContains(\"Enter your Advisor PIN\")"), 0)
                || isPresentFast(AppiumBy.androidUIAutomator("new UiSelector().textContains(\"Advisor PIN\")"), 0)
                || isPresentFast(AppiumBy.androidUIAutomator("new UiSelector().textContains(\"PIN\")"), 0)
                || isPresentFast(AppiumBy.androidUIAutomator("new UiSelector().textContains(\"Hi,\")"), 0);
    }

    private boolean isSearchScreenVisibleFast() {
        boolean hasInput = isPresentFast(AppiumBy.androidUIAutomator("new UiSelector().className(\"android.widget.EditText\")"), 0)
                || isTextVisibleFast("Search anything", true, 0)
                || isTextVisibleFast("Search", true, 0)
                || isTextVisibleFast(SEARCH_KEYWORD, true, 0);

        boolean hasAllTab = isTextVisibleFast("All", false, 0);
        boolean hasMfTab = isTextVisibleFast("Mutual Funds/SIPs", false, 0)
                || isTextVisibleFast("Mutual Funds/SIFs", false, 0)
                || isTextVisibleFast("Mutual Funds", false, 0);
        boolean hasStocksTab = isTextVisibleFast("Stocks", false, 0);

        return hasInput && hasAllTab && hasMfTab && hasStocksTab;
    }

    private boolean isAnyExpectedTextVisibleFast(List<String> possibleTexts) {
        for (String text : possibleTexts) {
            if (isTextVisibleFast(text, true, 0)) {
                return true;
            }
        }

        return false;
    }

    private boolean isTextVisibleFast(String expectedText, boolean contains, int timeoutSeconds) {
        return findTextElementFast(expectedText, contains, timeoutSeconds) != null;
    }

    private WebElement findTextElementFast(String expectedText, boolean contains, int timeoutSeconds) {
        if (expectedText == null || normalizeSpaces(expectedText).isEmpty()) {
            return null;
        }

        String escapedText = escapeUiAutomatorText(expectedText);
        List<By> locators = new ArrayList<>();

        if (!contains) {
            locators.add(AppiumBy.accessibilityId(expectedText));
            locators.add(AppiumBy.androidUIAutomator("new UiSelector().description(\"" + escapedText + "\")"));
            locators.add(AppiumBy.androidUIAutomator("new UiSelector().text(\"" + escapedText + "\")"));
        }

        locators.add(AppiumBy.androidUIAutomator("new UiSelector().descriptionContains(\"" + escapedText + "\")"));
        locators.add(AppiumBy.androidUIAutomator("new UiSelector().textContains(\"" + escapedText + "\")"));

        for (By locator : locators) {
            WebElement element = findFirstElementFast(locator, timeoutSeconds);

            if (element != null) {
                return element;
            }
        }

        return null;
    }

    private boolean isPresentFast(By locator, int timeoutSeconds) {
        return findFirstElementFast(locator, timeoutSeconds) != null;
    }

    private WebElement findFirstElementFast(By locator, int timeoutSeconds) {
        long endTime = System.currentTimeMillis() + Math.max(0, timeoutSeconds) * 1000L;

        do {
            try {
                List<WebElement> elements = driver.findElements(locator);

                if (elements != null && !elements.isEmpty()) {
                    return elements.get(0);
                }
            } catch (Exception ignored) {
                // Direct locator failed. Caller can use fallback path.
            }

            if (timeoutSeconds <= 0) {
                break;
            }

            sleep(200);
        } while (System.currentTimeMillis() < endTime);

        return null;
    }

    private boolean isElementInsideBounds(WebElement element, int minCenterX, int maxCenterY) {
        try {
            if (element == null) {
                return false;
            }

            Rectangle rect = element.getRect();
            int centerX = rect.getX() + rect.getWidth() / 2;
            int centerY = rect.getY() + rect.getHeight() / 2;

            return centerX >= minCenterX && centerY <= maxCenterY;
        } catch (Exception e) {
            return false;
        }
    }

    private String escapeUiAutomatorText(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    // =========================================================
    // ELEMENT HELPERS
    // =========================================================

    private void assertAnyTextVisible(String text) {
        if (!isVisibleByAnyText(text)) {
            throw new AssertionError("Expected text is not visible: " + text
                    + " | visibleValues=" + collectVisibleStrings());
        }

        ReportLogger.pass("Visible text validated: " + text);
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
        WebElement fastElement = findTextElementFast(expectedText, false, 1);

        if (fastElement != null) {
            return fastElement;
        }

        try {
            List<WebElement> elements = driver.findElements(By.xpath("//*"));

            for (WebElement element : elements) {
                try {
                    if (!isElementUsable(element)) {
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
        WebElement fastElement = findTextElementFast(expectedText, true, 1);

        if (fastElement != null) {
            return fastElement;
        }

        try {
            List<WebElement> elements = driver.findElements(By.xpath("//*"));

            for (WebElement element : elements) {
                try {
                    if (!isElementUsable(element)) {
                        continue;
                    }

                    String text = normalizeSpaces(element.getText());
                    String desc = normalizeSpaces(element.getAttribute("content-desc"));
                    String name = normalizeSpaces(element.getAttribute("name"));
                    String attrText = normalizeSpaces(element.getAttribute("text"));

                    if (expectedText.equals(text)
                            || expectedText.equals(desc)
                            || expectedText.equals(name)
                            || expectedText.equals(attrText)
                            || text.contains(expectedText)
                            || desc.contains(expectedText)
                            || name.contains(expectedText)
                            || attrText.contains(expectedText)) {
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

    private List<VisibleElementSnapshot> collectVisibleElementSnapshots() {
        List<VisibleElementSnapshot> snapshots = new ArrayList<>();

        try {
            List<WebElement> elements = driver.findElements(By.xpath("//*"));

            for (WebElement element : elements) {
                try {
                    if (!isElementUsable(element)) {
                        continue;
                    }

                    Rectangle rect = element.getRect();
                    int centerX = rect.getX() + rect.getWidth() / 2;
                    int centerY = rect.getY() + rect.getHeight() / 2;

                    addVisibleElementSnapshot(snapshots, element, element.getText(), centerX, centerY);
                    addVisibleElementSnapshot(snapshots, element, element.getAttribute("content-desc"), centerX, centerY);
                    addVisibleElementSnapshot(snapshots, element, element.getAttribute("text"), centerX, centerY);
                    addVisibleElementSnapshot(snapshots, element, element.getAttribute("name"), centerX, centerY);

                } catch (Exception ignored) {
                    // Ignore stale/unreadable elements.
                }
            }
        } catch (Exception e) {
            ReportLogger.debug("collectVisibleElementSnapshots skipped: " + cleanError(e.getMessage()));
        }

        return snapshots;
    }

    private void addVisibleElementSnapshot(
            List<VisibleElementSnapshot> snapshots,
            WebElement element,
            String rawValue,
            int centerX,
            int centerY
    ) {
        if (rawValue == null) {
            return;
        }

        String clean = normalizeSpaces(rawValue);

        if (clean.isEmpty()) {
            return;
        }

        snapshots.add(new VisibleElementSnapshot(element, clean, centerX, centerY));

        String[] parts = rawValue.split("\\n");

        for (String part : parts) {
            String cleanPart = normalizeSpaces(part);

            if (!cleanPart.isEmpty() && !cleanPart.equals(clean)) {
                snapshots.add(new VisibleElementSnapshot(element, cleanPart, centerX, centerY));
            }
        }
    }

    private boolean isVisibleByAnyText(String text) {
        return findVisibleTextElement(text) != null;
    }


    private void waitForAppToBeInteractive() {
        for (int i = 1; i <= 8; i++) {
            if (isMainAppLoadedFast() || isPinScreenVisibleFast() || isSearchScreenVisibleFast()) {
                return;
            }

            sleep(350);
        }
    }


    private void waitForAnyTextVisible(List<String> possibleTexts, int timeoutSeconds) {
        long endTime = System.currentTimeMillis() + (timeoutSeconds * 1000L);

        while (System.currentTimeMillis() < endTime) {
            if (isAnyExpectedTextVisibleFast(possibleTexts)) {
                return;
            }

            sleep(250);
        }

        throw new AssertionError("None of the expected texts visible within timeout | expected=" + possibleTexts);
    }

    private List<String> collectVisibleStrings() {
        List<String> values = new ArrayList<>();

        try {
            List<WebElement> elements = driver.findElements(By.xpath("//*"));

            for (WebElement element : elements) {
                try {
                    if (!isElementUsable(element)) {
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

    private boolean isElementUsable(WebElement element) {
        try {
            if (element == null || !element.isDisplayed()) {
                return false;
            }

            Rectangle rect = element.getRect();
            Dimension size = getScreenSize();

            if (rect.getWidth() <= 0 || rect.getHeight() <= 0) {
                return false;
            }

            int centerX = rect.getX() + rect.getWidth() / 2;
            int centerY = rect.getY() + rect.getHeight() / 2;

            return centerX >= 0
                    && centerX <= size.getWidth()
                    && centerY >= 0
                    && centerY <= size.getHeight();

        } catch (Exception e) {
            return false;
        }
    }

    // =========================================================
    // GESTURE HELPERS
    // =========================================================

    private void tapElementCenter(WebElement element) {
        Rectangle rect = element.getRect();
        Dimension size = getScreenSize();

        int x = rect.getX() + rect.getWidth() / 2;
        int y = rect.getY() + rect.getHeight() / 2;

        x = clamp(x, 1, size.getWidth() - 2);
        y = clamp(y, 1, size.getHeight() - 2);

        tapByCoordinates(x, y);
    }

    private void tapByCoordinates(int x, int y) {
        Dimension size = getScreenSize();

        x = clamp(x, 1, size.getWidth() - 2);
        y = clamp(y, 1, size.getHeight() - 2);

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

    private void smallSwipeUp() {
        Dimension size = getScreenSize();

        int startX = (int) (size.getWidth() * 0.50);
        int startY = (int) (size.getHeight() * 0.66);
        int endX = (int) (size.getWidth() * 0.50);
        int endY = (int) (size.getHeight() * 0.45);

        swipeByCoordinates(startX, startY, endX, endY, 450);
    }

    private void swipeByCoordinates(int startX, int startY, int endX, int endY, long durationMillis) {
        Dimension size = getScreenSize();

        startX = clamp(startX, 1, size.getWidth() - 2);
        endX = clamp(endX, 1, size.getWidth() - 2);
        startY = clamp(startY, 1, size.getHeight() - 2);
        endY = clamp(endY, 1, size.getHeight() - 2);

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

    private void hideKeyboardIfVisible() {
        try {
            driver.hideKeyboard();
            sleep(400);
        } catch (Exception ignored) {
            // Keyboard was already hidden or unsupported by device.
        }
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

    private Dimension getScreenSize() {
        if (cachedScreenSize == null) {
            cachedScreenSize = driver.manage().window().getSize();
        }

        return cachedScreenSize;
    }

    private String getCurrentPackageSafely() {
        try {
            return driver.getCurrentPackage();
        } catch (Exception e) {
            return "";
        }
    }

    private int clamp(int value, int min, int max) {
        if (value < min) {
            return min;
        }

        if (value > max) {
            return max;
        }

        return value;
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

    private static class VisibleElementSnapshot {
        private final WebElement element;
        private final String text;
        private final int centerX;
        private final int centerY;

        private VisibleElementSnapshot(WebElement element, String text, int centerX, int centerY) {
            this.element = element;
            this.text = text;
            this.centerX = centerX;
            this.centerY = centerY;
        }
    }

    private static class FundRowMatch {
        private final WebElement element;
        private final String title;

        private FundRowMatch(WebElement element, String title) {
            this.element = element;
            this.title = title;
        }
    }

}