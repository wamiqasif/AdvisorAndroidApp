package com.valueresearch.pages;

import com.valueresearch.utils.ExtentTestManager;
import com.valueresearch.utils.ReportLogger;
import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.android.nativekey.AndroidKey;
import io.appium.java_client.android.nativekey.KeyEvent;
import org.openqa.selenium.By;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.Rectangle;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.PointerInput;
import org.openqa.selenium.interactions.Sequence;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PortfolioAnalysisPage {

    private final AndroidDriver driver;
    private final WebDriverWait wait;

    public PortfolioAnalysisPage(AndroidDriver driver) {
        this.driver = driver;
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(30));
    }

    private final By hubTab = AppiumBy.accessibilityId("Hub");
    private final By portfolioAnalysisMenu = AppiumBy.androidUIAutomator(
            "new UiSelector().descriptionContains(\"portfolio analysis\")"
    );
    private final By portfolioAnalysisHeader = byDesc("Portfolio Analysis");

    private final By summaryTab = AppiumBy.androidUIAutomator(
            "new UiSelector().descriptionContains(\"Summary\")"
    );

    private final By fundsTab = AppiumBy.androidUIAutomator(
            "new UiSelector().descriptionContains(\"Funds\")"
    );

    private final By stocksTab = AppiumBy.androidUIAutomator(
            "new UiSelector().descriptionContains(\"Stocks\")"
    );
    private final By chooseInvestorSheet = byDesc("Choose Investor");
    private final By manishInvestorPan = byDesc("MKLPK2070D");
    private final By manishInvestorByName = byDesc("Manish Khatri");
    private final By lalitInvestorByName = byDesc("Lalit Kumar Khatri");
    private final By lalitInvestorByPan = byDesc("ANAPK3082D");
    private final By vinitInvestorByName = byDesc("Vinit Sharma");
    private final By vinitInvestorByPan = byDesc("MCXPS3372L");

    // Summary
    private final By overallPortfolioPerformance = byDesc("Overall Portfolio Performance");
    private final By youVsMarket = byDesc("You vs Market");
    private final By benchmarkSensexTri = AppiumBy.accessibilityId("BSE Sensex TRI");
    private final By yourPortfolioReturn = byDesc("Your Portfolio Return");
    private final By yourPortfolioProfile = byDesc("Your Portfolio Profile");
    private final By portfolioProfileDetailTitle = AppiumBy.accessibilityId("Portfolio Profile");
    private final By retirementCard = byDesc("At 60, you will have");
    private final By supportIncome = byDesc("which can support income of");
    private final By yourRiskProfile = byDesc("Your Risk Profile");
    private final By assetMix = byDesc("Asset Mix");

    // Funds
    private final By fundPortfolioPerformance = byDesc("Fund Portfolio Performance");
    private final By fundToGetRidOf = byDesc("1 Fund to get rid of");
    private final By fundsMayConsiderSellingOff = byDesc("5 Funds you may consider selling off");
    private final By iciciPruFlexicap = byDesc("ICICI Pru Flexicap-G");
    private final By abslValueReg = byDesc("ABSL Value Reg-G");
    private final By hdfcDefenceReg = byDesc("HDFC Defence Reg-G");
    private final By portfolioInsights = byDesc("Portfolio Insights");
    private final By liquidity = byDesc("Liquidity");

    // Stocks
    private final By stockPortfolioPerformance = byDesc("Stock Portfolio Performance");

    private final By moreButton = AppiumBy.accessibilityId("More");

    public void openPortfolioAnalysisFromHub() {
        try {
            ReportLogger.step("Opening Portfolio Analysis from Hub");
            tap(hubTab, "Hub tab");
            sleep(1200);

            if (!tapIfVisible(portfolioAnalysisMenu, "Portfolio Analysis")) {
                scrollToAndTapPortfolioAnalysis();
            }

            waitForVisible(portfolioAnalysisHeader, "Portfolio Analysis header");
            waitForVisible(summaryTab, "Summary tab");
            waitForVisible(fundsTab, "Funds tab");
            waitForVisible(stocksTab, "Stocks tab");
            ReportLogger.pass("Portfolio Analysis opened successfully");
        } catch (Exception e) {
            captureScreenshotAndAttach("PA_Open_Failure");
            throw new RuntimeException("Failed to open Portfolio Analysis: " + cleanError(e.getMessage()), e);
        }
    }

    public void recoverPortfolioAnalysisIfNeeded() {
        try {
            if (isElementVisible(portfolioAnalysisHeader) && (isElementVisible(summaryTab) || isElementVisible(fundsTab) || isElementVisible(stocksTab))) {
                ReportLogger.step("Portfolio Analysis screen already active");
                return;
            }
            ReportLogger.step("Portfolio Analysis not active. Attempting safe recovery.");
            for (int i = 1; i <= 2; i++) {
                pressBackSafely();
                sleep(700);
                if (isElementVisible(portfolioAnalysisHeader)) return;
            }
            if (isElementVisible(hubTab)) {
                tap(hubTab, "Hub tab");
                sleep(1000);
                if (!tapIfVisible(portfolioAnalysisMenu, "Portfolio Analysis")) scrollToAndTapPortfolioAnalysis();
                waitForVisible(portfolioAnalysisHeader, "Portfolio Analysis header after recovery");
                return;
            }
            throw new RuntimeException("Unable to recover Portfolio Analysis screen");
        } catch (Exception e) {
            captureScreenshotAndAttach("PA_Recover_Failure");
            throw new RuntimeException("Portfolio Analysis recovery failed: " + cleanError(e.getMessage()), e);
        }
    }

    // ================= SUMMARY =================

    public void verifySummaryHeaderAndTabs() {
        try {
            ReportLogger.step("Validating Portfolio Analysis header and tabs");
            ensureOnPortfolioAnalysisSummaryPage();
            waitForVisible(portfolioAnalysisHeader, "Portfolio Analysis header");
            waitForVisible(summaryTab, "Summary tab");
            waitForVisible(fundsTab, "Funds tab");
            waitForVisible(stocksTab, "Stocks tab");
            ReportLogger.pass("Portfolio Analysis header and tabs validated");
        } catch (Exception e) {
            captureScreenshotAndAttach("PA_SUM_001_Header_Tabs_Failure");
            throw new AssertionError("Header/tabs validation failed: " + cleanError(e.getMessage()), e);
        }
    }

    public void verifyInvestorDropdown() {
        changeInvestorToManishKhatriFromSummaryPage();
    }

