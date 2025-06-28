package com.gamo.gamoeconpro.stockmarket;

public class Stock {
    private final String companyName;
    private int totalShares = 100_000;
    private double currentPrice = 1000.0;
    private long lastTradeTimestamp;

    public Stock(String companyName) {
        this.companyName = companyName;
        this.lastTradeTimestamp = System.currentTimeMillis();
    }

    public String getCompanyName() {
        return companyName;
    }

    public int getTotalShares() {
        return totalShares;
    }

    public double getCurrentPrice() {
        return currentPrice;
    }

    public void updatePrice(boolean isBuy, double transactionAmount) {
        // Stronger price impact for larger companies
        double scaleFactor = Math.log(totalShares / 1000.0) / 10.0 + 1.0;

        // Base change rate plus transaction amount impact (0.1% per 1000 rupees)
        double baseChangeRate = 0.003 / scaleFactor;
        double transactionImpact = (transactionAmount / 1000.0) * 0.001 / scaleFactor;
        double totalChangeRate = baseChangeRate + transactionImpact;

        if (isBuy) {
            currentPrice *= (1 + totalChangeRate); // Price increase
        } else {
            currentPrice *= (1 - totalChangeRate); // Price decrease
        }

        // Price clamping
        if (currentPrice < 10.0) currentPrice = 10.0;
        if (currentPrice > 5000.0) currentPrice = 5000.0;

        lastTradeTimestamp = System.currentTimeMillis();
    }

    public void decayIfIdle() {
        long now = System.currentTimeMillis();
        long idleHours = (now - lastTradeTimestamp) / 3600000;

        if (idleHours > 0) {
            currentPrice *= Math.pow(0.995, idleHours); // 0.5% decay per hour
            lastTradeTimestamp = now;
        }
    }
}