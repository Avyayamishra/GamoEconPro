package com.gamo.gamoeconpro.commands;

import com.gamo.gamoeconpro.GamoEconPro;
import com.gamo.gamoeconpro.stockmarket.Stock;
import com.gamo.gamoeconpro.stockmarket.StockManager;
import org.bukkit.ChatColor;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class StockCommands implements CommandExecutor, TabCompleter {

    private final GamoEconPro plugin;
    private final StockManager stockManager;

    public StockCommands(GamoEconPro plugin) {
        this.plugin = plugin;
        this.stockManager = plugin.getStockManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {

        if (!(sender instanceof Player)) {
            sender.sendMessage("§cOnly players can use stock commands.");
            return true;
        }

        Player player = (Player) sender;

        if (args.length == 0 || args[0].equalsIgnoreCase("companies")) {
            player.sendMessage(ChatColor.YELLOW + "📈 Listed Companies:");
            for (Stock stock : stockManager.getAllStocks()) {
                player.sendMessage(" §6• " + stock.getCompanyName() +
                        " - ₹" + String.format("%.2f", stock.getCurrentPrice()) + "/share");
            }
            return true;
        }

        if (args[0].equalsIgnoreCase("invest")) {
            if (args.length != 3) {
                player.sendMessage("§cUsage: /stocks invest <name> <shares>");
                return true;
            }

            String name = args[1];
            int shares;

            try {
                shares = Integer.parseInt(args[2]);
            } catch (NumberFormatException e) {
                player.sendMessage("§cInvalid number of shares.");
                return true;
            }

            if (shares <= 0) {
                player.sendMessage("§cYou must buy at least 1 share.");
                return true;
            }

            boolean success = stockManager.invest(player, name, shares);
            if (success) {
                player.sendMessage("§aSuccessfully bought " + shares + " shares in §e" + name);
            } else {
                player.sendMessage("§cInvestment failed. Check balance or company name.");
            }
            return true;
        }

        if (args[0].equalsIgnoreCase("dashboard")) {
            Map<String, Integer> holdings = stockManager.getHoldings(player.getUniqueId());

            if (holdings.isEmpty()) {
                player.sendMessage("§7You have no investments.");
                return true;
            }

            player.sendMessage(ChatColor.GREEN + "📊 Your Investments:");
            for (Map.Entry<String, Integer> entry : holdings.entrySet()) {
                Stock stock = stockManager.getStock(entry.getKey());
                if (stock == null) continue;
                double worth = stock.getCurrentPrice() * entry.getValue();
                player.sendMessage(" §6• " + stock.getCompanyName() +
                        " - " + entry.getValue() + " shares (₹" + String.format("%.2f", worth) + ")");
            }
            return true;
        }

        if (args[0].equalsIgnoreCase("sell")) {
            if (args.length != 3) {
                player.sendMessage("§cUsage: /stocks sell <name> <shares>");
                return true;
            }

            String name = args[1];
            int shares;

            try {
                shares = Integer.parseInt(args[2]);
            } catch (NumberFormatException e) {
                player.sendMessage("§cInvalid number of shares.");
                return true;
            }

            if (shares <= 0) {
                player.sendMessage("§cYou must sell at least 1 share.");
                return true;
            }

            boolean success = stockManager.sellShares(player, name, shares);
            if (success) {
                player.sendMessage("§aSuccessfully sold " + shares + " shares in §e" + name);
            } else {
                player.sendMessage("§cSale failed. Check your holdings or share count.");
            }
            return true;
        }

        player.sendMessage("§cUnknown stock command. Try: companies, invest, dashboard, sell");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            // First argument - stock subcommands
            List<String> subcommands = Arrays.asList("companies", "invest", "dashboard", "sell");
            for (String subcommand : subcommands) {
                if (subcommand.toLowerCase().startsWith(args[0].toLowerCase())) {
                    completions.add(subcommand);
                }
            }
        } else if (args.length == 2) {
            // Second argument
            if (args[0].equalsIgnoreCase("invest") || args[0].equalsIgnoreCase("sell")) {
                // Suggest company names
                for (Stock stock : stockManager.getAllStocks()) {
                    String companyName = stock.getCompanyName();
                    if (companyName.toLowerCase().startsWith(args[1].toLowerCase())) {
                        completions.add(companyName);
                    }
                }
            }
        } else if (args.length == 3) {
            // Third argument
            if (args[0].equalsIgnoreCase("invest")) {
                // Suggest share amounts for investing
                completions.add("1");
                completions.add("5");
                completions.add("10");
                completions.add("25");
                completions.add("50");
                completions.add("100");
            } else if (args[0].equalsIgnoreCase("sell")) {
                // For selling, we could suggest the number of shares they own
                // But since we need access to player's holdings, we'll suggest common amounts
                completions.add("1");
                completions.add("5");
                completions.add("10");
                completions.add("all");
            }
        }

        return completions;
    }
}