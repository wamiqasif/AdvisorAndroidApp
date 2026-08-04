package com.valueresearch.pages;

import com.valueresearch.utils.ReportLogger;
import com.valueresearch.utils.ExtentTestManager;
import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;
import org.openqa.selenium.By;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.Rectangle;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.PointerInput;
import org.openqa.selenium.interactions.Sequence;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.regex.Pattern;

public class ManageSipSwpPage {

    private final AndroidDriver driver;


    // MSS_FAST_V1_ACTIVE
    private static final boolean MSS_FAST_MODE = true;
    private static final long VISIBLE_STRINGS_CACHE_TTL_MS = 650L;
    private List<String> visibleStringsCache = null;
    private long visibleStringsCacheAtMs = 0L;


    private static final String PAGE_TITLE = "Your SIPs/SWPs";
    private static final String SIP_TAB = "SIP";
    private static final String SWP_TAB = "SWP";
    private static final String DEFAULT_INVESTOR = "Manish Khatri";

    public ManageSipSwpPage(AndroidDriver driver) {
        this.driver = driver;
    }

    // =========================================================
    // MSS_001 - OPEN MANAGE SIP/SWP FROM HUB
    // =========================================================

    public void openManageSipSwpFromHub() {
        try {
            ReportLogger.step("Opening Manage SIP/SWP from Hub");

            if (isManageSipSwpListingPageVisible()) {
                ReportLogger.pass("Manage SIP/SWP listing page is already open");
                return;
            }

            recoverToAdvisorHomeIfNeeded();
            openHubTab();
            tapManageSipSwpTileFromHub();
            waitForManageSipSwpListingPage();

            ReportLogger.pass("Manage SIP/SWP listing page opened successfully");
        } catch (Exception e) {
            throw new AssertionError("Failed to open Manage SIP/SWP from Hub: " + cleanError(e.getMessage()), e);
        }
    }


    private void openHubTab() {
        ReportLogger.step("Opening Hub bottom tab (MSS_FAST_V1)");

        if (tapIfVisible(byDesc("Hub"), "Hub bottom tab")) {
            sleep(550);
            ReportLogger.pass("Hub bottom tab tapped");
            return;
        }

        if (tapIfVisible(byDescContains("Hub"), "Hub bottom tab contains")) {
            sleep(550);
            ReportLogger.pass("Hub bottom tab tapped using contains locator");
            return;
        }

        Dimension size = driver.manage().window().getSize();
        int x = (int) (size.getWidth() * 0.50);
        int y = (int) (size.getHeight() * 0.955);

        ReportLogger.step("Tapping Hub bottom tab by fast coordinate fallback | x=" + x + " | y=" + y);
        tapByCoordinates(x, y);
        sleep(650);

        if (pageHasText("Hub") || pageHasText("Manage your SIPs") || pageHasText("Manage SIP")) {
            ReportLogger.pass("Hub bottom tab tapped using fast coordinate fallback");
            return;
        }

        WebElement hub = findTextElementInBottomBand("Hub");
        if (hub != null) {
            tapElementCenter(hub);
            sleep(550);
            ReportLogger.pass("Hub bottom tab tapped using bounded bottom scan");
            return;
        }

        throw new AssertionError("Hub bottom tab not found");
    }


    private void tapManageSipSwpTileFromHub() {
        ReportLogger.step("Finding and opening Manage SIP/SWP tile from Hub (MSS_FAST_V1)");

        for (int attempt = 1; attempt <= 8; attempt++) {
            ReportLogger.step("Searching Manage SIP/SWP tile on Hub | fastAttempt=" + attempt);

            if (tapManageSipSwpTileByFreshLocator()) {
                sleep(850);

                if (isManageSipSwpListingPageVisible()) {
                    ReportLogger.pass("Manage SIP/SWP tile opened using fresh locator");
                    return;
                }
            }

            if (attempt <= 4 && tapManageSipSwpTileByBoundedScan()) {
                sleep(850);

                if (isManageSipSwpListingPageVisible()) {
                    ReportLogger.pass("Manage SIP/SWP tile opened using bounded scan");
                    return;
                }
            }

            pageSwipeUpW3C();
            sleep(350);
        }

        throw new AssertionError("Manage SIP/SWP tile not found in Hub"
                + " | visibleValues=" + collectVisibleStrings());
    }

    private boolean tapManageSipSwpTileByFreshLocator() {
        By[] preferredLocators = new By[]{
                byDesc("Manage your SIPs and SWPs"),
                byDescContains("Manage your SIPs"),
                byDescContains("Manage your SIPs and SWPs"),
                byDesc("Manage SIP/SWP"),
                byDesc("Manage SIP/SWPs"),
                byDescContains("Manage SIP/SWP"),
                byDescContains("Manage SIP"),
                byDescContains("SIP/SWP"),
                byDescContains("SIPs/SWPs")
        };

        for (By locator : preferredLocators) {
            try {
                List<WebElement> elements = driver.findElements(locator);

                for (WebElement element : elements) {
                    try {
                        if (element == null || !element.isDisplayed()) {
                            continue;
                        }

                        String readable = normalizeSpaces(getElementReadableText(element));

                        if (!isManageSipSwpHubTileText(readable)) {
                            continue;
                        }

                        Rectangle rect = element.getRect();

                        if (!isUsableHubTileRect(rect)) {
                            continue;
                        }

                        int x = rect.getX() + rect.getWidth() / 2;
                        int y = rect.getY() + rect.getHeight() / 2;

                        ReportLogger.step("Tapping Manage SIP/SWP Hub tile using fresh locator"
                                + " | readable=" + readable
                                + " | x=" + x
                                + " | y=" + y);

                        tapByCoordinates(x, y);
                        return true;

                    } catch (Exception staleOrInvalidElement) {
                        ReportLogger.debug("Skipping stale Manage SIP/SWP tile element: "
                                + cleanError(staleOrInvalidElement.getMessage()));
                    }
                }

            } catch (Exception locatorError) {
                ReportLogger.debug("Manage SIP/SWP tile locator failed safely: "
                        + locator
                        + " | "
                        + cleanError(locatorError.getMessage()));
            }
        }

        return false;
    }

    private boolean tapManageSipSwpTileByBoundedScan() {
        try {
            Dimension size = driver.manage().window().getSize();
            List<WebElement> elements = driver.findElements(By.xpath("//*"));

            for (WebElement element : elements) {
                try {
                    if (element == null || !element.isDisplayed()) {
                        continue;
                    }

                    String readable = normalizeSpaces(getElementReadableText(element));

                    if (!isManageSipSwpHubTileText(readable)) {
                        continue;
                    }

                    Rectangle rect = element.getRect();

                    if (!isUsableHubTileRect(rect)) {
                        continue;
                    }

                    int centerX = rect.getX() + rect.getWidth() / 2;
                    int centerY = rect.getY() + rect.getHeight() / 2;

                    /*
                     * Hub card text can be exposed as a child View.
                     * Tap slightly lower/right inside the same card so the parent card receives the tap.
                     */
                    int tapX = Math.min(size.getWidth() - 40, centerX + (rect.getWidth() / 4));
                    int tapY = Math.min((int) (size.getHeight() * 0.88), centerY + Math.max(20, rect.getHeight() / 3));

                    ReportLogger.step("Tapping Manage SIP/SWP Hub tile using bounded scan"
                            + " | readable=" + readable
                            + " | x=" + tapX
                            + " | y=" + tapY);

                    tapByCoordinates(tapX, tapY);
                    return true;

                } catch (Exception staleOrInvalidElement) {
                    ReportLogger.debug("Skipping stale bounded Hub tile candidate: "
                            + cleanError(staleOrInvalidElement.getMessage()));
                }
            }
        } catch (Exception scanError) {
            ReportLogger.debug("Manage SIP/SWP Hub tile bounded scan skipped: "
                    + cleanError(scanError.getMessage()));
        }

        return false;
    }

    private boolean isManageSipSwpHubTileText(String readableText) {
        if (readableText == null) {
            return false;
        }

        String lower = normalizeSpaces(readableText).toLowerCase();

        if (lower.isEmpty()) {
            return false;
        }

        if (lower.contains("calculator")) {
            return false;
        }

        return (lower.contains("manage") && lower.contains("sip") && lower.contains("swp"))
                || lower.contains("manage your sips")
                || lower.contains("sip/swp")
                || lower.contains("sips/swps");
    }

    private boolean isUsableHubTileRect(Rectangle rect) {
        if (rect == null) {
            return false;
        }

        Dimension size = driver.manage().window().getSize();

        int centerY = rect.getY() + rect.getHeight() / 2;

        /*
         * Ignore status/header and bottom navigation/footer areas.
         */
        return rect.getWidth() > 0
                && rect.getHeight() > 0
                && centerY > (int) (size.getHeight() * 0.12)
                && centerY < (int) (size.getHeight() * 0.88);
    }

    private void waitForManageSipSwpListingPage() {
        for (int attempt = 1; attempt <= 12; attempt++) {
            if (isManageSipSwpListingPageVisible()) {
                ReportLogger.pass("Your SIPs/SWPs listing page is ready");
                return;
            }

            sleep(700);
        }

        throw new AssertionError("Your SIPs/SWPs listing page did not load");
    }

    // =========================================================
    // MSS_002 - LISTING PAGE
    // =========================================================

    public void verifySipSwpListingPage() {
        try {
            ReportLogger.step("Validating Your SIPs/SWPs listing page");

            recoverManageSipSwpListingIfNeeded();

            assertTextVisibleFlexible(PAGE_TITLE, "Manage SIP/SWP page title");
            assertTextVisibleFlexible(SIP_TAB, "SIP tab");
            assertTextVisibleFlexible(SWP_TAB, "SWP tab");

            validateInvestorDropdownVisibleOnListing();
            validateListingInfoOrDataVisible();

            ReportLogger.pass("Your SIPs/SWPs listing page validated successfully");
        } catch (Exception e) {
            throw new AssertionError("Manage SIP/SWP listing validation failed: " + cleanError(e.getMessage()), e);
        }
    }

    private void validateInvestorDropdownVisibleOnListing() {
        List<String> values = collectVisibleStrings();

        boolean investorVisible = false;

        for (String value : values) {
            String clean = normalizeSpaces(value);

            if (clean.contains(DEFAULT_INVESTOR)
                    || clean.contains("Manish")
                    || clean.contains("Lalit")
                    || clean.contains("Vinit")
                    || clean.toLowerCase().contains("investor")) {
                investorVisible = true;
                logValidatedText("Investor dropdown/value", clean);
                break;
            }
        }

        if (!investorVisible) {
            throw new AssertionError("Investor dropdown/value not visible on Your SIPs/SWPs page. Values=" + values);
        }

        ReportLogger.pass("Investor dropdown/value visible on Manage SIP/SWP page");
    }

    private void validateListingInfoOrDataVisible() {
        List<String> values = collectVisibleStrings();

        boolean hasSipText = false;
        boolean hasAmount = false;
        boolean hasFundLikeText = false;
        boolean hasEmptyState = false;

        for (String value : values) {
            String clean = normalizeSpaces(value);
            String lower = clean.toLowerCase();

            if (lower.contains("sip") || lower.contains("swp")) {
                hasSipText = true;
            }

            if (isRupeeAmountText(clean) || lower.contains("/month")) {
                hasAmount = true;
            }

            if (isFundNameLike(clean)) {
                hasFundLikeText = true;
            }

            if (lower.contains("no sip")
                    || lower.contains("no swp")
                    || lower.contains("no active")
                    || lower.contains("nothing")
                    || lower.contains("empty")) {
                hasEmptyState = true;
            }
        }

        if (!hasSipText && !hasFundLikeText && !hasEmptyState) {
            throw new AssertionError("Neither SIP/SWP data nor empty state visible. Values=" + values);
        }

        if (hasFundLikeText) {
            ReportLogger.pass("SIP/SWP fund card/list data is visible");
        }

        if (hasAmount) {
            ReportLogger.pass("SIP/SWP amount pattern is visible");
        }

        if (hasEmptyState) {
            ReportLogger.pass("SIP/SWP empty state is visible");
        }
    }

    // =========================================================
    // MSS_003 - INVESTOR DROPDOWN
    // =========================================================

    public void changeInvestorFromDropdown() {
        changeInvestorFromDropdown(DEFAULT_INVESTOR);
    }

    public void changeInvestorFromDropdown(String investorName) {
        try {
            ReportLogger.step("Changing investor from Manage SIP/SWP dropdown: " + investorName);

            recoverManageSipSwpListingIfNeeded();
            openInvestorDropdown();
            selectInvestorFromSheet(investorName);
            waitForManageSipSwpListingPage();

            ReportLogger.pass("Investor selected from Manage SIP/SWP dropdown: " + investorName);
            logValidatedText("Selected investor", investorName);
        } catch (Exception e) {
            throw new AssertionError("Investor dropdown validation failed: " + cleanError(e.getMessage()), e);
        }
    }

