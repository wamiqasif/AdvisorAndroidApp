package api;

import api.model.FundItem;
import com.google.gson.JsonArray;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Client for the Fund Review API.
 *
 * Expected response shape:
 * {
 *   "data": [
 *     {
 *       "label_id": 1234,
 *       "plan_data": { "plan_id": 5678, "name": "Kotak Small Cap Dir-G" },
 *       "classification_type": "EXIT",
 *       "advice_data": "Switch to a recommended fund...",
 *       "action_data": "See Alternatives",
 *       "lock_in_state": false,
 *       "reason": "Consistent underperformance."
 *     }
 *   ]
 * }
 *
 * Configure fund.review.api.* in api-config.properties.
 */
public class FundReviewApiService {

    private static final Logger logger = LoggerFactory.getLogger(FundReviewApiService.class);

    private final ApiConfigReader config;
    private final HttpClient      httpClient;
    private List<FundItem>        cachedFunds;

    public FundReviewApiService() {
        this.config = ApiConfigReader.getInstance();
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(config.getTimeout()))
                .build();
    }

    // ----------------------------------------------------------------
    // Public API
    // ----------------------------------------------------------------

    /** Returns count of funds per classification_type (e.g. {"EXIT":1, "GOOD":11, "STEADY":4}). */
    public Map<String, Integer> getCategoryCounts() {
        Map<String, Integer> counts = new HashMap<>();
        for (FundItem fund : getAllFunds()) {
            counts.merge(fund.classificationType, 1, Integer::sum);
        }
        return Collections.unmodifiableMap(counts);
    }

    /** Returns all funds grouped by classification_type. */
    public Map<String, List<FundItem>> getFundsByCategory() {
        Map<String, List<FundItem>> map = new HashMap<>();
        for (FundItem fund : getAllFunds()) {
            map.computeIfAbsent(fund.classificationType, k -> new ArrayList<>()).add(fund);
        }
        // Wrap inner lists as unmodifiable
        Map<String, List<FundItem>> result = new HashMap<>();
        map.forEach((k, v) -> result.put(k, Collections.unmodifiableList(v)));
        return Collections.unmodifiableMap(result);
    }

    /** Returns the fund matching the given planId, or null if not found. */
    public FundItem getFundByPlanId(int planId) {
        return getAllFunds().stream()
                .filter(f -> f.planId == planId)
                .findFirst()
                .orElse(null);
    }

    /** Returns the fund matching the given name (case-insensitive), or null if not found. */
    public FundItem getFundByName(String fundName) {
        return getAllFunds().stream()
                .filter(f -> f.fundName.equalsIgnoreCase(fundName))
                .findFirst()
                .orElse(null);
    }

    /** Returns all funds (cached after first call). */
    public List<FundItem> getAllFunds() {
        if (cachedFunds == null) {
            cachedFunds = fetchFromApi();
        }
        return cachedFunds;
    }

    /** Clears the cache and forces a fresh API call on next access. */
    public void invalidateCache() {
        cachedFunds = null;
    }

    // ----------------------------------------------------------------
    // HTTP + JSON
    // ----------------------------------------------------------------

    private List<FundItem> fetchFromApi() {
        String url = config.getBaseUrl() + config.getEndpoint()
                + "?label_id=" + config.getLabelId();
        logger.info("Fetching Fund Review API: {}", url);

        try {
            HttpRequest.Builder reqBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(config.getTimeout()))
                    .GET();

            String token = config.getAuthToken();
            if (!token.isBlank()) {
                reqBuilder.header("Authorization", "Bearer " + token);
            }
            reqBuilder.header("Accept", "application/json");

            HttpResponse<String> response = httpClient.send(
                    reqBuilder.build(), HttpResponse.BodyHandlers.ofString());

            logger.info("Fund Review API responded: HTTP {}", response.statusCode());

            if (response.statusCode() != 200) {
                logger.error("Unexpected status {}: {}", response.statusCode(), response.body());
                return Collections.emptyList();
            }

            return parseResponse(response.body());

        } catch (Exception e) {
            logger.error("Fund Review API call failed: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    private List<FundItem> parseResponse(String json) {
        List<FundItem> funds = new ArrayList<>();
        try {
            JsonObject root = JsonParser.parseString(json).getAsJsonObject();

            // Support both {"data": [...]} and {"data": {"fund_reviews": [...]}}
            JsonElement dataEl = root.get("data");
            if (dataEl == null) {
                logger.warn("API response has no 'data' field");
                return funds;
            }

            JsonArray array;
            if (dataEl.isJsonArray()) {
                array = dataEl.getAsJsonArray();
            } else if (dataEl.isJsonObject()) {
                JsonElement inner = dataEl.getAsJsonObject().get("fund_reviews");
                if (inner == null || !inner.isJsonArray()) {
                    logger.warn("API response 'data' has no 'fund_reviews' array");
                    return funds;
                }
                array = inner.getAsJsonArray();
            } else {
                logger.warn("Unexpected 'data' shape in API response");
                return funds;
            }

            for (JsonElement el : array) {
                JsonObject obj = el.getAsJsonObject();
                funds.add(parseFundItem(obj));
            }

            logger.info("Parsed {} fund items from API", funds.size());

        } catch (Exception e) {
            logger.error("Failed to parse Fund Review API response: {}", e.getMessage(), e);
        }
        return funds;
    }

    private FundItem parseFundItem(JsonObject obj) {
        int     labelId    = getInt(obj, "label_id");
        String  classType  = getString(obj, "classification_type");
        String  adviceData = getString(obj, "advice_data");
        String  actionData = getString(obj, "action_data");
        boolean lockIn     = getBoolean(obj, "lock_in_state");
        String  reason     = getString(obj, "reason");

        int    planId   = 0;
        String fundName = "";
        if (obj.has("plan_data") && obj.get("plan_data").isJsonObject()) {
            JsonObject planData = obj.getAsJsonObject("plan_data");
            planId   = getInt(planData, "plan_id");
            fundName = getString(planData, "name");
        }

        return new FundItem(labelId, planId, fundName, classType, adviceData, actionData, lockIn, reason);
    }

    // ----------------------------------------------------------------
    // JSON helpers
    // ----------------------------------------------------------------

    private String getString(JsonObject obj, String key) {
        JsonElement el = obj.get(key);
        return (el != null && !el.isJsonNull()) ? el.getAsString() : "";
    }

    private int getInt(JsonObject obj, String key) {
        JsonElement el = obj.get(key);
        return (el != null && !el.isJsonNull()) ? el.getAsInt() : 0;
    }

    private boolean getBoolean(JsonObject obj, String key) {
        JsonElement el = obj.get(key);
        return (el != null && !el.isJsonNull()) && el.getAsBoolean();
    }
}
