package com.gamo.gamoeconpro.commands;

import com.gamo.gamoeconpro.GamoEconPro;
import com.gamo.gamoeconpro.jobs.JobType;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class JobCommands implements CommandExecutor, TabCompleter {

    private final GamoEconPro plugin;

    public JobCommands(GamoEconPro plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use job commands.");
            return true;
        }

        Player player = (Player) sender;

        if (cmd.getName().equalsIgnoreCase("jobs")) {
            if (args.length == 0) {
                sendJobHelp(player);
                return true;
            }

            String action = args[0].toLowerCase();
            switch (action) {
                case "join":
                    return handleJoinJob(player, args);
                case "leave":
                    return handleLeaveJob(player, args);
                case "list":
                    return handleListJobs(player);
                case "info":
                    return handleJobInfo(player, args);
                case "stats":
                    return handleJobStats(player, args);
                case "top":
                    return handleJobTop(player, args);
                case "bonus":
                    return handleJobBonus(player, args);
                case "toggle":
                    return handleJobToggle(player);
                default:
                    sendJobHelp(player);
                    return true;
            }
        }

        return false;
    }

    private void sendJobHelp(Player player) {
        player.sendMessage(ChatColor.GOLD + "=== Job Commands ===");
        player.sendMessage(ChatColor.YELLOW + "/jobs join <job> - Join a job");
        player.sendMessage(ChatColor.YELLOW + "/jobs leave <job> - Leave a job");
        player.sendMessage(ChatColor.YELLOW + "/jobs list - List available jobs");
        player.sendMessage(ChatColor.YELLOW + "/jobs info <job> - Get info about a job");
        player.sendMessage(ChatColor.YELLOW + "/jobs stats [player] - View your or another player's job stats");
        player.sendMessage(ChatColor.YELLOW + "/jobs top <job> - Show top players for a job");
        player.sendMessage(ChatColor.YELLOW + "/jobs bonus <job> - Show job bonuses");
        player.sendMessage(ChatColor.YELLOW + "/jobs toggle - Toggle job notifications");
    }

    private boolean handleJoinJob(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "Usage: /jobs join <job>");
            return true;
        }

        JobType job = JobType.getByName(args[1]);
        if (job == null) {
            player.sendMessage(ChatColor.RED + "Invalid job. Use /jobs list to see available jobs.");
            return true;
        }

        if (plugin.getJobManager().joinJob(player, job)) {
            player.sendMessage(ChatColor.GREEN + "You've joined the " + job.getDisplayName() + " job!");
        }
        return true;
    }

    private boolean handleLeaveJob(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "Usage: /jobs leave <job>");
            return true;
        }

        JobType job = JobType.getByName(args[1]);
        if (job == null) {
            player.sendMessage(ChatColor.RED + "Invalid job. Use /jobs list to see available jobs.");
            return true;
        }

        if (plugin.getJobManager().leaveJob(player, job)) {
            player.sendMessage(ChatColor.GREEN + "You've left the " + job.getDisplayName() + " job.");
        }
        return true;
    }

    private boolean handleListJobs(Player player) {
        player.sendMessage(ChatColor.GOLD + "=== Available Jobs ===");
        for (JobType job : plugin.getJobManager().getAvailableJobs(player)) {
            player.sendMessage(ChatColor.YELLOW + "- " + job.getDisplayName() + ": " +
                    ChatColor.WHITE + job.getDescription());
        }
        return true;
    }

    private boolean handleJobInfo(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "Usage: /jobs info <job>");
            return true;
        }

        JobType job = JobType.getByName(args[1]);
        if (job == null) {
            player.sendMessage(ChatColor.RED + "Invalid job. Use /jobs list to see available jobs.");
            return true;
        }

        player.sendMessage(ChatColor.GOLD + "=== " + job.getDisplayName() + " Job ===");
        player.sendMessage(ChatColor.YELLOW + "Description: " + ChatColor.WHITE + job.getDescription());

        if (plugin.getJobManager().hasJob(player, job)) {
            int level = plugin.getJobManager().getJobLevel(player, job);
            int exp = plugin.getJobManager().getJobExperience(player, job);
            int expNeeded = 10 * level + (level * level * 4);

            player.sendMessage(ChatColor.YELLOW + "Your Level: " + ChatColor.WHITE + level);
            player.sendMessage(ChatColor.YELLOW + "Experience: " + ChatColor.WHITE + exp + "/" + expNeeded);
            player.sendMessage(ChatColor.YELLOW + "Tax Rate: " + ChatColor.WHITE +
                    String.format("%.1f%%", plugin.getJobManager().getTaxRate(level) * 100));
        }

        return true;
    }

    private boolean handleJobStats(Player player, String[] args) {
        Player target = player;
        if (args.length > 1) {
            target = plugin.getServer().getPlayer(args[1]);
            if (target == null) {
                player.sendMessage(ChatColor.RED + "Player not found.");
                return true;
            }
        }

        player.sendMessage(ChatColor.GOLD + "=== Job Stats for " + target.getName() + " ===");
        for (JobType job : plugin.getJobManager().getPlayerJobs(target)) {
            int level = plugin.getJobManager().getJobLevel(target, job);
            int exp = plugin.getJobManager().getJobExperience(target, job);
            int expNeeded = 10 * level + (level * level * 4);

            player.sendMessage(ChatColor.YELLOW + job.getDisplayName() + ": " +
                    ChatColor.WHITE + "Level " + level + " (" + exp + "/" + expNeeded + " XP)");
        }
        return true;
    }

    private boolean handleJobTop(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "Usage: /jobs top <job>");
            return true;
        }

        JobType job = JobType.getByName(args[1]);
        if (job == null) {
            player.sendMessage(ChatColor.RED + "Invalid job. Use /jobs list to see available jobs.");
            return true;
        }

        List<String> topPlayers = plugin.getJobManager().getTopPlayers(job, 10);
        player.sendMessage(ChatColor.GOLD + "=== Top " + job.getDisplayName() + " Players ===");
        for (int i = 0; i < topPlayers.size(); i++) {
            player.sendMessage(ChatColor.YELLOW + String.valueOf(i + 1) + ". " + ChatColor.WHITE + topPlayers.get(i));
        }
        return true;
    }

    private boolean handleJobBonus(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "Usage: /jobs bonus <job>");
            return true;
        }

        JobType job = JobType.getByName(args[1]);
        if (job == null) {
            player.sendMessage(ChatColor.RED + "Invalid job. Use /jobs list to see available jobs.");
            return true;
        }

        player.sendMessage(ChatColor.GOLD + "=== " + job.getDisplayName() + " Bonuses ===");
        player.sendMessage(ChatColor.YELLOW + "Level 5: " + ChatColor.WHITE + "Reduced tax rate (5%)");
        player.sendMessage(ChatColor.YELLOW + "Level 10: " + ChatColor.WHITE + "10% more earnings");
        player.sendMessage(ChatColor.YELLOW + "Level 15: " + ChatColor.WHITE + "Access to special items");
        player.sendMessage(ChatColor.YELLOW + "Level 20: " + ChatColor.WHITE + "Exclusive quests");
        return true;
    }

    private boolean handleJobToggle(Player player) {
        boolean newState = plugin.getJobManager().toggleNotifications(player);
        player.sendMessage(ChatColor.GREEN + "Job notifications are now " +
                (newState ? "enabled" : "disabled"));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        if (!(sender instanceof Player)) {
            return Collections.emptyList();
        }

        Player player = (Player) sender;
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            // Main command completions
            List<String> commands = Arrays.asList("join", "leave", "list", "info", "stats", "top", "bonus", "toggle");
            return StringUtil.copyPartialMatches(args[0], commands, new ArrayList<>());
        } else if (args.length == 2) {
            // Job name completions for specific commands
            String subCommand = args[0].toLowerCase();
            if (subCommand.equals("join")) {
                // Only show jobs player doesn't have
                List<String> availableJobs = new ArrayList<>();
                for (JobType job : plugin.getJobManager().getAvailableJobs(player)) {
                    String jobName = job.name().toLowerCase();
                    if (jobName.startsWith(args[1].toLowerCase())) {
                        availableJobs.add(jobName);
                    }
                }
                return availableJobs;
            } else if (subCommand.equals("leave") || subCommand.equals("info") ||
                    subCommand.equals("top") || subCommand.equals("bonus")) {
                // Show jobs player has (for leave) or all jobs (for others)
                List<JobType> jobs = subCommand.equals("leave") ?
                        plugin.getJobManager().getPlayerJobs(player) :
                        Arrays.asList(JobType.values());
                List<String> jobNames = new ArrayList<>();
                for (JobType job : jobs) {
                    String jobName = job.name().toLowerCase();
                    if (jobName.startsWith(args[1].toLowerCase())) {
                        jobNames.add(jobName);
                    }
                }
                return jobNames;
            } else if (subCommand.equals("stats")) {
                // Player name completion
                return null; // Let Bukkit handle player name completion
            }
        } else if (args.length == 3 && args[0].equalsIgnoreCase("top")) {
            // Page number completion for top command
            return Arrays.asList("1", "2", "3", "4", "5");
        }

        return completions;
    }
}