package com.pallux.genpvp.listeners;

import com.pallux.genpvp.GenPvP;
import com.pallux.genpvp.guis.DefenseUpgradeGUI;
import com.pallux.genpvp.managers.DefenseDataManager;
import com.pallux.genpvp.managers.DefenseManager;
import com.pallux.genpvp.utils.ColorUtil;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Bisected;
import org.bukkit.block.data.type.Door;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

public class DefenseListener implements Listener {

    private final GenPvP plugin;

    public DefenseListener(GenPvP plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDefensePlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();
        ItemStack item = event.getItemInHand();

        // Check if the placed block is a defense block
        if (!plugin.getDefenseManager().isDefenseBlock(item)) {
            return;
        }

        // Check if world is allowed
        if (!isWorldAllowed(block.getWorld().getName())) {
            event.setCancelled(true);
            player.sendMessage(plugin.getConfigManager().getMessage("defense.world-disabled"));
            return;
        }

        // Get defense tier from item
        int tier = plugin.getDefenseManager().getDefenseTier(item);
        if (tier == 0) {
            return;
        }

        DefenseManager.DefenseTier defenseTier = plugin.getDefenseManager().getDefenseTier(tier);
        if (defenseTier == null) {
            return;
        }

        Location location = block.getLocation();

        // For doors, also register the top half
        if (defenseTier.isDoor() && block.getType() == Material.IRON_DOOR) {
            Block topBlock = block.getRelative(BlockFace.UP);
            plugin.getDefenseDataManager().addDefenseBlock(
                    topBlock.getLocation(), tier, defenseTier.getHearts(), player.getUniqueId()
            );
        }

        // Add defense block to data
        plugin.getDefenseDataManager().addDefenseBlock(
                location, tier, defenseTier.getHearts(), player.getUniqueId()
        );

        // Play sound
        if (plugin.getConfigManager().isSoundsEnabled()) {
            player.playSound(player.getLocation(), Sound.BLOCK_STONE_PLACE, 1.0f, 1.0f);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDefenseBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();
        Location location = block.getLocation();

        // Check if this block is a defense block
        if (!plugin.getDefenseDataManager().isDefenseBlock(location)) {
            return;
        }

        // Prevent normal break
        event.setCancelled(true);

        DefenseDataManager.DefenseBlockData data = plugin.getDefenseDataManager().getDefenseData(location);
        if (data == null) return;

        UUID owner = data.getOwner();

        // If player is owner, break instantly
        if (owner != null && player.getUniqueId().equals(owner)) {
            handleDefenseDestruction(player, block, location, data);
            return;
        }

        // Damage the block
        plugin.getDefenseDataManager().damageDefenseBlock(location, 1);
        int remainingHearts = plugin.getDefenseDataManager().getCurrentHearts(location);

        // Show hearts remaining in ACTION BAR
        if (shouldShowHearts()) {
            String message = plugin.getConfigManager().getDefenseConfig()
                    .getString("settings.damage-format", "&#EB4034{hearts} ❤");
            message = message.replace("{hearts}", String.valueOf(remainingHearts));

            // Send to action bar
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR,
                    new TextComponent(ColorUtil.colorize(message)));
        }

        // Spawn particles
        if (shouldShowParticles()) {
            spawnDamageParticles(location);
        }

        // Check if block is destroyed
        if (remainingHearts <= 0) {
            handleDefenseDestruction(player, block, location, data);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onDefenseInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }

        Block block = event.getClickedBlock();
        if (block == null) return;

        Player player = event.getPlayer();
        Location location = block.getLocation();

        // Check if this block is a defense block
        if (!plugin.getDefenseDataManager().isDefenseBlock(location)) {
            return;
        }

        DefenseDataManager.DefenseBlockData data = plugin.getDefenseDataManager().getDefenseData(location);
        if (data == null) return;

        int tier = data.getTier();
        DefenseManager.DefenseTier defenseTier = plugin.getDefenseManager().getDefenseTier(tier);
        if (defenseTier == null) return;

        // Handle door opening
        if (defenseTier.isDoor() && block.getType() == Material.IRON_DOOR) {
            event.setCancelled(true);

            // Check if player is owner
            UUID owner = data.getOwner();
            if (owner != null && !player.getUniqueId().equals(owner)) {
                player.sendMessage(plugin.getConfigManager().getMessage("defense.not-owner"));
                return;
            }

            // Toggle door
            Door doorData = (Door) block.getBlockData();
            doorData.setOpen(!doorData.isOpen());
            block.setBlockData(doorData);

            // Also update the other half of the door
            Block otherHalf;
            if (doorData.getHalf() == Bisected.Half.BOTTOM) {
                otherHalf = block.getRelative(BlockFace.UP);
            } else {
                otherHalf = block.getRelative(BlockFace.DOWN);
            }

            if (otherHalf.getType() == Material.IRON_DOOR) {
                Door otherDoorData = (Door) otherHalf.getBlockData();
                otherDoorData.setOpen(doorData.isOpen());
                otherHalf.setBlockData(otherDoorData);
            }

            // Play sound
            player.playSound(location, doorData.isOpen() ? Sound.BLOCK_IRON_DOOR_OPEN : Sound.BLOCK_IRON_DOOR_CLOSE, 1.0f, 1.0f);
            return;
        }

        // Handle upgrade with shift + right-click
        if (player.isSneaking() && !defenseTier.isDoor()) {
            event.setCancelled(true);

            // Check if player is owner
            UUID owner = data.getOwner();
            if (owner != null && !player.getUniqueId().equals(owner)) {
                player.sendMessage(plugin.getConfigManager().getMessage("defense.not-owner"));
                return;
            }

            // Check if upgrades are allowed
            if (!plugin.getConfigManager().getDefenseConfig().getBoolean("settings.allow-upgrades", true)) {
                player.sendMessage(plugin.getConfigManager().getMessage("defense.upgrades-disabled"));
                return;
            }

            // Open upgrade GUI
            new DefenseUpgradeGUI(plugin, player, location, tier).open();

            // Play sound
            if (plugin.getConfigManager().isSoundsEnabled()) {
                player.playSound(player.getLocation(),
                        Sound.valueOf(plugin.getConfigManager().getOpenSound()), 1.0f, 1.0f);
            }
        }
    }

