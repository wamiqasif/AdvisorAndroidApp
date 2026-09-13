package com.valueresearch.pages;

import com.valueresearch.utils.ReportLogger;
import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.android.nativekey.AndroidKey;
import io.appium.java_client.android.nativekey.KeyEvent;
import org.openqa.selenium.By;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.Rectangle;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Pause;
import org.openqa.selenium.interactions.PointerInput;
import org.openqa.selenium.interactions.Sequence;

import java.io.StringReader;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

/**
 * Simplified Analyst's Choice page object.
 *
 * Intentional scope:
 * 1. Open Analyst's Choice from Hub once.
 * 2. Open each recommendation card in top-to-bottom order.
 * 3. Capture the currently exposed live fund names from the accessibility tree.
 * 4. Press Android Back exactly once to close the detail page.
 * 5. Continue with the next card.
 *
 * No hardcoded fund names.
 * No rating/category/return validation.
 * No horizontal table swipe.
 * No vertical swipe inside a detail table.
 */
public class AnalystChoicePage {

    public static final String BUILD_VERSION = "VRSA-AC-2026-09-13-2.5";

    private static final long POLL_MS = 300L;
    private static final long SHORT_WAIT_MS = 12_000L;
    private static final long PAGE_WAIT_MS = 18_000L;
    private static final int MAX_LISTING_SCROLLS = 8;

    private final AndroidDriver driver;

    // ================= HUB / LISTING =================

    private final By hubTab = AppiumBy.accessibilityId("Hub");
    private final By analystChoiceHubMenu = AppiumBy.accessibilityId("Open Analyst's Choice screen");

    private final By analystChoiceTitle = AppiumBy.accessibilityId("Analyst’s Choice");
    private final By analystChoiceSubtitle = AppiumBy.accessibilityId(
            "Our recommended funds to match your financial goals."
    );

    /*
     * Current Flutter build exposes the clickable listing card using the
     * description/risk semantic instead of title + description in one node.
     */
    private final CardConfig aggressiveGrowth = new CardConfig(
            "Aggressive Growth",
            byDescContains("Best mid- and small-cap equity funds"),
            AppiumBy.accessibilityId("Aggressive Growth"),
            false
    );

    private final CardConfig growth = new CardConfig(
            "Growth",
            byDescContains("Top diversified equity funds"),
            AppiumBy.accessibilityId("Growth"),
            false
    );

    private final CardConfig taxPlanning = new CardConfig(
            "Tax Planning",
            byDescContains("ELSS funds similar to Growth funds"),
            AppiumBy.accessibilityId("Tax Planning"),
            false
    );

    private final CardConfig growthInternational = new CardConfig(
            "Growth - International",
            byDescContains("International equity funds"),
            AppiumBy.accessibilityId("Growth - International"),
            true
    );

    private final CardConfig conservativeGrowth = new CardConfig(
            "Conservative Growth",
            byDescContains("Large-cap equity funds"),
            AppiumBy.accessibilityId("Conservative Growth"),
            false
    );

    private final CardConfig conservativeGrowthIncome = new CardConfig(
            "Conservative Growth & Income",
            byDescContains("Equity-savings funds"),
            AppiumBy.accessibilityId("Conservative Growth & Income"),
            false
    );

    private final CardConfig coreFixedIncome = new CardConfig(
            "Core Fixed Income",
            byDescContains("Short-duration funds"),
            AppiumBy.accessibilityId("Core Fixed Income"),
            false
    );

    private final CardConfig capitalPreservation = new CardConfig(
            "Capital Preservation",
            byDescContains("Liquid and overnight funds"),
            AppiumBy.accessibilityId("Capital Preservation"),
            false
    );

    private final List<CardConfig> cards;

    // ================= DETAIL =================

    private final By fundHeader = AppiumBy.accessibilityId("Fund");
    private final By noRecommendationMessage = byDescContains("no recommendations for funds in this list");
    private final By compareInScreener = AppiumBy.accessibilityId("Compare in screener");

