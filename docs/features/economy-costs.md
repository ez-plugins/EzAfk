---
title: Economy Costs
nav_order: 7
parent: Features
---

# Economy Costs

EzAfk can charge players a currency fee when they go AFK or while they remain AFK. Both one-time
entry costs and recurring interval costs are supported. Costs are deducted via Vault, so any
Vault-compatible economy plugin (EssentialsX Economy, CMI, etc.) will work.

## Requirements

- [Vault](https://www.spigotmc.org/resources/vault.34315/) installed and enabled.
- A Vault-compatible economy plugin providing balances.
- `economy.enabled: true` in `config.yml`.

## Configuration

In your `config.yml`:

```yaml
economy:
  enabled: false
  bypass-permission: "ezafk.economy.bypass"

  cost:
    enter:
      enabled: true
      amount: 25.0          # currency deducted when a player first goes AFK
      require-funds: true   # if true, prevent going AFK when the player cannot afford it
      retry-delay: 60       # seconds before re-trying if require-funds blocked the transition

    recurring:
      enabled: false
      amount: 5.0           # currency deducted every interval while AFK
      interval: 300         # seconds between each deduction
      require-funds: true   # if true, apply kick-on-fail behaviour when funds are insufficient
      kick-on-fail: false   # kick the player when they can no longer afford the recurring cost
```

- **`economy.enabled`**: (bool) Master switch. When `false`, no economy activity occurs. Default: `false`.
- **`economy.bypass-permission`**: (string) Permission node that exempts a player from all economy
  costs. Default: `"ezafk.economy.bypass"`.

### Entry Cost (`cost.enter`)

- **`cost.enter.enabled`**: (bool) Charge a one-time fee when the player is first marked AFK.
  Default: `true`.
- **`cost.enter.amount`**: (decimal) Amount to deduct. Uses the economy plugin's default currency.
- **`cost.enter.require-funds`**: (bool) When `true`, EzAfk checks the player's balance before marking
  them AFK. If they cannot afford the fee, they are **not** marked AFK and receive an error message.
- **`cost.enter.retry-delay`**: (integer, seconds) If `require-funds` blocked the AFK transition,
  EzAfk waits this many seconds before trying again. This prevents the check from firing on every
  movement event.

### Recurring Cost (`cost.recurring`)

- **`cost.recurring.enabled`**: (bool) Deduct currency periodically while the player remains AFK.
  Default: `false`.
- **`cost.recurring.amount`**: (decimal) Amount deducted per interval.
- **`cost.recurring.interval`**: (integer, seconds) How often the deduction fires.
- **`cost.recurring.require-funds`**: (bool) When `true`, a failed deduction (insufficient balance)
  triggers the `kick-on-fail` behaviour instead of silently skipping.
- **`cost.recurring.kick-on-fail`**: (bool) When `true`, a player who can no longer afford the
  recurring cost is kicked from the server. When `false`, the recurring deduction is simply skipped.

## Customising Messages

Edit your active language file in `messages/` (e.g. `messages/en.yml`):

```yaml
economy:
  enter-cost: "&eGoing AFK costs &6{amount}&e."
  insufficient-funds: "&cYou don't have enough funds to go AFK."
  recurring-cost: "&eAFK cost: &6{amount} &ededucted."
  kicked-no-funds: "&cYou were kicked because you ran out of AFK funds."
```

See the [Messages](../messages) page for the full reference.

## How It Works

1. When a player is about to be marked AFK, EzAfk checks whether `cost.enter` is enabled.
2. If `require-funds` is `true` and the player cannot afford `cost.enter.amount`, the AFK transition
   is blocked and a message is sent. The check retries after `retry-delay` seconds.
3. If the player can afford it (or `require-funds` is `false`), the amount is deducted and the player
   is marked AFK.
4. Once AFK, a recurring task fires every `cost.recurring.interval` seconds if `cost.recurring.enabled`
   is `true`.
5. On each interval, EzAfk deducts `cost.recurring.amount`. If the deduction fails and
   `require-funds + kick-on-fail` are both `true`, the player is kicked.
6. Players with the bypass permission (or WorldGuard region bypass) skip all economy checks.

## Related

- [Economy / Vault Integration](../integrations/EconomyIntegration) — setup guide
- [AFK Zones](afk-zones) — grant economy rewards for being AFK in specific areas
- [Permissions](../permissions) — `ezafk.economy.bypass`
