package com.valueresearch.tests;

import com.aventstack.extentreports.ExtentTest;
import com.valueresearch.base.BaseTest;
import com.valueresearch.pages.PortfolioAnalysisFaPage;
import com.valueresearch.utils.ExtentManager;
import com.valueresearch.utils.ExtentTestManager;
import com.valueresearch.utils.ReportLogger;

import org.testng.annotations.AfterClass;
import org.testng.annotations.Test;

public class PortfolioAnalysisFaTest extends BaseTest {

    private PortfolioAnalysisFaPage.EntryPointSummary entryPointSummary;
    private PortfolioAnalysisFaPage.RegressionSummary optimizeSummary;
    private PortfolioAnalysisFaPage.RegressionSummary goodSummary;
    private PortfolioAnalysisFaPage.RegressionSummary newFundSummary;
    private PortfolioAnalysisFaPage.RegressionSummary steadySummary;

    @Test(priority = 0)
    public void PAFA_000_VerifyFundReviewEntryPointsAcrossSummaryAndFundsSifs() {
        createExtentTest(
                "PAFA_000",
                "Summary to Fund Review entry and safe return",
                "From Portfolio Analysis > Summary, tap one physically visible attention-fund name, confirm Fund Review opens, then use the supplied Go back control and confirm Portfolio Analysis is restored with Summary and Funds / SIFs tabs visible. Funds / SIFs card entry routes are intentionally out of PAFA_000 scope."
        );

        ReportLogger.step("[START] PAFA_000 | Summary > Fund Review > Portfolio Analysis return");
        PortfolioAnalysisFaPage page = new PortfolioAnalysisFaPage(driver);

        page.ensureAdvisorAppLoggedInForPortfolioAnalysisFa();
        page.openHubFromBottomNavigationForPortfolioAnalysisFa();
        page.openPortfolioAnalysisFromHubForPortfolioAnalysisFa();
        page.selectInvestment5ForPortfolioAnalysisFa();
        ReportLogger.pass("PAFA_000 | Portfolio Analysis ready | investor=My Investment - 5");

        entryPointSummary =
                page.verifyFundReviewEntryPointsAcrossSummaryAndFundsSifsForPortfolioAnalysisFa();

        logEntryPointResult("PAFA_000", entryPointSummary);

        if (entryPointSummary != null && !entryPointSummary.allOpened()) {
            throw new AssertionError(
                    "PAFA_000 functional entry validation completed with "
                            + (entryPointSummary.getTotalEntryPoints() - entryPointSummary.getOpenedEntryPoints())
                            + " failed route(s). See [ENTRY FAIL] lines above."
            );
        }

        ReportLogger.pass(
                "PAFA_000 | Entry pre-check complete | next=Optimize/Good/New Fund/Steady bucket regressions"
        );
    }

    @Test(priority = 1)
    public void PAFA_001_VerifyOptimizeCalculationTriggersAndDrawersAcrossFunds() {
        createExtentTest(
                "PAFA_001",
                "Optimize calculation drawer PRD validation",
                "Validate calculation cards and exact PRD drawer content across mapped Optimize funds."
        );

        ReportLogger.step("[START] PAFA_001 | Optimize calculation drawer regression");
        PortfolioAnalysisFaPage page = new PortfolioAnalysisFaPage(driver);
        navigateToOptimizeOnce(page, "PAFA_001");

        optimizeSummary =
                page.verifyCalculationDrawersAcrossOptimizeRegressionFundsForPortfolioAnalysisFa();
        logCaseResult("PAFA_001", optimizeSummary);
        failTestOnPrdMismatch("PAFA_001", optimizeSummary);
    }

    @Test(priority = 2)
    public void PAFA_002_VerifyGoodStartAdviceAcrossGoodFunds() {
        createExtentTest(
                "PAFA_002",
                "Good Start advice PRD validation",
                "Validate mapped Good Start Direct/Regular advice; [Month Year] accepts the real app month/year."
        );

        ReportLogger.step("[START] PAFA_002 | Good Start advice regression");
        PortfolioAnalysisFaPage page = new PortfolioAnalysisFaPage(driver);
        navigateToGoodOnce(page, "PAFA_002");

        goodSummary = page.verifyGoodStartAdviceAcrossGoodFundsForPortfolioAnalysisFa();
        logCaseResult("PAFA_002", goodSummary);
        failTestOnPrdMismatch("PAFA_002", goodSummary);
    }

