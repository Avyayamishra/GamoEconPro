package com.gamo.gamoeconpro.storage;

import java.util.Objects;

public class BankAccountData {
    private final double balance;
    private final long lastInterestTime;

    public BankAccountData(double balance, long lastInterestTime) {
        this.balance = Math.max(0.0, balance); // Ensure non-negative balance
        this.lastInterestTime = lastInterestTime;
    }

    public double getBalance() {
        return balance;
    }

    public long getLastInterestTime() {
        return lastInterestTime;
    }

    @Override
    public String toString() {
        return "BankAccountData{" +
                "balance=" + balance +
                ", lastInterestTime=" + lastInterestTime +
                '}';
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        com.gamo.gamoeconpro.bank.BankAccountData that = (com.gamo.gamoeconpro.bank.BankAccountData) obj;
        return Double.compare(that.getBalance(), this.getBalance()) == 0 &&
                this.getLastInterestTime() == that.getLastInterestTime();
    }

    @Override
    public int hashCode() {
        return Objects.hash(balance, lastInterestTime);
    }
}