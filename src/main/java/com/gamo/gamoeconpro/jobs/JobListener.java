package com.gamo.gamoeconpro.jobs;

import com.gamo.gamoeconpro.GamoEconPro;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class JobListener implements Listener {

    private final GamoEconPro plugin;
    private final JobManager jobManager;

    public JobListener(GamoEconPro plugin) {
        this.plugin = plugin;
        this.jobManager = plugin.getJobManager();
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();
        jobManager.handleBlockBreak(player, block.getType());
    }

    @EventHandler
    public void onPlayerFish(PlayerFishEvent event) {
        if (event.getState() == PlayerFishEvent.State.CAUGHT_FISH) {
            jobManager.handleAction(event.getPlayer(), "catch_fish");
        }
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        if (event.getEntity().getKiller() != null) {
            Player player = event.getEntity().getKiller();
            String mobType = event.getEntityType().name().toLowerCase();
            jobManager.handleAction(player, "kill_" + mobType);
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        // Load player data when they join
        plugin.getJobManager().getPlayerJobs(event.getPlayer());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        // Save player data when they leave
        plugin.getJobManager().savePlayerData(event.getPlayer());
    }

    @EventHandler
    public void onBrewingStandInteract(PlayerInteractEvent event) {
        if (event.getClickedBlock() != null &&
                event.getClickedBlock().getType() == Material.BREWING_STAND) {
            jobManager.handleAction(event.getPlayer(), "use_brewing_stand");
        }
    }

    @EventHandler
    public void onCraftingTableInteract(PlayerInteractEvent event) {
        if (event.getClickedBlock() != null &&
                event.getClickedBlock().getType() == Material.CRAFTING_TABLE) {
            jobManager.handleAction(event.getPlayer(), "use_crafting_table");
        }
    }

    @EventHandler
    public void onEnchantingTableInteract(PlayerInteractEvent event) {
        if (event.getClickedBlock() != null &&
                event.getClickedBlock().getType() == Material.ENCHANTING_TABLE) {
            jobManager.handleAction(event.getPlayer(), "use_enchanting_table");
        }
    }
}