    public AnalystChoicePage(AndroidDriver driver) {
        this.driver = driver;
        this.cards = List.of(
                aggressiveGrowth,
                growth,
                taxPlanning,
                growthInternational,
                conservativeGrowth,
                conservativeGrowthIncome,
                coreFixedIncome,
                capitalPreservation
        );
    }

    // ========================================================================
    // PUBLIC FLOW
    // ========================================================================

    /**
     * Map key   = recommendation card name.
     * Map value = live fund names captured from that card.
     */
    /**
     * Independent per-card entry points used by the split TestNG tests.
     * Each method opens Analyst's Choice from Hub, opens only the requested card,
     * captures its live fund names, returns to the listing once, and finishes.
     *
     * Keeping each card independent means one card failure does not skip or hide
     * the result of the remaining recommendation cards.
     */
    public List<String> captureAggressiveGrowthFundNames() {
        return captureFundNamesFromSingleCard(aggressiveGrowth);
    }

    public List<String> captureGrowthFundNames() {
        return captureFundNamesFromSingleCard(growth);
    }

    public List<String> captureTaxPlanningFundNames() {
        return captureFundNamesFromSingleCard(taxPlanning);
    }

    public List<String> captureGrowthInternationalFundNames() {
        return captureFundNamesFromSingleCard(growthInternational);
    }

    public List<String> captureConservativeGrowthFundNames() {
        return captureFundNamesFromSingleCard(conservativeGrowth);
    }

    public List<String> captureConservativeGrowthIncomeFundNames() {
        return captureFundNamesFromSingleCard(conservativeGrowthIncome);
    }

    public List<String> captureCoreFixedIncomeFundNames() {
        return captureFundNamesFromSingleCard(coreFixedIncome);
    }

    public List<String> captureCapitalPreservationFundNames() {
        return captureFundNamesFromSingleCard(capitalPreservation);
    }

    /**
     * Fast state probe used by the split TestNG cases.
     *
     * After one card test completes, the app is intentionally left on the
     * Analyst's Choice listing. The next test must recognize that as an already
     * authenticated module state and MUST NOT call the global AuthHelper again,
     * because AuthHelper only recognizes the normal bottom-tab login markers.
     */
    public boolean isAnalystChoiceModuleActive() {
        return isListingState() || isDetailState();
    }

    private List<String> captureFundNamesFromSingleCard(CardConfig card) {
        prepareAnalystChoiceListingForCard();
        return openCardCaptureFundsAndReturn(card);
    }

    /**
     * Reuse the current Analyst's Choice state whenever possible.
     *
     * - Already on listing -> continue immediately.
     * - Left inside a detail screen by a previous failed test -> Back exactly once.
     * - Outside the module -> navigate from Hub normally.
     */
    private void prepareAnalystChoiceListingForCard() {
        if (isListingState()) {
            ReportLogger.debug("Analyst's Choice listing already active; reusing current module state");
            return;
        }

        if (isDetailState()) {
            ReportLogger.step("Analyst's Choice detail state detected from previous test; returning to listing once");
            pressBackOnce();
            if (!waitForListingState(7_000L)) {
                throw new AssertionError(
                        "Unable to restore Analyst's Choice listing from existing detail state"
                );
            }
            ReportLogger.pass("Analyst's Choice listing restored from existing detail state");
            return;
        }

        openAnalystChoiceFromHub();
        ensureListingState();
    }

