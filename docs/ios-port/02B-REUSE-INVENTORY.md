# 02B — Inventario de reutilización (técnico y verificable)

Fecha (UTC): 2026-10-12 · Rama `feat/ios-foundation` @ `9825cbfe`.
Fuente: auditoría `02B-PORTABILITY-AUDIT.md`. Rutas relativas a la raíz del repo.
Esfuerzo relativo: LOW / MEDIUM / HIGH / VERY_HIGH (sin horas/días).
Destino: ruta esperada en Meld-iOS (`shared/src/commonMain/…`, `iosMain/…`, o nueva; `app/`/`innertube/` NO se tocan).

## Cómo leer cada fila

`ID` · ruta original · propósito · dependencias · clasificación · riesgos · esfuerzo ·
dependencias previas (qué debe existir antes) · estrategia · destino.

---

## A. Dominio y modelos (13)

| ID | Ruta original | Propósito | Dependencias | Clasif. | Riesgos | Esf. | Previas | Estrategia | Destino |
|---|---|---|---|---|---|---|---|---|---|
| DOM-01 | `innertube/…/models/YTItem.kt` (+`SongItem/AlbumItem/PlaylistItem/ArtistItem/Album/Artist`) | Modelo base de red YT (`id/title/thumbnail/explicit/shareLink`) | `kotlinx.serialization` | DIRECT | Divergencia de forma YT | LOW | Ninguna | Mover a común tal cual | `shared/commonMain/…/innertube/models/` |
| DOM-02 | `innertube/…/models/` (restantes ~40) + `models/response/` (13) | Renderers YT + respuestas browse/search/next/player | `kotlinx.serialization` | DIRECT | Volumen; `!!` en parsing | LOW | DOM-01 | Mover en bloque, golden-tests con fixtures | idem |
| DOM-03 | `innertube/…/pages/` (22: `SearchPage/AlbumPage/ArtistPage/PlaylistPage/HomePage/…`) | Parsing alto nivel → `YTItem` | DOM-02 | DIRECT | `!!` en `album/transcript` | LOW | DOM-02 | Mover + endurecer nulos | `shared/commonMain/…/innertube/pages/` |
| DOM-04 | `app/…/music/db/entities/LocalItem.kt` | Base `id/title/thumbnailUrl` de Song/Album/Artist/Playlist | Ninguna | DIRECT | Ninguno | LOW | Ninguna | Mover tal cual | `shared/commonMain/…/music/` |
| DOM-05 | `app/…/music/db/entities/ArtistPageCache.kt` | Caché página artista serializable | `kotlinx.serialization` | DIRECT | Ninguno | LOW | Ninguna | Mover tal cual | idem |
| DOM-06 | `app/…/music/db/entities/SongEntity.kt` + `Song.kt` | Canción persistida + joins N:M | Room, Compose Immutable, `java.time`, `YouTube` red | ADAPT | Side-effects en entidad | MEDIUM | P1.1 Room | Extraer campos a `Track` común (ya existe en 02A); joins→tablas KMP | Room-KMP entities |
| DOM-07 | `…/db/entities/ArtistEntity.kt` + `Artist.kt` | Artista (`channelId/spotifyId/bookmarkedAt/cachedPageJson`) | Igual | ADAPT | `cachedPageJson` sin tabla | MEDIUM | P1.1 | `Artist` común + `channelId?/bookmarkedAt(epoch)` | idem |
| DOM-08 | `…/db/entities/AlbumEntity.kt` + `Album.kt` | Álbum + N:M artistas | Igual | ADAPT | `artistName` desnormalizado en 02A | MEDIUM | P1.1 | Recuperar `artists:List<ArtistRef>` | idem |
| DOM-09 | `…/db/entities/PlaylistEntity.kt` + `Playlist.kt` + mapas (`PlaylistSongMap` + vistas) | Playlists locales/remotas, orden por `position` | Igual | ADAPT | `description` vive en red, no en Room | MEDIUM | P1.1 | Añadir `browseId?/isEditable` + tabla unión ordenada | idem |
| DOM-10 | `app/…/music/models/MediaMetadata.kt` | Modelo de reproductor (liked/tokens/isrc/episodios) | Compose, `Serializable`, `LocalDateTime`, `YouTube` | ADAPT | Acoplamiento red en `toSongEntity` | MEDIUM | P0.3 | Partir en `NowPlaying` puro + mappers por plataforma | `shared/commonMain/…/player/` |
| DOM-11 | `…/db/entities/{LyricsEntity,FormatEntity}.kt` | Letras + formatos/itags | Room | ADAPT | Ninguno | LOW | P1.1 | Quitar `@Entity`, `@Serializable` | `shared/commonMain/…/music/` |
| DOM-12 | `…/db/entities/{SearchHistory,Event,EventWithSong,RecognitionHistory,PlayCountEntity}.kt` | Historiales y stats | Room, `java.time` | ADAPT | Privacidad historial | LOW | P1.1 | `Long` epoch en vez de `LocalDateTime` | idem |
| DOM-13 | `app/…/music/constants/PreferenceKeys.kt` (804 lín.) | Claves de ajustes musicales | DataStore | ADAPT | ~500 usos acoplados a `Context` | MEDIUM | P1.4 | `MusicPreferences` data class puro; claves como spec | `shared/commonMain/…/prefs/` |

