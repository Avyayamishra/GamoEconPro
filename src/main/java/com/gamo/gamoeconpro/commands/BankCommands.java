package com.gamo.gamoeconpro.commands;

import com.gamo.gamoeconpro.GamoEconPro;
import com.gamo.gamoeconpro.bank.BankManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class BankCommands implements CommandExecutor, TabCompleter {

    private final GamoEconPro plugin;
    private final BankManager bank;

    public BankCommands(GamoEconPro plugin) {
        this.plugin = plugin;
        this.bank = plugin.getBankManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {

        if (!(sender instanceof Player)) {
            sender.sendMessage("§cOnly players can use bank commands.");
            return true;
        }

        Player player = (Player) sender;
        UUID uuid = player.getUniqueId();

        if (args.length == 0) {
            double bal = bank.getBalance(uuid);
            player.sendMessage(ChatColor.GREEN + "🏦 Bank Balance: ₹" + String.format("%.2f", bal));
            return true;
        }

        if (args[0].equalsIgnoreCase("create")) {
            if (bank.hasAccount(uuid)) {
                player.sendMessage("§eYou already have a bank account.");
                return true;
            }

            boolean created = bank.createAccount(uuid);
            if (created) {
                player.sendMessage("§aBank account created successfully for ₹500.");
            } else {
                player.sendMessage("§cFailed to create account. You may not have enough money.");
            }
            return true;
        }

        if (args[0].equalsIgnoreCase("deposit")) {
            if (args.length != 2) {
                player.sendMessage("§cUsage: /bank deposit <amount>");
                return true;
            }

            double amt;
            try {
                amt = Double.parseDouble(args[1]);
            } catch (NumberFormatException e) {
                player.sendMessage("§cInvalid amount.");
                return true;
            }

            if (amt <= 0) {
                player.sendMessage("§cAmount must be greater than zero.");
                return true;
            }

            if (!bank.hasAccount(uuid)) {
                player.sendMessage("§cYou don't have a bank account. Use /bank create.");
                return true;
            }

            boolean success = bank.deposit(uuid, amt);
            player.sendMessage(success
                    ? "§aDeposited ₹" + amt + " to your bank account."
                    : "§cFailed. Insufficient balance.");
            return true;
        }

        if (args[0].equalsIgnoreCase("withdraw")) {
            if (args.length != 2) {
                player.sendMessage("§cUsage: /bank withdraw <amount>");
                return true;
            }

            double amt;
            try {
                amt = Double.parseDouble(args[1]);
            } catch (NumberFormatException e) {
                player.sendMessage("§cInvalid amount.");
                return true;
            }

            if (amt <= 0) {
                player.sendMessage("§cAmount must be greater than zero.");
                return true;
            }

            if (!bank.hasAccount(uuid)) {
                player.sendMessage("§cYou don't have a bank account.");
                return true;
            }

            boolean success = bank.withdraw(uuid, amt);
            player.sendMessage(success
                    ? "§aWithdrew ₹" + amt + " from your bank account."
                    : "§cInsufficient bank funds.");
            return true;
        }

        if (args[0].equalsIgnoreCase("transfer")) {
            if (args.length != 3) {
                player.sendMessage("§cUsage: /bank transfer <player> <amount>");
                return true;
            }

            OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
            double amt;

            try {
                amt = Double.parseDouble(args[2]);
            } catch (NumberFormatException e) {
                player.sendMessage("§cInvalid amount.");
                return true;
            }

            if (amt <= 0) {
                player.sendMessage("§cAmount must be greater than zero.");
                return true;
            }

            if (!bank.hasAccount(uuid)) {
                player.sendMessage("§cYou don't have a bank account.");
                return true;
            }

            if (!bank.hasAccount(target.getUniqueId())) {
                player.sendMessage("§cTarget player doesn't have a bank account.");
                return true;
            }

            boolean success = bank.transfer(uuid, target.getUniqueId(), amt);
            if (success) {
                player.sendMessage("§aTransferred ₹" + amt + " to " + target.getName() + "'s bank account.");
            } else {
                player.sendMessage("§cTransfer failed. Check your balance.");
            }
            return true;
        }

        player.sendMessage("§cInvalid usage. Try: /bank, /bank create, /bank deposit <amt>, /bank withdraw <amt>, /bank transfer <player> <amt>");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (!(sender instanceof Player)) {
            return completions;
        }

        Player player = (Player) sender;
        UUID uuid = player.getUniqueId();

        if (args.length == 1) {
            // First argument - bank subcommands
            List<String> subcommands = new ArrayList<>();

            // Always show basic commands
            subcommands.add("create");
            subcommands.add("deposit");
            subcommands.add("withdraw");
            subcommands.add("transfer");

            // Filter based on what user has typed
            return subcommands.stream()
                    .filter(sub -> sub.toLowerCase().startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());

        } else if (args.length == 2) {
            // Second argument
            if (args[0].equalsIgnoreCase("transfer")) {
                // For transfer command, suggest online players with bank accounts
                return Bukkit.getOnlinePlayers().stream()
                        .filter(p -> !p.equals(player)) // Don't suggest self
                        .filter(p -> bank.hasAccount(p.getUniqueId())) // Only players with bank accounts
                        .map(Player::getName)
                        .filter(name -> name.toLowerCase().startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList());

            } else if (args[0].equalsIgnoreCase("deposit") || args[0].equalsIgnoreCase("withdraw")) {
                // For deposit/withdraw, suggest amounts based on player's balance
                List<String> amounts = new ArrayList<>();

                if (args[0].equalsIgnoreCase("deposit")) {
                    // Suggest based on wallet balance
                    double walletBalance = plugin.getEconomyManager().getBalance(uuid);
                    if (walletBalance >= 100) amounts.add("100");
                    if (walletBalance >= 500) amounts.add("500");
                    if (walletBalance >= 1000) amounts.add("1000");
                    if (walletBalance >= 5000) amounts.add("5000");
                } else {
                    // Suggest based on bank balance
                    double bankBalance = bank.getBalance(uuid);
                    if (bankBalance >= 100) amounts.add("100");
                    if (bankBalance >= 500) amounts.add("500");
                    if (bankBalance >= 1000) amounts.add("1000");
                    if (bankBalance >= 5000) amounts.add("5000");
                }

                return amounts.stream()
                        .filter(amount -> amount.startsWith(args[1]))
                        .collect(Collectors.toList());
            }

        } else if (args.length == 3 && args[0].equalsIgnoreCase("transfer")) {
            // Third argument for transfer - suggest amounts based on bank balance
            List<String> amounts = new ArrayList<>();
            double bankBalance = bank.getBalance(uuid);

            if (bankBalance >= 100) amounts.add("100");
            if (bankBalance >= 500) amounts.add("500");
            if (bankBalance >= 1000) amounts.add("1000");
            if (bankBalance >= 5000) amounts.add("5000");

            return amounts.stream()
                    .filter(amount -> amount.startsWith(args[2]))
                    .collect(Collectors.toList());
        }

        return completions;
    }
}