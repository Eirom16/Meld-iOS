package com.meld.shared.search

import com.meld.shared.innertube.models.YTItem
import com.meld.shared.innertube.models.getContinuation
import com.meld.shared.innertube.models.getItems
import com.meld.shared.innertube.models.response.SearchResponse
import com.meld.shared.innertube.pages.SearchPage
import com.meld.shared.innertube.pages.SearchResult
import com.metrolist.innertubex.InnerTube
import com.metrolist.innertubex.models.YouTubeClient.Companion.WEB_REMIX
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import kotlin.jvm.JvmInline

/**
 * Primera operación real de catálogo (Fase 02C, PASO E).
 *
 * Procedencia: `innertube/.../YouTube.kt` (`object YouTube`,
 * `search(query, filter)` + `searchContinuation(continuation)` +
 * `SearchFilter`). Lógica de recorrido de `SearchResponse` y conversión
 * vía `SearchPage.toYTItem` conservada tal cual.
 *
 * Adaptaciones 02C (registradas):
 * - El transporte ya no es el facade `InnerTube` propio (OkHttp +
 *   `java.net.Proxy`, JVM-only) sino `com.metrolist.innertubex.InnerTube`
 *   v0.8.4 construido sobre el `HttpClient` inyectado (Darwin en iOS).
 * - Sin `Timber`, sin estado global (`object`), sin sesión con login:
 *   búsqueda anónima (`setLogin` por defecto del servidor).
 * - Los reintentos (`withRetry` NET-12) los aporta InnerTubeX 0.8.x
 *   (`executeRequest`: 3 intentos, transitorios 408/425/429/500/502/503/504,
 *   429 solo con `Retry-After` ≤ 5 s); no se duplican aquí.
 * - Consulta en blanco: éxito vacío sin emitir red (evita llamadas inútiles
 *   y da estado vacío determinista a la UI).
 *
 * El llamador es dueño del ciclo de vida: [close] cancela el trabajo de
 * InnerTubeX y cierra el [httpClient] inyectado.
 */
class YouTubeMusicSearch(
    private val httpClient: HttpClient,
) {
    private val innerTube = InnerTube(httpClient)

    suspend fun search(
        query: String,
        filter: SearchFilter = SearchFilter.FILTER_SONG,
    ): Result<SearchResult> {
        if (query.isBlank()) return Result.success(SearchResult(emptyList()))
        return runCatching {
            val response = innerTube.search(WEB_REMIX, query, filter.value).body<SearchResponse>()
            val searchItems = mutableListOf<YTItem>()
            var searchContinuation: String? = null

            response.contents
                ?.tabbedSearchResultsRenderer
                ?.tabs
                ?.firstOrNull()
                ?.tabRenderer
                ?.content
                ?.sectionListRenderer
                ?.contents
                ?.forEach { section ->
                    if (section.musicShelfRenderer != null) {
                        val shelf = section.musicShelfRenderer
                        val items = shelf.contents?.getItems()?.mapNotNull { SearchPage.toYTItem(it) } ?: emptyList()
                        searchItems.addAll(items)
                        if (searchContinuation == null) {
                            searchContinuation = shelf.continuations?.getContinuation()
                        }
                    } else if (section.itemSectionRenderer != null) {
                        val items =
                            section.itemSectionRenderer.contents
                                ?.mapNotNull { it.musicResponsiveListItemRenderer }
                                ?.mapNotNull { SearchPage.toYTItem(it) }
                                ?: emptyList()
                        searchItems.addAll(items)
                    }
                }

            SearchResult(
                items = searchItems.distinctBy { it.id },
                continuation = searchContinuation,
            )
        }
    }

    suspend fun searchContinuation(continuation: String): Result<SearchResult> =
        runCatching {
            val response = innerTube.search(WEB_REMIX, continuation = continuation).body<SearchResponse>()
            val items =
                response.continuationContents
                    ?.musicShelfContinuation
                    ?.contents
                    ?.mapNotNull {
                        SearchPage.toYTItem(it.musicResponsiveListItemRenderer)
                    } ?: emptyList()
            SearchResult(
                items = items,
                continuation =
                    if (items.isEmpty()) {
                        null
                    } else {
                        response.continuationContents
                            ?.musicShelfContinuation
                            ?.continuations
                            ?.getContinuation()
                    },
            )
        }

    fun close() {
        innerTube.close()
        httpClient.close()
    }

    /**
     * Procedencia: `YouTube.SearchFilter` (mismo valor `params`).
     * Solo expone el filtro de canciones en esta fase; el resto llegará
     * con la exploración P0.2 (álbumes/artistas).
     */
    @JvmInline
    value class SearchFilter(
        val value: String,
    ) {
        companion object {
            val FILTER_SONG = SearchFilter("EgWKAQIIAWoKEAkQBRAKEAMQBA%3D%3D")
        }
    }
}
