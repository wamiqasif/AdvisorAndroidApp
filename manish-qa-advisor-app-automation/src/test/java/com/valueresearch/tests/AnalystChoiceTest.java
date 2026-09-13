package com.valueresearch.tests;

import com.aventstack.extentreports.ExtentTest;
import com.valueresearch.base.BaseTest;
import com.valueresearch.pages.AnalystChoicePage;
import com.valueresearch.utils.AuthHelper;
import com.valueresearch.utils.ExtentManager;
import com.valueresearch.utils.ExtentTestManager;
import com.valueresearch.utils.ReportLogger;
import org.testng.annotations.Test;

import java.util.List;
import java.util.function.Function;

/**
 * Analyst's Choice dynamic fund-capture regression.
 *
 * Each recommendation card is a separate TestNG test case so failures are
 * isolated and ExtentReports shows one result per card.
 *
 * Scope per test:
 * - confirm login/session
 * - open Analyst's Choice from Hub
 * - open one recommendation card
 * - capture live fund names dynamically
 * - return to the Analyst's Choice listing exactly once
 *
 * No hardcoded fund names and no rating/category/return/horizontal-table checks.
 */
public class AnalystChoiceTest extends BaseTest {

    @Test(
            priority = 1,
            alwaysRun = true,
            description = "Capture live fund names from Aggressive Growth"
    )
    public void AC_001_CaptureAggressiveGrowthFundNames() {
        runCardCapture(
                "AC_001",
                "Aggressive Growth",
                AnalystChoicePage::captureAggressiveGrowthFundNames
        );
    }

    @Test(
            priority = 2,
            alwaysRun = true,
            description = "Capture live fund names from Growth"
    )
    public void AC_002_CaptureGrowthFundNames() {
        runCardCapture(
                "AC_002",
                "Growth",
                AnalystChoicePage::captureGrowthFundNames
        );
    }

    @Test(
            priority = 3,
            alwaysRun = true,
            description = "Capture live fund names from Tax Planning"
    )
    public void AC_003_CaptureTaxPlanningFundNames() {
        runCardCapture(
                "AC_003",
                "Tax Planning",
                AnalystChoicePage::captureTaxPlanningFundNames
        );
    }

    @Test(
            priority = 4,
            alwaysRun = true,
            description = "Validate Growth - International current recommendation state"
    )
    public void AC_004_CaptureGrowthInternationalFundNames() {
        runCardCapture(
                "AC_004",
                "Growth - International",
                AnalystChoicePage::captureGrowthInternationalFundNames
        );
    }

    @Test(
            priority = 5,
            alwaysRun = true,
            description = "Capture live fund names from Conservative Growth"
    )
    public void AC_005_CaptureConservativeGrowthFundNames() {
        runCardCapture(
                "AC_005",
                "Conservative Growth",
                AnalystChoicePage::captureConservativeGrowthFundNames
        );
    }

    @Test(
            priority = 6,
            alwaysRun = true,
            description = "Capture live fund names from Conservative Growth & Income"
    )
    public void AC_006_CaptureConservativeGrowthIncomeFundNames() {
        runCardCapture(
                "AC_006",
                "Conservative Growth & Income",
                AnalystChoicePage::captureConservativeGrowthIncomeFundNames
        );
    }

    @Test(
            priority = 7,
            alwaysRun = true,
            description = "Capture live fund names from Core Fixed Income"
    )
    public void AC_007_CaptureCoreFixedIncomeFundNames() {
        runCardCapture(
                "AC_007",
                "Core Fixed Income",
                AnalystChoicePage::captureCoreFixedIncomeFundNames
        );
    }

    @Test(
            priority = 8,
            alwaysRun = true,
            description = "Capture live fund names from Capital Preservation"
    )
    public void AC_008_CaptureCapitalPreservationFundNames() {
        runCardCapture(
                "AC_008",
                "Capital Preservation",
                AnalystChoicePage::captureCapitalPreservationFundNames
        );
    }

    private void runCardCapture(
            String caseId,
            String cardName,
            Function<AnalystChoicePage, List<String>> captureAction
    ) {
        createExtentTest(
                caseId,
                "Capture live fund names - " + cardName,
                "Open only the " + cardName
                        + " recommendation card, capture the current live fund names dynamically, "
                        + "and return to the Analyst's Choice listing"
        );

        ReportLogger.step("Starting " + caseId + " - " + cardName + " dynamic fund capture");

        AnalystChoicePage page = new AnalystChoicePage(driver);

        /*
         * Do not run AuthHelper again when the previous card test has already
         * returned to Analyst's Choice. AuthHelper waits for normal bottom tabs,
         * which are not exposed on this module screen and can otherwise consume
         * the full long-wait timeout before every card.
         *
         * Each test remains independently runnable: when the suite starts from
         * PIN/Funds/Hub, the normal login check is still performed.
         */
        if (page.isAnalystChoiceModuleActive()) {
            ReportLogger.pass(
                    "Existing Analyst's Choice state detected; repeated login/session check skipped"
            );
        } else {
            new AuthHelper(driver).ensureLoggedIn();
            ReportLogger.pass("Advisor login/session confirmed");
        }

        List<String> funds = captureAction.apply(page);

        if (funds == null || funds.isEmpty()) {
            ReportLogger.pass(cardName + " -> no current fund recommendations");
        } else {
            ReportLogger.pass(
                    cardName + " -> captured " + funds.size() + " fund(s): "
                            + String.join(" | ", funds)
            );
        }

        markPassed(
                caseId + " - " + cardName + " fund capture validated successfully"
        );
    }

    private void createExtentTest(String caseId, String title, String validation) {
        ExtentTest test = ExtentManager.getExtentReports().createTest(
                caseId + " - " + title
        );

        test.info(
                "Module: Analyst's Choice<br>"
                        + "Case ID: " + caseId + "<br>"
                        + "Page Build: " + AnalystChoicePage.BUILD_VERSION + "<br>"
                        + "Data strategy: Live/dynamic fund capture; no hardcoded fund names<br>"
                        + "Execution strategy: Independent test case for this recommendation card<br>"
                        + "Validation: " + validation
        );

        ExtentTestManager.setTest(test);
    }

    private void markPassed(String message) {
        ExtentTest currentTest = ExtentTestManager.getTest();

        if (currentTest != null) {
            currentTest.pass(
                    "<span class='badge white-text green'>"
                            + message
                            + "</span>"
            );
        }

        ReportLogger.pass("Completed test case: " + message);
    }
}