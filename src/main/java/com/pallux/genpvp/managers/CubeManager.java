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

public class CubeManager {

    private final GenPvP plugin;
    private final Map<String, CubeRarity> cubeRarities;
    private final NamespacedKey cubeKey;

    public CubeManager(GenPvP plugin) {
        this.plugin = plugin;
        this.cubeRarities = new HashMap<>();
        this.cubeKey = new NamespacedKey(plugin, "cube_rarity");
        loadCubeRarities();
    }

    /**
     * Loads all cube rarities from config
     */
    public void loadCubeRarities() {
        cubeRarities.clear();

        ConfigurationSection section = plugin.getConfigManager().getCubesConfig()
                .getConfigurationSection("cubes");

        if (section == null) {
            plugin.getLogger().severe("No cubes configuration found!");
            return;
        }

        for (String rarity : section.getKeys(false)) {
            try {
                ConfigurationSection raritySection = section.getConfigurationSection(rarity);
                if (raritySection == null) continue;

                Material material = Material.valueOf(raritySection.getString("material", "WHITE_DYE"));
                String name = raritySection.getString("name", rarity + " Cube");
                List<String> lore = raritySection.getStringList("lore");
                boolean glow = raritySection.getBoolean("glow", true);
                int customModelData = raritySection.getInt("custom-model-data", 0);

                // Load rewards
                List<CubeReward> rewards = new ArrayList<>();
                ConfigurationSection rewardsSection = raritySection.getConfigurationSection("rewards");

                if (rewardsSection != null) {
                    for (String key : rewardsSection.getKeys(false)) {
                        ConfigurationSection rewardSection = rewardsSection.getConfigurationSection(key);
                        if (rewardSection == null) continue;

                        String type = rewardSection.getString("type", "MONEY");
                        double chance = rewardSection.getDouble("chance", 0);
                        String display = rewardSection.getString("display", "Unknown Reward");

                        CubeReward reward = new CubeReward(type, chance, display);

                        // Load type-specific data
                        if (type.equalsIgnoreCase("MONEY")) {
                            reward.setAmount(rewardSection.getInt("amount", 0));
                        } else if (type.equalsIgnoreCase("GEMS")) {
                            reward.setAmount(rewardSection.getInt("amount", 0));
                        } else if (type.equalsIgnoreCase("ITEM")) {
                            reward.setMaterial(Material.valueOf(rewardSection.getString("material", "STONE")));
                            reward.setAmount(rewardSection.getInt("amount", 1));
                        } else if (type.equalsIgnoreCase("COMMAND")) {
                            reward.setCommands(rewardSection.getStringList("commands"));
                        }

                        rewards.add(reward);
                    }
                }

                // Validate chances
                if (plugin.getConfigManager().getCubesConfig().getBoolean("validate-chances", true)) {
                    double totalChance = rewards.stream().mapToDouble(CubeReward::getChance).sum();
                    if (Math.abs(totalChance - 100.0) > 0.1) {
                        plugin.getLogger().warning("Cube rarity '" + rarity + "' has total chance of " +
                                String.format("%.2f", totalChance) + "% instead of 100%!");
                    }
                }

                CubeRarity cubeRarity = new CubeRarity(rarity, material, name, lore, glow, customModelData, rewards);
                cubeRarities.put(rarity.toLowerCase(), cubeRarity);

            } catch (Exception e) {
                plugin.getLogger().warning("Error loading cube rarity " + rarity + ": " + e.getMessage());
                e.printStackTrace();
            }
        }

        plugin.getLogger().info("Loaded " + cubeRarities.size() + " cube rarities!");
    }

    /**
     * Gets a cube rarity by name
     */
    public CubeRarity getCubeRarity(String rarity) {
        return cubeRarities.get(rarity.toLowerCase());
    }

    /**
     * Gets all cube rarities
     */
    public Map<String, CubeRarity> getAllRarities() {
        return new HashMap<>(cubeRarities);
    }

    /**
     * Checks if a rarity exists
     */
    public boolean rarityExists(String rarity) {
        return cubeRarities.containsKey(rarity.toLowerCase());
    }

