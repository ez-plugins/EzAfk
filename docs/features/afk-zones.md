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

- **`enabled`**: Master switch. Must be `true` for any zone to function. Default: `false`.

### Per-Region Fields

- **`name`**: Unique identifier for the zone. Used in commands and logs.
- **`world`**: World name where the zone exists.
- **`x1` / `y1` / `z1`** and **`x2` / `y2` / `z2`**: Opposite corners of the cuboid.
  The order of corners does not matter. EzAfk normalises min/max automatically.

### Reward Fields

- **`reward.enabled`**: Toggle rewards for this specific zone without removing its definition.
- **`reward.interval-seconds`**: How often (in seconds) the reward is granted to each AFK
  player inside the zone.
- **`reward.type`**: Reward delivery method.
  - `economy`: transfers `amount` currency via Vault. Requires a Vault-compatible economy plugin.
  - `command`: runs `command` as the console once per interval. Use `%player%` for the player name.
  - `item`: places the configured item directly into the player's inventory.
- **`reward.amount`**: Currency amount. Only used when `type: economy`.
- **`reward.max-stack`**: Maximum number of reward intervals that can accumulate before rewards
  stop. `0` means unlimited. Useful to prevent excessive overnight gains.
- **`reward.command`**: Console command template. Only used when `type: command`.
- **`reward.item.material`**: Item material name. Only used when `type: item`.
- **`reward.item.amount`**: Stack size of the item given per interval.

- **`reward.limit`**: Maximum number of rewards a player can receive per cooldown
  window (0 = unlimited).
- **`reward.limit-cooldown-seconds`**: Cooldown in seconds after the limit is
  reached before the counter resets (0 = no cooldown).

### Notification Fields

These fields configure a live countdown displayed to the player via
[EzCountdown](https://modrinth.com/plugin/ezcountdown) (if installed).
If EzCountdown is not present the notification is silently ignored.

- **`reward.notification.enabled`**: Toggle the countdown for this zone (`true`
  by default).
- **`reward.notification.displays`**: List of EzCountdown display types. Valid
  values: `ACTION_BAR`, `TITLE`, `BOSS_BAR`, `SCOREBOARD`, `DIALOG`.
- **`reward.notification.message`**: Message template. Supports EzCountdown live
  placeholders (`{seconds}`, `{formatted}`) and EzAfk placeholders (`%zone%`,
  `%amount%`).
- **`reward.notification.duration`**: How long the countdown runs (seconds).
  `0` means "match the zone's `interval-seconds`" so the timer resets exactly on
  each payout.

## Global Defaults

You can define fallback values for all zones in the top-level `defaults:` block
of `zones.yml`. Individual zones can override any default value.

```yaml
defaults:
  reward:
    enabled: false          # set to true to enable rewards for all zones by default
    interval-seconds: 300
    amount: 1.0
    type: economy
    notification:
      enabled: true
      displays: [ACTION_BAR]
      message: "&7Next reward in &e{seconds}s &7in &a%zone%"
      duration: 0
```

This is especially useful when Vault is present and you want all zones to grant
a small economy reward without configuring every zone individually.

## Session Reward Stats

EzAfk tracks how many rewards each player has received per zone during the
current server session (resets on restart). These are available as PlaceholderAPI
placeholders:

| Placeholder | Description |
|-------------|-------------|
| `%ezafk_zone_rewards_grants%` | Total reward grants across all zones |
| `%ezafk_zone_rewards_amount%` | Total currency earned across all zones |
| `%ezafk_zone_reward_<zone>_grants%` | Grants in a specific zone |
| `%ezafk_zone_reward_<zone>_amount%` | Currency earned in a specific zone |

Replace `<zone>` with the zone name, e.g. `%ezafk_zone_reward_spawn_grants%`.

## Entry / Exit Messages

When a player enters or leaves an AFK zone, EzAfk sends them a chat message. The message text
is configured in your language file under `messages/en.yml` (or the active locale):

```yaml
afkzone:
  enter: "&aYou have entered the AFK zone &e%zone%&a."
  exit:  "&7You have left the AFK zone &e%zone%&7."
```

Both messages support the `%zone%` placeholder, which is replaced with the zone's name.
Set either value to an empty string (`""`) to suppress that notification.

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
5. If EzCountdown is installed and `reward.notification.enabled` is `true`, a live countdown
   timer is displayed to the player showing the time until their next reward.

## Related

- [Economy Integration](../integrations/EconomyIntegration): required for `type: economy` rewards
- [EzCountdown Integration](../integrations/SimpleVoiceChatIntegration): live countdown notifications
- [WorldGuard Integration](../integrations/WorldGuardIntegration): use WorldEdit selections for zones
- [Commands](../commands): full `/afk zone` command reference
- [Permissions](../permissions): `ezafk.zone.manage`, `ezafk.zone.list`
