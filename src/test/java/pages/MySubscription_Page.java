package pages;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.openqa.selenium.By;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebElement;

import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;

/**
 * Advisor "My Subscription" (Subscription Details) screen.
 *
 * Locator strategy notes (from the captured UiAutomator2 hierarchy):
 *  - Labels and values are exposed as content-desc on android.view.View, so
 *    accessibilityId is the primary, stable strategy.
 *  - Date values use [0-9]/[A-Za-z] character classes rather than the \d
 *    shorthand: Appium's UiSelector string parser does not apply Java escape
 *    processing, so a backslash-d would reach UiAutomator literally.
 *  - The top-right header icon has no content-desc, so a positional ImageView
 *    is used as a last resort.
 *
 * Navigating links on this screen: "Invoice" and the header icon. "Active" is a
 * status indicator that the accessibility tree marks clickable but which does NOT
 * open a new screen, so the crawler skips it (see SKIP_LINKS). The recursive
 * crawler taps content-desc'd links, follows each opened screen for further links,
 * and backs out - bounded by depth and a visited-set, skipping status/destructive/
 * navigation labels.
 */
public class MySubscription_Page extends BasePage {

    // Bounds the depth-first link crawl so a cyclic/!back-restoring screen cannot loop forever.
    private static final int MAX_CRAWL_DEPTH = 4;

    // Links never tapped by the crawler:
    //  - navigation / destructive actions
    //  - status indicators ("Active"/"Inactive"/"Expired") which are reported as
    //    clickable by the accessibility tree but do NOT navigate to a new screen.
    private static final Set<String> SKIP_LINKS = new HashSet<>(Arrays.asList(
            "Go back", "Back", "Logout", "Log out", "Sign out",
            "Delete", "Remove", "Cancel Subscription", "Close",
            "Active", "Inactive", "Expired"));

    // ============================================================
    // ENTRY POINT / NAVIGATION
    // ============================================================

    /** Menu/profile entry that opens this screen. */
    private final By subscriptionEntry = AppiumBy.accessibilityId("Subscription Details");

    private final By backButton = AppiumBy.accessibilityId("Go back");

    /** Top-right header icon: no content-desc, positional (first ImageView). */
    private final By headerActionIcon = AppiumBy
            .androidUIAutomator("new UiSelector().className(\"android.widget.ImageView\").instance(0)");

    // ============================================================
    // SCREEN ANCHORS
    // ============================================================

    private final By subscriptionHeading = AppiumBy.accessibilityId("My Subscription");
    private final By planName = AppiumBy.accessibilityId("Fund Advisor");

    // ============================================================
    // DATA LABELS (content-desc)
    // ============================================================

    private final By memberFromLabel = AppiumBy.accessibilityId("Member from");
    private final By autoRenewLabel = AppiumBy.accessibilityId("Auto-renews on");
    private final By amountLabel = AppiumBy.accessibilityId("Amount");
    private final By frequencyLabel = AppiumBy.accessibilityId("Frequency");

    // ============================================================
    // DATA VALUES
    // ============================================================

    /** Dates like "20 May, 2026" - day, month name, year. */
    private final By dateValue = AppiumBy
            .androidUIAutomator("new UiSelector().descriptionMatches(\"[0-9]{1,2} [A-Za-z]{3,9}, [0-9]{4}\")");

    /** Amount contains the rupee sign. */
    private final By amountValue = AppiumBy
            .androidUIAutomator("new UiSelector().descriptionContains(\"₹\")");

    // ============================================================
    // LINKS (clickable)
    // ============================================================

    private final By invoiceLink = AppiumBy.accessibilityId("Invoice");
    private final By statusActiveLink = AppiumBy.accessibilityId("Active");

    // Generic clickable-link selector used by the recursive crawler.
    private final By anyClickable = AppiumBy
            .androidUIAutomator("new UiSelector().clickable(true)");

