package api;

import api.model.AlternativeFund;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class AlternativesApiService {

    private static final Logger logger = LoggerFactory.getLogger(AlternativesApiService.class);
    private static final String ALTERNATIVES_ENDPOINT = "/api/v1/advisory/alternatives";
    private static final int DIAGNOSTIC_TIMEOUT_SECONDS = 60;

    AlternativesApiResponse lastResponse = null;
    private final AdvisoryApiConfig config;
    private final HttpClient httpClient;
    private final Map<Integer, AlternativesApiResponse> cache = new LinkedHashMap<>();

    public AlternativesApiService() {
        config = AdvisoryApiConfig.getInstance();
        httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(DIAGNOSTIC_TIMEOUT_SECONDS))
                .build();
    }

    public AlternativesApiResponse getAlternativesResponse(int planId) {
        return cache.computeIfAbsent(planId, this::fetchWithRetry);
    }

    public List<AlternativeFund> getAlternatives(int planId) {
        return getAlternativesResponse(planId).alternatives;
    }

    private AlternativesApiResponse fetchAlternatives(int planId) {
        String labelIds = config.fundReviewLabelId();
        String baseUrl = config.baseUrl();
        String url = baseUrl
                + ALTERNATIVES_ENDPOINT
                + "?label-ids=" + encode(labelIds)
                + "&plan-id=" + planId;
        String token = config.authToken();

        logger.info(
                "CALLING_ALTERNATIVES_API\nBASE_URL={}\nFULL_URL={}\nPLAN_ID={}\nLABEL_IDS={}\nAUTH_TOKEN_PRESENT={}",
                baseUrl,
                url,
                planId,
                labelIds,
                !token.isBlank());

        long startTimeMs = System.currentTimeMillis();
        logger.info("REQUEST_START={}", startTimeMs);

        try {
            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(DIAGNOSTIC_TIMEOUT_SECONDS))
                    .GET()
                    .header("Accept", "application/json");

            if (!token.isBlank()) {
                builder.header("Authorization", "Bearer " + token);
            }

            HttpRequest request = builder.build();
            logger.info("REQUEST_HEADERS={}", request.headers().map());

            HttpResponse<String> response = httpClient.send(
                    request,
                    HttpResponse.BodyHandlers.ofString());

            long endTimeMs = System.currentTimeMillis();
            long responseTimeMs = endTimeMs - startTimeMs;
            logger.info(
                    "REQUEST_END={}\nHTTP_STATUS={}\nRESPONSE_TIME_MS={}",
                    endTimeMs,
                    response.statusCode(),
                    responseTimeMs);
            logger.info("RESPONSE_HEADERS={}", response.headers().map());

            if (response.statusCode() == 200) {
                logger.info("RESPONSE_LENGTH={}", response.body() != null ? response.body().length() : 0);
            } else {
                logger.error(
                        "HTTP_STATUS={}\nRESPONSE_BODY={}",
                        response.statusCode(),
                        response.body());
                runPostmanCompatibilityCheck(planId, labelIds, url, response.statusCode());
            }

            List<AlternativeFund> alternatives = response.statusCode() == 200
                    ? parseAlternatives(response.body())
                    : Collections.emptyList();

            return new AlternativesApiResponse(
                    response.statusCode(),
                    response.body(),
                    alternatives);
        } catch (Exception e) {
            long endTimeMs = System.currentTimeMillis();
            long responseTimeMs = endTimeMs - startTimeMs;
            logger.error(
                    "Alternatives API call failed\nPLAN_ID={}\nLABEL_IDS={}\nREQUEST_END={}\nRESPONSE_TIME_MS={}\nERROR={}",
                    planId,
                    labelIds,
                    endTimeMs,
                    responseTimeMs,
                    e.getMessage(),
                    e);
            runPostmanCompatibilityCheck(planId, labelIds, url, 0);
            return new AlternativesApiResponse(0, e.getMessage(), Collections.emptyList());
        }
    }

    private AlternativesApiResponse fetchWithRetry(int planId) {

        for (int attempt = 1; attempt <= 3; attempt++) {

            AlternativesApiResponse response =
                    fetchAlternatives(planId);

            if (response.statusCode == 200) {
                logger.info(
                        "SUCCESS_ON_ATTEMPT={} PLAN_ID={}",
                        attempt,
                        planId);
                return response;
            }

            logger.warn(
                    "FAILED_ATTEMPT={} PLAN_ID={} STATUS={}",
                    attempt,
                    planId,
                    response.statusCode);

            try {
				Thread.sleep(2000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
        }

        return lastResponse;
    }
    
    private HttpRequest buildPostmanLikeRequest(String url) {
        return HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(DIAGNOSTIC_TIMEOUT_SECONDS))
                .GET()
                .header("Accept", "*/*")
                .build();
    }

    private void runPostmanCompatibilityCheck(
            int planId,
            String labelIds,
            String url,
            int normalStatus) {

        long startTimeMs = System.currentTimeMillis();
        logger.info(
                "POSTMAN_COMPATIBILITY_CHECK_START\nPLAN_ID={}\nLABEL_IDS={}\nFULL_URL={}\nNORMAL_STATUS={}\nREQUEST_START={}",
                planId,
                labelIds,
                url,
                normalStatus,
                startTimeMs);

        try {
            HttpRequest postmanRequest = buildPostmanLikeRequest(url);
            logger.info("POSTMAN_REQUEST_HEADERS={}", postmanRequest.headers().map());

            HttpResponse<String> postmanResponse = httpClient.send(
                    postmanRequest,
                    HttpResponse.BodyHandlers.ofString());

            long endTimeMs = System.currentTimeMillis();
            logger.info(
                    "PLAN_ID={}\nNORMAL_STATUS={}\nPOSTMAN_STATUS={}\nREQUEST_END={}\nRESPONSE_TIME_MS={}",
                    planId,
                    normalStatus,
                    postmanResponse.statusCode(),
                    endTimeMs,
                    endTimeMs - startTimeMs);
            logger.info("POSTMAN_RESPONSE_HEADERS={}", postmanResponse.headers().map());

            if (postmanResponse.statusCode() == 200) {
                logger.info("POSTMAN_RESPONSE_LENGTH={}", postmanResponse.body() != null ? postmanResponse.body().length() : 0);
            } else {
                logger.error(
                        "POSTMAN_STATUS={}\nPOSTMAN_RESPONSE_BODY={}",
                        postmanResponse.statusCode(),
                        postmanResponse.body());
            }
        } catch (Exception e) {
            long endTimeMs = System.currentTimeMillis();
            logger.error(
                    "POSTMAN_COMPATIBILITY_CHECK_FAILED\nPLAN_ID={}\nNORMAL_STATUS={}\nREQUEST_END={}\nRESPONSE_TIME_MS={}\nERROR={}",
                    planId,
                    normalStatus,
                    endTimeMs,
                    endTimeMs - startTimeMs,
                    e.getMessage(),
                    e);
        }
    }

    private List<AlternativeFund> parseAlternatives(String json) {
        List<AlternativeFund> alternatives = new ArrayList<>();
        try {
            JsonObject root = JsonParser.parseString(json).getAsJsonObject();
            JsonElement dataElement = root.get("data");
            if (dataElement == null || !dataElement.isJsonObject()) {
                return Collections.emptyList();
            }

            JsonElement alternativesElement = dataElement.getAsJsonObject().get("alternatives");
            if (alternativesElement == null || !alternativesElement.isJsonArray()) {
                return Collections.emptyList();
            }

            for (JsonElement element : alternativesElement.getAsJsonArray()) {
                if (!element.isJsonObject()) {
                    continue;
                }

                JsonObject alternative = element.getAsJsonObject();
                alternatives.add(new AlternativeFund(
                        intOf(alternative, "plan_id"),
                        stringOf(alternative, "name"),
                        stringOf(alternative, "category_name"),
                        booleanOf(alternative, "is_etf_plan"),
                        booleanOf(alternative, "is_checked")));
            }
        } catch (Exception e) {
            logger.error("Alternatives API parse failed: {}", e.getMessage(), e);
        }
        return Collections.unmodifiableList(alternatives);
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private String stringOf(JsonObject object, String key) {
        JsonElement element = object.get(key);
        return element != null && !element.isJsonNull() ? element.getAsString().trim() : "";
    }

    private int intOf(JsonObject object, String key) {
        JsonElement element = object.get(key);
        return element != null && !element.isJsonNull() ? element.getAsInt() : 0;
    }

    private boolean booleanOf(JsonObject object, String key) {
        JsonElement element = object.get(key);
        return element != null && !element.isJsonNull() && element.getAsBoolean();
    }

    public static final class AlternativesApiResponse {
        public final int statusCode;
        public final String responseBody;
        public final List<AlternativeFund> alternatives;

        public AlternativesApiResponse(
                int statusCode,
                String responseBody,
                List<AlternativeFund> alternatives) {
            this.statusCode = statusCode;
            this.responseBody = responseBody != null ? responseBody : "";
            this.alternatives = alternatives != null ? alternatives : Collections.emptyList();
        }
    }
}
