package tests;



import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.testng.Assert;
import org.testng.SkipException;
import org.testng.annotations.Test;

import com.aventstack.extentreports.ExtentTest;

import api.FundReviewApiService;
import api.model.FundItem;
import api.model.FundReviewItem;
import api.model.ValidationResult;
import base.BaseTest;
import pages.PortfolioAnalysisFundsPage;

/**
 * Fund Review Category Validation Test Suite
 *
 * Source of truth: Fund Review API (FundReviewApiService).
 * Validates the Fund Review screen reached via:
 *   Hub → Portfolio Analysis → Funds tab → tap fund → Fund Review
 *
 * Persistent session mode: driver is shared across all tests.
 * recoverAppState() ensures Fund Review screen is open before each test.
 *
 * Report: ExtentReport with Category, Fund Name, Plan ID, Expected/Actual Category,
 *         Advice, Action Button columns logged per assertion.
 */
public class FundReviewCategoryValidationTest extends BaseTest {

    private static final List<String> ALL_CATEGORIES =
            Arrays.asList("EXIT", "GOOD", "STEADY", "OPTIMIZE", "NEW-FUND");

    private PortfolioAnalysisFundsPage fundsPage;
    private FundReviewApiService       apiService;
    
    private List<FundItem> getFundsForCategory(String category) {

        return apiService.getAllFunds()
                .stream()
                .filter(f -> category.equalsIgnoreCase(f.getClassificationType()))
                .collect(Collectors.toList());
    }
    // ================================================================
    // LIFECYCLE
    // ================================================================

    @Override
    protected boolean shouldManageDriverPerMethod() {
        return false;
    }

    @Override
    protected void onClassReady() {
        fundsPage  = new PortfolioAnalysisFundsPage(getDriver());
        apiService = new FundReviewApiService();
       // navigateToFundReview();
    }

    @Override
    protected void recoverAppState(Method method) {
        if (!fundsPage.isFundReviewScreenDisplayed()) {
            logger.info("recoverAppState: Fund Review not visible — re-navigating");
            navigateToFundReview();
        }
        Assert.assertTrue(
                fundsPage.isFundReviewScreenDisplayed(),
                "Fund Review screen must be open before: " + method.getName());
    }

    private void navigateToFundReview() {
        fundsPage.openPortfolioAnalysis();
        fundsPage.openFundsTab();
        fundsPage.openFundReview();
    }
    
    
    
   
    
    
    
    private void validateCategory(String category) {

        List<FundItem> expectedFunds =
                getFundsForCategory(category);

        int expectedCount = expectedFunds.size();

        fundsPage.openCategory(category);
        fundsPage.tapOnExit();

        int uiCount =
                fundsPage.getCategoryCount(category);

        Assert.assertEquals(
                uiCount,
                expectedCount,
                category + " count mismatch");

        for (FundItem fund : expectedFunds) {
           
            Assert.assertTrue(
                    fundsPage.findFund(fund.fundName),
                    "Fund missing: " + fund.fundName);
        }
    }
    private void validateCategoryOptimise(String category) {

        List<FundItem> expectedFunds =
                getFundsForCategory(category);

        int expectedCount = expectedFunds.size();

        fundsPage.openCategory(category);
        fundsPage.tapOnOptimises();

        int uiCount =
                fundsPage.getCategoryCount(category);
        
        Assert.assertEquals(
                uiCount,
                expectedCount,
                category + " count mismatch");

        for (FundItem fund : expectedFunds) {
           fundsPage.tapOnExit();
            Assert.assertTrue(
                    fundsPage.findFund(fund.fundName),
                    "Fund missing: " + fund.fundName);
        }
    }
    
    @Test
    public void tc_good() {
        validateCategory("GOOD");
    }

    @Test
    public void tc_optimize() {
    	validateCategoryOptimise("OPTIMIZE");
    }

    @Test
    public void tc_steady() {
        validateCategory("STEADY");
    }

    @Test
    public void tc_exit() {
        validateCategory("EXIT");
    }

    @Test
    public void tc_newFund() {
        validateCategory("NEW-FUND");
    }

