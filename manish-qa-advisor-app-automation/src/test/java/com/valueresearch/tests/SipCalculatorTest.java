package com.valueresearch.tests;

import com.aventstack.extentreports.Status;
import com.valueresearch.base.BaseTest;
import com.valueresearch.pages.SipCalculatorPage;
import com.valueresearch.utils.AuthHelper;
import com.valueresearch.utils.ExtentManager;
import com.valueresearch.utils.ExtentTestManager;
import com.valueresearch.utils.ReportLogger;
import org.testng.annotations.Test;

public class SipCalculatorTest extends BaseTest {

    /*
     * Same approach as TaxCalculatorTest:
     * First SIP test opens calculator from Hub.
     * Next SIP tests reuse/reset SIP Calculator screen.
     */
    private static boolean firstSipCase = true;

    @Test
    public void SIP_POS_001_DefaultValidSipOnly() {
        runCase(new SipTestCase(
                "SIP_POS_001",
                "Default valid SIP only",
                "5000",
                "0",
                "10",
                true
        ));
    }

    @Test
    public void SIP_POS_002_ValidMonthlySipWithLumpsum() {
        runCase(new SipTestCase(
                "SIP_POS_002",
                "Valid monthly SIP with lumpsum",
                "10000",
                "50000",
                "15",
                true
        ));
    }

    @Test
    public void SIP_POS_003_LowMonthlySipWithValidLumpsum() {
        runCase(new SipTestCase(
                "SIP_POS_003",
                "Low monthly SIP with valid lumpsum",
                "500",
                "10000",
                "5",
                true
        ));
    }

    @Test
    public void SIP_POS_004_HighMonthlySipAndHighLumpsum() {
        runCase(new SipTestCase(
                "SIP_POS_004",
                "High monthly SIP and high lumpsum",
                "100000",
                "1000000",
                "20",
                true
        ));
    }

    @Test
    public void SIP_POS_005_VeryHighMonthlySipWithZeroLumpsum() {
        runCase(new SipTestCase(
                "SIP_POS_005",
                "Very high monthly SIP with zero lumpsum",
                "500000",
                "0",
                "25",
                true
        ));
    }

    @Test
    public void SIP_POS_006_OnlyLumpsumInvestmentWithZeroMonthlySip() {
        runCase(new SipTestCase(
                "SIP_POS_006",
                "Only lumpsum investment with zero monthly SIP",
                "0",
                "500000",
                "10",
                true
        ));
    }

    @Test
    public void SIP_POS_007_SmallValidValues() {
        runCase(new SipTestCase(
                "SIP_POS_007",
                "Small valid values",
                "100",
                "100",
                "1",
                true
        ));
    }

    @Test
    public void SIP_POS_008_LargeValidValues() {
        runCase(new SipTestCase(
                "SIP_POS_008",
                "Large valid values",
                "250000",
                "2500000",
                "30",
                true
        ));
    }

    @Test
    public void SIP_EDGE_001_MinimumYearValue() {
        runCase(new SipTestCase(
                "SIP_EDGE_001",
                "Minimum year value",
                "5000",
                "10000",
                "1",
                true
        ));
    }

    @Test
    public void SIP_EDGE_002_LongDurationValue() {
        runCase(new SipTestCase(
                "SIP_EDGE_002",
                "Long duration value",
                "5000",
                "10000",
                "50",
                true
        ));
    }

    @Test
    public void SIP_EDGE_003_MonthlyAmountOneDigit() {
        runCase(new SipTestCase(
                "SIP_EDGE_003",
                "Monthly amount one digit",
                "1",
                "1000",
                "5",
                true
        ));
    }

    @Test
    public void SIP_EDGE_004_LumpsumAmountOneDigit() {
        runCase(new SipTestCase(
                "SIP_EDGE_004",
                "Lumpsum amount one digit",
                "1000",
                "1",
                "5",
                true
        ));
    }

    @Test
    public void SIP_EDGE_005_BothMonthlyAndLumpsumAreZero() {
        runCase(new SipTestCase(
                "SIP_EDGE_005",
                "Both monthly and lumpsum are zero",
                "0",
                "0",
                "5",
                false
        ));
    }

    @Test
    public void SIP_EDGE_006_YearValueZero() {
        runCase(new SipTestCase(
                "SIP_EDGE_006",
                "Year value zero",
                "5000",
                "10000",
                "0",
                false
        ));
    }

    @Test
    public void SIP_NEG_001_EmptyMonthlyInvestment() {
        runCase(new SipTestCase(
                "SIP_NEG_001",
                "Empty monthly investment",
                "",
                "10000",
                "5",
                true
        ));
    }

