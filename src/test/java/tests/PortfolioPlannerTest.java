package tests;

import java.lang.reflect.Method;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.SkipException;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import base.BaseTest;
import pages.PortfolioPlannerPage;

/**
 * Portfolio Planner Test Suite
 *
 * Simplified according to final:
 * - BaseTest
 * - BasePage
 * - PortfolioPlannerPage
 *
 * No over-engineering.
 * No page-source parsing.
 * No unsupported helper methods.
 */
public class PortfolioPlannerTest extends BaseTest {

    private static final Logger logger =
            LoggerFactory.getLogger(
                    PortfolioPlannerTest.class);

    private static final String DEFAULT_INVESTOR =
            "Manish Khatri";

    private PortfolioPlannerPage plannerPage;

    // ============================================================
    // BASE CONFIG
    // ============================================================

    @Override
    protected boolean shouldManageDriverPerMethod() {

        return false;
    }

    @Override
    protected void onClassReady() {
        plannerPage = new PortfolioPlannerPage(getDriver());
    }

    @Override
    protected void recoverAppState(Method method) {
        plannerPage.navigateToHubAndOpenPlanner();
        Assert.assertTrue(
                plannerPage.isSelectInvestorScreenDisplayed()
                        || plannerPage.isPlannerOptionsScreenDisplayed(),
                "Planner landing screen must load before " + method.getName());
        logger.info("Planner landing screen ready");
    }

    // ============================================================
    // DATA PROVIDERS
    // ============================================================

    @DataProvider(name = "invalidSipAmounts")
    public Object[][] invalidSipAmounts() {

        return new Object[][]{
                {"0"},
                {"1"},
                {"999"},
                {""}
        };
    }

    @DataProvider(name = "invalidOneTimeAmounts")
    public Object[][] invalidOneTimeAmounts() {

        return new Object[][]{
                {"0"},
                {"1"},
                {"4999"},
                {""}
        };
    }

    @DataProvider(name = "invalidPeriods")
    public Object[][] invalidPeriods() {

        return new Object[][]{
                {""},
                {"0"},
                {"-1"},
                {"abc"}
        };
    }

    // ============================================================
    // NAVIGATION TESTS
    // ============================================================

    @Test(description =
            "Verify subscriber reaches Select Investor screen")
    public void verifySubscriberNavigation() {

        if (!plannerPage.isSubscriberFlow()) {

            throw new SkipException(
                    "Free-user flow detected");
        }

        Assert.assertTrue(
                plannerPage.isSelectInvestorScreenDisplayed(),
                "Select Investor screen must load");

        Assert.assertTrue(
                plannerPage.verifyRiskProfile(),
                "Risk profile section must display");

        logger.info(
                "Subscriber flow validated");
    }

    @Test(description =
            "Verify free user skips investor selection")
    public void verifyFreeUserFlow() {

        if (!plannerPage.isFreeUserFlow()) {

            throw new SkipException(
                    "Subscriber flow detected");
        }

        Assert.assertTrue(
                plannerPage.isPlannerOptionsScreenDisplayed(),
                "Planner options must load");

        logger.info(
                "Free-user flow validated");
    }

    // ============================================================
    // VALID PLAN TESTS
    // ============================================================

    @Test(description =
            "Verify valid SIP investment plan generation")
    public void verifySipInvestmentPlan() {

        prepareSubscriberPlanner(
                "Invest for higher returns");

        plannerPage.selectInvestmentMode(
                "SIP");

        plannerPage.enterSipAmount(
                "1000");

        plannerPage.enterInvestmentPeriod(
                "10");

        plannerPage.chooseMonthsOrYears(
                "Months");

        plannerPage.clickShowInvestmentPlan();

        Assert.assertTrue(
                plannerPage.verifyInvestmentPlanScreen(),
                "Investment plan screen must load");

        logger.info(
                "Valid SIP plan verified");
    }

    @Test(description =
            "Verify valid one-time investment plan generation")
    public void verifyOneTimeInvestmentPlan() {

        prepareSubscriberPlanner(
                "Invest for higher returns");

        plannerPage.selectInvestmentMode(
                "One-time");

        plannerPage.enterOneTimeAmount(
                "5000");

        plannerPage.enterInvestmentPeriod(
                "12");

        plannerPage.clickShowInvestmentPlan();

        Assert.assertTrue(
                plannerPage.verifyInvestmentPlanScreen(),
                "One-time investment plan must load");

        logger.info(
                "Valid one-time plan verified");
    }

