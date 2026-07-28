# Traccar SDK Integration Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Turn the empty `whereami_android` Compose app into a working Traccar GPS tracking client by integrating the local `traccar-client-sdk` behind a Clean Architecture, porting the main features of the reference Flutter app.

**Architecture:** Single `:app` module, package-by-layer (domain / data / presentation) with the SDK hidden behind repository interfaces. MVVM on the presentation side, Hilt for DI, DataStore for persisted settings, Coroutines/Flow throughout.

**Tech Stack:** Kotlin 2.3.21, Jetpack Compose, Hilt, DataStore Preferences, Navigation-Compose, kotlinx-coroutines, CameraX + ML Kit barcode (QR scan), ZXing (QR generate), the composite-built `org.traccar:traccar-client-sdk`.

## Global Constraints

- App package / namespace: `com.anim.where.am.i` (verbatim).
- Kotlin version MUST be `2.3.21` (SDK core metadata requires it); AGP `9.2.0` (aligned with SDK for composite build; was 9.2.1); Gradle wrapper `9.4.1`.
- compileSdk `37`, minSdk `35`, targetSdk `36` — unchanged.
- SDK consumed via `includeBuild("../traccar-client-sdk")` + `implementation("org.traccar:traccar-client-sdk")`. Never copy SDK source.
- Domain layer MUST NOT reference any `org.traccar.client.*` type. Mapping happens only in `data/`.
- Deep-link scheme: `whereami`.
- Localization: only `res/values/strings.xml` (English) + `res/values-vi/strings.xml` (Vietnamese). All user-facing text via string resources.
- Settings defaults (first launch): random 8-digit deviceId, serverUrl `https://demo.traccar.org`, accuracy MEDIUM, interval 300, distance 75, buffer true, stopDetection true. Heartbeat, when > 0, has a minimum of 60.
- TDD: every logic task writes a failing test first. Commit after each task.
- Source root: `app/src/main/java/com/anim/where/am/i/`. Unit tests: `app/src/test/java/com/anim/where/am/i/`.

---

## Phase 0 — Toolchain & wiring

### Task 1: Upgrade toolchain and declare dependencies

**Files:**
- Modify: `gradle/libs.versions.toml`
- Modify: `app/build.gradle.kts`
- Modify: `build.gradle.kts` (root)

**Interfaces:**
- Produces: version catalog aliases used by every later task (`libs.hilt.android`, `libs.androidx.datastore.preferences`, `libs.androidx.navigation.compose`, `libs.androidx.lifecycle.viewmodel.compose`, `libs.kotlinx.coroutines.android`, `libs.camerax.*`, `libs.mlkit.barcode`, `libs.zxing.core`, `libs.plugins.hilt`, `libs.plugins.ksp`).

- [ ] **Step 1: Update `gradle/libs.versions.toml`.** Set `kotlin = "2.3.21"`. Add versions and libraries:

```toml
[versions]
agp = "9.2.1"
kotlin = "2.3.21"
coreKtx = "1.19.0"
junit = "4.13.2"
junitVersion = "1.3.0"
espressoCore = "3.7.0"
lifecycleRuntimeKtx = "2.10.0"
activityCompose = "1.13.0"
composeBom = "2026.02.01"
hilt = "2.54"
ksp = "2.3.21-1.0.29"
datastore = "1.1.1"
navigationCompose = "2.8.5"
lifecycleViewmodelCompose = "2.10.0"
coroutines = "1.11.0"
camerax = "1.4.1"
mlkitBarcode = "17.3.0"
zxing = "3.5.3"

[libraries]
# ...keep existing entries...
hilt-android = { group = "com.google.dagger", name = "hilt-android", version.ref = "hilt" }
hilt-compiler = { group = "com.google.dagger", name = "hilt-android-compiler", version.ref = "hilt" }
androidx-hilt-navigation-compose = { group = "androidx.hilt", name = "hilt-navigation-compose", version = "1.2.0" }
androidx-datastore-preferences = { group = "androidx.datastore", name = "datastore-preferences", version.ref = "datastore" }
androidx-navigation-compose = { group = "androidx.navigation", name = "navigation-compose", version.ref = "navigationCompose" }
androidx-lifecycle-viewmodel-compose = { group = "androidx.lifecycle", name = "lifecycle-viewmodel-compose", version.ref = "lifecycleViewmodelCompose" }
androidx-lifecycle-runtime-compose = { group = "androidx.lifecycle", name = "lifecycle-runtime-compose", version.ref = "lifecycleRuntimeKtx" }
kotlinx-coroutines-android = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-android", version.ref = "coroutines" }
kotlinx-coroutines-test = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-test", version.ref = "coroutines" }
traccar-client-sdk = { group = "org.traccar", name = "traccar-client-sdk", version = "0.0.1-SNAPSHOT" }
camerax-core = { group = "androidx.camera", name = "camera-core", version.ref = "camerax" }
camerax-camera2 = { group = "androidx.camera", name = "camera-camera2", version.ref = "camerax" }
camerax-lifecycle = { group = "androidx.camera", name = "camera-lifecycle", version.ref = "camerax" }
camerax-view = { group = "androidx.camera", name = "camera-view", version.ref = "camerax" }
mlkit-barcode = { group = "com.google.mlkit", name = "barcode-scanning", version.ref = "mlkitBarcode" }
zxing-core = { group = "com.google.zxing", name = "core", version.ref = "zxing" }

[plugins]
# ...keep existing android-application, kotlin-compose...
hilt = { id = "com.google.dagger.hilt.android", version.ref = "hilt" }
ksp = { id = "com.google.devtools.ksp", version.ref = "ksp" }
```

- [ ] **Step 2: Register plugins in root `build.gradle.kts`.** Ensure the root declares the new plugins with `apply false`:

```kotlin
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.ksp) apply false
}
```

(If the root has no `plugins {}` block, add it. Keep any existing content.)

- [ ] **Step 3: Update `app/build.gradle.kts`.** Add plugins and dependencies:

```kotlin
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
    kotlin("android")
}
```

Wait — Compose already brings the Kotlin android plugin transitively via the compose plugin? It does not. The existing file uses `alias(libs.plugins.kotlin.compose)` which is the compose *compiler* plugin and requires the Kotlin Android plugin to also be applied. Add a `kotlin-android` alias.

In `libs.versions.toml [plugins]` add:

```toml
kotlin-android = { id = "org.jetbrains.kotlin.android", version.ref = "kotlin" }
```

Then in `app/build.gradle.kts` plugins block use `alias(libs.plugins.kotlin.android)` instead of the raw `kotlin("android")`:

```kotlin
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}
```

Add to the `dependencies {}` block:

```kotlin
implementation(libs.traccar.client.sdk)

implementation(libs.hilt.android)
ksp(libs.hilt.compiler)
implementation(libs.androidx.hilt.navigation.compose)

implementation(libs.androidx.datastore.preferences)
implementation(libs.androidx.navigation.compose)
implementation(libs.androidx.lifecycle.viewmodel.compose)
implementation(libs.androidx.lifecycle.runtime.compose)
implementation(libs.kotlinx.coroutines.android)

implementation(libs.camerax.core)
implementation(libs.camerax.camera2)
implementation(libs.camerax.lifecycle)
implementation(libs.camerax.view)
implementation(libs.mlkit.barcode)
implementation(libs.zxing.core)

testImplementation(libs.kotlinx.coroutines.test)
```

- [ ] **Step 4: Sync-build to confirm the toolchain resolves.**

Run: `cd whereami_android && ./gradlew help`
Expected: `BUILD SUCCESSFUL` (dependencies not yet used, but plugins/catalog must resolve).

- [ ] **Step 5: Commit.**

```bash
git add gradle/libs.versions.toml app/build.gradle.kts build.gradle.kts
git commit -m "build: upgrade to Kotlin 2.3.21 and add Hilt/DataStore/CameraX/ZXing deps"
```

---

### Task 2: Wire the SDK via composite build

**Files:**
- Modify: `settings.gradle.kts`

**Interfaces:**
- Produces: resolvable `org.traccar:traccar-client-sdk` dependency backed by the local `:core` project.

- [ ] **Step 1: Add `includeBuild` to `settings.gradle.kts`.** After the `dependencyResolutionManagement { ... }` block, add:

```kotlin
includeBuild("../traccar-client-sdk")
```

The composite build substitutes the `org.traccar:traccar-client-sdk` coordinate (declared in Task 1) with the local `:core` project automatically.

- [ ] **Step 2: Resolve dependencies to prove substitution works.**

Run: `cd whereami_android && ./gradlew :app:dependencies --configuration debugRuntimeClasspath`
Expected: output shows `org.traccar:traccar-client-sdk -> project :core` (substituted). BUILD SUCCESSFUL.

- [ ] **Step 3: Full assemble to confirm the SDK AAR builds and links.**

Run: `cd whereami_android && ./gradlew :app:assembleDebug`
Expected: `BUILD SUCCESSFUL`. If it fails on Kotlin metadata version, re-check Task 1 set `kotlin = "2.3.21"`.

- [ ] **Step 4: Commit.**

```bash
git add settings.gradle.kts
git commit -m "build: consume traccar-client-sdk via composite includeBuild"
```

---

### Task 3: Hilt application + dispatcher module

**Files:**
- Create: `app/src/main/java/com/anim/where/am/i/WhereAmIApp.kt`
- Modify: `app/src/main/java/com/anim/where/am/i/MainActivity.kt`
- Create: `app/src/main/java/com/anim/where/am/i/di/DispatcherModule.kt`
- Create: `app/src/main/java/com/anim/where/am/i/di/Dispatcher.kt`
- Modify: `app/src/main/AndroidManifest.xml`

**Interfaces:**
- Produces: `@HiltAndroidApp WhereAmIApp`; qualifier annotations `@IoDispatcher`, `@DefaultDispatcher`; Hilt graph rooted in the app.

- [ ] **Step 1: Create the dispatcher qualifiers `di/Dispatcher.kt`.**

```kotlin
package com.anim.where.am.i.di

import javax.inject.Qualifier

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class IoDispatcher

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class DefaultDispatcher
```

- [ ] **Step 2: Create `di/DispatcherModule.kt`.**

```kotlin
package com.anim.where.am.i.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

@Module
@InstallIn(SingletonComponent::class)
object DispatcherModule {
    @Provides @IoDispatcher fun io(): CoroutineDispatcher = Dispatchers.IO
    @Provides @DefaultDispatcher fun default(): CoroutineDispatcher = Dispatchers.Default
}
```

- [ ] **Step 3: Create `WhereAmIApp.kt`.**

```kotlin
package com.anim.where.am.i

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class WhereAmIApp : Application()
```

- [ ] **Step 4: Annotate `MainActivity` with `@AndroidEntryPoint`.** Add the import and annotation above the class declaration; leave the rest untouched for now:

```kotlin
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() { /* existing body */ }
```

- [ ] **Step 5: Register the Application in `AndroidManifest.xml`.** Add `android:name=".WhereAmIApp"` to the `<application>` tag.

- [ ] **Step 6: Build to confirm Hilt code-gen succeeds.**

Run: `cd whereami_android && ./gradlew :app:assembleDebug`
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 7: Commit.**

```bash
git add app/src/main/java/com/anim/where/am/i/ app/src/main/AndroidManifest.xml
git commit -m "feat: add Hilt application, dispatcher module, entry point"
```

---

## Phase 1 — Domain

### Task 4: Domain models

**Files:**
- Create: `app/src/main/java/com/anim/where/am/i/domain/model/Accuracy.kt`
- Create: `app/src/main/java/com/anim/where/am/i/domain/model/TrackingSettings.kt`
- Create: `app/src/main/java/com/anim/where/am/i/domain/model/LocationFix.kt`
- Create: `app/src/main/java/com/anim/where/am/i/domain/model/TrackingStatus.kt`
- Create: `app/src/main/java/com/anim/where/am/i/domain/model/LogItem.kt`
- Create: `app/src/main/java/com/anim/where/am/i/domain/model/ConfigLink.kt`
- Test: `app/src/test/java/com/anim/where/am/i/domain/model/ConfigLinkTest.kt`

**Interfaces:**
- Produces: all domain model types used everywhere. `ConfigLink.applyTo(TrackingSettings): TrackingSettings`.

- [ ] **Step 1: Write the failing test `ConfigLinkTest.kt`.**

```kotlin
package com.anim.where.am.i.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals

class ConfigLinkTest {
    private val base = TrackingSettings(serverUrl = "https://a", deviceId = "1")

    @Test fun applyTo_overridesOnlyPresentFields() {
        val link = ConfigLink(serverUrl = "https://b", intervalSeconds = 60)
        val result = link.applyTo(base)
        assertEquals("https://b", result.serverUrl)
        assertEquals(60, result.intervalSeconds)
        assertEquals("1", result.deviceId)        // unchanged
        assertEquals(75, result.distanceMeters)   // unchanged default
    }

    @Test fun applyTo_emptyLinkKeepsEverything() {
        assertEquals(base, ConfigLink().applyTo(base))
    }
}
```

