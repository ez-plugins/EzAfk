---
title: AFK Zones
nav_order: 6
parent: Features
---

# AFK Zones

AFK Zones let you define cuboid regions where players earn rewards for being AFK. Each zone has its
own coordinates, world, and reward configuration. Rewards can be economy money (via Vault), console
commands, or items dropped into the player's inventory.

Zones are stored in `zones.yml` and managed in-game with `/afk zone`.

## Configuration

`zones.yml` (separate file from `config.yml`):

```yaml
enabled: false   # master switch for the AFK Zones system

regions:
  - name: spawn
    world: world
    x1: 100
    y1: 50
    z1: 100
    x2: 120
    y2: 70
    z2: 120
    reward:
      enabled: true
      interval-seconds: 60   # grant reward every N seconds the player is AFK in this zone
      type: economy           # economy | command | item
      amount: 5.0             # for economy type: amount of currency
      max-stack: 3            # maximum times the reward can accumulate (0 = unlimited)

  # Command reward example
  - name: arena
    world: world_nether
    x1: -50
    y1: 60
    z1: -50
    x2: 50
    y2: 120
    z2: 50
    reward:
      enabled: true
      interval-seconds: 120
      type: command
      command: "give %player% diamond 1"   # run as console; %player% = player name
      max-stack: 0

  # Item reward example
  - name: market
    world: world
    x1: 200
    y1: 64
    z1: 200
    x2: 220
    y2: 80
    z2: 220
    reward:
      enabled: true
      interval-seconds: 300
      type: item
      item:
        material: EMERALD
        amount: 3
      max-stack: 5
```

### Global

- **`enabled`**: (bool) Master switch. Must be `true` for any zone to function. Default: `false`.

### Per-Region Fields

- **`name`**: (string) Unique identifier for the zone. Used in commands and logs.
- **`world`**: (string) Bukkit world name where the zone exists.
- **`x1` / `y1` / `z1`** and **`x2` / `y2` / `z2`**: (integer) Opposite corners of the cuboid.
  The order of corners does not matter — EzAfk normalises min/max automatically.

### Reward Fields

- **`reward.enabled`**: (bool) Toggle rewards for this specific zone without removing its definition.
- **`reward.interval-seconds`**: (integer) How often (in seconds) the reward is granted to each AFK
  player inside the zone.
- **`reward.type`**: (string) Reward delivery method.
  - `economy` — transfers `amount` currency via Vault. Requires a Vault-compatible economy plugin.
  - `command` — runs `command` as the console once per interval. Use `%player%` for the player name.
  - `item` — places the configured item directly into the player's inventory.
- **`reward.amount`**: (decimal) Currency amount. Only used when `type: economy`.
- **`reward.max-stack`**: (integer) Maximum number of reward intervals that can accumulate before the
  reward stops. `0` means unlimited. Useful to prevent excessive overnight gains.
- **`reward.command`**: (string) Console command template. Only used when `type: command`.
- **`reward.item.material`**: (string) Bukkit material name. Only used when `type: item`.
- **`reward.item.amount`**: (integer) Stack size of the item given per interval.

## In-Game Zone Management

Zones can be created and managed with `/afk zone` without editing `zones.yml` directly:

| Subcommand | Description |
|------------|-------------|
| `/afk zone pos1` | Set the first corner to your current location |
| `/afk zone pos2` | Set the second corner to your current location |
| `/afk zone add <name>` | Create a zone between the two selected positions |
| `/afk zone remove <name>` | Delete a zone by name |
| `/afk zone list` | List all defined zones |

If you have WorldEdit installed you can also use your WorldEdit wand selection as the zone corners.
See [WorldGuard Integration](../integrations/WorldGuardIntegration) for details.

## How It Works

1. Every `interval-seconds`, EzAfk scans all AFK players and checks whether their location falls
   inside any enabled zone.
2. For each matching zone, EzAfk checks that the player's accumulated reward count is below `max-stack`
   (or that `max-stack` is 0).
3. The reward is delivered (economy transfer, console command, or item give).
4. The stack counter increments. It resets when the player leaves the zone or returns from AFK.

## Related

- [Economy Integration](../integrations/EconomyIntegration) — required for `type: economy` rewards
- [WorldGuard Integration](../integrations/WorldGuardIntegration) — use WorldEdit selections for zones
- [Commands](../commands) — full `/afk zone` command reference
- [Permissions](../permissions) — `ezafk.zone.manage`, `ezafk.zone.list`
