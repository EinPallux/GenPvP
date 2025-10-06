package com.pallux.genpvp.managers;

import com.pallux.genpvp.GenPvP;
import com.pallux.genpvp.utils.PlayerData;

public class LevelManager {

    private final GenPvP plugin;

    public LevelManager(GenPvP plugin) {
        this.plugin = plugin;
    }

    /**
     * Calculates the XP required for a specific level
     */
    public int calculateXPRequired(int level) {
        double baseXP = plugin.getConfigManager().getBaseXPRequired();
        double multiplier = plugin.getConfigManager().getXPMultiplier();

        return (int) (baseXP * Math.pow(multiplier, level - 1));
    }

    /**
     * Calculates the number of generator slots for a specific level
     */
    public int calculateGeneratorSlots(int level) {
        int baseSlotsAtLevelOne = plugin.getConfigManager().getBaseSlotsAtLevelOne();
        int slotsPerLevel = plugin.getConfigManager().getSlotsPerLevel();
        int reducedFrom = plugin.getConfigManager().getReducedSlotsFromLevel();
        int slotsEveryX = plugin.getConfigManager().getSlotsEveryXLevels();

        if (level == 1) {
            return baseSlotsAtLevelOne;
        }

        if (level < reducedFrom) {
            return baseSlotsAtLevelOne + ((level - 1) * slotsPerLevel);
        }

        // Base slots from levels before reduction
        int baseSlots = baseSlotsAtLevelOne + ((reducedFrom - 2) * slotsPerLevel);

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

        // Check XP requirement
        int xpRequired = calculateXPRequired(currentLevel + 1);
        return data.getExperience() >= xpRequired;
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

        // Calculate XP cost
        int xpRequired = calculateXPRequired(nextLevel);

        // Remove XP
        data.removeExperience(xpRequired);

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
     * Gets XP progress percentage for current level
     */
    public double getXPProgress(PlayerData data) {
        if (isMaxLevel(data.getLevel())) {
            return 100.0;
        }

        int currentXP = data.getExperience();
        int requiredXP = calculateXPRequired(data.getLevel() + 1);

        return ((double) currentXP / requiredXP) * 100.0;
    }

    /**
     * Reloads the level manager
     */
    public void reload() {
        // Nothing specific to reload, configuration is read on-demand
    }
}