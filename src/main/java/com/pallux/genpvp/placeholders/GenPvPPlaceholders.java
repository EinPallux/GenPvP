package com.pallux.genpvp.placeholders;

import com.pallux.genpvp.GenPvP;
import com.pallux.genpvp.utils.ColorUtil;
import com.pallux.genpvp.utils.PlayerData;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class GenPvPPlaceholders extends PlaceholderExpansion {

    private final GenPvP plugin;

    public GenPvPPlaceholders(GenPvP plugin) {
        this.plugin = plugin;
    }

    @Override
    @NotNull
    public String getAuthor() {
        return "Pallux";
    }

    @Override
    @NotNull
    public String getIdentifier() {
        return "gpvp";
    }

    @Override
    @NotNull
    public String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public boolean canRegister() {
        return true;
    }

    @Override
    @Nullable
    public String onRequest(OfflinePlayer player, @NotNull String params) {
        if (player == null) {
            return "";
        }

        PlayerData data = plugin.getDataManager().getPlayerData(player.getUniqueId());
        if (data == null) {
            return "";
        }

        // Level placeholders
        if (params.equalsIgnoreCase("level")) {
            return String.valueOf(data.getLevel());
        }

        // XP placeholders
        if (params.equalsIgnoreCase("xp")) {
            return String.valueOf(data.getExperience());
        }

        if (params.equalsIgnoreCase("xp_formatted")) {
            return formatNumber(data.getExperience());
        }

        if (params.equalsIgnoreCase("xp_required")) {
            int required = plugin.getLevelManager().calculateXPRequired(data.getLevel() + 1);
            return String.valueOf(required);
        }

        if (params.equalsIgnoreCase("xp_required_formatted")) {
            int required = plugin.getLevelManager().calculateXPRequired(data.getLevel() + 1);
            return formatNumber(required);
        }

        if (params.equalsIgnoreCase("xp_progress")) {
            return String.format("%.1f", plugin.getLevelManager().getXPProgress(data));
        }

        // Gems placeholders
        if (params.equalsIgnoreCase("gems")) {
            return String.valueOf(data.getGems());
        }

        if (params.equalsIgnoreCase("gems_formatted")) {
            return formatNumber(data.getGems());
        }

        // Generator placeholders
        if (params.equalsIgnoreCase("generators_placed")) {
            return String.valueOf(data.getGeneratorsPlaced());
        }

        if (params.equalsIgnoreCase("generators_max")) {
            return String.valueOf(plugin.getLevelManager().getCurrentSlots(data));
        }

        if (params.equalsIgnoreCase("generators_available")) {
            return String.valueOf(plugin.getLevelManager().getAvailableSlots(data));
        }

        if (params.equalsIgnoreCase("generators_info")) {
            return data.getGeneratorsPlaced() + "/" + plugin.getLevelManager().getCurrentSlots(data);
        }

        // Statistics placeholders
        if (params.equalsIgnoreCase("kills")) {
            return String.valueOf(data.getKills());
        }

        if (params.equalsIgnoreCase("deaths")) {
            return String.valueOf(data.getDeaths());
        }

        if (params.equalsIgnoreCase("kdr")) {
            return String.format("%.2f", data.getKDR());
        }

        if (params.equalsIgnoreCase("blocks_placed")) {
            return String.valueOf(data.getBlocksPlaced());
        }

        if (params.equalsIgnoreCase("blocks_placed_formatted")) {
            return formatNumber(data.getBlocksPlaced());
        }

        if (params.equalsIgnoreCase("blocks_broken")) {
            return String.valueOf(data.getBlocksBroken());
        }

        if (params.equalsIgnoreCase("blocks_broken_formatted")) {
            return formatNumber(data.getBlocksBroken());
        }

        if (params.equalsIgnoreCase("money_collected")) {
            return String.valueOf((int) data.getMoneyCollected());
        }

        if (params.equalsIgnoreCase("money_collected_formatted")) {
            return formatNumber(data.getMoneyCollected());
        }

        if (params.equalsIgnoreCase("gems_collected")) {
            return String.valueOf(data.getGemsCollected());
        }

        if (params.equalsIgnoreCase("gems_collected_formatted")) {
            return formatNumber(data.getGemsCollected());
        }

        if (params.equalsIgnoreCase("playtime")) {
            return ColorUtil.formatTime(data.getPlaytime());
        }

        if (params.equalsIgnoreCase("playtime_seconds")) {
            return String.valueOf(data.getPlaytime());
        }

        // Level info placeholders
        if (params.equalsIgnoreCase("next_level")) {
            return String.valueOf(data.getLevel() + 1);
        }

        if (params.equalsIgnoreCase("next_level_slots")) {
            return String.valueOf(plugin.getLevelManager().getNextLevelSlots(data));
        }

        // Rank placeholders (by level)
        if (params.equalsIgnoreCase("level_rank")) {
            return String.valueOf(getLevelRank(data));
        }

        // Top placeholders
        if (params.startsWith("top_level_")) {
            return handleTopPlaceholder(params, "level");
        }

        if (params.startsWith("top_kills_")) {
            return handleTopPlaceholder(params, "kills");
        }

        if (params.startsWith("top_money_")) {
            return handleTopPlaceholder(params, "money");
        }

        return null;
    }

    private String formatNumber(double number) {
        if (plugin.getConfigManager().isCompactNumbers()) {
            return ColorUtil.formatNumberCompact(number);
        } else {
            return ColorUtil.formatNumber(number);
        }
    }

    private int getLevelRank(PlayerData playerData) {
        var topLevels = plugin.getStatisticsManager().getTopLevels(1000);
        for (int i = 0; i < topLevels.size(); i++) {
            if (topLevels.get(i).getKey().equals(playerData.getUuid())) {
                return i + 1;
            }
        }
        return -1;
    }

    private String handleTopPlaceholder(String params, String type) {
        try {
            String[] parts = params.split("_");
            int position = Integer.parseInt(parts[parts.length - 2]);
            String dataType = parts[parts.length - 1]; // "name" or "value"

            if (type.equals("level")) {
                var topList = plugin.getStatisticsManager().getTopLevels(position);
                if (topList.size() >= position) {
                    var entry = topList.get(position - 1);
                    if (dataType.equals("name")) {
                        return plugin.getServer().getOfflinePlayer(entry.getKey()).getName();
                    } else if (dataType.equals("value")) {
                        return String.valueOf(entry.getValue());
                    }
                }
            } else if (type.equals("kills")) {
                var topList = plugin.getStatisticsManager().getTopKills(position);
                if (topList.size() >= position) {
                    var entry = topList.get(position - 1);
                    if (dataType.equals("name")) {
                        return plugin.getServer().getOfflinePlayer(entry.getKey()).getName();
                    } else if (dataType.equals("value")) {
                        return String.valueOf(entry.getValue());
                    }
                }
            } else if (type.equals("money")) {
                var topList = plugin.getStatisticsManager().getTopMoneyCollected(position);
                if (topList.size() >= position) {
                    var entry = topList.get(position - 1);
                    if (dataType.equals("name")) {
                        return plugin.getServer().getOfflinePlayer(entry.getKey()).getName();
                    } else if (dataType.equals("value")) {
                        return formatNumber(entry.getValue());
                    }
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Error parsing placeholder: " + params);
            e.printStackTrace();
            return "";
        }

        return "N/A";
    }
}