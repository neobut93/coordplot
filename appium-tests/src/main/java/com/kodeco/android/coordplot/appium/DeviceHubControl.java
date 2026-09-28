package com.kodeco.android.coordplot.appium;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Rotates and folds a foldable iPhone simulator (iPhone Duo) the way Xcode's Device Hub does.
 *
 * On the Duo, Appium's {@code driver.rotate(...)} fails with "Unable To Rotate Device" for every
 * orientation: WebDriverAgent rotates through {@code XCUIDevice.orientation}, and the Duo's virtual
 * machine provider republishes orientation and overwrites that ordinary rotation. Device Hub instead
 * sends a private vendor-defined HID event (usage page 0xFF61, usage 0x5B) naming its orientation
 * picker or hinge slider. This class compiles a tiny helper ({@code device_hub_helper.c}) for the
 * simulator SDK once, caches it, and runs it inside the simulator with {@code xcrun simctl spawn}.
 *
 * Private and undocumented: may break with any Xcode update. Requires macOS with Xcode 27.1+,
 * an Apple silicon Mac, and a booted simulator in the default simulator set.
 *
 * WebDriverAgent's orientation readback is unreliable on the unfolded Duo, so verify the result
 * from your app's layout (e.g. {@code driver.manage().window().getSize()}), not {@code driver.rotation()}.
 *
 * Usage with Appium:
 * <pre>
 *   String udid = (String) driver.getCapabilities().getCapability("appium:udid");
 *   DeviceHubControl.setOrientation(udid, DeviceHubControl.Orientation.PORTRAIT);
 *   DeviceHubControl.setHingeAngle(udid, 180);        // unfold flat
 *   HingeReader.waitForAngle(udid, 180, 2.0, 30_000); // confirm the hinge settled
 * </pre>
 *
 * Background and related reports:
 * <ul>
 *   <li>https://github.com/Mastersam07/OpenDeviceHub/pull/64 (why ordinary rotation does not stick)</li>
 *   <li>https://github.com/artemnovichkov/hinge (the in-simulator HID helper approach)</li>
 *   <li>https://github.com/appium/appium-xcuitest-driver/issues/2829</li>
 *   <li>https://github.com/appium/appium/issues/16413</li>
 *   <li>https://github.com/gladiuscode/react-native-orientation-director/issues/115</li>
 *   <li>https://github.com/mobile-dev-inc/Maestro/issues/3617</li>
 * </ul>
 */
public final class DeviceHubControl {

    /**
     * Values of Device Hub's orientation picker. Left/right are the picker's names; they may be
     * mirrored relative to UIKit's interface orientation, so confirm each once against your layout.
     */
    public enum Orientation {
        PORTRAIT("portrait"),
        LANDSCAPE_LEFT("landscape-left"),
        LANDSCAPE_RIGHT("landscape-right"),
        PORTRAIT_UPSIDE_DOWN("pud");

        final String pickerValue;

        Orientation(String pickerValue) {
            this.pickerValue = pickerValue;
        }
    }

    private static final String HELPER_SOURCE = "device_hub_helper.c";
    private static final long BUILD_TIMEOUT_SECONDS = 120;
    private static final long SPAWN_TIMEOUT_SECONDS = 30;

    private DeviceHubControl() {}

    /** Turns the simulator to the given orientation through Device Hub's orientation picker event. */
    public static void setOrientation(String udid, Orientation orientation)
            throws IOException, InterruptedException {
        spawn(udid, "orientation", orientation.pickerValue);
    }

    /** Sets the hinge angle through Device Hub's hinge slider event: 0 = closed, 180 = flat. */
    public static void setHingeAngle(String udid, double degrees) throws IOException, InterruptedException {
        if (Double.isNaN(degrees) || degrees < 0 || degrees > 180) {
            throw new IllegalArgumentException("Hinge angle must be between 0 and 180: " + degrees);
        }
        spawn(udid, "hinge", String.valueOf(degrees));
    }

    // ---------------------------------------------------------------- internals

    private static void spawn(String udid, String command, String argument)
            throws IOException, InterruptedException {
        Path helper = helperBinary();
        run(SPAWN_TIMEOUT_SECONDS, "xcrun", "simctl", "spawn", udid, helper.toString(), command, argument);
    }

    /** Builds the helper once per helper source and selected Xcode, cached under ~/.cache. */
    private static synchronized Path helperBinary() throws IOException, InterruptedException {
        byte[] source = readHelperSource();
        String xcode = run(30, "xcode-select", "-p").trim();
        Path cacheDir = Paths.get(System.getProperty("user.home"), ".cache", "coordplot-device-hub");
        Path binary = cacheDir.resolve("device_hub_helper-" + cacheKey(source, xcode));
        if (Files.isExecutable(binary)) {
            return binary;
        }

        Files.createDirectories(cacheDir);
        Path sourceFile = Files.createTempFile(cacheDir, "device_hub_helper-", ".c");
        Path tmpBinary = Files.createTempFile(cacheDir, "device_hub_helper-", ".tmp");
        try {
            Files.write(sourceFile, source);
            run(BUILD_TIMEOUT_SECONDS,
                    "xcrun", "-sdk", "iphonesimulator", "clang",
                    "-arch", "arm64", "-mios-simulator-version-min=17.0", "-O2",
                    "-o", tmpBinary.toString(), sourceFile.toString(),
                    "-framework", "IOKit", "-framework", "CoreFoundation");
            run(30, "codesign", "-f", "-s", "-", tmpBinary.toString());
            Files.move(tmpBinary, binary, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            return binary;
        } finally {
            Files.deleteIfExists(sourceFile);
            Files.deleteIfExists(tmpBinary);
        }
    }

    private static byte[] readHelperSource() throws IOException {
        try (InputStream in = DeviceHubControl.class.getResourceAsStream(HELPER_SOURCE)) {
            if (in == null) {
                throw new IOException(HELPER_SOURCE + " not found on the classpath next to "
                        + DeviceHubControl.class.getName());
            }
            return in.readAllBytes();
        }
    }

    private static String cacheKey(byte[] source, String xcode) {
        try {
            MessageDigest sha = MessageDigest.getInstance("SHA-256");
            sha.update(source);
            sha.update(xcode.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : Arrays.copyOf(sha.digest(), 6)) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    /** Runs a command, returning its combined output; throws on timeout or non-zero exit. */
    private static String run(long timeoutSeconds, String... cmd) throws IOException, InterruptedException {
        List<String> command = new ArrayList<>(Arrays.asList(cmd));
        Path out = Files.createTempFile("device-hub-", ".log");
        try {
            Process p = new ProcessBuilder(command)
                    .redirectErrorStream(true)
                    .redirectOutput(out.toFile())
                    .start();
            if (!p.waitFor(timeoutSeconds, TimeUnit.SECONDS)) {
                p.destroyForcibly();
                throw new IOException("Timed out: " + String.join(" ", command));
            }
            String text = Files.readString(out, StandardCharsets.UTF_8);
            if (p.exitValue() != 0) {
                throw new IOException("Exit " + p.exitValue() + " from " + String.join(" ", command)
                        + ": " + text.trim());
            }
            return text;
        } finally {
            Files.deleteIfExists(out);
        }
    }
}
