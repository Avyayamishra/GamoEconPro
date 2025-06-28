package com.gamo.gamoeconpro.stockmarket;

import java.util.*;

import com.gamo.gamoeconpro.GamoEconPro;
import org.bukkit.entity.Player;

public class StockManager {

    private final GamoEconPro plugin;
    private final Map<String, Stock> stocks = new HashMap<>();
    private final Map<UUID, Map<String, Integer>> playerHoldings = new HashMap<>();

    public StockManager(GamoEconPro plugin) {
        this.plugin = plugin;
        plugin.getLogger().info("StockManager initialized.");
    }

    public void registerStock(String companyName) {
        if (!stocks.containsKey(companyName.toLowerCase())) {
            stocks.put(companyName.toLowerCase(), new Stock(companyName));
        }
    }

    public Collection<Stock> getAllStocks() {
        return stocks.values();
    }

    public boolean invest(Player player, String companyName, int shares) {
        Stock stock = stocks.get(companyName.toLowerCase());
        if (stock == null) return false;

        double cost = stock.getCurrentPrice() * shares;
        double tax = cost * 0.07;
        double total = cost + tax;

        if (!plugin.getEconomyManager().removeBalance(player.getUniqueId(), total)) return false;

        plugin.getTreasuryManager().addToTreasury(tax);
        stock.updatePrice(true, total);

        playerHoldings
                .computeIfAbsent(player.getUniqueId(), k -> new HashMap<>())
                .merge(companyName.toLowerCase(), shares, Integer::sum);

        return true;
    }

    public boolean sellShares(Player player, String companyName, int shares) {
        UUID uuid = player.getUniqueId();
        Map<String, Integer> holdings = playerHoldings.getOrDefault(uuid, new HashMap<>());

        if (!holdings.containsKey(companyName.toLowerCase()) || holdings.get(companyName.toLowerCase()) < shares)
            return false;

        Stock stock = stocks.get(companyName.toLowerCase());
        double value = stock.getCurrentPrice() * shares;
        double tax = value * 0.10;
        double net = value - tax;

        plugin.getEconomyManager().addBalance(uuid, net);
        plugin.getTreasuryManager().addToTreasury(tax);

        holdings.put(companyName.toLowerCase(), holdings.get(companyName.toLowerCase()) - shares);
        if (holdings.get(companyName.toLowerCase()) <= 0) {
            holdings.remove(companyName.toLowerCase());
        }

        stock.updatePrice(false, value);
        return true;
    }

    public Map<String, Integer> getHoldings(UUID uuid) {
        return playerHoldings.getOrDefault(uuid, new HashMap<>());
    }

    public Stock getStock(String name) {
        return stocks.get(name.toLowerCase());
    }

    // New method to update stock price based on shop transaction
    public void updateStockFromShop(UUID ownerUUID, double transactionAmount, boolean isBuy) {
        // Find all businesses owned by this player
        List<String> businesses = plugin.getBusinessManager().getBusinesses(ownerUUID);

        for (String businessName : businesses) {
            Stock stock = stocks.get(businessName.toLowerCase());
            if (stock != null) {
                stock.updatePrice(isBuy, transactionAmount);
            }
        }
    }
}