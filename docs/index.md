---
layout: home
title: EzAfk
nav_order: 1
description: "Powerful AFK management for Minecraft servers"
permalink: /
---

# EzAfk

[![Build](https://github.com/ez-plugins/EzAfk/actions/workflows/maven-publish.yml/badge.svg)](https://github.com/ez-plugins/EzAfk/actions)
[![Release](https://img.shields.io/github/v/release/ez-plugins/EzAfk)](https://github.com/ez-plugins/EzAfk/releases)
[![Downloads](https://img.shields.io/github/downloads/ez-plugins/EzAfk/total)](https://github.com/ez-plugins/EzAfk/releases)
[![License](https://img.shields.io/badge/license-MIT-green.svg)](https://github.com/ez-plugins/EzAfk/blob/main/LICENSE)

**EzAfk** is a powerful and flexible AFK management plugin for Spigot, Bukkit, Paper, and Purpur servers.
It provides advanced AFK detection, player management, and deep integration with popular plugins and server systems.

---

## Features

- **Automatic AFK detection** — idle timeout with configurable thresholds
- **AFK kick warnings** — multi-stage countdown messages before kicking inactive players
- **GUI overview** — in-game panel to view and manage AFK players
- **AFK zones** — WorldGuard-region-based AFK rules and overrides
- **Multi-language support** — EN, ES, NL, RU, ZH, DE out of the box
- **Storage backends** — YAML, SQLite, and MySQL with a unified repository API
- **Anti-bypass detection** — catches water flow, vehicle, and bubble column movement
- **Developer API** — cancellable `PlayerAfkStatusChangeEvent` and `AfkReason` enum
- **Integrations** — Economy/Vault, PlaceholderAPI, Tab, WorldGuard, Simple Voice Chat

---

## Quick start

**1. Download and install:**

Download the latest EzAfk `.jar` from [GitHub Releases](https://github.com/ez-plugins/EzAfk/releases)
and place it in your server's `plugins/` folder. Restart the server.

**2. Configure:**

Edit the files generated in `plugins/EzAfk/`:

```
config.yml   — main settings (AFK timeout, kick, zones, …)
gui.yml      — GUI layout and item settings
mysql.yml    — database connection (if using MySQL)
messages/    — per-language message files
```

**3. Set permissions:**

Grant `ezafk.*` to administrators or assign individual nodes — see the
[Permissions](permissions) page for the full list.

---

## Documentation

| Page | What it covers |
|------|----------------|
| [Getting Started](getting-started) | Install, first config, verify it works |
| [Commands](commands) | All `/afk` commands, arguments, and permission nodes |
| [Configuration](configuration) | Every config option explained |
| [Configuration → AFK Kick Warnings](afk-kick-warnings) | Multi-stage kick warning system |
| [Permissions](permissions) | Permission nodes and defaults |
| [Messages](messages) | Customising plugin messages and language files |
| [Storage](mysql) | YAML, SQLite, and MySQL storage backends |
| [Integrations](integrations) | Economy, PlaceholderAPI, Tab, WorldGuard, and more |
| [FAQ & Troubleshooting](faq) | Common questions and solutions |
| [Developer API](api/) | Events and enums for plugin developers |