    public Map<String, List<String>> captureFundNamesFromAllCards() {
        openAnalystChoiceFromHub();

        Map<String, List<String>> captured = new LinkedHashMap<>();
        List<String> errors = new ArrayList<>();

        for (CardConfig card : cards) {
            try {
                ensureListingState();

                List<String> funds = openCardCaptureFundsAndReturn(card);
                captured.put(card.name, funds);

                if (funds.isEmpty()) {
                    ReportLogger.pass(card.name + " -> no current fund recommendations");
                } else {
                    ReportLogger.pass(
                            card.name + " -> captured " + funds.size()
                                    + " fund(s): " + String.join(" | ", funds)
                    );
                }
            } catch (Exception | AssertionError e) {
                String error = card.name + " -> " + clean(e.getMessage());
                errors.add(error);
                ReportLogger.debug("[ANALYST CHOICE CARD CAPTURE FAILED] " + error);

                // One recovery attempt only. Never blindly press Back twice.
                try {
                    recoverListingAfterCardFailure();
                } catch (Exception | AssertionError recoveryError) {
                    ReportLogger.debug(
                            "[ANALYST CHOICE RECOVERY FAILED] "
                                    + card.name + " -> " + clean(recoveryError.getMessage())
                    );
                    break;
                }
            }
        }

        if (!errors.isEmpty()) {
            throw new AssertionError(
                    "Analyst's Choice capture completed with " + errors.size() + " issue(s):\n- "
                            + String.join("\n- ", errors)
            );
        }

        ReportLogger.pass("Captured fund names from all Analyst's Choice cards successfully");
        return captured;
    }

    public void openAnalystChoiceFromHub() {
        ReportLogger.step("Opening Analyst's Choice from Hub");

        WebElement hub = waitForVisibleOptional(hubTab, 4_000L);
        if (hub == null) {
            pressBackOnce();
            hub = waitForVisibleOptional(hubTab, 4_000L);
        }

        if (hub == null) {
            throw new AssertionError("Hub tab is not available");
        }

        tap(hub, "Hub tab");
        sleep(700L);

        WebElement menu = waitForInViewportOptional(analystChoiceHubMenu, 1_800L);

        for (int attempt = 1; menu == null && attempt <= MAX_LISTING_SCROLLS; attempt++) {
            ReportLogger.step("Searching Analyst's Choice tile in Hub | attempt=" + attempt);
            swipeUp(0.50, 0.76, 0.34, 600L);
            sleep(400L);
            menu = waitForInViewportOptional(analystChoiceHubMenu, 900L);
        }

        if (menu == null) {
            throw new AssertionError("Analyst's Choice Hub tile was not found");
        }

        tap(menu, "Analyst's Choice Hub tile");

        if (!waitForListingState(PAGE_WAIT_MS)) {
            throw new AssertionError("Analyst's Choice listing did not open");
        }

        ReportLogger.pass("Analyst's Choice listing opened");
    }

    // ========================================================================
    // CARD FLOW
    // ========================================================================

    private List<String> openCardCaptureFundsAndReturn(CardConfig card) {
        ReportLogger.step("Opening card: " + card.name);

        /*
         * Cards are processed in top-to-bottom order. Do NOT reset the listing
         * to the top before every card. The list keeps its previous scroll state,
         * so continuing downward is faster and much less flaky.
         */
        WebElement cardElement = findCardWithBoundedScroll(card);
        tap(cardElement, card.name + " card");

        waitForVisible(card.detailTitle, PAGE_WAIT_MS);
        ReportLogger.pass(card.name + " detail opened");

        List<String> funds = captureCurrentDetailFundNames(card);

        returnToListingOnce(card.name);
        ReportLogger.pass(card.name + " detail closed and listing restored");

        return funds;
    }

    private WebElement findCardWithBoundedScroll(CardConfig card) {
        WebElement element = waitForInViewportOptional(card.cardLocator, 1_000L);
        if (element != null) {
            return element;
        }

        for (int attempt = 1; attempt <= MAX_LISTING_SCROLLS; attempt++) {
            ReportLogger.step("Finding " + card.name + " card | scroll=" + attempt);
            swipeUp(0.50, 0.76, 0.34, 550L);
            sleep(350L);

            element = waitForInViewportOptional(card.cardLocator, 800L);
            if (element != null) {
                return element;
            }
        }

        throw new AssertionError(card.name + " card was not found");
    }

    // ========================================================================
    // DYNAMIC FUND CAPTURE
    // ========================================================================

