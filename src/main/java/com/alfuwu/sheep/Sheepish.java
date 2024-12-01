package com.alfuwu.sheep;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.EnumWrappers;
import de.tr7zw.nbtapi.NBT;
import io.papermc.paper.event.player.PrePlayerAttackEntityEvent;
import it.unimi.dsi.fastutil.Pair;
import it.unimi.dsi.fastutil.objects.ObjectIntMutablePair;
import org.bukkit.Bukkit;
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
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.meta.ArmorMeta;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Sheepish implements Listener  {
    public static List<EnumWrappers.ItemSlot> ARMORS = List.of(EnumWrappers.ItemSlot.HEAD, EnumWrappers.ItemSlot.CHEST, EnumWrappers.ItemSlot.LEGS, EnumWrappers.ItemSlot.FEET);
    public List<DamageSource> tempSources = new ArrayList<>(); // prevents infinite recursion from happening between damaging players & sheep
    public Map<Player, Integer> cooldowns = new HashMap<>();

    public void toggleSheepify(Player player, int time) {
        if (SheepEmpire.instance.sheeps.containsKey(player)) { // player is sheepo mode
            Pair<Sheep, Integer> pair = SheepEmpire.instance.sheeps.get(player);
            if (pair.right() == -1) {
                pair.left().remove();
                SheepEmpire.instance.sheeps.remove(player);
                player.setInvisible(false);
                sendArmor(player);
            } else if (time != -1) {
                pair.right(time);
            }
        } else {
            Sheep sheep = (Sheep)player.getWorld().spawnEntity(player.getLocation(), EntityType.SHEEP, false);
            sheep.setAI(false);
            player.setInvisible(true);
            SheepEmpire.instance.sheeps.put(player, new ObjectIntMutablePair<>(sheep, time > 0 ? time : -1));
            new SoulmateTask(player, sheep).runTaskTimer(SheepEmpire.instance, 0, 1);
            if (time > 0)
                new UnsheepifyTask(player).runTaskTimer(SheepEmpire.instance, 0, 1);
            removeArmor(player);
        }
        player.getWorld().spawnParticle(Particle.EXPLOSION, player.getLocation(), 12);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 1.0f);
        if (time > 0) {
            cooldowns.put(player, 700);
            new CooldownTask(this, player).runTaskTimer(SheepEmpire.instance, 0, 1);
        }
    }

    public void handleClick(Player player, @Nullable Player hitPlayer) {
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item.isEmpty())
            return;
        boolean isSheepish = NBT.get(item, nbt -> {
            return nbt.getBoolean("sheep:sheepish");
        });
        if (isSheepish) {
            if (hitPlayer != null) {
                if (!cooldowns.containsKey(hitPlayer) && !hitPlayer.isInvisible())
                    toggleSheepify(hitPlayer, 100);
            } else {
                toggleSheepify(player, -1);
                player.swingMainHand();
            }
        }
    }

    public static void sendArmor(Player player) {
        if (player == null)
            return;
        PacketContainer packetContainer = new PacketContainer(PacketType.Play.Server.ENTITY_EQUIPMENT);
        packetContainer.getIntegers().write(0, player.getEntityId());
        packetContainer.getSlotStackPairLists().write(0, ARMORS.stream().map(s -> (com.comphenix.protocol.wrappers.Pair<EnumWrappers.ItemSlot, ItemStack>)new com.comphenix.protocol.wrappers.Pair(s, getItem(player, s))).toList());
        for (Player onlinePlayer : player.getWorld().getPlayers()) {
            if (onlinePlayer.getUniqueId().equals(player.getUniqueId()))
                continue;
            try {
                ProtocolLibrary.getProtocolManager().sendServerPacket(onlinePlayer, packetContainer, false); //false=ignore listeners that don't just monitor
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            }
        }
    }

    public static void removeArmor(Player player) {
        if (player == null)
            return;
        PacketContainer packetContainer = new PacketContainer(PacketType.Play.Server.ENTITY_EQUIPMENT);
        packetContainer.getIntegers().write(0, player.getEntityId());
        packetContainer.getSlotStackPairLists().write(0, ARMORS.stream().map(s -> (com.comphenix.protocol.wrappers.Pair<EnumWrappers.ItemSlot, ItemStack>)new com.comphenix.protocol.wrappers.Pair(s, ItemStack.empty())).toList());
        for (Player onlinePlayer : player.getWorld().getPlayers())  {
            if (onlinePlayer.getUniqueId().equals(player.getUniqueId()))
                continue;
            try {
                ProtocolLibrary.getProtocolManager().sendServerPacket(onlinePlayer, packetContainer);
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            }
        }
    }

    private static ItemStack getItem(Player player, EnumWrappers.ItemSlot slot) {
        return player.getInventory().getItem(switch (slot) {
            case EnumWrappers.ItemSlot.HEAD -> EquipmentSlot.HEAD;
            case EnumWrappers.ItemSlot.CHEST -> EquipmentSlot.CHEST;
            case EnumWrappers.ItemSlot.LEGS -> EquipmentSlot.LEGS;
            case EnumWrappers.ItemSlot.FEET -> EquipmentSlot.FEET;
            default -> EquipmentSlot.HAND;
        });
    }

    @EventHandler
    public void onPlayerEquip(InventoryClickEvent event) {
        if (((event.getSlot() >= 36 && event.getSlot() <= 39) || event.isShiftClick()) && event.getWhoClicked() instanceof Player player && SheepEmpire.instance.sheeps.containsKey(player))
            Bukkit.getScheduler().runTaskLater(SheepEmpire.instance, () -> removeArmor(player), 1);
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction().isRightClick()) {
            if (event.getAction() != Action.RIGHT_CLICK_BLOCK)
                handleClick(event.getPlayer(), null);
            if (event.getItem() != null && SheepEmpire.instance.sheeps.containsKey(event.getPlayer()))
                Bukkit.getScheduler().runTaskLater(SheepEmpire.instance, () -> removeArmor(event.getPlayer()), 1);
        }
    }

    @EventHandler
    public void onPlayerAttack(PrePlayerAttackEntityEvent event) {
        if (event.getAttacked() instanceof Player player)
            handleClick(event.getPlayer(), player);
        for (Map.Entry<Player, Pair<Sheep, Integer>> entry : SheepEmpire.instance.sheeps.entrySet())
            if (event.getPlayer() == entry.getKey() && event.getAttacked() == entry.getValue().left())
                event.setCancelled(true); // prevent attacking own transformation
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
