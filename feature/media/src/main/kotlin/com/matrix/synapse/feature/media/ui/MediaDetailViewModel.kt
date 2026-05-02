package com.matrix.synapse.feature.media.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.matrix.synapse.database.AuditAction
import com.matrix.synapse.database.AuditLogEntry
import com.matrix.synapse.database.AuditLogger
import com.matrix.synapse.core.resources.R
import com.matrix.synapse.feature.media.data.MediaInfoResponse
import com.matrix.synapse.feature.media.data.MediaRepository
import com.matrix.synapse.feature.users.data.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MediaDetailState(
    val media: MediaInfoResponse? = null,
    val isLoading: Boolean = false,
    val isActioning: Boolean = false,
    val isDeleted: Boolean = false,
    val error: String? = null,
    val actionMessage: StringResMessage? = null,
)

@HiltViewModel
class MediaDetailViewModel @Inject constructor(
    private val mediaRepository: MediaRepository,
    private val auditLogger: AuditLogger,
    private val userRepository: UserRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(MediaDetailState())
    val state: StateFlow<MediaDetailState> = _state.asStateFlow()

    private var serverUrl: String = ""
    private var serverId: String = ""

    /** Cached for Synapse admin media paths (`server_name` in URLs). */
    private var resolvedLocalServerName: String? = null

    private suspend fun localServerName(): String {
        resolvedLocalServerName?.let { return it }
        val n = userRepository.resolveLocalServerNameForMediaAdmin(serverUrl)
        resolvedLocalServerName = n
        return n
    }

    fun loadMedia(serverUrl: String, serverId: String, serverName: String, mediaId: String) {
        this.serverUrl = serverUrl
        this.serverId = serverId
        resolvedLocalServerName = null
        _state.value = MediaDetailState(isLoading = true)
        viewModelScope.launch {
            runCatching { mediaRepository.getMediaInfo(serverUrl, localServerName(), mediaId) }
                .onSuccess { info -> _state.value = MediaDetailState(media = info) }
                .onFailure { e -> _state.value = MediaDetailState(error = e.message) }
        }
    }

    fun quarantine(serverName: String, mediaId: String) =
        performAction(AuditAction.QUARANTINE_MEDIA, mediaId, R.string.media_action_quarantined) {
            mediaRepository.quarantineMedia(serverUrl, localServerName(), mediaId)
        }

    fun unquarantine(serverName: String, mediaId: String) =
        performAction(AuditAction.UNQUARANTINE_MEDIA, mediaId, R.string.media_action_unquarantined) {
            mediaRepository.unquarantineMedia(serverUrl, localServerName(), mediaId)
        }

    fun protect(mediaId: String) =
        performAction(AuditAction.PROTECT_MEDIA, mediaId, R.string.media_action_protected) {
            mediaRepository.protectMedia(serverUrl, mediaId)
        }

    fun unprotect(mediaId: String) =
        performAction(AuditAction.UNPROTECT_MEDIA, mediaId, R.string.media_action_unprotected) {
            mediaRepository.unprotectMedia(serverUrl, mediaId)
        }

    /** Synapse DELETE /media/{serverName}/{mediaId} requires the configured Matrix server name, not the admin URL host. */
    fun delete(mediaId: String) {
        _state.value = _state.value.copy(isActioning = true, error = null, actionMessage = null)
        viewModelScope.launch {
            runCatching { mediaRepository.deleteMedia(serverUrl, localServerName(), mediaId) }
                .onSuccess {
                    _state.value = _state.value.copy(
                        isActioning = false,
                        isDeleted = true,
                        actionMessage = StringResMessage(R.string.media_action_deleted),
                    )
                    auditLogger.insert(AuditLogEntry(serverId = serverId, action = AuditAction.DELETE_MEDIA, details = mapOf("media_id" to mediaId)))
                }
                .onFailure { e -> _state.value = _state.value.copy(isActioning = false, error = e.message) }
        }
    }

    fun clearActionMessage() {
        _state.value = _state.value.copy(actionMessage = null)
    }

    private fun performAction(action: AuditAction, mediaId: String, messageResId: Int, block: suspend () -> Unit) {
        _state.value = _state.value.copy(isActioning = true, error = null, actionMessage = null)
        viewModelScope.launch {
            runCatching { block() }
                .onSuccess {
                    _state.value = _state.value.copy(
                        isActioning = false,
                        actionMessage = StringResMessage(messageResId),
                    )
                    auditLogger.insert(AuditLogEntry(serverId = serverId, action = action, details = mapOf("media_id" to mediaId)))
                    reloadMedia(mediaId)
                }
                .onFailure { e -> _state.value = _state.value.copy(isActioning = false, error = e.message) }
        }
    }

    private fun reloadMedia(mediaId: String) {
        viewModelScope.launch {
            runCatching { mediaRepository.getMediaInfo(serverUrl, localServerName(), mediaId) }
                .onSuccess { info -> _state.value = _state.value.copy(media = info) }
        }
    }
}