- [ ] **Step 2: Run test, expect compile failure (types not defined).**

Run: `cd whereami_android && ./gradlew :app:testDebugUnitTest --tests "*ConfigLinkTest"`
Expected: FAIL — unresolved references.

- [ ] **Step 3: Create the model files.**

`Accuracy.kt`:
```kotlin
package com.anim.where.am.i.domain.model

enum class Accuracy { HIGHEST, HIGH, MEDIUM, LOW }
```

`TrackingSettings.kt`:
```kotlin
package com.anim.where.am.i.domain.model

data class TrackingSettings(
    val serverUrl: String,
    val deviceId: String,
    val accuracy: Accuracy = Accuracy.MEDIUM,
    val distanceMeters: Int = 75,
    val intervalSeconds: Int = 300,
    val angleDegrees: Int = 0,
    val heartbeatSeconds: Int = 0,
    val buffer: Boolean = true,
    val wakeLock: Boolean = false,
    val stopDetection: Boolean = true,
    val preferPlatformProviders: Boolean = false,
)
```

`LocationFix.kt`:
```kotlin
package com.anim.where.am.i.domain.model

data class LocationFix(
    val latitude: Double?,
    val longitude: Double?,
    val accuracy: Double?,
    val timeMillis: Long,
)
```

`TrackingStatus.kt`:
```kotlin
package com.anim.where.am.i.domain.model

data class TrackingStatus(
    val enabled: Boolean = false,
    val paused: Boolean = false,
    val lastLocation: LocationFix? = null,
)
```

`LogItem.kt`:
```kotlin
package com.anim.where.am.i.domain.model

data class LogItem(val timeMillis: Long, val message: String)
```

`ConfigLink.kt`:
```kotlin
package com.anim.where.am.i.domain.model

data class ConfigLink(
    val serverUrl: String? = null,
    val deviceId: String? = null,
    val accuracy: Accuracy? = null,
    val distanceMeters: Int? = null,
    val intervalSeconds: Int? = null,
    val angleDegrees: Int? = null,
    val heartbeatSeconds: Int? = null,
    val buffer: Boolean? = null,
    val wakeLock: Boolean? = null,
    val stopDetection: Boolean? = null,
    val preferPlatformProviders: Boolean? = null,
) {
    fun applyTo(settings: TrackingSettings): TrackingSettings = settings.copy(
        serverUrl = serverUrl ?: settings.serverUrl,
        deviceId = deviceId ?: settings.deviceId,
        accuracy = accuracy ?: settings.accuracy,
        distanceMeters = distanceMeters ?: settings.distanceMeters,
        intervalSeconds = intervalSeconds ?: settings.intervalSeconds,
        angleDegrees = angleDegrees ?: settings.angleDegrees,
        heartbeatSeconds = heartbeatSeconds ?: settings.heartbeatSeconds,
        buffer = buffer ?: settings.buffer,
        wakeLock = wakeLock ?: settings.wakeLock,
        stopDetection = stopDetection ?: settings.stopDetection,
        preferPlatformProviders = preferPlatformProviders ?: settings.preferPlatformProviders,
    )
}
```

- [ ] **Step 4: Run test, expect PASS.**

Run: `cd whereami_android && ./gradlew :app:testDebugUnitTest --tests "*ConfigLinkTest"`
Expected: PASS.

- [ ] **Step 5: Commit.**

```bash
git add app/src/main/java/com/anim/where/am/i/domain/model app/src/test/java/com/anim/where/am/i/domain/model
git commit -m "feat: add domain models"
```

---

### Task 5: Repository interfaces + tracker action enum

**Files:**
- Create: `app/src/main/java/com/anim/where/am/i/domain/repository/TrackerRepository.kt`
- Create: `app/src/main/java/com/anim/where/am/i/domain/repository/SettingsRepository.kt`
- Create: `app/src/main/java/com/anim/where/am/i/domain/repository/LogRepository.kt`
- Create: `app/src/main/java/com/anim/where/am/i/domain/model/TrackerAction.kt`

**Interfaces:**
- Produces:
  - `TrackerRepository { fun observeStatus(): Flow<TrackingStatus>; suspend fun start(); suspend fun stop(); suspend fun requestPosition(alarm: String? = null): Boolean; suspend fun updateConfig(settings: TrackingSettings) }`
  - `SettingsRepository { fun observeSettings(): Flow<TrackingSettings>; suspend fun save(settings: TrackingSettings) }`
  - `LogRepository { suspend fun getLogs(): List<LogItem>; suspend fun clear() }`
  - `enum class TrackerAction { START, STOP, SOS }`

- [ ] **Step 1: Create `TrackerAction.kt`.**

```kotlin
package com.anim.where.am.i.domain.model

enum class TrackerAction { START, STOP, SOS }
```

- [ ] **Step 2: Create `TrackerRepository.kt`.**

```kotlin
package com.anim.where.am.i.domain.repository

import com.anim.where.am.i.domain.model.TrackingSettings
import com.anim.where.am.i.domain.model.TrackingStatus
import kotlinx.coroutines.flow.Flow

interface TrackerRepository {
    fun observeStatus(): Flow<TrackingStatus>
    suspend fun start()
    suspend fun stop()
    suspend fun requestPosition(alarm: String? = null): Boolean
    suspend fun updateConfig(settings: TrackingSettings)
}
```

- [ ] **Step 3: Create `SettingsRepository.kt`.**

```kotlin
package com.anim.where.am.i.domain.repository

import com.anim.where.am.i.domain.model.TrackingSettings
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    fun observeSettings(): Flow<TrackingSettings>
    suspend fun save(settings: TrackingSettings)
}
```

- [ ] **Step 4: Create `LogRepository.kt`.**

```kotlin
package com.anim.where.am.i.domain.repository

import com.anim.where.am.i.domain.model.LogItem

interface LogRepository {
    suspend fun getLogs(): List<LogItem>
    suspend fun clear()
}
```

- [ ] **Step 5: Compile.**

Run: `cd whereami_android && ./gradlew :app:compileDebugKotlin`
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 6: Commit.**

```bash
git add app/src/main/java/com/anim/where/am/i/domain
git commit -m "feat: add domain repository interfaces"
```

---

### Task 6: Use cases

**Files:**
- Create: `app/src/main/java/com/anim/where/am/i/domain/usecase/TrackingUseCases.kt`
- Create: `app/src/main/java/com/anim/where/am/i/domain/usecase/SettingsUseCases.kt`
- Create: `app/src/main/java/com/anim/where/am/i/domain/usecase/LogUseCases.kt`
- Create: `app/src/main/java/com/anim/where/am/i/domain/usecase/ConfigLinkUseCases.kt`
- Test: `app/src/test/java/com/anim/where/am/i/domain/usecase/SaveSettingsTest.kt`

**Interfaces:**
- Consumes: repositories from Task 5, models from Task 4.
- Produces:
  - `ObserveTrackingStatus()` → `Flow<TrackingStatus>`; `StartTracking()`, `StopTracking()`, `RequestSos()` suspend; `RunTrackerAction(action: TrackerAction)` suspend.
  - `ObserveSettings()` → `Flow<TrackingSettings>`; `SaveSettings(settings)` suspend — **normalizes heartbeat: any value in 1..59 becomes 60** — then persists and pushes config to the tracker.
  - `GetLogs()` → `List<LogItem>`; `ClearLogs()` suspend.
  - `ApplyConfigLink(link: ConfigLink)` suspend; `BuildConfigLink()` → uses current settings (returns `TrackingSettings`) via injected repo — actual URI building is Task 11's `ConfigLinkBuilder`; this use case just exposes current settings. (Kept minimal.)

- [ ] **Step 1: Write the failing test `SaveSettingsTest.kt`.**

```kotlin
package com.anim.where.am.i.domain.usecase

import com.anim.where.am.i.domain.model.TrackingSettings
import com.anim.where.am.i.domain.repository.SettingsRepository
import com.anim.where.am.i.domain.repository.TrackerRepository
import com.anim.where.am.i.domain.model.TrackingStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class SaveSettingsTest {
    private var saved: TrackingSettings? = null
    private var pushed: TrackingSettings? = null

    private val settingsRepo = object : SettingsRepository {
        override fun observeSettings(): Flow<TrackingSettings> = MutableStateFlow(base)
        override suspend fun save(settings: TrackingSettings) { saved = settings }
    }
    private val trackerRepo = object : TrackerRepository {
        override fun observeStatus(): Flow<TrackingStatus> = MutableStateFlow(TrackingStatus())
        override suspend fun start() {}
        override suspend fun stop() {}
        override suspend fun requestPosition(alarm: String?) = true
        override suspend fun updateConfig(settings: TrackingSettings) { pushed = settings }
    }
    private val base = TrackingSettings(serverUrl = "https://a", deviceId = "1")

    @Test fun clampsHeartbeatBelowMinimum() = runTest {
        SaveSettings(settingsRepo, trackerRepo)(base.copy(heartbeatSeconds = 30))
        assertEquals(60, saved?.heartbeatSeconds)
        assertEquals(60, pushed?.heartbeatSeconds)
    }

    @Test fun keepsZeroHeartbeat() = runTest {
        SaveSettings(settingsRepo, trackerRepo)(base.copy(heartbeatSeconds = 0))
        assertEquals(0, saved?.heartbeatSeconds)
    }
}
```

- [ ] **Step 2: Run test, expect compile failure.**

Run: `cd whereami_android && ./gradlew :app:testDebugUnitTest --tests "*SaveSettingsTest"`
Expected: FAIL — `SaveSettings` unresolved.

- [ ] **Step 3: Create the use case files.**

`TrackingUseCases.kt`:
```kotlin
package com.anim.where.am.i.domain.usecase

import com.anim.where.am.i.domain.model.TrackerAction
import com.anim.where.am.i.domain.model.TrackingStatus
import com.anim.where.am.i.domain.repository.TrackerRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveTrackingStatus @Inject constructor(private val repo: TrackerRepository) {
    operator fun invoke(): Flow<TrackingStatus> = repo.observeStatus()
}

class StartTracking @Inject constructor(private val repo: TrackerRepository) {
    suspend operator fun invoke() = repo.start()
}

class StopTracking @Inject constructor(private val repo: TrackerRepository) {
    suspend operator fun invoke() = repo.stop()
}

class RequestSos @Inject constructor(private val repo: TrackerRepository) {
    suspend operator fun invoke(): Boolean = repo.requestPosition(alarm = "sos")
}

class RunTrackerAction @Inject constructor(
    private val start: StartTracking,
    private val stop: StopTracking,
    private val sos: RequestSos,
) {
    suspend operator fun invoke(action: TrackerAction) = when (action) {
        TrackerAction.START -> start()
        TrackerAction.STOP -> stop()
        TrackerAction.SOS -> { sos(); Unit }
    }
}
```

`SettingsUseCases.kt`:
```kotlin
package com.anim.where.am.i.domain.usecase

import com.anim.where.am.i.domain.model.TrackingSettings
import com.anim.where.am.i.domain.repository.SettingsRepository
import com.anim.where.am.i.domain.repository.TrackerRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveSettings @Inject constructor(private val repo: SettingsRepository) {
    operator fun invoke(): Flow<TrackingSettings> = repo.observeSettings()
}

class SaveSettings @Inject constructor(
    private val settingsRepo: SettingsRepository,
    private val trackerRepo: TrackerRepository,
) {
    suspend operator fun invoke(settings: TrackingSettings) {
        val normalized = settings.copy(
            heartbeatSeconds = if (settings.heartbeatSeconds in 1..59) 60 else settings.heartbeatSeconds,
        )
        settingsRepo.save(normalized)
        trackerRepo.updateConfig(normalized)
    }
}
```

`LogUseCases.kt`:
```kotlin
package com.anim.where.am.i.domain.usecase

import com.anim.where.am.i.domain.model.LogItem
import com.anim.where.am.i.domain.repository.LogRepository
import javax.inject.Inject

class GetLogs @Inject constructor(private val repo: LogRepository) {
    suspend operator fun invoke(): List<LogItem> = repo.getLogs()
}

class ClearLogs @Inject constructor(private val repo: LogRepository) {
    suspend operator fun invoke() = repo.clear()
}
```

`ConfigLinkUseCases.kt`:
```kotlin
package com.anim.where.am.i.domain.usecase

import com.anim.where.am.i.domain.model.ConfigLink
import com.anim.where.am.i.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class ApplyConfigLink @Inject constructor(
    private val settingsRepo: SettingsRepository,
    private val saveSettings: SaveSettings,
) {
    suspend operator fun invoke(link: ConfigLink) {
        val current = settingsRepo.observeSettings().first()
        saveSettings(link.applyTo(current))
    }
}
```

- [ ] **Step 4: Run test, expect PASS.**

Run: `cd whereami_android && ./gradlew :app:testDebugUnitTest --tests "*SaveSettingsTest"`
Expected: PASS.

