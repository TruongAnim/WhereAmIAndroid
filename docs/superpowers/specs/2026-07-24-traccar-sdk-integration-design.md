# WhereAmI — Traccar Client SDK Integration (Design)

**Date:** 2026-07-24
**Status:** Approved for planning
**App package:** `com.anim.where.am.i`

## 1. Goal

Turn the empty `whereami_android` Compose app into a working Traccar GPS tracking
client by integrating the local Kotlin Multiplatform `traccar-client-sdk`, using a
clean, extensible architecture, and porting the main features from the reference
Flutter app (`traccar-client`).

## 2. Scope

**In scope (approved MVP):**

- Core tracking: start/stop, Device ID + Server URL, tracking status + last location.
- Full settings: accuracy, distance, interval, angle, heartbeat, buffer, wakelock,
  stop-detection, prefer-platform-providers, with a basic/advanced split.
- Status/logs screen: view SDK logs (auto-refresh), share, clear.
- Single-shot position request (SOS).
- QR: scan config from QR, generate/share config QR.
- Quick actions (app shortcuts): start / stop / sos.
- Deep links: `whereami://action/*` and config links.
- Localization: English (default) + Vietnamese only.

**Out of scope (YAGNI):** Firebase (push/analytics/crashlytics), rate-my-app,
password-protected settings, ~30 localizations. Architecture stays open to add these
later (e.g. push = one extra data source + use case).

## 3. Key decisions

| Decision | Choice |
| --- | --- |
| SDK consumption | Composite build: `includeBuild("../traccar-client-sdk")`, depend on `org.traccar:traccar-client-sdk` (substitutes to local `:core`). |
| Toolchain | Bump app Kotlin `2.2.10 → 2.3.21` to match SDK metadata; keep AGP 9.2.x, compileSdk 37, minSdk 35. |
| DI / stack | Hilt + Compose + Coroutines/Flow, MVVM, DataStore for preferences. |
| Deep link scheme | `whereami://` |
| QR scanning | CameraX + ML Kit barcode scanning; QR generation via ZXing. |

### 3.1 Toolchain note (critical)

The SDK `:core` module is compiled with Kotlin 2.3.21. Kotlin 2.2.x cannot read 2.3
metadata, so the app **must** move to Kotlin 2.3.21 (and the matching Compose
compiler plugin). This is a required prerequisite, not optional. `libs.versions.toml`
in `whereami_android` is updated accordingly. `play-services-location` is pulled in
transitively by the SDK and need not be declared.

## 4. SDK surface used

- `sharedTracker(config: Config): Tracker` — singleton bootstrap.
- `Tracker`: `start()`, `stop()`, `requestPosition(alarm)`, `getLogs()`, `clearLogs()`,
  `updateConfig(newConfig): Tracker`, `state: StateFlow<State>`.
- Android extension `Tracker.startTracking(context)` — runs permission flow + battery
  optimization prompt + foreground service. Works with **application context**: the SDK
  launches its own `PermissionActivity` (via `startActivity` with `NEW_TASK`), so the
  app's ViewModels never need an Activity reference to start tracking.
- `Tracker.requestPosition(context, alarm)` — permission-guarded single shot.
- SDK auto-registers `TrackerStartup` ContentProvider (captures app context, creates the
  notification channel) and contributes its own manifest entries (PermissionActivity,
  foreground service) via manifest merger.
- Data classes: `Config`, `LocationConfig`, `NotificationConfig`, `Accuracy`, `State`,
  `Position`, `LogEntry`.

## 5. Architecture

Single `:app` module, organized package-by-layer so it can later be split into
Gradle modules without moving logic across boundaries.

```
com.anim.where.am.i/
├── WhereAmIApp                 @HiltAndroidApp Application
├── MainActivity               @AndroidEntryPoint, hosts NavHost, handles intents
├── di/
│   ├── TrackerModule          provides Tracker singleton + TrackerRepository
│   ├── RepositoryModule       binds SettingsRepository, LogRepository
│   └── DispatcherModule       provides IO/Default dispatchers
├── domain/
│   ├── model/                 TrackingSettings, TrackingStatus, LocationFix, LogItem,
│   │                          ConfigLink, Accuracy (domain enum)
│   ├── repository/            TrackerRepository, SettingsRepository, LogRepository
│   └── usecase/               one thin use case per action (see 5.2)
├── data/
│   ├── tracker/               TrackerRepositoryImpl (wraps SDK Tracker) + mappers
│   ├── settings/              SettingsRepositoryImpl (DataStore) + Config mapper
│   ├── log/                   LogRepositoryImpl (wraps getLogs/clearLogs) + mapper
│   └── config/                ConfigLinkParser, ConfigLinkBuilder
├── presentation/
│   ├── main/                  MainScreen + MainViewModel
│   ├── settings/              SettingsScreen + SettingsViewModel
│   ├── status/                StatusScreen + StatusViewModel
│   ├── qr/                    QrScanScreen + QrShareScreen + QrViewModel
│   ├── navigation/            NavHost, route definitions
│   └── theme/                 existing Compose theme
├── quickactions/              ShortcutManager setup + shortcut handler
└── deeplink/                  intent parsing → ApplyConfigLink / action use cases
```

**Core principle — SDK isolation:** the domain layer defines its own models and never
exposes SDK types. The data layer maps `TrackingSettings ↔ Config`, `State ↔
TrackingStatus`, `LogEntry ↔ LogItem`, domain `Accuracy ↔` SDK `Accuracy`. The SDK sits
entirely behind repository interfaces, so it is testable and replaceable.

