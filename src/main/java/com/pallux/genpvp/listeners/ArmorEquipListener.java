package com.pallux.genpvp.listeners;

import com.pallux.genpvp.GenPvP;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerItemBreakEvent;
import org.bukkit.inventory.ItemStack;

public class ArmorEquipListener implements Listener {

    private final GenPvP plugin;
    private final ArmorListener armorListener;

    public ArmorEquipListener(GenPvP plugin, ArmorListener armorListener) {
        this.plugin = plugin;
        this.armorListener = armorListener;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getWhoClicked();

        // Check if armor slot was affected
        if (event.getSlotType() == InventoryType.SlotType.ARMOR ||
                (event.getSlot() >= 36 && event.getSlot() <= 39)) {

            // Update armor effects after a short delay
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                armorListener.updateArmorEffects(player);
            }, 1L);
        }

        // Check if clicked item or cursor item is armor
        ItemStack clicked = event.getCurrentItem();
        ItemStack cursor = event.getCursor();

        if (isArmor(clicked) || isArmor(cursor)) {
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                armorListener.updateArmorEffects(player);
            }, 1L);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItemDrop().getItemStack();

        if (isArmor(item)) {
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                armorListener.updateArmorEffects(player);
            }, 1L);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerItemBreak(PlayerItemBreakEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getBrokenItem();

        if (isArmor(item)) {
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                armorListener.updateArmorEffects(player);
            }, 1L);
        }
    }

    private boolean isArmor(ItemStack item) {
        if (item == null) return false;

        switch (item.getType()) {
            case LEATHER_HELMET:
            case LEATHER_CHESTPLATE:
            case LEATHER_LEGGINGS:
            case LEATHER_BOOTS:
            case CHAINMAIL_HELMET:
            case CHAINMAIL_CHESTPLATE:
            case CHAINMAIL_LEGGINGS:
            case CHAINMAIL_BOOTS:
            case IRON_HELMET:
            case IRON_CHESTPLATE:
            case IRON_LEGGINGS:
            case IRON_BOOTS:
            case GOLDEN_HELMET:
            case GOLDEN_CHESTPLATE:
            case GOLDEN_LEGGINGS:
            case GOLDEN_BOOTS:
            case DIAMOND_HELMET:
            case DIAMOND_CHESTPLATE:
            case DIAMOND_LEGGINGS:
            case DIAMOND_BOOTS:
            case NETHERITE_HELMET:
            case NETHERITE_CHESTPLATE:
            case NETHERITE_LEGGINGS:
            case NETHERITE_BOOTS:
                return true;
            default:
                return false;
        }
    }
}