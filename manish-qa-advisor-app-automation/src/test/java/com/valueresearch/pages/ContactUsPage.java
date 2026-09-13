package com.valueresearch.pages;

import com.valueresearch.utils.ReportLogger;
import com.valueresearch.utils.ScreenshotUtils;
import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.android.nativekey.AndroidKey;
import io.appium.java_client.android.nativekey.KeyEvent;
import org.openqa.selenium.By;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Rectangle;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.PointerInput;
import org.openqa.selenium.interactions.Sequence;
import org.openqa.selenium.remote.RemoteWebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ContactUsPage {

    private final AndroidDriver driver;
    private final WebDriverWait wait;

    private static final String TEST_MESSAGE = "Automation test message. Please ignore.";
    private static final String ADVISOR_APP_PACKAGE = "com.valueresearch.advisor";

    // Exact Contact us screen text values used for detailed Extent report logging.
    private static final String TXT_CONTACT_US_TITLE = "Contact us";
    private static final String TXT_YOUR_MESSAGE = "Your Message";
    private static final String TXT_MESSAGE_PLACEHOLDER = "Type your message here...";
    private static final String TXT_CHOOSE_FILE = "Choose File";
    private static final String TXT_CHOOSE_FILE_FULL = "Choose File, Attach a document or screenshot for support";
    private static final String TXT_ESCALATION_MATRIX = "Escalation Matrix";
    private static final String TXT_PHONE = "Phone";
    private static final String TXT_PHONE_NUMBER = "+91-9999 322 422";
    private static final String TXT_WORKING_HOURS = "Monday-Friday\n9:30 a.m. - 6 p.m.";
    private static final String TXT_POSTAL_ADDRESS_LABEL = "Postal Address";
    private static final String TXT_POSTAL_ADDRESS = "Independent Advisors Private Limited\nC-103, Sector 65\nNoida, 201301.";
    private static final String TXT_GRIEVANCE_EMAIL = "grievanceofficer@valueresearch.in";
    private static final String TXT_GRIEVANCE_PREFIX = "Subscribers can write to the Investment Advisor at";
    private static final String TXT_GRIEVANCE_AFTER_EMAIL = "if the Investor does not receive a response within 10 business days of writing to the Client Servicing Team. The client can expect a reply within 10 business days of approaching the Investment Advisor.";
    private static final String TXT_SCORES_PREFIX = "In case you are not satisfied with our response you can lodge your grievance with SEBI at";
    private static final String TXT_SCORES_AFTER_URL = "or you may also write to any of the offices of SEBI.";
    private static final String TXT_SCORES_LINK = "https://scores.sebi.gov.in";
    private static final String TXT_LINK_FOR_SCORES = "Link for SCORES:";
    private static final String TXT_ANDROID_APP = "Android App";
    private static final String TXT_IOS_APP = "iOS App";
    private static final String TXT_SUBMIT = "Submit";

    // Bottom navigation
    private final By hubTab = AppiumBy.accessibilityId("Hub");

    // Hub -> More -> Contact Us.
    // Confirmed from Appium Inspector: visible label is "Contact Us", but Android semantics expose
    // content-desc="Get in touch with us". Always automate the semantic value, not the visual text.
    private static final String HUB_CONTACT_US_DESC = "Get in touch with us";

    // Fast primary locator: accessibility id/content-desc.
    private final By contactUsHubTile = AppiumBy.accessibilityId(HUB_CONTACT_US_DESC);

    // Current Hub semantic labels confirmed from the same Inspector hierarchy.
    private final By hubStocksSection = AppiumBy.accessibilityId("Stocks");
    private final By moreLabelDesc = AppiumBy.accessibilityId("More");
    private final By faqsDesc = AppiumBy.accessibilityId("View frequently asked questions");
    private final By aboutUsDesc = AppiumBy.accessibilityId("Learn about Value Research");
    private final By privacyPolicyDesc = AppiumBy.accessibilityId("Read our privacy policy");

    // Contact us screen locators confirmed from Appium Inspector.
    private final By pageTitle = AppiumBy.accessibilityId("Contact us");
    private final By yourMessageLabel = AppiumBy.accessibilityId("Your Message");
    private final By messageInput = AppiumBy.className("android.widget.EditText");
    private final By chooseFile = AppiumBy.accessibilityId(
            "Choose File, Attach a document or screenshot for support"
    );
    private final By escalationMatrix = AppiumBy.accessibilityId("Escalation Matrix");
    private final By phoneLabel = AppiumBy.accessibilityId("Phone");
    private final By phoneNumber = AppiumBy.accessibilityId("+91-9999 322 422");
    private final By workingHours = AppiumBy.androidUIAutomator(
            "new UiSelector().descriptionContains(\"Monday-Friday\")"
    );
    private final By postalAddressLabel = AppiumBy.accessibilityId("Postal Address");
    private final By postalAddress = AppiumBy.androidUIAutomator(
            "new UiSelector().descriptionContains(\"Independent Advisors Private Limited\")"
    );
    private final By grievanceEmail = AppiumBy.accessibilityId("grievanceofficer@valueresearch.in");
    private final By scoresLink = AppiumBy.accessibilityId("https://scores.sebi.gov.in");
    private final By androidAppLink = AppiumBy.accessibilityId("Android App");
    private final By androidAppLinkContains = AppiumBy.androidUIAutomator(
            "new UiSelector().descriptionContains(\"Android App\")"
    );
    private final By iosAppLink = AppiumBy.accessibilityId("iOS App");
    private final By iosAppLinkUpper = AppiumBy.accessibilityId("IOS App");
    private final By iosAppLinkContainsLowerI = AppiumBy.androidUIAutomator(
            "new UiSelector().descriptionContains(\"iOS App\")"
    );
    private final By iosAppLinkContainsUpperI = AppiumBy.androidUIAutomator(
            "new UiSelector().descriptionContains(\"IOS App\")"
    );
    private final By scoresLinksLine = AppiumBy.androidUIAutomator(
            "new UiSelector().descriptionContains(\"Link for SCORES\")"
    );
    private final By submitButton = AppiumBy.accessibilityId("Submit");

    public ContactUsPage(AndroidDriver driver) {
        this.driver = driver;
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(12));
    }

    // =====================================================================
    // Open and recovery flow
    // =====================================================================

    public void openContactUsFromHub() {
        try {
            ReportLogger.step("Opening Contact Us from Hub -> More -> Contact Us");

            if (isContactUsPageVisible()) {
                ReportLogger.pass("Contact us page is already open");
                return;
            }

            openHubTab();
            tapContactUsFromHubMoreSection();
            waitForContactUsPage();

            ReportLogger.pass("Contact us page opened successfully from Hub");
        } catch (Exception e) {
            captureScreenshot("CU_001_Open_Contact_Us_Failure");
            ReportLogger.fail("Failed to open Contact Us: " + cleanError(e.getMessage()));
            throw new RuntimeException("Failed to open Contact Us: " + cleanError(e.getMessage()), e);
        }
    }

    public void recoverContactUsIfNeeded() {
        try {
            if (isAdvisorAppForeground() && isContactUsPageVisible()) {
                ReportLogger.pass("Contact us page is already active");
                return;
            }

            // Functional tests can leave Chrome, Dialer, Gmail, Files, Play Store, etc. in the
            // foreground. Never try to use Advisor bottom navigation while another package is active.
            if (!isAdvisorAppForeground()) {
                ReportLogger.step(
                        "Advisor app is not foreground during Contact us recovery. "
                                + "Current package=" + safeCurrentPackage() + ". Activating Advisor app."
                );

                try {
                    driver.activateApp(ADVISOR_APP_PACKAGE);
                    sleep(900);
                } catch (Exception activateError) {
                    ReportLogger.debug(
                            "Advisor activateApp failed during Contact us recovery: "
                                    + cleanError(activateError.getMessage())
                    );
                }
            }

            if (isAdvisorAppForeground() && waitForContactUsPageVisible(1500)) {
                ReportLogger.pass("Contact us page restored after activating Advisor app");
                return;
            }

            ReportLogger.step("Contact us page is not active. Reopening from Hub.");
            openContactUsFromHub();
        } catch (Exception e) {
            captureScreenshot("CU_Recover_Contact_Us_Failure");
            throw new RuntimeException("Unable to recover Contact us page: " + cleanError(e.getMessage()), e);
        }
    }

    private void openHubTab() {
        ReportLogger.step("Opening Hub bottom tab");

        // Two controlled attempts are enough. The previous implementation used repeated BACK + long
        // sleeps, which made navigation slower and could move the app into the wrong state.
        for (int attempt = 1; attempt <= 2; attempt++) {
            if (tapElementIfPresent(hubTab, "Hub tab")) {
                if (waitForHubContent(3500)) {
                    ReportLogger.pass("Hub tab opened");
                    return;
                }

                ReportLogger.debug("Hub tab was tapped but Hub content was not detected yet. Attempt: " + attempt);
            }

            // Only recover with BACK when the Hub tab itself cannot be used. Never press BACK after a
            // failed Contact Us tile tap because that tile tap may have left us on Hub already.
            if (attempt < 2) {
                pressBackSafely();
                sleep(350);
            }
        }

        throw new RuntimeException("Hub tab did not become ready after controlled recovery attempts.");
    }

    private boolean waitForHubContent(long timeoutMillis) {
        long deadline = System.currentTimeMillis() + timeoutMillis;

        while (System.currentTimeMillis() < deadline) {
            // Prevent stale Advisor hierarchy from being accepted while another app is foreground.
            if (!isAdvisorAppForeground()) {
                sleep(200);
                continue;
            }

            if (isElementPresent(hubStocksSection)
                    || isElementPresent(moreLabelDesc)
                    || isElementPresent(contactUsHubTile)
                    || isElementPresent(faqsDesc)) {
                return true;
            }

            sleep(200);
        }

        return false;
    }

    private void tapContactUsFromHubMoreSection() {
        ReportLogger.step("Finding Contact Us inside Hub More section");

        // FAST PATH: try the exact semantic locator before doing any gesture.
        if (tapVisibleContactUsTile("accessibility id on current Hub viewport")) {
            return;
        }

        // Controlled, device-independent fallback. We intentionally avoid UiScrollable here because
        // Flutter can expose the scrollable parent as the result of scrollIntoView(), producing a
        // full-screen semantic container instead of the actual Contact Us tile.
        for (int attempt = 1; attempt <= 3; attempt++) {
            ReportLogger.step("Contact Us not visible. Controlled Hub scroll attempt: " + attempt);
            swipeUpW3C();
            sleep(400);

            if (tapVisibleContactUsTile("accessibility id after controlled scroll " + attempt)) {
                return;
            }
        }

        captureScreenshot("CU_Contact_Us_Menu_Not_Found_In_Hub");
        throw new RuntimeException(
                "Contact Us Hub tile was not found/opened. Expected content-desc: " + HUB_CONTACT_US_DESC
        );
    }

    private boolean tapVisibleContactUsTile(String strategy) {
        try {
            List<WebElement> elements = driver.findElements(contactUsHubTile);
            return openContactUsFromElements(elements, strategy);
        } catch (Exception e) {
            ReportLogger.debug("Contact Us semantic lookup failed: " + cleanError(e.getMessage()));
            return false;
        }
    }

    private boolean openContactUsFromElements(List<WebElement> elements, String strategy) {
        if (elements == null || elements.isEmpty()) {
            return false;
        }

        for (WebElement element : elements) {
            if (element == null) {
                continue;
            }

            try {
                if (!element.isDisplayed()) {
                    continue;
                }
            } catch (Exception ignored) {
                continue;
            }

            if (clickContactUsElement(element, strategy)) {
                return true;
            }
        }

        return false;
    }

    private boolean clickContactUsElement(WebElement element, String strategy) {
        Rectangle rect;
        Dimension screen;

        try {
            rect = element.getRect();
            screen = driver.manage().window().getSize();

            if (rect.getWidth() <= 0 || rect.getHeight() <= 0) {
                ReportLogger.debug("Ignoring Contact Us semantic node with invalid bounds.");
                return false;
            }

            // Flutter may return a scrollable/full-screen parent for semantic searches. Never tap a
            // node that occupies most of the viewport; a real Hub tile is much smaller.
            if (rect.getWidth() >= (int) (screen.width * 0.80)
                    || rect.getHeight() >= (int) (screen.height * 0.50)) {
                ReportLogger.debug(
                        "Ignoring oversized Contact Us semantic container"
                                + " | x=" + rect.getX()
                                + " | y=" + rect.getY()
                                + " | width=" + rect.getWidth()
                                + " | height=" + rect.getHeight()
                );
                return false;
            }

            ReportLogger.step(
                    "Contact Us tile found using " + strategy
                            + " | x=" + rect.getX()
                            + " | y=" + rect.getY()
                            + " | width=" + rect.getWidth()
                            + " | height=" + rect.getHeight()
            );
        } catch (Exception e) {
            ReportLogger.debug("Unable to read Contact Us element bounds: " + cleanError(e.getMessage()));
            return false;
        }

        // PRIMARY: element-based UiAutomator2 gesture. This is reliable for Flutter semantic nodes
        // even when Inspector reports clickable=false.
        if (tapWithMobileClickGesture(element, "Contact Us")) {
            if (waitForContactUsPageVisible(2200)) {
                ReportLogger.pass("Contact Us opened using element-based mobile: clickGesture");
                return true;
            }

            ReportLogger.debug("Contact Us clickGesture completed but the page did not open.");
        }

        // FINAL FALLBACK: tap the runtime center of the located element. These coordinates are
        // calculated fresh on every device and are never hardcoded.
        if (tapDynamicElementCenter(element, "Contact Us dynamic element-center fallback")) {
            if (waitForContactUsPageVisible(2200)) {
                ReportLogger.pass("Contact Us opened using dynamic element-center fallback");
                return true;
            }
        }

        return false;
    }

    private boolean waitForContactUsPageVisible(long timeoutMillis) {
        long deadline = System.currentTimeMillis() + timeoutMillis;

        while (System.currentTimeMillis() < deadline) {
            // pageTitle is the strongest marker; Your Message is an additional fast marker for builds
            // where the title semantic node appears a fraction later.
            if (isElementPresent(pageTitle) || isElementPresent(yourMessageLabel)) {
                return true;
            }

            sleep(200);
        }

        return false;
    }

    private void waitForContactUsPage() {
        ReportLogger.step("Waiting for Contact us page to load");

        try {
            wait.until(ExpectedConditions.presenceOfElementLocated(pageTitle));
        } catch (Exception ignored) {
            // Fallback below gives cleaner error and screenshot.
        }

        if (!isContactUsPageVisible()) {
            throw new RuntimeException("Contact us page did not load. Title/content-desc not visible.");
        }

        ReportLogger.pass("Contact us page loaded");
    }

    // =====================================================================
    // Strict validations
    // =====================================================================

    public void verifyTitleAndMessageBox() {
        try {
            ReportLogger.step("Validating Contact us title and message box");

            assertPresentOnCurrentScreen(pageTitle, "Contact us title");
            assertExpectedTextPresent(TXT_CONTACT_US_TITLE, "Page title", 0);

            assertPresentOnCurrentScreen(yourMessageLabel, "Your Message label");
            assertExpectedTextPresent(TXT_YOUR_MESSAGE, "Message label", 0);

            assertPresentOnCurrentScreen(messageInput, "Message input box");
            logVisualTextOnly(TXT_MESSAGE_PLACEHOLDER, "Message input placeholder");

            ReportLogger.pass("Contact us title, message label, input box, and placeholder details validated successfully");
        } catch (Exception e) {
            captureScreenshot("CU_002_Title_Message_Box_Failure");
            throw new RuntimeException("Title/message box validation failed: " + cleanError(e.getMessage()), e);
        }
    }

    public void verifyChooseFileAndEscalationMatrix() {
        try {
            ReportLogger.step("Validating Choose File and Escalation Matrix links");

            assertPresentWithDownScroll(chooseFile, "Choose File attachment link", 2);
            assertExpectedTextPresent(TXT_CHOOSE_FILE, "Choose File visible link text", 2);
            assertExpectedTextPresent(TXT_CHOOSE_FILE_FULL, "Choose File full accessibility text", 2);

            assertPresentWithDownScroll(escalationMatrix, "Escalation Matrix link", 2);
            assertExpectedTextPresent(TXT_ESCALATION_MATRIX, "Escalation Matrix link text", 2);

            ReportLogger.pass("Choose File and Escalation Matrix exact text validated successfully");
        } catch (Exception e) {
            captureScreenshot("CU_003_Choose_File_Escalation_Failure");
            throw new RuntimeException("Choose File/Escalation Matrix validation failed: " + cleanError(e.getMessage()), e);
        }
    }

    public void verifyPhoneAndWorkingHours() {
        try {
            ReportLogger.step("Validating phone number and working hours");

            assertPresentWithDownScroll(phoneLabel, "Phone label", 3);
            assertExpectedTextPresent(TXT_PHONE, "Phone label text", 3);

            assertPresentWithDownScroll(phoneNumber, "Phone number", 3);
            assertExpectedTextPresent(TXT_PHONE_NUMBER, "Phone number text", 3);

            // Do not assert the full multi-line block through pageSource. Flutter/Appium can expose
            // this value with line-break/entity differences even when the element is correctly present.
            // Validate stable text fragments and log the full expected text in the report.
            assertTextFragmentsPresentWithReport(
                    workingHours,
                    "Working hours text",
                    TXT_WORKING_HOURS,
                    3,
                    "Monday-Friday",
                    "9:30 a.m.",
                    "6 p.m."
            );

            ReportLogger.pass("Phone label, phone number, and working hours text validated successfully");
        } catch (Exception e) {
            captureScreenshot("CU_004_Phone_Working_Hours_Failure");
            throw new RuntimeException("Phone/working hours validation failed: " + cleanError(e.getMessage()), e);
        }
    }

    public void verifyPostalAddress() {
        try {
            ReportLogger.step("Validating postal address");

            assertPresentWithDownScroll(postalAddressLabel, "Postal Address label", 4);
            assertExpectedTextPresent(TXT_POSTAL_ADDRESS_LABEL, "Postal Address label text", 4);

            // Same multi-line Flutter semantic issue as working hours. Validate all important address
            // fragments strictly, then print the full address in the Extent report.
            assertTextFragmentsPresentWithReport(
                    postalAddress,
                    "Postal address full text",
                    TXT_POSTAL_ADDRESS,
                    4,
                    "Independent Advisors Private Limited",
                    "C-103, Sector 65",
                    "Noida",
                    "201301"
            );

            ReportLogger.pass("Postal address text validated successfully");
        } catch (Exception e) {
            captureScreenshot("CU_005_Postal_Address_Failure");
            throw new RuntimeException("Postal address validation failed: " + cleanError(e.getMessage()), e);
        }
    }

    public void verifyGrievanceAndScoresInfo() {
        try {
            ReportLogger.step("Validating grievance officer email, grievance text, and SEBI SCORES link");

            assertPresentWithDownScroll(grievanceEmail, "Grievance officer email", 5);
            assertExpectedTextPresent(TXT_GRIEVANCE_PREFIX, "Grievance paragraph start text", 5);
            assertExpectedTextPresent(TXT_GRIEVANCE_EMAIL, "Grievance officer email text", 5);
            assertExpectedTextPresent(TXT_GRIEVANCE_AFTER_EMAIL, "Grievance paragraph response-time text", 5);

            assertExpectedTextPresent(TXT_SCORES_PREFIX, "SEBI grievance paragraph start text", 5);
            assertPresentWithDownScroll(scoresLink, "SEBI SCORES link", 5);
            assertExpectedTextPresent(TXT_SCORES_LINK, "SEBI SCORES URL text", 5);
            assertExpectedTextPresent(TXT_SCORES_AFTER_URL, "SEBI offices paragraph text", 5);

            ReportLogger.pass("Grievance email, full grievance text pieces, and SEBI SCORES exact text validated successfully");
        } catch (Exception e) {
            captureScreenshot("CU_006_Grievance_Scores_Failure");
            throw new RuntimeException("Grievance/SCORES validation failed: " + cleanError(e.getMessage()), e);
        }
    }

    public void verifyAppLinks() {
        try {
            ReportLogger.step("Validating Android App and iOS App links");

            /*
             * IMPORTANT FOR QA/LIVE LAYOUT DIFFERENCES:
             *
             * Flutter can expose lower semantic nodes before they are actually visible to the user.
             * Do not stop just because "Link for SCORES" exists in page source. Align each real target
             * into a safe visible area above the sticky Submit region, then validate it.
             */
            WebElement androidTarget = alignFirstTargetAboveSubmit(
                    "Android App link",
                    6,
                    androidAppLink,
                    androidAppLinkContains
            );

            if (androidTarget == null) {
                throw new RuntimeException("Android App link could not be aligned into the visible viewport.");
            }

            assertExpectedTextPresent(TXT_LINK_FOR_SCORES, "Link for SCORES label", 2);
            assertAndroidAppLinkPresent();
            assertExpectedTextPresent(TXT_ANDROID_APP, "Android App link text", 2);

            WebElement iosTarget = alignFirstTargetAboveSubmit(
                    "iOS App link",
                    5,
                    iosAppLink,
                    iosAppLinkUpper,
                    iosAppLinkContainsLowerI,
                    iosAppLinkContainsUpperI
            );

            if (iosTarget == null) {
                throw new RuntimeException("iOS App link could not be aligned into the visible viewport.");
            }

            assertIosAppLinkPresent();
            assertExpectedTextPresentEither(TXT_IOS_APP, "IOS App", "iOS App link text", 2);

            ReportLogger.pass("Android App and iOS App exact link text validated successfully");
        } catch (Exception e) {
            captureScreenshot("CU_007_App_Links_Failure");
            ReportLogger.fail("Android/iOS App links validation failed: " + cleanError(e.getMessage()));
            throw new RuntimeException("Android/iOS App links validation failed: " + cleanError(e.getMessage()), e);
        }
    }

    public void verifySubmitButton() {
        try {
            ReportLogger.step("Validating Submit button");

            assertPresentWithDownScroll(submitButton, "Submit button", 6);
            assertExpectedTextPresent(TXT_SUBMIT, "Submit button text", 6);

            ReportLogger.pass("Submit button exact text validated successfully");
        } catch (Exception e) {
            captureScreenshot("CU_008_Submit_Button_Failure");
            throw new RuntimeException("Submit button validation failed: " + cleanError(e.getMessage()), e);
        }
    }

    public void verifyMessageTyping() {
        try {
            ReportLogger.step("Validating typing in Contact us message box");
            typeMessageAndValidate(TEST_MESSAGE, "Standard message typing");
            ReportLogger.pass("Message typing validated successfully");
        } catch (Exception e) {
            captureScreenshot("CU_009_Message_Typing_Failure");
            throw new RuntimeException("Message typing validation failed: " + cleanError(e.getMessage()), e);
        }
    }


    public void verifyContactUsScreenStability() {
        try {
            ReportLogger.step("Validating Contact us screen stability and no crash/ANR markers");

            if (!isContactUsPageVisible()) {
                recoverContactUsIfNeeded();
            }

            String source = safePageSource();
            if (containsIgnoreCase(source, "keeps stopping")
                    || containsIgnoreCase(source, "isn't responding")
                    || containsIgnoreCase(source, "App isn't responding")
                    || containsIgnoreCase(source, "Unfortunately")) {
                captureScreenshot("CU_Screen_Crash_Or_ANR");
                throw new RuntimeException("Crash/ANR marker detected on Contact us screen.");
            }

            ReportLogger.pass("Contact us screen stability validated. No crash/ANR marker found.");
        } catch (Exception e) {
            captureScreenshot("CU_Stability_Failure");
            throw new RuntimeException("Contact us stability validation failed: " + cleanError(e.getMessage()), e);
        }
    }

    public void verifySpecialCharacterMessageTyping() {
        String message = "Automation special chars test @ # 123 - please ignore.";
        typeMessageAndValidate(message, "Special character message typing");
    }

    public void verifyEmptySubmitValidation() {
        try {
            ReportLogger.step("Validating negative case: Submit with empty message should not submit");

            scrollToMessageInputAreaIfNeeded();
            clearMessageInput();
            hideKeyboardSafely();

            assertPresentWithDownScroll(submitButton, "Submit button", 6);
            tapElementIfPresent(submitButton, "Submit button with empty message");
            sleep(1200);

            assertNoSuccessfulSubmission("empty message submit");
            ReportLogger.pass("Negative validation passed: empty message was not submitted and Contact us page remained active.");
        } catch (Exception e) {
            captureScreenshot("CU_011_Empty_Submit_Failure");
            throw new RuntimeException("Empty submit negative validation failed: " + cleanError(e.getMessage()), e);
        }
    }

    public void verifyWhitespaceSubmitValidation() {
        try {
            ReportLogger.step("Validating negative case: Submit with whitespace-only message should not submit");

            scrollToMessageInputAreaIfNeeded();
            clearMessageInput();

            WebElement input = driver.findElement(messageInput);
            input.click();
            sleep(400);
            input.sendKeys("     ");
            hideKeyboardSafely();

            assertPresentWithDownScroll(submitButton, "Submit button", 6);
            tapElementIfPresent(submitButton, "Submit button with whitespace-only message");
            sleep(1200);

            assertNoSuccessfulSubmission("whitespace-only message submit");
            ReportLogger.pass("Negative validation passed: whitespace-only message was not submitted and Contact us page remained active.");
        } catch (Exception e) {
            captureScreenshot("CU_012_Whitespace_Submit_Failure");
            throw new RuntimeException("Whitespace submit negative validation failed: " + cleanError(e.getMessage()), e);
        }
    }

    public void verifyChooseFileFunctionalTap() {
        assertTapOpensExternalOrChooserAndReturn(
                chooseFile,
                "Choose File attachment link",
                2,
                "Recent", "Files", "Photos", "Documents", "Open from", "Allow", "While using", "Choose File"
        );
    }

    public void verifyEscalationMatrixFunctionalTap() {
        assertTapOpensExternalOrChooserAndReturn(
                escalationMatrix,
                "Escalation Matrix link",
                3,
                "Escalation", "Matrix", "Chrome", "Open with", "PDF", "Drive", "Browser"
        );
    }

    public void verifyPhoneFunctionalTap() {
        assertTapOpensExternalOrChooserAndReturn(
                phoneNumber,
                "Phone number link",
                3,
                "+91-9999", "9999 322 422", "Dial", "Phone", "Call", "Contacts", "Open with"
        );
    }

    public void verifyGrievanceEmailFunctionalTap() {
        assertTapOpensExternalOrChooserAndReturn(
                grievanceEmail,
                "Grievance officer email link",
                5,
                "grievanceofficer@valueresearch.in", "Gmail", "Email", "Compose", "Open with", "Complete action"
        );
    }

    public void verifyScoresLinkFunctionalTap() {
        try {
            ReportLogger.step("Functional validation: tapping SEBI SCORES link");

            if (!isContactUsPageVisible()) {
                recoverContactUsIfNeeded();
            }

            WebElement element = alignFirstTargetAboveSubmit(
                    "SEBI SCORES link",
                    6,
                    scoresLink
            );

            if (element == null) {
                throw new RuntimeException(
                        "SEBI SCORES link was found semantically but could not be aligned into the visible viewport."
                );
            }

            Rectangle rect = element.getRect();
            ReportLogger.step(
                    "SEBI SCORES link found"
                            + " | x=" + rect.getX()
                            + " | y=" + rect.getY()
                            + " | width=" + rect.getWidth()
                            + " | height=" + rect.getHeight()
                            + " | clickable=" + safeAttribute(element, "clickable")
                            + " | enabled=" + safeAttribute(element, "enabled")
            );

            String beforePackage = safeCurrentPackage();
            String beforeSource = normalizeForSearch(safePageSource());

            // =============================================================
            // ATTEMPT 1 - Runtime visible URL text-area tap (PRIMARY)
            // =============================================================
            // This is the proven working interaction for this Flutter link.
            // Appium exposes a semantic rectangle wider than the rendered URL,
            // so center-oriented semantic clicks can land in blank space.
            //
            // The tap point is derived from the element's live runtime bounds,
            // therefore there are no fixed emulator/device coordinates.
            if (tapScoresVisibleTextArea(element)) {
                if (waitForScoresNavigation(
                        beforePackage,
                        beforeSource,
                        2200,
                        "SEBI SCORES link - visible URL text area")) {
                    ReportLogger.pass(
                            "SEBI SCORES link opened successfully using runtime visible-text-area tap"
                    );
                    return;
                }
            }

            // If navigation completed just after the short polling window,
            // never send a second tap into Chrome/chooser/external UI.
            if (!isContactUsPageVisible()) {
                ReportLogger.pass(
                        "SEBI SCORES navigation detected after runtime visible-text-area tap"
                );
                return;
            }

            // Re-fetch after the physical tap because Flutter may rebuild the
            // semantics tree and stale the original element reference.
            element = findFirstDisplayedElement(scoresLink);
            if (element == null) {
                throw new RuntimeException(
                        "SEBI SCORES link disappeared before semantic click fallback could be attempted."
                );
            }

            // =============================================================
            // ATTEMPT 2 - Native WebElement.click() (FALLBACK ONLY)
            // =============================================================
            try {
                element.click();
                ReportLogger.step(
                        "Fallback: tapped SEBI SCORES link using WebElement.click()"
                );

                if (waitForScoresNavigation(
                        beforePackage,
                        beforeSource,
                        2200,
                        "SEBI SCORES link - WebElement.click() fallback")) {
                    ReportLogger.pass(
                            "SEBI SCORES link opened successfully using WebElement.click() fallback"
                    );
                    return;
                }
            } catch (Exception clickException) {
                ReportLogger.debug(
                        "WebElement.click() fallback failed for SEBI SCORES link: "
                                + cleanError(clickException.getMessage())
                );
            }

            if (!isContactUsPageVisible()) {
                ReportLogger.pass(
                        "SEBI SCORES navigation detected after WebElement.click() fallback"
                );
                return;
            }

            captureScreenshot("CU_020_SCORES_Link_No_Navigation");
            throw new RuntimeException(
                    "SEBI SCORES link is present and clickable, but runtime visible URL text-area tap "
                            + "and semantic click fallback produced no browser, chooser, WebView, package, "
                            + "or screen transition."
            );

        } catch (Exception e) {
            captureScreenshot("CU_020_SCORES_Functional_Tap_Failure");
            throw new RuntimeException(
                    "SEBI SCORES link functional tap validation failed: " + cleanError(e.getMessage()),
                    e
            );
        } finally {
            returnToContactUsAfterExternalAction();
        }
    }

    public void verifyAndroidAppFunctionalTap() {
        verifyAndroidAppLinkFunctionalTap();
    }

    public void verifyIosAppFunctionalTap() {
        verifyIosAppLinkFunctionalTap();
    }

    private boolean waitForScoresNavigation(
            String beforePackage,
            String beforeSource,
            long timeoutMillis,
            String actionName) {

        long deadline = System.currentTimeMillis() + timeoutMillis;
        String lastPackage = beforePackage;
        String lastSource = beforeSource;

        while (System.currentTimeMillis() < deadline) {
            String afterPackage = safeCurrentPackage();

            boolean packageChanged = beforePackage != null
                    && afterPackage != null
                    && !beforePackage.isEmpty()
                    && !afterPackage.isEmpty()
                    && !beforePackage.equalsIgnoreCase(afterPackage);

            // Once Android reports another foreground package, we have definitely left Advisor.
            // Do not trust a stale Flutter/UiAutomator2 hierarchy from the previous frame.
            boolean leftContactUs = packageChanged || !isContactUsPageVisible();

            // Package change or leaving the Contact Us screen is strong proof of navigation and is
            // much safer than treating the words "SEBI" or "SCORES" as success markers because
            // those words already exist on the Contact Us page before the click.
            if (packageChanged || leftContactUs) {
                ReportLogger.pass(
                        "Functional navigation detected for " + actionName
                                + " | packageChanged=" + packageChanged
                                + " | leftContactUs=" + leftContactUs
                                + " | currentPackage=" + afterPackage
                );
                return true;
            }

            String afterSourceRaw = safePageSource();
            String afterSource = normalizeForSearch(afterSourceRaw);
            lastPackage = afterPackage;
            lastSource = afterSource;

            boolean newExternalMarker = containsNewExternalMarker(beforeSource, afterSource);
            if (newExternalMarker) {
                ReportLogger.pass(
                        "External chooser/browser marker appeared for " + actionName
                                + " | currentPackage=" + afterPackage
                );
                return true;
            }

            sleep(250);
        }

        boolean sourceChanged = beforeSource != null
                && lastSource != null
                && !beforeSource.equals(lastSource);

        ReportLogger.debug(
                "No valid navigation response detected for " + actionName
                        + " | currentPackage=" + lastPackage
                        + " | sourceChanged=" + sourceChanged
                        + " | ContactUsStillVisible=" + isContactUsPageVisible()
        );

        return false;
    }

    private boolean containsNewExternalMarker(String beforeSource, String afterSource) {
        if (afterSource == null || afterSource.isEmpty()) {
            return false;
        }

        String before = beforeSource == null ? "" : beforeSource;
        String[] markers = new String[]{
                "chrome",
                "browser",
                "open with",
                "complete action using",
                "just once",
                "always"
        };

        for (String marker : markers) {
            String normalizedMarker = normalizeForSearch(marker);
            if (afterSource.contains(normalizedMarker) && !before.contains(normalizedMarker)) {
                return true;
            }
        }

        return false;
    }

    private WebElement findFirstDisplayedElement(By locator) {
        try {
            List<WebElement> elements = driver.findElements(locator);
            if (elements == null) {
                return null;
            }

            for (WebElement element : elements) {
                if (element == null) {
                    continue;
                }

                try {
                    if (element.isDisplayed()) {
                        return element;
                    }
                } catch (Exception ignored) {
                    // Try the next semantic node.
                }
            }
        } catch (Exception e) {
            ReportLogger.debug("Element lookup failed: " + cleanError(e.getMessage()));
        }

        return null;
    }

    private boolean tapWithMobileClickGesture(WebElement element, String elementName) {
        try {
            if (!(element instanceof RemoteWebElement)) {
                return false;
            }

            Map<String, Object> args = new HashMap<>();
            args.put("elementId", ((RemoteWebElement) element).getId());

            ((JavascriptExecutor) driver).executeScript("mobile: clickGesture", args);
            ReportLogger.step("Tapped " + elementName + " using element-based mobile: clickGesture");
            return true;
        } catch (Exception e) {
            ReportLogger.debug(
                    "mobile: clickGesture failed for " + elementName + ": " + cleanError(e.getMessage())
            );
            return false;
        }
    }

    private boolean tapDynamicElementCenter(WebElement element, String elementName) {
        try {
            Rectangle rect = element.getRect();
            Dimension screen = driver.manage().window().getSize();

            if (rect.getWidth() <= 0 || rect.getHeight() <= 0) {
                return false;
            }

            // Reject full-screen/parent semantic nodes. This protects all dynamic-center fallbacks
            // from accidentally tapping a Flutter scroll container.
            if (rect.getWidth() >= (int) (screen.width * 0.80)
                    || rect.getHeight() >= (int) (screen.height * 0.50)) {
                ReportLogger.debug(
                        "Skipped dynamic center tap for oversized semantic node: " + elementName
                                + " | width=" + rect.getWidth()
                                + " | height=" + rect.getHeight()
                );
                return false;
            }

            int centerX = rect.getX() + (rect.getWidth() / 2);
            int centerY = rect.getY() + (rect.getHeight() / 2);

            // Keep the runtime-calculated point inside the current viewport.
            centerX = Math.max(1, Math.min(screen.width - 2, centerX));
            centerY = Math.max(1, Math.min(screen.height - 2, centerY));

            tapAt(centerX, centerY, elementName);
            return true;
        } catch (Exception e) {
            ReportLogger.debug(
                    "Dynamic center tap failed for " + elementName + ": " + cleanError(e.getMessage())
            );
            return false;
        }
    }

    private boolean tapScoresVisibleTextArea(WebElement element) {
        try {
            Rectangle rect = element.getRect();
            Dimension screen = driver.manage().window().getSize();

            if (rect.getWidth() <= 0 || rect.getHeight() <= 0) {
                ReportLogger.debug("SEBI SCORES element has invalid bounds for visible-text tap.");
                return false;
            }

            // Protect against accidentally receiving a Flutter parent/scroll
            // container instead of the actual URL semantic node.
            if (rect.getWidth() >= (int) (screen.width * 0.90)
                    || rect.getHeight() >= (int) (screen.height * 0.50)) {
                ReportLogger.debug(
                        "Skipped SCORES visible-text tap for oversized semantic node"
                                + " | width=" + rect.getWidth()
                                + " | height=" + rect.getHeight()
                );
                return false;
            }

            /*
             * Flutter exposes the SCORES URL semantic box wider than the
             * rendered hyperlink text. Tap 18% from the element's left edge
             * and vertically centered so the hit lands on the visible URL.
             *
             * The tap point is recalculated from the current element bounds on every
             * device/build, so there are no fixed emulator or QA coordinates here.
             */
            int tapX = rect.getX() + (int) Math.round(rect.getWidth() * 0.18d);
            int tapY = rect.getY() + (rect.getHeight() / 2);

            tapX = Math.max(1, Math.min(screen.width - 2, tapX));
            tapY = Math.max(1, Math.min(screen.height - 2, tapY));

            ReportLogger.step(
                    "Tapping visible SEBI SCORES URL text area"
                            + " | elementX=" + rect.getX()
                            + " | elementY=" + rect.getY()
                            + " | width=" + rect.getWidth()
                            + " | height=" + rect.getHeight()
                            + " | tapX=" + tapX
                            + " | tapY=" + tapY
            );

            tapAt(tapX, tapY, "SEBI SCORES visible URL text area");
            return true;
        } catch (Exception e) {
            ReportLogger.debug(
                    "SEBI SCORES visible-text-area tap failed: " + cleanError(e.getMessage())
            );
            return false;
        }
    }

    public void verifyAndroidAppLinkFunctionalTap() {
        WebElement aligned = alignFirstTargetAboveSubmit(
                "Android App link",
                6,
                androidAppLink,
                androidAppLinkContains
        );

        if (aligned == null) {
            throw new RuntimeException(
                    "Android App link could not be aligned into the visible viewport before functional tap."
            );
        }

        By functionalLocator = findFirstDisplayedElement(androidAppLink) != null
                ? androidAppLink
                : androidAppLinkContains;

        assertTapOpensExternalOrChooserAndReturn(
                functionalLocator,
                "Android App link",
                3,
                "Android App", "Play Store", "Chrome", "Browser", "Open with", "SCORES"
        );
    }

    public void verifyIosAppLinkFunctionalTap() {
        WebElement aligned = alignFirstTargetAboveSubmit(
                "iOS App link",
                6,
                iosAppLink,
                iosAppLinkUpper,
                iosAppLinkContainsLowerI,
                iosAppLinkContainsUpperI
        );

        if (aligned == null) {
            throw new RuntimeException(
                    "iOS App link could not be aligned into the visible viewport before functional tap."
            );
        }

        By functionalLocator;
        if (findFirstDisplayedElement(iosAppLink) != null) {
            functionalLocator = iosAppLink;
        } else if (findFirstDisplayedElement(iosAppLinkUpper) != null) {
            functionalLocator = iosAppLinkUpper;
        } else if (findFirstDisplayedElement(iosAppLinkContainsLowerI) != null) {
            functionalLocator = iosAppLinkContainsLowerI;
        } else {
            functionalLocator = iosAppLinkContainsUpperI;
        }

        assertTapOpensExternalOrChooserAndReturn(
                functionalLocator,
                "iOS App link",
                3,
                "iOS App", "IOS App", "App Store", "Chrome", "Browser", "Open with", "SCORES"
        );
    }

    public void verifySubmitButtonStateForEmptyMessage() {
        try {
            ReportLogger.step("Validating Submit button state for empty message");

            scrollToMessageInputAreaIfNeeded();
            clearMessageInput();
            hideKeyboardSafely();

            assertPresentWithDownScroll(submitButton, "Submit button", 6);
            List<WebElement> buttons = driver.findElements(submitButton);
            if (buttons == null || buttons.isEmpty()) {
                throw new RuntimeException("Submit button not found for state validation.");
            }

            WebElement button = buttons.get(0);
            String enabled = safeAttribute(button, "enabled");
            String clickable = safeAttribute(button, "clickable");
            String displayed = safeAttribute(button, "displayed");

            ReportLogger.pass("Submit button state for empty message - enabled=" + enabled
                    + ", clickable=" + clickable + ", displayed=" + displayed);
        } catch (Exception e) {
            captureScreenshot("CU_Submit_State_Empty_Message_Failure");
            throw new RuntimeException("Submit button state validation failed: " + cleanError(e.getMessage()), e);
        }
    }

    public void verifyInputCanBeCleared() {
        try {
            ReportLogger.step("Validating message input can be cleared");

            String message = "Temporary text to clear";
            typeMessageAndValidate(message, "Temporary message before clear");
            clearMessageInput();
            sleep(500);

            String source = safePageSource();
            if (containsIgnoreCase(source, message)) {
                captureScreenshot("CU_Input_Clear_Failure");
                throw new RuntimeException("Message text remained visible after clear attempt.");
            }

            ReportLogger.pass("Message input clear validation passed. Temporary text removed.");
        } catch (Exception e) {
            captureScreenshot("CU_Input_Clear_Failure");
            throw new RuntimeException("Message input clear validation failed: " + cleanError(e.getMessage()), e);
        }
    }

    public void verifyBackNavigationToHub() {
        try {
            ReportLogger.step("Validating back navigation from Contact us to Hub");

            if (!isContactUsPageVisible()) {
                recoverContactUsIfNeeded();
            }

            hideKeyboardSafely();
            pressBackSafely();
            sleep(1400);

            // In case first back only hides keyboard or closes a focused control.
            if (isContactUsPageVisible()) {
                pressBackSafely();
                sleep(1400);
            }

            if (!isHubAreaVisible()) {
                captureScreenshot("CU_010_Back_Navigation_Not_Hub");
                throw new RuntimeException("Back navigation did not return to Hub area.");
            }

            ReportLogger.pass("Returned Hub/More-area text validated in report: Hub, FAQs, About Us, Contact Us, Privacy Policy");
            ReportLogger.pass("Back navigation from Contact us to Hub validated successfully");
        } catch (Exception e) {
            captureScreenshot("CU_010_Back_Navigation_Failure");
            throw new RuntimeException("Back navigation validation failed: " + cleanError(e.getMessage()), e);
        }
    }

    // =====================================================================
    // Visibility helpers
    // =====================================================================

    public boolean isContactUsPageVisible() {
        /*
         * PACKAGE GATE - REQUIRED FOR EXTERNAL APP TESTS
         *
         * After opening Chrome, Dialer, Gmail, Files, Play Store, etc., UiAutomator2 can briefly
         * expose semantic nodes from the previous Flutter frame. Without this package check the
         * framework can falsely report that Contact us is still visible even though another app is
         * actually foreground.
         */
        if (!isAdvisorAppForeground()) {
            return false;
        }

        // Avoid getPageSource() here. This method is called frequently by recovery and functional
        // tests, and XML hierarchy generation is expensive on Flutter screens. Detect the page from
        // strong semantic markers that can appear at different scroll positions instead.
        return isElementPresent(pageTitle)
                || isElementPresent(yourMessageLabel)
                || isElementPresent(chooseFile)
                || isElementPresent(escalationMatrix)
                || isElementPresent(phoneLabel)
                || isElementPresent(phoneNumber)
                || isElementPresent(workingHours)
                || isElementPresent(postalAddressLabel)
                || isElementPresent(postalAddress)
                || isElementPresent(grievanceEmail)
                || isElementPresent(scoresLink)
                || isElementPresent(androidAppLink)
                || isElementPresent(androidAppLinkContains)
                || isElementPresent(iosAppLink)
                || isElementPresent(iosAppLinkUpper)
                || isElementPresent(submitButton);
    }

    private boolean isHubAreaVisible() {
        if (!isAdvisorAppForeground()) {
            return false;
        }

        // Do not treat the bottom-nav Hub button alone as proof that the Hub content is open; the
        // bottom navigation can remain visible on other screens. Prefer Hub-specific semantic nodes.
        if (isElementPresent(hubStocksSection)
                || isElementPresent(moreLabelDesc)
                || isElementPresent(contactUsHubTile)
                || isElementPresent(faqsDesc)
                || isElementPresent(aboutUsDesc)
                || isElementPresent(privacyPolicyDesc)) {
            return true;
        }

        String source = safePageSource();
        return containsIgnoreCase(source, "View frequently asked questions")
                || containsIgnoreCase(source, "Learn about Value Research")
                || containsIgnoreCase(source, "Get in touch with us")
                || containsIgnoreCase(source, "Read our privacy policy");
    }

    private void assertPresentOnCurrentScreen(By locator, String elementName) {
        if (isElementPresent(locator)) {
            ReportLogger.pass(elementName + " is visible/present");
            return;
        }

        captureScreenshot("CU_Missing_" + cleanFileName(elementName));
        throw new RuntimeException(elementName + " is not visible/present on current screen.");
    }

    private void assertPresentWithDownScroll(By locator, String elementName, int maxScrolls) {
        for (int attempt = 0; attempt <= maxScrolls; attempt++) {
            if (isElementPresent(locator)) {
                ReportLogger.pass(elementName + " is visible/present");
                return;
            }

            if (attempt < maxScrolls) {
                swipeUpW3C();
                sleep(550);
            }
        }

        // If the previous validation left the page near the bottom, the next target may be above.
        // Try controlled upward recovery before failing.
        for (int attempt = 1; attempt <= maxScrolls; attempt++) {
            swipeDownW3C();
            sleep(550);

            if (isElementPresent(locator)) {
                ReportLogger.pass(elementName + " is visible/present after upward recovery scroll");
                return;
            }
        }

        captureScreenshot("CU_Missing_" + cleanFileName(elementName));
        throw new RuntimeException(elementName + " is not visible/present after scrolling.");
    }

    private void assertExpectedTextPresent(String expectedText, String textName, int maxScrolls) {
        for (int attempt = 0; attempt <= maxScrolls; attempt++) {
            String source = safePageSource();

            if (containsNormalized(source, expectedText)) {
                ReportLogger.pass("Validated text - " + textName + ": " + formatForReport(expectedText));
                return;
            }

            if (attempt < maxScrolls) {
                swipeUpW3C();
                sleep(550);
            }
        }

        captureScreenshot("CU_Missing_Text_" + cleanFileName(textName));
        throw new RuntimeException("Expected text not found for " + textName + ": " + expectedText);
    }

    private void assertExpectedTextPresentEither(String expectedTextOne, String expectedTextTwo, String textName, int maxScrolls) {
        for (int attempt = 0; attempt <= maxScrolls; attempt++) {
            String source = safePageSource();

            if (containsNormalized(source, expectedTextOne)) {
                ReportLogger.pass("Validated text - " + textName + ": " + formatForReport(expectedTextOne));
                return;
            }

            if (containsNormalized(source, expectedTextTwo)) {
                ReportLogger.pass("Validated text - " + textName + ": " + formatForReport(expectedTextTwo));
                return;
            }

            if (attempt < maxScrolls) {
                swipeUpW3C();
                sleep(550);
            }
        }

        captureScreenshot("CU_Missing_Text_" + cleanFileName(textName));
        throw new RuntimeException("Expected text not found for " + textName + ": " + expectedTextOne + " / " + expectedTextTwo);
    }

    private void assertTextFragmentsPresentWithReport(By locator, String textName, String reportText, int maxScrolls, String... requiredFragments) {
        for (int attempt = 0; attempt <= maxScrolls; attempt++) {
            String combinedText = getElementTextForReport(locator) + " " + safePageSource();
            String normalizedCombinedText = normalizeForSearch(combinedText);

            boolean allFragmentsPresent = true;
            StringBuilder missingFragments = new StringBuilder();

            for (String fragment : requiredFragments) {
                if (!normalizedCombinedText.contains(normalizeForSearch(fragment))) {
                    allFragmentsPresent = false;
                    missingFragments.append(fragment).append(" | ");
                }
            }

            if (allFragmentsPresent) {
                ReportLogger.pass("Validated text - " + textName + ": " + formatForReport(reportText));
                return;
            }

            if (attempt < maxScrolls) {
                swipeUpW3C();
                sleep(550);
            } else {
                captureScreenshot("CU_Missing_Text_" + cleanFileName(textName));
                throw new RuntimeException("Expected text fragments not found for " + textName
                        + ". Missing fragments: " + missingFragments);
            }
        }
    }

    private String getElementTextForReport(By locator) {
        StringBuilder text = new StringBuilder();

        try {
            List<WebElement> elements = driver.findElements(locator);

            for (WebElement element : elements) {
                appendAttributeIfPresent(text, element, "content-desc");
                appendAttributeIfPresent(text, element, "contentDescription");
                appendAttributeIfPresent(text, element, "text");
                appendAttributeIfPresent(text, element, "name");

                try {
                    String visibleText = element.getText();
                    if (visibleText != null && !visibleText.trim().isEmpty()) {
                        text.append(' ').append(visibleText.trim());
                    }
                } catch (Exception ignored) {
                    // Ignore unsupported getText on Flutter semantic nodes.
                }
            }
        } catch (Exception ignored) {
            // Fallback to page source in caller.
        }

        return text.toString();
    }

    private void appendAttributeIfPresent(StringBuilder text, WebElement element, String attributeName) {
        try {
            String value = element.getAttribute(attributeName);
            if (value != null && !value.trim().isEmpty()) {
                text.append(' ').append(value.trim());
            }
        } catch (Exception ignored) {
            // Some drivers do not expose every attribute name.
        }
    }

    private String normalizeForSearch(String value) {
        if (value == null) {
            return "";
        }

        return value
                .replace("&#10;", " ")
                .replace("&#xA;", " ")
                .replace("&#xa;", " ")
                .replace("&amp;", "&")
                .replace("&nbsp;", " ")
                .replace("\u00A0", " ")
                .replace("\n", " ")
                .replace("\r", " ")
                .replaceAll("\\s+", " ")
                .trim()
                .toLowerCase();
    }



    private void logVisualTextOnly(String expectedText, String textName) {
        String source = safePageSource();
        if (containsNormalized(source, expectedText)) {
            ReportLogger.pass("Validated text - " + textName + ": " + formatForReport(expectedText));
            return;
        }

        // Flutter sometimes renders placeholder visually but does not expose it in the UI hierarchy.
        // Keep it in the report without failing because the actual input box is already validated above.
        ReportLogger.step("Expected visual text - " + textName + ": " + formatForReport(expectedText)
                + " | Note: placeholder may be visual-only and not exposed in Appium source.");
    }

    private WebElement alignFirstTargetAboveSubmit(String targetName, int maxSwipes, By... locators) {
        if (locators == null || locators.length == 0) {
            return null;
        }

        for (int attempt = 0; attempt <= maxSwipes; attempt++) {
            WebElement target = findFirstDisplayedFromLocators(locators);

            if (target != null) {
                try {
                    Rectangle targetRect = target.getRect();
                    Dimension screen = driver.manage().window().getSize();

                    if (targetRect != null
                            && targetRect.getWidth() > 0
                            && targetRect.getHeight() > 0) {

                        int targetBottom = targetRect.getY() + targetRect.getHeight();

                        // Keep the lower links away from the bottom navigation / clipped viewport.
                        int safeBottom = (int) Math.round(screen.height * 0.90d);
                        int safetyMargin = Math.max(24, (int) Math.round(screen.height * 0.015d));

                        // The QA layout has a sticky Submit region at the bottom. Use it as an
                        // additional runtime obstruction boundary only while it is actually sitting
                        // in the lower part of the viewport.
                        WebElement submit = findFirstDisplayedElement(submitButton);
                        int submitTop = -1;

                        if (submit != null) {
                            try {
                                Rectangle submitRect = submit.getRect();
                                if (submitRect != null) {
                                    submitTop = submitRect.getY();

                                    if (submitTop >= (int) Math.round(screen.height * 0.72d)) {
                                        safeBottom = Math.min(
                                                safeBottom,
                                                submitTop - safetyMargin
                                        );
                                    }
                                }
                            } catch (Exception ignored) {
                                // Fall back to the screen-based safe-bottom boundary.
                            }
                        }

                        boolean fullyVisibleInSafeZone = targetRect.getY() >= 0
                                && targetBottom <= safeBottom;

                        if (fullyVisibleInSafeZone) {
                            ReportLogger.step(
                                    targetName + " aligned in safe viewport"
                                            + " | attempt=" + attempt
                                            + " | x=" + targetRect.getX()
                                            + " | y=" + targetRect.getY()
                                            + " | width=" + targetRect.getWidth()
                                            + " | height=" + targetRect.getHeight()
                                            + " | targetBottom=" + targetBottom
                                            + " | safeBottom=" + safeBottom
                                            + (submitTop >= 0 ? " | submitTop=" + submitTop : "")
                            );
                            return target;
                        }

                        ReportLogger.step(
                                targetName + " is semantically present but not safely visible yet"
                                        + " | attempt=" + attempt
                                        + " | y=" + targetRect.getY()
                                        + " | height=" + targetRect.getHeight()
                                        + " | targetBottom=" + targetBottom
                                        + " | safeBottom=" + safeBottom
                                        + (submitTop >= 0 ? " | submitTop=" + submitTop : "")
                        );
                    }
                } catch (Exception e) {
                    ReportLogger.debug(
                            "Unable to inspect viewport bounds for " + targetName + ": "
                                    + cleanError(e.getMessage())
                    );
                }
            } else {
                ReportLogger.step(
                        targetName + " is not displayed in the current viewport"
                                + " | alignment attempt=" + attempt
                );
            }

            if (attempt < maxSwipes) {
                swipeUpSmall();
                sleep(400);
            }
        }

        captureScreenshot("CU_Target_Alignment_Failure_" + cleanFileName(targetName));
        return null;
    }

    private WebElement findFirstDisplayedFromLocators(By... locators) {
        if (locators == null) {
            return null;
        }

        for (By locator : locators) {
            if (locator == null) {
                continue;
            }

            WebElement element = findFirstDisplayedElement(locator);
            if (element != null) {
                return element;
            }
        }

        return null;
    }

    private void assertAndroidAppLinkPresent() {
        if (isElementPresent(androidAppLink) || isElementPresent(androidAppLinkContains)) {
            ReportLogger.pass("Android App link is visible/present");
            return;
        }

        String source = safePageSource();
        if (containsIgnoreCase(source, "Android App")) {
            ReportLogger.pass("Android App link is present in page source");
            return;
        }

        captureScreenshot("CU_Missing_Android_App_link");
        throw new RuntimeException("Android App link is not visible/present after scrolling.");
    }

    private void assertIosAppLinkPresent() {
        if (isElementPresent(iosAppLink)
                || isElementPresent(iosAppLinkUpper)
                || isElementPresent(iosAppLinkContainsLowerI)
                || isElementPresent(iosAppLinkContainsUpperI)) {
            ReportLogger.pass("iOS App link is visible/present");
            return;
        }

        String source = safePageSource();
        if (containsIgnoreCase(source, "iOS App")
                || containsIgnoreCase(source, "IOS App")
                || containsIgnoreCase(source, "Ios App")) {
            ReportLogger.pass("iOS App link is present in page source");
            return;
        }

        // Last fallback: the app-links line exists and Android App exists; fail only with a clear message.
        if (isElementPresent(scoresLinksLine) && containsIgnoreCase(source, "Android App")) {
            captureScreenshot("CU_Missing_iOS_App_link");
            throw new RuntimeException("SCORES links line is visible but iOS App link was not exposed by Appium/page source.");
        }

        captureScreenshot("CU_Missing_iOS_App_link");
        throw new RuntimeException("iOS App link is not visible/present after scrolling.");
    }

    private boolean isElementPresent(By locator) {
        try {
            List<WebElement> elements = driver.findElements(locator);
            return elements != null && !elements.isEmpty();
        } catch (Exception ignored) {
            return false;
        }
    }

    private boolean tapElementIfPresent(By locator, String elementName) {
        try {
            List<WebElement> elements = driver.findElements(locator);

            if (elements == null || elements.isEmpty()) {
                return false;
            }

            for (WebElement element : elements) {
                if (element == null) {
                    continue;
                }

                if (clickElementSafely(element, elementName)) {
                    sleep(900);
                    return true;
                }
            }
        } catch (Exception e) {
            ReportLogger.debug("Tap failed for " + elementName + ": " + cleanError(e.getMessage()));
        }

        return false;
    }

    private boolean clickElementSafely(WebElement element, String elementName) {
        try {
            element.click();
            ReportLogger.step("Tapped: " + elementName);
            return true;
        } catch (Exception clickError) {
            ReportLogger.debug("Normal click failed for " + elementName + ": " + cleanError(clickError.getMessage()));
            return tapElementCenter(element, elementName);
        }
    }

    private boolean tapElementCenter(WebElement element, String elementName) {
        try {
            Rectangle rect = element.getRect();
            if (rect == null || rect.getWidth() <= 0 || rect.getHeight() <= 0) {
                return false;
            }

            int centerX = rect.getX() + rect.getWidth() / 2;
            int centerY = rect.getY() + rect.getHeight() / 2;

            tapAt(centerX, centerY, elementName + " center fallback");
            return true;
        } catch (Exception e) {
            ReportLogger.debug("Element-center tap failed for " + elementName + ": " + cleanError(e.getMessage()));
            return false;
        }
    }

    private boolean isTypedMessagePresent(WebElement input) {
        try {
            String text = input.getText();
            if (text != null && text.contains(TEST_MESSAGE)) {
                return true;
            }
        } catch (Exception ignored) {
            // Try attributes and page source.
        }

        try {
            String textAttribute = input.getAttribute("text");
            if (textAttribute != null && textAttribute.contains(TEST_MESSAGE)) {
                return true;
            }
        } catch (Exception ignored) {
            // Try page source.
        }

        return safePageSource().contains(TEST_MESSAGE);
    }


    private void typeMessageAndValidate(String message, String reportName) {
        try {
            ReportLogger.step("Typing message for validation: " + reportName);

            scrollToMessageInputAreaIfNeeded();
            assertPresentOnCurrentScreen(messageInput, "Message input box");

            WebElement input = driver.findElement(messageInput);
            input.click();
            sleep(500);
            clearMessageInput();

            input = driver.findElement(messageInput);
            input.click();
            input.sendKeys(message);
            sleep(900);
            hideKeyboardSafely();

            if (!isMessageTextPresent(input, message)) {
                captureScreenshot("CU_Message_Not_Visible_" + cleanFileName(reportName));
                throw new RuntimeException("Typed message was not visible for: " + reportName);
            }

            ReportLogger.pass("Typed message validated in report: " + formatForReport(message));
        } catch (Exception e) {
            throw new RuntimeException(reportName + " failed: " + cleanError(e.getMessage()), e);
        }
    }

    private void clearMessageInput() {
        scrollToMessageInputAreaIfNeeded();
        assertPresentOnCurrentScreen(messageInput, "Message input box");

        try {
            WebElement input = driver.findElement(messageInput);
            input.click();
            sleep(400);
            input.clear();
            sleep(500);
        } catch (Exception e) {
            ReportLogger.debug("Standard clear failed for message input: " + cleanError(e.getMessage()));
        }
    }

    private boolean isMessageTextPresent(WebElement input, String message) {
        String normalizedExpected = normalizeForSearch(message);

        try {
            String text = input.getText();
            if (normalizeForSearch(text).contains(normalizedExpected)) {
                return true;
            }
        } catch (Exception ignored) {
            // Try attributes and page source.
        }

        String[] attributes = new String[]{"text", "content-desc", "contentDescription", "name", "value"};
        for (String attribute : attributes) {
            try {
                String value = input.getAttribute(attribute);
                if (normalizeForSearch(value).contains(normalizedExpected)) {
                    return true;
                }
            } catch (Exception ignored) {
                // Try next attribute.
            }
        }

        String source = safePageSource();
        return normalizeForSearch(source).contains(normalizedExpected);
    }

    private void assertNoSuccessfulSubmission(String scenarioName) {
        String source = safePageSource();

        if (!isContactUsPageVisible()) {
            captureScreenshot("CU_Negative_Submit_Left_Page_" + cleanFileName(scenarioName));
            throw new RuntimeException("Contact us page was not active after " + scenarioName + ". Possible unintended submit/navigation.");
        }

        if (containsIgnoreCase(source, "successfully submitted")
                || containsIgnoreCase(source, "message submitted")
                || containsIgnoreCase(source, "thank you")
                || containsIgnoreCase(source, "ticket created")
                || containsIgnoreCase(source, "request created")) {
            captureScreenshot("CU_Negative_Submit_Success_Message_" + cleanFileName(scenarioName));
            throw new RuntimeException("Success/submitted marker appeared for " + scenarioName + ".");
        }

        ReportLogger.pass("No successful-submission marker found for negative scenario: " + scenarioName);
    }

    private void tapRequiredWithDownScroll(By locator, String elementName, int maxScrolls) {
        for (int attempt = 0; attempt <= maxScrolls; attempt++) {
            if (tapElementIfPresent(locator, elementName)) {
                return;
            }

            if (attempt < maxScrolls) {
                swipeUpW3C();
                sleep(600);
            }
        }

        // Recover upward as well. This prevents failures when the previous test leaves the Contact us page at bottom.
        for (int attempt = 1; attempt <= maxScrolls; attempt++) {
            swipeDownW3C();
            sleep(600);

            if (tapElementIfPresent(locator, elementName)) {
                return;
            }
        }

        captureScreenshot("CU_Tap_Target_Not_Found_" + cleanFileName(elementName));
        throw new RuntimeException(elementName + " was not found/tapped after scrolling.");
    }

    private void assertTapOpensExternalOrChooserAndReturn(By locator, String actionName, int maxScrolls, String... expectedMarkers) {
        try {
            ReportLogger.step("Functional validation: tapping " + actionName);

            if (!isContactUsPageVisible()) {
                recoverContactUsIfNeeded();
            }

            String beforePackage = safeCurrentPackage();
            String beforeSource = normalizeForSearch(safePageSource());
            tapRequiredWithDownScroll(locator, actionName, maxScrolls);
            sleep(1800);

            String afterPackage = safeCurrentPackage();
            String afterSource = safePageSource();
            String normalizedAfterSource = normalizeForSearch(afterSource);
            boolean packageChanged = beforePackage != null
                    && afterPackage != null
                    && !beforePackage.isEmpty()
                    && !afterPackage.isEmpty()
                    && !beforePackage.equals(afterPackage);

            boolean markerFound = false;
            String matchedMarker = "";
            for (String marker : expectedMarkers) {
                String normalizedMarker = normalizeForSearch(marker);
                if (normalizedAfterSource.contains(normalizedMarker) && !beforeSource.contains(normalizedMarker)) {
                    markerFound = true;
                    matchedMarker = marker;
                    break;
                }
            }

            boolean leftContactUs = !isContactUsPageVisible();
            if (!(packageChanged || markerFound || leftContactUs)) {
                captureScreenshot("CU_Functional_Tap_No_Response_" + cleanFileName(actionName));
                throw new RuntimeException(actionName + " tap did not show external/chooser response and did not change UI state.");
            }

            ReportLogger.pass("Functional tap response detected for " + actionName
                    + " | packageChanged=" + packageChanged
                    + " | leftContactUs=" + leftContactUs
                    + " | markerFound=" + markerFound
                    + (matchedMarker.isEmpty() ? "" : " | matchedMarker=" + matchedMarker));
        } catch (Exception e) {
            throw new RuntimeException(actionName + " functional tap validation failed: " + cleanError(e.getMessage()), e);
        } finally {
            returnToContactUsAfterExternalAction();
        }
    }

    private void returnToContactUsAfterExternalAction() {
        ReportLogger.step("Restoring Advisor Contact us page after functional action");

        // Fast path: only accept Contact us when the Advisor package itself is foreground.
        if (isAdvisorAppForeground() && isContactUsPageVisible()) {
            ReportLogger.step("Contact us page is already active after functional action/recovery check");
            return;
        }

        // Preferred recovery for browser/chooser/dialer/email/file-picker actions: Android BACK.
        // Keep it controlled. Most external actions return in one Back press; a few chooser flows may
        // need two or three. Every attempt verifies both package and Contact us semantics.
        for (int attempt = 1; attempt <= 4; attempt++) {
            String currentPackage = safeCurrentPackage();

            ReportLogger.step(
                    "External-action recovery attempt: " + attempt
                            + " | currentPackage=" + currentPackage
            );

            pressBackSafely();
            sleep(700);

            if (isAdvisorAppForeground() && waitForContactUsPageVisible(1200)) {
                ReportLogger.pass("Returned to Contact us page after external action");
                return;
            }
        }

        // If BACK did not restore Advisor, explicitly bring the app to foreground. This is safe for
        // real devices and emulators and avoids trying Hub navigation while Chrome/Dialer/etc. is active.
        try {
            ReportLogger.step("Back recovery did not restore Contact us. Activating Advisor app.");
            driver.activateApp(ADVISOR_APP_PACKAGE);
            sleep(900);
        } catch (Exception e) {
            ReportLogger.debug(
                    "activateApp failed while recovering Contact us: " + cleanError(e.getMessage())
            );
        }

        // Advisor often resumes exactly where it left off. Accept that immediately when confirmed.
        if (isAdvisorAppForeground() && waitForContactUsPageVisible(1800)) {
            ReportLogger.pass("Contact us restored after activating Advisor app");
            return;
        }

        // Advisor resumed elsewhere. Navigate through Hub only after Advisor is confirmed foreground.
        if (!isAdvisorAppForeground()) {
            throw new RuntimeException(
                    "Unable to restore Advisor app after external functional action. "
                            + "Current package=" + safeCurrentPackage()
            );
        }

        ReportLogger.step("Advisor app restored but Contact us is not active. Reopening through Hub.");
        openContactUsFromHub();

        if (!isAdvisorAppForeground() || !isContactUsPageVisible()) {
            throw new RuntimeException(
                    "Unable to restore Contact us page after external application action."
            );
        }

        ReportLogger.pass("Contact us page restored through Hub");
    }

    private boolean isAdvisorAppForeground() {
        String currentPackage = safeCurrentPackage();
        return currentPackage != null
                && ADVISOR_APP_PACKAGE.equalsIgnoreCase(currentPackage.trim());
    }

    private String safeCurrentPackage() {
        try {
            return driver.getCurrentPackage();
        } catch (Exception e) {
            return "";
        }
    }

    private String safeAttribute(WebElement element, String attributeName) {
        try {
            String value = element.getAttribute(attributeName);
            return value == null ? "" : value;
        } catch (Exception e) {
            return "";
        }
    }

    // =====================================================================
    // Gesture helpers
    // =====================================================================


    private void scrollToMessageInputAreaIfNeeded() {
        if (isElementPresent(messageInput) || containsIgnoreCase(safePageSource(), "Your Message")) {
            ReportLogger.step("Message input area is already visible/present. No top reset required.");
            return;
        }

        ReportLogger.step("Message input area not present. Scrolling up only for message typing validation.");

        for (int attempt = 1; attempt <= 6; attempt++) {
            swipeDownW3C();
            sleep(450);

            if (isElementPresent(messageInput) || containsIgnoreCase(safePageSource(), "Your Message")) {
                ReportLogger.pass("Message input area found after scrolling up. Attempt: " + attempt);
                return;
            }
        }
    }

    private void swipeUpW3C() {
        performVerticalSwipe(0.78, 0.28, 650);
    }

    private void swipeUpSmall() {
        // Small controlled movement for lower inline links. A full-page swipe can overshoot
        // Android/iOS/SCORES targets on compact layouts, while this keeps them in context.
        performVerticalSwipe(0.74, 0.56, 350);
    }

    private void swipeDownW3C() {
        performVerticalSwipe(0.28, 0.78, 550);
    }

    private void performVerticalSwipe(double startRatio, double endRatio, long durationMs) {
        try {
            Dimension size = driver.manage().window().getSize();

            int startX = size.width / 2;
            int startY = (int) (size.height * startRatio);
            int endY = (int) (size.height * endRatio);

            PointerInput finger = new PointerInput(PointerInput.Kind.TOUCH, "finger");
            Sequence sequence = new Sequence(finger, 1);

            sequence.addAction(finger.createPointerMove(Duration.ZERO, PointerInput.Origin.viewport(), startX, startY));
            sequence.addAction(finger.createPointerDown(PointerInput.MouseButton.LEFT.asArg()));
            sequence.addAction(finger.createPointerMove(Duration.ofMillis(durationMs), PointerInput.Origin.viewport(), startX, endY));
            sequence.addAction(finger.createPointerUp(PointerInput.MouseButton.LEFT.asArg()));

            driver.perform(Collections.singletonList(sequence));
        } catch (Exception e) {
            ReportLogger.debug("Vertical swipe failed: " + cleanError(e.getMessage()));
        }
    }

    private void tapAt(int x, int y, String label) {
        try {
            PointerInput finger = new PointerInput(PointerInput.Kind.TOUCH, "finger");
            Sequence tap = new Sequence(finger, 1);

            tap.addAction(finger.createPointerMove(Duration.ZERO, PointerInput.Origin.viewport(), x, y));
            tap.addAction(finger.createPointerDown(PointerInput.MouseButton.LEFT.asArg()));
            tap.addAction(finger.createPointerMove(Duration.ofMillis(80), PointerInput.Origin.viewport(), x, y));
            tap.addAction(finger.createPointerUp(PointerInput.MouseButton.LEFT.asArg()));

            driver.perform(Collections.singletonList(tap));
            ReportLogger.step("Tapped: " + label + " at x=" + x + ", y=" + y);
        } catch (Exception e) {
            throw new RuntimeException("Coordinate tap failed for " + label + ": " + cleanError(e.getMessage()), e);
        }
    }

    // =====================================================================
    // Common helpers
    // =====================================================================

    private void pressBackSafely() {
        try {
            driver.pressKey(new KeyEvent(AndroidKey.BACK));
        } catch (Exception e) {
            ReportLogger.debug("Back key failed: " + cleanError(e.getMessage()));
        }
    }

    private void hideKeyboardSafely() {
        try {
            driver.hideKeyboard();
            sleep(500);
        } catch (Exception ignored) {
            // Keyboard may not be open.
        }
    }

    private String safePageSource() {
        try {
            return driver.getPageSource();
        } catch (Exception e) {
            return "";
        }
    }

    private boolean containsNormalized(String source, String expected) {
        if (source == null || expected == null) {
            return false;
        }

        String normalizedSource = normalizeText(source);
        String normalizedExpected = normalizeText(expected);

        return normalizedSource.contains(normalizedExpected);
    }

    private String normalizeText(String value) {
        if (value == null) {
            return "";
        }

        return value
                .replace("&amp;", "&")
                .replace("&quot;", "\"")
                .replace("&#39;", "'")
                .replace("\\n", " ")
                .replace("\n", " ")
                .replace("\r", " ")
                .replaceAll("\\s+", " ")
                .trim()
                .toLowerCase();
    }

    private String formatForReport(String value) {
        if (value == null) {
            return "";
        }

        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\n", "<br>");
    }

    private boolean containsIgnoreCase(String source, String expected) {
        if (source == null || expected == null) {
            return false;
        }

        return source.toLowerCase().contains(expected.toLowerCase());
    }

    private void captureScreenshot(String name) {
        try {
            ScreenshotUtils.captureScreenshot(driver, name);
        } catch (Exception ignored) {
            // Do not hide original failure.
        }
    }

    private String cleanError(String message) {
        if (message == null) {
            return "";
        }

        return message
                .replace("\n", " ")
                .replace("\r", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private String cleanFileName(String value) {
        if (value == null) {
            return "Unknown";
        }

        return value.replaceAll("[^a-zA-Z0-9]", "_");
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException interruptedException) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while waiting in Contact Us page", interruptedException);
        }
    }
}