package com.valueresearch.tests;

import com.aventstack.extentreports.Status;
import com.valueresearch.base.BaseTest;
import com.valueresearch.pages.ManageSipSwpPage;
import com.valueresearch.utils.AuthHelper;
import com.valueresearch.utils.ExtentManager;
import com.valueresearch.utils.ExtentTestManager;
import com.valueresearch.utils.ReportLogger;
import org.testng.SkipException;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class ManageSipSwpTest extends BaseTest {

    private static final String DEFAULT_INVESTOR_NAME = "Manish Khatri";

    private ManageSipSwpPage manageSipSwpPage;

    @BeforeMethod(alwaysRun = true)
    public void setUpManageSipSwpPage() {
        manageSipSwpPage = new ManageSipSwpPage(driver);
    }

    @Test(priority = 1, description = "Open Manage SIP/SWP module from Hub")
    public void MSS_001_OpenManageSipSwpFromHub() {
        startExtentCase(
                "MSS_001",
                "Open Manage SIP/SWP from Hub",
                "Open Hub, tap Manage SIP/SWP, and verify Your SIPs/SWPs page opens"
        );

        ReportLogger.step("Validation: Open Hub, tap Manage SIP/SWP, and verify Your SIPs/SWPs page opens");
        ReportLogger.step("Starting test case: MSS_001 - Open Manage SIP/SWP from Hub");

        ReportLogger.step("Checking Advisor login/session");
        AuthHelper authHelper = new AuthHelper(driver);
        authHelper.ensureLoggedIn();
        ReportLogger.pass("Advisor login/session confirmed");

        runFirstAvailablePageMethod(
                "Open Manage SIP/SWP from Hub",
                method("openManageSipSwpFromHub")
        );

        ReportLogger.pass("MSS_001 - Manage SIP/SWP opened successfully");
        markExtentCasePassed("MSS_001", "Open Manage SIP/SWP from Hub");
    }

    @Test(
            priority = 2,
            dependsOnMethods = "MSS_001_OpenManageSipSwpFromHub",
            description = "Verify Your SIPs/SWPs listing page"
    )
    public void MSS_002_VerifySipSwpListingPage() {
        startExtentCase(
                "MSS_002",
                "Verify SIP/SWP listing page",
                "Validate Your SIPs/SWPs title, SIP/SWP tabs, investor dropdown and listing/empty-state data"
        );

        ReportLogger.step("Validation: Validate Your SIPs/SWPs title, SIP/SWP tabs, investor dropdown and listing/empty-state data");
        ReportLogger.step("Starting test case: MSS_002 - Verify SIP/SWP listing page");

        runFirstAvailablePageMethod(
                "Verify SIP/SWP listing page",
                method("validateManageSipSwpListingPage"),
                method("verifySipSwpListingPage")
        );

        ReportLogger.pass("MSS_002 - SIP/SWP listing page validated successfully");
        markExtentCasePassed("MSS_002", "Verify SIP/SWP listing page");
    }

    @Test(
            priority = 3,
            dependsOnMethods = "MSS_002_VerifySipSwpListingPage",
            description = "Open investor dropdown and select investor"
    )
    public void MSS_003_ChangeInvestorFromDropdown() {
        startExtentCase(
                "MSS_003",
                "Change investor from dropdown",
                "Open Choose Investor bottom sheet and select " + DEFAULT_INVESTOR_NAME
        );

        ReportLogger.step("Validation: Open Choose Investor bottom sheet and select " + DEFAULT_INVESTOR_NAME);
        ReportLogger.step("Starting test case: MSS_003 - Change investor from dropdown");

        runFirstAvailablePageMethod(
                "Change investor from dropdown",
                method("changeInvestorFromManageSipSwpDropdown", new Class<?>[]{String.class}, DEFAULT_INVESTOR_NAME),
                method("changeInvestorFromDropdown")
        );

        ReportLogger.pass("MSS_003 - Investor dropdown validated successfully");
        markExtentCasePassed("MSS_003", "Change investor from dropdown");
    }

    @Test(
            priority = 4,
            dependsOnMethods = "MSS_003_ChangeInvestorFromDropdown",
            description = "Verify SIP card data on listing"
    )
    public void MSS_004_VerifySipCardData() {
        startExtentCase(
                "MSS_004",
                "Verify SIP card data",
                "Validate visible SIP card fund name, amount pattern and tag if exposed"
        );

        ReportLogger.step("Validation: Validate visible SIP card fund name, amount pattern and tag if exposed");
        ReportLogger.step("Starting test case: MSS_004 - Verify SIP card data");

        runFirstAvailablePageMethod(
                "Verify SIP card data",
                method("verifySipCardData")
        );

        ReportLogger.pass("MSS_004 - SIP card data validated successfully");
        markExtentCasePassed("MSS_004", "Verify SIP card data");
    }

    @Test(
            priority = 5,
            dependsOnMethods = "MSS_004_VerifySipCardData",
            description = "Open SIP details and validate details"
    )
    public void MSS_005_OpenSipDetailsAndValidateDetails() {
        startExtentCase(
                "MSS_005",
                "Open SIP details and validate details",
                "Open first visible SIP card and validate SIP details page with live value capture"
        );

        ReportLogger.step("Validation: Open first visible SIP card and validate SIP details page with live value capture");
        ReportLogger.step("Starting test case: MSS_005 - Open SIP details and validate details");

        runFirstAvailablePageMethod(
                "Open SIP details and validate details",
                method("openSipDetailsAndValidateDetails")
        );

        ReportLogger.pass("MSS_005 - SIP details validated successfully");
        markExtentCasePassed("MSS_005", "Open SIP details and validate details");
    }

    @Test(
            priority = 6,
            dependsOnMethods = "MSS_005_OpenSipDetailsAndValidateDetails",
            description = "Open View past investments and validate Transaction History"
    )
    public void MSS_006_OpenPastInvestmentsAndValidateTransactionHistory() {
        startExtentCase(
                "MSS_006",
                "Open past investments and validate Transaction History",
                "Tap View past instalments/investments and validate Transaction History tabs/filters"
        );

        ReportLogger.step("Validation: Tap View past investments and validate Transaction History tabs/filters");
        ReportLogger.step("Starting test case: MSS_006 - Open past investments and validate Transaction History");

        runFirstAvailablePageMethod(
                "Open past investments and validate Transaction History",
                method("openPastInvestmentsAndValidateTransactionHistory")
        );

        ReportLogger.pass("MSS_006 - Transaction History validated successfully");
        markExtentCasePassed("MSS_006", "Open past investments and validate Transaction History");
    }

    @Test(
            priority = 7,
            dependsOnMethods = "MSS_006_OpenPastInvestmentsAndValidateTransactionHistory",
            description = "Return from Transaction History to SIP details"
    )
    public void MSS_007_ReturnBackToSipDetails() {
        startExtentCase(
                "MSS_007",
                "Return back to SIP details",
                "Press back from Transaction History and verify SIP details page is restored"
        );

        ReportLogger.step("Validation: Press back from Transaction History and verify SIP details page is restored");
        ReportLogger.step("Starting test case: MSS_007 - Return back to SIP details");

        runFirstAvailablePageMethod(
                "Return back to SIP details",
                method("returnBackToSipDetails")
        );

        ReportLogger.pass("MSS_007 - Returned back to SIP details successfully");
        markExtentCasePassed("MSS_007", "Return back to SIP details");
    }

    @Test(
            priority = 8,
            dependsOnMethods = "MSS_007_ReturnBackToSipDetails",
            description = "Return from SIP details to Your SIPs/SWPs listing"
    )
    public void MSS_008_ReturnBackToSipSwpListing() {
        startExtentCase(
                "MSS_008",
                "Return back to SIP/SWP listing",
                "Press back from SIP details and verify Your SIPs/SWPs listing page is restored"
        );

        ReportLogger.step("Validation: Press back from SIP details and verify Your SIPs/SWPs listing page is restored");
        ReportLogger.step("Starting test case: MSS_008 - Return back to SIP/SWP listing");

        runFirstAvailablePageMethod(
                "Return back to SIP/SWP listing",
                method("returnBackToSipSwpListing")
        );

        ReportLogger.pass("MSS_008 - Returned back to SIP/SWP listing successfully");
        markExtentCasePassed("MSS_008", "Return back to SIP/SWP listing");
    }

    @Test(
            priority = 9,
            dependsOnMethods = "MSS_008_ReturnBackToSipSwpListing",
            description = "Verify SWP tab switch"
    )
    public void MSS_009_VerifySwpTabSwitch() {
        startExtentCase(
                "MSS_009",
                "Verify SWP tab switch",
                "Tap SWP tab and verify SWP tab/content state without hardcoded data"
        );

        ReportLogger.step("Validation: Tap SWP tab and verify SWP tab/content state without hardcoded data");
        ReportLogger.step("Starting test case: MSS_009 - Verify SWP tab switch");

        runFirstAvailablePageMethod(
                "Verify SWP tab switch",
                method("verifySwpTabSwitch")
        );

        ReportLogger.pass("MSS_009 - SWP tab switch validated successfully");
        markExtentCasePassed("MSS_009", "Verify SWP tab switch");
    }

    @Test(
            priority = 10,
            dependsOnMethods = "MSS_009_VerifySwpTabSwitch",
            description = "Verify SIP action buttons visible only"
    )
    public void MSS_010_VerifyActionButtonsVisibleOnly() {
        startExtentCase(
                "MSS_010",
                "Verify action buttons visible only",
                "Open SIP details again and validate Invest more and Cancel SIP buttons are visible only. Cancel SIP is not tapped."
        );

        ReportLogger.step("Validation: Open SIP details again and validate Invest more and Cancel SIP buttons are visible only. Cancel SIP is not tapped.");
        ReportLogger.step("Starting test case: MSS_010 - Verify action buttons visible only");

        runFirstAvailablePageMethod(
                "Verify SIP action buttons visible only",
                method("verifyActionButtonsVisibleOnly")
        );

        ReportLogger.pass("MSS_010 - SIP action buttons visibility validated successfully");
        markExtentCasePassed("MSS_010", "Verify action buttons visible only");
    }

    @Test(
            priority = 11,
            dependsOnMethods = "MSS_010_VerifyActionButtonsVisibleOnly",
            description = "Verify strict SIP details field mapping"
    )
    public void MSS_011_VerifyStrictSipDetailsFieldMapping() {
        startExtentCase(
                "MSS_011",
                "Verify strict SIP details field mapping",
                "Validate Investor, Folio No., Amount, Frequency, No of instalments, View past instalments, Invest more and Cancel SIP with correct value mapping"
        );

        ReportLogger.step("Validation: Strictly map SIP details labels to their correct values");
        ReportLogger.step("Starting test case: MSS_011 - Verify strict SIP details field mapping");

        runFirstAvailablePageMethod(
                "Verify strict SIP details field mapping",
                method("verifyStrictSipDetailsFieldMapping")
        );

        ReportLogger.pass("MSS_011 - Strict SIP details field mapping validated successfully");
        markExtentCasePassed("MSS_011", "Verify strict SIP details field mapping");
    }

    @Test(
            priority = 12,
            dependsOnMethods = "MSS_011_VerifyStrictSipDetailsFieldMapping",
            description = "Verify past instalments list and applied filter"
    )
    public void MSS_012_VerifyPastInstalmentsListAndAppliedFilter() {
        startExtentCase(
                "MSS_012",
                "Verify past instalments list and applied filter",
                "Open View past instalments and verify Transaction History controls, applied SIP/fund/folio filters and first visible SIP transaction row"
        );

        ReportLogger.step("Validation: Verify past instalments list, applied filters and first visible transaction row");
        ReportLogger.step("Starting test case: MSS_012 - Verify past instalments list and applied filter");

        runFirstAvailablePageMethod(
                "Verify past instalments list and applied filter",
                method("verifyPastInstalmentsListAndAppliedFilter")
        );

        ReportLogger.pass("MSS_012 - Past instalments list and applied filter validated successfully");
        markExtentCasePassed("MSS_012", "Verify past instalments list and applied filter");
    }

    @Test(
            priority = 13,
            dependsOnMethods = "MSS_012_VerifyPastInstalmentsListAndAppliedFilter",
            description = "Open first past instalment and validate transaction details"
    )
    public void MSS_013_OpenFirstPastInstalmentAndValidateTransactionDetails() {
        startExtentCase(
                "MSS_013",
                "Open first past instalment and validate transaction details",
                "Open first visible SIP transaction row and validate amount, investor, transaction type, date, folio, units/source details"
        );

        ReportLogger.step("Validation: Open first past instalment and validate transaction detail page");
        ReportLogger.step("Starting test case: MSS_013 - Open first past instalment and validate transaction details");

        runFirstAvailablePageMethod(
                "Open first past instalment and validate transaction details",
                method("openFirstPastInstalmentAndValidateTransactionDetails")
        );

        ReportLogger.pass("MSS_013 - First past instalment transaction details validated successfully");
        markExtentCasePassed("MSS_013", "Open first past instalment and validate transaction details");
    }

    @Test(
            priority = 14,
            dependsOnMethods = "MSS_013_OpenFirstPastInstalmentAndValidateTransactionDetails",
            description = "Verify transaction detail action buttons visible only"
    )
    public void MSS_014_VerifyTransactionDetailActionButtonsVisibleOnly() {
        startExtentCase(
                "MSS_014",
                "Verify transaction detail action buttons visible only",
                "Verify Edit and Delete buttons are visible on transaction detail page but do not tap them"
        );

        ReportLogger.step("Validation: Verify Edit/Delete visible only. No edit/delete action will be tapped.");
        ReportLogger.step("Starting test case: MSS_014 - Verify transaction detail action buttons visible only");

        runFirstAvailablePageMethod(
                "Verify transaction detail action buttons visible only",
                method("verifyTransactionDetailActionButtonsVisibleOnly")
        );

        ReportLogger.pass("MSS_014 - Transaction detail action buttons visibility validated successfully");
        markExtentCasePassed("MSS_014", "Verify transaction detail action buttons visible only");
    }

    @Test(
            priority = 15,
            dependsOnMethods = "MSS_014_VerifyTransactionDetailActionButtonsVisibleOnly",
            description = "Return from transaction detail to SIP details"
    )
    public void MSS_015_ReturnBackFromTransactionDetailToSipDetails() {
        startExtentCase(
                "MSS_015",
                "Return from transaction detail to SIP details",
                "Press back from transaction detail/history and verify SIP details page is restored"
        );

        ReportLogger.step("Validation: Return from transaction detail to SIP details page");
        ReportLogger.step("Starting test case: MSS_015 - Return from transaction detail to SIP details");

        runFirstAvailablePageMethod(
                "Return from transaction detail to SIP details",
                method("returnBackFromTransactionDetailToSipDetails")
        );

        ReportLogger.pass("MSS_015 - Returned from transaction detail to SIP details successfully");
        markExtentCasePassed("MSS_015", "Return from transaction detail to SIP details");
    }


    @Test(
            priority = 16,
            dependsOnMethods = "MSS_015_ReturnBackFromTransactionDetailToSipDetails",
            description = "Verify Transaction History Sort bottom sheet"
    )
    public void MSS_016_VerifyTransactionHistorySortBottomSheet() {
        startExtentCase(
                "MSS_016",
                "Verify Transaction History Sort bottom sheet",
                "Open Sort from Transaction History, validate sort options/actions and close it safely"
        );

        ReportLogger.step("Validation: Open Sort bottom sheet from Transaction History and close it safely");
        ReportLogger.step("Starting test case: MSS_016 - Verify Transaction History Sort bottom sheet");

        runFirstAvailablePageMethod(
                "Verify Transaction History Sort bottom sheet",
                method("verifyTransactionHistorySortBottomSheet")
        );

        ReportLogger.pass("MSS_016 - Transaction History Sort bottom sheet validated successfully");
        markExtentCasePassed("MSS_016", "Verify Transaction History Sort bottom sheet");
    }

    @Test(
            priority = 17,
            dependsOnMethods = "MSS_016_VerifyTransactionHistorySortBottomSheet",
            description = "Verify Transaction History Filters screen"
    )
    public void MSS_017_VerifyTransactionHistoryFiltersScreen() {
        startExtentCase(
                "MSS_017",
                "Verify Transaction History Filters screen",
                "Open Filters/Add Filter from Transaction History, validate filter categories/actions and return safely"
        );

        ReportLogger.step("Validation: Open Filters screen from Transaction History and verify filter controls");
        ReportLogger.step("Starting test case: MSS_017 - Verify Transaction History Filters screen");

        runFirstAvailablePageMethod(
                "Verify Transaction History Filters screen",
                method("verifyTransactionHistoryFiltersScreen")
        );

        ReportLogger.pass("MSS_017 - Transaction History Filters screen validated successfully");
        markExtentCasePassed("MSS_017", "Verify Transaction History Filters screen");
    }

    @Test(
            priority = 18,
            dependsOnMethods = "MSS_017_VerifyTransactionHistoryFiltersScreen",
            description = "Verify Update Transactions page and return"
    )
    public void MSS_018_VerifyUpdateTransactionsPageAndReturn() {
        startExtentCase(
                "MSS_018",
                "Verify Update Transactions page and return",
                "Open Update Transactions from Transaction History, validate update/import page and return without starting import"
        );

        ReportLogger.step("Validation: Open Update Transactions page and return without starting import");
        ReportLogger.step("Starting test case: MSS_018 - Verify Update Transactions page and return");

        runFirstAvailablePageMethod(
                "Verify Update Transactions page and return",
                method("verifyUpdateTransactionsPageAndReturn")
        );

        ReportLogger.pass("MSS_018 - Update Transactions page validated successfully");
        markExtentCasePassed("MSS_018", "Verify Update Transactions page and return");
    }

    @Test(
            priority = 19,
            dependsOnMethods = "MSS_018_VerifyUpdateTransactionsPageAndReturn",
            description = "Verify Transaction History Sort apply"
    )
    public void MSS_019_VerifyTransactionHistorySortApply() {
        startExtentCase(
                "MSS_019",
                "Verify Transaction History Sort apply",
                "Open Sort, select a safe visible sort option when exposed, apply/done and verify transaction list remains valid"
        );

        ReportLogger.step("Validation: Apply a safe Sort option and verify Transaction History remains valid");
        ReportLogger.step("Starting test case: MSS_019 - Verify Transaction History Sort apply");

        runFirstAvailablePageMethod(
                "Verify Transaction History Sort apply",
                method("verifyTransactionHistorySortApply")
        );

        ReportLogger.pass("MSS_019 - Transaction History Sort apply validated successfully");
        markExtentCasePassed("MSS_019", "Verify Transaction History Sort apply");
    }

    // =========================================================
    // EXTENT REPORT HELPERS
    // Same reporting pattern as stable modules such as Tax Calculator.
    // This does not change test/app logic; it only creates the Extent node
    // before ReportLogger.step/pass starts writing details.
    // =========================================================

    private void startExtentCase(String caseId, String title, String validation) {
        ExtentTestManager.setTest(
                ExtentManager.getExtentReports().createTest(
                        caseId + " - " + title
                )
        );

        ExtentTestManager.getTest().log(
                Status.INFO,
                "<b>Module:</b> Manage SIP/SWP<br>"
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
            // Reporting should never affect test execution.
        }
    }

    // =========================================================
    // COMPATIBILITY HELPERS
    // Supports both old and new ManageSipSwpPage method names.
    // =========================================================

    private MethodCall method(String methodName) {
        return new MethodCall(methodName, new Class<?>[]{}, new Object[]{});
    }

    private MethodCall method(String methodName, Class<?>[] parameterTypes, Object... args) {
        return new MethodCall(methodName, parameterTypes, args);
    }

    private void runFirstAvailablePageMethod(String actionName, MethodCall... methodCalls) {
        Throwable lastFailure = null;

        for (MethodCall methodCall : methodCalls) {
            try {
                Method method = manageSipSwpPage
                        .getClass()
                        .getMethod(methodCall.methodName, methodCall.parameterTypes);

                ReportLogger.step("Executing page method: " + methodCall.methodName);
                method.invoke(manageSipSwpPage, methodCall.args);
                return;

            } catch (NoSuchMethodException e) {
                lastFailure = e;
            } catch (InvocationTargetException e) {
                Throwable cause = e.getCause();

                if (cause instanceof RuntimeException) {
                    throw (RuntimeException) cause;
                }

                if (cause instanceof Error) {
                    throw (Error) cause;
                }

                throw new RuntimeException("Page method failed for action: " + actionName, cause);

            } catch (Exception e) {
                throw new RuntimeException("Unable to execute page method for action: " + actionName, e);
            }
        }

        throw new SkipException(
                "No compatible ManageSipSwpPage method found for action: "
                        + actionName
                        + ". Last failure: "
                        + (lastFailure == null ? "unknown" : lastFailure.getMessage())
        );
    }

    private static class MethodCall {
        private final String methodName;
        private final Class<?>[] parameterTypes;
        private final Object[] args;

        private MethodCall(String methodName, Class<?>[] parameterTypes, Object[] args) {
            this.methodName = methodName;
            this.parameterTypes = parameterTypes;
            this.args = args;
        }
    }
}