    @Test
    public void SIP_NEG_002_EmptyLumpsumAmount() {
        runCase(new SipTestCase(
                "SIP_NEG_002",
                "Empty lumpsum amount",
                "5000",
                "",
                "5",
                true
        ));
    }

    @Test
    public void SIP_NEG_003_EmptyTimeYears() {
        runCase(new SipTestCase(
                "SIP_NEG_003",
                "Empty time years",
                "5000",
                "10000",
                "",
                false
        ));
    }

    @Test
    public void SIP_NEG_004_AllFieldsEmpty() {
        runCase(new SipTestCase(
                "SIP_NEG_004",
                "All fields empty",
                "",
                "",
                "",
                false
        ));
    }

    @Test
    public void SIP_NEG_005_NegativeMonthlyInvestment() {
        runCase(new SipTestCase(
                "SIP_NEG_005",
                "Negative monthly investment",
                "-5000",
                "10000",
                "5",
                false
        ));
    }

    @Test
    public void SIP_NEG_006_NegativeLumpsumAmount() {
        runCase(new SipTestCase(
                "SIP_NEG_006",
                "Negative lumpsum amount",
                "5000",
                "-10000",
                "5",
                false
        ));
    }

    @Test
    public void SIP_NEG_007_NegativeTimeYears() {
        runCase(new SipTestCase(
                "SIP_NEG_007",
                "Negative time years",
                "5000",
                "10000",
                "-5",
                false
        ));
    }

    @Test
    public void SIP_NEG_008_DecimalMonthlyInvestment() {
        runCase(new SipTestCase(
                "SIP_NEG_008",
                "Decimal monthly investment",
                "5000.50",
                "10000",
                "5",
                false
        ));
    }

    @Test
    public void SIP_NEG_009_DecimalLumpsumAmount() {
        runCase(new SipTestCase(
                "SIP_NEG_009",
                "Decimal lumpsum amount",
                "5000",
                "10000.50",
                "5",
                false
        ));
    }

    @Test
    public void SIP_NEG_010_DecimalTimeYears() {
        runCase(new SipTestCase(
                "SIP_NEG_010",
                "Decimal time years",
                "5000",
                "10000",
                "5.5",
                false
        ));
    }

    @Test
    public void SIP_NEG_011_AlphabeticMonthlyInvestment() {
        runCase(new SipTestCase(
                "SIP_NEG_011",
                "Alphabetic monthly investment",
                "abc",
                "10000",
                "5",
                false
        ));
    }

    @Test
    public void SIP_NEG_012_AlphabeticLumpsumAmount() {
        runCase(new SipTestCase(
                "SIP_NEG_012",
                "Alphabetic lumpsum amount",
                "5000",
                "abc",
                "5",
                false
        ));
    }

    @Test
    public void SIP_NEG_013_AlphabeticTimeYears() {
        runCase(new SipTestCase(
                "SIP_NEG_013",
                "Alphabetic time years",
                "5000",
                "10000",
                "abc",
                false
        ));
    }

    @Test
    public void SIP_NEG_014_SpecialCharactersInMonthlyInvestment() {
        runCase(new SipTestCase(
                "SIP_NEG_014",
                "Special characters in monthly investment",
                "@@@",
                "10000",
                "5",
                false
        ));
    }

    @Test
    public void SIP_NEG_015_SpecialCharactersInLumpsumAmount() {
        runCase(new SipTestCase(
                "SIP_NEG_015",
                "Special characters in lumpsum amount",
                "5000",
                "@@@",
                "5",
                false
        ));
    }

    @Test
    public void SIP_NEG_016_SpecialCharactersInTimeYears() {
        runCase(new SipTestCase(
                "SIP_NEG_016",
                "Special characters in time years",
                "5000",
                "10000",
                "@@@",
                false
        ));
    }

    @Test
    public void SIP_FMT_001_AmountWithCommaInMonthlyInvestment() {
        runCase(new SipTestCase(
                "SIP_FMT_001",
                "Amount with comma in monthly investment",
                "5,000",
                "10000",
                "5",
                false
        ));
    }

    @Test
    public void SIP_FMT_002_AmountWithCommaInLumpsum() {
        runCase(new SipTestCase(
                "SIP_FMT_002",
                "Amount with comma in lumpsum",
                "5000",
                "10,000",
                "5",
                false
        ));
    }

    @Test
    public void SIP_FMT_003_AmountWithRupeeSymbolInMonthlyInvestment() {
        runCase(new SipTestCase(
                "SIP_FMT_003",
                "Amount with rupee symbol in monthly investment",
                "₹5000",
                "10000",
                "5",
                false
        ));
    }

