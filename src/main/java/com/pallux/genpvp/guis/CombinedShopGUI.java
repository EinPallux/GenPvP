package com.pallux.genpvp.guis;

import com.pallux.genpvp.GenPvP;
import com.pallux.genpvp.managers.DefenseManager;
import com.pallux.genpvp.managers.GeneratorManager;
import com.pallux.genpvp.utils.ColorUtil;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class CombinedShopGUI extends BaseGUI {

    public CombinedShopGUI(GenPvP plugin, Player player) {
        super(plugin, player);
    }

    @Override
    protected String getTitle() {
        return plugin.getConfigManager().getMessagesConfig().getString("gui-titles.shop",
                "<gradient:#FFD700:#FFA500>Shop</gradient>");
    }

    @Override
    protected int getSize() {
        return 54; // 6 rows
    }

    @Override
    protected void setContents() {
        // Generator Shop Button (slot 20)
        ItemStack generatorShopItem = createItem(Material.FURNACE,
                "<gradient:#4ECDC4:#556270>Generator Shop</gradient>",
                List.of(
                        "&#808080Buy and upgrade generators",
                        "&#808080to earn money passively!",
                        "",
                        "&#00FF00Click to browse!"
                ), true);
        inventory.setItem(20, generatorShopItem);

        // Defense Shop Button (slot 24)
        ItemStack defenseShopItem = createItem(Material.STONE_BRICKS,
                "<gradient:#808080:#C0C0C0>Defense Shop</gradient>",
                List.of(
                        "&#808080Buy defense blocks",
                        "&#808080to protect your base!",
                        "",
                        "&#00FF00Click to browse!"
                ), true);
        inventory.setItem(24, defenseShopItem);

        // Close button (slot 49)
        inventory.setItem(49, createCloseButton());

        // Fill empty slots
        fillEmptySlots();
    }

    @Override
    public void handleClick(Player player, int slot, ItemStack item, ClickType clickType) {
        if (slot == 20) {
            // Open Generator Shop
            new GeneratorShopGUI(plugin, player).open();
            playOpenSound();
        } else if (slot == 24) {
            // Open Defense Shop
            new DefenseShopGUI(plugin, player).open();
            playOpenSound();
        } else if (slot == 49) {
            // Close button
            close();
        }
    }
}