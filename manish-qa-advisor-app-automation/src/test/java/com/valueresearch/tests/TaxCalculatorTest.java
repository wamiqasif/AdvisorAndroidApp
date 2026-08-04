package com.valueresearch.tests;

import com.aventstack.extentreports.Status;
import com.valueresearch.base.BaseTest;
import com.valueresearch.pages.TaxCalculatorPage;
import com.valueresearch.utils.AuthHelper;
import com.valueresearch.utils.ExtentManager;
import com.valueresearch.utils.ExtentTestManager;
import com.valueresearch.utils.ReportLogger;
import org.testng.annotations.Test;

public class TaxCalculatorTest extends BaseTest {

    /*
     * Important:
     * Keep this true only for the first tax test.
     * First test opens Tax Calculator from Hub.
     * Next tests reuse app state and reopen Tax Calculator using Exit -> Help me calculate this.
     */
    private static boolean firstTaxCase = true;

    @Test
    public void TAX_001_FY2027_WithAllValueFill() {
        runTaxCalculatorCase(new TaxData(
                "TAX_001",
                "FY 2027 with all value fill",
                "All fields filled with FY 2024-25 data",
                "31", "1200000", "25000", "10000",
                "60000", "7200", "7200", "5000", "5000", "10000",
                true, "20000", "15000",
                true, "50000", "100000",
                "10000", "50000", "25000", "10000", "10000", "20000", "5000",
                true, "5000", "10000", "30"
        ));
    }

    @Test
    public void TAX_002_FY2027_WithAllValueFill() {
        runTaxCalculatorCase(new TaxData(
                "TAX_002",
                "FY 2027 with all value fill",
                "All fields filled with FY 2025-26 data",
                "32", "1500000", "30000", "15000",
                "75000", "9000", "9000", "6000", "6000", "12000",
                true, "25000", "18000",
                true, "60000", "120000",
                "15000", "60000", "30000", "12000", "15000", "25000", "7000",
                true, "6000", "12000", "100"
        ));
    }

    @Test
    public void TAX_003_PrivateJobEmployee() {
        runTaxCalculatorCase(new TaxData(
                "TAX_003",
                "Private job employee",
                "Private employee tax calculator flow",
                "35", "900000", "12000", "5000",
                "45000", "5400", "5400", "0", "0", "0",
                true, "15000", "10000",
                false, "0", "0",
                "20000", "15000", "10000", "0", "0", "0", "0",
                false, "0", "0", "0"
        ));
    }

    @Test
    public void TAX_004_FY2027WithoutHomeLoan() {
        runTaxCalculatorCase(new TaxData(
                "TAX_004",
                "FY 2027 without home loan",
                "Home loan skipped",
                "34", "850000", "10000", "5000",
                "42000", "5040", "5040", "2000", "2000", "5000",
                true, "12000", "8000",
                false, "0", "0",
                "10000", "10000", "10000", "0", "0", "0", "0",
                false, "0", "0", "0"
        ));
    }

    @Test
    public void TAX_005_FY2027_WithoutHealthPremium() {
        runTaxCalculatorCase(new TaxData(
                "TAX_005",
                "FY 2027 without health premium",
                "Health premium not selected",
                "36", "950000", "12000", "5000",
                "48000", "5760", "5760", "3000", "3000", "6000",
                true, "16000", "9000",
                true, "30000", "70000",
                "12000", "20000", "10000", "5000", "5000", "0", "0",
                false, "0", "0", "0"
        ));
    }

    @Test
    public void TAX_006_FY2027_MinimumValueFill() {
        runTaxCalculatorCase(new TaxData(
                "TAX_006",
                "FY 2027 minimum value fill",
                "Minimum required values only",
                "31", "800000", "0", "0",
                "40000", "0", "0", "0", "0", "0",
                false, "0", "0",
                false, "0", "0",
                "0", "0", "0", "0", "0", "0", "0",
                true, "4000", "8000", "101"
        ));
    }

