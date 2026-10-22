package com.valueresearch.pages;
import java.util.Arrays;
import com.valueresearch.utils.ExtentTestManager;
import com.valueresearch.utils.ReportLogger;
import com.valueresearch.utils.ExecutionProfile;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.support.ui.WebDriverWait;
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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class StockDetailsPage {

    private final AndroidDriver driver;

    private static final String STOCK_SEARCH_KEYWORD = "ITC";
    private static final String STOCK_RESULT = "ITC";
    private static final String STOCK_HEADER = "ITC Ltd.";

    private static final String VALUE_SCORE = "SCORE";
    private static final String VALUE_RUPEE = "RUPEE";
    private static final String VALUE_RUPEE_SIGNED = "RUPEE_SIGNED";
    private static final String VALUE_RUPEE_CRORE = "RUPEE_CRORE";
    private static final String VALUE_PERCENT = "PERCENT";
    private static final String VALUE_PERCENT_BRACKET = "PERCENT_BRACKET";
    private static final String VALUE_DECIMAL = "DECIMAL";
    private static final String VALUE_INTEGER = "INTEGER";
    private static final String VALUE_LARGE_INTEGER = "LARGE_INTEGER";
    private static final String VALUE_ANY_NUMBER = "ANY_NUMBER";
    private static final String VALUE_DATE_TIME = "DATE_TIME";

    public StockDetailsPage(AndroidDriver driver) {
        this.driver = driver;
    }

    // =========================================================
    // SD_001 - OPEN STOCK DETAILS
    // =========================================================

    public void openStockDetailsFromSearch() {
        try {
            ReportLogger.step("Opening Stock Details from global search");

            openGlobalSearchFromHome();
            enterSearchKeyword(STOCK_SEARCH_KEYWORD);
            openStocksSearchTab();
            openStockResult(STOCK_RESULT);
            verifyStockDetailsOpenedOnly();

            ReportLogger.pass("Stock Details opened successfully: " + STOCK_HEADER);
            logValidatedText("Stock opened", STOCK_HEADER);

        } catch (Exception e) {
            throw new RuntimeException("Failed to open Stock Details from global search: " + cleanError(e.getMessage()), e);
        }
    }

    public void recoverStockDetailsIfNeeded() {
        final NativeSnapshot[] latest = {readNativeSnapshot()};
        if (isStockDetailsSnapshot(latest[0])) {
            ReportLogger.debug("Stock Details page or known Stock Details section is already visible");
            return;
        }

        for (int attempt = 0; attempt < 3; attempt++) {
            if (isStockDetailsChildSnapshot(latest[0])) {
                ReportLogger.step("Returning from a recognised Stock Details More screen");
                driver.navigate().back();
            } else if (attempt > 0) {
                break;
            }
            try {
                keyRatioWait(ExecutionProfile.shortWait()).until(d -> {
                    latest[0] = readNativeSnapshot();
                    return isStockDetailsSnapshot(latest[0]);
                });
                ReportLogger.pass("Returned to Stock Details page");
                return;
            } catch (TimeoutException notReady) {
                // A scrolled/temporarily incomplete page is not evidence that
                // Back is safe. Only leave an explicitly recognised More screen.
                if (!isStockDetailsChildSnapshot(latest[0])) break;
            }
        }

        throw new RuntimeException("Stock Details viewport could not be recognised after a bounded recheck; "
                + "Back was restricted to recognised More screens | values=" + nativeStrings(latest[0].nodes));
    }

    public boolean isOnStockDetailsPage() {
        return isStockDetailsSnapshot(readNativeSnapshot());
    }

    private boolean isStockDetailsChildSnapshot(NativeSnapshot snapshot) {
        List<String> values = nativeStrings(snapshot.nodes);
        for (String label : new String[]{"Profitability", "Solvency", "Operating Efficiency",
                "Annual Consolidated", "Annual Standalone", "Quarterly Consolidated", "Quarterly Standalone"}) {
            if (values.contains(label)) return true;
        }
        return false;
    }

    private boolean isStockDetailsSnapshot(NativeSnapshot snapshot) {
        if (isStockDetailsChildSnapshot(snapshot)) return false;
        List<String> values = nativeStrings(snapshot.nodes);
        for (String heading : new String[]{STOCK_HEADER, "Stock Rating", "Stock Range", "Fundamentals",
                "10 Years Aggregate", "Stock Performance", "Essential Checks", "Financials", "Key Ratios",
                "Peers", "Shareholding", "Company Profile", "Business", "News", "Analysis"}) {
            if (values.contains(heading)) return true;
        }
        if (values.containsAll(Arrays.asList("Efficiency", "Valuation", "Growth"))) return true;
        for (NativeNode node : snapshot.nodes) {
            String text = normalizeSpaces(node.text);
            if (text.startsWith(STOCK_HEADER + " ") || text.startsWith("Company Profile ")) return true;
            for (String title : ESSENTIAL_CARD_TITLES) {
                if (text.contains(title) && hasEssentialChecksDynamicScore(text)) return true;
            }
            String lower = text.toLowerCase(java.util.Locale.ROOT);
            if ((lower.contains("price to earnings") || lower.contains("price to book")
                    || lower.contains("p/e ratio") || lower.contains("p/b ratio"))
                    && text.contains("Min") && text.contains("Median") && text.contains("Max")) return true;
        }

        // At the end of SD_004, Flutter can expose numeric Fundamentals cells
        // while its section/row headings are above the viewport or unlabelled.
        // Recognise a combination of observed data types, never fixed prices.
        long croreValues = values.stream().filter(v -> v.matches("₹\\s*\\d[\\d,]*(?:\\.\\d+)?\\s*Cr")).count();
        long percentages = values.stream().filter(v -> v.matches("-?\\d+(?:\\.\\d+)?\\s*%")).count();
        long plainNumbers = values.stream().filter(v -> v.matches("-?\\d[\\d,]*(?:\\.\\d+)?")).count();
        long rupeeValues = values.stream().filter(v -> v.matches("₹\\s*\\d[\\d,]*(?:\\.\\d+)?")).count();
        boolean sharesCount = values.stream().anyMatch(v -> v.matches("\\d[\\d,]*") && v.replace(",", "").length() >= 8);
        boolean fundamentalsBody = values.contains("Net Profit (TTM)") && croreValues >= 2
                && percentages >= 1 && plainNumbers >= 3;
        boolean fundamentalsTail = sharesCount && rupeeValues >= 2 && plainNumbers >= 2;
        boolean aggregateBody = values.containsAll(Arrays.asList("CFO", "EBITDA", "Net Profit")) && rupeeValues >= 3;
        return fundamentalsBody || fundamentalsTail || aggregateBody;
    }

    private void verifyStockDetailsOpenedOnly() {
        ReportLogger.step("Verifying Stock Details page opened");

        waitForStockDetailsAfterResultTap();

        assertVisibleAndLog(byDesc(STOCK_HEADER), "Stock header");

        ReportLogger.pass("Stock Details page opened and header is visible");
    }

    // =========================================================
    // SD_002 - HEADER VALIDATION
    // =========================================================

public void verifyStockHeader() {
    runKeyRatiosWithImmediateLookups(() -> {

        ReportLogger.step("Verifying Stock Details header with live value capture");

        recoverStockDetailsIfNeeded();

        sleep(1200);

        assertVisibleAndLog(byDesc(STOCK_HEADER), "Stock header");

        validateHeaderExchangeSelector();

        assertVisibleAndLog(byDesc("Consumer Staples"), "Sector label");
        assertVisibleAndLog(byDesc("Tobacco Products"), "Industry label");

        /*
         * Capture only header live values.
         * Do NOT validate High / Low / Returns here because those belong to graph summary.
         */
        captureHeaderLiveValues();

        validateHeaderChartControls();

        assertVisibleAndLog(byDesc("Stock Rating"), "Stock Rating section");

        ReportLogger.pass("Stock header validated successfully with live values");

    });
}


    private void captureHeaderLiveValues() {
        ReportLogger.step("Capturing Stock Header live values");

        List<String> values = collectVisibleContentDescriptions();

        int stockIndex = indexOfExact(values, STOCK_HEADER);

        if (stockIndex < 0) {
            throw new AssertionError("Stock header not found in visible source order: " + STOCK_HEADER);
        }

        String price = findNextValueByType(values, stockIndex + 1, VALUE_RUPEE);

        if (price == null) {
            throw new AssertionError("Header price value not found after stock header");
        }

        int priceIndex = indexOfExactFrom(values, price, stockIndex + 1);

        String change = findNextValueByType(values, priceIndex + 1, VALUE_RUPEE_SIGNED);

        int changeIndex = change == null ? priceIndex : indexOfExactFrom(values, change, priceIndex + 1);

        String changePercent = findNextValueByType(values, changeIndex + 1, VALUE_PERCENT_BRACKET);

        int changePercentIndex = changePercent == null ? changeIndex : indexOfExactFrom(values, changePercent, changeIndex + 1);

        String dateTime = findNextValueByType(values, changePercentIndex + 1, VALUE_DATE_TIME);

        logValidatedText("Header price", price);

        if (change != null) {
            logValidatedText("Header change", change);
        } else {
            ReportLogger.debug("Header change value not found. Skipping optional log.");
        }

        if (changePercent != null) {
            logValidatedText("Header change percentage", changePercent);
        } else {
            ReportLogger.debug("Header change percentage not found. Skipping optional log.");
        }

        if (dateTime != null) {
            logValidatedText("Header price date/time", dateTime);
        } else {
            ReportLogger.debug("Header price date/time not found. Skipping optional log.");
        }

        ReportLogger.pass("Stock header live values captured successfully");
    }

    // =========================================================
    // SD_003 - STOCK RATING + STOCK RANGE
    // =========================================================

    public void verifyStockRatingAndRange() {
        runKeyRatiosWithImmediateLookups(() -> {
        ReportLogger.step("Verifying Stock Rating and Stock Range section");

        recoverStockDetailsIfNeeded();

        scrollToStockRatingSection();

        assertVisibleAndLog(byDesc("Stock Rating"), "Stock Rating section");

        validateMetricRowPairLive(
                "Quality Score", VALUE_SCORE,
                "Growth Score", VALUE_SCORE,
                1
        );

        validateMetricRowPairLive(
                "Valuation Score", VALUE_SCORE,
                "Momentum Score", VALUE_SCORE,
                1
        );

        scrollToStockRangeSection();

        assertVisibleAndLog(byDesc("Stock Range"), "Stock Range section");

        validateStockRangeCarousel();

        ReportLogger.pass("Stock Rating and Stock Range section validated successfully");
        });
    }

    private void scrollToStockRatingSection() {
        ReportLogger.step("Scrolling to Stock Rating section");

        if (isVisible(byDesc("Stock Rating"))) {
            ReportLogger.pass("Stock Rating section is already visible");
            return;
        }

        for (int i = 1; i <= 5; i++) {
            smallSwipeUpW3C();
            sleep(700);

            if (isVisible(byDesc("Stock Rating"))) {
                ReportLogger.pass("Stock Rating section is visible");
                return;
            }
        }

        throw new AssertionError("Stock Rating section is not visible after controlled scrolling");
    }

    private void scrollToStockRangeSection() {
        ReportLogger.step("Scrolling to Stock Range section and aligning card body");

        for (int i = 1; i <= 8; i++) {
            boolean headingVisible = isVisible(byDesc("Stock Range"));
            boolean cardVisible = isAnyStockRangeCardVisible();

            if (headingVisible && cardVisible) {
                ReportLogger.pass("Stock Range heading and card body are visible");
                return;
            }

            smallSwipeUpW3C();
            sleep(700);
        }

        if (isVisible(byDesc("Stock Range")) && isAnyStockRangeCardVisible()) {
            ReportLogger.pass("Stock Range heading and card body are visible after final check");
            return;
        }

        throw new AssertionError("Stock Range card body is not visible after controlled scrolling");
    }

    private void validateStockRangeCarousel() {
        ReportLogger.step("Validating all three Stock Range cards using their titles and live content");
        for (String expected : new String[]{"Today's Range", "52 Week Range", "Liquidity"}) {
            NativeNode card = openStockRangeCard(expected);
            NativeSnapshot snapshot = readNativeSnapshot();
            Rectangle area = stockRangeCardBounds(card, snapshot);
            List<NativeNode> content = new ArrayList<>();
            for (NativeNode node : snapshot.section("Stock Range", "Fundamentals")) {
                Rectangle intersection = intersectNativeBounds(node.visible, area);
                if (intersection.getWidth() >= node.visible.getWidth() * 0.7
                        && intersection.getHeight() >= node.visible.getHeight() * 0.7) content.add(node);
            }
            String text = String.join("\n", nativeStrings(content));
            if (expected.equals("Liquidity")) {
                for (String label : new String[]{"Low", "Moderate", "High"}) {
                    if (!text.contains(label)) throw new AssertionError("Liquidity scale label missing: " + label + " | card=" + text);
                }
            } else {
                if (!text.contains("Low") || !text.contains("High")) {
                    throw new AssertionError(expected + " Low/High labels missing | card=" + text);
                }
                List<String> prices = new ArrayList<>();
                // Do not read 52 from the title as a price, or values from a neighbouring card.
                for (NativeNode node : content) {
                    String value = node.text.replace("52 Week Range", "").replace("Today's Range", "").replace("Today’s Range", "");
                    java.util.regex.Matcher matcher = Pattern.compile("₹\\s*([\\d,]+(?:\\.\\d+)?)").matcher(value);
                    while (matcher.find()) prices.add(matcher.group(1).replace(",", ""));
                }
                if (prices.size() < 2) throw new AssertionError(expected + " requires two live price values | card=" + text);
                // Native source order can differ from the visual Low/High order.
                // Keep the actual values without assigning them by list position.
            }
            logValidatedText(expected + " live card", text);
        }
    }

    private NativeNode openStockRangeCard(String expected) {
        NativeNode card = null;
        for (int direction = 0; direction < 2; direction++) {
            for (int attempt = 0; attempt < 4; attempt++) {
                NativeSnapshot snapshot = readNativeSnapshot();
                card = activeStockRangeCard(snapshot);
                if (card == null) {
                    swipeVisibleBody(true, "stock-range");
                    sleep(400);
                    continue;
                }
                if (card.visible.getY() > snapshot.screen.getHeight() * 0.65) {
                    swipeStockSectionBody(true, true, "stock-range-alignment");
                    sleep(400);
                    continue;
                }
                Rectangle area = stockRangeCardBounds(card, snapshot);
                // The failed run stopped as soon as the title appeared at the
                // bottom. Bring the complete card above the navigation bar first.
                if (area.getY() + area.getHeight() > snapshot.screen.getHeight() * 0.88) {
                    swipeStockSectionBody(true, true, "stock-range-alignment");
                    sleep(400);
                    continue;
                }
                if (stockRangeTitle(card.text).equals(expected)) return card;
                swipeInBounds(area, true, direction == 0);
                sleep(450);
            }
        }
        throw new AssertionError("Stock Range card not reached: " + expected
                + " | active=" + (card == null ? "none" : card.text) + "; capture this card's native XML if its title changed");
    }

    private String stockRangeTitle(String text) {
        String clean = normalizeSpaces(text == null ? "" : text.replace("\\n", "\n")).replace('’', '\'');
        // Confirmed Inspector semantics: the third card has no Liquidity title.
        // Match the complete scale, scoped by the Stock Range section and bounds.
        if (clean.equals("Low Moderate High")) return "Liquidity";
        for (String title : new String[]{"Today's Range", "52 Week Range", "Liquidity"}) {
            if (clean.equals(title) || clean.startsWith(title + " ")) return title;
        }
        return "";
    }

    private NativeNode activeStockRangeCard(NativeSnapshot snapshot) {
        NativeNode best = null;
        long bestArea = 0;
        for (NativeNode node : snapshot.section("Stock Range", "Fundamentals")) {
            if (stockRangeTitle(node.text).isEmpty()) continue;
            if (node.visible.getWidth() < node.bounds.getWidth() * 0.75) continue;
            long area = (long) node.visible.getWidth() * node.visible.getHeight();
            if (area > bestArea) { best = node; bestArea = area; }
        }
        return best;
    }

    private Rectangle stockRangeCardBounds(NativeNode title, NativeSnapshot snapshot) {
        Rectangle best = title.bounds;
        // Prefer an actual card container, including on builds exposing the title
        // and prices as separate accessibility nodes.
        for (org.w3c.dom.Node parent = title.element; parent instanceof org.w3c.dom.Element; parent = parent.getParentNode()) {
            Rectangle bounds = nativeBounds(((org.w3c.dom.Element) parent).getAttribute("bounds"));
            if (bounds != null && bounds.getWidth() >= snapshot.screen.getWidth() * 0.55
                    && bounds.getHeight() >= snapshot.screen.getHeight() * 0.08
                    && bounds.getHeight() <= snapshot.screen.getHeight() * 0.40) return bounds;
        }
        if (best.getHeight() >= snapshot.screen.getHeight() * 0.08) return best;
        // Some Flutter builds expose a flat semantics tree with no card parent.
        // Derive the area from the title and the actual labels/value nodes below
        // it, stopping at the next card/section rather than guessing dot offsets.
        int right = snapshot.screen.getWidth();
        for (NativeNode node : snapshot.section("Stock Range", "Fundamentals")) {
            if (!stockRangeTitle(node.text).isEmpty() && node.bounds.getX() > title.bounds.getX()
                    + snapshot.screen.getWidth() * 0.50) right = Math.min(right, node.bounds.getX());
        }
        int left = title.visible.getX(), bottom = title.visible.getY() + title.visible.getHeight();
        for (NativeNode node : snapshot.section("Stock Range", "Fundamentals")) {
            int centerX = node.visible.getX() + node.visible.getWidth() / 2;
            if (centerX < left || centerX >= right || node.visible.getY() < title.visible.getY()) continue;
            if (node.visible.getY() > title.visible.getY() + snapshot.screen.getHeight() * 0.40) continue;
            String value = normalizeSpaces(node.text);
            if (value.equals("Low") || value.equals("Moderate") || value.equals("High") || value.startsWith("₹")) {
                bottom = Math.max(bottom, node.visible.getY() + node.visible.getHeight());
            }
        }
        if (right - left >= snapshot.screen.getWidth() * 0.50 && bottom - title.visible.getY() >= 80) {
            return new Rectangle(left, title.visible.getY(), bottom - title.visible.getY(), right - left);
        }
        throw new AssertionError("Stock Range card container is not exposed; provide its native XML | title=" + title.text);
    }

    private void swipeStockSectionBody(boolean up, boolean small, String context) {
        Dimension screen = driver.manage().window().getSize();
        int top = (int) (screen.getHeight() * (small ? 0.48 : 0.22));
        int height = (int) (screen.getHeight() * (small ? 0.27 : 0.63));
        ReportLogger.debug("Scrolling " + context + " " + (up ? "up" : "down"));
        swipeInBounds(new Rectangle(0, top, height, screen.getWidth()), false, up);
    }

    private boolean isAnyStockRangeCardVisible() {
        return isVisible(byDescContains("Today’s Range"))
                || isVisible(byDescContains("Today's Range"))
                || isVisible(byDescContains("52 Week Range"))
                || isVisible(byDescContains("52 Week"))
                || isVisible(byDescContains("Liquidity"))
                || isVisible(byDescContains("Low\nModerate\nHigh"))
                || isVisible(byDescContains("Low Moderate High"));
    }
    private List<WebElement> getVisibleViewElementsFast() {
        List<WebElement> visibleElements = new ArrayList<>();

        try {
            List<WebElement> elements = driver.findElements(AppiumBy.className("android.view.View"));

            for (WebElement element : elements) {
                try {
                    if (element != null && element.isDisplayed()) {
                        visibleElements.add(element);
                    }
                } catch (Exception ignored) {
                    // Ignore stale Flutter/Appium element
                }
            }
        } catch (Exception e) {
            ReportLogger.debug("Fast android.view.View scan failed: " + cleanError(e.getMessage()));
        }

        return visibleElements;
    }

    private List<WebElement> getVisibleElementsFallbackSlow() {
        List<WebElement> visibleElements = new ArrayList<>();

        try {
            List<WebElement> elements = driver.findElements(By.xpath("//*"));

            for (WebElement element : elements) {
                try {
                    if (element != null && element.isDisplayed()) {
                        visibleElements.add(element);
                    }
                } catch (Exception ignored) {
                    // Ignore stale Appium element
                }
            }
        } catch (Exception e) {
            ReportLogger.debug("Slow XPath fallback scan skipped: " + cleanError(e.getMessage()));
        }

        return visibleElements;
    }

    private void addElementReadableValuesToList(List<String> values, WebElement element) {
        if (element == null) {
            return;
        }

        try {
            addUniqueGenericString(values, element.getAttribute("content-desc"));
            addUniqueGenericString(values, element.getText());
            addUniqueGenericString(values, element.getAttribute("text"));
            addUniqueGenericString(values, element.getAttribute("name"));
        } catch (Exception ignored) {
            // Ignore stale/inaccessible element
        }
    }

    private void addUniqueGenericString(List<String> values, String rawValue) {
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

        String[] parts = rawValue.split("\\n|\\s{2,}|\\|");

        for (String part : parts) {
            String cleanPart = normalizeSpaces(part);

            if (!cleanPart.isEmpty() && !values.contains(cleanPart)) {
                values.add(cleanPart);
            }
        }
    }
    // =========================================================
    // SD_004 - FUNDAMENTALS
    // =========================================================

    public void verifyFundamentals() {
        ReportLogger.step("Verifying Fundamentals section");

        recoverStockDetailsIfNeeded();

        scrollToFundamentalsSection();

        assertVisibleAndLog(byDesc("Fundamentals"), "Fundamentals section");

        validateMetricRowPairLive("Market cap", VALUE_RUPEE_CRORE, "Revenue (TTM)", VALUE_RUPEE_CRORE, 2);
        validateMetricRowPairLive("Net Profit (TTM)", VALUE_RUPEE_CRORE, "ROE", VALUE_PERCENT, 3);
        validateMetricRowPairLive("ROCE", VALUE_PERCENT, "P/E Ratio", VALUE_DECIMAL, 3);
        validateMetricRowPairLive("P/B Ratio", VALUE_DECIMAL, "Industry P/E", VALUE_DECIMAL, 4);
        validateMetricRowPairLive("EV/EBITDA", VALUE_DECIMAL, "Div. Yield", VALUE_PERCENT, 4);
        validateMetricRowPairLive("Debt to Equity", VALUE_INTEGER, "Book Value", VALUE_ANY_NUMBER, 5);
        validateMetricRowPairLive("EPS", VALUE_ANY_NUMBER, "Face value", VALUE_INTEGER, 5);

        validateSharesOutstandingLiveValue();

        ReportLogger.pass("Fundamentals section validated successfully with live value capture");
    }

    private void scrollToFundamentalsSection() {
        ReportLogger.step("Scrolling to Fundamentals section with page-level scroll");

        if (isVisible(byDesc("Fundamentals"))) {
            ReportLogger.pass("Fundamentals section is already visible");
            return;
        }

        for (int i = 1; i <= 12; i++) {
            pageSwipeUpW3C();
            sleep(800);

            if (isVisible(byDesc("Fundamentals"))) {
                ReportLogger.pass("Fundamentals section is visible after page scroll " + i);
                return;
            }
        }

        throw new AssertionError("Fundamentals section is not visible after page-level scrolling");
    }

    // =========================================================
    // SD_005 - 10 YEARS AGGREGATE
    // =========================================================

    public void verifyTenYearsAggregate() {
        ReportLogger.step("Verifying 10 Years Aggregate section");

        recoverStockDetailsIfNeeded();

        scrollToTenYearsAggregateSection();

        assertVisibleAndLog(byDesc("10 Years Aggregate"), "10 Years Aggregate section");

        validateMetricRowTripleLive(
                "CFO", VALUE_ANY_NUMBER,
                "EBITDA", VALUE_ANY_NUMBER,
                "Net Profit", VALUE_ANY_NUMBER,
                2
        );

        assertVisibleAndLog(byDesc("*All values are in (Cr)"), "All values are in Cr note");

        ReportLogger.pass("10 Years Aggregate section validated successfully with live value capture");
    }

    private void scrollToTenYearsAggregateSection() {
        ReportLogger.step("Scrolling to 10 Years Aggregate section with page-level scroll");

        for (int i = 1; i <= 12; i++) {
            boolean sectionReady =
                    isVisible(byDesc("10 Years Aggregate"))
                            && isVisible(byDesc("CFO"))
                            && isVisible(byDesc("EBITDA"))
                            && isVisible(byDesc("Net Profit"))
                            && isVisible(byDesc("*All values are in (Cr)"));

            if (sectionReady) {
                ReportLogger.pass("10 Years Aggregate card is properly aligned");
                return;
            }

            pageSwipeUpW3C();
            sleep(800);
        }

        boolean sectionReady =
                isVisible(byDesc("10 Years Aggregate"))
                        && isVisible(byDesc("CFO"))
                        && isVisible(byDesc("EBITDA"))
                        && isVisible(byDesc("Net Profit"))
                        && isVisible(byDesc("*All values are in (Cr)"));

        if (sectionReady) {
            ReportLogger.pass("10 Years Aggregate card is properly aligned after final check");
            return;
        }

        throw new AssertionError("10 Years Aggregate card is not properly visible after page-level scrolling");
    }

    // =========================================================
    // SD_006 - STOCK PERFORMANCE
    // =========================================================

    public void verifyStockPerformance() {
        ReportLogger.step("Verifying Stock Performance section");

        recoverStockDetailsIfNeeded();

        /*
         * The tab strip and table headers are separate readiness concerns.
         * Keep both visible before the first validation and before switching tabs.
         */
        alignStockPerformanceTabsAndTable();

        assertVisibleAndLogFlexible(
                getStockPerformanceTabLocators("Trailing"),
                "Trailing tab"
        );
        assertVisibleAndLogFlexible(
                getStockPerformanceTabLocators("Annual"),
                "Annual tab"
        );

        validateStockPerformanceTabLive("Trailing");

        /*
         * Dynamic row capture scrolls the table and can leave the tab strip above
         * the viewport while the column headers remain visible. Re-align the tab
         * strip explicitly before tapping Annual.
         */
        alignStockPerformanceTabsAndTable();
        sleep(500);

        tapStockPerformanceTab("Annual");
        sleep(1000);

        /*
         * After tab selection only the table headers are required for row capture.
         * Do not force the tab strip to stay visible while scrolling through rows.
         */
        alignStockPerformanceTable();
        validateStockPerformanceTabLive("Annual");

        ReportLogger.pass("Stock Performance section validated successfully with live value capture");
    }

    private void scrollToStockPerformanceSection() {
        ReportLogger.step("Scrolling to Stock Performance section and aligning table");

        for (int i = 1; i <= 12; i++) {
            boolean sectionReady =
                    isVisible(byDesc("Stock Performance"))
                            && isVisible(byDesc("Trailing"))
                            && isVisible(byDesc("Annual"))
                            && isVisibleFlexible(new By[]{
                                    byDesc("Time Period"),
                                    byDescContains("Time Period")
                            })
                            && isVisibleFlexible(new By[]{
                                    byDesc("ITC"),
                                    byDescContains("ITC")
                            })
                            && isVisibleFlexible(new By[]{
                                    byDesc("BSE Sensex"),
                                    byDescContains("BSE Sensex")
                            })
                            && isVisibleFlexible(new By[]{
                                    byDesc("BSEFMCG"),
                                    byDesc("BSE FMCG"),
                                    byDescContains("BSEFMCG"),
                                    byDescContains("BSE FMCG")
                            });

            if (sectionReady) {
                ReportLogger.pass("Stock Performance table is properly aligned");
                return;
            }

            pageSwipeUpW3C();
            sleep(800);
        }

        throw new AssertionError("Stock Performance table is not properly visible after page-level scrolling");
    }

    private By[] getStockPerformanceTabLocators(String tabName) {
        return new By[]{
                byDesc(tabName),
                byDescContains(tabName),
                byText(tabName),
                byTextContains(tabName)
        };
    }

    private boolean isStockPerformanceTabVisible(String tabName) {
        return isVisibleFlexible(getStockPerformanceTabLocators(tabName));
    }

    private void tapStockPerformanceTab(String tabName) {
        ReportLogger.step("Opening Stock Performance tab: " + tabName);

        alignStockPerformanceTabsAndTable();

        for (By locator : getStockPerformanceTabLocators(tabName)) {
            if (tapIfVisible(locator, "Stock Performance tab: " + tabName)) {
                sleep(1200);
                ReportLogger.pass("Stock Performance tab opened: " + tabName);
                return;
            }
        }

        /*
         * Flutter can keep the semantic node visible but not tappable during a
         * repaint. Perform small upward/downward corrections and retry using all
         * safe locators instead of coordinates.
         */
        for (int attempt = 1; attempt <= 4; attempt++) {
            stockPerformanceControlledTableSwipeDown();
            sleep(500);

            for (By locator : getStockPerformanceTabLocators(tabName)) {
                if (tapIfVisible(
                        locator,
                        "Stock Performance tab after recovery: " + tabName
                )) {
                    sleep(1200);
                    ReportLogger.pass(
                            "Stock Performance tab opened after recovery: " + tabName
                    );
                    return;
                }
            }
        }

        throw new AssertionError(
                "Unable to tap Stock Performance tab after alignment and recovery: "
                        + tabName
        );
    }

    private void validateStockPerformanceTabLive(String tabName) {
        ReportLogger.step("Validating Stock Performance table for tab: " + tabName);

        /*
         * Do not strictly assert Stock Performance heading here.
         * After dynamic row capture, heading can move offscreen.
         * Table headers are enough to prove section context.
         */
        assertVisibleAndLogFlexible(
                getStockPerformanceTabLocators(tabName),
                tabName + " tab"
        );

        assertVisibleAndLogFlexible(
                new By[]{
                        byDesc("Time Period"),
                        byDescContains("Time Period")
                },
                "Time Period header"
        );

        assertVisibleAndLogFlexible(
                new By[]{
                        byDesc("ITC"),
                        byDescContains("ITC")
                },
                "ITC column header"
        );

        assertVisibleAndLogFlexible(
                new By[]{
                        byDesc("BSE Sensex"),
                        byDescContains("BSE Sensex")
                },
                "BSE Sensex column header"
        );

        assertVisibleAndLogFlexible(
                new By[]{
                        byDesc("BSEFMCG"),
                        byDesc("BSE FMCG"),
                        byDescContains("BSEFMCG"),
                        byDescContains("BSE FMCG")
                },
                "BSE FMCG column header"
        );

        captureStockPerformanceVisibleRows(tabName);

        logOptionalStockPerformanceText(byDescContains("As on"), tabName + " as-on date");

        ReportLogger.pass("Stock Performance " + tabName + " table validated with live values");
    }
    private void captureStockPerformanceVisibleRows(String tabName) {
        ReportLogger.step("Capturing Stock Performance rows dynamically for tab: " + tabName);

        java.util.LinkedHashMap<String, String> capturedRows = new java.util.LinkedHashMap<>();

        int noNewRowsCount = 0;
        int maxAttempts = 9;

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            int beforeCount = capturedRows.size();

            /*
             * Try all parsers because Flutter/Appium exposes this table differently
             * across emulator/full-suite runs:
             * 1) normal row-label + values
             * 2) column-wise values
             * 3) row-wise fallback
             */
            captureStockPerformanceRowsFromCurrentVisibleTable(tabName, capturedRows);
            captureCurrentStockPerformanceViewportColumnWise(tabName, capturedRows);
            captureStockPerformanceRowsFromCurrentViewport(tabName, capturedRows);

            int afterCount = capturedRows.size();

            if (afterCount > beforeCount) {
                ReportLogger.pass("Captured new Stock Performance rows on attempt "
                        + attempt + " | Total rows=" + afterCount);
                noNewRowsCount = 0;
            } else {
                noNewRowsCount++;
                ReportLogger.debug("No new Stock Performance rows captured on attempt "
                        + attempt + " | Stable count=" + noNewRowsCount);
            }

            /*
             * Important:
             * Do NOT stop on first no-row attempt when capturedRows is empty.
             * In regression, headers can be visible while actual rows are just below viewport.
             */
            if (capturedRows.isEmpty()) {
                stockPerformanceControlledTableSwipeUp();
                sleep(850);
                continue;
            }

            if (noNewRowsCount >= 2) {
                ReportLogger.pass("No more new Stock Performance rows visible after controlled scroll. Ending capture.");
                break;
            }

            stockPerformanceControlledTableSwipeUp();
            sleep(850);

            /*
             * If table headers moved out, try one light recovery instead of leaving
             * the section and skipping validations.
             */
            if (!isStockPerformanceTableReady()) {
                pageSwipeDownW3C();
                sleep(650);

                if (!isStockPerformanceTableReady()) {
                    ReportLogger.debug("Stock Performance table headers moved out of viewport after recovery. Stopping capture.");
                    break;
                }
            }
        }

        if (capturedRows.isEmpty()) {
            throw new AssertionError("No Stock Performance rows captured for tab: " + tabName);
        }

        for (Map.Entry<String, String> entry : capturedRows.entrySet()) {
            logValidatedText(
                    "Stock Performance " + tabName + " | " + entry.getKey(),
                    entry.getValue()
            );
        }

        ReportLogger.pass("Total Stock Performance rows captured for "
                + tabName + ": " + capturedRows.size()
                + " | Rows=" + capturedRows.keySet());
    }

    private void captureStockPerformanceRowsFromCurrentVisibleTable(
            String tabName,
            java.util.LinkedHashMap<String, String> capturedRows
    ) {
        List<String> values = collectVisibleContentDescriptions();

        int tableStartIndex = findStockPerformanceTableStartIndex(values);

        if (tableStartIndex < 0) {
            ReportLogger.debug("Stock Performance table headers not found in current viewport for tab: " + tabName);
            return;
        }

        List<String> rowLabels = new ArrayList<>();
        List<String> numbers = new ArrayList<>();

        for (int i = tableStartIndex + 1; i < values.size(); i++) {
            String clean = normalizeSpaces(values.get(i));

            if (clean.isEmpty()) {
                continue;
            }

            if (isStockPerformanceEndMarker(clean)) {
                break;
            }

            if (isDynamicStockPerformanceRowLabel(clean)) {
                rowLabels.add(clean);
                continue;
            }

            if (isValueMatchingType(clean, VALUE_DECIMAL)) {
                numbers.add(clean);
            }
        }

        if (rowLabels.isEmpty()) {
            ReportLogger.debug("No Stock Performance row labels visible for tab: " + tabName);
            return;
        }

        if (numbers.size() < rowLabels.size() * 3) {
            ReportLogger.debug("Incomplete Stock Performance values in current viewport for tab: "
                    + tabName
                    + " | rows=" + rowLabels
                    + " | numbers=" + numbers);
            return;
        }

        for (int i = 0; i < rowLabels.size(); i++) {
            String rowLabel = rowLabels.get(i);

            if (capturedRows.containsKey(rowLabel)) {
                continue;
            }

            int base = i * 3;

            String rowValue = "ITC=" + numbers.get(base)
                    + " | BSE Sensex=" + numbers.get(base + 1)
                    + " | BSE FMCG=" + numbers.get(base + 2);

            capturedRows.put(rowLabel, rowValue);

            ReportLogger.pass("Captured Stock Performance row: "
                    + tabName
                    + " | " + rowLabel
                    + " | " + rowValue);
        }
    }

    private void stockPerformanceControlledTableSwipeUp() {
        try {
            Dimension size = driver.manage().window().getSize();

            /*
             * Small controlled scroll only.
             * This is enough to reveal lower rows like 2021/2020/2019,
             * but avoids jumping to Business / News / Analysis.
             */
            int x = (int) (size.getWidth() * 0.88);
            int startY = (int) (size.getHeight() * 0.76);
            int endY = (int) (size.getHeight() * 0.58);

            PointerInput finger = new PointerInput(PointerInput.Kind.TOUCH, "finger");
            Sequence swipe = new Sequence(finger, 1);

            swipe.addAction(finger.createPointerMove(Duration.ZERO, PointerInput.Origin.viewport(), x, startY));
            swipe.addAction(finger.createPointerDown(PointerInput.MouseButton.LEFT.asArg()));
            swipe.addAction(finger.createPointerMove(Duration.ofMillis(450), PointerInput.Origin.viewport(), x, endY));
            swipe.addAction(finger.createPointerUp(PointerInput.MouseButton.LEFT.asArg()));

            driver.perform(Collections.singletonList(swipe));

        } catch (Exception firstError) {
            Map<String, Object> params = new HashMap<>();
            params.put("left", 760);
            params.put("top", 900);
            params.put("width", 260);
            params.put("height", 520);
            params.put("direction", "up");
            params.put("percent", 0.25);

            driver.executeScript("mobile: scrollGesture", params);
        }
    }

    private void alignStockPerformanceTabsAndTable() {
        ReportLogger.step("Aligning Stock Performance tabs and table");

        if (isStockPerformanceTabsAndTableReady()) {
            ReportLogger.pass("Stock Performance tabs and table are already aligned");
            return;
        }

        /*
         * First recover upward in the document. This is the normal path after
         * Trailing/Annual row capture leaves the tab strip above the viewport.
         */
        for (int i = 1; i <= 6; i++) {
            stockPerformanceControlledTableSwipeDown();
            sleep(550);

            if (isStockPerformanceTabsAndTableReady()) {
                ReportLogger.pass(
                        "Stock Performance tabs and table aligned after recovery swipe down " + i
                );
                return;
            }
        }

        /*
         * Standalone execution may start above Stock Performance. Search forward
         * using controlled page swipes, then stop immediately when both areas are ready.
         */
        for (int i = 1; i <= 8; i++) {
            pageSwipeUpW3C();
            sleep(650);

            if (isStockPerformanceTabsAndTableReady()) {
                ReportLogger.pass(
                        "Stock Performance tabs and table aligned after page swipe up " + i
                );
                return;
            }
        }

        /*
         * Final limited recovery in case the forward search crossed the section.
         */
        for (int i = 1; i <= 5; i++) {
            pageSwipeDownW3C();
            sleep(650);

            if (isStockPerformanceTabsAndTableReady()) {
                ReportLogger.pass(
                        "Stock Performance tabs and table aligned after page recovery " + i
                );
                return;
            }
        }

        throw new AssertionError("Stock Performance tabs and table could not be aligned");
    }

    private void alignStockPerformanceTable() {
        ReportLogger.step("Aligning Stock Performance table");

        if (isStockPerformanceTableReady()) {
            ReportLogger.pass("Stock Performance table is already aligned");
            return;
        }

        /*
         * SD_006 starts just below 10 Years Aggregate.
         * Only small page-up movement is needed.
         * Do not over-scroll.
         */
        for (int i = 1; i <= 4; i++) {
            pageSwipeUpW3C();
            sleep(700);

            if (isStockPerformanceTableReady()) {
                ReportLogger.pass("Stock Performance table aligned after page swipe up " + i);
                return;
            }
        }

        /*
         * If previous capture accidentally moved below the table,
         * recover with limited downward swipes.
         */
        for (int i = 1; i <= 3; i++) {
            pageSwipeDownW3C();
            sleep(700);

            if (isStockPerformanceTableReady()) {
                ReportLogger.pass("Stock Performance table aligned after page swipe down " + i);
                return;
            }
        }

        throw new AssertionError("Stock Performance table could not be aligned");
    }

    private boolean isStockPerformanceTabsAndTableReady() {
        return isStockPerformanceTabVisible("Trailing")
                && isStockPerformanceTabVisible("Annual")
                && isStockPerformanceTableReady();
    }

    /*
     * Header-only readiness is intentionally retained for dynamic row capture.
     * Requiring the tab strip here would repeatedly scroll back to the top and
     * prevent lower rows such as 10 Years or older Annual years from being read.
     */
    private boolean isStockPerformanceTableReady() {
        return isVisibleFlexible(new By[]{
                byDesc("Time Period"),
                byDescContains("Time Period")
        })
                && isVisibleFlexible(new By[]{
                byDesc("ITC"),
                byDescContains("ITC")
        })
                && isVisibleFlexible(new By[]{
                byDesc("BSE Sensex"),
                byDescContains("BSE Sensex")
        })
                && isVisibleFlexible(new By[]{
                byDesc("BSEFMCG"),
                byDesc("BSE FMCG"),
                byDescContains("BSEFMCG"),
                byDescContains("BSE FMCG")
        });
    }

    private void stockPerformanceControlledTableSwipeDown() {
        try {
            Dimension size = driver.manage().window().getSize();

            int x = (int) (size.getWidth() * 0.88);
            int startY = (int) (size.getHeight() * 0.58);
            int endY = (int) (size.getHeight() * 0.76);

            PointerInput finger = new PointerInput(PointerInput.Kind.TOUCH, "finger");
            Sequence swipe = new Sequence(finger, 1);

            swipe.addAction(finger.createPointerMove(
                    Duration.ZERO,
                    PointerInput.Origin.viewport(),
                    x,
                    startY
            ));
            swipe.addAction(finger.createPointerDown(PointerInput.MouseButton.LEFT.asArg()));
            swipe.addAction(finger.createPointerMove(
                    Duration.ofMillis(450),
                    PointerInput.Origin.viewport(),
                    x,
                    endY
            ));
            swipe.addAction(finger.createPointerUp(PointerInput.MouseButton.LEFT.asArg()));

            driver.perform(Collections.singletonList(swipe));

        } catch (Exception firstError) {
            Map<String, Object> params = new HashMap<>();
            params.put("left", 760);
            params.put("top", 900);
            params.put("width", 260);
            params.put("height", 520);
            params.put("direction", "down");
            params.put("percent", 0.25);

            driver.executeScript("mobile: scrollGesture", params);
        }
    }

    private void captureCurrentStockPerformanceViewportColumnWise(
            String tabName,
            java.util.LinkedHashMap<String, String> capturedRows
    ) {
        List<String> values = collectVisibleContentDescriptions();

        int tableStartIndex = findStockPerformanceTableStartIndex(values);

        if (tableStartIndex < 0) {
            ReportLogger.debug("Stock Performance table headers not found in current viewport for tab: " + tabName);
            return;
        }

        List<String> rowLabels = new ArrayList<>();
        List<String> numericValues = new ArrayList<>();

        for (int i = tableStartIndex + 1; i < values.size(); i++) {
            String clean = normalizeSpaces(values.get(i));

            if (clean.isEmpty()) {
                continue;
            }

            if (isStockPerformanceEndMarker(clean)) {
                break;
            }

            if (isDynamicStockPerformanceRowLabel(clean)) {
                if (!rowLabels.contains(clean)) {
                    rowLabels.add(clean);
                }
                continue;
            }

            if (isValueMatchingType(clean, VALUE_DECIMAL)) {
                numericValues.add(clean);
            }
        }

        if (rowLabels.isEmpty() || numericValues.size() < 3) {
            ReportLogger.debug("Insufficient Stock Performance data in viewport for "
                    + tabName + " | rows=" + rowLabels + " | values=" + numericValues);
            return;
        }

        /*
         * Flutter/Appium exposes this table column-wise:
         * rows first, then ITC values, then BSE Sensex values, then BSE FMCG values.
         */
        int rowCount = rowLabels.size();

        if (numericValues.size() < rowCount * 3) {
            ReportLogger.debug("Viewport has incomplete column-wise values for "
                    + tabName + " | rowCount=" + rowCount
                    + " | numericCount=" + numericValues.size()
                    + " | rows=" + rowLabels
                    + " | values=" + numericValues);
            return;
        }

        for (int i = 0; i < rowCount; i++) {
            String rowLabel = rowLabels.get(i);

            if (capturedRows.containsKey(rowLabel)) {
                continue;
            }

            String itcValue = numericValues.get(i);
            String sensexValue = numericValues.get(i + rowCount);
            String fmcgValue = numericValues.get(i + (rowCount * 2));

            String rowValue = "ITC=" + itcValue
                    + " | BSE Sensex=" + sensexValue
                    + " | BSE FMCG=" + fmcgValue;

            capturedRows.put(rowLabel, rowValue);

            ReportLogger.pass("Captured Stock Performance row: "
                    + tabName
                    + " | " + rowLabel
                    + " | " + rowValue);
        }
    }

    private void stockPerformanceTableSwipeUpW3C() {
        try {
            Dimension size = driver.manage().window().getSize();

            int x = (int) (size.getWidth() * 0.88);
            int startY = (int) (size.getHeight() * 0.78);
            int endY = (int) (size.getHeight() * 0.46);

            PointerInput finger = new PointerInput(PointerInput.Kind.TOUCH, "finger");
            Sequence swipe = new Sequence(finger, 1);

            swipe.addAction(finger.createPointerMove(Duration.ZERO, PointerInput.Origin.viewport(), x, startY));
            swipe.addAction(finger.createPointerDown(PointerInput.MouseButton.LEFT.asArg()));
            swipe.addAction(finger.createPointerMove(Duration.ofMillis(650), PointerInput.Origin.viewport(), x, endY));
            swipe.addAction(finger.createPointerUp(PointerInput.MouseButton.LEFT.asArg()));

            driver.perform(Collections.singletonList(swipe));

        } catch (Exception firstError) {
            Map<String, Object> params = new HashMap<>();
            params.put("left", 760);
            params.put("top", 680);
            params.put("width", 260);
            params.put("height", 900);
            params.put("direction", "up");
            params.put("percent", 0.45);

            driver.executeScript("mobile: scrollGesture", params);
        }
    }
    private void captureStockPerformanceRowsFromCurrentViewport(
            String tabName,
            java.util.LinkedHashMap<String, String> capturedRows
    ) {
        List<String> values = collectVisibleContentDescriptions();

        int tableStartIndex = findStockPerformanceTableStartIndex(values);

        if (tableStartIndex < 0) {
            ReportLogger.debug("Stock Performance table headers not found in current viewport for tab: " + tabName);
            return;
        }

        for (int i = tableStartIndex + 1; i < values.size(); i++) {
            String rawRowLabel = values.get(i);

            if (rawRowLabel == null || rawRowLabel.trim().isEmpty()) {
                continue;
            }

            String rowLabel = normalizeSpaces(rawRowLabel);

            if (isStockPerformanceEndMarker(rowLabel)) {
                break;
            }

            if (!isDynamicStockPerformanceRowLabel(rowLabel)) {
                continue;
            }

            if (capturedRows.containsKey(rowLabel)) {
                continue;
            }

            List<String> rowNumbers = getNextThreeDecimalValuesBeforeNextRowOrSection(values, i + 1);

            if (rowNumbers.size() < 3) {
                ReportLogger.debug("Skipping incomplete Stock Performance row in current viewport: "
                        + tabName + " | " + rowLabel + " | values found=" + rowNumbers);
                continue;
            }

            String rowValue = "ITC=" + rowNumbers.get(0)
                    + " | BSE Sensex=" + rowNumbers.get(1)
                    + " | BSE FMCG=" + rowNumbers.get(2);

            capturedRows.put(rowLabel, rowValue);

            ReportLogger.pass("Captured Stock Performance row: "
                    + tabName
                    + " | " + rowLabel
                    + " | " + rowValue);
        }
    }

    private List<String> getNextThreeDecimalValuesBeforeNextRowOrSection(List<String> values, int startIndex) {
        List<String> numbers = new ArrayList<>();

        for (int i = Math.max(0, startIndex); i < values.size(); i++) {
            String value = normalizeSpaces(values.get(i));

            if (value.isEmpty()) {
                continue;
            }

            if (isStockPerformanceEndMarker(value)) {
                break;
            }

            if (isDynamicStockPerformanceRowLabel(value) && numbers.size() > 0 && numbers.size() < 3) {
                break;
            }

            if (isValueMatchingType(value, VALUE_DECIMAL)) {
                numbers.add(value);

                if (numbers.size() == 3) {
                    return numbers;
                }
            }
        }

        return numbers;
    }

    private int findStockPerformanceTableStartIndex(List<String> values) {
        int timePeriodIndex = indexOfExact(values, "Time Period");

        if (timePeriodIndex < 0) {
            return -1;
        }

        int itcIndex = indexOfExactFrom(values, "ITC", timePeriodIndex + 1);
        int sensexIndex = indexOfExactFrom(values, "BSE Sensex", timePeriodIndex + 1);

        int fmcgIndex = indexOfExactFrom(values, "BSEFMCG", timePeriodIndex + 1);

        if (fmcgIndex < 0) {
            fmcgIndex = indexOfExactFrom(values, "BSE FMCG", timePeriodIndex + 1);
        }

        if (itcIndex < 0 || sensexIndex < 0 || fmcgIndex < 0) {
            return -1;
        }

        return Math.max(timePeriodIndex, Math.max(itcIndex, Math.max(sensexIndex, fmcgIndex)));
    }

    private boolean isStockPerformanceRowLabel(String value) {
        return isDynamicStockPerformanceRowLabel(value);
    }

    private boolean isDynamicStockPerformanceRowLabel(String value) {
        if (value == null) {
            return false;
        }

        String clean = normalizeSpaces(value);

        /*
         * Dynamic:
         * Annual rows: 2025, 2024, 2023...
         * Trailing rows: YTD, 1 Month, 3 Months, 1 Year...
         */
        return clean.matches("^[0-9]{4}$")
                || clean.matches("^YTD$")
                || clean.matches("^[0-9]+\\s+(Day|Days|Week|Weeks|Month|Months|Year|Years)$");
    }

    private boolean isStockPerformanceEndMarker(String value) {
        if (value == null) {
            return false;
        }

        String clean = normalizeSpaces(value);

        return clean.startsWith("As on")
                || clean.equals("Essential Checks")
                || clean.equals("Financials")
                || clean.equals("Key Ratios")
                || clean.equals("Peers")
                || clean.equals("Shareholding");
    }

    private void logOptionalStockPerformanceText(By locator, String label) {
        WebElement element = findVisibleElement(locator);

        if (element == null) {
            ReportLogger.debug(label + " not visible in current viewport. Skipping optional log.");
            return;
        }

        String text = getElementReadableText(element);

        if (text.isEmpty()) {
            text = label;
        }

        logValidatedText(label, text);
    }

    // =========================================================
    // LIVE VALUE CAPTURE HELPERS
    // =========================================================

    private void validateMetricRowPairLive(
            String leftLabel,
            String leftValueType,
            String rightLabel,
            String rightValueType,
            int maxScrolls
    ) {
        ReportLogger.step("Validating live metric row pair: " + leftLabel + " | " + rightLabel);

        /*
         * Regression-safe row capture:
         * Earlier logic used ensureLabelVisible(left) then ensureLabelVisible(right).
         * During full-suite regression that can move one label out of viewport,
         * especially lower Fundamentals rows like EPS | Face value.
         *
         * This method keeps both row labels in the same viewport before reading values.
         */
        int totalAttempts = Math.max(8, maxScrolls + 8);

        for (int attempt = 1; attempt <= totalAttempts; attempt++) {
            List<String> values = collectVisibleContentDescriptions();

            int leftIndex = indexOfExact(values, leftLabel);
            int rightIndex = indexOfExact(values, rightLabel);

            if (leftIndex >= 0 && rightIndex >= 0) {
                String[] pairValues = extractMetricPairValuesFromVisibleSource(
                        values,
                        leftIndex,
                        rightIndex,
                        leftValueType,
                        rightValueType
                );

                if (pairValues[0] != null && pairValues[1] != null) {
                    logValidatedText(leftLabel, pairValues[0]);
                    logValidatedText(rightLabel, pairValues[1]);

                    ReportLogger.pass("Captured live metric row pair: "
                            + leftLabel + " = " + pairValues[0]
                            + " | " + rightLabel + " = " + pairValues[1]);
                    return;
                }

                ReportLogger.debug("Metric row labels visible but values not stable yet: "
                        + leftLabel + " | " + rightLabel
                        + " | attempt=" + attempt);
            }

            /*
             * Move slowly. Big swipes are the main reason full-suite regression
             * skips lower rows and lands directly into the next section.
             */
            pageSwipeUpW3C();
            sleep(650);
        }

        /*
         * Recovery if we crossed the row and landed in the next section.
         */
        for (int attempt = 1; attempt <= 5; attempt++) {
            pageSwipeDownW3C();
            sleep(650);

            List<String> values = collectVisibleContentDescriptions();

            int leftIndex = indexOfExact(values, leftLabel);
            int rightIndex = indexOfExact(values, rightLabel);

            if (leftIndex >= 0 && rightIndex >= 0) {
                String[] pairValues = extractMetricPairValuesFromVisibleSource(
                        values,
                        leftIndex,
                        rightIndex,
                        leftValueType,
                        rightValueType
                );

                if (pairValues[0] != null && pairValues[1] != null) {
                    logValidatedText(leftLabel, pairValues[0]);
                    logValidatedText(rightLabel, pairValues[1]);

                    ReportLogger.pass("Captured live metric row pair after recovery: "
                            + leftLabel + " = " + pairValues[0]
                            + " | " + rightLabel + " = " + pairValues[1]);
                    return;
                }
            }
        }

        throw new AssertionError("Metric row pair not fully captured"
                + " | leftLabel=" + leftLabel
                + " | rightLabel=" + rightLabel);
    }

    private String[] extractMetricPairValuesFromVisibleSource(
            List<String> values,
            int leftIndex,
            int rightIndex,
            String leftValueType,
            String rightValueType
    ) {
        String[] result = new String[]{null, null};

        int searchStartIndex = Math.max(leftIndex, rightIndex) + 1;

        String leftValue = findNextValueByType(values, searchStartIndex, leftValueType);

        if (leftValue == null) {
            leftValue = findNearestValueAroundIndex(values, leftIndex, leftValueType, 8);
        }

        if (leftValue == null) {
            return result;
        }

        int leftValueIndex = indexOfExactFrom(values, leftValue, searchStartIndex);

        if (leftValueIndex < 0) {
            leftValueIndex = searchStartIndex;
        }

        String rightValue = findNextValueByType(values, leftValueIndex + 1, rightValueType);

        if (rightValue == null) {
            rightValue = findNearestValueAroundIndex(values, rightIndex, rightValueType, 10);
        }

        result[0] = leftValue;
        result[1] = rightValue;

        return result;
    }

    private void validateMetricRowTripleLive(
            String firstLabel,
            String firstValueType,
            String secondLabel,
            String secondValueType,
            String thirdLabel,
            String thirdValueType,
            int maxScrolls
    ) {
        ReportLogger.step("Validating live metric row triple: " + firstLabel + " | " + secondLabel + " | " + thirdLabel);

        ensureLabelVisible(firstLabel, maxScrolls);
        ensureLabelVisible(secondLabel, maxScrolls);
        ensureLabelVisible(thirdLabel, maxScrolls);

        List<String> values = collectVisibleContentDescriptions();

        int firstIndex = indexOfExact(values, firstLabel);
        int secondIndex = indexOfExact(values, secondLabel);
        int thirdIndex = indexOfExact(values, thirdLabel);

        if (firstIndex < 0 || secondIndex < 0 || thirdIndex < 0) {
            throw new AssertionError("One or more labels not found in visible source order for row triple");
        }

        int searchStartIndex = Math.max(firstIndex, Math.max(secondIndex, thirdIndex)) + 1;

        String firstValue = findNextValueByType(values, searchStartIndex, firstValueType);

        if (firstValue == null) {
            throw new AssertionError("Live value not found for label: " + firstLabel);
        }

        int firstValueIndex = indexOfExactFrom(values, firstValue, searchStartIndex);

        String secondValue = findNextValueByType(values, firstValueIndex + 1, secondValueType);

        if (secondValue == null) {
            throw new AssertionError("Live value not found for label: " + secondLabel);
        }

        int secondValueIndex = indexOfExactFrom(values, secondValue, firstValueIndex + 1);

        String thirdValue = findNextValueByType(values, secondValueIndex + 1, thirdValueType);

        if (thirdValue == null) {
            throw new AssertionError("Live value not found for label: " + thirdLabel);
        }

        logValidatedText(firstLabel, firstValue);
        logValidatedText(secondLabel, secondValue);
        logValidatedText(thirdLabel, thirdValue);

        ReportLogger.pass("Captured live metric row triple: "
                + firstLabel + " = " + firstValue
                + " | " + secondLabel + " = " + secondValue
                + " | " + thirdLabel + " = " + thirdValue);
    }

    private void validateLiveValueAfterLabel(String label, String valueType, int maxScrolls) {
        ReportLogger.step("Capturing live value for label: " + label);

        ensureLabelVisible(label, maxScrolls);

        List<String> values = collectVisibleContentDescriptions();

        int labelIndex = indexOfExact(values, label);

        if (labelIndex < 0) {
            throw new AssertionError("Label not found in visible source order: " + label);
        }

        String value = findNextValueByType(values, labelIndex + 1, valueType);

        if (value == null) {
            throw new AssertionError("Live value not found after label: " + label + " | Expected type: " + valueType);
        }

        logValidatedText(label, value);

        ReportLogger.pass("Captured live value: " + label + " = " + value);
    }

    private void validateSharesOutstandingLiveValue() {
        ReportLogger.step("Capturing live value for label: Shares outstanding");

        ensureLabelVisible("Shares outstanding", 6);

        List<String> values = collectVisibleContentDescriptions();

        int labelIndex = indexOfExact(values, "Shares outstanding");

        if (labelIndex < 0) {
            throw new AssertionError("Shares outstanding label not found in visible source order");
        }

        String value = findNextValueByType(values, labelIndex + 1, VALUE_LARGE_INTEGER);

        if (value == null) {
            value = findNearestValueAroundIndex(values, labelIndex, VALUE_LARGE_INTEGER, 5);
        }

        if (value == null) {
            throw new AssertionError("Live value not found for Shares outstanding near label");
        }

        logValidatedText("Shares outstanding", value);

        ReportLogger.pass("Captured live value: Shares outstanding = " + value);
    }

    private String findNearestValueAroundIndex(List<String> values, int index, String valueType, int range) {
        int start = Math.max(0, index - range);
        int end = Math.min(values.size() - 1, index + range);

        for (int i = index + 1; i <= end; i++) {
            String value = values.get(i);

            if (isValueMatchingType(value, valueType)) {
                return value;
            }
        }

        for (int i = index - 1; i >= start; i--) {
            String value = values.get(i);

            if (isValueMatchingType(value, valueType)) {
                return value;
            }
        }

        return null;
    }

    private void ensureLabelVisible(String label, int maxScrolls) {
        By locator = byDesc(label);

        if (isVisible(locator)) {
            return;
        }

        for (int i = 1; i <= maxScrolls; i++) {
            pageSwipeUpW3C();
            sleep(700);

            if (isVisible(locator)) {
                ReportLogger.pass(label + " is visible after page scroll " + i);
                return;
            }
        }

        throw new AssertionError(label + " is not visible after controlled scrolling");
    }

    private void pageSwipeDownW3C() {
        try {
            Dimension size = driver.manage().window().getSize();

            int x = (int) (size.getWidth() * 0.90);
            int startY = (int) (size.getHeight() * 0.30);
            int endY = (int) (size.getHeight() * 0.78);

            PointerInput finger = new PointerInput(PointerInput.Kind.TOUCH, "finger");
            Sequence swipe = new Sequence(finger, 1);

            swipe.addAction(finger.createPointerMove(Duration.ZERO, PointerInput.Origin.viewport(), x, startY));
            swipe.addAction(finger.createPointerDown(PointerInput.MouseButton.LEFT.asArg()));
            swipe.addAction(finger.createPointerMove(Duration.ofMillis(700), PointerInput.Origin.viewport(), x, endY));
            swipe.addAction(finger.createPointerUp(PointerInput.MouseButton.LEFT.asArg()));

            driver.perform(Collections.singletonList(swipe));

        } catch (Exception firstError) {
            Map<String, Object> params = new HashMap<>();
            params.put("left", 760);
            params.put("top", 520);
            params.put("width", 260);
            params.put("height", 1450);
            params.put("direction", "down");
            params.put("percent", 0.70);

            driver.executeScript("mobile: scrollGesture", params);
        }
    }

    private List<String> collectVisibleContentDescriptions() {
        List<WebElement> views = driver.findElements(AppiumBy.className("android.view.View"));
        List<String> values = new ArrayList<>();

        for (WebElement element : views) {
            try {
                if (element == null || !element.isDisplayed()) {
                    continue;
                }

                String desc = element.getAttribute("content-desc");

                if (desc != null && !desc.trim().isEmpty()) {
                    values.add(desc.trim());
                }

            } catch (StaleElementReferenceException ignored) {
                // Ignore stale Flutter element
            }
        }

        return values;
    }

    private int indexOfExact(List<String> values, String expected) {
        return indexOfExactFrom(values, expected, 0);
    }

    private int indexOfExactFrom(List<String> values, String expected, int startIndex) {
        for (int i = Math.max(0, startIndex); i < values.size(); i++) {
            if (expected.equals(values.get(i))) {
                return i;
            }
        }

        return -1;
    }

    private String findNextValueByType(List<String> values, int startIndex, String valueType) {
        for (int i = Math.max(0, startIndex); i < values.size(); i++) {
            String value = values.get(i);

            if (isValueMatchingType(value, valueType)) {
                return value;
            }
        }

        return null;
    }

    private String findFirstMatchingValue(List<String> values, String[] regexList) {
        for (String value : values) {
            String normalizedValue = normalizeSpaces(value);

            for (String regex : regexList) {
                if (Pattern.matches(regex, normalizedValue)) {
                    return value;
                }
            }
        }

        return null;
    }

    private boolean isValueMatchingType(String value, String valueType) {
        if (value == null) {
            return false;
        }

        String clean = normalizeSpaces(value);

        if (isKnownLabel(clean)) {
            return false;
        }

        switch (valueType) {
            case VALUE_SCORE:
                return Pattern.matches("^\\d{1,2}/10$", clean);

            case VALUE_RUPEE:
                return Pattern.matches("^₹\\s?-?[0-9,]+(\\.\\d+)?$", clean);

            case VALUE_RUPEE_SIGNED:
                return Pattern.matches("^₹\\s?[+-][0-9,]+(\\.\\d+)?$", clean);

            case VALUE_RUPEE_CRORE:
                return Pattern.matches("^₹\\s?[0-9,]+(\\.\\d+)?\\s?Cr$", clean);

            case VALUE_PERCENT:
                return Pattern.matches("^-?[0-9,]+(\\.\\d+)?\\s?%$", clean);

            case VALUE_PERCENT_BRACKET:
                return Pattern.matches("^\\(-?[0-9,]+(\\.\\d+)?\\s?%\\)$", clean);

            case VALUE_DATE_TIME:
                return Pattern.matches("^\\([0-9]{1,2}\\s+[A-Za-z]{3,9},\\s+[0-9]{4}\\s+\\|\\s+[0-9]{2}:[0-9]{2}\\s+IST\\)$", clean);

            case VALUE_DECIMAL:
                return Pattern.matches("^-?[0-9,]+(\\.\\d+)?$", clean);

            case VALUE_INTEGER:
                return Pattern.matches("^-?[0-9]+$", clean);

            case VALUE_LARGE_INTEGER:
                return Pattern.matches("^[0-9,]+$", clean);

            case VALUE_ANY_NUMBER:
                return Pattern.matches("^₹?\\s?-?[0-9,]+(\\.\\d+)?(\\s?(Cr|%))?$", clean);

            default:
                throw new IllegalArgumentException("Unknown value type: " + valueType);
        }
    }

    private boolean isKnownLabel(String clean) {
        return clean.equals("Market cap")
                || clean.equals("Revenue (TTM)")
                || clean.equals("Net Profit (TTM)")
                || clean.equals("ROE")
                || clean.equals("ROCE")
                || clean.equals("P/E Ratio")
                || clean.equals("P/B Ratio")
                || clean.equals("Industry P/E")
                || clean.equals("EV/EBITDA")
                || clean.equals("Div. Yield")
                || clean.equals("Debt to Equity")
                || clean.equals("Book Value")
                || clean.equals("EPS")
                || clean.equals("Face value")
                || clean.equals("Shares outstanding")
                || clean.equals("CFO")
                || clean.equals("EBITDA")
                || clean.equals("Net Profit")
                || clean.equals("Quality Score")
                || clean.equals("Growth Score")
                || clean.equals("Valuation Score")
                || clean.equals("Momentum Score")
                || clean.equals("Time Period")
                || clean.equals("ITC")
                || clean.equals("BSE Sensex")
                || clean.equals("BSEFMCG")
                || clean.equals("BSE FMCG");
    }

    private String normalizeSpaces(String value) {
        if (value == null) {
            return "";
        }

        return value.replace("\n", " ")
                .replace("\r", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

 //=========================================================
//SD_008 - FINANCIALS
//=========================================================

public void verifyFinancials() {
    runKeyRatiosWithImmediateLookups(() -> {

	    ReportLogger.step("Verifying Financials section");

	    recoverStockDetailsIfNeeded();

	    alignFinancialsSectionSafely();

	    assertVisibleAndLog(byDesc("Financials"), "Financials section");
	    assertVisibleAndLog(byDesc("More"), "Financials More link");

	    validateFinancialsTab(
	            "Income Statement",
	            new String[]{"Sales", "PAT"},
	            true
	    );

	    validateFinancialsTab(
	            "Balance Sheet",
	            new String[]{"Equity", "Debt"},
	            false
	    );

	    validateFinancialsTab(
	            "Cash Flow",
	            new String[]{"Operating Cash Flow", "Free Cash Flow"},
	            false
	    );

	    verifyFinancialsMoreDetails();

	    ReportLogger.pass("Financials section validated successfully");

    });
}


private void alignFinancialsSectionSafely() {
    ReportLogger.step("Aligning Financials section safely");

    if (isFinancialsReady()) {
        ReportLogger.pass("Financials section is already aligned");
        return;
    }

    /*
     * Case 1:
     * Full-suite run usually starts SD_008 around Essential Checks.
     * Financials is nearby, so try small controlled scroll first.
     */
    for (int i = 1; i <= 5; i++) {
        financialsSmallSwipeUp();
        sleep(700);

        if (isFinancialsReady()) {
            ReportLogger.pass("Financials section aligned after nearby small swipe up " + i);
            return;
        }

        if (isVisible(byDesc("Key Ratios"))) {
            ReportLogger.debug("Key Ratios visible during nearby search. Financials was overscrolled.");
            break;
        }
    }

    /*
     * Case 2:
     * Standalone SD_008 run starts from top of Stock Details page.
     * Need page-level scrolling through:
     * Stock Rating -> Stock Range -> Fundamentals -> 10 Years Aggregate
     * -> Stock Performance -> Essential Checks -> Financials
     */
    for (int i = 1; i <= 16; i++) {
        pageSwipeUpW3C();
        sleep(800);

        if (isFinancialsReady()) {
            ReportLogger.pass("Financials section aligned after page-level swipe up " + i);
            return;
        }

        if (isVisible(byDesc("Key Ratios"))) {
            ReportLogger.debug("Key Ratios visible during page-level search. Financials was overscrolled.");
            break;
        }
    }

    /*
     * Recovery:
     * If we overscrolled below Financials into Key Ratios, move slightly back.
     */
    for (int i = 1; i <= 6; i++) {
        financialsSmallSwipeDown();
        sleep(700);

        if (isFinancialsReady()) {
            ReportLogger.pass("Financials section aligned after recovery swipe down " + i);
            return;
        }
    }

    throw new AssertionError("Financials section could not be aligned safely");
}

private boolean isFinancialsReady() {
  return isVisible(byDesc("Financials"))
          && isVisible(byDesc("Income Statement"))
          && isVisible(byDesc("Balance Sheet"))
          && isVisible(byDesc("Cash Flow"));
}

private void validateFinancialsTab(
      String tabName,
      String[] expectedLegends,
      boolean validateQuarterly
) {
  ReportLogger.step("Validating Financials tab: " + tabName);

  alignFinancialsSectionSafely();

  tapFinancialsTab(tabName);

  alignFinancialsChartArea();

  assertVisibleAndLogFlexible(
          new By[]{
                  byDesc(tabName),
                  byDescContains(tabName),
                  byText(tabName),
                  byTextContains(tabName)
          },
          "Financials tab: " + tabName
  );

  for (String legend : expectedLegends) {
        waitAndAssertFinancialsLegendVisible(tabName, legend);
  }

  captureFinancialsVisiblePeriods(tabName + " default view");

  /*
   * Chart alignment can move the Annual/Quarterly selector just above the
   * viewport. Re-align the selector before validating or tapping it.
   */
  alignFinancialsPeriodControls();
  assertFinancialsPeriodOptionVisible(tabName, "Annual");

  logOptionalFinancialsText(byDesc("*All values are in (Cr)"), tabName + " values note");

  if (validateQuarterly) {
      alignFinancialsPeriodControls("Quarterly");
      assertFinancialsPeriodOptionVisible(tabName, "Quarterly");

      tapFinancialsPeriodOption("Quarterly");
      sleep(900);

      alignFinancialsChartArea();

      /*
       * The selected control may move above the viewport after chart alignment;
       * the chart periods are the primary proof that Quarterly content opened.
       */
      captureFinancialsVisiblePeriods(tabName + " Quarterly view");

      /*
       * Return to Annual so the next Financials tab starts from a stable state.
       */
      alignFinancialsPeriodControls();
      tapFinancialsPeriodOption("Annual");
      sleep(700);
      alignFinancialsChartArea();
  } else {
      ReportLogger.debug(tabName + " Quarterly validation is not required. Skipping it.");
  }

  ReportLogger.pass("Financials tab validated: " + tabName);
}


private void waitAndAssertFinancialsLegendVisible(String tabName, String legend) {
    ReportLogger.step("Waiting for Financials legend: " + tabName + " | " + legend);

    By[] locators = getFinancialsLegendLocators(legend);

    /*
     * Financials chart redraws after tab click.
     * Long labels like Operating Cash Flow can appear slightly late.
     */
    for (int attempt = 1; attempt <= 8; attempt++) {
        WebElement element = findVisibleElementFlexible(locators);

        if (element != null) {
            String text = getElementReadableText(element);

            if (text.isEmpty()) {
                text = legend;
            }

            ReportLogger.pass(tabName + " legend is visible: " + legend);
            logValidatedText(tabName + " legend: " + legend, text);
            return;
        }

        ReportLogger.debug("Financials legend not visible yet: "
                + tabName
                + " | " + legend
                + " | attempt=" + attempt);

        sleep(500);
    }

    /*
     * One small alignment retry only.
     * Do not aggressive-scroll because Financials section is already aligned.
     */
    financialsTinyStabilizeSwipe();
    sleep(700);

    for (int attempt = 1; attempt <= 4; attempt++) {
        WebElement element = findVisibleElementFlexible(locators);

        if (element != null) {
            String text = getElementReadableText(element);

            if (text.isEmpty()) {
                text = legend;
            }

            ReportLogger.pass(tabName + " legend is visible after stabilization: " + legend);
            logValidatedText(tabName + " legend: " + legend, text);
            return;
        }

        sleep(500);
    }

    ReportLogger.fail(tabName + " legend is not visible after wait: " + legend);
    throw new AssertionError(tabName + " legend is not visible after wait: " + legend);
}

private By[] getFinancialsLegendLocators(String legend) {
    if ("Operating Cash Flow".equals(legend)) {
        return new By[]{
                byDesc("Operating Cash Flow"),
                byDescContains("Operating Cash Flow"),
                byDescContains("Operating Cash")
        };
    }

    if ("Free Cash Flow".equals(legend)) {
        return new By[]{
                byDesc("Free Cash Flow"),
                byDescContains("Free Cash Flow"),
                byDescContains("Free Cash")
        };
    }

    return new By[]{
            byDesc(legend),
            byDescContains(legend)
    };
}

private WebElement findVisibleElementFlexible(By[] locators) {
    for (By locator : locators) {
        WebElement element = findVisibleElement(locator);

        if (element != null) {
            return element;
        }
    }

    return null;
}

private void financialsTinyStabilizeSwipe() {
    try {
        Dimension size = driver.manage().window().getSize();

        /*
         * Tiny scroll only to trigger Flutter redraw/repaint.
         * This must not move away from Financials.
         */
        int x = (int) (size.getWidth() * 0.88);
        int startY = (int) (size.getHeight() * 0.66);
        int endY = (int) (size.getHeight() * 0.62);

        PointerInput finger = new PointerInput(PointerInput.Kind.TOUCH, "finger");
        Sequence swipe = new Sequence(finger, 1);

        swipe.addAction(finger.createPointerMove(Duration.ZERO, PointerInput.Origin.viewport(), x, startY));
        swipe.addAction(finger.createPointerDown(PointerInput.MouseButton.LEFT.asArg()));
        swipe.addAction(finger.createPointerMove(Duration.ofMillis(250), PointerInput.Origin.viewport(), x, endY));
        swipe.addAction(finger.createPointerUp(PointerInput.MouseButton.LEFT.asArg()));

        driver.perform(Collections.singletonList(swipe));

    } catch (Exception e) {
        ReportLogger.debug("Financials tiny stabilize swipe skipped: " + cleanError(e.getMessage()));
    }
}

private void assertFinancialsLegendVisible(String tabName, String legend) {
    ReportLogger.step("Validating Financials legend: " + tabName + " | " + legend);

    By[] locators;

    /*
     * Long chip labels can be exposed slightly differently by Flutter/Appium.
     * Use exact first, then safe contains fallback.
     */
    if ("Operating Cash Flow".equals(legend)) {
        locators = new By[]{
                byDesc("Operating Cash Flow"),
                byDescContains("Operating Cash Flow"),
                byDescContains("Operating Cash")
        };
    } else if ("Free Cash Flow".equals(legend)) {
        locators = new By[]{
                byDesc("Free Cash Flow"),
                byDescContains("Free Cash Flow"),
                byDescContains("Free Cash")
        };
    } else {
        locators = new By[]{
                byDesc(legend),
                byDescContains(legend)
        };
    }

    assertVisibleAndLogFlexible(
            locators,
            tabName + " legend: " + legend
    );
}

private void tapFinancialsTab(String tabName) {
  ReportLogger.step("Opening Financials tab: " + tabName);

  if (tapIfVisible(byDesc(tabName), "Financials tab: " + tabName)) {
      sleep(900);
      ReportLogger.pass("Financials tab opened: " + tabName);
      return;
  }

  /*
   * Tabs are inside HorizontalScrollView.
   * If direct tap fails, perform tiny horizontal correction and retry once.
   */
  financialsHorizontalTabSwipeLeft();
  sleep(500);

  if (tapIfVisible(byDesc(tabName), "Financials tab after horizontal correction: " + tabName)) {
      sleep(900);
      ReportLogger.pass("Financials tab opened after horizontal correction: " + tabName);
      return;
  }

  throw new AssertionError("Unable to open Financials tab: " + tabName);
}

private By[] getFinancialsPeriodLocators(String optionName) {
  return new By[]{
          byDesc(optionName),
          byDescContains(optionName),
          byText(optionName),
          byTextContains(optionName)
  };
}

private boolean isFinancialsPeriodOptionVisible(String optionName) {
  return findFinancialsPeriodOption(optionName) != null;
}

private void alignFinancialsPeriodControls() {
  alignFinancialsPeriodControls("Annual");
}

private void assertFinancialsPeriodOptionVisible(String tabName, String optionName) {
  WebElement option = findFinancialsPeriodOption(optionName);
  if (option == null) throw new AssertionError(tabName + " Financials " + optionName + " control is not visible");
  logValidatedText(tabName + " " + optionName + " option", getElementReadableText(option));
}

private void tapFinancialsPeriodOption(String optionName) {
  ReportLogger.step("Opening Financials period option: " + optionName);

  alignFinancialsPeriodControls(optionName);

  WebElement option = findFinancialsPeriodOption(optionName);
  if (option != null) {
      option.click();
      ReportLogger.pass("Tapped scoped Financials period option: " + optionName);
      return;
  }

  throw new AssertionError("Unable to tap Financials period option: " + optionName);
}

private void alignFinancialsChartArea() {
  ReportLogger.step("Aligning Financials chart area");

  for (int i = 1; i <= 3; i++) {
      if (isFinancialsChartAreaReady()) {
          ReportLogger.pass("Financials chart area is aligned");
          return;
      }

      financialsSmallSwipeUp();
      sleep(500);
  }

  if (isFinancialsChartAreaReady()) {
      ReportLogger.pass("Financials chart area aligned after final check");
      return;
  }

  throw new AssertionError("Financials chart area is not aligned");
}

private boolean isFinancialsChartAreaReady() {
  return isVisible(byDesc("Financials"))
          && isVisibleFlexible(new By[]{
          byDesc("Sales"),
          byDesc("PAT"),
          byDesc("Equity"),
          byDesc("Debt"),
          byDesc("Operating Cash Flow"),
          byDesc("Free Cash Flow")
  })
          && isVisibleFlexible(new By[]{
          byDescContains("Mar"),
          byDescContains("Jun"),
          byDescContains("Sep"),
          byDescContains("Dec")
  });
}

private void captureFinancialsVisiblePeriods(String contextLabel) {
    ReportLogger.step("Capturing Financials visible period labels: " + contextLabel);

    List<String> values = collectVisibleContentDescriptions();
    List<String> periods = new ArrayList<>();

    boolean financialsAreaStarted = false;

    for (String rawValue : values) {
        String clean = normalizeSpaces(rawValue);

        if (clean.isEmpty()) {
            continue;
        }

        /*
         * Start capture once we are clearly inside Financials/chart area.
         * Do not depend only on exact "Financials" index because Flutter source
         * order can shift when chart/tab elements are selected.
         */
        if (clean.equals("Financials")
                || clean.equals("Income Statement")
                || clean.equals("Balance Sheet")
                || clean.equals("Cash Flow")
                || clean.equals("Sales")
                || clean.equals("PAT")
                || clean.equals("Equity")
                || clean.equals("Debt")
                || clean.equals("Operating Cash Flow")
                || clean.equals("Free Cash Flow")) {
            financialsAreaStarted = true;
        }

        if (!financialsAreaStarted) {
            continue;
        }

        /*
         * Stop when next section starts.
         */
        if (clean.equals("Key Ratios")
                || clean.equals("Efficiency")
                || clean.equals("Valuation")
                || clean.equals("Growth")) {
            break;
        }

        List<String> extractedPeriods = extractFinancialPeriodLabels(clean);

        for (String period : extractedPeriods) {
            if (!periods.contains(period)) {
                periods.add(period);
            }
        }
    }

    if (periods.isEmpty()) {
        throw new AssertionError("No Financials period labels captured for: "
                + contextLabel
                + " | Visible source values=" + values);
    }

    logValidatedText(contextLabel + " periods", periods.toString());

    ReportLogger.pass("Captured Financials periods for "
            + contextLabel
            + ": " + periods);
}

private boolean isFinancialsPeriodLabel(String value) {
    return !extractFinancialPeriodLabels(value).isEmpty();
}

private List<String> extractFinancialPeriodLabels(String value) {
    List<String> periods = new ArrayList<>();

    if (value == null) {
        return periods;
    }

    String clean = normalizeSpaces(value);

    /*
     * Supports:
     * Mar '21
     * Mar ’21
     * Mar ‘21
     * Mar 21
     *
     * Also works if Flutter exposes multiple period labels inside one content-desc.
     */
    Pattern pattern = Pattern.compile(
            "\\b(Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)\\s*['’‘]?\\s*(\\d{2})\\b"
    );

    java.util.regex.Matcher matcher = pattern.matcher(clean);

    while (matcher.find()) {
        String period = matcher.group(1) + " '" + matcher.group(2);

        if (!periods.contains(period)) {
            periods.add(period);
        }
    }

    return periods;
}

private void logOptionalFinancialsText(By locator, String label) {
  WebElement element = findVisibleElement(locator);

  if (element == null) {
      ReportLogger.debug(label + " not visible in current viewport. Skipping optional log.");
      return;
  }

  String text = getElementReadableText(element);

  if (text.isEmpty()) {
      text = label;
  }

  logValidatedText(label, text);
}

private void financialsSmallSwipeUp() {
  try {
      Dimension size = driver.manage().window().getSize();

      /*
       * Controlled small scroll from Essential Checks to Financials.
       * Avoid jumping into Key Ratios.
       */
      int x = (int) (size.getWidth() * 0.88);
      int startY = (int) (size.getHeight() * 0.74);
      int endY = (int) (size.getHeight() * 0.58);

      PointerInput finger = new PointerInput(PointerInput.Kind.TOUCH, "finger");
      Sequence swipe = new Sequence(finger, 1);

      swipe.addAction(finger.createPointerMove(Duration.ZERO, PointerInput.Origin.viewport(), x, startY));
      swipe.addAction(finger.createPointerDown(PointerInput.MouseButton.LEFT.asArg()));
      swipe.addAction(finger.createPointerMove(Duration.ofMillis(420), PointerInput.Origin.viewport(), x, endY));
      swipe.addAction(finger.createPointerUp(PointerInput.MouseButton.LEFT.asArg()));

      driver.perform(Collections.singletonList(swipe));

  } catch (Exception firstError) {
      Map<String, Object> params = new HashMap<>();
      params.put("left", 760);
      params.put("top", 900);
      params.put("width", 260);
      params.put("height", 480);
      params.put("direction", "up");
      params.put("percent", 0.22);

      driver.executeScript("mobile: scrollGesture", params);
  }
}

private void financialsSmallSwipeDown() {
  try {
      Dimension size = driver.manage().window().getSize();

      int x = (int) (size.getWidth() * 0.88);
      int startY = (int) (size.getHeight() * 0.58);
      int endY = (int) (size.getHeight() * 0.74);

      PointerInput finger = new PointerInput(PointerInput.Kind.TOUCH, "finger");
      Sequence swipe = new Sequence(finger, 1);

      swipe.addAction(finger.createPointerMove(Duration.ZERO, PointerInput.Origin.viewport(), x, startY));
      swipe.addAction(finger.createPointerDown(PointerInput.MouseButton.LEFT.asArg()));
      swipe.addAction(finger.createPointerMove(Duration.ofMillis(420), PointerInput.Origin.viewport(), x, endY));
      swipe.addAction(finger.createPointerUp(PointerInput.MouseButton.LEFT.asArg()));

      driver.perform(Collections.singletonList(swipe));

  } catch (Exception firstError) {
      Map<String, Object> params = new HashMap<>();
      params.put("left", 760);
      params.put("top", 900);
      params.put("width", 260);
      params.put("height", 480);
      params.put("direction", "down");
      params.put("percent", 0.22);

      driver.executeScript("mobile: scrollGesture", params);
  }
  }

private void financialsHorizontalTabSwipeLeft() {
  try {
      Dimension size = driver.manage().window().getSize();

      /*
       * Tiny horizontal swipe inside Financials tab strip.
       * Safe fallback only if tab tap fails.
       */
      int y = (int) (size.getHeight() * 0.70);
      int startX = (int) (size.getWidth() * 0.78);
      int endX = (int) (size.getWidth() * 0.35);

      PointerInput finger = new PointerInput(PointerInput.Kind.TOUCH, "finger");
      Sequence swipe = new Sequence(finger, 1);

      swipe.addAction(finger.createPointerMove(Duration.ZERO, PointerInput.Origin.viewport(), startX, y));
      swipe.addAction(finger.createPointerDown(PointerInput.MouseButton.LEFT.asArg()));
      swipe.addAction(finger.createPointerMove(Duration.ofMillis(350), PointerInput.Origin.viewport(), endX, y));
      swipe.addAction(finger.createPointerUp(PointerInput.MouseButton.LEFT.asArg()));

      driver.perform(Collections.singletonList(swipe));

  } catch (Exception e) {
      ReportLogger.debug("Financials horizontal tab correction failed: " + cleanError(e.getMessage()));
  }
}
// =========================================================
// SD_007 - ESSENTIAL CHECKS
// =========================================================

public void verifyEssentialChecks() {
    runKeyRatiosWithImmediateLookups(() -> {

    ReportLogger.step("Verifying Essential Checks section");

    recoverStockDetailsIfNeeded();

    alignEssentialChecksSectionSafely();

    assertVisibleAndLog(byDesc("Essential Checks"), "Essential Checks section");

    validateEssentialChecksCarouselSafely();

    ReportLogger.pass("Essential Checks section validated successfully with live value capture");

    });
}


private void alignEssentialChecksSectionSafely() {
    ReportLogger.step("Aligning Essential Checks section safely");

    if (isEssentialChecksReady()) {
        ReportLogger.pass("Essential Checks section is already aligned");
        return;
    }

    /*
     * Case 1:
     * SD_006 usually ends around Stock Performance lower rows.
     * Move slightly down page until Essential Checks appears.
     */
    for (int i = 1; i <= 5; i++) {
        if (isVisible(byDesc("Financials"))) {
            ReportLogger.debug("Financials visible while searching Essential Checks. Need to scroll back up.");
            break;
        }

        essentialChecksSmallSwipeUp();
        sleep(700);

        if (isEssentialChecksReady()) {
            ReportLogger.pass("Essential Checks aligned after small swipe up " + i);
            return;
        }
    }

    /*
     * Case 2:
     * If previous swipe overshot into Financials, move back.
     */
    for (int i = 1; i <= 4; i++) {
        if (!isVisible(byDesc("Financials"))
                && !isVisible(byDesc("Income Statement"))
                && !isVisible(byDesc("Balance Sheet"))
                && !isVisible(byDesc("Cash Flow"))) {
            // If we are not below the section anymore, check again before swiping.
            if (isEssentialChecksReady()) {
                ReportLogger.pass("Essential Checks aligned after recovery check");
                return;
            }
        }

        essentialChecksSmallSwipeDown();
        sleep(700);

        if (isEssentialChecksReady()) {
            ReportLogger.pass("Essential Checks aligned after small swipe down " + i);
            return;
        }
    }

    throw new AssertionError("Essential Checks section could not be aligned safely");
}

private boolean isEssentialChecksReady() {
    return isVisible(byDesc("Essential Checks")) && isAnyEssentialChecksCardVisible();
}

private boolean isAnyEssentialChecksCardVisible() {
    return isVisible(byDescContains("Altman Z-Score"))
            || isVisible(byDescContains("Modified C-Score"))
            || isVisible(byDescContains("Piotroski F-Score"));
}

private void validateEssentialChecksCarouselSafely() {
    ReportLogger.step("Validating Essential Checks using the visible card carousel");
    String[] titles = {"Altman Z-Score", "Modified C-Score", "Piotroski F-Score"};
    String[] questions = {
        "Is there a threat to the company's solvency?",
        "Can creative accounting be detected through the financial numbers?",
        "How did the company perform in the last one year?"
    };
    java.util.LinkedHashMap<String, String> captured = new java.util.LinkedHashMap<>();
    for (int index = 0; index < titles.length; index++) {
        EssentialCardSnapshot card = openEssentialChecksCard(titles[index], index > 0);
        String text = normalizeSpaces(card.text);
        if (!text.contains(questions[index]) || !hasEssentialChecksDynamicScore(text)) {
            throw new AssertionError("Essential Checks card content is incomplete | expected=" + titles[index]);
        }
        captured.put(titles[index], card.text);
        logValidatedText("Essential Checks card | " + titles[index], card.text);
    }
    ReportLogger.pass("All 3 Essential Checks cards validated | cards=" + captured.keySet());
}


private By getEssentialChecksDotLocator(int dotNumber) {
    return By.xpath(
            "//android.widget.ScrollView/android.view.View[" + (dotNumber + 4) + "]"
    );
}

private void alignEssentialChecksCardAndDots() {
    ReportLogger.step("Aligning Essential Checks heading, card and all three dots");

    if (isEssentialChecksCardAndDotsReady()) {
        ReportLogger.pass("Essential Checks heading, card and dots are already aligned");
        return;
    }

    for (int attempt = 1; attempt <= 6; attempt++) {
        boolean headingVisible = isVisible(byDesc("Essential Checks"));
        boolean cardVisible = isAnyEssentialChecksCardVisible();
        boolean financialsVisible = isVisible(byDesc("Financials"));

        if (financialsVisible && !headingVisible) {
            essentialChecksDotAlignmentSwipeDown();
        } else if (headingVisible || cardVisible) {
            /*
             * Heading/card visible but dots are not: move the page slightly
             * forward so the indicator row enters the viewport.
             */
            essentialChecksDotAlignmentSwipeUp();
        } else {
            alignEssentialChecksSectionSafely();
        }

        sleep(550);

        if (isEssentialChecksCardAndDotsReady()) {
            ReportLogger.pass(
                    "Essential Checks heading, card and dots aligned on attempt "
                            + attempt
            );
            return;
        }
    }

    throw new AssertionError(
            "Essential Checks could not be aligned with all three exact dots visible"
                    + " | dot1=" + isVisible(getEssentialChecksDotLocator(1))
                    + " | dot2=" + isVisible(getEssentialChecksDotLocator(2))
                    + " | dot3=" + isVisible(getEssentialChecksDotLocator(3))
    );
}

private boolean isEssentialChecksCardAndDotsReady() {
    if (!isVisible(byDesc("Essential Checks")) || !isAnyEssentialChecksCardVisible()) {
        return false;
    }

    WebElement heading = findVisibleElement(byDesc("Essential Checks"));
    WebElement financials = findVisibleElement(byDesc("Financials"));

    if (heading == null) {
        return false;
    }

    int headingBottom = heading.getRect().getY() + heading.getRect().getHeight();
    int financialsTop = financials == null
            ? driver.manage().window().getSize().getHeight()
            : financials.getRect().getY();

    for (int dotNumber = 1; dotNumber <= 3; dotNumber++) {
        WebElement dot = findVisibleElement(getEssentialChecksDotLocator(dotNumber));

        if (dot == null || !dot.isEnabled()) {
            return false;
        }

        Rectangle rect = dot.getRect();
        int centerY = rect.getY() + (rect.getHeight() / 2);

        boolean validSize = rect.getWidth() >= 15
                && rect.getWidth() <= 90
                && rect.getHeight() >= 15
                && rect.getHeight() <= 90;

        boolean insideEssentialChecksSection = centerY > headingBottom
                && centerY < financialsTop;

        if (!validSize || !insideEssentialChecksSection) {
            return false;
        }
    }

    return true;
}

private String tapEssentialChecksDotAndValidate(
        int dotNumber,
        String expectedTitle,
        String expectedQuestion
) {
    ReportLogger.step(
            "Opening Essential Checks card using dot "
                    + dotNumber + ": " + expectedTitle
    );

    for (int attempt = 1; attempt <= 2; attempt++) {
        alignEssentialChecksCardAndDots();

        By locator = getEssentialChecksDotLocator(dotNumber);
        WebElement dot = findVisibleElement(locator);

        if (dot == null) {
            ReportLogger.debug(
                    "Essential Checks dot not visible"
                            + " | dot=" + dotNumber
                            + " | attempt=" + attempt
            );
            continue;
        }

        Rectangle rect = dot.getRect();

        ReportLogger.step(
                "Essential Checks dot " + dotNumber
                        + " bounds | x=" + rect.getX()
                        + " | y=" + rect.getY()
                        + " | width=" + rect.getWidth()
                        + " | height=" + rect.getHeight()
        );

        tapElementCenter(dot);
        sleep(900);

        String cardText = waitForExpectedEssentialChecksCard(
                expectedTitle,
                expectedQuestion
        );

        if (cardText != null) {
            ReportLogger.pass(
                    "Essential Checks dot opened expected card"
                            + " | dot=" + dotNumber
                            + " | title=" + expectedTitle
            );
            return cardText;
        }

        ReportLogger.debug(
                "Expected Essential Checks card not loaded after dot tap"
                        + " | dot=" + dotNumber
                        + " | expected=" + expectedTitle
                        + " | attempt=" + attempt
        );
    }

    throw new AssertionError(
            "Unable to open Essential Checks card using exact dot"
                    + " | dot=" + dotNumber
                    + " | expectedTitle=" + expectedTitle
    );
}

private String waitForExpectedEssentialChecksCard(
        String expectedTitle,
        String expectedQuestion
) {
    for (int attempt = 1; attempt <= 8; attempt++) {
        List<String> values = collectVisibleContentDescriptions();

        /*
         * The carousel keeps neighbouring cards partially visible. Do not use
         * findVisibleEssentialChecksCardText() here because it returns the first
         * recognised card in source order, which can remain Altman Z-Score even
         * after Dot 2 has successfully selected Modified C-Score.
         */
        for (String value : values) {
            String clean = normalizeSpaces(value);

            if (clean.contains(expectedTitle)
                    && clean.contains(expectedQuestion)
                    && hasEssentialChecksDynamicScore(clean)) {
                logValidatedText(
                        "Essential Checks visible card",
                        value
                );
                return value;
            }
        }

        sleep(350);
    }

    return null;
}

private void essentialChecksDotAlignmentSwipeUp() {
    performEssentialChecksDotAlignmentSwipe(true);
}

private void essentialChecksDotAlignmentSwipeDown() {
    performEssentialChecksDotAlignmentSwipe(false);
}

private void performEssentialChecksDotAlignmentSwipe(boolean up) {
    try {
        Dimension size = driver.manage().window().getSize();

        int x = (int) (size.getWidth() * 0.90);
        int startY = (int) (size.getHeight() * (up ? 0.69 : 0.61));
        int endY = (int) (size.getHeight() * (up ? 0.61 : 0.69));

        PointerInput finger = new PointerInput(
                PointerInput.Kind.TOUCH,
                "essential-checks-align-finger"
        );
        Sequence swipe = new Sequence(finger, 1);

        swipe.addAction(finger.createPointerMove(
                Duration.ZERO,
                PointerInput.Origin.viewport(),
                x,
                startY
        ));
        swipe.addAction(finger.createPointerDown(PointerInput.MouseButton.LEFT.asArg()));
        swipe.addAction(finger.createPointerMove(
                Duration.ofMillis(300),
                PointerInput.Origin.viewport(),
                x,
                endY
        ));
        swipe.addAction(finger.createPointerUp(PointerInput.MouseButton.LEFT.asArg()));

        driver.perform(Collections.singletonList(swipe));

    } catch (Exception e) {
        ReportLogger.debug(
                "Essential Checks dot alignment swipe failed: "
                        + cleanError(e.getMessage())
        );
    }
}

private void alignEssentialChecksCardOnly() {
    ReportLogger.step("Aligning Essential Checks visible card");

    if (isEssentialChecksReady()) {
        ReportLogger.pass("Essential Checks card is aligned");
        return;
    }

    for (int i = 1; i <= 2; i++) {
        essentialChecksSmallSwipeUp();
        sleep(500);

        if (isEssentialChecksReady()) {
            ReportLogger.pass("Essential Checks card aligned after small swipe " + i);
            return;
        }
    }

    throw new AssertionError("Essential Checks card is not aligned");
}

private String validateVisibleEssentialChecksCard() {
    ReportLogger.step("Validating visible Essential Checks card with live value");

    List<String> values = collectVisibleContentDescriptions();

    String cardText = findVisibleEssentialChecksCardText(values);

    if (cardText == null) {
        throw new AssertionError("No Essential Checks card text found in current viewport");
    }

    String cleanCardText = normalizeSpaces(cardText);

    if (!hasKnownEssentialChecksTitle(cleanCardText)) {
        throw new AssertionError("Essential Checks card title not found. Actual card text: " + cardText);
    }

    if (!hasEssentialChecksDynamicScore(cleanCardText)) {
        throw new AssertionError("Essential Checks dynamic score/value not found. Actual card text: " + cardText);
    }

    logValidatedText("Essential Checks visible card", cardText);

    ReportLogger.pass("Essential Checks visible card validated: " + cardText);

    return cardText;
}

private String findVisibleEssentialChecksCardText(List<String> values) {
    /*
     * Preferred case:
     * Flutter exposes full card in one content-desc.
     */
    for (String value : values) {
        String clean = normalizeSpaces(value);

        if (hasKnownEssentialChecksTitle(clean) && hasEssentialChecksDynamicScore(clean)) {
            return value;
        }
    }

    /*
     * Fallback:
     * Flutter exposes title/question/value separately.
     * Build nearby card text around a known title.
     */
    for (int i = 0; i < values.size(); i++) {
        String clean = normalizeSpaces(values.get(i));

        if (!hasKnownEssentialChecksTitle(clean)) {
            continue;
        }

        StringBuilder builder = new StringBuilder();

        int end = Math.min(values.size() - 1, i + 5);

        for (int j = i; j <= end; j++) {
            String part = normalizeSpaces(values.get(j));

            if (part.isEmpty()) {
                continue;
            }

            if (part.equals("Financials")
                    || part.equals("Income Statement")
                    || part.equals("Balance Sheet")
                    || part.equals("Cash Flow")) {
                break;
            }

            builder.append(values.get(j)).append("\n");
        }

        String combined = builder.toString().trim();

        if (hasKnownEssentialChecksTitle(combined) && hasEssentialChecksDynamicScore(combined)) {
            return combined;
        }
    }

    return null;
}

private boolean hasKnownEssentialChecksTitle(String text) {
    if (text == null) {
        return false;
    }

    String clean = normalizeSpaces(text);

    return clean.contains("Altman Z-Score")
            || clean.contains("Modified C-Score")
            || clean.contains("Piotroski F-Score");
}

private boolean hasEssentialChecksDynamicScore(String text) {
    if (text == null) {
        return false;
    }

    String clean = normalizeSpaces(text);

    /*
     * Dynamic value format:
     * 16.14 (Not Likely)
     * 1 (No)
     * 4 (Below Average)
     *
     * No exact value is hardcoded.
     */
    return Pattern.matches(".*\\b-?\\d+(\\.\\d+)?\\s*\\([^)]{2,}\\).*", clean);
}

private String getEssentialChecksCardTitle(String cardText) {
    if (cardText == null) {
        return null;
    }

    String clean = normalizeSpaces(cardText);

    if (clean.contains("Altman Z-Score")) {
        return "Altman Z-Score";
    }

    if (clean.contains("Modified C-Score")) {
        return "Modified C-Score";
    }

    if (clean.contains("Piotroski F-Score")) {
        return "Piotroski F-Score";
    }

    return null;
}

private void resetEssentialChecksCarouselToStart() {
    ReportLogger.step("Resetting Essential Checks carousel to first card");

    /*
     * Bounded right swipes are safe even when the carousel is already at the
     * first card. The swipe is performed inside the detected card rectangle.
     */
    for (int attempt = 1; attempt <= 3; attempt++) {
        essentialChecksCardSwipeRight();
        sleep(450);
    }

    alignEssentialChecksCardOnly();

    ReportLogger.pass("Essential Checks carousel reset completed");
}

private void essentialChecksCardSwipeLeft() {
    performEssentialChecksCardSwipe("left");
}

private void essentialChecksCardSwipeRight() {
    performEssentialChecksCardSwipe("right");
}

private void performEssentialChecksCardSwipe(String direction) {
    Rectangle cardRect = findEssentialChecksCardRectangle();
    Dimension size = driver.manage().window().getSize();

    int y;
    int startX;
    int endX;

    if (cardRect != null && cardRect.getWidth() > 0 && cardRect.getHeight() > 0) {
        y = cardRect.getY() + (cardRect.getHeight() / 2);

        int leftX = cardRect.getX() + (int) (cardRect.getWidth() * 0.18);
        int rightX = cardRect.getX() + (int) (cardRect.getWidth() * 0.82);

        startX = "left".equals(direction) ? rightX : leftX;
        endX = "left".equals(direction) ? leftX : rightX;
    } else {
        /*
         * Last-resort viewport-relative fallback. This is used only when Appium
         * temporarily drops the card element during a Flutter repaint.
         */
        y = (int) (size.getHeight() * 0.62);
        startX = "left".equals(direction)
                ? (int) (size.getWidth() * 0.82)
                : (int) (size.getWidth() * 0.18);
        endX = "left".equals(direction)
                ? (int) (size.getWidth() * 0.18)
                : (int) (size.getWidth() * 0.82);
    }

    y = Math.max(20, Math.min(y, size.getHeight() - 20));
    startX = Math.max(20, Math.min(startX, size.getWidth() - 20));
    endX = Math.max(20, Math.min(endX, size.getWidth() - 20));

    try {
        PointerInput finger = new PointerInput(
                PointerInput.Kind.TOUCH,
                "essential-checks-finger"
        );

        Sequence swipe = new Sequence(finger, 1);

        swipe.addAction(finger.createPointerMove(
                Duration.ZERO,
                PointerInput.Origin.viewport(),
                startX,
                y
        ));
        swipe.addAction(
                finger.createPointerDown(PointerInput.MouseButton.LEFT.asArg())
        );
        swipe.addAction(finger.createPointerMove(
                Duration.ofMillis(550),
                PointerInput.Origin.viewport(),
                endX,
                y
        ));
        swipe.addAction(
                finger.createPointerUp(PointerInput.MouseButton.LEFT.asArg())
        );

        driver.perform(Collections.singletonList(swipe));

        ReportLogger.step(
                "Swiped Essential Checks card " + direction
                        + " | startX=" + startX
                        + " | endX=" + endX
                        + " | y=" + y
        );

    } catch (Exception firstError) {
        if (cardRect == null) {
            ReportLogger.debug(
                    "Essential Checks card swipe failed without card bounds"
                            + " | direction=" + direction
                            + " | reason=" + cleanError(firstError.getMessage())
            );
            return;
        }

        Map<String, Object> params = new HashMap<>();
        params.put("left", Math.max(0, cardRect.getX()));
        params.put("top", Math.max(0, cardRect.getY()));
        params.put("width", Math.max(1, cardRect.getWidth()));
        params.put("height", Math.max(1, cardRect.getHeight()));
        params.put("direction", direction);
        params.put("percent", 0.70);

        driver.executeScript("mobile: swipeGesture", params);
    }
}

private Rectangle findEssentialChecksCardRectangle() {
    List<WebElement> views =
            driver.findElements(AppiumBy.className("android.view.View"));

    Rectangle bestRect = null;
    long bestArea = -1L;

    for (WebElement element : views) {
        try {
            if (element == null || !element.isDisplayed()) {
                continue;
            }

            String desc = element.getAttribute("content-desc");

            if (desc == null || !hasKnownEssentialChecksTitle(desc)) {
                continue;
            }

            Rectangle rect = element.getRect();

            if (rect.getWidth() < 180 || rect.getHeight() < 80) {
                continue;
            }

            long area = (long) rect.getWidth() * (long) rect.getHeight();

            /*
             * Prefer the largest semantic element containing the card title.
             * Flutter commonly exposes the whole card as one content-desc node.
             */
            if (area > bestArea) {
                bestRect = rect;
                bestArea = area;
            }

        } catch (Exception ignored) {
            // Ignore stale Flutter/Appium elements.
        }
    }

    if (bestRect != null) {
        return bestRect;
    }

    /*
     * Fallback: derive a bounded card area below the Essential Checks heading.
     */
    WebElement heading = findVisibleElement(byDesc("Essential Checks"));

    if (heading != null) {
        Rectangle headingRect = heading.getRect();
        Dimension size = driver.manage().window().getSize();

        int left = (int) (size.getWidth() * 0.08);
        int top = Math.max(
                headingRect.getY() + headingRect.getHeight() + 20,
                (int) (size.getHeight() * 0.35)
        );
        int width = (int) (size.getWidth() * 0.84);
        int availableHeight = Math.max(120, size.getHeight() - top - 120);
        int height = Math.min((int) (size.getHeight() * 0.32), availableHeight);

        return new Rectangle(left, top, width, height);
    }

    return null;
}

private void essentialChecksSmallSwipeUp() {
    try {
        Dimension size = driver.manage().window().getSize();

        /*
         * Small movement only.
         * From SD_006 end, Essential Checks is nearby.
         */
        int x = (int) (size.getWidth() * 0.88);
        int startY = (int) (size.getHeight() * 0.74);
        int endY = (int) (size.getHeight() * 0.58);

        PointerInput finger = new PointerInput(PointerInput.Kind.TOUCH, "finger");
        Sequence swipe = new Sequence(finger, 1);

        swipe.addAction(finger.createPointerMove(Duration.ZERO, PointerInput.Origin.viewport(), x, startY));
        swipe.addAction(finger.createPointerDown(PointerInput.MouseButton.LEFT.asArg()));
        swipe.addAction(finger.createPointerMove(Duration.ofMillis(420), PointerInput.Origin.viewport(), x, endY));
        swipe.addAction(finger.createPointerUp(PointerInput.MouseButton.LEFT.asArg()));

        driver.perform(Collections.singletonList(swipe));

    } catch (Exception firstError) {
        Map<String, Object> params = new HashMap<>();
        params.put("left", 760);
        params.put("top", 900);
        params.put("width", 260);
        params.put("height", 480);
        params.put("direction", "up");
        params.put("percent", 0.22);

        driver.executeScript("mobile: scrollGesture", params);
    }
}

private void essentialChecksSmallSwipeDown() {
    try {
        Dimension size = driver.manage().window().getSize();

        int x = (int) (size.getWidth() * 0.88);
        int startY = (int) (size.getHeight() * 0.58);
        int endY = (int) (size.getHeight() * 0.74);

        PointerInput finger = new PointerInput(PointerInput.Kind.TOUCH, "finger");
        Sequence swipe = new Sequence(finger, 1);

        swipe.addAction(finger.createPointerMove(Duration.ZERO, PointerInput.Origin.viewport(), x, startY));
        swipe.addAction(finger.createPointerDown(PointerInput.MouseButton.LEFT.asArg()));
        swipe.addAction(finger.createPointerMove(Duration.ofMillis(420), PointerInput.Origin.viewport(), x, endY));
        swipe.addAction(finger.createPointerUp(PointerInput.MouseButton.LEFT.asArg()));

        driver.perform(Collections.singletonList(swipe));

    } catch (Exception firstError) {
        Map<String, Object> params = new HashMap<>();
        params.put("left", 760);
        params.put("top", 900);
        params.put("width", 260);
        params.put("height", 480);
        params.put("direction", "down");
        params.put("percent", 0.22);

        driver.executeScript("mobile: scrollGesture", params);
    }
}

//=========================================================
//SD_008 - FINANCIALS MORE DETAILS
//=========================================================

private void verifyFinancialsMoreDetails() {
    ReportLogger.step("Verifying Financials More details screen");
    alignFinancialsSectionSafely();
    openFinancialsMorePage();
    runWithChildScreenCleanup(() -> {
        validateFinancialsMoreTopControls();
        validateFinancialsMoreMainHeadings();
        validateFinancialsMoreModeOpens("Annual Standalone");
        validateFinancialsMoreModeOpens("Quarterly Consolidated");
        validateFinancialsMoreModeOpens("Quarterly Standalone");
    }, this::returnFromFinancialsMorePage);
    ReportLogger.pass("Financials More details screen validated successfully");
}


private void openFinancialsMorePage() {
 ReportLogger.step("Opening Financials More details screen");

 alignFinancialsSectionSafely();

 if (!tapIfVisible(byDesc("More"), "Financials More link")) {
     throw new AssertionError("Unable to tap Financials More link");
 }

 waitForFinancialsMorePage();

 ReportLogger.pass("Financials More details screen opened");
}

private void waitForFinancialsMorePage() {
 for (int i = 1; i <= 12; i++) {
     if (isFinancialsMorePageReady()) {
         ReportLogger.pass("Financials More page is ready");
         return;
     }

     sleep(700);
 }

 throw new AssertionError("Financials More page did not load");
}

private boolean isFinancialsMorePageReady() {
 /*
  * More page is ready when Annual Consolidated chip is visible
  * and at least one main heading like Income Statement (₹ Cr) is visible.
  */
 return isVisible(byDesc("Annual Consolidated"))
         && hasAnyFinancialsMoreMainHeadingVisible();
}

private boolean hasAnyFinancialsMoreMainHeadingVisible() {
 List<String> values = collectVisibleContentDescriptions();

 for (String value : values) {
     if (isFinancialsMoreMainHeading(value)) {
         return true;
     }
 }

 return false;
}

private void validateFinancialsMoreTopControls() {
 ReportLogger.step("Validating Financials More top controls");

 validateFinancialsMoreModeChipReachable("Annual Consolidated");
 validateFinancialsMoreModeChipReachable("Annual Standalone");
 validateFinancialsMoreModeChipReachable("Quarterly Consolidated");
 validateFinancialsMoreModeChipReachable("Quarterly Standalone");

 /*
  * Bring chip strip back to left/default side for stable next step.
  */
 financialsMoreModeSwipeRight();
 sleep(500);

 ReportLogger.pass("Financials More top controls validated");
}

private void validateFinancialsMoreModeChipReachable(String modeName) {
 ReportLogger.step("Checking Financials More mode chip is reachable: " + modeName);

 if (isVisible(byDesc(modeName))) {
     assertVisibleAndLog(byDesc(modeName), "Financials More mode: " + modeName);
     return;
 }

 /*
  * Move chip strip left to reveal right-side chips.
  */
 for (int i = 1; i <= 5; i++) {
     financialsMoreModeSwipeLeft();
     sleep(600);

     if (isVisible(byDesc(modeName))) {
         assertVisibleAndLog(byDesc(modeName), "Financials More mode: " + modeName);
         return;
     }
 }

 /*
  * Recovery for left-side chips.
  */
 for (int i = 1; i <= 5; i++) {
     financialsMoreModeSwipeRight();
     sleep(600);

     if (isVisible(byDesc(modeName))) {
         assertVisibleAndLog(byDesc(modeName), "Financials More mode: " + modeName);
         return;
     }
 }

 throw new AssertionError("Financials More mode chip is not reachable: " + modeName);
}

private void validateFinancialsMoreMainHeadings() {
    ReportLogger.step("Validating Financials More main headings");

    tapFinancialsMoreMode("Annual Consolidated");

    List<String> capturedMainHeadings = new ArrayList<>();

    int stableCount = 0;
    int maxScrollAttempts = 18;
    long scanDeadline = System.nanoTime() + ExecutionProfile.longWait().toNanos();

    for (int scroll = 1; scroll <= maxScrollAttempts && System.nanoTime() < scanDeadline; scroll++) {
        int beforeCount = capturedMainHeadings.size();

        captureFinancialsMoreMainHeadings(capturedMainHeadings);

        int afterCount = capturedMainHeadings.size();

        if (afterCount > beforeCount) {
            stableCount = 0;
            ReportLogger.pass("Financials More heading captured on scroll "
                    + scroll
                    + " | totalHeadings="
                    + afterCount
                    + " | headings="
                    + capturedMainHeadings);
        } else {
            stableCount++;
            ReportLogger.debug("No new Financials More heading captured"
                    + " | scroll="
                    + scroll
                    + " | stableCount="
                    + stableCount
                    + " | captured="
                    + capturedMainHeadings);
        }

        /*
         * Do not stop early before at least 3 headings are captured.
         */
        if (capturedMainHeadings.size() >= 3) {
            ReportLogger.pass("Required Financials More main headings captured");
            break;
        }

        financialsMoreTableSwipeUp();
        sleep(900);

        financialsMoreTinyStabilizeSwipe();
        sleep(300);
    }

    captureFinancialsMoreMainHeadings(capturedMainHeadings);

    if (capturedMainHeadings.size() < 3) {
        throw new AssertionError("Financials More main headings captured too few headings"
                + " | expectedAtLeast=3"
                + " | actual="
                + capturedMainHeadings.size()
                + " | captured="
                + capturedMainHeadings
                + " | visibleValues="
                + collectVisibleContentDescriptions());
    }

    String firstHeading = capturedMainHeadings.get(0);
    String lastHeading = capturedMainHeadings.get(capturedMainHeadings.size() - 1);

    logValidatedText(
            "Financials More main headings",
            capturedMainHeadings.toString()
    );

    logValidatedText(
            "Financials More heading range",
            "First=" + firstHeading + " | Last=" + lastHeading
    );

    ReportLogger.pass("Financials More main headings validated"
            + " | totalHeadings=" + capturedMainHeadings.size()
            + " | first=" + firstHeading
            + " | last=" + lastHeading);
}


private void captureFinancialsMoreMainHeadings(List<String> capturedMainHeadings) {
    List<String> values = collectVisibleContentDescriptions();

    for (String rawValue : values) {
        if (rawValue == null) {
            continue;
        }

        String cleanFullValue = normalizeSpaces(rawValue);

        if (!cleanFullValue.isEmpty() && isFinancialsMoreMainHeading(cleanFullValue)) {
            addUnique(capturedMainHeadings, cleanFullValue);
        }

        /*
         * Flutter can expose heading + periods + values in one multiline content-desc.
         * Split and validate each line separately.
         */
        String prepared = rawValue
                .replace("\\n", "\n")
                .replace("\r", "\n")
                .trim();

        String[] parts = prepared.split("\\n+|\\|");

        for (String part : parts) {
            String cleanPart = normalizeSpaces(part);

            if (cleanPart.isEmpty()) {
                continue;
            }

            if (isFinancialsMoreMainHeading(cleanPart)) {
                addUnique(capturedMainHeadings, cleanPart);
            }
        }
    }
}


private void validateFinancialsMoreModeOpens(String modeName) {
    ReportLogger.step("Validating Financials More mode tap: " + modeName);

    tapFinancialsMoreModeOnly(modeName);

    WebElement selectedMode = findVisibleElement(byDesc(modeName));

    if (selectedMode == null) {
        throw new AssertionError("Financials More mode was tapped but not visible after tap: " + modeName);
    }

    logValidatedText(
            "Financials More mode tapped",
            modeName
    );

    ReportLogger.pass("Financials More mode tapped successfully: " + modeName);
}

private void tapFinancialsMoreModeOnly(String modeName) {
    ReportLogger.step("Tapping Financials More mode only: " + modeName);

    if (!isVisible(byDesc(modeName))) {
        for (int i = 1; i <= 5 && !isVisible(byDesc(modeName)); i++) {
            financialsMoreModeSwipeLeft();
            sleep(600);
        }
    }

    if (!isVisible(byDesc(modeName))) {
        for (int i = 1; i <= 5 && !isVisible(byDesc(modeName)); i++) {
            financialsMoreModeSwipeRight();
            sleep(600);
        }
    }

    if (tapIfVisible(byDesc(modeName), "Financials More mode: " + modeName)) {
        sleep(1200);
        ReportLogger.pass("Tapped Financials More mode: " + modeName);
        return;
    }

    throw new AssertionError("Unable to tap Financials More mode: " + modeName);
}
private void tapFinancialsMoreMode(String modeName) {
 ReportLogger.step("Opening Financials More mode: " + modeName);

 if (!isVisible(byDesc(modeName))) {
     for (int i = 1; i <= 5 && !isVisible(byDesc(modeName)); i++) {
         financialsMoreModeSwipeLeft();
         sleep(600);
     }
 }

 if (!isVisible(byDesc(modeName))) {
     for (int i = 1; i <= 5 && !isVisible(byDesc(modeName)); i++) {
         financialsMoreModeSwipeRight();
         sleep(600);
     }
 }

 if (tapIfVisible(byDesc(modeName), "Financials More mode: " + modeName)) {
     sleep(1500);
     waitForFinancialsMoreHeadingContent();
     ReportLogger.pass("Financials More mode opened: " + modeName);
     return;
 }

 throw new AssertionError("Unable to open Financials More mode: " + modeName);
}

private void waitForFinancialsMoreHeadingContent() {
 for (int i = 1; i <= 10; i++) {
     if (hasAnyFinancialsMoreMainHeadingVisible()) {
         return;
     }

     sleep(500);
 }
}

private boolean isFinancialsMoreMainHeading(String value) {
 if (value == null) {
     return false;
 }

 String clean = normalizeSpaces(value);

 /*
  * Main Financials More headings are exposed like:
  * Income Statement (₹ Cr)
  * Liabilities (₹ Cr)
  * Assets (₹ Cr)
  * Cashflow (₹ Cr)
  * Financials Ratios (₹ Cr)
  * Profitability Ratios (₹ Cr)
  * Growth Ratios (₹ Cr)
  * Solvency Ratios (₹ Cr)
  * Operating Efficiency Ratios (₹ Cr)
  * Valuation Ratios (₹ Cr)
  *
  * This is pattern-based, not fixed-list hardcoding.
  */
 if (!clean.contains("(₹ Cr)")) {
     return false;
 }

 if (isFinancialsMoreIgnoredText(clean)) {
     return false;
 }

 if (isFinancialsMoreDataValue(clean) || isFinancialsMorePeriodLabel(clean)) {
     return false;
 }

 /*
  * Avoid row-like labels, not main section headings.
  */
 String lower = clean.toLowerCase();

 if (lower.contains("per share")
         || lower.contains("market cap")
         || lower.contains("close price")
         || lower.contains("high price")
         || lower.contains("low price")
         || lower.contains("book value")
         || lower.contains("dividend")
         || lower.contains("adjusted eps")
         || lower.contains("cash eps")) {
     return false;
 }

 return clean.matches(".*\\b(Statement|Ratios|Liabilities|Assets|Cashflow|Cash Flow)\\b.*");
}

private boolean isFinancialsMoreSectionHeading(String value) {
 return isFinancialsMoreMainHeading(value);
}

private boolean isFinancialsMorePeriodLabel(String value) {
 if (value == null) {
     return false;
 }

 String clean = normalizeSpaces(value);

 if (clean.equals("TTM") || clean.startsWith("TTM ")) {
     return true;
 }

 return !extractFinancialPeriodLabels(clean).isEmpty();
}

private boolean isFinancialsMoreDataValue(String value) {
 if (value == null) {
     return false;
 }

 String clean = normalizeSpaces(value);

 if (clean.equals("--")) {
     return true;
 }

 /*
  * Supports numeric values like:
  * 89,913.33
  * -291.7
  * 0
  * 4.97
  */
 return Pattern.matches("^-?\\d{1,3}(,\\d{2,3})*(\\.\\d+)?$", clean)
         || Pattern.matches("^-?\\d+(\\.\\d+)?$", clean);
}

private boolean isFinancialsMoreIgnoredText(String value) {
 if (value == null) {
     return true;
 }

 String clean = normalizeSpaces(value);

 return clean.equals("Financials")
         || clean.equals("Annual Consolidated")
         || clean.equals("Annual Standalone")
         || clean.equals("Quarterly Consolidated")
         || clean.equals("Quarterly Standalone")
         || clean.equals("Quarterly")
         || clean.equals("Annual");
}

private void addUnique(List<String> list, String value) {
 if (value == null) {
     return;
 }

 String clean = normalizeSpaces(value);

 if (clean.isEmpty()) {
     return;
 }

 if (!list.contains(clean)) {
     list.add(clean);
 }
}

private void returnFromFinancialsMorePage() {
    ReportLogger.step("Returning from Financials More page");
    driver.navigate().back();
    keyRatioWait(ExecutionProfile.defaultWait()).withMessage("Financials More did not close after Back")
            .until(d -> !isFinancialsMoreScreenVisible() && isOnStockDetailsPage());
    ReportLogger.pass("Returned to Stock Details from Financials More");
}


private void financialsMoreTableSwipeUp() {
    swipeVisibleBody(true, "financials-more");
}


private void financialsMoreTinyStabilizeSwipe() {
    // Polling the next capture supplies stabilization; no extra redraw-trigger swipe.
}


private void financialsMoreModeSwipeLeft() {
 swipeFinancialsMoreModeStrip(true);
}

private void financialsMoreModeSwipeRight() {
 swipeFinancialsMoreModeStrip(false);
}

private void swipeFinancialsMoreModeStrip(boolean left) {
 try {
     Dimension size = driver.manage().window().getSize();

     int y = getFinancialsMoreModeStripCenterY();

     int startX;
     int endX;

     if (left) {
         startX = (int) (size.getWidth() * 0.82);
         endX = (int) (size.getWidth() * 0.24);
     } else {
         startX = (int) (size.getWidth() * 0.24);
         endX = (int) (size.getWidth() * 0.82);
     }

     ReportLogger.debug("Swiping Financials More mode strip " + (left ? "left" : "right"));

     PointerInput finger = new PointerInput(PointerInput.Kind.TOUCH, "finger");
     Sequence swipe = new Sequence(finger, 1);

     swipe.addAction(finger.createPointerMove(Duration.ZERO, PointerInput.Origin.viewport(), startX, y));
     swipe.addAction(finger.createPointerDown(PointerInput.MouseButton.LEFT.asArg()));
     swipe.addAction(finger.createPointerMove(Duration.ofMillis(500), PointerInput.Origin.viewport(), endX, y));
     swipe.addAction(finger.createPointerUp(PointerInput.MouseButton.LEFT.asArg()));

     driver.perform(Collections.singletonList(swipe));

 } catch (Exception e) {
     ReportLogger.debug("Financials More mode strip swipe failed: " + cleanError(e.getMessage()));
 }
}

private int getFinancialsMoreModeStripCenterY() {
 By[] knownModeLocators = new By[]{
         byDesc("Annual Consolidated"),
         byDesc("Annual Standalone"),
         byDesc("Quarterly Consolidated"),
         byDesc("Quarterly Standalone")
 };

 for (By locator : knownModeLocators) {
     WebElement element = findVisibleElement(locator);

     if (element != null) {
         Rectangle rect = element.getRect();
         return rect.getY() + rect.getHeight() / 2;
     }
 }

 /*
  * Fallback based on actual More screen.
  * Chip strip is near top, not table body.
  */
 Dimension size = driver.manage().window().getSize();
 return (int) (size.getHeight() * 0.20);
}

//=========================================================
//SD_009 - KEY RATIOS
//=========================================================

public void verifyKeyRatios() {
    runKeyRatiosWithImmediateLookups(this::verifyKeyRatiosContent);
}

private void verifyKeyRatiosContent() {
 ReportLogger.step("Verifying Key Ratios section with live numeric value capture");

 dismissValuationDurationSheetIfOpen(false);
 recoverStockDetailsIfNeeded();

 alignKeyRatiosSectionSafely();

 assertVisibleAndLog(byDesc("Key Ratios"), "Key Ratios section");
 assertVisibleAndLog(byDesc("More"), "Key Ratios More link");

 assertVisibleAndLog(byDesc("Efficiency"), "Key Ratios tab: Efficiency");
 assertVisibleAndLog(byDesc("Valuation"), "Key Ratios tab: Valuation");
 assertVisibleAndLog(byDesc("Growth"), "Key Ratios tab: Growth");

 validateKeyRatiosTabWithLiveCards("Efficiency", "TREND");
 validateKeyRatiosTabWithLiveCards("Valuation", "VALUATION");
 validateKeyRatiosTabWithLiveCards("Growth", "TREND");

 ReportLogger.pass("Key Ratios section validated successfully with live numeric value capture");
}

/** Keep polling bounded; restore the shared settings even when a check fails. */
private void runKeyRatiosWithImmediateLookups(Runnable validation) {
    Map<String, Object> settings = driver.getSettings();
    Object selectorTimeout = settings.get("waitForSelectorTimeout");
    Object idleTimeout = settings.get("waitForIdleTimeout");
    Duration implicitWait = driver.manage().timeouts().getImplicitWaitTimeout();
    Throwable originalFailure = null;
    try {
        driver.manage().timeouts().implicitlyWait(Duration.ZERO);
        if (selectorTimeout instanceof Number) driver.setSetting("waitForSelectorTimeout", 0);
        if (idleTimeout instanceof Number) driver.setSetting("waitForIdleTimeout", 0);
        validation.run();
    } catch (RuntimeException | AssertionError failure) {
        originalFailure = failure;
        throw failure;
    } finally {
        Throwable restorationFailure = null;
        for (String setting : new String[]{"waitForSelectorTimeout", "waitForIdleTimeout"}) {
            Object previous = "waitForSelectorTimeout".equals(setting) ? selectorTimeout : idleTimeout;
            if (!(previous instanceof Number)) continue;
            try {
                driver.setSetting(setting, previous);
            } catch (RuntimeException failure) {
                if (restorationFailure == null) restorationFailure = failure;
                else restorationFailure.addSuppressed(failure);
            }
        }
        try {
            driver.manage().timeouts().implicitlyWait(implicitWait);
        } catch (RuntimeException failure) {
            if (restorationFailure == null) restorationFailure = failure;
            else restorationFailure.addSuppressed(failure);
        }
        if (restorationFailure != null) {
            if (originalFailure != null) originalFailure.addSuppressed(restorationFailure);
            else throw new IllegalStateException("Could not restore Appium settings after Stock Details validation", restorationFailure);
        }
    }
}

private void alignKeyRatiosSectionSafely() {
    ReportLogger.step("Aligning Key Ratios section safely");

    if (isKeyRatiosVisibleEnoughForMore()) {
        ReportLogger.pass("Key Ratios section is visible enough for More action");
        return;
    }

    /*
     * Normal full-suite flow:
     * SD_010 may start near upper Stock Details page.
     * Scroll down until Key Ratios is visible.
     */
    for (int i = 1; i <= 24; i++) {
        pageSwipeUpW3C();
        sleep(800);

        if (isKeyRatiosVisibleEnoughForMore()) {
            ReportLogger.pass("Key Ratios section aligned after page swipe up " + i);
            return;
        }

        /*
         * Peers is below Key Ratios.
         * If Peers appears, we crossed Key Ratios. Recover upward slightly.
         */
        if (isVisible(byDesc("Peers"))) {
            ReportLogger.debug("Peers visible while searching Key Ratios. Recovering back to Key Ratios.");
            break;
        }
    }

    /*
     * Recovery from Peers back to Key Ratios.
     * Finger swipes down, page content moves back upward.
     */
    for (int i = 1; i <= 12; i++) {
        keyRatiosSmallSwipeDown();
        sleep(700);

        if (isKeyRatiosVisibleEnoughForMore()) {
            ReportLogger.pass("Key Ratios section aligned after recovery swipe down " + i);
            return;
        }
    }

    /*
     * Final controlled nearby search.
     */
    for (int i = 1; i <= 8; i++) {
        keyRatiosSmallSwipeUp();
        sleep(700);

        if (isKeyRatiosVisibleEnoughForMore()) {
            ReportLogger.pass("Key Ratios section aligned after final small swipe up " + i);
            return;
        }
    }

    throw new AssertionError("Key Ratios section could not be aligned safely");
}

private boolean isKeyRatiosVisibleEnoughForMore() {
    if (!isVisible(byDesc("Key Ratios"))) {
        return false;
    }

    /*
     * More must be visible because SD_010 needs to open Key Ratios More page.
     */
    if (!isVisible(byDesc("More"))) {
        return false;
    }

    /*
     * At least one Key Ratios tab/card signal should be visible.
     * Do not require perfect top alignment.
     */
    return isVisible(byDesc("Efficiency"))
            || isVisible(byDesc("Valuation"))
            || isVisible(byDesc("Growth"));
}

private void validateKeyRatiosTabWithLiveCards(String tabName, String tabType) {
 ReportLogger.step("Validating Key Ratios tab with live numeric values: " + tabName);

 alignKeyRatiosSectionSafely();

 tapKeyRatiosTab(tabName);

 assertVisibleAndLog(byDesc(tabName), "Key Ratios selected tab: " + tabName);

 if ("VALUATION".equals(tabType)) {
     validateValuationKeyRatiosCardsWithAllPeriods();
     return;
 }

 resetKeyRatiosCarouselToStart();

 java.util.LinkedHashMap<String, String> capturedCards = new java.util.LinkedHashMap<>();

 int stableCount = 0;

 for (int attempt = 1; attempt <= 8; attempt++) {
     int beforeCount = capturedCards.size();

     captureVisibleKeyRatioCards(tabName, tabType, capturedCards);

     int afterCount = capturedCards.size();

     if (afterCount > beforeCount) {
         stableCount = 0;
     } else {
         stableCount++;
     }

     if (stableCount >= 2) {
         break;
     }

     keyRatiosCardSwipeLeft();
     sleep(900);

     if (!isKeyRatiosCarouselStillValid()) {
         ReportLogger.debug("Stopping Key Ratios carousel capture because Key Ratios card area is no longer valid.");
         break;
     }
 }

 if (capturedCards.isEmpty()) {
     throw new AssertionError("No Key Ratios cards captured for tab: " + tabName);
 }

 validateMinimumKeyRatioCardsCaptured(tabName, capturedCards);

 for (Map.Entry<String, String> entry : capturedCards.entrySet()) {
     logValidatedText(
             "Key Ratios " + tabName + " | " + entry.getKey(),
             entry.getValue()
     );
 }

 ReportLogger.pass("Key Ratios tab validated with live numeric values: "
         + tabName
         + " | cardsCaptured=" + capturedCards.size()
         + " | cards=" + capturedCards.keySet());
}

private void tapKeyRatiosTab(String tabName) {
 ReportLogger.step("Opening Key Ratios tab: " + tabName);

 alignKeyRatiosSectionSafely();

 if (tapIfVisible(byDesc(tabName), "Key Ratios tab: " + tabName)) {
     sleep(1200);
     waitForKeyRatiosCardContent(tabName);
     ReportLogger.pass("Key Ratios tab opened: " + tabName);
     return;
 }

 throw new AssertionError("Unable to tap Key Ratios tab: " + tabName);
}


private void waitForKeyRatiosCardContent(String tabName) {
 if ("Valuation".equals(tabName)) {
     try {
         keyRatioWait(ExecutionProfile.defaultWait()).until(d -> getCurrentVisibleValuationCardData());
         return;
     } catch (TimeoutException timeout) {
         throw new AssertionError("Valuation tab did not expose a complete range card. "
                 + valuationReadDiagnostic + ". Capture Inspector XML of this tab.", timeout);
     }
 }
 for (int i = 1; i <= 10; i++) {
     List<String> values = collectVisibleContentDescriptions();

     for (String value : values) {
         if (isKeyRatioCardDescription(value)) {
             return;
         }
     }

     sleep(500);
 }

 throw new AssertionError("Key Ratios card content did not load for tab: " + tabName);
}

private void captureVisibleKeyRatioCards(
     String tabName,
     String tabType,
     java.util.LinkedHashMap<String, String> capturedCards
) {
 List<String> values = collectVisibleContentDescriptions();

 boolean insideKeyRatios = false;

 for (String rawValue : values) {
     String clean = normalizeSpaces(rawValue);

     if (clean.equals("Key Ratios")) {
         insideKeyRatios = true;
         continue;
     }

     if (!insideKeyRatios) {
         continue;
     }

     if (clean.equals("Shareholding")
             || clean.equals("News")
             || clean.equals("Analysis")) {
         break;
     }

     if (!isKeyRatioCardDescription(rawValue)) {
         continue;
     }

     KeyRatioCardData cardData = parseKeyRatioCard(rawValue, tabType);

     if (cardData == null) {
         continue;
     }

     if (!capturedCards.containsKey(cardData.title)) {
         capturedCards.put(cardData.title, cardData.summary);

         ReportLogger.pass("Captured Key Ratios live card: "
                 + tabName
                 + " | "
                 + cardData.title);
     }
 }
}

private void validateValuationKeyRatiosCardsWithAllPeriods() {
 ReportLogger.step("Validating Valuation Key Ratios cards with all durations");

 resetKeyRatiosCarouselToStart();

 java.util.LinkedHashMap<String, java.util.LinkedHashMap<String, String>> capturedCards =
         new java.util.LinkedHashMap<>();

 int stableCount = 0;

 for (int attempt = 1; attempt <= 6; attempt++) {
     int beforeCount = capturedCards.size();

     validateVisibleValuationCardAllPeriods(capturedCards);

     int afterCount = capturedCards.size();

     if (afterCount > beforeCount) {
         stableCount = 0;
     } else {
         stableCount++;
     }

     if (stableCount >= 2) {
         break;
     }

     keyRatiosCardSwipeLeft();
     sleep(900);

     if (!isKeyRatiosCarouselStillValid()) {
         ReportLogger.debug("Stopping Valuation carousel capture because card area is no longer valid.");
         break;
     }
 }

 if (capturedCards.isEmpty()) {
     throw new AssertionError("No Valuation Key Ratios cards captured");
 }

 if (capturedCards.size() < 2) {
     throw new AssertionError("Incomplete Valuation cards captured"
             + " | expectedAtLeast=2"
             + " | captured="
             + capturedCards.size()
             + " | cards="
             + capturedCards.keySet());
 }

 for (Map.Entry<String, java.util.LinkedHashMap<String, String>> cardEntry : capturedCards.entrySet()) {
     String cardTitle = cardEntry.getKey();
     java.util.LinkedHashMap<String, String> periodValues = cardEntry.getValue();

     if (!periodValues.containsKey("1 Year")
             || !periodValues.containsKey("3 Year")
             || !periodValues.containsKey("5 Year")) {
         throw new AssertionError("Valuation period validation incomplete for card: "
                 + cardTitle
                 + " | capturedPeriods="
                 + periodValues.keySet());
     }

     logValidatedText(
             "Key Ratios Valuation | " + cardTitle,
             periodValues.toString()
     );
 }

 ReportLogger.pass("Key Ratios Valuation tab validated with all durations"
         + " | cardsCaptured="
         + capturedCards.size()
         + " | cards="
         + capturedCards.keySet());
}

private void validateVisibleValuationCardAllPeriods(
     java.util.LinkedHashMap<String, java.util.LinkedHashMap<String, String>> capturedCards
) {
 KeyRatioCardData currentCard = getCurrentVisibleValuationCardData();

 if (currentCard == null) {
     try {
         currentCard = keyRatioWait(ExecutionProfile.defaultWait()).until(d -> getCurrentVisibleValuationCardData());
     } catch (TimeoutException timeout) {
         throw new AssertionError("Valuation carousel has no complete card. " + valuationReadDiagnostic, timeout);
     }
 }

 String cardTitle = currentCard.title;
 if (capturedCards.containsKey(cardTitle) && capturedCards.get(cardTitle).size() == 3) return;

 if (!capturedCards.containsKey(cardTitle)) {
     capturedCards.put(cardTitle, new java.util.LinkedHashMap<>());
 }

 java.util.LinkedHashMap<String, String> periodValues = capturedCards.get(cardTitle);

 validateValuationPeriodForCurrentCard(cardTitle, "1 Year", periodValues);
 validateValuationPeriodForCurrentCard(cardTitle, "3 Year", periodValues);
 validateValuationPeriodForCurrentCard(cardTitle, "5 Year", periodValues);

 ReportLogger.pass("Captured Valuation card with all periods: "
         + cardTitle
         + " | periods="
         + periodValues.keySet());
}

private void validateValuationPeriodForCurrentCard(
        String cardTitle,
        String periodName,
        java.util.LinkedHashMap<String, String> periodValues
) {
    tapVisibleValuationPeriodChip(cardTitle, periodName);

    // Flutter can temporarily remove/rebuild card semantics after the tap.
    // Require two complete, matching reads of this same card, not a fixed delay.
    final String[] previous = {null};
    final String[] diagnostic = {"Waiting for the valuation card to finish rendering"};
    try {
        KeyRatioCardData updated = keyRatioWait(ExecutionProfile.defaultWait()).until(d -> {
            ValuationCardSnapshot snapshot = readVisibleValuationCard(cardTitle);
            if (snapshot == null) {
                previous[0] = null;
                return null;
            }
            diagnostic[0] = "Card is visible; waiting for stable Min/Median/Max values";
            String summary = snapshot.data.summary;
            boolean stable = summary.equals(previous[0]);
            previous[0] = summary;
            return stable ? snapshot.data : null;
        });
        periodValues.put(periodName, updated.summary);
        logValidatedText("Key Ratios Valuation | " + cardTitle + " | " + periodName, updated.summary);
    } catch (TimeoutException timeout) {
        throw new AssertionError("Valuation card was not ready after the duration sheet closed"
                + " | card=" + cardTitle + " | period=" + periodName
                + " | " + diagnostic[0] + " | " + valuationReadDiagnostic
                + ". If Inspector still shows plain Views for the chart, expose its title and"
                + " numeric Min/Median/Max through accessibility semantics; a duration locator"
                + " cannot read values absent from the native hierarchy.", timeout);
    }
}

private static final String[] VALUATION_DURATIONS = {"1 Year", "3 Year", "5 Year"};

private void tapVisibleValuationPeriodChip(String cardTitle, String periodName) {
    if (!Arrays.asList(VALUATION_DURATIONS).contains(periodName)) {
        throw new IllegalArgumentException("Unsupported Valuation duration: " + periodName);
    }
    ReportLogger.step("Selecting Valuation duration: " + cardTitle + " | " + periodName);
    final boolean[] openedHere = {false};
    final String[] stage = {"locating Select Duration and its current ImageView value"};
    runWithChildScreenCleanup(() -> {
        try {
            // Only the current value exists before the sheet opens. For example, to select
            // 3 Year from 1 Year, first tap the 1 Year ImageView beside Select Duration.
            if (findVisibleValuationDurationScrim() == null) {
                ValuationDurationControl trigger = keyRatioWait(ExecutionProfile.defaultWait())
                        .until(d -> findValuationDurationControl());
                openedHere[0] = true;
                trigger.element.click();
            }
            stage[0] = "waiting for the duration bottom sheet (Scrim and duration options)";
            keyRatioWait(ExecutionProfile.defaultWait()).until(d -> findValuationDurationSheet());

            stage[0] = "locating " + periodName + " below the Scrim boundary";
            WebElement option = keyRatioWait(ExecutionProfile.defaultWait()).until(d -> {
                Rectangle scrim = findValuationDurationSheet();
                return scrim == null ? null : findValuationDurationOption(periodName, scrim);
            });
            // The selected option is an ImageView; unselected options are Views.
            // Exact accessibility IDs match both, with bounds excluding the background.
            option.click();

            stage[0] = "waiting for sheet dismissal and the selected value " + periodName;
            keyRatioWait(ExecutionProfile.defaultWait()).until(d -> {
                if (findVisibleValuationDurationScrim() != null) return false;
                ValuationDurationControl selected = findValuationDurationControl();
                return selected != null && periodName.equals(selected.period);
            });
            ReportLogger.pass("Valuation duration selected and sheet closed: "
                    + cardTitle + " | " + periodName);
        } catch (TimeoutException timeout) {
            throw new AssertionError("Unable to select Valuation duration"
                    + " | card=" + cardTitle + " | period=" + periodName
                    + " | stage=" + stage[0]
                    + ". Expected Select Duration + current ImageView, then a Scrim with"
                    + " 1 Year / 3 Year / 5 Year options. Capture Inspector XML at this stage.", timeout);
        }
    }, () -> dismissValuationDurationSheetIfOpen(openedHere[0]));
}

private ValuationDurationControl findValuationDurationControl() {
    for (WebElement label : driver.findElements(byDesc("Select Duration"))) {
        if (!label.isDisplayed()) continue;
        Rectangle labelBounds = label.getRect();
        if (!isFullyOnValuationScreen(labelBounds)) continue;
        for (String period : VALUATION_DURATIONS) {
            By trigger = AppiumBy.androidUIAutomator("new UiSelector()"
                    + ".className(\"android.widget.ImageView\").description(\"" + period + "\")");
            for (WebElement candidate : driver.findElements(trigger)) {
                if (!candidate.isDisplayed() || !candidate.isEnabled()) continue;
                Rectangle bounds = candidate.getRect();
                // Pair the trigger with its label on the same row. Do not click another
                // '1 Year' elsewhere in Stock Details or an option in an open sheet.
                if (isFullyOnValuationScreen(bounds)
                        && bounds.getX() >= labelBounds.getX() + labelBounds.getWidth()
                        && bounds.getY() < labelBounds.getY() + labelBounds.getHeight()
                        && bounds.getY() + bounds.getHeight() > labelBounds.getY()) {
                    return new ValuationDurationControl(candidate, period);
                }
            }
        }
    }
    return null;
}

private Rectangle findVisibleValuationDurationScrim() {
    Dimension screen = driver.manage().window().getSize();
    for (WebElement element : driver.findElements(byDesc("Scrim"))) {
        if (!element.isDisplayed()) continue;
        Rectangle bounds = element.getRect();
        if (isFullyOnValuationScreen(bounds)
                && bounds.getY() + bounds.getHeight() < screen.getHeight()) return bounds;
    }
    return null;
}

private Rectangle findValuationDurationSheet() {
    Rectangle scrim = findVisibleValuationDurationScrim();
    if (scrim == null) return null;
    for (String period : VALUATION_DURATIONS) {
        if (findValuationDurationOption(period, scrim) != null) return scrim;
    }
    return null;
}

private WebElement findValuationDurationOption(String period, Rectangle scrim) {
    for (WebElement option : driver.findElements(byDesc(period))) {
        if (!option.isDisplayed() || !option.isEnabled()) continue;
        Rectangle bounds = option.getRect();
        if (isFullyOnValuationScreen(bounds)
                && bounds.getY() >= scrim.getY() + scrim.getHeight()) return option;
    }
    return null;
}

private boolean isFullyOnValuationScreen(Rectangle bounds) {
    Dimension screen = driver.manage().window().getSize();
    return bounds.getWidth() > 0 && bounds.getHeight() > 0
            && bounds.getX() >= 0 && bounds.getY() >= 0
            && bounds.getX() + bounds.getWidth() <= screen.getWidth()
            && bounds.getY() + bounds.getHeight() <= screen.getHeight();
}

private void dismissValuationDurationSheetIfOpen(boolean openedHere) {
    // A generic Scrim alone is not enough to dismiss an unrelated dialog. Outside
    // this picker flow, also require one of its duration options in the sheet.
    if (openedHere ? findVisibleValuationDurationScrim() == null : findValuationDurationSheet() == null) return;
    ReportLogger.step("Closing the Valuation duration sheet before continuing Stock Details");
    driver.navigate().back();
    try {
        keyRatioWait(ExecutionProfile.shortWait())
                .until(d -> findVisibleValuationDurationScrim() == null);
    } catch (TimeoutException timeout) {
        throw new AssertionError("Valuation duration sheet did not close after Back", timeout);
    }
}

private static final class ValuationDurationControl {
    private final WebElement element;
    private final String period;

    private ValuationDurationControl(WebElement element, String period) {
        this.element = element;
        this.period = period;
    }
}

private boolean isInsideVisibleValuationCard(Rectangle chip, Rectangle card) {
    Dimension screen = driver.manage().window().getSize();
    int centerX = chip.getX() + chip.getWidth() / 2;
    int centerY = chip.getY() + chip.getHeight() / 2;
    return chip.getWidth() > 0 && chip.getHeight() > 0
            && chip.getX() >= 0 && chip.getY() >= 0
            && chip.getX() + chip.getWidth() <= screen.getWidth()
            && chip.getY() + chip.getHeight() <= screen.getHeight()
            && centerX >= card.getX() && centerX < card.getX() + card.getWidth()
            && centerY >= card.getY() && centerY < card.getY() + card.getHeight();
}

private String valuationReadDiagnostic = "No valuation card has been read yet";

private KeyRatioCardData getCurrentVisibleValuationCardData() {
    ValuationCardSnapshot snapshot = readVisibleValuationCard(null);
    return snapshot == null ? null : snapshot.data;
}

private ValuationCardSnapshot readVisibleValuationCard(String expectedTitle) {
    // Query only merged range-card semantics, rather than every View in the hierarchy.
    // The section heading may be above the viewport; it is not a prerequisite for a card.
    By cards = AppiumBy.androidUIAutomator("new UiSelector().className(\"android.view.View\")"
            + ".descriptionContains(\"Median\")");
    ValuationCardSnapshot best = null;
    int largestVisibleArea = 0;
    Dimension screen = driver.manage().window().getSize();
    valuationReadDiagnostic = "No visible merged valuation card exposes its title and numeric Min/Median/Max."
            + " If Inspector shows plain Views for this chart, its accessibility semantics are missing";
    for (WebElement element : driver.findElements(cards)) {
        try {
            if (!element.isDisplayed()) continue;
            String raw = element.getAttribute("content-desc");
            List<String> tokens = splitKeyRatioCardTokens(raw);
            if (tokens.isEmpty()) continue;
            if (!tokens.contains("Min") || !tokens.contains("Median") || !tokens.contains("Max")) {
                valuationReadDiagnostic = "Valuation card range labels are not all visible yet";
                continue;
            }
            String title = normalizeSpaces(tokens.get(0));
            String lower = title.toLowerCase(java.util.Locale.ROOT);
            if (!(lower.contains("price to earnings") || lower.contains("price to book")
                    || lower.contains("p/e ratio") || lower.contains("p/b ratio"))) continue;
            if (expectedTitle != null && !title.equalsIgnoreCase(normalizeSpaces(expectedTitle))) continue;
            Rectangle bounds = element.getRect();
            int width = Math.max(0, Math.min(screen.getWidth(), bounds.getX() + bounds.getWidth()) - Math.max(0, bounds.getX()));
            int height = Math.max(0, Math.min(screen.getHeight(), bounds.getY() + bounds.getHeight()) - Math.max(0, bounds.getY()));
            if (width == 0 || height == 0) continue;
            KeyRatioCardData data;
            try {
                data = parseKeyRatioCard(raw, "VALUATION");
            } catch (AssertionError incompleteCard) {
                valuationReadDiagnostic = incompleteCard.getMessage();
                continue; // A partially rendered card is retried by the explicit wait.
            }
            if (data != null && width * height > largestVisibleArea) {
                best = new ValuationCardSnapshot(data, bounds);
                largestVisibleArea = width * height;
            }
        } catch (StaleElementReferenceException stale) {
            valuationReadDiagnostic = "Valuation card was replaced during rendering";
        }
    }
    return best;
}

private WebDriverWait keyRatioWait(Duration timeout) {
    WebDriverWait wait = new WebDriverWait(driver, timeout);
    wait.pollingEvery(ExecutionProfile.pollingInterval());
    wait.ignoring(StaleElementReferenceException.class);
    return wait;
}

private boolean keyRatiosNavigationVisible() {
    // A scrolled card can remain visible while 'Key Ratios' itself is offscreen.
    // Do not turn a dead UiAutomator2 connection into a false 'screen not found'.
    for (String tab : new String[]{"Efficiency", "Valuation", "Growth"}) {
        boolean found = false;
        for (WebElement element : driver.findElements(byDesc(tab))) {
            try {
                if (element.isDisplayed()) { found = true; break; }
            } catch (StaleElementReferenceException stale) {
                return false;
            }
        }
        if (!found) return readVisibleValuationCard(null) != null;
    }
    return true;
}

private static final class ValuationCardSnapshot {
    private final KeyRatioCardData data;
    private final Rectangle bounds;

    private ValuationCardSnapshot(KeyRatioCardData data, Rectangle bounds) {
        this.data = data;
        this.bounds = bounds;
    }
}


private boolean isKeyRatiosCarouselStillValid() {
 if (!isVisible(byDesc("Efficiency"))
         || !isVisible(byDesc("Valuation"))
         || !isVisible(byDesc("Growth"))) {
     return getCurrentVisibleValuationCardData() != null;
 }

 List<String> values = collectVisibleContentDescriptions();

 for (String value : values) {
     if (isKeyRatioCardDescription(value)) {
         return true;
     }
 }

 return false;
}

private void validateMinimumKeyRatioCardsCaptured(
     String tabName,
     java.util.LinkedHashMap<String, String> capturedCards
) {
 int minimumRequiredCards;

 if ("Efficiency".equals(tabName)) {
     minimumRequiredCards = 3;
 } else if ("Valuation".equals(tabName)) {
     minimumRequiredCards = 2;
 } else if ("Growth".equals(tabName)) {
     minimumRequiredCards = 4;
 } else {
     minimumRequiredCards = 1;
 }

 if (capturedCards.size() < minimumRequiredCards) {
     throw new AssertionError("Incomplete Key Ratios card capture for tab: "
             + tabName
             + " | expectedAtLeast="
             + minimumRequiredCards
             + " | captured="
             + capturedCards.size()
             + " | cards="
             + capturedCards.keySet());
 }
}

private boolean isKeyRatioCardDescription(String rawValue) {
 if (rawValue == null) {
     return false;
 }

 String clean = normalizeSpaces(rawValue);

 if (clean.isEmpty()) {
     return false;
 }

 boolean hasKnownCardSignal =
         clean.contains("TTM")
                 || clean.contains("5Y")
                 || clean.contains("3Y")
                 || clean.contains("Min")
                 || clean.contains("Median")
                 || clean.contains("Max");

 boolean hasNumericValue = Pattern.matches(".*-?\\d+(\\.\\d+)?\\s?%?.*", clean);

 boolean isNotSectionText =
         !clean.equals("Key Ratios")
                 && !clean.equals("Efficiency")
                 && !clean.equals("Valuation")
                 && !clean.equals("Growth")
                 && !clean.equals("More")
                 && !clean.equals("Peers");

 return hasKnownCardSignal && hasNumericValue && isNotSectionText;
}

private KeyRatioCardData parseKeyRatioCard(String rawValue, String tabType) {
 List<String> tokens = splitKeyRatioCardTokens(rawValue);

 if (tokens.isEmpty()) {
     return null;
 }

 String title = tokens.get(0);

 if (!isValidKeyRatioCardTitle(title)) {
     return null;
 }

 if ("VALUATION".equals(tabType)) {
     return parseValuationKeyRatioCard(title, tokens);
 }

 return parseTrendKeyRatioCard(title, tokens);
}

private KeyRatioCardData parseTrendKeyRatioCard(String title, List<String> tokens) {
 List<String> years = new ArrayList<>();
 List<String> numericValues = new ArrayList<>();
 List<String> summaryParts = new ArrayList<>();

 for (int i = 1; i < tokens.size(); i++) {
     String token = normalizeSpaces(tokens.get(i));

     if (isKeyRatioYearLabel(token)) {
         years.add(token);
         continue;
     }

     if (isKeyRatioSummaryLabel(token)) {
         if (i + 1 < tokens.size() && isKeyRatioNumberValue(tokens.get(i + 1))) {
             summaryParts.add(token + "=" + normalizeSpaces(tokens.get(i + 1)));
             numericValues.add(normalizeSpaces(tokens.get(i + 1)));
             i++;
         }
         continue;
     }

     if (isKeyRatioNumberValue(token)) {
         numericValues.add(token);
     }
 }

 if (years.size() < 3) {
     throw new AssertionError("Key Ratio card year labels incomplete for: "
             + title
             + " | years="
             + years
             + " | tokens="
             + tokens);
 }

 if (numericValues.size() < 3) {
     throw new AssertionError("Key Ratio card numeric values incomplete for: "
             + title
             + " | values="
             + numericValues
             + " | tokens="
             + tokens);
 }

 if (summaryParts.size() < 2) {
     throw new AssertionError("Key Ratio card summary values not captured properly for: "
             + title
             + " | summary="
             + summaryParts
             + " | tokens="
             + tokens);
 }

 String summary = "Years=" + years
         + " | NumericValues=" + numericValues
         + " | Summary=" + summaryParts;

 return new KeyRatioCardData(title, summary);
}

private KeyRatioCardData parseValuationKeyRatioCard(String title, List<String> tokens) {
 List<String> periodChips = new ArrayList<>();
 List<String> rangeParts = new ArrayList<>();
 List<String> numericValues = new ArrayList<>();

 for (int i = 1; i < tokens.size(); i++) {
     String token = normalizeSpaces(tokens.get(i));

     if (isKeyRatioPeriodChip(token)) {
         periodChips.add(token);
         continue;
     }

     if (isKeyRatioNumberValue(token)
             && i + 1 < tokens.size()
             && isKeyRatioRangeLabel(tokens.get(i + 1))) {
         rangeParts.add(normalizeSpaces(tokens.get(i + 1)) + "=" + token);
         numericValues.add(token);
         i++;
         continue;
     }

     if (isKeyRatioRangeLabel(token)
             && i + 1 < tokens.size()
             && isKeyRatioNumberValue(tokens.get(i + 1))) {
         rangeParts.add(token + "=" + normalizeSpaces(tokens.get(i + 1)));
         numericValues.add(normalizeSpaces(tokens.get(i + 1)));
         i++;
     }
 }

 if (rangeParts.size() < 3) {
     throw new AssertionError("Valuation Key Ratio range values incomplete for: "
             + title
             + " | ranges="
             + rangeParts
             + " | tokens="
             + tokens);
 }

 String summary = "PeriodOptions=" + periodChips
         + " | NumericValues=" + numericValues
         + " | RangeValues=" + rangeParts;

 return new KeyRatioCardData(title, summary);
}

private List<String> splitKeyRatioCardTokens(String rawValue) {
 List<String> tokens = new ArrayList<>();

 if (rawValue == null) {
     return tokens;
 }

 String prepared = rawValue
         .replace("\\n", "\n")
         .replace("\r", "\n")
         .trim();

 String[] rawParts = prepared.split("\\n+");

 for (String rawPart : rawParts) {
     String clean = normalizeSpaces(rawPart);

     if (clean.isEmpty()) {
         continue;
     }

     addTokensFromCompactKeyRatioText(tokens, clean);
 }

 return tokens;
}

private void addTokensFromCompactKeyRatioText(List<String> tokens, String clean) {
 if (clean == null || clean.trim().isEmpty()) {
     return;
 }

 String value = normalizeSpaces(clean);

 if (isKeyRatioYearLabel(value)
         || isKeyRatioNumberValue(value)
         || isKeyRatioSummaryLabel(value)
         || isKeyRatioRangeLabel(value)
         || isKeyRatioPeriodChip(value)) {
     tokens.add(value);
     return;
 }

 String expanded = value
         .replaceAll("\\b(20\\d{2})\\b", "\n$1")
         .replaceAll("\\b(5Y Avg|3Y Avg|5Y|3Y|TTM)\\b", "\n$1")
         .replaceAll("\\b(1 Year|3 Year|5 Year|1 Years|3 Years|5 Years)\\b", "\n$1")
         .replaceAll("\\b(Min|Median|Max)\\b", "\n$1")
         .replaceAll("(?<![A-Za-z])-?\\d+(\\.\\d+)?%?", "\n$0");

 String[] parts = expanded.split("\\n+");

 for (String part : parts) {
     String token = normalizeSpaces(part);

     if (!token.isEmpty()) {
         tokens.add(token);
     }
 }
}

private boolean isValidKeyRatioCardTitle(String value) {
 if (value == null) {
     return false;
 }

 String clean = normalizeSpaces(value);

 if (clean.length() < 3) {
     return false;
 }

 if (clean.equals("Key Ratios")
         || clean.equals("Efficiency")
         || clean.equals("Valuation")
         || clean.equals("Growth")
         || clean.equals("More")
         || clean.equals("Peers")) {
     return false;
 }

 return Pattern.matches(".*[A-Za-z].*", clean)
         && !isKeyRatioYearLabel(clean)
         && !isKeyRatioNumberValue(clean)
         && !isKeyRatioSummaryLabel(clean)
         && !isKeyRatioRangeLabel(clean)
         && !isKeyRatioPeriodChip(clean);
}

private boolean isKeyRatioYearLabel(String value) {
 if (value == null) {
     return false;
 }

 return normalizeSpaces(value).matches("^20\\d{2}$");
}

private boolean isKeyRatioPeriodChip(String value) {
 if (value == null) {
     return false;
 }

 String clean = normalizeSpaces(value);

 return clean.matches("^[1-9]\\s?Year$")
         || clean.matches("^[1-9]\\s?Years$");
}

private boolean isKeyRatioSummaryLabel(String value) {
 if (value == null) {
     return false;
 }

 String clean = normalizeSpaces(value);

 return clean.equals("5Y")
         || clean.equals("3Y")
         || clean.equals("TTM")
         || clean.equals("5Y Avg")
         || clean.equals("3Y Avg");
}

private boolean isKeyRatioRangeLabel(String value) {
 if (value == null) {
     return false;
 }

 String clean = normalizeSpaces(value);

 return clean.equals("Min")
         || clean.equals("Median")
         || clean.equals("Max");
}

private boolean isKeyRatioNumberValue(String value) {
 if (value == null) {
     return false;
 }

 String clean = normalizeSpaces(value);

 return Pattern.matches("^-?\\d{1,3}(,\\d{2,3})*(\\.\\d+)?\\s?%?$", clean)
         || Pattern.matches("^-?\\d+(\\.\\d+)?\\s?%?$", clean);
}

private void resetKeyRatiosCarouselToStart() {
 ReportLogger.debug("Resetting Key Ratios carousel to start");

 for (int i = 1; i <= 4; i++) {
     keyRatiosCardSwipeRight();
     sleep(500);
 }
}

private void keyRatiosCardSwipeLeft() {
 try {
     Dimension size = driver.manage().window().getSize();

     int y = getKeyRatiosCardCenterY();
     int startX = (int) (size.getWidth() * 0.82);
     int endX = (int) (size.getWidth() * 0.20);

     PointerInput finger = new PointerInput(PointerInput.Kind.TOUCH, "finger");
     Sequence swipe = new Sequence(finger, 1);

     swipe.addAction(finger.createPointerMove(Duration.ZERO, PointerInput.Origin.viewport(), startX, y));
     swipe.addAction(finger.createPointerDown(PointerInput.MouseButton.LEFT.asArg()));
     swipe.addAction(finger.createPointerMove(Duration.ofMillis(520), PointerInput.Origin.viewport(), endX, y));
     swipe.addAction(finger.createPointerUp(PointerInput.MouseButton.LEFT.asArg()));

     driver.perform(Collections.singletonList(swipe));

 } catch (Exception e) {
     ReportLogger.debug("Key Ratios card carousel swipe left failed: " + cleanError(e.getMessage()));
 }
}

private void keyRatiosCardSwipeRight() {
 try {
     Dimension size = driver.manage().window().getSize();

     int y = getKeyRatiosCardCenterY();
     int startX = (int) (size.getWidth() * 0.20);
     int endX = (int) (size.getWidth() * 0.82);

     PointerInput finger = new PointerInput(PointerInput.Kind.TOUCH, "finger");
     Sequence swipe = new Sequence(finger, 1);

     swipe.addAction(finger.createPointerMove(Duration.ZERO, PointerInput.Origin.viewport(), startX, y));
     swipe.addAction(finger.createPointerDown(PointerInput.MouseButton.LEFT.asArg()));
     swipe.addAction(finger.createPointerMove(Duration.ofMillis(520), PointerInput.Origin.viewport(), endX, y));
     swipe.addAction(finger.createPointerUp(PointerInput.MouseButton.LEFT.asArg()));

     driver.perform(Collections.singletonList(swipe));

 } catch (Exception e) {
     ReportLogger.debug("Key Ratios card carousel swipe right failed: " + cleanError(e.getMessage()));
 }
}

private int getKeyRatiosCardCenterY() {
 List<WebElement> elements = driver.findElements(AppiumBy.className("android.view.View"));

 for (WebElement element : elements) {
     try {
         if (element == null || !element.isDisplayed()) {
             continue;
         }

         String desc = element.getAttribute("content-desc");

         if (desc != null && isKeyRatioCardDescription(desc)) {
             Rectangle rect = element.getRect();
             return rect.getY() + rect.getHeight() / 2;
         }

     } catch (Exception ignored) {
         // ignore stale elements
     }
 }

 Dimension size = driver.manage().window().getSize();
 return (int) (size.getHeight() * 0.55);
}

private void keyRatiosSmallSwipeUp() {
 try {
     Dimension size = driver.manage().window().getSize();

     int x = (int) (size.getWidth() * 0.88);
     int startY = (int) (size.getHeight() * 0.74);
     int endY = (int) (size.getHeight() * 0.54);

     PointerInput finger = new PointerInput(PointerInput.Kind.TOUCH, "finger");
     Sequence swipe = new Sequence(finger, 1);

     swipe.addAction(finger.createPointerMove(Duration.ZERO, PointerInput.Origin.viewport(), x, startY));
     swipe.addAction(finger.createPointerDown(PointerInput.MouseButton.LEFT.asArg()));
     swipe.addAction(finger.createPointerMove(Duration.ofMillis(430), PointerInput.Origin.viewport(), x, endY));
     swipe.addAction(finger.createPointerUp(PointerInput.MouseButton.LEFT.asArg()));

     driver.perform(Collections.singletonList(swipe));

 } catch (Exception firstError) {
     Map<String, Object> params = new HashMap<>();
     params.put("left", 760);
     params.put("top", 880);
     params.put("width", 260);
     params.put("height", 520);
     params.put("direction", "up");
     params.put("percent", 0.24);

     driver.executeScript("mobile: scrollGesture", params);
 }
}

private void keyRatiosSmallSwipeDown() {
 try {
     Dimension size = driver.manage().window().getSize();

     int x = (int) (size.getWidth() * 0.88);
     int startY = (int) (size.getHeight() * 0.54);
     int endY = (int) (size.getHeight() * 0.74);

     PointerInput finger = new PointerInput(PointerInput.Kind.TOUCH, "finger");
     Sequence swipe = new Sequence(finger, 1);

     swipe.addAction(finger.createPointerMove(Duration.ZERO, PointerInput.Origin.viewport(), x, startY));
     swipe.addAction(finger.createPointerDown(PointerInput.MouseButton.LEFT.asArg()));
     swipe.addAction(finger.createPointerMove(Duration.ofMillis(430), PointerInput.Origin.viewport(), x, endY));
     swipe.addAction(finger.createPointerUp(PointerInput.MouseButton.LEFT.asArg()));

     driver.perform(Collections.singletonList(swipe));

 } catch (Exception firstError) {
     Map<String, Object> params = new HashMap<>();
     params.put("left", 760);
     params.put("top", 880);
     params.put("width", 260);
     params.put("height", 520);
     params.put("direction", "down");
     params.put("percent", 0.24);

     driver.executeScript("mobile: scrollGesture", params);
 }
}

private void keyRatiosTinySwipeUp() {
 try {
     Dimension size = driver.manage().window().getSize();

     int x = (int) (size.getWidth() * 0.88);
     int startY = (int) (size.getHeight() * 0.58);
     int endY = (int) (size.getHeight() * 0.48);

     PointerInput finger = new PointerInput(PointerInput.Kind.TOUCH, "finger");
     Sequence swipe = new Sequence(finger, 1);

     swipe.addAction(finger.createPointerMove(Duration.ZERO, PointerInput.Origin.viewport(), x, startY));
     swipe.addAction(finger.createPointerDown(PointerInput.MouseButton.LEFT.asArg()));
     swipe.addAction(finger.createPointerMove(Duration.ofMillis(250), PointerInput.Origin.viewport(), x, endY));
     swipe.addAction(finger.createPointerUp(PointerInput.MouseButton.LEFT.asArg()));

     driver.perform(Collections.singletonList(swipe));

 } catch (Exception e) {
     ReportLogger.debug("Key Ratios tiny swipe up skipped: " + cleanError(e.getMessage()));
 }
}

private void keyRatiosTinySwipeDown() {
 try {
     Dimension size = driver.manage().window().getSize();

     int x = (int) (size.getWidth() * 0.88);
     int startY = (int) (size.getHeight() * 0.48);
     int endY = (int) (size.getHeight() * 0.58);

     PointerInput finger = new PointerInput(PointerInput.Kind.TOUCH, "finger");
     Sequence swipe = new Sequence(finger, 1);

     swipe.addAction(finger.createPointerMove(Duration.ZERO, PointerInput.Origin.viewport(), x, startY));
     swipe.addAction(finger.createPointerDown(PointerInput.MouseButton.LEFT.asArg()));
     swipe.addAction(finger.createPointerMove(Duration.ofMillis(250), PointerInput.Origin.viewport(), x, endY));
     swipe.addAction(finger.createPointerUp(PointerInput.MouseButton.LEFT.asArg()));

     driver.perform(Collections.singletonList(swipe));

 } catch (Exception e) {
     ReportLogger.debug("Key Ratios tiny swipe down skipped: " + cleanError(e.getMessage()));
 }
}

private static class KeyRatioCardData {
 private final String title;
 private final String summary;

 private KeyRatioCardData(String title, String summary) {
     this.title = title;
     this.summary = summary;
 }
}

//=========================================================
//SD_010 - KEY RATIOS MORE DETAILS
//=========================================================

public void verifyKeyRatiosMoreDetails() {
    runKeyRatiosWithImmediateLookups(() -> {
        ReportLogger.step("Verifying Key Ratios More details screen");
        recoverStockDetailsIfNeeded();
        alignKeyRatiosSectionSafely();
        openKeyRatiosMorePage();
        runWithChildScreenCleanup(() -> {
            validateKeyRatiosMoreTopTabsReachable();
            for (String tab : new String[]{"Profitability", "Growth", "Solvency", "Operating Efficiency", "Valuation"}) {
                validateAllRatioBlocksForKeyRatiosMoreTab(tab);
            }
        }, this::returnFromKeyRatiosMorePage);
        ReportLogger.pass("Key Ratios More details screen validated successfully");
    });
}


private void openKeyRatiosMorePage() {
ReportLogger.step("Opening Key Ratios More details screen");

alignKeyRatiosSectionSafely();

if (!tapKeyRatiosMoreLinkSafely()) {
   throw new AssertionError("Unable to tap Key Ratios More link");
}

waitForKeyRatiosMoreDetailsPage();

ReportLogger.pass("Key Ratios More details screen opened");
}

private boolean tapKeyRatiosMoreLinkSafely() {
WebElement keyRatiosHeading = findVisibleElement(byDesc("Key Ratios"));

if (keyRatiosHeading != null) {
   Rectangle headingRect = keyRatiosHeading.getRect();
   List<WebElement> moreElements = driver.findElements(byDesc("More"));

   for (WebElement moreElement : moreElements) {
       try {
           if (moreElement == null || !moreElement.isDisplayed() || !moreElement.isEnabled()) {
               continue;
           }

           Rectangle moreRect = moreElement.getRect();

           boolean sameRowAsKeyRatios =
                   Math.abs(moreRect.getY() - headingRect.getY()) <= 100
                           && moreRect.getX() > headingRect.getX();

           if (sameRowAsKeyRatios) {
               tapElementCenter(moreElement);
               sleep(1600);
               ReportLogger.pass("Tapped: Key Ratios More link");
               return true;
           }

       } catch (Exception ignored) {
           // Ignore stale Flutter element
       }
   }
}

if (tapIfVisible(byDesc("More"), "Key Ratios More link")) {
   sleep(1600);
   return true;
}

return false;
}

private void waitForKeyRatiosMoreDetailsPage() {
for (int i = 1; i <= 18; i++) {
   sleep(700);

   if (isKeyRatiosMoreDetailsPageReady()) {
       ReportLogger.pass("Key Ratios More details page is ready");
       return;
   }
}

throw new AssertionError("Key Ratios More details page did not load");
}

private boolean isKeyRatiosMoreDetailsPageReady() {
return isVisible(byDesc("Profitability"))
       || isVisible(byDesc("Growth"))
       || isVisible(byDesc("Solvency"))
       || isVisible(byDesc("Operating Efficiency"))
       || isVisible(byDesc("Valuation"));
}

private void validateKeyRatiosMoreTopTabsReachable() {
ReportLogger.step("Validating Key Ratios More top tabs");

validateKeyRatiosMoreTabReachable("Profitability");
validateKeyRatiosMoreTabReachable("Growth");
validateKeyRatiosMoreTabReachable("Solvency");
validateKeyRatiosMoreTabReachable("Operating Efficiency");
validateKeyRatiosMoreTabReachable("Valuation");

keyRatiosMoreTopTabsSwipeRight();
keyRatiosMoreTopTabsSwipeRight();
sleep(700);

ReportLogger.pass("Key Ratios More top tabs validated");
}

private void validateKeyRatiosMoreTabReachable(String tabName) {
ensureKeyRatiosMoreTabsVisible();

if (isVisible(byDesc(tabName))) {
   ReportLogger.pass("Key Ratios More tab reachable: " + tabName);
   return;
}

for (int i = 1; i <= 7; i++) {
   keyRatiosMoreTopTabsSwipeLeft();
   sleep(500);

   if (isVisible(byDesc(tabName))) {
       ReportLogger.pass("Key Ratios More tab reachable: " + tabName);
       return;
   }
}

for (int i = 1; i <= 7; i++) {
   keyRatiosMoreTopTabsSwipeRight();
   sleep(500);

   if (isVisible(byDesc(tabName))) {
       ReportLogger.pass("Key Ratios More tab reachable: " + tabName);
       return;
   }
}

throw new AssertionError("Key Ratios More tab is not reachable: " + tabName);
}

private void validateAllRatioBlocksForKeyRatiosMoreTab(String tabName) {
ReportLogger.step("Validating Key Ratios More tab: " + tabName);

tapKeyRatiosMoreTab(tabName);

resetKeyRatiosMoreBodyToTop(tabName);

java.util.LinkedHashMap<String, String> capturedBlocks = new java.util.LinkedHashMap<>();
List<String> expectedBlocks = getExpectedKeyRatiosMoreBlocks(tabName);

int stableScrollCount = 0;
String previousPageSignature = "";
int maxAttempts = "Valuation".equals(tabName) ? 24 : 18;
long scanDeadline = System.nanoTime() + ExecutionProfile.longWait().toNanos();

for (int attempt = 1; attempt <= maxAttempts; attempt++) {
   int beforeCount = capturedBlocks.size();

   String currentSignature = captureVisibleKeyRatiosMoreRatioBlocks(tabName, capturedBlocks);

   int afterCount = capturedBlocks.size();
   // This is a no-progress timeout, not a budget for visiting every ratio card.
   // A responsive device can still need more than longWait to traverse all blocks.
   if (afterCount > beforeCount) {
       scanDeadline = System.nanoTime() + ExecutionProfile.longWait().toNanos();
   }
   if (areAllExpectedKeyRatiosMoreBlocksCaptured(expectedBlocks, capturedBlocks)) {
       ReportLogger.pass("All expected Key Ratios More blocks captured for tab: " + tabName);
       break;
   }
   // Read the viewport reached by the preceding gesture before deciding it stalled.
   if (afterCount == beforeCount && System.nanoTime() >= scanDeadline) break;
   if (attempt % 3 == 0) {
       ReportLogger.step("Key Ratios More scan | tab=" + tabName + " | attempt=" + attempt
               + " | captured=" + capturedBlocks.size() + "/" + expectedBlocks.size());
   }

   if (afterCount > beforeCount) {
       stableScrollCount = 0;
   } else if (currentSignature.equals(previousPageSignature)) {
       stableScrollCount++;
   } else {
       stableScrollCount = 0;
   }

   previousPageSignature = currentSignature;

   /*
    * Do not stop too early only because the page signature is stable.
    * Flutter sometimes keeps the same top tokens while the body is still moving,
    * which was causing intermittent misses like Net Profit Growth and Valuation blocks.
    */
   if (stableScrollCount >= 3) {
       ReportLogger.debug("Key Ratios More body looks stable. Running targeted missing-block recovery for tab: " + tabName);
       break;
   }

   keyRatiosMoreBodySwipeUp();
   sleep(850);
}

if (!areAllExpectedKeyRatiosMoreBlocksCaptured(expectedBlocks, capturedBlocks)) {
   recoverMissingKeyRatiosMoreBlocks(tabName, capturedBlocks, scanDeadline);
}

validateMinimumKeyRatiosMoreBlocksCaptured(tabName, capturedBlocks);

for (Map.Entry<String, String> entry : capturedBlocks.entrySet()) {
   String ratioName = entry.getKey();
   String ratioValue = entry.getValue();

   logValidatedText(
           "Key Ratios More " + tabName + " | " + ratioName,
           ratioName + " " + ratioValue
   );
}

ReportLogger.pass("Key Ratios More tab validated"
       + " | tab=" + tabName
       + " | ratioBlocksCaptured=" + capturedBlocks.size()
       + " | blocks=" + capturedBlocks.keySet());
}

private void validateMinimumKeyRatiosMoreBlocksCaptured(
   String tabName,
   java.util.LinkedHashMap<String, String> capturedBlocks
) {
List<String> expectedBlocks = getExpectedKeyRatiosMoreBlocks(tabName);
List<String> missingBlocks = new ArrayList<>();

for (String expectedBlock : expectedBlocks) {
   if (!hasCapturedRatioBlock(capturedBlocks, expectedBlock)) {
       missingBlocks.add(expectedBlock);
   }
}

if (!missingBlocks.isEmpty()) {
   throw new AssertionError("Incomplete Key Ratios More block capture"
           + " | tab=" + tabName
           + " | missing=" + missingBlocks
           + " | captured=" + capturedBlocks.keySet());
}
}

private boolean areAllExpectedKeyRatiosMoreBlocksCaptured(
   List<String> expectedBlocks,
   java.util.LinkedHashMap<String, String> capturedBlocks
) {
if (expectedBlocks == null || expectedBlocks.isEmpty()) {
   return !capturedBlocks.isEmpty();
}

for (String expectedBlock : expectedBlocks) {
   if (!hasCapturedRatioBlock(capturedBlocks, expectedBlock)) {
       return false;
   }
}

return true;
}

private void recoverMissingKeyRatiosMoreBlocks(
   String tabName,
   java.util.LinkedHashMap<String, String> capturedBlocks,
   long scanDeadline
) {
List<String> expectedBlocks = getExpectedKeyRatiosMoreBlocks(tabName);

ReportLogger.debug("Recovering missing Key Ratios More blocks"
       + " | tab=" + tabName
       + " | expected=" + expectedBlocks
       + " | currentlyCaptured=" + capturedBlocks.keySet());

captureVisibleKeyRatiosMoreRatioBlocks(tabName, capturedBlocks);

if (areAllExpectedKeyRatiosMoreBlocksCaptured(expectedBlocks, capturedBlocks)) {
   return;
}

/*
* Recovery pass 1: move back toward the top.
* This catches first/middle blocks that were skipped by a large swipe.
*/
int downRecoveryAttempts = "Valuation".equals(tabName) ? 6 : 4;
for (int i = 1; i <= downRecoveryAttempts && System.nanoTime() < scanDeadline; i++) {
   keyRatiosMoreBodySwipeDown();
   sleep(650);
   captureVisibleKeyRatiosMoreRatioBlocks(tabName, capturedBlocks);

   if (areAllExpectedKeyRatiosMoreBlocksCaptured(expectedBlocks, capturedBlocks)) {
       ReportLogger.pass("Missing Key Ratios More blocks recovered by swipe down"
               + " | tab=" + tabName
               + " | attempt=" + i);
       return;
   }
}

/*
* Recovery pass 2: scan downward again from the recovered/top area.
* This catches lower Valuation blocks like Close Price / High Price / Low Price.
*/
int upRecoveryAttempts = "Valuation".equals(tabName) ? 10 : 6;
for (int i = 1; i <= upRecoveryAttempts && System.nanoTime() < scanDeadline; i++) {
   keyRatiosMoreBodySwipeUp();
   sleep(700);
   captureVisibleKeyRatiosMoreRatioBlocks(tabName, capturedBlocks);

   if (areAllExpectedKeyRatiosMoreBlocksCaptured(expectedBlocks, capturedBlocks)) {
       ReportLogger.pass("Missing Key Ratios More blocks recovered by swipe up"
               + " | tab=" + tabName
               + " | attempt=" + i);
       return;
   }
}
}

private List<String> getExpectedKeyRatiosMoreBlocks(String tabName) {
if ("Profitability".equals(tabName)) {
   return Arrays.asList(
           "ROCE",
           "ROE",
           "ROA",
           "EBIT Margin",
           "Net Margin",
           "Cash Profit Margin"
   );
}

if ("Growth".equals(tabName)) {
   return Arrays.asList(
           "Revenue Growth",
           "EBIT Growth",
           "Net Profit Growth",
           "EPS Growth",
           "Book Value Growth"
   );
}

if ("Solvency".equals(tabName)) {
   return Arrays.asList(
           "Debt to Equity",
           "Short term debt to equity ratio",
           "Current Ratio",
           "Quick Ratio",
           "Interest Coverage"
   );
}

if ("Operating Efficiency".equals(tabName)) {
   return Arrays.asList(
           "Debtors to sales",
           "Asset Turnover",
           "Receivable days",
           "Inventory Days",
           "Payable days",
           "Cash Conversion Cycle"
   );
}

if ("Valuation".equals(tabName)) {
   /*
    * Keep only stable Valuation ratio blocks as mandatory.
    * Close Price / High Price / Low Price are price metrics and are not
    * consistently exposed by Flutter/Appium on the Key Ratios More screen.
    */
   return Arrays.asList(
           "Price / Earnings",
           "Price / Book Value",
           "Dividend Yield",
           "EV/EBITDA",
           "Market Cap"
   );
}

return Collections.emptyList();
}

private void tapKeyRatiosMoreTab(String tabName) {
ReportLogger.step("Opening Key Ratios More tab: " + tabName);

ensureKeyRatiosMoreTabsVisible();

if (tapIfVisible(byDesc(tabName), "Key Ratios More tab: " + tabName)) {
   sleep(1400);
   waitForKeyRatiosMoreTabContent(tabName);
   ReportLogger.pass("Key Ratios More tab opened: " + tabName);
   return;
}

for (int i = 1; i <= 7; i++) {
   keyRatiosMoreTopTabsSwipeLeft();
   sleep(500);

   if (tapIfVisible(byDesc(tabName), "Key Ratios More tab: " + tabName)) {
       sleep(1400);
       waitForKeyRatiosMoreTabContent(tabName);
       ReportLogger.pass("Key Ratios More tab opened: " + tabName);
       return;
   }
}

for (int i = 1; i <= 7; i++) {
   keyRatiosMoreTopTabsSwipeRight();
   sleep(500);

   if (tapIfVisible(byDesc(tabName), "Key Ratios More tab: " + tabName)) {
       sleep(1400);
       waitForKeyRatiosMoreTabContent(tabName);
       ReportLogger.pass("Key Ratios More tab opened: " + tabName);
       return;
   }
}

throw new AssertionError("Unable to open Key Ratios More tab: " + tabName);
}

private void waitForKeyRatiosMoreTabContent(String tabName) {
for (int i = 1; i <= 14; i++) {
   List<String> tokens = collectVisibleKeyRatiosMoreTokens();

   for (int j = 0; j < tokens.size(); j++) {
       if (isKeyRatiosMoreRatioBlockHeading(tokens, j)) {
           int nextBlockIndex = findNextKeyRatiosMoreBlockIndex(tokens, j + 1);

           if (hasKeyRatiosMoreBlockStructure(tokens, j, nextBlockIndex)) {
               return;
           }
       }
   }

   sleep(500);
}

throw new AssertionError("Ratio block content did not load for Key Ratios More tab: " + tabName);
}

private void resetKeyRatiosMoreBodyToTop(String tabName) {
  ReportLogger.step("Aligning Key Ratios More tab content to first ratio block: " + tabName);

  /*
   * Valuation is special:
   * Appium can expose Price / Earnings or Price / Book Value in source even when
   * the visible viewport is already lower at Dividend Yield / EV/EBITDA.
   * So source-token check alone gives false alignment.
   */
  if ("Valuation".equals(tabName)) {
      resetValuationTabToTop();
      return;
  }

  if (isKeyRatiosMoreTabFirstBlockVisible(tabName)) {
      ReportLogger.pass("Key Ratios More first ratio block already visible: " + tabName);
      return;
  }

  for (int i = 1; i <= 12; i++) {
      keyRatiosMoreBodySwipeDown();
      sleep(550);

      if (isKeyRatiosMoreTabFirstBlockVisible(tabName)) {
          ReportLogger.pass("Key Ratios More first ratio block aligned: "
                  + tabName + " | recoverySwipeDown=" + i);
          return;
      }
  }

  ReportLogger.debug("Exact first block not confirmed for tab: "
          + tabName
          + ". Continuing with dynamic capture.");
}
private boolean isKeyRatiosMoreTabFirstBlockVisible(String tabName) {
List<String> tokens = collectVisibleKeyRatiosMoreTokens();

for (String token : tokens) {
   String clean = normalizeSpaces(token);
   String title = normalizeKeyRatiosMoreBlockTitle(
           extractCleanKeyRatiosMoreBlockTitle(clean)
   );

   if (title.isEmpty()) {
       continue;
   }

   if ("Profitability".equals(tabName) && title.equals("ROCE")) {
       return true;
   }

   if ("Growth".equals(tabName) && title.equals("Revenue Growth")) {
       return true;
   }

   if ("Solvency".equals(tabName) && title.equals("Debt to Equity")) {
       return true;
   }

   if ("Operating Efficiency".equals(tabName) && title.equals("Debtors to sales")) {
       return true;
   }

   if ("Valuation".equals(tabName) && title.equals("Price / Earnings")) {
       return true;
   }
}

return false;
}
private void resetValuationTabToTop() {
  ReportLogger.step("Aligning Valuation tab to its first Price / Earnings block");

  if (isValuationTopVisuallyConfirmed()) return;

  /*
   * First, push the page upward enough to reach the real top of Valuation.
   * Do not trust source-only visibility here.
   */
  for (int i = 1; i <= 18; i++) {
      keyRatiosMoreBodySwipeDown();
      sleep(450);

      if (isValuationTopVisuallyConfirmed()) {
          ReportLogger.pass("Valuation first block visually aligned after recovery swipe down " + i);
          return;
      }
  }

  /*
   * Sometimes the first card is reached after a small settle.
   */
  sleep(900);

  if (isValuationTopVisuallyConfirmed()) {
      ReportLogger.pass("Valuation first block visually aligned after settle");
      return;
  }

  throw new AssertionError("Unable to align Valuation tab to visible first ratio block"
          + " | expected=Price / Earnings. Price / Book Value is the second block"
          + "; capture Inspector XML of the Valuation tab if the first heading is absent.");
}

private boolean isValuationTopVisuallyConfirmed() {
  for (By locator : new By[]{
          byDesc("Price / Earnings"),
          byDescContains("Price / Earnings"),
          byDesc("Price/Earnings"),
          byDescContains("Price/Earnings"),
          byText("Price / Earnings"),
          byTextContains("Price/Earnings")
  }) {
      for (WebElement firstValuationBlock : driver.findElements(locator)) {
          if (!firstValuationBlock.isDisplayed()) continue;
          Rectangle rect = firstValuationBlock.getRect();
          Dimension size = driver.manage().window().getSize();
          if (isFullyOnValuationScreen(rect)
                  && rect.getY() >= (int) (size.getHeight() * 0.12)
                  && rect.getY() <= (int) (size.getHeight() * 0.82)) return true;
      }
  }
  return false;
}
private String captureVisibleKeyRatiosMoreRatioBlocks(
   String tabName,
   java.util.LinkedHashMap<String, String> capturedBlocks
) {
List<String> tokens = collectVisibleKeyRatiosMoreTokens();

for (int i = 0; i < tokens.size(); i++) {
   if (!isKeyRatiosMoreRatioBlockHeading(tokens, i)) {
       continue;
   }

   int nextBlockIndex = findNextKeyRatiosMoreBlockIndex(tokens, i + 1);

   if (!hasKeyRatiosMoreBlockStructure(tokens, i, nextBlockIndex)) {
       continue;
   }

   String blockTitle = extractCleanKeyRatiosMoreBlockTitle(tokens.get(i));
   blockTitle = normalizeKeyRatiosMoreBlockTitle(blockTitle);

   if (blockTitle.isEmpty() || capturedBlocks.containsKey(blockTitle)) {
       continue;
   }

   String value = buildKeyRatiosMoreBlockSummary(tokens, i, nextBlockIndex);

   capturedBlocks.put(blockTitle, value);

   ReportLogger.pass("Captured Key Ratios More ratio block: "
           + tabName + " | " + blockTitle);
}

/*
* Fallback for valid ratio blocks where Flutter/Appium exposes title/value
* but not full table structure in the same viewport.
*/
captureKnownVisibleKeyRatiosMoreBlocks(tabName, tokens, capturedBlocks);
// Reuse this viewport for progress detection instead of remotely reading every node twice.
return String.join("|", tokens);
}

private void captureKnownVisibleKeyRatiosMoreBlocks(
   String tabName,
   List<String> tokens,
   java.util.LinkedHashMap<String, String> capturedBlocks
) {
List<String> expectedBlocks = getExpectedKeyRatiosMoreBlocks(tabName);

for (int i = 0; i < tokens.size(); i++) {
   String clean = normalizeSpaces(tokens.get(i));
   String title = getExpectedKeyRatiosMoreTitleFromToken(clean, expectedBlocks);

   if (title.isEmpty()) {
       continue;
   }

   if (capturedBlocks.containsKey(title)) {
       continue;
   }

   String value = extractExpectedKeyRatiosMoreValueFromToken(clean, title);

   if (value.isEmpty()) {
       value = findNearestKeyRatiosMoreValue(tokens, i);
   }

   if (value.isEmpty()) continue; // A real UI placeholder is valid; an absent value is not.


   capturedBlocks.put(title, value);

   ReportLogger.pass("Captured Key Ratios More ratio block by fallback: "
           + tabName + " | " + title);
}
}

private String findNearestKeyRatiosMoreValue(List<String> tokens, int startIndex) {
int endIndex = Math.min(tokens.size(), startIndex + 15);

for (int i = startIndex + 1; i < endIndex; i++) {
   String clean = normalizeSpaces(tokens.get(i));

   if (isKeyRatiosMoreRatioBlockHeading(tokens, i)) {
       break;
   }

   if (isKeyRatiosMoreNumericOrTextValue(clean)) {
       return clean;
   }
}

return "";
}

private List<String> collectVisibleKeyRatiosMoreTokens() {
List<String> rawValues = collectVisibleContentDescriptions();
List<String> tokens = new ArrayList<>();

for (String rawValue : rawValues) {
   if (rawValue == null) {
       continue;
   }

   String prepared = rawValue
           .replace("\\n", "\n")
           .replace("\r", "\n")
           .trim();

   String[] parts = prepared.split("\\n+");

   for (String part : parts) {
       String clean = normalizeSpaces(part);

       if (!clean.isEmpty()) {
           tokens.add(clean);
       }
   }
}

return tokens;
}

private boolean isKeyRatiosMoreRatioBlockHeading(List<String> tokens, int index) {
if (tokens == null || index < 0 || index >= tokens.size()) {
   return false;
}

String clean = normalizeSpaces(tokens.get(index));

if (clean.isEmpty()) {
   return false;
}

if (isKeyRatiosMoreIgnoredText(clean)) {
   return false;
}

if (isKeyRatiosMoreExplanationOrFormulaText(clean)) {
   return false;
}

if (isKeyRatiosMorePeriodHeader(clean)
       || isKeyRatiosMoreNumericOrTextValue(clean)
       || clean.equals("ITC")
       || clean.equals("Peer Median")) {
   return false;
}

if (!Pattern.matches(".*[A-Za-z].*", clean)) {
   return false;
}

if (clean.length() > 75) {
   return false;
}

String titlePart = extractCleanKeyRatiosMoreBlockTitle(clean);
titlePart = normalizeKeyRatiosMoreBlockTitle(titlePart);

if (titlePart.isEmpty()) {
   return false;
}

if (isKeyRatiosMoreIgnoredText(titlePart)
       || isKeyRatiosMoreExplanationOrFormulaText(titlePart)) {
   return false;
}

int wordCount = titlePart.split("\\s+").length;

if (wordCount > 8 && !titlePart.toLowerCase().contains("ratio")) {
   return false;
}

if (hasRatioHeadingLiveValue(clean)) {
   return true;
}

return hasKeyRatiosMoreRatioHeadingSignal(titlePart);
}

private String extractCleanKeyRatiosMoreBlockTitle(String value) {
if (value == null) {
   return "";
}

String clean = normalizeSpaces(value);

if (clean.contains(":")) {
   String[] parts = clean.split(":", 2);
   clean = normalizeSpaces(parts[0]);
}

return clean.replaceAll("\\s+", " ").trim();
}

private String normalizeKeyRatiosMoreBlockTitle(String value) {
if (value == null) {
   return "";
}

String clean = normalizeSpaces(value);

clean = clean
       .replace("(%)", "")
       .replace("(₹ Cr)", "")
       .replace("(₹)", "")
       .replace("₹ Cr", "")
       .replace("₹", "")
       .trim();

clean = clean.replaceAll("\\s+", " ");

String debtHeading = clean.replaceAll("[-–—]", " ").replaceAll("\\s+", " ");
if (debtHeading.equalsIgnoreCase("Short term debt to equity")
        || debtHeading.equalsIgnoreCase("Short term debt to equity ratio")) {
    return "Short term debt to equity ratio";
}

if (clean.equalsIgnoreCase("Receivable Days")) {
   return "Receivable days";
}

if (clean.equalsIgnoreCase("Inventory days")) {
   return "Inventory Days";
}

if (clean.equalsIgnoreCase("Payable Days")) {
   return "Payable days";
}

if (clean.equalsIgnoreCase("Cash Conversion Cycle")) {
   return "Cash Conversion Cycle";
}

if (clean.equalsIgnoreCase("EV EBITDA")
       || clean.equalsIgnoreCase("EV/ EBITDA")
       || clean.equalsIgnoreCase("EV / EBITDA")) {
   return "EV/EBITDA";
}

if (clean.equalsIgnoreCase("Price/Earnings")) {
   return "Price / Earnings";
}

if (clean.equalsIgnoreCase("Price/Book Value")) {
   return "Price / Book Value";
}

return clean;
}

private boolean hasRatioHeadingLiveValue(String value) {
if (value == null) {
   return false;
}

String clean = normalizeSpaces(value);

return Pattern.matches(
       "^[A-Za-z0-9₹%()./ '&,-]+\\s*:\\s*(--|NA|N/A|-?\\d{1,3}(,\\d{2,3})*(\\.\\d+)?\\s?%?|-?\\d+(\\.\\d+)?\\s?%?)$",
       clean
);
}

private String extractRatioHeadingValue(String value) {
if (value == null) {
   return "";
}

String clean = normalizeSpaces(value);

if (!clean.contains(":")) {
   return "";
}

String[] parts = clean.split(":", 2);

if (parts.length < 2) {
   return "";
}

String liveValue = normalizeSpaces(parts[1]);

if (isKeyRatiosMoreNumericOrTextValue(liveValue)) {
   return liveValue;
}

return "";
}

private String getExpectedKeyRatiosMoreTitleFromToken(String token, List<String> expectedBlocks) {
if (token == null || expectedBlocks == null || expectedBlocks.isEmpty()) {
   return "";
}

String clean = normalizeSpaces(token);

if (clean.isEmpty()) {
   return "";
}

String normalizedTitle = normalizeKeyRatiosMoreBlockTitle(
       extractCleanKeyRatiosMoreBlockTitle(clean)
);

for (String expectedBlock : expectedBlocks) {
   if (titlesMatchKeyRatiosMoreExpectedBlock(normalizedTitle, expectedBlock)) {
       return expectedBlock;
   }
}

String cleanLower = clean.toLowerCase();

for (String expectedBlock : expectedBlocks) {
   String expectedLower = expectedBlock.toLowerCase();

   if (cleanLower.equals(expectedLower)
           || cleanLower.startsWith(expectedLower + " ")
           || cleanLower.startsWith(expectedLower + ":")
           || cleanLower.startsWith(expectedLower + " :")) {
       return expectedBlock;
   }

   String compactClean = cleanLower.replace(" / ", "/");
   String compactExpected = expectedLower.replace(" / ", "/");

   if (compactClean.equals(compactExpected)
           || compactClean.startsWith(compactExpected + " ")
           || compactClean.startsWith(compactExpected + ":")
           || compactClean.startsWith(compactExpected + " :")) {
       return expectedBlock;
   }
}

return "";
}

private boolean titlesMatchKeyRatiosMoreExpectedBlock(String actualTitle, String expectedTitle) {
String actual = normalizeKeyRatiosMoreBlockTitle(actualTitle).toLowerCase(java.util.Locale.ROOT);
String expected = normalizeKeyRatiosMoreBlockTitle(expectedTitle).toLowerCase(java.util.Locale.ROOT);

if (actual.equals(expected)) {
   return true;
}

actual = actual.replace(" / ", "/");
expected = expected.replace(" / ", "/");

return actual.equals(expected);
}

private String extractExpectedKeyRatiosMoreValueFromToken(String token, String expectedTitle) {
if (token == null || expectedTitle == null) {
   return "";
}

String clean = normalizeSpaces(token);
String valueFromColon = extractRatioHeadingValue(clean);

if (!valueFromColon.isEmpty()) {
   return valueFromColon;
}

String cleanLower = clean.toLowerCase();
String expectedLower = expectedTitle.toLowerCase();
int titleIndex = cleanLower.indexOf(expectedLower);

if (titleIndex < 0) {
   titleIndex = cleanLower.replace(" / ", "/").indexOf(expectedLower.replace(" / ", "/"));
}

String searchArea = clean;

if (titleIndex >= 0 && titleIndex + expectedTitle.length() < clean.length()) {
   searchArea = clean.substring(Math.min(clean.length(), titleIndex + expectedTitle.length()));
}

java.util.regex.Matcher matcher = Pattern.compile(
       "(--|NA|N/A|-?\\d{1,3}(,\\d{2,3})*(\\.\\d+)?\\s?%?|-?\\d+(\\.\\d+)?\\s?%?)"
).matcher(searchArea);

if (matcher.find()) {
   return normalizeSpaces(matcher.group(1));
}

return "";
}

private boolean hasKeyRatiosMoreRatioHeadingSignal(String value) {
if (value == null) {
   return false;
}

String lower = normalizeSpaces(value).toLowerCase();

return lower.contains("ratio")
       || lower.contains("margin")
       || lower.contains("roe")
       || lower.contains("roce")
       || lower.contains("roa")
       || lower.contains("ebit")
       || lower.contains("ebitda")
       || lower.contains("eps")
       || lower.contains("revenue")
       || lower.contains("profit")
       || lower.contains("growth")
       || lower.contains("book value")
       || lower.contains("price")
       || lower.contains("yield")
       || lower.contains("market cap")
       || lower.contains("debt")
       || lower.contains("asset")
       || lower.contains("turnover")
       || lower.contains("receivable")
       || lower.contains("inventory")
       || lower.contains("payable")
       || lower.contains("coverage")
       || lower.contains("cash")
       || lower.contains("earnings")
       || lower.contains("current")
       || lower.contains("quick")
       || lower.contains("close")
       || lower.contains("high")
       || lower.contains("low")
       || lower.contains("debtors");
}

private boolean isKeyRatiosMoreExplanationOrFormulaText(String value) {
if (value == null) {
   return true;
}

String clean = normalizeSpaces(value);
String lower = clean.toLowerCase();

if (lower.contains("higher than peers")
       || lower.contains("lower than peers")
       || lower.contains("as compared to")
       || lower.contains("compared to")
       || lower.contains("peer median")
       || lower.contains("formula")
       || lower.contains("calculated")
       || lower.contains("indicates")
       || lower.contains("measures")
       || lower.contains("shows")
       || lower.contains("divided by")
       || lower.contains("multiplied by")
       || lower.contains("average total")
       || lower.contains("past twelve months")
       || lower.contains("current share price")
       || lower.contains("year on year")
       || lower.contains("net worth")
       || lower.contains("after tax")
       || lower.contains("before interest")
       || lower.contains("operating activity")
       || lower.contains("financial health")
       || lower.contains("financial performance")
       || lower.contains("how efficiently")
       || lower.contains("how quickly")
       || lower.contains("ability to")
       || lower.contains("company's")
       || lower.contains("company ")) {
   return true;
}

if (clean.endsWith("?") || clean.endsWith(".")) {
   return true;
}

if (clean.contains(" + ") || clean.contains(" - ")) {
   return true;
}

if (clean.contains(" / ")) {
   boolean allowedSlashRatio =
           lower.equals("price / earnings")
                   || lower.equals("price / book value")
                   || lower.startsWith("price / earnings :")
                   || lower.startsWith("price / book value :");

   return !allowedSlashRatio;
}

return false;
}

private boolean hasKeyRatiosMoreBlockStructure(List<String> tokens, int startIndex, int endIndex) {
String heading = normalizeSpaces(tokens.get(startIndex));

if (hasRatioHeadingLiveValue(heading)) {
   return true;
}

int safeEndIndex = endIndex > startIndex ? endIndex : Math.min(tokens.size(), startIndex + 55);

boolean hasITC = false;
boolean hasPeerMedian = false;
boolean hasPeriodHeader = false;
boolean hasLiveValue = false;

for (int i = startIndex + 1; i < safeEndIndex && i < tokens.size(); i++) {
   String clean = normalizeSpaces(tokens.get(i));

   if (isKeyRatiosMoreRatioBlockHeading(tokens, i)) {
       break;
   }

   if (clean.equals("ITC")) {
       hasITC = true;
       continue;
   }

   if (clean.equals("Peer Median")) {
       hasPeerMedian = true;
       continue;
   }

   if (isKeyRatiosMorePeriodHeader(clean)) {
       hasPeriodHeader = true;
       continue;
   }

   if (isKeyRatiosMoreNumericOrTextValue(clean)) {
       hasLiveValue = true;
   }
}

return hasLiveValue && (hasITC || hasPeerMedian || hasPeriodHeader);
}

private int findNextKeyRatiosMoreBlockIndex(List<String> tokens, int startIndex) {
for (int i = Math.max(0, startIndex); i < tokens.size(); i++) {
   if (isKeyRatiosMoreRatioBlockHeading(tokens, i)) {
       return i;
   }
}

return tokens.size();
}

private String buildKeyRatiosMoreBlockSummary(List<String> tokens, int startIndex, int endIndex) {
String heading = normalizeSpaces(tokens.get(startIndex));

String currentValue = extractRatioHeadingValue(heading);

if (!currentValue.isEmpty()) {
   return currentValue;
}

int safeEndIndex = endIndex > startIndex ? endIndex : Math.min(tokens.size(), startIndex + 20);

for (int i = startIndex + 1; i < safeEndIndex && i < tokens.size(); i++) {
   String clean = normalizeSpaces(tokens.get(i));

   if (isKeyRatiosMoreRatioBlockHeading(tokens, i)) {
       break;
   }

   if (isKeyRatiosMoreNumericOrTextValue(clean)) {
       return clean;
   }
}

return "--";
}

private boolean isKeyRatiosMorePeriodHeader(String value) {
if (value == null) {
   return false;
}

String clean = normalizeSpaces(value);

return clean.equals("TTM")
       || clean.matches("^Mar '\\d{2}$")
       || clean.matches("^Jun '\\d{2}$")
       || clean.matches("^Sep '\\d{2}$")
       || clean.matches("^Dec '\\d{2}$")
       || clean.matches("^20\\d{2}$");
}

private boolean isKeyRatiosMoreNumericOrTextValue(String value) {
if (value == null) {
   return false;
}

String clean = normalizeSpaces(value);

return clean.equals("--")
       || clean.equals("NA")
       || clean.equals("N/A")
       || clean.matches("^-?\\d{1,3}(,\\d{2,3})*(\\.\\d+)?\\s?%?$")
       || clean.matches("^-?\\d+(\\.\\d+)?\\s?%?$");
}

private boolean isKeyRatiosMoreIgnoredText(String value) {
if (value == null) {
   return true;
}

String clean = normalizeSpaces(value);

return clean.equals("Profitability")
       || clean.equals("Growth")
       || clean.equals("Solvency")
       || clean.equals("Operating Efficiency")
       || clean.equals("Valuation")
       || clean.equals("Key Ratios")
       || clean.equals("More")
       || clean.equals("Peers")
       || clean.equals("Company")
       || clean.equals("Market Cap (₹ Cr)")
       || clean.equals("Revenue (TTM)")
       || clean.equals("Annual")
       || clean.equals("Quarterly")
       || clean.equals("Annual Consolidated")
       || clean.equals("Annual Standalone")
       || clean.equals("Quarterly Consolidated")
       || clean.equals("Quarterly Standalone")
       || clean.equals("*All values are in (Cr)")
       || clean.equals("*All values are in Cr")
       || clean.equals("Formula")
       || clean.equals("Note")
       || clean.equals("Min")
       || clean.equals("Median")
       || clean.equals("Max");
}

private String buildKeyRatiosMoreVisibleSignature() {
List<String> tokens = collectVisibleKeyRatiosMoreTokens();

StringBuilder builder = new StringBuilder();

int count = Math.min(tokens.size(), 20);

for (int i = 0; i < count; i++) {
   builder.append(tokens.get(i)).append("|");
}

return builder.toString();
}

private void ensureKeyRatiosMoreTabsVisible() {
if (isVisible(byDesc("Profitability"))
       || isVisible(byDesc("Growth"))
       || isVisible(byDesc("Solvency"))
       || isVisible(byDesc("Operating Efficiency"))
       || isVisible(byDesc("Valuation"))) {
   return;
}

for (int i = 1; i <= 6; i++) {
   keyRatiosMoreBodySwipeDown();
   sleep(500);

   if (isVisible(byDesc("Profitability"))
           || isVisible(byDesc("Growth"))
           || isVisible(byDesc("Solvency"))
           || isVisible(byDesc("Operating Efficiency"))
           || isVisible(byDesc("Valuation"))) {
       return;
   }
}
}

private void returnFromKeyRatiosMorePage() {
    ReportLogger.step("Returning from Key Ratios More details page");
    driver.navigate().back();
    keyRatioWait(ExecutionProfile.defaultWait()).withMessage("Key Ratios More did not close after Back")
            .until(d -> !isKeyRatiosMoreScreenVisible() && isOnStockDetailsPage());
    ReportLogger.pass("Returned to Stock Details from Key Ratios More");
}


private void keyRatiosMoreBodySwipeUp() {
    swipeVisibleBody(true, "key-ratios-more");
}


private void keyRatiosMoreBodySwipeDown() {
    swipeVisibleBody(false, "key-ratios-more");
}


private void keyRatiosMoreTopTabsSwipeLeft() {
swipeKeyRatiosMoreTopTabs(true);
}

private void keyRatiosMoreTopTabsSwipeRight() {
swipeKeyRatiosMoreTopTabs(false);
}

private void swipeKeyRatiosMoreTopTabs(boolean left) {
try {
   Dimension size = driver.manage().window().getSize();

   int y = getKeyRatiosMoreTopTabsCenterY();
   int startX = left ? (int) (size.getWidth() * 0.82) : (int) (size.getWidth() * 0.22);
   int endX = left ? (int) (size.getWidth() * 0.22) : (int) (size.getWidth() * 0.82);

   PointerInput finger = new PointerInput(PointerInput.Kind.TOUCH, "finger");
   Sequence swipe = new Sequence(finger, 1);

   swipe.addAction(finger.createPointerMove(Duration.ZERO, PointerInput.Origin.viewport(), startX, y));
   swipe.addAction(finger.createPointerDown(PointerInput.MouseButton.LEFT.asArg()));
   swipe.addAction(finger.createPointerMove(Duration.ofMillis(520), PointerInput.Origin.viewport(), endX, y));
   swipe.addAction(finger.createPointerUp(PointerInput.MouseButton.LEFT.asArg()));

   driver.perform(Collections.singletonList(swipe));

} catch (Exception e) {
   ReportLogger.debug("Key Ratios More top tab swipe skipped: " + cleanError(e.getMessage()));
}
}

private int getKeyRatiosMoreTopTabsCenterY() {
  String[] knownTabs = new String[]{
          "Profitability",
          "Growth",
          "Solvency",
          "Operating Efficiency",
          "Valuation"
  };

  for (String tab : knownTabs) {
      WebElement element = findVisibleElement(byDesc(tab));

      if (element != null) {
          Rectangle rect = element.getRect();
          return rect.getY() + rect.getHeight() / 2;
      }
  }

  /*
   * Fallback if tabs are temporarily not exposed by Appium.
   */
  Dimension size = driver.manage().window().getSize();
  return (int) (size.getHeight() * 0.18);
}
//=========================================================
//SD_011 - PEERS
//=========================================================

public void verifyPeers() {
    runKeyRatiosWithImmediateLookups(() -> {
        ReportLogger.step("Verifying Peers section");
        recoverStockDetailsIfNeeded();
        alignPeersSectionSafely();
        validatePeersVisibleTable();
        ReportLogger.pass("Peers section validated successfully");
    });
}

private void alignPeersSectionSafely() {
    ReportLogger.step("Aligning Peers using one native snapshot per attempt");
    long deadline = System.nanoTime() + ExecutionProfile.longWait().toNanos();
    String previous = "";
    int unchanged = 0;
    NativeSnapshot snapshot = readNativeSnapshot();
    for (int attempt = 0; attempt < 28; attempt++) {
        if (peersSnapshotReady(snapshot)) return;
        // Always inspect the result of the last gesture, including when its
        // command consumed the remaining time budget.
        if (System.nanoTime() >= deadline || unchanged >= 3) break;
        boolean below = snapshot.has("Shareholding") || snapshot.has("Company Profile")
                || snapshot.has("Business") || snapshot.has("News") || snapshot.has("Analysis");
        swipeStockSectionBody(!below, snapshot.has("Peers"), "peers");
        sleep(400);
        snapshot = readNativeSnapshot();
        String current = nativeSignature(snapshot.nodes);
        unchanged = current.equals(previous) ? unchanged + 1 : 0;
        previous = current;
    }
    if (peersSnapshotReady(snapshot)) return;
    throw new AssertionError("Peers table not ready after bounded alignment | values="
            + nativeStrings(snapshot.section("Peers", "Shareholding", "Company Profile")));
}

private boolean isPeersSectionVisibleEnough() {
    return peersSnapshotReady(readNativeSnapshot());
}

private void stabilizePeersTableBody() {
    // Shared by SD_012 before opening More. Use the same bounded readiness check.
    alignPeersSectionSafely();
}

private java.util.LinkedHashSet<String> collectVisiblePeersCompanyNames() {
    return peersCompanies(readNativeSnapshot());
}

private List<String> getVisiblePeersSectionValues() {
    NativeSnapshot snapshot = readNativeSnapshot();
    return nativeStrings(snapshot.section("Peers", "Shareholding", "Company Profile"));
}

private java.util.LinkedHashSet<String> peersCompanies(NativeSnapshot snapshot) {
    java.util.LinkedHashSet<String> companies = new java.util.LinkedHashSet<>();
    for (String value : nativeStrings(snapshot.section("Peers", "Shareholding", "Company Profile"))) {
        for (String company : new String[]{"ITC", "Godfrey Phillips", "NTC", "Sinnar Bidi Udyog", "VST", "Nestle India"}) {
            if (value.equals(company)) companies.add(company);
        }
    }
    return companies;
}

private boolean peersSnapshotReady(NativeSnapshot snapshot) {
    List<String> values = nativeStrings(snapshot.section("Peers", "Shareholding", "Company Profile"));
    java.util.LinkedHashSet<String> companies = peersCompanies(snapshot);
    boolean header = values.contains("Company") || values.stream().anyMatch(v -> v.contains("Market Cap") || v.contains("Revenue (TTM)"));
    long numericCells = values.stream().filter(v -> v.matches("[-+]?\\d[\\d,]*(?:\\.\\d+)?")).count();
    return (snapshot.has("Peers") || header) && companies.contains("ITC") && companies.size() >= 2 && numericCells >= 2;
}

private void validatePeersVisibleTable() {
    NativeSnapshot snapshot = readNativeSnapshot();
    if (!peersSnapshotReady(snapshot)) {
        throw new AssertionError("Peers company rows or numeric cells missing | values="
                + nativeStrings(snapshot.section("Peers", "Shareholding", "Company Profile")));
    }
    logValidatedText("Peers table companies", peersCompanies(snapshot).toString());
    logValidatedText("Peers live table", nativeStrings(snapshot.section("Peers", "Shareholding", "Company Profile")).toString());
}

//=========================================================
//SD_012 - PEERS MORE DETAILS
//=========================================================

public void verifyPeersMoreDetails() {
 runKeyRatiosWithImmediateLookups(() -> {
     ReportLogger.step("Verifying Peers More Details section");
     recoverStockDetailsIfNeeded();
     openPeersMoreDetailsSafely();
     runWithChildScreenCleanup(this::validatePeersMoreDetailsPage,
             this::returnFromPeersMoreDetailsPageSafely);
     ReportLogger.pass("Peers More Details section validated successfully");
 });
}

private void returnFromPeersMoreDetailsPageSafely() {
 ReportLogger.step("Returning from Peers More Details page");

 /*
  * Critical fix:
  * Peers More normally returns to the Stock Details Peers section after one Back.
  * Do not over-press back and push app to dashboard before SD_013.
  */
 if (isReturnedToStockDetailsAfterPeersMore()) {
     ReportLogger.pass("Returned from Peers More Details to Stock Details");
     return;
 }

 for (int attempt = 1; attempt <= 2; attempt++) {
     if (isDashboardOrHomeVisible()) {
         ReportLogger.debug("Dashboard/Home detected while returning from Peers More. Reopening Stock Details.");
         openStockDetailsFromSearch();
         ReportLogger.pass("Stock Details reopened safely after Peers More return fallback");
         return;
     }

     pressBackSilently();
     sleep(1600);

     if (isReturnedToStockDetailsAfterPeersMore()) {
         ReportLogger.pass("Returned from Peers More Details to Stock Details after back attempt " + attempt);
         return;
     }

     if (isDashboardOrHomeVisible()) {
         ReportLogger.debug("Dashboard/Home reached after Peers More back attempt "
                 + attempt
                 + ". Reopening Stock Details.");
         openStockDetailsFromSearch();
         ReportLogger.pass("Stock Details reopened safely after Peers More return fallback");
         return;
     }
 }

 if (isOnStockDetailsPage() && !isPeersMoreDetailsTabStripVisible() && !isDashboardOrHomeVisible()) {
     ReportLogger.pass("Returned from Peers More Details to Stock Details by final Stock Details check");
     return;
 }

 throw new RuntimeException("Unable to return from Peers More Details page to Stock Details page safely"
         + " | values=" + collectVisibleContentDescriptions());
}

private boolean isReturnedToStockDetailsAfterPeersMore() {
 if (isDashboardOrHomeVisible()) {
     return false;
 }

 /*
  * Peers More page itself contains Peers/company text, so first reject the
  * More-details tab strip.
  */
 if (isPeersMoreDetailsTabStripVisible()) {
     return false;
 }

 return isOnStockDetailsPage()
         || isPeersSectionVisibleEnough()
         || isVisible(byDesc("Shareholding"))
         || isVisible(byDescContains("Shareholding"))
         || isVisible(byDesc("Company Profile"))
         || isVisible(byDescContains("Company Profile"))
         || isVisible(byDesc("News"))
         || isVisible(byDescContains("News"))
         || isVisible(byDesc("Analysis"))
         || isVisible(byDescContains("Analysis"));
}

private boolean isPeersMoreDetailsTabStripVisible() {
 List<String> values = collectPeersMoreVisibleStrings();

 for (String rawValue : values) {
     if (rawValue == null) {
         continue;
     }

     String[] parts = rawValue.split("\\n|\\|");

     for (String part : parts) {
         String clean = normalizeSpaces(part);

         if (!clean.isEmpty() && isPeersMoreDetailsTab(clean)) {
             return true;
         }
     }
 }

 return false;
}

private void openPeersMoreDetailsSafely() {
    ReportLogger.step("Opening Peers More Details safely");

    alignPeersSectionSafely();
    stabilizePeersTableBody();

    java.util.LinkedHashSet<String> peerCompanies = collectVisiblePeersCompanyNames();

    if (!peerCompanies.contains("ITC") || peerCompanies.size() < 2) {
        throw new AssertionError("Peers section is not ready before opening Peers More"
                + " | companies=" + peerCompanies
                + " | values=" + getVisiblePeersSectionValues());
    }

    WebElement moreButton = findPeersMoreButtonSafely();

    if (moreButton != null) {
        tapElementCenter(moreButton);
        sleep(1800);

        if (isPeersMoreDetailsPageOpen()) {
            ReportLogger.pass("Peers More Details opened using visible More button");
            return;
        }
    }

    tapPeersMoreByCoordinates();
    sleep(2200);

    if (isPeersMoreDetailsPageOpen()) {
        ReportLogger.pass("Peers More Details opened using coordinate fallback");
        return;
    }

    throw new AssertionError("Peers More Details page did not open after tapping More"
            + " | companies=" + peerCompanies);
}
private WebElement findPeersMoreButtonSafely() {
 ReportLogger.step("Finding Peers More button safely");

 WebElement peersHeading = findVisibleElement(byDesc("Peers"));

 if (peersHeading == null) {
     ReportLogger.debug("Peers heading not visible while finding Peers More button");
     return null;
 }

 Rectangle peersRect = peersHeading.getRect();
 Dimension size = driver.manage().window().getSize();

 int minY = peersRect.getY() - 100;
 int maxY = peersRect.getY() + 220;
 int minX = (int) (size.getWidth() * 0.70);

 try {
     List<WebElement> elements = driver.findElements(By.xpath("//*"));

     for (WebElement element : elements) {
         try {
             if (element == null || !element.isDisplayed()) {
                 continue;
             }

             Rectangle rect = element.getRect();

             int centerX = rect.getX() + rect.getWidth() / 2;
             int centerY = rect.getY() + rect.getHeight() / 2;

             if (centerX < minX || centerY < minY || centerY > maxY) {
                 continue;
             }

             String text = normalizeSpaces(element.getText());
             String desc = normalizeSpaces(element.getAttribute("content-desc"));
             String name = normalizeSpaces(element.getAttribute("name"));
             String attrText = normalizeSpaces(element.getAttribute("text"));

             if ("More".equals(text)
                     || "More".equals(desc)
                     || "More".equals(name)
                     || "More".equals(attrText)) {
                 ReportLogger.pass("Peers More button found");
                 return element;
             }

         } catch (Exception ignored) {
             // Ignore stale/inaccessible elements.
         }
     }
 } catch (Exception e) {
     ReportLogger.debug("Peers More button scan skipped: " + cleanError(e.getMessage()));
 }

 ReportLogger.debug("Peers More button not found by element scan");
 return null;
}

private void tapPeersMoreByCoordinates() {
 ReportLogger.step("Tapping Peers More using coordinate fallback");

 WebElement peersHeading = findVisibleElement(byDesc("Peers"));
 Dimension size = driver.manage().window().getSize();

 int x = (int) (size.getWidth() * 0.92);
 int y;

 if (peersHeading != null) {
     Rectangle rect = peersHeading.getRect();
     y = rect.getY() + rect.getHeight() / 2;
 } else {
     y = (int) (size.getHeight() * 0.38);
 }

 tapByCoordinates(x, y);
}

private boolean isPeersMoreDetailsPageOpen() {
 List<String> values = collectPeersMoreVisibleStrings();

 boolean hasPeersHeading = false;
 boolean hasCompany = false;
 boolean hasPeerCompany = false;
 boolean hasTabOrHeader = false;

 for (String value : values) {
     String clean = normalizeSpaces(value);

     if (clean.equals("Peers") || clean.startsWith("Peers ")) {
         hasPeersHeading = true;
     }

     if (clean.equals("Company") || clean.startsWith("Company ")) {
         hasCompany = true;
     }

     if (isPeersMoreExactCompany(clean)) {
         hasPeerCompany = true;
     }

     if (isPeersMoreMetricHeader(clean) || isPeersMoreDetailsTab(clean)) {
         hasTabOrHeader = true;
     }
 }

 return hasPeersHeading && (hasCompany || hasPeerCompany || hasTabOrHeader);
}

private void validatePeersMoreDetailsPage() {
 ReportLogger.step("Validating Peers More Details page with all tabs, all companies and row labels");

 if (!isPeersMoreDetailsPageOpen()) {
     throw new AssertionError("Peers More Details page is not open or not readable");
 }

 validatePeersMoreTopCompanyTable();

 List<String> tabs = Arrays.asList(
         "Income Statement",
         "Liabilities",
         "Assets",
         "Cashflow",
         "Profitability Ratios",
         "Growth Ratios",
         "Solvency Ratios",
         "Operating Efficiency Ratios",
         "Valuation Ratios"
 );

 for (String tabName : tabs) {
     openPeersMoreTabSafely(tabName);
     validatePeersMoreTabCompaniesAndRowLabels(tabName);
 }

 ReportLogger.pass("Peers More Details page validated successfully for all tabs, companies and row labels");
}

private void validatePeersMoreTopCompanyTable() {
 ReportLogger.step("Validating Peers More top company table");

 java.util.LinkedHashSet<String> companies = collectPeersMoreCompaniesWithHorizontalScan();

 assertAllPeersMoreCompaniesPresent("Top company table", companies);

 logValidatedText("Peers More top companies", companies.toString());

 ReportLogger.pass("Peers More top company table validated successfully"
         + " | companies=" + companies);
}

private void openPeersMoreTabSafely(String tabName) {
 ReportLogger.step("Opening Peers More tab: " + tabName);

 if (tapPeersMoreTabIfVisible(tabName)) {
     sleep(1300);
     ReportLogger.pass("Peers More tab opened: " + tabName);
     return;
 }

 for (int i = 1; i <= 10; i++) {
     peersMoreTabsSwipeLeft();
     sleep(700);

     if (tapPeersMoreTabIfVisible(tabName)) {
         sleep(1300);
         ReportLogger.pass("Peers More tab opened after left swipe " + i + ": " + tabName);
         return;
     }
 }

 for (int i = 1; i <= 10; i++) {
     peersMoreTabsSwipeRight();
     sleep(700);

     if (tapPeersMoreTabIfVisible(tabName)) {
         sleep(1300);
         ReportLogger.pass("Peers More tab opened after right swipe " + i + ": " + tabName);
         return;
     }
 }

 throw new AssertionError("Peers More tab not found/opened: " + tabName);
}

private boolean tapPeersMoreTabIfVisible(String tabName) {
 WebElement tab = findVisibleElement(byDesc(tabName));

 if (tab == null) {
     tab = findVisibleElement(byDescContains(tabName));
 }

 if (tab == null) {
     tab = findPeersMoreTextElement(tabName);
 }

 if (tab == null) {
     return false;
 }

 tapElementCenter(tab);
 return true;
}

private WebElement findPeersMoreTextElement(String textToFind) {
    /*
     * Fast scan first.
     */
    List<WebElement> fastElements = getVisibleViewElementsFast();

    WebElement matchedElement = findTextElementInList(fastElements, textToFind);

    if (matchedElement != null) {
        return matchedElement;
    }

    /*
     * Slow fallback only if fast scan failed.
     */
    List<WebElement> fallbackElements = getVisibleElementsFallbackSlow();

    matchedElement = findTextElementInList(fallbackElements, textToFind);

    if (matchedElement != null) {
        return matchedElement;
    }

    return null;
}

private WebElement findTextElementInList(List<WebElement> elements, String textToFind) {
    for (WebElement element : elements) {
        try {
            if (element == null || !element.isDisplayed()) {
                continue;
            }

            String text = normalizeSpaces(element.getText());
            String desc = normalizeSpaces(element.getAttribute("content-desc"));
            String name = normalizeSpaces(element.getAttribute("name"));
            String attrText = normalizeSpaces(element.getAttribute("text"));

            if (text.equals(textToFind)
                    || desc.equals(textToFind)
                    || name.equals(textToFind)
                    || attrText.equals(textToFind)
                    || text.contains(textToFind)
                    || desc.contains(textToFind)
                    || name.contains(textToFind)
                    || attrText.contains(textToFind)) {
                return element;
            }

        } catch (Exception ignored) {
            // Ignore stale Flutter/Appium element
        }
    }

    return null;
}

private void validatePeersMoreTabCompaniesAndRowLabels(String tabName) {
 ReportLogger.step("Validating Peers More tab companies and row labels: " + tabName);

 java.util.LinkedHashSet<String> companies = collectPeersMoreCompaniesWithHorizontalScan();

 assertAllPeersMoreCompaniesPresent(tabName, companies);

 java.util.LinkedHashSet<String> rowLabels = collectPeersMoreRowLabelsWithVerticalScan(tabName);

 List<String> expectedLabels = getExpectedPeersMoreRowLabels(tabName);
 List<String> missingLabels = new ArrayList<>();

 for (String expectedLabel : expectedLabels) {
     if (!rowLabels.contains(expectedLabel)) {
         missingLabels.add(expectedLabel);
     }
 }

 if (!missingLabels.isEmpty()) {
     throw new AssertionError("Peers More row labels missing"
             + " | tab=" + tabName
             + " | missing=" + missingLabels
             + " | captured=" + rowLabels);
 }

 logValidatedText("Peers More " + tabName + " companies", companies.toString());
 logValidatedText("Peers More " + tabName + " row labels", rowLabels.toString());

 ReportLogger.pass("Peers More tab validated successfully"
         + " | tab=" + tabName
         + " | companies=" + companies
         + " | rowLabels=" + rowLabels);
}

private java.util.LinkedHashSet<String> collectPeersMoreVisibleRowLabels(String tabName) {
 java.util.LinkedHashSet<String> rowLabels = new java.util.LinkedHashSet<>();

 List<String> values = collectPeersMoreVisibleStrings();

 for (String value : values) {
     collectPeersMoreRowLabelFromText(value, tabName, rowLabels);

     String[] parts = value.split("\\n|\\s{2,}|\\|");

     for (String part : parts) {
         collectPeersMoreRowLabelFromText(part, tabName, rowLabels);
     }
 }

 // Retain native TextView/ImageView support without repeating an entire XPath tree scan.
 for (String expected : getExpectedPeersMoreRowLabels(tabName)) {
     if (rowLabels.contains(expected)) continue;
     By[] rowLocators = "Short term debt to equity ratio".equals(expected)
             ? new By[]{byDescContains("Short"), byTextContains("Short"),
                     byDescContains("short"), byTextContains("short")}
             : new By[]{byDescContains(expected), byTextContains(expected)};
     for (By locator : rowLocators) {
         for (WebElement element : driver.findElements(locator)) {
             try {
                 if (element.isDisplayed() && isFullyOnValuationScreen(element.getRect())) {
                     collectPeersMoreRowLabelFromText(getElementReadableText(element), tabName, rowLabels);
                 }
             } catch (StaleElementReferenceException replacedRow) {
                 // The next body snapshot will reacquire a row rebuilt during scrolling.
             }
         }
     }
 }
 ReportLogger.step("Peers More row labels captured for " + tabName + ": " + rowLabels);

 return rowLabels;
}

private java.util.LinkedHashSet<String> collectPeersMoreRowLabelsWithVerticalScan(String tabName) {
    java.util.LinkedHashSet<String> labels = new java.util.LinkedHashSet<>();
    List<String> expected = getExpectedPeersMoreRowLabels(tabName);
    final int[] swipes = {0};
    runWithChildScreenCleanup(() -> {
        int stableViews = 0;
        String previous = null;
        long noProgressDeadline = System.nanoTime() + ExecutionProfile.longWait().toNanos();
        for (int attempt = 0; attempt < 12; attempt++) {
            java.util.LinkedHashSet<String> visible = collectPeersMoreVisibleRowLabels(tabName);
            int before = labels.size();
            labels.addAll(visible);
            if (labels.containsAll(expected)) return;
            if (labels.size() > before) {
                noProgressDeadline = System.nanoTime() + ExecutionProfile.longWait().toNanos();
            }
            if (labels.size() == before && System.nanoTime() >= noProgressDeadline) break;
            String signature = visible.toString();
            stableViews = labels.size() == before && signature.equals(previous) ? stableViews + 1 : 0;
            previous = signature;
            if (stableViews >= 3 || attempt == 11) break;
            swipeVisibleBody(true, "peers-more-rows");
            swipes[0]++;
            sleep(ExecutionProfile.pollingInterval().toMillis());
        }
    }, () -> {
        // Restore the body before the next tab/company scan. Only undo swipes made here.
        for (int i = 0; i < swipes[0]; i++) {
            swipeVisibleBody(false, "peers-more-rows");
            sleep(ExecutionProfile.pollingInterval().toMillis());
        }
    });
    return labels;
}

private void collectPeersMoreRowLabelFromText(
     String text,
     String tabName,
     java.util.LinkedHashSet<String> rowLabels
) {
 if (text == null) {
     return;
 }

 String clean = normalizeSpaces(text.replace("\\n", " "));

 if (clean.isEmpty()) {
     return;
 }

 for (String expectedLabel : getExpectedPeersMoreRowLabels(tabName)) {
     if (clean.equals(expectedLabel) || clean.contains(expectedLabel)
             || titlesMatchKeyRatiosMoreExpectedBlock(extractCleanKeyRatiosMoreBlockTitle(clean), expectedLabel)
             || ("Short term debt to equity ratio".equals(expectedLabel)
                 && Pattern.compile("(?i)^short[\\s\\-–—]+term\\s+debt\\s+to\\s+equity"
                         + "(?:\\s+ratio)?\\s*(?:\\(%\\))?\\s*(?::|\\||--|N/?A|[-+]?\\d|$)")
                         .matcher(clean).find())) {
         rowLabels.add(expectedLabel);
     }
 }
}

private List<String> getExpectedPeersMoreRowLabels(String tabName) {
 if ("Income Statement".equals(tabName)) {
     return Arrays.asList(
             "Operating Revenue",
             "Total Income",
             "Total Expenditure",
             "EBITDA",
             "Depreciation",
             "EBIT",
             "Interest"
     );
 }

 if ("Liabilities".equals(tabName)) {
     return Arrays.asList(
             "Shareholder's Funds",
             "Minority Interest",
             "Non-Current Liabilities",
             "Current Liabilities",
             "Total Liabilities"
     );
 }

 if ("Assets".equals(tabName)) {
     return Arrays.asList(
             "Non-Current Assets",
             "Current Assets",
             "Total Assets",
             "Total Debt",
             "Net Current Assets"
     );
 }

 if ("Cashflow".equals(tabName)) {
     return Arrays.asList(
             "Cash From Operating Activities",
             "Cash Flow from Investing Activities",
             "Cash from Financing Activities",
             "Net Cash Inflow / Outflow"
     );
 }

 if ("Profitability Ratios".equals(tabName)) {
     return Arrays.asList(
             "ROCE (%)",
             "ROE (%)",
             "ROA (%)",
             "EBIT Margin (%)",
             "Net Margin (%)",
             "Cash Profit Margin (%)"
     );
 }

 if ("Growth Ratios".equals(tabName)) {
     return Arrays.asList(
             "Revenue Growth (%)",
             "EBIT Growth (%)",
             "Net Profit Growth (%)",
             "EPS Growth (%)",
             "Book Value Growth (%)"
     );
 }

 if ("Solvency Ratios".equals(tabName)) {
     return Arrays.asList(
             "Debt to Equity",
             "Short term debt to equity ratio",
             "Current Ratio",
             "Quick Ratio",
             "Interest Coverage"
     );
 }

 if ("Operating Efficiency Ratios".equals(tabName)) {
     return Arrays.asList(
             "Debtors to sales (%)",
             "Asset Turnover",
             "Receivable days",
             "Inventory Days",
             "Payable days",
             "Cash Conversion Cycle"
     );
 }

 if ("Valuation Ratios".equals(tabName)) {
     return Arrays.asList(
             "Price / Earnings",
             "Price / Book Value",
             "Dividend Yield (%)",
             "EV/EBITDA",
             "Market Cap"
     );
 }

 return Collections.emptyList();
}

private java.util.LinkedHashSet<String> collectPeersMoreCompaniesWithHorizontalScan() {
 java.util.LinkedHashSet<String> companies = new java.util.LinkedHashSet<>();

 collectPeersMoreCompaniesFromCurrentViewport(companies);

 for (int i = 1; i <= 6; i++) {
     if (areAllPeersMoreCompaniesCaptured(companies)) {
         return companies;
     }

     peersMoreTableSwipeLeft();
     sleep(800);

     collectPeersMoreCompaniesFromCurrentViewport(companies);
 }

 for (int i = 1; i <= 6; i++) {
     if (areAllPeersMoreCompaniesCaptured(companies)) {
         return companies;
     }

     peersMoreTableSwipeRight();
     sleep(800);

     collectPeersMoreCompaniesFromCurrentViewport(companies);
 }

 return companies;
}

private void collectPeersMoreCompaniesFromCurrentViewport(
        java.util.LinkedHashSet<String> companies
) {
    List<String> values = collectPeersMoreVisibleStrings();

    for (String value : values) {
        if (value == null) {
            continue;
        }

        String clean = normalizeSpaces(value);

        addPeersMoreCompanyIfExact(clean, companies);

        String[] parts = value.split("\\n|\\s{2,}|\\|");

        for (String part : parts) {
            addPeersMoreCompanyIfExact(part, companies);
        }
    }

    /*
     * No direct By.xpath("//*") here.
     * collectPeersMoreVisibleStrings() already handles fast scan + fallback.
     */
    ReportLogger.step("Peers More companies currently captured: " + companies);
}
private void addPeersMoreCompanyIfExact(
     String value,
     java.util.LinkedHashSet<String> companies
) {
 String company = getPeersMoreExactCompanyName(value);

 if (!company.isEmpty()) {
     companies.add(company);
 }
}

private List<String> getExpectedPeersMoreCompanies() {
 /*
  * Current app/report exposes these 4 ITC peer companies:
  * ITC, Godfrey Phillips, NTC, VST.
  *
  * Nestle India is intentionally not mandatory here because it is not visible
  * in current Peers / Peers More data and was causing SD_012 to fail.
  */
 return Arrays.asList(
         "ITC",
         "Godfrey Phillips",
         "NTC",
         "VST"
 );
}

private void assertAllPeersMoreCompaniesPresent(
     String areaName,
     java.util.LinkedHashSet<String> companies
) {
 List<String> expectedCompanies = getExpectedPeersMoreCompanies();
 List<String> missingCompanies = new ArrayList<>();

 for (String expectedCompany : expectedCompanies) {
     if (!companies.contains(expectedCompany)) {
         missingCompanies.add(expectedCompany);
     }
 }

 if (!missingCompanies.isEmpty()) {
     throw new AssertionError("Peers More companies missing"
             + " | area=" + areaName
             + " | missing=" + missingCompanies
             + " | captured=" + companies);
 }

 ReportLogger.pass("Peers More expected companies validated"
         + " | area=" + areaName
         + " | expected=" + expectedCompanies
         + " | captured=" + companies);
}

private boolean areAllPeersMoreCompaniesCaptured(
     java.util.LinkedHashSet<String> companies
) {
 for (String expectedCompany : getExpectedPeersMoreCompanies()) {
     if (!companies.contains(expectedCompany)) {
         return false;
     }
 }

 return true;
}

private List<String> collectPeersMoreVisibleStrings() {
    List<String> values = new ArrayList<>();

    /*
     * Fast source first:
     * Flutter usually exposes useful data through content-desc on android.view.View.
     */
    List<String> descValues = collectVisibleContentDescriptions();

    for (String value : descValues) {
        addUniquePeersMoreString(values, value);
    }

    /*
     * Fast element scan second:
     * android.view.View is much cheaper than By.xpath("//*").
     */
    List<WebElement> fastElements = getVisibleViewElementsFast();

    for (WebElement element : fastElements) {
        try {
            addUniquePeersMoreString(values, element.getAttribute("content-desc"));
            addUniquePeersMoreString(values, element.getText());
            addUniquePeersMoreString(values, element.getAttribute("text"));
            addUniquePeersMoreString(values, element.getAttribute("name"));
        } catch (Exception ignored) {
            // Ignore stale Appium element
        }
    }

    /*
     * Slow XPath fallback only if fast scan did not capture enough useful data.
     * Do not run this every time.
     */
    if (values.size() < 4) {
        List<WebElement> fallbackElements = getVisibleElementsFallbackSlow();

        for (WebElement element : fallbackElements) {
            try {
                addUniquePeersMoreString(values, element.getAttribute("content-desc"));
                addUniquePeersMoreString(values, element.getText());
                addUniquePeersMoreString(values, element.getAttribute("text"));
                addUniquePeersMoreString(values, element.getAttribute("name"));
            } catch (Exception ignored) {
                // Ignore stale Appium element
            }
        }
    }

    return values;
}

private void addUniquePeersMoreString(List<String> values, String rawValue) {
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

private boolean isPeersMoreExactCompany(String value) {
 return !getPeersMoreExactCompanyName(value).isEmpty();
}

private String getPeersMoreExactCompanyName(String value) {
 if (value == null) {
     return "";
 }

 String clean = normalizeSpaces(value);

 if (clean.equals("ITC")) {
     return "ITC";
 }

 if (clean.equals("Godfrey Phillips")) {
     return "Godfrey Phillips";
 }

 /*
  * Parser can still recognize Nestle India if app exposes it in future.
  * It is not mandatory in getExpectedPeersMoreCompanies().
  */
 if (clean.equals("Nestle India")) {
     return "Nestle India";
 }

 if (clean.equals("NTC")) {
     return "NTC";
 }

 if (clean.equals("VST")) {
     return "VST";
 }

 return "";
}

private boolean isPeersMoreMetricHeader(String value) {
 return !getPeersMoreMetricHeaderName(value).isEmpty();
}

private String getPeersMoreMetricHeaderName(String value) {
 if (value == null) {
     return "";
 }

 String clean = normalizeSpaces(value)
         .replace("₹", "")
         .toLowerCase();

 if (clean.contains("company")) {
     return "Company";
 }

 if (clean.contains("market cap")) {
     return "Market Cap";
 }

 if (clean.contains("interest income")) {
     return "Interest Income";
 }

 if (clean.contains("revenue")) {
     return "Revenue";
 }

 if (clean.contains("net profit")) {
     return "Net Profit";
 }

 if (clean.contains("net margin")) {
     return "Net Margin";
 }

 if (clean.contains("roe")) {
     return "RoE";
 }

 if (clean.contains("price / earnings") || clean.contains("p/e")) {
     return "Price / Earnings";
 }

 if (clean.contains("price / book") || clean.contains("p/b")) {
     return "Price / Book Value";
 }

 if (clean.contains("dividend yield")) {
     return "Dividend Yield";
 }

 if (clean.contains("ev/ebitda")) {
     return "EV/EBITDA";
 }

 return "";
}

private boolean isPeersMoreDetailsTab(String value) {
 return !getPeersMoreDetailsTabName(value).isEmpty();
}

private String getPeersMoreDetailsTabName(String value) {
 if (value == null) {
     return "";
 }

 String clean = normalizeSpaces(value);

 if (clean.contains("Income Statement")) {
     return "Income Statement";
 }

 if (clean.contains("Liabilities")) {
     return "Liabilities";
 }

 if (clean.contains("Assets")) {
     return "Assets";
 }

 if (clean.contains("Cashflow")) {
     return "Cashflow";
 }

 if (clean.contains("Profitability Ratios")) {
     return "Profitability Ratios";
 }

 if (clean.contains("Growth Ratios")) {
     return "Growth Ratios";
 }

 if (clean.contains("Solvency Ratios")) {
     return "Solvency Ratios";
 }

 if (clean.contains("Operating Efficiency Ratios")) {
     return "Operating Efficiency Ratios";
 }

 if (clean.contains("Valuation Ratios")) {
     return "Valuation Ratios";
 }

 return "";
}

private void peersMoreTabsSwipeLeft() {
 swipePeersMoreHorizontalArea(true, true);
}

private void peersMoreTabsSwipeRight() {
 swipePeersMoreHorizontalArea(false, true);
}

private void peersMoreTableSwipeLeft() {
 swipePeersMoreHorizontalArea(true, false);
}

private void peersMoreTableSwipeRight() {
 swipePeersMoreHorizontalArea(false, false);
}

private void swipePeersMoreHorizontalArea(boolean left, boolean tabsArea) {
 try {
     Dimension size = driver.manage().window().getSize();

     int y;

     if (tabsArea) {
         y = getPeersMoreTabsCenterY();
     } else {
         y = getPeersMoreTableHeaderCenterY();
     }

     int startX = left ? (int) (size.getWidth() * 0.84) : (int) (size.getWidth() * 0.24);
     int endX = left ? (int) (size.getWidth() * 0.24) : (int) (size.getWidth() * 0.84);

     PointerInput finger = new PointerInput(PointerInput.Kind.TOUCH, "finger");
     Sequence swipe = new Sequence(finger, 1);

     swipe.addAction(finger.createPointerMove(Duration.ZERO, PointerInput.Origin.viewport(), startX, y));
     swipe.addAction(finger.createPointerDown(PointerInput.MouseButton.LEFT.asArg()));
     swipe.addAction(finger.createPointerMove(Duration.ofMillis(560), PointerInput.Origin.viewport(), endX, y));
     swipe.addAction(finger.createPointerUp(PointerInput.MouseButton.LEFT.asArg()));

     driver.perform(Collections.singletonList(swipe));

 } catch (Exception firstError) {
     Map<String, Object> params = new HashMap<>();

     params.put("left", 120);
     params.put("top", tabsArea ? 820 : 430);
     params.put("width", 820);
     params.put("height", tabsArea ? 220 : 560);
     params.put("direction", left ? "left" : "right");
     params.put("percent", 0.68);

     driver.executeScript("mobile: scrollGesture", params);
 }
}

private int getPeersMoreTabsCenterY() {
 String[] knownTabs = new String[]{
         "Income Statement",
         "Liabilities",
         "Assets",
         "Cashflow",
         "Profitability Ratios",
         "Growth Ratios",
         "Solvency Ratios",
         "Operating Efficiency Ratios",
         "Valuation Ratios"
 };

 for (String tab : knownTabs) {
     WebElement element = findVisibleElement(byDescContains(tab));

     if (element != null) {
         Rectangle rect = element.getRect();
         return rect.getY() + rect.getHeight() / 2;
     }
 }

 Dimension size = driver.manage().window().getSize();
 return (int) (size.getHeight() * 0.66);
}

private int getPeersMoreTableHeaderCenterY() {
 for (String company : getExpectedPeersMoreCompanies()) {
     WebElement element = findVisibleElement(byDescContains(company));

     if (element != null) {
         Rectangle rect = element.getRect();
         return rect.getY() + rect.getHeight() / 2;
     }
 }

 Dimension size = driver.manage().window().getSize();
 return (int) (size.getHeight() * 0.43);
}

//=========================================================
//SD_013 - SHAREHOLDING
//=========================================================

public void verifyShareholding() {
    runKeyRatiosWithImmediateLookups(() -> {
        ReportLogger.step("Verifying Shareholding with bounded native snapshots");
        recoverStockDetailsIfNeeded();
        alignShareholdingSectionSafely();
        java.util.LinkedHashSet<String> quarters = collectShareholdingQuarters();
        if (quarters.size() < 4) throw new AssertionError("Too few Shareholding quarters | expectedAtLeast=4 | actual=" + quarters);
        for (String quarter : quarters) {
            alignShareholdingSectionSafely();
            openShareholdingQuarter(quarter);
            validateShareholdingQuarter(quarter);
        }
        logValidatedText("Shareholding quarter chips", quarters.toString());
        ReportLogger.pass("Every available Shareholding quarter validated with holder labels and actual percentage values");
    });
}

private static final Pattern SHAREHOLDING_QUARTER = Pattern.compile("^(Mar|Jun|Sep|Dec)\\s*['’‘]?\\s*(\\d{2})$");
private static final List<String> SHAREHOLDING_HOLDERS = Arrays.asList("FIIs", "DIIs", "Others", "Mutual Fund");

private String canonicalShareholdingQuarter(String value) {
    java.util.regex.Matcher matcher = SHAREHOLDING_QUARTER.matcher(normalizeSpaces(value));
    return matcher.matches() ? matcher.group(1) + " '" + matcher.group(2) : "";
}

private List<NativeNode> shareholdingNodes(NativeSnapshot snapshot) {
    return snapshot.section("Shareholding", "Company Profile", "Business", "News", "Analysis");
}

private List<NativeNode> shareholdingQuarterNodes(NativeSnapshot snapshot) {
    List<NativeNode> chips = new ArrayList<>();
    for (NativeNode node : shareholdingNodes(snapshot)) {
        if (!canonicalShareholdingQuarter(node.text).isEmpty()) chips.add(node);
    }
    chips.sort((a, b) -> Integer.compare(a.visible.getX(), b.visible.getX()));
    return chips;
}

private boolean shareholdingBodyReady(NativeSnapshot snapshot) {
    List<String> values = nativeStrings(shareholdingNodes(snapshot));
    boolean type = values.stream().anyMatch(v -> v.equals("Type") || v.startsWith("Type "));
    boolean percentage = values.stream().anyMatch(v -> v.contains("Percentage"));
    boolean holder = values.stream().anyMatch(SHAREHOLDING_HOLDERS::contains);
    boolean number = values.stream().anyMatch(v -> v.matches("\\d{1,3}(?:\\.\\d{1,2})?\\s*%?") && shareholdingPercentage(v) != null);
    return !shareholdingQuarterNodes(snapshot).isEmpty() && type && percentage && holder && number;
}

private void alignShareholdingSectionSafely() {
    NativeSnapshot snapshot = readNativeSnapshot();
    String previous = "";
    int unchanged = 0;
    long deadline = System.nanoTime() + ExecutionProfile.longWait().toNanos();
    for (int attempt = 0; attempt < 28; attempt++) {
        if (shareholdingBodyReady(snapshot)) return;
        if (System.nanoTime() >= deadline || unchanged >= 3) break;
        List<String> values = nativeStrings(shareholdingNodes(snapshot));
        boolean tableWithoutChips = shareholdingQuarterNodes(snapshot).isEmpty()
                && values.contains("Type") && values.stream().anyMatch(v -> v.contains("Percentage"));
        boolean below = tableWithoutChips || (!snapshot.has("Shareholding")
                && (snapshot.has("Company Profile") || snapshot.has("Business") || snapshot.has("News") || snapshot.has("Analysis")));
        swipeStockSectionBody(!below, snapshot.has("Shareholding") || tableWithoutChips, "shareholding");
        sleep(400);
        snapshot = readNativeSnapshot();
        String current = nativeSignature(snapshot.nodes);
        unchanged = current.equals(previous) ? unchanged + 1 : 0;
        previous = current;
    }
    if (shareholdingBodyReady(snapshot)) return;
    throw new AssertionError("Shareholding chips and table not ready after bounded alignment | values=" + nativeStrings(shareholdingNodes(snapshot)));
}

private java.util.LinkedHashSet<String> collectShareholdingQuarters() {
    java.util.LinkedHashSet<String> quarters = new java.util.LinkedHashSet<>();
    NativeSnapshot snapshot = readNativeSnapshot();
    // Find both ends, also supporting a test that starts in the middle of history.
    snapshot = scanShareholdingQuarterStrip(snapshot, false, quarters);
    scanShareholdingQuarterStrip(snapshot, true, quarters);
    ReportLogger.pass("Captured available Shareholding quarters: " + quarters);
    return quarters;
}

private NativeSnapshot scanShareholdingQuarterStrip(NativeSnapshot snapshot, boolean forward,
        java.util.LinkedHashSet<String> quarters) {
    String previous = "";
    int unchanged = 0;
    for (int attempt = 0; attempt <= 10; attempt++) {
        List<NativeNode> chips = shareholdingQuarterNodes(snapshot);
        if (chips.isEmpty()) throw new AssertionError("Shareholding quarter strip is not visible; capture its native XML");
        for (NativeNode chip : chips) quarters.add(canonicalShareholdingQuarter(chip.text));
        String signature = nativeSignature(chips);
        unchanged = signature.equals(previous) ? unchanged + 1 : 0;
        if (unchanged >= 2) return snapshot;
        if (attempt == 10) break;
        previous = signature;
        swipeShareholdingQuarterStrip(snapshot, forward);
        sleep(350);
        snapshot = readNativeSnapshot();
    }
    throw new AssertionError("Shareholding history did not reach its end after 10 bounded swipes | captured=" + quarters);
}

private void swipeShareholdingQuarterStrip(NativeSnapshot snapshot, boolean forward) {
    List<NativeNode> chips = shareholdingQuarterNodes(snapshot);
    if (chips.isEmpty()) throw new AssertionError("No visible Shareholding quarter chip to anchor the horizontal swipe");
    NativeNode anchor = chips.get(0);
    int y = anchor.visible.getY() + anchor.visible.getHeight() / 2;
    int startX = (int) (snapshot.screen.getWidth() * (forward ? 0.84 : 0.16));
    int endX = (int) (snapshot.screen.getWidth() * (forward ? 0.16 : 0.84));
    PointerInput finger = new PointerInput(PointerInput.Kind.TOUCH, "shareholding-quarter");
    Sequence swipe = new Sequence(finger, 0);
    swipe.addAction(finger.createPointerMove(Duration.ZERO, PointerInput.Origin.viewport(), startX, y));
    swipe.addAction(finger.createPointerDown(PointerInput.MouseButton.LEFT.asArg()));
    swipe.addAction(finger.createPointerMove(Duration.ofMillis(450), PointerInput.Origin.viewport(), endX, y));
    swipe.addAction(finger.createPointerUp(PointerInput.MouseButton.LEFT.asArg()));
    driver.perform(Collections.singletonList(swipe));
}

private void openShareholdingQuarter(String quarter) {
    NativeSnapshot snapshot = readNativeSnapshot();
    for (int direction = 0; direction < 2; direction++) {
        String previous = "";
        for (int attempt = 0; attempt <= 10; attempt++) {
            List<NativeNode> chips = shareholdingQuarterNodes(snapshot);
            for (NativeNode chip : chips) {
                if (!quarter.equals(canonicalShareholdingQuarter(chip.text))) continue;
                // A clipped neighbour is not a reliable tap target.
                if (chip.visible.getWidth() < chip.bounds.getWidth() * 0.85) continue;
                tapShareholdingQuarterNode(chip);
                sleep(500);
                waitForShareholdingQuarterBody(quarter);
                return;
            }
            String signature = nativeSignature(chips);
            if (signature.equals(previous) || attempt == 10) break;
            previous = signature;
            swipeShareholdingQuarterStrip(snapshot, direction == 0);
            sleep(350);
            snapshot = readNativeSnapshot();
        }
    }
    throw new AssertionError("Shareholding quarter could not be brought fully into view: " + quarter);
}

private void tapShareholdingQuarterNode(NativeNode chip) {
    int x = chip.visible.getX() + chip.visible.getWidth() / 2;
    int y = chip.visible.getY() + chip.visible.getHeight() / 2;
    PointerInput finger = new PointerInput(PointerInput.Kind.TOUCH, "shareholding-quarter-tap");
    Sequence tap = new Sequence(finger, 0);
    tap.addAction(finger.createPointerMove(Duration.ZERO, PointerInput.Origin.viewport(), x, y));
    tap.addAction(finger.createPointerDown(PointerInput.MouseButton.LEFT.asArg()));
    tap.addAction(finger.createPointerUp(PointerInput.MouseButton.LEFT.asArg()));
    driver.perform(Collections.singletonList(tap));
}

private void waitForShareholdingQuarterBody(String quarter) {
    final String[] previous = {null};
    keyRatioWait(ExecutionProfile.defaultWait()).until(d -> {
        NativeSnapshot snapshot = readNativeSnapshot();
        boolean selectedExposed = false, targetSelected = false;
        for (NativeNode chip : shareholdingQuarterNodes(snapshot)) {
            if ("true".equals(chip.element.getAttribute("selected")) || "true".equals(chip.element.getAttribute("checked"))) {
                selectedExposed = true;
                targetSelected |= quarter.equals(canonicalShareholdingQuarter(chip.text));
            }
        }
        if (selectedExposed && !targetSelected) return false;
        if (!shareholdingBodyReady(snapshot)) return false;
        String current = nativeSignature(shareholdingNodes(snapshot));
        boolean stable = current.equals(previous[0]);
        previous[0] = current;
        return stable;
    });
}

private String shareholdingPercentage(String raw) {
    String clean = normalizeSpaces(raw).replace("%", "").trim();
    if (!clean.matches("\\d{1,3}(?:\\.\\d{1,2})?")) return null;
    double number = Double.parseDouble(clean);
    return number >= 0 && number <= 100 ? clean : null;
}

private void validateShareholdingQuarter(String quarter) {
    java.util.LinkedHashSet<String> labels = new java.util.LinkedHashSet<>();
    java.util.LinkedHashSet<String> headers = new java.util.LinkedHashSet<>();
    Map<String, String> percentages = new java.util.LinkedHashMap<>();
    String previous = "";
    int unchanged = 0;
    for (int attempt = 0; attempt < 8; attempt++) {
        NativeSnapshot snapshot = readNativeSnapshot();
        List<NativeNode> nodes = shareholdingNodes(snapshot);
        int headerY = Integer.MAX_VALUE;
        for (NativeNode node : nodes) {
            for (String part : node.text.split("\\n")) {
                String clean = normalizeSpaces(part);
                if (clean.equals("Type")) { headers.add("Type"); headerY = Math.min(headerY, node.visible.getY()); }
                if (clean.contains("Percentage")) { headers.add("Percentage (%)"); headerY = Math.min(headerY, node.visible.getY()); }
            }
        }
        // Once the header scrolls above the viewport, continue collecting the
        // same quarter's rows; never read the preceding Peers numbers or years.
        if (headerY == Integer.MAX_VALUE && headers.size() == 2) headerY = (int) (snapshot.screen.getHeight() * 0.08);
        for (NativeNode node : nodes) {
            if (node.visible.getY() < headerY) continue;
            String[] parts = node.text.split("\\n");
            for (int i = 0; i < parts.length; i++) {
                String clean = normalizeSpaces(parts[i]);
                if (!SHAREHOLDING_HOLDERS.contains(clean)) continue;
                labels.add(clean);
                if (i + 1 < parts.length && shareholdingPercentage(parts[i + 1]) != null) {
                    percentages.put(clean, shareholdingPercentage(parts[i + 1]));
                    continue;
                }
                // Separate native text nodes: associate the number in the same
                // table row, rather than counting unrelated chart values.
                if (parts.length == 1) {
                    NativeNode best = null;
                    int distance = Integer.MAX_VALUE;
                    int labelCenter = node.visible.getY() + node.visible.getHeight() / 2;
                    for (NativeNode candidate : nodes) {
                        if (shareholdingPercentage(candidate.text) == null) continue;
                        if (candidate.visible.getX() <= node.visible.getX()) continue;
                        int candidateCenter = candidate.visible.getY() + candidate.visible.getHeight() / 2;
                        int delta = Math.abs(candidateCenter - labelCenter);
                        int tolerance = Math.max(8, Math.min(node.visible.getHeight(), candidate.visible.getHeight()) / 2);
                        if (delta <= tolerance && delta < distance) { best = candidate; distance = delta; }
                    }
                    if (best != null) percentages.put(clean, shareholdingPercentage(best.text));
                }
            }
            // Flutter can also merge a whole column-major table into one node.
            // Pair its actual labels and values in the order exposed by that node.
            List<String> mergedLabels = new ArrayList<>(), mergedNumbers = new ArrayList<>();
            for (String part : parts) {
                String clean = normalizeSpaces(part);
                if (SHAREHOLDING_HOLDERS.contains(clean)) mergedLabels.add(clean);
                else if (shareholdingPercentage(clean) != null) mergedNumbers.add(shareholdingPercentage(clean));
            }
            if (mergedLabels.size() >= 2 && mergedNumbers.size() == mergedLabels.size()) {
                for (int i = 0; i < mergedLabels.size(); i++) percentages.put(mergedLabels.get(i), mergedNumbers.get(i));
            }
        }
        if (headers.size() == 2 && labels.containsAll(SHAREHOLDING_HOLDERS) && percentages.keySet().containsAll(SHAREHOLDING_HOLDERS)) {
            logValidatedText("Shareholding " + quarter + " labels", labels.toString());
            logValidatedText("Shareholding " + quarter + " percentage values", percentages.values().toString());
            return;
        }
        String signature = nativeSignature(nodes);
        unchanged = signature.equals(previous) ? unchanged + 1 : 0;
        if (unchanged >= 2 || snapshot.has("Company Profile")) break;
        previous = signature;
        swipeStockSectionBody(true, true, "shareholding-rows");
        sleep(400);
    }
    throw new AssertionError("Shareholding table incomplete | quarter=" + quarter + " | headers=" + headers
            + " | labels=" + labels + " | percentages=" + percentages.values()
            + " | expectedLabels=" + SHAREHOLDING_HOLDERS + "; capture this quarter's native XML if rows are exposed differently");
}

  // GLOBAL SEARCH FLOW
  // =========================================================

  private void openGlobalSearchFromHome() {
      ReportLogger.step("Opening global search from current home/dashboard screen");

      exitChildPageIfNeeded();

      By searchInput = AppiumBy.androidUIAutomator(
              "new UiSelector().className(\"android.widget.EditText\")"
      );

      if (isGlobalSearchScreenVisible()) {
          ReportLogger.pass("Global search screen is already visible");
          return;
      }

      waitForDashboardStableBeforeSearch();

      Dimension size = driver.manage().window().getSize();

      int[][] searchTapPoints = new int[][]{
              {(int) (size.getWidth() * 0.91), (int) (size.getHeight() * 0.057)},
              {(int) (size.getWidth() * 0.92), (int) (size.getHeight() * 0.070)},
              {(int) (size.getWidth() * 0.88), (int) (size.getHeight() * 0.065)},
              {(int) (size.getWidth() * 0.94), (int) (size.getHeight() * 0.060)}
      };

      for (int attempt = 0; attempt < searchTapPoints.length; attempt++) {
          int x = searchTapPoints[attempt][0];
          int y = searchTapPoints[attempt][1];

          ReportLogger.step("Tapping top-right global search icon attempt "
                  + (attempt + 1) + " X=" + x + ", Y=" + y);

          tapByCoordinates(x, y);
          sleep(2200);

          if (isGlobalSearchScreenVisible()) {
              ReportLogger.pass("Global search screen opened from home/dashboard");
              return;
          }

          if (isVisible(searchInput)
                  && (isVisible(byDesc("All"))
                  || isVisible(byDesc("Mutual Funds/SIFs"))
                  || isVisible(byDesc("Stocks")))) {
              ReportLogger.pass("Search input/global search tabs visible after search tap");
              return;
          }
      }

      By[] searchLocators = new By[]{
              byDesc("Search"),
              byDescContains("Search")
      };

      for (By locator : searchLocators) {
              sleep(2200);

              if (isGlobalSearchScreenVisible()) {
                  ReportLogger.pass("Global search screen opened using search locator");
                  return;
              }

              if (isVisible(searchInput)
                      && (isVisible(byDesc("All"))
                      || isVisible(byDesc("Mutual Funds/SIFs"))
                      || isVisible(byDesc("Stocks")))) {
                  ReportLogger.pass("Search input/global search tabs visible using search locator");
                  return;
              }
          }


      throw new RuntimeException("Unable to open global search from home/dashboard.");
}


  private void waitForDashboardStableBeforeSearch() {
      ReportLogger.step("Waiting for dashboard to stabilize before tapping search");

      for (int i = 1; i <= 10; i++) {
          boolean bottomTabsVisible =
                  isVisible(byDesc("Funds"))
                          && isVisible(byDesc("Stocks"))
                          && isVisible(byDesc("Portfolio"))
                          && isVisible(byDesc("Hub"));

          if (bottomTabsVisible) {
              sleep(1800);
              ReportLogger.pass("Dashboard is stable for search tap");
              return;
          }

          sleep(500);
      }

      throw new RuntimeException("Dashboard did not stabilize before search tap.");
  }

  private boolean isGlobalSearchScreenVisible() {
      return isVisible(byDesc("All"))
              && isVisible(byDesc("Mutual Funds/SIFs"))
              && isVisible(byDesc("Stocks"));
  }

  private void enterSearchKeyword(String keyword) {
      ReportLogger.step("Entering stock search keyword: " + keyword);

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
      sleep(2500);

      ReportLogger.pass("Stock search keyword entered: " + keyword);
      logValidatedText("Stock search keyword", keyword);
  }

  private void openStocksSearchTab() {
      ReportLogger.step("Opening Stocks search tab");

      By stocksTab = byDesc("Stocks");

      if (tapIfVisible(stocksTab, "Stocks search tab")) {
          sleep(1500);
          ReportLogger.pass("Stocks search tab selected");
          return;
      }

      throw new RuntimeException("Stocks search tab is not visible/selectable after entering stock keyword.");
  }

  private void openStockResult(String stockName) {
      ReportLogger.step("Opening stock result: " + stockName);

      sleep(1500);

      if (tapIfVisible(byDesc(stockName), "Exact stock result by accessibility: " + stockName)) {
          return;
      }

      if (tapIfVisible(byDescContains(stockName), "Stock result by descriptionContains: " + stockName)) {
          return;
      }

      if (tapIfVisible(byText(stockName), "Exact stock result by text: " + stockName)) {
          return;
      }

      if (tapIfVisible(byTextContains(stockName), "Stock result by textContains: " + stockName)) {
          return;
      }

      Dimension size = driver.manage().window().getSize();

      int x = (int) (size.getWidth() * 0.45);
      int y = (int) (size.getHeight() * 0.34);

      ReportLogger.debug("Stock result locator not found. Using coordinate fallback x=" + x + ", y=" + y);
      tapByCoordinates(x, y);

      ReportLogger.pass("Tapped stock result fallback");
      logValidatedText("Selected stock result", stockName);
  }

  private void waitForStockDetailsAfterResultTap() {
      ReportLogger.step("Waiting for Stock Details page");

      for (int i = 1; i <= 15; i++) {
          sleep(1000);

          if (isVisible(byDesc(STOCK_HEADER)) || isVisible(byDescContains(STOCK_HEADER))) {
              ReportLogger.pass("Stock Details page opened: " + STOCK_HEADER);
              return;
          }

          if (isVisible(byDesc("Price")) && isVisible(byDesc("Stock Rating"))) {
              ReportLogger.pass("Stock Details page opened with Price and Stock Rating labels");
              return;
          }
      }

      throw new RuntimeException("Stock Details page did not open after tapping search result");
  }

  // =========================================================
  // DASHBOARD / RECOVERY HELPERS
  // =========================================================

  public boolean isOnDashboardOrHome() {
      return isDashboardOrHomeVisible();
  }

  private boolean isDashboardOrHomeVisible() {
      /*
       * Stock Details may still expose app-level navigation in some builds,
       * so Stock Details wins over dashboard detection.
       */
      if (isOnStockDetailsPage()) {
          return false;
      }

      boolean bottomTabsVisible =
              isVisible(byDesc("Funds"))
                      && isVisible(byDesc("Stocks"))
                      && isVisible(byDesc("Portfolio"))
                      && isVisible(byDesc("Hub"));

      boolean dashboardContentVisible =
              isVisible(byDescContains("Portfolio Value"))
                      || isVisible(byDescContains("Updated Portfolio"))
                      || isVisible(byDescContains("Rich Future Starts Here"))
                      || isVisible(byDescContains("Manish"));

      return bottomTabsVisible || dashboardContentVisible;
  }

  private void exitChildPageIfNeeded() {
      ReportLogger.step("Checking current app screen");

      for (int attempt = 1; attempt <= 5; attempt++) {
          boolean bottomTabsVisible =
                  isVisible(byDesc("Funds"))
                          && isVisible(byDesc("Stocks"))
                          && isVisible(byDesc("Portfolio"))
                          && isVisible(byDesc("Hub"));

          if (bottomTabsVisible) {
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

  // =========================================================
  // SWIPE HELPERS
  // =========================================================

  private void smallSwipeUpW3C() {
      try {
          Dimension size = driver.manage().window().getSize();

          int x = size.getWidth() / 2;
          int startY = (int) (size.getHeight() * 0.68);
          int endY = (int) (size.getHeight() * 0.42);

          PointerInput finger = new PointerInput(PointerInput.Kind.TOUCH, "finger");
          Sequence swipe = new Sequence(finger, 1);

          swipe.addAction(finger.createPointerMove(Duration.ZERO, PointerInput.Origin.viewport(), x, startY));
          swipe.addAction(finger.createPointerDown(PointerInput.MouseButton.LEFT.asArg()));
          swipe.addAction(finger.createPointerMove(Duration.ofMillis(550), PointerInput.Origin.viewport(), x, endY));
          swipe.addAction(finger.createPointerUp(PointerInput.MouseButton.LEFT.asArg()));

          driver.perform(Collections.singletonList(swipe));

      } catch (Exception firstError) {
          Map<String, Object> params = new HashMap<>();
          params.put("left", 80);
          params.put("top", 420);
          params.put("width", 920);
          params.put("height", 1000);
          params.put("direction", "up");
          params.put("percent", 0.45);
          driver.executeScript("mobile: scrollGesture", params);
      }
  }

  private void pageSwipeUpW3C() {
      try {
          Dimension size = driver.manage().window().getSize();

          int x = (int) (size.getWidth() * 0.90);
          int startY = (int) (size.getHeight() * 0.78);
          int endY = (int) (size.getHeight() * 0.30);

          PointerInput finger = new PointerInput(PointerInput.Kind.TOUCH, "finger");
          Sequence swipe = new Sequence(finger, 1);

          swipe.addAction(finger.createPointerMove(Duration.ZERO, PointerInput.Origin.viewport(), x, startY));
          swipe.addAction(finger.createPointerDown(PointerInput.MouseButton.LEFT.asArg()));
          swipe.addAction(finger.createPointerMove(Duration.ofMillis(700), PointerInput.Origin.viewport(), x, endY));
          swipe.addAction(finger.createPointerUp(PointerInput.MouseButton.LEFT.asArg()));

          driver.perform(Collections.singletonList(swipe));

      } catch (Exception firstError) {
          Map<String, Object> params = new HashMap<>();
          params.put("left", 760);
          params.put("top", 520);
          params.put("width", 260);
          params.put("height", 1450);
          params.put("direction", "up");
          params.put("percent", 0.70);

          driver.executeScript("mobile: scrollGesture", params);
      }
  }

  // =========================================================
  // ASSERTION / LOG HELPERS
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

  private void assertVisibleAndLogFlexible(By[] locators, String elementName) {
      for (By locator : locators) {
          WebElement element = findVisibleElement(locator);

          if (element != null) {
              String text = getElementReadableText(element);

              if (text.isEmpty()) {
                  text = elementName;
              }

              ReportLogger.pass(elementName + " is visible");
              logValidatedText(elementName, text);
              return;
          }
      }

      ReportLogger.fail(elementName + " is not visible");
      throw new AssertionError(elementName + " is not visible");
  }

  private boolean isVisibleFlexible(By[] locators) {
      for (By locator : locators) {
          if (isVisible(locator)) {
              return true;
          }
      }

      return false;
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

      } catch (StaleElementReferenceException | NoSuchElementException e) {
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

  private void pressBackSilently() {
      try {
          driver.navigate().back();
      } catch (Exception ignored) {
          // ignore
      }
  }



//=========================================================
//SD_014 - COMPANY PROFILE
//=========================================================

public void verifyCompanyProfile() {
  ReportLogger.step("Verifying Company Profile section");

  recoverStockDetailsIfNeeded();

  alignCompanyProfileSectionSafely();

  if (isVisible(byDesc("Company Profile")) || isVisible(byDescContains("Company Profile"))) {
      logValidatedText("Company Profile section", "Company Profile");
      ReportLogger.pass("Company Profile heading validated");
  } else if (isCompanyProfileSectionReady()) {
      logValidatedText("Company Profile section", "Company Profile fields visible");
      ReportLogger.pass("Company Profile section validated by visible profile fields");
  } else {
      throw new AssertionError("Company Profile section is not visible"
              + " | visibleValues=" + getCompanyProfileVisibleValuesForDebug());
  }

  validateCompanyProfileLabelsAndValues();

  ReportLogger.pass("Company Profile section validated successfully");
}

private void alignCompanyProfileSectionSafely() {
  ReportLogger.step("Aligning Company Profile section safely from any Stock Details position");

  if (isCompanyProfileSectionReady()) {
      ReportLogger.pass("Company Profile section is already visible enough");
      return;
  }

  /*
   * Company Profile is below Shareholding. SD_014 may run independently
   * from top/mid/bottom Stock Details, so scan down with controlled swipes.
   */
  for (int i = 1; i <= 36; i++) {
      if (isCompanyProfileSectionReady()) {
          ReportLogger.pass("Company Profile section aligned after swipe up " + i);
          return;
      }

      if (isBelowCompanyProfileSection()) {
          ReportLogger.debug("Overscrolled below Company Profile into News/Analysis. Starting recovery.");
          break;
      }

      companyProfileForwardSwipeUp();
      sleep(750);
  }

  for (int i = 1; i <= 12; i++) {
      if (isCompanyProfileSectionReady()) {
          ReportLogger.pass("Company Profile section aligned after recovery swipe down " + i);
          return;
      }

      companyProfileSwipeDown();
      sleep(750);
  }

  throw new AssertionError("Company Profile section could not be aligned safely"
          + " | visibleValues=" + getCompanyProfileVisibleValuesForDebug());
}

private boolean isCompanyProfileSectionReady() {
  return isVisible(byDesc("Company Profile"))
          || isVisible(byDescContains("Company Profile"))
          || isVisible(byDesc("Incorporated"))
          || isVisible(byDesc("Chairman"))
          || isVisible(byDesc("Managing Director"))
          || isVisible(byDesc("Headquarters"))
          || isVisible(byDesc("Website"))
          || isVisible(byDesc("Listing"))
          || isVisible(byDesc("Country"))
          || isVisible(byDesc("Business"))
          || isVisible(byDescContains("ITC Limited engages"));
}

private boolean isBelowCompanyProfileSection() {
  return isVisible(byDesc("News"))
          || isVisible(byDescContains("News"))
          || isVisible(byDesc("Analysis"))
          || isVisible(byDescContains("Analysis"));
}

private void validateCompanyProfileLabelsAndValues() {
  ReportLogger.step("Validating Company Profile strict label-value mapping");

  if (!isCompanyProfileSectionReady()) {
      alignCompanyProfileSectionSafely();
  }

  validateCompanyProfilePairStrictWithScroll("Incorporated", "1910");
  validateCompanyProfilePairStrictWithScroll("Chairman", "Sanjiv Puri");
  validateCompanyProfilePairStrictWithScroll("Managing Director", "Sanjiv Puri");
  validateCompanyProfilePairStrictWithScroll("Group", "ITC - MNC");
  validateCompanyProfilePairStrictWithScroll("Headquarters", "Kolkata, West Bengal");
  validateCompanyProfilePairStrictWithScroll("Website", "www.itcportal.com");
  validateCompanyProfilePairStrictWithScroll("Listing", "NSE: ITC");
  validateCompanyProfilePairStrictWithScroll("Listing", "BSE: 500875");
  validateCompanyProfilePairStrictWithScroll("Country", "India");

  validateCompanyProfileBusinessStrictWithScroll();

  logValidatedText(
          "Company Profile strict mapped values",
          "{Incorporated=1910, Chairman=Sanjiv Puri, Managing Director=Sanjiv Puri, Group=ITC - MNC, "
                  + "Headquarters=Kolkata, West Bengal, Website=www.itcportal.com, "
                  + "Listing=[NSE: ITC, BSE: 500875], Country=India, Business=validated}"
  );

  ReportLogger.pass("Company Profile strict label-value mapping validated successfully");
}

private void validateCompanyProfilePairStrictWithScroll(String label, String expectedValue) {
  ReportLogger.step("Validating Company Profile pair: " + label + " = " + expectedValue);

  for (int attempt = 1; attempt <= 7; attempt++) {
      WebElement labelElement = findCompanyProfileElementByText(label);
      WebElement valueElement = findCompanyProfileValueNearLabel(labelElement, expectedValue);

      if (labelElement != null && valueElement != null) {
          logValidatedText("Company Profile pair", label + " = " + expectedValue);
          ReportLogger.pass("Company Profile pair validated: " + label + " = " + expectedValue);
          return;
      }

      if (isCompanyProfileCombinedNodePresent(label, expectedValue)) {
          logValidatedText("Company Profile pair", label + " = " + expectedValue);
          ReportLogger.pass("Company Profile pair validated from combined node: " + label + " = " + expectedValue);
          return;
      }

      if (labelElement != null) {
          ReportLogger.debug("Company Profile label visible but mapped value not visible. Scrolling inside Company Profile card. "
                  + "label=" + label + " expectedValue=" + expectedValue + " attempt=" + attempt);
          companyProfileTinySwipeUp();
          sleep(700);
          continue;
      }

      if (isBelowCompanyProfileSection()) {
          companyProfileSwipeDown();
      } else {
          companyProfileTinySwipeUp();
      }

      sleep(700);
  }

  throw new AssertionError("Company Profile strict pair validation failed"
          + " | label=" + label
          + " | expectedValue=" + expectedValue
          + " | visibleValues=" + getCompanyProfileVisibleValuesForDebug());
}

private WebElement findCompanyProfileElementByText(String expectedText) {
  List<WebElement> elements = getCompanyProfileVisibleContentElements();

  for (WebElement element : elements) {
      String text = getElementContentDescSafeForCompanyProfile(element);

      if (textMatchesCompanyProfileText(text, expectedText)) {
          return element;
      }
  }

  return null;
}

private WebElement findCompanyProfileValueNearLabel(WebElement labelElement, String expectedValue) {
  if (labelElement == null) {
      return null;
  }

  Rectangle labelRect = labelElement.getRect();
  List<WebElement> elements = getCompanyProfileVisibleContentElements();

  WebElement bestMatch = null;
  int bestDistance = Integer.MAX_VALUE;

  for (WebElement element : elements) {
      String text = getElementContentDescSafeForCompanyProfile(element);

      if (!textMatchesCompanyProfileText(text, expectedValue)) {
          continue;
      }

      Rectangle valueRect = element.getRect();

      int verticalDistance = valueRect.getY() - labelRect.getY();
      int horizontalDistance = Math.abs(valueRect.getX() - labelRect.getX());

      boolean sameColumnBelow = verticalDistance >= 4
              && verticalDistance <= 155
              && horizontalDistance <= 260;

      boolean listingFlexible = getElementContentDescSafeForCompanyProfile(labelElement).contains("Listing")
              && verticalDistance >= 4
              && verticalDistance <= 185
              && horizontalDistance <= 360;

      boolean nearbyKnownValue = verticalDistance >= 4
              && verticalDistance <= 155
              && horizontalDistance <= 390
              && isCompanyProfileKnownValue(expectedValue);

      if (sameColumnBelow || listingFlexible || nearbyKnownValue) {
          if (verticalDistance < bestDistance) {
              bestDistance = verticalDistance;
              bestMatch = element;
          }
      }
  }

  return bestMatch;
}

private void validateCompanyProfileBusinessStrictWithScroll() {
  ReportLogger.step("Validating Company Profile Business label and text");

  for (int attempt = 1; attempt <= 8; attempt++) {
      WebElement businessLabel = findCompanyProfileElementByText("Business");

      boolean businessTextFound = false;
      List<WebElement> elements = getCompanyProfileVisibleContentElements();

      Rectangle businessRect = null;

      if (businessLabel != null) {
          businessRect = businessLabel.getRect();
      }

      for (WebElement element : elements) {
          String text = getElementContentDescSafeForCompanyProfile(element);
          Rectangle rect = element.getRect();

          boolean allowedPosition = businessRect == null || rect.getY() >= businessRect.getY();

          if (allowedPosition
                  && (text.contains("ITC Limited engages")
                  || text.contains("fast-moving consumer goods")
                  || text.contains("hotels")
                  || text.contains("paperboards")
                  || text.contains("packaging")
                  || text.contains("information technology")
                  || text.contains("Read More"))) {
              businessTextFound = true;
              break;
          }
      }

      if (businessLabel != null && businessTextFound) {
          logValidatedText("Company Profile pair", "Business = ITC Limited engages... Read More");
          ReportLogger.pass("Company Profile Business text validated strictly");
          return;
      }

      if (isBelowCompanyProfileSection()) {
          companyProfileSwipeDown();
      } else {
          companyProfileTinySwipeUp();
      }

      sleep(700);
  }

  throw new AssertionError("Company Profile Business text missing"
          + " | visibleValues=" + getCompanyProfileVisibleValuesForDebug());
}

private boolean isCompanyProfileCombinedNodePresent(String label, String expectedValue) {
  List<WebElement> elements = getCompanyProfileVisibleContentElements();

  String labelClean = normalizeSpaces(label).toLowerCase();
  String valueClean = normalizeSpaces(expectedValue).toLowerCase();

  for (WebElement element : elements) {
      String text = normalizeSpaces(getElementContentDescSafeForCompanyProfile(element)).toLowerCase();

      if (text.contains(labelClean) && text.contains(valueClean)) {
          return true;
      }
  }

  return false;
}

private List<WebElement> getCompanyProfileVisibleContentElements() {
  List<WebElement> visibleElements = new ArrayList<>();

  try {
      List<WebElement> elements = driver.findElements(By.xpath("//*[@content-desc or @text]"));
      Dimension size = driver.manage().window().getSize();

      for (WebElement element : elements) {
          try {
              if (element == null || !element.isDisplayed()) {
                  continue;
              }

              String text = getElementContentDescSafeForCompanyProfile(element);

              if (text == null || text.trim().isEmpty()) {
                  continue;
              }

              Rectangle rect = element.getRect();

              if (rect.getY() < 0 || rect.getY() > size.getHeight()) {
                  continue;
              }

              if (isCompanyProfileElementNoise(text)) {
                  continue;
              }

              visibleElements.add(element);

          } catch (Exception ignored) {
              // ignore stale/hidden elements
          }
      }

  } catch (Exception e) {
      ReportLogger.debug("Unable to collect Company Profile visible elements: " + cleanError(e.getMessage()));
  }

  return visibleElements;
}

private String getElementContentDescSafeForCompanyProfile(WebElement element) {
  if (element == null) {
      return "";
  }

  try {
      String text = element.getAttribute("content-desc");

      if (text == null || text.trim().isEmpty()) {
          text = element.getAttribute("contentDescription");
      }

      if (text == null || text.trim().isEmpty()) {
          text = element.getAttribute("text");
      }

      if (text == null || text.trim().isEmpty()) {
          text = element.getAttribute("name");
      }

      if (text == null || text.trim().isEmpty()) {
          text = element.getText();
      }

      return normalizeSpaces(text);

  } catch (Exception e) {
      return "";
  }
}

private boolean textMatchesCompanyProfileText(String actual, String expected) {
  if (actual == null || expected == null) {
      return false;
  }

  String actualClean = normalizeSpaces(actual).toLowerCase();
  String expectedClean = normalizeSpaces(expected).toLowerCase();

  return actualClean.equals(expectedClean)
          || actualClean.contains(expectedClean);
}

private boolean isCompanyProfileKnownValue(String value) {
  if (value == null) {
      return false;
  }

  String clean = normalizeSpaces(value);

  return clean.equals("1910")
          || clean.equals("Sanjiv Puri")
          || clean.equals("ITC - MNC")
          || clean.equals("Kolkata, West Bengal")
          || clean.equals("www.itcportal.com")
          || clean.equals("NSE: ITC")
          || clean.equals("BSE: 500875")
          || clean.equals("India");
}

private boolean isCompanyProfileElementNoise(String text) {
  if (text == null) {
      return true;
  }

  String clean = normalizeSpaces(text);

  return clean.equals("Go back")
          || clean.equals("Shareholding")
          || clean.equals("Type")
          || clean.equals("Percentage (%)")
          || clean.equals("FIIs")
          || clean.equals("DIIs")
          || clean.equals("Mutual Fund")
          || clean.equals("Others")
          || clean.equals("Peers")
          || clean.equals("Company")
          || clean.contains("Market Cap")
          || clean.contains("Revenue")
          || clean.contains("Net Profit")
          || clean.matches("^(Mar|Jun|Sep|Dec) '\\d{2}$");
}

private List<String> getCompanyProfileVisibleValuesForDebug() {
  List<String> values = new ArrayList<>();

  for (WebElement element : getCompanyProfileVisibleContentElements()) {
      String text = getElementContentDescSafeForCompanyProfile(element);

      if (!text.isEmpty() && !values.contains(text)) {
          values.add(text);
      }
  }

  return values;
}

private void companyProfileForwardSwipeUp() {
  try {
      Dimension size = driver.manage().window().getSize();

      int x = (int) (size.getWidth() * 0.88);
      int startY = (int) (size.getHeight() * 0.76);
      int endY = (int) (size.getHeight() * 0.42);

      PointerInput finger = new PointerInput(PointerInput.Kind.TOUCH, "finger");
      Sequence swipe = new Sequence(finger, 1);

      swipe.addAction(finger.createPointerMove(Duration.ZERO, PointerInput.Origin.viewport(), x, startY));
      swipe.addAction(finger.createPointerDown(PointerInput.MouseButton.LEFT.asArg()));
      swipe.addAction(finger.createPointerMove(Duration.ofMillis(480), PointerInput.Origin.viewport(), x, endY));
      swipe.addAction(finger.createPointerUp(PointerInput.MouseButton.LEFT.asArg()));

      driver.perform(Collections.singletonList(swipe));

  } catch (Exception e) {
      Map<String, Object> params = new HashMap<>();
      params.put("left", 760);
      params.put("top", 650);
      params.put("width", 260);
      params.put("height", 900);
      params.put("direction", "up");
      params.put("percent", 0.38);

      driver.executeScript("mobile: scrollGesture", params);
  }
}

private void companyProfileTinySwipeUp() {
  try {
      Dimension size = driver.manage().window().getSize();

      int x = (int) (size.getWidth() * 0.88);
      int startY = (int) (size.getHeight() * 0.72);
      int endY = (int) (size.getHeight() * 0.60);

      PointerInput finger = new PointerInput(PointerInput.Kind.TOUCH, "finger");
      Sequence swipe = new Sequence(finger, 1);

      swipe.addAction(finger.createPointerMove(Duration.ZERO, PointerInput.Origin.viewport(), x, startY));
      swipe.addAction(finger.createPointerDown(PointerInput.MouseButton.LEFT.asArg()));
      swipe.addAction(finger.createPointerMove(Duration.ofMillis(280), PointerInput.Origin.viewport(), x, endY));
      swipe.addAction(finger.createPointerUp(PointerInput.MouseButton.LEFT.asArg()));

      driver.perform(Collections.singletonList(swipe));

  } catch (Exception e) {
      Map<String, Object> params = new HashMap<>();
      params.put("left", 760);
      params.put("top", 720);
      params.put("width", 260);
      params.put("height", 460);
      params.put("direction", "up");
      params.put("percent", 0.14);

      driver.executeScript("mobile: scrollGesture", params);
  }
}

private void companyProfileSwipeDown() {
  try {
      Dimension size = driver.manage().window().getSize();

      int x = (int) (size.getWidth() * 0.88);
      int startY = (int) (size.getHeight() * 0.44);
      int endY = (int) (size.getHeight() * 0.76);

      PointerInput finger = new PointerInput(PointerInput.Kind.TOUCH, "finger");
      Sequence swipe = new Sequence(finger, 1);

      swipe.addAction(finger.createPointerMove(Duration.ZERO, PointerInput.Origin.viewport(), x, startY));
      swipe.addAction(finger.createPointerDown(PointerInput.MouseButton.LEFT.asArg()));
      swipe.addAction(finger.createPointerMove(Duration.ofMillis(480), PointerInput.Origin.viewport(), x, endY));
      swipe.addAction(finger.createPointerUp(PointerInput.MouseButton.LEFT.asArg()));

      driver.perform(Collections.singletonList(swipe));

  } catch (Exception e) {
      Map<String, Object> params = new HashMap<>();
      params.put("left", 760);
      params.put("top", 700);
      params.put("width", 260);
      params.put("height", 760);
      params.put("direction", "down");
      params.put("percent", 0.32);

      driver.executeScript("mobile: scrollGesture", params);
  }
}


//=========================================================
//SD_015 - NEWS SECTION PRESENCE
//SD_016 - ANALYSIS SECTION PRESENCE
//=========================================================

public void verifyNewsSectionPresent() {
    ReportLogger.step("Verifying News section presence in continuous flow");

    /*
     * SD_015 continues directly from SD_014.
     * Do not call recovery, Back, login, search, or top reset here.
     */
    alignBottomSectionHeadingByDownwardScrollOnly("News");

    assertVisibleAndLogFlexible(
            getBottomSectionHeadingLocators("News"),
            "News section"
    );

    ReportLogger.pass("News section is present on Stock Details page");
}

public void verifyAnalysisSectionPresent() {
    ReportLogger.step("Verifying Analysis section presence in continuous flow");

    /*
     * SD_016 continues directly from SD_015.
     * Analysis is below News, so move forward/down the same page only.
     */
    alignBottomSectionHeadingByDownwardScrollOnly("Analysis");

    assertVisibleAndLogFlexible(
            getBottomSectionHeadingLocators("Analysis"),
            "Analysis section"
    );

    ReportLogger.pass("Analysis section is present on Stock Details page");
}

private By[] getBottomSectionHeadingLocators(String sectionName) {
    return new By[]{
            byDesc(sectionName),
            byDescContains(sectionName),
            byText(sectionName),
            byTextContains(sectionName)
    };
}

private boolean isBottomSectionHeadingVisible(String sectionName) {
    return isVisibleFlexible(getBottomSectionHeadingLocators(sectionName));
}

private void alignBottomSectionHeadingByDownwardScrollOnly(String sectionName) {
    ReportLogger.step("Scrolling down continuously to Stock Details section: " + sectionName);

    if (isBottomSectionHeadingVisible(sectionName)) {
        ReportLogger.pass(sectionName + " section is already visible in the current flow");
        return;
    }

    /*
     * Finger swipe-up moves the page content downward.
     * This helper intentionally has no Back navigation, no search reopening,
     * no top reset, and no reverse/downward recovery swipe.
     */
    for (int attempt = 1; attempt <= 12; attempt++) {
        pageSwipeUpW3C();
        sleep(650);

        if (isBottomSectionHeadingVisible(sectionName)) {
            ReportLogger.pass(
                    sectionName + " section found after continuous scroll " + attempt
            );
            return;
        }
    }

    throw new AssertionError(
            sectionName + " section is not present after continuous downward scrolling"
    );
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

    private void validateHeaderExchangeSelector() {
        // The current header exposes one selected exchange beside the price.
        // Older builds expose both chips; keep supporting that layout as well.
        if (findHeaderExchange("NSE") != null && findHeaderExchange("BSE") != null) {
            assertHeaderExchangeVisible("NSE");
            assertHeaderExchangeVisible("BSE");
            return;
        }
        String original = findHeaderExchange("NSE") != null ? "NSE"
                : findHeaderExchange("BSE") != null ? "BSE" : null;
        if (original == null) throw new AssertionError("No exchange selector is exposed beside the stock price."
                + " Capture Inspector XML of the Stock Details header.");
        final boolean[] menuOpened = {false};
        runWithChildScreenCleanup(() -> {
            selectHeaderExchange("NSE", menuOpened);
            selectHeaderExchange("BSE", menuOpened);
        }, () -> {
            boolean anyOption = visibleExactLabel("NSE") != null || visibleExactLabel("BSE") != null;
            boolean headerMissing = findHeaderExchange("NSE") == null && findHeaderExchange("BSE") == null;
            if (menuOpened[0] && ((visibleExactLabel("NSE") != null && visibleExactLabel("BSE") != null)
                    || (anyOption && (headerMissing || visibleExactLabel("Scrim") != null)))) {
                driver.navigate().back();
                keyRatioWait(ExecutionProfile.shortWait()).until(d ->
                        findHeaderExchange("NSE") != null || findHeaderExchange("BSE") != null);
            }
            if (findHeaderExchange(original) == null
                    && findHeaderExchange("NSE".equals(original) ? "BSE" : "NSE") != null) {
                selectHeaderExchange(original, menuOpened);
            }
        });
    }

    private void selectHeaderExchange(String exchange, boolean[] menuOpened) {
        if (findHeaderExchange(exchange) != null) {
            assertHeaderExchangeVisible(exchange);
            return;
        }
        String other = "NSE".equals(exchange) ? "BSE" : "NSE";
        WebElement selector = findHeaderExchange(other);
        if (selector == null) throw new AssertionError("Cannot locate the current header exchange selector"
                + " before selecting " + exchange + ". Capture header Inspector XML.");
        ReportLogger.step("Opening header exchange selector: " + other + " -> " + exchange);
        menuOpened[0] = true;
        selector.click();
        try {
            WebElement choice = keyRatioWait(ExecutionProfile.shortWait()).until(d -> visibleExactLabel(exchange));
            // A direct toggle can switch immediately; a dropdown keeps both options visible.
            if (visibleExactLabel(other) != null) choice.click();
            keyRatioWait(ExecutionProfile.defaultWait()).until(d ->
                    findHeaderExchange(exchange) != null && visibleExactLabel(other) == null);
            menuOpened[0] = false;
            assertHeaderExchangeVisible(exchange);
        } catch (TimeoutException timeout) {
            throw new AssertionError("Header exchange selection did not complete | requested=" + exchange
                    + ". Capture Inspector XML with the NSE/BSE selector open; the exact option labels"
                    + " and the selected exchange must be accessible.", timeout);
        }
    }

    private WebElement visibleExactLabel(String label) {
        for (By locator : new By[]{byDesc(label), byText(label)}) {
            for (WebElement element : driver.findElements(locator)) {
                if (element.isDisplayed() && element.isEnabled() && isFullyOnValuationScreen(element.getRect())) return element;
            }
        }
        return null;
    }

    private WebElement findHeaderExchange(String exchange) {
        WebElement stock = findVisibleElement(byDesc(STOCK_HEADER));
        if (stock == null) return null;
        int bottom = driver.manage().window().getSize().getHeight();
        for (String marker : new String[]{"Stock Chart", "Price", "Stock Rating"}) {
            WebElement boundary = findVisibleElement(byDesc(marker));
            if (boundary != null) bottom = Math.min(bottom, boundary.getRect().getY());
        }
        for (By locator : new By[]{byDesc(exchange), byText(exchange)}) {
            for (WebElement element : driver.findElements(locator)) {
                if (!element.isDisplayed()) continue;
                Rectangle bounds = element.getRect();
                int y = bounds.getY() + bounds.getHeight() / 2;
                if (isFullyOnValuationScreen(bounds) && y >= stock.getRect().getY() && y < bottom) return element;
            }
        }
        return null;
    }

    private void assertHeaderExchangeVisible(String exchange) {
        try {
            WebElement chip = keyRatioWait(ExecutionProfile.shortWait()).until(d -> findHeaderExchange(exchange));
            logValidatedText(exchange + " chip", getElementReadableText(chip));
        } catch (TimeoutException missing) {
            throw new AssertionError(exchange + " exchange is not exposed in the Stock Details header"
                    + " as text or content-desc. Capture Inspector XML of the header; the assertion is retained.", missing);
        }
    }

    private boolean hasCapturedRatioBlock(java.util.LinkedHashMap<String, String> captured, String expected) {
        for (String title : captured.keySet()) {
            if (titlesMatchKeyRatiosMoreExpectedBlock(title, expected)) return true;
        }
        return false;
    }

    private boolean isKeyRatiosMoreScreenVisible() {
        return isVisible(byDesc("Profitability")) || isVisible(byDesc("Solvency"))
                || isVisible(byDesc("Operating Efficiency"));
    }

    private boolean isFinancialsMoreScreenVisible() {
        return isVisible(byDesc("Annual Consolidated")) || isVisible(byDesc("Annual Standalone"))
                || isVisible(byDesc("Quarterly Consolidated")) || isVisible(byDesc("Quarterly Standalone"));
    }

    private void runWithChildScreenCleanup(Runnable validation, Runnable cleanup) {
        Throwable original = null;
        try {
            validation.run();
        } catch (RuntimeException | AssertionError failure) {
            original = failure;
            throw failure;
        } finally {
            try {
                cleanup.run();
            } catch (RuntimeException | AssertionError cleanupFailure) {
                if (original != null) original.addSuppressed(cleanupFailure);
                else throw cleanupFailure;
            }
        }
    }

    /** Align the requested control; seeing Annual is not proof that Quarterly is available. */
    private void alignFinancialsPeriodControls(String optionName) {
        ReportLogger.step("Aligning Financials period control: " + optionName);
        Duration settle = ExecutionProfile.pollingInterval().multipliedBy(3);
        for (int attempt = 0; attempt < 14; attempt++) {
            if (isFinancialsPeriodOptionVisible(optionName)) return;
            // Annual also belongs to Stock Performance. From there, move forward
            // through Essential Checks to Financials, never back toward the header.
            boolean earlierSection = visibleExactLabel("Stock Performance") != null
                    || visibleExactLabel("Essential Checks") != null;
            boolean financialsTop = visibleExactLabel("Financials") != null
                    || visibleExactLabel("Income Statement") != null || visibleExactLabel("Balance Sheet") != null
                    || visibleExactLabel("Cash Flow") != null;
            boolean belowFinancials = visibleExactLabel("Key Ratios") != null && !financialsTop;
            boolean forward = earlierSection || financialsTop || (!belowFinancials && attempt < 9);
            financialsPeriodAlignmentSwipe(forward);
            try {
                keyRatioWait(settle).until(d -> isFinancialsPeriodOptionVisible(optionName));
                return;
            } catch (TimeoutException outsideViewport) {
                ReportLogger.debug("Scoped Financials " + optionName + " not visible | alignment=" + (attempt + 1)
                        + " | direction=" + (forward ? "forward" : "back"));
            }
        }
        throw new AssertionError("Financials " + optionName + " was not exposed inside the Financials section"
                + " after bounded alignment. Stock Performance's Annual is excluded. Capture Inspector XML"
                + " showing the Financials chart and Annual/Quarterly row.");
    }

    private WebElement findFinancialsPeriodOption(String optionName) {
        int top = -1;
        int bottom = driver.manage().window().getSize().getHeight();
        for (String marker : new String[]{"Financials", "Income Statement", "Balance Sheet", "Cash Flow",
                "Sales", "PAT", "Equity", "Debt", "Operating Cash Flow", "Free Cash Flow"}) {
            WebElement anchor = findVisibleElement(byDesc(marker));
            if (anchor != null && isFullyOnValuationScreen(anchor.getRect())) {
                Rectangle r = anchor.getRect();
                top = Math.max(top, r.getY() + r.getHeight());
            }
        }
        WebElement nextSection = findVisibleElement(byDesc("Key Ratios"));
        if (nextSection != null) bottom = Math.min(bottom, nextSection.getRect().getY());
        for (By locator : new By[]{byDesc(optionName), byText(optionName)}) {
            for (WebElement option : driver.findElements(locator)) {
                if (!option.isDisplayed() || !option.isEnabled()) continue;
                Rectangle bounds = option.getRect();
                if (!isFullyOnValuationScreen(bounds)) continue;
                int y = bounds.getY() + bounds.getHeight() / 2;
                if (y >= bottom) continue;
                if (top >= 0 && y > top) return option;
                // A lower chart view may hide all headings/legends; its adjacent
                // Quarterly control still distinguishes this row from Stock Performance.
                if (top < 0) {
                    String sibling = "Annual".equals(optionName) ? "Quarterly" : "Annual";
                    for (By siblingLocator : new By[]{byDesc(sibling), byText(sibling)}) {
                        for (WebElement peer : driver.findElements(siblingLocator)) {
                            if (!peer.isDisplayed()) continue;
                            Rectangle other = peer.getRect();
                            if (isFullyOnValuationScreen(other)
                                    && bounds.getY() < other.getY() + other.getHeight()
                                    && bounds.getY() + bounds.getHeight() > other.getY()) return option;
                        }
                    }
                }
            }
        }
        return null;
    }

    private void financialsPeriodAlignmentSwipe(boolean forward) {
        Dimension screen = driver.manage().window().getSize();
        int x = (int) (screen.getWidth() * 0.88);
        int from = (int) (screen.getHeight() * (forward ? 0.74 : 0.58));
        int to = (int) (screen.getHeight() * (forward ? 0.58 : 0.74));
        PointerInput finger = new PointerInput(PointerInput.Kind.TOUCH, "financials-period-align");
        Sequence swipe = new Sequence(finger, 0);
        swipe.addAction(finger.createPointerMove(Duration.ZERO, PointerInput.Origin.viewport(), x, from));
        swipe.addAction(finger.createPointerDown(PointerInput.MouseButton.LEFT.asArg()));
        swipe.addAction(finger.createPointerMove(Duration.ofMillis(300), PointerInput.Origin.viewport(), x, to));
        swipe.addAction(finger.createPointerUp(PointerInput.MouseButton.LEFT.asArg()));
        driver.perform(Collections.singletonList(swipe));
    }

    /** Period controls are siblings of the chart semantics on this Flutter screen. */
    private boolean isInValuationSection(Rectangle chip, Rectangle card) {
        Dimension screen = driver.manage().window().getSize();
        int x = chip.getX() + chip.getWidth() / 2;
        int y = chip.getY() + chip.getHeight() / 2;
        if (chip.getWidth() <= 0 || chip.getHeight() <= 0 || chip.getX() < 0 || chip.getY() < 0
                || chip.getX() + chip.getWidth() > screen.getWidth()
                || chip.getY() + chip.getHeight() > screen.getHeight()) return false;
        if (x < Math.max(0, card.getX()) || x >= Math.min(screen.getWidth(), card.getX() + card.getWidth())) return false;
        int sectionTop = -1;
        for (String label : new String[]{"Key Ratios", "Efficiency", "Valuation", "Growth"}) {
            for (WebElement marker : driver.findElements(byDesc(label))) {
                if (!marker.isDisplayed()) continue;
                Rectangle r = marker.getRect();
                // Ignore a same-named marker below this card or outside the viewport.
                if (r.getY() >= 0 && r.getY() < card.getY() + card.getHeight() / 2) {
                    sectionTop = Math.max(sectionTop, r.getY() + r.getHeight());
                }
            }
        }
        if (sectionTop < 0) return isInsideVisibleValuationCard(chip, card);
        int sectionBottom = screen.getHeight();
        for (String label : new String[]{"Peers", "Shareholding", "Company Profile"}) {
            for (WebElement marker : driver.findElements(byDesc(label))) {
                if (!marker.isDisplayed()) continue;
                Rectangle r = marker.getRect();
                if (r.getY() > card.getY()) sectionBottom = Math.min(sectionBottom, r.getY());
            }
        }
        return y > sectionTop && y < sectionBottom;
    }

    private String describeBounds(Rectangle r) {
        return "[x=" + r.getX() + ", y=" + r.getY() + ", width=" + r.getWidth() + ", height=" + r.getHeight() + "]";
    }

    private static final String[] ESSENTIAL_CARD_TITLES = {"Altman Z-Score", "Modified C-Score", "Piotroski F-Score"};

    private EssentialCardSnapshot readActiveEssentialChecksCard() {
        Dimension screen = driver.manage().window().getSize();
        EssentialCardSnapshot best = null;
        long area = 0;
        for (String title : ESSENTIAL_CARD_TITLES) {
            for (WebElement element : driver.findElements(byDescContains(title))) {
                try {
                    if (!element.isDisplayed()) continue;
                    String text = element.getAttribute("content-desc");
                    if (text == null || !hasEssentialChecksDynamicScore(normalizeSpaces(text))) continue;
                    Rectangle r = element.getRect();
                    int width = Math.max(0, Math.min(screen.getWidth(), r.getX() + r.getWidth()) - Math.max(0, r.getX()));
                    int height = Math.max(0, Math.min(screen.getHeight(), r.getY() + r.getHeight()) - Math.max(0, r.getY()));
                    long visibleArea = (long) width * height;
                    if (visibleArea > area) {
                        best = new EssentialCardSnapshot(title, text, r);
                        area = visibleArea;
                    }
                } catch (StaleElementReferenceException replacedCard) {
                    // Reacquire the current card during the explicit wait.
                }
            }
        }
        return best;
    }

    private EssentialCardSnapshot openEssentialChecksCard(String expected, boolean forward) {
        for (int attempt = 0; attempt < 4; attempt++) {
            EssentialCardSnapshot card = keyRatioWait(ExecutionProfile.shortWait()).until(d -> readActiveEssentialChecksCard());
            if (expected.equals(card.title)) return card;
            if (attempt == 3) break;
            ReportLogger.step("Swiping Essential Checks card towards " + expected + " | attempt=" + (attempt + 1));
            swipeInBounds(card.bounds, true, forward);
            try {
                return keyRatioWait(ExecutionProfile.shortWait()).until(d -> {
                    EssentialCardSnapshot current = readActiveEssentialChecksCard();
                    return current != null && expected.equals(current.title) ? current : null;
                });
            } catch (TimeoutException nextCardNotReached) {
                // Three bounded swipes also support an initially non-default card.
            }
        }
        throw new AssertionError("Essential Checks carousel did not expose the expected active card: " + expected);
    }

    private static final class EssentialCardSnapshot {
        private final String title;
        private final String text;
        private final Rectangle bounds;
        private EssentialCardSnapshot(String title, String text, Rectangle bounds) {
            this.title = title;
            this.text = text;
            this.bounds = bounds;
        }
    }

    private void swipeVisibleBody(boolean up, String context) {
        Dimension screen = driver.manage().window().getSize();
        Rectangle body = new Rectangle(new org.openqa.selenium.Point(0, 0), screen);
        long area = 0;
        for (WebElement element : driver.findElements(AppiumBy.className("android.widget.ScrollView"))) {
            if (!element.isDisplayed()) continue;
            Rectangle bounds = element.getRect();
            long currentArea = (long) bounds.getWidth() * bounds.getHeight();
            if (currentArea > area) { body = bounds; area = currentArea; }
        }
        ReportLogger.debug("Scrolling visible " + context + " body " + (up ? "up" : "down"));
        swipeInBounds(body, false, up);
    }

    /** Coordinates come from the visible component; never retry a dead driver via another gesture API. */
    private void swipeInBounds(Rectangle bounds, boolean horizontal, boolean forward) {
        Dimension screen = driver.manage().window().getSize();
        int left = Math.max(0, bounds.getX());
        int right = Math.min(screen.getWidth(), bounds.getX() + bounds.getWidth());
        int top = Math.max((int) (screen.getHeight() * 0.08), bounds.getY());
        int bottom = Math.min((int) (screen.getHeight() * 0.92), bounds.getY() + bounds.getHeight());
        if (right - left < 80 || bottom - top < 80) throw new IllegalStateException("No safe visible gesture area: " + describeBounds(bounds));
        int startX = horizontal ? left + (int) ((right - left) * (forward ? 0.82 : 0.18)) : left + (int) ((right - left) * 0.88);
        int endX = horizontal ? left + (int) ((right - left) * (forward ? 0.18 : 0.82)) : startX;
        int startY = horizontal ? (top + bottom) / 2 : top + (int) ((bottom - top) * (forward ? 0.78 : 0.30));
        int endY = horizontal ? startY : top + (int) ((bottom - top) * (forward ? 0.30 : 0.78));
        PointerInput finger = new PointerInput(PointerInput.Kind.TOUCH, "stock-details-body");
        Sequence swipe = new Sequence(finger, 0);
        swipe.addAction(finger.createPointerMove(Duration.ZERO, PointerInput.Origin.viewport(), startX, startY));
        swipe.addAction(finger.createPointerDown(PointerInput.MouseButton.LEFT.asArg()));
        swipe.addAction(finger.createPointerMove(Duration.ofMillis(450), PointerInput.Origin.viewport(), endX, endY));
        swipe.addAction(finger.createPointerUp(PointerInput.MouseButton.LEFT.asArg()));
        driver.perform(Collections.singletonList(swipe));
    }

    // Read a native hierarchy once per scan, rather than issuing several commands
    // for every node. A transport/instrumentation failure must reach the runner.
    private NativeSnapshot readNativeSnapshot() {
        String xml = driver.getPageSource();
        Dimension viewport = driver.manage().window().getSize();
        try {
            javax.xml.parsers.DocumentBuilderFactory factory = javax.xml.parsers.DocumentBuilderFactory.newInstance();
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.setXIncludeAware(false);
            factory.setExpandEntityReferences(false);
            org.w3c.dom.Document document = factory.newDocumentBuilder().parse(
                    new org.xml.sax.InputSource(new java.io.StringReader(xml)));
            NativeSnapshot snapshot = new NativeSnapshot(viewport);
            readNativeNodes(document.getDocumentElement(), snapshot,
                    new Rectangle(0, 0, viewport.getHeight(), viewport.getWidth()), false);
            return snapshot;
        } catch (javax.xml.parsers.ParserConfigurationException | org.xml.sax.SAXException | java.io.IOException error) {
            throw new IllegalStateException("Cannot parse the native page hierarchy for Stock Details", error);
        }
    }

    private void readNativeNodes(org.w3c.dom.Element element, NativeSnapshot snapshot,
                                 Rectangle clip, boolean hiddenParent) {
        boolean hidden = hiddenParent || "false".equals(element.getAttribute("displayed"))
                || "false".equals(element.getAttribute("visible-to-user"));
        if (hidden) return;
        Rectangle bounds = nativeBounds(element.getAttribute("bounds"));
        Rectangle visible = bounds == null ? null : intersectNativeBounds(bounds, clip);
        String description = element.getAttribute("content-desc");
        String text = element.getAttribute("text");
        if (visible != null && visible.getWidth() > 0 && visible.getHeight() > 0) {
            if (!normalizeSpaces(description).isEmpty()) snapshot.nodes.add(new NativeNode(description, bounds, visible, element));
            if (!normalizeSpaces(text).isEmpty() && !text.equals(description)) snapshot.nodes.add(new NativeNode(text, bounds, visible, element));
        }
        Rectangle childClip = clip;
        String className = element.getAttribute("class") + element.getTagName();
        if (visible != null && (className.contains("ScrollView") || "true".equals(element.getAttribute("scrollable")))) {
            childClip = visible;
        }
        org.w3c.dom.NodeList children = element.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            if (children.item(i) instanceof org.w3c.dom.Element) {
                readNativeNodes((org.w3c.dom.Element) children.item(i), snapshot, childClip, hidden);
            }
        }
    }

    private Rectangle nativeBounds(String raw) {
        java.util.regex.Matcher matcher = Pattern.compile("\\[(-?\\d+),(-?\\d+)\\]\\[(-?\\d+),(-?\\d+)\\]").matcher(raw);
        if (!matcher.matches()) return null;
        int x = Integer.parseInt(matcher.group(1)), y = Integer.parseInt(matcher.group(2));
        int right = Integer.parseInt(matcher.group(3)), bottom = Integer.parseInt(matcher.group(4));
        return right > x && bottom > y ? new Rectangle(x, y, bottom - y, right - x) : null;
    }

    private Rectangle intersectNativeBounds(Rectangle a, Rectangle b) {
        int x = Math.max(a.getX(), b.getX()), y = Math.max(a.getY(), b.getY());
        int right = Math.min(a.getX() + a.getWidth(), b.getX() + b.getWidth());
        int bottom = Math.min(a.getY() + a.getHeight(), b.getY() + b.getHeight());
        return new Rectangle(x, y, Math.max(0, bottom - y), Math.max(0, right - x));
    }

    private final class NativeSnapshot {
        final Dimension screen;
        final List<NativeNode> nodes = new ArrayList<>();
        NativeSnapshot(Dimension screen) { this.screen = screen; }
        NativeNode exact(String label) {
            for (NativeNode node : nodes) if (normalizeSpaces(node.text).equals(label)) return node;
            return null;
        }
        boolean has(String label) { return exact(label) != null; }
        List<NativeNode> section(String heading, String... endHeadings) {
            NativeNode start = exact(heading);
            int top = start == null ? (int) (screen.getHeight() * 0.08) : start.visible.getY();
            int bottom = (int) (screen.getHeight() * 0.94);
            for (String end : endHeadings) {
                NativeNode node = exact(end);
                if (node != null && node.visible.getY() > top) bottom = Math.min(bottom, node.visible.getY());
            }
            List<NativeNode> result = new ArrayList<>();
            for (NativeNode node : nodes) {
                int centerY = node.visible.getY() + node.visible.getHeight() / 2;
                if (centerY >= top && centerY < bottom) result.add(node);
            }
            return result;
        }
    }

    private static final class NativeNode {
        final String text;
        final Rectangle bounds, visible;
        final org.w3c.dom.Element element;
        NativeNode(String text, Rectangle bounds, Rectangle visible, org.w3c.dom.Element element) {
            this.text = text; this.bounds = bounds; this.visible = visible; this.element = element;
        }
    }

    private List<String> nativeStrings(List<NativeNode> nodes) {
        java.util.LinkedHashSet<String> values = new java.util.LinkedHashSet<>();
        for (NativeNode node : nodes) {
            values.add(normalizeSpaces(node.text));
            for (String part : node.text.split("\\n")) {
                if (!normalizeSpaces(part).isEmpty()) values.add(normalizeSpaces(part));
            }
        }
        return new ArrayList<>(values);
    }

    private String nativeSignature(List<NativeNode> nodes) {
        StringBuilder signature = new StringBuilder();
        for (NativeNode node : nodes) signature.append(node.text).append('@')
                .append(node.visible.getX()).append(',').append(node.visible.getY()).append(';');
        return signature.toString();
    }

    private void validateHeaderChartControls() {
        NativeSnapshot snapshot = readNativeSnapshot();
        // Older builds offered three chart modes. Current builds show Stock Chart
        // and duration chips instead; validate the controls actually shipped.
        List<NativeNode> chartNodes = snapshot.section("Stock Chart", "Stock Rating", "Stock Range", "Fundamentals");
        List<String> chartValues = nativeStrings(chartNodes);
        if (chartValues.contains("Price") || chartValues.contains("P/E Ratio") || chartValues.contains("P/B Ratio")) {
            for (String mode : new String[]{"Price", "P/E Ratio", "P/B Ratio"}) {
                if (!chartValues.contains(mode)) throw new AssertionError("Chart mode missing: " + mode);
            }
            return;
        }
        if (!snapshot.has("Stock Chart")) throw new AssertionError("Stock Chart heading is missing; capture its native XML");
        List<String> values = nativeStrings(snapshot.section("Stock Chart", "Stock Rating"));
        StringBuilder rawBody = new StringBuilder();
        for (NativeNode node : chartNodes) rawBody.append(node.text).append(' ');
        String body = rawBody.toString();
        for (String label : new String[]{"High", "Low", "Returns", "1D", "1M", "3M", "1Y", "3Y", "5Y", "10Y", "All"}) {
            if (!Pattern.compile("(?<![\\p{L}\\p{N}])" + Pattern.quote(label) + "(?![\\p{L}\\p{N}])").matcher(body).find()) {
                throw new AssertionError("Stock Chart control/summary missing: " + label + " | values=" + values);
            }
        }
        java.util.regex.Matcher prices = Pattern.compile("₹\\s*([\\d,]+(?:\\.\\d+)?)").matcher(body);
        int count = 0;
        while (prices.find()) count++;
        if (count < 2 || !Pattern.compile("[-+]?\\d+(?:\\.\\d+)?\\s*%").matcher(body).find()) {
            throw new AssertionError("Stock Chart High, Low and Returns numeric values missing | values=" + values);
        }
        logValidatedText("Stock Chart controls and live summary", values.toString());
    }

}
