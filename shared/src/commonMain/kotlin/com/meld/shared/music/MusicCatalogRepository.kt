package com.meld.shared.music

/**
 * Contrato mínimo de acceso al catálogo musical (Fase 02A).
 *
 * Funciones `suspend` para que una fase posterior pueda sustituir la
 * implementación en memoria por Ktor + fuentes reales sin cambiar la UI.
 * Las búsquedas por identificador devuelven `null` si no existen.
 */
interface MusicCatalogRepository {
    suspend fun tracks(): List<Track>
    suspend fun track(id: String): Track?

    suspend fun albums(): List<Album>
    suspend fun album(id: String): Album?

    suspend fun artists(): List<Artist>
    suspend fun artist(id: String): Artist?

    suspend fun playlists(): List<Playlist>
    suspend fun playlist(id: String): Playlist?

    suspend fun playlistTracks(playlistId: String): List<Track>
    suspend fun albumTracks(albumId: String): List<Track>
    suspend fun artistTracks(artistId: String): List<Track>
}

data class CatalogSnapshot(
    val tracks: List<Track>,
    val albums: List<Album>,
    val artists: List<Artist>,
    val playlists: List<Playlist>,
)

suspend fun MusicCatalogRepository.snapshot(): CatalogSnapshot =
    CatalogSnapshot(
        tracks = tracks(),
        albums = albums(),
        artists = artists(),
        playlists = playlists(),
    )

class DemoCatalogException(message: String) : Exception(message)

/**
 * Implementación en memoria con datos inyectados.
 *
 * `failure != null` simula un origen de datos roto (cada llamada lanza esa
 * excepción) para probar los estados de error sin red ni disco.
 */
class InMemoryMusicCatalogRepository(
    tracks: List<Track> = emptyList(),
    albums: List<Album> = emptyList(),
    artists: List<Artist> = emptyList(),
    playlists: List<Playlist> = emptyList(),
    private val failure: DemoCatalogException? = null,
) : MusicCatalogRepository {
    private val tracksById = tracks.associateBy { it.id }
    private val albumsById = albums.associateBy { it.id }
    private val artistsById = artists.associateBy { it.id }
    private val playlistsById = playlists.associateBy { it.id }

    private fun checkHealth() {
        failure?.let { throw it }
    }

    override suspend fun tracks(): List<Track> {
        checkHealth()
        return tracksById.values.toList()
    }

    override suspend fun track(id: String): Track? {
        checkHealth()
        return tracksById[id]
    }

    override suspend fun albums(): List<Album> {
        checkHealth()
        return albumsById.values.toList()
    }

    override suspend fun album(id: String): Album? {
        checkHealth()
        return albumsById[id]
    }

    override suspend fun artists(): List<Artist> {
        checkHealth()
        return artistsById.values.toList()
    }

    override suspend fun artist(id: String): Artist? {
        checkHealth()
        return artistsById[id]
    }

    override suspend fun playlists(): List<Playlist> {
        checkHealth()
        return playlistsById.values.toList()
    }

    override suspend fun playlist(id: String): Playlist? {
        checkHealth()
        return playlistsById[id]
    }

    override suspend fun playlistTracks(playlistId: String): List<Track> {
        checkHealth()
        val playlist = playlistsById[playlistId] ?: return emptyList()
        return playlist.trackIds.mapNotNull { tracksById[it] }
    }

    override suspend fun albumTracks(albumId: String): List<Track> {
        checkHealth()
        val album = albumsById[albumId] ?: return emptyList()
        val wanted = album.trackIds.toSet()
        return tracksById.values.filter { it.album?.id == albumId || it.id in wanted }
    }

    override suspend fun artistTracks(artistId: String): List<Track> {
        checkHealth()
        if (artistsById[artistId] == null) return emptyList()
        return tracksById.values.filter { track -> track.artists.any { it.id == artistId } }
    }
}
