package com.gamo.gamoeconpro.jobs;

import com.gamo.gamoeconpro.GamoEconPro;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;

public enum JobType {
    MINER("Miner", "Mine ores and stone blocks"),
    FARMER("Farmer", "Grow crops and breed animals"),
    LUMBERJACK("Lumberjack", "Chop trees and gather wood"),
    DIGGER("Digger", "Excavate dirt and sand"),
    EXPLORER("Explorer", "Explore new areas and discover treasures"),
    HUNTER("Hunter", "Hunt mobs and animals"),
    FISHERMAN("Fisherman", "Catch fish and treasure"),
    BREWER("Brewer", "Brew potions and alchemy"),
    BUILDER("Builder", "Construct buildings and structures"),
    CRAFTER("Crafter", "Craft items and tools"),
    ENCHANTER("Enchanter", "Enchant gear and weapons"),
    WEAPONSMITH("Weaponsmith", "Forge weapons and armor");

    private final String displayName;
    private final String description;
    private Map<Material, Double> paymentMap;
    private Map<String, Double> actionPayments;
    private List<String> quests;

    JobType(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
        this.paymentMap = new HashMap<>();
        this.actionPayments = new HashMap<>();
        this.quests = new ArrayList<>();
    }

    public void loadConfig() {
        GamoEconPro plugin = GamoEconPro.getInstance();
        if (plugin == null) {
            return;
        }

        File configFile = new File(plugin.getDataFolder(), "jobs/" + name().toLowerCase() + ".yml");
        if (!configFile.exists()) {
            try {
                configFile.getParentFile().mkdirs();
                configFile.createNewFile();
                createDefaultConfig(configFile);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);

        // Load block payments
        ConfigurationSection blocks = config.getConfigurationSection("blocks");
        if (blocks != null) {
            for (String key : blocks.getKeys(false)) {
                try {
                    Material material = Material.matchMaterial(key.toUpperCase());
                    if (material != null) {
                        paymentMap.put(material, blocks.getDouble(key));
                    }
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("Invalid material in job config: " + key);
                }
            }
        }

        // Load action payments
        ConfigurationSection actions = config.getConfigurationSection("actions");
        if (actions != null) {
            for (String action : actions.getKeys(false)) {
                actionPayments.put(action, actions.getDouble(action));
            }
        }

        // Load quests
        quests = config.getStringList("quests");
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    public double getPayment(Material material) {
        return paymentMap.getOrDefault(material, 0.0);
    }

    public double getActionPayment(String action) {
        return actionPayments.getOrDefault(action, 0.0);
    }

    public List<String> getQuests() {
        return quests;
    }

    private void createDefaultConfig(File configFile) throws IOException {
        FileConfiguration config = new YamlConfiguration();

        // Default block payments
        ConfigurationSection blocks = config.createSection("blocks");
        switch (this) {
            case MINER:
                blocks.set("STONE", 1.0);
                blocks.set("DEEPSLATE", 1.25);
                blocks.set("GRANITE", 1.0);
                blocks.set("DIORITE", 1.0);
                blocks.set("ANDESITE", 1.0);
                blocks.set("COAL_ORE", 3.0);
                blocks.set("DEEPSLATE_COAL_ORE", 4.0);
                blocks.set("IRON_ORE", 3.5);
                blocks.set("DEEPSLATE_IRON_ORE", 4.5);
                blocks.set("COPPER_ORE", 3.5);
                blocks.set("DEEPSLATE_COPPER_ORE", 4.5);
                blocks.set("GOLD_ORE", 5.0);
                blocks.set("DEEPSLATE_GOLD_ORE", 6.0);
                blocks.set("REDSTONE_ORE", 2.5);
                blocks.set("DEEPSLATE_REDSTONE_ORE", 3.5);
                blocks.set("LAPIS_ORE", 7.5);
                blocks.set("DEEPSLATE_LAPIS_ORE", 8.5);
                blocks.set("DIAMOND_ORE", 10.0);
                blocks.set("DEEPSLATE_DIAMOND_ORE", 12.5);
                blocks.set("EMERALD_ORE", 15.0);
                blocks.set("DEEPSLATE_EMERALD_ORE", 17.5);
                blocks.set("NETHER_QUARTZ_ORE", 2.5);
                blocks.set("OBSIDIAN", 5.0);
                blocks.set("NETHER_BRICKS", 1.0);
                blocks.set("NETHERRACK", 0.1);
                blocks.set("PRISMARINE", 2.5);
                blocks.set("PRISMARINE_BRICKS", 2.5);
                blocks.set("DARK_PRISMARINE", 2.5);
                break;
            case FARMER:
                blocks.set("WHEAT", 1.5);
                blocks.set("CARROTS", 1.0);
                blocks.set("POTATOES", 1.0);
                blocks.set("BEETROOTS", 1.5);
                blocks.set("PUMPKIN", 0.5);
                blocks.set("MELON", 0.5);
                blocks.set("SUGAR_CANE", 0.2);
                blocks.set("COCOA", 4.0);
                blocks.set("CHORUS_PLANT", 1.5);
                blocks.set("CHORUS_FLOWER", 1.5);
                blocks.set("NETHER_WART", 1.0);
                blocks.set("LILY_PAD", 2.0);
                blocks.set("CACTUS", 1.0);
                blocks.set("VINES", 1.0);
                ConfigurationSection actions = config.createSection("actions");
                actions.set("breed_sheep", 4.0);
                actions.set("breed_cow", 4.0);
                actions.set("breed_pig", 4.0);
                actions.set("breed_chicken", 4.0);
                actions.set("breed_wolf", 4.0);
                actions.set("breed_ocelot", 4.0);
                actions.set("breed_rabbit", 4.0);
                actions.set("breed_llama", 4.0);
                actions.set("breed_turtle", 4.0);
                actions.set("shear_wool", 4.0);
                actions.set("milk_cow", 5.0);
                actions.set("tame_wolf", 5.0);
                actions.set("tame_horse", 5.0);
                actions.set("tame_parrot", 5.0);
                actions.set("tame_ocelot", 5.0);
                actions.set("tame_llama", 5.0);
                break;
            case LUMBERJACK:
                blocks.set("OAK_LOG", 1.0);
                blocks.set("BIRCH_LOG", 1.0);
                blocks.set("SPRUCE_LOG", 1.0);
                blocks.set("JUNGLE_LOG", 1.0);
                blocks.set("ACACIA_LOG", 1.0);
                blocks.set("DARK_OAK_LOG", 1.0);
                blocks.set("STRIPPED_OAK_LOG", 0.75);
                blocks.set("STRIPPED_BIRCH_LOG", 0.75);
                blocks.set("STRIPPED_SPRUCE_LOG", 0.75);
                blocks.set("STRIPPED_JUNGLE_LOG", 0.75);
                blocks.set("STRIPPED_ACACIA_LOG", 0.75);
                blocks.set("STRIPPED_DARK_OAK_LOG", 0.75);
                break;
            case BUILDER:
                blocks.set("STONE", 1.3);
                blocks.set("ANDESITE", 1.3);
                blocks.set("GRANITE", 1.3);
                blocks.set("DIORITE", 1.3);
                blocks.set("COBBLESTONE", 0.7);
                blocks.set("OAK_PLANKS", 1.5);
                blocks.set("SPRUCE_PLANKS", 1.5);
                blocks.set("BIRCH_PLANKS", 1.5);
                blocks.set("JUNGLE_PLANKS", 1.8);
                blocks.set("ACACIA_PLANKS", 1.5);
                blocks.set("DARK_OAK_PLANKS", 1.5);
                blocks.set("WOOL", 1.5);
                blocks.set("GOLD_BLOCK", 1.5);
                blocks.set("IRON_BLOCK", 55.0);
                blocks.set("BRICKS", 1.5);
                blocks.set("BOOKSHELF", 1.5);
                blocks.set("MOSSY_COBBLESTONE", 1.5);
                blocks.set("OBSIDIAN", 1.5);
                blocks.set("CHEST", 1.5);
                blocks.set("DIAMOND_BLOCK", 1.5);
                blocks.set("CRAFTING_TABLE", 1.5);
                blocks.set("FURNACE", 1.5);
                blocks.set("LADDER", 1.5);
                blocks.set("ICE", 1.5);
                blocks.set("SNOW_BLOCK", 1.0);
                blocks.set("JUKEBOX", 1.0);
                blocks.set("GLOWSTONE", 1.5);
                blocks.set("GLASS", 8.0);
                blocks.set("STONE_BRICKS", 1.5);
                blocks.set("MOSSY_STONE_BRICKS", 1.5);
                blocks.set("CRACKED_STONE_BRICKS", 1.5);
                blocks.set("CHISELED_STONE_BRICKS", 1.5);
                blocks.set("IRON_BARS", 1.5);
                blocks.set("NETHER_BRICKS", 1.5);
                blocks.set("ENCHANTING_TABLE", 1.5);
                blocks.set("ENDER_CHEST", 1.5);
                blocks.set("EMERALD_BLOCK", 1.5);
                blocks.set("REDSTONE_BLOCK", 1.5);
                blocks.set("HOPPER", 1.5);
                blocks.set("QUARTZ_BLOCK", 36.0);
                blocks.set("SMOOTH_QUARTZ", 145.0);
                blocks.set("QUARTZ_PILLAR", 75.0);
                blocks.set("CHISELED_QUARTZ_BLOCK", 1.5);
                blocks.set("TERRACOTTA", 1.5);
                blocks.set("PACKED_ICE", 1.5);
                blocks.set("PRISMARINE", 1.3);
                blocks.set("PRISMARINE_BRICKS", 1.3);
                blocks.set("DARK_PRISMARINE", 1.3);
                blocks.set("SEA_LANTERN", 1.5);
                blocks.set("HAY_BLOCK", 1.5);
                blocks.set("COAL_BLOCK", 1.5);
                break;
            case DIGGER:
                blocks.set("DIRT", 2.25);
                blocks.set("GRASS_BLOCK", 0.3);
                blocks.set("GRAVEL", 1.0);
                blocks.set("SAND", 0.4);
                blocks.set("RED_SAND", 1.0);
                blocks.set("CLAY", 1.0);
                blocks.set("COARSE_DIRT", 1.0);
                break;
            // Add defaults for other jobs
        }

        // Default action payments
        ConfigurationSection actions = config.createSection("actions");
        switch (this) {
            case EXPLORER:
                ConfigurationSection explorerActions = config.createSection("actions");
                explorerActions.set("explore_level_1", 5.0);
                explorerActions.set("explore_level_2", 2.5);
                explorerActions.set("explore_level_3", 1.0);
                explorerActions.set("explore_level_4", 0.5);
                explorerActions.set("explore_level_5", 0.1);
                explorerActions.set("brush_suspicious_sand", 5.0);
                explorerActions.set("brush_suspicious_gravel", 6.0);
                explorerActions.set("brush_coal", 10.0);
                break;
            case HUNTER:
                ConfigurationSection hunterActions = config.createSection("actions");
                hunterActions.set("kill_chicken", 5.0);
                hunterActions.set("kill_cow", 6.0);
                hunterActions.set("kill_pig", 5.0);
                hunterActions.set("kill_sheep", 5.0);
                hunterActions.set("kill_wolf", 10.0);
                hunterActions.set("kill_creeper", 15.0);
                hunterActions.set("kill_skeleton", 10.0);
                hunterActions.set("kill_spider", 10.0);
                hunterActions.set("kill_zombie", 10.0);
                hunterActions.set("kill_blaze", 20.0);
                hunterActions.set("kill_cave_spider", 20.0);
                hunterActions.set("kill_enderman", 2.0);
                hunterActions.set("kill_ghast", 30.0);
                hunterActions.set("kill_iron_golem", 30.0);
                hunterActions.set("kill_mushroom_cow", 5.0);
                hunterActions.set("kill_guardian", 2.0);
                hunterActions.set("kill_shulker", 5.0);
                hunterActions.set("kill_phantom", 5.0);
                hunterActions.set("kill_drowned", 5.0);
                hunterActions.set("kill_husk", 5.0);
                hunterActions.set("kill_wither", 50.0);
                hunterActions.set("kill_ender_dragon", 2000.0);
                hunterActions.set("tame_wolf", 20.0);
                hunterActions.set("tame_ocelot", 20.0);
                hunterActions.set("tame_horse", 20.0);
                break;
            case FISHERMAN:
                ConfigurationSection fishermanActions = config.createSection("actions");
                fishermanActions.set("catch_cod", 15.0);
                fishermanActions.set("catch_salmon", 20.0);
                fishermanActions.set("catch_tropical_fish", 25.0);
                fishermanActions.set("catch_pufferfish", 25.0);
                break;
            case BREWER:
                ConfigurationSection brewerActions = config.createSection("actions");
                brewerActions.set("brew_potion", 5.0);
                brewerActions.set("add_nether_wart", 6.0);
                brewerActions.set("add_glowstone", 8.0);
                brewerActions.set("add_redstone", 6.0);
                brewerActions.set("add_fermented_spider_eye", 12.0);
                brewerActions.set("add_gunpowder", 6.0);
                brewerActions.set("add_dragon_breath", 25.0);
                brewerActions.set("add_sugar", 7.0);
                brewerActions.set("add_rabbit_foot", 18.0);
                brewerActions.set("add_glistering_melon", 10.0);
                brewerActions.set("add_spider_eye", 9.0);
                brewerActions.set("add_pufferfish", 14.0);
                brewerActions.set("add_magma_cream", 12.0);
                brewerActions.set("add_golden_carrot", 14.0);
                brewerActions.set("add_blaze_powder", 12.0);
                brewerActions.set("add_ghast_tear", 22.0);
                brewerActions.set("add_turtle_helmet", 12.0);
                brewerActions.set("add_phantom_membrane", 12.0);
                break;
            case WEAPONSMITH:
                ConfigurationSection weaponsmithActions = config.createSection("actions");
                weaponsmithActions.set("craft_wooden_sword", 1.0);
                weaponsmithActions.set("craft_stone_sword", 2.0);
                weaponsmithActions.set("craft_iron_sword", 3.0);
                weaponsmithActions.set("craft_golden_sword", 4.0);
                weaponsmithActions.set("craft_diamond_sword", 5.0);
                weaponsmithActions.set("craft_leather_boots", 3.0);
                weaponsmithActions.set("craft_leather_chestplate", 4.0);
                weaponsmithActions.set("craft_leather_helmet", 2.0);
                weaponsmithActions.set("craft_leather_leggings", 3.0);
                weaponsmithActions.set("craft_iron_boots", 5.0);
                weaponsmithActions.set("craft_iron_chestplate", 8.0);
                weaponsmithActions.set("craft_iron_helmet", 5.0);
                weaponsmithActions.set("craft_iron_leggings", 7.0);
                weaponsmithActions.set("craft_golden_boots", 6.0);
                weaponsmithActions.set("craft_golden_chestplate", 10.0);
                weaponsmithActions.set("craft_golden_helmet", 6.0);
                weaponsmithActions.set("craft_golden_leggings", 9.0);
                weaponsmithActions.set("craft_diamond_boots", 10.0);
                weaponsmithActions.set("craft_diamond_chestplate", 15.0);
                weaponsmithActions.set("craft_diamond_helmet", 10.0);
                weaponsmithActions.set("craft_diamond_leggings", 13.0);
                break;
            case CRAFTER:
                ConfigurationSection crafterActions = config.createSection("actions");
                crafterActions.set("craft_stick", 0.1);
                crafterActions.set("craft_dispenser", 4.0);
                crafterActions.set("craft_note_block", 1.5);
                crafterActions.set("craft_powered_rail", 5.0);
                crafterActions.set("craft_detector_rail", 5.0);
                crafterActions.set("craft_sticky_piston", 3.0);
                crafterActions.set("craft_tnt", 4.0);
                crafterActions.set("craft_chest", 1.3);
                crafterActions.set("craft_crafting_table", 0.7);
                crafterActions.set("craft_furnace", 1.0);
                crafterActions.set("craft_ladder", 0.1);
                crafterActions.set("craft_rail", 1.1);
                crafterActions.set("craft_jukebox", 10.0);
                crafterActions.set("craft_enchanting_table", 30.0);
                crafterActions.set("craft_beacon", 100.0);
                crafterActions.set("craft_anvil", 20.0);
                crafterActions.set("craft_hopper", 7.0);
                crafterActions.set("craft_dropper", 2.0);
                crafterActions.set("smelt_cooked_chicken", 3.0);
                break;
            case ENCHANTER:
                ConfigurationSection enchanterActions = config.createSection("actions");
                enchanterActions.set("enchant_wood_sword", 1.5);
                enchanterActions.set("enchant_leather_boots", 1.0);
                enchanterActions.set("enchant_leather_chestplate", 2.0);
                enchanterActions.set("enchant_leather_helmet", 1.0);
                enchanterActions.set("enchant_leather_leggings", 2.0);
                enchanterActions.set("enchant_iron_sword", 3.0);
                enchanterActions.set("enchant_iron_boots", 2.5);
                enchanterActions.set("enchant_iron_chestplate", 4.5);
                enchanterActions.set("enchant_iron_helmet", 2.5);
                enchanterActions.set("enchant_iron_leggings", 4.5);
                enchanterActions.set("enchant_gold_sword", 4.5);
                enchanterActions.set("enchant_gold_boots", 2.5);
                enchanterActions.set("enchant_gold_chestplate", 5.5);
                enchanterActions.set("enchant_gold_helmet", 2.5);
                enchanterActions.set("enchant_gold_leggings", 5.5);
                enchanterActions.set("enchant_diamond_sword", 9.0);
                enchanterActions.set("enchant_diamond_shovel", 5.0);
                enchanterActions.set("enchant_diamond_pickaxe", 10.0);
                enchanterActions.set("enchant_diamond_axe", 10.0);
                enchanterActions.set("enchant_diamond_helmet", 6.0);
                enchanterActions.set("enchant_diamond_chestplate", 12.0);
                enchanterActions.set("enchant_diamond_leggings", 12.0);
                enchanterActions.set("enchant_diamond_boots", 6.0);
                // Add enchantment specific payments
                enchanterActions.set("apply_efficiency_1", 1.0);
                enchanterActions.set("apply_efficiency_2", 2.0);
                enchanterActions.set("apply_efficiency_3", 3.0);
                enchanterActions.set("apply_efficiency_4", 4.0);
                enchanterActions.set("apply_unbreaking_1", 1.0);
                enchanterActions.set("apply_unbreaking_2", 2.0);
                enchanterActions.set("apply_unbreaking_3", 3.0);
                enchanterActions.set("apply_silk_touch", 6.0);
                enchanterActions.set("apply_fortune_1", 4.0);
                enchanterActions.set("apply_fortune_2", 5.0);
                enchanterActions.set("apply_fortune_3", 6.0);
                enchanterActions.set("apply_protection_1", 1.0);
                enchanterActions.set("apply_protection_2", 2.0);
                enchanterActions.set("apply_protection_3", 2.0);
                enchanterActions.set("apply_protection_4", 3.0);
                enchanterActions.set("apply_sharpness_1", 2.0);
                enchanterActions.set("apply_sharpness_2", 3.0);
                enchanterActions.set("apply_sharpness_3", 4.0);
                enchanterActions.set("apply_sharpness_4", 5.0);
                enchanterActions.set("apply_power_1", 1.0);
                enchanterActions.set("apply_power_2", 2.0);
                enchanterActions.set("apply_power_3", 3.0);
                enchanterActions.set("apply_power_4", 4.0);
                break;
            // Add defaults for other jobs
        }

        // Default quests
        config.set("quests", Arrays.asList(
                "Complete basic tasks to level up",
                "Reach level 5 for reduced taxes",
                "Reach level 10 for bonus earnings"
        ));

        config.save(configFile);
    }

    public static JobType getByName(String name) {
        for (JobType job : values()) {
            if (job.name().equalsIgnoreCase(name)) {
                return job;
            }
        }
        return null;
    }

    // Static method to load all job configs
    public static void loadAllConfigs() {
        for (JobType job : values()) {
            job.loadConfig();
        }
    }
}