package com.meld.shared.music

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RepositoryTest {
    @Test
    fun demo_listsAreNonEmpty() =
        runTest {
            val repo = DemoCatalog.repository()
            val snapshot = repo.snapshot()
            assertTrue(snapshot.tracks.isNotEmpty())
            assertTrue(snapshot.albums.isNotEmpty())
            assertTrue(snapshot.artists.isNotEmpty())
            assertTrue(snapshot.playlists.isNotEmpty())
        }

    @Test
    fun demo_trackDetail_resolves() =
        runTest {
            val repo = DemoCatalog.repository()
            val track = repo.track("demo-track-1")
            assertNotNull(track)
            assertEquals("Demo Sunrise", track.title)
            assertEquals("Demo Artist One", track.artistNames)
            assertEquals("demo-album-1", track.album?.id)
        }

    @Test
    fun demo_unknownIds_returnNull() =
        runTest {
            val repo = DemoCatalog.repository()
            assertNull(repo.track("no-such-track"))
            assertNull(repo.album("no-such-album"))
            assertNull(repo.artist("no-such-artist"))
            assertNull(repo.playlist("no-such-playlist"))
        }

    @Test
    fun demo_unknownRelations_returnEmptyLists() =
        runTest {
            val repo = DemoCatalog.repository()
            assertTrue(repo.playlistTracks("no-such-playlist").isEmpty())
            assertTrue(repo.albumTracks("no-such-album").isEmpty())
            assertTrue(repo.artistTracks("no-such-artist").isEmpty())
        }

    @Test
    fun demo_albumTracks_resolveAlbumMembers() =
        runTest {
            val repo = DemoCatalog.repository()
            val tracks = repo.albumTracks("demo-album-1")
            assertEquals(setOf("demo-track-1", "demo-track-2", "demo-track-5"), tracks.map { it.id }.toSet())
        }

    @Test
    fun demo_artistTracks_resolveCollaborations() =
        runTest {
            val repo = DemoCatalog.repository()
            val ensemble = repo.artistTracks("demo-artist-3")
            assertEquals(setOf("demo-track-4", "demo-track-6"), ensemble.map { it.id }.toSet())
            val one = repo.artistTracks("demo-artist-1")
            assertEquals(setOf("demo-track-1", "demo-track-2", "demo-track-5"), one.map { it.id }.toSet())
        }

    @Test
    fun demo_playlistTracks_skipMissingIds() =
        runTest {
            val repo = DemoCatalog.repository()
            val tracks = repo.playlistTracks("demo-playlist-2")
            assertEquals(listOf("demo-track-4"), tracks.map { it.id })
        }

    @Test
    fun emptyRepository_returnsEmptyListsAndNulls() =
        runTest {
            val repo = DemoCatalog.emptyRepository()
            val snapshot = repo.snapshot()
            assertTrue(snapshot.tracks.isEmpty())
            assertTrue(snapshot.albums.isEmpty())
            assertTrue(snapshot.artists.isEmpty())
            assertTrue(snapshot.playlists.isEmpty())
            assertNull(repo.track("demo-track-1"))
            assertTrue(repo.playlistTracks("demo-playlist-1").isEmpty())
        }

    @Test
    fun failingRepository_throwsOnEveryCall() =
        runTest {
            val repo = DemoCatalog.failingRepository()
            assertFailsWith<DemoCatalogException> { repo.tracks() }
            assertFailsWith<DemoCatalogException> { repo.track("demo-track-1") }
            assertFailsWith<DemoCatalogException> { repo.albums() }
            assertFailsWith<DemoCatalogException> { repo.artists() }
            assertFailsWith<DemoCatalogException> { repo.playlists() }
            assertFailsWith<DemoCatalogException> { repo.playlistTracks("demo-playlist-1") }
        }
}
