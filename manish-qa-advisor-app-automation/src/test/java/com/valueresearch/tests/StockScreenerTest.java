package com.valueresearch.tests;

import com.aventstack.extentreports.ExtentTest;
import com.valueresearch.base.BaseTest;
import com.valueresearch.pages.StockScreenerPage;
import com.valueresearch.utils.AdvisorAccountSession;
import com.valueresearch.utils.AuthHelper;
import com.valueresearch.utils.ExtentManager;
import com.valueresearch.utils.ExtentTestManager;
import com.valueresearch.utils.ReportLogger;
import com.valueresearch.utils.ScreenshotUtils;
import io.appium.java_client.android.AndroidDriver;
import org.testng.ITestResult;
import org.testng.SkipException;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import java.lang.reflect.Method;
import java.util.Locale;

public class StockScreenerTest extends BaseTest {

    private StockScreenerPage stockScreenerPage;
    private AdvisorAccountSession accountSession;
    private boolean accountPrepared;
    private String currentCaseId;
    private ExtentTest currentReport;

    @BeforeClass(alwaysRun = true)
    public void resetAccountSession() {
        accountSession = null;
    }

    @BeforeMethod(alwaysRun = true)
    @Parameters("loginMethod")
    public void createStockScreenerPage(Method method, @Optional("PASSWORD") String loginMethod) {
        stockScreenerPage = null;
        accountPrepared = false;
        currentCaseId = null;
        currentReport = null;
        Test annotation = method.getAnnotation(Test.class);
        String caseId = method.getName().substring(0, 6);
        startExtentCase(caseId, annotation.description(),
                "Shared account verification, configured login and biometric/PIN authentication before feature checks");
        try {
            AuthHelper.LoginMethod selectedMethod;
            try {
                selectedMethod = AuthHelper.LoginMethod.valueOf(loginMethod.trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException invalid) {
                throw new IllegalArgumentException("Suite loginMethod must be OTP or PASSWORD");
            }
            if (accountSession == null || !accountSession.isForDriver(driver)) {
                accountSession = createAccountSession(driver);
            }
            // SS_017-SS_020, SS_030 (group "lapsed-user") need the lapsed account and SS_031-SS_035
            // (group "vrfa-user") the VRFA-only account; prepare() verifies the
            // logged-in email and switches through Logout + login only when it differs.
            try {
                accountSession.prepare(requiredAccountFor(method), selectedMethod);
            } catch (IllegalStateException lateSecurityPrompt) {
                // After a driver recovery the app relaunches and its PIN/biometric screen can appear
                // a moment late, while prepare() is already returning to Hub. A second prepare()
                // sees the prompt first and completes it, so retry exactly once.
                if (!String.valueOf(lateSecurityPrompt.getMessage()).contains("authentication screen")) {
                    throw lateSecurityPrompt;
                }
                ReportLogger.info("Security screen appeared late after app relaunch; preparing the account once more");
                accountSession.invalidate();
                accountSession.prepare(requiredAccountFor(method), selectedMethod);
            }
            accountSession.assertHubVisible();
            stockScreenerPage = createPage(driver, accountSession);
            accountPrepared = true;
        } catch (RuntimeException | AssertionError | LinkageError failure) {
            if (accountSession != null) accountSession.invalidate();
            ReportLogger.fail("App/account preparation failed: " + failure.getClass().getSimpleName()
                    + ". See the TestNG configuration failure trace.");
            attachFailureScreenshot();
            if (accountSession != null) {
                try {
                    accountSession.restoreTimingSettings();
                } catch (RuntimeException | AssertionError restoreFailure) {
                    failure.addSuppressed(restoreFailure);
                }
            }
            throw failure;
        }
    }

    private AuthHelper.Account requiredAccountFor(Method method) {
        for (String group : method.getAnnotation(Test.class).groups()) {
            if ("lapsed-user".equals(group)) return AuthHelper.Account.LAPSED_USER;
            if ("vrfa-user".equals(group)) return AuthHelper.Account.VRFA_ACTIVE_SUBSCRIBER;
        }
        return AuthHelper.Account.ACTIVE_SUBSCRIBER;
    }

    protected AdvisorAccountSession createAccountSession(AndroidDriver currentDriver) {
        return new AdvisorAccountSession(currentDriver);
    }

    protected StockScreenerPage createPage(AndroidDriver currentDriver, AdvisorAccountSession session) {
        return new StockScreenerPage(currentDriver, session);
    }

    @Test(priority = 1, description = "Open Stock Screener from Hub and verify heading")
    public void SS_001_OpenStockScreenerAndVerifyHeading() {
        logValidation("SS_001",
                "Navigate Hub -> Stocks -> Open stock screener, verify Stock Screener heading, return to Hub via back button");

        stockScreenerPage.openFromHub();
        stockScreenerPage.verifyHeading();
        stockScreenerPage.navigateBackToHub();

        markPassed("SS_001", "Stock Screener heading verified and returned to Hub");
    }

    @Test(priority = 2, description = "Verify Predefined and Saved tabs, open Attractive Bluechips screen and verify")
    public void SS_002_VerifyPredefinedSavedTabsAndCreateScreen() {
        logValidation("SS_002",
                "Verify Predefined and Saved tabs on Stock Screener, click Create New Screen (opens Attractive Bluechips), verify screen, return to Hub");

        stockScreenerPage.openFromHub();
        stockScreenerPage.verifyPredefinedAndSavedTabs();
        stockScreenerPage.clickCreateNewScreen();
        stockScreenerPage.verifyAttractiveBluechipsScreen();
        stockScreenerPage.navigateBackFromAttractiveBluechips();
        stockScreenerPage.navigateBackToHub();

        markPassed("SS_002", "Predefined/Saved tabs verified, Attractive Bluechips screen validated, returned to Hub");
    }

    @Test(priority = 3, description = "Verify all predefined screen headings by scrolling")
    public void SS_003_VerifyAllPredefinedScreenHeadings() {
        logValidation("SS_003",
                "Navigate to Stock Screener, scroll the Predefined tab once top-to-bottom and verify every section heading (Seasonal included), screen title and description");

        stockScreenerPage.openFromHub();
        stockScreenerPage.verifyAllPredefinedScreenHeadings();
        stockScreenerPage.navigateBackToHub();

        markPassed("SS_003", "All predefined screen headings and sections verified");
    }

    @Test(priority = 4, description = "Open all Attractive Bluechips tabs and verify Filters button")
    public void SS_004_VerifyAttractiveBluechipsTabsAndFilters() {
        logValidation("SS_004",
                "Open Stock Screener, click Create New Screen, verify Attractive Bluechips stocks filtered, horizontally scroll the tab rail and open all 6 tabs (Overview, Returns, Valuation, Per share data, Stock Rating, Custom View), verify Filters button presence, return to Hub");

        stockScreenerPage.openFromHub();
        stockScreenerPage.clickCreateNewScreen();
        stockScreenerPage.verifyAttractiveBluechipsTabsAndFilters();
        stockScreenerPage.navigateBackFromAttractiveBluechips();
        stockScreenerPage.navigateBackToHub();

        markPassed("SS_004", "All Attractive Bluechips tabs opened and Filters button verified, returned to Hub");
    }

    @Test(priority = 5, description = "Open every Value Research Ratings screen and verify heading and opening tab")
    public void SS_005_VerifyValueResearchRatingsScreens() {
        logValidation("SS_005",
                "Open Stock Screener, open each 'Value Research Ratings' screen (Top Rated Stocks, High Quality Small Caps, High Growth Large Caps, Value Meets Momentum, Top Rated Banks), wait for it to load, verify heading, verify Top Rated Stocks and Top Rated Banks open on the Stock Rating tab and the others on Overview, return to Hub");

        stockScreenerPage.openFromHub();
        stockScreenerPage.verifyPredefinedSectionScreens("Value Research Ratings");
        stockScreenerPage.navigateBackToHub();

        markPassed("SS_005", "All Value Research Ratings screens opened with correct heading and opening tab, returned to Hub");
    }

    @Test(priority = 6, description = "Open every Momentum Picks screen and verify heading and opening tab")
    public void SS_006_VerifyMomentumPicksScreens() {
        logValidation("SS_006",
                "Open Stock Screener, open each 'Momentum Picks' screen (Top Momentum Stocks, High Momentum Large Caps, High Momentum Mid Caps, High Momentum Small Caps), wait for it to load, verify heading, verify each opens on the Overview tab, return to Hub");

        stockScreenerPage.openFromHub();
        stockScreenerPage.verifyPredefinedSectionScreens("Momentum Picks");
        stockScreenerPage.navigateBackToHub();

        markPassed("SS_006", "All Momentum Picks screens opened with correct heading on the Overview tab, returned to Hub");
    }

    @Test(priority = 7, description = "Open every Investment Ideas screen and verify heading and opening tab")
    public void SS_007_VerifyInvestmentIdeasScreens() {
        logValidation("SS_007",
                "Open Stock Screener, open each 'Investment Ideas' screen (Attractive Bluechips, Book Value Discount, Growth at Reasonable PE, High Dividend Yield, Cheap Quality Stocks, Small Cap Growth), verify heading and 'N Stocks Filtered', verify each opens on the Overview tab, return to Hub");

        stockScreenerPage.openFromHub();
        stockScreenerPage.verifyPredefinedSectionScreens("Investment Ideas");
        stockScreenerPage.navigateBackToHub();

        markPassed("SS_007", "All Investment Ideas screens opened with correct heading on the Overview tab, returned to Hub");
    }

    @Test(priority = 8, description = "Open every Value guru screen and verify heading and opening tab")
    public void SS_008_VerifyValueGuruScreens() {
        logValidation("SS_008",
                "Open Stock Screener, open each 'Value guru screens' screen (Ben Graham, Joel Greenblatt, John Neff, Peter Lynch, Walter Schloss), verify heading and 'N Stocks Filtered', verify each opens on the Overview tab, return to Hub");

        stockScreenerPage.openFromHub();
        stockScreenerPage.verifyPredefinedSectionScreens("Value guru screens");
        stockScreenerPage.navigateBackToHub();

        markPassed("SS_008", "All Value guru screens opened with correct heading on the Overview tab, returned to Hub");
    }

    @Test(priority = 9, description = "Verify table columns on every tab of Top Rated Stocks")
    public void SS_009_VerifyTopRatedStocksColumns() {
        logValidation("SS_009",
                "Open Stock Screener, open Top Rated Stocks (Value Research Ratings), open Overview, Returns, Valuation, Per share data and Stock Rating tabs, scroll each table header left to right and verify the exact columns with their units (₹ / % / CR), EPS in Per share data, no removed columns (Neff Total Return, PE / Median PE, PB / Median PB) on Valuation, return to Hub");

        stockScreenerPage.openFromHub();
        stockScreenerPage.verifyPredefinedScreenColumns("Value Research Ratings", "Top Rated Stocks");
        stockScreenerPage.navigateBackToHub();

        markPassed("SS_009", "All expected columns verified on every tab of Top Rated Stocks, returned to Hub");
    }

    @Test(priority = 10, description = "Verify table columns on every tab of all Momentum Picks screens")
    public void SS_010_VerifyMomentumPicksColumns() {
        logValidation("SS_010",
                "Open Stock Screener, for each 'Momentum Picks' screen (Top Momentum Stocks, High Momentum Large Caps, High Momentum Mid Caps, High Momentum Small Caps) open Overview, Returns, Valuation, Per share data and Stock Rating tabs, scroll each table header left to right and verify the exact columns with their units (₹ / % / CR), EPS in Per share data, no removed columns (Neff Total Return, PE / Median PE, PB / Median PB) on Valuation, return to Hub");

        stockScreenerPage.openFromHub();
        stockScreenerPage.verifyPredefinedScreenColumns("Momentum Picks");
        stockScreenerPage.navigateBackToHub();

        markPassed("SS_010", "All expected columns verified on every Momentum Picks screen, returned to Hub");
    }

    @Test(priority = 11, description = "Verify table columns on every tab of all Investment Ideas screens")
    public void SS_011_VerifyInvestmentIdeasColumns() {
        logValidation("SS_011",
                "Open Stock Screener, for each 'Investment Ideas' screen (Attractive Bluechips, Book Value Discount, Growth at Reasonable PE, High Dividend Yield, Cheap Quality Stocks, Small Cap Growth) open Overview, Returns, Valuation, Per share data and Stock Rating tabs, scroll each table header left to right and verify the exact columns with their units (₹ / % / CR), EPS in Per share data, no removed columns (Neff Total Return, PE / Median PE, PB / Median PB) on Valuation, return to Hub");

        stockScreenerPage.openFromHub();
        stockScreenerPage.verifyPredefinedScreenColumns("Investment Ideas");
        stockScreenerPage.navigateBackToHub();

        markPassed("SS_011", "All expected columns verified on every Investment Ideas screen, returned to Hub");
    }

    @Test(priority = 12, description = "Verify table columns on every tab of all Value guru screens")
    public void SS_012_VerifyValueGuruColumns() {
        logValidation("SS_012",
                "Open Stock Screener, for each 'Value guru screens' screen (Ben Graham, Joel Greenblatt, John Neff, Peter Lynch, Walter Schloss) open Overview, Returns, Valuation, Per share data and Stock Rating tabs, scroll each table header left to right and verify the exact columns with their units (₹ / % / CR), EPS in Per share data, no removed columns (Neff Total Return, PE / Median PE, PB / Median PB) on Valuation, return to Hub");

        stockScreenerPage.openFromHub();
        stockScreenerPage.verifyPredefinedScreenColumns("Value guru screens");
        stockScreenerPage.navigateBackToHub();

        markPassed("SS_012", "All expected columns verified on every Value guru screen, returned to Hub");
    }

    @Test(priority = 13, description = "Verify default filters of every Value Research Ratings screen")
    public void SS_013_VerifyValueResearchRatingsDefaultFilters() {
        logValidation("SS_013",
                "Open Stock Screener, open each 'Value Research Ratings' screen, open Filters and verify the default filters (Top Rated Stocks: Stock Rating; High Quality Small Caps: Quality Score 8, Growth Score 4, Valuation Score 4, By Capitalisation; High Growth Large Caps: Growth Score 7, By Capitalisation; Value Meets Momentum: Quality Score 4, Valuation Score 6, Momentum Score 7, Mcap 500; Top Rated Banks: By Sector, Stock Rating, '6 Filters Applied', By Sector > Financial with only the 4 bank sub-sectors ticked), close Filters with the device Back key, return to Hub");

        stockScreenerPage.openFromHub();
        stockScreenerPage.verifyDefaultFilters("Value Research Ratings");
        stockScreenerPage.navigateBackToHub();

        markPassed("SS_013", "Default filters verified on all Value Research Ratings screens, returned to Hub");
    }

    @Test(priority = 14, description = "Verify default filters of every Momentum Picks screen")
    public void SS_014_VerifyMomentumPicksDefaultFilters() {
        logValidation("SS_014",
                "Open Stock Screener, open each 'Momentum Picks' screen, open Filters and verify the default filters (Top Momentum Stocks: Quality Score 5, Momentum Score 8, Mcap 500; High Momentum Large Caps: Momentum Score 7, By Capitalisation; High Momentum Mid Caps: Quality Score 4, Momentum Score 8, By Capitalisation; High Momentum Small Caps: Quality Score 6, Momentum Score 8, Mcap 500, By Capitalisation), close Filters with the device Back key, return to Hub");

        stockScreenerPage.openFromHub();
        stockScreenerPage.verifyDefaultFilters("Momentum Picks");
        stockScreenerPage.navigateBackToHub();

        markPassed("SS_014", "Default filters verified on all Momentum Picks screens, returned to Hub");
    }

    @Test(priority = 15, description = "Verify default filters of every Investment Ideas screen")
    public void SS_015_VerifyInvestmentIdeasDefaultFilters() {
        logValidation("SS_015",
                "Open Stock Screener, open each 'Investment Ideas' screen (Attractive Bluechips, Book Value Discount, Growth at Reasonable PE, High Dividend Yield, Cheap Quality Stocks, Small Cap Growth), open Filters and verify the default filters and their values, close Filters with the device Back key, return to Hub");

        stockScreenerPage.openFromHub();
        stockScreenerPage.verifyDefaultFilters("Investment Ideas");
        stockScreenerPage.navigateBackToHub();

        markPassed("SS_015", "Default filters verified on all Investment Ideas screens, returned to Hub");
    }

    @Test(priority = 16, description = "Verify default filters of every Value guru screen")
    public void SS_016_VerifyValueGuruDefaultFilters() {
        logValidation("SS_016",
                "Open Stock Screener, open each 'Value guru screens' screen (Ben Graham, Joel Greenblatt, John Neff, Peter Lynch, Walter Schloss), open Filters and verify the default filters and their values, close Filters with the device Back key, return to Hub");

        stockScreenerPage.openFromHub();
        stockScreenerPage.verifyDefaultFilters("Value guru screens");
        stockScreenerPage.navigateBackToHub();

        markPassed("SS_016", "Default filters verified on all Value guru screens, returned to Hub");
    }

    // ===================== Iteration 2 (navigation) =====================

    @Test(priority = 21, description = "Open a stock's detail page from Top Rated Stocks / Stock Rating tab")
    public void SS_021_OpenStockDetailFromTopRatedStocks() {
        logValidation("SS_021",
                "Open Stock Screener, open Top Rated Stocks, go to the Stock Rating tab, tap the first stock, verify the stock detail page shows that stock's name, go back to Top Rated Stocks, return to Hub");

        stockScreenerPage.openFromHub();
        stockScreenerPage.verifyStockDetailFromTopRatedStocks();
        stockScreenerPage.navigateBackToHub();

        markPassed("SS_021", "Stock detail page opened with the correct stock name and returned, back on Hub");
    }

    @Test(priority = 22, description = "Attractive Bluechips heading after pull-to-refresh (both entry points)")
    public void SS_022_AttractiveBluechipsHeadingAfterRefresh() {
        logValidation("SS_022",
                "Open Attractive Bluechips from Create New Screen and from its Predefined card and pull down to refresh: from the Predefined card the heading must stay 'Attractive Bluechips'; from Create New Screen (a general screener) it must read 'Stock Screener', return to Hub");

        stockScreenerPage.openFromHub();
        stockScreenerPage.verifyAttractiveBluechipsHeadingAfterRefresh();
        stockScreenerPage.navigateBackToHub();

        markPassed("SS_022", "Headings after refresh correct from both entry points, back on Hub");
    }

    @Test(priority = 23, description = "Create a custom screen from Top Rated Stocks, save it and verify it in the Saved tab")
    public void SS_023_CreateAndVerifySavedScreen() {
        logValidation("SS_023",
                "Open Top Rated Stocks, Filters > Add Filter > Ownership: add Promoter's Stake and Institutional Stake with default values, Apply Filters, Update Screen (heading becomes 'Stock Screener', stock count drops), three dots > Save Screen as screen<N> (N > 100), verify the 'New Record Added' popup, go back, open the Saved tab and refresh until the new screen is listed, return to Hub");

        stockScreenerPage.openFromHub();
        String saved = stockScreenerPage.createAndVerifySavedScreen();
        stockScreenerPage.navigateBackToHub();

        markPassed("SS_023", "Custom screen saved as '" + saved + "' and listed in the Saved tab, back on Hub");
    }

    @Test(priority = 24, description = "Scrolling the Predefined tab must not scroll the Saved tab")
    public void SS_024_SavedTabKeepsOwnScrollPosition() {
        logValidation("SS_024",
                "Open Stock Screener, note the Saved tab's top entry and position, scroll the Predefined tab down, open the Saved tab again and verify it was not scrolled, return to Hub");

        stockScreenerPage.openFromHub();
        stockScreenerPage.verifySavedTabNotScrolledByPredefined();
        stockScreenerPage.navigateBackToHub();

        markPassed("SS_024", "Saved tab kept its own scroll position, back on Hub");
    }

    // ===================== Iteration 2 (data) =====================

    @Test(priority = 25, description = "Verify table columns on every tab of all Seasonal screens")
    public void SS_025_VerifySeasonalColumns() {
        logValidation("SS_025",
                "Open Stock Screener, for each 'Seasonal' screen (52 Week / 3 Year / 5 Year / All Time High and Low) open Overview, Returns, Valuation, Per share data and Stock Rating, scroll each table header and verify the exact columns (fix: Seasonal screens showed only 2 columns), return to Hub");

        stockScreenerPage.openFromHub();
        stockScreenerPage.verifyPredefinedScreenColumns("Seasonal");
        stockScreenerPage.navigateBackToHub();

        markPassed("SS_025", "All expected columns verified on every Seasonal screen, returned to Hub");
    }

    @Test(priority = 26, description = "Stock Rating column shows 'Unrated' instead of '0.00'")
    public void SS_026_StockRatingShowsUnrated() {
        logValidation("SS_026",
                "Open Small Cap Growth (Investment Ideas), switch to the Stock Rating tab, scroll the table and verify unrated stocks show 'Unrated' (bug: '0.00'); skipped when no stock is unrated today, return to Hub");

        stockScreenerPage.openFromHub();
        boolean found = stockScreenerPage.verifyUnratedStockRating();
        stockScreenerPage.navigateBackToHub();

        if (!found) {
            throw new SkipException("No unrated stock in Small Cap Growth today; nothing to verify");
        }
        markPassed("SS_026", "'Unrated' shown in the Stock Rating column, back on Hub");
    }

    @Test(priority = 27, description = "Verify Custom View columns on every predefined screen")
    public void SS_027_VerifyCustomViewColumns() {
        logValidation("SS_027",
                "Open Stock Screener, open every predefined screen (all sections incl. Seasonal), open the Custom View tab and verify its exact columns match the VRO site, return to Hub");

        stockScreenerPage.openFromHub();
        stockScreenerPage.verifyCustomViewColumns();
        stockScreenerPage.navigateBackToHub();

        markPassed("SS_027", "Custom View columns verified on every predefined screen, returned to Hub");
    }

    @Test(priority = 28, description = "Add Columns offers EPS")
    public void SS_028_AddColumnsOffersEps() {
        logValidation("SS_028",
                "Open Top Rated Stocks > Custom View > Columns > Add Columns and verify EPS is offered (under Per share data); leave with the device Back key without applying, return to Hub");

        stockScreenerPage.openFromHub();
        stockScreenerPage.verifyAddColumnsOffersEps();
        stockScreenerPage.navigateBackToHub();

        markPassed("SS_028", "EPS offered in Add Columns, back on Hub");
    }

    @Test(priority = 29, description = "Add Columns lists every column per category")
    public void SS_029_AddColumnsListsAllColumns() {
        logValidation("SS_029",
                "Open Top Rated Stocks > Custom View > Columns > Add Columns, scroll the panel and verify every column under Overview, Return, Valuation, Per share data and Stock Rating; leave with the device Back key without applying, return to Hub");

        stockScreenerPage.openFromHub();
        stockScreenerPage.verifyAddColumnsListsAllColumns();
        stockScreenerPage.navigateBackToHub();

        markPassed("SS_029", "Add Columns lists every expected column, back on Hub");
    }

    // ===================== Lapsed user (TC 17 - TC 20) =====================

    @Test(priority = 17, groups = "lapsed-user", description = "Lapsed user: Create New Screen opens the subscription page")
    public void SS_017_LapsedCreateNewScreenShowsSubscriptionPage() {
        logValidation("SS_017",
                "With the lapsed-user account, open Stock Screener, tap Create New Screen, verify the subscription page (headline and Subscribe button), close it with the device Back key, return to Hub");

        stockScreenerPage.openFromHub();
        stockScreenerPage.verifyLapsedCreateNewScreenOpensSubscription();
        stockScreenerPage.navigateBackToHub();

        markPassed("SS_017", "Create New Screen opened the subscription page for the lapsed user, returned to Hub");
    }

    @Test(priority = 18, groups = "lapsed-user", description = "Lapsed user: Saved tab shows the subscription nudge")
    public void SS_018_LapsedSavedTabShowsSubscriptionNudge() {
        logValidation("SS_018",
                "With the lapsed-user account, open Stock Screener, open the Saved tab, verify the 'limited version' nudge and Subscribe Now button, return to Hub");

        stockScreenerPage.openFromHub();
        stockScreenerPage.verifyLapsedSavedTabShowsNudge();
        stockScreenerPage.navigateBackToHub();

        markPassed("SS_018", "Saved tab showed the subscription nudge for the lapsed user, returned to Hub");
    }

    @Test(priority = 19, groups = "lapsed-user", description = "Lapsed user: Custom View shows the subscription nudge on each section's first screen")
    public void SS_019_LapsedCustomViewShowsSubscriptionNudge() {
        logValidation("SS_019",
                "With the lapsed-user account, open the first screen of every Predefined section (Top Rated Stocks, Top Momentum Stocks, Attractive Bluechips, Ben Graham; Seasonal disabled), open the Custom View tab and verify the subscription nudge, return to Hub");

        stockScreenerPage.openFromHub();
        stockScreenerPage.verifyLapsedCustomViewNudgeOnFirstScreens();
        stockScreenerPage.navigateBackToHub();

        markPassed("SS_019", "Custom View showed the subscription nudge on every section's first screen, returned to Hub");
    }

    @Test(priority = 20, groups = "lapsed-user", description = "Lapsed user: Save Screen opens the subscription page")
    public void SS_020_LapsedSaveScreenOpensSubscriptionPage() {
        logValidation("SS_020",
                "With the lapsed-user account, open Top Rated Stocks, go to the Valuation tab, open the three-dots drawer, tap Save Screen; it must open the subscription page; return to Hub");

        stockScreenerPage.openFromHub();
        stockScreenerPage.verifyLapsedScreenActionsOpenSubscription("Value Research Ratings", "Top Rated Stocks");
        stockScreenerPage.navigateBackToHub();

        markPassed("SS_020", "Save Screen opened the subscription page for the lapsed user, returned to Hub");
    }

    @Test(priority = 30, groups = "lapsed-user", description = "Lapsed user: on Seasonal screens only Custom View is locked")
    public void SS_030_LapsedSeasonalOnlyCustomViewLocked() {
        logValidation("SS_030",
                "With the lapsed-user account, open every Seasonal screen; Overview, Returns, Valuation, Per share data and Stock Rating must be open (no nudge) and only Custom View must show the subscription nudge (bug: whole screens locked), return to Hub");

        stockScreenerPage.openFromHub();
        stockScreenerPage.verifyLapsedSeasonalOnlyCustomViewLocked();
        stockScreenerPage.navigateBackToHub();

        markPassed("SS_030", "Seasonal screens lock only Custom View for the lapsed user, back on Hub");
    }

    // ===================== Customisation (active account) =====================

    @Test(priority = 36, description = "Reset all on Review Filters restores the default filters")
    public void SS_036_ResetAllRestoresDefaultFilters() {
        logValidation("SS_036",
                "Open Top Rated Stocks, Filters > Add Filter > Ownership > Promoter's Stake (default range) > Apply Filters; on Review Filters tap Reset all (before Update Screen) and verify '1 Filter Applied' with only Stock Rating; leave the sheet and verify the stock count is unchanged, return to Hub");

        stockScreenerPage.openFromHub();
        stockScreenerPage.verifyResetAllRestoresDefaultFilters();
        stockScreenerPage.navigateBackToHub();

        markPassed("SS_036", "Reset all restored the default filter (Stock Rating only), back on Hub");
    }

    @Test(priority = 37, description = "Open a saved screen: its name, filters and columns are restored")
    public void SS_037_SavedScreenRestoresFiltersAndColumns() {
        logValidation("SS_037",
                "Build a screen from Top Rated Stocks (Promoter's Stake + Institutional Stake, Update Screen), add EPS in Custom View (Columns > Add Columns > Per share data > EPS > Apply Columns > Update Columns), save it as screen<N>; reopen it from the Saved tab and verify the name, the Overview tab, the stock count, '3 Filters Applied' with all three filters, and the EPS (₹) column in Custom View, return to Hub");

        stockScreenerPage.openFromHub();
        String saved = stockScreenerPage.verifySavedScreenRestoresFiltersAndColumns();
        stockScreenerPage.navigateBackToHub();

        markPassed("SS_037", "Saved screen '" + saved + "' reopened with its filters and EPS column, back on Hub");
    }

    @Test(priority = 38, description = "Export to Excel from Top Rated Stocks")
    public void SS_038_ExportToExcel() {
        logValidation("SS_038",
                "Open Top Rated Stocks, three dots > Export; verify the download dialog (file name, size 'downloaded', Cancel, Open), close it with Cancel, and verify the file name has the screen name and date as on VRO (top-rated-stocks-dd-MMM-yyyy-HHMM), return to Hub");

        stockScreenerPage.openFromHub();
        stockScreenerPage.verifyExportToExcel();
        stockScreenerPage.navigateBackToHub();

        markPassed("SS_038", "Export dialog and file name verified, back on Hub");
    }

    // ===================== VRFA-only user (free Stock Advisor access) =====================
    // A Fund Advisor-only subscriber has no Stock Advisor subscription, so the screener behaves
    // as for a free user: the same checks as the lapsed-user cases, run on that account.

    @Test(priority = 31, groups = "vrfa-user", description = "VRFA-only user: Create New Screen opens the subscription page")
    public void SS_031_VrfaCreateNewScreenShowsSubscriptionPage() {
        logValidation("SS_031",
                "With the VRFA-only account, open Stock Screener, tap Create New Screen, verify the subscription page (headline and Subscribe button), close it with the device Back key, return to Hub");

        stockScreenerPage.openFromHub();
        stockScreenerPage.verifyLapsedCreateNewScreenOpensSubscription();
        stockScreenerPage.navigateBackToHub();

        markPassed("SS_031", "Create New Screen opened the subscription page for the VRFA-only user, returned to Hub");
    }

    @Test(priority = 32, groups = "vrfa-user", description = "VRFA-only user: Saved tab shows the subscription nudge")
    public void SS_032_VrfaSavedTabShowsSubscriptionNudge() {
        logValidation("SS_032",
                "With the VRFA-only account, open Stock Screener, open the Saved tab, verify the 'limited version' nudge and Subscribe Now button, return to Hub");

        stockScreenerPage.openFromHub();
        stockScreenerPage.verifyLapsedSavedTabShowsNudge();
        stockScreenerPage.navigateBackToHub();

        markPassed("SS_032", "Saved tab showed the subscription nudge for the VRFA-only user, returned to Hub");
    }

    @Test(priority = 33, groups = "vrfa-user", description = "VRFA-only user: Custom View shows the subscription nudge on each section's first screen")
    public void SS_033_VrfaCustomViewShowsSubscriptionNudge() {
        logValidation("SS_033",
                "With the VRFA-only account, open the first screen of every Predefined section, open the Custom View tab and verify the subscription nudge, return to Hub");

        stockScreenerPage.openFromHub();
        stockScreenerPage.verifyLapsedCustomViewNudgeOnFirstScreens();
        stockScreenerPage.navigateBackToHub();

        markPassed("SS_033", "Custom View showed the subscription nudge on every section's first screen, returned to Hub");
    }

    @Test(priority = 34, groups = "vrfa-user", description = "VRFA-only user: Save Screen opens the subscription page")
    public void SS_034_VrfaSaveScreenOpensSubscriptionPage() {
        logValidation("SS_034",
                "With the VRFA-only account, open Top Rated Stocks, go to the Valuation tab, open the three-dots drawer, tap Save Screen; it must open the subscription page; return to Hub");

        stockScreenerPage.openFromHub();
        stockScreenerPage.verifyLapsedScreenActionsOpenSubscription("Value Research Ratings", "Top Rated Stocks");
        stockScreenerPage.navigateBackToHub();

        markPassed("SS_034", "Save Screen opened the subscription page for the VRFA-only user, returned to Hub");
    }

    @Test(priority = 35, groups = "vrfa-user", description = "VRFA-only user: on Seasonal screens only Custom View is locked")
    public void SS_035_VrfaSeasonalOnlyCustomViewLocked() {
        logValidation("SS_035",
                "With the VRFA-only account, open every Seasonal screen; Overview, Returns, Valuation, Per share data and Stock Rating must be open (no nudge) and only Custom View must show the subscription nudge, return to Hub");

        stockScreenerPage.openFromHub();
        stockScreenerPage.verifyLapsedSeasonalOnlyCustomViewLocked();
        stockScreenerPage.navigateBackToHub();

        markPassed("SS_035", "Seasonal screens lock only Custom View for the VRFA-only user, back on Hub");
    }

    @AfterMethod(alwaysRun = true)
    public void cleanupStockScreenerCase(ITestResult result) {
        Throwable original = result.getThrowable();
        if (accountPrepared && original != null) attachFailureScreenshot();
        Throwable cleanupFailure = null;
        try {
            if (accountPrepared && stockScreenerPage != null) {
                stockScreenerPage.navigateBackToHub();
                accountSession.assertHubVisible();
            }
        } catch (RuntimeException | AssertionError failure) {
            // Navigation-only failure: keep the verified account. The next
            // @BeforeMethod prepare() re-checks PIN/login screens and returns to
            // Hub itself, so invalidating here would only force a slow
            // Account Details re-verification.
            cleanupFailure = failure;
        } finally {
            if (accountSession != null) {
                try {
                    accountSession.restoreTimingSettings();
                } catch (RuntimeException | AssertionError failure) {
                    if (cleanupFailure == null) cleanupFailure = failure;
                    else cleanupFailure.addSuppressed(failure);
                    accountSession.invalidate();
                }
            }
            accountPrepared = false;
        }
        if (cleanupFailure != null) {
            if (original != null) original.addSuppressed(cleanupFailure);
            else {
                result.setThrowable(cleanupFailure);
                result.setStatus(ITestResult.FAILURE);
                attachFailureScreenshot();
            }
            ReportLogger.fail("Stock Screener cleanup failed: " + cleanupFailure.getClass().getSimpleName());
        }
    }

    private void attachFailureScreenshot() {
        try {
            String path = ScreenshotUtils.captureScreenshot(driver, currentCaseId + "_before_Hub_reset");
            if (path != null && currentReport != null) {
                currentReport.addScreenCaptureFromPath(path, "Failed state before Hub reset");
            }
        } catch (Exception unavailable) {
            ReportLogger.debug("Failure screenshot unavailable: " + unavailable.getClass().getSimpleName());
        }
    }

    /** Creates the Extent node for a case (called once per case from @BeforeMethod). */
    private void startExtentCase(String caseId, String title, String validation) {
        ExtentTest test = ExtentManager.getExtentReports().createTest(caseId + " - " + title);
        currentCaseId = caseId;
        currentReport = test;
        test.info(
                "Module: Stock Screener<br>"
                        + "Case ID: " + caseId + "<br>"
                        + "Page Build: " + StockScreenerPage.BUILD_VERSION + "<br>"
                        + "Validation: " + validation
        );
        ExtentTestManager.setTest(test);
    }

    /** Adds the case-specific validation text to the node created in @BeforeMethod. */
    private void logValidation(String caseId, String validation) {
        if (currentReport == null || !caseId.equals(currentCaseId)) {
            // Safety net: the case is running without its @BeforeMethod node.
            startExtentCase(caseId, caseId, validation);
            return;
        }
        currentReport.info("Validation: " + validation);
        ExtentTestManager.setTest(currentReport);
    }

    private void markPassed(String caseId, String message) {
        ExtentTestManager.getTest().pass(
                "<span class='badge white-text green'>" + caseId + " - " + message + "</span>"
        );
        ReportLogger.pass("Completed test case: " + caseId + " - " + message);
    }
}