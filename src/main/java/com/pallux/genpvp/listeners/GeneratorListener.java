package com.pallux.genpvp.listeners;

import com.pallux.genpvp.GenPvP;
import com.pallux.genpvp.guis.GeneratorUpgradeGUI;
import com.pallux.genpvp.utils.ColorUtil;
import com.pallux.genpvp.utils.PlayerData;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

public class GeneratorListener implements Listener {

    private final GenPvP plugin;

    public GeneratorListener(GenPvP plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();
        ItemStack item = event.getItemInHand();

        // Check if the placed block is a generator
        if (!isGeneratorItem(item)) {
            return;
        }

        // Check if world is allowed
        if (!plugin.getConfigManager().isWorldAllowed(block.getWorld().getName())) {
            event.setCancelled(true);
            player.sendMessage(ColorUtil.colorize(plugin.getConfigManager().getMessage("generator.world-disabled")));
            return;
        }

        // Get player data
        PlayerData data = plugin.getDataManager().getPlayerData(player);

        // Check if player has available slots
        if (!plugin.getLevelManager().hasAvailableSlots(data)) {
            event.setCancelled(true);
            player.sendMessage(ColorUtil.colorize(plugin.getConfigManager()
                    .getMessage("generator.max-reached", "{max}",
                            String.valueOf(plugin.getLevelManager().getCurrentSlots(data)))));
            player.sendMessage(ColorUtil.colorize(plugin.getConfigManager().getMessage("generator.max-reached-hint")));

            if (plugin.getConfigManager().isSoundsEnabled()) {
                player.playSound(player.getLocation(), Sound.valueOf(plugin.getConfigManager().getErrorSound()), 1.0f, 1.0f);
            }
            return;
        }

        // Determine generator tier from item
        int tier = getGeneratorTierFromItem(item);
        if (tier == 0) {
            return;
        }

        // Add generator to data
        Location location = block.getLocation();
        plugin.getDataManager().addGenerator(location, tier);
        data.addGenerator();

        // Send success message
        player.sendMessage(ColorUtil.colorize(plugin.getConfigManager()
                .getMessage("generator.placed",
                        "{current}", String.valueOf(data.getGeneratorsPlaced()),
                        "{max}", String.valueOf(plugin.getLevelManager().getCurrentSlots(data)))));

        // Play sound
        if (plugin.getConfigManager().isSoundsEnabled()) {
            player.playSound(player.getLocation(), Sound.BLOCK_STONE_PLACE, 1.0f, 1.0f);
        }

        // Record statistic
        plugin.getStatisticsManager().recordBlockPlaced(player);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();
        Location location = block.getLocation();

        // Check if this block is a generator
        if (!plugin.getDataManager().isGenerator(location)) {
            return;
        }

        // Get generator tier
        int tier = plugin.getDataManager().getGeneratorTier(location);

        // Remove generator from data
        plugin.getDataManager().removeGenerator(location);

        // Find and update the owner's generator count
        // Since generators can be stolen, we need to check all players
        for (PlayerData data : plugin.getDataManager().getAllPlayerData()) {
            if (data.getGeneratorsPlaced() > 0) {
                // We'll just decrement the breaker's count if they have any
                if (data.getUuid().equals(player.getUniqueId())) {
                    data.removeGenerator();
                    break;
                }
            }
        }

        // Handle drop
        if (plugin.getConfigManager().isAutoPickupOnBreak()) {
            event.setDropItems(false);
            ItemStack generator = plugin.getGeneratorManager().createGeneratorItem(tier, 1);
            if (generator != null) {
                player.getInventory().addItem(generator);
            }
        } else {
            event.setDropItems(false);
            ItemStack generator = plugin.getGeneratorManager().createGeneratorItem(tier, 1);
            if (generator != null) {
                block.getWorld().dropItemNaturally(location, generator);
            }
        }

        // Get player data for the breaker
        PlayerData breakerData = plugin.getDataManager().getPlayerData(player);

        // Send message
        player.sendMessage(ColorUtil.colorize(plugin.getConfigManager()
                .getMessage("generator.removed",
                        "{current}", String.valueOf(breakerData.getGeneratorsPlaced()),
                        "{max}", String.valueOf(plugin.getLevelManager().getCurrentSlots(breakerData)))));

        // Play sound
        if (plugin.getConfigManager().isSoundsEnabled()) {
            player.playSound(player.getLocation(), Sound.BLOCK_STONE_BREAK, 1.0f, 1.0f);
        }

        // Record statistic
        plugin.getStatisticsManager().recordBlockBroken(player);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteract(PlayerInteractEvent event) {
        // Only handle right-click on block
        if (!event.getAction().toString().contains("RIGHT_CLICK")) {
            return;
        }

        // Only handle main hand
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }

        Block block = event.getClickedBlock();
        if (block == null) {
            return;
        }

        Player player = event.getPlayer();
        Location location = block.getLocation();

        // Check if this block is a generator
        if (!plugin.getDataManager().isGenerator(location)) {
            return;
        }

        // Check if player is sneaking (shift + right-click)
        if (player.isSneaking()) {
            event.setCancelled(true);

            // Open upgrade GUI
            int tier = plugin.getDataManager().getGeneratorTier(location);
            new GeneratorUpgradeGUI(plugin, player, location, tier).open();

            // Play sound
            if (plugin.getConfigManager().isSoundsEnabled()) {
                player.playSound(player.getLocation(),
                        Sound.valueOf(plugin.getConfigManager().getOpenSound()), 1.0f, 1.0f);
            }
        }
    }

    /**
     * Checks if an item is a generator item
     */
    private boolean isGeneratorItem(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return false;
        }

        if (!item.getItemMeta().hasDisplayName()) {
            return false;
        }

        String displayName = item.getItemMeta().getDisplayName();

        // Check against all generator tiers
        for (int tier = 1; tier <= plugin.getGeneratorManager().getMaxTier(); tier++) {
            ItemStack genItem = plugin.getGeneratorManager().createGeneratorItem(tier, 1);
            if (genItem != null && genItem.hasItemMeta() && genItem.getItemMeta().hasDisplayName()) {
                if (displayName.equals(genItem.getItemMeta().getDisplayName())) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Gets the tier of a generator item
     */
    private int getGeneratorTierFromItem(ItemStack item) {
        if (item == null || !item.hasItemMeta() || !item.getItemMeta().hasDisplayName()) {
            return 0;
        }

        String displayName = item.getItemMeta().getDisplayName();

        // Check against all generator tiers
        for (int tier = 1; tier <= plugin.getGeneratorManager().getMaxTier(); tier++) {
            ItemStack genItem = plugin.getGeneratorManager().createGeneratorItem(tier, 1);
            if (genItem != null && genItem.hasItemMeta() && genItem.getItemMeta().hasDisplayName()) {
                if (displayName.equals(genItem.getItemMeta().getDisplayName())) {
                    return tier;
                }
            }
        }

        return 0;
    }
}