    @Test(priority = 3)
    public void PAFA_003_VerifyNewFundAdviceAgainstPrdAcrossFunds() {
        createExtentTest(
                "PAFA_003",
                "New Fund advice PRD validation",
                "Validate Abakkus Large & Mid Cap Direct/Regular New Fund advice against the PRD; [Month Year] accepts the real app month/year."
        );

        ReportLogger.step("[START] PAFA_003 | New Fund advice regression");
        PortfolioAnalysisFaPage page = new PortfolioAnalysisFaPage(driver);
        navigateToNewFundOnce(page, "PAFA_003");

        newFundSummary = page.verifyNewFundAdviceAgainstPrdAcrossFundsForPortfolioAnalysisFa();
        logCaseResult("PAFA_003", newFundSummary);
        failTestOnPrdMismatch("PAFA_003", newFundSummary);
    }

    @Test(priority = 4)
    public void PAFA_004_VerifySteadyAdviceAgainstPrdAcrossMappedFunds() {
        createExtentTest(
                "PAFA_004",
                "Steady advice PRD validation",
                "Validate TC-03 to TC-08 Direct/Regular advice; any PRD mismatch is a real test failure while all mapped cards are still checked and reported."
        );

        ReportLogger.step("[START] PAFA_004 | Steady advice regression");
        PortfolioAnalysisFaPage page = new PortfolioAnalysisFaPage(driver);
        navigateToSteadyOnce(page, "PAFA_004");

        steadySummary = page.verifySteadyAdviceAgainstPrdAcrossFundsForPortfolioAnalysisFa();
        logCaseResult("PAFA_004", steadySummary);
        failTestOnPrdMismatch("PAFA_004", steadySummary);
    }

    /**
     * Final combined-run cleanup and one-line team-lead summary.
     * After PAFA_000 and all four bucket regressions finish, the app is deliberately
     * left on the Hub bottom-navigation home screen.
     */
    @AfterClass(alwaysRun = true)
    public void finalizePortfolioAnalysisFaCombinedRun() {
        ExtentTest combinedSummaryTest = ExtentManager.getExtentReports()
                .createTest("PAFA Combined - Final Summary");
        ExtentTestManager.setTest(combinedSummaryTest);

        boolean hubHomeReached = false;

        try {
            if (driver != null && driver.getSessionId() != null) {
                PortfolioAnalysisFaPage page = new PortfolioAnalysisFaPage(driver);
                hubHomeReached =
                        page.returnToHubHomeForPortfolioAnalysisFa();
            }

            if (!hubHomeReached) {
                ReportLogger.step(
                        "[CLEANUP WARNING] Hub final state was not confirmed"
                );
            }
        } catch (Throwable cleanupError) {
            ReportLogger.step(
                    "[CLEANUP WARNING] Could not confirm Hub final state | "
                            + safeThrowableMessage(cleanupError)
            );
        }

        int totalMismatches = mismatchCount(optimizeSummary)
                + mismatchCount(goodSummary)
                + mismatchCount(newFundSummary)
                + mismatchCount(steadySummary);

        int entryPointFailures = entryPointSummary == null
                ? 0
                : entryPointSummary.getTotalEntryPoints() - entryPointSummary.getOpenedEntryPoints();

        int totalAutomationFailures = automationFailureCount(optimizeSummary)
                + automationFailureCount(goodSummary)
                + automationFailureCount(newFundSummary)
                + automationFailureCount(steadySummary);

        boolean allFiveCompleted = entryPointSummary != null
                && optimizeSummary != null
                && goodSummary != null
                && newFundSummary != null
                && steadySummary != null;

        String status;
        if (!allFiveCompleted || totalAutomationFailures > 0) {
            status = "FAIL - AUTOMATION/EXECUTION ERROR";
        } else if (entryPointFailures > 0) {
            status = "FAIL - FUNCTIONAL ENTRY";
        } else if (totalMismatches > 0) {
            status = "FAIL - PRD MISMATCHES";
        } else {
            status = "PASS";
        }

        String finalMessage =
                "[FINAL RESULT] PAFA combined | "
                        + entryPointSummaryPart(entryPointSummary)
                        + " | " + summaryPart("Optimize", optimizeSummary)
                        + " | " + summaryPart("Good", goodSummary)
                        + " | " + summaryPart("New Fund", newFundSummary)
                        + " | " + summaryPart("Steady", steadySummary)
                        + " | functionalEntryFailures=" + entryPointFailures
                        + " | totalPRDMismatches=" + totalMismatches
                        + " | automationFailures=" + totalAutomationFailures
                        + " | finalScreen="
                        + (hubHomeReached ? "Hub" : "NOT CONFIRMED")
                        + " | status=" + status;

        if ("PASS".equals(status)) {
            ReportLogger.pass(finalMessage);
            combinedSummaryTest.pass(finalMessage);
        } else {
            ReportLogger.fail(finalMessage);
            combinedSummaryTest.fail(finalMessage);
        }
    }

