package com.valueresearch.utils;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Locale;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Fetches the KYC OTP from a mailbox that can be different from the
 * Advisor login OTP mailbox.
 *
 * The implementation uses reflection so it works with either javax.mail
 * or jakarta.mail, whichever is already present in the project.
 */
public final class KycOtpEmailReader {

    private static final Pattern CONTEXTUAL_OTP_PATTERN = Pattern.compile(
            "(?i)(?:otp|one[\\s-]*time\\s+password|verification\\s+code|security\\s+code|code)"
                    + "[^0-9]{0,60}([0-9]{6})(?![0-9])"
    );

    private static final Pattern GENERIC_SIX_DIGIT_PATTERN = Pattern.compile(
            "(?<![0-9])([0-9]{6})(?![0-9])"
    );

    private KycOtpEmailReader() {
    }

    public static String fetchLatestKycOtp() {
        String email = requiredConfigOrEnvironment("kycOtpEmail", "KYC_OTP_EMAIL");
        String appPassword = requiredConfigOrEnvironment(
                "kycOtpEmailAppPassword",
                "KYC_OTP_EMAIL_APP_PASSWORD"
        );

        String host = ConfigReader.getOptional("kycOtpImapHost", "imap.gmail.com").trim();
        int port = parseInt(ConfigReader.getOptional("kycOtpImapPort", "993"), 993);
        int timeoutSeconds = parseInt(ConfigReader.getOptional("kycOtpTimeoutSeconds", "120"), 120);
        int recentMessageLimit = parseInt(ConfigReader.getOptional("kycOtpRecentMessageLimit", "30"), 30);
        String subjectKeywords = ConfigReader.getOptional(
                "kycOtpEmailSubjectKeyword",
                "OTP|One Time Password|verification"
        ).trim();
        String senderKeyword = ConfigReader.getOptional("kycOtpSenderKeyword", "").trim();

        long fetchStartedAt = System.currentTimeMillis();
        long notBefore = fetchStartedAt - 45_000L;
        long endAt = fetchStartedAt + (timeoutSeconds * 1000L);

        ReportLogger.step("Connecting to separate KYC OTP email inbox: " + maskEmail(email));

        String mailPackage = detectMailPackage();
        ReportLogger.step("KYC OTP reader is using " + mailPackage + ".mail");

        String lastSeenSubject = "";

        while (System.currentTimeMillis() < endAt) {
            MailSearchResult result = searchMailboxOnce(
                    mailPackage,
                    host,
                    port,
                    email,
                    appPassword,
                    subjectKeywords,
                    senderKeyword,
                    notBefore,
                    recentMessageLimit
            );

            if (result.otp != null && result.otp.matches("\\d{6}")) {
                ReportLogger.step("Recent KYC OTP email found. Subject: " + safeSubject(result.subject));
                ReportLogger.pass("KYC OTP fetched successfully from separate email inbox");
                return result.otp;
            }

            if (result.subject != null && !result.subject.isBlank()) {
                lastSeenSubject = result.subject;
            }

            ReportLogger.step("KYC OTP email not found yet. Waiting before retry.");
            sleep(5000);
        }

        throw new AssertionError(
                "KYC OTP email was not found within " + timeoutSeconds + " seconds"
                        + (lastSeenSubject.isBlank() ? "" : " | latestCheckedSubject=" + safeSubject(lastSeenSubject))
                        + ". Check kycOtpEmail, app password, subject/sender filters, and IMAP access."
        );
    }

