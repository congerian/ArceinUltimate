package ru.arcein.plugins.arceinultimate.core;

import org.bukkit.ChatColor;
import org.bukkit.plugin.java.JavaPlugin;
import ru.arcein.plugins.arceinultimate.core.integration.IntegrationsHandler;
import ru.arcein.plugins.arceinultimate.core.module.ModulesHandler;

public class ArceinUltimate extends JavaPlugin {

    ModulesHandler modulesHandler;
    IntegrationsHandler integrationsHandler;

    public void onLoad() {
        this.getLogger().info(ChatColor.AQUA + "The CORE has started loading!");

        modulesHandler = new ModulesHandler(this);
        integrationsHandler = new IntegrationsHandler(this);

        modulesHandler.loadModules();
        //...
        this.getLogger().info(ChatColor.GREEN + "The CORE has been loaded!");
    }

    public void onEnable() {
        this.getLogger().info(ChatColor.AQUA + "The CORE has started enabling!");

        modulesHandler.enableModules();
        //...
        this.getLogger().info(ChatColor.GREEN + "The CORE has been enabled!");
    }

    public void onDisable(){
        this.getLogger().info(ChatColor.AQUA + "The CORE has started disabling!");

        modulesHandler.disableModules();
        //...
        this.getLogger().info(ChatColor.GREEN + "The CORE has been disabled!");
    }
}
