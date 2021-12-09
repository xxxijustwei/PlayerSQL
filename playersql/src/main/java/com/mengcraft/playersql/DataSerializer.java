package com.mengcraft.playersql;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import ink.ptms.zaphkiel.ZaphkielAPI;
import ink.ptms.zaphkiel.api.ItemStream;
import lombok.SneakyThrows;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class DataSerializer {

    private final static Gson gson = new Gson();

    @SneakyThrows
    public static String serialize(ItemStack input) {
        ItemStream itemStream = ZaphkielAPI.INSTANCE.read(input);
        if (itemStream.isVanilla()) return "";

        JsonObject result = ZaphkielAPI.INSTANCE.serialize(input);
        result.addProperty("amount", input.getAmount());

        return result.toString();
    }

    @SneakyThrows
    public static ItemStack deserialize(Player player, String input) {
        try {
            JsonObject target = gson.fromJson(input, JsonObject.class);
            ItemStream itemStream = ZaphkielAPI.INSTANCE.deserialize(target);
            ItemStack itemStack = itemStream.rebuildToItemStack(player);
            itemStack.setAmount(target.get("amount").getAsInt());

            return itemStack;
        }
        catch (Exception e) {
            return new ItemStack(Material.AIR);
        }
    }
}