    private static MailSearchResult searchMailboxOnce(
            String mailPackage,
            String host,
            int port,
            String email,
            String appPassword,
            String subjectKeywords,
            String senderKeyword,
            long notBefore,
            int recentMessageLimit
    ) {
        Object store = null;
        Object folder = null;

        try {
            Properties properties = new Properties();
            properties.put("mail.store.protocol", "imaps");
            properties.put("mail.imaps.host", host);
            properties.put("mail.imaps.port", String.valueOf(port));
            properties.put("mail.imaps.ssl.enable", "true");
            properties.put("mail.imaps.connectiontimeout", "15000");
            properties.put("mail.imaps.timeout", "15000");
            properties.put("mail.imaps.writetimeout", "15000");

            Class<?> sessionClass = Class.forName(mailPackage + ".mail.Session");
            Object session = sessionClass
                    .getMethod("getInstance", Properties.class)
                    .invoke(null, properties);

            store = sessionClass.getMethod("getStore", String.class).invoke(session, "imaps");
            invoke(store, "connect", new Class<?>[]{String.class, int.class, String.class, String.class},
                    host, port, email, appPassword);

            folder = invoke(store, "getFolder", new Class<?>[]{String.class}, "INBOX");

            Class<?> folderClass = Class.forName(mailPackage + ".mail.Folder");
            Field readOnlyField = folderClass.getField("READ_ONLY");
            int readOnly = readOnlyField.getInt(null);
            invoke(folder, "open", new Class<?>[]{int.class}, readOnly);

            int count = (Integer) invoke(folder, "getMessageCount", new Class<?>[]{});
            if (count <= 0) {
                return MailSearchResult.empty();
            }

            int start = Math.max(1, count - Math.max(1, recentMessageLimit) + 1);
            Object messageArray = invoke(folder, "getMessages",
                    new Class<?>[]{int.class, int.class}, start, count);

            String latestCheckedSubject = "";

            for (int index = Array.getLength(messageArray) - 1; index >= 0; index--) {
                Object message = Array.get(messageArray, index);
                String subject = stringValue(invokeQuietly(message, "getSubject"));
                latestCheckedSubject = subject;

                Date receivedDate = dateValue(invokeQuietly(message, "getReceivedDate"));
                Date sentDate = dateValue(invokeQuietly(message, "getSentDate"));
                Date effectiveDate = receivedDate != null ? receivedDate : sentDate;

                if (effectiveDate != null && effectiveDate.getTime() < notBefore) {
                    continue;
                }

                if (!matchesAnyKeyword(subject, subjectKeywords)) {
                    continue;
                }

                String fromText = arrayToString(invokeQuietly(message, "getFrom"));
                if (!senderKeyword.isBlank()
                        && !fromText.toLowerCase(Locale.ENGLISH)
                        .contains(senderKeyword.toLowerCase(Locale.ENGLISH))) {
                    continue;
                }

                String body = readMailPart(message);
                String searchable = (subject == null ? "" : subject) + "\n" + body;
                String otp = extractOtp(searchable);

                if (otp != null) {
                    return new MailSearchResult(otp, subject);
                }
            }

            return new MailSearchResult(null, latestCheckedSubject);

        } catch (AssertionError e) {
            throw e;
        } catch (Exception e) {
            throw new AssertionError(
                    "Unable to read KYC OTP email inbox: " + clean(rootMessage(e)),
                    e
            );
        } finally {
            closeFolderQuietly(folder);
            closeStoreQuietly(store);
        }
    }

    private static String detectMailPackage() {
        try {
            Class.forName("jakarta.mail.Session");
            return "jakarta";
        } catch (ClassNotFoundException ignored) {
            // Try javax.mail below.
        }

        try {
            Class.forName("javax.mail.Session");
            return "javax";
        } catch (ClassNotFoundException ignored) {
            throw new AssertionError(
                    "No JavaMail library found. The existing login OTP reader requires either "
                            + "jakarta.mail or javax.mail on the test classpath."
            );
        }
    }

    private static String readMailPart(Object part) throws Exception {
        Object content = invokeQuietly(part, "getContent");
        if (content == null) {
            return "";
        }

        if (content instanceof String) {
            return (String) content;
        }

        if (content instanceof InputStream) {
            return readInputStream((InputStream) content);
        }

        Method getCount = findMethod(content.getClass(), "getCount");
        Method getBodyPart = findMethod(content.getClass(), "getBodyPart", int.class);

        if (getCount != null && getBodyPart != null) {
            int count = ((Number) getCount.invoke(content)).intValue();
            StringBuilder body = new StringBuilder();

            for (int i = 0; i < count; i++) {
                Object bodyPart = getBodyPart.invoke(content, i);
                body.append(readMailPart(bodyPart)).append('\n');
            }

            return body.toString();
        }

        return String.valueOf(content);
    }

    private static String readInputStream(InputStream inputStream) throws Exception {
        try (InputStream in = inputStream;
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[4096];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
            return out.toString(StandardCharsets.UTF_8);
        }
    }

    private static String extractOtp(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }

        Matcher contextual = CONTEXTUAL_OTP_PATTERN.matcher(text);
        if (contextual.find()) {
            return contextual.group(1);
        }

