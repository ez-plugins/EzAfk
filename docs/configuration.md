
# EzAfk Configuration Guide (Advanced)

This document provides advanced documentation for every configuration option in EzAfk's main configuration files.

---

## config.yml

### messages
- `language`: Default language for plugin messages. Supported: `en`, `es`, `nl`, `ru`, `zh`. Determines which file in `messages/` is loaded.

### afk
- `timeout`: (int, seconds) Time of inactivity before a player is marked AFK.
- `bypass.enabled`: (bool) If true, enables the `ezafk.bypass` permission (OPs by default).
- `broadcast.enabled`: (bool) Broadcasts a message to all players when someone goes AFK.
- `broadcast`:
	- `enabled`: (bool) Enable/disable AFK broadcast messages.
	- Placeholders: `%player%`, `%afk_count%`, `%active_count%`.
- `title.enabled`: (bool) Show a title message when a player goes AFK.
- `hide-screen.enabled`: (bool) Apply blindness effect to AFK players to hide their screen.
- `animation.enabled`: (bool) Enable AFK animation effects.
- `storage.flush-interval-seconds`: (int) How often (in seconds) AFK time is saved to disk.
- `anti`:
	- `infinite-waterflow`: (bool) Prevents water flow AFK bypass.
	- `infinite-vehicle`: (bool) Prevents vehicle AFK bypass.
	- `bubble-column`: (bool) Prevents bubble column AFK bypass.
	- `flag-only`: (bool) If true, bypass attempts silently mark as AFK instead of alerting.
- `tab-prefix`:
	- `enabled`: (bool) Enable AFK prefix in TAB list.
	- `mode`: (string) `auto`, `tab`, or `custom`. Controls integration with TAB plugin.
	- `prefix`: (string) Prefix for AFK players in TAB.
	- `suffix`: (string) Suffix for AFK players in TAB.
	- `format`: (string) Full format for TAB display name. Placeholders: `%prefix%`, `%player%`, `%suffix%`.
- `display-name`:
	- `enabled`: (bool) Change in-game display name for AFK players.
	- `prefix`, `suffix`, `format`: As above, but for chat/display name.

### unafk
- `broadcast.enabled`: (bool) Broadcasts a message when a player is no longer AFK.
- `title.enabled`: (bool) Show a title message when a player returns from AFK.
- `animation.enabled`: (bool) Enable animation when returning from AFK.

### economy
- `enabled`: (bool) Enable economy-based AFK costs (requires Vault).
- `bypass-permission`: (string) Permission to bypass AFK costs.
- `cost.enter.enabled`: (bool) Charge when a player becomes AFK.
- `cost.enter.amount`: (double) Amount to charge on AFK.
- `cost.enter.require-funds`: (bool) If true, AFK toggle is cancelled if player can't pay.
- `cost.enter.retry-delay`: (int, seconds) Delay before retrying AFK toggle after failed charge.
- `cost.recurring.enabled`: (bool) Continuously charge AFK players.
- `cost.recurring.amount`: (double) Amount to charge per interval.
- `cost.recurring.interval`: (int, seconds) Time between recurring charges.
- `cost.recurring.require-funds`: (bool) If true, player is removed from AFK if they can't pay.
- `cost.recurring.kick-on-fail`: (bool) If true, removes AFK state on failed optional charge.

### kick
- `enabled`: (bool) Enable kicking after being AFK too long.
- `enabledWhenFull`: (bool) Enable kicking when server is full.
- `timeout`: (int, seconds) Time before kicking for AFK.
- `warnings.enabled`: (bool) Enable warnings before kick.
- `warnings.intervals`: (list[int]) Seconds before kick to warn.
- `warnings.mode`: (string) `chat`, `title`, or `both`.

### integration
- `worldguard`: (bool) Enable WorldGuard integration.
- `tab`: (bool) Enable TAB plugin integration.
- `playtime.enabled`: (bool) Enable Playtime plugin integration.
- `playtime.placeholder`: (string) Placeholder for total playtime in seconds.
- `spigot.check-for-update`: (bool) Check for plugin updates on startup.

---

## gui.yml

### inventory-size
- (int) Number of slots in the GUI. Must be a multiple of 9, between 9 and 54.

### actions
Each action is a named section (e.g., `kick`, `alert`, `teleport`).
- `slot`: (int) Slot index in the GUI (0-based).
- `material`: (string) Minecraft material for the item icon.
- `display-name`: (string) Name shown in the GUI (supports color codes).
- `type`: (string) Action type. Supported: `KICK`, `MESSAGE`, `TELEPORT`, `COMMAND`.
- `target-message`: (string, optional) Message sent to the AFK player (for KICK/MESSAGE).
- `feedback-message`: (string, optional) Message sent to the staff member who used the action.
- Placeholders: `%player%` (AFK player), `%executor%` (staff member).

---

## mysql.yml

- `enabled`: (bool) Enable MySQL storage for AFK data.
- `host`: (string) MySQL server address.
- `port`: (int) MySQL server port.
- `database`: (string) Database name.
- `username`: (string) Database user.
- `password`: (string) Database password. Leave blank for security; do not commit real credentials.

---

## messages/*.yml

Language files for all plugin messages. You can edit or add translations. Each key corresponds to a message or message group used by the plugin. Placeholders are documented in the config and code comments.

---
For more details, see the README.md or the docs/ folder.
