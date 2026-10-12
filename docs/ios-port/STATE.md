# STATE.md — Estado de continuidad del port Meld → iOS

Última actualización (UTC): 2026-10-12 · Rama `feat/ios-foundation` @ `6bc80ce8` + cambios 02C · **02C — IMPLEMENTED / PENDING_CI** (ciclo CI 1/3) · 02A DEVICE VERIFIED · 02B AUDIT COMPLETE.

> 02B — AUDIT COMPLETE / MIGRATION PLAN READY (2026-10-12, commit `4a50bbf6`, run #18 `38187686201` 4/4 SUCCESS
> docs-only): auditoría de 531 `.kt` (377 con plataforma / 154 candidatos),
> inventario de ~70 componentes, mapa de dependencias, roadmap P0→P1→P2. Hallazgo clave: InnerTubeX upstream es KMP con iOS
> desde 0.6.0; Meld fija 0.5.2 → vía de salida subir a 0.8.x. Estimación funcional: DIRECT ~15 % / ADAPT ~35 % / ADAPTER ~20 % /
> ANDROID_ONLY ~25 % / DEFER ~5 %. Documentos: `docs/ios-port/02B-{PORTABILITY-AUDIT,REUSE-INVENTORY,MIGRATION-ROADMAP,DEPENDENCY-MAP}.md`.
> Sin cambios funcionales; nada portado (analizado ≠ portado).

> 02A — DEVICE VERIFIED (2026-10-12, smoke test manual del propietario en iPhone 11 Pro): arranque sin cierres, renderizado
> Compose, navegación Songs/Albums/Artists/Playlists, datos demo visibles, detalles, regreso, sin cierres inesperados.
> Sin reproducción musical ni conectividad real (fuera de alcance por diseño).

