package cc.modlabs.commands

import cc.modlabs.HytlConfigIO
import cc.modlabs.HytlPluginConfig
import cc.modlabs.info
import cc.modlabs.warn
import com.hypixel.hytale.server.core.Message
import com.hypixel.hytale.server.core.command.system.CommandContext
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase
import cc.modlabs.HytlDev

class HytlRootCommand(private val plugin: HytlDev) : CommandBase("hytl", "HYTL integration") {
    init {
        setOwner(plugin)
        addAliases("hytldev")
        addSubCommand(HytlSetupCommand(plugin))
        addSubCommand(HytlStatusCommand(plugin))
        addSubCommand(HytlReloadCommand(plugin))
    }

    override fun executeSync(context: CommandContext) {
        context.sendMessage(
            Message.raw("HYTL: use /hytl setup <serverId> <serverSecret> [backendBaseUrl] or /hytl status")
        )
    }
}

private class HytlSetupCommand(private val plugin: HytlDev) : CommandBase("setup", "Write HYTL serverId/serverSecret to config") {
    private val serverIdArg = withRequiredArg("serverId", "HYTL serverId", ArgTypes.STRING)
    private val serverSecretArg = withRequiredArg("serverSecret", "HYTL serverSecret (keep private)", ArgTypes.STRING)

    init {
        setOwner(plugin)
        // Default permission node is generated, but allow explicit requirement as well:
        requirePermission(plugin.basePermission + ".hytl.setup")

        // Allow a 3rd positional arg variant: /hytl setup <serverId> <serverSecret> <backendBaseUrl>
        addUsageVariant(HytlSetupCommandWithBackend(plugin))
    }

    override fun executeSync(context: CommandContext) {
        val serverId = serverIdArg.get(context).trim()
        val serverSecret = serverSecretArg.get(context).trim()
        handleSetup(plugin, context, serverId, serverSecret, backendBaseUrl = null)
    }
}

private class HytlSetupCommandWithBackend(private val plugin: HytlDev) :
    CommandBase("Write HYTL serverId/serverSecret/backendBaseUrl to config") {
    private val serverIdArg = withRequiredArg("serverId", "HYTL serverId", ArgTypes.STRING)
    private val serverSecretArg = withRequiredArg("serverSecret", "HYTL serverSecret (keep private)", ArgTypes.STRING)
    private val backendBaseUrlArg = withRequiredArg("backendBaseUrl", "Backend base url (e.g. https://hytl.dev/api/plugin)", ArgTypes.STRING)

    init {
        setOwner(plugin)
        requirePermission(plugin.basePermission + ".hytl.setup")
    }

    override fun executeSync(context: CommandContext) {
        val serverId = serverIdArg.get(context).trim()
        val serverSecret = serverSecretArg.get(context).trim()
        val backendBaseUrl = backendBaseUrlArg.get(context).trim()
        handleSetup(plugin, context, serverId, serverSecret, backendBaseUrl)
    }
}

private fun handleSetup(
    plugin: HytlDev,
    context: CommandContext,
    serverId: String,
    serverSecret: String,
    backendBaseUrl: String?
) {
    // Strongly recommended to run from console to avoid leaking the secret to nearby players/screenshots.
    if (context.isPlayer) {
        context.sendMessage(
            Message.raw("For security, run this from console. It contains your serverSecret.")
        )
        return
    }

    if (serverId.isBlank() || serverSecret.isBlank()) {
        context.sendMessage(Message.raw("serverId/serverSecret must not be blank."))
        return
    }

    val existing = HytlConfigIO.loadOrCreate(plugin.dataDirectory)
    val updated = existing.copy(
        serverId = serverId,
        serverSecret = serverSecret,
        backendBaseUrl = backendBaseUrl ?: existing.backendBaseUrl
    )
    HytlConfigIO.save(plugin.dataDirectory, updated)

    // Apply immediately.
    plugin.applyNewConfig(updated)

    context.sendMessage(
        Message.raw("HYTL configured and saved to ${HytlConfigIO.configPath(plugin.dataDirectory)}. Heartbeat/votes should start shortly.")
    )
}

private class HytlReloadCommand(private val plugin: HytlDev) : CommandBase("reload", "Reload HYTL config from disk") {
    init {
        setOwner(plugin)
        requirePermission(plugin.basePermission + ".hytl.reload")
    }

    override fun executeSync(context: CommandContext) {
        val cfg = HytlConfigIO.loadOrCreate(plugin.dataDirectory)
        plugin.applyNewConfig(cfg)
        context.sendMessage(Message.raw("HYTL config reloaded from ${HytlConfigIO.configPath(plugin.dataDirectory)}"))
    }
}

private class HytlStatusCommand(private val plugin: HytlDev) : CommandBase("status", "Show HYTL plugin status") {
    init {
        setOwner(plugin)
        requirePermission(plugin.basePermission + ".hytl.status")
    }

    override fun executeSync(context: CommandContext) {
        val cfg = plugin.currentConfigSnapshot()
        val hasCreds = cfg.serverId.isNotBlank() && cfg.serverSecret.isNotBlank()
        val msg = buildString {
            append("HYTL status:\n")
            append("- config: ").append(HytlConfigIO.configPath(plugin.dataDirectory)).append('\n')
            append("- configured: ").append(hasCreds).append('\n')
            append("- serverId: ").append(if (cfg.serverId.isBlank()) "<empty>" else cfg.serverId).append('\n')
            append("- backendBaseUrl: ").append(cfg.backendBaseUrl).append('\n')
            append("- heartbeatSeconds: ").append(cfg.heartbeatSeconds).append('\n')
            append("- votePollSeconds: ").append(cfg.votePollSeconds).append('\n')
        }
        context.sendMessage(Message.raw(msg))
    }
}