    private void openInvestorDropdown() {
        ReportLogger.step("Opening investor dropdown on Manage SIP/SWP page");

        if (isInvestorSheetOpen()) {
            ReportLogger.pass("Choose Investor sheet already open");
            return;
        }

        ensureManageSipSwpListingPageVisible();

        /*
         * Correct stable locator from Appium/Flutter source:
         * "Manish Khatri, Shows investor selection list"
         * Do this before any generic top-right clickable scan.
         */
        if (tapCorrectInvestorDropdownAccessibilityId()) {
            return;
        }

        /*
         * Secondary stable attempt:
         * Tap the readable investor selector only when the element text/desc proves
         * it is the investor selection control.
         */
        WebElement investorElement = findInvestorDropdownElementInTopBand();

        if (investorElement != null) {
            tapElementCenter(investorElement);
            sleep(1200);

            if (isInvestorSheetOpen()) {
                ReportLogger.pass("Investor dropdown opened using top-right investor text/element");
                return;
            }
        }

        /*
         * Last safe fallback only:
         * Use bounded top-right coordinates. Do not tap generic clickable elements
         * with null readable text because that was selecting the wrong control.
         */
        Dimension size = driver.manage().window().getSize();

        int[] yPoints = new int[]{
                (int) (size.getHeight() * 0.165),
                (int) (size.getHeight() * 0.145),
                (int) (size.getHeight() * 0.125),
                (int) (size.getHeight() * 0.105)
        };

        int[] xPoints = new int[]{
                (int) (size.getWidth() * 0.78),
                (int) (size.getWidth() * 0.86),
                (int) (size.getWidth() * 0.70),
                (int) (size.getWidth() * 0.62)
        };

        for (int y : yPoints) {
            for (int x : xPoints) {
                tapByCoordinates(x, y);
                sleep(700);

                if (isInvestorSheetOpen()) {
                    ReportLogger.pass("Investor dropdown opened using bounded fallback x=" + x + ", y=" + y);
                    return;
                }
            }
        }

        throw new AssertionError("Unable to open investor dropdown on Manage SIP/SWP page"
                + " | visibleValues=" + collectVisibleStrings());
    }

    
    private boolean tapCorrectInvestorDropdownAccessibilityId() {
        By[] correctLocators = new By[]{
                byDesc(DEFAULT_INVESTOR + ", Shows investor selection list"),
                byDescContains("Shows investor selection list"),
                byDescContains("investor selection list")
        };

        for (By locator : correctLocators) {
            try {
                WebElement element = findVisibleElement(locator);

                if (element == null) {
                    continue;
                }

                String readable = normalizeSpaces(getElementReadableText(element));
                Rectangle rect = element.getRect();

                ReportLogger.step("Clicking investor dropdown directly using accessibility locator"
                        + " | readable=" + readable
                        + " | bounds=" + rect);

                try {
                    element.click();
                    sleep(700);

                    if (isInvestorSheetOpen()) {
                        ReportLogger.pass("Investor dropdown opened using direct element click: " + readable);
                        return true;
                    }
                } catch (Exception clickError) {
                    ReportLogger.debug("Direct investor dropdown click did not open sheet: "
                            + cleanError(clickError.getMessage()));
                }

                /*
                 * Flutter exposes the semantic label on the investor selector, but
                 * the actual tappable hotspot is on the right/top part of the same
                 * header selector. Use the located element bounds, not fixed device
                 * coordinates, so this remains future-safe across screens.
                 */
                int dynamicX = rect.getX() + (int) (rect.getWidth() * 0.78);
                int dynamicY = rect.getY() + (int) (rect.getHeight() * 0.35);

                ReportLogger.step("Tapping investor dropdown hotspot from located accessibility bounds"
                        + " | readable=" + readable
                        + " | x=" + dynamicX
                        + " | y=" + dynamicY);

                tapByCoordinates(dynamicX, dynamicY);
                sleep(800);

                if (isInvestorSheetOpen()) {
                    ReportLogger.pass("Investor dropdown opened using located accessibility hotspot: " + readable);
                    return true;
                }

            } catch (Exception e) {
                ReportLogger.debug("Correct investor dropdown locator attempt skipped: " + cleanError(e.getMessage()));
            }
        }

        return false;
    }

    private void ensureManageSipSwpListingPageVisible() {
        if (isManageSipSwpListingPageVisible()) {
            return;
        }

        recoverManageSipSwpListingIfNeeded();

        if (!isManageSipSwpListingPageVisible()) {
            throw new AssertionError("Manage SIP/SWP listing page is not visible before opening investor dropdown"
                    + " | visibleValues=" + collectVisibleStrings());
        }
    }

    private WebElement findInvestorDropdownElementInTopBand() {
        Dimension size = driver.manage().window().getSize();

        try {
            List<WebElement> elements = driver.findElements(By.xpath("//*"));

            for (WebElement element : elements) {
                try {
                    if (element == null || !element.isDisplayed()) {
                        continue;
                    }

                    Rectangle rect = element.getRect();

                    if (!isInsideInvestorHeaderBand(rect, size)) {
                        continue;
                    }

                    String readable = normalizeSpaces(getElementReadableText(element));

                    if (isInvestorHeaderText(readable)) {
                        ReportLogger.pass("Investor selector element found in top-right band: " + readable);
                        return element;
                    }

                } catch (Exception ignored) {
                    // Ignore stale Flutter/Appium elements.
                }
            }
        } catch (Exception e) {
            ReportLogger.debug("Investor dropdown top-band text scan skipped: " + cleanError(e.getMessage()));
        }

        return null;
    }

    private WebElement findTopRightHeaderClickableElement() {
        Dimension size = driver.manage().window().getSize();

        try {
            List<WebElement> elements = driver.findElements(By.xpath("//*"));

            for (WebElement element : elements) {
                try {
                    if (element == null || !element.isDisplayed()) {
                        continue;
                    }

                    Rectangle rect = element.getRect();

                    if (!isInsideInvestorHeaderBand(rect, size)) {
                        continue;
                    }

                    String readable = normalizeSpaces(getElementReadableText(element));

                    /*
                     * Avoid tapping title/back/status content. The top-right selector
                     * may have empty readable text, so do not require text here.
                     */
                    if (readable.equals(PAGE_TITLE)
                            || readable.equals(SIP_TAB)
                            || readable.equals(SWP_TAB)
                            || readable.toLowerCase().contains("your sip")) {
                        continue;
                    }

                    ReportLogger.pass("Top-right header clickable candidate found for investor dropdown"
                            + " | readable=" + readable
                            + " | bounds=" + rect);
                    return element;

                } catch (Exception ignored) {
                    // Ignore stale Flutter/Appium elements.
                }
            }
        } catch (Exception e) {
            ReportLogger.debug("Top-right clickable header scan skipped: " + cleanError(e.getMessage()));
        }

        return null;
    }

    private boolean isInsideInvestorHeaderBand(Rectangle rect, Dimension size) {
        if (rect == null || size == null) {
            return false;
        }

        int centerX = rect.getX() + rect.getWidth() / 2;
        int centerY = rect.getY() + rect.getHeight() / 2;

        int minX = (int) (size.getWidth() * 0.58);
        int maxX = (int) (size.getWidth() * 0.98);
        int minY = (int) (size.getHeight() * 0.08);
        int maxY = (int) (size.getHeight() * 0.30);

        return centerX >= minX
                && centerX <= maxX
                && centerY >= minY
                && centerY <= maxY
                && rect.getWidth() > 0
                && rect.getHeight() > 0;
    }

    private boolean isInvestorHeaderText(String value) {
        if (value == null) {
            return false;
        }

        String clean = normalizeSpaces(value);
        String lower = clean.toLowerCase();

        if (clean.isEmpty()) {
            return false;
        }

        return lower.contains("manish")
                || lower.contains("lalit")
                || lower.contains("vinit")
                || lower.contains("khatri")
                || lower.contains("sharma")
                || lower.contains("investor")
                || lower.matches(".*[a-z]{3,}.*\\s*▼.*")
                || lower.matches(".*[a-z]{3,}.*\\s*⌄.*")
                || lower.matches(".*[a-z]{3,}.*\\s*v.*");
    }

    private boolean isInvestorSheetOpen() {
        return isVisible(byDesc("Choose Investor"))
                || isVisible(byDescContains("Choose Investor"))
                || pageHasText("Choose Investor");
    }

    private void selectInvestorFromSheet(String investorName) {
        ReportLogger.step("Selecting investor from sheet: " + investorName);

        if (!isInvestorSheetOpen()) {
            throw new AssertionError("Choose Investor sheet is not open");
        }

        if (pageHasText(investorName)) {
            WebElement investor = findVisibleTextElement(investorName);

            if (investor != null) {
                tapElementCenter(investor);
                sleep(1600);
                ReportLogger.pass("Investor tapped from sheet: " + investorName);
                return;
            }
        }

        for (int attempt = 1; attempt <= 6; attempt++) {
            WebElement investor = findVisibleTextElement(investorName);

            if (investor != null) {
                tapElementCenter(investor);
                sleep(1600);
                ReportLogger.pass("Investor tapped from sheet after scroll: " + investorName);
                return;
            }

            smallSwipeUpW3C();
            sleep(600);
        }

        /*
         * If the requested investor is already selected and the sheet did not expose
         * another matching row, close the sheet safely and continue.
         */
        if (pageHasText(investorName)) {
            pressBackSilently();
            sleep(1000);
            ReportLogger.pass("Investor already selected: " + investorName);
            return;
        }

        throw new AssertionError("Investor not found in Choose Investor sheet: " + investorName
                + " | values=" + collectVisibleStrings());
    }

    // =========================================================
    // MSS_004 - SIP CARD DATA
    // =========================================================

    public void verifySipCardData() {
        try {
            ReportLogger.step("Validating SIP card data on listing page");

            recoverManageSipSwpListingIfNeeded();
            openSipTabIfNeeded();

            SipCardData cardData = captureVisibleSipCardData();

            if (cardData == null) {
                throw new AssertionError(
                        "No SIP card evidence found on listing page. "
                                + "Expected a visible fund name or an amount + monthly schedule pair. "
                                + "Values=" + collectVisibleStrings()
                );
            }

            /*
             * Flutter currently exposes the SIP amounts and monthly dates reliably,
             * but the fund name may be painted visually without a separate semantic
             * node. Do not reject a genuine SIP card only because the fund title is
             * absent from the Android accessibility hierarchy.
             */
            if (!cardData.fundName.isEmpty()) {
                logValidatedText("SIP card fund", cardData.fundName);
            } else {
                ReportLogger.debug(
                        "SIP fund name is not separately exposed in the accessibility hierarchy. "
                                + "Validated the card using amount and monthly schedule."
                );
            }

            logValidatedText("SIP card amount", cardData.amount);
            logValidatedText("SIP card schedule", cardData.schedule);

            if (!cardData.tag.isEmpty()) {
                logValidatedText("SIP card tag", cardData.tag);
            }

            ReportLogger.pass("SIP card data validated successfully: " + cardData);
        } catch (AssertionError e) {
            throw e;
        } catch (Exception e) {
            throw new AssertionError("SIP card validation failed: " + cleanError(e.getMessage()), e);
        }
    }

    private SipCardData captureVisibleSipCardData() {
        List<String> values = collectVisibleStrings();

        String fundName = "";
        String amount = "";
        String schedule = "";
        String tag = "";

        for (String value : values) {
            String clean = normalizeSpaces(value);

            if (fundName.isEmpty() && isFundNameLike(clean)) {
                fundName = clean;
            }

            if (amount.isEmpty()) {
                amount = extractFirstRupeeAmountFromText(clean);
            }

            if (schedule.isEmpty()) {
                schedule = extractFirstSipScheduleFromText(clean);
            }

            if (tag.isEmpty()
                    && (clean.equalsIgnoreCase("External") || clean.equalsIgnoreCase("Internal"))) {
                tag = clean;
            }
        }

        boolean hasFundEvidence = !fundName.isEmpty();
        boolean hasStructuredCardEvidence = !amount.isEmpty() && !schedule.isEmpty();

        if (!hasFundEvidence && !hasStructuredCardEvidence) {
            return null;
        }

        return new SipCardData(fundName, amount, schedule, tag);
    }


    // =========================================================
    // MSS_005 - SIP DETAILS
    // =========================================================

    public void openSipDetailsAndValidateDetails() {
        try {
            ReportLogger.step("Opening SIP details and validating details");

            recoverManageSipSwpListingIfNeeded();
            openSipTabIfNeeded();
            openFirstVisibleSipCard();
            waitForSipDetailsPage();
            validateSipDetailsPage();

            ReportLogger.pass("SIP details page opened and validated successfully");
        } catch (Exception e) {
            throw new AssertionError("SIP details validation failed: " + cleanError(e.getMessage()), e);
        }
    }

    private void openFirstVisibleSipCard() {
        ReportLogger.step("Opening first visible SIP card");

        WebElement card = findFirstVisibleSipCardElement();

        if (card == null) {
            throw new AssertionError("No visible SIP card found to open. Values=" + collectVisibleStrings());
        }

        tapElementCenter(card);
        sleep(1800);
    }

