# FASE 02B — Auditoría de reutilización y plan de migración (solo investigación)

Fecha (UTC): 2026-10-12 · Rama `feat/ios-foundation` @ `9825cbfe` · Estado: AUDIT COMPLETE / MIGRATION PLAN READY.

## 02B.0 Cierre formal de 02A: DEVICE VERIFIED

Evidencia del propietario sobre el IPA 02A (iPhone 11 Pro): arranque sin cierres, renderizado de Compose,
navegación entre Songs/Albums/Artists/Playlists, datos demo visibles, pantallas de detalle, navegación de regreso,
sin cierres inesperados. Alcance: UI demo. Sin reproducción musical ni conectividad real (fuera de alcance por diseño).
Estado: **02A — DEVICE VERIFIED.**

## 02B.1 Método y cifras (medidas, no estimadas)

Sobre el árbol real: 531 `.kt` (app 444 + innertube 87). Con imports directos `android./androidx./java./javax.`:
377/531 (71 %); sin ellos: 154/531 (29 %), de los cuales ~77 modelos `@Serializable`/utils portables.
UI (191 `.kt`): 7/191 sin plataforma. Acoplamientos: Media3 56 · Hilt/Dagger 58 · Room 35.
Criterio funcional: PORTABLE_DIRECT / PORTABLE_ADAPT / REQUIRES_PLATFORM_ADAPTER / ANDROID_ONLY / DEFER
(definiciones en `02B-PORTABILITY-AUDIT.md` §0). Las 6135 líneas de `MusicService` no se usan para porcentajes.

## 02B.2 Hallazgos principales

- Modelos 02A (`Track/Artist/Album/Playlist`) = subconjunto de lectura correcto del original; nada de `db/entities`
  (31/33 con Room/Compose/`java.time`) es portable tal cual salvo `LocalItem` y `ArtistPageCache`.
- UI: pantallas de catálogo PORTABLE_ADAPT (bloqueos aislables: Hilt, CompositionLocals, Coil, 2 APIs plataforma);
  Player/MiniPlayer ANDROID_ONLY hasta `PlayerController`; menús y sliders/shimmer, ganancia temprana.
- InnerTube: catálogo/metadatos reutilización alta (parsing puro); extracción audio cero sin port (binario).
  **Evidencia nueva: InnerTubeX upstream es KMP con `iosArm64/iosSimulatorArm64` desde 0.6.0 (2026-09-07);
  Meld fija 0.5.2. Vía de salida: subir a 0.8.x (coordenada KMP variant-aware).** PoToken sigue siendo del host.
- Player: reglas (colas, shuffle/repeat, errores, calidades) portables; motor+sistema se reescriben
  (AVPlayer/AVAudioSession/MPRemoteCommandCenter). Prerrequisito: interfaz `PlayerController`.
- Persistencia: Room-KMP / datastore-preferences-core / Koin-manual / navigation-CMP / ktor-darwin / Coil3-KMP.
  Esquema v41 intocable.
- Estimación funcional: DIRECT ~15 % · ADAPT ~35 % · ADAPTER ~20 % · ANDROID_ONLY ~25 % · DEFER ~5 %
  (condicionada al bump de InnerTubeX; método en AUDIT §9).

## 02B.3 Roadmap

P0 (buscar+reproducir: P0.1 transporte → P0.2 catálogo real → P0.3 primer audible → P0.4 controles),
P1 (biblioteca, favoritos/playlists, home, ajustes, navegación), P2 (descargas, letras, Spotify, avanzado).
Primeros a portar: DOM-01–05, NET-12, PLY-08/11, UI-03. Detalle en `02B-MIGRATION-ROADMAP.md`.

## 02B.4 Archivos y CI

Creados: `docs/ios-port/02B-{PORTABILITY-AUDIT,REUSE-INVENTORY,MIGRATION-ROADMAP,DEPENDENCY-MAP}.md`.
Actualizados: `REPORT/RESULTS/STATE.md`. Sin cambios funcionales (UI 02A, `app/`, `innertube/`, Gradle intactos).
Experimentos: solo lectura + conteos + webfetch upstream (sin spikes de compilación).
Push a `feat/ios-foundation` (docs-only; si CI corre, registrar abajo sin declararlo bloqueo).

## 02B.5 Veredicto ciclo 1 (PENDING → registrar run id tras el push)

---

# FASE 02A — Fundación iOS-only + primer catálogo musical (autonomía GitHub, máx. 3 ciclos CI)

Fecha (UTC): 2026-10-11 · Rama `feat/ios-foundation` @ `427e4140` (01D-FIX01 CI VERIFIED como base) · Estado: IMPLEMENTED, PENDING_CI, ciclo CI 1/3.

## 02A.0 Cierre de 01D: DEVICE VERIFIED (smoke test manual del propietario)

Evidencia física aportada por el propietario sobre el IPA corregido de 01D-FIX01 (firmado con 3uTools, instalado en iPhone 11 Pro):

- Interfaz Meld-iOS renderizada en el dispositivo.
- Texto compartido Kotlin visible.
- Compose Multiplatform funcionando.
- Contador interactivo con seis pulsaciones registradas.
- Ausencia del cierre inmediato anterior en esa prueba.

Alcance declarado: **arranque, renderizado e interacción inicial verificados**. No se afirma que funciones futuras estén verificadas. Estado: **01D — DEVICE VERIFIED (manual smoke test)**.

## 02A.1 Decisión arquitectónica: iOS-only

Por decisión del propietario, Meld-iOS es exclusivamente una app iOS. Sin requisito de compatibilidad, compilación, mantenimiento ni regresión Android. Meld y Metrolist originales siguen siendo la referencia Android. Kotlin/Native + Compose Multiplatform se conservan como base; Swift/SwiftUI/Foundation/AVFoundation son válidos donde aporten ventaja. `app/` e `innertube/` permanecen en disco como referencia histórica, fuera de la configuración Gradle activa. Sin reescribir el proyecto en Swift.

## 02A.2 Auditoría y simplificación (Paso A)

Clasificación de dependencias Gradle:

- A (necesarias iOS): plugin `kotlinMultiplatform`, plugin `composeMultiplatform` + compose-compiler, `compose.runtime/foundation/ui/material3` en commonMain, JDK 21, Xcode 26.4.1, XcodeGen 2.45.4, targets `iosArm64` + `iosSimulatorArm64`, framework estático `MeldShared`.
- B (solo Android, retiradas del build activo): target `android` de `:shared` (plugin `androidKmpLibrary` + bloque `android{}`), `iosX64()` (Tier-3 sin artefactos CMP 1.12.1; el override `ARCHS=arm64` ya lo evitaba), `shared/src/androidMain/Platform.android.kt`, includes `:app`/`:innertube` en `settings.gradle.kts`, job `android-regression`, pasos `Set up Android SDK` + `Ensure Android 37` en los 3 jobs iOS, `:app:assembleFossDebug` como gate.
- C (compartidas reemplazables): ninguna nueva en esta fase.
- D (referencia histórica, no participa en la compilación): directorios `app/`, `innertube/` (presentes en disco, excluidos de settings).
- Residual documentado (no se toca por mínimo riesgo, no descarga SDK): alias `androidKmpLibrary apply false` y `classpath(libs.gradle)` (AGP) en el root `build.gradle.kts`, bloque `exclusiveContent` de InnerTubeX y flags `android.*` en `gradle.properties`, resto de entradas Android en `libs.versions.toml`, workflows `build*.yml` (solo corren en `main`/PR/`dispatch`; `build_pr.yml` fallaría en una PR desde esta rama porque `:app` ya no configura — sin merges previstos, fuera de alcance).

Añadido: target `jvm()` en `:shared` SOLO como host de pruebas (`:shared:jvmTest` ejecuta `commonTest` en CI ubuntu y en Linux local); no es plataforma destino y no genera framework. `expect platformName()` conserva su `actual` en `iosMain` y suma `jvmMain/Platform.jvm.kt`.

Verificación local sin `ANDROID_HOME`: `:shared:tasks --all` PASS, `compileKotlinIosSimulatorArm64` + `compileKotlinIosArm64` PASS (klib), tareas `linkDebugFrameworkIos{SimulatorArm64,Arm64}` + `embedAndSignAppleFrameworkForXcode` + `jvmTest` presentes. `project.yml` e `Info.plist` intactos (claves 01D conservadas).

## 02A.3 Dominio musical (Pasos B–C)

Estudiados `MediaMetadata` (id/title/artists/duration-int-segundos/thumbnail/album/explicit/liked), `SongEntity`/`AlbumEntity`/`ArtistEntity`/`PlaylistEntity` y modelos `innertube` `@Serializable`. Reutilizados los conceptos (IDs opacos, duración en segundos con `-1` = desconocida, artwork opcional, relaciones canción↔álbum↔artista↔playlist) sin copiar dependencias Android/Room/Media3/Hilt ni APIs JVM (`LocalDateTime`, `Serializable`).

Nuevos modelos en `shared/src/commonMain/.../music/Models.kt` (solo stdlib, inmutables): `Track` (id, title, `artists: List<ArtistRef>`, `album: AlbumRef?`, `durationSeconds`, `artworkUrl?`, `explicit`, derivados `formattedDuration`/`artistNames`), `Artist`, `Album` (con `trackIds`), `Playlist` (con `trackIds`), más `ArtistRef`/`AlbumRef` y `formatDuration()` (`m:ss`, `h:mm:ss`, `"--:--"` si negativa).

## 02A.4 Contratos y demo (Paso D)

`music/MusicCatalogRepository.kt`: interfaz con `tracks/track(id)`, `albums/album(id)`, `artists/artist(id)`, `playlists/playlist(id)` (null si ausente) + resolutores `playlistTracks/albumTracks/artistTracks` (listas vacías si ausente) + `snapshot()`. `InMemoryMusicCatalogRepository` con datos inyectados; `failure: DemoCatalogException?` opcional para estados de error. `music/DemoCatalog.kt`: 6 pistas / 2 álbumes / 3 artistas / 2 playlists ficticios (`demo-*`, p. ej. "Demo Sunrise"), con casos útiles (explícito, colaboración multi-artista, duración larga 1:02:01, duración desconocida, single sin álbum, playlist con ID inexistente que se ignora). Sin networking, sin credenciales, sin YouTube Music/Spotify/InnerTubeX.

## 02A.5 Interfaz (Paso E)

