# EzAfk Messages & Localization Guide

EzAfk supports full message customization and translation through YAML files in the `messages/` directory. This guide explains how to edit, translate, and use placeholders in message files.

---

## Message Files
- Located in `messages/` (e.g., `messages_en.yml`, `messages_es.yml`, etc.).
- The active language is set in `config.yml` under `messages.language`.
- If a translation is missing a key, the default (English) message is used.

## Editing Messages
- Open the relevant YAML file in `messages/`.
- Edit the value for any message key. Color codes (`&a`, `&c`, etc.) are supported.
- Save and reload the plugin to apply changes.

## Adding a New Language
1. Copy `messages_en.yml` to a new file (e.g., `messages_fr.yml`).
2. Translate the values for each key.
3. Set `messages.language: fr` in `config.yml`.

## Placeholders
Many messages support placeholders that are replaced at runtime:
- `%player%`: Player's display name
- `%afk_count%`: Number of AFK players
- `%active_count%`: Number of active players
- `%duration%`: Formatted time duration
- `%reason%`: AFK reason
- `%detail%`: Additional AFK details
- `%position%`: Leaderboard position
- `%executor%`: Name of the player who triggered an action

Refer to the configuration and command documentation for where each placeholder is used.

## Best Practices
- Do not remove keys; if you don't want a message, set its value to an empty string.
- Keep formatting consistent for readability.
- Test your changes in-game.

---
For more information, see the configuration and commands documentation.
