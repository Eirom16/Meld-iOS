plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidKmpLibrary)
}

kotlin {
    android {
        namespace = "com.meld.shared"
        compileSdk = 37
        minSdk = 26
    }

    listOf(
        iosX64(),
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
        }
        androidMain.dependencies {
        }
        iosMain.dependencies {
        }
    }
}
