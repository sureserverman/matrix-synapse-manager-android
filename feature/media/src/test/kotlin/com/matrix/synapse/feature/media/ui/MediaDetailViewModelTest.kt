package com.matrix.synapse.feature.media.ui

import app.cash.turbine.test
import com.matrix.synapse.database.AuditAction
import com.matrix.synapse.database.AuditLogger
import com.matrix.synapse.feature.media.data.*
import com.matrix.synapse.feature.users.data.UserRepository
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MediaDetailViewModelTest {
    private val mediaRepository = mockk<MediaRepository>()
    private val auditLogger = mockk<AuditLogger>(relaxed = true)
    private val userRepository = mockk<UserRepository>()
    private val dispatcher = UnconfinedTestDispatcher()

    private fun createVm(): MediaDetailViewModel {
        coEvery { userRepository.resolveLocalServerNameForMediaAdmin(any()) } returns "example.com"
        return MediaDetailViewModel(mediaRepository, auditLogger, userRepository)
    }

    @Before fun setup() { Dispatchers.setMain(dispatcher) }
    @After fun tearDown() { Dispatchers.resetMain() }

    @Test
    fun `loadMedia populates detail`() = runTest {
        coEvery { mediaRepository.getMediaInfo(any(), any(), any()) } returns MediaInfoResponse(
            mediaId = "abc123", mediaType = "image/png", mediaLength = 1024,
        )
        val vm = createVm()
        vm.state.test {
            vm.loadMedia("https://example.com", "srv1", "example.com", "abc123")
            val state = expectMostRecentItem()
            assertEquals("abc123", state.media?.mediaId)
            assertEquals("image/png", state.media?.mediaType)
        }
    }

    @Test
    fun `quarantine logs audit`() = runTest {
        coEvery { mediaRepository.getMediaInfo(any(), any(), any()) } returns MediaInfoResponse(mediaId = "abc123")
        coEvery { mediaRepository.quarantineMedia(any(), any(), any()) } returns Unit
        val vm = createVm()
        vm.loadMedia("https://example.com", "srv1", "example.com", "abc123")
        vm.state.test {
            vm.quarantine("example.com", "abc123")
            expectMostRecentItem()
            coVerify { auditLogger.insert(match { it.action == AuditAction.QUARANTINE_MEDIA }) }
        }
    }

    @Test
    fun `delete sets isDeleted and logs audit`() = runTest {
        coEvery { mediaRepository.getMediaInfo(any(), any(), any()) } returns MediaInfoResponse(mediaId = "abc123")
        coEvery { mediaRepository.deleteMedia(any(), any(), any()) } returns DeleteMediaResponse(total = 1)
        val vm = createVm()
        vm.loadMedia("https://example.com", "srv1", "example.com", "abc123")
        vm.state.test {
            vm.delete("abc123")
            val state = expectMostRecentItem()
            assertTrue(state.isDeleted)
            coVerify { mediaRepository.deleteMedia("https://example.com", "example.com", "abc123") }
            coVerify { auditLogger.insert(match { it.action == AuditAction.DELETE_MEDIA }) }
        }
    }

    @Test
    fun `loadMedia sets error on failure`() = runTest {
        coEvery { mediaRepository.getMediaInfo(any(), any(), any()) } throws RuntimeException("not found")
        val vm = createVm()
        vm.state.test {
            vm.loadMedia("https://example.com", "srv1", "example.com", "abc123")
            val state = expectMostRecentItem()
            assertNotNull(state.error)
        }
    }
}
