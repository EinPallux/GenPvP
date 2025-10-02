package com.pallux.genpvp.managers;

import com.pallux.genpvp.GenPvP;
import com.pallux.genpvp.utils.PlayerData;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.stream.Collectors;

public class StatisticsManager {

    private final GenPvP plugin;

    public StatisticsManager(GenPvP plugin) {
        this.plugin = plugin;
    }

    /**
     * Records a player kill
     */
    public void recordKill(Player killer) {
        if (!plugin.getConfigManager().isTrackKills()) return;

        PlayerData data = plugin.getDataManager().getPlayerData(killer);
        data.addKill();
    }

    /**
     * Records a player death
     */
    public void recordDeath(Player victim) {
        if (!plugin.getConfigManager().isTrackDeaths()) return;

        PlayerData data = plugin.getDataManager().getPlayerData(victim);
        data.addDeath();
    }

    /**
     * Records a block placement
     */
    public void recordBlockPlaced(Player player) {
        if (!plugin.getConfigManager().isTrackBlocksPlaced()) return;

        PlayerData data = plugin.getDataManager().getPlayerData(player);
        data.addBlockPlaced();
    }

    /**
     * Records a block break
     */
    public void recordBlockBroken(Player player) {
        if (!plugin.getConfigManager().isTrackBlocksBroken()) return;

        PlayerData data = plugin.getDataManager().getPlayerData(player);
        data.addBlockBroken();
    }

    /**
     * Records money collected from generators
     */
    public void recordMoneyCollected(Player player, double amount) {
        if (!plugin.getConfigManager().isTrackMoneyCollected()) return;

        PlayerData data = plugin.getDataManager().getPlayerData(player);
        data.addMoneyCollected(amount);
    }

    /**
     * Records gems collected from generators
     */
    public void recordGemsCollected(Player player, int amount) {
        if (!plugin.getConfigManager().isTrackGemsCollected()) return;

        PlayerData data = plugin.getDataManager().getPlayerData(player);
        data.addGemsCollected(amount);
    }

    /**
     * Updates player playtime
     */
    public void updatePlaytime(Player player) {
        if (!plugin.getConfigManager().isTrackPlaytime()) return;

        PlayerData data = plugin.getDataManager().getPlayerData(player);
        data.updateSessionPlaytime();
    }

    /**
     * Gets the top players by kills
     */
    public List<Map.Entry<UUID, Integer>> getTopKills(int limit) {
        Map<UUID, Integer> killsMap = new HashMap<>();

        for (PlayerData data : plugin.getDataManager().getAllPlayerData()) {
            killsMap.put(data.getUuid(), data.getKills());
        }

        return killsMap.entrySet().stream()
                .sorted(Map.Entry.<UUID, Integer>comparingByValue().reversed())
                .limit(limit)
                .collect(Collectors.toList());
    }

    /**
     * Gets the top players by level
     */
    public List<Map.Entry<UUID, Integer>> getTopLevels(int limit) {
        Map<UUID, Integer> levelMap = new HashMap<>();

        for (PlayerData data : plugin.getDataManager().getAllPlayerData()) {
            levelMap.put(data.getUuid(), data.getLevel());
        }

        return levelMap.entrySet().stream()
                .sorted(Map.Entry.<UUID, Integer>comparingByValue().reversed())
                .limit(limit)
                .collect(Collectors.toList());
    }

    /**
     * Gets the top players by money collected
     */
    public List<Map.Entry<UUID, Double>> getTopMoneyCollected(int limit) {
        Map<UUID, Double> moneyMap = new HashMap<>();

        for (PlayerData data : plugin.getDataManager().getAllPlayerData()) {
            moneyMap.put(data.getUuid(), data.getMoneyCollected());
        }

        return moneyMap.entrySet().stream()
                .sorted(Map.Entry.<UUID, Double>comparingByValue().reversed())
                .limit(limit)
                .collect(Collectors.toList());
    }

