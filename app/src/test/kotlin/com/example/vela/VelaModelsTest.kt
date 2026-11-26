package com.example.vela

import com.example.vela.model.PlatformType
import com.example.vela.model.formatBytes
import com.example.vela.model.formatDuration
import org.junit.Assert.assertEquals
import org.junit.Test

class VelaModelsTest {
    @Test
    fun platformDetection_identifiesSupportedSites() {
        assertEquals(PlatformType.TIKTOK, PlatformType.detect("https://www.tiktok.com/@lavendergadget/video/7422251697185033488"))
        assertEquals(PlatformType.YOUTUBE, PlatformType.detect("https://youtu.be/dQw4w9WgXcQ"))
        assertEquals(PlatformType.INSTAGRAM, PlatformType.detect("https://www.instagram.com/reel/ABC123/"))
        assertEquals(PlatformType.X_TWITTER, PlatformType.detect("https://x.com/user/status/123"))
        assertEquals(PlatformType.GENERIC, PlatformType.detect("https://example.com/video.mp4"))
    }

    @Test
    fun formatDuration_formatsMinutesAndHours() {
        assertEquals("--:--", formatDuration(0))
        assertEquals("01:05", formatDuration(65))
        assertEquals("1:01:05", formatDuration(3665))
    }

    @Test
    fun formatBytes_formatsReadableSizes() {
        assertEquals("0 B", formatBytes(0))
        assertEquals("512 B", formatBytes(512))
        assertEquals("2.0 MB", formatBytes(2L * 1024L * 1024L))
    }
}

