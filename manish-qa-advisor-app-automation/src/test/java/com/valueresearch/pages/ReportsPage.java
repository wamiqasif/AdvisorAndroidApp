package com.valueresearch.pages;

import com.valueresearch.utils.AuthHelper;
import com.valueresearch.utils.ReportLogger;
import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;
import org.openqa.selenium.By;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.Rectangle;
import org.openqa.selenium.WebElement;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Portfolio -> Reports module page object.
 *
 * Locator strategy:
 * 1. Accessibility IDs/content descriptions from Appium Inspector.
 * 2. Description/text contains locators with section-based Y-position mapping.
 * 3. Bounded visible-element scans.
 * 4. Coordinate tap only as a final fallback for opening the Portfolio menu.
 *
 * This page object intentionally avoids:
 * - absolute XPath
 * - fixed Download instance indexes
 * - hardcoded investor names
 * - hardcoded financial years or report dates
 */
public class ReportsPage {

    private static final String ADVISOR_PACKAGE = "com.valueresearch.advisor";
    private static final String NAVIGATION_BUILD = "REPORTS_NAV_V5_20260731_1220";
    private static final ZoneId IST = ZoneId.of("Asia/Kolkata");

    private static final By PORTFOLIO = AppiumBy.accessibilityId("Portfolio");

    // Exact locator supplied from Appium Inspector for the top-right three-dot menu
    // on the Portfolio Summary screen.
    private static final By PORTFOLIO_OPTIONS_MENU = AppiumBy.androidUIAutomator(
            "new UiSelector().className(\"android.widget.ImageView\").instance(1)"
    );

    private static final By REPORTS = AppiumBy.accessibilityId("Reports");
    private static final By TAX_REPORT = AppiumBy.accessibilityId("Tax Report");
    private static final By HOLDINGS_STATEMENT = AppiumBy.accessibilityId("Holdings Statement");
    private static final By TRANSACTION_HISTORY = AppiumBy.accessibilityId("Transaction History");
    private static final By FINANCIAL_YEAR = AppiumBy.accessibilityId("Financial Year");
    private static final By AS_OF = AppiumBy.accessibilityId("As of");
    private static final By DOWNLOAD = AppiumBy.accessibilityId("Download");
    private static final By SCRIM = AppiumBy.accessibilityId("Scrim");

    private static final Pattern FINANCIAL_YEAR_PATTERN = Pattern.compile(
            "(?i)^Financial\\s+Year\\s+(\\d{4})-(\\d{2})$"
    );

    private static final Pattern DISPLAY_DATE_PATTERN = Pattern.compile(
            "^\\d{1,2}-[A-Za-z]{3}-\\d{4}$"
    );

    private static final DateTimeFormatter DISPLAY_DATE_FORMATTER =
            new DateTimeFormatterBuilder()
                    .parseCaseInsensitive()
                    .appendPattern("d-MMM-uuuu")
                    .toFormatter(Locale.ENGLISH);

    private final AndroidDriver driver;

    public ReportsPage(AndroidDriver driver) {
        this.driver = driver;
    }

    private void logLoadedClassLocation() {
        try {
            String location = ReportsPage.class
                    .getProtectionDomain()
                    .getCodeSource()
                    .getLocation()
                    .toString();
            ReportLogger.step("[" + NAVIGATION_BUILD + "] Loaded ReportsPage class from: " + location);
        } catch (Exception ignored) {
            ReportLogger.step("[" + NAVIGATION_BUILD + "] Loaded ReportsPage class location unavailable");
        }
    }

    // =========================================================
    // Navigation
    // =========================================================

    public void ensureReportsPageOpen() {
        ReportLogger.step("[" + NAVIGATION_BUILD + "] Ensuring Portfolio Reports page is open");
        logLoadedClassLocation();

        if (isReportsPageActive()) {
            closeUnexpectedOverlayIfPresent();
            if (isReportsPageActive()) {
                ReportLogger.pass("Reports page is already active");
                return;
            }
        }

        ensureAdvisorAppActive();

        ReportLogger.step("Checking Advisor login/session");
        new AuthHelper(driver).ensureLoggedIn();
        ReportLogger.pass("Advisor login/session confirmed");

        tapBottomPortfolioTab();
        waitForPortfolioLanding();

        if (!isVisible(REPORTS)) {
            openPortfolioMenu();
        }

        WebElement reportsMenu = waitForVisible(REPORTS, 12);
        safeClick(reportsMenu);

        waitForReportsPage();
        ReportLogger.pass("Portfolio Reports page opened successfully");
    }

    public void verifyBackNavigationAndReopen() {
        ensureReportsPageOpen();
        scrollToTop();

        ReportLogger.step("Pressing Android back from Reports page");
        driver.navigate().back();
        sleep(900);

        boolean portfolioVisible = isVisible(PORTFOLIO)
                || pageContains("Summary")
                || pageContains("Portfolio Value")
                || pageContains("Your Investments");

        if (!portfolioVisible) {
            throw new AssertionError(
                    "Back navigation from Reports did not return to Portfolio. visibleValues="
                            + collectVisibleValues()
            );
        }

        ReportLogger.pass("Back navigation returned to Portfolio");

        ensureReportsPageOpen();
        ReportLogger.pass("Reports page reopened successfully after back navigation");
    }