> 02A — CI VERIFIED / DEVICE RETEST PENDING (2026-10-11, run #17 `38178379518`, commit `bd0e97a4`, ciclo CI 1/3): 4 jobs SUCCESS (`ios-framework`, `shared-tests` con 18/18 tests, `ios-app`, `ios-device-ipa` con 13/13 validaciones incl. plist booleano). IPA `MeldIOS-unsigned-device.ipa` (14.102.066 bytes, SHA-256 `9c49d72f…96c1cbb06`). UI musical en iPhone: NOT TESTED (prueba del propietario pendiente).

> 01D — DEVICE VERIFIED (manual smoke test, 2026-10-11): el propietario firmó el IPA 01D-FIX01 con 3uTools, lo instaló en iPhone 11 Pro y la app arrancó y renderizó (texto Kotlin visible, Compose OK, contador con 6 taps, sin el cierre inmediato anterior). Alcance: arranque + renderizado + interacción inicial. No se afirman funciones futuras.

> 01D-FIX01 CI VERIFIED / DEVICE RETEST PENDING (2026-10-11, ciclo CI 1/3): run #16 `38175702380` con los 4 jobs SUCCESS y 13/13 validaciones PASS (`CADisableMinimumFrameDurationOnPhone boolean true` en CI + `True [bool]` verificado en local). IPA `MeldIOS-unsigned-device.ipa` (13.054.877 bytes, SHA-256 `0ab228f9…dd587a`). Retest en iPhone pendiente del propietario (firma + instalación + arranque).

> 01C VERIFIED — UNSIGNED IPA GENERATED (2026-10-11): run #14 `38172210884` con los 4 jobs SUCCESS y 12/12 validaciones PASS (arm64, `platform IOS`, minos 16.0, sdk 26.4, bundle `com.eirom16.meldios`, estático, sin firma). IPA `MeldIOS-unsigned-device.ipa` (13.054.764 bytes, SHA-256 `57f6291d…369c1f0`, verificado también en local). Cupo consumido: 2/3. Instalación en iPhone: NOT TESTED.

> 01B-FIX03 CERRADO (2/3 ciclos, 2026-10-11): run #11 `38168783902` con los 3 jobs SUCCESS y `** BUILD SUCCEEDED **` (`arm64-apple-ios16.0-simulator`, Debug). `ARCHS=arm64` eliminó `ios_x64` (ciclo 1); `MainViewControllerKt.MainViewController()` resolvió el case Swift (ciclo 2, header línea 182). Artifacts `MeldIOS-simulator-app` (13,8 MB) + framework (66 MB) vigentes. 01B **VERIFIED**. 01C no iniciada. Conclusión FIX02 conservada: XcodeGen se mantiene porque ya funciona.

## 1. Objetivo general

Adaptar Meld (fork Android de Metrolist, `com.meld.app`, v0.9.2) a iOS con Kotlin Multiplatform + Compose Multiplatform, conservando la app Android funcional, sin reescribir en Swift, con reproductor iOS sobre AVFoundation y distribución inicial por sideloading (IPA), en fases pequeñas y verificables compiladas en GHA macOS (no hay Mac físico; dev en Linux).

## 2. Decisiones de arquitectura (consolidadas Fase 00 + 01A + 01B)

- D1. `app/` e `innertube/` permanecen en disco como referencia histórica, pero desde 02A están FUERA de la configuración Gradle activa (`settings.gradle.kts` solo incluye `:shared`); en 01A/01B no se tocó ningún fichero suyo.
- D2. `shared/` KMP iOS-only (`com.meld.shared`; targets iosArm64/iosSimulatorArm64 + `jvm()` solo como host de tests; framework estático `MeldShared`) con CMP: `compose.runtime/foundation/ui/material3` en commonMain. Sin target Android, sin `iosX64()`, sin `androidMain/`. Decisión iOS-only del propietario: sin compatibilidad/compilación/regresión Android.
- D3. Toolchain: Kotlin **2.4.20** · AGP **9.3.1** · Gradle **9.7.0** · KSP **2.3.11** (VERIFIED en Android por la run #4) · CMP **1.12.1** (clave `composeMultiplatform`, separada del BOM Jetpack).
- D4. UI iOS: `MeldApp()` (tema oscuro, `Greeting`, contador demo) + `MainViewController()` en fichero sin package → Swift `MainViewControllerKt.mainViewController()` (grep en `MeldShared.h` como árbitro CI).
- D5. Host `iosApp/` SwiftUI mínimo (`com.eirom16.meldios`, iOS 16.0, `CADisableMinimumFrameDurationOnPhone`); proyecto vía XcodeGen (`project.yml` versionado, `.xcodeproj` solo en CI); integración directa `embedAndSignAppleFrameworkForXcode` con `ENABLE_USER_SCRIPT_SANDBOXING=NO` y heaps capados por CLI.
- D6. Esquema Room intocable; `MusicService` no se porta; sin IPA/firma hasta 01C.
- D7. Nomenclatura Android-KMP (variante única): `compileAndroidMain`/`assembleAndroidMain`; iOS: `linkDebugFrameworkIos{SimulatorArm64,Arm64,X64}`.

## 3. Estado de cada fase

| Fase | Objetivo | Estado |
|---|---|---|
| 00 — Auditoría técnica | Diagnóstico + matriz A–E | **COMPLETE** 2026-10-09 (capítulos Fase 00 conservados bajo los de 01A/01B) |
| 01A — Fundación KMP + framework | Toolchain 2.4.20, `shared/`, workflow 2 jobs | **VERIFIED** (run #4 @ `34ba79a3`: frameworks sim+device PASS, `:app:assembleFossDebug` PASS, Xcode 26.4.1) |
| 01B — CMP + host iOS | `MeldApp()`, `MainViewController()`, `iosApp/`, job `ios-app` → `MeldIOS.app` | **VERIFIED** 2026-10-11 (run #11: 3 jobs SUCCESS, `BUILD SUCCEEDED` arm64 simulador, `MeldIOS.app` + header + artifacts verificados) |
| 01C — Generación de IPA | Archive iphoneos + ZIP `Payload/` sin firma, artifact `.ipa` | **VERIFIED** 2026-10-11 (run #14: 4 jobs SUCCESS, 12/12 checks, IPA 13.054.764 bytes) |
| 01D — Arranque en iPhone físico | Crash SIGABRT (`PlistSanityCheck`): IPA sin `CADisableMinimumFrameDurationOnPhone` | **DEVICE VERIFIED** 2026-10-11 (smoke test manual del propietario: IPA 01D-FIX01 firmado con 3uTools, arranque + render + 6 taps OK en iPhone 11 Pro; alcance limitado) |
| 02A — Fundación iOS-only + catálogo musical | Settings solo `:shared`; `MusicCatalogRepository` + demo + UI navegable + 18 tests | **DEVICE VERIFIED** 2026-10-12 (propietario, iPhone 11 Pro; alcance: UI demo, sin streaming) |
| 02B — Auditoría reutilización + plan migración | Inventario ~70 comp., mapa deps, roadmap P0→P1→P2, hallazgo iTX-iOS 0.6.0 | **AUDIT COMPLETE / MIGRATION PLAN READY** 2026-10-12 (pendiente push) |
| 02C — InnerTubeX iOS + búsqueda real | iTX v0.8.4 + Darwin + `search()` + UI Search + 41 tests | **IMPLEMENTED / PENDING_CI** 2026-10-12 (ciclo 1/3; live en iPhone pendiente del propietario) |
| 02C+ — Transporte real, modelos remotos, player AVFoundation | — | **NOT STARTED** |

## 4. Últimas fases concluidas

- Fase 00: inventario (531 `.kt`, 56 Media3, 54 Hilt, 223 JVM/Android-imports, 57 locales), matriz 19 componentes, bloqueantes B0–B6 / riesgos R1–R8.
- Fase 01A: 3 Gradle modificados, `shared/` + workflow creados; klib iOS en Linux; VERIFIED en CI (run #4).
- Fase 01B: CMP 1.12.1 integrado, pantalla demo + entry point + host SwiftUI + `project.yml` + job `ios-app`; klib con Compose compilado en local (`root_package` + `package_com.meld.shared.ui` presentes); 0 ficheros Android tocados.

## 5. Bloqueos abiertos

- 01D-FIX01 (ciclo 1/3, CI VERIFIED): run #16 `38175702380` (commit `2a53af13`) con 4 jobs SUCCESS + check 13 PASS. Retest en iPhone pendiente del propietario (firma + instalación + arranque con el IPA nuevo).
- CI: run #14 `38172210884` (commit `9a8c104a`) con 4 jobs SUCCESS + artifacts `MeldIOS-unsigned-device-ipa` (13.054.764 bytes, SHA-256 `57f6291d…369c1f0`) y `MeldIOS-device-validation` vigentes. 01C VERIFIED.
- FIX03 cerrado (run #11 verde): `ARCHS=arm64` + `MainViewController()` capitalizado; `iosX64()` sigue declarado sin molestar (el override basta); diagnóstico de header conservado en el workflow para futuros fallos.
- FIX02 cerrado: XcodeGen genera en CI (exit 0, runs #9–#11). XcodeGen se mantiene porque ya funciona.
- Incógnita menor: nombre exacto del símbolo Swift si la regla de facade difiriera (el grep de CI lo dictamina con candidatos en el log).
- XcodeGen latest: si una versión futura rompe `project.yml`, fijar versión.
- B2 InnerTubeX · B3 OkHttp · B4 `SpotifyAuth` JVM-only · B6 migraciones Room (fuera de alcance hasta Fase 02).
- Local: sin Android SDK, Xcode ni simulador (NOT TESTED por diseño).

## 6. Próximo objetivo

**Fase 02C** (IMPLEMENTED 2026-10-12, ciclo CI 1/3 PENDING): InnerTubeX v0.8.4
resolviendo para iosArm64/iosSimulatorArm64, transporte Darwin, `search()`
songs + UI Search + 41/41 tests en local. Tras el verde CI, el propietario
prueba en iPhone (búsqueda manual); hasta entonces NO declarar live verified.
Siguientes decisiones (P0.2/P0.3, requieren autorización nueva): D2 PoToken
iOS · cipher/extracción · player AVFoundation. No iniciarlas en esta fase.
Decisiones pendientes del propietario: D1 bump iTX (¿valida regresión Android?) · D2 PoToken iOS · D3 Room-KMP ·
D4 Koin/manual · D5 Router · D6 alcance Spotify · D7 orden P0 (recomendado: búsqueda+playback primero).

**Fase 02A — DEVICE VERIFIED** 2026-10-12 (ver §02B.0 en REPORT): UI musical verificada en iPhone del propietario.

**Fase 02B+** (NOT STARTED, requiere autorización nueva): transporte real (Ktor/Darwin, InnerTubeX pendiente B2), autenticación, persistencia, reproductor AVFoundation. No iniciar en esta fase.

## 7. Información crítica para el siguiente agente

- Rama `feat/ios-foundation`. Autonomía 02A vigente: commits + push solo a esta rama, máx. 3 ciclos CI (1/3 al hacer push de este ciclo), sin force-push/reset destructivo/merge/release/main. `.gitignore` preexistente (append 01B: `.xcodeproj` + `iosApp/build/`) analizado y justificado en RESULTS §02A.1: se incluye en el commit 02A. `AGENTS.md` vigente salvo §building (`:app:assembleFossDebug` retirado por iOS-only); solo los tres informes de `docs/ios-port/` son Markdown editable.
- Rutas clave 02A: `shared/src/commonMain/.../music/{Models,MusicCatalogRepository,DemoCatalog}.kt`, `shared/src/commonMain/.../ui/{MeldApp,CatalogComponents}.kt`, `shared/src/commonTest/.../music/{ModelsTest,RepositoryTest}.kt`, `shared/src/jvmMain/.../Platform.jvm.kt`, `settings.gradle.kts` (solo `:shared`), `.github/workflows/ios-foundation.yml` (jobs `ios-framework, shared-tests, ios-app, ios-device-ipa`). `iosApp/project.yml` + claves plist 01D intactos.
- Cifras ancla: 6135 lín. `MusicService`, 531 `.kt`, 56 Media3, 54 Hilt, 223 JVM/Android-imports, 57 locales, 20 entidades Room, 14 ops GQL Spotify.
- No afirmar VERIFIED sin GHA; no inventar comandos (reales en `RESULTS.md §01B.1`).