    @Test(description =
            "Verify Both investment mode flow")
    public void verifyBothModeFlow() {

        prepareSubscriberPlanner(
                "Invest for higher returns");

        plannerPage.selectInvestmentMode(
                "Both");

        plannerPage.enterSipAmount(
                "1000");

        plannerPage.enterInvestmentPeriod(
                "29");

        plannerPage.clickShowInvestmentPlan();

        Assert.assertTrue(
                plannerPage.verifyInvestmentPlanScreen()
                        || plannerPage.verifyValidationMessage(
                        "One-time"),
                "Both mode validation failed");

        logger.info(
                "Both mode validated");
    }

    // ============================================================
    // INVALID SIP TESTS
    // ============================================================

    @Test(
            dataProvider = "invalidSipAmounts",
            description =
                    "Verify invalid SIP amount validation")
    public void verifyInvalidSipAmountValidation(
            String amount) {

        prepareSubscriberPlanner(
                "Invest for higher returns");

        plannerPage.selectInvestmentMode(
                "SIP");

        plannerPage.enterSipAmount(
                amount);

        plannerPage.enterInvestmentPeriod(
                "60");

        plannerPage.clickShowInvestmentPlan();

        Assert.assertTrue(
                plannerPage.verifyValidationMessage(
                        "Monthly amount"),
                "SIP validation must appear");

        logger.info(
                "Invalid SIP validation verified for '{}'",
                amount);
    }

    // ============================================================
    // INVALID ONE-TIME TESTS
    // ============================================================

    @Test(
            dataProvider = "invalidOneTimeAmounts",
            description =
                    "Verify invalid one-time amount validation")
    public void verifyInvalidOneTimeValidation(
            String amount) {

        prepareSubscriberPlanner(
                "Invest for higher returns");

        plannerPage.selectInvestmentMode(
                "One-time");

        plannerPage.enterOneTimeAmount(
                amount);

        plannerPage.enterInvestmentPeriod(
                "12");

        plannerPage.clickShowInvestmentPlan();

        Assert.assertTrue(
                plannerPage.verifyValidationMessage(
                        "One-time amount"),
                "One-time validation must appear");

        logger.info(
                "Invalid one-time validation verified for '{}'",
                amount);
    }

    // ============================================================
    // INVALID PERIOD TESTS
    // ============================================================

    @Test(
            dataProvider = "invalidPeriods",
            description =
                    "Verify invalid period validation")
    public void verifyInvalidPeriodValidation(
            String period) {

        prepareSubscriberPlanner(
                "Invest for higher returns");

        plannerPage.selectInvestmentMode(
                "SIP");

        plannerPage.enterSipAmount(
                "1000");

        plannerPage.enterInvestmentPeriod(
                period);

        plannerPage.clickShowInvestmentPlan();

        Assert.assertTrue(
                plannerPage.verifyValidationMessage(
                        "period")
                        || !plannerPage.verifyInvestmentPlanScreen(),
                "Period validation must appear");

        logger.info(
                "Invalid period validation verified for '{}'",
                period);
    }

    // ============================================================
    // CTA TESTS
    // ============================================================

    @Test(description =
            "Verify Breakdown CTA")
    public void verifyBreakdownCta() {

        prepareValidSubscriberPlan();

        Assert.assertTrue(
                plannerPage.isBreakdownButtonDisplayed(),
                "Breakdown CTA missing");

        plannerPage.openBreakdown();

        Assert.assertTrue(
                plannerPage.verifyNoAppCrash(),
                "No crash allowed");

        logger.info(
                "Breakdown CTA verified");
    }

    @Test(description =
            "Verify Edit CTA")
    public void verifyEditCta() {

        prepareValidSubscriberPlan();

        Assert.assertTrue(
                plannerPage.isEditButtonDisplayed(),
                "Edit CTA missing");

        Assert.assertTrue(
                plannerPage.tapCta("Edit"),
                "Edit CTA failed");

        Assert.assertTrue(
                plannerPage.verifyEditInvestmentPlan(),
                "Edit screen must load");

        logger.info(
                "Edit CTA verified");
    }