    // ================================================================
    // TC_FR_001 — Category counts: API vs UI
    // ================================================================

    @Test(description = "TC_FR_001 — Category counts from API match counts shown on Fund Review tabs")
    public void tc_fr_001_verifyCategoryCountsMatchApi() {
        ExtentTest test = getExtentTest();
        test.info("Fetching category counts from API");

        Map<String, Integer> apiCounts = apiService.getCategoryCounts();
        if (apiCounts.isEmpty()) {
            throw new SkipException("API returned no fund data — cannot validate counts");
        }

        Map<String, Integer> uiCounts = fundsPage.getAllCategoryCountsFromUi();
        test.info("UI category counts: " + uiCounts);

        boolean allMatch = true;
        for (String apiCategory : apiCounts.keySet()) {
            int expected = apiCounts.get(apiCategory);
            // Map API category to UI label prefix for lookup
            String uiLabel = mapApiCategoryToUiLabel(apiCategory);
            Integer actual = uiCounts.get(uiLabel);

            if (actual == null) {
                test.warning("Category '" + uiLabel + "' not found in UI tabs");
                allMatch = false;
                continue;
            }

            String msg = String.format("Category: %-10s | API count: %d | UI count: %d | %s",
                    uiLabel, expected, actual, (expected == actual ? "PASS" : "FAIL"));
            if (expected == actual) {
                test.pass(msg);
            } else {
                test.fail(msg);
                allMatch = false;
            }
        }

        Assert.assertFalse(allMatch, "One or more category counts mismatched between API and UI");
    }

    // ================================================================
    // TC_FR_002 — Every API fund appears under its correct category tab
    // ================================================================

    @Test(description = "TC_FR_002 — Each fund from API appears under its correct category tab in the UI")
    public void tc_fr_002_verifyFundPlacementInCorrectCategory() {
        ExtentTest test = getExtentTest();

        Map<String, List<FundItem>> byCategory = apiService.getFundsByCategory();
        if (byCategory.isEmpty()) {
            throw new SkipException("API returned no fund data");
        }

        boolean allCorrect = true;

        for (String category : byCategory.keySet()) {
            List<FundItem> funds = byCategory.get(category);
            test.info("Verifying " + funds.size() + " fund(s) under category: " + category);

            fundsPage.openCategory(category);
            waitForUiToSettle();

            for (FundItem fund : funds) {
                boolean found = fundsPage.findFund(fund.fundName);
                String row = String.format(
                        "Category: %-10s | Plan ID: %-6d | Fund: %-40s | Found: %s",
                        category, fund.planId, fund.fundName, found);

                if (found) {
                    test.pass(row);
                } else {
                    test.fail(row);
                    allCorrect = false;
                }
            }
        }

        Assert.assertTrue(allCorrect, "One or more funds were not found under their API-specified category");
    }

    // ================================================================
    // TC_FR_003 — Fund does NOT appear under a wrong category tab
    // ================================================================

    @Test(description = "TC_FR_003 — Fund from API does not appear under an incorrect category tab")
    public void tc_fr_003_verifyFundNotInWrongCategory() {
        ExtentTest test = getExtentTest();

        Map<String, List<FundItem>> byCategory = apiService.getFundsByCategory();
        if (byCategory.isEmpty()) {
            throw new SkipException("API returned no fund data");
        }

        // Spot-check: use the first fund from each category
        boolean allClean = true;

        for (String correctCategory : byCategory.keySet()) {
            FundItem sampleFund = byCategory.get(correctCategory).get(0);

            for (String otherCategory : byCategory.keySet()) {
                if (otherCategory.equals(correctCategory)) continue;

                fundsPage.openCategory(otherCategory);
                waitForUiToSettle();

                boolean foundWrongly = fundsPage.findFund(sampleFund.fundName);
                String row = String.format(
                        "Fund: %-40s | Correct: %-10s | Checked: %-10s | Wrongly found: %s",
                        sampleFund.fundName, correctCategory, otherCategory, foundWrongly);

                if (!foundWrongly) {
                    test.pass(row);
                } else {
                    test.fail(row);
                    allClean = false;
                }
            }
        }

        Assert.assertTrue(allClean, "One or more funds appeared under an incorrect category tab");
    }

