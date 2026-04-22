---
title: Anti-Bypass Protection
nav_order: 2
parent: Features
---

# Anti-Bypass Protection

Some automated farms or clients exploit game mechanics — such as flowing water, rideable entities, or
bubble columns — to produce continuous movement events that prevent AFK detection. EzAfk's anti-bypass
system intercepts each of these exploit vectors and suppresses the resulting activity signal so idle
players are flagged correctly.

## Configuration

In your `config.yml`:

```yaml
afk:
  anti:
    infinite-waterflow: false   # ignore movement caused by flowing water
    infinite-vehicle: false     # ignore movement while riding a vehicle/entity
    bubble-column: false        # ignore upward push from bubble columns
    flag-only: false            # if true, only mark AFK silently; do not warn/eject
```

- **`afk.anti.infinite-waterflow`**: (bool) When `true`, movement events caused by a flowing water
  current are not counted as player activity. Useful for servers with water-based AFK fish farms.
  Default: `false`.
- **`afk.anti.infinite-vehicle`**: (bool) When `true`, movement events while the player is riding a
  mob or minecart are ignored. Prevents AFK grinders that rely on riding entity movement to stay
  "active". Default: `false`.
- **`afk.anti.bubble-column`**: (bool) When `true`, the upward velocity force from a soul-sand bubble
  column is not counted as player activity. Default: `false`.
- **`afk.anti.flag-only`**: (bool) When `true`, exploiting players are silently marked AFK without any
  warning message or ejection. When `false` (default), EzAfk may warn the player and/or interrupt the
  exploit. Default: `false`.

## How It Works

1. EzAfk listens for the relevant Bukkit events (`PlayerMoveEvent`, `VehicleMoveEvent`, etc.).
2. Before crediting activity to the player, it checks the cause of the movement against the enabled
   anti-bypass rules.
3. Movement that matches an enabled rule is discarded — the player's last-activity timestamp is **not**
   updated.
4. After the normal `afk.timeout` elapses without legitimate activity, the player is marked AFK as
   usual.
5. If `flag-only` is `false`, EzAfk may send a warning to the player or interrupt the exploit source
   (e.g. eject from a vehicle). If `flag-only` is `true`, the transition happens silently.

## Notes

- Anti-bypass rules are independent — enable only the ones relevant to your server's gameplay.
- WorldGuard region flags can restrict AFK behaviour on a per-region basis. See
  [WorldGuard Integration](../integrations/WorldGuardIntegration).
- Players with the `ezafk.bypass` permission are not subject to anti-bypass checks when
  `afk.bypass.enabled` is `true`.

## Related

- [AFK Detection](afk-detection) — the core idle detection system
- [WorldGuard Integration](../integrations/WorldGuardIntegration) — region-based AFK flags
- [Permissions](../permissions) — `ezafk.bypass`