    // ============================================================

    public MySubscription_Page(AndroidDriver driver) {
        super(driver);
    }

    // ============================================================
    // NAVIGATION
    // ============================================================

    /**
     * Opens the My Subscription screen. If already on it, returns immediately.
     * Otherwise taps the "Subscription Details" entry (scrolling to it if needed).
     */
    public MySubscription_Page openSubscriptionScreen() {
        logger.info("Opening My Subscription screen");

        if (isSubscriptionScreenDisplayed()) {
            logger.info("My Subscription screen already open");
            return this;
        }

        if (!isDisplayed(subscriptionEntry)) {
            scrollDownUntilVisible(subscriptionEntry, 8);
        }
        safeClick(subscriptionEntry);
        waitForSubscriptionScreen();
        logger.info("My Subscription screen opened");
        return this;
    }

    private void waitForSubscriptionScreen() {
        try {
            wait.until(driver -> isSubscriptionScreenDisplayed());
        } catch (TimeoutException e) {
            throw new AssertionError("My Subscription screen failed to load", e);
        }
    }

    // ============================================================
    // SCREEN DETECTION
    // ============================================================

    public boolean isSubscriptionScreenDisplayed() {
        return isDisplayed(subscriptionHeading) && isAnyDisplayed(memberFromLabel, amountLabel);
    }

    // ============================================================
    // LABEL VISIBILITY
    // ============================================================

    public boolean isPlanNameDisplayed() {
        logger.info("Checking plan name visibility");
        waitForVisible(planName);
        return isDisplayed(planName);
    }

    public boolean isMemberFromLabelDisplayed() {
        logger.info("Checking Member from label visibility");
        waitForVisible(memberFromLabel);
        return isDisplayed(memberFromLabel);
    }

    public boolean isAutoRenewLabelDisplayed() {
        logger.info("Checking Auto-renews on label visibility");
        waitForVisible(autoRenewLabel);
        return isDisplayed(autoRenewLabel);
    }

    public boolean isAmountLabelDisplayed() {
        logger.info("Checking Amount label visibility");
        waitForVisible(amountLabel);
        return isDisplayed(amountLabel);
    }

    public boolean isFrequencyLabelDisplayed() {
        logger.info("Checking Frequency label visibility");
        waitForVisible(frequencyLabel);
        return isDisplayed(frequencyLabel);
    }

    // ============================================================
    // VALUE VISIBILITY
    // ============================================================

    public boolean isAmountValueDisplayed() {
        logger.info("Checking amount value visibility");
        waitForVisible(amountValue);
        return isDisplayed(amountValue);
    }

    public boolean isDateValueDisplayed() {
        logger.info("Checking date value visibility");
        waitForVisible(dateValue);
        return isDisplayed(dateValue);
    }

    public boolean isStatusActiveDisplayed() {
        logger.info("Checking Active status visibility");
        waitForVisible(statusActiveLink);
        return isDisplayed(statusActiveLink);
    }

    // ============================================================
    // VALUE GETTERS  (values are content-desc, so read content-desc)
    // ============================================================

    public String getPlanName() {
        return readContentDesc(planName);
    }

    public String getAmount() {
        return readContentDesc(amountValue);
    }

    /** First date on the screen = "Member from". */
    public String getMemberFromDate() {
        return readContentDesc(AppiumBy.androidUIAutomator(
                "new UiSelector().descriptionMatches(\"[0-9]{1,2} [A-Za-z]{3,9}, [0-9]{4}\").instance(0)"));
    }

    /** Second date on the screen = "Auto-renews on". */
    public String getAutoRenewDate() {
        return readContentDesc(AppiumBy.androidUIAutomator(
                "new UiSelector().descriptionMatches(\"[0-9]{1,2} [A-Za-z]{3,9}, [0-9]{4}\").instance(1)"));
    }