    private WebElement findFirstVisibleSipCardElement() {
        Dimension size = driver.manage().window().getSize();
        int minY = (int) (size.getHeight() * 0.20);
        int maxY = (int) (size.getHeight() * 0.88);

        try {
            List<WebElement> elements = driver.findElements(By.xpath("//*"));
            WebElement amountFallback = null;
            WebElement scheduleFallback = null;

            for (WebElement element : elements) {
                try {
                    if (element == null || !element.isDisplayed()) {
                        continue;
                    }

                    Rectangle rect = element.getRect();
                    if (rect == null || rect.getWidth() <= 0 || rect.getHeight() <= 0) {
                        continue;
                    }

                    int centerY = rect.getY() + rect.getHeight() / 2;
                    if (centerY < minY || centerY > maxY) {
                        continue;
                    }

                    String readable = normalizeSpaces(getElementReadableText(element));

                    // Preferred: tap the exposed fund title.
                    if (isFundNameLike(readable)) {
                        ReportLogger.pass("SIP card candidate found using fund title: " + readable);
                        return element;
                    }

                    /*
                     * Flutter may expose only amount/date semantic nodes for a card.
                     * Keep the first visible amount as the primary fallback because
                     * tapping its centre still lands inside the same SIP card.
                     */
                    if (amountFallback == null && isRupeeAmountText(readable)) {
                        amountFallback = element;
                    }

                    if (scheduleFallback == null && isSipScheduleText(readable)) {
                        scheduleFallback = element;
                    }

                } catch (Exception ignored) {
                    // Ignore stale Flutter/Appium elements.
                }
            }

            if (amountFallback != null) {
                ReportLogger.pass(
                        "SIP card candidate found using visible amount fallback: "
                                + normalizeSpaces(getElementReadableText(amountFallback))
                );
                return amountFallback;
            }

            if (scheduleFallback != null) {
                ReportLogger.pass(
                        "SIP card candidate found using monthly schedule fallback: "
                                + normalizeSpaces(getElementReadableText(scheduleFallback))
                );
                return scheduleFallback;
            }

        } catch (Exception e) {
            ReportLogger.debug("SIP card element scan skipped: " + cleanError(e.getMessage()));
        }

        return null;
    }

    private void waitForSipDetailsPage() {
        for (int attempt = 1; attempt <= 12; attempt++) {
            if (isSipDetailsPageVisible()) {
                ReportLogger.pass("SIP details page is ready");
                return;
            }

            sleep(700);
        }

        throw new AssertionError("SIP details page did not load");
    }

    
    private boolean isSipDetailsPageVisible() {
        List<String> values = collectVisibleStrings();

        boolean hasFund = false;
        boolean hasCoreSipDetail = false;
        boolean hasViewPastLink = false;
        boolean hasSipActionButton = false;
        boolean hasTransactionHistorySignal = false;
        boolean hasTransactionDetailSignal = false;

        for (String value : values) {
            String clean = normalizeSpaces(value);
            String lower = clean.toLowerCase();

            if (lower.contains("transaction history")
                    || lower.equals("your orders")
                    || lower.equals("all transactions")
                    || lower.equals("funds, tab 1 of 7")
                    || lower.contains("update transactions")
                    || lower.equals("sort")
                    || lower.equals("filters")) {
                hasTransactionHistorySignal = true;
            }

            if (lower.equals("edit")
                    || lower.equals("delete")
                    || lower.contains("price per unit")
                    || lower.contains("no. of units")
                    || lower.contains("balance units")
                    || lower.contains("transaction source")
                    || lower.contains("sip investment")) {
                hasTransactionDetailSignal = true;
            }

            if (isFundNameLike(clean)) {
                hasFund = true;
            }

            if (lower.equals("folio no.")
                    || lower.equals("folio")
                    || lower.equals("amount")
                    || lower.equals("frequency")
                    || lower.contains("no of instalments")
                    || lower.contains("no of installments")) {
                hasCoreSipDetail = true;
            }

            if (lower.contains("view past")
                    && (lower.contains("instalment")
                    || lower.contains("installment")
                    || lower.contains("investment"))) {
                hasViewPastLink = true;
            }

            if (lower.equals("invest more")
                    || lower.equals("cancel sip")
                    || lower.equals("cancel sip, button")) {
                hasSipActionButton = true;
            }
        }

        if (hasTransactionHistorySignal || hasTransactionDetailSignal) {
            return false;
        }

        return hasFund && hasCoreSipDetail && hasViewPastLink && hasSipActionButton;
    }

    public void validateSipDetailsPage() {
        ReportLogger.step("Validating SIP details page with strict live value mapping");

        /*
         * Use the same strict mapping used by MSS_011 so MSS_005 does not
         * accidentally map Investor as Folio or Amount as Frequency.
         */
        validateStrictSipDetailsFieldMappingOnCurrentScreen();

        ReportLogger.pass("SIP details page validated with strict live values");
    }


    private void validateDetailsSignal(List<String> values, String[] labels, String logLabel) {
        for (String label : labels) {
            for (int i = 0; i < values.size(); i++) {
                String clean = normalizeSpaces(values.get(i));

                if (clean.equalsIgnoreCase(label) || clean.toLowerCase().contains(label.toLowerCase())) {
                    String value = findNearbyDetailsValue(values, i);
                    logValidatedText(logLabel, clean + (value.isEmpty() ? "" : " = " + value));
                    return;
                }
            }
        }

        throw new AssertionError(logLabel + " not found. Expected labels=" + java.util.Arrays.toString(labels)
                + " | values=" + values);
    }

    private String findNearbyDetailsValue(List<String> values, int labelIndex) {
        int end = Math.min(values.size() - 1, labelIndex + 5);

        for (int i = labelIndex + 1; i <= end; i++) {
            String clean = normalizeSpaces(values.get(i));

            if (clean.isEmpty()) {
                continue;
            }

            if (isKnownSipDetailLabel(clean)) {
                continue;
            }

            return clean;
        }

        return "";
    }

    private boolean isKnownSipDetailLabel(String value) {
        String lower = normalizeSpaces(value).toLowerCase();

        return lower.equals("folio")
                || lower.equals("folio no.")
                || lower.equals("amount")
                || lower.equals("frequency")
                || lower.contains("instalment")
                || lower.contains("installment")
                || lower.equals("view past investments")
                || lower.equals("view past instalments")
                || lower.equals("view past installments")
                || lower.equals("invest more")
                || lower.equals("cancel sip");
    }

    private void validateViewPastInstalmentsLinkVisible() {
        String matched = findViewPastInstalmentsTextOnCurrentScreen();

        if (!matched.isEmpty()) {
            logValidatedText("View past instalments link", matched);
            return;
        }

        throw new AssertionError("View past instalments/investments link not visible"
                + " | expectedAny=[View past instalments, View past installments, View past investments]"
                + " | values=" + collectVisibleStrings());
    }

    private boolean tapViewPastInstalmentsLink() {
        String[] labels = new String[]{
                "View past instalments",
                "View past installments",
                "View past investments"
        };

        for (String label : labels) {
            if (tapIfVisible(byDesc(label), label)) {
                ReportLogger.pass("Tapped: " + label);
                return true;
            }
        }

        for (String label : labels) {
            WebElement viewPast = findVisibleTextElement(label);
            if (viewPast != null) {
                tapElementCenter(viewPast);
                ReportLogger.pass("Tapped visible link: " + label);
                return true;
            }
        }

        /*
         * Future-safe fallback: App copy may change between
         * instalments/installments/investments. Only tap an element that contains
         * both "View past" and one of the valid SIP history keywords.
         */
        try {
            List<WebElement> elements = driver.findElements(By.xpath("//*"));

            for (WebElement element : elements) {
                try {
                    if (element == null || !element.isDisplayed()) {
                        continue;
                    }

                    String readable = normalizeSpaces(getElementReadableText(element));
                    String lower = readable.toLowerCase();

                    if (lower.contains("view past")
                            && (lower.contains("instalment")
                            || lower.contains("installment")
                            || lower.contains("investment"))) {
                        tapElementCenter(element);
                        ReportLogger.pass("Tapped visible View past SIP history link: " + readable);
                        return true;
                    }

                } catch (Exception ignored) {
                    // Continue scanning fresh visible elements.
                }
            }
        } catch (Exception e) {
            ReportLogger.debug("View past SIP history scan skipped: " + cleanError(e.getMessage()));
        }

        return false;
    }

    private String findViewPastInstalmentsTextOnCurrentScreen() {
        String[] labels = new String[]{
                "View past instalments",
                "View past installments",
                "View past investments"
        };

        List<String> values = collectVisibleStrings();

        for (String label : labels) {
            String expected = normalizeSpaces(label).toLowerCase();

            for (String value : values) {
                String clean = normalizeSpaces(value);
                String lower = clean.toLowerCase();

                if (lower.equals(expected) || lower.contains(expected)) {
                    return clean;
                }
            }
        }

        for (String value : values) {
            String clean = normalizeSpaces(value);
            String lower = clean.toLowerCase();

            if (lower.contains("view past")
                    && (lower.contains("instalment")
                    || lower.contains("installment")
                    || lower.contains("investment"))) {
                return clean;
            }
        }

        return "";
    }

    // =========================================================
    // MSS_006 - VIEW PAST INVESTMENTS
    // =========================================================

    public void openPastInvestmentsAndValidateTransactionHistory() {
        try {
            ReportLogger.step("Opening View past investments and validating Transaction History");

            recoverSipDetailsPageIfNeeded();

            if (!tapViewPastInstalmentsLink()) {
                throw new AssertionError("View past instalments/investments link not found on SIP details page"
                        + " | values=" + collectVisibleStrings());
            }

            sleep(2200);
            waitForTransactionHistoryPage();
            validateTransactionHistoryPage();

            ReportLogger.pass("Transaction History page opened and validated successfully");
        } catch (Exception e) {
            throw new AssertionError("View past instalments validation failed: " + cleanError(e.getMessage()), e);
        }
    }

    private void waitForTransactionHistoryPage() {
        for (int attempt = 1; attempt <= 12; attempt++) {
            if (isTransactionHistoryPageVisible()) {
                ReportLogger.pass("Transaction History page is ready");
                return;
            }

            sleep(700);
        }

        throw new AssertionError("Transaction History page did not load");
    }

    private boolean isTransactionHistoryPageVisible() {
        return pageHasText("Transaction History")
                || (pageHasText("Your Orders") && pageHasText("All Transactions"))
                || (pageHasText("Funds") && pageHasText("Stocks & ETFs") && pageHasText("NPS"));
    }

    private void validateTransactionHistoryPage() {
        assertTextVisibleFlexible("Transaction History", "Transaction History title");
        assertTextVisibleFlexible("Your Orders", "Your Orders tab");
        assertTextVisibleFlexible("All Transactions", "All Transactions tab");
        assertTextVisibleFlexible("Funds", "Funds filter tab");

        logOptionalText("Stocks & ETFs", "Stocks & ETFs filter tab");
        logOptionalText("NPS", "NPS filter tab");
        logOptionalText("Bonds & FDs", "Bonds & FDs filter tab");

        ReportLogger.pass("Transaction History page validated successfully");
    }

    // =========================================================
    // MSS_007 - RETURN TO SIP DETAILS
    // =========================================================

    public void returnBackToSipDetails() {
        try {
            ReportLogger.step("Returning back to SIP details page");

            if (isSipDetailsPageVisible()) {
                ReportLogger.pass("Already on SIP details page");
                return;
            }

            if (!isTransactionHistoryPageVisible()) {
                ReportLogger.debug("Transaction History is not visible before back. Current values=" + collectVisibleStrings());
            }

            pressBackSilently();
            sleep(1600);

            if (isSipDetailsPageVisible()) {
                ReportLogger.pass("Returned back to SIP details page");
                return;
            }

            throw new AssertionError("Did not return to SIP details page after back. Values=" + collectVisibleStrings());
        } catch (Exception e) {
            throw new AssertionError("Back to SIP details failed: " + cleanError(e.getMessage()), e);
        }
    }

    // =========================================================
    // MSS_008 - RETURN TO LISTING
    // =========================================================

    public void returnBackToSipSwpListing() {
        try {
            ReportLogger.step("Returning back to Your SIPs/SWPs listing page");

            if (isManageSipSwpListingPageVisible()) {
                ReportLogger.pass("Already on Your SIPs/SWPs listing page");
                return;
            }

            for (int attempt = 1; attempt <= 3; attempt++) {
                pressBackSilently();
                sleep(1400);

                if (isManageSipSwpListingPageVisible()) {
                    ReportLogger.pass("Returned back to Your SIPs/SWPs listing page");
                    return;
                }

                if (isDashboardOrHomeVisible()) {
                    openManageSipSwpFromHub();
                    ReportLogger.pass("Reopened Manage SIP/SWP listing after reaching dashboard");
                    return;
                }
            }

            throw new AssertionError("Unable to return to Your SIPs/SWPs listing page. Values=" + collectVisibleStrings());
        } catch (Exception e) {
            throw new AssertionError("Back to Manage SIP/SWP listing failed: " + cleanError(e.getMessage()), e);
        }
    }

    // =========================================================
    // MSS_009 - SWP TAB
    // =========================================================

    public void verifySwpTabSwitch() {
        try {
            ReportLogger.step("Validating SWP tab switch");

            recoverManageSipSwpListingIfNeeded();

            if (!tapIfVisible(byDesc(SWP_TAB), "SWP tab")) {
                WebElement swpTab = findVisibleTextElement(SWP_TAB);
                if (swpTab == null) {
                    throw new AssertionError("SWP tab not found on listing page");
                }
                tapElementCenter(swpTab);
            }

            sleep(1200);

            assertTextVisibleFlexible(SWP_TAB, "SWP selected/visible tab");

            List<String> values = collectVisibleStrings();
            boolean hasSwpEvidence = false;

            for (String value : values) {
                String lower = normalizeSpaces(value).toLowerCase();

                if (lower.contains("swp")
                        || lower.contains("withdraw")
                        || lower.contains("no swp")
                        || lower.contains("no active")) {
                    hasSwpEvidence = true;
                    logValidatedText("SWP tab visible content", normalizeSpaces(value));
                    break;
                }
            }

            if (!hasSwpEvidence) {
                ReportLogger.debug("SWP-specific content not exposed, but SWP tab is visible. Values=" + values);
            }

            ReportLogger.pass("SWP tab switch validated successfully");
        } catch (Exception e) {
            throw new AssertionError("SWP tab validation failed: " + cleanError(e.getMessage()), e);
        }
    }

