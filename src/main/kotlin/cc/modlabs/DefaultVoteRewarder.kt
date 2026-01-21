package cc.modlabs

import cc.modlabs.api.PendingVote
import cc.modlabs.api.VoteRewarder
import com.hypixel.hytale.logger.HytaleLogger

class DefaultVoteRewarder(
    private val cfg: HytlPluginConfig,
    private val logger: HytaleLogger
) : VoteRewarder {
    override fun reward(vote: PendingVote): Boolean {
        val mode = cfg.voteRewards.mode.uppercase()
        if (mode == "DISABLED") return false

        val tier = computeTier(vote.streak)
        val multiplier = cfg.voteRewards.tierMultipliers.getOrNull(tier - 1) ?: 1
        val amount = cfg.voteRewards.baseAmount * multiplier

        val who = vote.playerUuid ?: vote.playerName ?: "<unknown>"
        logger.info("Rewarding voteId=${vote.voteId} player=$who streak=${vote.streak} tier=$tier amount=$amount (default LOG_ONLY)")

        // Default implementation is intentionally "log only" because reward systems are server-specific.
        // External plugins can hook in via HytlDevApi.setVoteRewarder(...) to grant actual items/currency/etc.
        return true
    }

    private fun computeTier(streak: Int): Int {
        val thresholds = cfg.voteRewards.tierStreakThresholds
            .map { it.coerceAtLeast(1) }
            .sorted()
            .distinct()
            .ifEmpty { listOf(1) }

        var tier = 1
        for (t in thresholds) {
            if (streak >= t) tier++
        }
        // If thresholds=[1,3,7,14,30], tiers become 1..5. Clamp to multipliers length if provided.
        val maxTier = cfg.voteRewards.tierMultipliers.size.takeIf { it > 0 } ?: 1
        return (tier - 1).coerceIn(1, maxTier)
    }
}

