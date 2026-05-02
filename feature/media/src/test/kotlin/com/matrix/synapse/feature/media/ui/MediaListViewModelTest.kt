package com.matrix.synapse.feature.media.ui

import app.cash.turbine.test
import com.matrix.synapse.database.AuditLogger
import com.matrix.synapse.feature.media.data.MediaRepository
import com.matrix.synapse.feature.media.data.RoomMediaResponse
import com.matrix.synapse.feature.rooms.data.RoomListResponse
import com.matrix.synapse.feature.rooms.data.RoomRepository
import com.matrix.synapse.feature.servers.data.ServerRepository
import com.matrix.synapse.feature.users.data.UserRepository
import io.mockk.*
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MediaListViewModelTest {
    private val mediaRepository = mockk<MediaRepository>()
    private val roomRepository = mockk<RoomRepository>()
    private val userRepository = mockk<UserRepository>()
    private val auditLogger = mockk<AuditLogger>(relaxed = true)
    private val serverRepository = mockk<ServerRepository>()
    private val dispatcher = UnconfinedTestDispatcher()

    private fun createVm(): MediaListViewModel {
        coEvery { roomRepository.listRooms(any(), any(), any(), any(), any()) } returns RoomListResponse(rooms = emptyList())
        coEvery { userRepository.listUsersForMediaFilters(any(), any()) } returns emptyList()
        coEvery { userRepository.resolveLocalServerNameForMediaAdmin(any()) } returns "example.com"
        every { serverRepository.getServerById(any()) } returns flowOf(null)
        return MediaListViewModel(mediaRepository, roomRepository, userRepository, auditLogger, serverRepository)
    }

    @Before fun setup() { Dispatchers.setMain(dispatcher) }
    @After fun tearDown() { Dispatchers.resetMain() }

    @Test
    fun `loadRoomMedia populates media items`() = runTest {
        coEvery { mediaRepository.listRoomMedia(any(), any()) } returns RoomMediaResponse(
            local = listOf("mxc://example.com/abc123", "mxc://example.com/def456"),
            remote = listOf("mxc://matrix.org/ghi789"),
        )
        val vm = createVm()
        vm.state.test {
            vm.init("https://example.com", "srv1", filterUserId = null, filterRoomId = "!room:example.com")
            val state = expectMostRecentItem()
            assertFalse(state.isLoading)
            assertEquals(3, state.mediaItems.size)
            assertEquals("example.com", state.mediaItems[0].origin)
            assertEquals("abc123", state.mediaItems[0].mediaId)
            assertEquals("matrix.org", state.mediaItems[2].origin)
            assertEquals("ghi789", state.mediaItems[2].mediaId)
            assertTrue(state.mediaItems[0].isLocal)
            assertFalse(state.mediaItems[2].isLocal)
        }
    }

    @Test
    fun `loadRoomMedia sets error on failure`() = runTest {
        coEvery { mediaRepository.listRoomMedia(any(), any()) } throws RuntimeException("network error")
        val vm = createVm()
        vm.state.test {
            vm.init("https://example.com", "srv1", null, "!room:x")
            val state = expectMostRecentItem()
            assertNotNull(state.error)
            assertFalse(state.isLoading)
        }
    }
}
