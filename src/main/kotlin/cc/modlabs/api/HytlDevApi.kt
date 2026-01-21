package cc.modlabs.api

import java.util.concurrent.atomic.AtomicReference

/**
 * Public API surface for other plugins to hook into HYTL integration.
 */
object HytlDevApi {
    private val rewarderRef = AtomicReference<VoteRewarder?>(null)

    @JvmStatic
    fun setVoteRewarder(rewarder: VoteRewarder?) {
        rewarderRef.set(rewarder)
    }

    @JvmStatic
    fun getVoteRewarder(): VoteRewarder? = rewarderRef.get()
}