## B. Red InnerTube / Spotify (12)

| ID | Ruta original | Propósito | Dependencias | Clasif. | Riesgos | Esf. | Previas | Estrategia | Destino |
|---|---|---|---|---|---|---|---|---|---|
| NET-01 | `innertube/…/InnerTube.kt` (455 lín.) | Fachada transporte (search/browse/next/mutaciones) | OkHttp, `java.io/net`, InnerTubeX 0.5.2 | ADAPTER | Engine + `jvm.javaio` upload | MEDIUM | P0.1 (bump iTX) | `expect/actual HttpClientFactory(Darwin)`; `ByteReadChannel` | `shared/commonMain/…/innertube/` |
| NET-02 | `innertube/…/YouTube.kt` (3477 lín., 65 `suspend fun`) | Catálogo sin red propia | NET-01, Timber, `FilterInputStream` | ADAPT | `!!`, paginación encadenable | MEDIUM | NET-01 | Sustituir Timber + `InputStream→NSData`; `Result<T>` se conserva | idem |
| NET-03 | `innertube/…/utils/Utils.kt` + `YouTubeUrlParser.kt` + `YouTubeConstants.kt` | `sha1`, cookies, `parseTime`, paginación `completed()` | `MessageDigest` (solo `sha1`) | ADAPT | Ninguno | LOW | Ninguna | `expect hash` o sha1 puro | idem |
| NET-04 | `app/…/music/utils/InnerTubeXPlayer.kt` (374 lín.) | Entrada única extracción audio | Context, ConnectivityManager, WebView-PoToken, iTX-cipher | ADAPTER | PoToken iOS; cipher rotante | HIGH | P0.1+P0.3 | Conservar política+`toPlaybackData`; reescribir ensamblaje con `TokenProvider` iOS | `shared/commonMain` (política) + `iosMain` (provider) |
| NET-05 | `app/…/music/utils/potoken/PoTokenGenerator.kt` + `PoTokenWebView.kt` | PoToken BotGuard vía WebView | `android.webkit.*`, OkHttp, `Instant` | ANDROID_ONLY | Sin equivalente directo | HIGH | P0.3 | WKWebView+bridge o estrategia sin-poToken; decisión D2 | `iosMain/…/potoken/` (nuevo) |
| NET-06 | `app/src/main/kotlin/com/metrolist/spotify/Spotify.kt` (1590 lín.) | REST+GQL Spotify (14 ops) | OkHttp, `TimeUnit/TimeZone` | ADAPT | Hashes rotantes; alcance P2 | MEDIUM | NET-01 | Engine Darwin; parsing manual se conserva | `shared/commonMain/…/spotify/` (P2) |
| NET-07 | `app/…/spotify/{SpotifyAuth,SpotifyHashProvider,SpotifyMapper}.kt` + `models/` (11) | Auth/hashes/mapeo Spotify | `HttpURLConnection`+`Mac` (Auth, JVM) | ADAPT | Credenciales | MEDIUM | NET-06 | Auth por plataforma; modelos puros directos | idem (P2) |
| NET-08 | `app/…/lrclib/` + `kugou/` + `music/betterlyrics/` (modelos+clientes) | Letras (LrcLib/KuGou/BetterLyrics) | Ktor/OkHttp, serialization | ADAPT | Múltiples proveedores | MEDIUM | NET-01 | Unificar tras `HttpClientFactory`; modelos directos | `shared/commonMain/…/lyrics/` (P2) |
| NET-09 | `app/…/lastfm/` + `music/discord/` + `listentogether/` puros | LastFM/Discord/listening-party (partes puras) | Ktor | ADAPT/DEFER | Alcance P2 | MEDIUM | NET-01 | Modelos directos; sockets por plataforma | P2 |
| NET-10 | `app/…/music/utils/YouTubeUtils.kt` (en `ui/utils/`) | Helpers URL/share YT | `android.net.Uri`? (verificar) | ADAPT | Menor | LOW | Ninguna | Purificar | `shared/commonMain` |
| NET-11 | `innertube/…/YouTube.kt:3069` `timedLyrics` + `transcript` | Letras YT (`ANDROID_MUSIC` vs `WEB_REMIX`) | NET-01 | ADAPT | Formatos timed | MEDIUM | NET-01 | Conservar truco de cliente | `shared/commonMain/…/lyrics/` (P2) |
| NET-12 | `innertube/…/InnerTube.kt:381` `withRetry` + `requireSuccess` | Reintentos `IOException` + errores HTTP tipados | Ktor | DIRECT | Ninguno | LOW | NET-01 | Mover tal cual | `shared/commonMain/…/innertube/` |