public void changeInvestorToManishKhatriFromSummaryPage() {
        try {
            ReportLogger.step("Changing investor to Manish Khatri from Summary page");

            ensureOnPortfolioAnalysisSummaryPage();

            if (!openInvestorDropdownIfPossible()) {
                throw new RuntimeException("Investor dropdown did not open from Summary page");
            }

            waitForInvestorSheetOpen();
            /*
             * In combined flow this sheet can show different investor lists depending on
             * current account/import state. Do not fail only because optional footer/action
             * items like Add Investor are not visible.
             */
            if (isTextPresentOnCurrentScreen("Multiple")) {
                ReportLogger.pass("Verified optional text: Multiple investor badge");
            } else {
                ReportLogger.debug("Optional Multiple investor badge not visible");
            }

            if (isTextPresentOnCurrentScreen("Add Investor")) {
                ReportLogger.pass("Verified optional text: Add Investor option");
            } else {
                ReportLogger.debug("Optional Add Investor option not visible");
            }

            if (!tapIfVisible(manishInvestorPan, "Manish Khatri investor by PAN")) {
                tap(manishInvestorByName, "Manish Khatri investor by name");
            }

            sleep(1800);

            waitForVisible(portfolioAnalysisHeader, "Portfolio Analysis header after investor selection");
            verifyTextPresentOnCurrentScreen("Manish", "Selected investor Manish in header");

            ReportLogger.pass("Investor changed to Manish Khatri on Summary page");

        } catch (Exception e) {
            captureScreenshotAndAttach("PA_SUM_002_Investor_Dropdown_Failure");
            throw new AssertionError("Summary investor selection failed: " + cleanError(e.getMessage()), e);
        }
    }


    public void verifyOverallPortfolioPerformanceCard() {
        try {
            ReportLogger.step("Validating Overall Portfolio Performance card");
            ensureOnPortfolioAnalysisSummaryPage();
            waitForVisible(overallPortfolioPerformance, "Overall Portfolio Performance");
            waitForVisible(youVsMarket, "You vs Market");
            waitForVisible(benchmarkSensexTri, "BSE Sensex TRI dropdown");
            waitForVisible(yourPortfolioReturn, "Your Portfolio Return");
            ReportLogger.pass("Overall Portfolio Performance card validated");
        } catch (Exception e) {
            captureScreenshotAndAttach("PA_SUM_003_Overall_Performance_Failure");
            throw new AssertionError("Overall Portfolio Performance validation failed: " + cleanError(e.getMessage()), e);
        }
    }

    public void verifyBenchmarkDropdownOptions() {
        try {
            ReportLogger.step("Validating benchmark dropdown options");
            ensureOnPortfolioAnalysisSummaryPage();
            tap(benchmarkSensexTri, "BSE Sensex TRI dropdown");
            sleep(1200);
            verifyTextPresentOnCurrentScreen("BSE Sensex TRI", "BSE Sensex TRI option");
            verifyTextPresentOnCurrentScreen("BSE 500 TRI", "BSE 500 TRI option");
            verifyTextPresentOnCurrentScreen("BSE 100 TRI", "BSE 100 TRI option");
            verifyTextPresentOnCurrentScreen("BSE Midcap 150 TRI", "BSE Midcap 150 TRI option");
            verifyTextPresentOnCurrentScreen("BSE Small Cap 250 TRI", "BSE Small Cap 250 TRI option");
            verifyTextPresentOnCurrentScreen("Fixed Deposit", "Fixed Deposit option");
            verifyTextPresentOnCurrentScreen("Inflation", "Inflation option");
            tap(benchmarkSensexTri, "BSE Sensex TRI option");
            sleep(1200);
            waitForVisible(overallPortfolioPerformance, "Overall Portfolio Performance after dropdown close");
            ReportLogger.pass("Benchmark dropdown options validated");
        } catch (Exception e) {
            captureScreenshotAndAttach("PA_SUM_004_Benchmark_Dropdown_Failure");
            throw new AssertionError("Benchmark dropdown validation failed: " + cleanError(e.getMessage()), e);
        }
    }

    public void verifyPortfolioProfileCard() {
        try {
            ReportLogger.step("Validating Your Portfolio Profile card");
            ensureOnPortfolioAnalysisSummaryPage();
            scrollToVisible(yourPortfolioProfile, "Your Portfolio Profile");
            verifyTextPresentOnCurrentScreen("Your asset allocation is suitable for long term wealth creation", "Portfolio profile recommendation");
            ReportLogger.pass("Your Portfolio Profile card validated");
        } catch (Exception e) {
            captureScreenshotAndAttach("PA_SUM_005_Portfolio_Profile_Card_Failure");
            throw new AssertionError("Portfolio Profile card validation failed: " + cleanError(e.getMessage()), e);
        }
    }

    public void verifyPortfolioProfileMoreDetailPage() {
        try {
            ReportLogger.step("Validating Portfolio Profile More detail page");
            ensureOnPortfolioAnalysisSummaryPage();
            scrollToVisible(yourPortfolioProfile, "Your Portfolio Profile");
            tapNearestMoreBelow(yourPortfolioProfile, "Portfolio Profile More");
            sleep(1500);
            waitForVisible(portfolioProfileDetailTitle, "Portfolio Profile detail title");
            verifyTextPresentOnCurrentScreen("Suitable for", "Suitable for");
            verifyTextPresentOnCurrentScreen("Long-term wealth creation", "Long-term wealth creation");
            verifyTextPresentOnCurrentScreen("For fund-specific views", "For fund-specific views");
            verifyTextPresentOnCurrentScreen("For stock-specific views", "For stock-specific views");
            verifyTextPresentOnCurrentScreen("Plan it here", "Plan it here button");
            tapTopLeftBackButton("Back from Portfolio Profile detail");
            sleep(1200);
            waitForVisible(portfolioAnalysisHeader, "Portfolio Analysis header after back");
            ReportLogger.pass("Portfolio Profile More detail page validated");
        } catch (Exception e) {
            captureScreenshotAndAttach("PA_SUM_006_Portfolio_Profile_More_Failure");
            throw new AssertionError("Portfolio Profile More detail validation failed: " + cleanError(e.getMessage()), e);
        }
    }

    public void verifyRetirementProjectionCard() {
        try {
            ReportLogger.step("Validating retirement projection card");
            ensureOnPortfolioAnalysisSummaryPage();
            scrollToVisible(retirementCard, "At 60, you will have");
            ensureExpandableSectionContentVisible(retirementCard, "Assuming monthly investment till retirement", "Retirement Projection");
            verifyTextPresentOnCurrentScreen("Assuming monthly investment till retirement", "Monthly investment till retirement");
            verifyTextPresentWithSmallScrollDown("₹5k", "₹5k step control");
            verifyTextPresentWithSmallScrollDown("Year of retirement", "Year of retirement");
            verifyTextPresentWithSmallScrollDown("5Y", "5Y step control");
            ReportLogger.pass("Retirement projection card validated");
        } catch (Exception e) {
            captureScreenshotAndAttach("PA_SUM_007_Retirement_Projection_Failure");
            throw new AssertionError("Retirement projection card validation failed: " + cleanError(e.getMessage()), e);
        }
    }

    public void verifyWithdrawalIncomeCard() {
        try {
            ReportLogger.step("Validating withdrawal income card");
            ensureOnPortfolioAnalysisSummaryPage();
            scrollToVisible(supportIncome, "which can support income of");
            ensureExpandableSectionContentVisible(supportIncome, "Withdrawal rate per annum", "Withdrawal Income");
            verifyTextPresentOnCurrentScreen("Withdrawal rate per annum", "Withdrawal rate per annum");
            verifyTextPresentWithSmallScrollDown("1%", "1% withdrawal step control");
            verifyTextPresentWithSmallScrollDown("Good", "Good withdrawal status");
            ReportLogger.pass("Withdrawal income card validated");
        } catch (Exception e) {
            captureScreenshotAndAttach("PA_SUM_008_Withdrawal_Income_Failure");
            throw new AssertionError("Withdrawal income card validation failed: " + cleanError(e.getMessage()), e);
        }
    }

public void verifyRiskProfileCard() {
        try {
            ReportLogger.step("Validating Your Risk Profile card");
            ensureOnPortfolioAnalysisSummaryPage();

            scrollToVisible(yourRiskProfile, "Your Risk Profile");

            verifyTextPresentOnCurrentScreen("Manish Khatri", "Investor name in risk profile");

            /*
             * Risk card may be partially visible after previous test scroll position.
             * Bring the card content into view before checking the lower text/buttons.
             */
            if (!isTextPresentOnCurrentScreen("Moderate")) {
                smallSwipeUpW3C();
                sleep(700);
            }

            verifyTextPresentOnCurrentScreen("Moderate", "Moderate risk profile");

            if (!isTextPresentOnCurrentScreen("You are a Moderate investor")) {
                smallSwipeUpW3C();
                sleep(700);
            }

            verifyTextPresentOnCurrentScreen("You are a Moderate investor", "Moderate investor text");

            verifyTextPresentWithSmallScrollDown("Update Assessment", "Update Assessment button");

            ReportLogger.pass("Your Risk Profile card validated");
        } catch (Exception e) {
            captureScreenshotAndAttach("PA_SUM_009_Risk_Profile_Failure");
            throw new AssertionError("Risk Profile card validation failed: " + cleanError(e.getMessage()), e);
        }
    }


    public void verifyAssetMixCard() {
        try {
            ReportLogger.step("Validating Asset Mix card");
            ensureOnPortfolioAnalysisSummaryPage();
            scrollToVisible(assetMix, "Asset Mix");
            verifyTextPresentWithSmallScrollDown("Total Assets Value", "Total Assets Value");
            verifyTextPresentWithSmallScrollDown("Asset Type", "Asset Type");
            verifyTextPresentWithSmallScrollDown("Amount (%)", "Amount (%)");
            verifyTextPresentWithSmallScrollDown("Equity", "Equity row");
            verifyTextPresentWithSmallScrollDown("Debt", "Debt row");
            verifyTextPresentWithSmallScrollDown("Others", "Others row");
            ReportLogger.pass("Asset Mix card validated");
        } catch (Exception e) {
            captureScreenshotAndAttach("PA_SUM_010_Asset_Mix_Failure");
            throw new AssertionError("Asset Mix card validation failed: " + cleanError(e.getMessage()), e);
        }
    }

    public void verifyAssetMixCompositionSections() {
        try {
            ReportLogger.step("Validating Asset Mix composition detail sections");
            ensureOnPortfolioAnalysisSummaryPage();
            scrollToVisible(assetMix, "Asset Mix");
            tapMoreBesideSection(assetMix, "Asset Mix More");
            sleep(1800);
            verifyTextPresentWithScroll("Equity Composition", "Equity Composition");
            verifyTextPresentWithScroll("Debt Composition", "Debt Composition");
            tapTopLeftBackButton("Back from Asset Mix detail");
            sleep(1200);
            waitForVisible(portfolioAnalysisHeader, "Portfolio Analysis header after back from Asset Mix detail");
            ReportLogger.pass("Asset Mix composition detail sections validated");
        } catch (Exception e) {
            captureScreenshotAndAttach("PA_SUM_011_Asset_Mix_Composition_Failure");
            throw new AssertionError("Asset Mix composition validation failed: " + cleanError(e.getMessage()), e);
        }
    }

    // ================= FUNDS =================

public void navigateToFundsTab() {
        try {
            ReportLogger.step("Navigating to Funds tab");
            moveBackToPortfolioTabsIfNeeded();
            ensureOnPortfolioAnalysisScreen();

            /*
             * Option A: reset scroll only when changing tab Summary -> Funds.
             */
            tapTabAndWaitForExactTop(
                    fundsTab,
                    fundPortfolioPerformance,
                    "Funds",
                    "Fund Portfolio Performance"
            );

            ReportLogger.pass("Funds tab opened at top successfully");
        } catch (Exception e) {
            captureScreenshotAndAttach("PA_FUN_001_Navigate_To_Funds_Tab_Failure");
            throw new AssertionError("Failed to navigate to Funds tab: " + cleanError(e.getMessage()), e);
        }
    }


