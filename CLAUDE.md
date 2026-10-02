# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this project is

A minimal Android weather app (Kotlin, Jetpack Compose, OpenWeatherMap API) whose real purpose is **not the app itself**. It's the evidence project for a security-training exercise on "Risks of API keys & secrets in code": the git history is deliberately staged to progress from antipatterns (hardcoded key, exposed config file, late `.gitignore`) to fixes (`BuildConfig` + `local.properties`, certificate pinning, `EncryptedSharedPreferences` for a runtime token), mirrored in the GitHub Actions pipeline. See `docs/evidencia-practica-es.md` for the stage-by-stage narrative with evidence (logs, decompilation checks, screenshots).

**Do not "clean up" the antipatterns you find in earlier files/commits unless you're explicitly asked to implement the next stage** — a hardcoded key or an un-gitignored config file may be intentional, staged content, not a bug.

## Two files you cannot create/read here, by design

This environment's permission settings block Claude from writing or reading any `*.properties` file, in any directory. This is intentional — it's exactly the kind of file that tends to hold secrets (`local.properties`, `gradle.properties`, `secrets.properties`). Two of them are required for this project and must be created manually (by a human, in Android Studio or a plain editor):

- **`gradle.properties`** (repo root, safe to commit — contains no secrets):
  ```properties
  org.gradle.jvmargs=-Xmx2048m -Dfile.encoding=UTF-8
  org.gradle.parallel=true
  org.gradle.caching=true

  android.useAndroidX=true
  android.nonTransitiveRClass=true

  kotlin.code.style=official
  ```
- **`local.properties`** (repo root, machine-specific, already covered by `.gitignore` — never commit it):
  ```properties
  sdk.dir=/home/guillermo/Android/Sdk
  ```
  From the stage that introduces `BuildConfig`-based key injection onward, this file is also where the real (disposable, training-only) OpenWeatherMap API key is defined as a Gradle property, read by `app/build.gradle.kts`.

If Claude needs to run a Gradle build in this environment without these files on disk, pass the equivalent values as command-line/env overrides instead of trying to write the files, e.g.:
```bash
export ANDROID_HOME=~/Android/Sdk
export GRADLE_OPTS="-Dorg.gradle.jvmargs=-Xmx3072m"   # the sandbox's default daemon heap (512 MiB) is too small for dexing
./gradlew :app:assembleDebug -Pandroid.useAndroidX=true
```

## Commands

```bash
# Debug build
export ANDROID_HOME=~/Android/Sdk
./gradlew :app:assembleDebug

# Unit tests (all)
./gradlew :app:testDebugUnitTest

# A single test class / method
./gradlew :app:testDebugUnitTest --tests "com.securitytraining.weatherapp.domain.usecase.GetCurrentWeatherUseCaseTest"
./gradlew :app:testDebugUnitTest --tests "*.GetCurrentWeatherUseCaseTest.invoke delegates*"

# Lint
./gradlew :app:lint
```

Local Android SDK lives at `~/Android/Sdk` (platforms up to `android-37`, build-tools up to `36.0.0`; no `cmdline-tools`/`sdkmanager` that works under JDK 21 — don't rely on `sdkmanager`, the SDK packages already present are sufficient for `compileSdk 37`). JDK 21 is the system default and satisfies AGP 9's JDK 17 minimum.

## Architecture

Single Gradle module (`app/`), Clean Architecture expressed through packages rather than a multi-module split — the app is deliberately simple; the security treatment is where the complexity lives, not the UI:

```
com.securitytraining.weatherapp
├── domain/         — pure Kotlin: WeatherRepository interface, GetCurrentWeatherUseCase, CurrentWeather model
├── data/           — Ktor-based WeatherApiService, DTOs (data/remote/dto), WeatherRepositoryImpl
├── presentation/   — Compose (WeatherScreen, WeatherViewModel, WeatherUiState) + theme/
└── di/             — Koin modules: NetworkModule (HttpClient), AppModule (repository/use case/view model bindings)
```

Key provisioning is the part that changes shape across security stages — as of v0.4 it's `BuildConfig.OPEN_WEATHER_API_KEY`, generated from `app/build.gradle.kts` reading `local.properties`/a Gradle property, read directly by `WeatherApiService` (no `core/` package, no DI involved; earlier stages used a since-deleted `core/AppConfig`+`core/ConfigLoader` pair).

Dependency injection is Koin, wired in `WeatherApplication.onCreate()`. Networking is Ktor with the OkHttp engine (`ktor-client-okhttp`) — chosen specifically because certificate pinning (a later security stage) is configured through OkHttp's `CertificatePinner`, reachable from Ktor's engine config block.

All versions are centralized in `gradle/libs.versions.toml` — add new dependencies there, not as inline version strings in a module's `build.gradle.kts`.

### A build-file quirk worth knowing

AGP 9+ has **built-in Kotlin support**: `org.jetbrains.kotlin.android` must *not* be applied (AGP applies it fails loudly if you do). The root `build.gradle.kts` instead pins the Kotlin Gradle Plugin version via a `buildscript { dependencies { classpath(...) } }` block, so it matches the `org.jetbrains.kotlin.plugin.compose` version exactly (they must be identical). Don't re-add a `kotlin-android` plugin alias when adding new modules.

## Commit/tag conventions for this repo specifically

Commits land on `main`, tagged per stage (`v0.1-hardcoded-key`, `v0.2-config-file-exposed`, …) — see `docs/evidencia-practica-es.md` for the full list and what each stage demonstrates. Don't squash or rewrite this history; its persistence across commits is itself part of the evidence (per the reference document's point that `.gitignore` only prevents *future* commits, not past ones).

GitHub repo setup, pushes, and Actions secrets configuration are handled by the project owner directly, not by Claude in this environment.