    private void handleDefenseDestruction(Player player, Block block, Location location, DefenseDataManager.DefenseBlockData data) {
        int tier = data.getTier();
        DefenseManager.DefenseTier defenseTier = plugin.getDefenseManager().getDefenseTier(tier);

        // Remove from data
        plugin.getDefenseDataManager().removeDefenseBlock(location);

        // If it's a door, also remove the other half
        if (defenseTier != null && defenseTier.isDoor() && block.getType() == Material.IRON_DOOR) {
            Door doorData = (Door) block.getBlockData();
            Block otherHalf;
            if (doorData.getHalf() == Bisected.Half.BOTTOM) {
                otherHalf = block.getRelative(BlockFace.UP);
            } else {
                otherHalf = block.getRelative(BlockFace.DOWN);
            }
            plugin.getDefenseDataManager().removeDefenseBlock(otherHalf.getLocation());
        }

        // Break the block
        block.setType(Material.AIR);

        // Handle drop
        boolean autoPickup = plugin.getConfigManager().getDefenseConfig()
                .getBoolean("settings.auto-pickup-on-break", true);
        boolean preventDrop = plugin.getConfigManager().getDefenseConfig()
                .getBoolean("settings.prevent-drop", true);

        if (!preventDrop) {
            ItemStack drop = plugin.getDefenseManager().createDefenseItem(tier, 1);
            if (drop != null) {
                if (autoPickup) {
                    player.getInventory().addItem(drop);
                } else {
                    location.getWorld().dropItemNaturally(location, drop);
                }
            }
        }

        // Play sound
        player.playSound(location, Sound.BLOCK_STONE_BREAK, 1.0f, 1.0f);
    }

    private void spawnDamageParticles(Location location) {
        try {
            String particleType = plugin.getConfigManager().getDefenseConfig()
                    .getString("particles.type", "BLOCK_CRACK");
            int count = plugin.getConfigManager().getDefenseConfig()
                    .getInt("particles.count", 10);

            Particle particle = Particle.valueOf(particleType);
            Location center = location.clone().add(0.5, 0.5, 0.5);
            location.getWorld().spawnParticle(particle, center, count, 0.3, 0.3, 0.3, 0);
        } catch (IllegalArgumentException e) {
            // Invalid particle type, ignore
        }
    }

    private boolean shouldShowHearts() {
        return plugin.getConfigManager().getDefenseConfig()
                .getBoolean("settings.show-hearts-on-damage", true);
    }

    private boolean shouldShowParticles() {
        return plugin.getConfigManager().getDefenseConfig()
                .getBoolean("particles.enabled", true);
    }

    private boolean isWorldAllowed(String worldName) {
        var allowedWorlds = plugin.getConfigManager().getDefenseConfig()
                .getStringList("settings.allowed-worlds");
        return allowedWorlds.isEmpty() || allowedWorlds.contains(worldName);
    }
}