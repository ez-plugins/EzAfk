---
title: Getting Started
nav_order: 2
---

# Getting Started

This page walks you through installing EzAfk on your server and running it for the first time.

---

## Requirements

- Paper, Spigot, Bukkit, or Purpur **1.19+**
- Java **17** or newer

---

## Installation

1. Download the latest `EzAfk-x.x.x.jar` from [GitHub Releases](https://github.com/ez-plugins/EzAfk/releases).
2. Place the jar in your server's `plugins/` folder.
3. Start or restart the server. EzAfk will generate all configuration files.

---

## Generated files

After first startup, EzAfk creates the following inside `plugins/EzAfk/`:

| File | Purpose |
|------|---------|
| `config.yml` | Main settings — AFK timeout, kick, GUI, zones, anti-bypass |
| `gui.yml` | In-game GUI layout and item definitions |
| `mysql.yml` | MySQL/SQLite database connection settings |
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

```
ezafk.reload      — reload config without restarting
ezafk.gui         — open the AFK player overview GUI
ezafk.afk.others  — toggle AFK for another player
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
| Set up MySQL for persistence | [Storage](mysql) |
| Connect PlaceholderAPI / WorldGuard / Economy | [Integrations](integrations) |
| Customise plugin messages | [Messages](messages) |
