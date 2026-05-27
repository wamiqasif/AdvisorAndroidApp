package pages;

import java.util.Arrays;
import java.util.List;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;

public class DashboardPage extends BasePage {

    private final By portfolioTab =
            AppiumBy.accessibilityId("Portfolio");

    private final By hubTab =
            AppiumBy.accessibilityId("Hub");

    private final By fundsTab =
            AppiumBy.accessibilityId("Funds");

    private final By stocksTab =
            AppiumBy.accessibilityId("Stocks");
    By bellIcon=AppiumBy.androidUIAutomator("new UiSelector().className(\"android.widget.ImageView\").instance(0)");

    public DashboardPage(AndroidDriver driver) {

        super(driver);
    }

    public DashboardPage tapPortfolioTab() {

        logger.info("Tapping Portfolio tab");
        safeClick(portfolioTab);
        waitForUiToSettle();
        logger.info("Portfolio tab tapped");

        return this;
    }

    public DashboardPage tapHubTab() {

        logger.info("Tapping Hub tab");
        safeClick(hubTab);
        waitForUiToSettle();
        logger.info("Hub tab tapped");

        return this;
    }

    public DashboardPage tapFundsTab() {

        logger.info("Tapping Funds tab");
        safeClick(fundsTab);
        waitForUiToSettle();
        logger.info("Funds tab tapped");

        return this;
    }

    public DashboardPage tapStocksTab() {

        logger.info("Tapping Stocks tab");
        safeClick(stocksTab);
        waitForUiToSettle();
        logger.info("Stocks tab tapped");

        return this;
    }

    public boolean isDashboardDisplayed() {

        return isBottomNavigationVisible();
    }

    public boolean isDashboardDisplayed(int seconds) {

        return isDashboardDisplayed();
    }

    public boolean isDashboardNotDisplayed() {

        return !isDashboardDisplayed();
    }

    public boolean isFundsTabDisplayed() {

        return isDisplayed(fundsTab);
    }

    public boolean isStocksTabDisplayed() {

        return isDisplayed(stocksTab);
    }

    public boolean isHubTabDisplayed() {

        return isDisplayed(hubTab);
    }

    public boolean isBottomNavigationVisible() {

        return isDisplayed(portfolioTab)
                && isDisplayed(hubTab)
                && isDisplayed(fundsTab)
                && isDisplayed(bellIcon);
    }

    public void waitForDashboardLoaded() {

        waitForUiToSettle();
    }
    public boolean forceNavigateToHome() {

        try {

            List<By> tabs =
                    Arrays.asList(

                            AppiumBy.accessibilityId("Hub"),
                            AppiumBy.accessibilityId("Portfolio"),
                            AppiumBy.accessibilityId("Funds"),
                            AppiumBy.accessibilityId("Stocks"));

            for (By tab : tabs) {

                List<WebElement> elements =
                        driver.findElements(tab);

                if (!elements.isEmpty()
                        && elements.get(0).isDisplayed()) {

                    elements.get(0).click();

                    waitForUiToSettle();

                    // Normalize to Hub
                    List<WebElement> hub =
                            driver.findElements(
                                    AppiumBy.accessibilityId("Hub"));

                    if (!hub.isEmpty()) {

                        hub.get(0).click();

                        waitForUiToSettle();

                        return true;
                    }
                }
            }

        } catch (Exception ignored) {
        }

        return false;
    }

    public boolean recoverToDashboard() {

        logger.info("recoverToDashboard: checking current state");

        if (isDashboardDisplayed()) {

            logger.info("recoverToDashboard: already on dashboard");
            return true;
        }

        if (isBottomNavigationVisible()) {

            logger.info("recoverToDashboard: bottom navigation visible — tapping Hub tab");
            tapHubTab();
            waitForDashboardLoaded();
            boolean result = isDashboardDisplayed();
            logger.info("recoverToDashboard: recovery result — {}", result ? "success" : "failed");
            return result;
        }

        logger.warn("recoverToDashboard: bottom navigation not visible — cannot recover");
        return false;
    }
}