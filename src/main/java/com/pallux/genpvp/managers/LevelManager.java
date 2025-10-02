package com.pallux.genpvp.managers;

import com.pallux.genpvp.GenPvP;
import com.pallux.genpvp.utils.PlayerData;

public class LevelManager {

    private final GenPvP plugin;

    public LevelManager(GenPvP plugin) {
        this.plugin = plugin;
    }

    /**
     * Calculates the cost for a specific level
     */
    public double calculateLevelCost(int level) {
        double baseCost = plugin.getConfigManager().getBaseLevelCost();
        double multiplier = plugin.getConfigManager().getLevelCostMultiplier();

        return baseCost * Math.pow(multiplier, level - 1);
    }

    /**
     * Calculates the gem cost for a specific level
     */
    public int calculateGemCost(int level) {
        int gemsRequiredFrom = plugin.getConfigManager().getGemsRequiredFromLevel();

        if (level < gemsRequiredFrom) {
            return 0;
        }

        int baseGemCost = plugin.getConfigManager().getBaseGemCost();
        double multiplier = plugin.getConfigManager().getGemCostMultiplier();
        int levelsAboveThreshold = level - gemsRequiredFrom;

        return (int) (baseGemCost * Math.pow(multiplier, levelsAboveThreshold));
    }

    /**
     * Calculates the number of generator slots for a specific level
     */
    public int calculateGeneratorSlots(int level) {
        int slotsPerLevel = plugin.getConfigManager().getSlotsPerLevel();
        int reducedFrom = plugin.getConfigManager().getReducedSlotsFromLevel();
        int slotsEveryX = plugin.getConfigManager().getSlotsEveryXLevels();

        if (level < reducedFrom) {
            return level * slotsPerLevel;
        }

        // Base slots from levels before reduction
        int baseSlots = (reducedFrom - 1) * slotsPerLevel;

        // Additional slots from reduced rate
        int levelsAboveThreshold = level - reducedFrom + 1;
        int additionalSlots = (levelsAboveThreshold / slotsEveryX) * slotsPerLevel;

        return baseSlots + additionalSlots;
    }

    /**
     * Checks if a player can level up
     */
    public boolean canLevelUp(PlayerData data) {
        int currentLevel = data.getLevel();
        int maxLevel = plugin.getConfigManager().getMaxLevel();

        // Check if max level reached
        if (maxLevel > 0 && currentLevel >= maxLevel) {
            return false;
        }

        // Check money requirement
        double cost = calculateLevelCost(currentLevel + 1);
        double balance = plugin.getEconomy().getBalance(plugin.getServer().getOfflinePlayer(data.getUuid()));

        if (balance < cost) {
            return false;
        }

        // Check gem requirement
        int gemCost = calculateGemCost(currentLevel + 1);
        if (gemCost > 0 && data.getGems() < gemCost) {
            return false;
        }

        return true;
    }

    /**
     * Attempts to level up a player
     */
    public boolean levelUp(PlayerData data) {
        if (!canLevelUp(data)) {
            return false;
        }

        int currentLevel = data.getLevel();
        int nextLevel = currentLevel + 1;

        // Calculate costs
        double moneyCost = calculateLevelCost(nextLevel);
        int gemCost = calculateGemCost(nextLevel);

        // Withdraw money
        plugin.getEconomy().withdrawPlayer(plugin.getServer().getOfflinePlayer(data.getUuid()), moneyCost);

        // Withdraw gems if required
        if (gemCost > 0) {
            data.removeGems(gemCost);
        }

        // Level up
        data.setLevel(nextLevel);

        return true;
    }

    /**
     * Gets the maximum level from config
     */
    public int getMaxLevel() {
        return plugin.getConfigManager().getMaxLevel();
    }

    /**
     * Checks if a player has reached max level
     */
    public boolean isMaxLevel(int level) {
        int maxLevel = getMaxLevel();
        return maxLevel > 0 && level >= maxLevel;
    }

    /**
     * Gets the next level for a player
     */
    public int getNextLevel(int currentLevel) {
        int maxLevel = getMaxLevel();

        if (maxLevel > 0 && currentLevel >= maxLevel) {
            return currentLevel;
        }

        return currentLevel + 1;
    }

    /**
     * Calculates the current slots a player has
     */
    public int getCurrentSlots(PlayerData data) {
        return calculateGeneratorSlots(data.getLevel());
    }

    /**
     * Calculates the slots the player will have at next level
     */
    public int getNextLevelSlots(PlayerData data) {
        return calculateGeneratorSlots(data.getLevel() + 1);
    }

    /**
     * Checks if player has enough slots to place a generator
     */
    public boolean hasAvailableSlots(PlayerData data) {
        int currentSlots = getCurrentSlots(data);
        int usedSlots = data.getGeneratorsPlaced();

        return usedSlots < currentSlots;
    }

    /**
     * Gets the number of available slots
     */
    public int getAvailableSlots(PlayerData data) {
        int currentSlots = getCurrentSlots(data);
        int usedSlots = data.getGeneratorsPlaced();

        return Math.max(0, currentSlots - usedSlots);
    }

    /**
     * Reloads the level manager
     */
    public void reload() {
        // Nothing specific to reload, configuration is read on-demand
    }
}