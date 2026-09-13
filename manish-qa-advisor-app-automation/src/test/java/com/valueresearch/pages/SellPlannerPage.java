package com.valueresearch.pages;

import com.valueresearch.utils.AuthHelper;
import com.valueresearch.utils.ReportLogger;
import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;
import org.openqa.selenium.By;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Rectangle;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.PointerInput;
import org.openqa.selenium.interactions.Sequence;
import org.openqa.selenium.remote.RemoteWebElement;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * VRSA Sell Planner page object.
 *
 * Stability rules:
 * - Exact accessibility IDs/content-desc from Appium Inspector are primary.
 * - Link/page-title collisions are separated by Android class (ImageView vs View).
 * - Holding selection uses Inspector-backed row/checkbox-column geometry for the unchecked state; checked-state ImageView is only a post-condition. Stepper controls use runtime row geometry, never UiSelector instance indexes.
 * - Horizontal table movement uses bounded W3C drags inside the actual
 *   HorizontalScrollView, matching the stable Investment Planner approach.
 * - Every testcase starts from a verified Sell Planner baseline. Both populated
 *   holdings and the legitimate empty/import state are supported; dirty selection
 *   state is reset with one controlled Back-to-Hub cycle only.
 * - Preview calculations are validated from runtime weights/shares. Real app
 *   calculation defects remain testcase failures and are never hidden.
 */
public class SellPlannerPage {

    public static final String BUILD_VERSION = "VRSA-SP-2026-09-13-1.8";

    private static final String APP_PACKAGE = "com.valueresearch.advisor";

    private static final int SHORT_WAIT_SECONDS = 12;
    private static final int PAGE_WAIT_SECONDS = 25;
    private static final int POLL_MS = 250;
    private static final long SCREEN_SETTLE_MS = 900L;
    private static final long ROW_SETTLE_MS = 450L;

    private static final double DEFAULT_WEIGHT_TOLERANCE =
            Double.parseDouble(System.getProperty("sell.weightTolerance", "0.40"));

    private static final double SUMMARY_AMOUNT_TOLERANCE = 2.0;

    private final AndroidDriver driver;

    // =========================================================
    // CONFIRMED APPIUM INSPECTOR LOCATORS
    // =========================================================

    private static final By HUB_TAB = AppiumBy.accessibilityId("Hub");
    private static final By SELL_PLANNER_HUB_TILE = AppiumBy.accessibilityId("Open sell planner");

    private static final By SELL_PLANNER_TITLE = AppiumBy.accessibilityId("Sell Planner");
    private static final By TOTAL_WORTH_LABEL =
            AppiumBy.accessibilityId("TOTAL WORTH OF STOCK SELECTED");
    private static final By YOUR_HOLDINGS = AppiumBy.accessibilityId("Your Holdings");
    private static final By STOCKS_HEADER = AppiumBy.accessibilityId("Stocks");
    private static final By SHARES_HEADER = AppiumBy.accessibilityId("Shares");
    private static final By AMOUNT_HEADER = AppiumBy.accessibilityId("Amount");
    private static final By PERCENT_PORTFOLIO_HEADER = AppiumBy.accessibilityId("% Portfolio");

    private static final By INITIAL_CTA = AppiumBy.accessibilityId("Select holdings to sell");
    private static final By MAIL_MY_ORDER = AppiumBy.accessibilityId("Mail my order");

    // Empty-holdings state observed on the current Sell Planner build. Navigation must
    // treat this as a successfully loaded planner state rather than waiting forever for
    // the holdings-only initial CTA.
    private static final By EMPTY_HOLDINGS_MESSAGE =
            AppiumBy.androidUIAutomator(
                    "new UiSelector().descriptionContains(\"underlying data is unavailable\")"
            );
    private static final By IMPORT_YOUR_STOCKS = AppiumBy.accessibilityId("Import your stocks");

    private static final By GUIDELINES_LINK =
            AppiumBy.androidUIAutomator(
                    "new UiSelector().className(\"android.widget.ImageView\").description(\"Guidelines for selling stocks\")"
            );

    private static final By GUIDELINES_SHEET_TITLE =
            AppiumBy.androidUIAutomator(
                    "new UiSelector().className(\"android.view.View\").description(\"Guidelines for selling stocks\")"
            );

    private static final By INVESTOR_SELECTOR =
            AppiumBy.androidUIAutomator(
                    "new UiSelector().className(\"android.widget.Button\").descriptionContains(\"Shows investor selection list\")"
            );

    private static final By CHOOSE_INVESTOR = AppiumBy.accessibilityId("Choose Investor");

    private static final By PORTFOLIO_PREVIEW_LINK =
            AppiumBy.androidUIAutomator(
                    "new UiSelector().className(\"android.widget.ImageView\").description(\"Portfolio preview\")"
            );

    private static final By PORTFOLIO_PREVIEW_PAGE_TITLE =
            AppiumBy.androidUIAutomator(
                    "new UiSelector().className(\"android.view.View\").description(\"Portfolio preview\")"
            );

    private static final By COMPANY_NAME_HEADER = AppiumBy.accessibilityId("Company Name");
    private static final By CURRENT_WEIGHT_HEADER = AppiumBy.accessibilityId("Current wt.");
    private static final By NET_WEIGHT_HEADER = AppiumBy.accessibilityId("Net wt.");

    private static final By SELECTED_COUNT_MARKER =
            AppiumBy.androidUIAutomator(
                    "new UiSelector().descriptionContains(\"holdings ready to sell\")"
            );

    private static final By HORIZONTAL_SCROLL_VIEW =
            AppiumBy.className("android.widget.HorizontalScrollView");

    private static final String UNICODE_MINUS = "\u2212";
    private static final String ASCII_MINUS = "-";
    private static final String PLUS_SEMANTIC = "+";

    private static final String[] DEFAULT_EXPECTED_HOLDINGS = new String[]{
            "Adani Power",
            "Bajaj Housing Finance",
            "CMS Info Systems",
            "Emcure Pharmaceuticals",
            "Groww Gold ETF-G",
            "Tata Power Company"
    };

    public SellPlannerPage(AndroidDriver driver) {
        this.driver = driver;
    }

    // =========================================================
    // PUBLIC - NAVIGATION / LANDING
    // =========================================================

    public void openSellPlannerFromHub() {
        ReportLogger.step("Opening VRSA Sell Planner");
        activateAdvisorAppIfNeeded();

        if (recoverToCleanSellPlannerLocally()) {
            waitForCleanSellPlannerReadyOrThrow("local Sell Planner recovery");
            sleep(SCREEN_SETTLE_MS);
            ReportLogger.pass("Sell Planner ready using local screen recovery");
            return;
        }

        // If local recovery intentionally returned a dirty Sell Planner state to Hub,
        // do not run AuthHelper again. Only authenticate when Hub/bottom navigation
        // is not already available.
        if (firstDisplayed(HUB_TAB) == null) {
            new AuthHelper(driver).ensureLoggedIn();
            ReportLogger.pass("Advisor app login/session confirmed");
        } else {
            ReportLogger.debug("Hub/bottom navigation already visible; skipping redundant auth recovery");
        }

        WebElement hub = waitForDisplayed(HUB_TAB, SHORT_WAIT_SECONDS);
        if (hub == null) {
            throw new AssertionError("Hub bottom navigation tab is not visible");
        }
        clickElement(hub, "Hub bottom navigation tab");
        sleep(650L);

        WebElement tile = findSellPlannerHubTileWithBoundedScroll();
        if (tile == null) {
            throw new AssertionError("Sell Planner Hub tile was not found");
        }
        clickElement(tile, "Sell Planner Hub tile");

        waitForCleanSellPlannerReadyOrThrow("Hub navigation");
        sleep(SCREEN_SETTLE_MS);
        ReportLogger.pass("VRSA Sell Planner opened successfully");
    }

    public void validateLandingPage() {
        ReportLogger.step("Validating Sell Planner landing page");

        requireDisplayed(SELL_PLANNER_TITLE, "Sell Planner title");
        requireDisplayed(TOTAL_WORTH_LABEL, "TOTAL WORTH OF STOCK SELECTED label");
        requireDisplayed(GUIDELINES_LINK, "Guidelines for selling stocks link");
        requireDisplayed(YOUR_HOLDINGS, "Your Holdings section");

        if (firstDisplayed(MAIL_MY_ORDER) != null) {
            throw new AssertionError("Sell Planner landing unexpectedly shows Mail my order CTA before a test selection");
        }

        int selected = readSelectedHoldingsCountSafe();
        if (selected >= 0 && selected != 0) {
            throw new AssertionError("Sell Planner landing expected 0 selected holdings but found " + selected);
        }

        double totalWorth = readTopSelectedWorth();
        if (Math.abs(totalWorth) > 0.01) {
            throw new AssertionError("Sell Planner landing expected zero selected worth but found " + totalWorth);
        }

        // A user with no imported/available stocks has a valid, fully loaded Sell Planner
        // empty state. It intentionally does not render the holdings table, Portfolio
        // preview link or the "Select holdings to sell" CTA.
        if (isEmptyHoldingsState()) {
            requireDisplayed(IMPORT_YOUR_STOCKS, "Import your stocks CTA");
            ReportLogger.pass(
                    "Sell Planner empty-holdings landing validated successfully"
                            + " | selected=" + (selected < 0 ? "not exposed" : selected)
                            + " | totalWorth=₹" + formatNumber(totalWorth)
            );
            return;
        }

        // Populated holdings state.
        requireDisplayed(STOCKS_HEADER, "Stocks header");
        requireDisplayed(PORTFOLIO_PREVIEW_LINK, "Portfolio preview link");
        requireDisplayed(INITIAL_CTA, "Select holdings to sell CTA");

        if (selected < 0) {
            ReportLogger.info(
                    "Selected-count leading digit is not exposed reliably on the populated clean baseline; "
                            + "validated zero-selection state using initial CTA and absent Mail CTA"
            );
        } else {
            ReportLogger.pass("Sell Planner selected holdings count is 0");
        }

        for (String holding : expectedHoldings()) {
            requireDisplayed(AppiumBy.accessibilityId(holding), "holding: " + holding);
        }

        ReportLogger.pass("Sell Planner populated holdings landing page validated successfully");
    }

    /**
     * Returns configured holdings that are unavailable on the current Sell Planner page.
     * This is used by the TestNG layer to mark data-dependent sell scenarios as SKIPPED
     * when the current investor has no portfolio, rather than reporting false product
     * failures for every case.
     */
    public List<String> missingConfiguredHoldings() {
        return missingRequiredHoldings(expectedHoldings());
    }

    public List<String> missingRequiredHoldings(String... requiredStocks) {
        if (!isSellPlannerStructureReady()) {
            throw new AssertionError("Cannot inspect Sell Planner holdings because the planner structure is not ready");
        }

        List<String> missing = new ArrayList<>();
        if (requiredStocks == null || requiredStocks.length == 0) {
            return missing;
        }

        if (isEmptyHoldingsState()) {
            for (String stock : requiredStocks) {
                if (stock != null && !stock.trim().isEmpty()) {
                    missing.add(stock.trim());
                }
            }
            return missing;
        }

        for (String stock : requiredStocks) {
            if (stock == null || stock.trim().isEmpty()) {
                continue;
            }
            String value = stock.trim();
            if (firstDisplayed(AppiumBy.accessibilityId(value)) == null) {
                missing.add(value);
            }
        }
        return missing;
    }

