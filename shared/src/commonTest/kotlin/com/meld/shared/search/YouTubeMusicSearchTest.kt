package com.meld.shared.search

import com.meld.shared.innertube.models.SongItem
import com.meld.shared.network.createYouTubeHttpClient
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.request.HttpResponseData
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.url
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Pruebas deterministas de la primera búsqueda real (Fase 02C, PASO G).
 *
 * `MockEngine` evita depender de YouTube Music en CI; la prueba en iPhone
 * del propietario es la que valida la conectividad real extremo a extremo.
 */
@OptIn(ExperimentalSerializationApi::class)
class YouTubeMusicSearchTest {
    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        encodeDefaults = true
    }

    private val jsonHeaders = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())

    private fun mockClient(
        requestTimeoutMillis: Long = 60_000,
        handler: suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData,
    ): HttpClient =
        HttpClient(MockEngine) {
            engine { addHandler(handler) }
            expectSuccess = false
            install(ContentNegotiation) { json(this@YouTubeMusicSearchTest.json) }
            install(HttpTimeout) {
                this.requestTimeoutMillis = requestTimeoutMillis
                connectTimeoutMillis = 30_000
                socketTimeoutMillis = 60_000
            }
            defaultRequest { url("https://music.youtube.com/youtubei/v1/") }
        }

    // --- Tests ---

    @Test
    fun clientConstruction_doesNotThrow() {
        val client = createYouTubeHttpClient()
        assertNotNull(client)
        client.close()
    }

    @Test
    fun blankQuery_returnsEmptyWithoutNetwork() =
        runTest {
            var calls = 0
            val search = YouTubeMusicSearch(mockClient { calls++; respond("{}") })
            val result = search.search("   ")
            assertTrue(result.isSuccess)
            assertEquals(emptyList(), result.getOrThrow().items)
            assertEquals(0, calls)
            search.close()
        }

    @Test
    fun parsesSongs_withFullAndMinimalMetadata() =
        runTest {
            val body =
                SearchFixtures.searchResponse(
                    SearchFixtures.shelf(
                        SearchFixtures.songRenderer("vidFull", "Full Song", SearchFixtures.fullSongSubtitle, explicit = true),
                        SearchFixtures.songRenderer("vidMin", "Minimal Song", SearchFixtures.minimalSongSubtitle),
                    ),
                )
            val search = YouTubeMusicSearch(mockClient { respond(body, headers = jsonHeaders) })
            val page = search.search("demo").getOrThrow()
            assertEquals(2, page.items.size)
            assertNull(page.continuation)

            val full = page.items.filterIsInstance<SongItem>().first { it.id == "vidFull" }
            assertEquals("Full Song", full.title)
            assertEquals(listOf("Full Artist"), full.artists.map { it.name })
            assertEquals("UCartist1", full.artists.first().id)
            assertEquals("Full Album", full.album?.name)
            assertEquals("MPREalbum1", full.album?.id)
            assertEquals(225, full.duration)
            assertEquals("https://img/song.jpg", full.thumbnail)
            assertTrue(full.explicit)

            val minimal = page.items.filterIsInstance<SongItem>().first { it.id == "vidMin" }
            assertNull(minimal.album)
            assertEquals(127, minimal.duration)
            search.close()
        }

    @Test
    fun skipsUnparseableItems_withoutFailing() =
        runTest {
            val broken =
                """{"badges":null,"fixedColumns":null,"flexColumns":[${SearchFixtures.flexColumn(SearchFixtures.run("Lonely title"))}],"thumbnail":null,"menu":null,"playlistItemData":null,"overlay":null,"navigationEndpoint":null}"""
            val body =
                SearchFixtures.searchResponse(
                    SearchFixtures.shelf(SearchFixtures.songRenderer("vid1", "Good Song", SearchFixtures.minimalSongSubtitle), broken),
                )
            val search = YouTubeMusicSearch(mockClient { respond(body, headers = jsonHeaders) })
            val page = search.search("demo").getOrThrow()
            assertEquals(1, page.items.size)
            assertEquals("vid1", page.items.first().id)
            search.close()
        }

    @Test
    fun emptyShelf_returnsEmptyList() =
        runTest {
            val body = SearchFixtures.searchResponse(SearchFixtures.shelf())
            val search = YouTubeMusicSearch(mockClient { respond(body, headers = jsonHeaders) })
            val page = search.search("demo").getOrThrow()
            assertEquals(emptyList(), page.items)
            search.close()
        }

    @Test
    fun httpError_returnsFailure() =
        runTest {
            val search = YouTubeMusicSearch(mockClient { respondError(HttpStatusCode.BadRequest) })
            val result = search.search("demo")
            assertTrue(result.isFailure)
            search.close()
        }

    @Test
    fun malformedJson_returnsFailure() =
        runTest {
            val search = YouTubeMusicSearch(mockClient { respond("this is not json", headers = jsonHeaders) })
            assertTrue(search.search("demo").isFailure)
            search.close()
        }

    @Test
    fun wrongShape_returnsFailure() =
        runTest {
            val search =
                YouTubeMusicSearch(
                    mockClient {
                        respond(
                            """{"contents":{"tabbedSearchResultsRenderer":{"tabs":"oops"}}}""",
                            headers = jsonHeaders,
                        )
                    },
                )
            assertTrue(search.search("demo").isFailure)
            search.close()
        }

    @Test
    fun unknownFields_areIgnored() =
        runTest {
            val body =
                SearchFixtures.searchResponse(
                    SearchFixtures.shelf(SearchFixtures.songRenderer("vid1", "Good Song", SearchFixtures.minimalSongSubtitle)),
                ).replace("\"contents\":[", "\"futureField\":123,\"contents\":[")
            val search = YouTubeMusicSearch(mockClient { respond(body, headers = jsonHeaders) })
            assertEquals(1, search.search("demo").getOrThrow().items.size)
            search.close()
        }

    @Test
    fun timeout_returnsFailure() =
        runTest {
            val search =
                YouTubeMusicSearch(
                    mockClient(requestTimeoutMillis = 100) {
                        delay(5_000)
                        respond("{}")
                    },
                )
            assertTrue(search.search("demo").isFailure)
            search.close()
        }

    @Test
    fun cancellation_doesNotProduceResults() =
        runTest {
            val search =
                YouTubeMusicSearch(
                    mockClient {
                        delay(60_000)
                        respond("{}")
                    },
                )
            val deferred = async { search.search("demo") }
            launch { delay(10); deferred.cancel() }
            try {
                val result = deferred.await()
                assertTrue(result.isFailure)
                assertIs<CancellationException>(result.exceptionOrNull())
            } catch (e: CancellationException) {
                // También aceptable: la cancelación se propaga sin envolver.
            }
            search.close()
        }

    @Test
    fun continuation_returnsItemsAndToken() =
        runTest {
            val body =
                """{
                  "contents":null,
                  "continuationContents":{"musicShelfContinuation":{
                    "contents":[{"musicResponsiveListItemRenderer":${SearchFixtures.songRenderer("vid9", "Continued Song", SearchFixtures.minimalSongSubtitle)}}],
                    "continuations":[{"nextContinuationData":{"continuation":"next-token"}}]
                  }}
                }"""
            val search = YouTubeMusicSearch(mockClient { respond(body, headers = jsonHeaders) })
            val page = search.searchContinuation("token-1").getOrThrow()
            assertEquals(1, page.items.size)
            assertEquals("next-token", page.continuation)
            search.close()
        }

    @Test
    fun continuation_emptyItems_yieldsNullToken() =
        runTest {
            val body =
                """{"contents":null,"continuationContents":{"musicShelfContinuation":{"contents":[],"continuations":[{"nextContinuationData":{"continuation":"stale"}}]}}}"""
            val search = YouTubeMusicSearch(mockClient { respond(body, headers = jsonHeaders) })
            val page = search.searchContinuation("token-1").getOrThrow()
            assertEquals(emptyList(), page.items)
            assertNull(page.continuation)
            search.close()
        }
}