    // =========================================================
    // MSS_010 - ACTION BUTTONS VISIBLE ONLY
    // =========================================================

    public void verifyActionButtonsVisibleOnly() {
        try {
            ReportLogger.step("Validating SIP action buttons visibility only");

            recoverSipDetailsPageIfNeeded();
            validateActionButtonsVisibleOnly();

            ReportLogger.pass("SIP action buttons are visible. No destructive action tapped.");
        } catch (Exception e) {
            throw new AssertionError("SIP action buttons visibility validation failed: " + cleanError(e.getMessage()), e);
        }
    }

    private void validateActionButtonsVisibleOnly() {
        ReportLogger.step("Validating action buttons visible only. Cancel SIP will not be tapped.");

        boolean investMoreVisible = pageHasText("Invest more") || pageHasText("Invest More") || pageHasText("Invest");
        boolean cancelSipVisible = pageHasText("Cancel SIP") || pageHasText("Cancel Sip") || pageHasText("Cancel");

        if (!investMoreVisible) {
            throw new AssertionError("Invest more button not visible on SIP details page. Values=" + collectVisibleStrings());
        }

        if (!cancelSipVisible) {
            throw new AssertionError("Cancel SIP button not visible on SIP details page. Values=" + collectVisibleStrings());
        }

        logValidatedText("SIP action button", "Invest more visible");
        logValidatedText("SIP action button", "Cancel SIP visible only - not tapped");
    }


    // =========================================================
    // MSS_011 - STRICT SIP DETAILS FIELD MAPPING
    // =========================================================

    public void verifyStrictSipDetailsFieldMapping() {
        try {
            ReportLogger.step("Validating strict SIP details field mapping");

            recoverSipDetailsPageIfNeeded();
            validateStrictSipDetailsFieldMappingOnCurrentScreen();

            ReportLogger.pass("Strict SIP details field mapping validated successfully");
        } catch (Exception e) {
            throw new AssertionError("Strict SIP details field mapping failed: " + cleanError(e.getMessage()), e);
        }
    }

    private void validateStrictSipDetailsFieldMappingOnCurrentScreen() {
        List<String> values = collectVisibleStrings();

        String fundName = findFirstFundName(values);
        if (fundName.isEmpty()) {
            throw new AssertionError("SIP details fund name not found. Values=" + values);
        }
        logValidatedText("Strict SIP details fund", fundName);

        String investor = findDetailValueAfterLabels(
                values,
                new String[]{"Investor"},
                this::isInvestorValue,
                "Investor"
        );
        logValidatedText("Strict SIP details Investor", investor);

        String folio = findDetailValueAfterLabels(
                values,
                new String[]{"Folio No.", "Folio", "Folio Number"},
                this::isFolioNumber,
                "Folio No."
        );
        logValidatedText("Strict SIP details Folio No.", folio);

        String amount = findDetailValueAfterLabels(
                values,
                new String[]{"Amount"},
                this::isRupeeAmountText,
                "Amount"
        );
        logValidatedText("Strict SIP details Amount", amount);

        String frequency = findDetailValueAfterLabels(
                values,
                new String[]{"Frequency"},
                this::isFrequencyValue,
                "Frequency"
        );
        logValidatedText("Strict SIP details Frequency", frequency);

        String instalments = findDetailValueAfterLabels(
                values,
                new String[]{"No of instalments", "No of installments", "Instalments", "Installments"},
                this::isInstalmentProgress,
                "No of instalments"
        );
        logValidatedText("Strict SIP details Instalments", instalments);

        validateViewPastInstalmentsLinkVisible();
        validateActionButtonsVisibleOnly();
    }

    // =========================================================
    // MSS_012 - PAST INSTALMENTS LIST AND APPLIED FILTER
    // =========================================================

    public void verifyPastInstalmentsListAndAppliedFilter() {
        try {
            ReportLogger.step("Validating past instalments list, applied filter and transaction row");

            recoverSipDetailsPageIfNeeded();

            if (!isTransactionHistoryPageVisible()) {
                if (!tapViewPastInstalmentsLink()) {
                    throw new AssertionError("View past instalments/investments link not found before list validation"
                            + " | values=" + collectVisibleStrings());
                }

                sleep(2200);
                waitForTransactionHistoryPage();
            }

            validateTransactionHistoryPage();
            validateTransactionHistoryControlsAndAppliedFilter();
            validateFirstVisiblePastInstalmentRow();

            ReportLogger.pass("Past instalments list and applied filter validated successfully");
        } catch (Exception e) {
            throw new AssertionError("Past instalments list/filter validation failed: " + cleanError(e.getMessage()), e);
        }
    }

    private void validateTransactionHistoryControlsAndAppliedFilter() {
        List<String> values = collectVisibleStrings();

        logOptionalText(DEFAULT_INVESTOR, "Transaction History investor dropdown/value");
        logOptionalText("Update Transactions", "Update Transactions action");
        logOptionalText("Sort", "Sort action");
        logOptionalText("Filters", "Filters action");

        boolean hasSearchEvidence = pageHasText("Search") || isVisible(byDescContains("Search"));
        if (hasSearchEvidence) {
            logValidatedText("Transaction History search", "Search icon/control visible");
        } else {
            ReportLogger.debug("Search icon/control not exposed as text/content-desc. Skipping optional search validation.");
        }

        String fundFilter = findFirstFundName(values);
        if (!fundFilter.isEmpty()) {
            logValidatedText("Applied SIP fund filter", fundFilter);
        } else {
            ReportLogger.debug("Applied SIP fund filter not exposed clearly. Values=" + values);
        }

        String folioFilter = findFirstVisibleFolioNumber(values);
        if (!folioFilter.isEmpty()) {
            logValidatedText("Applied folio filter", folioFilter);
        } else {
            ReportLogger.debug("Applied folio filter not exposed clearly. Values=" + values);
        }
    }

    private void validateFirstVisiblePastInstalmentRow() {
        List<String> values = collectVisibleStrings();

        String dateSource = findFirstMatchingValue(values, this::isDateLikeText);
        String date = extractFirstDateFromText(dateSource);
        if (date.isEmpty()) {
            throw new AssertionError("Transaction date not found in past instalments list. Values=" + values);
        }
        logValidatedText("Past instalment row date", date);

        String amountSource = findFirstMatchingValue(values, this::isRupeeAmountText);
        String amount = extractFirstRupeeAmountFromText(amountSource);
        if (amount.isEmpty()) {
            throw new AssertionError("Transaction amount not found in past instalments list. Values=" + values);
        }
        logValidatedText("Past instalment row amount", amount);

        boolean importedVisible = pageHasText("Imported");
        if (importedVisible) {
            logValidatedText("Past instalment row source", "Imported");
        } else {
            ReportLogger.debug("Imported source not visible on first past instalment list. Values=" + values);
        }

        boolean sipInvestmentVisible = pageHasText("SIP Investment") || pageHasText("Investment");
        if (!sipInvestmentVisible) {
            throw new AssertionError("SIP Investment transaction type not found in past instalments list. Values=" + values);
        }
        logValidatedText("Past instalment row type", "SIP Investment");

        String fundName = findFirstPastInstalmentFundName(values);
        if (!fundName.isEmpty()) {
            logValidatedText("Past instalment row fund", fundName);
        } else {
            ReportLogger.debug("Past instalment row fund not exposed separately. Values=" + values);
        }
    }


    // =========================================================
    // MSS_013 - OPEN FIRST PAST INSTALMENT TRANSACTION DETAILS
    // =========================================================

    public void openFirstPastInstalmentAndValidateTransactionDetails() {
        try {
            ReportLogger.step("Opening first past instalment and validating transaction details");

            recoverTransactionHistoryPageIfNeeded();

            WebElement row = findFirstVisibleTransactionRowElement();
            if (row == null) {
                throw new AssertionError("No visible past instalment transaction row found. Values=" + collectVisibleStrings());
            }

            tapElementCenter(row);
            sleep(1800);
            waitForTransactionDetailPage();
            validateTransactionDetailPage();

            ReportLogger.pass("First past instalment transaction details validated successfully");
        } catch (Exception e) {
            throw new AssertionError("Past instalment transaction detail validation failed: " + cleanError(e.getMessage()), e);
        }
    }

    private void waitForTransactionDetailPage() {
        for (int attempt = 1; attempt <= 10; attempt++) {
            if (isTransactionDetailPageVisible()) {
                ReportLogger.pass("Transaction detail page is ready");
                return;
            }

            sleep(700);
        }

        throw new AssertionError("Transaction detail page did not load. Values=" + collectVisibleStrings());
    }

    private void validateTransactionDetailPage() {
        List<String> values = collectVisibleStringsWithOptionalSmallScroll();

        String amountSource = findFirstMatchingValue(values, this::isRupeeAmountText);
        String amount = extractFirstRupeeAmountFromText(amountSource);
        if (amount.isEmpty()) {
            throw new AssertionError("Transaction detail amount not found. Values=" + values);
        }
        logValidatedText("Transaction detail amount", amount);

        if (containsAnyValue(values, new String[]{"SIP Investment", "Investment"})) {
            logValidatedText("Transaction detail type", findFirstContainsValue(values, new String[]{"SIP Investment", "Investment"}));
        } else {
            throw new AssertionError("Transaction detail type not found. Values=" + values);
        }

        String dateSource = findFirstMatchingValue(values, this::isDateLikeText);
        String date = extractFirstDateFromText(dateSource);
        if (date.isEmpty()) {
            throw new AssertionError("Transaction detail date not found. Values=" + values);
        }
        logValidatedText("Transaction detail date", date);

        String investor = findFirstMatchingValue(values, this::isInvestorValue);
        if (!investor.isEmpty()) {
            logValidatedText("Transaction detail investor", investor);
        } else {
            ReportLogger.debug("Investor value not exposed on transaction detail. Values=" + values);
        }

        String folio = findFirstVisibleFolioNumber(values);
        if (!folio.isEmpty()) {
            logValidatedText("Transaction detail folio", folio);
        } else {
            ReportLogger.debug("Folio number not exposed on transaction detail. Values=" + values);
        }

        if (containsAnyValue(values, new String[]{"Price per unit", "Price", "NAV"})) {
            logValidatedText("Transaction detail price per unit label", findFirstContainsValue(values, new String[]{"Price per unit", "Price", "NAV"}));
        } else {
            ReportLogger.debug("Price per unit/NAV label not visible. Values=" + values);
        }

        String units = findUnitsValueAfterUnitsLabel(values);
        if (!units.isEmpty()) {
            logValidatedText("Transaction detail units", units);
        } else {
            ReportLogger.debug("Units value not clearly exposed. Values=" + values);
        }

        if (containsAnyValue(values, new String[]{"Imported", "Manual", "External"})) {
            logValidatedText("Transaction detail source", findFirstContainsValue(values, new String[]{"Imported", "Manual", "External"}));
        } else {
            ReportLogger.debug("Transaction source not clearly visible. Values=" + values);
        }

        validateTransactionDetailActionButtonsVisibleOnly();
    }


    // =========================================================
    // MSS_014 - TRANSACTION DETAIL ACTION BUTTONS VISIBLE ONLY
    // =========================================================

    public void verifyTransactionDetailActionButtonsVisibleOnly() {
        try {
            ReportLogger.step("Validating transaction detail Edit/Delete buttons visible only");

            recoverTransactionDetailPageIfNeeded();
            validateTransactionDetailActionButtonsVisibleOnly();

            ReportLogger.pass("Transaction detail action buttons are visible. Edit/Delete were not tapped.");
        } catch (Exception e) {
            throw new AssertionError("Transaction detail action button validation failed: " + cleanError(e.getMessage()), e);
        }
    }

    private void validateTransactionDetailActionButtonsVisibleOnly() {
        List<String> values = collectVisibleStrings();

        boolean editVisible = pageHasText("Edit");
        boolean deleteVisible = pageHasText("Delete");

        if (!editVisible) {
            throw new AssertionError("Edit button not visible on transaction detail page. Values=" + values);
        }

        if (!deleteVisible) {
            throw new AssertionError("Delete button not visible on transaction detail page. Values=" + values);
        }

        logValidatedText("Transaction detail action button", "Edit visible only - not tapped");
        logValidatedText("Transaction detail action button", "Delete visible only - not tapped");
    }