    /**
     * Gets the top players by gems collected
     */
    public List<Map.Entry<UUID, Integer>> getTopGemsCollected(int limit) {
        Map<UUID, Integer> gemsMap = new HashMap<>();

        for (PlayerData data : plugin.getDataManager().getAllPlayerData()) {
            gemsMap.put(data.getUuid(), data.getGemsCollected());
        }

        return gemsMap.entrySet().stream()
                .sorted(Map.Entry.<UUID, Integer>comparingByValue().reversed())
                .limit(limit)
                .collect(Collectors.toList());
    }

    /**
     * Gets the top players by playtime
     */
    public List<Map.Entry<UUID, Long>> getTopPlaytime(int limit) {
        Map<UUID, Long> playtimeMap = new HashMap<>();

        for (PlayerData data : plugin.getDataManager().getAllPlayerData()) {
            playtimeMap.put(data.getUuid(), data.getPlaytime());
        }

        return playtimeMap.entrySet().stream()
                .sorted(Map.Entry.<UUID, Long>comparingByValue().reversed())
                .limit(limit)
                .collect(Collectors.toList());
    }

    /**
     * Gets K/D ratio for a player
     */
    public double getKDR(Player player) {
        PlayerData data = plugin.getDataManager().getPlayerData(player);
        return data.getKDR();
    }

    /**
     * Gets formatted playtime string
     */
    public String getFormattedPlaytime(Player player) {
        PlayerData data = plugin.getDataManager().getPlayerData(player);
        long seconds = data.getPlaytime();

        return formatPlaytime(seconds);
    }

    /**
     * Formats playtime in seconds to readable format
     */
    public String formatPlaytime(long seconds) {
        long days = seconds / 86400;
        long hours = (seconds % 86400) / 3600;
        long minutes = (seconds % 3600) / 60;
        long secs = seconds % 60;

        StringBuilder sb = new StringBuilder();

        if (days > 0) {
            sb.append(days).append("d ");
        }
        if (hours > 0 || days > 0) {
            sb.append(hours).append("h ");
        }
        if (minutes > 0 || hours > 0 || days > 0) {
            sb.append(minutes).append("m ");
        }
        sb.append(secs).append("s");

        return sb.toString().trim();
    }

    /**
     * Resets all statistics for a player
     */
    public void resetStatistics(UUID uuid) {
        PlayerData data = plugin.getDataManager().getPlayerData(uuid);

        data.setKills(0);
        data.setDeaths(0);
        data.setBlocksPlaced(0);
        data.setBlocksBroken(0);
        data.setMoneyCollected(0);
        data.setGemsCollected(0);
        data.setPlaytime(0);
    }

    /**
     * Gets total server statistics
     */
    public Map<String, Object> getServerStats() {
        Map<String, Object> stats = new HashMap<>();

        int totalKills = 0;
        int totalDeaths = 0;
        int totalBlocksPlaced = 0;
        int totalBlocksBroken = 0;
        double totalMoney = 0;
        int totalGems = 0;
        long totalPlaytime = 0;
        int totalPlayers = 0;

        for (PlayerData data : plugin.getDataManager().getAllPlayerData()) {
            totalKills += data.getKills();
            totalDeaths += data.getDeaths();
            totalBlocksPlaced += data.getBlocksPlaced();
            totalBlocksBroken += data.getBlocksBroken();
            totalMoney += data.getMoneyCollected();
            totalGems += data.getGemsCollected();
            totalPlaytime += data.getPlaytime();
            totalPlayers++;
        }

        stats.put("totalKills", totalKills);
        stats.put("totalDeaths", totalDeaths);
        stats.put("totalBlocksPlaced", totalBlocksPlaced);
        stats.put("totalBlocksBroken", totalBlocksBroken);
        stats.put("totalMoneyCollected", totalMoney);
        stats.put("totalGemsCollected", totalGems);
        stats.put("totalPlaytime", totalPlaytime);
        stats.put("totalPlayers", totalPlayers);
        stats.put("totalGenerators", plugin.getDataManager().getAllGenerators().size());

        return stats;
    }
}