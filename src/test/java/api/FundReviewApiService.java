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
                + "?label-ids=" + config.getLabelId();
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

    private static final String[] KNOWN_CATEGORIES =
            {"GOOD", "STEADY", "OPTIMIZE", "EXIT", "NEW-FUND"};

    private List<FundItem> parseResponse(String json) {
        List<FundItem> funds = new ArrayList<>();
        try {
            JsonObject root = JsonParser.parseString(json).getAsJsonObject();

            JsonElement dataEl = root.get("data");
            if (dataEl == null) {
                logger.warn("API response has no 'data' field");
                return funds;
            }

            // Shape A: { "data": [ ...funds... ] } — flat array, classification_type inside each item
            if (dataEl.isJsonArray()) {
                for (JsonElement el : dataEl.getAsJsonArray()) {
                    if (el.isJsonObject()) funds.add(parseFundItem(el.getAsJsonObject()));
                }
                logger.info("Parsed {} fund items from flat array", funds.size());
                return funds;
            }

            if (dataEl.isJsonObject()) {
                JsonObject dataObj = dataEl.getAsJsonObject();

                // Shape B (current): { "data": { "classification_data": { "GOOD": {"data":[...]}, ... } } }
                JsonElement classDataEl = dataObj.get("classification_data");
                if (classDataEl != null && classDataEl.isJsonObject()) {
                    JsonObject classData = classDataEl.getAsJsonObject();
                    for (String category : KNOWN_CATEGORIES) {
                        JsonElement bucketEl = classData.get(category);
                        if (bucketEl == null || !bucketEl.isJsonObject()) continue;
                        JsonElement bucketDataEl = bucketEl.getAsJsonObject().get("data");
                        if (bucketDataEl == null || !bucketDataEl.isJsonArray()) continue;
                        int count = 0;
                        for (JsonElement el : bucketDataEl.getAsJsonArray()) {
                            if (!el.isJsonObject()) continue;
                            funds.add(parseFundItem(el.getAsJsonObject(), category));
                            count++;
                        }
                        logger.info("Parsed {} funds from category {}", count, category);
                    }
                    return funds;
                }

                // Shape C (legacy): { "data": { "fund_reviews": [ ...funds... ] } }
                JsonElement inner = dataObj.get("fund_reviews");
                if (inner != null && inner.isJsonArray()) {
                    for (JsonElement el : inner.getAsJsonArray()) {
                        if (el.isJsonObject()) funds.add(parseFundItem(el.getAsJsonObject()));
                    }
                    logger.info("Parsed {} fund items from fund_reviews array", funds.size());
                    return funds;
                }

                logger.warn("API response 'data' has no 'classification_data' or 'fund_reviews' — cannot parse");
            } else {
                logger.warn("Unexpected 'data' shape in API response");
            }

        } catch (Exception e) {
            logger.error("Failed to parse Fund Review API response: {}", e.getMessage(), e);
        }
        return funds;
    }

    /**
     * Parses a fund item whose classification_type comes from inside the JSON object.
     * Used for flat-array and fund_reviews shapes.
     */
    private FundItem parseFundItem(JsonObject obj) {
        return parseFundItem(obj, getString(obj, "classification_type"));
    }

    /**
     * Parses a fund item using the provided category (from the bucket key in classification_data).
     * Falls back to whatever classification_type is in the object if category is blank.
     *
     * Handles both old and new API shapes:
     *
     * Old: advice_data=string, action_data=string, lock_in_state=boolean
     * New: advice_data={advice, action_data:{text,...},...}, lock_in_state="ALL_LOCKED"|"ALL_FREE"
     */
    private FundItem parseFundItem(JsonObject obj, String category) {
        String classType = (category != null && !category.isBlank())
                           ? category : getString(obj, "classification_type");

        // label_id: top-level (old) or inside label_data[0] (new)
        int labelId = getInt(obj, "label_id");
        if (labelId == 0 && obj.has("label_data") && obj.get("label_data").isJsonArray()) {
            JsonArray labelArr = obj.getAsJsonArray("label_data");
            if (labelArr.size() > 0 && labelArr.get(0).isJsonObject()) {
                labelId = getInt(labelArr.get(0).getAsJsonObject(), "label_id");
            }
        }

        // advice_data: string (old) or object with "advice" field (new)
        String adviceData;
        String actionData;
        JsonElement adviceEl = obj.get("advice_data");
        if (adviceEl != null && adviceEl.isJsonObject()) {
            JsonObject adviceObj = adviceEl.getAsJsonObject();
            adviceData = getString(adviceObj, "advice");
            JsonElement actionEl = adviceObj.get("action_data");
            actionData = (actionEl != null && actionEl.isJsonObject())
                         ? getString(actionEl.getAsJsonObject(), "text") : "";
        } else {
            adviceData = getString(obj, "advice_data");
            actionData = getString(obj, "action_data");
        }

        // lock_in_state: boolean (old) or string "ALL_LOCKED"/"ALL_FREE"/"PARTIAL_LOCKED" (new)
        boolean lockIn = getLockInState(obj);

        String reason = getString(obj, "reason");

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

    /** Safe string extraction — returns "" for null, JsonNull, JsonObject, or JsonArray. */
    private String getString(JsonObject obj, String key) {
        JsonElement el = obj.get(key);
        if (el == null || el.isJsonNull()) return "";
        if (el.isJsonPrimitive()) return el.getAsString();
        return ""; // JsonObject/JsonArray — not a string
    }

    private int getInt(JsonObject obj, String key) {
        JsonElement el = obj.get(key);
        return (el != null && !el.isJsonNull() && el.isJsonPrimitive()) ? el.getAsInt() : 0;
    }

    private boolean getBoolean(JsonObject obj, String key) {
        JsonElement el = obj.get(key);
        return (el != null && !el.isJsonNull() && el.isJsonPrimitive()) && el.getAsBoolean();
    }

    /**
     * Handles lock_in_state in both shapes:
     *  Old: boolean true/false
     *  New: string "ALL_LOCKED", "PARTIAL_LOCKED" → true; "ALL_FREE" → false
     */
    private boolean getLockInState(JsonObject obj) {
        JsonElement el = obj.get("lock_in_state");
        if (el == null || el.isJsonNull()) return false;
        if (!el.isJsonPrimitive()) return false;
        if (el.getAsJsonPrimitive().isBoolean()) return el.getAsBoolean();
        return el.getAsString().contains("LOCKED");
    }
}