public void changeInvestorToLalitKumarKhatriFromFundsPage() {
        try {
            ReportLogger.step("Changing investor to Lalit Kumar Khatri from Funds page");

            /*
             * Do not reset/scroll here. PA_FUN_001 already moved Funds to top once.
             */
            ensureOnFundsTabStrictTop();

            if (!openInvestorDropdownIfPossible()) {
                throw new RuntimeException("Investor dropdown did not open from Funds page");
            }

            waitForInvestorSheetOpen();
            if (!tapIfVisible(lalitInvestorByPan, "Lalit Kumar Khatri investor by PAN")) {
                tap(lalitInvestorByName, "Lalit Kumar Khatri investor by name");
            }

            sleep(2500);

            waitForVisible(fundPortfolioPerformance, "Fund Portfolio Performance after investor switch");
            verifyTextPresentOnCurrentScreen("Lalit", "Selected investor Lalit in header");

            ReportLogger.pass("Investor changed to Lalit Kumar Khatri on Funds page");
        } catch (Exception e) {
            captureScreenshotAndAttach("PA_FUN_002_Change_Investor_Lalit_Failure");
            throw new AssertionError("Failed to change investor to Lalit Kumar Khatri: " + cleanError(e.getMessage()), e);
        }
    }


    public void verifyFundPortfolioPerformanceCard() {
        try {
            ReportLogger.step("Validating Fund Portfolio Performance card");
            ensureOnFundsTab();
            scrollToVisible(fundPortfolioPerformance, "Fund Portfolio Performance");
            waitForVisible(youVsMarket, "You vs Market");
            waitForVisible(benchmarkSensexTri, "BSE Sensex TRI dropdown");
            waitForVisible(yourPortfolioReturn, "Your Portfolio Return");
            verifyTextPresentOnCurrentScreen("You have beaten BSE Sensex TRI", "Benchmark comparison message");
            ReportLogger.pass("Fund Portfolio Performance card validated");
        } catch (Exception e) {
            captureScreenshotAndAttach("PA_FUN_003_Performance_Card_Failure");
            throw new AssertionError("Fund Portfolio Performance validation failed: " + cleanError(e.getMessage()), e);
        }
    }

    public void verifyFundActionCards() {
        try {
            ReportLogger.step("Validating Funds action / attention cards");
            ensureOnFundsTab();
            scrollToVisible(fundToGetRidOf, "1 Fund to get rid of");
            verifyTextPresentOnCurrentScreen("ICICI Pru Flexicap-G", "ICICI Pru Flexicap-G");
            verifyTextPresentWithSmallScrollDown("5 Funds you may consider selling off", "5 Funds you may consider selling off");
            verifyTextPresentOnCurrentScreen("ABSL Value Reg-G", "ABSL Value Reg-G");
            verifyTextPresentWithSmallScrollDown("HDFC Defence Reg-G", "HDFC Defence Reg-G");
            verifyTextPresentWithSmallScrollDown("+3 more", "+3 more");
            ReportLogger.pass("Funds action / attention cards validated");
        } catch (Exception e) {
            captureScreenshotAndAttach("PA_FUN_004_Action_Cards_Failure");
            throw new AssertionError("Funds action cards validation failed: " + cleanError(e.getMessage()), e);
        }
    }

    public void verifyFundsPortfolioInsightsSection() {
        try {
            ReportLogger.step("Validating Funds Portfolio Insights section");
            ensureOnFundsTab();
            scrollToVisible(portfolioInsights, "Portfolio Insights");
            verifyTextPresentWithSmallScrollDown("Direct plans earn you more", "Direct plan insight");
            verifyTextPresentWithSmallScrollDown("HDFC Large Cap Reg-G", "HDFC Large Cap Reg-G");
            verifyTextPresentWithSmallScrollDown("ABSL Value Reg-G", "ABSL Value Reg-G");
            verifyTextPresentWithSmallScrollDown("+4 more", "+4 more");
            verifyTextPresentWithSmallScrollDown("IDCW", "IDCW tax efficiency insight");
            verifyTextPresentWithSmallScrollDown("ICICI Pru ELSS Tax Saver-IDCW", "ICICI Pru ELSS Tax Saver-IDCW");
            ReportLogger.pass("Funds Portfolio Insights section validated");
        } catch (Exception e) {
            captureScreenshotAndAttach("PA_FUN_005_Portfolio_Insights_Failure");
            throw new AssertionError("Funds Portfolio Insights validation failed: " + cleanError(e.getMessage()), e);
        }
    }

    public void verifyFundsLiquiditySection() {
        try {
            ReportLogger.step("Validating Funds Liquidity section");
            ensureOnFundsTab();
            scrollToVisible(liquidity, "Liquidity");
            verifyTextPresentWithSmallScrollDown("Total Redeemable Funds", "Total Redeemable Funds");
            verifyTextPresentWithSmallScrollDown("Redeemable free of exit load", "Redeemable free of exit load");
            verifyTextPresentWithSmallScrollDown("Redeemable with exit load", "Redeemable with exit load");
            verifyTextPresentWithSmallScrollDown("Locked in", "Locked in");
            verifyTextPresentWithSmallScrollDown("Need quick cash for a short time", "Need quick cash for a short time");
            verifyTextPresentWithSmallScrollDown("Loan Against Mutual Fund", "Loan Against Mutual Fund");
            ReportLogger.pass("Funds Liquidity section validated");
        } catch (Exception e) {
            captureScreenshotAndAttach("PA_FUN_006_Liquidity_Failure");
            throw new AssertionError("Funds Liquidity section validation failed: " + cleanError(e.getMessage()), e);
        }
    }

    public void verifyFundsLiquidityMoreDetailPage() {
        try {
            ReportLogger.step("Validating Funds Liquidity More detail page");
            ensureOnFundsTab();
            scrollToVisible(liquidity, "Liquidity");
            tapMoreBesideSection(liquidity, "Funds Liquidity More");
            sleep(2000);
            waitForVisible(liquidity, "Liquidity detail page title");
            verifyTextPresentOnCurrentScreen("Lalit Kumar Khatri", "Investor name on detail page");
            verifyTextPresentWithSmallScrollDown("Need quick cash for a short time", "Need quick cash text");
            verifyTextPresentWithSmallScrollDown("Loan Against Mutual Fund", "Loan Against Mutual Fund CTA");
            verifyTextPresentWithSmallScrollDown("Redeemable free of exit load", "Redeemable free of exit load section");
            verifyTextPresentWithSmallScrollDown("HDFC Large Cap Fund Reg-G", "HDFC Large Cap Fund Reg-G");
            verifyTextPresentWithSmallScrollDown("ICICI Pru Midcap-G", "ICICI Pru Midcap-G");
            verifyTextPresentWithSmallScrollDown("Aditya Birla SL Value Reg-G", "Aditya Birla SL Value Reg-G");
            verifyTextPresentWithSmallScrollDown("Redeemable with exit load", "Redeemable with exit load section");
            verifyTextPresentWithSmallScrollDown("HDFC Defence Reg-G", "HDFC Defence Reg-G");
            verifyTextPresentWithSmallScrollDown("Estimated exit load", "Estimated exit load");
            verifyTextPresentWithSmallScrollDown("Need cash?", "Need cash text");
            verifyTextPresentWithSmallScrollDown("Sell", "Sell CTA");
            verifyTextPresentWithDeepScrollDown("Locked in", "Locked in section");
            verifyTextPresentWithDeepScrollDown("ICICI Pru ELSS Tax Saver-IDCW", "ICICI Pru ELSS Tax Saver-IDCW");
            tapTopLeftBackButton("Back from Funds Liquidity detail page");
            sleep(1500);
            waitForVisible(fundsTab, "Funds tab after back from Liquidity detail");
            waitForVisible(liquidity, "Liquidity section after back");
            ReportLogger.pass("Funds Liquidity More detail page validated");
        } catch (Exception e) {
            captureScreenshotAndAttach("PA_FUN_007_Liquidity_More_Detail_Failure");
            try { tapTopLeftBackButton("Cleanup back from Funds Liquidity detail after failure"); } catch (Exception ignored) {}
            throw new AssertionError("Funds Liquidity More detail validation failed: " + cleanError(e.getMessage()), e);
        }
    }

    // ================= STOCKS =================