    private void tapBottomPortfolioTab() {
        ReportLogger.step("Opening Portfolio bottom tab");

        List<WebElement> portfolioElements = visibleElements(PORTFOLIO);
        if (portfolioElements.isEmpty()) {
            throw new AssertionError(
                    "Portfolio bottom tab not found. visibleValues=" + collectVisibleValues()
            );
        }

        WebElement bottomMost = portfolioElements.stream()
                .max(Comparator.comparingInt(this::centerY))
                .orElse(portfolioElements.get(0));

        safeClick(bottomMost);
        sleep(800);
        ReportLogger.pass("Portfolio bottom tab tapped");
    }

    private void waitForPortfolioLanding() {
        long endAt = System.currentTimeMillis() + 12_000L;

        while (System.currentTimeMillis() < endAt) {
            if (pageContains("Summary")
                    || pageContains("Portfolio Value")
                    || pageContains("Your Investments")
                    || isVisible(REPORTS)) {
                ReportLogger.pass("Portfolio landing/menu state detected");
                return;
            }
            sleep(400);
        }

        throw new AssertionError(
                "Portfolio landing screen did not become ready. visibleValues=" + collectVisibleValues()
        );
    }

    private void openPortfolioMenu() {
        ReportLogger.step("Opening Portfolio options menu using exact top-right locator");

        // Do not tap the Portfolio bottom tab again. The tab is already selected and
        // re-tapping it does not open the menu on the current application build.
        if (isVisible(REPORTS)) {
            ReportLogger.pass("Portfolio options menu is already open");
            return;
        }

        WebElement exactMenuButton = findVisible(PORTFOLIO_OPTIONS_MENU);
        if (exactMenuButton != null) {
            safeClick(exactMenuButton);
            if (waitUntilVisible(REPORTS, 6)) {
                ReportLogger.pass("Portfolio options menu opened using exact ImageView instance(1) locator");
                return;
            }
        }

        // Future-safe fallback: locate a clickable ImageView inside the top-right
        // header area. This avoids depending only on a global instance number.
        WebElement boundedMenuButton = findTopRightPortfolioMenuButton();
        if (boundedMenuButton != null) {
            safeClick(boundedMenuButton);
            if (waitUntilVisible(REPORTS, 5)) {
                ReportLogger.pass("Portfolio options menu opened using bounded top-right ImageView");
                return;
            }
        }

        // Final fallback: tap the centre of the observed three-dot control bounds.
        Dimension size = driver.manage().window().getSize();
        int x = (int) (size.getWidth() * 0.905);
        int y = (int) (size.getHeight() * 0.061);
        tapCoordinates(x, y);

        if (!waitUntilVisible(REPORTS, 5)) {
            throw new AssertionError(
                    "Unable to open Portfolio menu using exact or bounded top-right locator. "
                            + "visibleValues=" + collectVisibleValues()
            );
        }

        ReportLogger.pass("Portfolio options menu opened using top-right coordinate fallback");
    }

    private WebElement findTopRightPortfolioMenuButton() {
        Dimension size = driver.manage().window().getSize();
        int minX = (int) (size.getWidth() * 0.80);
        int maxY = (int) (size.getHeight() * 0.16);

        return safeFindElements(AppiumBy.className("android.widget.ImageView")).stream()
                .filter(this::isDisplayed)
                .filter(this::isClickable)
                .filter(element -> centerX(element) >= minX)
                .filter(element -> centerY(element) > 40 && centerY(element) <= maxY)
                .max(Comparator.comparingInt(this::centerX))
                .orElse(null);
    }

    private WebElement findTopLeftClickableHeader() {
        Dimension size = driver.manage().window().getSize();
        int maxY = (int) (size.getHeight() * 0.22);
        int maxX = (int) (size.getWidth() * 0.62);

        List<WebElement> candidates = new ArrayList<>();
        candidates.addAll(safeFindElements(AppiumBy.className("android.widget.Button")));
        candidates.addAll(safeFindElements(AppiumBy.className("android.view.View")));

        return candidates.stream()
                .filter(this::isDisplayed)
                .filter(this::isClickable)
                .filter(element -> centerY(element) > 50 && centerY(element) < maxY)
                .filter(element -> centerX(element) < maxX)
                .filter(element -> {
                    String value = elementValue(element).toLowerCase(Locale.ENGLISH);
                    return !value.contains("update")
                            && !value.contains("notification")
                            && !value.equals("more")
                            && !value.equals("search");
                })
                .min(Comparator.comparingInt(this::centerX))
                .orElse(null);
    }

    private void waitForReportsPage() {
        long endAt = System.currentTimeMillis() + 12_000L;

        while (System.currentTimeMillis() < endAt) {
            if (isReportsPageActive()) {
                return;
            }
            sleep(400);
        }

        throw new AssertionError(
                "Reports page did not load after tapping Reports. visibleValues="
                        + collectVisibleValues()
        );
    }

