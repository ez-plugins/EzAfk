---
title: AFK Time & Leaderboard
nav_order: 8
parent: Features
---

# AFK Time & Leaderboard

EzAfk records the total amount of time each player has spent AFK since they first joined. This
cumulative counter persists across server restarts (stored in your configured storage backend) and is
exposed via in-game commands and PlaceholderAPI.

## Configuration

In your `config.yml`:

```yaml
storage:
  type: yaml                   # yaml | sqlite | mysql
  flush-interval-seconds: 30   # how often in-memory totals are written to disk/database
```

- **`storage.type`**: (string) Backend used to persist AFK time. All backends support the full
  leaderboard feature.
  - `yaml` — stores data in `plugins/EzAfk/data/` as per-player YAML files (no external dependencies).
  - `sqlite` — stores data in a single `ezafk.db` SQLite database file.
  - `mysql` — stores data in a remote MySQL database. See the [Storage / MySQL](../mysql) page for
    additional MySQL connection settings.
- **`storage.flush-interval-seconds`**: (integer) EzAfk accumulates AFK time in memory and writes it
  to the storage backend on this interval (in seconds) to reduce I/O. Data is also flushed on plugin
  shutdown and on player disconnect. Default: `30`.

## Commands

| Command | Description |
|---------|-------------|
| `/afk time` | View your own total AFK time |
| `/afk time <player>` | View another player's total AFK time |
| `/afk top` | Show the server-wide AFK time leaderboard |
| `/afk time reset <player>` | Reset a player's AFK time counter to zero |

## PlaceholderAPI Placeholders

If [PlaceholderAPI](../integrations/PlaceholderApiIntegration) is installed:

| Placeholder | Returns |
|-------------|---------|
| `%ezafk_total_seconds%` | Total lifetime AFK seconds (raw integer) |
| `%ezafk_total%` | Total AFK time in `HH:MM:SS` format |
| `%ezafk_total_formatted%` | Verbose format, e.g. `2 hours 15 minutes` |
| `%ezafk_since%` | Seconds since the current AFK session started (empty if not AFK) |

## How It Works

1. When a player goes AFK a session start timestamp is recorded.
2. When the player returns from AFK (or disconnects while AFK) the session duration is added to their
   cumulative total in memory.
3. Every `flush-interval-seconds` the in-memory map is written to the storage backend.
4. `/afk top` reads the stored totals, sorts them, and displays the top entries.
5. AFK time resets (`/afk time reset`) immediately update both the in-memory map and the storage
   backend.

## Notes

- AFK time is tracked per UUID, so name changes do not lose data.
- The leaderboard is sorted by total AFK time descending.
- Lowering `flush-interval-seconds` increases I/O but reduces data loss on unexpected crashes.

## Related

- [Storage / MySQL](../mysql) — MySQL connection settings and schema
- [PlaceholderAPI Integration](../integrations/PlaceholderApiIntegration) — all available placeholders
- [Commands](../commands) — full command reference
- [Permissions](../permissions) — `ezafk.time`, `ezafk.time.others`, `ezafk.top`, `ezafk.time.reset`
