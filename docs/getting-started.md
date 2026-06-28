---
title: Getting Started
nav_order: 2
---

# Getting Started

This page walks you through installing EzAfk on your server and running it for the first time.

---

## Requirements

- Paper, Spigot, Bukkit, or Purpur **1.21+**
- Java **21** or newer

### Server version compatibility

EzAfk supports Minecraft **1.21 and newer**. Some features rely on additions made in later releases;
the table below shows where runtime behaviour differs by MC version.

| Feature | Min MC | Notes |
|---------|--------|-------|
| Core AFK detection, kick, GUI, commands | 1.19 | Fully supported |
| Kick with proper disconnect reason (`PlayerKickEvent.Cause`) | 1.19.2 | Falls back to a simple kick message on 1.19.0 – 1.19.1 |
| Bubble column anti-bypass | 1.13 | `BUBBLE_COLUMN` material added in 1.13; always available on 1.19+ |
| Cherry Leaves animation particle | 1.20 | Silently skipped on 1.19.x; all other animation particles still play |
| Simple Voice Chat sound | Any | Requires the [Simple Voice Chat](https://modrinth.com/plugin/simple-voice-chat) plugin |
| Economy costs / zone economy rewards | Any | Requires a [Vault](https://www.spigotmc.org/resources/vault.34315/)-compatible economy plugin |
| GUI item metadata (PersistentDataContainer) | 1.14 | Falls back to lore-based metadata on older versions |
| InventoryView API | 1.21+ | Class → interface change in 1.21 handled automatically |

See [Configuration](configuration) for every available option.

---

## Installation

1. Download the latest `EzAfk-x.x.x.jar` from [Modrinth](https://modrinth.com/plugin/ezafk).
2. Place the jar in your server's `plugins/` folder.
3. Start or restart the server. EzAfk will generate all configuration files.

---

## Generated files

After first startup, EzAfk creates the following inside `plugins/EzAfk/`:

| File | Purpose |
|------|---------|
| `config.yml` | Main settings: AFK timeout, kick, GUI, zones, anti-bypass |
| `gui.yml` | In-game GUI layout and item definitions |
| `mysql.yml` | MySQL/SQLite database connection settings |
| `bypass-lists.yml` | Persistent bypass whitelist and blacklist |
| `messages/` | One YAML file per language (en, es, nl, ru, zh, de) |

---

## First configuration steps

**1. Set your AFK timeout**

Open `config.yml` and find the AFK detection section. Set the idle threshold in seconds:

```yaml
afk:
  timeout: 300   # mark a player AFK after 5 minutes of no activity
```

**2. Configure kick behaviour (optional)**

```yaml
kick:
  enabled: true
  timeout: 600    # kick after 10 minutes of being AFK
  warnings:
    enabled: true
    intervals: [60, 30, 10]
```

See [Configuration](configuration) for every available option.

**3. Grant operator permissions**

The minimum set for an admin:

```text
ezafk.reload      - reload config without restarting
ezafk.gui         - open the AFK player overview GUI
ezafk.afk.others  - toggle AFK for another player
```

See [Permissions](permissions) for the full node list.

**4. Choose a language**

Set the active language in `config.yml`:

```yaml
messages:
  language: en   # en | es | nl | ru | zh | de
```

---

## Verifying the installation

Join your server and run `/afk`. If your status toggles, the plugin is working.
Run `/afk reload` to hot-reload config changes at any time (requires `ezafk.reload`).

---

## Next steps

| I want to… | Go to… |
|---|---|
| See all commands | [Commands](commands) |
| Tune every config option | [Configuration](configuration) |
| Understand each feature in depth | [Features](features/) |
| Set up MySQL for persistence | [Storage](mysql) |
| Connect PlaceholderAPI / WorldGuard / Economy | [Integrations](integrations) |
| Customise plugin messages | [Messages](messages) |
