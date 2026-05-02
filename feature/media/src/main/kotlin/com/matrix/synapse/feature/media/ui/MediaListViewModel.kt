package com.matrix.synapse.feature.media.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.matrix.synapse.database.AuditAction
import com.matrix.synapse.database.AuditLogEntry
import com.matrix.synapse.database.AuditLogger
import com.matrix.synapse.feature.media.data.MediaRepository
import com.matrix.synapse.feature.rooms.data.RoomRepository
import com.matrix.synapse.feature.rooms.data.RoomSummary
import com.matrix.synapse.feature.servers.data.ServerRepository
import com.matrix.synapse.feature.users.data.UserRepository
import com.matrix.synapse.feature.users.data.UserSummary
import com.matrix.synapse.model.MatrixMediaMxcParser
import com.matrix.synapse.model.Server
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MediaListItem(
    val mediaId: String,
    val origin: String,
    val isLocal: Boolean,
)

fun MediaListItem.stableKey(): String = "${origin}|${mediaId}"

data class MediaListState(
    val currentServer: Server? = null,
    val mediaItems: List<MediaListItem> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val actionMessage: String? = null,
    val filterMode: String = "room",
    val filterValue: String = "",
    val rooms: List<RoomSummary> = emptyList(),
    val users: List<UserSummary> = emptyList(),
    val roomsLoading: Boolean = false,
    val usersLoading: Boolean = false,
    val selectedRoomId: String? = null,
    val selectedUserId: String? = null,
    /** Created-upload timestamp filters for user-media listing and user-scoped bulk delete (Synapse admin API). */
    val userMediaFromTs: Long? = null,
    val userMediaUntilTs: Long? = null,
    val selectedKeys: Set<String> = emptySet(),
)

