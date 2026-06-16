package api;

import api.model.FundOpinionItem;
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

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Client for the Fund Opinion API — the ONLY source of truth for category calculation.
 *
 * Endpoint: GET {baseUrl}/api/v1/funds/fund-opinion-data/get-list?plan_id={csv}
 *
 * Reads: plan_id, is_provisional, opinion_name, provisional_opinion_name
 *
 * Expected response shapes (both handled):
 *   Shape A: { "data": [ { "plan_id": 5678, "opinion_name": "Good", ... }, ... ] }
 *   Shape B: { "data": { "results": [ ... ] } }
 */
public class FundOpinionApiService {

    private static final Logger logger = LoggerFactory.getLogger(FundOpinionApiService.class);

    private final AdvisoryApiConfig     config;
    private final HttpClient            http;
    private final Map<Integer, FundOpinionItem> cache = new HashMap<>();

    public FundOpinionApiService() {
        this.config = AdvisoryApiConfig.getInstance();
        this.http   = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(config.timeoutSeconds()))
                .build();
    }

    // ----------------------------------------------------------------
    // Public API
    // ----------------------------------------------------------------

    /**
     * Fetches opinion data for a list of plan IDs.
     * Returns a map of planId → FundOpinionItem.
     * Plan IDs not found in the API response are absent from the map.
     */
    public Map<Integer, FundOpinionItem> getOpinionsByPlanIds(List<Integer> planIds) {
        if (planIds == null || planIds.isEmpty()) return Collections.emptyMap();

        // Identify which IDs are not yet cached
        List<Integer> missing = planIds.stream()
                .filter(id -> !cache.containsKey(id))
                .distinct()
                .collect(Collectors.toList());

        if (!missing.isEmpty()) {
            Map<Integer, FundOpinionItem> fetched = fetchFromApi(missing);
            cache.putAll(fetched);
            // Record explicit nulls for plan IDs that were requested but not returned
            missing.forEach(id -> cache.putIfAbsent(id, null));
        }

        Map<Integer, FundOpinionItem> result = new HashMap<>();
        for (int id : planIds) {
            FundOpinionItem item = cache.get(id);
            if (item != null) result.put(id, item);
        }
        return Collections.unmodifiableMap(result);
    }

    /**
     * Returns opinion for a single plan ID, or null if unavailable.
     */
    public FundOpinionItem getOpinionByPlanId(int planId) {
        Map<Integer, FundOpinionItem> map = getOpinionsByPlanIds(List.of(planId));
        return map.get(planId);
    }

    /** Clears internal cache; forces fresh API calls on next access. */
    public void invalidateCache() { cache.clear(); }

    // ----------------------------------------------------------------
    // Internal
    // ----------------------------------------------------------------

    private Map<Integer, FundOpinionItem> fetchFromApi(List<Integer> planIds) {
        String csv = planIds.stream().map(String::valueOf).collect(Collectors.joining(","));
        String url = config.baseUrl() + config.fundOpinionEndpoint() + "?plan_id=" + csv;
        logger.info("Calling Fund Opinion API: {} plan IDs", planIds.size());
        logger.debug("Fund Opinion API URL: {}", url);

        try {
            HttpRequest.Builder rb = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(config.timeoutSeconds()))
                    .GET()
                    .header("Accept", "application/json");
            String token = config.authToken();
            if (!token.isBlank()) rb.header("Authorization", "Bearer " + token);

            HttpResponse<String> resp = http.send(rb.build(), HttpResponse.BodyHandlers.ofString());
            logger.info("Fund Opinion API → HTTP {}", resp.statusCode());

            if (resp.statusCode() != 200) {
                logger.error("Unexpected status {}: {}", resp.statusCode(), resp.body());
                return Collections.emptyMap();
            }
            return parse(resp.body());

        } catch (Exception e) {
            logger.error("Fund Opinion API call failed: {}", e.getMessage(), e);
            return Collections.emptyMap();
        }
    }

    private Map<Integer, FundOpinionItem> parse(String json) {
        Map<Integer, FundOpinionItem> result = new HashMap<>();
        try {
            JsonObject root  = JsonParser.parseString(json).getAsJsonObject();
            JsonArray  array = resolveArray(root);
            if (array == null) {
                logger.warn("Fund Opinion API: could not locate opinion array in response");
                return result;
            }
            for (JsonElement el : array) {
                if (!el.isJsonObject()) continue;
                JsonObject obj      = el.getAsJsonObject();
                int     planId       = intOf(obj, "plan_id");
                boolean isProvisional = boolOf(obj, "is_provisional");
                String  opinion      = strOf(obj, "opinion_name");
                String  provisional  = strOf(obj, "provisional_opinion_name");
                if (planId > 0) {
                    result.put(planId, new FundOpinionItem(planId, isProvisional, opinion, provisional));
                }
            }
            logger.info("Fund Opinion API: parsed {} records", result.size());
        } catch (Exception e) {
            logger.error("Fund Opinion API parse error: {}", e.getMessage(), e);
        }
        return result;
    }

    private JsonArray resolveArray(JsonObject root) {
        JsonElement data = root.get("data");
        if (data == null) return null;
        if (data.isJsonArray()) return data.getAsJsonArray();
        if (data.isJsonObject()) {
            // Try common inner keys
            for (String key : new String[]{"results", "fund_opinions", "opinions", "items"}) {
                JsonElement inner = data.getAsJsonObject().get(key);
                if (inner != null && inner.isJsonArray()) return inner.getAsJsonArray();
            }
        }
        return null;
    }

    private String strOf(JsonObject o, String k) {
        JsonElement e = o.get(k);
        return (e != null && !e.isJsonNull()) ? e.getAsString() : "";
    }

    private int intOf(JsonObject o, String k) {
        JsonElement e = o.get(k);
        return (e != null && !e.isJsonNull()) ? e.getAsInt() : 0;
    }

    private boolean boolOf(JsonObject o, String k) {
        JsonElement e = o.get(k);
        return (e != null && !e.isJsonNull()) && e.getAsBoolean();
    }
}
