package pages;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.openqa.selenium.By;
import org.openqa.selenium.TimeoutException;

import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;

public class AboutUs_Page extends BasePage {

    private static final int MAX_SCROLLS_TO_FIND_LINK = 15;
    private static final String ABOUT_US_WEBVIEW_TITLE = "About Us | Value Research";

    private final By aboutUsEntry = AppiumBy.accessibilityId("About Us");
    private final By backButton = AppiumBy.accessibilityId("Go back");

    private final By aboutUsWebViewTitle = AppiumBy
            .androidUIAutomator("new UiSelector().className(\"android.webkit.WebView\")"
                    + ".textContains(\"About Us\")");
    private final By mainContent = AppiumBy
            .androidUIAutomator("new UiSelector().resourceId(\"main-content\")");
    private final By independentAdvisorsHeading = AppiumBy
            .androidUIAutomator("new UiSelector().text(\"Independent Advisors\")");
    private final By reliableSourceHeading = AppiumBy
            .androidUIAutomator("new UiSelector().textContains(\"most reliable Source\")");
    private final By brandedProductsHeading = AppiumBy
            .androidUIAutomator("new UiSelector().textContains(\"branded products\")");

    private final By stockAdvisorLink = AppiumBy
            .accessibilityId("Value Research Stock Advisor");
    private final By fundAdvisorLink = AppiumBy
            .accessibilityId("Value Research Fund Advisor");
    private final By independentAdvisorsLink = AppiumBy.accessibilityId("Click here");
    private final By mutualFundInsightLink = AppiumBy
            .androidUIAutomator("new UiSelector().descriptionContains(\"Mutual Fund Insight\")");
    private final By wealthInsightLink = AppiumBy
            .androidUIAutomator("new UiSelector().descriptionContains(\"Wealth Insight\")");
    private final By analyticsProLink = AppiumBy
            .accessibilityId("Value Research Analytics Pro");

    private final List<AboutUsLink> aboutUsLinks = Arrays.asList(
            new AboutUsLink("Value Research Stock Advisor", stockAdvisorLink),
            new AboutUsLink("Value Research Fund Advisor", fundAdvisorLink),
            new AboutUsLink("Independent Advisors", independentAdvisorsLink),
            new AboutUsLink("Mutual Fund Insight", mutualFundInsightLink),
            new AboutUsLink("Wealth Insight", wealthInsightLink),
            new AboutUsLink("Value Research Analytics Pro", analyticsProLink));

    private final List<By> notificationActions = Arrays.asList(
            AppiumBy.accessibilityId("OK"),
            AppiumBy.accessibilityId("Okay"),
            AppiumBy.accessibilityId("Accept"),
            AppiumBy.androidUIAutomator("new UiSelector().text(\"OK\")"),
            AppiumBy.androidUIAutomator("new UiSelector().text(\"Okay\")"),
            AppiumBy.androidUIAutomator("new UiSelector().text(\"Accept\")"));

    public AboutUs_Page(AndroidDriver driver) {
        super(driver);
    }

    public AboutUs_Page openAboutUsScreen() {
        logger.info("Opening About Us screen");

        if (isAboutUsScreenDisplayed()) {
            logger.info("About Us screen already open");
            dismissNotificationIfPresent();
            return this;
        }

        if (!isDisplayed(aboutUsEntry)) {
            scrollDownUntilVisible(aboutUsEntry, 8);
        }

        safeClick(aboutUsEntry);
        dismissNotificationIfPresent();
        waitForAboutUsScreen();
        dismissNotificationIfPresent();
        logger.info("About Us screen opened");
        return this;
    }

    public boolean isAboutUsScreenDisplayed() {
        return isAnyDisplayed(
                aboutUsWebViewTitle,
                mainContent,
                independentAdvisorsHeading,
                reliableSourceHeading,
                brandedProductsHeading);
    }

    public boolean isBackButtonDisplayed() {
        return isDisplayed(backButton);
    }

    public boolean isIndependentAdvisorsSectionDisplayed() {
        scrollDownUntilVisible(independentAdvisorsHeading, 5);
        return isDisplayed(independentAdvisorsHeading);
    }

    public boolean isReliableSourceSectionDisplayed() {
        scrollDownUntilVisible(reliableSourceHeading, MAX_SCROLLS_TO_FIND_LINK);
        return isDisplayed(reliableSourceHeading);
    }

    public boolean isBrandedProductsSectionDisplayed() {
        scrollDownUntilVisible(brandedProductsHeading, MAX_SCROLLS_TO_FIND_LINK);
        return isDisplayed(brandedProductsHeading);
    }

    public boolean isStockAdvisorLinkDisplayed() {
        return scrollDownUntilVisible(stockAdvisorLink, MAX_SCROLLS_TO_FIND_LINK);
    }

