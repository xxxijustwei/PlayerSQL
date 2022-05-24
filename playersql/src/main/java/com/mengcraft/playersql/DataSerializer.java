package com.mengcraft.playersql;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import ink.ptms.zaphkiel.ZaphkielAPI;
import ink.ptms.zaphkiel.api.ItemStream;
import lombok.SneakyThrows;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class DataSerializer {

    private final static JsonParser parser = new JsonParser();

    @SneakyThrows
    public static ItemStack deserialize(Player player, String id, int amount, String data, String unique) {
        JsonObject object = new JsonObject();
        object.addProperty("id", id);
        if (data != null && !data.isEmpty()) object.add("data", parser.parse(data));
        if (unique != null && !unique.isEmpty()) object.add("unique", parser.parse(unique));

        ItemStream itemStream = ZaphkielAPI.INSTANCE.deserialize(object);
        ItemStack result = itemStream.rebuildToItemStack(player);
        result.setAmount(amount);

        return result;
    }
}
