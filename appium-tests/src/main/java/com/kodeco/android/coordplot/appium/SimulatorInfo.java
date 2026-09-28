package com.kodeco.android.coordplot.appium;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Identifies an iOS simulator by its CoreSimulator device type, read on the Appium host with
 * {@code xcrun simctl list devices -j}.
 *
 * Use this instead of the device name (iOS 16+ only exposes a generic name to apps) or the
 * platform version (iPhone Duo ships on 27.1 today, but will move to later versions, and a
 * simulator's runtime is chosen independently of its device type).
 *
 * Simulators only, in the default simulator set; a physical device's UDID is not listed.
 *
 * Usage with Appium (XCUITest driver):
 * <pre>
 *   String udid = (String) driver.getCapabilities().getCapability("appium:udid");
 *   if (SimulatorInfo.isIPhoneDuo(udid)) { ... }
 * </pre>
 */
public final class SimulatorInfo {

    public static final String IPHONE_DUO_DEVICE_TYPE = "com.apple.CoreSimulator.SimDeviceType.iPhone-Duo";

    private static final long TIMEOUT_SECONDS = 30;
    /** simctl's device entries are flat JSON objects (no nested objects), one per simulator. */
    private static final Pattern DEVICE_ENTRY = Pattern.compile("\\{[^{}]*\\}");
    private static final Pattern UDID = Pattern.compile("\"udid\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern DEVICE_TYPE =
            Pattern.compile("\"deviceTypeIdentifier\"\\s*:\\s*\"([^\"]+)\"");

    private SimulatorInfo() {}

    /** True if the simulator with this UDID is an iPhone Duo. */
    public static boolean isIPhoneDuo(String udid) throws IOException, InterruptedException {
        return IPHONE_DUO_DEVICE_TYPE.equals(deviceTypeIdentifier(udid));
    }

    /**
     * The simulator's device type, e.g. {@code com.apple.CoreSimulator.SimDeviceType.iPhone-Duo}.
     *
     * @throws IOException if simctl fails or no simulator has this UDID (e.g. a physical device)
     */
    public static String deviceTypeIdentifier(String udid) throws IOException, InterruptedException {
        String[] cmd = {"xcrun", "simctl", "list", "devices", "-j"};
        Path out = Files.createTempFile("simctl-devices-", ".json");
        try {
            Process p = new ProcessBuilder(cmd)
                    .redirectError(ProcessBuilder.Redirect.DISCARD)
                    .redirectOutput(out.toFile())
                    .start();
            if (!p.waitFor(TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                p.destroyForcibly();
                throw new IOException("Timed out: " + String.join(" ", cmd));
            }
            if (p.exitValue() != 0) {
                throw new IOException("Exit " + p.exitValue() + " from " + String.join(" ", cmd));
            }
            String json = Files.readString(out, StandardCharsets.UTF_8);
            return findDeviceType(json, udid).orElseThrow(() -> new IOException(
                    "No simulator with UDID " + udid + " in the default simulator set"));
        } finally {
            Files.deleteIfExists(out);
        }
    }

    /** Device type of the entry whose udid matches, from simctl's JSON. Package-private for tests. */
    static Optional<String> findDeviceType(String simctlJson, String udid) {
        Matcher entry = DEVICE_ENTRY.matcher(simctlJson);
        while (entry.find()) {
            String device = entry.group();
            Matcher u = UDID.matcher(device);
            if (u.find() && u.group(1).equalsIgnoreCase(udid)) {
                Matcher type = DEVICE_TYPE.matcher(device);
                return type.find() ? Optional.of(type.group(1)) : Optional.empty();
            }
        }
        return Optional.empty();
    }
}
