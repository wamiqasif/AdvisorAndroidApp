package com.valueresearch.tests;

import com.aventstack.extentreports.ExtentTest;
import com.valueresearch.base.BaseTest;
import com.valueresearch.pages.StoriesVideosPage;
import com.valueresearch.utils.AuthHelper;
import com.valueresearch.utils.ExtentManager;
import com.valueresearch.utils.ExtentTestManager;
import com.valueresearch.utils.ReportLogger;
import org.testng.annotations.Test;

public class StoriesVideosTest extends BaseTest {

    private static boolean storiesVideosOpened = false;

    @Test(priority = 1)
    public void SV_001_OpenStoriesAndVideosFromHub() {
        createExtentTest(
                "SV_001",
                "Open Stories & Videos from Hub",
                "Open Hub tab, locate Mutual Funds section, tap Stories & Videos, and verify module opens"
        );

        ReportLogger.step("Starting test case: SV_001 - Open Stories & Videos from Hub");

        new AuthHelper(driver).ensureLoggedIn();
        ReportLogger.pass("Advisor login/session confirmed");

        StoriesVideosPage page = new StoriesVideosPage(driver);
        page.openStoriesAndVideosFromHub();
        storiesVideosOpened = true;

        markPassed("SV_001 - Stories & Videos opened successfully");
    }

    @Test(priority = 2, dependsOnMethods = "SV_001_OpenStoriesAndVideosFromHub")
    public void SV_002_VerifyLandingStaticContent() {
        createExtentTest(
                "SV_002",
                "Verify Stories & Videos landing static content",
                "Validate Stories & Videos title, Fund Advisor's Note chip, and Quick Guides chip"
        );

        ReportLogger.step("Starting test case: SV_002 - Verify landing static content");
        getStoriesVideosPage().verifyStoriesVideosLandingStaticContent();
        markPassed("SV_002 - Landing static content validated successfully");
    }

    @Test(priority = 3, dependsOnMethods = "SV_001_OpenStoriesAndVideosFromHub")
    public void SV_003_VerifyFundAdvisorNotesStoryList() {
        createExtentTest(
                "SV_003",
                "Verify Fund Advisor's Note story list",
                "Validate visible story titles, duration labels, and list content under Fund Advisor's Note"
        );

        ReportLogger.step("Starting test case: SV_003 - Verify Fund Advisor's Note story list");
        getStoriesVideosPage().verifyFundAdvisorNotesStoryList();
        markPassed("SV_003 - Fund Advisor's Note story list validated successfully");
    }

    @Test(priority = 4, dependsOnMethods = "SV_001_OpenStoriesAndVideosFromHub")
    public void SV_004_VerifyOpenFirstStoryDetail() {
        createExtentTest(
                "SV_004",
                "Verify first story opens detail page",
                "Tap first Fund Advisor's Note story and verify detail screen opens"
        );

        ReportLogger.step("Starting test case: SV_004 - Verify first story opens detail page");
        getStoriesVideosPage().verifyOpenFirstStoryDetail();
        markPassed("SV_004 - First story detail opened successfully");
    }

    @Test(priority = 5, dependsOnMethods = "SV_001_OpenStoriesAndVideosFromHub")
    public void SV_005_VerifyFirstStoryDetailTopContent() {
        createExtentTest(
                "SV_005",
                "Verify first story detail top content",
                "Validate story title, category, and opening body text on story detail screen"
        );

        ReportLogger.step("Starting test case: SV_005 - Verify first story detail top content");
        getStoriesVideosPage().verifyFirstStoryDetailTopContent();
        markPassed("SV_005 - First story detail top content validated successfully");
    }

    @Test(priority = 6, dependsOnMethods = "SV_001_OpenStoriesAndVideosFromHub")
    public void SV_006_VerifyFirstStoryDetailScrollContent() {
        createExtentTest(
                "SV_006",
                "Verify first story detail scroll content",
                "Scroll first story detail and validate lower body/bottom text markers are reachable"
        );

        ReportLogger.step("Starting test case: SV_006 - Verify first story detail scroll content");
        getStoriesVideosPage().verifyFirstStoryDetailScrollContent();
        markPassed("SV_006 - First story detail scroll content validated successfully");
    }

