---
title: Economy / Vault
nav_order: 2
parent: Integrations
---

# Economy / Vault Integration

EzAfk integrates with any Vault-compatible economy plugin to charge players when they go AFK or
while they remain AFK. Zones can also reward economy currency to incentivise AFK in specific areas.

## Requirements

- [Vault](https://www.spigotmc.org/resources/vault.34315/) installed and enabled.
- A Vault-compatible economy plugin (e.g. [EzEconomy](https://www.spigotmc.org/resources/1-7-1-21-ezeconomy-modern-vault-economy-plugin-for-minecraft-servers.130975/),
  EssentialsX Economy, CMI, etc.).

## Setup

1. Install Vault and your economy plugin.
2. Enable the economy feature in `config.yml`:

```yaml
economy:
  enabled: true
```

3. Restart the server. EzAfk will detect Vault automatically and activate economy integration.

## Configuration Summary

Full documentation for all economy settings is on the [Economy Costs](../features/economy-costs) feature page.

```yaml
economy:
  enabled: false
  bypass-permission: "ezafk.economy.bypass"
  cost:
    enter:
      enabled: true
      amount: 25.0
      require-funds: true
      retry-delay: 60
    recurring:
      enabled: false
      amount: 5.0
      interval: 300
      require-funds: true
      kick-on-fail: false
```

## AFK Zone Rewards

AFK Zones can grant economy rewards to players who stay AFK inside a defined region. Configure this
in `zones.yml` with `reward.type: economy`. See [AFK Zones](../features/afk-zones) for details.

## Related

- [Economy Costs](../features/economy-costs): detailed config reference
- [AFK Zones](../features/afk-zones): reward economy currency in specific regions
- [Permissions](../permissions): `ezafk.economy.bypass`
