package com.alfuwu.sheep;

import it.unimi.dsi.fastutil.Pair;
import org.bukkit.entity.Player;
import org.bukkit.entity.Sheep;
import org.bukkit.scheduler.BukkitRunnable;

public class UnsheepifyTask extends BukkitRunnable {
    public final Player player;

    public UnsheepifyTask(Player player) {
        this.player = player;
    }

    @Override
    public void run() {
        Pair<Sheep, Integer> pair = SheepEmpire.instance.sheeps.get(player);
        if (pair == null || pair.right() == 0) {
            this.cancel();
            if (pair != null) {
                SheepEmpire.instance.sheeps.remove(player);
                pair.left().remove();
                player.setInvisible(false);
                Sheepish.sendArmor(player);
            }
        } else {
            pair.right(pair.right() - 1);
        }
    }
}
