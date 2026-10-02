# WeatherApp

A minimal Android weather app (Kotlin, Jetpack Compose, Clean Architecture, MVVM) consuming the OpenWeatherMap API — built as a hands-on case study for a security-training exercise on **API keys & secrets in code**.

**The app itself isn't the point.** Its git history is deliberately staged: it starts with common antipatterns (hardcoded secrets, exposed config files, late `.gitignore` additions) and progresses, commit by commit, through the fixes recommended for handling API keys in an Android client — `BuildConfig` + `local.properties`, certificate pinning, `EncryptedSharedPreferences` for a runtime token, and more. See [`docs/evidencia-practica-es.md`](docs/evidencia-practica-es.md) for the full stage-by-stage narrative with evidence (currently a Spanish draft; an English version will follow once it stabilizes).

## Status

✅ Done — `v0.9-final-hardening` closes the staged progression (`v0.1` → `v0.9`). See [`SECURITY.md`](SECURITY.md) for the final control-by-control status and [`docs/evidencia-practica-es.md`](docs/evidencia-practica-es.md) for the full stage-by-stage evidence.

## Setup

Requirements: JDK 17+, an Android SDK with `compileSdk 37` available, Gradle 9+ (via the included wrapper — no local Gradle install needed).

One file is intentionally **not** tracked in this repo, since it holds machine- and account-specific values. Create it yourself before building — see [`CLAUDE.md`](CLAUDE.md) for exact contents:

- `local.properties` (repo root) — Android Studio generates this automatically on first sync (`sdk.dir=...`). As of stage `v0.4`, it's also where you add your own OpenWeatherMap API key: `OPEN_WEATHER_API_KEY=<your key>`.

```bash
./gradlew :app:assembleDebug
./gradlew :app:testDebugUnitTest
```

## Why this exists

This project supports an assigned engineering objective on Security & Data Privacy ("Risks of API keys & secrets in code"). See [`CLAUDE.md`](CLAUDE.md) for architecture notes and conventions used throughout the staged history.

## License

MIT — see [`LICENSE.md`](LICENSE.md).
