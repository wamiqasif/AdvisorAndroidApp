package com.valueresearch.utils;

import org.jsoup.Jsoup;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.support.ui.FluentWait;

import javax.mail.Address;
import javax.mail.BodyPart;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.UIDFolder;
import javax.mail.internet.MimeMultipart;
import java.time.Duration;
import java.util.Date;
import java.util.Locale;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Existing IMAP provider/body parsing, with polling and an additive fresh-login API. */
public final class OtpEmailReader {

    private static final long DEFAULT_TIMEOUT_MILLIS = 90_000L;
    private static final long MAX_EMAIL_AGE_MILLIS = 180_000L;
    private static final Pattern OTP_PATTERN = Pattern.compile("\\b\\d{6}\\b");
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "[a-zA-Z0-9._%+\\-]+@[a-zA-Z0-9.\\-]+\\.[a-zA-Z]{2,}");

    private OtpEmailReader() {
    }

    public static final class LoginRequestMarker {
        private final long uidValidity;
        private final long lastUid;

        private LoginRequestMarker(long uidValidity, long lastUid) {
            this.uidValidity = uidValidity;
            this.lastUid = lastUid;
        }
    }

    /** Validate config without connecting to IMAP; matching-session reuse never calls this. */
    public static void validateBatch1Configuration() {
        String email = batch1ConfigEmail();
        if (!EMAIL_PATTERN.matcher(email).matches()) {
            throw new IllegalArgumentException("Invalid batch1OtpEmail / BATCH1_OTP_EMAIL");
        }
        resolveImapHost(email);
        if (batch1ConfigPassword().isEmpty()) {
            throw new IllegalArgumentException("Empty Batch 1 mailbox app password");
        }
        batch1ConfigSubject();
        positiveLong("batch1OtpTimeoutSeconds", 90L);
        positiveLong("batch1OtpPollingMillis", 2000L);
    }

    /** Batch 1 mailbox only. Capture before Next; earlier UIDs cannot satisfy the new login. */
    public static LoginRequestMarker captureLoginRequestMarker() {
        try (Mailbox mailbox = Mailbox.open(batch1ConfigEmail(), batch1ConfigPassword())) {
            UIDFolder uidFolder = requireUidFolder(mailbox.inbox);
            int count = mailbox.inbox.getMessageCount();
            long lastUid = count == 0 ? 0L : uidFolder.getUID(mailbox.inbox.getMessage(count));
            if (lastUid < 0L) {
                throw new IllegalStateException("The mailbox did not expose a valid UID");
            }
            return new LoginRequestMarker(uidFolder.getUIDValidity(), lastUid);
        } catch (MessagingException e) {
            throw new RuntimeException("Could not capture the login OTP mailbox checkpoint", e);
        }
    }

    /** New persona-aware entry point. Legacy overloads below remain available to existing modules. */
    public static String fetchLatestOtpAfter(LoginRequestMarker marker, String expectedRecipient) {
        if (marker == null || expectedRecipient == null || !EMAIL_PATTERN.matcher(expectedRecipient).matches()) {
            throw new IllegalArgumentException("A mailbox checkpoint and valid login email are required");
        }
        long timeoutMillis = positiveLong("batch1OtpTimeoutSeconds", 90L) * 1000L;
        return fetch(batch1ConfigEmail(), batch1ConfigPassword(), batch1ConfigSubject(),
                timeoutMillis, marker, expectedRecipient);
    }

    public static String fetchLatestOtp() {
        return fetchLatestOtp(configEmail(), configPassword(), configSubject(), DEFAULT_TIMEOUT_MILLIS);
    }

    public static String fetchLatestOtp(String email, String password, String subjectKeyword, long timeoutMillis) {
        return fetch(email, password, subjectKeyword, timeoutMillis, null, null);
    }

    private static String fetch(String email, String password, String keyword, long timeoutMillis,
            LoginRequestMarker marker, String recipient) {
        if (timeoutMillis <= 0L) {
            throw new IllegalArgumentException("OTP timeout must be positive");
        }
        try (Mailbox mailbox = Mailbox.open(email, password)) {
            if (marker != null && requireUidFolder(mailbox.inbox).getUIDValidity() != marker.uidValidity) {
                throw new IllegalStateException("Mailbox UID validity changed; request a new login OTP");
            }
            ReportLogger.step("Polling the OTP mailbox for " + (marker == null ? "a recent" : "a new") + " login email");
            FluentWait<Folder> wait = new FluentWait<>(mailbox.inbox)
                    .withTimeout(Duration.ofMillis(timeoutMillis))
                    .pollingEvery(Duration.ofMillis(marker == null ? 5000L
                            : positiveLong("batch1OtpPollingMillis", 2000L)))
                    .withMessage("No matching OTP email arrived within the configured timeout");
            String otp = wait.until(folder -> {
                try {
                    return findOtp(folder, keyword, marker, recipient, email);
                } catch (MessagingException e) {
                    throw new RuntimeException("OTP mailbox read failed", e);
                }
            });
            ReportLogger.pass("OTP fetched successfully; OTP value is not logged");
            return otp;
        } catch (TimeoutException e) {
            throw new RuntimeException("No matching OTP arrived. Check the mailbox, recipient and login subject", e);
        } catch (MessagingException e) {
            throw new RuntimeException("OTP mailbox connection failed", e);
        }
    }

    private static String findOtp(Folder inbox, String keyword, LoginRequestMarker marker,
            String recipient, String mailboxEmail) throws MessagingException {
        Message[] messages;
        if (marker != null) {
            messages = requireUidFolder(inbox).getMessagesByUID(marker.lastUid + 1L, UIDFolder.LASTUID);
        } else {
            int count = inbox.getMessageCount();
            if (count == 0) {
                return null;
            }
            messages = inbox.getMessages(Math.max(1, count - 30), count);
        }
        for (int i = messages.length - 1; i >= 0; i--) {
            Message message = messages[i];
            if (message == null || message.isExpunged()) {
                continue;
            }
            // Some IMAP servers return an existing boundary message for an empty UID range.
            if (marker != null && requireUidFolder(inbox).getUID(message) <= marker.lastUid) {
                continue;
            }
            Date received = message.getReceivedDate();
            if (received == null || System.currentTimeMillis() - received.getTime() > MAX_EMAIL_AGE_MILLIS) {
                continue;
            }
            String subject = message.getSubject();
            if (!isOtpSubject(subject, keyword, marker != null)) {
                continue;
            }
            if (recipient != null && !matchesRecipient(message, recipient, mailboxEmail)) {
                continue;
            }
            String body;
            try {
                body = textFromMessage(message);
            } catch (java.io.IOException e) {
                throw new RuntimeException("Could not decode OTP email", e);
            }
            Matcher matcher = OTP_PATTERN.matcher(body);
            if (matcher.find()) {
                return matcher.group();
            }
        }
        return null;
    }

    private static boolean isOtpSubject(String subject, String keyword, boolean strict) {
        if (subject == null) {
            return false;
        }
        String lower = subject.toLowerCase(Locale.ROOT);
        String required = keyword == null || keyword.trim().isEmpty() ? "otp" : keyword.toLowerCase(Locale.ROOT);
        if (strict) {
            return lower.contains(required);
        }
        // Preserve the original broad subject matching for legacy callers.
        return lower.contains(required) || lower.contains("otp")
                || lower.contains("one time password") || lower.contains("verification code");
    }

    private static boolean matchesRecipient(Message message, String expected, String mailbox)
            throws MessagingException {
        StringBuilder headers = new StringBuilder();
        Address[] recipients = message.getAllRecipients();
        if (recipients != null) {
            for (Address address : recipients) {
                headers.append(address.toString()).append(' ');
            }
        }
        for (String name : new String[]{"Delivered-To", "X-Original-To", "Envelope-To", "X-Forwarded-To"}) {
            String[] values = message.getHeader(name);
            if (values != null) {
                for (String value : values) {
                    headers.append(value).append(' ');
                }
            }
        }
        return matchesRecipientHeaders(headers.toString(), expected, mailbox);
    }

    private static boolean matchesRecipientHeaders(String headers, String expected, String mailbox) {
        String target = expected.toLowerCase(Locale.ROOT);
        String base = mailbox.toLowerCase(Locale.ROOT);
        String localPart = base.substring(0, base.indexOf('@'));
        String domain = base.substring(base.indexOf('@'));
        boolean targetSeen = false;
        boolean conflictingAlias = false;
        boolean baseSeen = false;
        Matcher matcher = EMAIL_PATTERN.matcher(headers);
        while (matcher.find()) {
            String address = matcher.group().toLowerCase(Locale.ROOT);
            targetSeen |= address.equals(target);
            baseSeen |= address.equals(base);
            conflictingAlias |= address.startsWith(localPart + "+") && address.endsWith(domain)
                    && !address.equals(target);
        }
        // A forwarded email may explicitly target the base inbox. The UID checkpoint still enforces freshness.
        // Reject conflicting persona aliases even if Delivered-To also names the base inbox.
        return !conflictingAlias && (targetSeen || baseSeen);
    }

    private static String textFromMessage(javax.mail.Part part) throws MessagingException, java.io.IOException {
        if (part.isMimeType("text/plain")) {
            return String.valueOf(part.getContent());
        }
        if (part.isMimeType("text/html")) {
            return Jsoup.parse(String.valueOf(part.getContent())).text();
        }
        if (part.isMimeType("multipart/*")) {
            MimeMultipart multipart = (MimeMultipart) part.getContent();
            StringBuilder text = new StringBuilder();
            for (int i = 0; i < multipart.getCount(); i++) {
                BodyPart child = multipart.getBodyPart(i);
                if (!javax.mail.Part.ATTACHMENT.equalsIgnoreCase(child.getDisposition())) {
                    text.append(textFromMessage(child)).append('\n');
                }
            }
            return text.toString();
        }
        return "";
    }

    private static UIDFolder requireUidFolder(Folder inbox) {
        if (!(inbox instanceof UIDFolder)) {
            throw new IllegalStateException("Fresh OTP retrieval requires an IMAP mailbox with UID support");
        }
        return (UIDFolder) inbox;
    }

    private static String resolveImapHost(String email) {
        String lower = email.toLowerCase(Locale.ROOT);
        if (lower.endsWith("@gmail.com") || lower.endsWith("@valueresearchonline.com")
                || lower.endsWith("@valueresearch.in")) {
            return "imap.gmail.com";
        }
        if (lower.endsWith("@outlook.com") || lower.endsWith("@hotmail.com") || lower.endsWith("@live.com")) {
            return "outlook.office365.com";
        }
        throw new IllegalArgumentException("Unsupported OTP email provider");
    }

    private static String configEmail() {
        return configOrEnv("otpEmail", "OTP_EMAIL", null);
    }

    private static String configPassword() {
        return configOrEnv("otpEmailAppPassword", "OTP_EMAIL_APP_PASSWORD", null).replace(" ", "");
    }

    private static String configSubject() {
        return configOrEnv("otpEmailSubjectKeyword", "OTP_EMAIL_SUBJECT_KEYWORD", "OTP");
    }

    // Deliberately do not fall back to the legacy OTP_EMAIL* settings.
    // Existing regression callers keep using configEmail/configPassword/configSubject above.
    private static String batch1ConfigEmail() {
        return configOrEnv("batch1OtpEmail", "BATCH1_OTP_EMAIL", null);
    }

    private static String batch1ConfigPassword() {
        return configOrEnv("batch1OtpEmailAppPassword", "BATCH1_OTP_EMAIL_APP_PASSWORD", null)
                .replace(" ", "");
    }

    private static String batch1ConfigSubject() {
        return configOrEnv("batch1OtpEmailSubjectKeyword", "BATCH1_OTP_EMAIL_SUBJECT_KEYWORD",
                "One Time Password for Login");
    }

    private static String configOrEnv(String key, String environmentKey, String fallback) {
        String value = System.getenv(environmentKey);
        if (value == null || value.trim().isEmpty()) {
            value = ConfigReader.getOptional(key, fallback);
        }
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("Missing OTP setting: " + environmentKey + " / " + key
                    + ". Set the environment variable in the Eclipse TestNG Run Configuration or the property "
                    + "in src/test/resources/config.properties before starting an email/OTP login");
        }
        return value.trim();
    }

    private static long positiveLong(String key, long fallback) {
        long value = Long.parseLong(ConfigReader.getOptional(key, String.valueOf(fallback)));
        if (value <= 0L) {
            throw new IllegalArgumentException(key + " must be positive");
        }
        return value;
    }

    private static final class Mailbox implements AutoCloseable {
        private final Store store;
        private final Folder inbox;

        private Mailbox(Store store, Folder inbox) {
            this.store = store;
            this.inbox = inbox;
        }

        private static Mailbox open(String email, String password) throws MessagingException {
            String host = resolveImapHost(email);
            Properties properties = new Properties();
            properties.put("mail.store.protocol", "imaps");
            properties.put("mail.imaps.host", host);
            properties.put("mail.imaps.port", "993");
            properties.put("mail.imaps.ssl.enable", "true");
            properties.put("mail.imaps.ssl.checkserveridentity", "true");
            properties.put("mail.imaps.connectiontimeout", "15000");
            properties.put("mail.imaps.timeout", "15000");
            properties.put("mail.imaps.writetimeout", "15000");
            Store store = Session.getInstance(properties).getStore("imaps");
            Folder inbox = null;
            try {
                store.connect(host, email, password.replace(" ", "").trim());
                inbox = store.getFolder("INBOX");
                inbox.open(Folder.READ_ONLY);
                return new Mailbox(store, inbox);
            } catch (MessagingException | RuntimeException e) {
                new Mailbox(store, inbox).close();
                throw e;
            }
        }

        @Override
        public void close() {
            try {
                if (inbox != null && inbox.isOpen()) {
                    inbox.close(false);
                }
            } catch (MessagingException ignored) {
                // Best-effort cleanup; retain the original failure.
            }
            try {
                if (store != null && store.isConnected()) {
                    store.close();
                }
            } catch (MessagingException ignored) {
                // Best-effort cleanup; retain the original failure.
            }
        }
    }
}
