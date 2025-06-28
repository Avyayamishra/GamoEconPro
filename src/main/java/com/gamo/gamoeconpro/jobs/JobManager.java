package com.gamo.gamoeconpro.jobs;

import com.gamo.gamoeconpro.GamoEconPro;
import com.gamo.gamoeconpro.economy.EconomyManager;
import com.gamo.gamoeconpro.economy.TreasuryManager;
import com.gamo.gamoeconpro.storage.DataManager;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class JobManager {

    private final GamoEconPro plugin;
    private final EconomyManager economy;
    private final TreasuryManager treasury;
    private final DataManager dataManager;

    private final Map<UUID, PlayerJobData> playerData = new ConcurrentHashMap<>();
    private final Set<UUID> notificationsEnabled = new HashSet<>();

    private int maxJobs = 3;
    private double baseTaxRate = 0.025; // 2.5% base tax

    public JobManager(GamoEconPro plugin) {
        this.plugin = plugin;
        this.economy = plugin.getEconomyManager();
        this.treasury = plugin.getTreasuryManager();
        this.dataManager = plugin.getDataManager();
        loadAllPlayerData();

        // Load job configurations
        JobType.loadAllConfigs();

        // Auto-save task
        new BukkitRunnable() {
            @Override
            public void run() {
                saveAllPlayerData();
            }
        }.runTaskTimer(plugin, 20 * 60 * 5, 20 * 60 * 5); // Save every 5 minutes
    }

    public boolean joinJob(Player player, JobType job) {
        UUID uuid = player.getUniqueId();
        PlayerJobData data = playerData.computeIfAbsent(uuid, k -> new PlayerJobData());

        if (data.getJobs().size() >= maxJobs) {
            player.sendMessage("§cYou can only have " + maxJobs + " jobs at a time!");
            return false;
        }

        if (data.hasJob(job)) {
            player.sendMessage("§cYou already have this job!");
            return false;
        }

        data.addJob(job);
        player.sendMessage("§aYou've joined the " + job.getDisplayName() + " job!");
        return true;
    }

    public boolean leaveJob(Player player, JobType job) {
        UUID uuid = player.getUniqueId();
        PlayerJobData data = playerData.get(uuid);

        if (data == null || !data.hasJob(job)) {
            player.sendMessage("§cYou don't have this job!");
            return false;
        }

        data.removeJob(job);
        player.sendMessage("§aYou've left the " + job.getDisplayName() + " job!");
        return true;
    }

    public void handleBlockBreak(Player player, Material material) {
        if (!notificationsEnabled.contains(player.getUniqueId())) return;

        UUID uuid = player.getUniqueId();
        PlayerJobData data = playerData.get(uuid);

        if (data == null) return;

        for (JobType job : data.getJobs()) {
            double payment = job.getPayment(material);
            if (payment > 0) {
                // Calculate tax based on job level
                int jobLevel = data.getJobLevel(job);
                double taxRate = getTaxRate(jobLevel);
                double tax = payment * taxRate;
                double finalPayment = payment - tax;

                // Apply level bonus
                finalPayment *= getLevelBonus(jobLevel);

                // Pay player and add tax to treasury
                economy.addBalance(uuid, finalPayment);
                treasury.addToTreasury(tax);

                // Add experience
                data.addJobExperience(job, 1);

                // Send feedback
                if (notificationsEnabled.contains(uuid)) {
                    sendActionBar(player, String.format("§a+₹%.2f (%s Lvl %d)", finalPayment, job.getDisplayName(), jobLevel));
                }

                // Check for level up
                checkLevelUp(player, job, data);
                break;
            }
        }
    }

    public void handleAction(Player player, String action) {
        if (!notificationsEnabled.contains(player.getUniqueId())) return;

        UUID uuid = player.getUniqueId();
        PlayerJobData data = playerData.get(uuid);

        if (data == null) return;

        for (JobType job : data.getJobs()) {
            double payment = job.getActionPayment(action);
            if (payment > 0) {
                // Calculate tax based on job level
                int jobLevel = data.getJobLevel(job);
                double taxRate = getTaxRate(jobLevel);
                double tax = payment * taxRate;
                double finalPayment = payment - tax;

                // Apply level bonus
                finalPayment *= getLevelBonus(jobLevel);

                // Pay player and add tax to treasury
                economy.addBalance(uuid, finalPayment);
                treasury.addToTreasury(tax);

                // Add experience
                data.addJobExperience(job, 1);

                // Send feedback
                if (notificationsEnabled.contains(uuid)) {
                    sendActionBar(player, String.format("§a+₹%.2f (%s Lvl %d)", finalPayment, job.getDisplayName(), jobLevel));
                }

                // Check for level up
                checkLevelUp(player, job, data);
                break;
            }
        }
    }

    private void sendActionBar(Player player, String message) {
        try {
            // Try using Spigot API first (newer versions)
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(message));
        } catch (NoSuchMethodError | NoClassDefFoundError e) {
            // Fallback to regular chat message if action bar is not available
            player.sendMessage(message);
        }
    }

    public double getTaxRate(int jobLevel) {
        if (jobLevel < 5) return baseTaxRate; // 2.5%
        if (jobLevel < 10) return baseTaxRate * 2; // 5%
        if (jobLevel < 15) return baseTaxRate * 3; // 7.5%
        return baseTaxRate * 4; // 10%
    }

    private double getLevelBonus(int jobLevel) {
        if (jobLevel < 10) return 1.0; // No bonus
        if (jobLevel < 20) return 1.1; // 10% bonus
        if (jobLevel < 30) return 1.2; // 20% bonus
        return 1.3; // 30% bonus for level 30+
    }

    private void checkLevelUp(Player player, JobType job, PlayerJobData data) {
        int currentLevel = data.getJobLevel(job);
        int expNeeded = getExpForLevel(currentLevel + 1);

        if (data.getJobExperience(job) >= expNeeded) {
            data.setJobLevel(job, currentLevel + 1);
            data.setJobExperience(job, 0);
            if (notificationsEnabled.contains(player.getUniqueId())) {
                player.sendMessage(String.format("§6§lLEVEL UP! §eYou are now a %s Level %d!", job.getDisplayName(), currentLevel + 1));
            }
        }
    }

    private int getExpForLevel(int level) {
        return 10 * level + (level * level * 4);
    }

    public List<JobType> getAvailableJobs(Player player) {
        PlayerJobData data = playerData.get(player.getUniqueId());
        if (data == null) return new ArrayList<>(Arrays.asList(JobType.values()));

        List<JobType> available = new ArrayList<>();
        for (JobType job : JobType.values()) {
            if (!data.hasJob(job)) {
                available.add(job);
            }
        }
        return available;
    }

    public boolean hasJob(Player player, JobType job) {
        PlayerJobData data = playerData.get(player.getUniqueId());
        return data != null && data.hasJob(job);
    }

    public int getJobLevel(Player player, JobType job) {
        PlayerJobData data = playerData.get(player.getUniqueId());
        return data != null ? data.getJobLevel(job) : 0;
    }

    public int getJobExperience(Player player, JobType job) {
        PlayerJobData data = playerData.get(player.getUniqueId());
        return data != null ? data.getJobExperience(job) : 0;
    }

    public List<JobType> getPlayerJobs(Player player) {
        PlayerJobData data = playerData.get(player.getUniqueId());
        return data != null ? new ArrayList<>(data.getJobs()) : new ArrayList<>();
    }

    public List<String> getTopPlayers(JobType job, int count) {
        // This is a simplified version - in a real implementation you'd want to cache this
        // or store it more efficiently rather than calculating on demand
        return playerData.entrySet().stream()
                .filter(entry -> entry.getValue().hasJob(job))
                .sorted((e1, e2) -> Integer.compare(
                        e2.getValue().getJobLevel(job),
                        e1.getValue().getJobLevel(job)))
                .limit(count)
                .map(entry -> {
                    OfflinePlayer player = Bukkit.getOfflinePlayer(entry.getKey());
                    return player.getName() + " - Level " + entry.getValue().getJobLevel(job);
                })
                .collect(Collectors.toList());
    }

    public boolean toggleNotifications(Player player) {
        UUID uuid = player.getUniqueId();
        if (notificationsEnabled.contains(uuid)) {
            notificationsEnabled.remove(uuid);
            return false;
        } else {
            notificationsEnabled.add(uuid);
            return true;
        }
    }

    // Data management methods
    private void loadAllPlayerData() {
        Map<UUID, Map<String, Object>> loadedData = dataManager.loadJobData();
        for (Map.Entry<UUID, Map<String, Object>> entry : loadedData.entrySet()) {
            playerData.put(entry.getKey(), new PlayerJobData(entry.getValue()));

            // Enable notifications by default
            notificationsEnabled.add(entry.getKey());
        }
    }

    private void saveAllPlayerData() {
        Map<UUID, Map<String, Object>> saveData = new HashMap<>();
        for (Map.Entry<UUID, PlayerJobData> entry : playerData.entrySet()) {
            saveData.put(entry.getKey(), entry.getValue().serialize());
        }
        dataManager.saveJobData(saveData);
    }

    public void savePlayerData(Player player) {
        PlayerJobData data = playerData.get(player.getUniqueId());
        if (data != null) {
            dataManager.saveJobData(player.getUniqueId(), data.serialize());
        }
    }

    // Player job data container
    private static class PlayerJobData {
        private final Set<JobType> jobs = new HashSet<>();
        private final Map<JobType, Integer> jobLevels = new HashMap<>();
        private final Map<JobType, Integer> jobExperience = new HashMap<>();

        public PlayerJobData() {
            for (JobType job : JobType.values()) {
                jobLevels.put(job, 1);
                jobExperience.put(job, 0);
            }
        }

        public PlayerJobData(Map<String, Object> data) {
            // Deserialize from saved data
            List<String> jobNames = (List<String>) data.get("jobs");
            if (jobNames != null) {
                for (String name : jobNames) {
                    JobType job = JobType.getByName(name);
                    if (job != null) {
                        jobs.add(job);
                    }
                }
            }

            Map<String, Integer> levels = (Map<String, Integer>) data.get("levels");
            if (levels != null) {
                for (Map.Entry<String, Integer> entry : levels.entrySet()) {
                    JobType job = JobType.getByName(entry.getKey());
                    if (job != null) {
                        jobLevels.put(job, entry.getValue());
                    }
                }
            }

            Map<String, Integer> exp = (Map<String, Integer>) data.get("experience");
            if (exp != null) {
                for (Map.Entry<String, Integer> entry : exp.entrySet()) {
                    JobType job = JobType.getByName(entry.getKey());
                    if (job != null) {
                        jobExperience.put(job, entry.getValue());
                    }
                }
            }

            // Ensure all jobs have default values
            for (JobType job : JobType.values()) {
                jobLevels.putIfAbsent(job, 1);
                jobExperience.putIfAbsent(job, 0);
            }
        }

        public Set<JobType> getJobs() {
            return jobs;
        }

        public boolean hasJob(JobType job) {
            return jobs.contains(job);
        }

        public void addJob(JobType job) {
            jobs.add(job);
        }

        public void removeJob(JobType job) {
            jobs.remove(job);
        }

        public int getJobLevel(JobType job) {
            return jobLevels.getOrDefault(job, 1);
        }

        public void setJobLevel(JobType job, int level) {
            jobLevels.put(job, level);
        }

        public int getJobExperience(JobType job) {
            return jobExperience.getOrDefault(job, 0);
        }

        public void setJobExperience(JobType job, int exp) {
            jobExperience.put(job, exp);
        }

        public void addJobExperience(JobType job, int amount) {
            int current = getJobExperience(job);
            setJobExperience(job, current + amount);
        }

        public Map<String, Object> serialize() {
            Map<String, Object> data = new HashMap<>();

            List<String> jobNames = new ArrayList<>();
            for (JobType job : jobs) {
                jobNames.add(job.name());
            }
            data.put("jobs", jobNames);

            Map<String, Integer> levels = new HashMap<>();
            for (Map.Entry<JobType, Integer> entry : jobLevels.entrySet()) {
                levels.put(entry.getKey().name(), entry.getValue());
            }
            data.put("levels", levels);

            Map<String, Integer> exp = new HashMap<>();
            for (Map.Entry<JobType, Integer> entry : jobExperience.entrySet()) {
                exp.put(entry.getKey().name(), entry.getValue());
            }
            data.put("experience", exp);

            return data;
        }
    }
}