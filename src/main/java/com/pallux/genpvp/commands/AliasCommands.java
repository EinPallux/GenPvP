package com.pallux.genpvp.commands;

import com.pallux.genpvp.GenPvP;
import com.pallux.genpvp.guis.*;
import com.pallux.genpvp.utils.PlayerData;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class AliasCommands implements CommandExecutor {

    private final GenPvP plugin;

    public AliasCommands(GenPvP plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(plugin.getConfigManager().getMessage("player-only"));
            return true;
        }

        Player player = (Player) sender;
        String commandName = command.getName().toLowerCase();

        switch (commandName) {
            case "level":
            case "levelup":
                new LevelUpGUI(plugin, player).open();
                break;

            case "gens":
            case "generators":
                new GeneratorShopGUI(plugin, player).open();
                break;

            case "gems":
                PlayerData data = plugin.getDataManager().getPlayerData(player);
                player.sendMessage(plugin.getConfigManager()
                        .getMessage("stats.gems-balance", "{gems}", String.valueOf(data.getGems())));
                break;

            case "stats":
                new StatsGUI(plugin, player).open();
                break;

            case "armor":
            case "armory":
                new ArmoryGUI(plugin, player).open();
                break;

            case "shop":
                new CombinedShopGUI(plugin, player).open();
                break;

            default:
                return false;
        }

        return true;
    }
}