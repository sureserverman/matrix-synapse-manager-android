# Matrix Synapse Manager — Android

<p align="center">
  <img src="app/src/main/res/mipmap-xxxhdpi/ic_launcher.png" alt="App icon" width="128" height="128">
</p>

A production-ready Android app for administering [Synapse](https://matrix.org/docs/projects/server/synapse) homeservers. Manage users, devices, rooms, media, federation, and server health across multiple Synapse instances from a single, secure, mobile-first admin panel.

## Features

- **Multi-server management** — Add unlimited Synapse servers via `.well-known` discovery or manual URL; switch between them from the home screen.
- **Secure authentication** — Admin login per server; access tokens stored in Android Keystore (`EncryptedSharedPreferences`); passwords are never persisted.
- **User lifecycle** — List, search (paginated), create, edit, lock/unlock, suspend/unsuspend, and deactivate users; optional media erasure before deactivation.
- **Device & session control** — View, inspect, and revoke device sessions; whois active-connection view.
- **Rooms** — List rooms, view details, block/unblock, delete, add members, set room admin.
- **Media** — List room/user media (room listings are unencrypted-events only per Synapse), MXC-aware per-item selection and deletes, optional scoped user bulk delete and delete-listed-room-media (`DELETE …/users/{id}/media`, room-scoped deletes via listed MXC rows).
- **Federation** — List federated servers, view details, reset connection.
- **Server dashboard** — View server version and key metrics; background jobs (updates status, pause/resume, start job).
- **Moderation** — List and manage event reports (view, dismiss).
- **Audit logging** — Destructive actions (deactivate, delete device/room/media, federation reset, etc.) are recorded locally in a Room database (no in-app audit viewer/export; re-verify before each release if you rely on this behavior).
- **App lock** — Optional PIN gate on app resume; configurable in Settings.

## Architecture

```
:app                    — Main activity, Compose navigation, tab bar, Hilt
:core:network           — Retrofit + OkHttp + kotlinx-serialization, capability detection
:core:security          — EncryptedSharedPreferences token store (no password persistence)
:core:database          — Room audit log (AuditLogEntity, AuditLogDao, AuditLogger)
:core:model             — Domain models (Server, ServerCapabilities)
:core:ui                — Shared UI (SynapseTopBar, EmptyState, Spacing)
:core:testing           — Standalone contract tests for Synapse API (no app dependency)
:feature:auth           — Login screen, session validation
:feature:servers        — Server discovery (.well-known), profile management
:feature:users          — User list/detail/create/edit, deactivate flow
:feature:devices        — Device list, delete, whois
:feature:rooms          — Room list/detail, block, delete, members, admin
:feature:media          — Media list/detail, quarantine, delete, bulk actions
:feature:federation     — Federation list/detail, reset connection
:feature:stats          — Server dashboard (version, metrics)
:feature:jobs           — Background jobs (updates status, pause/resume, start job)
:feature:moderation     — Event reports list/detail, dismiss
:feature:settings       — App lock settings, PIN create/change flow
```

Clean Architecture + MVVM: ViewModels call use cases, use cases call repositories, repositories use per-server Retrofit instances from `RetrofitFactory`.

## Requirements

| Requirement | Value |
|-------------|--------|
| Min Android | API 26 (Android 8.0) |
| Target Android | API 35 (Android 15) |
| Min Synapse | See [Compatibility matrix](docs/compatibility-matrix.md) |
| Build | JDK 17, AGP 8.6.x, Kotlin 2.1.x |

## Building

```bash
git clone https://github.com/sureserverman/matrix-synapse-manager-android.git
cd matrix-synapse-manager-android
```

Set the Android SDK path (or use `ANDROID_HOME`):

```bash
echo "sdk.dir=$HOME/Android/Sdk" > local.properties
```

Build and test:

```bash
./gradlew :app:assembleDebug
./gradlew testDebugUnitTest
./gradlew lint testDebugUnitTest
```

Install the debug build on a device or emulator:

```bash
./gradlew :app:installDebug
```

## Security

See [docs/threat-model.md](docs/threat-model.md) for the full threat model. Summary:

- Admin **passwords are never stored**; only short-lived access tokens are persisted.
- Tokens are kept in `EncryptedSharedPreferences` backed by Android Keystore (AES-256-GCM).
- Cleartext HTTP is disabled (`usesCleartextTraffic=false`).
- Destructive operations require explicit confirmation (and, for deactivation, typing the user ID).
- Optional app lock (PIN) protects the app when the device is unlocked.

## Documentation

| Document | Description |
|----------|-------------|
| [docs/privacy.md](docs/privacy.md) | Privacy policy — what data the app stores and where it is sent |
| [docs/threat-model.md](docs/threat-model.md) | Security design, assets, and mitigations |
| [docs/compatibility-matrix.md](docs/compatibility-matrix.md) | Synapse versions and admin API endpoints used by the app |
| [docs/publishing-f-droid.md](docs/publishing-f-droid.md) | How to publish the app to F-Droid |
| [docs/publishing-google-play.md](docs/publishing-google-play.md) | How to publish the app to Google Play |
| [docs/UI_UX_ANALYSIS_AND_IMPROVEMENTS.md](docs/UI_UX_ANALYSIS_AND_IMPROVEMENTS.md) | UI/UX analysis and spacing/empty-state alignment |
| [docs/AUDIT_LOG_AND_DELETE_UX_PROPOSALS.md](docs/AUDIT_LOG_AND_DELETE_UX_PROPOSALS.md) | Proposals for audit log and delete flows |

## Contributing

1. Create a feature branch from `main`.
2. Follow TDD where applicable: failing test → implement → green → commit.
3. Run `./gradlew lint testDebugUnitTest` before opening a PR.
4. Respect the project’s security and build rules (e.g. no cleartext, no password persistence).

## License

This project is licensed under the [Apache License 2.0](LICENSE).  
Copyright 2026 The Matrix Synapse Manager Android Project.  
Replace the copyright holder in [LICENSE](LICENSE) with your name or organization if you fork or distribute.
