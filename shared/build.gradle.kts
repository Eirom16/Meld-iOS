plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.compose.compiler)
}

kotlin {
    // Fase 02A iOS-only: sin target Android (el plugin androidKmpLibrary y el
    // bloque android{} se retiraron) y sin iosX64 (Tier-3, sin artefactos CMP
    // 1.12.1 para ios_x64; el override ARCHS=arm64 ya lo evitaba en Xcode).
    // El target jvm/ existe SOLO como host para ejecutar las pruebas de
    // lógica común en CI/Linux (tarea :shared:jvmTest); no es plataforma destino.
    jvm()

    listOf(
        iosArm64(),
        iosSimulatorArm64(),
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "MeldShared"
            isStatic = true
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.ui)
            implementation(compose.material3)
            // Fase 02C: InnerTubeX (motor YouTube Music) + transporte Ktor.
            // El engine concreto va por plataforma (Darwin en iosMain,
            // CIO solo como host JVM de pruebas); aquí solo el core común.
            implementation(libs.innertubex)
            implementation(libs.coroutines.core)
            implementation(libs.serialization.json)
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.json)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.coroutines.test)
            implementation(libs.ktor.client.mock)
        }
        jvmTest.dependencies {
            implementation(kotlin("test-junit"))
        }
        jvmMain.dependencies {
            implementation(libs.ktor.client.cio)
        }
        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
        }
    }
}
