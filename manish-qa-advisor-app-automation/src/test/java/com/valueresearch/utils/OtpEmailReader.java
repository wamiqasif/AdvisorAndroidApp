package com.valueresearch.utils;

import org.jsoup.Jsoup;

import javax.mail.BodyPart;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import java.util.Date;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class OtpEmailReader {

    private static final long DEFAULT_TIMEOUT_MILLIS = 90_000L;
    private static final long POLL_INTERVAL_MILLIS = 5_000L;
    private static final long MAX_EMAIL_AGE_MILLIS = 180_000L;

    private OtpEmailReader() {
        // Utility class
    }

    public static String fetchLatestOtp() {
        String email = getRequiredConfigOrEnv("otpEmail", "OTP_EMAIL");
        String password = getRequiredConfigOrEnv("otpEmailAppPassword", "OTP_EMAIL_APP_PASSWORD");

        /*
         * Gmail app passwords are often copied with spaces.
         * Remove spaces safely before IMAP login.
         */
        password = password.replace(" ", "").trim();

        String subjectKeyword = getOptionalConfigOrEnv(
                "otpEmailSubjectKeyword",
                "OTP_EMAIL_SUBJECT_KEYWORD",
                "OTP"
        );

        return fetchLatestOtp(email, password, subjectKeyword, DEFAULT_TIMEOUT_MILLIS);
    }

    public static String fetchLatestOtp(
            String email,
            String password,
            String subjectKeyword,
            long timeoutMillis
    ) {
        Store store = null;
        Folder inbox = null;

        try {
            String host = resolveImapHost(email);

            ReportLogger.step("Connecting to OTP email inbox using IMAP");

            Properties props = new Properties();
            props.put("mail.store.protocol", "imaps");
            props.put("mail.imaps.host", host);
            props.put("mail.imaps.port", "993");
            props.put("mail.imaps.ssl.enable", "true");
            props.put("mail.imaps.ssl.trust", host);
            props.put("mail.imaps.connectiontimeout", "15000");
            props.put("mail.imaps.timeout", "15000");
            props.put("mail.imaps.writetimeout", "15000");

            Session session = Session.getInstance(props);
            session.setDebug(false);

            store = session.getStore("imaps");
            store.connect(host, email, password);

            inbox = store.getFolder("INBOX");
            inbox.open(Folder.READ_ONLY);

            ReportLogger.pass("Connected to OTP email inbox successfully");

            long startTime = System.currentTimeMillis();

            while (System.currentTimeMillis() - startTime < timeoutMillis) {
                String otp = searchLatestOtpInInbox(inbox, subjectKeyword);

                if (otp != null) {
                    ReportLogger.pass("OTP fetched successfully from email");
                    return otp;
                }

                ReportLogger.step("OTP email not found yet. Waiting before retry.");
                sleep(POLL_INTERVAL_MILLIS);
            }

            ReportLogger.fail("OTP email not received within timeout");
            throw new RuntimeException("OTP email not received within timeout.");

        } catch (RuntimeException e) {
            ReportLogger.fail("OTP email fetch failed: " + e.getMessage());
            throw e;
        } catch (Exception e) {
            ReportLogger.fail("OTP email fetch failed unexpectedly: " + e.getMessage());
            throw new RuntimeException("OTP email fetch failed unexpectedly: " + e.getMessage(), e);
        } finally {
            closeInboxSafely(inbox);
            closeStoreSafely(store);
        }
    }

    private static String searchLatestOtpInInbox(Folder inbox, String subjectKeyword) throws Exception {
        int messageCount = inbox.getMessageCount();

        if (messageCount <= 0) {
            return null;
        }

        int startIndex = Math.max(1, messageCount - 30);
        Message[] messages = inbox.getMessages(startIndex, messageCount);

        Date now = new Date();

        for (int i = messages.length - 1; i >= 0; i--) {
            MimeMessage message = (MimeMessage) messages[i];

            Date receivedDate = message.getReceivedDate();

            if (receivedDate == null) {
                continue;
            }

            long ageMillis = now.getTime() - receivedDate.getTime();

            if (ageMillis > MAX_EMAIL_AGE_MILLIS) {
                continue;
            }

            String subject = message.getSubject();

            if (!isOtpSubject(subject, subjectKeyword)) {
                continue;
            }

            ReportLogger.step("Recent OTP email found. Subject: " + safeText(subject));

            String body = getTextFromMessage(message);
            String otp = extractSixDigitOtp(body);

            if (otp != null) {
                return otp;
            }

            ReportLogger.step("OTP email found but 6-digit OTP was not present in body.");
        }

        return null;
    }

    private static boolean isOtpSubject(String subject, String subjectKeyword) {
        if (subject == null || subject.trim().isEmpty()) {
            return false;
        }

        String lowerSubject = subject.toLowerCase();
        String lowerKeyword = subjectKeyword == null ? "otp" : subjectKeyword.toLowerCase();

        return lowerSubject.contains(lowerKeyword)
                || lowerSubject.contains("otp")
                || lowerSubject.contains("one time password")
                || lowerSubject.contains("verification code");
    }

    private static String extractSixDigitOtp(String body) {
        if (body == null || body.trim().isEmpty()) {
            return null;
        }

        Matcher matcher = Pattern.compile("\\b\\d{6}\\b").matcher(body);

        if (matcher.find()) {
            return matcher.group();
        }

        return null;
    }

    private static String getTextFromMessage(Message message) throws Exception {
        if (message.isMimeType("text/plain")) {
            return String.valueOf(message.getContent());
        }

        if (message.isMimeType("text/html")) {
            return Jsoup.parse(String.valueOf(message.getContent())).text();
        }

        if (message.isMimeType("multipart/*")) {
            return getTextFromMimeMultipart((MimeMultipart) message.getContent());
        }

        return "";
    }

    private static String getTextFromMimeMultipart(MimeMultipart mimeMultipart) throws Exception {
        StringBuilder result = new StringBuilder();

        int count = mimeMultipart.getCount();

        for (int i = 0; i < count; i++) {
            BodyPart bodyPart = mimeMultipart.getBodyPart(i);

            if (bodyPart.isMimeType("text/plain")) {
                result.append(bodyPart.getContent());
            } else if (bodyPart.isMimeType("text/html")) {
                result.append(Jsoup.parse(String.valueOf(bodyPart.getContent())).text());
            } else if (bodyPart.getContent() instanceof MimeMultipart) {
                result.append(getTextFromMimeMultipart((MimeMultipart) bodyPart.getContent()));
            }
        }

        return result.toString();
    }

    private static String resolveImapHost(String email) {
        if (email == null || email.trim().isEmpty()) {
            throw new RuntimeException("OTP email is missing.");
        }

        String lowerEmail = email.toLowerCase();

        if (lowerEmail.endsWith("@gmail.com")) {
            return "imap.gmail.com";
        }

        if (lowerEmail.endsWith("@outlook.com")
                || lowerEmail.endsWith("@hotmail.com")
                || lowerEmail.endsWith("@live.com")) {
            return "outlook.office365.com";
        }

        if (lowerEmail.endsWith("@valueresearchonline.com")
                || lowerEmail.endsWith("@valueresearch.in")) {
            return "imap.gmail.com";
        }

        throw new RuntimeException("Unsupported OTP email provider: " + email);
    }

    private static String getRequiredConfigOrEnv(String configKey, String envKey) {
        String value = System.getenv(envKey);

        if (value == null || value.trim().isEmpty()) {
            value = ConfigReader.get(configKey);
        }

        if (value == null || value.trim().isEmpty()) {
            throw new RuntimeException(
                    "Missing required OTP config. Set environment variable "
                            + envKey
                            + " or config key "
                            + configKey
            );
        }

        return value.trim();
    }

    private static String getOptionalConfigOrEnv(String configKey, String envKey, String defaultValue) {
        String value = System.getenv(envKey);

        if (value == null || value.trim().isEmpty()) {
            try {
                value = ConfigReader.get(configKey);
            } catch (Exception ignored) {
                // Use default
            }
        }

        if (value == null || value.trim().isEmpty()) {
            return defaultValue;
        }

        return value.trim();
    }

    private static void closeInboxSafely(Folder inbox) {
        try {
            if (inbox != null && inbox.isOpen()) {
                inbox.close(false);
            }
        } catch (Exception ignored) {
            // Ignore cleanup failure
        }
    }

    private static void closeStoreSafely(Store store) {
        try {
            if (store != null && store.isConnected()) {
                store.close();
            }
        } catch (Exception ignored) {
            // Ignore cleanup failure
        }
    }

    private static String safeText(String text) {
        if (text == null) {
            return "";
        }

        return text.replace("\n", " ").replace("\r", " ").trim();
    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("OTP email polling interrupted", e);
        }
    }
}