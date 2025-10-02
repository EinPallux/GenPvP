package com.pallux.genpvp.commands;

import com.pallux.genpvp.GenPvP;
import com.pallux.genpvp.guis.*;
import com.pallux.genpvp.utils.ColorUtil;
import com.pallux.genpvp.utils.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class GenPvPCommand implements CommandExecutor, TabCompleter {

    private final GenPvP plugin;

    public GenPvPCommand(GenPvP plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "gen":
                return handleGenCommand(sender, args);

            case "cube":
                return handleCubeCommand(sender, args);

            case "armor":
                return handleArmorCommand(sender, args);

            case "level":
                return handleLevelCommand(sender, args);

            case "shop":
                return handleShopCommand(sender, args);

            case "armory":
                return handleArmoryCommand(sender, args);

            case "gems":
                return handleGemsCommand(sender, args);

            case "stats":
                return handleStatsCommand(sender, args);

            case "reload":
                return handleReloadCommand(sender, args);

            default:
                sendHelp(sender);
                return true;
        }
    }

    private boolean handleGenCommand(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(plugin.getConfigManager().getMessage("prefix") +
                    " &#FFFF00Usage: /gpvp gen <give|giveall> <tier> <amount> [player]");
            return true;
        }

        String action = args[1].toLowerCase();

        if (action.equals("give")) {
            if (!sender.hasPermission("gpvp.gen.give")) {
                sender.sendMessage(plugin.getConfigManager().getMessage("no-permission"));
                return true;
            }

            if (args.length < 5) {
                sender.sendMessage(plugin.getConfigManager().getMessage("prefix") +
                        " &#FFFF00Usage: /gpvp gen give <player> <tier> <amount>");
                return true;
            }

            Player target = Bukkit.getPlayer(args[2]);
            if (target == null) {
                sender.sendMessage(plugin.getConfigManager()
                        .getMessage("player-not-found", "{player}", args[2]));
                return true;
            }

            int tier;
            int amount;

            try {
                tier = Integer.parseInt(args[3]);
                amount = Integer.parseInt(args[4]);
            } catch (NumberFormatException e) {
                sender.sendMessage(plugin.getConfigManager().getMessage("invalid-amount"));
                return true;
            }

            if (!plugin.getGeneratorManager().tierExists(tier)) {
                sender.sendMessage(plugin.getConfigManager().getMessage("invalid-tier"));
                return true;
            }

            ItemStack generator = plugin.getGeneratorManager().createGeneratorItem(tier, amount);
            if (generator != null) {
                target.getInventory().addItem(generator);

                sender.sendMessage(plugin.getConfigManager()
                        .getMessage("generator.given", "{amount}", String.valueOf(amount),
                                "{tier}", String.valueOf(tier), "{player}", target.getName()));

                target.sendMessage(plugin.getConfigManager()
                        .getMessage("generator.received", "{amount}", String.valueOf(amount),
                                "{tier}", String.valueOf(tier)));
            }

            return true;
        }

        if (action.equals("giveall")) {
            if (!sender.hasPermission("gpvp.gen.giveall")) {
                sender.sendMessage(plugin.getConfigManager().getMessage("no-permission"));
                return true;
            }

            if (args.length < 4) {
                sender.sendMessage(plugin.getConfigManager().getMessage("prefix") +
                        " &#FFFF00Usage: /gpvp gen giveall <tier> <amount>");
                return true;
            }

            int tier;
            int amount;

            try {
                tier = Integer.parseInt(args[2]);
                amount = Integer.parseInt(args[3]);
            } catch (NumberFormatException e) {
                sender.sendMessage(plugin.getConfigManager().getMessage("invalid-amount"));
                return true;
            }

            if (!plugin.getGeneratorManager().tierExists(tier)) {
                sender.sendMessage(plugin.getConfigManager().getMessage("invalid-tier"));
                return true;
            }

            ItemStack generator = plugin.getGeneratorManager().createGeneratorItem(tier, amount);
            if (generator != null) {
                for (Player online : Bukkit.getOnlinePlayers()) {
                    online.getInventory().addItem(generator.clone());
                    online.sendMessage(plugin.getConfigManager()
                            .getMessage("generator.received", "{amount}", String.valueOf(amount),
                                    "{tier}", String.valueOf(tier)));
                }

                sender.sendMessage(plugin.getConfigManager()
                        .getMessage("generator.given-all", "{amount}", String.valueOf(amount),
                                "{tier}", String.valueOf(tier)));
            }

            return true;
        }

        return true;
    }

    private boolean handleCubeCommand(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(plugin.getConfigManager().getMessage("prefix") +
                    " &#FFFF00Usage: /gpvp cube <give|giveall> <rarity> <amount> [player]");
            return true;
        }

        String action = args[1].toLowerCase();

        if (action.equals("give")) {
            if (!sender.hasPermission("gpvp.cube.give")) {
                sender.sendMessage(plugin.getConfigManager().getMessage("no-permission"));
                return true;
            }

            if (args.length < 5) {
                sender.sendMessage(plugin.getConfigManager().getMessage("prefix") +
                        " &#FFFF00Usage: /gpvp cube give <player> <rarity> <amount>");
                return true;
            }

            Player target = Bukkit.getPlayer(args[2]);
            if (target == null) {
                sender.sendMessage(plugin.getConfigManager()
                        .getMessage("player-not-found", "{player}", args[2]));
                return true;
            }

            String rarity = args[3].toLowerCase();
            int amount;

            try {
                amount = Integer.parseInt(args[4]);
            } catch (NumberFormatException e) {
                sender.sendMessage(plugin.getConfigManager().getMessage("invalid-amount"));
                return true;
            }

            if (!plugin.getCubeManager().rarityExists(rarity)) {
                sender.sendMessage(plugin.getConfigManager().getMessage("invalid-rarity"));
                return true;
            }

            ItemStack cube = plugin.getCubeManager().createCubeItem(rarity, amount);
            if (cube != null) {
                target.getInventory().addItem(cube);

                sender.sendMessage(plugin.getConfigManager()
                        .getMessage("cube.given", "{amount}", String.valueOf(amount),
                                "{rarity}", rarity, "{player}", target.getName()));

                target.sendMessage(plugin.getConfigManager()
                        .getMessage("cube.received", "{amount}", String.valueOf(amount),
                                "{rarity}", rarity));
            }

            return true;
        }

        if (action.equals("giveall")) {
            if (!sender.hasPermission("gpvp.cube.giveall")) {
                sender.sendMessage(plugin.getConfigManager().getMessage("no-permission"));
                return true;
            }

            if (args.length < 4) {
                sender.sendMessage(plugin.getConfigManager().getMessage("prefix") +
                        " &#FFFF00Usage: /gpvp cube giveall <rarity> <amount>");
                return true;
            }

            String rarity = args[2].toLowerCase();
            int amount;

            try {
                amount = Integer.parseInt(args[3]);
            } catch (NumberFormatException e) {
                sender.sendMessage(plugin.getConfigManager().getMessage("invalid-amount"));
                return true;
            }

            if (!plugin.getCubeManager().rarityExists(rarity)) {
                sender.sendMessage(plugin.getConfigManager().getMessage("invalid-rarity"));
                return true;
            }

            ItemStack cube = plugin.getCubeManager().createCubeItem(rarity, amount);
            if (cube != null) {
                for (Player online : Bukkit.getOnlinePlayers()) {
                    online.getInventory().addItem(cube.clone());
                    online.sendMessage(plugin.getConfigManager()
                            .getMessage("cube.received", "{amount}", String.valueOf(amount),
                                    "{rarity}", rarity));
                }

                sender.sendMessage(plugin.getConfigManager()
                        .getMessage("cube.given-all", "{amount}", String.valueOf(amount),
                                "{rarity}", rarity));
            }

            return true;
        }

        return true;
    }

    private boolean handleArmorCommand(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(plugin.getConfigManager().getMessage("prefix") +
                    " &#FFFF00Usage: /gpvp armor give <player> <armor_piece>");
            return true;
        }

        String action = args[1].toLowerCase();

        if (action.equals("give")) {
            if (!sender.hasPermission("gpvp.armor.give")) {
                sender.sendMessage(plugin.getConfigManager().getMessage("no-permission"));
                return true;
            }

            if (args.length < 4) {
                sender.sendMessage(plugin.getConfigManager().getMessage("prefix") +
                        " &#FFFF00Usage: /gpvp armor give <player> <armor_piece>");
                sender.sendMessage(plugin.getConfigManager().getMessage("prefix") +
                        " &#808080Example: /gpvp armor give Pallux blaze_helmet");
                return true;
            }

            Player target = Bukkit.getPlayer(args[2]);
            if (target == null) {
                sender.sendMessage(plugin.getConfigManager()
                        .getMessage("player-not-found", "{player}", args[2]));
                return true;
            }

            String armorPiece = args[3].toLowerCase();

            // Parse armor set and piece type (e.g., "blaze_helmet")
            String[] parts = armorPiece.split("_");
            if (parts.length != 2) {
                sender.sendMessage(plugin.getConfigManager().getMessage("prefix") +
                        " &#FF0000Invalid armor piece format! Use: <set>_<piece>");
                sender.sendMessage(plugin.getConfigManager().getMessage("prefix") +
                        " &#808080Example: blaze_helmet, armadillo_chestplate");
                return true;
            }

            String setName = parts[0];
            String pieceType = parts[1];

            if (!plugin.getArmorManager().armorSetExists(setName)) {
                sender.sendMessage(plugin.getConfigManager().getMessage("prefix") +
                        " &#FF0000Invalid armor set! Valid sets: blaze, armadillo, angel, speedstar");
                return true;
            }

            ItemStack armor = plugin.getArmorManager().createArmorItem(setName, pieceType);
            if (armor == null) {
                sender.sendMessage(plugin.getConfigManager().getMessage("prefix") +
                        " &#FF0000Invalid armor piece! Valid pieces: helmet, chestplate, leggings, boots");
                return true;
            }

            target.getInventory().addItem(armor);

            sender.sendMessage(plugin.getConfigManager()
                    .getMessage("armory.given", "{armor}",
                            capitalizeWords(setName + " " + pieceType), "{player}", target.getName()));

            target.sendMessage(plugin.getConfigManager()
                    .getMessage("armory.received", "{armor}",
                            capitalizeWords(setName + " " + pieceType)));

            return true;
        }

        return true;
    }

    private boolean handleLevelCommand(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(plugin.getConfigManager().getMessage("player-only"));
            return true;
        }

        Player player = (Player) sender;
        new LevelUpGUI(plugin, player).open();
        return true;
    }

    private boolean handleShopCommand(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(plugin.getConfigManager().getMessage("player-only"));
            return true;
        }

        Player player = (Player) sender;
        new ShopGUI(plugin, player).open();
        return true;
    }

    private boolean handleArmoryCommand(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(plugin.getConfigManager().getMessage("player-only"));
            return true;
        }

        Player player = (Player) sender;
        new ArmoryGUI(plugin, player).open();
        return true;
    }

    private boolean handleGemsCommand(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(plugin.getConfigManager().getMessage("player-only"));
            return true;
        }

        Player player = (Player) sender;
        PlayerData data = plugin.getDataManager().getPlayerData(player);

        player.sendMessage(plugin.getConfigManager()
                .getMessage("stats.gems-balance", "{gems}", String.valueOf(data.getGems())));
        return true;
    }

    private boolean handleStatsCommand(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(plugin.getConfigManager().getMessage("player-only"));
            return true;
        }

        Player player = (Player) sender;
        new StatsGUI(plugin, player).open();
        return true;
    }

    private boolean handleReloadCommand(CommandSender sender, String[] args) {
        if (!sender.hasPermission("gpvp.reload")) {
            sender.sendMessage(plugin.getConfigManager().getMessage("no-permission"));
            return true;
        }

        plugin.reload();
        sender.sendMessage(plugin.getConfigManager().getMessage("config-reloaded"));
        return true;
    }

    private String capitalizeWords(String text) {
        if (text == null || text.isEmpty()) return text;

        String[] words = text.split("_| ");
        StringBuilder result = new StringBuilder();

        for (int i = 0; i < words.length; i++) {
            if (i > 0) result.append(" ");
            result.append(words[i].substring(0, 1).toUpperCase())
                    .append(words[i].substring(1).toLowerCase());
        }

        return result.toString();
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(ColorUtil.colorize("&#808080&m                                    "));
        sender.sendMessage(ColorUtil.colorize("<gradient:#FF6B6B:#4ECDC4>GenPvP Commands</gradient>"));
        sender.sendMessage(ColorUtil.colorize("&#808080&m                                    "));
        sender.sendMessage(ColorUtil.colorize("&#FFFF00/gpvp gen give <player> <tier> <amount> &#808080- Give generator"));
        sender.sendMessage(ColorUtil.colorize("&#FFFF00/gpvp gen giveall <tier> <amount> &#808080- Give all generator"));
        sender.sendMessage(ColorUtil.colorize("&#FFFF00/gpvp cube give <player> <rarity> <amount> &#808080- Give cube"));
        sender.sendMessage(ColorUtil.colorize("&#FFFF00/gpvp cube giveall <rarity> <amount> &#808080- Give all cube"));
        sender.sendMessage(ColorUtil.colorize("&#FFFF00/gpvp armor give <player> <armor_piece> &#808080- Give armor"));
        sender.sendMessage(ColorUtil.colorize("&#FFFF00/gpvp level &#808080- Open level up GUI"));
        sender.sendMessage(ColorUtil.colorize("&#FFFF00/gpvp shop &#808080- Open generator shop"));
        sender.sendMessage(ColorUtil.colorize("&#FFFF00/gpvp armory &#808080- Open armory"));
        sender.sendMessage(ColorUtil.colorize("&#FFFF00/gpvp gems &#808080- Check your gems"));
        sender.sendMessage(ColorUtil.colorize("&#FFFF00/gpvp stats &#808080- View your statistics"));
        sender.sendMessage(ColorUtil.colorize("&#FFFF00/gpvp reload &#808080- Reload configuration"));
        sender.sendMessage(ColorUtil.colorize("&#808080&m                                    "));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            completions.addAll(Arrays.asList("gen", "cube", "armor", "level", "shop", "armory", "gems", "stats", "reload"));
        } else if (args.length == 2) {
            if (args[0].equalsIgnoreCase("gen")) {
                completions.addAll(Arrays.asList("give", "giveall"));
            } else if (args[0].equalsIgnoreCase("cube")) {
                completions.addAll(Arrays.asList("give", "giveall"));
            } else if (args[0].equalsIgnoreCase("armor")) {
                completions.addAll(Arrays.asList("give"));
            }
        } else if (args.length == 3) {
            if (args[0].equalsIgnoreCase("gen") && args[1].equalsIgnoreCase("give")) {
                completions.addAll(Bukkit.getOnlinePlayers().stream()
                        .map(Player::getName)
                        .collect(Collectors.toList()));
            } else if (args[0].equalsIgnoreCase("gen") && args[1].equalsIgnoreCase("giveall")) {
                for (int i = 1; i <= plugin.getGeneratorManager().getMaxTier(); i++) {
                    completions.add(String.valueOf(i));
                }
            } else if (args[0].equalsIgnoreCase("cube") && args[1].equalsIgnoreCase("give")) {
                completions.addAll(Bukkit.getOnlinePlayers().stream()
                        .map(Player::getName)
                        .collect(Collectors.toList()));
            } else if (args[0].equalsIgnoreCase("cube") && args[1].equalsIgnoreCase("giveall")) {
                completions.addAll(Arrays.asList("common", "uncommon", "rare", "epic", "legendary"));
            } else if (args[0].equalsIgnoreCase("armor") && args[1].equalsIgnoreCase("give")) {
                completions.addAll(Bukkit.getOnlinePlayers().stream()
                        .map(Player::getName)
                        .collect(Collectors.toList()));
            }
        } else if (args.length == 4) {
            if (args[0].equalsIgnoreCase("gen") && args[1].equalsIgnoreCase("give")) {
                for (int i = 1; i <= plugin.getGeneratorManager().getMaxTier(); i++) {
                    completions.add(String.valueOf(i));
                }
            } else if (args[0].equalsIgnoreCase("gen") && args[1].equalsIgnoreCase("giveall")) {
                completions.addAll(Arrays.asList("1", "8", "16", "32", "64"));
            } else if (args[0].equalsIgnoreCase("cube") && args[1].equalsIgnoreCase("give")) {
                completions.addAll(Arrays.asList("common", "uncommon", "rare", "epic", "legendary"));
            } else if (args[0].equalsIgnoreCase("cube") && args[1].equalsIgnoreCase("giveall")) {
                completions.addAll(Arrays.asList("1", "8", "16", "32", "64"));
            } else if (args[0].equalsIgnoreCase("armor") && args[1].equalsIgnoreCase("give")) {
                completions.addAll(Arrays.asList(
                        "blaze_helmet", "blaze_chestplate", "blaze_leggings", "blaze_boots",
                        "armadillo_helmet", "armadillo_chestplate", "armadillo_leggings", "armadillo_boots",
                        "angel_helmet", "angel_chestplate", "angel_leggings", "angel_boots",
                        "speedstar_helmet", "speedstar_chestplate", "speedstar_leggings", "speedstar_boots"
                ));
            }
        } else if (args.length == 5) {
            if (args[0].equalsIgnoreCase("gen") && args[1].equalsIgnoreCase("give")) {
                completions.addAll(Arrays.asList("1", "8", "16", "32", "64"));
            } else if (args[0].equalsIgnoreCase("cube") && args[1].equalsIgnoreCase("give")) {
                completions.addAll(Arrays.asList("1", "8", "16", "32", "64"));
            }
        }

        return completions.stream()
                .filter(s -> s.toLowerCase().startsWith(args[args.length - 1].toLowerCase()))
                .collect(Collectors.toList());
    }
}