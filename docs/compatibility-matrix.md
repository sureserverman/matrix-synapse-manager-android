# Synapse Compatibility Matrix

This document maps each app feature to the minimum Synapse server version required.

## Tested Synapse Versions

| Synapse Version | Status |
|---|---|
| 1.73.x – 1.97.x | ✅ Fully supported |
| 1.61.x – 1.72.x | ⚠️ Supported (no suspend/unsuspend) |
| < 1.61.0 | ❌ Not supported |

## Feature / Endpoint Matrix

| Feature | Endpoint | Min Synapse | Notes |
|---|---|---|---|
| Server discovery | `GET /.well-known/matrix/client` | Any | Falls back to input URL on 404 |
| Admin login | `POST /_matrix/client/v3/login` | Any | `m.login.password` type |
| Server version | `GET /_synapse/admin/v1/server_version` | 1.0.0 | Used for capability detection |
| List users | `GET /_synapse/admin/v2/users` | 1.8.0 | Paginated via `next_token` |
| Get user | `GET /_synapse/admin/v2/users/{userId}` | 1.8.0 | |
| Create / update user | `PUT /_synapse/admin/v2/users/{userId}` | 1.8.0 | Upsert semantics |
| Lock / unlock user | `PUT /_synapse/admin/v2/users/{userId}` with `locked` | 1.93.0 | Capability-gated in UI |
| Suspend / unsuspend | `PUT /_synapse/admin/v1/suspend/{userId}` | **1.73.0** | Hidden on older servers |
| Deactivate user | `POST /_synapse/admin/v1/deactivate/{userId}` | 1.0.0 | |
| List user media | `GET /_synapse/admin/v1/users/{userId}/media` | 1.32.0 | Pagination and sort/filter query params per Synapse docs; optional `from_ts`/`until_ts` when supported by server |
| Delete user media (bulk) | `DELETE /_synapse/admin/v1/users/{userId}/media` | 1.32.0 | Same filters as list; no `next_token`; loops until a batch deletes 0 items |
| Delete media | `DELETE /_synapse/admin/v1/media/{serverName}/{mediaId}` | 1.32.0 | Best-effort; partial failures allowed |
| List room media | `GET /_synapse/admin/v1/room/{roomId}/media` | 1.32.0 | Returns MXC URIs; **only media referenced from unencrypted events** |
| Bulk delete local media (global) | `POST /_synapse/admin/v1/media/delete` | 1.32.0 | Synapse API (last access via `before_ts`); **not called from this app** — use server-side tools if needed |
| List devices | `GET /_synapse/admin/v2/users/{userId}/devices` | 1.28.0 | |
| Get device | `GET /_synapse/admin/v2/users/{userId}/devices/{deviceId}` | 1.28.0 | |
| Delete device | `DELETE /_synapse/admin/v2/users/{userId}/devices/{deviceId}` | 1.28.0 | Requires confirmation |
| Whois | `GET /_synapse/admin/v1/whois/{userId}` | 1.0.0 | Active-connection view |
| Background updates status | `GET /_synapse/admin/v1/background_updates/status` | 1.0.0 | View and control background jobs (V2) |
| Background updates enabled | `GET|POST /_synapse/admin/v1/background_updates/enabled` | 1.0.0 | Pause/resume updates |
| Start background job | `POST /_synapse/admin/v1/background_updates/start_job` | 1.0.0 | Run e.g. regenerate_directory, populate_stats_process_rooms |
| List event reports | `GET /_synapse/admin/v1/event_reports` | 1.0.0 | Moderation — paginated, filter by room_id, user_id (V2) |
| Event report detail | `GET /_synapse/admin/v1/event_reports/{report_id}` | 1.0.0 | Includes event_json |
| Delete event report | `DELETE /_synapse/admin/v1/event_reports/{report_id}` | 1.0.0 | Dismiss report |

## Capability Detection

`CapabilityService` fetches the server version on first use and caches the result per server ID.
The following feature flags are derived at runtime:

| Flag | Condition |
|---|---|
| `canSuspendUsers` | Synapse ≥ 1.73.0 |
| `canManageDevices` | Always `true` (available since early versions) |
| `canDeleteMedia` | Always `true` (available since early versions) |

UI elements gated by a capability flag are hidden (not just disabled) when the capability is absent.

## Authentication Requirements

All admin API endpoints require the `Authorization: Bearer <access_token>` header.
The token must belong to a Synapse **server administrator** account.
