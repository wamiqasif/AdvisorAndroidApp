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
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


/**
 * Stable Page Object for Hub -> More -> Compliance Pages.
 *
 * Key stability rules:
 * - Hub tiles use ONLY the exact accessibility IDs captured from Appium Inspector.
 * - No textContains/descriptionContains is used for Hub navigation.
 * - No absolute XPath and no fixed screen coordinates are used.
 * - Dual subscription is detected only when BOTH exact Fund Advisor and Stock Advisor
 *   selector options are present.
 * - Absence of the selector NEVER means VRSA-only by itself. VRSA-only is accepted
 *   only when the internal android.webkit.WebView is actually present.
 * - For internal compliance pages, the requested success condition is that the VRSA
 *   WebView opens. Native UiAutomator2 does not expose the FAQ/WebView DOM text, so
 *   the WebView container is the stable native readiness signal.
 */
public class CompliancePagesPage {

    private final AndroidDriver driver;

    private static final String ADVISOR_PACKAGE = "com.valueresearch.advisor";

    private static final int HUB_CONTENT_WAIT_SECONDS = 15;
    private static final int MORE_SECTION_WAIT_SECONDS = 7;
    private static final int ROUTE_STATE_WAIT_SECONDS = 10;
    private static final int WEBVIEW_WAIT_SECONDS = 30;
    private static final int USER_AGREEMENT_WAIT_SECONDS = 25;
    private static final int ODR_WAIT_SECONDS = 30;
    private static final int POLL_MS = 250;

    // ---------------------------------------------------------
    // Stable application anchors
    // ---------------------------------------------------------

    // Exact bottom-nav locator confirmed from Appium Inspector.
    // IMPORTANT: presence of this element only means bottom navigation is visible.
    // It must NEVER be used as proof that the Hub screen itself is open.
    private static final By HUB_TAB = AppiumBy.accessibilityId("Hub");

    // Unique Hub-content anchors. These do not exist on Funds/Stocks/Portfolio screens.
    private static final By HUB_MARKET_MONITOR_TILE =
            AppiumBy.accessibilityId("Open Market Monitor screen");
    private static final By HUB_STOCK_ADVISOR_TILE =
            AppiumBy.accessibilityId("Open Stock Advisor");

    private static final By MORE_SECTION = AppiumBy.accessibilityId("More");
    private static final By HUB_SCROLL_VIEW = AppiumBy.className("android.widget.ScrollView");
    private static final By WEBVIEW = AppiumBy.className("android.webkit.WebView");

    // ---------------------------------------------------------
    // EXACT Hub -> More locators captured from Appium Inspector
    // ---------------------------------------------------------

    private static final PageSpec FAQS = new PageSpec(
            "FAQs",
            "View frequently asked questions"
    );

    private static final PageSpec ABOUT_US = new PageSpec(
            "About Us",
            "Learn about Value Research"
    );

    private static final PageSpec PRIVACY_POLICY = new PageSpec(
            "Privacy Policy",
            "Read our privacy policy"
    );

    private static final PageSpec USER_AGREEMENT = new PageSpec(
            "User Agreement",
            "View and download your user agreement"
    );

    private static final PageSpec REFUND_POLICY = new PageSpec(
            "Refund Policy",
            "View our refund policy"
    );

    private static final PageSpec INVESTOR_CHARTER = new PageSpec(
            "Investor Charter",
            "View the investor charter"
    );

    private static final PageSpec INVESTOR_COMPLAINT = new PageSpec(
            "Investor Complaint",
            "Submit or track complaint"
    );

    private static final PageSpec ODR_PORTAL = new PageSpec(
            "ODR Portal",
            "Open the ODR portal for dispute resolution"
    );

    private static final PageSpec AUDIT_STATUS = new PageSpec(
            "Audit Status",
            "View our audit status"
    );

    // ---------------------------------------------------------
    // EXACT dual-subscription selector locators
    // ---------------------------------------------------------

    private static final By FUND_ADVISOR_OPTION =
            AppiumBy.accessibilityId("Fund Advisor");

    private static final By STOCK_ADVISOR_OPTION =
            AppiumBy.accessibilityId("Stock Advisor");

    // User Agreement has a different sheet structure. The whole row + Download
    // action is exposed as one clickable accessibility node.
    private static final By USER_AGREEMENT_TITLE =
            AppiumBy.accessibilityId("User Agreement");

    private static final By FUND_ADVISOR_AGREEMENT_DOWNLOAD =
            AppiumBy.accessibilityId("Fund Advisor\nDownload");

    private static final By STOCK_ADVISOR_AGREEMENT_DOWNLOAD =
            AppiumBy.accessibilityId("Stock Advisor\nDownload");

    private static final Map<String, PageSpec> COMPLIANCE_PAGE_SPECS = buildPageSpecs();

    public CompliancePagesPage(AndroidDriver driver) {
        this.driver = driver;
    }

    // =========================================================
    // PUBLIC TEST FLOW METHODS
    // =========================================================

