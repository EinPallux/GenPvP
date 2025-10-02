package com.pallux.genpvp.listeners;

import com.pallux.genpvp.GenPvP;
import com.pallux.genpvp.utils.ColorUtil;
import com.pallux.genpvp.utils.PlayerData;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerListener implements Listener {

    private final GenPvP plugin;

    public PlayerListener(GenPvP plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        // Load player data
        PlayerData data = plugin.getDataManager().loadPlayerData(player.getUniqueId());

        // Update last join time
        data.updateLastJoin();

        // Start session for playtime tracking
        data.startSession();

        // Debug message
        if (plugin.getConfigManager().isDebugEnabled()) {
            plugin.getLogger().info("Loaded data for player: " + player.getName() +
                    " - Level: " + data.getLevel() + ", Gems: " + data.getGems());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        PlayerData data = plugin.getDataManager().getPlayerData(player);

        // Update playtime before saving
        data.updateSessionPlaytime();

        // Save and unload player data
        plugin.getDataManager().unloadPlayerData(player.getUniqueId());

        // Debug message
        if (plugin.getConfigManager().isDebugEnabled()) {
            plugin.getLogger().info("Saved and unloaded data for player: " + player.getName());
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        Player killer = victim.getKiller();

        // Record death statistic
        plugin.getStatisticsManager().recordDeath(victim);

        // Only handle PvP deaths
        if (killer == null || killer.equals(victim)) {
            return;
        }

        // Record kill statistic
        plugin.getStatisticsManager().recordKill(killer);

        // Get victim's balance
        double balance = plugin.getEconomy().getBalance(victim);

        if (balance <= 0) {
            return;
        }

        // Calculate money to lose
        double lossPercentage = plugin.getConfigManager().getDeathMoneyLossPercentage();
        double moneyLost = balance * lossPercentage;

        if (moneyLost <= 0) {
            return;
        }

        // Remove money from victim
        plugin.getEconomy().withdrawPlayer(victim, moneyLost);

        // Give money to killer
        plugin.getEconomy().depositPlayer(killer, moneyLost);

        // Send messages
        victim.sendMessage(plugin.getConfigManager()
                .getMessage("death.money-lost",
                        "{amount}", ColorUtil.formatNumber(moneyLost),
                        "{percentage}", String.format("%.0f", lossPercentage * 100)));

        killer.sendMessage(plugin.getConfigManager()
                .getMessage("death.money-stolen",
                        "{amount}", ColorUtil.formatNumber(moneyLost),
                        "{player}", victim.getName()));
    }
}