# 02B — Mapa de dependencias (Android original → objetivos iOS)

Fecha (UTC): 2026-10-12 · Rama `feat/ios-foundation` @ `9825cbfe`.
Convención: `A → B` = "A depende de B". Los nodos raíz del port son los que no tienen antecesores iOS.

## 1. Grafo de alto nivel (original)

```text
MainActivity (1700+ lín., FragmentActivity, @AndroidEntryPoint)
├── NavigationBuilder (57 destinos, necesita Activity + NavHostController)
│   ├── Home/Search/Library/Artist/Album/Playlist ← ViewModel (@HiltViewModel) ← YouTube/Innertube (OkHttp) + Room DAO
│   ├── Player/MiniPlayer/Queue ← PlayerConnection ← MusicService (ExoPlayer 6135 lín.)
│   └── Settings ← DataStore (Context) + Intent/SAF/Alarm/Auto
├── CompositionLocals: LocalDatabase (Room) · LocalPlayerConnection (Media3) · LocalNavController · LocalDownloadUtil
└── Transversales: Theme (Palette/Bitmap) · Coil (AsyncImage+OkHttp) · Hilt DI · Timber
```

## 2. Nodos críticos (fan-in alto: muchas pantallas dependen de ellos)

```text
PlayerConnection ── usado por Player, MiniPlayer, Queue, SongMenu, Home, menús
YouTube/Innertube ─ usado por todos los ViewModels online + InnerTubeXPlayer → MusicService/DownloadUtil
MusicDatabase/DAO ─ usado por Library, Artist, Album, Playlist-local, menús, stats
DataStore/prefs ── usado por ~500 llamadas + Settings + colas (filtros) + calidad
Coil ──────────── usado por ~49 ficheros UI + precarga artwork
Theme/Palette ─── usado por TODA la UI (MetrolistTheme)
```

Regla de orden: un componente solo se porta cuando sus dependencias están portadas o abstraídas.
Por eso el orden real es: modelos puros → HTTP → repos → prefs → tema/imagen → PlayerController → pantallas.

## 3. Capa de red (detalle)

```text
SearchScreen/HomeScreen/…ViewModel
└── YouTube (object, 65 suspend fun, sin red propia)
    └── InnerTube (fachada: HttpClient(OkHttp) + okhttp3.Cache + java.net.Proxy + jvm.javaio)
        └── innertubex:v0.5.2 (binario: InnerTubeX, cipher, extractor, YTClient/Y TLocale)
            ├── Cipher: PlayerConfigRepository → RemotePlayerConfigStore → YouTubeCipherService (QuickJS)
            ├── Parser: YtConfigParserImpl (watch HTML) + TokenProvider (PoToken ← PoTokenWebView[WebView])
            └── Extractor: InnerTubeExtractor → ExtractedStream → toPlaybackData → PlaybackData
                ├── MusicService/ExoPlayer (streaming)   [ANDROID]
                └── DownloadUtil (offline)               [ANDROID]
Spotify pantallas ── Spotify.kt (HttpClient(OkHttp)×2, 14 ops GQL, hashes) ── SpotifyAuth (JVM)
```

Objetivo iOS:

```text
SearchRepository/HomeRepository (común, Result<T>)
└── YouTube portado (común, models+pages movidos)
    └── InnerTube portado (común, HttpClientFactory expect/actual → Darwin)
        └── innertubex ≥0.6.0 KMP (iosArm64/iosSimulatorArm64, variant-aware JitPack)
            └── TokenProvider iOS (iosMain: WKWebView-bridge o sin-poToken) [DECISIÓN D2]
                └── PlaybackData (común) → PlayerController (AVPlayer, iosMain)
```

## 4. Capa de reproductor (detalle)

```text
Player/MiniPlayer/Queue/Thumbnail
└── PlayerConnection (StateFlows; expone ExoPlayer crudo)
    └── MusicService (MediaLibraryService)
        ├── ExoPlayer.Builder + LoadControl + AudioAttributes + DSP (VolumeNorm/Silence/Sonic)
        ├── Queue impls (List/YouTube*/Spotify*/LocalAlbumRadio) sobre MediaItem/Timeline
        ├── CacheDataSource/SimpleCache/ResolvingDataSource + StreamUrlCache + DownloadUtil
        ├── MediaLibrarySessionCallback (comandos custom like/repeat/shuffle/radio)
        └── Sistema: foreground+mediaPlayback, AudioFocus, noisy/bluetooth, alarmas, notificación, Cast(gms)
```

