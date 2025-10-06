package com.pallux.genpvp.guis;

import com.pallux.genpvp.GenPvP;
import com.pallux.genpvp.managers.ArmorManager;
import com.pallux.genpvp.utils.ColorUtil;
import com.pallux.genpvp.utils.PlayerData;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ArmoryGUI extends BaseGUI {

    private static final List<String> ARMOR_SETS = Arrays.asList("blaze", "armadillo", "angel", "speedstar");
    private static final List<String> PIECE_TYPES = Arrays.asList("helmet", "chestplate", "leggings", "boots");

    public ArmoryGUI(GenPvP plugin, Player player) {
        super(plugin, player);
    }

    @Override
    protected String getTitle() {
        return plugin.getConfigManager().getMessagesConfig()
                .getString("gui-titles.armory", "<gradient:#FF6B6B:#4ECDC4>Armory</gradient>");
    }

    @Override
    protected int getSize() {
        return 54; // 6 rows
    }

    @Override
    protected void setContents() {
        // Column positions for each armor set (with spacing)
        int[] columns = {1, 3, 5, 7};

        for (int i = 0; i < ARMOR_SETS.size(); i++) {
            String setName = ARMOR_SETS.get(i);
            int baseSlot = columns[i];

            ArmorManager.ArmorSet armorSet = plugin.getArmorManager().getArmorSet(setName);
            if (armorSet == null) continue;

            // Set header item
            ItemStack headerItem = createSetHeaderItem(armorSet);
            inventory.setItem(baseSlot, headerItem);

            // Place armor pieces vertically
            for (int j = 0; j < PIECE_TYPES.size(); j++) {
                String pieceType = PIECE_TYPES.get(j);
                int slot = baseSlot + ((j + 1) * 9); // Move down one row for each piece

                ItemStack armorItem = createArmorPurchaseItem(armorSet, pieceType);
                inventory.setItem(slot, armorItem);
            }
        }

        // Back button (slot 45)
        inventory.setItem(45, createBackButton());

        // Close button
        inventory.setItem(49, createCloseButton());

        // Fill empty slots
        fillEmptySlots();
    }

    @Override
    public void handleClick(Player player, int slot, ItemStack item, ClickType clickType) {
        if (slot == 45) {
            // Back to main shop
            new CombinedShopGUI(plugin, player).open();
            return;
        }

        if (slot == 49) {
            close();
            return;
        }

        // Determine which armor piece was clicked
        String setName = getSetNameFromSlot(slot);
        String pieceType = getPieceTypeFromSlot(slot);

        if (setName == null || pieceType == null) {
            return;
        }

        // Attempt purchase
        attemptPurchase(player, setName, pieceType);
    }

    /**
     * Creates the header item for an armor set
     */
    private ItemStack createSetHeaderItem(ArmorManager.ArmorSet armorSet) {
        String name = plugin.getConfigManager().getArmorsConfig()
                .getString("armor-sets." + armorSet.getName() + ".header-name", armorSet.getDisplayName());

        List<String> lore = plugin.getConfigManager().getArmorsConfig()
                .getStringList("armor-sets." + armorSet.getName() + ".header-lore");

        // Add effect description
        List<String> finalLore = new ArrayList<>();
        for (String line : lore) {
            finalLore.add(ColorUtil.colorize(line));
        }

        return createItem(Material.LEATHER_HORSE_ARMOR, name, finalLore, true);
    }

    /**
     * Creates a purchasable armor item
     */
    private ItemStack createArmorPurchaseItem(ArmorManager.ArmorSet armorSet, String pieceType) {
        ArmorManager.ArmorPiece piece = armorSet.getPiece(pieceType);
        if (piece == null) {
            return createItem(Material.BARRIER, "&#FF0000Error", Arrays.asList("&#FF0000Invalid armor piece"));
        }

        // Create the actual armor item as display
        ItemStack displayItem = plugin.getArmorManager().createArmorItem(armorSet.getName(), pieceType);
        if (displayItem != null && displayItem.hasItemMeta()) {
            ItemStack clone = displayItem.clone();
            org.bukkit.inventory.meta.ItemMeta meta = clone.getItemMeta();

            // Get the base lore and add cost information for the GUI
            List<String> newLore = new ArrayList<>(meta.getLore() != null ? meta.getLore() : new ArrayList<>());
            newLore.add("");
            newLore.add(ColorUtil.colorize("&#FFD700&lCOST"));
            newLore.add(ColorUtil.colorize("  &#808080&l- &#FFD700$" + ColorUtil.formatNumber(piece.getMoneyCost())));
            newLore.add(ColorUtil.colorize("  &#DDA0DD&l- " + piece.getGemsCost() + " Gems"));
            newLore.add("");
            newLore.add(ColorUtil.colorize("&#00FF00Click to purchase!"));

            meta.setLore(newLore);
            clone.setItemMeta(meta);
            return clone;
        }

        return createItem(Material.BARRIER, "Error", new ArrayList<>());
    }

    /**
     * Attempts to purchase an armor piece
     */
    private void attemptPurchase(Player player, String setName, String pieceType) {
        ArmorManager.ArmorSet armorSet = plugin.getArmorManager().getArmorSet(setName);
        if (armorSet == null) {
            playErrorSound();
            return;
        }

        ArmorManager.ArmorPiece piece = armorSet.getPiece(pieceType);
        if (piece == null) {
            playErrorSound();
            return;
        }

        PlayerData data = plugin.getDataManager().getPlayerData(player);

        // Check money
        double balance = plugin.getEconomy().getBalance(player);
        if (balance < piece.getMoneyCost()) {
            playErrorSound();
            player.sendMessage(plugin.getConfigManager()
                    .getMessage("armory.not-enough-money", "{cost}", ColorUtil.formatNumber(piece.getMoneyCost())));
            return;
        }

        // Check gems
        if (data.getGems() < piece.getGemsCost()) {
            playErrorSound();
            player.sendMessage(plugin.getConfigManager()
                    .getMessage("armory.not-enough-gems", "{gems}", String.valueOf(piece.getGemsCost())));
            return;
        }

        // Check inventory space
        if (player.getInventory().firstEmpty() == -1) {
            playErrorSound();
            player.sendMessage(plugin.getConfigManager().getMessage("armory.inventory-full"));
            return;
        }

        // Deduct costs
        plugin.getEconomy().withdrawPlayer(player, piece.getMoneyCost());
        data.removeGems(piece.getGemsCost());

        // Give armor
        ItemStack armor = plugin.getArmorManager().createArmorItem(setName, pieceType);
        if (armor != null) {
            player.getInventory().addItem(armor);
        }

        // Send message
        player.sendMessage(plugin.getConfigManager()
                .getMessage("armory.purchased", "{armor}",
                        armorSet.getDisplayName() + " " + capitalize(pieceType)));

        // Play success sound
        playSuccessSound();

        // Refresh GUI
        refresh();
    }

    /**
     * Gets set name from slot
     */
    private String getSetNameFromSlot(int slot) {
        int[] columns = {1, 3, 5, 7};

        for (int i = 0; i < columns.length; i++) {
            int baseSlot = columns[i];
            // Check if slot is in this column
            if (slot == baseSlot || (slot > baseSlot && slot < baseSlot + 45 && (slot - baseSlot) % 9 == 0)) {
                return ARMOR_SETS.get(i);
            }
        }

        return null;
    }

    /**
     * Gets piece type from slot
     */
    private String getPieceTypeFromSlot(int slot) {
        int[] columns = {1, 3, 5, 7};

        for (int baseSlot : columns) {
            if (slot == baseSlot) {
                return null; // Header item
            }

            for (int i = 0; i < PIECE_TYPES.size(); i++) {
                int pieceSlot = baseSlot + ((i + 1) * 9);
                if (slot == pieceSlot) {
                    return PIECE_TYPES.get(i);
                }
            }
        }

        return null;
    }

    /**
     * Capitalizes first letter
     */
    private String capitalize(String text) {
        if (text == null || text.isEmpty()) return text;
        return text.substring(0, 1).toUpperCase() + text.substring(1).toLowerCase();
    }
}