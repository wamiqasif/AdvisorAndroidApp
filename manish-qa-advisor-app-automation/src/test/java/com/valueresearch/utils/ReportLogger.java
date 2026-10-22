package com.valueresearch.utils;

import com.aventstack.extentreports.Status;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

/** Concise execution output; technical diagnostics are opt-in. Existing public APIs are preserved. */
public final class ReportLogger {

    private static final ZoneId IST = ZoneId.of("Asia/Kolkata");
    private static final DateTimeFormatter TIMESTAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    private ReportLogger() {
    }

    public static void step(String message) { log(Status.INFO, "STEP", message); }
    public static void pass(String message) { log(Status.PASS, "PASS", message); }
    public static void fail(String message) { log(Status.FAIL, "FAIL", message); }
    public static void info(String message) { log(Status.INFO, "INFO", message); }

    public static void debug(String message) {
        if (isDebugConsoleEnabled()) {
            console("DEBUG", message);
        }
        if (isDebugReportEnabled()) {
            logToReport(Status.INFO, "<b>[DEBUG]</b> " + escapeHtml(message));
        }
    }

    public static boolean isDebugReportEnabled() {
        return flag("debugReport", "AUTOMATION_DEBUG_REPORT");
    }

    public static boolean isDebugConsoleEnabled() {
        return flag("debugConsole", "AUTOMATION_DEBUG_CONSOLE");
    }

    /** Drop Selenium's diagnostic footer, preserving the actionable error or assertion message. */
    public static String summary(Throwable failure) {
        if (failure == null) {
            return "Unknown failure";
        }
        String message = compact(failure.getMessage());
        return failure.getClass().getSimpleName() + (message.isEmpty() ? "" : ": " + message);
    }

    /** Console-only output for infrastructure/listener messages, with an explicit IST timestamp. */
    public static void console(String level, String message) {
        System.out.println("[" + TIMESTAMP.format(ZonedDateTime.now(IST)) + "] [" + level + "] " + compact(message));
    }

    private static void log(Status status, String level, String message) {
        String concise = compact(message);
        logToReport(status, "<b>[" + level + "]</b> " + escapeHtml(concise));
        console(level, concise);
    }

    private static boolean flag(String property, String environment) {
        String value = System.getProperty(property);
        return Boolean.parseBoolean(value == null ? System.getenv(environment) : value.trim());
    }

    private static String compact(String message) {
        if (message == null) {
            return "";
        }
        StringBuilder result = new StringBuilder();
        for (String line : message.split("\\R")) {
            String trimmed = line.trim();
            if (trimmed.startsWith("Build info:") || trimmed.startsWith("System info:")
                    || trimmed.startsWith("Driver info:") || trimmed.startsWith("Capabilities ")
                    || trimmed.startsWith("Session ID:") || trimmed.startsWith("Command:")) {
                break;
            }
            if (!trimmed.isEmpty()) {
                if (result.length() > 0) {
                    result.append(' ');
                }
                result.append(trimmed);
            }
        }
        return result.toString();
    }

    private static void logToReport(Status status, String message) {
        try {
            if (ExtentTestManager.getTest() != null) {
                ExtentTestManager.getTest().log(status, message);
            }
        } catch (Exception e) {
            console("REPORT_LOG_ERROR", summary(e));
        }
    }

    private static String escapeHtml(String text) {
        return text == null ? "" : text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
