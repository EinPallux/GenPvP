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
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.List;

public class RaidManager {

    private final GenPvP plugin;
    private final NamespacedKey raidItemKey;

    public RaidManager(GenPvP plugin) {
        this.plugin = plugin;
        this.raidItemKey = new NamespacedKey(plugin, "raid_item");
    }

    public ItemStack createRaidItem(String type) {
        switch (type.toLowerCase()) {
            case "raid_pickaxe":
                return createRaidPickaxe();
            case "raid_bomb":
                return createRaidBomb();
            case "quickbreak_potion":
                return createQuickbreakPotion();
            default:
                return null;
        }
    }

    private ItemStack createRaidPickaxe() {
        ConfigurationSection config = plugin.getConfigManager().getRaidConfig()
                .getConfigurationSection("raid-items.raid_pickaxe");

        Material material = Material.valueOf(config.getString("material", "GOLDEN_PICKAXE"));
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            String name = config.getString("name", "<gradient:#FFD700:#FF8C00>Raid Pickaxe</gradient>");
            meta.setDisplayName(ColorUtil.colorize(name));

            List<String> lore = new ArrayList<>();
            for (String line : config.getStringList("lore")) {
                lore.add(ColorUtil.colorize(line));
            }
            meta.setLore(lore);

            // Set durability to 1 (using Damageable interface)
            if (meta instanceof org.bukkit.inventory.meta.Damageable) {
                org.bukkit.inventory.meta.Damageable damageable = (org.bukkit.inventory.meta.Damageable) meta;
                damageable.setDamage(item.getType().getMaxDurability() - 1);
            }

            // Add glow if configured
            if (config.getBoolean("glow", true)) {
                meta.addEnchant(Enchantment.DURABILITY, 1, true);
                meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            }

            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);

            // Add persistent data
            meta.getPersistentDataContainer().set(raidItemKey, PersistentDataType.STRING, "raid_pickaxe");

            item.setItemMeta(meta);
        }

        return item;
    }

    private ItemStack createRaidBomb() {
        ConfigurationSection config = plugin.getConfigManager().getRaidConfig()
                .getConfigurationSection("raid-items.raid_bomb");

        Material material = Material.valueOf(config.getString("material", "FIRE_CHARGE"));
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            String name = config.getString("name", "<gradient:#FF4500:#FF6347>Raid Bomb</gradient>");
            meta.setDisplayName(ColorUtil.colorize(name));

            List<String> lore = new ArrayList<>();
            for (String line : config.getStringList("lore")) {
                lore.add(ColorUtil.colorize(line));
            }
            meta.setLore(lore);

            // Add glow if configured
            if (config.getBoolean("glow", true)) {
                meta.addEnchant(Enchantment.DURABILITY, 1, true);
                meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            }

            // Add persistent data
            meta.getPersistentDataContainer().set(raidItemKey, PersistentDataType.STRING, "raid_bomb");

            item.setItemMeta(meta);
        }

        return item;
    }

    private ItemStack createQuickbreakPotion() {
        ConfigurationSection config = plugin.getConfigManager().getRaidConfig()
                .getConfigurationSection("raid-items.quickbreak_potion");

        Material material = Material.valueOf(config.getString("material", "POTION"));
        ItemStack item = new ItemStack(material);
        PotionMeta meta = (PotionMeta) item.getItemMeta();

        if (meta != null) {
            String name = config.getString("name", "<gradient:#87CEEB:#4682B4>Potion of Quickbreak</gradient>");
            meta.setDisplayName(ColorUtil.colorize(name));

            List<String> lore = new ArrayList<>();
            for (String line : config.getStringList("lore")) {
                lore.add(ColorUtil.colorize(line));
            }
            meta.setLore(lore);

            // Add potion effect from config
            String effectName = config.getString("effect", "FAST_DIGGING");
            int effectLevel = config.getInt("effect-level", 2);
            int duration = config.getInt("duration", 6000);

            try {
                PotionEffectType effectType = PotionEffectType.getByName(effectName);
                if (effectType != null) {
                    meta.addCustomEffect(new PotionEffect(effectType, duration, effectLevel - 1), true);
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Invalid potion effect: " + effectName);
            }

            // Add glow if configured
            if (config.getBoolean("glow", true)) {
                meta.addEnchant(Enchantment.DURABILITY, 1, true);
                meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            }

            meta.addItemFlags(ItemFlag.HIDE_POTION_EFFECTS);

            // Add persistent data
            meta.getPersistentDataContainer().set(raidItemKey, PersistentDataType.STRING, "quickbreak_potion");

            item.setItemMeta(meta);
        }

        return item;
    }

    public boolean isRaidItem(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        return meta != null && meta.getPersistentDataContainer().has(raidItemKey, PersistentDataType.STRING);
    }

    public String getRaidItemType(ItemStack item) {
        if (!isRaidItem(item)) return null;
        ItemMeta meta = item.getItemMeta();
        return meta.getPersistentDataContainer().get(raidItemKey, PersistentDataType.STRING);
    }

    public String getRaidItemName(String type) {
        ConfigurationSection config = plugin.getConfigManager().getRaidConfig()
                .getConfigurationSection("raid-items." + type);

        if (config != null) {
            String name = config.getString("name", "Unknown Item");
            return ColorUtil.stripColor(ColorUtil.colorize(name));
        }

        return "Unknown Item";
    }

    public int getGemCost(String type) {
        return plugin.getConfigManager().getRaidConfig()
                .getInt("raid-items." + type + ".gem-cost", 0);
    }
}