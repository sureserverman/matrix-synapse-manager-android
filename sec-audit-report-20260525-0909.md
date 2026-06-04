# Security Review — matrix-synapse-manager-android

**Date (UTC):** 2026-05-25 09:09
**Scope:** Whole repository tree (`app/`, `core/*`, `feature/*`, `gradle/`, `.github/workflows/`).
**Excluded:** `**/build/` outputs; test/fixture code (INFO-only); gitignored developer AI-tooling (`.claude/`, `.cursor/`, `.codex/`, `.gemini/`, `AGENTS.md`, `GEMINI.md`, `CLAUDE.md`) — covered separately in the AI-tools note below; gitignored secrets (`keystore.properties`, `upload.keystore`, `local.properties`) — confirmed **not** committed via `git ls-files`.
**Inventory:** Kotlin 2.1.0 / Jetpack Compose, multi-module (app + 7 core + 11 feature). AGP 8.6.1, minSdk 26, targetSdk 35. Hilt 2.53.1, Room 2.6.1, Retrofit 2.11.0 + OkHttp 4.12.0, kotlinx-serialization. Auth: OAuth2/PKCE via AppAuth 0.11.1 (MSC3861 / Matrix Authentication Service). Token storage: EncryptedSharedPreferences (security-crypto 1.1.0-alpha06, AES256-GCM + Android Keystore). Handles **server-wide Synapse admin access tokens**.
**CVE feeds:** OSV (ok — queried directly), NVD (n/a — no CPE map), GHSA (n/a for Maven REST). **0 known vulnerabilities** across 20 dependencies.
**Findings:** 0 CRITICAL, 0 HIGH, 2 MEDIUM, 11 LOW, plus 1 advisory note.

