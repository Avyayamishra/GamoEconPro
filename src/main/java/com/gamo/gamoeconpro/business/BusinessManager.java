package com.gamo.gamoeconpro.business;

import com.gamo.gamoeconpro.GamoEconPro;
import com.gamo.gamoeconpro.stockmarket.StockManager;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.*;

public class BusinessManager {
    private final GamoEconPro plugin;
    private final Map<UUID, List<String>> registeredBusinesses = new HashMap<>();
    private final Map<UUID, String> pendingApplications = new HashMap<>();

    public BusinessManager(GamoEconPro plugin) {
        this.plugin = plugin;
    }

    public boolean hasPendingApplication(UUID uuid) {
        return pendingApplications.containsKey(uuid);
    }

    public void applyForBusiness(UUID uuid, String businessName) {
        pendingApplications.put(uuid, businessName);
    }

    public void cancelApplication(UUID uuid) {
        pendingApplications.remove(uuid);
    }

    public boolean approveBusiness(UUID uuid) {
        if (!pendingApplications.containsKey(uuid)) return false;

        String businessName = pendingApplications.get(uuid);
        registeredBusinesses.computeIfAbsent(uuid, k -> new ArrayList<>()).add(businessName);
        pendingApplications.remove(uuid);

        // Register the business in the stock market
        StockManager stockManager = plugin.getStockManager();
        if (stockManager != null) {
            stockManager.registerStock(businessName);
        }

        // Give the player their business license item
        Player player = Bukkit.getPlayer(uuid);
        if (player != null) {
            player.getInventory().addItem(BusinessLicenseItem.create(businessName));
        }

        return true;
    }

    public boolean cancelBusiness(UUID uuid, String businessName) {
        if (!registeredBusinesses.containsKey(uuid)) return false;

        List<String> businesses = registeredBusinesses.get(uuid);
        boolean removed = businesses.remove(businessName);

        if (businesses.isEmpty()) {
            registeredBusinesses.remove(uuid);
        }

        return removed;
    }

    public List<String> getBusinesses(UUID uuid) {
        return registeredBusinesses.getOrDefault(uuid, new ArrayList<>());
    }

    public Map<String, Object> serialize() {
        Map<String, Object> data = new HashMap<>();

        // Serialize registered businesses
        Map<String, List<String>> regBusinesses = new HashMap<>();
        for (Map.Entry<UUID, List<String>> entry : registeredBusinesses.entrySet()) {
            regBusinesses.put(entry.getKey().toString(), entry.getValue());
        }
        data.put("registered", regBusinesses);

        // Serialize pending applications
        Map<String, String> pendingApps = new HashMap<>();
        for (Map.Entry<UUID, String> entry : pendingApplications.entrySet()) {
            pendingApps.put(entry.getKey().toString(), entry.getValue());
        }
        data.put("pending", pendingApps);

        return data;
    }

    @SuppressWarnings("unchecked")
    public void deserialize(Map<String, Object> data) {
        registeredBusinesses.clear();
        pendingApplications.clear();

        // Deserialize registered businesses
        Map<String, List<String>> regBusinesses = (Map<String, List<String>>) data.get("registered");
        if (regBusinesses != null) {
            for (Map.Entry<String, List<String>> entry : regBusinesses.entrySet()) {
                try {
                    UUID uuid = UUID.fromString(entry.getKey());
                    registeredBusinesses.put(uuid, entry.getValue());
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("Invalid UUID in registered businesses: " + entry.getKey());
                }
            }
        }

        // Deserialize pending applications
        Map<String, String> pendingApps = (Map<String, String>) data.get("pending");
        if (pendingApps != null) {
            for (Map.Entry<String, String> entry : pendingApps.entrySet()) {
                try {
                    UUID uuid = UUID.fromString(entry.getKey());
                    pendingApplications.put(uuid, entry.getValue());
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("Invalid UUID in pending applications: " + entry.getKey());
                }
            }
        }

        // Register stocks for all businesses
        StockManager stockManager = plugin.getStockManager();
        if (stockManager != null) {
            for (List<String> businessNames : registeredBusinesses.values()) {
                for (String name : businessNames) {
                    stockManager.registerStock(name);
                }
            }
        }
    }
}