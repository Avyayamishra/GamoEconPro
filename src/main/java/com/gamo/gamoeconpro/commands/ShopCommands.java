package com.gamo.gamoeconpro.commands;

import com.gamo.gamoeconpro.GamoEconPro;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.StringUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class ShopCommands implements CommandExecutor, TabCompleter {

    private final GamoEconPro plugin;

    public ShopCommands(GamoEconPro plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use shop commands.");
            return true;
        }

        Player player = (Player) sender;

        if (command.getName().equalsIgnoreCase("shop")) {
            if (args.length == 0) {
                sendShopHelp(player);
                return true;
            }

            switch (args[0].toLowerCase()) {
                case "create":
                    handleCreateShop(player, args);
                    break;
                case "remove":
                    handleRemoveShop(player);
                    break;
                case "info":
                    handleShopInfo(player);
                    break;
                case "help":
                default:
                    sendShopHelp(player);
                    break;
            }
        }

        return true;
    }

    private void sendShopHelp(Player player) {
        player.sendMessage(ChatColor.GOLD + "=== Shop Commands ===");
        player.sendMessage(ChatColor.YELLOW + "/shop create <buy/sell> <item> <quantity> <price> - Create a shop sign");
        player.sendMessage(ChatColor.YELLOW + "/shop remove - Remove a shop sign (look at it)");
        player.sendMessage(ChatColor.YELLOW + "/shop info - Get info about a shop sign (look at it)");
        player.sendMessage(ChatColor.GRAY + "Place the sign on any side of a chest to create a shop!");
        player.sendMessage(ChatColor.GRAY + "Example: /shop create buy diamond 1 100");
    }

    private void handleCreateShop(Player player, String[] args) {
        if (args.length != 5) {
            player.sendMessage(ChatColor.RED + "Usage: /shop create <buy/sell> <item> <quantity> <price>");
            return;
        }

        String type = args[1].toLowerCase();
        if (!type.equals("buy") && !type.equals("sell")) {
            player.sendMessage(ChatColor.RED + "Shop type must be 'buy' or 'sell'");
            return;
        }

        String itemName = args[2];
        Material material = Material.matchMaterial(itemName.toUpperCase().replace(" ", "_"));
        if (material == null || material == Material.AIR) {
            player.sendMessage(ChatColor.RED + "Invalid item: " + itemName);
            return;
        }

        int quantity;
        double price;
        try {
            quantity = Integer.parseInt(args[3]);
            price = Double.parseDouble(args[4]);

            if (quantity <= 0 || price <= 0) {
                player.sendMessage(ChatColor.RED + "Quantity and price must be positive numbers.");
                return;
            }
        } catch (NumberFormatException e) {
            player.sendMessage(ChatColor.RED + "Invalid quantity or price format.");
            return;
        }

        ItemStack sign = new ItemStack(Material.OAK_SIGN, 1);
        player.getInventory().addItem(sign);

        player.sendMessage(ChatColor.GREEN + "Shop sign created! Place it on any side of a chest and write:");
        player.sendMessage(ChatColor.YELLOW + "Line 1: " + (type.equals("buy") ? "Buy" : "Sell"));
        player.sendMessage(ChatColor.YELLOW + "Line 2: " + material.name().toLowerCase());
        player.sendMessage(ChatColor.YELLOW + "Line 3: " + quantity);
        player.sendMessage(ChatColor.YELLOW + "Line 4: " + price);
    }

    private void handleRemoveShop(Player player) {
        Block targetBlock = player.getTargetBlockExact(5);
        if (targetBlock == null || !targetBlock.getType().name().contains("SIGN")) {
            player.sendMessage(ChatColor.RED + "Look at a shop sign to remove it.");
            return;
        }

        Chest chest = plugin.getShopManager().getAttachedChest(targetBlock);
        if (chest == null) {
            player.sendMessage(ChatColor.RED + "This is not a valid shop sign.");
            return;
        }

        String ownerUUID = chest.getCustomName();
        if (ownerUUID == null || !ownerUUID.equals(player.getUniqueId().toString())) {
            player.sendMessage(ChatColor.RED + "You can only remove your own shop signs.");
            return;
        }

        targetBlock.setType(Material.AIR);
        chest.setCustomName(null);
        chest.update();

        player.sendMessage(ChatColor.GREEN + "Shop sign removed successfully.");
    }

    private void handleShopInfo(Player player) {
        Block targetBlock = player.getTargetBlockExact(5);
        if (targetBlock == null || !targetBlock.getType().name().contains("SIGN")) {
            player.sendMessage(ChatColor.RED + "Look at a shop sign to get its info.");
            return;
        }

        Chest chest = plugin.getShopManager().getAttachedChest(targetBlock);
        if (chest == null) {
            player.sendMessage(ChatColor.RED + "This is not a valid shop sign.");
            return;
        }

        String ownerUUID = chest.getCustomName();
        if (ownerUUID == null) {
            player.sendMessage(ChatColor.RED + "This shop has no owner information.");
            return;
        }

        try {
            org.bukkit.OfflinePlayer owner = org.bukkit.Bukkit.getOfflinePlayer(java.util.UUID.fromString(ownerUUID));
            player.sendMessage(ChatColor.GOLD + "=== Shop Information ===");
            player.sendMessage(ChatColor.YELLOW + "Owner: " + (owner.getName() != null ? owner.getName() : "Unknown"));
            player.sendMessage(ChatColor.YELLOW + "Online: " + (owner.isOnline() ? "Yes" : "No"));

            int itemCount = 0;
            for (ItemStack item : chest.getInventory().getContents()) {
                if (item != null) {
                    itemCount += item.getAmount();
                }
            }
            player.sendMessage(ChatColor.YELLOW + "Items in stock: " + itemCount);
        } catch (IllegalArgumentException e) {
            player.sendMessage(ChatColor.RED + "Invalid shop owner information.");
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!(sender instanceof Player)) {
            return Collections.emptyList();
        }

        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            // Main command completions
            List<String> commands = Arrays.asList("create", "remove", "info", "help");
            return StringUtil.copyPartialMatches(args[0], commands, new ArrayList<>());
        } else if (args.length == 2 && args[0].equalsIgnoreCase("create")) {
            // Shop type completions
            List<String> types = Arrays.asList("buy", "sell");
            return StringUtil.copyPartialMatches(args[1], types, new ArrayList<>());
        } else if (args.length == 3 && args[0].equalsIgnoreCase("create")) {
            // Material completions (common items)
            List<String> materials = Arrays.asList("diamond", "emerald", "gold_ingot", "iron_ingot",
                    "coal", "wheat", "apple", "bread", "stone", "oak_log", "cobblestone");
            return StringUtil.copyPartialMatches(args[2], materials, new ArrayList<>());
        } else if (args.length == 4 && args[0].equalsIgnoreCase("create")) {
            // Quantity suggestions
            return Arrays.asList("1", "8", "16", "32", "64");
        } else if (args.length == 5 && args[0].equalsIgnoreCase("create")) {
            // Price suggestions
            return Arrays.asList("10", "50", "100", "500", "1000");
        }

        return completions;
    }
}