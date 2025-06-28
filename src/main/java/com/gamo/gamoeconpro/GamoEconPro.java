package com.gamo.gamoeconpro;

import com.gamo.gamoeconpro.bank.BankManager;
import com.gamo.gamoeconpro.business.BusinessManager;
import com.gamo.gamoeconpro.casino.CasinoManager;
import com.gamo.gamoeconpro.chestshop.ShopManager;
import com.gamo.gamoeconpro.chestshop.ShopSignListener;
import com.gamo.gamoeconpro.commands.*;
import com.gamo.gamoeconpro.economy.EconomyManager;
import com.gamo.gamoeconpro.economy.TreasuryManager;
import com.gamo.gamoeconpro.government.MayorManager;
import com.gamo.gamoeconpro.jobs.JobListener;
import com.gamo.gamoeconpro.jobs.JobManager;
import com.gamo.gamoeconpro.stockmarket.StockManager;
import com.gamo.gamoeconpro.storage.DataManager;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public class GamoEconPro extends JavaPlugin {
    private static GamoEconPro instance;
    private EconomyManager economyManager;
    private TreasuryManager treasuryManager;
    private DataManager dataManager;
    private StockManager stockManager;
    private JobManager jobManager;
    private BankManager bankManager;
    private CasinoManager casinoManager;
    private BusinessManager businessManager;
    private ShopManager shopManager;
    private MayorManager mayorManager;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        // Initialize data manager first
        dataManager = new DataManager(this);

        // Initialize core managers
        economyManager = new EconomyManager(this);
        treasuryManager = new TreasuryManager(this);
        mayorManager = new MayorManager(this);

        // Load all data after core managers are initialized
        dataManager.loadAllData();

        // Initialize other managers
        stockManager = new StockManager(this);
        jobManager = new JobManager(this);
        bankManager = new BankManager(this);
        casinoManager = new CasinoManager(this);
        businessManager = new BusinessManager(this);
        shopManager = new ShopManager(this);

        // Register commands with tab completers
        EconomyCommands economyCommands = new EconomyCommands(this);
        if (getCommand("rupiya") != null) {
            getCommand("rupiya").setExecutor(economyCommands);
            getCommand("rupiya").setTabCompleter(economyCommands);
        }
        if (getCommand("khata") != null) {
            getCommand("khata").setExecutor(economyCommands);
        }
        if (getCommand("bheje") != null) {
            getCommand("bheje").setExecutor(economyCommands);
            getCommand("bheje").setTabCompleter(economyCommands);
        }

        BusinessCommands businessCommands = new BusinessCommands(this);
        if (getCommand("registerbusiness") != null) {
            getCommand("registerbusiness").setExecutor(businessCommands);
            getCommand("registerbusiness").setTabCompleter(businessCommands);
        }
        if (getCommand("cancelapplication") != null) {
            getCommand("cancelapplication").setExecutor(businessCommands);
        }
        if (getCommand("cancelbusiness") != null) {
            getCommand("cancelbusiness").setExecutor(businessCommands);
            getCommand("cancelbusiness").setTabCompleter(businessCommands);
        }
        if (getCommand("viewbusinesses") != null) {
            getCommand("viewbusinesses").setExecutor(businessCommands);
            getCommand("viewbusinesses").setTabCompleter(businessCommands);
        }
        if (getCommand("approvebusiness") != null) {
            getCommand("approvebusiness").setExecutor(businessCommands);
            getCommand("approvebusiness").setTabCompleter(businessCommands);
        }

        CasinoCommands casinoCommands = new CasinoCommands(this);
        if (getCommand("coinflip") != null) {
            getCommand("coinflip").setExecutor(casinoCommands);
            getCommand("coinflip").setTabCompleter(casinoCommands);
        }
        if (getCommand("dicebet") != null) {
            getCommand("dicebet").setExecutor(casinoCommands);
            getCommand("dicebet").setTabCompleter(casinoCommands);
        }
        if (getCommand("blackjack") != null) {
            getCommand("blackjack").setExecutor(casinoCommands);
            getCommand("blackjack").setTabCompleter(casinoCommands);
        }

        StockCommands stockCommands = new StockCommands(this);
        if (getCommand("stocks") != null) {
            getCommand("stocks").setExecutor(stockCommands);
            getCommand("stocks").setTabCompleter(stockCommands);
        }

        JobCommands jobCommands = new JobCommands(this);
        if (getCommand("jobs") != null) {
            getCommand("jobs").setExecutor(jobCommands);
            getCommand("jobs").setTabCompleter(jobCommands);
        }

        BankCommands bankCommands = new BankCommands(this);
        if (getCommand("bank") != null) {
            getCommand("bank").setExecutor(bankCommands);
            getCommand("bank").setTabCompleter(bankCommands);
        }

        GovernmentCommands governmentCommands = new GovernmentCommands(this);
        if (getCommand("treasury") != null) {
            getCommand("treasury").setExecutor(governmentCommands);
            getCommand("treasury").setTabCompleter(governmentCommands);
        }
        if (getCommand("tax") != null) {
            getCommand("tax").setExecutor(governmentCommands);
            getCommand("tax").setTabCompleter(governmentCommands);
        }
        if (getCommand("mayor") != null) {
            getCommand("mayor").setExecutor(governmentCommands);
            getCommand("mayor").setTabCompleter(governmentCommands);
        }

        ShopCommands shopCommands = new ShopCommands(this);
        if (getCommand("shop") != null) {
            getCommand("shop").setExecutor(shopCommands);
            getCommand("shop").setTabCompleter(shopCommands);
        }

        // Register event listeners
        getServer().getPluginManager().registerEvents(new ShopSignListener(this), this);
        getServer().getPluginManager().registerEvents(new JobListener(this), this);

        // Schedule tasks
        scheduleTasks();

        getLogger().info("✅ GamoEconPro loaded successfully!");
    }

    private void scheduleTasks() {
        Bukkit.getScheduler().runTaskTimer(this, () -> {
            if (bankManager != null) {
                bankManager.applyInterest();
            }
        }, 0L, 72000L);

        Bukkit.getScheduler().runTaskTimer(this, () -> {
            if (shopManager != null) {
                shopManager.cleanupInvalidShops();
            }
        }, 0L, 36000L);

        Bukkit.getScheduler().runTaskTimer(this, () -> {
            if (mayorManager != null) {
                mayorManager.checkTermExpiration();
            }
        }, 0L, 12000L);
    }

    @Override
    public void onDisable() {
        if (dataManager != null) {
            dataManager.saveAllData();
        }
        getLogger().info("❌ GamoEconPro unloaded.");
    }

    public static GamoEconPro getInstance() {
        return instance;
    }

    public EconomyManager getEconomyManager() {
        return economyManager;
    }

    public TreasuryManager getTreasuryManager() {
        return treasuryManager;
    }

    public DataManager getDataManager() {
        return dataManager;
    }

    public StockManager getStockManager() {
        return stockManager;
    }

    public JobManager getJobManager() {
        return jobManager;
    }

    public BankManager getBankManager() {
        return bankManager;
    }

    public CasinoManager getCasinoManager() {
        return casinoManager;
    }

    public BusinessManager getBusinessManager() {
        return businessManager;
    }

    public ShopManager getShopManager() {
        return shopManager;
    }

    public MayorManager getMayorManager() {
        return mayorManager;
    }
}