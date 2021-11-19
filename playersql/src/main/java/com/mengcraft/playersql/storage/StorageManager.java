package com.mengcraft.playersql.storage;

import com.mengcraft.playersql.PlayerData;
import com.mengcraft.playersql.PluginMain;
import com.mengcraft.playersql.storage.table.AccountTable;
import net.sakuragame.serversystems.manage.api.database.DataManager;
import net.sakuragame.serversystems.manage.api.database.DatabaseQuery;
import net.sakuragame.serversystems.manage.client.api.ClientManagerAPI;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.*;

public class StorageManager {

    private PluginMain plugin;
    private DataManager dataManager;

    public static final ItemStack AIR = new ItemStack(Material.AIR);

    public StorageManager(PluginMain plugin) {
        this.plugin = plugin;
        this.dataManager = ClientManagerAPI.getDataManager();
    }

    public void init() {
        for (AccountTable table : AccountTable.values()) {
            table.createTable();
        }
    }

    public PlayerData find(UUID uuid) {
        int uid = ClientManagerAPI.getUserID(uuid);
        if (uid == -1) return null;

        try (DatabaseQuery query = dataManager.createQuery(
                AccountTable.PLAYER_SQL_DATE.getTableName(),
                "uid", uid
        )) {
            ResultSet result = query.getResultSet();
            if (result.next()) {
                int hand = result.getInt("hand");
                String inventory = result.getString("inventory");
                String armor = result.getString("armor");
                boolean locked = result.getBoolean("locked");
                Timestamp timestamp = result.getTimestamp("timestamp");

                return new PlayerData(uid, hand, inventory, armor, locked, timestamp);
            }
        }
        catch (SQLException e ) {
            e.printStackTrace();
        }

        return null;
    }

    public void update(PlayerData data) {
        dataManager.executeUpdate(
                AccountTable.PLAYER_SQL_DATE.getTableName(),
                new String[] {"hand", "inventory", "armor", "locked"},
                new Object[] {
                        data.getHand(),
                        data.getInventory(),
                        data.getArmor(),
                        data.isLocked()
                },
                new String[] {"uid"},
                new Object[] {data.getUid()}
        );
    }

    public void save(PlayerData data) {
        dataManager.executeReplace(
                AccountTable.PLAYER_SQL_DATE.getTableName(),
                new String[] {"uid", "hand", "inventory", "armor", "locked", "lastUpdate"},
                new Object[] {
                        data.getUid(),
                        data.getHand(),
                        data.getInventory(),
                        data.getArmor(),
                        data.isLocked(),
                        new Timestamp(System.currentTimeMillis())
                }
        );
    }

    public int updateDateLock(UUID uuid, boolean lock) {
        int uid = ClientManagerAPI.getUserID(uuid);
        if (uid == -1) return 0;

        return dataManager.executeUpdate(
                AccountTable.PLAYER_SQL_DATE.getTableName(),
                "locked", lock,
                "uid", uid
        );
    }
}
