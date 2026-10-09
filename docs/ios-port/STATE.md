# STATE.md — Estado de continuidad del port Meld → iOS

Última actualización (UTC): 2026-10-09 · Rama `feat/ios-foundation` @ `c97f11ff` (sin commit/push en esta fase) · `git diff --check` limpio.

> 01B-FIX01 (2026-10-09): run #5 con `ios-app` FAIL (XcodeGen 2.46.0 SIGTRAP en "Generating project..."). Corregido con XcodeGen **2.45.4 fijado y verificado por SHA-256** + diagnóstico automático (log + crash reports macOS + artifact en `failure()`). `project.yml` sin errores verificables → sin cambios. 01B sigue IMPLEMENTED / PENDING_CI.

## 1. Objetivo general

Adaptar Meld (fork Android de Metrolist, `com.meld.app`, v0.9.2) a iOS con Kotlin Multiplatform + Compose Multiplatform, conservando la app Android funcional, sin reescribir en Swift, con reproductor iOS sobre AVFoundation y distribución inicial por sideloading (IPA), en fases pequeñas y verificables compiladas en GHA macOS (no hay Mac físico; dev en Linux).

## 2. Decisiones de arquitectura (consolidadas Fase 00 + 01A + 01B)

- D1. `app/` e `innertube/` permanecen módulos Android puros; en 01A/01B no se tocó ningún fichero suyo.
- D2. `shared/` KMP (`com.meld.shared`, compileSdk 37, minSdk 26; targets android/iosX64/iosArm64/iosSimulatorArm64; framework estático `MeldShared`) ahora con CMP: `compose.runtime/foundation/ui/material3` en commonMain.
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
| 01B — CMP + host iOS | `MeldApp()`, `MainViewController()`, `iosApp/`, job `ios-app` → `MeldIOS.app` | **IMPLEMENTED** 2026-10-09 (klib iOS con CMP compilado en local PASS; `.app` y regresión PENDING_CI) |
| 01C — Generación de IPA | Archive + export sin firma / `Payload/` para sideloading | **NOT STARTED** (requiere job `ios-app` en verde) |
| 02+ — Transporte MP, modelos, Room/DataStore, player AVFoundation | — | **NOT STARTED** (B2 InnerTubeX sigue siendo la incógnita mayor) |

## 4. Últimas fases concluidas

- Fase 00: inventario (531 `.kt`, 56 Media3, 54 Hilt, 223 JVM/Android-imports, 57 locales), matriz 19 componentes, bloqueantes B0–B6 / riesgos R1–R8.
- Fase 01A: 3 Gradle modificados, `shared/` + workflow creados; klib iOS en Linux; VERIFIED en CI (run #4).
- Fase 01B: CMP 1.12.1 integrado, pantalla demo + entry point + host SwiftUI + `project.yml` + job `ios-app`; klib con Compose compilado en local (`root_package` + `package_com.meld.shared.ui` presentes); 0 ficheros Android tocados.

## 5. Bloqueos abiertos

- CI pendiente: subir `feat/ios-foundation` y reportar el job `ios-app` (y re-verde de los otros dos). FIX01 aplicado: si `xcodegen-diagnostics` aparece, revisar log + `.ips` antes de tocar el spec.
- Incógnita menor: nombre exacto del símbolo Swift si la regla de facade difiriera (el grep de CI lo dictamina con candidatos en el log).
- XcodeGen latest: si una versión futura rompe `project.yml`, fijar versión.
- B2 InnerTubeX · B3 OkHttp · B4 `SpotifyAuth` JVM-only · B6 migraciones Room (fuera de alcance hasta Fase 02).
- Local: sin Android SDK, Xcode ni simulador (NOT TESTED por diseño).

## 6. Próximo objetivo

**Fase 01C — generación de IPA** (NO INICIADA, no comenzarla hasta el verde del job `ios-app`): `xcodebuild archive` + export sin firma (o `Payload/` manual) para sideloading, artifact `.ipa`, manteniendo cero firma Apple.

## 7. Información crítica para el siguiente agente

- Rama `feat/ios-foundation` (publicar = tarea del propietario). No commit/push/merge. `AGENTS.md` vigente; solo los tres informes de `docs/ios-port/` son Markdown editable.
- Rutas clave 01B: `shared/src/commonMain/.../ui/MeldApp.kt`, `shared/src/iosMain/.../MainViewController.kt`, `iosApp/project.yml`, `iosApp/MeldIOS/*`, `.github/workflows/ios-foundation.yml` (job `ios-app`), `gradle/libs.versions.toml` (clave `composeMultiplatform`).
- Cifras ancla: 6135 lín. `MusicService`, 531 `.kt`, 56 Media3, 54 Hilt, 223 JVM/Android-imports, 57 locales, 20 entidades Room, 14 ops GQL Spotify.
- No afirmar VERIFIED sin GHA; no inventar comandos (reales en `RESULTS.md §01B.1`).