    private List<String> captureCurrentDetailFundNames(CardConfig card) {
        if (isVisible(noRecommendationMessage)) {
            if (!card.emptyStateAllowed) {
                throw new AssertionError(card.name + " unexpectedly shows no recommendations");
            }

            ReportLogger.pass(card.name + " correctly has no current recommendations");
            return Collections.emptyList();
        }

        waitForVisible(fundHeader, SHORT_WAIT_MS);

        /*
         * No swiping inside the fund table.
         * The current Flutter/UiAutomator2 hierarchy exposes loaded rows in the
         * accessibility tree. Each real fund row is an android.view.View with
         * direct child semantics such as:
         *
         *   Kotak
         *     Mid Cap
         *     Direct G
         *
         * We capture those row structures directly and ignore large container
         * nodes. This avoids the false "null Mode..." / category-only values
         * seen in the previous geometry implementation.
         */
        List<String> funds = captureFundRowsFromDirectChildren();

        ReportLogger.step(card.name + " fund capture | totalCaptured=" + funds.size());

        if (funds.isEmpty()) {
            if (card.emptyStateAllowed) {
                ReportLogger.pass(card.name + " has no fund rows in the current accessibility tree");
                return Collections.emptyList();
            }

            throw new AssertionError(
                    "No valid fund rows could be captured from " + card.name
                            + ". Fund header is visible, but no row with direct category/plan semantics was found."
            );
        }

        return funds;
    }

    /**
     * Capture fund rows directly from Appium XML source.
     *
     * Why XML instead of element-relative XPath:
     * UiAutomator2 currently exposes each fund as a semantic parent with child
     * semantics, for example:
     *
     *   <android.view.View content-desc="Kotak">
     *       <android.view.View content-desc="Mid Cap"/>
     *       <android.view.View content-desc="Direct G"/>
     *   </android.view.View>
     *
     * Relative findElements() from a WebElement is unreliable for this Flutter
     * hierarchy on the current build. Parsing the already-returned page source
     * preserves the hierarchy exactly and avoids extra gestures.
     */
    private List<String> captureFundRowsFromDirectChildren() {
        LinkedHashSet<String> funds = new LinkedHashSet<>();

        String source;
        try {
            source = driver.getPageSource();
        } catch (Exception e) {
            throw new AssertionError("Unable to read Analyst's Choice page source: " + clean(e.getMessage()), e);
        }

        if (source == null || source.isBlank()) {
            return new ArrayList<>();
        }

        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(false);
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            try {
                factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            } catch (Exception ignored) {
                // Parser-specific hardening option. Safe to continue if unsupported.
            }
            factory.setExpandEntityReferences(false);

            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(new InputSource(new StringReader(source)));

            Element root = document.getDocumentElement();
            if (root != null) {
                collectLeafFundRows(root, funds);
            }
        } catch (Exception e) {
            throw new AssertionError("Unable to parse Analyst's Choice accessibility tree: " + clean(e.getMessage()), e);
        }

