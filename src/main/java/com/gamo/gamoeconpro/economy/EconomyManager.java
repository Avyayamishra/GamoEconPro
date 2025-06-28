package com.gamo.gamoeconpro.economy;

import com.gamo.gamoeconpro.GamoEconPro;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class EconomyManager {

    private final GamoEconPro plugin;
    private final HashMap<UUID, Double> balances = new HashMap<>();

    public EconomyManager(GamoEconPro plugin) {
        this.plugin = plugin;
        // Don't load balances here, let DataManager handle it
    }

    public double getBalance(UUID uuid) {
        return balances.getOrDefault(uuid, 0.0);
    }

    public void setBalance(UUID uuid, double amount) {
        balances.put(uuid, Math.max(0, amount));
    }

    public boolean addBalance(UUID uuid, double amount) {
        if (amount <= 0) return false;
        double currentBalance = getBalance(uuid);
        setBalance(uuid, currentBalance + amount);
        return true;
    }

    public boolean removeBalance(UUID uuid, double amount) {
        if (amount <= 0) return false;
        double currentBalance = getBalance(uuid);
        if (currentBalance < amount) return false;
        setBalance(uuid, currentBalance - amount);
        return true;
    }

    public boolean hasBalance(UUID uuid, double amount) {
        return getBalance(uuid) >= amount;
    }

    public boolean transferBalance(UUID from, UUID to, double amount) {
        return transferBalance(from, to, amount, 0.0);
    }

    public boolean transferBalance(UUID from, UUID to, double amount, double taxRate) {
        if (amount <= 0) return false;

        double tax = amount * (taxRate / 100.0);
        double total = amount + tax;

        if (!removeBalance(from, total)) return false;

        addBalance(to, amount);
        if (tax > 0) {
            plugin.getTreasuryManager().addToTreasury(tax);
        }
        return true;
    }

    public HashMap<UUID, Double> getAllBalances() {
        return balances;
    }

    public void loadBalances(Map<UUID, Double> loadedBalances) {
        balances.clear();
        balances.putAll(loadedBalances);
    }
}