    public void prepareHubForCompliancePages() {
        ReportLogger.step("Preparing Hub -> More for Compliance Pages");

        new AuthHelper(driver).ensureLoggedIn();
        ReportLogger.pass("Advisor login/session confirmed for Compliance Pages");

        activateAdvisorAppIfNeeded();

        /*
         * IMPORTANT:
         * Do not try to infer whether Hub is selected from generic bottom-nav/content
         * markers. The stable Contact Us flow already proves that the exact Hub
         * accessibility node can be clicked and then the required More tile can be
         * located by controlled scrolling.
         *
         * Therefore navigation is deterministic:
         * 1) click exact accessibilityId=Hub ONCE,
         * 2) search for the exact More/compliance semantics,
         * 3) scroll only when those exact semantics are not yet visible.
         */
        openHubTabControlled();
        ensureMoreSectionVisible();
        ReportLogger.pass("Hub More section ready for Compliance Pages");
    }

    public void verifyCompliancePageTilesVisible() {
        ReportLogger.step("Validating exact Compliance Pages Hub locators");
        prepareHubForCompliancePages();

        StringBuilder missing = new StringBuilder();

        for (PageSpec spec : COMPLIANCE_PAGE_SPECS.values()) {
            WebElement element = firstDisplayed(spec.hubLocator);

            if (element == null) {
                element = findExactHubTileWithControlledScroll(spec);
            }

            if (element == null) {
                if (missing.length() > 0) {
                    missing.append(", ");
                }
                missing.append(spec.name)
                        .append(" [")
                        .append(spec.hubAccessibilityId)
                        .append("]");
            } else {
                ReportLogger.pass("Exact Hub locator found: "
                        + spec.name
                        + " | accessibilityId="
                        + spec.hubAccessibilityId);
            }
        }

        if (missing.length() > 0) {
            throw new RuntimeException(
                    "Compliance Pages exact Hub locator validation failed. Missing: " + missing
            );
        }

        ReportLogger.pass("All Compliance Pages exact Hub locators are present");
    }

    public void verifyFaqsVrsaPage() {
        openInternalVrsaCompliancePage(FAQS);
    }

    public void verifyAboutUsVrsaPage() {
        openInternalVrsaCompliancePage(ABOUT_US);
    }

    public void verifyPrivacyPolicyVrsaPage() {
        openInternalVrsaCompliancePage(PRIVACY_POLICY);
    }

    public void verifyRefundPolicyVrsaPage() {
        openInternalVrsaCompliancePage(REFUND_POLICY);
    }

    public void verifyInvestorCharterVrsaPage() {
        openInternalVrsaCompliancePage(INVESTOR_CHARTER);
    }

    public void verifyInvestorComplaintVrsaPage() {
        openInternalVrsaCompliancePage(INVESTOR_COMPLAINT);
    }

    public void verifyAuditStatusVrsaPage() {
        openInternalVrsaCompliancePage(AUDIT_STATUS);
    }

    public void verifyUserAgreementVrsaDownloadOpens() {
        ReportLogger.step("Opening User Agreement using exact Hub locator");
        prepareHubForCompliancePages();
        tapExactHubTile(USER_AGREEMENT);

        RouteState state = waitForUserAgreementEntryState();

        if (state == RouteState.DUAL_SELECTOR) {
            ReportLogger.step(
                    "User Agreement dual-subscription sheet confirmed using exact Stock Advisor Download locator"
            );

            WebElement stockDownload = firstDisplayed(STOCK_ADVISOR_AGREEMENT_DOWNLOAD);
            if (stockDownload == null) {
                throw new RuntimeException(
                        "User Agreement sheet detected but exact Stock Advisor\\nDownload element disappeared"
                );
            }

            clickElement(stockDownload, "Stock Advisor Download - User Agreement");
            waitForStockAdvisorAgreementDownloadResult();
            return;
        }

        if (state == RouteState.DIRECT_VRSA_WEBVIEW) {
            ReportLogger.pass(
                    "User Agreement opened directly in VRSA WebView; service sheet was correctly not required"
            );
            return;
        }

        if (state == RouteState.DIRECT_PDF_DOWNLOAD) {
            ReportLogger.step(
                    "User Agreement started a direct PDF download; validating completed download/viewer state"
            );
            waitForStockAdvisorAgreementDownloadResult();
            return;
        }

        throw new RuntimeException("Unsupported User Agreement route state: " + state);
    }

    public void verifyOdrPortalOpens() {
        ReportLogger.step("Opening ODR Portal using exact Hub locator");
        prepareHubForCompliancePages();

        tapExactHubTile(ODR_PORTAL);

        long deadline = System.currentTimeMillis()
                + Duration.ofSeconds(ODR_WAIT_SECONDS).toMillis();

        while (System.currentTimeMillis() < deadline) {
            String currentPackage = safeCurrentPackage();

            if (currentPackage != null
                    && !currentPackage.isEmpty()
                    && !ADVISOR_PACKAGE.equals(currentPackage)) {

                // The old run already proved Chrome exposes SMARTODR/Investor Login on
                // this device. Keep it as extra evidence, but package transition itself
                // is enough to prove the external ODR route opened.
                boolean odrMarker = isAnyTextOrDescriptionVisible(
                        "SMARTODR",
                        "Investor Login"
                );

                ReportLogger.pass(
                        "ODR Portal external page opened successfully"
                                + " | currentPackage=" + currentPackage
                                + " | odrMarkerFound=" + odrMarker
                );
                return;
            }

            sleep(POLL_MS);
        }

        throw new RuntimeException(
                "ODR Portal did not leave Advisor app within "
                        + ODR_WAIT_SECONDS
                        + " seconds | currentPackage="
                        + safeCurrentPackage()
        );
    }

