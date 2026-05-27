package tests;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.SkipException;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import base.BaseTest;
import pages.PortfolioPlannerPage;

/**
 * Portfolio Planner investor + CTA validation tests.
 *
 * Uses:
 * - Persistent Appium session
 * - Simplified enterprise workflow
 * - Existing BasePage/BaseTest APIs only
 */
public class PortfolioPlannerInvestorTest extends BaseTest {

    private static final Logger logger =
            LoggerFactory.getLogger(
                    PortfolioPlannerInvestorTest.class);

    private static final String PRIMARY_INVESTOR =
            "Manish Khatri";

    private PortfolioPlannerPage plannerPage;

    private List<String> cachedInvestors;

    private boolean subscriberFlow;

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
        plannerPage.navigateToHubAndOpenPlanner();
        subscriberFlow = plannerPage.isSelectInvestorScreenDisplayed();
        if (subscriberFlow) {
            cachedInvestors = plannerPage.discoverInvestorNames();
            logger.info("Discovered investors: {}", cachedInvestors);
        } else {
            cachedInvestors = Collections.emptyList();
            logger.info("Free-user planner flow detected");
        }
    }

    @Override
    protected void recoverAppState(Method method) {
        plannerPage.navigateToHubAndOpenPlanner();
        Assert.assertTrue(
                plannerPage.isSelectInvestorScreenDisplayed()
                        || plannerPage.isPlannerOptionsScreenDisplayed()
                        || plannerPage.verifyKycPendingScreen()
                        || plannerPage.verifyRiskAssessmentPending(),
                "Planner landing screen must load before " + method.getName());
        logger.info("Planner state ready for '{}'", method.getName());
    }

    // ============================================================
    // DATA PROVIDER
    // ============================================================

    @DataProvider(name = "allInvestors")
    public Object[][] allInvestors() {

        if (cachedInvestors == null
                || cachedInvestors.isEmpty()) {

            return new Object[][]{
                    {PRIMARY_INVESTOR}
            };
        }

        return cachedInvestors.stream()
                .map(name -> new Object[]{name})
                .toArray(Object[][]::new);
    }

    // ============================================================
    // INVESTOR TESTS
    // ============================================================

    @Test(description =
            "Verify all investors appear on investor selection screen")
    public void verifyAllInvestorsDisplayed() {

        if (!subscriberFlow) {

            throw new SkipException(
                    "Free-user flow");
        }

        Assert.assertFalse(
                cachedInvestors.isEmpty(),
                "No investors discovered");

        for (String investor : cachedInvestors) {

            Assert.assertTrue(
                    plannerPage.isCtaVisible(investor),
                    "Investor missing: " + investor);

            logger.info(
                    "Verified investor '{}'",
                    investor);
        }
    }

    @Test(
            dataProvider = "allInvestors",
            description =
                    "Verify investor reaches planner options")
    public void verifyInvestorNavigation(
            String investorName) {

        if (!subscriberFlow) {

            throw new SkipException(
                    "Free-user flow");
        }

        plannerPage.selectInvestor(
                investorName);

        plannerPage.clickNext();

        Assert.assertTrue(
                plannerPage.isPlannerOptionsScreenDisplayed(),
                "Planner options must load");

        Assert.assertTrue(
                plannerPage.verifyNoAppCrash(),
                "No crash allowed");

        logger.info(
                "Investor '{}' reached planner options",
                investorName);
    }

    // ============================================================
    // PLAN GENERATION
    // ============================================================

    @Test(description =
            "Verify SIP investment plan generation")
    public void verifySipInvestmentPlan() {

        prepareSipPlan();
        

        Assert.assertTrue(
                plannerPage.verifyInvestmentPlanScreen(),
                "Investment plan screen must load");

        logger.info(
                "SIP investment plan validated");
    }

    @Test(description =
            "Verify one-time investment plan generation")
    public void verifyOneTimeInvestmentPlan() {

        if (subscriberFlow) {

            plannerPage.selectInvestor(
                    PRIMARY_INVESTOR);

            plannerPage.clickNext();
        }

        plannerPage.choosePlannerType(
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
                "One-time plan validated");
    }

    @Test(description =
            "Verify Both mode validation")
    public void verifyBothInvestmentMode() {

        if (subscriberFlow) {

            plannerPage.selectInvestor(
                    PRIMARY_INVESTOR);

            plannerPage.clickNext();
        }

        plannerPage.choosePlannerType(
                "Invest for higher returns");

        plannerPage.selectInvestmentMode(
                "Both");

        plannerPage.enterSipAmount(
                "6000");

        plannerPage.enterInvestmentPeriod(
                "30");

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
    // CTA VALIDATION
    // ============================================================

    @Test(description =
            "Validate Breakdown CTA")
    public void validateBreakdownCta() {

        prepareSipPlan();

        Assert.assertTrue(
                plannerPage.isBreakdownButtonDisplayed(),
                "Breakdown CTA missing");

        Assert.assertTrue(
                plannerPage.tapCta("Breakdown"),
                "Breakdown CTA failed");

        Assert.assertTrue(
                plannerPage.verifyNoAppCrash(),
                "No crash allowed");

        plannerPage.navigateBackSafely();

        logger.info(
                "Breakdown CTA validated");
    }

    @Test(description =
            "Validate Edit CTA")
    public void validateEditCta() {

        prepareSipPlan();

        Assert.assertTrue(
                plannerPage.isEditButtonDisplayed(),
                "Edit CTA missing");

        Assert.assertTrue(
                plannerPage.tapCta("Edit"),
                "Edit CTA failed");

        Assert.assertTrue(
                plannerPage.verifyEditInvestmentPlan(),
                "Edit plan screen missing");

        logger.info(
                "Edit CTA validated");
    }

    @Test(description =
            "Validate Invest Now CTA")
    public void validateInvestNowCta() {

        prepareSipPlan();

        Assert.assertTrue(
                plannerPage.isInvestNowEnabled(),
                "Invest Now disabled");

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

        plannerPage.navigateBackSafely();

        logger.info(
                "Invest CTA validated");
    }

    @Test(description =
            "Validate Add Fund CTA")
    public void validateAddFundCta() {

        prepareSipPlan();

        Assert.assertTrue(
                plannerPage.tapCta("Edit"),
                "Edit CTA failed");

        Assert.assertTrue(
                plannerPage.verifyEditInvestmentPlan(),
                "Edit screen missing");

        Assert.assertTrue(
                plannerPage.isCtaVisible("Add Fund"),
                "Add Fund CTA missing");

        Assert.assertTrue(
                plannerPage.tapCta("Add Fund"),
                "Add Fund CTA failed");

        plannerPage.navigateBackSafely();

        logger.info(
                "Add Fund CTA validated");
    }

    @Test(description =
            "Validate Reset CTA")
    public void validateResetCta() {

        prepareSipPlan();

        Assert.assertTrue(
                plannerPage.tapCta("Edit"),
                "Edit CTA failed");

        Assert.assertTrue(
                plannerPage.verifyEditInvestmentPlan(),
                "Edit screen missing");

        Assert.assertTrue(
                plannerPage.isCtaVisible("Reset"),
                "Reset CTA missing");

        Assert.assertTrue(
                plannerPage.tapCta("Reset"),
                "Reset CTA failed");

        plannerPage.navigateBackSafely();

        logger.info(
                "Reset CTA validated");
    }

    @Test(description =
            "Validate all visible investment plan CTAs")
    public void validateAllInvestmentPlanCtas() {

        prepareSipPlan();

        List<String> ctaIds =
                plannerPage.getInvestmentPlanCtaIds();

        Assert.assertFalse(
                ctaIds.isEmpty(),
                "No CTAs visible");

        for (String ctaId : ctaIds) {

            Assert.assertTrue(
                    plannerPage.isCtaVisible(ctaId),
                    "CTA missing: " + ctaId);

            Assert.assertTrue(
                    plannerPage.isCtaEnabled(ctaId),
                    "CTA disabled: " + ctaId);

            logger.info(
                    "Validated CTA '{}'",
                    ctaId);
        }
    }

    // ============================================================
    // FREE USER FLOW
    // ============================================================

    @Test(description =
            "Validate Subscribe CTA for free user")
    public void validateSubscribeCta() {

        if (subscriberFlow) {

            throw new SkipException(
                    "Subscriber flow");
        }

        plannerPage.choosePlannerType(
                "Invest for higher returns");

        plannerPage.selectInvestmentMode(
                "SIP");

        plannerPage.enterSipAmount(
                "1000");

        plannerPage.enterInvestmentPeriod(
                "60");

        plannerPage.clickShowInvestmentPlan();

        Assert.assertTrue(
                plannerPage.isBlurredInvestmentPlanDisplayed(),
                "Blurred investment plan missing");

        Assert.assertTrue(
                plannerPage.isSubscribeCtaDisplayed(),
                "Subscribe CTA missing");

        Assert.assertTrue(
                plannerPage.tapCta("Subscribe"),
                "Subscribe CTA failed");

        logger.info(
                "Subscribe CTA validated");
    }

    // ============================================================
    // NAVIGATION RECOVERY
    // ============================================================

    @Test(description =
            "Validate planner navigation recovery")
    public void validatePlannerRecovery() {

        prepareSipPlan();

        plannerPage.navigateToHubAndOpenPlanner();

        Assert.assertTrue(
                plannerPage.isSelectInvestorScreenDisplayed()
                        || plannerPage.isPlannerOptionsScreenDisplayed(),
                "Planner recovery failed");

        Assert.assertTrue(
                plannerPage.verifyNoAppCrash(),
                "No crash allowed");

        logger.info(
                "Planner recovery validated");
    }

    // ============================================================
    // PRIVATE HELPERS
    // ============================================================

    private void prepareSipPlan() {

        if (subscriberFlow
                && plannerPage.isSelectInvestorScreenDisplayed()) {

            plannerPage.selectInvestor(
                    PRIMARY_INVESTOR);

            plannerPage.clickNext();
        }

        plannerPage.choosePlannerType(
                "Invest for higher returns");

        plannerPage.selectInvestmentMode(
                "SIP");

        plannerPage.enterSipAmount(
                "1000");

        plannerPage.enterInvestmentPeriod(
                "60");

        plannerPage.clickShowInvestmentPlan();

        Assert.assertTrue(
                plannerPage.verifyInvestmentPlanScreen(),
                "Investment plan must load");
    }
}
