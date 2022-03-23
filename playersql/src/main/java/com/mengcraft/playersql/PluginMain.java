package com.mengcraft.playersql;

//import com.comphenix.protocol.ProtocolLibrary;

import com.github.caoli5288.playersql.bungee.Constants;
import com.mengcraft.playersql.locker.EventLocker;
import com.mengcraft.playersql.storage.StorageManager;
import lombok.Getter;
import lombok.SneakyThrows;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.logging.Level;

import static org.bukkit.Bukkit.getPluginManager;

/**
 * Created on 16-1-2.
 */
public class PluginMain extends JavaPlugin implements Executor {

    @Getter
    private static Messenger messenger;
    @Getter
    private static PluginMain plugin;
    @Getter
    private static StorageManager storageManager;
    private static boolean applyNullUserdata;

    @Override
    public void onLoad() {
        saveDefaultConfig();
        applyNullUserdata = getConfig().getBoolean("plugin.apply-null-userdata", false);
    }

    @SneakyThrows
    public void onEnable() {
        plugin = this;

        getConfig().options().copyDefaults(true);
        saveConfig();

        messenger = new Messenger(this);

        storageManager = new StorageManager(this);
        storageManager.init();

        UserManager manager = UserManager.INSTANCE;
        manager.setMain(this);
        manager.setDb(storageManager);

        EventExecutor executor = new EventExecutor(this);

        getServer().getPluginManager().registerEvents(executor, this);
        try {
            getServer().getPluginManager().registerEvents(new ExtendEventExecutor(), this);
        } catch (Exception ignore) {
        }

        getPluginManager().registerEvents(new EventLocker(), this);

        getServer().getMessenger().registerOutgoingPluginChannel(this, Constants.PLUGIN_CHANNEL);
        getServer().getMessenger().registerIncomingPluginChannel(this, Constants.PLUGIN_CHANNEL, executor);

        /*getCommand("playersql").setExecutor(new Commands());*/

        getServer().getConsoleSender().sendMessage(ChatColor.GREEN + "PlayerSQL enabled!");
    }

    @Override
    public void onDisable() {
        for (Player p : getServer().getOnlinePlayers()) {
            UserManager.INSTANCE.saveUser(p, false);
        }
    }

    public Player getPlayer(UUID uuid) {
        return getServer().getPlayer(uuid);
    }

    public void log(Exception e) {
        if (Config.DEBUG) {
            getLogger().log(Level.SEVERE, e.toString(), e);
        }
    }

    public void log(String info) {
        getLogger().info(info);
    }

    public void debug(String line) {
        if (Config.DEBUG) {
            log(line);
        }
    }

    public static void resetPlayerState(Player player) {
        player.getInventory().clear();
    }

    @Override
    public void execute(Runnable command) {
        Bukkit.getScheduler().runTask(this, command);
    }

    public static CompletableFuture<Void> runAsync(Runnable r) {
        return CompletableFuture.runAsync(r).exceptionally(thr -> {
            Bukkit.getLogger().log(Level.SEVERE, "" + thr, thr);
            return null;
        });
    }

    public void run(Runnable r) {
        getServer().getScheduler().runTask(this, r);
    }

    public static boolean isApplyNullUserdata() {
        return applyNullUserdata;
    }

    public static boolean nil(Object i) {
        return i == null;
    }

}
