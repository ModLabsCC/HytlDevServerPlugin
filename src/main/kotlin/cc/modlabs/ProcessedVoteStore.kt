package cc.modlabs

import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap

class ProcessedVoteStore(private val path: Path) {
    private val processed = ConcurrentHashMap.newKeySet<String>()

    fun load() {
        if (!Files.exists(path)) return
        Files.readAllLines(path)
            .asSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .forEach { processed.add(it) }
    }

    fun has(voteId: String): Boolean = processed.contains(voteId)

    /**
     * Marks a voteId as processed and persists it immediately.
     * This is intentionally "append only" so it stays robust across crashes.
     */
    fun markProcessed(voteId: String) {
        val added = processed.add(voteId)
        if (!added) return
        Files.createDirectories(path.parent)
        Files.writeString(
            path,
            voteId + System.lineSeparator(),
            java.nio.file.StandardOpenOption.CREATE,
            java.nio.file.StandardOpenOption.WRITE,
            java.nio.file.StandardOpenOption.APPEND
        )
    }
}

