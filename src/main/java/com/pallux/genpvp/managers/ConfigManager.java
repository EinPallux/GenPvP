package com.pallux.genpvp.managers;

import com.pallux.genpvp.GenPvP;
import com.pallux.genpvp.utils.ColorUtil;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ConfigManager {

    private final GenPvP plugin;
    private final Map<String, FileConfiguration> configs;
    private final Map<String, File> configFiles;

    public ConfigManager(GenPvP plugin) {
        this.plugin = plugin;
        this.configs = new HashMap<>();
        this.configFiles = new HashMap<>();
    }

    /**
     * Loads all configuration files
     */
    public void loadConfigs() {
        // Save default configs if they don't exist
        saveDefaultConfig("config.yml");
        saveDefaultConfig("messages.yml");
        saveDefaultConfig("generators.yml");
        saveDefaultConfig("cubes.yml");
        saveDefaultConfig("armors.yml");
        saveDefaultConfig("defense.yml");
        saveDefaultConfig("raid.yml");

        // Load all configs
        loadConfig("config.yml");
        loadConfig("messages.yml");
        loadConfig("generators.yml");
        loadConfig("cubes.yml");
        loadConfig("armors.yml");
        loadConfig("defense.yml");
        loadConfig("raid.yml");

        plugin.getLogger().info("All configuration files loaded!");
    }

    /**
     * Loads a specific configuration file
     */
    private void loadConfig(String fileName) {
        File file = new File(plugin.getDataFolder(), fileName);

        if (!file.exists()) {
            saveDefaultConfig(fileName);
        }

        FileConfiguration config = YamlConfiguration.loadConfiguration(file);

        // Load defaults
        InputStream defConfigStream = plugin.getResource(fileName);
        if (defConfigStream != null) {
            YamlConfiguration defConfig = YamlConfiguration.loadConfiguration(new InputStreamReader(defConfigStream));
            config.setDefaults(defConfig);
        }

        configs.put(fileName, config);
        configFiles.put(fileName, file);
    }

    /**
     * Saves the default config from resources if it doesn't exist
     */
    private void saveDefaultConfig(String fileName) {
        File file = new File(plugin.getDataFolder(), fileName);

        if (!file.exists()) {
            try {
                plugin.saveResource(fileName, false);
            } catch (IllegalArgumentException e) {
                // File doesn't exist in resources, create empty
                plugin.getLogger().warning("Default " + fileName + " not found in resources, creating empty file.");
            }
        }
    }

    /**
     * Reloads a specific configuration file
     */
    public void reloadConfig(String fileName) {
        File file = configFiles.get(fileName);
        if (file != null && file.exists()) {
            FileConfiguration config = YamlConfiguration.loadConfiguration(file);
            configs.put(fileName, config);
        }
    }

    /**
     * Reloads all configuration files
     */
    public void reloadAllConfigs() {
        for (String fileName : configs.keySet()) {
            reloadConfig(fileName);
        }
    }

    /**
     * Saves a specific configuration file
     */
    public void saveConfig(String fileName) {
        FileConfiguration config = configs.get(fileName);
        File file = configFiles.get(fileName);

        if (config != null && file != null) {
            try {
                config.save(file);
            } catch (IOException e) {
                plugin.getLogger().severe("Could not save " + fileName + "!");
                e.printStackTrace();
            }
        }
    }

    /**
     * Gets a configuration file
     */
    public FileConfiguration getConfig(String fileName) {
        return configs.getOrDefault(fileName, plugin.getConfig());
    }

    /**
     * Gets the main config.yml
     */
    public FileConfiguration getMainConfig() {
        return getConfig("config.yml");
    }

    /**
     * Gets the messages.yml
     */
    public FileConfiguration getMessagesConfig() {
        return getConfig("messages.yml");
    }

    /**
     * Gets the generators.yml
     */
    public FileConfiguration getGeneratorsConfig() {
        return getConfig("generators.yml");
    }

    /**
     * Gets the cubes.yml
     */
    public FileConfiguration getCubesConfig() {
        return getConfig("cubes.yml");
    }

    /**
     * Gets the armors.yml
     */
    public FileConfiguration getArmorsConfig() {
        return getConfig("armors.yml");
    }

    /**
     * Gets the defense.yml
     */
    public FileConfiguration getDefenseConfig() {
        return getConfig("defense.yml");
    }

    /**
     * Gets the raid.yml
     */
    public FileConfiguration getRaidConfig() {
        return getConfig("raid.yml");
    }

    /**
     * Gets a message from messages.yml with color support and placeholder replacement
     */
    public String getMessage(String path, Object... replacements) {
        FileConfiguration messages = getMessagesConfig();
        String message = messages.getString(path, path);

        // Replace prefix placeholder FIRST
        String prefix = messages.getString("prefix", "");
        message = message.replace("{prefix}", prefix);

        // Replace custom placeholders BEFORE colorizing
        if (replacements.length > 0 && replacements.length % 2 == 0) {
            for (int i = 0; i < replacements.length; i += 2) {
                String placeholder = String.valueOf(replacements[i]);
                String value = String.valueOf(replacements[i + 1]);
                message = message.replace(placeholder, value);
            }
        }

        // Colorize AFTER all replacements
        return ColorUtil.colorize(message);
    }

    /**
     * Gets a message from messages.yml without replacements
     */
    public String getMessage(String path) {
        return getMessage(path, new Object[0]);
    }

    // Settings getter methods
    public boolean isHungerDisabled() {
        return getMainConfig().getBoolean("settings.disable-hunger", false);
    }

    public double getDeathMoneyLossPercentage() {
        return getMainConfig().getDouble("settings.death-money-loss-percentage", 0.15);
    }

    public int getGeneratorInterval() {
        return getMainConfig().getInt("settings.generator-interval", 10);
    }

    public int getMaxItemsPerGenerator() {
        return getMainConfig().getInt("settings.max-items-per-generator", 64);
    }

    public boolean isAutoPickupOnBreak() {
        return getMainConfig().getBoolean("settings.auto-pickup-on-break", true);
    }

    public boolean isWorldAllowed(String worldName) {
        var allowedWorlds = getMainConfig().getStringList("settings.allowed-worlds");
        return allowedWorlds.isEmpty() || allowedWorlds.contains(worldName);
    }

    public boolean isDebugEnabled() {
        return getMainConfig().getBoolean("settings.debug", false);
    }

    // Level settings
    public int getStartingLevel() {
        return getMainConfig().getInt("levels.starting-level", 1);
    }

    public int getBaseSlotsAtLevelOne() {
        return getMainConfig().getInt("levels.base-slots-at-level-one", 4);
    }

    public double getBaseXPRequired() {
        return getMainConfig().getDouble("levels.base-xp-required", 100);
    }

    public double getXPMultiplier() {
        return getMainConfig().getDouble("levels.xp-multiplier", 1.5);
    }

    public int getSlotsPerLevel() {
        return getMainConfig().getInt("levels.slots-per-level", 1);
    }

    public int getReducedSlotsFromLevel() {
        return getMainConfig().getInt("levels.reduced-slots-from-level", 20);
    }

    public int getSlotsEveryXLevels() {
        return getMainConfig().getInt("levels.slots-every-x-levels", 5);
    }

    public int getMaxLevel() {
        return getMainConfig().getInt("levels.max-level", 200);
    }

    // GUI settings
    public boolean isFillEmptySlots() {
        return getMainConfig().getBoolean("gui.fill-empty-slots", true);
    }

    public String getFillerMaterial() {
        return getMainConfig().getString("gui.filler-material", "BLACK_STAINED_GLASS_PANE");
    }

    public boolean isSoundsEnabled() {
        return getMainConfig().getBoolean("gui.sounds-enabled", true);
    }

    public String getOpenSound() {
        return getMainConfig().getString("gui.open-sound", "BLOCK_CHEST_OPEN");
    }

    public String getSuccessSound() {
        return getMainConfig().getString("gui.success-sound", "ENTITY_PLAYER_LEVELUP");
    }

    public String getErrorSound() {
        return getMainConfig().getString("gui.error-sound", "ENTITY_VILLAGER_NO");
    }

    // Cube animation settings
    public int getCubeAnimationDuration() {
        return getMainConfig().getInt("cube-animation.duration-ticks", 60);
    }

    public int getCubeAnimationUpdateInterval() {
        return getMainConfig().getInt("cube-animation.update-interval", 2);
    }

    public String getCubeRollSound() {
        return getMainConfig().getString("cube-animation.roll-sound", "BLOCK_NOTE_BLOCK_HAT");
    }

    public String getCubeWinSound() {
        return getMainConfig().getString("cube-animation.win-sound", "ENTITY_PLAYER_LEVELUP");
    }

    // Statistics settings
    public boolean isTrackKills() {
        return getMainConfig().getBoolean("statistics.track-kills", true);
    }

    public boolean isTrackDeaths() {
        return getMainConfig().getBoolean("statistics.track-deaths", true);
    }

    public boolean isTrackBlocksPlaced() {
        return getMainConfig().getBoolean("statistics.track-blocks-placed", true);
    }

    public boolean isTrackBlocksBroken() {
        return getMainConfig().getBoolean("statistics.track-blocks-broken", true);
    }

    public boolean isTrackMoneyCollected() {
        return getMainConfig().getBoolean("statistics.track-money-collected", true);
    }

    public boolean isTrackGemsCollected() {
        return getMainConfig().getBoolean("statistics.track-gems-collected", true);
    }

    public boolean isTrackPlaytime() {
        return getMainConfig().getBoolean("statistics.track-playtime", true);
    }

    // Particle settings
    public boolean isParticlesEnabled() {
        return getGeneratorsConfig().getBoolean("particles.enabled", true);
    }

    public String getMoneyParticleType() {
        return getGeneratorsConfig().getString("particles.money.type", "VILLAGER_HAPPY");
    }

    public int getMoneyParticleCount() {
        return getGeneratorsConfig().getInt("particles.money.count", 5);
    }

    public String getGemsParticleType() {
        return getGeneratorsConfig().getString("particles.gems.type", "DRAGON_BREATH");
    }

    public int getGemsParticleCount() {
        return getGeneratorsConfig().getInt("particles.gems.count", 10);
    }

    // Hopper settings
    public boolean isHopperCollectionEnabled() {
        return getGeneratorsConfig().getBoolean("hopper-collection.enabled", true);
    }

    public boolean isHopperCollectFromStacks() {
        return getGeneratorsConfig().getBoolean("hopper-collection.collect-from-stacks", true);
    }

    // Storage settings
    public String getStorageType() {
        return getMainConfig().getString("storage.type", "YAML");
    }

    public int getAutoSaveInterval() {
        return getMainConfig().getInt("storage.auto-save-interval", 5);
    }

    // Placeholder settings
    public boolean isCompactNumbers() {
        return getMainConfig().getBoolean("placeholders.compact-numbers", true);
    }

    public int getDecimalPlaces() {
        return getMainConfig().getInt("placeholders.decimal-places", 1);
    }

    // Armor settings
    public int getArmorEffectUpdateInterval() {
        return getArmorsConfig().getInt("armor-effects.update-interval", 20);
    }

    public boolean isArmorDebugEnabled() {
        return getArmorsConfig().getBoolean("armor-effects.debug", false);
    }
}