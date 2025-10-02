package com.pallux.genpvp.listeners;

import com.pallux.genpvp.GenPvP;
import com.pallux.genpvp.utils.ColorUtil;
import com.pallux.genpvp.utils.PlayerData;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

public class NuggetListener implements Listener {

    private final GenPvP plugin;

    public NuggetListener(GenPvP plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteract(PlayerInteractEvent event) {
        // Only handle right-click
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        // Only handle main hand
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }

        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        if (item == null) {
            return;
        }

        // Check if item is a money nugget
        if (plugin.getGeneratorManager().isMoneyNugget(item)) {
            event.setCancelled(true);
            collectAllMoneyNuggets(player);
            return;
        }

        // Check if item is a gem nugget
        if (plugin.getGeneratorManager().isGemNugget(item)) {
            event.setCancelled(true);
            collectAllGemNuggets(player);
            return;
        }
    }

    /**
     * Collects all money nuggets from player's inventory
     */
    private void collectAllMoneyNuggets(Player player) {
        double totalMoney = 0;
        int nuggetCount = 0;

        // Iterate through inventory
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && plugin.getGeneratorManager().isMoneyNugget(item)) {
                int amount = plugin.getGeneratorManager().extractMoneyAmount(item);
                int itemCount = item.getAmount();

                totalMoney += amount * itemCount;
                nuggetCount += itemCount;

                // Remove the item
                item.setAmount(0);
            }
        }

        if (totalMoney <= 0) {
            player.sendMessage(plugin.getConfigManager().getMessage("nugget.none-found"));
            return;
        }

        // Add money to player
        plugin.getEconomy().depositPlayer(player, totalMoney);

        // Record statistic
        plugin.getStatisticsManager().recordMoneyCollected(player, totalMoney);

        // Send messages
        if (nuggetCount > 1) {
            player.sendMessage(plugin.getConfigManager().getMessage("nugget.all-collected"));
        }
        player.sendMessage(plugin.getConfigManager()
                .getMessage("nugget.money-collected", "{amount}", ColorUtil.formatNumber(totalMoney)));

        // Play sound
        if (plugin.getConfigManager().isSoundsEnabled()) {
            player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
        }
    }

    /**
     * Collects all gem nuggets from player's inventory
     */
    private void collectAllGemNuggets(Player player) {
        int totalGems = 0;
        int nuggetCount = 0;

        // Iterate through inventory
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && plugin.getGeneratorManager().isGemNugget(item)) {
                int amount = plugin.getGeneratorManager().extractGemAmount(item);
                int itemCount = item.getAmount();

                totalGems += amount * itemCount;
                nuggetCount += itemCount;

                // Remove the item
                item.setAmount(0);
            }
        }

        if (totalGems <= 0) {
            player.sendMessage(plugin.getConfigManager().getMessage("nugget.none-found"));
            return;
        }

        // Add gems to player
        PlayerData data = plugin.getDataManager().getPlayerData(player);
        data.addGems(totalGems);

        // Record statistic
        plugin.getStatisticsManager().recordGemsCollected(player, totalGems);

        // Send messages
        if (nuggetCount > 1) {
            player.sendMessage(plugin.getConfigManager().getMessage("nugget.all-collected"));
        }
        player.sendMessage(plugin.getConfigManager()
                .getMessage("nugget.gems-collected", "{amount}", String.valueOf(totalGems)));

        // Play sound
        if (plugin.getConfigManager().isSoundsEnabled()) {
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.5f);
        }
    }
}