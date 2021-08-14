package ru.arcein.plugins.arceinultimate.modules.fixcps;

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
import org.bukkit.event.player.PlayerJoinEvent;
import ru.arcein.plugins.arceinultimate.core.module.Module;
import ru.arcein.plugins.arceinultimate.core.module.ModulesHandler;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class FixCPSModule extends Module {

    private FileConfiguration config;

    public Map<Player, Long> playersDamage = new HashMap<Player, Long>(){
        public boolean removeEldestEntry(Entry<Player, Long> eldest) {
            return (Long)eldest.getValue() + 10000L <= System.currentTimeMillis();
        }
    };

    public FixCPSModule(ModulesHandler modulesHandler, String moduleName, String apiVersion,
                        String moduleVersion, String moduleClasspath, String moduleInfo) {

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
                new FixCPSListener(this),
                modulesHandler.getPlugin());

        modulesHandler.getPlugin().getServer().getOnlinePlayers().forEach((Player player) -> {
            player.getAttribute(Attribute.GENERIC_ATTACK_SPEED).setBaseValue(1000);
        });

    }

    public void onDisable(){

    }

    private class FixCPSListener implements Listener {
        FixCPSModule module;

        public FixCPSListener(FixCPSModule module){
            this.module = module;
        }

        @EventHandler(
                priority = EventPriority.LOWEST
        )
        public void onPlayerJoin(PlayerJoinEvent event) {
            event.getPlayer().getAttribute(Attribute.GENERIC_ATTACK_SPEED).setBaseValue(1000);
        }

        @EventHandler(
                priority = EventPriority.LOWEST
        )
        public void onEntityDamage(EntityDamageByEntityEvent event) {

            if (event.isCancelled()) return;
            if (event.getCause() != EntityDamageEvent.DamageCause.ENTITY_ATTACK &&
                    event.getCause() != EntityDamageEvent.DamageCause.ENTITY_SWEEP_ATTACK) return;

            Entity attackerEntity = event.getDamager();

            if (!(attackerEntity instanceof Player)) return;

            Player attackerPlayer = (Player) attackerEntity;

            double cps = this.module.config.getDouble("cps");
            double attackRate = 1000.0D/cps;

            if (!this.module.playersDamage.containsKey(attackerPlayer)){
                this.module.playersDamage.put(attackerPlayer, System.currentTimeMillis());
            }
            else{
                if((System.currentTimeMillis() - module.playersDamage.get(attackerPlayer)) > attackRate){
                    this.module.playersDamage.put(attackerPlayer, System.currentTimeMillis());
                }
                else{
                    event.setCancelled(true);
                }
            }

            return;
        }

    }

}
