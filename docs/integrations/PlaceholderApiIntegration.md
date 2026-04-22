---
title: PlaceholderAPI
nav_order: 3
parent: Integrations
---

# PlaceholderAPI Integration

EzAfk registers a custom PlaceholderAPI expansion when the PlaceholderAPI plugin is detected.
This makes all AFK data available to any plugin that supports PlaceholderAPI — scoreboards,
chat formatters, holograms, GUIs, and more.

## Setup

1. Install [PlaceholderAPI](https://www.spigotmc.org/resources/placeholderapi.6245/) on your server.
2. EzAfk detects it automatically — no extra configuration is required.
3. Use the placeholders below anywhere PlaceholderAPI syntax is accepted.

## Available Placeholders

| Placeholder | Returns |
|-------------|---------|
| `%ezafk_status%` | `AFK` if the player is AFK, otherwise `ACTIVE` |
| `%ezafk_status_colored%` | `&cAFK` or `&aACTIVE` (colour-coded) |
| `%ezafk_since%` | Seconds elapsed since the current AFK session started (empty if not AFK) |
| `%ezafk_last_active%` | Seconds since the player last performed a tracked activity |
| `%ezafk_total_seconds%` | Total lifetime AFK time in seconds (raw integer) |
| `%ezafk_total%` | Total AFK time in `HH:MM:SS` format |
| `%ezafk_total_formatted%` | Total AFK time in verbose format, e.g. `2 hours 15 minutes` |
| `%ezafk_prefix%` | The configured AFK display name prefix (empty when not AFK) |
| `%ezafk_suffix%` | The configured AFK display name suffix (empty when not AFK) |
| `%ezafk_playtime_active_seconds%` | Active (non-AFK) playtime in seconds — requires Playtime integration |
| `%ezafk_playtime_active%` | Active playtime in `HH:MM:SS` format |
| `%ezafk_playtime_active_formatted%` | Active playtime in verbose format |
| `%ezafk_afk_count%` | Number of currently AFK players on the server |
| `%ezafk_afk_players%` | Same as `%ezafk_afk_count%` (alias) |
| `%ezafk_active_count%` | Number of non-AFK online players |
| `%ezafk_active_players%` | Same as `%ezafk_active_count%` (alias) |

Run `/papi list ezafk` in-game to confirm the expansion is loaded.

## Notes

- Placeholders update in real time — there is no caching delay.
- `%ezafk_since%` returns an empty string when the player is not AFK, making it safe to use in
  conditional display contexts.
- `%ezafk_playtime_active_*` placeholders require the Playtime integration to be configured in
  `config.yml` (`integration.playtime.enabled: true`).

## Related

- [AFK Detection](../features/afk-detection) — AFK state and display-name prefix/suffix settings
- [AFK Time & Leaderboard](../features/leaderboard) — total AFK time tracking