    private boolean isReportsPageActive() {
        boolean headingAndSectionVisible = isVisible(REPORTS)
                && (isVisible(TAX_REPORT)
                || isVisible(HOLDINGS_STATEMENT)
                || isVisible(TRANSACTION_HISTORY));

        boolean reportsContentVisible = (pageContains("Tax Report")
                || pageContains("Holdings Statement")
                || pageContains("Transaction History"))
                && (pageContains("Financial Year")
                || pageContains("As of")
                || pageContains("Download"));

        return headingAndSectionVisible || reportsContentVisible;
    }

    private void closeUnexpectedOverlayIfPresent() {
        boolean overlayPresent = isVisible(SCRIM)
                || hasVisibleDismissableOverlay()
                || pageContains("Previous month")
                || pageContains("Next month")
                || pageContains("Select year");

        if (!overlayPresent) {
            return;
        }

        ReportLogger.step("Unexpected Reports overlay detected. Closing it before continuing.");
        driver.navigate().back();
        sleep(700);
    }

    // =========================================================
    // Main screen and section validations
    // =========================================================

    public void validateReportsPageHeaderAndPrimarySections() {
        ensureReportsPageOpen();
        scrollToTop();

        requireVisible(REPORTS, "Reports page heading");
        requireVisible(TAX_REPORT, "Tax Report section");
        requireVisible(HOLDINGS_STATEMENT, "Holdings Statement section");

        ReportLogger.pass("Reports heading and primary report sections are visible");
    }

    public void validateTaxReportCard() {
        ensureReportsPageOpen();
        WebElement heading = scrollToHeading("Tax Report", 6);

        WebElement investor = findNearestVisibleBelow(
                heading,
                descContains("Choose Investor"),
                screenHeight()
        );
        requireElement(investor, "Tax Report investor selector");

        WebElement financialYearLabel = findNearestVisibleBelow(
                heading,
                FINANCIAL_YEAR,
                screenHeight()
        );
        requireElement(financialYearLabel, "Tax Report Financial Year label");

        WebElement financialYearValue = findFinancialYearValueBelow(heading);
        requireElement(financialYearValue, "Tax Report selected financial year");
        validateFinancialYearValue(elementValue(financialYearValue));

        WebElement download = findDownloadForSection("Tax Report");
        assertActionable(download, "Tax Report Download button");

        ReportLogger.pass(
                "Tax Report card validated with investor selector, financial year and Download action"
        );
    }

    public void validateHoldingsStatementCard() {
        ensureReportsPageOpen();
        WebElement heading = scrollToHeading("Holdings Statement", 7);

        WebElement investor = findNearestVisibleBelow(
                heading,
                descContains("Choose Investor"),
                screenHeight()
        );
        requireElement(investor, "Holdings Statement investor selector");

        WebElement asOfLabel = findNearestVisibleBelow(heading, AS_OF, screenHeight());
        requireElement(asOfLabel, "Holdings Statement As of label");

        WebElement dateElement = findDateValueBelow(heading);
        requireElement(dateElement, "Holdings Statement As of date");
        validateReportDate(elementValue(dateElement), "Holdings Statement As of date");

        WebElement download = findDownloadForSection("Holdings Statement");
        assertActionable(download, "Holdings Statement Download button");

        ReportLogger.pass(
                "Holdings Statement card validated with investor selector, As of date and Download action"
        );
    }

    public void validateTransactionHistorySection() {
        ensureReportsPageOpen();
        WebElement heading = scrollToHeading("Transaction History", 9);

        WebElement download = findDownloadForSection("Transaction History");
        assertActionable(download, "Transaction History Download button");

        ReportLogger.pass("Transaction History section and Download action are available");
    }

    public void validateTransactionHistoryControls() {
        ensureReportsPageOpen();
        WebElement heading = scrollToHeading("Transaction History", 9);

        WebElement investor = findNearestVisibleBelow(
                heading,
                descContains("Choose Investor"),
                screenHeight()
        );

        if (investor == null) {
            throw new AssertionError(
                    "Transaction History investor selector not found below section heading. visibleValues="
                            + collectVisibleValues()
            );
        }

        List<String> regionValues = collectValuesBelow(heading);
        boolean hasDateLabel = containsAnyIgnoreCase(
                regionValues,
                "From",
                "To",
                "As of",
                "Start Date",
                "End Date",
                "Date"
        );
        boolean hasDateValue = regionValues.stream().anyMatch(this::isDisplayDate);

        if (!hasDateLabel && !hasDateValue) {
            throw new AssertionError(
                    "Transaction History date controls were not found. regionValues=" + regionValues
            );
        }

        ReportLogger.pass(
                "Transaction History investor and date controls validated. regionValues=" + regionValues
        );
    }

