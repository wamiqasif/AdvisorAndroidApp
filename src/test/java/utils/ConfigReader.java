package utils;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Singleton config reader.
 * Loads config.properties once and exposes typed getters.
 * Thread-safe via double-checked locking.
 */
public class ConfigReader {

    private static volatile ConfigReader instance;
    private final Properties properties = new Properties();

    private static final String CONFIG_PATH =
            "src/test/resources/config.properties";

    private ConfigReader() {
        try (InputStream is = new FileInputStream(CONFIG_PATH)) {
            properties.load(is);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load config.properties from: " + CONFIG_PATH, e);
        }
    }

    public static ConfigReader getInstance() {
        if (instance == null) {
            synchronized (ConfigReader.class) {
                if (instance == null) {
                    instance = new ConfigReader();
                }
            }
        }
        return instance;
    }

    // ----------------------------------------------------------------
    // Raw getter — all other getters delegate here
    // ----------------------------------------------------------------

    public String get(String key) {
        String value = readOverride(key);
        if (value == null || value.isBlank()) {
            value = properties.getProperty(key);
        }
        if (value == null || value.isBlank()) {
            throw new RuntimeException("Missing property: '" + key + "' in config.properties");
        }
        return value.trim();
    }

    public String getOrDefault(String key, String defaultValue) {
        String value = readOverride(key);
        if (value == null || value.isBlank()) {
            value = properties.getProperty(key);
        }
        return (value == null || value.isBlank()) ? defaultValue : value.trim();
    }

    private String readOverride(String key) {
        String systemValue = System.getProperty(key);
        if (systemValue != null && !systemValue.isBlank()) {
            return systemValue;
        }

        String envKey = key.toUpperCase().replace('.', '_');
        String envValue = System.getenv(envKey);
        if (envValue != null && !envValue.isBlank()) {
            return envValue;
        }

        return null;
    }

    // ----------------------------------------------------------------
    // Execution context
    // ----------------------------------------------------------------

    /** Returns "emulator" or "real". */
    public String getExecutionType() {
        return get("executionType").toLowerCase();
    }

    public boolean isEmulator() {
        return "emulator".equals(getExecutionType());
    }

    // ----------------------------------------------------------------
    // Device configuration — resolved per executionType
    // ----------------------------------------------------------------

    public String getDeviceName() {
        return isEmulator() ? get("emulator.deviceName") : get("real.deviceName");
    }

    public String getUdid() {
        return isEmulator() ? get("emulator.udid") : get("real.udid");
    }

    public String getPlatformVersion() {
        return isEmulator() ? get("emulator.platformVersion") : get("real.platformVersion");
    }

    // ----------------------------------------------------------------
    // Application
    // ----------------------------------------------------------------

    public String getAppPackage() {
        return get("appPackage");
    }

    public String getAppActivity() {
        return get("appActivity");
    }

    // ----------------------------------------------------------------
    // Capabilities
    // ----------------------------------------------------------------

    public String getPlatformName() {
        return get("platformName");
    }

    public String getAutomationName() {
        return get("automationName");
    }

    public boolean isNoReset() {
        return Boolean.parseBoolean(getOrDefault("noReset", "true"));
    }

    public boolean isAutoGrantPermissions() {
        return Boolean.parseBoolean(getOrDefault("autoGrantPermissions", "true"));
    }

    // ----------------------------------------------------------------
    // Server
    // ----------------------------------------------------------------

    public String getAppiumURL() {
        return get("appiumURL");
    }

    // ----------------------------------------------------------------
    // Parallel port allocation base
    // ----------------------------------------------------------------

    public int getSystemPortBase() {
        return Integer.parseInt(getOrDefault("systemPortBase", "8200"));
    }

    // ----------------------------------------------------------------
    // Timeouts
    // ----------------------------------------------------------------

    public int getImplicitWaitSeconds() {
        return Integer.parseInt(getOrDefault("implicitWaitSeconds", "10"));
    }

    public int getExplicitWaitSeconds() {
        return Integer.parseInt(getOrDefault("explicitWaitSeconds", "20"));
    }

    public int getNewCommandTimeout() {
        return Integer.parseInt(getOrDefault("newCommandTimeout", "120"));
    }

    // ----------------------------------------------------------------
    // Test credentials
    // ----------------------------------------------------------------

    public String getLoginUsername() {
        return get("login.username");
    }

    public String getLoginPin() {
        return get("login.pin");
    }
}
