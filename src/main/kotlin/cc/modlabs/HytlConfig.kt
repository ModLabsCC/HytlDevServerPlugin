package cc.modlabs

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.nio.file.Files
import java.nio.file.Path

@Serializable
data class HytlPluginConfig(
    val serverId: String = "",
    val serverSecret: String = "",
    val backendBaseUrl: String = "https://hytl.dev/api/plugin",
    val heartbeatSeconds: Int = 10,
    val votePollSeconds: Int = 15,
    val privacy: PrivacyConfig = PrivacyConfig(),
    val voteRewards: VoteRewardsConfig = VoteRewardsConfig(),
    val httpTimeoutMs: Long = 3_000,
    val maxBackoffSeconds: Int = 60,
    // Optional convenience: override the public host/port shown in HYTL.
    val publicHost: String? = null,
    val publicPort: Int? = null,
    // Optional convenience: override server version/patchline reported to HYTL.
    val gameVersion: String? = null,
    val patchline: String? = null
)

@Serializable
data class PrivacyConfig(
    val shareMods: Boolean = true,
    val sharePlayerCount: Boolean = true,
    val sharePlayerNames: Boolean = true,
    val sharePlayerUUIDs: Boolean = false
)

@Serializable
data class VoteRewardsConfig(
    /**
     * LOG_ONLY: logs what would be rewarded and ACKs votes (default).
     * DISABLED: does not reward and does not ACK (votes stay pending).
     */
    val mode: String = "LOG_ONLY",
    val baseAmount: Int = 1,
    /**
     * Tier thresholds (inclusive) by streak. Example default tiers:
     * 1 -> tier 1, 3 -> tier 2, 7 -> tier 3, 14 -> tier 4, 30 -> tier 5.
     */
    val tierStreakThresholds: List<Int> = listOf(1, 3, 7, 14, 30),
    /**
     * Amount multipliers per tier index (1-based). Default: [1,2,3,4,5].
     */
    val tierMultipliers: List<Int> = listOf(1, 2, 3, 4, 5)
)

object HytlConfigIO {
    private val json = Json {
        prettyPrint = true
        encodeDefaults = true
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    const val CONFIG_FILE_NAME: String = "config.json"

    fun configPath(dataDir: Path): Path = dataDir.resolve(CONFIG_FILE_NAME)

    /**
     * Loads config from the plugin data directory. If missing, writes a starter file and returns defaults.
     */
    fun loadOrCreate(dataDir: Path): HytlPluginConfig {
        Files.createDirectories(dataDir)
        val path = configPath(dataDir)
        if (!Files.exists(path)) {
            val starter = HytlPluginConfig()
            Files.writeString(path, json.encodeToString(HytlPluginConfig.serializer(), starter))
            return starter
        }
        val raw = Files.readString(path)
        return json.decodeFromString(HytlPluginConfig.serializer(), raw)
    }

    fun save(dataDir: Path, cfg: HytlPluginConfig) {
        Files.createDirectories(dataDir)
        val path = configPath(dataDir)
        Files.writeString(path, json.encodeToString(HytlPluginConfig.serializer(), cfg))
    }
}

