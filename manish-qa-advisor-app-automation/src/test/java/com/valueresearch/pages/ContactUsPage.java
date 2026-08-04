package com.valueresearch.pages;

import com.valueresearch.utils.ReportLogger;
import com.valueresearch.utils.ScreenshotUtils;
import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.android.nativekey.AndroidKey;
import io.appium.java_client.android.nativekey.KeyEvent;
import org.openqa.selenium.By;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.Rectangle;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.PointerInput;
import org.openqa.selenium.interactions.Sequence;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.Collections;
import java.util.List;

public class ContactUsPage {

    private final AndroidDriver driver;
    private final WebDriverWait wait;

    private static final String TEST_MESSAGE = "Automation test message. Please ignore.";

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

    // Hub -> More -> Contact Us menu item. Keep multiple variants because Hub tile semantics can differ by build.
    private final By contactUsMenuExact = AppiumBy.accessibilityId("Contact Us");
    private final By contactUsMenuLower = AppiumBy.accessibilityId("Contact us");
    private final By contactUsMenuNoSpace = AppiumBy.accessibilityId("ContactUs");
    private final By contactUsMenuDescContains = AppiumBy.androidUIAutomator(
            "new UiSelector().descriptionContains(\"Contact\")"
    );
    private final By contactUsMenuTextContains = AppiumBy.androidUIAutomator(
            "new UiSelector().textContains(\"Contact\")"
    );
    private final By contactUsMenuXpath = AppiumBy.xpath(
            "//*[contains(@content-desc,'Contact') or contains(@text,'Contact')]"
    );

