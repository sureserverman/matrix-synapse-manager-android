package com.matrix.synapse.model

/**
 * Parsed reference to a Matrix media object for admin API calls (`/{origin}/{media_id}`).
 *
 * Synapse room-media listings return `mxc://origin/mediaId` strings. User-media listing returns
 * bare `media_id` values for **local** media; those use [fallbackOrigin] (typically the
 * homeserver name derived from the admin base URL or Matrix user ID host).
 */
data class ParsedMediaReference(
    val origin: String,
    val mediaId: String,
    /** True when the input used the `mxc://` scheme (not a bare media id). */
    val hadMxcScheme: Boolean,
)

/**
 * Normalizes Synapse media identifiers for storage/API paths.
 *
 * Global last-access cleanup (`POST /_synapse/admin/v1/media/delete`) uses **last access** time.
 * User/room listing filters use **created** timestamps where the Admin API exposes them — do not
 * conflate the two when presenting date ranges in UI.
 */
object MatrixMediaMxcParser {

    /**
     * @param raw Value from Synapse (`mxc://…`, bare `media_id`, or optional https URL to a media download path)
     * @param fallbackOrigin Server name used when [raw] is not an MXC URI (e.g. `example.com`)
     */
    fun parse(raw: String, fallbackOrigin: String): ParsedMediaReference? {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return null

        if (trimmed.startsWith("mxc://", ignoreCase = true)) {
            val rest = trimmed.drop(6) // "mxc://".length
            val slash = rest.indexOf('/')
            if (slash <= 0 || slash >= rest.length - 1) return null
            val origin = rest.substring(0, slash)
            val mediaId = rest.substring(slash + 1)
            if (origin.isBlank() || mediaId.isBlank()) return null
            return ParsedMediaReference(
                origin = origin,
                mediaId = mediaId,
                hadMxcScheme = true,
            )
        }

        val httpsDerived = tryParseHttpDownloadPath(trimmed)
        if (httpsDerived != null) return httpsDerived

        if (fallbackOrigin.isBlank()) return null
        return ParsedMediaReference(
            origin = fallbackOrigin,
            mediaId = trimmed,
            hadMxcScheme = false,
        )
    }

    /**
     * Supports optional `https://host/.../download/.../mediaId` style paths used by some tools.
     */
    private fun tryParseHttpDownloadPath(raw: String): ParsedMediaReference? {
        if (!raw.startsWith("http://", ignoreCase = true) && !raw.startsWith("https://", ignoreCase = true)) {
            return null
        }
        return runCatching {
            val uri = java.net.URI(raw)
            val host = uri.host ?: return null
            val path = uri.path ?: return null
            val segments = path.trim('/').split('/').filter { it.isNotEmpty() }
            val mediaId = segments.lastOrNull() ?: return null
            if (host.isBlank() || mediaId.isBlank()) return null
            ParsedMediaReference(origin = host, mediaId = mediaId, hadMxcScheme = false)
        }.getOrNull()
    }
}