    /** Reads the billing frequency from the known set of values. */
    public String getFrequency() {
        for (String f : Arrays.asList("Yearly", "Monthly", "Quarterly", "Half-Yearly", "Weekly")) {
            if (isDisplayed(AppiumBy.accessibilityId(f))) {
                return f;
            }
        }
        return "";
    }

    public String getStatus() {
        if (isDisplayed(statusActiveLink)) return "Active";
        if (isDisplayed(AppiumBy.accessibilityId("Inactive"))) return "Inactive";
        if (isDisplayed(AppiumBy.accessibilityId("Expired"))) return "Expired";
        return "";
    }

    // ============================================================
    // LINK VISIBILITY
    // ============================================================

    public boolean isBackButtonDisplayed() {
        return isDisplayed(backButton);
    }

    public boolean isHeaderActionIconDisplayed() {
        return isDisplayed(headerActionIcon);
    }

    public boolean isInvoiceLinkDisplayed() {
        return isDisplayed(invoiceLink);
    }

    public boolean isActiveLinkDisplayed() {
        return isDisplayed(statusActiveLink);
    }

    // ============================================================
    // SINGLE-LINK TAP + VALIDATE + RETURN
    // ============================================================

    /** Taps "Invoice", validates a new screen opens, then returns. */
    public boolean tapInvoiceAndVerify() {
        return tapLinkAndVerify(invoiceLink, "Invoice");
    }

    /** Taps the "Active" status row, validates a new screen opens, then returns. */
//    public boolean tapActiveAndVerify() {
//        return tapLinkAndVerify(statusActiveLink, "Active");
//    }

    /** Taps the top-right header icon, validates a new screen opens, then returns. */
    public boolean tapHeaderActionIconAndVerify() {
        if (!isHeaderActionIconDisplayed()) {
            logger.warn("Header action icon not visible - skipping");
            return false;
        }
        return tapLinkAndVerify(headerActionIcon, "Header action icon");
    }

    /** Taps "Go back" and verifies navigation away from the My Subscription screen. */
    public boolean tapBackAndVerify() {
        logger.info("=== Link test: Back ===");
        safeClick(backButton);
        waitForUiToSettle();
        boolean leftScreen = !isSubscriptionScreenDisplayed();
        logger.info("Navigated away from My Subscription: {}", leftScreen);
        return leftScreen;
    }

    private boolean tapLinkAndVerify(By link, String name) {
        logger.info("=== Link test: {} ===", name);
        String before = screenSignature();
        safeClick(link);
        waitForUiToSettle();
        boolean opened = screenChanged(before);
        logger.info("Link '{}' opened a new screen: {}", name, opened);
        returnToSubscriptionScreen();
        logger.info("=== Link test: {} - done ===", name);
        return opened;
    }

    // ============================================================
    // RECURSIVE LINK CRAWL
    // Requirement: tap every available link; on each opened screen, look for
    // further links and tap those too, recursing until no new links remain.
    // ============================================================

    /**
     * Depth-first crawl of every content-desc'd clickable link reachable from the
     * My Subscription screen. Validates that each tap opens a new screen, follows
     * nested links, and backs out between branches. Returns one LinkResult per
     * link tapped (passed = a new screen was detected after the tap).
     */
    public List<LinkResult> tapAllLinksRecursively() {
        logger.info("=== Recursive link crawl: start ===");
        List<LinkResult> results = new ArrayList<>();
        Set<String> visited = new HashSet<>();

        crawl(0, results, visited);

        // Make sure the crawl ended back on the subscription screen.
        returnToSubscriptionScreen();
        logger.info("=== Recursive link crawl: complete - {} link(s) tapped ===", results.size());
        return results;
    }

    public boolean verifyAllLinksOpenScreens() {
        for (LinkResult result : tapAllLinksRecursively()) {
            if (!result.passed) {
                return false;
            }
        }
        return true;
    }

