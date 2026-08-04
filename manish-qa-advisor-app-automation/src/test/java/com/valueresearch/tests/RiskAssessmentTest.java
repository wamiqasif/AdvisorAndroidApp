package com.valueresearch.tests;

import com.aventstack.extentreports.Status;
import com.valueresearch.base.BaseTest;
import com.valueresearch.pages.RiskAssessmentPage;
import com.valueresearch.utils.AuthHelper;
import com.valueresearch.utils.ExtentManager;
import com.valueresearch.utils.ExtentTestManager;
import com.valueresearch.utils.ReportLogger;
import org.testng.Assert;
import org.testng.SkipException;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class RiskAssessmentTest extends BaseTest {

    private static boolean firstRiskAssessmentCase = true;
    private static boolean appiumSessionBroken = false;

    @DataProvider(name = "riskAssessmentPrdCases")
    public Object[][] riskAssessmentPrdCases() {
        return new Object[][]{

                {
                        "RA_POS_001",
                        "Moderate investor complete flow",
                        "Positive",
                        "P0",
                        "22",
                        "₹5 lakh to ₹10 lakh",
                        "₹10 lakh to ₹25 lakh",
                        "₹10 lakh to ₹25 lakh",
                        "75% to 90%",
                        "Retirement",
                        "Steady returns, with some ups and downs",
                        "Withdraw ₹35,000 and leave the rest invested",
                        2.0,
                        "Moderate"
                },

                {
                        "RA_POS_002",
                        "Conservative investor low-risk flow",
                        "Positive",
                        "P0",
                        "55",
                        "Less than ₹5 lakh",
                        "Less than ₹5 lakh",
                        "Nothing — I have no loans",
                        "Less than 75%",
                        "Retirement",
                        "Keeping my money safe, even if returns are low",
                        "Withdraw the full ₹70,000 to stop further losses",
                        1.5,
                        "Conservative"
                },

                {
                        "RA_POS_003",
                        "Aggressive investor high-risk flow",
                        "Positive",
                        "P0",
                        "28",
                        "More than ₹1 crore",
                        "More than ₹1 crore",
                        "Nothing — I have no loans",
                        "Less than 75%",
                        "Long-term wealth",
                        "High returns, even when values swing sharply",
                        "Stay invested and continue my SIPs",
                        3.0,
                        "Aggressive"
                },

                {
                        "RA_SCORE_001",
                        "Score up to 1.30 maps to Conservative",
                        "Boundary",
                        "P0",
                        "30",
                        "₹5 lakh to ₹10 lakh",
                        "₹10 lakh to ₹25 lakh",
                        "Less than ₹5 lakh",
                        "More than 90%",
                        "Retirement",
                        "Keeping my money safe, even if returns are low",
                        "Withdraw the full ₹70,000 to stop further losses",
                        1.0,
                        "Conservative"
                },

                {
                        "RA_SCORE_002",
                        "Score 1.375 with age 55 maps to Conservative",
                        "Boundary",
                        "P0",
                        "55",
                        "₹5 lakh to ₹10 lakh",
                        "₹10 lakh to ₹25 lakh",
                        "Less than ₹5 lakh",
                        "More than 90%",
                        "Retirement",
                        "Keeping my money safe, even if returns are low",
                        "Withdraw ₹35,000 and leave the rest invested",
                        1.375,
                        "Conservative"
                },

                {
                        "RA_SCORE_003",
                        "Score 1.375 with age 50 maps to Moderate",
                        "Boundary",
                        "P0",
                        "50",
                        "₹5 lakh to ₹10 lakh",
                        "₹10 lakh to ₹25 lakh",
                        "Less than ₹5 lakh",
                        "More than 90%",
                        "Retirement",
                        "Keeping my money safe, even if returns are low",
                        "Withdraw ₹35,000 and leave the rest invested",
                        1.375,
                        "Moderate"
                },

                {
                        "RA_SCORE_004",
                        "Score 2.0 maps to Moderate for any age",
                        "Boundary",
                        "P0",
                        "22",
                        "₹5 lakh to ₹10 lakh",
                        "₹10 lakh to ₹25 lakh",
                        "₹10 lakh to ₹25 lakh",
                        "75% to 90%",
                        "Retirement",
                        "Steady returns, with some ups and downs",
                        "Withdraw ₹35,000 and leave the rest invested",
                        2.0,
                        "Moderate"
                },

                {
                        "RA_SCORE_005",
                        "Score 2.4375 with age 40 maps to Moderate",
                        "Boundary",
                        "P0",
                        "40",
                        "₹10 lakh to ₹25 lakh",
                        "₹25 lakh to ₹1 crore",
                        "Less than ₹5 lakh",
                        "Less than 75%",
                        "Buying a home",
                        "Steady returns, with some ups and downs",
                        "Stay invested, but stop new SIPs",
                        2.4375,
                        "Moderate"
                },

                {
                        "RA_SCORE_006",
                        "Score 2.4375 with age 39 maps to Aggressive",
                        "Boundary",
                        "P0",
                        "39",
                        "₹10 lakh to ₹25 lakh",
                        "₹25 lakh to ₹1 crore",
                        "Less than ₹5 lakh",
                        "Less than 75%",
                        "Buying a home",
                        "Steady returns, with some ups and downs",
                        "Stay invested, but stop new SIPs",
                        2.4375,
                        "Aggressive"
                },

                {
                        "RA_SCORE_007",
                        "Score above 2.70 maps to Aggressive",
                        "Boundary",
                        "P0",
                        "60",
                        "More than ₹1 crore",
                        "More than ₹1 crore",
                        "Nothing — I have no loans",
                        "Less than 75%",
                        "Long-term wealth",
                        "High returns, even when values swing sharply",
                        "Stay invested and continue my SIPs",
                        3.0,
                        "Aggressive"
                },

                {
                        "RA_SCORE_008",
                        "Income savings borrowings and goals do not affect risk score",
                        "Regression",
                        "P0",
                        "22",
                        "More than ₹1 crore",
                        "More than ₹1 crore",
                        "More than ₹1 crore",
                        "75% to 90%",
                        "Long-term wealth",
                        "Steady returns, with some ups and downs",
                        "Withdraw ₹35,000 and leave the rest invested",
                        2.0,
                        "Moderate"
                },

                {
                        "RA_GOAL_001",
                        "Investor Account goal Retirement selectable",
                        "Positive",
                        "P1",
                        "30",
                        "₹5 lakh to ₹10 lakh",
                        "₹10 lakh to ₹25 lakh",
                        "Less than ₹5 lakh",
                        "75% to 90%",
                        "Retirement",
                        "Steady returns, with some ups and downs",
                        "Withdraw ₹35,000 and leave the rest invested",
                        2.0,
                        "Moderate"
                },

                {
                        "RA_GOAL_002",
                        "Investor Account goal Buying a home selectable",
                        "Positive",
                        "P1",
                        "30",
                        "₹5 lakh to ₹10 lakh",
                        "₹10 lakh to ₹25 lakh",
                        "Less than ₹5 lakh",
                        "75% to 90%",
                        "Buying a home",
                        "Steady returns, with some ups and downs",
                        "Withdraw ₹35,000 and leave the rest invested",
                        2.0,
                        "Moderate"
                },

                {
                        "RA_GOAL_003",
                        "Investor Account goal Major life event selectable",
                        "Positive",
                        "P1",
                        "30",
                        "₹5 lakh to ₹10 lakh",
                        "₹10 lakh to ₹25 lakh",
                        "Less than ₹5 lakh",
                        "75% to 90%",
                        "Major life event",
                        "Steady returns, with some ups and downs",
                        "Withdraw ₹35,000 and leave the rest invested",
                        2.0,
                        "Moderate"
                },

                {
                        "RA_GOAL_004",
                        "Investor Account goal Long-term wealth selectable",
                        "Positive",
                        "P1",
                        "30",
                        "₹5 lakh to ₹10 lakh",
                        "₹10 lakh to ₹25 lakh",
                        "Less than ₹5 lakh",
                        "75% to 90%",
                        "Long-term wealth",
                        "Steady returns, with some ups and downs",
                        "Withdraw ₹35,000 and leave the rest invested",
                        2.0,
                        "Moderate"
                },

                {
                        "RA_GOAL_005",
                        "Investor Account goal Children selectable",
                        "Positive",
                        "P1",
                        "30",
                        "₹5 lakh to ₹10 lakh",
                        "₹10 lakh to ₹25 lakh",
                        "Less than ₹5 lakh",
                        "75% to 90%",
                        "Children",
                        "Steady returns, with some ups and downs",
                        "Withdraw ₹35,000 and leave the rest invested",
                        2.0,
                        "Moderate"
                }
        };
    }

    @Test(dataProvider = "riskAssessmentPrdCases")
    public void runRiskAssessmentPrdAutomationCases(
            String caseId,
            String title,
            String scenarioType,
            String priority,
            String age,
            String annualIncomeRange,
            String savingsRange,
            String loanRange,
            String essentialExpenseRange,
            String goal,
            String investmentPreference,
            String marketCrashReaction,
            double expectedScoreFromPrd,
            String expectedRiskProfile
    ) {
        runRiskAssessmentCase(
                caseId,
                title,
                scenarioType,
                priority,
                age,
                annualIncomeRange,
                savingsRange,
                loanRange,
                essentialExpenseRange,
                goal,
                investmentPreference,
                marketCrashReaction,
                expectedScoreFromPrd,
                expectedRiskProfile
        );
    }

    private void runRiskAssessmentCase(
            String caseId,
            String title,
            String scenarioType,
            String priority,
            String age,
            String annualIncomeRange,
            String savingsRange,
            String loanRange,
            String essentialExpenseRange,
            String goal,
            String investmentPreference,
            String marketCrashReaction,
            double expectedScoreFromPrd,
            String expectedRiskProfile
    ) {
        if (appiumSessionBroken) {
            throw new SkipException(
                    "Skipping " + caseId
                            + " because Appium/UiAutomator2 session is broken from previous test case. "
                            + "Restart Appium/emulator session and rerun."
            );
        }

        try {
            ExtentTestManager.setTest(
                    ExtentManager.getExtentReports().createTest(
                            caseId + " - " + title
                    )
            );

            double calculatedScore = calculateExpectedScoreFromPrd(
                    essentialExpenseRange,
                    investmentPreference,
                    marketCrashReaction
            );

            String calculatedProfile = calculateExpectedProfileFromPrd(
                    calculatedScore,
                    Integer.parseInt(age)
            );

            Assert.assertEquals(
                    calculatedScore,
                    expectedScoreFromPrd,
                    0.0001,
                    "Internal test data score mismatch for " + caseId
            );

            Assert.assertEquals(
                    calculatedProfile,
                    expectedRiskProfile,
                    "Internal test data expected profile mismatch for " + caseId
            );

            ExtentTestManager.getTest().log(
                    Status.INFO,
                    "<b>Module:</b> Risk Assessment<br>"
                            + "<b>Case ID:</b> " + caseId + "<br>"
                            + "<b>Scenario Type:</b> " + scenarioType + "<br>"
                            + "<b>Priority:</b> " + priority + "<br>"
                            + "<b>Automation Candidate:</b> Yes<br>"
                            + "<b>Scenario:</b> " + title + "<br>"
                            + "<b>Age:</b> " + age + "<br>"
                            + "<b>Annual Income:</b> " + annualIncomeRange + "<br>"
                            + "<b>Savings:</b> " + savingsRange + "<br>"
                            + "<b>Borrowings:</b> " + loanRange + "<br>"
                            + "<b>Essentials:</b> " + essentialExpenseRange + "<br>"
                            + "<b>Goal:</b> " + goal + "<br>"
                            + "<b>Investment Priority:</b> " + investmentPreference + "<br>"
                            + "<b>Drawdown Reaction:</b> " + marketCrashReaction + "<br>"
                            + "<b>Expected PRD Score:</b> " + calculatedScore + "<br>"
                            + "<b>Expected Risk Profile:</b> " + expectedRiskProfile
            );

            ReportLogger.step("Starting test case: " + caseId + " - " + title);
            ReportLogger.step("Scenario Type: " + scenarioType + " | Priority: " + priority);
            ReportLogger.step("Expected PRD Score: " + calculatedScore);
            ReportLogger.step("Expected PRD Risk Profile: " + expectedRiskProfile);

            RiskAssessmentPage riskAssessmentPage = new RiskAssessmentPage(driver);

            if (firstRiskAssessmentCase) {
                ReportLogger.step("Checking Advisor login/session");

                AuthHelper authHelper = new AuthHelper(driver);
                authHelper.ensureLoggedIn();

                ReportLogger.pass("Advisor login/session confirmed");

                ReportLogger.step("Opening Risk Assessment from Hub for first test case");
                riskAssessmentPage.openRiskAssessmentFromHub();

                firstRiskAssessmentCase = false;
            } else {
                ReportLogger.step("Continuing Risk Assessment directly from current screen");
                riskAssessmentPage.continueRiskAssessmentFromCurrentScreen();
            }

            riskAssessmentPage.completeRiskAssessmentFlow(
                    age,
                    annualIncomeRange,
                    savingsRange,
                    loanRange,
                    essentialExpenseRange,
                    goal,
                    investmentPreference,
                    marketCrashReaction,
                    expectedRiskProfile
            );

            ReportLogger.pass(caseId + " - " + title + " completed successfully");

        } catch (AssertionError e) {
            ReportLogger.fail(caseId + " failed: " + e.getMessage());
            throw e;

        } catch (SkipException e) {
            throw e;

        } catch (Exception e) {
            String cleanMessage = cleanError(e.getMessage());

            ReportLogger.fail(caseId + " failed: " + cleanMessage);

            if (isInfrastructureFailure(e)) {
                appiumSessionBroken = true;
                ReportLogger.fail(
                        "Infrastructure failure detected. Remaining Risk Assessment test cases will be skipped. "
                                + "Restart Appium/emulator session and rerun."
                );
            }

            throw new RuntimeException(caseId + " failed: " + cleanMessage, e);
        }
    }

    private double calculateExpectedScoreFromPrd(
            String essentialExpenseRange,
            String investmentPreference,
            String marketCrashReaction
    ) {
        double essentialsScore = getEssentialsScore(essentialExpenseRange);
        double priorityScore = getInvestmentPriorityScore(investmentPreference);
        double drawdownScore = getDrawdownScore(marketCrashReaction);

        return (0.25 * essentialsScore)
                + (0.375 * priorityScore)
                + (0.375 * drawdownScore);
    }

    private String calculateExpectedProfileFromPrd(double score, int age) {
        if (score <= 1.30) {
            return "Conservative";
        }

        if (score > 1.30 && score <= 1.70) {
            if (age > 50) {
                return "Conservative";
            }
            return "Moderate";
        }

        if (score > 1.70 && score <= 2.30) {
            return "Moderate";
        }

        if (score > 2.30 && score <= 2.70) {
            if (age >= 40) {
                return "Moderate";
            }
            return "Aggressive";
        }

        return "Aggressive";
    }

    private double getEssentialsScore(String essentialExpenseRange) {
        if ("Less than 75%".equals(essentialExpenseRange)) {
            return 3.0;
        }

        if ("75% to 90%".equals(essentialExpenseRange)) {
            return 2.0;
        }

        if ("More than 90%".equals(essentialExpenseRange)) {
            return 1.0;
        }

        throw new IllegalArgumentException("Unsupported essentials option: " + essentialExpenseRange);
    }

    private double getInvestmentPriorityScore(String investmentPreference) {
        if ("Keeping my money safe, even if returns are low".equals(investmentPreference)) {
            return 1.0;
        }

        if ("Steady returns, with some ups and downs".equals(investmentPreference)) {
            return 2.0;
        }

        if ("High returns, even when values swing sharply".equals(investmentPreference)) {
            return 3.0;
        }

        throw new IllegalArgumentException("Unsupported investment priority option: " + investmentPreference);
    }

    private double getDrawdownScore(String marketCrashReaction) {
        if ("Withdraw the full ₹70,000 to stop further losses".equals(marketCrashReaction)) {
            return 1.0;
        }

        if ("Withdraw ₹35,000 and leave the rest invested".equals(marketCrashReaction)) {
            return 2.0;
        }

        if ("Stay invested, but stop new SIPs".equals(marketCrashReaction)) {
            return 2.5;
        }

        if ("Stay invested and continue my SIPs".equals(marketCrashReaction)) {
            return 3.0;
        }

        throw new IllegalArgumentException("Unsupported drawdown option: " + marketCrashReaction);
    }

    private boolean isInfrastructureFailure(Exception e) {
        String message = e.getMessage();

        if (message == null) {
            Throwable cause = e.getCause();
            message = cause != null ? cause.getMessage() : "";
        }

        if (message == null) {
            return false;
        }

        return message.contains("socket hang up")
                || message.contains("instrumentation process is not running")
                || message.contains("UiAutomator2 server")
                || message.contains("cannot be proxied")
                || message.contains("Appium connection crashed");
    }

    private String cleanError(String message) {
        if (message == null) {
            return "";
        }

        int buildInfoIndex = message.indexOf("Build info:");
        if (buildInfoIndex > 0) {
            return message.substring(0, buildInfoIndex).trim();
        }

        int driverInfoIndex = message.indexOf("Driver info:");
        if (driverInfoIndex > 0) {
            return message.substring(0, driverInfoIndex).trim();
        }

        int capabilitiesIndex = message.indexOf("Capabilities");
        if (capabilitiesIndex > 0) {
            return message.substring(0, capabilitiesIndex).trim();
        }

        return message.trim();
    }
}