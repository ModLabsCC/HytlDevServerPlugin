package cc.modlabs

import kotlin.math.min
import kotlin.random.Random

class ExponentialBackoff(
    private val baseSeconds: Long = 1,
    private val maxSeconds: Long = 60,
    private val jitterRatio: Double = 0.2
) {
    private var failures: Int = 0

    fun reset() {
        failures = 0
    }

    fun nextDelaySeconds(): Long {
        failures = min(failures + 1, 30) // avoid overflow
        val exp = 1L shl min(failures - 1, 20)
        val raw = min(maxSeconds, baseSeconds * exp)

        val jitter = (raw * jitterRatio).toLong().coerceAtLeast(0)
        val delta = if (jitter == 0L) 0L else Random.nextLong(-jitter, jitter + 1)
        return (raw + delta).coerceIn(1, maxSeconds)
    }
}

