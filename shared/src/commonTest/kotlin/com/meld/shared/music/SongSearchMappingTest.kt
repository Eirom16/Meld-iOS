package com.meld.shared.music

import com.meld.shared.innertube.models.Album
import com.meld.shared.innertube.models.Artist
import com.meld.shared.innertube.models.SongItem
import com.meld.shared.search.YouTubeMusicSearch
import com.meld.shared.search.SearchFixtures
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.url
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Conversión `SongItem` → `Track` y repositorio sobre búsqueda real
 * (Fase 02C, PASO G). Sin red real: `MockEngine` con forma mínima válida.
 */
@OptIn(ExperimentalSerializationApi::class)
class SongSearchMappingTest {
    private fun song(
        id: String = "vid1",
        title: String = "Title",
        artists: List<Artist> = listOf(Artist("Name", "UC1")),
        album: Album? = Album("Album", "MPRE1"),
        duration: Int? = 200,
        thumbnail: String = "https://img/x.jpg",
        explicit: Boolean = false,
    ) = SongItem(
        id = id,
        title = title,
        artists = artists,
        album = album,
        duration = duration,
        thumbnail = thumbnail,
        explicit = explicit,
    )

    @Test
    fun fullMapping_preservesEveryField() {
        val track = song().toTrack()
        assertEquals("vid1", track.id)
        assertEquals("Title", track.title)
        assertEquals(listOf("UC1"), track.artists.map { it.id })
        assertEquals(listOf("Name"), track.artists.map { it.name })
        assertEquals("MPRE1", track.album?.id)
        assertEquals("Album", track.album?.title)
        assertEquals(200, track.durationSeconds)
        assertEquals("3:20", track.formattedDuration)
        assertEquals("https://img/x.jpg", track.artworkUrl)
        assertEquals(false, track.explicit)
        assertEquals("Name", track.artistNames)
    }

    @Test
    fun missingAlbum_mapsToNull() {
        assertNull(song(album = null).toTrack().album)
    }

    @Test
    fun missingDuration_mapsToUnknown() {
        val track = song(duration = null).toTrack()
        assertEquals(-1, track.durationSeconds)
        assertEquals("--:--", track.formattedDuration)
    }

    @Test
    fun artistWithoutId_mapsToEmptyId() {
        val track = song(artists = listOf(Artist("Plain", null))).toTrack()
        assertEquals("", track.artists.single().id)
        assertEquals("Plain", track.artistNames)
    }

    @Test
    fun emptyArtists_fallsBackToUnknownArtist() {
        assertEquals("Unknown artist", song(artists = emptyList()).toTrack().artistNames)
    }

    @Test
    fun explicit_isPreserved() {
        assertEquals(true, song(explicit = true).toTrack().explicit)
    }

    private fun repoClient(body: String): HttpClient =
        HttpClient(MockEngine) {
            engine { addHandler { respond(body, headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())) } }
            expectSuccess = false
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true; explicitNulls = false; encodeDefaults = true })
            }
            defaultRequest { url("https://music.youtube.com/youtubei/v1/") }
        }

    private val oneSongResponse: String by lazy {
        val subtitle =
            listOf(
                SearchFixtures.run("Repo Artist", "UC9"),
                SearchFixtures.sep,
                SearchFixtures.run("4:00"),
            ).joinToString(",")
        SearchFixtures.searchResponse(
            SearchFixtures.shelf(
                SearchFixtures.songRenderer("vidR", "Repo Song", subtitle, thumb = "https://img/r.jpg"),
            ),
        )
    }


    @Test
    fun repository_returnsTracksFromLiveSearch() =
        runTest {
            val repo = YouTubeSongSearchRepository(YouTubeMusicSearch(repoClient(oneSongResponse)))
            val result = repo.searchSongs("repo")
            assertTrue(result.isSuccess)
            val tracks = result.getOrThrow()
            assertEquals(1, tracks.size)
            assertEquals("vidR", tracks.single().id)
            assertEquals("Repo Song", tracks.single().title)
            assertEquals("Repo Artist", tracks.single().artistNames)
            assertEquals(240, tracks.single().durationSeconds)
            assertNull(tracks.single().album)
            repo.toString()
        }

    @Test
    fun repository_propagatesFailuresWithoutFakeData() =
        runTest {
            val failing =
                HttpClient(MockEngine) {
                    engine { addHandler { respondError(HttpStatusCode.BadGateway) } }
                    expectSuccess = false
                    install(ContentNegotiation) {
                        json(Json { ignoreUnknownKeys = true; explicitNulls = false; encodeDefaults = true })
                    }
                    defaultRequest { url("https://music.youtube.com/youtubei/v1/") }
                }
            val repo = YouTubeSongSearchRepository(YouTubeMusicSearch(failing))
            val result = repo.searchSongs("repo")
            assertTrue(result.isFailure)
            assertTrue(result.getOrNull() == null)
        }
}