    private void navigateToOptimizeOnce(PortfolioAnalysisFaPage page, String caseId) {
        executeNavigationWithRecovery(
                caseId,
                page,
                () -> navigateToOptimizeOnceRaw(page, caseId)
        );
    }

    private void navigateToOptimizeOnceRaw(PortfolioAnalysisFaPage page, String caseId) {
        page.ensureAdvisorAppLoggedInForPortfolioAnalysisFa();
        ReportLogger.pass(caseId + " | 1/6 Session ready");

        page.openHubFromBottomNavigationForPortfolioAnalysisFa();
        ReportLogger.pass(caseId + " | 2/6 Hub opened");

        page.openPortfolioAnalysisFromHubForPortfolioAnalysisFa();
        ReportLogger.pass(caseId + " | 3/6 Portfolio Analysis opened");

        page.selectInvestment5ForPortfolioAnalysisFa();
        ReportLogger.pass(caseId + " | 4/6 My Investment - 5 selected");

        page.openFundsSifsForPortfolioAnalysisFa();
        ReportLogger.pass(caseId + " | 5/6 Funds / SIFs loaded without scrolling");

        page.openOptimizeFundReviewForPortfolioAnalysisFa();
        ReportLogger.pass(caseId + " | 6/6 Optimize opened");
    }

    private void navigateToGoodOnce(PortfolioAnalysisFaPage page, String caseId) {
        executeNavigationWithRecovery(
                caseId,
                page,
                () -> navigateToGoodOnceRaw(page, caseId)
        );
    }

    private void navigateToGoodOnceRaw(PortfolioAnalysisFaPage page, String caseId) {
        page.ensureAdvisorAppLoggedInForPortfolioAnalysisFa();
        ReportLogger.pass(caseId + " | 1/6 Session ready");

        page.openHubFromBottomNavigationForPortfolioAnalysisFa();
        ReportLogger.pass(caseId + " | 2/6 Hub opened");

        page.openPortfolioAnalysisFromHubForPortfolioAnalysisFa();
        ReportLogger.pass(caseId + " | 3/6 Portfolio Analysis opened");

        page.selectInvestment5ForPortfolioAnalysisFa();
        ReportLogger.pass(caseId + " | 4/6 My Investment - 5 selected");

        page.openFundsSifsForPortfolioAnalysisFa();
        ReportLogger.pass(caseId + " | 5/6 Funds / SIFs loaded without scrolling");

        page.openGoodFundReviewForPortfolioAnalysisFa();
        ReportLogger.pass(caseId + " | 6/6 Good opened");
    }

    private void navigateToNewFundOnce(PortfolioAnalysisFaPage page, String caseId) {
        executeNavigationWithRecovery(
                caseId,
                page,
                () -> navigateToNewFundOnceRaw(page, caseId)
        );
    }