### 5.1 Repositories (domain interfaces)

- `TrackerRepository`
  - `observeStatus(): Flow<TrackingStatus>`
  - `start()` / `stop()`
  - `requestPosition(alarm: String? = null): Boolean`
  - `updateConfig(settings: TrackingSettings)`
- `SettingsRepository`
  - `observeSettings(): Flow<TrackingSettings>`
  - `save(settings: TrackingSettings)`
- `LogRepository`
  - `getLogs(): List<LogItem>`
  - `clear()`

### 5.2 Use cases (thin)

`StartTracking`, `StopTracking`, `RequestSos`, `ObserveTrackingStatus`, `LoadSettings`,
`ObserveSettings`, `SaveSettings`, `GetLogs`, `ClearLogs`, `ApplyConfigLink`,
`BuildConfigLink`. Each wraps one repository call plus any small policy (e.g. heartbeat
minimum 60s, URL validation).

### 5.3 Data layer details

- `TrackerRepositoryImpl` holds the singleton `Tracker` (from `sharedTracker(config)`),
  calls `startTracking(appContext)` on start, maps `tracker.state` → `TrackingStatus`.
  `updateConfig` calls `tracker.updateConfig(newConfig)` and swaps the held instance.
- `SettingsRepositoryImpl` uses DataStore Preferences. On first launch it seeds a random
  8-digit `deviceId`, `serverUrl = https://demo.traccar.org`, `accuracy = MEDIUM`,
  `interval = 300`, `distance = 75`, `buffer = true`, `stopDetection = true` (matching the
  Flutter defaults). Settings are the source of truth; saving rebuilds `Config` and calls
  `tracker.updateConfig`.
- Mappers live next to their repository impl.

## 6. Presentation (Compose + MVVM)

- **MainScreen**: card with Device ID and a tracking `Switch` bound to
  `TrackingStatus.enabled`; Android background-location disclosure text; buttons to
  Settings, Status, and "Request position"; refreshes status on resume.
- **SettingsScreen**: basic fields (Server URL, Device ID, Accuracy dropdown, Distance,
  Interval) plus an **Advanced** toggle revealing angle, heartbeat (min 60), buffer,
  wakelock, stop-detection, prefer-platform-providers. URL validated as http/https.
  Entry points to QR scan and share-config-QR.
- **StatusScreen**: log list, auto-refresh every 5s while resumed, share via Android share
  sheet, clear.
- **QR**: scan via CameraX preview + ML Kit barcode analyzer → `ApplyConfigLink`; share
  screen renders a QR from `BuildConfigLink` (ZXing).
- **Navigation**: Navigation-Compose; routes `main`, `settings`, `status`, `qrScan`,
  `qrShare`.
- ViewModels expose `StateFlow<UiState>`; collect with `collectAsStateWithLifecycle`.

## 7. Quick actions + deep links

- **Quick actions**: dynamic `ShortcutManager` shortcuts `start` / `stop` / `sos`.
  Launching a shortcut routes its id to the matching use case, then finishes the Activity
  (mirrors the Flutter `SystemNavigator.pop()` behavior).
- **Deep links** (scheme `whereami`):
  - `whereami://action/{start|stop|sos}` → run the action use case, no UI.
  - `whereami://config?url&id&accuracy&distance&interval&angle&heartbeat&buffer&wakelock&stop_detection&prefer_platform_providers`
    and `http`/`https` links (server URL = origin + path) → parsed by `ConfigLinkParser`,
    then a confirmation dialog before `ApplyConfigLink` (mirrors Flutter).
  - Boolean params parsed as literal `true`/`false`; unknown/missing params are ignored.

## 8. Localization

`res/values/strings.xml` (English, default) and `res/values-vi/strings.xml`
(Vietnamese). All user-facing strings are resource-backed; only these two locales.

## 9. Error handling

- Permission denied → SDK throws `IllegalStateException` from `startTracking`; the
  ViewModel catches it, forces the tracking switch back off, and shows a snackbar.
- Invalid URL in settings → inline validation error, save blocked.
- QR/deep-link parse failure → ignored silently (no config change), matching Flutter.

## 10. Testing

- Unit tests (JUnit + fakes): use cases, mappers (`TrackingSettings ↔ Config`,
  `State ↔ TrackingStatus`, `LogEntry ↔ LogItem`), `ConfigLinkParser` / `ConfigLinkBuilder`
  round-trips, settings defaults/seeding, heartbeat/URL policies. No real SDK in unit
  tests — repositories are faked behind their interfaces.
- Optional: a basic Compose UI test for MainScreen switch behavior.

## 11. Prerequisites / risks

- **Kotlin bump to 2.3.21** is required for composite consumption; verify Compose BOM /
  compose-compiler compatibility during setup.
- Composite `includeBuild` dependency substitution must resolve `org.traccar:traccar-client-sdk`
  to the local `:core` project (its published coordinates); verify at first sync.
- Manifest merger must pull in the SDK's foreground-service + PermissionActivity entries;
  app manifest declares its own permissions (INTERNET, ACCESS_FINE_LOCATION,
  POST_NOTIFICATIONS, ACTIVITY_RECOGNITION, ACCESS_BACKGROUND_LOCATION, CAMERA for QR) and
  the `whereami` deep-link intent filter.
