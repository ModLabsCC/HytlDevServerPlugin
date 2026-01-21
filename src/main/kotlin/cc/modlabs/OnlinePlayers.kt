package cc.modlabs

import com.hypixel.hytale.server.core.event.events.player.PlayerConnectEvent
import com.hypixel.hytale.server.core.event.events.player.PlayerDisconnectEvent
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

data class OnlinePlayerSnapshot(
    val uuid: UUID,
    val name: String,
    val world: String? = null
)

class OnlinePlayersTracker {
    private val players = ConcurrentHashMap<UUID, OnlinePlayerSnapshot>()

    fun register(plugin: HytlDev) {
        plugin.eventRegistry.register(PlayerConnectEvent::class.java) { e ->
            val ref = e.playerRef
            players[ref.uuid] = OnlinePlayerSnapshot(
                uuid = ref.uuid,
                name = ref.username,
                world = e.world?.name
            )
        }
        plugin.eventRegistry.register(PlayerDisconnectEvent::class.java) { e ->
            players.remove(e.playerRef.uuid)
        }
    }

    fun count(): Int = players.size

    fun list(): List<OnlinePlayerSnapshot> = players.values.toList()
}