    @Test
    public void TAX_007_70YearOld_FY2027() {
        runTaxCalculatorCase(new TaxData(
                "TAX_007",
                "70 year old FY 2027",
                "Senior citizen age 70 scenario",
                "70", "3000000", "500000", "100000",
                "120000", "14400", "14400", "10000", "10000", "50000",
                true, "15000", "62500",
                true, "264133", "335867",
                "0", "0", "0", "0", "0", "0", "150000",
                false, "0", "0", "0"
        ));
    }

    @Test
    public void TAX_008_25YearOld_FY2027() {
        runTaxCalculatorCase(new TaxData(
                "TAX_008",
                "25 year old FY 2027",
                "Young taxpayer age 25 scenario",
                "25", "600000", "5000", "0",
                "30000", "1800", "1800", "0", "0", "0",
                false, "0", "0",
                false, "0", "0",
                "10000", "5000", "5000", "0", "0", "0", "0",
                false, "0", "0", "0"
        ));
    }

    @Test
    public void TAX_009_FY2027_HRANo_HomeLoanNo() {
        runTaxCalculatorCase(new TaxData(
                "TAX_009",
                "FY 2027 HRA No Home Loan No",
                "HRA No and Home Loan No",
                "33", "700000", "8000", "2000",
                "35000", "2100", "2100", "0", "0", "0",
                false, "0", "0",
                false, "0", "0",
                "10000", "5000", "5000", "0", "0", "0", "0",
                false, "0", "0", "0"
        ));
    }

    @Test
    public void TAX_010_FY2027_NonGovEmployee() {
        runTaxCalculatorCase(new TaxData(
                "TAX_010",
                "FY 2027 Non Gov Employee",
                "NPS entered and government employee No selected",
                "38", "1100000", "18000", "6000",
                "55000", "6600", "6600", "3000", "3000", "7000",
                true, "18000", "12000",
                false, "0", "0",
                "12000", "20000", "10000", "5000", "5000", "0", "0",
                false, "0", "0", "0"
        ));
    }

    @Test
    public void TAX_011_FY2027_MetroCityNo() {
        runTaxCalculatorCase(new TaxData(
                "TAX_011",
                "FY 2027 Metro City No",
                "HRA flow with metro city No scenario",
                "39", "1250000", "20000", "8000",
                "62500", "7500", "7500", "4000", "4000", "10000",
                true, "22000", "14000",
                true, "50000", "90000",
                "15000", "30000", "15000", "5000", "5000", "10000", "0",
                false, "0", "0", "0"
        ));
    }

    @Test
    public void TAX_012_FY2027_WithoutHomeLoan() {
        runTaxCalculatorCase(new TaxData(
                "TAX_012",
                "FY 2027 without home loan",
                "Home loan skipped for FY 2025-26",
                "40", "1300000", "22000", "10000",
                "65000", "7800", "7800", "5000", "5000", "12000",
                true, "20000", "15000",
                false, "0", "0",
                "20000", "30000", "15000", "10000", "10000", "0", "0",
                false, "0", "0", "0"
        ));
    }

    @Test
    public void TAX_013_SeniorCitizen60_FY2027() {
        runTaxCalculatorCase(new TaxData(
                "TAX_013",
                "Senior Citizen 60 FY 2027",
                "Senior citizen age 60 scenario",
                "60", "1400000", "30000", "10000",
                "70000", "8400", "8400", "5000", "5000", "15000",
                true, "18000", "12000",
                true, "60000", "100000",
                "20000", "40000", "20000", "10000", "10000", "0", "0",
                false, "0", "0", "0"
        ));
    }

    @Test
    public void TAX_014_SuperSeniorCitizen80_FY2027() {
        runTaxCalculatorCase(new TaxData(
                "TAX_014",
                "Super Senior Citizen 80 FY 2027",
                "Super senior citizen age 80 scenario",
                "80", "1800000", "40000", "20000",
                "90000", "10800", "10800", "7000", "7000", "20000",
                true, "20000", "15000",
                true, "80000", "150000",
                "30000", "50000", "30000", "10000", "10000", "0", "0",
                false, "0", "0", "0"
        ));
    }

