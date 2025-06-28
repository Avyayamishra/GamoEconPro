package com.gamo.gamoeconpro.commands;

import com.gamo.gamoeconpro.GamoEconPro;
import com.gamo.gamoeconpro.economy.EconomyManager;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class EconomyCommands implements CommandExecutor, TabCompleter {

    private final GamoEconPro plugin;
    private final EconomyManager economy;

    public EconomyCommands(GamoEconPro plugin) {
        this.plugin = plugin;
        this.economy = plugin.getEconomyManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {

        if (cmd.getName().equalsIgnoreCase("rupiya")) {
            if (!(sender instanceof ConsoleCommandSender) && !sender.hasPermission("gamoecon.admin")) {
                sender.sendMessage("§cOnly admins can use this command.");
                return true;
            }
            if (args.length != 3) {
                sender.sendMessage("§cUsage: /rupiya <add|remove> <player> <amount>");
                return true;
            }

            String action = args[0];
            OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
            double amount;

            try {
                amount = Double.parseDouble(args[2]);
            } catch (NumberFormatException e) {
                sender.sendMessage("§cInvalid amount.");
                return true;
            }

            UUID uuid = target.getUniqueId();
            if (action.equalsIgnoreCase("add")) {
                economy.addBalance(uuid, amount);
                sender.sendMessage("§aAdded ₹" + amount + " to " + target.getName());
            } else if (action.equalsIgnoreCase("remove")) {
                if (!economy.removeBalance(uuid, amount)) {
                    sender.sendMessage("§cPlayer does not have enough balance.");
                } else {
                    sender.sendMessage("§aRemoved ₹" + amount + " from " + target.getName());
                }
            } else {
                sender.sendMessage("§cUsage: /rupiya <add|remove> <player> <amount>");
            }
            return true;
        }

        if (cmd.getName().equalsIgnoreCase("khata")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage("§cOnly players can use this command.");
                return true;
            }
            Player player = (Player) sender;
            double balance = economy.getBalance(player.getUniqueId());
            player.sendMessage("§6Your balance: §a₹" + String.format("%.2f", balance));
            return true;
        }

        if (cmd.getName().equalsIgnoreCase("bheje")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage("§cOnly players can use this command.");
                return true;
            }

            if (args.length != 2) {
                sender.sendMessage("§cUsage: /bheje <player> <amount>");
                return true;
            }

            Player senderPlayer = (Player) sender;
            OfflinePlayer receiver = Bukkit.getOfflinePlayer(args[0]);
            double amount;

            try {
                amount = Double.parseDouble(args[1]);
            } catch (NumberFormatException e) {
                sender.sendMessage("§cInvalid amount.");
                return true;
            }

            UUID senderUUID = senderPlayer.getUniqueId();
            UUID receiverUUID = receiver.getUniqueId();

            boolean success = economy.transferBalance(senderUUID, receiverUUID, amount, 15.0);
            if (success) {
                sender.sendMessage("§aTransferred ₹" + amount + " to " + receiver.getName() + " (15% tax applied)");
            } else {
                sender.sendMessage("§cYou don't have enough money (₹" + amount + " + tax)");
            }
            return true;
        }

        return false;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        String cmdName = cmd.getName().toLowerCase();

        if (cmdName.equals("rupiya")) {
            if (!(sender instanceof ConsoleCommandSender) && !sender.hasPermission("gamoecon.admin")) {
                return completions;
            }

            if (args.length == 1) {
                // First argument - action
                return Arrays.asList("add", "remove").stream()
                        .filter(action -> action.startsWith(args[0].toLowerCase()))
                        .collect(Collectors.toList());

            } else if (args.length == 2) {
                // Second argument - player names
                return Bukkit.getOnlinePlayers().stream()
                        .map(Player::getName)
                        .filter(name -> name.toLowerCase().startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList());

            } else if (args.length == 3) {
                // Third argument - amount suggestions
                return Arrays.asList("100", "500", "1000", "5000", "10000").stream()
                        .filter(amount -> amount.startsWith(args[2]))
                        .collect(Collectors.toList());
            }

        } else if (cmdName.equals("bheje")) {
            if (!(sender instanceof Player)) {
                return completions;
            }

            Player player = (Player) sender;

            if (args.length == 1) {
                // First argument - player names (exclude self)
                return Bukkit.getOnlinePlayers().stream()
                        .filter(p -> !p.equals(player))
                        .map(Player::getName)
                        .filter(name -> name.toLowerCase().startsWith(args[0].toLowerCase()))
                        .collect(Collectors.toList());

            } else if (args.length == 2) {
                // Second argument - amount suggestions based on player's balance
                List<String> amounts = new ArrayList<>();
                double balance = economy.getBalance(player.getUniqueId());
                double taxRate = 0.15; // 15% tax

                // Suggest amounts that player can afford including tax
                if (balance >= 100 * (1 + taxRate)) amounts.add("100");
                if (balance >= 500 * (1 + taxRate)) amounts.add("500");
                if (balance >= 1000 * (1 + taxRate)) amounts.add("1000");
                if (balance >= 5000 * (1 + taxRate)) amounts.add("5000");

                return amounts.stream()
                        .filter(amount -> amount.startsWith(args[1]))
                        .collect(Collectors.toList());
            }
        }

        // khata command doesn't need tab completion as it has no arguments

        return completions;
    }
}