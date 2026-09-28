# Appium helpers

Host-side Java helpers for Appium tests. Not part of the Android Gradle build (`settings.gradle.kts` includes only `:app`).

## HingeReader

Reads the hinge angle of a foldable iPhone simulator via `xcrun devicectl device motion hinge-angle`
(Xcode 27.1+, macOS only). It is read-only: neither `devicectl` nor `simctl` can set the angle, so fold/unfold
the simulator from Xcode Device Hub first, then wait for the angle before validating the UI.

No dependencies beyond the JDK (Java 11+).

## SimulatorInfo

Tells whether a simulator is an iPhone Duo from its CoreSimulator device type
(`com.apple.CoreSimulator.SimDeviceType.iPhone-Duo`), read with `xcrun simctl list devices -j`. Pass the UDID from
the `appium:udid` capability. More reliable than the device name (generic on iOS 16+) or the platform version
(a simulator's runtime is independent of its model, and the Duo will move past 27.1). Simulators in the default
set only; use `HingeReader` afterwards to confirm the hinge pose.
