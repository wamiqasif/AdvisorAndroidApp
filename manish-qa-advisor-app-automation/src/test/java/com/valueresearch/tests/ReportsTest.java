package com.valueresearch.tests;

import com.aventstack.extentreports.Status;
import com.valueresearch.base.BaseTest;
import com.valueresearch.pages.ReportsPage;
import com.valueresearch.utils.ExtentManager;
import com.valueresearch.utils.ExtentTestManager;
import com.valueresearch.utils.ReportLogger;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * Portfolio -> Reports module automation.
 *
 * Design notes:
 * - Every test can recover/open the Reports page independently.
 * - No hardcoded investor, financial year, or report date is used.
 * - Download tests validate the correct section-scoped control without
 *   creating a real file on the device.
 */
public class ReportsTest extends BaseTest {

    private ReportsPage reportsPage;

    @BeforeClass(alwaysRun = true)
    public void setUpReportsPage() {
        reportsPage = new ReportsPage(driver);
    }

    @Test(priority = 1, description = "Open Reports module from Portfolio")
    public void RPT_001_OpenReportsFromPortfolio() {
        startExtentCase(
                "RPT_001",
                "Open Reports from Portfolio",
                "Open Portfolio, open the Portfolio menu and tap Reports"
        );

        ReportLogger.step("Starting test case: RPT_001 - Open Reports from Portfolio");
        reportsPage.ensureReportsPageOpen();
        ReportLogger.pass("RPT_001 - Reports page opened successfully");

        markExtentCasePassed("RPT_001", "Open Reports from Portfolio");
    }

    @Test(priority = 2, description = "Verify Reports heading and primary sections")
    public void RPT_002_VerifyReportsHeaderAndPrimarySections() {
        startExtentCase(
                "RPT_002",
                "Verify Reports heading and primary sections",
                "Validate Reports, Tax Report and Holdings Statement headings"
        );

        ReportLogger.step("Starting test case: RPT_002 - Verify Reports heading and primary sections");
        reportsPage.validateReportsPageHeaderAndPrimarySections();
        ReportLogger.pass("RPT_002 - Reports header and primary sections validated");

        markExtentCasePassed("RPT_002", "Verify Reports heading and primary sections");
    }

    @Test(priority = 3, description = "Verify Tax Report card")
    public void RPT_003_VerifyTaxReportCard() {
        startExtentCase(
                "RPT_003",
                "Verify Tax Report card",
                "Validate investor selector, Financial Year value and section-scoped Download action"
        );

        ReportLogger.step("Starting test case: RPT_003 - Verify Tax Report card");
        reportsPage.validateTaxReportCard();
        ReportLogger.pass("RPT_003 - Tax Report card validated");

        markExtentCasePassed("RPT_003", "Verify Tax Report card");
    }

    @Test(priority = 4, description = "Verify Tax Report investor selector")
    public void RPT_004_VerifyTaxInvestorSelector() {
        startExtentCase(
                "RPT_004",
                "Verify Tax Report investor selector",
                "Open the Tax Report investor selection overlay and close it without changing data"
        );

        ReportLogger.step("Starting test case: RPT_004 - Verify Tax Report investor selector");
        reportsPage.validateTaxInvestorSelectorInteraction();
        ReportLogger.pass("RPT_004 - Tax Report investor selector validated");

        markExtentCasePassed("RPT_004", "Verify Tax Report investor selector");
    }

    @Test(priority = 5, description = "Verify Financial Year selector")
    public void RPT_005_VerifyFinancialYearSelector() {
        startExtentCase(
                "RPT_005",
                "Verify Financial Year selector",
                "Validate the live Financial Year format, open year options and close safely"
        );

        ReportLogger.step("Starting test case: RPT_005 - Verify Financial Year selector");
        reportsPage.validateFinancialYearSelectorInteraction();
        ReportLogger.pass("RPT_005 - Financial Year selector validated");

        markExtentCasePassed("RPT_005", "Verify Financial Year selector");
    }

    @Test(priority = 6, description = "Verify Tax Report Download action")
    public void RPT_006_VerifyTaxDownloadAction() {
        startExtentCase(
                "RPT_006",
                "Verify Tax Report Download action",
                "Validate the Download control mapped below the Tax Report heading is visible, enabled and clickable"
        );

        ReportLogger.step("Starting test case: RPT_006 - Verify Tax Report Download action");
        reportsPage.validateTaxDownloadAction();
        ReportLogger.pass("RPT_006 - Tax Report Download action validated");

        markExtentCasePassed("RPT_006", "Verify Tax Report Download action");
    }

    @Test(priority = 8, description = "Verify Holdings Statement investor selector")
    public void RPT_008_VerifyHoldingsInvestorSelector() {
        startExtentCase(
                "RPT_008",
                "Verify Holdings Statement investor selector",
                "Open the Holdings Statement investor selection overlay and close it safely"
        );

        ReportLogger.step("Starting test case: RPT_008 - Verify Holdings Statement investor selector");
        reportsPage.validateHoldingsInvestorSelectorInteraction();
        ReportLogger.pass("RPT_008 - Holdings Statement investor selector validated");

        markExtentCasePassed("RPT_008", "Verify Holdings Statement investor selector");
    }

