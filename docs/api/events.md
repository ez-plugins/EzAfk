# EzAfk Custom Events

EzAfk exposes custom events to allow other plugins to hook into AFK status changes and extend functionality.

---

## PlayerAfkStatusChangeEvent

- **Package:** `com.gyvex.ezafk.event`
- **Fired:** When a player goes AFK or returns from AFK.
- **Cancellable:** Yes. Plugins can cancel AFK status changes.
- **Fields:**
  - `Player getPlayer()` — The player whose status changed
  - `boolean isAfk()` — `true` if now AFK, `false` if returned
  - `AfkReason getReason()` — Reason for AFK status change (see [AfkReasons](./AfkReasons.md))
  - `String getDetail()` — Additional details about the change

### Example Listener
```java
import com.gyvex.ezafk.event.PlayerAfkStatusChangeEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class MyAfkListener implements Listener {
    @EventHandler
    public void onAfkStatusChange(PlayerAfkStatusChangeEvent event) {
        if (event.isAfk()) {
            // Player went AFK
            String reason = event.getReason() != null ? event.getReason().name() : "UNKNOWN";
            String detail = event.getDetail();
        } else {
            // Player returned from AFK
        }

        // Example: Cancel AFK status change
        // if (shouldBlockAfk(event.getPlayer())) {
        //     event.setCancelled(true);
        // }
    }
}
```

Register your listener as usual in your plugin to receive these events.