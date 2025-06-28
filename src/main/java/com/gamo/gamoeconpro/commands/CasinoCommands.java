package com.gamo.gamoeconpro.commands;

import com.gamo.gamoeconpro.GamoEconPro;
import com.gamo.gamoeconpro.casino.BlackjackGame;
import com.gamo.gamoeconpro.casino.CasinoManager;
import org.bukkit.ChatColor;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CasinoCommands implements CommandExecutor, TabCompleter {

    private final GamoEconPro plugin;
    private final CasinoManager casino;

    public CasinoCommands(GamoEconPro plugin) {
        this.plugin = plugin;
        this.casino = plugin.getCasinoManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {

        if (!(sender instanceof Player)) {
            sender.sendMessage("§cOnly players can use this command.");
            return true;
        }

        Player player = (Player) sender;

        switch (cmd.getName().toLowerCase()) {
            case "coinflip":
                return handleCoinFlip(player, args);
            case "dicebet":
                return handleDiceBet(player, args);
            case "blackjack":
                return handleBlackjack(player, args);
        }

        return false;
    }

    private boolean handleCoinFlip(Player player, String[] args) {
        if (args.length != 2) {
            player.sendMessage("§cUsage: /coinflip <heads|tails> <amount>");
            return true;
        }

        String choice = args[0];
        double amount;

        try {
            amount = Double.parseDouble(args[1]);
        } catch (NumberFormatException e) {
            player.sendMessage("§cInvalid amount.");
            return true;
        }

        if (!choice.equalsIgnoreCase("heads") && !choice.equalsIgnoreCase("tails")) {
            player.sendMessage("§cChoose either 'heads' or 'tails'.");
            return true;
        }

        boolean result = casino.coinFlip(player, choice, amount);
        player.sendMessage(result
                ? "§a🎉 You won ₹" + (amount * 1.8) + " in CoinFlip!"
                : "§c💸 You lost the bet. Money went to treasury.");
        return true;
    }

    private boolean handleDiceBet(Player player, String[] args) {
        if (args.length != 2) {
            player.sendMessage("§cUsage: /dicebet <1-6> <amount>");
            return true;
        }

        int guess;
        double amount;

        try {
            guess = Integer.parseInt(args[0]);
            amount = Double.parseDouble(args[1]);
        } catch (NumberFormatException e) {
            player.sendMessage("§cInvalid number or amount.");
            return true;
        }

        if (guess < 1 || guess > 6) {
            player.sendMessage("§cPick a number between 1 and 6.");
            return true;
        }

        boolean result = casino.diceBet(player, guess, amount);
        player.sendMessage(result
                ? "§a🎉 You guessed right! ₹" + (amount * 6) + " rewarded!"
                : "§c💸 Wrong guess. You lost ₹" + amount);
        return true;
    }

    private boolean handleBlackjack(Player player, String[] args) {
        if (args.length == 1) {
            // Start game
            double amount;
            try {
                amount = Double.parseDouble(args[0]);
            } catch (NumberFormatException e) {
                player.sendMessage("§cInvalid amount.");
                return true;
            }

            if (casino.hasActiveBlackjack(player.getUniqueId())) {
                player.sendMessage("§cYou are already in a Blackjack game!");
                return true;
            }

            casino.startBlackjack(player, amount);
            BlackjackGame game = casino.getGame(player.getUniqueId());
            player.sendMessage("§eBlackjack started. Your cards: " + game.getPlayerCards()
                    + " (Total: " + game.getPlayerTotal() + ")");
            return true;

        } else if (args.length == 1 && args[0].equalsIgnoreCase("hit")) {
            if (!casino.hasActiveBlackjack(player.getUniqueId())) {
                player.sendMessage("§cNo active game. Use /blackjack <amount>.");
                return true;
            }

            boolean stillInGame = casino.blackjackHit(player);
            BlackjackGame game = casino.getGame(player.getUniqueId());

            if (game.getPlayerTotal() > 21) {
                player.sendMessage("§c💥 You busted! Cards: " + game.getPlayerCards());
                casino.blackjackStand(player);
            } else {
                player.sendMessage("§eYou hit. Cards: " + game.getPlayerCards()
                        + " (Total: " + game.getPlayerTotal() + ")");
            }
            return true;

        } else if (args.length == 1 && args[0].equalsIgnoreCase("stand")) {
            if (!casino.hasActiveBlackjack(player.getUniqueId())) {
                player.sendMessage("§cNo active game. Use /blackjack <amount>.");
                return true;
            }

            BlackjackGame game = casino.getGame(player.getUniqueId());
            casino.blackjackStand(player);

            player.sendMessage("§bDealer's cards: " + game.getDealerCards()
                    + " (Total: " + game.getDealerTotal() + ")");

            if (game.getDealerTotal() > 21 || game.getPlayerTotal() > game.getDealerTotal()) {
                player.sendMessage("§a🎉 You win! ₹" + (game.betAmount * 2.5));
            } else {
                player.sendMessage("§c💸 You lost. Better luck next time.");
            }

            return true;
        }

        player.sendMessage("§cUsage: /blackjack <amount> | /blackjack hit | /blackjack stand");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (!(sender instanceof Player)) {
            return completions;
        }

        Player player = (Player) sender;

        if (args.length == 1) {
            // First argument suggestions based on command
            switch (cmd.getName().toLowerCase()) {
                case "coinflip":
                    List<String> coinOptions = Arrays.asList("heads", "tails");
                    for (String option : coinOptions) {
                        if (option.toLowerCase().startsWith(args[0].toLowerCase())) {
                            completions.add(option);
                        }
                    }
                    break;
                case "dicebet":
                    for (int i = 1; i <= 6; i++) {
                        if (String.valueOf(i).startsWith(args[0])) {
                            completions.add(String.valueOf(i));
                        }
                    }
                    break;
                case "blackjack":
                    // Check if player has active game
                    if (casino.hasActiveBlackjack(player.getUniqueId())) {
                        List<String> blackjackActions = Arrays.asList("hit", "stand");
                        for (String action : blackjackActions) {
                            if (action.toLowerCase().startsWith(args[0].toLowerCase())) {
                                completions.add(action);
                            }
                        }
                    } else {
                        // Suggest bet amounts
                        completions.add("100");
                        completions.add("500");
                        completions.add("1000");
                    }
                    break;
            }
        } else if (args.length == 2) {
            // Second argument - usually bet amounts
            if (cmd.getName().equalsIgnoreCase("coinflip") || cmd.getName().equalsIgnoreCase("dicebet")) {
                completions.add("50");
                completions.add("100");
                completions.add("250");
                completions.add("500");
                completions.add("1000");
            }
        }

        return completions;
    }
}