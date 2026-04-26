---
title: AFK Kick
nav_order: 3
parent: Features
---

# AFK Kick

EzAfk can automatically kick players that have been AFK for too long. You can choose to kick all AFK
players after a fixed timer, or limit the kick to situations when the server is at capacity, freeing
up slots for new players.

Multi-stage warnings before the kick are configured separately; see
[AFK Kick Warnings](../afk-kick-warnings).

## Configuration

In your `config.yml`:

```yaml
kick:
  enabled: false         # enable AFK kicking
  enabledWhenFull: false # kick only when the server is at max player count
  timeout: 600           # seconds of AFK time before the kick is issued
```

- **`kick.enabled`**: Master switch. When `false`, no players are ever kicked by EzAfk,
  regardless of other settings. Default: `false`.
- **`kick.enabledWhenFull`**: When `true`, EzAfk only kicks AFK players when the server player
  count equals `max-players` in `server.properties`. Useful for keeping AFK players around on
  quieter servers while still freeing slots during peak times. Requires `kick.enabled: true`.
  Default: `false`.
- **`kick.timeout`**: How long (in seconds) a player must be continuously AFK before EzAfk kicks
  them. This timer starts from the moment the player was marked AFK (i.e. after the initial
  `afk.timeout` has already elapsed). Default: `600` (10 minutes).

## Customising the Kick Message

Edit your active language file in `messages/` (e.g. `messages/en.yml`):

```yaml
kick:
  message: "&cYou have been kicked for being AFK too long."
```

See the [Messages](../messages) page for the full reference.

## How It Works

1. When a player goes AFK, a kick countdown starts alongside the existing AFK state.
2. The countdown runs for `kick.timeout` seconds.
3. If the player remains AFK for the full duration, EzAfk kicks the player with the
   configured kick message.
4. If `enabledWhenFull` is `true`, EzAfk first checks whether `online players ≥ max players` before
   issuing the kick. If the server is not full, the kick is skipped even if the timer expired.
5. Returning from AFK at any point resets the kick countdown.

## Notes

- The kick timer is separate from (and always longer than) the `afk.timeout` idle timer.
- To warn players before the kick fires, enable the [AFK Kick Warnings](../afk-kick-warnings) system.
- Players with the `ezafk.kick.bypass` permission are not kicked.

## Related

- [AFK Kick Warnings](../afk-kick-warnings): send countdown messages before kicking
- [AFK Detection](afk-detection): the idle detection system that starts the kick timer
- [Permissions](../permissions): `ezafk.kick.bypass`
