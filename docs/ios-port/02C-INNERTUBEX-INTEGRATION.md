# 02C — Integración de InnerTubeX (iOS) + primera búsqueda real

Fecha (UTC): 2026-10-12 · Rama `feat/ios-foundation` · Estado: IMPLEMENTED, PENDING_CI (ciclo 1/3).
Alcance: motor InnerTubeX en `shared/`, transporte Darwin, búsqueda de
canciones en YouTube Music + UI mínima. Sin reproducción, sin auth, sin
biblioteca local. `app/` e `innertube/` intactos (referencia).

## 1. Versión y coordenada (PASO A verificado, no asumido)

- Versión: **v0.8.4** (08-oct-2026, latest upstream).
- Coordenada: `com.github.MetrolistGroup.innertubex:innertubex:v0.8.4`
  (JitPack, variant-aware KMP; el tag lleva prefijo `v`, coincide con
  `VERSION_NAME` de `gradle.properties` upstream).
- Evidencia de publicación iOS (`.module` de JitPack, descargado y leído):
  variantes `iosArm64ApiElements` → `innertubex-iosarm64-v0.8.4.klib`
  (1,2 MB) e `iosSimulatorArm64ApiElements` → `innertubex-iossimulatorarm64`
  en `.../innertubex-iosarm64|iossimulatorarm64/v0.8.4/`.
- Transitivas (del propio `.module`, `requires` exactos):
  `ktor-client-core 3.6.0`, `kotlinx-serialization-json 1.11.0`,
  `kotlinx-coroutines-core 1.11.0`, `kotlin-stdlib 2.4.20`,
  `quickjs-kt 1.0.15` (variante `quickjs-kt-iosarm64:1.0.15` verificada en
  Maven Central).
- Compatibilidad sin degradar nada: upstream 0.8.4 usa **Kotlin 2.4.20**
  (= nuestro proyecto), coroutines 1.11.0 (= nuestro). Único bump:
  **ktor 3.5.2 → 3.6.0** (nuestro `libs.versions.toml`), exigido por el
  `requires` transitivo; evita partir core 3.5.2 / engine 3.6.0.
- Por qué 0.8.4 y no otra: 0.5.2 (la de Meld) no tiene targets iOS
  (solo `android` + `jvm(desktop)`); iOS existe desde **0.6.0**
  (`iosArm64()` + `iosSimulatorArm64()` en su `build.gradle.kts`, CI
  `compileKotlinIos{Arm64,SimulatorArm64}` en verde); **0.8.0–0.8.2 nunca
  se publicaron en JitPack** (fallo de workers, ver su CHANGELOG 0.8.3);
  0.8.3/0.8.4 sí. 0.7.4 queda como alternativa solo si hubiera que evitar
  ktor 3.6.0 (se pierden los fixes de memoria del cipher 0.8.x).
- Cambios de API 0.5.2 → 0.8.4 que nos afectan: **ninguna ruptura en lo
  usado**. `InnerTube(httpClient, retryDelay, logger)`, `search/browse/
  player/next`, `YouTubeClient` (+companions `WEB_REMIX`) y `YouTubeLocale`
  (`defaultYouTubeLocale()` nativo desde 0.6.0) estables. La única ruptura
  (0.8.0, `InnerTubeExtractor` acepta `ExtractionCipherService`, firma JVM
  cambiada) no nos toca: en esta fase no se construye extractor (P0.3).
  Desde 0.8.0 `content-negotiation`/`serialization-json-ktor` son solo
  `test` upstream → el host instala `ContentNegotiation` (hecho en
  `HttpClientFactory`).

## 2. Resolución Kotlin/Native (PASO B, evidencia local)

- `./gradlew :shared:compileKotlinJvm :shared:compileKotlinIosSimulatorArm64 :shared:compileKotlinIosArm64`
  **BUILD SUCCESSFUL** en Linux (metadatos/klibs; el link a framework lo
  hace CI en macOS). Gradle resuelve las variantes `iosArm64` /
  `iosSimulatorArm64` (no JVM/Android; sin deps JVM-only).
- Incidencias reales y fix (2, sin ocultar errores):
  1. `MusicQueueRenderer` referenciaba `PlaylistPanelRenderer` (no portado)
     → se portaron `PlaylistPanelRenderer.kt`, `PlaylistPanelVideoRenderer.kt`
     y `AutomixPreviewVideoRenderer.kt` (puros, mismo método).
  2. `value class SearchFilter` sin `@JvmInline` no compila en `jvm()`
     → se conserva `@JvmInline` con `import kotlin.jvm.JvmInline`
     (disponible en `commonMain` con import explícito).
