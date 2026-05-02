# Localization

The app uses Android string resources for localization. All UI strings are defined in the **`:core:resources`** module.

## Supported locales

- **Default (English):** `res/values/strings.xml`
- **German:** `res/values-de/strings.xml`
- **Spanish:** `res/values-es/strings.xml`
- **French:** `res/values-fr/strings.xml`
- **Italian:** `res/values-it/strings.xml`
- **Japanese:** `res/values-ja/strings.xml`
- **Korean:** `res/values-ko/strings.xml`
- **Portuguese (Brazil):** `res/values-pt-rBR/strings.xml`
- **Russian:** `res/values-ru/strings.xml`
- **Chinese (Simplified):** `res/values-zh/strings.xml`
- **Ukrainian:** `res/values-uk/strings.xml`

The system locale selects the appropriate `values-*` folder at runtime; missing keys fall back to `values/strings.xml`.

## Using strings in Compose

1. Add a dependency on `:core:resources` in the module’s `build.gradle.kts` (already done for app and feature modules).
2. In Composables, use:
   - `stringResource(com.matrix.synapse.core.resources.R.string.<key>)` for static text
   - `stringResource(com.matrix.synapse.core.resources.R.string.<key>, arg1, arg2)` for format strings (e.g. `%1$d`)

Example (see `feature/auth/ui/LoginScreen.kt`):

```kotlin
import androidx.compose.ui.res.stringResource
import com.matrix.synapse.core.resources.R

// Static
Text(stringResource(R.string.admin_login))

// With format args
Text(stringResource(R.string.deactivate_users_message, state.selectedUserIds.size))
```

## Adding or changing strings

1. Edit `core/resources/src/main/res/values/strings.xml` (and any `values-<locale>/strings.xml`) with the same key.
2. Use placeholders for dynamic parts: `%1$s` (string), `%2$d` (integer), etc.

## Screen coverage

All user-facing screens use `stringResource()` for titles, labels, buttons, dialogs, empty states, and accessibility content. Covered areas:

- **App:** MainActivity (PIN), AppNavHost (tabs), MoreScreen, RearrangeTabsScreen
- **Auth:** LoginScreen
- **Servers:** ServerListScreen, ServerFormScreen
- **Users:** UserListScreen, UserDetailScreen, UserEditScreen
- **Rooms:** RoomListScreen, RoomDetailScreen
- **Devices:** DeviceListScreen, WhoisScreen
- **Stats:** ServerDashboardScreen
- **Media:** MediaListScreen, MediaDetailScreen (filters, selection/delete, user/room-scoped actions, media info)
- **Federation:** FederationListScreen, FederationDetailScreen
- **Jobs:** BackgroundJobsScreen
- **Moderation:** EventReportsScreen, EventReportDetailScreen
- **Settings:** AppLockSettingsScreen
- **Core UI:** SynapseTopBar (server tap and back descriptions)

When adding new UI text, add the key to `values/strings.xml` (and locale files) and use `stringResource(R.string.<key>)` in Composables.