    @Test(priority = 7, dependsOnMethods = "SV_001_OpenStoriesAndVideosFromHub")
    public void SV_007_VerifyBackFromStoryDetailToList() {
        createExtentTest(
                "SV_007",
                "Verify back from story detail to list",
                "Validate back navigation from story detail returns to Stories & Videos list"
        );

        ReportLogger.step("Starting test case: SV_007 - Verify back from story detail to list");
        getStoriesVideosPage().verifyBackFromStoryDetailToList();
        markPassed("SV_007 - Back from story detail to list validated successfully");
    }

    @Test(priority = 8, dependsOnMethods = "SV_001_OpenStoriesAndVideosFromHub")
    public void SV_008_VerifyQuickGuidesTabSwitch() {
        createExtentTest(
                "SV_008",
                "Verify Quick Guides tab switch",
                "Tap Quick Guides chip and validate videos list loads"
        );

        ReportLogger.step("Starting test case: SV_008 - Verify Quick Guides tab switch");
        getStoriesVideosPage().verifyQuickGuidesTabSwitch();
        markPassed("SV_008 - Quick Guides tab switch validated successfully");
    }

    @Test(priority = 9, dependsOnMethods = "SV_001_OpenStoriesAndVideosFromHub")
    public void SV_009_VerifyQuickGuidesVideoList() {
        createExtentTest(
                "SV_009",
                "Verify Quick Guides video list",
                "Validate Quick Guides visible video titles and duration labels"
        );

        ReportLogger.step("Starting test case: SV_009 - Verify Quick Guides video list");
        getStoriesVideosPage().verifyQuickGuidesVideoList();
        markPassed("SV_009 - Quick Guides video list validated successfully");
    }

    @Test(priority = 10, dependsOnMethods = "SV_001_OpenStoriesAndVideosFromHub")
    public void SV_010_VerifyOpenFirstQuickGuideVideo() {
        createExtentTest(
                "SV_010",
                "Verify first Quick Guides video opens",
                "Tap first Quick Guides video and validate video/player/loading screen response"
        );

        ReportLogger.step("Starting test case: SV_010 - Verify first Quick Guides video opens");
        getStoriesVideosPage().verifyOpenFirstQuickGuideVideo();
        markPassed("SV_010 - First Quick Guides video tap validated successfully");
    }

    @Test(priority = 11, dependsOnMethods = "SV_001_OpenStoriesAndVideosFromHub")
    public void SV_011_VerifyBackFromVideoToQuickGuidesList() {
        createExtentTest(
                "SV_011",
                "Verify back from video to Quick Guides list",
                "Validate back navigation from video/player/loading screen returns to Quick Guides list"
        );

        ReportLogger.step("Starting test case: SV_011 - Verify back from video to Quick Guides list");
        getStoriesVideosPage().verifyBackFromVideoToQuickGuidesList();
        markPassed("SV_011 - Back from video to Quick Guides list validated successfully");
    }

    @Test(priority = 12, dependsOnMethods = "SV_001_OpenStoriesAndVideosFromHub")
    public void SV_012_VerifySelectedTabRetapDoesNotNavigateAway() {
        createExtentTest(
                "SV_012",
                "Negative: verify selected tab re-tap does not navigate away",
                "Re-tap selected Fund Advisor's Note tab and validate list remains active without opening story detail"
        );

        ReportLogger.step("Starting test case: SV_012 - Negative selected tab re-tap validation");
        getStoriesVideosPage().verifySelectedTabRetapDoesNotNavigateAway();
        markPassed("SV_012 - Selected tab re-tap negative validation passed");
    }

