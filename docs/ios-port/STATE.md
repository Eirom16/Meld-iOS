# STATE.md — Estado de continuidad del port Meld → iOS

Última actualización (UTC): 2026-10-09 · Rama `feat/ios-foundation` (local, desde `main` @ `2ae37b17e55cc024ec5ba7d44c267cfb2aad995d`, sin commit/push) · `git diff --check` limpio.

## 1. Objetivo general

Adaptar Meld (fork Android de Metrolist, `com.meld.app`, v0.9.2) a iOS con Kotlin Multiplatform + Compose Multiplatform, conservando la app Android funcional, sin reescribir en Swift, con reproductor iOS sobre AVFoundation y distribución inicial por sideloading (IPA), en fases pequeñas y verificables compiladas en GHA macOS (no hay Mac físico; dev en Linux).

## 2. Decisiones de arquitectura (consolidadas Fase 00 + 01A)

- D1. `app/` e `innertube/` permanecen módulos Android puros; no se convierten a KMP (AGP 9 lo prohíbe en el mismo módulo). En 01A no se tocó ningún fichero suyo.
- D2. Nuevo módulo `shared/` (KMP) con `org.jetbrains.kotlin.multiplatform` + `com.android.kotlin.multiplatform.library` (namespace `com.meld.shared`, compileSdk 37, minSdk 26) + futuro `iosApp/`. Targets: android, iosX64, iosArm64, iosSimulatorArm64; framework estático `MeldShared`.
- D3. Toolchain fijado en 01A: Kotlin **2.4.20** (AGP 9.3.1 y Gradle 9.7.0 dentro de su matriz oficial), KSP 2.3.11 conservado pendiente del veredicto del Job B. Sintaxis Android del módulo KMP: bloque `kotlin { android { … } }`.
- D4. Fronteras `expect/actual` previstas (sin implementar salvo `platformName()`): motor HTTP Darwin/CIO, auth Spotify, logging, DataStore-core + Okio, Room `@ConstructedBy`, `PlayerController` Media3/AVFoundation, descargas, navegación.
- D5. Esquema Room intocable; `MusicService` (6135 lín.) no se porta.
- D6. Nomenclatura real del plugin Android-KMP (variante única): `compileAndroidMain` / `assembleAndroidMain` / `bundleAndroidMainAar` (NO `compileDebugKotlinAndroid`); tareas iOS `linkDebugFrameworkIos{SimulatorArm64,Arm64,X64}`.
- D7. CI: workflow propio `ios-foundation.yml` (`macos-26`, Xcode 26.4.1 si disponible, Temurin 21, heaps capados solo por CLI); workflows Android existentes intactos.

## 3. Estado de cada fase

| Fase | Objetivo | Estado |
|---|---|---|
| 00 — Auditoría técnica y preparación | Diagnóstico con rutas reales + matriz A–E + plan Fase 01 | **COMPLETE** 2026-10-09 (ver REPORT §§1–8 y RESULTS §§1–6 originales, conservados bajo los capítulos 01A) |
| 01A — Fundación KMP: `shared/` mínimo + framework `MeldShared` + CI macOS + Android intacto | Toolchain 2.4.20, `Greeting` expect/actual, workflow de 2 jobs | **IMPLEMENTED** 2026-10-09 (evidencia local: configuración PASS, klib iOS PASS, framework y regresión Android PENDING_CI) |
| 01B — Compose Multiplatform y host iOS | Plugin CMP en `shared/`, pantalla demo, `iosApp/` SwiftUI+CMP, CI de simulador | **NOT STARTED** (requiere jobs 01A en verde) |
| 02+ — Transporte MP, modelos en `commonMain`, Room/DataStore, player AVFoundation | Bloqueadas hasta 01B; InnerTubeX (B2) sigue siendo la incógnita funcional mayor | **NOT STARTED** |

## 4. Últimas fases concluidas

- Fase 00: inventario verificado (531 `.kt`, 56 ficheros Media3, 54 Hilt, 223 con imports JVM/Android, 57 locales), matriz de 19 componentes, 7 bloqueantes (B0–B6) y 8 riesgos (R1–R8). B0 resuelto en 01A (Kotlin 2.4.20); B1 resuelto (plugin Android-KMP en módulo nuevo).
- Fase 01A: 3 ficheros Gradle modificados (+6/−1), 5 ficheros creados (`shared/` ×4 + workflow), 0 ficheros Android tocados. klib `package_com.meld.shared` compilado para iosSimulatorArm64 en Linux; link `.framework` omitido por host (requiere macOS).

## 5. Bloqueos abiertos

- CI pendiente: el propietario debe subir `feat/ios-foundation` y reportar los jobs (`ios-framework` en `macos-26`, `android-regression` en Ubuntu). Sin eso no hay VERIFIED.
- B5/KSP: si el Job B falla, el primer sospechoso es KSP 2.3.11 bajo Kotlin 2.4.20.
- B2 InnerTubeX sin variante Native · B3 OkHttp fijado · B4 `SpotifyAuth` JVM-only · B6 migraciones Room sin validar (todos fuera de alcance 01A/01B).
- Entorno local: sin Android SDK ni Xcode (muro documentado, no subsanado a propósito); `notes/` vacío.

## 6. Próximo objetivo

**Fase 01B — Compose Multiplatform y host iOS** (NO INICIADA, no comenzarla hasta el verde de CI): añadir `org.jetbrains.compose` a `shared/`, pantalla que muestre `Greeting().greet()`, crear `iosApp/` con `ComposeUIViewController`, extender CI al build del simulador. Detalle pendiente de redactar en su prompt.

## 7. Información crítica para el siguiente agente

- Rama de trabajo: `feat/ios-foundation` (local, sin publicar). No hacer commit/push/merge. `AGENTS.md` vigente: no compilar sin permiso del usuario (la Fase 01A traía autorización explícita ya consumida), no tocar README/AGENTS/docs ajenas, strings solo en `values/metrolist_strings.xml` inglesa, no cambiar esquema DB ni versión.
- Rutas clave 01A: `shared/build.gradle.kts`, `shared/src/*/kotlin/com/meld/shared/`, `.github/workflows/ios-foundation.yml`, `gradle/libs.versions.toml:7,112-116`, `settings.gradle.kts:40-43`.
- Cifras ancla: 6135 lín. `MusicService`, 531 `.kt`, 56 Media3, 54 Hilt, 223 JVM/Android-imports, 57 locales, 20 entidades Room, 14 ops GQL Spotify.
- No afirmar VERIFIED sin los resultados de GitHub Actions; no inventar comandos (los reales están en `RESULTS.md §01A.1`).
