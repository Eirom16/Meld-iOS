package com.meld.shared.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.meld.shared.music.SongSearchRepository
import com.meld.shared.music.Track

/**
 * Primera búsqueda real (Fase 02C, PASO F).
 *
 * Experiencia mínima sobre [SongSearchRepository]: campo editable, envío con
 * botón o teclado, carga, resultados con título/artista/duración, badge
 * explícito, vacío, error con reintento. Sin reproductor (fuera de alcance).
 *
 * Etiqueta honesta: los resultados llevan "YouTube Music · live"; el resto de
 * pestañas sigue siendo el catálogo de demostración.
 */
@Composable
fun SearchScreen(
    searchRepository: SongSearchRepository,
    onTrack: (results: List<Track>, trackId: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var query by remember { mutableStateOf("") }
    var submitted by remember { mutableStateOf<String?>(null) }
    var results by remember { mutableStateOf<List<Track>?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var searching by remember { mutableStateOf(false) }
    var attempt by remember { mutableStateOf(0) }

    fun submit() {
        val q = query.trim()
        if (q.isEmpty() || searching) return
        submitted = q
        attempt++
    }

    LaunchedEffect(attempt) {
        val q = submitted ?: return@LaunchedEffect
        searching = true
        error = null
        try {
            results = searchRepository.searchSongs(q).getOrThrow()
        } catch (e: Exception) {
            results = null
            error = e.message ?: "Unknown error"
        } finally {
            searching = false
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        Text(
            text = "YouTube Music · live search",
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.secondary,
        )
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.weight(1f),
                label = { Text("Song title") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { submit() }),
            )
            Spacer(Modifier.width(8.dp))
            Button(onClick = ::submit, enabled = !searching) {
                Text("Search")
            }
        }
        Spacer(Modifier.height(8.dp))
        val currentError = error
        val currentResults = results
        when {
            searching -> LoadingView(message = "Searching YouTube Music…")
            currentError != null ->
                ErrorView(
                    message = "Live search failed: $currentError",
                    onRetry = { attempt++ },
                )
            currentResults == null ->
                EmptyView("Type a song title above to search YouTube Music live.")
            currentResults.isEmpty() ->
                EmptyView("No songs found on YouTube Music for “${submitted ?: ""}”.")
            else ->
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(currentResults) { track ->
                        CatalogRow(
                            title = track.title,
                            subtitle = "${track.artistNames} · ${track.album?.title ?: "Single"}",
                            trailing = track.formattedDuration,
                            explicit = track.explicit,
                            onClick = { onTrack(currentResults, track.id) },
                        )
                    }
                }
        }
    }
}
