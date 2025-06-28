package com.gamo.gamoeconpro.government;

import com.gamo.gamoeconpro.GamoEconPro;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class MayorManager {

    private final GamoEconPro plugin;
    private final Map<UUID, Long> mayorTermStart = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> mayorTermCount = new ConcurrentHashMap<>();
    private final Set<UUID> mayorCandidates = ConcurrentHashMap.newKeySet();
    private boolean electionInProgress = false;
    private long electionStartTime = 0;
    private final Map<UUID, UUID> votes = new ConcurrentHashMap<>(); // voter -> candidate

    // Constants
    public static final long MAYOR_TERM_LENGTH = 7 * 24 * 60 * 60 * 1000L; // 1 week in milliseconds
    public static final long ELECTION_DURATION = 24 * 60 * 60 * 1000L; // 24 hours in milliseconds
    public static final double CANDIDACY_FEE = 5000.0;
    public static final int MAX_CONSECUTIVE_TERMS = 2;

    public MayorManager(GamoEconPro plugin) {
        this.plugin = plugin;
        loadMayorData();
    }

    public void loadMayorData() {
        try {
            // Load mayor term data from config
            if (plugin.getConfig().contains("mayor.term-start")) {
                UUID currentMayor = plugin.getTreasuryManager().getMayor();
                if (currentMayor != null) {
                    long termStart = plugin.getConfig().getLong("mayor.term-start", System.currentTimeMillis());
                    int termCount = plugin.getConfig().getInt("mayor.term-count." + currentMayor.toString(), 0);

                    mayorTermStart.put(currentMayor, termStart);
                    mayorTermCount.put(currentMayor, termCount);
                }
            }

            // Load election data
            electionInProgress = plugin.getConfig().getBoolean("election.in-progress", false);
            electionStartTime = plugin.getConfig().getLong("election.start-time", 0);

            // Load candidates
            List<String> candidateStrings = plugin.getConfig().getStringList("election.candidates");
            for (String candidateString : candidateStrings) {
                try {
                    mayorCandidates.add(UUID.fromString(candidateString));
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("Invalid candidate UUID: " + candidateString);
                }
            }

            // Load votes
            if (plugin.getConfig().contains("election.votes")) {
                for (String key : plugin.getConfig().getConfigurationSection("election.votes").getKeys(false)) {
                    try {
                        UUID voter = UUID.fromString(key);
                        UUID candidate = UUID.fromString(plugin.getConfig().getString("election.votes." + key));
                        votes.put(voter, candidate);
                    } catch (IllegalArgumentException e) {
                        plugin.getLogger().warning("Invalid vote data: " + key);
                    }
                }
            }

        } catch (Exception e) {
            plugin.getLogger().severe("Failed to load mayor data: " + e.getMessage());
        }
    }

    public void saveMayorData() {
        try {
            UUID currentMayor = plugin.getTreasuryManager().getMayor();
            if (currentMayor != null && mayorTermStart.containsKey(currentMayor)) {
                plugin.getConfig().set("mayor.term-start", mayorTermStart.get(currentMayor));
                plugin.getConfig().set("mayor.term-count." + currentMayor.toString(), mayorTermCount.getOrDefault(currentMayor, 0));
            }

            // Save election data
            plugin.getConfig().set("election.in-progress", electionInProgress);
            plugin.getConfig().set("election.start-time", electionStartTime);

            // Save candidates
            List<String> candidateStrings = new ArrayList<>();
            for (UUID candidate : mayorCandidates) {
                candidateStrings.add(candidate.toString());
            }
            plugin.getConfig().set("election.candidates", candidateStrings);

            // Save votes
            for (Map.Entry<UUID, UUID> entry : votes.entrySet()) {
                plugin.getConfig().set("election.votes." + entry.getKey().toString(), entry.getValue().toString());
            }

            plugin.saveConfig();
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to save mayor data: " + e.getMessage());
        }
    }

    public boolean registerCandidate(UUID playerUUID) {
        if (playerUUID == null) return false;
        if (electionInProgress) return false;
        if (mayorCandidates.contains(playerUUID)) return false;

        // Check if player can afford candidacy fee
        if (plugin.getEconomyManager().getBalance(playerUUID) < CANDIDACY_FEE) return false;

        // Check consecutive term limit
        if (isCurrentMayor(playerUUID) && getConsecutiveTerms(playerUUID) >= MAX_CONSECUTIVE_TERMS) {
            return false;
        }

        // Deduct candidacy fee and add to treasury
        if (plugin.getEconomyManager().removeBalance(playerUUID, CANDIDACY_FEE)) {
            plugin.getTreasuryManager().addToTreasury(CANDIDACY_FEE);
            mayorCandidates.add(playerUUID);
            saveMayorData();
            return true;
        }

        return false;
    }

    public boolean startElection() {
        if (electionInProgress) return false;
        if (mayorCandidates.isEmpty()) return false;

        electionInProgress = true;
        electionStartTime = System.currentTimeMillis();
        votes.clear();
        saveMayorData();

        // Notify all online players
        Bukkit.broadcastMessage(ChatColor.GOLD + "[Government] " + ChatColor.YELLOW +
                "Mayoral election has started! Use /mayor vote <candidate> to cast your vote.");
        return true;
    }

    public boolean vote(UUID voterUUID, UUID candidateUUID) {
        if (!electionInProgress) return false;
        if (!mayorCandidates.contains(candidateUUID)) return false;
        if (voterUUID == null || candidateUUID == null) return false;

        // Check if election is still ongoing
        if (System.currentTimeMillis() - electionStartTime > ELECTION_DURATION) {
            endElection();
            return false;
        }

        votes.put(voterUUID, candidateUUID);
        saveMayorData();
        return true;
    }

    public boolean endElection() {
        if (!electionInProgress) return false;

        // Count votes
        Map<UUID, Integer> voteCount = new HashMap<>();
        for (UUID candidate : mayorCandidates) {
            voteCount.put(candidate, 0);
        }

        for (UUID candidate : votes.values()) {
            voteCount.put(candidate, voteCount.getOrDefault(candidate, 0) + 1);
        }

        // Find winner
        UUID winner = null;
        int maxVotes = 0;
        boolean tie = false;
        for (Map.Entry<UUID, Integer> entry : voteCount.entrySet()) {
            if (entry.getValue() > maxVotes) {
                maxVotes = entry.getValue();
                winner = entry.getKey();
                tie = false;
            } else if (entry.getValue() == maxVotes) {
                tie = true;
            }
        }

        // Set new mayor
        if (winner != null && maxVotes > 0 && !tie) {
            setNewMayor(winner);
            Player winnerPlayer = Bukkit.getPlayer(winner);
            String winnerName = winnerPlayer != null ? winnerPlayer.getName() : winner.toString();
            Bukkit.broadcastMessage(ChatColor.GOLD + "[Government] " + ChatColor.YELLOW +
                    winnerName + " has been elected as the new mayor with " + maxVotes + " votes!");
        } else if (tie) {
            Bukkit.broadcastMessage(ChatColor.GOLD + "[Government] " + ChatColor.RED +
                    "Election ended in a tie! No mayor has been elected.");
        } else {
            Bukkit.broadcastMessage(ChatColor.GOLD + "[Government] " + ChatColor.RED +
                    "No valid votes were cast. No mayor has been elected.");
        }

        // Reset election data
        electionInProgress = false;
        electionStartTime = 0;
        mayorCandidates.clear();
        votes.clear();
        saveMayorData();

        return true;
    }

    private void setNewMayor(UUID newMayor) {
        UUID currentMayor = plugin.getTreasuryManager().getMayor();

        // Update consecutive term count
        if (newMayor.equals(currentMayor)) {
            mayorTermCount.put(newMayor, mayorTermCount.getOrDefault(newMayor, 0) + 1);
        } else {
            mayorTermCount.put(newMayor, 1);
        }

        // Set new mayor and term start time
        plugin.getTreasuryManager().setMayor(newMayor);
        mayorTermStart.put(newMayor, System.currentTimeMillis());
        saveMayorData();
    }

    public boolean isCurrentMayor(UUID playerUUID) {
        return plugin.getTreasuryManager().isMayor(playerUUID);
    }

    public int getConsecutiveTerms(UUID playerUUID) {
        if (!isCurrentMayor(playerUUID)) return 0;
        return mayorTermCount.getOrDefault(playerUUID, 0);
    }

    public long getRemainingTermTime() {
        UUID currentMayor = plugin.getTreasuryManager().getMayor();
        if (currentMayor == null) return 0;

        long termStart = mayorTermStart.getOrDefault(currentMayor, System.currentTimeMillis());
        long elapsed = System.currentTimeMillis() - termStart;
        return Math.max(0, MAYOR_TERM_LENGTH - elapsed);
    }

    public boolean isTermExpired() {
        return getRemainingTermTime() <= 0;
    }

    public boolean isElectionInProgress() {
        return electionInProgress;
    }

    public long getRemainingElectionTime() {
        if (!electionInProgress) return 0;
        long elapsed = System.currentTimeMillis() - electionStartTime;
        return Math.max(0, ELECTION_DURATION - elapsed);
    }

    public Set<UUID> getCandidates() {
        return new HashSet<>(mayorCandidates);
    }

    public Map<UUID, Integer> getVoteCount() {
        if (!electionInProgress) return new HashMap<>();

        Map<UUID, Integer> voteCount = new HashMap<>();
        for (UUID candidate : mayorCandidates) {
            voteCount.put(candidate, 0);
        }

        for (UUID candidate : votes.values()) {
            voteCount.put(candidate, voteCount.getOrDefault(candidate, 0) + 1);
        }

        return voteCount;
    }

    public boolean hasVoted(UUID voterUUID) {
        return votes.containsKey(voterUUID);
    }

    public UUID getVote(UUID voterUUID) {
        return votes.get(voterUUID);
    }

    // Method to check and handle term expiration (should be called periodically)
    public void checkTermExpiration() {
        if (isTermExpired() && plugin.getTreasuryManager().hasMayor()) {
            UUID currentMayor = plugin.getTreasuryManager().getMayor();
            Player mayorPlayer = Bukkit.getPlayer(currentMayor);
            String mayorName = mayorPlayer != null ? mayorPlayer.getName() : "Unknown";

            Bukkit.broadcastMessage(ChatColor.GOLD + "[Government] " + ChatColor.YELLOW +
                    "The term of mayor " + mayorName + " has expired.");

            // Clear current mayor
            plugin.getTreasuryManager().clearMayor();
            mayorTermStart.remove(currentMayor);
            saveMayorData();

            // Start new election
            startElection();
        }

        // Check if election time has expired
        if (electionInProgress && getRemainingElectionTime() <= 0) {
            endElection();
        }
    }

    // Utility methods
    public double getCandidacyFee() {
        return CANDIDACY_FEE;
    }

    public long getTermLength() {
        return MAYOR_TERM_LENGTH;
    }

    public long getElectionDuration() {
        return ELECTION_DURATION;
    }

    public int getMaxConsecutiveTerms() {
        return MAX_CONSECUTIVE_TERMS;
    }
}