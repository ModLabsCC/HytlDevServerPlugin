package cc.modlabs.api;

@FunctionalInterface
public interface VoteRewarder {
    /**
     * @return true if the reward was granted successfully and it is safe to ACK the vote.
     */
    boolean reward(PendingVote vote);
}

