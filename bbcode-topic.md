███████╗███████╗░█████╗░███████╗██╗░░██╗
██╔════╝╚════██║██╔══██╗██╔════╝██║░██╔╝
█████╗░░░░███╔═╝███████║█████╗░░█████═╝░
██╔══╝░░██╔══╝░░██╔══██║██╔══╝░░██╔═██╗░
███████╗███████╗██║░░██║██║░░░░░██║░╚██╗
╚══════╝╚══════╝╚═╝░░╚═╝╚═╝░░░░░╚═╝░░╚═╝

[B][SIZE=6]Keep AFK management simple[/SIZE][/B]

[B]EzAfk[/B] is a modern, lightweight AFK management plugin built for Paper and Spigot servers running Minecraft 26.1+ and Java 25. It automates AFK detection, rewards or charges players based on AFK state, provides staff overview tools, and integrates with the systems you already use, all without sacrificing performance.

[IMG]https://img.shields.io/badge/version-3.0.0-blue[/IMG]
[IMG]https://img.shields.io/badge/Minecraft-26.1-green[/IMG]
[IMG]https://img.shields.io/badge/Java-25-orange[/IMG]

Download on [URL='https://modrinth.com/plugin/ezafk'][B]Modrinth[/B][/URL] · Found an issue or have a question? Join [URL='https://discord.gg/yWP95XfmBS']the EzPlugins Discord server[/URL].

[IMG]https://i.ibb.co/ch6q5J0X/image.png[/IMG]

[SIZE=5][B]Feature highlights[/B][/SIZE]
[LIST]
[*][B]Automatic AFK detection[/B]: Detect idle players after a configurable timeout (default 5 min). Send chat or title messages, trigger animations, apply a blindness blur, and broadcast status changes server-wide.
[*][B]Anti-bypass protections[/B]: Block common AFK farm tricks including infinite water flow, vehicle riding, and bubble columns, with individual toggle switches. Combine with the `ezafk.bypass` permission or WorldGuard regions for fine-grained control.
[*][B]AFK kick with warnings[/B]: Kick players after a configurable idle period (default 10 min). Send multi-stage chat and/or title warnings at custom intervals (e.g. 60 s, 30 s, 10 s) before the kick fires. Optionally kick only when the server is full to free up slots.
[*][B]In-game staff GUI[/B]: Open `/afk gui` to see all AFK players at a glance. One-click buttons let staff kick, message, teleport to, or run console commands against any AFK player. Fully configurable layout in `gui.yml`.
[*][B]AFK Zones with rewards[/B]: Define coordinate-based cuboid regions where players earn rewards for being AFK. Reward types: economy currency (Vault), console commands, or item drops. Each zone has its own interval and reward cap.
[*][B]Economy-aware AFK[/B]: Charge players a one-time fee when going AFK and/or a recurring fee while staying AFK. Requires Vault. Block AFK if insufficient funds, or kick when recurring funds run out.
[*][B]AFK analytics & leaderboard[/B]: Track cumulative AFK time per player. Use `/afk time`, `/afk info`, and `/afk top` for detailed reports and server-wide leaderboards. Data persists across restarts.
[*][B]Custom display names & tab styling[/B]: Mirror AFK status in chat, name tags, and the tab list with configurable prefixes, suffixes, and formats. Works with the TAB plugin or EzAfk's built-in formatter.
[*][B]Persistent storage[/B]: Store AFK data in YAML (default), SQLite, or MySQL. The storage backend is swappable without data loss.
[*][B]Multi-language support[/B]: Ships with English, Spanish, Dutch, Russian, Chinese, and German message packs. Override any message per-server without touching the source.
[*][B]Simple Voice Chat integration[/B]: Play a custom MP3 sound to players when they go AFK, via the Simple Voice Chat mod API.
[*][B]PlaceholderAPI support[/B]: Expose 16 AFK placeholders to any PAPI-compatible plugin: status, session length, total time, prefix/suffix, player counts, and more.
[/LIST]

[SIZE=5][B]Commands[/B][/SIZE]
[LIST]
[*][B]/afk[/B]: Toggle your own AFK status.
[*][B]/afk reload[/B]: Reload all configuration files.
[*][B]/afk gui[/B]: Open the AFK player overview GUI.
[*][B]/afk toggle <player>[/B]: Force another player's AFK state.
[*][B]/afk bypass <player>[/B]: Toggle the AFK bypass flag for a player.
[*][B]/afk info <player>[/B]: View a player's current AFK state, idle reason, and session info.
[*][B]/afk time [player][/B]: View total AFK time for yourself or another player.
[*][B]/afk time reset <player>[/B]: Reset a player's cumulative AFK time counter.
[*][B]/afk top[/B]: Show the server-wide AFK time leaderboard.
[*][B]/afk zone pos1[/B] / [B]pos2[/B]: Select zone corners.
[*][B]/afk zone add <name>[/B]: Create a new AFK zone between the two selected corners.
[*][B]/afk zone remove <name>[/B]: Delete a zone.
[*][B]/afk zone list[/B]: List all configured zones.
[/LIST]
[I]Aliases[/I]: `/ezafk`, `/ea`, `/afktime`, `/afktop`

