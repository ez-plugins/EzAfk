# EzAfk Permissions Documentation

This document lists all permissions used by EzAfk, their effects, and recommended assignment.

---

## Permission List

| Permission                  | Description                                              | Default      |
|-----------------------------|----------------------------------------------------------|--------------|
| ezafk.reload                | Allows reloading the plugin configuration                | OP           |
| ezafk.gui                   | Allows opening the AFK player overview GUI               | OP           |
| ezafk.toggle                | Allows toggling AFK status for other players             | OP           |
| ezafk.bypass                | Exempts player from AFK detection                        | OP           |
| ezafk.bypass.manage         | Allows toggling AFK bypass for other players             | OP           |
| ezafk.info                  | Allows viewing detailed AFK info for other players       | OP           |
| ezafk.time                  | Allows viewing your own total AFK time                   | true         |
| ezafk.time.others           | Allows viewing total AFK time for other players          | OP           |
| ezafk.top                   | Allows viewing the AFK leaderboard                       | OP           |
| ezafk.economy.bypass        | Exempts player from economy-based AFK costs              | OP           |

---

## Permission Details

- **ezafk.reload**: Required to use `/afk reload`.
- **ezafk.gui**: Required to use `/afk gui`.
- **ezafk.toggle**: Required to use `/afk toggle <player>`.
- **ezafk.bypass**: Players with this permission are never marked as AFK automatically.
- **ezafk.bypass.manage**: Required to use `/afk bypass <player>`.
- **ezafk.info**: Required to use `/afk info <player>`.
- **ezafk.time**: Allows `/afk time` for self.
- **ezafk.time.others**: Allows `/afk time <player>` for others.
- **ezafk.top**: Allows `/afk top`.
- **ezafk.economy.bypass**: Exempts from all AFK-related economy costs.

---

## Managing Permissions
- Use a permissions plugin (LuckPerms, PermissionsEx, etc.) to assign these permissions.
- By default, most permissions are granted to OPs only, except `ezafk.time` (all players).
- For a public server, assign only necessary permissions to trusted staff.

---
For more information, see the README.md and commands documentation.
