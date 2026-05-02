package com.matrix.synapse.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MatrixMediaMxcParserTest {

    @Test
    fun local_mxc_parses_origin_and_id() {
        val p = MatrixMediaMxcParser.parse("mxc://my.matrix.org/AbCdEf123", "unused.example")!!
        assertEquals("my.matrix.org", p.origin)
        assertEquals("AbCdEf123", p.mediaId)
        assertEquals(true, p.hadMxcScheme)
    }

    @Test
    fun remote_mxc_preserves_foreign_origin() {
        val p = MatrixMediaMxcParser.parse("mxc://matrix.org/xyz", "localhost")!!
        assertEquals("matrix.org", p.origin)
        assertEquals("xyz", p.mediaId)
        assertEquals(true, p.hadMxcScheme)
    }

    @Test
    fun bare_media_id_uses_fallback_origin() {
        val p = MatrixMediaMxcParser.parse("bareMediaIdOnly", "example.com")!!
        assertEquals("example.com", p.origin)
        assertEquals("bareMediaIdOnly", p.mediaId)
        assertEquals(false, p.hadMxcScheme)
    }

    @Test
    fun malformed_mxc_returns_null() {
        assertNull(MatrixMediaMxcParser.parse("mxc://onlyhost", "example.com"))
        assertNull(MatrixMediaMxcParser.parse("mxc:///nohost", "example.com"))
        assertNull(MatrixMediaMxcParser.parse("mxc://", "example.com"))
    }

    @Test
    fun https_download_url_derives_origin_from_host() {
        val p = MatrixMediaMxcParser.parse(
            "https://media.example.com/_matrix/media/v3/download/media.example.com/abc123",
            "ignored",
        )!!
        assertEquals("media.example.com", p.origin)
        assertEquals("abc123", p.mediaId)
        assertEquals(false, p.hadMxcScheme)
    }
}
