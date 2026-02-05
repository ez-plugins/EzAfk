# EzAfk

[![Build](https://github.com/ez-plugins/EzAfk/actions/workflows/maven-publish.yml/badge.svg)](https://github.com/ez-plugins/EzAfk/actions)
[![Release](https://img.shields.io/github/v/release/ez-plugins/EzAfk)](https://github.com/ez-plugins/EzAfk/releases)
[![Downloads](https://img.shields.io/github/downloads/ez-plugins/EzAfk/total)](https://github.com/ez-plugins/EzAfk/releases)
[![License](https://img.shields.io/badge/license-MIT-green.svg)](LICENSE)

EzAfk is a powerful and flexible AFK (Away From Keyboard) management plugin for Minecraft servers. It provides advanced AFK detection, player management, and integration with popular plugins and server features.

## Features
- Automatic AFK detection and management
- AFK kick warnings and configurable actions
- GUI for AFK player overview and actions
- Integration with Economy, Tab, PlaceholderAPI, WorldGuard, and more
- Multi-language support (EN, ES, NL, RU, ZH)
- MySQL support for persistent data
- Highly configurable via YAML files

## Installation
1. Download the latest EzAfk jar from the releases page.
2. Place the jar in your server's `plugins` directory.
3. Start or reload your server.
4. Edit the configuration files in the `plugins/EzAfk` folder as needed.

## Configuration
- `config.yml`: Main plugin settings
- `gui.yml`: GUI layout and options
- `mysql.yml`: Database connection settings
- `messages/`: Language files for plugin messages

## Documentation
Comprehensive documentation is available in the [`docs/`](docs/) folder:

- [Configuration](docs/configuration.md): Detailed guide for all configuration options.
- [Commands](docs/commands.md): Full list of commands and usage examples.
- [Events](docs/api/events.md): Information on custom events for plugin developers.
- [API Reference](docs/api/AfkReasons.md): All possible AFK reasons and their usage.
- [Integrations](docs/integrations/README.md): Guides for integrating with Economy, Tab, PlaceholderAPI, WorldGuard, and more.
- [FAQ](docs/faq.md): Frequently asked questions and troubleshooting tips.
- [Permissions](docs/permissions.md): Complete list of permissions and their descriptions.
- [Messages](docs/messages.md): Customization of plugin messages and language support.

Refer to these documents for setup, customization, and advanced usage.

## Commands
- `/afk` — Toggle your AFK status
- `/afk reload` — Reload configuration
- `/afk gui` — Open AFK player overview GUI
- `/afk toggle <player>` — Toggle AFK for another player
- `/afk bypass <player>` — Toggle AFK bypass for a player
- `/afk info <player>` — Show AFK info for a player
- `/afk time [player]` — Show total AFK time
- `/afk time reset <player>` — Reset a player's total AFK time (admin)
- `/afk top` — Show AFK leaderboard
- `/afk zone list` — List configured AFK zones
- `/afk zone players` — List players currently in AFK zones
- `/afk zone add <name>` — Add a new AFK zone (uses WorldEdit selection, stored pos1/pos2, or coordinates)
- `/afk zone remove <name>` — Remove a configured AFK zone
- `/afk zone pos1` — Store position 1 for zone creation
- `/afk zone pos2` — Store position 2 for zone creation
- `/afk zone clearpos [player]` — Clear stored positions for yourself or target player
- `/afk zone reset <player> [zone]` — Reset zone reward counts for a player (optionally for a specific zone)

- Aliases: you can run the same commands via `/ezafk` or the short alias `/ea`. The plugin also registers `afktime`, `afktop`, and `afkzone` as command aliases.

## Permissions
See the documentation for a full list of permissions.

## Contributing
Contributions are welcome! Please see CONTRIBUTING.md for guidelines.

## License
This project is licensed under the MIT License. See LICENSE for details.
