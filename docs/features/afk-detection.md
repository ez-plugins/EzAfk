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

- **`afk.timeout`**: How long (in seconds) a player must be idle before EzAfk marks them AFK.
  Default: `300` (5 minutes).
- **`afk.bypass.enabled`**: When `true`, players with the `ezafk.bypass` permission are never
  marked AFK automatically. Default: `true`.
- **`afk.broadcast.enabled`** / **`unafk.broadcast.enabled`**: Send a chat message to all online
  players when someone goes or returns from AFK. Messages are configured in your language file.
- **`afk.title.enabled`** / **`unafk.title.enabled`**: Show a large title overlay to the player
  when their AFK state changes. Text is configured in your language file.
- **`afk.hide-screen.enabled`**: Apply a blindness effect to players while they are AFK,
  preventing them from seeing the world. Default: `false`.
- **`afk.animation.enabled`** / **`unafk.animation.enabled`**: Toggle a bobbing animation on
  the AFK player's name tag as seen by nearby players. Default: `true`.
- **`afk.display-name.enabled`**: Add a prefix and/or suffix to the player's display name while
  AFK. Visible in chat and commands. Default: `false`.
- **`afk.display-name.prefix`** / **`suffix`**: Text to add before or after the player's name.
  Supports `&` colour codes.
- **`afk.display-name.format`**: Full name format. Available placeholders: `%prefix%`,
  `%player%`, `%suffix%`.
- **`afk.sound.enabled`** / **`unafk.sound.enabled`**: Play a sound when a player's AFK state
  changes.
- **`afk.sound.file`** / **`unafk.sound.file`**: Path to an `.mp3` file in the plugin's data
  folder.

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

1. Every time a player moves, interacts, chats, or performs another tracked action, EzAfk records
   when it happened.
2. A background task runs at a fixed interval and checks each player's idle time against
   `afk.timeout`.
3. Once the threshold is exceeded, the player is marked AFK and the configured feedback is triggered
   (broadcast, title, display-name change, animation, sound, blindness).
4. The moment any tracked activity is received from an AFK player, they are immediately marked active
   and the `unafk` feedback fires.
5. Players with `ezafk.bypass` are skipped entirely unless `afk.bypass.enabled` is `false`.

## Related

- [Anti-Bypass Protection](anti-bypass): prevent waterflow/vehicle tricks from resetting idle time
- [AFK Kick](afk-kick): kick players that stay AFK too long
- [Tab Prefix Integration](../integrations/TabIntegration): show AFK status in the tab list
- [PlaceholderAPI Integration](../integrations/PlaceholderApiIntegration): use AFK placeholders in other plugins
- [Permissions](../permissions): `ezafk.bypass`, `ezafk.afk`