- [ ] **Step 5: Commit.**

```bash
git add app/src/main/java/com/anim/where/am/i/domain/usecase app/src/test/java/com/anim/where/am/i/domain/usecase
git commit -m "feat: add domain use cases with heartbeat normalization"
```

---

## Phase 2 — Data

### Task 7: Settings ↔ SDK Config mapper

**Files:**
- Create: `app/src/main/java/com/anim/where/am/i/data/tracker/ConfigMapper.kt`
- Test: `app/src/test/java/com/anim/where/am/i/data/tracker/ConfigMapperTest.kt`

**Interfaces:**
- Consumes: `TrackingSettings`, domain `Accuracy` (Task 4); SDK `Config`, `LocationConfig`, `NotificationConfig`, `Accuracy`, `State`, `Position` (from `org.traccar.client`).
- Produces:
  - `fun TrackingSettings.toConfig(notificationText: String): Config`
  - `fun com.anim.where.am.i.domain.model.Accuracy.toSdk(): org.traccar.client.Accuracy`
  - `fun org.traccar.client.State.toStatus(): TrackingStatus`
  - `fun org.traccar.client.Position.toLocationFix(): LocationFix`

- [ ] **Step 1: Write the failing test `ConfigMapperTest.kt`.**

```kotlin
package com.anim.where.am.i.data.tracker

import com.anim.where.am.i.domain.model.Accuracy
import com.anim.where.am.i.domain.model.TrackingSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import org.traccar.client.Accuracy as SdkAccuracy

class ConfigMapperTest {
    @Test fun mapsCoreFields() {
        val settings = TrackingSettings(
            serverUrl = "https://x", deviceId = "42",
            accuracy = Accuracy.HIGH, distanceMeters = 10, intervalSeconds = 120,
            angleDegrees = 5, heartbeatSeconds = 60, buffer = false, wakeLock = true,
            stopDetection = false, preferPlatformProviders = true,
        )
        val config = settings.toConfig("Tracking")
        assertEquals("https://x", config.serverUrl)
        assertEquals("42", config.deviceId)
        assertEquals(SdkAccuracy.HIGH, config.location.accuracy)
        assertEquals(10, config.location.distanceMeters)
        assertEquals(120, config.location.intervalSeconds)
        assertEquals(5, config.location.angleDegrees)
        assertEquals(60, config.location.heartbeatIntervalSeconds)
        assertEquals(false, config.location.stopDetection)
        assertEquals(false, config.buffer)
        assertEquals(true, config.wakeLock)
        assertEquals(true, config.preferPlatformProviders)
        assertEquals("Tracking", config.notification.text)
    }
}
```

- [ ] **Step 2: Run test, expect FAIL (unresolved `toConfig`).**

Run: `cd whereami_android && ./gradlew :app:testDebugUnitTest --tests "*ConfigMapperTest"`
Expected: FAIL.

- [ ] **Step 3: Create `ConfigMapper.kt`.**

```kotlin
package com.anim.where.am.i.data.tracker

import com.anim.where.am.i.domain.model.LocationFix
import com.anim.where.am.i.domain.model.TrackingSettings
import com.anim.where.am.i.domain.model.TrackingStatus
import org.traccar.client.Config
import org.traccar.client.LocationConfig
import org.traccar.client.NotificationConfig
import org.traccar.client.Position
import org.traccar.client.State
import com.anim.where.am.i.domain.model.Accuracy as DomainAccuracy
import org.traccar.client.Accuracy as SdkAccuracy

fun DomainAccuracy.toSdk(): SdkAccuracy = when (this) {
    DomainAccuracy.HIGHEST -> SdkAccuracy.HIGHEST
    DomainAccuracy.HIGH -> SdkAccuracy.HIGH
    DomainAccuracy.MEDIUM -> SdkAccuracy.MEDIUM
    DomainAccuracy.LOW -> SdkAccuracy.LOW
}

fun TrackingSettings.toConfig(notificationText: String): Config = Config(
    serverUrl = serverUrl,
    deviceId = deviceId,
    location = LocationConfig(
        accuracy = accuracy.toSdk(),
        distanceMeters = distanceMeters,
        intervalSeconds = intervalSeconds,
        angleDegrees = angleDegrees,
        stopDetection = stopDetection,
        heartbeatIntervalSeconds = heartbeatSeconds,
    ),
    wakeLock = wakeLock,
    buffer = buffer,
    preferPlatformProviders = preferPlatformProviders,
    notification = NotificationConfig(text = notificationText),
)

fun Position.toLocationFix(): LocationFix =
    LocationFix(latitude = latitude, longitude = longitude, accuracy = accuracy, timeMillis = time)

fun State.toStatus(): TrackingStatus =
    TrackingStatus(enabled = enabled, paused = paused, lastLocation = lastAcceptedLocation?.toLocationFix())
```

- [ ] **Step 4: Run test, expect PASS.**

Run: `cd whereami_android && ./gradlew :app:testDebugUnitTest --tests "*ConfigMapperTest"`
Expected: PASS.

- [ ] **Step 5: Commit.**

```bash
git add app/src/main/java/com/anim/where/am/i/data/tracker/ConfigMapper.kt app/src/test/java/com/anim/where/am/i/data/tracker/ConfigMapperTest.kt
git commit -m "feat: add TrackingSettings<->SDK Config mapper"
```

---

### Task 8: SettingsRepository backed by DataStore

**Files:**
- Create: `app/src/main/java/com/anim/where/am/i/data/settings/SettingsRepositoryImpl.kt`
- Create: `app/src/main/java/com/anim/where/am/i/data/settings/SettingsKeys.kt`
- Test: `app/src/test/java/com/anim/where/am/i/data/settings/SettingsSerializationTest.kt`

**Interfaces:**
- Consumes: `SettingsRepository` (Task 5), `TrackingSettings` + `Accuracy` (Task 4), `@IoDispatcher` (Task 3).
- Produces: `SettingsRepositoryImpl(dataStore: DataStore<Preferences>, io: CoroutineDispatcher) : SettingsRepository`. First read with no stored deviceId seeds defaults (Global Constraints). Exposes internal pure helpers `settingsFromPreferences(Preferences): TrackingSettings?` and `accuracyFromKey(String?): Accuracy` / `accuracyToKey(Accuracy): String` for testing.

- [ ] **Step 1: Write the failing test `SettingsSerializationTest.kt`** (tests the pure accuracy mapping helpers — DataStore I/O is covered by the build + manual run, not unit-tested here):

```kotlin
package com.anim.where.am.i.data.settings

import com.anim.where.am.i.domain.model.Accuracy
import kotlin.test.Test
import kotlin.test.assertEquals

class SettingsSerializationTest {
    @Test fun accuracyKeyRoundTrips() {
        for (a in Accuracy.entries) {
            assertEquals(a, accuracyFromKey(accuracyToKey(a)))
        }
    }

    @Test fun unknownAccuracyDefaultsToMedium() {
        assertEquals(Accuracy.MEDIUM, accuracyFromKey("bogus"))
        assertEquals(Accuracy.MEDIUM, accuracyFromKey(null))
    }
}
```

- [ ] **Step 2: Run test, expect FAIL.**

Run: `cd whereami_android && ./gradlew :app:testDebugUnitTest --tests "*SettingsSerializationTest"`
Expected: FAIL.

- [ ] **Step 3: Create `SettingsKeys.kt`** with keys and the pure helpers:

```kotlin
package com.anim.where.am.i.data.settings

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.anim.where.am.i.domain.model.Accuracy

internal object SettingsKeys {
    val URL = stringPreferencesKey("url")
    val ID = stringPreferencesKey("id")
    val ACCURACY = stringPreferencesKey("accuracy")
    val DISTANCE = intPreferencesKey("distance")
    val INTERVAL = intPreferencesKey("interval")
    val ANGLE = intPreferencesKey("angle")
    val HEARTBEAT = intPreferencesKey("heartbeat")
    val BUFFER = booleanPreferencesKey("buffer")
    val WAKELOCK = booleanPreferencesKey("wakelock")
    val STOP_DETECTION = booleanPreferencesKey("stop_detection")
    val PREFER_PLATFORM = booleanPreferencesKey("prefer_platform_providers")
}

internal fun accuracyToKey(a: Accuracy): String = a.name.lowercase()

internal fun accuracyFromKey(key: String?): Accuracy = when (key) {
    "highest" -> Accuracy.HIGHEST
    "high" -> Accuracy.HIGH
    "low" -> Accuracy.LOW
    else -> Accuracy.MEDIUM
}
```

- [ ] **Step 4: Create `SettingsRepositoryImpl.kt`.**

```kotlin
package com.anim.where.am.i.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.anim.where.am.i.di.IoDispatcher
import com.anim.where.am.i.domain.model.TrackingSettings
import com.anim.where.am.i.domain.repository.SettingsRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlin.random.Random
import javax.inject.Inject

private const val DEFAULT_URL = "https://demo.traccar.org"

class SettingsRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    @IoDispatcher private val io: CoroutineDispatcher,
) : SettingsRepository {

    override fun observeSettings(): Flow<TrackingSettings> =
        dataStore.data.map { prefs -> prefs.toSettings() }

    override suspend fun save(settings: TrackingSettings) = withContext(io) {
        dataStore.edit { prefs ->
            prefs[SettingsKeys.URL] = settings.serverUrl
            prefs[SettingsKeys.ID] = settings.deviceId
            prefs[SettingsKeys.ACCURACY] = accuracyToKey(settings.accuracy)
            prefs[SettingsKeys.DISTANCE] = settings.distanceMeters
            prefs[SettingsKeys.INTERVAL] = settings.intervalSeconds
            prefs[SettingsKeys.ANGLE] = settings.angleDegrees
            prefs[SettingsKeys.HEARTBEAT] = settings.heartbeatSeconds
            prefs[SettingsKeys.BUFFER] = settings.buffer
            prefs[SettingsKeys.WAKELOCK] = settings.wakeLock
            prefs[SettingsKeys.STOP_DETECTION] = settings.stopDetection
            prefs[SettingsKeys.PREFER_PLATFORM] = settings.preferPlatformProviders
        }
        Unit
    }

    private fun Preferences.toSettings(): TrackingSettings {
        val id = this[SettingsKeys.ID] ?: Random.nextInt(10_000_000, 100_000_000).toString()
        return TrackingSettings(
            serverUrl = this[SettingsKeys.URL] ?: DEFAULT_URL,
            deviceId = id,
            accuracy = accuracyFromKey(this[SettingsKeys.ACCURACY]),
            distanceMeters = this[SettingsKeys.DISTANCE] ?: 75,
            intervalSeconds = this[SettingsKeys.INTERVAL] ?: 300,
            angleDegrees = this[SettingsKeys.ANGLE] ?: 0,
            heartbeatSeconds = this[SettingsKeys.HEARTBEAT] ?: 0,
            buffer = this[SettingsKeys.BUFFER] ?: true,
            wakeLock = this[SettingsKeys.WAKELOCK] ?: false,
            stopDetection = this[SettingsKeys.STOP_DETECTION] ?: true,
            preferPlatformProviders = this[SettingsKeys.PREFER_PLATFORM] ?: false,
        )
    }
}
```

Note: the seeded random `deviceId` is only persisted once the app first calls `save` (e.g. after first config apply or when the user opens settings). Task 12 wires an initial persistence on startup.

- [ ] **Step 5: Run test, expect PASS.**

Run: `cd whereami_android && ./gradlew :app:testDebugUnitTest --tests "*SettingsSerializationTest"`
Expected: PASS.

- [ ] **Step 6: Commit.**

```bash
git add app/src/main/java/com/anim/where/am/i/data/settings app/src/test/java/com/anim/where/am/i/data/settings
git commit -m "feat: add DataStore-backed SettingsRepository"
```

---

### Task 9: TrackerRepository wrapping the SDK

**Files:**
- Create: `app/src/main/java/com/anim/where/am/i/data/tracker/TrackerRepositoryImpl.kt`

**Interfaces:**
- Consumes: SDK `Tracker`, `sharedTracker(config)`, `Tracker.startTracking(context)`, `Tracker.state` (from `org.traccar.client`); `ConfigMapper` (Task 7); `TrackingSettings`, `TrackingStatus` (Task 4); `@IoDispatcher` (Task 3).
- Produces: `TrackerRepositoryImpl(context: Context, io: CoroutineDispatcher, notificationText: String) : TrackerRepository`. Lazily bootstraps the shared `Tracker` from persisted config (or a bootstrap config passed by DI), holds it in a `MutableStateFlow<Tracker?>`, and flat-maps status.

- [ ] **Step 1: Create `TrackerRepositoryImpl.kt`.**

