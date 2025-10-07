package com.pallux.genpvp.guis;

import com.pallux.genpvp.GenPvP;
import com.pallux.genpvp.utils.ColorUtil;
import com.pallux.genpvp.utils.PlayerData;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class RaidShopGUI extends BaseGUI {

    public RaidShopGUI(GenPvP plugin, Player player) {
        super(plugin, player);
    }

    @Override
    protected String getTitle() {
        return plugin.getConfigManager().getMessagesConfig().getString("gui-titles.raid-shop",
                "<gradient:#FF0000:#8B0000>Raid Shop</gradient>");
    }

    @Override
    protected int getSize() {
        return 54;
    }

    @Override
    protected void setContents() {
        // Load items from config
        ConfigurationSection shopConfig = plugin.getConfigManager().getRaidConfig()
                .getConfigurationSection("shop-gui");

        if (shopConfig != null) {
            // Raid Pickaxe
            if (shopConfig.contains("raid_pickaxe")) {
                int slot = shopConfig.getInt("raid_pickaxe.slot", 20);
                inventory.setItem(slot, createShopItem("raid_pickaxe"));
            }

            // Raid Bomb
            if (shopConfig.contains("raid_bomb")) {
                int slot = shopConfig.getInt("raid_bomb.slot", 22);
                inventory.setItem(slot, createShopItem("raid_bomb"));
            }

            // Quickbreak Potion
            if (shopConfig.contains("quickbreak_potion")) {
                int slot = shopConfig.getInt("quickbreak_potion.slot", 24);
                inventory.setItem(slot, createShopItem("quickbreak_potion"));
            }
        }

        // Back button (slot 45)
        inventory.setItem(45, createBackButton());

        // Close button (slot 49)
        inventory.setItem(49, createCloseButton());

        // Fill empty slots
        fillEmptySlots();
    }

    @Override
    public void handleClick(Player player, int slot, ItemStack item, ClickType clickType) {
        if (slot == 45) {
            new CombinedShopGUI(plugin, player).open();
            return;
        }

        if (slot == 49) {
            close();
            return;
        }

        // Check which item was clicked based on slot
        ConfigurationSection shopConfig = plugin.getConfigManager().getRaidConfig()
                .getConfigurationSection("shop-gui");

        if (shopConfig == null) return;

        String itemType = null;

        if (slot == shopConfig.getInt("raid_pickaxe.slot", 20)) {
            itemType = "raid_pickaxe";
        } else if (slot == shopConfig.getInt("raid_bomb.slot", 22)) {
            itemType = "raid_bomb";
        } else if (slot == shopConfig.getInt("quickbreak_potion.slot", 24)) {
            itemType = "quickbreak_potion";
        }

        if (itemType != null) {
            int gemCost = plugin.getRaidManager().getGemCost(itemType);
            purchaseItem(player, itemType, gemCost);
        }
    }

    private ItemStack createShopItem(String itemType) {
        ConfigurationSection shopConfig = plugin.getConfigManager().getRaidConfig()
                .getConfigurationSection("shop-gui." + itemType);
        ConfigurationSection itemConfig = plugin.getConfigManager().getRaidConfig()
                .getConfigurationSection("raid-items." + itemType);

        if (shopConfig == null || itemConfig == null) {
            return createItem(Material.BARRIER, "&#FF0000Error", List.of("&#FF0000Configuration missing"));
        }

        Material material = Material.valueOf(itemConfig.getString("material", "STONE"));
        String name = shopConfig.getString("shop-name", itemConfig.getString("name", "Unknown Item"));
        List<String> lore = shopConfig.getStringList("shop-lore");

        return createItem(material, name, lore, itemConfig.getBoolean("glow", true));
    }

    private void purchaseItem(Player player, String itemType, int gemCost) {
        PlayerData data = plugin.getDataManager().getPlayerData(player);

        // Check gems
        if (data.getGems() < gemCost) {
            playErrorSound();
            player.sendMessage(plugin.getConfigManager()
                    .getMessage("raid-shop.not-enough-gems", "{gems}", String.valueOf(gemCost)));
            return;
        }

        // Check inventory space
        if (player.getInventory().firstEmpty() == -1) {
            playErrorSound();
            player.sendMessage(plugin.getConfigManager().getMessage("shop.inventory-full"));
            return;
        }

        // Deduct gems
        data.removeGems(gemCost);

        // Give item
        ItemStack raidItem = plugin.getRaidManager().createRaidItem(itemType);
        if (raidItem != null) {
            player.getInventory().addItem(raidItem);
        }

        // Send message
        String itemName = plugin.getRaidManager().getRaidItemName(itemType);
        player.sendMessage(plugin.getConfigManager()
                .getMessage("raid-shop.purchased", "{item}", itemName, "{gems}", String.valueOf(gemCost)));

        // Play success sound
        playSuccessSound();

        // Refresh GUI
        refresh();
    }
}