package com.pallux.genpvp.managers;

import com.pallux.genpvp.GenPvP;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.flags.Flag;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.flags.registry.FlagConflictException;
import com.sk89q.worldguard.protection.flags.registry.FlagRegistry;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import com.sk89q.worldguard.protection.regions.RegionQuery;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public class WorldGuardManager {

    private final GenPvP plugin;
    private boolean worldGuardEnabled = false;
    private StateFlag GENERATOR_BREAK_FLAG;
    private StateFlag GENERATOR_PLACE_FLAG;

    public WorldGuardManager(GenPvP plugin) {
        this.plugin = plugin;
    }

    /**
     * Registers custom flags with WorldGuard - MUST be called in onLoad()
     */
    public void registerFlags() {
        try {
            FlagRegistry registry = WorldGuard.getInstance().getFlagRegistry();

            try {
                // Create and register the generator break flag
                StateFlag breakFlag = new StateFlag("generator-break", true);
                registry.register(breakFlag);
                GENERATOR_BREAK_FLAG = breakFlag;

                // Create and register the generator place flag
                StateFlag placeFlag = new StateFlag("generator-place", true);
                registry.register(placeFlag);
                GENERATOR_PLACE_FLAG = placeFlag;

                worldGuardEnabled = true;
                plugin.getLogger().info("WorldGuard custom flags registered successfully!");
                plugin.getLogger().info("  - generator-break (default: allow)");
                plugin.getLogger().info("  - generator-place (default: allow)");

            } catch (FlagConflictException e) {
                // Flags already registered (plugin reload)
                GENERATOR_BREAK_FLAG = (StateFlag) registry.get("generator-break");
                GENERATOR_PLACE_FLAG = (StateFlag) registry.get("generator-place");
                worldGuardEnabled = true;
                plugin.getLogger().info("WorldGuard flags already registered.");
            }

        } catch (Exception e) {
            worldGuardEnabled = false;
            plugin.getLogger().severe("Error registering WorldGuard flags: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Checks if WorldGuard is enabled
     */
    public boolean isWorldGuardEnabled() {
        return worldGuardEnabled;
    }

    /**
     * Checks if a player can place a generator at a location
     */
    public boolean canPlaceGenerator(Player player, Location location) {
        if (!worldGuardEnabled) {
            return true;
        }

        try {
            RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
            RegionQuery query = container.createQuery();
            com.sk89q.worldedit.util.Location loc = BukkitAdapter.adapt(location);

            // Check custom generator-place flag
            if (GENERATOR_PLACE_FLAG != null) {
                StateFlag.State state = query.queryState(loc, WorldGuardPlugin.inst().wrapPlayer(player), GENERATOR_PLACE_FLAG);
                if (state == StateFlag.State.DENY) {
                    return false;
                }
                if (state == StateFlag.State.ALLOW) {
                    return true;
                }
            }

            // If no specific flag is set, fall back to default behavior
            return true;

        } catch (Exception e) {
            plugin.getLogger().warning("Error checking WorldGuard generator-place permission: " + e.getMessage());
            return true; // Allow if check fails
        }
    }

    /**
     * Checks if a player can break a generator at a location
     */
    public boolean canBreakGenerator(Player player, Location location) {
        if (!worldGuardEnabled) {
            return true;
        }

        try {
            RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
            RegionQuery query = container.createQuery();
            com.sk89q.worldedit.util.Location loc = BukkitAdapter.adapt(location);

            // Check custom generator-break flag
            if (GENERATOR_BREAK_FLAG != null) {
                StateFlag.State state = query.queryState(loc, WorldGuardPlugin.inst().wrapPlayer(player), GENERATOR_BREAK_FLAG);
                if (state == StateFlag.State.DENY) {
                    return false;
                }
                if (state == StateFlag.State.ALLOW) {
                    return true;
                }
            }

            // If no specific flag is set, fall back to default behavior
            return true;

        } catch (Exception e) {
            plugin.getLogger().warning("Error checking WorldGuard generator-break permission: " + e.getMessage());
            return true; // Allow if check fails
        }
    }

    /**
     * Gets the generator break flag
     */
    public StateFlag getGeneratorBreakFlag() {
        return GENERATOR_BREAK_FLAG;
    }

    /**
     * Gets the generator place flag
     */
    public StateFlag getGeneratorPlaceFlag() {
        return GENERATOR_PLACE_FLAG;
    }
}