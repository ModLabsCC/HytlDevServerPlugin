package cc.modlabs

import com.hypixel.hytale.logger.HytaleLogger
import java.util.logging.Level

fun HytaleLogger.info(msg: String) {
    at(Level.INFO).log(msg)
}

fun HytaleLogger.warn(msg: String) {
    at(Level.WARNING).log(msg)
}

fun HytaleLogger.debug(msg: String) {
    at(Level.FINE).log(msg)
}

fun HytaleLogger.error(msg: String, t: Throwable? = null) {
    if (t != null) {
        at(Level.SEVERE).withCause(t).log(msg)
    } else {
        at(Level.SEVERE).log(msg)
    }
}

