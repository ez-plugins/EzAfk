---
title: Anti-Bypass Protection
nav_order: 2
parent: Features
---

# Anti-Bypass Protection

Some automated farms or clients exploit game mechanics such as flowing water, rideable entities, or
bubble columns to trigger continuous movement that prevents AFK detection. EzAfk's anti-bypass
system detects each of these tricks and ignores the movement so idle players are flagged correctly.

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

- **`afk.anti.infinite-waterflow`**: When `true`, movement caused by a flowing water current is
  not counted as player activity. Useful for servers with water-based AFK fish farms.
  Default: `false`.
- **`afk.anti.infinite-vehicle`**: When `true`, movement while the player is riding a mob or
  minecart is ignored. Prevents AFK grinders that rely on riding entity movement to stay
  "active". Default: `false`.
- **`afk.anti.bubble-column`**: When `true`, the upward force from a soul-sand bubble column is
  not counted as player activity. Default: `false`.
- **`afk.anti.flag-only`**: When `true`, players using bypass tricks are silently marked AFK
  without any warning or ejection. When `false` (default), EzAfk may warn the player and/or
  interrupt the exploit. Default: `false`.

## How It Works

1. EzAfk monitors player movement events and checks the cause of each movement.
2. Before crediting activity to the player, it checks whether the movement matches an enabled
   anti-bypass rule.
3. Movement that matches an enabled rule is discarded; the player's last-activity timestamp is **not**
   updated.
4. After the normal `afk.timeout` elapses without legitimate activity, the player is marked AFK as
   usual.
5. If `flag-only` is `false`, EzAfk may send a warning to the player or interrupt the bypass source
   (e.g. eject from a vehicle). If `flag-only` is `true`, the transition happens silently.

## Notes

- Anti-bypass rules are independent. Enable only the ones relevant to your server's gameplay.
- WorldGuard region flags can restrict AFK behaviour on a per-region basis. See
  [WorldGuard Integration](../integrations/WorldGuardIntegration).
- Players with the `ezafk.bypass` permission are not subject to anti-bypass checks when
  `afk.bypass.enabled` is `true`.

## Related

- [AFK Detection](afk-detection): the core idle detection system
- [WorldGuard Integration](../integrations/WorldGuardIntegration): region-based AFK flags
- [Permissions](../permissions): `ezafk.bypass`
