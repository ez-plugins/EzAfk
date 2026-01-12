# EzAfk Commands Documentation

This document lists all commands provided by EzAfk, their arguments, permissions, and usage examples.

---

## /afk
- **Description:** Toggle your own AFK status.
- **Permission:** None (all players)
- **Usage:** `/afk`

## /afk reload
- **Description:** Reload all configuration files and integrations.
- **Permission:** `ezafk.reload`
- **Usage:** `/afk reload`

## /afk gui
- **Description:** Open the AFK player overview GUI.
- **Permission:** `ezafk.gui` (or OP)
- **Usage:** `/afk gui`

## /afk toggle <player>
- **Description:** Toggle AFK status for another player.
- **Permission:** `ezafk.toggle`
- **Usage:** `/afk toggle Notch`

## /afk bypass <player>
- **Description:** Toggle AFK bypass for a player (exempt from AFK detection).
- **Permission:** `ezafk.bypass.manage`
- **Usage:** `/afk bypass Steve`

## /afk info <player>
- **Description:** Show detailed AFK info for a player.
- **Permission:** `ezafk.info`
- **Usage:** `/afk info Alex`

## /afk time [player]
- **Description:** Show total AFK time for yourself or another player.
- **Permission:**
  - Self: `ezafk.time`
  - Others: `ezafk.time.others`
- **Usage:**
  - `/afk time` (self)
  - `/afk time Notch` (other)

## /afk top
- **Description:** Show the AFK leaderboard (top 10 by total AFK time).
- **Permission:** `ezafk.top`
- **Usage:** `/afk top`

---

### Notes
- All commands can be run as `/ezafk` as well as `/afk` (alias).
- Permissions can be managed via your permissions plugin (LuckPerms, PermissionsEx, etc).
- For more details on permissions, see `docs/permissions.md`.
