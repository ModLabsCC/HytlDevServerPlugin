package cc.modlabs

import com.hypixel.hytale.server.core.plugin.JavaPlugin
import com.hypixel.hytale.server.core.plugin.JavaPluginInit
import com.hypixel.hytale.server.core.HytaleServer
import com.hypixel.hytale.server.core.event.events.player.PlayerConnectEvent
import com.hypixel.hytale.server.core.event.events.player.PlayerDisconnectEvent
import io.ktor.client.HttpClient
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import cc.modlabs.api.HytlDevApi
import cc.modlabs.commands.HytlRootCommand
import java.util.concurrent.atomic.AtomicLong

class HytlDev(init: JavaPluginInit) : JavaPlugin(init) {

    private val running = AtomicBoolean(false)
    private val onlinePlayers = OnlinePlayersTracker()

    private lateinit var config: HytlPluginConfig
    private var http: HttpClient? = null

    private var heartbeatFuture: ScheduledFuture<*>? = null
    private var heartbeatBackoff: ExponentialBackoff? = null
    private var heartbeatService: HeartbeatService? = null
    private var heartbeatNudgeFuture: ScheduledFuture<*>? = null
    private val lastHeartbeatNudgeAtMs = AtomicLong(0L)

    private var voteFuture: ScheduledFuture<*>? = null
    private var voteBackoff: ExponentialBackoff? = null
    private var voteService: VoteService? = null
    private var processedVotes: ProcessedVoteStore? = null

    override fun setup() {
        config = HytlConfigIO.loadOrCreate(dataDirectory)
        onlinePlayers.register(this)

        // Trigger an extra heartbeat shortly after join/leave (coalesced with 1s min cooldown).
        eventRegistry.register(PlayerConnectEvent::class.java) {
            requestHeartbeatNudge()
        }
        eventRegistry.register(PlayerDisconnectEvent::class.java) {
            requestHeartbeatNudge()
        }

        logger.info("HYTL plugin config: ${HytlConfigIO.configPath(dataDirectory)}")
        if (config.serverId.isBlank() || config.serverSecret.isBlank()) {
            logger.warn("HYTL plugin is not configured yet (serverId/serverSecret empty). Fill ${HytlConfigIO.CONFIG_FILE_NAME} to enable heartbeat/votes.")
        }

        processedVotes = ProcessedVoteStore(dataDirectory.resolve("processed-votes.txt")).also {
            it.load()
        }

        // Commands
        commandRegistry.registerCommand(HytlRootCommand(this))
    }

    override fun start() {
        running.set(true)
        startRuntimeIfConfigured()
    }

    override fun shutdown() {
        running.set(false)
        stopRuntime(sendFinalHeartbeat = true)
    }

    /**
     * Called by /hytl setup and /hytl reload to apply new credentials/config at runtime.
     */
    fun applyNewConfig(newConfig: HytlPluginConfig) {
        config = newConfig
        if (!running.get()) return
        stopRuntime(sendFinalHeartbeat = false)
        startRuntimeIfConfigured()
    }

    /**
     * Used by /hytl status for safe introspection (does not expose secret).
     */
    fun currentConfigSnapshot(): HytlPluginConfig = config.copy(serverSecret = if (config.serverSecret.isBlank()) "" else "********")

    private fun startRuntimeIfConfigured() {
        if (config.serverId.isBlank() || config.serverSecret.isBlank()) return

        http = HytlHttp.create(config)
        heartbeatBackoff = ExponentialBackoff(
            baseSeconds = 1,
            maxSeconds = config.maxBackoffSeconds.toLong()
        )
        heartbeatService = HeartbeatService(
            http = http!!,
            config = config,
            onlinePlayers = onlinePlayers
        )
        scheduleHeartbeat(0)

        voteBackoff = ExponentialBackoff(
            baseSeconds = 1,
            maxSeconds = config.maxBackoffSeconds.toLong()
        )
        voteService = VoteService(
            http = http!!,
            config = config,
            processed = processedVotes!!,
            rewarder = (HytlDevApi.getVoteRewarder() ?: DefaultVoteRewarder(config, logger))
        )
        scheduleVotePoll(0)
    }

    private fun stopRuntime(sendFinalHeartbeat: Boolean) {
        heartbeatFuture?.cancel(false)
        heartbeatFuture = null
        heartbeatNudgeFuture?.cancel(false)
        heartbeatNudgeFuture = null
        voteFuture?.cancel(false)
        voteFuture = null

        if (sendFinalHeartbeat) {
            try {
                heartbeatService?.sendOnce { t ->
                    logger.debug("Final heartbeat failed: ${t.message}")
                }
            } catch (_: Throwable) {
                // ignore
            }
        }

        heartbeatService = null
        voteService = null
        heartbeatBackoff = null
        voteBackoff = null

        http?.close()
        http = null
    }

    /**
     * Request an additional heartbeat outside the normal schedule.
     * This is coalesced with a 1s minimum cooldown to avoid spamming.
     */
    private fun requestHeartbeatNudge() {
        if (!running.get()) return
        if (config.serverId.isBlank() || config.serverSecret.isBlank()) return
        if (heartbeatService == null) return

        val now = System.currentTimeMillis()
        val last = lastHeartbeatNudgeAtMs.get()
        val elapsed = now - last
        val delayMs = (1_000L - elapsed).coerceAtLeast(0L)

        // Coalesce multiple join/leave events into a single scheduled send.
        heartbeatNudgeFuture?.cancel(false)
        heartbeatNudgeFuture = HytaleServer.SCHEDULED_EXECUTOR.schedule({
            val now2 = System.currentTimeMillis()
            lastHeartbeatNudgeAtMs.set(now2)
            val svc = heartbeatService ?: return@schedule
            // Fire-and-forget; do not disturb the main heartbeat schedule/backoff.
            svc.sendOnce { t ->
                logger.debug("Heartbeat nudge failed: ${t.message}")
            }
        }, delayMs, TimeUnit.MILLISECONDS)
    }

    private fun scheduleHeartbeat(delaySeconds: Long) {
        heartbeatFuture?.cancel(false)
        heartbeatFuture = HytaleServer.SCHEDULED_EXECUTOR.schedule({
            if (!running.get()) return@schedule
            val svc = heartbeatService ?: return@schedule
            val backoff = heartbeatBackoff ?: return@schedule

            val ok = svc.sendOnce { t ->
                logger.warn("Heartbeat POST failed: ${t.message}")
            }

            if (ok) {
                backoff.reset()
                scheduleHeartbeat(config.heartbeatSeconds.toLong())
            } else {
                val next = backoff.nextDelaySeconds()
                scheduleHeartbeat(next)
            }
        }, delaySeconds, TimeUnit.SECONDS)
    }

    private fun scheduleVotePoll(delaySeconds: Long) {
        voteFuture?.cancel(false)
        voteFuture = HytaleServer.SCHEDULED_EXECUTOR.schedule({
            if (!running.get()) return@schedule
            val svc = voteService ?: return@schedule
            val backoff = voteBackoff ?: return@schedule

            val ok = svc.pollAndProcessOnce { t ->
                logger.warn("Vote poll failed: ${t.message}")
            }

            if (ok) {
                backoff.reset()
                scheduleVotePoll(config.votePollSeconds.toLong())
            } else {
                val next = backoff.nextDelaySeconds()
                scheduleVotePoll(next)
            }
        }, delaySeconds, TimeUnit.SECONDS)
    }
}