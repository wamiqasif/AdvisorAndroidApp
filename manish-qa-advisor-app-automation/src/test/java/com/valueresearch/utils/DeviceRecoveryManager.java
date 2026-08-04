package com.valueresearch.utils;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

/**
 * Global Android infrastructure recovery for sequential TestNG execution.
 *
 * <p>When only UiAutomator2 dies, the emulator is kept alive and a fresh
 * UiAutomator2 session is prepared. When emulator-5554 disappears, the same
 * AVD is restarted in either visible (headed) or headless mode according to
 * config.properties.</p>
 */
public final class DeviceRecoveryManager {

    private static final long DEVICE_CONNECT_TIMEOUT_SECONDS = 180;
    private static final long ANDROID_BOOT_TIMEOUT_SECONDS = 180;
    private static final long APPIUM_READY_TIMEOUT_SECONDS = 45;
    private static final long COMMAND_TIMEOUT_SECONDS = 15;

    private DeviceRecoveryManager() {
        // Utility class.
    }

    /**
     * Normal recovery path used before a new AndroidDriver session.
     *
     * <ul>
     *     <li>Device online: restart only stale UiAutomator2 state.</li>
     *     <li>Emulator offline: restart the configured AVD, wait for boot,
     *     then prepare UiAutomator2.</li>
     * </ul>
     */
    public static synchronized void ensureInfrastructureReady(
            String deviceUdid,
            String appiumServerUrl
    ) {
        String adb = getAdbExecutable();
        startAdbServer(adb);

        boolean emulatorRestarted = false;

        if (!isDeviceOnline(deviceUdid)) {
            if (!isEmulator(deviceUdid)) {
                throw new IllegalStateException(
                        "Android device is not connected and cannot be restarted automatically: "
                                + deviceUdid
                );
            }

            System.out.println(
                    "[RECOVERY] " + deviceUdid
                            + " is offline. Restarting the Android emulator."
            );

            restartEmulator(deviceUdid);
            emulatorRestarted = true;
        } else {
            System.out.println(
                    "[RECOVERY] Emulator/device is online. Preparing a fresh UiAutomator2 session."
            );
        }

        waitForDeviceOnline(deviceUdid);
        waitForAndroidBoot(deviceUdid, emulatorRestarted);
        prepareDeviceForFreshUiAutomator2(deviceUdid);
        waitForAppium(appiumServerUrl);
    }

    /**
     * Last-resort recovery. It performs a complete emulator restart when the
     * first fresh AndroidDriver creation still fails.
     */
    public static synchronized void forceCompleteRecovery(
            String deviceUdid,
            String appiumServerUrl
    ) {
        String adb = getAdbExecutable();
        startAdbServer(adb);

        if (isEmulator(deviceUdid)) {
            System.out.println(
                    "[RECOVERY] Fresh UiAutomator2 session failed. Performing a complete emulator restart."
            );

            restartEmulator(deviceUdid);
            waitForDeviceOnline(deviceUdid);
            waitForAndroidBoot(deviceUdid, true);

        } else if (!isDeviceOnline(deviceUdid)) {
            throw new IllegalStateException(
                    "Physical Android device is not connected: " + deviceUdid
            );
        }

        prepareDeviceForFreshUiAutomator2(deviceUdid);
        waitForAppium(appiumServerUrl);
    }

    public static boolean isDeviceOnline(String deviceUdid) {
        if (deviceUdid == null || deviceUdid.trim().isEmpty()) {
            return false;
        }

        CommandResult result = execute(
                8,
                getAdbExecutable(),
                "-s",
                deviceUdid,
                "get-state"
        );

        return result.exitCode == 0
                && "device".equals(result.output.trim());
    }

    private static void startAdbServer(String adb) {
        CommandResult result = execute(
                COMMAND_TIMEOUT_SECONDS,
                adb,
                "start-server"
        );

        if (result.exitCode != 0) {
            throw new IllegalStateException(
                    "Unable to start ADB server: " + result.output
            );
        }
    }

    private static void restartEmulator(String deviceUdid) {
        String avdName = ConfigReader.getOptional("avdName", "Pixel_9a");
        int emulatorPort = parseEmulatorPort(deviceUdid);

        stopExistingEmulator(deviceUdid, avdName, emulatorPort);
        sleep(5_000);
        startEmulator(avdName, emulatorPort);
    }

    private static void stopExistingEmulator(
            String deviceUdid,
            String avdName,
            int emulatorPort
    ) {
        if (isDeviceOnline(deviceUdid)) {
            execute(
                    10,
                    getAdbExecutable(),
                    "-s",
                    deviceUdid,
                    "emu",
                    "kill"
            );
        }

        String avdToken = "-avd " + avdName;
        String portToken = "-port " + emulatorPort;

        ProcessHandle.allProcesses().forEach(process -> {
            try {
                String commandLine = process.info().commandLine().orElse("");
                String lower = commandLine.toLowerCase(Locale.ROOT);

                boolean emulatorProcess = lower.contains("/emulator")
                        || lower.contains("qemu-system");

                boolean sameAvd = commandLine.contains(avdToken);
                boolean samePort = commandLine.contains(portToken);

                if (emulatorProcess && (sameAvd || samePort)) {
                    process.destroy();

                    try {
                        process.onExit().get(5, TimeUnit.SECONDS);
                    } catch (Exception ignored) {
                        process.destroyForcibly();
                    }
                }
            } catch (Exception ignored) {
                // Process may disappear while being inspected.
            }
        });
    }