```kotlin
package com.anim.where.am.i.data.tracker

import android.content.Context
import com.anim.where.am.i.di.IoDispatcher
import com.anim.where.am.i.domain.model.TrackingSettings
import com.anim.where.am.i.domain.model.TrackingStatus
import com.anim.where.am.i.domain.repository.TrackerRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.traccar.client.Tracker
import org.traccar.client.sharedTracker
import org.traccar.client.startTracking

class TrackerRepositoryImpl(
    private val context: Context,
    @IoDispatcher private val io: CoroutineDispatcher,
    private val notificationText: String,
    private val bootstrapSettings: TrackingSettings,
) : TrackerRepository {

    private val trackerFlow = MutableStateFlow<Tracker?>(null)
    private val mutex = Mutex()

    private suspend fun tracker(): Tracker = mutex.withLock {
        trackerFlow.value ?: withContext(io) {
            sharedTracker(bootstrapSettings.toConfig(notificationText)).also { trackerFlow.value = it }
        }
    }

    override fun observeStatus(): Flow<TrackingStatus> =
        trackerFlow.flatMapLatest { t ->
            if (t == null) flow { emitAll(ensureThenState()) } else t.state.map { it.toStatus() }
        }

    private fun ensureThenState(): Flow<TrackingStatus> = flow {
        val t = tracker()
        emitAll(t.state.map { it.toStatus() })
    }

    override suspend fun start() {
        tracker().startTracking(context)
    }

    override suspend fun stop() {
        tracker().stop()
    }

    override suspend fun requestPosition(alarm: String?): Boolean =
        org.traccar.client.requestPosition(tracker(), context, alarm)

    override suspend fun updateConfig(settings: TrackingSettings) = mutex.withLock {
        val current = trackerFlow.value ?: sharedTracker(settings.toConfig(notificationText))
        trackerFlow.value = current.updateConfig(settings.toConfig(notificationText))
    }
}
```

Note on `requestPosition`: the SDK exposes it two ways — `Tracker.requestPosition(alarm)` (no permission guard) and the extension `Tracker.requestPosition(context, alarm)` (permission-guarded). Use the extension. Correct the call to the extension form:

```kotlin
    override suspend fun requestPosition(alarm: String?): Boolean =
        tracker().requestPosition(context, alarm)
```

Ensure the import `import org.traccar.client.requestPosition` is present so the `(context, alarm)` extension resolves.

- [ ] **Step 2: Add `@OptIn` / opt-in for `flatMapLatest`** if the compiler flags it experimental — add `@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)` at the top of the file.

- [ ] **Step 3: Compile.**

Run: `cd whereami_android && ./gradlew :app:compileDebugKotlin`
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 4: Commit.**

```bash
git add app/src/main/java/com/anim/where/am/i/data/tracker/TrackerRepositoryImpl.kt
git commit -m "feat: add SDK-backed TrackerRepository"
```

---

### Task 10: LogRepository + log mapper

**Files:**
- Create: `app/src/main/java/com/anim/where/am/i/data/log/LogRepositoryImpl.kt`
- Test: `app/src/test/java/com/anim/where/am/i/data/log/LogMapperTest.kt`

**Interfaces:**
- Consumes: SDK `Tracker.getLogs()`, `Tracker.clearLogs()`, `LogEntry`; `LogItem` (Task 4); `TrackerRepositoryImpl`'s tracker access. To avoid exposing the SDK `Tracker`, `LogRepositoryImpl` takes a `suspend () -> Tracker` provider.
- Produces: `fun LogEntry.toLogItem(): LogItem`; `LogRepositoryImpl(trackerProvider: suspend () -> Tracker) : LogRepository`.

- [ ] **Step 1: Write the failing test `LogMapperTest.kt`.**

```kotlin
package com.anim.where.am.i.data.log

import kotlin.test.Test
import kotlin.test.assertEquals
import org.traccar.client.LogEntry

class LogMapperTest {
    @Test fun mapsFields() {
        val item = LogEntry(time = 123L, message = "hello").toLogItem()
        assertEquals(123L, item.timeMillis)
        assertEquals("hello", item.message)
    }
}
```

- [ ] **Step 2: Run test, expect FAIL.**

Run: `cd whereami_android && ./gradlew :app:testDebugUnitTest --tests "*LogMapperTest"`
Expected: FAIL.

- [ ] **Step 3: Create `LogRepositoryImpl.kt`.**

```kotlin
package com.anim.where.am.i.data.log

import com.anim.where.am.i.domain.model.LogItem
import com.anim.where.am.i.domain.repository.LogRepository
import org.traccar.client.LogEntry
import org.traccar.client.Tracker

fun LogEntry.toLogItem(): LogItem = LogItem(timeMillis = time, message = message)

class LogRepositoryImpl(
    private val trackerProvider: suspend () -> Tracker,
) : LogRepository {
    override suspend fun getLogs(): List<LogItem> =
        trackerProvider().getLogs().map { it.toLogItem() }

    override suspend fun clear() {
        trackerProvider().clearLogs()
    }
}
```

- [ ] **Step 4: Run test, expect PASS.**

Run: `cd whereami_android && ./gradlew :app:testDebugUnitTest --tests "*LogMapperTest"`
Expected: PASS.

- [ ] **Step 5: Commit.**

```bash
git add app/src/main/java/com/anim/where/am/i/data/log app/src/test/java/com/anim/where/am/i/data/log
git commit -m "feat: add LogRepository and log mapper"
```

---

### Task 11: Config link parser + builder

**Files:**
- Create: `app/src/main/java/com/anim/where/am/i/data/config/ConfigLinkParser.kt`
- Create: `app/src/main/java/com/anim/where/am/i/data/config/ConfigLinkBuilder.kt`
- Create: `app/src/main/java/com/anim/where/am/i/data/config/ParsedLink.kt`
- Test: `app/src/test/java/com/anim/where/am/i/data/config/ConfigLinkParserTest.kt`
- Test: `app/src/test/java/com/anim/where/am/i/data/config/ConfigLinkBuilderTest.kt`

**Interfaces:**
- Consumes: `ConfigLink`, `TrackerAction`, `TrackingSettings`, `Accuracy` (Tasks 4/5). Parses with `java.net.URI` (JVM-testable, no `android.net.Uri`).
- Produces:
  - `sealed interface ParsedLink { data class Action(val action: TrackerAction); data class Config(val link: ConfigLink) }`
  - `class ConfigLinkParser { fun parse(raw: String): ParsedLink? }`
  - `class ConfigLinkBuilder { fun build(settings: TrackingSettings): String }` producing `whereami://config?...`

Behavior (ported from Flutter `configuration_service.dart`, scheme changed to `whereami`):
- `whereami://action/start|stop|sos` → `ParsedLink.Action`.
- `http`/`https` URL → `ParsedLink.Config` with `serverUrl = "<scheme>://<host>[:port]<path>"`, plus any recognized query params.
- Any other scheme with a `url` query param → that becomes `serverUrl`; plus recognized query params.
- Query keys: `url`, `id`, `accuracy`, `distance`, `interval`, `angle`, `heartbeat`, `buffer`, `wakelock`, `stop_detection`, `prefer_platform_providers`.
- Booleans accept only literal `true`/`false`; ints via `toIntOrNull()`; bad values ignored.

- [ ] **Step 1: Write the failing tests.**

`ConfigLinkParserTest.kt`:
```kotlin
package com.anim.where.am.i.data.config

import com.anim.where.am.i.domain.model.Accuracy
import com.anim.where.am.i.domain.model.TrackerAction
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ConfigLinkParserTest {
    private val parser = ConfigLinkParser()

    @Test fun parsesStartAction() {
        val r = parser.parse("whereami://action/start")
        assertEquals(ParsedLink.Action(TrackerAction.START), r)
    }

    @Test fun parsesSosAction() {
        assertEquals(ParsedLink.Action(TrackerAction.SOS), parser.parse("whereami://action/sos"))
    }

    @Test fun parsesHttpUrlAsServer() {
        val r = parser.parse("https://server.example.com:8082/path?id=99&accuracy=high") as ParsedLink.Config
        assertEquals("https://server.example.com:8082/path", r.link.serverUrl)
        assertEquals("99", r.link.deviceId)
        assertEquals(Accuracy.HIGH, r.link.accuracy)
    }

    @Test fun parsesConfigSchemeWithParams() {
        val r = parser.parse(
            "whereami://config?url=https://s&id=7&distance=50&interval=90&angle=3" +
                "&heartbeat=60&buffer=false&wakelock=true&stop_detection=false&prefer_platform_providers=true"
        ) as ParsedLink.Config
        val link = r.link
        assertEquals("https://s", link.serverUrl)
        assertEquals("7", link.deviceId)
        assertEquals(50, link.distanceMeters)
        assertEquals(90, link.intervalSeconds)
        assertEquals(3, link.angleDegrees)
        assertEquals(60, link.heartbeatSeconds)
        assertEquals(false, link.buffer)
        assertEquals(true, link.wakeLock)
        assertEquals(false, link.stopDetection)
        assertEquals(true, link.preferPlatformProviders)
    }

    @Test fun ignoresBadBooleanAndInt() {
        val r = parser.parse("whereami://config?buffer=maybe&distance=xyz") as ParsedLink.Config
        assertNull(r.link.buffer)
        assertNull(r.link.distanceMeters)
    }

    @Test fun returnsNullForGarbage() {
        assertNull(parser.parse("not a uri at all ::: %%%"))
    }

    @Test fun unknownActionIsNull() {
        assertTrue(parser.parse("whereami://action/dance") == null)
    }
}
```

`ConfigLinkBuilderTest.kt`:
```kotlin
package com.anim.where.am.i.data.config

import com.anim.where.am.i.domain.model.Accuracy
import com.anim.where.am.i.domain.model.TrackingSettings
import kotlin.test.Test
import kotlin.test.assertEquals

class ConfigLinkBuilderTest {
    @Test fun buildRoundTripsThroughParser() {
        val settings = TrackingSettings(
            serverUrl = "https://s", deviceId = "7", accuracy = Accuracy.LOW,
            distanceMeters = 50, intervalSeconds = 90, angleDegrees = 3,
            heartbeatSeconds = 60, buffer = false, wakeLock = true,
            stopDetection = false, preferPlatformProviders = true,
        )
        val uri = ConfigLinkBuilder().build(settings)
        val parsed = ConfigLinkParser().parse(uri) as ParsedLink.Config
        val applied = parsed.link.applyTo(TrackingSettings(serverUrl = "x", deviceId = "0"))
        assertEquals(settings, applied)
    }
}
```

- [ ] **Step 2: Run tests, expect FAIL.**

Run: `cd whereami_android && ./gradlew :app:testDebugUnitTest --tests "*ConfigLink*"`
Expected: FAIL.

- [ ] **Step 3: Create `ParsedLink.kt`.**

```kotlin
package com.anim.where.am.i.data.config

import com.anim.where.am.i.domain.model.ConfigLink as DomainConfigLink
import com.anim.where.am.i.domain.model.TrackerAction

sealed interface ParsedLink {
    data class Action(val action: TrackerAction) : ParsedLink
    data class Config(val link: DomainConfigLink) : ParsedLink
}
```

- [ ] **Step 4: Create `ConfigLinkParser.kt`.**

```kotlin
package com.anim.where.am.i.data.config

import com.anim.where.am.i.domain.model.Accuracy
import com.anim.where.am.i.domain.model.ConfigLink
import com.anim.where.am.i.domain.model.TrackerAction
import java.net.URI
import java.net.URLDecoder

class ConfigLinkParser {

    fun parse(raw: String): ParsedLink? {
        val uri = try { URI(raw.trim()) } catch (_: Exception) { return null }
        val scheme = uri.scheme?.lowercase() ?: return null

        if (scheme == "whereami" && uri.host == "action") {
            val action = when (uri.path.trim('/').lowercase()) {
                "start" -> TrackerAction.START
                "stop" -> TrackerAction.STOP
                "sos" -> TrackerAction.SOS
                else -> return null
            }
            return ParsedLink.Action(action)
        }

        val params = parseQuery(uri.rawQuery)
        val serverUrl = when (scheme) {
            "http", "https" -> buildString {
                append(scheme).append("://").append(uri.host)
                if (uri.port != -1) append(":").append(uri.port)
                if (!uri.path.isNullOrEmpty()) append(uri.path)
            }
            else -> params["url"]
        }

        return ParsedLink.Config(
            ConfigLink(
                serverUrl = serverUrl,
                deviceId = params["id"],
                accuracy = params["accuracy"]?.let(::accuracyOrNull),
                distanceMeters = params["distance"]?.toIntOrNull(),
                intervalSeconds = params["interval"]?.toIntOrNull(),
                angleDegrees = params["angle"]?.toIntOrNull(),
                heartbeatSeconds = params["heartbeat"]?.toIntOrNull(),
                buffer = params["buffer"]?.let(::boolOrNull),
                wakeLock = params["wakelock"]?.let(::boolOrNull),
                stopDetection = params["stop_detection"]?.let(::boolOrNull),
                preferPlatformProviders = params["prefer_platform_providers"]?.let(::boolOrNull),
            ),
        )
    }

    private fun parseQuery(rawQuery: String?): Map<String, String> {
        if (rawQuery.isNullOrEmpty()) return emptyMap()
        return rawQuery.split("&").mapNotNull { pair ->
            val idx = pair.indexOf('=')
            if (idx <= 0) return@mapNotNull null
            val key = decode(pair.substring(0, idx))
            val value = decode(pair.substring(idx + 1))
            key to value
        }.toMap()
    }

    private fun decode(s: String): String =
        try { URLDecoder.decode(s, "UTF-8") } catch (_: Exception) { s }

    private fun accuracyOrNull(v: String): Accuracy? = when (v.lowercase()) {
        "highest" -> Accuracy.HIGHEST
        "high" -> Accuracy.HIGH
        "medium" -> Accuracy.MEDIUM
        "low" -> Accuracy.LOW
        else -> null
    }

    private fun boolOrNull(v: String): Boolean? = when (v) {
        "true" -> true
        "false" -> false
        else -> null
    }
}
```

