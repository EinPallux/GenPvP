package com.pallux.genpvp.managers;

import com.pallux.genpvp.GenPvP;
import com.pallux.genpvp.utils.ColorUtil;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;

public class ArmorManager {

    private final GenPvP plugin;
    private final Map<String, ArmorSet> armorSets;
    private final NamespacedKey armorKey;
    private final NamespacedKey armorTypeKey;

    public ArmorManager(GenPvP plugin) {
        this.plugin = plugin;
        this.armorSets = new HashMap<>();
        this.armorKey = new NamespacedKey(plugin, "custom_armor");
        this.armorTypeKey = new NamespacedKey(plugin, "armor_type");
        loadArmorSets();
    }

    /**
     * Loads all armor sets from config
     */
    public void loadArmorSets() {
        armorSets.clear();

        ConfigurationSection section = plugin.getConfigManager().getArmorsConfig()
                .getConfigurationSection("armor-sets");

        if (section == null) {
            plugin.getLogger().severe("No armor sets configuration found!");
            return;
        }

        for (String setName : section.getKeys(false)) {
            try {
                ConfigurationSection setSection = section.getConfigurationSection(setName);
                if (setSection == null) continue;

                String displayName = setSection.getString("display-name", setName);
                String colorHex = setSection.getString("color", "#FFFFFF");
                String effectType = setSection.getString("effect-type", "NONE");
                double effectValue = setSection.getDouble("effect-value", 0.0);

                Map<String, ArmorPiece> pieces = new HashMap<>();

                // Load each armor piece
                for (String pieceType : Arrays.asList("helmet", "chestplate", "leggings", "boots")) {
                    ConfigurationSection pieceSection = setSection.getConfigurationSection(pieceType);
                    if (pieceSection == null) continue;

                    int armor = pieceSection.getInt("armor", 1);
                    double moneyCost = pieceSection.getDouble("money-cost", 0);
                    int gemsCost = pieceSection.getInt("gems-cost", 0);

                    ArmorPiece piece = new ArmorPiece(
                            pieceType,
                            armor,
                            moneyCost,
                            gemsCost
                    );

                    pieces.put(pieceType, piece);
                }

                ArmorSet armorSet = new ArmorSet(
                        setName,
                        displayName,
                        colorHex,
                        effectType,
                        effectValue,
                        pieces
                );

                armorSets.put(setName.toLowerCase(), armorSet);

            } catch (Exception e) {
                plugin.getLogger().warning("Error loading armor set " + setName + ": " + e.getMessage());
                e.printStackTrace();
            }
        }

        plugin.getLogger().info("Loaded " + armorSets.size() + " armor sets!");
    }

    /**
     * Gets an armor set by name
     */
    public ArmorSet getArmorSet(String setName) {
        return armorSets.get(setName.toLowerCase());
    }

    /**
     * Gets all armor sets
     */
    public Map<String, ArmorSet> getAllArmorSets() {
        return new HashMap<>(armorSets);
    }

    /**
     * Checks if an armor set exists
     */
    public boolean armorSetExists(String setName) {
        return armorSets.containsKey(setName.toLowerCase());
    }

    /**
     * Creates a custom armor item
     */
    public ItemStack createArmorItem(String setName, String pieceType) {
        ArmorSet armorSet = armorSets.get(setName.toLowerCase());
        if (armorSet == null) return null;

        ArmorPiece piece = armorSet.getPiece(pieceType);
        if (piece == null) return null;

        // Determine material
        Material material;
        EquipmentSlot slot;
        switch (pieceType.toLowerCase()) {
            case "helmet":
                material = Material.LEATHER_HELMET;
                slot = EquipmentSlot.HEAD;
                break;
            case "chestplate":
                material = Material.LEATHER_CHESTPLATE;
                slot = EquipmentSlot.CHEST;
                break;
            case "leggings":
                material = Material.LEATHER_LEGGINGS;
                slot = EquipmentSlot.LEGS;
                break;
            case "boots":
                material = Material.LEATHER_BOOTS;
                slot = EquipmentSlot.FEET;
                break;
            default:
                return null;
        }

        ItemStack item = new ItemStack(material);
        LeatherArmorMeta meta = (LeatherArmorMeta) item.getItemMeta();

        if (meta != null) {
            // Set display name
            String displayName = plugin.getConfigManager().getArmorsConfig()
                    .getString("armor-sets." + setName + "." + pieceType + ".name",
                            armorSet.getDisplayName() + " " + capitalize(pieceType));
            meta.setDisplayName(ColorUtil.colorize(displayName));

            // Set color
            Color color = hexToColor(armorSet.getColorHex());
            meta.setColor(color);

            // Set lore from config
            List<String> lore = new ArrayList<>();
            List<String> configLore = plugin.getConfigManager().getArmorsConfig()
                    .getStringList("armor-sets." + setName + "." + pieceType + ".lore");

            for (String line : configLore) {
                String processedLine = line
                        .replace("{armor}", String.valueOf(piece.getArmor()))
                        .replace("{effect}", armorSet.getEffectType())
                        .replace("{effect_description}", getEffectDescription(armorSet.getEffectType(), armorSet.getEffectValue()));
                lore.add(ColorUtil.colorize(processedLine));
            }

            meta.setLore(lore);

            // Make unbreakable
            meta.setUnbreakable(true);
            meta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE);
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);

