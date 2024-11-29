package com.alfuwu.sheep;

import de.tr7zw.nbtapi.NBT;
import io.papermc.paper.event.player.PrePlayerAttackEntityEvent;
import it.unimi.dsi.fastutil.Pair;
import it.unimi.dsi.fastutil.objects.ObjectIntMutablePair;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.damage.DamageSource;
import org.bukkit.entity.Player;
import org.bukkit.entity.Sheep;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.entity.EntityType;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Sheepish implements Listener  {
    List<DamageSource> tempSources = new ArrayList<>(); // prevents infinite recursion from happening between damaging players & sheep

    public static void toggleSheepify(Player player, int time) {
        if (SheepEmpire.instance.sheeps.containsKey(player)) { // player is sheepo mode
            Pair<Sheep, Integer> pair = SheepEmpire.instance.sheeps.get(player);
            if (pair.right() == -1) {
                pair.left().remove();
                SheepEmpire.instance.sheeps.remove(player);
                player.setInvisible(false);
                player.setCollidable(true);
            } else if (time != -1) {
                pair.right(time);
            }
        } else {
            Sheep sheep = (Sheep)player.getWorld().spawnEntity(player.getLocation(), EntityType.SHEEP, false);
            sheep.setAI(false);
            sheep.setCollidable(false);
            player.setCollidable(false);
            player.setInvisible(true);
            SheepEmpire.instance.sheeps.put(player, new ObjectIntMutablePair<>(sheep, time > 0 ? time : -1));
            new SoulmateTask(player, sheep).runTaskTimer(SheepEmpire.instance, 0, 1);
            if (time > 0)
                new UnsheepifyTask(player).runTaskTimer(SheepEmpire.instance, 0, 1);
        }
        player.getWorld().spawnParticle(Particle.EXPLOSION, player.getLocation(), 12);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 1.0f);
    }

    public static void handleClick(Player player, @Nullable Player hitPlayer) {
        ItemStack item = player.getInventory().getItemInMainHand();
        boolean isSheepish = NBT.get(item, nbt -> {
            return nbt.getBoolean("sheep:sheepish");
        });
        if (isSheepish) {
            if (hitPlayer != null) {
                toggleSheepify(hitPlayer, 100);
            } else {
                toggleSheepify(player, -1);
                player.swingMainHand();
            }
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction().isRightClick() && event.getAction() != Action.RIGHT_CLICK_BLOCK)
            handleClick(event.getPlayer(), null);
    }

    @EventHandler
    public void onPlayerAttack(PrePlayerAttackEntityEvent event) {
        if (event.willAttack() && event.getAttacked() instanceof Player player)
            handleClick(event.getPlayer(), player);
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Pair<Sheep, Integer> pair = SheepEmpire.instance.sheeps.remove(event.getPlayer());
        if (pair != null) { // kill the sheep if the player dies
            pair.left().setInvulnerable(false);
            pair.left().damage(999999999);
        }
        event.getPlayer().setInvisible(false);
    }

    @EventHandler
    public void onSheepDeath(EntityDeathEvent event) {
        for (Map.Entry<Player, Pair<Sheep, Integer>> entry : SheepEmpire.instance.sheeps.entrySet())
            if (entry.getValue().left() == event.getEntity())
                event.setCancelled(true);
    }

    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        if (event.getDamageSource().getCausingEntity() != null && !tempSources.contains(event.getDamageSource())) { // only apply if the damage is from another enemy (bc we don't want the player to take double damage from lava, etc)
            for (Map.Entry<Player, Pair<Sheep, Integer>> entry : SheepEmpire.instance.sheeps.entrySet()) {
                if (entry.getValue().left() == event.getEntity()) {
                    tempSources.add(event.getDamageSource());
                    entry.getKey().damage(event.getDamage(), event.getDamageSource());
                } else if (entry.getKey() == event.getEntity()) {
                    tempSources.add(event.getDamageSource());
                    entry.getValue().left().damage(event.getDamage(), event.getDamageSource());
                }
            }
        } else if (event.getDamageSource().getCausingEntity() != null) {
            tempSources.remove(event.getDamageSource());
        }
    }

    @EventHandler
    public void onPlayerLeave(PlayerQuitEvent event) {
        Pair<Sheep, Integer> pair = SheepEmpire.instance.sheeps.remove(event.getPlayer());
        if (pair != null) // remove the sheep from the world if the transformed player leaves
            pair.left().remove();
        event.getPlayer().setInvisible(false);
    }
}