- Sin composite build ni fork: JitPack publica correctamente las variantes
  iOS; no hizo falta copiar InnerTubeX. Atribución/licencia: InnerTubeX es
  GPL-3.0, uso dentro del repo, sin fork publicado.

## 3. Código original reutilizado (PASO C)

Portado verbatim a `shared/src/commonMain/.../innertube/` con único cambio
mecánico `com.metrolist.innertube` → `com.meld.shared.innertube` (vía `sed`;
`app/` e `innertube/` sin tocar). Cierre de deserialización + parsing de
búsqueda, 34 ficheros:

- `models/`: YTItem, Runs, Thumbnails, ThumbnailRenderer,
  MusicResponsiveListItemRenderer, SectionListRenderer, MusicShelfRenderer,
  Tabs, Continuation, ContinuationItemRenderer, Endpoint, NavigationEndpoint,
  Menu, Badges, Icon, Button, UrlEndpoint, MusicCarouselShelfRenderer,
  MusicCardShelfRenderer, MusicPlaylistShelfRenderer,
  MusicDescriptionShelfRenderer, MusicResponsiveHeaderRenderer,
  MusicEditablePlaylistDetailHeaderRenderer, GridRenderer, MusicQueueRenderer,
  MusicMultiRowListItemRenderer, MusicNavigationButtonRenderer,
  MusicTwoRowItemRenderer, PlaylistPanelRenderer, PlaylistPanelVideoRenderer,
  AutomixPreviewVideoRenderer (31).
- `models/response/SearchResponse.kt`, `pages/SearchPage.kt`
  (`SearchResult` + `toYTItem`), `pages/PageHelper.kt` (2+1).
- `utils/ParseTime.kt`: **adaptado** (solo `String.parseTime()` puro; fuera
  `sha1`/`MessageDigest` JVM-only y `completed()` que exigen el `YouTube`
  completo + páginas de playlist/biblioteca).
- `YouTubeClient`/`YouTubeLocale` NO se portan: eran typealias a innertubex;
  en `shared/` se importa innertubex directamente.
- No portado a propósito (fuera de alcance): resto de `YouTube.kt`
  (player/browse/next/mutaciones/upload), `InnerTube.kt` facade (OkHttp +
  `java.net.Proxy`), `SearchSummaryPage`, resto de `pages/` y `response/`.

## 4. Adaptadores y código nuevo

| Fichero | Qué es |
|---|---|
| `network/HttpClientFactory.kt` (common, `expect platformHttpEngine`) | `createYouTubeHttpClient()`: mirror de `createClient()` (detalle en `02C-NETWORK-VALIDATION.md`) |
| `network/HttpClientFactory.ios.kt` | `actual = Darwin` |
| `network/HttpClientFactory.jvm.kt` | `actual = CIO` (solo host de tests) |
| `search/YouTubeMusicSearch.kt` | `search()` + `searchContinuation()` + `SearchFilter.FILTER_SONG`, adaptados de `YouTube.kt:321-384,3286-3301` (innertubex directo, sin Timber/Proxy/estado global; reintentos delegados a InnerTubeX; blank → vacío sin red) |
| `music/SongSearchRepository.kt` | Interfaz + `YouTubeSongSearchRepository` + `SongItem.toTrack()` (mapeo documentado, sin inventar datos) |
| `ui/SearchScreen.kt` + wiring en `MeldApp.kt` (5ª pestaña) | PASO F (detalle en network-validation) |
| Tests: `search/YouTubeMusicSearchTest` (13), `music/SongSearchMappingTest` (8), `innertube/utils/ParseTimeTest` (2), `search/SearchFixtures` | PASO G (detalle en network-validation) |

Dependencias nuevas (`libs.versions.toml` + `shared/build.gradle.kts` con
plugin `kotlin-serialization`): `innertubex` (v0.8.4),
`coroutines-core`, `serialization-json` (1.11.0), `ktor-client-core`,
`ktor-client-content-negotiation`, `ktor-serialization-json` (commonMain);
`ktor-client-darwin` (iosMain); `ktor-client-cio` (jvmMain test-host);
`ktor-client-mock` (commonTest). `ktor 3.6.0`.

## 5. Riesgos y límites conocidos

- La forma JSON de YT deriva: `ignoreUnknownKeys` + `toYTItem` devolviendo
  `null` ante items irreconocibles la absorben sin cierres (testeado).
- Cipher/PoToken/extracción quedan para P0.3 (aquí solo catálogo anónimo).
- JitPack como único origen del artifact (sin mirror); si cae, el build iOS
  no resuelve (igual que hoy para Android con 0.5.2).
- Subir ktor a 3.6.0 no afecta a Android en el pipeline iOS (`app/` fuera de
  settings); al re-sincronizar con Android habrá que alinearlo allí también.
