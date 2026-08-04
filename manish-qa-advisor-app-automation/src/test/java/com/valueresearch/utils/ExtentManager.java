package com.valueresearch.utils;

import com.aventstack.extentreports.ExtentReports;
import com.aventstack.extentreports.reporter.ExtentSparkReporter;
import com.aventstack.extentreports.reporter.configuration.Theme;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

public class ExtentManager {

    private static ExtentReports extent;

    public static ExtentReports getExtentReports() {
        if (extent == null) {
            String timestamp = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss.SSS").format(new Date());

            String reportDir = System.getProperty("user.dir")
                    + File.separator
                    + "test-output"
                    + File.separator
                    + "ExtentReports";

            new File(reportDir).mkdirs();

            /*
             * Common report filename for all modules:
             * SIP Calculator, Tax Calculator, OTP, Login, etc.
             */
            String reportPath = reportDir
                    + File.separator
                    + "AdvisorApp_Automation_Report_"
                    + timestamp
                    + ".html";

            ExtentSparkReporter sparkReporter = new ExtentSparkReporter(reportPath);

            sparkReporter.config().setDocumentTitle("Advisor App Automation Report");
            sparkReporter.config().setReportName("Advisor App Test Automation Report");
            sparkReporter.config().setTheme(Theme.DARK);
            sparkReporter.config().setEncoding("utf-8");
            sparkReporter.config().setTimeStampFormat("MMM dd, yyyy hh:mm:ss a");

            extent = new ExtentReports();
            extent.attachReporter(sparkReporter);

            extent.setSystemInfo("Application", "Advisor App");
            extent.setSystemInfo("Machine", "Local");
            extent.setSystemInfo("OS", System.getProperty("os.name"));
            extent.setSystemInfo("User", System.getProperty("user.name"));
            extent.setSystemInfo("Platform", "Android");
            extent.setSystemInfo("Automation", "Appium UiAutomator2");
            extent.setSystemInfo("Framework", "TestNG + Maven + ExtentReports");
        }

        return extent;
    }
}