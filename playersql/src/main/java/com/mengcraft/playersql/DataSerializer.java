package com.mengcraft.playersql;

import com.comphenix.protocol.utility.StreamSerializer;
import com.google.gson.stream.MalformedJsonException;
import ink.ptms.zaphkiel.ZaphkielAPI;
import ink.ptms.zaphkiel.api.ItemStream;
import lombok.SneakyThrows;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class DataSerializer {

    @SneakyThrows
    public static String serialize(ItemStack input) {
        ItemStream itemStream = ZaphkielAPI.INSTANCE.read(input);
        if (itemStream.isVanilla()) {
            return StreamSerializer.getDefault().serializeItemStack(input);
        }

        return ZaphkielAPI.INSTANCE.serialize(input).toString();
    }

    @SneakyThrows
    public static ItemStack deserialize(Player player, String input) {
        try {
            ItemStream itemStream = ZaphkielAPI.INSTANCE.deserialize(input);
            return itemStream.rebuildToItemStack(player);
        }
        catch (Exception e) {
            return StreamSerializer.getDefault().deserializeItemStack(input);
        }
    }
}
