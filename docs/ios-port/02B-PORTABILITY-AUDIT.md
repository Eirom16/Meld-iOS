# 02B — Auditoría de portabilidad Android → iOS (Meld → Meld-iOS)

Fecha (UTC): 2026-10-12 · Rama `feat/ios-foundation` @ `9825cbfe` · Estado: AUDIT COMPLETE / MIGRATION PLAN READY.
Alcance: solo lectura + documentación. Sin cambios funcionales, sin migración de pantallas, sin tocar `app/` ni `innertube/`.

> **02A — DEVICE VERIFIED (2026-10-12, evidencia del propietario):** el IPA 02A se instaló y ejecutó en iPhone 11 Pro.
> Arranque sin cierres, renderizado Compose, navegación Songs/Albums/Artists/Playlists, datos demo visibles,
> pantallas de detalle, navegación de regreso, sin cierres inesperados. Sin reproducción musical ni conectividad real
> (fuera del alcance de 02A por diseño).

## 0. Método y criterio de clasificación (no inventar porcentajes)

Conteos medidos el 2026-10-12 sobre el árbol real (`find` + `grep -rl "^import …"`):

- Total original: **531 `.kt`** (`app/src/main/kotlin` 444 + `innertube/src` 87).
- Con dependencia directa de plataforma (`^import android.` / `androidx.` / `java.` / `javax.`): **377/531 (71,0 %)**.
  Desglose con solape: `android.*` 160 · `androidx.*` 300 · `java./javax.` 153.
- Sin esos imports (candidatos a portabilidad): **154/531 (29,0 %)**. De ellos: 77 usan `kotlinx.serialization`,
  43/43 de `innertube/models/` puros, 1 con Hilt, 4 con `io.ktor`, 0 con Coil.
- UI (`ui/`, 191 `.kt`): solo **7/191 (3,7 %)** sin imports de plataforma. Acoplamientos: Media3 56 ficheros,
  Hilt/Dagger 58, Room (`androidx.room`) 35.

Clasificación funcional usada en todo el informe (por componente, no por fichero):

| Código | Significado |
|---|---|
| PORTABLE_DIRECT | Se mueve a `commonMain` casi tal cual (stdlib + `kotlinx.*` ya KMP). |
| PORTABLE_ADAPT | Lógica reutilizable con cambios limitados (quitar anotaciones, `Long` en vez de `LocalDateTime`, inyección por constructor). |
| REQUIRES_PLATFORM_ADAPTER | Reutilizable tras definir `expect/actual` o interfaz iOS (engine HTTP, clipboard, share, file-picker, color-extractor). |
| ANDROID_ONLY | No reutilizable directamente (Media3/ExoPlayer, Hilt, `WebView`, SAF, `AlarmManager`, Auto). |
| DEFER | Funcional pero no prioritario para el primer producto (ecualizador, Wrapped, recognition, podcasts, ListenTogether). |

Distinción obligatoria del informe: **líneas ≠ archivos ≠ componentes funcionales**.
Los porcentajes de arriba miden **archivos con imports de plataforma** (cota inferior de acoplamiento:
un fichero "limpio" puede seguir dependiendo de Hilt/Ktor/Coil/Room por imports no contados).
La estimación de reutilización funcional está en §9.

## 1. Dominio musical (modelos)

### 1.1 Inventario verificado

| Grupo | Ruta | Nº |
|---|---|---|
| Entidades Room + POJOs | `app/src/main/kotlin/com/metrolist/music/db/entities/` | 33 `.kt` (31/33 con Room/Compose/`java.time`) |
| Raíz DB | `app/src/main/kotlin/com/metrolist/music/db/` (`Converters.kt`, `MusicDatabase.kt` 868 lín., `DatabaseDao.kt` 2196 lín., `daos/SpeedDialDao.kt`) | 4 |
| Modelos de reproductor | `app/src/main/kotlin/com/metrolist/music/models/` (7: `MediaMetadata.kt`, `ItemsPage.kt`, `NewReleaseItem.kt`, `PersistQueue.kt`, `PersistPlayerState.kt`, `SimilarRecommendation.kt`, `SpotifyHomeSection.kt`) | 7 |
| Modelos de red Innertube | `innertube/src/main/kotlin/com/metrolist/innertube/models/` | 43/43 puros (0 con `import android`, 37+ con `kotlinx.serialization`) |
| Provisional 02A | `shared/src/commonMain/kotlin/com/meld/shared/music/` (`Models.kt` 77 lín., `DemoCatalog.kt` 111 lín., `MusicCatalogRepository.kt` 125 lín.) | 3 |

