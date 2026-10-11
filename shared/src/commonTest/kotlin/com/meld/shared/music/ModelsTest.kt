package com.meld.shared.music

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ModelsTest {
    @Test
    fun formatDuration_zero() {
        assertEquals("0:00", formatDuration(0))
    }

    @Test
    fun formatDuration_secondsOnly() {
        assertEquals("0:05", formatDuration(5))
        assertEquals("1:35", formatDuration(95))
    }

    @Test
    fun formatDuration_minutes() {
        assertEquals("3:34", formatDuration(214))
        assertEquals("59:59", formatDuration(3599))
    }

    @Test
    fun formatDuration_hours() {
        assertEquals("1:00:00", formatDuration(3600))
        assertEquals("1:02:01", formatDuration(3721))
    }

    @Test
    fun formatDuration_unknown() {
        assertEquals("--:--", formatDuration(-1))
        assertEquals("--:--", formatDuration(-100))
    }

    @Test
    fun track_artistNames_joinsMultiple() {
        val track =
            Track(
                id = "t",
                title = "Title",
                artists = listOf(ArtistRef("a1", "Alpha"), ArtistRef("a2", "Beta")),
            )
        assertEquals("Alpha, Beta", track.artistNames)
    }

    @Test
    fun track_artistNames_fallsBackWhenEmpty() {
        val track = Track(id = "t", title = "Title")
        assertEquals("Unknown artist", track.artistNames)
    }

    @Test
    fun track_formattedDuration_delegates() {
        assertEquals("3:34", Track(id = "t", title = "Title", durationSeconds = 214).formattedDuration)
        assertEquals("--:--", Track(id = "t", title = "Title").formattedDuration)
    }

    @Test
    fun models_areImmutableValues() {
        val track = Track(id = "t", title = "Title", durationSeconds = 60)
        assertEquals(track, track.copy())
        assertTrue(track.copy(title = "Other") != track)
    }
}
