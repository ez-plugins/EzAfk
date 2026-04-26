# EzAfk

**Keep AFK management simple.**

EzAfk is a modern, lightweight AFK management plugin for Paper and Spigot servers.
It automates AFK detection, rewards or charges players based on idle state, gives staff
a real-time overview panel, and integrates with the tools you already run, all without
sacrificing performance.

> **v3.0.0** · Minecraft 26.1+ · Java 25 · Paper / Spigot / Bukkit / Purpur

---

## Feature Highlights

| Feature | Summary |
|---------|---------|
| **AFK Detection** | Idle timeout with configurable broadcasts, titles, animations, display-name changes, and a blindness-blur overlay |
| **Anti-Bypass** | Block infinite water flow, vehicle riding, and bubble column tricks from resetting idle timers |
| **AFK Kick** | Kick idle players after a configurable period, optionally only when the server is full |
| **Kick Warnings** | Multi-stage countdown messages (chat and/or title) at custom intervals before the kick fires |
| **Staff GUI** | `/afk gui` panel showing all AFK players with one-click kick, message, teleport, and custom command buttons |
| **AFK Zones** | Cuboid regions where players earn rewards (economy, commands, or items) for being AFK |
| **Economy Costs** | Charge a one-time entry fee and/or a recurring fee while AFK via any Vault economy plugin |
| **AFK Leaderboard** | Per-player cumulative AFK time, `/afk top` leaderboard, persisted across restarts |
| **PlaceholderAPI** | 16 placeholders for status, session time, totals, counts, prefix/suffix, and playtime |
| **Multi-language** | EN, ES, NL, RU, ZH, DE, fully overridable per server |
| **Storage backends** | YAML (default), SQLite, or MySQL |
| **Simple Voice Chat** | Play a custom MP3 sound when a player goes AFK |

---

## Installation

1. Download `EzAfk-x.x.x.jar` from the files tab above.
2. Drop it into your server's `plugins/` folder.
3. Restart the server. EzAfk generates all config files automatically.
4. Edit `plugins/EzAfk/config.yml` to configure your desired features.
5. Run `/afk reload` in-game to apply changes without a restart.

---

## Commands

| Command | Description | Permission |
|---------|-------------|------------|
| `/afk` | Toggle your own AFK status | (none) |
| `/afk reload` | Reload all configuration files | `ezafk.reload` |
| `/afk gui` | Open the AFK staff overview panel | `ezafk.gui` |
| `/afk toggle <player>` | Force a player's AFK state | `ezafk.toggle` |
| `/afk bypass <player>` | Toggle AFK bypass for a player | `ezafk.bypass.manage` |
| `/afk info <player>` | View a player's AFK session details | `ezafk.info` |
| `/afk time [player]` | View total AFK time | `ezafk.time` |
| `/afk time reset <player>` | Reset a player's AFK time | `ezafk.time.reset` |
| `/afk top` | Show the AFK time leaderboard | `ezafk.top` |
| `/afk zone pos1` / `pos2` | Select zone corners | `ezafk.zone.manage` |
| `/afk zone add <name>` | Create an AFK zone | `ezafk.zone.manage` |
| `/afk zone remove <name>` | Remove an AFK zone | `ezafk.zone.manage` |
| `/afk zone list` | List all zones | `ezafk.zone.list` |

Aliases: `/ezafk`, `/ea`, `/afktime`, `/afktop`

---

## Permissions

| Node | Default | Description |
|------|---------|-------------|
| `ezafk.reload` | OP | Reload configuration |
| `ezafk.bypass` | OP | Never be marked AFK automatically |
| `ezafk.bypass.manage` | OP | Toggle bypass for other players |
| `ezafk.toggle` | OP | Toggle other players' AFK state |
| `ezafk.info` | OP | View AFK details for other players |
| `ezafk.kick.bypass` | OP | Never be kicked by EzAfk |
| `ezafk.gui` | OP | Open the staff GUI |
| `ezafk.gui.view-active` | OP | View non-AFK players in the GUI |
| `ezafk.gui.actions` | OP | Use action buttons in the GUI |
| `ezafk.time` | **all** | View own AFK time |
| `ezafk.time.others` | OP | View another player's AFK time |
| `ezafk.time.reset` | OP | Reset a player's AFK time |
| `ezafk.top` | OP | View the leaderboard |
| `ezafk.economy.bypass` | OP | Skip economy AFK charges |
| `ezafk.zone.list` | OP | List AFK zones |
| `ezafk.zone.manage` | OP | Create and remove zones |

