package com.mengcraft.playersql;

import com.mengcraft.playersql.event.PlayerDataFetchedEvent;
import com.mengcraft.playersql.event.PlayerDataProcessedEvent;
import com.mengcraft.playersql.event.PlayerDataStoreEvent;
import com.mengcraft.playersql.storage.StorageManager;
import com.mengcraft.playersql.task.DailySaveTask;
import lombok.SneakyThrows;
import lombok.val;
import net.sakuragame.serversystems.manage.client.api.ClientManagerAPI;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.json.simple.JSONArray;
import org.json.simple.JSONValue;

import java.util.*;

import static com.mengcraft.playersql.PluginMain.nil;

/**
 * Created on 16-1-2.
 */
public enum UserManager {

    INSTANCE;

    public static final ItemStack AIR = new ItemStack(Material.AIR);

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
    public PlayerData fetchUser(UUID uuid) {
        return storageManager.find(uuid);
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
        PlayerData user = new PlayerData();
        user.setUid(uid);

        if (closeInventory) {
            closeInventory(p);
        }
        user.setInventory(toString(p.getInventory().getContents()));
        user.setArmor(toString(p.getInventory().getArmorContents()));
        user.setHand(p.getInventory().getHeldItemSlot());

        PlayerDataStoreEvent.call(p, user);
        return user;
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
        val ctx = toStack(who, data.getInventory());
        who.closeInventory();
        val inv = who.getInventory();
        if (ctx.length > inv.getSize()) {// Fixed #36
            int size = inv.getSize();
            inv.setContents(Arrays.copyOf(ctx, size));
            val out = inv.addItem(Arrays.copyOfRange(ctx, size, ctx.length));
            if (!out.isEmpty()) {
                val location = who.getLocation();
                out.forEach((o, item) -> who.getWorld().dropItem(location, item));
            }
        } else {
            inv.setContents(ctx);
        }
        inv.setArmorContents(toStack(who, data.getArmor()));
        inv.setHeldItemSlot(data.getHand());
        who.updateInventory();// Force update needed

        createTask(who);
        unlockUser(who);
    }

    @SuppressWarnings("unchecked")
    private List<PotionEffect> toEffect(String input) {
        List<List> parsed = parseArray(input);
        List<PotionEffect> output = new ArrayList<>(parsed.size());
        for (List<Number> entry : parsed) {
            output.add(new PotionEffect(PotionEffectType.getById(entry.get(0).intValue()), entry.get(1).intValue(), entry.get(2).intValue()));
        }
        return output;
    }

    public static JSONArray parseArray(String in) {
        if (!nil(in)) {
            Object parsed = JSONValue.parse(in);
            if (parsed instanceof JSONArray) {
                return ((JSONArray) parsed);
            }
        }
        return new JSONArray();
    }

    @SuppressWarnings("unchecked")
    @SneakyThrows
    public ItemStack[] toStack(Player player, String input) {
        List<String> list = parseArray(input);
        List<ItemStack> output = new ArrayList<>(list.size());
        for (String line : list) {
            if (nil(line) || line.isEmpty()) {
                output.add(AIR);
            } else {
                output.add(DataSerializer.deserialize(player, line));
            }
        }
        return output.toArray(new ItemStack[list.size()]);
    }

    @SuppressWarnings("unchecked")
    public String toString(ItemStack[] stacks) {
        JSONArray array = new JSONArray();
        for (ItemStack stack : stacks)
            if (stack == null || stack.getAmount() == 0) {
                array.add("");
            } else try {
                array.add(DataSerializer.serialize(stack));
            } catch (Exception e) {
                main.log(e);
            }
        return array.toString();
    }

    @SuppressWarnings("unchecked")
    private String toString(Collection<PotionEffect> effects) {
        val out = new JSONArray();
        for (PotionEffect effect : effects) {
            val sub = new JSONArray();
            sub.add(effect.getType().getId());
            sub.add(effect.getDuration());
            sub.add(effect.getAmplifier());
            out.add(sub);
        }
        return out.toString();
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
        task.runTaskTimer(main, 6000, 6000);
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
