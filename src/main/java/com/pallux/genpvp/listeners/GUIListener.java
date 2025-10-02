package com.pallux.genpvp.listeners;

import com.pallux.genpvp.GenPvP;
import com.pallux.genpvp.guis.*;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.InventoryHolder;

public class GUIListener implements Listener {

    private final GenPvP plugin;

    public GUIListener(GenPvP plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }

        InventoryHolder holder = event.getInventory().getHolder();

        if (holder instanceof BaseGUI) {
            event.setCancelled(true);

            if (event.getCurrentItem() == null) {
                return;
            }

            Player player = (Player) event.getWhoClicked();
            BaseGUI gui = (BaseGUI) holder;

            gui.handleClick(player, event.getSlot(), event.getCurrentItem(), event.getClick());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getPlayer();
        InventoryHolder holder = event.getInventory().getHolder();

        if (holder instanceof CubeRollGUI) {
            CubeRollGUI gui = (CubeRollGUI) holder;
            if (gui.isRolling()) {
                // Re-open GUI on the next tick to prevent closing
                Bukkit.getScheduler().runTask(plugin, () -> player.openInventory(gui.getInventory()));
                return; // Don't call handleClose and stop the animation
            }
        }

        if (holder instanceof BaseGUI) {
            BaseGUI gui = (BaseGUI) holder;
            gui.handleClose(player);
        }
    }
}