package com.mengcraft.playersql;

import org.bukkit.configuration.Configuration;
import org.bukkit.plugin.java.JavaPlugin;

public class Config {

    public static final Configuration CONF;

    public static final boolean KICK_LOAD_FAILED;
    public static final boolean OMIT_PLAYER_DEATH;

    public static final boolean DEBUG;
    public static final int SYN_DELAY;

    static {
        CONF = JavaPlugin.getPlugin(PluginMain.class).getConfig();
        SYN_DELAY = CONF.getInt("plugin.delay", 30);
        DEBUG = CONF.getBoolean("plugin.debug", false);
        OMIT_PLAYER_DEATH = CONF.getBoolean("plugin.omit-player-death", false);
        KICK_LOAD_FAILED = CONF.getBoolean("kick-load-failed", true);
    }

}
