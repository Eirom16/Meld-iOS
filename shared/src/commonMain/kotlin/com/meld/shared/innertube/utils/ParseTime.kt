package com.meld.shared.innertube.utils

/**
 * Procedencia: `innertube/.../utils/Utils.kt` (`String.parseTime()`).
 *
 * Adaptación 02C: solo se porta `parseTime` (Kotlin puro). No se portan
 * `sha1` (requiere `java.security.MessageDigest`, JVM-only) ni las
 * extensiones `completed()` (dependen del objeto `YouTube` completo y de
 * páginas de playlist/biblioteca, fuera del alcance de búsqueda).
 */
fun String.parseTime(): Int? {
    try {
        // YouTube Music returns duration with locale-dependent separators
        // (":" en-US, "." some locales, "," EU). Accept all.
        val parts = split(Regex("[:.,]")).map { it.toInt() }
        if (parts.size == 2) {
            return parts[0] * 60 + parts[1]
        }
        if (parts.size == 3) {
            return parts[0] * 3600 + parts[1] * 60 + parts[2]
        }
    } catch (e: Exception) {
        return null
    }
    return null
}