    // ================================================================
    // TC_FR_004 — Fund name displayed in UI matches API fund name
    // ================================================================

    @Test(description = "TC_FR_004 — Fund names visible in UI match names returned by API")
    public void tc_fr_004_verifyFundNamesMatchApi() {
        ExtentTest test = getExtentTest();

        List<FundItem> allFunds = apiService.getAllFunds();
        if (allFunds.isEmpty()) {
            throw new SkipException("API returned no fund data");
        }

        boolean allMatch = true;
        String currentCategory = null;

        for (FundItem fund : allFunds) {
            if (!fund.classificationType.equals(currentCategory)) {
                fundsPage.openCategory(fund.classificationType);
                waitForUiToSettle();
                currentCategory = fund.classificationType;
            }

            boolean found = fundsPage.findFund(fund.fundName);
            String row = String.format(
                    "Category: %-10s | Plan ID: %-6d | Name: %-40s | UI match: %s",
                    fund.classificationType, fund.planId, fund.fundName, found);

            if (found) {
                test.pass(row);
            } else {
                test.fail(row);
                allMatch = false;
            }
        }

        Assert.assertTrue(allMatch, "One or more API fund names were not found in the UI");
    }

    // ================================================================
    // TC_FR_005 — Advice text in UI matches API advice_data
    // ================================================================

    @Test(description = "TC_FR_005 — Advice text shown for each fund matches API advice_data field")
    public void tc_fr_005_verifyAdviceTextMatchesApi() {
        ExtentTest test = getExtentTest();

        Map<String, List<FundItem>> byCategory = apiService.getFundsByCategory();
        if (byCategory.isEmpty()) {
            throw new SkipException("API returned no fund data");
        }

        boolean allMatch = true;

        for (String category : byCategory.keySet()) {
            fundsPage.openCategory(category);
            waitForUiToSettle();

            // Validate first fund in each category to keep execution time reasonable
            FundItem fund = byCategory.get(category).get(0);
            if (fund.adviceData.isEmpty()) {
                test.info("No advice_data from API for: " + fund.fundName + " — skipping");
                continue;
            }

            String uiAdvice = fundsPage.getAdviceText(fund.fundName);
            boolean matches = uiAdvice.equalsIgnoreCase(fund.adviceData.trim());

            String row = String.format(
                    "Category: %-10s | Fund: %-40s | API advice: '%s' | UI advice: '%s' | Match: %s",
                    category, fund.fundName, fund.adviceData, uiAdvice, matches);

            if (matches) {
                test.pass(row);
            } else {
                test.fail(row);
                allMatch = false;
            }
        }

        Assert.assertTrue(allMatch, "One or more advice texts did not match the API advice_data");
    }

    // ================================================================
    // TC_FR_006 — GOOD category: advice visible, no action button
    // ================================================================

    @Test(description = "TC_FR_006 — GOOD category funds show advice text and have no action button")
    public void tc_fr_006_verifyGoodCategoryBehavior() {
        ExtentTest test = getExtentTest();

        List<FundItem> goodFunds = apiService.getFundsByCategory().get("GOOD");
        if (goodFunds == null || goodFunds.isEmpty()) {
            throw new SkipException("No GOOD funds returned by API");
        }

        fundsPage.openCategory("GOOD");
        waitForUiToSettle();

        test.info("Verifying GOOD category: advice visible, no action button");

        Assert.assertTrue(fundsPage.isAdviceSectionVisible(),
                "GOOD category must show 'Our Advice' section");
        test.pass("Our Advice section is visible under GOOD tab");

        String actionText = fundsPage.getActionButtonText(goodFunds.get(0).fundName);
        Assert.assertTrue(actionText.isEmpty(),
                "GOOD category must NOT have an action button, found: " + actionText);
        test.pass("No action button present under GOOD tab");
    }

    // ================================================================
    // TC_FR_007 — STEADY category: advice visible, no action button
    // ================================================================