- [ ] **Step 5: Create `ConfigLinkBuilder.kt`.**

```kotlin
package com.anim.where.am.i.data.config

import com.anim.where.am.i.domain.model.Accuracy
import com.anim.where.am.i.domain.model.TrackingSettings
import java.net.URLEncoder

class ConfigLinkBuilder {
    fun build(settings: TrackingSettings): String {
        val params = listOf(
            "url" to settings.serverUrl,
            "id" to settings.deviceId,
            "accuracy" to accuracyKey(settings.accuracy),
            "distance" to settings.distanceMeters.toString(),
            "interval" to settings.intervalSeconds.toString(),
            "angle" to settings.angleDegrees.toString(),
            "heartbeat" to settings.heartbeatSeconds.toString(),
            "buffer" to settings.buffer.toString(),
            "wakelock" to settings.wakeLock.toString(),
            "stop_detection" to settings.stopDetection.toString(),
            "prefer_platform_providers" to settings.preferPlatformProviders.toString(),
        )
        val query = params.joinToString("&") { (k, v) -> "$k=${enc(v)}" }
        return "whereami://config?$query"
    }

    private fun accuracyKey(a: Accuracy): String = a.name.lowercase()
    private fun enc(s: String): String = URLEncoder.encode(s, "UTF-8")
}
```

- [ ] **Step 6: Run tests, expect PASS.**

Run: `cd whereami_android && ./gradlew :app:testDebugUnitTest --tests "*ConfigLink*"`
Expected: PASS.

- [ ] **Step 7: Commit.**

```bash
git add app/src/main/java/com/anim/where/am/i/data/config app/src/test/java/com/anim/where/am/i/data/config
git commit -m "feat: add config link parser and builder"
```

---

### Task 12: DI modules binding repositories

**Files:**
- Create: `app/src/main/java/com/anim/where/am/i/di/DataStoreModule.kt`
- Create: `app/src/main/java/com/anim/where/am/i/di/TrackerModule.kt`
- Create: `app/src/main/java/com/anim/where/am/i/di/RepositoryModule.kt`
- Modify: `app/src/main/res/values/strings.xml` (add `notification_text`)

**Interfaces:**
- Consumes: all repository impls (Tasks 8–11), `SettingsRepositoryImpl`, `TrackerRepositoryImpl`, `LogRepositoryImpl`, mappers.
- Produces: Hilt bindings for `SettingsRepository`, `TrackerRepository`, `LogRepository`, `ConfigLinkParser`, `ConfigLinkBuilder`, and a `DataStore<Preferences>` singleton.

**Design note on bootstrap ordering:** `TrackerRepositoryImpl` needs an initial `TrackingSettings` to bootstrap the shared tracker. Provide it by reading the persisted settings once via `runBlocking { settingsRepository.observeSettings().first() }` inside the provider (acceptable: DataStore first read is fast and this is app startup). Also, on first provision, persist that settings object so the seeded random deviceId is stored.

- [ ] **Step 1: Add the notification string** to `res/values/strings.xml`:

```xml
<string name="notification_text">Location tracking</string>
```

and to `res/values-vi/strings.xml` (created in Task 13; if not present yet, add it there when created):

```xml
<string name="notification_text">Đang theo dõi vị trí</string>
```

- [ ] **Step 2: Create `DataStoreModule.kt`.**

```kotlin
package com.anim.where.am.i.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore(name = "settings")

@Module
@InstallIn(SingletonComponent::class)
object DataStoreModule {
    @Provides @Singleton
    fun dataStore(@ApplicationContext context: Context): DataStore<Preferences> = context.dataStore
}
```

- [ ] **Step 3: Create `RepositoryModule.kt`** (binds settings + log + config helpers):

```kotlin
package com.anim.where.am.i.di

import com.anim.where.am.i.data.config.ConfigLinkBuilder
import com.anim.where.am.i.data.config.ConfigLinkParser
import com.anim.where.am.i.data.settings.SettingsRepositoryImpl
import com.anim.where.am.i.domain.repository.SettingsRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds @Singleton
    abstract fun settingsRepository(impl: SettingsRepositoryImpl): SettingsRepository

    companion object {
        @Provides @Singleton fun configLinkParser() = ConfigLinkParser()
        @Provides @Singleton fun configLinkBuilder() = ConfigLinkBuilder()
    }
}
```

- [ ] **Step 4: Create `TrackerModule.kt`** (provides tracker + log repositories, which need context, dispatcher, bootstrap settings):

```kotlin
package com.anim.where.am.i.di

import android.content.Context
import com.anim.where.am.i.R
import com.anim.where.am.i.data.log.LogRepositoryImpl
import com.anim.where.am.i.data.tracker.TrackerRepositoryImpl
import com.anim.where.am.i.domain.repository.LogRepository
import com.anim.where.am.i.domain.repository.SettingsRepository
import com.anim.where.am.i.domain.repository.TrackerRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.traccar.client.Tracker
import org.traccar.client.sharedTracker
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object TrackerModule {

    @Provides @Singleton
    fun trackerRepository(
        @ApplicationContext context: Context,
        @IoDispatcher io: CoroutineDispatcher,
        settingsRepository: SettingsRepository,
    ): TrackerRepository {
        val bootstrap = runBlocking { settingsRepository.observeSettings().first() }
        // Persist once so the seeded random deviceId is stored.
        runBlocking { settingsRepository.save(bootstrap) }
        return TrackerRepositoryImpl(
            context = context,
            io = io,
            notificationText = context.getString(R.string.notification_text),
            bootstrapSettings = bootstrap,
        )
    }

    @Provides @Singleton
    fun logRepository(): LogRepository =
        LogRepositoryImpl(trackerProvider = { sharedTracker() as Tracker })
}
```

Note: `sharedTracker()` (no-arg) returns the already-bootstrapped tracker or null; since `trackerRepository` provision persists settings and the tracker is bootstrapped lazily on first status/start, `LogRepositoryImpl` calling `sharedTracker()` may return null before first start. To avoid a null, change `LogRepositoryImpl` provider to bootstrap if needed:

```kotlin
    @Provides @Singleton
    fun logRepository(
        @ApplicationContext context: Context,
        settingsRepository: SettingsRepository,
    ): LogRepository = LogRepositoryImpl(
        trackerProvider = {
            sharedTracker() ?: run {
                val s = settingsRepository.observeSettings().first()
                sharedTracker(s.toConfig(context.getString(R.string.notification_text)))
            }
        },
    )
```

Add imports: `com.anim.where.am.i.data.tracker.toConfig`, `kotlinx.coroutines.flow.first`.

- [ ] **Step 5: Fix `TrackerRepositoryImpl` constructor injection.** Since it is constructed manually in `TrackerModule`, remove `@Inject`/qualifier annotations from its constructor (they were not added) — confirm the class has a plain constructor (it does, from Task 9). Leave as is.

- [ ] **Step 6: Build to verify the Hilt graph compiles.**

Run: `cd whereami_android && ./gradlew :app:assembleDebug`
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 7: Commit.**

```bash
git add app/src/main/java/com/anim/where/am/i/di app/src/main/res/values/strings.xml
git commit -m "feat: wire Hilt DI for repositories and DataStore"
```

---

## Phase 3 — Presentation

### Task 13: Strings, theme, navigation scaffold

**Files:**
- Modify: `app/src/main/res/values/strings.xml`
- Create: `app/src/main/res/values-vi/strings.xml`
- Create: `app/src/main/java/com/anim/where/am/i/presentation/navigation/WhereAmINav.kt`
- Modify: `app/src/main/java/com/anim/where/am/i/MainActivity.kt`

**Interfaces:**
- Produces: route constants `object Routes { const val MAIN="main"; const val SETTINGS="settings"; const val STATUS="status"; const val QR_SCAN="qr_scan"; const val QR_SHARE="qr_share" }`; `@Composable WhereAmINavHost(navController)`. String resource keys used by all screens.

- [ ] **Step 1: Populate `res/values/strings.xml`** (English) with every key the screens use:

```xml
<resources>
    <string name="app_name">WhereAmI</string>
    <string name="notification_text">Location tracking</string>
    <string name="tracking_title">Tracking</string>
    <string name="tracking_label">Enable tracking</string>
    <string name="id_label">Device identifier</string>
    <string name="disclosure_message">This app collects location data to enable tracking even when the app is closed or not in use.</string>
    <string name="request_position">Request position</string>
    <string name="settings_title">Settings</string>
    <string name="status_title">Status</string>
    <string name="server_url_label">Server URL</string>
    <string name="accuracy_label">Accuracy</string>
    <string name="distance_label">Distance (m)</string>
    <string name="interval_label">Interval (s)</string>
    <string name="angle_label">Angle (°)</string>
    <string name="heartbeat_label">Heartbeat (s)</string>
    <string name="buffer_label">Buffer</string>
    <string name="wakelock_label">Wake lock</string>
    <string name="stop_detection_label">Stop detection</string>
    <string name="prefer_platform_label">Prefer platform providers</string>
    <string name="advanced_label">Advanced</string>
    <string name="highest_accuracy_label">Highest</string>
    <string name="high_accuracy_label">High</string>
    <string name="medium_accuracy_label">Medium</string>
    <string name="low_accuracy_label">Low</string>
    <string name="invalid_url">Invalid URL</string>
    <string name="save_button">Save</string>
    <string name="cancel_button">Cancel</string>
    <string name="ok_button">OK</string>
    <string name="scan_qr">Scan QR</string>
    <string name="share_config">Share config</string>
    <string name="share_logs">Share logs</string>
    <string name="clear_logs">Clear logs</string>
    <string name="refresh">Refresh</string>
    <string name="configuration_message">Apply the scanned configuration?</string>
    <string name="permission_denied">Location permission denied</string>
    <string name="camera_permission_required">Camera permission is required to scan QR codes.</string>
    <string name="start_action">Start</string>
    <string name="stop_action">Stop</string>
    <string name="sos_action">SOS</string>
</resources>
```

- [ ] **Step 2: Create `res/values-vi/strings.xml`** (Vietnamese) mirroring every key:

```xml
<resources>
    <string name="app_name">WhereAmI</string>
    <string name="notification_text">Đang theo dõi vị trí</string>
    <string name="tracking_title">Theo dõi</string>
    <string name="tracking_label">Bật theo dõi</string>
    <string name="id_label">Mã thiết bị</string>
    <string name="disclosure_message">Ứng dụng thu thập dữ liệu vị trí để theo dõi ngay cả khi ứng dụng đã đóng hoặc không sử dụng.</string>
    <string name="request_position">Yêu cầu vị trí</string>
    <string name="settings_title">Cài đặt</string>
    <string name="status_title">Trạng thái</string>
    <string name="server_url_label">Địa chỉ máy chủ</string>
    <string name="accuracy_label">Độ chính xác</string>
    <string name="distance_label">Khoảng cách (m)</string>
    <string name="interval_label">Chu kỳ (giây)</string>
    <string name="angle_label">Góc (°)</string>
    <string name="heartbeat_label">Nhịp (giây)</string>
    <string name="buffer_label">Bộ đệm</string>
    <string name="wakelock_label">Giữ thức</string>
    <string name="stop_detection_label">Phát hiện dừng</string>
    <string name="prefer_platform_label">Ưu tiên nhà cung cấp nền tảng</string>
    <string name="advanced_label">Nâng cao</string>
    <string name="highest_accuracy_label">Cao nhất</string>
    <string name="high_accuracy_label">Cao</string>
    <string name="medium_accuracy_label">Trung bình</string>
    <string name="low_accuracy_label">Thấp</string>
    <string name="invalid_url">URL không hợp lệ</string>
    <string name="save_button">Lưu</string>
    <string name="cancel_button">Hủy</string>
    <string name="ok_button">Đồng ý</string>
    <string name="scan_qr">Quét QR</string>
    <string name="share_config">Chia sẻ cấu hình</string>
    <string name="share_logs">Chia sẻ nhật ký</string>
    <string name="clear_logs">Xóa nhật ký</string>
    <string name="refresh">Làm mới</string>
    <string name="configuration_message">Áp dụng cấu hình vừa quét?</string>
    <string name="permission_denied">Quyền vị trí bị từ chối</string>
    <string name="camera_permission_required">Cần quyền camera để quét mã QR.</string>
    <string name="start_action">Bắt đầu</string>
    <string name="stop_action">Dừng</string>
    <string name="sos_action">SOS</string>
</resources>
```

