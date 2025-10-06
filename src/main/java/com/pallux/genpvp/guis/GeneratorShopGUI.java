package com.pallux.genpvp.guis;

import com.pallux.genpvp.GenPvP;
import com.pallux.genpvp.managers.GeneratorManager;
import com.pallux.genpvp.utils.ColorUtil;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class GeneratorShopGUI extends BaseGUI {

    public GeneratorShopGUI(GenPvP plugin, Player player) {
        super(plugin, player);
    }

    @Override
    protected String getTitle() {
        return plugin.getConfigManager().getMessagesConfig().getString("gui-titles.generator-shop",
                "<gradient:#FFD700:#FFA500>Generator Shop</gradient>");
    }

    @Override
    protected int getSize() {
        return 54; // 6 rows for 18 tiers
    }

    @Override
    protected void setContents() {
        boolean hasTierUnlock = player.hasPermission("gpvp.shop.tierunlock");

        // Add all generator tiers
        for (int tier = 1; tier <= plugin.getGeneratorManager().getMaxTier(); tier++) {
            GeneratorManager.GeneratorTier genTier = plugin.getGeneratorManager().getGeneratorTier(tier);
            if (genTier == null) continue;

            int slot = tier - 1 + (tier > 9 ? 9 : 0); // Distribute across rows

            // Check if tier is locked
            boolean isLocked = tier > 1 && !hasTierUnlock;

            ItemStack item;
            if (isLocked) {
                item = createLockedItem(tier);
            } else {
                item = createGeneratorShopItem(genTier);
            }

            inventory.setItem(slot, item);
        }

        // Back button (slot 45)
        inventory.setItem(45, createBackButton());

        // Close button
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

        // Calculate tier from slot
        int tier = slot + 1 - (slot >= 9 ? 9 : 0);
        if (tier < 1 || tier > plugin.getGeneratorManager().getMaxTier()) {
            return;
        }

        // Check if tier is locked
        if (tier > 1 && !player.hasPermission("gpvp.shop.tierunlock")) {
            playErrorSound();
            player.sendMessage(ColorUtil.colorize(plugin.getConfigManager().getMessage("shop.tier-locked")));
            return;
        }

        GeneratorManager.GeneratorTier genTier = plugin.getGeneratorManager().getGeneratorTier(tier);
        if (genTier == null) return;

        // Determine amount to buy based on click type
        int amount = 1;
        if (clickType == ClickType.RIGHT) {
            amount = 8;
        } else if (clickType == ClickType.SHIFT_LEFT || clickType == ClickType.SHIFT_RIGHT) {
            amount = 64;
        }

        // Calculate total cost
        double totalCost = genTier.getShopPrice() * amount;

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

        // Give generator
        ItemStack generator = plugin.getGeneratorManager().createGeneratorItem(tier, amount);
        if (generator != null) {
            player.getInventory().addItem(generator);
        }

        // Send message
        player.sendMessage(ColorUtil.colorize(plugin.getConfigManager()
                .getMessage("shop.purchased",
                        "{amount}", String.valueOf(amount),
                        "{tier}", String.valueOf(tier),
                        "{cost}", ColorUtil.formatNumber(totalCost))));

        // Play success sound
        playSuccessSound();
    }

    private ItemStack createGeneratorShopItem(GeneratorManager.GeneratorTier tier) {
        String nameFormat = plugin.getConfigManager().getMessagesConfig()
                .getString("gui.shop.generator.name", "<gradient:#4ECDC4:#556270>Tier {tier} Generator</gradient>");
        String name = replacePlaceholders(nameFormat, "{tier}", String.valueOf(tier.getTier()));

        List<String> loreFormat = plugin.getConfigManager().getMessagesConfig()
                .getStringList("gui.shop.generator.lore");
        List<String> lore = replacePlaceholders(loreFormat,
                "{tier}", String.valueOf(tier.getTier()),
                "{block}", tier.getBlock().toString().replace("_", " "),
                "{money}", String.valueOf(tier.getMoney()),
                "{gems}", String.valueOf(tier.getGems()),
                "{chance}", String.valueOf(tier.getGemChance()),
                "{price}", ColorUtil.formatNumber(tier.getShopPrice()));

        return createItem(tier.getBlock(), name, lore, true);
    }

    private ItemStack createLockedItem(int tier) {
        String nameFormat = plugin.getConfigManager().getMessagesConfig()
                .getString("gui.shop.locked.name", "<red>Tier {tier} - LOCKED</red>");
        String name = replacePlaceholders(nameFormat, "{tier}", String.valueOf(tier));

        List<String> lore = plugin.getConfigManager().getMessagesConfig()
                .getStringList("gui.shop.locked.lore");

        return createItem(Material.BARRIER, name, lore);
    }
}