        return new ArrayList<>(funds);
    }

    /**
     * Depth-first traversal. A real fund row is the LEAF-MOST semantic container
     * that owns a plan child/descendant such as Direct G / Direct IDCW / IDCW.
     * Large Flutter containers also contain those values, but they are rejected
     * because a more specific descendant fund row is found first.
     *
     * @return true when this subtree already contains a captured fund row.
     */
    private boolean collectLeafFundRows(Element element, LinkedHashSet<String> funds) {
        boolean descendantFundFound = false;

        NodeList children = element.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child instanceof Element) {
                if (collectLeafFundRows((Element) child, funds)) {
                    descendantFundFound = true;
                }
            }
        }

        if (descendantFundFound) {
            return true;
        }

        String parentDesc = cleanText(element.getAttribute("content-desc"));
        if (!isPossibleFundParent(parentDesc)) {
            return false;
        }

        List<String> descendantSemantics = new ArrayList<>();
        collectDescendantSemantics(element, descendantSemantics);

        boolean hasPlan = false;
        for (String value : descendantSemantics) {
            if (looksLikePlan(value)) {
                hasPlan = true;
                break;
            }
        }

        if (!hasPlan) {
            return false;
        }

        List<String> parts = new ArrayList<>();
        parts.add(parentDesc);

        for (String value : descendantSemantics) {
            if (shouldAppendFundPart(parentDesc, parts, value)) {
                parts.add(value);
            }
        }

        String fullFundName = String.join(" ", parts)
                .replaceAll("\\s+", " ")
                .trim();

        if (!isValidCapturedFundName(fullFundName)) {
            return false;
        }

        funds.add(fullFundName);
        return true;
    }

    private void collectDescendantSemantics(Element parent, List<String> values) {
        NodeList children = parent.getChildNodes();

        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (!(child instanceof Element)) {
                continue;
            }

            Element childElement = (Element) child;
            String value = cleanText(childElement.getAttribute("content-desc"));

            if (!isNullish(value)
                    && !isNonFundSemantic(value)
                    && !containsIgnoreCase(values, value)) {
                values.add(value);
            }

            collectDescendantSemantics(childElement, values);
        }
    }

    private boolean isPossibleFundParent(String value) {
        return !isNullish(value)
                && !isNonFundSemantic(value)
                && !looksLikePlan(value)
                && value.length() <= 120;
    }

    private boolean shouldAppendFundPart(
            String parentDesc,
            List<String> currentParts,
            String candidate
    ) {
        if (isNullish(candidate) || isNonFundSemantic(candidate)) {
            return false;
        }

        String normalizedCandidate = normalize(candidate);
        String normalizedParent = normalize(parentDesc);

        // Example: "Bandhan ELSS - Tax Saver" already contains
        // child semantic "ELSS - Tax Saver". Do not duplicate it.
        if (!normalizedCandidate.isEmpty() && normalizedParent.contains(normalizedCandidate)) {
            return false;
        }

        for (String existing : currentParts) {
            String normalizedExisting = normalize(existing);
            if (normalizedExisting.equals(normalizedCandidate)
                    || normalizedExisting.contains(normalizedCandidate)) {
                return false;
            }
        }

        return true;
    }

    private boolean looksLikePlan(String value) {
        String lower = normalize(value);

        return lower.contains("direct")
                || lower.equals("idcw")
                || lower.endsWith(" idcw")
                || lower.equals("growth")
                || lower.equals("g")
                || lower.contains("regular plan");
    }

    private boolean isValidCapturedFundName(String value) {
        if (isNullish(value)) {
            return false;
        }

        String lower = normalize(value);

        return !lower.startsWith("null ")
                && !lower.contains("mode sip only")
                && !lower.contains("horizon")
                && !lower.equals("elss - tax saver")
                && !lower.equals("large & mid cap")
                && !lower.equals("mid cap")
                && !lower.equals("small cap")
                && !lower.equals("flexi cap")
                && !lower.equals("multi cap")
                && !lower.equals("value");
    }

    private boolean isNonFundSemantic(String value) {
        if (isNullish(value)) {
            return true;
        }

        String lower = normalize(value);

        return lower.equals("fund")
                || lower.equals("rating")
                || lower.equals("category")
                || lower.equals("consistency score")
                || lower.equals("5y return")
                || lower.equals("worst 1y return")
                || lower.equals("compare in screener")
                || lower.equals("mode")
                || lower.equals("horizon")
                || lower.startsWith("mode:")
                || lower.startsWith("horizon:")
                || lower.contains("best mid- and small-cap equity funds")
                || lower.contains("top diversified equity funds")
                || lower.contains("elss funds similar to growth funds")
                || lower.contains("international equity funds")
                || lower.contains("large-cap equity funds")
                || lower.contains("equity-savings funds")
                || lower.contains("short-duration funds")
                || lower.contains("liquid and overnight funds")
                || lower.matches("eq-[a-z&-]+")
                || lower.equals("unrated");
    }

    // ========================================================================
    // SAFE SINGLE-BACK NAVIGATION
    // ========================================================================

    private void ensureListingState() {
        if (isListingState()) {
            return;
        }

        throw new AssertionError(
                "Analyst's Choice listing is not active before opening the next card"
        );
    }

    /**
     * Important: press Back exactly once.
     *
     * Previous bug:
     * Conservative Growth is lower on the listing. After returning from detail,
     * the listing preserved that lower scroll position, so the top subtitle was
     * off-screen. The old method interpreted that as "not on listing" and sent a
     * second Back, which incorrectly navigated to Hub.
     *
     * The new state check accepts ANY visible recommendation card as proof that
     * the Analyst's Choice listing is active.
     */
    private void returnToListingOnce(String cardName) {
        if (isListingState()) {
            return;
        }

        pressBackOnce();

        if (!waitForListingState(7_000L)) {
            throw new AssertionError(
                    "Single Back did not restore Analyst's Choice listing after " + cardName
                            + ". A second Back was intentionally not sent to avoid leaving the module."
            );
        }
    }

    private void recoverListingAfterCardFailure() {
        if (isListingState()) {
            return;
        }

        // If a detail screen is still active, one Back is the only safe recovery.
        pressBackOnce();

        if (!waitForListingState(7_000L)) {
            throw new AssertionError("Unable to recover Analyst's Choice listing safely");
        }
    }

    private boolean waitForListingState(long timeoutMs) {
        long endAt = System.currentTimeMillis() + timeoutMs;

        while (System.currentTimeMillis() < endAt) {
            if (isListingState()) {
                return true;
            }
            sleep(POLL_MS);
        }

        return false;
    }

    private boolean isDetailState() {
        return isActuallyInViewport(fundHeader)
                || isActuallyInViewport(compareInScreener)
                || isActuallyInViewport(noRecommendationMessage);
    }

    private boolean isListingState() {
        /*
         * Detail pages reuse the same description semantics as their listing
         * cards. Therefore a card-description locator alone CANNOT prove that
         * we are back on the listing. Guard against detail-only anchors first.
         */
        if (isDetailState()) {
            return false;
        }

        if (isActuallyInViewport(analystChoiceTitle)
                || isActuallyInViewport(analystChoiceSubtitle)) {
            return true;
        }

        // When the listing is scrolled down, its title/subtitle can be off-screen.
        // A visible recommendation card is then enough once detail anchors are absent.
        for (CardConfig card : cards) {
            if (isActuallyInViewport(card.cardLocator)) {
                return true;
            }
        }

        return false;
    }

    // ========================================================================
    // GENERIC HELPERS
    // ========================================================================

    private WebElement waitForVisible(By locator, long timeoutMs) {
        WebElement element = waitForVisibleOptional(locator, timeoutMs);
        if (element == null) {
            throw new AssertionError("Timed out waiting for visible element: " + locator);
        }
        return element;
    }

    private WebElement waitForVisibleOptional(By locator, long timeoutMs) {
        long endAt = System.currentTimeMillis() + timeoutMs;

        while (System.currentTimeMillis() < endAt) {
            WebElement element = firstDisplayed(locator);
            if (element != null) {
                return element;
            }
            sleep(POLL_MS);
        }

        return null;
    }

    private WebElement waitForInViewportOptional(By locator, long timeoutMs) {
        long endAt = System.currentTimeMillis() + timeoutMs;

        while (System.currentTimeMillis() < endAt) {
            WebElement element = firstInViewport(locator);
            if (element != null) {
                return element;
            }
            sleep(POLL_MS);
        }

        return null;
    }

    private WebElement firstDisplayed(By locator) {
        for (WebElement element : safeFindElements(locator)) {
            try {
                if (element.isDisplayed()) {
                    return element;
                }
            } catch (Exception ignored) {
                // Try next element.
            }
        }
        return null;
    }

    private WebElement firstInViewport(By locator) {
        Dimension size = driver.manage().window().getSize();

        for (WebElement element : safeFindElements(locator)) {
            try {
                Rectangle rect = element.getRect();
                int centerX = rect.getX() + rect.getWidth() / 2;
                int centerY = rect.getY() + rect.getHeight() / 2;

                if (element.isDisplayed()
                        && rect.getWidth() > 0
                        && rect.getHeight() > 0
                        && centerX >= 0
                        && centerX <= size.getWidth()
                        && centerY >= 0
                        && centerY <= size.getHeight()) {
                    return element;
                }
            } catch (Exception ignored) {
                // Try next element.
            }
        }

        return null;
    }

    private boolean isVisible(By locator) {
        return firstDisplayed(locator) != null;
    }

    private boolean isActuallyInViewport(By locator) {
        return firstInViewport(locator) != null;
    }

    private List<WebElement> safeFindElements(By locator) {
        try {
            return driver.findElements(locator);
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    private void tap(WebElement element, String label) {
        try {
            element.click();
        } catch (Exception clickFailure) {
            Rectangle rect = element.getRect();
            Map<String, Object> args = new HashMap<>();
            args.put("x", rect.getX() + rect.getWidth() / 2);
            args.put("y", rect.getY() + rect.getHeight() / 2);
            driver.executeScript("mobile: clickGesture", args);
        }

        ReportLogger.step(label + " clicked");
        sleep(450L);
    }

    private void pressBackOnce() {
        driver.pressKey(new KeyEvent(AndroidKey.BACK));
        sleep(700L);
    }

    private void swipeUp(double xRatio, double startYRatio, double endYRatio, long durationMs) {
        Dimension size = driver.manage().window().getSize();

        int x = (int) Math.round(size.getWidth() * xRatio);
        int startY = (int) Math.round(size.getHeight() * startYRatio);
        int endY = (int) Math.round(size.getHeight() * endYRatio);

        PointerInput finger = new PointerInput(PointerInput.Kind.TOUCH, "analystChoiceFinger");
        Sequence swipe = new Sequence(finger, 1);

        swipe.addAction(finger.createPointerMove(
                Duration.ZERO,
                PointerInput.Origin.viewport(),
                x,
                startY
        ));
        swipe.addAction(finger.createPointerDown(PointerInput.MouseButton.LEFT.asArg()));
        swipe.addAction(new Pause(finger, Duration.ofMillis(70L)));
        swipe.addAction(finger.createPointerMove(
                Duration.ofMillis(durationMs),
                PointerInput.Origin.viewport(),
                x,
                endY
        ));
        swipe.addAction(finger.createPointerUp(PointerInput.MouseButton.LEFT.asArg()));

        driver.perform(Collections.singletonList(swipe));
    }

    private By byDescContains(String value) {
        return AppiumBy.androidUIAutomator(
                "new UiSelector().descriptionContains(\"" + escapeUiAutomator(value) + "\")"
        );
    }

    private String semanticText(WebElement element) {
        try {
            String value = element.getAttribute("content-desc");
            return cleanText(value);
        } catch (Exception e) {
            return "";
        }
    }

    private boolean containsIgnoreCase(List<String> values, String candidate) {
        for (String value : values) {
            if (value.equalsIgnoreCase(candidate)) {
                return true;
            }
        }
        return false;
    }

    private boolean isNullish(String value) {
        if (value == null) {
            return true;
        }

        String cleaned = cleanText(value);
        return cleaned.isEmpty()
                || cleaned.equalsIgnoreCase("null")
                || cleaned.toLowerCase().startsWith("null ");
    }

    private String normalize(String value) {
        return cleanText(value).toLowerCase();
    }

    private String cleanText(String value) {
        if (value == null) {
            return "";
        }
        return value.replace('\n', ' ')
                .replace('\r', ' ')
                .replaceAll("\\s+", " ")
                .trim();
    }

    private String escapeUiAutomator(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private String clean(String value) {
        if (value == null || value.trim().isEmpty()) {
            return "Unknown error";
        }

        int buildInfo = value.indexOf("Build info:");
        return (buildInfo > 0 ? value.substring(0, buildInfo) : value).trim();
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Thread interrupted", e);
        }
    }

    private static final class CardConfig {
        private final String name;
        private final By cardLocator;
        private final By detailTitle;
        private final boolean emptyStateAllowed;

        private CardConfig(
                String name,
                By cardLocator,
                By detailTitle,
                boolean emptyStateAllowed
        ) {
            this.name = name;
            this.cardLocator = cardLocator;
            this.detailTitle = detailTitle;
            this.emptyStateAllowed = emptyStateAllowed;
        }
    }
}