- [ ] **Step 3: Create `presentation/navigation/WhereAmINav.kt`** (routes + empty host; screens are added in later tasks):

```kotlin
package com.anim.where.am.i.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable

object Routes {
    const val MAIN = "main"
    const val SETTINGS = "settings"
    const val STATUS = "status"
    const val QR_SCAN = "qr_scan"
    const val QR_SHARE = "qr_share"
}

@Composable
fun WhereAmINavHost(navController: NavHostController) {
    NavHost(navController = navController, startDestination = Routes.MAIN) {
        composable(Routes.MAIN) { /* MainScreen added in Task 14 */ }
        composable(Routes.SETTINGS) { /* Task 15 */ }
        composable(Routes.STATUS) { /* Task 16 */ }
        composable(Routes.QR_SCAN) { /* Task 17 */ }
        composable(Routes.QR_SHARE) { /* Task 17 */ }
    }
}
```

- [ ] **Step 4: Rewrite `MainActivity` to host the nav graph** inside the existing theme:

```kotlin
package com.anim.where.am.i

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.navigation.compose.rememberNavController
import com.anim.where.am.i.presentation.navigation.WhereAmINavHost
import com.anim.where.am.i.ui.theme.WhereAmITheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            WhereAmITheme {
                val navController = rememberNavController()
                WhereAmINavHost(navController)
            }
        }
    }
}
```

(Use the actual theme composable name from `ui/theme/Theme.kt`; if it differs from `WhereAmITheme`, use that name.)

- [ ] **Step 5: Build.**

Run: `cd whereami_android && ./gradlew :app:assembleDebug`
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 6: Commit.**

```bash
git add app/src/main/res/values/strings.xml app/src/main/res/values-vi/strings.xml app/src/main/java/com/anim/where/am/i/presentation/navigation app/src/main/java/com/anim/where/am/i/MainActivity.kt
git commit -m "feat: add strings (EN/VI), theme wiring, navigation scaffold"
```

---

### Task 14: Main screen + ViewModel

**Files:**
- Create: `app/src/main/java/com/anim/where/am/i/presentation/main/MainViewModel.kt`
- Create: `app/src/main/java/com/anim/where/am/i/presentation/main/MainScreen.kt`
- Modify: `app/src/main/java/com/anim/where/am/i/presentation/navigation/WhereAmINav.kt`

**Interfaces:**
- Consumes: `ObserveTrackingStatus`, `ObserveSettings`, `StartTracking`, `StopTracking`, `RequestSos` (Task 6).
- Produces: `MainViewModel` with `val uiState: StateFlow<MainUiState>` where `data class MainUiState(val deviceId: String = "", val tracking: Boolean = false, val message: String? = null)`, and functions `onToggleTracking(enable: Boolean)`, `onRequestPosition()`, `consumeMessage()`.

- [ ] **Step 1: Create `MainViewModel.kt`.**

```kotlin
package com.anim.where.am.i.presentation.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anim.where.am.i.domain.usecase.ObserveSettings
import com.anim.where.am.i.domain.usecase.ObserveTrackingStatus
import com.anim.where.am.i.domain.usecase.RequestSos
import com.anim.where.am.i.domain.usecase.StartTracking
import com.anim.where.am.i.domain.usecase.StopTracking
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MainUiState(
    val deviceId: String = "",
    val tracking: Boolean = false,
    val message: String? = null,
)

@HiltViewModel
class MainViewModel @Inject constructor(
    observeStatus: ObserveTrackingStatus,
    observeSettings: ObserveSettings,
    private val startTracking: StartTracking,
    private val stopTracking: StopTracking,
    private val requestSos: RequestSos,
) : ViewModel() {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            observeStatus().collect { status -> _uiState.update { it.copy(tracking = status.enabled) } }
        }
        viewModelScope.launch {
            observeSettings().collect { s -> _uiState.update { it.copy(deviceId = s.deviceId) } }
        }
    }

    fun onToggleTracking(enable: Boolean) {
        viewModelScope.launch {
            try {
                if (enable) startTracking() else stopTracking()
            } catch (e: IllegalStateException) {
                _uiState.update { it.copy(tracking = false, message = e.message) }
            }
        }
    }

    fun onRequestPosition() {
        viewModelScope.launch {
            try { requestSos() } catch (e: IllegalStateException) {
                _uiState.update { it.copy(message = e.message) }
            }
        }
    }

    fun consumeMessage() = _uiState.update { it.copy(message = null) }
}
```

- [ ] **Step 2: Create `MainScreen.kt`.**

```kotlin
package com.anim.where.am.i.presentation.main

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.anim.where.am.i.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    onOpenSettings: () -> Unit,
    onOpenStatus: () -> Unit,
    viewModel: MainViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.message) {
        state.message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.consumeMessage()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                actions = {
                    IconButton(onClick = onOpenStatus) { Icon(Icons.Default.List, stringResource(R.string.status_title)) }
                    IconButton(onClick = onOpenSettings) { Icon(Icons.Default.Settings, stringResource(R.string.settings_title)) }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier.padding(padding).padding(16.dp).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Card {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.tracking_title), style = MaterialTheme.typography.headlineSmall)
                    Text(stringResource(R.string.id_label) + ": " + state.deviceId)
                    Text(stringResource(R.string.disclosure_message), style = MaterialTheme.typography.bodySmall)
                    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(stringResource(R.string.tracking_label))
                        Switch(checked = state.tracking, onCheckedChange = viewModel::onToggleTracking)
                    }
                }
            }
            Button(onClick = viewModel::onRequestPosition, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.request_position))
            }
        }
    }
}
```

- [ ] **Step 3: Wire the route** in `WhereAmINav.kt` — replace the `MAIN` composable body:

```kotlin
composable(Routes.MAIN) {
    com.anim.where.am.i.presentation.main.MainScreen(
        onOpenSettings = { navController.navigate(Routes.SETTINGS) },
        onOpenStatus = { navController.navigate(Routes.STATUS) },
    )
}
```

- [ ] **Step 4: Add the material icons dependency** if unresolved. In `app/build.gradle.kts` dependencies add `implementation("androidx.compose.material:material-icons-extended")` (version from the Compose BOM). Add a catalog alias `androidx-compose-material-icons-extended = { group = "androidx.compose.material", name = "material-icons-extended" }` and use `implementation(libs.androidx.compose.material.icons.extended)`.

- [ ] **Step 5: Build.**

Run: `cd whereami_android && ./gradlew :app:assembleDebug`
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 6: Commit.**

```bash
git add app/src/main/java/com/anim/where/am/i/presentation/main app/src/main/java/com/anim/where/am/i/presentation/navigation/WhereAmINav.kt app/build.gradle.kts gradle/libs.versions.toml
git commit -m "feat: add main screen and view model"
```

---

### Task 15: Settings screen + ViewModel

**Files:**
- Create: `app/src/main/java/com/anim/where/am/i/presentation/settings/SettingsViewModel.kt`
- Create: `app/src/main/java/com/anim/where/am/i/presentation/settings/SettingsScreen.kt`
- Modify: `app/src/main/java/com/anim/where/am/i/presentation/navigation/WhereAmINav.kt`

**Interfaces:**
- Consumes: `ObserveSettings`, `SaveSettings` (Task 6); `TrackingSettings`, `Accuracy` (Task 4).
- Produces: `SettingsViewModel` with `val settings: StateFlow<TrackingSettings?>`, `fun update(transform: (TrackingSettings) -> TrackingSettings)`, `fun save(): Boolean` (returns false + sets error if URL invalid). URL valid = non-blank and starts with `http://` or `https://`.

- [ ] **Step 1: Create `SettingsViewModel.kt`.**

```kotlin
package com.anim.where.am.i.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anim.where.am.i.domain.model.TrackingSettings
import com.anim.where.am.i.domain.usecase.ObserveSettings
import com.anim.where.am.i.domain.usecase.SaveSettings
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    observeSettings: ObserveSettings,
    private val saveSettings: SaveSettings,
) : ViewModel() {

    private val _settings = MutableStateFlow<TrackingSettings?>(null)
    val settings: StateFlow<TrackingSettings?> = _settings.asStateFlow()

    private val _urlError = MutableStateFlow(false)
    val urlError: StateFlow<Boolean> = _urlError.asStateFlow()

    init {
        viewModelScope.launch { _settings.value = observeSettings().first() }
    }

    fun update(transform: (TrackingSettings) -> TrackingSettings) {
        _settings.value = _settings.value?.let(transform)
        _urlError.value = false
    }

    fun save(onDone: () -> Unit) {
        val current = _settings.value ?: return
        if (!isValidUrl(current.serverUrl)) { _urlError.value = true; return }
        viewModelScope.launch { saveSettings(current); onDone() }
    }

    private fun isValidUrl(url: String): Boolean =
        url.isNotBlank() && (url.startsWith("http://") || url.startsWith("https://"))
}
```

- [ ] **Step 2: Create `SettingsScreen.kt`** with basic fields always visible and advanced ones behind a toggle:

```kotlin
package com.anim.where.am.i.presentation.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.anim.where.am.i.R
import com.anim.where.am.i.domain.model.Accuracy

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onScanQr: () -> Unit,
    onShareConfig: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val urlError by viewModel.urlError.collectAsStateWithLifecycle()
    var advanced by remember { mutableStateOf(false) }
    val s = settings ?: return

    Scaffold(topBar = { TopAppBar(title = { Text(stringResource(R.string.settings_title)) }) }) { padding ->
        Column(
            Modifier.padding(padding).padding(16.dp).fillMaxSize().verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedTextField(
                value = s.serverUrl, onValueChange = { v -> viewModel.update { it.copy(serverUrl = v) } },
                label = { Text(stringResource(R.string.server_url_label)) },
                isError = urlError, singleLine = true, modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = s.deviceId, onValueChange = { v -> viewModel.update { it.copy(deviceId = v) } },
                label = { Text(stringResource(R.string.id_label)) }, singleLine = true, modifier = Modifier.fillMaxWidth(),
            )
            AccuracyDropdown(s.accuracy) { a -> viewModel.update { it.copy(accuracy = a) } }
            IntField(R.string.distance_label, s.distanceMeters) { v -> viewModel.update { it.copy(distanceMeters = v) } }
            IntField(R.string.interval_label, s.intervalSeconds) { v -> viewModel.update { it.copy(intervalSeconds = v) } }

            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.advanced_label), Modifier.weight(1f))
                Switch(checked = advanced, onCheckedChange = { advanced = it })
            }

            if (advanced) {
                IntField(R.string.angle_label, s.angleDegrees) { v -> viewModel.update { it.copy(angleDegrees = v) } }
                IntField(R.string.heartbeat_label, s.heartbeatSeconds) { v -> viewModel.update { it.copy(heartbeatSeconds = v) } }
                BoolRow(R.string.buffer_label, s.buffer) { v -> viewModel.update { it.copy(buffer = v) } }
                BoolRow(R.string.wakelock_label, s.wakeLock) { v -> viewModel.update { it.copy(wakeLock = v) } }
                BoolRow(R.string.stop_detection_label, s.stopDetection) { v -> viewModel.update { it.copy(stopDetection = v) } }
                BoolRow(R.string.prefer_platform_label, s.preferPlatformProviders) { v -> viewModel.update { it.copy(preferPlatformProviders = v) } }
            }

            Button(onClick = onScanQr, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.scan_qr)) }
            Button(onClick = onShareConfig, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.share_config)) }
            Button(onClick = { viewModel.save(onBack) }, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.save_button)) }
        }
    }
}
```

Add the small reusable composables at the bottom of the same file:

```kotlin
@Composable
private fun IntField(labelRes: Int, value: Int, onChange: (Int) -> Unit) {
    OutlinedTextField(
        value = value.toString(),
        onValueChange = { it.toIntOrNull()?.let(onChange) ?: if (it.isEmpty()) onChange(0) else Unit },
        label = { Text(stringResource(labelRes)) },
        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true, modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun BoolRow(labelRes: Int, value: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(stringResource(labelRes), Modifier.weight(1f))
        Switch(checked = value, onCheckedChange = onChange)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AccuracyDropdown(value: Accuracy, onChange: (Accuracy) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    androidx.compose.material3.ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = accuracyLabel(value), onValueChange = {}, readOnly = true,
            label = { Text(stringResource(R.string.accuracy_label)) },
            modifier = Modifier.fillMaxWidth().menuAnchor(),
        )
        androidx.compose.material3.ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            Accuracy.entries.forEach { a ->
                androidx.compose.material3.DropdownMenuItem(
                    text = { Text(accuracyLabel(a)) },
                    onClick = { onChange(a); expanded = false },
                )
            }
        }
    }
}

@Composable
private fun accuracyLabel(a: Accuracy): String = stringResource(
    when (a) {
        Accuracy.HIGHEST -> R.string.highest_accuracy_label
        Accuracy.HIGH -> R.string.high_accuracy_label
        Accuracy.MEDIUM -> R.string.medium_accuracy_label
        Accuracy.LOW -> R.string.low_accuracy_label
    }
)
```

