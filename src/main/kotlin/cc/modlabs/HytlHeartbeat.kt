package cc.modlabs

import com.hypixel.hytale.server.core.HytaleServer
import com.hypixel.hytale.server.core.HytaleServerConfig
import com.hypixel.hytale.server.core.plugin.PluginManager
import io.ktor.client.HttpClient
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.isSuccess
import kotlinx.coroutines.runBlocking
import java.time.Instant

class HeartbeatService(
    private val http: HttpClient,
    private val config: HytlPluginConfig,
    private val onlinePlayers: OnlinePlayersTracker
) {
    fun sendOnce(onError: (Throwable) -> Unit): Boolean {
        val payload = buildPayload()
        return try {
            val ok = runBlocking {
                val resp: HttpResponse = http.post("${config.backendBaseUrl}/heartbeat") {
                    setBody(payload)
                }
                resp.status.isSuccess()
            }
            ok
        } catch (t: Throwable) {
            onError(t)
            false
        }
    }

    private fun buildPayload(): HeartbeatPayload {
        val serverCfg: HytaleServerConfig = HytaleServer.get().config

        val motdLines = serverCfg.motd
            ?.split("\n")
            ?.map { it.trimEnd('\r') }
            ?.filter { it.isNotBlank() }
            ?.takeIf { it.isNotEmpty() }

        val maxPlayers = serverCfg.maxPlayers.takeIf { it > 0 }

        val universe = if (!config.privacy.sharePlayerCount) {
            UniverseInfo(currentPlayers = -1, defaultWorld = serverCfg.defaults.world)
        } else {
            UniverseInfo(
                currentPlayers = onlinePlayers.count(),
                defaultWorld = serverCfg.defaults.world
            )
        }

        val players = if (!config.privacy.sharePlayerNames) {
            null
        } else {
            onlinePlayers.list().map { p ->
                PlayerInfo(
                    name = p.name,
                    uuid = if (config.privacy.sharePlayerUUIDs) p.uuid.toString() else null,
                    world = p.world
                )
            }
        }

        val mods = if (!config.privacy.shareMods) {
            ModsInfo(enabled = false, list = null)
        } else {
            val entries = PluginManager.get().plugins
                .map { plugin ->
                    ModEntry(
                        id = "${plugin.manifest.group}:${plugin.manifest.name}",
                        version = plugin.manifest.version.toString(),
                        state = plugin.state.name
                    )
                }
            ModsInfo(enabled = true, list = entries)
        }

        val host = config.publicHost
        val port = config.publicPort ?: HytaleServer.DEFAULT_PORT

        val gameVersion = config.gameVersion ?: HytaleServer::class.java.`package`?.implementationVersion
        val patchline = config.patchline

        return HeartbeatPayload(
            serverId = config.serverId,
            ts = Instant.now().toString(),
            game = GameInfo(
                host = host,
                port = port,
                version = gameVersion,
                patchline = patchline,
                maxPlayers = maxPlayers,
                motd = motdLines
            ),
            universe = universe,
            players = players,
            mods = mods
        )
    }
}

