package cc.modlabs.api

import kotlin.jvm.JvmRecord

@JvmRecord
data class PendingVote(
    val voteId: String,
    val createdAt: String?,
    val playerName: String?,
    val playerUuid: String?,
    val streak: Int
)