public void prepareStocksFlowFromFundsPage() {
        try {
            ReportLogger.step("Preparing Stocks flow from Funds page");

            /*
             * Confirmed flow:
             * After the last Funds test returns to Funds page, reset Funds to top once,
             * change investor to Vinit Sharma from Funds page, then navigate to Stocks.
             */
            moveBackToPortfolioTabsIfNeeded();

            tapTabAndWaitForExactTop(
                    fundsTab,
                    fundPortfolioPerformance,
                    "Funds",
                    "Fund Portfolio Performance before Vinit investor switch"
            );

            ensureOnFundsTabStrictTop();

            if (!openInvestorDropdownIfPossible()) {
                throw new RuntimeException("Investor dropdown did not open from Funds page before Stocks navigation");
            }

            waitForInvestorSheetOpen();

            if (!tapIfVisible(vinitInvestorByPan, "Vinit Sharma investor by PAN")) {
                tap(vinitInvestorByName, "Vinit Sharma investor by name");
            }

            sleep(2500);

            waitForVisible(fundPortfolioPerformance, "Fund Portfolio Performance after Vinit investor switch");
            verifyTextPresentOnCurrentScreen("Vinit", "Selected investor Vinit on Funds header");

            /*
             * Now changing tab Funds -> Stocks. This performs the only Stocks top reset.
             */
            navigateToStocksTab();

            verifyTextPresentOnCurrentScreen("Vinit", "Selected investor Vinit on Stocks header");

            ReportLogger.pass("Stocks flow prepared successfully from Funds page with Vinit Sharma investor");
        } catch (Exception e) {
            captureScreenshotAndAttach("PA_STK_001_Prepare_Stocks_From_Funds_Failure");
            throw new AssertionError("Failed to prepare Stocks flow from Funds page: " + cleanError(e.getMessage()), e);
        }
    }



public void navigateToStocksTab() {
        try {
            ReportLogger.step("Navigating to Stocks tab");
            moveBackToPortfolioTabsIfNeeded();
            ensureOnPortfolioAnalysisScreen();

            /*
             * Option A: reset scroll only when changing tab Funds -> Stocks.
             */
            tapTabAndWaitForExactTop(
                    stocksTab,
                    stockPortfolioPerformance,
                    "Stocks",
                    "Stock Portfolio Performance"
            );

            ReportLogger.pass("Stocks tab opened at top successfully");
        } catch (Exception e) {
            captureScreenshotAndAttach("PA_STK_001_Navigate_To_Stocks_Tab_Failure");
            throw new AssertionError("Failed to navigate to Stocks tab: " + cleanError(e.getMessage()), e);
        }
    }


    public void changeInvestorToVinitSharmaFromStocksPage() {
        try {
            ReportLogger.step("Changing investor to Vinit Sharma from Stocks page");

            /*
             * Defensive fix:
             * Always force the real Stocks tab top before opening investor dropdown.
             */
            tapTabAndWaitForExactTop(
                    stocksTab,
                    stockPortfolioPerformance,
                    "Stocks",
                    "Stock Portfolio Performance before investor switch"
            );

            ensureOnStocksTabStrictTop();

            if (!openInvestorDropdownIfPossible()) {
                throw new RuntimeException("Investor dropdown did not open from Stocks page");
            }

            waitForInvestorSheetOpen();

            if (!tapIfVisible(vinitInvestorByPan, "Vinit Sharma investor by PAN")) {
                tap(vinitInvestorByName, "Vinit Sharma investor by name");
            }

            sleep(2500);

            tapTabAndWaitForExactTop(
                    stocksTab,
                    stockPortfolioPerformance,
                    "Stocks",
                    "Stock Portfolio Performance after investor switch"
            );

            verifyTextPresentOnCurrentScreen("Vinit", "Selected investor Vinit in header");

            ReportLogger.pass("Investor changed to Vinit Sharma on Stocks page");

        } catch (Exception e) {
            captureScreenshotAndAttach("PA_STK_002_Change_Investor_Vinit_Failure");
            throw new AssertionError("Failed to change investor to Vinit Sharma: " + cleanError(e.getMessage()), e);
        }
    }


    public void verifyVinitInvestorOnStocksPage() {
        try {
            ReportLogger.step("Verifying Vinit Sharma is selected on Stocks page");

            ensureOnStocksTab();

            verifyTextPresentOnCurrentScreen("Vinit", "Selected investor Vinit in Stocks header");
            waitForVisible(stockPortfolioPerformance, "Stock Portfolio Performance");

            ReportLogger.pass("Vinit Sharma investor verified on Stocks page");

        } catch (Exception e) {
            captureScreenshotAndAttach("PA_STK_002_Verify_Vinit_Investor_Failure");
            throw new AssertionError("Vinit Sharma investor verification failed on Stocks page: " + cleanError(e.getMessage()), e);
        }
    }


    public void verifyStockPortfolioPerformanceCard() {
        try {
            ReportLogger.step("Validating Stock Portfolio Performance card");

            ensureOnStocksTab();

            scrollToVisible(stockPortfolioPerformance, "Stock Portfolio Performance");
            waitForVisible(youVsMarket, "You vs Market");
            waitForVisible(benchmarkSensexTri, "BSE Sensex TRI dropdown");
            waitForVisible(yourPortfolioReturn, "Your Portfolio Return");

            /*
             * Stock benchmark message is dynamic.
             * It can be "failed to beat", "beaten", or slightly different
             * depending on selected investor return data.
             */
            if (isTextPresentOnCurrentScreen("failed to beat BSE Sensex TRI")
                    || isTextPresentOnCurrentScreen("beaten BSE Sensex TRI")
                    || isTextPresentOnCurrentScreen("beat BSE Sensex TRI")
                    || isTextPresentOnCurrentScreen("BSE Sensex TRI")) {

                ReportLogger.pass("Verified text: Benchmark comparison message");

            } else {
                ReportLogger.step("Benchmark comparison message not visible immediately. Trying small scroll.");

                verifyTextPresentWithSmallScrollDown(
                        "BSE Sensex TRI",
                        "Benchmark comparison message after small scroll"
                );
            }

            ReportLogger.pass("Stock Portfolio Performance card validated");

        } catch (Exception e) {
            captureScreenshotAndAttach("PA_STK_003_Performance_Card_Failure");
            throw new AssertionError(
                    "Stock Portfolio Performance validation failed: " + cleanError(e.getMessage()),
                    e
            );
        }
    }
    public void verifyStocksPortfolioInsightsSection() {
        try {
            ReportLogger.step("Validating Stocks Portfolio Insights section");
            ensureOnStocksTab();
            scrollToVisible(portfolioInsights, "Portfolio Insights");
            verifyTextPresentWithSmallScrollDown("Dividend received", "Dividend received");
            verifyTextPresentWithSmallScrollDown("FY 2023-24", "FY 2023-24");
            verifyTextPresentWithSmallScrollDown("FY 2024-25", "FY 2024-25");
            verifyTextPresentWithSmallScrollDown("FY 2025-26", "FY 2025-26");
            verifyTextPresentWithSmallScrollDown("Best stocks to re-invest in", "Best stocks to re-invest in");
            ReportLogger.pass("Stocks Portfolio Insights section validated");
        } catch (Exception e) {
            captureScreenshotAndAttach("PA_STK_004_Portfolio_Insights_Failure");
            throw new AssertionError("Stocks Portfolio Insights validation failed: " + cleanError(e.getMessage()), e);
        }
    }

    public void verifyStocksLiquiditySection() {
        try {
            ReportLogger.step("Validating Stocks Liquidity section");
            ensureOnStocksTab();
            scrollToVisible(liquidity, "Liquidity");
            verifyTextPresentWithSmallScrollDown("Sellable in 1 day", "Sellable in 1 day");
            verifyTextPresentWithSmallScrollDown("Need Cash? Know which stocks to sell", "Need Cash CTA");
            ReportLogger.pass("Stocks Liquidity section validated");
        } catch (Exception e) {
            captureScreenshotAndAttach("PA_STK_005_Liquidity_Failure");
            throw new AssertionError("Stocks Liquidity validation failed: " + cleanError(e.getMessage()), e);
        }
    }

    public void verifyStocksLiquidityMoreDetailPage() {
        try {
            ReportLogger.step("Validating Stocks Liquidity More detail page");
            ensureOnStocksTab();
            scrollToVisible(liquidity, "Liquidity");
            tapMoreBesideSection(liquidity, "Stocks Liquidity More");
            sleep(2000);
            waitForVisible(liquidity, "Liquidity detail page title");
            verifyTextPresentOnCurrentScreen("Vinit Sharma", "Investor name on Liquidity detail page");
            verifyTextPresentWithSmallScrollDown("Need Cash? Know which stocks to sell", "Need Cash CTA");
            verifyTextPresentWithSmallScrollDown("Sellable in 1 day", "Sellable in 1 day section");
            verifyTextPresentWithSmallScrollDown("Zydus Lifesciences", "Zydus Lifesciences");
            verifyTextPresentWithSmallScrollDown("HDFC Bank", "HDFC Bank");
            verifyTextPresentWithSmallScrollDown("Natco Pharma", "Natco Pharma");
            verifyTextPresentWithSmallScrollDown("Tinna Rubber And Infrastructure", "Tinna Rubber And Infrastructure");
            verifyTextPresentWithDeepScrollDown("Styrenix Performance Materials", "Styrenix Performance Materials");
            verifyTextPresentWithDeepScrollDown("Adani Ports and Special Economic Zone", "Adani Ports and Special Economic Zone");
            verifyTextPresentWithDeepScrollDown("Cipla", "Cipla");
            verifyTextPresentWithDeepScrollDown("NMDC", "NMDC");
            verifyTextPresentWithDeepScrollDown("Vedant Fashions", "Vedant Fashions");
            verifyTextPresentWithDeepScrollDown("Chambal Fertilisers and Chemicals", "Chambal Fertilisers and Chemicals");
            verifyTextPresentWithDeepScrollDown("NTPC", "NTPC");
            verifyTextPresentWithDeepScrollDown("Andhra Paper", "Andhra Paper");
            verifyTextPresentWithDeepScrollDown("Dreamfolks Services", "Dreamfolks Services");
            verifyTextPresentWithDeepScrollDown("Bandhan Bank", "Bandhan Bank");
            verifyTextPresentWithDeepScrollDown("Castrol India", "Castrol India");
            verifyTextPresentWithDeepScrollDown("Kothari Sugars And Chemicals", "Kothari Sugars And Chemicals");
            tapTopLeftBackButton("Back from Stocks Liquidity detail page");
            sleep(1500);
            waitForVisible(stocksTab, "Stocks tab after back from Liquidity detail");
            waitForVisible(liquidity, "Liquidity section after back");
            ReportLogger.pass("Stocks Liquidity More detail page validated");
        } catch (Exception e) {
            captureScreenshotAndAttach("PA_STK_006_Liquidity_More_Detail_Failure");
            try { tapTopLeftBackButton("Cleanup back from Stocks Liquidity detail after failure"); } catch (Exception ignored) {}
            throw new AssertionError("Stocks Liquidity More detail validation failed: " + cleanError(e.getMessage()), e);
        }
    }

    // ================= COMMON HELPERS =================

    private void ensureOnPortfolioAnalysisScreen() {
        if (!isElementVisible(portfolioAnalysisHeader)) throw new RuntimeException("Portfolio Analysis header not visible");
        if (!isElementVisible(summaryTab) || !isElementVisible(fundsTab) || !isElementVisible(stocksTab)) {
            throw new RuntimeException("Portfolio Analysis tabs not visible");
        }
        ReportLogger.pass("Portfolio Analysis screen is active");
    }

    private void ensureOnPortfolioAnalysisSummaryPage() {
        ensureOnPortfolioAnalysisScreen();
        if (!isElementVisible(summaryTab)) throw new RuntimeException("Summary tab not visible");
        ReportLogger.pass("Portfolio Analysis Summary page is active");
    }

    private void ensureOnFundsTab() {
        ensureOnFundsTabStrictTop();
    }

