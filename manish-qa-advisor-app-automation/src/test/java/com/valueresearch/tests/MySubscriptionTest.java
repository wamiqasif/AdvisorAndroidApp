package com.valueresearch.tests;

import com.aventstack.extentreports.ExtentTest;
import com.valueresearch.base.BaseTest;
import com.valueresearch.pages.MySubscriptionPage;
import com.valueresearch.pages.MySubscriptionPage.PageModel;
import com.valueresearch.utils.AdvisorAccountSession;
import com.valueresearch.utils.AuthHelper;
import com.valueresearch.utils.ExtentManager;
import com.valueresearch.utils.ExtentTestManager;
import com.valueresearch.utils.ReportLogger;
import com.valueresearch.utils.ScreenshotUtils;
import io.appium.java_client.android.AndroidDriver;
import org.testng.ITestResult;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import java.lang.reflect.Method;
import java.util.Locale;

/**
 * My Subscriptions (Hub) - PRD "Subscription details and Invoice" (ticket 17157).
 *
 * <p>Accounts (switched once each by AdvisorAccountSession, grouped by TestNG group):
 * both-active = testEmail (VRFA + VRSA active), vrsa-active = ActiveSubscriberEmail (VRFA free),
 * vrfa-active = ActiveSubscriberVRFAEmail (VRSA free), vrfa-lapsed = LapsedUserVRFAEmail (VRSA free),
 * vrsa-lapsed = LapsedUserEmail (VRFA free), renew-window = RenewWindowUserEmail (autopay OFF, expiry within 30 days),
 * vrfa-rebuy-vrsa-lapsed = VrfaRebuyVrsaLapsedEmail, vrsa-active-vrfa-lapsed = VrsaActiveVrfaLapsedEmail,
 * both-lapsed = BothLapsedEmail. The free (non-subscriber) account is intentionally not covered:
 * the app does not open for it yet.
 *
 * <p>Each case collects every PRD mismatch as a [BUG-CANDIDATE] line and raises them together at the end.
 * Cases that currently fail because the app deviates from the PRD are expected to fail until fixed.
 */
public class MySubscriptionTest extends BaseTest {

    private MySubscriptionPage page;
    private AdvisorAccountSession accountSession;
    private boolean accountPrepared;
    private String currentCaseId;
    private ExtentTest currentReport;

    @BeforeClass(alwaysRun = true)
    public void resetAccountSession() {
        accountSession = null;
    }