### 1.2 Comparativa 02A vs original (campo por campo, resumida)

`Track` 02A = subconjunto de lectura de `SongEntity` + `Song` + `MediaMetadata` + `SongItem`:
`id/title/artists/album/durationSeconds/artworkUrl/explicit` conservados con renombres
(`duration→durationSeconds`, `thumbnail→artworkUrl`) y convención `-1` = duración desconocida (idéntica al original).
Ausentes en 02A (deliberado, fase demo): `liked/likedDate/inLibrary/libraryTokens/setVideoId/isrc/totalPlayTime/year/
isLocal/localPath/playbackPosition/isCached/format`.
`Artist`/`Album`/`Playlist` 02A conservan identidad+nombre+artwork y relaciones por `trackIds`;
el original usa relaciones N:M (`SongArtistMap`, `SongAlbumMap`, `AlbumArtistMap`, `PlaylistSongMap` con `position`)
más estado (`bookmarkedAt`, `channelId`, `spotifyId`, `browseId`, `isEditable`, `endpointParams`, `cachedPageJson`).

### 1.3 Fichas de reutilización

- `Song.kt` / `SongEntity.kt` / `models/MediaMetadata.kt`: **ANDROID_ONLY tal cual** (Room + `compose.runtime.Immutable` +
  `java.time.LocalDateTime` + `java.io.Serializable` + side-effects de red `YouTube.*` dentro de `toggleLike/toggleLibrary`).
  Reutilizable la **forma** (campos + convención `-1`), no el fichero. No copiar; mapear en capa de datos futura.
- `LocalItem.kt` (sealed `id/title/thumbnailUrl`, 0 imports): **PORTABLE_DIRECT**. Candidato a moverse tal cual.
- `ArtistPageCache.kt` (solo `kotlinx.serialization`): **PORTABLE_DIRECT**.
- `YTItem.kt` y `models/` de Innertube (`SongItem/AlbumItem/PlaylistItem/ArtistItem/Album/Artist`): **PORTABLE_DIRECT**,
  mejores candidatos a primera reutilización real en `commonMain`.
- `PlaylistEntity` (prefijos `LP_*`, `generatePlaylistId()`), `ArtistEntity.generateLocalId` (`LA`): idea **PORTABLE_ADAPT**
  (convención de prefijos reutilizable; fichero no, por Room).
- `LyricsEntity` / `FormatEntity`: campos **PORTABLE_ADAPT** (quitar `@Entity`, poner `@Serializable`).
- Historial (`SearchHistory`, `Event`, `RecognitionHistory`): **PORTABLE_ADAPT** como
  `SearchQuery` / `PlayEvent(songId, timestampEpochMs, playTimeMs)` / `RecognitionEntry` con `Long` en vez de `LocalDateTime`.
- Ajustes musicales (`music/constants/PreferenceKeys.kt`, 804 lín., `datastore.preferences.core`): **REQUIRES_PLATFORM_ADAPTER**
  (definir `MusicPreferences` puro en común + implementación DataStore/Keychain por plataforma).

## 2. Interfaz Compose (191 `.kt` en `ui/`)

Inventario por subcarpeta (conteo real): `screens/` raíz 17 · `library/` 9 · `playlist/` 7 · `search/` 4 · `artist/` 4 ·
`settings/`+`integrations/` 23 · `wrapped/` 22 · `equalizer/` 7 · `album/podcast/recognition` 4 · `component/`+`shimmer/` 51 ·
`menu/` 23 · `player/` 6 · `theme/` 5 · `utils/` 9 · `dialog/` 1. ViewModels: 34 en `viewmodels/`, todos `@HiltViewModel`;
46 ficheros UI usan `hiltViewModel()`; ~20 usan Coil con `LocalContext`.

