package com.gamo.gamoeconpro.chestshop;

import com.gamo.gamoeconpro.GamoEconPro;
import org.bukkit.*;
import org.bukkit.block.*;
import org.bukkit.block.data.Directional;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public class ShopManager {

    private final GamoEconPro plugin;
    private final Set<Location> knownShops = new HashSet<>();

    public ShopManager(GamoEconPro plugin) {
        this.plugin = plugin;
    }

    public Chest getAttachedChest(Block signBlock) {
        if (signBlock == null) return null;

        BlockState state = signBlock.getState();
        if (!(state instanceof Sign)) {
            return null;
        }

        if (!(signBlock.getBlockData() instanceof Directional)) {
            return null;
        }

        Directional directional = (Directional) signBlock.getBlockData();
        Block attachedBlock = signBlock.getRelative(directional.getFacing().getOppositeFace());

        if (attachedBlock.getState() instanceof Chest) {
            return (Chest) attachedBlock.getState();
        }

        return null;
    }

    public boolean handleBuy(Player buyer, Chest chest, ItemStack item, int quantity, double price, OfflinePlayer owner) {
        double taxRate = plugin.getConfig().getDouble("taxes.transaction", 5.0) / 100.0;
        double tax = price * taxRate;
        double total = price + tax;

        if (plugin.getEconomyManager().getBalance(buyer.getUniqueId()) < total) {
            buyer.sendMessage(ChatColor.RED + "You don't have enough money. Need: ₹" + String.format("%.2f", total));
            return false;
        }

        Inventory shopInv = chest.getBlockInventory();

        if (!shopHasEnough(shopInv, item, quantity)) {
            buyer.sendMessage(ChatColor.RED + "Shop is out of stock for " + item.getType().name().toLowerCase());
            return false;
        }

        plugin.getEconomyManager().removeBalance(buyer.getUniqueId(), total);
        plugin.getEconomyManager().addBalance(owner.getUniqueId(), price);
        plugin.getTreasuryManager().addToTreasury(tax);

        if (!transferItems(shopInv, buyer.getInventory(), item, quantity)) {
            plugin.getEconomyManager().addBalance(buyer.getUniqueId(), total);
            plugin.getEconomyManager().removeBalance(owner.getUniqueId(), price);
            plugin.getTreasuryManager().removeFromTreasury(tax);
            buyer.sendMessage(ChatColor.RED + "Transaction failed - your inventory might be full.");
            return false;
        }

        // Update stock prices for the shop owner's businesses
        plugin.getStockManager().updateStockFromShop(owner.getUniqueId(), price, true);

        buyer.sendMessage(ChatColor.GREEN + "Purchase successful! " +
                "Bought " + quantity + "x " + item.getType().name().toLowerCase() +
                " for ₹" + String.format("%.2f", price) + " (Tax: ₹" + String.format("%.2f", tax) + ")");

        if (owner.isOnline()) {
            ((Player) owner).sendMessage(ChatColor.GREEN + buyer.getName() + " bought " +
                    quantity + "x " + item.getType().name().toLowerCase() +
                    " from your shop for ₹" + String.format("%.2f", price));
        }

        return true;
    }

    public boolean handleSell(Player seller, Chest chest, ItemStack item, int quantity, double price, OfflinePlayer owner) {
        double taxRate = plugin.getConfig().getDouble("taxes.transaction", 5.0) / 100.0;
        double tax = price * taxRate;
        double net = price - tax;

        Inventory shopInv = chest.getBlockInventory();

        if (!playerHasEnough(seller.getInventory(), item, quantity)) {
            seller.sendMessage(ChatColor.RED + "You don't have enough " + item.getType().name().toLowerCase() +
                    ". Need: " + quantity);
            return false;
        }

        if (plugin.getEconomyManager().getBalance(owner.getUniqueId()) < price) {
            seller.sendMessage(ChatColor.RED + "Shop owner doesn't have enough money.");
            return false;
        }

        if (!chestHasSpace(shopInv, item, quantity)) {
            seller.sendMessage(ChatColor.RED + "Shop chest is full.");
            return false;
        }

        plugin.getEconomyManager().addBalance(seller.getUniqueId(), net);
        plugin.getEconomyManager().removeBalance(owner.getUniqueId(), price);
        plugin.getTreasuryManager().addToTreasury(tax);

        if (!transferItems(seller.getInventory(), shopInv, item, quantity)) {
            plugin.getEconomyManager().removeBalance(seller.getUniqueId(), net);
            plugin.getEconomyManager().addBalance(owner.getUniqueId(), price);
            plugin.getTreasuryManager().removeFromTreasury(tax);
            seller.sendMessage(ChatColor.RED + "Transaction failed - shop chest might be full.");
            return false;
        }

        // Update stock prices for the shop owner's businesses
        plugin.getStockManager().updateStockFromShop(owner.getUniqueId(), price, false);

        seller.sendMessage(ChatColor.GREEN + "Sold successfully! " +
                "Sold " + quantity + "x " + item.getType().name().toLowerCase() +
                " for ₹" + String.format("%.2f", net) + " (Tax: ₹" + String.format("%.2f", tax) + ")");

        if (owner.isOnline()) {
            ((Player) owner).sendMessage(ChatColor.GREEN + seller.getName() + " sold " +
                    quantity + "x " + item.getType().name().toLowerCase() +
                    " to your shop for ₹" + String.format("%.2f", price));
        }

        return true;
    }

    public void registerShop(Location location) {
        if (location != null) {
            knownShops.add(location.clone());
        }
    }

    public void unregisterShop(Location location) {
        if (location != null) {
            knownShops.remove(location);
        }
    }

    public void cleanupInvalidShops() {
        try {
            Iterator<Location> iterator = knownShops.iterator();
            int removed = 0;

            while (iterator.hasNext()) {
                Location location = iterator.next();

                World world = location.getWorld();
                if (world == null) {
                    iterator.remove();
                    removed++;
                    continue;
                }

                if (!world.isChunkLoaded(location.getBlockX() >> 4, location.getBlockZ() >> 4)) {
                    continue;
                }

                Block block = location.getBlock();

                if (!isValidShopBlock(block)) {
                    iterator.remove();
                    removed++;
                }
            }

            if (removed > 0) {
                plugin.getLogger().info("Cleaned up " + removed + " invalid shop locations");
            }

        } catch (Exception e) {
            plugin.getLogger().warning("Error during shop cleanup: " + e.getMessage());
        }
    }

    private boolean isValidShopBlock(Block block) {
        if (block == null) return false;

        Material type = block.getType();

        if (type == Material.CHEST || type == Material.TRAPPED_CHEST) {
            return true;
        }

        if (type.name().contains("SIGN")) {
            BlockState state = block.getState();
            if (state instanceof Sign) {
                Sign sign = (Sign) state;
                String[] lines = sign.getLines();

                return lines.length > 0 &&
                        (lines[0].contains("[") && lines[0].contains("]")) ||
                        (lines[1].contains("₹") || lines[1].contains("$"));
            }
        }

        return false;
    }

    public Set<Location> getRegisteredShops() {
        return new HashSet<>(knownShops);
    }

    public int getShopCount() {
        return knownShops.size();
    }

    private boolean shopHasEnough(Inventory shopInventory, ItemStack item, int quantity) {
        if (shopInventory == null || item == null) return false;

        int found = 0;
        for (ItemStack stack : shopInventory.getContents()) {
            if (stack != null && stack.isSimilar(item)) {
                found += stack.getAmount();
                if (found >= quantity) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean playerHasEnough(Inventory playerInventory, ItemStack item, int quantity) {
        if (playerInventory == null || item == null) return false;

        int found = 0;
        for (ItemStack stack : playerInventory.getContents()) {
            if (stack != null && stack.isSimilar(item)) {
                found += stack.getAmount();
                if (found >= quantity) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean chestHasSpace(Inventory chestInventory, ItemStack item, int quantity) {
        if (chestInventory == null || item == null) return false;

        int remainingQuantity = quantity;

        for (ItemStack stack : chestInventory.getContents()) {
            if (stack == null) {
                remainingQuantity -= item.getMaxStackSize();
            } else if (stack.isSimilar(item)) {
                int spaceInStack = item.getMaxStackSize() - stack.getAmount();
                remainingQuantity -= spaceInStack;
            }

            if (remainingQuantity <= 0) {
                return true;
            }
        }

        return remainingQuantity <= 0;
    }

    private boolean transferItems(Inventory sourceInventory, Inventory destInventory, ItemStack item, int quantity) {
        if (sourceInventory == null || destInventory == null || item == null || quantity <= 0) {
            return false;
        }

        if (sourceInventory != destInventory) {
            if (!shopHasEnough(sourceInventory, item, quantity) || !chestHasSpace(destInventory, item, quantity)) {
                return false;
            }
        }

        int remainingToTransfer = quantity;
        List<ItemStack> sourceStacks = new ArrayList<>();
        List<Integer> sourceSlots = new ArrayList<>();

        for (int i = 0; i < sourceInventory.getSize() && remainingToTransfer > 0; i++) {
            ItemStack stack = sourceInventory.getItem(i);
            if (stack != null && stack.isSimilar(item)) {
                int takeAmount = Math.min(remainingToTransfer, stack.getAmount());
                sourceStacks.add(new ItemStack(item.getType(), takeAmount));
                sourceSlots.add(i);
                remainingToTransfer -= takeAmount;
            }
        }

        if (remainingToTransfer > 0) {
            return false;
        }

        remainingToTransfer = quantity;
        for (int i = 0; i < sourceStacks.size(); i++) {
            int slot = sourceSlots.get(i);
            ItemStack sourceStack = sourceInventory.getItem(slot);
            int takeAmount = Math.min(remainingToTransfer, sourceStack.getAmount());

            if (sourceStack.getAmount() <= takeAmount) {
                sourceInventory.setItem(slot, null);
            } else {
                sourceStack.setAmount(sourceStack.getAmount() - takeAmount);
            }
            remainingToTransfer -= takeAmount;
        }

        HashMap<Integer, ItemStack> leftover = destInventory.addItem(new ItemStack(item.getType(), quantity));

        if (!leftover.isEmpty()) {
            sourceInventory.addItem(new ItemStack(item.getType(), quantity));
            return false;
        }

        return true;
    }

    public GamoEconPro getPlugin() {
        return plugin;
    }
}