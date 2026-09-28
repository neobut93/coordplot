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

## DeviceHubControl

Rotates and folds an iPhone Duo simulator the way Xcode's Device Hub does. On the Duo, Appium's `driver.rotate(...)`
fails with `Unable To Rotate Device` for every orientation, because the simulator's virtual machine provider
republishes orientation and overwrites the ordinary `XCUIDevice.orientation` rotation that WebDriverAgent uses.

`DeviceHubControl` compiles `src/main/resources/.../device_hub_helper.c` for the simulator SDK on first use
(cached in `~/.cache/coordplot-device-hub`) and runs it inside the simulator with `xcrun simctl spawn`. The helper
posts Device Hub's private vendor-defined HID event (usage page `0xFF61`, usage `0x5B`) for its orientation picker
(`portrait`, `landscape-left`, `landscape-right`, `pud`) or hinge slider (0–180).

- Private and undocumented; may break with any Xcode update. Xcode 27.1+, Apple silicon Mac, default simulator set.
- Keep `device_hub_helper.c` on the classpath next to the class (standard `src/main/resources` layout does this).
- Verify the result from the app's layout (e.g. window size); WebDriverAgent's orientation readback is unreliable on
  the unfolded Duo.

Related reports: [OpenDeviceHub#64](https://github.com/Mastersam07/OpenDeviceHub/pull/64),
[artemnovichkov/hinge](https://github.com/artemnovichkov/hinge),
[appium-xcuitest-driver#2829](https://github.com/appium/appium-xcuitest-driver/issues/2829),
[appium#16413](https://github.com/appium/appium/issues/16413),
[react-native-orientation-director#115](https://github.com/gladiuscode/react-native-orientation-director/issues/115),
[Maestro#3617](https://github.com/mobile-dev-inc/Maestro/issues/3617).
