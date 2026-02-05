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

## /afk time reset <player>
- **Description:** Reset a player's total AFK time (admin).
- **Permission:** `ezafk.time.reset`
- **Usage:** `/afk time reset Notch`

## /afk zone <subcommand>
- **Description:** Manage AFK zones (regions where AFK behaviour can differ).
- **Permission:** Varies by subcommand (see below)
- **Usage:** `/afk zone <list|players|add|remove|pos1|pos2|clearpos|reset> [args]`

Zone subcommands:
- **`list`**: Show configured AFK zones. Permission: `ezafk.zone.list`. Usage: `/afk zone list`
- **`players`**: Show players currently in AFK zones. Permission: `ezafk.zone.list`. Usage: `/afk zone players`
- **`add <name>`**: Add a new AFK zone. Uses WorldEdit selection if available, else uses stored `pos1`/`pos2` or explicit coordinates. Permission: `ezafk.zone.manage`. Usage: `/afk zone add myzone` or `/afk zone add myzone x1 y1 z1 x2 y2 z2`
- **`remove <name>`**: Remove a configured AFK zone. Permission: `ezafk.zone.manage`. Usage: `/afk zone remove myzone`
- **`pos1`**: Set position 1 for zone creation (stores selection per-player). Permission: `ezafk.zone.manage`. Usage: `/afk zone pos1`
- **`pos2`**: Set position 2 for zone creation. Permission: `ezafk.zone.manage`. Usage: `/afk zone pos2`
- **`clearpos [player]`**: Clear stored pos1/pos2 for yourself or target player. Permission: `ezafk.zone.manage` (required when clearing another player). Usage: `/afk zone clearpos` or `/afk zone clearpos Notch`
- **`reset <player> [zone]`**: Reset zone reward counts for a player (optionally for a specific zone). Permission: `ezafk.zone.manage`. Usage: `/afk zone reset Notch` or `/afk zone reset Notch myzone`

---

### Notes
- All commands can be run as `/ezafk` as well as `/afk` (alias).
- Permissions can be managed via your permissions plugin (LuckPerms, PermissionsEx, etc).
- For more details on permissions, see `docs/permissions.md`.

- Additional aliases: plugin registers several command aliases so you can also run `/ea`, `/afktime`, `/afktop`, or `/afkzone` (see `plugin.yml`).
