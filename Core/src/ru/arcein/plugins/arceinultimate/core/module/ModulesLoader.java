package ru.arcein.plugins.arceinultimate.core.module;

import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import ru.arcein.plugins.arceinultimate.core.module.event.ModuleLoadEvent;
import ru.arcein.plugins.arceinultimate.core.module.exception.ModuleLoadException;
import ru.arcein.plugins.arceinultimate.core.module.exception.WrongModuleConfigException;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.net.URLClassLoader;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class ModulesLoader extends URLClassLoader {
    public final static String MODULE_YML_INFO = "module.yml";

    protected ModulesHandler handler;

    public ModulesLoader(ModulesHandler handler){
        super(((URLClassLoader)handler.plugin.getClass().getClassLoader()).getURLs(), handler.plugin.getClass().getClassLoader());

        handler.plugin.getLogger().info(ChatColor.AQUA + "The ModulesLoader has started loading!");

        this.handler = handler;

        handler.plugin.getLogger().info(ChatColor.GREEN + "The ModulesLoader has been loaded!");
    }

    public void loadModules(){

        handler.plugin.getLogger().info(ChatColor.AQUA + "Modules have started loading!");
        Map<String, File> moduleFiles = new HashMap<>();

        File dir = new File(handler.plugin.getDataFolder(), "modules");

        if(!dir.exists()){
            boolean newFolder = dir.mkdirs();
            if(!newFolder){
                handler.plugin.getLogger().warning(ChatColor.RED + "Can't create /modules folder!");
                handler.plugin.getLogger().warning(ChatColor.RED + "Stopping loading...");
                return;
            }
        }

        String[] pathsToFiles = dir.list();

        if(pathsToFiles == null){
            handler.plugin.getLogger().warning(ChatColor.RED + "Can't get module paths!");
            handler.plugin.getLogger().warning(ChatColor.RED + "Stopping loading...");
            return;
        }

        for(String filePath : pathsToFiles){
            File file = new File(dir, filePath);
            if(!filePath.contains(".jar")) {
                handler.plugin.getLogger().warning(ChatColor.RED + filePath + " is not a .jar file!");
                handler.plugin.getLogger().warning(ChatColor.RED + "Skipping...");
                continue;
            }
            moduleFiles.put(filePath, file);
        }

        for(Map.Entry<String, File> entry : moduleFiles.entrySet()){
            try {
                JarFile moduleJar = new JarFile(entry.getValue());
                //module.yml
                boolean isConfigFound = false;
                String moduleName;
                String apiVersion;
                String moduleVersion;
                String moduleClasspath;
                String moduleInfo;

                handler.plugin.getLogger().info(ChatColor.AQUA + "Loading " + moduleJar.getName() + "...");

                for(JarEntry jarInsideFile : Collections.list(moduleJar.entries())){

                    if(!jarInsideFile.getName().equals(MODULE_YML_INFO)) continue;

                    isConfigFound = true;

                    FileConfiguration config = YamlConfiguration.loadConfiguration(new InputStreamReader(moduleJar.getInputStream(jarInsideFile)));

                    moduleName = config.getString("name");
                    apiVersion = config.getString("api-version");
                    moduleVersion = config.getString("version");
                    moduleClasspath = config.getString("classpath");
                    moduleInfo = config.getString("info");

                    if(moduleInfo == null) moduleInfo = "";

                    if(moduleName == null || moduleName.isEmpty())
                        throw new WrongModuleConfigException("Module name can not be null or empty.");

                    if(apiVersion == null || apiVersion.isEmpty())
                        throw new WrongModuleConfigException("API version can not be null or empty.");

                    if(moduleVersion == null || moduleVersion.isEmpty())
                        throw new WrongModuleConfigException("Version can not be null or empty.");

                    if(moduleClasspath == null || moduleClasspath.isEmpty())
                        throw new WrongModuleConfigException("Module Classpath can not be null or empty.");

                    //TODO: Check api-version

                    //TODO: Compare versions and unload older if present

                    this.addURL(entry.getValue().toURI().toURL());

                    Class<?> ModuleClass = this.loadClass(moduleClasspath);

                    if(Modifier.isAbstract(ModuleClass.getModifiers()))
                        throw new ModuleLoadException("Module class can not be abstract!");

                    Class<? extends Module> CastedModuleClass = ModuleClass.asSubclass(Module.class);
                    Constructor<? extends Module> CastedModuleClassConstructor =
                            CastedModuleClass.getConstructor(
                                    this.handler.getClass(), moduleName.getClass(),
                                    apiVersion.getClass(), moduleVersion.getClass(),
                                    moduleClasspath.getClass(), moduleInfo.getClass()
                            );

                    Module module = CastedModuleClassConstructor.newInstance(
                            this.handler, moduleName,
                            apiVersion, moduleVersion,
                            moduleClasspath, moduleInfo);

                    module.onLoad();

                    handler.loadedModules.put(moduleName, module);

                    ModuleLoadEvent moduleLoadEvent = new ModuleLoadEvent(moduleName);
                    handler.plugin.getServer().getPluginManager().callEvent(moduleLoadEvent);

                    handler.plugin.getLogger().info(ChatColor.GREEN + "Module " + moduleName + " was successfully loaded!");

                    handler.plugin.getLogger().info(ChatColor.GREEN + "------------------------------------------");
                    handler.plugin.getLogger().info(ChatColor.GREEN + "               Module info:               ");
                    handler.plugin.getLogger().info(ChatColor.GREEN + "------------------------------------------");
                    handler.plugin.getLogger().info(ChatColor.GREEN + "Name:              " + moduleName);
                    handler.plugin.getLogger().info(ChatColor.GREEN + "API version:       " + apiVersion);
                    handler.plugin.getLogger().info(ChatColor.GREEN + "Version:           " + moduleVersion);
                    handler.plugin.getLogger().info(ChatColor.GREEN + "Module Classpath:  " + moduleClasspath);

                    if(!moduleInfo.isEmpty())
                        handler.plugin.getLogger().info(ChatColor.GREEN + "Module INFO:       " + moduleInfo);

                    handler.plugin.getLogger().info(ChatColor.GREEN + "------------------------------------------");

                }

                if(!isConfigFound){
                    handler.plugin.getLogger().warning(ChatColor.RED + "No config file found in " + moduleJar.getName());
                    handler.plugin.getLogger().warning(ChatColor.RED + "Skipping...");
                }


            }

            catch (SecurityException exception){
                handler.plugin.getLogger().warning(ChatColor.RED + "Error during initialization of " + entry.getKey() + "!");
                handler.plugin.getLogger().warning(ChatColor.RED + "File system security error!");
                handler.plugin.getLogger().warning(ChatColor.RED + exception.getMessage());
                handler.plugin.getLogger().warning(ChatColor.RED + "Skipping module...");
            }

            catch (IOException exception){
                handler.plugin.getLogger().warning(ChatColor.RED + "Error during initialization of " + entry.getKey() + "!");
                handler.plugin.getLogger().warning(ChatColor.RED + "I/O error!");
                handler.plugin.getLogger().warning(ChatColor.RED + exception.getMessage());
                handler.plugin.getLogger().warning(ChatColor.RED + "Skipping module...");
            }

            catch (ClassNotFoundException exception) {
                handler.plugin.getLogger().warning(ChatColor.RED + "Error during initialization of " + entry.getKey() + "!");
                handler.plugin.getLogger().warning(ChatColor.RED + "Could not detect the proper Module class to load.");
                handler.plugin.getLogger().warning(ChatColor.RED + "Classpath: " + exception.getMessage());
                handler.plugin.getLogger().warning(ChatColor.RED + "Skipping module...");
            }

            catch (ModuleLoadException exception){
                handler.plugin.getLogger().warning(ChatColor.RED + "Error during initialization of " + entry.getKey() + "!");
                handler.plugin.getLogger().warning(ChatColor.RED + exception.getMessage());
                handler.plugin.getLogger().warning(ChatColor.RED + "Skipping module...");
            }

            catch (ReflectiveOperationException exception){
                handler.plugin.getLogger().warning(ChatColor.RED + "Error during initialization of " + entry.getKey() + "!");
                handler.plugin.getLogger().warning(ChatColor.RED + exception.getMessage());
                handler.plugin.getLogger().warning(ChatColor.RED + "Skipping module...");
            }

        }

        handler.plugin.getLogger().warning(ChatColor.GREEN + "Module loading has been finished. " +
                handler.loadedModules.size() + " modules loaded!");

        try{
            this.close();
        } catch (IOException exception) {
            handler.plugin.getLogger().warning(ChatColor.RED + "Error closing all opened files!");
            handler.plugin.getLogger().warning(ChatColor.RED + exception.getMessage());
            exception.printStackTrace();
        }
    }

}
