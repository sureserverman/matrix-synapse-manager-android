package com.matrix.synapse.feature.stats.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.matrix.synapse.feature.federation.data.FederationRepository
import com.matrix.synapse.feature.jobs.data.JobsRepository
import com.matrix.synapse.feature.moderation.data.ModerationRepository
import com.matrix.synapse.feature.rooms.data.RoomRepository
import com.matrix.synapse.feature.servers.data.ServerRepository
import com.matrix.synapse.feature.stats.data.*
import com.matrix.synapse.model.Server
import com.matrix.synapse.network.ActiveTokenHolder
import com.matrix.synapse.security.SecureTokenStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Largest room row: id, size, and optional name/avatar from room detail. */
data class LargestRoomDisplay(
    val roomId: String,
    val estimatedSize: Long,
    val name: String? = null,
    val avatarUrl: String? = null,
)

data class DashboardState(
    val currentServer: Server? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val serverVersion: String? = null,
    val totalUsers: Long = 0L,
    val totalRooms: Int = 0,
    val dau: Int = 0,
    val mau: Int = 0,
    val totalMediaBytes: Long? = null,
    val federationDestinations: Int? = null,
    val federationFailures: Int? = null,
    val backgroundUpdatesEnabled: Boolean? = null,
    val backgroundUpdatesJobName: String? = null,
    val openEventReportsCount: Int? = null,
    val largestRooms: List<RoomSizeEntry> = emptyList(),
    val largestRoomsDisplay: List<LargestRoomDisplay> = emptyList(),
    val dbStatsUnavailable: Boolean = false,
    val topMediaUsers: List<UserMediaStats> = emptyList(),
)

private fun mxcToDownloadUrl(serverBaseUrl: String, mxc: String?): String? {
    if (mxc.isNullOrBlank() || !mxc.startsWith("mxc://")) return null
    val rest = mxc.removePrefix("mxc://")
    val parts = rest.split("/", limit = 2)
    if (parts.size != 2) return null
    val (serverName, mediaId) = parts
    val base = serverBaseUrl.trimEnd('/')
    return "$base/_matrix/media/r0/download/$serverName/$mediaId"
}

