# EzAfk objective

EzAfk is a Minecraft plugin to provide AFK features to Spigot / Bukkit / Paper / Purpur servers. The codebase must be clean, testable, and follow clear standards.

## High-level goals
- Maintain a single source of truth for runtime state via Registry.
- Keep startup logic in Bootstrap.
- Implement a repository pattern for storage so storage type is configurable (MySQL, SQLite, YAML, etc.).
- Keep tasks in `task/` as `BukkitRunnable` subclasses.
- Keep utility helpers in `util/`.

## Project layout (recommended)
- src/main/java/com/gyvex/ezafk/
  - bootstrap/Bootstrap.java
  - registry/Registry.java
  - command/
  - event/
  - gui/
  - integration/
  - manager/ (business logic managers)
  - repository/ (storage interfaces + implementations)
    - StorageRepository.java
    - mysql/ MySQLStorage.java
    - sqlite/ SQLiteStorage.java
    - yaml/ YamlStorage.java
    - StorageFactory.java
  - task/ (one class per task; extends BukkitRunnable)
  - util/
  - config/ (config parsing helpers)

## Code standards / rules
- Target Java 17 (match modern Minecraft server JVM).
- Prefer immutability and final fields where possible.
- Use clear package-private/public boundaries; avoid broad statics.
- All runtime singletons live in Registry with typed getters:
  - Registry.get().getBootstrap()
  - Registry.get().getTaskManager()
  - Registry.get().getStorageRepository()
  - Registry.get().getConfigManager()
  - Registry.get().getIntegrationManager()
- Keep Bootstrap responsible for registering listeners and starting tasks only.
- Avoid direct Bukkit calls in business logic; pass required services via manager constructors when possible.
- Use plugin logger through Registry: Registry.get().getLogger().