    @Test
    public void SIP_FMT_004_YearsWithText() {
        runCase(new SipTestCase(
                "SIP_FMT_004",
                "Years with text",
                "5000",
                "10000",
                "5 years",
                false
        ));
    }

    @Test
    public void SIP_BOUNDARY_001_VeryLargeMonthlyAmount() {
        runCase(new SipTestCase(
                "SIP_BOUNDARY_001",
                "Very large monthly amount",
                "999999999",
                "10000",
                "10",
                true
        ));
    }

    @Test
    public void SIP_BOUNDARY_002_VeryLargeLumpsumAmount() {
        runCase(new SipTestCase(
                "SIP_BOUNDARY_002",
                "Very large lumpsum amount",
                "10000",
                "999999999",
                "10",
                true
        ));
    }

    @Test
    public void SIP_BOUNDARY_003_VeryLargeYearValue() {
        runCase(new SipTestCase(
                "SIP_BOUNDARY_003",
                "Very large year value",
                "10000",
                "10000",
                "999",
                true
        ));
    }

    private void runCase(SipTestCase testCase) {
        ExtentTestManager.setTest(
                ExtentManager.getExtentReports().createTest(
                        testCase.getCaseId() + " - " + testCase.getDescription()
                )
        );

        ExtentTestManager.getTest().log(
                Status.INFO,
                "<b>Module:</b> SIP Calculator<br>"
                        + "<b>Case ID:</b> " + testCase.getCaseId() + "<br>"
                        + "<b>Scenario:</b> " + testCase.getDescription() + "<br>"
                        + "<b>Validation:</b> "
                        + (testCase.shouldCalculate()
                        ? "SIP calculation should proceed for given input"
                        : "SIP calculation should not proceed for invalid/edge input")
        );

        ReportLogger.step(
                "Starting test case: "
                        + testCase.getCaseId()
                        + " - "
                        + testCase.getDescription()
        );

        SipCalculatorPage sipCalculatorPage = new SipCalculatorPage(driver);

        if (firstSipCase) {
            ReportLogger.step("Checking Advisor login/session");

            AuthHelper authHelper = new AuthHelper(driver);
            authHelper.ensureLoggedIn();

            ReportLogger.pass("Advisor login/session confirmed");

            ReportLogger.step("Opening SIP Calculator from Hub");

            sipCalculatorPage.openSipCalculatorFromHub();

            ReportLogger.pass("SIP Calculator opened from Hub");

            firstSipCase = false;
        } else {
            ReportLogger.step("Preparing fresh SIP Calculator for next test case");

            sipCalculatorPage.prepareFreshSipCalculatorForNextCase();

            ReportLogger.pass("Fresh SIP Calculator ready for next test case");
        }

        sipCalculatorPage.runSipCase(
                testCase.getCaseId(),
                testCase.getMonthlyAmount(),
                testCase.getLumpsumAmount(),
                testCase.getYears(),
                testCase.shouldCalculate()
        );

        ExtentTestManager.getTest().pass(
                "<span class='badge white-text green'>"
                        + testCase.getCaseId()
                        + " - "
                        + testCase.getDescription()
                        + " completed successfully</span>"
        );

        ReportLogger.pass(
                "Completed test case: "
                        + testCase.getCaseId()
                        + " - "
                        + testCase.getDescription()
        );

        /*
         * Do not call ExtentTestManager.unload() here.
         * Listener will unload at suite finish.
         */
    }

    private static class SipTestCase {

        private final String caseId;
        private final String description;
        private final String monthlyAmount;
        private final String lumpsumAmount;
        private final String years;
        private final boolean shouldCalculate;

        public SipTestCase(
                String caseId,
                String description,
                String monthlyAmount,
                String lumpsumAmount,
                String years,
                boolean shouldCalculate
        ) {
            this.caseId = caseId;
            this.description = description;
            this.monthlyAmount = monthlyAmount;
            this.lumpsumAmount = lumpsumAmount;
            this.years = years;
            this.shouldCalculate = shouldCalculate;
        }

        public String getCaseId() {
            return caseId;
        }

        public String getDescription() {
            return description;
        }

        public String getMonthlyAmount() {
            return monthlyAmount;
        }

        public String getLumpsumAmount() {
            return lumpsumAmount;
        }

        public String getYears() {
            return years;
        }

        public boolean shouldCalculate() {
            return shouldCalculate;
        }

        public String getReportName() {
            return caseId + " - " + description;
        }

        public String getExpectedActionText() {
            return shouldCalculate ? "Calculate should be allowed" : "Calculate should not be allowed";
        }
    }
}