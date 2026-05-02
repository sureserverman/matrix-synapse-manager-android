package com.matrix.synapse.manager

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.compose.NavHost
import com.matrix.synapse.core.resources.R
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.matrix.synapse.feature.auth.ui.LoginScreen
import com.matrix.synapse.feature.devices.ui.DeviceListScreen
import com.matrix.synapse.feature.devices.ui.WhoisScreen
import com.matrix.synapse.feature.servers.ui.ServerFormScreen
import com.matrix.synapse.feature.servers.ui.ServerListScreen
import com.matrix.synapse.feature.settings.ui.AppLockSettingsScreen
import com.matrix.synapse.feature.users.ui.UserDetailScreen
import com.matrix.synapse.feature.users.ui.UserEditScreen
import com.matrix.synapse.feature.users.ui.UserListScreen
import com.matrix.synapse.feature.rooms.ui.RoomDetailScreen
import com.matrix.synapse.feature.rooms.ui.RoomListScreen
import com.matrix.synapse.feature.stats.ui.ServerDashboardScreen
import com.matrix.synapse.feature.media.ui.MediaListScreen
import com.matrix.synapse.feature.media.ui.MediaDetailScreen
import com.matrix.synapse.feature.federation.ui.FederationListScreen
import com.matrix.synapse.feature.federation.ui.FederationDetailScreen
import com.matrix.synapse.feature.jobs.ui.BackgroundJobsScreen
import com.matrix.synapse.feature.moderation.ui.EventReportDetailScreen
import com.matrix.synapse.feature.moderation.ui.EventReportsScreen
import com.matrix.synapse.manager.MoreScreen
import com.matrix.synapse.manager.tabs.TabItemId
import com.matrix.synapse.manager.tabs.iconForTabItem
import com.matrix.synapse.manager.tabs.TabOrderRepository
import androidx.lifecycle.compose.collectAsStateWithLifecycle

private enum class MainTab(val routePattern: String, val labelResId: Int) {
    Users("UserList", R.string.tab_users),
    Rooms("RoomList", R.string.tab_rooms),
    Stats("ServerDashboard", R.string.tab_stats),
    Settings("Settings", R.string.tab_settings),
    More("More", R.string.nav_more),
}

private val TAB_ROUTE_PATTERNS = listOf(
    "UserList", "RoomList", "ServerDashboard", "Settings", "More",
    "FederationList", "BackgroundJobs", "EventReportsList"
)

private fun String?.isTabRoute(): Boolean = this != null && TAB_ROUTE_PATTERNS.any { this.contains(it) }

private fun String?.selectedTab(): MainTab = when {
    this == null -> MainTab.Users
    this.contains(MainTab.Users.routePattern) -> MainTab.Users
    this.contains(MainTab.Rooms.routePattern) -> MainTab.Rooms
    this.contains(MainTab.Stats.routePattern) -> MainTab.Stats
    this.contains(MainTab.Settings.routePattern) -> MainTab.Settings
    this.contains(MainTab.More.routePattern) -> MainTab.More
    else -> MainTab.Users
}

private fun String?.routeToTabItemId(): TabItemId? = when {
    this == null -> null
    this.contains("UserList") -> TabItemId.Users
    this.contains("RoomList") -> TabItemId.Rooms
    this.contains("ServerDashboard") -> TabItemId.Stats
    this.contains("Settings") && !this.contains("AppLock") -> TabItemId.Settings
    this.contains("More") -> null
    this.contains("FederationList") -> TabItemId.Federation
    this.contains("BackgroundJobs") -> TabItemId.BackgroundJobs
    this.contains("EventReportsList") -> TabItemId.EventReports
    else -> null
}

private fun tabLabelResId(id: TabItemId): Int = when (id) {
    TabItemId.Users -> R.string.tab_users
    TabItemId.Rooms -> R.string.tab_rooms
    TabItemId.Stats -> R.string.tab_stats
    TabItemId.Settings -> R.string.tab_settings
    TabItemId.Federation -> R.string.tab_federation
    TabItemId.BackgroundJobs -> R.string.tab_jobs
    TabItemId.EventReports -> R.string.tab_reports
}