    // =========================================================
    // PUBLIC - INVESTOR / GUIDELINES
    // =========================================================

    public void validateInvestorSelector() {
        ReportLogger.step("Validating Sell Planner investor selector");

        WebElement selector = waitForDisplayed(INVESTOR_SELECTOR, SHORT_WAIT_SECONDS);
        if (selector == null) {
            throw new AssertionError("Sell Planner investor selector is not visible");
        }

        String selectorSemantic = semanticValue(selector);
        String currentInvestor = selectorSemantic;
        int comma = selectorSemantic.indexOf(',');
        if (comma > 0) {
            currentInvestor = selectorSemantic.substring(0, comma).trim();
        }

        clickElement(selector, "Sell Planner investor selector");

        if (waitForDisplayed(CHOOSE_INVESTOR, SHORT_WAIT_SECONDS) == null) {
            throw new AssertionError("Choose Investor sheet did not open");
        }

        if (currentInvestor != null && !currentInvestor.trim().isEmpty()) {
            boolean foundCurrent = false;
            for (WebElement button : safeFindElements(AppiumBy.className("android.widget.Button"))) {
                String value = semanticValue(button);
                if (value.startsWith(currentInvestor + " (") || value.equals(currentInvestor)) {
                    foundCurrent = true;
                    break;
                }
            }
            if (!foundCurrent) {
                throw new AssertionError("Current investor was not listed in Choose Investor sheet: " + currentInvestor);
            }
        }

        driver.navigate().back();
        waitForPlannerVisibleOrThrow("closing Choose Investor sheet");
        ReportLogger.pass("Sell Planner investor selector validated successfully");
    }

    public void validateGuidelinesSheet() {
        ReportLogger.step("Validating Guidelines for selling stocks sheet");

        WebElement link = waitForDisplayed(GUIDELINES_LINK, SHORT_WAIT_SECONDS);
        if (link == null) {
            throw new AssertionError("Guidelines for selling stocks link is not visible");
        }
        clickElement(link, "Guidelines for selling stocks");

        if (waitForDisplayed(GUIDELINES_SHEET_TITLE, SHORT_WAIT_SECONDS) == null) {
            throw new AssertionError("Guidelines for selling stocks sheet did not open");
        }

        String[] requiredText = new String[]{
                "We recommend you sell stocks in the following priority.",
                "For stocks not in our recommendations",
                "Low-quality stocks",
                "Stocks where you have insignificant holding, such as less than 2%",
                "Loss-making stocks to offset your gains and save tax",
                "For recommended stocks",
                "Stocks we have exited",
                "Hold for expensive valuations",
                "Hold for fundamentals"
        };

        String source = safePageSource().toLowerCase(Locale.US);
        for (String expected : requiredText) {
            if (!source.contains(expected.toLowerCase(Locale.US))) {
                throw new AssertionError("Guidelines sheet is missing expected text: " + expected);
            }
        }

        driver.navigate().back();
        waitForPlannerVisibleOrThrow("closing Guidelines sheet");
        ReportLogger.pass("Guidelines for selling stocks content validated successfully");
    }

    // =========================================================
    // PUBLIC - HOLDING / QUANTITY SCENARIOS
    // =========================================================

    public void validatePrimaryHoldingSelection(String stockName) {
        SaleContext context = prepareSale(singlePlan(stockName, -1), false);

        int selected = readSelectedHoldingsCount();
        if (selected != 1) {
            throw new AssertionError("Expected exactly 1 selected holding but found " + selected);
        }

        requireDisplayed(MAIL_MY_ORDER, "Mail my order CTA after selecting holding");
        validateSelectedWorthAgainstRows(context);

        ReportLogger.pass("Holding selection and selected-worth summary validated: " + stockName);
    }

    public void validateSingleSellQuantity(String stockName, int targetSellQuantity) {
        SaleContext context = prepareSale(singlePlan(stockName, targetSellQuantity), true);
        Integer actual = context.saleQuantities.get(stockName);
        if (actual == null || actual.intValue() != targetSellQuantity) {
            throw new AssertionError(
                    "Sell quantity did not settle to target"
                            + " | stock=" + stockName
                            + " | expected=" + targetSellQuantity
                            + " | actual=" + actual
            );
        }
        validateSelectedWorthAgainstRows(context);
        ReportLogger.pass("Sell quantity validated | stock=" + stockName + " | quantity=" + targetSellQuantity);
    }

    public int readTotalShares(String stockName) {
        ensureSharesColumnsVisible();
        RowControls controls = readRowControls(stockName);
        return controls.quantity;
    }

    public void validateSingleHoldingPreview(String stockName, int targetSellQuantity) {
        SaleContext context = prepareSale(singlePlan(stockName, targetSellQuantity), true);
        openPortfolioPreview();
        validateHoldingsPreview(context);
    }

    public void validateFullSalePreview(String stockName) {
        Map<String, HoldingSnapshot> snapshots = captureHoldingSnapshots();
        SaleContext context = prepareSaleUsingSnapshots(singlePlan(stockName, -1), snapshots, true);
        openPortfolioPreview();
        validateHoldingsPreview(context);
    }

    public void validateDualHoldingPreview(
            String primaryStock,
            int primarySellQuantity,
            String secondaryStock,
            int secondarySellQuantity
    ) {
        LinkedHashMap<String, Integer> plan = new LinkedHashMap<>();
        plan.put(primaryStock, primarySellQuantity);
        plan.put(secondaryStock, secondarySellQuantity);

        SaleContext context = prepareSale(plan, true);
        openPortfolioPreview();
        validateHoldingsPreview(context);
    }

    public void validateFullPrimaryPartialSecondarySectorPreview(
            String primaryStock,
            String secondaryStock,
            int secondarySellQuantity
    ) {
        Map<String, HoldingSnapshot> snapshots = captureHoldingSnapshots();

        LinkedHashMap<String, Integer> plan = new LinkedHashMap<>();
        plan.put(primaryStock, -1);
        plan.put(secondaryStock, secondarySellQuantity);

        SaleContext context = prepareSaleUsingSnapshots(plan, snapshots, true);
        openPortfolioPreview();
        // Holdings calculations are covered independently by SP_006-SP_008.
        // Keep Sector validation independent so a Holdings product defect does
        // not prevent the suite from reaching and validating the Sector tab.
        validateCategoryPreview(context, "sector");
    }

    public void validateFullPrimaryFullSecondarySectorPreview(
            String primaryStock,
            String secondaryStock
    ) {
        Map<String, HoldingSnapshot> snapshots = captureHoldingSnapshots();

        LinkedHashMap<String, Integer> plan = new LinkedHashMap<>();
        plan.put(primaryStock, -1);
        plan.put(secondaryStock, -1);

        SaleContext context = prepareSaleUsingSnapshots(plan, snapshots, true);
        openPortfolioPreview();
        // Validate Sector independently of Holdings so both product defects can
        // be surfaced in separate testcases.
        validateCategoryPreview(context, "sector");

        HoldingSnapshot primary = context.snapshots.get(primaryStock);
        HoldingSnapshot secondary = context.snapshots.get(secondaryStock);
        if (primary != null
                && secondary != null
                && !primary.sector.isEmpty()
                && primary.sector.equals(secondary.sector)
                && noOtherHoldingInSector(context, primary.sector, primaryStock, secondaryStock)) {
            PreviewRow sectorRow = findPreviewRowByExactLabel(primary.sector);
            if (sectorRow == null) {
                throw new AssertionError("Sector row not found after full sale: " + primary.sector);
            }
            if (Math.abs(sectorRow.netWeight) > DEFAULT_WEIGHT_TOLERANCE) {
                throw new AssertionError(
                        "Expected fully sold sector to become 0%"
                                + " | sector=" + primary.sector
                                + " | actualNetWeight=" + formatPercent(sectorRow.netWeight)
                );
            }
        }
    }

    public void validateDualPartialMarketCapPreview(
            String primaryStock,
            int primarySellQuantity,
            String secondaryStock,
            int secondarySellQuantity
    ) {
        LinkedHashMap<String, Integer> plan = new LinkedHashMap<>();
        plan.put(primaryStock, primarySellQuantity);
        plan.put(secondaryStock, secondarySellQuantity);

        SaleContext context = prepareSale(plan, true);
        openPortfolioPreview();
        // Market Cap has its own testcase; do not let a Holdings defect short-
        // circuit this validation before the Market Cap tab is exercised.
        validateCategoryPreview(context, "market cap");
    }

    public void validateMailCtaAvailability(String stockName, int targetSellQuantity) {
        prepareSale(singlePlan(stockName, targetSellQuantity), true);

        WebElement mail = waitForDisplayed(MAIL_MY_ORDER, SHORT_WAIT_SECONDS);
        if (mail == null) {
            throw new AssertionError("Mail my order CTA is not visible after selecting a holding");
        }
        try {
            if (!mail.isEnabled()) {
                throw new AssertionError("Mail my order CTA is visible but disabled");
            }
        } catch (Exception e) {
            throw new AssertionError("Unable to read Mail my order enabled state", e);
        }

        ReportLogger.pass("Mail my order CTA is visible and enabled after a valid sell selection");
    }

    // =========================================================
    // PUBLIC - CLEANUP
    // =========================================================

    public void recoverToSellPlannerSafely() {
        try {
            activateAdvisorAppIfNeeded();

            if (recoverToCleanSellPlannerLocally()) {
                sleep(650L);
                ReportLogger.debug("Sell Planner cleanup returned locally to clean baseline");
                return;
            }

            recoverToHubSafely();
        } catch (Exception e) {
            ReportLogger.debug("Sell Planner cleanup ignored: " + cleanError(e.getMessage()));
        } catch (AssertionError e) {
            ReportLogger.debug("Sell Planner cleanup assertion ignored: " + cleanError(e.getMessage()));
        }
    }

    // =========================================================
    // INTERNAL - SALE PREPARATION
    // =========================================================

    private SaleContext prepareSale(Map<String, Integer> requestedPlan, boolean requireExactQuantities) {
        Map<String, HoldingSnapshot> snapshots = captureHoldingSnapshots();
        return prepareSaleUsingSnapshots(requestedPlan, snapshots, requireExactQuantities);
    }

