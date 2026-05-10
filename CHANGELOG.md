# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.3.3] - 2026-05-10

### Changed

- Release APK now signs with v2/v3 schemes only; legacy v1 (JAR) signing is
  disabled. Required for F-Droid reproducible-build verification under
  `AllowedAPKSigningKeys` — leftover `META-INF/CERT.RSA`, `CERT.SF`, and
  `MANIFEST.MF` files from v1 signing were causing F-Droid's content diff
  against rebuilt-from-source APKs to fail. minSdk is 26, so v2/v3 covers
  every supported device.

## [1.3.2] - 2026-05-08

### Fixed

- Accounts screen returning HTTP 400 on MAS-delegated Synapse homeservers.
  The `GET /_synapse/admin/v2/users` handler rejects requests when the
  `guests` parameter resolves to its `True` default while auth delegation is
  enabled (`"The guests parameter is not supported when delegating to MAS."`).
  The app now sends `guests=false` explicitly. (#3)

## [1.3.0] - 2026-05-04

### Added

- OAuth 2.0 + PKCE login against Matrix Authentication Service (MAS / MSC3861).
  When a homeserver advertises `org.matrix.msc2965.authentication`, the app
  drives the auth-code flow in a Custom Tab and requests the
  `urn:synapse:admin:*` scope. Fixes admin endpoints returning 403 against
  MAS-fronted Synapse deployments. (#3)
- Dynamic OAuth client registration (RFC 7591) — no manual config required.
- MAS refresh-token rotation with single-flight HTTP coalescing across
  concurrent admin calls; transparent 401-retry via OkHttp Authenticator.
- RFC 7009 token revocation on logout for OAuth servers.
- Per-server persistence of refresh tokens, OAuth client IDs, and issued-at
  timestamps in `EncryptedSharedPreferences`.

### Changed

- Login screen now branches by detected auth strategy (OAuth vs password).
- Hardened the OAuth launch path: a device with no browser/Custom Tab now
  surfaces a recoverable error instead of crashing.

### Fixed

- Admin API calls returning 403 against rollenspiel.chat and other
  MAS-fronted Synapse 1.122+ deployments. (#3)

## [1.2.0] - 2026-04-28

### Added

- Adaptive launcher icons with monochrome layer and white background.
- Fastlane en-US phone screenshots for F-Droid metadata.

### Fixed

- Duplicate `META-INF` resources in feature androidTest builds causing CI failures.
- Missing newline in F-Droid YAML configuration.

## [1.1.0] - 2026-04-XX

### Added

- MXC-aware media flows: user media list/delete, room media list (unencrypted events only).
- Localized media UI strings.

## [1.0.2] - 2026-04-XX

### Added

- `dependenciesInfo` block to exclude dependency metadata from APK/bundle (reproducibility).
- F-Droid fastlane icon and author metadata.

### Fixed

- CI: grant `contents:write` permission to release workflow.

## [1.0.0] - 2026-04-XX

Initial release. Multi-server Synapse admin panel with user lifecycle, device
control, room management, media, federation, background jobs, moderation, and
audit logging. Password-based authentication with `EncryptedSharedPreferences`
token storage.
