package com.kodeco.android.coordplot.appium;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Reads the hinge angle of a foldable iPhone simulator through
 * {@code xcrun devicectl device motion hinge-angle} (read-only; devicectl cannot set the angle).
 *
 * The command streams one human-readable line per sample ("Angle: 180.0°", or "Angle:180,0°"
 * under a comma-decimal locale) and never ends on its own, so each read is bounded by devicectl's
 * --timeout and exits non-zero by design. The last sample printed before that is the answer.
 * The --json-output file carries no samples, so stdout is the only source.
 *
 * Runs on the Appium host (macOS with Xcode 27.1+), not on the device. The simulator must be in
 * the default simulator set; CoreDevice reports a simulator in a custom set as "not found".
 *
 * Usage with Appium (XCUITest driver):
 * <pre>
 *   String udid = (String) driver.getCapabilities().getCapability("appium:udid");
 *   double angle = HingeReader.readAngle(udid);          // 0 = closed, 180 = flat
 *   HingeReader.waitForAngle(udid, 180, 2.0, 30_000);    // after unfolding, before UI checks
 * </pre>
 */
public final class HingeReader {

    /** Smallest --timeout devicectl accepts; one read costs about this long. */
    private static final int STREAM_SECONDS = 5;
    /** Hard kill if CoreDevice wedges and ignores its own --timeout. */
    private static final long PROCESS_TIMEOUT_SECONDS = 20;
    private static final int MAX_ATTEMPTS = 4;
    private static final long RETRY_DELAY_MS = 2_000;
    private static final double STABLE_DEGREES = 0.5;
    private static final Pattern SAMPLE = Pattern.compile("Angle:\\s*(-?\\d+(?:[.,]\\d+)?)\\s*°");

    private HingeReader() {}

    /** Reads the current angle (0 = closed, 180 = flat), retrying when no sample was printed. */
    public static double readAngle(String udid) throws IOException, InterruptedException {
        IOException last = null;
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                return readOnce(udid);
            } catch (IOException e) {
                last = e;
                if (attempt < MAX_ATTEMPTS) {
                    Thread.sleep(RETRY_DELAY_MS * attempt);
                }
            }
        }
        throw new IOException("hinge-angle failed after " + MAX_ATTEMPTS + " attempts", last);
    }

    /**
     * Polls until the angle is within tolerance of the target AND two consecutive reads agree
     * within 0.5 deg (the hinge has stopped moving). Each read takes ~5 s, so allow at least
     * ~15 s. May overrun timeoutMs by at most one read.
     */
    public static double waitForAngle(String udid, double target, double tolerance, long timeoutMs)
            throws IOException, InterruptedException {
        long deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(timeoutMs);
        double previous = Double.NaN;
        double current = Double.NaN;
        do {
            current = readAngle(udid);
            boolean onTarget = Math.abs(current - target) <= tolerance;
            boolean stable = !Double.isNaN(previous) && Math.abs(current - previous) <= STABLE_DEGREES;
            if (onTarget && stable) {
                return current;
            }
            previous = current;
        } while (System.nanoTime() < deadline);
        throw new IllegalStateException(
                "Hinge did not settle at " + target + " deg (last read: " + current
                        + ", previous: " + previous + ")");
    }

    /** Last "Angle: N°" sample in the output, or NaN if none. Package-private for tests. */
    static double parseLastSample(String output) {
        double angle = Double.NaN;
        Matcher m = SAMPLE.matcher(output);
        while (m.find()) {
            angle = Double.parseDouble(m.group(1).replace(',', '.'));
        }
        return angle;
    }

    private static double readOnce(String udid) throws IOException, InterruptedException {
        String[] cmd = {
                "xcrun", "devicectl", "device", "motion", "hinge-angle",
                "--device", udid,
                "--session-timeout", "1",
                "--timeout", String.valueOf(STREAM_SECONDS)};
        // Redirect to a file so waitFor() is not blocked behind a read of a never-ending stream.
        Path out = Files.createTempFile("hinge-angle-", ".log");
        try {
            Process p = new ProcessBuilder(cmd)
                    .redirectErrorStream(true)
                    .redirectOutput(out.toFile())
                    .start();
            if (!p.waitFor(PROCESS_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                p.destroyForcibly();
            }
            // Exit code is intentionally ignored: the stream always ends via --timeout (non-zero).
            String text = Files.readString(out, StandardCharsets.UTF_8);
            double angle = parseLastSample(text);
            if (Double.isNaN(angle)) {
                throw new IOException("No hinge angle sample from " + String.join(" ", cmd)
                        + ": " + text.trim());
            }
            return angle;
        } finally {
            Files.deleteIfExists(out);
        }
    }
}
