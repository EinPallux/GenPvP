package com.pallux.genpvp.guis;

import com.pallux.genpvp.GenPvP;
import com.pallux.genpvp.utils.ColorUtil;
import com.pallux.genpvp.utils.PlayerData;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class StatsGUI extends BaseGUI {

    public StatsGUI(GenPvP plugin, Player player) {
        super(plugin, player);
    }

    @Override
    protected String getTitle() {
        String titleFormat = plugin.getConfigManager().getMessagesConfig()
                .getString("gui-titles.stats", "<gradient:#9B59B6:#E74C3C>{player}'s Stats</gradient>");
        return replacePlaceholders(titleFormat, "{player}", player.getName());
    }

    @Override
    protected int getSize() {
        return 45; // 5 rows
    }

    @Override
    protected void setContents() {
        PlayerData data = plugin.getDataManager().getPlayerData(player);

        // Level (slot 10)
        inventory.setItem(10, createLevelItem(data));

        // Kills (slot 11)
        inventory.setItem(11, createKillsItem(data));

        // Deaths (slot 12)
        inventory.setItem(12, createDeathsItem(data));

        // K/D Ratio (slot 13)
        inventory.setItem(13, createKDRItem(data));

        // Blocks Placed (slot 14)
        inventory.setItem(14, createBlocksPlacedItem(data));

        // Blocks Broken (slot 15)
        inventory.setItem(15, createBlocksBrokenItem(data));

        // Money Collected (slot 16)
        inventory.setItem(16, createMoneyCollectedItem(data));

        // Gems Collected (slot 20)
        inventory.setItem(20, createGemsCollectedItem(data));

        // Playtime (slot 21)
        inventory.setItem(21, createPlaytimeItem(data));

        // Generators (slot 22)
        inventory.setItem(22, createGeneratorsItem(data));

        // Close button (slot 40)
        inventory.setItem(40, createCloseButton());

        // Fill empty slots
        fillEmptySlots();
    }

    @Override
    public void handleClick(Player player, int slot, ItemStack item, ClickType clickType) {
        if (slot == 40) {
            // Close button
            close();
        }
    }

    private ItemStack createLevelItem(PlayerData data) {
        String name = plugin.getConfigManager().getMessagesConfig()
                .getString("gui.stats.level.name", "<gradient:#FFD700:#FFA500>Level</gradient>");

        List<String> loreFormat = plugin.getConfigManager().getMessagesConfig()
                .getStringList("gui.stats.level.lore");
        List<String> lore = replacePlaceholders(loreFormat, "{level}", String.valueOf(data.getLevel()));

        return createItem(Material.EXPERIENCE_BOTTLE, name, lore);
    }

    private ItemStack createKillsItem(PlayerData data) {
        String name = plugin.getConfigManager().getMessagesConfig()
                .getString("gui.stats.kills.name", "<red>Player Kills</red>");

        List<String> loreFormat = plugin.getConfigManager().getMessagesConfig()
                .getStringList("gui.stats.kills.lore");
        List<String> lore = replacePlaceholders(loreFormat, "{kills}", String.valueOf(data.getKills()));

        return createItem(Material.DIAMOND_SWORD, name, lore);
    }

    private ItemStack createDeathsItem(PlayerData data) {
        String name = plugin.getConfigManager().getMessagesConfig()
                .getString("gui.stats.deaths.name", "<dark_red>Deaths</dark_red>");

        List<String> loreFormat = plugin.getConfigManager().getMessagesConfig()
                .getStringList("gui.stats.deaths.lore");
        List<String> lore = replacePlaceholders(loreFormat, "{deaths}", String.valueOf(data.getDeaths()));

        return createItem(Material.SKELETON_SKULL, name, lore);
    }

    private ItemStack createKDRItem(PlayerData data) {
        String name = plugin.getConfigManager().getMessagesConfig()
                .getString("gui.stats.kdr.name", "<gold>K/D Ratio</gold>");

        List<String> loreFormat = plugin.getConfigManager().getMessagesConfig()
                .getStringList("gui.stats.kdr.lore");
        List<String> lore = replacePlaceholders(loreFormat, "{kdr}", String.format("%.2f", data.getKDR()));

        return createItem(Material.IRON_SWORD, name, lore);
    }

    private ItemStack createBlocksPlacedItem(PlayerData data) {
        String name = plugin.getConfigManager().getMessagesConfig()
                .getString("gui.stats.blocks-placed.name", "<green>Blocks Placed</green>");

        List<String> loreFormat = plugin.getConfigManager().getMessagesConfig()
                .getStringList("gui.stats.blocks-placed.lore");
        List<String> lore = replacePlaceholders(loreFormat, "{blocks}", ColorUtil.formatNumber(data.getBlocksPlaced()));

        return createItem(Material.GRASS_BLOCK, name, lore);
    }

    private ItemStack createBlocksBrokenItem(PlayerData data) {
        String name = plugin.getConfigManager().getMessagesConfig()
                .getString("gui.stats.blocks-broken.name", "<yellow>Blocks Broken</yellow>");

        List<String> loreFormat = plugin.getConfigManager().getMessagesConfig()
                .getStringList("gui.stats.blocks-broken.lore");
        List<String> lore = replacePlaceholders(loreFormat, "{blocks}", ColorUtil.formatNumber(data.getBlocksBroken()));

        return createItem(Material.DIAMOND_PICKAXE, name, lore);
    }

    private ItemStack createMoneyCollectedItem(PlayerData data) {
        String name = plugin.getConfigManager().getMessagesConfig()
                .getString("gui.stats.money-collected.name", "<gradient:#2ECC71:#27AE60>Money Collected</gradient>");

        List<String> loreFormat = plugin.getConfigManager().getMessagesConfig()
                .getStringList("gui.stats.money-collected.lore");
        List<String> lore = replacePlaceholders(loreFormat, "{money}", ColorUtil.formatNumber(data.getMoneyCollected()));

        return createItem(Material.EMERALD, name, lore);
    }

    private ItemStack createGemsCollectedItem(PlayerData data) {
        String name = plugin.getConfigManager().getMessagesConfig()
                .getString("gui.stats.gems-collected.name", "<gradient:#E74C3C:#9B59B6>Gems Collected</gradient>");

        List<String> loreFormat = plugin.getConfigManager().getMessagesConfig()
                .getStringList("gui.stats.gems-collected.lore");
        List<String> lore = replacePlaceholders(loreFormat, "{gems}", String.valueOf(data.getGemsCollected()));

        return createItem(Material.DIAMOND, name, lore);
    }

    private ItemStack createPlaytimeItem(PlayerData data) {
        String name = plugin.getConfigManager().getMessagesConfig()
                .getString("gui.stats.playtime.name", "<aqua>Playtime</aqua>");

        String playtime = ColorUtil.formatTime(data.getPlaytime());

        List<String> loreFormat = plugin.getConfigManager().getMessagesConfig()
                .getStringList("gui.stats.playtime.lore");
        List<String> lore = replacePlaceholders(loreFormat, "{playtime}", playtime);

        return createItem(Material.CLOCK, name, lore);
    }

    private ItemStack createGeneratorsItem(PlayerData data) {
        String name = plugin.getConfigManager().getMessagesConfig()
                .getString("gui.stats.generators.name", "<gradient:#4ECDC4:#556270>Generators</gradient>");

        int maxSlots = plugin.getLevelManager().getCurrentSlots(data);

        List<String> loreFormat = plugin.getConfigManager().getMessagesConfig()
                .getStringList("gui.stats.generators.lore");
        List<String> lore = replacePlaceholders(loreFormat,
                "{placed}", String.valueOf(data.getGeneratorsPlaced()),
                "{max}", String.valueOf(maxSlots));

        return createItem(Material.FURNACE, name, lore);
    }
}