        Matcher generic = GENERIC_SIX_DIGIT_PATTERN.matcher(text);
        if (generic.find()) {
            return generic.group(1);
        }

        return null;
    }

    private static boolean matchesAnyKeyword(String subject, String configuredKeywords) {
        if (configuredKeywords == null || configuredKeywords.isBlank()) {
            return true;
        }

        String safeSubject = subject == null ? "" : subject.toLowerCase(Locale.ENGLISH);
        for (String keyword : configuredKeywords.split("\\|")) {
            String trimmed = keyword.trim().toLowerCase(Locale.ENGLISH);
            if (!trimmed.isEmpty() && safeSubject.contains(trimmed)) {
                return true;
            }
        }
        return false;
    }

    private static String requiredConfigOrEnvironment(String configKey, String environmentKey) {
        String environmentValue = System.getenv(environmentKey);
        if (environmentValue != null && !environmentValue.trim().isEmpty()) {
            return environmentValue.trim();
        }

        String value = ConfigReader.getOptional(configKey, "").trim();
        if (value.isEmpty()) {
            throw new AssertionError(
                    "Missing KYC OTP mailbox setting: " + configKey
                            + ". Add it to config.properties or set environment variable "
                            + environmentKey + "."
            );
        }
        return value;
    }

    private static Object invoke(Object target, String methodName, Class<?>[] parameterTypes, Object... args)
            throws Exception {
        Method method = target.getClass().getMethod(methodName, parameterTypes);
        return method.invoke(target, args);
    }

    private static Object invokeQuietly(Object target, String methodName) {
        if (target == null) {
            return null;
        }
        try {
            return target.getClass().getMethod(methodName).invoke(target);
        } catch (Exception ignored) {
            return null;
        }
    }

    private static Method findMethod(Class<?> type, String name, Class<?>... parameterTypes) {
        try {
            return type.getMethod(name, parameterTypes);
        } catch (Exception e) {
            return null;
        }
    }

    private static void closeFolderQuietly(Object folder) {
        if (folder == null) {
            return;
        }
        try {
            Object open = invokeQuietly(folder, "isOpen");
            if (Boolean.TRUE.equals(open)) {
                invoke(folder, "close", new Class<?>[]{boolean.class}, false);
            }
        } catch (Exception ignored) {
            // Best-effort close.
        }
    }

    private static void closeStoreQuietly(Object store) {
        if (store == null) {
            return;
        }
        try {
            invoke(store, "close", new Class<?>[]{});
        } catch (Exception ignored) {
            // Best-effort close.
        }
    }

    private static String arrayToString(Object array) {
        if (array == null || !array.getClass().isArray()) {
            return String.valueOf(array == null ? "" : array);
        }

        StringBuilder result = new StringBuilder();
        for (int i = 0; i < Array.getLength(array); i++) {
            if (i > 0) {
                result.append(' ');
            }
            result.append(String.valueOf(Array.get(array, i)));
        }
        return result.toString();
    }

    private static Date dateValue(Object value) {
        return value instanceof Date ? (Date) value : null;
    }

    private static String stringValue(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private static String safeSubject(String subject) {
        if (subject == null || subject.isBlank()) {
            return "(no subject)";
        }
        return clean(subject);
    }

    private static String maskEmail(String email) {
        if (email == null || !email.contains("@")) {
            return "configured KYC inbox";
        }

        String[] parts = email.split("@", 2);
        String user = parts[0];
        String domain = parts[1];
        if (user.length() <= 2) {
            return "**@" + domain;
        }
        return user.charAt(0) + "***" + user.charAt(user.length() - 1) + "@" + domain;
    }

    private static int parseInt(String value, int defaultValue) {
        try {
            return Integer.parseInt(value.trim());
        } catch (Exception e) {
            return defaultValue;
        }
    }

    private static String rootMessage(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null) {
            current = current.getCause();
        }
        return current.getMessage() == null ? current.toString() : current.getMessage();
    }

    private static String clean(String value) {
        return value == null ? "" : value.replaceAll("\\s+", " ").trim();
    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AssertionError("KYC OTP email polling was interrupted", e);
        }
    }

    private static final class MailSearchResult {
        private final String otp;
        private final String subject;

        private MailSearchResult(String otp, String subject) {
            this.otp = otp;
            this.subject = subject == null ? "" : subject;
        }

        private static MailSearchResult empty() {
            return new MailSearchResult(null, "");
        }
    }
}