    /**
     * Creates a cube item for a specific rarity
     */
    public ItemStack createCubeItem(String rarity, int amount) {
        CubeRarity cubeRarity = cubeRarities.get(rarity.toLowerCase());
        if (cubeRarity == null) {
            return null;
        }

        ItemStack item = new ItemStack(cubeRarity.getMaterial(), amount);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(ColorUtil.colorize(cubeRarity.getName()));

            List<String> lore = new ArrayList<>();
            for (String line : cubeRarity.getLore()) {
                lore.add(ColorUtil.colorize(line));
            }

            // Add rewards to lore
            String rewardFormat = plugin.getConfigManager().getCubesConfig()
                    .getString("reward-lore-format", "&#808080• {display} &#404040({chance}%)");

            for (CubeReward reward : cubeRarity.getRewards()) {
                String rewardLine = rewardFormat
                        .replace("{display}", ColorUtil.colorize(reward.getDisplay()))
                        .replace("{chance}", String.format("%.1f", reward.getChance()));
                lore.add(ColorUtil.colorize(rewardLine));
            }

            meta.setLore(lore);

            if (cubeRarity.isGlow()) {
                meta.addEnchant(Enchantment.DURABILITY, 1, true);
                meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            }

            if (cubeRarity.getCustomModelData() > 0) {
                meta.setCustomModelData(cubeRarity.getCustomModelData());
            }

            // Add persistent data to identify cube
            meta.getPersistentDataContainer().set(cubeKey, PersistentDataType.STRING, rarity.toLowerCase());

            item.setItemMeta(meta);
        }

        return item;
    }

    /**
     * Checks if an item is a cube (using NBT tag)
     */
    public boolean isCube(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;

        return meta.getPersistentDataContainer().has(cubeKey, PersistentDataType.STRING);
    }

    /**
     * Gets the rarity of a cube item (using NBT tag)
     */
    public String getCubeRarity(ItemStack item) {
        if (!isCube(item)) return null;

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return null;

        return meta.getPersistentDataContainer().get(cubeKey, PersistentDataType.STRING);
    }

    /**
     * Rolls a reward from a cube
     */
    public CubeReward rollReward(String rarity) {
        CubeRarity cubeRarity = cubeRarities.get(rarity.toLowerCase());
        if (cubeRarity == null || cubeRarity.getRewards().isEmpty()) {
            return null;
        }

        double random = Math.random() * 100;
        double cumulative = 0;

        for (CubeReward reward : cubeRarity.getRewards()) {
            cumulative += reward.getChance();
            if (random <= cumulative) {
                return reward;
            }
        }

        // Fallback to last reward if no match (shouldn't happen with proper chances)
        return cubeRarity.getRewards().get(cubeRarity.getRewards().size() - 1);
    }

    /**
     * Reloads the cube manager
     */
    public void reload() {
        loadCubeRarities();
    }

    /**
     * Inner class representing a cube rarity
     */
    public static class CubeRarity {
        private final String rarity;
        private final Material material;
        private final String name;
        private final List<String> lore;
        private final boolean glow;
        private final int customModelData;
        private final List<CubeReward> rewards;

        public CubeRarity(String rarity, Material material, String name, List<String> lore,
                          boolean glow, int customModelData, List<CubeReward> rewards) {
            this.rarity = rarity;
            this.material = material;
            this.name = name;
            this.lore = lore;
            this.glow = glow;
            this.customModelData = customModelData;
            this.rewards = rewards;
        }

        public String getRarity() { return rarity; }
        public Material getMaterial() { return material; }
        public String getName() { return name; }
        public List<String> getLore() { return lore; }
        public boolean isGlow() { return glow; }
        public int getCustomModelData() { return customModelData; }
        public List<CubeReward> getRewards() { return rewards; }
    }

    /**
     * Inner class representing a cube reward
     */
    public static class CubeReward {
        private final String type;
        private final double chance;
        private final String display;

        private int amount;
        private Material material;
        private List<String> commands;

        public CubeReward(String type, double chance, String display) {
            this.type = type;
            this.chance = chance;
            this.display = display;
            this.commands = new ArrayList<>();
        }

        public String getType() { return type; }
        public double getChance() { return chance; }
        public String getDisplay() { return display; }
        public int getAmount() { return amount; }
        public Material getMaterial() { return material; }
        public List<String> getCommands() { return commands; }

        public void setAmount(int amount) { this.amount = amount; }
        public void setMaterial(Material material) { this.material = material; }
        public void setCommands(List<String> commands) { this.commands = commands; }
    }
}