`ui/MeldApp.kt` reescrito (tema oscuro Meld conservado): `NavigationBar` con 4 pestañas (Songs/Albums/Artists/Playlists), listas `LazyColumn` con datos reales del repositorio (carga `suspend` vía `LaunchedEffect`), 4 pantallas de detalle con navegación de regreso (`‹ Back`), cabeceras, `CatalogRow` reutilizable, placeholder de artwork local (inicial, sin red), badge explícito, estados loading/vacío/error con reintento, etiqueta honesta "Demo catalog · no streaming yet". `ui/CatalogComponents.kt`: `ArtPlaceholder`, `CatalogRow`, `ExplicitBadge`, `SectionTitle`, `LoadingView`, `EmptyView`, `ErrorView`. Sin reproductor falso: el detalle de canción muestra `OutlinedButton` deshabilitado "Playback not available in this phase". `ContentView.swift` y host SwiftUI intactos.

## 02A.6 Pruebas y pipeline (Paso F)

`commonTest/.../music/ModelsTest.kt` (9 tests: formato 0:00/1:35/59:59/1:02:01/`--:--`, `artistNames`, delegación, inmutabilidad) + `RepositoryTest.kt` (9 tests: snapshot demo, detalle, IDs desconocidos → null/vacío, relaciones álbum/artista/playlist incl. skip de ID inexistente, repositorio vacío, repositorio en fallo → `DemoCatalogException` en las 6 llamadas). Local: **18/18 PASS** (`:shared:jvmTest`, verificado en esta sesión). Workflow: 4 jobs (`ios-framework`, `shared-tests` nuevo en ubuntu con `:shared:jvmTest` + upload de reports en fallo, `ios-app`, `ios-device-ipa`); validaciones 01C/01D (13 checks incl. plist booleano) intactas.

## 02A.7 Veredicto ciclo 1 (run #17 `actions/runs/38178379518`, commit `bd0e97a4`) — 02A CI VERIFIED

- Los 4 jobs SUCCESS: `ios-framework`, `shared-tests` (nuevo, `:shared:jvmTest` verde en ubuntu), `ios-app` (incl. check de lectura del plist en simulador), `ios-device-ipa` (13/13 validaciones PASS).
- Informe CI `device-ipa-validation.txt`: `PASS: CADisableMinimumFrameDurationOnPhone boolean true` intacto, resto intacto (`platform IOS`, minos 16.0, sdk 26.4, arm64 sin x86_64, bundle OK, MeldShared estático, sin firma, sin junk).
- IPA: `MeldIOS-unsigned-device.ipa`, **14.102.066 bytes**, SHA-256 `9c49d72ffa66c670a8ad990ecf8875769c9fd1b610095a70864505796c1cbb06` (crece ~1 MB por el catálogo/UI nuevos; esperado).
- Cupo consumido: 1/3. **Estado: 02A — CI VERIFIED / DEVICE RETEST PENDING** (la UI musical solo se declara verificada en iPhone cuando el propietario firme, instale y pruebe el nuevo IPA).
- Registro documental del veredicto en ciclo 2/3 (solo docs, sin cambios funcionales).

---

# FASE 01D-FIX01 — Crash de arranque por Info.plist reescrito (autonomía GitHub, máx. 3 ciclos CI)

