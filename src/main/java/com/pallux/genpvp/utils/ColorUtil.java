package com.pallux.genpvp.utils;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ColorUtil {

    private static final Pattern HEX_PATTERN = Pattern.compile("&#([A-Fa-f0-9]{6})");
    private static final Pattern GRADIENT_PATTERN = Pattern.compile("<gradient:(#[A-Fa-f0-9]{6}):(#[A-Fa-f0-9]{6})>(.+?)</gradient>");

    /**
     * Translates color codes in a string, supporting:
     * - Legacy color codes (&a, &b, etc.)
     * - Hex colors (&#RRGGBB)
     * - Gradients (<gradient:#FF0000:#00FF00>text</gradient>)
     */
    public static String colorize(String message) {
        if (message == null || message.isEmpty()) {
            return message;
        }

        // Process gradients first
        message = processGradients(message);

        // Process hex colors
        message = processHexColors(message);

        // Process legacy color codes
        message = ChatColor.translateAlternateColorCodes('&', message);

        return message;
    }

    /**
     * Colorizes a list of strings
     */
    public static List<String> colorize(List<String> messages) {
        if (messages == null) {
            return new ArrayList<>();
        }

        List<String> colorized = new ArrayList<>();
        for (String message : messages) {
            colorized.add(colorize(message));
        }
        return colorized;
    }

    /**
     * Strips all color codes from a string
     */
    public static String stripColor(String message) {
        if (message == null || message.isEmpty()) {
            return message;
        }
        return ChatColor.stripColor(message);
    }

    /**
     * Processes hex color codes (&#RRGGBB)
     */
    private static String processHexColors(String message) {
        Matcher matcher = HEX_PATTERN.matcher(message);
        StringBuffer buffer = new StringBuffer();

        while (matcher.find()) {
            String hexColor = matcher.group(1);
            matcher.appendReplacement(buffer, ChatColor.of("#" + hexColor).toString());
        }

        matcher.appendTail(buffer);
        return buffer.toString();
    }

    /**
     * Processes gradient color codes
     * Format: <gradient:#FF0000:#00FF00>text</gradient>
     */
    private static String processGradients(String message) {
        Matcher matcher = GRADIENT_PATTERN.matcher(message);
        StringBuffer buffer = new StringBuffer();

        while (matcher.find()) {
            String startHex = matcher.group(1);
            String endHex = matcher.group(2);
            String text = matcher.group(3);

            String gradient = applyGradient(text, startHex, endHex);
            matcher.appendReplacement(buffer, Matcher.quoteReplacement(gradient));
        }

        matcher.appendTail(buffer);
        return buffer.toString();
    }

    /**
     * Applies a gradient effect to text between two colors
     */
    private static String applyGradient(String text, String startHex, String endHex) {
        // Remove any existing color codes from the text
        text = ChatColor.stripColor(text);

        if (text.isEmpty()) {
            return "";
        }

        // Parse start and end colors
        Color startColor = Color.decode(startHex);
        Color endColor = Color.decode(endHex);

        StringBuilder result = new StringBuilder();
        int length = text.length();

        for (int i = 0; i < length; i++) {
            char c = text.charAt(i);

            // Skip spaces (don't color them)
            if (c == ' ') {
                result.append(c);
                continue;
            }

            // Calculate the color for this character
            float ratio = (float) i / (float) (length - 1);

            int red = (int) (startColor.getRed() + ratio * (endColor.getRed() - startColor.getRed()));
            int green = (int) (startColor.getGreen() + ratio * (endColor.getGreen() - startColor.getGreen()));
            int blue = (int) (startColor.getBlue() + ratio * (endColor.getBlue() - startColor.getBlue()));

            Color color = new Color(red, green, blue);
            String hex = String.format("#%02x%02x%02x", color.getRed(), color.getGreen(), color.getBlue());

            result.append(ChatColor.of(hex)).append(c);
        }

        return result.toString();
    }

    /**
     * Formats a number with commas (1000 -> 1,000)
     */
    public static String formatNumber(double number) {
        return String.format("%,.0f", number);
    }

    /**
     * Formats a number in compact form (1000 -> 1K, 1000000 -> 1M)
     */
    public static String formatNumberCompact(double number) {
        if (number < 1000) {
            return String.format("%.0f", number);
        } else if (number < 1000000) {
            return String.format("%.1fK", number / 1000);
        } else if (number < 1000000000) {
            return String.format("%.1fM", number / 1000000);
        } else {
            return String.format("%.1fB", number / 1000000000);
        }
    }

    /**
     * Formats a number with decimal places
     */
    public static String formatNumber(double number, int decimals) {
        return String.format("%,." + decimals + "f", number);
    }

    /**
     * Converts seconds to a readable time format (e.g., "1d 2h 30m")
     */
    public static String formatTime(long seconds) {
        if (seconds < 60) {
            return seconds + "s";
        }

        long minutes = seconds / 60;
        if (minutes < 60) {
            return minutes + "m " + (seconds % 60) + "s";
        }

        long hours = minutes / 60;
        if (hours < 24) {
            return hours + "h " + (minutes % 60) + "m";
        }

        long days = hours / 24;
        return days + "d " + (hours % 24) + "h";
    }

    /**
     * Sends a colored message to a player
     */
    public static void sendMessage(org.bukkit.entity.Player player, String message) {
        if (player != null && message != null && !message.isEmpty()) {
            player.sendMessage(colorize(message));
        }
    }

    /**
     * Broadcasts a colored message to all online players
     */
    public static void broadcast(String message) {
        if (message != null && !message.isEmpty()) {
            Bukkit.broadcastMessage(colorize(message));
        }
    }

    /**
     * Replaces placeholders in a message
     */
    public static String replacePlaceholders(String message, Object... replacements) {
        if (message == null || replacements.length % 2 != 0) {
            return message;
        }

        for (int i = 0; i < replacements.length; i += 2) {
            String placeholder = String.valueOf(replacements[i]);
            String value = String.valueOf(replacements[i + 1]);
            message = message.replace(placeholder, value);
        }

        return message;
    }
}