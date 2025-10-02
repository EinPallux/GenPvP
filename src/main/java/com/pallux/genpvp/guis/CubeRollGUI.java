package com.pallux.genpvp.guis;

import com.pallux.genpvp.GenPvP;
import com.pallux.genpvp.managers.CubeManager;
import com.pallux.genpvp.utils.ColorUtil;
import com.pallux.genpvp.utils.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class CubeRollGUI extends BaseGUI {

    private final String rarity;
    private final Random random;
    private boolean isRolling;

    public CubeRollGUI(GenPvP plugin, Player player, String rarity) {
        super(plugin, player);
        this.rarity = rarity;
        this.random = new Random();
        this.isRolling = false;
    }

    @Override
    protected String getTitle() {
        String titleFormat = plugin.getConfigManager().getColorizedString("cubes.yml", "animation.title");
        return ColorUtil.replacePlaceholders(titleFormat, "{rarity}", capitalizeFirst(rarity));
    }

    @Override
    protected int getSize() {
        return 27; // 3 rows
    }

    @Override
    protected void setContents() {
        // Fill with cycling items initially
        fillWithCycleItems();

        // Start the roll animation
        startRollAnimation();
    }

    @Override
    public void handleClick(Player player, int slot, ItemStack item, ClickType clickType) {
        // Prevent any interaction during rolling
    }

    @Override
    public void handleClose(Player player) {
        isRolling = false;
    }

    private void fillWithCycleItems() {
        String cycleName = plugin.getConfigManager().getColorizedString("cubes.yml", "animation.cycle-item.name");
        List<String> cycleLore = plugin.getConfigManager().getColorizedStringList("cubes.yml", "animation.cycle-item.lore");

        ItemStack cycleItem = createItem(Material.BARRIER, cycleName, cycleLore);

        for (int i = 0; i < inventory.getSize(); i++) {
            inventory.setItem(i, cycleItem);
        }
    }

    private void startRollAnimation() {
        isRolling = true;

        CubeManager.CubeRarity cubeRarity = plugin.getCubeManager().getCubeRarity(rarity);
        if (cubeRarity == null) {
            close();
            return;
        }

        int duration = plugin.getConfigManager().getCubeAnimationDuration();
        int updateInterval = plugin.getConfigManager().getCubeAnimationUpdateInterval();

        // Pre-roll the final reward
        CubeManager.CubeReward finalReward = plugin.getCubeManager().rollReward(rarity);

        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (!isRolling || !player.isOnline()) {
                    cancel();
                    return;
                }

                if (ticks >= duration) {
                    // Animation complete - show reward
                    showReward(finalReward);
                    cancel();
                    return;
                }

                // Update animation
                if (ticks % updateInterval == 0) {
                    updateAnimation(cubeRarity);

                    // Play roll sound
                    try {
                        player.playSound(player.getLocation(),
                                Sound.valueOf(plugin.getConfigManager().getCubeRollSound()),
                                0.5f, 1.0f + (ticks / (float) duration));
                    } catch (IllegalArgumentException e) {
                        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_HAT, 0.5f, 1.0f);
                    }
                }

                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private void updateAnimation(CubeManager.CubeRarity cubeRarity) {
        List<CubeManager.CubeReward> rewards = cubeRarity.getRewards();
        if (rewards.isEmpty()) return;

        // Randomly show different possible rewards
        for (int i = 10; i <= 16; i++) {
            CubeManager.CubeReward randomReward = rewards.get(random.nextInt(rewards.size()));
            ItemStack rewardItem = createRewardPreviewItem(randomReward);
            inventory.setItem(i, rewardItem);
        }
    }

    private void showReward(CubeManager.CubeReward reward) {
        isRolling = false;

        // Clear inventory
        inventory.clear();

        // Show the won reward in center
        ItemStack rewardItem = createFinalRewardItem(reward);
        inventory.setItem(13, rewardItem);

        // Fill rest with glass
        fillEmptySlots();

        // Play win sound
        try {
            player.playSound(player.getLocation(),
                    Sound.valueOf(plugin.getConfigManager().getCubeWinSound()),
                    1.0f, 1.0f);
        } catch (IllegalArgumentException e) {
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
        }

        // Give reward
        giveReward(player, reward);

        // Send message
        player.sendMessage(plugin.getConfigManager()
                .getMessage("cube.won", "{reward}", reward.getDisplay()));

        // Close after delay
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline()) {
                close();
            }
        }, 60L); // 3 seconds
    }

    private ItemStack createRewardPreviewItem(CubeManager.CubeReward reward) {
        Material material = getRewardMaterial(reward);
        String name = ColorUtil.colorize(reward.getDisplay());
        List<String> lore = new ArrayList<>();
        lore.add(ColorUtil.colorize("&#808080Chance: &#FFFF00" + String.format("%.1f", reward.getChance()) + "%"));

        return createItem(material, name, lore);
    }

    private ItemStack createFinalRewardItem(CubeManager.CubeReward reward) {
        Material material = getRewardMaterial(reward);
        String name = ColorUtil.colorize("<gradient:#FFD700:#FFA500>YOU WON!</gradient>");
        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add(ColorUtil.colorize(reward.getDisplay()));
        lore.add("");
        lore.add(ColorUtil.colorize("&#00FF00Reward has been added!"));

        return createItem(material, name, lore, true);
    }

    private Material getRewardMaterial(CubeManager.CubeReward reward) {
        String type = reward.getType().toUpperCase();

        switch (type) {
            case "MONEY":
                return Material.EMERALD;
            case "GEMS":
                return Material.DIAMOND;
            case "ITEM":
                return reward.getMaterial() != null ? reward.getMaterial() : Material.CHEST;
            case "COMMAND":
                return Material.COMMAND_BLOCK;
            default:
                return Material.PAPER;
        }
    }

    private void giveReward(Player player, CubeManager.CubeReward reward) {
        String type = reward.getType().toUpperCase();

        switch (type) {
            case "MONEY":
                plugin.getEconomy().depositPlayer(player, reward.getAmount());
                break;

            case "GEMS":
                PlayerData data = plugin.getDataManager().getPlayerData(player);
                data.addGems(reward.getAmount());
                break;

            case "ITEM":
                if (reward.getMaterial() != null) {
                    ItemStack item = new ItemStack(reward.getMaterial(), reward.getAmount());
                    player.getInventory().addItem(item);
                }
                break;

            case "COMMAND":
                for (String command : reward.getCommands()) {
                    String processedCommand = command.replace("{player}", player.getName());
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), processedCommand);
                }
                break;
        }
    }

    private String capitalizeFirst(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        return text.substring(0, 1).toUpperCase() + text.substring(1).toLowerCase();
    }
}