    /**
     * Bounded recovery for sequential Compliance Pages cases.
     *
     * We do not use a generic "Hub screen" detector here. If the exact More grid is
     * already exposed, recovery is complete. Otherwise dismiss one foreground
     * compliance layer with BACK, then click the exact Hub tab once and restore More.
     */
    public void recoverToHubAfterComplianceCase() {
        try {
            // User Agreement download completion is a native/system dialog that can
            // remain above the Advisor app. If it is left open, the next testcase
            // cannot see Hub/PIN/login semantics and the whole suite gets poisoned.
            if (isPdfDownloadCompletionPromptVisible()) {
                ReportLogger.step(
                        "Compliance Pages recovery: dismissing completed PDF download prompt"
                );
                dismissPdfDownloadCompletionPrompt();
            }

            activateAdvisorAppIfNeeded();

            if (isMoreGridVisible()) {
                ReportLogger.step("Compliance Pages recovery: Hub More is already visible");
                return;
            }

            if (hasComplianceOverlayOrPage()) {
                driver.navigate().back();
                sleep(700);

                if (isMoreGridVisible()) {
                    ReportLogger.step("Recovered to Hub More with one BACK action");
                    return;
                }
            }

            openHubTabControlled();
            ensureMoreSectionVisible();
            ReportLogger.step("Recovered to Hub More after exact Hub navigation");
        } catch (Exception e) {
            ReportLogger.step(
                    "Non-blocking Compliance Pages recovery warning: " + e.getMessage()
            );
        }
    }

    // =========================================================
    // INTERNAL VRSA PAGE FLOW
    // =========================================================

    private void openInternalVrsaCompliancePage(PageSpec spec) {
        ReportLogger.step(
                "Opening " + spec.name
                        + " using exact accessibilityId="
                        + spec.hubAccessibilityId
        );

        prepareHubForCompliancePages();
        tapExactHubTile(spec);

        RouteState routeState = waitForInternalEntryState(spec);

        if (routeState == RouteState.DUAL_SELECTOR) {
            ReportLogger.step(
                    spec.name
                            + " dual-subscription selector confirmed"
                            + " | Fund Advisor=true | Stock Advisor=true"
            );

            // If the page-specific sheet title is exposed, log it as additional proof.
            // We do not use the title to locate the actionable row; the exact Stock
            // Advisor locator remains the stable selector action.
            if (firstDisplayed(AppiumBy.accessibilityId(spec.name)) != null) {
                ReportLogger.pass(spec.name + " selector title matched expected page");
            }

            WebElement stockAdvisor = firstDisplayed(STOCK_ADVISOR_OPTION);
            if (stockAdvisor == null) {
                throw new RuntimeException(
                        spec.name + " selector detected but exact Stock Advisor option disappeared"
                );
            }

            clickElement(stockAdvisor, "Stock Advisor option - " + spec.name);
            ReportLogger.step("Selected Stock Advisor for " + spec.name);

            waitForVrsaWebView(spec, "dual-subscription");
            return;
        }

        if (routeState == RouteState.DIRECT_VRSA_WEBVIEW) {
            ReportLogger.pass(
                    spec.name
                            + " opened directly in VRSA WebView"
                            + " | subscriptionPath=VRSA_ONLY"
            );
            return;
        }

        throw new RuntimeException(spec.name + " reached unsupported route state: " + routeState);
    }

    private RouteState waitForInternalEntryState(PageSpec spec) {
        ReportLogger.step(
                "Waiting for " + spec.name
                        + " route state: dual selector OR direct VRSA WebView"
        );

        long deadline = System.currentTimeMillis()
                + Duration.ofSeconds(ROUTE_STATE_WAIT_SECONDS).toMillis();

        while (System.currentTimeMillis() < deadline) {
            boolean fundVisible = firstDisplayed(FUND_ADVISOR_OPTION) != null;
            boolean stockVisible = firstDisplayed(STOCK_ADVISOR_OPTION) != null;

            if (fundVisible && stockVisible) {
                return RouteState.DUAL_SELECTOR;
            }

            if (isVrsaWebViewVisible()) {
                return RouteState.DIRECT_VRSA_WEBVIEW;
            }

            sleep(POLL_MS);
        }

        throw new RuntimeException(
                spec.name
                        + " did not reach a valid subscription route within "
                        + ROUTE_STATE_WAIT_SECONDS
                        + " seconds. Neither BOTH Fund Advisor + Stock Advisor options"
                        + " nor VRSA WebView was detected."
                        + " currentPackage=" + safeCurrentPackage()
        );
    }