| Pantalla / familia (raíz `ui/`) | Clasificación | Bloqueo real (aislable por Regla 3 salvo indicación) |
|---|---|---|
| Navegación (`screens/Screens.kt`, `screens/NavigationBuilder.kt` 57 destinos, `component/AppNavigation.kt`, `utils/NavControllerUtils.kt`) | REQUIRES_PLATFORM_ADAPTER (`AppNavigation` casi DIRECT) | `R.string/drawable` + `activity: Activity` + `navigation-compose` stock → modelo de rutas común + `org.jetbrains.androidx.navigation` o `Router` propio + VM sin Hilt. `BackHandler` y `collectAsStateWithLifecycle` **sí son CMP** (falsos positivos habituales). |
| Tema (`theme/Theme.kt`, `PlayerColorExtractor.kt`, `Font.kt`) | REQUIRES_PLATFORM_ADAPTER | `Bitmap` + `Palette` + `dynamic*ColorScheme(context)` → `expect ColorExtractor / SystemDynamicColors` (fallback: `rememberDynamicColorScheme(seed)` común). Migrar **antes** que cualquier pantalla. |
| Home (`screens/HomeScreen.kt` 665 lín.) | PORTABLE_ADAPT | `hiltViewModel` + 3 CompositionLocals + Coil → `HomeRepository` común + `PlayerController` mínimo + Coil3-CMP. |
| Search (`search/` 4) | PORTABLE_ADAPT (contenedor casi DIRECT) | Igual trío + `savedStateHandle` → `rememberSaveable`/router-state. |
| Library (`library/` 9) | PORTABLE_ADAPT | Room-DAO + prefs + `FilePicker expect/actual` (aísla `Uri`/launchers/`Toast`). |
| Artist (`artist/` 4) | PORTABLE_ADAPT | Solo 2 APIs plataforma (clipboard + toast) → adapters triviales. Caso de libro de la Regla 3. |
| Album (`screens/AlbumScreen.kt` + `album/SpotifyAlbumScreen.kt`) | PORTABLE_ADAPT | Igual + fijar versión M3-CMP con `ExperimentalMaterial3ExpressiveApi`. |
| Playlist (`playlist/` 7) | ADAPT (Online/Auto/Cache/Top/Spotify) / ADAPTER (Local) | Local añade `Intent` + `Bitmap` + picker → `ImagePicker` + `ShareHandler expect/actual`. |
| Queue + SongMenu (`player/Queue.kt` 1348 lín., `menu/SongMenu.kt`) | REQUIRES_PLATFORM_ADAPTER | Estado 100 % Media3 (`Timeline`, `moveItem`) → interfaz `QueueController` antes de la UI. |
| Player + MiniPlayer (`player/Player.kt` 2177 lín., `MiniPlayer.kt` 1131 lín., `Thumbnail.kt`) | ANDROID_ONLY (núcleo) / DEFER (pantalla) | Mayor superficie Android (`WindowManager`, `Palette`, `Intent`, letras, EQ). Último en migrar. |
| Menús (`menu/` 23) | PORTABLE_ADAPT homogéneo | Un `ShareHandler.shareText()` + `Notifier` + callbacks elimina todo `android.*`. Migrar pronto (desbloquea "⋮"). |
| Settings (23) | ADAPT ~17 / ANDROID_ONLY (`AndroidAuto`, `Updater` APK, `Alarm`, `Storage`, `BackupAndRestore` SAF) | `PreferencesRepository` común (DataStore-KMP) + `SystemSettingsOpener` + `FilePicker`. |
| Componentes (`component/Items.kt`, sliders, shimmer, headers) | ADAPT (`Items.kt`) / DIRECT (sliders/headers/shimmer) | `Items.kt` parametrizado (`artworkUrl`, callbacks) + Coil3-CMP = el archivo con más reutilización por línea. |
| Equalizer / Wrapped / Recognition / CrashActivity / ListenTogether / Podcasts / Login OAuth | ANDROID_ONLY / DEFER | Reimplementación nativa (AudioUnit, `AVAudioSession`, `ASWebAuthenticationSession`). Fuera del primer producto. |

