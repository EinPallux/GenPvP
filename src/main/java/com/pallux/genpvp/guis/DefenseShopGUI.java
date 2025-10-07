package com.pallux.genpvp.guis;

import com.pallux.genpvp.GenPvP;
import com.pallux.genpvp.managers.DefenseManager;
import com.pallux.genpvp.utils.ColorUtil;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class DefenseShopGUI extends BaseGUI {

    public DefenseShopGUI(GenPvP plugin, Player player) {
        super(plugin, player);
    }

    @Override
    protected String getTitle() {
        return plugin.getConfigManager().getMessagesConfig().getString("gui-titles.defense-shop",
                "<gradient:#808080:#C0C0C0>Defense Shop</gradient>");
    }

    @Override
    protected int getSize() {
        return 54; // 6 rows
    }

    @Override
    protected void setContents() {
        // Add defense tiers 1-6 in slots 10-15
        // Add door (tier 7) in slot 16
        int[] slots = {10, 11, 12, 13, 14, 15, 16};

        for (int i = 0; i < slots.length; i++) {
            int tier = i + 1;
            DefenseManager.DefenseTier defenseTier = plugin.getDefenseManager().getDefenseTier(tier);
            if (defenseTier == null) continue;

            ItemStack item = createDefenseShopItem(defenseTier);
            inventory.setItem(slots[i], item);
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
            // Back to main shop
            new CombinedShopGUI(plugin, player).open();
            return;
        }

        if (slot == 49) {
            // Close button
            close();
            return;
        }

        // Determine which defense tier was clicked
        int[] slots = {10, 11, 12, 13, 14, 15, 16};
        int tier = -1;

        for (int i = 0; i < slots.length; i++) {
            if (slot == slots[i]) {
                tier = i + 1;
                break;
            }
        }

        if (tier == -1 || tier > 7) {
            return;
        }

        DefenseManager.DefenseTier defenseTier = plugin.getDefenseManager().getDefenseTier(tier);
        if (defenseTier == null) return;

        // Determine amount to buy based on click type
        int amount = 1;
        if (clickType == ClickType.RIGHT) {
            amount = 8;
        } else if (clickType == ClickType.SHIFT_LEFT || clickType == ClickType.SHIFT_RIGHT) {
            amount = 16;
        }

        // Calculate total cost
        double totalCost = defenseTier.getPrice() * amount;

        // Check if player has enough money
        double balance = plugin.getEconomy().getBalance(player);
        if (balance < totalCost) {
            playErrorSound();
            player.sendMessage(ColorUtil.colorize(plugin.getConfigManager()
                    .getMessage("shop.not-enough-money", "{cost}", ColorUtil.formatNumber(totalCost))));
            return;
        }

        // Check if inventory has space
        if (player.getInventory().firstEmpty() == -1) {
            playErrorSound();
            player.sendMessage(ColorUtil.colorize(plugin.getConfigManager().getMessage("shop.inventory-full")));
            return;
        }

        // Withdraw money
        plugin.getEconomy().withdrawPlayer(player, totalCost);

        // Give defense block
        ItemStack defenseBlock = plugin.getDefenseManager().createDefenseItem(tier, amount);
        if (defenseBlock != null) {
            player.getInventory().addItem(defenseBlock);
        }

        // Send message
        player.sendMessage(ColorUtil.colorize(plugin.getConfigManager()
                .getMessage("defense.purchased",
                        "{amount}", String.valueOf(amount),
                        "{tier}", String.valueOf(tier),
                        "{cost}", ColorUtil.formatNumber(totalCost))));

        // Play success sound
        playSuccessSound();
    }

    private ItemStack createDefenseShopItem(DefenseManager.DefenseTier tier) {
        // Get the name from the DefenseTier object, which is loaded from defense.yml
        String nameFormat = tier.getShopItemName();

        // Replace placeholders in name
        String name = replacePlaceholders(nameFormat,
                "{tier}", String.valueOf(tier.getTier()),
                "{name}", ColorUtil.stripColor(tier.getDisplayName()),
                "{block}", tier.getBlock().toString().replace("_", " ")
        );

        // Get the lore from the DefenseTier object
        List<String> loreFormat = tier.getShopLore();

        // If shop lore is empty, create default shop lore
        if (loreFormat == null || loreFormat.isEmpty()) {
            loreFormat = new ArrayList<>();
            loreFormat.add("&#808080Tier: &#FFFFFF{tier}");
            loreFormat.add("&#808080Block: &#FFFFFF{block}");
            loreFormat.add("&#808080Hearts: &#EB4034{hearts} ❤");
            loreFormat.add("");
            loreFormat.add("&#FFFF00Price: &#FFD700${price}");
            loreFormat.add("");
            loreFormat.add("&#00FF00Left-Click: Buy 1");
            loreFormat.add("&#00FF00Right-Click: Buy 8");
            loreFormat.add("&#00FF00Shift-Click: Buy 16");
        }

        // Replace placeholders in the lore
        List<String> lore = replacePlaceholders(loreFormat,
                "{tier}", String.valueOf(tier.getTier()),
                "{block}", tier.getBlock().toString().replace("_", " "),
                "{hearts}", String.valueOf(tier.getHearts()),
                "{price}", ColorUtil.formatNumber(tier.getPrice())
        );

        // The createItem method in BaseGUI already handles colorizing the lore,
        // so we just pass the processed list.
        return createItem(tier.getBlock(), name, lore, true);
    }
}