> **Headline:** This is a well-engineered, security-conscious app. There are **no remotely-exploitable defects, no auth bypass, no injection, no TLS-trust bypass, no committed secrets, and no vulnerable dependencies.** Every finding below is defense-in-depth hardening of *local-device* or *CI-pipeline* attack surface. The single most valuable fix is gating OkHttp body logging on `BuildConfig.DEBUG` (#1).

> **Scoring note:** Scores are deterministic — `CVSS/severity (0–40) + Exposure (0–25) + Exploit-in-wild (0–20) + Auth (0–15)`. Because this is a client app, "Exposure" maps to how reachable the sink is. Local-only sinks (logcat, recents, on-disk DB) are not network-reachable, which is *why* nothing buckets above MEDIUM — that is an accurate reflection of risk, not a gap in the review.

---

## MEDIUM

### 1. OkHttp `Level.BODY` logging is enabled in **release** builds — admin/refresh tokens written to Logcat
- **File:** `core/network/src/main/kotlin/com/matrix/synapse/network/NetworkModule.kt:35` (also injected into the `@Named("refresh")` client at `:52` and the primary client at `:76`)
- **CWE:** CWE-532 (Insertion of Sensitive Information into Log File)
- **CVE(s):** none
- **Score:** 45 / 100 (Severity HIGH 28 + Exposure 15 + Exploit 0 + Auth 2, confidence: high)
- **Evidence:**
  ```kotlin
  fun provideLoggingInterceptor(): HttpLoggingInterceptor =
      HttpLoggingInterceptor().apply {
          level = HttpLoggingInterceptor.Level.BODY   // <-- never gated on BuildConfig.DEBUG
          redactHeader("Authorization")
      }
  ```
- **Why it matters:** `redactHeader("Authorization")` only strips the *outbound request header*. It does **not** redact response bodies. The MAS token-refresh response (`MasTokenRefresher`) and login/exchange responses (`MasTokenExchanger`) contain `access_token` and `refresh_token` **in the JSON body**, which `Level.BODY` writes verbatim to Logcat in the shipped release APK. These are *server-wide Synapse admin* tokens. Realistic capture vectors: `adb logcat` over USB debugging, user-submitted bug reports, MDM/log-collection agents, and any process granted `READ_LOGS`.
- **Recommended fix:** gate the level on the build type. There is no verbatim recipe in the reference packs for this OkHttp-specific pattern; the canonical fix is:
  ```kotlin
  level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY
          else HttpLoggingInterceptor.Level.NONE
  ```
  (Add `buildConfig = true` is already enabled in `app/build.gradle.kts`. For library modules, expose the flag via DI or a `BuildConfig` field.) Even in debug, consider `Level.HEADERS` plus a body redactor for token-bearing endpoints.
- **Sources:**
  - `references/mobile/android-runtime.md`
  - https://developer.android.com/privacy-and-security/security-tips
  - https://square.github.io/okhttp/features/interceptors/

### 2. CI third-party Actions pinned to mutable tags — including the release/signing pipeline
- **File:** `.github/workflows/release.yml:49` (`softprops/action-gh-release@v2`), `:13` (`actions/checkout@v4`), `:16` (`setup-java@v4`), `:24` (`android-actions/setup-android@v3`); same pattern throughout `.github/workflows/android-ci.yml` (incl. `reactivecircus/android-emulator-runner@v2`)
- **CWE:** CWE-829 (Inclusion of Functionality from Untrusted Control Sphere)
- **CVE(s):** none — but exploit class is demonstrated in the wild (`tj-actions/changed-files` tag-repoint compromise, March 2025)
- **Score:** 41 / 100 (Severity MEDIUM 16 + Exposure 15 + Exploit 10 + Auth 0, confidence: high)
- **Evidence:**
  ```yaml
  - name: Decode keystore
    run: echo "${{ secrets.KEYSTORE_BASE64 }}" | base64 -d > upload.keystore
  ...
  - uses: softprops/action-gh-release@v2      # mutable tag
  ```
- **Why it matters:** `release.yml` decodes the **signing keystore** (`KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`) into the runner before invoking third-party actions pinned to mutable tags. If any such action's maintainer is compromised and re-points the tag, the malicious version runs in a job that holds the production signing secrets — a supply-chain path to signing-key exfiltration or a tampered signed APK. `zizmor` flagged 22 `unpinned-uses` across both workflows.
- **Recommended fix** (quoted verbatim from `references/infra/gh-actions-permissions.md`):
  > **Before (dangerous):**
  > ```yaml
  > - uses: tj-actions/changed-files@v44
  > ```
  > **After (safe):**
  > ```yaml
  > - uses: tj-actions/changed-files@4c5f5d698fbf2d763b8c8fd0e16b6e9a7e6e2c1f  # v44.5.7
  >   # Dependabot will open a PR when a newer SHA exists; review and merge.
  > ```
- **Sources:**
  - `references/infra/gh-actions-permissions.md`
  - https://docs.github.com/en/actions/learn-github-actions/finding-and-customizing-actions#using-shas

---

## LOW

### 3. No `FLAG_SECURE` on the single Activity — admin tokens/PII visible in recents thumbnail & screenshots
- **File:** `app/src/main/kotlin/com/matrix/synapse/manager/MainActivity.kt` (no `FLAG_SECURE` anywhere in the codebase — confirmed by grep)
- **CWE:** CWE-200 (Exposure of Sensitive Information)
- **Score:** 23 / 100 (Severity MEDIUM 16 + Exposure 5 + Exploit 0 + Auth 2, confidence: high)
- **Why it matters:** Single-activity Compose app; `MainActivity` hosts the login screen (where an admin **pastes an access token**), the server list (admin URLs/usernames), and all admin-operation screens. Without `FLAG_SECURE`, these render into the system recents thumbnail and are capturable by screen-recording. Consider scoping the flag to sensitive screens only (login/token entry) to avoid blocking legitimate screenshots elsewhere.
- **Recommended fix** (quoted verbatim from `references/mobile/android-data.md`):
  > ```kotlin
  > window.setFlags(
  >     WindowManager.LayoutParams.FLAG_SECURE,
  >     WindowManager.LayoutParams.FLAG_SECURE
  > )
  > ```
  > Must be set BEFORE `setContentView`.
- **Sources:** `references/mobile/android-data.md`; https://cheatsheetseries.owasp.org/cheatsheets/Mobile_Application_Security_Cheat_Sheet.html

### 4. Network Security Config permits cleartext HTTP to the public domain `example.com`
- **File:** `app/src/main/res/xml/network_security_config.xml:8`
- **CWE:** CWE-319 (Cleartext Transmission of Sensitive Information)
- **Score:** 21 / 100 (Severity MEDIUM 16 + Exposure 5 + Exploit 0 + Auth 0, confidence: high)
- **Evidence:** `<domain includeSubdomains="false">example.com</domain>`
- **Why it matters:** The three loopback/emulator entries (`10.0.2.2`, `127.0.0.1`, `localhost`) are legitimate test scaffolding. `example.com` is a publicly-routable domain; if an admin ever names a server `example.com`, the admin bearer token would traverse cleartext HTTP. Drop the `example.com` entry — it serves no purpose in a shipped app.
- **Recommended fix** (quoted verbatim from `references/mobile/android-manifest.md`):
  > ```xml
  > <network-security-config>
  >     <base-config cleartextTrafficPermitted="false">
  >         <trust-anchors>
  >             <certificates src="system" />
  >         </trust-anchors>
  >     </base-config>
  > </network-security-config>
  > ```
  > Keep an emulator-only `domain-config` for `10.0.2.2`/`localhost` if needed for local testing; remove public domains.
- **Sources:** `references/mobile/android-manifest.md`; https://developer.android.com/privacy-and-security/security-config

### 5. CI: GITHUB_TOKEN credentials persisted in checkout / uploaded artifacts (`artipacked`)
- **File:** `.github/workflows/release.yml:13`, `.github/workflows/android-ci.yml` (all `actions/checkout` steps)
- **CWE:** CWE-522 (Insufficiently Protected Credentials)
- **Score:** 21 / 100 (Severity MEDIUM 16 + Exposure 5 + Exploit 0 + Auth 0, confidence: medium)
- **Why it matters:** `zizmor` flagged 6 `artipacked` instances — `actions/checkout` defaults to `persist-credentials: true`, leaving the token in `.git/config`; workflows that `upload-artifact` the workspace can leak it. Add `with: { persist-credentials: false }` to every checkout that does not push.
- **Sources:** `references/infra/gh-actions-secrets.md`; https://woodruffw.github.io/zizmor/audits/#artipacked

### 6. CI: release workflow uses build cache then produces a signed artifact (`cache-poisoning`)
- **File:** `.github/workflows/release.yml:18` (`cache: gradle` in `setup-java`)
- **CWE:** CWE-349 (Acceptance of Extraneous Untrusted Data With Trusted Data)
- **Score:** 18 / 100 (Severity MEDIUM 16 + Exposure 0 + Exploit 0 + Auth 2, confidence: medium)
- **Why it matters:** A cache entry poisoned from a less-trusted workflow run can be restored into the release job that builds the signed APK. For a reproducible-build / F-Droid project this undermines the build-integrity guarantee. Consider disabling caching in the release job, or scoping cache keys so the release job cannot read PR-writable caches.
- **Sources:** `references/infra/gh-actions-permissions.md`; https://woodruffw.github.io/zizmor/audits/#cache-poisoning

### 7. CI: `android-ci.yml` declares no `permissions:` block
- **File:** `.github/workflows/android-ci.yml:1`
- **CWE:** CWE-732 (Incorrect Permission Assignment for Critical Resource)
- **Score:** 11 / 100 (Severity LOW 6 + Exposure 5 + Exploit 0 + Auth 0, confidence: medium)
- **Why it matters:** Runs on `pull_request` (incl. forks) and inherits the repository-default `GITHUB_TOKEN` scope. Add `permissions: { contents: read }` at the top. (`release.yml` correctly declares `contents: write`.)
- **Sources:** `references/infra/gh-actions-permissions.md`; https://docs.github.com/en/actions/using-jobs/assigning-permissions-to-jobs

### 8. App-lock PIN: PBKDF2 at 10,000 iterations with no attempt lockout
- **File:** `feature/settings/src/main/kotlin/com/matrix/synapse/feature/settings/security/DefaultAppLockManager.kt:18,93,96`
- **CWE:** CWE-916 (Use of Password Hash With Insufficient Computational Effort)
- **Score:** 13 / 100 (Severity LOW 6 + Exposure 5 + Exploit 0 + Auth 2, confidence: medium)
- **Evidence:** `private const val PBKDF2_ITERATIONS = 10_000` (PBKDF2WithHmacSHA256). No `attempt`/`lockout`/`backoff` logic found in the app-lock package.
- **Why it matters:** A 4-digit PIN has only 10⁴ candidates; 10k PBKDF2 iterations are brute-forced in well under a second once the hash+salt (stored in EncryptedSharedPreferences) are extracted. For a short PIN the real defense is **rate-limiting/lockout after N attempts**, which is absent. Raise iterations toward OWASP-2023 (600k) *and* add exponential backoff + a max-attempts lockout.
- **Sources:** `references/auth/password-storage.md`; https://cheatsheetseries.owasp.org/cheatsheets/Password_Storage_Cheat_Sheet.html

### 9. Room database is unencrypted at rest (`audit_log`)
- **File:** `core/database/src/main/kotlin/com/matrix/synapse/database/DatabaseModule.kt:25`
- **CWE:** CWE-312 (Cleartext Storage of Sensitive Information)
- **Score:** 13 / 100 (Severity LOW 6 + Exposure 5 + Exploit 0 + Auth 2, confidence: high)
- **Evidence:** `Room.databaseBuilder(context, AppDatabase::class.java, "synapse-manager.db").build()` (no SQLCipher / passphrase).
- **Why it matters:** Verified contents (`AuditLogEntity`): `serverId`, `action`, `targetUserId`, `timestampMs`, redacted `detailsJson` — operational **metadata** (which admin action targeted which user), **not** tokens or credentials. `allowBackup=false` blocks ADB-backup extraction; reading the `.db` requires root/physical access. Low risk, but the audit trail is PII-adjacent. If encrypting, use SQLCipher (`net.zetetic:android-database-sqlcipher`) with a Keystore-held passphrase. (No SQLCipher-specific recipe in the reference packs; the `android-data.md` EncryptedSharedPreferences recipe is the nearest analog.)
- **Sources:** `references/mobile/android-data.md`; https://developer.android.com/topic/security/data

### 10. OAuth redirect uses a custom URI scheme (interception risk — largely mitigated by PKCE)
- **File:** `feature/auth/src/main/AndroidManifest.xml:27` — `android:scheme="com.matrix.synapse.manager"`, host `oauth`, path `/redirect`
- **CWE:** CWE-939 (Improper Authorization in Handler for Custom URL Scheme)
- **Score:** 11 / 100 (Severity LOW 6 + Exposure 5 + Exploit 0 + Auth 0, confidence: medium — **FP-suspected**)
- **Why it matters:** A rogue app registering the same custom scheme could receive the authorization code. **Mitigated:** PKCE S256 is confirmed enabled (AppAuth default; `OAuthLoginUseCase` requires a non-null `codeVerifier`), so a stolen code is unusable without the verifier — the residual risk is auth-flow DoS, not token theft. RFC 8252 considers PKCE-protected custom schemes acceptable for native apps; HTTPS App Links with `assetlinks.json` would eliminate the vector entirely if a hostable redirect domain is available.
- **Sources:** `references/mobile/android-manifest.md`; https://datatracker.ietf.org/doc/html/rfc8252#section-8.1

### 11. MAS error exceptions embed raw HTTP response bodies in their messages
- **File:** `feature/auth/src/main/kotlin/com/matrix/synapse/feature/auth/oauth/MasTokenExchanger.kt:84-85`; `MasClientRegistrar.kt:16-17`
- **CWE:** CWE-532 (Insertion of Sensitive Information into Log File)
- **Score:** 13 / 100 (Severity LOW 6 + Exposure 5 + Exploit 0 + Auth 2, confidence: medium)
- **Evidence:** `class MasTokenExchangeException(val code: Int, val responseBody: String) : RuntimeException("Token exchange failed: HTTP $code — $responseBody")`
- **Why it matters:** If an OIDC/MAS error response ever echoes token material, it propagates into the exception message → uncaught-exception logs / crash reporters. Low likelihood (error bodies should not carry valid tokens), but log only `code` + a sanitized error code rather than the full body.
- **Sources:** `references/mobile/android-runtime.md`

### 12. Dead/legacy `TokenStore.kt` uses a plain (unencrypted) DataStore with a misleading "encrypted" docstring
- **File:** `core/security/src/main/kotlin/com/matrix/synapse/security/TokenStore.kt:13,19-22`
- **CWE:** CWE-312 (latent)
- **Score:** 6 / 100 (Severity LOW 6 + Exposure 0 + Exploit 0 + Auth 0, confidence: high)
- **Why it matters:** This class persists `access_token`/`refresh_token` into a plain `DataStore<Preferences>` and its KDoc claims "encrypted DataStore," but the production `SecureTokenStore` binding in `SecurityModule` is `TokenStoreImpl` (EncryptedSharedPreferences). Grep confirms **no consumer injects `TokenStore`** — it is unreachable dead code. **At runtime tokens ARE encrypted.** Risk is purely latent: a future dev could wire this up and silently lose encryption, and the misleading docstring invites exactly that. **Delete the class** (or rename + fix the comment).
- **Sources:** `references/mobile/android-data.md`

### 13. Production use of `androidx.security:security-crypto 1.1.0-alpha06` (deprecated, alpha)
- **File:** `gradle/libs.versions.toml:16`
- **CWE:** CWE-1104 (Use of Unmaintained Third Party Components)
- **Score:** 11 / 100 (Severity LOW 6 + Exposure 5 + Exploit 0 + Auth 0, confidence: high)
- **Why it matters:** Jetpack Security Crypto is **deprecated** and this is a 2021-era **alpha** still in production. The crypto itself (AES256-GCM MasterKey, AES256-SIV keys, AES256-GCM values) is sound and there are no CVEs, but no upstream maintenance means no future fixes. Plan a migration to Tink-on-DataStore (or pin awareness that this dependency is end-of-life). Not urgent; track it.
- **Sources:** `references/mobile/android-data.md`; https://developer.android.com/reference/androidx/security/crypto/EncryptedSharedPreferences

---

## Advisory note (not scored)

### A. Local signing secret on disk — correctly kept out of git
- **File:** `keystore.properties` (gitignored), `upload.keystore` (gitignored)
- `keystore.properties` on this developer machine contains a real signing password (`storePassword`/`keyPassword` identical), and `upload.keystore` is present. **Both are gitignored and confirmed absent from `git ls-files`** — they are *not* in the repository or its history at HEAD. CI correctly uses GitHub Secrets (`KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, …) rather than committing them. No action required for the repo; for hygiene, avoid reusing one password for both store and key, and never let these files leave the machine. (Reported as advisory only — nothing is exposed via the distributed artifact or VCS.)

---

## AI-tooling configuration (gitignored — informational only)

The repo carries developer AI-tool configs (`.claude/`, `.cursor/mcp.json`, `.codex/`, `.gemini/`, `AGENTS.md`, `GEMINI.md`) that are **all gitignored** and **not shipped** in the APK or committed. They are out of the app's security scope, but one item is worth flagging to the developer:

- `.claude/settings.local.json` allowlists `Bash(...)` entries that invoke `claude --print --dangerously-skip-permissions`. Allowlisting a permission-bypassing nested-agent invocation means any future command matching that prefix runs unsandboxed on your machine. This is local-only dev config (not a repo/app risk), but consider tightening or removing those allow entries. All AI-config JSON files parsed as valid (`jq` structural check passed).

---

## Dependency CVE summary

All 20 declared dependencies were resolved against **OSV.dev** (`/v1/querybatch`, Maven ecosystem) directly at review time. **Zero known vulnerabilities.**

| Package | Version | CVEs | Status |
|---|---|---|---|
| org.jetbrains.kotlin:kotlin-stdlib | 2.1.0 | 0 | ok |
| org.jetbrains.kotlinx:kotlinx-coroutines-android | 1.9.0 | 0 | ok |
| org.jetbrains.kotlinx:kotlinx-serialization-json | 1.7.3 | 0 | ok |
| com.google.dagger:hilt-android | 2.53.1 | 0 | ok |
| androidx.hilt:hilt-navigation-compose | 1.2.0 | 0 | ok |
| androidx.room:room-runtime | 2.6.1 | 0 | ok |
| com.squareup.retrofit2:retrofit | 2.11.0 | 0 | ok |
| com.squareup.retrofit2:converter-kotlinx-serialization | 2.11.0 | 0 | ok |
| com.squareup.okhttp3:okhttp | 4.12.0 | 0 | ok |
| com.squareup.okhttp3:logging-interceptor | 4.12.0 | 0 | ok |
| androidx.security:security-crypto | 1.1.0-alpha06 | 0 | ok (deprecated — see #13) |
| androidx.datastore:datastore-preferences | 1.1.1 | 0 | ok |
| androidx.work:work-runtime-ktx | 2.9.1 | 0 | ok |
| net.openid:appauth | 0.11.1 | 0 | ok |
| androidx.navigation:navigation-compose | 2.8.2 | 0 | ok |
| androidx.activity:activity-compose | 1.9.2 | 0 | ok |
| androidx.lifecycle:lifecycle-runtime-ktx | 2.8.6 | 0 | ok |
| io.coil-kt.coil3:coil | 3.2.0 | 0 | ok |
| io.coil-kt.coil3:coil-network-okhttp | 3.2.0 | 0 | ok |
| org.burnoutcrew.composereorderable:reorderable-jvm | 0.9.6 | 0 | ok |

> Note: `build.gradle.kts` parses direct declarations only; no `gradle.lockfile` is committed, so deep transitive dependencies (e.g. those pulled by the Compose BOM `2024.09.00`) were not individually enumerated. Commit a `gradle.lockfile` to enable full transitive CVE coverage on future runs.

---

## Review metadata

- **Plugin:** sec-audit 1.14.0
- **Lanes dispatched:** `sec-expert` (Android stack, sonnet), `gh-actions` (actionlint + zizmor), `cve-enricher` (OSV/NVD/GHSA + KEV).
- **Lanes skipped — tools not installed:** `sast` (semgrep, bandit absent), `android` (mobsfscan, apkleaks, android-lint absent). ⚠ Installing `mobsfscan` + `semgrep` would add deterministic Kotlin/Android rule coverage on a future run.
- **Lanes n/a (no signal):** shell (no shell scripts), python (scripts/*.py have no manifest/package shape), ios/macos/windows/linux/k8s/iac/virt/go/rust/netcfg/image/webapp/webext.
- **ai-tools:** detected (`.claude/`, `.cursor/`, `.codex/`, `AGENTS.md`) but all gitignored/non-shipped — handled as an informational note above (jq structural validation passed) rather than a full lane.
- **Reference packs cited:** `mobile/android-manifest.md`, `mobile/android-data.md`, `mobile/android-runtime.md`, `auth/password-storage.md`, `infra/gh-actions-permissions.md`, `infra/gh-actions-secrets.md`.
- **CVE lookups:** 20 packages via OSV querybatch (2 batches), CISA KEV cross-referenced (1602 entries). Total HTTP requests well under the 500 cap.
- **Corrections applied during orchestration:** (a) the cve-enricher sub-agent reported all packages "offline" — re-verified directly against OSV (Maven *is* indexed) and corrected to "ok, 0 vulns"; (b) sec-expert's "Room/DataStore unencrypted (MEDIUM)" findings were verified against source — the active token store IS encrypted and `TokenStore.kt` is dead code, so those were downgraded to LOW with evidence.
- **Limits hit:** none.
