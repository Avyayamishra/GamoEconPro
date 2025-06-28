package com.gamo.gamoeconpro.business;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class BusinessLicenseItem {

    public static ItemStack create(String businessName) {
        ItemStack license = new ItemStack(Material.PAPER);
        ItemMeta meta = license.getItemMeta();

        meta.setDisplayName("§6Business License: " + businessName);
        meta.setUnbreakable(true);
        meta.setLore(java.util.Arrays.asList(
                "§7Official government-issued",
                "§eThis proves your business is licensed."
        ));

        license.setItemMeta(meta);
        return license;
    }
}