            // Add glow effect
            meta.addEnchant(Enchantment.DURABILITY, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);

            // Set armor attribute
            UUID uuid = UUID.randomUUID();
            AttributeModifier armorModifier = new AttributeModifier(
                    uuid,
                    "custom_armor",
                    piece.getArmor(),
                    AttributeModifier.Operation.ADD_NUMBER,
                    slot
            );
            meta.addAttributeModifier(Attribute.GENERIC_ARMOR, armorModifier);

            // Add persistent data to identify armor
            meta.getPersistentDataContainer().set(armorKey, PersistentDataType.STRING, setName.toLowerCase());
            meta.getPersistentDataContainer().set(armorTypeKey, PersistentDataType.STRING, pieceType.toLowerCase());

            item.setItemMeta(meta);
        }

        return item;
    }

    /**
     * Checks if an item is a custom armor piece
     */
    public boolean isCustomArmor(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;

        return meta.getPersistentDataContainer().has(armorKey, PersistentDataType.STRING);
    }

    /**
     * Gets the armor set name from an item
     */
    public String getArmorSetName(ItemStack item) {
        if (!isCustomArmor(item)) return null;

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return null;

        return meta.getPersistentDataContainer().get(armorKey, PersistentDataType.STRING);
    }

    /**
     * Gets the armor piece type from an item
     */
    public String getArmorPieceType(ItemStack item) {
        if (!isCustomArmor(item)) return null;

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return null;

        return meta.getPersistentDataContainer().get(armorTypeKey, PersistentDataType.STRING);
    }

    /**
     * Converts hex color to Bukkit Color
     */
    private Color hexToColor(String hex) {
        try {
            hex = hex.replace("#", "");
            int r = Integer.parseInt(hex.substring(0, 2), 16);
            int g = Integer.parseInt(hex.substring(2, 4), 16);
            int b = Integer.parseInt(hex.substring(4, 6), 16);
            return Color.fromRGB(r, g, b);
        } catch (Exception e) {
            return Color.WHITE;
        }
    }

    /**
     * Gets effect description for lore
     */
    private String getEffectDescription(String effectType, double effectValue) {
        switch (effectType.toUpperCase()) {
            case "BLAZE":
                return "&#FF4500Sets attackers on fire!";
            case "HARDENED":
                return "&#808080Enhanced armor protection!";
            case "EXTRA_HEARTS":
                return "&#FF1493+" + (effectValue) + " ❤ per piece!";
            case "SPEED":
                return "&#00FFFFIncreased movement speed!";
            default:
                return "&#808080Special armor effect";
        }
    }

    /**
     * Capitalizes first letter
     */
    private String capitalize(String text) {
        if (text == null || text.isEmpty()) return text;
        return text.substring(0, 1).toUpperCase() + text.substring(1).toLowerCase();
    }

    /**
     * Reloads the armor manager
     */
    public void reload() {
        loadArmorSets();
    }

    /**
     * Inner class representing an armor set
     */
    public static class ArmorSet {
        private final String name;
        private final String displayName;
        private final String colorHex;
        private final String effectType;
        private final double effectValue;
        private final Map<String, ArmorPiece> pieces;

        public ArmorSet(String name, String displayName, String colorHex, String effectType,
                        double effectValue, Map<String, ArmorPiece> pieces) {
            this.name = name;
            this.displayName = displayName;
            this.colorHex = colorHex;
            this.effectType = effectType;
            this.effectValue = effectValue;
            this.pieces = pieces;
        }

        public String getName() { return name; }
        public String getDisplayName() { return displayName; }
        public String getColorHex() { return colorHex; }
        public String getEffectType() { return effectType; }
        public double getEffectValue() { return effectValue; }
        public Map<String, ArmorPiece> getPieces() { return pieces; }
        public ArmorPiece getPiece(String type) { return pieces.get(type.toLowerCase()); }
    }

    /**
     * Inner class representing an armor piece
     */
    public static class ArmorPiece {
        private final String type;
        private final int armor;
        private final double moneyCost;
        private final int gemsCost;

        public ArmorPiece(String type, int armor, double moneyCost, int gemsCost) {
            this.type = type;
            this.armor = armor;
            this.moneyCost = moneyCost;
            this.gemsCost = gemsCost;
        }

        public String getType() { return type; }
        public int getArmor() { return armor; }
        public double getMoneyCost() { return moneyCost; }
        public int getGemsCost() { return gemsCost; }
    }
}