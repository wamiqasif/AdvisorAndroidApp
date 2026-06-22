package tests;

import api.AlternativesApiService;
import api.AlternativesApiService.AlternativesApiResponse;
import api.FundReviewDataService;
import api.model.AlternativeFund;
import api.model.FundReviewItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.SkipException;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.List;

/**
 * API-only Alternatives validation for EXIT and OPTIMIZE funds.
 *
 * Data flow:
 *  Step 1. Call Fund Review API through FundReviewDataService.
 *  Step 2. Filter EXIT and OPTIMIZE funds.
 *  Step 3. Call Alternatives API using label-ids + plan-id.
 *  Step 4. Validate recommendation business rules from data.alternatives[].
 */
public class AlternativesApiValidationTest {

    private static final Logger logger = LoggerFactory.getLogger(AlternativesApiValidationTest.class);

    private FundReviewDataService fundReviewDataService;
    private AlternativesApiService alternativesApiService;
    private List<FundReviewItem> sourceFunds;

    @BeforeClass
    public void setUpApiClients() {
        fundReviewDataService = new FundReviewDataService();
        alternativesApiService = new AlternativesApiService();
        sourceFunds = fundReviewDataService.getFunds()
                .stream()
                .filter(this::isAlternativesSourceFund)
                .toList();

        if (sourceFunds.isEmpty()) {
            throw new SkipException("Fund Review API returned no EXIT or OPTIMIZE funds");
        }
    }

    @DataProvider(name = "sourceFunds")
    public Object[][] sourceFunds() {
        return sourceFunds.stream()
                .map(fund -> new Object[]{fund})
                .toArray(Object[][]::new);
    }

    @Test(
            dataProvider = "sourceFunds",
            description = "TC_ALT_001 - Alternatives API business rules for EXIT and OPTIMIZE funds")
    public void tc_alt_001_validateAlternativesBusinessRules(FundReviewItem sourceFund) {
        logger.info(
                "SOURCE_CATEGORY={} | SOURCE_PLAN_ID={} | SOURCE_FUND_NAME={}",
                sourceFund.actualCategory,
                sourceFund.planId,
                sourceFund.fundName);

        AlternativesApiResponse response = alternativesApiService.getAlternativesResponse(sourceFund.planId);
        assertHttp200(response, sourceFund);

        List<AlternativeFund> alternatives = response.alternatives;
        Assert.assertFalse(
                alternatives.isEmpty(),
                failureContext(sourceFund, response) + " | Alternatives list must not be empty");
        Assert.assertTrue(
                alternatives.size() <= 5,
                failureContext(sourceFund, response) + " | Alternatives count must be <= 5");

        for (AlternativeFund alternative : alternatives) {
            Assert.assertNotEquals(
                    alternative.planId,
                    sourceFund.planId,
                    failureContext(sourceFund, response) + " | Source fund must not appear in alternatives");
        }

        long checkedCount = alternatives.stream()
                .filter(AlternativeFund::isChecked)
                .count();
        Assert.assertEquals(
                checkedCount,
                1,
                failureContext(sourceFund, response) + " | Exactly one alternative must have is_checked=true");

        AlternativeFund recommendedFund = alternatives.stream()
                .filter(AlternativeFund::isChecked)
                .findFirst()
                .orElse(null);

        Assert.assertNotNull(
                recommendedFund,
                failureContext(sourceFund, response) + " | Recommended alternative must exist");
        Assert.assertTrue(
                recommendedFund.planId > 0,
                failureContext(sourceFund, response) + " | Recommended alternative planId must be > 0");
        Assert.assertFalse(
                recommendedFund.fundName.isBlank(),
                failureContext(sourceFund, response) + " | Recommended alternative fundName must not be blank");

        logger.info(
                "SOURCE_CATEGORY={} | SOURCE_PLAN_ID={} | SOURCE_FUND_NAME={} | RECOMMENDED_PLAN_ID={} | RECOMMENDED_FUND_NAME={}",
                sourceFund.actualCategory,
                sourceFund.planId,
                sourceFund.fundName,
                recommendedFund.planId,
                recommendedFund.fundName);
    }

    private boolean isAlternativesSourceFund(FundReviewItem fund) {
        return "EXIT".equalsIgnoreCase(fund.actualCategory)
                || "OPTIMIZE".equalsIgnoreCase(fund.actualCategory);
    }

    private void assertHttp200(AlternativesApiResponse response, FundReviewItem sourceFund) {
        if (response.statusCode == 500) {
            String message = failureContext(sourceFund, response) + " | Alternatives API returned HTTP 500";
            logger.error(message);
            Assert.fail(message);
        }

        Assert.assertEquals(
                response.statusCode,
                200,
                failureContext(sourceFund, response) + " | Alternatives API must return HTTP 200");
    }

    private String context(FundReviewItem sourceFund) {
        return "SOURCE_CATEGORY=" + sourceFund.actualCategory
                + " | SOURCE_PLAN_ID=" + sourceFund.planId
                + " | SOURCE_FUND_NAME=" + sourceFund.fundName;
    }

    private String failureContext(FundReviewItem sourceFund, AlternativesApiResponse response) {
        return context(sourceFund)
                + "\nHTTP_STATUS=" + response.statusCode
                + "\nRESPONSE_BODY=" + response.responseBody;
    }
}