## C. Reproductor (12)

| ID | Ruta original | Propósito | Dependencias | Clasif. | Riesgos | Esf. | Previas | Estrategia | Destino |
|---|---|---|---|---|---|---|---|---|---|
| PLY-01 | `app/…/playback/queues/Queue.kt` + 10 impls (`List/YouTube*/Spotify*/LocalAlbumRadio/Empty`) | Contrato colas + `Status` + filtros | Media3 `MediaItem` en firmas | ADAPT | Firmas con `MediaItem` | MEDIUM | Ninguna | `CommonQueue` con items propios + adaptadores finos | `shared/commonMain/…/player/` |
| PLY-02 | `app/…/playback/MusicService.kt:765-768,1980-2260,2789-3121` (shuffle/repeat/seek) | Reglas shuffle persistente/repeat-one/all/navegación | `Player.REPEAT_MODE_*`, `Timeline` | ADAPT | Semántica repeat-all+load-more | MEDIUM | PLY-01 | Reglas a común; actuación a `AVQueuePlayer` | idem |
| PLY-03 | `app/…/playback/PlayerConnection.kt` | StateFlows UI (`isEffectivelyPlaying/mediaMetadata/queue/error`) | ExoPlayer crudo expuesto | ADAPTER | Expone motor sin interfaz | HIGH | PLY-01 | **Interponer `PlayerController`** y replicar StateFlows | `shared/commonMain` (interfaz) + `iosMain` (AVPlayer) |
| PLY-04 | `app/…/ui/player/PlaybackError.kt` + `MusicService:358 onPlayerError` | Taxonomía + UX de errores | `PlaybackException` | ADAPT | Códigos ExoPlayer | LOW | PLY-03 | `CommonPlaybackError` + mapear `AVPlayerItemFailed…` | `shared/commonMain/…/player/` |
| PLY-05 | `app/…/playback/MusicService.kt:1427-1515` (construcción ExoPlayer + DSP) | Motor + normalización/silence/sonic | ExoPlayer, AudioProcessor | ANDROID_ONLY | Calidad audio iOS | VERY_HIGH | PLY-03 | `AVPlayer` + `MTAudioProcessingTap`/`AVAudioUnitTimePitch` | `iosMain/…/player/` (nuevo) |
| PLY-06 | `…/MusicService.kt:4048-4105` + `StreamUrlCache.kt` + `AppModule(playerCache)` | Caché streaming (`SimpleCache`, LRU 500) | Media3 cache/DataSpec | ANDROID_ONLY/ADAPT | Rangos `requireBoundedRange` | HIGH | PLY-03 | `AVAssetResourceLoaderDelegate` + caché propia; `StreamUrlCache` desacoplado | `iosMain` + común |
| PLY-07 | `app/…/playback/DownloadUtil.kt` + `ExoDownloadService.kt` | Descargas offline + progreso | DownloadManager/Service, Scheduler | ANDROID_ONLY | Background iOS | HIGH | P1.1 | `URLSession` background + tabla Room | `iosMain` (P2) |
| PLY-08 | `PreferenceKeys:108-114` + `MusicService:796-947` (calidad) | Calidad AUTO/LOW/HIGH→itag | Prefs | DIRECT | Ninguno | LOW | PLY-03 | Tabla tal cual; aplicar al elegir URL | `shared/commonMain/…/player/` |
| PLY-09 | `…/MusicService:5066-5136,1506-1578,1464` + `playback/alarm/` (4) + manifest | Foreground/foco audio/alarmas/sleep-timer | Framework Android | ANDROID_ONLY/ADAPT | Interrupciones | HIGH | PLY-03 | `AVAudioSession` + interrupciones + `BGTaskScheduler`; lógica timer a común | `iosMain` + común |
| PLY-10 | `app/…/playback/MediaLibrarySessionCallback.kt` (1250 lín.) | Comandos custom (like/repeat/shuffle/radio) | Media3 session | ADAPTER | Paridad de comandos | HIGH | PLY-03 | `MPRemoteCommandCenter` + `MPNowPlayingInfoCenter` | `iosMain/…/player/` |
| PLY-11 | `PlaybackData` (`InnerTubeXPlayer:219-231`: URL+headers+rangos+expiración+cliente) | Contrato extracción→player | Ninguna (data class) | DIRECT | Expiración 5 min | LOW | NET-04 | Data class común; refresco con margen | `shared/commonMain/…/player/` |
| PLY-12 | `app/…/music/models/PersistQueue.kt` + `PersistPlayerState.kt` | Persistencia de cola/estado | `Serializable` | ADAPT | Versiones de formato | LOW | P1.1 | `@Serializable` + migración de formato | `shared/commonMain/…/player/` |