    private void crawl(int depth, List<LinkResult> results, Set<String> visited) {
        if (depth >= MAX_CRAWL_DEPTH) {
            logger.info("Crawl depth {} reached - stopping this branch", MAX_CRAWL_DEPTH);
            return;
        }

        List<String> linkDescs = currentClickableLinkDescs();
        logger.info("Depth {}: {} candidate link(s): {}", depth, linkDescs.size(), linkDescs);

        for (String desc : linkDescs) {
            if (visited.contains(desc) || isSkipped(desc)) {
                continue;
            }
            visited.add(desc);

            By linkLocator = AppiumBy.androidUIAutomator(
                    "new UiSelector().description(\"" + desc + "\")");
            if (!isDisplayed(linkLocator)) {
                continue;
            }

            try {
                String before = screenSignature();
                logger.info("Depth {}: tapping link '{}'", depth, desc);
                safeClick(linkLocator);
                waitForUiToSettle();

                boolean opened = screenChanged(before);
                results.add(new LinkResult(desc, opened));

                if (opened) {
                    // New screen - look for further links and follow them.
                    crawl(depth + 1, results, visited);
                }
            } catch (Exception e) {
                logger.warn("Depth {}: link '{}' failed: {}", depth, desc, e.getMessage());
                results.add(new LinkResult(desc, false));
            } finally {
                // Back out so the next sibling link is reachable from this level.
                navigateBackOnce();
                waitForUiToSettle();
            }
        }
    }

    /** Content-descs of all currently-visible clickable elements (skips empty/desc-less). */
    private List<String> currentClickableLinkDescs() {
        List<String> descs = new ArrayList<>();
        try {
            List<WebElement> clickables = findElements(anyClickable);
            for (WebElement el : clickables) {
                try {
                    String d = el.getAttribute("content-desc");
                    if (d != null && !d.trim().isEmpty() && !descs.contains(d)) {
                        descs.add(d);
                    }
                } catch (Exception ignored) {
                }
            }
        } catch (Exception e) {
            logger.warn("Unable to enumerate clickable links: {}", e.getMessage());
        }
        return descs;
    }

    private boolean isSkipped(String desc) {
        return SKIP_LINKS.contains(desc);
    }

    // ============================================================
    // ACTIONS
    // ============================================================

    public MySubscription_Page tapBack() {
        logger.info("Tapping back on My Subscription screen");
        safeClick(backButton);
        waitForUiToSettle();
        return this;
    }

    // ============================================================
    // INTERNAL HELPERS
    // ============================================================

    /**
     * Lightweight screen signature: the joined content-descs of clickable elements.
     * Used to detect that a tap navigated to a different screen without paying the
     * cost (and instability) of a full getPageSource dump on a Flutter tree.
     */
    private String screenSignature() {
        return String.join("|", currentClickableLinkDescs());
    }

    private boolean screenChanged(String beforeSignature) {
        try {
            shortWait(8).until(driver -> !screenSignature().equals(beforeSignature));
            return true;
        } catch (TimeoutException e) {
            return false;
        }
    }

    private void navigateBackOnce() {
        try {
            driver.navigate().back();
        } catch (Exception e) {
            logger.warn("navigate().back() failed: {}", e.getMessage());
        }
    }

    /** Presses back until the My Subscription screen is shown again (bounded). */
    private boolean returnToSubscriptionScreen() {
        for (int attempt = 0; attempt < MAX_CRAWL_DEPTH + 2; attempt++) {
            if (isSubscriptionScreenDisplayed()) {
                return true;
            }
            navigateBackOnce();
            waitForUiToSettle();
        }
        return isSubscriptionScreenDisplayed();
    }

    private String readContentDesc(By locator) {
        try {
            return waitForVisible(locator).getAttribute("content-desc");
        } catch (Exception e) {
            logger.warn("Unable to read content-desc for {}", locator);
            return "";
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

    // ============================================================

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