    private void navigateToNewFundOnceRaw(PortfolioAnalysisFaPage page, String caseId) {
        page.ensureAdvisorAppLoggedInForPortfolioAnalysisFa();
        ReportLogger.pass(caseId + " | 1/6 Session ready");

        page.openHubFromBottomNavigationForPortfolioAnalysisFa();
        ReportLogger.pass(caseId + " | 2/6 Hub opened");

        page.openPortfolioAnalysisFromHubForPortfolioAnalysisFa();
        ReportLogger.pass(caseId + " | 3/6 Portfolio Analysis opened");

        page.selectInvestment5ForPortfolioAnalysisFa();
        ReportLogger.pass(caseId + " | 4/6 My Investment - 5 selected");

        page.openFundsSifsForPortfolioAnalysisFa();
        ReportLogger.pass(caseId + " | 5/6 Funds / SIFs loaded without scrolling");

        page.openNewFundReviewForPortfolioAnalysisFa();
        ReportLogger.pass(caseId + " | 6/6 New Fund opened");
    }

    private void navigateToSteadyOnce(PortfolioAnalysisFaPage page, String caseId) {
        executeNavigationWithRecovery(
                caseId,
                page,
                () -> navigateToSteadyOnceRaw(page, caseId)
        );
    }

    private void navigateToSteadyOnceRaw(PortfolioAnalysisFaPage page, String caseId) {
        page.ensureAdvisorAppLoggedInForPortfolioAnalysisFa();
        ReportLogger.pass(caseId + " | 1/6 Session ready");

        page.openHubFromBottomNavigationForPortfolioAnalysisFa();
        ReportLogger.pass(caseId + " | 2/6 Hub opened");

        page.openPortfolioAnalysisFromHubForPortfolioAnalysisFa();
        ReportLogger.pass(caseId + " | 3/6 Portfolio Analysis opened");

        page.selectInvestment5ForPortfolioAnalysisFa();
        ReportLogger.pass(caseId + " | 4/6 My Investment - 5 selected");

        page.openFundsSifsForPortfolioAnalysisFa();
        ReportLogger.pass(caseId + " | 5/6 Funds / SIFs loaded without scrolling");

        page.openSteadyFundReviewForPortfolioAnalysisFa();
        ReportLogger.pass(caseId + " | 6/6 Steady opened");
    }

    /**
     * Happy-path behavior is unchanged: the original six navigation steps run once.
     * Recovery is activated only after that proven flow has exhausted its own
     * condition-based waits. There is no additional fixed 10-second sleep here.
     * The same original flow is retried from Hub; a second failure triggers one
     * full Advisor app restart and one final retry.
     */
    private void executeNavigationWithRecovery(
            String caseId,
            PortfolioAnalysisFaPage page,
            NavigationAttempt navigationAttempt
    ) {
        Throwable firstFailure;

        try {
            navigationAttempt.run();
            return;
        } catch (Throwable failure) {
            firstFailure = failure;
        }

        ReportLogger.step(
                "[RECOVERY] " + caseId
                        + " | expected navigation state not ready after explicit wait"
                        + " | action=reopen Portfolio Analysis from Hub"
        );

        try {
            navigationAttempt.run();
            return;
        } catch (Throwable secondFailure) {
            ReportLogger.step(
                    "[RECOVERY] " + caseId
                            + " | Hub reopen did not restore expected route"
                            + " | action=restart Advisor app and retry fresh"
            );

            try {
                page.restartAdvisorAppForPortfolioAnalysisFa();
                navigationAttempt.run();
                return;
            } catch (Throwable finalFailure) {
                throw new AssertionError(
                        "Navigation recovery exhausted"
                                + " | caseId=" + caseId
                                + " | firstError=" + safeThrowableMessage(firstFailure)
                                + " | finalError=" + safeThrowableMessage(finalFailure),
                        finalFailure
                );
            }
        }
    }

    @FunctionalInterface
    private interface NavigationAttempt {
        void run();
    }

    private void logEntryPointResult(
            String caseId,
            PortfolioAnalysisFaPage.EntryPointSummary summary
    ) {
        if (summary == null) {
            return;
        }

        String message =
                "[RESULT] " + caseId
                        + " | Summary entry"
                        + " | opened=" + summary.getOpenedEntryPoints()
                        + "/" + summary.getTotalEntryPoints()
                        + " | hubRecoveries=" + summary.getHubRecoveries()
                        + " | appRestarts=" + summary.getAppRestarts()
                        + " | returnedScreen=Portfolio Analysis (Summary + Funds / SIFs tabs visible)"
                        + " | status=" + (summary.allOpened() ? "PASS" : "COMPLETED WITH ENTRY FAILURES");

        if (summary.allOpened()) {
            ReportLogger.pass(message);
            if (ExtentTestManager.getTest() != null) {
                ExtentTestManager.getTest().pass(message);
            }
        } else {
            ReportLogger.fail(message);
            if (ExtentTestManager.getTest() != null) {
                ExtentTestManager.getTest().fail(message);
            }
        }
    }