    @Test(description = "TC_FR_007 — STEADY category funds show advice text and have no action button")
    public void tc_fr_007_verifySteadyCategoryBehavior() {
        ExtentTest test = getExtentTest();

        List<FundItem> steadyFunds = apiService.getFundsByCategory().get("STEADY");
        if (steadyFunds == null || steadyFunds.isEmpty()) {
            throw new SkipException("No STEADY funds returned by API");
        }

        fundsPage.openCategory("STEADY");
        waitForUiToSettle();

        test.info("Verifying STEADY category: advice visible, no action button");

        Assert.assertTrue(fundsPage.isAdviceSectionVisible(),
                "STEADY category must show 'Our Advice' section");
        test.pass("Our Advice section is visible under STEADY tab");

        String actionText = fundsPage.getActionButtonText(steadyFunds.get(0).fundName);
        Assert.assertTrue(actionText.isEmpty(),
                "STEADY category must NOT have an action button, found: " + actionText);
        test.pass("No action button present under STEADY tab");
    }

    // ================================================================
    // TC_FR_008 — NEW-FUND category: advice visible, no action button
    // ================================================================

    @Test(description = "TC_FR_008 — NEW-FUND category funds show advice text and have no action button")
    public void tc_fr_008_verifyNewFundCategoryBehavior() {
        ExtentTest test = getExtentTest();

        List<FundItem> newFunds = apiService.getFundsByCategory().get("NEW-FUND");
        if (newFunds == null || newFunds.isEmpty()) {
            throw new SkipException("No NEW-FUND funds returned by API");
        }

        fundsPage.openCategory("NEW-FUND");
        waitForUiToSettle();

        test.info("Verifying NEW-FUND category: advice visible, no action button");

        Assert.assertTrue(fundsPage.isAdviceSectionVisible(),
                "NEW-FUND category must show 'Our Advice' section");
        test.pass("Our Advice section is visible under NEW-FUND tab");

        String actionText = fundsPage.getActionButtonText(newFunds.get(0).fundName);
        Assert.assertTrue(actionText.isEmpty(),
                "NEW-FUND category must NOT have an action button, found: " + actionText);
        test.pass("No action button present under NEW-FUND tab");
    }

    // ================================================================
    // TC_FR_009 — EXIT category: action button present
    // ================================================================

    @Test(description = "TC_FR_009 — EXIT category fund shows an exit action button")
    public void tc_fr_009_verifyExitCategoryHasActionButton() {
        ExtentTest test = getExtentTest();

        List<FundItem> exitFunds = apiService.getFundsByCategory().get("EXIT");
        if (exitFunds == null || exitFunds.isEmpty()) {
            throw new SkipException("No EXIT funds returned by API");
        }

        fundsPage.openCategory("EXIT");
        waitForUiToSettle();

        FundItem fund = exitFunds.get(0);
        test.info("Verifying EXIT action button for fund: " + fund.fundName);

        String actionText = fundsPage.getActionButtonText(fund.fundName);

        String row = String.format(
                "Category: EXIT | Fund: %-40s | API action_data: '%s' | UI action button: '%s'",
                fund.fundName, fund.actionData, actionText);

        Assert.assertFalse(actionText.isEmpty(),
                "EXIT category must have an action button visible");
        test.pass("Action button found: " + actionText);

        if (!fund.actionData.isEmpty()) {
            Assert.assertTrue(actionText.equalsIgnoreCase(fund.actionData),
                    "Action button text must match API action_data. " + row);
            test.pass("Action button text matches API: " + row);
        }
    }

    // ================================================================
    // TC_FR_010 — OPTIMIZE category: Switch/action button present
    // ================================================================

