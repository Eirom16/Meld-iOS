package com.meld.shared

/**
 * Minimal entry point of the shared Kotlin Multiplatform module.
 * Phase 01A: proves the KMP toolchain (Android + iOS targets) without
 * pulling any Android-only dependency (no Hilt, Room, Media3, InnerTubeX).
 */
class Greeting {
    fun greet(): String = "Meld-iOS shared · platform=${platformName()}"
}

expect fun platformName(): String