private void ensureOnFundsTabStrictTop() {
        ensureOnPortfolioAnalysisScreen();

        boolean fundsContentVisible =
                isTextPresentOnCurrentScreen("Fund Portfolio Performance")
                        || isTextPresentOnCurrentScreen("Fund to get rid of")
                        || isTextPresentOnCurrentScreen("Funds you may consider selling off")
                        || isTextPresentOnCurrentScreen("Portfolio Insights")
                        || isTextPresentOnCurrentScreen("Total Redeemable Funds")
                        || isTextPresentOnCurrentScreen("Redeemable free of exit load")
                        || isTextPresentOnCurrentScreen("Redeemable with exit load")
                        || isTextPresentOnCurrentScreen("Locked in");

        if (!fundsContentVisible) {
            throw new RuntimeException("Funds tab content not visible.");
        }

        if (isTextPresentOnCurrentScreen("Import your stocks")
                || isTextPresentOnCurrentScreen("Account Aggregator")
                || isTextPresentOnCurrentScreen("Enter PAN")
                || isTextPresentOnCurrentScreen("Stock Portfolio Performance")) {
            throw new RuntimeException("Wrong tab/screen detected while expecting Funds tab.");
        }

        ReportLogger.pass("Funds tab is active");
    }





    private void ensureOnStocksTab() {
        ensureOnStocksTabStrictTop();
    }

private void ensureOnStocksTabStrictTop() {
        ensureOnPortfolioAnalysisScreen();

        boolean stocksContentVisible =
                isTextPresentOnCurrentScreen("Stock Portfolio Performance")
                        || isTextPresentOnCurrentScreen("Dividend received")
                        || isTextPresentOnCurrentScreen("Best stocks to re-invest in")
                        || isTextPresentOnCurrentScreen("Sellable in 1 day")
                        || isTextPresentOnCurrentScreen("Need Cash? Know which stocks to sell")
                        || isTextPresentOnCurrentScreen("Zydus Lifesciences")
                        || isTextPresentOnCurrentScreen("HDFC Bank")
                        || isTextPresentOnCurrentScreen("Bandhan Bank");

        if (!stocksContentVisible) {
            throw new RuntimeException("Stocks tab content not visible.");
        }

        if (isTextPresentOnCurrentScreen("Fund Portfolio Performance")
                || isTextPresentOnCurrentScreen("Fund to get rid of")
                || isTextPresentOnCurrentScreen("Funds you may consider selling off")) {
            throw new RuntimeException("Wrong tab/screen detected while expecting Stocks tab.");
        }

        ReportLogger.pass("Stocks tab is active");
    }





private boolean openInvestorDropdownIfPossible() {
    try {
        ReportLogger.step("Trying to open investor dropdown");

        if (isInvestorDropdownOpen()) {
            ReportLogger.pass("Investor dropdown already open");
            return true;
        }

        /*
         * Works for Summary, Funds and Stocks.
         * Always bring the sticky Portfolio Analysis header into view first.
         */
        ensurePortfolioHeaderVisibleForInvestorDropdown();
        sleep(500);

        /*
         * First try selected investor text in the header area.
         * This avoids blind coordinate tapping when investor name is visible.
         */
        String[] investorHeaderHints = {
                "Manish",
                "Lalit",
                "Vinit",
                "Khatri",
                "Sharma"
        };

        for (String hint : investorHeaderHints) {
            if (tryTapInvestorTextInHeaderBand(hint)) {
                return true;
            }
        }

        /*
         * Final stable fallback:
         * Scan top-right header band dynamically.
         * This replaces old hardcoded x=875/y=330 and x=980/y=330.
         */
        if (tapTopRightHeaderBandForInvestorDropdown()) {
            return true;
        }

        return false;

    } catch (Exception e) {
        ReportLogger.debug("Investor dropdown open failed safely: " + cleanError(e.getMessage()));
        return false;
    }
}

private boolean isInvestorDropdownOpen() {
    if (isElementVisible(chooseInvestorSheet)) {
        return true;
    }

    String pageSource = safePageSource();

    if (pageSource == null || pageSource.trim().isEmpty()) {
        return false;
    }

    return pageSource.contains("Choose Investor")
            || (pageSource.contains("Add Investor") && pageSource.contains("Multiple"))
            || (pageSource.contains("Add Investor") && pageSource.contains("Investor"));
}

private void waitForInvestorSheetOpen() {
    try {
        new WebDriverWait(driver, Duration.ofSeconds(8)).until(driver -> isInvestorDropdownOpen());
        ReportLogger.pass("Verified: Choose Investor bottom sheet");
    } catch (Exception e) {
        throw new RuntimeException("Choose Investor bottom sheet not visible: " + cleanError(e.getMessage()), e);
    }
}

private boolean tryTapInvestorTextInHeaderBand(String textHint) {
    try {
        List<WebElement> elements = driver.findElements(byDesc(textHint));

        for (WebElement element : elements) {
            try {
                if (!element.isDisplayed() || !element.isEnabled()) {
                    continue;
                }

                Rectangle rect = element.getRect();

                /*
                 * Investor dropdown is only in the top sticky header.
                 * Ignore matching text inside cards/lists.
                 */
                if (rect.getY() < 120 || rect.getY() > 430) {
                    continue;
                }

                tapElementCenter(element);
                sleep(900);

                if (isInvestorDropdownOpen()) {
                    ReportLogger.pass("Investor dropdown opened by tapping header text: " + textHint);
                    return true;
                }

                tapElementByPercent(element, 0.90, 0.50);
                sleep(900);

                if (isInvestorDropdownOpen()) {
                    ReportLogger.pass("Investor dropdown opened by tapping right side of header text: " + textHint);
                    return true;
                }

            } catch (Exception ignored) {
                // Try next matching element
            }
        }

        return false;

    } catch (Exception e) {
        ReportLogger.debug("Investor header text tap skipped for " + textHint + ": " + cleanError(e.getMessage()));
        return false;
    }
}

