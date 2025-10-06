package com.pallux.genpvp.managers;

import com.pallux.genpvp.GenPvP;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class DefenseDataManager {

    private final GenPvP plugin;
    private final Map<Location, DefenseBlockData> defenseBlocks;
    private File defenseFile;

    public DefenseDataManager(GenPvP plugin) {
        this.plugin = plugin;
        this.defenseBlocks = new HashMap<>();
        setupFile();
    }

    private void setupFile() {
        File dataFolder = new File(plugin.getDataFolder(), "data");
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }

        defenseFile = new File(dataFolder, "defenses.yml");
        if (!defenseFile.exists()) {
            try {
                defenseFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Could not create defenses.yml!");
                e.printStackTrace();
            }
        }
    }

    public void loadDefenseBlocks() {
        defenseBlocks.clear();

        if (!defenseFile.exists()) {
            return;
        }

        FileConfiguration config = YamlConfiguration.loadConfiguration(defenseFile);
        ConfigurationSection section = config.getConfigurationSection("defenses");

        if (section == null) {
            return;
        }

        for (String key : section.getKeys(false)) {
            ConfigurationSection defSection = section.getConfigurationSection(key);
            if (defSection == null) continue;

            try {
                String worldName = defSection.getString("world");
                int x = defSection.getInt("x");
                int y = defSection.getInt("y");
                int z = defSection.getInt("z");
                int tier = defSection.getInt("tier");
                int currentHearts = defSection.getInt("current-hearts");
                String ownerString = defSection.getString("owner");

                UUID owner = null;
                if (ownerString != null && !ownerString.isEmpty()) {
                    try {
                        owner = UUID.fromString(ownerString);
                    } catch (IllegalArgumentException e) {
                        plugin.getLogger().warning("Invalid owner UUID in defense data: " + ownerString);
                    }
                }

                Location location = new Location(
                        Bukkit.getWorld(worldName),
                        x, y, z
                );

                if (location.getWorld() != null) {
                    defenseBlocks.put(location, new DefenseBlockData(tier, currentHearts, owner));
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Error loading defense block at key: " + key);
                e.printStackTrace();
            }
        }

        plugin.getLogger().info("Loaded " + defenseBlocks.size() + " defense blocks!");
    }

    public void saveDefenseBlocks() {
        FileConfiguration config = new YamlConfiguration();

        int index = 0;
        for (Map.Entry<Location, DefenseBlockData> entry : defenseBlocks.entrySet()) {
            Location loc = entry.getKey();
            DefenseBlockData data = entry.getValue();

            String path = "defenses." + index;
            config.set(path + ".world", loc.getWorld().getName());
            config.set(path + ".x", loc.getBlockX());
            config.set(path + ".y", loc.getBlockY());
            config.set(path + ".z", loc.getBlockZ());
            config.set(path + ".tier", data.getTier());
            config.set(path + ".current-hearts", data.getCurrentHearts());
            config.set(path + ".owner", data.getOwner() != null ? data.getOwner().toString() : "");

            index++;
        }

        try {
            config.save(defenseFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save defense blocks!");
            e.printStackTrace();
        }
    }

    public void addDefenseBlock(Location location, int tier, int hearts, UUID owner) {
        defenseBlocks.put(location, new DefenseBlockData(tier, hearts, owner));
    }

    public void removeDefenseBlock(Location location) {
        defenseBlocks.remove(location);
    }

    public boolean isDefenseBlock(Location location) {
        return defenseBlocks.containsKey(location);
    }

    public DefenseBlockData getDefenseData(Location location) {
        return defenseBlocks.get(location);
    }

    public int getDefenseTier(Location location) {
        DefenseBlockData data = defenseBlocks.get(location);
        return data != null ? data.getTier() : 0;
    }

    public int getCurrentHearts(Location location) {
        DefenseBlockData data = defenseBlocks.get(location);
        return data != null ? data.getCurrentHearts() : 0;
    }

    public void setCurrentHearts(Location location, int hearts) {
        DefenseBlockData data = defenseBlocks.get(location);
        if (data != null) {
            data.setCurrentHearts(hearts);
        }
    }

    public void damageDefenseBlock(Location location, int damage) {
        DefenseBlockData data = defenseBlocks.get(location);
        if (data != null) {
            int newHearts = Math.max(0, data.getCurrentHearts() - damage);
            data.setCurrentHearts(newHearts);
        }
    }

    public UUID getOwner(Location location) {
        DefenseBlockData data = defenseBlocks.get(location);
        return data != null ? data.getOwner() : null;
    }

    public void updateTier(Location location, int newTier, int newHearts) {
        DefenseBlockData data = defenseBlocks.get(location);
        if (data != null) {
            defenseBlocks.put(location, new DefenseBlockData(newTier, newHearts, data.getOwner()));
        }
    }

    public Map<Location, DefenseBlockData> getAllDefenseBlocks() {
        return new HashMap<>(defenseBlocks);
    }

    public static class DefenseBlockData {
        private final int tier;
        private int currentHearts;
        private final UUID owner;

        public DefenseBlockData(int tier, int currentHearts, UUID owner) {
            this.tier = tier;
            this.currentHearts = currentHearts;
            this.owner = owner;
        }

        public int getTier() {
            return tier;
        }

        public int getCurrentHearts() {
            return currentHearts;
        }

        public void setCurrentHearts(int currentHearts) {
            this.currentHearts = currentHearts;
        }

        public UUID getOwner() {
            return owner;
        }
    }
}