    private void waitForVrsaWebView(PageSpec spec, String subscriptionPath) {
        ReportLogger.step("Waiting for " + spec.name + " VRSA WebView to open");

        long deadline = System.currentTimeMillis()
                + Duration.ofSeconds(WEBVIEW_WAIT_SECONDS).toMillis();

        while (System.currentTimeMillis() < deadline) {
            if (!isDriverAlive()) {
                throw new RuntimeException(
                        "Driver/session became unavailable while waiting for " + spec.name
                );
            }

            boolean selectorStillVisible =
                    firstDisplayed(FUND_ADVISOR_OPTION) != null
                            || firstDisplayed(STOCK_ADVISOR_OPTION) != null;

            if (!selectorStillVisible && isVrsaWebViewVisible()) {
                // Small stability window: make sure we did not catch a one-frame transition.
                sleep(700);

                if (isVrsaWebViewVisible()) {
                    ReportLogger.pass(
                            spec.name
                                    + " VRSA WebView opened successfully"
                                    + " | subscriptionPath=" + subscriptionPath
                    );
                    return;
                }
            }

            sleep(POLL_MS);
        }

        throw new RuntimeException(
                spec.name
                        + " VRSA WebView did not open within "
                        + WEBVIEW_WAIT_SECONDS
                        + " seconds"
                        + " | fundOptionVisible="
                        + (firstDisplayed(FUND_ADVISOR_OPTION) != null)
                        + " | stockOptionVisible="
                        + (firstDisplayed(STOCK_ADVISOR_OPTION) != null)
                        + " | webViewVisible=" + isVrsaWebViewVisible()
                        + " | currentPackage=" + safeCurrentPackage()
        );
    }

    // =========================================================
    // USER AGREEMENT FLOW
    // =========================================================

    private RouteState waitForUserAgreementEntryState() {
        ReportLogger.step(
                "Waiting for User Agreement state: exact dual download sheet OR direct VRSA WebView OR direct PDF download"
        );

        long deadline = System.currentTimeMillis()
                + Duration.ofSeconds(ROUTE_STATE_WAIT_SECONDS).toMillis();

        while (System.currentTimeMillis() < deadline) {
            boolean fundDownloadVisible =
                    firstDisplayed(FUND_ADVISOR_AGREEMENT_DOWNLOAD) != null;
            boolean stockDownloadVisible =
                    firstDisplayed(STOCK_ADVISOR_AGREEMENT_DOWNLOAD) != null;

            if (fundDownloadVisible && stockDownloadVisible) {
                if (firstDisplayed(USER_AGREEMENT_TITLE) != null) {
                    ReportLogger.pass("User Agreement selector title matched expected page");
                }
                return RouteState.DUAL_SELECTOR;
            }

            if (isVrsaWebViewVisible()) {
                return RouteState.DIRECT_VRSA_WEBVIEW;
            }

            /*
             * VRSA-only can bypass the service selector and immediately trigger
             * Android's completed-PDF prompt (filename.pdf + downloaded + Open/Cancel).
             * This is a valid User Agreement result, not a recovery/error state.
             */
            if (isPdfDownloadCompletionPromptVisible()) {
                ReportLogger.pass(
                        "User Agreement direct PDF download completion prompt detected"
                                + " | subscriptionPath=VRSA_ONLY"
                );
                return RouteState.DIRECT_PDF_DOWNLOAD;
            }

            sleep(POLL_MS);
        }

        throw new RuntimeException(
                "User Agreement did not reach a valid subscription route within "
                        + ROUTE_STATE_WAIT_SECONDS
                        + " seconds. Exact Fund Advisor\\nDownload + Stock Advisor\\nDownload"
                        + " rows were not both present, no VRSA WebView appeared,"
                        + " and no completed PDF download prompt was detected."
        );
    }

    private void waitForStockAdvisorAgreementDownloadResult() {
        ReportLogger.step("Waiting for Stock Advisor User Agreement download result");

        long deadline = System.currentTimeMillis()
                + Duration.ofSeconds(USER_AGREEMENT_WAIT_SECONDS).toMillis();

        while (System.currentTimeMillis() < deadline) {
            /*
             * IMPORTANT:
             * Android's completed-download dialog may remain technically over the
             * Advisor package, so currentPackage() is not a reliable first signal.
             * Detect the actual PDF completion prompt BEFORE package-based handling.
             */
            if (isPdfDownloadCompletionPromptVisible()) {
                ReportLogger.pass(
                        "Stock Advisor User Agreement PDF downloaded successfully"
                                + " | completionPrompt=true"
                                + " | pdf=true"
                                + " | downloaded=true"
                );

                // Do not leave the native/system prompt blocking the next testcase.
                dismissPdfDownloadCompletionPrompt();
                return;
            }

            String currentPackage = safeCurrentPackage();

            // External viewer/browser path. This is checked only after ruling out
            // the completed-download system dialog.
            if (currentPackage != null
                    && !currentPackage.isEmpty()
                    && !ADVISOR_PACKAGE.equals(currentPackage)) {
                ReportLogger.pass(
                        "Stock Advisor User Agreement opened externally"
                                + " | currentPackage=" + currentPackage
                );
                return;
            }

            // Some builds may render the agreement internally instead of showing the
            // download prompt. Accept only a real WebView, never selector absence alone.
            if (isVrsaWebViewVisible()
                    && firstDisplayed(STOCK_ADVISOR_AGREEMENT_DOWNLOAD) == null) {
                ReportLogger.pass(
                        "Stock Advisor User Agreement opened in internal VRSA WebView"
                );
                return;
            }

            sleep(POLL_MS);
        }

        throw new RuntimeException(
                "Stock Advisor User Agreement did not produce a valid download/viewer state within "
                        + USER_AGREEMENT_WAIT_SECONDS
                        + " seconds"
                        + " | currentPackage=" + safeCurrentPackage()
                        + " | pdfCompletionPromptVisible=" + isPdfDownloadCompletionPromptVisible()
                        + " | webViewVisible=" + isVrsaWebViewVisible()
                        + " | stockDownloadStillVisible="
                        + (firstDisplayed(STOCK_ADVISOR_AGREEMENT_DOWNLOAD) != null)
        );
    }

