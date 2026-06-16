package api;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Singleton config reader for advisory-api-config.properties.
 * Supports system-property and environment-variable overrides
 * (same pattern as ConfigReader / ApiConfigReader).
 */
public class AdvisoryApiConfig {

    private static volatile AdvisoryApiConfig instance;
    private final Properties props = new Properties();
    private static final String PATH = "src/test/resources/advisory-api-config.properties";

    private AdvisoryApiConfig() {
        try (InputStream is = new FileInputStream(PATH)) {
            props.load(is);
        } catch (IOException e) {
            throw new RuntimeException("Cannot load advisory-api-config.properties from: " + PATH, e);
        }
    }

    public static AdvisoryApiConfig getInstance() {
        if (instance == null) {
            synchronized (AdvisoryApiConfig.class) {
                if (instance == null) instance = new AdvisoryApiConfig();
            }
        }
        return instance;
    }

    /** Returns property value; system prop and env var take precedence over file. */
    public String get(String key) {
        String v = System.getProperty(key);
        if (v != null && !v.isBlank()) return v.trim();
        v = System.getenv(key.toUpperCase().replace('.', '_'));
        if (v != null && !v.isBlank()) return v.trim();
        v = props.getProperty(key);
        if (v == null || v.isBlank())
            throw new RuntimeException("Missing advisory API property: '" + key + "'");
        return v.trim();
    }

    public String getOrDefault(String key, String def) {
        try {
            String v = get(key);
            return v.isEmpty() ? def : v;
        } catch (RuntimeException e) {
            return def;
        }
    }

    // ----------------------------------------------------------------
    // Typed accessors
    // ----------------------------------------------------------------
    public String baseUrl()               { return get("advisory.api.baseUrl"); }
    public String fundReviewEndpoint()    { return get("advisory.fund.review.endpoint"); }
    public String fundReviewLabelId()     { return get("advisory.fund.review.labelId"); }
    public String fundOpinionEndpoint()   { return get("advisory.fund.opinion.endpoint"); }
    public String authToken()             { return getOrDefault("advisory.api.authToken", ""); }
    public int    timeoutSeconds()        { return Integer.parseInt(getOrDefault("advisory.api.timeoutSeconds", "30")); }
}
