package com.meld.shared.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.meld.shared.music.Album
import com.meld.shared.music.Artist
import com.meld.shared.music.CatalogSnapshot
import com.meld.shared.music.DemoCatalog
import com.meld.shared.music.InMemoryMusicCatalogRepository
import com.meld.shared.music.MusicCatalogRepository
import com.meld.shared.music.Playlist
import com.meld.shared.music.Track
import com.meld.shared.music.YouTubeSongSearchRepository
import com.meld.shared.music.snapshot
import com.meld.shared.network.createYouTubeHttpClient
import com.meld.shared.search.YouTubeMusicSearch

private val MeldDarkColors =
    darkColorScheme(
        primary = Color(0xFFBB86FC),
        secondary = Color(0xFF03DAC6),
        background = Color(0xFF121212),
        surface = Color(0xFF1E1E1E),
    )

private sealed interface Route {
    data class Tabs(val index: Int) : Route
    data class TrackDetail(val id: String) : Route
    data class AlbumDetail(val id: String) : Route
    data class ArtistDetail(val id: String) : Route
    data class PlaylistDetail(val id: String) : Route
    data class SearchTrackDetail(val tracks: List<Track>, val id: String) : Route
}

private val TAB_TITLES = listOf("Songs", "Albums", "Artists", "Playlists", "Search")

/**
 * First functional Meld-iOS screen (Phase 02A): browsable demo music catalog
 * backed by the real [MusicCatalogRepository] contract (in-memory data).
 *
 * No playback, no networking, no login: the disabled button on the track
 * detail marks playback as explicitly unavailable instead of faking it.
 */
