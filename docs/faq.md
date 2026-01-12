# EzAfk FAQ (Frequently Asked Questions)

This document answers common questions and provides solutions to frequent issues with EzAfk.

---

## General

**Q: How do I install EzAfk?**
A: Place the EzAfk jar in your server's `plugins` folder and restart the server.

**Q: How do I change the plugin language?**
A: Set `messages.language` in `config.yml` to your desired language code (e.g., `en`, `es`, `nl`, `ru`, `zh`).

**Q: How do I reload the configuration?**
A: Use `/afk reload` (requires `ezafk.reload` permission).

**Q: How do I exempt a player from AFK detection?**
A: Grant them the `ezafk.bypass` permission or use `/afk bypass <player>` (requires `ezafk.bypass.manage`).

---

## Troubleshooting

**Q: MySQL is not connecting.**
A: Check your `mysql.yml` settings, ensure the database is running, and verify credentials. See server logs for errors.

**Q: WorldGuard/TAB/PlaceholderAPI integration is not working.**
A: Make sure the plugin is installed and enabled. Check `config.yml` integration settings and server logs for errors.

**Q: Players are not being kicked for AFK.**
A: Ensure `kick.enabled` is true in `config.yml` and the timeout is set correctly. Check for permission conflicts.

**Q: Messages are not translated.**
A: Make sure the correct language file exists in `messages/` and is set in `config.yml`.

---

## Advanced

**Q: How do I add a new action to the AFK GUI?**
A: Edit `gui.yml` and add a new entry under `actions:` following the format of existing actions.

**Q: How do I customize placeholders?**
A: See `docs/messages.md` for a list of supported placeholders and where they are used.

---
For more help, see the README.md, configuration guide, or open an issue on GitHub.
