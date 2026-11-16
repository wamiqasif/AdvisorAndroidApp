package com.valueresearch.pages;

import com.valueresearch.utils.AdvisorAccountSession;
import com.valueresearch.utils.ExecutionProfile;
import com.valueresearch.utils.ReportLogger;
import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;
import org.openqa.selenium.By;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.Rectangle;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * My Subscriptions page (Hub -> My Subscriptions) for active, lapsed and single-product accounts.
 *
 * <p>Design: every screen is read from ONE Appium page source (content-desc / text + bounds), so card
 * order, card state and field values are derived from on-screen positions and do not depend on how the
 * Flutter widgets are nested. Only the Hub tile locator and the back button below are real locators; everything
 * else is label based. Every failed check is logged as a {@code [BUG-CANDIDATE]} line (account, check, PRD page,
 * expected, actual), collected, and raised together by {@link #assertNoIssues()}.
 * The same text is appended to {@code test-output/my-subscription-issues.txt} for copy-paste into tickets.
 */
public class MySubscriptionPage {

    public static final String BUILD_VERSION = "MS-v1";
    public static final String FUND = "Fund Advisor";
    public static final String STOCK = "Stock Advisor";

    // ------------------------------------------------------------------------------------------------
    // Locators - the only places that may need tuning after an Appium Inspector check.
    // ------------------------------------------------------------------------------------------------
    private static final By BACK_BUTTON = AppiumBy.accessibilityId("Go back");
    // Hub > Profile > "Subscription Details" tile (Appium Inspector: android.widget.Button, content-desc below).
    private static final By HUB_TILE = AppiumBy.accessibilityId("View your subscription details");
    private static final By DOWNLOAD_CONTROL = AppiumBy.androidUIAutomator(
            "new UiSelector().descriptionContains(\"ownload\")");

    // ------------------------------------------------------------------------------------------------
    // Expected copy / formats (PRD: Subscription details and Invoice)
    // ------------------------------------------------------------------------------------------------
    private static final String PAGE_TITLE = "My Subscriptions";
    private static final String[] SECTION_HEADINGS = {"Past Subscriptions", "Other Subscriptions"};
    private static final String PRD_SECTION_HEADING = "Past Subscriptions";
    private static final String[] RENEWAL_LABELS = {
            "Renews on", "Auto-renews on", "Auto-renew on", "Auto renews on", "Expiring on", "Expires on"};
    private static final Pattern AMOUNT = Pattern.compile(
            "^(₹|Rs\\.?|INR)\\s?[\\d,]+(\\.\\d{1,2})?(\\s*(/|per)\\s*\\w+)?$", Pattern.CASE_INSENSITIVE);
    private static final Pattern INVOICE_NUMBER = Pattern.compile("^[A-Za-z0-9#/\\-]{6,}$");
    private static final DateTimeFormatter[] DATE_FORMATS = {
            DateTimeFormatter.ofPattern("d MMM, yyyy", Locale.ENGLISH),
            DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH),
            DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.ENGLISH),
            DateTimeFormatter.ofPattern("d-MMM-yyyy", Locale.ENGLISH),
            DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.ENGLISH)};
    private static final ZoneId IST = ZoneId.of("Asia/Kolkata");
    private static final int MAX_HUB_SWIPES = 12;
    private static final int MAX_VIEWPORTS = 4;

    private final AndroidDriver driver;
    private final AdvisorAccountSession preparedSession;
    private final List<String> issues = new ArrayList<>();
    private String caseId = "MS_???";
    private String account = "unknown account";

    public MySubscriptionPage(AndroidDriver driver, AdvisorAccountSession preparedSession) {
        this.driver = Objects.requireNonNull(driver, "AndroidDriver is required");
        this.preparedSession = Objects.requireNonNull(preparedSession, "Prepared AdvisorAccountSession is required");
        if (!preparedSession.isForDriver(driver)) {
            throw new IllegalArgumentException("Prepared account session belongs to another driver");
        }
    }

    // ================================================================================================
    // Models
    // ================================================================================================

    private static final class Box {
        final int x, y, w, h;
        Box(int x, int y, int w, int h) { this.x = x; this.y = y; this.w = w; this.h = h; }
        int cx() { return x + w / 2; }
        int cy() { return y + h / 2; }
    }

    private static final class Node {
        final List<String> lines;
        final Box box;
        Node(List<String> lines, Box box) { this.lines = lines; this.box = box; }
    }

    /** One text line of one accessibility node. Multi-line descriptions become several lines of one node. */
    private static final class Line {
        final String text;
        final Node node;
        final int idx;
        final int cy;
        Line(String text, Node node, int idx) {
            this.text = text;
            this.node = node;
            this.idx = idx;
            this.cy = node.box.y + (int) ((idx + 0.5) * node.box.h / node.lines.size());
        }
    }

    /** One subscription card (title + everything vertically below it up to the next card / section heading). */
    public static final class Card {
        private final String product;
        private final Line title;
        private final List<Line> lines = new ArrayList<>();
        private String state = "UNKNOWN";

        private Card(String product, Line title) { this.product = product; this.title = title; }

        public String product() { return product; }
        public String state() { return state; }

        private void classify() {
            if (line("Member from") != null) state = "ACTIVE";
            else if (expiredMessage() != null) state = "EXPIRED";
            else if (cta() != null && norm(cta().text).startsWith("subscribe")) state = "FREE";
            else state = "UNKNOWN";
        }

        private Line line(String... names) {
            for (Line l : lines) for (String n : names) if (norm(l.text).equals(norm(n))) return l;
            return null;
        }

        private Line firstPresent(String[] names) {
            for (String n : names) { Line l = line(n); if (l != null) return l; }
            return null;
        }

        private String valueOf(Line label) { return label == null ? null : valueOfLine(label, lines); }

        private Line statusPill() { return line("Active", "Renew"); }

        private Line cta() {
            for (Line l : lines) {
                String n = norm(l.text);
                if (n.startsWith("subscribe now") || n.startsWith("renew now")) return l;
            }
            return null;
        }

        private Line expiredMessage() {
            for (Line l : lines) {
                String n = norm(l.text);
                if (n.contains("expired") && !n.equals("expired on")) return l;
            }
            return null;
        }

        private boolean hasInvoice() { return line("Invoice", "Invoices") != null; }

        private List<String> benefitLines() {
            List<String> out = new ArrayList<>();
            for (Line l : lines) {
                String n = norm(l.text);
                if (l == title || n.equals(norm(product)) || n.startsWith("subscribe") || n.startsWith("renew")
                        || n.contains("invoice") || n.length() < 12) continue;
                out.add(l.text);
            }
            return out;
        }

        private String summary() {
            StringBuilder sb = new StringBuilder();
            for (Line l : lines) sb.append(l.text.replace('\n', ' ')).append(" | ");
            return trim(sb.toString(), 360);
        }
    }

    /** Everything read from the page in one pass. */
    public static final class PageModel {
        private final List<Card> cards = new ArrayList<>();
        private String sectionHeading;
        private final Set<String> pastTexts = new LinkedHashSet<>();
        private List<Line> pastCard = new ArrayList<>();
        private boolean tabFund, tabStock, pillPast, pillUpcoming, hasPastCard;

        private Card card(String product) {
            for (Card c : cards) if (c.product.equalsIgnoreCase(product)) return c;
            return null;
        }

        private boolean has(String product) { return card(product) != null; }

        private boolean tabsShown() { return tabFund && tabStock; }

        private String orderSummary() {
            if (cards.isEmpty()) return "(no Fund Advisor / Stock Advisor card found)";
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < cards.size(); i++) {
                if (i > 0) sb.append("  ");
                sb.append(i + 1).append(") ").append(cards.get(i).product).append(" [").append(cards.get(i).state).append("]");
            }
            return sb.toString();
        }

        private void derive() {
            tabFund = pastTexts.contains(norm(FUND));
            tabStock = pastTexts.contains(norm(STOCK));
            pillPast = pastTexts.contains("past");
            pillUpcoming = pastTexts.contains("upcoming");
            for (Line l : pastCard) if (norm(l.text).equals("expired on")) { hasPastCard = true; break; }
        }
    }

    // ================================================================================================
    // Case bookkeeping + bug-ready reporting
    // ================================================================================================

    public void beginCase(String caseId, String accountLabel) {
        this.caseId = caseId;
        this.account = accountLabel;
        issues.clear();
        ReportLogger.step("[" + BUILD_VERSION + "] " + caseId + " | account under test: " + accountLabel);
    }

    private void expect(boolean ok, String check, String prd, String expected, String actual) {
        if (ok) {
            ReportLogger.pass("[CHECK OK] " + check + " | PRD " + prd + " | Actual: " + trim(actual, 220));
            return;
        }
        String issue = "[BUG-CANDIDATE] " + caseId + " | Account: " + account + " | Check: " + check
                + " | PRD: " + prd + " | Expected: " + expected + " | Actual: " + trim(actual, 400);
        issues.add(issue);
        ReportLogger.fail(issue);
    }

    private void note(String message) {
        ReportLogger.info("[NOTE] " + caseId + " | " + message);
    }

    /** Raises every collected issue at once so a single run lists all defects of the case. */
    public void assertNoIssues() {
        if (issues.isEmpty()) {
            ReportLogger.pass(caseId + ": all PRD checks passed for " + account);
            return;
        }
        StringBuilder sb = new StringBuilder(issues.size() + " issue(s) in " + caseId + " [" + account + "]:");
        for (int i = 0; i < issues.size(); i++) sb.append("\n ").append(i + 1).append(") ").append(issues.get(i));
        writeIssueFile(sb.toString());
        throw new AssertionError(sb.toString());
    }

    private void writeIssueFile(String text) {
        try {
            Path file = Path.of(System.getProperty("user.dir"), "test-output", "my-subscription-issues.txt");
            Files.createDirectories(file.getParent());
            String block = "==== " + LocalDateTime.now(IST) + " | " + BUILD_VERSION + " ====\n" + text + "\n\n";
            Files.writeString(file, block, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException | RuntimeException unavailable) {
            ReportLogger.debug("Issue file not written: " + unavailable.getClass().getSimpleName());
        }
    }

    // ================================================================================================
    // Navigation
    // ================================================================================================

    public void openFromHub() {
        ReportLogger.step("[" + BUILD_VERSION + "] Opening My Subscriptions from Hub");
        preparedSession.assertPrepared();
        if (isPageReady()) {
            ReportLogger.pass("My Subscriptions is already open");
            return;
        }
        preparedSession.returnToHub();
        Dimension screen = driver.manage().window().getSize();
        for (int swipe = 0; swipe <= MAX_HUB_SWIPES; swipe++) {
            WebElement tile = visibleHubTile();
            if (tile != null) {
                Rectangle r = tile.getRect();
                if (r.getY() + r.getHeight() > screen.height - 220) {
                    swipeUp(screen, 0.3); // too close to the bottom navigation bar to tap safely
                    continue;
                }
                ReportLogger.step("Hub tile found; opening My Subscriptions");
                tapXY(r.getX() + r.getWidth() / 2, r.getY() + r.getHeight() / 2);
                try {
                    waitForPage(ExecutionProfile.shortWait(), "after tapping the Hub tile");
                } catch (AssertionError firstTapIgnored) {
                    WebElement again = visibleHubTile();
                    if (again == null) throw firstTapIgnored;
                    ReportLogger.step("First tap did not navigate; retrying with an element click");
                    again.click();
                    waitForPage(ExecutionProfile.defaultWait(), "after retrying the Hub tile");
                }
                waitForAnyProductTitle();
                ReportLogger.pass("My Subscriptions page opened from Hub");
                return;
            }
            swipeUp(screen, 0.5);
        }
        throw new AssertionError("Hub tile for My Subscriptions was not found after " + MAX_HUB_SWIPES
                + " swipes (accessibility id 'View your subscription details'). Visible: "
                + brief(lines()));
    }

    public void recoverToHub() {
        preparedSession.returnToHub();
        preparedSession.assertHubVisible();
    }

    private WebElement visibleHubTile() {
        for (WebElement e : driver.findElements(HUB_TILE)) {
            try { if (e.isDisplayed()) return e; } catch (WebDriverException ignored) { /* stale */ }
        }
        return null;
    }

    private boolean isPageReady() {
        try {
            if (driver.findElements(BACK_BUTTON).isEmpty()) return false;
            for (Line l : lines()) if (norm(l.text).equals(norm(PAGE_TITLE))) return true;
        } catch (WebDriverException ignored) { /* transient while the screen changes */ }
        return false;
    }

    private void waitForPage(java.time.Duration timeout, String when) {
        try {
            new WebDriverWait(driver, timeout).pollingEvery(ExecutionProfile.pollingInterval())
                    .until(d -> isPageReady());
        } catch (TimeoutException timeoutFailure) {
            throw new AssertionError("My Subscriptions page did not appear " + when + ". Visible: " + brief(lines()),
                    timeoutFailure);
        }
    }

    private void waitForAnyProductTitle() {
        try {
            new WebDriverWait(driver, ExecutionProfile.defaultWait()).pollingEvery(ExecutionProfile.pollingInterval())
                    .until(d -> {
                        for (Line l : lines()) {
                            String n = norm(l.text);
                            if (n.equals(norm(FUND)) || n.equals(norm(STOCK))) return true;
                        }
                        return false;
                    });
        } catch (TimeoutException timeoutFailure) {
            throw new AssertionError("My Subscriptions opened but no Fund Advisor / Stock Advisor card loaded in "
                    + ExecutionProfile.defaultWait().getSeconds() + "s. Visible: " + brief(lines()), timeoutFailure);
        }
    }

    private void returnToMySubscriptions() {
        for (int attempt = 0; attempt < 4; attempt++) {
            if (isPageReady()) return;
            if (!driver.findElements(BACK_BUTTON).isEmpty()) driver.findElement(BACK_BUTTON).click();
            else driver.navigate().back();
            try {
                waitForPage(ExecutionProfile.shortWait(), "after going back");
                return;
            } catch (AssertionError stillAway) {
                ReportLogger.step("Still not on My Subscriptions (attempt " + (attempt + 1) + "); going back again");
            }
        }
        throw new AssertionError("Could not return to My Subscriptions. Visible: " + brief(lines()));
    }

    // ================================================================================================
    // Page reading
    // ================================================================================================

    public PageModel readPage() {
        ReportLogger.step("Reading My Subscriptions (cards, section heading, past list)");
        waitForPage(ExecutionProfile.defaultWait(), "before reading cards");
        waitForAnyProductTitle();
        PageModel m = new PageModel();
        Dimension screen = driver.manage().window().getSize();
        String previous = null;
        for (int viewport = 0; viewport < MAX_VIEWPORTS; viewport++) {
            List<Line> lines = lines();
            String signature = signature(lines);
            if (signature.equals(previous)) break; // the page cannot scroll any further
            previous = signature;
            Line heading = findHeading(lines);
            int limit = heading != null ? heading.cy : Integer.MAX_VALUE;
            for (Card c : parseCards(lines, limit)) if (!m.has(c.product)) m.cards.add(c);
            if (heading != null) {
                m.sectionHeading = heading.text;
                List<Line> below = linesBelow(lines, heading.cy);
                collectPastTexts(m, below);
                m.pastCard = below;
                if (!containsText(below, "Expired on")) {
                    swipeUp(screen, 0.6);
                    List<Line> second = lines();
                    if (!signature(second).equals(signature)) {
                        Line heading2 = findHeading(second);
                        List<Line> belowSecond = heading2 != null ? linesBelow(second, heading2.cy) : second;
                        collectPastTexts(m, belowSecond);
                        m.pastCard = belowSecond;
                    }
                }
                break;
            }
            swipeUp(screen, 0.6);
        }
        scrollToTop(screen);
        m.derive();
        logObserved(m);
        return m;
    }

    private void collectPastTexts(PageModel m, List<Line> below) {
        for (Line l : below) m.pastTexts.add(norm(l.text));
    }

    private List<Card> parseCards(List<Line> lines, int limitY) {
        List<Line> titles = new ArrayList<>();
        for (String product : new String[]{FUND, STOCK}) {
            Line best = null;
            for (Line l : lines) {
                if (l.cy < limitY && norm(l.text).equals(norm(product)) && (best == null || l.cy < best.cy)) best = l;
            }
            if (best != null) titles.add(best);
        }
        titles.sort(Comparator.comparingInt(l -> l.cy));
        List<Card> cards = new ArrayList<>();
        for (int i = 0; i < titles.size(); i++) {
            Line title = titles.get(i);
            Card card = new Card(norm(title.text).equals(norm(FUND)) ? FUND : STOCK, title);
            int from = title.cy - 30; // the Invoice link sits on the title row, slightly above its centre
            int to = i + 1 < titles.size() ? titles.get(i + 1).cy - 30 : limitY;
            for (Line l : lines) if (l.cy >= from && l.cy < to) card.lines.add(l);
            card.classify();
            cards.add(card);
        }
        return cards;
    }

    private List<Card> freshTopCards() {
        scrollToTop(driver.manage().window().getSize());
        List<Line> lines = lines();
        Line heading = findHeading(lines);
        return parseCards(lines, heading != null ? heading.cy : Integer.MAX_VALUE);
    }

    private Line findHeading(List<Line> lines) {
        for (Line l : lines) for (String h : SECTION_HEADINGS) if (norm(l.text).equals(norm(h))) return l;
        return null;
    }

    private List<Line> linesBelow(List<Line> lines, int y) {
        List<Line> out = new ArrayList<>();
        for (Line l : lines) if (l.cy > y) out.add(l);
        return out;
    }

    private void logObserved(PageModel m) {
        ReportLogger.info("[OBSERVED] " + caseId + " | card order (top to bottom): " + m.orderSummary());
        for (int i = 0; i < m.cards.size(); i++) {
            Card c = m.cards.get(i);
            Line cta = c.cta();
            Line pill = c.statusPill();
            ReportLogger.info("[OBSERVED] Card " + (i + 1) + ": " + c.product + " | state=" + c.state
                    + " | cta=" + (cta == null ? "-" : "'" + cta.text + "'")
                    + " | pill=" + (pill == null ? "-" : "'" + pill.text + "'") + " | content: " + c.summary());
        }
        ReportLogger.info("[OBSERVED] Section heading='" + (m.sectionHeading == null ? "(none)" : m.sectionHeading)
                + "' | Fund/Stock tabs=" + m.tabsShown() + " | Past pill=" + m.pillPast
                + " | Upcoming pill=" + m.pillUpcoming + " | past card visible=" + m.hasPastCard);
    }

    // ================================================================================================
    // Card position + content checks
    // ================================================================================================

    /** expected entries look like "Stock Advisor|ACTIVE" (PRD p.6: Active first, then Expired, then Free). */
    public void verifyCardOrder(PageModel m, String... expected) {
        StringBuilder exp = new StringBuilder();
        for (int i = 0; i < expected.length; i++) {
            String[] parts = expected[i].split("\\|");
            if (i > 0) exp.append("  ");
            exp.append(i + 1).append(") ").append(parts[0]).append(" [").append(parts[1]).append("]");
        }
        for (int i = 0; i < expected.length; i++) {
            String[] parts = expected[i].split("\\|");
            String actual = i < m.cards.size() ? m.cards.get(i).product + " [" + m.cards.get(i).state + "]"
                    : "(missing)";
            note("Position " + (i + 1) + " expected " + parts[0] + " [" + parts[1] + "], actual " + actual);
        }
        expect(m.orderSummary().equals(exp.toString()),
                "Card order top to bottom (Active first, then Expired, then Free)",
                "p.6 Placement Rules", exp.toString(), m.orderSummary());
    }

    /** PRD p.2: both active subscriptions stacked, ordered by date of purchase with the latest on top. */
    public void verifyBothActiveOrder(PageModel m) {
        expect(m.cards.size() == 2 && "ACTIVE".equals(m.cards.get(0).state) && "ACTIVE".equals(m.cards.get(1).state),
                "Both Fund Advisor and Stock Advisor are shown as stacked Active cards",
                "p.2", "Two Active cards (Fund Advisor + Stock Advisor)", m.orderSummary());
        if (m.cards.size() != 2) return;
        LocalDate first = parseDate(m.cards.get(0).valueOf(m.cards.get(0).line("Member from")));
        LocalDate second = parseDate(m.cards.get(1).valueOf(m.cards.get(1).line("Member from")));
        if (first == null || second == null) {
            note("Could not read both 'Member from' dates; verify the latest-first order manually against purchase dates");
            return;
        }
        if (first.equals(second)) {
            note("Both cards show the same Member from date (" + first + "); latest-first order cannot be derived from the UI");
            return;
        }
        expect(first.isAfter(second), "Latest subscription is on top (compared by 'Member from')",
                "p.2 / p.6 (latest subscribed/renewed first)",
                "Top card Member from >= second card Member from",
                m.cards.get(0).product + " Member from " + first + " vs " + m.cards.get(1).product + " Member from " + second);
    }

    /** PRD p.6 sorting rule for any account: Active cards first, then Expired, then Free. */
    public void verifyPlacementRule(PageModel m) {
        int previous = -1;
        boolean ordered = !m.cards.isEmpty();
        for (Card c : m.cards) {
            int rank = "ACTIVE".equals(c.state) ? 0 : "EXPIRED".equals(c.state) ? 1 : "FREE".equals(c.state) ? 2 : 3;
            if (rank < previous) ordered = false;
            previous = Math.max(previous, rank);
        }
        expect(ordered, "Cards follow the sorting rule Active, then Expired, then Free", "p.6 Placement Rules",
                "Active -> Expired -> Free", m.orderSummary());
    }

    /** Both products lapsed: two Expired cards (PRD p.5); the PRD does not fix their relative order. */
    public void verifyBothExpired(PageModel m) {
        expect(m.cards.size() == 2 && "EXPIRED".equals(m.cards.get(0).state) && "EXPIRED".equals(m.cards.get(1).state),
                "Both Fund Advisor and Stock Advisor are shown as Expired cards", "p.5 / p.6",
                "Two Expired cards", m.orderSummary());
        note("Order of two expired cards is not defined in the PRD; observed " + m.orderSummary() + " (confirm with PM)");
    }

    /**
     * Active card with autopay OFF and 1 month or less to expiry (PRD p.3): label 'Expiring on', button 'Renew'.
     *
     * @return the product of that card, or null when no such card is shown
     */
    public String verifyRenewWindowCard(PageModel m) {
        Card target = null;
        Line label = null;
        StringBuilder seen = new StringBuilder();
        for (Card c : m.cards) {
            Line l = c.firstPresent(RENEWAL_LABELS);
            seen.append(c.product).append(" [").append(c.state).append(", ").append(l == null ? "-" : l.text).append("] ");
            if ("ACTIVE".equals(c.state) && l != null && norm(l.text).startsWith("expir")) { target = c; label = l; }
        }
        expect(target != null, "An active card without autopay shows 'Expiring on'", "p.2",
                "Active card labelled 'Expiring on' (autopay OFF)", seen.toString().trim());
        if (target == null) return null;
        LocalDate today = LocalDate.now(IST);
        LocalDate expiry = parseDate(target.valueOf(label));
        expect(expiry != null && !expiry.isBefore(today) && !expiry.isAfter(today.plusMonths(1)),
                "TEST DATA precondition: " + target.product + " expiry is within 1 month (refresh the account if this fails)",
                "p.3", "Expiring on between " + today + " and " + today.plusMonths(1), String.valueOf(expiry));
        Line pill = target.statusPill();
        expect(pill != null && norm(pill.text).equals("renew"),
                target.product + ": 'Renew' replaces 'Active' when 1 month or less is left and there is no mandate",
                "p.3", "Renew", pill == null ? "(no status button)" : "'" + pill.text + "'");
        return target.product;
    }

    public void verifyActiveCard(PageModel m, String product) {
        Card c = m.card(product);
        String pre = product + " active card: ";
        expect(c != null, pre + "card is present", "p.2", product + " card on My Subscriptions", m.orderSummary());
        if (c == null) return;
        expect("ACTIVE".equals(c.state), pre + "shown in Active state (has 'Member from')", "p.2",
                "Active card with Member from, renewal date, Amount, Frequency, status", "state=" + c.state + " | " + c.summary());
        if (!"ACTIVE".equals(c.state)) return;

        expect(c.hasInvoice(), pre + "'Invoice' CTA is shown", "p.2 / p.3", "Invoice link in the card header", c.summary());

        String from = c.valueOf(c.line("Member from"));
        LocalDate fromDate = parseDate(from);
        expect(fromDate != null, pre + "'Member from' shows the subscription start date", "p.2",
                "A date such as 8 Oct, 2026", "'" + from + "'");
        LocalDate today = LocalDate.now(IST);
        if (fromDate != null) {
            expect(!fromDate.isAfter(today), pre + "'Member from' is not in the future", "p.2",
                    "Start date <= " + today, fromDate.toString());
        }

        Line renewLabel = c.firstPresent(RENEWAL_LABELS);
        expect(renewLabel != null, pre + "'Renews on / Expiring on' row is shown", "p.2",
                "One of " + String.join(" / ", RENEWAL_LABELS), c.summary());
        LocalDate renewDate = null;
        if (renewLabel != null) {
            String value = c.valueOf(renewLabel);
            renewDate = parseDate(value);
            expect(renewDate != null, pre + "'" + renewLabel.text + "' shows a valid date", "p.2",
                    "A date such as 7 Oct, 2027", "'" + value + "'");
            if (renewDate != null) {
                expect(!renewDate.isBefore(today), pre + "'" + renewLabel.text + "' date is not in the past for an Active card",
                        "p.2", "Date >= " + today, renewDate.toString());
                if (fromDate != null) {
                    expect(renewDate.isAfter(fromDate), pre + "Renew/Expiry date is after 'Member from'", "p.2",
                            "Date > " + fromDate, renewDate.toString());
                }
            }
        }

        String amount = c.valueOf(c.line("Amount"));
        expect(amount != null && AMOUNT.matcher(amount.trim()).matches(), pre + "'Amount' shows the renewal price in rupees",
                "p.2", "Rupee amount such as ₹499", "'" + amount + "'");
        String frequency = c.valueOf(c.line("Frequency"));
        expect(frequency != null && !frequency.trim().isEmpty(), pre + "'Frequency' is shown", "p.2",
                "Plan frequency such as Quarterly / Yearly", "'" + frequency + "'");

        Line pill = c.statusPill();
        expect(pill != null, pre + "status pill (Active / Renew) is shown", "p.3", "Active or Renew", c.summary());
        if (pill != null && renewLabel != null && renewDate != null) {
            boolean mandate = !norm(renewLabel.text).startsWith("expir");
            if (mandate) {
                expect(norm(pill.text).equals("active"),
                        pre + "mandate exists ('" + renewLabel.text + "') so status must be Active", "p.3",
                        "Active", "'" + pill.text + "'");
            } else {
                boolean monthOrLess = !renewDate.isAfter(today.plusMonths(1));
                String expectedPill = monthOrLess ? "Renew" : "Active";
                expect(norm(pill.text).equals(norm(expectedPill)),
                        pre + "no mandate ('" + renewLabel.text + "' " + renewDate + "): "
                                + (monthOrLess ? "1 month or less left => Renew" : "more than 1 month left => Active"),
                        "p.3", expectedPill, "'" + pill.text + "'");
            }
        }
        if (renewLabel != null && !norm(renewLabel.text).equals("renews on") && !norm(renewLabel.text).startsWith("expir")) {
            note("Label reads '" + renewLabel.text + "'; PRD wording is 'Renews on' / 'Expiring on' (copy difference, confirm against Figma)");
        }
    }

    public void verifyExpiredCard(PageModel m, String product) {
        Card c = m.card(product);
        String pre = product + " expired card: ";
        expect(c != null, pre + "card is present", "p.5", product + " card on My Subscriptions", m.orderSummary());
        if (c == null) return;
        expect("EXPIRED".equals(c.state), pre + "shown as an Expired card", "p.5",
                "Message 'Your subscription to Value Research " + product + " has expired.' + Renew now",
                "state=" + c.state + " | " + c.summary());
        if (!"EXPIRED".equals(c.state)) return;

        Line message = c.expiredMessage();
        expect(message != null && norm(message.text).contains(norm(product)), pre + "expiry message names the product",
                "p.5", "Message mentioning " + product, message == null ? "(none)" : "'" + message.text + "'");
        if (message != null && !norm(message.text).startsWith(norm("Your subscription to Value Research " + product + " has expired"))) {
            note("Expiry copy differs from PRD p.5. Observed: '" + message.text + "'");
        }
        Line cta = c.cta();
        expect(cta != null && norm(cta.text).startsWith("renew now"), pre + "CTA is 'Renew now'", "p.5", "Renew now",
                cta == null ? "(no CTA)" : "'" + cta.text + "' (if this account's lapsed plan was a trial / Rs 0 plan, "
                        + "ask PM whether 'Subscribe now' is intended for trial users)");
        expect(c.line("Member from") == null && c.statusPill() == null,
                pre + "no Member from / Active / Renew status on an expired card", "p.5",
                "Only the expiry message and Renew now", c.summary());
        expect(!c.hasInvoice(), pre + "no Invoice link on the expired card (old invoices live under Past Subscriptions)",
                "p.5 / p.7", "No Invoice link here", c.summary());
    }

    public void verifyFreeCard(PageModel m, String product) {
        Card c = m.card(product);
        String pre = product + " free card: ";
        expect(c != null, pre + "card is present", "p.5 / p.6", product + " 'Subscribe Now' card", m.orderSummary());
        if (c == null) return;
        expect("FREE".equals(c.state), pre + "shown as a free card with 'Subscribe Now'", "p.5 / p.6",
                "Title + benefits text + Subscribe Now", "state=" + c.state + " | " + c.summary());
        if (!"FREE".equals(c.state)) return;
        Line cta = c.cta();
        if (cta != null && !norm(cta.text).equals("subscribe now")) {
            note("Button copy differs; PRD p.5 says 'Subscribe Now'. Observed: '" + cta.text + "'");
        }
        List<String> benefits = c.benefitLines();
        expect(!benefits.isEmpty(), pre + "benefits / description text is shown above the button", "p.5",
                "At least one benefit line under the title", benefits.isEmpty() ? "only title and button: " + c.summary()
                        : benefits.toString());
        expect(c.line("Member from") == null && !c.hasInvoice() && c.statusPill() == null,
                pre + "no subscription details, Invoice link or Active/Renew status", "p.5",
                "Free card has no subscription details", c.summary());
    }

    /** One card of the Other / Past Subscriptions list. */
    private static final class PastCard {
        private final String product;
        private String purchase, expired, amount, frequency;
        private Line invoiceLine;
        private PastCard(String product) { this.product = product; }
        private String label() { return product + " past card [" + purchase + " to " + expired + "]"; }
        private int completeness() {
            int n = 0;
            for (String v : new String[]{purchase, expired, amount, frequency}) if (v != null) n++;
            return n + (invoiceLine != null ? 1 : 0);
        }
    }

    /** Past cards listed under one product tab (or under the untabbed list). */
    private static final class PastTab {
        private final String product;
        private final List<PastCard> cards = new ArrayList<>();
        private String emptyText;
        private PastTab(String product) { this.product = product; }
    }

    /**
     * Other / Past Subscriptions checks (PRD p.4-p.7).
     *
     * <p>Tabs: when the Fund/Stock tabs are shown, each tab is opened (and the Stock Advisor "Past" pill selected)
     * and its past cards are read, so "tabs only when BOTH products have past subscriptions" is decided from the
     * data on screen, not from assumptions about the account.
     *
     * @param pastExpected true when the account must show past tenures for every product in pastProducts
     * @param pastProducts products that MUST have a past tenure (lapsed / re-bought accounts); empty when unknown
     */
    public void verifyPastSection(PageModel m, boolean pastExpected, String... pastProducts) {
        String pastProduct = pastProducts.length == 1 ? pastProducts[0] : null;
        if (m.sectionHeading == null) {
            if (pastExpected) {
                expect(false, "Past Subscriptions section is shown for a user with an expired tenure", "p.5 / p.6",
                        "Section with the expired " + String.join(" + ", pastProducts) + " tenure(s)", "No Past / Other Subscriptions section");
            } else {
                note("No Past / Other Subscriptions section (correct when there is no past or upcoming subscription)");
            }
            return;
        }
        expect(norm(m.sectionHeading).equals(norm(PRD_SECTION_HEADING)), "Section heading text", "p.5 / p.6",
                "'" + PRD_SECTION_HEADING + "'", "'" + m.sectionHeading + "'");

        // 1) Read everything the section contains: past cards per tab, then the Stock Advisor Upcoming list.
        List<PastTab> tabs = new ArrayList<>();
        if (m.tabsShown()) {
            for (String product : new String[]{FUND, STOCK}) {
                if (!scrollToHeading() || !selectTab(product)) {
                    expect(false, product + " tab can be opened", "p.4 / p.6", "Tab tappable under the section heading",
                            "Tab not found. Visible: " + brief(lines()));
                    continue;
                }
                PastTab tab = collectPastCards(product);
                tabs.add(tab);
                logPastTab(tab);
            }
        } else {
            scrollToHeading();
            PastTab tab = collectPastCards(pastProduct != null ? pastProduct : "Past list");
            tabs.add(tab);
            logPastTab(tab);
        }
        UpcomingInfo upcoming = readUpcoming();
        boolean anyPast = false;
        StringBuilder inventory = new StringBuilder();
        for (PastTab tab : tabs) {
            if (!tab.cards.isEmpty()) anyPast = true;
            inventory.append(tab.product).append(" past=").append(tab.cards.size()).append(", ");
        }
        inventory.append(upcoming.summary);
        ReportLogger.info("[OBSERVED] " + caseId + " | " + m.sectionHeading + " content: " + inventory);

        // 2) Section-level rule: no past and no upcoming entry anywhere => the section must not be shown at all.
        expect(anyPast || upcoming.hasData,
                "'" + m.sectionHeading + "' section is shown only when a past or upcoming subscription exists",
                "p.6 (section appears ONLY if past tenures exist) / p.4 (Upcoming)",
                "Section hidden when Fund Advisor and Stock Advisor have no past or upcoming subscriptions",
                "Section shown with no entries: " + inventory);
        if (!anyPast && !upcoming.hasData) {
            scrollToTop(driver.manage().window().getSize());
            return; // the section itself is the defect; per-tab failures would only repeat it
        }

        // 3) Tabs rule: tabs only when BOTH products have past subscriptions.
        if (m.tabsShown()) {
            for (PastTab tab : tabs) {
                expect(!tab.cards.isEmpty(),
                        "Fund/Stock tabs are shown only when BOTH products have past subscriptions: " + tab.product + " tab",
                        "p.6", "Past subscription card(s) under the " + tab.product + " tab, otherwise no tabs at all",
                        "Tab is empty" + (tab.emptyText == null ? "" : " and shows '" + tab.emptyText + "'"));
            }
        }

        if (pastExpected && pastProducts.length > 1) {
            expect(m.tabsShown(), "Fund/Stock tabs are shown when BOTH products have past subscriptions", "p.6",
                    "Fund Advisor and Stock Advisor tabs", "No tabs");
        }
        if (pastExpected) {
            for (String product : pastProducts) {
                PastTab target = null;
                for (PastTab tab : tabs) if (tab.product.equals(product)) target = tab;
                expect(target != null && !target.cards.isEmpty(), product + " expired tenure is listed under Past Subscriptions",
                        "p.5 / p.6 / p.7", "At least one " + product + " past card",
                        target == null ? "No " + product + " list found" : target.cards.size() + " card(s)");
            }
        }
        for (PastTab tab : tabs) {
            for (PastCard c : tab.cards) verifyPastCardFields(m, c);
            verifyNoContinuousSplit(tab);
            if (tab.emptyText != null) {
                note("Empty " + tab.product + " list shows '" + tab.emptyText + "'. This reads like a data error; PRD p.6 says "
                        + "the section should not appear when there is no past subscription");
            }
        }
        if (upcoming.pill && upcoming.emptyText != null) {
            note("Empty Upcoming list shows '" + upcoming.emptyText + "'");
        }
        scrollToTop(driver.manage().window().getSize());
    }

    /** What the Stock Advisor "Upcoming" pill shows (PRD p.4). */
    private static final class UpcomingInfo {
        private boolean pill, hasData;
        private String emptyText;
        private String summary = "Upcoming pill not shown";
    }

    private UpcomingInfo readUpcoming() {
        UpcomingInfo u = new UpcomingInfo();
        if (!scrollToHeading()) return u;
        List<Line> lines = lines();
        Line heading = findHeading(lines);
        boolean fundTab = false, stockTab = false;
        for (Line l : lines) {
            if (heading == null || l.cy <= heading.cy) continue;
            if (norm(l.text).equals(norm(FUND))) fundTab = true;
            if (norm(l.text).equals(norm(STOCK))) stockTab = true;
        }
        if (fundTab && stockTab) { // the Past / Upcoming pills live under the Stock Advisor tab
            selectTab(STOCK);
            lines = lines();
            heading = findHeading(lines);
        }
        Line pill = null;
        for (Line l : lines) {
            if ((heading == null || l.cy > heading.cy) && norm(l.text).equals("upcoming")) { pill = l; break; }
        }
        if (pill == null) return u;
        u.pill = true;
        ReportLogger.step("Selecting the Upcoming pill");
        tapLine(pill);
        waitForStable();
        List<Line> after = lines();
        Line pillNow = null;
        for (Line l : after) if (norm(l.text).equals("upcoming")) { pillNow = l; break; }
        int minY = pillNow != null ? pillNow.cy + 10 : 0;
        List<String> content = new ArrayList<>();
        boolean dated = false;
        for (Line l : after) {
            if (l.cy <= minY) continue;
            String n = norm(l.text);
            if (n.contains("data is unavailable") || n.contains("no upcoming") || n.contains("no subscription")) {
                u.emptyText = l.text;
                continue;
            }
            content.add(l.text);
            if (parseDate(l.text) != null) dated = true;
        }
        u.hasData = dated && u.emptyText == null;
        u.summary = u.hasData ? "Stock Advisor upcoming: " + trim(content.toString(), 200)
                : "Stock Advisor upcoming: empty" + (u.emptyText == null ? "" : " ('" + u.emptyText + "')");
        for (Line l : after) { // leave the list on its default Past pill
            if (norm(l.text).equals("past")) { tapLine(l); waitForStable(); break; }
        }
        return u;
    }

    private void verifyPastCardFields(PageModel m, PastCard c) {
        String pre = c.label() + ": ";
        LocalDate today = LocalDate.now(IST);
        LocalDate purchase = parseDate(c.purchase);
        LocalDate expired = parseDate(c.expired);
        expect(purchase != null, pre + "header shows the date of purchase", "p.6", "A date at the top of the card", "'" + c.purchase + "'");
        expect(expired != null, pre + "'Expired on' shows a date", "p.6", "A valid date", "'" + c.expired + "'");
        if (expired != null) {
            expect(!expired.isAfter(today), pre + "a past subscription has already ended", "p.6",
                    "Expired on <= " + today, expired.toString());
        }
        if (purchase != null && expired != null) {
            expect(!expired.isBefore(purchase), pre + "expiry is not before the purchase date", "p.6",
                    "Expired on >= " + purchase, expired.toString());
        }
        expect(c.amount != null && AMOUNT.matcher(c.amount.trim()).matches(), pre + "'Amount' is shown", "p.6",
                "Rupee amount", "'" + c.amount + "'");
        expect(c.frequency != null && !c.frequency.trim().isEmpty(), pre + "'Frequency' is shown", "p.6",
                "Plan frequency", "'" + c.frequency + "'");
        expect(c.invoiceLine != null, pre + "'Invoice' CTA is shown", "p.6 / p.7", "Invoice link on the card",
                c.invoiceLine == null ? "(none)" : "present");
        Card active = m.card(c.product);
        if (active != null && "ACTIVE".equals(active.state) && expired != null) {
            LocalDate activeFrom = parseDate(active.valueOf(active.line("Member from")));
            if (activeFrom != null) {
                expect(expired.isBefore(activeFrom),
                        pre + "past tenure ended before the current " + c.product + " subscription started (active one is excluded)",
                        "p.6", "Expired on < active Member from " + activeFrom, expired.toString());
            }
        }
    }

    /** PRD p.6: a continuous stretch of renewals is ONE tenure, so consecutive past cards must have a gap. */
    private void verifyNoContinuousSplit(PastTab tab) {
        List<PastCard> dated = new ArrayList<>();
        for (PastCard c : tab.cards) if (parseDate(c.purchase) != null && parseDate(c.expired) != null) dated.add(c);
        dated.sort(Comparator.comparing(c -> parseDate(c.purchase)));
        for (int i = 0; i + 1 < dated.size(); i++) {
            LocalDate end = parseDate(dated.get(i).expired);
            LocalDate nextStart = parseDate(dated.get(i + 1).purchase);
            expect(nextStart.isAfter(end.plusDays(1)),
                    tab.product + ": continuous renewal is not split into separate past tenures",
                    "p.6 (continuous renewals count as one subscription)",
                    "A gap between " + dated.get(i).label() + " and " + dated.get(i + 1).label(),
                    "Next tenure starts " + nextStart + ", previous ended " + end);
        }
    }

    private void logPastTab(PastTab tab) {
        ReportLogger.info("[OBSERVED] " + caseId + " | " + tab.product + " past list: " + tab.cards.size() + " card(s)"
                + (tab.emptyText == null ? "" : " | empty state: '" + tab.emptyText + "'"));
        for (PastCard c : tab.cards) {
            ReportLogger.info("[OBSERVED]   " + c.label() + " | amount=" + c.amount + " | frequency=" + c.frequency
                    + " | invoice=" + (c.invoiceLine != null));
        }
    }

    /** Scrolls so the Other / Past Subscriptions heading is in the upper part of the screen. */
    private boolean scrollToHeading() {
        Dimension s = driver.manage().window().getSize();
        scrollToTop(s);
        String previous = null;
        for (int i = 0; i < MAX_VIEWPORTS + 2; i++) {
            List<Line> lines = lines();
            Line heading = findHeading(lines);
            if (heading != null && heading.cy < s.height * 0.55) return true;
            String signature = signature(lines);
            if (signature.equals(previous)) return heading != null;
            previous = signature;
            swipeUp(s, heading != null ? 0.3 : 0.5);
        }
        return findHeading(lines()) != null;
    }

    /** Taps the Fund Advisor / Stock Advisor tab under the heading; for Stock Advisor also selects the Past pill. */
    private boolean selectTab(String product) {
        List<Line> lines = lines();
        Line heading = findHeading(lines);
        if (heading == null) return false;
        Line tab = null;
        for (Line l : lines) if (l.cy > heading.cy && norm(l.text).equals(norm(product))) { tab = l; break; }
        if (tab == null) return false;
        ReportLogger.step("Opening the " + product + " tab under " + heading.text);
        tapLine(tab);
        waitForStable();
        for (Line l : lines()) {
            if (norm(l.text).equals("past")) {
                ReportLogger.step("Selecting the Past pill");
                tapLine(l);
                waitForStable();
                break;
            }
        }
        return true;
    }

    /** Reads every past card of the selected tab, scrolling down; cards are merged by their Expired on date. */
    private PastTab collectPastCards(String product) {
        PastTab tab = new PastTab(product);
        Dimension s = driver.manage().window().getSize();
        Map<String, PastCard> byExpiry = new java.util.LinkedHashMap<>();
        String previous = null;
        for (int viewport = 0; viewport < 6; viewport++) {
            List<Line> lines = lines();
            String signature = signature(lines);
            if (signature.equals(previous)) break;
            previous = signature;
            Line heading = findHeading(lines);
            int minY = heading != null ? heading.cy : 0;
            for (Line l : lines) {
                String n = norm(l.text);
                if (l.cy > minY && (n.contains("data is unavailable") || n.contains("no past") || n.contains("no subscription"))) {
                    tab.emptyText = l.text;
                }
                if (l.cy > minY && n.equals("expired on")) {
                    PastCard c = parsePastCard(lines, l, product);
                    String key = String.valueOf(c.expired);
                    PastCard known = byExpiry.get(key);
                    if (known == null || c.completeness() > known.completeness()) byExpiry.put(key, c);
                }
            }
            swipeUp(s, 0.5);
        }
        tab.cards.addAll(byExpiry.values());
        if (!tab.cards.isEmpty()) tab.emptyText = null;
        return tab;
    }

    private PastCard parsePastCard(List<Line> lines, Line expiredLabel, String product) {
        PastCard c = new PastCard(product);
        c.expired = valueOfLine(expiredLabel, lines);
        Line header = null;
        for (Line l : lines) {
            if (l.cy >= expiredLabel.cy - 15 || parseDate(l.text) == null) continue;
            boolean otherCardBetween = false; // a Frequency row in between means the date belongs to the previous card
            for (Line f : lines) {
                if (norm(f.text).equals("frequency") && f.cy > l.cy && f.cy < expiredLabel.cy) { otherCardBetween = true; break; }
            }
            if (!otherCardBetween && (header == null || l.cy > header.cy)) header = l;
        }
        if (header != null) {
            c.purchase = header.text;
            for (Line l : lines) {
                String n = norm(l.text);
                if ((n.equals("invoice") || n.equals("invoices")) && Math.abs(l.cy - header.cy) <= 45) c.invoiceLine = l;
            }
        }
        c.amount = valueOfLine(nearestBelow(lines, "Amount", expiredLabel), lines);
        c.frequency = valueOfLine(nearestBelow(lines, "Frequency", expiredLabel), lines);
        return c;
    }

    private void waitForStable() {
        final String[] last = {""};
        try {
            new WebDriverWait(driver, ExecutionProfile.shortWait()).pollingEvery(ExecutionProfile.pollingInterval())
                    .until(d -> {
                        String signature = signature(lines());
                        boolean same = signature.equals(last[0]);
                        last[0] = signature;
                        return same;
                    });
        } catch (TimeoutException stillChanging) {
            ReportLogger.debug("Screen kept changing after a tab tap; reading it anyway");
        }
    }

    // ================================================================================================
    // Invoice list
    // ================================================================================================

    public boolean openInvoiceFromCard(String product) {
        Card c = null;
        for (Card candidate : freshTopCards()) if (candidate.product.equals(product)) c = candidate;
        Line invoice = c == null ? null : c.line("Invoice", "Invoices");
        expect(invoice != null, product + " card has an 'Invoice' CTA to open", "p.2 / p.3",
                "Invoice link on the " + product + " card", c == null ? "(card not found)" : c.summary());
        if (invoice == null) return false;
        ReportLogger.step("Tapping Invoice on the " + product + " card");
        tapLine(invoice);
        return true;
    }

    public boolean openInvoiceFromPastCard(String product) {
        Dimension screen = driver.manage().window().getSize();
        if (!scrollToHeading()) {
            expect(false, "Past Subscriptions section is reachable", "p.5 / p.6", "Section heading on the page",
                    "Not found. Visible: " + brief(lines()));
            return false;
        }
        List<Line> current = lines();
        Line heading = findHeading(current);
        boolean fundTab = false, stockTab = false;
        for (Line l : current) {
            if (heading != null && l.cy > heading.cy && norm(l.text).equals(norm(FUND))) fundTab = true;
            if (heading != null && l.cy > heading.cy && norm(l.text).equals(norm(STOCK))) stockTab = true;
        }
        if (fundTab && stockTab) selectTab(product);
        for (int i = 0; i < 6; i++) {
            List<Line> lines = lines();
            for (Line l : lines) {
                if (!norm(l.text).equals("expired on") || l.cy > screen.height - 250 || l.cy < screen.height * 0.15) continue;
                PastCard c = parsePastCard(lines, l, product);
                expect(c.invoiceLine != null, c.label() + ": 'Invoice' CTA to open", "p.6 / p.7",
                        "Invoice link on the past card", c.invoiceLine == null ? "(none)" : "present");
                if (c.invoiceLine == null) return false;
                ReportLogger.step("Tapping Invoice on " + c.label());
                tapLine(c.invoiceLine);
                return true;
            }
            swipeUp(screen, 0.4);
        }
        expect(false, product + " past subscription card is reachable", "p.5 / p.6 / p.7", "Past card with Expired on",
                "not found while scrolling the " + product + " list");
        return false;
    }

    public void verifyInvoicePage(String context) {
        List<Line> lines;
        try {
            new WebDriverWait(driver, ExecutionProfile.defaultWait()).pollingEvery(ExecutionProfile.pollingInterval())
                    .until(d -> isInvoicePage());
            lines = lines();
        } catch (TimeoutException notOpened) {
            expect(false, context + ": Invoice page opens", "p.3 / p.4", "Invoice page with Date, Invoice # and Amount columns",
                    "Not detected within " + ExecutionProfile.defaultWait().getSeconds() + "s. Visible: " + brief(lines()));
            return;
        }
        Line dateHeader = null;
        boolean invoiceColumn = false, amountColumn = false;
        for (Line l : lines) {
            String n = norm(l.text);
            if (dateHeader == null && (n.equals("date") || n.startsWith("date "))) dateHeader = l;
            if (n.matches("invoice ?(#|no|number).*")) invoiceColumn = true;
            if (n.equals("amount")) amountColumn = true;
        }
        expect(dateHeader != null && invoiceColumn && amountColumn, context + ": table columns are Date, Invoice # and Amount",
                "p.4", "Date | Invoice # | Amount",
                "Date=" + (dateHeader != null) + ", Invoice #=" + invoiceColumn + ", Amount=" + amountColumn);
        String title = null;
        for (Line l : lines) if (norm(l.text).equals("invoice") || norm(l.text).equals("invoices")) title = l.text;
        note("Invoice page title observed: " + (title == null ? "(none exposed)" : "'" + title + "'") + " (PRD p.4 shows 'Invoice')");
        int headerY = dateHeader == null ? 0 : dateHeader.cy;
        List<Line> rowsDates = new ArrayList<>();
        for (Line l : lines) if (l.cy > headerY && parseDate(l.text) != null) rowsDates.add(l);
        expect(!rowsDates.isEmpty(), context + ": at least one invoice row is listed", "p.3 / p.4",
                "One invoice per billing cycle of the current subscription", rowsDates.size() + " dated row(s)");
        for (int i = 0; i < rowsDates.size(); i++) {
            Line d = rowsDates.get(i);
            String invoiceNo = null, amount = null;
            for (Line l : lines) {
                if (Math.abs(l.cy - d.cy) > 45 || l == d) continue;
                String t = l.text.trim();
                if (AMOUNT.matcher(t).matches()) amount = t;
                else if (parseDate(t) == null && INVOICE_NUMBER.matcher(t).matches() && t.matches(".*\\d.*")) invoiceNo = t;
            }
            ReportLogger.info("[OBSERVED] " + caseId + " | invoice row " + (i + 1) + ": date=" + d.text
                    + " | invoice#=" + invoiceNo + " | amount=" + amount);
            expect(invoiceNo != null && amount != null, context + ": invoice row " + (i + 1) + " has Invoice # and Amount", "p.4",
                    "Date, Invoice # and Amount on every row", "date=" + d.text + ", invoice#=" + invoiceNo + ", amount=" + amount);
        }
        int downloads = driver.findElements(DOWNLOAD_CONTROL).size();
        if (downloads > 0) {
            expect(downloads >= rowsDates.size(), context + ": each invoice row has a download control", "p.3 / p.4",
                    rowsDates.size() + " download control(s)", downloads + " found");
        } else {
            note("MANUAL CHECK: the download icon exposes no accessibility label, so it cannot be asserted; "
                    + "verify the icon and the downloaded PDF manually (PRD p.3 / p.4)");
        }
        note("MANUAL CHECK: invoice count vs plan frequency and that only this product's invoices are listed (PRD p.3) need backend data");
    }

    private boolean isInvoicePage() {
        try {
            if (driver.findElements(BACK_BUTTON).isEmpty()) return false;
            boolean date = false, invoice = false;
            for (Line l : lines()) {
                String n = norm(l.text);
                if (n.equals(norm(PAGE_TITLE))) return false;
                if (n.equals("date") || n.startsWith("date ")) date = true;
                if (n.matches("invoice ?(#|no|number).*")) invoice = true;
            }
            return date && invoice;
        } catch (WebDriverException ignored) {
            return false;
        }
    }

    public void backFromInvoice() {
        ReportLogger.step("Going back from the Invoice page to My Subscriptions");
        returnToMySubscriptions();
    }

    // ================================================================================================
    // CTA navigation (Subscribe now / Renew now)
    // ================================================================================================

    public void verifyCtaOpensSubscriptionPage(String product) {
        Card c = null;
        for (Card candidate : freshTopCards()) if (candidate.product.equals(product)) c = candidate;
        Line cta = c == null ? null : c.cta();
        if (cta == null && c != null && c.statusPill() != null && norm(c.statusPill().text).equals("renew")) {
            cta = c.statusPill(); // active card inside the 1-month window shows "Renew" instead of Active (p.3)
        }
        expect(cta != null, product + " card has a Subscribe now / Renew now / Renew button to tap", "p.3 / p.5",
                "Button on the " + product + " card", c == null ? "(card not found)" : c.summary());
        if (cta == null) return;
        String text = cta.text;
        ReportLogger.step("Tapping '" + text + "' on the " + product + " card");
        tapLine(cta);
        boolean left;
        try {
            new WebDriverWait(driver, ExecutionProfile.defaultWait()).pollingEvery(ExecutionProfile.pollingInterval())
                    .until(d -> !isPageReady());
            left = true;
        } catch (TimeoutException stayed) {
            left = false;
        }
        List<Line> now = left ? lines() : new ArrayList<>();
        expect(left, "Tapping '" + text + "' on the " + product + " card opens the subscription / renewal screen", "p.3 / p.5",
                "Navigates away from My Subscriptions", left ? "Opened screen: " + brief(now) : "Stayed on My Subscriptions");
        if (left) {
            String mine = norm(product), other = norm(FUND.equals(product) ? STOCK : FUND);
            boolean mentionsMine = false, mentionsOther = false;
            for (Line l : now) {
                if (norm(l.text).contains(mine)) mentionsMine = true;
                if (norm(l.text).contains(other)) mentionsOther = true;
            }
            expect(!(mentionsOther && !mentionsMine), "'" + text + "' on the " + product + " card opens the SAME product's page",
                    "p.5", product + " subscription page", "Opened screen: " + brief(now));
            returnToMySubscriptions();
        }
    }

    // ================================================================================================
    // Low-level helpers
    // ================================================================================================

    private List<Line> lines() {
        String source = driver.getPageSource();
        Document doc = Jsoup.parse(source, "", Parser.xmlParser());
        List<Line> out = new ArrayList<>();
        for (Element e : doc.getAllElements()) {
            if (!e.hasAttr("bounds")) continue;
            String desc = e.attr("content-desc");
            String raw = desc.trim().isEmpty() ? e.attr("text") : desc;
            if (raw == null || raw.trim().isEmpty()) continue;
            Box box = parseBounds(e.attr("bounds"));
            if (box == null || box.w <= 0 || box.h <= 0) continue;
            List<String> parts = new ArrayList<>();
            for (String p : raw.split("\\R")) { String t = p.trim(); if (!t.isEmpty()) parts.add(t); }
            if (parts.isEmpty()) continue;
            Node node = new Node(parts, box);
            for (int i = 0; i < parts.size(); i++) out.add(new Line(parts.get(i), node, i));
        }
        out.sort(Comparator.<Line>comparingInt(l -> l.cy).thenComparingInt(l -> l.node.box.x));
        return out;
    }

    private static Box parseBounds(String bounds) {
        java.util.regex.Matcher m = Pattern.compile("\\[(-?\\d+),(-?\\d+)]\\[(-?\\d+),(-?\\d+)]").matcher(bounds == null ? "" : bounds);
        if (!m.find()) return null;
        int x1 = Integer.parseInt(m.group(1)), y1 = Integer.parseInt(m.group(2));
        int x2 = Integer.parseInt(m.group(3)), y2 = Integer.parseInt(m.group(4));
        return new Box(x1, y1, x2 - x1, y2 - y1);
    }

    private static String valueOfLine(Line label, List<Line> pool) {
        if (label == null) return null;
        if (label.node.lines.size() > 1 && label.idx + 1 < label.node.lines.size()) return label.node.lines.get(label.idx + 1);
        Line best = null;
        for (Line o : pool) {
            if (o == label || o.node == label.node) continue;
            if (Math.abs(o.cy - label.cy) > Math.max(30, label.node.box.h)) continue;
            if (o.node.box.x <= label.node.box.x) continue;
            if (best == null || o.node.box.x < best.node.box.x) best = o;
        }
        return best == null ? null : best.text;
    }

    private static Line nearestBelow(List<Line> pool, String key, Line anchor) {
        Line best = null;
        for (Line l : pool) {
            if (norm(l.text).equals(norm(key)) && l.cy >= anchor.cy - 5 && (best == null || l.cy < best.cy)) best = l;
        }
        return best;
    }

    private static boolean containsText(List<Line> lines, String text) {
        for (Line l : lines) if (norm(l.text).equals(norm(text))) return true;
        return false;
    }

    private void tapLine(Line line) {
        Box b = line.node.box;
        boolean composite = line.node.lines.size() > 1;
        // A merged multi-line node cannot be tapped on one line; aim at the right edge where CTAs such as Invoice sit.
        tapXY(composite ? b.x + (int) (b.w * 0.9) : b.cx(), composite ? line.cy : b.cy());
    }

    private void tapXY(int x, int y) {
        driver.executeScript("mobile: clickGesture", Map.of("x", x, "y", y));
    }

    private void swipeUp(Dimension s, double percent) {
        swipe(s, "up", percent);
    }

    private void swipeDown(Dimension s, double percent) {
        swipe(s, "down", percent);
    }

    private void swipe(Dimension s, String direction, double percent) {
        driver.executeScript("mobile: swipeGesture", Map.of("left", s.width / 10, "top", s.height / 4,
                "width", s.width * 8 / 10, "height", s.height / 2, "direction", direction, "percent", percent));
    }

    private void scrollToTop(Dimension s) {
        for (int i = 0; i < MAX_VIEWPORTS + 1; i++) {
            List<Line> lines = lines();
            for (Line l : lines) {
                if (norm(l.text).equals(norm(PAGE_TITLE)) && l.node.box.y >= s.height * 0.08) return;
            }
            String before = signature(lines);
            swipeDown(s, 0.8);
            if (signature(lines()).equals(before)) return;
        }
    }

    private static String signature(List<Line> lines) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < Math.min(lines.size(), 40); i++) sb.append(lines.get(i).text).append('@').append(lines.get(i).cy).append(';');
        return sb.toString();
    }

    private static String brief(List<Line> lines) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < Math.min(lines.size(), 40); i++) {
            if (i > 0) sb.append(" | ");
            sb.append(lines.get(i).text.replace('\n', ' '));
        }
        return trim(sb.append("]").toString(), 700);
    }

    private static LocalDate parseDate(String value) {
        if (value == null) return null;
        String t = value.trim().replace("Sept", "Sep");
        for (DateTimeFormatter f : DATE_FORMATS) {
            try { return LocalDate.parse(t, f); } catch (DateTimeParseException ignored) { /* try next format */ }
        }
        return null;
    }

    private static String norm(String s) {
        return s == null ? "" : s.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9₹# ]", "").replaceAll("\\s+", " ").trim();
    }

    private static String trim(String s, int max) {
        if (s == null) return "null";
        return s.length() <= max ? s : s.substring(0, max) + "...";
    }
}