package com.gamo.gamoeconpro.economy;

import com.gamo.gamoeconpro.GamoEconPro;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class TreasuryManager {

    private final GamoEconPro plugin;
    private double treasuryBalance = 0.0;
    private UUID mayorUUID;
    private final Map<String, Double> taxRates = new ConcurrentHashMap<>();

    public TreasuryManager(GamoEconPro plugin) {
        this.plugin = plugin;
        loadTreasuryData();
    }

    public void loadTreasuryData() {
        try {
            FileConfiguration config = plugin.getConfig();

            // Load treasury balance from config first, then from DataManager if available
            treasuryBalance = config.getDouble("treasury.balance", 0.0);

            // If DataManager is available and has treasury data, use that instead
            if (plugin.getDataManager() != null) {
                double dataManagerBalance = plugin.getDataManager().loadTreasuryBalance();
                if (dataManagerBalance > 0 || treasuryBalance == 0.0) {
                    treasuryBalance = dataManagerBalance;
                }
            }

            // Load mayor UUID
            String mayorUuidString = config.getString("mayor-uuid");
            if (mayorUuidString != null && !mayorUuidString.isEmpty()) {
                try {
                    mayorUUID = UUID.fromString(mayorUuidString);
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("Invalid mayor UUID in config: " + mayorUuidString);
                    mayorUUID = null;
                }
            }

            // Load tax rates with default values
            taxRates.clear();
            taxRates.put("Transfer", config.getDouble("taxes.transfer", 15.0));
            taxRates.put("Registration", config.getDouble("taxes.registration", 1500.0));
            taxRates.put("Transaction", config.getDouble("taxes.transaction", 5.0));
            taxRates.put("FinancialExp", config.getDouble("taxes.financial-exp", 7.0));
            taxRates.put("FinancialRev", config.getDouble("taxes.financial-rev", 10.0));
            taxRates.put("Employment", config.getDouble("taxes.employment", 2.0));
            taxRates.put("Entertainment", config.getDouble("taxes.entertainment", 15.0));
            taxRates.put("Bank", config.getDouble("taxes.bank", 2.5));

        } catch (Exception e) {
            plugin.getLogger().severe("Failed to load treasury data: " + e.getMessage());
            // Set default values on error
            treasuryBalance = 0.0;
            mayorUUID = null;
            setDefaultTaxRates();
        }
    }

    private void setDefaultTaxRates() {
        taxRates.clear();
        taxRates.put("Transfer", 15.0);
        taxRates.put("Registration", 1500.0);
        taxRates.put("Transaction", 5.0);
        taxRates.put("FinancialExp", 7.0);
        taxRates.put("FinancialRev", 10.0);
        taxRates.put("Employment", 2.0);
        taxRates.put("Entertainment", 15.0);
        taxRates.put("Bank", 2.5);
    }

    public double getTreasuryBalance() {
        return treasuryBalance;
    }

    public synchronized void addToTreasury(double amount) {
        if (amount > 0) {
            treasuryBalance += amount;
            saveTreasuryBalance();
        }
    }

    public synchronized boolean removeFromTreasury(double amount) {
        if (amount <= 0) return false;

        if (treasuryBalance >= amount) {
            treasuryBalance -= amount;
            saveTreasuryBalance();
            return true;
        }
        return false;
    }

    private void saveTreasuryBalance() {
        try {
            // Save to plugin config
            plugin.getConfig().set("treasury.balance", treasuryBalance);
            plugin.saveConfig();

            // Also save via data manager if available
            if (plugin.getDataManager() != null) {
                plugin.getDataManager().saveTreasuryBalance(treasuryBalance);
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to save treasury balance: " + e.getMessage());
        }
    }

    public void setMayor(UUID uuid) {
        this.mayorUUID = uuid;
        try {
            if (uuid != null) {
                plugin.getConfig().set("mayor-uuid", uuid.toString());
            } else {
                plugin.getConfig().set("mayor-uuid", null);
            }
            plugin.saveConfig();
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to save mayor UUID: " + e.getMessage());
        }
    }

    public UUID getMayor() {
        return mayorUUID;
    }

    public boolean isMayor(UUID uuid) {
        if (uuid == null) return false;
        return mayorUUID != null && mayorUUID.equals(uuid);
    }

    public double getTax(String taxType) {
        if (taxType == null || taxType.isEmpty()) return 0.0;
        return taxRates.getOrDefault(taxType, 0.0);
    }

    public boolean setTax(String taxType, double value) {
        if (taxType == null || taxType.isEmpty() || value < 0) return false;

        try {
            taxRates.put(taxType, value);
            String configKey = "taxes." + taxType.toLowerCase().replace("_", "-");
            plugin.getConfig().set(configKey, value);
            plugin.saveConfig();
            return true;
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to save tax rate for " + taxType + ": " + e.getMessage());
            return false;
        }
    }

    public Map<String, Double> getAllTaxes() {
        return new HashMap<>(taxRates); // Return defensive copy
    }

    // Utility methods
    public boolean hasMayor() {
        return mayorUUID != null;
    }

    public void clearMayor() {
        setMayor(null);
    }

    // Method to calculate tax amount
    public double calculateTax(String taxType, double amount) {
        if (amount <= 0) return 0.0;
        double rate = getTax(taxType);
        return amount * (rate / 100.0); // Convert percentage to decimal
    }

    // Method to apply tax and add to treasury
    public double applyTax(String taxType, double amount) {
        double tax = calculateTax(taxType, amount);
        if (tax > 0) {
            addToTreasury(tax);
        }
        return tax;
    }

    // Method to get formatted tax rate string
    public String getFormattedTaxRate(String taxType) {
        double rate = getTax(taxType);
        return String.format("%.2f%%", rate);
    }

    // Method to transfer treasury funds (mayor function)
    public boolean transferTreasuryFunds(UUID recipient, double amount) {
        if (recipient == null || amount <= 0) return false;

        if (removeFromTreasury(amount)) {
            plugin.getEconomyManager().addBalance(recipient, amount);
            return true;
        }
        return false;
    }
}