    private static void startEmulator(String avdName, int emulatorPort) {
        String emulator = getEmulatorExecutable();
        File emulatorFile = new File(emulator);

        if (!emulatorFile.isFile() || !emulatorFile.canExecute()) {
            throw new IllegalStateException(
                    "Android emulator executable was not found or is not executable: "
                            + emulator
            );
        }

        boolean headless = Boolean.parseBoolean(
                ConfigReader.getOptional("emulatorHeadless", "false")
        );

        String gpuMode = ConfigReader.getOptional(
                "emulatorGpuMode",
                "swiftshader"
        );

        if (!headless && !hasDesktopDisplay()) {
            throw new IllegalStateException(
                    "Visible emulator recovery was requested, but DISPLAY/WAYLAND_DISPLAY "
                            + "is not available. Run from the Ubuntu desktop/Eclipse or set "
                            + "emulatorHeadless=true in config.properties."
            );
        }

        Path recoveryDirectory = Paths.get(
                System.getProperty("user.dir"),
                "test-output",
                "recovery"
        );

        try {
            Files.createDirectories(recoveryDirectory);
        } catch (IOException e) {
            throw new IllegalStateException(
                    "Unable to create recovery log directory: "
                            + recoveryDirectory,
                    e
            );
        }

        String modeName = headless ? "headless" : "headed";
        Path emulatorLog = recoveryDirectory.resolve(
                modeName + "-emulator-recovery.log"
        );

        List<String> command = new ArrayList<>();
        command.add(emulator);
        command.add("-avd");
        command.add(avdName);
        command.add("-port");
        command.add(String.valueOf(emulatorPort));
        command.add("-no-audio");
        command.add("-no-snapshot");
        command.add("-no-boot-anim");
        command.add("-gpu");
        command.add(gpuMode);
        command.add("-camera-back");
        command.add("none");
        command.add("-camera-front");
        command.add("none");
        command.add("-netdelay");
        command.add("none");
        command.add("-netspeed");
        command.add("full");

        if (headless) {
            command.add("-no-window");
        }

        ProcessBuilder builder = new ProcessBuilder(command);
        builder.redirectErrorStream(true);
        builder.redirectOutput(
                ProcessBuilder.Redirect.appendTo(emulatorLog.toFile())
        );

        try {
            Process process = builder.start();

            System.out.println(
                    "[RECOVERY] Emulator started in "
                            + modeName.toUpperCase(Locale.ROOT)
                            + " mode"
                            + " | AVD=" + avdName
                            + " | port=" + emulatorPort
                            + " | GPU=" + gpuMode
                            + " | pid=" + process.pid()
                            + " | log=" + emulatorLog
            );

        } catch (IOException e) {
            throw new IllegalStateException(
                    "Unable to start " + modeName + " emulator " + avdName,
                    e
            );
        }
    }

    private static boolean hasDesktopDisplay() {
        String display = System.getenv("DISPLAY");
        if (display != null && !display.trim().isEmpty()) {
            return true;
        }

        String waylandDisplay = System.getenv("WAYLAND_DISPLAY");
        return waylandDisplay != null && !waylandDisplay.trim().isEmpty();
    }

    private static void waitForDeviceOnline(String deviceUdid) {
        long deadline = System.nanoTime()
                + TimeUnit.SECONDS.toNanos(DEVICE_CONNECT_TIMEOUT_SECONDS);

        while (System.nanoTime() < deadline) {
            if (isDeviceOnline(deviceUdid)) {
                System.out.println(
                        "[RECOVERY] Android device connected: " + deviceUdid
                );
                return;
            }

            sleep(3_000);
        }

        throw new IllegalStateException(
                deviceUdid + " did not connect to ADB within "
                        + DEVICE_CONNECT_TIMEOUT_SECONDS + " seconds"
        );
    }

    private static void waitForAndroidBoot(
            String deviceUdid,
            boolean settleAfterBoot
    ) {
        long deadline = System.nanoTime()
                + TimeUnit.SECONDS.toNanos(ANDROID_BOOT_TIMEOUT_SECONDS);

        while (System.nanoTime() < deadline) {
            if (!isDeviceOnline(deviceUdid)) {
                sleep(3_000);
                continue;
            }

            CommandResult result = execute(
                    8,
                    getAdbExecutable(),
                    "-s",
                    deviceUdid,
                    "shell",
                    "getprop",
                    "sys.boot_completed"
            );

            if (result.exitCode == 0
                    && "1".equals(result.output.trim())) {
                System.out.println(
                        "[RECOVERY] Android boot completed: " + deviceUdid
                );

                if (settleAfterBoot) {
                    sleep(15_000);
                }
                return;
            }

            sleep(3_000);
        }

        throw new IllegalStateException(
                "Android did not finish booting within "
                        + ANDROID_BOOT_TIMEOUT_SECONDS + " seconds"
        );
    }