    // =========================================================
    // MSS_015 - RETURN FROM TRANSACTION DETAIL TO SIP DETAILS
    // =========================================================

    
    public void returnBackFromTransactionDetailToSipDetails() {
        try {
            ReportLogger.step("Returning from transaction detail/history to real SIP details page");

            for (int attempt = 1; attempt <= 6; attempt++) {
                if (isSipDetailsPageVisible()) {
                    ReportLogger.pass("Real SIP details page is visible after returning from transaction detail/history");
                    return;
                }

                if (isTransactionDetailPageVisible()) {
                    ReportLogger.step("Currently on transaction detail. Pressing back to Transaction History | attempt=" + attempt);
                } else if (isTransactionHistoryPageVisible()) {
                    ReportLogger.step("Currently on Transaction History. Pressing back to SIP details | attempt=" + attempt);
                } else {
                    ReportLogger.step("Current screen is not SIP details yet. Pressing back safely | attempt=" + attempt);
                }

                pressBackSilently();
                sleep(1200);
            }

            if (isSipDetailsPageVisible()) {
                ReportLogger.pass("Returned to real SIP details page from transaction detail/history");
                return;
            }

            throw new AssertionError("Unable to return from transaction detail/history to real SIP details. Values="
                    + collectVisibleStrings());
        } catch (Exception e) {
            throw new AssertionError("Return from transaction detail/history to SIP details failed: "
                    + cleanError(e.getMessage()), e);
        }
    }


    // =========================================================
    // MSS_016 - TRANSACTION HISTORY SORT BOTTOM SHEET
    // =========================================================

    public void verifyTransactionHistorySortBottomSheet() {
        try {
            ReportLogger.step("Validating Transaction History Sort bottom sheet");

            recoverTransactionHistoryPageIfNeeded();
            openTransactionHistorySortUi();
            validateTransactionHistorySortUi();
            closeTransactionHistoryOverlayAndVerifyHistory("Sort bottom sheet");

            ReportLogger.pass("Transaction History Sort bottom sheet validated successfully");
        } catch (Exception e) {
            throw new AssertionError("Transaction History Sort bottom sheet validation failed: " + cleanError(e.getMessage()), e);
        }
    }

    private void openTransactionHistorySortUi() {
        ReportLogger.step("Opening Sort control from Transaction History");

        recoverTransactionHistoryPageIfNeeded();

        if (!tapTransactionHistoryControl(new String[]{"Sort", "Sort By", "Sort by"}, "Sort")) {
            throw new AssertionError("Sort control not found on Transaction History page. Values=" + collectVisibleStrings());
        }

        waitForSortUi();
    }

    private void waitForSortUi() {
        for (int attempt = 1; attempt <= 8; attempt++) {
            if (isTransactionHistorySortUiVisible()) {
                ReportLogger.pass("Transaction History Sort UI is visible");
                return;
            }

            sleep(600);
        }

        throw new AssertionError("Sort UI did not open. Values=" + collectVisibleStrings());
    }

    
    private boolean isTransactionHistorySortUiVisible() {
        List<String> values = collectVisibleStrings();

        boolean hasSortHeader = containsAnyValue(values, new String[]{
                "Sort by",
                "Sort By",
                "Sort transactions",
                "Sort Transactions"
        });

        boolean hasSortOption = containsAnyValue(values, new String[]{
                "Date",
                "Amount",
                "Newest",
                "Oldest",
                "Latest",
                "Date - Newest",
                "Date - Oldest",
                "Amount - High",
                "Amount - Low",
                "High to Low",
                "Low to High"
        });

        boolean hasAction = containsAnyValue(values, new String[]{
                "Done",
                "Apply",
                "Cancel",
                "Reset"
        });

        return hasSortHeader || (hasSortOption && hasAction);
    }
    
    private void validateTransactionHistorySortUi() {
        List<String> values = collectVisibleStrings();

        String sortHeader = findFirstContainsValue(values, new String[]{
                "Sort by",
                "Sort By",
                "Sort transactions",
                "Sort Transactions"
        });

        if (!sortHeader.isEmpty()) {
            logValidatedText("Transaction History Sort header", sortHeader);
        } else {
            throw new AssertionError("Sort header is not visible. Values=" + values);
        }

        String sortOption = findFirstExactOrContainsValue(values, new String[]{
                "Date",
                "Amount",
                "Newest",
                "Oldest",
                "Latest",
                "Date - Newest",
                "Date - Oldest",
                "Amount - High",
                "Amount - Low",
                "High to Low",
                "Low to High"
        });

        if (!sortOption.isEmpty()) {
            logValidatedText("Transaction History Sort option", sortOption);
        } else {
            throw new AssertionError("No real Sort option is visible/exposed. Expected Date/Amount/Newest/Oldest option. Values=" + values);
        }

        String action = findFirstContainsValue(values, new String[]{
                "Done",
                "Apply",
                "Cancel",
                "Reset"
        });

        if (!action.isEmpty()) {
            logValidatedText("Transaction History Sort action", action);
        } else {
            throw new AssertionError("Sort action button is not visible. Values=" + values);
        }
    }
    // =========================================================
    // MSS_017 - TRANSACTION HISTORY FILTERS SCREEN
    // =========================================================

    public void verifyTransactionHistoryFiltersScreen() {
        try {
            ReportLogger.step("Validating Transaction History Filters/Add Filter screen");

            recoverTransactionHistoryPageIfNeeded();
            openTransactionHistoryFiltersScreen();
            validateTransactionHistoryFiltersScreen();
            returnToTransactionHistoryFromSecondaryScreen("Filters screen");

            ReportLogger.pass("Transaction History Filters screen validated successfully");
        } catch (Exception e) {
            throw new AssertionError("Transaction History Filters screen validation failed: " + cleanError(e.getMessage()), e);
        }
    }

    private void openTransactionHistoryFiltersScreen() {
        ReportLogger.step("Opening Filters control from Transaction History");

        recoverTransactionHistoryPageIfNeeded();

        if (!tapTransactionHistoryControl(new String[]{"Filters", "Filter", "Add Filter", "Add Filters"}, "Filters")) {
            throw new AssertionError("Filters control not found on Transaction History page. Values=" + collectVisibleStrings());
        }

        waitForFiltersScreen();
    }

    private void waitForFiltersScreen() {
        for (int attempt = 1; attempt <= 10; attempt++) {
            if (isTransactionHistoryFiltersScreenVisible()) {
                ReportLogger.pass("Transaction History Filters/Add Filter screen is visible");
                return;
            }

            sleep(700);
        }

        throw new AssertionError("Filters/Add Filter screen did not open. Values=" + collectVisibleStrings());
    }

    
    private boolean isTransactionHistoryFiltersScreenVisible() {
        List<String> values = collectVisibleStrings();

        boolean hasStrongFilterTitle = containsAnyValue(values, new String[]{
                "Add Filter",
                "Filter Transactions",
                "Transaction Filters"
        });

        boolean hasGenericFilterTitle = containsAnyValue(values, new String[]{"Filters", "Filter"});
        boolean hasFilterCategory = !findFirstFilterCategoryText(values).isEmpty();
        boolean hasFilterAction = containsAnyValue(values, new String[]{
                "Apply Filters",
                "Apply",
                "Reset",
                "Clear",
                "Done"
        });

        boolean historyVisible = isTransactionHistoryPageVisible();

        /*
         * Transaction History itself contains the "Filters" control and "Funds" tab.
         * Do not treat that base page as the filter screen unless a stronger filter
         * screen title/action/category is exposed.
         */
        if (historyVisible && !hasStrongFilterTitle && !hasFilterAction && !hasFilterCategory) {
            return false;
        }

        return (hasStrongFilterTitle || hasGenericFilterTitle)
                && (hasFilterCategory || hasFilterAction || !historyVisible);
    }

    
    private void validateTransactionHistoryFiltersScreen() {
        List<String> values = collectVisibleStrings();

        String title = findFirstContainsValue(values, new String[]{
                "Add Filter",
                "Filters",
                "Filter Transactions",
                "Transaction Filters"
        });
        if (title.isEmpty()) {
            throw new AssertionError("Filters/Add Filter title not visible. Values=" + values);
        }
        logValidatedText("Transaction History Filters title", title);

        String category = findFirstFilterCategoryText(values);
        if (!category.isEmpty()) {
            logValidatedText("Transaction History Filters category", category);
        } else {
            throw new AssertionError("No real filter category visible. Investor dropdown text must not be counted as a category. Values="
                    + values);
        }

        String action = findFirstContainsValue(values, new String[]{"Apply Filters", "Apply", "Reset", "Clear", "Done"});
        if (!action.isEmpty()) {
            logValidatedText("Transaction History Filters action", action);
        } else {
            throw new AssertionError("Filter action button not visible. Values=" + values);
        }
    }

    // =========================================================
    // MSS_018 - UPDATE TRANSACTIONS PAGE AND RETURN
    // =========================================================

    public void verifyUpdateTransactionsPageAndReturn() {
        try {
            ReportLogger.step("Validating Update Transactions page and safe return");

            recoverTransactionHistoryPageIfNeeded();
            openUpdateTransactionsPage();
            validateUpdateTransactionsPage();
            returnToTransactionHistoryFromSecondaryScreen("Update Transactions page");

            ReportLogger.pass("Update Transactions page validated successfully without starting import/update action");
        } catch (Exception e) {
            throw new AssertionError("Update Transactions page validation failed: " + cleanError(e.getMessage()), e);
        }
    }

    private void openUpdateTransactionsPage() {
        ReportLogger.step("Opening Update Transactions from Transaction History");

        recoverTransactionHistoryPageIfNeeded();

        if (!tapTransactionHistoryControl(new String[]{"Update Transactions", "Update Transaction", "Update"}, "Update Transactions")) {
            throw new AssertionError("Update Transactions control not found. Values=" + collectVisibleStrings());
        }

        waitForUpdateTransactionsPage();
    }

    private void waitForUpdateTransactionsPage() {
        for (int attempt = 1; attempt <= 12; attempt++) {
            if (isUpdateTransactionsPageVisible()) {
                ReportLogger.pass("Update Transactions page is visible");
                return;
            }

            sleep(700);
        }

        throw new AssertionError("Update Transactions page did not open. Values=" + collectVisibleStrings());
    }

    private boolean isUpdateTransactionsPageVisible() {
        List<String> values = collectVisibleStrings();

        boolean hasUpdateTitle = containsAnyValue(values, new String[]{
                "Update Transactions",
                "Update Transaction",
                "Import Transactions",
                "Import transaction",
                "Add Transactions"
        });

        boolean hasStrongUpdatePageContent = containsAnyValue(values, new String[]{
                "Import your",
                "Import from",
                "Import via",
                "CAS",
                "Consolidated Account Statement",
                "Email",
                "Upload",
                "Statement",
                "Auto-read",
                "Fetch",
                "Manual Entry",
                "Add manually"
        });

        /*
         * Transaction History itself has an "Update Transactions" action and rows
         * with source "Imported". Do not mistake that page for the update/import
         * page unless stronger update-page content is present.
         */
        if (isTransactionHistoryPageVisible() && !hasStrongUpdatePageContent) {
            return false;
        }

        return hasUpdateTitle || hasStrongUpdatePageContent;
    }

    private void validateUpdateTransactionsPage() {
        List<String> values = collectVisibleStrings();

        String title = findFirstContainsValue(values, new String[]{
                "Update Transactions",
                "Update Transaction",
                "Import Transactions",
                "Import transaction",
                "Add Transactions"
        });
        if (title.isEmpty()) {
            throw new AssertionError("Update Transactions title not visible. Values=" + values);
        }
        logValidatedText("Update Transactions page title", title);

        String updateOption = findFirstContainsValue(values, new String[]{
                "Import",
                "CAS",
                "Consolidated Account Statement",
                "Email",
                "Upload",
                "Statement",
                "Portfolio",
                "Auto-read",
                "Fetch",
                "Manual"
        });
        if (!updateOption.isEmpty()) {
            logValidatedText("Update Transactions visible option", updateOption);
        } else {
            ReportLogger.debug("Update/import options not exposed clearly. Values=" + values);
        }
    }

    // =========================================================
    // MSS_019 - TRANSACTION HISTORY SORT APPLY
    // =========================================================

    public void verifyTransactionHistorySortApply() {
        try {
            ReportLogger.step("Validating Transaction History Sort apply flow");

            recoverTransactionHistoryPageIfNeeded();
            openTransactionHistorySortUi();
            selectSafeSortOptionIfVisible();
            tapSortDoneOrApplyIfVisible();
            waitForTransactionHistoryPage();
            validateFirstVisiblePastInstalmentRow();

            ReportLogger.pass("Transaction History Sort apply flow validated successfully");
        } catch (Exception e) {
            throw new AssertionError("Transaction History Sort apply validation failed: " + cleanError(e.getMessage()), e);
        }
    }

    
    private void selectSafeSortOptionIfVisible() {
        ReportLogger.step("Selecting first safe Sort option if exposed");

        String[] safeOptions = new String[]{
                "Date",
                "Amount",
                "Newest first",
                "Latest first",
                "Date - Newest",
                "Date Newest",
                "Oldest first",
                "Date - Oldest",
                "Date Oldest",
                "Amount - High to Low",
                "Amount High to Low",
                "Amount - Low to High",
                "Amount Low to High",
                "High to Low",
                "Low to High"
        };

        for (String option : safeOptions) {
            WebElement optionElement = findExactVisibleTextElement(option);

            if (optionElement != null) {
                tapElementCenter(optionElement);
                sleep(700);
                logValidatedText("Selected Sort option", option);
                return;
            }
        }

        ReportLogger.debug("No explicit Sort option text was exposed. Continuing to Done/Apply if available. Values="
                + collectVisibleStrings());
    }
    