- [ ] **Step 3: Wire the route** — replace `SETTINGS` composable body in `WhereAmINav.kt`:

```kotlin
composable(Routes.SETTINGS) {
    com.anim.where.am.i.presentation.settings.SettingsScreen(
        onBack = { navController.popBackStack() },
        onScanQr = { navController.navigate(Routes.QR_SCAN) },
        onShareConfig = { navController.navigate(Routes.QR_SHARE) },
    )
}
```

- [ ] **Step 4: Build.**

Run: `cd whereami_android && ./gradlew :app:assembleDebug`
Expected: `BUILD SUCCESSFUL`. (If `menuAnchor()` is deprecated in this Compose version, use the parameterized overload `menuAnchor(MenuAnchorType.PrimaryNotEditable)`.)

- [ ] **Step 5: Commit.**

```bash
git add app/src/main/java/com/anim/where/am/i/presentation/settings app/src/main/java/com/anim/where/am/i/presentation/navigation/WhereAmINav.kt
git commit -m "feat: add settings screen with basic/advanced fields"
```

---

### Task 16: Status/logs screen + ViewModel

**Files:**
- Create: `app/src/main/java/com/anim/where/am/i/presentation/status/StatusViewModel.kt`
- Create: `app/src/main/java/com/anim/where/am/i/presentation/status/StatusScreen.kt`
- Modify: `app/src/main/java/com/anim/where/am/i/presentation/navigation/WhereAmINav.kt`

**Interfaces:**
- Consumes: `GetLogs`, `ClearLogs` (Task 6); `LogItem` (Task 4).
- Produces: `StatusViewModel` with `val logs: StateFlow<List<LogItem>>`, `fun refresh()`, `fun clear()`, `fun formatShare(): String`. Shows newest first.

- [ ] **Step 1: Create `StatusViewModel.kt`.**

```kotlin
package com.anim.where.am.i.presentation.status

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anim.where.am.i.domain.model.LogItem
import com.anim.where.am.i.domain.usecase.ClearLogs
import com.anim.where.am.i.domain.usecase.GetLogs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class StatusViewModel @Inject constructor(
    private val getLogs: GetLogs,
    private val clearLogs: ClearLogs,
) : ViewModel() {

    private val _logs = MutableStateFlow<List<LogItem>>(emptyList())
    val logs: StateFlow<List<LogItem>> = _logs.asStateFlow()
    private val fullFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)

    init { refresh() }

    fun refresh() {
        viewModelScope.launch { _logs.value = getLogs().reversed() }
    }

    fun clear() {
        viewModelScope.launch { clearLogs(); _logs.value = emptyList() }
    }

    fun formatShare(): String = _logs.value.joinToString("\n") {
        "${fullFormat.format(Date(it.timeMillis))} ${it.message}"
    }
}
```

- [ ] **Step 2: Create `StatusScreen.kt`** with 5-second auto-refresh while resumed and share via `Intent.ACTION_SEND`:

```kotlin
package com.anim.where.am.i.presentation.status

import android.content.Intent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.hilt.navigation.compose.hiltViewModel
import com.anim.where.am.i.R
import kotlinx.coroutines.delay
import androidx.compose.runtime.LaunchedEffect
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatusScreen(viewModel: StatusViewModel = hiltViewModel()) {
    val logs by viewModel.logs.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val displayFormat = remember0()

    LaunchedEffect(Unit) {
        while (true) { delay(5000); viewModel.refresh() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.status_title)) },
                actions = {
                    IconButton(onClick = viewModel::refresh) { Icon(Icons.Default.Refresh, stringResource(R.string.refresh)) }
                    IconButton(onClick = {
                        val text = viewModel.formatShare()
                        if (text.isNotEmpty()) {
                            val send = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"; putExtra(Intent.EXTRA_TEXT, text)
                            }
                            context.startActivity(Intent.createChooser(send, null))
                        }
                    }) { Icon(Icons.Default.Share, stringResource(R.string.share_logs)) }
                    IconButton(onClick = viewModel::clear) { Icon(Icons.Default.Delete, stringResource(R.string.clear_logs)) }
                },
            )
        },
    ) { padding ->
        LazyColumn(Modifier.padding(padding).fillMaxSize()) {
            items(logs) { entry ->
                ListItem(
                    headlineContent = { Text(entry.message) },
                    supportingContent = { Text(displayFormat.format(Date(entry.timeMillis))) },
                )
            }
        }
    }
}

private fun remember0() = SimpleDateFormat("HH:mm:ss", Locale.US)
```

Replace the `remember0()` helper with an inline `remember { SimpleDateFormat("HH:mm:ss", Locale.US) }` — add the `androidx.compose.runtime.remember` import and use it directly in the composable:

```kotlin
    val displayFormat = androidx.compose.runtime.remember { SimpleDateFormat("HH:mm:ss", Locale.US) }
```

and delete the `remember0()` function.

- [ ] **Step 3: Wire the route** — replace `STATUS` body in `WhereAmINav.kt`:

```kotlin
composable(Routes.STATUS) { com.anim.where.am.i.presentation.status.StatusScreen() }
```

- [ ] **Step 4: Build.**

Run: `cd whereami_android && ./gradlew :app:assembleDebug`
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 5: Commit.**

```bash
git add app/src/main/java/com/anim/where/am/i/presentation/status app/src/main/java/com/anim/where/am/i/presentation/navigation/WhereAmINav.kt
git commit -m "feat: add status/logs screen with auto-refresh, share, clear"
```

---

### Task 17: QR scan (CameraX + ML Kit) and QR share (ZXing)

**Files:**
- Create: `app/src/main/java/com/anim/where/am/i/presentation/qr/QrScanScreen.kt`
- Create: `app/src/main/java/com/anim/where/am/i/presentation/qr/QrShareScreen.kt`
- Create: `app/src/main/java/com/anim/where/am/i/presentation/qr/QrViewModel.kt`
- Create: `app/src/main/java/com/anim/where/am/i/presentation/qr/QrCodeGenerator.kt`
- Test: `app/src/test/java/com/anim/where/am/i/presentation/qr/QrCodeGeneratorTest.kt`
- Modify: `app/src/main/java/com/anim/where/am/i/presentation/navigation/WhereAmINav.kt`

**Interfaces:**
- Consumes: `ConfigLinkParser`, `ConfigLinkBuilder` (Task 11); `ApplyConfigLink` (Task 6); `ObserveSettings` (Task 6).
- Produces:
  - `QrCodeGenerator.encode(text: String, sizePx: Int): android.graphics.Bitmap` (ZXing).
  - `QrViewModel` with `fun onScanned(raw: String, onConfig: (ConfigLink) -> Unit)`, `val shareUri: StateFlow<String?>`, `fun applyConfig(link: ConfigLink)`.

- [ ] **Step 1: Write the failing test `QrCodeGeneratorTest.kt`** (pure ZXing bit-matrix, no Android bitmap — test the matrix path):

```kotlin
package com.anim.where.am.i.presentation.qr

import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import kotlin.test.Test
import kotlin.test.assertTrue

class QrCodeGeneratorTest {
    @Test fun encodesNonEmptyMatrix() {
        val matrix = MultiFormatWriter().encode("whereami://config?id=1", BarcodeFormat.QR_CODE, 64, 64)
        assertTrue(matrix.width == 64 && matrix.height == 64)
    }
}
```

- [ ] **Step 2: Run test, expect PASS once ZXing is on the test classpath** (this validates the dependency, not our code yet).

Run: `cd whereami_android && ./gradlew :app:testDebugUnitTest --tests "*QrCodeGeneratorTest"`
Expected: PASS.

- [ ] **Step 3: Create `QrCodeGenerator.kt`.**

```kotlin
package com.anim.where.am.i.presentation.qr

import android.graphics.Bitmap
import android.graphics.Color
import androidx.core.graphics.createBitmap
import androidx.core.graphics.set
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter

object QrCodeGenerator {
    fun encode(text: String, sizePx: Int): Bitmap {
        val matrix = MultiFormatWriter().encode(text, BarcodeFormat.QR_CODE, sizePx, sizePx)
        val bitmap = createBitmap(sizePx, sizePx)
        for (x in 0 until sizePx) {
            for (y in 0 until sizePx) {
                bitmap[x, y] = if (matrix[x, y]) Color.BLACK else Color.WHITE
            }
        }
        return bitmap
    }
}
```

- [ ] **Step 4: Create `QrViewModel.kt`.**

```kotlin
package com.anim.where.am.i.presentation.qr

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anim.where.am.i.data.config.ConfigLinkBuilder
import com.anim.where.am.i.data.config.ConfigLinkParser
import com.anim.where.am.i.data.config.ParsedLink
import com.anim.where.am.i.domain.model.ConfigLink
import com.anim.where.am.i.domain.usecase.ApplyConfigLink
import com.anim.where.am.i.domain.usecase.ObserveSettings
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class QrViewModel @Inject constructor(
    private val parser: ConfigLinkParser,
    private val builder: ConfigLinkBuilder,
    private val applyConfigLink: ApplyConfigLink,
    private val observeSettings: ObserveSettings,
) : ViewModel() {

    private val _shareUri = MutableStateFlow<String?>(null)
    val shareUri: StateFlow<String?> = _shareUri.asStateFlow()

    init {
        viewModelScope.launch { _shareUri.value = builder.build(observeSettings().first()) }
    }

    /** Returns the parsed ConfigLink for a confirmation dialog, or null if not a config link. */
    fun parseConfig(raw: String): ConfigLink? =
        (parser.parse(raw) as? ParsedLink.Config)?.link

    fun applyConfig(link: ConfigLink) {
        viewModelScope.launch { applyConfigLink(link) }
    }
}
```

- [ ] **Step 5: Create `QrScanScreen.kt`** — CameraX preview + ML Kit analyzer, camera permission via Activity Result, confirmation dialog before applying:

```kotlin
package com.anim.where.am.i.presentation.qr

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.anim.where.am.i.R
import com.anim.where.am.i.domain.model.ConfigLink
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QrScanScreen(onDone: () -> Unit, viewModel: QrViewModel = hiltViewModel()) {
    val context = LocalContext.current
    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED,
        )
    }
    var pending by remember { mutableStateOf<ConfigLink?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        hasPermission = it
        if (!it) onDone()
    }
    LaunchedEffect(Unit) { if (!hasPermission) permissionLauncher.launch(Manifest.permission.CAMERA) }

    Scaffold(topBar = { TopAppBar(title = { Text(stringResource(R.string.scan_qr)) }) }) { padding ->
        Box(Modifier.fillMaxSize()) {
            if (hasPermission) {
                CameraPreview(onQr = { raw ->
                    if (pending == null) {
                        val link = viewModel.parseConfig(raw)
                        if (link != null) pending = link
                    }
                })
            } else {
                Text(stringResource(R.string.camera_permission_required), Modifier.fillMaxSize())
            }
        }
    }

    pending?.let { link ->
        AlertDialog(
            onDismissRequest = { pending = null; onDone() },
            confirmButton = {
                TextButton(onClick = { viewModel.applyConfig(link); pending = null; onDone() }) {
                    Text(stringResource(R.string.ok_button))
                }
            },
            dismissButton = {
                TextButton(onClick = { pending = null; onDone() }) { Text(stringResource(R.string.cancel_button)) }
            },
            text = { Text(stringResource(R.string.configuration_message)) },
        )
    }
}

@Composable
private fun CameraPreview(onQr: (String) -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scanner = remember { BarcodeScanning.getClient() }
    val executor = remember { Executors.newSingleThreadExecutor() }

    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { ctx ->
            val previewView = PreviewView(ctx)
            val providerFuture = ProcessCameraProvider.getInstance(ctx)
            providerFuture.addListener({
                val provider = providerFuture.get()
                val preview = Preview.Builder().build().also { it.surfaceProvider = previewView.surfaceProvider }
                val analysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST).build()
                analysis.setAnalyzer(executor) { proxy: ImageProxy ->
                    val media = proxy.image
                    if (media != null) {
                        val image = InputImage.fromMediaImage(media, proxy.imageInfo.rotationDegrees)
                        scanner.process(image)
                            .addOnSuccessListener { codes -> codes.firstOrNull()?.rawValue?.let(onQr) }
                            .addOnCompleteListener { proxy.close() }
                    } else proxy.close()
                }
                provider.unbindAll()
                provider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, analysis)
            }, ContextCompat.getMainExecutor(ctx))
            previewView
        },
    )
}
```