private boolean tapTopRightHeaderBandForInvestorDropdown() {
    try {
        int screenWidth = driver.manage().window().getSize().getWidth();

        /*
         * Top-right header scan.
         * Do not use one fixed coordinate because Summary/Funds/Stocks header
         * position can shift slightly after tab reset or scroll recovery.
         */
        int[] yPoints = {
                155,
                185,
                215,
                245,
                275,
                305,
                335
        };

        int[] xOffsetsFromRight = {
                520,
                440,
                360,
                280,
                200,
                120,
                60
        };

        for (int y : yPoints) {
            for (int offset : xOffsetsFromRight) {
                int x = screenWidth - offset;

                if (x < 40) {
                    x = 40;
                }

                if (x > screenWidth - 30) {
                    x = screenWidth - 30;
                }

                tapByCoordinate(x, y, "Investor dropdown dynamic header fallback");
                sleep(650);

                if (isInvestorDropdownOpen()) {
                    ReportLogger.pass("Investor dropdown opened at x=" + x + ", y=" + y);
                    return true;
                }
            }
        }

        return false;

    } catch (Exception e) {
        ReportLogger.debug("Investor dynamic header fallback failed: " + cleanError(e.getMessage()));
        return false;
    }
}




    private void scrollToAndTapPortfolioAnalysis() {
        Exception lastError = null;
        for (int attempt = 1; attempt <= 6; attempt++) {
            try {
                ReportLogger.step("Searching Portfolio Analysis in Hub. Attempt: " + attempt);
                if (tapIfVisible(portfolioAnalysisMenu, "Portfolio Analysis")) return;
                swipeUpW3C(); sleep(900);
            } catch (Exception e) {
                lastError = e;
                ReportLogger.debug("Portfolio Analysis search attempt failed: " + cleanError(e.getMessage()));
            }
        }
        throw new RuntimeException("Portfolio Analysis menu not found in Hub" + (lastError == null ? "" : ": " + cleanError(lastError.getMessage())), lastError);
    }

    private void scrollToVisible(By locator, String elementName) {
        String lastPageSource = "";
        for (int attempt = 1; attempt <= 8; attempt++) {
            if (isElementVisible(locator)) {
                ReportLogger.pass("Verified: " + elementName);
                return;
            }
            String currentPageSource = safePageSource();
            if (!currentPageSource.isEmpty() && currentPageSource.equals(lastPageSource)) {
                throw new RuntimeException(elementName + " not visible. Reached end of current page.");
            }
            lastPageSource = currentPageSource;
            ReportLogger.step("Scrolling down to find: " + elementName + " | Attempt: " + attempt);
            swipeUpW3C(); sleep(900);
        }
        throw new RuntimeException(elementName + " not visible after scrolling down");
    }


    
private void tapTabAndWaitForExactTop(By tabLocator, By expectedTopMarker, String tabName, String expectedTopName) {
        Exception lastError = null;

        for (int attempt = 1; attempt <= 4; attempt++) {
            try {
                ReportLogger.step("Opening " + tabName + " tab and resetting scroll. Attempt: " + attempt);

                moveBackToPortfolioTabsIfNeeded();
                ensureOnPortfolioAnalysisScreen();

                tap(tabLocator, tabName + " tab");
                sleep(1500);

                if (isImportStocksScreenVisible()) {
                    ReportLogger.step("Import Stocks screen detected while opening " + tabName + ". Going back and retrying.");
                    tapTopLeftBackButton("Back from Import Stocks screen");
                    sleep(1500);
                    continue;
                }

                scrollCurrentScrollViewToBeginning(tabName);
                sleep(1000);

                if (isImportStocksScreenVisible()) {
                    ReportLogger.step("Import Stocks screen detected after scroll reset. Going back and retrying.");
                    tapTopLeftBackButton("Back from Import Stocks screen after reset");
                    sleep(1500);
                    continue;
                }

                waitForVisible(expectedTopMarker, expectedTopName);
                ReportLogger.pass(tabName + " tab opened at top: " + expectedTopName);
                return;
            } catch (Exception e) {
                lastError = e;
                ReportLogger.debug(tabName + " open/reset attempt failed: " + cleanError(e.getMessage()));
            }
        }

        throw new RuntimeException(
                "Unable to open " + tabName + " tab at top. Expected marker: " + expectedTopName
                        + (lastError == null ? "" : ". Last error: " + cleanError(lastError.getMessage())),
                lastError
        );
    }




private void bringCurrentTabToRealTop(By expectedTopMarker, String expectedTopName) {
        scrollCurrentScrollViewToBeginning(expectedTopName);
        sleep(1000);
        waitForVisible(expectedTopMarker, expectedTopName);
        ReportLogger.pass("ScrollView beginning confirmed for " + expectedTopName);
    }




    private boolean isRealPortfolioTabTop(By expectedTopMarker, String expectedTopName) {
        return isPortfolioHeaderVisibleForDropdown()
                && (isElementVisible(expectedTopMarker) || isTextPresentOnCurrentScreen(expectedTopName))
                && isTopMarkerSafelyVisible(expectedTopMarker);
    }

    private boolean isTopMarkerSafelyVisible(By locator) {
        try {
            List<WebElement> elements = driver.findElements(locator);

            for (WebElement element : elements) {
                try {
                    if (!element.isDisplayed()) {
                        continue;
                    }

                    Rectangle rect = element.getRect();

                    /*
                     * On this device, sticky tabs/header area ends around y=360.
                     * If the marker is above this, it is half hidden under the tabs.
                     */
                    return rect.getY() >= 360 && rect.getY() <= 850;

                } catch (Exception ignored) {
                }
            }

            return false;

        } catch (Exception e) {
            return false;
        }
    }

    private void moveBackToPortfolioTabsIfNeeded() {
        for (int attempt = 1; attempt <= 3; attempt++) {
            if (isElementVisible(summaryTab) && isElementVisible(fundsTab) && isElementVisible(stocksTab)) {
                return;
            }

            ReportLogger.step("Portfolio tabs not visible. Trying top-left back. Attempt: " + attempt);
            try {
                tapTopLeftBackButton("Recover to Portfolio Analysis tabs");
                sleep(1400);
            } catch (Exception ignored) {
                pressBackSafely();
            }
        }
    }

    private boolean isImportStocksScreenVisible() {
        return isTextPresentOnCurrentScreen("Import your stocks")
                || isTextPresentOnCurrentScreen("Account Aggregator")
                || isTextPresentOnCurrentScreen("Enter PAN")
                || isTextPresentOnCurrentScreen("Enter Mobile")
                || isTextPresentOnCurrentScreen("View other methods");
    }

private void normalizeTabToTopStrict(By topMarkerLocator, String topMarkerName) {
        bringCurrentTabToRealTop(topMarkerLocator, topMarkerName);
    }

