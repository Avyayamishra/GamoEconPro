package com.gamo.gamoeconpro.chestshop;

import com.gamo.gamoeconpro.GamoEconPro;
import org.bukkit.*;
import org.bukkit.block.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

public class ShopSignListener implements Listener {

    private final GamoEconPro plugin;
    private final ShopManager shopManager;

    public ShopSignListener(GamoEconPro plugin) {
        this.plugin = plugin;
        this.shopManager = new ShopManager(plugin);
    }

    @EventHandler
    public void onChestOpen(InventoryOpenEvent event) {
        if (!(event.getInventory().getHolder() instanceof Chest)) return;

        Chest chest = (Chest) event.getInventory().getHolder();
        String ownerUUID = chest.getCustomName();
        if (ownerUUID == null) return;

        if (!(event.getPlayer() instanceof Player)) return;
        Player player = (Player) event.getPlayer();

        if (!player.getUniqueId().toString().equals(ownerUUID) && !player.hasPermission("gamoecon.admin")) {
            event.setCancelled(true);
            player.sendMessage(ChatColor.RED + "This is a shop chest - you can only interact with the sign!");
        }
    }

    @EventHandler
    public void onSignChange(SignChangeEvent event) {
        Player player = event.getPlayer();
        Block signBlock = event.getBlock();
        String[] lines = event.getLines();

        // Check if editing existing shop sign
        if (signBlock.getState() instanceof Sign) {
            Sign existingSign = (Sign) signBlock.getState();
            String existingLine0 = ChatColor.stripColor(existingSign.getLine(0));
            if (existingLine0.equalsIgnoreCase("Buy") || existingLine0.equalsIgnoreCase("Sell")) {
                Chest chest = shopManager.getAttachedChest(signBlock);
                if (chest != null) {
                    String ownerUUID = chest.getCustomName();
                    if (ownerUUID != null && !player.getUniqueId().toString().equals(ownerUUID)
                            && !player.hasPermission("gamoecon.admin")) {
                        event.setCancelled(true);
                        player.sendMessage(ChatColor.RED + "You can't edit someone else's shop sign!");
                        return;
                    }
                }
            }
        }

        // Check if creating new shop sign
        if (lines[0] == null) return;
        String line0 = ChatColor.stripColor(lines[0].toLowerCase());

        if (!line0.equals("buy") && !line0.equals("sell")) return;

        // Validate sign format
        if (lines[1] == null || lines[1].trim().isEmpty()) {
            player.sendMessage(ChatColor.RED + "Line 2 must contain item name!");
            event.setCancelled(true);
            return;
        }

        try {
            Integer.parseInt(lines[2]);
            Double.parseDouble(lines[3]);
        } catch (NumberFormatException e) {
            player.sendMessage(ChatColor.RED + "Line 3 must be quantity (number) and Line 4 must be price (number)!");
            event.setCancelled(true);
            return;
        }

        // Check if chest exists adjacent
        Chest chest = shopManager.getAttachedChest(signBlock);
        if (chest == null) {
            player.sendMessage(ChatColor.RED + "You must place the sign on a chest!");
            event.setCancelled(true);
            return;
        }

        // Store owner information in the chest
        chest.setCustomName(player.getUniqueId().toString());
        chest.update();

        // Format sign appearance
        event.setLine(0, (line0.equals("buy") ? ChatColor.GREEN + "Buy" : ChatColor.BLUE + "Sell"));
        player.sendMessage(ChatColor.GREEN + "Shop sign created successfully!");
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        Player player = event.getPlayer();

        // Check if breaking a shop chest
        if (block.getState() instanceof Chest) {
            Chest chest = (Chest) block.getState();
            String ownerUUID = chest.getCustomName();
            if (ownerUUID != null && !player.getUniqueId().toString().equals(ownerUUID)
                    && !player.hasPermission("gamoecon.admin")) {
                event.setCancelled(true);
                player.sendMessage(ChatColor.RED + "You can't break someone else's shop chest!");
                return;
            }
        }

        // Check if breaking a shop sign
        if (block.getState() instanceof Sign) {
            Sign sign = (Sign) block.getState();
            String line0 = ChatColor.stripColor(sign.getLine(0));
            if (line0.equalsIgnoreCase("Buy") || line0.equalsIgnoreCase("Sell")) {
                Chest chest = shopManager.getAttachedChest(block);
                if (chest != null) {
                    String ownerUUID = chest.getCustomName();
                    if (ownerUUID != null && !player.getUniqueId().toString().equals(ownerUUID)
                            && !player.hasPermission("gamoecon.admin")) {
                        event.setCancelled(true);
                        player.sendMessage(ChatColor.RED + "You can't break someone else's shop sign!");
                        return;
                    }
                }
            }
        }
    }

