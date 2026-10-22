package com.valueresearch.pages;

import com.valueresearch.utils.ActionUtils;
import com.valueresearch.utils.AuthHelper;
import com.valueresearch.utils.ReportLogger;
import com.valueresearch.utils.WaitUtils;

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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class PortfolioAnalysisFaPage {

    private final AndroidDriver driver;

    /**
     * Team-lead reports are concise by default. Set -Dpafa.verbose.logs=true only
     * while debugging a flaky run to restore the detailed page-object trace.
     */
    private static final boolean VERBOSE_DIAGNOSTICS =
            Boolean.parseBoolean(System.getProperty("pafa.verbose.logs", "false"));

    /*
     * Additional functional-entry validation is deliberately isolated from the
     * proven PRD bucket flows below. The existing Optimize/Good/New Fund/Steady
     * navigation is not replaced or shortened by these checks.
     */
    private static final String ADVISOR_APP_PACKAGE = "com.valueresearch.advisor";

    /*
     * PAFA_000 uses condition-based explicit waits only. These values are maximum
     * timeouts, not fixed sleeps; execution continues as soon as the required UI
     * state becomes available.
     */
    private static final int FUNCTIONAL_ROUTE_WAIT_SECONDS = 60;
    private static final int FUNCTIONAL_CONTROL_WAIT_SECONDS = 45;
    private static final int FUNCTIONAL_TAP_CONFIRM_WAIT_SECONDS = 15;
    private static final int FUNCTIONAL_SCROLL_CHECK_SECONDS = 2;
    private static final int FUNCTIONAL_SOURCE_MAX_SCROLLS = 18;

    private int functionalHubRecoveryCount = 0;
    private int functionalAppRestartCount = 0;

    /*
     * Login/session verification is tied to the Appium session, not to whichever
     * Flutter page happens to be visible when the next @Test starts.
     *
     * This is critical for combined bucket runs: a previous test can finish or fail
     * while Fund Review/Portfolio Analysis is rebuilding and temporarily expose none
     * of our page-specific accessibility markers. That does NOT mean the user has
     * been logged out. Once this exact Appium session has been positively verified,
     * later tests reuse that fact unless a PIN screen is actually visible.
     */
    private static volatile String verifiedAuthenticatedSessionId;

    /*
     * Navigation stability policy for this module:
     *
     * 1. First wait for a real page marker with WaitUtils.
     * 2. After the marker appears, keep the newly opened Flutter page untouched
     *    for 4.5-5 seconds before the next interaction.
     * 3. Drawers use a shorter settle because they are overlays, not full pages.
     *
     * The fixed settle is intentionally AFTER the explicit wait. It is not being
     * used as a replacement for explicit waits.
     */
    private static final long PAGE_OPEN_SETTLE_MS = 4500L;
    private static final long HEAVY_PAGE_SETTLE_MS = 5000L;
    private static final long DRAWER_SETTLE_MS = 2000L;

    /*
     * Optimize card-by-card navigation timing. Every known fund heading remains
     * an exact checkpoint, so no fund is skipped. The idle time between those
     * checkpoints is deliberately shorter than before to speed up the long
     * ~18-20 fund traversal without increasing the swipe distance.
     *
     * Safety is unchanged: after every small W3C move the exact NEXT fund locator
     * is checked, and the existing reverse/top-recovery logic still activates if
     * Flutter exposes a later checkpoint before the expected one.
     */
    private static final long FUND_CHECKPOINT_SETTLE_MS = 450L;
    private static final long SEARCH_SCROLL_SETTLE_MS = 450L;
    private static final long OPTIMIZE_CARD_SCROLL_DURATION_MS = 700L;

    /*
     * Legacy ordered-scanner limits are intentionally retained because the old
     * private recovery helpers remain in this page object for rollback/reference.
     * PAFA_001 no longer depends on those helpers or on every intermediate fund.
     */
    private static final int MAX_FORWARD_SWIPES_PER_FUND = 20;
    private static final int MAX_REVERSE_SWIPES_PER_FUND = 14;
    private static final int MAX_TOP_RECOVERY_SWIPES = 50;
    private static final int MAX_CARD_BY_CARD_RECOVERY_CYCLES = 4;

    /*
     * Current Optimize regression is TARGET-DRIVEN, like the proven Steady flow.
     * Only the four actual regression targets matter. Any fund inserted/removed
     * between them is ignored while bounded small W3C swipes search for the exact
     * next target accessibility id. No full-list top reset is used mid-regression.
     */
    private static final int OPTIMIZE_MAX_REVERSE_RECOVERY_SWIPES = 12;
    private static final int OPTIMIZE_MAX_INITIAL_REVERSE_SWIPES = 24;
    private static final long OPTIMIZE_TARGET_SEARCH_SETTLE_MS = 500L;

    /* Index within OPTIMIZE_DRAWER_REGRESSION_FUNDS, not the physical UI list. */
    private int optimizeTraversalCheckpointIndex = -1;

    // =========================================================
    // BOTTOM NAVIGATION
    // =========================================================

    private static final By FUNDS_BOTTOM_TAB =
            AppiumBy.accessibilityId("Funds");

    private static final By HUB_BOTTOM_TAB =
            AppiumBy.accessibilityId("Hub");

    private static final By HUB_READY_MARKER =
            AppiumBy.accessibilityId("Investor Accounts");

    private static final By FUNDS_HOME_ATTENTION_CARD =
            AppiumBy.androidUIAutomator(
                    "new UiSelector().descriptionContains(\"Funds that need attention\")"
            );

    /*
     * This suite must never start an Email + OTP login.
     * A PIN screen is only an app lock over an existing authenticated session, so
     * PIN unlock is allowed. A true logged-out/email-login screen is treated as a
     * failed suite precondition instead of consuming credentials from config.
     */
    private static final By PIN_SCREEN_MARKER =
            AppiumBy.androidUIAutomator(
                    "new UiSelector().descriptionContains(\"Enter your Advisor PIN\")"
            );

    // =========================================================
    // HUB -> PORTFOLIO ANALYSIS
    // =========================================================

    private final By portfolioAnalysisTile =
            AppiumBy.accessibilityId("Open portfolio analysis screen");

    private final By portfolioAnalysisHeader =
            AppiumBy.accessibilityId("Portfolio Analysis");

    private final By summaryTab =
            AppiumBy.accessibilityId("Summary, tab 1 of 3");

    private final By fundsSifsTab =
            AppiumBy.accessibilityId("Funds / SIFs, tab 2 of 3");

    /*
     * Flutter can expose the same tab semantics either with the complete
     * "tab X of 3" description or only the visible label during a rebuild.
     * Page identity therefore has prefix fallbacks and does not rely on one
     * transient semantics string.
     */
    private final By summaryTabFlexible =
            AppiumBy.androidUIAutomator(
                    "new UiSelector().descriptionStartsWith(\"Summary\")"
            );

    private final By fundsSifsTabFlexible =
            AppiumBy.androidUIAutomator(
                    "new UiSelector().descriptionStartsWith(\"Funds / SIFs\")"
            );

    /*
     * Exact back-navigation locator supplied from Fund Review Inspector.
     * It reports clickable=false, therefore it is tapped with a W3C centre tap.
     */
    private final By fundReviewGoBackButton =
            AppiumBy.accessibilityId("Go back");

    /*
     * Stable page-load markers captured from the supplied Appium Inspector views.
     * IMPORTANT: page readiness must never depend on whichever dynamic fund happens
     * to be one of the first two rows today. Those rows can change when portfolio
     * data changes, while the page itself is already fully loaded.
     */
    private final By summaryOverallPortfolioPerformance =
            AppiumBy.accessibilityId("Overall Portfolio Performance");

    private final By summaryPortfolioProfile =
            AppiumBy.accessibilityId("Your Portfolio Profile");

    private final By fundsSifsPortfolioInsights =
            AppiumBy.accessibilityId("Portfolio Insights");

    // Functional entry-point anchors supplied from Appium Inspector.
    private final By summaryAttentionHeader =
            AppiumBy.androidUIAutomator(
                    "new UiSelector().descriptionContains(\"Funds that need attention\")"
            );

    private final By exitCardHeader =
            AppiumBy.androidUIAutomator(
                    "new UiSelector().descriptionContains(\"Funds to exit\")"
            );

    private final By goodCardHeader =
            AppiumBy.androidUIAutomator(
                    "new UiSelector().descriptionContains(\"Funds that are good\")"
            );

    private final By steadyCardHeader =
            AppiumBy.androidUIAutomator(
                    "new UiSelector().descriptionContains(\"Funds that are steady\")"
            );

    private final By newFundsCardHeader =
            AppiumBy.androidUIAutomator(
                    "new UiSelector().descriptionContains(\"New funds\")"
            );

    private final By anyMoreChevron =
            AppiumBy.androidUIAutomator(
                    "new UiSelector().descriptionContains(\"more\")"
            );

    private final By summaryHdfcEntryFund =
            AppiumBy.accessibilityId("HDFC Aggressive Hybrid Reg-G");

    private final By summaryTataEntryFund =
            AppiumBy.accessibilityId("Tata Children's Reg");

    /* Latest Summary preview supplied by the tester can expose this Optimize fund. */
    private final By summaryAbslUltraShortEntryFund =
            AppiumBy.accessibilityId("ABSL Ultra Short Term Reg-G");

    private final By summaryExpandedAxisElssFund =
            AppiumBy.accessibilityId("Axis ELSS - Tax Saver Reg-G");

    private final By summaryExpandedMiraeLargeCapFund =
            AppiumBy.accessibilityId("Mirae Asset Large Cap Reg-G");

    // Exact +XX more locators captured from the supplied Inspector screenshots.
    // The dynamic card-header fallback below keeps the test stable if the counts change.
    private final By summaryAttentionMoreCurrent =
            AppiumBy.accessibilityId("+15 more");

    private final By optimizeMoreCurrent =
            AppiumBy.accessibilityId("+16 more");

    private final By goodMoreCurrent =
            AppiumBy.accessibilityId("+3 more");

    private final By steadyMoreCurrent =
            AppiumBy.accessibilityId("+12 more");

    private final By newFundMoreCurrent =
            AppiumBy.accessibilityId("+2 more");

    // Every direct fund locator supplied from Appium Inspector for PAFA_000.
    private final By optimizeDirectEntryFund =
            AppiumBy.accessibilityId("ABSL Corporate Bond Reg-G");

    private final By optimizeSecondEntryFund =
            AppiumBy.accessibilityId("ABSL Liquid Reg-G");

    private final By goodFirstEntryFund =
            AppiumBy.accessibilityId("Edelweiss Technology Dir-G");

    private final By goodDirectEntryFund =
            AppiumBy.accessibilityId("Edelweiss Technology Reg-G");

    private final By steadyDirectEntryFund =
            AppiumBy.accessibilityId("ABSL Transportation and Logistics Dir-G");

    private final By steadySecondEntryFund =
            AppiumBy.accessibilityId("ABSL Transportation And Logistics Reg-G");

    private final By newFundDirectEntryFund =
            AppiumBy.accessibilityId("Axis Nifty500 Low Volatility 50 Index Dir-G");

    private final By newFundSecondEntryFund =
            AppiumBy.accessibilityId("Axis Nifty500 Low Volatility 50 Index Reg-G");

    private final By fundReviewInvestment5Context =
            AppiumBy.androidUIAutomator(
                    "new UiSelector().descriptionContains(\"My Investment - 5\")"
            );

    // =========================================================
    // INVESTOR SELECTOR
    // =========================================================

    private final By investorSelectionDropdown =
            AppiumBy.androidUIAutomator(
                    "new UiSelector().descriptionContains(\"Shows investor selection list\")"
            );

    private final By investment5SelectedDropdown =
            AppiumBy.accessibilityId(
                    "My Investment - 5, Shows investor selection list"
            );

    private final By chooseInvestorTitle =
            AppiumBy.accessibilityId("Choose Investor");

    private final By oldLabels =
            AppiumBy.accessibilityId("Old Labels");

    private final By investment5 =
            AppiumBy.accessibilityId("My Investment - 5");

    // =========================================================
    // FUNDS / SIFS
    // =========================================================

    private final By fundPortfolioPerformance =
            AppiumBy.accessibilityId("Fund Portfolio Performance");

    /*
     * The bucket count is dynamic, so do not hardcode "18" here.
     * Current UI example: "18 Funds that can do better".
     */
    private final By optimizeCardHeader =
            AppiumBy.androidUIAutomator(
                    "new UiSelector().descriptionContains(\"Funds that can do better\")"
            );

    /*
     * Current My Investment - 5 data shows this fund as the first Optimize row.
     * Inspector confirms it is exposed as an accessibility node and can be opened
     * through the right-side lead-in arrow when a normal click is ignored by Flutter.
     */
    private final By optimizeEntryFund =
            AppiumBy.accessibilityId("ABSL Corporate Bond Reg-G");

    /*
     * Entry into Fund Review must happen WITHOUT scrolling the Funds / SIFs page.
     * These fund names have already been observed/shared for My Investment - 5.
     * We use whichever one is actually visible after the cards finish loading.
     */
    private static final String[] FUNDS_SIFS_ENTRY_FUND_NAMES = {
            "HDFC Aggressive Hybrid Reg-G",
            "Tata Children's Reg",
            "ABSL Corporate Bond Reg-G",
            "Axis Liquid Reg-G",
            "ICICI Pru Balanced Advantage-G"
    };

    /*
     * Optimize regression anchors. The first fund is the existing baseline.
     * The next two are middle-page cards and the final fund is the end-page
     * card confirmed through Appium Inspector. Counts/amounts are deliberately
     * not hardcoded because those are dynamic QA data.
     */
    private static final String PRIMARY_OPTIMIZE_FUND_NAME =
            "ABSL Corporate Bond Reg-G";

    private static final String MIDDLE_OPTIMIZE_FUND_1_NAME =
            "Axis ELSS - Tax Saver Reg-G";

    private static final String MIDDLE_OPTIMIZE_FUND_2_NAME =
            "Mirae Asset Large Cap Reg-G";

    private static final String LAST_OPTIMIZE_FUND_NAME =
            "UTI Liquid Reg-G";

    private static final String[] OPTIMIZE_DRAWER_REGRESSION_FUNDS = {
            PRIMARY_OPTIMIZE_FUND_NAME,
            MIDDLE_OPTIMIZE_FUND_1_NAME,
            MIDDLE_OPTIMIZE_FUND_2_NAME,
            LAST_OPTIMIZE_FUND_NAME
    };

    /*
     * Full ordered set of Optimize fund-card accessibility ids shared from
     * Appium Inspector for My Investment - 5. These are used as navigation
     * landmarks only; amounts, units, counts and calculation values remain dynamic.
     *
     * The ordered landmarks let the scroll logic know exactly where it is in the
     * long Flutter list, slow down before a requested card, and detect immediately
     * if a requested card was passed.
     */
    private static final String[] OPTIMIZE_FUNDS_IN_ORDER = {
            "ABSL Corporate Bond Reg-G",
            "ABSL Liquid Reg-G",
            "ABSL Ultra Short Term Reg-G",
            "Axis Banking and PSU Debt Reg-G",
            "Axis ELSS - Tax Saver Reg-G",
            "Axis Liquid Reg-G",
            "HDFC Credit Risk Reg-G",
            "ICICI Pru Aggressive Hybrid-G",
            "ICICI Pru Balanced Advantage-G",
            "ICICI Pru Floating Interest Rates-G",
            "ICICI Pru Large Cap-G",
            "Kotak Arbitrage Reg-G",
            "Kotak Flexi Cap Reg-G",
            "Mirae Asset Large Cap Reg-G",
            "Motilal Oswal Flexi Cap Reg-G",
            "Nippon India Large Cap Reg-G",
            "Nippon India Liquid Reg-G",
            "UTI Liquid Reg-G"
    };

    // =========================================================
    // FUND REVIEW - GOOD BUCKET TEST DATA
    // =========================================================

    /*
     * Current Good bucket order captured from Appium Inspector for My Investment - 5.
     * These fund-name accessibility ids are navigation checkpoints only.
     * We deliberately move one whole fund-card at a time in this exact order.
     */
    private static final String EDELWEISS_TECH_DIR_G =
            "Edelweiss Technology Dir-G";

    private static final String EDELWEISS_TECH_REG_G =
            "Edelweiss Technology Reg-G";

    private static final String ICICI_RURAL_OPPORTUNITIES_IDCW =
            "ICICI Pru Rural Opportunities-IDCW";

    private static final String MOTILAL_LARGE_CAP_DIR_G =
            "Motilal Oswal Large Cap Dir-G";

    private static final String MOTILAL_LARGE_CAP_REG_G =
            "Motilal Oswal Large Cap Reg-G";

    private static final String[] GOOD_FUNDS_IN_ORDER = {
            EDELWEISS_TECH_DIR_G,
            EDELWEISS_TECH_REG_G,
            ICICI_RURAL_OPPORTUNITIES_IDCW,
            MOTILAL_LARGE_CAP_DIR_G,
            MOTILAL_LARGE_CAP_REG_G
    };

    /*
     * Actual Good PRD validation targets only. The physical bucket may contain
     * any number of unrelated funds between these targets.
     */
    private static final String[] GOOD_MAPPED_FUNDS_IN_ORDER = {
            EDELWEISS_TECH_DIR_G,
            EDELWEISS_TECH_REG_G,
            MOTILAL_LARGE_CAP_DIR_G,
            MOTILAL_LARGE_CAP_REG_G
    };

    /*
     * Only the yellow-highlighted Good Start rows from the PRD decision table are
     * in scope for this suite. The manual QA Month/Year values are retained only
     * as diagnostic reference. PRD validation treats [Month Year] as a placeholder
     * and accepts the real app value (for example Apr 2027 or Jul 2028) while all
     * surrounding decision-table wording must still match exactly.
     */
    private static final String PRD_GOOD_START_DIRECT_TEMPLATE =
            "Strong start. Full opinion by [Month Year].";

    private static final String PRD_GOOD_START_REGULAR_TEMPLATE =
            "Strong start, but a cheaper Direct plan is available. Full opinion by [Month Year].";

    private static final GoodAdviceExpectation[] GOOD_START_EXPECTATIONS = {
            new GoodAdviceExpectation(
                    "TC-02",
                    EDELWEISS_TECH_DIR_G,
                    "Sector fund (Technology) | Mutual Fund | NOT overconcentrated",
                    "Good Start",
                    "Direct Plan",
                    PRD_GOOD_START_DIRECT_TEMPLATE,
                    "Strong start. Full opinion by Apr 2027."
            ),
            new GoodAdviceExpectation(
                    "TC-02",
                    EDELWEISS_TECH_REG_G,
                    "Sector fund (Technology) | Mutual Fund | NOT overconcentrated",
                    "Good Start",
                    "Regular Plan",
                    PRD_GOOD_START_REGULAR_TEMPLATE,
                    "Strong start, but a cheaper Direct plan is available. Full opinion by Apr 2027."
            ),
            new GoodAdviceExpectation(
                    "TC-01",
                    MOTILAL_LARGE_CAP_DIR_G,
                    "Mutual Fund | Non-ETF | Non-sector fund",
                    "Good Start",
                    "Direct Plan",
                    PRD_GOOD_START_DIRECT_TEMPLATE,
                    "Strong start. Full opinion by Mar 2027."
            ),
            new GoodAdviceExpectation(
                    "TC-01",
                    MOTILAL_LARGE_CAP_REG_G,
                    "Mutual Fund | Non-ETF | Non-sector fund",
                    "Good Start",
                    "Regular Plan",
                    PRD_GOOD_START_REGULAR_TEMPLATE,
                    "Strong start, but a cheaper Direct plan is available. Full opinion by Mar 2027."
            )
    };

    private static final long GOOD_CARD_SCROLL_SETTLE_MS = 1200L;
    private static final long GOOD_CARD_CHECKPOINT_SETTLE_MS = 650L;
    private static final int GOOD_MAX_TARGET_SEARCH_SWIPES = 56;
    private static final int GOOD_MAX_REVERSE_RECOVERY_SWIPES = 8;
    private static final int GOOD_MAX_ADVICE_POSITION_SWIPES = 10;
    private static final long GOOD_TARGET_SEARCH_SETTLE_MS = 550L;

    /*
     * Index within GOOD_MAPPED_FUNDS_IN_ORDER. It tracks only validation targets,
     * never the physical card position of unrelated funds in the bucket.
     */
    private int goodTraversalCheckpointIndex = -1;

    // =========================================================
    // FUND REVIEW - NEW FUND BUCKET TEST DATA
    // =========================================================

    /*
     * New Fund accessibility ids previously captured from Appium Inspector for
     * My Investment - 5. Abakkus Direct/Regular are the ONLY current PRD targets.
     * 360 ONE and the older Axis/Kotak locators are retained for future reference,
     * but PAFA_003 does not depend on them for scrolling or validation.
     */
    private static final String ONE_360_ELSS_TAX_SAVER_NIFTY_50_INDEX_DIR_G =
            "360 ONE ELSS Tax Saver Nifty 50 Index Dir-G";

    private static final String ONE_360_ELSS_TAX_SAVER_NIFTY_50_INDEX_REG_G =
            "360 ONE ELSS Tax Saver Nifty 50 Index Reg-G";

    private static final String ABAKKUS_LARGE_MID_CAP_DIR_G =
            "Abakkus Large & Mid Cap Dir-G";

    private static final String ABAKKUS_LARGE_MID_CAP_REG_G =
            "Abakkus Large & Mid Cap Reg-G";

    // Retained from the previous implementation; not asserted in the current PAFA_003 scope.
    private static final String AXIS_NIFTY500_LOW_VOL_DIR_G =
            "Axis Nifty500 Low Volatility 50 Index Dir-G";

    private static final String AXIS_NIFTY500_LOW_VOL_REG_G =
            "Axis Nifty500 Low Volatility 50 Index Reg-G";

    private static final String KOTAK_MULTI_SECTOR_OMNI_FOF_DIR_G =
            "Kotak Multi Sector Omni FoF Dir-G";

    private static final String KOTAK_MULTI_SECTOR_OMNI_FOF_REG_G =
            "Kotak Multi Sector Omni FoF Reg-G";

    /*
     * Legacy observed order retained for reference only. Current target-driven
     * PAFA_003 does not traverse this array.
     */
    private static final String[] NEW_FUND_FUNDS_IN_ORDER = {
            ONE_360_ELSS_TAX_SAVER_NIFTY_50_INDEX_DIR_G,
            ONE_360_ELSS_TAX_SAVER_NIFTY_50_INDEX_REG_G,
            ABAKKUS_LARGE_MID_CAP_DIR_G,
            ABAKKUS_LARGE_MID_CAP_REG_G
    };

    /*
     * Actual PAFA_003 validation targets. 360 ONE/older Axis/Kotak locators are
     * retained above for context/future reuse, but scrolling no longer depends on
     * them being immediately before Abakkus.
     */
    private static final String[] NEW_FUND_MAPPED_FUNDS_IN_ORDER = {
            ABAKKUS_LARGE_MID_CAP_DIR_G,
            ABAKKUS_LARGE_MID_CAP_REG_G
    };

    /*
     * PRD New Fund decision-table text. Month/Year is intentionally NOT hardcoded:
     * the PRD makes it data-dependent, and the supplied manual QA mapping does not
     * provide the launch/objective-change date needed to independently calculate it.
     * We therefore require a real Month Year token to exist in the UI advice.
     */
    private static final String PRD_NEW_FUND_DIRECT_TEMPLATE =
            "Too early to evaluate. Opinion available by [Month Year].";

    private static final String PRD_NEW_FUND_REGULAR_TEMPLATE =
            "Too early to evaluate, but a cheaper Direct plan is available. Opinion available by [Month Year].";

    private static final String MONTH_YEAR_REGEX =
            "(?:Jan(?:uary)?|Feb(?:ruary)?|Mar(?:ch)?|Apr(?:il)?|May|Jun(?:e)?|Jul(?:y)?|Aug(?:ust)?|Sep(?:t(?:ember)?)?|Oct(?:ober)?|Nov(?:ember)?|Dec(?:ember)?)\\s+\\d{4}";

    private static final String NEW_FUND_DIRECT_REGEX =
            "^Too early to evaluate\\. Opinion available by "
                    + MONTH_YEAR_REGEX
                    + "\\.$";

    private static final String NEW_FUND_REGULAR_REGEX =
            "^Too early to evaluate, but a cheaper Direct plan is available\\. Opinion available by "
                    + MONTH_YEAR_REGEX
                    + "\\.$";

    private static final NewFundAdviceExpectation[] NEW_FUND_ADVICE_EXPECTATIONS = {
            new NewFundAdviceExpectation(
                    "PRD New Funds table",
                    ABAKKUS_LARGE_MID_CAP_DIR_G,
                    "New Fund",
                    "Direct Plan",
                    PRD_NEW_FUND_DIRECT_TEMPLATE,
                    NEW_FUND_DIRECT_REGEX,
                    "Post-NFO fund under the Value Research opinion framework"
            ),
            new NewFundAdviceExpectation(
                    "PRD New Funds table",
                    ABAKKUS_LARGE_MID_CAP_REG_G,
                    "New Fund",
                    "Regular Plan",
                    PRD_NEW_FUND_REGULAR_TEMPLATE,
                    NEW_FUND_REGULAR_REGEX,
                    "Post-NFO fund under the Value Research opinion framework"
            )
    };

    private static final long NEW_FUND_CARD_SCROLL_SETTLE_MS = 1000L;
    private static final long NEW_FUND_CARD_CHECKPOINT_SETTLE_MS = 600L;
    private static final int NEW_FUND_MAX_TARGET_SEARCH_SWIPES = 36;
    private static final int NEW_FUND_MAX_REVERSE_RECOVERY_SWIPES = 6;
    private static final int NEW_FUND_MAX_ADVICE_POSITION_SWIPES = 8;
    private static final long NEW_FUND_TARGET_SEARCH_SETTLE_MS = 500L;

    /*
     * Separate state from Optimize and Good so the earlier proven flows remain
     * untouched while New Fund is isolated and stabilised.
     */
    private int newFundTraversalCheckpointIndex = -1;

    // =========================================================
    // FUND REVIEW - STEADY BUCKET TEST DATA
    // =========================================================

    /*
     * Steady validation is driven only by the supplied manual QA mapping (TC-03 to
     * TC-08). These are the only fund headings automation needs to know.
     *
     * IMPORTANT: traversal is TARGET-DRIVEN, not physical-card-order-driven.
     * Unrelated/new Steady funds may appear anywhere between two mapped funds and do
     * not need locators. The scanner keeps making small bounded W3C swipes until the
     * exact next mapped accessibility id enters the safe capture band.
     */
    private static final String ABSL_TRANSPORTATION_LOGISTICS_DIR_G =
            "ABSL Transportation and Logistics Dir-G";

    private static final String ABSL_TRANSPORTATION_LOGISTICS_REG_G =
            "ABSL Transportation And Logistics Reg-G";

    private static final String JIOBLACKROCK_MONEY_MARKET_DIR_G =
            "JioBlackRock Money Market Dir-G";

    private static final String JIOBLACKROCK_MONEY_MARKET_REG_G =
            "JioBlackRock Money Market Reg-G";

    private static final String KOTAK_NIFTY_COMMODITIES_DIR_G =
            "Kotak Nifty Commodities Index Dir-G";

    private static final String KOTAK_NIFTY_COMMODITIES_REG_G =
            "Kotak Nifty Commodities Index Reg-G";

    private static final String KOTAK_TRANSPORTATION_LOGISTICS_DIR_G =
            "Kotak Transportation & Logistics Dir-G";

    private static final String KOTAK_TRANSPORTATION_LOGISTICS_REG_G =
            "Kotak Transportation & Logistics Reg-G";

    private static final String SAMCO_LARGE_MID_CAP_DIR_G =
            "Samco Large & Mid Cap Dir-G";

    private static final String SAMCO_LARGE_MID_CAP_REG_G =
            "Samco Large & Mid Cap Reg-G";

    private static final String SBI_BSE_PSU_BANK_INDEX_DIR_G =
            "SBI BSE PSU Bank Index Dir-G";

    private static final String SBI_BSE_PSU_BANK_INDEX_REG_G =
            "SBI BSE PSU Bank Index Reg-G";

    /*
     * Validation order only. Intermediate cards are deliberately NOT represented.
     * This keeps PAFA_004 stable when the product inserts/removes unrelated Steady
     * funds between these mapped Direct/Regular targets.
     */
    private static final String[] STEADY_MAPPED_FUNDS_IN_ORDER = {
            ABSL_TRANSPORTATION_LOGISTICS_DIR_G,
            ABSL_TRANSPORTATION_LOGISTICS_REG_G,
            JIOBLACKROCK_MONEY_MARKET_DIR_G,
            JIOBLACKROCK_MONEY_MARKET_REG_G,
            KOTAK_NIFTY_COMMODITIES_DIR_G,
            KOTAK_NIFTY_COMMODITIES_REG_G,
            KOTAK_TRANSPORTATION_LOGISTICS_DIR_G,
            KOTAK_TRANSPORTATION_LOGISTICS_REG_G,
            SAMCO_LARGE_MID_CAP_DIR_G,
            SAMCO_LARGE_MID_CAP_REG_G,
            SBI_BSE_PSU_BANK_INDEX_DIR_G,
            SBI_BSE_PSU_BANK_INDEX_REG_G
    };

    private static final String STEADY_MONTH_YEAR_REGEX =
            "(?:Jan(?:uary)?|Feb(?:ruary)?|Mar(?:ch)?|Apr(?:il)?|May|Jun(?:e)?|Jul(?:y)?|Aug(?:ust)?|Sep(?:t(?:ember)?)?|Oct(?:ober)?|Nov(?:ember)?|Dec(?:ember)?)\\s+\\d{4}";

    private static final String PRD_STEADY_START_DIRECT_TEMPLATE =
            "Performing okay so far. Full opinion by [Month Year].";

    private static final String PRD_STEADY_START_REGULAR_TEMPLATE =
            "Performing okay so far, but a cheaper Direct plan is available. Full opinion by [Month Year].";

    private static final String PRD_POOR_START_ACTIVE_DIRECT_TEMPLATE =
            "Weak early performance. Stay invested for now. We're watching this closely. Full opinion by [Month Year].";

    private static final String PRD_POOR_START_ACTIVE_REGULAR_TEMPLATE =
            "Weak early performance, and a cheaper Direct plan is available. Full opinion by [Month Year].";

    private static final String PRD_POOR_START_PASSIVE_DIRECT_TEMPLATE =
            "Not tracking its index well so far. Stay invested for now. We're watching this closely. Full opinion by [Month Year].";

    private static final String PRD_POOR_START_PASSIVE_REGULAR_TEMPLATE =
            "Not tracking its index well so far, and a cheaper Direct plan is available. Full opinion by [Month Year].";

    private static final String STEADY_START_DIRECT_REGEX =
            "^Performing okay so far\\. Full opinion by " + STEADY_MONTH_YEAR_REGEX + "\\.$";

    private static final String STEADY_START_REGULAR_REGEX =
            "^Performing okay so far, but a cheaper Direct plan is available\\. Full opinion by "
                    + STEADY_MONTH_YEAR_REGEX + "\\.$";

    private static final String POOR_START_ACTIVE_DIRECT_REGEX =
            "^Weak early performance\\. Stay invested for now\\. We're watching this closely\\. Full opinion by "
                    + STEADY_MONTH_YEAR_REGEX + "\\.$";

    private static final String POOR_START_ACTIVE_REGULAR_REGEX =
            "^Weak early performance, and a cheaper Direct plan is available\\. Full opinion by "
                    + STEADY_MONTH_YEAR_REGEX + "\\.$";

    private static final String POOR_START_PASSIVE_DIRECT_REGEX =
            "^Not tracking its index well so far\\. Stay invested for now\\. We're watching this closely\\. Full opinion by "
                    + STEADY_MONTH_YEAR_REGEX + "\\.$";

    private static final String POOR_START_PASSIVE_REGULAR_REGEX =
            "^Not tracking its index well so far, and a cheaper Direct plan is available\\. Full opinion by "
                    + STEADY_MONTH_YEAR_REGEX + "\\.$";

    private static final SteadyAdviceExpectation[] STEADY_ADVICE_EXPECTATIONS = {
            new SteadyAdviceExpectation(
                    "TC-06",
                    ABSL_TRANSPORTATION_LOGISTICS_DIR_G,
                    "Aditya Birla Sun Life Transportation and Logistics Fund",
                    "Sector fund | MF/ETF | NOT overconcentrated | Auto and Transportation",
                    "Steady Start",
                    "Not applicable",
                    "Direct Plan",
                    PRD_STEADY_START_DIRECT_TEMPLATE,
                    STEADY_START_DIRECT_REGEX,
                    "Performing okay so far. Full opinion by Dec 2026.",
                    "PASS"
            ),
            new SteadyAdviceExpectation(
                    "TC-06",
                    ABSL_TRANSPORTATION_LOGISTICS_REG_G,
                    "Aditya Birla Sun Life Transportation and Logistics Fund",
                    "Sector fund | MF/ETF | NOT overconcentrated | Auto and Transportation",
                    "Steady Start",
                    "Not applicable",
                    "Regular Plan",
                    PRD_STEADY_START_REGULAR_TEMPLATE,
                    STEADY_START_REGULAR_REGEX,
                    "Performing okay so far, but a cheaper Direct plan is available. Full opinion by Dec 2026.",
                    "PASS"
            ),
            new SteadyAdviceExpectation(
                    "TC-03",
                    JIOBLACKROCK_MONEY_MARKET_DIR_G,
                    "JioBlackRock Money Market Fund",
                    "Mutual Fund | Excluding ETFs and sector funds",
                    "Steady Start",
                    "Not applicable",
                    "Direct Plan",
                    PRD_STEADY_START_DIRECT_TEMPLATE,
                    STEADY_START_DIRECT_REGEX,
                    "Performing okay so far. Full opinion by Feb 2027.",
                    "PASS"
            ),
            new SteadyAdviceExpectation(
                    "TC-03",
                    JIOBLACKROCK_MONEY_MARKET_REG_G,
                    "JioBlackRock Money Market Fund",
                    "Mutual Fund | Excluding ETFs and sector funds",
                    "Steady Start",
                    "Not applicable",
                    "Regular Plan",
                    PRD_STEADY_START_REGULAR_TEMPLATE,
                    STEADY_START_REGULAR_REGEX,
                    "Performing okay so far, but a cheaper Direct plan is available. Full opinion by Feb 2027.",
                    "PASS"
            ),
            new SteadyAdviceExpectation(
                    "TC-05",
                    KOTAK_NIFTY_COMMODITIES_DIR_G,
                    "Kotak Nifty Commodities Index Fund",
                    "Mutual Fund | Excluding ETFs and sector funds",
                    "Poor Start",
                    "Passive Fund",
                    "Direct Plan",
                    PRD_POOR_START_PASSIVE_DIRECT_TEMPLATE,
                    POOR_START_PASSIVE_DIRECT_REGEX,
                    "Weak early performance. Stay invested for now. We're watching this closely. Full opinion by Oct 2026.",
                    "FAIL"
            ),
            new SteadyAdviceExpectation(
                    "TC-05",
                    KOTAK_NIFTY_COMMODITIES_REG_G,
                    "Kotak Nifty Commodities Index Fund",
                    "Mutual Fund | Excluding ETFs and sector funds",
                    "Poor Start",
                    "Passive Fund",
                    "Regular Plan",
                    PRD_POOR_START_PASSIVE_REGULAR_TEMPLATE,
                    POOR_START_PASSIVE_REGULAR_REGEX,
                    "Weak early performance, and a cheaper Direct plan is available. Full opinion by Oct 2026.",
                    "FAIL"
            ),
            new SteadyAdviceExpectation(
                    "TC-07",
                    KOTAK_TRANSPORTATION_LOGISTICS_DIR_G,
                    "Kotak Transportations & Logistics Fund",
                    "Sector fund | MF/ETF | NOT overconcentrated | Transportation & Logistics",
                    "Poor Start",
                    "Active Fund",
                    "Direct Plan",
                    PRD_POOR_START_ACTIVE_DIRECT_TEMPLATE,
                    POOR_START_ACTIVE_DIRECT_REGEX,
                    "Weak early performance. Stay invested for now. We're watching this closely. Full opinion by Jan 2028.",
                    "PASS"
            ),
            new SteadyAdviceExpectation(
                    "TC-07",
                    KOTAK_TRANSPORTATION_LOGISTICS_REG_G,
                    "Kotak Transportations & Logistics Fund",
                    "Sector fund | MF/ETF | NOT overconcentrated | Transportation & Logistics",
                    "Poor Start",
                    "Active Fund",
                    "Regular Plan",
                    PRD_POOR_START_ACTIVE_REGULAR_TEMPLATE,
                    POOR_START_ACTIVE_REGULAR_REGEX,
                    "Weak early performance, and a cheaper Direct plan is available. Full opinion by Jan 2028.",
                    "PASS"
            ),
            new SteadyAdviceExpectation(
                    "TC-04",
                    SAMCO_LARGE_MID_CAP_DIR_G,
                    "Samco Large & Mid Cap Fund",
                    "Mutual Fund | Excluding ETFs and sector funds",
                    "Poor Start",
                    "Active Fund",
                    "Direct Plan",
                    PRD_POOR_START_ACTIVE_DIRECT_TEMPLATE,
                    POOR_START_ACTIVE_DIRECT_REGEX,
                    "Weak early performance. Stay invested for now. We're watching this closely. Full opinion by Jul 2028.",
                    "PASS"
            ),
            new SteadyAdviceExpectation(
                    "TC-04",
                    SAMCO_LARGE_MID_CAP_REG_G,
                    "Samco Large & Mid Cap Fund",
                    "Mutual Fund | Excluding ETFs and sector funds",
                    "Poor Start",
                    "Active Fund",
                    "Regular Plan",
                    PRD_POOR_START_ACTIVE_REGULAR_TEMPLATE,
                    POOR_START_ACTIVE_REGULAR_REGEX,
                    "Weak early performance, and a cheaper Direct plan is available. Full opinion by Jul 2028.",
                    "PASS"
            ),
            new SteadyAdviceExpectation(
                    "TC-08",
                    SBI_BSE_PSU_BANK_INDEX_DIR_G,
                    "SBI BSE PSU Bank Index Fund",
                    "Sector fund | MF/ETF | NOT overconcentrated | PSU Bank",
                    "Poor Start",
                    "Passive Fund",
                    "Direct Plan",
                    PRD_POOR_START_PASSIVE_DIRECT_TEMPLATE,
                    POOR_START_PASSIVE_DIRECT_REGEX,
                    "Weak early performance. Stay invested for now. We're watching this closely. Full opinion by Oct 2026.",
                    "FAIL"
            ),
            new SteadyAdviceExpectation(
                    "TC-08",
                    SBI_BSE_PSU_BANK_INDEX_REG_G,
                    "SBI BSE PSU Bank Index Fund",
                    "Sector fund | MF/ETF | NOT overconcentrated | PSU Bank",
                    "Poor Start",
                    "Passive Fund",
                    "Regular Plan",
                    PRD_POOR_START_PASSIVE_REGULAR_TEMPLATE,
                    POOR_START_PASSIVE_REGULAR_REGEX,
                    "Weak early performance, and a cheaper Direct plan is available. Full opinion by Oct 2026.",
                    "FAIL"
            )
    };

    /*
     * Steady cards are tall and Flutter can publish a heading before/after it is
     * visually usable. Search and validation therefore use TWO different bands:
     *
     * 1. CAPTURE band -> broad safe viewport. While searching for the next MAPPED
     *    fund, every unrelated/unknown card is ignored. Scrolling stops as soon as
     *    the exact mapped accessibility id enters this safe band.
     *
     * 2. ADVICE band -> upper viewport. After a mapped fund is captured, position
     *    only that same fund with tiny W3C moves until its Our Advice area can be
     *    read. Search for the next mapped fund starts only after validation finishes.
     *
     * A later mapped target appearing before the expected target is treated as real
     * overshoot evidence and triggers bounded small reverse recovery. No fling and
     * no coordinate jump based on an off-screen Flutter node is used.
     */
    private static final long STEADY_SMALL_SCROLL_SETTLE_MS = 550L;
    private static final long STEADY_CARD_CHECKPOINT_SETTLE_MS = 500L;
    private static final int STEADY_MAX_TARGET_SEARCH_SWIPES = 36;
    private static final int STEADY_MAX_REVERSE_RECOVERY_SWIPES = 6;
    private static final int STEADY_MAX_ADVICE_POSITION_SWIPES = 8;

    // Forward search moves about 12% of the screen per gesture.
    private static final double STEADY_SMALL_SCROLL_START_FRACTION = 0.80;
    private static final double STEADY_SMALL_SCROLL_END_FRACTION = 0.68;

    // Advice positioning/reverse recovery moves about 8% of the screen.
    private static final double STEADY_FINE_SCROLL_UP_START_FRACTION = 0.72;
    private static final double STEADY_FINE_SCROLL_UP_END_FRACTION = 0.64;
    private static final double STEADY_FINE_SCROLL_DOWN_START_FRACTION = 0.40;
    private static final double STEADY_FINE_SCROLL_DOWN_END_FRACTION = 0.48;

    private int steadyTraversalCheckpointIndex = -1;
    private int steadyControlledSwipeCount = 0;
    private int steadyReverseRecoverySwipeCount = 0;

    // =========================================================
    // FUND REVIEW - GOOD
    // =========================================================

    private final By goodBucketTab =
            AppiumBy.androidUIAutomator(
                    "new UiSelector().descriptionStartsWith(\"Good (\")"
            );

    private final By goodReviewHeader =
            AppiumBy.accessibilityId("Funds that are good");

    private final By ourAdviceLabel =
            AppiumBy.accessibilityId("Our Advice");

    /*
     * Both Direct and Regular Good Start advice begin with "Strong start".
     * We anchor the repeated advice text to the current fund's own "Our Advice"
     * label by live Y-coordinate; no instance() or absolute XPath is used.
     */
    private final By strongStartAdviceText =
            AppiumBy.androidUIAutomator(
                    "new UiSelector().descriptionStartsWith(\"Strong start\")"
            );

    // =========================================================
    // FUND REVIEW - NEW FUND
    // =========================================================

    private final By newFundBucketTab =
            AppiumBy.androidUIAutomator(
                    "new UiSelector().descriptionStartsWith(\"New Fund (\")"
            );

    private final By newFundReviewHeader =
            AppiumBy.accessibilityId("New funds");

    /*
     * Current app text is intentionally located by prefix so this automation can
     * read the actual advice even when the required PRD Month/Year suffix is absent.
     * The exact PRD validation happens after the card is anchored.
     */
    private final By newFundAdviceText =
            AppiumBy.androidUIAutomator(
                    "new UiSelector().descriptionStartsWith(\"Too early to evaluate\")"
            );

    // =========================================================
    // FUND REVIEW - STEADY
    // =========================================================

    private final By steadyBucketTab =
            AppiumBy.androidUIAutomator(
                    "new UiSelector().descriptionStartsWith(\"Steady (\")"
            );

    private final By steadyReviewHeader =
            AppiumBy.accessibilityId("Funds that are steady");

    private final By steadyPerformingOkayAdviceText =
            AppiumBy.androidUIAutomator(
                    "new UiSelector().descriptionStartsWith(\"Performing okay so far\")"
            );

    private final By steadyWeakEarlyAdviceText =
            AppiumBy.androidUIAutomator(
                    "new UiSelector().descriptionStartsWith(\"Weak early performance\")"
            );

    private final By steadyNotTrackingAdviceText =
            AppiumBy.androidUIAutomator(
                    "new UiSelector().descriptionStartsWith(\"Not tracking its index well so far\")"
            );

    // =========================================================
    // FUND REVIEW - OPTIMIZE
    // =========================================================

    private final By fundReviewHeader =
            AppiumBy.accessibilityId("Fund Review");

    private final By exitReviewHeader =
            AppiumBy.androidUIAutomator(
                    "new UiSelector().descriptionContains(\"Funds to exit\")"
            );

    /*
     * UI currently renders the tab label as "Optimise (XX)" while the PRD calls
     * the bucket "Optimize". Keep both spellings as fallbacks and ignore the count.
     */
    private final By optimiseBucketTab =
            AppiumBy.androidUIAutomator(
                    "new UiSelector().descriptionStartsWith(\"Optimise\")"
            );

    private final By optimizeBucketTabFallback =
            AppiumBy.androidUIAutomator(
                    "new UiSelector().descriptionStartsWith(\"Optimize\")"
            );

    private final By optimizeReviewHeader =
            AppiumBy.accessibilityId("Funds that can do better");

    private final By whyOptimize =
            AppiumBy.accessibilityId("Why Optimize?");

    private final By alreadyPaidExtra =
            AppiumBy.accessibilityId("Already paid extra");

    private final By next12Months =
            AppiumBy.accessibilityId("Next 12 months");

    private final By next3Years =
            AppiumBy.accessibilityId("Next 3 years");

    private final By howIsThisCalculated =
            AppiumBy.accessibilityId("How is this calculated?");

    // =========================================================
    // PRD - CALCULATION DRAWER CONTENT
    // =========================================================

    private static final String DRAWER_ALREADY_PAID_EXTRA_TEXT =
            "Already paid extra: The additional corpus you could have today if the same investments had gone into "
                    + "the Direct plan of the same fund. This reflects the impact of the expense difference on corpus "
                    + "growth, not just the total expenses paid.";

    private static final String DRAWER_NEXT_12_MONTHS_AND_3_YEARS_TEXT =
            "Next 12 months / Next 3 years: Illustrative projections of how this corpus difference could evolve if "
                    + "you continue with the Regular plan. Both assume the fund delivers returns similar to its last 3 "
                    + "years. Actual outcomes will depend on how the fund performs going forward. For funds younger than "
                    + "3 years, we use since-launch annualised returns. For funds less than 1 year old, these projections "
                    + "are not shown.";

    private static final String DRAWER_ANNUAL_EXPENSE_HEADING =
            "Why does this look bigger than the annual expense difference?";

    private static final String DRAWER_ANNUAL_EXPENSE_TEXT =
            "The annual figure reflects the extra expenses in one year, based on your current investment value. "
                    + "The three boxes show the cumulative impact over time. A small annual expense difference can "
                    + "translate into a much larger corpus gap over many years, because the amount that would otherwise "
                    + "be saved would itself have earned returns each year.";

    private final By drawerAlreadyPaidExtraText =
            AppiumBy.accessibilityId(DRAWER_ALREADY_PAID_EXTRA_TEXT);

    private final By drawerNext12MonthsAnd3YearsText =
            AppiumBy.accessibilityId(DRAWER_NEXT_12_MONTHS_AND_3_YEARS_TEXT);

    private final By drawerAnnualExpenseHeading =
            AppiumBy.accessibilityId(DRAWER_ANNUAL_EXPENSE_HEADING);

    private final By drawerAnnualExpenseText =
            AppiumBy.accessibilityId(DRAWER_ANNUAL_EXPENSE_TEXT);

    // =========================================================
    // CONSTRUCTOR
    // =========================================================

    public PortfolioAnalysisFaPage(AndroidDriver driver) {
        this.driver = driver;
    }

    private void diagnostic(String message) {
        if (VERBOSE_DIAGNOSTICS) {
            ReportLogger.debug(message);
        }
    }

    // =========================================================
    // LOGIN
    // =========================================================

    public void ensureAdvisorAppLoggedInForPortfolioAnalysisFa() {
        diagnostic(
                "Checking Android driver and existing Advisor authenticated session "
                        + "for PortfolioAnalysisFa | Email/OTP login=DISABLED"
        );

        if (driver == null || driver.getSessionId() == null) {
            throw new AssertionError(
                    "Android driver/session is not available for PortfolioAnalysisFa"
            );
        }

        String currentSessionId = String.valueOf(driver.getSessionId());

        diagnostic(
                "Android driver session is alive | sessionId=" + currentSessionId
        );

        /*
         * If the app has locked again, handle the PIN first even when this Appium
         * session was authenticated earlier. AuthHelper is invoked ONLY after the
         * exact Advisor PIN marker is visible, so this suite never intentionally
         * enters the Email/OTP branch.
         */
        if (isDisplayedNow(PIN_SCREEN_MARKER)) {
            unlockExistingAdvisorSessionWithPinOnly(currentSessionId);
            return;
        }

        /*
         * FAST PATH FOR PAFA_002 / PAFA_003
         *
         * PAFA_001 already proved that this exact Appium session belongs to an
         * authenticated Advisor user. Do not re-classify login state from transient
         * page markers after every test. A failed/slow Flutter navigation can leave
         * the hierarchy between pages for several seconds; that is navigation state,
         * not authentication state.
         */
        if (currentSessionId.equals(verifiedAuthenticatedSessionId)) {
            long quickGuardDeadline =
                    System.nanoTime() + Duration.ofSeconds(2).toNanos();

            while (System.nanoTime() < quickGuardDeadline) {
                if (isDisplayedNow(PIN_SCREEN_MARKER)) {
                    unlockExistingAdvisorSessionWithPinOnly(currentSessionId);
                    return;
                }

                if (isPortfolioAnalysisFaAuthenticatedStateVisible()) {
                    diagnostic(
                            "Same previously verified authenticated Advisor session is active; "
                                    + "continuing immediately without PIN/Email/OTP login"
                    );
                    return;
                }

                sleep(200);
            }

            diagnostic(
                    "Same Appium session was already authenticated earlier in this suite; "
                            + "current Flutter page is between known markers, so login is not re-attempted "
                            + "| sessionId=" + currentSessionId
            );
            return;
        }

        /*
         * FIRST AUTHENTICATION CHECK FOR A NEW APPIUM SESSION
         *
         * Only two states are accepted:
         *   1. a known authenticated Advisor screen; or
         *   2. the Advisor PIN lock screen for an existing account session.
         *
         * Email/OTP is deliberately never initiated by this module.
         */
        final long stateWaitMs = 30000L;
        final long pollMs = 300L;
        final long deadline =
                System.nanoTime() + Duration.ofMillis(stateWaitMs).toNanos();

        diagnostic(
                "First authentication check for this Appium session: waiting up to 30 seconds "
                        + "for authenticated Advisor state or Advisor PIN | Email/OTP will not be attempted"
        );

        while (System.nanoTime() < deadline) {
            if (isPortfolioAnalysisFaAuthenticatedStateVisible()) {
                verifiedAuthenticatedSessionId = currentSessionId;

                diagnostic(
                        "Authenticated Advisor state verified for current Appium session; "
                                + "later PAFA tests will reuse this session without repeating login detection"
                );
                return;
            }

            if (isDisplayedNow(PIN_SCREEN_MARKER)) {
                unlockExistingAdvisorSessionWithPinOnly(currentSessionId);
                return;
            }

            sleep(pollMs);
        }

        ReportLogger.fail(
                "PortfolioAnalysisFa precondition failed for a NEW Appium session: no authenticated "
                        + "Advisor marker or PIN screen became visible within 30 seconds. "
                        + "Email/OTP login is disabled for this suite."
        );

        throw new AssertionError(
                "Unable to establish the initial authenticated Advisor session. "
                        + "Email + OTP login is intentionally disabled for PortfolioAnalysisFa. "
                        + "Log in manually once (or leave the Advisor PIN screen available) and rerun."
        );
    }

    private void unlockExistingAdvisorSessionWithPinOnly(String currentSessionId) {
        diagnostic(
                "Advisor PIN lock screen detected; unlocking existing session only "
                        + "| Email/OTP login remains disabled for this suite"
        );

        /*
         * IMPORTANT: do not call AuthHelper unless the exact PIN marker is visible.
         * This prevents this module from deliberately entering Email + OTP login.
         */
        new AuthHelper(driver).ensureLoggedIn();

        final long postPinDeadline =
                System.nanoTime() + Duration.ofSeconds(25).toNanos();

        while (System.nanoTime() < postPinDeadline) {
            if (isPortfolioAnalysisFaAuthenticatedStateVisible()) {
                verifiedAuthenticatedSessionId = currentSessionId;
                settleAfterPageOpen("logged-in Advisor screen after PIN unlock");

                diagnostic(
                        "Existing Advisor session unlocked with PIN and marked verified "
                                + "for this Appium session; no Email/OTP login used"
                );
                return;
            }

            sleep(250);
        }

        throw new AssertionError(
                "PIN was entered but no authenticated Advisor marker became visible within 25 seconds"
        );
    }

    private boolean isPortfolioAnalysisFaAuthenticatedStateVisible() {
        return isDisplayedNow(FUNDS_BOTTOM_TAB)
                || isDisplayedNow(HUB_BOTTOM_TAB)
                || isDisplayedNow(HUB_READY_MARKER)
                || isDisplayedNow(portfolioAnalysisHeader)
                || isDisplayedNow(fundReviewHeader)
                || isDisplayedNow(optimizeReviewHeader)
                || isDisplayedNow(goodReviewHeader)
                || isDisplayedNow(newFundReviewHeader)
                || isDisplayedNow(steadyReviewHeader);
    }

    // =========================================================
    // OPEN HUB
    // =========================================================

    public void openHubFromBottomNavigationForPortfolioAnalysisFa() {
        diagnostic("Opening Hub from bottom navigation for PortfolioAnalysisFa");

        if (isVisibleWithin(HUB_READY_MARKER, 2)) {
            settleAfterPageOpen("Hub");
            diagnostic("Hub page is already open");
            return;
        }

        /*
         * In the combined suite the previous test can intentionally finish deep inside
         * Fund Review. Bottom navigation is not visible there. Do not restart the app
         * and do not invoke login. Simply unwind the authenticated navigation stack
         * until either Hub itself or the bottom navigation becomes available.
         */
        if (!isDisplayedNow(HUB_BOTTOM_TAB)) {
            diagnostic(
                    "Current authenticated page does not expose bottom navigation; "
                            + "unwinding to a navigable app level without login/restart"
            );

            for (int attempt = 1; attempt <= 5; attempt++) {
                if (isVisibleWithin(HUB_READY_MARKER, 1)) {
                    settleAfterPageOpen("Hub");
                    diagnostic(
                            "Hub reached while unwinding previous test page "
                                    + "| backAttempts=" + (attempt - 1)
                    );
                    return;
                }

                if (isDisplayedNow(HUB_BOTTOM_TAB)) {
                    break;
                }

                diagnostic(
                        "Back navigation to expose bottom navigation"
                                + " | attempt=" + attempt + "/5"
                );
                if (isDisplayedNow(fundReviewHeader)
                        && isDisplayedNow(fundReviewGoBackButton)) {
                    tapFundReviewGoBackButton();
                } else {
                    pressBackSafely();
                }
                settleAfterNavigation();
            }
        }

        if (isVisibleWithin(HUB_READY_MARKER, 1)) {
            settleAfterPageOpen("Hub");
            diagnostic("Hub reached after authenticated navigation unwind");
            return;
        }

        if (!isDisplayedNow(HUB_BOTTOM_TAB)) {
            throw new AssertionError(
                    "Authenticated Advisor session is valid, but Hub bottom navigation "
                            + "could not be exposed after controlled Back navigation"
            );
        }

        ActionUtils.click(driver, HUB_BOTTOM_TAB);
        WaitUtils.waitForElement(driver, HUB_READY_MARKER, 30);

        settleAfterPageOpen("Hub");

        diagnostic("Hub page opened successfully");
    }

    // =========================================================
    // OPEN PORTFOLIO ANALYSIS
    // =========================================================

    public void openPortfolioAnalysisFromHubForPortfolioAnalysisFa() {
        diagnostic("Opening Portfolio Analysis from Hub");

        if (isVisibleWithin(portfolioAnalysisHeader, 2)) {
            diagnostic("Portfolio Analysis is already open");
            return;
        }

        if (!isVisibleWithin(portfolioAnalysisTile, 3)) {
            scrollUntilVisible(
                    portfolioAnalysisTile,
                    "Portfolio Analysis option in Hub",
                    6
            );
        }

        WebElement tile = WaitUtils.waitForElement(
                driver,
                portfolioAnalysisTile,
                10
        );

        diagnostic(
                "Portfolio Analysis tile located"
                        + " | bounds=" + rectangleToString(tile.getRect())
                        + " | clickable=" + safeAttribute(tile, "clickable")
        );

        /*
         * First use the framework click. If Flutter accepts the touch but the route
         * does not render, do not sit in a single 45-second blind wait. Re-acquire
         * the current semantics node and use one W3C centre tap as a controlled
         * retry. This is the exact transition that failed before PAFA_003 and left
         * the next test on an unclassified intermediate page.
         */
        try {
            ActionUtils.click(driver, portfolioAnalysisTile);
        } catch (Exception firstClickFailure) {
            diagnostic(
                    "Primary Portfolio Analysis tile click threw an exception; "
                            + "using W3C fallback | error=" + safeMessage(firstClickFailure)
            );
            tile = findVisibleElement(portfolioAnalysisTile);
            if (tile != null) {
                tapElementCenter(tile);
            }
        }

        if (!isVisibleWithin(portfolioAnalysisHeader, 15)) {
            WebElement retryTile = findVisibleElement(portfolioAnalysisTile);

            if (retryTile != null) {
                diagnostic(
                        "Portfolio Analysis route did not open after primary click; "
                                + "retrying once with fresh W3C centre tap"
                                + " | bounds=" + rectangleToString(retryTile.getRect())
                );
                tapElementCenter(retryTile);
            } else {
                diagnostic(
                        "Portfolio Analysis tile disappeared after primary click; "
                                + "navigation may already be in progress, waiting for delayed Flutter route"
                );
            }
        }

        WaitUtils.waitForElement(driver, portfolioAnalysisHeader, 35);
        WaitUtils.waitForElement(driver, summaryTab, 35);
        WaitUtils.waitForElement(driver, fundsSifsTab, 35);

        settleAfterPageOpen("Portfolio Analysis");

        diagnostic("Portfolio Analysis page opened successfully");
    }

    // =========================================================
    // SELECT MY INVESTMENT - 5
    // =========================================================

    public void selectInvestment5ForPortfolioAnalysisFa() {
        diagnostic(
                "Ensuring My Investment - 5 is selected before opening Funds / SIFs"
        );

        WaitUtils.waitForElement(driver, portfolioAnalysisHeader, 30);

        WebElement selector =
                WaitUtils.waitForElement(driver, investorSelectionDropdown, 30);

        String selectorDescription = getReadableText(selector);

        diagnostic(
                "Current investor selector found"
                        + " | value=" + selectorDescription
                        + " | bounds=" + rectangleToString(selector.getRect())
                        + " | clickable=" + safeAttribute(selector, "clickable")
        );

        /*
         * Combined-regression policy: always re-select the required investor.
         * This deliberately avoids inheriting stale investor state from a previous
         * PAFA test or a manual session. The same proven drawer flow is therefore
         * executed for every bucket, even when My Investment - 5 already appears
         * selected in the collapsed control.
         */
        if (isInvestment5CurrentlySelected(selector)) {
            diagnostic(
                    "My Investment - 5 is already selected; reopening Choose Investor and re-selecting it for deterministic combined regression state"
            );
        } else {
            diagnostic(
                    "A different investor is selected. Opening Choose Investor and switching to My Investment - 5"
            );
        }

        openInvestorDrawerRobustly();
        settleAfterDrawerOpen("Choose Investor");

        diagnostic("Choose Investor drawer opened successfully");

        if (!isVisibleWithin(investment5, 2)) {
            diagnostic(
                    "My Investment - 5 is not currently visible. Ensuring Old Labels is expanded"
            );

            WebElement oldLabelsElement =
                    WaitUtils.waitForElement(driver, oldLabels, 10);

            try {
                oldLabelsElement.click();
                sleep(500);
            } catch (Exception e) {
                diagnostic(
                        "Direct Old Labels click failed; using W3C centre tap"
                );
                tapElementCenter(oldLabelsElement);
                sleep(500);
            }

            if (!isVisibleWithin(investment5, 8)) {
                diagnostic(
                        "My Investment - 5 still not visible after first Old Labels interaction; retrying with W3C tap"
                );
                oldLabelsElement = WaitUtils.waitForElement(driver, oldLabels, 5);
                tapElementCenter(oldLabelsElement);
                sleep(650);
            }

            WaitUtils.waitForElement(driver, investment5, 15);
            diagnostic("Old Labels expanded and My Investment - 5 is visible");
        } else {
            diagnostic("My Investment - 5 is already visible inside Choose Investor");
        }

        WebElement investment5Element =
                WaitUtils.waitForElement(driver, investment5, 15);

        diagnostic(
                "Selecting My Investment - 5"
                        + " | bounds=" + rectangleToString(investment5Element.getRect())
                        + " | clickable=" + safeAttribute(investment5Element, "clickable")
        );

        tapElementCenter(investment5Element);
        sleep(700);

        if (isInvestorDrawerOpen()) {
            diagnostic(
                    "Choose Investor remained open after selection; closing drawer while preserving the selected investor"
            );
            closeInvestorDrawerByDraggingDown();
        }

        WaitUtils.waitForElement(driver, portfolioAnalysisHeader, 30);

        long selectionDeadline =
                System.nanoTime() + Duration.ofSeconds(20).toNanos();

        boolean selected = false;
        while (System.nanoTime() < selectionDeadline) {
            WebElement currentSelector = findVisibleElement(investorSelectionDropdown);
            if (currentSelector != null && isInvestment5CurrentlySelected(currentSelector)) {
                selected = true;
                break;
            }
            sleep(250);
        }

        if (!selected) {
            throw new AssertionError(
                    "Investor selection failed. Expected My Investment - 5 after Choose Investor selection"
            );
        }

        // Investor change can trigger a complete data refresh on Portfolio Analysis.
        settleAfterHeavyPageOpen("Portfolio Analysis after investor selection");

        diagnostic("My Investment - 5 confirmed as the selected investor");
    }

    private boolean isInvestment5CurrentlySelected(WebElement selector) {
        if (selector == null) {
            return false;
        }

        String readable = getReadableText(selector);

        return isDisplayedNow(investment5SelectedDropdown)
                || (readable != null && readable.contains("My Investment - 5"));
    }

    // =========================================================
    // OPEN INVESTOR DRAWER ROBUSTLY
    // =========================================================

    private void openInvestorDrawerRobustly() {
        if (isInvestorDrawerOpen()) {
            return;
        }

        WebElement selector = findVisibleElement(investorSelectionDropdown);

        if (selector == null) {
            throw new AssertionError(
                    "Investor selection control with description 'Shows investor selection list' "
                            + "was not visible on Portfolio Analysis"
            );
        }

        Rectangle rect = selector.getRect();
        String readable = getReadableText(selector);
        String clickable = safeAttribute(selector, "clickable");

        diagnostic(
                "Investor dropdown semantics located"
                        + " | readable=" + readable
                        + " | bounds=" + rectangleToString(rect)
                        + " | clickable=" + clickable
        );

        if (!"false".equalsIgnoreCase(clickable)) {
            try {
                selector.click();
                sleep(600);

                if (isInvestorDrawerOpen()) {
                    diagnostic("Investor drawer opened using direct element click");
                    return;
                }
            } catch (Exception e) {
                diagnostic(
                        "Direct investor-selector click failed: " + safeMessage(e)
                );
            }
        } else {
            diagnostic(
                    "Investor selector reports clickable=false. Skipping reliance on element.click() "
                            + "and using its W3C tappable hotspot."
            );
        }

        int hotspotX = rect.getX() + (int) (rect.getWidth() * 0.78);
        int hotspotY = rect.getY() + (int) (rect.getHeight() * 0.35);

        diagnostic(
                "Tapping investor selector Flutter hotspot"
                        + " | x=" + hotspotX
                        + " | y=" + hotspotY
        );

        tapByCoordinates(hotspotX, hotspotY);
        sleep(700);

        if (isInvestorDrawerOpen()) {
            diagnostic(
                    "Investor drawer opened using selector-relative Flutter hotspot"
            );
            return;
        }

        // Re-acquire the selector because Flutter can replace the semantics node.
        selector = findVisibleElement(investorSelectionDropdown);
        if (selector == null) {
            if (isInvestorDrawerOpen()) {
                return;
            }
            throw new AssertionError(
                    "Investor selector disappeared before right-side fallback tap"
            );
        }

        rect = selector.getRect();

        int arrowX = rect.getX() + (int) (rect.getWidth() * 0.91);
        int arrowY = rect.getY() + (int) (rect.getHeight() * 0.50);

        diagnostic(
                "Primary selector hotspot did not open drawer. "
                        + "Tapping right-side arrow region"
                        + " | x=" + arrowX
                        + " | y=" + arrowY
        );

        tapByCoordinates(arrowX, arrowY);
        sleep(700);

        if (isInvestorDrawerOpen()) {
            diagnostic(
                    "Investor drawer opened using right-side selector hotspot"
            );
            return;
        }

        selector = findVisibleElement(investorSelectionDropdown);
        if (selector != null) {
            diagnostic(
                    "Right-side hotspot did not open drawer. "
                            + "Trying selector centre as final bounds-based fallback."
            );

            tapElementCenter(selector);
            sleep(700);

            if (isInvestorDrawerOpen()) {
                diagnostic(
                        "Investor drawer opened using selector centre W3C tap"
                );
                return;
            }
        }

        throw new AssertionError(
                "Unable to open Choose Investor drawer even though investor selector was located"
                        + " | selector=" + readable
                        + " | bounds=" + rectangleToString(rect)
                        + " | clickable=" + clickable
        );
    }

    // =========================================================
    // OPEN FUNDS / SIFS
    // =========================================================

    public void openFundsSifsForPortfolioAnalysisFa() {
        diagnostic(
                "Opening Funds / SIFs and waiting for fund cards to load without scrolling"
        );

        ActionUtils.click(driver, fundsSifsTab);

        WaitUtils.waitForElement(
                driver,
                fundPortfolioPerformance,
                60
        );

        settleAfterPageOpen("Funds / SIFs");

        /*
         * Required behavior:
         * - DO NOT scroll on Funds / SIFs.
         * - Wait until one of the already-known fund rows is actually visible.
         * - Enter Fund Review from that visible row.
         */
        WebElement visibleEntryFund =
                waitForVisibleFundsSifsEntryFundWithoutScrolling(120);

        diagnostic(
                "Funds / SIFs cards loaded without scrolling"
                        + " | visible entry fund="
                        + getReadableText(visibleEntryFund)
        );
    }

    // =========================================================
    // OPEN ANY VISIBLE FUND -> FUND REVIEW -> OPTIMIZE
    // =========================================================

    public void openOptimizeFundReviewForPortfolioAnalysisFa() {
        diagnostic(
                "Opening Fund Review from a currently visible Funds / SIFs fund without scrolling that page"
        );

        WebElement entryFund =
                waitForVisibleFundsSifsEntryFundWithoutScrolling(30);

        String entryFundName =
                getReadableText(entryFund);

        if (entryFundName == null || entryFundName.trim().isEmpty()) {
            throw new AssertionError(
                    "A visible Funds / SIFs fund was found but its accessibility description was empty"
            );
        }

        By entryFundLocator =
                AppiumBy.accessibilityId(entryFundName);

        diagnostic(
                "Opening Fund Review from visible fund"
                        + " | fund=" + entryFundName
                        + " | bounds=" + rectangleToString(entryFund.getRect())
                        + " | no Funds / SIFs scrolling performed"
        );

        openFundReviewFromFundRowRobustly(
                entryFundLocator,
                entryFundName
        );

        WaitUtils.waitForElement(
                driver,
                fundReviewHeader,
                45
        );

        settleAfterHeavyPageOpen("Fund Review");

        diagnostic(
                "Fund Review opened successfully from " + entryFundName
        );

        /*
         * Only after Fund Review opens do we switch to Optimize.
         * Stop navigation here. The regression continues from this exact screen
         * and validates all target cards without returning to Hub/Funds in between.
         */
        ensureOptimizeBucketSelected();

        diagnostic(
                "Fund Review is on Optimize; continuing directly with trigger and drawer validation"
        );
    }

    // =========================================================
    // OPEN ANY VISIBLE FUND -> FUND REVIEW -> GOOD
    // =========================================================

    /**
     * Uses the exact same proven Funds / SIFs -> Fund Review entry journey as
     * the Optimize regression, but selects Good instead of Optimize.
     *
     * IMPORTANT: Funds / SIFs itself is not scrolled here.
     */
    public void openGoodFundReviewForPortfolioAnalysisFa() {
        diagnostic(
                "Opening Fund Review from a currently visible Funds / SIFs fund without scrolling that page"
        );

        WebElement entryFund =
                waitForVisibleFundsSifsEntryFundWithoutScrolling(30);

        String entryFundName =
                getReadableText(entryFund);

        if (entryFundName == null || entryFundName.trim().isEmpty()) {
            throw new AssertionError(
                    "A visible Funds / SIFs fund was found but its accessibility description was empty"
            );
        }

        By entryFundLocator =
                AppiumBy.accessibilityId(entryFundName);

        diagnostic(
                "Opening Fund Review from visible fund"
                        + " | fund=" + entryFundName
                        + " | bounds=" + rectangleToString(entryFund.getRect())
                        + " | no Funds / SIFs scrolling performed"
        );

        openFundReviewFromFundRowRobustly(
                entryFundLocator,
                entryFundName
        );

        WaitUtils.waitForElement(
                driver,
                fundReviewHeader,
                45
        );

        /*
         * Full data-heavy page wait retained exactly as requested: do not start
         * interacting with the newly opened Fund Review until it has settled.
         */
        settleAfterHeavyPageOpen("Fund Review");

        diagnostic(
                "Fund Review opened successfully from " + entryFundName
        );

        ensureGoodBucketSelected();

        diagnostic(
                "Fund Review is on Good; advice regression will continue from this exact screen"
        );
    }

    // =========================================================
    // OPEN ANY VISIBLE FUND -> FUND REVIEW -> NEW FUND
    // =========================================================

    /**
     * Uses the exact same proven Funds / SIFs -> Fund Review entry journey as
     * Optimize and Good. Funds / SIFs itself is not scrolled. Only after Fund
     * Review is fully settled do we select the New Fund bucket.
     */
    public void openNewFundReviewForPortfolioAnalysisFa() {
        diagnostic(
                "Opening Fund Review from a currently visible Funds / SIFs fund without scrolling that page"
        );

        WebElement entryFund =
                waitForVisibleFundsSifsEntryFundWithoutScrolling(30);

        String entryFundName =
                getReadableText(entryFund);

        if (entryFundName == null || entryFundName.trim().isEmpty()) {
            throw new AssertionError(
                    "A visible Funds / SIFs fund was found but its accessibility description was empty"
            );
        }

        By entryFundLocator =
                AppiumBy.accessibilityId(entryFundName);

        diagnostic(
                "Opening Fund Review from visible fund"
                        + " | fund=" + entryFundName
                        + " | bounds=" + rectangleToString(entryFund.getRect())
                        + " | no Funds / SIFs scrolling performed"
        );

        openFundReviewFromFundRowRobustly(
                entryFundLocator,
                entryFundName
        );

        WaitUtils.waitForElement(
                driver,
                fundReviewHeader,
                45
        );

        settleAfterHeavyPageOpen("Fund Review");

        diagnostic(
                "Fund Review opened successfully from " + entryFundName
        );

        ensureNewFundBucketSelected();

        diagnostic(
                "Fund Review is on New Fund; advice regression will continue from this exact screen"
        );
    }

    // =========================================================
    // OPEN ANY VISIBLE FUND -> FUND REVIEW -> STEADY
    // =========================================================

    /**
     * Uses the same proven no-scroll Funds / SIFs -> Fund Review entry journey
     * as Optimize, Good and New Fund. Only after Fund Review is fully loaded and
     * left untouched for the heavy-page settle do we select Steady.
     */
    public void openSteadyFundReviewForPortfolioAnalysisFa() {
        diagnostic(
                "Opening Fund Review from a currently visible Funds / SIFs fund without scrolling that page"
        );

        WebElement entryFund =
                waitForVisibleFundsSifsEntryFundWithoutScrolling(30);

        String entryFundName =
                getReadableText(entryFund);

        if (entryFundName == null || entryFundName.trim().isEmpty()) {
            throw new AssertionError(
                    "A visible Funds / SIFs fund was found but its accessibility description was empty"
            );
        }

        By entryFundLocator =
                AppiumBy.accessibilityId(entryFundName);

        diagnostic(
                "Opening Fund Review from visible fund"
                        + " | fund=" + entryFundName
                        + " | bounds=" + rectangleToString(entryFund.getRect())
                        + " | no Funds / SIFs scrolling performed"
        );

        openFundReviewFromFundRowRobustly(
                entryFundLocator,
                entryFundName
        );

        WaitUtils.waitForElement(
                driver,
                fundReviewHeader,
                45
        );

        settleAfterHeavyPageOpen("Fund Review");

        diagnostic(
                "Fund Review opened successfully from " + entryFundName
        );

        ensureSteadyBucketSelected();

        diagnostic(
                "Fund Review is on Steady; advice regression will continue from this exact screen"
        );
    }

    /**
     * Waits for an already-known fund locator to become visible on the current
     * Funds / SIFs viewport. No swipe/scroll is performed in this method.
     */
    private WebElement waitForVisibleFundsSifsEntryFundWithoutScrolling(
            int timeoutSeconds
    ) {
        diagnostic(
                "Waiting for a visible Funds / SIFs fund card without scrolling"
                        + " | timeout=" + timeoutSeconds + " sec"
        );

        long deadline =
                System.nanoTime()
                        + Duration.ofSeconds(timeoutSeconds).toNanos();

        while (System.nanoTime() < deadline) {

            for (String fundName : FUNDS_SIFS_ENTRY_FUND_NAMES) {

                By locator =
                        AppiumBy.accessibilityId(fundName);

                WebElement element =
                        findVisibleElement(locator);

                if (element != null
                        && isElementInsideCurrentViewport(element)) {

                    diagnostic(
                            "Visible Funds / SIFs entry fund found"
                                    + " | fund=" + fundName
                                    + " | bounds="
                                    + rectangleToString(element.getRect())
                    );

                    return element;
                }
            }

            sleep(300);
        }

        throw new AssertionError(
                "Funds / SIFs page opened, but none of the known fund locators became visible "
                        + "within " + timeoutSeconds + " seconds. "
                        + "No scrolling was performed as required. "
                        + "Known locators checked: HDFC Aggressive Hybrid Reg-G, "
                        + "Tata Children's Reg, ABSL Corporate Bond Reg-G, "
                        + "Axis Liquid Reg-G, ICICI Pru Balanced Advantage-G"
        );
    }

    private boolean isElementInsideCurrentViewport(
            WebElement element
    ) {
        if (element == null) {
            return false;
        }

        try {
            Rectangle rect =
                    element.getRect();

            Dimension screen =
                    driver.manage()
                            .window()
                            .getSize();

            int elementTop =
                    rect.getY();

            int elementBottom =
                    rect.getY()
                            + rect.getHeight();

            return rect.getWidth() > 0
                    && rect.getHeight() > 0
                    && elementBottom > 0
                    && elementTop < screen.getHeight();

        } catch (Exception e) {
            return false;
        }
    }

    // =========================================================
    // SELECT / CONFIRM OPTIMIZE BUCKET
    // =========================================================

    private void ensureOptimizeBucketSelected() {

        if (isVisibleWithin(
                optimizeReviewHeader,
                2
        )) {

            settleAfterPageOpen(
                    "Fund Review - Optimize bucket"
            );

            optimizeTraversalCheckpointIndex = -1;

            diagnostic(
                    "Fund Review is already on Optimize - Funds that can do better"
            );

            return;
        }

        diagnostic(
                "Selecting Optimize/Optimise bucket inside Fund Review"
        );

        WebElement optimizeTab =
                findVisibleElement(optimiseBucketTab);

        if (optimizeTab == null) {
            optimizeTab =
                    findVisibleElement(optimizeBucketTabFallback);
        }

        if (optimizeTab == null) {
            throw new AssertionError(
                    "Optimize/Optimise bucket tab was not visible on Fund Review"
            );
        }

        String clickable =
                safeAttribute(
                        optimizeTab,
                        "clickable"
                );

        diagnostic(
                "Optimize bucket tab located"
                        + " | value=" + getReadableText(optimizeTab)
                        + " | bounds="
                        + rectangleToString(optimizeTab.getRect())
                        + " | clickable="
                        + clickable
        );

        boolean opened = false;

        if (!"false".equalsIgnoreCase(clickable)) {

            try {
                optimizeTab.click();

                opened =
                        isVisibleWithin(
                                optimizeReviewHeader,
                                5
                        );

            } catch (Exception e) {
                diagnostic(
                        "Direct Optimize-tab click failed: "
                                + safeMessage(e)
                );
            }
        }

        if (!opened) {

            optimizeTab =
                    findVisibleElement(optimiseBucketTab);

            if (optimizeTab == null) {
                optimizeTab =
                        findVisibleElement(optimizeBucketTabFallback);
            }

            if (optimizeTab == null) {
                throw new AssertionError(
                        "Optimize/Optimise tab disappeared before W3C fallback tap"
                );
            }

            diagnostic(
                    "Using W3C centre tap on Optimize/Optimise bucket"
            );

            tapElementCenter(optimizeTab);
        }

        WaitUtils.waitForElement(
                driver,
                optimizeReviewHeader,
                30
        );

        settleAfterHeavyPageOpen(
                "Fund Review - Optimize bucket"
        );

        optimizeTraversalCheckpointIndex = -1;

        diagnostic(
                "Optimize bucket opened successfully"
        );
    }

    // =========================================================
    // SELECT / CONFIRM GOOD BUCKET
    // =========================================================

    private void ensureGoodBucketSelected() {
        if (isVisibleWithin(goodReviewHeader, 2)) {
            diagnostic(
                    "Fund Review is already on Good - Funds that are good"
            );

            settleAfterHeavyPageOpen("Fund Review - Good bucket");
            assertGoodBucketStartsAtExpectedFirstFund();
            goodTraversalCheckpointIndex = -1;
            return;
        }

        diagnostic(
                "Selecting Good bucket inside Fund Review"
        );

        WebElement goodTab =
                findVisibleElement(goodBucketTab);

        if (goodTab == null) {
            throw new AssertionError(
                    "Good bucket tab was not visible on Fund Review"
            );
        }

        String clickable =
                safeAttribute(goodTab, "clickable");

        diagnostic(
                "Good bucket tab located"
                        + " | value=" + getReadableText(goodTab)
                        + " | bounds=" + rectangleToString(goodTab.getRect())
                        + " | clickable=" + clickable
        );

        boolean opened = false;

        if (!"false".equalsIgnoreCase(clickable)) {
            try {
                goodTab.click();
                opened = isVisibleWithin(goodReviewHeader, 5);
            } catch (Exception e) {
                diagnostic(
                        "Direct Good-tab click failed: " + safeMessage(e)
                );
            }
        }

        if (!opened) {
            goodTab = findVisibleElement(goodBucketTab);

            if (goodTab == null) {
                throw new AssertionError(
                        "Good bucket tab disappeared before W3C fallback tap"
                );
            }

            diagnostic(
                    "Using W3C centre tap on Good bucket"
            );

            tapElementCenter(goodTab);
        }

        WaitUtils.waitForElement(
                driver,
                goodReviewHeader,
                30
        );

        /*
         * Good is a full data-heavy list. Keep it untouched for 5 seconds after
         * its real page marker appears, exactly like the stable Optimize journey.
         */
        settleAfterHeavyPageOpen("Fund Review - Good bucket");

        assertGoodBucketStartsAtExpectedFirstFund();
        goodTraversalCheckpointIndex = -1;

        diagnostic(
                "Good bucket opened successfully"
        );
    }

    private void assertGoodBucketStartsAtExpectedFirstFund() {
        WebElement firstMapped =
                waitForGoodFundInCardBand(
                        AppiumBy.accessibilityId(GOOD_MAPPED_FUNDS_IN_ORDER[0]),
                        1200
                );

        if (firstMapped != null) {
            diagnostic(
                    "Good bucket loaded with first mapped QA target already visible"
                            + " | fund=" + GOOD_MAPPED_FUNDS_IN_ORDER[0]
                            + " | bounds=" + rectangleToString(firstMapped.getRect())
            );
            return;
        }

        diagnostic(
                "Good bucket loaded; first mapped target is not yet in the usable card band. "
                        + "Target-driven scrolling will locate it without assuming the physical first card."
        );
    }

    // =========================================================
    // SELECT / CONFIRM NEW FUND BUCKET
    // =========================================================

    private void ensureNewFundBucketSelected() {
        if (isVisibleWithin(newFundReviewHeader, 2)) {
            diagnostic(
                    "Fund Review is already on New Fund - New funds"
            );

            settleAfterHeavyPageOpen("Fund Review - New Fund bucket");
            assertNewFundBucketStartsAtExpectedFirstFund();
            newFundTraversalCheckpointIndex = -1;
            return;
        }

        diagnostic(
                "Selecting New Fund bucket inside Fund Review"
        );

        WebElement newFundTab =
                findVisibleElement(newFundBucketTab);

        if (newFundTab == null || !isElementTappableInsideCurrentScreen(newFundTab)) {
            diagnostic(
                    "New Fund tab is not currently visible; revealing the right side of the Fund Review tab strip with one controlled W3C horizontal swipe"
            );

            revealNewFundTabWithHorizontalSwipe();
            sleep(700);
            newFundTab = findVisibleElement(newFundBucketTab);
        }

        if (newFundTab == null || !isElementTappableInsideCurrentScreen(newFundTab)) {
            throw new AssertionError(
                    "New Fund bucket tab could not be exposed after the controlled horizontal tab-strip swipe"
            );
        }

        String clickable = safeAttribute(newFundTab, "clickable");

        diagnostic(
                "New Fund bucket tab located"
                        + " | value=" + getReadableText(newFundTab)
                        + " | bounds=" + rectangleToString(newFundTab.getRect())
                        + " | clickable=" + clickable
        );

        boolean opened = false;

        if (!"false".equalsIgnoreCase(clickable)) {
            try {
                newFundTab.click();
                opened = isVisibleWithin(newFundReviewHeader, 5);
            } catch (Exception e) {
                diagnostic(
                        "Direct New Fund-tab click failed: " + safeMessage(e)
                );
            }
        }

        if (!opened) {
            newFundTab = findVisibleElement(newFundBucketTab);

            if (newFundTab == null) {
                throw new AssertionError(
                        "New Fund tab disappeared before W3C fallback tap"
                );
            }

            diagnostic(
                    "Using W3C centre tap on New Fund bucket"
            );

            tapElementCenter(newFundTab);
        }

        WaitUtils.waitForElement(
                driver,
                newFundReviewHeader,
                30
        );

        /*
         * New Fund is a data list too. Keep the same 5-second post-marker settle
         * policy used by the stable Good/Optimize flows before reading cards.
         */
        settleAfterHeavyPageOpen("Fund Review - New Fund bucket");

        assertNewFundBucketStartsAtExpectedFirstFund();
        newFundTraversalCheckpointIndex = -1;

        diagnostic(
                "New Fund bucket opened successfully"
        );
    }

    private boolean isElementTappableInsideCurrentScreen(
            WebElement element
    ) {
        if (element == null) {
            return false;
        }

        try {
            Rectangle rect = element.getRect();
            Dimension screen = driver.manage().window().getSize();

            int centerX = rect.getX() + rect.getWidth() / 2;
            int centerY = rect.getY() + rect.getHeight() / 2;

            return rect.getWidth() > 0
                    && rect.getHeight() > 0
                    && centerX > 0
                    && centerX < screen.getWidth()
                    && centerY > 0
                    && centerY < screen.getHeight();
        } catch (Exception e) {
            return false;
        }
    }

    private void revealNewFundTabWithHorizontalSwipe() {
        Dimension size = driver.manage().window().getSize();

        int y = (int) (size.getHeight() * 0.215);
        WebElement goodTab = findVisibleElement(goodBucketTab);

        if (goodTab != null) {
            try {
                Rectangle rect = goodTab.getRect();
                y = rect.getY() + rect.getHeight() / 2;
            } catch (Exception ignored) {
                // Use percentage fallback if Flutter refreshes the Good tab node.
            }
        }

        swipeByCoordinates(
                (int) (size.getWidth() * 0.88),
                y,
                (int) (size.getWidth() * 0.24),
                y,
                700
        );
    }

    private void assertNewFundBucketStartsAtExpectedFirstFund() {
        WebElement firstMapped =
                waitForNewFundInCardBand(
                        AppiumBy.accessibilityId(NEW_FUND_MAPPED_FUNDS_IN_ORDER[0]),
                        1200
                );

        if (firstMapped != null) {
            diagnostic(
                    "New Fund bucket loaded with first mapped Abakkus target already visible"
                            + " | fund=" + NEW_FUND_MAPPED_FUNDS_IN_ORDER[0]
                            + " | bounds=" + rectangleToString(firstMapped.getRect())
            );
            return;
        }

        diagnostic(
                "New Fund bucket loaded; mapped Abakkus target is below the current viewport. "
                        + "Target-driven scrolling will cross any preceding/unknown New Fund cards silently."
        );
    }

    // =========================================================
    // SELECT / CONFIRM STEADY BUCKET
    // =========================================================

    private void ensureSteadyBucketSelected() {
        if (isVisibleWithin(steadyReviewHeader, 2)) {
            diagnostic(
                    "Fund Review is already on Steady - Funds that are steady"
            );

            settleAfterHeavyPageOpen("Fund Review - Steady bucket");
            assertSteadyBucketStartsAtExpectedFirstFund();
            steadyTraversalCheckpointIndex = -1;
            steadyControlledSwipeCount = 0;
            steadyReverseRecoverySwipeCount = 0;
            return;
        }

        diagnostic(
                "Selecting Steady bucket inside Fund Review"
        );

        WebElement steadyTab =
                findVisibleElement(steadyBucketTab);

        if (steadyTab == null) {
            throw new AssertionError(
                    "Steady bucket tab was not visible on Fund Review"
            );
        }

        String clickable =
                safeAttribute(steadyTab, "clickable");

        diagnostic(
                "Steady bucket tab located"
                        + " | value=" + getReadableText(steadyTab)
                        + " | bounds=" + rectangleToString(steadyTab.getRect())
                        + " | clickable=" + clickable
        );

        boolean opened = false;

        if (!"false".equalsIgnoreCase(clickable)) {
            try {
                steadyTab.click();
                opened = isVisibleWithin(steadyReviewHeader, 5);
            } catch (Exception e) {
                diagnostic(
                        "Direct Steady-tab click failed: " + safeMessage(e)
                );
            }
        }

        if (!opened) {
            steadyTab = findVisibleElement(steadyBucketTab);

            if (steadyTab == null) {
                throw new AssertionError(
                        "Steady bucket tab disappeared before W3C fallback tap"
                );
            }

            diagnostic(
                    "Using W3C centre tap on Steady bucket"
            );

            tapElementCenter(steadyTab);
        }

        WaitUtils.waitForElement(
                driver,
                steadyReviewHeader,
                30
        );

        /*
         * Steady is a data-heavy Fund Review page. Keep the same proven 5-second
         * post-marker settle used by the other buckets before reading any card.
         */
        settleAfterHeavyPageOpen("Fund Review - Steady bucket");

        assertSteadyBucketStartsAtExpectedFirstFund();
        steadyTraversalCheckpointIndex = -1;
        steadyControlledSwipeCount = 0;
        steadyReverseRecoverySwipeCount = 0;

        diagnostic(
                "Steady bucket opened successfully"
        );
    }

    private void assertSteadyBucketStartsAtExpectedFirstFund() {
        WebElement firstMapped =
                waitForSteadyFundInCaptureBand(
                        AppiumBy.accessibilityId(STEADY_MAPPED_FUNDS_IN_ORDER[0]),
                        1200
                );

        if (firstMapped != null) {
            diagnostic(
                    "Steady bucket loaded with first mapped QA target already visible"
                            + " | fund=" + STEADY_MAPPED_FUNDS_IN_ORDER[0]
                            + " | bounds=" + rectangleToString(firstMapped.getRect())
            );
            return;
        }

        diagnostic(
                "Steady bucket loaded; first mapped target is not yet visible. "
                        + "Initialization will use bounded target-driven scrolling and ignore unknown cards."
        );
    }

    // =========================================================
    // POSITION TARGET OPTIMIZE FUND + CALCULATION CARD
    // =========================================================

    /**
     * Baseline wrapper retained for any existing callers.
     */
    private OptimizeCardElements positionApplicableOptimizeCalculationCard() {
        return positionOptimizeFundCalculationCard(
                PRIMARY_OPTIMIZE_FUND_NAME,
                70
        );
    }

    /**
     * Finds a specific Optimize regression target and positions its complete
     * calculation card. Search is target-driven: only the four mapped regression
     * funds matter, while unrelated/new cards between them are ignored.
     */
    private OptimizeCardElements positionOptimizeFundCalculationCard(
            String fundName,
            int maxScrollAttemptsPerPass
    ) {
        diagnostic(
                "Locating Optimize mapped target"
                        + " | fund=" + fundName
                        + " | traversal=target-driven"
        );

        verifyOptimizeTabStripIsVisible(
                "before locating " + fundName
        );

        By fundLocator = AppiumBy.accessibilityId(fundName);

        findOptimizeMappedTargetDriven(
                fundLocator,
                fundName,
                maxScrollAttemptsPerPass
        );

        positionFundAnchorForFullCalculationCard(
                fundLocator,
                fundName,
                maxScrollAttemptsPerPass
        );

        for (int attempt = 1; attempt <= 10; attempt++) {
            OptimizeCardElements card =
                    locateVisibleCalculationElementsForFund(fundName);

            if (card != null) {
                validateCardElementGeometry(card, fundName);
                diagnostic(
                        "Optimize calculation card positioned"
                                + " | fund=" + fundName
                                + " | fundY=" + card.fund.getRect().getY()
                                + " | triggerY=" + card.trigger.getRect().getY()
                );
                return card;
            }

            WebElement fund = waitForFundInCurrentViewport(fundLocator, 750);
            if (fund == null) {
                diagnostic(
                        "Optimize target heading moved outside safe band while exposing calculation controls; "
                                + "using local reverse recovery only"
                                + " | fund=" + fundName
                );

                fund = recoverOptimizeMappedTargetLocally(fundLocator, fundName);
                if (fund == null) {
                    throw new AssertionError(
                            "Optimize mapped target could not be re-acquired locally while positioning its calculation card"
                                    + " | fund=" + fundName
                    );
                }
                continue;
            }

            gentleSearchSwipeUp();
            settleAfterSearchScroll();
        }

        throw new AssertionError(
                "Fund was found but its complete calculation section could not be positioned"
                        + " | fund=" + fundName
        );
    }

    /**
     * Target-driven Optimize search. Only the four regression targets matter;
     * all other cards are crossed without needing locators. The search proceeds
     * from the current scroll position and never performs the old full-list top
     * reset when a target is temporarily lost.
     */
    private WebElement findOptimizeMappedTargetDriven(
            By fundLocator,
            String fundName,
            int maxForwardSwipes
    ) {
        int targetIndex = indexOfOptimizeRegressionFund(fundName);
        if (targetIndex < 0) {
            throw new AssertionError(
                    "Optimize target is not present in the regression target map"
                            + " | fund=" + fundName
            );
        }

        if (optimizeTraversalCheckpointIndex > targetIndex) {
            throw new AssertionError(
                    "Optimize target-driven traversal cannot move backwards"
                            + " | currentTarget="
                            + OPTIMIZE_DRAWER_REGRESSION_FUNDS[optimizeTraversalCheckpointIndex]
                            + " | requestedTarget=" + fundName
            );
        }

        WebElement target = waitForFundInCurrentViewport(fundLocator, 700);

        /*
         * Re-entrant lookup for the SAME mapped target can happen after its lower
         * calculation controls/drawer have been exposed and Flutter has moved the
         * heading just above the safe band. Recover locally first; never reset the
         * whole Optimize list to the top for this case.
         */
        if (target == null && optimizeTraversalCheckpointIndex == targetIndex) {
            target = recoverOptimizeMappedTargetLocally(fundLocator, fundName);
        }

        /*
         * First mapped target: if Fund Review restored a lower offset, try a bounded
         * reverse correction first. If a new/unknown card was inserted above ABSL,
         * the following forward search will simply cross it.
         */
        if (target == null && optimizeTraversalCheckpointIndex < 0 && targetIndex == 0) {
            for (int reverse = 1;
                 reverse <= OPTIMIZE_MAX_INITIAL_REVERSE_SWIPES && target == null;
                 reverse++) {
                if (findLaterOptimizeRegressionTargetInSafeBand(targetIndex) < 0 && reverse > 4) {
                    break;
                }
                cardByCardSearchSwipeDown();
                sleep(OPTIMIZE_TARGET_SEARCH_SETTLE_MS);
                target = waitForFundInCurrentViewport(fundLocator, 650);
            }
        }

        int forwardSwipesUsed = 0;
        int reverseSwipesUsed = 0;
        int boundedMax = Math.max(12, maxForwardSwipes);

        while (target == null && forwardSwipesUsed < boundedMax) {
            int laterTargetIndex = findLaterOptimizeRegressionTargetInSafeBand(targetIndex);
            if (laterTargetIndex > targetIndex) {
                OptimizeReverseRecovery recovery = recoverExpectedOptimizeMappedTarget(
                        fundLocator,
                        fundName
                );
                reverseSwipesUsed += recovery.swipesUsed;
                target = recovery.fund;
                if (target != null) {
                    break;
                }
            }

            forwardSwipesUsed++;
            cardByCardSearchSwipeUp();
            sleep(OPTIMIZE_TARGET_SEARCH_SETTLE_MS);
            target = waitForFundInCurrentViewport(fundLocator, 850);
        }

        if (target == null) {
            int laterTargetIndex = findLaterOptimizeRegressionTargetInSafeBand(targetIndex);
            if (laterTargetIndex > targetIndex) {
                OptimizeReverseRecovery recovery = recoverExpectedOptimizeMappedTarget(
                        fundLocator,
                        fundName
                );
                reverseSwipesUsed += recovery.swipesUsed;
                target = recovery.fund;
            }
        }

        if (target == null) {
            throw new AssertionError(
                    "Unable to locate Optimize mapped target after bounded target-driven scrolling"
                            + " | target=" + fundName
                            + " | forwardSwipesUsed=" + forwardSwipesUsed
                            + " | reverseRecoverySwipesUsed=" + reverseSwipesUsed
                            + " | maxForwardSwipes=" + boundedMax
                            + " | intermediateUnknownFunds=ignored"
                            + " | fullListTopReset=false"
                            + " | fling=false"
            );
        }

        optimizeTraversalCheckpointIndex = targetIndex;
        diagnostic(
                "Optimize mapped target captured"
                        + " | " + (targetIndex + 1)
                        + "/" + OPTIMIZE_DRAWER_REGRESSION_FUNDS.length
                        + " | fund=" + fundName
                        + " | forwardSwipes=" + forwardSwipesUsed
                        + " | reverseRecoverySwipes=" + reverseSwipesUsed
                        + " | bounds=" + rectangleToString(target.getRect())
        );
        sleep(FUND_CHECKPOINT_SETTLE_MS);
        return target;
    }

    private int indexOfOptimizeRegressionFund(String fundName) {
        if (fundName == null) {
            return -1;
        }

        for (int index = 0; index < OPTIMIZE_DRAWER_REGRESSION_FUNDS.length; index++) {
            if (fundName.equals(OPTIMIZE_DRAWER_REGRESSION_FUNDS[index])) {
                return index;
            }
        }
        return -1;
    }

    private int findLaterOptimizeRegressionTargetInSafeBand(int expectedTargetIndex) {
        for (int index = expectedTargetIndex + 1;
             index < OPTIMIZE_DRAWER_REGRESSION_FUNDS.length;
             index++) {
            WebElement later = findVisibleElement(
                    AppiumBy.accessibilityId(OPTIMIZE_DRAWER_REGRESSION_FUNDS[index])
            );
            if (later != null) {
                int[] safeBand = getOptimizeFundSafeVerticalBand();
                if (isElementFullyInsideVerticalBand(later, safeBand[0], safeBand[1])) {
                    return index;
                }
            }
        }
        return -1;
    }

    private OptimizeReverseRecovery recoverExpectedOptimizeMappedTarget(
            By expectedLocator,
            String expectedFundName
    ) {
        WebElement expected = waitForFundInCurrentViewport(expectedLocator, 350);
        if (expected != null) {
            return new OptimizeReverseRecovery(expected, 0);
        }

        for (int reverseSwipe = 1;
             reverseSwipe <= OPTIMIZE_MAX_REVERSE_RECOVERY_SWIPES;
             reverseSwipe++) {
            cardByCardSearchSwipeDown();
            sleep(OPTIMIZE_TARGET_SEARCH_SETTLE_MS);
            expected = waitForFundInCurrentViewport(expectedLocator, 750);
            if (expected != null) {
                return new OptimizeReverseRecovery(expected, reverseSwipe);
            }
        }

        return new OptimizeReverseRecovery(null, OPTIMIZE_MAX_REVERSE_RECOVERY_SWIPES);
    }

    private WebElement recoverOptimizeMappedTargetLocally(
            By fundLocator,
            String fundName
    ) {
        WebElement fund = waitForFundInCurrentViewport(fundLocator, 900);
        if (fund != null) {
            return fund;
        }

        for (int reverseSwipe = 1; reverseSwipe <= 10; reverseSwipe++) {
            cardByCardSearchSwipeDown();
            sleep(OPTIMIZE_TARGET_SEARCH_SETTLE_MS);
            fund = waitForFundInCurrentViewport(fundLocator, 750);
            if (fund != null) {
                diagnostic(
                        "Optimize mapped target locally recovered"
                                + " | fund=" + fundName
                                + " | reverseSwipe=" + reverseSwipe
                );
                return fund;
            }
        }
        return null;
    }

    private static final class OptimizeReverseRecovery {
        private final WebElement fund;
        private final int swipesUsed;

        private OptimizeReverseRecovery(WebElement fund, int swipesUsed) {
            this.fund = fund;
            this.swipesUsed = swipesUsed;
        }
    }

    /**
     * Searches a requested Optimize fund without leaving the current Fund Review.
     *
     * Behaviour:
     * - continue from the CURRENT Optimize scroll position;
     * - use all known fund locators as ordered landmarks;
     * - slow down to precision swipes when the requested fund is near;
     * - if a later fund is observed before the requested target, recover to the
     *   top of Optimize and repeat the same target search;
     * - never navigate back to Hub/Funds during this search.
     */
    private void findOptimizeFundWithTopRecovery(
            By fundLocator,
            String fundName,
            int maxScrollAttemptsPerPass
    ) {
        int targetIndex = indexOfOptimizeFund(fundName);

        if (targetIndex < 0) {
            throw new AssertionError(
                    "Optimize target is not present in the ordered locator map"
                            + " | fund=" + fundName
            );
        }

        ensureSearchThreadIsActive(fundName);

        /*
         * Always establish the first heading as our navigation origin. This makes
         * the scan deterministic even if Flutter restores Fund Review at an old
         * scroll offset from a previous interaction.
         */
        if (optimizeTraversalCheckpointIndex < 0) {
            ensureFirstOptimizeFundCheckpoint(fundName);
        }

        /*
         * A helper may be called again for an earlier card after a Flutter semantics
         * refresh. Recover only inside Optimize and rebuild the checkpoint path.
         */
        if (targetIndex < optimizeTraversalCheckpointIndex) {
            diagnostic(
                    "Requested fund is above the last visited Optimize checkpoint; "
                            + "recovering to the first fund before rebuilding the ordered path"
                            + " | target=" + fundName
                            + " | targetIndex=" + targetIndex
                            + " | lastCheckpointIndex=" + optimizeTraversalCheckpointIndex
            );

            returnToOptimizeTopForSearchRecovery(
                    fundName,
                    0
            );

            optimizeTraversalCheckpointIndex = 0;
            recordOptimizeFundCheckpoint(
                    0,
                    WaitUtils.waitForElement(
                            driver,
                            AppiumBy.accessibilityId(OPTIMIZE_FUNDS_IN_ORDER[0]),
                            10
                    ),
                    "top recovery"
            );
        }

        if (targetIndex > optimizeTraversalCheckpointIndex) {
            advanceThroughOptimizeFundsCardByCard(
                    targetIndex,
                    fundName
            );
        }

        WebElement target =
                waitForFundInCurrentViewport(
                        fundLocator,
                        1400
                );

        if (target == null
                && targetIndex == optimizeTraversalCheckpointIndex) {

            diagnostic(
                    "Visited target heading is temporarily outside the viewport; "
                            + "attempting local reverse recovery"
                            + " | fund=" + fundName
            );

            if (!recoverSpecificVisitedCheckpoint(targetIndex)) {
                returnToOptimizeTopForSearchRecovery(
                        fundName,
                        1
                );

                optimizeTraversalCheckpointIndex = 0;

                if (targetIndex > 0) {
                    advanceThroughOptimizeFundsCardByCard(
                            targetIndex,
                            fundName
                    );
                }
            }

            target =
                    waitForFundInCurrentViewport(
                            fundLocator,
                            1800
                    );
        }

        if (target == null) {
            throw new AssertionError(
                    "Target Optimize fund could not be retained after card-by-card navigation"
                            + " | fund=" + fundName
                            + " | targetIndex=" + targetIndex
                            + " | lastCheckpointIndex=" + optimizeTraversalCheckpointIndex
            );
        }
    }

    /**
     * Makes the first Optimize fund an explicit checkpoint before any downward
     * regression scrolling begins.
     */
    private void ensureFirstOptimizeFundCheckpoint(
            String requestedFundName
    ) {
        String firstFundName = OPTIMIZE_FUNDS_IN_ORDER[0];
        By firstFundLocator = AppiumBy.accessibilityId(firstFundName);

        WebElement firstFund =
                waitForFundInCurrentViewport(
                        firstFundLocator,
                        1200
                );

        if (firstFund == null) {
            diagnostic(
                    "First Optimize fund is not visible; recovering to Optimize top before card-by-card scan"
                            + " | requestedTarget=" + requestedFundName
            );

            returnToOptimizeTopForSearchRecovery(
                    requestedFundName,
                    0
            );

            firstFund =
                    waitForFundInCurrentViewport(
                            firstFundLocator,
                            2500
                    );
        }

        if (firstFund == null) {
            throw new AssertionError(
                    "Unable to establish first Optimize fund checkpoint"
                            + " | expected=" + firstFundName
            );
        }

        optimizeTraversalCheckpointIndex = 0;
        recordOptimizeFundCheckpoint(
                0,
                firstFund,
                "initial Optimize checkpoint"
        );
    }

    /**
     * Walks through every known fund heading between the last visited checkpoint
     * and the requested target. No heading in OPTIMIZE_FUNDS_IN_ORDER is skipped.
     */
    private void advanceThroughOptimizeFundsCardByCard(
            int targetIndex,
            String targetFundName
    ) {
        int recoveryCycle = 0;

        while (optimizeTraversalCheckpointIndex < targetIndex) {
            ensureSearchThreadIsActive(targetFundName);

            int expectedIndex =
                    optimizeTraversalCheckpointIndex + 1;

            boolean found =
                    moveToNextOptimizeFundCheckpoint(
                            expectedIndex,
                            targetFundName
                    );

            if (found) {
                optimizeTraversalCheckpointIndex = expectedIndex;
                continue;
            }

            recoveryCycle++;

            if (recoveryCycle > MAX_CARD_BY_CARD_RECOVERY_CYCLES) {
                throw new AssertionError(
                        "Unable to reach required Optimize fund checkpoint after controlled recoveries"
                                + " | expected=" + OPTIMIZE_FUNDS_IN_ORDER[expectedIndex]
                                + " | target=" + targetFundName
                                + " | recoveryCycles=" + recoveryCycle
                );
            }

            diagnostic(
                    "Could not reliably capture the next ordered Optimize heading; "
                            + "restarting the card-by-card path from the top"
                            + " | expected=" + OPTIMIZE_FUNDS_IN_ORDER[expectedIndex]
                            + " | target=" + targetFundName
                            + " | recoveryCycle=" + recoveryCycle
            );

            returnToOptimizeTopForSearchRecovery(
                    targetFundName,
                    recoveryCycle
            );

            WebElement firstFund =
                    WaitUtils.waitForElement(
                            driver,
                            AppiumBy.accessibilityId(OPTIMIZE_FUNDS_IN_ORDER[0]),
                            10
                    );

            optimizeTraversalCheckpointIndex = 0;
            recordOptimizeFundCheckpoint(
                    0,
                    firstFund,
                    "card-by-card recovery origin"
            );
        }
    }

    /**
     * Finds exactly the next fund heading in the ordered list. The scanner uses
     * small W3C moves and checks the expected accessibility id after every move.
     * If a later fund is exposed first, it reverses immediately instead of
     * continuing downward.
     */
    private boolean moveToNextOptimizeFundCheckpoint(
            int expectedIndex,
            String targetFundName
    ) {
        if (expectedIndex < 0
                || expectedIndex >= OPTIMIZE_FUNDS_IN_ORDER.length) {
            return false;
        }

        String expectedFundName =
                OPTIMIZE_FUNDS_IN_ORDER[expectedIndex];

        By expectedFundLocator =
                AppiumBy.accessibilityId(expectedFundName);

        WebElement expectedFund =
                waitForFundInCurrentViewport(
                        expectedFundLocator,
                        650
                );

        if (expectedFund != null) {
            recordOptimizeFundCheckpoint(
                    expectedIndex,
                    expectedFund,
                    "already visible before scrolling"
            );
            return true;
        }

        String previousFundName =
                optimizeTraversalCheckpointIndex >= 0
                        && optimizeTraversalCheckpointIndex < OPTIMIZE_FUNDS_IN_ORDER.length
                        ? OPTIMIZE_FUNDS_IN_ORDER[optimizeTraversalCheckpointIndex]
                        : "unknown";

        for (int swipe = 1;
             swipe <= MAX_FORWARD_SWIPES_PER_FUND;
             swipe++) {

            ensureSearchThreadIsActive(targetFundName);

            VisibleOptimizeFundWindow before =
                    inspectVisibleOptimizeFundWindow();

            if (before.hasKnownFund()
                    && before.lastIndex > expectedIndex) {

                diagnostic(
                        "A later Optimize fund is already visible before the next card checkpoint; reversing"
                                + " | expected=" + expectedFundName
                                + " | laterVisible=" + before.lastFundName
                );

                return recoverPassedOptimizeCheckpoint(
                        expectedIndex,
                        targetFundName
                );
            }

            diagnostic(
                    "Scrolling Optimize one fund-card checkpoint at a time"
                            + " | from=" + previousFundName
                            + " | next=" + expectedFundName
                            + " | checkpoint=" + (expectedIndex + 1)
                            + "/" + OPTIMIZE_FUNDS_IN_ORDER.length
                            + " | microSwipe=" + swipe
                            + "/" + MAX_FORWARD_SWIPES_PER_FUND
            );

            cardByCardSearchSwipeUp();
            settleAfterSearchScroll();

            expectedFund =
                    waitForFundInCurrentViewport(
                            expectedFundLocator,
                            1200
                    );

            if (expectedFund != null) {
                recordOptimizeFundCheckpoint(
                        expectedIndex,
                        expectedFund,
                        "captured after controlled downward scroll"
                );
                return true;
            }

            VisibleOptimizeFundWindow after =
                    inspectVisibleOptimizeFundWindow();

            if (after.hasKnownFund()
                    && after.lastIndex > expectedIndex) {

                diagnostic(
                        "Scanner moved past the exact next fund checkpoint; reversing immediately"
                                + " | expected=" + expectedFundName
                                + " | laterVisible=" + after.lastFundName
                                + " | microSwipe=" + swipe
                );

                return recoverPassedOptimizeCheckpoint(
                        expectedIndex,
                        targetFundName
                );
            }
        }

        return false;
    }

    /**
     * Reverse-searches only far enough to reacquire a single missed checkpoint.
     * This prevents one Flutter semantics delay from causing the whole list to
     * continue downward past the required fund.
     */
    private boolean recoverPassedOptimizeCheckpoint(
            int expectedIndex,
            String targetFundName
    ) {
        String expectedFundName =
                OPTIMIZE_FUNDS_IN_ORDER[expectedIndex];

        By expectedFundLocator =
                AppiumBy.accessibilityId(expectedFundName);

        for (int reverseSwipe = 1;
             reverseSwipe <= MAX_REVERSE_SWIPES_PER_FUND;
             reverseSwipe++) {

            ensureSearchThreadIsActive(targetFundName);

            WebElement expectedFund =
                    waitForFundInCurrentViewport(
                            expectedFundLocator,
                            450
                    );

            if (expectedFund != null) {
                recordOptimizeFundCheckpoint(
                        expectedIndex,
                        expectedFund,
                        "recovered after passing checkpoint"
                );
                return true;
            }

            diagnostic(
                    "Reversing slightly to recover passed Optimize fund checkpoint"
                            + " | expected=" + expectedFundName
                            + " | reverseSwipe=" + reverseSwipe
                            + "/" + MAX_REVERSE_SWIPES_PER_FUND
            );

            cardByCardSearchSwipeDown();
            settleAfterSearchScroll();

            expectedFund =
                    waitForFundInCurrentViewport(
                            expectedFundLocator,
                            1000
                    );

            if (expectedFund != null) {
                recordOptimizeFundCheckpoint(
                        expectedIndex,
                        expectedFund,
                        "recovered with controlled reverse scroll"
                );
                return true;
            }

            VisibleOptimizeFundWindow window =
                    inspectVisibleOptimizeFundWindow();

            if (window.hasKnownFund()
                    && window.lastIndex < expectedIndex - 1
                    && reverseSwipe >= 3) {

                diagnostic(
                        "Reverse recovery moved above the previous expected region; top recovery will be used"
                                + " | expected=" + expectedFundName
                                + " | latestVisible=" + window.lastFundName
                );
                return false;
            }
        }

        return false;
    }

    /**
     * Local recovery for a fund that was already visited but whose heading moved
     * just above the viewport while its lower calculation area was being exposed.
     */
    private boolean recoverSpecificVisitedCheckpoint(
            int checkpointIndex
    ) {
        String fundName =
                OPTIMIZE_FUNDS_IN_ORDER[checkpointIndex];

        By fundLocator =
                AppiumBy.accessibilityId(fundName);

        for (int reverseSwipe = 1;
             reverseSwipe <= 10;
             reverseSwipe++) {

            WebElement fund =
                    waitForFundInCurrentViewport(
                            fundLocator,
                            400
                    );

            if (fund != null) {
                return true;
            }

            cardByCardSearchSwipeDown();
            settleAfterSearchScroll();
        }

        return waitForFundInCurrentViewport(
                fundLocator,
                900
        ) != null;
    }

    private void recordOptimizeFundCheckpoint(
            int fundIndex,
            WebElement fundElement,
            String reason
    ) {
        if (fundElement == null) {
            throw new AssertionError(
                    "Cannot record null Optimize fund checkpoint"
                            + " | index=" + fundIndex
            );
        }

        String fundName =
                OPTIMIZE_FUNDS_IN_ORDER[fundIndex];

        /*
         * IMPORTANT:
         * Reaching the exact accessibility-id is the checkpoint.
         * Do not perform an extra alignment swipe here.
         *
         * The previous implementation found a heading at the lower edge and then
         * immediately tried to move that SAME Flutter semantics node upward. On this
         * screen Flutter can recycle/unpublish that node during the swipe, which is
         * exactly what happened for ABSL Liquid Reg-G in the failed run.
         *
         * waitForFundInCurrentViewport() now returns a heading only when the FULL
         * heading is inside the safe Optimize viewport, so no extra stabilization
         * movement is required before moving to the next ordered fund.
         */
        Rectangle observedBounds;
        try {
            observedBounds = fundElement.getRect();
        } catch (Exception e) {
            WebElement refreshed =
                    waitForFundInCurrentViewport(
                            AppiumBy.accessibilityId(fundName),
                            1400
                    );

            if (refreshed == null) {
                throw new AssertionError(
                        "Optimize fund checkpoint was detected but could not be re-acquired"
                                + " | fund=" + fundName
                                + " | reason=" + reason
                );
            }

            fundElement = refreshed;
            observedBounds = fundElement.getRect();
        }

        diagnostic(
                "Optimize fund checkpoint captured in safe viewport"
                        + " | " + (fundIndex + 1)
                        + "/" + OPTIMIZE_FUNDS_IN_ORDER.length
                        + " | fund=" + fundName
                        + " | bounds=" + rectangleToString(observedBounds)
                        + " | reason=" + reason
        );

        sleep(FUND_CHECKPOINT_SETTLE_MS);
    }



    private int indexOfOptimizeFund(String fundName) {
        if (fundName == null) {
            return -1;
        }

        for (int index = 0; index < OPTIMIZE_FUNDS_IN_ORDER.length; index++) {
            if (fundName.equals(OPTIMIZE_FUNDS_IN_ORDER[index])) {
                return index;
            }
        }

        return -1;
    }

    /**
     * Reads only currently exposed ImageView semantics and maps them to the
     * ordered fund locators supplied from Inspector. This is lighter and more
     * deterministic than repeatedly searching the whole hierarchy for every fund.
     */
    private VisibleOptimizeFundWindow inspectVisibleOptimizeFundWindow() {
        int firstIndex = Integer.MAX_VALUE;
        int lastIndex = -1;
        String firstFundName = null;
        String lastFundName = null;

        /*
         * Only count fund headings that are genuinely inside the usable Optimize
         * viewport. Flutter can expose the next card's semantics a few pixels above
         * the Android navigation area even though the heading is not yet visually
         * usable. Treating that as visible is what made the scanner think it had
         * reached ABSL Liquid Reg-G at y=2318 in the failed run.
         */
        int[] safeBand = getOptimizeFundSafeVerticalBand();

        try {
            List<WebElement> imageViews =
                    driver.findElements(By.className("android.widget.ImageView"));

            for (WebElement element : imageViews) {
                try {
                    if (element == null
                            || !element.isDisplayed()
                            || !isElementFullyInsideVerticalBand(
                                    element,
                                    safeBand[0],
                                    safeBand[1]
                            )) {
                        continue;
                    }

                    String description = safeAttribute(element, "content-desc");
                    int index = indexOfOptimizeFund(description);

                    if (index < 0) {
                        continue;
                    }

                    if (index < firstIndex) {
                        firstIndex = index;
                        firstFundName = description;
                    }

                    if (index > lastIndex) {
                        lastIndex = index;
                        lastFundName = description;
                    }
                } catch (Exception ignored) {
                    // Flutter may replace one semantics node while the viewport is read.
                }
            }
        } catch (Exception e) {
            diagnostic(
                    "Unable to inspect visible Optimize fund landmarks: " + safeMessage(e)
            );
        }

        return new VisibleOptimizeFundWindow(
                firstIndex == Integer.MAX_VALUE ? -1 : firstIndex,
                lastIndex,
                firstFundName,
                lastFundName
        );
    }

    private WebElement waitForFundInCurrentViewport(
            By fundLocator,
            long timeoutMillis
    ) {
        long deadline =
                System.nanoTime()
                        + Duration.ofMillis(
                                Math.max(1L, timeoutMillis)
                        ).toNanos();

        int[] safeBand = getOptimizeFundSafeVerticalBand();

        do {
            WebElement fund =
                    findVisibleElement(fundLocator);

            if (fund != null
                    && isElementFullyInsideVerticalBand(
                            fund,
                            safeBand[0],
                            safeBand[1]
                    )) {
                return fund;
            }

            sleep(140);

        } while (System.nanoTime() < deadline);

        return null;
    }

    /**
     * Returns the usable vertical area for Optimize fund headings.
     *
     * Top boundary: just below the sticky Optimize/Optimise tab when available.
     * Bottom boundary: 90% of the device height so a semantics node published in
     * the Android navigation / clipped bottom region is NOT considered captured.
     */
    private int[] getOptimizeFundSafeVerticalBand() {
        Dimension screen =
                driver.manage()
                        .window()
                        .getSize();

        int screenHeight = screen.getHeight();
        int safeTop = (int) (screenHeight * 0.18);
        int safeBottom = (int) (screenHeight * 0.90);

        WebElement optimizeTab =
                findVisibleElement(optimiseBucketTab);

        if (optimizeTab == null) {
            optimizeTab =
                    findVisibleElement(optimizeBucketTabFallback);
        }

        if (optimizeTab != null) {
            try {
                Rectangle tabRect = optimizeTab.getRect();
                int tabBottom =
                        tabRect.getY()
                                + tabRect.getHeight();

                safeTop =
                        Math.max(
                                safeTop,
                                tabBottom + 24
                        );
            } catch (Exception ignored) {
                // Use the percentage fallback when Flutter refreshes the tab node.
            }
        }

        if (safeBottom <= safeTop + 200) {
            safeTop = (int) (screenHeight * 0.20);
            safeBottom = (int) (screenHeight * 0.90);
        }

        return new int[] {
                safeTop,
                safeBottom
        };
    }

    private boolean isElementFullyInsideVerticalBand(
            WebElement element,
            int safeTop,
            int safeBottom
    ) {
        if (element == null) {
            return false;
        }

        try {
            Rectangle rect = element.getRect();
            int elementTop = rect.getY();
            int elementBottom =
                    rect.getY()
                            + rect.getHeight();

            return rect.getWidth() > 0
                    && rect.getHeight() > 0
                    && elementTop >= safeTop
                    && elementBottom <= safeBottom;

        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Recovers upward only inside the Optimize list. It never presses Back and
     * never re-opens Hub/Portfolio Analysis.
     */
    private void returnToOptimizeTopForSearchRecovery(
            String targetFundName,
            int completedPass
    ) {
        diagnostic(
                "Recovering to Optimize top without leaving Fund Review"
                        + " | target=" + targetFundName
                        + " | completedPass=" + completedPass
        );

        By topFundLocator =
                AppiumBy.accessibilityId(PRIMARY_OPTIMIZE_FUND_NAME);

        for (int recoverySwipe = 0;
             recoverySwipe <= MAX_TOP_RECOVERY_SWIPES;
             recoverySwipe++) {

            ensureSearchThreadIsActive(targetFundName);

            WebElement topFund =
                    waitForFundInCurrentViewport(
                            topFundLocator,
                            700
                    );

            if (topFund != null) {
                settleAfterSearchScroll();

                diagnostic(
                        "Optimize top recovered on exact first-fund locator"
                                + " | anchor=" + PRIMARY_OPTIMIZE_FUND_NAME
                                + " | target=" + targetFundName
                                + " | recoverySwipes=" + recoverySwipe
                );
                return;
            }

            if (recoverySwipe == MAX_TOP_RECOVERY_SWIPES) {
                break;
            }

            diagnostic(
                    "Scrolling upward one controlled step toward Optimize top"
                            + " | target=" + targetFundName
                            + " | recoverySwipe=" + (recoverySwipe + 1)
                            + "/" + MAX_TOP_RECOVERY_SWIPES
            );

            cardByCardSearchSwipeDown();
            settleAfterSearchScroll();
        }

        throw new AssertionError(
                "Unable to recover exact Optimize top fund after controlled upward scrolling"
                        + " | expectedTopFund=" + PRIMARY_OPTIMIZE_FUND_NAME
                        + " | target=" + targetFundName
        );
    }


    private void ensureSearchThreadIsActive(
            String fundName
    ) {
        if (Thread.currentThread().isInterrupted()) {
            throw new RuntimeException(
                    "Optimize fund search was interrupted"
                            + " | fund=" + fundName
            );
        }
    }

    private static final class VisibleOptimizeFundWindow {
        private final int firstIndex;
        private final int lastIndex;
        private final String firstFundName;
        private final String lastFundName;

        private VisibleOptimizeFundWindow(
                int firstIndex,
                int lastIndex,
                String firstFundName,
                String lastFundName
        ) {
            this.firstIndex = firstIndex;
            this.lastIndex = lastIndex;
            this.firstFundName = firstFundName;
            this.lastFundName = lastFundName;
        }

        private boolean hasKnownFund() {
            return firstIndex >= 0 && lastIndex >= 0;
        }
    }

    private void positionFundAnchorForFullCalculationCard(
            By fundLocator,
            String fundName,
            int maxScrollAttemptsPerPass
    ) {
        Dimension screen =
                driver.manage()
                        .window()
                        .getSize();

        int desiredUpperY =
                (int) (screen.getHeight() * 0.34);

        for (int attempt = 1; attempt <= 7; attempt++) {
            WebElement fund =
                    waitForFundInCurrentViewport(
                            fundLocator,
                            650
                    );

            if (fund == null) {
                diagnostic(
                        "Fund anchor disappeared while positioning card; using local mapped-target recovery"
                                + " | fund=" + fundName
                );

                fund = recoverOptimizeMappedTargetLocally(fundLocator, fundName);
                if (fund == null) {
                    throw new AssertionError(
                            "Optimize mapped target could not be locally recovered while positioning the card"
                                    + " | fund=" + fundName
                    );
                }

                continue;
            }

            int fundY =
                    fund.getRect()
                            .getY();

            if (fundY <= desiredUpperY) {
                return;
            }

            diagnostic(
                    "Moving fund anchor into upper viewport with gentle W3C swipe"
                            + " | fund=" + fundName
                            + " | currentY=" + fundY
                            + " | attempt=" + attempt
                            + " of 7"
            );

            gentleSearchSwipeUp();
            settleAfterSearchScroll();
        }
    }

    /**
     * The three labels and trigger are repeated for every Optimize fund. A
     * simple accessibility-id lookup can therefore return a neighboring card.
     * This method anchors all repeated controls to the requested fund using
     * their live Y coordinates.
     */
    private OptimizeCardElements locateVisibleCalculationElementsForFund(
            String fundName
    ) {
        By fundLocator =
                AppiumBy.accessibilityId(fundName);

        WebElement fund =
                findVisibleElement(fundLocator);

        if (fund == null) {
            return null;
        }

        WebElement alreadyPaid =
                findClosestVisibleBelow(
                        fund,
                        alreadyPaidExtra,
                        0.48
                );

        WebElement next12 =
                findClosestVisibleBelow(
                        fund,
                        next12Months,
                        0.48
                );

        WebElement next3 =
                findClosestVisibleBelow(
                        fund,
                        next3Years,
                        0.48
                );

        WebElement trigger =
                findClosestVisibleBelow(
                        fund,
                        howIsThisCalculated,
                        0.52
                );

        if (alreadyPaid == null
                || next12 == null
                || next3 == null
                || trigger == null) {
            return null;
        }

        return new OptimizeCardElements(
                fund,
                alreadyPaid,
                next12,
                next3,
                trigger
        );
    }

    private WebElement findClosestVisibleBelow(
            WebElement anchor,
            By locator,
            double maxDistanceAsScreenFraction
    ) {
        if (anchor == null) {
            return null;
        }

        int anchorY;
        int maxDistance;

        try {
            anchorY =
                    anchor.getRect()
                            .getY();

            maxDistance =
                    (int) (
                            driver.manage()
                                    .window()
                                    .getSize()
                                    .getHeight()
                                    * maxDistanceAsScreenFraction
                    );

        } catch (Exception e) {
            return null;
        }

        WebElement closest = null;
        int closestY = Integer.MAX_VALUE;

        try {
            List<WebElement> candidates =
                    driver.findElements(locator);

            for (WebElement candidate : candidates) {
                try {
                    if (candidate == null
                            || !candidate.isDisplayed()) {
                        continue;
                    }

                    Rectangle rect =
                            candidate.getRect();

                    int candidateY =
                            rect.getY();

                    int distance =
                            candidateY - anchorY;

                    if (rect.getWidth() <= 0
                            || rect.getHeight() <= 0
                            || distance <= 0
                            || distance > maxDistance) {
                        continue;
                    }

                    if (candidateY < closestY) {
                        closest = candidate;
                        closestY = candidateY;
                    }

                } catch (Exception ignored) {
                    // Flutter may replace one candidate while the hierarchy is read.
                }
            }

        } catch (Exception e) {
            diagnostic(
                    "Repeated card-control lookup failed: "
                            + safeMessage(e)
            );
        }

        return closest;
    }

    private void validateCardElementGeometry(
            OptimizeCardElements card,
            String fundName
    ) {
        int fundTop =
                card.fund.getRect()
                        .getY();

        int alreadyPaidTop =
                card.alreadyPaid.getRect()
                        .getY();

        int next12Top =
                card.next12.getRect()
                        .getY();

        int next3Top =
                card.next3.getRect()
                        .getY();

        int triggerTop =
                card.trigger.getRect()
                        .getY();

        int lowestBoxTop =
                Math.max(
                        alreadyPaidTop,
                        Math.max(
                                next12Top,
                                next3Top
                        )
                );

        diagnostic(
                "Optimize card geometry"
                        + " | fund=" + fundName
                        + " | fundY=" + fundTop
                        + " | alreadyPaidY=" + alreadyPaidTop
                        + " | next12Y=" + next12Top
                        + " | next3Y=" + next3Top
                        + " | triggerY=" + triggerTop
        );

        if (alreadyPaidTop <= fundTop
                || next12Top <= fundTop
                || next3Top <= fundTop) {
            throw new AssertionError(
                    "Calculation labels were not associated with the requested fund card"
                            + " | fund=" + fundName
            );
        }

        if (triggerTop <= lowestBoxTop) {
            throw new AssertionError(
                    "How is this calculated? is not below the three calculation boxes"
                            + " | fund=" + fundName
            );
        }
    }

    // =========================================================
    // OPEN FUND REVIEW ROBUSTLY
    // =========================================================

    /**
     * Opens Fund Review from the Funds/SIFs lead-in row.
     *
     * The current run showed an intermittent failure when a normal Flutter row
     * click caused the semantics node to disappear before Fund Review finished
     * rendering. For stability, the W3C lead-in/caret hotspot is now the primary
     * action. Direct element click is retained only as a fallback.
     */
    private void openFundReviewFromFundRowRobustly(
            By fundLocator,
            String fundName
    ) {
        if (isVisibleWithin(fundReviewHeader, 1)) {
            diagnostic("Fund Review is already open");
            return;
        }

        WebElement fundRow =
                WaitUtils.waitForElement(
                        driver,
                        fundLocator,
                        30
                );

        Rectangle originalRect =
                fundRow.getRect();

        diagnostic(
                "Fund row located"
                        + " | fund=" + fundName
                        + " | class=" + safeAttribute(fundRow, "class")
                        + " | clickable=" + safeAttribute(fundRow, "clickable")
                        + " | bounds=" + rectangleToString(originalRect)
        );

        /*
         * Attempt 1: tap the right-side lead-in/caret directly. This is the
         * actual navigation affordance defined by the Fund Review journey and
         * avoids relying on Flutter's full-row click dispatch.
         */
        int chevronX =
                originalRect.getX()
                        + (int) (originalRect.getWidth() * 0.965);

        int chevronY =
                originalRect.getY()
                        + (int) (originalRect.getHeight() * 0.50);

        diagnostic(
                "Tapping fund lead-in/caret using W3C"
                        + " | fund=" + fundName
                        + " | x=" + chevronX
                        + " | y=" + chevronY
        );

        tapByCoordinates(
                chevronX,
                chevronY
        );

        if (waitForFundReviewOpen(20)) {
            diagnostic(
                    "Fund Review opened using W3C lead-in/caret tap"
            );
            return;
        }

        /*
         * If Funds/SIFs controls have disappeared, navigation probably started
         * and QA is simply rendering slowly. Give it a longer transition wait
         * before attempting a second interaction.
         */
        if (!isVisibleWithin(fundsSifsTab, 1)) {
            diagnostic(
                    "Funds / SIFs controls disappeared after lead-in tap; waiting for delayed Fund Review render"
            );

            if (waitForFundReviewOpen(25)) {
                diagnostic(
                        "Fund Review opened after delayed Flutter transition"
                );
                return;
            }
        }

        /*
         * Attempt 2: re-acquire and use normal click. Re-finding is important
         * because Flutter often replaces the original semantics node.
         */
        fundRow =
                findVisibleElement(fundLocator);

        if (fundRow != null) {
            try {
                diagnostic(
                        "W3C lead-in tap did not open Fund Review. Trying re-acquired fund-row click"
                                + " | fund=" + fundName
                );

                fundRow.click();

                if (waitForFundReviewOpen(20)) {
                    diagnostic(
                            "Fund Review opened using re-acquired fund-row click"
                    );
                    return;
                }

            } catch (Exception e) {
                diagnostic(
                        "Re-acquired fund-row click failed: "
                                + safeMessage(e)
                );
            }
        }

        /*
         * Attempt 3: the row may have been replaced during a Flutter rebuild.
         * If the page itself is still Funds/SIFs, wait briefly for the known row
         * to return, then tap the right-middle area.
         */
        if (isVisibleWithin(fundsSifsTab, 2)) {
            long deadline =
                    System.nanoTime()
                            + Duration.ofSeconds(8).toNanos();

            while (System.nanoTime() < deadline) {
                fundRow =
                        findVisibleElement(fundLocator);

                if (fundRow != null) {
                    diagnostic(
                            "Trying final right-middle W3C tap on re-rendered fund row"
                                    + " | fund=" + fundName
                    );

                    tapElementByPercent(
                            fundRow,
                            0.82,
                            0.50
                    );

                    if (waitForFundReviewOpen(20)) {
                        diagnostic(
                                "Fund Review opened using final W3C fund-row fallback"
                        );
                        return;
                    }

                    break;
                }

                sleep(300);
            }
        }

        throw new AssertionError(
                "Unable to open Fund Review"
                        + " | fund=" + fundName
                        + " | W3C lead-in, delayed-render wait and row-click fallbacks exhausted"
        );
    }

    private boolean waitForFundReviewOpen(
            int seconds
    ) {
        return isVisibleWithin(
                fundReviewHeader,
                seconds
        );
    }

    // =========================================================
    // VERIFY PRIMARY CALCULATION BOXES + TRIGGER
    // =========================================================

    public void verifyThreeCalculationBoxesAndTriggerForPortfolioAnalysisFa() {
        diagnostic(
                "Verifying primary Optimize fund calculation boxes and trigger"
        );

        OptimizeCardElements card =
                positionApplicableOptimizeCalculationCard();

        validateCardElementGeometry(
                card,
                PRIMARY_OPTIMIZE_FUND_NAME
        );

        diagnostic(
                "Verified trigger placement for "
                        + PRIMARY_OPTIMIZE_FUND_NAME
                        + ": How is this calculated? appears below "
                        + "Already paid extra, Next 12 months and Next 3 years"
        );
    }

    // =========================================================
    // SINGLE PRIMARY DRAWER VALIDATION
    // =========================================================

    public void openAndValidateCalculationDrawerForPortfolioAnalysisFa() {
        diagnostic(
                "Opening and validating primary How is this calculated? drawer against PRD"
        );

        OptimizeCardElements card =
                positionApplicableOptimizeCalculationCard();

        openAndValidateCalculationDrawerForFund(
                PRIMARY_OPTIMIZE_FUND_NAME,
                card,
                false
        );
    }

    // =========================================================
    // MULTI-FUND REGRESSION: FIRST + 2 MIDDLE + LAST
    // =========================================================

    /**
     * Regression coverage requested for the Optimize page:
     *
     * 1. Existing first/baseline fund.
     * 2. Axis ELSS - Tax Saver Reg-G (middle card).
     * 3. Mirae Asset Large Cap Reg-G (middle card).
     * 4. UTI Liquid Reg-G (end-page card).
     *
     * For every fund we anchor the repeated labels to that fund, open the
     * calculation drawer, match all exact PRD text, close the drawer and remain
     * in Fund Review before moving downward to the next fund.
     */
    public RegressionSummary verifyCalculationDrawersAcrossOptimizeRegressionFundsForPortfolioAnalysisFa() {
        verifyOptimizeTabStripIsVisible("before multi-fund regression");

        int matched = 0;
        for (String fundName : OPTIMIZE_DRAWER_REGRESSION_FUNDS) {
            int maxScrollAttempts = LAST_OPTIMIZE_FUND_NAME.equals(fundName) ? 110 : 80;
            OptimizeCardElements card =
                    positionOptimizeFundCalculationCard(fundName, maxScrollAttempts);

            openAndValidateCalculationDrawerForFund(fundName, card, true);
            matched++;

            ReportLogger.pass(
                    "[MATCH] Optimize | " + fundName
                            + " | calculation cards + PRD drawer verified"
            );
        }

        return new RegressionSummary(
                "Optimize",
                OPTIMIZE_DRAWER_REGRESSION_FUNDS.length,
                matched,
                matched,
                0,
                0
        );
    }

    private void openAndValidateCalculationDrawerForFund(
            String fundName,
            OptimizeCardElements card,
            boolean closeAfterValidation
    ) {
        if (card == null) {
            throw new AssertionError(
                    "Cannot validate calculation drawer because the anchored card is null"
                            + " | fund=" + fundName
            );
        }

        validateCardElementGeometry(
                card,
                fundName
        );

        diagnostic(
                "Opening How is this calculated? drawer"
                        + " | fund=" + fundName
                        + " | triggerBounds="
                        + rectangleToString(card.trigger.getRect())
                        + " | clickable="
                        + safeAttribute(card.trigger, "clickable")
        );

        openCalculationDrawerRobustly(
                fundName,
                card.trigger
        );

        validateExactCalculationDrawerPrdText(
                fundName
        );

        if (closeAfterValidation) {
            closeCalculationDrawerAndStayOnOptimize(
                    fundName
            );
        }
    }

    private void openCalculationDrawerRobustly(
            String fundName,
            WebElement anchoredTrigger
    ) {
        WebElement trigger =
                anchoredTrigger;

        try {
            trigger.click();

            if (isCalculationDrawerOpen()) {
                settleAfterDrawerOpen(
                        "How is this calculated"
                );

                diagnostic(
                        "Calculation drawer opened using anchored trigger click"
                                + " | fund=" + fundName
                );

                return;
            }

        } catch (Exception e) {
            diagnostic(
                    "Anchored trigger click failed/was ignored"
                            + " | fund=" + fundName
                            + " | error=" + safeMessage(e)
            );
        }

        /*
         * Re-anchor after a stale Flutter semantics refresh. This deliberately
         * avoids driver.findElement(howIsThisCalculated), which could select a
         * neighboring fund's identical trigger.
         */
        OptimizeCardElements refreshedCard =
                locateVisibleCalculationElementsForFund(
                        fundName
                );

        if (refreshedCard == null) {
            refreshedCard =
                    positionOptimizeFundCalculationCard(
                            fundName,
                            4
                    );
        }

        trigger =
                refreshedCard.trigger;

        diagnostic(
                "Using W3C centre tap on re-anchored How is this calculated? trigger"
                        + " | fund=" + fundName
        );

        tapElementCenter(trigger);

        WaitUtils.waitForElement(
                driver,
                drawerAlreadyPaidExtraText,
                20
        );

        settleAfterDrawerOpen(
                "How is this calculated"
        );

        diagnostic(
                "Calculation drawer opened using W3C trigger fallback"
                        + " | fund=" + fundName
        );
    }

    private void validateExactCalculationDrawerPrdText(
            String fundName
    ) {
        diagnostic(
                "Matching calculation drawer text against PRD"
                        + " | fund=" + fundName
        );

        WaitUtils.waitForElement(
                driver,
                drawerAlreadyPaidExtraText,
                20
        );

        WaitUtils.waitForElement(
                driver,
                drawerNext12MonthsAnd3YearsText,
                20
        );

        WaitUtils.waitForElement(
                driver,
                drawerAnnualExpenseHeading,
                20
        );

        WaitUtils.waitForElement(
                driver,
                drawerAnnualExpenseText,
                20
        );

        verifyDrawerTitleIsVisibleInBottomSheet();

        diagnostic(
                "Exact PRD drawer text matched"
                        + " | fund=" + fundName
        );
    }

    private boolean isCalculationDrawerOpen() {
        return isVisibleWithin(
                drawerAlreadyPaidExtraText,
                8
        );
    }

    // =========================================================
    // CLOSE DRAWER AND CONTINUE SAME OPTIMIZE LIST
    // =========================================================

    private void closeCalculationDrawerAndStayOnOptimize(
            String fundName
    ) {
        diagnostic(
                "Closing calculation drawer before continuing to next fund"
                        + " | fund=" + fundName
        );

        /*
         * Android Back is the least destructive close action for this bottom
         * sheet and preserves the underlying Optimize scroll position.
         */
        pressBackSafely();

        if (!WaitUtils.waitForInvisible(
                driver,
                drawerAlreadyPaidExtraText,
                8
        )) {
            diagnostic(
                    "Android Back did not close calculation drawer; using drag-down fallback"
                            + " | fund=" + fundName
            );

            WebElement drawerTitle =
                    findDrawerTitleInBottomSheet();

            if (drawerTitle == null) {
                throw new AssertionError(
                        "Calculation drawer remained open and bottom-sheet title could not be located"
                                + " | fund=" + fundName
                );
            }

            Rectangle titleRect =
                    drawerTitle.getRect();

            Dimension screen =
                    driver.manage()
                            .window()
                            .getSize();

            int startX =
                    titleRect.getX()
                            + titleRect.getWidth() / 2;

            int startY =
                    titleRect.getY()
                            + titleRect.getHeight() / 2;

            int endY =
                    (int) (screen.getHeight() * 0.94);

            swipeByCoordinates(
                    startX,
                    startY,
                    startX,
                    endY,
                    650
            );

            if (!WaitUtils.waitForInvisible(
                    driver,
                    drawerAlreadyPaidExtraText,
                    8
            )) {
                throw new AssertionError(
                        "Calculation drawer did not close after Android Back and drag-down fallback"
                                + " | fund=" + fundName
                );
            }
        }

        verifyOptimizeTabStripIsVisible(
                "after closing drawer for " + fundName
        );

        sleep(400);

        diagnostic(
                "Calculation drawer closed; Optimize Fund Review retained"
                        + " | fund=" + fundName
        );
    }

    private void verifyOptimizeTabStripIsVisible(
            String context
    ) {
        boolean optimiseVisible =
                isVisibleWithin(
                        optimiseBucketTab,
                        3
                );

        boolean optimizeVisible =
                optimiseVisible
                        || isVisibleWithin(
                                optimizeBucketTabFallback,
                                3
                        );

        if (!optimizeVisible) {
            throw new AssertionError(
                    "Optimize/Optimise tab strip is not visible"
                            + " | context=" + context
            );
        }
    }

    private WebElement findDrawerTitleInBottomSheet() {
        Dimension screen =
                driver.manage()
                        .window()
                        .getSize();

        int drawerThresholdY =
                (int) (screen.getHeight() * 0.45);

        try {
            List<WebElement> titles =
                    driver.findElements(howIsThisCalculated);

            for (WebElement title : titles) {
                try {
                    if (title != null
                            && title.isDisplayed()
                            && title.getRect().getY() >= drawerThresholdY) {
                        return title;
                    }

                } catch (Exception ignored) {
                    // Ignore stale Flutter semantics nodes.
                }
            }

        } catch (Exception e) {
            diagnostic(
                    "Bottom-sheet title lookup failed: "
                            + safeMessage(e)
            );
        }

        return null;
    }

    private static final class OptimizeCardElements {
        private final WebElement fund;
        private final WebElement alreadyPaid;
        private final WebElement next12;
        private final WebElement next3;
        private final WebElement trigger;

        private OptimizeCardElements(
                WebElement fund,
                WebElement alreadyPaid,
                WebElement next12,
                WebElement next3,
                WebElement trigger
        ) {
            this.fund = fund;
            this.alreadyPaid = alreadyPaid;
            this.next12 = next12;
            this.next3 = next3;
            this.trigger = trigger;
        }
    }

    // =========================================================
    // GOOD BUCKET - GOOD START ADVICE REGRESSION
    // =========================================================

    /**
     * Current scope = yellow-highlighted Good Start rows only:
     *
     * TC-02: Edelweiss Technology Direct + Regular
     *        Sector fund (Technology), Mutual Fund, NOT overconcentrated.
     *
     * TC-01: Motilal Oswal Large Cap Direct + Regular
     *        Mutual Fund, excluding ETFs and sector funds.
     *
     * Physical cards between mapped targets are intentionally ignored. The flow
     * searches directly for each mapped accessibility id using bounded small swipes.
     */
    public RegressionSummary verifyGoodStartAdviceAcrossGoodFundsForPortfolioAnalysisFa() {
        verifyGoodBucketReady("before Good Start advice regression");

        goodTraversalCheckpointIndex = -1;
        List<String> prdMismatches = new ArrayList<>();
        List<String> automationFailures = new ArrayList<>();
        int validatedAgainstPrd = 0;
        int prdMatches = 0;

        for (GoodAdviceExpectation expectation : GOOD_START_EXPECTATIONS) {
            moveGoodTraversalToMappedFund(expectation.fundName);

            AdviceValidationResult result =
                    validateMappedGoodStartAdviceForCurrentFund(expectation);
            validatedAgainstPrd += recordAdviceValidationResult(
                    "GOOD", result, prdMismatches, automationFailures);
            if (result != null && result.kind == AdviceValidationKind.MATCH) {
                prdMatches++;
            }
        }

        RegressionSummary summary = new RegressionSummary(
                "Good", GOOD_START_EXPECTATIONS.length, validatedAgainstPrd, prdMatches,
                prdMismatches.size(), automationFailures.size());

        assertNoAutomationFailures("Good Start Our Advice", automationFailures);
        return summary;
    }

    private GoodAdviceExpectation expectationForGoodFund(
            String fundName
    ) {
        for (GoodAdviceExpectation expectation : GOOD_START_EXPECTATIONS) {
            if (expectation.fundName.equals(fundName)) {
                return expectation;
            }
        }

        throw new AssertionError(
                "No Good Start advice expectation is mapped for fund"
                        + " | fund=" + fundName
        );
    }

    private AdviceValidationResult validateMappedGoodStartAdviceForCurrentFund(
            GoodAdviceExpectation expectation
    ) {
        if (expectation == null) {
            String detail = "Good Start expectation is null";
            ReportLogger.step("[AUTOMATION] Good | " + detail);
            return AdviceValidationResult.automationFailure(detail);
        }

        WebElement fund = positionGoodMappedFundForAdvice(
                expectation.fundName,
                AppiumBy.accessibilityId(expectation.fundName)
        );
        if (fund == null) {
            String detail = "fund not positioned | " + expectation.fundName;
            ReportLogger.step("[AUTOMATION] Good | " + detail);
            return AdviceValidationResult.automationFailure(detail);
        }

        WebElement adviceLabel = findClosestVisibleBelowOnScreen(fund, ourAdviceLabel, 0.58);
        WebElement adviceText = adviceLabel == null
                ? null
                : findClosestVisibleBelowOnScreen(
                        adviceLabel,
                        strongStartAdviceText,
                        0.16
                );

        /*
         * Flutter can recycle the label/text semantics immediately after the final
         * positioning swipe. Re-acquire the same mapped card once before declaring
         * an automation failure. No bucket navigation or global recovery is used.
         */
        if (adviceLabel == null || adviceText == null) {
            sleep(180);
            fund = positionGoodMappedFundForAdvice(
                    expectation.fundName,
                    AppiumBy.accessibilityId(expectation.fundName)
            );

            if (fund != null) {
                adviceLabel = findClosestVisibleBelowOnScreen(fund, ourAdviceLabel, 0.58);
                adviceText = adviceLabel == null
                        ? null
                        : findClosestVisibleBelowOnScreen(
                                adviceLabel,
                                strongStartAdviceText,
                                0.16
                        );
            }
        }

        if (adviceLabel == null) {
            String detail = "Our Advice missing | " + expectation.fundName + " | " + expectation.plan;
            ReportLogger.step("[AUTOMATION] Good | " + detail);
            return AdviceValidationResult.automationFailure(detail);
        }

        if (adviceText == null) {
            String detail = "advice text missing | " + expectation.fundName + " | " + expectation.plan;
            ReportLogger.step("[AUTOMATION] Good | " + detail);
            return AdviceValidationResult.automationFailure(detail);
        }

        String actualAdvice = normalizeUiText(getReadableText(adviceText));
        boolean matchesPrd = matchesPrdTemplateWithDynamicMonthYear(
                actualAdvice, expectation.prdTemplate);

        if (!matchesPrd) {
            String detail = expectation.manualCaseId
                    + " | " + expectation.fundName
                    + " | " + expectation.plan
                    + " | expected=\"" + expectation.prdTemplate + "\""
                    + " | actual=\"" + actualAdvice + "\"";
            ReportLogger.fail("[MISMATCH] Good | " + detail);
            return AdviceValidationResult.prdMismatch(detail);
        }

        ReportLogger.pass(
                "[MATCH] Good | " + expectation.manualCaseId
                        + " | " + expectation.fundName
                        + " | " + expectation.plan
                        + " | \"" + actualAdvice + "\""
        );
        return AdviceValidationResult.match();
    }

    /**
     * Good traversal is target-driven. Only the mapped PRD targets are required;
     * unrelated/new funds between them are ignored and need no locators.
     */
    private void moveGoodTraversalToMappedFund(String expectedMappedFund) {
        int targetIndex = indexOfGoodMappedFund(expectedMappedFund);
        if (targetIndex < 0) {
            throw new AssertionError(
                    "Mapped Good fund is missing from the validation target map"
                            + " | fund=" + expectedMappedFund
            );
        }

        if (goodTraversalCheckpointIndex > targetIndex) {
            throw new AssertionError(
                    "Good target-driven traversal cannot move backwards"
                            + " | requested=" + expectedMappedFund
            );
        }

        By targetLocator = AppiumBy.accessibilityId(expectedMappedFund);
        WebElement target = waitForGoodFundInCardBand(targetLocator, 550);
        int forwardSwipes = 0;
        int reverseSwipes = 0;

        while (target == null && forwardSwipes < GOOD_MAX_TARGET_SEARCH_SWIPES) {
            int laterMapped = findLaterGoodMappedTargetInCardBand(targetIndex);
            if (laterMapped > targetIndex) {
                GoodReverseRecovery recovery = recoverExpectedGoodMappedTarget(
                        targetLocator, expectedMappedFund);
                reverseSwipes += recovery.swipesUsed;
                target = recovery.fund;
                if (target != null) {
                    break;
                }
            }

            forwardSwipes++;
            goodSmallFixedSwipeUp();
            sleep(GOOD_TARGET_SEARCH_SETTLE_MS);
            target = waitForGoodFundInCardBand(targetLocator, 850);
        }

        if (target == null) {
            throw new AssertionError(
                    "Unable to locate mapped Good fund after bounded target-driven scrolling"
                            + " | target=" + expectedMappedFund
                            + " | forwardSwipesUsed=" + forwardSwipes
                            + " | reverseRecoverySwipesUsed=" + reverseSwipes
                            + " | intermediateUnknownFunds=ignored"
                            + " | fling=false"
            );
        }

        goodTraversalCheckpointIndex = targetIndex;
        recordGoodMappedCheckpoint(
                targetIndex,
                target,
                "captured mapped Good target"
                        + " | forwardSwipes=" + forwardSwipes
                        + " | reverseRecoverySwipes=" + reverseSwipes
        );
    }

    private int indexOfGoodMappedFund(String fundName) {
        if (fundName == null) {
            return -1;
        }
        for (int index = 0; index < GOOD_MAPPED_FUNDS_IN_ORDER.length; index++) {
            if (fundName.equals(GOOD_MAPPED_FUNDS_IN_ORDER[index])) {
                return index;
            }
        }
        return -1;
    }

    private int findLaterGoodMappedTargetInCardBand(int expectedIndex) {
        for (int index = expectedIndex + 1; index < GOOD_MAPPED_FUNDS_IN_ORDER.length; index++) {
            WebElement later = findVisibleElement(
                    AppiumBy.accessibilityId(GOOD_MAPPED_FUNDS_IN_ORDER[index]));
            if (later != null && isGoodFundHeadingInCardBand(later)) {
                return index;
            }
        }
        return -1;
    }

    private GoodReverseRecovery recoverExpectedGoodMappedTarget(
            By expectedLocator,
            String expectedFundName
    ) {
        WebElement expected = waitForGoodFundInCardBand(expectedLocator, 350);
        if (expected != null) {
            return new GoodReverseRecovery(expected, 0);
        }

        for (int reverse = 1; reverse <= GOOD_MAX_REVERSE_RECOVERY_SWIPES; reverse++) {
            goodFineSwipeDown();
            sleep(GOOD_TARGET_SEARCH_SETTLE_MS);
            expected = waitForGoodFundInCardBand(expectedLocator, 700);
            if (expected != null) {
                diagnostic(
                        "Good mapped target reverse-recovered"
                                + " | fund=" + expectedFundName
                                + " | reverseSwipe=" + reverse
                );
                return new GoodReverseRecovery(expected, reverse);
            }
        }
        return new GoodReverseRecovery(null, GOOD_MAX_REVERSE_RECOVERY_SWIPES);
    }

    private WebElement positionGoodMappedFundForAdvice(
            String fundName,
            By fundLocator
    ) {
        /*
         * Keep the target-driven traversal, but restore the proven Good-card
         * positioning rule used by the earlier stable flow: the fund heading
         * must be moved into the upper-middle part of the screen before advice
         * is read. Merely seeing "Our Advice" is not enough because Flutter can
         * expose that label while the actual advice text is still clipped below
         * the viewport. That produced the intermittent "advice text missing"
         * failures on Regular/Direct cards in the combined run.
         */
        Dimension screen = driver.manage().window().getSize();
        int desiredTop = (int) (screen.getHeight() * 0.22);
        int desiredBottom = (int) (screen.getHeight() * 0.36);

        for (int attempt = 0; attempt <= GOOD_MAX_ADVICE_POSITION_SWIPES; attempt++) {
            WebElement fund = waitForGoodFundInCardBand(fundLocator, 600);

            if (fund == null) {
                /*
                 * The heading was just captured by moveGoodTraversalToMappedFund().
                 * If Flutter moved it a little above the safe band while settling,
                 * recover locally only; never restart/re-scan the Good bucket.
                 */
                goodFineSwipeDown();
                sleep(GOOD_TARGET_SEARCH_SETTLE_MS);
                continue;
            }

            int fundY;
            try {
                fundY = fund.getRect().getY();
            } catch (Exception stale) {
                sleep(180);
                continue;
            }

            if (fundY > desiredBottom) {
                goodFineSwipeUp();
                sleep(GOOD_TARGET_SEARCH_SETTLE_MS);
                continue;
            }

            if (fundY < desiredTop) {
                goodFineSwipeDown();
                sleep(GOOD_TARGET_SEARCH_SETTLE_MS);
                continue;
            }

            WebElement adviceLabel =
                    findClosestVisibleBelowOnScreen(fund, ourAdviceLabel, 0.58);

            WebElement adviceText = adviceLabel == null
                    ? null
                    : findClosestVisibleBelowOnScreen(
                            adviceLabel,
                            strongStartAdviceText,
                            0.16
                    );

            if (adviceLabel != null && adviceText != null) {
                return fund;
            }

            /*
             * Heading is correctly anchored but the lower advice text has not
             * entered the viewport yet. Nudge upward once and re-acquire all live
             * semantics nodes on the next loop iteration.
             */
            if (attempt < GOOD_MAX_ADVICE_POSITION_SWIPES) {
                goodAdviceNudgeUp();
                sleep(GOOD_TARGET_SEARCH_SETTLE_MS);
            }
        }

        WebElement finalFund = waitForGoodFundInCardBand(fundLocator, 1000);
        if (finalFund != null) {
            WebElement finalAdviceLabel =
                    findClosestVisibleBelowOnScreen(finalFund, ourAdviceLabel, 0.58);
            WebElement finalAdviceText = finalAdviceLabel == null
                    ? null
                    : findClosestVisibleBelowOnScreen(
                            finalAdviceLabel,
                            strongStartAdviceText,
                            0.16
                    );

            if (finalAdviceLabel != null && finalAdviceText != null) {
                return finalFund;
            }
        }

        diagnostic(
                "Good mapped target advice block could not be fully positioned"
                        + " | fund=" + fundName
                        + " | requirement=fund + Our Advice + Strong start text visible"
        );
        return null;
    }

    private void recordGoodMappedCheckpoint(
            int mappedIndex,
            WebElement fundElement,
            String reason
    ) {
        if (fundElement == null) {
            throw new AssertionError("Cannot record null Good mapped checkpoint | index=" + mappedIndex);
        }

        String fundName = GOOD_MAPPED_FUNDS_IN_ORDER[mappedIndex];
        diagnostic(
                "Good mapped fund checkpoint captured"
                        + " | " + (mappedIndex + 1)
                        + "/" + GOOD_MAPPED_FUNDS_IN_ORDER.length
                        + " | fund=" + fundName
                        + " | bounds=" + rectangleToString(fundElement.getRect())
                        + " | reason=" + reason
        );
        sleep(GOOD_CARD_CHECKPOINT_SETTLE_MS);
    }

    private static final class GoodReverseRecovery {
        private final WebElement fund;
        private final int swipesUsed;

        private GoodReverseRecovery(WebElement fund, int swipesUsed) {
            this.fund = fund;
            this.swipesUsed = swipesUsed;
        }
    }

    private void verifyGoodBucketReady(
            String context
    ) {
        if (!isVisibleWithin(goodBucketTab, 3)) {
            throw new AssertionError(
                    "Good tab strip is not visible"
                            + " | context=" + context
            );
        }

        if (!isVisibleWithin(goodReviewHeader, 3)) {
            throw new AssertionError(
                    "Funds that are good heading is not visible"
                            + " | context=" + context
            );
        }
    }

    private WebElement waitForGoodFundInCardBand(
            By fundLocator,
            long timeoutMillis
    ) {
        long deadline =
                System.nanoTime()
                        + Duration.ofMillis(
                                Math.max(1L, timeoutMillis)
                        ).toNanos();

        do {
            WebElement fund =
                    findVisibleElement(fundLocator);

            if (fund != null
                    && isGoodFundHeadingInCardBand(fund)) {
                return fund;
            }

            sleep(140);

        } while (System.nanoTime() < deadline);

        return null;
    }

    /**
     * A Good fund heading is accepted only in the central usable card area.
     * This prevents a pre-rendered heading near the bottom navigation area from
     * being treated as the next card before the single full-card swipe happens.
     */
    private boolean isGoodFundHeadingInCardBand(
            WebElement fund
    ) {
        if (fund == null) {
            return false;
        }

        try {
            Rectangle rect = fund.getRect();
            Dimension screen = driver.manage().window().getSize();

            int top = rect.getY();
            int bottom = rect.getY() + rect.getHeight();

            int safeTop = (int) (screen.getHeight() * 0.18);
            int safeBottom = (int) (screen.getHeight() * 0.80);

            return rect.getWidth() > 0
                    && rect.getHeight() > 0
                    && top >= safeTop
                    && bottom <= safeBottom;

        } catch (Exception e) {
            return false;
        }
    }

    private WebElement findClosestVisibleBelowOnScreen(
            WebElement anchor,
            By locator,
            double maxDistanceAsScreenFraction
    ) {
        if (anchor == null) {
            return null;
        }

        int anchorY;
        int maxDistance;

        try {
            anchorY = anchor.getRect().getY();
            maxDistance =
                    (int) (
                            driver.manage()
                                    .window()
                                    .getSize()
                                    .getHeight()
                                    * maxDistanceAsScreenFraction
                    );
        } catch (Exception e) {
            return null;
        }

        WebElement closest = null;
        int closestY = Integer.MAX_VALUE;

        try {
            List<WebElement> candidates =
                    driver.findElements(locator);

            for (WebElement candidate : candidates) {
                try {
                    if (candidate == null
                            || !candidate.isDisplayed()
                            || !isElementInsideCurrentViewport(candidate)) {
                        continue;
                    }

                    Rectangle rect = candidate.getRect();
                    int candidateY = rect.getY();
                    int distance = candidateY - anchorY;

                    if (rect.getWidth() <= 0
                            || rect.getHeight() <= 0
                            || distance <= 0
                            || distance > maxDistance) {
                        continue;
                    }

                    if (candidateY < closestY) {
                        closest = candidate;
                        closestY = candidateY;
                    }

                } catch (Exception ignored) {
                    // Flutter may replace a semantics node while the viewport is read.
                }
            }

        } catch (Exception e) {
            diagnostic(
                    "Good card anchored lookup failed: " + safeMessage(e)
            );
        }

        return closest;
    }

    private String normalizeUiText(
            String text
    ) {
        if (text == null) {
            return "";
        }

        return text
                .replace('\u00A0', ' ')
                .replace('\u2019', '\'')
                .replace('\u2018', '\'')
                .replaceAll("\\s+", " ")
                .trim();
    }

    private boolean matchesPrdTemplateWithDynamicMonthYear(
            String actualAdvice,
            String prdTemplate
    ) {
        String actual = normalizeUiText(actualAdvice);
        String template = normalizeUiText(prdTemplate);

        String placeholder = "[Month Year]";
        int placeholderIndex = template.indexOf(placeholder);

        if (placeholderIndex < 0) {
            return template.equals(actual);
        }

        String before = template.substring(0, placeholderIndex);
        String after = template.substring(placeholderIndex + placeholder.length());

        String regex =
                "^"
                        + Pattern.quote(before)
                        + MONTH_YEAR_REGEX
                        + Pattern.quote(after)
                        + "$";

        return actual.matches(regex);
    }

    public static final class RegressionSummary {
        private final String bucket;
        private final int mappedCards;
        private final int validatedCards;
        private final int matchedCards;
        private final int prdMismatchCount;
        private final int automationFailureCount;

        public RegressionSummary(
                String bucket,
                int mappedCards,
                int validatedCards,
                int matchedCards,
                int prdMismatchCount,
                int automationFailureCount
        ) {
            this.bucket = bucket;
            this.mappedCards = mappedCards;
            this.validatedCards = validatedCards;
            this.matchedCards = matchedCards;
            this.prdMismatchCount = prdMismatchCount;
            this.automationFailureCount = automationFailureCount;
        }

        public String getBucket() {
            return bucket;
        }

        public int getMappedCards() {
            return mappedCards;
        }

        public int getValidatedCards() {
            return validatedCards;
        }

        public int getMatchedCards() {
            return matchedCards;
        }

        public int getPrdMismatchCount() {
            return prdMismatchCount;
        }

        public int getAutomationFailureCount() {
            return automationFailureCount;
        }

        public boolean hasPrdMismatches() {
            return prdMismatchCount > 0;
        }

        public boolean hasAutomationFailures() {
            return automationFailureCount > 0;
        }
    }

    private enum AdviceValidationKind {
        MATCH,
        PRD_MISMATCH,
        AUTOMATION_FAILURE
    }

    private static final class AdviceValidationResult {
        private final AdviceValidationKind kind;
        private final String detail;

        private AdviceValidationResult(
                AdviceValidationKind kind,
                String detail
        ) {
            this.kind = kind;
            this.detail = detail == null ? "" : detail;
        }

        private static AdviceValidationResult match() {
            return new AdviceValidationResult(AdviceValidationKind.MATCH, "");
        }

        private static AdviceValidationResult prdMismatch(String detail) {
            return new AdviceValidationResult(AdviceValidationKind.PRD_MISMATCH, detail);
        }

        private static AdviceValidationResult automationFailure(String detail) {
            return new AdviceValidationResult(AdviceValidationKind.AUTOMATION_FAILURE, detail);
        }
    }

    /**
     * Adds only genuine product/PRD mismatches to the PRD list. Locator, positioning,
     * scrolling and semantics failures are tracked independently as automation
     * validation failures and never inflate the product mismatch count.
     *
     * @return 1 when the card was actually compared against PRD text; 0 otherwise.
     */
    private int recordAdviceValidationResult(
            String bucket,
            AdviceValidationResult result,
            List<String> prdMismatches,
            List<String> automationFailures
    ) {
        if (result == null) {
            String detail = bucket + " validator returned null result";
            automationFailures.add(detail);
            ReportLogger.step("[AUTOMATION] " + bucket + " | " + detail);
            return 0;
        }

        if (result.kind == AdviceValidationKind.PRD_MISMATCH) {
            prdMismatches.add(result.detail);
            return 1;
        }

        if (result.kind == AdviceValidationKind.AUTOMATION_FAILURE) {
            automationFailures.add(result.detail);
            return 0;
        }

        return 1;
    }

    private void assertNoAutomationFailures(
            String scope,
            List<String> automationFailures
    ) {
        if (automationFailures == null || automationFailures.isEmpty()) {
            return;
        }

        throw new AssertionError(
                scope
                        + " automation validation failed"
                        + " | count=" + automationFailures.size()
                        + " | details=" + String.join(" || ", automationFailures)
        );
    }

    private static final class GoodAdviceExpectation {
        private final String manualCaseId;
        private final String fundName;
        private final String fundCategory;
        private final String opinion;
        private final String plan;
        private final String prdTemplate;
        private final String expectedActualAdvice;

        private GoodAdviceExpectation(
                String manualCaseId,
                String fundName,
                String fundCategory,
                String opinion,
                String plan,
                String prdTemplate,
                String expectedActualAdvice
        ) {
            this.manualCaseId = manualCaseId;
            this.fundName = fundName;
            this.fundCategory = fundCategory;
            this.opinion = opinion;
            this.plan = plan;
            this.prdTemplate = prdTemplate;
            this.expectedActualAdvice = expectedActualAdvice;
        }
    }

    // =========================================================
    // NEW FUND BUCKET - OUR ADVICE PRD REGRESSION
    // =========================================================

    /**
     * Validates only the Abakkus Large & Mid Cap Direct/Regular pair against the
     * PRD New Fund advice rule.
     *
     * Funds before/between Abakkus are ignored by validation. This is intentional:
     * an NFO / not-yet-framework-covered fund can legitimately show "Too early to
     * evaluate" without a projected opinion date, so PAFA_003 must not mark those
     * cards as PRD failures.
     *
     * Traversal is target-driven: NEW_FUND_ADVICE_EXPECTATIONS defines the Abakkus
     * targets that are both located and asserted. Preceding/intermediate cards are
     * crossed silently and do not participate in pass/fail.
     */
    public RegressionSummary verifyNewFundAdviceAgainstPrdAcrossFundsForPortfolioAnalysisFa() {
        verifyNewFundBucketReady("before New Fund advice regression");

        newFundTraversalCheckpointIndex = -1;
        List<String> prdMismatches = new ArrayList<>();
        List<String> automationFailures = new ArrayList<>();
        int validatedAgainstPrd = 0;
        int prdMatches = 0;

        for (NewFundAdviceExpectation expectation : NEW_FUND_ADVICE_EXPECTATIONS) {
            moveNewFundTraversalToMappedFund(expectation.fundName);

            AdviceValidationResult result =
                    validateNewFundAdviceForCurrentFund(expectation);

            validatedAgainstPrd += recordAdviceValidationResult(
                    "NEW FUND", result, prdMismatches, automationFailures);

            if (result != null && result.kind == AdviceValidationKind.MATCH) {
                prdMatches++;
            }
        }

        RegressionSummary summary = new RegressionSummary(
                "New Fund",
                NEW_FUND_ADVICE_EXPECTATIONS.length,
                validatedAgainstPrd,
                prdMatches,
                prdMismatches.size(),
                automationFailures.size()
        );

        assertNoAutomationFailures("New Fund Our Advice", automationFailures);
        return summary;
    }


    private AdviceValidationResult validateNewFundAdviceForCurrentFund(
            NewFundAdviceExpectation expectation
    ) {
        WebElement fund = positionNewFundMappedFundForAdvice(
                expectation.fundName,
                AppiumBy.accessibilityId(expectation.fundName)
        );
        if (fund == null) {
            String detail = "fund not positioned | " + expectation.fundName;
            ReportLogger.step("[AUTOMATION] New Fund | " + detail);
            return AdviceValidationResult.automationFailure(detail);
        }

        WebElement adviceLabel = findClosestVisibleBelowOnScreen(fund, ourAdviceLabel, 0.20);
        if (adviceLabel == null) {
            String detail = "Our Advice missing | " + expectation.fundName + " | " + expectation.plan;
            ReportLogger.step("[AUTOMATION] New Fund | " + detail);
            return AdviceValidationResult.automationFailure(detail);
        }

        WebElement adviceText = findClosestVisibleBelowOnScreen(
                adviceLabel, newFundAdviceText, 0.08);
        if (adviceText == null) {
            String detail = "advice text missing | " + expectation.fundName + " | " + expectation.plan;
            ReportLogger.step("[AUTOMATION] New Fund | " + detail);
            return AdviceValidationResult.automationFailure(detail);
        }

        String actualAdvice = normalizeUiText(getReadableText(adviceText));
        boolean matchesPrd = actualAdvice.matches(expectation.expectedRegex);

        if (!matchesPrd) {
            String detail = expectation.reference
                    + " | " + expectation.fundName
                    + " | " + expectation.plan
                    + " | expected=\"" + expectation.prdTemplate + "\""
                    + " | actual=\"" + actualAdvice + "\"";
            ReportLogger.fail("[MISMATCH] New Fund | " + detail);
            return AdviceValidationResult.prdMismatch(detail);
        }

        ReportLogger.pass(
                "[MATCH] New Fund | " + expectation.reference
                        + " | " + expectation.fundName
                        + " | " + expectation.plan
                        + " | \"" + actualAdvice + "\""
        );
        return AdviceValidationResult.match();
    }

    /**
     * New Fund traversal is target-driven. Abakkus Direct/Regular are the only
     * validation targets; every preceding/intermediate New Fund card is ignored.
     */
    private void moveNewFundTraversalToMappedFund(String expectedMappedFund) {
        int targetIndex = indexOfNewFundMappedFund(expectedMappedFund);
        if (targetIndex < 0) {
            throw new AssertionError(
                    "Mapped New Fund target is missing from the validation target map"
                            + " | fund=" + expectedMappedFund
            );
        }

        if (newFundTraversalCheckpointIndex > targetIndex) {
            throw new AssertionError(
                    "New Fund target-driven traversal cannot move backwards"
                            + " | requested=" + expectedMappedFund
            );
        }

        By targetLocator = AppiumBy.accessibilityId(expectedMappedFund);
        WebElement target = waitForNewFundInCardBand(targetLocator, 500);
        int forwardSwipes = 0;
        int reverseSwipes = 0;

        while (target == null && forwardSwipes < NEW_FUND_MAX_TARGET_SEARCH_SWIPES) {
            int laterMapped = findLaterNewFundMappedTargetInCardBand(targetIndex);
            if (laterMapped > targetIndex) {
                NewFundReverseRecovery recovery = recoverExpectedNewFundMappedTarget(
                        targetLocator, expectedMappedFund);
                reverseSwipes += recovery.swipesUsed;
                target = recovery.fund;
                if (target != null) {
                    break;
                }
            }

            forwardSwipes++;
            newFundSmallFixedSwipeUp();
            sleep(NEW_FUND_TARGET_SEARCH_SETTLE_MS);
            target = waitForNewFundInCardBand(targetLocator, 800);
        }

        if (target == null) {
            throw new AssertionError(
                    "Unable to locate mapped New Fund target after bounded target-driven scrolling"
                            + " | target=" + expectedMappedFund
                            + " | forwardSwipesUsed=" + forwardSwipes
                            + " | reverseRecoverySwipesUsed=" + reverseSwipes
                            + " | intermediateUnknownFunds=ignored"
                            + " | fling=false"
            );
        }

        newFundTraversalCheckpointIndex = targetIndex;
        recordNewFundMappedCheckpoint(
                targetIndex,
                target,
                "captured mapped New Fund target"
                        + " | forwardSwipes=" + forwardSwipes
                        + " | reverseRecoverySwipes=" + reverseSwipes
        );
    }

    private int indexOfNewFundMappedFund(String fundName) {
        if (fundName == null) {
            return -1;
        }
        for (int index = 0; index < NEW_FUND_MAPPED_FUNDS_IN_ORDER.length; index++) {
            if (fundName.equals(NEW_FUND_MAPPED_FUNDS_IN_ORDER[index])) {
                return index;
            }
        }
        return -1;
    }

    private int findLaterNewFundMappedTargetInCardBand(int expectedIndex) {
        for (int index = expectedIndex + 1;
             index < NEW_FUND_MAPPED_FUNDS_IN_ORDER.length;
             index++) {
            WebElement later = findVisibleElement(
                    AppiumBy.accessibilityId(NEW_FUND_MAPPED_FUNDS_IN_ORDER[index]));
            if (later != null && isNewFundHeadingInCardBand(later)) {
                return index;
            }
        }
        return -1;
    }

    private NewFundReverseRecovery recoverExpectedNewFundMappedTarget(
            By expectedLocator,
            String expectedFundName
    ) {
        WebElement expected = waitForNewFundInCardBand(expectedLocator, 300);
        if (expected != null) {
            return new NewFundReverseRecovery(expected, 0);
        }

        for (int reverse = 1; reverse <= NEW_FUND_MAX_REVERSE_RECOVERY_SWIPES; reverse++) {
            newFundFineSwipeDown();
            sleep(NEW_FUND_TARGET_SEARCH_SETTLE_MS);
            expected = waitForNewFundInCardBand(expectedLocator, 650);
            if (expected != null) {
                diagnostic(
                        "New Fund mapped target reverse-recovered"
                                + " | fund=" + expectedFundName
                                + " | reverseSwipe=" + reverse
                );
                return new NewFundReverseRecovery(expected, reverse);
            }
        }
        return new NewFundReverseRecovery(null, NEW_FUND_MAX_REVERSE_RECOVERY_SWIPES);
    }

    private WebElement positionNewFundMappedFundForAdvice(
            String fundName,
            By fundLocator
    ) {
        for (int attempt = 0; attempt <= NEW_FUND_MAX_ADVICE_POSITION_SWIPES; attempt++) {
            WebElement fund = waitForNewFundInCardBand(fundLocator, 400);
            if (fund != null
                    && findClosestVisibleBelowOnScreen(fund, ourAdviceLabel, 0.24) != null) {
                return fund;
            }

            if (fund == null) {
                newFundFineSwipeDown();
                sleep(NEW_FUND_TARGET_SEARCH_SETTLE_MS);
                continue;
            }

            if (attempt == NEW_FUND_MAX_ADVICE_POSITION_SWIPES) {
                break;
            }

            newFundFineSwipeUp();
            sleep(NEW_FUND_TARGET_SEARCH_SETTLE_MS);
        }

        WebElement finalFund = waitForNewFundInCardBand(fundLocator, 800);
        if (finalFund != null
                && findClosestVisibleBelowOnScreen(finalFund, ourAdviceLabel, 0.24) != null) {
            return finalFund;
        }

        diagnostic("New Fund mapped target advice block could not be positioned | fund=" + fundName);
        return null;
    }

    private void recordNewFundMappedCheckpoint(
            int mappedIndex,
            WebElement fundElement,
            String reason
    ) {
        if (fundElement == null) {
            throw new AssertionError(
                    "Cannot record null New Fund mapped checkpoint | index=" + mappedIndex
            );
        }

        String fundName = NEW_FUND_MAPPED_FUNDS_IN_ORDER[mappedIndex];
        diagnostic(
                "New Fund mapped checkpoint captured"
                        + " | " + (mappedIndex + 1)
                        + "/" + NEW_FUND_MAPPED_FUNDS_IN_ORDER.length
                        + " | fund=" + fundName
                        + " | bounds=" + rectangleToString(fundElement.getRect())
                        + " | reason=" + reason
        );
        sleep(NEW_FUND_CARD_CHECKPOINT_SETTLE_MS);
    }

    private static final class NewFundReverseRecovery {
        private final WebElement fund;
        private final int swipesUsed;

        private NewFundReverseRecovery(WebElement fund, int swipesUsed) {
            this.fund = fund;
            this.swipesUsed = swipesUsed;
        }
    }

    private void verifyNewFundBucketReady(
            String context
    ) {
        if (!isVisibleWithin(newFundBucketTab, 3)) {
            throw new AssertionError(
                    "New Fund tab strip is not visible"
                            + " | context=" + context
            );
        }

        if (!isVisibleWithin(newFundReviewHeader, 3)) {
            throw new AssertionError(
                    "New funds heading is not visible"
                            + " | context=" + context
            );
        }
    }

    private WebElement waitForNewFundInCardBand(
            By fundLocator,
            long timeoutMillis
    ) {
        long deadline =
                System.nanoTime()
                        + Duration.ofMillis(
                                Math.max(1L, timeoutMillis)
                        ).toNanos();

        do {
            WebElement fund =
                    findVisibleElement(fundLocator);

            if (fund != null
                    && isNewFundHeadingInCardBand(fund)) {
                return fund;
            }

            sleep(100);
        } while (System.nanoTime() < deadline);

        return null;
    }

    private boolean isNewFundHeadingInCardBand(
            WebElement fund
    ) {
        if (fund == null) {
            return false;
        }

        try {
            Rectangle rect = fund.getRect();
            Dimension screen = driver.manage().window().getSize();

            int top = rect.getY();
            int bottom = rect.getY() + rect.getHeight();
            int safeTop = (int) (screen.getHeight() * 0.18);
            int safeBottom = (int) (screen.getHeight() * 0.82);

            return rect.getWidth() > 0
                    && rect.getHeight() > 0
                    && top >= safeTop
                    && bottom <= safeBottom;
        } catch (Exception e) {
            return false;
        }
    }

    private static final class NewFundAdviceExpectation {
        private final String reference;
        private final String fundName;
        private final String bucket;
        private final String plan;
        private final String prdTemplate;
        private final String expectedRegex;
        private final String coverageSource;

        private NewFundAdviceExpectation(
                String reference,
                String fundName,
                String bucket,
                String plan,
                String prdTemplate,
                String expectedRegex,
                String coverageSource
        ) {
            this.reference = reference;
            this.fundName = fundName;
            this.bucket = bucket;
            this.plan = plan;
            this.prdTemplate = prdTemplate;
            this.expectedRegex = expectedRegex;
            this.coverageSource = coverageSource;
        }
    }

    // =========================================================
    // STEADY BUCKET - OUR ADVICE PRD REGRESSION
    // =========================================================

    /**
     * Validates the yellow-highlighted manual QA mapping TC-03 through TC-08.
     *
     * Important behavior:
     * - only Steady is opened for this test;
     * - target-driven small W3C scrolling searches only for mapped fund locators;
     * - unrelated/new cards between mapped funds are skipped without requiring locators;
     * - every mapped Direct/Regular fund is logged with manual case, category,
     *   opinion, Active/Passive branch, plan, PRD template and actual UI advice;
     * - mismatches are collected and asserted only after all mapped funds are read,
     *   so TC-05 and TC-08 evidence is not hidden by the first failure.
     */
    public RegressionSummary verifySteadyAdviceAgainstPrdAcrossFundsForPortfolioAnalysisFa() {
        verifySteadyBucketReady("before Steady advice regression");

        steadyTraversalCheckpointIndex = -1;
        steadyControlledSwipeCount = 0;
        steadyReverseRecoverySwipeCount = 0;

        initializeSteadyTraversalAtFirstMappedTarget();

        List<String> prdMismatches = new ArrayList<>();
        List<String> automationFailures = new ArrayList<>();
        int validatedAgainstPrd = 0;
        int prdMatches = 0;

        for (SteadyAdviceExpectation expectation : STEADY_ADVICE_EXPECTATIONS) {
            moveSteadyTraversalToMappedFund(expectation.fundName);

            AdviceValidationResult result = validateSteadyAdviceForCurrentMappedFund(expectation);
            validatedAgainstPrd += recordAdviceValidationResult(
                    "STEADY", result, prdMismatches, automationFailures);
            if (result != null && result.kind == AdviceValidationKind.MATCH) {
                prdMatches++;
            }
        }

        RegressionSummary summary = new RegressionSummary(
                "Steady", STEADY_ADVICE_EXPECTATIONS.length, validatedAgainstPrd, prdMatches,
                prdMismatches.size(), automationFailures.size());

        // Product copy mismatches are findings. Only an actual automation-validation
        // failure should fail TestNG / trigger a failure screenshot.
        assertNoAutomationFailures("Steady Our Advice", automationFailures);
        return summary;
    }

    private boolean isLastExpectationForManualCase(
            SteadyAdviceExpectation currentExpectation
    ) {
        int currentIndex = -1;

        for (int i = 0; i < STEADY_ADVICE_EXPECTATIONS.length; i++) {
            if (STEADY_ADVICE_EXPECTATIONS[i] == currentExpectation) {
                currentIndex = i;
                break;
            }
        }

        return currentIndex < 0
                || currentIndex + 1 == STEADY_ADVICE_EXPECTATIONS.length
                || !STEADY_ADVICE_EXPECTATIONS[currentIndex + 1]
                .manualCaseId
                .equals(currentExpectation.manualCaseId);
    }

    private AdviceValidationResult validateSteadyAdviceForCurrentMappedFund(
            SteadyAdviceExpectation expectation
    ) {
        WebElement fund = positionSteadyMappedFundForAdvice(
                expectation.fundName, AppiumBy.accessibilityId(expectation.fundName));

        if (fund == null) {
            String detail = expectation.manualCaseId + " | " + expectation.fundName
                    + " | could not position card for advice";
            ReportLogger.step("[AUTOMATION] Steady | " + detail);
            return AdviceValidationResult.automationFailure(detail);
        }

        WebElement adviceLabel = findClosestVisibleBelowOnScreen(
                fund, ourAdviceLabel, 0.70);
        if (adviceLabel == null) {
            String detail = expectation.manualCaseId + " | " + expectation.fundName
                    + " | Our Advice missing";
            ReportLogger.step("[AUTOMATION] Steady | " + detail);
            return AdviceValidationResult.automationFailure(detail);
        }

        WebElement adviceText = findClosestVisibleBelowOnScreen(
                adviceLabel, AppiumBy.accessibilityId(expectation.manualSheetObservedAdvice), 0.14);
        if (adviceText == null) {
            adviceText = findClosestSteadyAdviceTextBelow(adviceLabel);
        }
        if (adviceText == null) {
            String detail = expectation.manualCaseId + " | " + expectation.fundName
                    + " | advice text missing";
            ReportLogger.step("[AUTOMATION] Steady | " + detail);
            return AdviceValidationResult.automationFailure(detail);
        }

        String actualAdvice = normalizeUiText(getReadableText(adviceText));
        boolean matchesPrd = actualAdvice.matches(expectation.expectedRegex);

        if (!matchesPrd) {
            String branch = "";
            if (expectation.prdTemplate.startsWith("Not tracking its index well so far")
                    && actualAdvice.startsWith("Weak early performance")) {
                branch = " | issue=Passive Poor Start is showing Active Poor Start wording";
            }

            String detail = expectation.manualCaseId
                    + " | " + expectation.fundName
                    + " | " + expectation.strategy
                    + " | " + expectation.plan
                    + " | expected=\"" + expectation.prdTemplate + "\""
                    + " | actual=\"" + actualAdvice + "\""
                    + branch;
            ReportLogger.fail("[MISMATCH] Steady | " + detail);
            return AdviceValidationResult.prdMismatch(detail);
        }

        ReportLogger.pass(
                "[MATCH] Steady | " + expectation.manualCaseId
                        + " | " + expectation.fundName
                        + " | " + expectation.plan
                        + " | \"" + actualAdvice + "\""
        );
        return AdviceValidationResult.match();
    }

    private WebElement findClosestSteadyAdviceTextBelow(
            WebElement adviceLabel
    ) {
        WebElement closest = null;
        int closestY = Integer.MAX_VALUE;

        By[] adviceLocators = {
                steadyPerformingOkayAdviceText,
                steadyWeakEarlyAdviceText,
                steadyNotTrackingAdviceText
        };

        for (By locator : adviceLocators) {
            WebElement candidate =
                    findClosestVisibleBelowOnScreen(
                            adviceLabel,
                            locator,
                            0.12
                    );

            if (candidate == null) {
                continue;
            }

            try {
                int candidateY = candidate.getRect().getY();

                if (candidateY < closestY) {
                    closest = candidate;
                    closestY = candidateY;
                }
            } catch (Exception ignored) {
                // Try the remaining live Flutter semantics nodes.
            }
        }

        return closest;
    }

    /**
     * Moves only between mapped Steady validation targets.
     *
     * No assumption is made about how many unrelated funds sit between two mapped
     * targets. Unknown cards need no locator: they are simply crossed by small W3C
     * swipes while the exact next mapped accessibility id is checked after each move.
     */
    private void initializeSteadyTraversalAtFirstMappedTarget() {
        String firstMappedFund = STEADY_MAPPED_FUNDS_IN_ORDER[0];
        By firstMappedLocator = AppiumBy.accessibilityId(firstMappedFund);

        WebElement firstFund = waitForSteadyFundInCaptureBand(firstMappedLocator, 700);
        int forwardSwipes = 0;

        while (firstFund == null && forwardSwipes < STEADY_MAX_TARGET_SEARCH_SWIPES) {
            forwardSwipes++;
            steadySmallFixedSwipeUp();
            steadyControlledSwipeCount++;
            sleep(STEADY_SMALL_SCROLL_SETTLE_MS);
            firstFund = waitForSteadyFundInCaptureBand(firstMappedLocator, 800);
        }

        if (firstFund == null) {
            throw new AssertionError(
                    "STEADY AUTOMATION SETUP FAILURE | first mapped fund not found after bounded target-driven scrolling"
                            + " | target=" + firstMappedFund
                            + " | forwardSwipesUsed=" + forwardSwipes
                            + " | intermediateUnknownFunds=ignored"
            );
        }

        steadyTraversalCheckpointIndex = 0;
        recordSteadyMappedCheckpoint(
                0,
                firstFund,
                "initial mapped Steady checkpoint | forwardSwipes=" + forwardSwipes
        );
    }

    private void moveSteadyTraversalToMappedFund(
            String expectedMappedFund
    ) {
        int targetMappedIndex = indexOfSteadyMappedFund(expectedMappedFund);

        if (targetMappedIndex < 0) {
            throw new AssertionError(
                    "Mapped Steady fund is missing from the validation locator map"
                            + " | fund=" + expectedMappedFund
            );
        }

        if (steadyTraversalCheckpointIndex < 0) {
            throw new AssertionError(
                    "Steady mapped traversal was not initialized before validation"
                            + " | fund=" + expectedMappedFund
            );
        }

        if (targetMappedIndex < steadyTraversalCheckpointIndex) {
            throw new AssertionError(
                    "Steady mapped traversal cannot move backwards"
                            + " | current="
                            + STEADY_MAPPED_FUNDS_IN_ORDER[steadyTraversalCheckpointIndex]
                            + " | requested=" + expectedMappedFund
            );
        }

        while (steadyTraversalCheckpointIndex < targetMappedIndex) {
            moveToNextMappedSteadyTarget();
        }

        String currentMappedFund =
                STEADY_MAPPED_FUNDS_IN_ORDER[steadyTraversalCheckpointIndex];

        if (!currentMappedFund.equals(expectedMappedFund)) {
            throw new AssertionError(
                    "Steady mapped checkpoint does not match requested validation fund"
                            + " | checkpoint=" + currentMappedFund
                            + " | requested=" + expectedMappedFund
            );
        }
    }

    private int indexOfSteadyMappedFund(
            String fundName
    ) {
        if (fundName == null) {
            return -1;
        }

        for (int index = 0; index < STEADY_MAPPED_FUNDS_IN_ORDER.length; index++) {
            if (fundName.equals(STEADY_MAPPED_FUNDS_IN_ORDER[index])) {
                return index;
            }
        }

        return -1;
    }

    /**
     * Searches directly for the next mapped Steady target. Any newly inserted or
     * otherwise unrelated fund between the current and next mapped target is ignored
     * automatically and does not require an accessibility locator in this class.
     */
    private void moveToNextMappedSteadyTarget() {
        if (steadyTraversalCheckpointIndex < 0) {
            throw new AssertionError(
                    "Steady mapped traversal was not initialized before requesting next target"
            );
        }

        int nextMappedIndex = steadyTraversalCheckpointIndex + 1;

        if (nextMappedIndex >= STEADY_MAPPED_FUNDS_IN_ORDER.length) {
            throw new AssertionError(
                    "Steady mapped traversal requested a target beyond the validation map"
                            + " | currentIndex=" + steadyTraversalCheckpointIndex
            );
        }

        String previousMappedFund =
                STEADY_MAPPED_FUNDS_IN_ORDER[steadyTraversalCheckpointIndex];
        String nextMappedFund =
                STEADY_MAPPED_FUNDS_IN_ORDER[nextMappedIndex];
        By nextMappedLocator = AppiumBy.accessibilityId(nextMappedFund);

        diagnostic(
                "Searching directly for next mapped Steady fund"
                        + " | fromMapped=" + previousMappedFund
                        + " | targetMapped=" + nextMappedFund
                        + " | mappedCheckpoint=" + (nextMappedIndex + 1)
                        + "/" + STEADY_MAPPED_FUNDS_IN_ORDER.length
                        + " | intermediateUnknownFunds=ignored"
                        + " | maxSmallW3CSwipes=" + STEADY_MAX_TARGET_SEARCH_SWIPES
                        + " | fling=false"
        );

        // Advice positioning on the previous mapped card may already expose the next
        // mapped target. Capture it first so no unnecessary swipe is performed.
        WebElement target = waitForSteadyFundInCaptureBand(nextMappedLocator, 500);

        int forwardSwipesUsed = 0;
        int reverseSwipesUsed = 0;

        while (target == null && forwardSwipesUsed < STEADY_MAX_TARGET_SEARCH_SWIPES) {
            int laterMappedIndex =
                    findLaterSteadyMappedTargetInCaptureBand(nextMappedIndex);

            if (laterMappedIndex > nextMappedIndex) {
                ReverseSteadyRecovery recovery =
                        recoverExpectedSteadyMappedTarget(
                                nextMappedLocator,
                                nextMappedFund,
                                previousMappedFund
                        );

                reverseSwipesUsed += recovery.swipesUsed;
                target = recovery.fund;

                if (target != null) {
                    break;
                }
            }

            forwardSwipesUsed++;
            steadySmallFixedSwipeUp();
            steadyControlledSwipeCount++;
            sleep(STEADY_SMALL_SCROLL_SETTLE_MS);

            target = waitForSteadyFundInCaptureBand(nextMappedLocator, 900);

            if (target == null) {
                laterMappedIndex =
                        findLaterSteadyMappedTargetInCaptureBand(nextMappedIndex);

                if (laterMappedIndex > nextMappedIndex) {
                    ReverseSteadyRecovery recovery =
                            recoverExpectedSteadyMappedTarget(
                                    nextMappedLocator,
                                    nextMappedFund,
                                    previousMappedFund
                            );

                    reverseSwipesUsed += recovery.swipesUsed;
                    target = recovery.fund;
                }
            }
        }

        if (target == null) {
            int laterMappedIndex =
                    findLaterSteadyMappedTargetInCaptureBand(nextMappedIndex);

            if (laterMappedIndex > nextMappedIndex) {
                ReverseSteadyRecovery recovery =
                        recoverExpectedSteadyMappedTarget(
                                nextMappedLocator,
                                nextMappedFund,
                                previousMappedFund
                        );

                reverseSwipesUsed += recovery.swipesUsed;
                target = recovery.fund;
            }
        }

        if (target == null) {
            throw new AssertionError(
                    "Unable to locate the next mapped Steady fund after bounded target-driven scrolling"
                            + " | fromMapped=" + previousMappedFund
                            + " | targetMapped=" + nextMappedFund
                            + " | forwardSwipesUsed=" + forwardSwipesUsed
                            + " | reverseRecoverySwipesUsed=" + reverseSwipesUsed
                            + " | maxForwardSwipes=" + STEADY_MAX_TARGET_SEARCH_SWIPES
                            + " | intermediateUnknownFunds=ignored"
                            + " | noLargeJump=true"
                            + " | noFling=true"
            );
        }

        steadyTraversalCheckpointIndex = nextMappedIndex;

        recordSteadyMappedCheckpoint(
                nextMappedIndex,
                target,
                "captured next mapped fund in safe corridor"
                        + " | forwardSwipes=" + forwardSwipesUsed
                        + " | reverseRecoverySwipes=" + reverseSwipesUsed
        );
    }

    /**
     * Overshoot detection considers only later MAPPED validation funds. Unknown
     * intermediate cards are intentionally invisible to this algorithm.
     */
    private int findLaterSteadyMappedTargetInCaptureBand(
            int expectedMappedIndex
    ) {
        for (int index = expectedMappedIndex + 1;
             index < STEADY_MAPPED_FUNDS_IN_ORDER.length;
             index++) {

            WebElement laterMappedFund =
                    findVisibleElement(
                            AppiumBy.accessibilityId(
                                    STEADY_MAPPED_FUNDS_IN_ORDER[index]
                            )
                    );

            if (laterMappedFund != null
                    && isSteadyFundHeadingInCaptureBand(laterMappedFund)) {
                return index;
            }
        }

        return -1;
    }

    /**
     * Small local reverse recovery used only when a later mapped target proves that
     * the expected mapped target was passed. It does not depend on intermediate cards.
     */
    private ReverseSteadyRecovery recoverExpectedSteadyMappedTarget(
            By expectedLocator,
            String expectedMappedFund,
            String previousMappedFund
    ) {
        WebElement expected = waitForSteadyFundInCaptureBand(expectedLocator, 350);

        if (expected != null) {
            return new ReverseSteadyRecovery(expected, 0);
        }

        for (int reverseSwipe = 1;
             reverseSwipe <= STEADY_MAX_REVERSE_RECOVERY_SWIPES;
             reverseSwipe++) {

            diagnostic(
                    "Steady mapped-target reverse correction"
                            + " | fromMapped=" + previousMappedFund
                            + " | recoveringMapped=" + expectedMappedFund
                            + " | reverseSwipe=" + reverseSwipe
                            + "/" + STEADY_MAX_REVERSE_RECOVERY_SWIPES
            );

            steadyFineSwipeDown();
            steadyReverseRecoverySwipeCount++;
            sleep(STEADY_SMALL_SCROLL_SETTLE_MS);

            expected = waitForSteadyFundInCaptureBand(expectedLocator, 800);

            if (expected != null) {
                return new ReverseSteadyRecovery(expected, reverseSwipe);
            }
        }

        return new ReverseSteadyRecovery(null, STEADY_MAX_REVERSE_RECOVERY_SWIPES);
    }

    private static final class ReverseSteadyRecovery {
        private final WebElement fund;
        private final int swipesUsed;

        private ReverseSteadyRecovery(
                WebElement fund,
                int swipesUsed
        ) {
            this.fund = fund;
            this.swipesUsed = swipesUsed;
        }
    }

    private void recordSteadyMappedCheckpoint(
            int mappedIndex,
            WebElement fundElement,
            String reason
    ) {
        if (fundElement == null) {
            throw new AssertionError(
                    "Cannot record null Steady mapped checkpoint"
                            + " | index=" + mappedIndex
            );
        }

        String fundName = STEADY_MAPPED_FUNDS_IN_ORDER[mappedIndex];

        diagnostic(
                "Steady mapped fund checkpoint captured"
                        + " | " + (mappedIndex + 1)
                        + "/" + STEADY_MAPPED_FUNDS_IN_ORDER.length
                        + " | fund=" + fundName
                        + " | bounds=" + rectangleToString(fundElement.getRect())
                        + " | controlledSmallSwipesSoFar=" + steadyControlledSwipeCount
                        + " | reason=" + reason
        );

        sleep(STEADY_CARD_CHECKPOINT_SETTLE_MS);
    }

    private void verifySteadyBucketReady(
            String context
    ) {
        if (!isVisibleWithin(steadyBucketTab, 3)) {
            throw new AssertionError(
                    "Steady tab strip is not visible"
                            + " | context=" + context
            );
        }

        if (!isVisibleWithin(steadyReviewHeader, 3)) {
            throw new AssertionError(
                    "Funds that are steady heading is not visible"
                            + " | context=" + context
            );
        }
    }

    /**
     * Broad capture lookup used only for traversal. A heading is considered captured
     * as soon as it is fully visible below the sticky Steady tab and above the
     * bottom system area. It does NOT need to be in the upper advice-reading band.
     */
    private WebElement waitForSteadyFundInCaptureBand(
            By fundLocator,
            long timeoutMillis
    ) {
        long deadline =
                System.nanoTime()
                        + Duration.ofMillis(
                                Math.max(1L, timeoutMillis)
                        ).toNanos();

        do {
            WebElement fund =
                    findVisibleElement(fundLocator);

            if (fund != null
                    && isSteadyFundHeadingInCaptureBand(fund)) {
                return fund;
            }

            sleep(120);
        } while (System.nanoTime() < deadline);

        return null;
    }

    private boolean isSteadyFundHeadingInCaptureBand(
            WebElement fund
    ) {
        if (fund == null) {
            return false;
        }

        try {
            Rectangle rect = fund.getRect();
            Dimension screen = driver.manage().window().getSize();

            int safeTop = getSteadyUsableTopY(screen);
            int safeBottom = (int) (screen.getHeight() * 0.90);
            int top = rect.getY();
            int bottom = rect.getY() + rect.getHeight();

            return rect.getWidth() > 0
                    && rect.getHeight() > 0
                    && top >= safeTop
                    && bottom <= safeBottom;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Positions one already-captured mapped fund high enough to expose that SAME
     * card's Our Advice block. This method never searches for the next fund.
     */
    private WebElement positionSteadyMappedFundForAdvice(
            String fundName,
            By fundLocator
    ) {
        int previousY = Integer.MIN_VALUE;

        for (int attempt = 0; attempt <= STEADY_MAX_ADVICE_POSITION_SWIPES; attempt++) {
            WebElement inAdviceBand = waitForSteadyFundInValidationBand(fundLocator, 450);
            if (inAdviceBand != null) {
                return inAdviceBand;
            }

            WebElement captured = waitForSteadyFundInCaptureBand(fundLocator, 350);
            if (captured == null) {
                diagnostic("Steady positioning reverse correction | fund=" + fundName);
                steadyFineSwipeDown();
                steadyReverseRecoverySwipeCount++;
                sleep(STEADY_SMALL_SCROLL_SETTLE_MS);
                continue;
            }

            Rectangle rect = captured.getRect();
            Dimension screen = driver.manage().window().getSize();
            int headingBottom = rect.getY() + rect.getHeight();
            int adviceBottom = getSteadyAdviceBandBottomY(screen);

            if (headingBottom <= adviceBottom) {
                return captured;
            }

            /*
             * End-of-list guard: Flutter may stop moving the final card even though
             * its Our Advice block is already fully readable. In that situation the
             * older strict Y-band check produced a false automation failure.
             */
            if (rect.getY() == previousY) {
                WebElement visibleAdvice = findClosestVisibleBelowOnScreen(
                        captured, ourAdviceLabel, 0.70);
                if (visibleAdvice != null) {
                    diagnostic("Steady end-of-list readable fallback | fund=" + fundName
                            + " | y=" + rect.getY());
                    return captured;
                }
            }
            previousY = rect.getY();

            if (attempt == STEADY_MAX_ADVICE_POSITION_SWIPES) {
                break;
            }

            steadyFineSwipeUp();
            steadyControlledSwipeCount++;
            sleep(STEADY_SMALL_SCROLL_SETTLE_MS);
        }

        WebElement finalCapture = waitForSteadyFundInCaptureBand(fundLocator, 1000);
        if (finalCapture != null
                && findClosestVisibleBelowOnScreen(finalCapture, ourAdviceLabel, 0.70) != null) {
            return finalCapture;
        }

        return waitForSteadyFundInValidationBand(fundLocator, 800);
    }

    private int getSteadyUsableTopY(
            Dimension screen
    ) {
        int safeTop = (int) (screen.getHeight() * 0.20);
        WebElement tab = findVisibleElement(steadyBucketTab);

        if (tab != null) {
            try {
                Rectangle tabRect = tab.getRect();
                safeTop = Math.max(
                        safeTop,
                        tabRect.getY() + tabRect.getHeight() + 24
                );
            } catch (Exception ignored) {
                // Percentage fallback above remains valid.
            }
        }

        return safeTop;
    }

    private int getSteadyAdviceBandBottomY(
            Dimension screen
    ) {
        return (int) (screen.getHeight() * 0.46);
    }

    private WebElement waitForSteadyFundInValidationBand(
            By fundLocator,
            long timeoutMillis
    ) {
        long deadline =
                System.nanoTime()
                        + Duration.ofMillis(
                                Math.max(1L, timeoutMillis)
                        ).toNanos();

        do {
            WebElement fund =
                    findVisibleElement(fundLocator);

            if (fund != null
                    && isSteadyFundHeadingInValidationBand(fund)) {
                return fund;
            }

            sleep(140);
        } while (System.nanoTime() < deadline);

        return null;
    }

    /**
     * Keep the target heading high enough that the same card's Rationale,
     * suitability and Our Advice can still be read below it. A lower heading is
     * NOT accepted as a checkpoint; the bounded small-scroll loop continues until
     * the exact heading reaches this band.
     */
    private boolean isSteadyFundHeadingInValidationBand(
            WebElement fund
    ) {
        if (fund == null) {
            return false;
        }

        try {
            Rectangle rect = fund.getRect();
            Dimension screen = driver.manage().window().getSize();

            int top = rect.getY();
            int bottom = rect.getY() + rect.getHeight();
            int safeTop = getSteadyUsableTopY(screen);
            int safeBottom = getSteadyAdviceBandBottomY(screen);

            return rect.getWidth() > 0
                    && rect.getHeight() > 0
                    && top >= safeTop
                    && bottom <= safeBottom;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Small fixed forward search increment only. No distance is derived from the
     * target element. With the current emulator this is about 12% of screen height.
     */
    private void steadySmallFixedSwipeUp() {
        Dimension size = driver.manage().window().getSize();

        int startX = (int) (size.getWidth() * 0.50);
        int startY = (int) (size.getHeight() * STEADY_SMALL_SCROLL_START_FRACTION);
        int endY = (int) (size.getHeight() * STEADY_SMALL_SCROLL_END_FRACTION);

        diagnostic(
                "Steady forward W3C geometry"
                        + " | startY=" + startY
                        + " | endY=" + endY
                        + " | screenHeight=" + size.getHeight()
                        + " | approximateContentShift=" + (startY - endY)
        );

        swipeByCoordinates(
                startX,
                startY,
                startX,
                endY,
                480
        );
    }

    private void steadyFineSwipeUp() {
        Dimension size = driver.manage().window().getSize();
        int x = (int) (size.getWidth() * 0.50);
        int startY = (int) (size.getHeight() * STEADY_FINE_SCROLL_UP_START_FRACTION);
        int endY = (int) (size.getHeight() * STEADY_FINE_SCROLL_UP_END_FRACTION);

        diagnostic(
                "Steady fine-up W3C geometry"
                        + " | startY=" + startY
                        + " | endY=" + endY
                        + " | approximateContentShift=" + (startY - endY)
        );

        swipeByCoordinates(x, startY, x, endY, 420);
    }

    private void steadyFineSwipeDown() {
        Dimension size = driver.manage().window().getSize();
        int x = (int) (size.getWidth() * 0.50);
        int startY = (int) (size.getHeight() * STEADY_FINE_SCROLL_DOWN_START_FRACTION);
        int endY = (int) (size.getHeight() * STEADY_FINE_SCROLL_DOWN_END_FRACTION);

        diagnostic(
                "Steady fine-down W3C geometry"
                        + " | startY=" + startY
                        + " | endY=" + endY
                        + " | approximateContentShift=" + (endY - startY)
        );

        swipeByCoordinates(x, startY, x, endY, 420);
    }

    private static final class SteadyAdviceExpectation {
        private final String manualCaseId;
        private final String fundName;
        private final String manualDataUsed;
        private final String fundCategory;
        private final String opinion;
        private final String strategy;
        private final String plan;
        private final String prdTemplate;
        private final String expectedRegex;
        private final String manualSheetObservedAdvice;
        private final String manualSheetOutcome;

        private SteadyAdviceExpectation(
                String manualCaseId,
                String fundName,
                String manualDataUsed,
                String fundCategory,
                String opinion,
                String strategy,
                String plan,
                String prdTemplate,
                String expectedRegex,
                String manualSheetObservedAdvice,
                String manualSheetOutcome
        ) {
            this.manualCaseId = manualCaseId;
            this.fundName = fundName;
            this.manualDataUsed = manualDataUsed;
            this.fundCategory = fundCategory;
            this.opinion = opinion;
            this.strategy = strategy;
            this.plan = plan;
            this.prdTemplate = prdTemplate;
            this.expectedRegex = expectedRegex;
            this.manualSheetObservedAdvice = manualSheetObservedAdvice;
            this.manualSheetOutcome = manualSheetOutcome;
        }
    }

    // =========================================================
    // ADDITIONAL FUNCTIONAL COVERAGE - FUND REVIEW ENTRY POINTS
    // =========================================================

    /**
     * PAFA_000 functional entry coverage is intentionally limited to the Summary card.
     *
     * Required journey:
     * Summary attention fund -> Fund Review -> supplied Go back -> Portfolio Analysis.
     * Success after Back is based on the stable Portfolio Analysis shell with both
     * Summary and Funds / SIFs tabs visible.  No bucket-specific assertion belongs here.
     *
     * PAFA_001..PAFA_004 are deliberately untouched and retain their existing bucket
     * navigation, PRD validation, scrolling and recovery logic.
     */
    public EntryPointSummary verifyFundReviewEntryPointsAcrossSummaryAndFundsSifsForPortfolioAnalysisFa() {
        functionalHubRecoveryCount = 0;
        functionalAppRestartCount = 0;

        int totalEntryPoints = 1;
        int openedEntryPoints = 0;

        /*
         * PAFA_000 scope is intentionally narrow:
         * Summary attention card -> one physically visible fund name -> Fund Review ->
         * supplied Go back control -> Portfolio Analysis shell.
         *
         * It does NOT test direct entry from Exit/Optimize/Good/Steady/New Fund cards.
         * Those bucket suites keep their existing navigation and regression logic.
         */
        ensureFunctionalSourceScreenReady(
                FunctionalSourceScreen.SUMMARY,
                "Summary direct-fund entry coverage"
        );

        if (verifyRepresentativeSummaryPreviewFundEntry()) {
            openedEntryPoints++;
        }

        /*
         * Final PAFA_000 state: Portfolio Analysis itself is restored.  We deliberately
         * require only the stable shell requested by QA: heading + Summary tab +
         * Funds / SIFs tab.  Dynamic Summary card content is not part of Back success.
         */
        if (!waitForPortfolioAnalysisShellReady(FUNCTIONAL_CONTROL_WAIT_SECONDS)) {
            throw new AssertionError(
                    "PAFA_000 finished but Portfolio Analysis shell was not restored"
                            + " | required=Portfolio Analysis + Summary tab + Funds / SIFs tab"
            );
        }

        return new EntryPointSummary(
                totalEntryPoints,
                openedEntryPoints,
                functionalHubRecoveryCount,
                functionalAppRestartCount
        );
    }

    /**
     * Finds either of the supplied fund names while moving DOWN through Funds / SIFs
     * with bounded small W3C swipes. Both visible rows in each card are acceptable;
     * no +XX more control is ever queried or tapped by this flow.
     */
    private boolean verifyFirstAvailableFundsSifsDirectFundEntry(
            String cardLabel,
            FunctionalBucket expectedBucket,
            String... candidateFundNames
    ) {
        final String routePrefix = "Funds / SIFs > " + cardLabel;
        final By cardHeaderLocator = functionalCardHeaderForBucket(expectedBucket);
        final By nextSectionLocator = functionalNextSectionForBucket(expectedBucket);

        try {
            ensureFunctionalSourceScreenReady(
                    FunctionalSourceScreen.FUNDS_SIFS,
                    routePrefix
            );

            for (int scan = 0; scan <= FUNCTIONAL_SOURCE_MAX_SCROLLS; scan++) {
                WebElement cardHeader = cardHeaderLocator == null
                        ? null
                        : findVisibleElement(cardHeaderLocator);

                if (cardHeader != null && isElementInsideCurrentViewport(cardHeader)) {
                    WebElement cardEnd = findClosestVisibleMoreChevronBelowHeader(cardHeader);
                    WebElement nextSection = nextSectionLocator == null
                            ? null
                            : findVisibleElement(nextSectionLocator);

                    if (candidateFundNames != null) {
                        for (String fundName : candidateFundNames) {
                            if (fundName == null || fundName.trim().isEmpty()) {
                                continue;
                            }

                            By locator = AppiumBy.accessibilityId(fundName);
                            WebElement row = findVisibleElement(locator);
                            if (!isFundInsideFundsSifsCategoryCard(
                                    row,
                                    cardHeader,
                                    cardEnd,
                                    nextSection
                            )) {
                                continue;
                            }

                            return verifyFunctionalFundReviewEntry(new FunctionalEntrySpec(
                                    FunctionalSourceScreen.FUNDS_SIFS,
                                    routePrefix + " > " + fundName,
                                    locator,
                                    null,
                                    expectedBucket
                            ));
                        }
                    }
                }

                if (scan < FUNCTIONAL_SOURCE_MAX_SCROLLS) {
                    smallSwipeUp();
                }
            }

            ReportLogger.step(
                    "[ENTRY FAIL] " + routePrefix
                            + " | supplied fund names were not physically located inside this card"
            );
            return false;

        } catch (Throwable failure) {
            ReportLogger.step(
                    "[ENTRY FAIL] " + routePrefix
                            + " | error=" + safeThrowableMessage(failure)
            );
            return false;
        }
    }

    private By functionalCardHeaderForBucket(FunctionalBucket bucket) {
        if (bucket == null) {
            return null;
        }
        switch (bucket) {
            case EXIT:
                return exitCardHeader;
            case OPTIMIZE:
                return optimizeCardHeader;
            case GOOD:
                return goodCardHeader;
            case STEADY:
                return steadyCardHeader;
            case NEW_FUND:
                return newFundsCardHeader;
            default:
                return null;
        }
    }

    /**
     * Used only as a geometric boundary; these locators are never tapped here.
     */
    private By functionalNextSectionForBucket(FunctionalBucket bucket) {
        if (bucket == null) {
            return null;
        }
        switch (bucket) {
            case EXIT:
                return optimizeCardHeader;
            case OPTIMIZE:
                return goodCardHeader;
            case GOOD:
                return steadyCardHeader;
            case STEADY:
                return newFundsCardHeader;
            case NEW_FUND:
                return fundsSifsPortfolioInsights;
            default:
                return null;
        }
    }

    /**
     * Prevents a duplicate fund accessibility-id elsewhere on Funds/SIFs (for example
     * Portfolio Insights) from being mistaken for a row inside the requested card.
     */
    private boolean isFundInsideFundsSifsCategoryCard(
            WebElement fund,
            WebElement header,
            WebElement cardEnd,
            WebElement nextSection
    ) {
        if (fund == null || header == null || !isElementInsideCurrentViewport(fund)) {
            return false;
        }

        try {
            Rectangle headerRect = header.getRect();
            Rectangle fundRect = fund.getRect();
            int headerBottom = headerRect.getY() + headerRect.getHeight();
            int fundTop = fundRect.getY();
            int fundBottom = fundTop + fundRect.getHeight();

            if (fundRect.getWidth() <= 0
                    || fundRect.getHeight() <= 0
                    || fundTop < headerBottom) {
                return false;
            }

            int boundaryY = Integer.MAX_VALUE;

            if (cardEnd != null && isElementInsideCurrentViewport(cardEnd)) {
                Rectangle endRect = cardEnd.getRect();
                if (endRect.getY() > headerBottom) {
                    boundaryY = Math.min(boundaryY, endRect.getY());
                }
            }

            if (nextSection != null && isElementInsideCurrentViewport(nextSection)) {
                Rectangle nextRect = nextSection.getRect();
                if (nextRect.getY() > headerBottom) {
                    boundaryY = Math.min(boundaryY, nextRect.getY());
                }
            }

            if (boundaryY == Integer.MAX_VALUE) {
                int screenHeight = driver.manage().window().getSize().getHeight();
                boundaryY = headerBottom + (int) (screenHeight * 0.24);
            }

            return fundBottom <= boundaryY;
        } catch (Exception ignored) {
            return false;
        }
    }

    /**
     * Summary preview data is dynamic. QA only needs one of the two currently
     * displayed attention funds to prove the direct Summary -> Fund Review route.
     * We therefore choose the first supplied/current locator that is actually inside
     * the visible attention card instead of hardcoding a specific first row.
     */
    private boolean verifyRepresentativeSummaryPreviewFundEntry() {
        ensureFunctionalSourceScreenReady(
                FunctionalSourceScreen.SUMMARY,
                "Summary direct-fund entry"
        );

        WebElement header = waitForFunctionalElementInViewport(
                summaryAttentionHeader,
                FUNCTIONAL_CONTROL_WAIT_SECONDS
        );
        if (header == null) {
            ReportLogger.step("[ENTRY FAIL] Summary > attention preview | attention card not visible");
            return false;
        }

        /*
         * Flutter may publish accessibility nodes for the complete attention list even
         * while Summary is collapsed.  Use the card geometry only to choose a fund that
         * is physically inside the visible Summary preview.  +XX more is a boundary only
         * and is never clicked by PAFA_000.
         */
        WebElement previewEnd = findClosestVisibleMoreChevronBelowHeader(header);
        WebElement lowerSummaryBoundary = findVisibleElement(summaryOverallPortfolioPerformance);
        if (lowerSummaryBoundary == null) {
            lowerSummaryBoundary = findVisibleElement(summaryPortfolioProfile);
        }

        String[] candidateNames = {
                "ABSL Liquid Reg-G",
                "ABSL Ultra Short Term Reg-G",
                "ABSL Corporate Bond Reg-G",
                "HDFC Aggressive Hybrid Reg-G",
                "Tata Children's Reg",
                "Axis Banking and PSU Debt Reg-G",
                "Axis ELSS - Tax Saver Reg-G",
                "Axis Liquid Reg-G",
                "HDFC Credit Risk Reg-G",
                "ICICI Pru Aggressive Hybrid-G",
                "ICICI Pru Balanced Advantage-G",
                "ICICI Pru Floating Interest Rates-G",
                "ICICI Pru Large Cap-G",
                "Kotak Arbitrage Reg-G",
                "Kotak Flexi Cap Reg-G",
                "Mirae Asset Large Cap Reg-G",
                "Motilal Oswal Flexi Cap Reg-G",
                "Nippon India Large Cap Reg-G"
        };

        for (String fundName : candidateNames) {
            By locator = AppiumBy.accessibilityId(fundName);
            WebElement row = findVisibleElement(locator);
            if (!isFundInsideCollapsedSummaryPreview(
                    row,
                    header,
                    previewEnd,
                    lowerSummaryBoundary
            )) {
                continue;
            }

            return verifySummaryFundReviewEntryAndReturn(fundName, locator);
        }

        ReportLogger.step(
                "[ENTRY FAIL] Summary > attention preview"
                        + " | no supplied fund name was physically inside the visible Summary card"
        );
        return false;
    }

    /**
     * PAFA_000-only journey.  Success means exactly what the functional requirement asks:
     * a visible Summary fund opens Fund Review, then the supplied "Go back" control returns
     * to Portfolio Analysis where both Summary and Funds / SIFs tabs are exposed.
     *
     * No Fund Review bucket assertion is performed here.  Bucket behavior belongs to the
     * dedicated PAFA_001..PAFA_004 regressions and is intentionally left untouched.
     */
    private boolean verifySummaryFundReviewEntryAndReturn(
            String fundName,
            By fundLocator
    ) {
        final String routeLabel = "Summary > " + fundName;
        Throwable lastFailure = null;
        String failureStage = "not started";

        for (int attempt = 1; attempt <= 3; attempt++) {
            boolean reviewOpened = false;
            try {
                failureStage = "restore Summary";
                if (attempt == 1) {
                    ensureFunctionalSourceScreenReady(FunctionalSourceScreen.SUMMARY, routeLabel);
                } else if (attempt == 2) {
                    recoverFunctionalSourceThroughHub(FunctionalSourceScreen.SUMMARY, routeLabel);
                } else {
                    restartAdvisorAppAndRestoreFunctionalSource(FunctionalSourceScreen.SUMMARY, routeLabel);
                }

                FunctionalEntrySpec entry = new FunctionalEntrySpec(
                        FunctionalSourceScreen.SUMMARY,
                        routeLabel,
                        fundLocator,
                        null,
                        null
                );

                failureStage = "locate visible Summary fund";
                WebElement control = findDirectFunctionalEntryControl(entry);
                String actualControlLabel = getReadableText(control);

                failureStage = "open Fund Review";
                if (!openDirectFundNameAndWaitForFundReview(
                        FunctionalSourceScreen.SUMMARY,
                        control
                )) {
                    throw new AssertionError(
                            "Fund Review + Go back did not appear after clicking the Summary fund"
                    );
                }
                reviewOpened = true;

                failureStage = "Go back to Portfolio Analysis";
                returnFromSummaryFundReviewToPortfolioAnalysisShell(routeLabel);

                failureStage = "confirm Portfolio Analysis tabs";
                if (!waitForPortfolioAnalysisShellReady(FUNCTIONAL_CONTROL_WAIT_SECONDS)) {
                    throw new AssertionError(
                            "Portfolio Analysis shell was not ready after Fund Review Go back"
                                    + " | required=Portfolio Analysis + Summary + Funds / SIFs"
                    );
                }

                ReportLogger.pass(
                        "[ENTRY PASS] " + routeLabel
                                + " | tapped=" + quote(actualControlLabel)
                                + " | opened=Fund Review"
                                + " | returned=Portfolio Analysis"
                                + " | tabs=Summary + Funds / SIFs visible"
                );
                return true;

            } catch (Throwable routeFailure) {
                lastFailure = routeFailure;

                if (reviewOpened
                        || isDisplayedNow(fundReviewHeader)
                        || isDisplayedNow(fundReviewGoBackButton)) {
                    tryReturnFromSummaryFundReviewToPortfolioAnalysisShellWithoutRecovery();
                }

                if (attempt == 1) {
                    ReportLogger.step(
                            "[RECOVERY] " + routeLabel
                                    + " | stage=" + failureStage
                                    + " | error=" + safeThrowableMessage(routeFailure)
                                    + " | action=reopen Portfolio Analysis from Hub"
                    );
                } else if (attempt == 2) {
                    ReportLogger.step(
                            "[RECOVERY] " + routeLabel
                                    + " | stage=" + failureStage
                                    + " | error=" + safeThrowableMessage(routeFailure)
                                    + " | action=restart Advisor app and retry Summary once"
                    );
                }
            }
        }

        ReportLogger.step(
                "[ENTRY FAIL] " + routeLabel
                        + " | stage=" + failureStage
                        + " | after=Hub recovery + app restart"
                        + " | error=" + safeThrowableMessage(lastFailure)
        );
        return false;
    }

    private void returnFromSummaryFundReviewToPortfolioAnalysisShell(
            String routeLabel
    ) {
        if (waitForPortfolioAnalysisShellReady(2)) {
            return;
        }

        /* Primary path: exact Inspector locator accessibilityId("Go back"). */
        boolean suppliedBackWorked = tapFundReviewGoBackButton();
        if (suppliedBackWorked
                && waitForPortfolioAnalysisShellReady(FUNCTIONAL_CONTROL_WAIT_SECONDS)) {
            return;
        }

        /*
         * If Fund Review disappeared after the supplied Go back tap, do NOT press Back
         * again.  A second Back could leave Portfolio Analysis while Flutter is rebuilding.
         */
        if (!isDisplayedNow(fundReviewHeader)) {
            if (waitForPortfolioAnalysisShellReady(15)) {
                return;
            }
        } else {
            // Single bounded Android Back fallback only when Fund Review is still present.
            pressBackSafely();
            if (waitForPortfolioAnalysisShellReady(20)) {
                return;
            }
        }

        throw new AssertionError(
                "Fund Review Go back did not restore Portfolio Analysis shell"
                        + " | route=" + routeLabel
                        + " | requiredTabs=Summary, Funds / SIFs"
        );
    }

    private boolean tryReturnFromSummaryFundReviewToPortfolioAnalysisShellWithoutRecovery() {
        if (waitForPortfolioAnalysisShellReady(1)) {
            return true;
        }

        if (isDisplayedNow(fundReviewHeader) || isDisplayedNow(fundReviewGoBackButton)) {
            if (tapFundReviewGoBackButton()
                    && waitForPortfolioAnalysisShellReady(12)) {
                return true;
            }
        }

        if (isDisplayedNow(fundReviewHeader)) {
            pressBackSafely();
            return waitForPortfolioAnalysisShellReady(12);
        }

        return waitForPortfolioAnalysisShellReady(6);
    }

    /**
     * Accepts a Summary fund only when its complete rectangle is physically between
     * the attention-card heading and the collapsed preview's lower boundary.  This is
     * intentionally stricter than isDisplayed(): Flutter can expose off-card semantics
     * for funds that become visible only after expanding the list.
     */
    private boolean isFundInsideCollapsedSummaryPreview(
            WebElement fund,
            WebElement header,
            WebElement previewEnd,
            WebElement lowerSummaryBoundary
    ) {
        if (fund == null || header == null || !isElementInsideCurrentViewport(fund)) {
            return false;
        }

        try {
            Rectangle headerRect = header.getRect();
            Rectangle fundRect = fund.getRect();
            int headerBottom = headerRect.getY() + headerRect.getHeight();
            int fundTop = fundRect.getY();
            int fundBottom = fundTop + fundRect.getHeight();

            if (fundRect.getWidth() <= 0
                    || fundRect.getHeight() <= 0
                    || fundTop < headerBottom) {
                return false;
            }

            int boundaryY = Integer.MAX_VALUE;

            if (previewEnd != null && isElementInsideCurrentViewport(previewEnd)) {
                Rectangle moreRect = previewEnd.getRect();
                if (moreRect.getY() > headerBottom) {
                    boundaryY = Math.min(boundaryY, moreRect.getY());
                }
            }

            if (lowerSummaryBoundary != null
                    && isElementInsideCurrentViewport(lowerSummaryBoundary)) {
                Rectangle lowerRect = lowerSummaryBoundary.getRect();
                if (lowerRect.getY() > headerBottom) {
                    boundaryY = Math.min(boundaryY, lowerRect.getY());
                }
            }

            if (boundaryY == Integer.MAX_VALUE) {
                int screenHeight = driver.manage().window().getSize().getHeight();
                boundaryY = headerBottom + (int) (screenHeight * 0.18);
            }

            return fundBottom <= boundaryY;
        } catch (Exception ignored) {
            return false;
        }
    }

    /**
     * Validates the complete +XX more journey requested by QA:
     * source card -> +XX more -> expanded list -> fund -> Fund Review -> Go back ->
     * original Portfolio Analysis source page. No fixed sleep is used; every state
     * transition is guarded by an explicit condition wait.
     */
    private boolean verifyFunctionalMoreDrawerFundEntry(
            FunctionalSourceScreen sourceScreen,
            String routeLabel,
            By cardHeaderLocator,
            By currentMoreLocator,
            FunctionalBucket expectedBucket,
            By... drawerFundCandidates
    ) {
        Throwable lastFailure = null;

        for (int attempt = 1; attempt <= 3; attempt++) {
            try {
                if (attempt == 1) {
                    ensureFunctionalSourceScreenReady(sourceScreen, routeLabel);
                } else if (attempt == 2) {
                    recoverFunctionalSourceThroughHub(sourceScreen, routeLabel);
                } else {
                    restartAdvisorAppAndRestoreFunctionalSource(sourceScreen, routeLabel);
                }

                WebElement header = findFunctionalSourceElement(
                        sourceScreen,
                        cardHeaderLocator,
                        routeLabel + " card"
                );

                WebElement more = locateFunctionalEntryControl(
                        sourceScreen,
                        currentMoreLocator,
                        cardHeaderLocator,
                        routeLabel
                );

                String tappedMoreLabel = getReadableText(more);
                if (tappedMoreLabel == null || !tappedMoreLabel.matches("\\+\\d+ more")) {
                    throw new AssertionError(
                            "Expanded-list control did not expose the expected +XX more accessibility label"
                    );
                }

                tapElementCenter(more);

                if (!waitForFunctionalExpandedDrawerReady(
                        cardHeaderLocator,
                        tappedMoreLabel,
                        drawerFundCandidates,
                        FUNCTIONAL_CONTROL_WAIT_SECONDS
                )) {
                    throw new AssertionError(
                            "Expanded fund list did not become ready after tapping " + tappedMoreLabel
                    );
                }

                WebElement drawerFund = findFirstVisibleFunctionalCandidate(drawerFundCandidates);
                if (drawerFund == null) {
                    throw new AssertionError(
                            "Expanded fund list opened but none of the supplied fund rows was visible"
                    );
                }

                String selectedFund = getReadableText(drawerFund);
                tapElementByPercent(drawerFund, 0.90, 0.50);

                if (!waitForExpectedFunctionalFundReview(
                        expectedBucket,
                        FUNCTIONAL_ROUTE_WAIT_SECONDS
                )) {
                    throw new AssertionError(
                            "Fund Review did not become ready after selecting " + selectedFund
                    );
                }

                returnFromFundReviewToFunctionalSourceAfterDrawerEntry(
                        sourceScreen,
                        routeLabel,
                        cardHeaderLocator,
                        tappedMoreLabel,
                        drawerFundCandidates
                );

                ReportLogger.pass(
                        "[ENTRY PASS] " + routeLabel
                                + " | " + tappedMoreLabel
                                + " > " + selectedFund
                                + " | opened=" + functionalBucketDisplayName(expectedBucket)
                                + " | investor=My Investment - 5"
                                + " | returned=" + functionalSourceDisplayName(sourceScreen)
                );
                return true;
            } catch (Throwable routeFailure) {
                lastFailure = routeFailure;

                if (attempt == 1) {
                    ReportLogger.step(
                            "[RECOVERY] " + routeLabel
                                    + " | route not ready after explicit wait"
                                    + " | action=reopen Portfolio Analysis from Hub"
                    );
                } else if (attempt == 2) {
                    ReportLogger.step(
                            "[RECOVERY] " + routeLabel
                                    + " | Hub retry failed"
                                    + " | action=restart Advisor app and retry fresh"
                    );
                }
            }
        }

        ReportLogger.step(
                "[ENTRY FAIL] " + routeLabel
                        + " | after=Hub recovery + app restart"
                        + " | error=" + safeThrowableMessage(lastFailure)
        );

        try {
            ensureFunctionalSourceScreenReady(
                    sourceScreen,
                    routeLabel + " final restore"
            );
        } catch (Throwable ignored) {
            diagnostic("Unable to restore source after failed +XX more route: " + routeLabel);
        }
        return false;
    }

    private boolean waitForFunctionalExpandedDrawerReady(
            By cardHeaderLocator,
            String tappedMoreLabel,
            By[] drawerFundCandidates,
            int timeoutSeconds
    ) {
        By tappedMoreLocator = AppiumBy.accessibilityId(tappedMoreLabel);
        final int[] consecutiveReadyPolls = {0};

        return explicitWaitUntil(timeoutSeconds, () -> {
            boolean ready = !isDisplayedNow(fundReviewHeader)
                    && isDisplayedNow(cardHeaderLocator)
                    && !isDisplayedNow(tappedMoreLocator)
                    && findFirstVisibleFunctionalCandidate(drawerFundCandidates) != null;

            if (ready) {
                consecutiveReadyPolls[0]++;
            } else {
                consecutiveReadyPolls[0] = 0;
            }
            return consecutiveReadyPolls[0] >= 2;
        });
    }

    private WebElement findFirstVisibleFunctionalCandidate(By... candidates) {
        if (candidates == null) {
            return null;
        }

        for (By candidateLocator : candidates) {
            if (candidateLocator == null) {
                continue;
            }
            WebElement candidate = findVisibleElement(candidateLocator);
            if (candidate != null && isElementInsideCurrentViewport(candidate)) {
                return candidate;
            }
        }
        return null;
    }

    /**
     * A Fund Review opened from an expanded list can return either directly to the
     * source page or briefly restore that list. Both are valid app transitions. If
     * the list is restored we close it with the same downward sheet gesture QA uses,
     * then explicitly wait for the Portfolio Analysis source page.
     */
    private void returnFromFundReviewToFunctionalSourceAfterDrawerEntry(
            FunctionalSourceScreen sourceScreen,
            String routeLabel,
            By drawerHeaderLocator,
            String tappedMoreLabel,
            By... drawerFundCandidates
    ) {
        if (isDisplayedNow(fundReviewHeader)) {
            if (!tapFundReviewGoBackButton()) {
                pressBackSafely();
            }
        }

        final boolean[] sourceReady = {false};
        final boolean[] drawerRestored = {false};

        explicitWaitUntil(FUNCTIONAL_CONTROL_WAIT_SECONDS, () -> {
            if (isFunctionalSourceScreenReady(sourceScreen)) {
                sourceReady[0] = true;
                return true;
            }

            if (isFunctionalExpandedDrawerReadyNow(
                    drawerHeaderLocator,
                    tappedMoreLabel,
                    drawerFundCandidates
            )) {
                drawerRestored[0] = true;
                return true;
            }
            return false;
        });

        if (sourceReady[0]) {
            return;
        }

        if (drawerRestored[0]) {
            closeFunctionalExpandedDrawerBySwipingDown(
                    sourceScreen,
                    drawerHeaderLocator
            );
            return;
        }

        if (isDisplayedNow(fundReviewHeader)) {
            pressBackSafely();
            if (waitForFunctionalSourceScreenReady(
                    sourceScreen,
                    FUNCTIONAL_CONTROL_WAIT_SECONDS
            )) {
                return;
            }
        }

        ReportLogger.step(
                "[RECOVERY] " + routeLabel
                        + " | back navigation did not restore "
                        + functionalSourceDisplayName(sourceScreen)
                        + " | action=reopen Portfolio Analysis from Hub"
        );
        recoverFunctionalSourceThroughHub(sourceScreen, routeLabel + " return path");
    }

    private boolean isFunctionalExpandedDrawerReadyNow(
            By drawerHeaderLocator,
            String tappedMoreLabel,
            By... drawerFundCandidates
    ) {
        if (isDisplayedNow(fundReviewHeader)
                || drawerHeaderLocator == null
                || tappedMoreLabel == null
                || tappedMoreLabel.trim().isEmpty()) {
            return false;
        }

        return isDisplayedNow(drawerHeaderLocator)
                && !isDisplayedNow(AppiumBy.accessibilityId(tappedMoreLabel))
                && findFirstVisibleFunctionalCandidate(drawerFundCandidates) != null;
    }

    private void closeFunctionalExpandedDrawerBySwipingDown(
            FunctionalSourceScreen sourceScreen,
            By drawerHeaderLocator
    ) {
        for (int attempt = 1; attempt <= 2; attempt++) {
            Dimension screen = driver.manage().window().getSize();
            int screenWidth = screen.getWidth();
            int screenHeight = screen.getHeight();

            int startY = (int) (screenHeight * (attempt == 1 ? 0.18 : 0.16));
            WebElement drawerHeading = findVisibleElement(drawerHeaderLocator);
            if (drawerHeading != null) {
                try {
                    Rectangle headerRect = drawerHeading.getRect();
                    int derivedY = headerRect.getY() - (int) (screenHeight * 0.03);
                    int minY = (int) (screenHeight * 0.13);
                    int maxY = (int) (screenHeight * 0.25);
                    startY = Math.max(minY, Math.min(maxY, derivedY));
                } catch (Exception ignored) {
                    // Use screen-relative top-of-sheet fallback.
                }
            }

            swipeByCoordinates(
                    screenWidth / 2,
                    startY,
                    screenWidth / 2,
                    (int) (screenHeight * (attempt == 1 ? 0.93 : 0.97)),
                    attempt == 1 ? 850 : 1050
            );

            if (waitForFunctionalSourceScreenReady(sourceScreen, 12)) {
                return;
            }
        }

        throw new AssertionError(
                "Expanded fund list did not close after two downward sheet gestures"
        );
    }

    /**
     * Builds Summary entry coverage from only the supplied fund locators that are
     * actually visible in the current dynamic attention card. This prevents a
     * portfolio data change (for example HDFC disappearing and ABSL appearing) from
     * being misclassified as "Summary page did not load".
     */
    private List<FunctionalEntrySpec> buildVisibleSummaryEntrySpecsFromSuppliedLocators() {
        List<FunctionalEntrySpec> entries = new ArrayList<>();

        WebElement attentionHeader = waitForFunctionalElementInViewport(
                summaryAttentionHeader,
                FUNCTIONAL_CONTROL_WAIT_SECONDS
        );
        if (attentionHeader == null) {
            throw new AssertionError(
                    "Summary page is loaded, but the supplied 'Funds that need attention' card was not visible"
            );
        }

        addSummaryEntryIfVisible(
                entries,
                attentionHeader,
                "HDFC Aggressive Hybrid Reg-G",
                summaryHdfcEntryFund,
                FunctionalBucket.EXIT
        );
        addSummaryEntryIfVisible(
                entries,
                attentionHeader,
                "Tata Children's Reg",
                summaryTataEntryFund,
                FunctionalBucket.EXIT
        );
        addSummaryEntryIfVisible(
                entries,
                attentionHeader,
                "ABSL Corporate Bond Reg-G",
                optimizeDirectEntryFund,
                FunctionalBucket.OPTIMIZE
        );
        addSummaryEntryIfVisible(
                entries,
                attentionHeader,
                "ABSL Liquid Reg-G",
                optimizeSecondEntryFund,
                FunctionalBucket.OPTIMIZE
        );
        addSummaryEntryIfVisible(
                entries,
                attentionHeader,
                "Edelweiss Technology Dir-G",
                goodFirstEntryFund,
                FunctionalBucket.GOOD
        );
        addSummaryEntryIfVisible(
                entries,
                attentionHeader,
                "Edelweiss Technology Reg-G",
                goodDirectEntryFund,
                FunctionalBucket.GOOD
        );
        addSummaryEntryIfVisible(
                entries,
                attentionHeader,
                "ABSL Transportation and Logistics Dir-G",
                steadyDirectEntryFund,
                FunctionalBucket.STEADY
        );
        addSummaryEntryIfVisible(
                entries,
                attentionHeader,
                "ABSL Transportation And Logistics Reg-G",
                steadySecondEntryFund,
                FunctionalBucket.STEADY
        );
        addSummaryEntryIfVisible(
                entries,
                attentionHeader,
                "Axis Nifty500 Low Volatility 50 Index Dir-G",
                newFundDirectEntryFund,
                FunctionalBucket.NEW_FUND
        );
        addSummaryEntryIfVisible(
                entries,
                attentionHeader,
                "Axis Nifty500 Low Volatility 50 Index Reg-G",
                newFundSecondEntryFund,
                FunctionalBucket.NEW_FUND
        );

        if (entries.isEmpty()) {
            throw new AssertionError(
                    "Summary page is fully loaded, but none of the supplied direct fund locators "
                            + "is currently visible inside the attention card"
            );
        }

        /*
         * Summary +XX more opens an expanded attention list, not Fund Review.
         * That transition is validated separately below.
         */
        return entries;
    }

    private void addSummaryEntryIfVisible(
            List<FunctionalEntrySpec> entries,
            WebElement attentionHeader,
            String fundName,
            By fundLocator,
            FunctionalBucket expectedBucket
    ) {
        WebElement fund = findVisibleElement(fundLocator);
        if (fund == null
                || !isElementInsideCurrentViewport(fund)
                || !isControlBelowHeader(fund, attentionHeader)) {
            return;
        }

        entries.add(new FunctionalEntrySpec(
                FunctionalSourceScreen.SUMMARY,
                "Summary > " + fundName,
                fundLocator,
                null,
                expectedBucket
        ));
    }

    /**
     * Summary +XX more is an expanded-list route, not a Fund Review route.
     */
    private boolean verifySummaryAttentionMoreExpansionAndReturn() {
        final String routeLabel = "Summary > Funds that need attention > +XX more";
        Throwable lastFailure = null;

        for (int attempt = 1; attempt <= 3; attempt++) {
            try {
                if (attempt == 1) {
                    ensureFunctionalSourceScreenReady(FunctionalSourceScreen.SUMMARY, routeLabel);
                } else if (attempt == 2) {
                    recoverFunctionalSourceThroughHub(FunctionalSourceScreen.SUMMARY, routeLabel);
                } else {
                    restartAdvisorAppAndRestoreFunctionalSource(FunctionalSourceScreen.SUMMARY, routeLabel);
                }

                WebElement header = WaitUtils.waitForElement(
                        driver,
                        summaryAttentionHeader,
                        FUNCTIONAL_CONTROL_WAIT_SECONDS
                );

                WebElement more = waitForFunctionalElementNearHeader(
                        summaryAttentionMoreCurrent,
                        header,
                        FUNCTIONAL_SCROLL_CHECK_SECONDS
                );
                if (more == null) {
                    more = findClosestVisibleMoreChevronBelowHeader(header);
                }
                if (more == null) {
                    By dynamicMore = deriveMoreChevronLocatorFromHeader(header, routeLabel);
                    more = waitForFunctionalElementNearHeader(
                            dynamicMore,
                            header,
                            FUNCTIONAL_CONTROL_WAIT_SECONDS
                    );
                }
                if (more == null) {
                    throw new AssertionError("Summary dynamic +XX more control was not found");
                }

                String tappedLabel = getReadableText(more);
                tapElementCenter(more);

                if (!waitForSummaryAttentionExpandedListReady(FUNCTIONAL_CONTROL_WAIT_SECONDS)) {
                    throw new AssertionError(
                            "Summary +XX more did not open the expanded Funds that need attention list"
                    );
                }

                /*
                 * Close this specific expanded-list bottom sheet with the same user
                 * gesture used manually: drag DOWN from the top/handle area of the
                 * sheet. Do not use Android Back here. The starting point is derived
                 * from the live sheet heading and screen size, so it is not tied to
                 * Pixel coordinates.
                 */
                closeSummaryAttentionExpandedDrawerBySwipingDown();

                ReportLogger.pass(
                        "[ENTRY PASS] " + routeLabel
                                + " | tapped=" + quote(tappedLabel)
                                + " | opened=expanded attention list"
                                + " | closed=swipe-down"
                                + " | returned=Summary"
                );
                return true;
            } catch (Throwable routeFailure) {
                lastFailure = routeFailure;
                if (attempt == 1) {
                    ReportLogger.step(
                            "[RECOVERY] " + routeLabel
                                    + " | expanded-list route not ready"
                                    + " | action=reopen Portfolio Analysis from Hub"
                    );
                } else if (attempt == 2) {
                    ReportLogger.step(
                            "[RECOVERY] " + routeLabel
                                    + " | Hub retry did not restore expanded-list route"
                                    + " | action=restart Advisor app and retry fresh"
                    );
                }
            }
        }

        ReportLogger.step(
                "[ENTRY FAIL] " + routeLabel
                        + " | after=Hub recovery + app restart"
                        + " | error=" + safeThrowableMessage(lastFailure)
        );
        try {
            ensureFunctionalSourceScreenReady(
                    FunctionalSourceScreen.SUMMARY,
                    routeLabel + " final restore"
            );
        } catch (Throwable ignored) {
            diagnostic("Unable to restore Summary after failed +XX more route");
        }
        return false;
    }

    /**
     * Dismisses the Summary "Funds that need attention" expanded bottom sheet by
     * swiping DOWN from the sheet's top/handle region. This is intentionally not an
     * Android Back action because the current Flutter route can ignore Back for this
     * modal sheet.
     *
     * The drag start is derived from the live attention heading position and clamped
     * to a narrow top-of-sheet band. A second, slightly longer drag is used only if
     * the first gesture is ignored. Both attempts use condition-based waits and stop
     * immediately once the collapsed Summary page is confirmed.
     */
    private void closeSummaryAttentionExpandedDrawerBySwipingDown() {
        if (!isSummaryAttentionExpandedListReady()) {
            if (waitForFunctionalSourceScreenReady(FunctionalSourceScreen.SUMMARY, 3)) {
                return;
            }
            throw new AssertionError(
                    "Expanded Summary attention drawer was not in a swipe-closeable state"
            );
        }

        for (int attempt = 1; attempt <= 2; attempt++) {
            Dimension screen = driver.manage().window().getSize();
            int screenWidth = screen.getWidth();
            int screenHeight = screen.getHeight();

            int minTopBandY = (int) (screenHeight * 0.14);
            int maxTopBandY = (int) (screenHeight * 0.24);
            int startY = (int) (screenHeight * (attempt == 1 ? 0.18 : 0.16));

            WebElement drawerHeading = findVisibleElement(summaryAttentionHeader);
            if (drawerHeading != null) {
                try {
                    Rectangle headerRect = drawerHeading.getRect();
                    int derivedFromHeading = headerRect.getY() - (int) (screenHeight * 0.035);
                    startY = Math.max(
                            minTopBandY,
                            Math.min(maxTopBandY, derivedFromHeading)
                    );
                } catch (Exception ignored) {
                    // Use the screen-relative top-of-sheet fallback calculated above.
                }
            }

            int startX = screenWidth / 2;
            int endY = (int) (screenHeight * (attempt == 1 ? 0.93 : 0.97));
            int durationMs = attempt == 1 ? 850 : 1050;

            diagnostic(
                    "Closing Summary attention drawer with downward W3C drag"
                            + " | attempt=" + attempt + "/2"
                            + " | start=(" + startX + "," + startY + ")"
                            + " | endY=" + endY
            );

            swipeByCoordinates(
                    startX,
                    startY,
                    startX,
                    endY,
                    durationMs
            );

            if (waitForFunctionalSourceScreenReady(
                    FunctionalSourceScreen.SUMMARY,
                    12
            )) {
                return;
            }
        }

        throw new AssertionError(
                "Expanded attention list remained open after two top-to-bottom swipe-down attempts"
        );
    }

    private boolean verifyFunctionalFundReviewEntry(FunctionalEntrySpec entry) {
        Throwable lastFailure = null;
        String failureStage = "not started";

        for (int attempt = 1; attempt <= 3; attempt++) {
            boolean fundReviewOpened = false;
            try {
                failureStage = "restore " + functionalSourceDisplayName(entry.sourceScreen);
                if (attempt == 1) {
                    ensureFunctionalSourceScreenReady(entry.sourceScreen, entry.routeLabel);
                } else if (attempt == 2) {
                    recoverFunctionalSourceThroughHub(entry.sourceScreen, entry.routeLabel);
                } else {
                    restartAdvisorAppAndRestoreFunctionalSource(entry.sourceScreen, entry.routeLabel);
                }

                failureStage = "locate fund row";
                WebElement control = findDirectFunctionalEntryControl(entry);
                String actualControlLabel = getReadableText(control);

                failureStage = "open Fund Review";
                if (!openDirectFundNameAndWaitForFundReview(entry.sourceScreen, control)) {
                    throw new AssertionError(
                            "Fund Review + Go back did not appear after clicking the fund name"
                    );
                }
                fundReviewOpened = true;

                failureStage = "validate Fund Review context";
                verifyFunctionalFundReviewContext(
                        entry.expectedBucket,
                        entry.routeLabel
                );

                failureStage = "return with Fund Review Go back";
                returnFromFundReviewToFunctionalSource(entry.sourceScreen, entry.routeLabel);

                failureStage = "completed";
                ReportLogger.pass(
                        "[ENTRY PASS] " + entry.routeLabel
                                + " | tapped=" + quote(actualControlLabel)
                                + " | opened=Fund Review"
                                + (entry.expectedBucket == null
                                        ? ""
                                        : " > " + functionalBucketDisplayName(entry.expectedBucket))
                                + " | investor=My Investment - 5"
                                + " | returned=" + functionalSourceDisplayName(entry.sourceScreen)
                );
                return true;

            } catch (Throwable routeFailure) {
                lastFailure = routeFailure;

                /*
                 * If Fund Review was reached, always try the supplied Go back locator
                 * before entering Hub/app recovery.  Previously a bucket/investor
                 * validation failure jumped straight to recovery and made it look as
                 * if Go back itself had failed even though it was never attempted.
                 */
                if (fundReviewOpened
                        || isDisplayedNow(fundReviewHeader)
                        || isDisplayedNow(fundReviewGoBackButton)) {
                    tryReturnFromFundReviewToSourceWithoutRecovery(entry.sourceScreen);
                }

                if (attempt == 1) {
                    ReportLogger.step(
                            "[RECOVERY] " + entry.routeLabel
                                    + " | stage=" + failureStage
                                    + " | error=" + safeThrowableMessage(routeFailure)
                                    + " | action=reopen Portfolio Analysis from Hub"
                    );
                } else if (attempt == 2) {
                    ReportLogger.step(
                            "[RECOVERY] " + entry.routeLabel
                                    + " | stage=" + failureStage
                                    + " | error=" + safeThrowableMessage(routeFailure)
                                    + " | action=restart Advisor app and retry fresh"
                    );
                }
            }
        }

        ReportLogger.step(
                "[ENTRY FAIL] " + entry.routeLabel
                        + " | stage=" + failureStage
                        + " | after=Hub recovery + app restart"
                        + " | error=" + safeThrowableMessage(lastFailure)
        );

        try {
            ensureFunctionalSourceScreenReady(
                    entry.sourceScreen,
                    entry.routeLabel + " next-route restore"
            );
        } catch (Throwable ignored) {
            diagnostic("Unable to restore source after failed functional entry: " + entry.routeLabel);
        }

        return false;
    }

    /**
     * Re-acquires the exact direct fund row inside the expected source card.  This
     * prevents a duplicate accessibility-id from another section, or an offstage
     * Flutter semantics node, from being tapped during a retry.
     */
    private WebElement findDirectFunctionalEntryControl(FunctionalEntrySpec entry) {
        if (entry == null || entry.exactControlLocator == null) {
            throw new AssertionError("Direct functional entry did not provide a fund-name locator");
        }

        if (entry.sourceScreen == FunctionalSourceScreen.SUMMARY) {
            WebElement header = waitForFunctionalElementInViewport(
                    summaryAttentionHeader,
                    FUNCTIONAL_CONTROL_WAIT_SECONDS
            );
            if (header == null) {
                throw new AssertionError("Summary attention preview header was not visible");
            }

            WebElement previewEnd = findClosestVisibleMoreChevronBelowHeader(header);
            WebElement lowerBoundary = findVisibleElement(summaryOverallPortfolioPerformance);
            if (lowerBoundary == null) {
                lowerBoundary = findVisibleElement(summaryPortfolioProfile);
            }

            final WebElement stableHeader = header;
            final WebElement stablePreviewEnd = previewEnd;
            final WebElement stableLowerBoundary = lowerBoundary;
            final WebElement[] match = new WebElement[1];

            boolean found = explicitWaitUntil(
                    FUNCTIONAL_CONTROL_WAIT_SECONDS,
                    () -> {
                        WebElement candidate = findVisibleElement(entry.exactControlLocator);
                        if (!isFundInsideCollapsedSummaryPreview(
                                candidate,
                                stableHeader,
                                stablePreviewEnd,
                                stableLowerBoundary
                        )) {
                            return false;
                        }
                        match[0] = candidate;
                        return true;
                    }
            );

            if (found) {
                return match[0];
            }

            throw new AssertionError(
                    entry.routeLabel
                            + " fund row was not physically inside the collapsed Summary preview"
            );
        }

        By cardHeaderLocator = functionalCardHeaderForBucket(entry.expectedBucket);
        By nextSectionLocator = functionalNextSectionForBucket(entry.expectedBucket);
        if (cardHeaderLocator == null) {
            return findFunctionalSourceElement(
                    entry.sourceScreen,
                    entry.exactControlLocator,
                    entry.routeLabel + " fund row"
            );
        }

        for (int scan = 0; scan <= FUNCTIONAL_SOURCE_MAX_SCROLLS; scan++) {
            WebElement header = findVisibleElement(cardHeaderLocator);
            if (header != null && isElementInsideCurrentViewport(header)) {
                WebElement cardEnd = findClosestVisibleMoreChevronBelowHeader(header);
                WebElement nextSection = nextSectionLocator == null
                        ? null
                        : findVisibleElement(nextSectionLocator);
                WebElement candidate = findVisibleElement(entry.exactControlLocator);

                if (isFundInsideFundsSifsCategoryCard(
                        candidate,
                        header,
                        cardEnd,
                        nextSection
                )) {
                    return candidate;
                }
            }

            if (scan < FUNCTIONAL_SOURCE_MAX_SCROLLS) {
                smallSwipeUp();
            }
        }

        throw new AssertionError(
                entry.routeLabel
                        + " fund row was not physically located inside its Funds / SIFs card"
        );
    }

    private void ensureFunctionalSourceScreenReady(
            FunctionalSourceScreen sourceScreen,
            String context
    ) {
        /*
         * Fast probe only. Do NOT spend 45-60 seconds waiting for the requested tab
         * when Portfolio Analysis is already showing the other tab. If the requested
         * source is not already ready, validate the stable Portfolio Analysis shell
         * and select the requested tab immediately.
         */
        if (waitForFunctionalSourceScreenReady(sourceScreen, 3)) {
            return;
        }

        if (!waitForPortfolioAnalysisShellReady(FUNCTIONAL_CONTROL_WAIT_SECONDS)) {
            recoverFunctionalSourceThroughHub(sourceScreen, context);
            return;
        }

        openFunctionalSourceScreen(sourceScreen);
        if (!waitForFunctionalSourceScreenReady(sourceScreen, FUNCTIONAL_ROUTE_WAIT_SECONDS)) {
            throw new AssertionError(
                    functionalSourceDisplayName(sourceScreen)
                            + " did not reach the stable loaded state after explicit wait"
                            + " | context=" + context
            );
        }
    }

    private void openFunctionalSourceScreen(FunctionalSourceScreen sourceScreen) {
        if (!waitForPortfolioAnalysisShellReady(FUNCTIONAL_ROUTE_WAIT_SECONDS)) {
            throw new AssertionError(
                    "Portfolio Analysis shell did not become ready"
                            + " | required=Portfolio Analysis + Summary/Funds tabs + investor selector"
            );
        }

        if (waitForFunctionalSourceScreenReady(sourceScreen, 2)) {
            return;
        }

        WebElement tab = findPortfolioAnalysisTabElement(sourceScreen);
        if (tab == null) {
            throw new AssertionError(
                    "Requested Portfolio Analysis tab was not visible"
                            + " | tab=" + functionalSourceDisplayName(sourceScreen)
            );
        }

        tapElementCenter(tab);

        if (!waitForFunctionalSourceScreenReady(sourceScreen, FUNCTIONAL_ROUTE_WAIT_SECONDS)) {
            throw new AssertionError(
                    functionalSourceDisplayName(sourceScreen)
                            + " did not become fully loaded after selecting its tab"
            );
        }
    }

    private WebElement findPortfolioAnalysisTabElement(FunctionalSourceScreen sourceScreen) {
        By exact = sourceScreen == FunctionalSourceScreen.SUMMARY ? summaryTab : fundsSifsTab;
        By flexible = sourceScreen == FunctionalSourceScreen.SUMMARY
                ? summaryTabFlexible
                : fundsSifsTabFlexible;

        WebElement tab = findVisibleElement(exact);
        return tab != null ? tab : findVisibleElement(flexible);
    }

    private boolean isSummaryTabSemanticsVisible() {
        return isDisplayedNow(summaryTab) || isDisplayedNow(summaryTabFlexible);
    }

    private boolean isFundsSifsTabSemanticsVisible() {
        return isDisplayedNow(fundsSifsTab) || isDisplayedNow(fundsSifsTabFlexible);
    }

    /**
     * Stable Portfolio Analysis shell. Page identity deliberately does not depend
     * on the exact selected-investor accessibility string; My Investment - 5 is
     * validated separately by the investor-selection flow.
     */
    private boolean isPortfolioAnalysisShellReady() {
        return !isDisplayedNow(fundReviewHeader)
                && isDisplayedNow(portfolioAnalysisHeader)
                && isSummaryTabSemanticsVisible()
                && isFundsSifsTabSemanticsVisible()
                && isDisplayedNow(investorSelectionDropdown);
    }

    private boolean waitForPortfolioAnalysisShellReady(int timeoutSeconds) {
        return explicitWaitUntil(timeoutSeconds, this::isPortfolioAnalysisShellReady);
    }

    /**
     * Collapsed Summary is fully loaded only when the attention card AND lower
     * Summary content are both present. Current fund names/count are not required.
     */
    private boolean isSummarySourcePageLoaded() {
        if (isDisplayedNow(fundReviewHeader) || !isSummaryTabSemanticsVisible()) {
            return false;
        }

        WebElement attentionHeader = findVisibleElement(summaryAttentionHeader);
        if (attentionHeader == null || !isElementInsideCurrentViewport(attentionHeader)) {
            return false;
        }

        // This is a DIRECT-FUND flow. The collapsed Summary page is identified by
        // its selected tab, attention card and one stable lower Summary section.
        return isDisplayedNow(summaryOverallPortfolioPerformance)
                || isDisplayedNow(summaryPortfolioProfile);
    }

    /**
     * Expanded Summary attention list shown after tapping +XX more. The latest
     * Inspector capture exposes at least ABSL Corporate Bond, ABSL Liquid and
     * Axis ELSS simultaneously. Requiring three supplied rows distinguishes the
     * expanded list from the two-row Summary preview without depending on whether
     * Flutter keeps obscured tab semantics in the hierarchy.
     */
    private boolean isSummaryAttentionExpandedListReady() {
        /*
         * The expanded attention list is a modal bottom sheet. While that sheet is
         * open Flutter can keep the underlying Portfolio Analysis header in the
         * hierarchy but report it as not displayed/obscured. Therefore the parent
         * page header must NOT be part of the drawer-ready condition.
         *
         * The sheet itself is identified by its own heading plus at least three of
         * the supplied fund rows that cannot all be visible in the collapsed two-row
         * Summary preview. This matches the Inspector state supplied for the drawer.
         */
        return !isDisplayedNow(fundReviewHeader)
                && isDisplayedNow(summaryAttentionHeader)
                && countVisibleSuppliedExpandedAttentionRows() >= 3;
    }

    private int countVisibleSuppliedExpandedAttentionRows() {
        int visibleSuppliedRows = 0;
        By[] suppliedExpandedRows = {
                optimizeDirectEntryFund,
                optimizeSecondEntryFund,
                summaryAbslUltraShortEntryFund,
                summaryExpandedAxisElssFund,
                summaryExpandedMiraeLargeCapFund
        };

        for (By locator : suppliedExpandedRows) {
            WebElement row = findVisibleElement(locator);
            if (row != null && isElementInsideCurrentViewport(row)) {
                visibleSuppliedRows++;
            }
        }
        return visibleSuppliedRows;
    }

    private boolean waitForSummaryAttentionExpandedListReady(int timeoutSeconds) {
        final int[] consecutiveReadyPolls = {0};
        return explicitWaitUntil(timeoutSeconds, () -> {
            if (isSummaryAttentionExpandedListReady()) {
                consecutiveReadyPolls[0]++;
            } else {
                consecutiveReadyPolls[0] = 0;
            }
            return consecutiveReadyPolls[0] >= 3;
        });
    }

    /**
     * Funds/SIFs can restore at its prior scroll offset after Fund Review Back.
     * Any supplied category/header marker therefore proves the page identity.
     */
    private boolean isFundsSifsSourcePageLoaded() {
        if (isDisplayedNow(fundReviewHeader) || !isFundsSifsTabSemanticsVisible()) {
            return false;
        }

        if (isDisplayedNow(fundPortfolioPerformance)
                || isDisplayedNow(fundsSifsPortfolioInsights)) {
            return true;
        }

        By[] bucketHeaders = {
                exitCardHeader,
                optimizeCardHeader,
                goodCardHeader,
                steadyCardHeader,
                newFundsCardHeader
        };

        for (By header : bucketHeaders) {
            WebElement element = findVisibleElement(header);
            if (element != null && isElementInsideCurrentViewport(element)) {
                return true;
            }
        }

        return false;
    }

    private boolean isFunctionalSourceScreenReady(FunctionalSourceScreen sourceScreen) {
        return sourceScreen == FunctionalSourceScreen.SUMMARY
                ? isSummarySourcePageLoaded()
                : isFundsSifsSourcePageLoaded();
    }

    private boolean waitForFunctionalSourceScreenReady(
            FunctionalSourceScreen sourceScreen,
            int timeoutSeconds
    ) {
        final int[] consecutiveReadyPolls = {0};

        return explicitWaitUntil(
                timeoutSeconds,
                () -> {
                    if (isFunctionalSourceScreenReady(sourceScreen)) {
                        consecutiveReadyPolls[0]++;
                    } else {
                        consecutiveReadyPolls[0] = 0;
                    }
                    return consecutiveReadyPolls[0] >= 3;
                }
        );
    }

    private boolean waitForPortfolioAnalysisHeader(int timeoutSeconds) {
        return explicitWaitUntil(
                timeoutSeconds,
                () -> isDisplayedNow(portfolioAnalysisHeader)
        );
    }

    private WebElement locateFunctionalEntryControl(
            FunctionalSourceScreen sourceScreen,
            By exactControlLocator,
            By cardHeaderForMoreControl,
            String routeLabel
    ) {
        /*
         * +XX more controls are anchored to their NON-clickable category heading.
         * We never tap the heading. The exact supplied +XX locator is preferred,
         * but the dynamic header-derived value is used when portfolio counts change.
         */
        if (cardHeaderForMoreControl != null) {
            WebElement header = findFunctionalSourceElement(
                    sourceScreen,
                    cardHeaderForMoreControl,
                    routeLabel + " category header"
            );

            if (exactControlLocator != null) {
                WebElement exactMore = waitForFunctionalElementNearHeader(
                        exactControlLocator,
                        header,
                        FUNCTIONAL_SCROLL_CHECK_SECONDS
                );
                if (exactMore != null) {
                    return exactMore;
                }
            }

            WebElement visibleMore = findClosestVisibleMoreChevronBelowHeader(header);
            if (visibleMore != null) {
                return visibleMore;
            }

            By dynamicMore = deriveMoreChevronLocatorFromHeader(header, routeLabel);
            WebElement derived = waitForFunctionalElementNearHeader(
                    dynamicMore,
                    header,
                    FUNCTIONAL_CONTROL_WAIT_SECONDS
            );
            if (derived != null) {
                return derived;
            }

            throw new AssertionError(
                    routeLabel + " +XX more control was not available below its category header"
            );
        }

        if (exactControlLocator == null) {
            throw new AssertionError("No functional entry locator was supplied | route=" + routeLabel);
        }

        return findFunctionalSourceElement(
                sourceScreen,
                exactControlLocator,
                routeLabel + " fund row"
        );
    }

    private WebElement waitForFunctionalElementNearHeader(
            By locator,
            WebElement header,
            int timeoutSeconds
    ) {
        if (locator == null || header == null) {
            return null;
        }

        final WebElement[] match = new WebElement[1];
        boolean found = explicitWaitUntil(timeoutSeconds, () -> {
            WebElement candidate = findVisibleElement(locator);
            if (candidate == null || !isElementInsideCurrentViewport(candidate)) {
                return false;
            }
            if (!isControlBelowHeader(candidate, header)) {
                return false;
            }
            match[0] = candidate;
            return true;
        });
        return found ? match[0] : null;
    }

    private boolean isControlBelowHeader(WebElement candidate, WebElement header) {
        try {
            Rectangle headerRect = header.getRect();
            Rectangle candidateRect = candidate.getRect();
            int headerBottom = headerRect.getY() + headerRect.getHeight();
            int distance = candidateRect.getY() - headerBottom;
            int maxDistance = (int) (driver.manage().window().getSize().getHeight() * 0.24);
            return distance >= 0 && distance <= maxDistance;
        } catch (Exception e) {
            return false;
        }
    }

    private WebElement findClosestVisibleMoreChevronBelowHeader(WebElement header) {
        if (header == null) {
            return null;
        }

        try {
            Rectangle headerRect = header.getRect();
            int headerBottom = headerRect.getY() + headerRect.getHeight();
            int maxDistance = (int) (driver.manage().window().getSize().getHeight() * 0.24);
            WebElement closest = null;
            int closestDistance = Integer.MAX_VALUE;

            for (WebElement candidate : driver.findElements(anyMoreChevron)) {
                try {
                    if (candidate == null || !candidate.isDisplayed()) {
                        continue;
                    }

                    String description = safeAttribute(candidate, "content-desc");
                    if (description == null || !description.matches("\\+\\d+ more")) {
                        continue;
                    }

                    Rectangle rect = candidate.getRect();
                    int distance = rect.getY() - headerBottom;
                    if (distance >= 0
                            && distance <= maxDistance
                            && distance < closestDistance
                            && isElementInsideCurrentViewport(candidate)) {
                        closest = candidate;
                        closestDistance = distance;
                    }
                } catch (Exception ignored) {
                    // Flutter can replace one semantics node during a hierarchy refresh.
                }
            }
            return closest;
        } catch (Exception e) {
            return null;
        }
    }

    private By deriveMoreChevronLocatorFromHeader(
            WebElement header,
            String routeLabel
    ) {
        String headerText = getReadableText(header);
        int firstSpace = headerText.indexOf(' ');

        if (firstSpace <= 0) {
            throw new AssertionError(
                    "Unable to derive dynamic +XX more locator from card header"
                            + " | route=" + routeLabel
                            + " | header=" + quote(headerText)
            );
        }

        int totalCount;
        try {
            totalCount = Integer.parseInt(headerText.substring(0, firstSpace).trim());
        } catch (NumberFormatException e) {
            throw new AssertionError(
                    "Card header did not begin with a numeric count"
                            + " | route=" + routeLabel
                            + " | header=" + quote(headerText)
            );
        }

        int hiddenCount = totalCount - 2;
        if (hiddenCount <= 0) {
            throw new AssertionError(
                    "Card does not expose a +XX more control"
                            + " | route=" + routeLabel
                            + " | header=" + quote(headerText)
            );
        }

        return AppiumBy.accessibilityId("+" + hiddenCount + " more");
    }

    private WebElement findFunctionalSourceElement(
            FunctionalSourceScreen sourceScreen,
            By locator,
            String elementName
    ) {
        WebElement visible = waitForFunctionalElementInViewport(
                locator,
                sourceScreen == FunctionalSourceScreen.SUMMARY
                        ? FUNCTIONAL_CONTROL_WAIT_SECONDS
                        : FUNCTIONAL_SCROLL_CHECK_SECONDS
        );
        if (visible != null) {
            return visible;
        }

        if (sourceScreen == FunctionalSourceScreen.SUMMARY) {
            throw new AssertionError(
                    elementName + " was not visible on Summary after explicit wait; no scrolling was performed"
            );
        }

        for (int swipe = 1; swipe <= FUNCTIONAL_SOURCE_MAX_SCROLLS; swipe++) {
            smallSwipeUp();

            visible = waitForFunctionalElementInViewport(
                    locator,
                    FUNCTIONAL_SCROLL_CHECK_SECONDS
            );
            if (visible != null) {
                return visible;
            }
        }

        throw new AssertionError(
                elementName + " was not visible after bounded Funds / SIFs scrolling"
        );
    }

    private WebElement waitForFunctionalElementInViewport(By locator, int timeoutSeconds) {
        if (locator == null) {
            return null;
        }

        final WebElement[] match = new WebElement[1];
        boolean found = explicitWaitUntil(timeoutSeconds, () -> {
            WebElement candidate = findVisibleElement(locator);
            if (candidate == null || !isElementInsideCurrentViewport(candidate)) {
                return false;
            }
            match[0] = candidate;
            return true;
        });
        return found ? match[0] : null;
    }

    private boolean openFunctionalEntryAndWaitForReview(
            FunctionalSourceScreen sourceScreen,
            WebElement control,
            boolean isMoreControl,
            FunctionalBucket expectedBucket
    ) {
        if (control == null) {
            return false;
        }

        // Primary action is a W3C gesture. Fund rows use the right-side lead-in;
        // +XX more controls use their own center.
        WebElement fresh = refreshByAccessibilityDescription(control);
        if (fresh == null) {
            fresh = control;
        }

        if (isMoreControl) {
            tapElementCenter(fresh);
        } else {
            tapElementByPercent(fresh, 0.90, 0.50);
        }

        if (waitForExpectedFunctionalFundReview(expectedBucket, FUNCTIONAL_ROUTE_WAIT_SECONDS)) {
            return true;
        }

        // Silent Flutter tap misses do not throw. Retry once with a fresh semantics
        // node only if the original source page is still present.
        if (isFunctionalSourceScreenReady(sourceScreen)) {
            fresh = refreshByAccessibilityDescription(control);
            if (fresh != null) {
                try {
                    fresh.click();
                } catch (Exception clickFailure) {
                    if (isMoreControl) {
                        tapElementCenter(fresh);
                    } else {
                        tapElementByPercent(fresh, 0.82, 0.50);
                    }
                }
                return waitForExpectedFunctionalFundReview(
                        expectedBucket,
                        FUNCTIONAL_ROUTE_WAIT_SECONDS
                );
            }
        }

        return false;
    }

    /**
     * Direct fund-name click only.
     *
     * The project's WaitUtils is used first for clickable fund rows. WaitUtils is
     * backed by the framework's short-poll WebDriverWait/FluentWait behaviour, so
     * the tap happens as soon as Flutter marks the row clickable instead of waiting
     * for an arbitrary sleep. A W3C CENTER tap remains the bounded fallback for a
     * stale/ignored Flutter semantics click.
     *
     * The previous 90%-width tap is intentionally not used so Summary's delete/X
     * area can never be hit accidentally.
     */
    private boolean openDirectFundNameAndWaitForFundReview(
            FunctionalSourceScreen sourceScreen,
            WebElement control
    ) {
        if (control == null) {
            return false;
        }

        WebElement fresh = refreshByAccessibilityDescription(control);
        if (fresh == null) {
            fresh = control;
        }

        String accessibilityDescription = safeAttribute(fresh, "content-desc");
        boolean frameworkClickDone = false;

        if (accessibilityDescription != null && !accessibilityDescription.trim().isEmpty()) {
            try {
                WebElement clickable = WaitUtils.waitForClickable(
                        driver,
                        AppiumBy.accessibilityId(accessibilityDescription.trim()),
                        4
                );
                clickable.click();
                fresh = clickable;
                frameworkClickDone = true;
            } catch (Exception frameworkClickFailure) {
                diagnostic(
                        "Framework fluent-style clickable wait/click was not usable; "
                                + "falling back to W3C centre tap"
                                + " | locator=" + accessibilityDescription
                                + " | error=" + safeMessage(frameworkClickFailure)
                );
            }
        }

        if (!frameworkClickDone) {
            tapElementCenter(fresh);
        }

        if (waitForFunctionalFundReviewIdentity(FUNCTIONAL_TAP_CONFIRM_WAIT_SECONDS)) {
            return true;
        }

        // Retry only if the source page is still genuinely present. This avoids
        // double-tapping during a slow but successful Flutter route transition.
        if (isFunctionalSourceScreenReady(sourceScreen)) {
            fresh = refreshByAccessibilityDescription(control);
            if (fresh != null) {
                tapElementCenter(fresh);
            }
        }

        return waitForFunctionalFundReviewIdentity(FUNCTIONAL_ROUTE_WAIT_SECONDS);
    }

    private boolean waitForFunctionalFundReviewIdentity(int timeoutSeconds) {
        return explicitWaitUntil(
                timeoutSeconds,
                () -> isDisplayedNow(fundReviewHeader)
                        && isDisplayedNow(fundReviewGoBackButton)
        );
    }

    /**
     * Page navigation, bucket correctness and investor correctness are deliberately
     * validated as separate states. This prevents a late investor/bucket semantics
     * node from being misreported as a failed fund-name click.
     */
    private void verifyFunctionalFundReviewContext(
            FunctionalBucket expectedBucket,
            String routeLabel
    ) {
        if (expectedBucket != null
                && !explicitWaitUntil(
                        FUNCTIONAL_CONTROL_WAIT_SECONDS,
                        () -> isFunctionalBucketOpen(expectedBucket)
                )) {
            throw new AssertionError(
                    "Fund Review opened but the expected bucket was not active"
                            + " | route=" + routeLabel
                            + " | expectedBucket=" + functionalBucketDisplayName(expectedBucket)
            );
        }

        if (!explicitWaitUntil(
                FUNCTIONAL_CONTROL_WAIT_SECONDS,
                () -> isDisplayedNow(fundReviewInvestment5Context)
        )) {
            throw new AssertionError(
                    "Fund Review opened but investor context was not My Investment - 5"
                            + " | route=" + routeLabel
            );
        }
    }

    private WebElement refreshByAccessibilityDescription(WebElement original) {
        if (original == null) {
            return null;
        }

        try {
            String description = safeAttribute(original, "content-desc");
            if (description == null || description.trim().isEmpty()) {
                return null;
            }
            return findVisibleElement(AppiumBy.accessibilityId(description.trim()));
        } catch (Exception e) {
            return null;
        }
    }

    private boolean waitForExpectedFunctionalFundReview(
            FunctionalBucket expectedBucket,
            int timeoutSeconds
    ) {
        return explicitWaitUntil(timeoutSeconds, () -> {
            boolean reviewReady = isDisplayedNow(fundReviewHeader);
            boolean backReady = isDisplayedNow(fundReviewGoBackButton);
            boolean bucketReady = expectedBucket == null || isFunctionalBucketOpen(expectedBucket);
            boolean investorReady = isDisplayedNow(fundReviewInvestment5Context);
            return reviewReady && backReady && bucketReady && investorReady;
        });
    }

    private boolean isFunctionalBucketOpen(FunctionalBucket bucket) {
        switch (bucket) {
            case EXIT:
                return isDisplayedNow(exitReviewHeader);
            case OPTIMIZE:
                return isDisplayedNow(optimizeReviewHeader);
            case GOOD:
                return isDisplayedNow(goodReviewHeader);
            case STEADY:
                return isDisplayedNow(steadyReviewHeader);
            case NEW_FUND:
                return isDisplayedNow(newFundReviewHeader);
            default:
                return false;
        }
    }

    private void returnFromFundReviewToFunctionalSource(
            FunctionalSourceScreen sourceScreen,
            String routeLabel
    ) {
        if (!isDisplayedNow(fundReviewHeader)
                && waitForFunctionalSourceScreenReady(sourceScreen, 2)) {
            return;
        }

        /* Primary path: exact accessibility-id "Go back" supplied from Inspector. */
        boolean semanticBackWorked = tapFundReviewGoBackButton();
        if (semanticBackWorked
                && waitForFunctionalSourceScreenReady(
                        sourceScreen,
                        FUNCTIONAL_CONTROL_WAIT_SECONDS
                )) {
            return;
        }

        /*
         * If Fund Review disappeared but Portfolio Analysis is still rebuilding,
         * never press Android BACK again because that could leave Portfolio Analysis.
         * Re-select the expected source tab once the shell is available.
         */
        if (!isDisplayedNow(fundReviewHeader)) {
            if (waitForPortfolioAnalysisShellReady(12)) {
                openFunctionalSourceScreen(sourceScreen);
                if (waitForFunctionalSourceScreenReady(
                        sourceScreen,
                        FUNCTIONAL_CONTROL_WAIT_SECONDS
                )) {
                    return;
                }
            }
        } else {
            // Android BACK is a single bounded fallback only when still in Fund Review.
            pressBackSafely();
            if (waitForFunctionalSourceScreenReady(
                    sourceScreen,
                    FUNCTIONAL_CONTROL_WAIT_SECONDS
            )) {
                return;
            }
        }

        ReportLogger.step(
                "[RECOVERY] " + routeLabel
                        + " | Go back did not restore "
                        + functionalSourceDisplayName(sourceScreen)
                        + " | action=reopen Portfolio Analysis from Hub"
        );
        recoverFunctionalSourceThroughHub(sourceScreen, routeLabel + " return path");
    }

    /**
     * Best-effort cleanup used only after a route assertion fails inside Fund Review.
     * It deliberately avoids Hub/app recovery; the caller owns that retry policy.
     */
    private boolean tryReturnFromFundReviewToSourceWithoutRecovery(
            FunctionalSourceScreen sourceScreen
    ) {
        if (waitForFunctionalSourceScreenReady(sourceScreen, 1)) {
            return true;
        }

        if (isDisplayedNow(fundReviewHeader) || isDisplayedNow(fundReviewGoBackButton)) {
            if (tapFundReviewGoBackButton()
                    && waitForFunctionalSourceScreenReady(sourceScreen, 12)) {
                return true;
            }
        }

        if (isDisplayedNow(fundReviewHeader)) {
            pressBackSafely();
            return waitForFunctionalSourceScreenReady(sourceScreen, 12);
        }

        if (waitForPortfolioAnalysisShellReady(6)) {
            try {
                openFunctionalSourceScreen(sourceScreen);
                return waitForFunctionalSourceScreenReady(sourceScreen, 12);
            } catch (Throwable ignored) {
                return false;
            }
        }

        return false;
    }

    private boolean tapFundReviewGoBackButton() {
        WebElement back = waitForFunctionalElementInViewport(fundReviewGoBackButton, 8);
        if (back == null) {
            diagnostic("Fund Review Go back accessibility button was not visible");
            return false;
        }

        diagnostic(
                "Tapping Fund Review Go back with W3C centre tap"
                        + " | bounds=" + rectangleToString(back.getRect())
                        + " | clickable=" + safeAttribute(back, "clickable")
        );
        tapElementCenter(back);

        if (explicitWaitUntil(
                6,
                () -> !isDisplayedNow(fundReviewHeader)
                        || isPortfolioAnalysisShellReady()
        )) {
            return true;
        }

        /*
         * Flutter can occasionally keep the first semantics node while ignoring its
         * touch. Re-acquire the same supplied "Go back" node and try one more W3C
         * centre tap. This retry happens only while Fund Review is still visible, so
         * a successful first tap can never become an accidental second Back.
         */
        if (isDisplayedNow(fundReviewHeader)) {
            WebElement refreshedBack = waitForFunctionalElementInViewport(
                    fundReviewGoBackButton,
                    3
            );
            if (refreshedBack != null) {
                tapElementCenter(refreshedBack);
                return explicitWaitUntil(
                        8,
                        () -> !isDisplayedNow(fundReviewHeader)
                                || isPortfolioAnalysisShellReady()
                );
            }
        }

        return !isDisplayedNow(fundReviewHeader);
    }

    private void recoverFunctionalSourceThroughHub(
            FunctionalSourceScreen sourceScreen,
            String context
    ) {
        functionalHubRecoveryCount++;

        try {
            openHubFromBottomNavigationForPortfolioAnalysisFa();
            openPortfolioAnalysisFromHubForPortfolioAnalysisFa();
            selectInvestment5ForPortfolioAnalysisFa();
            openFunctionalSourceScreen(sourceScreen);

            if (!waitForFunctionalSourceScreenReady(sourceScreen, FUNCTIONAL_ROUTE_WAIT_SECONDS)) {
                throw new AssertionError("Portfolio Analysis source page did not recover from Hub");
            }
        } catch (Throwable recoveryFailure) {
            throw new FunctionalRecoveryException(
                    "Hub recovery failed"
                            + " | context=" + context
                            + " | error=" + safeThrowableMessage(recoveryFailure),
                    recoveryFailure
            );
        }
    }

    private void restartAdvisorAppAndRestoreFunctionalSource(
            FunctionalSourceScreen sourceScreen,
            String context
    ) {
        functionalAppRestartCount++;
        restartAdvisorAppInCurrentDriverSession();

        verifiedAuthenticatedSessionId = null;
        ensureAdvisorAppLoggedInForPortfolioAnalysisFa();
        openHubFromBottomNavigationForPortfolioAnalysisFa();
        openPortfolioAnalysisFromHubForPortfolioAnalysisFa();
        selectInvestment5ForPortfolioAnalysisFa();
        openFunctionalSourceScreen(sourceScreen);

        if (!waitForFunctionalSourceScreenReady(sourceScreen, FUNCTIONAL_ROUTE_WAIT_SECONDS)) {
            throw new AssertionError(
                    "Portfolio Analysis source page was not restored after full app restart"
                            + " | context=" + context
            );
        }
    }

    /**
     * Public recovery hook used by the existing bucket-navigation wrapper.
     */
    public void restartAdvisorAppForPortfolioAnalysisFa() {
        restartAdvisorAppInCurrentDriverSession();
        verifiedAuthenticatedSessionId = null;
    }

    private void restartAdvisorAppInCurrentDriverSession() {
        Map<String, Object> params = new HashMap<>();
        params.put("appId", ADVISOR_APP_PACKAGE);

        try {
            driver.executeScript("mobile: terminateApp", params);
        } catch (Exception terminateError) {
            diagnostic("Advisor app terminate command reported: " + safeMessage(terminateError));
        }

        try {
            driver.executeScript("mobile: activateApp", params);
        } catch (Exception activateError) {
            throw new AssertionError(
                    "Unable to activate Advisor app after restart"
                            + " | error=" + safeMessage(activateError)
            );
        }

        if (!explicitWaitUntil(
                FUNCTIONAL_ROUTE_WAIT_SECONDS,
                () -> isDisplayedNow(PIN_SCREEN_MARKER)
                        || isPortfolioAnalysisFaAuthenticatedStateVisible()
        )) {
            throw new AssertionError(
                    "Advisor app was activated but no known PIN/authenticated UI state became ready"
            );
        }
    }

    private boolean explicitWaitUntil(int timeoutSeconds, FunctionalCondition condition) {
        if (condition == null) {
            return false;
        }

        try {
            WebDriverWait wait = new WebDriverWait(
                    driver,
                    Duration.ofSeconds(Math.max(1, timeoutSeconds))
            );
            wait.pollingEvery(Duration.ofMillis(250));
            return Boolean.TRUE.equals(
                    wait.until(ignored -> {
                        try {
                            return condition.evaluate();
                        } catch (Throwable transientUiFailure) {
                            return false;
                        }
                    })
            );
        } catch (Exception timeoutOrTransientFailure) {
            return false;
        }
    }

    private String functionalSourceDisplayName(FunctionalSourceScreen sourceScreen) {
        return sourceScreen == FunctionalSourceScreen.SUMMARY
                ? "Summary"
                : "Funds / SIFs";
    }

    @FunctionalInterface
    private interface FunctionalCondition {
        boolean evaluate();
    }

    private static final class FunctionalEntrySpec {
        private final FunctionalSourceScreen sourceScreen;
        private final String routeLabel;
        private final By exactControlLocator;
        private final By cardHeaderForMoreControl;
        private final FunctionalBucket expectedBucket;

        private FunctionalEntrySpec(
                FunctionalSourceScreen sourceScreen,
                String routeLabel,
                By exactControlLocator,
                By cardHeaderForMoreControl,
                FunctionalBucket expectedBucket
        ) {
            this.sourceScreen = sourceScreen;
            this.routeLabel = routeLabel;
            this.exactControlLocator = exactControlLocator;
            this.cardHeaderForMoreControl = cardHeaderForMoreControl;
            this.expectedBucket = expectedBucket;
        }
    }

    private String functionalBucketDisplayName(FunctionalBucket bucket) {
        if (bucket == null) {
            return "Fund Review";
        }

        switch (bucket) {
            case EXIT:
                return "Fund Review > Exit";
            case OPTIMIZE:
                return "Fund Review > Optimize";
            case GOOD:
                return "Fund Review > Good";
            case STEADY:
                return "Fund Review > Steady";
            case NEW_FUND:
                return "Fund Review > New Fund";
            default:
                return "Fund Review";
        }
    }

    private String quote(String value) {
        return "\"" + (value == null ? "" : value) + "\"";
    }

    private String safeThrowableMessage(Throwable error) {
        if (error == null || error.getMessage() == null) {
            return "unknown error";
        }

        return error.getMessage()
                .replace('\n', ' ')
                .replace('\r', ' ')
                .trim();
    }

    private enum FunctionalSourceScreen {
        SUMMARY,
        FUNDS_SIFS
    }

    private enum FunctionalBucket {
        EXIT,
        OPTIMIZE,
        GOOD,
        STEADY,
        NEW_FUND
    }

    private static final class FunctionalRecoveryException extends RuntimeException {
        private FunctionalRecoveryException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    public static final class EntryPointSummary {
        private final int totalEntryPoints;
        private final int openedEntryPoints;
        private final int hubRecoveries;
        private final int appRestarts;

        public EntryPointSummary(
                int totalEntryPoints,
                int openedEntryPoints,
                int hubRecoveries,
                int appRestarts
        ) {
            this.totalEntryPoints = totalEntryPoints;
            this.openedEntryPoints = openedEntryPoints;
            this.hubRecoveries = hubRecoveries;
            this.appRestarts = appRestarts;
        }

        public int getTotalEntryPoints() {
            return totalEntryPoints;
        }

        public int getOpenedEntryPoints() {
            return openedEntryPoints;
        }

        public int getHubRecoveries() {
            return hubRecoveries;
        }

        public int getAppRestarts() {
            return appRestarts;
        }

        public boolean allOpened() {
            return openedEntryPoints == totalEntryPoints;
        }
    }

    // =========================================================
    // CLEANUP
    // =========================================================

    /**
     * Final combined-suite state requested by the team: leave the Advisor app on Hub.
     *
     * Reuse the same proven authenticated navigation unwind used by every bucket test:
     * Fund Review uses the supplied accessibility-id "Go back" first, intermediate
     * screens are unwound only as needed, and the Hub bottom-tab click itself goes
     * through ActionUtils -> WaitUtils.waitForClickable (framework polling wait).
     */
    public boolean returnToHubHomeForPortfolioAnalysisFa() {
        diagnostic("Final cleanup: returning app to Hub home");

        try {
            openHubFromBottomNavigationForPortfolioAnalysisFa();
        } catch (Throwable cleanupFailure) {
            diagnostic(
                    "Final Hub cleanup navigation failed"
                            + " | error=" + safeThrowableMessage(cleanupFailure)
            );
            return false;
        }

        boolean ready = explicitWaitUntil(
                20,
                () -> isDisplayedNow(HUB_READY_MARKER)
                        && isDisplayedNow(HUB_BOTTOM_TAB)
        );

        if (ready) {
            diagnostic("Final state confirmed: Hub home");
        }
        return ready;
    }

    public void returnToFundsTabForPortfolioAnalysisFa() {
        if (!returnToFundsHomeForPortfolioAnalysisFa()) {
            throw new AssertionError("Unable to return to Funds home");
        }
    }

    /**
     * Final combined-suite state requested by QA: leave the Advisor app on the Funds
     * bottom-navigation home screen after every bucket regression has finished.
     *
     * If cleanup starts inside Fund Review, use the supplied "Go back" semantics first.
     * Generic Android Back is used only to unwind non-Fund-Review pages/overlays until
     * bottom navigation is exposed.  The Funds tab is then clicked and Funds-home content
     * is explicitly confirmed.
     */
    public boolean returnToFundsHomeForPortfolioAnalysisFa() {
        diagnostic("Final cleanup: returning app to Funds home");

        if (isFundsHomeReady()) {
            diagnostic("Funds home is already the final screen");
            return true;
        }

        for (int attempt = 1; attempt <= 7; attempt++) {
            if (isDisplayedNow(FUNDS_BOTTOM_TAB)) {
                try {
                    ActionUtils.click(driver, FUNDS_BOTTOM_TAB);
                } catch (Exception clickFailure) {
                    WebElement fundsTab = findVisibleElement(FUNDS_BOTTOM_TAB);
                    if (fundsTab != null) {
                        tapElementCenter(fundsTab);
                    }
                }

                if (explicitWaitUntil(45, this::isFundsHomeReady)) {
                    settleAfterPageOpen("Funds home");
                    diagnostic("Final state confirmed: Funds home");
                    return true;
                }
            }

            /*
             * Fund Review has a supplied app-level back control.  Prefer it over Android
             * Back so the route is unwound through the app's intended navigation path.
             */
            if (isDisplayedNow(fundReviewHeader) || isDisplayedNow(fundReviewGoBackButton)) {
                if (!tapFundReviewGoBackButton() && isDisplayedNow(fundReviewHeader)) {
                    pressBackSafely();
                }
            } else if (isInvestorDrawerOpen()) {
                closeInvestorDrawerByDraggingDown();
            } else {
                pressBackSafely();
            }

            settleAfterNavigation();

            if (isFundsHomeReady()) {
                diagnostic("Final state confirmed after navigation unwind: Funds home");
                return true;
            }
        }

        /* One final deterministic Funds-tab selection if bottom navigation is exposed. */
        if (isDisplayedNow(FUNDS_BOTTOM_TAB)) {
            WebElement fundsTab = findVisibleElement(FUNDS_BOTTOM_TAB);
            if (fundsTab != null) {
                tapElementCenter(fundsTab);
            }
            if (explicitWaitUntil(60, this::isFundsHomeReady)) {
                settleAfterPageOpen("Funds home");
                return true;
            }
        }

        return false;
    }

    private boolean isFundsHomeReady() {
        return isDisplayedNow(FUNDS_BOTTOM_TAB)
                && isDisplayedNow(FUNDS_HOME_ATTENTION_CARD);
    }

    // =========================================================
    // INVESTOR DRAWER HELPERS
    // =========================================================

    private boolean isInvestorDrawerOpen() {
        return isVisibleWithin(chooseInvestorTitle, 1);
    }

    private void closeInvestorDrawerByDraggingDown() {
        WebElement title = WaitUtils.waitForElement(driver, chooseInvestorTitle, 5);
        Rectangle titleRect = title.getRect();
        Dimension size = driver.manage().window().getSize();

        int startX = size.getWidth() / 2;
        int startY = titleRect.getY() - 90;
        int minimumStartY = (int) (size.getHeight() * 0.40);
        startY = Math.max(minimumStartY, startY);
        int endY = (int) (size.getHeight() * 0.93);

        diagnostic(
                "Dragging Choose Investor drawer down"
                        + " | startX=" + startX
                        + " | startY=" + startY
                        + " | endY=" + endY
        );

        swipeByCoordinates(startX, startY, startX, endY, 700);

        if (!WaitUtils.waitForInvisible(driver, chooseInvestorTitle, 8)) {
            diagnostic(
                    "First drawer drag did not dismiss sheet. Retrying with longer downward drag."
            );

            int secondStartY = Math.max(1, titleRect.getY() - 120);

            swipeByCoordinates(
                    startX,
                    secondStartY,
                    startX,
                    (int) (size.getHeight() * 0.97),
                    850
            );

            if (!WaitUtils.waitForInvisible(driver, chooseInvestorTitle, 8)) {
                throw new AssertionError(
                        "Choose Investor drawer did not close after two drag-down attempts"
                );
            }
        }

        settleAfterPageOpen("Portfolio Analysis after closing investor drawer");
        diagnostic("Choose Investor drawer closed successfully");
    }

    // =========================================================
    // CALCULATION DRAWER
    // =========================================================

    private void verifyDrawerTitleIsVisibleInBottomSheet() {
        Dimension size = driver.manage().window().getSize();
        int drawerThresholdY = (int) (size.getHeight() * 0.45);
        List<WebElement> titles = driver.findElements(howIsThisCalculated);

        for (WebElement title : titles) {
            try {
                if (title.isDisplayed()
                        && title.getRect().getY() >= drawerThresholdY) {
                    diagnostic(
                            "Verified How is this calculated? title inside bottom drawer"
                    );
                    return;
                }
            } catch (Exception ignored) {
                // Flutter semantics can refresh during animation.
            }
        }

        throw new AssertionError(
                "How is this calculated? drawer title was not visible inside bottom-sheet area"
        );
    }

    // =========================================================
    // ELEMENT HELPERS
    // =========================================================

    private WebElement findVisibleElement(By locator) {
        try {
            List<WebElement> elements = driver.findElements(locator);

            for (WebElement element : elements) {
                try {
                    if (element != null
                            && element.isDisplayed()
                            && element.getRect().getWidth() > 0
                            && element.getRect().getHeight() > 0) {
                        return element;
                    }
                } catch (Exception ignored) {
                    // Ignore stale Flutter semantics nodes.
                }
            }
        } catch (Exception e) {
            diagnostic(
                    "Visible element lookup failed: " + safeMessage(e)
            );
        }

        return null;
    }

    private boolean isVisibleWithin(By locator, int seconds) {
        try {
            WaitUtils.waitForElement(driver, locator, seconds);
            return true;
        } catch (Exception e) {
            return false;
        }
    }


    /**
     * Final-suite cleanup. Fund Review uses the supplied accessibility-id "Go back"
     * as the primary navigation path (W3C centre tap because clickable=false). Android
     * Back is reserved for overlays/fallback. Final destination remains Funds / SIFs.
     */
    public boolean returnToPortfolioAnalysisFundsUsingBackNavigationForPortfolioAnalysisFa() {
        if (waitForFunctionalSourceScreenReady(FunctionalSourceScreen.FUNDS_SIFS, 2)) {
            return true;
        }

        for (int attempt = 1; attempt <= 4; attempt++) {
            if (isDisplayedNow(fundReviewHeader)) {
                if (!tapFundReviewGoBackButton()) {
                    pressBackSafely();
                }
            } else if (isSummaryAttentionExpandedListReady()) {
                pressBackSafely();
            } else if (waitForFunctionalSourceScreenReady(FunctionalSourceScreen.SUMMARY, 2)) {
                try {
                    openFunctionalSourceScreen(FunctionalSourceScreen.FUNDS_SIFS);
                    return waitForFunctionalSourceScreenReady(
                            FunctionalSourceScreen.FUNDS_SIFS,
                            FUNCTIONAL_CONTROL_WAIT_SECONDS
                    );
                } catch (Throwable switchFailure) {
                    diagnostic(
                            "Final cleanup could not switch Summary to Funds/SIFs: "
                                    + safeThrowableMessage(switchFailure)
                    );
                }
            } else {
                pressBackSafely();
            }

            if (waitForFunctionalSourceScreenReady(FunctionalSourceScreen.FUNDS_SIFS, 12)) {
                return true;
            }

            if (waitForFunctionalSourceScreenReady(FunctionalSourceScreen.SUMMARY, 2)) {
                try {
                    openFunctionalSourceScreen(FunctionalSourceScreen.FUNDS_SIFS);
                    if (waitForFunctionalSourceScreenReady(
                            FunctionalSourceScreen.FUNDS_SIFS,
                            FUNCTIONAL_CONTROL_WAIT_SECONDS
                    )) {
                        return true;
                    }
                } catch (Throwable ignored) {
                    // Continue bounded cleanup.
                }
            }

            if (isDisplayedNow(HUB_READY_MARKER)) {
                return false;
            }
        }

        return waitForFunctionalSourceScreenReady(FunctionalSourceScreen.FUNDS_SIFS, 2);
    }

    private boolean isPortfolioAnalysisFundsPageReady() {
        return isFundsSifsSourcePageLoaded();
    }

    private boolean isDisplayedNow(By locator) {
        return findVisibleElement(locator) != null;
    }

    private String getReadableText(WebElement element) {
        if (element == null) {
            return "";
        }

        String contentDesc = safeAttribute(element, "content-desc");

        if (contentDesc != null && !contentDesc.trim().isEmpty()) {
            return contentDesc.trim();
        }

        try {
            String text = element.getText();
            return text == null ? "" : text.trim();
        } catch (Exception e) {
            return "";
        }
    }

    private String safeAttribute(WebElement element, String attribute) {
        try {
            String value = element.getAttribute(attribute);
            return value == null ? "" : value.trim();
        } catch (Exception e) {
            return "";
        }
    }

    private String rectangleToString(Rectangle rect) {
        if (rect == null) {
            return "null";
        }

        return "[x=" + rect.getX()
                + ", y=" + rect.getY()
                + ", width=" + rect.getWidth()
                + ", height=" + rect.getHeight()
                + "]";
    }

    // =========================================================
    // SCROLLING
    // =========================================================

    private void scrollUntilVisible(
            By targetLocator,
            String elementName,
            int maxAttempts
    ) {
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            if (isVisibleWithin(targetLocator, 2)) {
                diagnostic(elementName + " is visible");
                return;
            }

            diagnostic(
                    "Scrolling to find " + elementName
                            + " | Attempt " + attempt
                            + " of " + maxAttempts
            );

            if (attempt <= 2) {
                smallSwipeUp();
            } else {
                swipeUp();
            }

            settleAfterScroll();
        }

        if (isVisibleWithin(targetLocator, 3)) {
            diagnostic(elementName + " is visible after scrolling");
            return;
        }

        throw new AssertionError(
                elementName + " was not visible after bounded scrolling"
        );
    }

    // =========================================================
    // W3C TOUCH
    // =========================================================

    private void tapElementCenter(WebElement element) {
        tapElementByPercent(element, 0.50, 0.50);
    }

    private void tapElementByPercent(
            WebElement element,
            double xPercent,
            double yPercent
    ) {
        if (element == null) {
            throw new AssertionError("Cannot tap null WebElement");
        }

        Rectangle rect = element.getRect();

        int x = rect.getX() + (int) (rect.getWidth() * xPercent);
        int y = rect.getY() + (int) (rect.getHeight() * yPercent);

        tapByCoordinates(x, y);
    }

    private void tapByCoordinates(int x, int y) {
        Dimension screen = driver.manage().window().getSize();

        int safeX = Math.max(1, Math.min(x, screen.getWidth() - 2));
        int safeY = Math.max(1, Math.min(y, screen.getHeight() - 2));

        diagnostic(
                "Performing W3C tap | x=" + safeX + " | y=" + safeY
        );

        PointerInput finger =
                new PointerInput(PointerInput.Kind.TOUCH, "finger");

        Sequence tap = new Sequence(finger, 1);

        tap.addAction(
                finger.createPointerMove(
                        Duration.ZERO,
                        PointerInput.Origin.viewport(),
                        safeX,
                        safeY
                )
        );

        tap.addAction(
                finger.createPointerDown(
                        PointerInput.MouseButton.LEFT.asArg()
                )
        );

        tap.addAction(
                finger.createPointerMove(
                        Duration.ofMillis(120),
                        PointerInput.Origin.viewport(),
                        safeX,
                        safeY
                )
        );

        tap.addAction(
                finger.createPointerUp(
                        PointerInput.MouseButton.LEFT.asArg()
                )
        );

        driver.perform(Collections.singletonList(tap));
    }

    private void swipeByCoordinates(
            int startX,
            int startY,
            int endX,
            int endY,
            long durationMillis
    ) {
        PointerInput finger =
                new PointerInput(PointerInput.Kind.TOUCH, "finger");

        Sequence swipe = new Sequence(finger, 1);

        swipe.addAction(
                finger.createPointerMove(
                        Duration.ZERO,
                        PointerInput.Origin.viewport(),
                        startX,
                        startY
                )
        );

        swipe.addAction(
                finger.createPointerDown(
                        PointerInput.MouseButton.LEFT.asArg()
                )
        );

        swipe.addAction(
                finger.createPointerMove(
                        Duration.ofMillis(durationMillis),
                        PointerInput.Origin.viewport(),
                        endX,
                        endY
                )
        );

        swipe.addAction(
                finger.createPointerUp(
                        PointerInput.MouseButton.LEFT.asArg()
                )
        );

        driver.perform(Collections.singletonList(swipe));
    }

    /**
     * Legacy helper retained for compatibility with older internal calls. The
     * movement now matches the card-by-card scanner and is intentionally smooth.
     */
    private void overlapSearchSwipeUp() {
        cardByCardSearchSwipeUp();
    }

    /**
     * Precision movement retained for any local positioning fallback.
     */
    private void precisionSearchSwipeUp() {
        Dimension size = driver.manage().window().getSize();

        swipeByCoordinates(
                (int) (size.getWidth() * 0.50),
                (int) (size.getHeight() * 0.64),
                (int) (size.getWidth() * 0.50),
                (int) (size.getHeight() * 0.59),
                700
        );
    }

    /**
     * Legacy reverse helper retained for compatibility.
     */
    private void overlapSearchSwipeDown() {
        cardByCardSearchSwipeDown();
    }

    /**
     * Main Optimize regression scroll: one controlled micro-step at a time, followed
     * by a locator check for the exact next fund heading. The distance is deliberately
     * unchanged from the stable flow; only gesture/settle timing is moderately faster.
     * This keeps the ordered checkpoint and reverse-recovery guarantees intact.
     */
    private void cardByCardSearchSwipeUp() {
        Dimension size = driver.manage().window().getSize();

        swipeByCoordinates(
                (int) (size.getWidth() * 0.50),
                (int) (size.getHeight() * 0.72),
                (int) (size.getWidth() * 0.50),
                (int) (size.getHeight() * 0.62),
                OPTIMIZE_CARD_SCROLL_DURATION_MS
        );
    }

    /**
     * Exact reverse of the card-by-card downward movement. Used only when the
     * scanner detects that the expected next fund heading was passed.
     */
    private void cardByCardSearchSwipeDown() {
        Dimension size = driver.manage().window().getSize();

        swipeByCoordinates(
                (int) (size.getWidth() * 0.50),
                (int) (size.getHeight() * 0.42),
                (int) (size.getWidth() * 0.50),
                (int) (size.getHeight() * 0.52),
                OPTIMIZE_CARD_SCROLL_DURATION_MS
        );
    }

    /**
     * Once a fund heading is found, this slightly larger but still controlled
     * movement brings that SAME heading from the lower edge toward the upper
     * viewport. It is never used to search for an unknown fund.
     */
    private void checkpointAlignmentSwipeUp() {
        Dimension size = driver.manage().window().getSize();

        swipeByCoordinates(
                (int) (size.getWidth() * 0.50),
                (int) (size.getHeight() * 0.74),
                (int) (size.getWidth() * 0.50),
                (int) (size.getHeight() * 0.60),
                880
        );
    }

    private void checkpointAlignmentSwipeDown() {
        Dimension size = driver.manage().window().getSize();

        swipeByCoordinates(
                (int) (size.getWidth() * 0.50),
                (int) (size.getHeight() * 0.40),
                (int) (size.getWidth() * 0.50),
                (int) (size.getHeight() * 0.52),
                820
        );
    }

    /**
     * Small upward movement after the target fund is already found. It exposes
     * that same card's lower calculation controls without throwing away the anchor.
     */
    private void gentleSearchSwipeUp() {
        Dimension size = driver.manage().window().getSize();

        swipeByCoordinates(
                (int) (size.getWidth() * 0.50),
                (int) (size.getHeight() * 0.62),
                (int) (size.getWidth() * 0.50),
                (int) (size.getHeight() * 0.54),
                520
        );
    }

    private void settleAfterSearchScroll() {
        // Flutter semantics often lag behind the visual movement.
        sleep(SEARCH_SCROLL_SETTLE_MS);
    }

    private void swipeUp() {
        Dimension size = driver.manage().window().getSize();

        swipeByCoordinates(
                (int) (size.getWidth() * 0.50),
                (int) (size.getHeight() * 0.78),
                (int) (size.getWidth() * 0.50),
                (int) (size.getHeight() * 0.30),
                650
        );
    }

    private void smallSwipeUp() {
        Dimension size = driver.manage().window().getSize();

        swipeByCoordinates(
                (int) (size.getWidth() * 0.50),
                (int) (size.getHeight() * 0.65),
                (int) (size.getWidth() * 0.50),
                (int) (size.getHeight() * 0.45),
                500
        );
    }

    /*
     * Target-driven bucket search gestures. These are fixed, small W3C movements;
     * their distance never depends on an off-screen Flutter node.
     */
    private void goodSmallFixedSwipeUp() {
        Dimension size = driver.manage().window().getSize();
        int x = (int) (size.getWidth() * 0.50);
        swipeByCoordinates(
                x,
                (int) (size.getHeight() * 0.80),
                x,
                (int) (size.getHeight() * 0.67),
                500
        );
    }

    private void goodFineSwipeUp() {
        Dimension size = driver.manage().window().getSize();
        int x = (int) (size.getWidth() * 0.50);
        swipeByCoordinates(
                x,
                (int) (size.getHeight() * 0.72),
                x,
                (int) (size.getHeight() * 0.63),
                430
        );
    }

    private void goodFineSwipeDown() {
        Dimension size = driver.manage().window().getSize();
        int x = (int) (size.getWidth() * 0.50);
        swipeByCoordinates(
                x,
                (int) (size.getHeight() * 0.40),
                x,
                (int) (size.getHeight() * 0.49),
                430
        );
    }

    /**
     * Very small upward correction used only after a Good fund heading has already
     * been anchored in the upper-middle band. It exposes the lower advice line
     * without making the larger search movement that could skip into the next card.
     */
    private void goodAdviceNudgeUp() {
        Dimension size = driver.manage().window().getSize();
        int x = (int) (size.getWidth() * 0.50);
        swipeByCoordinates(
                x,
                (int) (size.getHeight() * 0.70),
                x,
                (int) (size.getHeight() * 0.65),
                360
        );
    }

    private void newFundSmallFixedSwipeUp() {
        Dimension size = driver.manage().window().getSize();
        int x = (int) (size.getWidth() * 0.50);
        swipeByCoordinates(
                x,
                (int) (size.getHeight() * 0.79),
                x,
                (int) (size.getHeight() * 0.67),
                480
        );
    }

    private void newFundFineSwipeUp() {
        Dimension size = driver.manage().window().getSize();
        int x = (int) (size.getWidth() * 0.50);
        swipeByCoordinates(
                x,
                (int) (size.getHeight() * 0.70),
                x,
                (int) (size.getHeight() * 0.62),
                420
        );
    }

    private void newFundFineSwipeDown() {
        Dimension size = driver.manage().window().getSize();
        int x = (int) (size.getWidth() * 0.50);
        swipeByCoordinates(
                x,
                (int) (size.getHeight() * 0.41),
                x,
                (int) (size.getHeight() * 0.49),
                420
        );
    }

    /**
     * Good cards are much taller than the Optimize cards, so the Good flow does
     * NOT reuse Optimize's micro-swipe scanner.
     *
     * We use the supplied exact next-fund heading locator to calculate one W3C
     * gesture that brings that next heading near the upper-middle card area.
     * This gives us one scroll per Fund Card while adapting to slightly different
     * card heights. There is no fling and no repeated scroll loop.
     */
    private void goodOneFundCardSwipeUp(
            String expectedNextFundName
    ) {
        Dimension size = driver.manage().window().getSize();

        int screenHeight = size.getHeight();
        int startY = (int) (screenHeight * 0.86);
        int targetHeadingY = (int) (screenHeight * 0.30);

        int defaultShift = (int) (screenHeight * 0.46);
        int minimumShift = (int) (screenHeight * 0.12);
        int maximumShift = (int) (screenHeight * 0.60);

        int calculatedShift = defaultShift;
        int observedNextY = -1;

        WebElement nextHeading =
                findRenderableElement(
                        AppiumBy.accessibilityId(expectedNextFundName)
                );

        if (nextHeading != null) {
            try {
                observedNextY = nextHeading.getRect().getY();
                calculatedShift = observedNextY - targetHeadingY;
            } catch (Exception ignored) {
                calculatedShift = defaultShift;
            }
        }

        int appliedShift =
                Math.max(
                        minimumShift,
                        Math.min(calculatedShift, maximumShift)
                );

        int endY =
                Math.max(
                        (int) (screenHeight * 0.20),
                        startY - appliedShift
                );

        diagnostic(
                "Good one-card W3C scroll geometry"
                        + " | expectedNext=" + expectedNextFundName
                        + " | observedNextY=" + observedNextY
                        + " | targetHeadingY=" + targetHeadingY
                        + " | appliedContentShift=" + appliedShift
                        + " | startY=" + startY
                        + " | endY=" + endY
        );

        swipeByCoordinates(
                (int) (size.getWidth() * 0.50),
                startY,
                (int) (size.getWidth() * 0.50),
                endY,
                950
        );
    }

    /**
     * New Fund cards are shorter than Good cards. Use the supplied exact next
     * heading to calculate one adaptive W3C card-step, keeping the same one-scroll
     * policy without reusing Optimize micro-swipes.
     */
    private void newFundOneFundCardSwipeUp(
            String expectedNextFundName
    ) {
        Dimension size = driver.manage().window().getSize();

        int screenHeight = size.getHeight();
        int startY = (int) (screenHeight * 0.82);
        int targetHeadingY = (int) (screenHeight * 0.30);

        int defaultShift = (int) (screenHeight * 0.28);
        int minimumShift = (int) (screenHeight * 0.10);
        int maximumShift = (int) (screenHeight * 0.42);

        int calculatedShift = defaultShift;
        int observedNextY = -1;

        WebElement nextHeading =
                findRenderableElement(
                        AppiumBy.accessibilityId(expectedNextFundName)
                );

        if (nextHeading != null) {
            try {
                observedNextY = nextHeading.getRect().getY();
                calculatedShift = observedNextY - targetHeadingY;
            } catch (Exception ignored) {
                calculatedShift = defaultShift;
            }
        }

        int appliedShift =
                Math.max(
                        minimumShift,
                        Math.min(calculatedShift, maximumShift)
                );

        int endY =
                Math.max(
                        (int) (screenHeight * 0.24),
                        startY - appliedShift
                );

        diagnostic(
                "New Fund one-card W3C scroll geometry"
                        + " | expectedNext=" + expectedNextFundName
                        + " | observedNextY=" + observedNextY
                        + " | targetHeadingY=" + targetHeadingY
                        + " | appliedContentShift=" + appliedShift
                        + " | startY=" + startY
                        + " | endY=" + endY
        );

        swipeByCoordinates(
                (int) (size.getWidth() * 0.50),
                startY,
                (int) (size.getWidth() * 0.50),
                endY,
                850
        );
    }

    /**
     * Returns the exact semantics node even when Flutter has pre-rendered the
     * next fund heading slightly below the currently usable viewport. This method
     * is used only to calculate the ONE card-step distance; it is never treated
     * as proof that the fund has already been reached.
     */
    private WebElement findRenderableElement(
            By locator
    ) {
        try {
            List<WebElement> elements = driver.findElements(locator);

            for (WebElement element : elements) {
                try {
                    if (element == null) {
                        continue;
                    }

                    Rectangle rect = element.getRect();

                    if (rect.getWidth() > 0
                            && rect.getHeight() > 0) {
                        return element;
                    }
                } catch (Exception ignored) {
                    // Ignore one stale semantics node and continue to a fresh one.
                }
            }
        } catch (Exception e) {
            diagnostic(
                    "Renderable Good fund lookup failed: " + safeMessage(e)
            );
        }

        return null;
    }

    // =========================================================
    // NAVIGATION / SETTLE
    // =========================================================

    private void pressBackSafely() {
        try {
            driver.pressKey(new KeyEvent(AndroidKey.BACK));
        } catch (Exception e) {
            diagnostic(
                    "Android Back cleanup attempt failed: " + safeMessage(e)
            );
        }
    }

    private void settleAfterPageOpen(String pageName) {
        diagnostic(
                "Allowing Flutter page to settle after opening "
                        + pageName + " | " + PAGE_OPEN_SETTLE_MS + " ms"
        );
        sleep(PAGE_OPEN_SETTLE_MS);
    }

    private void settleAfterHeavyPageOpen(String pageName) {
        diagnostic(
                "Allowing data-heavy Flutter page to settle after opening "
                        + pageName + " | " + HEAVY_PAGE_SETTLE_MS + " ms"
        );
        sleep(HEAVY_PAGE_SETTLE_MS);
    }

    private void settleAfterDrawerOpen(String drawerName) {
        diagnostic(
                "Allowing Flutter drawer to settle after opening "
                        + drawerName + " | " + DRAWER_SETTLE_MS + " ms"
        );
        sleep(DRAWER_SETTLE_MS);
    }

    private void settleAfterScroll() {
        sleep(900);
    }

    private void settleAfterNavigation() {
        sleep(800);
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(
                    "Thread interrupted while waiting for Flutter UI to settle",
                    e
            );
        }
    }

    private String safeMessage(Exception e) {
        if (e == null || e.getMessage() == null) {
            return "unknown error";
        }

        return e.getMessage()
                .replace('\n', ' ')
                .replace('\r', ' ')
                .trim();
    }
}