    private SaleContext prepareSaleUsingSnapshots(
            Map<String, Integer> requestedPlan,
            Map<String, HoldingSnapshot> snapshots,
            boolean requireExactQuantities
    ) {
        if (requestedPlan == null || requestedPlan.isEmpty()) {
            throw new IllegalArgumentException("Sell plan cannot be empty");
        }

        // Checkbox selection is most stable while the table is still in its default
        // Stocks/Rating/% Portfolio position. Select all requested holdings first,
        // verify the selected-count transition, and only then move the horizontal
        // table to Shares/Amount for stepper interaction.
        ensurePortfolioColumnsVisible();

        LinkedHashMap<String, Integer> actualPlan = new LinkedHashMap<>();
        LinkedHashMap<String, Double> rowAmounts = new LinkedHashMap<>();
        LinkedHashMap<String, Integer> totalSharesByStock = new LinkedHashMap<>();

        int expectedSelectedCount = readSelectedHoldingsCount();
        if (expectedSelectedCount != 0) {
            throw new AssertionError(
                    "Sell Planner testcase did not start from clean selection state"
                            + " | selected=" + expectedSelectedCount
            );
        }

        // Phase 1: select requested holdings on the unshifted table.
        for (String stock : requestedPlan.keySet()) {
            if (snapshots != null && !snapshots.containsKey(stock)) {
                throw new AssertionError("Runtime holding baseline is missing configured stock: " + stock);
            }
            selectHolding(stock, expectedSelectedCount);
            expectedSelectedCount++;
        }

        if (readSelectedHoldingsCount() != requestedPlan.size()) {
            throw new AssertionError(
                    "Selected holdings count did not match requested sell plan before quantity setup"
                            + " | expected=" + requestedPlan.size()
                            + " | actual=" + readSelectedHoldingsCount()
            );
        }

        // Phase 2: reveal the Shares/Amount columns once and handle all steppers there.
        ensureSharesColumnsVisible();

        for (Map.Entry<String, Integer> entry : requestedPlan.entrySet()) {
            String stock = entry.getKey();

            RowControls initialControls = readRowControls(stock);
            int totalShares = initialControls.quantity;
            totalSharesByStock.put(stock, totalShares);

            int requestedQuantity = entry.getValue() == null ? -1 : entry.getValue().intValue();
            int targetQuantity = requestedQuantity <= 0 ? totalShares : requestedQuantity;

            if (targetQuantity < 1 || targetQuantity > totalShares) {
                throw new AssertionError(
                        "Invalid sell quantity for current holding"
                                + " | stock=" + stock
                                + " | requested=" + targetQuantity
                                + " | available=" + totalShares
                );
            }

            setSellQuantity(stock, targetQuantity);
            RowControls settled = readRowControls(stock);

            if (requireExactQuantities && settled.quantity != targetQuantity) {
                throw new AssertionError(
                        "Sell quantity mismatch after stepper interaction"
                                + " | stock=" + stock
                                + " | expected=" + targetQuantity
                                + " | actual=" + settled.quantity
                );
            }

            actualPlan.put(stock, settled.quantity);
            rowAmounts.put(stock, settled.amount);
        }

        SaleContext context = new SaleContext(
                snapshots,
                actualPlan,
                rowAmounts,
                totalSharesByStock
        );

        validateSelectedWorthAgainstRows(context);
        return context;
    }

    private Map<String, Integer> singlePlan(String stockName, int quantity) {
        LinkedHashMap<String, Integer> plan = new LinkedHashMap<>();
        plan.put(stockName, quantity);
        return plan;
    }

    private Map<String, HoldingSnapshot> captureHoldingSnapshots() {
        List<String> missing = missingConfiguredHoldings();
        if (!missing.isEmpty()) {
            throw new AssertionError(
                    "SELL PLANNER TEST DATA PRECONDITION FAILED: configured holdings are unavailable"
                            + " | missing=" + missing
                            + " | emptyState=" + isEmptyHoldingsState()
                            + ". Restore/import the configured portfolio before running sell/preview scenarios."
            );
        }

        ensurePortfolioColumnsVisible();

        LinkedHashMap<String, HoldingSnapshot> snapshots = new LinkedHashMap<>();
        for (String stock : expectedHoldings()) {
            WebElement stockElement = waitForStock(stock);
            double currentWeight = readCurrentWeightFromHoldingRow(stockElement, stock);
            String metadata = readHoldingMetadata(stockElement);

            String marketCap = "";
            String sector = "";
            if (metadata.contains("|")) {
                String[] parts = metadata.split("\\|", 2);
                marketCap = normalizeSpaces(parts[0]);
                sector = normalizeSpaces(parts[1]);
            }

            snapshots.put(
                    stock,
                    new HoldingSnapshot(stock, currentWeight, marketCap, sector)
            );
        }

        double total = 0.0;
        for (HoldingSnapshot snapshot : snapshots.values()) {
            total += snapshot.currentWeight;
        }

        if (total < 80.0 || total > 120.0) {
            throw new AssertionError(
                    "Captured Sell Planner holding weights are not plausible"
                            + " | totalDisplayedWeight=" + formatPercent(total)
                            + " | snapshots=" + snapshots.values()
            );
        }

        ReportLogger.info(
                "Captured Sell Planner runtime holding baseline"
                        + " | holdings=" + snapshots.size()
                        + " | totalDisplayedWeight=" + formatPercent(total)
        );

        return snapshots;
    }

    private void selectHolding(String stockName, int expectedBeforeCount) {
        // Inspector evidence proves an important Flutter behavior:
        // - unchecked row checkbox: no standalone ImageView is exposed
        // - checked row checkbox: a clickable android.widget.ImageView appears
        // Therefore selection must not depend on locating an unchecked checkbox node.
        // Anchor to the semantic stock row, derive the checkbox column from the
        // viewport width, derive row Y from stock + subtitle semantics, and verify
        // the selected-count transition after each bounded W3C tap.
        ensurePortfolioColumnsVisible();

        int expectedAfter = expectedBeforeCount + 1;
        WebElement stock = waitForStock(stockName);
        Rectangle stockRect = stock.getRect();
        int stockCenterY = centerY(stockRect);

        Dimension size = driver.manage().window().getSize();
        int checkboxX = (int) Math.round(size.getWidth() * 0.0523);
        checkboxX = Math.max(24, Math.min(size.getWidth() - 24, checkboxX));

        WebElement metadata = findHoldingMetadataElement(stock);
        int metadataCenterY = metadata == null ? stockCenterY : centerY(metadata.getRect());
        int rowCenterY = metadata == null
                ? stockCenterY + Math.max(14, stockRect.getHeight())
                : (stockCenterY + metadataCenterY) / 2;

        int[] candidateYs = new int[]{
                rowCenterY,
                rowCenterY + 8,
                rowCenterY - 8,
                metadataCenterY - 4
        };

        ReportLogger.debug(
                "Resolved unchecked checkbox tap anchors from Inspector-backed row geometry"
                        + " | stock=" + stockName
                        + " | checkboxX=" + checkboxX
                        + " | stockCenterY=" + stockCenterY
                        + " | metadataCenterY=" + metadataCenterY
                        + " | rowCenterY=" + rowCenterY
        );

        int lastAttemptY = rowCenterY;
        for (int i = 0; i < candidateYs.length; i++) {
            int tapY = Math.max(1, Math.min(size.getHeight() - 2, candidateYs[i]));
            lastAttemptY = tapY;

            int before = readSelectedHoldingsCountSafe();
            if (before == expectedAfter) {
                ReportLogger.pass(
                        "Holding already selected after prior verified tap"
                                + " | stock=" + stockName
                                + " | selectedCount=" + expectedAfter
                );
                return;
            }
            if (before != expectedBeforeCount) {
                throw new AssertionError(
                        "Unexpected selected-count state before checkbox tap"
                                + " | stock=" + stockName
                                + " | expectedBefore=" + expectedBeforeCount
                                + " | actual=" + before
                );
            }

            tapViewportPoint(
                    checkboxX,
                    tapY,
                    "unchecked checkbox column for " + stockName + " | attempt=" + (i + 1)
            );

            if (waitForSelectedCount(expectedAfter, 2200L)) {
                sleep(ROW_SETTLE_MS);

                // After selection, Flutter exposes the green checked control as an
                // ImageView. This is a post-condition only; it is not required to
                // perform the unchecked-state tap.
                WebElement checkedImage = waitForHoldingCheckbox(stockName, 1200L);
                ReportLogger.pass(
                        "Holding selected successfully"
                                + " | stock=" + stockName
                                + " | selectedCount=" + expectedAfter
                                + " | method=row-anchored checkbox column"
                                + " | tapX=" + checkboxX
                                + " | tapY=" + tapY
                                + " | checkedImageVisible=" + (checkedImage != null)
                );
                return;
            }

            int after = readSelectedHoldingsCountSafe();
            if (after != expectedBeforeCount) {
                throw new AssertionError(
                        "Checkbox tap changed Sell Planner to an unexpected selection state"
                                + " | stock=" + stockName
                                + " | expectedAfter=" + expectedAfter
                                + " | actual=" + after
                                + " | tapX=" + checkboxX
                                + " | tapY=" + tapY
                );
            }
        }

        throw new AssertionError(
                "Holding selection did not update selected count after bounded row-anchored checkbox taps"
                        + " | stock=" + stockName
                        + " | expectedSelected=" + expectedAfter
                        + " | actualSelected=" + readSelectedHoldingsCountSafe()
                        + " | mailVisible=" + (firstDisplayed(MAIL_MY_ORDER) != null)
                        + " | checkboxX=" + checkboxX
                        + " | rowCenterY=" + rowCenterY
                        + " | lastTapY=" + lastAttemptY
                        + " | checkedImageVisible=" + (findHoldingCheckbox(stockName) != null)
        );
    }

    private WebElement findHoldingMetadataElement(WebElement stockElement) {
        Rectangle stockRect = stockElement.getRect();
        int stockY = centerY(stockRect);

        WebElement best = null;
        double bestScore = Double.MAX_VALUE;

        for (WebElement element : allDisplayedElements()) {
            try {
                String semantic = semanticValue(element);
                if (!semantic.contains("|")) {
                    continue;
                }

                Rectangle rect = element.getRect();
                int cy = centerY(rect);
                int cx = centerX(rect);
                int dy = cy - stockY;

                // The market-cap/sector subtitle is the semantic sibling directly
                // below the stock name. Prefer below-row candidates and keep the
                // search tightly bounded around the same stock column.
                if (dy < -10 || dy > 95) {
                    continue;
                }
                if (Math.abs(cx - centerX(stockRect)) > 180) {
                    continue;
                }

                double score = Math.abs(dy - 28) * 4.0 + Math.abs(cx - centerX(stockRect));
                if (score < bestScore) {
                    best = element;
                    bestScore = score;
                }
            } catch (Exception ignored) {
            }
        }
        return best;
    }

