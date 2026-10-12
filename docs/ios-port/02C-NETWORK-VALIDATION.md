# 02C — Validación de red y búsqueda (Darwin + YouTube Music)

Fecha (UTC): 2026-10-12 · Rama `feat/ios-foundation` · Estado: IMPLEMENTED, PENDING_CI.

## 1. Transporte Ktor/Darwin (PASO D)

`createYouTubeHttpClient()` = mirror documentado de
`innertube/InnerTube.kt:createClient()` (`HttpClient(OkHttp)`):

| Original | iOS 02C | Motivo |
|---|---|---|
| `expectSuccess = false` | se conserva | InnerTubeX valida estados y reintenta transitorios |
| `ContentNegotiation(json{ignoreUnknownKeys, explicitNulls=false, encodeDefaults=true})` | idéntico | InnerTubeX 0.8.4 no lo trae en runtime (solo tests) → lo aporta el host |
| `HttpTimeout` 60 s / 30 s / 60 s | idéntico | mismos presupuestos |
| `defaultRequest`: base `https://music.youtube.com/youtubei/v1/` + `Accept: application/json` + `Cache-Control: no-cache` | idéntico | innertubex postea rutas relativas (`"search"`) |
| Engine OkHttp (pool, HTTP/2, `retryOnConnectionFailure`, caché 50 MB en `java.io.tmpdir`, proxy/auth) | `Darwin` (NSURLSession), sin caché en disco ni proxy | B3: sin OkHttp/`java.*`; sin proxies/credenciales en esta fase (prohibidos) |
| `ContentEncoding` gzip/deflate | omitido | NSURLSession lo negocia solo |
| Cookies/visitorData/login | nada (búsqueda anónima) | sin credenciales por diseño; PoToken queda para P0.3 |

Inyección: `expect fun platformHttpEngine()` (`iosMain` → Darwin,
`jvmMain` → CIO solo host de tests). Sin `java.net`, `Android Context` ni
TLS custom (validación intacta). Ciclo de vida: el cliente vive lo que la
app (`remember` en `MeldApp`); `YouTubeMusicSearch.close()` existe para
tests y cancela el trabajo de InnerTubeX (`close()` solo cancela lo propio,
el `HttpClient` es del host, como documenta upstream).

## 2. Operación de búsqueda (PASO E)

`YouTubeMusicSearch.search(query, FILTER_SONG)` (02C: solo canciones):

1. Blank → `success(SearchResult(emptyList()))` sin emitir red.
2. `innerTube.search(WEB_REMIX, query, params)` → `.body<SearchResponse>()`.
3. Recorrido `tabs[0].sectionListRenderer` (`musicShelfRenderer` con
   `getItems()`/`getContinuation()` + `itemSectionRenderer`), `toYTItem`,
   `distinctBy{id}` — igual que `YouTube.search`.
4. `YouTubeSongSearchRepository` filtra `SongItem` y mapea a `Track`:
   id/title directos; artistas → `ArtistRef(browseId ?: "", name)`; álbum →
   `AlbumRef` o `null` (single); `duration null → -1` (convención 02A);
   `thumbnail → artworkUrl`; `explicit` directo.
5. Fallos (HTTP, timeout, JSON, forma) → `Result.failure`, nunca datos
   falsos; la UI distingue vacío (`EmptyView`) de error (`ErrorView`).
6. `searchContinuation()` portado y testeado (paginación lista para P0.2).

Reintentos NET-12: residen en InnerTubeX 0.8.4 (`executeRequest`: 3 intentos,
transitorios 408/425/429/500/502/503/504, 429 solo con `Retry-After` ≤ 5 s);
no se duplican.

## 3. Interfaz (PASO F)

5ª pestaña **Search** (01–04 intactas, demo). `SearchScreen`:
campo editable + botón + `ImeAction.Search`, carga (`Searching YouTube
Music…`), resultados `CatalogRow` (título, artistas · álbum/Single,
duración, badge E), vacío inicial/sin resultados, error con reintento.
Etiqueta `YouTube Music · live search` vs `Demo catalog` en el resto: sin
mezclar orígenes. Detalle de un resultado vivo reutilizando
`TrackDetailScreen` sobre un `InMemoryMusicCatalogRepository` efímero con
esos tracks (navegación y regreso funcionales; el botón deshabilitado
sigue marcando que no hay reproductor). Sin thumbnails de red (placeholder
local, como 02A); sin botón de play.

## 4. Pruebas (PASO G, deterministas, MockEngine)

Local `:shared:jvmTest` **41/41 PASS** (02A: 18 → 02C: 41; +23):

| Suite | n | Cubre |
|---|---|---|
| `YouTubeMusicSearchTest` | 13 | construcción del cliente; blank sin red (0 llamadas); parseo full (todos los campos incl. explícito) y mínimo (sin álbum, 2:07→127 s); items irreconocibles omitidos; shelf vacío; HTTP 400; JSON inválido; forma errónea; campos futuros ignorados; timeout (100 ms vs 5 s); cancelación (sin resultados); continuación con token y con items vacíos→token null |
| `SongSearchMappingTest` | 8 | mapeo completo; sin álbum→null; sin duración→-1/`--:--`; artista sin id→`""`; sin artistas→`Unknown artist`; explicit; repositorio sobre MockEngine (1 track, 240 s, sin álbum); error 502→failure sin datos falsos |
| `ParseTimeTest` | 2 | `3:45`/`12.34`/`1:02:03`, inválidos→null |
| 02A intactos | 18 | `ModelsTest` 9 + `RepositoryTest` 9, sin cambios |

Sin llamadas reales en CI por diseño. La conexión real la valida el
propietario en iPhone (búsqueda manual); hasta entonces el estado honesto es
`NATIVE BUILD VERIFIED / LIVE SEARCH UNVERIFIED` si CI está verde pero el
device test está pendiente.

## 5. CI y validación nativa (sin cambios de workflow)

Workflow intacto (4 jobs, 13 checks conservados): la compilación del
framework ya valida resolución iOS de InnerTubeX + link; `shared-tests`
ejecuta los 41; `ios-app`/`ios-device-ipa` verifican app/IPA sin firma.
No se añadieron validaciones nuevas: el `BUILD SUCCESSFUL` del framework +
los tests son la evidencia suficiente en esta fase.