## D. Persistencia / prefs / DI (8)

| ID | Ruta original | Propósito | Dependencias | Clasif. | Riesgos | Esf. | Previas | Estrategia | Destino |
|---|---|---|---|---|---|---|---|---|---|
| DB-01 | `…/db/MusicDatabase.kt` (v41, 20 entidades + 3 vistas) | Esquema (intocable) | Room, `SupportSQLite`, `contentValuesOf` | ADAPTER | AutoMigration 40→41 sin listar | VERY_HIGH | Decisión D3 | Room-KMP replicando tablas/columnas | `shared/commonMain/…/db/` (P1) |
| DB-02 | `…/db/DatabaseDao.kt` (~260 fns, 194 `@Query`) | Consultas (spec) | Room, `Flow` | ADAPT | Volumen | HIGH | DB-01 | Portar por familias (library → playlist → stats) | idem |
| DB-03 | `…/db/Converters.kt` (`LocalDateTime↔Long` UTC) | Conversores | `java.time` | ADAPT | Zona horaria | LOW | DB-01 | Epoch `Long` + `kotlinx-datetime`/`Clock` | idem |
| DB-04 | `…/utils/DataStore.kt` (191 lín.) + `PreferenceKeys` | Prefs tipadas + snapshot | `Context.dataStore`, `runBlocking` | ADAPTER | ~500 usos | MEDIUM | Ninguna | `datastore-preferences-core` + `dataStoreFile()` expect/actual | `shared/commonMain/…/prefs/` |
| DB-05 | `…/di/{AppModule,NetworkModule,WrappedModule,Qualifiers}.kt` | Grafo DI (DB, cachés, managers) | Hilt, `Context` | ANDROID_ONLY | Transversal (58 ficheros) | VERY_HIGH | Decisión D4 | Koin o manual, constructor injection | `shared/commonMain/…/di/` |
| DB-06 | Navegación (77 ficheros; `MainActivity:730-914 NavHost`) | Grafo 57 destinos | navigation-compose, Hilt | ADAPTER | Acoplamiento destinos | VERY_HIGH | Decisión D5 | Router propio o navigation-CMP; rutas sin `R.*` | `shared/commonMain/…/nav/` |
| DB-07 | Coil (`App.kt` loader + 49 usos) | Imagen (memoria/disco/red) | `coil-network-okhttp`, `LocalContext` | ADAPTER | Migración 49 usos | LOW–MEDIUM | Ninguna | `coil-compose` + `coil-network-ktor` + `PlatformContext` | Transversal |
| DB-08 | Ktor engines (`HttpClient(OkHttp)` ×N + `okhttp3.*` directos) | HTTP | OkHttp/CIO | ADAPTER | `okhttp3.*` directos (DeepL/OpenRouter/Discord) | MEDIUM | P0.1 | `HttpClientFactory` expect/actual (Darwin) | `shared/commonMain+iosMain` |

