package ru.arcein.plugins.arceinultimate.modules.hibc;

import org.bukkit.attribute.Attribute;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import ru.arcein.plugins.arceinultimate.core.module.Module;
import ru.arcein.plugins.arceinultimate.core.module.ModulesHandler;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class HIBCModule extends Module {

    private FileConfiguration config;

    public Set<Player> fightingPlayers = new HashSet<>();

    public HIBCModule(ModulesHandler modulesHandler, String moduleName, String apiVersion, String moduleVersion, String moduleClasspath, String moduleInfo) {
        super(modulesHandler, moduleName, apiVersion, moduleVersion, moduleClasspath, moduleInfo);
    }

    public void onLoad() {

        this.copyDefaultResources();
        Path configPath = this.getResource("config.yml");

        try {
            config = YamlConfiguration.loadConfiguration(new InputStreamReader(Files.newInputStream(configPath)));
        } catch (IOException exception) {
            exception.printStackTrace();
        }

        //config.save(configPath.toFile());
    }

    public void onEnable() {

        modulesHandler.getPlugin().getServer().getPluginManager().registerEvents(
                new HIBCListener(this),
                modulesHandler.getPlugin());

        modulesHandler.getPlugin().getServer().getOnlinePlayers().forEach((Player player) ->
                player.getAttribute(Attribute.GENERIC_ATTACK_SPEED).setBaseValue(1000));

    }

    public void onDisable(){

    }

    private class HIBCListener implements Listener {
        HIBCModule module;

        public HIBCListener(HIBCModule module){
            this.module = module;
        }

        @EventHandler(
                priority = EventPriority.HIGH,
                ignoreCancelled = true
        )
        public void onPlayerSwapHands(PlayerSwapHandItemsEvent event) {
            //event.setCancelled(true);

            Player player = event.getPlayer();

            if(fightingPlayers.contains(player)){
                player.sendMessage("Вы вышли из режима использования способностей!");
                fightingPlayers.remove(player);
            }
            else{
                fightingPlayers.add(player);
                player.sendMessage("Вы вошли в режим использования способностей!");
            }
        }


        @EventHandler(
                priority = EventPriority.LOW
        )
        public void onPlayerItemHeldChange(PlayerItemHeldEvent event) {
            //event.setCancelled(true);

            Player player = event.getPlayer();

            if(fightingPlayers.contains(player)){
                player.sendMessage("Вы попытались сменить слот " + event.getPreviousSlot()
                        + " на слот " + event.getNewSlot() + " в режиме боя!");
                event.setCancelled(true);
            }
        }



    }
}