Fecha (UTC): 2026-10-11 · Rama `feat/ios-foundation` @ `57d08cda` (run #15 `38172944050` con los 4 jobs SUCCESS como base) · Estado: IMPLEMENTED, PENDING_CI, ciclo CI 1/3.

Contexto: 01C VERIFIED (IPA sin firma generado). El propietario firmó con 3uTools, instaló en iPhone 11 Pro (iOS 27.0.1) y la app cerró al arrancar (pantalla negra breve). Informe `MeldIOS-2026-10-11-170452.ips`: EXC_CRASH / SIGABRT, `IllegalStateException` en `androidx.compose.ui.uikit.PlistSanityCheck.performIfNeeded`. En la copia firmada falta `CADisableMinimumFrameDurationOnPhone` (bundle ID con sufijo `com.eirom16.meldios.TT9HY6U979`, sin atribuir al proyecto).

## FIX01.1 Comparación fuente vs IPA original (run #15, evidencia propia)

- Fuente `iosApp/MeldIOS/Info.plist`: contiene las 4 claves (`CADisableMinimumFrameDurationOnPhone=true` booleano + `LSRequiresIPhoneOS`, `UILaunchScreen`, `UISupportedInterfaceOrientations`). Verificado con `plistlib` (`type is bool and is True`).
- IPA original descargado con `gh` (artifact `MeldIOS-unsigned-device-ipa`, 13.054.764 bytes): `Payload/MeldIOS.app/Info.plist` carece de **las 4** claves de la fuente; solo contiene defaults (`CFBundle*`, `DT*`, `MinimumOSVersion 16.0`, `UIDeviceFamily`, `UIRequiredDeviceCapabilities`), con `CFBundleIdentifier=com.eirom16.meldios` y `CFBundleExecutable=MeldIOS` correctos.
- Conclusión: deficiencia confirmada en generación/empaquetado (el IPA original ya sale sin la clave). No se atribuye a 3uTools: pendiente comparar contra el firmado solo como validación posterior.

## FIX01.2 Causa confirmada (código fuente de XcodeGen 2.45.4)

- `Sources/XcodeGenKit/FileWriter.swift` (`writePlists()`): si el target define `info`, XcodeGen **reescribe el fichero en cada `generate`** con `generateProperties() + info.properties` (solo defaults + propiedades declaradas). Nuestro spec declaraba `info.path` sin `properties` → cada `xcodegen generate` en CI borraba silenciosamente las 4 claves escritas a mano.
- `Sources/XcodeGenKit/InfoPlistGenerator.swift`: los defaults generados son exactamente las claves vistas en el IPA (coincidencia forense total).
- Reproducción independiente publicada (mismo patrón: claves a mano que desaparecen tras regenerar).
- `INFOPLIST_FILE` sí se aplica desde `info.path` (el bundle ID resuelve bien); el problema era el **contenido** del fichero, no la ruta. No hacen falta settings `INFOPLIST_FILE`/`GENERATE_INFOPLIST_FILE` adicionales.

## FIX01.3 Corrección ciclo 1 (mínima, sin redundancias)

- `iosApp/project.yml`: bloque `info.properties` con las 4 claves de la fuente (`CADisableMinimumFrameDurationOnPhone: true` sin comillas = booleano YAML → `<true/>`; `LSRequiresIPhoneOS: true`; `UILaunchScreen: {}`; `UISupportedInterfaceOrientations: [UIInterfaceOrientationPortrait]`). Sintaxis `Plist{path, properties}` válida en 2.45.4; simulación local defaults+properties → `plistlib` confirma booleano `true`.
- Sin cambios de versiones (Kotlin/Compose/Gradle/XcodeGen), arquitectura, `shared/`, `app/`, `innertube/`, ni configs (Debug+Release intactas). Sin desactivar el check de Compose.
- Workflow (`.github/workflows/ios-foundation.yml`): job `ios-device-ipa` pasa de 12 a 13 comprobaciones (nuevo check `plistlib`: clave presente, `type is bool and is True`; rechaza cadena `"true"`, número y ausencia; el job falla si no cumple). Job `ios-app`: nuevo paso de solo lectura `Verify Compose plist key (simulator)` sobre el `.app` ya compilado (sin alterar su compilación). Las 12 validaciones previas se conservan intactas.
- Veredicto y artifact nuevo: PENDING_CI (registrar run id y resultado abajo tras la ejecución). Cupo: 0/3 consumido hasta el push.

## FIX01.4 Veredicto ciclo 1 (run #16 `actions/runs/38175702380`, commit `2a53af13`) — 01D-FIX01 CI VERIFIED

- Los 4 jobs SUCCESS: `ios-framework`, `android-regression`, `ios-app` (incl. nuevo paso `Verify Compose plist key (simulator)`), `ios-device-ipa` (13/13 validaciones PASS).
- Informe CI `device-ipa-validation.txt`: `PASS: CADisableMinimumFrameDurationOnPhone boolean true` (línea `CADisableMinimumFrameDurationOnPhone is boolean true`), resto intacto (`platform IOS`, minos 16.0, sdk 26.4, arm64 sin x86_64, bundle OK, MeldShared estático, sin firma, sin junk).
- Verificación independiente en Linux del artifact descargado: las 4 claves presentes con tipos correctos (`CADisableMinimumFrameDurationOnPhone True [bool]`, `LSRequiresIPhoneOS True [bool]`, `UILaunchScreen {} [dict]`, `UISupportedInterfaceOrientations ['UIInterfaceOrientationPortrait'] [list]`), `CFBundleIdentifier=com.eirom16.meldios`, `MinimumOSVersion=16.0`.
- IPA: `MeldIOS-unsigned-device.ipa`, **13.054.877 bytes**, SHA-256 `0ab228f956db868f8d9577bd98eb81d1e9eefc5000e883346d8b32dd587ac5cf`.
- Cupo consumido: 1/3. **Estado: 01D-FIX01 — CI VERIFIED / DEVICE RETEST PENDING** (el crash solo se declara resuelto cuando el propietario firme, instale y arranque en su iPhone).

---

# FASE 01C — Primer IPA para iPhone físico (autonomía GitHub, máx. 3 ciclos CI)

Fecha (UTC): 2026-10-11 · Rama `feat/ios-foundation` @ `9e0e511b` (run #12 `38169795066` con los 3 jobs SUCCESS como base) · Estado: IMPLEMENTED, PENDING_CI, ciclo CI 1/3.

Contexto: 01B VERIFIED (run #11 `38168783902`): `MeldIOS.app` de simulador compila (`ARCHS=arm64` + `MainViewControllerKt.MainViewController()`). 01C añade el cuarto job `ios-device-ipa` sin tocar los tres existentes.

## 01C.1 Estrategia de archive

- `xcodebuild archive` con `-destination 'generic/platform=iOS'`, `-sdk implícito iphoneos`, `ARCHS=arm64`, `CODE_SIGNING_ALLOWED=NO` + `CODE_SIGN_IDENTITY=""` + `CODE_SIGNING_REQUIRED=NO`, `-configuration Debug`, `-archivePath iosApp/build/device/MeldIOS.xcarchive`, `-derivedDataPath iosApp/build/device/DerivedData` (bajo `iosApp/build/`, ya ignorado en `.gitignore`).
- `SDK_NAME=iphoneos` hace que `embedAndSignAppleFrameworkForXcode` seleccione **iosArm64** (nunca iosSimulatorArm64/iosX64); `project.yml` y `shared/build.gradle.kts` intactos (incl. `iosX64()` declarado y config Release).
- Fallback reproducible en el mismo paso: si `archive` rechazara la falta de firma, `xcodebuild -sdk iphoneos ... build` (misma config/flags) y el empaquetado usa `DerivedData/Build/Products/Debug-iphoneos/MeldIOS.app`. El paso `Locate` acepta ambas rutas y falla en voz alta si no hay `.app`.

## 01C.2 Procedimiento de empaquetado

- Sin `exportArchive` (exige firma): `ditto` del `.app` a `ipa-staging/Payload/MeldIOS.app`, `chmod +x` del ejecutable, `zip -qry ../MeldIOS-unsigned-device.ipa Payload`.
- Salida: `iosApp/build/device/MeldIOS-unsigned-device.ipa`. Sin simulador, sin cachés Gradle, sin fuentes, sin SDKs, sin credenciales.

## 01C.3 Validación Mach-O y estructural (12 comprobaciones, el job falla si alguna crítica falla)

1. IPA existe y no vacío · 2. `unzip -t` válido · 3. contiene `Payload/MeldIOS.app/` · 4. ejecutable `CFBundleExecutable` presente y con bit +x · 5. `lipo -info` = arm64 sin slice x86_64 (+ `file`) · 6. `vtool -show-build`: plataforma iOS dispositivo (nombre `IOS` o número `2`; `IOSSIMULATOR`/`7` = FAIL) · 7. `CFBundleIdentifier = com.eirom16.meldios` (PlistBuddy) · 8. `minos = 16.0` · 9. `Info.plist` y recursos presentes · 10. MeldShared estático (`otool -L` sin referencia, sin `Frameworks/MeldShared.framework` embebido) · 11. sin firma (`codesign -dv` debe fallar) · 12. sin `DerivedData/.gradle/iphonesimulator/x86_64//build/` en el ZIP.
- Informe `device-ipa-validation.txt` (arquitectura, plataforma, bundle ID, tamaño, SHA-256, estado de firma) como segundo artifact.

## 01C.4 Publicación y regresión

- Artifacts: `MeldIOS-unsigned-device-ipa` (el `.ipa`, `if-no-files-found: error`) + `MeldIOS-device-validation` (el informe). Sin releases. Jobs `ios-framework`, `android-regression`, `ios-app` intactos (mismo XcodeGen 2.45.4 + SHA, mismo Xcode 26.4.1, mismas versiones Kotlin/Compose/Gradle). Sin tocar `app/`, `innertube/` ni la interfaz de Meld.

## 01C.5 Veredicto ciclo 1 (run #13 `actions/runs/38171331668`, commit `e978ba59`)

- `ios-framework` / `android-regression` / `ios-app`: SUCCESS (regresión intacta). Job `ios-device-ipa`: FAILURE solo en la validación.
- `archive` iphoneos arm64 sin firma: PASS por la vía `archive` (xcarchive real, fallback no usado). Empaquetado `Payload/`: PASS. Validación 11/12 PASS (`platform IOS`, minos 16.0, sdk 26.4, arm64 sin x86_64, bundle OK, MeldShared estático, sin firma).
- Check 12 FAIL espurio: el `grep` sobre `unzip -l` casaba su propia cabecera `Archive: iosApp/build/...` (patrón `/build/`), no contenido real.
- Fix ciclo 2 (este push): comprobación 12 usa `unzip -Z1` (solo nombres de fichero, sin cabecera). Cupo consumido: 1/3.

## 01C.6 Veredicto ciclo 2 (run #14 `actions/runs/38172210884`, commit `9a8c104a`) — 01C VERIFIED

- Los 4 jobs SUCCESS: `ios-framework`, `android-regression`, `ios-app`, `ios-device-ipa` (11m aprox.).
- Validación 12/12 PASS en CI (`platform IOS`, minos 16.0, sdk 26.4, arm64, bundle OK, estático, sin firma, sin junk) + artifacts publicados.
- Verificación independiente en Linux del artifact descargado: ZIP válido, `Payload/MeldIOS.app/{MeldIOS,Info.plist,PkgInfo,compose-resources/}`, `file` = `Mach-O 64-bit arm64 executable`, plist `com.eirom16.meldios` / `MinimumOSVersion 16.0`, bit +x preservado, SHA-256 local idéntico al del informe CI.
- IPA: `MeldIOS-unsigned-device.ipa`, **13.054.764 bytes**, SHA-256 `57f6291d66ea80e66b2cdf29d446b57917a7b20a405109724b445f47b369c1f0`.
- Cupo consumido: 2/3. **Fase 01C: VERIFIED — UNSIGNED IPA GENERATED.** No se afirma instalación ni ejecución en iPhone (sin evidencia real).

---

# FASE 01B-FIX03 — Forzar simulador arm64 para evitar el slice iosX64 (autonomía GitHub, máx. 3 ciclos CI)

Fecha (UTC): 2026-10-11 · Rama `feat/ios-foundation` @ `812a9767` (remoto `df3a1867` + commit local documental conservado) · Estado: IN_PROGRESS, ciclo CI 1/3.

Contexto: run #9 (`actions/runs/38166146096`, commit `df3a1867`): XcodeGen PASS (`xcodegen exit code: 0`, `Created project .../MeldIOS.xcodeproj`), `List schemes` PASS, `ios-framework` PASS, `android-regression` PASS, `Build MeldIOS.app` FAIL en `PhaseScriptExecution Compile Kotlin Framework` → `:shared:compileKotlinIosX64` → `No matching variant of org.jetbrains.compose.{runtime,foundation,ui}:1.12.1 ... 'org.jetbrains.kotlin.native.target' with value 'ios_x64'`. Evidencia del log: el destino genérico fija `ARCHS=arm64 x86_64` (`VALID_ARCHS=arm64 x86_64`, `SDK_NAME=iphonesimulator26.4`, `CONFIGURATION=Debug`) y la tarea `embedAndSignAppleFrameworkForXcode` intenta compilar el slice x86_64; CMP 1.12.1 no publica artefactos `ios_x64` (target Tier-3/deprecado, heredado de 01A). Aviso adicional en el log: `iosX64Test ... cannot run on the current host (macos-aarch64) ... architecture mismatch`.

## FIX03.1 Corrección ciclo 1 (solo paso `Build MeldIOS.app`; resto intacto)

- `xcodebuild` añade override `ARCHS=arm64` (línea de comando, gana sobre el genérico): `-destination 'generic/platform=iOS Simulator' -derivedDataPath iosApp/build ARCHS=arm64 CODE_SIGNING_ALLOWED=NO build`. Configuración Debug mantenida. El script Kotlin debe recibir `CONFIGURATION=Debug`, `SDK_NAME` de simulador y `ARCHS=arm64`, seleccionando `iosSimulatorArm64` en lugar de `iosX64`.
- Sin cambios en este intento: `iosX64()` se conserva en `shared/build.gradle.kts`; versiones de Kotlin/Compose/Gradle intactas; compilación `iosArm64` de dispositivo intacta (job `ios-framework`); resto de pasos del job `ios-app` intactos.
- Lectura prevista: si el error `ios_x64` desaparece, continuar la compilación completa de `MeldIOS.app`; si aparece otro error, tratarlo como bloqueo independiente; si `ARCHS=arm64` no evita `iosX64`, el siguiente paso es retirar `iosX64()` del módulo `shared` (alcance actual: simulador ARM64 + dispositivo ARM64), justificado en la documentación.

## FIX03.2 Veredicto ciclo 1 (run #10 `actions/runs/38168024527`, commit `3d4fb88c`)

- `ARCHS=arm64` **elimina el error `ios_x64`**: el log muestra `:shared:compileKotlinIosSimulatorArm64` (ya no `compileKotlinIosX64`) y la fase `Compile Kotlin Framework` completa. `ios-framework` SUCCESS, `android-regression` SUCCESS, XcodeGen + `List schemes` SUCCESS.
- Nuevo bloqueo independiente (capa Swift, no Kotlin): `ContentView.swift:9:30: error: type 'MainViewControllerKt' has no member 'mainViewController'`. El facade `MainViewControllerKt` sí resuelve (framework compilado y enlazado); solo el miembro difiere: Kotlin/Native conserva el case del `fun MainViewController()` en el selector ObjC, luego el miembro Swift es `MainViewController()` (mayúscula), como en el template oficial `compose-multiplatform-ios-android-template` (`Main_iosKt.MainViewController()`).
- Cupo: 1/3 consumido. Ciclo 2: llamada `MainViewControllerKt.MainViewController()` en `ContentView.swift`, grep del verify a `MainViewController`, + paso diagnóstico que vuelca los símbolos `ViewController` de `MeldShared.h` si el build vuelve a fallar.

## FIX03.3 Veredicto ciclo 2 (run #11 `actions/runs/38168783902`, commit `5f7a4a58`) — 01B VERIFIED

- Los 3 jobs SUCCESS: `ios-framework`, `android-regression`, `ios-app` (conclusión `success`, 11m54s).
- `Generate Xcode project`: `xcodegen exit code: 0`. `List schemes`: SUCCESS.
- `Build MeldIOS.app`: `** BUILD SUCCEEDED **` (target `arm64-apple-ios16.0-simulator`, `-framework MeldShared` en el `Ld`, `CONFIGURATION=Debug`, `SDK_NAME=iphonesimulator26.4`).
- `Verify MeldShared export`: header `shared/build/xcode-frameworks/Debug/iphonesimulator26.4/MeldShared.framework/Headers/MeldShared.h` con `+ (UIViewController *)MainViewController __attribute__((swift_name("MainViewController()")));` (línea 182) + `MainViewControllerKt` (línea 172).
- `Verify MeldIOS.app exists`: PASS (`Debug-iphonesimulator/MeldIOS.app`).
- Artifacts: `MeldIOS-simulator-app` (13.809.915 bytes) + `MeldShared-iosSimulatorArm64-framework` (66.060.373 bytes) + `xcodegen-isolation`, todos vigentes.
- Error `ios_x64`: ELIMINADO (ciclo 1). Case Swift: RESUELTO (ciclo 2). Cupo consumido: 2/3. **Fase 01B: VERIFIED.** No iniciar 01C en esta sesión.

---

# FASE 01B-FIX02 — Aislar el crash de XcodeGen (autonomía GitHub, máx. 3 ciclos CI)

Fecha (UTC): 2026-10-11 · Rama `feat/ios-foundation` @ `70b3f02b` · Estado: IN_PROGRESS, ciclo CI 1/3.

Contexto: run #6 (`actions/runs/37985938502`, commit `70b3f02b`): `ios-framework` PASS, `android-regression` PASS, `ios-app` FAIL en `Generate Xcode project` con XcodeGen **2.45.4 fijado y verificado** (`xcodegen.zip: OK`, `Version: 2.45.4`) → `⚙️ Generating project...` → `xcodegen exit code: 133`, sin `DiagnosticReports` (directorio vacío) y `xcodegen.log` de 59 bytes (solo las dos líneas ⚙️). **Hipótesis de regresión de 2.46.0 refutada**: 2.45.4 aborta idénticamente. El binario sí ejecuta (`--version` OK), luego el crash es un `fatalError` en fase de generación del proyecto, no un binario inejecutable. Quedan dos hipótesis: (H1) incompatibilidad del binario con el runner `macos-26-arm64`/macOS 26.6.2, (H2) alguna opción de `iosApp/project.yml` dispara el `fatalError`.

## FIX02.0 Decisión arquitectónica sobre xtool (investigación 1.21.0 concluida)

- GitHub Actions macOS es el sistema principal para iOS; **no se instala Darwin SDK en Linux** (restricción de licencia Apple: prohibidos redistribuir/hostear SDKs y usarlos en hardware no Apple; además no hay distribución oficial de `darwin.xtoolsdk`).
- **xtool queda como herramienta auxiliar y experimental**, fuera del camino crítico.
- Se conservan Kotlin Multiplatform + Compose Multiplatform; **no se migra a Swift puro** (Kotlin/Native exige host macOS+Xcode para binarios Apple finales; xtool no lo sustituye).
- Roadmap técnico: 01B (desbloquear `MeldIOS.app`) → 01C (IPA sideloading) → 02+ (transporte MP, modelos, Room/DataStore, player AVFoundation). Sin cambios en esta decisión salvo evidencia nueva.

## FIX02.1 Corrección ciclo 1 (solo job `ios-app`; los otros dos intactos)

- Nuevo paso `XcodeGen isolation test (minimal project)` **antes** de `Generate Xcode project`: imprime `sw_vers`, `which/file/lipo --version` del binario, crea un proyecto mínimo (`Mini`, 1 target application iOS, 2 configs, 1 fuente, 1 scheme) en `$RUNNER_TEMP` y ejecuta el **mismo binario** con `xcodegen generate`, guardando `xcodegen-minimal.log`. **Nunca falla el job** (`exit 0`): el árbitro es el artifact, no su exit code.
- Nuevo paso `Upload XcodeGen isolation log` con `if: always()` (artifact `xcodegen-isolation`, `warn` si vacío).
- Sin cambios en `project.yml`, versiones, ni en los jobs `ios-framework`/`android-regression`. Sin versiones arbitrarias.

## FIX02.2 Lectura prevista del ciclo 1

- Mínimo PASS + real FAIL → H2 (spec): ciclo 2 = bisección de opciones de `project.yml` (primero duplicar a 2 configs Debug+Release, principal sospechoso: proyecto de 1 sola config).
- Mínimo FAIL → H1 (binario/runner): ciclo 2 = investigar alternativa (herramienta o `.xcodeproj` versionado desde macOS válido, sin inventar internos, conservando host SwiftUI + MeldShared + Xcode 26.4.1).
- Ambos PASS → el crash era dependiente de versión/entorno ya superado; continuar a `xcodebuild`.

## FIX02.3 Veredicto ciclo 1 (run #7 `actions/runs/38165060516`, commit `6e4d8afe`)

- Host: macOS 26.6.2 (25G83), runner arm64. Binario: Mach-O universal x86_64+arm64, `Version: 2.45.4`, nativo.
- Mínimo: exit **0**, `Created project at .../Mini.xcodeproj`. Real: exit **133** (sin cambios).
- `ios-framework` SUCCESS, `android-regression` SUCCESS (los dos jobs que funcionan, intactos).
- **H1 refutada, H2 confirmada**: el crash lo dispara alguna opción de `iosApp/project.yml`. La única diferencia estructural ya exonerada es `deploymentTarget` en el target (el Mini la incluía y pasó).

## FIX02.4 Plan ciclo 2 (bisección V1–V5 en un solo job, sin cambios de spec real)

- Nuevo paso `XcodeGen spec bisection (V1-V5)` tras el test mínimo: genera 5 variantes con el mismo binario (V1 = 1 sola config Debug; V2 = settings.base exacto; V3 = preBuildScripts exacto; V4 = excludes+info.plist; V5 = copia real de `iosApp/` + `Release: release` vía `awk`, verificado localmente: solo añade esa línea), recoge `summary.txt` y nunca falla el job.
- Upload `xcodegen-isolation` ampliado a `xcodegen-minimal.log` + `xcbis-logs/` (`if: always()`).
## FIX02.5 Fix ciclo 3: añadir config Release (causa probada, cambio mínimo)

Bisección run #8 (`actions/runs/38165963313`, commit `4c6ba525`), artifact `xcodegen-isolation` → `xcbis-logs/summary.txt`:

| Variante | Contenido | Exit |
|---|---|---|
| V1 | Mini + 1 sola config Debug | **133** (reproduce la firma exacta: 2 líneas ⚙️) |
| V2 | Mini + settings.base exacto | 0 (`V2Settings.xcodeproj`) |
| V3 | Mini + preBuildScripts exacto | 0 (`V3Scripts.xcodeproj`) |
| V4 | Mini + excludes + info.plist | 0 (`V4Info.xcodeproj`) |
| V5 | Copia real de `iosApp/` + `Release: release` | 0 (`MeldIOS.xcodeproj`) |

Causa: XcodeGen 2.45.4 hace `fatalError` (SIGTRAP, exit 133, sin validación ni crash report) al generar un spec de **una sola configuración**. Settings, scripts, excludes e info quedan exonerados. Fix: `Release: release` en `iosApp/project.yml` (una línea + comentario; idéntico a lo probado en V5). La compilación sigue usando `-configuration Debug`; Release solo completa el modelo. Bisección retirada del workflow (misión cumplida); se conserva el test mínimo como canario de runner. Sin plan alternativo necesario salvo que el `xcodebuild` posterior revele un bloqueo nuevo.

## FIX02.6 Veredicto ciclo 3 (run #9 `actions/runs/38166146096`, commit `df3a1867`)

- `Generate Xcode project`: exit **0**, `Created project at .../iosApp/MeldIOS.xcodeproj`. `List schemes`: SUCCESS. **Bloqueo XcodeGen RESUELTO y verificado en CI.**
- `Build MeldIOS.app`: xcodebuild arranca pero falla en `PhaseScriptExecution Compile Kotlin Framework` → `:shared:compileKotlinIosX64` → `No matching variant of org.jetbrains.compose.{runtime,foundation,ui}:1.12.1 ... 'org.jetbrains.kotlin.native.target' with value 'ios_x64'`. Causa nueva, distinta capa: `-destination 'generic/platform=iOS Simulator'` fija `ARCHS=arm64 x86_64` y la tarea `embedAndSignAppleFrameworkForXcode` intenta compilar el slice x86_64, pero CMP 1.12.1 no publica artefactos `iosX64` (target Tier-3/deprecado, heredado de 01A).
- `ios-framework` y `android-regression`: SUCCESS.
- Cupo de 3 ciclos CI agotado: el fix (retirar `iosX64()` de `shared/` y/o fijar destino a un simulador arm64 concreto) queda documentado como siguiente paso con autorización nueva. **01B permanece BLOCKED** (sin `MeldIOS.app`, no todos los jobs PASS): Caso B.

---

# FASE 01B-FIX01 — Corregir fallo de XcodeGen en GitHub Actions

Fecha (UTC): 2026-10-09 · Rama `feat/ios-foundation` @ `c97f11ff` · Estado: IMPLEMENTED, PENDING_CI.

Contexto: run #5 (`actions/runs/37983046004`): `ios-framework` PASS, `android-regression` PASS, `ios-app` FAIL en el paso `Generate Xcode project` con `XcodeGen 2.46.0 → Generating project... → Trace/BPT trap: 5` (exit 133), antes de xcodebuild.

## FIX01.1 Diagnóstico

- El crash es del binario (SIGTRAP = `fatalError` de Swift en tiempo de ejecución), no un error de validación del spec: XcodeGen valida los specs con mensajes limpios y exit ≠ 133.
- Revisión completa de `iosApp/project.yml` sin hallazgos verificables: opciones válidas, rutas existentes (`MeldIOS/` con 2 Swift, `Info.plist` presente y excluido de sources), una sola config Debug (permitida), scheme mínimo válido. **Sin cambios en `project.yml`** (prohibidos los especulativos).
- Evidencia externa: XcodeGen 2.46.0 (2026-07-16, bump a XcodeProj 9.14.0 + reordenación de targets) acumula reportes de rotura, incluido el issue "2.46.0 Is Broken on Homebrew" (bottles con checksums de 2.45.4). Hipótesis de trabajo: regresión de 2.46.0 en el runner `macos-26`/Xcode 26.4.1, no defecto de nuestro spec.
- Validación previa del remedio en Linux: descarga del `xcodegen.zip` oficial 2.45.4 (4,3 MB) con `sha256sum` = `090ec294…bdbef` (coincide con el SHA oficial); layout confirmado (`xcodegen/bin/xcodegen`).

## FIX01.2 Correcciones (solo job `ios-app`; los otros dos intactos)

- Instalación brew → **XcodeGen 2.45.4 fijado**: `curl -fsSL` del release oficial + `shasum -a 256 -c` (falla en voz alta si difiere) + `unzip` + localización robusta del binario con `find` + `chmod +x` + `--version` registrado en el log + `GITHUB_PATH` (sin instalaciones globales).
- Paso `Generate Xcode project` con diagnóstico: captura `PIPESTATUS`, `tee` a `xcodegen.log`, eco del exit code, `ls -la` y `exit` con el código real (sin ocultar errores).
- Recogida de crash reports macOS (`~/Library/Logs/DiagnosticReports`, `/Library/...`, filtro `*xcodegen*`) + copia del log a `diagnostics/` y subida como artifact `xcodegen-diagnostics` con `if: failure()` (warn si vacío).

## FIX01.3 Riesgo restante

Si 2.45.4 también abortara, el artifact de diagnóstico (log + `.ips`) dará la causa exacta en una sola ronda; el siguiente sospechoso sería entonces el spec o el runner, no la versión.

---

# FASE 01B — Compose Multiplatform y primera aplicación iOS

Fecha (UTC): 2026-10-09 · Rama `feat/ios-foundation` @ `34ba79a3` (commit verificado de la run #4 de 01A) · Estado: IMPLEMENTED, pendiente de CI.

Nota previa: la Fase 01A quedó **VERIFIED** por GitHub Actions (run #4, commit `34ba79a3`): frameworks iosSimulatorArm64 + iosArm64 PASS, `:app:assembleFossDebug` PASS (KSP/Hilt incluidos), artifact `MeldShared-iosSimulatorArm64-framework` generado, Xcode 26.4.1 confirmado. Las correcciones CI del propietario (SDK `platforms;android-37.0` + symlink `android-37`, `debug.keystore` generado) se conservan intactas.

## 01B.1 Resumen de arquitectura

- `shared/` suma UI compartida: `MeldApp()` (commonMain) + `MainViewController()` (iosMain, `ComposeUIViewController`).
- `iosApp/` es un host SwiftUI mínimo (sin lógica de negocio) que incrusta el `UIViewController` de Kotlin vía `UIViewControllerRepresentable`.
- Integración Gradle↔Xcode por vía directa oficial: fase Run Script `Compile Kotlin Framework` → `:shared:embedAndSignAppleFrameworkForXcode` (estático: enlazado, nada que embeber).
- Proyecto Xcode reproducible con XcodeGen (`iosApp/project.yml` versionado; el `.xcodeproj` se genera solo en CI).
- CI: nuevo job `ios-app` (xcodebuild → `MeldIOS.app` de simulador sin firma + artifact). Sin IPA, sin certificados.

## 01B.2 Versiones finales

Kotlin **2.4.20** · AGP **9.3.1** · Gradle **9.7.0** · KSP **2.3.11** (sin cambios) · Compose Multiplatform **1.12.1** (nueva clave `composeMultiplatform`, separada del BOM Jetpack `compose = 1.12.0`) · Xcode **26.4.1** en `macos-26` · XcodeGen **latest de Homebrew** (versión registrada en el log del job) · iOS deployment target **16.0** · Bundle ID `com.eirom16.meldios`.

## 01B.3 Archivos añadidos

```text
shared/src/commonMain/kotlin/com/meld/shared/ui/MeldApp.kt   # @Composable MeldApp(): tema oscuro musical,
                                                             #  título, Greeting().greet(), nota KMP, botón-contador
shared/src/iosMain/kotlin/com/meld/shared/MainViewController.kt  # fun MainViewController(): UIViewController
                                                             #  (fichero SIN package a propósito → facade MainViewControllerKt)
iosApp/MeldIOS/MeldIOSApp.swift      # @main App → WindowGroup(ContentView())
iosApp/MeldIOS/ContentView.swift     # ComposeViewController (UIViewControllerRepresentable) → MainViewControllerKt.mainViewController()
iosApp/MeldIOS/Info.plist            # CADisableMinimumFrameDurationOnPhone=true (obligatorio CMP) + launch keys mínimas
iosApp/project.yml                   # definición XcodeGen: target MeldIOS, scheme compartido, settings sin firma,
                                     #  FRAMEWORK_SEARCH_PATHS a xcode-frameworks, -framework MeldShared, sandbox de scripts OFF
```

Modificados: `gradle/libs.versions.toml` (+versión y plugin `composeMultiplatform`), `build.gradle.kts` (`apply false`), `shared/build.gradle.kts` (plugins compose + `compose.runtime/foundation/ui/material3` en commonMain), `.github/workflows/ios-foundation.yml` (job `ios-app`), `.gitignore` (`iosApp/*.xcodeproj`, `iosApp/build/`). No tocados: `app/**`, `innertube/**`, resto de workflows, versión de Meld.

## 01B.4 Integración SwiftUI/Compose

Patrón oficial (kotlinlang compose-swiftui-integration, jun-2026): `MainViewController(): UIViewController = ComposeUIViewController { MeldApp() }` en Kotlin; en Swift `UIViewControllerRepresentable` que lo instancia e ignora el safe area. Nombre Swift `MainViewControllerKt.mainViewController()` según la regla de export Kotlin/Native (facade = nombre de fichero + `Kt`; el fichero no declara package para que el nombre sea exacto y comprobable). El job CI verifica el símbolo con `grep mainViewController` sobre el `MeldShared.h` generado y falla en voz alta si difiere.

## 01B.5 Integración Gradle/Xcode

La fase corre antes de Compile Sources, sin análisis de dependencias (`basedOnDependencyAnalysis: false`), con guardia `OVERRIDE_KOTLIN_BUILD_IDE_SUPPORTED`, invocando gradlew con los heaps capados (`-Xmx3g`, sin tocar `gradle.properties`). Xcode aporta `CONFIGURATION/SDK_NAME/ARCHS/...`; Debug mapea al framework debug (sin configs custom, sin `KOTLIN_FRAMEWORK_BUILD_TYPE`). `ENABLE_USER_SCRIPT_SANDBOXING = NO` (obligatorio desde Xcode 15 o el daemon muere con errores crípticos) y enlazado vía `OTHER_LDFLAGS -framework MeldShared`.

## 01B.6 Diseño de CI (job `ios-app`, `macos-26`, 45 min)

JDK 21 → Android SDK 37.0 + symlink `android-37` (solución 01A reutilizada) → Gradle cache → Xcode 26.4.1 si disponible → `brew install xcodegen` (+`--version` registrado) → `xcodegen generate` → `xcodebuild -list` (esquemas) → `xcodebuild -scheme MeldIOS -configuration Debug -destination 'generic/platform=iOS Simulator' -derivedDataPath iosApp/build CODE_SIGNING_ALLOWED=NO build` → grep del export en `MeldShared.h` → `test -d …/MeldIOS.app` → artifact `MeldIOS-simulator-app` (`if-no-files-found: error`: prohibido subir vacío). Jobs `ios-framework` y `android-regression` intactos.

## 01B.7 Riesgos y problemas restantes

- Nombre del símbolo Swift (`MainViewControllerKt.mainViewController()`): convención documentada pero header no inspeccionable en Linux; el grep de CI es el árbitro (si falla, el log muestra los candidatos reales).
- XcodeGen latest vía brew: reproducible por definición versionada + versión en log; si un futuro XcodeGen rompe la generación, fijar versión será el primer paso.
- Arranque en simulador (`xcrun simctl launch`) y dispositivo físico: fuera de alcance / NOT TESTED.
- Siguiente fase natural (01C): IPA sin firma para sideloading (`xcodebuild archive` + export sin firma o `Payload/` manual).

---

# FASE 01A — Fundación Kotlin Multiplatform y primera compilación iOS

Fecha (UTC): 2026-10-09 · Rama `feat/ios-foundation` (creada local desde `main` @ `2ae37b1`, sin commit/push) · Estado: IMPLEMENTED, pendiente de CI.

## 01A.1 Resumen de lo implementado

- Toolchain alineado: **Kotlin 2.4.10 → 2.4.20** (único bump de versión del proyecto); AGP 9.3.1, Gradle 9.7.0 y KSP 2.3.11 conservados.
- Nuevos aliases en el catálogo: `kotlinMultiplatform` (`org.jetbrains.kotlin.multiplatform`) y `androidKmpLibrary` (`com.android.kotlin.multiplatform.library`, versión = AGP); registrados con `apply false` en el root.
- Nuevo módulo `:shared` dado de alta en `settings.gradle.kts`; `app/` e `innertube/` no tocados (ni lógica ni dependencias).
- `shared/` compila Kotlin para Android + iOS con `Greeting` + `expect/actual platformName()`; cero dependencias externas.
- Framework iOS **estático `MeldShared`** declarado para iosX64/iosArm64/iosSimulatorArm64.
- Nuevo workflow `.github/workflows/ios-foundation.yml` (Job A macOS `macos-26` + Job B regresión Android); workflows Android existentes intactos.
- Verificación local real: configuración Gradle completa PASS, `compileKotlinIosSimulatorArm64` PASS (klib generado), link de framework omitido por host Linux (requiere macOS), lado Android bloqueado por falta de SDK local. CI queda PENDING_CI.

## 01A.2 Arquitectura nueva

```text
Meld-iOS/                          # rama feat/ios-foundation
├── app/            # SIN CAMBIOS (com.android.application + Hilt + Media3)
├── innertube/      # SIN CAMBIOS (com.android.library)
├── shared/         # NUEVO: org.jetbrains.kotlin.multiplatform
│                   #        + com.android.kotlin.multiplatform.library
│                   # commonMain: Greeting + expect platformName()
│                   # androidMain: actual "Android" · iosMain: actual "iOS"
└── .github/workflows/ios-foundation.yml  # NUEVO (los 6 workflows previos intactos)
```

Sin `iosApp/`, sin Swift, sin Compose Multiplatform (fuera de alcance 01A por especificación).

## 01A.3 Configuración definitiva de Gradle

- `gradle/libs.versions.toml:7`: `kotlin = "2.4.20"`; `[plugins]` +2 líneas (`kotlinMultiplatform`, `androidKmpLibrary` con `version.ref = "androidGradlePlugin"`).
- `build.gradle.kts`: ambos aliases con `apply false` (sin cambiar el bloque `buildscript`, que resuelve `kotlin("gradle-plugin", libs.versions.kotlin)` automáticamente a 2.4.20).
- `settings.gradle.kts`: `include(":shared")` (única línea añadida).
- `shared/build.gradle.kts`: plugins KMP + Android-KMP; bloque `kotlin { android { namespace = "com.meld.shared", compileSdk = 37, minSdk = 26 } }` (sintaxis `android {}` válida para AGP ≥ 8.12, coherente con compileSdk/minSdk del proyecto); targets creados con `listOf(iosX64(), iosArm64(), iosSimulatorArm64())`; framework estático `baseName = "MeldShared"` por target; source sets `commonMain/androidMain/iosMain` con bloques de dependencias vacíos (jerarquía por defecto: appleMain/iosMain/nativeMain intermedios generados por el plugin).

## 01A.4 Versiones utilizadas

Kotlin **2.4.20** · AGP **9.3.1** · Gradle **9.7.0** · KSP **2.3.11** (sin cambios) · Xcode previsto **26.4.1** en runner `macos-26` (con fallback documentado al Xcode del runner + `xcodebuild -version` como evidencia). Daemon Gradle en CI: JDK Temurin 21 vía `actions/setup-java` (el repo ya fija `toolchainVersion=21` en `gradle/gradle-daemon-jvm.properties`).

## 01A.5 Decisiones sobre targets

- Android + iosArm64 + iosSimulatorArm64 (obligatorios) + **iosX64 incluido**: no introdujo bloqueo (tareas `compileKotlinIosX64`/`linkDebugFrameworkIosX64` generadas; en host Linux sus tests aparecen como *disabled* con aviso, lo cual es esperado: los tests de simulador requieren macOS). No se añadió `kotlin.native.ignoreDisabledTargets` para no tocar la configuración global.
- Nombres de tarea verificados contra `tasks --all`: `linkDebugFrameworkIosSimulatorArm64`, `linkDebugFrameworkIosArm64` (usados en el workflow); lado Android del nuevo plugin usa compilación única `compileAndroidMain`/`assembleAndroidMain` (sin variantes debug/release; el intento `compileDebugKotlinAndroid` falla por nombre inexistente — documentado, no es error de config).

## 01A.6 Estructura exacta del módulo shared

```text
shared/
├── build.gradle.kts
└── src/
    ├── commonMain/kotlin/com/meld/shared/Greeting.kt          # class Greeting { fun greet() } + expect fun platformName()
    ├── androidMain/kotlin/com/meld/shared/Platform.android.kt # actual fun platformName() = "Android"
    └── iosMain/kotlin/com/meld/shared/Platform.ios.kt         # actual fun platformName() = "iOS"
```

Sin imports de plataforma en `iosMain` (literal `"iOS"`), sin dependencias, sin recursos.

## 01A.7 Diseño y explicación del workflow CI

`.github/workflows/ios-foundation.yml` — trigger `workflow_dispatch` + `push` a `feat/ios-foundation`; `permissions: contents: read`; sin secretos ni firma Apple; sin runners de pago.

- **Job A `ios-framework` (`macos-26`, 45 min)**: checkout, Temurin 21, Android SDK (necesario para *configurar* el proyecto AGP), Gradle cache, selección de Xcode 26.4.1 con comprobación de disponibilidad (`ls /Applications`, fallback al defecto, `xcodebuild -version` siempre impreso), `java -version` + SDK del simulador, `:shared:tasks --all` como puerta de configuración, `linkDebugFrameworkIosSimulatorArm64` (obligatorio) + `linkDebugFrameworkIosArm64` (device, paso propio), `ls -R shared/build/bin` y subida del `.framework` como artifact de inspección (no IPA). Límites de memoria solo por CLI (`-Dorg.gradle.jvmargs="-Xmx3g" -Pkotlin.daemon.jvm.options="-Xmx3g"`) para no alterar `gradle.properties` global (4 GB, pensado para Android).
- **Job B `android-regression` (`ubuntu-latest`, 20 min)**: checkout, Temurin 21, Android SDK, Gradle cache, `:app:assembleFossDebug` + paso final que registra el outcome (`always()`).

## 01A.8 Riesgos pendientes (heredan B-códigos de Fase 00)

- B5/KSP: 2.3.11 con Kotlin 2.4.20 sin demostrar → lo arbitrará el Job B (si `:app` deja de compilar, realinear KSP es el primer sospechoso).
- Link `MeldShared` en macOS sin probar (omitido en Linux por diseño de K/N) → Job A.
- B2 InnerTubeX, B3 motor OkHttp, B4 `SpotifyAuth`, B6 migraciones Room: intactos y fuera de alcance; el `shared/` actual no los toca.
- `iosX64` podría emitir aviso de deprecación en82026+: vigilar la salida del Job A; si molesta, se retira solo ese target sin afectar a los demás.

## 01A.9 Próximo paso: Fase 01B

Compose Multiplatform y host iOS: plugin `org.jetbrains.compose` en `shared/`, pantalla demo con `Greeting().greet()`, proyecto `iosApp/` (SwiftUI + `ComposeUIViewController`), job CI que compile la app de simulador. Requiere el semáforo verde de ambos jobs de este workflow.

---

# FASE 00 — Auditoría técnica de la migración Meld a iOS

Fecha (UTC): 2026-10-09
Rama: `main` · HEAD: `2ae37b17e55cc024ec5ba7d44c267cfb2aad995d`
Estado del árbol: limpio (`git status --short` vacío, verificado).

## 1. Resumen ejecutivo

- El proyecto es hoy una app 100 % Android (531 ficheros `.kt` en `app/` + `innertube/`), sin ningún módulo KMP ni proyecto Xcode.
- La combinación actual **Kotlin 2.4.10 + AGP 9.3.1 está fuera de la matriz oficial de compatibilidad**: KGP 2.4.0–2.4.10 admite AGP hasta 9.1.0; AGP 9.3.1 exige KGP 2.4.20–2.4.21 (fuente: tabla oficial de compatibilidad Kotlin/Gradle/AGP, consultada 2026-10-09).
- Con AGP ≥ 9.0, el plugin `org.jetbrains.kotlin.multiplatform` **no puede combinarse** en el mismo módulo con `com.android.application`/`com.android.library`. El módulo compartido futuro deberá usar el nuevo plugin `com.android.kotlin.multiplatform.library` en un módulo **nuevo** (`shared/`); `app/` se conserva intacto como entry point Android.
- El primer bloqueante funcional para iOS es **InnerTubeX**: solo existen artefactos `innertubex`, `innertubex-android` y `innertubex-desktop` (ver `settings.gradle.kts:20-31`); no hay evidencia de artefacto iOS/Native, y `InnerTube.kt:139` + `Spotify.kt:84,101` fijan el motor Ktor **OkHttp** (JVM/Android). Sin resolver la capa de red/extracción no hay app iOS útil, aunque el `shared/` mínimo sí puede compilarse sin ella.
- Reproductor (Media3/ExoPlayer, `MusicService.kt`, 6135 líneas, 86 imports `android.*`, 62 `androidx.*`), DI (Hilt, 54 ficheros), navegación (`androidx.navigation.compose`) y DataStore-preferences con API `Context` son específicos de Android y requieren `expect/actual` o reimplementación (AVFoundation, Koin/manual, Voyager/Decompose o navegación CMP, datastore-preferences-core).
- Migrables con adaptación: modelos y parsing `innertube` (kotlinx.serialization), `Spotify.kt` (lógica GQL/REST pura salvo motor HTTP y `SpotifyAuth` JVM), Room 2.8.4 (KMP soportado oficialmente: runtime + `sqlite-bundled` + KSP por target + patrón `@ConstructedBy`; verificar AutoMigrations en KMP), Ktor-core/serialización, Coil 3 (ya multiplatform-first), coroutines/serialization.
- Ningún bloqueo impide la **Fase 01** (módulo KMP mínimo + pantalla CMP + CI macOS + Android intacto): el `shared/` mínimo no necesita InnerTubeX, Media3 ni Room.

## 2. Commit y rama inspeccionados

- Rama actual: `main` (sigue a `origin/main`; `git log -1`: `2ae37b1 chore(spotify): update GQL hashes [automated]`, 2026-10-09 12:27:25 UTC).
- `git status --short`: sin salida (árbol limpio). `git diff --check`: sin errores (salida 0).
- Instrucciones locales leídas: `AGENTS.md` (raíz). Respetadas: sin cambios en código, sin commits/push, sin tocar README/AGENTS/docs ajenas, sin bump de versión, sin cambios de esquema DB.

## 3. Arquitectura actual verificada

### 3.1 Módulos Gradle (`settings.gradle.kts:40-42`)

| Módulo | Tipo | Fichero build |
|---|---|---|
| `:app` | `com.android.application` (namespace `com.metrolist.music`, appId `com.meld.app`) | `app/build.gradle.kts` |
| `:innertube` | `com.android.library` (namespace `com.metrolist.innertube`) | `innertube/build.gradle.kts` |

No existe ningún módulo con `org.jetbrains.kotlin.multiplatform`, ni directorio `iosApp/`, ni `.xcodeproj`/`.xcworkspace` (búsqueda negativa verificada).

### 3.2 Versiones base (`gradle/libs.versions.toml`, `gradle-wrapper.properties`)

- Kotlin `2.4.10`, AGP `9.3.1`, Gradle wrapper `9.7.0`, `compileSdk 37`, `minSdk 26`, `targetSdk 36`, toolchain/bytecode JVM 21 (`app/build.gradle.kts:165-176`, `innertube/build.gradle.kts:14-23`).
- Compose (BOM) `1.12.0`, Material3 `1.5.0-alpha27`, Media3 `1.10.1`, Room `2.8.4`, Hilt `2.60.1`, Ktor `3.5.2`, Coil `3.6.0`, InnerTubeX `v0.5.2`, KSP `2.3.11` (ver §6 riesgo R4: serie 2.3.x frente a Kotlin 2.4.x, a verificar en Fase 01).
- Flavors: `foss` (defecto), `gms` (Cast), `izzy` (F-Droid). Versión `0.9.2`, `versionCode 28`.
- JDK del entorno local: OpenJDK 27; los workflows usan Temurin 21 → el build local reproducible exige JDK 21 (limitación documentada, no se compiló).

### 3.3 Entry points y componentes principales (rutas reales)

- `app/src/main/kotlin/com/metrolist/music/App.kt` — `Application` con `@HiltAndroidApp`; inicializa Coil, `InnerTubeXPlayer`, YouTube locale/sesión, LastFM, `Spotify`+`SpotifyHashSync`+`SpotifyTokenManager`, DataStore snapshot, proxy, notificaciones. Todo con APIs Android (`NotificationChannel`, `WebView CookieManager`, `filesDir`).
- `app/src/main/kotlin/com/metrolist/music/MainActivity.kt` (~1087+ líneas) — `FragmentActivity` + `@AndroidEntryPoint`; bindea `MusicService`, monta `setContent { MetrolistApp(...) }` (Scaffold, NavHost, player bottom-sheet, updater, temas dinámicos).
- `app/src/main/kotlin/com/metrolist/music/playback/MusicService.kt` — **6135 líneas**; `class MusicService : MediaLibraryService(), Player.Listener, PlaybackStatsListener.Callback`, `@AndroidEntryPoint`; 86 imports `android.*`, 62 `androidx.*` (ExoPlayer, MediaSession, cachés, ecualizador, Discord RPC, Qobuz, SponsorBlock, crossfade…).
- Red/extracción: `innertube/src/main/kotlin/com/metrolist/innertube/InnerTube.kt` (fachada sobre `com.metrolist.innertubex.InnerTube`; `HttpClient(OkHttp)` en línea 139, usa `java.io`, `java.net.Proxy`, `io.ktor.utils.io.jvm.javaio`), `innertube/src/main/kotlin/com/metrolist/innertube/YouTube.kt` (objeto de alto nivel + modelos `models/`, `pages/`), `app/src/main/kotlin/com/metrolist/music/utils/InnerTubeXPlayer.kt` (único punto de extracción de streams; guarda `Context`, `ConnectivityManager`, `ConcurrentHashMap` de JDK).
- Spotify: `app/src/main/kotlin/com/metrolist/spotify/Spotify.kt` (objeto; clientes Ktor `restClient`/`gqlClient` con motor **OkHttp**, 14 operaciones GQL según `docs/spotify-gql-hashes.json`), `SpotifyAuth.kt` (usa `java.net.HttpURLConnection` + `javax.crypto.Mac` → JVM-only), `SpotifyHashProvider.kt`, `SpotifyMapper.kt`, `models/` (11 ficheros, `@Serializable` puros).
- Persistencia: `app/src/main/kotlin/com/metrolist/music/db/MusicDatabase.kt` (`@Database` con 20 entidades + 3 vistas, `AutoMigration`, esquemas versionados en `app/schemas/com.metrolist.music.db.InternalDatabase/1.json…`; wrapper `MusicDatabase(delegate: InternalDatabase)`), `DatabaseDao.kt` (`@Dao`), `Converters.kt` (`@TypeConverter`), `db/daos/SpeedDialDao.kt`. Ojo: el esquema es intocable por restricción de fase.
- Preferencias: `app/src/main/kotlin/com/metrolist/music/utils/DataStore.kt` — `preferencesDataStore(name="settings")` sobre `Context.dataStore` + lecturas bloqueantes `runBlocking` + snapshot en memoria; ~500 llamadas `dataStore.get(...)` estimadas por comentario interno.
- DI: `di/AppModule.kt` (provides con `@ApplicationContext Context`: Room, cachés Media3 `SimpleCache`, `ListenTogetherManager`), `di/NetworkModule.kt`, `di/WrappedModule.kt`, `di/Qualifiers.kt` (`@PlayerCache`, `@DownloadCache`); 54 ficheros usan Hilt/`@Inject`.
- Navegación/UI: `ui/screens/Screens.kt` (sealed `Home/Search/ListenTogether/Library`), `ui/screens/NavigationBuilder.kt` (usa `androidx.navigation.compose` + `android.app.Activity`), 191 ficheros en `ui/`, ~30 ViewModels en `viewmodels/` (usan `hiltViewModel`, `lifecycle-viewmodel-compose`).
- Descargas/caché: `playback/DownloadUtil.kt`, `playback/ExoDownloadService.kt` (Media3 `DownloadManager`/`DownloadService` + `SimpleCache`; `di` provee `@PlayerCache`/`@DownloadCache`).
- Recursos: `app/src/main/res/` con drawables, mipmaps, fuentes, `values/` (+ `metrolist_strings.xml` canónico) y **57 directorios `values-<locale>`**; `app/src/main/proto/listentogether.proto` (protobuf-lite + `MessageCodec.kt`); `app/src/main/AndroidManifest.xml` (permisos FOREGROUND_SERVICE_MEDIA_PLAYBACK, RECORD_AUDIO, receivers de widgets en `music/widget/`).
- CI actual (`.github/workflows/build.yml`, `build_pr.yml`, `build_quick.yml`): solo `runs-on: ubuntu-latest`, JDK Temurin 21; **no existe job macOS**. No se modifican workflows en esta fase.

## 4. Matriz de compatibilidad (A–E)

Leyenda — A: reutilizable en `commonMain` tal cual · B: reutilizable tras adaptar dependencias · C: específico Android (se queda en `app/`) · D: requiere implementación iOS nueva · E: compatibilidad con Kotlin/Native no demostrada.

| # | Componente | Clase | Evidencia (ruta real) | Justificación |
|---|---|---|---|---|
| 1 | Kotlin 2.4.10 + Gradle 9.7.0 | toolchain del proyecto | `gradle/libs.versions.toml:7`, `gradle-wrapper.properties:3` | **E (versiones)**. KGP 2.4.0–2.4.10 solo admite AGP ≤ 9.1.0 y Gradle ≤ 9.5.0; con AGP 9.3.1 se exige KGP 2.4.20–2.4.21 (tabla oficial Kotlin). Candidata de estudio: **Kotlin 2.4.20**, sin actualizar aún. |
| 2 | Android Gradle Plugin 9.3.1 | build | `gradle/libs.versions.toml:2`, `app/build.gradle.kts:26`, `innertube/build.gradle.kts:2` | **C con restricción D**. Con AGP ≥ 9, `kotlin.multiplatform` es incompatible con `com.android.application/library` en el mismo módulo; el futuro `shared/` debe usar `com.android.kotlin.multiplatform.library`. `app/` e `innertube/` no se tocan. |
| 3 | Compose UI (1.12.0) + Material3 | UI | `MainActivity.kt:23-107`, `ui/` (191 ficheros) | **B**. La UI declarativa es portable a Compose Multiplatform, pero hay que migrar de artefactos `androidx.compose` a plugin `org.jetbrains.compose`, sustituir `androidx.palette` (C), recursos `res/` → `composeResources`, y los puentes `Activity`/`LocalContext`. |
| 4 | Navegación | navegación | `ui/screens/NavigationBuilder.kt` (`androidx.navigation.compose`), `ui/screens/Screens.kt` | **C → D**. `androidx.navigation.compose` es Android/JVM; en iOS exige reimplementar la navegación (navegación CMP / Voyager / Decompose). `Screens.kt` (rutas como datos) es **A** parcial. |
| 5 | Hilt 2.60.1 | DI | `App.kt:54,74`, `di/AppModule.kt`, `di/NetworkModule.kt`; 54 ficheros con `@Inject` | **C → D**. Hilt es solo Android (KSP + Dagger). El código compartido deberá usar Koin o DI manual con `expect/actual`; la migración afecta a 54 ficheros: no mover de golpe. |
| 6 | Room 2.8.4 | persistencia | `db/MusicDatabase.kt:92` (`@Database`, 20 entidades, AutoMigrations), `DatabaseDao.kt:83`, `app/schemas/…/1.json…` | **B**. Room es KMP oficial (Android/iOS/JVM/Native): `room-runtime` + `sqlite-bundled` en `commonMain`, KSP por target (`kspIosArm64`…), patrón `@ConstructedBy` + `RoomDatabaseConstructor`, builder iOS con `NSFileManager`. A verificar en Fase 01: AutoMigrations existentes y `SupportSQLiteOpenHelper`/migraciones manuales (`Migration`, `contentValuesOf`, `android.database.sqlite`) usadas en `MusicDatabase.kt:9-21`. Esquema intocable. |
| 7 | Ktor 3.5.2 (core, content-negotiation, encoding, serialization-json) | red | `innertube/InnerTube.kt:8-26`, `spotify/Spotify.kt:17-33` | **B**. Esos artefactos son multiplatform; el bloqueo está en el **motor**: `HttpClient(OkHttp)` fijado en `InnerTube.kt:139` y `Spotify.kt:84,101` (OkHttp = JVM/Android). En `iosMain` usar motor Darwin (o CIO, que ya se usa para Musixmatch en `app/build.gradle.kts:324`) vía `expect/actual`. |
| 8 | InnerTubeX v0.5.2 | extracción YT | `settings.gradle.kts:20-31`, `libs.versions.toml:34,104`, `innertube/InnerTube.kt:6-7`, `utils/InnerTubeXPlayer.kt` | **E (bloqueante)**. Los únicos artefactos referenciados son `innertubex`, `innertubex-android`, `innertubex-desktop`; no hay evidencia de variante iOS/Native. Además `InnerTubeXPlayer.kt` y `InnerTube.kt` usan `android.content.Context`, `ConnectivityManager`, `java.io`, `java.net.Proxy`, `io.ktor.utils.io.jvm.javaio`. Sin resolver esto no hay streaming en iOS. |
| 9 | Modelos/parsing innertube | dominio YT | `innertube/models/` (~50 ficheros), `innertube/pages/`, `YouTube.kt` (imports solo `com.metrolist.*`, `io.ktor.*`, `kotlinx.*`) | **B**. Modelos `@Serializable` y lógica de parseo sin imports Android → candidatos naturales a `commonMain` una vez desacoplados del transporte OkHttp. |
| 10 | Spotify (GQL + REST + mapper + modelos) | integraciones | `spotify/Spotify.kt`, `SpotifyMapper.kt`, `models/` (11 ficheros), `SpotifyHashProvider.kt` | **B** para `Spotify.kt`/mapper/modelos (JSON puro + corrutinas; hashes externos en `docs/spotify-gql-hashes.json`); **C** para `SpotifyAuth.kt` (`java.net.HttpURLConnection`, `javax.crypto` → JVM-only) que exige `expect/actual` o token vía capa de plataforma. |
| 11 | Media3 1.10.1 / ExoPlayer (reproducción) | player | `playback/MusicService.kt` (6135 lín.), `playback/PlayerConnection.kt`, `playback/queues/` (10 ficheros), `ExoDownloadService.kt`, `MediaLibrarySessionCallback.kt`; 56 ficheros importan `androidx.media3` | **C → D**. Media3/ExoPlayer/MediaSession son exclusivos Android. iOS exige reproductor nuevo sobre **AVFoundation** tras una interfaz `expect/actual` (`PlayerController`). `Queue` y `MediaMetadata` (puros) son **A/B**. |
| 12 | Almacenamiento y descargas | caché/offline | `di/AppModule.kt` (`SimpleCache`, `StandaloneDatabaseProvider`), `playback/DownloadUtil.kt`, `playback/ExoDownloadService.kt` | **C → D**. `SimpleCache`/`DownloadManager`/`DownloadService` son Media3-Android. En iOS: `NSURLSession`/descargas AVFoundation + caché propia; la política (qué descargar, límites `MaxSongCacheSizeKey`) puede compartirse (**B**). |
| 13 | DataStore preferences | settings | `utils/DataStore.kt` (`preferencesDataStore`, `Context.dataStore`, `runBlocking`) | **B**. Migrar a `datastore-preferences-core` (multiplatform) + fichero vía Okio con `expect/actual`; eliminar el patrón `runBlocking` en getters antes de compartir. |
| 14 | Recursos gráficos y strings | res | `app/src/main/res/` (57 locales `values-*`, mipmaps, fuentes), `values/metrolist_strings.xml` (canónico por `AGENTS.md`) | **B → D parcial**. Vectores/drawables y `res/` Android no sirven en iOS: migrar a `composeResources` (CMP) + `Lyric`/strings vía `multiplatform-settings`-style o recursos CMP. Los 57 locales implican coste de migración; no tocar en Fase 00/01. |
| 15 | Imágenes (Coil 3.6.0) | media | 49 ficheros con `coil3` (`App.kt:17-29`, `MainActivity.kt:126-132`) | **B**. Coil 3 es multiplatform-first (`coil-compose` en `commonMain`); adaptar `SingletonImageLoader.Factory`/`PlatformContext` Android a inicialización CMP. |
| 16 | Protobuf-lite + `listentogether.proto` | listen-together | `app/src/main/proto/listentogether.proto`, `listentogether/MessageCodec.kt`, `ListenTogetherClient.kt` | **E**. `protobuf-javalite/kotlin-lite` + plugin `com.google.protobuf` son JVM/Android; falta demostrar codegen para Kotlin/Native (alternativas: pbandk u otro runtime MP). No bloquea Fase 01. |
| 17 | Corrutinas + kotlinx.serialization | base | uso transversal (`kotlinx.coroutines`, `kotlinx.serialization.json` en `InnerTube.kt`, `Spotify.kt`, modelos) | **A**. Ambas son multiplatform; van directas a `commonMain`. |
| 18 | Guava, Timber, `java.time`, colecciones JDK | utilidades | `libs.versions.toml:21,31`, `App.kt`, `MusicService.kt`, `InnerTubeXPlayer.kt` (`ConcurrentHashMap`), `MusicDatabase.kt` (`SimpleDateFormat`, `java.time`) | **B/C mixto**. `kotlinx-datetime` sustituye a `java.time`; Timber → expect/actual de logging (p. ej. Napier/Kermit o interfaz propia); Guava (`ImmutableList`, cachés) debe eliminarse del código compartido. |
| 19 | Widgets, workers, Cast (gms), Shazam, LastFM/KuGou/LrcLib | satélites | `music/widget/` (5 receivers), `shazamkit/`, `lastfm/`, `kugou/`, `lrclib/`, flavors en `app/build.gradle.kts:77-100` | **C** (widgets/Cast/Auto = Android) o **B** (clientes HTTP puros LastFM/KuGou/LrcLib/Musixmatch si no usan OkHttp directo — verificar por fichero en su fase). Fuera del camino crítico iOS inicial. |

Conclusión de la matriz: el conjunto **A puro** hoy es pequeño (corrutinas, serialization, parte de modelos). El grueso útil es **B** (modelos innertube/Spotify, Room, Ktor-core, Coil, DataStore-core) y el esfuerzo real está en **C→D** (player AVFoundation, DI, navegación, descargas) más la incógnita **E** (InnerTubeX/Native).

## 5. Dependencias que bloquean iOS (orden de aparición en Fase 01)

1. **B0 — Toolchain: Kotlin 2.4.10 + AGP 9.3.1 fuera de matriz.** Evidencia: tabla oficial (KGP 2.4.0–2.4.10 → AGP máx 9.1.0; AGP 9.3.1 → KGP 2.4.20–2.4.21) + `libs.versions.toml:2,7`. Sin alinear esto, el módulo KMP nace cojo. Candidata: Kotlin **2.4.20** (misma serie, admite Gradle 9.7.0 y AGP 9.3.1). Verificación: tabla oficial + sync/build en Fase 01. No actualizar en Fase 00.
2. **B1 — Plugin Android-KMP obligatorio.** Evidencia: docs oficiales AGP 9 (incompatibilidad `kotlin.multiplatform` + `com.android.application/library` en el mismo módulo). Consecuencia: `shared/` nuevo con `com.android.kotlin.multiplatform.library`; `app/` sigue siendo app pura. Error típico si se intenta convertir `app/` in situ: fallo de sync irresoluble sin separar entry point.
3. **B2 — InnerTubeX sin variante Native (bloqueante funcional, no de compilación mínima).** Evidencia: `settings.gradle.kts:20-31` (solo `innertubex[-android|-desktop]`), `InnerTube.kt:6-7,139`, `InnerTubeXPlayer.kt:1-60` (`Context`, `ConnectivityManager`). El `shared/` mínimo de Fase 01 compila sin InnerTubeX; la app iOS útil lo necesitará (opciones a estudiar en Fase 02: artefacto MP del upstream, portar extracción, o capa de red propia + reutilizar modelos/parsing **B**).
4. **B3 — Motor HTTP OkHttp fijado.** Evidencia: `InnerTube.kt:139`, `Spotify.kt:84,101`. En iOS exige motor Darwin/CIO por source-set. Es el primer `expect/actual` de red.
5. **B4 — `SpotifyAuth` JVM-only.** Evidencia: `SpotifyAuth.kt:1-40` (`java.net.HttpURLConnection`, `javax.crypto.Mac`). El login Spotify en iOS requiere reimplementar TOTP/fetch de token en `iosMain` (o mover auth a la capa Swift y exponer token al shared).
6. **B5 — KSP 2.3.11 frente a Kotlin 2.4.10.** Evidencia: `libs.versions.toml:18,113-115`. La serie KSP debe alinearse con el KGP que se adopte (y AGP 9 auto-eleva KSP < 2.2.10-2.0.2). Verificar en Fase 01 con el sync; hoy se marca como riesgo, no como fallo (no se compiló: ver RESULTS.md).
7. **B6 — Room KMP: migraciones.** Evidencia: `MusicDatabase.kt:9-21,92-170` (imports `android.database`, `SupportSQLite*`, `Migration`, `AutoMigrationSpec`), `app/schemas/`. El patrón KMP (`@ConstructedBy`, driver `sqlite-bundled`, builder iOS `NSFileManager`) está documentado oficialmente, pero las AutoMigrations/migraciones manuales existentes deben validarse una a una; el esquema no se toca.

## 6. Arquitectura multiplataforma recomendada (incremental, sin mover código de golpe)

```text
Meld-iOS/
├── app/            # SIN CAMBIOS: app Android (com.android.application + Hilt + Media3)
├── innertube/      # SIN CAMBIOS por ahora: library Android; candidata a portarse a shared en Fase 02+
├── shared/         # NUEVO (Fase 01): org.jetbrains.kotlin.multiplatform
│                   #          + com.android.kotlin.multiplatform.library
│                   #   commonMain: Greeting demo → dominio puro (MediaMetadata, modelos)
│                   #   androidMain: actuals mínimos (p. ej. Platform.context)
│                   #   iosMain: actuals mínimos + framework para iosApp
├── iosApp/         # NUEVO (Fase 01, mínimo): host SwiftUI + Compose UIViewController
└── gradle/libs.versions.toml  # + plugins kotlinMultiplatform, androidKmpLibrary, compose CMP
```

Reglas de evolución:

- **Fase 01**: solo `shared/` mínimo + `iosApp/` hola-mundo; `app/` e `innertube/` ni se tocan (ni siquiera dependencias hacia `shared/` hasta que el sync sea verde).
- **Fase 02**: desacoplar transporte HTTP (`expect/actual` HttpClientEngine) y decidir InnerTubeX (B2); portar modelos `innertube/models|pages` + `Spotify` (sin Auth) a `commonMain`.
- **Fase 03**: Room KMP en `shared/` (entidades/DAO primero, migraciones después, esquema intacto) + DataStore-core.
- **Fase 04+**: interfaz `PlayerController` expect/actual (Android Media3 / iOS AVFoundation), DI compartida, navegación CMP, recursos CMP, descargas iOS.
- Prohibido en todas: convertir `app/` en módulo KMP (incompatible con AGP 9), usar `checkout --theirs/--ours` por fichero completo en rebases, y tocar el esquema Room sin necesidad demostrada.

## 7. Riesgos técnicos priorizados

| ID | Riesgo | Prob. | Impacto | Mitigación en Fase 01 |
|---|---|---|---|---|
| R1 | Kotlin 2.4.10 + AGP 9.3.1 rompen el sync KMP | Alta | Bloquea todo | Adoptar Kotlin 2.4.20 solo cuando el sync lo exija; verificar contra tabla oficial |
| R2 | InnerTubeX no tiene variante Native | Alta | Sin streaming en iOS | Aislar extracción tras interfaz; Fase 01 no la necesita |
| R3 | `MusicService` (6135 lín.) inseparable a corto plazo | Cierta | Player iOS desde cero | No portar: interfaz `PlayerController` + AVFoundation nuevo |
| R4 | KSP 2.3.11 desalineado del KGP objetivo | Media | Fallo de codegen (Room/Hilt) | Fijar KSP a la serie del KGP adoptado; Hilt queda fuera de `shared/` |
| R5 | AutoMigrations Room no portables 1:1 | Media | Divergencia de esquema iOS | Validar migraciones en KMP antes de mover `InternalDatabase`; esquema intacto |
| R6 | Solo runners Ubuntu en CI; sin job macOS | Cierta | iOS no verificable | Nuevo job `macos-*` en Fase 01 (workflow nuevo o existente —decidir entonces—) |
| R7 | Entorno local con JDK 27 (CI exige 21) | Cierta | Builds locales no reproducibles | Documentar JDK 21 (Temurin) como requisito; no compilar en local hasta tenerlo |
| R8 | 57 locales + recursos `res/` sin equivalente CMP | Baja (fase tardía) | Coste de migración UI | Excluir recursos de Fase 01–03; pantalla demo con strings en código |

## 8. Plan de implementación de la Fase 01 (solo propuesta, no implementar)

Objetivo: módulo KMP mínimo + entry point iOS con pantalla CMP sencilla + build inicial en GHA macOS + Android intacto.

1. **T0 — Primera tarea implementable**: crear `shared/` con `shared/build.gradle.kts` mínimo:
   `org.jetbrains.kotlin.multiplatform` + `com.android.kotlin.multiplatform.library` (versión = AGP actual), targets `android()` + `iosX64() + iosArm64() + iosSimulatorArm64()`, `commonMain` con `Greeting().greet()` + `expect fun platformName()`, `androidMain`/`iosMain` con sus `actual`, framework estático (`baseName = "shared"`). Registrar en `settings.gradle.kts` (`include(":shared")`) y añadir los aliases al catálogo. No añadir dependencias externas todavía (cero riesgo de B2–B6).
2. **Versiones a utilizar** (a confirmar en el sync, no antes): Kotlin **2.4.20** (serie 2.4, admite AGP 9.3.1 y Gradle 9.7.0 según tabla oficial), AGP **9.3.1** (sin cambios), plugin CMP `org.jetbrains.compose` en serie compatible con Kotlin 2.4.20, KSP alineado a 2.4.20 cuando Room entre en juego (no en T0). **Cómo se comprobará**: `./gradlew :shared:assembleDebug` (o `compileKotlinIosSimulatorArm64`) en local con JDK 21 + `gradle/actions/setup-gradle`; el sync debe ser verde antes de tocar `app/`.
3. **iosApp mínimo**: proyecto Xcode (`iosApp/iosApp.xcodeproj`, `ContentView.swift` con `ComposeUIViewController` que muestre el `Greeting`), generado a mano y versionado; sin CocoaPods/SPM ajenos en T0.
4. **CI macOS**: nuevo job con `runs-on: macos-15` (o imagen con Xcode ≥ 26.4, que exige KMP 2.4.x según tabla; verificar `xcodebuild -version` en el runner) que ejecute el build del framework `shared` + `xcodebuild -scheme iosApp -destination 'platform=iOS Simulator'`. Los jobs Ubuntu existentes deben seguir verdes (misma matriz de variantes); no modificar workflows existentes salvo para añadir el job (decisión de la Fase 01).
5. **Puerta de salida**: `shared` compila para Android e iOS en CI, `iosApp` muestra la pantalla demo, `:app:assembleFossDebug` sigue verde, y el informe de Fase 01 documenta la matriz de versiones final + el estado de B0–B6.

*Nota de alcance: esta fase no mueve ni una línea de `app/` o `innertube/` a `shared/`, no actualiza dependencias del proyecto y no crea más que el esqueleto descrito.*
