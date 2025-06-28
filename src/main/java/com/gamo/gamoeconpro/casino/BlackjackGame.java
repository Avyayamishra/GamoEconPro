package com.gamo.gamoeconpro.casino;

import com.gamo.gamoeconpro.economy.EconomyManager;
import com.gamo.gamoeconpro.economy.TreasuryManager;

import java.util.*;

public class BlackjackGame {

    private final UUID playerId;
    public final double betAmount;
    private final List<Integer> playerCards = new ArrayList<>();
    private final List<Integer> dealerCards = new ArrayList<>();
    private boolean isFinished = false;

    public BlackjackGame(UUID playerId, double betAmount) {
        this.playerId = playerId;
        this.betAmount = betAmount;
    }

    public void dealInitialCards() {
        playerCards.add(drawCard());
        playerCards.add(drawCard());
        dealerCards.add(drawCard());
        dealerCards.add(drawCard());
    }

    public boolean playerHit() {
        if (isFinished) return false;
        playerCards.add(drawCard());
        if (calculateTotal(playerCards) > 21) {
            isFinished = true;
        }
        return true;
    }

    public void resolve(EconomyManager economy, TreasuryManager treasury) {
        if (isFinished) return;

        int playerTotal = calculateTotal(playerCards);
        int dealerTotal = calculateTotal(dealerCards);

        while (dealerTotal < 17) {
            dealerCards.add(drawCard());
            dealerTotal = calculateTotal(dealerCards);
        }

        isFinished = true;

        if (playerTotal > 21 || (dealerTotal <= 21 && dealerTotal >= playerTotal)) {
            treasury.addToTreasury(betAmount);
        } else {
            economy.addBalance(playerId, betAmount * 2.5);
        }
    }

    public int getPlayerTotal() {
        return calculateTotal(playerCards);
    }

    public int getDealerTotal() {
        return calculateTotal(dealerCards);
    }

    public List<Integer> getPlayerCards() {
        return playerCards;
    }

    public List<Integer> getDealerCards() {
        return dealerCards;
    }

    public boolean isFinished() {
        return isFinished;
    }

    private int drawCard() {
        return new Random().nextInt(13) + 1; // 1-13
    }

    private int calculateTotal(List<Integer> cards) {
        int sum = 0;
        for (int card : cards) {
            if (card > 10) sum += 10;
            else sum += card;
        }
        return sum;
    }
}
