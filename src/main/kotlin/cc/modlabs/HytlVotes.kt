package cc.modlabs

import cc.modlabs.api.PendingVote
import cc.modlabs.api.VoteRewarder
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.isSuccess
import kotlinx.coroutines.runBlocking

class VoteService(
    private val http: HttpClient,
    private val config: HytlPluginConfig,
    private val processed: ProcessedVoteStore,
    private val rewarder: VoteRewarder
) {
    fun pollAndProcessOnce(onError: (Throwable) -> Unit): Boolean {
        return try {
            runBlocking {
                val pending: PendingVotesResponse = http.get("${config.backendBaseUrl}/votes/pending") {
                    parameter("serverId", config.serverId)
                    parameter("limit", 25)
                }.body()

                for (event in pending.events) {
                    processEvent(event)
                }
            }
            true
        } catch (t: Throwable) {
            onError(t)
            false
        }
    }

    private suspend fun processEvent(event: VoteEvent) {
        val already = processed.has(event.voteId)
        if (!already) {
            val vote = PendingVote(
                voteId = event.voteId,
                createdAt = event.createdAt,
                playerName = event.playerName,
                playerUuid = event.playerUuid,
                streak = event.streak ?: 1
            )
            val rewarded = rewarder.reward(vote)
            if (!rewarded) return
            processed.markProcessed(event.voteId)
        }

        // Always try to ACK (even if already processed), to drain remote pending queue.
        val ackOk = ack(event.voteId)
        if (!ackOk) {
            // Leave it pending remotely; we'll retry on next poll.
        }
    }

    private suspend fun ack(voteId: String): Boolean {
        val resp: HttpResponse = http.post("${config.backendBaseUrl}/votes/ack") {
            setBody(VoteAckRequest(serverId = config.serverId, voteId = voteId))
        }
        if (!resp.status.isSuccess()) return false
        val body: VoteAckResponse = resp.body()
        return body.ok && body.acked
    }
}