El provisional 02A (`MeldApp.kt` 516 lín. + `CatalogComponents.kt` 189 lín.) cubre el esqueleto de catálogo
(tabs + listas + 4 detalles + estados loading/vacío/error) con estado `remember+LaunchedEffect`.
No cubre: player, imágenes de red, menús, búsqueda, settings, NavController, theming dinámico. Válido como 02A;
el salto es inyectar repos reales + adapters.

## 3. InnerTube / InnerTubeX (87 `.kt` en `innertube/` + `InnerTubeXPlayer.kt`)

Estructura verificada: `InnerTube.kt` 455 lín. (fachada) · `YouTube.kt` 3477 lín., 65 `suspend fun` (catálogo sin red propia) ·
`models/` 43 · `pages/` 22 · `models/response/` 13 · `utils/` 2 · `InnerTubeXPlayer.kt` 374 lín. (única extracción audio).

- Motor fijado: `HttpClient(OkHttp)` en `InnerTube.kt:12,139` (config `okhttp3.*` en `:158-177`) y doble
  `HttpClient(OkHttp)` en `spotify/Spotify.kt:84,100-101`. Imports JVM en fachada: `java.io`, `java.net.Proxy`,
  `java.util.*`, `io.ktor.utils.io.jvm.javaio` (`InnerTube.kt:26,32-37`).
- Serialización `kotlinx.serialization` + `ContentNegotiation.json` (`ignoreUnknownKeys`, `explicitNulls=false`): portable.
- Cookies/auth delegadas a InnerTubeX (passthrough `locale/visitorData/dataSyncId/authUser/cookie/proxy/useLoginForBrowse`,
  `InnerTube.kt:56-126`). Cipher/firma/poToken: nada en `innertube/`; todo en el binario + `InnerTubeXPlayer`.
- `InnerTubeXPlayer` (app): ANDROID_ONLY (`Context`+`SharedPreferences`, `ConnectivityManager`, `ConcurrentHashMap`,
  `PoTokenWebView` = `WebView`+`WebChromeClient`+`Handler/Looper`). Portable: política
  (`allowHls/sabr=false`, TTL 5 min, `excludedClients`) y mapeo `ExtractedStream→PlaybackData` (`:294-373`).

Por áreas: **A) búsqueda/catálogo y B) metadatos: reutilización alta** (parsing `@Serializable` puro; solo falta transporte).
**C) extracción audio: reutilización cero sin port** (cipher `YouTubeCipherService`, `YtConfigParserImpl`,
`RemotePlayerConfigStore`, `InnerTubeExtractor` viven en el binario). **D) auth: modelo sesión portable** (`String?`/`Boolean`;
proxy requiere `expect/actual`). **E) integración player: contrato `PlaybackData` portable** (URL + headers + rangos +
expiración); consumidores actuales Media3.

### InnerTubeX y Kotlin/Native — evidencia (conclusión rectificada respecto a Fase 00)

- Meld fija `innertubex = "v0.5.2"` (`libs.versions.toml:36,107`; repos JitPack en `settings.gradle.kts:20-31`,
  sin artefacto `-ios` referenciado; `innertube/build.gradle.kts` es `com.android.library` puro).
- **Evidencia nueva (2026-10-12, fuente upstream):** el README actual de `MetrolistGroup/innertubex` lo describe como
  "extended Android, JVM, and **iOS Kotlin Multiplatform** client"; su `build.gradle.kts` (main) declara
  `kotlin("multiplatform")` con targets **`iosArm64()` + `iosSimulatorArm64()`**, `commonMain` con
  `ktor-client-core` + `kotlinx.serialization.json` + `coroutines-core` + QuickJS, y `nativeMain` con EJS embebido.
  El CHANGELOG fija el hito: **[0.6.0] (2026-09-07) — "Add iOS device and simulator targets** with native locale,
  hashing, and EJS implementations". Versión actual 0.8.4; Meld usa 0.5.2 (2026-09-02, anterior a iOS).