@HiltViewModel
class MediaListViewModel @Inject constructor(
    private val mediaRepository: MediaRepository,
    private val roomRepository: RoomRepository,
    private val userRepository: UserRepository,
    private val auditLogger: AuditLogger,
    private val serverRepository: ServerRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(MediaListState())
    val state: StateFlow<MediaListState> = _state.asStateFlow()

    private var serverUrl: String = ""
    private var serverId: String = ""

    fun init(serverUrl: String, serverId: String, filterUserId: String?, filterRoomId: String?) {
        this.serverUrl = serverUrl
        this.serverId = serverId
        serverRepository.getServerById(serverId).onEach { server ->
            _state.value = _state.value.copy(currentServer = server)
        }.launchIn(viewModelScope)
        loadRooms()
        loadUsers()
        when {
            filterRoomId != null -> {
                _state.value = _state.value.copy(
                    filterMode = "room",
                    filterValue = filterRoomId,
                    selectedRoomId = filterRoomId,
                    selectedUserId = null,
                )
                loadRoomMedia(filterRoomId)
            }
            filterUserId != null -> {
                _state.value = _state.value.copy(
                    filterMode = "user",
                    filterValue = filterUserId,
                    selectedRoomId = null,
                    selectedUserId = filterUserId,
                )
                loadUserMedia(filterUserId)
            }
            else -> {
                _state.value = _state.value.copy(filterMode = "room", filterValue = "")
            }
        }
    }

    fun loadRooms() {
        _state.value = _state.value.copy(roomsLoading = true)
        viewModelScope.launch {
            runCatching {
                roomRepository.listRooms(serverUrl, limit = 500)
            }.onSuccess { response ->
                _state.value = _state.value.copy(rooms = response.rooms, roomsLoading = false)
            }.onFailure {
                _state.value = _state.value.copy(roomsLoading = false)
            }
        }
    }

    fun loadUsers() {
        _state.value = _state.value.copy(usersLoading = true)
        viewModelScope.launch {
            runCatching {
                userRepository.listUsersForMediaFilters(serverUrl, limit = 500)
            }.onSuccess { users ->
                _state.value = _state.value.copy(users = users, usersLoading = false)
            }.onFailure {
                _state.value = _state.value.copy(usersLoading = false)
            }
        }
    }

    fun selectRoom(roomId: String?) {
        _state.value = _state.value.copy(
            selectedRoomId = roomId,
            selectedUserId = null,
            filterMode = "room",
            filterValue = roomId ?: "",
            selectedKeys = emptySet(),
        )
        if (roomId != null) loadRoomMedia(roomId)
        else _state.value = _state.value.copy(mediaItems = emptyList())
    }

    fun selectUser(userId: String?) {
        _state.value = _state.value.copy(
            selectedRoomId = null,
            selectedUserId = userId,
            filterMode = "user",
            filterValue = userId ?: "",
            selectedKeys = emptySet(),
        )
        if (userId != null) loadUserMedia(userId)
        else _state.value = _state.value.copy(mediaItems = emptyList())
    }

    fun setUserMediaDateRange(fromTs: Long?, untilTs: Long?) {
        _state.value = _state.value.copy(userMediaFromTs = fromTs, userMediaUntilTs = untilTs)
        val uid = _state.value.selectedUserId
        if (uid != null) loadUserMedia(uid)
    }

    fun toggleSelection(item: MediaListItem) {
        val key = item.stableKey()
        val next = _state.value.selectedKeys.toMutableSet()
        if (!next.add(key)) next.remove(key)
        _state.value = _state.value.copy(selectedKeys = next)
    }

    fun clearSelection() {
        _state.value = _state.value.copy(selectedKeys = emptySet())
    }

    fun deleteSelectedMedia() {
        val keys = _state.value.selectedKeys
        if (keys.isEmpty()) return
        val items = _state.value.mediaItems.filter { it.stableKey() in keys }
        _state.value = _state.value.copy(error = null, actionMessage = null)
        viewModelScope.launch {
            var ok = 0
            var failed = 0
            for (item in items) {
                val r = runCatching { mediaRepository.deleteMedia(serverUrl, item.origin, item.mediaId) }
                if (r.isSuccess) {
                    ok++
                    auditLogger.insert(
                        AuditLogEntry(serverId = serverId, action = AuditAction.DELETE_MEDIA, details = mapOf("media_id" to item.mediaId, "origin" to item.origin)),
                    )
                } else {
                    failed++
                }
            }
            val msg = buildString {
                append("Deleted $ok items")
                if (failed > 0) append(" ($failed failed)")
            }
            _state.value = _state.value.copy(actionMessage = msg, selectedKeys = emptySet())
            refresh()
        }
    }

    /**
     * Deletes all local media uploaded by the selected user matching optional created-ts bounds.
     */
    fun bulkDeleteUserScopedMedia() {
        val userId = _state.value.selectedUserId ?: return
        val fromTs = _state.value.userMediaFromTs
        val untilTs = _state.value.userMediaUntilTs
        if (fromTs != null && untilTs != null && fromTs > untilTs) {
            _state.value = _state.value.copy(error = "Invalid date range")
            return
        }
        _state.value = _state.value.copy(error = null, actionMessage = null)
        viewModelScope.launch {
            runCatching {
                var deletedTotal = 0
                do {
                    val batch = userRepository.deleteUserMediaBulk(
                        serverUrl = serverUrl,
                        userId = userId,
                        limit = 1000,
                        fromTs = fromTs,
                        untilTs = untilTs,
                    )
                    deletedTotal += batch.total
                    if (batch.total <= 0) break
                } while (true)
                deletedTotal
            }.onSuccess { total ->
                _state.value = _state.value.copy(actionMessage = "Deleted $total user media items")
                auditLogger.insert(
                    AuditLogEntry(
                        serverId = serverId,
                        action = AuditAction.BULK_DELETE_MEDIA,
                        details = mapOf(
                            "scope" to "user",
                            "user_id" to userId,
                            "from_ts" to (fromTs?.toString() ?: ""),
                            "until_ts" to (untilTs?.toString() ?: ""),
                            "deleted" to total.toString(),
                        ),
                    ),
                )
                refresh()
            }.onFailure { e ->
                _state.value = _state.value.copy(error = e.message)
            }
        }
    }

    /** Deletes discoverable media for the current room list (per MXC origin/id); remote-only rows use their origin from MXC. */
    fun bulkDeleteRoomScopedMedia() {
        val roomId = _state.value.selectedRoomId ?: return
        val items = _state.value.mediaItems
        if (items.isEmpty()) return
        _state.value = _state.value.copy(error = null, actionMessage = null)
        viewModelScope.launch {
            var ok = 0
            var failed = 0
            for (item in items) {
                val r = runCatching { mediaRepository.deleteMedia(serverUrl, item.origin, item.mediaId) }
                if (r.isSuccess) ok++ else failed++
            }
            auditLogger.insert(
                AuditLogEntry(
                    serverId = serverId,
                    action = AuditAction.BULK_DELETE_MEDIA,
                    details = mapOf(
                        "scope" to "room",
                        "room_id" to roomId,
                        "deleted" to ok.toString(),
                        "failed" to failed.toString(),
                    ),
                ),
            )
            val msg = buildString {
                append("Deleted $ok room media items")
                if (failed > 0) append(" ($failed failed)")
            }
            _state.value = _state.value.copy(actionMessage = msg)
            loadRoomMedia(roomId)
        }
    }

    fun refresh() {
        val s = _state.value
        when {
            s.filterMode == "room" && s.filterValue.isNotBlank() -> loadRoomMedia(s.filterValue)
            s.filterMode == "user" && s.filterValue.isNotBlank() -> loadUserMedia(s.filterValue)
            else -> {
                loadRooms()
                loadUsers()
            }
        }
    }

    fun loadRoomMedia(roomId: String) {
        _state.value = _state.value.copy(isLoading = true, error = null, filterMode = "room", filterValue = roomId)
        viewModelScope.launch {
            runCatching {
                val serverName = extractServerName(serverUrl)
                val response = mediaRepository.listRoomMedia(serverUrl, roomId)
                val localItems = response.local.mapNotNull { raw ->
                    val p = MatrixMediaMxcParser.parse(raw, serverName) ?: return@mapNotNull null
                    MediaListItem(mediaId = p.mediaId, origin = p.origin, isLocal = true)
                }
                val remoteItems = response.remote.mapNotNull { raw ->
                    val p = MatrixMediaMxcParser.parse(raw, serverName) ?: return@mapNotNull null
                    MediaListItem(mediaId = p.mediaId, origin = p.origin, isLocal = false)
                }
                localItems + remoteItems
            }.onSuccess { items ->
                _state.value = _state.value.copy(mediaItems = items, isLoading = false)
            }.onFailure { e ->
                _state.value = _state.value.copy(error = e.message, isLoading = false)
            }
        }
    }

    fun loadUserMedia(userId: String) {
        val fromTs = _state.value.userMediaFromTs
        val untilTs = _state.value.userMediaUntilTs
        _state.value = _state.value.copy(isLoading = true, error = null, filterMode = "user", filterValue = userId)
        viewModelScope.launch {
            runCatching {
                val serverName = extractServerName(serverUrl)
                val response = userRepository.listUserMedia(
                    serverUrl = serverUrl,
                    userId = userId,
                    fromTs = fromTs,
                    untilTs = untilTs,
                )
                response.media.mapNotNull { item ->
                    val p = MatrixMediaMxcParser.parse(item.mediaId, serverName) ?: return@mapNotNull null
                    MediaListItem(mediaId = p.mediaId, origin = p.origin, isLocal = true)
                }
            }.onSuccess { items ->
                _state.value = _state.value.copy(mediaItems = items, isLoading = false)
            }.onFailure { e ->
                _state.value = _state.value.copy(error = e.message, isLoading = false)
            }
        }
    }

    private fun extractServerName(serverUrl: String): String =
        serverUrl.removePrefix("https://").removePrefix("http://").trimEnd('/')
}
