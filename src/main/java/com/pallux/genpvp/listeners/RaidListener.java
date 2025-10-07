package com.pallux.genpvp.listeners;

import com.pallux.genpvp.GenPvP;
import com.pallux.genpvp.managers.DefenseDataManager;
import com.pallux.genpvp.utils.ColorUtil;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.Random;

public class RaidListener implements Listener {

    private final GenPvP plugin;
    private final Random random;

    public RaidListener(GenPvP plugin) {
        this.plugin = plugin;
        this.random = new Random();
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onRaidPickaxeUse(BlockBreakEvent event) {
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();

        if (!plugin.getRaidManager().isRaidItem(item)) return;

        String type = plugin.getRaidManager().getRaidItemType(item);
        if (!"raid_pickaxe".equals(type)) return;

        Block block = event.getBlock();
        Location location = block.getLocation();

        // Check if it's a defense block
        if (!plugin.getDefenseDataManager().isDefenseBlock(location)) {
            event.setCancelled(true);
            player.sendMessage(plugin.getConfigManager().getMessage("raid-shop.pickaxe-only-defense"));
            return;
        }

        // Get damage from config
        int damage = plugin.getConfigManager().getRaidConfig().getInt("raid-items.raid_pickaxe.damage", 3500);

        // Deal damage
        plugin.getDefenseDataManager().damageDefenseBlock(location, damage);
        int remainingHearts = plugin.getDefenseDataManager().getCurrentHearts(location);

        // Spawn particles
        location.getWorld().spawnParticle(Particle.BLOCK_CRACK, location.clone().add(0.5, 0.5, 0.5), 50, 0.3, 0.3, 0.3, 0, block.getBlockData());
        location.getWorld().playSound(location, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 1.0f);

        // Check if destroyed
        if (remainingHearts <= 0) {
            // Prevent drops
            event.setDropItems(false);

            // Handle door destruction
            if (block.getType() == Material.IRON_DOOR) {
                org.bukkit.block.data.type.Door doorData = (org.bukkit.block.data.type.Door) block.getBlockData();
                Block otherHalf;
                if (doorData.getHalf() == org.bukkit.block.data.Bisected.Half.BOTTOM) {
                    otherHalf = block.getRelative(org.bukkit.block.BlockFace.UP);
                } else {
                    otherHalf = block.getRelative(org.bukkit.block.BlockFace.DOWN);
                }
                if (otherHalf.getType() == Material.IRON_DOOR) {
                    plugin.getDefenseDataManager().removeDefenseBlock(otherHalf.getLocation());
                    otherHalf.setType(Material.AIR);
                }
            }

            // Block will be destroyed by normal break event
            // Remove the pickaxe after use
            if (item.getAmount() > 1) {
                item.setAmount(item.getAmount() - 1);
            } else {
                player.getInventory().setItemInMainHand(null);
            }
            return;
        }

        // Cancel the event since we dealt custom damage
        event.setCancelled(true);

        // Remove the pickaxe after use (one-time use)
        if (item.getAmount() > 1) {
            item.setAmount(item.getAmount() - 1);
        } else {
            player.getInventory().setItemInMainHand(null);
        }

        // Show remaining hearts in action bar (without prefix)
        String format = plugin.getConfigManager().getRaidConfig()
                .getString("damage-display.damage-format", "&#FF0000-{damage} ❤ &#808080({remaining} ❤ remaining)");
        String message = ColorUtil.colorize(format
                .replace("{damage}", String.valueOf(damage))
                .replace("{remaining}", String.valueOf(remainingHearts)));
        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(message));
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onRaidBombUse(PlayerInteractEvent event) {
        // Check for right-click actions (both block and air)
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK && event.getAction() != Action.RIGHT_CLICK_AIR) return;
        if (event.getHand() != EquipmentSlot.HAND) return;

        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        if (item == null || !plugin.getRaidManager().isRaidItem(item)) return;

        String type = plugin.getRaidManager().getRaidItemType(item);
        if (!"raid_bomb".equals(type)) return;

        // Cancel the event to prevent fire charge from being thrown
        event.setCancelled(true);

        // If right-clicking air, just inform the player
        if (event.getAction() == Action.RIGHT_CLICK_AIR) {
            player.sendMessage(plugin.getConfigManager().getMessage("raid-shop.bomb-only-defense"));
            return;
        }

        Block block = event.getClickedBlock();
        if (block == null) return;

        Location location = block.getLocation();

        // Check if it's a defense block
        if (!plugin.getDefenseDataManager().isDefenseBlock(location)) {
            player.sendMessage(plugin.getConfigManager().getMessage("raid-shop.bomb-only-defense"));
            return;
        }

        // Remove one bomb
        if (item.getAmount() > 1) {
            item.setAmount(item.getAmount() - 1);
        } else {
            player.getInventory().setItemInMainHand(null);
        }

        // Get damage range from config
        int minDamage = plugin.getConfigManager().getRaidConfig().getInt("raid-items.raid_bomb.min-damage", 250);
        int maxDamage = plugin.getConfigManager().getRaidConfig().getInt("raid-items.raid_bomb.max-damage", 750);

        // Deal random damage
        int damage = minDamage + random.nextInt(maxDamage - minDamage + 1);
        plugin.getDefenseDataManager().damageDefenseBlock(location, damage);
        int remaining = plugin.getDefenseDataManager().getCurrentHearts(location);

        // Enhanced particle effects
        location.getWorld().spawnParticle(Particle.FLAME, location.clone().add(0.5, 0.5, 0.5), 30, 0.3, 0.3, 0.3, 0.1);
        location.getWorld().spawnParticle(Particle.LAVA, location.clone().add(0.5, 0.5, 0.5), 10, 0.3, 0.3, 0.3, 0);
        location.getWorld().spawnParticle(Particle.SMOKE_LARGE, location.clone().add(0.5, 0.5, 0.5), 20, 0.3, 0.3, 0.3, 0.05);
        location.getWorld().spawnParticle(Particle.EXPLOSION_NORMAL, location.clone().add(0.5, 0.5, 0.5), 5, 0.2, 0.2, 0.2, 0.1);
        location.getWorld().playSound(location, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 1.2f);

        // Check if destroyed
        if (remaining <= 0) {
            plugin.getDefenseDataManager().removeDefenseBlock(location);
            block.setType(Material.AIR);

            // Big explosion particles for destroyed block
            location.getWorld().spawnParticle(Particle.EXPLOSION_LARGE, location.clone().add(0.5, 0.5, 0.5), 2);

            // Send to action bar (without prefix)
            String format = plugin.getConfigManager().getRaidConfig()
                    .getString("damage-display.destroyed-format", "&#00FF00Defense block destroyed!");
            String message = ColorUtil.colorize(format);
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(message));
        } else {
            // Send to action bar (without prefix)
            String format = plugin.getConfigManager().getRaidConfig()
                    .getString("damage-display.damage-format", "&#FF0000-{damage} ❤ &#808080({remaining} ❤ remaining)");
            String message = ColorUtil.colorize(format
                    .replace("{damage}", String.valueOf(damage))
                    .replace("{remaining}", String.valueOf(remaining)));
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(message));
        }
    }
}