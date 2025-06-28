package com.gamo.gamoeconpro.storage;

import com.gamo.gamoeconpro.GamoEconPro;
import com.gamo.gamoeconpro.bank.BankAccountData;
import com.gamo.gamoeconpro.business.BusinessManager;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class DataManager {
    private final GamoEconPro plugin;
    private final File dataFolder;

    public DataManager(GamoEconPro plugin) {
        this.plugin = plugin;
        this.dataFolder = plugin.getDataFolder();

        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }
    }

    public void loadAllData() {
        try {
            plugin.getLogger().info("Loading all data...");

            // Load economy data
            if (plugin.getEconomyManager() != null) {
                Map<UUID, Double> economyData = loadEconomyData();
                plugin.getEconomyManager().loadBalances(economyData);
            }

            // Load business data
            if (plugin.getBusinessManager() != null) {
                loadBusinessData(plugin.getBusinessManager());
            }

            // Load bank data
            if (plugin.getBankManager() != null) {
                plugin.getBankManager().loadAccounts();
            }

            plugin.getLogger().info("All data loaded successfully!");
        } catch (Exception e) {
            plugin.getLogger().severe("Error loading all data: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void saveAllData() {
        try {
            plugin.getLogger().info("Saving all data...");

            // Save economy data
            if (plugin.getEconomyManager() != null) {
                Map<UUID, Double> economyData = plugin.getEconomyManager().getAllBalances();
                saveEconomyData(economyData);
            }

            // Save business data
            if (plugin.getBusinessManager() != null) {
                saveBusinessData(plugin.getBusinessManager());
            }

            // Save bank data
            if (plugin.getBankManager() != null) {
                plugin.getBankManager().saveAccounts();
            }

            plugin.getLogger().info("All data saved successfully!");
        } catch (Exception e) {
            plugin.getLogger().severe("Error saving all data: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @SuppressWarnings("unchecked")
    public void loadBusinessData(BusinessManager businessManager) {
        File file = new File(dataFolder, "businesses.yml");
        if (!file.exists()) {
            plugin.getLogger().info("No business data file found, creating new one.");
            return;
        }

        try {
            FileConfiguration config = YamlConfiguration.loadConfiguration(file);
            Map<String, Object> data = (Map<String, Object>) config.get("data");
            if (data != null) {
                businessManager.deserialize(data);
                plugin.getLogger().info("Business data loaded successfully.");
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to load business data: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void saveBusinessData(BusinessManager businessManager) {
        File file = new File(dataFolder, "businesses.yml");
        FileConfiguration config = new YamlConfiguration();
        config.set("data", businessManager.serialize());

        try {
            config.save(file);
            plugin.getLogger().info("Business data saved successfully.");
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save business data: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void saveEconomyData(Map<UUID, Double> economyData) {
        CompletableFuture.runAsync(() -> {
            try {
                File economyFile = new File(dataFolder, "economy.yml");
                FileConfiguration config = new YamlConfiguration();

                config.set("balances", null); // Clear existing data

                for (Map.Entry<UUID, Double> entry : economyData.entrySet()) {
                    config.set("balances." + entry.getKey().toString(), entry.getValue());
                }

                config.save(economyFile);
                plugin.getLogger().info("Saved economy data for " + economyData.size() + " players");
            } catch (IOException e) {
                plugin.getLogger().severe("Could not save economy data: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    public Map<UUID, Double> loadEconomyData() {
        Map<UUID, Double> economyData = new HashMap<>();

        try {
            File economyFile = new File(dataFolder, "economy.yml");
            if (!economyFile.exists()) {
                plugin.getLogger().info("No economy data found, starting fresh.");
                return economyData;
            }

            FileConfiguration config = YamlConfiguration.loadConfiguration(economyFile);

            if (config.isConfigurationSection("balances")) {
                for (String uuidString : config.getConfigurationSection("balances").getKeys(false)) {
                    try {
                        UUID uuid = UUID.fromString(uuidString);
                        double balance = config.getDouble("balances." + uuidString, 0.0);
                        economyData.put(uuid, Math.max(0.0, balance)); // Ensure non-negative
                    } catch (IllegalArgumentException e) {
                        plugin.getLogger().warning("Invalid UUID in economy data: " + uuidString);
                    }
                }
            }

            plugin.getLogger().info("Loaded economy data for " + economyData.size() + " players");
        } catch (Exception e) {
            plugin.getLogger().severe("Could not load economy data: " + e.getMessage());
            e.printStackTrace();
        }

        return economyData;
    }

    // Method to save bank data
    public void saveBankData(Map<UUID, com.gamo.gamoeconpro.bank.BankAccountData> bankData) {
        CompletableFuture.runAsync(() -> {
            try {
                File bankFile = new File(dataFolder, "bank.yml");
                FileConfiguration config = new YamlConfiguration();

                config.set("accounts", null); // Clear existing data

                for (Map.Entry<UUID, com.gamo.gamoeconpro.bank.BankAccountData> entry : bankData.entrySet()) {
                    String uuidString = entry.getKey().toString();
                    com.gamo.gamoeconpro.bank.BankAccountData accountData = entry.getValue();

                    config.set("accounts." + uuidString + ".balance", accountData.getBalance());
                    config.set("accounts." + uuidString + ".lastInterestTime", accountData.getLastInterestTime());
                }

                config.save(bankFile);
                plugin.getLogger().info("Saved bank data for " + bankData.size() + " accounts");
            } catch (IOException e) {
                plugin.getLogger().severe("Could not save bank data: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    // Method to load bank data
    public Map<UUID, com.gamo.gamoeconpro.bank.BankAccountData> loadBankData() {
        Map<UUID, com.gamo.gamoeconpro.bank.BankAccountData> bankData = new HashMap<>();

        try {
            File bankFile = new File(dataFolder, "bank.yml");
            if (!bankFile.exists()) {
                plugin.getLogger().info("No bank data found, starting fresh.");
                return bankData;
            }

            FileConfiguration config = YamlConfiguration.loadConfiguration(bankFile);

            if (config.isConfigurationSection("accounts")) {
                for (String uuidString : config.getConfigurationSection("accounts").getKeys(false)) {
                    try {
                        UUID uuid = UUID.fromString(uuidString);
                        double balance = config.getDouble("accounts." + uuidString + ".balance", 0.0);
                        long lastInterestTime = config.getLong("accounts." + uuidString + ".lastInterestTime", System.currentTimeMillis());

                        // Ensure non-negative balance
                        balance = Math.max(0.0, balance);

                        bankData.put(uuid, new BankAccountData(balance, lastInterestTime));
                    } catch (IllegalArgumentException e) {
                        plugin.getLogger().warning("Invalid UUID in bank data: " + uuidString);
                    }
                }
            }

            plugin.getLogger().info("Loaded bank data for " + bankData.size() + " accounts");
        } catch (Exception e) {
            plugin.getLogger().severe("Could not load bank data: " + e.getMessage());
            e.printStackTrace();
        }

        return bankData;
    }

    // Method to save treasury balance
    public void saveTreasuryBalance(double balance) {
        CompletableFuture.runAsync(() -> {
            try {
                File treasuryFile = new File(dataFolder, "treasury.yml");
                FileConfiguration config;

                if (treasuryFile.exists()) {
                    config = YamlConfiguration.loadConfiguration(treasuryFile);
                } else {
                    config = new YamlConfiguration();
                }

                config.set("treasury.balance", balance);
                config.save(treasuryFile);

                plugin.getLogger().info("Treasury balance saved: " + balance);
            } catch (IOException e) {
                plugin.getLogger().severe("Could not save treasury balance: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    // Method to load treasury balance
    public double loadTreasuryBalance() {
        try {
            File treasuryFile = new File(dataFolder, "treasury.yml");
            if (!treasuryFile.exists()) {
                plugin.getLogger().info("No treasury data found, starting with 0 balance.");
                return 0.0;
            }

            FileConfiguration config = YamlConfiguration.loadConfiguration(treasuryFile);
            double balance = config.getDouble("treasury.balance", 0.0);

            plugin.getLogger().info("Treasury balance loaded: " + balance);
            return Math.max(0.0, balance); // Ensure non-negative
        } catch (Exception e) {
            plugin.getLogger().severe("Could not load treasury balance: " + e.getMessage());
            e.printStackTrace();
            return 0.0;
        }
    }

    public void createBackup() {
        CompletableFuture.runAsync(() -> {
            try {
                File backupFolder = new File(dataFolder, "backups");
                if (!backupFolder.exists()) {
                    backupFolder.mkdirs();
                }

                String timestamp = String.valueOf(System.currentTimeMillis());
                File timestampFolder = new File(backupFolder, timestamp);
                timestampFolder.mkdirs();

                copyFile(new File(dataFolder, "economy.yml"), new File(timestampFolder, "economy.yml"));
                copyFile(new File(dataFolder, "businesses.yml"), new File(timestampFolder, "businesses.yml"));
                copyFile(new File(dataFolder, "treasury.yml"), new File(timestampFolder, "treasury.yml"));
                copyFile(new File(dataFolder, "bank.yml"), new File(timestampFolder, "bank.yml"));

                plugin.getLogger().info("Created backup: " + timestamp);
            } catch (Exception e) {
                plugin.getLogger().severe("Could not create backup: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    private void copyFile(File source, File destination) throws IOException {
        if (source.exists()) {
            FileConfiguration config = YamlConfiguration.loadConfiguration(source);
            config.save(destination);
        }
    }
    public void saveJobData(UUID uuid, Map<String, Object> jobData) {
        CompletableFuture.runAsync(() -> {
            try {
                File jobFile = new File(dataFolder, "jobs/playerdata/" + uuid.toString() + ".yml");
                FileConfiguration config = new YamlConfiguration();
                config.set("data", jobData);
                config.save(jobFile);
            } catch (IOException e) {
                plugin.getLogger().severe("Could not save job data for " + uuid + ": " + e.getMessage());
            }
        });
    }

    public Map<UUID, Map<String, Object>> loadJobData() {
        Map<UUID, Map<String, Object>> jobData = new HashMap<>();
        File playerDataFolder = new File(dataFolder, "jobs/playerdata");

        if (!playerDataFolder.exists()) {
            playerDataFolder.mkdirs();
            return jobData;
        }

        File[] playerFiles = playerDataFolder.listFiles();
        if (playerFiles == null) return jobData;

        for (File file : playerFiles) {
            try {
                String fileName = file.getName();
                if (fileName.endsWith(".yml")) {
                    UUID uuid = UUID.fromString(fileName.substring(0, fileName.length() - 4));
                    FileConfiguration config = YamlConfiguration.loadConfiguration(file);
                    jobData.put(uuid, (Map<String, Object>) config.get("data"));
                }
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Invalid UUID in job data filename: " + file.getName());
            } catch (Exception e) {
                plugin.getLogger().severe("Error loading job data from " + file.getName() + ": " + e.getMessage());
            }
        }

        return jobData;
    }

    public void saveJobData(Map<UUID, Map<String, Object>> jobData) {
        CompletableFuture.runAsync(() -> {
            File playerDataFolder = new File(dataFolder, "jobs/playerdata");
            if (!playerDataFolder.exists()) {
                playerDataFolder.mkdirs();
            }

            // Save all player data
            for (Map.Entry<UUID, Map<String, Object>> entry : jobData.entrySet()) {
                try {
                    File jobFile = new File(playerDataFolder, entry.getKey().toString() + ".yml");
                    FileConfiguration config = new YamlConfiguration();
                    config.set("data", entry.getValue());
                    config.save(jobFile);
                } catch (IOException e) {
                    plugin.getLogger().severe("Could not save job data for " + entry.getKey() + ": " + e.getMessage());
                }
            }
        });
    }

}