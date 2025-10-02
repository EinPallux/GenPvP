package com.pallux.genpvp.guis;

import com.pallux.genpvp.GenPvP;
import com.pallux.genpvp.utils.ColorUtil;
import com.pallux.genpvp.utils.PlayerData;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class LevelUpGUI extends BaseGUI {

    public LevelUpGUI(GenPvP plugin, Player player) {
        super(plugin, player);
    }

    @Override
    protected String getTitle() {
        return plugin.getConfigManager().getColorizedString("messages.yml", "gui-titles.level-up");
    }

    @Override
    protected int getSize() {
        return 27; // 3 rows
    }

    @Override
    protected void setContents() {
        PlayerData data = plugin.getDataManager().getPlayerData(player);

        // Current level item (slot 11)
        ItemStack currentItem = createCurrentLevelItem(data);
        inventory.setItem(11, currentItem);

        // Check if max level
        if (plugin.getLevelManager().isMaxLevel(data.getLevel())) {
            // Max level item (slot 15)
            ItemStack maxItem = createMaxLevelItem();
            inventory.setItem(15, maxItem);
        } else {
            // Next level item (slot 15)
            ItemStack nextItem = createNextLevelItem(data);
            inventory.setItem(15, nextItem);
        }

        // Close button (slot 22)
        inventory.setItem(22, createCloseButton());

        // Fill empty slots
        fillEmptySlots();
    }

    @Override
    public void handleClick(Player player, int slot, ItemStack item, ClickType clickType) {
        if (slot == 15) {
            // Level up button
            PlayerData data = plugin.getDataManager().getPlayerData(player);

            if (plugin.getLevelManager().isMaxLevel(data.getLevel())) {
                playErrorSound();
                player.sendMessage(plugin.getConfigManager().getMessage("level.max-level"));
                return;
            }

            int nextLevel = data.getLevel() + 1;
            double moneyCost = plugin.getLevelManager().calculateLevelCost(nextLevel);
            int gemCost = plugin.getLevelManager().calculateGemCost(nextLevel);

            // Check money
            double balance = plugin.getEconomy().getBalance(player);
            if (balance < moneyCost) {
                playErrorSound();
                player.sendMessage(plugin.getConfigManager()
                        .getMessage("level.not-enough-money", "{cost}", ColorUtil.formatNumber(moneyCost)));
                return;
            }

            // Check gems
            if (gemCost > 0 && data.getGems() < gemCost) {
                playErrorSound();
                player.sendMessage(plugin.getConfigManager()
                        .getMessage("level.not-enough-gems", "{gems}", String.valueOf(gemCost)));
                return;
            }

            // Attempt level up
            if (plugin.getLevelManager().levelUp(data)) {
                int newSlots = plugin.getLevelManager().getCurrentSlots(data);

                // Send messages
                player.sendMessage(plugin.getConfigManager()
                        .getMessage("level.leveled-up", "{level}", String.valueOf(data.getLevel())));
                player.sendMessage(plugin.getConfigManager()
                        .getMessage("level.new-slots", "{slots}", String.valueOf(newSlots)));

                // Play success sound
                playSuccessSound();

                // Refresh GUI
                refresh();
            } else {
                playErrorSound();
            }
        } else if (slot == 22) {
            // Close button
            close();
        }
    }

    private ItemStack createCurrentLevelItem(PlayerData data) {
        String nameFormat = plugin.getConfigManager().getColorizedString("messages.yml", "gui.levelup.current.name");
        String name = ColorUtil.replacePlaceholders(nameFormat, "{level}", String.valueOf(data.getLevel()));

        int currentSlots = plugin.getLevelManager().getCurrentSlots(data);

        List<String> loreFormat = plugin.getConfigManager().getColorizedStringList("messages.yml", "gui.levelup.current.lore");
        List<String> lore = replacePlaceholders(loreFormat,
                "{level}", String.valueOf(data.getLevel()),
                "{slots}", String.valueOf(currentSlots));

        return createItem(Material.EXPERIENCE_BOTTLE, name, lore, true);
    }

    private ItemStack createNextLevelItem(PlayerData data) {
        int nextLevel = data.getLevel() + 1;
        int nextSlots = plugin.getLevelManager().calculateGeneratorSlots(nextLevel);
        double moneyCost = plugin.getLevelManager().calculateLevelCost(nextLevel);
        int gemCost = plugin.getLevelManager().calculateGemCost(nextLevel);

        String nameFormat = plugin.getConfigManager().getColorizedString("messages.yml", "gui.levelup.next.name");
        String name = ColorUtil.replacePlaceholders(nameFormat, "{level}", String.valueOf(nextLevel));

        List<String> loreFormat = plugin.getConfigManager().getColorizedStringList("messages.yml", "gui.levelup.next.lore");
        List<String> lore = replacePlaceholders(loreFormat,
                "{level}", String.valueOf(nextLevel),
                "{slots}", String.valueOf(nextSlots),
                "{money}", ColorUtil.formatNumber(moneyCost),
                "{gems}", gemCost > 0 ? String.valueOf(gemCost) : "0");

        return createItem(Material.NETHER_STAR, name, lore, true);
    }

    private ItemStack createMaxLevelItem() {
        String name = plugin.getConfigManager().getColorizedString("messages.yml", "gui.levelup.max.name");
        List<String> lore = plugin.getConfigManager().getColorizedStringList("messages.yml", "gui.levelup.max.lore");

        return createItem(Material.BEACON, name, lore, true);
    }
}