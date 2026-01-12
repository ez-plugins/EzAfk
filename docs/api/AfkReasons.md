# AfkReasons

EzAfk uses the `AfkReason` enum to describe why a player's AFK status changed. Below are all possible reasons:

## Possible AfkReasons
- `MANUAL` — Player toggled AFK manually.
- `COMMAND_FORCED` — Marked AFK by a staff command.
- `INACTIVITY` — No recent player activity was detected.
- `ANTI_INFINITE_WATER` — Bypass detection: sustained water flow movement.
- `ANTI_VEHICLE` — Bypass detection: vehicle movement without input.
- `ANTI_BUBBLE_COLUMN` — Bypass detection: bubble column movement.
- `OTHER` — AFK status updated by the plugin.

Refer to the [PlayerAfkStatusChangeEvent](./events.md#playerafkstatuschangeevent) for how these reasons are used in events.