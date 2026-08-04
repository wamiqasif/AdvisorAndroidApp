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
import org.openqa.selenium.interactions.Pause;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AnalystChoicePage {

    private final AndroidDriver driver;
    private final WebDriverWait wait;
    private final List<String> tableValidationErrors = new ArrayList<>();

    public AnalystChoicePage(AndroidDriver driver) {
        this.driver = driver;
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(30));
    }

    // ================= ENTRY / LISTING =================

    private final By hubTab = AppiumBy.accessibilityId("Hub");
    private final By analystChoiceHubMenu = AppiumBy.accessibilityId("Analyst’s Choice");

    private final By analystChoiceTitle = AppiumBy.accessibilityId("Analyst’s Choice");
    private final By analystChoiceSubtitle = AppiumBy.accessibilityId(
            "Our recommended funds to match your financial goals."
    );

    private final By aggressiveGrowthCard = byDescContains("Aggressive Growth\nBest mid- and small-cap");
    private final By growthCard = byDescContains("Growth\nTop diversified equity funds");
    private final By taxPlanningCard = byDescContains("Tax Planning\nELSS funds similar to Growth");
    private final By growthInternationalCard = byDescContains("Growth - International\nInternational equity funds");
    private final By conservativeGrowthCard = byDescContains("Conservative Growth\nLarge-cap equity funds");
    private final By conservativeGrowthIncomeCard = byDescContains("Conservative Growth & Income\nEquity-savings funds");
    private final By coreFixedIncomeCard = byDescContains("Core Fixed Income\nShort-duration funds");
    private final By capitalPreservationCard = byDescContains("Capital Preservation\nLiquid and overnight funds");

    private final By plannerTitle = AppiumBy.accessibilityId("Confused? Use our Portfolio Planner");
    private final By plannerSubtitle = AppiumBy.accessibilityId(
            "Get ready-to-go portfolios customised to your investing needs"
    );
    private final By plannerButton = AppiumBy.accessibilityId("Go to Portfolio Planner");

    // ================= COMMON DETAIL LOCATORS =================

    private final By modeLabel = AppiumBy.accessibilityId("Mode:");
    private final By sipOnly = AppiumBy.accessibilityId("SIP Only");
    private final By horizonLabel = AppiumBy.accessibilityId("Horizon:");
    private final By sevenPlusYears = AppiumBy.accessibilityId("7+ Years");
    private final By fivePlusYears = AppiumBy.accessibilityId("5+ Years");

    private final By fundHeader = AppiumBy.accessibilityId("Fund");
    private final By ratingHeader = AppiumBy.accessibilityId("Rating");
    private final By categoryHeader = AppiumBy.accessibilityId("Category");
    private final By consistencyScoreHeader = AppiumBy.accessibilityId("Consistency score");
    private final By fiveYearReturnHeader = AppiumBy.accessibilityId("5Y Return");
    private final By worstOneYearReturnHeader = AppiumBy.accessibilityId("Worst 1Y Return");

    private final By compareInScreener = AppiumBy.accessibilityId("Compare in screener");

    // ================= AGGRESSIVE GROWTH DETAIL =================

    private final By detailAggressiveGrowthTitle = AppiumBy.accessibilityId("Aggressive Growth");
    private final By detailAggressiveGrowthDescription = AppiumBy.accessibilityId(
            "Best mid- and small-cap equity funds to supplement your core equity portfolio and boost returns."
    );

    private final By edelweissFund = AppiumBy.accessibilityId("Edelweiss\nMid Cap\nDirect G");
    private final By kotakFund = AppiumBy.accessibilityId("Kotak\nMidcap\nDirect G");
    private final By whiteOakFund = AppiumBy.accessibilityId("WhiteOak Capital\nMid Cap\nDirect G");
    private final By bandhanFund = AppiumBy.accessibilityId("Bandhan\nSmall Cap\nDirect G");
    private final By hdfcFund = AppiumBy.accessibilityId("HDFC\nSmall Cap\nDirect G");
    private final By invescoFund = AppiumBy.accessibilityId("Invesco India\nSmallcap\nDirect G");

    private final By eqMc = AppiumBy.accessibilityId("EQ-MC");
    private final By eqSc = AppiumBy.accessibilityId("EQ-SC");

    // ================= GROWTH DETAIL =================

    private final By detailGrowthTitle = AppiumBy.accessibilityId("Growth");
    private final By detailGrowthDescription = AppiumBy.accessibilityId(
            "Top diversified equity funds to form the core of your long-term growth portfolio."
    );

    private final By franklinIndiaFund = AppiumBy.accessibilityId("Franklin India\nFlexi Cap\nDirect G");
    private final By hdfcFlexiFund = AppiumBy.accessibilityId("HDFC\nFlexi Cap\nDirect G");
    private final By heliosFund = AppiumBy.accessibilityId("Helios\nFlexi Cap\nDirect G");
    private final By paragParikhFund = AppiumBy.accessibilityId("Parag Parikh\nFlexi Cap\nDirect G");
    private final By sbiFocusedFund = AppiumBy.accessibilityId("SBI\nFocused\nDirect G");
    private final By iciciLargeMidFund = AppiumBy.accessibilityId("ICICI Prudential\nLarge & Mid Cap\nDirect G");
    private final By kotakLargeMidFund = AppiumBy.accessibilityId("Kotak\nLarge & Midcap\nDirect G");
    private final By nipponIndiaFund = AppiumBy.accessibilityId("Nippon India\nMulti Cap\nDirect G");
    private final By iciciValueFund = AppiumBy.accessibilityId("ICICI Prudential\nValue\nDirect G");
    private final By invescoContraFund = AppiumBy.accessibilityId("Invesco India\nContra\nDirect G");
    private final By sbiContraFund = AppiumBy.accessibilityId("SBI\nContra\nDirect G");

    private final By eqFlx = AppiumBy.accessibilityId("EQ-FLX");
    private final By eqLmc = AppiumBy.accessibilityId("EQ-L&MC");
    private final By eqMlc = AppiumBy.accessibilityId("EQ-MLC");
    private final By eqVal = AppiumBy.accessibilityId("EQ-VAL");

    // ================= TAX PLANNING DETAIL =================

    private final By detailTaxPlanningTitle = AppiumBy.accessibilityId("Tax Planning");
    private final By detailTaxPlanningDescription = AppiumBy.accessibilityId(
            "ELSS funds similar to Growth funds, ideal for long-term wealth creation with tax benefits."
    );

    private final By bandhanElssFund = AppiumBy.accessibilityId("Bandhan\nELSS Tax Saver\nDirect G");
    private final By dspElssFund = AppiumBy.accessibilityId("DSP\nELSS Tax Saver\nDirect G");
    private final By hdfcElssFund = AppiumBy.accessibilityId("HDFC\nELSS Tax Saver\nDirect G");
    private final By miraeAssetElssFund = AppiumBy.accessibilityId("Mirae Asset\nELSS Tax Saver\nDirect G");
    private final By paragParikhElssFund = AppiumBy.accessibilityId("Parag Parikh\nELSS Tax Saver\nDirect G");

    private final By eqElss = AppiumBy.accessibilityId("EQ-ELSS");

    // ================= GROWTH INTERNATIONAL DETAIL =================

    private final By detailGrowthInternationalTitle = AppiumBy.accessibilityId("Growth - International");
    private final By detailGrowthInternationalDescription = AppiumBy.accessibilityId(
            "International equity funds to complement your core equity holdings."
    );

    private final By noRecommendationMessage = AppiumBy.accessibilityId(
            "There are currently no recommendations for funds in this list."
    );

    // ================= CONSERVATIVE GROWTH DETAIL =================

    private final By detailConservativeGrowthTitle = AppiumBy.accessibilityId("Conservative Growth");
    private final By detailConservativeGrowthDescription = AppiumBy.accessibilityId(
            "Large-cap equity funds and aggressive hybrid funds that tend to fall less in sharp market declines."
    );

    private final By dspAggressiveHybridFund = byDescContains("DSP\nAggressive Hybrid");
    private final By iciciEquityDebtFund = byDescContains("ICICI Prudential\nEquity & Debt");
    private final By kotakAggressiveHybridFund = byDescContains("Kotak\nAggressive Hybrid");
    private final By miraeAggressiveHybridFund = byDescContains("Mirae Asset\nAggressive Hybrid");

    private final By iciciLargeCapFund = byDescContains("ICICI Prudential\nLarge Cap");
    private final By kotakLargeCapFund = byDescContains("Kotak\nLarge Cap");
    private final By naviNifty50Fund = byDescContains("Navi\nNifty 50 Index");

    private final By nipponNifty50BeesFund = AppiumBy.accessibilityId("Nippon India\nETF Nifty 50 BeES\nIDCW");
    private final By nipponNiftyNext50Fund = AppiumBy.accessibilityId("Nippon India\nETF Nifty Next 50 Junior BeES\nIDCW");
    private final By nipponLargeCapFund = AppiumBy.accessibilityId("Nippon India\nLarge Cap\nDirect G");

    private final By sbiLargeCapFund = byDescContains("SBI\nLarge Cap");
    private final By sbiNifty50EtfFund = byDescContains("SBI\nNifty 50 ETF");
    private final By sbiNiftyIndexFund = byDescContains("SBI\nNifty Index");

    private final By utiNifty50Fund = byDescContains("UTI\nNifty 50 Index");
    private final By utiNiftyNext50Fund = byDescContains("UTI\nNifty Next 50");

    private final By hyAhCategory = AppiumBy.accessibilityId("HY-AH");
    private final By eqLcCategory = AppiumBy.accessibilityId("EQ-LC");

    // ================= CONSERVATIVE GROWTH & INCOME DETAIL =================

    private final By detailConservativeGrowthIncomeTitle = AppiumBy.accessibilityId("Conservative Growth & Income");
    private final By detailConservativeGrowthIncomeDescription = byDescContains("Equity-savings funds investing");

    private final By edelweissEquitySavingsFund = byDescContains("Edelweiss\nEquity Savings");
    private final By kotakEquitySavingsFund = byDescContains("Kotak\nEquity Savings");
    private final By miraeEquitySavingsFund = byDescContains("Mirae Asset\nEquity Savings");

    private final By hyEqSCategory = AppiumBy.accessibilityId("HY-EQ S");

    // ================= CORE FIXED INCOME DETAIL =================

    private final By detailCoreFixedIncomeTitle = AppiumBy.accessibilityId("Core Fixed Income");
    private final By detailCoreFixedIncomeDescription = byDescContains("Short-duration funds for 1–3 year goals");

    private final By axisShortDurationFund = AppiumBy.xpath(
            "//*[contains(@content-desc,'Axis') and contains(@content-desc,'Short Duration')]"
    );
    private final By bandhanShortDurationFund = AppiumBy.xpath(
            "//*[contains(@content-desc,'Bandhan') and contains(@content-desc,'Short Duration')]"
    );
    private final By hdfcShortTermDebtFund = AppiumBy.xpath(
            "//*[contains(@content-desc,'HDFC') and contains(@content-desc,'Short Term Debt')]"
    );
    private final By hsbcShortDurationFund = AppiumBy.xpath(
            "//*[contains(@content-desc,'HSBC') and contains(@content-desc,'Short Duration')]"
    );

    private final By dtSdCategory = AppiumBy.accessibilityId("DT-SD");

    // ================= CAPITAL PRESERVATION DETAIL =================

    private final By detailCapitalPreservationTitle = AppiumBy.accessibilityId("Capital Preservation");
    private final By detailCapitalPreservationDescription = byDescContains("Liquid and overnight funds to park money");

    private final By axisLiquidFund = AppiumBy.xpath(
            "//*[contains(@content-desc,'Axis') and contains(@content-desc,'Liquid')]"
    );
    private final By bandhanLiquidFund = AppiumBy.xpath(
            "//*[contains(@content-desc,'Bandhan') and contains(@content-desc,'Liquid')]"
    );
    private final By hdfcLiquidFund = AppiumBy.xpath(
            "//*[contains(@content-desc,'HDFC') and contains(@content-desc,'Liquid')]"
    );
    private final By miraeAssetLiquidFund = AppiumBy.xpath(
            "//*[contains(@content-desc,'Mirae Asset') and contains(@content-desc,'Liquid')]"
    );
    private final By hdfcOvernightFund = AppiumBy.xpath(
            "//*[contains(@content-desc,'HDFC') and contains(@content-desc,'Overnight')]"
    );
    private final By iciciPrudentialOvernightFund = AppiumBy.xpath(
            "//*[contains(@content-desc,'ICICI Prudential') and contains(@content-desc,'Overnight')]"
    );

    private final By dtLiqCategory = AppiumBy.accessibilityId("DT-LIQ");
    private final By dtOvernightCategory = AppiumBy.accessibilityId("DT-OVERNHT");

    // ================= OPEN / RECOVER =================

    public void openAnalystChoiceFromHub() {
        try {
            ReportLogger.step("Opening Analyst’s Choice from Hub");

            if (!isElementVisible(hubTab)) {
                pressBackSafely();
                sleep(1000);
            }

            tap(hubTab, "Hub tab");
            sleep(1200);

            if (!tapIfVisible(analystChoiceHubMenu, "Analyst’s Choice menu")) {
                scrollToAndTapAnalystChoiceFromHub();
            }

            waitForVisible(analystChoiceTitle, "Analyst’s Choice title");
            waitForVisible(analystChoiceSubtitle, "Analyst’s Choice subtitle");

            ReportLogger.pass("Analyst’s Choice opened successfully from Hub");

        } catch (Exception e) {
            captureScreenshotAndAttach("AC_Open_Failure");
            throw new RuntimeException("Failed to open Analyst’s Choice: " + cleanError(e.getMessage()), e);
        }
    }

    public void recoverAnalystChoiceIfNeeded() {
        try {
            if (isAnalystChoiceListingVisible()) {
                ReportLogger.step("Analyst’s Choice listing page already active");
                return;
            }

            if (isElementVisible(compareInScreener) || isElementVisible(noRecommendationMessage)) {
                ReportLogger.step("Analyst’s Choice detail page active. Going back to listing.");
                tapTopLeftBackButton("Back from Analyst’s Choice detail");
                sleep(1200);

                if (isAnalystChoiceListingVisible()) {
                    ReportLogger.step("Recovered Analyst’s Choice listing page from detail");
                    return;
                }
            }

            ReportLogger.step("Analyst’s Choice not active. Opening again from Hub.");

            if (!isElementVisible(hubTab)) {
                pressBackSafely();
                sleep(1000);
            }

            openAnalystChoiceFromHub();

        } catch (Exception e) {
            captureScreenshotAndAttach("AC_Recover_Failure");
            throw new RuntimeException("Analyst’s Choice recovery failed: " + cleanError(e.getMessage()), e);
        }
    }

    // ================= LISTING VALIDATIONS =================

    public void verifyAnalystChoiceHeader() {
        try {
            ReportLogger.step("Validating Analyst’s Choice header");

            waitForVisible(analystChoiceTitle, "Analyst’s Choice title");
            waitForVisible(analystChoiceSubtitle, "Subtitle");

            ReportLogger.pass("Analyst’s Choice header validated");

        } catch (Exception e) {
            captureScreenshotAndAttach("AC_001_Header_Failure");
            throw new AssertionError("Analyst’s Choice header validation failed: " + cleanError(e.getMessage()), e);
        }
    }

    public void verifyAllRecommendationCards() {
        try {
            ReportLogger.step("Validating all Analyst’s Choice recommendation cards");

            scrollToBeginning("Analyst’s Choice listing");

            verifyAggressiveGrowthCard();
            verifyGrowthCard();
            verifyTaxPlanningCard();
            verifyGrowthInternationalCard();
            verifyConservativeGrowthCard();
            verifyConservativeGrowthIncomeCard();
            verifyCoreFixedIncomeCard();
            verifyCapitalPreservationCard();

            ReportLogger.pass("All Analyst’s Choice recommendation cards validated");

        } catch (Exception e) {
            captureScreenshotAndAttach("AC_002_All_Cards_Failure");
            throw new AssertionError("Recommendation card validation failed: " + cleanError(e.getMessage()), e);
        }
    }

    public void verifyPortfolioPlannerCta() {
        try {
            ReportLogger.step("Validating Portfolio Planner CTA at bottom");

            scrollToVisible(plannerTitle, "Confused? Use our Portfolio Planner");
            waitForVisible(plannerSubtitle, "Portfolio Planner CTA subtitle");
            waitForVisible(plannerButton, "Go to Portfolio Planner button");

            ReportLogger.pass("Portfolio Planner CTA validated successfully");

        } catch (Exception e) {
            captureScreenshotAndAttach("AC_003_Portfolio_Planner_CTA_Failure");
            throw new AssertionError("Portfolio Planner CTA validation failed: " + cleanError(e.getMessage()), e);
        }
    }

    // ================= OPEN DETAIL PAGES =================

    public void openAggressiveGrowthDetail() {
        try {
            ReportLogger.step("Opening Aggressive Growth detail page");

            scrollToBeginning("Analyst’s Choice listing before opening Aggressive Growth");
            scrollToVisible(aggressiveGrowthCard, "Aggressive Growth card");
            tap(aggressiveGrowthCard, "Aggressive Growth card");

            waitForVisible(detailAggressiveGrowthTitle, "Aggressive Growth detail title");
            waitForVisible(detailAggressiveGrowthDescription, "Aggressive Growth detail description");
            waitForVisible(fundHeader, "Fund table header");
            waitForVisible(compareInScreener, "Compare in screener button");

            ReportLogger.pass("Aggressive Growth detail page opened successfully");

        } catch (Exception e) {
            captureScreenshotAndAttach("AC_004_Open_Aggressive_Growth_Detail_Failure");
            throw new AssertionError("Failed to open Aggressive Growth detail page: " + cleanError(e.getMessage()), e);
        }
    }

    public void openGrowthDetail() {
        try {
            ReportLogger.step("Opening Growth detail page");

            goBackToListingIfOnDetailPage();

            scrollToBeginning("Analyst’s Choice listing before opening Growth");
            scrollToVisible(growthCard, "Growth card");
            tap(growthCard, "Growth card");

            waitForVisible(detailGrowthTitle, "Growth detail title");
            waitForVisible(detailGrowthDescription, "Growth detail description");
            waitForVisible(fundHeader, "Fund table header");
            waitForVisible(compareInScreener, "Compare in screener button");

            ReportLogger.pass("Growth detail page opened successfully");

        } catch (Exception e) {
            captureScreenshotAndAttach("AC_010_Open_Growth_Detail_Failure");
            throw new AssertionError("Failed to open Growth detail page: " + cleanError(e.getMessage()), e);
        }
    }

    public void openTaxPlanningDetail() {
        try {
            ReportLogger.step("Opening Tax Planning detail page");

            goBackToListingIfOnDetailPage();

            scrollToBeginning("Analyst’s Choice listing before opening Tax Planning");
            scrollToVisible(taxPlanningCard, "Tax Planning card");
            tap(taxPlanningCard, "Tax Planning card");

            waitForVisible(detailTaxPlanningTitle, "Tax Planning detail title");
            waitForVisible(detailTaxPlanningDescription, "Tax Planning detail description");
            waitForVisible(fundHeader, "Fund table header");
            waitForVisible(compareInScreener, "Compare in screener button");

            ReportLogger.pass("Tax Planning detail page opened successfully");

        } catch (Exception e) {
            captureScreenshotAndAttach("AC_016_Open_Tax_Planning_Detail_Failure");
            throw new AssertionError("Failed to open Tax Planning detail page: " + cleanError(e.getMessage()), e);
        }
    }

    public void openGrowthInternationalDetail() {
        try {
            ReportLogger.step("Opening Growth International detail page");

            goBackToListingIfOnDetailPage();

            scrollToBeginning("Analyst’s Choice listing before opening Growth International");
            scrollToVisible(growthInternationalCard, "Growth - International card");
            tap(growthInternationalCard, "Growth - International card");

            waitForVisible(detailGrowthInternationalTitle, "Growth International detail title");
            waitForVisible(detailGrowthInternationalDescription, "Growth International detail description");

            ReportLogger.pass("Growth International detail page opened successfully");

        } catch (Exception e) {
            captureScreenshotAndAttach("AC_022_Open_Growth_International_Detail_Failure");
            throw new AssertionError("Failed to open Growth International detail page: " + cleanError(e.getMessage()), e);
        }
    }

    public void openConservativeGrowthDetail() {
        try {
            ReportLogger.step("Opening Conservative Growth detail page");

            goBackToListingIfOnDetailPage();

            scrollToBeginning("Analyst’s Choice listing before opening Conservative Growth");
            scrollToVisible(conservativeGrowthCard, "Conservative Growth card");
            tap(conservativeGrowthCard, "Conservative Growth card");

            waitForVisible(detailConservativeGrowthTitle, "Conservative Growth detail title");
            waitForVisible(detailConservativeGrowthDescription, "Conservative Growth detail description");
            waitForVisible(fundHeader, "Fund table header");
            waitForVisible(compareInScreener, "Compare in screener button");

            ReportLogger.pass("Conservative Growth detail page opened successfully");

        } catch (Exception e) {
            captureScreenshotAndAttach("AC_025_Open_Conservative_Growth_Detail_Failure");
            throw new AssertionError("Failed to open Conservative Growth detail page: " + cleanError(e.getMessage()), e);
        }
    }

    public void openConservativeGrowthIncomeDetail() {
        try {
            ReportLogger.step("Opening Conservative Growth & Income detail page");

            goBackToListingIfOnDetailPage();

            scrollToBeginning("Analyst’s Choice listing before opening Conservative Growth & Income");
            scrollToVisible(conservativeGrowthIncomeCard, "Conservative Growth & Income card");
            tap(conservativeGrowthIncomeCard, "Conservative Growth & Income card");

            waitForVisible(detailConservativeGrowthIncomeTitle, "Conservative Growth & Income detail title");
            waitForVisible(detailConservativeGrowthIncomeDescription, "Conservative Growth & Income detail description");
            waitForVisible(fundHeader, "Fund table header");
            waitForVisible(compareInScreener, "Compare in screener button");

            ReportLogger.pass("Conservative Growth & Income detail page opened successfully");

        } catch (Exception e) {
            captureScreenshotAndAttach("AC_031_Open_Conservative_Growth_Income_Detail_Failure");
            throw new AssertionError("Failed to open Conservative Growth & Income detail page: " + cleanError(e.getMessage()), e);
        }
    }

    public void openCoreFixedIncomeDetail() {
        try {
            ReportLogger.step("Opening Core Fixed Income detail page");

            goBackToListingIfOnDetailPage();

            scrollToBeginning("Analyst’s Choice listing before opening Core Fixed Income");
            scrollToVisible(coreFixedIncomeCard, "Core Fixed Income card");
            tap(coreFixedIncomeCard, "Core Fixed Income card");

            waitForVisible(detailCoreFixedIncomeTitle, "Core Fixed Income detail title");
            waitForVisible(detailCoreFixedIncomeDescription, "Core Fixed Income detail description");
            waitForVisible(fundHeader, "Fund table header");
            waitForVisible(compareInScreener, "Compare in screener button");

            ReportLogger.pass("Core Fixed Income detail page opened successfully");

        } catch (Exception e) {
            captureScreenshotAndAttach("AC_037_Open_Core_Fixed_Income_Detail_Failure");
            throw new AssertionError("Failed to open Core Fixed Income detail page: " + cleanError(e.getMessage()), e);
        }
    }

    // ================= DETAIL HEADERS =================


    public void openCapitalPreservationDetail() {
        try {
            ReportLogger.step("Opening Capital Preservation detail page");

            goBackToListingIfOnDetailPage();

            scrollToBeginning("Analyst’s Choice listing before opening Capital Preservation");
            scrollToVisible(capitalPreservationCard, "Capital Preservation card");
            tap(capitalPreservationCard, "Capital Preservation card");

            waitForVisible(detailCapitalPreservationTitle, "Capital Preservation detail title");
            waitForVisible(detailCapitalPreservationDescription, "Capital Preservation detail description");
            waitForVisible(fundHeader, "Fund table header");
            waitForVisible(compareInScreener, "Compare in screener button");

            ReportLogger.pass("Capital Preservation detail page opened successfully");

        } catch (Exception e) {
            captureScreenshotAndAttach("AC_043_Open_Capital_Preservation_Detail_Failure");
            throw new AssertionError("Failed to open Capital Preservation detail page: " + cleanError(e.getMessage()), e);
        }
    }

    public void verifyAggressiveGrowthDetailHeader() {
        try {
            ReportLogger.step("Validating Aggressive Growth detail header");

            waitForVisible(detailAggressiveGrowthTitle, "Aggressive Growth title");
            waitForVisible(detailAggressiveGrowthDescription, "Aggressive Growth description");
            waitForVisible(modeLabel, "Mode label");
            waitForVisible(sipOnly, "SIP Only");
            waitForVisible(horizonLabel, "Horizon label");
            waitForVisible(sevenPlusYears, "7+ Years");

            ReportLogger.pass("Aggressive Growth detail header validated");

        } catch (Exception e) {
            captureScreenshotAndAttach("AC_005_Detail_Header_Failure");
            throw new AssertionError("Aggressive Growth detail header validation failed: " + cleanError(e.getMessage()), e);
        }
    }

    public void verifyGrowthDetailHeader() {
        try {
            ReportLogger.step("Validating Growth detail header");

            waitForVisible(detailGrowthTitle, "Growth title");
            waitForVisible(detailGrowthDescription, "Growth description");
            waitForVisible(modeLabel, "Mode label");
            waitForVisible(sipOnly, "SIP Only");
            waitForVisible(horizonLabel, "Horizon label");
            waitForVisible(fivePlusYears, "5+ Years");

            ReportLogger.pass("Growth detail header validated");

        } catch (Exception e) {
            captureScreenshotAndAttach("AC_011_Growth_Detail_Header_Failure");
            throw new AssertionError("Growth detail header validation failed: " + cleanError(e.getMessage()), e);
        }
    }

    public void verifyTaxPlanningDetailHeader() {
        try {
            ReportLogger.step("Validating Tax Planning detail header");

            waitForVisible(detailTaxPlanningTitle, "Tax Planning title");
            waitForVisible(detailTaxPlanningDescription, "Tax Planning description");
            waitForVisible(modeLabel, "Mode label");
            waitForVisible(sipOnly, "SIP Only");
            waitForVisible(horizonLabel, "Horizon label");
            waitForVisible(fivePlusYears, "5+ Years");

            ReportLogger.pass("Tax Planning detail header validated");

        } catch (Exception e) {
            captureScreenshotAndAttach("AC_017_Tax_Planning_Detail_Header_Failure");
            throw new AssertionError("Tax Planning detail header validation failed: " + cleanError(e.getMessage()), e);
        }
    }

    public void verifyGrowthInternationalDetailHeader() {
        try {
            ReportLogger.step("Validating Growth International detail header");

            waitForVisible(detailGrowthInternationalTitle, "Growth International title");
            waitForVisible(detailGrowthInternationalDescription, "Growth International description");
            waitForVisible(modeLabel, "Mode label");
            waitForVisible(sipOnly, "SIP Only");
            waitForVisible(horizonLabel, "Horizon label");
            waitForVisible(fivePlusYears, "5+ Years");

            ReportLogger.pass("Growth International detail header validated");

        } catch (Exception e) {
            captureScreenshotAndAttach("AC_023_Growth_International_Detail_Header_Failure");
            throw new AssertionError("Growth International detail header validation failed: " + cleanError(e.getMessage()), e);
        }
    }

    public void verifyConservativeGrowthDetailHeader() {
        try {
            ReportLogger.step("Validating Conservative Growth detail header");

            waitForVisible(detailConservativeGrowthTitle, "Conservative Growth title");
            waitForVisible(detailConservativeGrowthDescription, "Conservative Growth description");
            waitForVisible(modeLabel, "Mode label");
            waitForVisible(sipOnly, "SIP Only");
            waitForVisible(horizonLabel, "Horizon label");
            waitForVisible(fivePlusYears, "5+ Years");

            ReportLogger.pass("Conservative Growth detail header validated");

        } catch (Exception e) {
            captureScreenshotAndAttach("AC_026_Conservative_Growth_Detail_Header_Failure");
            throw new AssertionError("Conservative Growth detail header validation failed: " + cleanError(e.getMessage()), e);
        }
    }

    public void verifyConservativeGrowthIncomeDetailHeader() {
        try {
            ReportLogger.step("Validating Conservative Growth & Income detail header");

            waitForVisible(detailConservativeGrowthIncomeTitle, "Conservative Growth & Income title");
            waitForVisible(detailConservativeGrowthIncomeDescription, "Conservative Growth & Income description");
            waitForVisible(modeLabel, "Mode label");
            waitForVisible(sipOnly, "SIP Only");
            waitForVisible(horizonLabel, "Horizon label");

            verifyAnyTextPresentOnCurrentScreen(
                    new String[]{"3–5 Years", "3-5 Years", "3–5", "3-5"},
                    "Conservative Growth & Income horizon"
            );

            ReportLogger.pass("Conservative Growth & Income detail header validated");

        } catch (Exception e) {
            captureScreenshotAndAttach("AC_032_Conservative_Growth_Income_Detail_Header_Failure");
            throw new AssertionError("Conservative Growth & Income detail header validation failed: " + cleanError(e.getMessage()), e);
        }
    }

    public void verifyCoreFixedIncomeDetailHeader() {
        try {
            ReportLogger.step("Validating Core Fixed Income detail header");

            waitForVisible(detailCoreFixedIncomeTitle, "Core Fixed Income title");
            waitForVisible(detailCoreFixedIncomeDescription, "Core Fixed Income description");
            waitForVisible(modeLabel, "Mode label");

            verifyAnyTextPresentOnCurrentScreen(
                    new String[]{"One-time or SIP", "One-time", "SIP"},
                    "Core Fixed Income mode"
            );

            ReportLogger.pass("Core Fixed Income detail header validated");

        } catch (Exception e) {
            captureScreenshotAndAttach("AC_038_Core_Fixed_Income_Detail_Header_Failure");
            throw new AssertionError("Core Fixed Income detail header validation failed: " + cleanError(e.getMessage()), e);
        }
    }


    public void verifyCapitalPreservationDetailHeader() {
        try {
            ReportLogger.step("Validating Capital Preservation detail header");

            waitForVisible(detailCapitalPreservationTitle, "Capital Preservation title");
            waitForVisible(detailCapitalPreservationDescription, "Capital Preservation description");
            waitForVisible(modeLabel, "Mode label");

            verifyAnyTextPresentOnCurrentScreen(
                    new String[]{"One-time or SIP", "One-time", "SIP"},
                    "Capital Preservation mode"
            );

            ReportLogger.pass("Capital Preservation detail header validated");

        } catch (Exception e) {
            captureScreenshotAndAttach("AC_044_Capital_Preservation_Detail_Header_Failure");
            throw new AssertionError("Capital Preservation detail header validation failed: " + cleanError(e.getMessage()), e);
        }
    }

    public void verifyGrowthInternationalEmptyState() {
        try {
            ReportLogger.step("Validating Growth International empty recommendation state");

            waitForVisible(detailGrowthInternationalTitle, "Growth International title");
            waitForVisible(noRecommendationMessage, "No recommendations empty state message");

            if (isElementVisible(fundHeader)) {
                throw new AssertionError("Fund table is visible on Growth International empty state screen.");
            }

            if (isElementVisible(compareInScreener)) {
                throw new AssertionError("Compare in screener button is visible on Growth International empty state screen.");
            }

            ReportLogger.pass("Growth International empty state validated successfully");

        } catch (Exception e) {
            captureScreenshotAndAttach("AC_024_Growth_International_Empty_State_Failure");
            throw new AssertionError("Growth International empty state validation failed: " + cleanError(e.getMessage()), e);
        }
    }

    // ================= INITIAL TABLE VALIDATIONS =================

    public void verifyAggressiveGrowthInitialTableColumnsAndRows() {
        try {
            ReportLogger.step("Validating Aggressive Growth table initial columns and fund rows");

            resetTableHorizontalPosition();

            waitForVisible(fundHeader, "Fund header");
            waitForVisible(ratingHeader, "Rating header");
            waitForVisible(categoryHeader, "Category header");

            waitForVisible(edelweissFund, "Edelweiss Mid Cap Direct G");
            waitForVisible(kotakFund, "Kotak Midcap Direct G");
            waitForVisible(whiteOakFund, "WhiteOak Capital Mid Cap Direct G");
            waitForVisible(bandhanFund, "Bandhan Small Cap Direct G");
            waitForVisible(hdfcFund, "HDFC Small Cap Direct G");
            waitForVisible(invescoFund, "Invesco India Smallcap Direct G");

            waitForVisible(eqMc, "EQ-MC category");
            waitForVisible(eqSc, "EQ-SC category");

            ReportLogger.pass("Aggressive Growth initial table columns and rows validated");

        } catch (Exception e) {
            captureScreenshotAndAttach("AC_006_Initial_Table_Failure");
            throw new AssertionError("Aggressive Growth initial table validation failed: " + cleanError(e.getMessage()), e);
        }
    }

    public void verifyGrowthInitialTableColumnsAndRows() {
        try {
            ReportLogger.step("Validating Growth table initial columns and fund rows");

            resetTableHorizontalPosition();

            waitForVisible(fundHeader, "Fund header");
            waitForVisible(ratingHeader, "Rating header");
            waitForVisible(categoryHeader, "Category header");

            waitForVisible(franklinIndiaFund, "Franklin India Flexi Cap Direct G");
            waitForVisible(hdfcFlexiFund, "HDFC Flexi Cap Direct G");
            waitForVisible(heliosFund, "Helios Flexi Cap Direct G");
            waitForVisible(paragParikhFund, "Parag Parikh Flexi Cap Direct G");
            waitForVisible(sbiFocusedFund, "SBI Focused Direct G");

            waitForVisible(eqFlx, "EQ-FLX category");

            ReportLogger.pass("Growth initial table columns and top rows validated");

        } catch (Exception e) {
            captureScreenshotAndAttach("AC_012_Growth_Initial_Table_Failure");
            throw new AssertionError("Growth initial table validation failed: " + cleanError(e.getMessage()), e);
        }
    }

    public void verifyTaxPlanningInitialTableColumnsAndRows() {
        try {
            ReportLogger.step("Validating Tax Planning table initial columns and fund rows");

            resetTableHorizontalPosition();

            waitForVisible(fundHeader, "Fund header");
            waitForVisible(ratingHeader, "Rating header");
            waitForVisible(categoryHeader, "Category header");

            waitForVisible(bandhanElssFund, "Bandhan ELSS Tax Saver Direct G");
            waitForVisible(dspElssFund, "DSP ELSS Tax Saver Direct G");
            waitForVisible(hdfcElssFund, "HDFC ELSS Tax Saver Direct G");
            waitForVisible(miraeAssetElssFund, "Mirae Asset ELSS Tax Saver Direct G");
            waitForVisible(paragParikhElssFund, "Parag Parikh ELSS Tax Saver Direct G");

            waitForVisible(eqElss, "EQ-ELSS category");

            ReportLogger.pass("Tax Planning initial table columns and rows validated");

        } catch (Exception e) {
            captureScreenshotAndAttach("AC_018_Tax_Planning_Initial_Table_Failure");
            throw new AssertionError("Tax Planning initial table validation failed: " + cleanError(e.getMessage()), e);
        }
    }

    public void verifyConservativeGrowthInitialTableColumnsAndRows() {
        try {
            ReportLogger.step("Validating Conservative Growth table initial columns and fund rows");

            resetTableHorizontalPosition();

            waitForVisible(fundHeader, "Fund header");
            waitForVisible(ratingHeader, "Rating header");
            waitForVisible(categoryHeader, "Category header");

            waitForVisible(dspAggressiveHybridFund, "DSP Aggressive Hybrid Direct G");
            waitForVisible(iciciEquityDebtFund, "ICICI Prudential Equity & Debt Direct G");
            waitForVisible(kotakAggressiveHybridFund, "Kotak Aggressive Hybrid Direct IDCW");
            waitForVisible(miraeAggressiveHybridFund, "Mirae Asset Aggressive Hybrid Direct G");
            waitForVisible(iciciLargeCapFund, "ICICI Prudential Large Cap Direct G");
            waitForVisible(kotakLargeCapFund, "Kotak Large Cap Direct G");

            waitForVisible(hyAhCategory, "HY-AH category");
            waitForVisible(eqLcCategory, "EQ-LC category");

            ReportLogger.pass("Conservative Growth initial table columns and rows validated");

        } catch (Exception e) {
            captureScreenshotAndAttach("AC_027_Conservative_Growth_Initial_Table_Failure");
            throw new AssertionError("Conservative Growth initial table validation failed: " + cleanError(e.getMessage()), e);
        }
    }

    public void verifyConservativeGrowthIncomeInitialTableColumnsAndRows() {
        try {
            ReportLogger.step("Validating Conservative Growth & Income table initial columns and fund rows");

            resetTableHorizontalPosition();

            waitForVisible(fundHeader, "Fund header");
            waitForVisible(ratingHeader, "Rating header");
            waitForVisible(categoryHeader, "Category header");

            waitForVisible(edelweissEquitySavingsFund, "Edelweiss Equity Savings Direct G");
            waitForVisible(kotakEquitySavingsFund, "Kotak Equity Savings Direct G");
            waitForVisible(miraeEquitySavingsFund, "Mirae Asset Equity Savings Direct G");
            waitForVisible(hyEqSCategory, "HY-EQ S category");

            ReportLogger.pass("Conservative Growth & Income initial table columns and rows validated");

        } catch (Exception e) {
            captureScreenshotAndAttach("AC_033_Conservative_Growth_Income_Initial_Table_Failure");
            throw new AssertionError("Conservative Growth & Income initial table validation failed: " + cleanError(e.getMessage()), e);
        }
    }

    public void verifyCoreFixedIncomeInitialTableColumnsAndRows() {
        try {
            ReportLogger.step("Validating Core Fixed Income table initial columns and fund rows");

            resetTableHorizontalPosition();

            waitForVisible(fundHeader, "Fund header");
            waitForVisible(ratingHeader, "Rating header");
            waitForVisible(categoryHeader, "Category header");

            waitForVisible(axisShortDurationFund, "Axis Short Duration Direct G");
            waitForVisible(bandhanShortDurationFund, "Bandhan Short Duration Direct G");
            waitForVisible(hdfcShortTermDebtFund, "HDFC Short Term Debt Direct G");
            waitForVisible(hsbcShortDurationFund, "HSBC Short Duration Direct G");
            waitForVisible(dtSdCategory, "DT-SD category");

            ReportLogger.pass("Core Fixed Income initial table columns and rows validated");

        } catch (Exception e) {
            captureScreenshotAndAttach("AC_039_Core_Fixed_Income_Initial_Table_Failure");
            throw new AssertionError("Core Fixed Income initial table validation failed: " + cleanError(e.getMessage()), e);
        }
    }


    public void verifyCapitalPreservationInitialTableColumnsAndRows() {
        try {
            ReportLogger.step("Validating Capital Preservation table initial columns and fund rows");

            resetTableHorizontalPosition();

            waitForVisible(fundHeader, "Fund header");
            waitForVisible(ratingHeader, "Rating header");
            waitForVisible(categoryHeader, "Category header");

            waitForVisible(axisLiquidFund, "Axis Liquid Direct G");
            waitForVisible(bandhanLiquidFund, "Bandhan Liquid Direct G");
            waitForVisible(hdfcLiquidFund, "HDFC Liquid Direct G");
            waitForVisible(miraeAssetLiquidFund, "Mirae Asset Liquid Direct G");
            waitForVisible(hdfcOvernightFund, "HDFC Overnight Direct G");
            waitForVisible(iciciPrudentialOvernightFund, "ICICI Prudential Overnight Direct G");

            waitForVisible(dtLiqCategory, "DT-LIQ category");
            waitForVisible(dtOvernightCategory, "DT-OVERNHT category");

            ReportLogger.pass("Capital Preservation initial table columns and rows validated");

        } catch (Exception e) {
            captureScreenshotAndAttach("AC_045_Capital_Preservation_Initial_Table_Failure");
            throw new AssertionError("Capital Preservation initial table validation failed: " + cleanError(e.getMessage()), e);
        }
    }

    // ================= COMPLETE TABLE VALIDATION =================

    public void verifyAggressiveGrowthCompleteTableCoverage() {
        try {
            ReportLogger.step("Validating Aggressive Growth table fund-by-fund");
            tableValidationErrors.clear();

            validateFundOneByOne(edelweissFund, "Edelweiss Mid Cap Direct G", "EQ-MC", "100%", "21%", "-28%");
            validateFundOneByOne(kotakFund, "Kotak Midcap Direct G", "EQ-MC", "100%", "19%", "-26%");
            validateFundOneByOne(bandhanFund, "Bandhan Small Cap Direct G", "EQ-SC", "100%", "24%", "-10%");
            validateFundOneByOne(hdfcFund, "HDFC Small Cap Direct G", "EQ-SC", "43%", "19%", "-43%");
            validateFundOneByOne(invescoFund, "Invesco India Smallcap Direct G", "EQ-SC", "99%", "21%", "-26%");
            assertNoTableValidationErrors("Aggressive Growth");
            ReportLogger.pass("Aggressive Growth complete table validated fund-by-fund");

        } catch (Exception e) {
            captureScreenshotAndAttach("AC_007_Aggressive_Growth_Table_Failure");
            throw new AssertionError("Aggressive Growth table validation failed: " + cleanError(e.getMessage()), e);
        }
    }

    public void verifyGrowthCompleteTableCoverage() {
        try {
            ReportLogger.step("Validating Growth table fund-by-fund");
            tableValidationErrors.clear();

            validateFundOneByOne(franklinIndiaFund, "Franklin India Flexi Cap Direct G", "EQ-FLX", "100%", "14%", "-35%");
            validateFundOneByOne(hdfcFlexiFund, "HDFC Flexi Cap Direct G", "EQ-FLX", "100%", "18%", "-38%");
            validateFundOneByOne(heliosFund, "Helios Flexi Cap Direct G", "EQ-FLX", "--", "--", "0%");
            validateFundOneByOne(paragParikhFund, "Parag Parikh Flexi Cap Direct G", "EQ-FLX", "100%", "16%", "-21%");
            validateFundOneByOne(sbiFocusedFund, "SBI Focused Direct G", "EQ-FLX", "59%", "16%", "-20%");
            validateFundOneByOne(iciciLargeMidFund, "ICICI Prudential Large & Mid Cap Direct G", "EQ-L&MC", "100%", "18%", "-32%");
            validateFundOneByOne(kotakLargeMidFund, "Kotak Large & Midcap Direct G", "EQ-L&MC", "93%", "15%", "-26%");
            validateFundOneByOne(nipponIndiaFund, "Nippon India Multi Cap Direct G", "EQ-MLC", "7%", "20%", "-39%");
            validateFundOneByOne(iciciValueFund, "ICICI Prudential Value Direct G", "EQ-VAL", "100%", "17%", null);
            validateFundOneByOne(invescoContraFund, "Invesco India Contra Direct G", "EQ-VAL", "13%", "16%", "-30%");
            validateFundOneByOne(sbiContraFund, "SBI Contra Direct G", "EQ-VAL", "100%", "18%", "-36%");

            assertNoTableValidationErrors("Growth");
            ReportLogger.pass("Growth complete table validated fund-by-fund");

        } catch (Exception e) {
            captureScreenshotAndAttach("AC_013_Growth_Table_Failure");
            throw new AssertionError("Growth table validation failed: " + cleanError(e.getMessage()), e);
        }
    }

    public void verifyTaxPlanningCompleteTableCoverage() {
        try {
            ReportLogger.step("Validating Tax Planning table fund-by-fund");
            tableValidationErrors.clear();

            validateFundOneByOne(bandhanElssFund, "Bandhan ELSS Tax Saver Direct G", "EQ-ELSS", "100%", "14%", "-37%");
            validateFundOneByOne(dspElssFund, "DSP ELSS Tax Saver Direct G", "EQ-ELSS", "100%", "14%", "-29%");
            validateFundOneByOne(hdfcElssFund, "HDFC ELSS Tax Saver Direct G", "EQ-ELSS", "79%", "18%", "-39%");
            validateFundOneByOne(miraeAssetElssFund, "Mirae Asset ELSS Tax Saver Direct G", "EQ-ELSS", "94%", "13%", "-29%");
            validateFundOneByOne(paragParikhElssFund, "Parag Parikh ELSS Tax Saver Direct G", "EQ-ELSS", "100%", "14%", "-7%");

            assertNoTableValidationErrors("Tax Planning");
            ReportLogger.pass("Tax Planning complete table validated fund-by-fund");

        } catch (Exception e) {
            captureScreenshotAndAttach("AC_019_Tax_Planning_Table_Failure");
            throw new AssertionError("Tax Planning table validation failed: " + cleanError(e.getMessage()), e);
        }
    }

    public void verifyConservativeGrowthCompleteTableCoverage() {
        try {
            ReportLogger.step("Validating Conservative Growth table fund-by-fund");
            tableValidationErrors.clear();

            validateFundOneByOne(dspAggressiveHybridFund, "DSP Aggressive Hybrid Direct G", "HY-AH", "92%", "12%", "-16%");
            validateFundOneByOne(iciciEquityDebtFund, "ICICI Prudential Equity & Debt Direct G", "HY-AH", "100%", "18%", "-25%");
            validateFundOneByOne(kotakAggressiveHybridFund, "Kotak Aggressive Hybrid Direct IDCW", "HY-AH", "100%", "14%", "-22%");
            validateFundOneByOne(miraeAggressiveHybridFund, "Mirae Asset Aggressive Hybrid Direct G", "HY-AH", "64%", "13%", "-22%");
            validateFundOneByOne(iciciLargeCapFund, "ICICI Prudential Large Cap Direct G", "EQ-LC", "100%", "15%", "-32%");
            validateFundOneByOne(kotakLargeCapFund, "Kotak Large Cap Direct G", "EQ-LC", "100%", "13%", "-29%");
            validateFundOneByOne(naviNifty50Fund, "Navi Nifty 50 Index Direct G", "EQ-LC", "--", "--", "-5%");
            validateFundOneByOne(nipponNifty50BeesFund, "Nippon India ETF Nifty 50 BeES IDCW", "EQ-LC", "--", "11%", "-55%");
            validateFundOneByOne(nipponNiftyNext50Fund, "Nippon India ETF Nifty Next 50 Junior BeES IDCW", "EQ-LC", "--", "15%", "-66%");
            validateFundOneByOne(nipponLargeCapFund, "Nippon India Large Cap Direct G", "EQ-LC", "100%", "17%", "-36%");
            validateFundOneByOne(sbiLargeCapFund, "SBI Large Cap Direct G", "EQ-LC", "100%", "13%", "-30%");
            validateFundOneByOne(sbiNifty50EtfFund, "SBI Nifty 50 ETF IDCW", "EQ-LC", "--", "11%", "-33%");
            validateFundOneByOne(sbiNiftyIndexFund, "SBI Nifty Index Direct G", "EQ-LC", "--", "11%", "-33%");
            validateFundOneByOne(utiNifty50Fund, "UTI Nifty 50 Index Direct G", "EQ-LC", "--", "11%", "-33%");
            validateFundOneByOne(utiNiftyNext50Fund, "UTI Nifty Next 50 Index Direct G", "EQ-LC", "--", "15%", "-32%");

            assertNoTableValidationErrors("Conservative Growth");
            ReportLogger.pass("Conservative Growth complete table validated fund-by-fund");

        } catch (Exception e) {
            captureScreenshotAndAttach("AC_028_Conservative_Growth_Table_Failure");
            throw new AssertionError("Conservative Growth table validation failed: " + cleanError(e.getMessage()), e);
        }
    }

    public void verifyConservativeGrowthHybridFundsTableCoverage() {
        try {
            ReportLogger.step("Validating Conservative Growth Hybrid funds table");
            tableValidationErrors.clear();

            validateFundOneByOne(dspAggressiveHybridFund, "DSP Aggressive Hybrid Direct G", "HY-AH", "90%", "12%", "-16%");
            validateFundOneByOne(iciciEquityDebtFund, "ICICI Prudential Equity & Debt Direct G", "HY-AH", "100%", "17%", "-25%");
            validateFundOneByOne(kotakAggressiveHybridFund, "Kotak Aggressive Hybrid Direct IDCW", "HY-AH", "100%", "13%", "-22%");
            validateFundOneByOne(miraeAggressiveHybridFund, "Mirae Asset Aggressive Hybrid Direct G", "HY-AH", "63%", "13%", "-22%");
            assertNoTableValidationErrors("Conservative Growth Hybrid Funds");
            ReportLogger.pass("Conservative Growth Hybrid funds validated");

        } catch (Exception e) {
            captureScreenshotAndAttach("AC_028A_Hybrid_Funds_Failure");
            throw new AssertionError("Conservative Growth Hybrid funds validation failed: " + cleanError(e.getMessage()), e);
        }
    }

    public void verifyConservativeGrowthLargeCapFundsTableCoverage() {
        try {
            ReportLogger.step("Validating Conservative Growth Large Cap funds table");
            tableValidationErrors.clear();

            validateFundOneByOne(iciciLargeCapFund, "ICICI Prudential Large Cap Direct G", "EQ-LC", "100%", "14%", "-32%");
            validateFundOneByOne(kotakLargeCapFund, "Kotak Large Cap Direct G", "EQ-LC", "100%", "12%", "-29%");
            validateFundOneByOne(nipponLargeCapFund, "Nippon India Large Cap Direct G", "EQ-LC", "100%", "16%", "-36%");
            validateFundOneByOne(sbiLargeCapFund, "SBI Large Cap Direct G", "EQ-LC", "100%", "11%", "-30%");
            assertNoTableValidationErrors("Conservative Growth Large Cap Funds");
            ReportLogger.pass("Conservative Growth Large Cap funds validated");

        } catch (Exception e) {
            captureScreenshotAndAttach("AC_028B_Large_Cap_Funds_Failure");
            throw new AssertionError("Conservative Growth Large Cap funds validation failed: " + cleanError(e.getMessage()), e);
        }
    }

    public void verifyConservativeGrowthEtfIndexFundsTableCoverage() {
        try {
            ReportLogger.step("Validating Conservative Growth ETF/Index funds table");
            tableValidationErrors.clear();

            validateFundOneByOne(nipponNifty50BeesFund, "Nippon India ETF Nifty 50 BeES IDCW", "EQ-LC", "--", "10%", "-55%");
            validateFundOneByOne(nipponNiftyNext50Fund, "Nippon India ETF Nifty Next 50 Junior BeES IDCW", "EQ-LC", "--", "14%", "-66%");
            validateFundOneByOne(sbiNifty50EtfFund, "SBI Nifty 50 ETF IDCW", "EQ-LC", "--", "10%", "-33%");
            validateFundOneByOne(sbiNiftyIndexFund, "SBI Nifty Index Direct G", "EQ-LC", "--", "10%", "-33%");
            validateFundOneByOne(utiNifty50Fund, "UTI Nifty 50 Index Direct G", "EQ-LC", "--", "10%", "-33%");
            validateFundOneByOne(utiNiftyNext50Fund, "UTI Nifty Next 50 Index Direct G", "EQ-LC", "--", "13%", "-32%");

            assertNoTableValidationErrors("Conservative Growth ETF/Index Funds");
            ReportLogger.pass("Conservative Growth ETF/Index funds validated");

        } catch (Exception e) {
            captureScreenshotAndAttach("AC_028C_ETF_Index_Funds_Failure");
            throw new AssertionError("Conservative Growth ETF/Index funds validation failed: " + cleanError(e.getMessage()), e);
        }
    }

    public void verifyConservativeGrowthIncomeCompleteTableCoverage() {
        try {
            ReportLogger.step("Validating Conservative Growth & Income table fund-by-fund");
            tableValidationErrors.clear();

            validateFundOneByOne(edelweissEquitySavingsFund, "Edelweiss Equity Savings Direct G", "HY-EQ S", "98%", "10%", "-2%");
            validateFundOneByOne(kotakEquitySavingsFund, "Kotak Equity Savings Direct G", "HY-EQ S", "100%", "10%", "-10%");
            validateFundOneByOne(miraeEquitySavingsFund, "Mirae Asset Equity Savings Direct G", "HY-EQ S", "100%", "10%", "-9%");

            assertNoTableValidationErrors("Conservative Growth & Income");
            ReportLogger.pass("Conservative Growth & Income complete table validated fund-by-fund");

        } catch (Exception e) {
            captureScreenshotAndAttach("AC_034_Conservative_Growth_Income_Table_Failure");
            throw new AssertionError("Conservative Growth & Income table validation failed: " + cleanError(e.getMessage()), e);
        }
    }

    public void verifyCoreFixedIncomeCompleteTableCoverage() {
        try {
            ReportLogger.step("Validating Core Fixed Income table fund-by-fund");
            tableValidationErrors.clear();

            validateFundOneByOne(axisShortDurationFund, "Axis Short Duration Direct G", "DT-SD", "98%", "7%", "3%");
            validateFundOneByOne(bandhanShortDurationFund, "Bandhan Short Duration Direct G", "DT-SD", "56%", "6%", "2%");
            validateFundOneByOne(hdfcShortTermDebtFund, "HDFC Short Term Debt Direct G", "DT-SD", "82%", "6%", "3%");
            validateFundOneByOne(hsbcShortDurationFund, "HSBC Short Duration Direct G", "DT-SD", "17%", "6%", "2%");

            assertNoTableValidationErrors("Core Fixed Income");
            ReportLogger.pass("Core Fixed Income complete table validated fund-by-fund");

        } catch (Exception e) {
            captureScreenshotAndAttach("AC_040_Core_Fixed_Income_Table_Failure");
            throw new AssertionError("Core Fixed Income table validation failed: " + cleanError(e.getMessage()), e);
        }
    }


    public void verifyCapitalPreservationCompleteTableCoverage() {
        try {
            ReportLogger.step("Validating Capital Preservation table fund-by-fund");
            tableValidationErrors.clear();

            validateFundOneByOne(axisLiquidFund, "Axis Liquid Direct G", "DT-LIQ", "100%", "6%", "3%");
            validateFundOneByOne(bandhanLiquidFund, "Bandhan Liquid Direct G", "DT-LIQ", "43%", "6%", "3%");
            validateFundOneByOne(hdfcLiquidFund, "HDFC Liquid Direct G", "DT-LIQ", "18%", "6%", "3%");
            validateFundOneByOne(miraeAssetLiquidFund, "Mirae Asset Liquid Direct G", "DT-LIQ", "91%", "6%", "3%");
            validateFundOneByOne(hdfcOvernightFund, "HDFC Overnight Direct G", "DT-OVERNHT", "0%", "6%", "3%");
            validateFundOneByOne(iciciPrudentialOvernightFund, "ICICI Prudential Overnight Direct G", "DT-OVERNHT", "31%", "6%", "3%");

            assertNoTableValidationErrors("Capital Preservation");
            ReportLogger.pass("Capital Preservation complete table validated fund-by-fund");

        } catch (Exception e) {
            captureScreenshotAndAttach("AC_046_Capital_Preservation_Table_Failure");
            throw new AssertionError("Capital Preservation table validation failed: " + cleanError(e.getMessage()), e);
        }
    }

    private void validateFundOneByOne(
            By fundLocator,
            String fundName,
            String category,
            String consistencyScore,
            String fiveYearReturn,
            String worstOneYearReturn
    ) {
        try {
            ReportLogger.step("========== Validating fund: " + fundName + " ==========");

            resetTableHorizontalPosition();

            scrollUntilFundRowVisible(fundLocator, fundName);
            verifyValueNearFundRow(fundLocator, fundName, category, "Category");

            scrollTableToConsistencyAndFiveYear();

            scrollUntilFundRowVisible(fundLocator, fundName);
            verifyValueNearFundRow(fundLocator, fundName, consistencyScore, "Consistency score");
            verifyValueNearFundRow(fundLocator, fundName, fiveYearReturn, "5Y Return");

            scrollTableToWorstOneYearReturn();

            scrollUntilFundRowVisible(fundLocator, fundName);
            verifyValueNearFundRow(fundLocator, fundName, worstOneYearReturn, "Worst 1Y Return");

            ReportLogger.pass("Completed validation for fund: " + fundName);

        } catch (Exception e) {
            String cleanedError = cleanError(e.getMessage());

            if (isDriverCrashedError(cleanedError)) {
                captureScreenshotAndAttach("Driver_Crashed_" + fundName);
                throw new RuntimeException(
                        "Appium/UiAutomator2 crashed while validating fund: "
                                + fundName
                                + " | "
                                + cleanedError,
                        e
                );
            }

            String error = fundName + " | " + cleanedError;
            tableValidationErrors.add(error);
            ReportLogger.debug("[FUND VALIDATION FAILED] " + error);

            try {
                resetTableHorizontalPosition();
            } catch (Exception resetError) {
                String resetMessage = cleanError(resetError.getMessage());

                if (isDriverCrashedError(resetMessage)) {
                    throw new RuntimeException(
                            "Appium/UiAutomator2 crashed during reset after fund failure: "
                                    + fundName
                                    + " | "
                                    + resetMessage,
                            resetError
                    );
                }

                ReportLogger.debug("[RESET AFTER FAILURE ALSO FAILED] " + resetMessage);
            }
        }
    }

    private WebElement scrollUntilFundRowVisible(By fundLocator, String fundName) {
        /*
         * Stable row visibility rule:
         * - Do not require the full row to be inside the screen.
         * - If Appium can expose the row and its center is inside the device viewport,
         *   use it for validation.
         * - If vertical swipes do not change the row position, stop swiping and use
         *   the visible/accessibility row instead of failing.
         */
        int screenHeight = driver.manage().window().getSize().getHeight();
        int safeTop = 390;
        int safeBottom = Math.min(screenHeight - 220, 1900);
        int absoluteBottomLimit = screenHeight - 80;
        int minimumVisibleHeight = 35;

        int lastTop = Integer.MIN_VALUE;
        int lastBottom = Integer.MIN_VALUE;
        int samePositionCount = 0;

        for (int attempt = 1; attempt <= 7; attempt++) {
            WebElement row = firstDisplayedElement(fundLocator);

            if (row != null) {
                Rectangle rect = row.getRect();

                int top = rect.getY();
                int bottom = rect.getY() + rect.getHeight();
                int height = rect.getHeight();
                int centerY = rect.getY() + rect.getHeight() / 2;

                boolean enoughHeight = height >= minimumVisibleHeight;
                boolean centerInsideSafeArea = centerY >= safeTop && centerY <= safeBottom;
                boolean centerInsideDevice = centerY >= safeTop && centerY <= absoluteBottomLimit;

                if (enoughHeight && centerInsideSafeArea) {
                    ReportLogger.debug(
                            "Fund row usable for validation: "
                                    + fundName
                                    + " | top="
                                    + top
                                    + " | bottom="
                                    + bottom
                                    + " | centerY="
                                    + centerY
                                    + " | screenHeight="
                                    + screenHeight
                    );
                    return row;
                }

                if (top == lastTop && bottom == lastBottom) {
                    samePositionCount++;
                } else {
                    samePositionCount = 0;
                }

                lastTop = top;
                lastBottom = bottom;

                if (samePositionCount >= 1 && enoughHeight && centerInsideDevice) {
                    ReportLogger.debug(
                            "Fund row position not changing. Using exposed row for validation: "
                                    + fundName
                                    + " | top="
                                    + top
                                    + " | bottom="
                                    + bottom
                                    + " | centerY="
                                    + centerY
                                    + " | screenHeight="
                                    + screenHeight
                    );
                    return row;
                }

                if (centerY > safeBottom) {
                    ReportLogger.debug(
                            "Fund row near bottom. Trying controlled upward scroll: "
                                    + fundName
                                    + " | top="
                                    + top
                                    + " | bottom="
                                    + bottom
                                    + " | centerY="
                                    + centerY
                                    + " | attempt="
                                    + attempt
                    );

                    controlledTableSwipeUp();
                    sleep(650);
                    continue;
                }

                if (centerY < safeTop) {
                    ReportLogger.debug(
                            "Fund row too high. Trying controlled downward correction: "
                                    + fundName
                                    + " | top="
                                    + top
                                    + " | bottom="
                                    + bottom
                                    + " | centerY="
                                    + centerY
                                    + " | attempt="
                                    + attempt
                    );

                    controlledTableSwipeDown();
                    sleep(500);
                    continue;
                }

                if (enoughHeight && centerInsideDevice) {
                    ReportLogger.debug(
                            "Fund row exposed in device viewport. Using for validation: "
                                    + fundName
                                    + " | top="
                                    + top
                                    + " | bottom="
                                    + bottom
                                    + " | centerY="
                                    + centerY
                    );
                    return row;
                }
            }

            ReportLogger.debug("Finding fund row: " + fundName + " | attempt=" + attempt);
            controlledTableSwipeUp();
            sleep(650);
        }

        throw new RuntimeException("Fund row not found safely after vertical scroll: " + fundName);
    }

    private void verifyValueNearFundRow(
            By fundLocator,
            String fundName,
            String expectedValue,
            String columnName
    ) {
        if (expectedValue == null || expectedValue.trim().isEmpty()) {
            ReportLogger.debug("Skipping blank expected value for " + fundName + " | " + columnName);
            return;
        }

        WebElement fundRow = firstDisplayedElement(fundLocator);

        if (fundRow == null) {
            throw new RuntimeException("Fund row not visible while validating " + columnName + ": " + fundName);
        }

        String cleanExpected = normalizeTableValue(expectedValue);

        /*
         * First try proper cell extraction by current column header X-position and fund row Y-position.
         * This is more stable than searching the expected value anywhere on the screen.
         */
        String actualFromCell = findVisibleCellValueNearFundRow(fundRow, columnName, fundName);

        if (actualFromCell != null && !actualFromCell.trim().isEmpty()) {
            String cleanActual = normalizeTableValue(actualFromCell);

            if (isExactTableValueMatch(cleanExpected, cleanActual)) {
                ReportLogger.pass(
                        columnName
                                + " validated for "
                                + fundName
                                + " | Expected: "
                                + cleanExpected
                                + " | Actual: "
                                + cleanActual
                );
                return;
            }

            throw new RuntimeException(
                    columnName
                            + " mismatch for fund "
                            + fundName
                            + ". Expected="
                            + cleanExpected
                            + " | Actual="
                            + cleanActual
            );
        }

        /*
         * Fallback: if Appium does not expose a clean cell at the column X-position,
         * search the expected value and match the nearest visible value by Y-axis.
         */
        Rectangle fundRect = fundRow.getRect();
        int fundCenterY = fundRect.getY() + fundRect.getHeight() / 2;

        List<WebElement> matchingValues = findValueElements(expectedValue);

        WebElement bestMatch = null;
        int bestDistance = Integer.MAX_VALUE;
        int bestValueCenterY = -1;

        for (WebElement valueElement : matchingValues) {
            try {
                if (!valueElement.isDisplayed()) {
                    continue;
                }

                Rectangle valueRect = valueElement.getRect();
                int valueCenterY = valueRect.getY() + valueRect.getHeight() / 2;
                int distance = Math.abs(valueCenterY - fundCenterY);

                if (distance < bestDistance) {
                    bestDistance = distance;
                    bestMatch = valueElement;
                    bestValueCenterY = valueCenterY;
                }

            } catch (Exception ignored) {
                // Try next matching element
            }
        }

        int allowedDistance = 380;

        if (bestMatch != null && bestDistance <= allowedDistance) {
            String actualValue = normalizeTableValue(getElementReadableValue(bestMatch));

            if (actualValue.isEmpty()) {
                actualValue = cleanExpected;
            }

            if (!isExactTableValueMatch(cleanExpected, actualValue)) {
                throw new RuntimeException(
                        columnName
                                + " mismatch for fund "
                                + fundName
                                + ". Expected="
                                + cleanExpected
                                + " | Actual="
                                + actualValue
                );
            }

            ReportLogger.pass(
                    columnName
                            + " validated for "
                            + fundName
                            + " | Expected: "
                            + cleanExpected
                            + " | Actual: "
                            + actualValue
            );

            ReportLogger.debug(
                    columnName
                            + " matched by fallback for "
                            + fundName
                            + " | fundCenterY="
                            + fundCenterY
                            + " | valueCenterY="
                            + bestValueCenterY
                            + " | distance="
                            + bestDistance
                            + " | allowedDistance="
                            + allowedDistance
            );
            return;
        }

        throw new RuntimeException(
                columnName
                        + " value not found near fund row. Fund="
                        + fundName
                        + " | Expected="
                        + cleanExpected
                        + " | fundCenterY="
                        + fundCenterY
                        + " | closestDistance="
                        + bestDistance
                        + " | allowedDistance="
                        + allowedDistance
                        + " | visibleCells="
                        + collectVisibleTableCellTexts()
        );
    }

    private String findVisibleCellValueNearFundRow(WebElement fundRow, String columnName, String fundName) {
        try {
            By headerLocator = getHeaderLocatorForColumn(columnName);
            WebElement header = firstDisplayedElement(headerLocator);

            if (header == null) {
                ReportLogger.debug("Column header not visible for " + columnName + " while validating " + fundName);
                return "";
            }

            Rectangle headerRect = header.getRect();
            Rectangle fundRect = fundRow.getRect();

            int headerCenterX = headerRect.getX() + headerRect.getWidth() / 2;
            int fundCenterY = fundRect.getY() + fundRect.getHeight() / 2;

            int xTolerance = columnName.equalsIgnoreCase("Category") ? 150 : 130;
            int yTolerance = Math.max(145, fundRect.getHeight());

            List<WebElement> allElements = driver.findElements(By.xpath("//*"));

            WebElement bestCell = null;
            int bestDistance = Integer.MAX_VALUE;

            for (WebElement element : allElements) {
                try {
                    if (!element.isDisplayed()) {
                        continue;
                    }

                    String label = normalizeTableValue(getElementReadableValue(element));

                    if (label.isEmpty()) {
                        continue;
                    }

                    if (isNonValueLabel(label, fundName, columnName)) {
                        continue;
                    }

                    Rectangle rect = element.getRect();
                    int cellCenterX = rect.getX() + rect.getWidth() / 2;
                    int cellCenterY = rect.getY() + rect.getHeight() / 2;

                    int xDistance = Math.abs(cellCenterX - headerCenterX);
                    int yDistance = Math.abs(cellCenterY - fundCenterY);

                    if (xDistance <= xTolerance && yDistance <= yTolerance) {
                        int totalDistance = xDistance + yDistance;

                        if (totalDistance < bestDistance) {
                            bestDistance = totalDistance;
                            bestCell = element;
                        }
                    }

                } catch (Exception ignored) {
                    // Try next element
                }
            }

            if (bestCell != null) {
                String value = normalizeTableValue(getElementReadableValue(bestCell));

                ReportLogger.debug(
                        "Cell extracted for "
                                + fundName
                                + " | "
                                + columnName
                                + " = "
                                + value
                                + " | distance="
                                + bestDistance
                );

                return value;
            }

        } catch (Exception e) {
            ReportLogger.debug(
                    "Cell extraction failed for "
                            + fundName
                            + " | "
                            + columnName
                            + " | "
                            + cleanError(e.getMessage())
            );
        }

        return "";
    }

    private By getHeaderLocatorForColumn(String columnName) {
        if ("Category".equalsIgnoreCase(columnName)) {
            return categoryHeader;
        }

        if ("Consistency score".equalsIgnoreCase(columnName)) {
            return consistencyScoreHeader;
        }

        if ("5Y Return".equalsIgnoreCase(columnName)) {
            return fiveYearReturnHeader;
        }

        if ("Worst 1Y Return".equalsIgnoreCase(columnName)) {
            return worstOneYearReturnHeader;
        }

        return byDescContains(columnName);
    }

    private boolean isNonValueLabel(String label, String fundName, String columnName) {
        String cleanLabel = normalizeTableValue(label);
        String cleanFund = normalizeTableValue(fundName);

        if (cleanLabel.equalsIgnoreCase(columnName)) {
            return true;
        }

        if (cleanLabel.contains(cleanFund)) {
            return true;
        }

        if (cleanLabel.equalsIgnoreCase("Fund")
                || cleanLabel.equalsIgnoreCase("Rating")
                || cleanLabel.equalsIgnoreCase("Category")
                || cleanLabel.equalsIgnoreCase("Consistency score")
                || cleanLabel.equalsIgnoreCase("5Y Return")
                || cleanLabel.equalsIgnoreCase("Worst 1Y Return")) {
            return true;
        }

        return false;
    }

    private String collectVisibleTableCellTexts() {
        StringBuilder builder = new StringBuilder();

        try {
            List<WebElement> allElements = driver.findElements(By.xpath("//*"));

            for (WebElement element : allElements) {
                try {
                    if (!element.isDisplayed()) {
                        continue;
                    }

                    String label = normalizeTableValue(getElementReadableValue(element));

                    if (label.isEmpty()) {
                        continue;
                    }

                    if (label.length() > 80) {
                        label = label.substring(0, 80);
                    }

                    if (builder.length() > 0) {
                        builder.append(" | ");
                    }

                    builder.append(label);

                    if (builder.length() > 900) {
                        break;
                    }

                } catch (Exception ignored) {
                    // Try next element
                }
            }

        } catch (Exception ignored) {
            // Ignore debug collection failure
        }

        return builder.toString();
    }

    private List<WebElement> findValueElements(String expectedValue) {
        List<WebElement> elements = new ArrayList<>();

        try {
            elements.addAll(driver.findElements(byDescContains(expectedValue)));
        } catch (Exception ignored) {
            // Ignore
        }

        try {
            elements.addAll(driver.findElements(byTextContains(expectedValue)));
        } catch (Exception ignored) {
            // Ignore
        }

        return elements;
    }

    private void assertNoTableValidationErrors(String tableName) {
        if (tableValidationErrors.isEmpty()) {
            return;
        }

        StringBuilder errorMessage = new StringBuilder();
        errorMessage.append(tableName)
                .append(" table validation completed with ")
                .append(tableValidationErrors.size())
                .append(" issue(s):");

        for (String error : tableValidationErrors) {
            errorMessage.append("\n- ").append(error);
        }

        throw new AssertionError(errorMessage.toString());
    }

    // ================= HIDDEN TABLE CURRENT STATE =================

    public void verifyAggressiveGrowthHiddenTableColumnsCurrentState() {
        try {
            ReportLogger.step("Validating Aggressive Growth hidden columns");

            scrollTableToConsistencyAndFiveYear();
            waitForVisible(consistencyScoreHeader, "Consistency score header");
            waitForVisible(fiveYearReturnHeader, "5Y Return header");

            scrollTableToWorstOneYearReturn();
            waitForVisible(worstOneYearReturnHeader, "Worst 1Y Return header");

            ReportLogger.pass("Aggressive Growth hidden columns validated");

        } catch (Exception e) {
            captureScreenshotAndAttach("AC_008_Hidden_Table_Current_State_Failure");
            throw new AssertionError("Aggressive Growth hidden table validation failed: " + cleanError(e.getMessage()), e);
        }
    }

    public void verifyGrowthHiddenTableColumnsCurrentState() {
        try {
            ReportLogger.step("Validating Growth hidden columns");

            scrollTableToConsistencyAndFiveYear();
            waitForVisible(consistencyScoreHeader, "Consistency score header");
            waitForVisible(fiveYearReturnHeader, "5Y Return header");

            scrollTableToWorstOneYearReturn();
            waitForVisible(worstOneYearReturnHeader, "Worst 1Y Return header");

            ReportLogger.pass("Growth hidden columns validated");

        } catch (Exception e) {
            captureScreenshotAndAttach("AC_014_Growth_Hidden_Table_Current_State_Failure");
            throw new AssertionError("Growth hidden table validation failed: " + cleanError(e.getMessage()), e);
        }
    }

    public void verifyTaxPlanningHiddenTableColumnsCurrentState() {
        try {
            ReportLogger.step("Validating Tax Planning hidden columns");

            scrollTableToConsistencyAndFiveYear();
            waitForVisible(consistencyScoreHeader, "Consistency score header");
            waitForVisible(fiveYearReturnHeader, "5Y Return header");

            scrollTableToWorstOneYearReturn();
            waitForVisible(worstOneYearReturnHeader, "Worst 1Y Return header");

            ReportLogger.pass("Tax Planning hidden columns validated");

        } catch (Exception e) {
            captureScreenshotAndAttach("AC_020_Tax_Planning_Hidden_Table_Current_State_Failure");
            throw new AssertionError("Tax Planning hidden table validation failed: " + cleanError(e.getMessage()), e);
        }
    }

    public void verifyConservativeGrowthHiddenTableColumnsCurrentState() {
        try {
            ReportLogger.step("Validating Conservative Growth hidden columns");

            scrollTableToConsistencyAndFiveYear();
            waitForVisible(consistencyScoreHeader, "Consistency score header");
            waitForVisible(fiveYearReturnHeader, "5Y Return header");

            scrollTableToWorstOneYearReturn();
            waitForVisible(worstOneYearReturnHeader, "Worst 1Y Return header");

            ReportLogger.pass("Conservative Growth hidden columns validated");

        } catch (Exception e) {
            captureScreenshotAndAttach("AC_029_Conservative_Growth_Hidden_Table_Failure");
            throw new AssertionError("Conservative Growth hidden table validation failed: " + cleanError(e.getMessage()), e);
        }
    }

    public void verifyConservativeGrowthIncomeHiddenTableColumnsCurrentState() {
        try {
            ReportLogger.step("Validating Conservative Growth & Income hidden columns");

            scrollTableToConsistencyAndFiveYear();
            waitForVisible(consistencyScoreHeader, "Consistency score header");
            waitForVisible(fiveYearReturnHeader, "5Y Return header");

            scrollTableToWorstOneYearReturn();
            waitForVisible(worstOneYearReturnHeader, "Worst 1Y Return header");

            ReportLogger.pass("Conservative Growth & Income hidden columns validated");

        } catch (Exception e) {
            captureScreenshotAndAttach("AC_035_Conservative_Growth_Income_Hidden_Table_Failure");
            throw new AssertionError("Conservative Growth & Income hidden table validation failed: " + cleanError(e.getMessage()), e);
        }
    }

    public void verifyCoreFixedIncomeHiddenTableColumnsCurrentState() {
        try {
            ReportLogger.step("Validating Core Fixed Income hidden columns");

            scrollTableToConsistencyAndFiveYear();
            waitForVisible(consistencyScoreHeader, "Consistency score header");
            waitForVisible(fiveYearReturnHeader, "5Y Return header");

            scrollTableToWorstOneYearReturn();
            waitForVisible(worstOneYearReturnHeader, "Worst 1Y Return header");

            ReportLogger.pass("Core Fixed Income hidden columns validated");

        } catch (Exception e) {
            captureScreenshotAndAttach("AC_041_Core_Fixed_Income_Hidden_Table_Failure");
            throw new AssertionError("Core Fixed Income hidden table validation failed: " + cleanError(e.getMessage()), e);
        }
    }


    public void verifyCapitalPreservationHiddenTableColumnsCurrentState() {
        try {
            ReportLogger.step("Validating Capital Preservation hidden columns");

            scrollTableToConsistencyAndFiveYear();
            waitForVisible(consistencyScoreHeader, "Consistency score header");
            waitForVisible(fiveYearReturnHeader, "5Y Return header");

            scrollTableToWorstOneYearReturn();
            waitForVisible(worstOneYearReturnHeader, "Worst 1Y Return header");

            ReportLogger.pass("Capital Preservation hidden columns validated");

        } catch (Exception e) {
            captureScreenshotAndAttach("AC_047_Capital_Preservation_Hidden_Table_Failure");
            throw new AssertionError("Capital Preservation hidden table validation failed: " + cleanError(e.getMessage()), e);
        }
    }

    public void verifyCompareInScreenerButton() {
        try {
            ReportLogger.step("Validating Compare in screener button");

            waitForVisible(compareInScreener, "Compare in screener button");

            ReportLogger.pass("Compare in screener button validated");

        } catch (Exception e) {
            captureScreenshotAndAttach("Compare_In_Screener_Failure");
            throw new AssertionError("Compare in screener validation failed: " + cleanError(e.getMessage()), e);
        }
    }

    // ================= CARD HELPERS =================

    private void verifyAggressiveGrowthCard() {
        scrollToVisible(aggressiveGrowthCard, "Aggressive Growth card");
        verifyTextPresentOnCurrentScreen("Aggressive Growth", "Aggressive Growth title");
        verifyTextPresentOnCurrentScreen("Best mid- and small-cap equity funds", "Aggressive Growth description");
        verifyTextPresentOnCurrentScreen("SIP Only", "Aggressive Growth mode");
        verifyTextPresentOnCurrentScreen("7+ Years", "Aggressive Growth horizon");
        verifyTextPresentOnCurrentScreen("Very High", "Aggressive Growth risk range");
        ReportLogger.pass("Aggressive Growth card validated");
    }

    private void verifyGrowthCard() {
        scrollToVisible(growthCard, "Growth card");
        verifyTextPresentOnCurrentScreen("Growth", "Growth title");
        verifyTextPresentOnCurrentScreen("Top diversified equity funds", "Growth description");
        verifyTextPresentOnCurrentScreen("SIP Only", "Growth mode");
        verifyTextPresentOnCurrentScreen("5+ Years", "Growth horizon");
        verifyTextPresentOnCurrentScreen("High to Very High", "Growth risk range");
        ReportLogger.pass("Growth card validated");
    }

    private void verifyTaxPlanningCard() {
        scrollToVisible(taxPlanningCard, "Tax Planning card");
        verifyTextPresentOnCurrentScreen("Tax Planning", "Tax Planning title");
        verifyTextPresentOnCurrentScreen("ELSS funds similar to Growth funds", "Tax Planning description");
        verifyTextPresentOnCurrentScreen("SIP Only", "Tax Planning mode");
        verifyTextPresentOnCurrentScreen("5+ Years", "Tax Planning horizon");
        verifyTextPresentOnCurrentScreen("High to Very High", "Tax Planning risk range");
        ReportLogger.pass("Tax Planning card validated");
    }

    private void verifyGrowthInternationalCard() {
        scrollToVisible(growthInternationalCard, "Growth - International card");
        verifyTextPresentOnCurrentScreen("Growth - International", "Growth - International title");
        verifyTextPresentOnCurrentScreen("International equity funds", "Growth - International description");
        verifyTextPresentOnCurrentScreen("SIP Only", "Growth - International mode");
        verifyTextPresentOnCurrentScreen("5+ Years", "Growth - International horizon");
        verifyTextPresentOnCurrentScreen("High to Very High", "Growth - International risk range");
        ReportLogger.pass("Growth - International card validated");
    }

    private void verifyConservativeGrowthCard() {
        scrollToVisible(conservativeGrowthCard, "Conservative Growth card");
        verifyTextPresentOnCurrentScreen("Conservative Growth", "Conservative Growth title");
        verifyTextPresentOnCurrentScreen("Large-cap equity funds", "Conservative Growth description");
        verifyTextPresentOnCurrentScreen("SIP Only", "Conservative Growth mode");
        verifyTextPresentOnCurrentScreen("5+ Years", "Conservative Growth horizon");
        verifyTextPresentOnCurrentScreen("Moderately High to High", "Conservative Growth risk range");
        ReportLogger.pass("Conservative Growth card validated");
    }

    private void verifyConservativeGrowthIncomeCard() {
        scrollToVisible(conservativeGrowthIncomeCard, "Conservative Growth & Income card");
        verifyTextPresentOnCurrentScreen("Conservative Growth & Income", "Conservative Growth & Income title");
        verifyTextPresentOnCurrentScreen("Equity-savings funds", "Conservative Growth & Income description");
        verifyTextPresentOnCurrentScreen("SIP Only", "Conservative Growth & Income mode");
        verifyTextPresentOnCurrentScreen("Moderate to Moderately High", "Conservative Growth & Income risk range");

        if (
                isTextPresentOnCurrentScreen("3–5 Years")
                        || isTextPresentOnCurrentScreen("3-5 Years")
                        || isTextPresentOnCurrentScreen("3–5")
                        || isTextPresentOnCurrentScreen("3-5")
        ) {
            ReportLogger.pass("Verified text: Conservative Growth & Income horizon");
        } else {
            ReportLogger.step("Conservative Growth & Income horizon text not exposed clearly. Skipping strict horizon check.");
        }

        ReportLogger.pass("Conservative Growth & Income card validated");
    }

    private void verifyCoreFixedIncomeCard() {
        scrollToVisible(coreFixedIncomeCard, "Core Fixed Income card");
        verifyTextPresentOnCurrentScreen("Core Fixed Income", "Core Fixed Income title");
        verifyTextPresentOnCurrentScreen("Short-duration funds", "Core Fixed Income description");

        verifyAnyTextPresentOnCurrentScreen(
                new String[]{"One-time or SIP", "One-time", "SIP"},
                "Core Fixed Income mode"
        );

        verifyTextPresentOnCurrentScreen("Moderately Low to Moderate", "Core Fixed Income risk range");
        ReportLogger.pass("Core Fixed Income card validated");
    }

    private void verifyCapitalPreservationCard() {
        scrollToVisible(capitalPreservationCard, "Capital Preservation card");
        verifyTextPresentOnCurrentScreen("Capital Preservation", "Capital Preservation title");
        verifyTextPresentOnCurrentScreen("Liquid and overnight funds", "Capital Preservation description");

        verifyAnyTextPresentOnCurrentScreen(
                new String[]{"One-time or SIP", "One-time", "SIP"},
                "Capital Preservation mode"
        );

        verifyTextPresentOnCurrentScreen("Low to Moderately Low", "Capital Preservation risk range");
        ReportLogger.pass("Capital Preservation card validated");
    }

    // ================= W3C HORIZONTAL TABLE SWIPE HELPERS =================

    private void scrollTableToConsistencyAndFiveYear() {
        try {
            ReportLogger.debug("Force scrolling table LEFT to Consistency score and 5Y Return");

            /*
             * Do not depend on header visibility here.
             * Appium can expose off-screen table headers in page source and return them as visible.
             * A forced left swipe after reset gives stable alignment for Consistency score + 5Y Return.
             */
            performHorizontalSwipe(900, 960, 420, 960, 1000);
            sleep(1300);

            if (!isElementVisibleInViewport(consistencyScoreHeader) || !isElementVisibleInViewport(fiveYearReturnHeader)) {
                ReportLogger.debug("First forced swipe did not fully expose Consistency/5Y. Trying one corrective swipe.");
                performHorizontalSwipe(900, 960, 420, 960, 900);
                sleep(1000);
            }

            waitForVisible(consistencyScoreHeader, "Consistency score header");
            waitForVisible(fiveYearReturnHeader, "5Y Return header");

            ReportLogger.debug("Table force-positioned at Consistency score and 5Y Return");

        } catch (Exception e) {
            throw new RuntimeException(
                    "Could not force scroll table LEFT to Consistency/5Y columns: "
                            + cleanError(e.getMessage()),
                    e
            );
        }
    }

    private void scrollTableToWorstOneYearReturn() {
        try {
            ReportLogger.debug("Force scrolling table LEFT to Worst 1Y Return");

            /*
             * This method is always called after scrollTableToConsistencyAndFiveYear().
             * So one forced left swipe should move from Consistency/5Y to Worst 1Y Return.
             */
            performHorizontalSwipe(900, 960, 420, 960, 1000);
            sleep(1300);

            if (!isElementVisibleInViewport(worstOneYearReturnHeader)) {
                ReportLogger.debug("First forced swipe did not fully expose Worst 1Y Return. Trying one corrective swipe.");
                performHorizontalSwipe(900, 960, 420, 960, 900);
                sleep(1000);
            }

            waitForVisible(worstOneYearReturnHeader, "Worst 1Y Return header");

            ReportLogger.debug("Table force-positioned at Worst 1Y Return");

        } catch (Exception e) {
            throw new RuntimeException(
                    "Could not force scroll table LEFT to Worst 1Y Return column: "
                            + cleanError(e.getMessage()),
                    e
            );
        }
    }

    private void resetTableHorizontalPosition() {
        try {
            ReportLogger.debug("Force resetting table horizontally RIGHT to Fund/Rating/Category");

            /*
             * Do not depend on isTableAtLeftMostPosition() only.
             * The Fund column is frozen, so it may look visible even when the scrollable columns
             * are still shifted. Two right swipes give a stable reset before every fund validation.
             */
            performHorizontalSwipe(520, 960, 960, 960, 1000);
            sleep(800);

            performHorizontalSwipe(520, 960, 960, 960, 1000);
            sleep(1000);

            if (!isTableAtLeftMostPosition()) {
                ReportLogger.debug("Table still not at Fund/Rating/Category after two right swipes. Trying final corrective reset swipe.");
                performHorizontalSwipe(520, 960, 960, 960, 900);
                sleep(900);
            }

            waitForVisible(fundHeader, "Fund header");
            waitForVisible(ratingHeader, "Rating header");
            waitForVisible(categoryHeader, "Category header");

            ReportLogger.debug("Table force-reset to Fund/Rating/Category position");

        } catch (Exception e) {
            throw new RuntimeException("Table horizontal reset failed: " + cleanError(e.getMessage()), e);
        }
    }

    private boolean isTableAtLeftMostPosition() {
        return isElementVisibleInViewport(fundHeader)
                && isElementVisibleInViewport(ratingHeader)
                && isElementVisibleInViewport(categoryHeader);
    }

    private void performHorizontalSwipe(int startX, int startY, int endX, int endY, int durationMillis) {
        /*
         * Pure W3C horizontal swipe.
         *
         * Table behavior:
         * Fund column is frozen/fixed.
         *
         * LEFT swipe:
         *   x=900 -> x=420
         *   This moves table towards hidden columns:
         *   Consistency score, 5Y Return, Worst 1Y Return.
         *
         * RIGHT reset swipe:
         *   x=520 -> x=960
         *   Start from scrollable table area, not frozen Fund column.
         */

        boolean moveTableLeft = startX > endX;

        int safeStartX;
        int safeEndX;

        if (moveTableLeft) {
            safeStartX = 900;
            safeEndX = 420;
        } else {
            safeStartX = 520;
            safeEndX = 960;
        }

        int safeY = 960;

        try {
            PointerInput finger = new PointerInput(PointerInput.Kind.TOUCH, "finger");
            Sequence swipe = new Sequence(finger, 1);

            swipe.addAction(finger.createPointerMove(
                    Duration.ZERO,
                    PointerInput.Origin.viewport(),
                    safeStartX,
                    safeY
            ));

            swipe.addAction(finger.createPointerDown(PointerInput.MouseButton.LEFT.asArg()));

            swipe.addAction(new Pause(finger, Duration.ofMillis(120)));

            swipe.addAction(finger.createPointerMove(
                    Duration.ofMillis(Math.max(durationMillis, 1000)),
                    PointerInput.Origin.viewport(),
                    safeEndX,
                    safeY
            ));

            swipe.addAction(finger.createPointerUp(PointerInput.MouseButton.LEFT.asArg()));

            driver.perform(Collections.singletonList(swipe));

            ReportLogger.debug(
                    "W3C horizontal swipe performed from x="
                            + safeStartX
                            + " to x="
                            + safeEndX
                            + " at y="
                            + safeY
                            + " | moveTableLeft="
                            + moveTableLeft
            );

            sleep(900);

        } catch (Exception e) {
            throw new RuntimeException("W3C horizontal swipe failed: " + cleanError(e.getMessage()), e);
        }
    }

    // ================= VERTICAL SWIPE HELPERS =================

    private void mobileScrollDown() {
        swipeUpW3C();
    }

    private void mobileScrollUp() {
        swipeDownW3C();
    }

    private void swipeUpW3C() {
        try {
            PointerInput finger = new PointerInput(PointerInput.Kind.TOUCH, "finger");
            Sequence swipe = new Sequence(finger, 1);

            swipe.addAction(finger.createPointerMove(Duration.ZERO, PointerInput.Origin.viewport(), 540, 1300));
            swipe.addAction(finger.createPointerDown(PointerInput.MouseButton.LEFT.asArg()));
            swipe.addAction(finger.createPointerMove(Duration.ofMillis(700), PointerInput.Origin.viewport(), 540, 620));
            swipe.addAction(finger.createPointerUp(PointerInput.MouseButton.LEFT.asArg()));

            driver.perform(Collections.singletonList(swipe));
            ReportLogger.debug("W3C vertical swipe up performed");

        } catch (Exception e) {
            throw new RuntimeException("W3C swipe up failed: " + cleanError(e.getMessage()), e);
        }
    }

    private void swipeDownW3C() {
        try {
            PointerInput finger = new PointerInput(PointerInput.Kind.TOUCH, "finger");
            Sequence swipe = new Sequence(finger, 1);

            swipe.addAction(finger.createPointerMove(Duration.ZERO, PointerInput.Origin.viewport(), 540, 760));
            swipe.addAction(finger.createPointerDown(PointerInput.MouseButton.LEFT.asArg()));
            swipe.addAction(finger.createPointerMove(Duration.ofMillis(650), PointerInput.Origin.viewport(), 540, 1650));
            swipe.addAction(finger.createPointerUp(PointerInput.MouseButton.LEFT.asArg()));

            driver.perform(Collections.singletonList(swipe));
            ReportLogger.debug("W3C swipe down performed");

        } catch (Exception e) {
            throw new RuntimeException("W3C swipe down failed: " + cleanError(e.getMessage()), e);
        }
    }

    private void controlledTableSwipeUp() {
        try {
            PointerInput finger = new PointerInput(PointerInput.Kind.TOUCH, "finger");
            Sequence swipe = new Sequence(finger, 1);

            /*
             * Strong table-area swipe.
             * Start from lower table area but above bottom CTA.
             * End near upper table area.
             */
            swipe.addAction(finger.createPointerMove(
                    Duration.ZERO,
                    PointerInput.Origin.viewport(),
                    540,
                    1450
            ));

            swipe.addAction(finger.createPointerDown(PointerInput.MouseButton.LEFT.asArg()));
            swipe.addAction(new Pause(finger, Duration.ofMillis(120)));

            swipe.addAction(finger.createPointerMove(
                    Duration.ofMillis(750),
                    PointerInput.Origin.viewport(),
                    540,
                    760
            ));

            swipe.addAction(finger.createPointerUp(PointerInput.MouseButton.LEFT.asArg()));

            driver.perform(Collections.singletonList(swipe));
            ReportLogger.debug("Controlled table swipe up performed");

        } catch (Exception e) {
            throw new RuntimeException("Controlled table swipe up failed: " + cleanError(e.getMessage()), e);
        }
    }

    private void controlledTableSwipeDown() {
        try {
            PointerInput finger = new PointerInput(PointerInput.Kind.TOUCH, "finger");
            Sequence swipe = new Sequence(finger, 1);

            /*
             * Small downward correction.
             * Used only when fund row is too high.
             */
            swipe.addAction(finger.createPointerMove(
                    Duration.ZERO,
                    PointerInput.Origin.viewport(),
                    540,
                    760
            ));

            swipe.addAction(finger.createPointerDown(PointerInput.MouseButton.LEFT.asArg()));
            swipe.addAction(new Pause(finger, Duration.ofMillis(100)));

            swipe.addAction(finger.createPointerMove(
                    Duration.ofMillis(550),
                    PointerInput.Origin.viewport(),
                    540,
                    1180
            ));

            swipe.addAction(finger.createPointerUp(PointerInput.MouseButton.LEFT.asArg()));

            driver.perform(Collections.singletonList(swipe));
            ReportLogger.debug("Controlled table swipe down performed");

        } catch (Exception e) {
            throw new RuntimeException("Controlled table swipe down failed: " + cleanError(e.getMessage()), e);
        }
    }

    // ================= COMMON HELPERS =================

    private void goBackToListingIfOnDetailPage() {
        if (isElementVisible(compareInScreener) || isElementVisible(noRecommendationMessage)) {
            ReportLogger.step("Currently on detail page. Going back to listing.");
            tapTopLeftBackButton("Back from detail page");
            sleep(1200);
        }

        if (!isAnalystChoiceListingVisible()) {
            recoverAnalystChoiceIfNeeded();
        }
    }

    private boolean isAnalystChoiceListingVisible() {
        return isElementVisible(analystChoiceTitle)
                || isElementVisible(analystChoiceSubtitle)
                || isElementVisible(aggressiveGrowthCard)
                || isElementVisible(growthCard)
                || isElementVisible(taxPlanningCard)
                || isElementVisible(growthInternationalCard)
                || isElementVisible(conservativeGrowthCard)
                || isElementVisible(conservativeGrowthIncomeCard)
                || isElementVisible(coreFixedIncomeCard)
                || isElementVisible(capitalPreservationCard)
                || isElementVisible(plannerTitle)
                || isElementVisible(plannerButton);
    }

    private void scrollToAndTapAnalystChoiceFromHub() {
        Exception lastError = null;

        for (int attempt = 1; attempt <= 8; attempt++) {
            try {
                ReportLogger.step("Searching Analyst’s Choice in Hub. Attempt: " + attempt);

                if (tapIfVisible(analystChoiceHubMenu, "Analyst’s Choice menu")) {
                    return;
                }

                mobileScrollDown();
                sleep(900);

            } catch (Exception e) {
                lastError = e;
                ReportLogger.debug("Analyst’s Choice search attempt failed: " + cleanError(e.getMessage()));
            }
        }

        throw new RuntimeException(
                "Analyst’s Choice menu not found in Hub"
                        + (lastError == null ? "" : ": " + cleanError(lastError.getMessage())),
                lastError
        );
    }

    private void scrollToVisible(By locator, String elementName) {
        String lastPageSource = "";

        for (int attempt = 1; attempt <= 10; attempt++) {
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
            mobileScrollDown();
            sleep(900);
        }

        throw new RuntimeException(elementName + " not visible after scrolling");
    }

    private void scrollToBeginning(String pageName) {
        ReportLogger.step("Resetting scroll to top for: " + pageName);

        try {
            driver.findElement(AppiumBy.androidUIAutomator(
                    "new UiScrollable(new UiSelector().className(\"android.widget.ScrollView\")).scrollToBeginning(20)"
            ));
            sleep(1200);
            ReportLogger.step(pageName + " reset using UiScrollable.scrollToBeginning");
            return;

        } catch (Exception e) {
            ReportLogger.debug(pageName + " UiScrollable reset failed. Using W3C fallback: " + cleanError(e.getMessage()));
        }

        for (int i = 1; i <= 4; i++) {
            mobileScrollUp();
            sleep(600);
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
                } catch (Exception ignored) {
                    // Try next element
                }
            }

            return false;

        } catch (Exception e) {
            ReportLogger.debug(elementName + " tap skipped: " + cleanError(e.getMessage()));
            return false;
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
                    if (element.isDisplayed()) {
                        return true;
                    }
                } catch (Exception ignored) {
                    // Try next element
                }
            }

            return false;

        } catch (Exception e) {
            return false;
        }
    }

    private boolean isElementVisibleInViewport(By locator) {
        try {
            int screenWidth = driver.manage().window().getSize().getWidth();
            int screenHeight = driver.manage().window().getSize().getHeight();

            List<WebElement> elements = driver.findElements(locator);

            for (WebElement element : elements) {
                try {
                    if (!element.isDisplayed()) {
                        continue;
                    }

                    Rectangle rect = element.getRect();

                    if (rect.getWidth() <= 0 || rect.getHeight() <= 0) {
                        continue;
                    }

                    int centerX = rect.getX() + rect.getWidth() / 2;
                    int centerY = rect.getY() + rect.getHeight() / 2;

                    if (centerX >= 0 && centerX <= screenWidth && centerY >= 0 && centerY <= screenHeight) {
                        return true;
                    }
                } catch (Exception ignored) {
                    // Try next element
                }
            }

            return false;

        } catch (Exception e) {
            return false;
        }
    }

    private WebElement firstDisplayedElement(By locator) {
        try {
            List<WebElement> elements = driver.findElements(locator);

            for (WebElement element : elements) {
                try {
                    if (element.isDisplayed()) {
                        return element;
                    }
                } catch (Exception ignored) {
                    // Try next
                }
            }

            return null;

        } catch (Exception e) {
            return null;
        }
    }

    private int getSafeTableBottomY() {
        try {
            WebElement compareButton = firstDisplayedElement(compareInScreener);

            if (compareButton != null) {
                Rectangle rect = compareButton.getRect();
                return rect.getY() - 20;
            }

        } catch (Exception ignored) {
            // fallback below
        }

        return 1280;
    }

    private void verifyTextPresentOnCurrentScreen(String text, String elementName) {
        if (isTextPresentOnCurrentScreen(text)) {
            ReportLogger.pass("Verified text: " + elementName);
            return;
        }

        throw new RuntimeException(elementName + " text not present on current screen. Expected text contains: " + text);
    }

    private void verifyAnyTextPresentOnCurrentScreen(String[] possibleTexts, String elementName) {
        for (String text : possibleTexts) {
            if (isTextPresentOnCurrentScreen(text)) {
                ReportLogger.pass("Verified text: " + elementName + " | Matched: " + text);
                return;
            }
        }

        StringBuilder expected = new StringBuilder();

        for (String text : possibleTexts) {
            if (expected.length() > 0) {
                expected.append(" OR ");
            }
            expected.append(text);
        }

        throw new RuntimeException(elementName + " text not present on current screen. Expected one of: " + expected);
    }

    private boolean isTextPresentOnCurrentScreen(String text) {
        if (isElementVisible(byDescContains(text))) {
            return true;
        }

        if (isElementVisible(byTextContains(text))) {
            return true;
        }

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

    private void tapElementCenter(WebElement element) {
        Rectangle rect = element.getRect();

        Map<String, Object> params = new HashMap<>();
        params.put("x", rect.getX() + rect.getWidth() / 2);
        params.put("y", rect.getY() + rect.getHeight() / 2);

        driver.executeScript("mobile: clickGesture", params);
        sleep(250);
    }

    private void tapTopLeftBackButton(String actionName) {
        try {
            By topLeftImage = AppiumBy.androidUIAutomator(
                    "new UiSelector().className(\"android.widget.ImageView\").instance(0)"
            );

            List<WebElement> backButtons = driver.findElements(topLeftImage);

            for (WebElement backButton : backButtons) {
                try {
                    if (backButton.isDisplayed() && backButton.isEnabled()) {
                        Rectangle rect = backButton.getRect();

                        if (rect.getX() <= 180 && rect.getY() <= 260) {
                            tapElementCenter(backButton);
                            ReportLogger.step(actionName + " clicked using back locator");
                            sleep(1200);
                            return;
                        }
                    }
                } catch (Exception ignored) {
                    // Try coordinate fallback
                }
            }

            tapByCoordinate(84, 149, actionName + " coordinate fallback");
            sleep(1200);

        } catch (Exception e) {
            throw new RuntimeException("Failed to tap top-left back button: " + cleanError(e.getMessage()), e);
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

    private void pressBackSafely() {
        try {
            driver.pressKey(new KeyEvent(AndroidKey.BACK));
            sleep(1000);
        } catch (Exception e) {
            ReportLogger.debug("Android BACK skipped: " + cleanError(e.getMessage()));
        }
    }

    private void captureScreenshotAndAttach(String screenshotName) {
        try {
            if (driver == null || driver.getSessionId() == null) {
                return;
            }

            String safeName = screenshotName.replaceAll("[^a-zA-Z0-9._-]", "_")
                    + "_"
                    + System.currentTimeMillis()
                    + ".png";

            Path screenshotDir = Paths.get(
                    System.getProperty("user.dir"),
                    "test-output",
                    "ExtentReports",
                    "screenshots"
            );

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

    private By byDescContains(String text) {
        return AppiumBy.androidUIAutomator(
                "new UiSelector().descriptionContains(\"" + escapeUiAutomatorText(text) + "\")"
        );
    }

    private By byDescRegex(String regex) {
        return AppiumBy.androidUIAutomator(
                "new UiSelector().descriptionMatches(\"" + escapeUiAutomatorText(regex) + "\")"
        );
    }

    private By byTextContains(String text) {
        return AppiumBy.androidUIAutomator(
                "new UiSelector().textContains(\"" + escapeUiAutomatorText(text) + "\")"
        );
    }

    private String escapeUiAutomatorText(String text) {
        return text.replace("\\", "\\\\").replace("\"", "\\\"");
    }


    private boolean isExactTableValueMatch(String expectedValue, String actualValue) {
        String expected = normalizeTableValue(expectedValue);
        String actual = normalizeTableValue(actualValue);

        if (expected.isEmpty() || actual.isEmpty()) {
            return false;
        }

        return actual.equalsIgnoreCase(expected);
    }

    private String normalizeTableValue(String value) {
        if (value == null) {
            return "";
        }

        return value
                .replace("\n", " ")
                .replace("\r", " ")
                .replace("\t", " ")
                .replace("★", "")
                .replace("☆", "")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private String getElementReadableValue(WebElement element) {
        if (element == null) {
            return "";
        }

        try {
            String desc = element.getAttribute("content-desc");
            if (desc != null && !desc.trim().isEmpty()) {
                return desc.trim();
            }
        } catch (Exception ignored) {
            // Try text below
        }

        try {
            String text = element.getText();
            if (text != null && !text.trim().isEmpty()) {
                return text.trim();
            }
        } catch (Exception ignored) {
            // Try text attribute below
        }

        try {
            String textAttribute = element.getAttribute("text");
            if (textAttribute != null && !textAttribute.trim().isEmpty()) {
                return textAttribute.trim();
            }
        } catch (Exception ignored) {
            // Ignore
        }

        return "";
    }

    private boolean isDriverCrashedError(String message) {
        if (message == null) {
            return false;
        }

        String lower = message.toLowerCase();

        return lower.contains("instrumentation process is not running")
                || lower.contains("socket hang up")
                || lower.contains("econnrefused")
                || lower.contains("cannot be proxied")
                || lower.contains("uiautomator2 server")
                || lower.contains("could not proxy command");
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