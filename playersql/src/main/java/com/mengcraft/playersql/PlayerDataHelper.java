package com.mengcraft.playersql;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import com.mengcraft.playersql.lib.LZ4;
import com.mengcraft.playersql.lib.VarInt;
import lombok.SneakyThrows;

import java.io.*;
import java.util.UUID;

public class PlayerDataHelper {

    @SneakyThrows
    public static byte[] encode(PlayerData dat) {
        ByteArrayDataOutput output = ByteStreams.newDataOutput();
        output.writeInt(dat.getUid());
        output.writeInt(dat.getHand());
        write(output, dat.getInventory());
        byte[] uncompressed = output.toByteArray();
        output = ByteStreams.newDataOutput();
        VarInt.writeUnsignedVarInt(output, uncompressed.length);
        byte[] compressed = LZ4.compress(uncompressed);
        if (Config.DEBUG) {
            PluginMain.getPlugin().log(String.format("PlayerDataHelper.encode LZ4 compressor %s -> %s", uncompressed.length, compressed.length));
        }
        VarInt.writeUnsignedVarInt(output, compressed.length);
        output.write(compressed);
        return output.toByteArray();
    }

    @SneakyThrows
    public static PlayerData decode(byte[] buf) {
        ByteArrayDataInput input = ByteStreams.newDataInput(buf);// DECOMPRESS STREAM
        int uncompressedLen = (int) VarInt.readUnsignedVarInt(input);
        int compressedLen = (int) VarInt.readUnsignedVarInt(input);
        byte[] compressed = new byte[compressedLen];
        input.readFully(compressed);
        byte[] decompressed = LZ4.decompress(compressed, uncompressedLen);
        input = ByteStreams.newDataInput(decompressed);
        PlayerData dat = new PlayerData();// PARSER PLAYER DATA
        dat.setUid(input.readInt());
        dat.setHand(input.readInt());
        dat.setInventory(readString(input));
        return dat;
    }

    @SneakyThrows
    private static void write(DataOutput buf, String input) {
        if (input == null) {
            VarInt.writeUnsignedVarInt(buf, 0);
            return;
        }
        byte[] data = input.getBytes("utf8");
        VarInt.writeUnsignedVarInt(buf, data.length);
        buf.write(data);
    }

    @SneakyThrows
    private static String readString(DataInput buf) {
        long len = VarInt.readUnsignedVarInt(buf);
        if (len == 0) {
            return null;
        }
        byte[] readbuf = new byte[(int) len];
        buf.readFully(readbuf);
        return new String(readbuf, "utf8");
    }
}
