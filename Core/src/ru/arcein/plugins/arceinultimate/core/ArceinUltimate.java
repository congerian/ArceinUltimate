package ru.arcein.plugins.arceinultimate.core;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public class ArceinUltimate extends JavaPlugin {
    public void onLoad() {
        Bukkit.getLogger().info("[ArceinUltimate] The CORE has started loading!");
        //...
        Bukkit.getLogger().info("[ArceinUltimate] The CORE has been loaded!");
    }

    public void onEnable() {
        Bukkit.getLogger().info("[ArceinUltimate] The CORE has started enabling!");
        //...
        Bukkit.getLogger().info("[ArceinUltimate] The CORE has been enabled!");
    }

    public void onDisable(){
        Bukkit.getLogger().info("[ArceinUltimate] The CORE has started disabling!");
        //...
        Bukkit.getLogger().info("[ArceinUltimate] The CORE has been disabled!");
    }
}
