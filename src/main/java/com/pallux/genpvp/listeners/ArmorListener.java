package com.pallux.genpvp.listeners;

import com.pallux.genpvp.GenPvP;
import com.pallux.genpvp.managers.ArmorManager;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ArmorListener implements Listener {

    private final GenPvP plugin;
    private final Map<UUID, Double> baseMaxHealthMap;

    public ArmorListener(GenPvP plugin) {
        this.plugin = plugin;
        this.baseMaxHealthMap = new HashMap<>();
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        // Store base max health
        AttributeInstance healthAttr = player.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        if (healthAttr != null) {
            baseMaxHealthMap.put(player.getUniqueId(), healthAttr.getBaseValue());
        }

        // Apply armor effects after a short delay
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            updateArmorEffects(player);
        }, 20L);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();

        // Clean up stored data
        baseMaxHealthMap.remove(player.getUniqueId());
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        // Check if victim is wearing Blaze armor
        if (event.getEntity() instanceof Player) {
            Player victim = (Player) event.getEntity();

            if (event.getDamager() instanceof Player) {
                Player attacker = (Player) event.getDamager();

                // Check for Blaze effect (sets attackers on fire)
                int blazePieces = countArmorPiecesWithEffect(victim, "blaze");
                if (blazePieces > 0) {
                    // Set attacker on fire (1 second per piece)
                    attacker.setFireTicks(20 * blazePieces);
                }
            }
        }
    }

    /**
     * Updates all armor effects for a player
     */
    public void updateArmorEffects(Player player) {
        // Count armor pieces by set
        Map<String, Integer> armorCounts = new HashMap<>();
        armorCounts.put("blaze", countArmorPiecesWithEffect(player, "blaze"));
        armorCounts.put("armadillo", countArmorPiecesWithEffect(player, "armadillo"));
        armorCounts.put("angel", countArmorPiecesWithEffect(player, "angel"));
        armorCounts.put("speedstar", countArmorPiecesWithEffect(player, "speedstar"));

        // Apply Angel armor effect (Extra Hearts)
        int angelPieces = armorCounts.get("angel");
        applyExtraHearts(player, angelPieces);

        // Apply Speedstar armor effect (Speed)
        int speedstarPieces = armorCounts.get("speedstar");
        applySpeed(player, speedstarPieces);
    }

    /**
     * Counts how many pieces of a specific armor set a player is wearing
     */
    private int countArmorPiecesWithEffect(Player player, String setName) {
        int count = 0;

        ItemStack[] armor = player.getInventory().getArmorContents();
        for (ItemStack piece : armor) {
            if (piece != null && plugin.getArmorManager().isCustomArmor(piece)) {
                String armorSet = plugin.getArmorManager().getArmorSetName(piece);
                if (armorSet != null && armorSet.equalsIgnoreCase(setName)) {
                    count++;
                }
            }
        }

        return count;
    }

    /**
     * Applies extra hearts effect
     */
    private void applyExtraHearts(Player player, int pieces) {
        AttributeInstance healthAttr = player.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        if (healthAttr == null) return;

        // Get base health
        double baseHealth = baseMaxHealthMap.getOrDefault(player.getUniqueId(), 20.0);

        // Angel armor gives +0.5 hearts (1 HP) per piece
        double extraHealth = pieces * 1.0;
        double newMaxHealth = baseHealth + extraHealth;

        // Set new max health
        healthAttr.setBaseValue(newMaxHealth);

        // Heal player to match new max health if needed
        if (player.getHealth() > newMaxHealth) {
            player.setHealth(newMaxHealth);
        }
    }

    /**
     * Applies speed effect
     */
    private void applySpeed(Player player, int pieces) {
        if (pieces > 0) {
            // Each piece gives Speed I (cumulative up to Speed IV)
            int speedLevel = Math.min(pieces, 4);
            player.addPotionEffect(new PotionEffect(
                    PotionEffectType.SPEED,
                    Integer.MAX_VALUE,
                    speedLevel - 1,
                    false,
                    false,
                    false
            ));
        } else {
            // Remove speed effect if no speedstar armor
            player.removePotionEffect(PotionEffectType.SPEED);
        }
    }

    /**
     * Removes all custom armor effects from a player
     */
    public void removeArmorEffects(Player player) {
        // Reset max health
        AttributeInstance healthAttr = player.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        if (healthAttr != null) {
            double baseHealth = baseMaxHealthMap.getOrDefault(player.getUniqueId(), 20.0);
            healthAttr.setBaseValue(baseHealth);
        }

        // Remove speed effect
        player.removePotionEffect(PotionEffectType.SPEED);
    }
}