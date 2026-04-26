---
title: Permissions
nav_order: 6
---

# EzAfk Permissions

This page lists all permission nodes, their defaults, and which feature each one belongs to.

---

## Quick Reference

| Permission | Description | Default |
|------------|-------------|---------|
| `ezafk.reload` | Reload plugin configuration | OP |
| `ezafk.bypass` | Never be marked AFK automatically | OP |
| `ezafk.bypass.manage` | Toggle AFK bypass for other players | OP |
| `ezafk.toggle` | Toggle AFK status for other players | OP |
| `ezafk.info` | View detailed AFK info for other players | OP |
| `ezafk.kick.bypass` | Never be kicked by EzAfk's AFK kick | OP |
| `ezafk.gui` | Open the AFK overview GUI | OP |
| `ezafk.gui.view-active` | View active players in the GUI | OP |
| `ezafk.gui.actions` | Use player actions in the GUI | OP |
| `ezafk.time` | View your own total AFK time | `true` |
| `ezafk.time.others` | View another player's AFK time | OP |
| `ezafk.time.reset` | Reset a player's total AFK time | OP |
| `ezafk.top` | View the AFK leaderboard | OP |
| `ezafk.economy.bypass` | Bypass economy AFK costs | OP |
| `ezafk.zone.list` | List AFK zones and view zone players | OP |
| `ezafk.zone.manage` | Create and remove AFK zones | OP |

---

## By Feature

### [AFK Detection](features/afk-detection) & General

- **`ezafk.bypass`**: Players with this node are never automatically marked AFK (requires
  `afk.bypass.enabled: true` in `config.yml`).
- **`ezafk.bypass.manage`**: Required for `/afk bypass <player>`.
- **`ezafk.toggle`**: Required for `/afk toggle <player>` to force-toggle another player's AFK state.
- **`ezafk.info`**: Required for `/afk info <player>` to view another player's session details.
- **`ezafk.reload`**: Required for `/afk reload`.

### [AFK Kick](features/afk-kick) & [Kick Warnings](features/afk-kick-warnings)

- **`ezafk.kick.bypass`**: Players with this node are never kicked by the AFK kick system,
  even if kick warnings have fired.

### [In-Game GUI](features/gui)

- **`ezafk.gui`**: Required to open `/afk gui`.
- **`ezafk.gui.view-active`**: Allows viewing active (non-AFK) players in the GUI, not just AFK ones.
- **`ezafk.gui.actions`**: Allows clicking action buttons (kick, message, teleport, command) in the GUI.

### [AFK Time & Leaderboard](features/leaderboard)

- **`ezafk.time`**: Allows `/afk time` to view your own total AFK time. Default: all players.
- **`ezafk.time.others`**: Allows `/afk time <player>` to view another player's AFK time.
- **`ezafk.time.reset`**: Allows `/afk time reset <player>`.
- **`ezafk.top`**: Allows `/afk top` to view the full leaderboard.

### [Economy Costs](features/economy-costs)

- **`ezafk.economy.bypass`**: Exempts the player from all economy enter and recurring costs.
  The permission node is configurable via `economy.bypass-permission` in `config.yml`.

### [AFK Zones](features/afk-zones)

- **`ezafk.zone.list`**: Required for `/afk zone list` and `/afk zone players`.
- **`ezafk.zone.manage`**: Required for `/afk zone add`, `remove`, `pos1`, `pos2`, `clearpos`, `reset`.

---

## Notes

- Most permissions default to **OP only**. Use a permissions plugin such as LuckPerms to grant
  specific nodes to staff groups or all players.
- `ezafk.time` is granted to all players by default so every player can check their own AFK stats.
- For a public server, grant `ezafk.gui` and `ezafk.gui.actions` only to trusted staff.
