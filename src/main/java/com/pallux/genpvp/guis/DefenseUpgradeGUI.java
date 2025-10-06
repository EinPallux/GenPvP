package com.pallux.genpvp.guis;

import com.pallux.genpvp.GenPvP;
import com.pallux.genpvp.managers.DefenseDataManager;
import com.pallux.genpvp.managers.DefenseManager;
import com.pallux.genpvp.utils.ColorUtil;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class DefenseUpgradeGUI extends BaseGUI {

    private final Location location;
    private int currentTier;

    public DefenseUpgradeGUI(GenPvP plugin, Player player, Location location, int currentTier) {
        super(plugin, player);
        this.location = location;
        this.currentTier = currentTier;
    }

    @Override
    protected String getTitle() {
        return plugin.getConfigManager().getMessagesConfig().getString("gui-titles.defense-upgrade",
                "<gradient:#FF6B6B:#4ECDC4>Defense Upgrade</gradient>");
    }

    @Override
    protected int getSize() {
        return 27; // 3 rows
    }

    @Override
    protected void setContents() {
        // Get current tier info
        DefenseManager.DefenseTier current = plugin.getDefenseManager().getDefenseTier(currentTier);
        if (current == null) return;

        // Current tier item (slot 11)
        ItemStack currentItem = createCurrentTierItem(current);
        inventory.setItem(11, currentItem);

        // Check if max tier
        int maxTier = plugin.getDefenseManager().getMaxTier();
        if (currentTier >= maxTier) {
            // Max tier reached item (slot 15)
            ItemStack maxItem = createMaxTierItem();
            inventory.setItem(15, maxItem);
        } else {
            // Next tier item (slot 15)
            DefenseManager.DefenseTier next = plugin.getDefenseManager().getDefenseTier(currentTier + 1);
            if (next != null) {
                ItemStack nextItem = createNextTierItem(next);
                inventory.setItem(15, nextItem);
            }
        }

        // Close button (slot 22)
        inventory.setItem(22, createCloseButton());

        // Fill empty slots
        fillEmptySlots();
    }

    @Override
    public void handleClick(Player player, int slot, ItemStack item, ClickType clickType) {
        if (slot == 15) {
            // Upgrade button
            int maxTier = plugin.getDefenseManager().getMaxTier();
            if (currentTier >= maxTier) {
                playErrorSound();
                player.sendMessage(ColorUtil.colorize(plugin.getConfigManager().getMessage("defense.max-tier")));
                return;
            }

            DefenseManager.DefenseTier next = plugin.getDefenseManager().getDefenseTier(currentTier + 1);
            if (next == null) return;

            // Check if player has enough money
            double balance = plugin.getEconomy().getBalance(player);
            if (balance < next.getUpgradePrice()) {
                playErrorSound();
                player.sendMessage(ColorUtil.colorize(plugin.getConfigManager()
                        .getMessage("defense.not-enough-money", "{amount}",
                                ColorUtil.formatNumber(next.getUpgradePrice()))));
                return;
            }

            // Withdraw money
            plugin.getEconomy().withdrawPlayer(player, next.getUpgradePrice());

            // Upgrade defense block
            plugin.getDefenseDataManager().updateTier(location, currentTier + 1, next.getHearts());
            location.getBlock().setType(next.getBlock());
            currentTier++;

            // Send message
            player.sendMessage(ColorUtil.colorize(plugin.getConfigManager()
                    .getMessage("defense.upgraded", "{tier}", String.valueOf(currentTier))));

            // Play success sound
            playSuccessSound();

            // Refresh GUI
            refresh();
        } else if (slot == 22) {
            // Close button
            close();
        }
    }

    private ItemStack createCurrentTierItem(DefenseManager.DefenseTier tier) {
        String nameFormat = plugin.getConfigManager().getMessagesConfig()
                .getString("gui.defense-upgrade.current-tier.name", "<gradient:#FFD700:#FFA500>Current: Tier {tier}</gradient>");
        String name = replacePlaceholders(nameFormat, "{tier}", String.valueOf(tier.getTier()));

        DefenseDataManager.DefenseBlockData data = plugin.getDefenseDataManager().getDefenseData(location);
        int currentHearts = data != null ? data.getCurrentHearts() : tier.getHearts();

        List<String> loreFormat = plugin.getConfigManager().getMessagesConfig()
                .getStringList("gui.defense-upgrade.current-tier.lore");
        List<String> lore = replacePlaceholders(loreFormat,
                "{hearts}", String.valueOf(tier.getHearts()),
                "{current_hearts}", String.valueOf(currentHearts));

        return createItem(tier.getBlock(), name, lore, true);
    }

    private ItemStack createNextTierItem(DefenseManager.DefenseTier tier) {
        String nameFormat = plugin.getConfigManager().getMessagesConfig()
                .getString("gui.defense-upgrade.next-tier.name", "<gradient:#2ECC71:#27AE60>Upgrade to Tier {tier}</gradient>");
        String name = replacePlaceholders(nameFormat, "{tier}", String.valueOf(tier.getTier()));

        List<String> loreFormat = plugin.getConfigManager().getMessagesConfig()
                .getStringList("gui.defense-upgrade.next-tier.lore");
        List<String> lore = replacePlaceholders(loreFormat,
                "{hearts}", String.valueOf(tier.getHearts()),
                "{cost}", ColorUtil.formatNumber(tier.getUpgradePrice()));

        return createItem(tier.getBlock(), name, lore, true);
    }

    private ItemStack createMaxTierItem() {
        String name = plugin.getConfigManager().getMessagesConfig()
                .getString("gui.defense-upgrade.max-tier.name", "<gradient:#FF1493:#9400D3>Maximum Tier Reached!</gradient>");

        List<String> lore = plugin.getConfigManager().getMessagesConfig()
                .getStringList("gui.defense-upgrade.max-tier.lore");

        return createItem(Material.NETHER_STAR, name, lore, true);
    }
}