---

## PlaceholderAPI Placeholders

Requires [PlaceholderAPI](https://hangar.papermc.io/HelpChat/PlaceholderAPI). The expansion registers automatically. No extra setup needed.

| Placeholder | Returns |
|-------------|---------|
| `%ezafk_status%` | `AFK` or `ACTIVE` |
| `%ezafk_status_colored%` | `&cAFK` or `&aACTIVE` |
| `%ezafk_since%` | Seconds since AFK session started (empty if not AFK) |
| `%ezafk_last_active%` | Seconds since last player activity |
| `%ezafk_total_seconds%` | Total lifetime AFK seconds |
| `%ezafk_total%` | Total AFK time `HH:MM:SS` |
| `%ezafk_total_formatted%` | Verbose, e.g. `2 hours 15 minutes` |
| `%ezafk_prefix%` | Configured AFK display-name prefix |
| `%ezafk_suffix%` | Configured AFK display-name suffix |
| `%ezafk_playtime_active_seconds%` | Active (non-AFK) playtime in seconds |
| `%ezafk_playtime_active%` | Active playtime `HH:MM:SS` |
| `%ezafk_playtime_active_formatted%` | Active playtime verbose |
| `%ezafk_afk_count%` / `%ezafk_afk_players%` | Number of AFK players |
| `%ezafk_active_count%` / `%ezafk_active_players%` | Number of non-AFK players |

---

## Configuration Overview

EzAfk's settings are spread across focused files for clarity:

```text
config.yml    AFK detection, kick, anti-bypass, economy, integrations
gui.yml       Staff GUI layout and action buttons
zones.yml     AFK zone definitions with coordinates and rewards
mysql.yml     Database connection (only when storage.type: mysql)
messages/     Language files: en.yml, es.yml, nl.yml, ru.yml, zh.yml, de.yml
```

### Core settings (`config.yml`)

```yaml
afk:
  timeout: 300             # seconds of inactivity → AFK
  broadcast:
    enabled: true          # announce AFK in chat
  title:
    enabled: true          # show title to the AFK player
  anti:
    infinite-waterflow: false
    infinite-vehicle: false
    bubble-column: false

kick:
  enabled: false           # kick AFK players
  enabledWhenFull: false   # only kick when server is at capacity
  timeout: 600             # seconds AFK before kick
  warnings:
    enabled: true
    intervals: [60, 30, 10]
    mode: both             # chat | title | both

economy:
  enabled: false
  cost:
    enter:
      enabled: true
      amount: 25.0

storage:
  type: yaml               # yaml | sqlite | mysql
  flush-interval-seconds: 30
```

---

## Integrations

| Integration | Notes |
|-------------|-------|
| **Vault / Economy** | Required for economy costs and zone economy rewards |
| **PlaceholderAPI** | Auto-detected; provides 16 AFK placeholders |
| **WorldGuard** | Adds `afk-bypass` region flag; use `/rg flag <region> afk-bypass allow` |
| **TAB** | Delegate tab-list AFK prefix formatting to the TAB plugin |
| **Simple Voice Chat** | Play MP3 sounds on AFK state changes |
| **bStats** | Anonymous usage statistics (opt-out in `config.yml`) |
| **Spigot update checker** | Console reminder when a new version is available |

---

## Links

- [Documentation](https://ez-plugins.github.io/EzAfk/): full configuration guide, feature pages, and API reference
- [Discord](https://discord.gg/yWP95XfmBS): support, bug reports, and feature requests
- [GitHub](https://github.com/ez-plugins/EzAfk): source code and issue tracker
- [Developer API](https://ez-plugins.github.io/EzAfk/api/): `PlayerAfkStatusChangeEvent` and `AfkReason` enum