- Conclusión: el bloqueo B2 de Fase 00 ("sin artefacto iOS") **tiene vía de salida verificada: subir
  `innertubex` 0.5.2 → ≥0.6.0 (recomendado 0.8.x)**, consumido con coordenadas KMP variant-aware de JitPack
  (el propio CHANGELOG 0.1.2 advierte de la coordenada agregada vs variante).
  Hipótesis pendientes: confirmar publicación JitPack de la variante iOS, API `TokenProvider` vigente
  (minting de PoToken sigue siendo responsabilidad del host: en iOS, WKWebView+bridge o estrategia sin poToken),
  y coste QuickJS nativo en memoria/arranque. Búsqueda (catálogo) ≠ extracción (audio): validar ambas por separado.

### Spotify (referencia, `spotify/Spotify.kt` 1590 lín.)

Doble `HttpClient(OkHttp)` (REST + GQL `api-partner.spotify.com`), 25 `suspend fun` / 14 `operationName`,
fallback `hash→previous_hash` ante 412, reintentos 429. Parsing `JsonObject` manual portable; deps JVM mínimas
(`TimeUnit`, `TimeZone`). Camino Darwin idéntico al de InnerTube. No prioritario (P2).

## 4. Reproductor (Android)

`MusicService.kt`: **6135 líneas**, `MediaLibraryService + Player.Listener`, 24 imports `android.*` + 62 `androidx.*`
(medición estricta `^import`; Fase 00 contó 86+62 con criterio más amplio — la magnitud es la misma:
el fichero más grande y acoplado del proyecto).

| Capa | Clasificación | Destino iOS |
|---|---|---|
| Reglas portables: colas (`playback/queues/` — `Queue.kt` contrato + `Status` + filtros explicit/video, 10 impls), shuffle/repeat/seek (claves `RememberShuffleAndRepeat`, `SHUFFLE_PRELOAD_MAX_ITEMS=500`), taxonomía de errores (`PlaybackError.kt`), calidad audio (`AudioQuality AUTO/LOW/HIGH`), temporizador/dormir | PORTABLE_ADAPT (contratos a `shared`, desacoplando `MediaItem`/`Timeline`/`PlaybackException`) | `shared/commonMain` |
| Motor: construcción ExoPlayer (`ExoPlayer.Builder`, `LoadControl`, `AudioAttributes`, DSP `VolumeNormalization/SilenceDetector/Sonic`), `CacheDataSource/SimpleCache/ResolvingDataSource`, `DownloadManager/DownloadService`, `MediaLibrarySessionCallback` (1250 lín., comandos custom) | ANDROID_ONLY | Reescribir: `AVPlayer`/`AVQueuePlayer` + `AVAssetResourceLoaderDelegate` + `URLSession` background + `MTAudioProcessingTap`/`AVAudioUnitTimePitch` |
| Sistema: foreground service + `mediaPlayback`, foco audio (LOSS/DUCK), `AudioBecomingNoisy`, bluetooth, alarmas (`playback/alarm/` 4 ficheros + `BOOT_COMPLETED`), notificación, Cast (solo flavor `gms`) | ANDROID_ONLY (servicios) | `AVAudioSession(.playback)` + interrupciones/route-change + `BGTaskScheduler` + `MPRemoteCommandCenter`/`MPNowPlayingInfoCenter` (Auto→CarPlay, Cast→AirPlay opcional) |
| `PlayerConnection.kt` (StateFlows `isEffectivelyPlaying/mediaMetadata/queueWindows/error`) | REQUIRES_PLATFORM_ADAPTER | Interponer interfaz `PlayerController` + replicar sus StateFlows; hoy expone `ExoPlayer` crudo — **prerrequisito de Queue/Player/MiniPlayer**. |

## 5. Persistencia y dependencias