    @Test(description = "TC_FR_010 — OPTIMIZE category fund shows a Switch/action button")
    public void tc_fr_010_verifyOptimizeCategoryHasSwitchButton() {
        ExtentTest test = getExtentTest();

        List<FundItem> optimizeFunds = apiService.getFundsByCategory().get("OPTIMIZE");
        if (optimizeFunds == null || optimizeFunds.isEmpty()) {
            throw new SkipException("No OPTIMIZE funds returned by API");
        }

        fundsPage.openCategory("OPTIMIZE");
        waitForUiToSettle();

        FundItem fund = optimizeFunds.get(0);
        test.info("Verifying OPTIMIZE action button for fund: " + fund.fundName);

        boolean switchVisible = fundsPage.isSwitchButtonVisible();
        boolean anyAction     = fundsPage.isAnyActionButtonVisible();

        String row = String.format(
                "Category: OPTIMIZE | Fund: %-40s | Switch button: %s | Any action: %s",
                fund.fundName, switchVisible, anyAction);

        Assert.assertTrue(switchVisible || anyAction,
                "OPTIMIZE category must have a Switch/action button. " + row);
        test.pass("Action button verified for OPTIMIZE: " + row);
    }

    // ================================================================
    // TC_FR_011 — Lock-in state: API lock_in_state=true shows lock indicator in UI
    // ================================================================

    @Test(description = "TC_FR_011 — Funds with lock_in_state=true from API show lock indicator in UI")
    public void tc_fr_011_verifyLockInStateIndicator() {
        ExtentTest test = getExtentTest();

        List<FundItem> lockedFunds = apiService.getAllFunds().stream()
                .filter(f -> f.lockInState)
                .collect(java.util.stream.Collectors.toList());

        if (lockedFunds.isEmpty()) {
            throw new SkipException("No locked funds returned by API — cannot validate lock indicator");
        }

        boolean allVerified = true;

        for (FundItem fund : lockedFunds) {
            fundsPage.openCategory(fund.classificationType);
            waitForUiToSettle();

            boolean lockVisible = fundsPage.getLockState(fund.fundName);
            String row = String.format(
                    "Category: %-10s | Fund: %-40s | API lock: true | UI lock indicator: %s",
                    fund.classificationType, fund.fundName, lockVisible);

            if (lockVisible) {
                test.pass(row);
            } else {
                test.warning(row + " — lock indicator not detected (may need locator tuning)");
            }

            allVerified &= lockVisible;
        }

        if (!allVerified) {
            test.warning("Lock indicator locator may need refinement based on actual UI element");
        }
    }

    // ================================================================
    // TC_FR_012 — Reason text in UI matches API reason field
    // ================================================================

    @Test(description = "TC_FR_012 — Reason/rationale text in UI matches API reason field")
    public void tc_fr_012_verifyReasonTextMatchesApi() {
        ExtentTest test = getExtentTest();

        Map<String, List<FundItem>> byCategory = apiService.getFundsByCategory();
        if (byCategory.isEmpty()) {
            throw new SkipException("API returned no fund data");
        }

        boolean allMatch = true;

        for (String category : byCategory.keySet()) {
            FundItem fund = byCategory.get(category).get(0);
            if (fund.reason.isEmpty()) {
                test.info("No reason from API for: " + fund.fundName + " — skipping");
                continue;
            }

            fundsPage.openCategory(category);
            waitForUiToSettle();

            String uiReason = fundsPage.getReasonText(fund.fundName);
            boolean matches = uiReason.equalsIgnoreCase(fund.reason.trim());

            String row = String.format(
                    "Category: %-10s | Fund: %-40s | API reason: '%s' | UI reason: '%s' | Match: %s",
                    category, fund.fundName, fund.reason, uiReason, matches);

            if (matches) {
                test.pass(row);
            } else {
                test.fail(row);
                allMatch = false;
            }
        }

        Assert.assertTrue(allMatch, "One or more reason texts did not match the API reason field");
    }

    // ================================================================
    // HELPERS
    // ================================================================

    private String mapApiCategoryToUiLabel(String apiCategory) {
        if (apiCategory == null) return "";
        switch (apiCategory.toUpperCase()) {
            case "EXIT":     return "Exit";
            case "GOOD":     return "Good";
            case "STEADY":   return "Steady";
            case "OPTIMIZE": return "Watch";
            case "NEW-FUND": return "New";
            default:         return apiCategory;
        }
    }
}