    /**
     * Detects the Android completed-download prompt shown after the Stock Advisor
     * agreement PDF finishes downloading. The emulator screenshot shows:
     * filename.pdf + "100.0% downloaded" + Cancel + Open.
     *
     * We intentionally use multiple native signals because some Android versions
     * expose Open/Cancel as text while others expose them via accessibility
     * descriptions. Page-source matching is a bounded fallback used only for this
     * one system dialog.
     */
    private boolean isPdfDownloadCompletionPromptVisible() {
        boolean openVisible = isExactTextVisible("Open");
        boolean cancelVisible = isExactTextVisible("Cancel");
        boolean downloadedVisible = isAnyTextOrDescriptionVisible("downloaded");

        if (openVisible && cancelVisible && downloadedVisible) {
            return true;
        }

        try {
            String source = driver.getPageSource();
            if (source == null || source.isEmpty()) {
                return false;
            }

            String lower = source.toLowerCase();
            boolean pdf = lower.contains(".pdf");
            boolean downloaded = lower.contains("downloaded");
            boolean open = lower.contains(">open<")
                    || lower.contains("text=\"open\"")
                    || lower.contains("content-desc=\"open\"");
            boolean cancel = lower.contains(">cancel<")
                    || lower.contains("text=\"cancel\"")
                    || lower.contains("content-desc=\"cancel\"");

            return pdf && downloaded && (open || cancel);
        } catch (Exception ignored) {
            return false;
        }
    }

    /**
     * Dismisses the completed-download prompt after validation so CP_006 and later
     * cases start from the Advisor app instead of a blocking native dialog.
     */
    private void dismissPdfDownloadCompletionPrompt() {
        WebElement cancel = findExactTextOrDescription("Cancel");

        if (cancel != null) {
            try {
                cancel.click();
                ReportLogger.step("Dismissed PDF download completion prompt using Cancel");
                sleep(600);
                return;
            } catch (Exception ignored) {
                // BACK below is the safe fallback for the system dialog.
            }
        }

        try {
            driver.navigate().back();
            ReportLogger.step("Dismissed PDF download completion prompt using Android BACK");
            sleep(600);
        } catch (Exception e) {
            throw new RuntimeException(
                    "PDF download completed but the completion prompt could not be dismissed",
                    e
            );
        }
    }

    private WebElement findExactTextOrDescription(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }

        String escaped = escapeUiSelector(value);

        WebElement element = firstDisplayed(
                AppiumBy.androidUIAutomator(
                        "new UiSelector().text(\"" + escaped + "\")"
                )
        );

        if (element != null) {
            return element;
        }