| Área (versión declarada) | Veredicto iOS |
|---|---|
| Room 2.8.4 (`app/build.gradle.kts:311-314`; DB v41, 20 entidades + 3 vistas, `exportSchema=true`, AutoMigrations 2→40; `DatabaseDao` 2196 lín./~260 fns/194 `@Query`) | Sustituto **Room KMP** (mismo grupo, `sqlite-bundled` + driver nativo). Esfuerzo VERY_HIGH. **Esquema intocable**: replicar tablas/columnas exactas. |
| DataStore 1.2.1 (`utils/DataStore.kt` 191 lín., `Context.dataStore`, ~500 usos) | Sustituto `datastore-preferences-core` (sin `Context`, `dataStoreFile()` expect/actual). MEDIUM. |
| Hilt 2.60.1 (5 ficheros `di/`, 58 con Dagger/Hilt) | Sin Native → Koin o manual (constructor injection). VERY_HIGH. |
| Navegación (77 ficheros `androidx.navigation`; `MainActivity` 1700+ lín.) | `org.jetbrains.androidx.navigation` o `Router` propio. VERY_HIGH. Nada declarado en `shared/` (solo runtime/foundation/ui/material3). |
| kotlinx.serialization (47 ficheros) | KMP nativo. LOW. |
| Ktor 3.5.2 (core/okhttp/cio/negotiation/encoding/json/mock; 15 ficheros `io.ktor`) | Core ya común; `okhttp/cio` → **`ktor-client-darwin`** vía `expect/actual HttpClientFactory`. `okhttp3.*` directos se aíslan. MEDIUM. |
| Coroutines 1.11.0 (194 ficheros; `coroutines-guava` solo para `ListenableFuture<MediaController>`) | Core KMP (ya en `shared` tests). Guava se elimina. LOW. |
| Coil 3.6.0 (49 ficheros; `coil-compose` + `coil-network-okhttp`) | Coil3 es KMP: `coil-compose` + `coil-network-ktor` + `PlatformContext` iOS. LOW–MEDIUM. |
| Activity/Lifecycle/ViewModel/Material3 | Artefactos CMP existentes (`composeMultiplatform 1.12.1` ya en `shared`). `appcompat/palette/browser` → sustitutos. MEDIUM–HIGH. |
| Media3 1.10.1 | Sin KMP (§4). VERY_HIGH. |

## 6. Mapa de dependencias (resumen; detalle en `02B-DEPENDENCY-MAP.md`)

`MainActivity → NavigationBuilder → {Home,Search,Library,Artist,Album,Playlist} → {ViewModel(Hilt) → YouTube/Innertube(OkHttp) / Room DAO / PlayerConnection→MusicService(ExoPlayer)}`,
con transversales `DataStore(prefs)`, `Coil(imagen)`, `Theme(Palette)`, `DownloadUtil(cache)`.
Nodos raíz del port (sin antecesores): modelos puros → `HttpClientFactory` → `HomeRepository/SearchRepository` →
`PreferencesRepository` → `ColorExtractor` → `PlayerController` → UI.

## 7. Inventario de reutilización (resumen; completo en `02B-REUSE-INVENTORY.md`)

~70 componentes inventariados con ID/ruta/clasificación/riesgo/esfuerzo/estrategia/destino.
Distribución funcional: DIRECT ≈ modelos red + utils puros · ADAPT ≈ pantallas de catálogo + reglas player + prefs ·
ADAPTER ≈ navegación/tema/imagen/ficheros/share/clipboard/HTTP · ANDROID_ONLY ≈ motor player + Hilt + servicios ·
DEFER ≈ EQ/Wrapped/recognition/podcasts/ListenTogether/OAuth.

## 8. Plan de migración (resumen; completo en `02B-MIGRATION-ROADMAP.md`)

- **P0 (primera app utilizable):** `HttpClientFactory(Darwin)` + `innertubex ≥0.6.0` + `models/pages` en común →
  búsqueda + `PlayerController(AVPlayer)` + `PlaybackData` → primera canción audible. Fases pequeñas con IPA cada una.
