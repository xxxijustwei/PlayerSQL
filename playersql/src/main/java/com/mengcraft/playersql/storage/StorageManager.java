package com.mengcraft.playersql.storage;

import com.mengcraft.playersql.DataSerializer;
import com.mengcraft.playersql.PlayerData;
import com.mengcraft.playersql.PluginMain;
import com.mengcraft.playersql.Utils;
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
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class StorageManager {

    private final DataManager dataManager;

    public StorageManager() {
        this.dataManager = ClientManagerAPI.getDataManager();
    }

    public void init() {
        for (AccountTable table : AccountTable.values()) {
            table.createTable();
        }
    }

    public PlayerData find(Player player) {
        int uid = ClientManagerAPI.getUserID(player.getUniqueId());
        if (uid == -1) return null;

        PlayerData account = new PlayerData(uid);

        try (DatabaseQuery query = dataManager.createQuery(
                AccountTable.PLAYER_SQL_DATA.getTableName(),
                "uid", uid
        )) {
            ResultSet result = query.getResultSet();
            if (result.next()) {
                int hand = result.getInt("hand");
                boolean locked = result.getBoolean("locked");
                Timestamp timestamp = result.getTimestamp("lastUpdate");

                account.setHand(hand);
                account.setLocked(locked);
                account.setLastUpdate(timestamp);
            }
            else {
                return null;
            }
        }
        catch (SQLException e ) {
            e.printStackTrace();
        }

        try (DatabaseQuery query = dataManager.createQuery(
                AccountTable.PLAYER_INVENTORY.getTableName(),
                "uid", uid
        )) {
            Map<Integer, ItemStack> slots = new HashMap<>();

            ResultSet result = query.getResultSet();
            while (result.next()) {
                int slot = result.getInt("slot");
                String id = result.getString("item_id");
                if (id == null || id.equals("")) continue;

                int amount = result.getInt("item_amount");
                String data = result.getString("item_data");
                String unique = result.getString("item_unique");

                slots.put(slot, DataSerializer.deserialize(player, id, amount, data, unique));
            }

            account.setSlots(slots);
        }
        catch (SQLException e) {
            e.printStackTrace();
        }

        return account;
    }

    public void update(PlayerData data) {
        dataManager.executeUpdate(
                AccountTable.PLAYER_SQL_DATA.getTableName(),
                new String[] {"hand", "locked"},
                new Object[] {data.getHand(), data.isLocked()},
                new String[] {"uid"},
                new Object[] {data.getUid()}
        );

        dataManager.executeReplace(
                AccountTable.PLAYER_INVENTORY.getTableName(),
                new String[] {"uid", "slot", "item_id", "item_amount", "item_data", "item_unique"},
                data.getSlotConvertData()
        );
    }

    public void save(PlayerData data) {
        dataManager.executeReplace(
                AccountTable.PLAYER_SQL_DATA.getTableName(),
                new String[] {"uid", "hand", "locked", "lastUpdate"},
                new Object[] {data.getUid(), data.getHand(), data.isLocked(), new Timestamp(System.currentTimeMillis())}
        );
    }

    public int updateDateLock(UUID uuid, boolean lock) {
        int uid = ClientManagerAPI.getUserID(uuid);
        if (uid == -1) return 0;

        return dataManager.executeUpdate(
                AccountTable.PLAYER_SQL_DATA.getTableName(),
                "locked", lock,
                "uid", uid
        );
    }
}