    private boolean waitForSelectedCount(int expected, long timeoutMs) {
        long deadline = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < deadline) {
            int current = readSelectedHoldingsCountSafe();
            if (current == expected) {
                return true;
            }
            assertDriverAlive("while waiting for Sell Planner selected-count update");
            sleep(POLL_MS);
        }
        return false;
    }

    private WebElement waitForHoldingCheckbox(String stockName, long timeoutMs) {
        long deadline = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < deadline) {
            WebElement checkbox = findHoldingCheckbox(stockName);
            if (checkbox != null) {
                return checkbox;
            }
            assertDriverAlive("while locating checkbox ImageView for " + stockName);
            sleep(180L);
        }
        return null;
    }

    private WebElement findHoldingCheckbox(String stockName) {
        WebElement stock = waitForStock(stockName);
        Rectangle stockRect = stock.getRect();
        int targetY = centerY(stockRect);

        WebElement best = null;
        double bestScore = Double.MAX_VALUE;

        // Exact Inspector evidence:
        //   class       = android.widget.ImageView
        //   content-desc= empty
        //   clickable   = true
        //   checkable   = false (even when visually selected)
        // The checkbox is the small ImageView immediately left of the stock row.
        // Use the exact class/attributes, then runtime row geometry to distinguish
        // the row checkbox from the header select-all checkbox and other icons.
        for (WebElement element : safeFindElements(AppiumBy.className("android.widget.ImageView"))) {
            try {
                if (element == null || !element.isDisplayed()) {
                    continue;
                }

                if (!semanticValue(element).isEmpty()) {
                    continue;
                }

                if (!Boolean.parseBoolean(element.getAttribute("clickable"))) {
                    continue;
                }

                Rectangle rect = element.getRect();
                if (rect.getWidth() < 12 || rect.getHeight() < 12
                        || rect.getWidth() > 110 || rect.getHeight() > 110) {
                    continue;
                }

                int cx = centerX(rect);
                int cy = centerY(rect);
                if (cx >= stockRect.getX()) {
                    continue;
                }

                // Stock-name semantics can sit slightly above the visual row center
                // because the market-cap/sector subtitle is a separate sibling. The
                // Inspector-confirmed checkbox center can therefore be below the stock
                // name center. Keep a generous row tolerance and choose the closest
                // ImageView; the header select-all checkbox is farther away vertically.
                int yTolerance = Math.max(110, stockRect.getHeight() * 3);
                if (Math.abs(cy - targetY) > yTolerance) {
                    continue;
                }

                int horizontalGap = Math.max(0, stockRect.getX() - cx);
                if (horizontalGap > 220) {
                    continue;
                }

                double score = Math.abs(cy - targetY) * 100.0 + horizontalGap;
                if (score < bestScore) {
                    best = element;
                    bestScore = score;
                }
            } catch (Exception ignored) {
            }
        }

        return best;
    }

    private void setSellQuantity(String stockName, int targetQuantity) {
        RowControls current = readRowControls(stockName);
        int guard = 0;

        while (current.quantity != targetQuantity) {
            guard++;
            if (guard > 300) {
                throw new AssertionError(
                        "Stepper guard exceeded while setting sell quantity"
                                + " | stock=" + stockName
                                + " | target=" + targetQuantity
                                + " | current=" + current.quantity
                );
            }

            int before = current.quantity;
            boolean decrement = before > targetQuantity;
            String label = decrement ? "minus" : "plus";
            int tapX = decrement ? current.minusTapX : current.plusTapX;
            int tapY = decrement ? current.minusTapY : current.plusTapY;

            tapViewportPoint(tapX, tapY, label + " for " + stockName);

            int expectedNext = decrement ? before - 1 : before + 1;
            waitForRowQuantity(stockName, expectedNext, 5000L);
            current = readRowControls(stockName);
        }

        ReportLogger.pass(
                "Sell quantity settled"
                        + " | stock=" + stockName
                        + " | quantity=" + targetQuantity
        );
    }

    private void waitForRowQuantity(String stockName, int expected, long timeoutMs) {
        long deadline = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < deadline) {
            try {
                RowControls controls = readRowControls(stockName);
                if (controls.quantity == expected) {
                    sleep(ROW_SETTLE_MS);
                    return;
                }
            } catch (AssertionError ignored) {
            }
            assertDriverAlive("while waiting for quantity change: " + stockName);
            sleep(180L);
        }

        throw new AssertionError(
                "Sell quantity did not update after stepper tap"
                        + " | stock=" + stockName
                        + " | expected=" + expected
        );
    }

    private RowControls readRowControls(String stockName) {
        ensureSharesColumnsVisible();
        WebElement stock = waitForStock(stockName);
        Rectangle stockRect = stock.getRect();
        int stockCenterY = centerY(stockRect);

        List<WebElement> elements = allDisplayedElements();

        WebElement plus = null;
        int plusDistance = Integer.MAX_VALUE;
        for (WebElement element : elements) {
            try {
                if (!PLUS_SEMANTIC.equals(semanticValue(element))) {
                    continue;
                }
                Rectangle rect = element.getRect();
                int distance = Math.abs(centerY(rect) - stockCenterY);
                if (distance <= 82 && distance < plusDistance) {
                    plus = element;
                    plusDistance = distance;
                }
            } catch (Exception ignored) {
            }
        }

        if (plus == null) {
            for (WebElement element : safeFindElements(AppiumBy.accessibilityId(PLUS_SEMANTIC))) {
                try {
                    Rectangle rect = element.getRect();
                    int distance = Math.abs(centerY(rect) - stockCenterY);
                    if (distance <= 82 && distance < plusDistance) {
                        plus = element;
                        plusDistance = distance;
                    }
                } catch (Exception ignored) {
                }
            }
        }

        if (plus == null) {
            throw new AssertionError("Plus stepper control not found for holding: " + stockName);
        }

        Rectangle plusRect = plus.getRect();
        int plusCenterX = centerX(plusRect);
        int rowY = centerY(plusRect);

        WebElement quantityElement = null;
        Integer quantity = null;
        double quantityScore = Double.MAX_VALUE;
        Double amount = null;
        double amountScore = Double.MAX_VALUE;

        for (WebElement element : elements) {
            try {
                String semantic = semanticValue(element);
                if (semantic.isEmpty()) {
                    continue;
                }

                Rectangle rect = element.getRect();
                int cy = centerY(rect);
                if (Math.abs(cy - rowY) > 55) {
                    continue;
                }

                int cx = centerX(rect);
                if (semantic.matches("\\d+") && cx < plusCenterX) {
                    int horizontalGap = plusCenterX - cx;
                    if (horizontalGap >= 8 && horizontalGap <= 230) {
                        double score = Math.abs(cy - rowY) * 5.0 + horizontalGap;
                        if (score < quantityScore) {
                            quantity = Integer.valueOf(semantic);
                            quantityElement = element;
                            quantityScore = score;
                        }
                    }
                }

                if (semantic.contains("₹") && cx > plusCenterX) {
                    Double parsed = parseMoney(semantic);
                    if (parsed != null) {
                        double score = Math.abs(cy - rowY) * 5.0
                                + Math.max(0, cx - plusCenterX) * 0.01;
                        if (score < amountScore) {
                            amount = parsed;
                            amountScore = score;
                        }
                    }
                }
            } catch (Exception ignored) {
            }
        }

        if (quantity == null || quantityElement == null) {
            throw new AssertionError(
                    "Share quantity semantic not found for holding: " + stockName
                            + " | plusFound=true"
            );
        }
        if (amount == null) {
            throw new AssertionError(
                    "Row amount semantic not found for holding: " + stockName
                            + " | quantity=" + quantity
                            + " | plusFound=true"
            );
        }

        Rectangle quantityRect = quantityElement.getRect();
        int quantityCenterX = centerX(quantityRect);
        int quantityCenterY = centerY(quantityRect);
        int plusGap = plusCenterX - quantityCenterX;
        if (plusGap < 18 || plusGap > 220) {
            throw new AssertionError(
                    "Unexpected Sell Planner quantity/+ geometry"
                            + " | stock=" + stockName
                            + " | quantityX=" + quantityCenterX
                            + " | plusX=" + plusCenterX
                            + " | gap=" + plusGap
            );
        }

        WebElement minus = findMinusNearRow(stockCenterY, quantityCenterX, elements);
        int minusTapX;
        int minusTapY;
        String minusMode;
        if (minus != null) {
            Rectangle minusRect = minus.getRect();
            minusTapX = centerX(minusRect);
            minusTapY = centerY(minusRect);
            minusMode = "semantic";
        } else {
            minusTapX = quantityCenterX - plusGap;
            minusTapY = (quantityCenterY + rowY) / 2;
            minusMode = "mirrored-from-quantity-plus";
        }

        Dimension viewport = driver.manage().window().getSize();
        if (minusTapX <= 0 || minusTapX >= viewport.getWidth()
                || minusTapY <= 0 || minusTapY >= viewport.getHeight()) {
            throw new AssertionError(
                    "Derived minus tap point is outside viewport"
                            + " | stock=" + stockName
                            + " | x=" + minusTapX
                            + " | y=" + minusTapY
            );
        }

        int plusTapX = plusCenterX;
        int plusTapY = rowY;

        ReportLogger.debug(
                "Resolved row stepper controls"
                        + " | stock=" + stockName
                        + " | quantity=" + quantity
                        + " | amount=₹" + formatNumber(amount.doubleValue())
                        + " | minusMode=" + minusMode
                        + " | minusTapX=" + minusTapX
                        + " | minusTapY=" + minusTapY
                        + " | plusTapX=" + plusTapX
                        + " | plusTapY=" + plusTapY
        );

        return new RowControls(
                minus,
                plus,
                quantity.intValue(),
                amount.doubleValue(),
                minusTapX,
                minusTapY,
                plusTapX,
                plusTapY
        );
    }

    private WebElement findMinusNearRow(
            int stockCenterY,
            int quantityCenterX,
            List<WebElement> displayedElements
    ) {
        WebElement best = null;
        int bestScore = Integer.MAX_VALUE;

        List<WebElement> candidates = new ArrayList<>();
        if (displayedElements != null) {
            candidates.addAll(displayedElements);
        }

        String[] minusSemantics = new String[]{
                ASCII_MINUS,
                UNICODE_MINUS,
                "‐",
                "‑",
                "‒",
                "–",
                "—",
                "﹣",
                "－"
        };
        for (String semantic : minusSemantics) {
            candidates.addAll(safeFindElements(AppiumBy.accessibilityId(semantic)));
        }

        for (WebElement element : candidates) {
            try {
                String semantic = semanticValue(element);
                if (!isMinusSemantic(semantic)) {
                    continue;
                }
                Rectangle rect = element.getRect();
                int cy = centerY(rect);
                int cx = centerX(rect);
                int yDistance = Math.abs(cy - stockCenterY);
                int xDistance = quantityCenterX - cx;
                if (yDistance > 82 || xDistance < 8 || xDistance > 230) {
                    continue;
                }
                int score = yDistance * 10 + xDistance;
                if (score < bestScore) {
                    best = element;
                    bestScore = score;
                }
            } catch (Exception ignored) {
            }
        }

        return best;
    }

    private boolean isMinusSemantic(String semantic) {
        if (semantic == null) {
            return false;
        }
        String value = semantic.trim();
        if (value.isEmpty() || value.length() > 3) {
            return false;
        }
        if (ASCII_MINUS.equals(value) || UNICODE_MINUS.equals(value)
                || "‐".equals(value) || "‑".equals(value)
                || "‒".equals(value) || "–".equals(value)
                || "—".equals(value) || "﹣".equals(value)
                || "－".equals(value)) {
            return true;
        }
        int cp = value.codePointAt(0);
        return Character.getType(cp) == Character.DASH_PUNCTUATION;
    }

    private void validateSelectedWorthAgainstRows(SaleContext context) {
        int selected = readSelectedHoldingsCount();
        if (selected != context.saleQuantities.size()) {
            throw new AssertionError(
                    "Selected holdings count mismatch"
                            + " | expected=" + context.saleQuantities.size()
                            + " | actual=" + selected
            );
        }

        double rowTotal = 0.0;
        for (Double amount : context.rowAmounts.values()) {
            if (amount != null) {
                rowTotal += amount.doubleValue();
            }
        }

        double topWorth = readTopSelectedWorth();
        double roundedRows = Math.round(rowTotal);
        if (Math.abs(topWorth - roundedRows) > SUMMARY_AMOUNT_TOLERANCE) {
            throw new AssertionError(
                    "TOTAL WORTH OF STOCK SELECTED does not match selected row amounts"
                            + " | topWorth=₹" + formatNumber(topWorth)
                            + " | selectedRows=₹" + formatNumber(rowTotal)
                            + " | roundedSelectedRows=₹" + formatNumber(roundedRows)
            );
        }

        if (waitForDisplayed(MAIL_MY_ORDER, SHORT_WAIT_SECONDS) == null) {
            throw new AssertionError("Mail my order CTA is not visible after valid selection");
        }

        ReportLogger.pass(
                "Selected holding summary validated"
                        + " | selected=" + selected
                        + " | topWorth=₹" + formatNumber(topWorth)
                        + " | rowTotal=₹" + formatNumber(rowTotal)
        );
    }

    // =========================================================
    // INTERNAL - PORTFOLIO PREVIEW
    // =========================================================

    private void openPortfolioPreview() {
        ReportLogger.step("Opening Sell Planner Portfolio preview");

        WebElement link = waitForDisplayed(PORTFOLIO_PREVIEW_LINK, SHORT_WAIT_SECONDS);
        if (link == null) {
            throw new AssertionError("Clickable Portfolio preview link is not visible");
        }
        clickElement(link, "Portfolio preview link");

        if (waitForDisplayed(PORTFOLIO_PREVIEW_PAGE_TITLE, PAGE_WAIT_SECONDS) == null) {
            throw new AssertionError("Sell Planner Portfolio preview page did not open");
        }

        if (waitForDisplayed(COMPANY_NAME_HEADER, SHORT_WAIT_SECONDS) == null
                || waitForDisplayed(CURRENT_WEIGHT_HEADER, SHORT_WAIT_SECONDS) == null
                || waitForDisplayed(NET_WEIGHT_HEADER, SHORT_WAIT_SECONDS) == null) {
            throw new AssertionError("Portfolio preview Holdings headers are incomplete");
        }

        sleep(800L);
        ReportLogger.pass("Sell Planner Portfolio preview opened successfully");
    }

    private void validateHoldingsPreview(SaleContext context) {
        ReportLogger.step("Validating Sell Planner Portfolio Preview Holdings calculations");

        Map<String, Double> expectedNet = computeExpectedNetWeights(context);
        Rectangle currentHeader = requireDisplayed(CURRENT_WEIGHT_HEADER, "Current wt. header").getRect();
        Rectangle netHeader = requireDisplayed(NET_WEIGHT_HEADER, "Net wt. header").getRect();
        int minY = Math.max(currentHeader.getY() + currentHeader.getHeight(), netHeader.getY() + netHeader.getHeight());

        for (HoldingSnapshot snapshot : context.snapshots.values()) {
            List<WebElement> companyNodes = displayedExactNodesBelow(snapshot.name, minY);
            if (companyNodes.size() != 1) {
                throw new AssertionError(
                        "Portfolio Preview contains duplicate/missing holding rows"
                                + " | holding=" + snapshot.name
                                + " | rowCount=" + companyNodes.size()
                );
            }

            WebElement company = companyNodes.get(0);
            int rowY = centerY(company.getRect());
            Double current = findPercentNearColumn(rowY, centerX(currentHeader), 90, minY);
            Double net = findPercentNearColumn(rowY, centerX(netHeader), 90, minY);

            if (current == null || net == null) {
                throw new AssertionError(
                        "Portfolio Preview row weights could not be read"
                                + " | holding=" + snapshot.name
                                + " | current=" + current
                                + " | net=" + net
                );
            }

            double expected = expectedNet.get(snapshot.name).doubleValue();
            assertPercentClose(
                    snapshot.currentWeight,
                    current.doubleValue(),
                    DEFAULT_WEIGHT_TOLERANCE,
                    "Current wt. mismatch for " + snapshot.name
            );
            assertPercentClose(
                    expected,
                    net.doubleValue(),
                    DEFAULT_WEIGHT_TOLERANCE,
                    "Net wt. mismatch for " + snapshot.name
            );
        }

        ReportLogger.pass("Portfolio Preview Holdings calculations validated against runtime sell plan");
    }

    private Map<String, Double> computeExpectedNetWeights(SaleContext context) {
        double displayedTotal = 0.0;
        for (HoldingSnapshot snapshot : context.snapshots.values()) {
            displayedTotal += snapshot.currentWeight;
        }

        double soldWeight = 0.0;
        for (Map.Entry<String, Integer> sale : context.saleQuantities.entrySet()) {
            HoldingSnapshot snapshot = context.snapshots.get(sale.getKey());
            Integer totalShares = context.totalSharesByStock.get(sale.getKey());
            if (snapshot == null || totalShares == null || totalShares.intValue() <= 0) {
                throw new AssertionError("Missing runtime baseline for sold holding: " + sale.getKey());
            }
            double fraction = sale.getValue().doubleValue() / totalShares.doubleValue();
            soldWeight += snapshot.currentWeight * fraction;
        }

        double remainingTotal = displayedTotal - soldWeight;
        if (remainingTotal <= 0.0) {
            throw new AssertionError("Sell plan leaves no valid portfolio denominator");
        }

        LinkedHashMap<String, Double> expected = new LinkedHashMap<>();
        for (HoldingSnapshot snapshot : context.snapshots.values()) {
            double fractionSold = 0.0;
            Integer sellQty = context.saleQuantities.get(snapshot.name);
            Integer totalShares = context.totalSharesByStock.get(snapshot.name);
            if (sellQty != null && totalShares != null && totalShares.intValue() > 0) {
                fractionSold = sellQty.doubleValue() / totalShares.doubleValue();
            }

            double remainingWeight = snapshot.currentWeight * (1.0 - fractionSold);
            expected.put(snapshot.name, remainingWeight / remainingTotal * 100.0);
        }

        ReportLogger.info(
                "Computed expected Sell Planner Holdings preview"
                        + " | displayedTotal=" + formatPercent(displayedTotal)
                        + " | soldWeight=" + formatPercent(soldWeight)
                        + " | remainingDenominator=" + formatPercent(remainingTotal)
        );

        return expected;
    }

    private void validateCategoryPreview(SaleContext context, String categoryType) {
        String normalized = categoryType.toLowerCase(Locale.US);
        if (!normalized.equals("sector") && !normalized.equals("market cap")) {
            throw new IllegalArgumentException("Unsupported preview category: " + categoryType);
        }

        String anchorStock = context.saleQuantities.keySet().iterator().next();
        HoldingSnapshot anchor = context.snapshots.get(anchorStock);
        String categoryLabel = normalized.equals("sector") ? anchor.sector : anchor.marketCap;

        if (categoryLabel == null || categoryLabel.trim().isEmpty()) {
            throw new AssertionError(
                    "Unable to derive " + categoryType + " for runtime holding: " + anchorStock
            );
        }

        tapPreviewTabByAnchor(normalized.equals("sector") ? "Sector" : "Market Cap");

        PreviewRow row = waitForPreviewRowByExactLabel(categoryLabel, 6000L);
        if (row == null) {
            throw new AssertionError(
                    "Portfolio Preview " + categoryType + " row not found after tab transition"
                            + " | category=" + categoryLabel
            );
        }

        double displayedTotal = 0.0;
        double categoryCurrent = 0.0;
        double allSoldWeight = 0.0;
        double categorySoldWeight = 0.0;

        for (HoldingSnapshot snapshot : context.snapshots.values()) {
            displayedTotal += snapshot.currentWeight;
            String value = normalized.equals("sector") ? snapshot.sector : snapshot.marketCap;
            boolean sameCategory = categoryLabel.equals(value);
            if (sameCategory) {
                categoryCurrent += snapshot.currentWeight;
            }

            Integer sellQty = context.saleQuantities.get(snapshot.name);
            Integer totalShares = context.totalSharesByStock.get(snapshot.name);
            if (sellQty != null && totalShares != null && totalShares.intValue() > 0) {
                double sold = snapshot.currentWeight
                        * (sellQty.doubleValue() / totalShares.doubleValue());
                allSoldWeight += sold;
                if (sameCategory) {
                    categorySoldWeight += sold;
                }
            }
        }

        double remainingTotal = displayedTotal - allSoldWeight;
        double expectedNet = (categoryCurrent - categorySoldWeight) / remainingTotal * 100.0;

        assertPercentClose(
                categoryCurrent,
                row.currentWeight,
                DEFAULT_WEIGHT_TOLERANCE + 0.20,
                categoryType + " Current wt. mismatch for " + categoryLabel
        );
        assertPercentClose(
                expectedNet,
                row.netWeight,
                DEFAULT_WEIGHT_TOLERANCE + 0.20,
                categoryType + " Net wt. mismatch for " + categoryLabel
        );

        ReportLogger.pass(
                "Portfolio Preview " + categoryType + " calculation validated"
                        + " | category=" + categoryLabel
                        + " | expectedNet=" + formatPercent(expectedNet)
                        + " | actualNet=" + formatPercent(row.netWeight)
        );
    }


    private boolean noOtherHoldingInSector(
            SaleContext context,
            String sector,
            String primaryStock,
            String secondaryStock
    ) {
        for (HoldingSnapshot snapshot : context.snapshots.values()) {
            if (snapshot.name.equals(primaryStock) || snapshot.name.equals(secondaryStock)) {
                continue;
            }
            if (sector.equals(snapshot.sector)) {
                return false;
            }
        }
        return true;
    }

    private PreviewRow waitForPreviewRowByExactLabel(String label, long timeoutMs) {
        long deadline = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < deadline) {
            PreviewRow row = findPreviewRowByExactLabel(label);
            if (row != null) {
                return row;
            }
            if (firstDisplayed(PORTFOLIO_PREVIEW_PAGE_TITLE) == null) {
                return null;
            }
            assertDriverAlive("while waiting for Portfolio Preview category row: " + label);
            sleep(250L);
        }
        return null;
    }

    private PreviewRow findPreviewRowByExactLabel(String label) {
        WebElement currentHeader = firstDisplayed(CURRENT_WEIGHT_HEADER);
        WebElement netHeader = firstDisplayed(NET_WEIGHT_HEADER);
        if (currentHeader == null || netHeader == null) {
            return null;
        }

        int minY = Math.max(
                currentHeader.getRect().getY() + currentHeader.getRect().getHeight(),
                netHeader.getRect().getY() + netHeader.getRect().getHeight()
        );

        List<WebElement> labels = displayedExactNodesBelow(label, minY);
        if (labels.size() != 1) {
            if (labels.size() > 1) {
                throw new AssertionError(
                        "Portfolio Preview contains duplicate category rows"
                                + " | label=" + label
                                + " | rowCount=" + labels.size()
                );
            }
            return null;
        }

        int rowY = centerY(labels.get(0).getRect());
        Double current = findPercentNearColumn(rowY, centerX(currentHeader.getRect()), 100, minY);
        Double net = findPercentNearColumn(rowY, centerX(netHeader.getRect()), 100, minY);
        if (current == null || net == null) {
            return null;
        }
        return new PreviewRow(label, current.doubleValue(), net.doubleValue());
    }

    private List<WebElement> displayedExactNodesBelow(String value, int minY) {
        List<WebElement> result = new ArrayList<>();
        for (WebElement element : safeFindElements(AppiumBy.accessibilityId(value))) {
            try {
                if (element.isDisplayed() && centerY(element.getRect()) > minY) {
                    result.add(element);
                }
            } catch (Exception ignored) {
            }
        }
        return result;
    }

    private Double findPercentNearColumn(int rowY, int targetX, int yTolerance, int minY) {
        Double best = null;
        double bestScore = Double.MAX_VALUE;

        for (WebElement element : allDisplayedElements()) {
            try {
                Rectangle rect = element.getRect();
                int cy = centerY(rect);
                if (cy <= minY || Math.abs(cy - rowY) > yTolerance) {
                    continue;
                }

                String semantic = semanticValue(element);
                if (!semantic.matches("[-+]?\\d+(?:\\.\\d+)?%")) {
                    continue;
                }

                int cx = centerX(rect);
                double score = Math.abs(cy - rowY) * 2.0 + Math.abs(cx - targetX);
                if (score < bestScore) {
                    best = parsePercent(semantic);
                    bestScore = score;
                }
            } catch (Exception ignored) {
            }
        }
        return best;
    }

    private void tapPreviewTabByAnchor(String tabName) {
        WebElement previewAnchor = waitForDisplayed(PORTFOLIO_PREVIEW_PAGE_TITLE, SHORT_WAIT_SECONDS);
        if (previewAnchor == null) {
            throw new AssertionError("Portfolio preview page anchor is not visible for tab: " + tabName);
        }

        /*
         * Same proven runtime-anchor strategy as Investment Planner v15.4.
         * On this Flutter preview header, Holdings / Sector / Market Cap are visual
         * controls and are not guaranteed to be exposed as individual accessibility
         * nodes. The Portfolio preview View is the stable runtime anchor, so derive
         * the tab tap point from its live rectangle instead of requiring a semantic
         * locator for the tab itself.
         */
        double xRatio;
        switch (tabName.toLowerCase(Locale.US)) {
            case "holdings":
                xRatio = 0.14;
                break;
            case "sector":
                xRatio = 0.38;
                break;
            case "market cap":
                xRatio = 0.64;
                break;
            default:
                throw new IllegalArgumentException("Unsupported Portfolio Preview tab: " + tabName);
        }

        Rectangle rect = previewAnchor.getRect();
        int x = rect.getX() + (int) Math.round(rect.getWidth() * xRatio);
        int y = rect.getY() + (int) Math.round(rect.getHeight() * 0.74);

        tapViewportPoint(x, y, "Portfolio Preview tab: " + tabName);
        sleep(650L);

        if (firstDisplayed(PORTFOLIO_PREVIEW_PAGE_TITLE) == null) {
            throw new AssertionError("Portfolio Preview route disappeared after tapping tab: " + tabName);
        }
    }

    // =========================================================
    // INTERNAL - LANDING TABLE CAPTURE
    // =========================================================

    private double readCurrentWeightFromHoldingRow(WebElement stockElement, String stockName) {
        Rectangle stockRect = stockElement.getRect();
        int targetY = centerY(stockRect);

        Double best = null;
        double bestScore = Double.MAX_VALUE;

        for (WebElement element : allDisplayedElements()) {
            try {
                String semantic = semanticValue(element);
                if (!semantic.matches("[-+]?\\d+(?:\\.\\d+)?%")) {
                    continue;
                }

                Rectangle rect = element.getRect();
                int cy = centerY(rect);
                int cx = centerX(rect);
                if (Math.abs(cy - targetY) > 70 || cx <= centerX(stockRect)) {
                    continue;
                }

                double score = Math.abs(cy - targetY) * 2.0 - cx * 0.001;
                if (score < bestScore) {
                    best = parsePercent(semantic);
                    bestScore = score;
                }
            } catch (Exception ignored) {
            }
        }

        if (best == null) {
            throw new AssertionError("% Portfolio value not found for holding: " + stockName);
        }
        return best.doubleValue();
    }

    private String readHoldingMetadata(WebElement stockElement) {
        Rectangle stockRect = stockElement.getRect();
        int targetY = centerY(stockRect);

        String best = "";
        int bestDistance = Integer.MAX_VALUE;

        for (WebElement element : allDisplayedElements()) {
            try {
                String semantic = semanticValue(element);
                if (!semantic.contains("|")) {
                    continue;
                }
                Rectangle rect = element.getRect();
                int distance = Math.abs(centerY(rect) - targetY);
                if (distance <= 75 && distance < bestDistance) {
                    best = semantic;
                    bestDistance = distance;
                }
            } catch (Exception ignored) {
            }
        }
        return best;
    }

    private WebElement waitForStock(String stockName) {
        WebElement stock = waitForDisplayed(AppiumBy.accessibilityId(stockName), SHORT_WAIT_SECONDS);
        if (stock == null) {
            throw new AssertionError("Expected Sell Planner holding is not visible: " + stockName);
        }
        return stock;
    }

    // =========================================================
    // INTERNAL - TABLE HORIZONTAL W3C DRAG
    // =========================================================

    private void ensureSharesColumnsVisible() {
        if (isSharesColumnStateVisible()) {
            return;
        }

        for (int attempt = 1; attempt <= 3; attempt++) {
            swipeHoldingsTable(true, attempt);
            sleep(750L);
            if (isSharesColumnStateVisible()) {
                ReportLogger.pass(
                        "Sell Planner Shares/Amount columns revealed"
                                + " | horizontalSwipes=" + attempt
                );
                return;
            }
        }

        throw new AssertionError("Sell Planner Shares/Amount columns could not be revealed after bounded W3C drags");
    }

    private void ensurePortfolioColumnsVisible() {
        if (isPortfolioColumnStateVisible()) {
            return;
        }

        for (int attempt = 1; attempt <= 3; attempt++) {
            swipeHoldingsTable(false, attempt);
            sleep(750L);
            if (isPortfolioColumnStateVisible()) {
                ReportLogger.debug(
                        "Sell Planner % Portfolio column restored"
                                + " | horizontalSwipes=" + attempt
                );
                return;
            }
        }

        throw new AssertionError("Sell Planner % Portfolio column could not be restored after bounded W3C drags");
    }


    private boolean isSharesColumnStateVisible() {
        boolean headersVisible =
                (firstDisplayed(SHARES_HEADER) != null && firstDisplayed(AMOUNT_HEADER) != null)
                        || (hasDisplayedSemantic("Shares") && hasDisplayedSemantic("Amount"));
        if (!headersVisible) {
            return false;
        }

        /*
         * Headers can become visible one drag before the row Amount values are
         * actually inside the viewport. SP_009 proved that stopping on headers alone
         * can leave quantity/+ visible while the rupee amount is still clipped.
         * Require real row data before declaring the Shares/Amount state ready.
         */
        List<WebElement> elements = allDisplayedElements();
        for (WebElement plus : elements) {
            try {
                if (!PLUS_SEMANTIC.equals(semanticValue(plus))) {
                    continue;
                }
                Rectangle plusRect = plus.getRect();
                int plusX = centerX(plusRect);
                int plusY = centerY(plusRect);

                for (WebElement amount : elements) {
                    try {
                        String semantic = semanticValue(amount);
                        if (semantic == null || !semantic.contains("₹")) {
                            continue;
                        }
                        Rectangle amountRect = amount.getRect();
                        if (centerX(amountRect) > plusX
                                && Math.abs(centerY(amountRect) - plusY) <= 60) {
                            return true;
                        }
                    } catch (Exception ignored) {
                    }
                }
            } catch (Exception ignored) {
            }
        }

        return false;
    }

    private boolean isPortfolioColumnStateVisible() {
        if (firstDisplayed(PERCENT_PORTFOLIO_HEADER) != null) {
            return true;
        }
        return hasDisplayedSemantic("% Portfolio");
    }

    private boolean hasDisplayedSemantic(String expected) {
        for (WebElement element : allDisplayedElements()) {
            try {
                String value = semanticValue(element);
                if (value.equalsIgnoreCase(expected)
                        || value.toLowerCase(Locale.US).contains(expected.toLowerCase(Locale.US))) {
                    return true;
                }
            } catch (Exception ignored) {
            }
        }
        return false;
    }

    private void swipeHoldingsTable(boolean left, int attempt) {
        WebElement scroll = firstDisplayed(HORIZONTAL_SCROLL_VIEW);
        if (scroll == null) {
            throw new AssertionError("Sell Planner HorizontalScrollView is not visible");
        }

        try {
            Rectangle rect = scroll.getRect();
            Dimension size = driver.manage().window().getSize();

            int firstStockY = rect.getY() + rect.getHeight() / 3;
            String[] expected = expectedHoldings();
            if (expected.length > 0) {
                WebElement firstStock = firstDisplayed(AppiumBy.accessibilityId(expected[0]));
                if (firstStock != null) {
                    firstStockY = centerY(firstStock.getRect());
                }
            }

            int y = Math.max(rect.getY() + 25, Math.min(rect.getY() + rect.getHeight() - 25, firstStockY));
            int startX = rect.getX() + (int) Math.round(rect.getWidth() * (left ? 0.74 : 0.32));
            int endX = rect.getX() + (int) Math.round(rect.getWidth() * (left ? 0.32 : 0.74));

            startX = Math.max(20, Math.min(size.getWidth() - 20, startX));
            endX = Math.max(20, Math.min(size.getWidth() - 20, endX));

            PointerInput finger = new PointerInput(PointerInput.Kind.TOUCH, "sellPlannerHorizontalFinger");
            Sequence swipe = new Sequence(finger, 1);
            swipe.addAction(finger.createPointerMove(Duration.ZERO, PointerInput.Origin.viewport(), startX, y));
            swipe.addAction(finger.createPointerDown(PointerInput.MouseButton.LEFT.asArg()));
            swipe.addAction(
                    finger.createPointerMove(
                            Duration.ofMillis(520L),
                            PointerInput.Origin.viewport(),
                            endX,
                            y
                    )
            );
            swipe.addAction(finger.createPointerUp(PointerInput.MouseButton.LEFT.asArg()));
            driver.perform(Collections.singletonList(swipe));

            if (firstDisplayed(SELL_PLANNER_TITLE) == null) {
                throw new AssertionError("Sell Planner title disappeared during table horizontal drag");
            }

            ReportLogger.step(
                    "Swiped Sell Planner holdings table " + (left ? "left" : "right")
                            + " using safe interior W3C drag"
                            + " | attempt=" + attempt
                            + " | startXRatio=" + (left ? "0.74" : "0.32")
                            + " | endXRatio=" + (left ? "0.32" : "0.74")
            );
        } catch (Exception e) {
            throw new AssertionError(
                    "Sell Planner horizontal table drag failed"
                            + " | direction=" + (left ? "left" : "right")
                            + " | error=" + cleanError(e.getMessage()),
                    e
            );
        }
    }

    // =========================================================
    // INTERNAL - CLEAN BASELINE / RECOVERY
    // =========================================================

    private boolean recoverToCleanSellPlannerLocally() {
        try {
            if (isCleanSellPlannerReady()) {
                return true;
            }

            if (firstDisplayed(HUB_TAB) != null) {
                ReportLogger.debug("Hub is already visible; Sell Planner local recovery not needed");
                return false;
            }

            if (firstDisplayed(PORTFOLIO_PREVIEW_PAGE_TITLE) != null) {
                ReportLogger.debug("Sell Planner recovery: Portfolio Preview -> Sell Planner");
                driver.navigate().back();
                if (!waitForPlannerVisible(8000L)) {
                    return false;
                }
            }

            if (firstDisplayed(GUIDELINES_SHEET_TITLE) != null) {
                ReportLogger.debug("Sell Planner recovery: Guidelines sheet -> Sell Planner");
                driver.navigate().back();
                if (!waitForPlannerVisible(6000L)) {
                    return false;
                }
            }

            if (firstDisplayed(CHOOSE_INVESTOR) != null) {
                ReportLogger.debug("Sell Planner recovery: Choose Investor -> Sell Planner");
                driver.navigate().back();
                if (!waitForPlannerVisible(6000L)) {
                    return false;
                }
            }

            if (firstDisplayed(SELL_PLANNER_TITLE) != null) {
                if (isCleanSellPlannerReady()) {
                    return true;
                }

                // A selected Sell Planner state replaces the initial CTA with Mail my order.
                // This semantic transition is much more reliable than parsing the dynamic
                // "0/1 of N holdings" summary, so use it as the dirty-state classifier.
                if (firstDisplayed(MAIL_MY_ORDER) != null) {
                    ReportLogger.debug(
                            "Sell Planner retains selected holdings; returning once to Hub for a clean baseline"
                    );
                    driver.navigate().back();
                    waitForHubVisible(8000L);
                    return false;
                }

                // The planner route can rebuild semantics briefly after Back/navigation.
                // Wait for the stable structural baseline instead of calling AuthHelper or
                // performing repeated Back actions.
                long deadline = System.currentTimeMillis() + 5000L;
                while (System.currentTimeMillis() < deadline) {
                    if (isCleanSellPlannerReady()) {
                        return true;
                    }
                    if (firstDisplayed(HUB_TAB) != null) {
                        return false;
                    }
                    if (firstDisplayed(MAIL_MY_ORDER) != null) {
                        ReportLogger.debug(
                                "Sell Planner selected state appeared during settle; returning once to Hub"
                        );
                        driver.navigate().back();
                        waitForHubVisible(8000L);
                        return false;
                    }
                    sleep(200L);
                }

                // We are visibly on Sell Planner but cannot prove the clean baseline.
                // Do not invoke AuthHelper from this deep route. Use one controlled Back
                // to Hub and let normal Hub navigation reopen Sell Planner cleanly.
                ReportLogger.debug(
                        "Sell Planner is visible but clean baseline is not fully exposed; "
                                + "using one controlled Back to Hub"
                );
                driver.navigate().back();
                waitForHubVisible(8000L);
                return false;
            }

            return false;
        } catch (Exception e) {
            ReportLogger.debug("Sell Planner local recovery could not complete: " + cleanError(e.getMessage()));
            return false;
        }
    }

    private boolean isCleanSellPlannerReady() {
        // Navigation/recovery is resolved in two phases:
        // 1) stable structural anchors prove that Sell Planner opened;
        // 2) one terminal body state must resolve: populated holdings or empty/import.
        //
        // The initial CTA is NOT a page-readiness requirement because it does not exist
        // for investors with zero holdings.
        if (!isSellPlannerStructureReady() || firstDisplayed(MAIL_MY_ORDER) != null) {
            return false;
        }

        if (firstDisplayed(INITIAL_CTA) != null
                || firstDisplayed(STOCKS_HEADER) != null
                || firstDisplayed(IMPORT_YOUR_STOCKS) != null
                || hasAnyDisplayedExpectedHolding()) {
            return true;
        }

        // Source fallback is used only after the cheap targeted locators above fail.
        return isEmptyHoldingsState();
    }

    private boolean isSellPlannerStructureReady() {
        return firstDisplayed(SELL_PLANNER_TITLE) != null
                && firstDisplayed(TOTAL_WORTH_LABEL) != null
                && firstDisplayed(YOUR_HOLDINGS) != null;
    }

    private boolean hasAnyDisplayedExpectedHolding() {
        for (String holding : expectedHoldings()) {
            if (firstDisplayed(AppiumBy.accessibilityId(holding)) != null) {
                return true;
            }
        }
        return false;
    }

    private boolean isEmptyHoldingsState() {
        if (firstDisplayed(IMPORT_YOUR_STOCKS) != null
                || firstDisplayed(EMPTY_HOLDINGS_MESSAGE) != null) {
            return true;
        }

        String source = safePageSource().toLowerCase(Locale.ROOT);
        return source.contains("underlying data is unavailable")
                && source.contains("import your stocks");
    }

    private void waitForCleanSellPlannerReadyOrThrow(String source) {
        long deadline = System.currentTimeMillis() + Duration.ofSeconds(PAGE_WAIT_SECONDS).toMillis();
        while (System.currentTimeMillis() < deadline) {
            if (isCleanSellPlannerReady()) {
                return;
            }
            assertDriverAlive("while waiting for clean Sell Planner after " + source);
            sleep(POLL_MS);
        }

        throw new AssertionError(
                "Sell Planner clean baseline did not become ready after " + source
                        + " | title=" + (firstDisplayed(SELL_PLANNER_TITLE) != null)
                        + " | totalWorthLabel=" + (firstDisplayed(TOTAL_WORTH_LABEL) != null)
                        + " | yourHoldings=" + (firstDisplayed(YOUR_HOLDINGS) != null)
                        + " | stocksHeader=" + (firstDisplayed(STOCKS_HEADER) != null)
                        + " | initialCta=" + (firstDisplayed(INITIAL_CTA) != null)
                        + " | importStocks=" + (firstDisplayed(IMPORT_YOUR_STOCKS) != null)
                        + " | emptyState=" + isEmptyHoldingsState()
                        + " | mailCta=" + (firstDisplayed(MAIL_MY_ORDER) != null)
        );
    }

    private void waitForPlannerVisibleOrThrow(String source) {
        if (!waitForPlannerVisible(Duration.ofSeconds(PAGE_WAIT_SECONDS).toMillis())) {
            throw new AssertionError("Sell Planner did not become visible after " + source);
        }
    }

    private boolean waitForPlannerVisible(long timeoutMs) {
        long deadline = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < deadline) {
            if (firstDisplayed(SELL_PLANNER_TITLE) != null) {
                return true;
            }
            if (firstDisplayed(HUB_TAB) != null) {
                return false;
            }
            assertDriverAlive("while waiting for Sell Planner route");
            sleep(POLL_MS);
        }
        return false;
    }

    private void recoverToHubSafely() {
        try {
            if (firstDisplayed(HUB_TAB) != null) {
                ReportLogger.debug("Sell Planner cleanup is already at Hub");
                return;
            }

            if (firstDisplayed(PORTFOLIO_PREVIEW_PAGE_TITLE) != null
                    || firstDisplayed(GUIDELINES_SHEET_TITLE) != null
                    || firstDisplayed(CHOOSE_INVESTOR) != null) {
                driver.navigate().back();
                waitForPlannerVisible(6000L);
            }

            if (firstDisplayed(SELL_PLANNER_TITLE) != null) {
                driver.navigate().back();
                if (waitForHubVisible(8000L)) {
                    ReportLogger.debug("Sell Planner cleanup returned to Hub with one controlled Back");
                    return;
                }
            }

            if (firstDisplayed(HUB_TAB) != null) {
                return;
            }

            ReportLogger.debug("Sell Planner cleanup could not prove Hub state; no blind Back loop was used");
        } catch (Exception e) {
            ReportLogger.debug("Sell Planner Hub cleanup ignored: " + cleanError(e.getMessage()));
        }
    }

    private boolean waitForHubVisible(long timeoutMs) {
        long deadline = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < deadline) {
            if (firstDisplayed(HUB_TAB) != null) {
                return true;
            }
            assertDriverAlive("while waiting for Hub after Sell Planner Back");
            sleep(POLL_MS);
        }
        return false;
    }

    // =========================================================
    // INTERNAL - SUMMARY / GEOMETRY READERS
    // =========================================================

    private int readSelectedHoldingsCountSafe() {
        try {
            return readSelectedHoldingsCount();
        } catch (Exception e) {
            return -1;
        } catch (AssertionError e) {
            return -1;
        }
    }

    private int readSelectedHoldingsCount() {
        WebElement marker = firstDisplayed(SELECTED_COUNT_MARKER);
        if (marker == null) {
            throw new AssertionError("Selected holdings summary marker is not visible");
        }

        String semantic = semanticValue(marker);
        Matcher inline = Pattern.compile("(\\d+)\\s+of\\s+(\\d+)\\s+holdings\\s+ready\\s+to\\s+sell", Pattern.CASE_INSENSITIVE)
                .matcher(semantic);
        if (inline.find()) {
            return Integer.parseInt(inline.group(1));
        }

        Matcher totalMatcher = Pattern.compile("(?:of\\s+)?(\\d+)\\s+holdings\\s+ready\\s+to\\s+sell", Pattern.CASE_INSENSITIVE)
                .matcher(semantic);
        int total = 99;
        if (totalMatcher.find()) {
            total = Integer.parseInt(totalMatcher.group(1));
        }

        Rectangle markerRect = marker.getRect();
        int markerY = centerY(markerRect);
        WebElement best = null;
        int bestDistance = Integer.MAX_VALUE;

        for (WebElement element : allDisplayedElements()) {
            try {
                String value = semanticValue(element);
                if (!value.matches("\\d+")) {
                    continue;
                }
                int parsed = Integer.parseInt(value);
                if (parsed < 0 || parsed > total) {
                    continue;
                }

                Rectangle rect = element.getRect();
                if (Math.abs(centerY(rect) - markerY) > 45 || centerX(rect) >= markerRect.getX()) {
                    continue;
                }

                int distance = markerRect.getX() - centerX(rect);
                if (distance < bestDistance) {
                    best = element;
                    bestDistance = distance;
                }
            } catch (Exception ignored) {
            }
        }

        if (best != null) {
            return Integer.parseInt(semanticValue(best));
        }

        // Flutter can expose the leading count as a sibling that UiAutomator2 does
        // not return consistently through findElements. Fall back to the XML source
        // and read the nearest numeric content-desc preceding the summary marker.
        try {
            String source = driver.getPageSource();
            String lower = source.toLowerCase(Locale.ROOT);
            String needle = "holdings ready to sell";
            int markerIndex = lower.indexOf(needle);
            if (markerIndex > 0) {
                int from = Math.max(0, markerIndex - 1400);
                String prefix = source.substring(from, markerIndex);
                Matcher numeric = Pattern.compile("content-desc=\"(\\d+)\"").matcher(prefix);
                Integer nearest = null;
                while (numeric.find()) {
                    int parsed = Integer.parseInt(numeric.group(1));
                    if (parsed >= 0 && parsed <= total) {
                        nearest = parsed;
                    }
                }
                if (nearest != null) {
                    return nearest;
                }
            }
        } catch (Exception ignored) {
        }

        throw new AssertionError("Unable to read selected holdings count from Sell Planner summary");
    }

    private double readTopSelectedWorth() {
        WebElement label = firstDisplayed(TOTAL_WORTH_LABEL);
        if (label == null) {
            throw new AssertionError("TOTAL WORTH OF STOCK SELECTED label is not visible");
        }

        Rectangle labelRect = label.getRect();
        WebElement guidelines = firstDisplayed(GUIDELINES_LINK);
        int maxY = guidelines == null
                ? labelRect.getY() + 240
                : guidelines.getRect().getY();
        int minY = labelRect.getY();

        Double best = null;
        int bestDistance = Integer.MAX_VALUE;

        for (WebElement element : allDisplayedElements()) {
            try {
                String semantic = semanticValue(element);
                if (!semantic.contains("₹")) {
                    continue;
                }
                Double money = parseMoney(semantic);
                if (money == null) {
                    continue;
                }

                Rectangle rect = element.getRect();
                int cy = centerY(rect);
                if (cy < minY || cy >= maxY) {
                    continue;
                }

                int distance = Math.abs(cy - (labelRect.getY() + labelRect.getHeight()));
                if (distance < bestDistance) {
                    best = money;
                    bestDistance = distance;
                }
            } catch (Exception ignored) {
            }
        }

        if (best == null) {
            throw new AssertionError("Unable to read TOTAL WORTH OF STOCK SELECTED value");
        }
        return best.doubleValue();
    }

    // =========================================================
    // INTERNAL - HUB / COMMON ELEMENT HELPERS
    // =========================================================

    private WebElement findSellPlannerHubTileWithBoundedScroll() {
        WebElement tile = firstDisplayed(SELL_PLANNER_HUB_TILE);
        if (tile != null) {
            return tile;
        }

        for (int attempt = 1; attempt <= 4; attempt++) {
            swipeVerticalW3C(0.76, 0.30, 500L);
            sleep(600L);
            tile = firstDisplayed(SELL_PLANNER_HUB_TILE);
            if (tile != null) {
                ReportLogger.step("Sell Planner Hub tile found after bounded Hub scroll | attempt=" + attempt);
                return tile;
            }
        }
        return null;
    }

    private WebElement requireDisplayed(By locator, String label) {
        WebElement element = waitForDisplayed(locator, SHORT_WAIT_SECONDS);
        if (element == null) {
            throw new AssertionError(label + " is not visible");
        }
        ReportLogger.pass(label + " is visible");
        return element;
    }

    private WebElement waitForDisplayed(By locator, int timeoutSeconds) {
        long deadline = System.currentTimeMillis() + Duration.ofSeconds(timeoutSeconds).toMillis();
        while (System.currentTimeMillis() < deadline) {
            WebElement element = firstDisplayed(locator);
            if (element != null) {
                return element;
            }
            assertDriverAlive("while waiting for displayed element");
            sleep(POLL_MS);
        }
        return null;
    }

    private WebElement firstDisplayed(By locator) {
        for (WebElement element : safeFindElements(locator)) {
            try {
                if (element != null && element.isDisplayed()) {
                    return element;
                }
            } catch (Exception ignored) {
            }
        }
        return null;
    }

    private List<WebElement> safeFindElements(By locator) {
        try {
            List<WebElement> elements = driver.findElements(locator);
            return elements == null ? Collections.<WebElement>emptyList() : elements;
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    private List<WebElement> allDisplayedElements() {
        List<WebElement> result = new ArrayList<>();
        try {
            for (WebElement element : driver.findElements(By.xpath("//*"))) {
                try {
                    if (element != null && element.isDisplayed()) {
                        result.add(element);
                    }
                } catch (Exception ignored) {
                }
            }
        } catch (Exception e) {
            ReportLogger.debug("Dynamic hierarchy read skipped: " + cleanError(e.getMessage()));
        }
        return result;
    }

    private void clickElement(WebElement element, String label) {
        if (element == null) {
            throw new AssertionError("Cannot tap null element: " + label);
        }

        try {
            if (element.isDisplayed() && element.isEnabled()) {
                String clickable = element.getAttribute("clickable");
                if (Boolean.parseBoolean(clickable)) {
                    element.click();
                    ReportLogger.step("Tapped using element click: " + label);
                    return;
                }
            }
        } catch (Exception ignored) {
        }

        try {
            Map<String, Object> args = new LinkedHashMap<>();
            args.put("elementId", ((RemoteWebElement) element).getId());
            ((JavascriptExecutor) driver).executeScript("mobile: clickGesture", args);
            ReportLogger.step("Tapped using element-based mobile: clickGesture: " + label);
            return;
        } catch (Exception ignored) {
        }

        try {
            tapDynamicElementCenter(element, label);
        } catch (Exception e) {
            throw new AssertionError("Unable to tap " + label + " | error=" + cleanError(e.getMessage()), e);
        }
    }

    private void tapDynamicElementCenter(WebElement element, String label) {
        Rectangle rect = element.getRect();
        tapViewportPoint(centerX(rect), centerY(rect), label);
    }

    private void tapViewportPoint(int x, int y, String label) {
        try {
            PointerInput finger = new PointerInput(PointerInput.Kind.TOUCH, "sellPlannerTapFinger");
            Sequence tap = new Sequence(finger, 1);
            tap.addAction(finger.createPointerMove(Duration.ZERO, PointerInput.Origin.viewport(), x, y));
            tap.addAction(finger.createPointerDown(PointerInput.MouseButton.LEFT.asArg()));
            tap.addAction(finger.createPointerUp(PointerInput.MouseButton.LEFT.asArg()));
            driver.perform(Collections.singletonList(tap));
            ReportLogger.step("Tapped runtime anchored position: " + label);
        } catch (Exception e) {
            throw new AssertionError(
                    "Unable to tap runtime point for " + label
                            + " | x=" + x
                            + " | y=" + y
                            + " | error=" + cleanError(e.getMessage()),
                    e
            );
        }
    }

    private void swipeVerticalW3C(double startYRatio, double endYRatio, long durationMs) {
        try {
            Dimension size = driver.manage().window().getSize();
            int x = size.getWidth() / 2;
            int startY = (int) Math.round(size.getHeight() * startYRatio);
            int endY = (int) Math.round(size.getHeight() * endYRatio);

            PointerInput finger = new PointerInput(PointerInput.Kind.TOUCH, "sellPlannerVerticalFinger");
            Sequence swipe = new Sequence(finger, 1);
            swipe.addAction(finger.createPointerMove(Duration.ZERO, PointerInput.Origin.viewport(), x, startY));
            swipe.addAction(finger.createPointerDown(PointerInput.MouseButton.LEFT.asArg()));
            swipe.addAction(
                    finger.createPointerMove(
                            Duration.ofMillis(durationMs),
                            PointerInput.Origin.viewport(),
                            x,
                            endY
                    )
            );
            swipe.addAction(finger.createPointerUp(PointerInput.MouseButton.LEFT.asArg()));
            driver.perform(Collections.singletonList(swipe));
        } catch (Exception e) {
            throw new AssertionError("Sell Planner W3C vertical scroll failed", e);
        }
    }

    // =========================================================
    // INTERNAL - VALUE / DRIVER HELPERS
    // =========================================================

    private String[] expectedHoldings() {
        String configured = System.getProperty("sell.expectedHoldings", "").trim();
        if (configured.isEmpty()) {
            return DEFAULT_EXPECTED_HOLDINGS.clone();
        }

        String[] raw = configured.split(",");
        List<String> cleaned = new ArrayList<>();
        for (String value : raw) {
            String item = value.trim();
            if (!item.isEmpty()) {
                cleaned.add(item);
            }
        }
        return cleaned.toArray(new String[cleaned.size()]);
    }

    private String semanticValue(WebElement element) {
        if (element == null) {
            return "";
        }

        String[] attrs = new String[]{"content-desc", "text", "name"};
        for (String attr : attrs) {
            try {
                String value = element.getAttribute(attr);
                if (value != null && !value.trim().isEmpty()) {
                    return normalizeSpaces(value);
                }
            } catch (Exception ignored) {
            }
        }

        try {
            return normalizeSpaces(element.getText());
        } catch (Exception ignored) {
            return "";
        }
    }

    private String normalizeSpaces(String value) {
        if (value == null) {
            return "";
        }
        return value.replaceAll("\\s+", " ").trim();
    }

    private Double parseMoney(String raw) {
        if (raw == null) {
            return null;
        }
        String cleaned = raw.replaceAll("[^0-9.\\-]", "");
        if (cleaned.isEmpty() || cleaned.equals("-") || cleaned.equals(".")) {
            return null;
        }
        try {
            return Double.valueOf(cleaned);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Double parsePercent(String raw) {
        if (raw == null) {
            return null;
        }
        String cleaned = raw.replaceAll("[^0-9.\\-]", "");
        if (cleaned.isEmpty() || cleaned.equals("-") || cleaned.equals(".")) {
            return null;
        }
        try {
            return Double.valueOf(cleaned);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private void assertPercentClose(double expected, double actual, double tolerance, String message) {
        if (Math.abs(expected - actual) > tolerance) {
            throw new AssertionError(
                    message
                            + " | expected=" + formatPercent(expected)
                            + " | actual=" + formatPercent(actual)
                            + " | tolerance=±" + formatNumber(tolerance) + "%"
            );
        }
    }

    private String formatPercent(double value) {
        return String.format(Locale.US, "%.2f%%", value);
    }

    private String formatNumber(double value) {
        return String.format(Locale.US, "%.2f", value);
    }

    private int centerX(Rectangle rect) {
        return rect.getX() + rect.getWidth() / 2;
    }

    private int centerY(Rectangle rect) {
        return rect.getY() + rect.getHeight() / 2;
    }

    private String safePageSource() {
        try {
            String source = driver.getPageSource();
            return source == null ? "" : source;
        } catch (Exception e) {
            return "";
        }
    }

    private void activateAdvisorAppIfNeeded() {
        String current = safeCurrentPackage();
        if (APP_PACKAGE.equals(current)) {
            return;
        }

        try {
            driver.activateApp(APP_PACKAGE);
            sleep(900L);
        } catch (Exception e) {
            throw new AssertionError("Unable to activate Advisor app | currentPackage=" + current, e);
        }
    }

    private String safeCurrentPackage() {
        try {
            String value = driver.getCurrentPackage();
            return value == null ? "" : value;
        } catch (Exception e) {
            return "";
        }
    }

    private void assertDriverAlive(String context) {
        try {
            driver.getCurrentPackage();
        } catch (Exception e) {
            throw new RuntimeException("Appium/UiAutomator2 session became unavailable " + context, e);
        }
    }

    private String cleanError(String message) {
        if (message == null) {
            return "";
        }
        return message.replaceAll("\\s+", " ").trim();
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    // =========================================================
    // INTERNAL DATA TYPES
    // =========================================================

    private static class HoldingSnapshot {
        private final String name;
        private final double currentWeight;
        private final String marketCap;
        private final String sector;

        private HoldingSnapshot(String name, double currentWeight, String marketCap, String sector) {
            this.name = name;
            this.currentWeight = currentWeight;
            this.marketCap = marketCap == null ? "" : marketCap;
            this.sector = sector == null ? "" : sector;
        }

        @Override
        public String toString() {
            return name + "=" + currentWeight + "% [" + marketCap + " | " + sector + "]";
        }
    }

    private static class RowControls {
        private final WebElement minus;
        private final WebElement plus;
        private final int quantity;
        private final double amount;
        private final int minusTapX;
        private final int minusTapY;
        private final int plusTapX;
        private final int plusTapY;

        private RowControls(
                WebElement minus,
                WebElement plus,
                int quantity,
                double amount,
                int minusTapX,
                int minusTapY,
                int plusTapX,
                int plusTapY
        ) {
            this.minus = minus;
            this.plus = plus;
            this.quantity = quantity;
            this.amount = amount;
            this.minusTapX = minusTapX;
            this.minusTapY = minusTapY;
            this.plusTapX = plusTapX;
            this.plusTapY = plusTapY;
        }
    }

    private static class SaleContext {
        private final Map<String, HoldingSnapshot> snapshots;
        private final Map<String, Integer> saleQuantities;
        private final Map<String, Double> rowAmounts;
        private final Map<String, Integer> totalSharesByStock;

        private SaleContext(
                Map<String, HoldingSnapshot> snapshots,
                Map<String, Integer> saleQuantities,
                Map<String, Double> rowAmounts,
                Map<String, Integer> totalSharesByStock
        ) {
            this.snapshots = snapshots;
            this.saleQuantities = saleQuantities;
            this.rowAmounts = rowAmounts;
            this.totalSharesByStock = totalSharesByStock;
        }
    }

    private static class PreviewRow {
        private final String label;
        private final double currentWeight;
        private final double netWeight;

        private PreviewRow(String label, double currentWeight, double netWeight) {
            this.label = label;
            this.currentWeight = currentWeight;
            this.netWeight = netWeight;
        }
    }
}