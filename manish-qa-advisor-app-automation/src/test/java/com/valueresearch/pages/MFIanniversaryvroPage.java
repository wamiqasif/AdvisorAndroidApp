// Automation active for 20-Oct-2026 to 19-Nov-2026 only.
package com.valueresearch.pages;

import com.valueresearch.utils.ActionUtils;
import com.valueresearch.utils.ConfigReader;
import com.valueresearch.utils.ExecutionProfile;
import com.valueresearch.utils.ReportLogger;
import com.valueresearch.utils.WaitUtils;

import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;

import org.openqa.selenium.By;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.Rectangle;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.remote.RemoteWebElement;
import org.openqa.selenium.interactions.PointerInput;
import org.openqa.selenium.interactions.Sequence;
import org.openqa.selenium.interactions.Pause;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.Assert;

import java.time.Duration;
import java.time.YearMonth;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.Locale;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MFIanniversaryvroPage {

    private final AndroidDriver driver;
    private static final Pattern STORY_METADATA =
            Pattern.compile("\\b\\d+\\s+MIN\\s+(?:READ|WATCH)\\b");
    private final By homeTab = AppiumBy.accessibilityId("Home");
    // Accept both straight and typographic apostrophes.
    private final By todaysStories = AppiumBy.androidUIAutomator(
            "new UiSelector().descriptionMatches(\"Today['’]s Stories\")"
    );
    private final By allStories = AppiumBy.accessibilityId("All Stories");
    private final By scrollView =
            By.className("android.widget.ScrollView");
    private final By subscribe =
            AppiumBy.accessibilityId("Subscribe");
    private final By storeHeading =
            AppiumBy.accessibilityId("Value Research Store");
    private final By magazinesHeading =
            AppiumBy.accessibilityId("Magazines");
    // Avoid matching the entire composite label or its changing price.
    private final By mfiCard = AppiumBy.androidUIAutomator(
            "new UiSelector().className(\"android.view.View\")"
                    + ".descriptionStartsWith(\"Mutual Fund Insight\")"
    );
    public MFIanniversaryvroPage(AndroidDriver driver) {
        this.driver = driver;
    }

    public void ensureHomePage() {
        resetToVroHome();
        ReportLogger.step("VRO Home page is ready");
    }

    public void openStoreAndVerifyMagazineCard() {
        WaitUtils.waitForElement(driver, subscribe);
        ReportLogger.step("Tapping Subscribe from VRO Home");
        click(subscribe);
        WebElement store =
                WaitUtils.waitForElement(driver, storeHeading);
        WebElement magazines =
                WaitUtils.waitForElement(driver, magazinesHeading);
        WebElement card =
                WaitUtils.waitForElement(driver, mfiCard);
        Rectangle storeBounds = store.getRect();
        Rectangle magazinesBounds = magazines.getRect();
        Rectangle cardBounds = card.getRect();
        Assert.assertTrue(
                magazinesBounds.getY()
                        >= storeBounds.getY() + storeBounds.getHeight(),
                "Magazines heading must appear beneath Value Research Store"
        );
        Assert.assertTrue(
                cardBounds.getY()
                        >= magazinesBounds.getY() + magazinesBounds.getHeight(),
                "MFI magazine card must appear beneath Magazines"
        );
        Assert.assertTrue(
                cardBounds.getWidth() > 0 && cardBounds.getHeight() > 0,
                "MFI magazine card has invalid bounds"
        );
        ReportLogger.pass(
                "Value Research Store, Magazines heading, "
                        + "and the MFI card beneath it are visible"
        );
    }

    private void click(By locator) {
        try {
            ActionUtils.click(driver, locator);
        } catch (StaleElementReferenceException ignored) {
            // Retry once using a fresh explicit-wait lookup.
            ActionUtils.click(driver, locator);
        }
    }

    private final By anniversaryInvestSubscribe = By.xpath(
            "//android.view.View[@content-desc='Invest in Clarity']"
                    + "/following-sibling::android.view.View[@content-desc='Subscribe Now']");


    public void verifyMfiAnniversaryMagazineCard() {
        openStoreAndVerifyMagazineCard();
        Assert.assertTrue(anniversaryDescription(driver.findElement(mfiCard))
                        .contains("Anniversary Special"),
                "Mutual Fund Insight card is missing Anniversary Special");
        Assert.assertTrue(anniversaryMagazineButton().isEnabled(),
                "Mutual Fund Insight Subscribe Now button is disabled");
        ReportLogger.pass("Mutual Fund Insight: Anniversary Special and Subscribe Now verified");
    }

    private AnniversaryViewport openAnniversaryLandingPage() {
        openStoreAndVerifyMagazineCard();
        ReportLogger.step("Opening Mutual Fund Insight subscription landing page");
        anniversaryMagazineButton().click();
        AnniversaryViewport viewport = anniversaryViewportWait(ExecutionProfile.longWait()).until(d -> {
            AnniversaryViewport current = anniversaryReadViewport();
            AnniversaryNode heading = current.find(AnniversaryCheck.exact(
                    "Celebrating 24 years of Mutual Fund Insight"));
            return heading != null && heading.visibleWithin(current.screen)
                    && current.scrollElement != null ? current : null;
        });
        ReportLogger.step("Subscription landing page loaded; scrolling the plans into view");
        return viewport;
    }

    public void verifySubscriptionPlansAndContent() {
        anniversaryWithFastPolling(() -> {
            AnniversaryViewport viewport = openAnniversaryLandingPage();
            viewport = anniversaryVerifyPlanMode(viewport, "Digital", "down", "₹80", "Save 60%",
                    "Billed at ₹960 for 1 year", "₹60", "Save 70%", "Billed at ₹2,160 for 3 years");
            viewport = anniversaryVerifyPlanMode(viewport, "Print + Digital", "up", "₹135", "Save 33%",
                    "Billed at ₹1,620 for 1 year", "₹115", "Save 43%", "Billed at ₹4,140 for 3 years");
            ReportLogger.pass("Four annual plans verified: Digital and Print + Digital");

            List<AnniversaryCheck> content = new ArrayList<>();
            String[] labels = {
                    "Become a Confident Investor",
                    "Our analysis and coverage have helped millions make better investing decisions",
                    "Wealth Building Ideas", "Investment Ideas", "Timely Guidance", "Why Subscribe?",
                    "Trusted by Leaders", "What our users are saying", "Frequently Asked Questions",
                    "Can I extend my membership?",
                    "What is automatic renewal? Is it mandatory?",
                    "Can I cancel my subscription before the year ends?",
                    "What is your Refund Policy?",
                    "Will I get access to previous issues of Mutual Fund Insight as well?",
                    "When is the new issue released?", "When is the new issue shipped?",
                    "How will I receive my digital copy?",
                    "Does Value Research distribute the magazine/book?",
                    "Got questions about the subscriptions?", "Privacy Policy"
            };
            for (String label : labels) content.add(AnniversaryCheck.exact(label));
            content.add(AnniversaryCheck.prefix("Fund Manager's Insight"));
            content.add(AnniversaryCheck.prefix("Industry Leader's Opinions"));
            content.add(AnniversaryCheck.prefix("Call"));
            content.add(new AnniversaryCheck("Email", true, "subscription@valueresearch.in"));
            content.add(AnniversaryCheck.prefix("WhatsApp"));
            ReportLogger.step("Scanning subscription content in one pass; all checks require visible bounds");
            anniversaryScanVisible(viewport, content, "Remaining subscription content");
            ReportLogger.pass("Benefits, testimonials, FAQs, contact cards and Privacy Policy verified");
        });
    }

    private AnniversaryViewport anniversaryVerifyPlanMode(AnniversaryViewport viewport, String tab,
            String direction, String annualRate, String annualSavings, String annualBilling,
            String threeYearRate, String threeYearSavings, String threeYearBilling) {
        ReportLogger.step("Scrolling " + tab + " header into view");
        viewport = anniversaryBringIntoView(viewport, AnniversaryCheck.exact(tab), direction, true);
        AnniversaryNode header = viewport.find(AnniversaryCheck.exact(tab));
        Assert.assertTrue(header != null && header.visibleWithin(viewport.content), tab + " header is not visible");
        anniversaryClickVisible(AppiumBy.accessibilityId(tab), header);
        // Never reuse a viewport captured before changing the tab.
        viewport = anniversaryReadViewport();
        if ("Digital".equals(tab)) {
            viewport = anniversaryBringIntoView(viewport,
                    AnniversaryCheck.exact("*Digital copies available instantly"), "down", false);
        }
        AnniversaryCheck firstPlan = AnniversaryCheck.plan("1 Year", annualRate, annualSavings, annualBilling);
        viewport = anniversaryBringIntoView(viewport, firstPlan, "down", false);
        viewport = anniversaryWaitForSelectedPlan(viewport, firstPlan);
        ReportLogger.step(tab + " plans loaded and visible; checking annual plans");
        return anniversaryScanVisible(viewport, List.of(firstPlan,
                AnniversaryCheck.plan("3 Years", threeYearRate, threeYearSavings, threeYearBilling)), tab);
    }

    public void verifyInvestInClarityCheckoutNavigation() {
        anniversaryWithFastPolling(() -> {
            AnniversaryViewport viewport = openAnniversaryLandingPage();
            ReportLogger.step("Scrolling until Invest in Clarity and its Subscribe Now button are visible");
            long deadline = System.nanoTime() + ExecutionProfile.longWait().toNanos();
            AnniversaryNode button = null;
            for (int swipe = 0; swipe <= 24 && System.nanoTime() <= deadline; swipe++) {
                button = viewport.stickySubscribe();
                if (button != null) break;
                if (swipe == 24) break;
                viewport = anniversaryAdvance(viewport, null, "down", false,
                        "Invest in Clarity with its Subscribe Now button");
            }
            Assert.assertNotNull(button, "Invest in Clarity and its scoped Subscribe Now button were not visible");
            anniversaryClickVisible(anniversaryInvestSubscribe, button);
            // Requirement-based heading; supply a Checkout locator if the app uses another label.
            anniversaryViewportWait(ExecutionProfile.longWait()).until(d -> {
                AnniversaryViewport current = anniversaryReadViewport();
                AnniversaryNode checkout = current.find(AnniversaryCheck.exact("Checkout"));
                return checkout != null && checkout.visibleWithin(current.screen);
            });
            ReportLogger.pass("Invest in Clarity Subscribe Now opened Checkout");
        });
    }

    /** TC_006: purchase the Digital three-year plan in the configured PayU sandbox. */
    public void purchaseThreeYearDigitalPlan(Runnable beforePaymentSubmission) {
        final List<String> payment = mfiSandboxPaymentData();

        ensureHomePage();
        verifyMfiAnniversaryMagazineCard();
        anniversaryWithFastPolling(() -> {
            ReportLogger.step("Opening the verified MFI Anniversary subscription card");
            anniversaryMagazineButton().click();
            AnniversaryViewport viewport = anniversaryViewportWait(ExecutionProfile.longWait()).until(d -> {
                AnniversaryViewport current = anniversaryReadViewport();
                return mfiVisibleText(current, "Celebrating 24 years of Mutual Fund Insight")
                        && current.scrollElement != null ? current : null;
            });
            viewport = anniversaryBringIntoView(viewport, AnniversaryCheck.exact("Digital"), "down", true);
            anniversaryTap(AppiumBy.accessibilityId("Digital"));
            viewport = anniversaryReadViewport();
            viewport = anniversaryBringIntoView(viewport,
                    AnniversaryCheck.exact("*Digital copies available instantly"), "down", false);
            Assert.assertTrue(mfiVisibleText(viewport, "*Digital copies available instantly"),
                    "Digital copies information must be visible before selecting a plan");
            AnniversaryCheck plan = AnniversaryCheck.plan("3 Years", "₹60", "Save 70%",
                    "Billed at ₹2,160 for 3 years");
            viewport = anniversaryBringIntoView(viewport, plan, "down", false);
            viewport = anniversaryWaitForSelectedPlan(viewport, plan);
            AnniversaryNode annual = viewport.find(plan);
            plan.verify(annual);
            Assert.assertTrue(annual.label.contains("BEST VALUE") && annual.label.contains("₹200"),
                    "Digital 3 Years must show BEST VALUE and the original monthly price");
            Assert.assertTrue(annual.enabled, "Digital 3 Years is disabled");
            anniversaryTap(AppiumBy.androidUIAutomator(
                    "new UiSelector().descriptionStartsWith(\"3 Years\")"));
            ReportLogger.pass("Selected Digital 3 Years: ₹60/month, Save 70%, billed ₹2,160");

            viewport = anniversaryReadViewport();
            viewport = anniversaryBringIntoView(viewport, AnniversaryCheck.exact("Subscribe Now"), "down", false);
            AnniversaryNode button = viewport.find(AnniversaryCheck.exact("Subscribe Now"));
            Assert.assertTrue(button.inScroll && button.enabled, "Plans-panel Subscribe Now is unavailable");
            // Scope to the plans panel; the sticky Invest in Clarity CTA can share this label.
            anniversaryTap(By.xpath("//android.widget.ScrollView"
                    + "//android.view.View[@content-desc='Subscribe Now']"));
            viewport = mfiAwaitCheckout();
            // Continue if the existing address screen appears; its duplicate validation is excluded.
            if (mfiVisibleText(viewport, "Billing Address") && mfiVisibleText(viewport, "Continue")) {
                anniversaryTap(AppiumBy.accessibilityId("Continue"));
                viewport = mfiAwaitCheckout();
            }
            Assert.assertTrue(mfiVisibleText(viewport, "Digital Only-3 Years"),
                    "Checkout did not retain the selected Digital three-year plan");
            viewport = mfiVerifyThreeYearCheckout(viewport);
            if (!mfiVisibleText(viewport, "Pay Securely Now")) {
                viewport = anniversaryBringIntoView(viewport,
                        AnniversaryCheck.exact("Pay Securely Now"), "down", false);
            }
            Assert.assertTrue(mfiVisibleText(viewport, "Pay Securely Now"),
                    "Checkout payment button must be visible after verification");
            Assert.assertTrue(viewport.find(AnniversaryCheck.exact("Pay Securely Now")).enabled,
                    "Checkout payment button is disabled");
            ReportLogger.step("Checkout details verified; tapping Pay Securely Now");
            anniversaryTap(AppiumBy.accessibilityId("Pay Securely Now"));

            mfiCompleteSandboxPurchase(beforePaymentSubmission, "2160", payment);
        });
    }

    /** TC_007: Print + Digital three-year purchase with the supplied billing-address variant. */
    public void purchaseThreeYearPrintDigitalPlan(Runnable beforePaymentSubmission) {
        final List<String> payment = mfiSandboxPaymentData();
        final String delivery = anniversaryClean(ConfigReader.get("MFIprintDeliveryAddress"));
        final String billing = anniversaryClean(ConfigReader.get("MFIprintBillingAddress"));
        Assert.assertTrue(!delivery.isEmpty() && !billing.isEmpty(), "Print-plan address expectations are required");
        ensureHomePage();
        verifyMfiAnniversaryMagazineCard();
        anniversaryWithFastPolling(() -> {
            ReportLogger.step("Opening the verified MFI Anniversary subscription card for Print + Digital");
            anniversaryMagazineButton().click();
            AnniversaryViewport viewport = anniversaryViewportWait(ExecutionProfile.longWait()).until(d -> {
                AnniversaryViewport current = anniversaryReadViewport();
                return mfiVisibleText(current, "Celebrating 24 years of Mutual Fund Insight")
                        && current.scrollElement != null ? current : null;
            });
            viewport = anniversaryBringIntoView(viewport, AnniversaryCheck.exact("Print + Digital"), "down", true);
            AnniversaryNode tab = viewport.find(AnniversaryCheck.exact("Print + Digital"));
            anniversaryClickVisible(AppiumBy.accessibilityId("Print + Digital"), tab);
            viewport = anniversaryReadViewport();
            viewport = anniversaryBringIntoView(viewport,
                    AnniversaryCheck.exact("*Digital copies available instantly"), "down", false);
            Assert.assertTrue(mfiVisibleText(viewport, "*Digital copies available instantly"),
                    "Digital copies information must be visible before selecting a Print + Digital plan");
            AnniversaryCheck plan = AnniversaryCheck.plan("3 Years", "₹115", "Save 43%",
                    "Billed at ₹4,140 for 3 years");
            viewport = anniversaryBringIntoView(viewport, plan, "down", false);
            viewport = anniversaryWaitForSelectedPlan(viewport, plan);
            AnniversaryNode annual = viewport.find(plan);
            plan.verify(annual);
            Assert.assertTrue(annual.label.contains("₹200"), "Print + Digital original monthly price is missing");
            Assert.assertTrue(annual.enabled, "Print + Digital 3 Years is disabled");
            anniversaryTap(AppiumBy.androidUIAutomator("new UiSelector().descriptionStartsWith(\"3 Years\")"));
            ReportLogger.pass("Selected Print + Digital 3 Years: ₹115/month, Save 43%, billed ₹4,140");
            viewport = anniversaryReadViewport();
            viewport = anniversaryBringIntoView(viewport, AnniversaryCheck.exact("Subscribe Now"), "down", false);
            AnniversaryNode button = viewport.find(AnniversaryCheck.exact("Subscribe Now"));
            Assert.assertTrue(button.inScroll && button.enabled, "Plans-panel Subscribe Now is unavailable");
            anniversaryTap(By.xpath("//android.widget.ScrollView//android.view.View[@content-desc='Subscribe Now']"));
            viewport = anniversaryViewportWait(ExecutionProfile.longWait()).until(d -> {
                AnniversaryViewport current = anniversaryReadViewport();
                return mfiVisibleText(current, "Checkout")
                        && current.find(AnniversaryCheck.exact("Delivery Address")) != null
                        && current.find(AnniversaryCheck.exact("Billing Address")) != null ? current : null;
            });
            viewport = mfiVerifyPrintAddresses(viewport, delivery, billing);
            anniversaryTap(AppiumBy.accessibilityId("Continue"));
            viewport = mfiAwaitPrintOrderSummary();
            // The supplied Print summary has Order Summary as its heading, without Checkout.
            viewport = mfiVerifyCheckout(viewport, "Order Summary", "Digital + Print-3 Years", "-3060", "4140", true);
            if (!mfiVisibleText(viewport, "Pay Securely Now")) {
                viewport = anniversaryBringIntoView(viewport,
                        AnniversaryCheck.exact("Pay Securely Now"), "down", false);
            }
            AnniversaryNode pay = viewport.find(AnniversaryCheck.exact("Pay Securely Now"));
            Assert.assertTrue(pay != null && pay.enabled
                            && pay.visibleWithin(pay.inScroll ? viewport.content : viewport.screen),
                    "Print checkout payment button must be visible and enabled after verification");
            ReportLogger.step("Print checkout verified; tapping Pay Securely Now");
            anniversaryTap(AppiumBy.accessibilityId("Pay Securely Now"));
            mfiCompleteSandboxPurchase(beforePaymentSubmission, "4140", payment);
        });
    }

    private List<String> mfiSandboxPaymentData() {
        Assert.assertEquals(ConfigReader.get("MFIpayuEnvironment"), "sandbox",
                "Purchase tests require the PayU sandbox configuration");
        final String card = ConfigReader.get("MFIpayuCardNumber");
        final String expiry = ConfigReader.get("MFIpayuExpiry");
        final String cvv = ConfigReader.get("MFIpayuCvv");
        final String otp = ConfigReader.get("MFIpayuOtp");
        Assert.assertTrue(card.matches("\\d{16}"), "Invalid sandbox card-number configuration");
        Assert.assertTrue(expiry.matches("(?:0[1-9]|1[0-2])/\\d{2}"), "Expiry must use MM/YY");
        Assert.assertTrue(cvv.matches("\\d{3}"), "Invalid sandbox CVV configuration");
        Assert.assertTrue(otp.matches("\\d{6}"), "Invalid sandbox OTP configuration");

        return List.of(card, expiry, cvv, otp);
    }

    private void mfiCompleteSandboxPurchase(Runnable beforePaymentSubmission, String total, List<String> payment) {
        final String card = payment.get(0), expiry = payment.get(1), cvv = payment.get(2), otp = payment.get(3);
        final String paidLabel = String.format(java.util.Locale.ROOT, "₹%,.2f Total Paid", new BigDecimal(total));
        AnniversaryViewport viewport;
        viewport = mfiPaymentWait().until(d -> {
            AnniversaryViewport current = anniversaryReadViewport();
            return mfiResourceVisible(current, "tv_pay_text")
                    && mfiResourceVisible(current, "tv_amount")
                    && mfiVisibleText(current, "Cards") ? current : null;
        });
        mfiAssertResourceText(viewport, "tv_pay_text", "Pay PayU");
        mfiAssertMoney(mfiResource(viewport, "tv_amount").label, total, "PayU amount");
        anniversaryTap(AppiumBy.androidUIAutomator("new UiSelector().text(\"Cards\")"));
        viewport = mfiPaymentWait().until(d -> {
            AnniversaryViewport current = anniversaryReadViewport();
            return mfiResourceVisible(current, "tvAddNewCard")
                    && mfiResourceVisible(current, "et_add_card")
                    && mfiResourceVisible(current, "etExpiry")
                    && mfiResourceVisible(current, "etCvv") ? current : null;
        });
        mfiAssertResourceText(viewport, "tvAddNewCard", "ENTER CARD DETAILS");
        mfiAssertResourceText(viewport, "tvCardNumberLabel", "Card Number");
        mfiAssertResourceText(viewport, "tvExpiry", "Expiry");
        mfiAssertResourceText(viewport, "tvCvv", "CVV");
        mfiType("et_add_card", card, false);
        mfiType("etExpiry", expiry, false);
        mfiType("etCvv", cvv, true);
        mfiHideKeyboard();
        viewport = anniversaryReadViewport();
        mfiAssertResourceText(viewport, "btnPay", "PROCEED & PAY");
        ReportLogger.step("Submitting the configured PayU sandbox card; sensitive values are omitted");
        // Set the test's guard BEFORE dispatch: an interrupted response does not prove rejection.
        Assert.assertNotNull(beforePaymentSubmission, "Payment submission guard is required");
        beforePaymentSubmission.run();
        mfiClickOnce(mfiPaymentId("btnPay"));
        mfiOpenManualOtp();
        mfiType("etEnterOtpEditTextSubmitOtp", otp, false);
        mfiHideKeyboard();
        viewport = anniversaryReadViewport();
        mfiAssertResourceText(viewport, "tvTitleText", "SUBMIT OTP");
        mfiAssertResourceText(viewport, "btnSubmitOtpManual", "SUBMIT");
        mfiClickOnce(mfiPaymentId("btnSubmitOtpManual"));
        viewport = mfiPaymentWait().until(d -> {
            AnniversaryViewport current = anniversaryReadViewport();
            return mfiVisibleText(current, "Order successfully!")
                    && current.find(AnniversaryCheck.prefix("Order ID:")) != null
                    && current.find(AnniversaryCheck.exact("Download your digital copy")) != null
                    && current.find(AnniversaryCheck.exact(paidLabel)) != null
                    ? current : null;
        });
        mfiVerifySuccessAndReturnHome(viewport, paidLabel);
    }

    private AnniversaryViewport mfiVerifyPrintAddresses(AnniversaryViewport viewport, String delivery, String billing) {
        viewport = anniversaryBringIntoView(viewport, AnniversaryCheck.exact("Checkout"), "up", false);
        viewport = anniversaryScanVisible(viewport, List.of(AnniversaryCheck.exact("Checkout"),
                AnniversaryCheck.exact("Logged in email:"), AnniversaryCheck.exact(ConfigReader.get("VRO_TEST_USER")),
                AnniversaryCheck.exact("Delivery Address")), "Print address checkout");
        viewport = mfiBringAddressIntoView(viewport, "Delivery Address", delivery);
        AnniversaryNode deliveryCard = mfiAddressInSection(viewport, "Delivery Address", delivery);
        Assert.assertEquals(deliveryCard.label, delivery, "Unexpected delivery address content");
        ReportLogger.pass("Delivery address content verified without changing the delivery selection");
        viewport = anniversaryBringIntoView(viewport, AnniversaryCheck.exact("Billing Address"), "down", false);
        viewport = mfiBringAddressIntoView(viewport, "Billing Address", billing);
        AnniversaryNode billingCard = mfiAddressInSection(viewport, "Billing Address", billing);
        Assert.assertTrue(billingCard.enabled, "Second billing address is disabled");
        int available = 0, matchedPosition = 0;
        for (AnniversaryNode option : mfiSectionNodes(viewport, "Billing Address")) {
            if (!option.displayed || !option.label.contains("Phone:")) continue;
            available++;
            if (option.element == billingCard.element) matchedPosition = available;
        }
        Assert.assertTrue(available >= 2 && matchedPosition == 2,
                "Configured billing address must be the second available address in the Billing Address section");
        // Delivery and billing can have identical descriptions; scope by the Billing Address heading.
        By secondBilling = By.xpath("//android.widget.ScrollView/android.view.View[@content-desc='Billing Address']"
                + "/following-sibling::android.view.View/descendant-or-self::android.view.View[@content-desc="
                + mfiXpathLiteral(billing) + "]");
        mfiClickOnce(secondBilling);
        ReportLogger.pass("Selected the verified second billing-address variant");
        viewport = anniversaryReadViewport();
        viewport = anniversaryBringIntoView(viewport, AnniversaryCheck.exact("Continue"), "down", false);
        Assert.assertTrue(viewport.find(AnniversaryCheck.exact("Continue")).enabled,
                "Address checkout Continue is disabled");
        return viewport;
    }

    private AnniversaryViewport mfiBringAddressIntoView(AnniversaryViewport viewport, String section, String expected) {
        long deadline = System.nanoTime() + ExecutionProfile.longWait().toNanos();
        for (int swipe = 0; swipe <= 24 && System.nanoTime() <= deadline; swipe++) {
            AnniversaryNode node = mfiAddressInSection(viewport, section, expected);
            if (node != null && node.inScroll && node.visibleWithin(viewport.content)) return viewport;
            if (swipe == 24) break;
            viewport = anniversaryAdvance(viewport, node, "down", false, section + " configured address");
        }
        throw new AssertionError("Configured address could not be brought fully into view under " + section);
    }

    private AnniversaryNode mfiAddressInSection(AnniversaryViewport viewport, String section, String expected) {
        AnniversaryNode match = null;
        for (AnniversaryNode candidate : mfiSectionNodes(viewport, section)) {
            if (candidate.displayed && candidate.label.equals(expected)) {
                Assert.assertTrue(match == null, "Duplicate configured addresses within " + section);
                match = candidate;
            }
        }
        return match;
    }

    private List<AnniversaryNode> mfiSectionNodes(AnniversaryViewport viewport, String section) {
        List<AnniversaryNode> result = new ArrayList<>();
        AnniversaryNode heading = viewport.find(AnniversaryCheck.exact(section));
        if (heading == null) return result;
        for (Node sibling = heading.element.getNextSibling(); sibling != null; sibling = sibling.getNextSibling()) {
            if (!(sibling instanceof Element)) continue;
            AnniversaryNode candidate = new AnniversaryNode((Element) sibling);
            if (candidate.label.equals("Billing Address") || candidate.label.equals("Continue")
                    || candidate.label.startsWith("+ Add")) break;
            candidate.inScroll = heading.inScroll;
            result.add(candidate);
            // Allow an accessibility group around the address options without positional locators.
            NodeList descendants = candidate.element.getElementsByTagName("*");
            for (int i = 0; i < descendants.getLength(); i++) {
                AnniversaryNode child = new AnniversaryNode((Element) descendants.item(i));
                child.inScroll = heading.inScroll;
                result.add(child);
            }
        }
        return result;
    }

    private String mfiXpathLiteral(String value) {
        if (!value.contains("'")) return "'" + value + "'";
        if (!value.contains("\"")) return "\"" + value + "\"";
        return "concat('" + value.replace("'", "',\"'\",'") + "')";
    }

    private AnniversaryViewport mfiAwaitPrintOrderSummary() {
        final String[] lastState = {"No Order Summary snapshot received"};
        try {
            AnniversaryViewport viewport = anniversaryViewportWait(ExecutionProfile.longWait()).until(d -> {
                AnniversaryViewport current = anniversaryReadViewport();
                AnniversaryNode heading = current.find(AnniversaryCheck.exact("Order Summary"));
                AnniversaryNode product = current.find(AnniversaryCheck.exact("Mutual Fund Insight"));
                AnniversaryNode plan = current.find(AnniversaryCheck.prefix("Digital + Print-"));
                AnniversaryNode pay = current.find(AnniversaryCheck.exact("Pay Securely Now"));
                lastState[0] = "Order Summary=" + mfiMarkerState(current, "Order Summary")
                        + ", Digital + Print-3 Years=" + mfiMarkerState(current, "Digital + Print-3 Years")
                        + ", Pay Securely Now=" + mfiMarkerState(current, "Pay Securely Now")
                        + ", address screen=" + (current.find(AnniversaryCheck.exact("Billing Address")) != null);
                // Detect the new screen from its hierarchy. Content below the fold is
                // checked by the existing bounded scroll scan, never by a visibility-only wait.
                return current.scrollElement != null && heading != null
                        && (product != null || plan != null || pay != null) ? current : null;
            });
            ReportLogger.step("Order Summary detected; scrolling and verifying Print plan, account and amounts before payment");
            return viewport;
        } catch (TimeoutException failure) {
            throw new AssertionError("Print Order Summary did not load after Continue; last screen markers: "
                    + lastState[0], failure);
        }
    }

    private String mfiMarkerState(AnniversaryViewport viewport, String label) {
        AnniversaryNode node = viewport.find(AnniversaryCheck.exact(label));
        if (node == null) return "absent";
        return node.visibleWithin(node.inScroll ? viewport.content : viewport.screen) ? "visible" : "offscreen";
    }

    private AnniversaryViewport mfiAwaitCheckout() {
        return anniversaryViewportWait(ExecutionProfile.longWait()).until(d -> {
            AnniversaryViewport current = anniversaryReadViewport();
            boolean summary = mfiVisibleText(current, "Digital Only-3 Years")
                    && mfiVisibleText(current, "Pay Securely Now");
            boolean address = mfiVisibleText(current, "Checkout")
                    && mfiVisibleText(current, "Billing Address") && mfiVisibleText(current, "Continue");
            return summary || address ? current : null;
        });
    }

    private AnniversaryViewport mfiVerifyThreeYearCheckout(AnniversaryViewport viewport) {
        return mfiVerifyCheckout(viewport, "Checkout", "Digital Only-3 Years", "-5040", "2160", false);
    }

    private AnniversaryViewport mfiVerifyCheckout(AnniversaryViewport viewport, String heading, String plan,
                                                   String discount, String total, boolean print) {
        // Start at the actual screen heading even when checkout retained a previous scroll position.
        viewport = anniversaryBringIntoView(viewport, AnniversaryCheck.exact(heading), "up", false);
        List<AnniversaryCheck> pending = new ArrayList<>();
        for (String label : new LinkedHashSet<>(List.of(heading, "Order Summary", "Mutual Fund Insight", plan,
                "Logged in email:", ConfigReader.get("VRO_TEST_USER"),
                "Subtotal", "Anniversary discount", "Total")))
            pending.add(AnniversaryCheck.exact(label));
        Map<String, String> amounts = new LinkedHashMap<>();
        amounts.put("Subtotal", "7200");
        amounts.put("Anniversary discount", discount);
        amounts.put("Total", total);
        if (print) pending.add(AnniversaryCheck.exact("Shipping"));
        boolean complimentarySeen = false;
        // Inspect each viewport once. Optional offers must not become missing scroll targets
        // on repeat purchases; an offer that is displayed still needs its plan and ₹0 verified.
        for (int swipe = 0; swipe <= 40; swipe++) {
            if (!complimentarySeen && (viewport.find(AnniversaryCheck.exact("Free Complimentary Subscription")) != null
                    || viewport.find(AnniversaryCheck.exact("Digital Only 1 year")) != null)) {
                complimentarySeen = true;
                pending.add(AnniversaryCheck.exact("Free Complimentary Subscription"));
                pending.add(AnniversaryCheck.exact("Digital Only 1 year"));
                amounts.put("Free Complimentary Subscription", "0");
            }
            AnniversaryNode partial = null;
            for (java.util.Iterator<AnniversaryCheck> iterator = pending.iterator(); iterator.hasNext();) {
                AnniversaryCheck check = iterator.next();
                AnniversaryNode node = viewport.find(check);
                if (node == null || !node.inScroll) continue;
                if (!node.visibleWithin(viewport.content)) {
                    if (partial == null && node.bounds.intersects(viewport.content)) partial = node;
                    continue;
                }
                if (amounts.containsKey(check.label)) {
                    AnniversaryNode value = mfiFollowingValue(node);
                    Assert.assertNotNull(value, "Missing checkout value beside " + check.label);
                    if (!value.visibleWithin(viewport.content)) {
                        if (partial == null) partial = value;
                        continue;
                    }
                    mfiAssertMoney(value.label, amounts.get(check.label), check.label);
                }
                if (print && check.label.equals("Shipping")) {
                    AnniversaryNode value = mfiFollowingValue(node);
                    Assert.assertNotNull(value, "Missing Shipping value");
                    if (!value.visibleWithin(viewport.content)) {
                        if (partial == null) partial = value;
                        continue;
                    }
                    Assert.assertEquals(value.label, "Free", "Print shipping must be Free");
                }
                check.verify(node);
                ReportLogger.pass("Verified Three-year checkout: " + node.label
                        + (amounts.containsKey(check.label) ? " = " + mfiFollowingValue(node).label : ""));
                iterator.remove();
            }
            if (pending.isEmpty()) {
                if (!complimentarySeen)
                    ReportLogger.info("No complimentary section displayed; three-year checkout requirements verified");
                ReportLogger.pass("Checkout verified: " + plan + "; subtotal ₹7,200, discount ₹" + discount + ", total ₹" + total);
                return viewport;
            }
            if (swipe == 40) break;
            viewport = anniversaryAdvance(viewport, partial, "down", false, anniversaryMissing(pending));
        }
        throw new AssertionError("Three-year checkout scan reached its swipe limit; missing: "
                + anniversaryMissing(pending));
    }

    private AnniversaryNode mfiFollowingValue(AnniversaryNode label) {
        // Content descriptions are flattened siblings in the supplied checkout hierarchy.
        // Resolve by semantic label, never by a positional XPath or UiSelector.instance().
        for (Node next = label.element.getNextSibling(); next != null; next = next.getNextSibling()) {
            if (!(next instanceof Element)) continue;
            AnniversaryNode value = new AnniversaryNode((Element) next);
            if (value.displayed && !value.label.isEmpty()) {
                value.inScroll = label.inScroll;
                return value;
            }
        }
        return null;
    }

    private void mfiAssertMoney(String label, String expected, String field) {
        String value = label.replaceAll("[₹,\\s]", "");
        Assert.assertTrue(label.contains("₹") && value.matches("-?\\d+(?:\\.\\d{1,2})?"),
                "Invalid monetary value for " + field);
        Assert.assertTrue(new BigDecimal(value).compareTo(new BigDecimal(expected)) == 0,
                "Unexpected amount for " + field + ": " + label);
    }

    private By mfiPaymentId(String suffix) {
        return By.id(ConfigReader.get("VROappPackage") + ":id/" + suffix);
    }

    private AnniversaryNode mfiResource(AnniversaryViewport viewport, String suffix) {
        String id = ConfigReader.get("VROappPackage") + ":id/" + suffix;
        for (AnniversaryNode node : viewport.nodes)
            if (id.equals(node.element.getAttribute("resource-id")) && node.visibleWithin(viewport.screen))
                return node;
        return null;
    }

    private boolean mfiResourceVisible(AnniversaryViewport viewport, String suffix) {
        return mfiResource(viewport, suffix) != null;
    }

    private boolean mfiVisibleText(AnniversaryViewport viewport, String label) {
        AnniversaryNode node = viewport.find(AnniversaryCheck.exact(label));
        return node != null && node.visibleWithin(node.inScroll ? viewport.content : viewport.screen);
    }

    private void mfiAssertResourceText(AnniversaryViewport viewport, String suffix, String expected) {
        AnniversaryNode node = mfiResource(viewport, suffix);
        Assert.assertNotNull(node, "PayU element is not visible: " + suffix);
        Assert.assertEquals(node.label, expected, "Unexpected PayU label: " + suffix);
    }

    private WebDriverWait mfiPaymentWait() {
        String value = ConfigReader.getOptional("MFIpaymentTimeoutSeconds", "120");
        int seconds;
        try { seconds = Integer.parseInt(value); }
        catch (NumberFormatException e) { throw new IllegalArgumentException("Invalid MFIpaymentTimeoutSeconds", e); }
        Assert.assertTrue(seconds > 0, "MFIpaymentTimeoutSeconds must be positive");
        return anniversaryViewportWait(Duration.ofSeconds(seconds));
    }

    private void mfiType(String suffix, String value, boolean masked) {
        for (int attempt = 0; attempt < 2; attempt++) {
            try {
                WebElement input = mfiPaymentWait().until(d -> {
                    WebElement current = driver.findElement(mfiPaymentId(suffix));
                    return current.isDisplayed() && current.isEnabled() ? current : null;
                });
                input.clear();
                input.sendKeys(value);
                mfiPaymentWait().until(d -> {
                    String actual = driver.findElement(mfiPaymentId(suffix)).getText().replaceAll("\\s", "");
                    return actual.replaceAll("[^0-9]", "").equals(value.replaceAll("[^0-9]", ""))
                            || (masked && actual.matches("[•●*]{" + value.length() + "}"));
                });
                return;
            } catch (StaleElementReferenceException e) {
                if (attempt == 1) throw e;
            }
        }
    }

    private void mfiHideKeyboard() {
        if (driver.isKeyboardShown()) {
            driver.hideKeyboard();
            mfiPaymentWait().until(d -> !driver.isKeyboardShown());
        }
    }

    private void mfiClickOnce(By locator) {
        WebElement button = mfiPaymentWait().until(d -> {
            WebElement current = driver.findElement(locator);
            return current.isDisplayed() && current.isEnabled() ? current : null;
        });
        // Never repeat a payment/OTP click after a lost response.
        button.click();
    }

    private void mfiOpenManualOtp() {
        final boolean[] nudgeHandled = {false};
        final boolean[] manualOtpRequested = {false};
        mfiPaymentWait().until(d -> {
            AnniversaryViewport current = anniversaryReadViewport();
            if (mfiResourceVisible(current, "etEnterOtpEditTextSubmitOtp")) {
                mfiAssertResourceText(current, "tvTitleText", "SUBMIT OTP");
                mfiAssertResourceText(current, "btnSubmitOtpManual", "SUBMIT");
                return true;
            }
            if (mfiResourceVisible(current, "tvCheckoutText") && !nudgeHandled[0]) {
                mfiAssertResourceText(current, "tvCheckoutText", "Secure your card for faster checkout");
                mfiAssertResourceText(current, "tvContinueWithoutSaving", "Continue without saving");
                nudgeHandled[0] = true;
                mfiClickOnce(mfiPaymentId("tvContinueWithoutSaving"));
            }
            AnniversaryNode countdown = mfiResource(current, "tvWaitingForOtp");
            if (countdown != null) {
                Assert.assertTrue(countdown.label.startsWith("WAITING FOR OTP"), "Unexpected OTP countdown");
                if (!manualOtpRequested[0]) {
                    // This clickable TextView opens the editable OTP field immediately.
                    // Locate by resource ID; the remaining seconds are irrelevant.
                    manualOtpRequested[0] = true;
                    ReportLogger.step("Tapping PayU OTP bar to enter the sandbox OTP immediately");
                    mfiClickOnce(mfiPaymentId("tvWaitingForOtp"));
                }
            }
            return false;
        });
    }

    private void mfiVerifySuccessAndReturnHome(AnniversaryViewport viewport) {
        mfiVerifySuccessAndReturnHome(viewport, "₹2,160.00 Total Paid");
    }

    private void mfiVerifySuccessAndReturnHome(AnniversaryViewport viewport, String paidLabel) {
        AnniversaryNode order = viewport.find(AnniversaryCheck.prefix("Order ID:"));
        Assert.assertTrue(order != null && order.label.matches("Order ID:\\s*\\d+"),
                "Order success must contain a generated order ID");
        List<AnniversaryCheck> checks = new ArrayList<>();
        for (String label : List.of("Order successfully!", order.label,
                "We have sent you a confirmation email with the order details.", "Payment details",
                paidLabel, "Paid via PayU", ConfigReader.get("VRO_TEST_USER"),
                "Download your digital copy"))
            checks.add(AnniversaryCheck.exact(label));
        // Address assertions belong to checkout. The success route verifies the completed order.
        viewport = anniversaryScanVisible(viewport, checks, "Order success", true);
        ReportLogger.pass("Download your digital copy is present; order ID verified: " + order.label);
        // Presence only: never click the Download button.
        viewport = mfiOpenSuccessSubscriptions(viewport, order.label);
        ReportLogger.pass("My Subscriptions screen verified");
        anniversaryTap(homeTab);
        resetToVroHome();
        ReportLogger.pass("Purchase returned to VRO Home through the Home tab");
    }

    private AnniversaryViewport mfiOpenSuccessSubscriptions(AnniversaryViewport viewport, String orderLabel) {
        AnniversaryNode link = null;
        int unchanged = 0;
        for (int swipe = 0; swipe <= 12; swipe++) {
            link = mfiSuccessLink(viewport);
            if (link != null && link.enabled && mfiFullyVisible(viewport, link)) break;
            Assert.assertTrue(swipe < 12 && unchanged < 2, "My Subscription hyperlink could not be brought into view");
            AnniversaryViewport next = mfiScrollViewport(viewport, link, "down", "My Subscription");
            unchanged = viewport.signature.equals(next.signature) ? unchanged + 1 : 0;
            viewport = next;
        }
        final AnniversaryNode visibleLink = link;
        Assert.assertNotNull(visibleLink, "Order success has no My Subscription control");
        ReportLogger.step("Opening My Subscriptions through the Order Success hyperlink");
        mfiClickNode(viewport, visibleLink);
        try {
            return mfiWaitScreen(ExecutionProfile.shortWait(), this::mfiSubscriptionsReady,
                    "My Subscriptions after the order-success hyperlink");
        } catch (TimeoutException firstClickTimeout) {
            // Retry navigation only. Payment and OTP submission are never repeated.
            AnniversaryViewport current = anniversaryReadViewport();
            AnniversaryNode fresh = mfiSuccessLink(current);
            boolean sameOrder = current.find(AnniversaryCheck.exact(orderLabel)) != null
                    || current.find(AnniversaryCheck.exact("Download your digital copy")) != null;
            if (sameOrder && fresh != null && fresh.enabled && mfiFullyVisible(current, fresh)) {
                ReportLogger.info("My Subscription click left the success screen unchanged; tapping the visible link bounds");
                mfiTapBounds(current, fresh);
            }
            return mfiWaitScreen(ExecutionProfile.longWait(), this::mfiSubscriptionsReady,
                    "My Subscriptions after order success; payment was not repeated");
        }
    }

    private AnniversaryNode mfiSuccessLink(AnniversaryViewport viewport) {
        AnniversaryNode exact = viewport.find(AnniversaryCheck.exact("My Subscription"));
        if (exact != null) return exact;
        AnniversaryNode candidate = null;
        for (AnniversaryNode node : viewport.nodes) {
            // A merged paragraph is usable only if the app exposes it as a clickable control.
            if (node.label.contains("My Subscription") && !node.label.contains("My Subscriptions")
                    && "true".equals(node.element.getAttribute("clickable"))
                    && mfiFullyVisible(viewport, node)
                    && (candidate == null || node.label.length() < candidate.label.length())) candidate = node;
        }
        return candidate;
    }

    private boolean mfiSubscriptionsReady(AnniversaryViewport viewport) {
        return mfiVisibleText(viewport, "My Subscriptions")
                && mfiVisibleText(viewport, "Mutual Fund Insight") && mfiVisibleText(viewport, "Home");
    }

    // TC_008: archive scrolling ends before Read Stories; magazine verification uses taps only.
    public void verifyProfileSubscriptionHistory() {
        ensureHomePage();
        anniversaryWithFastPolling(() -> {
            AnniversaryViewport viewport = anniversaryReadViewport();
            AnniversaryNode profile = mfiProfileTab(viewport);
            Assert.assertTrue(profile != null && mfiFullyVisible(viewport, profile), "Home Profile tab is missing");
            viewport = mfiNavigate(viewport, profile, this::mfiProfileReady, "Profile");
            viewport = mfiNavigate(viewport, viewport.find(AnniversaryCheck.exact("Subscriptions")),
                    this::mfiSubscriptionsReady, "My Subscriptions");
            viewport = mfiNavigate(viewport, viewport.find(AnniversaryCheck.exact("Mutual Fund Insight")),
                    this::mfiIssueListReady, "Mutual Fund Insight issues");
            viewport = mfiRevealIssueControl(viewport, "See All Issues", "down");
            viewport = mfiNavigate(viewport, viewport.find(AnniversaryCheck.exact("See All Issues")),
                    this::mfiArchiveScreen, "Issue archive");
            for (int year : new int[]{2025, 2024}) {
                viewport = mfiRevealIssueControl(viewport, String.valueOf(year), "up");
                Assert.assertTrue(mfiArchiveScreen(viewport), "Year selection must start on the issue archive");
                ReportLogger.step("Tapping archive year by accessibility ID: " + year);
                viewport = mfiNavigate(viewport, viewport.find(AnniversaryCheck.exact(String.valueOf(year))),
                        v -> mfiArchiveYearReady(mfiScopeIssues(v), year), "Archive " + year);
                viewport = mfiScopeIssues(viewport);
                viewport = mfiScrollToArchiveTop(viewport, year);
                viewport = mfiVerifyArchive(viewport, year);
                viewport = mfiScrollToArchiveTop(viewport, year);
            }
            // Finish the 2024 scan at the bottom, then explicitly restore its top before leaving.
            ReportLogger.step("2024 archive returned to its top; exiting with Back");
            viewport = mfiBackTo(viewport, this::mfiArchiveScreen, this::mfiIssueListReady,
                    "Mutual Fund Insight after leaving the archive");
            viewport = mfiRevealIssueControl(viewport, "Read Stories", "up");
            Assert.assertTrue(mfiIssueListReady(viewport), "Read Stories must start on the subscription issue list");
            viewport = mfiNavigate(viewport, viewport.find(AnniversaryCheck.exact("Read Stories")),
                    this::mfiIssueStoriesReady, "Magazine issue stories");
            viewport = mfiVerifyVisibleMagazineIssues(viewport);
            viewport = mfiBackTo(viewport, this::mfiIssueStoriesReady, this::mfiIssueListReady,
                    "My Subscriptions after Read Stories");
            viewport = mfiBackTo(viewport, this::mfiIssueListReady, this::mfiProfileReady,
                    "Profile after My Subscriptions");
            mfiNavigate(viewport, viewport.find(AnniversaryCheck.exact("Home")),
                    v -> mfiVisibleText(v, "Subscribe") && v.find(AnniversaryCheck.exact("All Stories")) != null,
                    "VRO Home");
            resetToVroHome();
            ReportLogger.pass("TC_008 completed: archives and three visible magazine issues verified; VRO Home restored");
        });
    }

    private AnniversaryNode mfiProfileTab(AnniversaryViewport viewport) {
        for (AnniversaryNode node : viewport.nodes)
            if (node.label.endsWith(" Profile") && !node.inScroll && mfiFullyVisible(viewport, node)) return node;
        return viewport.find(AnniversaryCheck.exact("Profile"));
    }

    private boolean mfiProfileReady(AnniversaryViewport viewport) {
        return mfiVisibleText(viewport, ConfigReader.get("VRO_TEST_USER"))
                && mfiVisibleText(viewport, "My Account") && mfiVisibleText(viewport, "Subscriptions");
    }

    private boolean mfiIssueListReady(AnniversaryViewport viewport) {
        // Back can retain the list's bottom position; the top Read Stories control may be virtualized.
        // Establish the route from its semantic markers; reveal the required control before clicking it.
        return viewport.nodes.stream().anyMatch(n -> "My Subscriptions".equals(n.label))
                && viewport.nodes.stream().anyMatch(n -> "Mutual Fund Insight".equals(n.label))
                && mfiVisibleText(viewport, "Home") && !mfiArchiveScreen(viewport)
                && (viewport.find(AnniversaryCheck.exact("Read Stories")) != null
                        || viewport.find(AnniversaryCheck.exact("See All Issues")) != null)
                && viewport.nodes.stream().anyMatch(n -> n.inScroll && n.displayed && mfiIssueMonth(n.label) != null);
    }

    private boolean mfiArchiveScreen(AnniversaryViewport viewport) {
        // Archive identity is different from the main list, even when its year tabs scroll offscreen.
        return mfiVisibleText(viewport, "My Subscriptions")
                && viewport.find(AnniversaryCheck.exact("2025")) != null
                && viewport.find(AnniversaryCheck.exact("2024")) != null;
    }

    private boolean mfiArchiveYearReady(AnniversaryViewport viewport, int year) {
        return mfiArchiveScreen(viewport) && mfiVisibleText(viewport, String.valueOf(year))
                && viewport.nodes.stream().noneMatch(n -> {
                    YearMonth month = mfiIssueMonth(n.label);
                    return month != null && month.getYear() != year && mfiFullyVisible(viewport, n);
                })
                && viewport.nodes.stream().anyMatch(n -> {
                    YearMonth month = mfiIssueMonth(n.label);
                    return month != null && month.getYear() == year && mfiFullyVisible(viewport, n);
                });
    }

    private static final DateTimeFormatter MFI_ISSUE_DATE = new DateTimeFormatterBuilder()
            .parseCaseInsensitive().appendPattern("dd MMM uuuu").toFormatter(Locale.ENGLISH)
            .withResolverStyle(ResolverStyle.STRICT);
    private static final DateTimeFormatter MFI_ISSUE_MONTH = DateTimeFormatter.ofPattern("MMMM uuuu", Locale.ENGLISH);

    private YearMonth mfiIssueMonth(String label) {
        if (!label.matches("01 [A-Za-z]{3} \\d{4}")) return null;
        try { return YearMonth.from(LocalDate.parse(label, MFI_ISSUE_DATE)); }
        catch (DateTimeParseException invalidDate) { throw new AssertionError("Invalid magazine issue date: " + label); }
    }

    private YearMonth mfiDropdownMonth(String label) {
        try { return YearMonth.parse(label, MFI_ISSUE_MONTH); }
        catch (DateTimeParseException notAnOption) { return null; }
    }

    private AnniversaryViewport mfiVerifyVisibleMagazineIssues(AnniversaryViewport viewport) {
        // Capture the opening viewport once. Neither this screen nor its popup may receive a swipe.
        final AnniversaryBounds initialViewport = mfiMagazineViewport(viewport);
        Assert.assertTrue(initialViewport.valid(), "Magazine screen has no usable initial viewport");
        viewport = mfiWaitScreen(ExecutionProfile.defaultWait(),
                v -> mfiMagazineReady(v, initialViewport, null), "Initial magazine asset and issue controls");
        ReportLogger.step("Magazine verification: visible options only; scrolling disabled");
        // These exact accessibility labels are present in the supplied initial dropdown viewport.
        for (YearMonth month : List.of(YearMonth.of(2025, 10), YearMonth.of(2025, 5), YearMonth.of(2024, 12)))
            viewport = mfiVerifyIssueSelection(viewport, month, initialViewport);
        return viewport;
    }

    private AnniversaryBounds mfiMagazineViewport(AnniversaryViewport viewport) {
        AnniversaryBounds area = viewport.content.intersection(viewport.screen);
        int top = area.top, bottom = area.bottom;
        for (AnniversaryNode node : viewport.nodes) {
            if (!node.visibleWithin(viewport.screen)) continue;
            if (!node.inScroll && "Home".equals(node.label)) bottom = Math.min(bottom, node.bounds.top);
            String id = node.element.getAttribute("resource-id");
            if ("android:id/statusBarBackground".equals(id)) top = Math.max(top, node.bounds.bottom);
            if ("android:id/navigationBarBackground".equals(id)) bottom = Math.min(bottom, node.bounds.top);
        }
        return new AnniversaryBounds(area.left, top, area.right, bottom);
    }

    private boolean mfiMagazineReady(AnniversaryViewport viewport, AnniversaryBounds initialViewport, YearMonth expected) {
        AnniversaryNode value = mfiIssueDropdown(viewport), cover = mfiIssueCover(viewport);
        return mfiIssueStoriesReady(viewport) && mfiIssueOptions(viewport, false).isEmpty()
                && value != null && value.enabled && value.visibleWithin(initialViewport)
                && (expected == null || expected.equals(mfiDropdownMonth(value.label)))
                && cover != null && cover.visibleWithin(initialViewport)
                && mfiFullyVisible(viewport, value) && mfiFullyVisible(viewport, cover)
                && mfiOptionVisible(viewport, cover) && mfiMagazineViewport(viewport).contains(cover.bounds);
    }

    private List<AnniversaryNode> mfiIssueOptions(AnniversaryViewport viewport, boolean visibleOnly) {
        List<AnniversaryNode> options = new ArrayList<>();
        AnniversaryNode field = mfiIssueDropdown(viewport);
        for (AnniversaryNode node : viewport.nodes) {
            if ((field != null && node.element == field.element)
                    || !"android.widget.Button".equals(node.element.getAttribute("class"))
                    || !node.enabled || !node.displayed
                    || !node.bounds.valid() || mfiDropdownMonth(node.label) == null) continue;
            if (mfiIssueOptionContainer(viewport, node) == null) continue;
            if (!visibleOnly || mfiOptionVisible(viewport, node)) options.add(node);
        }
        return options;
    }

    private Element mfiIssueOptionContainer(AnniversaryViewport viewport, AnniversaryNode option) {
        for (Node parent = option.element.getParentNode(); parent instanceof Element; parent = parent.getParentNode()) {
            Element element = (Element) parent;
            String type = element.getAttribute("class");
            if (!"true".equals(element.getAttribute("scrollable")) && !type.endsWith("ScrollView")
                    && !type.endsWith("ListView") && !type.endsWith("RecyclerView")) continue;
            // The main field is not a popup option, even when it has the same Month Year accessibility ID.
            for (AnniversaryNode heading : viewport.nodes) {
                if (!"Magazine Issue".equals(heading.label)) continue;
                for (Node ancestor = heading.element; ancestor != null; ancestor = ancestor.getParentNode())
                    if (ancestor == element) return null;
            }
            return element;
        }
        return null;
    }

    private AnniversaryViewport mfiVerifyArchive(AnniversaryViewport viewport, int year) {
        viewport = mfiScopeIssues(viewport);
        Set<YearMonth> advertised = new LinkedHashSet<>();
        Set<YearMonth> verified = new LinkedHashSet<>();
        int unchangedDown = 0;
        for (int gesture = 0; gesture <= 60; gesture++) {
            AnniversaryNode partial = null;
            for (AnniversaryNode date : viewport.nodes) {
                YearMonth month = mfiIssueMonth(date.label);
                if (month == null || !date.inScroll || !date.displayed) continue;
                Assert.assertTrue(month.getYear() == year, "Wrong archive year: " + date.label);
                advertised.add(month);
                if (verified.contains(month)) continue;
                MfiMagazineCard card = mfiMagazineCard(viewport, date);
                if (card != null && card.fullyVisible(viewport)) {
                    verified.add(month);
                    ReportLogger.pass("Archive " + year + ": " + date.label + " cover, Read and Download visible (not clicked)");
                } else if (partial == null) {
                    AnniversaryNode anchor = card == null ? date : card.scrollAnchor();
                    if (anchor.bounds.intersects(viewport.content) && !anchor.visibleWithin(viewport.content))
                        partial = anchor;
                }
            }
            if (unchangedDown >= 2 && partial == null) {
                Assert.assertTrue(!advertised.isEmpty(), "Archive " + year + " has no issues");
                Set<YearMonth> missing = new LinkedHashSet<>(advertised);
                missing.removeAll(verified);
                Assert.assertTrue(missing.isEmpty(), "Archive ended with unverified issue cards: " + missing);
                ReportLogger.pass("All " + verified.size() + " available " + year + " issues verified");
                return viewport;
            }
            Assert.assertTrue(gesture < 60, "Archive " + year + " exceeded its bounded scroll scan");
            // A Flutter scrollGesture false result is not sufficient evidence that every card was inspected.
            // Align a partial card first; confirm the boundary with two unchanged downward W3C gestures.
            AnniversaryViewport next = mfiScrollIssues(viewport, partial, "down", "Archive " + year);
            boolean changed = mfiIssuesMoved(viewport, next);
            if (partial != null) {
                Assert.assertTrue(changed || unchangedDown < 1,
                        "Archive card cannot be fully revealed: " + partial.label);
                unchangedDown = changed ? 0 : unchangedDown + 1;
            } else unchangedDown = changed ? 0 : unchangedDown + 1;
            viewport = next;
        }
        throw new AssertionError("Archive scan did not finish: " + year);
    }

    private MfiMagazineCard mfiMagazineCard(AnniversaryViewport viewport, AnniversaryNode date) {
        AnniversaryNode cover = mfiCardPart(viewport, date, null, "android.widget.ImageView", date.bounds.bottom);
        if (cover == null) return null;
        AnniversaryNode read = mfiCardPart(viewport, date, "Read", null, cover.bounds.bottom);
        AnniversaryNode download = mfiCardPart(viewport, date, "Download", null, cover.bounds.bottom);
        return read == null || download == null ? null : new MfiMagazineCard(date, cover, read, download);
    }

    private AnniversaryNode mfiCardPart(AnniversaryViewport viewport, AnniversaryNode date,
            String label, String type, int minimumY) {
        int columnX = date.bounds.left + date.bounds.width() / 2;
        int nextRow = Integer.MAX_VALUE;
        for (AnniversaryNode node : viewport.nodes)
            if (node != date && node.inScroll && mfiIssueMonth(node.label) != null
                    && node.bounds.top > date.bounds.top && node.bounds.left <= columnX && node.bounds.right >= columnX)
                nextRow = Math.min(nextRow, node.bounds.top);
        AnniversaryNode closest = null;
        for (AnniversaryNode node : viewport.nodes) {
            if (!node.inScroll || !node.displayed || !node.bounds.valid() || node.bounds.top < minimumY
                    || node.bounds.top >= nextRow || node.bounds.left > columnX || node.bounds.right < columnX) continue;
            if (label != null ? !label.equals(node.label)
                    : !node.label.isEmpty() || !type.equals(node.element.getAttribute("class"))) continue;
            if (closest == null || node.bounds.top < closest.bounds.top) closest = node;
        }
        return closest;
    }

    private static class MfiMagazineCard {
        private final AnniversaryNode date, cover, read, download;
        private MfiMagazineCard(AnniversaryNode date, AnniversaryNode cover, AnniversaryNode read, AnniversaryNode download) {
            this.date = date; this.cover = cover; this.read = read; this.download = download;
        }
        private boolean fullyVisible(AnniversaryViewport viewport) {
            return date.visibleWithin(viewport.content) && cover.visibleWithin(viewport.content)
                    && read.visibleWithin(viewport.content) && download.visibleWithin(viewport.content);
        }
        private AnniversaryNode scrollAnchor() {
            Element element = (Element) date.element.cloneNode(false);
            element.setAttribute("bounds", new AnniversaryBounds(
                    Math.min(date.bounds.left, cover.bounds.left), date.bounds.top,
                    Math.max(cover.bounds.right, Math.max(read.bounds.right, download.bounds.right)),
                    Math.max(read.bounds.bottom, download.bounds.bottom)).toString());
            AnniversaryNode anchor = new AnniversaryNode(element);
            anchor.inScroll = true;
            return anchor;
        }
    }

    private AnniversaryNode mfiIssueDropdown(AnniversaryViewport viewport) {
        AnniversaryNode heading = viewport.find(AnniversaryCheck.exact("Magazine Issue"));
        if (heading == null) return null;
        for (Node sibling = heading.element.getNextSibling(); sibling != null; sibling = sibling.getNextSibling()) {
            if (!(sibling instanceof Element)) continue;
            AnniversaryNode node = new AnniversaryNode((Element) sibling);
            node.inScroll = heading.inScroll;
            if (mfiDropdownMonth(node.label) != null) return node;
            if (!node.label.isEmpty()) break;
        }
        return null;
    }

    private AnniversaryNode mfiIssueCover(AnniversaryViewport viewport) {
        AnniversaryNode dropdown = mfiIssueDropdown(viewport);
        if (dropdown == null) return null;
        for (Node sibling = dropdown.element.getNextSibling(); sibling != null; sibling = sibling.getNextSibling()) {
            if (!(sibling instanceof Element)) continue;
            AnniversaryNode node = new AnniversaryNode((Element) sibling);
            node.inScroll = dropdown.inScroll;
            if ("android.widget.ImageView".equals(node.element.getAttribute("class")) && node.label.isEmpty()) return node;
            if (!node.label.isEmpty()) break;
        }
        return null;
    }

    private boolean mfiIssueStoriesReady(AnniversaryViewport viewport) {
        return mfiVisibleText(viewport, "Mutual Fund Insight") && mfiVisibleText(viewport, "Magazine Issue")
                && mfiIssueDropdown(viewport) != null;
    }

    private String mfiStorySignature(AnniversaryViewport viewport) {
        StringBuilder titles = new StringBuilder();
        for (AnniversaryNode node : viewport.nodes)
            if (node.inScroll && mfiFullyVisible(viewport, node) && STORY_METADATA.matcher(node.label).find())
                titles.append(node.label).append('|');
        return titles.toString();
    }

    private AnniversaryViewport mfiVerifyIssueSelection(AnniversaryViewport viewport, YearMonth expected,
                                                       AnniversaryBounds initialViewport) {
        Assert.assertTrue(mfiMagazineReady(viewport, initialViewport, null),
                "Magazine controls and cover must be fully visible before opening the dropdown; scrolling is disabled");
        AnniversaryNode dropdown = mfiIssueDropdown(viewport);
        Assert.assertTrue(dropdown != null && mfiFullyVisible(viewport, dropdown), "Magazine issue dropdown is not visible");
        YearMonth beforeMonth = mfiDropdownMonth(dropdown.label);
        String beforeStories = mfiStorySignature(viewport);
        mfiClickNode(viewport, dropdown);
        AnniversaryViewport menu = mfiFindIssueOption(expected);
        // Use fresh source bounds; the popup can replace the underlying Flutter semantics.
        AnniversaryNode option = mfiOption(menu, expected);
        Assert.assertNotNull(option, "Issue option disappeared: " + expected.format(MFI_ISSUE_MONTH));
        Assert.assertTrue(mfiOptionVisible(menu, option), "Issue option is clipped by the dropdown list");
        mfiClickIssueOption(menu, option);
        final String previousStories = beforeStories;
        final boolean changedMonth = !expected.equals(beforeMonth);
        AnniversaryViewport selected = anniversaryViewportWait(ExecutionProfile.defaultWait()).until(d -> {
            AnniversaryViewport current = anniversaryReadViewport();
            if (!mfiMagazineReady(current, initialViewport, expected)) return null;
            String stories = mfiStorySignature(current);
            if (stories.isEmpty() || (changedMonth && !previousStories.isEmpty() && previousStories.equals(stories))) return null;
            // Assert the selected issue and the asset's full bounds; never click the cover or swipe this screen.
            return current;
        });
        ReportLogger.pass("Magazine Issue " + expected.format(MFI_ISSUE_MONTH)
                + ": selected month, magazine asset entirely within the initial viewport and refreshed stories verified");
        return selected;
    }

    private AnniversaryNode mfiOption(AnniversaryViewport viewport, YearMonth expected) {
        AnniversaryNode partial = null;
        for (AnniversaryNode node : mfiIssueOptions(viewport, false))
            if (expected.equals(mfiDropdownMonth(node.label))) {
                if (mfiOptionVisible(viewport, node)) return node;
                if (partial == null) partial = node;
            }
        return partial;
    }

    private boolean mfiOptionVisible(AnniversaryViewport viewport, AnniversaryNode option) {
        if (!option.visibleWithin(viewport.screen)) return false;
        for (Node parent = option.element.getParentNode(); parent instanceof Element; parent = parent.getParentNode()) {
            Element element = (Element) parent;
            String type = element.getAttribute("class");
            if ("true".equals(element.getAttribute("scrollable")) || type.endsWith("ScrollView")
                    || type.endsWith("ListView") || type.endsWith("RecyclerView")) {
                AnniversaryBounds clip = AnniversaryBounds.parse(element.getAttribute("bounds")).intersection(viewport.screen);
                if (!clip.contains(option.bounds)) return false;
            }
        }
        return true;
    }

    private AnniversaryViewport mfiFindIssueOption(YearMonth expected) {
        // Poll only: a missing or clipped option must never trigger popup/page scrolling.
        return mfiWaitScreen(ExecutionProfile.shortWait(), v -> {
            AnniversaryNode option = mfiOption(v, expected);
            return option != null && mfiOptionVisible(v, option);
        }, "Visible issue option " + expected.format(MFI_ISSUE_MONTH) + " (scrolling disabled)");
    }

    private void mfiClickIssueOption(AnniversaryViewport viewport, AnniversaryNode option) {
        Assert.assertTrue(option != null && option.enabled && mfiOptionVisible(viewport, option)
                        && "android.widget.Button".equals(option.element.getAttribute("class")),
                "Issue option must be an enabled, fully visible dropdown button");
        // The selected field is an ImageView with the same label; scope the click to the popup Button.
        By locator = AppiumBy.androidUIAutomator("new UiSelector().className(\"android.widget.Button\")"
                + ".description(\"" + mfiSelectorLiteral(option.label) + "\")");
        try {
            driver.findElement(locator).click();
        } catch (NoSuchElementException | StaleElementReferenceException changedOption) {
            mfiFindIssueOption(mfiDropdownMonth(option.label));
            driver.findElement(locator).click();
        }
    }

    private boolean mfiFullyVisible(AnniversaryViewport viewport, AnniversaryNode node) {
        return node != null && node.visibleWithin(node.inScroll ? viewport.content : viewport.screen);
    }

    private AnniversaryViewport mfiWaitScreen(Duration timeout,
            java.util.function.Predicate<AnniversaryViewport> ready, String stage) {
        try {
            return anniversaryViewportWait(timeout).until(d -> {
                AnniversaryViewport current = anniversaryReadViewport();
                if (!ready.test(current)) return null;
                ReportLogger.step(stage + " screen verified");
                return current;
            });
        } catch (TimeoutException failure) {
            throw new TimeoutException("Timed out verifying " + stage, failure);
        }
    }

    private AnniversaryViewport mfiRevealControl(AnniversaryViewport viewport, String label, String direction) {
        int unchanged = 0;
        for (int gesture = 0; gesture <= 40; gesture++) {
            AnniversaryNode target = viewport.find(AnniversaryCheck.exact(label));
            // Year tabs and navigation controls can live outside the main scroll subtree.
            if (target != null && target.enabled && mfiFullyVisible(viewport, target)) return viewport;
            Assert.assertTrue(gesture < 40 && unchanged < 2,
                    "Control could not be brought fully into view: " + label);
            AnniversaryViewport next = mfiScrollViewport(viewport, target, direction, label);
            unchanged = viewport.signature.equals(next.signature) ? unchanged + 1 : 0;
            viewport = next;
        }
        throw new AssertionError("Navigation scroll scan did not finish: " + label);
    }

    private AnniversaryViewport mfiScrollToArchiveTop(AnniversaryViewport viewport, int year) {
        viewport = mfiScopeIssues(viewport);
        int unchanged = 0;
        for (int gesture = 0; gesture < 32; gesture++) {
            AnniversaryViewport next = mfiScrollIssues(viewport, null, "up", "Archive " + year + " top");
            unchanged = mfiIssuesMoved(viewport, next) ? 0 : unchanged + 1;
            viewport = next;
            if (unchanged >= 2) {
                // The outer header can collapse while the nested list scrolls. Restore it separately.
                AnniversaryViewport outer = anniversaryReadViewport();
                viewport = mfiScopeIssues(mfiRevealControl(outer, "My Subscriptions", "up"));
                Assert.assertTrue(mfiArchiveYearReady(viewport, year),
                        "Archive did not show the selected year after returning to its top: " + year);
                ReportLogger.step("Archive " + year + " top verified using its issue positions");
                return viewport;
            }
        }
        throw new AssertionError("Archive " + year + " could not be returned to its top");
    }

    private AnniversaryViewport mfiScrollViewport(AnniversaryViewport before, AnniversaryNode target,
            String preferredDirection, String stage) {
        return mfiScrollViewport(before, target, preferredDirection, stage, false);
    }

    private AnniversaryViewport mfiScrollIssues(AnniversaryViewport before, AnniversaryNode target,
            String direction, String stage) {
        return mfiScrollViewport(before, target, direction, stage, true);
    }

    private AnniversaryViewport mfiScrollViewport(AnniversaryViewport before, AnniversaryNode target,
            String preferredDirection, String stage, boolean issues) {
        if (issues) before = mfiScopeIssues(before);
        final AnniversaryViewport origin = before;
        AnniversaryBounds area = before.content;
        Assert.assertTrue(before.scrollElement != null && area.height() > 40 && area.width() > 40,
                "No visible scroll area for " + stage);
        String direction = preferredDirection;
        double fraction = 0.45;
        if (target != null && target.inScroll && target.bounds.valid()) {
            Assert.assertTrue(target.bounds.height() <= area.height(),
                    "Archive control/card exceeds the viewport: " + target.label);
            if (target.bounds.top < area.top) {
                direction = "up";
                fraction = (double) (area.top - target.bounds.top + 20) / area.height();
            } else if (target.bounds.bottom > area.bottom) {
                direction = "down";
                fraction = (double) (target.bounds.bottom - area.bottom + 20) / area.height();
            }
        }
        fraction = Math.max(0.08, Math.min(0.65, fraction));
        mfiSwipeArea(area, direction, fraction);
        final AnniversaryViewport[] latest = {anniversaryReadViewport()};
        if (issues) latest[0] = mfiScopeIssues(latest[0]);
        if (issues ? mfiIssuesMoved(origin, latest[0]) : !origin.signature.equals(latest[0].signature)) return latest[0];
        // Only a delayed frame or an edge needs polling. Never wait 35/60 seconds per swipe.
        Duration settle = mfiScrollSettleWait();
        try {
            return anniversaryViewportWait(settle).until(d -> {
                latest[0] = anniversaryReadViewport();
                if (issues) latest[0] = mfiScopeIssues(latest[0]);
                boolean moved = issues ? mfiIssuesMoved(origin, latest[0]) : !origin.signature.equals(latest[0].signature);
                return moved ? latest[0] : null;
            });
        } catch (TimeoutException boundaryOrDelayedFrame) {
            // The caller confirms a boundary with another gesture and inspects any pending card first.
            return latest[0];
        }
    }

    private Duration mfiScrollSettleWait() {
        Duration settle = ExecutionProfile.pollingInterval().multipliedBy(4);
        return settle.compareTo(ExecutionProfile.shortWait()) > 0 ? ExecutionProfile.shortWait() : settle;
    }

    private void mfiSwipeArea(AnniversaryBounds area, String direction, double fraction) {
        Assert.assertTrue(area.valid() && area.height() > 40 && area.width() > 40, "No visible area for a W3C swipe");
        int travel = (int) Math.round(area.height() * fraction);
        int x = area.left + area.width() / 2;
        int startY = "down".equals(direction) ? area.bottom - 8 : area.top + 8;
        int endY = "down".equals(direction) ? startY - travel : startY + travel;
        PointerInput finger = new PointerInput(PointerInput.Kind.TOUCH, "mfiArchiveFinger");
        Sequence swipe = new Sequence(finger, 1);
        swipe.addAction(finger.createPointerMove(Duration.ZERO, PointerInput.Origin.viewport(), x, startY));
        swipe.addAction(finger.createPointerDown(PointerInput.MouseButton.LEFT.asArg()));
        swipe.addAction(finger.createPointerMove(Duration.ofMillis(350), PointerInput.Origin.viewport(), x, endY));
        swipe.addAction(new Pause(finger, Duration.ofMillis(80)));
        swipe.addAction(finger.createPointerUp(PointerInput.MouseButton.LEFT.asArg()));
        driver.perform(Collections.singletonList(swipe));
        anniversarySwipes++;
    }

    private AnniversaryViewport mfiRevealIssueControl(AnniversaryViewport viewport, String label, String direction) {
        viewport = mfiScopeIssues(viewport);
        int unchanged = 0;
        for (int gesture = 0; gesture <= 40; gesture++) {
            AnniversaryNode target = viewport.find(AnniversaryCheck.exact(label));
            if (target != null && target.enabled && mfiFullyVisible(viewport, target)) return viewport;
            Assert.assertTrue(gesture < 40 && unchanged < 2, "Issue navigation control is absent or clipped: " + label);
            AnniversaryViewport next = mfiScrollIssues(viewport, target, direction, label);
            unchanged = mfiIssuesMoved(viewport, next) ? 0 : unchanged + 1;
            viewport = next;
        }
        throw new AssertionError("Issue navigation scan exceeded its limit: " + label);
    }

    private AnniversaryViewport mfiScopeIssues(AnniversaryViewport viewport) {
        // Inspector exposes an inner ScrollView for cards, inside an outer ScrollView for the page/header.
        // Select the nearest visible scroll ancestor of the issue dates, never a class-name instance index.
        Map<Element, Integer> candidates = new LinkedHashMap<>();
        for (AnniversaryNode date : viewport.nodes) {
            if (!date.displayed || mfiIssueMonth(date.label) == null || !date.bounds.valid()) continue;
            for (Node parent = date.element.getParentNode(); parent instanceof Element; parent = parent.getParentNode()) {
                Element element = (Element) parent;
                if (!"android.widget.ScrollView".equals(element.getAttribute("class"))) continue;
                AnniversaryNode scroll = new AnniversaryNode(element);
                if (scroll.displayed && scroll.bounds.intersection(viewport.screen).valid())
                    candidates.merge(element, 1, Integer::sum);
                break;
            }
        }
        Element selected = null;
        int mostDates = 0;
        for (Map.Entry<Element, Integer> entry : candidates.entrySet())
            if (entry.getValue() > mostDates) { selected = entry.getKey(); mostDates = entry.getValue(); }
        if (selected == null || selected == viewport.scrollElement && viewport.scopedIssues) return viewport;
        return new AnniversaryViewport(viewport.nodes.get(0).element.getOwnerDocument(),
                new Dimension(viewport.screen.width(), viewport.screen.height()), selected);
    }

    private boolean mfiIssuesMoved(AnniversaryViewport before, AnniversaryViewport after) {
        Map<String, Integer> oldPositions = mfiIssuePositions(before);
        Map<String, Integer> newPositions = mfiIssuePositions(after);
        if (!oldPositions.keySet().equals(newPositions.keySet())) return true;
        // Small physical-device layout jitter and a changing ACTIVE countdown are not scroll progress.
        int tolerance = Math.max(3, before.screen.height() / 300);
        for (String issue : oldPositions.keySet())
            if (Math.abs((long) oldPositions.get(issue) - newPositions.get(issue)) > tolerance) return true;
        return false;
    }

    private Map<String, Integer> mfiIssuePositions(AnniversaryViewport viewport) {
        Map<String, Integer> positions = new LinkedHashMap<>();
        for (AnniversaryNode node : viewport.nodes)
            if (node.inScroll && node.displayed && mfiIssueMonth(node.label) != null
                    && node.bounds.intersects(viewport.content))
                positions.put(node.label, node.bounds.top - viewport.content.top);
        return positions;
    }

    private AnniversaryViewport mfiBackTo(AnniversaryViewport viewport,
            java.util.function.Predicate<AnniversaryViewport> source,
            java.util.function.Predicate<AnniversaryViewport> destination, String stage) {
        Assert.assertTrue(source.test(viewport), "Back must start on the expected screen: " + stage);
        ReportLogger.step("Navigating Back to " + stage);
        driver.navigate().back();
        try { return mfiWaitScreen(ExecutionProfile.shortWait(), destination, stage); }
        catch (TimeoutException firstBackTimeout) {
            AnniversaryViewport current = anniversaryReadViewport();
            if (destination.test(current)) return current;
            if (source.test(current)) {
                ReportLogger.info("Back left the original screen unchanged; retrying Back once for " + stage);
                driver.navigate().back();
            }
            // A loading/unknown destination must not receive an extra Back action.
            return mfiWaitScreen(ExecutionProfile.longWait(), destination, stage);
        }
    }

    private AnniversaryViewport mfiNavigate(AnniversaryViewport viewport, AnniversaryNode node,
            java.util.function.Predicate<AnniversaryViewport> ready, String stage) {
        Assert.assertTrue(node != null && node.enabled && mfiFullyVisible(viewport, node),
                "Navigation control is absent, disabled or offscreen: " + stage);
        mfiClickNode(viewport, node);
        try { return mfiWaitScreen(ExecutionProfile.shortWait(), ready, stage); }
        catch (TimeoutException firstClickTimeout) {
            AnniversaryViewport current = anniversaryReadViewport();
            if (ready.test(current)) return current;
            AnniversaryNode fresh = current.find(AnniversaryCheck.exact(node.label));
            if (fresh != null && fresh.enabled && mfiFullyVisible(current, fresh)) mfiTapBounds(current, fresh);
            return mfiWaitScreen(ExecutionProfile.longWait(), ready, stage);
        }
    }

    private By mfiNodeLocator(AnniversaryNode node) {
        String resource = node.element.getAttribute("resource-id");
        if (!resource.isEmpty()) return By.id(resource);
        String description = node.element.getAttribute("content-desc");
        // Preserve real newline characters (e.g. the account initial + Profile).
        // UiSelector string parsing does not resolve the previously generated W\nProfile selector.
        if (!description.isEmpty()) return AppiumBy.accessibilityId(description);
        String value = node.element.getAttribute("text");
        Assert.assertTrue(!value.isEmpty(), "Cannot locate an unnamed control without its observed bounds");
        String type = node.element.getAttribute("class");
        return AppiumBy.androidUIAutomator("new UiSelector().className(\"" + mfiSelectorLiteral(type) + "\")"
                + ".text(\"" + mfiSelectorLiteral(value) + "\")");
    }

    private String mfiSelectorLiteral(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t");
    }

    private void mfiClickNode(AnniversaryViewport viewport, AnniversaryNode node) {
        Assert.assertTrue(node != null && node.enabled && mfiFullyVisible(viewport, node),
                "Control must be enabled and fully visible before clicking");
        int matches = 0;
        String description = node.element.getAttribute("content-desc");
        if (node.element.getAttribute("resource-id").isEmpty() && !description.isEmpty()) {
            for (AnniversaryNode candidate : viewport.nodes)
                if (description.equals(candidate.element.getAttribute("content-desc"))
                        && mfiFullyVisible(viewport, candidate)) matches++;
        }
        // The issue popup and its selected field can share the same Month Year label.
        if ("false".equals(node.element.getAttribute("clickable")) || matches > 1) {
            mfiTapBounds(viewport, node);
            return;
        }
        try {
            anniversaryClickVisible(mfiNodeLocator(node), node);
        } catch (NoSuchElementException | StaleElementReferenceException lookupFailure) {
            // Navigation controls only: refresh the same semantic control before a bounds tap.
            AnniversaryViewport current = anniversaryReadViewport();
            AnniversaryNode fresh = mfiFreshControl(current, node);
            Assert.assertTrue(fresh != null, "Navigation control disappeared before tapping: " + node.label);
            ReportLogger.info("Native lookup changed; tapping fresh visible bounds for " + fresh.label);
            mfiTapBounds(current, fresh);
        }
    }

    private AnniversaryNode mfiFreshControl(AnniversaryViewport viewport, AnniversaryNode previous) {
        String resource = previous.element.getAttribute("resource-id");
        String type = previous.element.getAttribute("class");
        AnniversaryNode closest = null;
        long bestDistance = Long.MAX_VALUE;
        for (AnniversaryNode candidate : viewport.nodes) {
            boolean sameControl = !resource.isEmpty()
                    ? resource.equals(candidate.element.getAttribute("resource-id"))
                    : previous.label.equals(candidate.label)
                            && type.equals(candidate.element.getAttribute("class"));
            if (!sameControl || !candidate.enabled || !mfiFullyVisible(viewport, candidate)) continue;
            long dx = (long) candidate.bounds.left + candidate.bounds.right - previous.bounds.left - previous.bounds.right;
            long dy = (long) candidate.bounds.top + candidate.bounds.bottom - previous.bounds.top - previous.bounds.bottom;
            long distance = dx * dx + dy * dy;
            if (distance < bestDistance) { closest = candidate; bestDistance = distance; }
        }
        return closest;
    }

    private void mfiTapBounds(AnniversaryViewport viewport, AnniversaryNode node) {
        Assert.assertTrue(node.enabled && mfiFullyVisible(viewport, node), "Cannot tap an offscreen or disabled control");
        int x = node.bounds.left + node.bounds.width() / 2;
        int y = node.bounds.top + node.bounds.height() / 2;
        PointerInput finger = new PointerInput(PointerInput.Kind.TOUCH, "mfiLinkFinger");
        Sequence tap = new Sequence(finger, 1);
        tap.addAction(finger.createPointerMove(Duration.ZERO, PointerInput.Origin.viewport(), x, y));
        tap.addAction(finger.createPointerDown(PointerInput.MouseButton.LEFT.asArg()));
        tap.addAction(new Pause(finger, Duration.ofMillis(80)));
        tap.addAction(finger.createPointerUp(PointerInput.MouseButton.LEFT.asArg()));
        driver.perform(Collections.singletonList(tap));
    }

    public void resetToVroHome() {
        if (!"NATIVE_APP".equals(driver.getContext())) driver.context("NATIVE_APP");
        String appPackage = ConfigReader.get("VROappPackage");
        if (!appPackage.equals(driver.getCurrentPackage())) {
            driver.executeScript("mobile: activateApp", java.util.Map.of("appId", appPackage));
            anniversaryWait().until(d -> appPackage.equals(driver.getCurrentPackage()));
        }

        for (int back = 0; back <= 6; back++) {
            if (anniversaryHomeReady()) return;
            if (anniversaryVisible(homeTab)) {
                anniversaryTap(homeTab);
                for (int scroll = 0; scroll <= 24; scroll++) {
                    if (anniversaryHomeReady()) return;
                    if (scroll == 24) break;
                    boolean more = anniversaryScroll("up");
                    if (!more) {
                        anniversaryWait().until(d -> anniversaryHomeReady());
                        return;
                    }
                }
                Assert.fail("Home tab opened, but VRO Home markers were not visible");
            }
            if (back == 6) break;
            driver.navigate().back();
            try {
                new WebDriverWait(driver, ExecutionProfile.shortWait(),
                        ExecutionProfile.pollingInterval())
                        .until(d -> anniversaryVisible(homeTab));
            } catch (TimeoutException ignored) {
                // Another in-app screen may remain in the back stack.
            }
        }
        Assert.fail("Unable to reset to VRO Home after six Back actions");
    }

    private boolean anniversaryHomeReady() {
        return anniversaryVisible(homeTab)
                && anniversaryVisible(subscribe)
                && anniversaryVisible(todaysStories)
                && anniversaryVisible(allStories);
    }

    private WebDriverWait anniversaryWait() {
        WebDriverWait wait = new WebDriverWait(
                driver,
                ExecutionProfile.longWait(),
                ExecutionProfile.pollingInterval()
        );

        wait.ignoring(StaleElementReferenceException.class);
        return wait;
    }

    private String anniversaryDescription(WebElement element) {
        String description = element.getAttribute("content-desc");
        Assert.assertNotNull(description, "Expected an accessible description");
        return anniversaryClean(description);
    }

    private Dimension anniversaryScreenSize;
    private long anniversarySourceMillis;
    private int anniversarySourceReads;
    private int anniversarySwipes;

    private void anniversaryWithFastPolling(Runnable verification) {
        Map<String, Object> current = driver.getSettings();
        Map<String, Object> previous = new HashMap<>();
        for (String key : List.of("waitForSelectorTimeout", "waitForIdleTimeout")) {
            Object value = current.get(key);
            if (!(value instanceof Number)) throw new IllegalStateException("Appium did not report " + key);
            previous.put(key, value);
        }
        anniversaryScreenSize = null;
        anniversarySourceMillis = 0;
        anniversarySourceReads = 0;
        anniversarySwipes = 0;
        Throwable failure = null;
        try {
            // Apply only inside this module; network/page waits remain explicit.
            long idle = Math.min(100L, ((Number) previous.get("waitForIdleTimeout")).longValue());
            driver.setSettings(Map.of("waitForSelectorTimeout", 0, "waitForIdleTimeout", idle));
            ReportLogger.info("Subscription waits: selector=0 ms, idle=" + idle
                    + " ms; W3C swipes with explicit viewport checks");
            verification.run();
        } catch (RuntimeException | Error exception) {
            failure = exception;
            throw exception;
        } finally {
            anniversaryScreenSize = null;
            try {
                driver.setSettings(previous);
            } catch (RuntimeException | Error restoreException) {
                if (failure != null) failure.addSuppressed(restoreException);
                else throw restoreException;
            } finally {
                ReportLogger.info("Subscription UI timings: " + anniversarySourceReads
                        + " viewport reads, " + anniversarySourceMillis + " ms fetching UI, "
                        + anniversarySwipes + " W3C swipes");
            }
        }
    }

    private WebDriverWait anniversaryViewportWait(Duration timeout) {
        // Each poll is one device call; do not busy-poll wireless ADB.
        WebDriverWait wait = new WebDriverWait(driver, timeout,
                Duration.ofMillis(Math.max(500L, ExecutionProfile.pollingInterval().toMillis())));
        wait.ignoring(StaleElementReferenceException.class);
        return wait;
    }

    private AnniversaryViewport anniversaryReadViewport() {
        if (anniversaryScreenSize == null) anniversaryScreenSize = driver.manage().window().getSize();
        long started = System.nanoTime();
        String source;
        try {
            source = driver.getPageSource();
        } finally {
            anniversarySourceReads++;
            anniversarySourceMillis += (System.nanoTime() - started) / 1_000_000;
        }
        return anniversaryParseViewport(source, anniversaryScreenSize);
    }

    private AnniversaryViewport anniversaryParseViewport(String source, Dimension size) {
        try {
            // All visible checks share one UI read; no remote calls per XML node.
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
            factory.setXIncludeAware(false);
            factory.setExpandEntityReferences(false);
            Document document = factory.newDocumentBuilder().parse(new InputSource(new StringReader(source)));
            return new AnniversaryViewport(document, size);
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to parse the current subscription viewport", exception);
        }
    }

    private AnniversaryViewport anniversaryWaitForSelectedPlan(AnniversaryViewport viewport,
                                                              AnniversaryCheck plan) {
        AnniversaryNode node = viewport.find(plan);
        if (node != null && node.visibleWithin(viewport.content) && plan.rateMatches(node)) return viewport;
        return anniversaryViewportWait(ExecutionProfile.longWait()).until(d -> {
            AnniversaryViewport current = anniversaryReadViewport();
            AnniversaryNode currentPlan = current.find(plan);
            return currentPlan != null && currentPlan.visibleWithin(current.content)
                    && plan.rateMatches(currentPlan) ? current : null;
        });
    }

    private AnniversaryViewport anniversaryBringIntoView(AnniversaryViewport viewport,
            AnniversaryCheck target, String direction, boolean alignHeader) {
        long deadline = System.nanoTime() + ExecutionProfile.longWait().toNanos();
        for (int swipe = 0; swipe <= 24 && System.nanoTime() <= deadline; swipe++) {
            AnniversaryNode node = viewport.find(target);
            if (node != null && node.inScroll && node.visibleWithin(viewport.content)
                    && (!alignHeader || node.bounds.top <= viewport.content.top + viewport.content.height() / 5))
                return viewport;
            if (swipe == 24) break;
            viewport = anniversaryAdvance(viewport, node, direction, alignHeader, target.label);
        }
        throw new AssertionError("Could not scroll fully into view: " + target.label);
    }

    private AnniversaryViewport anniversaryScanVisible(AnniversaryViewport viewport,
            List<AnniversaryCheck> checks, String stage) {
        return anniversaryScanVisible(viewport, checks, stage, false);
    }

    private AnniversaryViewport anniversaryScanVisible(AnniversaryViewport viewport,
            List<AnniversaryCheck> checks, String stage, boolean successScreen) {
        List<AnniversaryCheck> pending = new ArrayList<>(checks);
        long started = System.nanoTime();
        // One bounded pass for the whole page; no separate long wait per label.
        // Allow a progressing scan to finish even over a slower ADB connection.
        for (int swipe = 0; swipe <= 40; swipe++) {
            for (java.util.Iterator<AnniversaryCheck> iterator = pending.iterator(); iterator.hasNext();) {
                AnniversaryCheck check = iterator.next();
                AnniversaryNode node = viewport.find(check);
                boolean visible = node != null && (node.inScroll || successScreen)
                        && node.visibleWithin(node.inScroll ? viewport.content : viewport.screen);
                if (visible) {
                    check.verify(node);
                    ReportLogger.pass("Verified " + stage + ": " + node.label);
                    iterator.remove();
                }
            }
            if (pending.isEmpty()) {
                ReportLogger.info(stage + " verified in " + (System.nanoTime() - started) / 1_000_000 + " ms");
                return viewport;
            }
            if (swipe == 40) break;
            AnniversaryNode partial = null;
            for (AnniversaryCheck check : pending) {
                AnniversaryNode candidate = viewport.find(check);
                if (candidate != null && candidate.inScroll && candidate.bounds.intersects(viewport.content)) {
                    partial = candidate;
                    break;
                }
            }
            viewport = anniversaryAdvance(viewport, partial, "down", false, anniversaryMissing(pending));
        }
        throw new AssertionError(stage + " scan reached its swipe limit; missing: " + anniversaryMissing(pending));
    }

    private AnniversaryViewport anniversaryAdvance(AnniversaryViewport before, AnniversaryNode target,
            String preferredDirection, boolean alignHeader, String missing) {
        AnniversaryBounds area = before.content;
        Assert.assertTrue(before.scrollElement != null && area.height() > 40 && area.width() > 40,
                "No visible subscription scroll area; missing: " + missing);
        String direction = preferredDirection;
        double fraction = 0.45; // Overlapping viewports prevent skipping short headings.
        if (target != null && target.inScroll && target.bounds.valid()) {
            Assert.assertTrue(target.bounds.height() <= area.height(),
                    "Element exceeds the visible scroll area; use a child locator: " + target.label);
            if (target.bounds.top < area.top) {
                direction = "up";
                fraction = (double) (area.top - target.bounds.top + 20) / area.height();
            } else if (alignHeader && target.bounds.top > area.top + area.height() / 5) {
                direction = "down";
                fraction = (double) (target.bounds.top - area.top - area.height() / 5) / area.height();
            } else if (target.bounds.bottom > area.bottom) {
                direction = "down";
                fraction = (double) (target.bounds.bottom - area.bottom + 20) / area.height();
            }
        }
        fraction = Math.max(0.08, Math.min(0.65, fraction));
        int travel = (int) Math.round(area.height() * fraction);
        int x = area.left + area.width() / 2;
        int startY = "down".equals(direction) ? area.bottom - 8 : area.top + 8;
        int endY = "down".equals(direction) ? startY - travel : startY + travel;
        PointerInput finger = new PointerInput(PointerInput.Kind.TOUCH, "subscriptionFinger");
        Sequence swipe = new Sequence(finger, 1);
        swipe.addAction(finger.createPointerMove(Duration.ZERO, PointerInput.Origin.viewport(), x, startY));
        swipe.addAction(finger.createPointerDown(PointerInput.MouseButton.LEFT.asArg()));
        swipe.addAction(finger.createPointerMove(Duration.ofMillis(350), PointerInput.Origin.viewport(), x, endY));
        // A brief touch hold avoids a fling that would skip content after release.
        swipe.addAction(new Pause(finger, Duration.ofMillis(80)));
        swipe.addAction(finger.createPointerUp(PointerInput.MouseButton.LEFT.asArg()));
        driver.perform(Collections.singletonList(swipe));
        anniversarySwipes++;
        AnniversaryViewport next = anniversaryReadViewport();
        if (!before.signature.equals(next.signature)) return next;
        // Poll only when the initial post-gesture frame has not moved yet.
        try {
            return anniversaryViewportWait(ExecutionProfile.shortWait()).until(d -> {
                AnniversaryViewport settled = anniversaryReadViewport();
                return before.signature.equals(settled.signature) ? null : settled;
            });
        } catch (TimeoutException exception) {
            throw new AssertionError("Subscription scroll made no progress; missing: " + missing, exception);
        }
    }

    private void anniversaryClickVisible(By locator, AnniversaryNode node) {
        Assert.assertTrue(node.enabled, "Control is disabled: " + node.label);
        try {
            driver.findElement(locator).click();
        } catch (StaleElementReferenceException exception) {
            AnniversaryViewport current = anniversaryReadViewport();
            AnniversaryNode fresh = !node.inScroll && "Subscribe Now".equals(node.label)
                    ? current.stickySubscribe() : current.find(AnniversaryCheck.exact(node.label));
            Assert.assertTrue(fresh != null && fresh.enabled
                            && fresh.visibleWithin(node.inScroll ? current.content : current.screen),
                    "Control moved or disappeared before clicking: " + node.label);
            driver.findElement(locator).click();
        }
    }

    private String anniversaryMissing(List<AnniversaryCheck> checks) {
        List<String> labels = new ArrayList<>();
        for (AnniversaryCheck check : checks) labels.add(check.label);
        return String.join(" | ", labels);
    }

    private static String anniversaryClean(String value) {
        return value.replace("\\n", " ").replaceAll("\\s+", " ").trim();
    }

    private static class AnniversaryCheck {
        private final String label;
        private final boolean prefix;
        private final String[] required;
        private AnniversaryCheck(String label, boolean prefix, String... required) {
            this.label = label; this.prefix = prefix; this.required = required;
        }
        private static AnniversaryCheck exact(String label) { return new AnniversaryCheck(label, false); }
        private static AnniversaryCheck prefix(String label) { return new AnniversaryCheck(label, true); }
        private static AnniversaryCheck plan(String period, String rate, String savings, String billing) {
            return new AnniversaryCheck(period, true, rate + " / month", savings, billing);
        }
        private boolean matches(AnniversaryNode node) {
            return (prefix ? node.label.startsWith(label) : node.label.equals(label))
                    && (required.length != 3 || node.label.contains("Billed at"));
        }
        private boolean rateMatches(AnniversaryNode node) { return node.label.contains(required[0]); }
        private void verify(AnniversaryNode node) {
            for (String expected : required)
                Assert.assertTrue(node.label.contains(expected), "Incorrect/missing " + expected + ": " + node.label);
        }
    }

    private static class AnniversaryNode {
        private final Element element;
        private final String label;
        private final AnniversaryBounds bounds;
        private final boolean displayed;
        private final boolean enabled;
        private boolean inScroll;
        private AnniversaryNode(Element element) {
            this.element = element;
            String description = element.getAttribute("content-desc");
            label = anniversaryClean(description.isEmpty() ? element.getAttribute("text") : description);
            bounds = AnniversaryBounds.parse(element.getAttribute("bounds"));
            boolean visible = true;
            for (Node ancestor = element; ancestor instanceof Element; ancestor = ancestor.getParentNode()) {
                if ("false".equals(((Element) ancestor).getAttribute("displayed"))) {
                    visible = false;
                    break;
                }
            }
            displayed = visible;
            enabled = "true".equals(element.getAttribute("enabled"));
        }
        private boolean visibleWithin(AnniversaryBounds area) {
            return displayed && bounds.valid() && area.contains(bounds);
        }
    }

    private static class AnniversaryViewport {
        private final List<AnniversaryNode> nodes = new ArrayList<>();
        private final AnniversaryBounds screen;
        private final AnniversaryBounds content;
        private final Element scrollElement;
        private final String signature;
        private final boolean scopedIssues;
        private AnniversaryViewport(Document document, Dimension size) {
            this(document, size, null);
        }
        private AnniversaryViewport(Document document, Dimension size, Element preferredScroller) {
            scopedIssues = preferredScroller != null;
            screen = new AnniversaryBounds(0, 0, size.getWidth(), size.getHeight());
            Element scroller = null;
            AnniversaryBounds scrollBounds = new AnniversaryBounds(0, 0, 0, 0);
            NodeList elements = document.getElementsByTagName("*");
            for (int i = 0; i < elements.getLength(); i++) {
                Element element = (Element) elements.item(i);
                AnniversaryNode node = new AnniversaryNode(element);
                nodes.add(node);
                String type = element.getAttribute("class");
                if (type.isEmpty()) type = element.getTagName();
                AnniversaryBounds clipped = node.bounds.intersection(screen);
                if ("android.widget.ScrollView".equals(type) && node.displayed && clipped.valid()
                        && (long) clipped.width() * clipped.height() > (long) scrollBounds.width() * scrollBounds.height()) {
                    scroller = element;
                    scrollBounds = clipped;
                }
            }
            if (preferredScroller != null) {
                scroller = preferredScroller;
                scrollBounds = AnniversaryBounds.parse(scroller.getAttribute("bounds")).intersection(screen);
                for (Node parent = scroller.getParentNode(); parent instanceof Element; parent = parent.getParentNode()) {
                    Element ancestor = (Element) parent;
                    if ("android.widget.ScrollView".equals(ancestor.getAttribute("class")))
                        scrollBounds = scrollBounds.intersection(AnniversaryBounds.parse(ancestor.getAttribute("bounds")));
                }
            }
            scrollElement = scroller;
            for (AnniversaryNode node : nodes) {
                for (Node parent = node.element.getParentNode(); parent != null; parent = parent.getParentNode()) {
                    if (parent == scroller) { node.inScroll = true; break; }
                }
            }
            int top = scrollBounds.top, bottom = scrollBounds.bottom;
            if (preferredScroller != null) {
                for (AnniversaryNode node : nodes) {
                    if (node.inScroll || !node.visibleWithin(screen)) continue;
                    if (node.label.equals("2025") || node.label.equals("2024")) top = Math.max(top, node.bounds.bottom);
                    if (node.label.equals("Home")) bottom = Math.min(bottom, node.bounds.top);
                }
            }
            for (AnniversaryNode node : nodes)
                if (!node.inScroll && (node.label.equals("Invest in Clarity")
                        || node.label.equals("Pay Securely Now")) && node.visibleWithin(screen))
                    bottom = Math.min(bottom, node.bounds.top);
            int padding = Math.max(4, size.getHeight() / 200);
            content = new AnniversaryBounds(scrollBounds.left + padding, top + padding,
                    scrollBounds.right - padding, bottom - padding);
            StringBuilder layout = new StringBuilder(content.toString());
            for (AnniversaryNode node : nodes)
                if (node.inScroll && node.displayed && !node.label.isEmpty() && node.bounds.intersects(content))
                    layout.append('|').append(node.label).append('@').append(node.bounds);
            signature = layout.toString();
        }
        private AnniversaryNode find(AnniversaryCheck check) {
            AnniversaryNode partial = null;
            for (AnniversaryNode node : nodes) {
                if (!check.matches(node) || !node.displayed || !node.bounds.valid()) continue;
                if (node.visibleWithin(content)) return node;
                if (partial == null) partial = node;
            }
            return partial;
        }
        private AnniversaryNode stickySubscribe() {
            for (AnniversaryNode invest : nodes) {
                if (invest.inScroll || !invest.label.equals("Invest in Clarity") || !invest.visibleWithin(screen)) continue;
                for (AnniversaryNode button : nodes)
                    if (button.label.equals("Subscribe Now") && button.enabled && button.visibleWithin(screen)
                            && button.element.getParentNode() == invest.element.getParentNode()) return button;
            }
            return null;
        }
    }

    private static class AnniversaryBounds {
        private static final Pattern FORMAT = Pattern.compile("\\[(-?\\d+),(-?\\d+)\\]\\[(-?\\d+),(-?\\d+)\\]");
        private final int left, top, right, bottom;
        private AnniversaryBounds(int left, int top, int right, int bottom) {
            this.left = left; this.top = top; this.right = right; this.bottom = bottom;
        }
        private static AnniversaryBounds parse(String value) {
            Matcher match = FORMAT.matcher(value);
            if (!match.matches()) return new AnniversaryBounds(0, 0, 0, 0);
            return new AnniversaryBounds(Integer.parseInt(match.group(1)), Integer.parseInt(match.group(2)),
                    Integer.parseInt(match.group(3)), Integer.parseInt(match.group(4)));
        }
        private int width() { return Math.max(0, right - left); }
        private int height() { return Math.max(0, bottom - top); }
        private boolean valid() { return width() > 0 && height() > 0; }
        private boolean contains(AnniversaryBounds other) {
            return valid() && other.left >= left && other.top >= top && other.right <= right && other.bottom <= bottom;
        }
        private boolean intersects(AnniversaryBounds other) {
            return valid() && other.valid() && left < other.right && right > other.left && top < other.bottom && bottom > other.top;
        }
        private AnniversaryBounds intersection(AnniversaryBounds other) {
            return new AnniversaryBounds(Math.max(left, other.left), Math.max(top, other.top),
                    Math.min(right, other.right), Math.min(bottom, other.bottom));
        }
        public String toString() { return "[" + left + "," + top + "][" + right + "," + bottom + "]"; }
    }

    private WebElement anniversaryMagazineButton() {
        for (int scroll = 0; scroll <= 8; scroll++) {
            WebElement card = driver.findElement(mfiCard);
            for (WebElement button : card.findElements(
                    By.xpath(".//*[@content-desc='Subscribe Now']"))) {
                if (anniversaryOnScreen(button)) return button;
            }
            if (scroll == 8 || !anniversaryScroll("down")) {
                // A final gesture can expose the button while returning false.
                card = driver.findElement(mfiCard);
                for (WebElement button : card.findElements(
                        By.xpath(".//*[@content-desc='Subscribe Now']"))) {
                    if (anniversaryOnScreen(button)) return button;
                }
                break;
            }
        }
        throw new AssertionError("Subscribe Now was not visible inside the Mutual Fund Insight card");
    }

    private void anniversaryTap(By locator) {
        anniversaryWait().until(d -> {
            for (WebElement element : driver.findElements(locator)) {
                if (anniversaryOnScreen(element) && element.isEnabled()) {
                    element.click();
                    return true;
                }
            }
            return false;
        });
    }

    private boolean anniversaryVisible(By locator) {
        try {
            for (WebElement element : driver.findElements(locator))
                if (anniversaryOnScreen(element)) return true;
        } catch (StaleElementReferenceException ignored) { }
        return false;
    }

    private boolean anniversaryOnScreen(WebElement element) {
        org.openqa.selenium.Rectangle r = element.getRect();
        org.openqa.selenium.Dimension size = driver.manage().window().getSize();
        return element.isDisplayed() && r.getWidth() > 0 && r.getHeight() > 0
                && r.getX() >= 0 && r.getX() + r.getWidth() <= size.getWidth()
                && r.getY() >= 0 && r.getY() + r.getHeight() <= size.getHeight();
    }

    private WebElement anniversaryScrollView() {
        return anniversaryWait().until(d -> anniversaryFindScrollView());
    }

    private WebElement anniversaryFindScrollView() {
        WebElement largest = null;
        long largestArea = 0;
        for (WebElement element : driver.findElements(scrollView)) {
            Rectangle bounds = element.getRect();
            long area = (long) bounds.getWidth() * bounds.getHeight();
            if (element.isDisplayed() && area > largestArea) {
                largest = element;
                largestArea = area;
            }
        }
        return largest;
    }

    private boolean anniversaryScroll(String direction) {
        return anniversaryScroll(anniversaryScrollView(), direction);
    }

    private boolean anniversaryScroll(WebElement scrollView, String direction) {
        // The gesture command returns before the caller probes its target again.
        // Scanning every labelled descendant here delays each scroll/poll.
        Object more = driver.executeScript("mobile: scrollGesture", java.util.Map.of(
                "elementId", ((RemoteWebElement) scrollView).getId(),
                "direction", direction, "percent", 0.55));
        return Boolean.TRUE.equals(more);
    }
}
