package com.pallux.genpvp.managers;

import com.pallux.genpvp.GenPvP;
import com.pallux.genpvp.utils.ColorUtil;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;

public class DefenseManager {

    private final GenPvP plugin;
    private final Map<Integer, DefenseTier> defenseTiers;
    private final NamespacedKey defenseKey;
    private final NamespacedKey defenseTierKey;

    public DefenseManager(GenPvP plugin) {
        this.plugin = plugin;
        this.defenseTiers = new HashMap<>();
        this.defenseKey = new NamespacedKey(plugin, "defense_block");
        this.defenseTierKey = new NamespacedKey(plugin, "defense_tier");
        loadDefenseTiers();
    }

    public void loadDefenseTiers() {
        defenseTiers.clear();

        ConfigurationSection section = plugin.getConfigManager().getDefenseConfig()
                .getConfigurationSection("defense-blocks");

        if (section == null) {
            plugin.getLogger().severe("No defense blocks configuration found!");
            return;
        }

        for (String key : section.getKeys(false)) {
            try {
                int tier = Integer.parseInt(key);
                ConfigurationSection tierSection = section.getConfigurationSection(key);

                if (tierSection == null) continue;

                Material block = Material.valueOf(tierSection.getString("block", "STONE"));
                int hearts = tierSection.getInt("hearts", 100);
                double price = tierSection.getDouble("price", 1000);
                double upgradePrice = tierSection.getDouble("upgrade-price", 0);
                String displayName = tierSection.getString("display-name", "Defense Block");
                boolean isDoor = tierSection.getBoolean("is-door", false);

                DefenseTier defenseTier = new DefenseTier(
                        tier, block, hearts, price, upgradePrice, displayName, isDoor
                );

                defenseTiers.put(tier, defenseTier);

            } catch (Exception e) {
                plugin.getLogger().warning("Error loading defense tier " + key + ": " + e.getMessage());
                e.printStackTrace();
            }
        }

        plugin.getLogger().info("Loaded " + defenseTiers.size() + " defense block tiers!");
    }

    public DefenseTier getDefenseTier(int tier) {
        return defenseTiers.get(tier);
    }

    public Map<Integer, DefenseTier> getAllTiers() {
        return new HashMap<>(defenseTiers);
    }

    public int getMaxTier() {
        return defenseTiers.keySet().stream()
                .filter(tier -> !defenseTiers.get(tier).isDoor())
                .max(Integer::compare)
                .orElse(6);
    }

    public boolean tierExists(int tier) {
        return defenseTiers.containsKey(tier);
    }

    public ItemStack createDefenseItem(int tier, int amount) {
        DefenseTier defenseTier = defenseTiers.get(tier);
        if (defenseTier == null) {
            return null;
        }

        ItemStack item = new ItemStack(defenseTier.getBlock(), amount);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(ColorUtil.colorize(defenseTier.getDisplayName()));

            List<String> lore = new ArrayList<>();
            lore.add(ColorUtil.colorize("&#808080Tier: &#FFFFFF" + tier));
            lore.add(ColorUtil.colorize("&#808080Hearts: &#FF0000" + defenseTier.getHearts() + " ❤"));

            if (defenseTier.isDoor()) {
                lore.add("");
                lore.add(ColorUtil.colorize("&#FFFF00Right-Click to open (Owner only)"));
            }

            lore.add("");
            lore.add(ColorUtil.colorize("&#00FF00Place to defend your base!"));

            if (!defenseTier.isDoor() && tier < getMaxTier()) {
                lore.add(ColorUtil.colorize("&#FFFF00Shift + Right-Click to upgrade!"));
            }

            meta.setLore(lore);

            // Add glow effect
            meta.addEnchant(Enchantment.DURABILITY, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);

            // Add persistent data to identify defense blocks
            meta.getPersistentDataContainer().set(defenseKey, PersistentDataType.INTEGER, tier);

            item.setItemMeta(meta);
        }

        return item;
    }

    public boolean isDefenseBlock(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;

        return meta.getPersistentDataContainer().has(defenseKey, PersistentDataType.INTEGER);
    }

    public int getDefenseTier(ItemStack item) {
        if (!isDefenseBlock(item)) return 0;

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return 0;

        Integer tier = meta.getPersistentDataContainer().get(defenseKey, PersistentDataType.INTEGER);
        return tier != null ? tier : 0;
    }

    public void reload() {
        loadDefenseTiers();
    }

    public static class DefenseTier {
        private final int tier;
        private final Material block;
        private final int hearts;
        private final double price;
        private final double upgradePrice;
        private final String displayName;
        private final boolean isDoor;

        public DefenseTier(int tier, Material block, int hearts, double price,
                           double upgradePrice, String displayName, boolean isDoor) {
            this.tier = tier;
            this.block = block;
            this.hearts = hearts;
            this.price = price;
            this.upgradePrice = upgradePrice;
            this.displayName = displayName;
            this.isDoor = isDoor;
        }

        public int getTier() { return tier; }
        public Material getBlock() { return block; }
        public int getHearts() { return hearts; }
        public double getPrice() { return price; }
        public double getUpgradePrice() { return upgradePrice; }
        public String getDisplayName() { return displayName; }
        public boolean isDoor() { return isDoor; }
    }
}