    public void validateAllReportSectionsAndDownloadActions() {
        ensureReportsPageOpen();

        String[] sections = {"Tax Report", "Holdings Statement", "Transaction History"};
        Set<String> validatedSections = new LinkedHashSet<>();

        for (String section : sections) {
            WebElement heading = scrollToHeading(section, 9);
            requireElement(heading, section + " heading");

            WebElement download = findDownloadForSection(section);
            assertActionable(download, section + " Download button");
            validatedSections.add(section);
        }

        if (validatedSections.size() < 3) {
            throw new AssertionError(
                    "Expected at least 3 report sections with Download actions. Found="
                            + validatedSections
            );
        }

        ReportLogger.pass(
                "All known report sections and Download actions validated: " + validatedSections
        );
    }

    // =========================================================
    // Selector and date-picker interaction validations
    // =========================================================

    public void validateTaxInvestorSelectorInteraction() {
        validateInvestorSelectorForSection("Tax Report");
    }

    public void validateHoldingsInvestorSelectorInteraction() {
        validateInvestorSelectorForSection("Holdings Statement");
    }

    public void validateTransactionHistoryInvestorSelectorInteraction() {
        validateInvestorSelectorForSection("Transaction History");
    }

    private void validateInvestorSelectorForSection(String sectionTitle) {
        ensureReportsPageOpen();
        WebElement heading = scrollToHeading(sectionTitle, 9);

        WebElement investor = findNearestVisibleBelow(
                heading,
                descContains("Choose Investor"),
                screenHeight()
        );
        requireElement(investor, sectionTitle + " investor selector");

        String selectedInvestorValue = elementValue(investor);
        if (selectedInvestorValue.trim().isEmpty()) {
            throw new AssertionError(sectionTitle + " investor selector has no accessible value");
        }

        ReportLogger.step(
                "Opening " + sectionTitle + " investor selector | currentValue=" + selectedInvestorValue
        );
        String sourceBeforeOpen = compactSource();
        safeClick(investor);

        if (!waitForOverlayChange(sourceBeforeOpen, 7)) {
            throw new AssertionError(
                    sectionTitle + " investor selector did not open a selection overlay. visibleValues="
                            + collectVisibleValues()
            );
        }

        ReportLogger.pass(sectionTitle + " investor selection overlay opened successfully");
        closeOverlayAndReturnToReports(sectionTitle + " investor selector");
    }

    public void validateFinancialYearSelectorInteraction() {
        ensureReportsPageOpen();
        WebElement heading = scrollToHeading("Tax Report", 6);

        WebElement yearValue = findFinancialYearValueBelow(heading);
        requireElement(yearValue, "Tax Report selected financial year");
        validateFinancialYearValue(elementValue(yearValue));

        ReportLogger.step("Opening Financial Year selector");
        String sourceBeforeOpen = compactSource();
        safeClickOrClickableAncestor(yearValue);

        if (!waitForOverlayChange(sourceBeforeOpen, 7)) {
            throw new AssertionError(
                    "Financial Year selector did not open. visibleValues=" + collectVisibleValues()
            );
        }

        if (!overlayContainsYearOption()) {
            throw new AssertionError(
                    "Financial Year overlay opened but no year option was detected. visibleValues="
                            + collectVisibleValues()
            );
        }

        ReportLogger.pass("Financial Year selector opened with year options");
        closeOverlayAndReturnToReports("Financial Year selector");
    }

    public void validateHoldingsDatePickerInteraction() {
        ensureReportsPageOpen();
        WebElement heading = scrollToHeading("Holdings Statement", 7);

        WebElement dateElement = findDateValueBelow(heading);
        requireElement(dateElement, "Holdings Statement As of date");
        validateReportDate(elementValue(dateElement), "Holdings Statement As of date");

        ReportLogger.step("Opening Holdings Statement As of date picker");
        safeClickOrClickableAncestor(dateElement);

        if (!waitForDatePicker(8)) {
            throw new AssertionError(
                    "Holdings Statement date picker did not open. visibleValues="
                            + collectVisibleValues()
            );
        }

        ReportLogger.pass("Holdings Statement date picker opened successfully");
        closeOverlayAndReturnToReports("Holdings Statement date picker");
    }

    // =========================================================
    // Download action validations
    // =========================================================

    public void validateTaxDownloadAction() {
        validateDownloadActionForSection("Tax Report");
    }

    public void validateHoldingsDownloadAction() {
        validateDownloadActionForSection("Holdings Statement");
    }

    public void validateTransactionHistoryDownloadAction() {
        validateDownloadActionForSection("Transaction History");
    }

    private void validateDownloadActionForSection(String sectionTitle) {
        ensureReportsPageOpen();
        scrollToHeading(sectionTitle, 9);

        WebElement download = findDownloadForSection(sectionTitle);
        assertActionable(download, sectionTitle + " Download button");

        ReportLogger.pass(
                sectionTitle + " Download control is visible, enabled and clickable. "
                        + "The test intentionally does not create a real downloaded file."
        );
    }

    // =========================================================
    // Section mapping helpers
    // =========================================================