    @Test
    public void TAX_015_HighIncome_FY2027() {
        runTaxCalculatorCase(new TaxData(
                "TAX_015",
                "High Income FY 2027",
                "High income taxpayer scenario",
                "45", "5000000", "700000", "250000",
                "250000", "30000", "30000", "25000", "25000", "75000",
                true, "50000", "100000",
                true, "500000", "800000",
                "50000", "100000", "50000", "25000", "25000", "50000", "100000",
                false, "0", "0", "0"
        ));
    }

    @Test
    public void TAX_016_FY2027_HRANo_MetroNo_HomeLoanNo() {
        runTaxCalculatorCase(new TaxData(
                "TAX_016",
                "FY 2027 HRA No Metro No Home Loan No",
                "HRA No, metro No, home loan No",
                "33", "550000", "3000", "2000",
                "28000", "1680", "1680", "0", "0", "0",
                false, "0", "0",
                false, "0", "0",
                "15000", "5000", "8000", "0", "0", "0", "0",
                false, "0", "0", "0"
        ));
    }

    @Test
    public void TAX_017_FY2027_WithoutHealthInsurance() {
        runTaxCalculatorCase(new TaxData(
                "TAX_017",
                "FY 2027 without health insurance",
                "Health insurance skipped",
                "42", "1000000", "15000", "5000",
                "50000", "3000", "3000", "4000", "4000", "8000",
                true, "18000", "9000",
                false, "0", "0",
                "60000", "15000", "20000", "10000", "30000", "25000", "8000",
                true, "8000", "20000", "120"
        ));
    }

    @Test
    public void TAX_018_FY2027_HomeLoanDirect_NoEMICalc() {
        runTaxCalculatorCase(new TaxData(
                "TAX_018",
                "FY 2027 Home Loan Direct No EMI Calc",
                "Home loan direct values without EMI calculation",
                "37", "1600000", "25000", "10000",
                "80000", "9600", "9600", "5000", "5000", "15000",
                true, "20000", "15000",
                true, "100000", "200000",
                "25000", "35000", "20000", "10000", "10000", "10000", "5000",
                true, "10000", "10000", "111"
        ));
    }

    private void runTaxCalculatorCase(TaxData data) {
        /*
         * IMPORTANT:
         * This is the only place where Extent test node is created.
         * Listener must not create test node in onTestStart.
         * Do not call ExtentTestManager.unload() here.
         */
        ExtentTestManager.setTest(
                ExtentManager.getExtentReports().createTest(
                        data.caseId + " - " + data.title
                )
        );

        ExtentTestManager.getTest().log(
                Status.INFO,
                "<b>Module:</b> Tax Calculator<br>"
                        + "<b>Case ID:</b> " + data.caseId + "<br>"
                        + "<b>Scenario:</b> " + data.title + "<br>"
                        + "<b>Validation:</b> " + data.validation
        );

        ReportLogger.step("Starting test case: " + data.caseId + " - " + data.title);

        TaxCalculatorPage taxCalculatorPage = new TaxCalculatorPage(driver);

        if (firstTaxCase) {
            ReportLogger.step("Checking Advisor login/session");

            AuthHelper authHelper = new AuthHelper(driver);
            authHelper.ensureLoggedIn();

            ReportLogger.pass("Advisor login/session confirmed");

            ReportLogger.step("Opening Tax Calculator from Hub");

            taxCalculatorPage.openTaxCalculatorFromHub();

            ReportLogger.pass("Tax Calculator opened from Hub");

            firstTaxCase = false;
        } else {
            ReportLogger.step("Preparing fresh Tax Calculator for next test case");

            taxCalculatorPage.exitAndOpenFreshTaxCalculatorForNextCase();

            ReportLogger.pass("Fresh Tax Calculator ready for next test case");
        }

        taxCalculatorPage.runTaxCalculatorCase(data);

        ExtentTestManager.getTest().pass(
                "<span class='badge white-text green'>"
                        + data.caseId
                        + " - "
                        + data.title
                        + " completed successfully</span>"
        );

        ReportLogger.pass("Completed test case: " + data.caseId + " - " + data.title);

        /*
         * DO NOT call ExtentTestManager.unload() here.
         * If you unload here and test fails, listener creates duplicate fallback entries.
         */
    }

