package com.pallux.genpvp.guis;

import com.pallux.genpvp.GenPvP;
import com.pallux.genpvp.utils.ColorUtil;
import com.pallux.genpvp.utils.PlayerData;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.stream.Collectors;

public class LevelUpGUI extends BaseGUI {

    public LevelUpGUI(GenPvP plugin, Player player) {
        super(plugin, player);
    }

    @Override
    protected String getTitle() {
        return ColorUtil.colorize(plugin.getConfigManager().getMessagesConfig().getString("gui-titles.level-up"));
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
            // Level up button (automatic now, just shows info)
            PlayerData data = plugin.getDataManager().getPlayerData(player);

            if (plugin.getLevelManager().isMaxLevel(data.getLevel())) {
                playErrorSound();
                player.sendMessage(plugin.getConfigManager().getMessage("level.max-level"));
                return;
            }

            // Just refresh to show updated progress
            refresh();
        } else if (slot == 22) {
            // Close button
            close();
        }
    }

    private ItemStack createCurrentLevelItem(PlayerData data) {
        String nameFormat = plugin.getConfigManager().getMessagesConfig().getString("gui.levelup.current.name");
        String name = ColorUtil.colorize(replacePlaceholders(nameFormat, "{level}", String.valueOf(data.getLevel())));

        int currentSlots = plugin.getLevelManager().getCurrentSlots(data);
        int currentXP = data.getExperience();
        int nextLevelXP = plugin.getLevelManager().calculateXPRequired(data.getLevel() + 1);
        double progress = plugin.getLevelManager().getXPProgress(data);

        List<String> loreFormat = plugin.getConfigManager().getMessagesConfig().getStringList("gui.levelup.current.lore");
        List<String> lore = replacePlaceholders(loreFormat,
                "{level}", String.valueOf(data.getLevel()),
                "{slots}", String.valueOf(currentSlots),
                "{xp}", String.valueOf(currentXP),
                "{required_xp}", String.valueOf(nextLevelXP),
                "{progress}", String.format("%.1f", progress));

        return createItem(Material.EXPERIENCE_BOTTLE, name, lore.stream().map(ColorUtil::colorize).collect(Collectors.toList()), true);
    }

    private ItemStack createNextLevelItem(PlayerData data) {
        int nextLevel = data.getLevel() + 1;
        int nextSlots = plugin.getLevelManager().calculateGeneratorSlots(nextLevel);
        int xpRequired = plugin.getLevelManager().calculateXPRequired(nextLevel);
        int currentXP = data.getExperience();

        String nameFormat = plugin.getConfigManager().getMessagesConfig().getString("gui.levelup.next.name");
        String name = ColorUtil.colorize(replacePlaceholders(nameFormat, "{level}", String.valueOf(nextLevel)));

        List<String> loreFormat = plugin.getConfigManager().getMessagesConfig().getStringList("gui.levelup.next.lore");
        List<String> lore = replacePlaceholders(loreFormat,
                "{level}", String.valueOf(nextLevel),
                "{slots}", String.valueOf(nextSlots),
                "{xp}", String.valueOf(currentXP),
                "{required_xp}", String.valueOf(xpRequired));

        return createItem(Material.NETHER_STAR, name, lore.stream().map(ColorUtil::colorize).collect(Collectors.toList()), true);
    }

    private ItemStack createMaxLevelItem() {
        String name = ColorUtil.colorize(plugin.getConfigManager().getMessagesConfig().getString("gui.levelup.max.name"));
        List<String> lore = plugin.getConfigManager().getMessagesConfig().getStringList("gui.levelup.max.lore");

        return createItem(Material.BEACON, name, lore.stream().map(ColorUtil::colorize).collect(Collectors.toList()), true);
    }
}