    private String findFirstExactOrContainsValue(List<String> values, String[] tokens) {
        for (String token : tokens) {
            String expected = normalizeSpaces(token).toLowerCase();

            for (String value : values) {
                String clean = normalizeSpaces(value);
                String lower = clean.toLowerCase();

                if (lower.equals(expected)) {
                    return clean;
                }
            }
        }

        return findFirstContainsValue(values, tokens);
    }
    
    private WebElement findExactVisibleTextElement(String expectedText) {
        String expected = normalizeSpaces(expectedText).toLowerCase();

        try {
            List<WebElement> elements = driver.findElements(By.xpath("//*"));

            for (WebElement element : elements) {
                try {
                    if (element == null || !element.isDisplayed()) {
                        continue;
                    }

                    String readable = normalizeSpaces(getElementReadableText(element)).toLowerCase();

                    if (readable.equals(expected)) {
                        return element;
                    }

                } catch (Exception ignored) {
                    // Ignore stale elements.
                }
            }
        } catch (Exception e) {
            ReportLogger.debug("Exact visible text element scan skipped: " + cleanError(e.getMessage()));
        }

        return null;
    }
    private void tapSortDoneOrApplyIfVisible() {
        if (tapAnyVisibleTextOrDescription(new String[]{"Done", "Apply", "OK", "Ok"}, "Sort Done/Apply action")) {
            sleep(1600);
            return;
        }

        ReportLogger.debug("Sort Done/Apply action not exposed. Pressing back to close Sort UI safely.");
        pressBackSilently();
        sleep(1400);
    }

    // =========================================================
    // TRANSACTION HISTORY ADD-ON HELPERS
    // =========================================================

    private boolean tapTransactionHistoryControl(String[] labels, String controlName) {
        if (!isTransactionHistoryPageVisible()) {
            throw new AssertionError("Transaction History is not visible before tapping control: " + controlName
                    + " | values=" + collectVisibleStrings());
        }

        return tapAnyVisibleTextOrDescription(labels, "Transaction History " + controlName + " control");
    }

    private boolean tapAnyVisibleTextOrDescription(String[] labels, String labelForReport) {
        for (String label : labels) {
            if (tapIfVisible(byDesc(label), labelForReport + " | accessibilityId=" + label)) {
                sleep(900);
                return true;
            }
        }

        for (String label : labels) {
            if (tapIfVisible(byDescContains(label), labelForReport + " | descContains=" + label)) {
                sleep(900);
                return true;
            }
        }

        for (String label : labels) {
            WebElement element = findVisibleTextElement(label);
            if (element != null) {
                tapElementCenter(element);
                sleep(900);
                ReportLogger.pass("Tapped " + labelForReport + " using visible text: " + label);
                return true;
            }
        }

        return false;
    }

    private void closeTransactionHistoryOverlayAndVerifyHistory(String overlayName) {
        ReportLogger.step("Closing " + overlayName + " safely and verifying Transaction History is restored");

        if (tapAnyVisibleTextOrDescription(new String[]{"Done", "Apply", "Cancel", "Close"}, overlayName + " close/apply action")) {
            sleep(1200);
        } else {
            pressBackSilently();
            sleep(1400);
        }

        if (!isTransactionHistoryPageVisible()) {
            pressBackSilently();
            sleep(1200);
        }

        if (!isTransactionHistoryPageVisible()) {
            throw new AssertionError("Transaction History was not restored after closing " + overlayName
                    + " | values=" + collectVisibleStrings());
        }

        ReportLogger.pass("Transaction History restored after closing " + overlayName);
    }

    
    private void returnToTransactionHistoryFromSecondaryScreen(String screenName) {
        ReportLogger.step("Returning from " + screenName + " to Transaction History");

        /*
         * These screens are full pages in this flow, so the fastest and safest
         * recovery is one immediate Back, followed by a short clean-history wait.
         */
        pressBackSilently();
        sleep(900);

        for (int attempt = 1; attempt <= 8; attempt++) {
            if (isCleanTransactionHistoryPageVisible()) {
                ReportLogger.pass("Transaction History is visible after returning from " + screenName);
                return;
            }

            sleep(500);
        }

        /*
         * One extra Back handles Android/Flutter cases where the first Back closes
         * keyboard/focus or an intermediate sheet instead of the page.
         */
        pressBackSilently();
        sleep(900);

        for (int attempt = 1; attempt <= 6; attempt++) {
            if (isCleanTransactionHistoryPageVisible()) {
                ReportLogger.pass("Transaction History is visible after returning from " + screenName);
                return;
            }

            sleep(500);
        }

        throw new AssertionError("Unable to return to clean Transaction History from " + screenName
                + " | values=" + collectVisibleStrings());
    }


    private void recoverTransactionHistoryPageIfNeeded() {
        if (isTransactionHistoryPageVisible() && !isTransactionDetailPageVisible()) {
            ReportLogger.pass("Transaction History page is already visible");
            return;
        }

        if (isTransactionDetailPageVisible()) {
            pressBackSilently();
            sleep(1400);

            if (isTransactionHistoryPageVisible()) {
                ReportLogger.pass("Recovered to Transaction History from transaction detail");
                return;
            }
        }

        recoverSipDetailsPageIfNeeded();

        if (!tapViewPastInstalmentsLink()) {
            throw new AssertionError("Unable to open View past instalments while recovering Transaction History"
                    + " | values=" + collectVisibleStrings());
        }

        sleep(2200);
        waitForTransactionHistoryPage();
    }

    private void recoverTransactionDetailPageIfNeeded() {
        if (isTransactionDetailPageVisible()) {
            ReportLogger.pass("Transaction detail page is already visible");
            return;
        }

        recoverTransactionHistoryPageIfNeeded();

        WebElement row = findFirstVisibleTransactionRowElement();
        if (row == null) {
            throw new AssertionError("No transaction row found while recovering transaction detail. Values=" + collectVisibleStrings());
        }

        tapElementCenter(row);
        sleep(1800);
        waitForTransactionDetailPage();
    }

    private boolean isTransactionDetailPageVisible() {
        List<String> values = collectVisibleStrings();

        boolean hasAmount = false;
        boolean hasType = false;
        boolean hasDetailSignal = false;
        boolean hasHistoryTitle = false;

        for (String value : values) {
            String clean = normalizeSpaces(value);
            String lower = clean.toLowerCase();

            if (lower.contains("transaction history")) {
                hasHistoryTitle = true;
            }

            if (isRupeeAmountText(clean)) {
                hasAmount = true;
            }

            if (lower.contains("sip investment") || lower.equals("investment")) {
                hasType = true;
            }

            if (lower.equals("edit")
                    || lower.equals("delete")
                    || lower.contains("price per unit")
                    || lower.contains("no. of units")
                    || lower.contains("balance units")
                    || lower.contains("transaction source")) {
                hasDetailSignal = true;
            }
        }

        return !hasHistoryTitle && hasAmount && hasType && hasDetailSignal;
    }

    private WebElement findFirstVisibleTransactionRowElement() {
        Dimension size = driver.manage().window().getSize();
        int minY = (int) (size.getHeight() * 0.26);
        int maxY = (int) (size.getHeight() * 0.88);

        try {
            List<WebElement> elements = driver.findElements(By.xpath("//*"));

            for (WebElement element : elements) {
                try {
                    if (element == null || !element.isDisplayed()) {
                        continue;
                    }

                    Rectangle rect = element.getRect();
                    int centerY = rect.getY() + rect.getHeight() / 2;

                    if (centerY < minY || centerY > maxY) {
                        continue;
                    }

                    String readable = normalizeSpaces(getElementReadableText(element));
                    String lower = readable.toLowerCase();

                    if (isDateLikeText(readable)
                            || isRupeeAmountText(readable)
                            || lower.contains("sip investment")
                            || lower.contains("imported")) {
                        ReportLogger.pass("Past instalment transaction row candidate found: " + readable);
                        return element;
                    }

                } catch (Exception ignored) {
                    // Ignore stale elements.
                }
            }
        } catch (Exception e) {
            ReportLogger.debug("Transaction row scan skipped: " + cleanError(e.getMessage()));
        }

        return null;
    }

    private List<String> collectVisibleStringsWithOptionalSmallScroll() {
        LinkedHashSet<String> allValues = new LinkedHashSet<>(collectVisibleStrings());

        if (!containsAnyValue(new ArrayList<>(allValues), new String[]{"Delete", "Edit", "Price per unit", "No. of Units", "Balance Units"})) {
            smallSwipeUpW3C();
            sleep(900);
            allValues.addAll(collectVisibleStrings());
        }

        return new ArrayList<>(allValues);
    }

    private String findDetailValueAfterLabels(List<String> values, String[] labels, TextMatcher matcher, String fieldName) {
        for (String label : labels) {
            for (int i = 0; i < values.size(); i++) {
                String clean = normalizeSpaces(values.get(i));

                if (clean.equalsIgnoreCase(label) || clean.toLowerCase().contains(label.toLowerCase())) {
                    int end = Math.min(values.size() - 1, i + 10);

                    for (int j = i + 1; j <= end; j++) {
                        String candidate = normalizeSpaces(values.get(j));

                        if (candidate.isEmpty() || isKnownSipDetailLabel(candidate)) {
                            continue;
                        }

                        if (matcher.matches(candidate)) {
                            return candidate;
                        }
                    }
                }
            }
        }

        throw new AssertionError(fieldName + " value not found with strict mapping. Labels="
                + java.util.Arrays.toString(labels)
                + " | values=" + values);
    }

    private String findFirstMatchingValue(List<String> values, TextMatcher matcher) {
        for (String value : values) {
            String clean = normalizeSpaces(value);

            if (!clean.isEmpty() && matcher.matches(clean)) {
                return clean;
            }
        }

        return "";
    }

    private String findFirstContainsValue(List<String> values, String[] tokens) {
        for (String value : values) {
            String clean = normalizeSpaces(value);
            String lower = clean.toLowerCase();

            for (String token : tokens) {
                if (lower.contains(token.toLowerCase())) {
                    return clean;
                }
            }
        }

        return "";
    }

    private boolean containsAnyValue(List<String> values, String[] tokens) {
        return !findFirstContainsValue(values, tokens).isEmpty();
    }

    private String findFirstVisibleFolioNumber(List<String> values) {
        return findFirstMatchingValue(values, this::isFolioNumber);
    }

    private boolean isInvestorValue(String value) {
        String clean = normalizeSpaces(value);
        String lower = clean.toLowerCase();

        return clean.matches("[A-Za-z][A-Za-z .'-]{2,}")
                && (lower.contains("manish")
                || lower.contains("khatri")
                || lower.contains("lalit")
                || lower.contains("vinit")
                || lower.contains("sharma"));
    }

    private boolean isFolioNumber(String value) {
        String clean = normalizeSpaces(value);
        return clean.matches("[0-9]{4,}");
    }

    private boolean isFrequencyValue(String value) {
        String lower = normalizeSpaces(value).toLowerCase();

        return lower.equals("daily")
                || lower.equals("weekly")
                || lower.equals("monthly")
                || lower.equals("quarterly")
                || lower.equals("half yearly")
                || lower.equals("half-yearly")
                || lower.equals("yearly")
                || lower.equals("annually");
    }

    private boolean isInstalmentProgress(String value) {
        String clean = normalizeSpaces(value).toLowerCase();

        return clean.matches(".*[0-9]+\\s*/\\s*[0-9]+.*")
                || clean.contains("completed")
                || clean.contains("instalment")
                || clean.contains("installment");
    }

    private boolean isDateLikeText(String value) {
        String clean = normalizeSpaces(value);
        String lower = clean.toLowerCase();

        return clean.matches(".*[0-9]{1,2}\\s+[A-Za-z]{3,9},?\\s+[0-9]{4}.*")
                || clean.matches(".*[0-9]{1,2}[-/][0-9]{1,2}[-/][0-9]{2,4}.*")
                || lower.contains("january")
                || lower.contains("february")
                || lower.contains("march")
                || lower.contains("april")
                || lower.contains("may")
                || lower.contains("june")
                || lower.contains("july")
                || lower.contains("august")
                || lower.contains("september")
                || lower.contains("october")
                || lower.contains("november")
                || lower.contains("december");
    }

    private String findUnitsValueAfterUnitsLabel(List<String> values) {
        String[] labels = new String[]{
                "No. of Units",
                "No of Units",
                "Units",
                "Balance Units"
        };

        for (String label : labels) {
            for (int i = 0; i < values.size(); i++) {
                String clean = normalizeSpaces(values.get(i));

                if (clean.equalsIgnoreCase(label) || clean.toLowerCase().contains(label.toLowerCase())) {
                    int end = Math.min(values.size() - 1, i + 6);

                    for (int j = i + 1; j <= end; j++) {
                        String candidate = normalizeSpaces(values.get(j));

                        if (candidate.isEmpty() || candidate.toLowerCase().contains("unit")) {
                            continue;
                        }

                        if (isUnitsValue(candidate)) {
                            return candidate;
                        }
                    }
                }
            }
        }

        return "";
    }


