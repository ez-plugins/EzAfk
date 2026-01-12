---
title: AFK Kick Warnings
---

# AFK Kick Warnings Feature

EzAfk supports configurable warning messages before a player is kicked for being AFK. This helps notify players and gives them a chance to return before being removed from the server.

## Configuration

In your `config.yml`:

```yaml
kick:
  warnings:
    enabled: true
    intervals: [60, 30, 10] # Warn at 60s, 30s, and 10s before kick
    mode: both # Options: chat, title, both
```

- **enabled**: Turns the warning system on or off.
- **intervals**: List of seconds before kick to send warnings. You can use any positive integers.
- **mode**: How to send warnings. `chat` = chat message, `title` = title/subtitle, `both` = both.

## Customizing Messages

Edit `messages/messages.yml`:

```yaml
kick:
  warning:
    chat: "&eYou will be kicked for being AFK in &c%seconds% &eseconds!"
    title:
      title: "&cAFK Warning"
      subtitle: "&eKicked in &c%seconds% &esec!"
```

You can use `%seconds%` as a placeholder for the time remaining.

## How It Works

- Warnings are sent only once per interval per AFK session.
- If a player returns from AFK or is kicked, the warning state resets.
- Warnings are only sent if the player is AFK and within the kick timeout window.

## Example

If your kick timeout is 600 seconds (10 minutes) and intervals are `[60, 30, 10]`, players will be warned at 9:00, 9:30, and 9:50 of inactivity.

---
For more, see the main documentation or contact the plugin author.