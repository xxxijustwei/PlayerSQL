package com.mengcraft.playersql;

import ink.ptms.zaphkiel.ZaphkielAPI;
import ink.ptms.zaphkiel.api.ItemStream;
import ink.ptms.zaphkiel.taboolib.module.nms.ItemTag;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created on 16-1-2.
 */
public class PlayerData {

    private int uid;

    private int hand;

    private Map<Integer, ItemStack> slots;

    private boolean locked;

    private Timestamp lastUpdate;

    public PlayerData() {
        this.slots = new HashMap<>();
        this.lastUpdate = new Timestamp(System.currentTimeMillis());
    }

    public PlayerData(int uid) {
        this.uid = uid;
        this.slots = new HashMap<>();
        this.lastUpdate = new Timestamp(System.currentTimeMillis());
    }

    public int getUid() {
        return uid;
    }

    public void setUid(int uid) {
        this.uid = uid;
    }

    public int getHand() {
        return hand;
    }

    public void setHand(int hand) {
        this.hand = hand;
    }

    public Map<Integer, ItemStack> getSlots() {
        return slots;
    }

    public void setSlots(Map<Integer, ItemStack> slots) {
        this.slots = slots;
    }

    public void setSlots(Player player) {
        for (int i = 0; i < 41; i++) {
            ItemStack item = player.getInventory().getItem(i);
            if (item == null || item.getItemMeta() == null) {
                this.slots.put(i, null);
                continue;
            }

            this.slots.put(i, item);
        }
    }

    public boolean isLocked() {
        return locked;
    }

    public void setLocked(boolean locked) {
        this.locked = locked;
    }

    public Timestamp getLastUpdate() {
        return lastUpdate;
    }

    public void setLastUpdate(Timestamp lastUpdate) {
        this.lastUpdate = lastUpdate;
    }

    public List<Object[]> getSlotConvertData() {
        List<Object[]> result = new ArrayList<>();
        for (int index : this.slots.keySet()) {
            ItemStack item = this.slots.get(index);
            if (item == null) {
                result.add(new Object[] {this.uid, index, null, 0, null, null});
                continue;
            }

            ItemStream itemStream = ZaphkielAPI.INSTANCE.read(item);
            if (itemStream.isVanilla()) {
                result.add(new Object[] {this.uid, index, null, 0, null, null});
                continue;
            }

            String id = itemStream.getZaphkielName();
            int amount = item.getAmount();
            String data = itemStream.getZaphkielData().toJson();
            ItemTag uniqueTag = itemStream.getZaphkielUniqueData();
            String unique = uniqueTag == null ? null : uniqueTag.toJson();

            result.add(new Object[]{this.uid, index, id, amount, data, unique});
        }

        return result;
    }

}
