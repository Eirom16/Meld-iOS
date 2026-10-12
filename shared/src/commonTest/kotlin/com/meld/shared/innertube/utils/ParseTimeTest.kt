package com.meld.shared.innertube.utils

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * `parseTime` portado (Fase 02C): la puerta de `duration` en los fixtures.
 */
class ParseTimeTest {
    @Test
    fun minutesAndSeconds() {
        assertEquals(225, "3:45".parseTime())
        assertEquals(754, "12.34".parseTime())
        assertEquals(3723, "1:02:03".parseTime())
    }

    @Test
    fun invalid_returnsNull() {
        assertNull("x".parseTime())
        assertNull("".parseTime())
        assertNull("1:2:3:4".parseTime())
    }
}