    private WebElement scrollToHeading(String headingText, int maxSwipes) {
        scrollToTop();

        By headingLocator = exactTextOrDescription(headingText);
        WebElement heading = findVisible(headingLocator);
        if (heading != null) {
            return heading;
        }

        for (int attempt = 1; attempt <= maxSwipes; attempt++) {
            ReportLogger.step(
                    "Scrolling to report section: " + headingText + " | attempt=" + attempt
            );
            swipe("up");
            sleep(450);

            heading = findVisible(headingLocator);
            if (heading != null) {
                ReportLogger.pass("Report section visible: " + headingText);
                return heading;
            }
        }

        throw new AssertionError(
                "Report section not found after scrolling: " + headingText
                        + " | visibleValues=" + collectVisibleValues()
        );
    }

    private WebElement findDownloadForSection(String sectionTitle) {
        WebElement heading = findVisible(exactTextOrDescription(sectionTitle));
        if (heading == null) {
            heading = scrollToHeading(sectionTitle, 9);
        }

        WebElement download = findNearestVisibleBelow(heading, DOWNLOAD, screenHeight());
        if (download != null) {
            return download;
        }

        // The button can sit just below the viewport edge. Scroll once and remap by section.
        swipe("up");
        sleep(450);

        List<WebElement> downloads = visibleElements(DOWNLOAD);
        if (downloads.size() == 1) {
            return downloads.get(0);
        }

        heading = findVisible(exactTextOrDescription(sectionTitle));
        if (heading != null) {
            return findNearestVisibleBelow(heading, DOWNLOAD, screenHeight());
        }

        return null;
    }

    private WebElement findFinancialYearValueBelow(WebElement heading) {
        List<WebElement> candidates = visibleElements(descContains("Financial Year "));

        return candidates.stream()
                .filter(element -> centerY(element) > bottomY(heading))
                .filter(element -> FINANCIAL_YEAR_PATTERN.matcher(elementValue(element)).matches())
                .min(Comparator.comparingInt(element -> centerY(element) - bottomY(heading)))
                .orElse(null);
    }

    private WebElement findDateValueBelow(WebElement heading) {
        WebElement currentHeading = heading;

        for (int attempt = 0; attempt <= 3; attempt++) {
            List<WebElement> candidates = visibleDateCandidates();
            WebElement asOfLabel = findVisible(AS_OF);

            // Strongest mapping: the live date shown immediately below the As of label.
            if (asOfLabel != null) {
                int labelBottom = bottomY(asOfLabel);
                WebElement mappedBelowLabel = candidates.stream()
                        .filter(element -> centerY(element) > labelBottom)
                        .filter(element -> centerY(element) - labelBottom <= screenHeight() / 2)
                        .min(Comparator.comparingInt(element -> centerY(element) - labelBottom))
                        .orElse(null);

                if (mappedBelowLabel != null) {
                    return mappedBelowLabel;
                }
            }

            // Secondary mapping: date must be below Holdings Statement and, when visible,
            // above the Transaction History section.
            if (currentHeading == null || !isDisplayed(currentHeading)) {
                currentHeading = findVisible(HOLDINGS_STATEMENT);
            }

            if (currentHeading != null) {
                int headingBottom = bottomY(currentHeading);
                WebElement transactionHeading = findVisible(TRANSACTION_HISTORY);
                int transactionTop = transactionHeading == null
                        ? Integer.MAX_VALUE
                        : topY(transactionHeading);

                WebElement mappedBelowHeading = candidates.stream()
                        .filter(element -> centerY(element) > headingBottom)
                        .filter(element -> centerY(element) < transactionTop)
                        .min(Comparator.comparingInt(element -> centerY(element) - headingBottom))
                        .orElse(null);

                if (mappedBelowHeading != null) {
                    return mappedBelowHeading;
                }
            }

            // On some devices the value is just outside the initial viewport and is not
            // present in the accessibility tree until the card is scrolled slightly.
            if (attempt < 3) {
                ReportLogger.step(
                        "Scrolling Holdings Statement card to reveal As of date | attempt="
                                + (attempt + 1)
                );
                swipe("up");
                sleep(500);
                currentHeading = findVisible(HOLDINGS_STATEMENT);
            }
        }

        return null;
    }

    private List<WebElement> visibleDateCandidates() {
        List<WebElement> candidates = new ArrayList<>();

        addUniqueVisibleElements(
                candidates,
                safeFindElements(AppiumBy.androidUIAutomator(
                        "new UiSelector().textMatches(\"\\\\d{1,2}-[A-Za-z]{3}-\\\\d{4}\")"
                ))
        );

        addUniqueVisibleElements(
                candidates,
                safeFindElements(AppiumBy.androidUIAutomator(
                        "new UiSelector().descriptionMatches(\"\\\\d{1,2}-[A-Za-z]{3}-\\\\d{4}\")"
                ))
        );

        return candidates;
    }

    private void addUniqueVisibleElements(
            List<WebElement> target,
            List<WebElement> source
    ) {
        for (WebElement element : source) {
            if (isDisplayed(element)
                    && isDisplayDate(elementValue(element))
                    && !target.contains(element)) {
                target.add(element);
            }
        }
    }

