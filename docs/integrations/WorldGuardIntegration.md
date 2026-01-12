# WorldGuardIntegration

## Features
- Registers a custom WorldGuard region flag: `afk-bypass`.
- Allows regions to permit or block AFK detection for players within those regions.
- Automatically detects WorldGuard and enables integration if configured.
- Uses the `afk-bypass` flag to let players in specific regions avoid AFK detection.
- Logs integration status and flag registration for server admins.

## Setup & Usage
1. Install the WorldGuard plugin on your server.
2. Ensure `integration.worldguard` is enabled in your EzAfk configuration (`config.yml`).
3. EzAfk will automatically detect WorldGuard and register the `afk-bypass` flag.
4. To allow players to bypass AFK detection in a region:
	 - Add the `afk-bypass` flag to the desired region and set it to `ALLOW`.
	 - Example command:
		 ```
		 /region flag <region> afk-bypass allow
		 ```
5. Players inside regions with `afk-bypass: allow` will not be marked as AFK by EzAfk.
6. Server logs will indicate if the flag was registered or if an existing flag is used.
7. No further setup is required; integration works automatically once enabled.
