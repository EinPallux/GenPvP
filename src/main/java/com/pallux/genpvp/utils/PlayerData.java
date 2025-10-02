package com.pallux.genpvp.utils;

import java.util.UUID;

public class PlayerData {

    private final UUID uuid;

    // Core data
    private int level;
    private int gems;
    private int generatorsPlaced;

    // Statistics
    private int kills;
    private int deaths;
    private int blocksPlaced;
    private int blocksBroken;
    private double moneyCollected;
    private int gemsCollected;
    private long playtime; // in seconds

    // Session data
    private long sessionStart;
    private long firstJoin;
    private long lastJoin;

    public PlayerData(UUID uuid) {
        this.uuid = uuid;
        this.level = 1;
        this.gems = 0;
        this.generatorsPlaced = 0;
        this.kills = 0;
        this.deaths = 0;
        this.blocksPlaced = 0;
        this.blocksBroken = 0;
        this.moneyCollected = 0.0;
        this.gemsCollected = 0;
        this.playtime = 0;
        this.sessionStart = System.currentTimeMillis();
        this.firstJoin = System.currentTimeMillis();
        this.lastJoin = System.currentTimeMillis();
    }

    // UUID getter
    public UUID getUuid() {
        return uuid;
    }

    // Level
    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public void addLevel(int amount) {
        this.level += amount;
    }

    // Gems
    public int getGems() {
        return gems;
    }

    public void setGems(int gems) {
        this.gems = Math.max(0, gems);
    }

    public void addGems(int amount) {
        this.gems += amount;
    }

    public void removeGems(int amount) {
        this.gems = Math.max(0, this.gems - amount);
    }

    public boolean hasGems(int amount) {
        return this.gems >= amount;
    }

    // Generators
    public int getGeneratorsPlaced() {
        return generatorsPlaced;
    }

    public void setGeneratorsPlaced(int generatorsPlaced) {
        this.generatorsPlaced = Math.max(0, generatorsPlaced);
    }

    public void addGenerator() {
        this.generatorsPlaced++;
    }

    public void removeGenerator() {
        this.generatorsPlaced = Math.max(0, this.generatorsPlaced - 1);
    }

    // Kills
    public int getKills() {
        return kills;
    }

    public void setKills(int kills) {
        this.kills = Math.max(0, kills);
    }

    public void addKill() {
        this.kills++;
    }

    // Deaths
    public int getDeaths() {
        return deaths;
    }

    public void setDeaths(int deaths) {
        this.deaths = Math.max(0, deaths);
    }

    public void addDeath() {
        this.deaths++;
    }

    // K/D Ratio
    public double getKDR() {
        if (deaths == 0) {
            return kills;
        }
        return (double) kills / deaths;
    }

    // Blocks Placed
    public int getBlocksPlaced() {
        return blocksPlaced;
    }

    public void setBlocksPlaced(int blocksPlaced) {
        this.blocksPlaced = Math.max(0, blocksPlaced);
    }

    public void addBlockPlaced() {
        this.blocksPlaced++;
    }

    // Blocks Broken
    public int getBlocksBroken() {
        return blocksBroken;
    }

    public void setBlocksBroken(int blocksBroken) {
        this.blocksBroken = Math.max(0, blocksBroken);
    }

    public void addBlockBroken() {
        this.blocksBroken++;
    }

    // Money Collected
    public double getMoneyCollected() {
        return moneyCollected;
    }

    public void setMoneyCollected(double moneyCollected) {
        this.moneyCollected = Math.max(0, moneyCollected);
    }

    public void addMoneyCollected(double amount) {
        this.moneyCollected += amount;
    }

    // Gems Collected
    public int getGemsCollected() {
        return gemsCollected;
    }

    public void setGemsCollected(int gemsCollected) {
        this.gemsCollected = Math.max(0, gemsCollected);
    }

    public void addGemsCollected(int amount) {
        this.gemsCollected += amount;
    }

    // Playtime
    public long getPlaytime() {
        return playtime;
    }

    public void setPlaytime(long playtime) {
        this.playtime = Math.max(0, playtime);
    }

    public void addPlaytime(long seconds) {
        this.playtime += seconds;
    }

    /**
     * Updates playtime based on current session
     */
    public void updateSessionPlaytime() {
        long currentTime = System.currentTimeMillis();
        long sessionDuration = (currentTime - sessionStart) / 1000; // Convert to seconds
        this.playtime += sessionDuration;
        this.sessionStart = currentTime; // Reset session start
    }

    // Session management
    public void startSession() {
        this.sessionStart = System.currentTimeMillis();
    }

    public long getSessionStart() {
        return sessionStart;
    }

    // First join
    public long getFirstJoin() {
        return firstJoin;
    }

    public void setFirstJoin(long firstJoin) {
        this.firstJoin = firstJoin;
    }

    // Last join
    public long getLastJoin() {
        return lastJoin;
    }

    public void setLastJoin(long lastJoin) {
        this.lastJoin = lastJoin;
    }

    /**
     * Updates last join to current time
     */
    public void updateLastJoin() {
        this.lastJoin = System.currentTimeMillis();
    }

    @Override
    public String toString() {
        return "PlayerData{" +
                "uuid=" + uuid +
                ", level=" + level +
                ", gems=" + gems +
                ", generatorsPlaced=" + generatorsPlaced +
                ", kills=" + kills +
                ", deaths=" + deaths +
                ", kdr=" + String.format("%.2f", getKDR()) +
                '}';
    }
}