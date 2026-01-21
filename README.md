## HYTL Dev Server Plugin

Hytale server plugin that integrates your server with **HYTL** by:
- Sending a **heartbeat** (server status) to HYTL every N seconds
- Polling **pending vote rewards**, letting your server reward players, and ACKing votes

### License

This project is licensed under **GPL-3.0**. See `LICENSE`.

---

## Server Owner Guide

### Install

- Download the latest `*-shaded.jar` from the GitHub Releases.
- Put it into your server's `mods/` folder (or your plugin/mod folder as used by your server setup).
- Start the server once so the config file is created.

### Configure (recommended)

Run from **server console** (recommended so the secret never appears in chat):

```text
/hytl setup <serverId> <serverSecret>
```

If your dashboard provides a full backend URL (like ngrok), use the 3-arg form:

```text
/hytl setup <serverId> <serverSecret> <backendBaseUrl>
```

Example:

```text
/hytl setup plugin-dev-1 c661ebfc7470539eb683a11ab4f635641d4087291cb8ec8c https://hytl.dev/api/plugin
```

### Reload without restarting

```text
/hytl reload
```

### Check status

```text
/hytl status
```

### Config file location

The plugin stores config in the plugin data directory, typically like:

```text
mods/<Group>_<PluginName>/config.json
```

### Config file example

```json
{
  "serverId": "my-server-1",
  "serverSecret": "…",
  "backendBaseUrl": "https://hytl.dev/api/plugin",
  "heartbeatSeconds": 10,
  "votePollSeconds": 15,
  "privacy": {
    "shareMods": true,
    "sharePlayerCount": true,
    "sharePlayerNames": true,
    "sharePlayerUUIDs": false
  },
  "voteRewards": {
    "mode": "LOG_ONLY",
    "baseAmount": 1,
    "tierStreakThresholds": [1, 3, 7, 14, 30],
    "tierMultipliers": [1, 2, 3, 4, 5]
  },
  "httpTimeoutMs": 3000,
  "maxBackoffSeconds": 60,
  "publicHost": null,
  "publicPort": null,
  "gameVersion": null,
  "patchline": null
}
```

### Privacy behavior (plugin-side)

- **sharePlayerCount=false**: sends `currentPlayers = -1`
- **sharePlayerNames=false**: omits `players` entirely
- **sharePlayerUUIDs=false**: includes player names but omits UUIDs
- **shareMods=false**: sends `"mods": { "enabled": false }` (no list)

### Vote rewards default behavior

By default, the plugin uses `voteRewards.mode = LOG_ONLY`:
- It logs the computed reward tier/amount
- It returns success so the vote is ACKed

If you want to disable rewards entirely (and keep votes pending on HYTL):
- Set `voteRewards.mode = DISABLED`

---

## Developer Guide (Vote API Hook)

You can hook into the vote reward flow by providing a `VoteRewarder` implementation.
If present, the HYTL plugin calls your rewarder for each pending vote. If your rewarder returns `true`, the plugin ACKs the vote to HYTL.

### Public API (Java-friendly)

- `cc.modlabs.api.VoteRewarder` (Java `@FunctionalInterface`)
- `cc.modlabs.api.PendingVote` (Kotlin `@JvmRecord` → appears as a Java `record`)
- `cc.modlabs.api.HytlDevApi` (static registry)

### Registering a rewarder (Java)

```java
import cc.modlabs.api.HytlDevApi;
import cc.modlabs.api.PendingVote;

public final class MyRewards {
  public static void init() {
    HytlDevApi.setVoteRewarder((PendingVote vote) -> {
      // Prefer UUID if present, else name:
      String playerUuid = vote.playerUuid();
      String playerName = vote.playerName();

      int streak = vote.streak();
      // TODO: grant items/currency/permissions/etc.

      return true; // true => HYTL plugin will ACK the vote
    });
  }
}
```

### Notes

- Your rewarder should be **idempotent** on `voteId` if possible. The HYTL plugin also persists processed voteIds to disk.
- If your rewarder returns `false` (or throws), the plugin will **not ACK**, and the vote should be retried later.