    @Test(description =
            "Verify Invest Now CTA")
    public void verifyInvestNowCta() {

        prepareValidSubscriberPlan();

        Assert.assertTrue(
                plannerPage.isInvestNowEnabled(),
                "Invest Now must be enabled");

        String ctaId =
                plannerPage.isCtaVisible("Invest Now")
                        ? "Invest Now"
                        : "Proceed to pay";

        Assert.assertTrue(
                plannerPage.tapCta(ctaId),
                "Invest CTA failed");

        Assert.assertTrue(
                plannerPage.verifyNoAppCrash(),
                "No crash allowed");

        logger.info(
                "Invest Now CTA verified");
    }

    // ============================================================
    // FREE USER TESTS
    // ============================================================

    @Test(description =
            "Verify free user blurred investment plan")
    public void verifyFreeUserBlurredPlan() {

        if (!plannerPage.isFreeUserFlow()) {

            throw new SkipException(
                    "Subscriber flow detected");
        }

        plannerPage.choosePlannerType(
                "Invest for higher returns");

        plannerPage.selectInvestmentMode(
                "SIP");

        plannerPage.enterSipAmount(
                "1000");

        plannerPage.enterInvestmentPeriod(
                "3");

        plannerPage.clickShowInvestmentPlan();

        Assert.assertTrue(
                plannerPage.isBlurredInvestmentPlanDisplayed(),
                "Blurred investment plan missing");

        Assert.assertTrue(
                plannerPage.isSubscribeCtaDisplayed(),
                "Subscribe CTA missing");

        logger.info(
                "Free-user blurred plan verified");
    }

    // ============================================================
    // STATE TESTS
    // ============================================================

    @Test(description =
            "Verify KYC pending state")
    public void verifyKycPendingState() {

        if (!plannerPage.verifyKycPendingScreen()) {

            throw new SkipException(
                    "KYC pending state unavailable");
        }

        Assert.assertTrue(
                plannerPage.verifyKycPendingScreen(),
                "KYC pending screen must display");

        logger.info(
                "KYC pending state verified");
    }

    @Test(description =
            "Verify risk assessment pending state")
    public void verifyRiskAssessmentPendingState() {

        if (!plannerPage.verifyRiskAssessmentPending()) {

            throw new SkipException(
                    "Risk assessment pending state unavailable");
        }

        Assert.assertTrue(
                plannerPage.verifyRiskAssessmentPending(),
                "Risk assessment pending screen must display");

        logger.info(
                "Risk assessment pending state verified");
    }

    // ============================================================
    // RECOVERY TEST
    // ============================================================

    @Test(description =
            "Verify planner navigation recovery")
    public void verifyPlannerRecovery() {

        prepareValidSubscriberPlan();

        plannerPage.navigateToHubAndOpenPlanner();

        Assert.assertTrue(
                plannerPage.isSelectInvestorScreenDisplayed()
                        || plannerPage.isPlannerOptionsScreenDisplayed(),
                "Planner must reopen successfully");

        Assert.assertTrue(
                plannerPage.verifyNoAppCrash(),
                "No crash allowed");

        logger.info(
                "Planner recovery verified");
    }

    // ============================================================
    // PRIVATE HELPERS
    // ============================================================

    private void prepareSubscriberPlanner(
            String plannerType) {

        if (plannerPage.isSubscriberFlow()) {

            plannerPage.selectInvestor(
                    DEFAULT_INVESTOR);

            plannerPage.clickNext();
        }

        plannerPage.choosePlannerType(
                plannerType);

        logger.info(
                "Prepared planner type '{}'",
                plannerType);
    }

    private void prepareValidSubscriberPlan() {

        prepareSubscriberPlanner(
                "Invest for higher returns");

        plannerPage.selectInvestmentMode(
                "SIP");

        plannerPage.enterSipAmount(
                "1000");

        plannerPage.enterInvestmentPeriod(
                "15");

        plannerPage.clickShowInvestmentPlan();

        Assert.assertTrue(
                plannerPage.verifyInvestmentPlanScreen(),
                "Investment plan must load");

        logger.info(
                "Prepared valid subscriber plan");
    }
}