@Composable
fun AppNavHost(
    modifier: Modifier = Modifier,
    tabOrderRepository: TabOrderRepository,
) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showTabs = currentRoute.isTabRoute()
    val selectedTab = currentRoute.selectedTab()
    val tabOrder by tabOrderRepository.order.collectAsStateWithLifecycle(initialValue = TabItemId.defaultOrder)
    val mainTabIds = tabOrder.take(4)
    val currentTabId = currentRoute.routeToTabItemId()
    val selectedTabIndex = when {
        currentRoute?.contains("More") == true -> 4
        currentTabId != null -> mainTabIds.indexOf(currentTabId).takeIf { it >= 0 } ?: 4
        else -> 0
    }
    val (serverId, serverUrl) = when {
        backStackEntry == null -> "" to ""
        currentRoute?.contains("UserList") == true -> run {
            val r = backStackEntry!!.toRoute<UserList>()
            r.serverId to r.serverUrl
        }
        currentRoute?.contains("RoomList") == true -> run {
            val r = backStackEntry!!.toRoute<RoomList>()
            r.serverId to r.serverUrl
        }
        currentRoute?.contains("ServerDashboard") == true -> run {
            val r = backStackEntry!!.toRoute<ServerDashboard>()
            r.serverId to r.serverUrl
        }
        currentRoute?.contains("Settings") == true -> run {
            val r = backStackEntry!!.toRoute<Settings>()
            r.serverId to r.serverUrl
        }
        currentRoute?.contains("More") == true -> run {
            val r = backStackEntry!!.toRoute<More>()
            r.serverId to r.serverUrl
        }
        currentRoute?.contains("FederationList") == true -> run {
            val r = backStackEntry!!.toRoute<FederationList>()
            r.serverId to r.serverUrl
        }
        currentRoute?.contains("BackgroundJobs") == true -> run {
            val r = backStackEntry!!.toRoute<BackgroundJobs>()
            r.serverId to r.serverUrl
        }
        currentRoute?.contains("EventReportsList") == true -> run {
            val r = backStackEntry!!.toRoute<EventReportsList>()
            r.serverId to r.serverUrl
        }
        else -> run {
            val r = backStackEntry!!.toRoute<More>()
            r.serverId to r.serverUrl
        }
    }

    androidx.compose.material3.Scaffold(
        bottomBar = {
            if (showTabs && backStackEntry != null && serverId.isNotEmpty()) {
                NavigationBar {
                    mainTabIds.forEachIndexed { index, tabItemId ->
                        val isSelected = selectedTabIndex == index
                        NavigationBarItem(
                            selected = isSelected,
                            onClick = {
                                when (tabItemId) {
                                    TabItemId.Users -> navController.navigate(UserList(serverId, serverUrl)) {
                                        popUpTo<Login> { inclusive = false }
                                        launchSingleTop = true
                                    }
                                    TabItemId.Rooms -> navController.navigate(RoomList(serverId, serverUrl)) {
                                        popUpTo<Login> { inclusive = false }
                                        launchSingleTop = true
                                    }
                                    TabItemId.Stats -> navController.navigate(ServerDashboard(serverId, serverUrl)) {
                                        popUpTo<Login> { inclusive = false }
                                        launchSingleTop = true
                                    }
                                    TabItemId.Settings -> navController.navigate(Settings(serverId, serverUrl)) {
                                        popUpTo<Login> { inclusive = false }
                                        launchSingleTop = true
                                    }
                                    TabItemId.Federation -> navController.navigate(FederationList(serverId, serverUrl)) {
                                        popUpTo<Login> { inclusive = false }
                                        launchSingleTop = true
                                    }
                                    TabItemId.BackgroundJobs -> navController.navigate(BackgroundJobs(serverId, serverUrl)) {
                                        popUpTo<Login> { inclusive = false }
                                        launchSingleTop = true
                                    }
                                    TabItemId.EventReports -> navController.navigate(EventReportsList(serverId, serverUrl)) {
                                        popUpTo<Login> { inclusive = false }
                                        launchSingleTop = true
                                    }
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = iconForTabItem(tabItemId),
                                    contentDescription = stringResource(tabLabelResId(tabItemId)),
                                )
                            },
                            label = { Text(stringResource(tabLabelResId(tabItemId))) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                selectedTextColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            ),
                        )
                    }
                    NavigationBarItem(
                        selected = selectedTabIndex == 4,
                        onClick = {
                            navController.navigate(More(serverId, serverUrl)) {
                                popUpTo<Login> { inclusive = false }
                                launchSingleTop = true
                            }
                        },
                        icon = {
                            Icon(
                                imageVector = Icons.Filled.MoreVert,
                                contentDescription = stringResource(R.string.nav_more),
                            )
                        },
                        label = { Text(stringResource(R.string.nav_more)) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            selectedTextColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        ),
                    )
                }
            }
        },
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = ServerList,
            modifier = modifier.padding(paddingValues),
        ) {
        composable<ServerList> {
            ServerListScreen(
                onAddServer = { navController.navigate(ServerSetup) },
                onEditServer = { serverId ->
                    navController.navigate(ServerEdit(serverId))
                },
                onOpenLogin = { serverId, serverUrl ->
                    navController.navigate(Login(serverId, serverUrl)) {
                        popUpTo<ServerList> { inclusive = true }
                    }
                },
                onOpenUserList = { serverId, serverUrl ->
                    navController.navigate(UserList(serverId, serverUrl)) {
                        popUpTo<ServerList> { inclusive = true }
                    }
                },
            )
        }

        composable<ServerSetup> {
            ServerFormScreen(
                onServerAdded = { serverId, serverUrl ->
                    navController.navigate(Login(serverId, serverUrl)) {
                        popUpTo<ServerSetup> { inclusive = true }
                    }
                },
            )
        }

        composable<ServerEdit> { backStack ->
            val route = backStack.toRoute<ServerEdit>()
            ServerFormScreen(
                serverIdToEdit = route.serverId,
                onServerAdded = { _, _ -> },
                onServerUpdated = { navController.popBackStack() },
            )
        }

        composable<Login> { backStack ->
            val route = backStack.toRoute<Login>()
            LoginScreen(
                serverUrl = route.serverUrl,
                serverId = route.serverId,
                onLoginSuccess = {
                    navController.navigate(UserList(route.serverId, route.serverUrl)) {
                        popUpTo<Login> { inclusive = true }
                    }
                },
            )
        }

        composable<UserList> { backStack ->
            val route = backStack.toRoute<UserList>()
            UserListScreen(
                serverId = route.serverId,
                serverUrl = route.serverUrl,
                onUserClick = { userId ->
                    navController.navigate(UserDetail(route.serverId, route.serverUrl, userId))
                },
                onAddUser = {
                    navController.navigate(UserEdit(route.serverUrl, ""))
                },
                onServers = {
                    navController.navigate(ServerList) { launchSingleTop = true }
                },
                onSettings = {
                    navController.navigate(AppLockSettings)
                },
                onRooms = {
                    navController.navigate(RoomList(route.serverId, route.serverUrl))
                },
                onDashboard = {
                    navController.navigate(ServerDashboard(route.serverId, route.serverUrl))
                },
                onMedia = {
                    navController.navigate(MediaList(route.serverId, route.serverUrl))
                },
                onFederation = {
                    navController.navigate(FederationList(route.serverId, route.serverUrl))
                },
            )
        }

        composable<UserDetail> { backStack ->
            val route = backStack.toRoute<UserDetail>()
            UserDetailScreen(
                serverUrl = route.serverUrl,
                serverId = route.serverId,
                userId = route.userId,
                onEdit = { navController.navigate(UserEdit(route.serverUrl, route.userId)) },
                onDevices = { navController.navigate(DeviceList(route.serverUrl, route.userId)) },
                onWhois = { navController.navigate(Whois(route.serverUrl, route.userId)) },
                onMedia = {
                    navController.navigate(
                        MediaList(route.serverId, route.serverUrl, filterUserId = route.userId)
                    )
                },
                onBack = { navController.popBackStack() },
            )
        }

        composable<UserEdit> { backStack ->
            val route = backStack.toRoute<UserEdit>()
            UserEditScreen(
                serverUrl = route.serverUrl,
                existingUserId = route.userId.ifEmpty { null },
                onSaved = { navController.popBackStack() },
            )
        }

        composable<DeviceList> { backStack ->
            val route = backStack.toRoute<DeviceList>()
            DeviceListScreen(serverUrl = route.serverUrl, userId = route.userId)
        }

        composable<Whois> { backStack ->
            val route = backStack.toRoute<Whois>()
            WhoisScreen(serverUrl = route.serverUrl, userId = route.userId)
        }

        composable<AppLockSettings> {
            AppLockSettingsScreen(
                onRearrangeTabs = { navController.navigate(RearrangeTabs) },
            )
        }

        composable<RearrangeTabs> {
            RearrangeTabsScreen(onBack = { navController.popBackStack() })
        }

        composable<RoomList> { backStack ->
            val route = backStack.toRoute<RoomList>()
            RoomListScreen(
                serverUrl = route.serverUrl,
                serverId = route.serverId,
                onRoomClick = { roomId ->
                    navController.navigate(RoomDetail(route.serverId, route.serverUrl, roomId))
                },
                onServers = {
                    navController.navigate(ServerList) { launchSingleTop = true }
                },
                onBack = null,
            )
        }

        composable<RoomDetail> { backStack ->
            val route = backStack.toRoute<RoomDetail>()
            RoomDetailScreen(
                serverUrl = route.serverUrl,
                serverId = route.serverId,
                roomId = route.roomId,
                onBack = { navController.popBackStack() },
                onMedia = {
                    navController.navigate(
                        MediaList(route.serverId, route.serverUrl, filterRoomId = route.roomId)
                    )
                },
            )
        }

        composable<ServerDashboard> { backStack ->
            val route = backStack.toRoute<ServerDashboard>()
            ServerDashboardScreen(
                serverId = route.serverId,
                serverUrl = route.serverUrl,
                onServers = {
                    navController.navigate(ServerList) { launchSingleTop = true }
                },
                onBack = null,
                onUsersClick = {
                    navController.navigate(UserList(route.serverId, route.serverUrl)) {
                        launchSingleTop = true
                        popUpTo<Login> { inclusive = false }
                    }
                },
                onRoomsClick = {
                    navController.navigate(RoomList(route.serverId, route.serverUrl)) {
                        launchSingleTop = true
                        popUpTo<Login> { inclusive = false }
                    }
                },
                onRoomClick = { roomId ->
                    navController.navigate(RoomDetail(route.serverId, route.serverUrl, roomId))
                },
                onOpenReportsClick = {
                    navController.navigate(EventReportsList(route.serverId, route.serverUrl))
                },
                onTopMediaUserClick = { userId ->
                    navController.navigate(
                        MediaList(route.serverId, route.serverUrl, filterUserId = userId),
                    )
                },
            )
        }

        composable<MediaList> { backStack ->
            val route = backStack.toRoute<MediaList>()
            MediaListScreen(
                serverUrl = route.serverUrl,
                serverId = route.serverId,
                filterUserId = route.filterUserId,
                filterRoomId = route.filterRoomId,
                onMediaClick = { serverName, mediaId ->
                    navController.navigate(MediaDetail(route.serverId, route.serverUrl, serverName, mediaId))
                },
                onServers = {
                    navController.navigate(ServerList) { launchSingleTop = true }
                },
                onBack = null,
            )
        }

        composable<MediaDetail> { backStack ->
            val route = backStack.toRoute<MediaDetail>()
            MediaDetailScreen(
                serverUrl = route.serverUrl,
                serverId = route.serverId,
                serverName = route.serverName,
                mediaId = route.mediaId,
                onBack = { navController.popBackStack() },
            )
        }

        composable<Settings> {
            AppLockSettingsScreen(
                onRearrangeTabs = { navController.navigate(RearrangeTabs) },
            )
        }

        composable<More> { backStack ->
            val route = backStack.toRoute<More>()
            MoreScreen(
                serverId = route.serverId,
                serverUrl = route.serverUrl,
                moreItemsInOrder = tabOrder.drop(4),
                onUsers = {
                    navController.navigate(UserList(route.serverId, route.serverUrl)) {
                        popUpTo<Login> { inclusive = false }
                        launchSingleTop = true
                    }
                },
                onRooms = {
                    navController.navigate(RoomList(route.serverId, route.serverUrl)) {
                        popUpTo<Login> { inclusive = false }
                        launchSingleTop = true
                    }
                },
                onStats = {
                    navController.navigate(ServerDashboard(route.serverId, route.serverUrl)) {
                        popUpTo<Login> { inclusive = false }
                        launchSingleTop = true
                    }
                },
                onSettings = {
                    navController.navigate(Settings(route.serverId, route.serverUrl)) {
                        popUpTo<Login> { inclusive = false }
                        launchSingleTop = true
                    }
                },
                onFederation = {
                    navController.navigate(FederationList(route.serverId, route.serverUrl))
                },
                onBackgroundJobs = {
                    navController.navigate(BackgroundJobs(route.serverId, route.serverUrl))
                },
                onEventReports = {
                    navController.navigate(EventReportsList(route.serverId, route.serverUrl))
                },
                onServers = {
                    navController.navigate(ServerList) { launchSingleTop = true }
                },
            )
        }

        composable<FederationList> { backStack ->
            val route = backStack.toRoute<FederationList>()
            FederationListScreen(
                serverUrl = route.serverUrl,
                serverId = route.serverId,
                onDestinationClick = { destination ->
                    navController.navigate(FederationDetail(route.serverId, route.serverUrl, destination))
                },
                onServers = {
                    navController.navigate(ServerList) { launchSingleTop = true }
                },
                onBack = { navController.popBackStack() },
            )
        }

        composable<FederationDetail> { backStack ->
            val route = backStack.toRoute<FederationDetail>()
            FederationDetailScreen(
                serverUrl = route.serverUrl,
                serverId = route.serverId,
                destination = route.destination,
                onRoomClick = { roomId ->
                    navController.navigate(RoomDetail(route.serverId, route.serverUrl, roomId))
                },
                onBack = { navController.popBackStack() },
            )
        }

        composable<BackgroundJobs> { backStack ->
            val route = backStack.toRoute<BackgroundJobs>()
            BackgroundJobsScreen(
                serverId = route.serverId,
                serverUrl = route.serverUrl,
                onBack = { navController.popBackStack() },
            )
        }

        composable<EventReportsList> { backStack ->
            val route = backStack.toRoute<EventReportsList>()
            EventReportsScreen(
                serverId = route.serverId,
                serverUrl = route.serverUrl,
                onReportClick = { reportId ->
                    navController.navigate(EventReportDetail(route.serverId, route.serverUrl, reportId))
                },
                onBack = { navController.popBackStack() },
            )
        }

        composable<EventReportDetail> { backStack ->
            val route = backStack.toRoute<EventReportDetail>()
            EventReportDetailScreen(
                serverId = route.serverId,
                serverUrl = route.serverUrl,
                reportId = route.reportId,
                onBack = { navController.popBackStack() },
            )
        }
    }
}
}
