package com.meld.shared.music

/**
 * Dominio musical iOS-only (Fase 02A).
 *
 * Conceptos reutilizados de Meld/Metrolist (`MediaMetadata`, `SongEntity`,
 * `AlbumEntity`, `ArtistEntity`, `PlaylistEntity`) reducidos a lo necesario
 * para el primer catálogo navegable, sin dependencias Android/Room/Media3:
 * solo stdlib de Kotlin, inmutables y serializables más adelante.
 *
 * Convenciones:
 * - Identificadores: `String` no vacío, opacos (hoy de demostración; mañana
 *   IDs de YouTube Music / Spotify / InnerTubeX).
 * - Duración: segundos como `Int`; `-1` = desconocida (igual que el original).
 * - Artwork: URL opcional como `String`; en 02A no se carga ninguna imagen de
 *   red (placeholder local en la UI).
 * - Listas vacías en lugar de null para relaciones ausentes.
 */
data class Track(
    val id: String,
    val title: String,
    val artists: List<ArtistRef> = emptyList(),
    val album: AlbumRef? = null,
    val durationSeconds: Int = -1,
    val artworkUrl: String? = null,
    val explicit: Boolean = false,
) {
    val formattedDuration: String get() = formatDuration(durationSeconds)

    val artistNames: String
        get() = artists.joinToString(", ") { it.name }.ifEmpty { "Unknown artist" }
}

data class ArtistRef(
    val id: String,
    val name: String,
)

data class AlbumRef(
    val id: String,
    val title: String,
)

data class Artist(
    val id: String,
    val name: String,
    val artworkUrl: String? = null,
)

data class Album(
    val id: String,
    val title: String,
    val artistName: String? = null,
    val year: Int? = null,
    val artworkUrl: String? = null,
    val trackIds: List<String> = emptyList(),
)

data class Playlist(
    val id: String,
    val name: String,
    val description: String? = null,
    val artworkUrl: String? = null,
    val trackIds: List<String> = emptyList(),
)

fun formatDuration(totalSeconds: Int): String {
    if (totalSeconds < 0) return "--:--"
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        "$hours:${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}"
    } else {
        "$minutes:${seconds.toString().padStart(2, '0')}"
    }
}
