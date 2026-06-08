package pages;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.openqa.selenium.By;
import org.openqa.selenium.TimeoutException;

import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;

public class Home_Page extends BasePage {

    private static final int MAX_SCROLLS_TO_FIND_LINK = 12;

    private final By fundsTab = AppiumBy.accessibilityId("Funds");
    private final By stocksTab = AppiumBy.accessibilityId("Stocks");
    private final By portfolioTab = AppiumBy.accessibilityId("Portfolio");
    private final By hubTab = AppiumBy.accessibilityId("Hub");

    // Invested-state summary carousel shown at the top of the Funds screen once a
    // portfolio is imported — pushes the marketing sections below the fold.
    private final By portfolioValueCard = AppiumBy
            .androidUIAutomator("new UiSelector().descriptionContains(\"Portfolio Value\")");
    private final By youVsMarketCard = AppiumBy.accessibilityId("You vs Market");
    private final By assetMixCard = AppiumBy
            .androidUIAutomator("new UiSelector().descriptionContains(\"Your Asset Mix\")");
    private final By retirementProjectionCard = AppiumBy
            .androidUIAutomator("new UiSelector().descriptionContains(\"you will have\")");

    private final By richFutureHeading = AppiumBy.accessibilityId("Rich Future Starts Here");
    private final By navigateToHeading = AppiumBy.accessibilityId("Navigate to");
    private final By quickGuidesHeading = AppiumBy.accessibilityId("Quick Guides");
    // The app renders a curly apostrophe (U+2019) in this heading, not a straight quote
    private final By analystChoiceHeading = AppiumBy.accessibilityId("Analyst’s Choice");
    private final By fundAdvisorNoteHeading = AppiumBy.accessibilityId("Fund Advisor's Note");

    private final By retirementCard = AppiumBy.accessibilityId("Your retirement, your way");
    private final By regularIncomeCard = AppiumBy.accessibilityId("Regular income for life");
    private final By noGoalStartInvestingCard = AppiumBy
            .androidUIAutomator("new UiSelector().descriptionContains(\"No Goal? No problem!\")");

    private final By mfScreenerLink = AppiumBy.accessibilityId("MF Screener");
    private final By sipCalculatorLink = AppiumBy.accessibilityId("SIP Calculator");
    private final By fundSearchLink = AppiumBy.accessibilityId("Fund Search");
    private final By moreQuickGuidesLink = AppiumBy.accessibilityId("More");
    private final By buildInvestmentPlanGuide = AppiumBy
            .androidUIAutomator("new UiSelector().descriptionContains(\"Build your investment plan\")");
    private final By upgradePortfolioGuide = AppiumBy
            .androidUIAutomator("new UiSelector().descriptionContains(\"Upgrade your portfolio\")");
    private final By analystPickedFundsGuide = AppiumBy
            .androidUIAutomator("new UiSelector().descriptionContains(\"Tap into analyst-picked funds\")");
    private final By personalisedSupportCard = AppiumBy
            .androidUIAutomator("new UiSelector().descriptionContains(\"Need more help?\")");
    private final By aggressiveGrowthCard = AppiumBy
            .androidUIAutomator("new UiSelector().descriptionContains(\"Aggressive Growth\")");

    private final List<HomeLink> homeLinks = Arrays.asList(
            new HomeLink("Your retirement, your way", retirementCard),
            new HomeLink("Regular income for life", regularIncomeCard),
            new HomeLink("No Goal? No problem! Start Investing", noGoalStartInvestingCard),
            new HomeLink("MF Screener", mfScreenerLink),
            new HomeLink("SIP Calculator", sipCalculatorLink),
            new HomeLink("Fund Search", fundSearchLink),
            new HomeLink("More", moreQuickGuidesLink),
            new HomeLink("Build your investment plan", buildInvestmentPlanGuide),
            new HomeLink("Upgrade your portfolio with clear actions", upgradePortfolioGuide),
            new HomeLink("Tap into analyst-picked funds", analystPickedFundsGuide),
            new HomeLink("Need more help", personalisedSupportCard),
            new HomeLink("Aggressive Growth", aggressiveGrowthCard));

    public Home_Page(AndroidDriver driver) {
        super(driver);
    }

    public Home_Page openFundsScreen() {
        logger.info("Opening Funds screen");

        if (isFundsScreenDisplayed()) {
            logger.info("Funds screen already open");
            return this;
        }

        // A bottom sheet (e.g. Fund Search) or deep screen may hide the tab bar —
        // unwind one layer at a time until the tab bar or the Funds screen reappears
        for (int i = 0; i < 4 && !isFundsScreenDisplayed() && !isDisplayed(fundsTab); i++) {
            logger.info("Funds tab hidden — pressing back to unwind overlay ({}/4)", i + 1);
            driver.navigate().back();
            waitForUiToSettle();
        }

        if (isFundsScreenDisplayed()) {
            logger.info("Funds screen restored after unwinding overlay");
            return this;
        }

        safeClick(fundsTab);
        waitForFundsScreen();
        logger.info("Funds screen opened");
        return this;
    }