@HiltViewModel
class ServerDashboardViewModel @Inject constructor(
    private val statsRepository: StatsRepository,
    private val serverRepository: ServerRepository,
    private val federationRepository: FederationRepository,
    private val jobsRepository: JobsRepository,
    private val moderationRepository: ModerationRepository,
    private val roomRepository: RoomRepository,
    private val tokenStore: SecureTokenStore,
    private val activeTokenHolder: ActiveTokenHolder,
) : ViewModel() {

    private val _state = MutableStateFlow(DashboardState())
    val state: StateFlow<DashboardState> = _state.asStateFlow()

    fun loadDashboard(serverId: String, serverUrl: String) {
        serverRepository.getServerById(serverId).onEach { server ->
            _state.value = _state.value.copy(currentServer = server)
        }.launchIn(viewModelScope)
        _state.value = _state.value.copy(isLoading = true)
        viewModelScope.launch {
            activeTokenHolder.set(tokenStore.accessTokenFlow(serverId).first())
            // Every async is individually runCatching-wrapped so a 403 (or any
            // failure) on one endpoint does NOT cancel the siblings via structured
            // concurrency. Without this, a non-admin token causes the dashboard to
            // crash mid-load instead of degrading gracefully into "unknown" cells.
            runCatching {
                val versionDef = async { runCatching { statsRepository.getServerVersion(serverUrl) }.getOrNull() }
                val totalUsersDef = async { runCatching { statsRepository.getTotalUsers(serverUrl) }.getOrNull() }
                val totalRoomsDef = async { runCatching { statsRepository.getTotalRooms(serverUrl) }.getOrNull() }
                val dauDef = async { runCatching { statsRepository.getActiveUserCount(serverUrl, 24 * 60 * 60 * 1000L) }.getOrNull() }
                val mauDef = async { runCatching { statsRepository.getActiveUserCount(serverUrl, 30L * 24 * 60 * 60 * 1000L) }.getOrNull() }
                val dbStatsDef = async {
                    runCatching { statsRepository.getDatabaseRoomStats(serverUrl) }
                }
                val mediaDef = async {
                    runCatching {
                        statsRepository.getMediaUsage(serverUrl, limit = 50, orderBy = "media_length", dir = "b")
                    }.getOrNull()
                }
                val totalMediaDef = async {
                    runCatching { statsRepository.getTotalMediaStorage(serverUrl) }.getOrNull()
                }
                val federationDef = async {
                    runCatching {
                        val first = federationRepository.listDestinations(serverUrl, limit = 1)
                        val withFailures = federationRepository.listDestinations(serverUrl, limit = 500)
                        Pair(first.total, withFailures.destinations.count { it.failureTs != null })
                    }.getOrNull()
                }
                val jobsDef = async {
                    runCatching { jobsRepository.getStatus(serverUrl) }.getOrNull()
                }
                val reportsDef = async {
                    runCatching { moderationRepository.listEventReports(serverUrl, limit = 1) }.getOrNull()?.total
                }

                val version = versionDef.await()
                val totalUsers = totalUsersDef.await()
                val totalRooms = totalRoomsDef.await()
                val dau = dauDef.await()
                val mau = mauDef.await()
                val dbStats = dbStatsDef.await()
                val media = mediaDef.await()
                val totalMedia = totalMediaDef.await()
                val (federationTotal, federationFailing) = federationDef.await() ?: (null to null)
                val jobsStatus = jobsDef.await()
                val reportsTotal = reportsDef.await()

                val rooms = dbStats.getOrNull()?.rooms ?: emptyList()
                val displayList = rooms.take(20).map { r ->
                    LargestRoomDisplay(roomId = r.roomId, estimatedSize = r.estimatedSize)
                }
                // If every gated call failed, surface "not authorized" once at the top
                // instead of leaving the user with a screen full of em-dashes.
                val allAdminCallsFailed = version == null && totalUsers == null && totalRooms == null
                _state.value = _state.value.copy(
                    serverVersion = version?.serverVersion,
                    totalUsers = totalUsers ?: 0L,
                    totalRooms = totalRooms ?: 0,
                    dau = dau ?: 0,
                    mau = mau ?: 0,
                    totalMediaBytes = totalMedia,
                    federationDestinations = federationTotal,
                    federationFailures = federationFailing,
                    backgroundUpdatesEnabled = jobsStatus?.enabled,
                    backgroundUpdatesJobName = jobsStatus?.currentUpdates?.entries?.firstOrNull()?.value?.name,
                    openEventReportsCount = reportsTotal,
                    largestRooms = rooms,
                    largestRoomsDisplay = displayList,
                    dbStatsUnavailable = dbStats.isFailure,
                    topMediaUsers = media?.users ?: emptyList(),
                    isLoading = false,
                    error = if (allAdminCallsFailed) "Not authorized — your account is not a Synapse server admin on this homeserver." else null,
                )
                // Load room name + avatar for largest rooms (best-effort)
                if (displayList.isNotEmpty()) {
                    loadLargestRoomDetails(serverUrl, displayList)
                }
            }.onFailure { e ->
                _state.value = _state.value.copy(error = e.message, isLoading = false)
            }
        }
    }

    private fun loadLargestRoomDetails(serverUrl: String, list: List<LargestRoomDisplay>) {
        viewModelScope.launch {
            val updated = coroutineScope {
                list.map { room ->
                    async {
                        runCatching {
                            roomRepository.getRoom(serverUrl, room.roomId)
                        }.getOrNull()?.let { d ->
                            room.copy(
                                name = d.name,
                                avatarUrl = mxcToDownloadUrl(serverUrl, d.avatar),
                            )
                        } ?: room
                    }
                }.awaitAll()
            }
            _state.value = _state.value.copy(largestRoomsDisplay = updated)
        }
    }
}