    private String extractFirstRupeeAmountFromText(String value) {
        String clean = normalizeSpaces(value);
        java.util.regex.Matcher matcher = Pattern
                .compile("₹\\s?[0-9,]+(?:\\.[0-9]+)?(?:\\s*/\\s*(?:month|Month|MONTH))?")
                .matcher(clean);

        if (matcher.find()) {
            return normalizeSpaces(matcher.group());
        }

        java.util.regex.Matcher amountPerMonthMatcher = Pattern
                .compile("[0-9,]+(?:\\.[0-9]+)?\\s*/\\s*(?:month|Month|MONTH)")
                .matcher(clean);

        if (amountPerMonthMatcher.find()) {
            return normalizeSpaces(amountPerMonthMatcher.group());
        }

        return "";
    }

    private String extractFirstDateFromText(String value) {
        String clean = normalizeSpaces(value);
        java.util.regex.Matcher wordDateMatcher = Pattern
                .compile("[0-9]{1,2}\\s+[A-Za-z]{3,9},?\\s+[0-9]{4}")
                .matcher(clean);

        if (wordDateMatcher.find()) {
            return normalizeSpaces(wordDateMatcher.group());
        }

        java.util.regex.Matcher numericDateMatcher = Pattern
                .compile("[0-9]{1,2}[-/][0-9]{1,2}[-/][0-9]{2,4}")
                .matcher(clean);

        if (numericDateMatcher.find()) {
            return normalizeSpaces(numericDateMatcher.group());
        }

        return "";
    }

    private String findFirstPastInstalmentFundName(List<String> values) {
        for (String value : values) {
            String clean = normalizeSpaces(value);

            if (clean.isEmpty() || isNavigationOrFilterControlText(clean)) {
                continue;
            }

            if (isFundNameLike(clean)) {
                String extracted = extractFundNameFromCompositeTransactionText(clean);
                return extracted.isEmpty() ? clean : extracted;
            }
        }

        return "";
    }

    private String extractFundNameFromCompositeTransactionText(String value) {
        String clean = normalizeSpaces(value);

        if (clean.contains("SIP Investment")) {
            String withoutDate = clean.replaceFirst("^[0-9]{1,2}\\s+[A-Za-z]{3,9},?\\s+[0-9]{4}\\s*", "");
            withoutDate = withoutDate.replaceFirst("(?i)^Imported\\s+", "");
            String fundPart = withoutDate.replaceFirst("(?i)\\s+SIP Investment.*$", "").trim();
            if (!fundPart.isEmpty() && !isNavigationOrFilterControlText(fundPart)) {
                return fundPart;
            }
        }

        return "";
    }

    private boolean isNavigationOrFilterControlText(String value) {
        String clean = normalizeSpaces(value);
        String lower = clean.toLowerCase();

        return lower.equals("funds")
                || lower.equals("all transactions")
                || lower.equals("your orders")
                || lower.equals("stocks & etfs")
                || lower.equals("nps")
                || lower.equals("bonds & fds")
                || lower.equals("sort")
                || lower.equals("filters")
                || lower.equals("update transactions")
                || lower.contains("tab 1 of")
                || lower.contains("tab 2 of")
                || lower.contains("tab 3 of")
                || lower.contains("tab 4 of")
                || lower.contains("tab 5 of")
                || lower.contains("tab 6 of")
                || lower.contains("tab 7 of")
                || lower.matches(".*\\btab\\s+[0-9]+\\s+of\\s+[0-9]+.*");
    }

    private boolean isUnitsValue(String value) {
        String clean = normalizeSpaces(value);

        if (clean.isEmpty()) {
            return false;
        }

        if (isFolioNumber(clean) || isRupeeAmountText(clean) || isDateLikeText(clean)) {
            return false;
        }

        return clean.matches("[0-9]{1,3}(\\.[0-9]+)?")
                || clean.matches("0\\.[0-9]+");
    }


    private String findFirstSortOptionText(List<String> values) {
        String[] optionTokens = new String[]{
                "Newest first",
                "Latest first",
                "Newest to oldest",
                "Oldest to newest",
                "Oldest first",
                "Date - Newest",
                "Date Newest",
                "Date - Oldest",
                "Date Oldest",
                "Transaction date",
                "Order date",
                "Amount - High to Low",
                "Amount High to Low",
                "Amount - Low to High",
                "Amount Low to High",
                "High to Low",
                "Low to High"
        };

        for (String value : values) {
            String clean = normalizeSpaces(value);
            String lower = clean.toLowerCase();

            if (clean.isEmpty() || isSortControlText(clean)) {
                continue;
            }

            if (isDateLikeText(clean) || isRupeeAmountText(clean) || isFundNameLike(clean)) {
                continue;
            }

            for (String token : optionTokens) {
                if (lower.contains(token.toLowerCase())) {
                    return clean;
                }
            }

            if ((lower.contains("date") || lower.contains("amount"))
                    && (lower.contains("new")
                    || lower.contains("old")
                    || lower.contains("high")
                    || lower.contains("low")
                    || lower.contains("asc")
                    || lower.contains("desc"))) {
                return clean;
            }
        }

        return "";
    }

    private boolean isSortControlText(String value) {
        String lower = normalizeSpaces(value).toLowerCase();

        return lower.equals("sort")
                || lower.equals("sort by")
                || lower.equals("apply")
                || lower.equals("done")
                || lower.equals("cancel")
                || lower.equals("reset")
                || lower.contains("transaction history")
                || lower.equals("your orders")
                || lower.equals("all transactions")
                || lower.equals("funds")
                || lower.equals("stocks & etfs")
                || lower.equals("nps")
                || lower.equals("bonds & fds")
                || lower.equals("filters")
                || lower.equals("update transactions");
    }

    private String findFirstFilterCategoryText(List<String> values) {
        String[] exactCategories = new String[]{
                "Date",
                "Investor",
                "Transaction Type",
                "Transaction Source",
                "Source",
                "Folio",
                "Fund",
                "Asset",
                "Category"
        };

        for (String value : values) {
            String clean = normalizeSpaces(value);
            String lower = clean.toLowerCase();

            if (clean.isEmpty() || isNonCategoryFilterText(clean)) {
                continue;
            }

            for (String category : exactCategories) {
                if (clean.equalsIgnoreCase(category)) {
                    return clean;
                }
            }

            if (lower.contains("transaction type")
                    || lower.contains("transaction source")
                    || lower.contains("asset type")
                    || lower.contains("product type")) {
                return clean;
            }
        }

        return "";
    }

    private boolean isNonCategoryFilterText(String value) {
        String lower = normalizeSpaces(value).toLowerCase();

        return lower.contains("shows investor selection list")
                || lower.contains(DEFAULT_INVESTOR.toLowerCase())
                || lower.equals("add filter")
                || lower.equals("filters")
                || lower.equals("filter")
                || lower.equals("apply")
                || lower.equals("apply filters")
                || lower.equals("reset")
                || lower.equals("clear")
                || lower.equals("done")
                || lower.equals("transaction history")
                || lower.equals("your orders")
                || lower.equals("all transactions")
                || lower.equals("funds")
                || lower.equals("stocks & etfs")
                || lower.equals("nps")
                || lower.equals("bonds & fds")
                || lower.equals("sort")
                || lower.equals("update transactions")
                || isFundNameLike(value)
                || isRupeeAmountText(value)
                || isDateLikeText(value);
    }

    private boolean isCleanTransactionHistoryPageVisible() {
        List<String> values = collectVisibleStrings();

        boolean hasHistory = containsAnyValue(values, new String[]{"Transaction History"})
                || (containsAnyValue(values, new String[]{"Your Orders"})
                && containsAnyValue(values, new String[]{"All Transactions"}));

        if (!hasHistory) {
            return false;
        }

        boolean hasSecondaryScreenSignal = containsAnyValue(values, new String[]{
                "Add Filter",
                "Filter Transactions",
                "Transaction Filters",
                "Sort By",
                "Sort by",
                "Import your",
                "Import from",
                "Import via",
                "CAS",
                "Upload",
                "Manual Entry",
                "Add manually"
        });

        boolean hasTransactionDetailSignal = containsAnyValue(values, new String[]{
                "Price per unit",
                "No. of Units",
                "Balance Units",
                "Transaction Source",
                "Edit",
                "Delete"
        }) && !containsAnyValue(values, new String[]{"Your Orders", "All Transactions"});

        return !hasSecondaryScreenSignal && !hasTransactionDetailSignal;
    }

    private interface TextMatcher {
        boolean matches(String value);
    }

    // =========================================================
    // RECOVERY HELPERS
    // =========================================================

    private void recoverManageSipSwpListingIfNeeded() {
        if (isManageSipSwpListingPageVisible()) {
            ReportLogger.pass("Your SIPs/SWPs listing page is already visible");
            return;
        }

        if (isTransactionHistoryPageVisible()) {
            pressBackSilently();
            sleep(1400);
        }

        if (isSipDetailsPageVisible()) {
            pressBackSilently();
            sleep(1400);
        }

        if (isManageSipSwpListingPageVisible()) {
            ReportLogger.pass("Recovered to Your SIPs/SWPs listing page");
            return;
        }

        openManageSipSwpFromHub();
    }

    private void recoverSipDetailsPageIfNeeded() {
        if (isSipDetailsPageVisible()) {
            ReportLogger.pass("SIP details page is already visible");
            return;
        }

        if (isTransactionHistoryPageVisible()) {
            pressBackSilently();
            sleep(1600);

            if (isSipDetailsPageVisible()) {
                ReportLogger.pass("Recovered from Transaction History to SIP details page");
                return;
            }
        }

        recoverManageSipSwpListingIfNeeded();
        openSipTabIfNeeded();
        openFirstVisibleSipCard();
        waitForSipDetailsPage();
    }

    private void recoverToAdvisorHomeIfNeeded() {
        if (isDashboardOrHomeVisible()) {
            return;
        }

        if (isManageSipSwpListingPageVisible()) {
            return;
        }

        for (int attempt = 1; attempt <= 4; attempt++) {
            if (isDashboardOrHomeVisible()) {
                return;
            }

            pressBackSilently();
            sleep(1000);
        }
    }

    private boolean isDashboardOrHomeVisible() {
        return pageHasText("Hub")
                && (pageHasText("Funds") || pageHasText("Stocks") || pageHasText("Portfolio"));
    }

    private boolean isManageSipSwpListingPageVisible() {
        return pageHasText(PAGE_TITLE)
                || (pageHasText(SIP_TAB) && pageHasText(SWP_TAB) && pageHasText("SIPs"))
                || (pageHasText(SIP_TAB) && pageHasText(SWP_TAB) && pageHasText("SWPs"));
    }

    private void openSipTabIfNeeded() {
        if (tapIfVisible(byDesc(SIP_TAB), "SIP tab")) {
            sleep(900);
            return;
        }

        WebElement sipTab = findVisibleTextElement(SIP_TAB);
        if (sipTab != null) {
            tapElementCenter(sipTab);
            sleep(900);
            return;
        }

        throw new AssertionError("SIP tab not found on Manage SIP/SWP listing page");
    }

    // =========================================================
    // GENERIC HELPERS
    // =========================================================

    private By byDesc(String text) {
        return AppiumBy.accessibilityId(text);
    }

    private By byDescContains(String text) {
        return AppiumBy.androidUIAutomator(
                "new UiSelector().descriptionContains(\"" + escapeUiAutomatorText(text) + "\")"
        );
    }