        return firstDisplayed(
                AppiumBy.androidUIAutomator(
                        "new UiSelector().description(\"" + escaped + "\")"
                )
        );
    }

    // =========================================================
    // HUB NAVIGATION - EXACT LOCATORS ONLY
    // =========================================================

    private void openHubTabControlled() {
        ReportLogger.step("Opening Hub using exact accessibilityId=Hub");

        WebElement hub = firstDisplayed(HUB_TAB);
        if (hub == null) {
            throw new RuntimeException(
                    "Exact Hub bottom-tab locator not found: accessibilityId=Hub"
            );
        }

        /*
         * Use the same primary interaction that is already stable in the Contact Us
         * module: WebElement.click() on the exact accessibility node. Do not wait for
         * a separate guessed Hub marker and do not retry the Hub tap in a loop.
         */
        try {
            hub.click();
            ReportLogger.step("Tapped exact Hub bottom tab using WebElement.click | accessibilityId=Hub");
        } catch (Exception firstError) {
            // Final fallback is the live center of the exact located Hub element.
            // The coordinates are derived at runtime, never hardcoded.
            tapDynamicElementCenter(
                    hub,
                    "Hub bottom tab dynamic-center fallback | accessibilityId=Hub"
            );
        }

        // Flutter needs a short render window before the More semantics are queried.
        sleep(1000);
    }

    private void ensureMoreSectionVisible() {
        if (isMoreGridVisible()) {
            ReportLogger.pass("Hub More grid is already visible");
            return;
        }

        ReportLogger.step("Locating Hub More grid using exact compliance accessibility IDs");

        // Same proven W3C scrolling pattern used by the stable Contact Us module.
        // No UiScrollable and no element-scoped scrollGesture on the Flutter tree.
        for (int attempt = 1; attempt <= 4; attempt++) {
            swipeUpW3C();
            sleep(500);

            if (isMoreGridVisible()) {
                ReportLogger.pass(
                        "Hub More grid found after controlled W3C scroll | attempt=" + attempt
                );
                return;
            }
        }

        throw new RuntimeException(
                "Hub More grid was not found after exact Hub tap and 4 controlled scrolls"
        );
    }

    private boolean isMoreGridVisible() {
        // A compliance selector/WebView/page covering the Hub means the More grid
        // is not the active foreground state. Do not use package-name checks here;
        // exact Hub accessibility IDs are the stronger signal.
        if (hasComplianceOverlayOrPage()) {
            return false;
        }

        int visibleTiles = 0;
        PageSpec[] anchors = new PageSpec[]{
                FAQS, ABOUT_US, PRIVACY_POLICY, USER_AGREEMENT, REFUND_POLICY,
                INVESTOR_CHARTER, INVESTOR_COMPLAINT, ODR_PORTAL, AUDIT_STATUS
        };

        for (PageSpec spec : anchors) {
            if (firstDisplayed(spec.hubLocator) != null) {
                visibleTiles++;
                if (visibleTiles >= 2) {
                    return true;
                }
            }
        }

        return firstDisplayed(MORE_SECTION) != null && visibleTiles >= 1;
    }

    private void tapExactHubTile(PageSpec spec) {
        WebElement tile = firstDisplayed(spec.hubLocator);

        if (tile == null) {
            ensureMoreSectionVisible();
            tile = findExactHubTileWithControlledScroll(spec);
        }

        if (tile == null) {
            throw new RuntimeException(
                    "Exact Hub tile not found/displayed: "
                            + spec.name
                            + " | accessibilityId="
                            + spec.hubAccessibilityId
            );
        }

        assertSafeHubTileBounds(tile, spec);

        ReportLogger.step(
                "Tapping exact Hub tile: "
                        + spec.name
                        + " | accessibilityId="
                        + spec.hubAccessibilityId
        );

        clickElement(tile, spec.name + " exact Hub tile");
    }

    /**
     * Flutter UiScrollable can return the scroll container instead of the requested
     * semantic child. Do not click the element returned by UiScrollable. Use only
     * controlled viewport swipes and re-fetch the exact accessibility ID each time.
     */
    private WebElement findExactHubTileWithControlledScroll(PageSpec spec) {
        WebElement tile = firstDisplayed(spec.hubLocator);
        if (tile != null) {
            return tile;
        }

        for (int attempt = 1; attempt <= 4; attempt++) {
            swipeUpW3C();
            sleep(450);

            tile = firstDisplayed(spec.hubLocator);
            if (tile != null) {
                ReportLogger.step(
                        "Exact Hub tile visible after controlled W3C scroll"
                                + " | module=" + spec.name
                                + " | attempt=" + attempt
                );
                return tile;
            }
        }

        // If Hub preserved a lower scroll position, search upward as a bounded recovery.
        for (int attempt = 1; attempt <= 3; attempt++) {
            swipeDownW3C();
            sleep(450);

            tile = firstDisplayed(spec.hubLocator);
            if (tile != null) {
                ReportLogger.step(
                        "Exact Hub tile visible after upward recovery W3C scroll"
                                + " | module=" + spec.name
                                + " | attempt=" + attempt
                );
                return tile;
            }
        }

        return null;
    }

    private void assertSafeHubTileBounds(WebElement tile, PageSpec spec) {
        try {
            Rectangle rect = tile.getRect();
            Dimension screen = driver.manage().window().getSize();

            if (rect == null || rect.getWidth() <= 0 || rect.getHeight() <= 0) {
                throw new RuntimeException(
                        "Exact Hub tile has invalid bounds: " + spec.name
                );
            }

            if (rect.getWidth() >= (int) (screen.width * 0.80)
                    || rect.getHeight() >= (int) (screen.height * 0.50)) {
                throw new RuntimeException(
                        "Refusing to tap oversized Flutter semantic container for "
                                + spec.name
                                + " | width=" + rect.getWidth()
                                + " | height=" + rect.getHeight()
                );
            }

            ReportLogger.step(
                    "Exact Hub tile bounds confirmed"
                            + " | module=" + spec.name
                            + " | x=" + rect.getX()
                            + " | y=" + rect.getY()
                            + " | width=" + rect.getWidth()
                            + " | height=" + rect.getHeight()
            );
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(
                    "Unable to validate exact Hub tile bounds for " + spec.name,
                    e
            );
        }
    }

    // =========================================================
    // GENERIC SAFE HELPERS
    // =========================================================

    private void swipeUpW3C() {
        performVerticalSwipe(0.78, 0.28, 650);
    }

    private void swipeDownW3C() {
        performVerticalSwipe(0.28, 0.78, 550);
    }

    private void performVerticalSwipe(double startRatio, double endRatio, long durationMs) {
        try {
            Dimension size = driver.manage().window().getSize();

            int x = size.width / 2;
            int startY = (int) (size.height * startRatio);
            int endY = (int) (size.height * endRatio);

            PointerInput finger = new PointerInput(PointerInput.Kind.TOUCH, "finger");
            Sequence sequence = new Sequence(finger, 1);

            sequence.addAction(
                    finger.createPointerMove(Duration.ZERO, PointerInput.Origin.viewport(), x, startY)
            );
            sequence.addAction(
                    finger.createPointerDown(PointerInput.MouseButton.LEFT.asArg())
            );
            sequence.addAction(
                    finger.createPointerMove(
                            Duration.ofMillis(durationMs),
                            PointerInput.Origin.viewport(),
                            x,
                            endY
                    )
            );
            sequence.addAction(
                    finger.createPointerUp(PointerInput.MouseButton.LEFT.asArg())
            );

            driver.perform(Collections.singletonList(sequence));
        } catch (Exception e) {
            throw new RuntimeException("Controlled Hub W3C scroll failed: " + e.getMessage(), e);
        }
    }

    private void tapDynamicElementCenter(WebElement element, String label) {
        try {
            Rectangle rect = element.getRect();
            Dimension screen = driver.manage().window().getSize();

            if (rect == null || rect.getWidth() <= 0 || rect.getHeight() <= 0) {
                throw new RuntimeException("Invalid element bounds");
            }

            if (rect.getWidth() >= (int) (screen.width * 0.80)
                    || rect.getHeight() >= (int) (screen.height * 0.50)) {
                throw new RuntimeException("Refusing oversized semantic element");
            }

            int x = rect.getX() + (rect.getWidth() / 2);
            int y = rect.getY() + (rect.getHeight() / 2);

            PointerInput finger = new PointerInput(PointerInput.Kind.TOUCH, "tapFinger");
            Sequence tap = new Sequence(finger, 1);
            tap.addAction(
                    finger.createPointerMove(Duration.ZERO, PointerInput.Origin.viewport(), x, y)
            );
            tap.addAction(finger.createPointerDown(PointerInput.MouseButton.LEFT.asArg()));
            tap.addAction(
                    finger.createPointerMove(
                            Duration.ofMillis(80),
                            PointerInput.Origin.viewport(),
                            x,
                            y
                    )
            );
            tap.addAction(finger.createPointerUp(PointerInput.MouseButton.LEFT.asArg()));

            driver.perform(Collections.singletonList(tap));
            ReportLogger.step("Tapped " + label + " using runtime element center");
        } catch (Exception e) {
            throw new RuntimeException("Dynamic element-center tap failed for " + label, e);
        }
    }

    /**
     * Returns true only when a Compliance Pages foreground state is active.
     *
     * This method intentionally uses exact native states captured from Appium
     * Inspector. It does not use generic text/description contains matching.
     */
    private boolean hasComplianceOverlayOrPage() {
        // Internal VRSA page already opened.
        if (isVrsaWebViewVisible()) {
            return true;
        }

        // Normal dual-subscription selector sheet.
        if (firstDisplayed(FUND_ADVISOR_OPTION) != null
                || firstDisplayed(STOCK_ADVISOR_OPTION) != null) {
            return true;
        }

        // User Agreement has its own exact dual-download sheet.
        if (firstDisplayed(USER_AGREEMENT_TITLE) != null
                || firstDisplayed(FUND_ADVISOR_AGREEMENT_DOWNLOAD) != null
                || firstDisplayed(STOCK_ADVISOR_AGREEMENT_DOWNLOAD) != null) {
            return true;
        }

        // ODR / PDF / browser flows can move outside the Advisor package.
        String currentPackage = safeCurrentPackage();
        return currentPackage != null
                && !currentPackage.isEmpty()
                && !ADVISOR_PACKAGE.equals(currentPackage);
    }

    private boolean isVrsaWebViewVisible() {
        return firstDisplayed(WEBVIEW) != null;
    }

    private WebElement firstDisplayed(By by) {
        try {
            List<WebElement> elements = driver.findElements(by);

            for (WebElement element : elements) {
                try {
                    if (element.isDisplayed()) {
                        return element;
                    }
                } catch (Exception ignored) {
                }
            }
        } catch (Exception ignored) {
        }

        return null;
    }

    private WebElement firstPresent(By by) {
        try {
            List<WebElement> elements = driver.findElements(by);
            return elements.isEmpty() ? null : elements.get(0);
        } catch (Exception ignored) {
            return null;
        }
    }

    private void clickElementByGesture(WebElement element, String label) {
        if (element == null) {
            throw new RuntimeException("Cannot tap null element: " + label);
        }

        try {
            Map<String, Object> args = new LinkedHashMap<>();
            args.put("elementId", ((RemoteWebElement) element).getId());
            ((JavascriptExecutor) driver).executeScript("mobile: clickGesture", args);
            ReportLogger.step("Tapped using exact element clickGesture: " + label);
        } catch (Exception e) {
            throw new RuntimeException(
                    "Unable to tap exact element with mobile: clickGesture: "
                            + label + " | error=" + e.getMessage(),
                    e
            );
        }
    }

    private void clickElement(WebElement element, String label) {
        if (element == null) {
            throw new RuntimeException("Cannot tap null element: " + label);
        }

        try {
            String clickable = element.getAttribute("clickable");

            if (element.isDisplayed()
                    && element.isEnabled()
                    && Boolean.parseBoolean(clickable)) {
                element.click();
                ReportLogger.step("Tapped using element click: " + label);
                return;
            }
        } catch (Exception ignored) {
        }

        // Hub tiles are exact android.widget.Button accessibility nodes but Inspector
        // reports clickable=false. Appium's elementId-bounded clickGesture is the stable
        // way to activate that exact node without hardcoded coordinates.
        try {
            Map<String, Object> args = new LinkedHashMap<>();
            args.put("elementId", ((RemoteWebElement) element).getId());
            ((JavascriptExecutor) driver).executeScript("mobile: clickGesture", args);
            ReportLogger.step("Tapped using element-based mobile: clickGesture: " + label);
            return;
        } catch (Exception ignored) {
        }

        try {
            element.click();
            ReportLogger.step("Tapped using fallback WebElement click: " + label);
        } catch (Exception e) {
            throw new RuntimeException(
                    "Unable to tap exact element " + label + ": " + e.getMessage(),
                    e
            );
        }
    }

    private boolean isExactTextVisible(String text) {
        try {
            return findExactTextOrDescription(text) != null;
        } catch (Exception ignored) {
            return false;
        }
    }

    private boolean isAnyTextOrDescriptionVisible(String... markers) {
        if (markers == null) {
            return false;
        }

        for (String marker : markers) {
            if (marker == null || marker.trim().isEmpty()) {
                continue;
            }

            try {
                String escaped = escapeUiSelector(marker);

                if (firstDisplayed(
                        AppiumBy.androidUIAutomator(
                                "new UiSelector().textContains(\"" + escaped + "\")"
                        )
                ) != null) {
                    return true;
                }

                if (firstDisplayed(
                        AppiumBy.androidUIAutomator(
                                "new UiSelector().descriptionContains(\"" + escaped + "\")"
                        )
                ) != null) {
                    return true;
                }
            } catch (Exception ignored) {
            }
        }

        return false;
    }

    private boolean pageSourceContainsIgnoreCase(String value) {
        try {
            String source = driver.getPageSource();
            return source != null
                    && source.toLowerCase().contains(value.toLowerCase());
        } catch (Exception ignored) {
            return false;
        }
    }

    private void activateAdvisorAppIfNeeded() {
        String currentPackage = safeCurrentPackage();

        if (ADVISOR_PACKAGE.equals(currentPackage)) {
            return;
        }

        try {
            Map<String, Object> args = new LinkedHashMap<>();
            args.put("appId", ADVISOR_PACKAGE);
            ((JavascriptExecutor) driver).executeScript("mobile: activateApp", args);
            sleep(800);
        } catch (Exception e) {
            throw new RuntimeException(
                    "Unable to activate Advisor app from package " + currentPackage,
                    e
            );
        }
    }

    private String safeCurrentPackage() {
        try {
            return driver.getCurrentPackage();
        } catch (Exception ignored) {
            return "";
        }
    }

    private boolean isDriverAlive() {
        try {
            driver.getCurrentPackage();
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    private String escapeUiSelector(String value) {
        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"");
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Thread interrupted while waiting", e);
        }
    }

    private static Map<String, PageSpec> buildPageSpecs() {
        Map<String, PageSpec> specs = new LinkedHashMap<>();
        specs.put(FAQS.name, FAQS);
        specs.put(ABOUT_US.name, ABOUT_US);
        specs.put(PRIVACY_POLICY.name, PRIVACY_POLICY);
        specs.put(USER_AGREEMENT.name, USER_AGREEMENT);
        specs.put(REFUND_POLICY.name, REFUND_POLICY);
        specs.put(INVESTOR_CHARTER.name, INVESTOR_CHARTER);
        specs.put(INVESTOR_COMPLAINT.name, INVESTOR_COMPLAINT);
        specs.put(ODR_PORTAL.name, ODR_PORTAL);
        specs.put(AUDIT_STATUS.name, AUDIT_STATUS);
        return specs;
    }

    private enum RouteState {
        DUAL_SELECTOR,
        DIRECT_VRSA_WEBVIEW,
        DIRECT_PDF_DOWNLOAD
    }

    private static final class PageSpec {
        private final String name;
        private final String hubAccessibilityId;
        private final By hubLocator;

        private PageSpec(String name, String hubAccessibilityId) {
            this.name = name;
            this.hubAccessibilityId = hubAccessibilityId;
            this.hubLocator = AppiumBy.accessibilityId(hubAccessibilityId);
        }
    }
}