# Security

WeatherApp is a security-training artifact, not a product with its own release process or user base — see [`CLAUDE.md`](CLAUDE.md) for what it actually is and why its git history is staged the way it is. This document is the project's own close-out (stage `v0.9-final-hardening`): for every risk/control pair in §13 ("Resumen de riesgos y controles") of the reference document `api-keys-y-secretos-en-el-codigo-v0.4.md` (Objective 6, not part of this repo), it states what this project actually did about it — **applied**, **not applicable** (with the verified reason), or **out of scope** (with the explicit decision behind it) — linking back to the stage and evidence in [`docs/evidencia-practica-es.md`](docs/evidencia-practica-es.md).

Nothing here is "not applicable" by omission — every row was checked against this project's actual code, CI pipeline, or a verified, provider-specific finding, not assumed.

## Control matrix

| Risk / control (reference doc, §13) | Status | Detail |
|---|---|---|
| Hardcoded secret in source code | **Applied** | `v0.1` demonstrates the antipattern; `v0.4` replaces it with `BuildConfig` + `local.properties`/CI secret; `v0.9` adds its own gitleaks scan on every push/PR. Honest limit: `BuildConfig` removes the key from source and VCS, but not from the compiled binary (see the `apktool` finding in `v0.4`). |
| Secret persisting in git history | **Applied (demonstrated)** | `v0.3` demonstrates that a late `.gitignore` erases nothing from the past; `v0.8` demonstrates the purge mechanics (`git-filter-repo`) on a disposable clone. The real mitigation once a secret is already public remains rotation (see `v0.4`), not the purge — explicitly documented as such in `v0.8`. |
| Exposed configuration files | **Applied** | `v0.2` exposes the key in `config.json` without `.gitignore`; `v0.3` adds `.gitignore` (too late, see previous row); `v0.4` removes the whole mechanism. |
| Keys in documentation and examples | **Applied (cross-cutting practice)** | Every "insecure" value in this repo — including this very document and `docs/evidencia-practica-es.md` — is a fake, format-plausible placeholder; no real key was ever pasted into any tracked file (project convention since `v0.1`). |
| Keys in the client (web) | **Not applicable** | This app is a native Android client, not a web client — it maps to Scenario D of the reference document, not Scenario C. |
| Keys embedded in the APK/AAB — backend proxy | **Out of scope (explicit decision)** | Scope decision made during project planning: the exercise is limited to the Android client side, with no backend of its own, to focus on what's achievable from there. |
| Keys embedded in the APK/AAB — package + SHA-1 restriction | **Not applicable (verified finding)** | `v0.5`: OpenWeatherMap's dashboard offers no restriction by `applicationId`, signing SHA-1 fingerprint, domain, or IP — unlike Google Maps Platform, which does offer it (checked against both providers' official docs). |
| Keys embedded in the APK/AAB — Keystore/`EncryptedSharedPreferences` for runtime tokens | **Applied** | `v0.7`: an anonymous install token, generated once and persisted encrypted. Honest note documented there too: the `androidx.security.crypto` library has been deprecated upstream since `1.1.0-beta01`; kept anyway to stay aligned with what the reference document describes. |
| Keys embedded in the APK/AAB — certificate pinning | **Applied** | `v0.6`: `OkHttp.CertificatePinner` with real SPKI pins (leaf + backup intermediate CA), verified both in an isolated JVM test and on-device (real Logcat showing `SSLPeerUnverifiedException`). |
| Keys embedded in the APK/AAB — Play Integrity API | **Out of scope (explicit decision)** | Documented only as a recommended complementary control; never implemented — scope decision made during project planning. |
| Secrets in CI/CD and logs | **Applied** | `v0.3` demonstrates the antipattern (literal key in the YAML + `echo`'d to the log); `v0.4` replaces it with an encrypted GitHub Actions secret (masked in the log, verified with a real run); `v0.9` adds a gitleaks job over the full history and a job that confirms the build fails if the secret is absent. |
| Interception in transit | **Applied** | HTTPS by default against the API (Ktor/OkHttp) + certificate pinning (`v0.6`). mTLS between services doesn't apply — there are no first-party services in this project. |
| Long-lived key | **Provider limitation (partial)** | OpenWeatherMap offers no key expiration of its own (the same kind of structural limitation as the SHA-1 restriction row, `v0.5`). Rotation itself was exercised conceptually in `v0.4` (a second placeholder value simulating "the key has already rotated", reconfirmed with `apktool`). |
| Excessive permissions | **Not applicable (verified finding, `v0.5`)** | There's no scoping granularity available at all in OpenWeatherMap's dashboard (no IP, domain, endpoint, or quota option) — it's not that permissions are excessive by misconfiguration, it's that no option to narrow them exists. |
| Shared key / no traceability | **Out of scope** | A single-environment, single-key project by exercise design — there's no multi-environment/multi-team scenario to exercise here. |
| Abuse of a compromised key (rate limiting, gateway, monitoring) | **Out of scope** | Requires a backend/gateway of its own (see the backend-proxy row, out of scope). OpenWeatherMap's limits apply at the account level and aren't controllable from the Android client. |
| Lack of identity and fine-grained authorization (OAuth/OIDC/JWT/RBAC) | **Out of scope** | This app has no authentication system of its own. The `v0.7` token is an anonymous identifier with no authorization attached — explicitly documented there as *not* equivalent to an auth system. |
| Disruptive rotation | **Observed limitation, unresolved** | Honest finding: since the key lives in `BuildConfig` (a compile-time constant), rotating it requires a new build/release — this project doesn't automate or centralize that (a backend proxy would, and that's out of scope). The backup-pin pattern from `v0.6` does solve the analogous problem for TLS certificate rotation, but not for the API key. |
| Improvised response (playbook) | **Partially exercised** | `v0.3` (simulated pivot), `v0.4` (rotation simulation), and `v0.5` (dashboard review) are an informal walkthrough, not a written playbook. See the "What to do in a real leak" section below for the minimal version of that playbook, synthesized from those stages. |
| AI agents with broad credentials | **Applied (meta, to the exercise's own process)** | An honest observation about how this very project was built: Claude (the AI agent used) never had access to the real key, has `*.properties` file read/write permanently blocked by the permission system, and never ran a real `git push` — Guillermo did every push and every action involving the real key himself. This is a real instance, not planned as a "stage," of the least-privilege control for AI agents. |
| Insufficient per-resource authorization (BOLA) | **Not applicable** | A single public weather-lookup-by-city endpoint, with no per-user resources — there's no per-resource authorization model that could be misconfigured. |
| Leaks via responses and errors | **Applied (incidental finding, `v0.6`)** | `WeatherViewModel`/`WeatherScreen` already show a generic error message instead of the raw exception by default — confirmed while diagnosing `v0.6` (a temporary `Log.e` had to be added because, by the code's existing design, the real exception wasn't surfaced anywhere observable). Not a deliberate stage; it's the real, verified state of the code. |

## What to do in a real leak (synthesized from `v0.3`–`v0.5` and `v0.8`)

This project never leaked a real key, but its stages did exercise, in parts, what the right response would look like:

1. **Revoke and rotate first, always.** It's the only mitigation that truly invalidates an already-exposed secret (`v0.3`→`v0.4`). Nothing below replaces this step.
2. **Don't assume `.gitignore` or deleting the file is enough.** The value remains recoverable from earlier commits (`v0.3`, `git log --all` / `git show <sha>:<path>`).
3. **Purging history is additional hygiene, not the primary response.** `git-filter-repo` on a clone (never on history that someone may have already cloned/forked) rewrites the local blobs, but doesn't reach remotes/forks/caches that already exist (`v0.8`).
4. **Check which provider controls actually exist *before* assuming there's restriction by app/domain/IP** (`v0.5`) — if there aren't any, rotation and minimizing exposure are practically the only client-side controls left.
5. **Confirm masking in CI** once the key is rotated (a registered secret, not a YAML literal), and don't assume it without a real run (`v0.4`).

## Overall scope of this exercise

- Native Android client only — no backend/proxy of its own (an explicit decision, not a time constraint).
- A single provider (OpenWeatherMap), a single environment, a single key — no multi-environment scenario was exercised.
- Play Integrity API and service-to-service mTLS are documented as recommended complementary controls, never implemented.
- Every stage (`v0.1` through `v0.9`) and its real evidence (commands, logs, pending screenshots) lives in [`docs/evidencia-practica-es.md`](docs/evidencia-practica-es.md).
