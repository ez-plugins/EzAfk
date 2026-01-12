# PlaceholderApiIntegration

## Overview
The PlaceholderApiIntegration module enables seamless integration between EzAfk and the PlaceholderAPI plugin, allowing you to display dynamic AFK-related data in other plugins, chat, scoreboards, and GUIs.

## Features
- Provides custom EzAfk placeholders for use in supported plugins and server configurations.
- Enables real-time display of player AFK status, AFK duration, and other relevant data.
- Compatible with any plugin or configuration that supports PlaceholderAPI.

## Setup & Usage
1. Install the [PlaceholderAPI](https://www.spigotmc.org/resources/placeholderapi.6245/) plugin on your server.
2. Ensure EzAfk is installed and running.
3. EzAfk placeholders will be automatically registered and available for use.
4. Use EzAfk placeholders in supported plugins, chat formats, scoreboards, or GUIs. Example placeholders:
	- `%ezafk_is_afk%` — Returns `true` if the player is AFK, otherwise `false`.
	- `%ezafk_afk_time%` — Displays the player's current AFK duration.
	- `%ezafk_afk_reason%` — Shows the reason for the player's AFK status.
5. Refer to your other plugins' documentation for instructions on adding placeholders to their configuration files.

## Notes
- No additional configuration is required; integration is automatic.
- For a full list of available EzAfk placeholders, consult the EzAfk documentation or use `/papi list ezafk` in-game.
