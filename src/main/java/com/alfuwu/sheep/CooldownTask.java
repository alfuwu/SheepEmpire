package com.alfuwu.sheep;

import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class CooldownTask extends BukkitRunnable {
    public final Sheepish sheepish;
    public final Player player;

    public CooldownTask(Sheepish sheepish, Player player) {
        this.sheepish = sheepish;
        this.player = player;
    }

    @Override
    public void run() {
        Integer cooldown = sheepish.cooldowns.get(player);
        if (cooldown == null) {
            sheepish.cooldowns.remove(player);
        } else if (cooldown > 1) {
            sheepish.cooldowns.put(player, cooldown - 1);
        } else {
            sheepish.cooldowns.remove(player);
            this.cancel();
        }
    }
}