    @EventHandler
    public void onSignClick(PlayerInteractEvent event) {
        if (event.getAction() != Action.LEFT_CLICK_BLOCK) return;
        if (event.getClickedBlock() == null || !(event.getClickedBlock().getState() instanceof Sign)) return;

        Player player = event.getPlayer();
        Sign sign = (Sign) event.getClickedBlock().getState();

        String line0 = ChatColor.stripColor(sign.getLine(0));
        boolean isBuy = line0.equalsIgnoreCase("Buy");
        boolean isSell = line0.equalsIgnoreCase("Sell");

        if (!isBuy && !isSell) return;

        String itemName = sign.getLine(1);
        if (itemName == null || itemName.trim().isEmpty()) {
            player.sendMessage(ChatColor.RED + "Invalid shop sign - missing item name.");
            return;
        }

        int quantity;
        double price;

        try {
            String quantityStr = sign.getLine(2);
            String priceStr = sign.getLine(3);

            if (quantityStr == null || priceStr == null) {
                player.sendMessage(ChatColor.RED + "Invalid shop sign format.");
                return;
            }

            quantity = Integer.parseInt(quantityStr);
            price = Double.parseDouble(priceStr);

            if (quantity <= 0 || price <= 0) {
                player.sendMessage(ChatColor.RED + "Quantity and price must be positive numbers.");
                return;
            }
        } catch (NumberFormatException e) {
            player.sendMessage(ChatColor.RED + "Invalid quantity or price format.");
            return;
        }

        // Find attached chest
        Chest chest = shopManager.getAttachedChest(event.getClickedBlock());
        if (chest == null) {
            player.sendMessage(ChatColor.RED + "No chest found attached to this shop sign.");
            return;
        }

        // Get owner from chest
        String ownerUUID = chest.getCustomName();
        if (ownerUUID == null) {
            player.sendMessage(ChatColor.RED + "Shop owner not found.");
            return;
        }

        OfflinePlayer owner;
        try {
            owner = Bukkit.getOfflinePlayer(java.util.UUID.fromString(ownerUUID));
        } catch (IllegalArgumentException e) {
            player.sendMessage(ChatColor.RED + "Invalid shop owner.");
            return;
        }

        // Don't allow owner to use their own shop
        if (player.getUniqueId().equals(owner.getUniqueId())) {
            player.sendMessage(ChatColor.RED + "You cannot use your own shop!");
            return;
        }

        // Match the material
        Material material = Material.matchMaterial(itemName.toUpperCase().replace(" ", "_"));
        if (material == null || material == Material.AIR) {
            player.sendMessage(ChatColor.RED + "Invalid item type: " + itemName);
            return;
        }

        ItemStack match = new ItemStack(material, 1);

        boolean success;
        if (isBuy) {
            success = shopManager.handleBuy(player, chest, match, quantity, price, owner);
        } else {
            success = shopManager.handleSell(player, chest, match, quantity, price, owner);
        }

        if (!success) {
            player.sendMessage(ChatColor.RED + "Transaction failed!");
        } else {
            player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
        }
    }
}