- **P1 (esencia Meld):** biblioteca Room-KMP, favoritos/playlists, álbumes/artistas, cola, controles, menús, settings portables.
- **P2 (avanzado):** descargas, caché, letras, Spotify, sincronización, integraciones — según viabilidad.

## 9. Estimación de reutilización (honesta, con método)

Base: 531 archivos. Medida de acoplamiento directo: 377 archivos (71 %) con imports android/androidx/java/javax;
154 (29 %) sin ellos, de los cuales ~77 son modelos `@Serializable` puros o utils portables.

Traducción a **componentes funcionales** (juicio técnico fundado en §§1–5, no conteo ciego):

- **Reutilizable casi directo (DIRECT): ~15 %** — `innertube/models` + `pages` (parsing), `LocalItem`, `ArtistPageCache`,
  modelos kugou/lastfm/lrclib/betterlyrics, extensiones y utils puros, contratos `Queue/Status`, calidades, errores.
- **Adaptable con cambios limitados (ADAPT): ~35 %** — pantallas Home/Search/Library/Artist/Album/Playlist-online,
  menús, componentes `Items.kt`, reglas shuffle/repeat/cola, entidades Room (campos), DAOs (consultas como especificación),
  Spotify-GQL (parsing), DataStore (claves).
- **Requiere adaptador iOS (ADAPTER): ~20 %** — navegación, tema dinámico, Coil, Ktor-engine, InnerTube-transporte,
  share/clipboard/pickers/settings-sistema, `PlayerConnection→PlayerController`.
- **Android-only / reescribir (incl. motor player, Hilt, servicios): ~25 %**.
- **Diferido (DEFER): ~5 %**.

Condiciones: la estimación funcional **depende de subir `innertubex` a ≥0.6.0** (sin ello, catálogo+extracción caen a
ANDROID_ONLY y lo reutilizable directo baja a ~10 %). No se cuentan líneas (las 6135 de `MusicService` sesgarían todo)
ni se dan horas/días. Verificación pendiente: variante iOS de JitPack, PoToken en iOS, QuickJS nativo, Room-KMP AutoMigrations.

## 10. Riesgos principales

1. `innertubex` 0.5.2→0.8.x: cambios API (p.ej. `ExtractionCipherService` pública en 0.8.0, firma JVM de `InnerTubeExtractor`),
   coordenada KMP variant-aware, publicación JitPack de la variante iOS. Mitigación: spike P0.1 dedicado.
2. PoToken/BotGuard en iOS (WebView→WKWebView o clientes sin poToken). Sin esto, parte del catálogo protegido no reproduce.
3. Cipher/JS-player rotante (dependencia de `RemotePlayerConfigStore` + EJS/QuickJS nativo).
4. `!!` en `YouTube.album/transcript` ante cambios de forma de YT.
5. Room-KMP con v41 + AutoMigrations; `cookie`/SAPISID en logs.
6. M3-Expressive (`ContainedLoadingIndicator`) exige fijar versión CMP.

## 11. Experimentos realizados (fase de investigación)

Ningún cambio funcional. Solo lectura + conteos `find/grep/wc` + `webfetch` del upstream `innertubex`
(README + `build.gradle.kts` + CHANGELOG, evidencia §3). Sin spikes de compilación (los candidatos —
variante iOS de JitPack, `ktor-client-darwin`, Room-KMP — quedan como trabajo futuro P0.1/P1.1 documentado).
`app/` e `innertube/` intactos; UI 02A intacta.

## 12. Decisiones pendientes (requieren propietario)

1. Subir `innertubex` a 0.8.x (¿en `app/`+`innertube/` a la vez? ¿quién valida regresión Android si Meld-iOS es iOS-only?).
2. Estrategia PoToken iOS (WKWebView vs sin-poToken vs TV-discovery con credencial del host).
3. Room-KMP vs SwiftData/grado de fidelidad del esquema v41.
4. DI: Koin vs manual.
5. Navegación: `org.jetbrains.androidx.navigation` vs `Router` propio.
6. Spotify en iOS (P2): ¿alcance y credenciales?
7. Orden P0: ¿búsqueda+playback primero (recomendado) o biblioteca local primero?
