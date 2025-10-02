package com.pallux.genpvp.managers;

import com.pallux.genpvp.GenPvP;
import com.pallux.genpvp.utils.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class DataManager {

    private final GenPvP plugin;
    private final Map<UUID, PlayerData> playerDataMap;
    private final Map<Location, Integer> generatorLocations; // Location -> Tier
    private File dataFolder;
    private File playersFolder;
    private File generatorsFile;
    private BukkitTask autoSaveTask;

    public DataManager(GenPvP plugin) {
        this.plugin = plugin;
        this.playerDataMap = new HashMap<>();
        this.generatorLocations = new HashMap<>();

        setupFolders();
    }

    private void setupFolders() {
        dataFolder = new File(plugin.getDataFolder(), "data");
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }

        playersFolder = new File(dataFolder, "players");
        if (!playersFolder.exists()) {
            playersFolder.mkdirs();
        }

        generatorsFile = new File(dataFolder, "generators.yml");
        if (!generatorsFile.exists()) {
            try {
                generatorsFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Could not create generators.yml!");
                e.printStackTrace();
            }
        }
    }

    /**
     * Loads all player data and generator locations
     */
    public void loadAllData() {
        loadAllPlayerData();
        loadGeneratorLocations();
        plugin.getLogger().info("All data loaded!");
    }

    /**
     * Loads all player data from files
     */
    private void loadAllPlayerData() {
        File[] files = playersFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files == null) return;

        int loaded = 0;
        for (File file : files) {
            String uuidString = file.getName().replace(".yml", "");
            try {
                UUID uuid = UUID.fromString(uuidString);
                loadPlayerData(uuid);
                loaded++;
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Invalid UUID in file: " + file.getName());
            }
        }

        plugin.getLogger().info("Loaded " + loaded + " player data files!");
    }

    /**
     * Loads player data for a specific player
     */
    public PlayerData loadPlayerData(UUID uuid) {
        if (playerDataMap.containsKey(uuid)) {
            return playerDataMap.get(uuid);
        }

        File playerFile = new File(playersFolder, uuid.toString() + ".yml");
        PlayerData data;

        if (playerFile.exists()) {
            FileConfiguration config = YamlConfiguration.loadConfiguration(playerFile);
            data = new PlayerData(uuid);

            data.setLevel(config.getInt("level", plugin.getConfigManager().getStartingLevel()));
            data.setGems(config.getInt("gems", 0));
            data.setGeneratorsPlaced(config.getInt("generators-placed", 0));

            // Load statistics
            data.setKills(config.getInt("stats.kills", 0));
            data.setDeaths(config.getInt("stats.deaths", 0));
            data.setBlocksPlaced(config.getInt("stats.blocks-placed", 0));
            data.setBlocksBroken(config.getInt("stats.blocks-broken", 0));
            data.setMoneyCollected(config.getDouble("stats.money-collected", 0));
            data.setGemsCollected(config.getInt("stats.gems-collected", 0));
            data.setPlaytime(config.getLong("stats.playtime", 0));
            data.setFirstJoin(config.getLong("first-join", System.currentTimeMillis()));
            data.setLastJoin(config.getLong("last-join", System.currentTimeMillis()));

        } else {
            data = new PlayerData(uuid);
            data.setLevel(plugin.getConfigManager().getStartingLevel());
            data.setFirstJoin(System.currentTimeMillis());
            data.setLastJoin(System.currentTimeMillis());
        }

        playerDataMap.put(uuid, data);
        return data;
    }

    /**
     * Saves player data for a specific player
     */
    public void savePlayerData(UUID uuid) {
        PlayerData data = playerDataMap.get(uuid);
        if (data == null) return;

        File playerFile = new File(playersFolder, uuid.toString() + ".yml");
        FileConfiguration config = new YamlConfiguration();

        config.set("level", data.getLevel());
        config.set("gems", data.getGems());
        config.set("generators-placed", data.getGeneratorsPlaced());

        // Save statistics
        config.set("stats.kills", data.getKills());
        config.set("stats.deaths", data.getDeaths());
        config.set("stats.blocks-placed", data.getBlocksPlaced());
        config.set("stats.blocks-broken", data.getBlocksBroken());
        config.set("stats.money-collected", data.getMoneyCollected());
        config.set("stats.gems-collected", data.getGemsCollected());
        config.set("stats.playtime", data.getPlaytime());
        config.set("first-join", data.getFirstJoin());
        config.set("last-join", data.getLastJoin());

        try {
            config.save(playerFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save player data for " + uuid);
            e.printStackTrace();
        }
    }

    /**
     * Saves all player data
     */
    public void saveAllData() {
        // Save all player data
        for (UUID uuid : playerDataMap.keySet()) {
            savePlayerData(uuid);
        }

        // Save generator locations
        saveGeneratorLocations();

        plugin.getLogger().info("All data saved!");
    }

    /**
     * Gets player data (loads if not cached)
     */
    public PlayerData getPlayerData(UUID uuid) {
        if (!playerDataMap.containsKey(uuid)) {
            return loadPlayerData(uuid);
        }
        return playerDataMap.get(uuid);
    }

    /**
     * Gets player data by player object
     */
    public PlayerData getPlayerData(Player player) {
        return getPlayerData(player.getUniqueId());
    }

    /**
     * Unloads player data from cache (called on logout)
     */
    public void unloadPlayerData(UUID uuid) {
        savePlayerData(uuid);
        playerDataMap.remove(uuid);
    }

    /**
     * Loads generator locations from file
     */
    public void loadGeneratorLocations() {
        generatorLocations.clear();

        if (!generatorsFile.exists()) {
            return;
        }

        FileConfiguration config = YamlConfiguration.loadConfiguration(generatorsFile);
        ConfigurationSection section = config.getConfigurationSection("generators");

        if (section == null) {
            return;
        }

        for (String key : section.getKeys(false)) {
            ConfigurationSection genSection = section.getConfigurationSection(key);
            if (genSection == null) continue;

            try {
                String worldName = genSection.getString("world");
                int x = genSection.getInt("x");
                int y = genSection.getInt("y");
                int z = genSection.getInt("z");
                int tier = genSection.getInt("tier");

                Location location = new Location(
                        Bukkit.getWorld(worldName),
                        x, y, z
                );

                if (location.getWorld() != null) {
                    generatorLocations.put(location, tier);
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Error loading generator at key: " + key);
            }
        }

        plugin.getLogger().info("Loaded " + generatorLocations.size() + " generators!");
    }

    /**
     * Saves generator locations to file
     */
    public void saveGeneratorLocations() {
        FileConfiguration config = new YamlConfiguration();

        int index = 0;
        for (Map.Entry<Location, Integer> entry : generatorLocations.entrySet()) {
            Location loc = entry.getKey();
            int tier = entry.getValue();

            String path = "generators." + index;
            config.set(path + ".world", loc.getWorld().getName());
            config.set(path + ".x", loc.getBlockX());
            config.set(path + ".y", loc.getBlockY());
            config.set(path + ".z", loc.getBlockZ());
            config.set(path + ".tier", tier);

            index++;
        }

        try {
            config.save(generatorsFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save generator locations!");
            e.printStackTrace();
        }
    }

    /**
     * Adds a generator location
     */
    public void addGenerator(Location location, int tier) {
        generatorLocations.put(location, tier);
    }

    /**
     * Removes a generator location
     */
    public void removeGenerator(Location location) {
        generatorLocations.remove(location);
    }

    /**
     * Checks if a location is a generator
     */
    public boolean isGenerator(Location location) {
        return generatorLocations.containsKey(location);
    }

    /**
     * Gets the tier of a generator at a location
     */
    public int getGeneratorTier(Location location) {
        return generatorLocations.getOrDefault(location, 0);
    }

    /**
     * Updates the tier of a generator
     */
    public void updateGeneratorTier(Location location, int tier) {
        if (generatorLocations.containsKey(location)) {
            generatorLocations.put(location, tier);
        }
    }

    /**
     * Gets all generator locations
     */
    public Map<Location, Integer> getAllGenerators() {
        return new HashMap<>(generatorLocations);
    }

    /**
     * Gets the number of generators owned by a player
     */
    public int getPlayerGeneratorCount(UUID uuid) {
        PlayerData data = getPlayerData(uuid);
        return data.getGeneratorsPlaced();
    }

    /**
     * Starts the auto-save task
     */
    public void startAutoSaveTask() {
        int interval = plugin.getConfigManager().getAutoSaveInterval() * 60 * 20; // Convert minutes to ticks

        autoSaveTask = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            plugin.getLogger().info("Auto-saving data...");
            saveAllData();
        }, interval, interval);

        plugin.getLogger().info("Auto-save task started! Interval: " + plugin.getConfigManager().getAutoSaveInterval() + " minutes");
    }

    /**
     * Stops the auto-save task
     */
    public void stopAutoSaveTask() {
        if (autoSaveTask != null) {
            autoSaveTask.cancel();
        }
    }

    /**
     * Gets all loaded player data
     */
    public Collection<PlayerData> getAllPlayerData() {
        return playerDataMap.values();
    }
}