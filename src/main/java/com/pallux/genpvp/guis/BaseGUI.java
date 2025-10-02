package com.pallux.genpvp.guis;

import com.pallux.genpvp.GenPvP;
import com.pallux.genpvp.utils.ColorUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public abstract class BaseGUI implements InventoryHolder {

    protected final GenPvP plugin;
    protected final Player player;
    protected Inventory inventory;

    public BaseGUI(GenPvP plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
    }

    /**
     * Gets the GUI title
     */
    protected abstract String getTitle();

    /**
     * Gets the GUI size (must be multiple of 9)
     */
    protected abstract int getSize();

    /**
     * Sets up the GUI contents
     */
    protected abstract void setContents();

    /**
     * Handles click events
     */
    public abstract void handleClick(Player player, int slot, ItemStack item, ClickType clickType);

    /**
     * Handles GUI close event
     */
    public void handleClose(Player player) {
        // Override if needed
    }

    /**
     * Opens the GUI for the player
     */
    public void open() {
        inventory = Bukkit.createInventory(this, getSize(), ColorUtil.colorize(getTitle()));
        setContents();
        player.openInventory(inventory);
    }

    /**
     * Refreshes the GUI
     */
    public void refresh() {
        inventory.clear();
        setContents();
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    /**
     * Creates a GUI item
     */
    protected ItemStack createItem(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            if (name != null) {
                meta.setDisplayName(ColorUtil.colorize(name));
            }

            if (lore != null && !lore.isEmpty()) {
                List<String> coloredLore = new ArrayList<>();
                for (String line : lore) {
                    coloredLore.add(ColorUtil.colorize(line));
                }
                meta.setLore(coloredLore);
            }

            item.setItemMeta(meta);
        }

        return item;
    }

    /**
     * Creates a GUI item with glow effect
     */
    protected ItemStack createItem(Material material, String name, List<String> lore, boolean glow) {
        ItemStack item = createItem(material, name, lore);

        if (glow) {
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.addEnchant(Enchantment.DURABILITY, 1, true);
                meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
                item.setItemMeta(meta);
            }
        }

        return item;
    }

    /**
     * Creates a GUI item with custom model data
     */
    protected ItemStack createItem(Material material, String name, List<String> lore, int customModelData) {
        ItemStack item = createItem(material, name, lore);

        if (customModelData > 0) {
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.setCustomModelData(customModelData);
                item.setItemMeta(meta);
            }
        }

        return item;
    }

    /**
     * Fills empty slots with filler glass pane
     */
    protected void fillEmptySlots() {
        if (!plugin.getConfigManager().isFillEmptySlots()) {
            return;
        }

        Material fillerMaterial;
        try {
            fillerMaterial = Material.valueOf(plugin.getConfigManager().getFillerMaterial());
        } catch (IllegalArgumentException e) {
            fillerMaterial = Material.BLACK_STAINED_GLASS_PANE;
        }

        ItemStack filler = new ItemStack(fillerMaterial);
        ItemMeta meta = filler.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(" ");
            filler.setItemMeta(meta);
        }

        for (int i = 0; i < inventory.getSize(); i++) {
            if (inventory.getItem(i) == null) {
                inventory.setItem(i, filler);
            }
        }
    }

    /**
     * Creates a close button item
     */
    protected ItemStack createCloseButton() {
        String name = plugin.getConfigManager().getMessagesConfig().getString("gui.close.name", "<red>Close</red>");
        List<String> lore = plugin.getConfigManager().getMessagesConfig().getStringList("gui.close.lore");

        return createItem(Material.BARRIER, name, lore);
    }

    /**
     * Creates a back button item
     */
    protected ItemStack createBackButton() {
        String name = plugin.getConfigManager().getMessagesConfig().getString("gui.back.name", "<yellow>Back</yellow>");
        List<String> lore = plugin.getConfigManager().getMessagesConfig().getStringList("gui.back.lore");

        return createItem(Material.ARROW, name, lore);
    }

    /**
     * Plays a sound to the player
     */
    protected void playSound(Sound sound) {
        if (plugin.getConfigManager().isSoundsEnabled()) {
            player.playSound(player.getLocation(), sound, 1.0f, 1.0f);
        }
    }

    /**
     * Plays the open sound
     */
    protected void playOpenSound() {
        try {
            playSound(Sound.valueOf(plugin.getConfigManager().getOpenSound()));
        } catch (IllegalArgumentException e) {
            playSound(Sound.BLOCK_CHEST_OPEN);
        }
    }

    /**
     * Plays the success sound
     */
    protected void playSuccessSound() {
        try {
            playSound(Sound.valueOf(plugin.getConfigManager().getSuccessSound()));
        } catch (IllegalArgumentException e) {
            playSound(Sound.ENTITY_PLAYER_LEVELUP);
        }
    }

    /**
     * Plays the error sound
     */
    protected void playErrorSound() {
        try {
            playSound(Sound.valueOf(plugin.getConfigManager().getErrorSound()));
        } catch (IllegalArgumentException e) {
            playSound(Sound.ENTITY_VILLAGER_NO);
        }
    }

    /**
     * Closes the GUI for the player
     */
    protected void close() {
        player.closeInventory();
    }

    /**
     * Replaces placeholders in a string
     */
    protected String replacePlaceholders(String text, Object... replacements) {
        return ColorUtil.replacePlaceholders(text, replacements);
    }

    /**
     * Replaces placeholders in a list of strings
     */
    protected List<String> replacePlaceholders(List<String> list, Object... replacements) {
        List<String> result = new ArrayList<>();
        for (String line : list) {
            result.add(replacePlaceholders(line, replacements));
        }
        return result;
    }
}