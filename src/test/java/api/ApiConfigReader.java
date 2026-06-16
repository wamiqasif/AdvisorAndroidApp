package api;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class ApiConfigReader {

    private static volatile ApiConfigReader instance;
    private final Properties properties = new Properties();
    private static final String CONFIG_PATH = "src/test/resources/api-config.properties";

    private ApiConfigReader() {
        try (InputStream is = new FileInputStream(CONFIG_PATH)) {
            properties.load(is);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load api-config.properties from: " + CONFIG_PATH, e);
        }
    }

    public static ApiConfigReader getInstance() {
        if (instance == null) {
            synchronized (ApiConfigReader.class) {
                if (instance == null) {
                    instance = new ApiConfigReader();
                }
            }
        }
        return instance;
    }

    public String get(String key) {
        String value = System.getProperty(key);
        if (value != null && !value.isBlank()) return value.trim();
        String envKey = key.toUpperCase().replace('.', '_');
        value = System.getenv(envKey);
        if (value != null && !value.isBlank()) return value.trim();
        value = properties.getProperty(key);
        if (value == null || value.isBlank()) {
            throw new RuntimeException("Missing API config property: '" + key + "'");
        }
        return value.trim();
    }

    public String getOrDefault(String key, String defaultValue) {
        try {
            String v = get(key);
            return v.isEmpty() ? defaultValue : v;
        } catch (RuntimeException e) {
            return defaultValue;
        }
    }

    public String getBaseUrl()      { return get("fund.review.api.baseUrl"); }
    public String getEndpoint()     { return get("fund.review.api.endpoint"); }
    public String getLabelId()      { return get("fund.review.api.labelId"); }
    public String getAuthToken()    { return getOrDefault("fund.review.api.authToken", ""); }
    public int    getTimeout()      { return Integer.parseInt(getOrDefault("fund.review.api.timeoutSeconds", "30")); }
}
