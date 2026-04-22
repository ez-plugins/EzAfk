---
title: In-Game GUI
nav_order: 5
parent: Features
---

# In-Game GUI

EzAfk provides a chest-inventory GUI that lets staff quickly view all AFK players and take immediate
action — kick, message, teleport, or run a command — without leaving the game. The GUI is accessed with
`/afk gui` and is fully configurable in `gui.yml`.

## Configuration

All GUI settings live in `gui.yml` (not `config.yml`):

```yaml
inventory-size: 9       # multiples of 9 (9 – 54); one slot per AFK player head

actions:
  kick:
    slot: 0
    material: BARRIER
    display-name: "&cKick"
    lore:
      - "&7Click to kick %player%"
    type: KICK
    feedback-message: "&aKicked %player%."

  message:
    slot: 1
    material: PAPER
    display-name: "&eSend Message"
    lore:
      - "&7Click to message %player%"
    type: MESSAGE
    target-message: "&e[Staff] %executor% wants your attention!"
    feedback-message: "&aMessage sent to %player%."

  teleport:
    slot: 2
    material: ENDER_PEARL
    display-name: "&aTeleport"
    lore:
      - "&7Teleport to %player%"
    type: TELEPORT
    feedback-message: "&aTeleported to %player%."

  command:
    slot: 3
    material: COMMAND_BLOCK
    display-name: "&6Run Command"
    lore:
      - "&7Runs a command as the console"
    type: COMMAND
    command: "say %player% is AFK!"
    feedback-message: "&aCommand executed."

empty-slot-filler:
  enabled: true
  material: GRAY_STAINED_GLASS_PANE
  display-name: " "
  lore: []

back-button:
  display-name: "&7← Back"
  lore:
    - "&7Return to previous page"
```

### Inventory

- **`inventory-size`**: (integer) Chest size in slots. Must be a multiple of 9 between 9 and 54.
  Each AFK player occupies one slot (shown as their head). When more players are AFK than there are
  slots, a paged navigation is provided. Default: `9`.

### Actions

Each entry under `actions` defines a clickable button shown in the per-player detail view:

- **`slot`**: (integer, 0-based) Inventory slot position of this action button.
- **`material`**: (string) Bukkit material name for the button icon.
- **`display-name`**: (string, `&` colour codes) Button title text.
- **`lore`**: (list of strings) Tooltip lines displayed below the display name.
- **`type`**: (string) What happens on click. One of:
  - `KICK` — kicks the target player.
  - `MESSAGE` — sends `target-message` to the target player.
  - `TELEPORT` — teleports the executor to the target player.
  - `COMMAND` — runs `command` as the console.
- **`target-message`**: (string) Message delivered to the AFK player. Used by `MESSAGE` type.
- **`feedback-message`**: (string) Message sent back to the staff member after the action completes.
- **`command`**: (string) Console command to run. Used by `COMMAND` type.

**Placeholders available in all text fields:**

| Placeholder | Value |
|-------------|-------|
| `%player%` | Name of the AFK player being acted upon |
| `%executor%` | Name of the staff member using the GUI |

### Empty Slot Filler

- **`empty-slot-filler.enabled`**: (bool) Fill all unused inventory slots with a decorative item.
  Default: `true`.
- **`empty-slot-filler.material`**: Background item material (e.g. `GRAY_STAINED_GLASS_PANE`).
- **`empty-slot-filler.display-name`**: Display name for filler items (use `" "` for invisible).
- **`empty-slot-filler.lore`**: Lore lines for the filler item.

### Back Button

- **`back-button.display-name`** / **`back-button.lore`**: Appearance of the paged-navigation back
  arrow shown when the AFK list spans multiple pages.

## How It Works

1. `/afk gui` opens the inventory, populating each slot with the head of a currently AFK player.
2. Clicking a player head opens a detail view showing all configured action buttons.
3. Clicking an action button performs the action immediately and sends the `feedback-message` to the
   executor.
4. The `empty-slot-filler` fills any remaining slots that are not occupied by a player head.
5. If there are more AFK players than `inventory-size`, navigation buttons let staff page through the
   list.

## Related

- [Commands](../commands) — `/afk gui` and GUI-related subcommands
- [Permissions](../permissions) — `ezafk.gui`, `ezafk.gui.view-active`, `ezafk.gui.actions`
