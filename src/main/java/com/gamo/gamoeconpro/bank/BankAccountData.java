package com.gamo.gamoeconpro.bank;

import java.io.Serializable;
import java.util.Objects;

public class BankAccountData implements Serializable {
    private static final long serialVersionUID = 1L;

    private final double balance;
    private final long lastInterestTime;

    public BankAccountData(double balance, long lastInterestTime) {
        this.balance = Math.max(0.0, balance);
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
        BankAccountData that = (BankAccountData) obj;
        return Double.compare(that.balance, balance) == 0 &&
                lastInterestTime == that.lastInterestTime;
    }

    @Override
    public int hashCode() {
        return Objects.hash(balance, lastInterestTime);
    }
}