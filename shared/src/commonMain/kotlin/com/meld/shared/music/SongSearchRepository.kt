package com.meld.shared.music

import com.meld.shared.innertube.models.SongItem
import com.meld.shared.search.YouTubeMusicSearch

/**
 * Búsqueda de canciones en YouTube Music (Fase 02C, PASO E/F).
 *
 * Convierte `SongItem` (modelo original portado) al `Track` 02A:
 * - `id`/`title` directos (nunca inventados; si faltan, el parser ya
 *   descartó el item con `null` en `SearchPage.toYTItem`).
 * - `artists` → `ArtistRef(id = browseId o "" si ausente, name)`.
 * - `album` → `AlbumRef` o `null` (single sin álbum).
 * - `duration` null → `-1` (convención 02A = desconocida, igual que Meld).
 * - `thumbnail` → `artworkUrl`; `explicit` directo.
 *
 * Los errores de red/serialización se propagan como `Result.failure`
 * (la UI distingue vacío de error); nunca se convierten en datos falsos.
 */
interface SongSearchRepository {
    suspend fun searchSongs(query: String): Result<List<Track>>
}

fun SongItem.toTrack(): Track =
    Track(
        id = id,
        title = title,
        artists = artists.map { ArtistRef(id = it.id.orEmpty(), name = it.name) },
        album = album?.let { AlbumRef(id = it.id, title = it.name) },
        durationSeconds = duration ?: -1,
        artworkUrl = thumbnail,
        explicit = explicit,
    )

class YouTubeSongSearchRepository(
    private val search: YouTubeMusicSearch,
) : SongSearchRepository {
    override suspend fun searchSongs(query: String): Result<List<Track>> =
        search.search(query).map { page ->
            page.items.filterIsInstance<SongItem>().map { it.toTrack() }
        }
}
