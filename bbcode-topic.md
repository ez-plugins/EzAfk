███████╗███████╗░█████╗░███████╗██╗░░██╗
██╔════╝╚════██║██╔══██╗██╔════╝██║░██╔╝
█████╗░░░░███╔═╝███████║█████╗░░█████═╝░
██╔══╝░░██╔══╝░░██╔══██║██╔══╝░░██╔═██╗░
███████╗███████╗██║░░██║██║░░░░░██║░╚██╗
╚══════╝╚══════╝╚═╝░░╚═╝╚═╝░░░░░╚═╝░░╚═╝

[B][SIZE=6]Keep AFK management simple[/SIZE][/B]

[B]EzAfk[/B] is a modern, lightweight AFK management plugin that keeps your staff informed while gently nudging idle players back into the action. Built for Paper and Spigot servers (1.7 – 1.21.*), it automates AFK detection, provides configurable staff tools, and integrates with the systems you already use — all without sacrificing performance.

[IMG]https://img.shields.io/badge/Latest%20version-2.0.0-blue[/IMG]
[IMG]https://img.shields.io/badge/Minecraft%20version-1.7%20to%201.21.*-blue[/IMG]

[SIZE=5][B]Feature highlights[/B][/SIZE]
[LIST]
[*][B]Automatic AFK detection[/B]: Detect idle players after a configurable timeout, send chat or title messages, play optional animations, apply blindness, and broadcast status changes server-wide.
[*][B]Staff workflow tools[/B]: Open the AFK player overview GUI and trigger customizable actions (kick, alert, teleport, run console commands) per-player from an intuitive menu.
[*][B]Anti-bypass protections[/B]: Block common AFK bypass tricks with vehicle and water-flow checks, optionally gated behind the `ezafk.bypass` permission or WorldGuard regions.
[*][B]Automatic punishments[/B]: Kick AFK players after configurable grace periods or only when the server is full, with personalized kick reasons.
[*][B]AFK kick warnings[/B]: Send configurable chat and/or title warnings at multiple intervals before a player is kicked for being AFK, giving them a chance to return.
[*][B]Economy-aware AFK[/B]: Optionally charge players using Vault-supported economies when they go or stay AFK, with recurring billing plus bypass permissions or WorldGuard regions.
[*][URL=https://www.spigotmc.org/resources/1-21-ezeconomy-modern-vault-economy-plugin-for-minecraft-servers.130975/][B]EzEconomy integration[/B][/URL]: For best results, pair EzAfk with [B]EzEconomy[/B] for modern, reliable Vault economy support!
[*][B]Integrations that matter[/B]: Use the WorldGuard `afk-bypass` flag, track usage with bStats, receive console reminders when new releases are available, and surface AFK prefixes in the tab list without any external dependencies.
[*][B]Custom display names & tab styling[/B]: Mirror AFK status in chat and name tags with configurable prefixes, suffixes, and formats that work with TAB or the built-in formatter.
[*][B]Persistent storage[/B]: Optionally connect to MySQL to store the last active timestamp for every tracked player across restarts, plus per-player YAML totals for the AFK time leaderboard.
[*][B]AFK analytics[/B]: Surface `/afk time`, `/afk info`, and `/afk top` so staff can investigate reports and highlight the most idle players with a cached leaderboard.
[*][B]Player-friendly deterrents[/B]: Combine optional blindness and configurable animations to nudge players back to activity without being heavy-handed.
[*][B]Built-in translations[/B]: Ship ready-to-use English, Spanish, Dutch, Russian, and Chinese message packs with automatic fallbacks and per-server overrides.
[/LIST]

[SIZE=5][B]Commands[/B][/SIZE]
[LIST]
[*][B]/afk[/B]: Toggle your own AFK state (alias of `/ezafk`).
[*][B]/afk reload[/B]: Reload the configuration and refresh integrations.
[*][B]/afk gui[/B]: Open the AFK player overview GUI (includes pagination and quick actions).
[*][B]/afk toggle <player>[/B]: Force another player's AFK state.
[*][B]/afk bypass <player>[/B]: Toggle the bypass flag for a player when `afk.bypass.enabled` is active.
[*][B]/afk info <player>[/B]: Review why a player was flagged AFK, how long they've been idle, and their last activity.
[*][B]/afk time [player][/B]: Show lifetime AFK totals for yourself or, with permission, another player.
[*][B]/afk top[/B]: Display the cached AFK time leaderboard (also available via `/afktop`).
[I]Aliases[/I]: `/ezafk`, `/afk`, `/ea`, `/afktime`, `/afktop`
[/LIST]

[SIZE=5][B]Permissions[/B][/SIZE]
[LIST]
[*][B]ezafk.reload[/B]: Allows using `/afk reload`.
[*][B]ezafk.gui[/B]: Allows opening the overview GUI.
[*][B]ezafk.gui.view-active[/B]: Allows viewing active (non-AFK) players in the GUI.
[*][B]ezafk.gui.actions[/B]: Allows using the player action buttons in the GUI.
[*][B]ezafk.toggle[/B]: Allows toggling other players' AFK state.
[*][B]ezafk.bypass[/B]: Lets a player ignore the automatic AFK trigger when bypass checking is enabled.
[*][B]ezafk.bypass.manage[/B]: Allows toggling bypass mode for other players via `/afk bypass`.
[*][B]ezafk.economy.bypass[/B]: Exempts a player from economy charges when AFK costs are enabled.
[*][B]ezafk.info[/B]: Allows viewing AFK reports for other players via `/afk info`.
[*][B]ezafk.time[/B]: Allows checking your own AFK total.
[*][B]ezafk.time.others[/B]: Allows checking someone else's AFK total with `/afk time <player>`.
[*][B]ezafk.top[/B]: Allows viewing the AFK time leaderboard.
[/LIST]

[SIZE=5][B]GUI overview[/B][/SIZE]
The GUI is available with the command [U]/afk gui[/U], the permission [U]ezafk.gui[/U], or OP status. Default buttons let staff kick, alert, or teleport to AFK players, and you can add extra items that run console commands with `%player%` and `%executor%` placeholders. Configure the layout in `gui.yml`.
[IMG]https://i.ibb.co/Hx8BwCj/ezafk-gui.png[/IMG]

[SIZE=5][B]Customizable admin actions[/B][/SIZE]
Beyond the preconfigured buttons, EzAfk lets you build your own action items directly in `gui.yml`. Each slot can execute one or more console or player commands, display custom icons, and include hover descriptions so staff understand what the action does. Combine placeholders such as `%player%`, `%executor%`, or `%world%` with permission checks to craft targeted moderation workflows—anything from warning messages to teleport chains or integrations with external moderation plugins.

[SIZE=5][B]Integrations[/B][/SIZE]

[B]Tab list styling (built-in)[/B]
Enable the `afk.tab-prefix.enabled` setting to display a custom prefix or suffix whenever a player is marked AFK. Customize the prefix, suffix, and final format using placeholders like `%prefix%`, `%player%`, and `%suffix%`. EzAfk can either rely on the TAB plugin for formatting or use its own built-in implementation - choose your preferred behaviour with `afk.tab-prefix.mode` (options: `auto`, `tab`, or `custom`).

[IMG]https://i.ibb.co/nD4dbQj/afk-tab.png[/IMG]

[B]WorldGuard (> v1.2)[/B]
Enable the integration in `config.yml` to unlock the custom `afk-bypass` flag, allowing specific regions where players can idle without triggering punishments or economy charges.
[URL]https://dev.bukkit.org/projects/worldguard[/URL]

[I]Flag name[/I]: afk-bypass

[U]How to add the flag to your region?[/U]
[code]
/rg flag <region> afk-bypass allow
[/code]

[B]MySQL storage (> v1.3)[/B]
Store AFK player state in a central database. EzAfk automatically handles inserts, updates, and cleanup based on player UUIDs.

[B]Metrics & updates[/B]
Anonymous usage statistics are collected via bStats, and the plugin optionally checks SpigotMC for updates during startup. Both features can be disabled through `config.yml`.

[B]PlaceholderAPI (> v1.7)[/B]
Install [URL='https://www.spigotmc.org/resources/placeholderapi.6245/']PlaceholderAPI[/URL] to expose EzAfk's placeholders. The expansion registers itself automatically when the plugin is detected, so no extra permissions or config toggles are required.

[I]Provided placeholders[/I]
[LIST]
[*][ICODE]%ezafk_status%[/ICODE] — Returns `AFK` or `ACTIVE` for the targeted player.
[*][ICODE]%ezafk_status_colored%[/ICODE] — Returns the color-formatted status string (e.g., `&cAFK`).
[*][ICODE]%ezafk_since%[/ICODE] — Seconds since the player was marked AFK. Empty when they are active.
[*][ICODE]%ezafk_last_active%[/ICODE] — Seconds since the player last moved. Always available.
[*][ICODE]%ezafk_prefix%[/ICODE] — The configured AFK prefix applied to their display name while AFK.
[*][ICODE]%ezafk_suffix%[/ICODE] — The configured AFK suffix applied to their display name while AFK.
[*][ICODE]%ezafk_afk_count%[/ICODE] — Total number of players currently marked as AFK.
[*][ICODE]%ezafk_active_count%[/ICODE] — Total number of online players not marked as AFK.
[/LIST]

[I]Usage example[/I]
[code]
&7Status: %ezafk_status_colored%
&7AFK for: %ezafk_since%s
[/code]

[SIZE=5][B]Configuration[/B][/SIZE]
EzAfk ships with dedicated files to keep settings organized:
[LIST]
[*][B]config.yml[/B]: Core AFK behaviour, broadcasts, display-name styling, TAB integration, and punishment logic.
[*][B]gui.yml[/B]: Inventory size and per-slot actions for the staff GUI.
[*][B]mysql.yml[/B]: Connection details for optional persistent storage.
[*][B]messages_*.yml[/B]: Localised player-facing messages for English, Spanish, Dutch, Russian, and Chinese out of the box.
[/LIST]

[spoiler=Core config (config.yml)]
[code=YAML]
# Config for EzAfk 2.0.0
# GUI settings have moved to gui.yml.
# MySQL settings have moved to mysql.yml.
messages:
  # Default language for the generated messages file. Supported values: en, es, nl, ru, zh
  # Language files are located in the messages/ directory.
  language: en
afk:
  # Timeout in seconds
  timeout: 300
  # Enabling this will activate the function of the permission "ezafk.bypass"
  # By default OP players have this permission.
  bypass:
    enabled: true
  broadcast:
    # Enable broadcast message to all online players when player is AFK
    enabled: true
    # Placeholders:
    # %player% - Display name of player
    # %afk_count% - Current number of players marked as AFK
    # %active_count% - Current number of online players not marked as AFK
  title:
    enabled: true
  hide-screen:
    # Apply a blindness effect to AFK players to hide their screen until they return.
    enabled: false
  animation:
    enabled: true
  storage:
    # Interval in seconds between asynchronous flushes of AFK time data to disk.
    # Lower values write more frequently while higher values reduce disk activity.
    flush-interval-seconds: 30
  # Prevent players from bypassing AFK
  anti:
    # Toggle the infinite water flow protection (config path: afk.anti.infinite-waterflow)
    infinite-waterflow: false
    infinite-vehicle: false
    # When true, bypass attempts silently mark the player as AFK instead of alerting them.
    flag-only: false
  # TAB integration
  # Requires: https://www.spigotmc.org/resources/tab-1-5-1-21.57806/
  tab-prefix:
    enabled: false
    # Strategy for applying AFK names when the TAB plugin is installed.
    # Options:
    #   auto   - Use TAB when available, otherwise fall back to EzAfk's custom list handling.
    #   tab    - Require TAB for name changes. Falls back to the custom implementation if the plugin is missing.
    #   custom - Always use EzAfk's built-in implementation, even if TAB is installed.
    mode: auto
    prefix: "&7[AFK] "
    # Text to append to the end of the player's name while AFK.
    suffix: ""
    # Full format for the displayed name. Available placeholders: %prefix%, %player%, %suffix%
    format: "%prefix%%player%%suffix%"
  # Change the in-game display name (e.g., chat) while the player is AFK.
  display-name:
    enabled: false
    prefix: "&7[AFK] "
    suffix: ""
    format: "%prefix%%player%%suffix%"
kick:
  # Enable the kick function after being AFK for x amount of time
  enabled: false
  # Enable the kick function when the lobby is full
  enabledWhenFull: false
  # Timeout in seconds
  timeout: 600
  warnings:
    enabled: true
    # List of warning intervals (in seconds before kick) to send warnings at.
    # Example: [60, 30, 10] will warn at 60s, 30s, and 10s before kick.
    intervals: [60, 30, 10]
    # How to send warnings: 'chat', 'title', or 'both'.
    # - chat: Sends a chat message only.
    # - title: Sends a title/subtitle only.
    # - both: Sends both chat and title/subtitle.
    mode: both
    # Messages are configurable in messages.yml under kick.warning
    # Warnings are only sent once per interval per AFK session.
    # If a player returns from AFK or is kicked, the warning state resets.
unafk:
  broadcast:
    # Broadcast a message when a player is not longer AFK
    enabled: true
    # Placeholders:
    # %player% - Display name of player
    # %afk_count% - Current number of players marked as AFK
    # %active_count% - Current number of online players not marked as AFK
  title:
    enabled: true
  animation:
    enabled: true
economy:
  # Enable economy-based costs for going AFK. Requires Vault with an economy provider installed.
  enabled: false
    # Recommended: [URL=https://www.spigotmc.org/resources/1-21-ezeconomy-modern-vault-economy-plugin-for-minecraft-servers.130975/][B]EzEconomy[/B][/URL] for seamless Vault integration.
  # Players with this permission bypass all AFK costs.
  bypass-permission: "ezafk.economy.bypass"
  # Players inside a WorldGuard region with the afk-bypass flag will also skip costs.
  cost:
    enter:
      # Charge a player when they are marked as AFK (either manually or automatically).
      enabled: true
      amount: 25.0
      # When true, the AFK toggle is cancelled if the player cannot afford the cost.
      # When false, the charge is skipped and the player still becomes AFK.
      require-funds: true
      # Seconds to wait before attempting to automatically toggle a player AFK again after a failed charge.
      retry-delay: 60
    recurring:
      # Continuously charge AFK players while they remain AFK.
      enabled: false
      amount: 5.0
      # Time between recurring charges in seconds.
      interval: 300
      # When true, the player must have enough funds for the charge to succeed.
      # Failing to pay while this is true always removes their AFK state.
      require-funds: true
      # Controls optional charges (when require-funds is false): true removes the AFK state on failure, false retries later.
      kick-on-fail: false
kick:
  # Enable the kick function after being AFK for x amount of time
  enabled: false
  # Enable the kick function when the lobby is full
  enabledWhenFull: false
  # Timeout in seconds
  timeout: 600
integration:
  # https://dev.bukkit.org/projects/worldguard
  # Automatically skipped when WorldGuard is not installed.
  worldguard: true
  # https://www.spigotmc.org/resources/tab-1-5-1-21.57806/
  tab: true
  # Check for EzAfk updates while startup server
  # This is being done async and will not affect the startup time of your server
  spigot:
    check-for-update: true
[/code]
[/spoiler]

[spoiler=GUI actions (gui.yml)]
[code=YAML]
inventory-size: 9
actions:
  kick:
    slot: 0
    material: IRON_BOOTS
    display-name: "&cKick Player"
    type: KICK
    target-message: "&cYou were kicked for being AFK too long."
    feedback-message: "&aSuccessfully kicked %player%"
  alert:
    slot: 1
    material: PAPER
    display-name: "&eSend Alert"
    type: MESSAGE
    target-message: "&eYou are marked as AFK. Keep active to prevent getting kicked!"
    feedback-message: "&aSent alert to %player%"
  teleport:
    slot: 2
    material: COMPASS
    display-name: "&aTeleport to Player"
    type: TELEPORT
    feedback-message: "&aTeleported to %player%"
[/code]
[/spoiler]

[spoiler=Database (mysql.yml)]
[code=YAML]
enabled: false
host: "localhost"
port: 3306
database: "ezafk"
username: "root"
password: ""
[/code]
[/spoiler]

[SIZE=5][B]Multiple language support[/B][/SIZE]
Every alert, warning, or confirmation shown to players can be tailored in the language-specific files under `messages/`. EzAfk bundles fully translated packs for English, Spanish, Dutch, Russian, and Simplified Chinese, and automatically falls back to English if a language is missing.

Set `messages.language` in `config.yml` to match one of the bundled codes (`en`, `es`, `nl`, `ru`, or `zh`) and the plugin will copy the corresponding file on first launch. Want to localise EzAfk for your own community? Copy one of the provided files, translate the values, drop it back into the `messages/` folder, and point `messages.language` at your new filename (for example, `messages_fr`).

Messages include everything from AFK toggle confirmations and bypass notifications to GUI errors, blindness prompts, and tab-prefix text—making it easy to deliver a consistent experience in your players' preferred language.

[SIZE=5][B]Support[/B][/SIZE]
For support, suggestions, or bug reports, join our [URL='https://discord.gg/yWP95XfmBS']Discord server[/URL] or visit the support thread on SpigotMC.org.

Keep your server active and free from idle players with EzAfk! Download now and take control of AFK players on your server.

[IMG]https://bstats.org/signatures/bukkit/ezafk.svg[/IMG]

---

# Markdown Variant

```
███████╗███████╗░█████╗░███████╗██╗░░██╗
██╔════╝╚════██║██╔══██╗██╔════╝██║░██╔╝
█████╗░░░░███╔═╝███████║█████╗░░█████═╝░
██╔══╝░░██╔══╝░░██╔══██║██╔══╝░░██╔═██╗░
███████╗███████╗██║░░██║██║░░░░░██║░╚██╗
╚══════╝╚══════╝╚═╝░░╚═╝╚═╝░░░░░╚═╝░░╚═╝
```

## **Keep AFK management simple**

**EzAfk** is a modern, lightweight AFK management plugin that keeps your staff informed while gently nudging idle players back into the action. Built for Paper and Spigot servers (1.7 – 1.21.*), it automates AFK detection, provides configurable staff tools, and integrates with the systems you already use — all without sacrificing performance.

![Latest version](https://img.shields.io/badge/Latest%20version-2.0.0-blue)
![Minecraft version](https://img.shields.io/badge/Minecraft%20version-1.7%20to%201.21.*-blue)

---

### **Feature highlights**

- **Automatic AFK detection**: Detect idle players after a configurable timeout, send chat or title messages, play optional animations, apply blindness, and broadcast status changes server-wide.
- **Staff workflow tools**: Open the AFK player overview GUI and trigger customizable actions (kick, alert, teleport, run console commands) per-player from an intuitive menu.
- **Anti-bypass protections**: Block common AFK bypass tricks with vehicle and water-flow checks, optionally gated behind the `ezafk.bypass` permission or WorldGuard regions.
- **Automatic punishments**: Kick AFK players after configurable grace periods or only when the server is full, with personalized kick reasons.
- **AFK kick warnings**: Send configurable chat and/or title warnings at multiple intervals before a player is kicked for being AFK, giving them a chance to return.
- **Economy-aware AFK**: Optionally charge players using Vault-supported economies when they go or stay AFK, with recurring billing plus bypass permissions or WorldGuard regions.
- **[EzEconomy integration](https://www.spigotmc.org/resources/1-21-ezeconomy-modern-vault-economy-plugin-for-minecraft-servers.130975/)**: For best results, pair EzAfk with **EzEconomy** for modern, reliable Vault economy support!
- **Integrations that matter**: Use the WorldGuard `afk-bypass` flag, track usage with bStats, receive console reminders when new releases are available, and surface AFK prefixes in the tab list without any external dependencies.
- **Custom display names & tab styling**: Mirror AFK status in chat and name tags with configurable prefixes, suffixes, and formats that work with TAB or the built-in formatter.
- **Persistent storage**: Optionally connect to MySQL to store the last active timestamp for every tracked player across restarts, plus per-player YAML totals for the AFK time leaderboard.
- **AFK analytics**: Surface `/afk time`, `/afk info`, and `/afk top` so staff can investigate reports and highlight the most idle players with a cached leaderboard.
- **Player-friendly deterrents**: Combine optional blindness and configurable animations to nudge players back to activity without being heavy-handed.
- **Built-in translations**: Ship ready-to-use English, Spanish, Dutch, Russian, and Chinese message packs with automatic fallbacks and per-server overrides.

---

### **Commands**

- **/afk**: Toggle your own AFK state (alias of `/ezafk`).
- **/afk reload**: Reload the configuration and refresh integrations.
- **/afk gui**: Open the AFK player overview GUI (includes pagination and quick actions).
- **/afk toggle <player>**: Force another player's AFK state.
- **/afk bypass <player>**: Toggle the bypass flag for a player when `afk.bypass.enabled` is active.
- **/afk info <player>**: Review why a player was flagged AFK, how long they've been idle, and their last activity.
- **/afk time [player]**: Show lifetime AFK totals for yourself or, with permission, another player.
- **/afk top**: Display the cached AFK time leaderboard (also available via `/afktop`).

*Aliases*: `/ezafk`, `/afk`, `/ea`, `/afktime`, `/afktop`

---

### **Permissions**

- **ezafk.reload**: Allows using `/afk reload`.
- **ezafk.gui**: Allows opening the overview GUI.
- **ezafk.gui.view-active**: Allows viewing active (non-AFK) players in the GUI.
- **ezafk.gui.actions**: Allows using the player action buttons in the GUI.
- **ezafk.toggle**: Allows toggling other players' AFK state.
- **ezafk.bypass**: Lets a player ignore the automatic AFK trigger when bypass checking is enabled.
- **ezafk.bypass.manage**: Allows toggling bypass mode for other players via `/afk bypass`.
- **ezafk.economy.bypass**: Exempts a player from economy charges when AFK costs are enabled.
- **ezafk.info**: Allows viewing AFK reports for other players via `/afk info`.
- **ezafk.time**: Allows checking your own AFK total.
- **ezafk.time.others**: Allows checking someone else's AFK total with `/afk time <player>`.
- **ezafk.top**: Allows viewing the AFK time leaderboard.

---

### **GUI overview**

The GUI is available with the command `/afk gui`, the permission `ezafk.gui`, or OP status. Default buttons let staff kick, alert, or teleport to AFK players, and you can add extra items that run console commands with `%player%` and `%executor%` placeholders. Configure the layout in `gui.yml`.

![ezafk-gui](https://i.ibb.co/Hx8BwCj/ezafk-gui.png)

---

### **Customizable admin actions**

Beyond the preconfigured buttons, EzAfk lets you build your own action items directly in `gui.yml`. Each slot can execute one or more console or player commands, display custom icons, and include hover descriptions so staff understand what the action does. Combine placeholders such as `%player%`, `%executor%`, or `%world%` with permission checks to craft targeted moderation workflows—anything from warning messages to teleport chains or integrations with external moderation plugins.

---

### **Integrations**

#### **Tab list styling (built-in)**

Enable the `afk.tab-prefix.enabled` setting to display a custom prefix or suffix whenever a player is marked AFK. Customize the prefix, suffix, and final format using placeholders like `%prefix%`, `%player%`, and `%suffix%`. EzAfk can either rely on the TAB plugin for formatting or use its own built-in implementation - choose your preferred behaviour with `afk.tab-prefix.mode` (options: `auto`, `tab`, or `custom`).

![afk-tab](https://i.ibb.co/nD4dbQj/afk-tab.png)

#### **WorldGuard (> v1.2)**

Enable the integration in `config.yml` to unlock the custom `afk-bypass` flag, allowing specific regions where players can idle without triggering punishments or economy charges.  
[WorldGuard on BukkitDev](https://dev.bukkit.org/projects/worldguard)

**Flag name**: `afk-bypass`

**How to add the flag to your region?**
```shell
/rg flag <region> afk-bypass allow
```

#### **MySQL storage (> v1.3)**

Store AFK player state in a central database. EzAfk automatically handles inserts, updates, and cleanup based on player UUIDs.

#### **Metrics & updates**

Anonymous usage statistics are collected via bStats, and the plugin optionally checks SpigotMC for updates during startup. Both features can be disabled through `config.yml`.

#### **PlaceholderAPI (> v1.7)**

Install [PlaceholderAPI](https://www.spigotmc.org/resources/placeholderapi.6245/) to expose EzAfk's placeholders. The expansion registers itself automatically when the plugin is detected, so no extra permissions or config toggles are required.

**Provided placeholders:**
- `%ezafk_status%` — Returns `AFK` or `ACTIVE` for the targeted player.
- `%ezafk_status_colored%` — Returns the color-formatted status string (e.g., `&cAFK`).
- `%ezafk_since%` — Seconds since the player was marked AFK. Empty when they are active.
- `%ezafk_last_active%` — Seconds since the player last moved. Always available.
- `%ezafk_prefix%` — The configured AFK prefix applied to their display name while AFK.
- `%ezafk_suffix%` — The configured AFK suffix applied to their display name while AFK.
- `%ezafk_afk_count%` — Total number of players currently marked as AFK.
- `%ezafk_active_count%` — Total number of online players not marked as AFK.

**Usage example:**
```yaml
&7Status: %ezafk_status_colored%
&7AFK for: %ezafk_since%s
```

---

### **Configuration**

EzAfk ships with dedicated files to keep settings organized:

- **config.yml**: Core AFK behaviour, broadcasts, display-name styling, TAB integration, and punishment logic.
- **gui.yml**: Inventory size and per-slot actions for the staff GUI.
- **mysql.yml**: Connection details for optional persistent storage.
- **messages_*.yml**: Localised player-facing messages for English, Spanish, Dutch, Russian, and Chinese out of the box.

<details>
<summary>Core config (<code>config.yml</code>)</summary>

```yaml
# Config for EzAfk 2.0.0
# GUI settings have moved to gui.yml.
# MySQL settings have moved to mysql.yml.
messages:
  language: en
afk:
  timeout: 300
  bypass:
    enabled: true
  broadcast:
    enabled: true
  title:
    enabled: true
  hide-screen:
    enabled: false
  animation:
    enabled: true
  storage:
    flush-interval-seconds: 30
  anti:
    infinite-waterflow: false
    infinite-vehicle: false
    flag-only: false
  tab-prefix:
    enabled: false
    mode: auto
    prefix: "&7[AFK] "
    suffix: ""
    format: "%prefix%%player%%suffix%"
  display-name:
    enabled: false
    prefix: "&7[AFK] "
    suffix: ""
    format: "%prefix%%player%%suffix%"
kick:
  enabled: false
  enabledWhenFull: false
  timeout: 600
  warnings:
    enabled: true
    intervals: [60, 30, 10]
    mode: both
unafk:
  broadcast:
    enabled: true
  title:
    enabled: true
  animation:
    enabled: true
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
integration:
  worldguard: true
  tab: true
  spigot:
    check-for-update: true
```
</details>

<details>
<summary>GUI actions (<code>gui.yml</code>)</summary>

```yaml
inventory-size: 9
actions:
  kick:
    slot: 0
    material: IRON_BOOTS
    display-name: "&cKick Player"
    type: KICK
    target-message: "&cYou were kicked for being AFK too long."
    feedback-message: "&aSuccessfully kicked %player%"
  alert:
    slot: 1
    material: PAPER
    display-name: "&eSend Alert"
    type: MESSAGE
    target-message: "&eYou are marked as AFK. Keep active to prevent getting kicked!"
    feedback-message: "&aSent alert to %player%"
  teleport:
    slot: 2
    material: COMPASS
    display-name: "&aTeleport to Player"
    type: TELEPORT
    feedback-message: "&aTeleported to %player%"
```
</details>

<details>
<summary>Database (<code>mysql.yml</code>)</summary>

```yaml
enabled: false
host: "localhost"
port: 3306
database: "ezafk"
username: "root"
password: ""
```
</details>

---

### **Multiple language support**

Every alert, warning, or confirmation shown to players can be tailored in the language-specific files under `messages/`. EzAfk bundles fully translated packs for English, Spanish, Dutch, Russian, and Simplified Chinese, and automatically falls back to English if a language is missing.

Set `messages.language` in `config.yml` to match one of the bundled codes (`en`, `es`, `nl`, `ru`, or `zh`) and the plugin will copy the corresponding file on first launch. Want to localise EzAfk for your own community? Copy one of the provided files, translate the values, drop it back into the `messages/` folder, and point `messages.language` at your new filename (for example, `messages_fr`).

Messages include everything from AFK toggle confirmations and bypass notifications to GUI errors, blindness prompts, and tab-prefix text—making it easy to deliver a consistent experience in your players' preferred language.

---

### **Support**

For support, suggestions, or bug reports, join our [Discord server](https://discord.gg/yWP95XfmBS) or visit the support thread on SpigotMC.org.

Keep your server active and free from idle players with EzAfk! Download now and take control of AFK players on your server.

![bStats](https://bstats.org/signatures/bukkit/ezafk.svg)