    public static class TaxData {
        public final String caseId;
        public final String title;
        public final String validation;

        public final String age;
        public final String annualIncome;
        public final String annualInterest;
        public final String otherTaxableIncome;

        public final String basicSalary;
        public final String epfEmployee;
        public final String epfEmployer;
        public final String npsEmployee;
        public final String npsEmployer;
        public final String personalNps;

        public final boolean hraYes;
        public final String monthlyRent;
        public final String monthlyHra;

        public final boolean homeLoanYes;
        public final String homeLoanInterest;
        public final String homeLoanPrincipalAmount;

        public final String lifeInsurance;
        public final String elss;
        public final String ppf;
        public final String nsc;
        public final String fixedDeposit;
        public final String tuitionFee;
        public final String section80AnyOther;

        /*
         * IMPORTANT:
         * This boolean is Health Insurance Premium Yes/No.
         * It does NOT mean replace Donation / Education Loan / Any Other.
         *
         * Example:
         * true, "6000", "12000", "3000"
         *
         * Means:
         * Health Insurance Premium = Yes
         * Health Insurance Premium Field 1 = 6000
         * Health Insurance Premium Field 2 = 12000
         * Health Insurance Premium Field 3 = 3000
         */
        public final boolean healthInsurancePremiumYes;
        public final String healthInsurancePremiumField1;
        public final String healthInsurancePremiumField2;
        public final String healthInsurancePremiumField3;

        public TaxData(
                String caseId,
                String title,
                String validation,

                String age,
                String annualIncome,
                String annualInterest,
                String otherTaxableIncome,

                String basicSalary,
                String epfEmployee,
                String epfEmployer,
                String npsEmployee,
                String npsEmployer,
                String personalNps,

                boolean hraYes,
                String monthlyRent,
                String monthlyHra,

                boolean homeLoanYes,
                String homeLoanInterest,
                String homeLoanPrincipalAmount,

                String lifeInsurance,
                String elss,
                String ppf,
                String nsc,
                String fixedDeposit,
                String tuitionFee,
                String section80AnyOther,

                boolean healthInsurancePremiumYes,
                String healthInsurancePremiumField1,
                String healthInsurancePremiumField2,
                String healthInsurancePremiumField3
        ) {
            this.caseId = caseId;
            this.title = title;
            this.validation = validation;

            this.age = age;
            this.annualIncome = annualIncome;
            this.annualInterest = annualInterest;
            this.otherTaxableIncome = otherTaxableIncome;

            this.basicSalary = basicSalary;
            this.epfEmployee = epfEmployee;
            this.epfEmployer = epfEmployer;
            this.npsEmployee = npsEmployee;
            this.npsEmployer = npsEmployer;
            this.personalNps = personalNps;

            this.hraYes = hraYes;
            this.monthlyRent = monthlyRent;
            this.monthlyHra = monthlyHra;

            this.homeLoanYes = homeLoanYes;
            this.homeLoanInterest = homeLoanInterest;
            this.homeLoanPrincipalAmount = homeLoanPrincipalAmount;

            this.lifeInsurance = lifeInsurance;
            this.elss = elss;
            this.ppf = ppf;
            this.nsc = nsc;
            this.fixedDeposit = fixedDeposit;
            this.tuitionFee = tuitionFee;
            this.section80AnyOther = section80AnyOther;

            this.healthInsurancePremiumYes = healthInsurancePremiumYes;
            this.healthInsurancePremiumField1 = healthInsurancePremiumField1;
            this.healthInsurancePremiumField2 = healthInsurancePremiumField2;
            this.healthInsurancePremiumField3 = healthInsurancePremiumField3;
        }
    }
}