    private WebElement findNearestVisibleBelow(
            WebElement heading,
            By locator,
            int maxVerticalDistance
    ) {
        if (heading == null) {
            return null;
        }

        int headingBottom = bottomY(heading);

        return visibleElements(locator).stream()
                .filter(element -> centerY(element) > headingBottom)
                .filter(element -> centerY(element) - headingBottom <= maxVerticalDistance)
                .min(Comparator.comparingInt(element -> centerY(element) - headingBottom))
                .orElse(null);
    }

    // =========================================================
    // Value validation helpers
    // =========================================================

    private void validateFinancialYearValue(String value) {
        String normalized = normalize(value);
        Matcher matcher = FINANCIAL_YEAR_PATTERN.matcher(normalized);

        if (!matcher.matches()) {
            throw new AssertionError(
                    "Invalid Financial Year value. Expected format 'Financial Year YYYY-YY', actual="
                            + value
            );
        }

        int startYear = Integer.parseInt(matcher.group(1));
        int endYear = Integer.parseInt(matcher.group(2));
        int expectedEndYear = (startYear + 1) % 100;

        if (endYear != expectedEndYear) {
            throw new AssertionError(
                    "Financial Year range is not consecutive. value=" + value
            );
        }

        ReportLogger.pass("Validated Financial Year value: " + normalized);
    }

    private void validateReportDate(String value, String label) {
        String normalized = normalize(value);

        if (!isDisplayDate(normalized)) {
            throw new AssertionError(
                    label + " has invalid format. Expected dd-MMM-yyyy, actual=" + value
            );
        }

        LocalDate parsed;
        try {
            parsed = LocalDate.parse(normalized, DISPLAY_DATE_FORMATTER);
        } catch (Exception e) {
            throw new AssertionError(label + " could not be parsed: " + value, e);
        }

        LocalDate todayIst = LocalDate.now(IST);
        if (parsed.isAfter(todayIst)) {
            throw new AssertionError(
                    label + " cannot be in the future. actual=" + parsed + " | todayIST=" + todayIst
            );
        }

        ReportLogger.pass(label + " validated: " + normalized);
    }

    private boolean isDisplayDate(String value) {
        return value != null && DISPLAY_DATE_PATTERN.matcher(normalize(value)).matches();
    }

    private void assertActionable(WebElement element, String label) {
        requireElement(element, label);

        if (!isDisplayed(element)) {
            throw new AssertionError(label + " is not displayed");
        }

        if (!isEnabled(element)) {
            throw new AssertionError(label + " is disabled");
        }

        if (!isClickable(element) && findClickableAncestor(element) == null) {
            throw new AssertionError(
                    label + " is not clickable and has no clickable ancestor. attributes="
                            + summarizeElement(element)
            );
        }

        ReportLogger.pass(label + " is visible, enabled and clickable");
    }

    // =========================================================
    // Overlay helpers
    // =========================================================

    private boolean waitForOverlayChange(String sourceBeforeOpen, int timeoutSeconds) {
        long endAt = System.currentTimeMillis() + timeoutSeconds * 1000L;

        while (System.currentTimeMillis() < endAt) {
            if (isVisible(SCRIM) || hasVisibleDismissableOverlay()) {
                return true;
            }

            String currentSource = compactSource();
            int beforeChooseInvestorCount = countOccurrences(sourceBeforeOpen, "Choose Investor");
            int currentChooseInvestorCount = countOccurrences(currentSource, "Choose Investor");

            if (!currentSource.equals(sourceBeforeOpen)
                    && (currentSource.contains("Cancel")
                    || currentSource.contains("Select year")
                    || currentChooseInvestorCount > beforeChooseInvestorCount)) {
                return true;
            }

            sleep(350);
        }
        return false;
    }

    private boolean hasVisibleDismissableOverlay() {
        for (WebElement element : safeFindElements(By.xpath("//*[@dismissable='true']"))) {
            if (isDisplayed(element)) {
                return true;
            }
        }
        return false;
    }

    private boolean overlayContainsYearOption() {
        String source = safePageSource();
        return source.matches("(?s).*?(19|20)\\d{2}.*")
                || source.contains("Financial Year");
    }

    private boolean waitForDatePicker(int timeoutSeconds) {
        long endAt = System.currentTimeMillis() + timeoutSeconds * 1000L;

        while (System.currentTimeMillis() < endAt) {
            String source = safePageSource();
            String normalizedSource = source.toLowerCase(Locale.ENGLISH);

            if (source.contains("Select year")
                    || source.contains("Previous month")
                    || source.contains("Next month")
                    || source.contains("Switch to text input mode")
                    || normalizedSource.contains("android.widget.datepicker")
                    || normalizedSource.contains("date_picker")
                    || (source.contains("Cancel") && source.contains("OK"))) {
                return true;
            }
            sleep(350);
        }
        return false;
    }

    private void closeOverlayAndReturnToReports(String overlayName) {
        ReportLogger.step("Closing " + overlayName + " without changing report data");
        driver.navigate().back();
        sleep(700);

        if (!pageContains("Reports")) {
            ensureReportsPageOpen();
        }

        ReportLogger.pass(overlayName + " closed safely");
    }

