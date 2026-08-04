package com.valueresearch.utils;

import com.aventstack.extentreports.Status;

import java.text.SimpleDateFormat;
import java.util.Date;

public class ReportLogger {

    /*
     * Default behavior:
     * STEP / PASS / FAIL / INFO -> Extent report + terminal
     * DEBUG -> terminal only
     *
     * If debug logs are needed inside Extent report, run:
     * mvn clean test -DsuiteXmlFile=tax.xml -DdebugReport=true
     */
    private static final boolean DEBUG_IN_REPORT =
            Boolean.parseBoolean(System.getProperty("debugReport", "false"));

    private ReportLogger() {
        // Utility class
    }

    public static void step(String message) {
        String formattedMessage = "<b>[STEP]</b> " + escapeHtml(message);

        logToReport(Status.INFO, formattedMessage);
        logToConsole("STEP", message);
    }

    public static void pass(String message) {
        String formattedMessage = "<b>[PASS]</b> " + escapeHtml(message);

        logToReport(Status.PASS, formattedMessage);
        logToConsole("PASS", message);
    }

    public static void fail(String message) {
        String formattedMessage = "<b>[FAIL]</b> " + escapeHtml(message);

        logToReport(Status.FAIL, formattedMessage);
        logToConsole("FAIL", message);
    }

    public static void info(String message) {
        String formattedMessage = escapeHtml(message);

        logToReport(Status.INFO, formattedMessage);
        logToConsole("INFO", message);
    }

    public static void debug(String message) {
        /*
         * Keep technical locator/input debug in terminal only.
         * This keeps Extent report clean.
         */
        logToConsole("DEBUG", message);

        if (DEBUG_IN_REPORT) {
            String formattedMessage = "<b>[DEBUG]</b> " + escapeHtml(message);
            logToReport(Status.INFO, formattedMessage);
        }
    }

    private static void logToReport(Status status, String message) {
        try {
            if (ExtentTestManager.getTest() != null) {
                ExtentTestManager.getTest().log(status, message);
            }
        } catch (Exception e) {
            logToConsole("REPORT_LOG_ERROR", e.getMessage());
        }
    }

    private static void logToConsole(String level, String message) {
        String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(new Date());
        System.out.println("[" + timestamp + "] [" + level + "] " + message);
    }

    private static String escapeHtml(String text) {
        if (text == null) {
            return "";
        }

        return text
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }
}