## E. UI Compose (12 familias)

| ID | Ruta original | Propósito | Dependencias | Clasif. | Riesgos | Esf. | Previas | Estrategia | Destino |
|---|---|---|---|---|---|---|---|---|---|
| UI-01 | `ui/theme/Theme.kt` + `PlayerColorExtractor.kt` | `MetrolistTheme` + color dinámico | Bitmap/Palette/`dynamic*ColorScheme` | ADAPTER | Todo depende del tema | MEDIUM | Ninguna | `expect ColorExtractor/SystemDynamicColors` (primero) | `shared/commonMain+iosMain` |
| UI-02 | `ui/component/Items.kt` (rows/grid/album…) | Filas de catálogo | DB/Nav/Player/Coil directos | ADAPT | Archivo grande | MEDIUM | DB-07, PLY-03 | Parametrizar (`artworkUrl`, callbacks) | `shared/commonMain/…/ui/` |
| UI-03 | `ui/component/` sliders/shimmer/headers (`PlayerSlider/Squiggly/Wavy`, placeholders) | Controles y carga | M3 puro | DIRECT | Ninguno | LOW | Ninguna | Mover tal cual | idem |
| UI-04 | `ui/screens/HomeScreen.kt` + `HomeViewModel` | Home (quickPicks/recientes/similares) | Hilt, Room, Innertube, Coil | ADAPT | VM grande | MEDIUM | NET-02, PLY-03, DB-07 | `HomeRepository` común | idem |
| UI-05 | `ui/screens/search/` (4) | Búsqueda local+online | Suggestion VM, player, historial | ADAPT | Estado `savedStateHandle` | LOW–MEDIUM | NET-02, PLY-03 | Repo búsqueda común | idem |
| UI-06 | `ui/screens/library/` (9) | Biblioteca (tabs + grids) | DAO, prefs, SAF-import | ADAPT | Import local | MEDIUM | DB-01/02, DB-04 | `LibraryRepository` Room-KMP | idem |
| UI-07 | `ui/screens/artist/` (4) + `ArtistViewModel` | Artista (subs/likes) | Clipboard/Toast, Innertube, Room | ADAPT | `channelId` subscribe | LOW–MEDIUM | Adapters clipboard/toast | Repo artista común | idem |
| UI-08 | `ui/screens/AlbumScreen.kt` + `album/SpotifyAlbumScreen.kt` | Álbum YT + Spotify | Expressive-M3, GQL Spotify | ADAPT | Versión M3-CMP | MEDIUM | NET-02 (+NET-06 P2) | Fijar M3-CMP con expressive | idem |
| UI-09 | `ui/screens/playlist/` (7) | Playlists (online/local/auto/spotify) | Intent/Bitmap/Uri (Local) | ADAPT/ADAPTER | Picker thumbnail | MEDIUM | `ImagePicker/ShareHandler` | `PlaylistRepository` común | idem |
| UI-10 | `ui/player/{Player,MiniPlayer,Thumbnail}.kt` + `menu/*Menu` | Player full/mini + menús | Media3 estado, Palette, Intent | ANDROID_ONLY/ADAPT | Bloqueado por motor | VERY_HIGH/HIGH | PLY-03, UI-01 | Layouts tras `PlayerController`; menús pronto vía `ShareHandler` | idem (tardío) |
| UI-11 | `ui/screens/settings/` (23) | Ajustes (~17 portables, 6 Android) | Intent/SAF/Alarm/Auto/Updater | ADAPT/ANDROID_ONLY | SAF/ZIP/APK | MEDIUM | DB-04 | Grupo portable tras prefs; resto nativo diferido | idem |
| UI-12 | `ui/screens/{equalizer,wrapped,recognition,podcast}/` + `ListenTogether` + logins | EQ/Wrapped/reconocimiento/podcasts/OAuth | AudioEffect/Services/permisos/CustomTabs | DEFER | Reimplementación nativa | VERY_HIGH | Producto base | Fuera P0/P1 | P2 o nativo |

## Primeros componentes a portar (orden técnico, detal
...[truncated 915 chars]