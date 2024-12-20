package com.alfuwu.sheep;

import de.tr7zw.nbtapi.NBT;
import net.kyori.adventure.key.Key;
import org.bukkit.GameMode;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Sheep;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

public class Sheeper implements Listener {
    public double increaseAttribute(LivingEntity entity, Attribute attr, double base, String id) {
        AttributeModifier mod = entity.getAttribute(attr).getModifier(Key.key(id));
        if (mod != null) {
            base += mod.getAmount();
            entity.getAttribute(attr).removeModifier(Key.key(id));
        }
        entity.getAttribute(attr).addModifier(new AttributeModifier(NamespacedKey.fromString(id), base, AttributeModifier.Operation.ADD_NUMBER));
        return base;
    }

    @EventHandler
    public void onBlockPlaced(BlockPlaceEvent event) {
        ItemStack block = event.getItemInHand();
        boolean isSheeper = NBT.get(block, nbt -> {
            return nbt.getBoolean("sheep:sheeper");
        });
        if (isSheeper)
            event.setCancelled(true);
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEntityEvent event) {
        ItemStack item = event.getPlayer().getInventory().getItem(event.getHand());
        if (item.isEmpty())
            return;
        boolean isSheeper = NBT.get(item, nbt -> {
            return nbt.getBoolean("sheep:sheeper");
        });
        if (event.getHand() == EquipmentSlot.OFF_HAND && !event.getPlayer().getInventory().getItemInMainHand().isEmpty())
            isSheeper &= NBT.get(event.getPlayer().getInventory().getItemInMainHand(), nbt -> !nbt.getBoolean("sheep:sheeper"));
        if (event.getRightClicked() instanceof Sheep sheep && sheep.hasAI() && isSheeper) {
            double hp = increaseAttribute(sheep, Attribute.GENERIC_MAX_HEALTH, 4, "sheep:sheeper");
            sheep.heal(8);
            if (hp / 4 >= 24)
                increaseAttribute(sheep, Attribute.GENERIC_ARMOR, 2, "sheep:sheeper");
            if (event.getPlayer().getGameMode() != GameMode.CREATIVE)
                item.subtract();
            if (event.getHand() == EquipmentSlot.HAND)
                event.getPlayer().swingMainHand();
            else
                event.getPlayer().swingOffHand();
            sheep.getWorld().spawnParticle(Particle.HEART, sheep.getLocation().add(0, 0.5, 0), 7);
            sheep.getWorld().playSound(sheep.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 0.5f + ((float)(hp - 4) / 12));
        }
    }
}
