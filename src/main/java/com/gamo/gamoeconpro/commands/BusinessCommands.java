package com.gamo.gamoeconpro.commands;

import com.gamo.gamoeconpro.GamoEconPro;
import com.gamo.gamoeconpro.business.BusinessManager;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class BusinessCommands implements CommandExecutor, TabCompleter {

    private final GamoEconPro plugin;
    private final BusinessManager manager;

    public BusinessCommands(GamoEconPro plugin) {
        this.plugin = plugin;
        this.manager = plugin.getBusinessManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {

        if (!(sender instanceof Player)) {
            sender.sendMessage("§cOnly players can use these commands.");
            return true;
        }

        Player player = (Player) sender;
        UUID uuid = player.getUniqueId();

        switch (cmd.getName().toLowerCase()) {
            case "registerbusiness":
                if (args.length != 1) {
                    player.sendMessage("§cUsage: /registerbusiness <name>");
                    return true;
                }

                if (manager.hasPendingApplication(uuid)) {
                    player.sendMessage("§eYou already have a pending application.");
                    return true;
                }

                if (plugin.getEconomyManager().getBalance(uuid) < 1500) {
                    player.sendMessage("§cYou need ₹1500 to apply for a business license.");
                    return true;
                }

                plugin.getEconomyManager().removeBalance(uuid, 1500);
                plugin.getTreasuryManager().addToTreasury(1500);
                manager.applyForBusiness(uuid, args[0]);
                player.sendMessage("§aApplied for business license: " + args[0]);
                return true;

            case "cancelapplication":
                if (!manager.hasPendingApplication(uuid)) {
                    player.sendMessage("§cYou don't have any active application.");
                    return true;
                }
                manager.cancelApplication(uuid);
                player.sendMessage("§cYour application has been cancelled.");
                return true;

            case "cancelbusiness":
                if (args.length != 1) {
                    player.sendMessage("§cUsage: /cancelbusiness <name>");
                    return true;
                }
                if (!manager.cancelBusiness(uuid, args[0])) {
                    player.sendMessage("§cBusiness not found.");
                    return true;
                }
                player.sendMessage("§cBusiness '" + args[0] + "' cancelled.");
                return true;

            case "viewbusinesses":
                if (args.length != 1) {
                    player.sendMessage("§cUsage: /viewbusinesses <player>");
                    return true;
                }

                OfflinePlayer target = Bukkit.getOfflinePlayer(args[0]);
                UUID targetUUID = target.getUniqueId();
                List<String> businesses = manager.getBusinesses(targetUUID);

                if (businesses.isEmpty()) {
                    player.sendMessage("§7No businesses found for §f" + target.getName());
                } else {
                    player.sendMessage("§eBusinesses of §f" + target.getName() + "§e:");
                    for (String name : businesses) {
                        player.sendMessage(" §6• §f" + name);
                    }
                }
                return true;

            case "approvebusiness":
                if (!player.hasPermission("gamoecon.mayor")) {
                    player.sendMessage("§cOnly the Mayor can approve business licenses.");
                    return true;
                }

                if (args.length != 1) {
                    player.sendMessage("§cUsage: /approvebusiness <player>");
                    return true;
                }

                OfflinePlayer applicant = Bukkit.getOfflinePlayer(args[0]);
                UUID appUUID = applicant.getUniqueId();

                if (!manager.hasPendingApplication(appUUID)) {
                    player.sendMessage("§cThat player has no pending application.");
                    return true;
                }

                if (manager.approveBusiness(appUUID)) {
                    player.sendMessage("§aApproved business license for §e" + applicant.getName());
                } else {
                    player.sendMessage("§cApproval failed.");
                }
                return true;
        }

        return false;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (!(sender instanceof Player)) {
            return completions;
        }

        Player player = (Player) sender;
        String cmdName = cmd.getName().toLowerCase();

        if (args.length == 1) {
            switch (cmdName) {
                case "registerbusiness":
                    // Suggest common business name patterns
                    List<String> businessSuggestions = Arrays.asList(
                            "Shop", "Store", "Market", "Restaurant", "Cafe",
                            "Hotel", "Bank", "Factory", "Office", "Mall"
                    );
                    return businessSuggestions.stream()
                            .filter(name -> name.toLowerCase().startsWith(args[0].toLowerCase()))
                            .collect(Collectors.toList());

                case "cancelbusiness":
                    // Suggest player's own businesses
                    List<String> playerBusinesses = manager.getBusinesses(player.getUniqueId());
                    return playerBusinesses.stream()
                            .filter(name -> name.toLowerCase().startsWith(args[0].toLowerCase()))
                            .collect(Collectors.toList());

                case "viewbusinesses":
                    // Suggest online players
                    return Bukkit.getOnlinePlayers().stream()
                            .map(Player::getName)
                            .filter(name -> name.toLowerCase().startsWith(args[0].toLowerCase()))
                            .collect(Collectors.toList());

                case "approvebusiness":
                    if (player.hasPermission("gamoecon.mayor")) {
                        // Suggest players with pending applications
                        return Bukkit.getOnlinePlayers().stream()
                                .filter(p -> manager.hasPendingApplication(p.getUniqueId()))
                                .map(Player::getName)
                                .filter(name -> name.toLowerCase().startsWith(args[0].toLowerCase()))
                                .collect(Collectors.toList());
                    }
                    break;
            }
        }

        return completions;
    }
}