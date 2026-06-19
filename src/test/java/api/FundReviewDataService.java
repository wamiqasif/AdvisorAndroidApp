package api;

import api.model.FundReviewItem;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Client for the Fund Review API.
 *
 * Endpoint: GET {baseUrl}/api/v1/advisory/fund-review?label-ids={labelId}
 *
 * Reads ONLY: plan_data.plan_id, plan_data.name, classification_type
 *
 * classification_type is stored as-is for comparison purposes.
 * It is NEVER used as input to FundReviewCategoryCalculator.
 *
 * NOTE: This class is distinct from FundReviewApiService (previous task).
 *       It uses the correct advisory endpoint and a lean FundReviewItem model.
 *
 * Actual response shape:
 *   {
 *     "data": {
 *       "classification_data": {
 *         "OPTIMIZE": { "data": [ { "plan_data": { "plan_id": 116, "name": "..." }, "classification_type": "OPTIMIZE", ... } ] },
 *         "EXIT":     { "data": [] },
 *         "GOOD":     { "data": [] },
 *         "STEADY":   { "data": [] },
 *         "NEW-FUND": { "data": [] }
 *       }
 *     }
 *   }
 *
 * Fallback shapes also handled:
 *   Shape B: { "data": [ { "plan_data": {...}, "classification_type": "..." } ] }
 *   Shape C: { "data": { "fund_reviews": [ ... ] } }
 */
public class FundReviewDataService {

    private static final Logger logger = LoggerFactory.getLogger(FundReviewDataService.class);

    private final AdvisoryApiConfig config;
    private final HttpClient        http;
    private List<FundReviewItem>    cache;

    public FundReviewDataService() {
        this.config = AdvisoryApiConfig.getInstance();
        this.http   = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(config.timeoutSeconds()))
                .build();
    }

    // ----------------------------------------------------------------
    // Public API
    // ----------------------------------------------------------------

    /**
     * Returns all funds from Fund Review API (cached after first call).
     */
    public List<FundReviewItem> getFunds() {
        if (cache == null) cache = fetchFromApi();
        return Collections.unmodifiableList(cache);
    }

    /**
     * Returns the fund matching planId, or null if not found.
     */
    public FundReviewItem getFundByPlanId(int planId) {
        return getFunds().stream()
                .filter(f -> f.planId == planId)
                .findFirst()
                .orElse(null);
    }

    /** Clears the cache, forcing a fresh API call on next access. */
    public void invalidateCache() { cache = null; }

    // ----------------------------------------------------------------
    // Internal
    // ----------------------------------------------------------------

    private List<FundReviewItem> fetchFromApi() {
        String url = config.baseUrl() + config.fundReviewEndpoint()
                + "?label-ids=" + config.fundReviewLabelId();
        logger.info("Calling Fund Review API: {}", url);

        try {
            HttpRequest.Builder rb = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(config.timeoutSeconds()))
                    .GET()
                    .header("Accept", "application/json");
            String token = config.authToken();
            if (!token.isBlank()) rb.header("Authorization", "Bearer " + token);

            HttpResponse<String> resp = http.send(rb.build(), HttpResponse.BodyHandlers.ofString());
            logger.info("Fund Review API → HTTP {}", resp.statusCode());

            if (resp.statusCode() != 200) {
                logger.error("Unexpected status {}: {}", resp.statusCode(), resp.body());
                return Collections.emptyList();
            }
            return parse(resp.body());

        } catch (Exception e) {
            logger.error("Fund Review API call failed: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    private List<FundReviewItem> parse(String json) {
        List<FundReviewItem> items = new ArrayList<>();
        try {
            JsonObject root    = JsonParser.parseString(json).getAsJsonObject();
            JsonElement dataEl = root.get("data");
            if (dataEl == null) {
                logger.warn("Fund Review API: 'data' field missing in response");
                return items;
            }

            if (dataEl.isJsonObject()) {
                JsonObject data = dataEl.getAsJsonObject();

                // Primary shape: data.classification_data.{CATEGORY}.data[...]
                if (data.has("classification_data") && data.get("classification_data").isJsonObject()) {
                    JsonObject classData = data.getAsJsonObject("classification_data");
                    for (Map.Entry<String, JsonElement> catEntry : classData.entrySet()) {
                        if (!catEntry.getValue().isJsonObject()) continue;
                        JsonElement inner = catEntry.getValue().getAsJsonObject().get("data");
                        if (inner == null || !inner.isJsonArray()) continue;
                        for (JsonElement el : inner.getAsJsonArray()) {
                            parseFundItem(el, items);
                        }
                    }
                }
                // Fallback: data.fund_reviews[...]
                else if (data.has("fund_reviews") && data.get("fund_reviews").isJsonArray()) {
                    for (JsonElement el : data.getAsJsonArray("fund_reviews")) {
                        parseFundItem(el, items);
                    }
                } else {
                    logger.warn("Fund Review API: unrecognised object shape inside 'data'");
                }

            } else if (dataEl.isJsonArray()) {
                // Fallback: data is a flat array
                for (JsonElement el : dataEl.getAsJsonArray()) {
                    parseFundItem(el, items);
                }
            } else {
                logger.warn("Fund Review API: 'data' field is neither object nor array");
            }

            logger.info("Fund Review API: parsed {} records", items.size());
        } catch (Exception e) {
            logger.error("Fund Review API parse error: {}", e.getMessage(), e);
        }
        return items;
    }
    private void parseFundItem(JsonElement el, List<FundReviewItem> items) {

        if (!el.isJsonObject()) {
            return;
        }

        JsonObject obj = el.getAsJsonObject();

        int planId = 0;
        String fundName = "";

        if (obj.has("plan_data") && obj.get("plan_data").isJsonObject()) {

            JsonObject pd = obj.getAsJsonObject("plan_data");

            planId = intOf(pd, "plan_id");
            fundName = strOf(pd, "name");
        }

        String actualCategory = strOf(obj, "classification_type");

        // NEW FIELD
        String concentrationTrigger =
                strOf(obj, "concentration_trigger");

        if (planId > 0) {

            items.add(
                    new FundReviewItem(
                            planId,
                            fundName,
                            actualCategory,
                            concentrationTrigger
                    )
            );
        }
    }
   

    private String strOf(JsonObject o, String k) {
        JsonElement e = o.get(k);
        return (e != null && !e.isJsonNull()) ? e.getAsString() : "";
    }

    private int intOf(JsonObject o, String k) {
        JsonElement e = o.get(k);
        return (e != null && !e.isJsonNull()) ? e.getAsInt() : 0;
    }
}