    @Test(priority = 13, dependsOnMethods = "SV_001_OpenStoriesAndVideosFromHub")
    public void SV_013_VerifyRapidTabSwitchDoesNotCrash() {
        createExtentTest(
                "SV_013",
                "Verify rapid tab switching stability",
                "Rapidly switch between Fund Advisor's Note and Quick Guides and validate no crash/ANR"
        );

        ReportLogger.step("Starting test case: SV_013 - Verify rapid tab switching stability");
        getStoriesVideosPage().verifyRapidTabSwitchDoesNotCrash();
        markPassed("SV_013 - Rapid tab switching stability validated successfully");
    }

    @Test(priority = 14, dependsOnMethods = "SV_001_OpenStoriesAndVideosFromHub")
    public void SV_014_VerifyStoriesVideosScreenStability() {
        createExtentTest(
                "SV_014",
                "Verify Stories & Videos screen stability",
                "Validate Stories & Videos screen does not show crash or ANR markers"
        );

        ReportLogger.step("Starting test case: SV_014 - Verify Stories & Videos screen stability");
        getStoriesVideosPage().verifyStoriesVideosScreenStability();
        markPassed("SV_014 - Stories & Videos screen stability validated successfully");
    }

    @Test(priority = 15, dependsOnMethods = "SV_001_OpenStoriesAndVideosFromHub")
    public void SV_015_VerifyStoryInternalHyperlinksPresent() {
        createExtentTest(
                "SV_015",
                "Verify story internal hyperlinks present",
                "Open first Fund Advisor's Note story, scroll story body, and validate internal hyperlinks like Portfolio Planner and Analyst's Choice are present"
        );

        ReportLogger.step("Starting test case: SV_015 - Verify story internal hyperlinks present");
        getStoriesVideosPage().verifyStoryInternalHyperlinksPresent();
        markPassed("SV_015 - Story internal hyperlinks validated successfully");
    }


    @Test(priority = 16, dependsOnMethods = "SV_001_OpenStoriesAndVideosFromHub")
    public void SV_016_VerifyStoryInternalHyperlinksOpen() {
        createExtentTest(
                "SV_016",
                "Verify story internal hyperlinks open",
                "Open Portfolio Planner and Analyst's Choice internal links from the first story and validate navigation response"
        );

        ReportLogger.step("Starting test case: SV_016 - Verify story internal hyperlinks open");
        getStoriesVideosPage().verifyStoryInternalHyperlinksOpen();
        markPassed("SV_016 - Story internal hyperlinks opened successfully");
    }

    @Test(priority = 17, dependsOnMethods = "SV_001_OpenStoriesAndVideosFromHub")
    public void SV_017_VerifyBackNavigationToHub() {
        createExtentTest(
                "SV_017",
                "Verify back navigation to Hub",
                "Validate back navigation from Stories & Videos page returns user to Hub Mutual Funds area"
        );

        ReportLogger.step("Starting test case: SV_017 - Verify back navigation to Hub");
        getStoriesVideosPage().verifyBackNavigationToHub();
        markPassed("SV_017 - Back navigation to Hub validated successfully");
    }

    private StoriesVideosPage getStoriesVideosPage() {
        // Do not auto-recover here. Several tests intentionally start from story detail
        // or video/player screens and must validate a single back action from the current state.
        // Each page method now performs only the recovery it actually needs.
        return new StoriesVideosPage(driver);
    }

    private void createExtentTest(String caseId, String title, String validation) {
        ExtentTest test = ExtentManager.getExtentReports().createTest(caseId + " - " + title);
        ExtentTestManager.setTest(test);

        ExtentTestManager.getTest().info(
                "Module: Stories & Videos<br>"
                        + "Case ID: " + caseId + "<br>"
                        + "Validation: " + validation
        );
    }

    private void markPassed(String message) {
        ExtentTestManager.getTest().pass(
                "<span class='badge white-text green'>" + message + "</span>"
        );
        ReportLogger.pass("Completed test case: " + message);
    }
}