    @Test(priority = 10, description = "Verify Holdings Statement Download action")
    public void RPT_010_VerifyHoldingsDownloadAction() {
        startExtentCase(
                "RPT_010",
                "Verify Holdings Statement Download action",
                "Validate the Download control mapped below Holdings Statement is visible, enabled and clickable"
        );

        ReportLogger.step("Starting test case: RPT_010 - Verify Holdings Statement Download action");
        reportsPage.validateHoldingsDownloadAction();
        ReportLogger.pass("RPT_010 - Holdings Statement Download action validated");

        markExtentCasePassed("RPT_010", "Verify Holdings Statement Download action");
    }

    @Test(priority = 11, description = "Verify Transaction History section")
    public void RPT_011_VerifyTransactionHistorySection() {
        startExtentCase(
                "RPT_011",
                "Verify Transaction History section",
                "Scroll to Transaction History and validate its section-scoped Download action"
        );

        ReportLogger.step("Starting test case: RPT_011 - Verify Transaction History section");
        reportsPage.validateTransactionHistorySection();
        ReportLogger.pass("RPT_011 - Transaction History section validated");

        markExtentCasePassed("RPT_011", "Verify Transaction History section");
    }

    @Test(priority = 12, description = "Verify Transaction History controls")
    public void RPT_012_VerifyTransactionHistoryControls() {
        startExtentCase(
                "RPT_012",
                "Verify Transaction History controls",
                "Validate investor and date controls exposed below Transaction History"
        );

        ReportLogger.step("Starting test case: RPT_012 - Verify Transaction History controls");
        reportsPage.validateTransactionHistoryControls();
        ReportLogger.pass("RPT_012 - Transaction History controls validated");

        markExtentCasePassed("RPT_012", "Verify Transaction History controls");
    }

    @Test(priority = 13, description = "Verify Transaction History investor selector")
    public void RPT_013_VerifyTransactionHistoryInvestorSelector() {
        startExtentCase(
                "RPT_013",
                "Verify Transaction History investor selector",
                "Open the Transaction History investor selection overlay and close it safely"
        );

        ReportLogger.step("Starting test case: RPT_013 - Verify Transaction History investor selector");
        reportsPage.validateTransactionHistoryInvestorSelectorInteraction();
        ReportLogger.pass("RPT_013 - Transaction History investor selector validated");

        markExtentCasePassed("RPT_013", "Verify Transaction History investor selector");
    }

    @Test(priority = 14, description = "Verify Transaction History Download action")
    public void RPT_014_VerifyTransactionHistoryDownloadAction() {
        startExtentCase(
                "RPT_014",
                "Verify Transaction History Download action",
                "Validate the Download control mapped below Transaction History is visible, enabled and clickable"
        );

        ReportLogger.step("Starting test case: RPT_014 - Verify Transaction History Download action");
        reportsPage.validateTransactionHistoryDownloadAction();
        ReportLogger.pass("RPT_014 - Transaction History Download action validated");

        markExtentCasePassed("RPT_014", "Verify Transaction History Download action");
    }

    @Test(priority = 15, description = "Verify all report sections and Downloads")
    public void RPT_015_VerifyAllReportSectionsAndDownloads() {
        startExtentCase(
                "RPT_015",
                "Verify all report sections and Downloads",
                "Validate Tax Report, Holdings Statement and Transaction History with their own Download controls"
        );

        ReportLogger.step("Starting test case: RPT_015 - Verify all report sections and Downloads");
        reportsPage.validateAllReportSectionsAndDownloadActions();
        ReportLogger.pass("RPT_015 - All report sections and Download actions validated");

        markExtentCasePassed("RPT_015", "Verify all report sections and Downloads");
    }

    @Test(priority = 16, description = "Verify back navigation and Reports reopen")
    public void RPT_016_VerifyBackNavigationAndReopen() {
        startExtentCase(
                "RPT_016",
                "Verify back navigation and Reports reopen",
                "Return from Reports to Portfolio and reopen Reports in the same Appium session"
        );

        ReportLogger.step("Starting test case: RPT_016 - Verify back navigation and Reports reopen");
        reportsPage.verifyBackNavigationAndReopen();
        ReportLogger.pass("RPT_016 - Back navigation and reopen flow validated");

        markExtentCasePassed("RPT_016", "Verify back navigation and Reports reopen");
    }

    // =========================================================
    // Extent report helpers
    // =========================================================

    private void startExtentCase(String caseId, String title, String validation) {
        ExtentTestManager.setTest(
                ExtentManager.getExtentReports().createTest(caseId + " - " + title)
        );

        ExtentTestManager.getTest().log(
                Status.INFO,
                "<b>Module:</b> Portfolio Reports<br>"
                        + "<b>Case ID:</b> " + caseId + "<br>"
                        + "<b>Scenario:</b> " + title + "<br>"
                        + "<b>Validation:</b> " + validation
        );
    }

    private void markExtentCasePassed(String caseId, String title) {
        try {
            if (ExtentTestManager.getTest() != null) {
                ExtentTestManager.getTest().pass(
                        "<span class='badge white-text green'>"
                                + caseId
                                + " - "
                                + title
                                + " completed successfully</span>"
                );
            }
        } catch (Exception ignored) {
            // Reporting must not affect test execution.
        }
    }
}