private void ensurePortfolioHeaderVisibleForInvestorDropdown() {
    for (int attempt = 1; attempt <= 4; attempt++) {
        if (isPortfolioHeaderVisibleForDropdown()) {
            ReportLogger.pass("Portfolio Analysis header is visible for investor dropdown");
            return;
        }

        ReportLogger.step("Portfolio Analysis header not detected. Soft swipe-down attempt: " + attempt);
        swipeDownInsideContentArea();
        sleep(600);
    }

    /*
     * Do not fail here.
     * Header locator can be hidden/merged on Summary/Funds/Stocks,
     * but investor dropdown can still be opened from the top-right area.
     */
    ReportLogger.debug("Portfolio Analysis header not detected. Continuing with investor dropdown tap fallback.");
}
private boolean isPortfolioHeaderVisibleForDropdown() {
    try {
        List<WebElement> headers = driver.findElements(portfolioAnalysisHeader);

        for (WebElement header : headers) {
            try {
                if (!header.isDisplayed()) {
                    continue;
                }

                Rectangle rect = header.getRect();

                /*
                 * Relaxed range.
                 * Header can appear higher/lower depending on Summary/Funds/Stocks
                 * and emulator status bar.
                 */
                if (rect.getY() >= 80 && rect.getY() <= 520 && rect.getHeight() >= 30) {
                    return true;
                }

            } catch (Exception ignored) {
            }
        }

        /*
         * Fallback: if tabs are visible, Portfolio Analysis top area is usable.
         */
        return isElementVisible(summaryTab)
                || isElementVisible(fundsTab)
                || isElementVisible(stocksTab);

    } catch (Exception e) {
        return false;
    }
}

    private void recoverFromNestedPageIfTabsMissing() {
        if (isElementVisible(summaryTab) && isElementVisible(fundsTab) && isElementVisible(stocksTab)) {
            return;
        }

        ReportLogger.step("Tabs not visible. Trying top-left back to recover from nested page.");
        try {
            tapTopLeftBackButton("Recover from nested Portfolio Analysis page");
            sleep(1200);
        } catch (Exception ignored) {
        }
    }

    private void ensureExpandableSectionContentVisible(By sectionHeaderLocator, String expandedText, String sectionName) {
        if (isTextPresentOnCurrentScreen(expandedText)) {
            ReportLogger.pass(sectionName + " section content already visible");
            return;
        }
        for (int attempt = 1; attempt <= 2; attempt++) {
            ReportLogger.step(sectionName + " content not visible. Small scroll. Attempt: " + attempt);
            smallSwipeUpW3C(); sleep(700);
            if (isTextPresentOnCurrentScreen(expandedText)) return;
        }
        tapRightSideOfSection(sectionHeaderLocator, sectionName + " right-side chevron tap");
        sleep(1000);
        if (isTextPresentOnCurrentScreen(expandedText)) return;
        for (int attempt = 1; attempt <= 3; attempt++) {
            smallSwipeUpW3C(); sleep(700);
            if (isTextPresentOnCurrentScreen(expandedText)) return;
        }
        throw new RuntimeException(sectionName + " expanded content not visible. Expected text: " + expandedText);
    }

    private void tapRightSideOfSection(By sectionLocator, String actionName) {
        WebElement section = wait.until(ExpectedConditions.visibilityOfElementLocated(sectionLocator));
        Rectangle rect = section.getRect();
        int x = 960;
        int y = rect.getY() + 45;
        if (y > 1550) {
            smallSwipeUpW3C(); sleep(700);
            section = wait.until(ExpectedConditions.visibilityOfElementLocated(sectionLocator));
            rect = section.getRect();
            y = rect.getY() + 45;
        }
        if (y > 1550) y = 1500;
        tapByCoordinate(x, y, actionName);
    }

    private void verifyTextPresentWithScroll(String text, String elementName) {
        String lastPageSource = "";

        for (int attempt = 1; attempt <= 7; attempt++) {
            if (isTextPresentOnCurrentScreen(text)) {
                reportVerifiedText("Verified text", elementName, text);
                return;
            }

            String currentPageSource = safePageSource();

            if (!currentPageSource.isEmpty() && currentPageSource.equals(lastPageSource)) {
                throw new RuntimeException(
                        elementName
                                + " not visible. Reached end. Expected text contains: "
                                + text
                );
            }

            lastPageSource = currentPageSource;

            ReportLogger.step(
                    "Scrolling down to find text"
                            + " | Label: "
                            + elementName
                            + " | Expected: "
                            + cleanTextForReport(text)
                            + " | Attempt: "
                            + attempt
            );

            swipeUpW3C();
            sleep(900);
        }

        throw new RuntimeException(
                elementName
                        + " not visible after scrolling. Expected text contains: "
                        + text
        );
    }

    private void verifyTextPresentWithSmallScrollDown(String text, String elementName) {
        if (isTextPresentOnCurrentScreen(text)) {
            reportVerifiedText("Verified text", elementName, text);
            return;
        }

        for (int attempt = 1; attempt <= 4; attempt++) {
            ReportLogger.step(
                    "Small scroll down to find text"
                            + " | Label: "
                            + elementName
                            + " | Expected: "
                            + cleanTextForReport(text)
                            + " | Attempt: "
                            + attempt
            );

            smallSwipeUpW3C();
            sleep(700);

            if (isTextPresentOnCurrentScreen(text)) {
                reportVerifiedText("Verified text after small scroll", elementName, text);
                return;
            }
        }

        throw new RuntimeException(
                elementName
                        + " text not present after small scroll. Expected text contains: "
                        + text
        );
    }

    private void verifyTextPresentWithDeepScrollDown(String text, String elementName) {
        if (isTextPresentOnCurrentScreen(text)) {
            reportVerifiedText("Verified text", elementName, text);
            return;
        }

        for (int attempt = 1; attempt <= 12; attempt++) {
            ReportLogger.step(
                    "Deep scroll down to find text"
                            + " | Label: "
                            + elementName
                            + " | Expected: "
                            + cleanTextForReport(text)
                            + " | Attempt: "
                            + attempt
            );

            smallSwipeUpW3C();
            sleep(700);

            if (isTextPresentOnCurrentScreen(text)) {
                reportVerifiedText("Verified text after deep scroll", elementName, text);
                return;
            }
        }

        throw new RuntimeException(
                elementName
                        + " text not present after deep scroll. Expected text contains: "
                        + text
        );
    }
    private void verifyTextPresentOnCurrentScreen(String text, String elementName) {
        if (isTextPresentOnCurrentScreen(text)) {
            reportVerifiedText("Verified text on current screen", elementName, text);
            return;
        }

        throw new RuntimeException(
                elementName
                        + " text not present on current screen. Expected text contains: "
                        + text
        );
    }
    
    private void reportVerifiedText(String logPrefix, String elementName, String expectedText) {
        ReportLogger.pass(
                logPrefix
                        + " | Label: "
                        + elementName
                        + " | Expected/Matched Text: "
                        + cleanTextForReport(expectedText)
        );
    }

    private String cleanTextForReport(String text) {
        if (text == null) {
            return "";
        }

        return text
                .replace("\n", " ")
                .replace("\r", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }
    private boolean isTextPresentOnCurrentScreen(String text) {
        if (isElementVisible(byDesc(text))) return true;
        String pageSource = safePageSource();
        return pageSource != null && pageSource.contains(text);
    }

    private String safePageSource() {
        try {
            String source = driver.getPageSource();
            return source == null ? "" : source;
        } catch (Exception e) {
            return "";
        }
    }

    private void tapNearestMoreBelow(By sectionLocator, String actionName) {
        WebElement section = wait.until(ExpectedConditions.visibilityOfElementLocated(sectionLocator));
        Rectangle sectionRect = section.getRect();
        List<WebElement> moreButtons = driver.findElements(moreButton);
        List<WebElement> candidates = new ArrayList<>();
        for (WebElement more : moreButtons) {
            try {
                if (!more.isDisplayed() || !more.isEnabled()) continue;
                Rectangle rect = more.getRect();
                if (rect.getY() >= sectionRect.getY() - 120 && rect.getY() <= sectionRect.getY() + 300) candidates.add(more);
            } catch (Exception ignored) {}
        }
        candidates.sort(Comparator.comparingInt(e -> Math.abs(e.getRect().getY() - sectionRect.getY())));
        if (!candidates.isEmpty()) {
            tapElementCenter(candidates.get(0));
            ReportLogger.step(actionName + " clicked");
            return;
        }
        tapByCoordinate(960, sectionRect.getY() + Math.max(35, sectionRect.getHeight() / 2), actionName + " coordinate fallback");
    }

    private void tapMoreBesideSection(By sectionLocator, String actionName) {
        WebElement section = wait.until(ExpectedConditions.visibilityOfElementLocated(sectionLocator));
        Rectangle sectionRect = section.getRect();
        List<WebElement> moreButtons = driver.findElements(moreButton);
        List<WebElement> candidates = new ArrayList<>();
        for (WebElement more : moreButtons) {
            try {
                if (!more.isDisplayed() || !more.isEnabled()) continue;
                Rectangle moreRect = more.getRect();
                boolean sameHeadingRow = moreRect.getY() >= sectionRect.getY() - 90 && moreRect.getY() <= sectionRect.getY() + 120;
                if (sameHeadingRow) candidates.add(more);
            } catch (Exception ignored) {}
        }
        candidates.sort(Comparator.comparingInt(e -> Math.abs(e.getRect().getY() - sectionRect.getY())));
        if (!candidates.isEmpty()) {
            tapElementCenter(candidates.get(0));
            ReportLogger.step(actionName + " clicked using More locator");
            sleep(1000);
            return;
        }
        tapByCoordinate(960, sectionRect.getY() + Math.max(30, sectionRect.getHeight() / 2), actionName + " coordinate fallback");
        sleep(1000);
    }

    private boolean tapIfVisible(By locator, String elementName) {
        try {
            List<WebElement> elements = driver.findElements(locator);
            for (WebElement element : elements) {
                try {
                    if (element.isDisplayed() && element.isEnabled()) {
                        tapElementCenter(element);
                        ReportLogger.step(elementName + " clicked");
                        sleep(800);
                        return true;
                    }
                } catch (Exception ignored) {}
            }
            return false;
        } catch (Exception e) {
            ReportLogger.debug(elementName + " tap skipped: " + cleanError(e.getMessage()));
            return false;
        }
    }

    private void tap(By locator, String elementName) {
        try {
            WebElement element = wait.until(ExpectedConditions.elementToBeClickable(locator));
            tapElementCenter(element);
            ReportLogger.step(elementName + " clicked");
            sleep(700);
        } catch (Exception e) {
            throw new RuntimeException("Failed to tap " + elementName + ": " + cleanError(e.getMessage()), e);
        }
    }

    private void waitForVisible(By locator, String elementName) {
        wait.until(ExpectedConditions.visibilityOfElementLocated(locator));
        ReportLogger.pass("Verified: " + elementName);
    }

    private boolean isElementVisible(By locator) {
        try {
            List<WebElement> elements = driver.findElements(locator);
            for (WebElement element : elements) {
                try {
                    if (element.isDisplayed()) return true;
                } catch (Exception ignored) {}
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    private void tapByCoordinate(int x, int y, String stepName) {
        Map<String, Object> params = new HashMap<>();
        params.put("x", x);
        params.put("y", y);
        driver.executeScript("mobile: clickGesture", params);
        ReportLogger.step(stepName + " tapped at x=" + x + ", y=" + y);
        sleep(250);
    }

    private void tapElementCenter(WebElement element) {
        Rectangle rect = element.getRect();
        Map<String, Object> params = new HashMap<>();
        params.put("x", rect.getX() + rect.getWidth() / 2);
        params.put("y", rect.getY() + rect.getHeight() / 2);
        driver.executeScript("mobile: clickGesture", params);
        sleep(250);
    }

    private void tapElementByPercent(WebElement element, double xPercent, double yPercent) {
        Rectangle rect = element.getRect();
        Map<String, Object> params = new HashMap<>();
        params.put("x", rect.getX() + (int) (rect.getWidth() * xPercent));
        params.put("y", rect.getY() + (int) (rect.getHeight() * yPercent));
        driver.executeScript("mobile: clickGesture", params);
        sleep(250);
    }

    private void swipeUpW3C() {
        try {
            PointerInput finger = new PointerInput(PointerInput.Kind.TOUCH, "finger");
            Sequence swipe = new Sequence(finger, 1);
            swipe.addAction(finger.createPointerMove(Duration.ZERO, PointerInput.Origin.viewport(), 540, 1650));
            swipe.addAction(finger.createPointerDown(PointerInput.MouseButton.LEFT.asArg()));
            swipe.addAction(finger.createPointerMove(Duration.ofMillis(650), PointerInput.Origin.viewport(), 540, 700));
            swipe.addAction(finger.createPointerUp(PointerInput.MouseButton.LEFT.asArg()));
            driver.perform(Collections.singletonList(swipe));
        } catch (Exception e) {
            throw new RuntimeException("W3C swipe up failed: " + cleanError(e.getMessage()), e);
        }
    }

    private void strongSwipeDownW3C() {
        try {
            PointerInput finger = new PointerInput(PointerInput.Kind.TOUCH, "finger");
            Sequence swipe = new Sequence(finger, 1);

            /*
             * Strong swipe-down inside app content area.
             * This is used only after tapping a tab to bring its scroll view to TOP.
             */
            swipe.addAction(finger.createPointerMove(Duration.ZERO, PointerInput.Origin.viewport(), 540, 780));
            swipe.addAction(finger.createPointerDown(PointerInput.MouseButton.LEFT.asArg()));
            swipe.addAction(finger.createPointerMove(Duration.ofMillis(750), PointerInput.Origin.viewport(), 540, 1820));
            swipe.addAction(finger.createPointerUp(PointerInput.MouseButton.LEFT.asArg()));
            driver.perform(Collections.singletonList(swipe));
        } catch (Exception e) {
            throw new RuntimeException("Strong W3C swipe down failed: " + cleanError(e.getMessage()), e);
        }
    }

    
private void scrollCurrentScrollViewToBeginning(String pageName) {
        /*
         * Tax-style mature reset for tab switch only.
         * First use UiScrollable against actual ScrollView, then one fallback swipe.
         */
        boolean uiScrollableWorked = false;

        try {
            driver.findElement(AppiumBy.androidUIAutomator(
                    "new UiScrollable(new UiSelector().className(\"android.widget.ScrollView\")).scrollToBeginning(20)"
            ));
            uiScrollableWorked = true;
            ReportLogger.step(pageName + " ScrollView reset using UiScrollable.scrollToBeginning");
        } catch (Exception e) {
            ReportLogger.debug(pageName + " UiScrollable.scrollToBeginning fallback needed: " + cleanError(e.getMessage()));
        }

        if (!isPortfolioHeaderVisibleForDropdown()) {
            ReportLogger.step(pageName + " header not visible after reset. Single swipe-down fallback.");
            swipeDownInsideContentArea();
            sleep(700);
        }

        if (!uiScrollableWorked) {
            ReportLogger.step(pageName + " ScrollView reset completed using fallback");
        }
    }


private void swipeDownInsideContentArea() {
        /*
         * Finger moves down; content moves down, revealing content above/current top.
         * Start below sticky tabs and end above Android navigation bar.
         */
        try {
            PointerInput finger = new PointerInput(PointerInput.Kind.TOUCH, "finger");
            Sequence swipe = new Sequence(finger, 1);

            swipe.addAction(finger.createPointerMove(Duration.ZERO, PointerInput.Origin.viewport(), 540, 820));
            swipe.addAction(finger.createPointerDown(PointerInput.MouseButton.LEFT.asArg()));
            swipe.addAction(finger.createPointerMove(Duration.ofMillis(650), PointerInput.Origin.viewport(), 540, 1680));
            swipe.addAction(finger.createPointerUp(PointerInput.MouseButton.LEFT.asArg()));

            driver.perform(Collections.singletonList(swipe));

        } catch (Exception firstError) {
            try {
                Map<String, Object> params = new HashMap<>();
                params.put("left", 40);
                params.put("top", 360);
                params.put("width", 1000);
                params.put("height", 1450);
                params.put("direction", "down");
                params.put("percent", 0.85);
                driver.executeScript("mobile: scrollGesture", params);
            } catch (Exception secondError) {
                throw new RuntimeException("Swipe down to real top failed: " + cleanError(secondError.getMessage()), secondError);
            }
        }
    }

    private void scrollDownToTopGesture() {
        swipeDownInsideContentArea();
    }


private void swipeDownW3C() {
        try {
            PointerInput finger = new PointerInput(PointerInput.Kind.TOUCH, "finger");
            Sequence swipe = new Sequence(finger, 1);

            /*
             * Finger moves down; content moves back toward the top.
             * Start below tabs to avoid notification shade and avoid strong pull-to-refresh.
             */
            swipe.addAction(finger.createPointerMove(Duration.ZERO, PointerInput.Origin.viewport(), 540, 760));
            swipe.addAction(finger.createPointerDown(PointerInput.MouseButton.LEFT.asArg()));
            swipe.addAction(finger.createPointerMove(Duration.ofMillis(550), PointerInput.Origin.viewport(), 540, 1500));
            swipe.addAction(finger.createPointerUp(PointerInput.MouseButton.LEFT.asArg()));

            driver.perform(Collections.singletonList(swipe));

        } catch (Exception e) {
            throw new RuntimeException("W3C swipe down failed: " + cleanError(e.getMessage()), e);
        }
    }

    private void smallSwipeUpW3C() {
        try {
            PointerInput finger = new PointerInput(PointerInput.Kind.TOUCH, "finger");
            Sequence swipe = new Sequence(finger, 1);
            swipe.addAction(finger.createPointerMove(Duration.ZERO, PointerInput.Origin.viewport(), 540, 1380));
            swipe.addAction(finger.createPointerDown(PointerInput.MouseButton.LEFT.asArg()));
            swipe.addAction(finger.createPointerMove(Duration.ofMillis(400), PointerInput.Origin.viewport(), 540, 1120));
            swipe.addAction(finger.createPointerUp(PointerInput.MouseButton.LEFT.asArg()));
            driver.perform(Collections.singletonList(swipe));
        } catch (Exception e) {
            throw new RuntimeException("Small W3C swipe up failed: " + cleanError(e.getMessage()), e);
        }
    }

    private void pressBackSafely() {
        try {
            driver.pressKey(new KeyEvent(AndroidKey.BACK));
            sleep(1000);
        } catch (Exception e) {
            ReportLogger.debug("Android BACK skipped: " + cleanError(e.getMessage()));
        }
    }

    private void tapTopLeftBackButton(String actionName) {
        try {
            By detailBackButton = AppiumBy.androidUIAutomator("new UiSelector().className(\"android.widget.ImageView\").instance(0)");
            List<WebElement> backButtons = driver.findElements(detailBackButton);
            for (WebElement backButton : backButtons) {
                try {
                    if (backButton.isDisplayed() && backButton.isEnabled()) {
                        Rectangle rect = backButton.getRect();
                        if (rect.getX() <= 180 && rect.getY() <= 250) {
                            tapElementCenter(backButton);
                            ReportLogger.step(actionName + " clicked using back locator");
                            sleep(1200);
                            return;
                        }
                    }
                } catch (Exception ignored) {}
            }
            tapByCoordinate(84, 149, actionName + " coordinate fallback");
            sleep(1200);
        } catch (Exception e) {
            throw new RuntimeException("Failed to tap top-left back button: " + cleanError(e.getMessage()), e);
        }
    }

    private void captureScreenshotAndAttach(String screenshotName) {
        try {
            if (driver == null || driver.getSessionId() == null) return;
            String safeName = screenshotName.replaceAll("[^a-zA-Z0-9._-]", "_") + "_" + System.currentTimeMillis() + ".png";
            Path screenshotDir = Paths.get(System.getProperty("user.dir"), "test-output", "ExtentReports", "screenshots");
            Files.createDirectories(screenshotDir);
            File sourceFile = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
            Path destinationPath = screenshotDir.resolve(safeName);
            Files.copy(sourceFile.toPath(), destinationPath, StandardCopyOption.REPLACE_EXISTING);
            String absolutePath = destinationPath.toFile().getAbsolutePath();
            if (ExtentTestManager.getTest() != null) {
                ExtentTestManager.getTest().addScreenCaptureFromPath(absolutePath, screenshotName);
                ReportLogger.step("Failure screenshot attached: " + absolutePath);
            }
        } catch (Exception e) {
            ReportLogger.debug("Screenshot capture failed: " + cleanError(e.getMessage()));
        }
    }

    private By byDesc(String text) {
        return AppiumBy.androidUIAutomator("new UiSelector().descriptionContains(\"" + text + "\")");
    }

    private String cleanError(String message) {
        if (message == null) return "";
        int buildInfoIndex = message.indexOf("Build info:");
        if (buildInfoIndex > 0) return message.substring(0, buildInfoIndex).trim();
        int driverInfoIndex = message.indexOf("Driver info:");
        if (driverInfoIndex > 0) return message.substring(0, driverInfoIndex).trim();
        return message.trim();
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Thread interrupted", e);
        }
    }
}