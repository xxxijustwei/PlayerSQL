package com.mengcraft.playersql;

import com.google.common.collect.Maps;
import com.mengcraft.playersql.internal.GuidResolveService;
import com.mengcraft.playersql.lib.CustomInventory;
import com.mengcraft.playersql.task.FetchUserTask;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;

import java.util.Map;
import java.util.UUID;

import static com.mengcraft.playersql.PluginMain.runAsync;
import static org.bukkit.event.EventPriority.MONITOR;

/**
 * Created on 16-1-2.
 */
public class EventExecutor implements Listener {

    private final Map<UUID, UserState> states = Maps.newHashMap();
    private final PluginMain main;
    private final UserManager manager = UserManager.INSTANCE;

    public EventExecutor(PluginMain main) {
        this.main = main;
    }

    private UserState ofState(UUID id) {
        return states.computeIfAbsent(id, uuid -> new UserState());
    }

    @EventHandler
    public void handle(InventoryCloseEvent e) {
        Inventory inventory = e.getInventory();
        if (CustomInventory.isInstance(inventory)) {
            CustomInventory.close(inventory);
        }
    }

    @EventHandler
    public void handle(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        manager.lockUser(player);
        UUID id = player.getUniqueId();
        UserState state = ofState(id);
        if (state.getPlayerData() == null) {
            FetchUserTask task = new FetchUserTask(player);
            state.setFetchTask(task);
            task.runTaskTimerAsynchronously(main, Config.SYN_DELAY, Config.SYN_DELAY);
        } else {
            main.debug("process pending data_buf on join event");
            UUID guid = GuidResolveService.getService().getGuid(player);
            main.run(() -> {
                manager.pend(player, state.getPlayerData());
                runAsync(() -> manager.updateDataLock(guid, true));
            });
        }
    }

    @EventHandler(priority = MONITOR)
    public void handle(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID id = player.getUniqueId();
        UserState state = states.get(id);
        if (manager.isNotLocked(id)) {
            manager.cancelTimerSaver(id);
            manager.lockUser(player);// Lock user if not in bungee enchant mode
            PlayerData data = (state != null && state.isKicking() && state.getPlayerData() != null)
                    ? state.getPlayerData()
                    : manager.getUserData(id, true);
            if (data == null) {
                main.run(() -> manager.unlockUser(player));// Err? unlock next tick
            } else {
                runAsync(() -> manager.saveUser(data, false)).thenRun(() -> main.run(() -> manager.unlockUser(player)));
            }
        } else {
        	UUID guid = GuidResolveService.getService().getGuid(player);
            runAsync(() -> manager.updateDataLock(guid, false)).thenRun(() -> main.run(() -> manager.unlockUser(player)));
        }
        // leaks check
        if (states.size() > 64 && states.size() > Bukkit.getMaxPlayers()) {
            states.keySet()
                    .removeIf(it -> Bukkit.getPlayer(it) == null);
        }
    }
}