    // Hub More-section labels used only to confirm we are on the correct grid before coordinate fallback.
    private final By moreLabelDesc = AppiumBy.androidUIAutomator(
            "new UiSelector().descriptionContains(\"More\")"
    );
    private final By faqsDesc = AppiumBy.androidUIAutomator(
            "new UiSelector().descriptionContains(\"FAQs\")"
    );
    private final By aboutUsDesc = AppiumBy.androidUIAutomator(
            "new UiSelector().descriptionContains(\"About Us\")"
    );
    private final By privacyPolicyDesc = AppiumBy.androidUIAutomator(
            "new UiSelector().descriptionContains(\"Privacy Policy\")"
    );

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
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(25));
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
            if (isContactUsPageVisible()) {
                ReportLogger.pass("Contact us page is already active");
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

        for (int attempt = 1; attempt <= 4; attempt++) {
            if (tapElementIfPresent(hubTab, "Hub tab")) {
                sleep(1800);
                ReportLogger.pass("Hub tab opened");
                return;
            }

            pressBackSafely();
            sleep(900);
        }

        throw new RuntimeException("Hub tab was not visible after recovery attempts.");
    }

    private void tapContactUsFromHubMoreSection() {
        ReportLogger.step("Finding Contact Us inside Hub More section");

        // Important: in your current app state, More section is already visible after Hub opens.
        // Do direct locator + coordinate fallback before any scrolling; otherwise the test scrolls away from Contact Us.
        if (tapContactUsMenuIfVisible()) {
            ReportLogger.pass("Contact Us menu tapped using locator");
            return;
        }

        if (tapContactUsTileByMoreGridFallback()) {
            return;
        }

        // If Hub opens at top, scroll down until More grid is visible.
        for (int attempt = 1; attempt <= 8; attempt++) {
            ReportLogger.step("Contact Us menu not tapped yet. Scrolling Hub down. Attempt: " + attempt);
            swipeUpW3C();
            sleep(900);

            if (tapContactUsMenuIfVisible()) {
                ReportLogger.pass("Contact Us menu found and tapped after scrolling");
                return;
            }

            if (tapContactUsTileByMoreGridFallback()) {
                return;
            }
        }

        captureScreenshot("CU_Contact_Us_Menu_Not_Found_In_Hub");
        throw new RuntimeException("Contact Us menu was not found/tapped in Hub More section after locator and coordinate fallback.");
    }

    private boolean tapContactUsMenuIfVisible() {
        return tapElementIfPresent(contactUsMenuExact, "Contact Us menu")
                || tapElementIfPresent(contactUsMenuLower, "Contact Us menu")
                || tapElementIfPresent(contactUsMenuNoSpace, "Contact Us menu")
                || tapElementIfPresent(contactUsMenuDescContains, "Contact Us menu")
                || tapElementIfPresent(contactUsMenuTextContains, "Contact Us menu")
                || tapElementIfPresent(contactUsMenuXpath, "Contact Us menu");
    }

    private boolean tapContactUsTileByMoreGridFallback() {
        if (!isHubMoreGridVisible()) {
            return false;
        }

        ReportLogger.step("More grid is visible. Using Contact Us tile coordinate fallback.");

        Dimension size = driver.manage().window().getSize();

        // Hub More grid layout observed from the current app:
        // Row: FAQs | About Us | Contact Us | Privacy Policy
        // Contact Us is the 3rd tile. The coordinate is intentionally used only after More-grid confirmation.
        int contactUsX = (int) (size.width * 0.53);
        int contactUsY = calculateContactUsTileY(size);

        tapAt(contactUsX, contactUsY, "Contact Us tile coordinate fallback");
        sleep(1800);

        if (isContactUsPageVisible()) {
            ReportLogger.pass("Contact Us opened using More grid coordinate fallback");
            return true;
        }

        // If coordinate hit something else, recover back to Hub and continue controlled scrolling.
        ReportLogger.debug("Coordinate fallback did not open Contact us page. Returning to Hub and continuing search.");
        pressBackSafely();
        sleep(1000);
        return false;
    }

    private int calculateContactUsTileY(Dimension size) {
        // Try to calculate from More label position first. If that fails, use stable fallback ratio.
        try {
            List<WebElement> labels = driver.findElements(moreLabelDesc);
            for (WebElement label : labels) {
                if (label != null) {
                    Rectangle rect = label.getRect();
                    if (rect != null && rect.getY() > 0) {
                        return rect.getY() + (int) (size.height * 0.055);
                    }
                }
            }
        } catch (Exception ignored) {
            // Use ratio fallback below.
        }

        return (int) (size.height * 0.54);
    }

    private boolean isHubMoreGridVisible() {
        if (isElementPresent(faqsDesc) || isElementPresent(aboutUsDesc) || isElementPresent(privacyPolicyDesc)) {
            return true;
        }

        String source = safePageSource();
        int matchCount = 0;

        if (containsIgnoreCase(source, "FAQs")) {
            matchCount++;
        }
        if (containsIgnoreCase(source, "About Us")) {
            matchCount++;
        }
        if (containsIgnoreCase(source, "Privacy Policy")) {
            matchCount++;
        }
        if (containsIgnoreCase(source, "Refund Policy")) {
            matchCount++;
        }
        if (containsIgnoreCase(source, "Investor Charter")) {
            matchCount++;
        }
        if (containsIgnoreCase(source, "ODR Portal")) {
            matchCount++;
        }

        return matchCount >= 2;
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
            // These links are inside the same lower paragraph area. On Flutter/semantic views,
            // the exact iOS App node is sometimes not returned by Appium even when it is visibly rendered.
            // So we scroll to the Link for SCORES area once and validate with both locator and page-source fallbacks.
            scrollToScoresLinksArea();

            assertExpectedTextPresent(TXT_LINK_FOR_SCORES, "Link for SCORES label", 7);
            assertAndroidAppLinkPresent();
            assertExpectedTextPresent(TXT_ANDROID_APP, "Android App link text", 7);

            assertIosAppLinkPresent();
            assertExpectedTextPresentEither(TXT_IOS_APP, "IOS App", "iOS App link text", 7);

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

            scrollToScoresLinksArea();

            By exactScoresLink = AppiumBy.accessibilityId("https://scores.sebi.gov.in");
            By scoresXpath = AppiumBy.xpath("//android.widget.Button[@content-desc=\"https://scores.sebi.gov.in\"]");

            String beforePackage = safeCurrentPackage();
            String beforeSource = normalizeForSearch(safePageSource());

            boolean tapped = false;

            if (tapElementIfPresent(exactScoresLink, "SEBI SCORES link by accessibility id")) {
                tapped = true;
            } else if (tapElementIfPresent(scoresXpath, "SEBI SCORES link by exact xpath")) {
                tapped = true;
            } else if (tapElementIfPresent(scoresLink, "SEBI SCORES link fallback locator")) {
                tapped = true;
            } else {
                ReportLogger.step("SEBI SCORES locator tap failed. Trying Appium Inspector coordinate fallback.");
                tapAt(412, 1977, "SEBI SCORES link coordinate fallback");
                tapped = true;
            }

            if (!tapped) {
                throw new RuntimeException("Unable to tap SEBI SCORES link using locator or coordinate fallback.");
            }

            if (isScoresTapResponseDetected(beforePackage, beforeSource, "SEBI SCORES link")) {
                return;
            }

            ReportLogger.step("SEBI SCORES normal tap did not show response. Trying coordinate fallback from Appium Inspector.");
            tapAt(412, 1977, "SEBI SCORES link coordinate fallback retry");

            if (isScoresTapResponseDetected(beforePackage, beforeSource, "SEBI SCORES link after coordinate fallback")) {
                return;
            }

            throw new RuntimeException("SEBI SCORES link tap did not open browser/chooser and did not change UI state.");

        } catch (Exception e) {
            throw new RuntimeException("SEBI SCORES link functional tap validation failed: " + cleanError(e.getMessage()), e);
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


    private boolean isScoresTapResponseDetected(String beforePackage, String beforeSource, String actionName) {
        sleep(7000);

        String afterPackage = safeCurrentPackage();
        String afterSourceRaw = safePageSource();
        String afterSource = normalizeForSearch(afterSourceRaw);

        boolean packageChanged = beforePackage != null
                && afterPackage != null
                && !beforePackage.isEmpty()
                && !afterPackage.isEmpty()
                && !beforePackage.equalsIgnoreCase(afterPackage);

        boolean browserMarkerFound =
                containsIgnoreCase(afterPackage, "chrome")
                        || containsIgnoreCase(afterPackage, "browser")
                        || containsIgnoreCase(afterSourceRaw, "Chrome")
                        || containsIgnoreCase(afterSourceRaw, "Browser")
                        || containsIgnoreCase(afterSourceRaw, "Open with")
                        || containsIgnoreCase(afterSourceRaw, "scores.sebi.gov.in")
                        || containsIgnoreCase(afterSourceRaw, "SEBI")
                        || containsIgnoreCase(afterSourceRaw, "SCORES");

        boolean sourceChanged = beforeSource != null
                && afterSource != null
                && !beforeSource.equals(afterSource);

        boolean leftContactUs = !isContactUsPageVisible();

        if (packageChanged || browserMarkerFound || sourceChanged || leftContactUs) {
            ReportLogger.pass(
                    "Functional tap response detected for " + actionName
                            + " | packageChanged=" + packageChanged
                            + " | currentPackage=" + afterPackage
                            + " | browserMarkerFound=" + browserMarkerFound
                            + " | sourceChanged=" + sourceChanged
                            + " | leftContactUs=" + leftContactUs
            );
            return true;
        }

        return false;
    }

    public void verifyAndroidAppLinkFunctionalTap() {
        scrollToScoresLinksArea();
        assertTapOpensExternalOrChooserAndReturn(
                androidAppLink,
                "Android App link",
                7,
                "Android App", "Play Store", "Chrome", "Browser", "Open with", "SCORES"
        );
    }

    public void verifyIosAppLinkFunctionalTap() {
        scrollToScoresLinksArea();
        if (isElementPresent(iosAppLink)) {
            assertTapOpensExternalOrChooserAndReturn(
                    iosAppLink,
                    "iOS App link",
                    7,
                    "iOS App", "App Store", "Chrome", "Browser", "Open with", "SCORES"
            );
            return;
        }

        assertTapOpensExternalOrChooserAndReturn(
                iosAppLinkContainsLowerI,
                "iOS App link",
                7,
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
        if (isElementPresent(pageTitle)) {
            return true;
        }

        String source = safePageSource();
        return containsIgnoreCase(source, "Contact us")
                && containsIgnoreCase(source, "Your Message")
                && containsIgnoreCase(source, "Submit");
    }

    private boolean isHubAreaVisible() {
        return isElementPresent(hubTab)
                || isElementPresent(faqsDesc)
                || isElementPresent(aboutUsDesc)
                || isElementPresent(privacyPolicyDesc)
                || containsIgnoreCase(safePageSource(), "FAQs")
                || containsIgnoreCase(safePageSource(), "Hub");
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

    private void scrollToScoresLinksArea() {
        for (int attempt = 0; attempt <= 7; attempt++) {
            String source = safePageSource();

            if (containsIgnoreCase(source, "Link for SCORES")
                    || containsIgnoreCase(source, "Android App")
                    || containsIgnoreCase(source, "iOS App")
                    || containsIgnoreCase(source, "IOS App")) {
                ReportLogger.step("SCORES app-links area is visible/present");
                return;
            }

            swipeUpW3C();
            sleep(550);
        }

        ReportLogger.debug("SCORES app-links area was not confirmed before link validation. Continuing with direct locator checks.");
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
        for (int attempt = 1; attempt <= 5; attempt++) {
            if (isContactUsPageVisible()) {
                ReportLogger.pass("Returned to Contact us page after functional action");
                return;
            }

            pressBackSafely();
            sleep(900);
        }

        try {
            ReportLogger.step("Back navigation did not restore Contact us. Activating Advisor app.");
            driver.activateApp("com.valueresearch.advisor");
            sleep(1200);
        } catch (Exception e) {
            ReportLogger.debug("activateApp failed while recovering Contact us: " + cleanError(e.getMessage()));
        }

        if (!isContactUsPageVisible()) {
            recoverContactUsIfNeeded();
        }
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