    // =========================================================
    // Generic interaction helpers
    // =========================================================

    private void ensureAdvisorAppActive() {
        try {
            String currentPackage = driver.getCurrentPackage();
            if (ADVISOR_PACKAGE.equals(currentPackage)) {
                return;
            }

            Map<String, Object> args = new HashMap<>();
            args.put("appId", ADVISOR_PACKAGE);
            driver.executeScript("mobile: activateApp", args);
            sleep(1200);
        } catch (Exception e) {
            ReportLogger.step("Advisor app activation check skipped: " + clean(e.getMessage()));
        }
    }

    private void scrollToTop() {
        String previousSource = "";

        for (int i = 1; i <= 5; i++) {
            String currentSource = compactSource();
            if (i > 1 && currentSource.equals(previousSource)) {
                break;
            }

            previousSource = currentSource;
            swipe("down");
            sleep(350);
        }
    }

    private void swipe(String direction) {
        try {
            Dimension size = driver.manage().window().getSize();
            Map<String, Object> args = new HashMap<>();
            args.put("left", (int) (size.getWidth() * 0.05));
            args.put("top", (int) (size.getHeight() * 0.16));
            args.put("width", (int) (size.getWidth() * 0.90));
            args.put("height", (int) (size.getHeight() * 0.68));
            args.put("direction", direction);
            args.put("percent", 0.72);
            driver.executeScript("mobile: swipeGesture", args);
        } catch (Exception e) {
            throw new AssertionError(
                    "Failed to swipe " + direction + ": " + clean(e.getMessage()),
                    e
            );
        }
    }

    private void safeClickOrClickableAncestor(WebElement element) {
        if (isClickable(element)) {
            safeClick(element);
            sleep(500);
            return;
        }

        WebElement parent = findClickableAncestor(element);
        if (parent != null) {
            safeClick(parent);
            sleep(500);
            return;
        }

        tapCoordinates(centerX(element), centerY(element));
        sleep(500);
    }

    private void safeClick(WebElement element) {
        try {
            element.click();
        } catch (Exception clickError) {
            try {
                tapCoordinates(centerX(element), centerY(element));
            } catch (Exception gestureError) {
                throw new AssertionError(
                        "Unable to click element. clickError=" + clean(clickError.getMessage())
                                + " | gestureError=" + clean(gestureError.getMessage()),
                        gestureError
                );
            }
        }
    }

    private void tapCoordinates(int x, int y) {
        Map<String, Object> args = new HashMap<>();
        args.put("x", x);
        args.put("y", y);
        driver.executeScript("mobile: clickGesture", args);
    }

    private WebElement waitForVisible(By locator, int timeoutSeconds) {
        long endAt = System.currentTimeMillis() + timeoutSeconds * 1000L;

        while (System.currentTimeMillis() < endAt) {
            WebElement element = findVisible(locator);
            if (element != null) {
                return element;
            }
            sleep(350);
        }

        throw new AssertionError(
                "Visible element not found: " + locator + " | visibleValues=" + collectVisibleValues()
        );
    }

    private boolean waitUntilVisible(By locator, int timeoutSeconds) {
        long endAt = System.currentTimeMillis() + timeoutSeconds * 1000L;

        while (System.currentTimeMillis() < endAt) {
            if (isVisible(locator)) {
                return true;
            }
            sleep(300);
        }
        return false;
    }

    private void requireVisible(By locator, String label) {
        WebElement element = waitForVisible(locator, 8);
        requireElement(element, label);
        ReportLogger.pass(label + " is visible");
    }

    private void requireElement(WebElement element, String label) {
        if (element == null || !isDisplayed(element)) {
            throw new AssertionError(
                    label + " not found/displayed. visibleValues=" + collectVisibleValues()
            );
        }
    }

    private WebElement findVisible(By locator) {
        for (WebElement element : safeFindElements(locator)) {
            if (isDisplayed(element)) {
                return element;
            }
        }
        return null;
    }

    private List<WebElement> visibleElements(By locator) {
        List<WebElement> visible = new ArrayList<>();
        for (WebElement element : safeFindElements(locator)) {
            if (isDisplayed(element)) {
                visible.add(element);
            }
        }
        return visible;
    }

    private List<WebElement> safeFindElements(By locator) {
        try {
            return driver.findElements(locator);
        } catch (Exception ignored) {
            return new ArrayList<>();
        }
    }

    private boolean isVisible(By locator) {
        return findVisible(locator) != null;
    }

    private boolean isDisplayed(WebElement element) {
        try {
            return element != null && element.isDisplayed();
        } catch (Exception ignored) {
            return false;
        }
    }

    private boolean isClickable(WebElement element) {
        try {
            return element != null
                    && "true".equalsIgnoreCase(element.getAttribute("clickable"));
        } catch (Exception ignored) {
            return false;
        }
    }

    private boolean isEnabled(WebElement element) {
        try {
            return element != null && element.isEnabled();
        } catch (Exception ignored) {
            return false;
        }
    }