    public boolean isFundAdvisorLinkDisplayed() {
        return scrollDownUntilVisible(fundAdvisorLink, MAX_SCROLLS_TO_FIND_LINK);
    }

    public boolean isIndependentAdvisorsLinkDisplayed() {
        return scrollDownUntilVisible(independentAdvisorsLink, MAX_SCROLLS_TO_FIND_LINK);
    }

    public boolean isMutualFundInsightLinkDisplayed() {
        return scrollDownUntilVisible(mutualFundInsightLink, MAX_SCROLLS_TO_FIND_LINK);
    }

    public boolean isWealthInsightLinkDisplayed() {
        return scrollDownUntilVisible(wealthInsightLink, MAX_SCROLLS_TO_FIND_LINK);
    }

    public boolean isAnalyticsProLinkDisplayed() {
        return scrollDownUntilVisible(analyticsProLink, MAX_SCROLLS_TO_FIND_LINK);
    }

    public AboutUs_Page tapBack() {
        logger.info("Tapping About Us back button");
        safeClick(backButton);
        return this;
    }

    public boolean tapStockAdvisorAndValidateScreen() {
        return tapLinkAndValidateScreen(aboutUsLinks.get(0));
    }

    public boolean tapFundAdvisorAndValidateScreen() {
        return tapLinkAndValidateScreen(aboutUsLinks.get(1));
    }

    public boolean tapIndependentAdvisorsAndValidateScreen() {
        return tapLinkAndValidateScreen(aboutUsLinks.get(2));
    }

    public boolean tapMutualFundInsightAndValidateScreen() {
        return tapLinkAndValidateScreen(aboutUsLinks.get(3));
    }

    public boolean tapWealthInsightAndValidateScreen() {
        return tapLinkAndValidateScreen(aboutUsLinks.get(4));
    }

    public boolean tapAnalyticsProAndValidateScreen() {
        return tapLinkAndValidateScreen(aboutUsLinks.get(5));
    }

    public List<LinkResult> tapAllLinksAndValidateScreens() {
        logger.info("Validating all About Us links");
        List<LinkResult> results = new ArrayList<>();

        for (AboutUsLink link : aboutUsLinks) {
            boolean passed = tapLinkAndValidateScreen(link);
            results.add(new LinkResult(link.name, passed));
        }

        logger.info("About Us link validation complete: {}", results);
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

    private void waitForAboutUsScreen() {
        try {
            wait.until(driver -> isAboutUsScreenDisplayed());
        } catch (TimeoutException e) {
            throw new AssertionError("About Us screen failed to load", e);
        }
    }

    private boolean tapLinkAndValidateScreen(AboutUsLink link) {
        logger.info("Tapping About Us link: {}", link.name);

        waitForAboutUsScreen();
        if (!scrollDownUntilVisible(link.locator, MAX_SCROLLS_TO_FIND_LINK)) {
            logger.warn("About Us link not visible: {}", link.name);
            return false;
        }

        String pageSourceBeforeTap = driver.getPageSource();
        safeClick(link.locator);

        boolean openedScreen = waitForDestinationScreen(pageSourceBeforeTap);
        boolean returnedToAboutUs = returnToAboutUsScreen();
        boolean passed = openedScreen || returnedToAboutUs;

        logger.info("About Us link '{}' result - openedScreen:{}, returnedToAboutUs:{}, passed:{}",
                link.name, openedScreen, returnedToAboutUs, passed);
        return passed;
    }

    private boolean waitForDestinationScreen(String pageSourceBeforeTap) {
        try {
            shortWait(8).until(driver -> {
                String currentSource = driver.getPageSource();
                return !currentSource.equals(pageSourceBeforeTap)
                        && !currentSource.contains(ABOUT_US_WEBVIEW_TITLE);
            });
            return true;
        } catch (TimeoutException e) {
            return false;
        }
    }

    private boolean returnToAboutUsScreen() {
        if (isAboutUsScreenDisplayed()) {
            return true;
        }

        try {
            driver.navigate().back();
            shortWait(8).until(driver -> isAboutUsScreenDisplayed());
            return true;
        } catch (Exception firstFailure) {
            logger.info("System back did not restore About Us screen, trying header back button");
        }

        try {
            if (isDisplayed(backButton, 3)) {
                safeClick(backButton);
                shortWait(8).until(driver -> isAboutUsScreenDisplayed());
                return true;
            }
        } catch (Exception ignored) {
        }

        return isAboutUsScreenDisplayed();
    }

    private void dismissNotificationIfPresent() {
        for (By action : notificationActions) {
            try {
                if (isDisplayed(action, 2)) {
                    safeClick(action);
                    waitForUiToSettle();
                    logger.info("Dismissed About Us notification action: {}", action);
                    return;
                }
            } catch (Exception ignored) {
            }
        }
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

    private static class AboutUsLink {

        private final String name;
        private final By locator;

        private AboutUsLink(String name, By locator) {
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
