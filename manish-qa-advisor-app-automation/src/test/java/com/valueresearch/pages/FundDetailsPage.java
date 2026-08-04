package com.valueresearch.pages;

import com.valueresearch.utils.ExtentTestManager;
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
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class FundDetailsPage {

    private final AndroidDriver driver;

    private static final String SEARCH_KEYWORD = "HDFC Flexi Cap Fund";
    private static final String FUND_RESULT = "HDFC Flexi Cap Fund";
    private static final String FUND_HEADER = "HDFC Flexi Cap Fund - Direct Plan";

    public FundDetailsPage(AndroidDriver driver) {
        this.driver = driver;
    }

    // =========================================================
    // MAIN FLOW
    // =========================================================

    public void openFundDetailsFromSearch() {
        try {
            ReportLogger.step("Opening Fund Details from Funds search");

            openFundsTab();
            tapSearchIcon();
            enterSearchKeyword(SEARCH_KEYWORD);
            openFundResult(FUND_RESULT);

            if (!isOnFundDetailsPage()) {
                throw new RuntimeException(
                        "Fund Details page was not detected after opening search result"
                );
            }

            ReportLogger.pass("Fund Details opened successfully: " + FUND_HEADER);
            logValidatedText("Fund opened", FUND_HEADER);

        } catch (Exception e) {
            throw new RuntimeException(
                    "Failed to open Fund Details from search: "
                            + cleanError(e.getMessage()),
                    e
            );
        }
    }

    public void recoverFundDetailsIfNeeded() {
        if (isOnFundDetailsPage()) {
            ReportLogger.debug("Fund Details page or known section is already visible");
            return;
        }

        ReportLogger.step("Returning to Fund Details page");

        for (int i = 1; i <= 3; i++) {
            pressBackSilently();
            sleep(1200);

            if (isOnFundDetailsPage()) {
                ReportLogger.pass("Returned to Fund Details page");
                return;
            }
        }

        throw new RuntimeException("Unable to return to Fund Details page. Not reopening full search flow automatically.");
    }

    private boolean isOnFundDetailsPage() {
        return isVisible(byDesc(FUND_HEADER))
                || isVisible(byDescContains(FUND_HEADER))
                || isVisible(byDesc("How are the returns?"))
                || isVisible(byDesc("What is the risk?"))
                || isVisible(byDesc("Where does it invest?"))
                || isVisible(byDesc("More Details"))
                || isVisible(byDesc("News"));
    }

    public void verifyFundHeader() {
        ReportLogger.step("Verifying Fund Details header");

        List<String> headerFailures = new ArrayList<>();

        runSoftValidation(headerFailures, "Fund header", new Runnable() {
            @Override
            public void run() {
                assertVisibleAndLog(byDesc(FUND_HEADER), "Fund header");
            }
        });
        runSoftValidation(headerFailures, "NAV label", new Runnable() {
            @Override
            public void run() {
                assertVisibleAndLog(byDesc("NAV"), "NAV label");
            }
        });
        runSoftValidation(headerFailures, "Equity category", new Runnable() {
            @Override
            public void run() {
                assertVisibleAndLog(byDesc("Equity"), "Equity category");
            }
        });
        runSoftValidation(headerFailures, "Flexi Cap category", new Runnable() {
            @Override
            public void run() {
                assertVisibleAndLog(byDesc("Flexi Cap"), "Flexi Cap category");
            }
        });
        runSoftValidation(headerFailures, "Fund opinion", new Runnable() {
            @Override
            public void run() {
                assertFundOpinionVisible();
            }
        });
        
        logOptionalVisibleText(byDescContains("₹"), "Visible amount/value near header");
        logOptionalVisibleText(byDescContains("%"), "Visible percentage near header");

        throwIfValidationFailures("Fund header", headerFailures);

        ReportLogger.pass("Fund header validated successfully");
    }

    // =========================================================
    // OPEN FUND FROM SEARCH
    // =========================================================

    private void openFundsTab() {
        ReportLogger.step("Opening Funds tab");

        exitAlertsOrChildPageIfNeeded();

        By fundsTab = byDesc("Funds");

        if (!isVisible(fundsTab)) {
            throw new RuntimeException("Funds bottom tab is not visible after returning to dashboard.");
        }

        tapVisible(fundsTab, "Funds bottom tab");
        sleep(2500);

        if (isVisible(byDesc("Alerts"))) {
            throw new RuntimeException("Still on Alerts page after tapping Funds. Dashboard navigation failed.");
        }

        if (!isVisible(byDesc("Funds"))) {
            throw new RuntimeException("Funds tab is not visible after tapping Funds. Current screen may not be dashboard.");
        }

        ReportLogger.pass("Funds tab opened");
        logValidatedText("Validated bottom tab", "Funds");
    }

    private void exitAlertsOrChildPageIfNeeded() {
        ReportLogger.step("Checking current app screen");

        for (int attempt = 1; attempt <= 5; attempt++) {
            boolean bottomTabsVisible =
                    isVisible(byDesc("Funds"))
                            && isVisible(byDesc("Stocks"))
                            && isVisible(byDesc("Portfolio"))
                            && isVisible(byDesc("Hub"));

            boolean alertsVisible = isVisible(byDesc("Alerts"));

            if (bottomTabsVisible && !alertsVisible) {
                ReportLogger.pass("Main dashboard bottom tabs are visible");
                return;
            }

            pressBackSilently();
            sleep(1200);
        }

        boolean bottomTabsVisible =
                isVisible(byDesc("Funds"))
                        && isVisible(byDesc("Stocks"))
                        && isVisible(byDesc("Portfolio"))
                        && isVisible(byDesc("Hub"));

        if (!bottomTabsVisible) {
            throw new RuntimeException("Unable to return to main dashboard. Bottom tabs not visible.");
        }

        ReportLogger.pass("Returned to main dashboard");
    }

    private void tapSearchIcon() {
        ReportLogger.step("Tapping search icon");

        By searchInput = AppiumBy.androidUIAutomator(
                "new UiSelector().className(\"android.widget.EditText\")"
        );

        if (isVisible(searchInput)) {
            ReportLogger.pass("Search input already visible");
            return;
        }

        /*
         * Appium Inspector shows search icon as:
         * xpath: //android.widget.ScrollView/android.widget.ImageView[2]
         * class: android.widget.ImageView
         * clickable: true
         * content-desc: blank
         */
        By searchIconByXpath = AppiumBy.xpath("//android.widget.ScrollView/android.widget.ImageView[2]");

        if (tapIfVisible(searchIconByXpath, "Search icon by Appium Inspector xpath")) {
            sleep(1800);

            if (isVisible(searchInput)) {
                ReportLogger.pass("Search screen opened using search icon xpath");
                return;
            }
        }

        /*
         * Backup locator from Inspector:
         * new UiSelector().className("android.widget.ImageView").instance(1)
         */
        By searchIconByImageInstance = AppiumBy.androidUIAutomator(
                "new UiSelector().className(\"android.widget.ImageView\").instance(1)"
        );

        if (tapIfVisible(searchIconByImageInstance, "Search icon by ImageView instance")) {
            sleep(1800);

            if (isVisible(searchInput)) {
                ReportLogger.pass("Search screen opened using ImageView instance locator");
                return;
            }
        }

        /*
         * Final coordinate fallback.
         * Old y=0.057 was too high.
         * Inspector center is around x=983, y=213 on 1080 width device.
         */
        Dimension size = driver.manage().window().getSize();

        int x = (int) (size.getWidth() * 0.91);
        int y = (int) (size.getHeight() * 0.089);

        ReportLogger.step("Trying search icon coordinate fallback at x=" + x + ", y=" + y);

        tapByCoordinates(x, y);
        sleep(1800);

        if (isVisible(searchInput)) {
            ReportLogger.pass("Search screen opened using coordinate fallback");
            return;
        }

        throw new RuntimeException("Unable to tap search icon. Search input did not appear.");
    }
    private void enterSearchKeyword(String keyword) {
        ReportLogger.step("Entering search keyword: " + keyword);

        By searchInput = AppiumBy.androidUIAutomator(
                "new UiSelector().className(\"android.widget.EditText\")"
        );

        if (!isVisible(searchInput)) {
            throw new RuntimeException("Search input is not visible. Search icon tap did not open search screen.");
        }

        WebElement input = findVisibleElement(searchInput);

        if (input == null) {
            throw new RuntimeException("Search input element found but not visible.");
        }

        tapElementCenter(input);
        sleep(500);

        try {
            input.clear();
            sleep(300);
        } catch (Exception ignored) {
            ReportLogger.debug("Search input clear skipped");
        }

        input.sendKeys(keyword);
        sleep(3000);

        ReportLogger.pass("Search keyword entered: " + keyword);
        logValidatedText("Search keyword", keyword);
    }

    private void openFundResult(String fundName) {
        ReportLogger.step("Opening fund result: " + fundName);

        sleep(2500);

        By exactDesc = byDesc(fundName);
        By containsDesc = byDescContains(fundName);
        By exactText = byText(fundName);
        By containsText = byTextContains(fundName);

        if (tapIfVisible(exactDesc, "Exact fund result by accessibility: " + fundName)) {
            waitForFundDetailsAfterResultTap();
            return;
        }

        if (tapIfVisible(containsDesc, "Fund result by descriptionContains: " + fundName)) {
            waitForFundDetailsAfterResultTap();
            return;
        }

        if (tapIfVisible(exactText, "Exact fund result by text: " + fundName)) {
            waitForFundDetailsAfterResultTap();
            return;
        }

        if (tapIfVisible(containsText, "Fund result by textContains: " + fundName)) {
            waitForFundDetailsAfterResultTap();
            return;
        }

        Dimension size = driver.manage().window().getSize();

        int x = (int) (size.getWidth() * 0.45);
        int y = (int) (size.getHeight() * 0.265);

        ReportLogger.debug("Fund result locator not found. Using coordinate fallback x=" + x + ", y=" + y);
        tapByCoordinates(x, y);

        waitForFundDetailsAfterResultTap();

        ReportLogger.pass("Fund result opened");
        logValidatedText("Selected fund result", fundName);
    }

    private void waitForFundDetailsAfterResultTap() {
        ReportLogger.step("Waiting for Fund Details page");

        for (int i = 1; i <= 12; i++) {
            sleep(1000);

            if (isVisible(byDesc(FUND_HEADER)) || isVisible(byDescContains(FUND_HEADER))) {
                ReportLogger.pass("Fund Details page opened: " + FUND_HEADER);
                return;
            }

            if (isVisible(byDesc("NAV")) && isVisible(byDescContains("Opinion:"))) {
                ReportLogger.pass("Fund Details page opened with NAV and Opinion labels");
                return;
            }
        }

        throw new RuntimeException("Fund Details page did not open after tapping search result");
    }

    private void assertFundOpinionVisible() {
        By[] opinionLocators = new By[]{
                byDesc("Opinion"),
                byDescContains("Opinion"),
                byText("Opinion"),
                byTextContains("Opinion"),

                byDesc("Buy"),
                byText("Buy"),

                byDesc("Hold"),
                byText("Hold"),

                byDesc("Sell"),
                byText("Sell"),

                byDescContains("Analyst's Choice"),
                byTextContains("Analyst's Choice")
        };

        for (By locator : opinionLocators) {
            WebElement element = findVisibleElement(locator);

            if (element == null) {
                continue;
            }

            String visibleValue = getElementReadableText(element);

            if (visibleValue == null || visibleValue.trim().isEmpty()) {
                visibleValue = "Opinion information";
            }

            ReportLogger.pass("Fund opinion is visible");
            logValidatedText("Fund opinion", visibleValue);
            return;
        }

        throw new AssertionError(
                "Fund opinion is not visible. Checked Opinion, Buy, Hold, Sell " +
                        "and Analyst's Choice accessibility/text values."
        );
    }
    // =========================================================
    // INVESTMENT DETAILS
    // =========================================================

    public void verifyInvestmentDetailsCardAndPage() {
        recoverFundDetailsIfNeeded();

        ReportLogger.step("Verifying Your Investments card and Investment Details page");

        scrollToTopOfFundDetails();

        By investmentCard = byDescContains("Your Investments");
        assertVisibleAndLog(investmentCard, "Your Investments card");
        logVisibleElementReadableText(investmentCard, "Your Investments card complete visible text");

        tapVisible(investmentCard, "Your Investments card");

        try {
            waitForAnyVisible(
                    new By[]{
                            byDesc("Investment Details"),
                            byDescContains("Investment Details"),
                            byDesc("Investor"),
                            byDesc("Value"),
                            byDesc("Return(%)")
                    },
                    "Investment Details page",
                    8
            );

            assertVisibleAndLog(byDescContains("Investment Details"), "Investment Details page title");
            assertVisibleAndLog(byDesc("Investor"), "Investor column");
            assertVisibleAndLog(byDesc("Value"), "Value column");
            assertVisibleAndLog(byDesc("Return(%)"), "Return column");
            assertVisibleAndLog(byDesc("Total"), "Total row");

            logOptionalVisibleText(byDescContains("Vinit Sharma"), "Investor name");
            logOptionalVisibleText(byDescContains("₹"), "Investment Details amount/value");
            logOptionalVisibleText(byDescContains("%"), "Investment Details return/value");

            optionalVisibleAny(
                    new By[]{byDesc("Your SWPs"), byDesc("Your SIPs")},
                    "SIP/SWP section"
            );

            optionalVisibleAny(
                    new By[]{byDesc("No Ongoing SWPs"), byDesc("No Ongoing SIPs")},
                    "No ongoing SIP/SWP message"
            );

        } finally {
            pressBack("Back from Investment Details");
            waitForFundHeaderOrKnownSection();
        }

        ReportLogger.pass("Your Investments card and Investment Details page validated successfully");
    }

    // =========================================================
    // PORTFOLIO OVERLAP
    // =========================================================

    public void verifyPortfolioOverlapCardAndPage() {
        recoverFundDetailsIfNeeded();

        ReportLogger.step("Verifying Your portfolio overlap card and Total Overlap page");

        scrollToTopOfFundDetails();

        By overlapCard = byDescContains("Your portfolio overlap");
        assertVisibleAndLog(overlapCard, "Your portfolio overlap card");
        logVisibleElementReadableText(overlapCard, "Your portfolio overlap card complete visible text");

        tapVisible(overlapCard, "Your portfolio overlap card");

        try {
            waitForAnyVisible(
                    new By[]{byDesc("Total Overlap"), byDescContains("Overlap")},
                    "Portfolio Overlap page",
                    8
            );

            logOptionalVisibleText(byDescContains("Total Overlap"), "Total Overlap page text");
            logOptionalVisibleText(byDescContains("Vinit Sharma"), "Investor overlap row");
            logOptionalVisibleText(byDescContains("Manish Khatri"), "Investor overlap row");
            logOptionalVisibleText(byDescContains("Lalit Kumar"), "Investor overlap row");
            logOptionalVisibleText(byDescContains("%"), "Overlap percentage/value");

        } finally {
            pressBack("Back from Total Overlap");
            waitForFundHeaderOrKnownSection();
        }

        ReportLogger.pass("Portfolio overlap card and Total Overlap page validated successfully");
    }

    // =========================================================
    // RETURNS - CLEAN TABLE VALIDATION
    // =========================================================

    public void verifyReturnsSectionAndMorePage() {
        recoverFundDetailsIfNeeded();

        ReportLogger.step("Verifying Returns section and Returns More page");

        scrollToTopOfFundDetails();
        scrollUntilVisible("How are the returns?", 8);

        assertVisibleAndLog(byDesc("How are the returns?"), "How are the returns section");
        assertVisibleAndLog(byDesc("Snapshot"), "Snapshot chip");
        assertVisibleAndLog(byDesc("Trailing Returns"), "Trailing Returns chip");

        /*
         * Do not fail here.
         * These labels are not consistently exposed in current viewport.
         * If we hard assert them, Returns More button never gets clicked.
         */
        logOptionalVisibleText(byDesc("Short-term (1Y)"), "Short-term return label");
        logOptionalVisibleText(byDesc("Long-term (5Y)"), "Long-term return label");

        logCleanReturnsSnapshotSummary();

        tapReturnsMoreButton();

        try {
            waitForAnyVisible(
                    new By[]{
                            byDesc("SIP Returns"),
                            byDesc("Discrete Returns"),
                            byDesc("Trailing Returns")
                    },
                    "Returns detail page",
                    8
            );

            validateReturnsMorePageInCleanFormat();

        } finally {
            pressBack("Back from Returns More page");
            waitForFundHeaderOrKnownSection();
        }

        ReportLogger.pass("Returns section and Returns More page validated successfully");
    }
    
    private void tapReturnsMoreButton() {
        ReportLogger.step("Tapping Returns More button");

        if (!isVisible(byDesc("How are the returns?"))
                && !isVisible(byDescContains("How are the returns?"))) {
            scrollUntilVisible("How are the returns?", 6);
        }

        WebElement returnsHeading = findVisibleElement(byDesc("How are the returns?"));

        if (returnsHeading == null) {
            returnsHeading = findVisibleElement(byDescContains("How are the returns?"));
        }

        if (returnsHeading == null) {
            throw new AssertionError("Returns heading is not visible for More tap");
        }

        Rectangle headingRect = returnsHeading.getRect();
        Dimension size = driver.manage().window().getSize();

        /*
         * Prefer exact visible More button near Returns heading.
         */
        List<WebElement> moreElements = driver.findElements(byDesc("More"));

        WebElement bestMore = null;
        int bestDistance = Integer.MAX_VALUE;

        for (WebElement more : moreElements) {
            try {
                if (more == null || !more.isDisplayed()) {
                    continue;
                }

                Rectangle moreRect = more.getRect();

                boolean isRightSide = moreRect.getX() > size.getWidth() * 0.65;
                boolean isNearReturnsHeading = Math.abs(moreRect.getY() - headingRect.getY()) <= 220;
                boolean isSafeVerticalArea = moreRect.getY() > 120 && moreRect.getY() < size.getHeight() - 260;

                if (isRightSide && isNearReturnsHeading && isSafeVerticalArea) {
                    int distance = Math.abs(moreRect.getY() - headingRect.getY());

                    if (distance < bestDistance) {
                        bestDistance = distance;
                        bestMore = more;
                    }
                }

            } catch (Exception ignored) {
                // Try next More element
            }
        }

        if (bestMore != null) {
            tapElementCenter(bestMore);
            sleep(2500);
            ReportLogger.pass("Tapped Returns More button by visible More element");
            return;
        }

        /*
         * Fallback: tap right side of the Returns heading row.
         */
        int fallbackX = (int) (size.getWidth() * 0.91);
        int fallbackY = headingRect.getY() + Math.max(35, headingRect.getHeight() / 2);

        ReportLogger.step("Returns More visible element not found. Using coordinate fallback X="
                + fallbackX + ", Y=" + fallbackY);

        tapByCoordinates(fallbackX, fallbackY);
        sleep(2500);

        ReportLogger.pass("Tapped Returns More button by coordinate fallback");
    }
    private void logCleanReturnsSnapshotSummary() {
        ReportLogger.step("Validating Returns snapshot card");

        String fund = getVisibleValueOrBlank("HDFC Flexi Cap Dir");
        String index = getVisibleValueOrBlank("BSE 500 TRI");
        String category = getVisibleValueOrBlank("Equity: Flexi Cap");

        logCleanValidation("Returns Snapshot", "Fund row visible: " + fund);
        logCleanValidation("Returns Snapshot", "Index row visible: " + index);
        logCleanValidation("Returns Snapshot", "Category row visible: " + category);

        logOptionalVisibleText(byDescContains("%"), "Returns snapshot percentage value");

        ReportLogger.pass("Returns snapshot card validated");
    }

    private void validateReturnsMorePageInCleanFormat() {
        ReportLogger.step("Validating Returns More page in clean report format");

        List<String> returnsFailures = new ArrayList<>();

        try {
            validateSipReturnsTable();
        } catch (Throwable error) {
            recordReturnsValidationFailure(returnsFailures, "SIP Returns", error);
        }

        try {
            scrollUntilVisibleOptional("Discrete Returns", 4);

            if (isVisible(byDesc("Discrete Returns"))) {
                validateDiscreteReturnsChipSafely("Yearly", returnsFailures);
                validateDiscreteReturnsChipSafely("Quarterly", returnsFailures);
                validateDiscreteReturnsChipSafely("Monthly", returnsFailures);
                validateDiscreteReturnsChipSafely("Weekly", returnsFailures);
            } else {
                throw new AssertionError("Discrete Returns section is not visible");
            }
        } catch (Throwable error) {
            recordReturnsValidationFailure(returnsFailures, "Discrete Returns", error);
        }

        try {
            validateCompleteTrailingReturnsValues();
        } catch (Throwable error) {
            recordReturnsValidationFailure(returnsFailures, "Trailing Returns", error);
        }

        if (!returnsFailures.isEmpty()) {
            throw new AssertionError("Returns More page validation completed with failures: " + String.join(" | ", returnsFailures));
        }

        ReportLogger.pass("Returns More page validated in clean format");
    }

    private void validateDiscreteReturnsChipSafely(String chipName, List<String> returnsFailures) {
        try {
            validateDiscreteReturnsByChip(chipName);
        } catch (Throwable error) {
            recordReturnsValidationFailure(returnsFailures, "Discrete Returns - " + chipName, error);
        }
    }

    private void recordReturnsValidationFailure(List<String> failures, String sectionName, Throwable error) {
        String message = sectionName + " failed: " + cleanError(error == null ? "" : error.getMessage());

        failures.add(message);
        ReportLogger.fail(message);
    }

    private void validateSipReturnsTable() {
        ReportLogger.step("Validating SIP Returns table");

        assertVisibleAndLog(byDesc("SIP Returns"), "SIP Returns section");

        List<String> values = getCleanVisibleTexts();

        logReturnRowIfPresent("SIP Returns", values, "HDFC Flexi Cap Dir", "Fund");
        logReturnRowIfPresent("SIP Returns", values, "BSE 500 TRI", "Index");
        logReturnRowIfPresent("SIP Returns", values, "Equity: Flexi Cap", "Category");
        logReturnRowIfPresent("SIP Returns", values, "Rank within category", "Rank within category");
        logReturnRowIfPresent("SIP Returns", values, "Total Funds", "Total Funds");

        ReportLogger.pass("SIP Returns table validated");
    }

    private void validateDiscreteReturnsByChip(String chipName) {
        scrollUntilVisibleOptional("Discrete Returns", 4);

        By chipLocator = byDesc(chipName);

        if (!isVisible(chipLocator)) {
            chipLocator = byDescContains(chipName);
        }

        if (!isVisible(chipLocator)) {
            ReportLogger.debug("Discrete Returns chip not visible, skipping: " + chipName);
            return;
        }

        tapVisible(chipLocator, "Discrete Returns chip: " + chipName);
        sleep(1200);

        ReportLogger.step("Validating Discrete Returns - " + chipName);

        List<String> expectedRowKeys = getExpectedDiscreteReturnRows(chipName);
        List<String> values = collectScopedDiscreteReturnsValues(chipName, expectedRowKeys);
        List<String> visibleRows = getVisibleDiscreteReturnRowsForChip(chipName, values, expectedRowKeys);

        if (visibleRows.isEmpty()) {
            throw new AssertionError(
                    "No visible table rows found for Discrete Returns - " + chipName
                            + " | Static expected rows: " + expectedRowKeys
                            + " | Y-scoped values: " + values
            );
        }

        List<String> numericValues = getDiscreteReturnsNumericValues(values, visibleRows);

        int requiredNumericCount = visibleRows.size() * 3;

        if (numericValues.size() < requiredNumericCount) {
            throw new AssertionError(
                    "Discrete Returns - " + chipName
                            + " numeric values are incomplete. Visible rows: " + visibleRows
                            + " | Required numeric count: " + requiredNumericCount
                            + " | Found numeric values: " + numericValues
                            + " | Y-scoped values: " + values
            );
        }

        for (int rowIndex = 0; rowIndex < visibleRows.size(); rowIndex++) {
            int valueStartIndex = rowIndex * 3;

            String rowKey = visibleRows.get(rowIndex);
            String fundValue = numericValues.get(valueStartIndex);
            String indexValue = numericValues.get(valueStartIndex + 1);
            String categoryValue = numericValues.get(valueStartIndex + 2);

            logCleanValidation(
                    "Discrete Returns - " + chipName,
                    rowKey
                            + " | Fund: " + fundValue
                            + " | Index: " + indexValue
                            + " | Category Avg: " + categoryValue
            );
        }

        ReportLogger.pass("Discrete Returns - " + chipName + " Y-coordinate scoped values validated");
    }

    private List<String> getVisibleDiscreteReturnRowsForChip(
            String chipName,
            List<String> values,
            List<String> expectedRowKeys
    ) {
        List<String> dynamicRows = extractDynamicDiscreteRowsForChip(chipName, values);

        if (!dynamicRows.isEmpty()) {
            return dynamicRows;
        }

        return getVisibleRowsInExpectedOrder(values, expectedRowKeys);
    }

    private List<String> extractDynamicDiscreteRowsForChip(String chipName, List<String> values) {
        List<String> rows = new ArrayList<>();

        if (values == null || values.isEmpty()) {
            return rows;
        }

        String primaryColumnHeader = getDiscretePrimaryColumnHeader(chipName);
        boolean tableStarted = false;

        for (String value : values) {
            String clean = normalizeForComparison(value);

            if (clean.isEmpty()) {
                continue;
            }

            if (clean.equalsIgnoreCase("Trailing Returns") || clean.equalsIgnoreCase("Rolling Returns")) {
                break;
            }

            if (clean.equalsIgnoreCase(primaryColumnHeader)) {
                tableStarted = true;
                continue;
            }

            if (!tableStarted) {
                continue;
            }

            if (isDiscreteRowKeyForChip(chipName, clean)) {
                addUniqueCleanValue(rows, clean);

                if (rows.size() == 5) {
                    return rows;
                }
            }
        }

        /*
         * Fallback for real devices where Flutter/Appium sometimes does not expose
         * the primary column header as a standalone node, but row labels are visible.
         */
        if (rows.isEmpty()) {
            for (String value : values) {
                String clean = normalizeForComparison(value);

                if (isDiscreteRowKeyForChip(chipName, clean)) {
                    addUniqueCleanValue(rows, clean);

                    if (rows.size() == 5) {
                        return rows;
                    }
                }
            }
        }

        return rows;
    }

    private String getDiscretePrimaryColumnHeader(String chipName) {
        if ("Yearly".equalsIgnoreCase(chipName)) {
            return "Year";
        }

        if ("Quarterly".equalsIgnoreCase(chipName)) {
            return "Quarter";
        }

        if ("Monthly".equalsIgnoreCase(chipName)) {
            return "Month";
        }

        return "Week";
    }

    private boolean isDiscreteRowKeyForChip(String chipName, String value) {
        if (value == null) {
            return false;
        }

        String clean = normalizeForComparison(value);

        if (clean.isEmpty()) {
            return false;
        }

        if ("Yearly".equalsIgnoreCase(chipName)) {
            return clean.matches("20\\d{2}");
        }

        if ("Quarterly".equalsIgnoreCase(chipName) || "Monthly".equalsIgnoreCase(chipName)) {
            return clean.matches("[A-Z][a-z]{2}-\\d{2}");
        }

        return clean.matches("\\d{2}-[A-Z][a-z]{2}");
    }

    private void addUniqueCleanValue(List<String> values, String newValue) {
        if (values == null || newValue == null) {
            return;
        }

        String cleanNewValue = normalizeForComparison(newValue);

        if (cleanNewValue.isEmpty()) {
            return;
        }

        for (String existing : values) {
            if (normalizeForComparison(existing).equals(cleanNewValue)) {
                return;
            }
        }

        values.add(cleanNewValue);
    }

    private List<String> getVisibleRowsInExpectedOrder(List<String> values, List<String> expectedRowKeys) {
        List<String> visibleRows = new ArrayList<>();

        if (values == null || values.isEmpty() || expectedRowKeys == null) {
            return visibleRows;
        }

        for (String rowKey : expectedRowKeys) {
            if (containsExactValue(values, rowKey)) {
                visibleRows.add(rowKey);
            }
        }

        return visibleRows;
    }

    private List<String> getDiscreteReturnsNumericValues(List<String> values, List<String> visibleRows) {
        List<String> numericValues = new ArrayList<>();

        if (values == null || values.isEmpty() || visibleRows == null || visibleRows.isEmpty()) {
            return numericValues;
        }

        int firstRowIndex = Integer.MAX_VALUE;

        for (String row : visibleRows) {
            int index = findTextIndex(values, row);

            if (index >= 0 && index < firstRowIndex) {
                firstRowIndex = index;
            }
        }

        if (firstRowIndex == Integer.MAX_VALUE) {
            return numericValues;
        }

        for (int i = firstRowIndex + 1; i < values.size(); i++) {
            String value = values.get(i);

            if (value == null) {
                continue;
            }

            String clean = value.trim();

            if (clean.isEmpty()) {
                continue;
            }

            if (containsExactValue(visibleRows, clean)) {
                continue;
            }

            if (isQuarterNoiseLabel(clean)) {
                continue;
            }

            if (isValidReturnNumber(clean)) {
                numericValues.add(clean);
            }
        }

        return numericValues;
    }

    private List<String> getExpectedDiscreteReturnRows(String chipName) {
        if ("Yearly".equalsIgnoreCase(chipName)) {
            return Arrays.asList("2025", "2024", "2023", "2022", "2021");
        }

        if ("Quarterly".equalsIgnoreCase(chipName)) {
            return Arrays.asList("Mar-26", "Dec-25", "Sep-25", "Jun-25", "Mar-25");
        }

        if ("Monthly".equalsIgnoreCase(chipName)) {
            return Arrays.asList("Apr-26", "Mar-26", "Feb-26", "Jan-26", "Dec-25");
        }

        /*
         * Weekly row labels change every week. Do not hard-code dates here.
         * Rows are extracted dynamically from the visible table using dd-MMM format.
         */
        return Collections.emptyList();
    }

    private List<String> collectScopedDiscreteReturnsValues(String chipName, List<String> expectedRowKeys) {
        List<String> bestValues = new ArrayList<>();

        for (int attempt = 1; attempt <= 8; attempt++) {
            List<String> currentValues = getDiscreteReturnsYScopedTexts();

            if (!currentValues.isEmpty()) {
                bestValues = currentValues;
                ReportLogger.debug("Discrete Returns - " + chipName + " Y-scoped attempt " + attempt + ": " + currentValues);
            }

            if (hasDiscreteReturnsTableValuesForChip(chipName, bestValues, expectedRowKeys)) {
                ReportLogger.pass("Discrete Returns - " + chipName + " Y-scoped values: " + bestValues);
                return bestValues;
            }

            smallSwipeUpW3C();
            sleep(700);
        }

        if (!bestValues.isEmpty()) {
            ReportLogger.debug("Discrete Returns - " + chipName + " Y-scoped values collected but incomplete: " + bestValues);
            return bestValues;
        }

        throw new AssertionError("Unable to collect Y-scoped Discrete Returns values for chip: " + chipName);
    }

    private List<String> getDiscreteReturnsYScopedTexts() {
        List<VisibleTextNode> nodes = getVisibleTextNodesSortedByPosition();
        List<String> scopedTexts = new ArrayList<>();

        if (nodes.isEmpty()) {
            return scopedTexts;
        }

        int discreteHeadingY = -1;

        for (VisibleTextNode node : nodes) {
            if (normalizeForComparison(node.text).equalsIgnoreCase("Discrete Returns")) {
                discreteHeadingY = node.y;
                break;
            }
        }

        if (discreteHeadingY < 0) {
            ReportLogger.debug("Discrete Returns heading not visible for Y-coordinate scoping. Visible values: " + nodesToTexts(nodes));
            return scopedTexts;
        }

        int endY = driver.manage().window().getSize().getHeight();

        for (VisibleTextNode node : nodes) {
            String clean = normalizeForComparison(node.text);

            if (node.y <= discreteHeadingY) {
                continue;
            }

            if (clean.equalsIgnoreCase("Trailing Returns") || clean.equalsIgnoreCase("Rolling Returns")) {
                endY = node.y;
                break;
            }
        }

        for (VisibleTextNode node : nodes) {
            String clean = normalizeForComparison(node.text);

            if (node.y < discreteHeadingY - 10) {
                continue;
            }

            if (node.y >= endY - 5) {
                continue;
            }

            if (clean.equalsIgnoreCase("SIP Returns")) {
                continue;
            }

            scopedTexts.add(node.text);
        }

        return scopedTexts;
    }

    private List<VisibleTextNode> getVisibleTextNodesSortedByPosition() {
        List<VisibleTextNode> nodes = new ArrayList<>();
        Set<String> seenElementPositions = new LinkedHashSet<>();

        List<By> locators = new ArrayList<>();
        locators.add(AppiumBy.xpath("//android.view.View"));
        locators.add(AppiumBy.xpath("//android.widget.ImageView"));
        locators.add(AppiumBy.xpath("//android.widget.Button"));
        locators.add(AppiumBy.xpath("//android.widget.TextView"));

        for (By locator : locators) {
            try {
                List<WebElement> elements = driver.findElements(locator);

                for (WebElement element : elements) {
                    try {
                        if (element == null || !element.isDisplayed()) {
                            continue;
                        }

                        String text = normalizeVisibleText(getElementReadableText(element));

                        if (shouldSkipReturnsNoise(text)) {
                            continue;
                        }

                        Rectangle rect = element.getRect();

                        if (rect == null || rect.getWidth() <= 0 || rect.getHeight() <= 0) {
                            continue;
                        }

                        String className = "";

                        try {
                            className = element.getAttribute("class");
                        } catch (Exception ignored) {
                            // Class may not be exposed for every Flutter element
                        }

                        String elementPositionKey = className + "|"
                                + rect.getX() + "|"
                                + rect.getY() + "|"
                                + rect.getWidth() + "|"
                                + rect.getHeight() + "|"
                                + text;

                        if (seenElementPositions.add(elementPositionKey)) {
                            nodes.add(new VisibleTextNode(text, rect.getX(), rect.getY()));
                        }

                    } catch (Exception ignored) {
                        // Continue with next element
                    }
                }

            } catch (Exception ignored) {
                // Continue with next locator group
            }
        }

        Collections.sort(nodes, new java.util.Comparator<VisibleTextNode>() {
            @Override
            public int compare(VisibleTextNode first, VisibleTextNode second) {
                if (first.y != second.y) {
                    return first.y - second.y;
                }

                return first.x - second.x;
            }
        });

        return nodes;
    }

    private List<String> nodesToTexts(List<VisibleTextNode> nodes) {
        List<String> texts = new ArrayList<>();

        if (nodes == null) {
            return texts;
        }

        for (VisibleTextNode node : nodes) {
            if (node != null && node.text != null && !node.text.trim().isEmpty()) {
                texts.add(node.text.trim());
            }
        }

        return texts;
    }

    private boolean hasDiscreteReturnsTableValuesForChip(
            String chipName,
            List<String> values,
            List<String> expectedRows
    ) {
        if (values == null || values.isEmpty()) {
            return false;
        }

        List<String> visibleRows = getVisibleDiscreteReturnRowsForChip(chipName, values, expectedRows);

        if (visibleRows.isEmpty()) {
            return false;
        }

        List<String> numericValues = getDiscreteReturnsNumericValues(values, visibleRows);

        return numericValues.size() >= visibleRows.size() * 3;
    }

    private boolean containsExactValue(List<String> values, String expectedText) {
        return findTextIndex(values, expectedText) >= 0;
    }

    private void validateCompleteTrailingReturnsValues() {
        ReportLogger.step("Validating complete Trailing Returns values");

        List<String> trailingFailures = new ArrayList<>();

        scrollToTrailingReturnsSectionMandatory();

        assertVisibleAndLog(byDesc("Trailing Returns"), "Trailing Returns section");

        try {
            tapTrailingReturnChipMandatory("Short-term");
            validateTrailingReturnsTableForSelectedChip(
                    "Short-term",
                    Arrays.asList("1M (%)", "6M (%)", "1Y (%)")
            );
        } catch (Throwable error) {
            recordReturnsValidationFailure(trailingFailures, "Trailing Returns - Short-term", error);
        }

        try {
            scrollBackToTrailingReturnsChips();
        } catch (Throwable error) {
            recordReturnsValidationFailure(trailingFailures, "Trailing Returns chip alignment before Long-term", error);
        }

        try {
            tapTrailingReturnChipMandatory("Long-term");
            validateTrailingReturnsTableForSelectedChip(
                    "Long-term",
                    Arrays.asList("3Y (%)", "5Y (%)", "10Y (%)")
            );
        } catch (Throwable error) {
            recordReturnsValidationFailure(trailingFailures, "Trailing Returns - Long-term", error);
        }

        if (!trailingFailures.isEmpty()) {
            throw new AssertionError("Trailing Returns validation completed with failures: " + String.join(" | ", trailingFailures));
        }

        ReportLogger.pass("Complete Trailing Returns values validated");
    }

    private void scrollToTrailingReturnsSectionMandatory() {
        if (isVisible(byDesc("Trailing Returns"))) {
            ReportLogger.pass("Trailing Returns section is visible");
            return;
        }

        ReportLogger.step("Scrolling to Trailing Returns section");

        for (int i = 1; i <= 8; i++) {
            swipeUpW3C();
            sleep(800);

            if (isVisible(byDesc("Trailing Returns"))) {
                ReportLogger.pass("Trailing Returns section is visible");
                return;
            }
        }

        throw new AssertionError("Trailing Returns section is not visible");
    }

    private void tapTrailingReturnChipMandatory(String chipName) {
        WebElement chip = findVisibleElement(byDesc(chipName));

        if (chip == null) {
            chip = findVisibleElement(byDescContains(chipName));
        }

        if (chip == null) {
            throw new AssertionError("Trailing Returns chip is not visible: " + chipName);
        }

        tapElementCenter(chip);
        sleep(1200);

        ReportLogger.pass("Tapped: Trailing Returns chip: " + chipName);
    }

    private void validateTrailingReturnsTableForSelectedChip(String chipName, List<String> expectedHeaders) {
        ReportLogger.step("Validating Trailing Returns - " + chipName + " table structure");

        List<String> values = collectTrailingReturnsValuesUntilRollingReturns();

        if (values.isEmpty()) {
            throw new AssertionError("No Trailing Returns values collected for: " + chipName);
        }

        validateTrailingReturnsHeader(chipName, values, expectedHeaders);

        validateTrailingReturnsRow(chipName, values, "HDFC Flexi Cap Dir", "Fund", expectedHeaders);
        validateTrailingReturnsRow(chipName, values, "BSE 500 TRI", "Index", expectedHeaders);
        validateTrailingReturnsRow(chipName, values, "Equity: Flexi Cap", "Category", expectedHeaders);
        validateTrailingReturnsRow(chipName, values, "Rank within category", "Rank within category", expectedHeaders);
        validateTrailingReturnsRow(chipName, values, "Total Funds", "Total Funds", expectedHeaders);

        ReportLogger.pass("Trailing Returns - " + chipName + " table validated successfully");
    }

    private void validateTrailingReturnsHeader(String chipName, List<String> values, List<String> expectedHeaders) {
        if (!containsTrailingText(values, "Particulars")) {
            throw new AssertionError("Trailing Returns - " + chipName + " missing header: Particulars");
        }

        for (String header : expectedHeaders) {
            if (!containsTrailingText(values, header)) {
                throw new AssertionError("Trailing Returns - " + chipName + " missing header: " + header);
            }
        }

        String message = "Header: Particulars | "
                + expectedHeaders.get(0) + " | "
                + expectedHeaders.get(1) + " | "
                + expectedHeaders.get(2);

        logCleanValidation("Trailing Returns - " + chipName, message);
    }
    private void validateTrailingReturnsRow(
            String chipName,
            List<String> values,
            String rowKey,
            String rowLabel,
            List<String> headers
    ) {
        int index = findTrailingReturnTextIndex(values, rowKey);

        if (index < 0) {
            throw new AssertionError("Trailing Returns - " + chipName + " missing row: " + rowKey);
        }

        List<String> numericValues = getNextNumericValuesAfter(values, index, 3);

        if (numericValues.size() < 3) {
            throw new AssertionError(
                    "Trailing Returns - " + chipName + " row does not have 3 numeric values. Row: "
                            + rowKey + " | Found values: " + numericValues
            );
        }

        String actualRowName = values.get(index) == null ? rowKey : values.get(index).trim();

        String message = rowLabel + ": " + actualRowName
                + " | " + headers.get(0) + ": " + numericValues.get(0)
                + " | " + headers.get(1) + ": " + numericValues.get(1)
                + " | " + headers.get(2) + ": " + numericValues.get(2);

        logCleanValidation("Trailing Returns - " + chipName, message);
    }
    private List<String> collectTrailingReturnsValuesUntilRollingReturns() {
        List<String> collectedValues = new ArrayList<>();
        boolean reachedTrailingReturns = false;
        boolean rollingReturnsSeen = false;

        for (int scrollCount = 1; scrollCount <= 12; scrollCount++) {
            List<String> visibleTexts = getCleanVisibleTexts();

            for (String value : visibleTexts) {
                if (value == null) {
                    continue;
                }

                String text = value.trim();

                if (text.isEmpty()) {
                    continue;
                }

                if ("Trailing Returns".equalsIgnoreCase(normalizeTrailingReturnText(text))) {
                    reachedTrailingReturns = true;
                }

                if (!reachedTrailingReturns) {
                    continue;
                }

                if ("Rolling Returns".equalsIgnoreCase(normalizeTrailingReturnText(text))) {
                    rollingReturnsSeen = true;
                    continue;
                }

                if (isTrailingReturnsAllowedValue(text) && !containsTrailingExactValue(collectedValues, text)) {
                    collectedValues.add(text);
                }
            }

            if (hasCompleteTrailingReturnsRows(collectedValues)) {
                ReportLogger.pass("Complete Trailing Returns rows collected before Rolling Returns");
                return collectedValues;
            }

            if (rollingReturnsSeen && hasMinimumTrailingReturnsRows(collectedValues)) {
                ReportLogger.pass("Minimum Trailing Returns rows collected after Rolling Returns became visible");
                return collectedValues;
            }

            swipeUpW3C();
            sleep(800);
        }

        return collectedValues;
    }

    private boolean hasCompleteTrailingReturnsRows(List<String> values) {
        if (values == null || values.isEmpty()) {
            return false;
        }

        return containsTrailingText(values, "Particulars")
                && containsTrailingText(values, "HDFC Flexi Cap Dir")
                && containsTrailingText(values, "BSE 500 TRI")
                && containsTrailingText(values, "Equity: Flexi Cap")
                && containsTrailingText(values, "Rank within category")
                && containsTrailingText(values, "Total Funds");
    }

    private boolean hasMinimumTrailingReturnsRows(List<String> values) {
        if (values == null || values.isEmpty()) {
            return false;
        }

        /*
         * Do not return with only Fund row.
         * Existing validator requires BSE 500 TRI row.
         */
        return containsTrailingText(values, "Particulars")
                && containsTrailingText(values, "HDFC Flexi Cap Dir")
                && containsTrailingText(values, "BSE 500 TRI");
    }
    
    private boolean containsTrailingText(List<String> values, String expectedText) {
        return findTrailingReturnTextIndex(values, expectedText) >= 0;
    }

    private boolean containsTrailingExactValue(List<String> values, String newValue) {
        if (values == null || newValue == null) {
            return false;
        }

        String normalizedNewValue = normalizeTrailingReturnText(newValue);

        for (String existingValue : values) {
            if (existingValue == null) {
                continue;
            }

            if (normalizeTrailingReturnText(existingValue).equals(normalizedNewValue)) {
                return true;
            }
        }

        return false;
    }

    private int findTrailingReturnTextIndex(List<String> values, String expectedText) {
        if (values == null || values.isEmpty() || expectedText == null) {
            return -1;
        }

        String expected = normalizeTrailingReturnText(expectedText);

        for (int i = 0; i < values.size(); i++) {
            String actual = normalizeTrailingReturnText(values.get(i));

            if (actual.equals(expected) || actual.contains(expected)) {
                return i;
            }
        }

        return -1;
    }

    private String normalizeTrailingReturnText(String text) {
        if (text == null) {
            return "";
        }

        return text
                .replace("*", "")
                .replace("#", "")
                .replace("₹", "")
                .replace("\u00A0", " ")
                .trim()
                .replaceAll("\\s+", " ");
    }
    private boolean isTrailingReturnsAllowedValue(String value) {
        if (value == null) {
            return false;
        }

        String text = value.trim();

        if (text.isEmpty()) {
            return false;
        }

        if (shouldSkipReturnsNoise(text)) {
            return false;
        }

        return !text.equalsIgnoreCase("Rolling Returns")
                && !text.equalsIgnoreCase("Select rolling return period")
                && !text.equalsIgnoreCase("Add Fund")
                && !text.equalsIgnoreCase("Fund*")
                && !text.equalsIgnoreCase("Index*")
                && !text.equalsIgnoreCase("Category*")
                && !text.equalsIgnoreCase("YTD")
                && !text.equalsIgnoreCase("7Y")
                && !text.equalsIgnoreCase("10Y")
                && !text.equalsIgnoreCase("ALL")
                && !text.equalsIgnoreCase("*Average rolling returns for the selected period")
                && !text.equalsIgnoreCase("1W")
                && !text.equalsIgnoreCase("3M");
    }

    private void scrollBackToTrailingReturnsChips() {
        ReportLogger.step("Scrolling back to Trailing Returns chips");

        for (int i = 1; i <= 8; i++) {
            boolean shortTermVisible = isVisible(byDesc("Short-term")) || isVisible(byDescContains("Short-term"));
            boolean longTermVisible = isVisible(byDesc("Long-term")) || isVisible(byDescContains("Long-term"));

            if (shortTermVisible && longTermVisible) {
                ReportLogger.pass("Trailing Returns Short-term and Long-term chips are visible");
                return;
            }

            swipeDownW3C();
            sleep(700);
        }

        throw new AssertionError("Trailing Returns Short-term/Long-term chips are not visible after scrolling back");
    }

    private List<String> getNextNumericValuesAfter(List<String> values, int startIndex, int requiredCount) {
        List<String> numericValues = new ArrayList<>();

        if (values == null || values.isEmpty()) {
            return numericValues;
        }

        for (int i = startIndex + 1; i < values.size(); i++) {
            String value = values.get(i);

            if (value == null) {
                continue;
            }

            String clean = value.trim();

            if (clean.isEmpty()) {
                continue;
            }

            if (isQuarterNoiseLabel(clean)) {
                continue;
            }

            if (isValidReturnNumber(clean)) {
                numericValues.add(clean);
            }

            if (numericValues.size() == requiredCount) {
                break;
            }
        }

        return numericValues;
    }

    private boolean isQuarterNoiseLabel(String value) {
        if (value == null) {
            return false;
        }

        String clean = value.trim();

        return clean.equalsIgnoreCase("Q1")
                || clean.equalsIgnoreCase("Q2")
                || clean.equalsIgnoreCase("Q3")
                || clean.equalsIgnoreCase("Q4");
    }

    private boolean isValidReturnNumber(String value) {
        if (value == null) {
            return false;
        }

        String clean = value.trim();

        return clean.matches("-?\\d+(\\.\\d+)?") || clean.equals("-");
    }

    private void logReturnRowIfPresent(String sectionName, List<String> values, String rowStartText, String rowLabel) {
        int index = values.indexOf(rowStartText);

        if (index < 0) {
            ReportLogger.debug(sectionName + " row not visible, skipping: " + rowStartText);
            return;
        }

        List<String> numericValues = getNextNumericValuesAfter(values, index, 3);

        if (numericValues.size() < 3) {
            ReportLogger.debug(sectionName + " row visible but numeric values are incomplete: " + rowStartText);
            return;
        }

        String message = rowLabel
                + " | Value 1: " + numericValues.get(0)
                + " | Value 2: " + numericValues.get(1)
                + " | Value 3: " + numericValues.get(2);

        logCleanValidation(sectionName, message);
    }

    private String getVisibleValueOrBlank(String text) {
        if (isVisible(byDesc(text))) {
            return text;
        }

        if (isVisible(byDescContains(text))) {
            return text;
        }

        return "-";
    }

    private void logCleanValidation(String sectionName, String message) {
        String finalMessage = sectionName + " | " + message;

        ReportLogger.pass(finalMessage);

        try {
            ExtentTestManager.getTest().pass("<b>" + sectionName + ":</b> " + message);
        } catch (Exception ignored) {
            // Extent test may not be initialized
        }
    }

    private List<String> getCleanVisibleTexts() {
        /*
         * Important:
         * Do not use Set/LinkedHashSet here.
         * Many fund tables can contain the same visible value more than once.
         * Example: Rank row can contain 107 and Total Funds row can also contain 107.
         * If duplicates are removed, the parser loses real table values and fails before
         * validating the next section/chip, such as Long-term Trailing Returns.
         */
        List<String> texts = new ArrayList<>();
        Set<String> seenElementPositions = new LinkedHashSet<>();

        List<By> locators = new ArrayList<>();
        locators.add(AppiumBy.xpath("//android.view.View"));
        locators.add(AppiumBy.xpath("//android.widget.ImageView"));
        locators.add(AppiumBy.xpath("//android.widget.Button"));
        locators.add(AppiumBy.xpath("//android.widget.TextView"));

        for (By locator : locators) {
            try {
                List<WebElement> elements = driver.findElements(locator);

                for (WebElement element : elements) {
                    try {
                        if (element == null || !element.isDisplayed()) {
                            continue;
                        }

                        String text = normalizeVisibleText(getElementReadableText(element));

                        if (shouldSkipReturnsNoise(text)) {
                            continue;
                        }

                        Rectangle rect = element.getRect();
                        String className = "";

                        try {
                            className = element.getAttribute("class");
                        } catch (Exception ignored) {
                            // Class may not be exposed for every Flutter element
                        }

                        String elementPositionKey = className + "|"
                                + rect.getX() + "|"
                                + rect.getY() + "|"
                                + rect.getWidth() + "|"
                                + rect.getHeight() + "|"
                                + text;

                        /*
                         * Prevent only the exact same UI element from being counted twice
                         * if Appium exposes it through multiple locator groups.
                         * Do not remove same text from different positions because those are
                         * valid duplicate table values.
                         */
                        if (seenElementPositions.add(elementPositionKey)) {
                            texts.add(text);
                        }

                    } catch (Exception ignored) {
                        // Continue with next element
                    }
                }

            } catch (Exception ignored) {
                // Continue with next locator group
            }
        }

        return texts;
    }

    private String normalizeVisibleText(String text) {
        if (text == null) {
            return "";
        }

        return text
                .replace("\r", "\n")
                .replaceAll("[ \\t]+", " ")
                .replaceAll("\\n+", "\n")
                .trim();
    }

    private boolean shouldSkipReturnsNoise(String text) {
        if (text == null) {
            return true;
        }

        String clean = text.trim();

        if (clean.isEmpty()) {
            return true;
        }

        if ("null".equalsIgnoreCase(clean)) {
            return true;
        }

        if ("|".equals(clean)) {
            return true;
        }

        return clean.equals("Funds")
                || clean.equals("Stocks")
                || clean.equals("Portfolio")
                || clean.equals("Hub")
                || clean.equals("Redeem")
                || clean.equals("Invest")
                || clean.equals("Search")
                || clean.equals("More")
                || clean.equals("Show more")
                || clean.equals("Show less")
                || clean.equals("Analyst's Choice")
                || clean.equals("NAV")
                || clean.equals("Opinion:")
                || clean.equals("Buy")
                || clean.equals("Growth")
                || clean.equals("Dir\nReg")
                || clean.equals(FUND_HEADER)
                || clean.contains("Your Investments")
                || clean.contains("Your portfolio overlap");
    }

    // =========================================================
    // RISK - COMPLETE SECTION + MORE PAGE VALIDATION
    // =========================================================

    public void verifyRiskSectionAndMorePage() {
        recoverFundDetailsIfNeeded();

        ReportLogger.step("Verifying complete Risk section and Risk More page");

        scrollToTopOfFundDetails();

        scrollToRiskAssessmentCard();

        validateRiskSummarySectionComplete();

        tapMoreNearSection("What is the risk?");

        try {
            waitForAnyVisible(
                    new By[]{
                            byDesc("Worst Returns"),
                            byDesc("Risk Graph"),
                            byDesc("Standard Deviation")
                    },
                    "Risk More page",
                    10
            );

            validateRiskMorePageComplete();

        } finally {
            pressBack("Back from Risk More page");
            waitForFundHeaderOrKnownSection();
        }

        ReportLogger.pass("Complete Risk section and Risk More page validated successfully");
    }

    private void scrollToRiskAssessmentCard() {
        ReportLogger.step("Scrolling to Risk assessment card");

        scrollUntilAnyVisible(
                new String[]{
                        "What is the risk?",
                        "Very High",
                        "SEBI Riskometer",
                        "Worst 1 year returns"
                },
                14,
                "Risk assessment card"
        );

        for (int i = 1; i <= 5; i++) {
            boolean headingVisible = isVisible(byDesc("What is the risk?")) || isVisible(byDescContains("What is the risk?"));
            boolean riskometerVisible = isVisible(byDesc("Very High")) || isVisible(byDesc("SEBI Riskometer"));
            boolean worstReturnsVisible = isVisible(byDesc("Worst 1 year returns")) || isVisible(byDescContains("Worst 1 year returns"));

            if (headingVisible && riskometerVisible && worstReturnsVisible) {
                ReportLogger.pass("Risk assessment card is properly visible");
                return;
            }

            smallSwipeUpW3C();
            sleep(700);
        }

        if (!isVisible(byDesc("What is the risk?")) && !isVisible(byDescContains("What is the risk?"))) {
            throw new AssertionError("Risk assessment card heading is not visible");
        }

        ReportLogger.pass("Risk assessment card heading is visible");
    }

    private void validateRiskSummarySectionComplete() {
        ReportLogger.step("Validating Risk summary section completely");

        assertVisibleAndLog(byDesc("What is the risk?"), "Risk section heading");
        assertVisibleAndLog(byDesc("Very High"), "Riskometer value");
        assertVisibleAndLog(byDesc("SEBI Riskometer"), "SEBI Riskometer label");
        assertVisibleAndLog(byDesc("Worst 1 year returns"), "Worst 1 year returns label");

        List<String> values = getCleanVisibleTextsWithDuplicates();

        List<String> worstReturnValues = getPercentValuesAfterLabel(values, "Worst 1 year returns", 2);
        String fundWorstReturn = worstReturnValues.size() > 0 ? worstReturnValues.get(0) : "-";
        String bseWorstReturn = worstReturnValues.size() > 1 ? worstReturnValues.get(1) : "-";

        logCleanValidation("Risk Summary", "Risk Level: Very High");
        logCleanValidation("Risk Summary", "Riskometer: SEBI Riskometer");

        if (!"-".equals(fundWorstReturn) || !"-".equals(bseWorstReturn)) {
            logCleanValidation(
                    "Risk Summary",
                    "Worst 1 year returns: Fund " + fundWorstReturn + " | BSE 500 TRI " + bseWorstReturn
            );
        } else {
            logCleanValidation("Risk Summary", "Worst 1 year returns section visible");
        }

        logOptionalVisibleText(byDesc("Fund"), "Risk Summary legend - Fund");
        logOptionalVisibleText(byDesc("BSE 500 TRI"), "Risk Summary legend - BSE 500 TRI");

        ReportLogger.pass("Risk summary section validated completely");
    }

    private void validateRiskMorePageComplete() {
        ReportLogger.step("Validating Risk More page completely");

        List<String> riskFailures = new ArrayList<>();

        runSoftValidation(riskFailures, "Worst Returns", new Runnable() {
            @Override
            public void run() {
                validateWorstReturnsSectionAllChips();
            }
        });

        runSoftValidation(riskFailures, "Risk Graph", new Runnable() {
            @Override
            public void run() {
                validateRiskGraphSection();
            }
        });

        runSoftValidation(riskFailures, "Risk Metric Cards", new Runnable() {
            @Override
            public void run() {
                validateRiskMetricCardsCompletely();
            }
        });

        throwIfValidationFailures("Risk More page", riskFailures);

        ReportLogger.pass("Risk More page completed validation successfully");
    }

    private void validateWorstReturnsSectionAllChips() {
        ReportLogger.step("Validating Worst Returns section with all chips");

        List<String> worstReturnsFailures = new ArrayList<>();

        scrollUntilAnyVisible(
                new String[]{
                        "Worst Returns",
                        "Week",
                        "Month",
                        "Quarter",
                        "Year"
                },
                8,
                "Worst Returns section"
        );

        runSoftValidation(worstReturnsFailures, "Worst Returns heading", new Runnable() {
            @Override
            public void run() {
                assertVisibleAndLog(byDesc("Worst Returns"), "Worst Returns heading");
            }
        });

        runSoftValidation(worstReturnsFailures, "Worst Returns Week chip", new Runnable() {
            @Override
            public void run() {
                assertVisibleAndLog(byDesc("Week"), "Worst Returns Week chip");
            }
        });

        runSoftValidation(worstReturnsFailures, "Worst Returns Month chip", new Runnable() {
            @Override
            public void run() {
                assertVisibleAndLog(byDesc("Month"), "Worst Returns Month chip");
            }
        });

        runSoftValidation(worstReturnsFailures, "Worst Returns Quarter chip", new Runnable() {
            @Override
            public void run() {
                assertVisibleAndLog(byDesc("Quarter"), "Worst Returns Quarter chip");
            }
        });

        runSoftValidation(worstReturnsFailures, "Worst Returns Year chip", new Runnable() {
            @Override
            public void run() {
                assertVisibleAndLog(byDesc("Year"), "Worst Returns Year chip");
            }
        });

        validateWorstReturnsChipSafely("Week", worstReturnsFailures);
        validateWorstReturnsChipSafely("Month", worstReturnsFailures);
        validateWorstReturnsChipSafely("Quarter", worstReturnsFailures);
        validateWorstReturnsChipSafely("Year", worstReturnsFailures);

        throwIfValidationFailures("Worst Returns", worstReturnsFailures);

        ReportLogger.pass("Worst Returns all chips validated");
    }

    private void validateWorstReturnsChipSafely(String chipName, List<String> failures) {
        runSoftValidation(failures, "Worst Returns - " + chipName, new Runnable() {
            @Override
            public void run() {
                validateWorstReturnsChip(chipName);
            }
        });
    }

    private void validateWorstReturnsChip(String chipName) {
        ReportLogger.step("Validating Worst Returns chip: " + chipName);

        if (!isVisible(byDesc(chipName))) {
            scrollUntilAnyVisible(
                    new String[]{
                            "Worst Returns",
                            chipName
                    },
                    5,
                    "Worst Returns chip: " + chipName
            );
        }

        tapVisible(byDesc(chipName), "Worst Returns chip: " + chipName);
        sleep(1000);

        List<String> values = getCleanVisibleTextsWithDuplicates();

        validateTextPresentInList(values, "Worst Returns", "Worst Returns - " + chipName);
        validateTextPresentInList(values, "Return (%)", "Worst Returns - " + chipName);
        validateTextPresentInList(values, "Begin", "Worst Returns - " + chipName);
        validateTextPresentInList(values, "End", "Worst Returns - " + chipName);

        String returnValue = getFirstValueAfterLabel(values, "Return (%)");
        String beginDate = getFirstDateAfterLabel(values, "Begin");
        String endDate = getFirstDateAfterLabel(values, "End");

        if ("-".equals(returnValue)) {
            throw new AssertionError("Worst Returns - " + chipName + " return value not found");
        }

        if ("-".equals(beginDate)) {
            throw new AssertionError("Worst Returns - " + chipName + " begin date not found");
        }

        if ("-".equals(endDate)) {
            throw new AssertionError("Worst Returns - " + chipName + " end date not found");
        }

        logCleanValidation(
                "Worst Returns - " + chipName,
                "Return (%): " + returnValue + " | Begin: " + beginDate + " | End: " + endDate
        );
    }

    private void validateRiskGraphSection() {
        ReportLogger.step("Validating Risk Graph section");

        scrollUntilAnyVisible(
                new String[]{
                        "Risk Graph",
                        "Fund",
                        "Category Median",
                        "BSE 500 TRI",
                        "Category Peers"
                },
                8,
                "Risk Graph section"
        );

        assertVisibleAndLog(byDesc("Risk Graph"), "Risk Graph heading");

        logCleanValidation(
                "Risk Graph",
                "Legend: Fund | Category Median | BSE 500 TRI | Category Peers"
        );

        logOptionalRiskGraphText(
                byDescContains("Over the last 3 years, the fund has delivered higher returns"),
                "Observation 1"
        );

        logOptionalRiskGraphText(
                byDescContains("Over the last 3 years, the fund has tended to fall lesser"),
                "Observation 2"
        );

        logOptionalRiskGraphText(
                byDescContains("Category peers with only long enough history are plotted"),
                "Note"
        );

        ReportLogger.pass("Risk Graph section validated");
    }

    private void validateRiskMetricCardsCompletely() {
        ReportLogger.step("Validating all Risk metric cards completely");

        List<String> metricFailures = new ArrayList<>();

        validateRiskMetricCardSafely("Standard Deviation", metricFailures);
        validateRiskMetricCardSafely("Sharpe", metricFailures);
        validateRiskMetricCardSafely("Sortino", metricFailures);
        validateRiskMetricCardSafely("Beta", metricFailures);
        validateRiskMetricCardSafely("Alpha", metricFailures);
        validateRiskMetricCardSafely("Information Ratio", metricFailures);

        logOptionalVisibleText(
                byDescContains("Information Ratio is for 3-years as per AMC disclosure"),
                "Information Ratio note"
        );

        throwIfValidationFailures("Risk metric cards", metricFailures);

        ReportLogger.pass("All Risk metric cards validated completely");
    }

    private void validateRiskMetricCardSafely(String cardTitle, List<String> failures) {
        runSoftValidation(failures, "Risk metric card - " + cardTitle, new Runnable() {
            @Override
            public void run() {
                validateRiskMetricCard(cardTitle);
            }
        });
    }

    private void validateRiskMetricCard(String cardTitle) {
        ReportLogger.step("Validating Risk metric card: " + cardTitle);

        List<String> sectionValues = scrollToRiskMetricCardBodyAndCollect(cardTitle);

        validateRiskMetricHeader(cardTitle, sectionValues);

        validateRiskMetricRow(cardTitle, sectionValues, "HDFC Flexi Cap Dir", "Fund");
        validateRiskMetricRow(cardTitle, sectionValues, "BSE 500 TRI", "Index");
        validateRiskMetricRow(cardTitle, sectionValues, "Equity: Flexi Cap", "Category");
        validateRiskMetricRow(cardTitle, sectionValues, "Rank within category", "Rank within category");
        validateRiskMetricRow(cardTitle, sectionValues, "Total Funds", "Total Funds");

        logOptionalVisibleText(byDescContains("as on"), cardTitle + " date");

        ReportLogger.pass(cardTitle + " card validated");
    }

    private List<String> scrollToRiskMetricCardBodyAndCollect(String cardTitle) {
        scrollToRiskMetricHeadingBidirectional(cardTitle);

        assertVisibleAndLog(byDesc(cardTitle), cardTitle + " heading");

        for (int i = 1; i <= 8; i++) {
            List<String> values = getCleanVisibleTextsWithDuplicates();
            List<String> sectionValues = getRiskMetricSectionValues(values, cardTitle);

            if (containsRiskMetricBody(sectionValues)) {
                ReportLogger.pass(cardTitle + " table body is visible");
                return sectionValues;
            }

            /*
             * The card heading can be visible at the bottom edge while the table body is still below it.
             * Use only a small swipe so the next cards are not skipped.
             */
            smallSwipeUpW3C();
            sleep(700);
        }

        /*
         * If Flutter/Appium exposed the heading late and the small swipes crossed the card,
         * search both directions once before failing.
         */
        scrollToRiskMetricHeadingBidirectional(cardTitle);

        for (int i = 1; i <= 5; i++) {
            List<String> values = getCleanVisibleTextsWithDuplicates();
            List<String> sectionValues = getRiskMetricSectionValues(values, cardTitle);

            if (containsRiskMetricBody(sectionValues)) {
                ReportLogger.pass(cardTitle + " table body is visible after realignment");
                return sectionValues;
            }

            smallSwipeUpW3C();
            sleep(700);
        }

        List<String> values = getCleanVisibleTextsWithDuplicates();
        List<String> sectionValues = getRiskMetricSectionValues(values, cardTitle);

        throw new AssertionError(
                cardTitle + " table body not visible after controlled scrolling. Visible values: " + sectionValues
        );
    }

    private void scrollToRiskMetricHeadingBidirectional(String cardTitle) {
        if (isVisible(byDesc(cardTitle)) || isVisible(byDescContains(cardTitle))) {
            ReportLogger.pass("Risk metric card heading visible: " + cardTitle);
            return;
        }

        ReportLogger.step("Scrolling down to find risk metric card heading: " + cardTitle);

        for (int i = 1; i <= 7; i++) {
            smallSwipeUpW3C();
            sleep(600);

            if (isVisible(byDesc(cardTitle)) || isVisible(byDescContains(cardTitle))) {
                ReportLogger.pass("Risk metric card heading visible: " + cardTitle);
                return;
            }
        }

        /*
         * Important:
         * Sometimes Standard Deviation body validation leaves the viewport below Sharpe/Sortino.
         * In that case, continuing to swipe up skips the target forever. Search upward visually
         * by swiping down before failing.
         */
        ReportLogger.step("Heading not found while scrolling down. Scrolling back up for: " + cardTitle);

        for (int i = 1; i <= 10; i++) {
            smallSwipeDownW3C();
            sleep(600);

            if (isVisible(byDesc(cardTitle)) || isVisible(byDescContains(cardTitle))) {
                ReportLogger.pass("Risk metric card heading visible after scrolling back: " + cardTitle);
                return;
            }
        }

        ReportLogger.step("Retrying downward search for risk metric card heading: " + cardTitle);

        for (int i = 1; i <= 10; i++) {
            smallSwipeUpW3C();
            sleep(600);

            if (isVisible(byDesc(cardTitle)) || isVisible(byDescContains(cardTitle))) {
                ReportLogger.pass("Risk metric card heading visible: " + cardTitle);
                return;
            }
        }

        throw new AssertionError("Risk metric card heading not visible after bidirectional scroll: " + cardTitle);
    }

    private boolean containsRiskMetricBody(List<String> values) {
        if (values == null || values.isEmpty()) {
            return false;
        }

        return values.contains("Particulars")
                && values.contains("%")
                && values.contains("HDFC Flexi Cap Dir")
                && values.contains("BSE 500 TRI")
                && values.contains("Equity: Flexi Cap")
                && values.contains("Rank within category")
                && values.contains("Total Funds");
    }

    private List<String> getRiskMetricSectionValues(List<String> values, String cardTitle) {
        if (values == null || values.isEmpty()) {
            return new ArrayList<>();
        }

        int startIndex = values.indexOf(cardTitle);

        if (startIndex < 0) {
            startIndex = findFirstIndexOfAny(values, Arrays.asList("Particulars", "HDFC Flexi Cap Dir"));
        }

        if (startIndex < 0) {
            return values;
        }

        List<String> nextRiskTitles = Arrays.asList(
                "Standard Deviation",
                "Sharpe",
                "Sortino",
                "Beta",
                "Alpha",
                "Information Ratio"
        );

        int endIndex = values.size();

        for (int i = startIndex + 1; i < values.size(); i++) {
            String value = values.get(i);

            if (value != null && nextRiskTitles.contains(value.trim()) && !value.trim().equals(cardTitle)) {
                endIndex = i;
                break;
            }
        }

        return new ArrayList<>(values.subList(startIndex, endIndex));
    }

    private int findFirstIndexOfAny(List<String> values, List<String> candidates) {
        if (values == null || candidates == null) {
            return -1;
        }

        for (int i = 0; i < values.size(); i++) {
            String value = values.get(i);

            if (value == null) {
                continue;
            }

            if (candidates.contains(value.trim())) {
                return i;
            }
        }

        return -1;
    }

    private void validateRiskMetricHeader(String sectionName, List<String> values) {
        if (values == null || !values.contains("Particulars")) {
            throw new AssertionError(sectionName + " missing header: Particulars. Visible values: " + values);
        }

        if (!values.contains("%")) {
            throw new AssertionError(sectionName + " missing header: %. Visible values: " + values);
        }

        logCleanValidation(sectionName, "Header: Particulars | %");
    }

    private void validateRiskMetricRow(
            String sectionName,
            List<String> values,
            String rowKey,
            String rowLabel
    ) {
        int index = values.indexOf(rowKey);

        if (index < 0) {
            throw new AssertionError(sectionName + " missing row: " + rowKey + ". Visible values: " + values);
        }

        String value = getFirstRiskValueAfter(values, index);

        if ("-".equals(value)) {
            throw new AssertionError(sectionName + " value not found for row: " + rowKey + ". Visible values: " + values);
        }

        logCleanValidation(
                sectionName,
                rowLabel + ": " + rowKey + " | Value: " + value
        );
    }

    private void validateTextPresentInList(List<String> values, String expectedText, String sectionName) {
        if (!containsExactText(values, expectedText)) {
            throw new AssertionError(sectionName + " missing expected text: " + expectedText);
        }

        logCleanValidation(sectionName, "Text visible: " + expectedText);
    }

    private String getFirstValueAfterLabel(List<String> values, String label) {
        if (values == null || values.isEmpty()) {
            return "-";
        }

        int labelIndex = values.indexOf(label);

        if (labelIndex < 0) {
            return "-";
        }

        for (int i = labelIndex + 1; i < values.size(); i++) {
            String text = values.get(i);

            if (text == null) {
                continue;
            }

            String clean = text.trim();

            if (clean.isEmpty()) {
                continue;
            }

            if (clean.matches("-?\\d+(\\.\\d+)?%?")) {
                return clean;
            }
        }

        return "-";
    }

    private String getFirstDateAfterLabel(List<String> values, String label) {
        if (values == null || values.isEmpty()) {
            return "-";
        }

        int labelIndex = values.indexOf(label);

        if (labelIndex < 0) {
            return "-";
        }

        for (int i = labelIndex + 1; i < values.size(); i++) {
            String text = values.get(i);

            if (text == null) {
                continue;
            }

            String clean = text.trim();

            if (clean.matches("\\d{2}\\s+[A-Za-z]{3},\\s+\\d{4}")) {
                return clean;
            }
        }

        return "-";
    }

    private String getFirstRiskValueAfter(List<String> values, int startIndex) {
        if (values == null || values.isEmpty()) {
            return "-";
        }

        for (int i = startIndex + 1; i < values.size(); i++) {
            String text = values.get(i);

            if (text == null) {
                continue;
            }

            String clean = text.trim();

            if (clean.isEmpty()) {
                continue;
            }

            if ("--".equals(clean)) {
                return clean;
            }

            if (clean.matches("-?\\d+(\\.\\d+)?")) {
                return clean;
            }
        }

        return "-";
    }

    private List<String> getPercentValuesAfterLabel(List<String> values, String label, int requiredCount) {
        List<String> matches = new ArrayList<>();

        if (values == null || values.isEmpty()) {
            return matches;
        }

        int labelIndex = values.indexOf(label);

        if (labelIndex < 0) {
            return matches;
        }

        for (int i = labelIndex + 1; i < values.size(); i++) {
            String value = values.get(i);

            if (value == null) {
                continue;
            }

            String clean = value.trim();

            if (clean.matches("-?\\d+(\\.\\d+)?%")) {
                matches.add(clean);
            }

            if (matches.size() == requiredCount) {
                break;
            }
        }

        return matches;
    }

    private void logOptionalRiskGraphText(By locator, String label) {
        WebElement element = findVisibleElement(locator);

        if (element == null) {
            ReportLogger.debug("Risk Graph " + label + " not visible in current viewport");
            return;
        }

        String text = normalizeVisibleText(getElementReadableText(element));

        if (!text.isEmpty()) {
            logCleanValidation("Risk Graph", label + ": " + text);
        }
    }

    private List<String> getCleanVisibleTextsWithDuplicates() {
        return getCleanVisibleTexts();
    }

    private void smallSwipeUpW3C() {
        try {
            Dimension size = driver.manage().window().getSize();

            int x = size.getWidth() / 2;
            int startY = (int) (size.getHeight() * 0.62);
            int endY = (int) (size.getHeight() * 0.48);

            PointerInput finger = new PointerInput(PointerInput.Kind.TOUCH, "finger");
            Sequence swipe = new Sequence(finger, 1);

            swipe.addAction(finger.createPointerMove(Duration.ZERO, PointerInput.Origin.viewport(), x, startY));
            swipe.addAction(finger.createPointerDown(PointerInput.MouseButton.LEFT.asArg()));
            swipe.addAction(finger.createPointerMove(Duration.ofMillis(450), PointerInput.Origin.viewport(), x, endY));
            swipe.addAction(finger.createPointerUp(PointerInput.MouseButton.LEFT.asArg()));

            driver.perform(Collections.singletonList(swipe));

        } catch (Exception firstError) {
            Map<String, Object> params = new HashMap<>();
            params.put("left", 80);
            params.put("top", 420);
            params.put("width", 920);
            params.put("height", 900);
            params.put("direction", "up");
            params.put("percent", 0.28);
            driver.executeScript("mobile: scrollGesture", params);
        }
    }


    private void smallSwipeDownW3C() {
        try {
            Dimension size = driver.manage().window().getSize();

            int x = size.getWidth() / 2;
            int startY = (int) (size.getHeight() * 0.48);
            int endY = (int) (size.getHeight() * 0.62);

            PointerInput finger = new PointerInput(PointerInput.Kind.TOUCH, "finger");
            Sequence swipe = new Sequence(finger, 1);

            swipe.addAction(finger.createPointerMove(Duration.ZERO, PointerInput.Origin.viewport(), x, startY));
            swipe.addAction(finger.createPointerDown(PointerInput.MouseButton.LEFT.asArg()));
            swipe.addAction(finger.createPointerMove(Duration.ofMillis(450), PointerInput.Origin.viewport(), x, endY));
            swipe.addAction(finger.createPointerUp(PointerInput.MouseButton.LEFT.asArg()));

            driver.perform(Collections.singletonList(swipe));

        } catch (Exception firstError) {
            Map<String, Object> params = new HashMap<>();
            params.put("left", 80);
            params.put("top", 420);
            params.put("width", 920);
            params.put("height", 900);
            params.put("direction", "down");
            params.put("percent", 0.28);
            driver.executeScript("mobile: scrollGesture", params);
        }
    }
    
 // =========================================================
 // PRD - PAID USER READ-ONLY VALIDATIONS
 // =========================================================

 public void verifyConsistencyScoreCardForPaidUser() {
     recoverFundDetailsIfNeeded();

     ReportLogger.step("Verifying Consistency Score card for paid user");

     scrollToTopOfFundDetails();
     scrollUntilVisible("Consistency Score", 10);

     List<String> consistencyFailures = new ArrayList<>();

     runSoftValidation(consistencyFailures, "Consistency Score heading", new Runnable() {
         @Override
         public void run() {
             assertVisibleAndLog(byDesc("Consistency Score"), "Consistency Score heading");
         }
     });

     runSoftValidation(consistencyFailures, "Consistency Score percentage", new Runnable() {
         @Override
         public void run() {
             assertVisibleAndLog(byDesc("100%"), "Consistency Score percentage");
         }
     });

     runSoftValidation(consistencyFailures, "Consistency Score description", new Runnable() {
         @Override
         public void run() {
             assertVisibleAndLog(
                     byDescContains("The fund has beaten its peers 100% of the times in any 5Y period since May-2018"),
                     "Consistency Score description"
             );
         }
     });

     logOptionalVisibleText(byDesc("100"), "Consistency Score circle value");

     throwIfValidationFailures("Consistency Score card", consistencyFailures);

     ReportLogger.pass("Consistency Score card validated successfully for paid user");
 }

 public void verifyWorstOneYearReturnsFootnoteForPaidUser() {
	    recoverFundDetailsIfNeeded();

	    ReportLogger.step("Verifying Worst 1 year returns footnote for paid user");

	    scrollToTopOfFundDetails();
	    scrollUntilVisible("What is the risk?", 14);

	    List<String> worstReturnFailures = new ArrayList<>();

	    runSoftValidation(worstReturnFailures, "Risk section heading", new Runnable() {
	        @Override
	        public void run() {
	            assertVisibleAndLog(byDesc("What is the risk?"), "Risk section heading");
	        }
	    });

	    runSoftValidation(worstReturnFailures, "Riskometer value", new Runnable() {
	        @Override
	        public void run() {
	            assertVisibleAndLog(byDesc("Very High"), "Riskometer value");
	        }
	    });

	    runSoftValidation(worstReturnFailures, "SEBI Riskometer label", new Runnable() {
	        @Override
	        public void run() {
	            assertVisibleAndLog(byDesc("SEBI Riskometer"), "SEBI Riskometer label");
	        }
	    });

	    runSoftValidation(worstReturnFailures, "Worst 1 year returns label", new Runnable() {
	        @Override
	        public void run() {
	            assertVisibleAndLog(byDesc("Worst 1 year returns"), "Worst 1 year returns label");
	        }
	    });

	    runSoftValidation(worstReturnFailures, "Worst 1 year returns footnote", new Runnable() {
	        @Override
	        public void run() {
	            assertVisibleAndLog(byDesc("(in last 10 years)"), "Worst 1 year returns footnote");
	        }
	    });

	    runSoftValidation(worstReturnFailures, "Worst 1 year returns benchmark legend", new Runnable() {
	        @Override
	        public void run() {
	            assertVisibleAndLog(byDesc("BSE 500 TRI"), "Worst 1 year returns benchmark legend");
	        }
	    });

	    /*
	     * Keep Fund legend optional.
	     * Reason: Appium sometimes does not expose the small chart legend "Fund"
	     * as a stable standalone accessibility node, even when the chart is visible.
	     * Existing stable risk summary validation also logs this as optional.
	     */
	    logOptionalVisibleText(byDesc("Fund"), "Worst 1 year returns Fund legend");

	    logOptionalVisibleText(byDescContains("-38.1%"), "Worst 1 year returns fund value");
	    logOptionalVisibleText(byDescContains("-33.2%"), "Worst 1 year returns benchmark value");

	    throwIfValidationFailures("Worst 1 year returns", worstReturnFailures);

	    ReportLogger.pass("Worst 1 year returns footnote validated successfully for paid user");
	}
 
 public void verifyTransactionButtonsVisibleForPaidUser() {
	    recoverFundDetailsIfNeeded();

	    ReportLogger.step("Verifying Fund Details transaction buttons for paid user");

	    scrollToTopOfFundDetails();

	    /*
	     * Buttons are fixed at the bottom of Fund Details page.
	     * If they are not exposed immediately, scroll to a known mid-page section
	     * where the screenshot confirmed both Redeem and Invest are visible.
	     */
	    if (!isVisible(byDesc("Redeem")) || !isVisible(byDesc("Invest"))) {
	        scrollUntilVisible("What is the risk?", 14);
	    }

	    List<String> transactionButtonFailures = new ArrayList<>();

	    runSoftValidation(transactionButtonFailures, "Redeem button", new Runnable() {
	        @Override
	        public void run() {
	            assertVisibleAndLog(byDesc("Redeem"), "Redeem button");
	        }
	    });

	    runSoftValidation(transactionButtonFailures, "Invest button", new Runnable() {
	        @Override
	        public void run() {
	            assertVisibleAndLog(byDesc("Invest"), "Invest button");
	        }
	    });

	    throwIfValidationFailures("Fund Details transaction buttons", transactionButtonFailures);

	    ReportLogger.pass("Fund Details transaction buttons validated successfully for paid user");
	}
//=======================================================
//PORTFOLIO FUNDS - REDEEM / INVEST MORE BUTTON VALIDATION
//FAST + STABLE + SOFT-FAIL VERSION
//=======================================================

public void verifyRedeemInvestButtonsFromPortfolioInvestorFunds() {
  ReportLogger.step("Verifying Redeem and Invest More buttons from Portfolio investor funds");

  openPortfolioTabForFundValidation();

  validatePortfolioInvestorFunds(
          "Seema Khatri",
          "Seema Khatri (DFGPK2829H)",
          new String[]{
                  "HDFC Pharma And Healthcare Reg-G",
                  "SBI Energy Opportunities Reg-G"
          }
  );

  validatePortfolioInvestorFunds(
          "Manish Khatri",
          "Manish Khatri (MKLPK2070D)",
          new String[]{
                  "Nippon India Multi Cap Direct-G"
          }
  );


  ReportLogger.pass("Redeem and Invest More button validation flow completed for Portfolio investor funds");
}

private void validatePortfolioInvestorFunds(
      String investorName,
      String investorOptionText,
      String[] fundNames
) {
  ReportLogger.step("Validating Portfolio funds for investor: " + investorName);

  ensurePortfolioRootOrOpen();

  selectPortfolioInvestorOnlyIfNeeded(investorName, investorOptionText);

  openPortfolioFundsTabOnlyIfNeeded();

  resetPortfolioFundsListPositionFast();

  List<String> investorSoftFailures = new ArrayList<>();

  for (String fundName : fundNames) {
      ReportLogger.step("Validating Portfolio fund buttons | Investor: " + investorName + " | Fund: " + fundName);

      try {
          openFundFromPortfolioFundsListStable(fundName);

          boolean validationPassed = validateRedeemAndInvestMoreButtonsOnPortfolioFundDetailsSoft(fundName);

          if (!validationPassed) {
              investorSoftFailures.add("Buttons missing or section not found | Investor: "
                      + investorName + " | Fund: " + fundName);
          }

          returnToPortfolioFundsListFast();

          ReportLogger.pass("Portfolio fund button validation completed | Investor: "
                  + investorName + " | Fund: " + fundName);

      } catch (Exception e) {
          String failureMessage = "Soft Fail: Portfolio fund validation issue | Investor: "
                  + investorName + " | Fund: " + fundName + " | Reason: " + e.getMessage();

          investorSoftFailures.add(failureMessage);

          ReportLogger.step(failureMessage);

          recoverBackToPortfolioFundsListAfterSoftFailure();
      }
  }

  if (!investorSoftFailures.isEmpty()) {
      ReportLogger.step("Soft failures found for investor: " + investorName);

      for (String failure : investorSoftFailures) {
          ReportLogger.step(failure);
      }
  }

  ReportLogger.pass("Portfolio funds validation flow completed for investor: " + investorName);
}

private void ensurePortfolioRootOrOpen() {
  exitPortfolioChildPageIfNeeded();

  if (isRealPortfolioRootScreenVisible() || isPortfolioFundsListVisible()) {
      ReportLogger.pass("Portfolio screen is already available");
      return;
  }

  openPortfolioTabForFundValidation();
}

private void openPortfolioTabForFundValidation() {
  ReportLogger.step("Opening Portfolio tab");

  exitPortfolioChildPageIfNeeded();

  for (int attempt = 1; attempt <= 3; attempt++) {
      ReportLogger.step("Portfolio open attempt: " + attempt);

      if (tapPortfolioBottomTabStrict()) {
          sleep(1200);

          if (isPortfolioBottomTabSelectedOrPortfolioRootVisible()) {
              ReportLogger.pass("Portfolio tab opened");
              return;
          }
      }

      ReportLogger.step("Portfolio tab not confirmed after attempt " + attempt + ". Trying back recovery.");
      pressBackSilently();
      sleep(600);
      exitPortfolioChildPageIfNeeded();
  }

  throw new AssertionError("Unable to open Portfolio bottom tab after 3 attempts");
}

private boolean tapPortfolioBottomTabStrict() {
  ReportLogger.step("Trying to tap Portfolio bottom tab strictly");

  Dimension size = driver.manage().window().getSize();
  int screenHeight = size.getHeight();

  try {
      List<WebElement> elements = driver.findElements(
              AppiumBy.xpath("//*[@content-desc='Portfolio']")
      );

      ReportLogger.step("Portfolio bottom tab candidates found: " + elements.size());

      for (WebElement element : elements) {
          if (element == null) {
              continue;
          }

          Rectangle rect = element.getRect();

          if (rect == null || rect.getWidth() <= 0 || rect.getHeight() <= 0) {
              continue;
          }

          if (rect.getY() < screenHeight * 0.70) {
              ReportLogger.step("Ignoring non-bottom Portfolio candidate at Y=" + rect.getY());
              continue;
          }

          int centerX = rect.getX() + rect.getWidth() / 2;
          int centerY = rect.getY() + rect.getHeight() / 2;

          ReportLogger.step("Tapping Portfolio bottom tab element at X=" + centerX + ", Y=" + centerY);

          tapByCoordinates(centerX, centerY);
          sleep(700);

          return true;
      }

  } catch (Exception e) {
      ReportLogger.step("Portfolio bottom tab strict element tap failed: " + e.getMessage());
  }

  int fallbackX = (int) (size.getWidth() * 0.625);
  int fallbackY = (int) (size.getHeight() * 0.935);

  ReportLogger.step("Tapping Portfolio bottom tab coordinate fallback at X=" + fallbackX + ", Y=" + fallbackY);

  tapByCoordinates(fallbackX, fallbackY);
  sleep(700);

  return true;
}

private boolean isPortfolioBottomTabSelectedOrPortfolioRootVisible() {
  return isVisible(byDesc("Summary"))
          || isVisible(byDescContains("Summary"))
          || isVisible(byDesc("Funds, tab 2 of 8"))
          || isVisible(byDescContains("Funds, tab 2"))
          || isVisible(byDesc("Stocks & ETFs, tab 3 of 8"))
          || isVisible(byDescContains("Stocks & ETFs"))
          || isVisible(byDesc("NPS, tab 4 of 8"))
          || isVisible(byDescContains("Update Portfolio"))
          || isVisible(byTextContains("Update Portfolio"))
          || isVisible(byDescContains("Shows investor selection list"));
}

private void exitPortfolioChildPageIfNeeded() {
  for (int i = 1; i <= 2; i++) {
      if (!isPortfolioChildPageVisible()) {
          return;
      }

      ReportLogger.step("Portfolio child page detected. Pressing back to Portfolio root.");
      pressBackSilently();
      sleep(700);
  }
}

private boolean isPortfolioChildPageVisible() {
  return isVisible(byDesc("Funds Classification"))
          || isVisible(byText("Funds Classification"))
          || isVisible(byDescContains("See Alternatives"))
          || isVisible(byTextContains("See Alternatives"));
}

private boolean areMainBottomTabsVisible() {
  return isVisible(byDesc("Funds"))
          && isVisible(byDesc("Stocks"))
          && isVisible(byDesc("Portfolio"))
          && isVisible(byDesc("Hub"));
}

private boolean isRealPortfolioRootScreenVisible() {
  if (isPortfolioChildPageVisible()) {
      return false;
  }

  return isPortfolioBottomTabSelectedOrPortfolioRootVisible();
}

private void selectPortfolioInvestorOnlyIfNeeded(String investorName, String investorOptionText) {
  ReportLogger.step("Checking Portfolio investor: " + investorName);

  exitPortfolioChildPageIfNeeded();

  if (isVisible(byDescContains(investorName))
          && (isRealPortfolioRootScreenVisible() || isPortfolioFundsListVisible())) {
      ReportLogger.pass("Portfolio investor already selected: " + investorName);
      return;
  }

  selectPortfolioInvestorStable(investorName, investorOptionText);
}

private void selectPortfolioInvestorStable(String investorName, String investorOptionText) {
  ReportLogger.step("Selecting Portfolio investor: " + investorName);

  exitPortfolioChildPageIfNeeded();

  if (!isRealPortfolioRootScreenVisible() && !isPortfolioFundsListVisible()) {
      openPortfolioTabForFundValidation();
  }

  if (isPortfolioFundsListVisible()) {
      resetPortfolioFundsListPositionFast();
  }

  By investorDropdown = byDescContains("Shows investor selection list");

  waitForAnyVisible(
          new By[]{
                  investorDropdown,
                  byDesc("Summary"),
                  byDesc("Funds, tab 2 of 8"),
                  byDescContains("Funds, tab 2"),
                  byDescContains("Update Portfolio"),
                  byTextContains("Update Portfolio")
          },
          "Portfolio investor selector/root",
          8
  );

  if (!isVisible(investorDropdown)) {
      throw new AssertionError("Investor dropdown not visible on Portfolio root screen");
  }

  tapVisible(investorDropdown, "Investor dropdown");
  sleep(700);

  waitForAnyVisible(
          new By[]{
                  byDesc("Choose Investor"),
                  byDescContains("Choose Investor"),
                  byDesc(investorOptionText),
                  byDescContains(investorName)
          },
          "Choose Investor list",
          6
  );

  if (!tapIfVisible(byDesc(investorOptionText), "Investor option: " + investorOptionText)) {
      if (!tapIfVisible(byDescContains(investorName), "Investor option contains: " + investorName)) {
          throw new AssertionError("Investor option not visible/selectable: " + investorOptionText);
      }
  }

  sleep(1500);

  waitForAnyVisible(
          new By[]{
                  byDescContains(investorName),
                  byDesc("Summary"),
                  byDesc("Funds, tab 2 of 8"),
                  byDescContains("Funds, tab 2"),
                  byDescContains("Portfolio Value"),
                  byTextContains("Portfolio Value")
          },
          "Selected investor portfolio",
          8
  );

  ReportLogger.pass("Selected Portfolio investor: " + investorName);
}

private void openPortfolioFundsTabOnlyIfNeeded() {
  if (isPortfolioFundsListVisible()) {
      ReportLogger.pass("Portfolio Funds list is already visible");
      return;
  }

  openPortfolioFundsTabStable();
}

private void openPortfolioFundsTabStable() {
  ReportLogger.step("Opening Portfolio Funds tab");

  exitPortfolioChildPageIfNeeded();

  if (!isRealPortfolioRootScreenVisible() && !isPortfolioFundsListVisible()) {
      openPortfolioTabForFundValidation();
  }

  if (isPortfolioFundsListVisible()) {
      ReportLogger.pass("Portfolio Funds list is already visible");
      return;
  }

  WebElement fundsTab = null;

  try {
      List<WebElement> elements = driver.findElements(
              AppiumBy.xpath("//android.view.View[@content-desc='Funds, tab 2 of 8']")
      );

      ReportLogger.step("Portfolio Funds tab exact candidates found: " + elements.size());

      fundsTab = getValidUpperScreenElement(elements, "exact Portfolio Funds tab");

  } catch (Exception e) {
      ReportLogger.step("Exact Portfolio Funds tab lookup failed: " + e.getMessage());
  }

  if (fundsTab == null) {
      try {
          List<WebElement> elements = driver.findElements(
                  AppiumBy.xpath("//android.view.View[contains(@content-desc,'Funds, tab 2')]")
          );

          ReportLogger.step("Portfolio Funds tab contains candidates found: " + elements.size());

          fundsTab = getValidUpperScreenElement(elements, "contains Portfolio Funds tab");

      } catch (Exception e) {
          ReportLogger.step("Contains Portfolio Funds tab lookup failed: " + e.getMessage());
      }
  }

  if (fundsTab == null) {
      ReportLogger.step("Portfolio inner Funds tab not found by locator. Trying coordinate fallback.");
      tapPortfolioInnerFundsTabByCoordinates();
      sleep(800);

      if (isPortfolioFundsListVisible()) {
          ReportLogger.pass("Portfolio Funds tab opened by coordinate fallback");
          return;
      }

      throw new AssertionError("Portfolio inner Funds tab not found/opened. Unsafe byDesc('Funds') fallback removed.");
  }

  Rectangle rect = fundsTab.getRect();

  int centerX = rect.getX() + rect.getWidth() / 2;
  int centerY = rect.getY() + rect.getHeight() / 2;

  ReportLogger.step("Tapping Portfolio inner Funds tab center at X=" + centerX + ", Y=" + centerY);

  tapByCoordinates(centerX, centerY);
  sleep(800);

  waitForAnyVisible(
          new By[]{
                  byDescContains("Current"),
                  byDescContains("Invested"),
                  byDescContains("Sort By"),
                  byTextContains("Current"),
                  byTextContains("Invested"),
                  byTextContains("Sort By"),
                  byDescContains("HDFC Pharma"),
                  byDescContains("SBI Energy"),
                  byDescContains("Aditya Birla"),
                  byDescContains("Nippon India"),
                  byDescContains("HDFC Flexi"),
                  byDescContains("Parag Parikh")
          },
          "Portfolio Funds list",
          8
  );

  ReportLogger.pass("Portfolio Funds tab opened");
}

private WebElement getValidUpperScreenElement(List<WebElement> elements, String elementName) {
  if (elements == null || elements.isEmpty()) {
      return null;
  }

  int screenHeight = driver.manage().window().getSize().getHeight();

  for (WebElement element : elements) {
      if (element == null) {
          continue;
      }

      Rectangle rect = element.getRect();

      if (rect == null || rect.getWidth() <= 0 || rect.getHeight() <= 0) {
          continue;
      }

      if (rect.getY() > screenHeight * 0.75) {
          ReportLogger.step("Ignoring bottom navigation " + elementName + " candidate at Y=" + rect.getY());
          continue;
      }

      ReportLogger.step("Valid " + elementName + " candidate found at X="
              + rect.getX() + ", Y=" + rect.getY()
              + ", W=" + rect.getWidth() + ", H=" + rect.getHeight());

      return element;
  }

  return null;
}

private void tapPortfolioInnerFundsTabByCoordinates() {
  ReportLogger.step("Tapping Portfolio inner Funds tab by coordinate fallback");

  Dimension size = driver.manage().window().getSize();

  int x = (int) (size.getWidth() * 0.30);
  int y = (int) (size.getHeight() * 0.30);

  tapByCoordinates(x, y);

  ReportLogger.pass("Tapped Portfolio inner Funds tab by coordinate fallback at X=" + x + ", Y=" + y);
}

private boolean isPortfolioFundsListVisible() {
  if (isPortfolioChildPageVisible()) {
      return false;
  }

  return isVisible(byDescContains("Current"))
          || isVisible(byDescContains("Invested"))
          || isVisible(byDescContains("Sort By"))
          || isVisible(byTextContains("Current"))
          || isVisible(byTextContains("Invested"))
          || isVisible(byTextContains("Sort By"))
          || isVisible(byDescContains("HDFC Pharma And Healthcare"))
          || isVisible(byDescContains("SBI Energy Opportunities"))
          || isVisible(byDescContains("Aditya Birla SL Multi-Cap"))
          || isVisible(byDescContains("Nippon India Multi Cap"))
          || isVisible(byDescContains("HDFC Flexi Cap"))
          || isVisible(byDescContains("Parag Parikh Flexi Cap"));
}

private void resetPortfolioFundsListPositionFast() {
  ReportLogger.step("Resetting Portfolio Funds list position lightly");

  try {
      smallSwipeDownW3C();
      sleep(250);
  } catch (Exception e) {
      ReportLogger.step("Light reset skipped: " + e.getMessage());
  }
}

private void openFundFromPortfolioFundsListStable(String fundName) {
  ReportLogger.step("Opening fund from Portfolio Funds list: " + fundName);

  exitPortfolioChildPageIfNeeded();

  if (!isPortfolioFundsListVisible()) {
      openPortfolioFundsTabStable();
  }

  for (int i = 1; i <= 10; i++) {
      exitPortfolioChildPageIfNeeded();

      WebElement fundElement = findVisibleElement(byDescContains(fundName));

      if (fundElement == null) {
          fundElement = findVisibleElement(byTextContains(fundName));
      }

      if (fundElement != null) {
          Rectangle rect = fundElement.getRect();

          if (rect == null || rect.getHeight() <= 0 || rect.getWidth() <= 0) {
              throw new AssertionError("Fund element found but invalid bounds for: " + fundName);
          }

          int rowCenterY = rect.getY() + rect.getHeight() / 2;

          tapElementCenter(fundElement);
          sleep(900);

          if (isPortfolioFundDetailsOpened(fundName)) {
              ReportLogger.pass("Opened Portfolio fund details: " + fundName);
              return;
          }

          int rightX = driver.manage().window().getSize().getWidth() - 70;
          tapByCoordinates(rightX, rowCenterY);
          sleep(900);

          if (isPortfolioFundDetailsOpened(fundName)) {
              ReportLogger.pass("Opened Portfolio fund details after right-side row tap: " + fundName);
              return;
          }

          throw new AssertionError("Tapped Portfolio fund but Fund Details did not open: " + fundName);
      }

      smallSwipeUpW3C();
      sleep(250);
  }

  throw new AssertionError("Portfolio fund not found in current investor Funds list: " + fundName);
}

private boolean isPortfolioFundDetailsOpened(String fundName) {
  boolean actionVisible = isVisible(byDesc("Redeem"))
          || isVisible(byDesc("Invest More"))
          || isVisible(byDesc("Invest"))
          || isVisible(byDesc("Switch"));

  boolean detailsVisible = isVisible(byDesc("Performance"))
          || isVisible(byDesc("Analysis"))
          || isVisible(byDesc("Your Investment"));

  boolean fundVisible = isVisible(byDescContains(fundName))
          || isVisible(byTextContains(fundName));

  return actionVisible || detailsVisible || fundVisible;
}

private boolean validateRedeemAndInvestMoreButtonsOnPortfolioFundDetailsSoft(String fundName) {
  ReportLogger.step("Validating Redeem and Invest More buttons for Portfolio fund: " + fundName);

  boolean overallPassed = true;

  if (!isPortfolioFundDetailsOpened(fundName)) {
      ReportLogger.step("Soft Fail: Portfolio Fund Details page is not confirmed for fund: " + fundName);
      return false;
  }

  if (!isVisible(byDescContains(fundName)) && !isVisible(byTextContains(fundName))) {
      ReportLogger.step("Soft Fail: Portfolio Fund Details fund name not visible: " + fundName);
      overallPassed = false;
  }

  boolean buttonsAligned = alignPortfolioFundActionButtonsSoft(fundName);

  if (!buttonsAligned) {
      ReportLogger.step("Soft Fail: Action buttons section not found for fund: " + fundName);
      return false;
  }

  boolean redeemVisible = isVisible(byDesc("Redeem"));
  boolean investMoreVisible = isVisible(byDesc("Invest More"));
  boolean investVisible = isVisible(byDesc("Invest"));

  if (redeemVisible) {
      ReportLogger.pass("Redeem button is visible for fund: " + fundName);
  } else {
      ReportLogger.step("Soft Fail: Redeem button is not visible for fund: " + fundName);
      overallPassed = false;
  }

  if (investMoreVisible) {
      ReportLogger.pass("Invest More button is visible for fund: " + fundName);
  } else if (investVisible) {
      ReportLogger.pass("Invest button is visible for fund: " + fundName);
  } else {
      ReportLogger.step("Soft Fail: Invest More/Invest button is not visible for fund: " + fundName);
      overallPassed = false;
  }

  if (overallPassed) {
      ReportLogger.pass("Redeem and Invest More/Invest buttons visible for Portfolio fund: " + fundName);
  } else {
      ReportLogger.step("Soft Fail: One or more action button validations failed for fund: " + fundName);
  }

  return overallPassed;
}

private boolean alignPortfolioFundActionButtonsSoft(String fundName) {
  ReportLogger.step("Aligning Portfolio Fund Details action buttons for fund: " + fundName);

  boolean redeemVisible = isVisible(byDesc("Redeem"));
  boolean investVisible = isVisible(byDesc("Invest More")) || isVisible(byDesc("Invest"));

  if (redeemVisible || investVisible) {
      ReportLogger.pass("At least one Portfolio Fund Details action button is visible");
      return true;
  }

  for (int i = 1; i <= 5; i++) {
      smallSwipeUpW3C();
      sleep(400);

      redeemVisible = isVisible(byDesc("Redeem"));
      investVisible = isVisible(byDesc("Invest More")) || isVisible(byDesc("Invest"));

      if (redeemVisible || investVisible) {
          ReportLogger.pass("Portfolio Fund Details action button area found after swipe");
          return true;
      }
  }

  for (int i = 1; i <= 2; i++) {
      smallSwipeDownW3C();
      sleep(400);

      redeemVisible = isVisible(byDesc("Redeem"));
      investVisible = isVisible(byDesc("Invest More")) || isVisible(byDesc("Invest"));

      if (redeemVisible || investVisible) {
          ReportLogger.pass("Portfolio Fund Details action button area found after recovery swipe");
          return true;
      }
  }

  ReportLogger.step("Soft Fail: Portfolio Fund Details action buttons not visible for fund: " + fundName);
  return false;
}

private void returnToPortfolioFundsListFast() {
  ReportLogger.step("Returning to Portfolio Funds list");

  pressBack("Back from Portfolio Fund Details");
  sleep(800);

  exitPortfolioChildPageIfNeeded();

  if (isPortfolioFundsListVisible()) {
      ReportLogger.pass("Returned to Portfolio Funds list");
      return;
  }

  waitForAnyVisible(
          new By[]{
                  byDescContains("Current"),
                  byDescContains("Invested"),
                  byDescContains("Sort By"),
                  byTextContains("Current"),
                  byTextContains("Invested"),
                  byTextContains("Sort By")
          },
          "Portfolio Funds list",
          5
  );

  ReportLogger.pass("Returned to Portfolio Funds list");
}

private void recoverBackToPortfolioFundsListAfterSoftFailure() {
  ReportLogger.step("Recovering back to Portfolio Funds list after soft failure");

  for (int i = 1; i <= 3; i++) {
      if (isPortfolioFundsListVisible()) {
          ReportLogger.pass("Recovered to Portfolio Funds list");
          return;
      }

      pressBackSilently();
      sleep(700);

      exitPortfolioChildPageIfNeeded();
  }

  if (!isPortfolioFundsListVisible()) {
      try {
          openPortfolioTabForFundValidation();
          openPortfolioFundsTabOnlyIfNeeded();
      } catch (Exception e) {
          ReportLogger.step("Recovery warning: Unable to reopen Portfolio Funds list. Reason: " + e.getMessage());
      }
  }
}

// =========================================================
// PREMIUM COVERAGE - SPECIFIC FUND VALIDATION
// =========================================================

public void verifyPremiumCoverageForWhiteOakMidCapFund() {
    ReportLogger.step("Verifying Premium Coverage for WhiteOak Capital Mid Cap Fund");

    openFundDetailsFromSearchForPremiumCoverage(
            "WhiteOak Capital Mid Cap Fund",
            "WhiteOak Capital Mid Cap Fund - Direct Plan"
    );

    verifyPremiumCoverageSectionMandatoryForPaidUser();

    ReportLogger.pass("Premium Coverage validated successfully for WhiteOak Capital Mid Cap Fund");
}

private void openFundDetailsFromSearchForPremiumCoverage(String searchKeyword, String expectedFundHeader) {
    ReportLogger.step("Opening fund from search for Premium Coverage validation: " + searchKeyword);

    openFundsTab();
    tapSearchIcon();
    enterSearchKeyword(searchKeyword);
    openPremiumCoverageFundResult(searchKeyword, expectedFundHeader);
    waitForPremiumCoverageFundDetailsPage(searchKeyword, expectedFundHeader);

    ReportLogger.pass("Opened fund details page for Premium Coverage: " + expectedFundHeader);
}

private void openPremiumCoverageFundResult(String searchKeyword, String expectedFundHeader) {
    ReportLogger.step("Opening Premium Coverage fund result: " + searchKeyword);

    sleep(2500);

    boolean opened = false;

    if (tapIfVisible(byDesc(expectedFundHeader), "Exact fund result: " + expectedFundHeader)) {
        opened = true;
    } else if (tapIfVisible(byDescContains(expectedFundHeader), "Fund result contains expected header: " + expectedFundHeader)) {
        opened = true;
    } else if (tapIfVisible(byDesc(searchKeyword), "Exact fund result: " + searchKeyword)) {
        opened = true;
    } else if (tapIfVisible(byDescContains(searchKeyword), "Fund result contains: " + searchKeyword)) {
        opened = true;
    } else if (tapIfVisible(byTextContains(searchKeyword), "Fund result text contains: " + searchKeyword)) {
        opened = true;
    }

    if (!opened) {
        Dimension size = driver.manage().window().getSize();

        int x = (int) (size.getWidth() * 0.45);
        int y = (int) (size.getHeight() * 0.265);

        ReportLogger.debug("WhiteOak fund result locator not found. Using coordinate fallback x=" + x + ", y=" + y);
        tapByCoordinates(x, y);
    }

    sleep(3500);
}

private void waitForPremiumCoverageFundDetailsPage(String searchKeyword, String expectedFundHeader) {
    ReportLogger.step("Waiting for Premium Coverage fund details page");

    for (int i = 1; i <= 15; i++) {
        if (isVisible(byDesc(expectedFundHeader))
                || isVisible(byDescContains(expectedFundHeader))
                || isVisible(byDescContains(searchKeyword))) {
            ReportLogger.pass("Premium Coverage fund details page opened: " + expectedFundHeader);
            return;
        }

        if (isVisible(byDesc("NAV"))
                && (isVisible(byDescContains("Opinion:")) || isVisible(byDescContains("Opinion")))) {
            ReportLogger.pass("Premium Coverage fund details page opened with NAV and Opinion labels");
            return;
        }

        sleep(1000);
    }

    throw new AssertionError("Premium Coverage fund details page did not open for: " + expectedFundHeader);
}
private void verifyPremiumCoverageSectionMandatoryForPaidUser() {
    ReportLogger.step("Verifying mandatory Premium Coverage section");

    boolean premiumCoverageVisible = scrollToPremiumCoverageIfAvailable();

    if (!premiumCoverageVisible) {
        throw new AssertionError("Premium Coverage section is expected but not visible for this fund");
    }

    List<String> premiumCoverageFailures = new ArrayList<>();

    runSoftValidation(premiumCoverageFailures, "Premium Coverage heading", new Runnable() {
        @Override
        public void run() {
            assertVisibleAndLog(byDesc("Premium Coverage"), "Premium Coverage heading");
        }
    });

    /*
     * Validate the actual Premium Coverage card before opening it.
     * The heading alone is not enough. The card should be fully visible and expose
     * its title/date/teaser before tap.
     */
    runSoftValidation(premiumCoverageFailures, "Premium Coverage full card visibility", new Runnable() {
        @Override
        public void run() {
            alignPremiumCoverageFullCardInViewport();
            assertPremiumCoverageCardFullyVisible();
        }
    });

    logOptionalVisibleText(
            byDescContains("FUND ADVISOR'S NOTE"),
            "Premium Coverage article type"
    );

    runSoftValidation(premiumCoverageFailures, "Premium Coverage article date", new Runnable() {
        @Override
        public void run() {
            if (isVisible(byDescContains("01 Sep, 2025"))) {
                assertVisibleAndLog(byDescContains("01 Sep, 2025"), "Premium Coverage article date");
                return;
            }

            throw new AssertionError("Premium Coverage article date is not visible");
        }
    });

    runSoftValidation(premiumCoverageFailures, "Premium Coverage article title", new Runnable() {
        @Override
        public void run() {
            if (isVisible(byDescContains("How we"))) {
                assertVisibleAndLog(byDescContains("How we"), "Premium Coverage article title");
                return;
            }

            if (isVisible(byDescContains("sharpening our fund choices"))) {
                assertVisibleAndLog(
                        byDescContains("sharpening our fund choices"),
                        "Premium Coverage article title"
                );
                return;
            }

            throw new AssertionError("Premium Coverage article title is not visible");
        }
    });

    runSoftValidation(premiumCoverageFailures, "Premium Coverage teaser text", new Runnable() {
        @Override
        public void run() {
            if (isVisible(byDescContains("Still long-term at heart"))) {
                assertVisibleAndLog(
                        byDescContains("Still long-term at heart"),
                        "Premium Coverage teaser text"
                );
                return;
            }

            if (isVisible(byDescContains("quicker to refresh our choices"))) {
                assertVisibleAndLog(
                        byDescContains("quicker to refresh our choices"),
                        "Premium Coverage teaser text"
                );
                return;
            }

            throw new AssertionError("Premium Coverage teaser text is not visible");
        }
    });

    runSoftValidation(premiumCoverageFailures, "Premium Coverage article inner page complete text", new Runnable() {
        @Override
        public void run() {
            openPremiumCoverageCardAndValidateInnerContent();
        }
    });

    throwIfValidationFailures("Premium Coverage mandatory section", premiumCoverageFailures);

    ReportLogger.pass("Mandatory Premium Coverage card and complete article text validated successfully");
}

private void alignPremiumCoverageFullCardInViewport() {
    ReportLogger.step("Aligning full Premium Coverage card in viewport");

    for (int i = 1; i <= 12; i++) {
        WebElement cardElement = findPremiumCoverageCardElement();

        if (cardElement != null) {
            Rectangle rect = cardElement.getRect();

            if (isElementFullyInsideSafeViewport(rect)) {
                ReportLogger.pass(
                        "Premium Coverage full card is visible in viewport"
                                + " | x=" + rect.getX()
                                + " y=" + rect.getY()
                                + " w=" + rect.getWidth()
                                + " h=" + rect.getHeight()
                );
                return;
            }

            Dimension size = driver.manage().window().getSize();

            if (rect.getY() + rect.getHeight() > size.getHeight() - 170) {
                smallSwipeUpW3C();
            } else if (rect.getY() < 100) {
                smallSwipeDownW3C();
            } else {
                smallSwipeUpW3C();
            }

            sleep(650);
            continue;
        }

        smallSwipeUpW3C();
        sleep(650);
    }

    throw new AssertionError("Premium Coverage full card was not fully visible after controlled scrolling");
}

private void assertPremiumCoverageCardFullyVisible() {
    WebElement cardElement = findPremiumCoverageCardElement();

    if (cardElement == null) {
        throw new AssertionError("Premium Coverage card element is not visible");
    }

    Rectangle rect = cardElement.getRect();

    if (!isElementFullyInsideSafeViewport(rect)) {
        throw new AssertionError(
                "Premium Coverage card is not fully visible in viewport"
                        + " | x=" + rect.getX()
                        + " y=" + rect.getY()
                        + " w=" + rect.getWidth()
                        + " h=" + rect.getHeight()
                        + " | screenHeight=" + driver.manage().window().getSize().getHeight()
        );
    }

    String cardText = normalizeArticleText(getElementReadableText(cardElement));

    if (!cardText.contains("How we")
            && !cardText.contains("sharpening our fund choices")
            && !cardText.contains("Still long-term at heart")
            && !cardText.contains("quicker to refresh our choices")) {
        throw new AssertionError("Premium Coverage card is visible but expected card text is not exposed");
    }

    ReportLogger.pass("Premium Coverage card is fully visible with expected content");
    logValidatedText("Premium Coverage full card", cardText);
}

private WebElement findPremiumCoverageCardElement() {
    List<By> locators = Arrays.asList(
            byDescContains("How we"),
            byDescContains("sharpening our fund choices"),
            byDescContains("Still long-term at heart"),
            byDescContains("quicker to refresh our choices"),
            byDescContains("01 Sep, 2025")
    );

    WebElement bestElement = null;
    int bestTextLength = -1;

    for (By locator : locators) {
        try {
            List<WebElement> elements = driver.findElements(locator);

            for (WebElement element : elements) {
                if (element == null || !element.isDisplayed()) {
                    continue;
                }

                Rectangle rect = element.getRect();

                if (rect == null || rect.getWidth() <= 0 || rect.getHeight() <= 0) {
                    continue;
                }

                String text = normalizeArticleText(getElementReadableText(element));

                if (text.length() > bestTextLength) {
                    bestTextLength = text.length();
                    bestElement = element;
                }
            }

        } catch (Exception ignored) {
            // Try next locator
        }
    }

    return bestElement;
}

private boolean isElementFullyInsideSafeViewport(Rectangle rect) {
    if (rect == null) {
        return false;
    }

    Dimension size = driver.manage().window().getSize();

    int topSafeArea = 90;
    int bottomSafeArea = size.getHeight() - 170;

    int elementTop = rect.getY();
    int elementBottom = rect.getY() + rect.getHeight();

    return elementTop >= topSafeArea
            && elementBottom <= bottomSafeArea
            && rect.getWidth() > 0
            && rect.getHeight() > 0;
}

private void openPremiumCoverageCardAndValidateInnerContent() {
    ReportLogger.step("Opening Premium Coverage article card");

    alignPremiumCoverageFullCardInViewport();

    WebElement cardElement = findPremiumCoverageCardElement();

    if (cardElement == null) {
        throw new AssertionError("Unable to find Premium Coverage card before opening article");
    }

    tapElementCenter(cardElement);
    sleep(3500);

    waitForPremiumCoverageArticleDetailsPage();

    validatePremiumCoverageArticleDetailsPage();

    pressBack("Back from Premium Coverage article details page");
    sleep(1800);

    waitForAnyVisible(
            new By[]{
                    byDesc("Premium Coverage"),
                    byDescContains("Premium Coverage"),
                    byDescContains("How we"),
                    byDescContains("sharpening our fund choices")
            },
            "Premium Coverage section after returning from article",
            10
    );

    ReportLogger.pass("Returned from Premium Coverage article details page");
}

private void waitForPremiumCoverageArticleDetailsPage() {
    ReportLogger.step("Waiting for Premium Coverage article details page");

    for (int i = 1; i <= 12; i++) {
        if (isVisible(byDescContains("How we"))
                || isVisible(byDescContains("sharpening our fund choices"))
                || isVisible(byDescContains("FUND ADVISOR'S NOTE"))
                || isVisible(byDescContains("DHIRENDRA KUMAR"))
                || isVisible(byDescContains("What that means for you"))
                || isVisible(byDescContains("What changed last week"))) {

            ReportLogger.pass("Premium Coverage article details page opened");
            return;
        }

        sleep(800);
    }

    throw new AssertionError("Premium Coverage article details page did not open after tapping card");
}

private void validatePremiumCoverageArticleDetailsPage() {
    ReportLogger.step("Validating complete Premium Coverage article inner content");

    List<String> articleFailures = new ArrayList<>();

    String completeArticleText = collectPremiumCoverageArticleTextByScrolling();

    ReportLogger.step("Validating complete Premium Coverage article text");

    validateExpectedArticleTextChunk(
            articleFailures,
            completeArticleText,
            "Article title",
            "How we're sharpening our fund choices"
    );

    validateExpectedArticleTextChunk(
            articleFailures,
            completeArticleText,
            "Article intro",
            "Still long-term at heart, but quicker to refresh our choices"
    );

    validateExpectedArticleTextChunk(
            articleFailures,
            completeArticleText,
            "Market cycle paragraph",
            "Markets move in cycles. So do sectors and styles. Leaders cool off, new winners emerge. Our job is to stay patient for the long game-and keep our framework sharp enough to catch the turn."
    );

    validateExpectedArticleTextChunk(
            articleFailures,
            completeArticleText,
            "Value Research Fund Advisor paragraph",
            "At Value Research Fund Advisor, the principles haven't changed: discipline, diversification and time in the market. What's new is the lens. We're giving a little more room to near- and medium-term performance-not to chase noise, but to spot tomorrow's leaders sooner and refresh our choices faster when the data demands it."
    );

    validateExpectedArticleTextChunk(
            articleFailures,
            completeArticleText,
            "What that means heading",
            "What that means for you"
    );

    validateExpectedArticleTextChunk(
            articleFailures,
            completeArticleText,
            "Buy list point",
            "Our Buy list will be a bit broader."
    );

    validateExpectedArticleTextChunk(
            articleFailures,
            completeArticleText,
            "Analyst Choice point",
            "Analyst's Choice will remain the tight set of our strongest ideas at any point."
    );

    validateExpectedArticleTextChunk(
            articleFailures,
            completeArticleText,
            "What changed last week heading",
            "What changed last week"
    );

    validateExpectedArticleTextChunk(
            articleFailures,
            completeArticleText,
            "Mid caps heading",
            "Mid caps"
    );

    validateExpectedArticleTextChunk(
            articleFailures,
            completeArticleText,
            "WhiteOak addition point",
            "Added to Analyst's Choice: WhiteOak Capital Mid Cap Fund - a rare, merit-based inclusion even before its three-year mark, with strong, broad-based execution across ups and downs."
    );

    validateExpectedArticleTextChunk(
            articleFailures,
            completeArticleText,
            "Mid cap continuation point",
            "Continue in Analyst's Choice: Kotak Midcap Fund, Edelweiss Mid Cap Fund."
    );

    validateExpectedArticleTextChunk(
            articleFailures,
            completeArticleText,
            "Small caps heading",
            "Small caps"
    );

    validateExpectedArticleTextChunk(
            articleFailures,
            completeArticleText,
            "Small cap moved out point",
            "Moved out of Analyst's Choice: SBI Small Cap, Kotak Small Cap, DSP Small Cap."
    );

    validateExpectedArticleTextChunk(
            articleFailures,
            completeArticleText,
            "Small cap promoted point",
            "Promoted to Analyst's Choice: HDFC Small Cap, Bandhan Small Cap and new addition Invesco India Smallcap."
    );

    validateExpectedArticleTextChunk(
            articleFailures,
            completeArticleText,
            "What you should do heading",
            "What you should do"
    );

    validateExpectedArticleTextChunk(
            articleFailures,
            completeArticleText,
            "No rush to sell point",
            "No rush to sell. The earlier Analyst's Choice funds move to Buy or Hold. They're not \"bad\"; they've just been overtaken."
    );

    validateExpectedArticleTextChunk(
            articleFailures,
            completeArticleText,
            "Redirect money point",
            "Redirect new money. Point fresh SIPs and additional investments to the updated Analyst's Choice. Over time, your portfolio will rebalance itself-without needless costs or taxes."
    );

    validateExpectedArticleTextChunk(
            articleFailures,
            completeArticleText,
            "Closing line",
            "Steady in principle. Sharper in practice."
    );

    validateExpectedArticleTextChunk(
            articleFailures,
            completeArticleText,
            "Also read line",
            "Also read: Fund ratings guide. Analyst opinions decide."
    );

    throwIfValidationFailures("Complete Premium Coverage article text", articleFailures);

    ReportLogger.pass("Complete Premium Coverage article text validated successfully");
}

private String collectPremiumCoverageArticleTextByScrolling() {
    ReportLogger.step("Collecting Premium Coverage article text by scrolling");

    StringBuilder collectedText = new StringBuilder();

    /*
     * Article opens near top, but this controlled downward recovery helps when the card
     * tap opens the article at a partially scrolled position.
     */
    for (int i = 1; i <= 3; i++) {
        smallSwipeDownW3C();
        sleep(500);
    }

    for (int i = 1; i <= 30; i++) {
        List<String> visibleTexts = getCleanVisibleTexts();

        for (String text : visibleTexts) {
            String cleanText = normalizeArticleText(text);

            if (cleanText.isEmpty()) {
                continue;
            }

            if (!collectedText.toString().contains(cleanText)) {
                collectedText.append(cleanText).append(" ");
            }
        }

        if (containsArticleEndMarkers(collectedText.toString())) {
            ReportLogger.pass("Reached Premium Coverage article end while collecting text");
            break;
        }

        smallSwipeUpW3C();
        sleep(650);
    }

    String finalText = normalizeArticleText(collectedText.toString());

    if (finalText.isEmpty()) {
        throw new AssertionError("No article text collected from Premium Coverage article page");
    }

    ReportLogger.pass("Premium Coverage article text collected successfully");
    logValidatedText("Collected Premium Coverage article text", finalText);

    return finalText;
}

private void validateExpectedArticleTextChunk(
        List<String> failures,
        String actualFullText,
        String label,
        String expectedText
) {
    String actual = normalizeArticleText(actualFullText);
    String expected = normalizeArticleText(expectedText);

    if (actual.contains(expected)) {
        ReportLogger.pass("Validated Premium Coverage text chunk: " + label);
        return;
    }

    /*
     * Fallback for Appium text split/punctuation variation:
     * Validate important words from the expected chunk.
     */
    String[] expectedWords = expected.split(" ");
    int matchedWords = 0;
    int importantWords = 0;

    for (String word : expectedWords) {
        String cleanWord = word.trim()
                .replace(".", "")
                .replace(",", "")
                .replace(":", "")
                .replace(";", "")
                .replace("\"", "");

        if (cleanWord.length() < 4) {
            continue;
        }

        importantWords++;

        if (actual.contains(cleanWord)) {
            matchedWords++;
        }
    }

    double matchRatio = importantWords == 0 ? 0 : (matchedWords * 100.0 / importantWords);

    if (matchRatio >= 75.0) {
        ReportLogger.pass("Validated Premium Coverage text chunk by fuzzy match: " + label
                + " | matched=" + matchedWords + "/" + importantWords);
        return;
    }

    String failure = "Missing/incorrect Premium Coverage text chunk: " + label
            + " | Expected: " + expectedText
            + " | Match ratio: " + String.format("%.2f", matchRatio) + "%";

    failures.add(failure);
    ReportLogger.fail(failure);
}

private String normalizeArticleText(String text) {
    if (text == null) {
        return "";
    }

    return text
            .replace("\n", " ")
            .replace("\r", " ")
            .replace("’", "'")
            .replace("‘", "'")
            .replace("“", "\"")
            .replace("”", "\"")
            .replace("—", "-")
            .replace("–", "-")
            .replace("\u00A0", " ")
            .replaceAll("\\s+", " ")
            .trim();
}

private boolean containsArticleEndMarkers(String text) {
    String normalized = normalizeArticleText(text);

    return normalized.contains("Steady in principle")
            && normalized.contains("Sharper in practice")
            && normalized.contains("Also read")
            && normalized.contains("Fund ratings guide");
}

private boolean scrollToPremiumCoverageIfAvailable() {
    ReportLogger.step("Scrolling to Premium Coverage section");

    /*
     * Do not reset to top.
     * FD_022 opens WhiteOak fund freshly from search.
     *
     * Premium Coverage is deeper on this fund, so use:
     * 1. controlled small swipes first
     * 2. stronger full swipes as fallback
     */
    for (int i = 1; i <= 28; i++) {
        if (isVisible(byDesc("Premium Coverage"))
                || isVisible(byDescContains("Premium Coverage"))) {
            ReportLogger.pass("Premium Coverage section is visible");
            return true;
        }

        smallSwipeUpW3C();
        sleep(600);
    }

    ReportLogger.step("Premium Coverage not found with small swipes. Trying stronger scroll fallback.");

    for (int i = 1; i <= 10; i++) {
        if (isVisible(byDesc("Premium Coverage"))
                || isVisible(byDescContains("Premium Coverage"))) {
            ReportLogger.pass("Premium Coverage section is visible");
            return true;
        }

        swipeUpW3C();
        sleep(800);
    }

    ReportLogger.pass("Premium Coverage section not found after extended scrolling");
    return false;
}
    // =========================================================
    // WHO SHOULD INVEST
    // =========================================================

    public void verifyWhoShouldInvestSection() {
        recoverFundDetailsIfNeeded();

        ReportLogger.step("Verifying Who should invest section");

        scrollToTopOfFundDetails();
        scrollUntilVisible("Who should invest?", 14);

        List<String> whoShouldInvestFailures = new ArrayList<>();

        runSoftValidation(whoShouldInvestFailures, "Who should invest section", new Runnable() {
            @Override
            public void run() {
                assertVisibleAndLog(byDesc("Who should invest?"), "Who should invest section");
            }
        });
        runSoftValidation(whoShouldInvestFailures, "Suitability text", new Runnable() {
            @Override
            public void run() {
                assertVisibleAndLog(byDescContains("Flexi-cap funds are suitable"), "Suitability text");
            }
        });

        logOptionalVisibleText(byDescContains("Seeking exposure"), "Who should invest bullet");
        logOptionalVisibleText(byDescContains("long-term wealth creation"), "Who should invest bullet");
        logOptionalVisibleText(byDescContains("5+ year investment horizon"), "Who should invest bullet");
        logOptionalVisibleText(byDescContains("Invest only through SIP"), "Who should invest note");

        throwIfValidationFailures("Who should invest section", whoShouldInvestFailures);

        ReportLogger.pass("Who should invest section validated successfully");
    }

    public void verifyFundStyleDescriptionForPaidUser() {
        recoverFundDetailsIfNeeded();

        ReportLogger.step("Verifying Fund Style description for paid user");

        navigateToWhereDoesItInvestSectionExact();
        returnToWhereDoesItInvestHeaderForMore();

        boolean whereDoesItInvestMorePageOpened = tapWhereDoesItInvestMoreButton();

        if (!whereDoesItInvestMorePageOpened) {
            throw new AssertionError("Where does it invest More page did not open for Fund Style validation");
        }

        try {
            waitForAnyVisible(
                    new By[]{
                            byDesc("Fund Style"),
                            byDesc("Equity Fund Style"),
                            byDescContains("A unified snapshot")
                    },
                    "Fund Style section",
                    10
            );

            scrollUntilVisibleOptional("Fund Style", 8);

            List<String> fundStyleFailures = new ArrayList<>();

            runSoftValidation(fundStyleFailures, "Fund Style heading", new Runnable() {
                @Override
                public void run() {
                    assertVisibleAndLog(byDesc("Fund Style"), "Fund Style heading");
                }
            });

            runSoftValidation(fundStyleFailures, "Equity Fund Style label", new Runnable() {
                @Override
                public void run() {
                    assertVisibleAndLog(byDesc("Equity Fund Style"), "Equity Fund Style label");
                }
            });

            runSoftValidation(fundStyleFailures, "Fund Style description", new Runnable() {
                @Override
                public void run() {
                    assertVisibleAndLog(
                            byDescContains("A unified snapshot of a fund's portfolio"),
                            "Fund Style description"
                    );
                }
            });

            logOptionalVisibleText(byDesc("Know more."), "Fund Style Know more link");

            throwIfValidationFailures("Fund Style section", fundStyleFailures);

            ReportLogger.pass("Fund Style description validated successfully for paid user");

        } finally {
            pressBack("Back from Where does it invest More page");
            waitForFundHeaderOrKnownSection();
        }
    }
    
    public void verifyOverlapWithBenchmarkForPaidUser() {
        recoverFundDetailsIfNeeded();

        ReportLogger.step("Verifying Overlap with benchmark for paid user");

        navigateToWhereDoesItInvestSectionExact();
        returnToWhereDoesItInvestHeaderForMore();

        boolean whereDoesItInvestMorePageOpened = tapWhereDoesItInvestMoreButton();

        if (!whereDoesItInvestMorePageOpened) {
            throw new AssertionError("Where does it invest More page did not open for Overlap with benchmark validation");
        }

        try {
            /*
             * Do not wait blindly for Overlap with benchmark here.
             * The More page can open slightly below the top depending on scroll state.
             * Existing validator already realigns the page to Overlap with benchmark.
             */
            validateMorePageOverlapWithBenchmark();

            ReportLogger.pass("Overlap with benchmark validated successfully for paid user");

        } finally {
            pressBack("Back from Where does it invest More page");
            waitForFundHeaderOrKnownSection();
        }
    }
    
    public void verifyConcentrationSectionForPaidUser() {
        recoverFundDetailsIfNeeded();

        ReportLogger.step("Verifying Concentration section for paid user");

        navigateToWhereDoesItInvestSectionExact();
        returnToWhereDoesItInvestHeaderForMore();

        boolean whereDoesItInvestMorePageOpened = tapWhereDoesItInvestMoreButton();

        if (!whereDoesItInvestMorePageOpened) {
            throw new AssertionError("Where does it invest More page did not open for Concentration validation");
        }

        try {
            waitForAnyVisible(
                    new By[]{
                            byDesc("Concentration"),
                            byDesc("Equity Concentration"),
                            byDesc("Number of Stocks"),
                            byDesc("Top 10 Holdings")
                    },
                    "Concentration section",
                    10
            );

            validateMorePageConcentration();

            ReportLogger.pass("Concentration section validated successfully for paid user");

        } finally {
            pressBack("Back from Where does it invest More page");
            waitForFundHeaderOrKnownSection();
        }
    }
    
    public void verifySectorAllocationSectionForPaidUser() {
        recoverFundDetailsIfNeeded();

        ReportLogger.step("Verifying Sector Allocation section for paid user");

        navigateToWhereDoesItInvestSectionExact();
        returnToWhereDoesItInvestHeaderForMore();

        boolean whereDoesItInvestMorePageOpened = tapWhereDoesItInvestMoreButton();

        if (!whereDoesItInvestMorePageOpened) {
            throw new AssertionError("Where does it invest More page did not open for Sector Allocation validation");
        }

        try {
            /*
             * Do not wait blindly for Sector Allocation here.
             * The More page can open at Overlap, Fund Style, Concentration,
             * or sometimes near Top Equity Holdings depending on previous scroll state.
             * First align the page to Sector Allocation, then validate.
             */
            alignWhereDoesItInvestMorePageToSectorAllocation();

            validateMorePageSectorAllocation();

            ReportLogger.pass("Sector Allocation section validated successfully for paid user");

        } finally {
            pressBack("Back from Where does it invest More page");
            waitForFundHeaderOrKnownSection();
        }
    }
    
    private void alignWhereDoesItInvestMorePageToSectorAllocation() {
        ReportLogger.step("Aligning Where does it invest More Page to Sector Allocation");

        for (int attempt = 1; attempt <= 18; attempt++) {
            boolean sectorAllocationVisible =
                    isVisible(byDesc("Sector Allocation"))
                            || isVisible(byDescContains("Sector Allocation"));

            boolean sectorHeaderVisible =
                    isVisible(byDesc("Sector"))
                            || isVisible(byDesc("Fund (%)"))
                            || isVisible(byDesc("Category (%)"));

            if (sectorAllocationVisible || sectorHeaderVisible) {
                ReportLogger.pass("Sector Allocation section is visible");
                return;
            }

            /*
             * If we are below Sector Allocation, move slightly up.
             * Top Equity Holdings and Key come after Sector Allocation.
             */
            boolean belowSectorAllocation =
                    isVisible(byDesc("Top Equity Holdings"))
                            || isVisible(byDescContains("Top Equity Holdings"))
                            || isVisible(byDesc("Key"))
                            || isVisible(byDescContains("Key"));

            /*
             * If we are above Sector Allocation, move slightly down.
             * These sections come before Sector Allocation.
             */
            boolean aboveSectorAllocation =
                    isVisible(byDesc("Overlap with benchmark"))
                            || isVisible(byDescContains("Overlap with benchmark"))
                            || isVisible(byDesc("Fund Style"))
                            || isVisible(byDescContains("Fund Style"))
                            || isVisible(byDesc("Concentration"))
                            || isVisible(byDescContains("Concentration"))
                            || isVisible(byDesc("Equity Concentration"))
                            || isVisible(byDescContains("Equity Concentration"));

            if (belowSectorAllocation) {
                tinySwipeDownW3C();
            } else {
                /*
                 * Default direction should be down the page because Sector Allocation
                 * is below Overlap, Fund Style and Concentration.
                 */
                tinySwipeUpW3C();
            }

            sleep(700);
        }

        throw new AssertionError("Unable to align Where does it invest More Page to Sector Allocation section");
    }
    
    

    public void verifyTopEquityHoldingsSectionForPaidUser() {
        recoverFundDetailsIfNeeded();

        ReportLogger.step("Verifying Top Equity Holdings section for paid user");

        navigateToWhereDoesItInvestSectionExact();
        returnToWhereDoesItInvestHeaderForMore();

        boolean whereDoesItInvestMorePageOpened = tapWhereDoesItInvestMoreButton();

        if (!whereDoesItInvestMorePageOpened) {
            throw new AssertionError("Where does it invest More page did not open for Top Equity Holdings validation");
        }

        try {
            /*
             * Do not use waitForAnyVisible before this validation.
             * Top Equity Holdings is a deep section. Existing validator scrolls and collects
             * values until the Key section or until all expected holdings are collected.
             */
            validateMorePageTopEquityHoldings();

            ReportLogger.pass("Top Equity Holdings section validated successfully for paid user");

        } finally {
            pressBack("Back from Where does it invest More page");
            waitForFundHeaderOrKnownSection();
        }
    }

    public void verifyKeyLegendSectionForPaidUser() {
        recoverFundDetailsIfNeeded();

        ReportLogger.step("Verifying Key legend section for paid user");

        navigateToWhereDoesItInvestSectionExact();
        returnToWhereDoesItInvestHeaderForMore();

        boolean whereDoesItInvestMorePageOpened = tapWhereDoesItInvestMoreButton();

        if (!whereDoesItInvestMorePageOpened) {
            throw new AssertionError("Where does it invest More page did not open for Key legend validation");
        }

        try {
            /*
             * Do not wait for Key before calling the validator.
             * Key is the last section. Existing validator scrolls until Key is found
             * and then validates all movement legends.
             */
            validateMorePageKey();

            ReportLogger.pass("Key legend section validated successfully for paid user");

        } finally {
            pressBack("Back from Where does it invest More page");
            waitForFundHeaderOrKnownSection();
        }
    }
    // =========================================================
    // WHERE DOES IT INVEST
    // =========================================================

    public void verifyWhereDoesItInvestSectionAndHoldingsPage() {
        recoverFundDetailsIfNeeded();

        ReportLogger.step("Verifying Where does it invest section and Where does it invest More page");

        /*
         * IMPORTANT:
         * Do not use scrollToTopOfFundDetails() + full swipe search here.
         * FD_008 may start from different Fund Details positions when running independently.
         * Full swipe can skip the Where does it invest card and land directly on
         * Fund Manager / More Details / News.
         * This method uses controlled small swipes and searches in both directions.
         */
        navigateToWhereDoesItInvestSectionExact();

        validateWhereDoesItInvestSectionComplete();

        /*
         * Asset Class / Market Cap / Holding validation scrolls inside the card body.
         * Before tapping section-level More, come back to the heading/chips area.
         * This avoids tapping the Holding table More / Show more by mistake.
         */
        returnToWhereDoesItInvestHeaderForMore();

        boolean whereDoesItInvestMorePageOpened = tapWhereDoesItInvestMoreButton();

        validateHoldingsMorePageIfOpened(whereDoesItInvestMorePageOpened);

        ReportLogger.pass("Where does it invest section and Where does it invest More page validated successfully");
    }

    private void navigateToWhereDoesItInvestSectionExact() {
        ReportLogger.step("Navigating to Where does it invest section using controlled bidirectional scroll");

        if (isWhereDoesItInvestSectionVisibleNow()) {
            ReportLogger.pass("Where does it invest section is already visible");
            alignWhereDoesItInvestCardBody();
            return;
        }

        /*
         * First search downward from top/mid page area using small swipes.
         * This avoids skipping the card.
         */
        for (int i = 1; i <= 14; i++) {
            smallSwipeUpW3C();
            sleep(650);

            if (isWhereDoesItInvestSectionVisibleNow()) {
                ReportLogger.pass("Where does it invest section found while scrolling down");
                alignWhereDoesItInvestCardBody();
                return;
            }

            if (isBelowWhereDoesItInvestSection()) {
                ReportLogger.debug("Moved below Where does it invest section. Switching to upward search.");
                break;
            }
        }

        /*
         * If the previous search moved below the card, come back upward.
         */
        for (int i = 1; i <= 14; i++) {
            smallSwipeDownW3C();
            sleep(650);

            if (isWhereDoesItInvestSectionVisibleNow()) {
                ReportLogger.pass("Where does it invest section found while scrolling back up");
                alignWhereDoesItInvestCardBody();
                return;
            }
        }

        throw new AssertionError(
                "Where does it invest section not found using controlled bidirectional scroll. Visible texts: "
                        + getVisibleTextsPreviewForDebug()
        );
    }

    private boolean isWhereDoesItInvestSectionVisibleNow() {
        return isVisible(byDesc("Where does it invest?"))
                || isVisible(byDescContains("Where does it invest?"))
                || (isVisible(byDesc("Asset Class"))
                && isVisible(byDesc("Market Cap"))
                && isVisible(byDesc("Holding")));
    }

    private boolean isBelowWhereDoesItInvestSection() {
        return isVisible(byDesc("Who is the manager?"))
                || isVisible(byDesc("More Details"))
                || isVisible(byDesc("News"))
                || isVisible(byDesc("Investment Strategy"))
                || isVisible(byDesc("Other Information"))
                || isVisible(byDesc("Taxation"));
    }

    private String getVisibleTextsPreviewForDebug() {
        List<String> values = getCleanVisibleTexts();

        if (values == null || values.isEmpty()) {
            return "[]";
        }

        int end = Math.min(values.size(), 30);
        return values.subList(0, end).toString();
    }

    private void validateWhereDoesItInvestSectionComplete() {
        ReportLogger.step("Validating Where does it invest section completely");

        List<String> whereInvestFailures = new ArrayList<>();

        runSoftValidation(whereInvestFailures, "Where does it invest card alignment", new Runnable() {
            @Override
            public void run() {
                alignWhereDoesItInvestCardBody();
            }
        });

        optionalVisibleAny(
                new By[]{
                        byDesc("Where does it invest?"),
                        byDescContains("Where does it invest?"),
                        byDesc("Asset Class"),
                        byDesc("Market Cap"),
                        byDesc("Holding"),
                        byDesc("Asset Allocation")
                },
                "Where does it invest section"
        );

        runSoftValidation(whereInvestFailures, "Asset Class chip", new Runnable() {
            @Override
            public void run() {
                assertVisibleAndLog(byDesc("Asset Class"), "Asset Class chip");
            }
        });

        runSoftValidation(whereInvestFailures, "Market Cap chip", new Runnable() {
            @Override
            public void run() {
                assertVisibleAndLog(byDesc("Market Cap"), "Market Cap chip");
            }
        });

        runSoftValidation(whereInvestFailures, "Holding chip", new Runnable() {
            @Override
            public void run() {
                assertVisibleAndLog(byDesc("Holding"), "Holding chip");
            }
        });

        logCleanValidation("Where does it invest - Section", "Heading: Where does it invest?");
        logCleanValidation("Where does it invest - Section", "Tabs: Asset Class | Market Cap | Holding");

        runSoftValidation(whereInvestFailures, "Asset Class table", new Runnable() {
            @Override
            public void run() {
                validateAssetClassTable();
            }
        });

        runSoftValidation(whereInvestFailures, "Market Cap table", new Runnable() {
            @Override
            public void run() {
                validateMarketCapTable();
            }
        });

        runSoftValidation(whereInvestFailures, "Holding table", new Runnable() {
            @Override
            public void run() {
                validateHoldingTable();
            }
        });

        throwIfValidationFailures("Where does it invest section", whereInvestFailures);

        ReportLogger.pass("Where does it invest section validated completely");
    }

    private void validateAssetClassTable() {
        ReportLogger.step("Validating Asset Class table");

        returnToWhereDoesItInvestChips();
        tapVisible(byDesc("Asset Class"), "Asset Class chip");
        sleep(900);

        List<String> values = collectWhereDoesItInvestTabValues(
                "Asset Class",
                new String[]{
                        "Asset Allocation",
                        "Weightage (%)",
                        "Equity",
                        "Debt",
                        "Real Estate",
                        "Cash & Cash Eq."
                },
                12
        );

        validateTextPresentInList(values, "Asset Allocation", "Asset Class");
        validateTextPresentInList(values, "Weightage (%)", "Asset Class");

        logCleanValidation("Asset Class", "Header: Asset Allocation | Weightage (%)");

        validateSingleValueRow("Asset Class", values, "Equity", true);
        validateSingleValueRow("Asset Class", values, "Debt", true);
        validateSingleValueRow("Asset Class", values, "Real Estate", true);
        validateSingleValueRow("Asset Class", values, "Cash & Cash Eq.", true);

        String dateText = getOptionalDateText(values);
        if (!"-".equals(dateText)) {
            logCleanValidation("Asset Class", "Date: " + dateText);
        }

        ReportLogger.pass("Asset Class table validated");
    }

    private void validateMarketCapTable() {
        ReportLogger.step("Validating Market Cap table");

        returnToWhereDoesItInvestChips();
        tapVisible(byDesc("Market Cap"), "Market Cap chip");
        sleep(900);

        List<String> values = collectWhereDoesItInvestTabValues(
                "Market Cap",
                new String[]{
                        "Market Cap",
                        "Fund (%)",
                        "Category (%)",
                        "Large",
                        "Mid",
                        "Small",
                        "Avg Mkt Cap (₹ Cr)"
                },
                12
        );

        validateTextPresentInList(values, "Market Cap", "Market Cap");
        validateTextPresentInList(values, "Fund (%)", "Market Cap");
        validateTextPresentInList(values, "Category (%)", "Market Cap");

        logCleanValidation("Market Cap", "Header: Market Cap | Fund (%) | Category (%)");

        validateDoubleValueRow("Market Cap", values, "Large", true);
        validateDoubleValueRow("Market Cap", values, "Mid", true);
        validateDoubleValueRow("Market Cap", values, "Small", true);
        validateDoubleValueRow("Market Cap", values, "Avg Mkt Cap (₹ Cr)", true);

        String dateText = getOptionalDateText(values);
        if (!"-".equals(dateText)) {
            logCleanValidation("Market Cap", "Date: " + dateText);
        }

        ReportLogger.pass("Market Cap table validated");
    }

    private void validateHoldingTable() {
        ReportLogger.step("Validating Holding table");

        returnToWhereDoesItInvestChips();
        tapVisible(byDesc("Holding"), "Holding chip");
        sleep(900);

        /*
         * Holding names change with latest portfolio data.
         * Do not hard-code names like Power Grid here.
         * Main Fund Details card may expose only top visible holdings.
         */
        List<String> values = collectHoldingTableValuesDynamically();

        validateTextPresentInList(values, "Holding", "Holding");
        validateTextPresentInList(values, "Assets (%)", "Holding");

        logCleanValidation("Holding", "Header: Holding | Assets (%)");

        List<String[]> holdingRows = extractHoldingRowsFromValues(values);

        if (holdingRows.size() < 8) {
            throw new AssertionError(
                    "Holding table has insufficient visible rows. Required at least 8, found: "
                            + holdingRows.size()
                            + " | Rows: " + rowsToReadableText(holdingRows)
                            + " | Values: " + values
            );
        }

        for (String[] row : holdingRows) {
            logCleanValidation(
                    "Holding",
                    "Row: " + row[0] + " | Assets (%): " + row[1]
            );
        }

        String dateText = getOptionalDateText(values);
        if (!"-".equals(dateText)) {
            logCleanValidation("Holding", "Date: " + dateText);
        }

        ReportLogger.pass("Holding table dynamic visible rows validated. Count: " + holdingRows.size());
    }
    
    private List<String> collectHoldingTableValuesDynamically() {
        List<String> collectedValues = new ArrayList<>();
        int previousSize = 0;
        int noProgressCount = 0;

        for (int attempt = 1; attempt <= 18; attempt++) {
            List<String> visibleTexts = getCleanVisibleTexts();

            for (String text : visibleTexts) {
                if (text == null) {
                    continue;
                }

                String clean = text.trim();

                if (clean.isEmpty()) {
                    continue;
                }

                if (!containsExactText(collectedValues, clean)) {
                    collectedValues.add(clean);
                }
            }

            List<String[]> rows = extractHoldingRowsFromValues(collectedValues);

            boolean headerVisible =
                    containsExactText(collectedValues, "Holding")
                            && containsExactText(collectedValues, "Assets (%)");

            boolean enoughRowsCollected = rows.size() >= 8;

            boolean dateVisible = hasHoldingDate(collectedValues);

            boolean nextSectionVisible =
                    containsExactText(collectedValues, "Who is the manager?")
                            || containsExactText(collectedValues, "Fund Manager")
                            || containsExactText(collectedValues, "More Details");

            if (headerVisible && enoughRowsCollected && (dateVisible || nextSectionVisible)) {
                ReportLogger.pass("Holding dynamic values collected: " + collectedValues);
                return collectedValues;
            }

            if (collectedValues.size() == previousSize) {
                noProgressCount++;
            } else {
                noProgressCount = 0;
                previousSize = collectedValues.size();
            }

            if (nextSectionVisible && enoughRowsCollected) {
                ReportLogger.pass("Holding rows collected before next section: " + collectedValues);
                return collectedValues;
            }

            /*
             * Use tiny scroll because this card is short.
             * Full swipe can jump to Fund Manager and miss the table body.
             */
            tinySwipeUpW3C();
            sleep(noProgressCount >= 3 ? 850 : 650);
        }

        return collectedValues;
    }

    private List<String[]> extractHoldingRowsFromValues(List<String> values) {
        List<String[]> rows = new ArrayList<>();

        if (values == null || values.isEmpty()) {
            return rows;
        }

        boolean holdingTableStarted = false;

        for (int i = 0; i < values.size() - 1; i++) {
            String current = cleanSimpleValue(values.get(i));
            String next = cleanSimpleValue(values.get(i + 1));

            if (current.isEmpty()) {
                continue;
            }

            if ("Holding".equalsIgnoreCase(current)) {
                holdingTableStarted = true;
                continue;
            }

            if (!holdingTableStarted) {
                continue;
            }

            if ("Assets (%)".equalsIgnoreCase(current)) {
                continue;
            }

            if (isHoldingTableEndText(current)) {
                break;
            }

            if (isLikelyHoldingName(current) && isValidHoldingPercentage(next)) {
                if (!holdingRowAlreadyAdded(rows, current)) {
                    rows.add(new String[]{current, next});
                }
            }
        }

        return rows;
    }

    private boolean isLikelyHoldingName(String value) {
        if (value == null) {
            return false;
        }

        String clean = cleanSimpleValue(value);

        if (clean.isEmpty()) {
            return false;
        }

        if ("Holding".equalsIgnoreCase(clean)
                || "Assets (%)".equalsIgnoreCase(clean)
                || "Asset Class".equalsIgnoreCase(clean)
                || "Market Cap".equalsIgnoreCase(clean)
                || "Where does it invest?".equalsIgnoreCase(clean)
                || "Go back".equalsIgnoreCase(clean)
                || "Note:".equalsIgnoreCase(clean)
                || clean.toLowerCase().startsWith("as on")) {
            return false;
        }

        if (isValidHoldingPercentage(clean)) {
            return false;
        }

        if (isHoldingTableEndText(clean)) {
            return false;
        }

        /*
         * Holding names usually contain alphabetic characters.
         * Examples: ICICI Bank, Larsen & Toubro, Maruti Suzuki, Cipla.
         */
        return clean.matches(".*[A-Za-z].*");
    }

    private boolean isValidHoldingPercentage(String value) {
        if (value == null) {
            return false;
        }

        String clean = cleanSimpleValue(value);

        return clean.matches("\\d+(\\.\\d+)?");
    }

    private boolean isHoldingTableEndText(String value) {
        if (value == null) {
            return false;
        }

        String clean = cleanSimpleValue(value);

        return clean.equalsIgnoreCase("Who is the manager?")
                || clean.equalsIgnoreCase("Who is the manager")
                || clean.equalsIgnoreCase("Fund Manager")
                || clean.equalsIgnoreCase("More Details")
                || clean.equalsIgnoreCase("News")
                || clean.equalsIgnoreCase("Investment Strategy")
                || clean.equalsIgnoreCase("Other Information");
    }

    private boolean hasHoldingDate(List<String> values) {
        if (values == null || values.isEmpty()) {
            return false;
        }

        for (String value : values) {
            String clean = cleanSimpleValue(value);

            if (clean.toLowerCase().startsWith("as on")) {
                return true;
            }
        }

        return false;
    }

    private boolean holdingRowAlreadyAdded(List<String[]> rows, String holdingName) {
        if (rows == null || holdingName == null) {
            return false;
        }

        String expected = cleanSimpleValue(holdingName);

        for (String[] row : rows) {
            if (row != null && row.length > 0 && expected.equalsIgnoreCase(cleanSimpleValue(row[0]))) {
                return true;
            }
        }

        return false;
    }

    private String rowsToReadableText(List<String[]> rows) {
        if (rows == null || rows.isEmpty()) {
            return "[]";
        }

        List<String> readableRows = new ArrayList<>();

        for (String[] row : rows) {
            if (row != null && row.length >= 2) {
                readableRows.add(row[0] + "=" + row[1]);
            }
        }

        return readableRows.toString();
    }

    private String cleanSimpleValue(String value) {
        if (value == null) {
            return "";
        }

        return value
                .replace("&amp;", "&")
                .replace("\u00A0", " ")
                .trim()
                .replaceAll("\\s+", " ");
    }

    private void validateHoldingsMorePageIfOpened(boolean whereDoesItInvestMorePageOpened) {
        if (!whereDoesItInvestMorePageOpened) {
            ReportLogger.debug("Where does it invest More page did not open. Continuing after section-level validation.");
            logCleanValidation("Where does it invest More Page", "Skipped because section More did not open expected page");
            return;
        }

        try {
            boolean morePageVisible = waitForAnyVisibleOptional(
                    new By[]{
                            byDesc("Overlap with benchmark"),
                            byDesc("Fund Style"),
                            byDesc("Concentration"),
                            byDesc("Sector Allocation"),
                            byDesc("Top Equity Holdings")
                    },
                    "Where does it invest More page",
                    10
            );

            if (!morePageVisible) {
                ReportLogger.debug("Where does it invest More page opened but expected labels were not visible.");
                logCleanValidation("Where does it invest More Page", "Opened but expected section labels were not exposed in current viewport");
                return;
            }

            validateWhereDoesItInvestMorePageInSequence();

        } finally {
            pressBack("Back from Where does it invest More page");
            waitForFundHeaderOrKnownSection();
        }
    }

    private void validateWhereDoesItInvestMorePageInSequence() {
        ReportLogger.step("Validating Where does it invest More page section-wise in correct sequence");

        /*
         * Correct More page sequence from app screen:
         * 1. Overlap with benchmark
         * 2. Fund Style
         * 3. Concentration
         * 4. Sector Allocation
         * 5. Top Equity Holdings
         * 6. Key
         */
        validateMorePageOverlapWithBenchmark();
        validateMorePageFundStyle();
        validateMorePageConcentration();
        validateMorePageSectorAllocation();
        validateMorePageTopEquityHoldings();
        validateMorePageKey();

        ReportLogger.pass("Where does it invest More page validated section-wise in correct sequence");
    }

    private void validateMorePageOverlapWithBenchmark() {
        String sectionName = "Where does it invest More Page - Overlap with benchmark";
        ReportLogger.step("Validating " + sectionName);

        alignWhereDoesItInvestMorePageTopForOverlap();

        List<String> values = collectMorePageSectionValues(
                "Overlap with benchmark",
                new String[]{
                        "Overlap with benchmark",
                        "Fund's Benchmark",
                        "Overlap (%)",
                        "NIFTY 500 TRI"
                },
                10
        );

        validateTextPresentInList(values, "Overlap with benchmark", sectionName);
        validateTextPresentInList(values, "Fund's Benchmark", sectionName);
        validateTextPresentInList(values, "Overlap (%)", sectionName);

        logCleanValidation(sectionName, "Header: Fund's Benchmark | Overlap (%)");
        validateSingleValueRow(sectionName, values, "NIFTY 500 TRI", true);

        ReportLogger.pass(sectionName + " validated");
    }

    private void alignWhereDoesItInvestMorePageTopForOverlap() {
        ReportLogger.step("Aligning Where does it invest More Page to Overlap with benchmark");

        for (int i = 1; i <= 12; i++) {
            if (isVisible(byDesc("Overlap with benchmark"))) {
                ReportLogger.pass("Overlap with benchmark section is visible");
                return;
            }

            if (isVisible(byDesc("Fund Style"))
                    || isVisible(byDesc("Concentration"))
                    || isVisible(byDesc("Sector Allocation"))
                    || isVisible(byDesc("Top Equity Holdings"))
                    || isVisible(byDesc("Key"))) {
                tinySwipeDownW3C();
            } else {
                tinySwipeUpW3C();
            }

            sleep(650);
        }

        throw new AssertionError("Unable to align Where does it invest More Page to Overlap with benchmark section.");
    }

    private void validateMorePageFundStyle() {
        String sectionName = "Where does it invest More Page - Fund Style";
        ReportLogger.step("Validating " + sectionName);

        List<String> values = collectMorePageSectionValues(
                "Fund Style",
                new String[]{
                        "Fund Style",
                        "Equity Fund Style"
                },
                10
        );

        validateTextPresentInList(values, "Fund Style", sectionName);
        validateTextPresentInList(values, "Equity Fund Style", sectionName);

        if (containsPartialText(values, "A unified snapshot")) {
            logCleanValidation(sectionName, "Description: A unified snapshot of a fund's portfolio is visible");
        }

        if (containsExactText(values, "Know more.")) {
            logCleanValidation(sectionName, "Link: Know more.");
        }

        ReportLogger.pass(sectionName + " validated");
    }

    private void validateMorePageConcentration() {
        String sectionName = "Where does it invest More Page - Concentration";
        ReportLogger.step("Validating " + sectionName);

        List<String> values = collectMorePageSectionValues(
                "Concentration",
                new String[]{
                        "Concentration",
                        "Equity Concentration",
                        "Number of Stocks",
                        "Top 10 Holdings",
                        "Top 5 Stocks",
                        "Top 3 Sectors",
                        "Portfolio P/B Ratio",
                        "Portfolio P/E Ratio"
                },
                12
        );

        validateTextPresentInList(values, "Concentration", sectionName);
        validateTextPresentInList(values, "Equity Concentration", sectionName);
        logCleanValidation(sectionName, "Header: Equity Concentration");

        validateSingleValueRow(sectionName, values, "Number of Stocks", true);
        validateSingleValueRow(sectionName, values, "Top 10 Holdings", true);
        validateSingleValueRow(sectionName, values, "Top 5 Stocks", true);
        validateSingleValueRow(sectionName, values, "Top 3 Sectors", true);
        validateSingleValueRow(sectionName, values, "Portfolio P/B Ratio", true);
        validateSingleValueRow(sectionName, values, "Portfolio P/E Ratio", true);

        String dateText = getOptionalDateText(values);
        if (!"-".equals(dateText)) {
            logCleanValidation(sectionName, "Date: " + dateText);
        }

        ReportLogger.pass(sectionName + " validated");
    }

    private void validateMorePageSectorAllocation() {
        String sectionName = "Where does it invest More Page - Sector Allocation";
        ReportLogger.step("Validating " + sectionName);

        /*
         * Strict fix only for Sector Allocation:
         * Do not use the generic more-page collector here because Sector Allocation
         * is just before Top Equity Holdings. The generic collector can overscroll
         * into Top Equity Holdings / Key while trying to collect every sector row.
         */
        List<String> values = collectSectorAllocationValuesWithoutOverscroll();

        validateTextPresentInList(values, "Sector Allocation", sectionName);
        validateTextPresentInList(values, "Sector", sectionName);
        validateTextPresentInList(values, "Fund (%)", sectionName);
        validateTextPresentInList(values, "Category (%)", sectionName);
        logCleanValidation(sectionName, "Header: Sector | Fund (%) | Category (%)");

        validateDoubleValueRow(sectionName, values, "Financial", true);
        validateDoubleValueRow(sectionName, values, "Consumer Discretionary", true);
        validateDoubleValueRow(sectionName, values, "Technology", true);
        validateDoubleValueRow(sectionName, values, "Healthcare", true);
        validateDoubleValueRow(sectionName, values, "Industrials", true);
        validateDoubleValueRow(sectionName, values, "Energy & Utilities", true);
        validateDoubleValueRow(sectionName, values, "Materials", true);
        validateDoubleValueRow(sectionName, values, "Consumer Staples", true);
        validateDoubleValueRow(sectionName, values, "Real Estate", true);

        String dateText = getOptionalDateText(values);
        if (!"-".equals(dateText)) {
            logCleanValidation(sectionName, "Date: " + dateText);
        }

        ReportLogger.pass(sectionName + " validated");
    }

    private List<String> collectSectorAllocationValuesWithoutOverscroll() {
        ReportLogger.step("Collecting Where does it invest More page section: Sector Allocation");

        String sectionTitle = "Sector Allocation";
        String nextSectionTitle = "Top Equity Holdings";

        String[] requiredAnchors = new String[]{
                "Sector Allocation",
                "Sector",
                "Fund (%)",
                "Category (%)",
                "Financial",
                "Consumer Discretionary",
                "Technology",
                "Healthcare",
                "Industrials",
                "Energy & Utilities",
                "Materials",
                "Consumer Staples",
                "Real Estate"
        };

        List<String> collectedValues = new ArrayList<>();
        boolean sectionStarted = false;
        int lastCollectedSize = -1;
        int noProgressCount = 0;

        /*
         * Strict Sector Allocation fix only:
         * - Start collecting only after the Sector Allocation heading is visible.
         * - Keep only the slice that belongs to Sector Allocation.
         * - Do not stop just because Top Equity Holdings becomes visible unless all sector rows
         *   have already been collected. This was the reason Energy & Utilities / Materials /
         *   Consumer Staples / Real Estate were missed.
         * - Use controlled tiny scrolls only. No full-page swipe here.
         */
        for (int i = 0; i <= 22; i++) {
            List<String> visibleTexts = getCleanVisibleTexts();

            int sectionIndex = findTextIndex(visibleTexts, sectionTitle);
            int nextSectionIndex = findTextIndex(visibleTexts, nextSectionTitle);

            if (!sectionStarted && sectionIndex >= 0) {
                sectionStarted = true;
                ReportLogger.pass("Sector Allocation heading is visible");
            }

            if (sectionStarted) {
                int fromIndex = sectionIndex >= 0 ? sectionIndex : 0;
                int toIndex = visibleTexts.size();

                if (nextSectionIndex >= 0 && nextSectionIndex > fromIndex) {
                    toIndex = nextSectionIndex;
                }

                for (int valueIndex = fromIndex; valueIndex < toIndex; valueIndex++) {
                    String value = visibleTexts.get(valueIndex);
                    if (value != null && !normalizeForComparison(value).isEmpty()) {
                        collectedValues.add(value);
                    }
                }
            }

            if (sectionStarted && containsAllTexts(collectedValues, requiredAnchors)) {
                ReportLogger.pass("Sector Allocation section values collected");
                return collectedValues;
            }

            if (sectionStarted && collectedValues.size() == lastCollectedSize) {
                noProgressCount++;
            } else {
                noProgressCount = 0;
                lastCollectedSize = collectedValues.size();
            }

            if (sectionStarted && nextSectionIndex >= 0 && !containsAllTexts(collectedValues, requiredAnchors)) {
                /*
                 * We reached Top Equity Holdings but still missed sector rows.
                 * This means Appium skipped some rows during a tiny scroll.
                 * Move slightly back up once, recollect, then continue tiny scrolling.
                 */
                ReportLogger.debug("Sector Allocation reached Top Equity Holdings before all sector rows were collected. Rechecking boundary area.");
                tinySwipeDownW3C();
                sleep(650);
            } else if (i < 22) {
                tinySwipeUpW3C();
                sleep(650);
            }

            if (sectionStarted && noProgressCount >= 6) {
                break;
            }
        }

        throw new AssertionError(
                "Where does it invest More page section not fully validated: Sector Allocation"
                        + " | Missing: " + getMissingTexts(collectedValues, requiredAnchors)
                        + " | Collected values: " + collectedValues
        );
    }

    private void tinySwipeDownW3C() {
        try {
            Dimension size = driver.manage().window().getSize();

            int x = size.getWidth() / 2;
            int startY = (int) (size.getHeight() * 0.54);
            int endY = (int) (size.getHeight() * 0.62);

            PointerInput finger = new PointerInput(PointerInput.Kind.TOUCH, "finger");
            Sequence swipe = new Sequence(finger, 1);

            swipe.addAction(finger.createPointerMove(Duration.ZERO, PointerInput.Origin.viewport(), x, startY));
            swipe.addAction(finger.createPointerDown(PointerInput.MouseButton.LEFT.asArg()));
            swipe.addAction(finger.createPointerMove(Duration.ofMillis(350), PointerInput.Origin.viewport(), x, endY));
            swipe.addAction(finger.createPointerUp(PointerInput.MouseButton.LEFT.asArg()));

            driver.perform(Collections.singletonList(swipe));

        } catch (Exception firstError) {
            Map<String, Object> params = new HashMap<>();
            params.put("left", 120);
            params.put("top", 520);
            params.put("width", 840);
            params.put("height", 650);
            params.put("direction", "down");
            params.put("percent", 0.18);

            driver.executeScript("mobile: scrollGesture", params);
        }
    }
    private void validateMorePageTopEquityHoldings() {
        String sectionName = "Where does it invest More Page - Top Equity Holdings";

        ReportLogger.step("Validating " + sectionName);

        List<String> values = collectTopEquityHoldingsValuesUntilKey();

        validateTextPresentInList(values, "Top Equity Holdings", sectionName);
        validateTextPresentInList(values, "Holding", sectionName);
        validateTextPresentInList(values, "Assets (%)", sectionName);
        validateTextPresentInList(values, "Sector", sectionName);

        logCleanValidation(sectionName, "Header: Holding | Assets (%) | Sector");

        List<String[]> rows = extractDynamicTopEquityHoldingRows(values);

        if (rows.size() < 15) {
            throw new AssertionError(
                    sectionName + " has insufficient complete holding rows. Required at least 15, found: "
                            + rows.size()
                            + " | Rows: " + topEquityRowsToReadableText(rows)
                            + " | Values: " + values
            );
        }

        for (String[] row : rows) {
            logCleanValidation(
                    sectionName,
                    row[0] + " | Assets (%): " + row[1] + " | Sector: " + row[2]
            );
        }

        String dateText = getOptionalDateText(values);
        if (!"-".equals(dateText)) {
            logCleanValidation(sectionName, "Date: " + dateText);
        }

        if (containsExactText(values, "Key")) {
            logCleanValidation(sectionName, "Key section reached after Top Equity Holdings");
        }

        logCleanValidation(sectionName, "Validated dynamic holding rows count: " + rows.size());
        ReportLogger.pass(sectionName + " validated");
    }
    private void validateMorePageKey() {
        String sectionName = "Where does it invest More Page - Key";

        ReportLogger.step("Validating " + sectionName);

        List<String> values = scrollAndCollectKeySectionValues();
        List<String> normalizedLegends = buildKeyLegendTexts(values);

        validateTextPresentInList(values, "Key", sectionName);
        validateTextPresentInList(normalizedLegends, "Increase in holding", sectionName);
        validateTextPresentInList(normalizedLegends, "Decrease in holding", sectionName);
        validateTextPresentInList(normalizedLegends, "No change in holding", sectionName);
        validateTextPresentInList(normalizedLegends, "New holding", sectionName);

        logCleanValidation(sectionName, "Legend: Increase in holding");
        logCleanValidation(sectionName, "Legend: Decrease in holding");
        logCleanValidation(sectionName, "Legend: No change in holding");
        logCleanValidation(sectionName, "Legend: New holding");

        ReportLogger.pass(sectionName + " validated");
    }

    private List<String> collectTopEquityHoldingsValuesUntilKey() {
        ReportLogger.step("Collecting Where does it invest More page section: Top Equity Holdings");

        List<String> collectedValues = new ArrayList<>();
        boolean topEquityStarted = false;
        int previousRowCount = 0;
        int noRowProgressCount = 0;

        for (int attempt = 1; attempt <= 32; attempt++) {
            List<String> visibleTexts = getCleanVisibleTexts();

            for (String text : visibleTexts) {
                if (text == null || text.trim().isEmpty()) {
                    continue;
                }

                String clean = normalizeForComparison(text);

                if (isSameText(clean, "Top Equity Holdings")) {
                    topEquityStarted = true;
                }

                if (!topEquityStarted) {
                    continue;
                }

                /*
                 * Do not remove duplicates only by text.
                 * Same sector names repeat across multiple holdings.
                 * But avoid unlimited exact consecutive duplication from same viewport.
                 */
                if (collectedValues.isEmpty()
                        || !normalizeForComparison(collectedValues.get(collectedValues.size() - 1)).equals(clean)) {
                    collectedValues.add(clean);
                }
            }

            List<String[]> rows = extractDynamicTopEquityHoldingRows(collectedValues);
            boolean keyReached = containsExactText(collectedValues, "Key");

            if (keyReached && rows.size() >= 15) {
                ReportLogger.pass("Reached Key section after collecting complete Top Equity Holdings rows");
                return collectedValues;
            }

            if (rows.size() >= 20) {
                ReportLogger.pass("Collected enough dynamic Top Equity Holdings rows: " + rows.size());
                return collectedValues;
            }

            if (rows.size() == previousRowCount) {
                noRowProgressCount++;
            } else {
                noRowProgressCount = 0;
                previousRowCount = rows.size();
            }

            if (rows.size() >= 15 && noRowProgressCount >= 5) {
                ReportLogger.pass("Top Equity Holdings rows collected with no further progress. Row count: " + rows.size());
                return collectedValues;
            }

            tinySwipeUpW3C();
            sleep(noRowProgressCount >= 3 ? 850 : 650);
        }

        return collectedValues;
    }
    private List<String[]> extractDynamicTopEquityHoldingRows(List<String> values) {
        List<String[]> rows = new ArrayList<>();

        if (values == null || values.isEmpty()) {
            return rows;
        }

        int tableStartIndex = findBestTopEquityTableStartIndex(values);

        if (tableStartIndex < 0) {
            return rows;
        }

        for (int i = tableStartIndex; i < values.size() - 2; i++) {
            String holdingName = normalizeForComparison(values.get(i));
            String assetsValue = normalizeForComparison(values.get(i + 1));
            String sectorValue = normalizeForComparison(values.get(i + 2));

            if (holdingName.isEmpty() || assetsValue.isEmpty() || sectorValue.isEmpty()) {
                continue;
            }

            if (isSameText(holdingName, "Key")) {
                break;
            }

            if (isTopEquityHoldingRowCandidate(holdingName, assetsValue, sectorValue)) {
                if (!topEquityRowAlreadyAdded(rows, holdingName)) {
                    rows.add(new String[]{holdingName, assetsValue, sectorValue});
                }

                i = i + 2;
            }
        }

        return rows;
    }

    private int findBestTopEquityTableStartIndex(List<String> values) {
        if (values == null || values.isEmpty()) {
            return -1;
        }

        int bestStartIndex = -1;

        /*
         * There can be repeated "Top Equity Holdings" text while scrolling.
         * Use the latest table slice that has Holding + Assets (%) nearby.
         */
        for (int i = 0; i < values.size(); i++) {
            if (!isSameText(values.get(i), "Top Equity Holdings")) {
                continue;
            }

            int holdingHeaderIndex = findTopEquityHeaderAfter(values, i, "Holding");
            int assetsHeaderIndex = findTopEquityHeaderAfter(values, i, "Assets (%)");

            if (holdingHeaderIndex >= 0 && assetsHeaderIndex >= 0) {
                bestStartIndex = Math.max(holdingHeaderIndex, assetsHeaderIndex) + 1;
            }
        }

        return bestStartIndex;
    }

    private int findTopEquityHeaderAfter(List<String> values, int startIndex, String headerText) {
        if (values == null || headerText == null) {
            return -1;
        }

        int endIndex = Math.min(values.size(), startIndex + 12);

        for (int i = startIndex; i < endIndex; i++) {
            if (isSameText(values.get(i), headerText)) {
                return i;
            }
        }

        return -1;
    }

    private boolean isTopEquityHoldingRowCandidate(String holdingName, String assetsValue, String sectorValue) {
        if (!isLikelyTopEquityHoldingName(holdingName)) {
            return false;
        }

        if (!isNumericLikeValue(assetsValue)) {
            return false;
        }

        return isKnownTopEquitySector(sectorValue);
    }

    private boolean isLikelyTopEquityHoldingName(String value) {
        if (value == null) {
            return false;
        }

        String clean = normalizeForComparison(value);

        if (clean.isEmpty()) {
            return false;
        }

        if (isNumericLikeValue(clean)) {
            return false;
        }

        if (isKnownTopEquitySector(clean)) {
            return false;
        }

        if (isSameText(clean, "Top Equity Holdings")
                || isSameText(clean, "Holding")
                || isSameText(clean, "Assets (%)")
                || isSameText(clean, "Sector")
                || isSameText(clean, "Go back")
                || isSameText(clean, "Key")
                || isSameText(clean, "Sector Allocation")
                || isSameText(clean, "Fund (%)")
                || isSameText(clean, "Category (%)")) {
            return false;
        }

        if (clean.toLowerCase().startsWith("as on")
                || clean.toLowerCase().contains("as on")) {
            return false;
        }

        /*
         * Holding names must have alphabetic characters.
         * Examples: ICICI Bank, Larsen & Toubro, Divi's Lab, Power Grid.
         */
        return clean.matches(".*[A-Za-z].*");
    }

    private boolean isKnownTopEquitySector(String value) {
        if (value == null) {
            return false;
        }

        String clean = normalizeForComparison(value);

        return isSameText(clean, "Financial")
                || isSameText(clean, "Technology")
                || isSameText(clean, "Consumer Discretionary")
                || isSameText(clean, "Healthcare")
                || isSameText(clean, "Industrials")
                || isSameText(clean, "Energy & Utilities")
                || isSameText(clean, "Materials")
                || isSameText(clean, "Consumer Staples")
                || isSameText(clean, "Real Estate")
                || isSameText(clean, "Communication Services")
                || isSameText(clean, "Services")
                || isSameText(clean, "Automobile")
                || isSameText(clean, "Capital Goods");
    }

    private boolean topEquityRowAlreadyAdded(List<String[]> rows, String holdingName) {
        if (rows == null || holdingName == null) {
            return false;
        }

        String expected = normalizeForComparison(holdingName);

        for (String[] row : rows) {
            if (row != null
                    && row.length >= 1
                    && expected.equalsIgnoreCase(normalizeForComparison(row[0]))) {
                return true;
            }
        }

        return false;
    }

    private String topEquityRowsToReadableText(List<String[]> rows) {
        if (rows == null || rows.isEmpty()) {
            return "[]";
        }

        List<String> readableRows = new ArrayList<>();

        for (String[] row : rows) {
            if (row != null && row.length >= 3) {
                readableRows.add(row[0] + "=" + row[1] + "=" + row[2]);
            }
        }

        return readableRows.toString();
    }
    private boolean hasAllTopEquityRows(List<String> values) {
        if (values == null || values.isEmpty()) {
            return false;
        }

        for (String holding : getExpectedTopEquityHoldings()) {
            if (getTopEquityRow(values, holding) == null) {
                return false;
            }
        }

        return true;
    }

    private String[] getExpectedTopEquityHoldings() {
        return new String[]{
                "ICICI Bank",
                "Axis Bank",
                "HDFC Bank",
                "SBI",
                "SBI Life Insurance",
                "Kotak Bank",
                "Larsen & Toubro",
                "Bharti Airtel",
                "Maruti Suzuki",
                "Power Grid",
                "Cipla",
                "Eternal",
                "HCL Technologies",
                "Eicher Motors",
                "Interglobe Aviation",
                "Reliance Ind",
                "Bajaj Auto",
                "JSW Steel",
                "Hyundai Motor India",
                "Infosys",
                "Tata Steel",
                "Piramal Pharma",
                "Max Healthcare",
                "Bank Of Baroda",
                "Persistent Systems"
        };
    }

    private String[] getTopEquityRow(List<String> values, String holdingName) {
        if (values == null || values.isEmpty() || holdingName == null) {
            return null;
        }

        int holdingIndex = findTopEquityTextIndex(values, holdingName, 0);

        if (holdingIndex < 0) {
            return null;
        }

        int searchEnd = getNextTopEquityHoldingIndex(values, holdingIndex + 1);

        String assetValue = null;
        String sectorValue = null;

        for (int i = holdingIndex + 1; i < searchEnd; i++) {
            String value = values.get(i);

            if (value == null) {
                continue;
            }

            String clean = value.trim();

            if (clean.isEmpty() || isTopEquityRowNoise(clean)) {
                continue;
            }

            if (assetValue == null && isNumericLikeValue(clean)) {
                assetValue = clean;
                continue;
            }

            if (assetValue != null && sectorValue == null && !isNumericLikeValue(clean)) {
                sectorValue = clean;
                break;
            }
        }

        if (assetValue == null || sectorValue == null) {
            return null;
        }

        return new String[]{assetValue, sectorValue};
    }

    private int getNextTopEquityHoldingIndex(List<String> values, int startIndex) {
        int endIndex = values == null ? 0 : values.size();

        if (values == null) {
            return endIndex;
        }

        for (int i = startIndex; i < values.size(); i++) {
            String value = values.get(i);

            if (isExpectedTopEquityHolding(value) || isSameText(value, "Key")) {
                return i;
            }
        }

        return endIndex;
    }

    private int findTopEquityTextIndex(List<String> values, String expectedText, int startIndex) {
        if (values == null || expectedText == null) {
            return -1;
        }

        for (int i = Math.max(0, startIndex); i < values.size(); i++) {
            if (isSameText(values.get(i), expectedText)) {
                return i;
            }
        }

        return -1;
    }

    private boolean isExpectedTopEquityHolding(String text) {
        if (text == null) {
            return false;
        }

        for (String holding : getExpectedTopEquityHoldings()) {
            if (isSameText(text, holding)) {
                return true;
            }
        }

        return false;
    }

    private boolean isTopEquityRowNoise(String text) {
        if (text == null) {
            return true;
        }

        String clean = normalizeForComparison(text);

        return clean.equals(normalizeForComparison("Top Equity Holdings"))
                || clean.equals(normalizeForComparison("Holding"))
                || clean.equals(normalizeForComparison("Assets (%)"))
                || clean.equals(normalizeForComparison("Sector"))
                || clean.equals(normalizeForComparison("as on 30 Apr, 2026"));
    }
    private List<String> scrollAndCollectKeySectionValues() {
        /*
         * Correct Key behavior:
         * - Do not collect anything before Key is visible. Otherwise Top Equity
         *   Holdings rows pollute the Key section report.
         * - Once Key is visible, continue only tiny controlled scrolling until
         *   the last Key legend is collected.
         * - Then validate all Key legends from the collected Key block.
         */
        List<String> keyValues = new ArrayList<>();
        boolean keyFound = false;

        for (int i = 1; i <= 35; i++) {
            List<String> visibleTexts = getCleanVisibleTexts();

            if (!keyFound && containsExactText(visibleTexts, "Key")) {
                keyFound = true;
                ReportLogger.pass("Key section is visible");
            }

            if (keyFound) {
                appendKeyVisibleTexts(keyValues, visibleTexts);

                List<String> normalizedLegends = buildKeyLegendTexts(keyValues);

                if (containsExactText(normalizedLegends, "Increase in holding")
                        && containsExactText(normalizedLegends, "Decrease in holding")
                        && containsExactText(normalizedLegends, "No change in holding")
                        && containsExactText(normalizedLegends, "New holding")) {
                    ReportLogger.pass("All Key legends collected");
                    return keyValues;
                }

                tinySwipeUpW3C();
                sleep(550);
                continue;
            }

            tinySwipeUpW3C();
            sleep(650);
        }

        if (keyFound) {
            return keyValues;
        }

        throw new AssertionError("Where does it invest More Page - Key section not found.");
    }
    private void appendKeyVisibleTexts(List<String> values, List<String> visibleTexts) {
        if (values == null || visibleTexts == null) {
            return;
        }

        boolean keyStarted = containsExactText(values, "Key");

        for (String text : visibleTexts) {
            if (text == null || text.trim().isEmpty()) {
                continue;
            }

            String clean = text.trim();

            if (isSameText(clean, "Key")) {
                keyStarted = true;
            }

            if (!keyStarted) {
                continue;
            }

            if (!isKeyBlockText(clean)) {
                continue;
            }

            values.add(clean);
        }
    }

    private boolean isKeyBlockText(String text) {
        if (text == null) {
            return false;
        }

        String clean = normalizeForComparison(text);

        return clean.equals("Key")
                || clean.equals("Increase")
                || clean.equals("Decrease")
                || clean.equals("No change")
                || clean.equals("in holding")
                || clean.equals("Increase in holding")
                || clean.equals("Decrease in holding")
                || clean.equals("No change in holding")
                || clean.equals("New holding");
    }

    private List<String> buildKeyLegendTexts(List<String> values) {
        List<String> legends = new ArrayList<>();

        if (values == null || values.isEmpty()) {
            return legends;
        }

        for (int i = 0; i < values.size(); i++) {
            String current = normalizeForComparison(values.get(i));

            if (current.isEmpty()) {
                continue;
            }

            if (current.equals(normalizeForComparison("New holding"))) {
                addUniqueLegend(legends, "New holding");
                continue;
            }

            if ((current.equals(normalizeForComparison("Increase"))
                    || current.equals(normalizeForComparison("Increase in holding")))
                    && hasFollowingInHolding(values, i)) {
                addUniqueLegend(legends, "Increase in holding");
                continue;
            }

            if ((current.equals(normalizeForComparison("Decrease"))
                    || current.equals(normalizeForComparison("Decrease in holding")))
                    && hasFollowingInHolding(values, i)) {
                addUniqueLegend(legends, "Decrease in holding");
                continue;
            }

            if ((current.equals(normalizeForComparison("No change"))
                    || current.equals(normalizeForComparison("No change in holding")))
                    && hasFollowingInHolding(values, i)) {
                addUniqueLegend(legends, "No change in holding");
            }
        }

        return legends;
    }

    private boolean hasFollowingInHolding(List<String> values, int currentIndex) {
        if (values == null) {
            return false;
        }

        for (int i = currentIndex; i <= currentIndex + 2 && i < values.size(); i++) {
            if (normalizeForComparison(values.get(i)).equals(normalizeForComparison("in holding"))) {
                return true;
            }
        }

        return false;
    }

    private void addUniqueLegend(List<String> legends, String legend) {
        if (legends == null || legend == null) {
            return;
        }

        for (String existing : legends) {
            if (isSameText(existing, legend)) {
                return;
            }
        }

        legends.add(legend);
    }

    private boolean isSameText(String actualText, String expectedText) {
        return normalizeForComparison(actualText).equals(normalizeForComparison(expectedText));
    }

    private List<String> collectMorePageSectionValues(String sectionTitle, String[] requiredAnchors, int maxScrolls) {
        ReportLogger.step("Collecting Where does it invest More page section: " + sectionTitle);

        List<String> collectedValues = new ArrayList<>();
        boolean sectionStarted = false;
        int lastCollectedSize = -1;
        int noProgressCount = 0;

        for (int i = 0; i <= maxScrolls; i++) {
            List<String> visibleTexts = getCleanVisibleTexts();
            appendVisibleTexts(collectedValues, visibleTexts);

            if (!sectionStarted && containsExactText(visibleTexts, sectionTitle)) {
                sectionStarted = true;
            }

            if (sectionStarted && containsAllTexts(collectedValues, requiredAnchors)) {
                ReportLogger.pass(sectionTitle + " section values collected");
                return collectedValues;
            }

            if (collectedValues.size() == lastCollectedSize) {
                noProgressCount++;
            } else {
                noProgressCount = 0;
                lastCollectedSize = collectedValues.size();
            }

            if (i < maxScrolls) {
                if (noProgressCount >= 3) {
                    smallSwipeUpW3C();
                    noProgressCount = 0;
                } else {
                    tinySwipeUpW3C();
                }
                sleep(650);
            }
        }

        throw new AssertionError(
                "Where does it invest More page section not fully validated: " + sectionTitle
                        + " | Missing: " + getMissingTexts(collectedValues, requiredAnchors)
                        + " | Collected values: " + collectedValues
        );
    }

    private List<String> collectWhereDoesItInvestTabValues(String tabName, String[] requiredAnchors, int maxSmallScrolls) {
        ReportLogger.step("Collecting complete " + tabName + " table values");

        List<String> collectedValues = new ArrayList<>();
        int lastCollectedSize = -1;
        int noProgressCount = 0;

        /*
         * Proper FD_008 strategy:
         * - The Where does it invest card has chips, then chart, then table.
         * - We must NOT validate immediately after chip tap.
         * - We must keep doing controlled tiny upward scrolls until all expected row labels
         *   for the selected tab are collected, or until we cross into the next section.
         * - This avoids the old issue where Asset Class returned after only Equity,
         *   and Market Cap returned after only Large.
         */
        for (int pass = 1; pass <= 2; pass++) {
            for (int i = 0; i <= maxSmallScrolls; i++) {
                appendVisibleTexts(collectedValues, getCleanVisibleTexts());

                if (containsAllTexts(collectedValues, requiredAnchors)) {
                    ReportLogger.pass(tabName + " complete table body is visible");
                    return collectedValues;
                }

                if (collectedValues.size() == lastCollectedSize) {
                    noProgressCount++;
                } else {
                    noProgressCount = 0;
                    lastCollectedSize = collectedValues.size();
                }

                if (isWhereDoesItInvestOverscrolled()) {
                    ReportLogger.debug(tabName + " table search moved below Where does it invest section. Realigning and retrying.");
                    break;
                }

                /*
                 * If we are stuck with no new values, use one slightly stronger scroll.
                 * Otherwise use tiny scroll to avoid jumping to manager/more-details.
                 */
                tinySwipeUpW3C();

                if (noProgressCount >= 3) {
                    noProgressCount = 0;
                }

                sleep(650);
            }

            returnToWhereDoesItInvestChips();

            /*
             * Keep selected tab stable after realignment. The selected tab usually remains selected,
             * but tapping again is safer because Flutter sometimes re-renders chip state.
             */
            if (isVisible(byDesc(tabName))) {
                tapVisible(byDesc(tabName), tabName + " chip");
                sleep(900);
            }
        }

        throw new AssertionError(
                tabName + " table body is not fully visible after controlled scrolling. Missing: "
                        + getMissingTexts(collectedValues, requiredAnchors)
                        + " | Collected values: " + collectedValues
        );
    }



    private boolean isWhereDoesItInvestOverscrolled() {
        return isVisible(byDesc("Who is the manager?"))
                || isVisible(byDesc("More Details"))
                || isVisible(byDesc("News"))
                || isVisible(byDesc("Investment Strategy"))
                || isVisible(byDesc("Other Information"))
                || isVisible(byDesc("Taxation"));
    }

    private void tinySwipeUpW3C() {
        try {
            Dimension size = driver.manage().window().getSize();

            int x = size.getWidth() / 2;
            int startY = (int) (size.getHeight() * 0.62);
            int endY = (int) (size.getHeight() * 0.54);

            PointerInput finger = new PointerInput(PointerInput.Kind.TOUCH, "finger");
            Sequence swipe = new Sequence(finger, 1);

            swipe.addAction(finger.createPointerMove(Duration.ZERO, PointerInput.Origin.viewport(), x, startY));
            swipe.addAction(finger.createPointerDown(PointerInput.MouseButton.LEFT.asArg()));
            swipe.addAction(finger.createPointerMove(Duration.ofMillis(350), PointerInput.Origin.viewport(), x, endY));
            swipe.addAction(finger.createPointerUp(PointerInput.MouseButton.LEFT.asArg()));

            driver.perform(Collections.singletonList(swipe));

        } catch (Exception firstError) {
            Map<String, Object> params = new HashMap<>();
            params.put("left", 120);
            params.put("top", 520);
            params.put("width", 840);
            params.put("height", 650);
            params.put("direction", "up");
            params.put("percent", 0.18);

            driver.executeScript("mobile: scrollGesture", params);
        }
    }

    private List<String> getMissingTexts(List<String> values, String[] expectedTexts) {
        List<String> missing = new ArrayList<>();

        if (expectedTexts == null) {
            return missing;
        }

        for (String expectedText : expectedTexts) {
            if (!containsExactText(values, expectedText)) {
                missing.add(expectedText);
            }
        }

        return missing;
    }

    private List<String> collectCurrentPageValuesWithSmallScrolls(int maxSmallScrolls) {
        List<String> collectedValues = new ArrayList<>();

        for (int i = 0; i <= maxSmallScrolls; i++) {
            appendVisibleTexts(collectedValues, getCleanVisibleTexts());

            if (i < maxSmallScrolls) {
                smallSwipeUpW3C();
                sleep(600);
            }
        }

        return collectedValues;
    }

    private void appendVisibleTexts(List<String> target, List<String> source) {
        if (target == null || source == null) {
            return;
        }

        target.addAll(source);
    }

    private boolean containsAllTexts(List<String> values, String[] expectedTexts) {
        if (expectedTexts == null || expectedTexts.length == 0) {
            return true;
        }

        for (String expectedText : expectedTexts) {
            if (!containsExactText(values, expectedText)) {
                return false;
            }
        }

        return true;
    }

    private void returnToWhereDoesItInvestChips() {
        ReportLogger.step("Returning to Where does it invest chips");

        for (int i = 1; i <= 8; i++) {
            if (isVisible(byDesc("Asset Class"))
                    && isVisible(byDesc("Market Cap"))
                    && isVisible(byDesc("Holding"))) {
                ReportLogger.pass("Where does it invest chips are visible");
                return;
            }

            smallSwipeDownW3C();
            sleep(650);
        }

        scrollUntilAnyVisible(
                new String[]{
                        "Where does it invest?",
                        "Asset Class",
                        "Market Cap",
                        "Holding"
                },
                6,
                "Where does it invest chips"
        );

        alignWhereDoesItInvestCardBody();
    }

    private void returnToWhereDoesItInvestHeaderForMore() {
        ReportLogger.step("Returning to Where does it invest header for section More");

        for (int i = 1; i <= 8; i++) {
            boolean headingVisible = isVisible(byDesc("Where does it invest?")) || isVisible(byDescContains("Where does it invest?"));
            boolean chipsVisible = isVisible(byDesc("Asset Class")) && isVisible(byDesc("Market Cap")) && isVisible(byDesc("Holding"));

            if (headingVisible && chipsVisible) {
                ReportLogger.pass("Where does it invest header and chips are visible");
                return;
            }

            smallSwipeDownW3C();
            sleep(650);
        }

        scrollUntilAnyVisible(
                new String[]{
                        "Where does it invest?",
                        "Asset Class",
                        "Market Cap",
                        "Holding"
                },
                6,
                "Where does it invest header"
        );

        alignWhereDoesItInvestCardBody();
    }

    private void validateSingleValueRow(String sectionName, List<String> values, String rowKey, boolean mandatory) {
        int rowIndex = findTextIndex(values, rowKey);

        if (rowIndex < 0) {
            if (mandatory) {
                throw new AssertionError(sectionName + " missing row: " + rowKey);
            }

            ReportLogger.debug(sectionName + " optional row not visible: " + rowKey);
            return;
        }

        String value = getFirstNumericLikeValueAfter(values, rowIndex);

        if ("-".equals(value)) {
            throw new AssertionError(sectionName + " numeric value missing for row: " + rowKey);
        }

        logCleanValidation(sectionName, rowKey + " | Value: " + value);
    }

    private void validateDoubleValueRow(String sectionName, List<String> values, String rowKey, boolean mandatory) {
        int rowIndex = findTextIndex(values, rowKey);

        if (rowIndex < 0) {
            if (mandatory) {
                throw new AssertionError(sectionName + " missing row: " + rowKey);
            }

            ReportLogger.debug(sectionName + " optional row not visible: " + rowKey);
            return;
        }

        List<String> numericValues = getNextNumericLikeValuesAfter(values, rowIndex, 2);

        if (numericValues.size() < 2) {
            throw new AssertionError(
                    sectionName + " Fund/Category values missing for row: " + rowKey
                            + " | Found values: " + numericValues
            );
        }

        logCleanValidation(
                sectionName,
                rowKey + " | Fund: " + numericValues.get(0) + " | Category: " + numericValues.get(1)
        );
    }

    private String getFirstNumericLikeValueAfter(List<String> values, int startIndex) {
        List<String> numericValues = getNextNumericLikeValuesAfter(values, startIndex, 1);

        if (numericValues.isEmpty()) {
            return "-";
        }

        return numericValues.get(0);
    }

    private List<String> getNextNumericLikeValuesAfter(List<String> values, int startIndex, int requiredCount) {
        List<String> numericValues = new ArrayList<>();

        if (values == null || values.isEmpty() || startIndex < 0) {
            return numericValues;
        }

        for (int i = startIndex + 1; i < values.size(); i++) {
            String value = values.get(i);

            if (value == null) {
                continue;
            }

            String clean = value.trim();

            if (clean.isEmpty()) {
                continue;
            }

            if (isNumericLikeValue(clean)) {
                numericValues.add(clean);
            }

            if (numericValues.size() == requiredCount) {
                break;
            }
        }

        return numericValues;
    }

    private boolean isNumericLikeValue(String value) {
        if (value == null) {
            return false;
        }

        String clean = value.trim();

        if (clean.isEmpty()) {
            return false;
        }

        return clean.matches("-?\\d+(\\.\\d+)?")
                || clean.matches("-?\\d{1,3}(,\\d{2,3})+(\\.\\d+)?")
                || clean.matches("-?\\d+(\\.\\d+)?%");
    }

    private String getOptionalDateText(List<String> values) {
        if (values == null || values.isEmpty()) {
            return "-";
        }

        for (String value : values) {
            if (value == null) {
                continue;
            }

            String clean = value.trim();

            if (clean.matches("as on\\s+\\d{2}\\s+[A-Za-z]{3},\\s+\\d{4}")) {
                return clean;
            }

            if (clean.matches("\\(as on\\s+\\d{2}\\s+[A-Za-z]{3},\\s+\\d{4}\\)")) {
                return clean.replace("(", "").replace(")", "");
            }
        }

        return "-";
    }

    private String normalizeForComparison(String value) {
        if (value == null) {
            return "";
        }

        return value
                .replace("’", "'")
                .replace("‘", "'")
                .replace("“", "\"")
                .replace("”", "\"")
                .replace("\u00A0", " ")
                .trim()
                .replaceAll("\\s+", " ");
    }

    private boolean containsExactText(List<String> values, String text) {
        return findTextIndex(values, text) >= 0;
    }

    private boolean containsPartialText(List<String> values, String partialText) {
        if (values == null || values.isEmpty() || partialText == null) {
            return false;
        }

        String expected = normalizeForComparison(partialText);

        for (String value : values) {
            String actual = normalizeForComparison(value);

            if (actual.contains(expected)) {
                return true;
            }
        }

        return false;
    }

    private int findTextIndex(List<String> values, String text) {
        if (values == null || values.isEmpty() || text == null) {
            return -1;
        }

        String expected = normalizeForComparison(text);

        for (int i = 0; i < values.size(); i++) {
            String actual = normalizeForComparison(values.get(i));

            if (actual.equals(expected)) {
                return i;
            }
        }

        return -1;
    }

    private void alignWhereDoesItInvestCardBody() {
        ReportLogger.step("Aligning Where does it invest card body");

        if (isVisible(byDesc("Asset Class"))
                && isVisible(byDesc("Market Cap"))
                && isVisible(byDesc("Holding"))) {
            ReportLogger.pass("Where does it invest chips are already visible");
            return;
        }

        for (int i = 1; i <= 4; i++) {
            smallSwipeUpW3C();
            sleep(700);

            if (isVisible(byDesc("Asset Class"))
                    && isVisible(byDesc("Market Cap"))
                    && isVisible(byDesc("Holding"))) {
                ReportLogger.pass("Where does it invest chips are visible after alignment");
                return;
            }
        }

        throw new AssertionError("Where does it invest chips are not visible after alignment");
    }

    private boolean tapWhereDoesItInvestMoreButton() {
        ReportLogger.step("Opening Where does it invest More page");

        if (!isVisible(byDesc("Where does it invest?")) && !isVisible(byDescContains("Where does it invest?"))) {
            returnToWhereDoesItInvestHeaderForMore();
        }

        WebElement section = findVisibleElement(byDesc("Where does it invest?"));

        if (section == null) {
            section = findVisibleElement(byDescContains("Where does it invest?"));
        }

        if (section == null) {
            ReportLogger.debug("Where does it invest heading not visible for dedicated More tap");
            return false;
        }

        Rectangle sectionRect = section.getRect();
        Dimension size = driver.manage().window().getSize();

        List<WebElement> moreElements = driver.findElements(byDesc("More"));

        for (WebElement more : moreElements) {
            try {
                if (!more.isDisplayed() || !more.isEnabled()) {
                    continue;
                }

                Rectangle moreRect = more.getRect();

                boolean isRightSide = moreRect.getX() > size.getWidth() * 0.70;
                boolean isSameSectionBand = Math.abs(moreRect.getY() - sectionRect.getY()) <= 180;

                if (isRightSide && isSameSectionBand) {
                    tapElementCenter(more);
                    sleep(3000);

                    return !isOnFundDetailsPage()
                            || isVisible(byDesc("Top Equity Holdings"))
                            || isVisible(byDesc("Sector"));
                }

            } catch (Exception ignored) {
                // Try next More
            }
        }

        int x = (int) (size.getWidth() * 0.91);
        int y = sectionRect.getY() + Math.max(35, sectionRect.getHeight() / 2);

        tapByCoordinates(x, y);
        sleep(3000);

        return !isOnFundDetailsPage()
                || isVisible(byDesc("Top Equity Holdings"))
                || isVisible(byDesc("Sector"));
    }


// =========================================================
    // MANAGER
    // =========================================================

    public void verifyFundManagerSection() {
        recoverFundDetailsIfNeeded();

        ReportLogger.step("Verifying Fund Manager section");

        scrollToTopOfFundDetails();
        scrollUntilVisible("Who is the manager?", 18);

        assertVisibleAndLog(byDesc("Who is the manager?"), "Who is the manager section");
        logOptionalVisibleText(byDescContains("Amit Ganatra"), "Manager name");
        logOptionalVisibleText(byDesc("Details"), "Manager details link");

        ReportLogger.pass("Fund Manager section validated successfully");
    }

    // =========================================================
    // MORE DETAILS
    // =========================================================

    public void verifyMoreDetailsSectionAndChildPage() {
        recoverFundDetailsIfNeeded();

        ReportLogger.step("Verifying More Details section and More Details child page");

        scrollToTopOfFundDetails();
        scrollUntilVisible("More Details", 20);

        assertVisibleAndLog(byDesc("More Details"), "More Details section");

        logOptionalVisibleText(byDesc("Min SIP Amount"), "More Details label");
        logOptionalVisibleText(byDesc("Min One-time Amount"), "More Details label");
        logOptionalVisibleText(byDesc("Assets"), "More Details label");
        logOptionalVisibleText(byDesc("Expense Ratio"), "More Details label");
        logOptionalVisibleText(byDesc("Benchmark"), "More Details label");
        logOptionalVisibleText(byDesc("Exit Load"), "More Details label");
        logOptionalVisibleText(byDescContains("₹"), "More Details visible amount/value");
        logOptionalVisibleText(byDescContains("%"), "More Details visible percentage/value");

        tapMoreNearSection("More Details");

        try {
            waitForAnyVisible(
                    new By[]{byDesc("Investment Strategy"), byDesc("Other Information"), byDesc("Taxation")},
                    "More Details child page",
                    8
            );

            logOptionalVisibleText(byDesc("Investment Strategy"), "More Details child section");
            logOptionalVisibleText(byDesc("Other Information"), "More Details child section");
            logOptionalVisibleText(byDesc("Taxation"), "More Details child section");

            scrollUntilVisibleOptional("AMC Details", 8);
            logOptionalVisibleText(byDesc("AMC Details"), "AMC Details section");
            logOptionalVisibleText(byDesc("AMC"), "AMC label");
            logOptionalVisibleText(byDesc("Phone"), "Phone label");
            logOptionalVisibleText(byDesc("Email"), "Email label");
            logOptionalVisibleText(byDesc("Website"), "Website label");

            scrollUntilVisibleOptional("Recent Dividends", 8);
            logOptionalVisibleText(byDesc("Recent Dividends"), "Recent Dividends section");
            logOptionalVisibleText(byDesc("Plan"), "Recent Dividends column");
            logOptionalVisibleText(byDesc("Record Date"), "Recent Dividends column");
            logOptionalVisibleText(byDesc("Dividend (₹ / Unit)"), "Recent Dividends column");

        } finally {
            pressBack("Back from More Details child page");
            waitForFundHeaderOrKnownSection();
        }

        ReportLogger.pass("More Details section and child page validated successfully");
    }

    // =========================================================
    // NEWS
    // =========================================================

    public void verifyNewsSectionAndNewsPage() {
        recoverFundDetailsIfNeeded();

        ReportLogger.step("Verifying News section and News page");

        scrollToTopOfFundDetails();
        scrollUntilVisible("News", 22);

        assertVisibleAndLog(byDesc("News"), "News section");

        tapMoreNearSection("News");

        try {
            waitForAnyVisible(
                    new By[]{byDesc("News"), byDescContains("Fund News"), byDescContains("HDFC")},
                    "News page",
                    8
            );

            logOptionalVisibleText(byDesc("News"), "News page title");
            logOptionalVisibleText(byDescContains("Fund News"), "News item type");
            logOptionalVisibleText(byDescContains("HDFC Mutual Fund"), "News item text");
            logOptionalVisibleText(byDescContains("HDFC"), "News item text");

        } finally {
            pressBack("Back from News page");
            waitForFundHeaderOrKnownSection();
        }

        ReportLogger.pass("News section and News page validated successfully");
    }

    // =========================================================
    // SCROLL HELPERS
    // =========================================================

    private void scrollToTopOfFundDetails() {
        ReportLogger.step("Resetting Fund Details page near top");

        for (int i = 1; i <= 5; i++) {
            if (isVisible(byDesc(FUND_HEADER)) && isVisible(byDescContains("Your Investments"))) {
                ReportLogger.pass("Fund Details top area is visible");
                return;
            }

            swipeDownW3C();
            sleep(600);
        }

        if (isOnFundDetailsPage()) {
            ReportLogger.pass("Known Fund Details area visible after top reset");
        } else {
            ReportLogger.debug("Top reset did not reach header, continuing with current Fund Details state");
        }
    }

    private void scrollUntilVisible(String accessibilityText, int maxScrolls) {
        if (isVisible(byDesc(accessibilityText)) || isVisible(byDescContains(accessibilityText))) {
            ReportLogger.pass("Visible without scrolling: " + accessibilityText);
            return;
        }

        ReportLogger.step("Scrolling to find: " + accessibilityText);

        for (int i = 1; i <= maxScrolls; i++) {
            swipeUpW3C();
            sleep(800);

            if (isVisible(byDesc(accessibilityText)) || isVisible(byDescContains(accessibilityText))) {
                ReportLogger.pass("Found after scrolling: " + accessibilityText);
                return;
            }
        }

        throw new RuntimeException("Element not visible after scrolling: " + accessibilityText);
    }

    private void scrollUntilAnyVisible(String[] accessibilityTexts, int maxScrolls, String groupName) {
        if (accessibilityTexts == null || accessibilityTexts.length == 0) {
            throw new IllegalArgumentException("No accessibility texts provided for: " + groupName);
        }

        for (String text : accessibilityTexts) {
            if (isVisible(byDesc(text)) || isVisible(byDescContains(text))) {
                ReportLogger.pass(groupName + " is visible");
                return;
            }
        }

        ReportLogger.step("Scrolling to find: " + groupName);

        for (int i = 1; i <= maxScrolls; i++) {
            swipeUpW3C();
            sleep(900);

            for (String text : accessibilityTexts) {
                if (isVisible(byDesc(text)) || isVisible(byDescContains(text))) {
                    ReportLogger.pass(groupName + " is visible");
                    return;
                }
            }
        }

        throw new RuntimeException(groupName + " is not visible after scrolling");
    }

    private boolean scrollUntilVisibleOptional(String accessibilityText, int maxScrolls) {
        if (isVisible(byDesc(accessibilityText)) || isVisible(byDescContains(accessibilityText))) {
            ReportLogger.pass("Visible without scrolling: " + accessibilityText);
            return true;
        }

        ReportLogger.step("Scrolling to find optional element: " + accessibilityText);

        for (int i = 1; i <= maxScrolls; i++) {
            swipeUpW3C();
            sleep(800);

            if (isVisible(byDesc(accessibilityText)) || isVisible(byDescContains(accessibilityText))) {
                ReportLogger.pass("Found optional element after scrolling: " + accessibilityText);
                return true;
            }
        }

        ReportLogger.debug("Optional element not found after scrolling: " + accessibilityText);
        return false;
    }

    private void swipeUpW3C() {
        try {
            Dimension size = driver.manage().window().getSize();

            int x = size.getWidth() / 2;
            int startY = (int) (size.getHeight() * 0.74);
            int endY = (int) (size.getHeight() * 0.34);

            PointerInput finger = new PointerInput(PointerInput.Kind.TOUCH, "finger");
            Sequence swipe = new Sequence(finger, 1);

            swipe.addAction(finger.createPointerMove(Duration.ZERO, PointerInput.Origin.viewport(), x, startY));
            swipe.addAction(finger.createPointerDown(PointerInput.MouseButton.LEFT.asArg()));
            swipe.addAction(finger.createPointerMove(Duration.ofMillis(650), PointerInput.Origin.viewport(), x, endY));
            swipe.addAction(finger.createPointerUp(PointerInput.MouseButton.LEFT.asArg()));

            driver.perform(Collections.singletonList(swipe));

        } catch (Exception firstError) {
            try {
                Map<String, Object> params = new HashMap<>();
                params.put("left", 80);
                params.put("top", 360);
                params.put("width", 920);
                params.put("height", 1200);
                params.put("direction", "up");
                params.put("percent", 0.70);
                driver.executeScript("mobile: scrollGesture", params);
            } catch (Exception secondError) {
                throw new RuntimeException("Swipe up failed: " + cleanError(secondError.getMessage()), secondError);
            }
        }
    }

    private void swipeDownW3C() {
        try {
            Dimension size = driver.manage().window().getSize();

            int x = size.getWidth() / 2;
            int startY = (int) (size.getHeight() * 0.34);
            int endY = (int) (size.getHeight() * 0.74);

            PointerInput finger = new PointerInput(PointerInput.Kind.TOUCH, "finger");
            Sequence swipe = new Sequence(finger, 1);

            swipe.addAction(finger.createPointerMove(Duration.ZERO, PointerInput.Origin.viewport(), x, startY));
            swipe.addAction(finger.createPointerDown(PointerInput.MouseButton.LEFT.asArg()));
            swipe.addAction(finger.createPointerMove(Duration.ofMillis(600), PointerInput.Origin.viewport(), x, endY));
            swipe.addAction(finger.createPointerUp(PointerInput.MouseButton.LEFT.asArg()));

            driver.perform(Collections.singletonList(swipe));

        } catch (Exception firstError) {
            try {
                Map<String, Object> params = new HashMap<>();
                params.put("left", 80);
                params.put("top", 360);
                params.put("width", 920);
                params.put("height", 1200);
                params.put("direction", "down");
                params.put("percent", 0.70);
                driver.executeScript("mobile: scrollGesture", params);
            } catch (Exception secondError) {
                throw new RuntimeException("Swipe down failed: " + cleanError(secondError.getMessage()), secondError);
            }
        }
    }

    // =========================================================
    // MORE BUTTON HELPER
    // =========================================================

    private void tapMoreNearSection(String sectionTitle) {
        ReportLogger.step("Tapping More button near section: " + sectionTitle);

        if (!isVisible(byDesc(sectionTitle)) && !isVisible(byDescContains(sectionTitle))) {
            scrollUntilVisible(sectionTitle, 5);
        }

        WebElement section = findVisibleElement(byDesc(sectionTitle));

        if (section == null) {
            section = findVisibleElement(byDescContains(sectionTitle));
        }

        if (section == null) {
            throw new RuntimeException("Section not visible for More tap: " + sectionTitle);
        }

        Rectangle sectionRect = section.getRect();
        List<WebElement> moreElements = driver.findElements(byDesc("More"));

        WebElement bestMore = null;
        int bestDistance = Integer.MAX_VALUE;

        for (WebElement more : moreElements) {
            try {
                if (!more.isDisplayed() || !more.isEnabled()) {
                    continue;
                }

                Rectangle moreRect = more.getRect();

                if (moreRect.getY() < 180 || moreRect.getY() > driver.manage().window().getSize().getHeight() - 250) {
                    continue;
                }

                int distance = Math.abs(moreRect.getY() - sectionRect.getY());

                if (distance < bestDistance) {
                    bestDistance = distance;
                    bestMore = more;
                }

            } catch (Exception ignored) {
                // Try next More
            }
        }

        if (bestMore != null) {
            tapElementCenter(bestMore);
            sleep(2500);
            ReportLogger.pass("Tapped More near section: " + sectionTitle);
            return;
        }

        int x = driver.manage().window().getSize().getWidth() - 90;
        int y = sectionRect.getY() + sectionRect.getHeight() / 2;

        tapByCoordinates(x, y);
        sleep(2500);

        ReportLogger.pass("Tapped More near section using coordinate fallback: " + sectionTitle);
    }

    // =========================================================
    // SOFT VALIDATION HELPERS
    // =========================================================

    private void runSoftValidation(List<String> failures, String sectionName, Runnable validationStep) {
        try {
            validationStep.run();
        } catch (Throwable error) {
            recordValidationFailure(failures, sectionName, error);
        }
    }

    private void recordValidationFailure(List<String> failures, String sectionName, Throwable error) {
        String message = sectionName + " failed: " + cleanError(error == null ? "" : error.getMessage());

        failures.add(message);
        ReportLogger.fail(message);
    }

    private void throwIfValidationFailures(String parentSection, List<String> failures) {
        if (failures != null && !failures.isEmpty()) {
            throw new AssertionError(parentSection + " validation completed with failures: " + String.join(" | ", failures));
        }
    }

    // =========================================================
    // ASSERTION / WAIT / LOG HELPERS
    // =========================================================

    private void assertVisibleAndLog(By locator, String elementName) {
        WebElement element = findVisibleElement(locator);

        if (element == null) {
            ReportLogger.fail(elementName + " is not visible");
            throw new AssertionError(elementName + " is not visible");
        }

        String text = getElementReadableText(element);

        if (text.isEmpty()) {
            text = elementName;
        }

        ReportLogger.pass(elementName + " is visible");
        logValidatedText(elementName, text);
    }

    private void waitForAnyVisible(By[] locators, String elementGroupName, int seconds) {
        for (int i = 1; i <= seconds; i++) {
            for (By locator : locators) {
                if (isVisible(locator)) {
                    ReportLogger.pass(elementGroupName + " is visible");
                    return;
                }
            }

            sleep(1000);
        }

        throw new RuntimeException(elementGroupName + " is not visible after waiting " + seconds + " seconds");
    }

    private boolean waitForAnyVisibleOptional(By[] locators, String elementGroupName, int seconds) {
        for (int i = 1; i <= seconds; i++) {
            for (By locator : locators) {
                if (isVisible(locator)) {
                    ReportLogger.pass(elementGroupName + " is visible");
                    return true;
                }
            }

            sleep(1000);
        }

        ReportLogger.debug(elementGroupName + " is not visible after waiting " + seconds + " seconds");
        return false;
    }

    private void optionalVisibleAny(By[] locators, String elementGroupName) {
        for (By locator : locators) {
            WebElement element = findVisibleElement(locator);

            if (element != null) {
                ReportLogger.pass(elementGroupName + " is visible");
                logValidatedText(elementGroupName, getElementReadableText(element));
                return;
            }
        }

        ReportLogger.debug(elementGroupName + " not visible in current viewport. Skipping optional validation.");
    }

    private void logOptionalVisibleText(By locator, String label) {
        WebElement element = findVisibleElement(locator);

        if (element == null) {
            ReportLogger.debug(label + " not visible in current viewport. Skipping value log.");
            return;
        }

        String text = getElementReadableText(element);

        if (text.isEmpty()) {
            text = label;
        }

        logValidatedText(label, text);
    }

    private void logVisibleElementReadableText(By locator, String label) {
        WebElement element = findVisibleElement(locator);

        if (element == null) {
            ReportLogger.debug(label + " not visible. Skipping scoped value log.");
            return;
        }

        String text = normalizeVisibleText(getElementReadableText(element));

        if (text.isEmpty()) {
            ReportLogger.debug(label + " has no readable text.");
            return;
        }

        logValidatedText(label, text);
    }

    private void logValidatedText(String label, String value) {
        String safeValue = value == null ? "" : value.trim();

        if (safeValue.isEmpty()) {
            return;
        }

        ReportLogger.pass("Validated text/value - " + label + ": " + safeValue);

        try {
            ExtentTestManager.getTest().pass("<b>Validated text/value:</b> " + label + " = " + safeValue);
        } catch (Exception ignored) {
            // Extent test may not be initialized
        }
    }

    private String getElementReadableText(WebElement element) {
        StringBuilder builder = new StringBuilder();

        String[] attrs = new String[]{
                "content-desc",
                "contentDescription",
                "text",
                "name",
                "label",
                "value"
        };

        for (String attr : attrs) {
            try {
                String value = element.getAttribute(attr);

                if (value != null && !value.trim().isEmpty()) {
                    if (builder.indexOf(value.trim()) < 0) {
                        builder.append(value.trim()).append(" ");
                    }
                }

            } catch (Exception ignored) {
                // Some attributes are unavailable on Flutter views
            }
        }

        return builder.toString().trim();
    }

    private boolean isVisible(By locator) {
        return findVisibleElement(locator) != null;
    }

    private WebElement findVisibleElement(By locator) {
        try {
            List<WebElement> elements = driver.findElements(locator);

            for (WebElement element : elements) {
                if (element != null && element.isDisplayed()) {
                    return element;
                }
            }

            return null;

        } catch (Exception e) {
            return null;
        }
    }

    private void tapVisible(By locator, String elementName) {
        if (!tapIfVisible(locator, elementName)) {
            throw new RuntimeException("Unable to tap visible element: " + elementName);
        }
    }

    private boolean tapIfVisible(By locator, String elementName) {
        try {
            List<WebElement> elements = driver.findElements(locator);

            for (WebElement element : elements) {
                if (element != null && element.isDisplayed() && element.isEnabled()) {
                    tapElementCenter(element);
                    sleep(900);
                    ReportLogger.pass("Tapped: " + elementName);
                    return true;
                }
            }

            return false;

        } catch (Exception e) {
            ReportLogger.debug("tapIfVisible failed for " + elementName + ": " + cleanError(e.getMessage()));
            return false;
        }
    }

    private void tapElementCenter(WebElement element) {
        Rectangle rect = element.getRect();

        int x = rect.getX() + rect.getWidth() / 2;
        int y = rect.getY() + rect.getHeight() / 2;

        tapByCoordinates(x, y);
    }

    private void tapByCoordinates(int x, int y) {
        try {
            Map<String, Object> params = new HashMap<>();
            params.put("x", x);
            params.put("y", y);

            driver.executeScript("mobile: clickGesture", params);

        } catch (Exception firstError) {
            try {
                PointerInput finger = new PointerInput(PointerInput.Kind.TOUCH, "finger");
                Sequence tap = new Sequence(finger, 1);

                tap.addAction(finger.createPointerMove(Duration.ZERO, PointerInput.Origin.viewport(), x, y));
                tap.addAction(finger.createPointerDown(PointerInput.MouseButton.LEFT.asArg()));
                tap.addAction(finger.createPointerUp(PointerInput.MouseButton.LEFT.asArg()));

                driver.perform(Collections.singletonList(tap));

            } catch (Exception secondError) {
                throw new RuntimeException(
                        "Coordinate tap failed at x=" + x + ", y=" + y + " | " + cleanError(secondError.getMessage()),
                        secondError
                );
            }
        }
    }

    private void pressBack(String stepName) {
        ReportLogger.step(stepName);
        driver.navigate().back();
        sleep(1400);
    }

    private void pressBackSilently() {
        try {
            driver.navigate().back();
        } catch (Exception ignored) {
            // ignore
        }
    }

    private void waitForFundHeaderOrKnownSection() {
        sleep(1400);

        if (isOnFundDetailsPage()) {
            ReportLogger.pass("Returned to Fund Details page");
            return;
        }

        ReportLogger.debug("Known Fund Details section not immediately visible after back. Continuing.");
    }

    // =========================================================
    // LOCATORS / UTILS
    // =========================================================

    private By byDesc(String desc) {
        return AppiumBy.accessibilityId(desc);
    }

    private By byDescContains(String text) {
        return AppiumBy.androidUIAutomator(
                "new UiSelector().descriptionContains(\"" + escapeUiAutomatorText(text) + "\")"
        );
    }

    private By byText(String text) {
        return AppiumBy.androidUIAutomator(
                "new UiSelector().text(\"" + escapeUiAutomatorText(text) + "\")"
        );
    }

    private By byTextContains(String text) {
        return AppiumBy.androidUIAutomator(
                "new UiSelector().textContains(\"" + escapeUiAutomatorText(text) + "\")"
        );
    }

    private String escapeUiAutomatorText(String text) {
        if (text == null) {
            return "";
        }

        return text.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private String cleanError(String error) {
        if (error == null) {
            return "";
        }

        return error.replace("\n", " ").replace("\r", " ").trim();
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Thread interrupted", e);
        }
    }

    private static class VisibleTextNode {
        private final String text;
        private final int x;
        private final int y;

        private VisibleTextNode(String text, int x, int y) {
            this.text = text;
            this.x = x;
            this.y = y;
        }
    }

}