    private WebElement findClickableAncestor(WebElement element) {
        String value = elementValue(element);
        if (value.isEmpty()) {
            return null;
        }

        By parentLocator = By.xpath(
                "//*[@content-desc=" + quoteXpath(value) + " or @text=" + quoteXpath(value) + "]"
                        + "/ancestor::*[@clickable='true'][1]"
        );
        return findVisible(parentLocator);
    }

    private int centerX(WebElement element) {
        Rectangle rect = element.getRect();
        return rect.getX() + rect.getWidth() / 2;
    }

    private int centerY(WebElement element) {
        Rectangle rect = element.getRect();
        return rect.getY() + rect.getHeight() / 2;
    }

    private int bottomY(WebElement element) {
        Rectangle rect = element.getRect();
        return rect.getY() + rect.getHeight();
    }

    private int topY(WebElement element) {
        return element.getRect().getY();
    }

    private int screenHeight() {
        return driver.manage().window().getSize().getHeight();
    }

    private String elementValue(WebElement element) {
        if (element == null) {
            return "";
        }

        String[] attributes = {"contentDescription", "content-desc", "text", "name", "label"};
        for (String attribute : attributes) {
            try {
                String value = element.getAttribute(attribute);
                if (value != null && !value.trim().isEmpty()) {
                    return normalize(value);
                }
            } catch (Exception ignored) {
                // Try next attribute.
            }
        }

        try {
            String text = element.getText();
            return text == null ? "" : normalize(text);
        } catch (Exception ignored) {
            return "";
        }
    }

    private String summarizeElement(WebElement element) {
        if (element == null) {
            return "null";
        }

        try {
            return "value=" + elementValue(element)
                    + ", clickable=" + element.getAttribute("clickable")
                    + ", enabled=" + element.isEnabled()
                    + ", rect=" + element.getRect();
        } catch (Exception e) {
            return "Unable to summarize element: " + clean(e.getMessage());
        }
    }

    private List<String> collectValuesBelow(WebElement heading) {
        List<String> values = new ArrayList<>();
        int headingBottom = bottomY(heading);

        for (WebElement element : safeFindElements(By.xpath("//*"))) {
            if (!isDisplayed(element) || centerY(element) <= headingBottom) {
                continue;
            }

            String value = elementValue(element);
            if (!value.isEmpty() && !values.contains(value)) {
                values.add(value);
            }
        }

        return values;
    }

    private boolean containsAnyIgnoreCase(List<String> values, String... expectedValues) {
        for (String value : values) {
            String lower = value.toLowerCase(Locale.ENGLISH);
            for (String expected : expectedValues) {
                if (lower.contains(expected.toLowerCase(Locale.ENGLISH))) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean pageContains(String marker) {
        return safePageSource().toLowerCase(Locale.ENGLISH)
                .contains(marker.toLowerCase(Locale.ENGLISH));
    }

    private String safePageSource() {
        try {
            String source = driver.getPageSource();
            return source == null ? "" : source;
        } catch (Exception e) {
            return "";
        }
    }

    private String compactSource() {
        return safePageSource().replaceAll("\\s+", " ").trim();
    }

    private String collectVisibleValues() {
        List<String> values = new ArrayList<>();

        for (WebElement element : safeFindElements(By.xpath("//*"))) {
            if (!isDisplayed(element)) {
                continue;
            }

            String value = elementValue(element);
            if (!value.isEmpty() && !values.contains(value)) {
                values.add(value);
            }

            if (values.size() >= 60) {
                break;
            }
        }

        return values.toString();
    }

    private static By exactTextOrDescription(String value) {
        return By.xpath(
                "//*[@content-desc=" + quoteXpath(value) + " or @text=" + quoteXpath(value) + "]"
        );
    }

    private static By descContains(String value) {
        return AppiumBy.androidUIAutomator(
                "new UiSelector().descriptionContains(\""
                        + escapeUiAutomator(value)
                        + "\")"
        );
    }

    private static String escapeUiAutomator(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static String quoteXpath(String value) {
        if (!value.contains("'")) {
            return "'" + value + "'";
        }
        if (!value.contains("\"")) {
            return "\"" + value + "\"";
        }

        String[] parts = value.split("'", -1);
        StringBuilder builder = new StringBuilder("concat(");
        for (int i = 0; i < parts.length; i++) {
            if (i > 0) {
                builder.append(", \"'\", ");
            }
            builder.append("'").append(parts[i]).append("'");
        }
        builder.append(")");
        return builder.toString();
    }

    private static int countOccurrences(String value, String marker) {
        if (value == null || marker == null || marker.isEmpty()) {
            return 0;
        }

        int count = 0;
        int fromIndex = 0;
        while ((fromIndex = value.indexOf(marker, fromIndex)) >= 0) {
            count++;
            fromIndex += marker.length();
        }
        return count;
    }

    private static String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.replaceAll("\\s+", " ").trim();
    }

    private static String clean(String message) {
        return message == null ? "" : normalize(message);
    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}