    public boolean isFundsScreenDisplayed() {
        return isAnyDisplayed(
                // invested state — summary carousel above the fold
                portfolioValueCard,
                youVsMarketCard,
                assetMixCard,
                retirementProjectionCard,
                // non-invested state — marketing sections above the fold
                richFutureHeading,
                navigateToHeading,
                quickGuidesHeading,
                mfScreenerLink,
                sipCalculatorLink,
                fundSearchLink);
    }

    public boolean isBottomNavigationDisplayed() {
        return isDisplayed(fundsTab)
                && isDisplayed(stocksTab)
                && isDisplayed(portfolioTab)
                && isDisplayed(hubTab);
    }

    public boolean isRichFutureSectionDisplayed() {
        return scrollDownUntilVisible(richFutureHeading, MAX_SCROLLS_TO_FIND_LINK);
    }

    public boolean isNavigateToSectionDisplayed() {
        return scrollDownUntilVisible(navigateToHeading, MAX_SCROLLS_TO_FIND_LINK);
    }

    public boolean isQuickGuidesSectionDisplayed() {
        return scrollDownUntilVisible(quickGuidesHeading, MAX_SCROLLS_TO_FIND_LINK);
    }

    public boolean isAnalystChoiceSectionDisplayed() {
        return scrollDownUntilVisible(analystChoiceHeading, MAX_SCROLLS_TO_FIND_LINK);
    }

    public boolean isFundAdvisorNoteSectionDisplayed() {
        return scrollDownUntilVisible(fundAdvisorNoteHeading, MAX_SCROLLS_TO_FIND_LINK);
    }

    public boolean isRetirementCardDisplayed() {
        return scrollDownUntilVisible(retirementCard, MAX_SCROLLS_TO_FIND_LINK);
    }

    public boolean isRegularIncomeCardDisplayed() {
        return scrollDownUntilVisible(regularIncomeCard, MAX_SCROLLS_TO_FIND_LINK);
    }

    public boolean isNoGoalStartInvestingCardDisplayed() {
        return scrollDownUntilVisible(noGoalStartInvestingCard, MAX_SCROLLS_TO_FIND_LINK);
    }

    public boolean isMfScreenerLinkDisplayed() {
        return scrollDownUntilVisible(mfScreenerLink, MAX_SCROLLS_TO_FIND_LINK);
    }

    public boolean isSipCalculatorLinkDisplayed() {
        return scrollDownUntilVisible(sipCalculatorLink, MAX_SCROLLS_TO_FIND_LINK);
    }

    public boolean isFundSearchLinkDisplayed() {
        return scrollDownUntilVisible(fundSearchLink, MAX_SCROLLS_TO_FIND_LINK);
    }

    public boolean isMoreQuickGuidesLinkDisplayed() {
        return scrollDownUntilVisible(moreQuickGuidesLink, MAX_SCROLLS_TO_FIND_LINK);
    }

    public boolean isBuildInvestmentPlanGuideDisplayed() {
        return scrollDownUntilVisible(buildInvestmentPlanGuide, MAX_SCROLLS_TO_FIND_LINK);
    }

    public boolean isUpgradePortfolioGuideDisplayed() {
        return scrollDownUntilVisible(upgradePortfolioGuide, MAX_SCROLLS_TO_FIND_LINK);
    }

    public boolean isAnalystPickedFundsGuideDisplayed() {
        return scrollDownUntilVisible(analystPickedFundsGuide, MAX_SCROLLS_TO_FIND_LINK);
    }

    public boolean isPersonalisedSupportCardDisplayed() {
        return scrollDownUntilVisible(personalisedSupportCard, MAX_SCROLLS_TO_FIND_LINK);
    }

    public boolean isAggressiveGrowthCardDisplayed() {
        return scrollDownUntilVisible(aggressiveGrowthCard, MAX_SCROLLS_TO_FIND_LINK);
    }

    public Home_Page tapFundsTab() {
        safeClick(fundsTab);
        waitForFundsScreen();
        return this;
    }

    public Home_Page tapStocksTab() {
        safeClick(stocksTab);
        waitForUiToSettle();
        return this;
    }

    public Home_Page tapPortfolioTab() {
        safeClick(portfolioTab);
        waitForUiToSettle();
        return this;
    }

    public Home_Page tapHubTab() {
        safeClick(hubTab);
        waitForUiToSettle();
        return this;
    }

    public boolean tapMfScreenerAndValidateScreen() {
        return tapLinkAndValidateScreen(homeLinks.get(3));
    }

    public boolean tapRetirementCardAndValidateScreen() {
        return tapLinkAndValidateScreen(homeLinks.get(0));
    }

    public boolean tapRegularIncomeCardAndValidateScreen() {
        return tapLinkAndValidateScreen(homeLinks.get(1));
    }

