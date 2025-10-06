package com.pallux.genpvp.managers;

import com.pallux.genpvp.GenPvP;
import com.pallux.genpvp.utils.ColorUtil;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.Hopper;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Item;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

public class GeneratorManager {

    private final GenPvP plugin;
    private final Map<Integer, GeneratorTier> generatorTiers;
    private BukkitTask generatorTask;

    public GeneratorManager(GenPvP plugin) {
        this.plugin = plugin;
        this.generatorTiers = new HashMap<>();
        loadGeneratorTiers();
    }

    /**
     * Loads all generator tiers from config
     */
    public void loadGeneratorTiers() {
        generatorTiers.clear();

        ConfigurationSection section = plugin.getConfigManager().getGeneratorsConfig()
                .getConfigurationSection("generators");

        if (section == null) {
            plugin.getLogger().severe("No generators configuration found!");
            return;
        }

        for (String key : section.getKeys(false)) {
            try {
                int tier = Integer.parseInt(key);
                ConfigurationSection tierSection = section.getConfigurationSection(key);

                if (tierSection == null) continue;

                Material block = Material.valueOf(tierSection.getString("block", "STONE"));
                int money = tierSection.getInt("money", 1);
                int gems = tierSection.getInt("gems", 1);
                double gemChance = tierSection.getDouble("gem-chance", 0.001);
                int shopPrice = tierSection.getInt("shop-price", 100);
                int upgradeCost = tierSection.getInt("upgrade-cost", 0);
                String displayName = tierSection.getString("display-name", "Generator");
                int customModelData = tierSection.getInt("custom-model-data", 0);

                GeneratorTier generatorTier = new GeneratorTier(
                        tier, block, money, gems, gemChance, shopPrice, upgradeCost, displayName, customModelData
                );

                generatorTiers.put(tier, generatorTier);

            } catch (Exception e) {
                plugin.getLogger().warning("Error loading generator tier " + key + ": " + e.getMessage());
            }
        }

        plugin.getLogger().info("Loaded " + generatorTiers.size() + " generator tiers!");
    }

    /**
     * Gets a generator tier by its number
     */
    public GeneratorTier getGeneratorTier(int tier) {
        return generatorTiers.get(tier);
    }

    /**
     * Gets all generator tiers
     */
    public Map<Integer, GeneratorTier> getAllTiers() {
        return new HashMap<>(generatorTiers);
    }

    /**
     * Gets the maximum tier
     */
    public int getMaxTier() {
        return generatorTiers.keySet().stream().max(Integer::compare).orElse(18);
    }

    /**
     * Checks if a tier exists
     */
    public boolean tierExists(int tier) {
        return generatorTiers.containsKey(tier);
    }

    /**
     * Creates a generator item for a specific tier
     */
    public ItemStack createGeneratorItem(int tier, int amount) {
        GeneratorTier genTier = generatorTiers.get(tier);
        if (genTier == null) {
            return null;
        }

        ItemStack item = new ItemStack(genTier.getBlock(), amount);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(ColorUtil.colorize(genTier.getDisplayName()));

            List<String> lore = new ArrayList<>();
            lore.add(ColorUtil.colorize("&#808080Tier: &#FFFFFF" + tier));
            lore.add(ColorUtil.colorize("&#808080Money: &#00FF00$" + genTier.getMoney() + " &#404040/ 10s"));
            lore.add(ColorUtil.colorize("&#808080Gems: &#DDA0DD" + genTier.getGems() + " &#404040(" + genTier.getGemChance() + "%)"));
            lore.add("");
            lore.add(ColorUtil.colorize("&#FFFF00Place this block to create a generator!"));
            lore.add(ColorUtil.colorize("&#FFFF00Shift + Right-Click to upgrade!"));

            meta.setLore(lore);

            // Add glow effect
            meta.addEnchant(Enchantment.DURABILITY, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);

            // Custom model data if specified
            if (genTier.getCustomModelData() > 0) {
                meta.setCustomModelData(genTier.getCustomModelData());
            }

            item.setItemMeta(meta);
        }