[SIZE=5][B]Permissions[/B][/SIZE]
[LIST]
[*][B]ezafk.reload[/B]: Reload configuration.
[*][B]ezafk.bypass[/B]: Never be marked AFK automatically.
[*][B]ezafk.bypass.manage[/B]: Toggle bypass for other players.
[*][B]ezafk.toggle[/B]: Toggle other players' AFK state.
[*][B]ezafk.info[/B]: View AFK details for other players.
[*][B]ezafk.kick.bypass[/B]: Never be kicked by EzAfk's AFK kick.
[*][B]ezafk.gui[/B]: Open the staff GUI.
[*][B]ezafk.gui.view-active[/B]: View active players in the GUI.
[*][B]ezafk.gui.actions[/B]: Use action buttons in the GUI.
[*][B]ezafk.time[/B]: View own AFK time (granted to all players by default).
[*][B]ezafk.time.others[/B]: View other players' AFK time.
[*][B]ezafk.time.reset[/B]: Reset a player's AFK time.
[*][B]ezafk.top[/B]: View the leaderboard.
[*][B]ezafk.economy.bypass[/B]: Skip economy charges.
[*][B]ezafk.zone.list[/B]: List AFK zones.
[*][B]ezafk.zone.manage[/B]: Create and remove zones.
[/LIST]

[SIZE=5][B]GUI overview[/B][/SIZE]
Open the GUI with [U]/afk gui[/U] (requires [U]ezafk.gui[/U] or OP). Default buttons let staff kick, message, or teleport to AFK players. Additional slots can run any console command with `%player%` and `%executor%` placeholders. Configure the full layout in `gui.yml`.

[IMG]https://i.ibb.co/Hx8BwCj/ezafk-gui.png[/IMG]

[SIZE=5][B]Integrations[/B][/SIZE]

[B]Tab list styling (built-in)[/B]
Set `integration.tab-prefix.enabled: true` in `config.yml` to display a custom prefix/suffix when a player is AFK. EzAfk can use its own formatter or delegate to the TAB plugin (`mode: tab`).

[IMG]https://i.ibb.co/nD4dbQj/afk-tab.png[/IMG]

[B]WorldGuard[/B]
Enable WorldGuard integration in `config.yml` to unlock the custom `afk-bypass` flag. Set it on a region to allow players to idle there without triggering kick timers or economy charges.

[code]
/rg flag <region> afk-bypass allow
[/code]

[B]PlaceholderAPI[/B]
Install [URL='https://www.spigotmc.org/resources/placeholderapi.6245/']PlaceholderAPI[/URL] and the expansion registers automatically. Full list of placeholders:

[LIST]
[*][ICODE]%ezafk_status%[/ICODE]: [ICODE]AFK[/ICODE] or [ICODE]ACTIVE[/ICODE]
[*][ICODE]%ezafk_status_colored%[/ICODE]: Colour-coded status string
[*][ICODE]%ezafk_since%[/ICODE]: Seconds since the current AFK session started
[*][ICODE]%ezafk_last_active%[/ICODE]: Seconds since last activity
[*][ICODE]%ezafk_total_seconds%[/ICODE] / [ICODE]%ezafk_total%[/ICODE] / [ICODE]%ezafk_total_formatted%[/ICODE]: Total lifetime AFK time
[*][ICODE]%ezafk_prefix%[/ICODE] / [ICODE]%ezafk_suffix%[/ICODE]: Configured AFK display-name prefix/suffix
[*][ICODE]%ezafk_playtime_active_seconds%[/ICODE] / [ICODE]%ezafk_playtime_active%[/ICODE]: Active (non-AFK) playtime
[*][ICODE]%ezafk_afk_count%[/ICODE] / [ICODE]%ezafk_active_count%[/ICODE]: Server-wide AFK / active player counts
[/LIST]

[B]Simple Voice Chat[/B]
Place an MP3 file in `plugins/EzAfk/mp3/` and set `afk.sound.enabled: true`. EzAfk plays it to the player when they go AFK (and optionally on return) using the Simple Voice Chat API.

[B]Economy / Vault[/B]
Pair with any Vault-compatible economy plugin for AFK entry costs, recurring charges, and zone rewards. Recommended: [URL='https://www.spigotmc.org/resources/1-7-1-21-ezeconomy-modern-vault-economy-plugin-for-minecraft-servers.130975/'][B]EzEconomy[/B][/URL].

[SIZE=5][B]Configuration[/B][/SIZE]
EzAfk ships with dedicated files to keep settings organised:
[LIST]
[*][B]config.yml[/B]: Core AFK behaviour, broadcasts, display-name styling, tab integration, kick, anti-bypass, economy, and integrations.
[*][B]gui.yml[/B]: Inventory size and per-slot actions for the staff GUI.
[*][B]zones.yml[/B]: AFK zone definitions with world, coordinates, and reward settings.
[*][B]mysql.yml[/B]: Connection details for optional MySQL storage.
[*][B]messages/[/B]: Per-language message files: `en`, `es`, `nl`, `ru`, `zh`, `de`.
[/LIST]

[spoiler=Core config snippet (config.yml)]
[code=YAML]
messages:
  language: en   # en | es | nl | ru | zh | de

afk:
  timeout: 300
  broadcast:
    enabled: true
  title:
    enabled: true
  hide-screen:
    enabled: false
  animation:
    enabled: true
  anti:
    infinite-waterflow: false
    infinite-vehicle: false
    bubble-column: false
    flag-only: false

kick:
  enabled: false
  enabledWhenFull: false
  timeout: 600
  warnings:
    enabled: true
    intervals: [60, 30, 10]
    mode: both   # chat | title | both

economy:
  enabled: false
  cost:
    enter:
      enabled: true
      amount: 25.0
    recurring:
      enabled: false
      amount: 5.0
      interval: 300

storage:
  type: yaml   # yaml | sqlite | mysql
  flush-interval-seconds: 30

integration:
  worldguard: true
  tab: true
  voicechat: auto   # true | false | auto
[/code]
[/spoiler]
