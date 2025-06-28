package com.gamo.gamoeconpro.casino;

import com.gamo.gamoeconpro.GamoEconPro;
import org.bukkit.entity.Player;

import java.util.*;

public class CasinoManager {

    private final GamoEconPro plugin;
    private final Map<UUID, BlackjackGame> blackjackGames = new HashMap<>();

    public CasinoManager(GamoEconPro plugin) {
        this.plugin = plugin;
    }

    public boolean coinFlip(Player player, String choice, double amount) {
        if (!plugin.getEconomyManager().removeBalance(player.getUniqueId(), amount)) return false;

        String result = Math.random() < 0.5 ? "Heads" : "Tails";

        if (result.equalsIgnoreCase(choice)) {
            double reward = amount * 1.8;
            plugin.getEconomyManager().addBalance(player.getUniqueId(), reward);
            return true;
        } else {
            plugin.getTreasuryManager().addToTreasury(amount);
            return false;
        }
    }

    public boolean diceBet(Player player, int guessed, double amount) {
        if (!plugin.getEconomyManager().removeBalance(player.getUniqueId(), amount)) return false;

        int rolled = new Random().nextInt(6) + 1;
        if (rolled == guessed) {
            double reward = amount * 6;
            plugin.getEconomyManager().addBalance(player.getUniqueId(), reward);
            return true;
        } else {
            plugin.getTreasuryManager().addToTreasury(amount);
            return false;
        }
    }

    // Blackjack session handling
    public void startBlackjack(Player player, double amount) {
        if (!plugin.getEconomyManager().removeBalance(player.getUniqueId(), amount)) return;

        BlackjackGame game = new BlackjackGame(player.getUniqueId(), amount);
        game.dealInitialCards();
        blackjackGames.put(player.getUniqueId(), game);
    }

    public boolean blackjackHit(Player player) {
        BlackjackGame game = blackjackGames.get(player.getUniqueId());
        if (game == null) return false;

        return game.playerHit();
    }

    public void blackjackStand(Player player) {
        BlackjackGame game = blackjackGames.get(player.getUniqueId());
        if (game == null) return;

        game.resolve(plugin.getEconomyManager(), plugin.getTreasuryManager());
        blackjackGames.remove(player.getUniqueId());
    }

    public BlackjackGame getGame(UUID uuid) {
        return blackjackGames.get(uuid);
    }

    public boolean hasActiveBlackjack(UUID uuid) {
        return blackjackGames.containsKey(uuid);
    }
}