    private String escapeUiAutomatorText(String text) {
        if (text == null) {
            return "";
        }

        return text.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private void assertTextVisibleFlexible(String text, String label) {
        if (pageHasText(text)) {
            logValidatedText(label, text);
            return;
        }

        throw new AssertionError(label + " not visible. Expected text contains: " + text
                + " | values=" + collectVisibleStrings());
    }

    private void logOptionalText(String text, String label) {
        if (pageHasText(text)) {
            logValidatedText(label, text);
        } else {
            ReportLogger.debug(label + " not visible. Skipping optional validation.");
        }
    }


    private boolean pageHasText(String expectedText) {
        String expected = normalizeSpaces(expectedText).toLowerCase();

        if (expected.isEmpty()) {
            return false;
        }

        if (isTextVisibleFast(expectedText)) {
            return true;
        }

        for (String value : collectVisibleStrings()) {
            String clean = normalizeSpaces(value).toLowerCase();

            if (clean.equals(expected) || clean.contains(expected)) {
                return true;
            }
        }

        return false;
    }


    private WebElement findVisibleTextElement(String expectedText) {
        String expected = normalizeSpaces(expectedText).toLowerCase();

        if (expected.isEmpty()) {
            return null;
        }

        By[] fastLocators = new By[]{
                byDesc(expectedText),
                byDescContains(expectedText),
                AppiumBy.androidUIAutomator("new UiSelector().text(\"" + escapeUiAutomatorText(expectedText) + "\")"),
                AppiumBy.androidUIAutomator("new UiSelector().textContains(\"" + escapeUiAutomatorText(expectedText) + "\")")
        };

        for (By locator : fastLocators) {
            WebElement element = findVisibleElement(locator);

            if (element != null) {
                return element;
            }
        }

        try {
            List<WebElement> elements = driver.findElements(By.xpath("//*"));

            for (WebElement element : elements) {
                try {
                    if (element == null || !element.isDisplayed()) {
                        continue;
                    }

                    String readable = normalizeSpaces(getElementReadableText(element)).toLowerCase();

                    if (readable.equals(expected) || readable.contains(expected)) {
                        return element;
                    }

                } catch (Exception ignored) {
                    // Ignore stale elements.
                }
            }
        } catch (Exception e) {
            ReportLogger.debug("Visible text element fallback scan skipped: " + cleanError(e.getMessage()));
        }

        return null;
    }

    private WebElement findTextElementInBottomBand(String expectedText) {
        String expected = normalizeSpaces(expectedText).toLowerCase();
        Dimension size = driver.manage().window().getSize();
        int minY = (int) (size.getHeight() * 0.78);

        try {
            List<WebElement> elements = driver.findElements(By.xpath("//*"));

            for (WebElement element : elements) {
                try {
                    if (element == null || !element.isDisplayed()) {
                        continue;
                    }

                    Rectangle rect = element.getRect();
                    int centerY = rect.getY() + rect.getHeight() / 2;

                    if (centerY < minY) {
                        continue;
                    }

                    String readable = normalizeSpaces(getElementReadableText(element)).toLowerCase();

                    if (readable.equals(expected) || readable.contains(expected)) {
                        return element;
                    }

                } catch (Exception ignored) {
                    // Ignore stale elements.
                }
            }
        } catch (Exception e) {
            ReportLogger.debug("Bottom band text scan skipped: " + cleanError(e.getMessage()));
        }

        return null;
    }

    private WebElement findVisibleElement(By locator) {
        try {
            List<WebElement> elements = driver.findElements(locator);

            for (WebElement element : elements) {
                try {
                    if (element != null && element.isDisplayed()) {
                        return element;
                    }
                } catch (Exception ignored) {
                    // Ignore stale elements.
                }
            }
        } catch (Exception ignored) {
            // Ignore locator errors.
        }

        return null;
    }

    private boolean isVisible(By locator) {
        return findVisibleElement(locator) != null;
    }

    private boolean tapIfVisible(By locator, String label) {
        WebElement element = findVisibleElement(locator);

        if (element == null) {
            return false;
        }

        tapElementCenter(element);
        ReportLogger.pass("Tapped: " + label);
        return true;
    }

    private void tapElementCenter(WebElement element) {
        Rectangle rect = element.getRect();
        int x = rect.getX() + rect.getWidth() / 2;
        int y = rect.getY() + rect.getHeight() / 2;
        tapByCoordinates(x, y);
    }


    private void tapByCoordinates(int x, int y) {
        invalidateVisibleStringsCache();

        PointerInput finger = new PointerInput(PointerInput.Kind.TOUCH, "finger");
        Sequence tap = new Sequence(finger, 1);

        tap.addAction(finger.createPointerMove(Duration.ZERO, PointerInput.Origin.viewport(), x, y));
        tap.addAction(finger.createPointerDown(PointerInput.MouseButton.LEFT.asArg()));
        tap.addAction(finger.createPointerUp(PointerInput.MouseButton.LEFT.asArg()));

        driver.perform(Collections.singletonList(tap));
    }

    private void pageSwipeUpW3C() {
        performVerticalSwipe(0.88, 0.78, 0.36, 650);
    }

    private void smallSwipeUpW3C() {
        performVerticalSwipe(0.88, 0.70, 0.52, 430);
    }


    private void performVerticalSwipe(double xRatio, double startYRatio, double endYRatio, int durationMillis) {
        invalidateVisibleStringsCache();

        Dimension size = driver.manage().window().getSize();

        int x = (int) (size.getWidth() * xRatio);
        int startY = (int) (size.getHeight() * startYRatio);
        int endY = (int) (size.getHeight() * endYRatio);

        PointerInput finger = new PointerInput(PointerInput.Kind.TOUCH, "finger");
        Sequence swipe = new Sequence(finger, 1);

        swipe.addAction(finger.createPointerMove(Duration.ZERO, PointerInput.Origin.viewport(), x, startY));
        swipe.addAction(finger.createPointerDown(PointerInput.MouseButton.LEFT.asArg()));
        swipe.addAction(finger.createPointerMove(Duration.ofMillis(durationMillis), PointerInput.Origin.viewport(), x, endY));
        swipe.addAction(finger.createPointerUp(PointerInput.MouseButton.LEFT.asArg()));

        driver.perform(Collections.singletonList(swipe));
    }


    private void pressBackSilently() {
        try {
            invalidateVisibleStringsCache();
            driver.navigate().back();
        } catch (Exception ignored) {
            // Ignore back failures.
        }
    }


    private List<String> collectVisibleStrings() {
        long now = System.currentTimeMillis();

        if (visibleStringsCache != null && (now - visibleStringsCacheAtMs) <= VISIBLE_STRINGS_CACHE_TTL_MS) {
            return new ArrayList<>(visibleStringsCache);
        }

        LinkedHashSet<String> values = new LinkedHashSet<>();

        try {
            List<WebElement> elements = driver.findElements(By.xpath("//*"));

            for (WebElement element : elements) {
                try {
                    if (element == null || !element.isDisplayed()) {
                        continue;
                    }

                    addCleanString(values, element.getText());
                    addCleanString(values, element.getAttribute("content-desc"));
                    addCleanString(values, element.getAttribute("text"));
                    addCleanString(values, element.getAttribute("name"));

                } catch (Exception ignored) {
                    // Ignore stale elements.
                }
            }
        } catch (Exception e) {
            ReportLogger.debug("Visible string collection skipped: " + cleanError(e.getMessage()));
        }

        List<String> result = new ArrayList<>(values);
        visibleStringsCache = new ArrayList<>(result);
        visibleStringsCacheAtMs = System.currentTimeMillis();

        return result;
    }



    private void invalidateVisibleStringsCache() {
        visibleStringsCache = null;
        visibleStringsCacheAtMs = 0L;
    }

    private boolean isTextVisibleFast(String expectedText) {
        String clean = normalizeSpaces(expectedText);

        if (clean.isEmpty()) {
            return false;
        }

        By[] locators = new By[]{
                byDesc(clean),
                byDescContains(clean),
                AppiumBy.androidUIAutomator("new UiSelector().text(\"" + escapeUiAutomatorText(clean) + "\")"),
                AppiumBy.androidUIAutomator("new UiSelector().textContains(\"" + escapeUiAutomatorText(clean) + "\")")
        };

        for (By locator : locators) {
            if (isVisible(locator)) {
                return true;
            }
        }

        return false;
    }

    private void addCleanString(LinkedHashSet<String> values, String rawValue) {
        if (rawValue == null) {
            return;
        }

        String clean = normalizeSpaces(rawValue);

        if (!clean.isEmpty()) {
            values.add(clean);
        }

        String prepared = rawValue.replace("\\n", "\n").replace("\r", "\n");
        String[] parts = prepared.split("\\n+|\\|");

        for (String part : parts) {
            String cleanPart = normalizeSpaces(part);

            if (!cleanPart.isEmpty()) {
                values.add(cleanPart);
            }
        }
    }

    private String getElementReadableText(WebElement element) {
        List<String> values = new ArrayList<>();

        try {
            values.add(element.getText());
        } catch (Exception ignored) {
            // Ignore.
        }

        try {
            values.add(element.getAttribute("content-desc"));
        } catch (Exception ignored) {
            // Ignore.
        }

        try {
            values.add(element.getAttribute("text"));
        } catch (Exception ignored) {
            // Ignore.
        }

        try {
            values.add(element.getAttribute("name"));
        } catch (Exception ignored) {
            // Ignore.
        }

        for (String value : values) {
            String clean = normalizeSpaces(value);
            if (!clean.isEmpty()) {
                return clean;
            }
        }

        return "";
    }

    private String normalizeSpaces(String value) {
        if (value == null) {
            return "";
        }

        return value
                .replace("\\n", " ")
                .replace("\n", " ")
                .replace("\r", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private boolean isFundNameLike(String value) {
        String clean = normalizeSpaces(value);
        String lower = clean.toLowerCase();

        if (clean.length() < 5
                || isNavigationOrFilterControlText(clean)
                || isRupeeAmountText(clean)
                || isSipScheduleText(clean)) {
            return false;
        }

        if (lower.contains("your sips")
                || lower.contains("transaction history")
                || lower.contains("choose investor")
                || lower.contains("shows investor selection list")
                || lower.contains("this list includes")
                || lower.contains("external sips may not")
                || lower.equals("go back")) {
            return false;
        }

        /*
         * Do not maintain a tiny AMC whitelist. The page is dynamic and can show
         * schemes from any AMC. Recognise common scheme/plan/category signals.
         */
        return lower.contains("direct")
                || lower.contains("growth")
                || lower.contains("regular")
                || lower.contains("dividend")
                || lower.contains("idcw")
                || lower.contains("dir-g")
                || lower.contains("dir g")
                || lower.contains("fund")
                || lower.contains("scheme")
                || lower.contains("equity")
                || lower.contains("debt")
                || lower.contains("hybrid")
                || lower.contains("liquid")
                || lower.contains("index")
                || lower.contains("elss")
                || lower.contains("arbitrage")
                || lower.contains("small cap")
                || lower.contains("mid cap")
                || lower.contains("large cap")
                || lower.contains("flexi cap")
                || lower.contains("multi cap")
                || lower.contains("multicap")
                || lower.contains("balanced advantage")
                || lower.contains("prudential")
                || lower.contains("icici")
                || lower.contains("hdfc")
                || lower.contains("nippon")
                || lower.contains("quant")
                || lower.contains("motilal")
                || lower.contains("axis")
                || lower.contains("sbi")
                || lower.contains("kotak")
                || lower.contains("mirae")
                || lower.contains("parag parikh")
                || lower.contains("bandhan")
                || lower.contains("aditya birla")
                || lower.contains("canara")
                || lower.contains("dsp")
                || lower.contains("franklin")
                || lower.contains("tata")
                || lower.contains("uti");
    }


    private String findFirstFundName(List<String> values) {
        for (String value : values) {
            String clean = normalizeSpaces(value);

            if (clean.isEmpty() || isNavigationOrFilterControlText(clean)) {
                continue;
            }

            if (isFundNameLike(clean)) {
                String extracted = extractFundNameFromCompositeTransactionText(clean);
                return extracted.isEmpty() ? clean : extracted;
            }
        }

        return "";
    }

    private boolean isRupeeAmountText(String value) {
        String clean = normalizeSpaces(value);

        return Pattern.matches(".*₹\\s?[0-9,]+(\\.\\d+)?(\\s*/\\s*(month|Month|MONTH))?.*", clean)
                || Pattern.matches(".*[0-9,]+(\\.\\d+)?\\s*/\\s*(month|Month|MONTH).*", clean);
    }


    private boolean isSipScheduleText(String value) {
        String clean = normalizeSpaces(value);

        return Pattern.matches(
                ".*\\b[0-9]{1,2}(st|nd|rd|th)\\s+of\\s+every\\s+month\\b.*",
                clean.toLowerCase()
        );
    }

    private String extractFirstSipScheduleFromText(String value) {
        String clean = normalizeSpaces(value);
        java.util.regex.Matcher matcher = Pattern
                .compile(
                        "\\b[0-9]{1,2}(?:st|nd|rd|th)\\s+of\\s+every\\s+month\\b",
                        Pattern.CASE_INSENSITIVE
                )
                .matcher(clean);

        if (matcher.find()) {
            return normalizeSpaces(matcher.group());
        }

        return "";
    }

    private void logValidatedText(String label, String value) {
        String safeLabel = label == null ? "" : normalizeSpaces(label);
        String safeValue = value == null ? "" : normalizeSpaces(value);

        if (safeValue.isEmpty()) {
            return;
        }

        ReportLogger.pass("Validated text/value - " + safeLabel + ": " + safeValue);

        /*
         * Important:
         * ReportLogger prints to terminal in this framework, but Extent report
         * does not always receive those logs. Write important validation evidence
         * directly to Extent also, same pattern used in stable modules.
         */
        try {
            if (ExtentTestManager.getTest() != null) {
                ExtentTestManager.getTest().pass(
                        "<b>Validated text/value:</b> " + safeLabel + " = " + safeValue
                );
            }
        } catch (Exception ignored) {
            // Extent test may not be initialized on some listener paths.
        }
    }


    private void sleep(long millis) {
        long adjustedMillis = millis;

        if (MSS_FAST_MODE) {
            if (millis >= 5000) {
                adjustedMillis = 1800;
            } else if (millis >= 3000) {
                adjustedMillis = 1200;
            } else if (millis >= 2200) {
                adjustedMillis = 950;
            } else if (millis >= 1800) {
                adjustedMillis = 850;
            } else if (millis >= 1400) {
                adjustedMillis = 700;
            } else if (millis >= 1000) {
                adjustedMillis = 550;
            } else if (millis >= 700) {
                adjustedMillis = 420;
            }
        }

        try {
            Thread.sleep(adjustedMillis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private String cleanError(String message) {
        if (message == null) {
            return "";
        }

        return message.replace("\n", " ").replace("\r", " ").replaceAll("\\s+", " ").trim();
    }

    private static class SipCardData {
        private final String fundName;
        private final String amount;
        private final String schedule;
        private final String tag;

        private SipCardData(String fundName, String amount, String schedule, String tag) {
            this.fundName = fundName;
            this.amount = amount;
            this.schedule = schedule;
            this.tag = tag;
        }

        @Override
        public String toString() {
            return "SipCardData{"
                    + "fundName='" + fundName + '\''
                    + ", amount='" + amount + '\''
                    + ", schedule='" + schedule + '\''
                    + ", tag='" + tag + '\''
                    + '}';
        }
    }
}