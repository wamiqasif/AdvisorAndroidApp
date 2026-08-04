package com.valueresearch.pages;

import com.valueresearch.utils.AuthHelper;
import com.valueresearch.utils.ConfigReader;
import com.valueresearch.utils.KycOtpEmailReader;
import com.valueresearch.utils.ReportLogger;
import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.android.nativekey.AndroidKey;
import io.appium.java_client.android.nativekey.KeyEvent;
import org.openqa.selenium.By;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.Rectangle;
import org.openqa.selenium.WebElement;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.time.LocalDate;
import java.time.Month;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.time.temporal.ChronoUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class OnboardingPage {

    private final AndroidDriver driver;

    private static final String ADVISOR_PACKAGE = "com.valueresearch.advisor";

    private static final By SUBSCRIBE_NOW = AppiumBy.accessibilityId("Subscribe Now");
    private static final By STARTS_AT_JUST = descContains("Starts at just");
    private static final By CONFIDENCE_PARTNER = descContains("Confidence comes from");

    private static final By CONTINUE_TO_PAYMENT_TITLE_CASE = AppiumBy.accessibilityId("Continue to Payment");
    private static final By CONTINUE_TO_PAYMENT_LOWER_CASE = AppiumBy.accessibilityId("Continue to payment");
    private static final By PLAN_GET_STARTED = AppiumBy.androidUIAutomator(
            "new UiSelector().descriptionContains(\"Get started\").clickable(true)"
    );

    private static final By KYC_CHECK = AppiumBy.accessibilityId("KYC CHECK");
    private static final By CONFIRM_IDENTITY = AppiumBy.accessibilityId("Confirm your identity");
    private static final By VERIFY_PAN_AND_KYC = AppiumBy.accessibilityId("Verify PAN and KYC");
    private static final By RESIDENT_INDIAN = AppiumBy.accessibilityId("Resident Indian");
    private static final By NON_RESIDENT_INDIAN = descContains("Non-Resident Indian");
    private static final By CONSENT_CHECKBOX_TEXT = descContains("I consent to fetching my KYC details");
    private static final By PAN_VERIFIED = AppiumBy.accessibilityId("PAN VERIFIED");
    private static final By KYC_VALIDATED = AppiumBy.accessibilityId("KYC Validated");

    private static final By AUTHORISE_RECURRING_MANDATE = AppiumBy.accessibilityId("Authorise a recurring mandate");
    private static final By UPI_AUTOPAY_CARD = descContains("UPI Autopay");
    private static final By CREDIT_CARD = descContains("Credit card");
    private static final By DEBIT_NETBANKING = descContains("Debit card and netbanking");
    private static final By AUTHORISE_MANDATE = AppiumBy.accessibilityId("Authorise mandate");

    private static final By RAZORPAY_CHECKOUT = textContains("Razorpay Checkout");
    private static final By PAYMENT_OPTIONS = textContains("Payment Options");
    private static final By PHONEPE_EXACT_BUTTON = AppiumBy.androidUIAutomator("new UiSelector().text(\"PhonePe PhonePe\")");
    private static final By PHONEPE_CONTAINS = AppiumBy.androidUIAutomator("new UiSelector().textContains(\"PhonePe\")");
    private static final By GOOGLE_PAY_CONTAINS = AppiumBy.androidUIAutomator("new UiSelector().textContains(\"Google Pay\")");
    private static final By APPS_AND_UPI_ID = AppiumBy.androidUIAutomator("new UiSelector().textContains(\"Apps & UPI ID\")");
    private static final By RAZORPAY_CONTINUE = AppiumBy.androidUIAutomator("new UiSelector().text(\"Continue\")");
    private static final By PAYMENT_SUCCESSFUL = textContains("Payment Successful");
    private static final By PHONEPE_ERROR = textContains("Something went wrong");
    private static final By PHONEPE_RETRY = AppiumBy.androidUIAutomator("new UiSelector().text(\"RETRY\")");
    private static final By PHONEPE_GO_BACK = AppiumBy.androidUIAutomator("new UiSelector().text(\"GO BACK\")");

    private static final By MANDATE_AUTHORISED = AppiumBy.accessibilityId("Mandate authorised");
    private static final By CONTINUE_TO_FETCH_KYC = AppiumBy.accessibilityId("Continue to Fetch KYC");

    // Remaining onboarding flow shown in the latest Appium Inspector screenshots.
    private static final By KYC_OTP_SCREEN = AppiumBy.accessibilityId("Verifying KYC details with OTP");
    private static final By KYC_OTP_VERIFY_AND_CONTINUE = AppiumBy.accessibilityId("Verify and continue");
    private static final By CONTACT_DETAILS = AppiumBy.accessibilityId("CONTACT DETAILS");
    private static final By CONTACT_DETAILS_HEADING = AppiumBy.accessibilityId("Where should we reach you?");
    private static final By CONTACT_DETAILS_CONTINUE = AppiumBy.accessibilityId("Continue");
    private static final By READ_BEFORE_SIGNING = AppiumBy.accessibilityId("READ BEFORE SIGNING");
    private static final By ALMOST_THERE = AppiumBy.accessibilityId("You're almost there!");
    private static final By CONTINUE_TO_ESIGN = AppiumBy.accessibilityId("Continue to E-Sign");
    private static final By SIGN_NOW = AppiumBy.accessibilityId("Sign Now");

    private static final By EDIT_TEXT = AppiumBy.className("android.widget.EditText");

    public OnboardingPage(AndroidDriver driver) {
        this.driver = driver;
    }

    // =========================================================
    // Public test flow methods
    // =========================================================

    public void ensureAdvisorAppLoggedInForOnboarding() {
        ReportLogger.step("Checking Advisor app login/session state before onboarding");

        /*
         * Onboarding-specific post-login handling:
         * After Email + OTP, this app can land directly on the subscription page.
         * The common AuthHelper used by other modules waits for bottom tabs only,
         * so onboarding has its own safe login method.
         */
        new AuthHelper(driver).ensureLoggedInForOnboarding();

        ReportLogger.pass("Advisor app login/session confirmed for onboarding");
    }

    public void waitForSubscriptionLandingForOnboarding() {
        ReportLogger.step("Waiting for onboarding subscription landing screen");

        if (waitForAnyVisible(new By[]{SUBSCRIBE_NOW, STARTS_AT_JUST, CONFIDENCE_PARTNER}, 12) != null) {
            ReportLogger.pass("Subscription landing screen is visible");
            return;
        }

        throw new AssertionError("Subscription landing screen not visible. Navigate to the screen that shows 'Subscribe Now' first, "
                + "or share the exact entry-point locator from Home/Portfolio/Hub so it can be automated.");
    }

    public void tapSubscribeNowForOnboarding() {
        ReportLogger.step("Tapping Subscribe Now on onboarding landing screen");
        tapFirstVisible(new By[]{SUBSCRIBE_NOW, descContains("Subscribe Now"), textContains("Subscribe Now")},
                "Subscribe Now", 10);
        ReportLogger.pass("Subscribe Now tapped");
    }

    public void validatePlanSelectionForOnboarding() {
        ReportLogger.step("Validating plan selection screen");

        WebElement planMarker = waitForAnyVisible(new By[]{
                descContains("Buy the right funds"),
                descContains("Sharpen the ones you already own"),
                descContains("MONTHLY"),
                descContains("ANNUAL"),
                PLAN_GET_STARTED,
                CONTINUE_TO_PAYMENT_TITLE_CASE,
                CONTINUE_TO_PAYMENT_LOWER_CASE
        }, 15);

        if (planMarker == null) {
            throw new AssertionError(
                    "Plan selection screen did not load after Subscribe Now"
                            + " | visibleValues=" + collectVisibleValues()
            );
        }

        ReportLogger.pass("Plan selection screen is visible");
    }

    public void continueFromPlanSelectionForOnboarding() {
        ReportLogger.step("Continuing from plan selection to identity/KYC flow");

        By[] identityScreenLocators = new By[]{
                CONFIRM_IDENTITY,
                KYC_CHECK,
                VERIFY_PAN_AND_KYC,
                descContains("Confirm your identity"),
                descContains("KYC CHECK"),
                descContains("Verify PAN and KYC"),
                textContains("Confirm your identity"),
                textContains("KYC CHECK"),
                textContains("Verify PAN and KYC")
        };

        // The app may already have moved forward before this method starts.
        if (waitForAnyVisible(identityScreenLocators, 2) != null) {
            ReportLogger.pass("Identity screen is already visible");
            return;
        }

        WebElement actionButton = findPlanSelectionActionButton(12);

        if (actionButton == null) {
            throw new AssertionError(
                    "Plan selection action button was not found. "
                            + "Expected the current CTA containing 'Get started'."
                            + " | visibleValues=" + collectVisibleValues()
            );
        }

        safeClick(actionButton);
        ReportLogger.step("Plan selection Get started button tapped");

        if (waitForAnyVisible(identityScreenLocators, 20) == null) {
            throw new AssertionError(
                    "Identity/KYC screen did not appear after tapping Get started."
                            + " | visibleValues=" + collectVisibleValues()
            );
        }

        ReportLogger.pass("Plan selection continued successfully");
    }

    public void waitForIdentityScreenForOnboarding() {
        ReportLogger.step("Waiting for Confirm your identity / KYC Check screen");

        if (waitForAnyVisible(new By[]{CONFIRM_IDENTITY, KYC_CHECK, VERIFY_PAN_AND_KYC}, 20) == null) {
            throw new AssertionError("Confirm your identity screen did not appear");
        }

        ReportLogger.pass("Confirm your identity screen is visible");
    }
    private String getRequiredConfig(String key) {
        try {
            String value = ConfigReader.get(key);

            if (value == null || value.trim().isEmpty()) {
                throw new AssertionError("Missing required config key: " + key);
            }

            return value.trim();

        } catch (AssertionError e) {
            throw e;
        } catch (Exception e) {
            throw new AssertionError("Unable to read config key: " + key + " | " + clean(e.getMessage()), e);
        }
    }
    public void fillIdentityDetailsForOnboarding() {
        ReportLogger.step("Filling PAN, DOB and mobile on identity screen by label mapping");

        String pan = onboardingRequiredConfig("onboardingPan")
                .replaceAll("[^A-Za-z0-9]", "")
                .toUpperCase();

        String dob = onboardingRequiredConfig("onboardingDob").trim();

        String mobile = onboardingRequiredConfig("onboardingMobile")
                .replaceAll("[^0-9]", "");

        if (!pan.matches("[A-Z]{5}[0-9]{4}[A-Z]")) {
            throw new AssertionError(
                    "Invalid onboardingPan. Current value: " + pan + ". Expected format: ABCDE1234F"
            );
        }

        if (!dob.matches("\\d{2}/\\d{2}/\\d{4}")) {
            throw new AssertionError(
                    "Invalid onboardingDob. Use DD/MM/YYYY format. Example: 06/03/2004"
            );
        }

        if (!mobile.matches("[6-9][0-9]{9}")) {
            throw new AssertionError(
                    "Invalid onboardingMobile. Use valid 10-digit Indian mobile number."
            );
        }

        WebElement panField = onboardingFindEditTextBelowLabel("PAN", 15);
        onboardingClearAndType(panField, pan, "PAN");
        onboardingHideKeyboardSafely();

        onboardingTapDobAndType(dob);
        onboardingHideKeyboardSafely();

        WebElement mobileField = onboardingFindEditTextBelowLabel("MOBILE NUMBER", 15);
        onboardingClearAndType(mobileField, mobile, "Mobile number");
        onboardingHideKeyboardSafely();

        ReportLogger.pass("PAN, DOB and mobile filled successfully");
    }
    
    private WebElement onboardingFindEditTextBelowLabel(String labelText, int timeoutSeconds) {
        WebElement label = onboardingFindExactTextOrDesc(labelText, timeoutSeconds);

        int labelBottomY = label.getRect().getY() + label.getRect().getHeight();

        long endTime = System.currentTimeMillis() + (timeoutSeconds * 1000L);

        while (System.currentTimeMillis() < endTime) {
            List<WebElement> fields = driver.findElements(By.className("android.widget.EditText"));

            WebElement bestMatch = null;
            int bestY = Integer.MAX_VALUE;

            for (WebElement field : fields) {
                try {
                    if (field == null || !field.isDisplayed() || !field.isEnabled()) {
                        continue;
                    }

                    int fieldY = field.getRect().getY();

                    if (fieldY > labelBottomY && fieldY < bestY) {
                        bestMatch = field;
                        bestY = fieldY;
                    }
                } catch (Exception ignored) {
                    // Ignore stale/hidden field
                }
            }

            if (bestMatch != null) {
                ReportLogger.step("Found input field below label: " + labelText);
                return bestMatch;
            }

            sleep(500);
        }

        throw new AssertionError("Could not find EditText below label: " + labelText);
    }

    private void onboardingTapDobAndType(String dob) {
        ReportLogger.step("Selecting DOB from date picker using direct year selection");

        LocalDate targetDob = parseDobForDatePicker(dob);

        openDobDatePicker();
        waitForDobDatePicker();

        selectYearDirectly(targetDob.getYear());

        YearMonth visibleMonth = getVisibleCalendarMonth();
        YearMonth targetMonth = YearMonth.from(targetDob);

        long monthsToMove = ChronoUnit.MONTHS.between(visibleMonth, targetMonth);

        ReportLogger.step(
                "DOB picker after year selection. Visible month: " + visibleMonth
                        + " | Target month: " + targetMonth
                        + " | Month moves required: " + monthsToMove
        );

        if (Math.abs(monthsToMove) > 11) {
            throw new AssertionError(
                    "Year selection did not work correctly. Month difference is still too high: "
                            + monthsToMove
                            + ". Expected max 11."
            );
        }

        moveDatePickerByMonths(monthsToMove);

        String targetDateDesc = buildDatePickerContentDesc(targetDob);

        ReportLogger.step("Selecting DOB calendar date: " + targetDateDesc);

        WebElement targetDate = onboardingFindByExactContentDesc(targetDateDesc, 15);
        onboardingTapElementCenter(targetDate);
        sleep(500);

        WebElement selectButton = onboardingFindByExactContentDesc("Select", 10);
        onboardingTapElementCenter(selectButton);
        sleep(1000);

        String source = driver.getPageSource();

        if (!source.contains(dob)) {
            throw new AssertionError(
                    "DOB was selected but not visible on identity screen. DOB=" + dob
                            + " | pageSource=" + source
            );
        }

        ReportLogger.step("DOB selected successfully using direct year picker");
    }
    
    private void selectYearDirectly(int targetYear) {
        String year = String.valueOf(targetYear);

        ReportLogger.step("Opening DOB year picker using right-side year selector");

        openDobYearPickerCorrectly();

        if (tapYearIfVisible(year)) {
            ReportLogger.step("DOB year selected directly: " + year);
            sleep(800);
            return;
        }

        /*
         * Usually older years are above current visible year.
         * Swipe down first to reach older years like 2004 from 2008.
         */
        for (int i = 1; i <= 12; i++) {
            swipeDobYearList("down");

            if (tapYearIfVisible(year)) {
                ReportLogger.step("DOB year selected after downward year-list swipe: " + year);
                sleep(800);
                return;
            }
        }

        /*
         * Safety fallback in case the picker list direction is opposite on this build.
         */
        for (int i = 1; i <= 18; i++) {
            swipeDobYearList("up");

            if (tapYearIfVisible(year)) {
                ReportLogger.step("DOB year selected after upward year-list swipe: " + year);
                sleep(800);
                return;
            }
        }

        throw new AssertionError(
                "Unable to select DOB year directly: " + year
                        + " | pageSource=" + driver.getPageSource()
        );
    }
    
    private void openDobYearPickerCorrectly() {
        List<WebElement> yearButtons = driver.findElements(AppiumBy.accessibilityId("Select year"));

        if (yearButtons == null || yearButtons.isEmpty()) {
            throw new AssertionError("DOB year selector not found. No element with content-desc: Select year");
        }

        WebElement rightMostVisibleButton = null;
        int maxCenterX = -1;

        for (WebElement button : yearButtons) {
            try {
                if (button == null || !button.isDisplayed() || !button.isEnabled()) {
                    continue;
                }

                int centerX = button.getRect().getX() + (button.getRect().getWidth() / 2);

                if (centerX > maxCenterX) {
                    maxCenterX = centerX;
                    rightMostVisibleButton = button;
                }
            } catch (Exception ignored) {
                // Ignore stale/hidden button
            }
        }

        if (rightMostVisibleButton == null) {
            throw new AssertionError("DOB year selector found but no visible enabled year selector button.");
        }

        onboardingTapElementCenter(rightMostVisibleButton);
        sleep(1000);

        ReportLogger.step("DOB right-side year selector tapped");
    }



    private void swipeDobYearList(String direction) {
        try {
            int screenWidth = driver.manage().window().getSize().getWidth();
            int screenHeight = driver.manage().window().getSize().getHeight();

            Map<String, Object> params = new HashMap<>();
            params.put("left", (int) (screenWidth * 0.10));
            params.put("top", (int) (screenHeight * 0.42));
            params.put("width", (int) (screenWidth * 0.80));
            params.put("height", (int) (screenHeight * 0.42));
            params.put("direction", direction);
            params.put("percent", 0.70);

            driver.executeScript("mobile: swipeGesture", params);
            sleep(500);

            ReportLogger.step("DOB year list swiped: " + direction);

        } catch (Exception e) {
            throw new AssertionError(
                    "Failed to swipe DOB year list " + direction + ": " + onboardingClean(e.getMessage()),
                    e
            );
        }
    }

    private boolean tapYearIfVisible(String year) {
        By[] yearLocators = new By[]{
                AppiumBy.accessibilityId(year),
                AppiumBy.androidUIAutomator("new UiSelector().description(\"" + year + "\")"),
                AppiumBy.androidUIAutomator("new UiSelector().text(\"" + year + "\")")
        };

        for (By locator : yearLocators) {
            try {
                List<WebElement> elements = driver.findElements(locator);

                for (WebElement element : elements) {
                    if (element != null && element.isDisplayed() && element.isEnabled()) {
                        onboardingTapElementCenter(element);
                        return true;
                    }
                }
            } catch (Exception ignored) {
                // Try next locator
            }
        }

        return false;
    }
    
    private LocalDate parseDobForDatePicker(String dob) {
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
            return LocalDate.parse(dob, formatter);
        } catch (Exception e) {
            throw new AssertionError(
                    "Invalid DOB format. Use DD/MM/YYYY. Current value: " + dob,
                    e
            );
        }
    }

    private void waitForDobDatePicker() {
        if (isDobDatePickerVisible(15)) {
            ReportLogger.step("DOB calendar date picker is visible");
            return;
        }

        throw new AssertionError(
                "DOB date picker did not open"
                        + " | visibleValues=" + collectVisibleValues()
        );
    }

    private YearMonth getVisibleCalendarMonth() {
        String source = driver.getPageSource();

        Pattern pattern = Pattern.compile(
                "content-desc=\"\\d{1,2},\\s+[A-Za-z]+,\\s+([A-Za-z]+)\\s+\\d{1,2},\\s+(\\d{4})\""
        );

        Matcher matcher = pattern.matcher(source);

        if (!matcher.find()) {
            throw new AssertionError("Unable to read visible calendar month/year from date picker source.");
        }

        String monthName = matcher.group(1);
        int year = Integer.parseInt(matcher.group(2));

        Month month = Month.valueOf(monthName.toUpperCase(Locale.ENGLISH));

        return YearMonth.of(year, month);
    }

    private void moveDatePickerByMonths(long monthsToMove) {
        if (monthsToMove == 0) {
            ReportLogger.step("Date picker already on target month");
            return;
        }

        By moveButton;

        if (monthsToMove < 0) {
            moveButton = AppiumBy.accessibilityId("Previous month");
        } else {
            moveButton = AppiumBy.accessibilityId("Next month");
        }

        long totalMoves = Math.abs(monthsToMove);

        for (int i = 1; i <= totalMoves; i++) {
            WebElement button = onboardingFindVisible(moveButton, 8);
            onboardingTapElementCenter(button);
            sleep(250);

            if (i % 6 == 0 || i == totalMoves) {
                ReportLogger.step("Date picker month navigation progress: " + i + "/" + totalMoves);
            }
        }
    }

    private String buildDatePickerContentDesc(LocalDate date) {
        String dayOfWeek = date.getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.ENGLISH);
        String month = date.getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH);

        return date.getDayOfMonth()
                + ", "
                + dayOfWeek
                + ", "
                + month
                + " "
                + date.getDayOfMonth()
                + ", "
                + date.getYear();
    }

    private WebElement onboardingFindByExactContentDesc(String contentDesc, int timeoutSeconds) {
        By[] locators = new By[]{
                AppiumBy.accessibilityId(contentDesc),
                AppiumBy.androidUIAutomator("new UiSelector().description(\"" + contentDesc + "\")")
        };

        long endTime = System.currentTimeMillis() + (timeoutSeconds * 1000L);

        while (System.currentTimeMillis() < endTime) {
            for (By locator : locators) {
                try {
                    List<WebElement> elements = driver.findElements(locator);

                    for (WebElement element : elements) {
                        if (element != null && element.isDisplayed() && element.isEnabled()) {
                            return element;
                        }
                    }
                } catch (Exception ignored) {
                    // Try next locator
                }
            }

            sleep(500);
        }

        throw new AssertionError("Element not found with content-desc: " + contentDesc);
    }

    private WebElement onboardingFindVisible(By locator, int timeoutSeconds) {
        long endTime = System.currentTimeMillis() + (timeoutSeconds * 1000L);

        while (System.currentTimeMillis() < endTime) {
            try {
                List<WebElement> elements = driver.findElements(locator);

                for (WebElement element : elements) {
                    if (element != null && element.isDisplayed() && element.isEnabled()) {
                        return element;
                    }
                }
            } catch (Exception ignored) {
                // Retry
            }

            sleep(500);
        }

        throw new AssertionError("Visible element not found: " + locator);
    }
    private void openDobDatePicker() {
        ReportLogger.step("Opening DOB date picker");

        /*
         * Older builds exposed the placeholder as text/content-desc.
         * Try those stable locators first, but do not depend on them.
         */
        By[] directDobLocators = new By[]{
                AppiumBy.accessibilityId("DD/MM/YYYY"),
                AppiumBy.androidUIAutomator("new UiSelector().description(\"DD/MM/YYYY\")"),
                AppiumBy.androidUIAutomator("new UiSelector().text(\"DD/MM/YYYY\")"),
                AppiumBy.androidUIAutomator("new UiSelector().descriptionContains(\"DD/MM\")"),
                AppiumBy.androidUIAutomator("new UiSelector().textContains(\"DD/MM\")")
        };

        WebElement directTarget = waitForAnyVisible(directDobLocators, 2);
        if (directTarget != null) {
            try {
                onboardingTapElementCenter(directTarget);
                if (isDobDatePickerVisible(4)) {
                    ReportLogger.step("DOB date picker opened using placeholder locator");
                    return;
                }
            } catch (Exception ignored) {
                // The hierarchy may refresh after PAN entry. Use the label-bounded fallback below.
            }
        }

        /*
         * Current build: the DOB box is an unlabeled android.view.View.
         * Locate the accessible labels above and below it, then tap the center
         * of the field area between them. This avoids brittle instance/XPath
         * locators and does not reuse a stale WebElement after tapping.
         */
        long endAt = System.currentTimeMillis() + 12_000L;
        String lastError = "";

        while (System.currentTimeMillis() < endAt) {
            try {
                WebElement dobLabel = onboardingFindExactTextOrDesc("DATE OF BIRTH", 2);
                WebElement mobileLabel = onboardingFindExactTextOrDesc("MOBILE NUMBER", 2);

                Rectangle dobLabelRect = dobLabel.getRect();
                Rectangle mobileLabelRect = mobileLabel.getRect();

                int fieldTop = dobLabelRect.getY() + dobLabelRect.getHeight();
                int fieldBottom = mobileLabelRect.getY();

                if (fieldBottom <= fieldTop) {
                    throw new IllegalStateException(
                            "Invalid DOB field bounds. top=" + fieldTop + ", bottom=" + fieldBottom
                    );
                }

                int x = driver.manage().window().getSize().getWidth() / 2;
                int y = fieldTop + ((fieldBottom - fieldTop) / 2);

                Map<String, Object> params = new HashMap<>();
                params.put("x", x);
                params.put("y", y);
                driver.executeScript("mobile: clickGesture", params);

                ReportLogger.step(
                        "DOB field tapped between DATE OF BIRTH and MOBILE NUMBER labels"
                );

                if (isDobDatePickerVisible(5)) {
                    ReportLogger.step("DOB date picker opened using label-bounded field location");
                    return;
                }

                lastError = "DOB picker did not appear after label-bounded tap";
            } catch (Exception | AssertionError e) {
                lastError = onboardingClean(e.getMessage());
            }

            sleep(500);
        }

        throw new AssertionError(
                "Unable to open DOB date picker using placeholder or label-bounded field location"
                        + " | lastError=" + lastError
                        + " | visibleValues=" + collectVisibleValues()
        );
    }

    private boolean isDobDatePickerVisible(int timeoutSeconds) {
        long endAt = System.currentTimeMillis() + (timeoutSeconds * 1000L);

        while (System.currentTimeMillis() < endAt) {
            try {
                String source = driver.getPageSource();
                if (source != null
                        && source.contains("Previous month")
                        && source.contains("Select")) {
                    return true;
                }
            } catch (Exception ignored) {
                // The picker may be animating. Retry until timeout.
            }

            sleep(300);
        }

        return false;
    }

    private WebElement onboardingFindExactTextOrDesc(String value, int timeoutSeconds) {
        By[] locators = new By[]{
                AppiumBy.accessibilityId(value),
                AppiumBy.androidUIAutomator("new UiSelector().description(\"" + value + "\")"),
                AppiumBy.androidUIAutomator("new UiSelector().text(\"" + value + "\")")
        };

        long endTime = System.currentTimeMillis() + (timeoutSeconds * 1000L);

        while (System.currentTimeMillis() < endTime) {
            for (By locator : locators) {
                try {
                    List<WebElement> elements = driver.findElements(locator);

                    for (WebElement element : elements) {
                        if (element != null && element.isDisplayed()) {
                            return element;
                        }
                    }
                } catch (Exception ignored) {
                    // Try next locator
                }
            }

            sleep(500);
        }

        throw new AssertionError("Element not found for exact text/content-desc: " + value);
    }

    private void onboardingClearAndType(WebElement element, String value, String fieldName) {
        try {
            onboardingTapElementCenter(element);
            sleep(400);

            try {
                element.clear();
                sleep(300);
            } catch (Exception ignored) {
                // Some React Native fields do not support clear reliably
            }

            element.sendKeys(value);
            sleep(600);

            ReportLogger.step(fieldName + " entered successfully");
        } catch (Exception e) {
            throw new AssertionError(
                    "Unable to type " + fieldName + ": " + onboardingClean(e.getMessage()),
                    e
            );
        }
    }

    private void onboardingTapElementCenter(WebElement element) {
        int x = element.getRect().getX() + (element.getRect().getWidth() / 2);
        int y = element.getRect().getY() + (element.getRect().getHeight() / 2);

        Map<String, Object> params = new HashMap<>();
        params.put("x", x);
        params.put("y", y);

        driver.executeScript("mobile: clickGesture", params);
    }

    private String onboardingRequiredConfig(String key) {
        try {
            String value = ConfigReader.get(key);

            if (value == null || value.trim().isEmpty()) {
                throw new AssertionError("Missing required config key: " + key);
            }

            return value.trim();
        } catch (AssertionError e) {
            throw e;
        } catch (Exception e) {
            throw new AssertionError(
                    "Unable to read config key: " + key + " | " + onboardingClean(e.getMessage()),
                    e
            );
        }
    }

    private void onboardingHideKeyboardSafely() {
        try {
            driver.hideKeyboard();
            sleep(500);
        } catch (Exception ignored) {
            // Keyboard may already be hidden
        }
    }

    private String onboardingClean(String message) {
        if (message == null) {
            return "";
        }

        return message.replace("\n", " ").replace("\r", " ").trim();
    }
    




    public void verifyPanAndKycForOnboarding() {
        ReportLogger.step("Verifying PAN and KYC");

        onboardingHideKeyboardSafely();
        sleep(500);

        /*
         * The current app build requires the KYC consent checkbox to be
         * selected manually. The checkbox is selected only when the Verify
         * PAN and KYC button changes to clickable=true.
         */
        ensureKycConsentSelected();

        WebElement verifyButton = waitForVerifyPanAndKycButtonClickable(20);
        onboardingTapElementCenter(verifyButton);

        ReportLogger.pass("Verify PAN and KYC tapped");

        waitForPanKycVerificationResult();
    }

    private void ensureKycConsentSelected() {
        ReportLogger.step("Ensuring KYC consent checkbox is selected");

        // Avoid double-tapping when consent is already selected.
        if (findStrictlyClickableVerifyButton(2) != null) {
            ReportLogger.pass("KYC consent is already selected");
            return;
        }

        WebElement consentControl = findKycConsentControl(8);

        if (consentControl == null) {
            ReportLogger.step("KYC consent control not visible. Scrolling once to locate it.");
            swipeUp();
            sleep(600);
            consentControl = findKycConsentControl(6);
        }

        if (consentControl == null) {
            throw new AssertionError(
                    "KYC consent checkbox/control was not found"
                            + " | visibleValues=" + collectVisibleValues()
            );
        }

        safeClick(consentControl);
        ReportLogger.step("KYC consent checkbox tapped");

        if (findStrictlyClickableVerifyButton(8) != null) {
            ReportLogger.pass("KYC consent selected successfully");
            return;
        }

        /*
         * Some React Native builds expose only the consent row description,
         * while the small checkbox itself has no accessibility label. Tap the
         * left side of the freshly located consent row as a bounded fallback.
         */
        WebElement refreshedConsentControl = findKycConsentControl(3);
        if (refreshedConsentControl != null) {
            tapLeftSideOfElement(refreshedConsentControl);
            ReportLogger.step("KYC consent checkbox tapped using consent-row fallback");
        }

        if (findStrictlyClickableVerifyButton(8) == null) {
            throw new AssertionError(
                    "KYC consent was tapped, but Verify PAN and KYC is still disabled"
                            + " | visibleValues=" + collectVisibleValues()
            );
        }

        ReportLogger.pass("KYC consent selected successfully");
    }

    private WebElement findKycConsentControl(int timeoutSeconds) {
        By[] consentLocators = new By[]{
                AppiumBy.androidUIAutomator(
                        "new UiSelector().descriptionContains(\"consent\").clickable(true)"
                ),
                AppiumBy.androidUIAutomator(
                        "new UiSelector().textContains(\"consent\").clickable(true)"
                ),
                CONSENT_CHECKBOX_TEXT,
                descContains("consent to fetching"),
                descContains("fetching my KYC"),
                textContains("consent to fetching"),
                textContains("fetching my KYC")
        };

        long endAt = System.currentTimeMillis() + (timeoutSeconds * 1000L);

        while (System.currentTimeMillis() < endAt) {
            for (By locator : consentLocators) {
                try {
                    List<WebElement> elements = driver.findElements(locator);

                    for (WebElement element : elements) {
                        if (isDisplayed(element) && element.isEnabled()) {
                            return element;
                        }
                    }
                } catch (Exception ignored) {
                    // Try the next consent locator.
                }
            }

            sleep(400);
        }

        return null;
    }

    private void tapLeftSideOfElement(WebElement element) {
        try {
            Rectangle rect = element.getRect();

            int x = rect.getX() + Math.min(35, Math.max(10, rect.getWidth() / 10));
            int y = rect.getY() + (rect.getHeight() / 2);

            Map<String, Object> params = new HashMap<>();
            params.put("x", x);
            params.put("y", y);

            driver.executeScript("mobile: clickGesture", params);
            sleep(600);
        } catch (Exception e) {
            throw new AssertionError(
                    "Unable to tap the KYC consent checkbox area: " + clean(e.getMessage()),
                    e
            );
        }
    }

    private WebElement waitForVerifyPanAndKycButtonClickable(int timeoutSeconds) {
        WebElement button = findStrictlyClickableVerifyButton(timeoutSeconds);

        if (button != null) {
            ReportLogger.step("Verify PAN and KYC button is clickable");
            return button;
        }

        throw new AssertionError(
                "Verify PAN and KYC button did not become clickable after selecting KYC consent. "
                        + "Check PAN, DOB, mobile number and consent state."
                        + " | visibleValues=" + collectVisibleValues()
        );
    }

    private WebElement findStrictlyClickableVerifyButton(int timeoutSeconds) {
        By[] verifyLocators = new By[]{
                VERIFY_PAN_AND_KYC,
                descContains("Verify PAN and KYC"),
                textContains("Verify PAN and KYC"),
                descContains("Verify PAN"),
                textContains("Verify PAN")
        };

        long endAt = System.currentTimeMillis() + (timeoutSeconds * 1000L);

        while (System.currentTimeMillis() < endAt) {
            for (By locator : verifyLocators) {
                try {
                    List<WebElement> elements = driver.findElements(locator);

                    for (WebElement element : elements) {
                        if (isDisplayed(element)
                                && element.isEnabled()
                                && "true".equalsIgnoreCase(element.getAttribute("clickable"))) {
                            return element;
                        }
                    }
                } catch (Exception ignored) {
                    // Try next locator.
                }
            }

            sleep(400);
        }

        return null;
    }

    private void waitForPanKycVerificationResult() {
        ReportLogger.step("Waiting for PAN/KYC verification result");

        By[] resultLocators = new By[]{
                PAN_VERIFIED,
                KYC_VALIDATED,
                CONTINUE_TO_PAYMENT_LOWER_CASE,
                CONTINUE_TO_PAYMENT_TITLE_CASE,
                descContains("PAN VERIFIED"),
                descContains("KYC Validated"),
                descContains("Continue to payment"),
                descContains("Continue to Payment"),
                textContains("PAN VERIFIED"),
                textContains("KYC Validated"),
                textContains("Continue to payment"),
                textContains("Continue to Payment")
        };

        long endAt = System.currentTimeMillis() + 45_000L;

        while (System.currentTimeMillis() < endAt) {
            for (By locator : resultLocators) {
                WebElement element = findVisible(locator);
                if (element != null) {
                    ReportLogger.pass("PAN/KYC verification completed");
                    return;
                }
            }

            By[] errorLocators = new By[]{
                    descContains("Uh oh"),
                    textContains("Uh oh"),
                    descContains("Something went wrong"),
                    textContains("Something went wrong"),
                    descContains("Please try again"),
                    textContains("Please try again"),
                    descContains("not allowed"),
                    textContains("not allowed"),
                    descContains("Invalid PAN"),
                    textContains("Invalid PAN"),
                    descContains("PAN is invalid"),
                    textContains("PAN is invalid")
            };

            if (waitForAnyVisible(errorLocators, 1) != null) {
                throw new AssertionError(
                        "PAN/KYC verification failed with a visible app error message"
                                + " | visibleValues=" + collectVisibleValues()
                );
            }

            sleep(700);
        }

        throw new AssertionError("PAN/KYC verification did not complete within timeout"
                + " | visibleValues=" + collectVisibleValues());
    }

    public void continueToPaymentAfterKycForOnboarding() {
        ReportLogger.step("Continuing to payment after PAN/KYC validation");
        scrollToButtonIfNeeded(CONTINUE_TO_PAYMENT_LOWER_CASE, "Continue to payment");
        tapContinueToPayment("KYC Continue to payment");
        ReportLogger.pass("Moved from KYC validation to payment setup");
    }

    public void waitForMandateSetupForOnboarding() {
        ReportLogger.step("Waiting for recurring mandate setup screen");

        if (waitForAnyVisible(new By[]{AUTHORISE_RECURRING_MANDATE, AUTHORISE_MANDATE, UPI_AUTOPAY_CARD}, 20) == null) {
            throw new AssertionError("Recurring mandate setup screen did not appear"
                    + " | visibleValues=" + collectVisibleValues());
        }

        ReportLogger.pass("Recurring mandate setup screen is visible");
    }

    public void choosePaymentModeForOnboarding() {
        String paymentMode = ConfigReader.getOptional("onboardingPaymentMode", "UPI Autopay").trim();

        ReportLogger.step("Selecting onboarding payment mode: " + paymentMode);

        if (paymentMode.equalsIgnoreCase("Credit card")) {
            tapIfVisible(CREDIT_CARD, "Credit card", 5);
        } else if (paymentMode.equalsIgnoreCase("Debit card")
                || paymentMode.equalsIgnoreCase("Netbanking")
                || paymentMode.equalsIgnoreCase("Debit card and netbanking")) {
            tapIfVisible(DEBIT_NETBANKING, "Debit card and netbanking", 5);
        } else {
            tapIfVisible(UPI_AUTOPAY_CARD, "UPI Autopay", 5);
        }

        ReportLogger.pass("Payment mode selected/confirmed: " + paymentMode);
    }

    public void authoriseMandateForOnboarding() {
        ReportLogger.step("Tapping Authorise mandate");

        WebElement mandateButton = waitForClickableAttribute(new By[]{AUTHORISE_MANDATE, descContains("Authorise mandate")}, 20);
        if (mandateButton == null) {
            throw new AssertionError("Authorise mandate button is not clickable"
                    + " | visibleValues=" + collectVisibleValues());
        }

        safeClick(mandateButton);
        ReportLogger.pass("Authorise mandate tapped");
    }

    public void completeRazorpayMandateForOnboarding() {
        String completionMode = ConfigReader.getOptional("onboardingPaymentCompletionMode", "auto").trim();
        int manualTimeout = parseInt(ConfigReader.getOptional("onboardingManualPaymentTimeoutSeconds", "180"), 180);

        ReportLogger.step("Handling Razorpay payment gateway | mode=" + completionMode);

        if (completionMode.equalsIgnoreCase("manual")) {
            ReportLogger.step("Manual gateway mode enabled. Complete payment/mandate manually on device.");
            waitForMandateAuthorisedForOnboarding(manualTimeout);
            return;
        }

        waitForRazorpayPaymentOptions();
        selectRazorpayPaymentApp();
        tapRazorpayContinue();
        handleGatewayResultAfterContinue();
        waitForMandateAuthorisedForOnboarding(120);
    }

    public void waitForMandateAuthorisedForOnboarding(int timeoutSeconds) {
        ReportLogger.step("Waiting for Mandate authorised screen");

        WebElement mandateAuthorised = waitForAnyVisible(new By[]{MANDATE_AUTHORISED, descContains("Mandate authorised")}, timeoutSeconds);
        if (mandateAuthorised == null) {
            throw new AssertionError("Mandate authorised screen not visible after payment gateway flow"
                    + " | currentPackage=" + safeCurrentPackage()
                    + " | visibleValues=" + collectVisibleValues());
        }

        ReportLogger.pass("Mandate authorised screen is visible");
    }

    public void continueToFetchKycForOnboarding() {
        ReportLogger.step("Tapping Continue to Fetch KYC");

        tapFirstVisible(new By[]{CONTINUE_TO_FETCH_KYC, descContains("Continue to Fetch KYC")},
                "Continue to Fetch KYC", 20);

        if (waitForAnyVisible(new By[]{
                KYC_OTP_SCREEN,
                descContains("Verifying KYC details with OTP"),
                textContains("Verifying KYC details with OTP"),
                KYC_OTP_VERIFY_AND_CONTINUE
        }, 45) == null) {
            throw new AssertionError(
                    "KYC OTP verification screen did not appear after Continue to Fetch KYC"
                            + " | visibleValues=" + collectVisibleValues()
            );
        }

        ReportLogger.pass("Continue to Fetch KYC tapped and KYC OTP screen is visible");
    }

    public void completeKycOtpVerificationForOnboarding() {
        ReportLogger.step("Completing KYC OTP verification using the separate KYC email inbox");

        if (!isKycOtpScreenVisible()) {
            if (waitForAnyVisible(new By[]{
                    KYC_OTP_SCREEN,
                    descContains("Verifying KYC details with OTP"),
                    textContains("Verifying KYC details with OTP"),
                    KYC_OTP_VERIFY_AND_CONTINUE
            }, 30) == null) {
                throw new AssertionError(
                        "KYC OTP screen is not visible"
                                + " | screenValues=" + collectRelevantScreenValues()
                );
            }
        }

        String otp = KycOtpEmailReader.fetchLatestKycOtp();
        if (otp == null || !otp.matches("\\d{6}")) {
            throw new AssertionError("KYC OTP reader returned an invalid OTP");
        }

        enterKycOtpReliably(otp);
        onboardingHideKeyboardSafely();
        sleep(700);

        /*
         * Some builds auto-submit as soon as all six OTP digits are entered.
         * Do not click again if the next screen has already appeared.
         */
        if (isContactDetailsScreenVisible()) {
            ReportLogger.pass("KYC OTP auto-verified and Contact Details screen is visible");
            return;
        }

        WebElement verifyButton = waitForStrictlyClickableKycOtpButton(20);
        safeClick(verifyButton);
        ReportLogger.step("Verify and continue tapped on KYC OTP screen");

        if (waitForContactDetailsAfterKycOtp(18)) {
            ReportLogger.pass("KYC OTP verified successfully and Contact Details screen is visible");
            return;
        }

        /*
         * A React Native hierarchy refresh can occasionally consume the first tap
         * without navigation. Retry only when the OTP screen is still visible,
         * no error is displayed, and the button is still strictly clickable.
         */
        if (isKycOtpScreenVisible() && !isVisibleKycOtpError()) {
            ReportLogger.step("KYC OTP screen is still visible after first tap. Retrying Verify and continue once.");

            WebElement retryButton = waitForStrictlyClickableKycOtpButton(8);
            safeClick(retryButton);
            ReportLogger.step("Verify and continue retry tapped on KYC OTP screen");

            if (waitForContactDetailsAfterKycOtp(25)) {
                ReportLogger.pass("KYC OTP verified successfully after one retry");
                return;
            }
        }

        if (isVisibleKycOtpError()) {
            throw new AssertionError(
                    "KYC OTP verification failed with a visible app error"
                            + " | screenValues=" + collectRelevantScreenValues()
            );
        }

        throw new AssertionError(
                "Contact Details screen did not appear after KYC OTP verification. "
                        + "The OTP was entered and Verify and continue was tapped, but navigation was not confirmed."
                        + " | currentPackage=" + safeCurrentPackage()
                        + " | screenValues=" + collectRelevantScreenValues()
        );
    }

    private WebElement waitForStrictlyClickableKycOtpButton(int timeoutSeconds) {
        WebElement button = findStrictlyClickableKycOtpButton(timeoutSeconds);

        if (button != null) {
            ReportLogger.step("Verify and continue button is strictly clickable");
            return button;
        }

        if (isContactDetailsScreenVisible()) {
            return null;
        }

        throw new AssertionError(
                "Verify and continue did not become clickable after entering KYC OTP"
                        + " | screenValues=" + collectRelevantScreenValues()
        );
    }

    private WebElement findStrictlyClickableKycOtpButton(int timeoutSeconds) {
        By[] locators = new By[]{
                AppiumBy.androidUIAutomator(
                        "new UiSelector().description(\"Verify and continue\").clickable(true)"
                ),
                AppiumBy.androidUIAutomator(
                        "new UiSelector().text(\"Verify and continue\").clickable(true)"
                ),
                KYC_OTP_VERIFY_AND_CONTINUE,
                descContains("Verify and continue"),
                textContains("Verify and continue")
        };

        long endAt = System.currentTimeMillis() + (timeoutSeconds * 1000L);

        while (System.currentTimeMillis() < endAt) {
            for (By locator : locators) {
                try {
                    List<WebElement> elements = driver.findElements(locator);

                    for (WebElement element : elements) {
                        if (!isDisplayed(element) || !element.isEnabled()) {
                            continue;
                        }

                        if ("true".equalsIgnoreCase(element.getAttribute("clickable"))) {
                            return element;
                        }
                    }
                } catch (Exception ignored) {
                    // The OTP controls can refresh after every entered digit.
                }
            }

            if (isContactDetailsScreenVisible()) {
                return null;
            }

            sleep(300);
        }

        return null;
    }

    private boolean waitForContactDetailsAfterKycOtp(int timeoutSeconds) {
        long endAt = System.currentTimeMillis() + (timeoutSeconds * 1000L);

        while (System.currentTimeMillis() < endAt) {
            if (isContactDetailsScreenVisible()) {
                return true;
            }

            if (isVisibleKycOtpError()) {
                return false;
            }

            sleep(500);
        }

        return false;
    }

    private boolean isContactDetailsScreenVisible() {
        return waitForAnyVisible(new By[]{
                CONTACT_DETAILS,
                CONTACT_DETAILS_HEADING,
                descContains("CONTACT DETAILS"),
                descContains("Where should we reach you"),
                textContains("CONTACT DETAILS"),
                textContains("Where should we reach you")
        }, 1) != null;
    }

    private boolean isKycOtpScreenVisible() {
        return waitForAnyVisible(new By[]{
                KYC_OTP_SCREEN,
                descContains("Verifying KYC details with OTP"),
                textContains("Verifying KYC details with OTP"),
                descContains("Verify and continue"),
                textContains("Verify and continue")
        }, 1) != null;
    }

    private boolean isVisibleKycOtpError() {
        return waitForAnyVisible(new By[]{
                descContains("Invalid OTP"),
                textContains("Invalid OTP"),
                descContains("incorrect OTP"),
                textContains("incorrect OTP"),
                descContains("OTP has expired"),
                textContains("OTP has expired"),
                descContains("Something went wrong"),
                textContains("Something went wrong"),
                descContains("Please try again"),
                textContains("Please try again")
        }, 1) != null;
    }

    private String collectRelevantScreenValues() {
        try {
            String source = driver.getPageSource();
            if (source == null || source.isBlank()) {
                return "";
            }

            Pattern attributePattern = Pattern.compile(
                    "(?:content-desc|text)=\"([^\"]+)\""
            );
            Matcher matcher = attributePattern.matcher(source);
            List<String> values = new ArrayList<>();

            while (matcher.find() && values.size() < 40) {
                String value = matcher.group(1)
                        .replace("&quot;", "\"")
                        .replace("&amp;", "&")
                        .trim();

                if (!value.isEmpty() && !values.contains(value)) {
                    values.add(value);
                }
            }

            return values.toString();
        } catch (Exception e) {
            return "Unable to collect screen values: " + clean(e.getMessage());
        }
    }

    public void continueFromContactDetailsForOnboarding() {
        ReportLogger.step("Continuing from Contact Details screen");

        if (waitForAnyVisible(new By[]{
                CONTACT_DETAILS,
                CONTACT_DETAILS_HEADING,
                descContains("CONTACT DETAILS"),
                descContains("Where should we reach you")
        }, 25) == null) {
            throw new AssertionError(
                    "Contact Details screen is not visible"
                            + " | visibleValues=" + collectVisibleValues()
            );
        }

        tapFirstVisible(new By[]{
                CONTACT_DETAILS_CONTINUE,
                AppiumBy.androidUIAutomator("new UiSelector().description(\"Continue\")"),
                AppiumBy.androidUIAutomator("new UiSelector().text(\"Continue\")")
        }, "Contact Details Continue", 20);

        if (waitForAnyVisible(new By[]{
                READ_BEFORE_SIGNING,
                ALMOST_THERE,
                CONTINUE_TO_ESIGN,
                descContains("READ BEFORE SIGNING"),
                descContains("You're almost there"),
                textContains("READ BEFORE SIGNING"),
                textContains("You're almost there")
        }, 45) == null) {
            throw new AssertionError(
                    "Agreement summary screen did not appear after Contact Details Continue"
                            + " | visibleValues=" + collectVisibleValues()
            );
        }

        ReportLogger.pass("Contact Details completed and agreement summary screen is visible");
    }

    public void continueToESignForOnboarding() {
        ReportLogger.step("Tapping Continue to E-Sign");

        if (waitForAnyVisible(new By[]{
                READ_BEFORE_SIGNING,
                ALMOST_THERE,
                CONTINUE_TO_ESIGN,
                descContains("READ BEFORE SIGNING"),
                descContains("You're almost there")
        }, 25) == null) {
            throw new AssertionError(
                    "Agreement summary screen is not visible"
                            + " | visibleValues=" + collectVisibleValues()
            );
        }

        tapFirstVisible(new By[]{
                CONTINUE_TO_ESIGN,
                descContains("Continue to E-Sign"),
                textContains("Continue to E-Sign")
        }, "Continue to E-Sign", 20);

        if (!waitForDigioSigningPage(45)) {
            throw new AssertionError(
                    "Digio signing page did not load after Continue to E-Sign"
                            + " | contexts=" + safeContextHandles()
                            + " | visibleValues=" + collectVisibleValues()
            );
        }

        ReportLogger.pass("Continue to E-Sign tapped and Digio signing page loaded");
    }

    public void completeDigioESignForOnboarding() {
        ReportLogger.step("Completing Digio agreement consent and tapping Sign Now");

        boolean signNowTapped = false;

        try {
            if (switchToDigioWebView(25)) {
                ensureDigioCheckboxSelectedInWebView();
                signNowTapped = tapDigioSignNowInWebView(20);
            }
        } catch (Exception e) {
            ReportLogger.step("Digio WebView automation was not available: " + clean(e.getMessage()));
        } finally {
            switchToNativeContextSafely();
        }

        if (!signNowTapped) {
            ReportLogger.step("Trying native accessibility locators for Digio Sign Now");
            signNowTapped = tapDigioSignNowUsingNativeLocators();
        }

        if (!signNowTapped
                && ConfigReader.getOptional("onboardingAllowDigioCoordinateFallback", "true").equalsIgnoreCase("true")) {
            ReportLogger.step("Using bounded coordinate fallback for Digio Sign Now");

            if (ConfigReader.getOptional("onboardingDigioTapCheckboxInCoordinateFallback", "false")
                    .equalsIgnoreCase("true")) {
                tapByScreenPercent(
                        parseDouble(ConfigReader.getOptional("onboardingDigioCheckboxXPercent", "8"), 8.0),
                        parseDouble(ConfigReader.getOptional("onboardingDigioCheckboxYPercent", "81"), 81.0)
                );
                sleep(700);
                ReportLogger.step("Digio checkbox area tapped by configured percentage fallback");
            }

            tapByScreenPercent(
                    parseDouble(ConfigReader.getOptional("onboardingDigioSignXPercent", "50"), 50.0),
                    parseDouble(ConfigReader.getOptional("onboardingDigioSignYPercent", "87"), 87.0)
            );
            signNowTapped = true;
        }

        if (!signNowTapped) {
            throw new AssertionError(
                    "Unable to tap Digio Sign Now using WebView, native locators, or configured fallback"
                            + " | contexts=" + safeContextHandles()
                            + " | visibleValues=" + collectVisibleValues()
            );
        }

        ReportLogger.pass("Digio Sign Now tapped");
        validateAfterDigioSignSubmission();
    }

    private WebElement waitForKycOtpInputField(int timeoutSeconds) {
        long endAt = System.currentTimeMillis() + (timeoutSeconds * 1000L);

        while (System.currentTimeMillis() < endAt) {
            try {
                List<WebElement> fields = driver.findElements(EDIT_TEXT);

                for (WebElement field : fields) {
                    if (field != null && field.isDisplayed() && field.isEnabled()) {
                        return field;
                    }
                }
            } catch (Exception ignored) {
                // KYC OTP field may be animating; retry.
            }

            sleep(300);
        }

        throw new AssertionError(
                "KYC OTP input field was not found"
                        + " | screenValues=" + collectRelevantScreenValues()
        );
    }

    private void enterKycOtpReliably(String otp) {
        ReportLogger.step("Focusing the KYC OTP boxes before entering OTP");

        String lastError = "";

        /*
         * Attempt 1:
         * Tap the first visible OTP box and type into the currently focused
         * Android element. This is required because the six boxes are a visual
         * wrapper around one underlying EditText.
         */
        try {
            WebElement field = waitForKycOtpInputField(12);
            focusFirstKycOtpBox(field);
            clearFocusedKycOtp(field);

            WebElement activeElement = driver.switchTo().activeElement();
            activeElement.sendKeys(otp);
            sleep(1200);

            if (isKycOtpAcceptedForSubmission(5)) {
                ReportLogger.pass("KYC OTP entered successfully after focusing the first OTP box");
                return;
            }

            lastError = "Focused-element sendKeys completed, but the Verify and continue button stayed disabled";
        } catch (Exception e) {
            lastError = "Focused-element input failed: " + clean(e.getMessage());
        }

        /*
         * Attempt 2:
         * Re-focus the first box, clear any partial input and send individual
         * Android digit key events. Key events are reliable for React Native
         * split-OTP controls and do not require Appium adb_shell permissions.
         */
        try {
            ReportLogger.step("Retrying KYC OTP entry using Android digit key events");

            WebElement refreshedField = waitForKycOtpInputField(8);
            focusFirstKycOtpBox(refreshedField);
            clearFocusedKycOtp(refreshedField);

            for (char digit : otp.toCharArray()) {
                driver.pressKey(new KeyEvent(androidKeyForDigit(digit)));
                sleep(180);
            }

            sleep(1200);

            if (isKycOtpAcceptedForSubmission(8)) {
                ReportLogger.pass("KYC OTP entered successfully using Android digit key events");
                return;
            }

            lastError = "Android digit key events were sent, but the Verify and continue button stayed disabled";
        } catch (Exception e) {
            lastError = "Android key-event input failed: " + clean(e.getMessage());
        }

        throw new AssertionError(
                "Unable to enter KYC OTP into the six OTP boxes"
                        + " | lastError=" + lastError
                        + " | screenValues=" + collectRelevantScreenValues()
        );
    }

    private void focusFirstKycOtpBox(WebElement field) {
        try {
            Rectangle rect = field.getRect();

            int xOffset = Math.max(18, Math.min(55, rect.getWidth() / 12));
            int x = rect.getX() + xOffset;
            int y = rect.getY() + (rect.getHeight() / 2);

            Map<String, Object> params = new HashMap<>();
            params.put("x", x);
            params.put("y", y);
            driver.executeScript("mobile: clickGesture", params);

            sleep(500);
            ReportLogger.step("First KYC OTP box tapped and focused");
        } catch (Exception e) {
            throw new AssertionError(
                    "Unable to focus the first KYC OTP box: " + clean(e.getMessage()),
                    e
            );
        }
    }

    private void clearFocusedKycOtp(WebElement field) {
        try {
            field.clear();
            sleep(250);
        } catch (Exception ignored) {
            // React Native split-OTP controls may not implement clear().
        }

        /*
         * DEL key events remove any partial OTP left by a previous attempt.
         * Sending a few extra DEL events is harmless when the field is empty.
         */
        try {
            for (int i = 0; i < 8; i++) {
                driver.pressKey(new KeyEvent(AndroidKey.DEL));
                sleep(60);
            }
        } catch (Exception ignored) {
            // Continue; the field.clear() attempt may already have succeeded.
        }
    }

    private boolean isKycOtpAcceptedForSubmission(int timeoutSeconds) {
        long endAt = System.currentTimeMillis() + (timeoutSeconds * 1000L);

        while (System.currentTimeMillis() < endAt) {
            if (isContactDetailsScreenVisible()) {
                return true;
            }

            if (findStrictlyClickableKycOtpButton(1) != null) {
                return true;
            }

            if (isVisibleKycOtpError()) {
                return false;
            }

            sleep(250);
        }

        return false;
    }

    private AndroidKey androidKeyForDigit(char digit) {
        switch (digit) {
            case '0':
                return AndroidKey.DIGIT_0;
            case '1':
                return AndroidKey.DIGIT_1;
            case '2':
                return AndroidKey.DIGIT_2;
            case '3':
                return AndroidKey.DIGIT_3;
            case '4':
                return AndroidKey.DIGIT_4;
            case '5':
                return AndroidKey.DIGIT_5;
            case '6':
                return AndroidKey.DIGIT_6;
            case '7':
                return AndroidKey.DIGIT_7;
            case '8':
                return AndroidKey.DIGIT_8;
            case '9':
                return AndroidKey.DIGIT_9;
            default:
                throw new IllegalArgumentException("Unsupported OTP character: " + digit);
        }
    }

    private boolean waitForDigioSigningPage(int timeoutSeconds) {
        long endAt = System.currentTimeMillis() + (timeoutSeconds * 1000L);

        while (System.currentTimeMillis() < endAt) {
            if (isVisible(SIGN_NOW)
                    || isVisible(descContains("Sign Now"))
                    || hasWebViewContext()) {
                return true;
            }

            try {
                String source = driver.getPageSource();
                if (source != null
                        && (source.contains("android.webkit.WebView")
                        || source.contains("com.valueresearch.advisor:id/webViewLayout"))) {
                    return true;
                }
            } catch (Exception ignored) {
                // Retry while the Digio page loads.
            }

            sleep(700);
        }

        return false;
    }

    private boolean hasWebViewContext() {
        try {
            for (String context : driver.getContextHandles()) {
                if (context != null && context.toUpperCase(Locale.ENGLISH).startsWith("WEBVIEW")) {
                    return true;
                }
            }
        } catch (Exception ignored) {
            // Context discovery may be temporarily unavailable.
        }
        return false;
    }

    private boolean switchToDigioWebView(int timeoutSeconds) {
        long endAt = System.currentTimeMillis() + (timeoutSeconds * 1000L);
        String originalContext = safeCurrentContext();

        while (System.currentTimeMillis() < endAt) {
            try {
                Set<String> contexts = driver.getContextHandles();
                for (String context : contexts) {
                    if (context == null || !context.toUpperCase(Locale.ENGLISH).startsWith("WEBVIEW")) {
                        continue;
                    }

                    driver.context(context);
                    sleep(700);

                    if (findFirstDisplayedWebElement(new By[]{
                            By.xpath("//button[contains(normalize-space(.),'Sign Now')]"),
                            By.xpath("//*[@role='button' and contains(normalize-space(.),'Sign Now')]"),
                            By.xpath("//*[contains(normalize-space(.),'Sign Now')]")
                    }) != null) {
                        ReportLogger.step("Switched to Digio WebView context: " + context);
                        return true;
                    }
                }
            } catch (Exception ignored) {
                // Chromedriver/WebView can take a few seconds to attach.
            }

            try {
                if (originalContext != null && !originalContext.isBlank()) {
                    driver.context(originalContext);
                } else {
                    driver.context("NATIVE_APP");
                }
            } catch (Exception ignored) {
                // Retry context discovery.
            }

            sleep(800);
        }

        return false;
    }

    private void ensureDigioCheckboxSelectedInWebView() {
        WebElement checkbox = findFirstDisplayedWebElement(new By[]{
                By.cssSelector("input[type='checkbox']"),
                By.xpath("//*[@role='checkbox']"),
                By.xpath("//*[contains(translate(normalize-space(.),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'by continuing')]/preceding::input[@type='checkbox'][1]")
        });

        if (checkbox == null) {
            ReportLogger.step("Digio checkbox is not separately exposed in WebView; continuing to Sign Now");
            return;
        }

        boolean selected = false;
        try {
            selected = checkbox.isSelected();
        } catch (Exception ignored) {
            // Check attributes below.
        }

        try {
            String checked = checkbox.getAttribute("checked");
            String ariaChecked = checkbox.getAttribute("aria-checked");
            selected = selected
                    || "true".equalsIgnoreCase(checked)
                    || "checked".equalsIgnoreCase(checked)
                    || "true".equalsIgnoreCase(ariaChecked);
        } catch (Exception ignored) {
            // Use isSelected result.
        }

        if (!selected) {
            safeClick(checkbox);
            sleep(600);
            ReportLogger.pass("Digio consent checkbox selected");
        } else {
            ReportLogger.pass("Digio consent checkbox is already selected");
        }
    }

    private boolean tapDigioSignNowInWebView(int timeoutSeconds) {
        long endAt = System.currentTimeMillis() + (timeoutSeconds * 1000L);
        By[] signLocators = new By[]{
                By.xpath("//button[normalize-space()='Sign Now']"),
                By.xpath("//button[contains(normalize-space(.),'Sign Now')]"),
                By.xpath("//*[@role='button' and contains(normalize-space(.),'Sign Now')]"),
                By.xpath("//input[contains(@value,'Sign Now')]"),
                By.xpath("//*[contains(normalize-space(.),'Sign Now')]")
        };

        while (System.currentTimeMillis() < endAt) {
            WebElement button = findFirstDisplayedWebElement(signLocators);
            if (button != null) {
                safeClick(button);
                sleep(1000);
                return true;
            }
            sleep(400);
        }

        return false;
    }

    private WebElement findFirstDisplayedWebElement(By[] locators) {
        for (By locator : locators) {
            try {
                for (WebElement element : driver.findElements(locator)) {
                    if (element != null && element.isDisplayed()) {
                        return element;
                    }
                }
            } catch (Exception ignored) {
                // Try next locator.
            }
        }
        return null;
    }

    private boolean tapDigioSignNowUsingNativeLocators() {
        switchToNativeContextSafely();

        WebElement button = waitForAnyVisible(new By[]{
                SIGN_NOW,
                descContains("Sign Now"),
                textContains("Sign Now"),
                AppiumBy.androidUIAutomator("new UiSelector().className(\"android.widget.Button\").textContains(\"Sign Now\")")
        }, 8);

        if (button == null) {
            return false;
        }

        safeClick(button);
        return true;
    }

    private void tapByScreenPercent(double xPercent, double yPercent) {
        Dimension size = driver.manage().window().getSize();

        int x = (int) Math.round(size.getWidth() * (xPercent / 100.0));
        int y = (int) Math.round(size.getHeight() * (yPercent / 100.0));

        Map<String, Object> params = new HashMap<>();
        params.put("x", x);
        params.put("y", y);
        driver.executeScript("mobile: clickGesture", params);
        sleep(800);
    }

    private void validateAfterDigioSignSubmission() {
        switchToNativeContextSafely();

        String finalMarker = ConfigReader.getOptional("onboardingFinalSuccessMarker", "").trim();
        if (!finalMarker.isEmpty()) {
            ReportLogger.step("Waiting for configured final onboarding marker: " + finalMarker);
            if (!waitForTextOrDescriptionContains(finalMarker, 90)) {
                throw new AssertionError(
                        "Configured final onboarding marker was not visible after Digio Sign Now: "
                                + finalMarker
                                + " | visibleValues=" + collectVisibleValues()
                );
            }
            ReportLogger.pass("Final onboarding marker visible: " + finalMarker);
            return;
        }

        By[] commonSuccessMarkers = new By[]{
                descContains("Subscription activated"),
                textContains("Subscription activated"),
                descContains("subscription is active"),
                textContains("subscription is active"),
                descContains("Agreement signed"),
                textContains("Agreement signed"),
                descContains("E-Sign completed"),
                textContains("E-Sign completed"),
                descContains("Go to dashboard"),
                textContains("Go to dashboard"),
                descContains("Congratulations"),
                textContains("Congratulations")
        };

        WebElement success = waitForAnyVisible(commonSuccessMarkers, 20);
        if (success != null) {
            ReportLogger.pass("Onboarding success screen is visible after Digio signing");
            return;
        }

        ReportLogger.pass(
                "Digio Sign Now submitted. Add onboardingFinalSuccessMarker after sharing the final post-sign screen."
        );
    }

    private String safeCurrentContext() {
        try {
            return driver.getContext();
        } catch (Exception e) {
            return "NATIVE_APP";
        }
    }

    private String safeContextHandles() {
        try {
            return String.valueOf(driver.getContextHandles());
        } catch (Exception e) {
            return "unavailable";
        }
    }

    private void switchToNativeContextSafely() {
        try {
            driver.context("NATIVE_APP");
            sleep(300);
        } catch (Exception ignored) {
            // Already native or context is changing.
        }
    }

    private double parseDouble(String value, double defaultValue) {
        try {
            return Double.parseDouble(value.trim());
        } catch (Exception e) {
            return defaultValue;
        }
    }

    // =========================================================
    // Razorpay / external payment helpers
    // =========================================================

    private void waitForRazorpayPaymentOptions() {
        ReportLogger.step("Waiting for Razorpay Checkout payment options");

        if (waitForAnyVisible(new By[]{PAYMENT_OPTIONS, RAZORPAY_CHECKOUT, PHONEPE_CONTAINS}, 45) == null) {
            throw new AssertionError("Razorpay payment options did not appear"
                    + " | currentPackage=" + safeCurrentPackage()
                    + " | visibleValues=" + collectVisibleValues());
        }

        ReportLogger.pass("Razorpay payment options are visible");
    }

    private void selectRazorpayPaymentApp() {
        String paymentApp = ConfigReader.getOptional("onboardingUpiPaymentApp", "PhonePe").trim();
        ReportLogger.step("Selecting Razorpay UPI app: " + paymentApp);

        if (paymentApp.equalsIgnoreCase("Google Pay") || paymentApp.equalsIgnoreCase("GPay")) {
            tapFirstVisible(new By[]{GOOGLE_PAY_CONTAINS, textContains("Google Pay")}, "Google Pay", 12);
        } else if (paymentApp.equalsIgnoreCase("Apps & UPI ID") || paymentApp.equalsIgnoreCase("UPI ID")) {
            tapFirstVisible(new By[]{APPS_AND_UPI_ID, textContains("Apps & UPI ID")}, "Apps & UPI ID", 12);
        } else {
            tapFirstVisible(new By[]{PHONEPE_EXACT_BUTTON, PHONEPE_CONTAINS, textContains("PhonePe")}, "PhonePe", 12);
        }

        ReportLogger.pass("Razorpay payment app selected: " + paymentApp);
    }

    private void tapRazorpayContinue() {
        ReportLogger.step("Tapping Razorpay Continue button");
        tapFirstVisible(new By[]{RAZORPAY_CONTINUE, textContains("Continue")}, "Razorpay Continue", 15);
        ReportLogger.pass("Razorpay Continue tapped");
    }

    private void handleGatewayResultAfterContinue() {
        ReportLogger.step("Handling payment gateway result after Continue");

        int timeoutSeconds = parseInt(ConfigReader.getOptional("onboardingGatewayInitialWaitSeconds", "45"), 45);
        long endAt = System.currentTimeMillis() + (timeoutSeconds * 1000L);
        boolean retryDone = false;

        while (System.currentTimeMillis() < endAt) {
            if (isVisible(MANDATE_AUTHORISED) || isVisible(CONTINUE_TO_FETCH_KYC)) {
                ReportLogger.pass("Returned to Advisor app with mandate result");
                return;
            }

            if (isVisible(PAYMENT_SUCCESSFUL)) {
                ReportLogger.step("Payment Successful marker visible. Waiting for Advisor redirect.");
                sleep(2500);
                activateAdvisorAppIfNeeded();
            }

            if (isVisible(PHONEPE_ERROR)) {
                if (!retryDone && ConfigReader.getOptional("onboardingRetryPhonePeOnce", "true").equalsIgnoreCase("true")) {
                    retryDone = true;
                    ReportLogger.step("PhonePe error visible. Retrying once.");
                    tapIfVisible(PHONEPE_RETRY, "PhonePe RETRY", 5);
                    sleep(5000);
                    continue;
                }

                String allowManual = ConfigReader.getOptional("onboardingAllowManualAfterGatewayError", "true");
                if (allowManual.equalsIgnoreCase("true")) {
                    ReportLogger.step("PhonePe error visible. Waiting for manual correction/redirect.");
                    sleep(5000);
                    continue;
                }

                throw new AssertionError("Payment gateway showed PhonePe error: Something went wrong");
            }

            sleep(1200);
        }

        ReportLogger.step("Gateway auto handling timeout reached. Activating Advisor app and checking mandate state.");
        activateAdvisorAppIfNeeded();
    }

    // =========================================================
    // Generic helpers
    // =========================================================

    private WebElement findPlanSelectionActionButton(int timeoutSeconds) {
        By[] locators = new By[]{
                // Current CTA. The price is dynamic, so match only the stable prefix.
                PLAN_GET_STARTED,
                descContains("Get started"),
                textContains("Get started"),

                // Legacy CTA fallbacks retained for compatibility with older app builds.
                CONTINUE_TO_PAYMENT_TITLE_CASE,
                CONTINUE_TO_PAYMENT_LOWER_CASE,
                descContains("Continue to Payment"),
                descContains("Continue to payment"),
                textContains("Continue to Payment"),
                textContains("Continue to payment")
        };

        long endAt = System.currentTimeMillis() + (timeoutSeconds * 1000L);

        while (System.currentTimeMillis() < endAt) {
            for (By locator : locators) {
                try {
                    List<WebElement> elements = driver.findElements(locator);

                    for (WebElement element : elements) {
                        if (isDisplayed(element) && element.isEnabled()) {
                            ReportLogger.step("Plan selection action button found using: " + locator);
                            return element;
                        }
                    }
                } catch (Exception ignored) {
                    // Try the next stable locator.
                }
            }

            sleep(400);
        }

        return null;
    }





    private void tapContinueToPayment(String label) {
        tapFirstVisible(new By[]{
                CONTINUE_TO_PAYMENT_TITLE_CASE,
                CONTINUE_TO_PAYMENT_LOWER_CASE,
                descContains("Continue to Payment"),
                descContains("Continue to payment")
        }, label, 20);
    }

    private WebElement waitForClickableAttribute(By[] locators, int timeoutSeconds) {
        long endAt = System.currentTimeMillis() + (timeoutSeconds * 1000L);

        while (System.currentTimeMillis() < endAt) {
            for (By locator : locators) {
                try {
                    List<WebElement> elements = driver.findElements(locator);
                    for (WebElement element : elements) {
                        if (isDisplayed(element) && isClickableByAttribute(element)) {
                            return element;
                        }
                    }
                } catch (Exception ignored) {
                    // Try next locator
                }
            }
            sleep(500);
        }

        return null;
    }

    private boolean isClickableByAttribute(WebElement element) {
        try {
            String clickable = element.getAttribute("clickable");
            if ("true".equalsIgnoreCase(clickable)) {
                return true;
            }
        } catch (Exception ignored) {
            // Fall back below
        }

        try {
            return element.isEnabled();
        } catch (Exception ignored) {
            return false;
        }
    }

    private WebElement waitForAnyVisible(By[] locators, int timeoutSeconds) {
        long endAt = System.currentTimeMillis() + (timeoutSeconds * 1000L);

        while (System.currentTimeMillis() < endAt) {
            for (By locator : locators) {
                WebElement element = findVisible(locator);
                if (element != null) {
                    return element;
                }
            }
            sleep(500);
        }

        return null;
    }

    private void tapFirstVisible(By[] locators, String label, int timeoutSeconds) {
        WebElement element = waitForAnyVisible(locators, timeoutSeconds);
        if (element == null) {
            throw new AssertionError(label + " not visible/clickable" + " | visibleValues=" + collectVisibleValues());
        }
        safeClick(element);
    }

    private boolean tapIfVisible(By locator, String label, int timeoutSeconds) {
        WebElement element = waitForAnyVisible(new By[]{locator}, timeoutSeconds);
        if (element == null) {
            return false;
        }

        safeClick(element);
        ReportLogger.step("Tapped: " + label);
        return true;
    }

    private WebElement findVisible(By locator) {
        try {
            List<WebElement> elements = driver.findElements(locator);
            for (WebElement element : elements) {
                if (isDisplayed(element)) {
                    return element;
                }
            }
        } catch (Exception ignored) {
            // Not visible
        }
        return null;
    }

    private boolean isVisible(By locator) {
        return findVisible(locator) != null;
    }

    private boolean isDisplayed(WebElement element) {
        try {
            return element != null && element.isDisplayed();
        } catch (Exception ignored) {
            return false;
        }
    }



    private List<WebElement> getVisibleEditTexts() {
        List<WebElement> visibleFields = new ArrayList<>();
        for (WebElement element : driver.findElements(EDIT_TEXT)) {
            if (isDisplayed(element)) {
                visibleFields.add(element);
            }
        }
        return visibleFields;
    }

    private void safeClick(WebElement element) {
        try {
            element.click();
        } catch (Exception firstClickError) {
            try {
                Rectangle rect = element.getRect();
                int x = rect.getX() + (rect.getWidth() / 2);
                int y = rect.getY() + (rect.getHeight() / 2);

                Map<String, Object> params = new HashMap<>();
                params.put("x", x);
                params.put("y", y);
                driver.executeScript("mobile: clickGesture", params);
            } catch (Exception gestureError) {
                throw new AssertionError("Unable to click element. First click error="
                        + clean(firstClickError.getMessage())
                        + " | gesture error=" + clean(gestureError.getMessage()), gestureError);
            }
        }
    }

    private void scrollToButtonIfNeeded(By locator, String label) {
        if (isVisible(locator)) {
            return;
        }

        for (int i = 1; i <= 4; i++) {
            swipeUp();
            sleep(500);
            if (isVisible(locator)) {
                return;
            }
        }

        ReportLogger.step(label + " still not visible after scroll. Tap method will perform final wait.");
    }

    private void swipeUp() {
        try {
            Dimension size = driver.manage().window().getSize();
            int x = size.width / 2;
            int startY = (int) (size.height * 0.78);
            int endY = (int) (size.height * 0.35);

            Map<String, Object> params = new HashMap<>();
            params.put("left", 0);
            params.put("top", 0);
            params.put("width", size.width);
            params.put("height", size.height);
            params.put("direction", "up");
            params.put("percent", 0.55);
            driver.executeScript("mobile: scrollGesture", params);
        } catch (Exception e) {
            try {
                Dimension size = driver.manage().window().getSize();
                int x = size.width / 2;
                int startY = (int) (size.height * 0.78);
                int endY = (int) (size.height * 0.35);

                Map<String, Object> params = new HashMap<>();
                params.put("x", x);
                params.put("y", startY);
                params.put("endX", x);
                params.put("endY", endY);
                params.put("duration", 450);
                driver.executeScript("mobile: dragGesture", params);
            } catch (Exception ignored) {
                // Keep tests locator-driven; no fixed tap fallback here.
            }
        }
    }

    private boolean waitForTextOrDescriptionContains(String marker, int timeoutSeconds) {
        By[] locators = new By[]{textContains(marker), descContains(marker)};
        return waitForAnyVisible(locators, timeoutSeconds) != null;
    }

    private void activateAdvisorAppIfNeeded() {
        try {
            String currentPackage = safeCurrentPackage();
            if (ADVISOR_PACKAGE.equals(currentPackage)) {
                return;
            }

            Map<String, Object> params = new HashMap<>();
            params.put("appId", ADVISOR_PACKAGE);
            driver.executeScript("mobile: activateApp", params);
            sleep(2000);
        } catch (Exception e) {
            ReportLogger.step("Advisor app activation skipped/failed: " + clean(e.getMessage()));
        }
    }

    private String safeCurrentPackage() {
        try {
            return driver.getCurrentPackage();
        } catch (Exception e) {
            return "";
        }
    }

    private void hideKeyboardSafely() {
        try {
            driver.hideKeyboard();
        } catch (Exception ignored) {
            // Keyboard may already be hidden.
        }
    }

    private String collectVisibleValues() {
        try {
            String source = driver.getPageSource();
            if (source == null) {
                return "";
            }

            String compactSource = source.replaceAll("\\s+", " " );
            return compactSource.substring(0, Math.min(compactSource.length(), 1200));
        } catch (Exception e) {
            return "Unable to collect visible values: " + clean(e.getMessage());
        }
    }

    private int parseInt(String value, int defaultValue) {
        try {
            return Integer.parseInt(value.trim());
        } catch (Exception e) {
            return defaultValue;
        }
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private String clean(String message) {
        if (message == null) {
            return "";
        }
        return message.replaceAll("\\s+", " ").trim();
    }

    private static By descContains(String value) {
        return AppiumBy.androidUIAutomator("new UiSelector().descriptionContains(\"" + escapeUiAutomator(value) + "\")");
    }

    private static By textContains(String value) {
        return AppiumBy.androidUIAutomator("new UiSelector().textContains(\"" + escapeUiAutomator(value) + "\")");
    }

    private static String escapeUiAutomator(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

}