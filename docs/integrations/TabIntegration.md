# TabIntegration

## Overview
The TabIntegration module provides seamless compatibility between EzAfk and the TAB plugin, enabling advanced management of the player list and dynamic display of AFK status within the TAB interface.

## Features
- Automatically updates player list entries in TAB to reflect AFK status, including custom prefixes, suffixes, and formatting.
- Ensures real-time synchronization of AFK status changes with the TAB plugin.
- Supports integration with other plugins that modify player list names.
- No manual intervention required for updates; integration is fully automated.

## Setup & Usage
1. Install the [TAB plugin](https://www.spigotmc.org/resources/tab-1-5-x-1-20-x-free-version.57806/) on your server.
2. Ensure EzAfk is installed and running.
3. EzAfk will automatically detect and integrate with TAB if present.
4. AFK status and custom formatting will be reflected in the TAB player list without further configuration.

## Notes
- For advanced customization, refer to EzAfk's configuration options for display name formatting and AFK indicators.
- No additional setup is required; integration is automatic once both plugins are installed.

## Using EzAfk's `%afk%` placeholder with TAB

EzAfk exposes an `%afk%` placeholder to TAB that resolves to the AFK prefix (or an empty string when the player is not AFK). This allows server owners to build flexible TAB name and tablist templates that include AFK indicators while keeping formatting centralized in EzAfk.

Quick summary:
- EzAfk provides `%afk%` (color codes are supported, use `&` notation in EzAfk config).
- Enable EzAfk's TAB placeholder and prefix behavior in EzAfk's `config.yml`.
- Insert `%afk%` into TAB's name / tablist templates where you want the AFK marker to appear.

Steps — EzAfk configuration
1. Open EzAfk's `config.yml` (located in `plugins/EzAfk/config.yml`).
2. Under the `integration` section enable EzAfk's tab-prefix support:

```yaml
integration:
	tab: true
	tab-prefix:
		enabled: true
		mode: auto        # auto, tab, or custom
		prefix: "&7[AFK] "
		suffix: ""
		format: "%prefix%%player%%suffix%"
```

- `integration.tab-prefix.enabled`: when `true`, EzAfk registers the `%afk%` placeholder with TAB and supplies the prefix text.
- `mode`: controls whether EzAfk prefers TAB (`tab`), always uses EzAfk's built-in list handling (`custom`), or auto-detects (`auto`).
- `prefix` / `suffix` / `format`: control the text EzAfk returns via `%afk%` and how it composes player display names.

Steps — TAB plugin configuration
1. Open TAB's `config.yml` (usually `plugins/TAB/config.yml`).
2. Find the section that controls player name formatting or the tablist layout. This depends on your TAB version and layout configuration — common places are `tablist`, `groups`, or `player-names`.
3. Insert `%afk%` into the template where you want the AFK indicator to appear. Examples:

Example A — add AFK prefix to the display name template:

```yml
# TAB example (conceptual)
player-placeholder-format: "%afk%%displayname%"
```

Example B — include AFK in a group format or global tablist format:

```yml
tablist-format: "%afk%%player%%suffix%"
```

Notes for TAB templates:
- TAB configuration and template keys vary across TAB versions and setups (layouts, per-group templates, per-world templates). Search TAB's `config.yml` for `displayname`, `format`, or `tablist` to find the right template.
- TAB uses its own placeholders and supports external placeholders registered by plugins — `%afk%` will appear in the same placeholder namespace.

Troubleshooting
- No `%afk%` replacement shown:
	- Ensure EzAfk's `integration.tab-prefix.enabled: true` and `integration.tab: true`.
	- Make sure `integration.tab-prefix.prefix` is not empty (EzAfk will return an empty string if prefix is empty).
	- Check server startup logs for EzAfk TAB diagnostics — EzAfk logs whether the TAB adapter and `%afk%` placeholder registered successfully.
	- If EzAfk logs a `REFLECTION_ERROR` or `LINKAGE_ERROR`, update EzAfk to a build that matches your server and TAB versions (or check for shaded/class visibility issues). See EzAfk logs for a detailed stack trace.
- `%afk%` sometimes appears late on player join:
	- TAB processes players asynchronously. Use TAB's events (PlayerLoadEvent / TabLoadEvent) if you need guaranteed ordering. EzAfk retries initialization on startup to handle plugin load ordering.

Best practices
- Keep AFK text and color configuration in EzAfk so a single plugin controls AFK formatting across chat, tab, and other integrations.
- If you manage complex TAB layouts, test changes on a staging server — templates differ per TAB version.

If you give me your `plugins/TAB/config.yml` (the formatting/layout section), I can provide the exact edit to insert `%afk%` in your current setup.
