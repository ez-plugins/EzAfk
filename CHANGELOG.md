# Changelog

All notable changes to EzAfk are documented here.

Format follows [Keep a Changelog](https://keepachangelog.com/en/1.0.0/).
Versions follow [Semantic Versioning](https://semver.org/spec/v2.0.0.html).
Release tags use the `v` prefix (e.g. `v3.0.0`).

---

## [Unreleased]

---

## [3.1.1] - 2026-07-06

[Modrinth](https://modrinth.com/plugin/ezafk/version/3.1.1)

### Fixed

- AFK broadcast and return messages now resolve PlaceholderAPI placeholders
  with the AFK player's context, so integrations like LuckPerms prefixes work
  correctly in `messages.yml`.

---

## [3.1.0] - 2026-05-23

[Modrinth](https://modrinth.com/plugin/ezafk/version/3.1.0)

### Added

- **Bypass whitelist and blacklist** - Two persistent lists (stored in
  `bypass-lists.yml`) let admins fine-tune who bypasses AFK detection:
  - **Whitelist** — players that *always* bypass, regardless of the
    `afk.bypass.enabled` config flag or whether they hold the `ezafk.bypass`
    permission.
  - **Blacklist** - players that are *never* allowed to bypass, even when they
    hold `ezafk.bypass` (useful for ops/admins who want to be subject to AFK
    detection and zone rewards for testing or fairness).
  - Blacklist takes precedence over whitelist.
  - Managed in-game with `/afk bypass whitelist <add|remove|list> [player]`
    and `/afk bypass blacklist <add|remove|list> [player]`
    (requires `ezafk.bypass.manage`).
  - Tab completion is provided for all three argument depths.
  - Lists are reloaded on `/afk reload`.
- **AFK zone entry / exit messages** - Players now receive a chat message when
  they enter or leave an AFK zone (`afkzone.enter` / `afkzone.exit` message
  keys in `messages.yml`; both support the `%zone%` placeholder).

- **AFK zone reward countdowns via EzCountdown** - When
  [EzCountdown](https://github.com/ez-plugins/EzCountdown) is installed,
  players in AFK zones see a live countdown to their next reward (action bar,
  title, boss bar, scoreboard, or dialog — configurable per zone). The countdown
  resets automatically after each payout.
- **Global zone reward defaults** - A top-level `defaults.reward` block in
  `zones.yml` lets you configure economy reward amount, interval, and
  notification settings once for all zones. Individual zones can still override
  any value. When Vault is present and `defaults.reward.enabled: true`, all
  zones automatically grant small economy rewards without per-zone configuration.
- **Session reward stats per player per zone** - EzAfk now tracks how many
  rewards and how much currency each player has received in each zone for the
  current server session (resets on restart). Available via PlaceholderAPI:
  - `%ezafk_zone_rewards_grants%` - total grants across all zones
  - `%ezafk_zone_rewards_amount%` - total currency earned across all zones
  - `%ezafk_zone_reward_<zone>_grants%` - grants in a specific zone
  - `%ezafk_zone_reward_<zone>_amount%` - currency earned in a specific zone
- New notification config fields on zone rewards:
  `reward.notification.enabled`, `reward.notification.displays`,
  `reward.notification.message`, `reward.notification.duration`.

### Fixed

- Duplicate `reward:` YAML key under `afkzone:` in `messages.yml` — the second
  block (`failed:`) was silently overwriting the first (`granted.*`), making all
  reward granted messages fall back to their hardcoded defaults.


  [EzCountdown](https://github.com/ez-plugins/EzCountdown) plugin. Detected
  automatically when present (`integration.ezcountdown: auto`).
- **Configurable kick-warning display types** — `kick.warnings.displays` accepts
  a list of display types: `CHAT`, `TITLE`, `ACTION_BAR`, `BOSS_BAR`. Multiple
  types can be active at once (e.g. `[ACTION_BAR, BOSS_BAR]`). When the list is
  empty the legacy `kick.warnings.mode` value is used for backward compatibility.
- New message keys `kick.warning.action_bar` and `kick.warning.boss_bar` in
  `messages.yml` for the new display types.

### Changed

- `CompatibilityUtil` now handles `sendActionBar`, `showBossBarWarning`, and
  `removeWarningBossBar` via reflection - safe on servers without the BossBar
  API (pre-1.9) or without the action-bar method available.
- **Java 21+ is now required.** `maven.compiler.release` raised from `11` to
  `21`; the plugin JAR targets Java 21 bytecode (class file version 65).
  Servers running Minecraft 1.21+ already require Java 21, so this is a no-op
  for all supported server versions.
- Minimum supported Minecraft version is now **1.21** (was 1.19). Older
  versions are no longer tested or guaranteed to work.

### Removed

- Paper 1.20.4 / Java 17 legacy CI smoke test (not needed after dropping Java 17
  support).

---

## [3.0.0] - 2026-05-22

### Added

- Initial 3.x release.
