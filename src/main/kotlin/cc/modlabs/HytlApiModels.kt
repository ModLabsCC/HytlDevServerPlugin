package cc.modlabs

import kotlinx.serialization.Serializable

@Serializable
data class HeartbeatPayload(
    val serverId: String,
    val ts: String,
    val game: GameInfo,
    val universe: UniverseInfo? = null,
    val players: List<PlayerInfo>? = null,
    val mods: ModsInfo? = null
)

@Serializable
data class GameInfo(
    val host: String? = null,
    val port: Int? = null,
    val version: String? = null,
    val patchline: String? = null,
    val maxPlayers: Int? = null,
    val motd: List<String>? = null
)

@Serializable
data class UniverseInfo(
    val currentPlayers: Int? = null,
    val defaultWorld: String? = null
)

@Serializable
data class PlayerInfo(
    val name: String,
    val uuid: String? = null,
    val world: String? = null
)

@Serializable
data class ModsInfo(
    val enabled: Boolean,
    val list: List<ModEntry>? = null
)

@Serializable
data class ModEntry(
    val id: String,
    val version: String? = null,
    val state: String? = null
)

@Serializable
data class PendingVotesResponse(
    val serverId: String,
    val events: List<VoteEvent> = emptyList()
)

@Serializable
data class VoteEvent(
    val voteId: String,
    val createdAt: String? = null,
    val playerName: String? = null,
    val playerUuid: String? = null,
    val streak: Int? = null
)

@Serializable
data class VoteAckRequest(
    val serverId: String,
    val voteId: String
)

@Serializable
data class VoteAckResponse(
    val ok: Boolean = false,
    val acked: Boolean = false
)