    @BeforeMethod(alwaysRun = true)
    @Parameters("loginMethod")
    public void prepareMySubscription(Method method, @Optional("PASSWORD") String loginMethod) {
        page = null;
        accountPrepared = false;
        currentCaseId = method.getName().substring(0, 6);
        Test annotation = method.getAnnotation(Test.class);
        startExtentCase(currentCaseId, annotation.description());
        try {
            AuthHelper.LoginMethod selectedMethod;
            try {
                selectedMethod = AuthHelper.LoginMethod.valueOf(loginMethod.trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException invalid) {
                throw new IllegalArgumentException("Suite loginMethod must be OTP or PASSWORD");
            }
            if (accountSession == null || !accountSession.isForDriver(driver)) {
                accountSession = createAccountSession(driver);
            }
            AuthHelper.Account account = requiredAccountFor(method);
            try {
                accountSession.prepare(account, selectedMethod);
            } catch (IllegalStateException lateSecurityPrompt) {
                // After an app relaunch the PIN/biometric screen can appear a moment late; one more prepare() completes it.
                if (!String.valueOf(lateSecurityPrompt.getMessage()).contains("authentication screen")) {
                    throw lateSecurityPrompt;
                }
                ReportLogger.info("Security screen appeared late after app relaunch; preparing the account once more");
                accountSession.invalidate();
                accountSession.prepare(account, selectedMethod);
            }
            accountSession.assertHubVisible();
            page = new MySubscriptionPage(driver, accountSession);
            page.beginCase(currentCaseId, accountLabel(method));
            accountPrepared = true;
        } catch (RuntimeException | AssertionError | LinkageError failure) {
            if (accountSession != null) accountSession.invalidate();
            ReportLogger.fail("App/account preparation failed: " + failure.getClass().getSimpleName()
                    + ". See the TestNG configuration failure trace.");
            attachFailureScreenshot("setup");
            if (accountSession != null) {
                try {
                    accountSession.restoreTimingSettings();
                } catch (RuntimeException | AssertionError restoreFailure) {
                    failure.addSuppressed(restoreFailure);
                }
            }
            throw failure;
        }
    }

    protected AdvisorAccountSession createAccountSession(AndroidDriver currentDriver) {
        return new AdvisorAccountSession(currentDriver);
    }

    private AuthHelper.Account requiredAccountFor(Method method) {
        for (String group : method.getAnnotation(Test.class).groups()) {
            switch (group) {
                case "both-active": return AuthHelper.Account.DEFAULT;
                case "vrsa-active": return AuthHelper.Account.ACTIVE_SUBSCRIBER;
                case "vrfa-active": return AuthHelper.Account.VRFA_ACTIVE_SUBSCRIBER;
                case "vrfa-lapsed": return AuthHelper.Account.VRFA_LAPSED_USER;
                case "vrsa-lapsed": return AuthHelper.Account.LAPSED_USER;
                case "renew-window": return AuthHelper.Account.RENEW_WINDOW_USER;
                case "vrfa-rebuy-vrsa-lapsed": return AuthHelper.Account.VRFA_REBUY_VRSA_LAPSED;
                case "vrsa-active-vrfa-lapsed": return AuthHelper.Account.VRSA_ACTIVE_VRFA_LAPSED;
                case "both-lapsed": return AuthHelper.Account.BOTH_LAPSED;
                default: break;
            }
        }
        throw new IllegalStateException("Test " + method.getName() + " has no account group (both-active / vrsa-active / "
                + "vrfa-active / vrfa-lapsed / vrsa-lapsed / renew-window / vrfa-rebuy-vrsa-lapsed / vrsa-active-vrfa-lapsed / both-lapsed)");
    }

    private String accountLabel(Method method) {
        for (String group : method.getAnnotation(Test.class).groups()) {
            switch (group) {
                case "both-active": return "VRFA active + VRSA active (testEmail)";
                case "vrsa-active": return "VRSA active + VRFA free (ActiveSubscriberEmail)";
                case "vrfa-active": return "VRFA active + VRSA free (ActiveSubscriberVRFAEmail)";
                case "vrfa-lapsed": return "VRFA lapsed + VRSA free (LapsedUserVRFAEmail)";
                case "vrsa-lapsed": return "VRSA lapsed + VRFA free (LapsedUserEmail)";
                case "renew-window": return "Active, autopay OFF, expiry within 30 days (RenewWindowUserEmail)";
                case "vrfa-rebuy-vrsa-lapsed": return "VRFA active after a break + VRSA lapsed (VrfaRebuyVrsaLapsedEmail)";
                case "vrsa-active-vrfa-lapsed": return "VRSA active + VRFA lapsed (VrsaActiveVrfaLapsedEmail)";
                case "both-lapsed": return "VRFA lapsed + VRSA lapsed (BothLapsedEmail)";
                default: break;
            }
        }
        return "unknown";
    }

    // =====================================================================================================
    // ACCOUNT 1: VRFA active + VRSA active
    // =====================================================================================================

    @Test(priority = 1, groups = "both-active",
            description = "Both active: My Subscriptions opens from Hub, both Active cards are stacked, latest subscription on top")
    public void MS_001_BothActive_PageOpensAndCardOrder() {
        page.openFromHub();
        PageModel m = page.readPage();
        page.verifyBothActiveOrder(m);
        finish();
    }

    @Test(priority = 2, groups = "both-active",
            description = "Both active: Fund Advisor active card fields, renewal label and status rule")
    public void MS_002_BothActive_FundAdvisorActiveCard() {
        page.openFromHub();
        page.verifyActiveCard(page.readPage(), MySubscriptionPage.FUND);
        finish();
    }

    @Test(priority = 3, groups = "both-active",
            description = "Both active: Stock Advisor active card fields, renewal label and status rule")
    public void MS_003_BothActive_StockAdvisorActiveCard() {
        page.openFromHub();
        page.verifyActiveCard(page.readPage(), MySubscriptionPage.STOCK);
        finish();
    }

    @Test(priority = 4, groups = "both-active",
            description = "Both active: Fund Advisor Invoice CTA opens the invoice list")
    public void MS_004_BothActive_FundAdvisorInvoiceList() {
        page.openFromHub();
        if (page.openInvoiceFromCard(MySubscriptionPage.FUND)) {
            page.verifyInvoicePage("Fund Advisor invoices");
            page.backFromInvoice();
        }
        finish();
    }

    @Test(priority = 5, groups = "both-active",
            description = "Both active: Stock Advisor Invoice CTA opens the invoice list")
    public void MS_005_BothActive_StockAdvisorInvoiceList() {
        page.openFromHub();
        if (page.openInvoiceFromCard(MySubscriptionPage.STOCK)) {
            page.verifyInvoicePage("Stock Advisor invoices");
            page.backFromInvoice();
        }
        finish();
    }

    @Test(priority = 6, groups = "both-active",
            description = "Both active: Past section heading, tabs only when both products have past tenures, every past card, no overlap with the active tenure")
    public void MS_006_BothActive_PastSectionStructure() {
        page.openFromHub();
        page.verifyPastSection(page.readPage(), false);
        finish();
    }

    // =====================================================================================================
    // ACCOUNT 2: VRSA active + VRFA free
    // =====================================================================================================

    @Test(priority = 11, groups = "vrsa-active",
            description = "VRSA only: Stock Advisor Active card is first, Fund Advisor Subscribe Now card is last")
    public void MS_011_VrsaOnly_CardOrder() {
        page.openFromHub();
        page.verifyCardOrder(page.readPage(), "Stock Advisor|ACTIVE", "Fund Advisor|FREE");
        finish();
    }

    @Test(priority = 12, groups = "vrsa-active",
            description = "VRSA only: Stock Advisor active card fields, renewal label and status rule")
    public void MS_012_VrsaOnly_StockAdvisorActiveCard() {
        page.openFromHub();
        page.verifyActiveCard(page.readPage(), MySubscriptionPage.STOCK);
        finish();
    }

    @Test(priority = 13, groups = "vrsa-active",
            description = "VRSA only: Fund Advisor free card shows benefits and Subscribe Now")
    public void MS_013_VrsaOnly_FundAdvisorFreeCard() {
        page.openFromHub();
        page.verifyFreeCard(page.readPage(), MySubscriptionPage.FUND);
        finish();
    }

    @Test(priority = 14, groups = "vrsa-active",
            description = "VRSA only: Stock Advisor Invoice CTA opens the invoice list")
    public void MS_014_VrsaOnly_StockAdvisorInvoiceList() {
        page.openFromHub();
        if (page.openInvoiceFromCard(MySubscriptionPage.STOCK)) {
            page.verifyInvoicePage("Stock Advisor invoices");
            page.backFromInvoice();
        }
        finish();
    }

    @Test(priority = 15, groups = "vrsa-active",
            description = "VRSA only: Past section heading, tabs only when both products have past tenures, every past card")
    public void MS_015_VrsaOnly_PastSectionAndTabsRule() {
        page.openFromHub();
        page.verifyPastSection(page.readPage(), false);
        finish();
    }

    @Test(priority = 16, groups = "vrsa-active",
            description = "VRSA only: Subscribe Now on the Fund Advisor card opens the Fund Advisor subscription page")
    public void MS_016_VrsaOnly_FundAdvisorSubscribeNowNavigation() {
        page.openFromHub();
        page.verifyCtaOpensSubscriptionPage(MySubscriptionPage.FUND);
        finish();
    }

    // =====================================================================================================
    // ACCOUNT 3: VRFA active + VRSA free
    // =====================================================================================================

    @Test(priority = 21, groups = "vrfa-active",
            description = "VRFA only: Fund Advisor Active card is first, Stock Advisor Subscribe Now card is last")
    public void MS_021_VrfaOnly_CardOrder() {
        page.openFromHub();
        page.verifyCardOrder(page.readPage(), "Fund Advisor|ACTIVE", "Stock Advisor|FREE");
        finish();
    }

    @Test(priority = 22, groups = "vrfa-active",
            description = "VRFA only: Fund Advisor active card fields, renewal label and status rule")
    public void MS_022_VrfaOnly_FundAdvisorActiveCard() {
        page.openFromHub();
        page.verifyActiveCard(page.readPage(), MySubscriptionPage.FUND);
        finish();
    }

    @Test(priority = 23, groups = "vrfa-active",
            description = "VRFA only: Stock Advisor free card shows benefits and Subscribe Now")
    public void MS_023_VrfaOnly_StockAdvisorFreeCard() {
        page.openFromHub();
        page.verifyFreeCard(page.readPage(), MySubscriptionPage.STOCK);
        finish();
    }

    @Test(priority = 24, groups = "vrfa-active",
            description = "VRFA only: Fund Advisor Invoice CTA opens the invoice list")
    public void MS_024_VrfaOnly_FundAdvisorInvoiceList() {
        page.openFromHub();
        if (page.openInvoiceFromCard(MySubscriptionPage.FUND)) {
            page.verifyInvoicePage("Fund Advisor invoices");
            page.backFromInvoice();
        }
        finish();
    }

    @Test(priority = 25, groups = "vrfa-active",
            description = "VRFA only: Past section heading, tabs only when both products have past tenures, every past card")
    public void MS_025_VrfaOnly_PastSectionAndTabsRule() {
        page.openFromHub();
        page.verifyPastSection(page.readPage(), false);
        finish();
    }

    @Test(priority = 26, groups = "vrfa-active",
            description = "VRFA only: Subscribe Now on the Stock Advisor card opens the Stock Advisor subscription page")
    public void MS_026_VrfaOnly_StockAdvisorSubscribeNowNavigation() {
        page.openFromHub();
        page.verifyCtaOpensSubscriptionPage(MySubscriptionPage.STOCK);
        finish();
    }

    // =====================================================================================================
    // ACCOUNT 4: VRFA lapsed + VRSA free
    // =====================================================================================================

    @Test(priority = 31, groups = "vrfa-lapsed",
            description = "VRFA lapsed: Fund Advisor Expired card is first, Stock Advisor Subscribe Now card is last")
    public void MS_031_VrfaLapsed_CardOrder() {
        page.openFromHub();
        page.verifyCardOrder(page.readPage(), "Fund Advisor|EXPIRED", "Stock Advisor|FREE");
        finish();
    }

    @Test(priority = 32, groups = "vrfa-lapsed",
            description = "VRFA lapsed: Fund Advisor expired card (message, Renew now) and Stock Advisor free card content")
    public void MS_032_VrfaLapsed_ExpiredAndFreeCardContent() {
        page.openFromHub();
        PageModel m = page.readPage();
        page.verifyExpiredCard(m, MySubscriptionPage.FUND);
        page.verifyFreeCard(m, MySubscriptionPage.STOCK);
        finish();
    }

    @Test(priority = 33, groups = "vrfa-lapsed",
            description = "VRFA lapsed: Past Subscriptions list with the expired tenure, no Fund/Stock tabs (VRSA never bought)")
    public void MS_033_VrfaLapsed_PastSubscriptions() {
        page.openFromHub();
        page.verifyPastSection(page.readPage(), true, MySubscriptionPage.FUND);
        finish();
    }

    @Test(priority = 34, groups = "vrfa-lapsed",
            description = "VRFA lapsed: Invoice CTA on the past subscription card opens the invoice list")
    public void MS_034_VrfaLapsed_PastCardInvoiceList() {
        page.openFromHub();
        if (page.openInvoiceFromPastCard(MySubscriptionPage.FUND)) {
            page.verifyInvoicePage("Past Fund Advisor invoices");
            page.backFromInvoice();
        }
        finish();
    }

    @Test(priority = 35, groups = "vrfa-lapsed",
            description = "VRFA lapsed: the Fund Advisor CTA opens the Fund Advisor renewal / subscription page")
    public void MS_035_VrfaLapsed_FundAdvisorRenewNavigation() {
        page.openFromHub();
        page.verifyCtaOpensSubscriptionPage(MySubscriptionPage.FUND);
        finish();
    }

    // =====================================================================================================
    // ACCOUNT 5: VRSA lapsed + VRFA free
    // =====================================================================================================

    @Test(priority = 41, groups = "vrsa-lapsed",
            description = "VRSA lapsed: Stock Advisor Expired card is first, Fund Advisor Subscribe Now card is last")
    public void MS_041_VrsaLapsed_CardOrder() {
        page.openFromHub();
        page.verifyCardOrder(page.readPage(), "Stock Advisor|EXPIRED", "Fund Advisor|FREE");
        finish();
    }

    @Test(priority = 42, groups = "vrsa-lapsed",
            description = "VRSA lapsed: Stock Advisor expired card (message, Renew now) and Fund Advisor free card content")
    public void MS_042_VrsaLapsed_ExpiredAndFreeCardContent() {
        page.openFromHub();
        PageModel m = page.readPage();
        page.verifyExpiredCard(m, MySubscriptionPage.STOCK);
        page.verifyFreeCard(m, MySubscriptionPage.FUND);
        finish();
    }

    @Test(priority = 43, groups = "vrsa-lapsed",
            description = "VRSA lapsed: Past Subscriptions list with the expired tenure, no Fund/Stock tabs (VRFA never bought)")
    public void MS_043_VrsaLapsed_PastSubscriptions() {
        page.openFromHub();
        page.verifyPastSection(page.readPage(), true, MySubscriptionPage.STOCK);
        finish();
    }

    @Test(priority = 44, groups = "vrsa-lapsed",
            description = "VRSA lapsed: Invoice CTA on the past subscription card opens the invoice list")
    public void MS_044_VrsaLapsed_PastCardInvoiceList() {
        page.openFromHub();
        if (page.openInvoiceFromPastCard(MySubscriptionPage.STOCK)) {
            page.verifyInvoicePage("Past Stock Advisor invoices");
            page.backFromInvoice();
        }
        finish();
    }

    @Test(priority = 45, groups = "vrsa-lapsed",
            description = "VRSA lapsed: the Stock Advisor CTA opens the Stock Advisor renewal / subscription page")
    public void MS_045_VrsaLapsed_StockAdvisorRenewNavigation() {
        page.openFromHub();
        page.verifyCtaOpensSubscriptionPage(MySubscriptionPage.STOCK);
        finish();
    }

    // =====================================================================================================
    // ACCOUNT 6: Active, autopay OFF, expiry within 30 days (product decided from the card on screen)
    // =====================================================================================================

    @Test(priority = 51, groups = "renew-window",
            description = "Renew window: cards follow the sorting rule Active, then Expired, then Free")
    public void MS_051_RenewWindow_PlacementRule() {
        page.openFromHub();
        page.verifyPlacementRule(page.readPage());
        finish();
    }

    @Test(priority = 52, groups = "renew-window",
            description = "Renew window: active card shows 'Expiring on' within 1 month and 'Renew' instead of Active, all fields present")
    public void MS_052_RenewWindow_ExpiringOnAndRenewButton() {
        page.openFromHub();
        PageModel m = page.readPage();
        String product = page.verifyRenewWindowCard(m);
        if (product != null) page.verifyActiveCard(m, product);
        finish();
    }

    @Test(priority = 53, groups = "renew-window",
            description = "Renew window: 'Renew' opens the renewal page of the same product")
    public void MS_053_RenewWindow_RenewOpensRenewalPage() {
        page.openFromHub();
        String product = page.verifyRenewWindowCard(page.readPage());
        if (product != null) page.verifyCtaOpensSubscriptionPage(product);
        finish();
    }

    @Test(priority = 54, groups = "renew-window",
            description = "Renew window: Invoice CTA on the expiring card opens the invoice list")
    public void MS_054_RenewWindow_InvoiceList() {
        page.openFromHub();
        String product = page.verifyRenewWindowCard(page.readPage());
        if (product != null && page.openInvoiceFromCard(product)) {
            page.verifyInvoicePage(product + " invoices");
            page.backFromInvoice();
        }
        finish();
    }

    // =====================================================================================================
    // ACCOUNT 7: VRFA active after a break + VRSA lapsed
    // =====================================================================================================

    @Test(priority = 61, groups = "vrfa-rebuy-vrsa-lapsed",
            description = "VRFA re-bought + VRSA lapsed: Fund Advisor Active card first, Stock Advisor Expired card last")
    public void MS_061_VrfaRebuy_CardOrder() {
        page.openFromHub();
        page.verifyCardOrder(page.readPage(), "Fund Advisor|ACTIVE", "Stock Advisor|EXPIRED");
        finish();
    }

    @Test(priority = 62, groups = "vrfa-rebuy-vrsa-lapsed",
            description = "VRFA re-bought + VRSA lapsed: Fund Advisor active card fields and Stock Advisor expired card")
    public void MS_062_VrfaRebuy_ActiveAndExpiredCards() {
        page.openFromHub();
        PageModel m = page.readPage();
        page.verifyActiveCard(m, MySubscriptionPage.FUND);
        page.verifyExpiredCard(m, MySubscriptionPage.STOCK);
        finish();
    }

    @Test(priority = 63, groups = "vrfa-rebuy-vrsa-lapsed",
            description = "VRFA re-bought + VRSA lapsed: tabs shown, old VRFA tenure and expired VRSA tenure listed, "
                    + "old tenure ended before the new Member from")
    public void MS_063_VrfaRebuy_PastSubscriptionsBothProducts() {
        page.openFromHub();
        page.verifyPastSection(page.readPage(), true, MySubscriptionPage.FUND, MySubscriptionPage.STOCK);
        finish();
    }

    @Test(priority = 64, groups = "vrfa-rebuy-vrsa-lapsed",
            description = "VRFA re-bought + VRSA lapsed: Invoice on the active Fund Advisor card opens the invoice list")
    public void MS_064_VrfaRebuy_ActiveInvoiceList() {
        page.openFromHub();
        if (page.openInvoiceFromCard(MySubscriptionPage.FUND)) {
            page.verifyInvoicePage("Fund Advisor invoices (current tenure)");
            page.backFromInvoice();
        }
        finish();
    }

    @Test(priority = 65, groups = "vrfa-rebuy-vrsa-lapsed",
            description = "VRFA re-bought + VRSA lapsed: Invoice on the old Fund Advisor past card opens the invoice list")
    public void MS_065_VrfaRebuy_PastInvoiceList() {
        page.openFromHub();
        if (page.openInvoiceFromPastCard(MySubscriptionPage.FUND)) {
            page.verifyInvoicePage("Past Fund Advisor invoices");
            page.backFromInvoice();
        }
        finish();
    }

    @Test(priority = 66, groups = "vrfa-rebuy-vrsa-lapsed",
            description = "VRFA re-bought + VRSA lapsed: Renew now on the Stock Advisor card opens the Stock Advisor page")
    public void MS_066_VrfaRebuy_StockAdvisorRenewNavigation() {
        page.openFromHub();
        page.verifyCtaOpensSubscriptionPage(MySubscriptionPage.STOCK);
        finish();
    }

    // =====================================================================================================
    // ACCOUNT 8: VRSA active + VRFA lapsed
    // =====================================================================================================

    @Test(priority = 71, groups = "vrsa-active-vrfa-lapsed",
            description = "VRSA active + VRFA lapsed: Stock Advisor Active card first, Fund Advisor Expired card last")
    public void MS_071_VrsaActiveVrfaLapsed_CardOrder() {
        page.openFromHub();
        page.verifyCardOrder(page.readPage(), "Stock Advisor|ACTIVE", "Fund Advisor|EXPIRED");
        finish();
    }

    @Test(priority = 72, groups = "vrsa-active-vrfa-lapsed",
            description = "VRSA active + VRFA lapsed: Stock Advisor active card fields and Fund Advisor expired card")
    public void MS_072_VrsaActiveVrfaLapsed_ActiveAndExpiredCards() {
        page.openFromHub();
        PageModel m = page.readPage();
        page.verifyActiveCard(m, MySubscriptionPage.STOCK);
        page.verifyExpiredCard(m, MySubscriptionPage.FUND);
        finish();
    }

    @Test(priority = 73, groups = "vrsa-active-vrfa-lapsed",
            description = "VRSA active + VRFA lapsed: expired VRFA tenure listed under Past, tabs only when both products have past")
    public void MS_073_VrsaActiveVrfaLapsed_PastSubscriptions() {
        page.openFromHub();
        page.verifyPastSection(page.readPage(), true, MySubscriptionPage.FUND);
        finish();
    }

    @Test(priority = 74, groups = "vrsa-active-vrfa-lapsed",
            description = "VRSA active + VRFA lapsed: Invoice on the active Stock Advisor card opens the invoice list")
    public void MS_074_VrsaActiveVrfaLapsed_ActiveInvoiceList() {
        page.openFromHub();
        if (page.openInvoiceFromCard(MySubscriptionPage.STOCK)) {
            page.verifyInvoicePage("Stock Advisor invoices");
            page.backFromInvoice();
        }
        finish();
    }

    @Test(priority = 75, groups = "vrsa-active-vrfa-lapsed",
            description = "VRSA active + VRFA lapsed: Renew now on the Fund Advisor card opens the Fund Advisor page")
    public void MS_075_VrsaActiveVrfaLapsed_FundAdvisorRenewNavigation() {
        page.openFromHub();
        page.verifyCtaOpensSubscriptionPage(MySubscriptionPage.FUND);
        finish();
    }

    // =====================================================================================================
    // ACCOUNT 9: VRFA lapsed + VRSA lapsed
    // =====================================================================================================

    @Test(priority = 81, groups = "both-lapsed",
            description = "Both lapsed: both products shown as Expired cards (order logged; not defined by the PRD)")
    public void MS_081_BothLapsed_TwoExpiredCards() {
        page.openFromHub();
        page.verifyBothExpired(page.readPage());
        finish();
    }

    @Test(priority = 82, groups = "both-lapsed",
            description = "Both lapsed: each expired card shows its own expiry message and Renew now, no subscription details")
    public void MS_082_BothLapsed_ExpiredCardContent() {
        page.openFromHub();
        PageModel m = page.readPage();
        page.verifyExpiredCard(m, MySubscriptionPage.FUND);
        page.verifyExpiredCard(m, MySubscriptionPage.STOCK);
        finish();
    }

    @Test(priority = 83, groups = "both-lapsed",
            description = "Both lapsed: tabs shown and each tab lists that product's expired tenure")
    public void MS_083_BothLapsed_PastSubscriptionsBothProducts() {
        page.openFromHub();
        page.verifyPastSection(page.readPage(), true, MySubscriptionPage.FUND, MySubscriptionPage.STOCK);
        finish();
    }

    @Test(priority = 84, groups = "both-lapsed",
            description = "Both lapsed: Invoice on the Fund Advisor past card opens the invoice list")
    public void MS_084_BothLapsed_FundPastInvoiceList() {
        page.openFromHub();
        if (page.openInvoiceFromPastCard(MySubscriptionPage.FUND)) {
            page.verifyInvoicePage("Past Fund Advisor invoices");
            page.backFromInvoice();
        }
        finish();
    }

    @Test(priority = 85, groups = "both-lapsed",
            description = "Both lapsed: Invoice on the Stock Advisor past card opens the invoice list")
    public void MS_085_BothLapsed_StockPastInvoiceList() {
        page.openFromHub();
        if (page.openInvoiceFromPastCard(MySubscriptionPage.STOCK)) {
            page.verifyInvoicePage("Past Stock Advisor invoices");
            page.backFromInvoice();
        }
        finish();
    }

    @Test(priority = 86, groups = "both-lapsed",
            description = "Both lapsed: Renew now on the Fund Advisor card opens the Fund Advisor page")
    public void MS_086_BothLapsed_FundAdvisorRenewNavigation() {
        page.openFromHub();
        page.verifyCtaOpensSubscriptionPage(MySubscriptionPage.FUND);
        finish();
    }

    @Test(priority = 87, groups = "both-lapsed",
            description = "Both lapsed: Renew now on the Stock Advisor card opens the Stock Advisor page")
    public void MS_087_BothLapsed_StockAdvisorRenewNavigation() {
        page.openFromHub();
        page.verifyCtaOpensSubscriptionPage(MySubscriptionPage.STOCK);
        finish();
    }

    // =====================================================================================================
    // Reporting + cleanup
    // =====================================================================================================

    /** Raises every collected PRD mismatch of the case at once; marks the case passed when there are none. */
    private void finish() {
        page.assertNoIssues();
        ExtentTestManager.getTest().pass("<span class='badge white-text green'>" + currentCaseId
                + " - all PRD checks passed</span>");
    }

    @AfterMethod(alwaysRun = true)
    public void returnToHub(ITestResult result) {
        Throwable original = result.getThrowable();
        if (original != null) attachFailureScreenshot("failed_state");
        Throwable cleanupFailure = null;
        try {
            if (accountPrepared && page != null) page.recoverToHub();
        } catch (RuntimeException | AssertionError failure) {
            // Navigation-only failure: keep the verified account; the next @BeforeMethod prepare() re-checks screens.
            cleanupFailure = failure;
        } finally {
            if (accountSession != null) {
                try {
                    accountSession.restoreTimingSettings();
                } catch (RuntimeException | AssertionError failure) {
                    if (cleanupFailure == null) cleanupFailure = failure;
                    else cleanupFailure.addSuppressed(failure);
                    accountSession.invalidate();
                }
            }
            accountPrepared = false;
        }
        if (cleanupFailure != null) {
            if (original != null) {
                original.addSuppressed(cleanupFailure);
            } else {
                result.setThrowable(cleanupFailure);
                result.setStatus(ITestResult.FAILURE);
                attachFailureScreenshot("cleanup");
            }
            ReportLogger.fail("My Subscription cleanup failed: " + cleanupFailure.getClass().getSimpleName());
        }
    }

    private void startExtentCase(String caseId, String title) {
        currentReport = ExtentManager.getExtentReports().createTest(caseId + " - " + title);
        currentReport.info("Module: My Subscription<br>Case ID: " + caseId + "<br>Page Build: "
                + MySubscriptionPage.BUILD_VERSION + "<br>PRD: Subscription details and Invoice (ticket 17157)");
        ExtentTestManager.setTest(currentReport);
    }

    private void attachFailureScreenshot(String label) {
        try {
            String path = ScreenshotUtils.captureScreenshot(driver, currentCaseId + "_" + label);
            if (path != null && currentReport != null) {
                currentReport.addScreenCaptureFromPath(path, "State at " + label);
            }
        } catch (Exception unavailable) {
            ReportLogger.debug("Failure screenshot unavailable: " + unavailable.getClass().getSimpleName());
        }
    }
}