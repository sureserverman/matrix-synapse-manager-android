# Open GitHub Issues Research Report

Date checked: 2026-05-22

Repository: <https://github.com/sureserverman/matrix-synapse-manager-android>

Scope: open GitHub issues only. Closed issues are intentionally excluded.

This report uses:

- Open GitHub issue bodies from `gh issue list/view`.
- Current app source in this checkout at `main` / `v1.3.3` (`7dae611`).
- Current public server evidence where an issue named a public homeserver.
- Official Android, Matrix, Synapse, and MAS documentation.

Confidence labels:

- **Confirmed**: directly supported by issue text plus code, release history, or live/public data.
- **Unresolved**: the issue is open and needs reporter/server-side verification before claiming a full fix.

## Executive summary

| Issue | State | Most solid cause found | Confidence |
| --- | --- | --- | --- |
| [#5 Bright splash screen in dark setting](https://github.com/sureserverman/matrix-synapse-manager-android/issues/5) | Open | App launch theme is hardcoded to a light platform theme and does not define Android splash-screen dark resources. | Confirmed |
| [#4 The app doesn't start MAS auth](https://github.com/sureserverman/matrix-synapse-manager-android/issues/4) | Open | The named public `.well-known/matrix/client` currently does not advertise `org.matrix.msc2965.authentication`, while the app only starts MAS auth when that key is present. | Confirmed for current public config |

## #5 Bright splash screen in dark setting

Status: open.

Reporter symptom: cold launch shows a bright splash on a dark-themed device.

Evidence from code:

- `app/src/main/res/values/themes.xml` defines `Theme.MatrixSynapseManager` with parent `android:Theme.Material.Light.NoActionBar`.
- The same theme sets only `android:windowBackground` to `?android:attr/colorBackground`.
- `app/src/main/AndroidManifest.xml` applies that theme to both the application and `MainActivity`.
- There is no `res/values-night/themes.xml` in the current tree.
- There are no `windowSplashScreenBackground`, `windowSplashScreenAnimatedIcon`, or `postSplashScreenTheme` attributes in the current app resources.

Evidence from official docs:

- Android splash-screen customization uses `windowSplashScreenBackground` for the splash background. Android also notes that splash elements can have light and dark variants.
- AndroidX `SplashScreen` documents that `windowSplashScreenBackground` defaults to the theme `?attr/colorBackground`.

Conclusion:

The bright flash is caused by the launch window using a light platform theme before Compose applies the app theme. The current theme is explicitly `Material.Light`, and the app does not provide a night-mode launch theme or explicit splash-screen background attributes. This is a launch-theme/resource issue, not a Synapse/backend issue.

Relevant files:

- `app/src/main/res/values/themes.xml`
- `app/src/main/AndroidManifest.xml`

Relevant sources:

- Android splash-screen customization: <https://developer.android.com/develop/ui/views/launch/splash-screen>
- AndroidX `SplashScreen` theme attributes: <https://developer.android.com/reference/androidx/core/splashscreen/SplashScreen>

Next evidence-based check:

Add or verify a dark launch theme and Android splash-screen attributes in `values` and `values-night`, then test a cold launch on Android 12+ in dark mode. Do not change unrelated Compose screen styling until the launch-window resource behavior is verified.

## #4 The app doesn't start MAS auth

Status: open.

Reporter symptom: homeserver uses MAS and `.well-known`; entering the homeserver shows the legacy auth path instead of MAS auth.

Public server evidence checked on 2026-05-22:

```json
{"m.homeserver": {"base_url": "https://matrix.libre.tw"}, "org.matrix.msc4143.rtc_foci":[ {"type": "livekit", "livekit_service_url": "https://livekit.libre.tw"}]}
```

This was the live response from:

```text
https://matrix.libre.tw/.well-known/matrix/client
```

Evidence from code:

- `MasDiscoveryService.discover()` fetches `/.well-known/matrix/client`.
- It returns `MasDiscoveryResult.NotMas` if `orgMatrixMsc2965Authentication == null`.
- `WellKnownMatrixClient` maps only `org.matrix.msc2965.authentication` for MAS discovery.
- `LoginStrategyResolver` maps `NotMas` to `LoginStrategy.Password`, which is the legacy/password form.

Evidence from official docs:

- Matrix Client-Server discovery allows app-specific namespaced keys under `.well-known/matrix/client`.
- The MSC2965-era MAS `.well-known` documentation shows `org.matrix.msc2965.authentication` as the discovery key; that page marks itself out of date for Element-maintained MAS versions after 0.12.0, so it is used here only to explain the key this app currently implements.
- Current Element MAS docs state Synapse should delegate auth to MAS through `matrix_authentication_service`, and the compatibility layer requires proxying `/_matrix/client/*/login`, `logout`, and `refresh` to MAS. Current Element MAS reverse-proxy examples also include a `discovery` resource on the MAS HTTP listener.

Conclusion:

For the public config available on 2026-05-22, the immediate cause is missing MAS discovery metadata in `.well-known/matrix/client`. The app is following its current code path: no `org.matrix.msc2965.authentication` key means "not MAS", so it shows legacy auth.

This is still unresolved as an app issue because one server-side fact must be confirmed first: whether the reporter's MAS deployment is supposed to publish `org.matrix.msc2965.authentication` at `https://matrix.libre.tw/.well-known/matrix/client`. The live response currently advertises homeserver and RTC focus only.

Relevant files:

- `core/network/src/main/kotlin/com/matrix/synapse/network/auth/MasDiscoveryService.kt`
- `core/network/src/main/kotlin/com/matrix/synapse/network/auth/WellKnownMatrixClient.kt`
- `feature/auth/src/main/kotlin/com/matrix/synapse/feature/auth/domain/LoginStrategyResolver.kt`

Relevant sources:

- Matrix `.well-known/matrix/client`: <https://spec.matrix.org/latest/client-server-api/index.html#well-known-uri>
- MSC2965-era MAS `.well-known` configuration, marked out of date by that page: <https://matrix-org.github.io/matrix-authentication-service/setup/well-known.html>
- MAS homeserver configuration and compatibility layer: <https://element-hq.github.io/matrix-authentication-service/setup/homeserver.html>
- MAS reverse-proxy/discovery resource: <https://element-hq.github.io/matrix-authentication-service/setup/reverse-proxy.html>

Next evidence-based check:

Ask the server operator to verify the public response from `https://matrix.libre.tw/.well-known/matrix/client` and confirm whether it should include `org.matrix.msc2965.authentication`. If yes, fix the server/proxy `.well-known` source first. If the current MAS version intentionally uses a different discovery key, update the app only after identifying that current key from official Element MAS docs or the server's advertised discovery endpoints.