    private static void prepareDeviceForFreshUiAutomator2(
            String deviceUdid
    ) {
        if (!isDeviceOnline(deviceUdid)) {
            throw new IllegalStateException(
                    "Cannot prepare UiAutomator2 because device is offline: "
                            + deviceUdid
            );
        }

        String adb = getAdbExecutable();

        // These are best-effort device preparation commands.
        execute(10, adb, "-s", deviceUdid,
                "shell", "svc", "power", "stayon", "true");
        execute(10, adb, "-s", deviceUdid,
                "shell", "input", "keyevent", "224");
        execute(10, adb, "-s", deviceUdid,
                "shell", "wm", "dismiss-keyguard");

        System.out.println(
                "[RECOVERY] Cleaning stale UiAutomator2 processes and port forwarding"
        );

        execute(10, adb, "-s", deviceUdid,
                "shell", "am", "force-stop",
                "io.appium.uiautomator2.server");
        execute(10, adb, "-s", deviceUdid,
                "shell", "am", "force-stop",
                "io.appium.uiautomator2.server.test");
        execute(10, adb, "-s", deviceUdid,
                "forward", "--remove", "tcp:8200");

        sleep(1_500);
    }

    private static void waitForAppium(String appiumServerUrl) {
        long deadline = System.nanoTime()
                + TimeUnit.SECONDS.toNanos(APPIUM_READY_TIMEOUT_SECONDS);

        while (System.nanoTime() < deadline) {
            if (isAppiumReady(appiumServerUrl)) {
                System.out.println(
                        "[RECOVERY] Appium is ready: " + appiumServerUrl
                );
                return;
            }

            sleep(2_000);
        }

        throw new IllegalStateException(
                "Appium is not ready at " + appiumServerUrl
                        + ". Start Appium before running the suite."
        );
    }

    private static boolean isAppiumReady(String appiumServerUrl) {
        HttpURLConnection connection = null;

        try {
            String baseUrl = appiumServerUrl == null
                    ? ""
                    : appiumServerUrl.trim().replaceAll("/+$", "");

            URL statusUrl = new URL(baseUrl + "/status");
            connection = (HttpURLConnection) statusUrl.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(2_000);
            connection.setReadTimeout(2_000);

            int status = connection.getResponseCode();
            return status >= 200 && status < 300;

        } catch (Exception ignored) {
            return false;

        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private static int parseEmulatorPort(String deviceUdid) {
        if (deviceUdid == null
                || !deviceUdid.startsWith("emulator-")) {
            return 5554;
        }

        try {
            return Integer.parseInt(
                    deviceUdid.substring("emulator-".length())
            );
        } catch (NumberFormatException ignored) {
            return 5554;
        }
    }

    private static boolean isEmulator(String deviceUdid) {
        return deviceUdid != null
                && deviceUdid.startsWith("emulator-");
    }

    private static String getAdbExecutable() {
        return Paths.get(
                getAndroidSdkRoot(),
                "platform-tools",
                "adb"
        ).toString();
    }

    private static String getEmulatorExecutable() {
        return Paths.get(
                getAndroidSdkRoot(),
                "emulator",
                "emulator"
        ).toString();
    }

    private static String getAndroidSdkRoot() {
        String androidHome = System.getenv("ANDROID_HOME");
        if (androidHome != null && !androidHome.trim().isEmpty()) {
            return androidHome.trim();
        }

        String androidSdkRoot = System.getenv("ANDROID_SDK_ROOT");
        if (androidSdkRoot != null
                && !androidSdkRoot.trim().isEmpty()) {
            return androidSdkRoot.trim();
        }

        return Paths.get(
                System.getProperty("user.home"),
                "Android",
                "Sdk"
        ).toString();
    }

    private static CommandResult execute(
            long timeoutSeconds,
            String... command
    ) {
        Process process = null;

        try {
            ProcessBuilder builder = new ProcessBuilder(command);
            builder.redirectErrorStream(true);
            process = builder.start();

            boolean finished = process.waitFor(
                    timeoutSeconds,
                    TimeUnit.SECONDS
            );

            if (!finished) {
                process.destroyForcibly();
                return new CommandResult(
                        -1,
                        "Command timed out: "
                                + String.join(" ", command)
                );
            }

            String output = new String(
                    process.getInputStream().readAllBytes(),
                    StandardCharsets.UTF_8
            ).trim();

            return new CommandResult(
                    process.exitValue(),
                    output
            );

        } catch (Exception e) {
            if (process != null) {
                process.destroyForcibly();
            }

            String message = e.getMessage();
            if (message == null || message.trim().isEmpty()) {
                message = e.getClass().getSimpleName();
            }

            return new CommandResult(-1, message);
        }
    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(
                    "Device recovery was interrupted",
                    e
            );
        }
    }

    private static final class CommandResult {
        private final int exitCode;
        private final String output;

        private CommandResult(int exitCode, String output) {
            this.exitCode = exitCode;
            this.output = output == null ? "" : output;
        }
    }
}