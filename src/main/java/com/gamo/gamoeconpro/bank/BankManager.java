package com.gamo.gamoeconpro.bank;

import com.gamo.gamoeconpro.GamoEconPro;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class BankManager {

    private final GamoEconPro plugin;
    private final Map<UUID, Double> accounts = new ConcurrentHashMap<>();
    private final Map<UUID, Long> lastInterestTime = new ConcurrentHashMap<>();

    public BankManager(GamoEconPro plugin) {
        this.plugin = plugin;
        loadAccounts();
    }

    // Getter methods
    public Map<UUID, Double> getAccounts() {
        return new HashMap<>(accounts); // Return defensive copy
    }

    public long getLastInterestTime(UUID uuid) {
        if (uuid == null) return System.currentTimeMillis();
        return lastInterestTime.getOrDefault(uuid, System.currentTimeMillis());
    }

    // Account management methods
    public boolean hasAccount(UUID uuid) {
        if (uuid == null) return false;
        return accounts.containsKey(uuid);
    }

    public boolean createAccount(UUID uuid) {
        if (uuid == null || hasAccount(uuid)) return false;

        // Check if player has enough money for the account creation fee
        if (plugin.getEconomyManager().getBalance(uuid) < 500) return false;

        // Deduct the fee and add to treasury
        if (!plugin.getEconomyManager().removeBalance(uuid, 500)) return false;

        plugin.getTreasuryManager().addToTreasury(500);
        accounts.put(uuid, 0.0);
        lastInterestTime.put(uuid, System.currentTimeMillis());
        saveAccounts();
        return true;
    }

    public boolean deposit(UUID uuid, double amount) {
        if (uuid == null || !hasAccount(uuid) || amount <= 0) return false;

        if (plugin.getEconomyManager().removeBalance(uuid, amount)) {
            double currentBalance = accounts.getOrDefault(uuid, 0.0);
            accounts.put(uuid, currentBalance + amount);
            saveAccounts();
            return true;
        }
        return false;
    }

    public boolean withdraw(UUID uuid, double amount) {
        if (uuid == null || !hasAccount(uuid) || amount <= 0) return false;

        double currentBalance = accounts.getOrDefault(uuid, 0.0);
        if (currentBalance < amount) return false;

        accounts.put(uuid, currentBalance - amount);
        plugin.getEconomyManager().addBalance(uuid, amount);
        saveAccounts();
        return true;
    }

    public boolean transfer(UUID sender, UUID receiver, double amount) {
        if (sender == null || receiver == null || sender.equals(receiver)) return false;
        if (!hasAccount(sender) || !hasAccount(receiver) || amount <= 0) return false;

        // Get tax rate from treasury manager
        double taxRate = plugin.getTreasuryManager().getTax("Bank") / 100.0; // Convert percentage to decimal
        double tax = amount * taxRate;
        double total = amount + tax;

        double senderBalance = accounts.getOrDefault(sender, 0.0);
        if (senderBalance < total) return false;

        double receiverBalance = accounts.getOrDefault(receiver, 0.0);

        accounts.put(sender, senderBalance - total);
        accounts.put(receiver, receiverBalance + amount);
        plugin.getTreasuryManager().addToTreasury(tax);
        saveAccounts();
        return true;
    }

    public double getBalance(UUID uuid) {
        if (uuid == null) return 0.0;
        return accounts.getOrDefault(uuid, 0.0);
    }

    public void applyInterest() {
        long now = System.currentTimeMillis();
        boolean changed = false;
        final double INTEREST_RATE = 0.0175; // 1.75% per hour
        final long INTEREST_INTERVAL = 3600000; // 1 hour in milliseconds

        for (Map.Entry<UUID, Double> entry : accounts.entrySet()) {
            UUID uuid = entry.getKey();
            double balance = entry.getValue();

            long lastTime = lastInterestTime.getOrDefault(uuid, now);
            if (now - lastTime >= INTEREST_INTERVAL && balance > 0) {
                double interest = balance * INTEREST_RATE;
                accounts.put(uuid, balance + interest);
                lastInterestTime.put(uuid, now);
                changed = true;
            }
        }

        if (changed) {
            saveAccounts();
        }
    }

    public void saveAccounts() {
        try {
            Map<UUID, BankAccountData> data = new HashMap<>();
            for (Map.Entry<UUID, Double> entry : accounts.entrySet()) {
                UUID uuid = entry.getKey();
                double balance = entry.getValue();
                long lastTime = lastInterestTime.getOrDefault(uuid, 0L);
                data.put(uuid, new BankAccountData(balance, lastTime));
            }
            plugin.getDataManager().saveBankData(data);
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to save bank accounts: " + e.getMessage());
        }
    }

    public void loadAccounts() {
        try {
            Map<UUID, BankAccountData> data = plugin.getDataManager().loadBankData();
            if (data != null) {
                accounts.clear();
                lastInterestTime.clear();

                for (Map.Entry<UUID, BankAccountData> entry : data.entrySet()) {
                    UUID uuid = entry.getKey();
                    BankAccountData accountData = entry.getValue();

                    if (uuid != null && accountData != null) {
                        accounts.put(uuid, Math.max(0.0, accountData.getBalance())); // Ensure non-negative balance
                        lastInterestTime.put(uuid, accountData.getLastInterestTime());
                    }
                }
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to load bank accounts: " + e.getMessage());
        }
    }

    // Utility method to close an account
    public boolean closeAccount(UUID uuid) {
        if (uuid == null || !hasAccount(uuid)) return false;

        double balance = accounts.get(uuid);
        if (balance > 0) {
            plugin.getEconomyManager().addBalance(uuid, balance);
        }

        accounts.remove(uuid);
        lastInterestTime.remove(uuid);
        saveAccounts();
        return true;
    }

    // Method to get total bank system balance
    public double getTotalBankBalance() {
        return accounts.values().stream().mapToDouble(Double::doubleValue).sum();
    }
}