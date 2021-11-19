package com.mengcraft.playersql;

import lombok.AllArgsConstructor;

import java.sql.Timestamp;

/**
 * Created on 16-1-2.
 */
@AllArgsConstructor
public class PlayerData {

    private int uid;

    private int hand;

    private String inventory;

    private String armor;

    private boolean locked;

    private Timestamp lastUpdate;

    public PlayerData() {
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

    public String getInventory() {
        return inventory;
    }

    public void setInventory(String inventory) {
        this.inventory = inventory;
    }

    public String getArmor() {
        return armor;
    }

    public void setArmor(String armor) {
        this.armor = armor;
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

}
