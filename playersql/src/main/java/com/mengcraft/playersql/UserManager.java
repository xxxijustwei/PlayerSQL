package com.mengcraft.playersql;

import com.mengcraft.playersql.event.PlayerDataFetchedEvent;
import com.mengcraft.playersql.event.PlayerDataProcessedEvent;
import com.mengcraft.playersql.event.PlayerDataStoreEvent;
import com.mengcraft.playersql.storage.StorageManager;
import com.mengcraft.playersql.task.DailySaveTask;
import lombok.val;
import net.sakuragame.serversystems.manage.client.api.ClientManagerAPI;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

import static com.mengcraft.playersql.PluginMain.nil;

/**
 * Created on 16-1-2.
 */
public enum UserManager {

    INSTANCE;

    private final Map<UUID, BukkitRunnable> scheduled = new HashMap<>();
    private final Set<UUID> locked = new HashSet<>();

    private PluginMain main;
    private StorageManager storageManager;

    public void addFetched(Player player, PlayerData user) {
        main.run(() -> pend(player, user));
    }

    /**
     * @return The user, or <code>null</code> if not exists.
     */
    public PlayerData fetchUser(Player player) {
        return storageManager.find(player);
    }

    public void saveUser(Player p, boolean lock) {
        saveUser(getUserData(p, !lock), lock);
    }

    public void saveUser(PlayerData user, boolean lock) {
        user.setLocked(lock);
        storageManager.update(user);
        if (Config.DEBUG) {
            String name = ClientManagerAPI.getUserName(user.getUid());
            main.log("Save user data " + name + " done!");
        }
    }

    public void updateDataLock(UUID who, boolean lock) {
        int result = storageManager.updateDateLock(who, lock);
        if (Config.DEBUG) {
            if (result == 1) {
                main.log("Update " + who + " lock to " + lock + " okay");
            } else {
                main.log(new IllegalStateException("Update " + who + " lock to " + lock + " failed"));
            }
        }
    }

    private void closeInventory(Player p) {
        // Try fix some duplicate item issue
        val view = p.getOpenInventory();
        if (!nil(view)) {
            val cursor = view.getCursor();
            if (!nil(cursor)) {
                view.setCursor(null);
                val d = p.getInventory().addItem(cursor);
                if (!d.isEmpty()) {
                    // Bypass to opened inventory
                    for (val item : d.values()) {
                        view.getTopInventory().addItem(item);
                    }
                }
            }
        }
    }

    public PlayerData getUserData(UUID id, boolean closeInventory) {
        val p = main.getServer().getPlayer(id);
        if (!nil(p)) {
            return getUserData(p, closeInventory);
        }
        return null;
    }

    public PlayerData getUserData(Player p, boolean closeInventory) {
        int uid = ClientManagerAPI.getUserID(p.getUniqueId());
        PlayerData account = new PlayerData();
        account.setUid(uid);

        if (closeInventory) {
            closeInventory(p);
        }
        account.setSlots(p);
        account.setHand(p.getInventory().getHeldItemSlot());

        PlayerDataStoreEvent.call(p, account);
        return account;
    }

    public static boolean isLocked(UUID uuid) {
        return INSTANCE.locked.contains(uuid);
    }

    public boolean isNotLocked(UUID uuid) {
        return !isLocked(uuid);
    }

    public void lockUser(Player player) {
        main.debug(String.format("manager.lockUser(%s)", player.getName()));
        locked.add(player.getUniqueId());
        if (main.getConfig().getBoolean("plugin.disable-filter-network")) {
            return;
        }
        Utils.setAutoRead(player, false);
    }

    public void unlockUser(Player player) {
        main.debug(String.format("manager.unlockUser(%s)", player.getName()));
        UUID uuid = player.getUniqueId();
        while (isLocked(uuid)) {
            locked.remove(uuid);
        }
        if (main.getConfig().getBoolean("plugin.disable-filter-network")) {
            return;
        }
        Utils.setAutoRead(player, true);
    }

    void onLoadFailed(Player who) {
        if (Config.KICK_LOAD_FAILED) {
            who.kickPlayer(PluginMain.getMessenger().find("kick_load", "Your game data loading error, please contact the operator"));
        } else {
            unlockUser(who);
            createTask(who);
        }
    }

    public void pend(Player who, PlayerData data) {
        if (who == null || !who.isOnline()) {
            main.log(new IllegalStateException("Player " + data.getUid() + " not found"));
            return;
        }

        if (isNotLocked(who.getUniqueId())) {
            main.debug(String.format("manager.pend cancel pend ops, player %s not locked", who.getName()));
            return;
        }

        val event = PlayerDataFetchedEvent.call(who, data);
        if (event.isCancelled()) {
            onLoadFailed(who);
        } else {
            Exception exception = null;
            try {
                syncUserdata(who, data);
            } catch (Exception e) {
                exception = e;
                onLoadFailed(who);
                if (Config.DEBUG) {
                    main.log(e);
                } else {
                    main.log(e.toString());
                }
            }

            PlayerDataProcessedEvent.call(who, exception);
        }
    }

    private void syncUserdata(Player who, PlayerData data) {
        data.getSlots().forEach((index, item) -> who.getInventory().setItem(index, item));
        who.getInventory().setHeldItemSlot(data.getHand());
        who.updateInventory();// Force update needed

        createTask(who);
        unlockUser(who);
    }

    public void cancelTimerSaver(UUID uuid) {
        val i = scheduled.remove(uuid);
        if (!nil(i)) {
            i.cancel();
        } else if (Config.DEBUG) {
            main.log("No task can be canceled for " + uuid + '!');
        }
    }

    public void createTask(Player player) {
        if (Config.DEBUG) {
            this.main.log("Scheduling daily save task for user " + player.getName() + '.');
        }
        val task = new DailySaveTask(player);
        task.runTaskTimer(main, 18000, 18000);
        val old = scheduled.put(player.getUniqueId(), task);
        if (!nil(old)) {
            old.cancel();
            if (Config.DEBUG) {
                this.main.log("Already scheduled task for user " + player.getName() + '!');
            }
        }
    }

    public void setMain(PluginMain main) {
        this.main = main;
    }

    public PluginMain getMain() {
        return main;
    }

    public void setDb(StorageManager db) {
        this.storageManager = db;
    }

    public void newUser(UUID uuid) {
        PlayerData user = new PlayerData();
        user.setUid(ClientManagerAPI.getUserID(uuid));
        user.setLocked(true);
        Player player = Bukkit.getPlayer(uuid);
        if (player == null) {
            user.setHand(0);
        }
        else {
            user.setHand(player.getInventory().getHeldItemSlot());
        }
        storageManager.save(user);
    }
}