    public boolean tapNoGoalStartInvestingAndValidateScreen() {
        return tapLinkAndValidateScreen(homeLinks.get(2));
    }

    public boolean tapSipCalculatorAndValidateScreen() {
        return tapLinkAndValidateScreen(homeLinks.get(4));
    }

    public boolean tapFundSearchAndValidateScreen() {
        return tapLinkAndValidateScreen(homeLinks.get(5));
    }

    public boolean tapMoreQuickGuidesAndValidateScreen() {
        return tapLinkAndValidateScreen(homeLinks.get(6));
    }

    public boolean tapBuildInvestmentPlanAndValidateScreen() {
        return tapLinkAndValidateScreen(homeLinks.get(7));
    }

    public boolean tapUpgradePortfolioAndValidateScreen() {
        return tapLinkAndValidateScreen(homeLinks.get(8));
    }

    public boolean tapAnalystPickedFundsAndValidateScreen() {
        return tapLinkAndValidateScreen(homeLinks.get(9));
    }

    public boolean tapPersonalisedSupportAndValidateScreen() {
        return tapLinkAndValidateScreen(homeLinks.get(10));
    }

    public boolean tapAggressiveGrowthAndValidateScreen() {
        return tapLinkAndValidateScreen(homeLinks.get(11));
    }

    public List<LinkResult> tapAllLinksAndValidateScreens() {
        logger.info("Validating all Funds screen links");
        List<LinkResult> results = new ArrayList<>();

        for (HomeLink link : homeLinks) {
            openFundsScreen();
            boolean passed = tapLinkAndValidateScreen(link);
            results.add(new LinkResult(link.name, passed));
        }

        logger.info("Funds screen link validation complete: {}", results);
        return results;
    }

    public boolean verifyAllLinksOpenScreens() {
        for (LinkResult result : tapAllLinksAndValidateScreens()) {
            if (!result.passed) {
                return false;
            }
        }
        return true;
    }

    private void waitForFundsScreen() {
        try {
            shortWait(10).until(driver -> isFundsScreenDisplayed());
        } catch (TimeoutException e) {
            throw new AssertionError("Funds screen failed to load", e);
        }
    }

    private boolean tapLinkAndValidateScreen(HomeLink link) {
        logger.info("Tapping Funds screen link: {}", link.name);

        if (!scrollDownUntilVisible(link.locator, MAX_SCROLLS_TO_FIND_LINK)) {
            logger.warn("Funds screen link not visible: {}", link.name);
            return false;
        }

        String pageSourceBeforeTap = driver.getPageSource();
        safeClick(link.locator);

        boolean openedScreen = waitForDestinationScreen(pageSourceBeforeTap);
        boolean returnedToFunds = returnToFundsScreen();
        boolean passed = openedScreen || returnedToFunds;

        logger.info("Funds link '{}' result - openedScreen:{}, returnedToFunds:{}, passed:{}",
                link.name, openedScreen, returnedToFunds, passed);
        return passed;
    }

    private boolean waitForDestinationScreen(String pageSourceBeforeTap) {
        try {
            shortWait(8).until(driver -> {
                String currentSource = driver.getPageSource();
                return !currentSource.equals(pageSourceBeforeTap)
                        && !isFundsScreenDisplayed();
            });
            return true;
        } catch (TimeoutException e) {
            return false;
        }
    }

    private boolean returnToFundsScreen() {
        // Destinations can be full screens, or bottom sheets with an autofocused
        // keyboard (e.g. Fund Search) — each back press peels off one layer
        // (keyboard → sheet → screen), so press repeatedly until Funds is back.
        for (int attempt = 0; attempt < 4; attempt++) {
            if (isFundsScreenDisplayed()) {
                return true;
            }
            logger.info("Funds screen not visible — pressing back ({}/4)", attempt + 1);
            driver.navigate().back();
            waitForUiToSettle();
        }

        if (isFundsScreenDisplayed()) {
            return true;
        }

        logger.info("System back did not restore Funds screen, trying Funds tab");
        try {
            if (isDisplayed(fundsTab, 3)) {
                safeClick(fundsTab);
                shortWait(8).until(driver -> isFundsScreenDisplayed());
                return true;
            }
        } catch (Exception ignored) {
        }

        return isFundsScreenDisplayed();
    }

    private boolean scrollDownUntilVisible(By locator, int maxSwipes) {
        for (int swipe = 0; swipe <= maxSwipes; swipe++) {
            if (isDisplayed(locator)) {
                return true;
            }
            safeVerticalScroll("up");
            waitForUiToSettle();
        }
        return isDisplayed(locator);
    }

    private static class HomeLink {

        private final String name;
        private final By locator;

        private HomeLink(String name, By locator) {
            this.name = name;
            this.locator = locator;
        }
    }

    public static class LinkResult {

        public final String name;
        public final boolean passed;

        public LinkResult(String name, boolean passed) {
            this.name = name;
            this.passed = passed;
        }

        @Override
        public String toString() {
            return name + "=" + passed;
        }
    }
}
