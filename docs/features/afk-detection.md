---
title: AFK Detection
nav_order: 1
parent: Features
---

# AFK Detection

The core feature of EzAfk. A player is marked AFK when they have not performed any tracked activity
(movement, block interaction, chat, etc.) for a configurable number of seconds. When they return,
the plugin marks them active and optionally announces the change.

## Configuration

In your `config.yml`:

```yaml
afk:
  timeout: 300          # seconds of inactivity before a player is marked AFK
  bypass:
    enabled: true       # allow ezafk.bypass permission to skip AFK detection
  broadcast:
    enabled: true       # announce AFK state in chat
  title:
    enabled: true       # send a title screen when going AFK
  hide-screen:
    enabled: false      # apply a blindness-blur overlay while AFK
  animation:
    enabled: true       # bob the player's name-tag while AFK (per-viewer)
  display-name:
    enabled: false      # prepend/append a prefix/suffix to the in-game display name
    prefix: "&7[AFK] "
    suffix: ""
    format: "%prefix%%player%%suffix%"
  sound:
    enabled: true
    file: "mp3/ezafk-sound.mp3"   # relative to plugin folder

unafk:
  broadcast:
    enabled: true       # announce return from AFK in chat
  title:
    enabled: true       # send a title screen when returning
  animation:
    enabled: true       # stop name-tag animation on return
  sound:
    enabled: true
    file: "mp3/ezafk-sound.mp3"
```

- **`afk.timeout`**: (integer, seconds) How long a player must be idle before EzAfk marks them AFK.
  Default: `300` (5 minutes).
- **`afk.bypass.enabled`**: (bool) When `true`, players with the `ezafk.bypass` permission are never
  marked AFK automatically. Default: `true`.
- **`afk.broadcast.enabled`** / **`unafk.broadcast.enabled`**: (bool) Send a chat message to all
  online players when someone goes or returns from AFK. Messages are configured in your language file.
- **`afk.title.enabled`** / **`unafk.title.enabled`**: (bool) Show a large title overlay to the
  player themselves when their AFK state changes. Text is configured in your language file.
- **`afk.hide-screen.enabled`**: (bool) Apply a blindness blur to the player while they are AFK,
  preventing them from seeing the world. Default: `false`.
- **`afk.animation.enabled`** / **`unafk.animation.enabled`**: (bool) Toggle a bobbing animation on
  the AFK player's name-tag as seen by other players. Default: `true`.
- **`afk.display-name.enabled`**: (bool) Modify the player's display name using `prefix`, `suffix`,
  and `format`. Visible in chat and commands that echo display names. Default: `false`.
- **`afk.display-name.prefix`** / **`suffix`**: (string, supports `&` colour codes) Text prepended or
  appended to the player's name.
- **`afk.display-name.format`**: (string) Full format string. Available placeholders: `%prefix%`,
  `%player%`, `%suffix%`.
- **`afk.sound.enabled`** / **`unafk.sound.enabled`**: (bool) Play a sound to the player when their
  AFK state changes.
- **`afk.sound.file`** / **`unafk.sound.file`**: (string) Path to an `.mp3` file inside the plugin's
  data folder, relative to the plugin root.

## Customising Messages

Edit your active language file in `messages/` (e.g. `messages/en.yml`):

```yaml
afk:
  broadcast: "&e{player} &7is now AFK."
  title: "&eYou are AFK"
  subtitle: "&7You have been marked as AFK."

unafk:
  broadcast: "&e{player} &7is no longer AFK."
  title: "&aWelcome back!"
  subtitle: "&7You are no longer AFK."
```

See the [Messages](../messages) page for the full reference.

## How It Works

1. On every player action (move, interact, chat, etc.) a timestamp is updated in EzAfk's session map.
2. The idle-check task runs on a fixed interval and compares `now − lastActivity` against `afk.timeout`.
3. Once the threshold is exceeded, the player is marked AFK and the configured feedback is triggered
   (broadcast, title, display-name change, animation, sound, blindness).
4. The moment any tracked activity is received from an AFK player, they are immediately marked active
   and the `unafk` feedback fires.
5. Players with `ezafk.bypass` are skipped entirely unless `afk.bypass.enabled` is `false`.

## Related

- [Anti-Bypass Protection](anti-bypass) — prevent waterflow/vehicle tricks from resetting idle time
- [AFK Kick](afk-kick) — kick players that stay AFK too long
- [Tab Prefix Integration](../integrations/TabIntegration) — show AFK status in the tab list
- [PlaceholderAPI Integration](../integrations/PlaceholderApiIntegration) — use AFK placeholders in other plugins
- [Permissions](../permissions) — `ezafk.bypass`, `ezafk.afk`