- [ ] **Step 6: Create `QrShareScreen.kt`** — render the config QR:

```kotlin
package com.anim.where.am.i.presentation.qr

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.anim.where.am.i.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QrShareScreen(viewModel: QrViewModel = hiltViewModel()) {
    val uri by viewModel.shareUri.collectAsStateWithLifecycle()
    Scaffold(topBar = { TopAppBar(title = { Text(stringResource(R.string.share_config)) }) }) { padding ->
        Column(
            Modifier.padding(padding).fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            uri?.let { text ->
                val bitmap = remember(text) { QrCodeGenerator.encode(text, 640) }
                Image(bitmap = bitmap.asImageBitmap(), contentDescription = null, modifier = Modifier.size(280.dp))
                Text(text, Modifier.padding(16.dp))
            }
        }
    }
}
```

- [ ] **Step 7: Wire the routes** — replace `QR_SCAN` and `QR_SHARE` bodies in `WhereAmINav.kt`:

```kotlin
composable(Routes.QR_SCAN) {
    com.anim.where.am.i.presentation.qr.QrScanScreen(onDone = { navController.popBackStack() })
}
composable(Routes.QR_SHARE) { com.anim.where.am.i.presentation.qr.QrShareScreen() }
```

- [ ] **Step 8: Build.**

Run: `cd whereami_android && ./gradlew :app:assembleDebug`
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 9: Commit.**

```bash
git add app/src/main/java/com/anim/where/am/i/presentation/qr app/src/main/java/com/anim/where/am/i/presentation/navigation/WhereAmINav.kt app/src/test/java/com/anim/where/am/i/presentation/qr
git commit -m "feat: add QR scan (CameraX+MLKit) and QR share (ZXing)"
```

---

## Phase 4 — Platform integration

### Task 18: Deep links

**Files:**
- Modify: `app/src/main/AndroidManifest.xml`
- Create: `app/src/main/java/com/anim/where/am/i/deeplink/DeepLinkHandler.kt`
- Modify: `app/src/main/java/com/anim/where/am/i/MainActivity.kt`

**Interfaces:**
- Consumes: `ConfigLinkParser`, `ParsedLink` (Task 11); `RunTrackerAction` (Task 6); `ApplyConfigLink` (Task 6).
- Produces: `@Singleton DeepLinkHandler` with `suspend fun handle(raw: String): DeepLinkResult` where `sealed interface DeepLinkResult { object None; object ActionHandled; data class ConfirmConfig(val link: ConfigLink) }`.

- [ ] **Step 1: Create `DeepLinkHandler.kt`.**

```kotlin
package com.anim.where.am.i.deeplink

import com.anim.where.am.i.data.config.ConfigLinkParser
import com.anim.where.am.i.data.config.ParsedLink
import com.anim.where.am.i.domain.model.ConfigLink
import com.anim.where.am.i.domain.usecase.RunTrackerAction
import javax.inject.Inject
import javax.inject.Singleton

sealed interface DeepLinkResult {
    data object None : DeepLinkResult
    data object ActionHandled : DeepLinkResult
    data class ConfirmConfig(val link: ConfigLink) : DeepLinkResult
}

@Singleton
class DeepLinkHandler @Inject constructor(
    private val parser: ConfigLinkParser,
    private val runTrackerAction: RunTrackerAction,
) {
    suspend fun handle(raw: String): DeepLinkResult = when (val parsed = parser.parse(raw)) {
        is ParsedLink.Action -> {
            try { runTrackerAction(parsed.action) } catch (_: IllegalStateException) {}
            DeepLinkResult.ActionHandled
        }
        is ParsedLink.Config -> DeepLinkResult.ConfirmConfig(parsed.link)
        null -> DeepLinkResult.None
    }
}
```

- [ ] **Step 2: Add the deep-link intent filter** to the `<activity android:name=".MainActivity">` in `AndroidManifest.xml` (keep the existing MAIN/LAUNCHER filter):

```xml
<intent-filter>
    <action android:name="android.intent.action.VIEW" />
    <category android:name="android.intent.category.DEFAULT" />
    <category android:name="android.intent.category.BROWSABLE" />
    <data android:scheme="whereami" />
</intent-filter>
```

Also add `android:launchMode="singleTop"` to the activity so re-delivered links call `onNewIntent`.

- [ ] **Step 3: Handle the intent in `MainActivity`.** Inject the handler via an entry point (Activity is `@AndroidEntryPoint`) and process `intent.data` on create and on new intent. Extend `MainActivity`:

```kotlin
    @Inject lateinit var deepLinkHandler: com.anim.where.am.i.deeplink.DeepLinkHandler
    private val pendingConfig = MutableStateFlow<com.anim.where.am.i.domain.model.ConfigLink?>(null)
```

In `onCreate`, after `enableEdgeToEdge()`, call `handleIntent(intent)`; override `onNewIntent`:

```kotlin
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        val data = intent?.data?.toString() ?: return
        lifecycleScope.launch {
            when (val result = deepLinkHandler.handle(data)) {
                is DeepLinkResult.ConfirmConfig -> pendingConfig.value = result.link
                else -> Unit
            }
        }
    }
```

Collect `pendingConfig` in the composition and show an `AlertDialog` (message `R.string.configuration_message`) that, on confirm, calls an injected `ApplyConfigLink`. Provide `ApplyConfigLink` via a second `@Inject lateinit var applyConfigLink: ApplyConfigLink` and invoke it in a coroutine. Add the required imports (`Intent`, `lifecycleScope`, `MutableStateFlow`, `launch`, `DeepLinkResult`).

- [ ] **Step 4: Build.**

Run: `cd whereami_android && ./gradlew :app:assembleDebug`
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 5: Commit.**

```bash
git add app/src/main/AndroidManifest.xml app/src/main/java/com/anim/where/am/i/deeplink app/src/main/java/com/anim/where/am/i/MainActivity.kt
git commit -m "feat: handle whereami:// deep links for actions and config"
```

---

### Task 19: Quick actions (app shortcuts)

**Files:**
- Create: `app/src/main/java/com/anim/where/am/i/quickactions/ShortcutManagerHelper.kt`
- Create: `app/src/main/java/com/anim/where/am/i/quickactions/ShortcutActivity.kt`
- Modify: `app/src/main/AndroidManifest.xml`
- Modify: `app/src/main/java/com/anim/where/am/i/WhereAmIApp.kt`

**Interfaces:**
- Consumes: `RunTrackerAction` (Task 6); `TrackerAction` (Task 5).
- Produces: `ShortcutManagerHelper.register(context)` creating dynamic shortcuts `start`/`stop`/`sos`; `ShortcutActivity` (no UI) that reads its intent action id, runs the use case, finishes.

- [ ] **Step 1: Create `ShortcutManagerHelper.kt`.**

```kotlin
package com.anim.where.am.i.quickactions

import android.content.Context
import android.content.Intent
import androidx.core.content.getSystemService
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import com.anim.where.am.i.R

object ShortcutManagerHelper {
    const val EXTRA_ACTION = "shortcut_action"

    fun register(context: Context) {
        val manager = context.getSystemService<ShortcutManager>() ?: return
        manager.dynamicShortcuts = listOf(
            shortcut(context, "start", R.string.start_action),
            shortcut(context, "stop", R.string.stop_action),
            shortcut(context, "sos", R.string.sos_action),
        )
    }

    private fun shortcut(context: Context, id: String, labelRes: Int): ShortcutInfo {
        val intent = Intent(context, ShortcutActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            putExtra(EXTRA_ACTION, id)
        }
        return ShortcutInfo.Builder(context, id)
            .setShortLabel(context.getString(labelRes))
            .setIntent(intent)
            .build()
    }
}
```

- [ ] **Step 2: Create `ShortcutActivity.kt`.**

```kotlin
package com.anim.where.am.i.quickactions

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import com.anim.where.am.i.domain.model.TrackerAction
import com.anim.where.am.i.domain.usecase.RunTrackerAction
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class ShortcutActivity : ComponentActivity() {

    @Inject lateinit var runTrackerAction: RunTrackerAction

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val id = intent.getStringExtra(ShortcutManagerHelper.EXTRA_ACTION)
        val action = when (id) {
            "start" -> TrackerAction.START
            "stop" -> TrackerAction.STOP
            "sos" -> TrackerAction.SOS
            else -> null
        }
        if (action != null) {
            lifecycleScope.launch {
                try { runTrackerAction(action) } catch (_: IllegalStateException) {}
                finish()
            }
        } else finish()
    }
}
```

- [ ] **Step 3: Register `ShortcutActivity`** in `AndroidManifest.xml` (inside `<application>`):

```xml
<activity
    android:name=".quickactions.ShortcutActivity"
    android:exported="true"
    android:theme="@android:style/Theme.Translucent.NoTitleBar" />
```

- [ ] **Step 4: Register shortcuts on app startup.** In `WhereAmIApp.onCreate`:

```kotlin
import com.anim.where.am.i.quickactions.ShortcutManagerHelper

@HiltAndroidApp
class WhereAmIApp : Application() {
    override fun onCreate() {
        super.onCreate()
        ShortcutManagerHelper.register(this)
    }
}
```

- [ ] **Step 5: Build.**

Run: `cd whereami_android && ./gradlew :app:assembleDebug`
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 6: Commit.**

```bash
git add app/src/main/java/com/anim/where/am/i/quickactions app/src/main/AndroidManifest.xml app/src/main/java/com/anim/where/am/i/WhereAmIApp.kt
git commit -m "feat: add start/stop/sos app shortcuts"
```

---

### Task 20: Manifest permissions, final wiring, end-to-end verification

**Files:**
- Modify: `app/src/main/AndroidManifest.xml`

**Interfaces:**
- Consumes: everything. No new code — this task adds the CAMERA permission, verifies the merged manifest, runs the full test + build, and does a manual smoke test.

- [ ] **Step 1: Add the CAMERA permission** to `AndroidManifest.xml` (location/foreground/boot permissions come from the SDK's merged manifest, so do not redeclare them):

```xml
<uses-permission android:name="android.permission.CAMERA" />
```

- [ ] **Step 2: Run the full unit-test suite.**

Run: `cd whereami_android && ./gradlew :app:testDebugUnitTest`
Expected: `BUILD SUCCESSFUL`, all tests pass.

- [ ] **Step 3: Verify the merged manifest contains the SDK service + permissions.**

Run: `cd whereami_android && ./gradlew :app:processDebugMainManifest && grep -c "FOREGROUND_SERVICE_LOCATION" app/build/intermediates/merged_manifests/debug/processDebugMainManifest/AndroidManifest.xml`
Expected: count ≥ 1 (SDK permission merged in). If the intermediates path differs in this AGP version, locate the merged manifest under `app/build/intermediates/**/AndroidManifest.xml` and grep there.

- [ ] **Step 4: Assemble the release variant** to confirm nothing is debug-only.

Run: `cd whereami_android && ./gradlew :app:assembleRelease`
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 5: Manual smoke test** (device/emulator with Google Play services). Install, launch, and verify:
  - Main screen shows a generated Device ID.
  - Toggling the tracking switch triggers the SDK permission prompts; granting starts the foreground service (notification appears).
  - Settings: edit server URL / interval, toggle Advanced, Save; reopen to confirm persistence.
  - Status: entries appear and auto-refresh; Share opens the chooser; Clear empties the list.
  - Share config → a QR renders; Scan QR on a second device applies it after confirmation.
  - `adb shell am start -a android.intent.action.VIEW -d "whereami://action/start"` starts tracking; `whereami://action/stop` stops it.
  - Long-press the launcher icon shows Start / Stop / SOS shortcuts that work.

Run: `cd whereami_android && ./gradlew :app:installDebug`
Expected: `BUILD SUCCESSFUL`; then perform the checks above.

- [ ] **Step 6: Commit.**

```bash
git add app/src/main/AndroidManifest.xml
git commit -m "feat: add camera permission and finalize manifest"
```

---

## Self-Review Notes

- **Spec coverage:** core tracking (Tasks 9, 14), full settings incl. advanced (Tasks 8, 15), status/logs (Tasks 10, 16), single-shot SOS (Tasks 6, 14, 19), QR scan+share (Task 17), quick actions (Task 19), deep links (Task 18), EN+VI localization (Task 13), Clean Architecture layering + Hilt DI (Tasks 3–12), composite SDK build + Kotlin bump (Tasks 1–2). All spec sections map to at least one task.
- **SDK isolation:** domain references no `org.traccar.client.*` type; all mapping is in `data/tracker`, `data/log`, `data/config`. Verified against each task's imports.
- **Type consistency:** `TrackingSettings`, `TrackingStatus`, `LogItem`, `ConfigLink`, `TrackerAction`, `ParsedLink`, repository method signatures, and use-case names are used identically across producing and consuming tasks.
- **Out of scope confirmed absent:** no Firebase, rate-my-app, password lock, or extra locales.
