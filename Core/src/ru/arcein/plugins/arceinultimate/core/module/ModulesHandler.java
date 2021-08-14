package ru.arcein.plugins.arceinultimate.core.module;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import ru.arcein.plugins.arceinultimate.core.ArceinUltimate;

import java.util.HashMap;
import java.util.Map;

public class ModulesHandler {

    protected ArceinUltimate plugin;
    protected Map<String, Module> loadedModules;
    protected Map<String, Module> modules = new HashMap<>();

    private ModulesLoader loader;

    public ModulesHandler(ArceinUltimate plugin){
        plugin.getLogger().info(ChatColor.AQUA + "The ModulesHandler has started loading!");

        this.plugin = plugin;
        this.loader = new ModulesLoader(this);
        this.loadedModules = new HashMap<>();

        plugin.getLogger().info(ChatColor.GREEN + "The ModulesHandler has been loaded!");
    }

    public void loadModules(){
        loadedModules = new HashMap<>();
        loader.loadModules();
    }

    public void enableModules(){

        if(!modules.isEmpty()){
            plugin.getLogger().warning(ChatColor.RED + "There are some already enabled modules. Disable them first.");
            return;
        }

        plugin.getLogger().info(ChatColor.AQUA + "Enabling modules...");

        for(Map.Entry<String, Module> entry : loadedModules.entrySet()){

            Module module = entry.getValue();
            plugin.getLogger().info(ChatColor.AQUA + "Enabling " + module.moduleName + "...");

            try{
                module.onEnable();
                modules.put(entry.getKey(), entry.getValue());
                plugin.getLogger().info(ChatColor.GREEN + module.moduleName + " module has been enabled!");

            } catch (Exception exception) {
                exception.printStackTrace();
                plugin.getLogger().warning(ChatColor.RED + "Error enabling " + entry.getKey() + " module!");
                plugin.getLogger().warning(ChatColor.RED + "Unloading it...");
            }

        }

        plugin.getLogger().info(ChatColor.AQUA + "" + modules.size() + " modules have been enabled!");
    }

    public void disableModules(){

        plugin.getLogger().info(ChatColor.AQUA + "Disabling modules...!");

        for(Map.Entry<String, Module> entry : modules.entrySet()){

            Module module = entry.getValue();
            plugin.getLogger().info(ChatColor.AQUA + "Disabling " + module.moduleName + "...");

            try{
                module.onDisable();
                plugin.getLogger().info(ChatColor.GREEN + module.moduleName + " module has been disabled!");

            } catch (Exception exception) {
                exception.printStackTrace();
                plugin.getLogger().warning(ChatColor.RED + "Error disabling " + entry.getKey() + " module!");
                plugin.getLogger().warning(ChatColor.RED + "Skipping it...");
            }

        }

        plugin.getLogger().info(ChatColor.GREEN + "Modules have been disabled!");

        modules = new HashMap<>();
    }

}
