package com.alfuwu.sheep;

import org.bukkit.entity.LivingEntity;
import org.bukkit.scheduler.BukkitRunnable;

public class SoulmateTask extends BukkitRunnable {
    public final LivingEntity target;
    public final LivingEntity follower;

    public SoulmateTask(LivingEntity target, LivingEntity follower) {
        this.target = target;
        this.follower = follower;
    }

    @Override
    public void run() {
        if (target.isDead() || follower.isDead())
            this.cancel();
        follower.teleport(target.getLocation().add(target.getVelocity()));
        follower.setBodyYaw(target.getBodyYaw());
        follower.setRotation(target.getYaw(), target.getPitch());
    }
}
