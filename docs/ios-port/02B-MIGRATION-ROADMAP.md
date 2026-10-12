# 02B — Roadmap de migración (por dependencias reales, en verticales pequeñas)

Fecha (UTC): 2026-10-12 · Rama `feat/ios-foundation` @ `9825cbfe` · Estado: PLAN READY (sin implementar).
Principio: cada fase entrega funcionalidad visible + IPA comprobable en iPhone, reutilizando Meld
(Reglas 1–10 en `02B-PORTABILITY-AUDIT.md` §12 del brief / AUDIT §11). Orden justificado por el mapa de dependencias.

## P0 — Primera app realmente utilizable (buscar + explorar + reproducir)

| Fase | Visible para el propietario | Código original portado | Adaptadores iOS | Criterios de aceptación | Riesgos / IPA |
|---|---|---|---|---|---|
| P0.1 Transporte + InnerTubeX iOS | Ninguno visible (base) | `innertube/models+pages` (DOM-01–03) a común | Bump `innertubex` 0.5.2→0.8.x KMP; `HttpClientFactory(Darwin)`;JUnit→`commonTest` con fixtures | `commonTest` parsing verde en CI; `search()` real devuelve resultados en test instrumentado | Variante iOS en JitPack; API 0.8.x. IPA: no (sin UI) |
| P0.2 Búsqueda y exploración | Buscar canciones/artistas/álbumes reales; ver detalle | `YouTube.search/artist/album/playlist` (NET-02) + `SearchScreen/OnlineSearchResult/ArtistScreen/AlbumScreen` layouts (UI-05/07/08) | `HomeRepository/SearchRepository` común; Coil3-CMP | Buscar "demo"→resultados YT reales en iPhone; detalle abre sin cierres | PoToken solo si YT lo exige para browse. IPA: **sí (catálogo real, sin playback)** |
| P0.3 Primer reproductor funcional | Play/pausa/seek en una canción real + miniplayer | `PlaybackData` (PLY-11), reglas `Queue/Status` (PLY-01), calidades (PLY-08), `Queue.kt` estado (UI) | `PlayerController` + backend `AVQueuePlayer` + `AVAudioSession` + `TokenProvider` iOS (D2) | Una canción suena de principio a fin; pausa/seek; sobrevive a expiración 5 min (refresco) | Cipher/PoToken/403/SABR. IPA: **sí (hito P0)** |
| P0.4 Controles esenciales | Cola, shuffle/repeat, error visible, artwork, menús ⋮ | `PlayerConnection` StateFlows (PLY-03), `PlaybackError` (PLY-04), `Items.kt` (UI-02), menús (UI-10 parcial) | `MPRemoteCommandCenter` básico; `ColorExtractor`; `ShareHandler/Notifier` | Cola reordena; shuffle/repeat persisten; error 403/offline muestra guía; artwork carga | QuickJS nativo memoria. IPA: **sí** |

Salida P0: buscar + reproducir + cola + controles básicos con código original.

## P1 — Experiencia esencial de Meld

| Fase | Visible | Código original | Adaptadores | Aceptación | IPA |
|---|---|---|---|---|---|
| P1.1 Biblioteca local | Songs/Albums/Artists/Playlists locales persistentes | `MusicDatabase` esquema v41 (DB-01), DAOs por familias (DB-02), entidades (DOM-06–09) | Room-KMP + driver nativo | Reiniciar conserva biblioteca; 194 queries críticas verdes | AutoMigration 40→41; tamaño binario. **sí** |
| P1.2 Favoritos + playlists | Like, biblioteca, crear/editar/reordenar playlists | `toggleLike/toggleLibrary` como casos de uso (no en entidad), `PlaylistSongMap`, `Online/LocalPlaylistScreen` (UI-09) | `ImagePicker` (thumbnails), `FilePicker` | Like sincroniza con YT; playlist local reordena y persiste | Mutaciones con sesión explícita. **sí** |
| P1.3 Home + historial + stats | Home real, historial, stats | `HomeScreen/HomeViewModel` (UI-04), `Event/SearchHistory` (DOM-12) | `HomeRepository` completo | Home carga secciones; historial registra plays | Rate-limit `resolveArtistIds`. **sí** |
| P1.4 Ajustes portables | Appearance/Player/Content/Privacy/Theme (17 pantallas) | `PreferenceKeys` como spec (DOM-13), pantallas settings (UI-11 parcial) | `datastore-preferences-core`, `SystemSettingsOpener` | Cambiar tema/calidad/filtros surte efecto y persiste | ~500 usos DataStore. **sí** |
| P1.5 Navegación completa | Grafo completo + bottom bar + back | `Screens/NavigationBuilder` (DB-06), `AppNavigation` | Router/navigation-CMP | Back + deep-links básicos sin cierres | Migración 57 destinos. **sí** |

## P2 — Funciones avanzadas (según viabilidad)

Descargas offline (`DownloadUtil`, URLSession background), cachéBounded (`StreamUrlCache`),
letras (LrcLib/KuGou/BetterLyrics + `timedLyrics`), Spotify (NET-06/07, decisión D6),
sincronización YTM, ListenTogether, podcasts, ecualizador (nativo AudioUnit), Wrapped/recognition
(nativo), AndroidAuto→CarPlay, Updater→TestFlight/sideload. Cada una con spike previo y decisión del propietario.

## Orden alternativo considerado y descartado

"Biblioteca local primero" se descarta: Room-KMP (VERY_HIGH) tardaría más en dar feedback audible que
transporte+player, y la biblioteca sin contenido online aporta poco en iOS (sin importación Android).
El orden P0→P1 maximiza funcionalidad verificable por fase con el mínimo prerrequisito.

## Matriz fase → IPA

P0.2 (catálogo) · P0.3 (hito audible) · P0.4 · P1.1 · P1.2 · P1.3 · P1.4 · P1.5: cada una con IPA firmable y smoke test
(arranque + función nueva + regreso + sin cierres), continuando la práctica 01C/01D/02A.