@Composable
fun MeldApp() {
    val repository = remember { DemoCatalog.repository() }
    // Fase 02C: búsqueda real YouTube Music (cliente app-lifetime, Darwin en
    // iOS). Las pestañas 0-3 siguen usando el catálogo de demostración.
    val searchRepository =
        remember {
            YouTubeSongSearchRepository(YouTubeMusicSearch(createYouTubeHttpClient()))
        }
    var route by remember { mutableStateOf<Route>(Route.Tabs(0)) }
    var snapshot by remember { mutableStateOf<CatalogSnapshot?>(null) }
    var loadError by remember { mutableStateOf<String?>(null) }
    var attempt by remember { mutableStateOf(0) }

    LaunchedEffect(attempt) {
        try {
            snapshot = repository.snapshot()
            loadError = null
        } catch (e: Exception) {
            loadError = e.message ?: "Unknown error"
        }
    }

    MaterialTheme(colorScheme = MeldDarkColors) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background,
        ) {
            val current = snapshot
            when {
                loadError != null -> ErrorView(message = loadError!!, onRetry = { attempt++ })
                current == null -> LoadingView()
                else ->
                    when (val r = route) {
                        is Route.Tabs ->
                            TabsScreen(
                                snapshot = current,
                                tabIndex = r.index,
                                onTab = { route = Route.Tabs(it) },
                                onTrack = { route = Route.TrackDetail(it) },
                                onAlbum = { route = Route.AlbumDetail(it) },
                                onArtist = { route = Route.ArtistDetail(it) },
                                onPlaylist = { route = Route.PlaylistDetail(it) },
                                searchRepository = searchRepository,
                                onSearchTrack = { tracks, id -> route = Route.SearchTrackDetail(tracks, id) },
                            )
                        is Route.TrackDetail ->
                            TrackDetailScreen(
                                repository = repository,
                                trackId = r.id,
                                onBack = { route = Route.Tabs(0) },
                                onAlbum = { route = Route.AlbumDetail(it) },
                            )
                        is Route.AlbumDetail ->
                            AlbumDetailScreen(
                                repository = repository,
                                albumId = r.id,
                                onBack = { route = Route.Tabs(1) },
                                onTrack = { route = Route.TrackDetail(it) },
                            )
                        is Route.ArtistDetail ->
                            ArtistDetailScreen(
                                repository = repository,
                                artistId = r.id,
                                onBack = { route = Route.Tabs(2) },
                                onTrack = { route = Route.TrackDetail(it) },
                            )
                        is Route.PlaylistDetail ->
                            PlaylistDetailScreen(
                                repository = repository,
                                playlistId = r.id,
                                onBack = { route = Route.Tabs(3) },
                                onTrack = { route = Route.TrackDetail(it) },
                            )
                        is Route.SearchTrackDetail ->
                            TrackDetailScreen(
                                repository = remember(r.tracks) { InMemoryMusicCatalogRepository(tracks = r.tracks) },
                                trackId = r.id,
                                onBack = { route = Route.Tabs(4) },
                                onAlbum = { route = Route.Tabs(4) },
                            )
                    }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TabsScreen(
    snapshot: CatalogSnapshot,
    tabIndex: Int,
    onTab: (Int) -> Unit,
    onTrack: (String) -> Unit,
    onAlbum: (String) -> Unit,
    onArtist: (String) -> Unit,
    onPlaylist: (String) -> Unit,
    searchRepository: com.meld.shared.music.SongSearchRepository,
    onSearchTrack: (List<Track>, String) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Meld-iOS", fontWeight = FontWeight.Bold)
                        Text(
                            text = "Demo catalog · no streaming yet",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
            )
        },
        bottomBar = {
            NavigationBar {
                TAB_TITLES.forEachIndexed { index, title ->
                    NavigationBarItem(
                        selected = tabIndex == index,
                        onClick = { onTab(index) },
                        label = { Text(title) },
                        icon = {},
                    )
                }
            }
        },
    ) { innerPadding ->
        when (tabIndex) {
            0 ->
                CatalogList(
                    modifier = Modifier.padding(innerPadding),
                    items = snapshot.tracks,
                    emptyMessage = "No songs in this demo catalog.",
                    row = { track ->
                        CatalogRow(
                            title = track.title,
                            subtitle = "${track.artistNames} · ${track.album?.title ?: "Single"}",
                            trailing = track.formattedDuration,
                            explicit = track.explicit,
                            onClick = { onTrack(track.id) },
                        )
                    },
                )
            1 ->
                CatalogList(
                    modifier = Modifier.padding(innerPadding),
                    items = snapshot.albums,
                    emptyMessage = "No albums in this demo catalog.",
                    row = { album ->
                        CatalogRow(
                            title = album.title,
                            subtitle = listOfNotNull(album.artistName, album.year?.toString()).joinToString(" · "),
                            trailing = "${album.trackIds.size} tracks",
                            onClick = { onAlbum(album.id) },
                        )
                    },
                )
            2 ->
                CatalogList(
                    modifier = Modifier.padding(innerPadding),
                    items = snapshot.artists,
                    emptyMessage = "No artists in this demo catalog.",
                    row = { artist ->
                        CatalogRow(
                            title = artist.name,
                            subtitle = "Artist",
                            onClick = { onArtist(artist.id) },
                        )
                    },
                )
            3 ->
                CatalogList(
                    modifier = Modifier.padding(innerPadding),
                    items = snapshot.playlists,
                    emptyMessage = "No playlists in this demo catalog.",
                    row = { playlist ->
                        CatalogRow(
                            title = playlist.name,
                            subtitle = playlist.description ?: "Playlist",
                            trailing = "${playlist.trackIds.size} tracks",
                            onClick = { onPlaylist(playlist.id) },
                        )
                    },
                )
            else ->
                SearchScreen(
                    searchRepository = searchRepository,
                    onTrack = onSearchTrack,
                    modifier = Modifier.padding(innerPadding),
                )
        }
    }
}

