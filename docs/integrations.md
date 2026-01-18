# EzAfk Integrations Guide
EzAfk supports integration with several popular Minecraft plugins to enhance AFK detection and player management. This guide explains each integration, setup steps, and troubleshooting tips.

## Supported Integrations

Register your listener as usual in your plugin to receive these events.
### WorldGuard
- **Purpose:** Allows region-based AFK bypass and custom flags.
- **Setup:**
  - Install WorldGuard on your server.
  - Set `integration.worldguard: true` in `config.yml`.
  - EzAfk will auto-detect WorldGuard and register custom flags if available.
- **Troubleshooting:**
  - Ensure WorldGuard is enabled and up to date.
  - Check server logs for integration errors.

### TAB
- **Purpose:** Customizes player names in the tab list when AFK.
- **Setup:**
  - Install the TAB plugin.
  - Set `integration.tab: true` in `config.yml`.
  - Configure `afk.tab-prefix` options for prefix, suffix, and format.
- **Troubleshooting:**
  - Ensure TAB is enabled and compatible with your server version.

### Vault (Economy)
- **Purpose:** Enables economy-based AFK costs and rewards.
- **Setup:**
  - Install Vault and an economy provider (e.g., EssentialsX Economy).
  - Set `economy.enabled: true` in `config.yml`.
  - Configure cost options under `economy.cost`.
- **Troubleshooting:**
  - Ensure Vault and your economy plugin are enabled.
  - Check for errors in the server log.

### PlaceholderAPI
- **Purpose:** Adds placeholders for AFK status, time, and more.
- **Setup:**
  - Install PlaceholderAPI.
  - Placeholders are registered automatically if PlaceholderAPI is present.
- **Troubleshooting:**
  - Use `/papi ecloud download EzAfk` if available.

### Playtime
- **Purpose:** Adjusts playtime statistics to exclude AFK time.
- **Setup:**
  - Install Playtime plugin.
  - Set `integration.playtime.enabled: true` in `config.yml`.
  - Configure the playtime placeholder as needed.

### Simple Voice Chat
- **Purpose:** Plays a custom MP3 sound to the player when they go AFK using Simple Voice Chat.
- **Setup:**
  - Install the Simple Voice Chat plugin on your server and mod on your client.
  - Set `afk.sound.enabled: true` and specify the MP3 file path in `config.yml` (default: `mp3/ezafk-sound.mp3`).
  - Set `integration.voicechat: auto` (or `true`/`false`) in `config.yml`.
  - Place your MP3 file in `plugins/EzAfk/mp3/` or use the default provided.
- **Troubleshooting:**
  - Ensure Simple Voice Chat is installed and enabled on both server and client.
  - The MP3 file must be valid and compatible (48kHz recommended).
  - Check server logs for any errors related to sound playback or decoding.
  - Verify you are connected to voice chat and the config is correct.

---

## General Troubleshooting
- Ensure all plugins are compatible with your server version.
- Reload or restart the server after installing new plugins.
- Check the server log for integration-related errors or warnings.

---
For more details, see the README.md and configuration guide.
