package com.mengcraft.playersql;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;


/**
 * Created on 16-7-25.
 */
public class ExtendEventExecutor implements Listener {

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void handle(PlayerInteractAtEntityEvent event) {
        if (UserManager.isLocked(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void pre(PlayerInteractAtEntityEvent event) {
        handle(event);
    }
}
