package com.gamo.gamoeconpro.commands;

import com.gamo.gamoeconpro.GamoEconPro;
import com.gamo.gamoeconpro.economy.TreasuryManager;
import com.gamo.gamoeconpro.government.MayorManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.TimeUnit;

public class GovernmentCommands implements CommandExecutor, TabCompleter {

    private final GamoEconPro plugin;
    private final TreasuryManager treasury;
    private final MayorManager mayorManager;

    public GovernmentCommands(GamoEconPro plugin) {
        this.plugin = plugin;
        this.treasury = plugin.getTreasuryManager();
        this.mayorManager = plugin.getMayorManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (cmd.getName().equalsIgnoreCase("mayor")) {
            return handleMayorCommand(sender, args);
        } else if (cmd.getName().equalsIgnoreCase("treasury")) {
            return handleTreasuryCommand(sender, args);
        } else if (cmd.getName().equalsIgnoreCase("tax")) {
            return handleTaxCommand(sender, args);
        }
        return false;
    }

    private boolean handleMayorCommand(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this command.");
            return true;
        }

        Player player = (Player) sender;

        if (args.length == 0) {
            return showMayorInfo(player);
        }

        String action = args[0].toLowerCase();
        switch (action) {
            case "register":
                return handleMayorRegister(player);
            case "vote":
                return handleMayorVote(player, args);
            case "startelection":
                return handleStartElection(player);
            case "endelection":
                return handleEndElection(player);
            case "info":
                return showMayorInfo(player);
            case "force":
                return handleForceMayor(player, args);
            default:
                player.sendMessage(ChatColor.RED + "Usage: /mayor [register|vote|startelection|endelection|info|force]");
                return true;
        }
    }

    private boolean handleMayorRegister(Player player) {
        if (mayorManager.isElectionInProgress()) {
            player.sendMessage(ChatColor.RED + "Cannot register during an active election.");
            return true;
        }

        if (mayorManager.getCandidates().contains(player.getUniqueId())) {
            player.sendMessage(ChatColor.RED + "You are already registered as a candidate.");
            return true;
        }

        if (mayorManager.isCurrentMayor(player.getUniqueId()) &&
                mayorManager.getConsecutiveTerms(player.getUniqueId()) >= mayorManager.getMaxConsecutiveTerms()) {
            player.sendMessage(ChatColor.RED + "You have reached the maximum consecutive terms limit.");
            return true;
        }

        if (plugin.getEconomyManager().getBalance(player.getUniqueId()) < mayorManager.getCandidacyFee()) {
            player.sendMessage(ChatColor.RED + "You need ₹" + String.format("%.2f", mayorManager.getCandidacyFee()) + " to register as a candidate.");
            return true;
        }

        if (mayorManager.registerCandidate(player.getUniqueId())) {
            player.sendMessage(ChatColor.GREEN + "Successfully registered as a mayoral candidate! Fee: ₹" + String.format("%.2f", mayorManager.getCandidacyFee()));
            Bukkit.broadcastMessage(ChatColor.GOLD + "[Government] " + ChatColor.YELLOW + player.getName() + " has registered as a mayoral candidate!");
            return true;
        } else {
            player.sendMessage(ChatColor.RED + "Failed to register as candidate.");
            return true;
        }
    }

    private boolean handleMayorVote(Player player, String[] args) {
        if (!mayorManager.isElectionInProgress()) {
            player.sendMessage(ChatColor.RED + "No election is currently in progress.");
            return true;
        }

        if (args.length != 2) {
            player.sendMessage(ChatColor.RED + "Usage: /mayor vote <candidate>");
            return true;
        }

        String candidateName = args[1];
        Player candidate = Bukkit.getPlayer(candidateName);
        if (candidate == null) {
            OfflinePlayer offlineCandidate = Bukkit.getOfflinePlayer(candidateName);
            if (offlineCandidate.hasPlayedBefore()) {
                if (!mayorManager.getCandidates().contains(offlineCandidate.getUniqueId())) {
                    player.sendMessage(ChatColor.RED + "Player is not a registered candidate.");
                    return true;
                }

                if (mayorManager.vote(player.getUniqueId(), offlineCandidate.getUniqueId())) {
                    player.sendMessage(ChatColor.GREEN + "Your vote has been cast for " + candidateName + "!");
                    return true;
                } else {
                    player.sendMessage(ChatColor.RED + "Failed to cast vote.");
                    return true;
                }
            } else {
                player.sendMessage(ChatColor.RED + "Player not found.");
                return true;
            }
        }

        if (!mayorManager.getCandidates().contains(candidate.getUniqueId())) {
            player.sendMessage(ChatColor.RED + "Player is not a registered candidate.");
            return true;
        }

        if (mayorManager.vote(player.getUniqueId(), candidate.getUniqueId())) {
            player.sendMessage(ChatColor.GREEN + "Your vote has been cast for " + candidate.getName() + "!");
            return true;
        } else {
            player.sendMessage(ChatColor.RED + "Failed to cast vote.");
            return true;
        }
    }

    private boolean handleStartElection(Player player) {
        if (!player.hasPermission("gamoecon.admin") && !mayorManager.isCurrentMayor(player.getUniqueId())) {
            player.sendMessage(ChatColor.RED + "You don't have permission to start elections.");
            return true;
        }

        if (mayorManager.isElectionInProgress()) {
            player.sendMessage(ChatColor.RED + "An election is already in progress.");
            return true;
        }

        if (mayorManager.getCandidates().isEmpty()) {
            player.sendMessage(ChatColor.RED + "No candidates have registered for the election.");
            return true;
        }

        if (mayorManager.startElection()) {
            player.sendMessage(ChatColor.GREEN + "Election started successfully!");
            return true;
        } else {
            player.sendMessage(ChatColor.RED + "Failed to start election.");
            return true;
        }
    }

    private boolean handleEndElection(Player player) {
        if (!player.hasPermission("gamoecon.admin")) {
            player.sendMessage(ChatColor.RED + "You don't have permission to end elections.");
            return true;
        }

        if (!mayorManager.isElectionInProgress()) {
            player.sendMessage(ChatColor.RED + "No election is currently in progress.");
            return true;
        }

        if (mayorManager.endElection()) {
            player.sendMessage(ChatColor.GREEN + "Election ended successfully!");
            return true;
        } else {
            player.sendMessage(ChatColor.RED + "Failed to end election.");
            return true;
        }
    }

    private boolean showMayorInfo(Player player) {
        player.sendMessage(ChatColor.GOLD + "=== Mayor Information ===");

        UUID currentMayor = treasury.getMayor();
        if (currentMayor != null) {
            OfflinePlayer mayorPlayer = Bukkit.getOfflinePlayer(currentMayor);
            String mayorName = mayorPlayer.getName() != null ? mayorPlayer.getName() : "Unknown";
            player.sendMessage(ChatColor.YELLOW + "Current Mayor: " + ChatColor.WHITE + mayorName);
            player.sendMessage(ChatColor.YELLOW + "Consecutive Terms: " + ChatColor.WHITE + mayorManager.getConsecutiveTerms(currentMayor));

            long remainingTime = mayorManager.getRemainingTermTime();
            if (remainingTime > 0) {
                String timeLeft = formatTime(remainingTime);
                player.sendMessage(ChatColor.YELLOW + "Term Remaining: " + ChatColor.WHITE + timeLeft);
            } else {
                player.sendMessage(ChatColor.RED + "Term has expired!");
            }
        } else {
            player.sendMessage(ChatColor.RED + "No current mayor.");
        }

        if (mayorManager.isElectionInProgress()) {
            player.sendMessage(ChatColor.GOLD + "=== Election in Progress ===");
            long electionTime = mayorManager.getRemainingElectionTime();
            String timeLeft = formatTime(electionTime);
            player.sendMessage(ChatColor.YELLOW + "Election Time Remaining: " + ChatColor.WHITE + timeLeft);

            player.sendMessage(ChatColor.YELLOW + "Candidates:");
            for (UUID candidateUUID : mayorManager.getCandidates()) {
                OfflinePlayer candidate = Bukkit.getOfflinePlayer(candidateUUID);
                String candidateName = candidate.getName() != null ? candidate.getName() : "Unknown";
                int votes = mayorManager.getVoteCount().getOrDefault(candidateUUID, 0);
                player.sendMessage(ChatColor.WHITE + "- " + candidateName + " (" + votes + " votes)");
            }

            if (mayorManager.hasVoted(player.getUniqueId())) {
                UUID votedFor = mayorManager.getVote(player.getUniqueId());
                OfflinePlayer votedPlayer = Bukkit.getOfflinePlayer(votedFor);
                String votedName = votedPlayer.getName() != null ? votedPlayer.getName() : "Unknown";
                player.sendMessage(ChatColor.GREEN + "You voted for: " + votedName);
            } else {
                player.sendMessage(ChatColor.RED + "You haven't voted yet.");
            }
        }

        player.sendMessage(ChatColor.YELLOW + "Candidacy Fee: " + ChatColor.WHITE + "₹" + String.format("%.2f", mayorManager.getCandidacyFee()));
        player.sendMessage(ChatColor.YELLOW + "Max Consecutive Terms: " + ChatColor.WHITE + mayorManager.getMaxConsecutiveTerms());

        return true;
    }

    private boolean handleForceMayor(Player player, String[] args) {
        if (!player.hasPermission("gamoecon.admin")) {
            player.sendMessage(ChatColor.RED + "You don't have permission to force set mayor.");
            return true;
        }

        if (args.length != 2) {
            player.sendMessage(ChatColor.RED + "Usage: /mayor force <player>");
            return true;
        }

        String targetName = args[1];
        Player target = Bukkit.getPlayer(targetName);
        UUID targetUUID;

        if (target != null) {
            targetUUID = target.getUniqueId();
        } else {
            OfflinePlayer offlineTarget = Bukkit.getOfflinePlayer(targetName);
            if (!offlineTarget.hasPlayedBefore()) {
                player.sendMessage(ChatColor.RED + "Player not found.");
                return true;
            }
            targetUUID = offlineTarget.getUniqueId();
        }

        treasury.setMayor(targetUUID);
        player.sendMessage(ChatColor.GREEN + "Successfully set " + targetName + " as mayor.");
        Bukkit.broadcastMessage(ChatColor.GOLD + "[Government] " + ChatColor.YELLOW + targetName + " has been appointed as mayor!");

        return true;
    }

    private boolean handleTaxCommand(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this command.");
            return true;
        }

        Player player = (Player) sender;

        if (!treasury.isMayor(player.getUniqueId()) && !player.hasPermission("gamoecon.admin")) {
            player.sendMessage(ChatColor.RED + "You don't have permission to manage taxes.");
            return true;
        }

        if (args.length == 0) {
            player.sendMessage(ChatColor.GOLD + "=== Tax Rates ===");
            Map<String, Double> allTaxes = treasury.getAllTaxes();
            for (Map.Entry<String, Double> entry : allTaxes.entrySet()) {
                player.sendMessage(ChatColor.YELLOW + entry.getKey() + ": " + ChatColor.WHITE + String.format("%.1f%%", entry.getValue()));
            }
            return true;
        }

        String action = args[0].toLowerCase();
        if (action.equals("set")) {
            return handleSetTax(player, args);
        } else {
            player.sendMessage(ChatColor.RED + "Usage: /tax [set <type> <rate>]");
            return true;
        }
    }

    private boolean handleSetTax(Player player, String[] args) {
        if (args.length != 3) {
            player.sendMessage(ChatColor.RED + "Usage: /tax set <type> <rate>");
            player.sendMessage(ChatColor.YELLOW + "Available types: Transfer, Registration, Transaction, FinancialExp, FinancialRev, Employment, Entertainment, Bank");
            return true;
        }

        String taxType = args[1];
        try {
            double taxRate = Double.parseDouble(args[2]);

            if (taxRate < 0 || taxRate > 100) {
                player.sendMessage(ChatColor.RED + "Tax rate must be between 0% and 100%.");
                return true;
            }

            if (treasury.setTax(taxType, taxRate)) {
                player.sendMessage(ChatColor.GREEN + "Tax rate for " + taxType + " set to " + String.format("%.1f%%", taxRate));
                Bukkit.broadcastMessage(ChatColor.GOLD + "[Government] " + ChatColor.YELLOW +
                        player.getName() + " changed " + taxType + " tax rate to " + String.format("%.1f%%", taxRate));
                return true;
            } else {
                player.sendMessage(ChatColor.RED + "Failed to set tax rate. Please check the tax type.");
                return true;
            }
        } catch (NumberFormatException e) {
            player.sendMessage(ChatColor.RED + "Invalid tax rate. Please enter a number.");
            return true;
        }
    }

    private boolean handleTreasuryCommand(CommandSender sender, String[] args) {
        // Permission check for all treasury commands
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this command.");
            return true;
        }

        Player player = (Player) sender;
        if (!treasury.isMayor(player.getUniqueId()) && !sender.hasPermission("gamoecon.admin")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission to use this command.");
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage(ChatColor.GOLD + "Treasury Balance: ₹" + String.format("%.2f", treasury.getTreasuryBalance()));
            return true;
        }

        String action = args[0].toLowerCase();
        switch (action) {
            case "deposit":
                return handleDeposit(player, args);
            case "withdraw":
                return handleWithdraw(player, args);
            default:
                sender.sendMessage(ChatColor.RED + "Usage: /treasury [deposit|withdraw] <amount>");
                return true;
        }
    }

    private boolean handleDeposit(Player player, String[] args) {
        if (args.length != 2) {
            player.sendMessage(ChatColor.RED + "Usage: /treasury deposit <amount>");
            return true;
        }

        try {
            double amount = Double.parseDouble(args[1]);
            if (amount <= 0) {
                player.sendMessage(ChatColor.RED + "Amount must be positive.");
                return true;
            }

            // Remove money from player's wallet
            if (!plugin.getEconomyManager().removeBalance(player.getUniqueId(), amount)) {
                player.sendMessage(ChatColor.RED + "You don't have enough money.");
                return true;
            }

            // Add to treasury
            treasury.addToTreasury(amount);
            player.sendMessage(ChatColor.GREEN + "Deposited ₹" + String.format("%.2f", amount) + " to treasury.");
            return true;
        } catch (NumberFormatException e) {
            player.sendMessage(ChatColor.RED + "Invalid amount.");
            return true;
        }
    }

    private boolean handleWithdraw(Player player, String[] args) {
        if (args.length != 2) {
            player.sendMessage(ChatColor.RED + "Usage: /treasury withdraw <amount>");
            return true;
        }

        try {
            double amount = Double.parseDouble(args[1]);
            if (amount <= 0) {
                player.sendMessage(ChatColor.RED + "Amount must be positive.");
                return true;
            }

            // Remove from treasury
            if (!treasury.removeFromTreasury(amount)) {
                player.sendMessage(ChatColor.RED + "Insufficient treasury funds.");
                return true;
            }

            // Add to player's wallet
            plugin.getEconomyManager().addBalance(player.getUniqueId(), amount);
            player.sendMessage(ChatColor.GREEN + "Withdrew ₹" + String.format("%.2f", amount) + " from treasury.");
            return true;
        } catch (NumberFormatException e) {
            player.sendMessage(ChatColor.RED + "Invalid amount.");
            return true;
        }
    }

    private String formatTime(long millis) {
        long days = TimeUnit.MILLISECONDS.toDays(millis);
        long hours = TimeUnit.MILLISECONDS.toHours(millis) % 24;
        long minutes = TimeUnit.MILLISECONDS.toMinutes(millis) % 60;

        StringBuilder sb = new StringBuilder();
        if (days > 0) {
            sb.append(days).append("d ");
        }
        if (hours > 0) {
            sb.append(hours).append("h ");
        }
        if (minutes > 0) {
            sb.append(minutes).append("m");
        }

        return sb.toString().trim();
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (cmd.getName().equalsIgnoreCase("treasury")) {
            if (args.length == 1) {
                completions.addAll(Arrays.asList("deposit", "withdraw"));
            }
        } else if (cmd.getName().equalsIgnoreCase("tax")) {
            if (args.length == 1) {
                completions.add("set");
            } else if (args.length == 2 && args[0].equalsIgnoreCase("set")) {
                completions.addAll(Arrays.asList("Transfer", "Registration", "Transaction", "FinancialExp", "FinancialRev", "Employment", "Entertainment", "Bank"));
            }
        } else if (cmd.getName().equalsIgnoreCase("mayor")) {
            if (args.length == 1) {
                completions.addAll(Arrays.asList("register", "vote", "startelection", "endelection", "info", "force"));
            } else if (args.length == 2) {
                if (args[0].equalsIgnoreCase("vote") && mayorManager.isElectionInProgress()) {
                    for (UUID candidate : mayorManager.getCandidates()) {
                        OfflinePlayer player = Bukkit.getOfflinePlayer(candidate);
                        if (player.getName() != null) {
                            completions.add(player.getName());
                        }
                    }
                } else if (args[0].equalsIgnoreCase("force")) {
                    for (Player player : Bukkit.getOnlinePlayers()) {
                        completions.add(player.getName());
                    }
                }
            }
        }

        return completions;
    }
}