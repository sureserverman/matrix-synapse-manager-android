package com.matrix.synapse.feature.users.domain

import com.matrix.synapse.feature.users.data.UserRepository
import javax.inject.Inject

class DeactivateUserUseCase @Inject constructor(
    private val userRepository: UserRepository,
) {
    /**
     * Deactivates a Synapse user.
     *
     * When [deleteMedia] is true, user media is removed with
     * `DELETE /_synapse/admin/v1/users/{userId}/media` (batched) before deactivation with `erase=true`.
     * Failures from that bulk-delete phase are swallowed so deactivation can still proceed.
     *
     * [confirmed] must be `true`; callers are responsible for obtaining typed confirmation
     * from the administrator before calling this function.
     */
    suspend fun deactivate(
        serverUrl: String,
        userId: String,
        deleteMedia: Boolean,
        confirmed: Boolean,
    ): Result<Unit> = runCatching {
        require(confirmed) { "Deactivation requires explicit administrator confirmation" }

        if (deleteMedia) {
            // Bulk-delete via DELETE …/users/{id}/media (same filters as listing). Loop until a batch deletes nothing.
            runCatching {
                do {
                    val batch = userRepository.deleteUserMediaBulk(
                        serverUrl = serverUrl,
                        userId = userId,
                        limit = 1000,
                    )
                    if (batch.total <= 0) break
                } while (true)
            }
        }

        userRepository.deactivateUser(serverUrl, userId, erase = deleteMedia)
    }
}
