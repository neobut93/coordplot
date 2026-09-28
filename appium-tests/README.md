# Appium helpers

Host-side Java helpers for Appium tests. Not part of the Android Gradle build (`settings.gradle.kts` includes only `:app`).

## HingeReader

Reads the hinge angle of a foldable iPhone simulator via `xcrun devicectl device motion hinge-angle`
(Xcode 27.1+, macOS only). It is read-only: neither `devicectl` nor `simctl` can set the angle, so fold/unfold
the simulator from Xcode Device Hub first, then wait for the angle before validating the UI.

No dependencies beyond the JDK (Java 11+).