Objetivo iOS:

```text
PlayerScreen/MiniPlayer/QueueScreen (común, tardíos)
└── PlayerController (interfaz común: play/pause/seek/queue/move/shuffle/repeat + StateFlows)
    └── AVPlayerBackend (iosMain: AVQueuePlayer + AVAudioSession + MPRemoteCommandCenter/NowPlaying)
        ├── CommonQueue/Status + reglas shuffle/repeat (común, de queues/ + MusicService)
        ├── PlaybackData + StreamUrlCache desacoplado (común)
        └── Descargas URLSession + Room isCached (P2)
```

## 5. Capa de datos (detalle)

```text
Library/Artist/Album/Playlist-local/menús/stats
└── DatabaseDao (194 @Query) + SpeedDialDao
    └── MusicDatabase v41 (20 entidades + 3 vistas, AutoMigration 2→40, Converters LocalDateTime)
Settings/colas/calidad ← DataStore (Context.preferencesDataStore, snapshot, runBlocking)
```

Objetivo: Room-KMP con esquema idéntico (mismo `song.db`, mismo `user_version`) + `datastore-preferences-core`
con `dataStoreFile()` expect/actual. DAOs por familias. `Converters` → epoch `Long`.

## 6. Transversales

```text
TODA la UI ← MetrolistTheme ← PlayerColorExtractor (Palette+Bitmap) + dynamic*ColorScheme(Context)
49 ficheros ← Coil (coil-compose + coil-network-okhttp + SingletonImageLoader en App.kt)
34 ViewModels ← Hilt (@HiltViewModel/hiltViewModel, AppModule/NetworkModule/WrappedModule/Qualifiers)
77 ficheros ← navigation-compose stock (+ R.string/drawable en Screens.kt)
```

Objetivos: `expect ColorExtractor/SystemDynamicColors` · `coil-compose+coil-network-ktor+PlatformContext` ·
Koin/manual · Router propio o navigation-CMP con rutas sin `R.*`.

## 7. Puertos de plataforma (tabla expect/actual e interfaces)

| Puerto | Común | Android (referencia) | iOS (nuevo) | Desbloquea |
|---|---|---|---|---|
| `HttpClientFactory` | `expect fun httpClient()` | `HttpClient(OkHttp)` (existe) | `HttpClient(Darwin)` + timeouts | NET-01/02/06, P0 |
| `TokenProvider` (PoToken) | interfaz iTX | `PoTokenWebView` (existe) | WKWebView-bridge o stub | NET-04/05, P0.3 |
| `PlayerController` | interfaz + StateFlows | adaptador ExoPlayer (no hay; crear fino) | `AVQueuePlayer` backend | PLY-03, P0.3, UI-10 |
| `ColorExtractor` | `expect fun extractSeed(bitmap)` | Palette (existe) | paleta propia / seed directo | UI-01, P0.4 |
| `ImageLoader` | Coil3-CMP | `coil-network-okhttp` (existe) | `coil-network-ktor` | DB-07, P0.4 |
| `ShareHandler/Notifier/Clipboard` | interfaces | Intent/Toast/ClipboardManager | `UIActivityViewController`/toast/clipboard | Menús, UI-07/08/09 |
| `FilePicker/ImagePicker` | interfaces | SAF/PickVisualMedia | `UIDocumentPicker/UIImagePicker` | UI-06/09/11 |
| `PreferencesStore` | `datastore-preferences-core` | `Context.dataStore` (existe) | `dataStoreFile()` + Keychain donde aplique | DB-04, P1.4 |
| `RoomDatabase` | Room-KMP | `room-runtime` (existe) | `sqlite-bundled` + driver nativo | DB-01/02, P1.1 |
| `Router` | rutas tipadas sin `R.*` | navigation-compose (existe) | mismo común | DB-06, P1 |
| `SystemSettingsOpener` | interfaz | `Settings.ACTION_*` | `UIApplication.openSettingsURLString` | UI-11 |
