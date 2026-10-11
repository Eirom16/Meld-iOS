package com.meld.shared.music

/**
 * Datos de demostración: ficticios y claramente marcados como demo.
 *
 * Ningún título/artista existe como tal en un catálogo real; sirven solo
 * para navegar la UI de la Fase 02A. Sin networking, sin credenciales.
 */
object DemoCatalog {
    private val demoArtistOne = Artist(id = "demo-artist-1", name = "Demo Artist One")
    private val demoArtistTwo = Artist(id = "demo-artist-2", name = "Demo Artist Two")
    private val demoEnsemble = Artist(id = "demo-artist-3", name = "Demo Ensemble")

    private val demoAlbumAurora =
        Album(
            id = "demo-album-1",
            title = "Demo Aurora",
            artistName = demoArtistOne.name,
            year = 2026,
            trackIds = listOf("demo-track-1", "demo-track-2", "demo-track-5"),
        )
    private val demoAlbumNocturne =
        Album(
            id = "demo-album-2",
            title = "Demo Nocturne",
            artistName = demoArtistTwo.name,
            year = 2025,
            trackIds = listOf("demo-track-3", "demo-track-4"),
        )

    private val demoTracks =
        listOf(
            Track(
                id = "demo-track-1",
                title = "Demo Sunrise",
                artists = listOf(ArtistRef(demoArtistOne.id, demoArtistOne.name)),
                album = AlbumRef(demoAlbumAurora.id, demoAlbumAurora.title),
                durationSeconds = 214,
            ),
            Track(
                id = "demo-track-2",
                title = "Demo Meridian (Explicit)",
                artists = listOf(ArtistRef(demoArtistOne.id, demoArtistOne.name)),
                album = AlbumRef(demoAlbumAurora.id, demoAlbumAurora.title),
                durationSeconds = 187,
                explicit = true,
            ),
            Track(
                id = "demo-track-3",
                title = "Demo Midnight Train",
                artists = listOf(ArtistRef(demoArtistTwo.id, demoArtistTwo.name)),
                album = AlbumRef(demoAlbumNocturne.id, demoAlbumNocturne.title),
                durationSeconds = 243,
            ),
            Track(
                id = "demo-track-4",
                title = "Demo Duet at Dusk",
                artists =
                    listOf(
                        ArtistRef(demoArtistTwo.id, demoArtistTwo.name),
                        ArtistRef(demoEnsemble.id, demoEnsemble.name),
                    ),
                album = AlbumRef(demoAlbumNocturne.id, demoAlbumNocturne.title),
                durationSeconds = 3721,
            ),
            Track(
                id = "demo-track-5",
                title = "Demo Untitled Sketch",
                artists = listOf(ArtistRef(demoArtistOne.id, demoArtistOne.name)),
                album = AlbumRef(demoAlbumAurora.id, demoAlbumAurora.title),
                durationSeconds = -1,
            ),
            Track(
                id = "demo-track-6",
                title = "Demo Single Without Album",
                artists = listOf(ArtistRef(demoEnsemble.id, demoEnsemble.name)),
                durationSeconds = 95,
            ),
        )

    private val demoPlaylists =
        listOf(
            Playlist(
                id = "demo-playlist-1",
                name = "Demo Favorites",
                description = "A short demo playlist mixing both demo albums.",
                trackIds = listOf("demo-track-1", "demo-track-3", "demo-track-6"),
            ),
            Playlist(
                id = "demo-playlist-2",
                name = "Demo Longform",
                description = "Demo playlist with one long demo track.",
                trackIds = listOf("demo-track-4", "demo-track-missing"),
            ),
        )

    fun repository(): InMemoryMusicCatalogRepository =
        InMemoryMusicCatalogRepository(
            tracks = demoTracks,
            albums = listOf(demoAlbumAurora, demoAlbumNocturne),
            artists = listOf(demoArtistOne, demoArtistTwo, demoEnsemble),
            playlists = demoPlaylists,
        )

    fun emptyRepository(): InMemoryMusicCatalogRepository = InMemoryMusicCatalogRepository()

    fun failingRepository(): InMemoryMusicCatalogRepository =
        InMemoryMusicCatalogRepository(
            failure = DemoCatalogException("Demo data source unavailable"),
        )
}