        return item;
    }

    /**
     * Creates a money nugget item
     */
    public ItemStack createMoneyNugget(int amount) {
        ConfigurationSection section = plugin.getConfigManager().getGeneratorsConfig()
                .getConfigurationSection("money-nugget");

        Material material = Material.valueOf(section.getString("material", "IRON_NUGGET"));
        ItemStack item = new ItemStack(material, 1);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            String name = section.getString("name", "<gradient:#2ECC71:#27AE60>{amount} Money</gradient>");
            name = name.replace("{amount}", String.valueOf(amount));
            meta.setDisplayName(ColorUtil.colorize(name));

            List<String> lore = new ArrayList<>();
            for (String line : section.getStringList("lore")) {
                lore.add(ColorUtil.colorize(line));
            }
            meta.setLore(lore);

            if (section.getBoolean("glow", true)) {
                meta.addEnchant(Enchantment.DURABILITY, 1, true);
                meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            }

            int customModelData = section.getInt("custom-model-data", 0);
            if (customModelData > 0) {
                meta.setCustomModelData(customModelData);
            }

            item.setItemMeta(meta);
        }

        return item;
    }

    /**
     * Creates a gem nugget item
     */
    public ItemStack createGemNugget(int amount) {
        ConfigurationSection section = plugin.getConfigManager().getGeneratorsConfig()
                .getConfigurationSection("gem-nugget");

        Material material = Material.valueOf(section.getString("material", "GOLD_NUGGET"));
        ItemStack item = new ItemStack(material, 1);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            String name = section.getString("name", "<gradient:#E74C3C:#9B59B6>{amount} Gem(s)</gradient>");
            name = name.replace("{amount}", String.valueOf(amount));
            meta.setDisplayName(ColorUtil.colorize(name));

            List<String> lore = new ArrayList<>();
            for (String line : section.getStringList("lore")) {
                lore.add(ColorUtil.colorize(line));
            }
            meta.setLore(lore);

            if (section.getBoolean("glow", true)) {
                meta.addEnchant(Enchantment.DURABILITY, 1, true);
                meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            }

            int customModelData = section.getInt("custom-model-data", 0);
            if (customModelData > 0) {
                meta.setCustomModelData(customModelData);
            }

            item.setItemMeta(meta);
        }

        return item;
    }

    /**
     * Checks if an item is a money nugget
     */
    public boolean isMoneyNugget(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;

        ConfigurationSection section = plugin.getConfigManager().getGeneratorsConfig()
                .getConfigurationSection("money-nugget");
        Material material = Material.valueOf(section.getString("material", "IRON_NUGGET"));

        if (item.getType() != material) return false;

        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasDisplayName()) return false;

        String displayName = ChatColor.stripColor(meta.getDisplayName());
        return displayName.contains("Money");
    }

    /**
     * Checks if an item is a gem nugget
     */
    public boolean isGemNugget(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;

        ConfigurationSection section = plugin.getConfigManager().getGeneratorsConfig()
                .getConfigurationSection("gem-nugget");
        Material material = Material.valueOf(section.getString("material", "GOLD_NUGGET"));

        if (item.getType() != material) return false;

        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasDisplayName()) return false;

        String displayName = ChatColor.stripColor(meta.getDisplayName());
        return displayName.contains("Gem");
    }

    /**
     * Extracts the amount from a money nugget
     */
    public int extractMoneyAmount(ItemStack item) {
        if (!isMoneyNugget(item)) return 0;

        String displayName = ChatColor.stripColor(item.getItemMeta().getDisplayName());
        displayName = displayName.replace("$", "").replace("Money", "").trim();

        try {
            return Integer.parseInt(displayName);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /**
     * Extracts the amount from a gem nugget
     */
    public int extractGemAmount(ItemStack item) {
        if (!isGemNugget(item)) return 0;

        String displayName = ChatColor.stripColor(item.getItemMeta().getDisplayName());
        displayName = displayName.replace("Gem(s)", "").replace("Gems", "").replace("Gem", "").trim();

        try {
            return Integer.parseInt(displayName);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /**
     * Starts the generator task that spawns items
     */
    public void startGeneratorTask() {
        int interval = plugin.getConfigManager().getGeneratorInterval() * 20; // Convert to ticks

        generatorTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            Map<Location, Integer> generators = plugin.getDataManager().getAllGenerators();

            for (Map.Entry<Location, Integer> entry : generators.entrySet()) {
                Location location = entry.getKey();
                int tier = entry.getValue();

                if (location.getWorld() == null) continue;

                // Check if chunk is loaded
                if (!location.getWorld().isChunkLoaded(location.getBlockX() >> 4, location.getBlockZ() >> 4)) {
                    continue;
                }

                GeneratorTier genTier = getGeneratorTier(tier);
                if (genTier == null) continue;

                // Find the highest generator in the stack
                Location topLocation = findTopGenerator(location);

                // Check if max items reached at this location
                if (countItemsAtLocation(topLocation) >= plugin.getConfigManager().getMaxItemsPerGenerator()) {
                    continue;
                }

                // Generate money nugget
                spawnNugget(topLocation, createMoneyNugget(genTier.getMoney()), false);

                // Chance to generate gem nugget
                double random = Math.random() * 100;
                if (random < genTier.getGemChance()) {
                    spawnNugget(topLocation, createGemNugget(genTier.getGems()), true);
                }
            }
        }, interval, interval);

        plugin.getLogger().info("Generator task started! Interval: " + plugin.getConfigManager().getGeneratorInterval() + " seconds");
    }

    /**
     * Stops the generator task
     */
    public void stopGeneratorTask() {
        if (generatorTask != null) {
            generatorTask.cancel();
            generatorTask = null;
        }
    }

    /**
     * Finds the topmost generator in a stack
     */
    private Location findTopGenerator(Location location) {
        Location current = location.clone();

        // Check upwards for more generators
        while (true) {
            Location above = current.clone().add(0, 1, 0);
            if (plugin.getDataManager().isGenerator(above)) {
                current = above;
            } else {
                break;
            }
        }

        return current;
    }

    /**
     * Spawns a nugget at a location (with hopper support)
     */
    private void spawnNugget(Location location, ItemStack item, boolean isGem) {
        // Check if there's a hopper above the generator
        Location hopperLocation = location.clone().add(0, 1, 0);
        Block hopperBlock = hopperLocation.getBlock();

        if (hopperBlock.getType() == Material.HOPPER) {
            // Try to add to hopper
            Hopper hopper = (Hopper) hopperBlock.getState();

            // Check if hopper has space
            HashMap<Integer, ItemStack> leftover = hopper.getInventory().addItem(item);

            if (leftover.isEmpty()) {
                // Successfully added to hopper, spawn particles
                if (plugin.getConfigManager().isParticlesEnabled()) {
                    spawnParticles(location, isGem);
                }
                return;
            }
            // If hopper is full, continue to drop item normally
        }

        // No hopper or hopper is full, spawn item as entity
        Location spawnLoc = location.clone().add(0.5, 1.2, 0.5);
        Item droppedItem = location.getWorld().dropItem(spawnLoc, item);
        droppedItem.setVelocity(droppedItem.getVelocity().zero());
        droppedItem.setPickupDelay(0);

        // Spawn particles
        if (plugin.getConfigManager().isParticlesEnabled()) {
            spawnParticles(location, isGem);
        }
    }

    /**
     * Spawns particles for nugget generation
     */
    private void spawnParticles(Location location, boolean isGem) {
        String particleType = isGem ?
                plugin.getConfigManager().getGemsParticleType() :
                plugin.getConfigManager().getMoneyParticleType();
        int particleCount = isGem ?
                plugin.getConfigManager().getGemsParticleCount() :
                plugin.getConfigManager().getMoneyParticleCount();

        try {
            Particle particle = Particle.valueOf(particleType);
            Location spawnLoc = location.clone().add(0.5, 1.2, 0.5);
            location.getWorld().spawnParticle(particle, spawnLoc, particleCount, 0.3, 0.3, 0.3, 0);
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Invalid particle type: " + particleType);
        }
    }

    /**
     * Counts items at a specific location
     */
    private int countItemsAtLocation(Location location) {
        Location center = location.clone().add(0.5, 1, 0.5);
        return (int) location.getWorld().getNearbyEntities(center, 0.5, 1, 0.5)
                .stream()
                .filter(entity -> entity instanceof Item)
                .count();
    }

    /**
     * Reloads the generator manager
     */
    public void reload() {
        stopGeneratorTask();
        loadGeneratorTiers();
        startGeneratorTask();
    }

    /**
     * Inner class representing a generator tier
     */
    public static class GeneratorTier {
        private final int tier;
        private final Material block;
        private final int money;
        private final int gems;
        private final double gemChance;
        private final int shopPrice;
        private final int upgradeCost;
        private final String displayName;
        private final int customModelData;

        public GeneratorTier(int tier, Material block, int money, int gems, double gemChance,
                             int shopPrice, int upgradeCost, String displayName, int customModelData) {
            this.tier = tier;
            this.block = block;
            this.money = money;
            this.gems = gems;
            this.gemChance = gemChance;
            this.shopPrice = shopPrice;
            this.upgradeCost = upgradeCost;
            this.displayName = displayName;
            this.customModelData = customModelData;
        }

        public int getTier() { return tier; }
        public Material getBlock() { return block; }
        public int getMoney() { return money; }
        public int getGems() { return gems; }
        public double getGemChance() { return gemChance; }
        public int getShopPrice() { return shopPrice; }
        public int getUpgradeCost() { return upgradeCost; }
        public String getDisplayName() { return displayName; }
        public int getCustomModelData() { return customModelData; }
    }
}