@Composable
private fun <T> CatalogList(
    modifier: Modifier = Modifier,
    items: List<T>,
    emptyMessage: String,
    row: @Composable (T) -> Unit,
) {
    if (items.isEmpty()) {
        EmptyView(message = emptyMessage)
    } else {
        LazyColumn(modifier = modifier.fillMaxSize()) {
            items(items) { item -> row(item) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DetailScaffold(
    title: String,
    onBack: () -> Unit,
    content: @Composable () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title, maxLines = 1) },
                navigationIcon = {
                    TextButton(onClick = onBack) {
                        Text("‹ Back")
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            content()
        }
    }
}

@Composable
private fun TrackDetailScreen(
    repository: MusicCatalogRepository,
    trackId: String,
    onBack: () -> Unit,
    onAlbum: (String) -> Unit,
) {
    var track by remember(trackId) { mutableStateOf<Track?>(null) }
    var missing by remember(trackId) { mutableStateOf(false) }
    LaunchedEffect(trackId) {
        track = repository.track(trackId)
        missing = track == null
    }
    DetailScaffold(title = "Song", onBack = onBack) {
        val current = track
        when {
            missing -> EmptyView("This demo song no longer exists.")
            current == null -> LoadingView()
            else -> {
                Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
                    ArtPlaceholder(name = current.title, modifier = Modifier.size(160.dp))
                    Spacer(Modifier.height(16.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = current.title,
                            modifier = Modifier.weight(1f, fill = false),
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground,
                        )
                        if (current.explicit) {
                            Spacer(Modifier.width(8.dp))
                            ExplicitBadge()
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = current.artistNames,
                        color = MaterialTheme.colorScheme.secondary,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Duration ${current.formattedDuration}",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    current.album?.let { album ->
                        Spacer(Modifier.height(12.dp))
                        CatalogRow(
                            title = album.title,
                            subtitle = "Album · view details",
                            onClick = { onAlbum(album.id) },
                        )
                    }
                    Spacer(Modifier.height(20.dp))
                    OutlinedButton(
                        onClick = {},
                        enabled = false,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Playback not available in this phase")
                    }
                }
            }
        }
    }
}

@Composable
private fun AlbumDetailScreen(
    repository: MusicCatalogRepository,
    albumId: String,
    onBack: () -> Unit,
    onTrack: (String) -> Unit,
) {
    var album by remember(albumId) { mutableStateOf<Album?>(null) }
    var tracks by remember(albumId) { mutableStateOf<List<Track>?>(null) }
    LaunchedEffect(albumId) {
        album = repository.album(albumId)
        tracks = repository.albumTracks(albumId)
    }
    DetailScaffold(title = "Album", onBack = onBack) {
        val current = album
        when {
            current == null && tracks != null -> EmptyView("This demo album no longer exists.")
            current == null -> LoadingView()
            else -> {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    item {
                        DetailHeader(
                            title = current.title,
                            subtitle = listOfNotNull(current.artistName, current.year?.toString()).joinToString(" · "),
                        )
                        SectionTitle("Songs")
                    }
                    val list = tracks ?: emptyList()
                    if (list.isEmpty()) {
                        item { EmptyView("This demo album has no songs.") }
                    } else {
                        items(list) { track ->
                            CatalogRow(
                                title = track.title,
                                subtitle = track.artistNames,
                                trailing = track.formattedDuration,
                                explicit = track.explicit,
                                onClick = { onTrack(track.id) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ArtistDetailScreen(
    repository: MusicCatalogRepository,
    artistId: String,
    onBack: () -> Unit,
    onTrack: (String) -> Unit,
) {
    var artist by remember(artistId) { mutableStateOf<Artist?>(null) }
    var tracks by remember(artistId) { mutableStateOf<List<Track>?>(null) }
    LaunchedEffect(artistId) {
        artist = repository.artist(artistId)
        tracks = repository.artistTracks(artistId)
    }
    DetailScaffold(title = "Artist", onBack = onBack) {
        val current = artist
        when {
            current == null && tracks != null -> EmptyView("This demo artist no longer exists.")
            current == null -> LoadingView()
            else -> {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    item {
                        DetailHeader(title = current.name, subtitle = "Artist")
                        SectionTitle("Songs")
                    }
                    val list = tracks ?: emptyList()
                    if (list.isEmpty()) {
                        item { EmptyView("This demo artist has no songs.") }
                    } else {
                        items(list) { track ->
                            CatalogRow(
                                title = track.title,
                                subtitle = track.album?.title ?: "Single",
                                trailing = track.formattedDuration,
                                explicit = track.explicit,
                                onClick = { onTrack(track.id) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PlaylistDetailScreen(
    repository: MusicCatalogRepository,
    playlistId: String,
    onBack: () -> Unit,
    onTrack: (String) -> Unit,
) {
    var playlist by remember(playlistId) { mutableStateOf<Playlist?>(null) }
    var tracks by remember(playlistId) { mutableStateOf<List<Track>?>(null) }
    LaunchedEffect(playlistId) {
        playlist = repository.playlist(playlistId)
        tracks = repository.playlistTracks(playlistId)
    }
    DetailScaffold(title = "Playlist", onBack = onBack) {
        val current = playlist
        when {
            current == null && tracks != null -> EmptyView("This demo playlist no longer exists.")
            current == null -> LoadingView()
            else -> {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    item {
                        DetailHeader(
                            title = current.name,
                            subtitle = current.description ?: "Playlist",
                        )
                        SectionTitle("Songs")
                    }
                    val list = tracks ?: emptyList()
                    if (list.isEmpty()) {
                        item { EmptyView("This demo playlist has no songs.") }
                    } else {
                        items(list) { track ->
                            CatalogRow(
                                title = track.title,
                                subtitle = track.artistNames,
                                trailing = track.formattedDuration,
                                explicit = track.explicit,
                                onClick = { onTrack(track.id) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailHeader(
    title: String,
    subtitle: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(20.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ArtPlaceholder(name = title, modifier = Modifier.size(96.dp))
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = subtitle,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