    private String entryPointSummaryPart(PortfolioAnalysisFaPage.EntryPointSummary summary) {
        if (summary == null) {
            return "EntryPoints=NOT COMPLETED";
        }

        return "EntryPoints="
                + summary.getOpenedEntryPoints()
                + "/" + summary.getTotalEntryPoints()
                + " opened,recovery="
                + (summary.getHubRecoveries() + summary.getAppRestarts());
    }

    private void logCaseResult(
            String caseId,
            PortfolioAnalysisFaPage.RegressionSummary summary
    ) {
        if (summary == null) {
            return;
        }

        String status = summary.hasPrdMismatches()
                ? "FAIL - PRD MISMATCHES"
                : "PASS";

        String message =
                "[RESULT] " + caseId
                        + " | " + summary.getBucket()
                        + " | matched=" + summary.getMatchedCards()
                        + "/" + summary.getMappedCards()
                        + " | prdMismatches=" + summary.getPrdMismatchCount()
                        + " | automationFailures=" + summary.getAutomationFailureCount()
                        + " | status=" + status;

        ExtentTest test = ExtentTestManager.getTest();
        if (summary.hasPrdMismatches()) {
            ReportLogger.fail(message);
            if (test != null) {
                test.fail(message);
            }
        } else {
            ReportLogger.pass(message);
            if (test != null) {
                test.pass(message);
            }
        }
    }

    /**
     * QA policy: a PRD mismatch is a real functional failure, not a warning.
     * The page object still validates all mapped cards first, so the report contains
     * the complete mismatch count/details before this assertion marks TestNG red.
     */
    private void failTestOnPrdMismatch(
            String caseId,
            PortfolioAnalysisFaPage.RegressionSummary summary
    ) {
        if (summary == null || !summary.hasPrdMismatches()) {
            return;
        }

        throw new AssertionError(
                caseId
                        + " failed PRD validation"
                        + " | bucket=" + summary.getBucket()
                        + " | prdMismatches=" + summary.getPrdMismatchCount()
                        + " | matched=" + summary.getMatchedCards()
                        + "/" + summary.getMappedCards()
                        + " | See [MISMATCH] failure lines above for expected vs actual values."
        );
    }

    private String summaryPart(
            String label,
            PortfolioAnalysisFaPage.RegressionSummary summary
    ) {
        if (summary == null) {
            return label + "=NOT COMPLETED";
        }

        return label
                + "=" + summary.getMatchedCards() + "/" + summary.getMappedCards() + " matched"
                + "," + summary.getPrdMismatchCount() + " mismatch";
    }

    private int mismatchCount(PortfolioAnalysisFaPage.RegressionSummary summary) {
        return summary == null ? 0 : summary.getPrdMismatchCount();
    }

    private int automationFailureCount(PortfolioAnalysisFaPage.RegressionSummary summary) {
        return summary == null ? 0 : summary.getAutomationFailureCount();
    }

    private void createExtentTest(
            String caseId,
            String title,
            String validation
    ) {
        ExtentTest test = ExtentManager.getExtentReports()
                .createTest(caseId + " - " + title);

        test.info(
                "Module: PortfolioAnalysisFa<br>"
                        + "Case ID: " + caseId + "<br>"
                        + "Validation: " + validation
        );

        ExtentTestManager.setTest(test);
    }

    private String safeThrowableMessage(Throwable error) {
        if (error == null || error.getMessage() == null) {
            return "unknown error";
        }
        return error.getMessage().replace('\n', ' ').replace('\r', ' ').trim();
    }

    private String safeMessage(Exception error) {
        if (error == null || error.getMessage() == null) {
            return "unknown error";
        }
        return error.getMessage().replace('\n', ' ').trim();
    }
}
