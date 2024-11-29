package com.alfuwu.sheep;

import it.unimi.dsi.fastutil.Pair;
import org.bukkit.entity.Player;
import org.bukkit.entity.Sheep;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

public final class SheepEmpire extends JavaPlugin {
    public Map<Player, Pair<Sheep, @Nullable Integer>> sheeps = new HashMap<>();

    public static SheepEmpire instance;

    @Override
    public void onEnable() {
        instance = this;
        // Plugin startup logic
        getLogger().info("SHEEP");
        getServer().getPluginManager().registerEvents(new Sheepish(), this);
        getServer().getPluginManager().registerEvents(new Sheeper(), this);
    }

    @Override
    public void onDisable() {
        for (Map.Entry<Player, Pair<Sheep, Integer>> entry : sheeps.entrySet()) {
            entry.getValue().left().remove();
            entry.getKey().setInvisible(false);
        }
    }
}
