package ru.arcein.plugins.arceinultimate.core.module;

import com.sun.org.apache.xpath.internal.operations.Mod;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import ru.arcein.plugins.arceinultimate.core.module.event.ModuleLoadEvent;
import ru.arcein.plugins.arceinultimate.core.module.exception.ModuleLoadException;
import ru.arcein.plugins.arceinultimate.core.module.exception.WrongModuleConfigException;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class ModulesLoader extends URLClassLoader {
    public final static String MODULE_YML_INFO = "module.yml";

    protected ModulesHandler handler;

    public ModulesLoader(ModulesHandler handler){
        super(((URLClassLoader)handler.getPlugin().getClass().getClassLoader()).getURLs(), handler.getPlugin().getClass().getClassLoader());

        handler.getPlugin().getLogger().info(ChatColor.AQUA + "The ModulesLoader has started loading!");

        this.handler = handler;

        handler.getPlugin().getLogger().info(ChatColor.GREEN + "The ModulesLoader has been loaded!");
    }

    public void loadModules(){

        handler.getPlugin().getLogger().info(ChatColor.AQUA + "Modules have started loading!");
        Map<String, File> moduleFiles = new HashMap<>();

        File dir = new File(handler.getPlugin().getDataFolder(), "modules");

        if(!dir.exists()){
            boolean newFolder = dir.mkdirs();
            if(!newFolder){
                handler.getPlugin().getLogger().warning(ChatColor.RED + "Can't create /modules folder!");
                handler.getPlugin().getLogger().warning(ChatColor.RED + "Stopping loading...");
                return;
            }
        }

        String[] pathsToFiles = dir.list();

        if(pathsToFiles == null){
            handler.getPlugin().getLogger().warning(ChatColor.RED + "Can't get module paths!");
            handler.getPlugin().getLogger().warning(ChatColor.RED + "Stopping loading...");
            return;
        }

        for(String filePath : pathsToFiles){
            File file = new File(dir, filePath);
            if(file.isDirectory()){
                handler.getPlugin().getLogger().warning(ChatColor.RED + filePath + " is a directory!");
                handler.getPlugin().getLogger().warning(ChatColor.RED + "Skipping...");
                continue;
            }
            if(!filePath.contains(".jar")) {
                handler.getPlugin().getLogger().warning(ChatColor.RED + filePath + " is not a .jar file!");
                handler.getPlugin().getLogger().warning(ChatColor.RED + "Skipping...");
                continue;
            }
            moduleFiles.put(filePath, file);
        }

        for(Map.Entry<String, File> entry : moduleFiles.entrySet()){
            try {
                JarFile moduleJar = new JarFile(entry.getValue());
                //module.yml
                String moduleName;
                String apiVersion;
                String moduleVersion;
                String moduleClasspath;
                String moduleInfo;

                handler.getPlugin().getLogger().info(ChatColor.AQUA + "Loading " + moduleJar.getName() + "...");

                JarEntry configEntry = moduleJar.getJarEntry(MODULE_YML_INFO);

                if(configEntry == null){
                    handler.getPlugin().getLogger().warning(ChatColor.RED + "No config file found in " + moduleJar.getName());
                    handler.getPlugin().getLogger().warning(ChatColor.RED + "Skipping...");
                    continue;
                }

                InputStreamReader configReader = new InputStreamReader(moduleJar.getInputStream(configEntry));
                FileConfiguration config = YamlConfiguration.loadConfiguration(configReader);

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

                Class<?> ModuleClass =
                        //this.loadClass(moduleClasspath);
                        Class.forName(moduleClasspath, true, this);

                if(Modifier.isAbstract(ModuleClass.getModifiers()))
                    throw new ModuleLoadException("Module class can not be abstract!");

                File moduleDataFolder = new File(dir, File.separator + moduleName);
                File moduleDataDefaultsFolder = new File(moduleDataFolder, File.separator + "defaults");

                if(!moduleDataFolder.exists()){
                    boolean newFolder = moduleDataFolder.mkdirs();
                    if(!newFolder){
                        handler.getPlugin().getLogger().warning(ChatColor.RED + "Can't create /modules/" + moduleName +" folder!");
                        handler.getPlugin().getLogger().warning(ChatColor.RED + "Stopping loading...");
                        return;
                    }
                }

                if(!moduleDataDefaultsFolder.exists()){
                    boolean newFolder = moduleDataDefaultsFolder.mkdirs();
                    if(!newFolder){
                        handler.getPlugin().getLogger().warning(ChatColor.RED + "Can't create /modules/" + moduleName +"/defaults folder!");
                        handler.getPlugin().getLogger().warning(ChatColor.RED + "Stopping loading...");
                        return;
                    }
                }

                Class<? extends Module> CastedModuleClass = ModuleClass.asSubclass(Module.class);
                Constructor<? extends Module> CastedModuleClassConstructor =
                        CastedModuleClass.getConstructor(
                                this.handler.getClass(), moduleName.getClass(),
                                apiVersion.getClass(), moduleVersion.getClass(),
                                moduleClasspath.getClass(), moduleInfo.getClass()
                        );

                Module module = (Module) CastedModuleClassConstructor.newInstance(
                        this.handler, moduleName,
                        apiVersion, moduleVersion,
                        moduleClasspath, moduleInfo);


                for(JarEntry jarInsideFile : Collections.list(moduleJar.entries())){

                    if(jarInsideFile.getName().equals(MODULE_YML_INFO)) continue;
                    if(!jarInsideFile.getName().startsWith(moduleName + "/")) continue;

                    Path output = Paths.get(entry.getValue().getParentFile().getAbsolutePath()
                            + File.separator + jarInsideFile.getName().replace("/", File.separator));

                    if(jarInsideFile.isDirectory()){
                        Files.createDirectories(output);
                        continue;
                    }

                    InputStream inputStream = moduleJar.getInputStream(jarInsideFile);
                    Files.copy(inputStream, output, StandardCopyOption.REPLACE_EXISTING);

                }

                module.onLoad();

                handler.loadedModules.put(moduleName, module);

                ModuleLoadEvent moduleLoadEvent = new ModuleLoadEvent(moduleName);
                handler.getPlugin().getServer().getPluginManager().callEvent(moduleLoadEvent);

                handler.getPlugin().getLogger().info(ChatColor.GREEN + "Module " + moduleName + " was successfully loaded!");

                handler.getPlugin().getLogger().info(ChatColor.GREEN + "------------------------------------------");
                handler.getPlugin().getLogger().info(ChatColor.GREEN + "               Module info:               ");
                handler.getPlugin().getLogger().info(ChatColor.GREEN + "------------------------------------------");
                handler.getPlugin().getLogger().info(ChatColor.GREEN + "Name:              " + moduleName);
                handler.getPlugin().getLogger().info(ChatColor.GREEN + "API version:       " + apiVersion);
                handler.getPlugin().getLogger().info(ChatColor.GREEN + "Version:           " + moduleVersion);
                handler.getPlugin().getLogger().info(ChatColor.GREEN + "Module Classpath:  " + moduleClasspath);

                if(!moduleInfo.isEmpty())
                    handler.getPlugin().getLogger().info(ChatColor.GREEN + "Module INFO:       " + moduleInfo);

                handler.getPlugin().getLogger().info(ChatColor.GREEN + "------------------------------------------");

                moduleJar.close();
            }

            catch (SecurityException exception){
                handler.getPlugin().getLogger().warning(ChatColor.RED + "Error during initialization of " + entry.getKey() + "!");
                handler.getPlugin().getLogger().warning(ChatColor.RED + "File system security error!");
                handler.getPlugin().getLogger().warning(ChatColor.RED + exception.getMessage());
                handler.getPlugin().getLogger().warning(ChatColor.RED + "Skipping module...");
            }

            catch (IOException exception){
                handler.getPlugin().getLogger().warning(ChatColor.RED + "Error during initialization of " + entry.getKey() + "!");
                handler.getPlugin().getLogger().warning(ChatColor.RED + "I/O error!");
                handler.getPlugin().getLogger().warning(ChatColor.RED + exception.getMessage());
                handler.getPlugin().getLogger().warning(ChatColor.RED + "Skipping module...");
            }

            catch (ClassNotFoundException exception) {
                handler.getPlugin().getLogger().warning(ChatColor.RED + "Error during initialization of " + entry.getKey() + "!");
                handler.getPlugin().getLogger().warning(ChatColor.RED + "Could not detect the proper Module class to load.");
                handler.getPlugin().getLogger().warning(ChatColor.RED + "Classpath: " + exception.getMessage());
                handler.getPlugin().getLogger().warning(ChatColor.RED + "Skipping module...");
            }

            catch (ModuleLoadException exception){
                handler.getPlugin().getLogger().warning(ChatColor.RED + "Error during initialization of " + entry.getKey() + "!");
                handler.getPlugin().getLogger().warning(ChatColor.RED + exception.getMessage());
                handler.getPlugin().getLogger().warning(ChatColor.RED + "Skipping module...");
            }

            catch (ReflectiveOperationException exception){
                handler.getPlugin().getLogger().warning(ChatColor.RED + "Error during initialization of " + entry.getKey() + "!");
                handler.getPlugin().getLogger().warning(ChatColor.RED + exception.getMessage());
                handler.getPlugin().getLogger().warning(ChatColor.RED + "Skipping module...");
            }

        }

        handler.getPlugin().getLogger().info(ChatColor.GREEN + "Module loading has been finished. " +
                handler.loadedModules.size() + " modules loaded!");


        /*
        try{
            this.close();
        } catch (IOException exception) {
            handler.getPlugin().getLogger().warning(ChatColor.RED + "Error closing all opened files!");
            handler.getPlugin().getLogger().warning(ChatColor.RED + exception.getMessage());
            exception.printStackTrace();
        }

        */

    }

}
