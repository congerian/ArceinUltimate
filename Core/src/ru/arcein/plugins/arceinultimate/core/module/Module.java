package ru.arcein.plugins.arceinultimate.core.module;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

public class Module {

    protected ModulesHandler modulesHandler;

    private Path dataFolder;

    public final String moduleName;
    public final String apiVersion;
    public final String moduleVersion;
    public final String moduleClasspath;
    public final String moduleInfo;

    public Module(ModulesHandler modulesHandler, String moduleName,
                  String apiVersion, String moduleVersion,
                  String moduleClasspath, String moduleInfo){

        this.modulesHandler = modulesHandler;
        this.moduleInfo = moduleInfo;
        this.moduleName = moduleName;
        this.apiVersion = apiVersion;
        this.moduleVersion = moduleVersion;
        this.moduleClasspath = moduleClasspath;

        this.dataFolder = Paths.get(this.modulesHandler.getPlugin().getDataFolder().getAbsolutePath() +
                File.separator + "modules" + File.separator + moduleName);
    }

    public void onLoad() {
    }

    public void onDisable() {
    }

    public void onEnable() {
    }

    public Path getDataFolder(){
        return dataFolder;
    }

    public Path getDataDefaultFolder(){
        return Paths.get(dataFolder + File.separator + "defaults");
    }

    public Path getResource(String path){
        return Paths.get(dataFolder + File.separator + path);
    }

    public Path getDefaultResource(String path){
        return Paths.get(dataFolder + File.separator + "defaults" + File.separator + path);
    }

    public void copyDefaultResources(){

        List<Path> defaultFiles;

        try {
            Stream<Path> walk = Files.walk(getDataDefaultFolder());

            defaultFiles = walk//.filter(Files::isRegularFile)
                    .collect(Collectors.toList());

            for(Path path : defaultFiles){
                Path outFile = Paths.get(path.toString().replace(File.separator + "defaults", ""));
                if(Files.exists(outFile)) continue;
                Files.copy(path, outFile, REPLACE_EXISTING);
            }
        }
        catch (Exception exception){
            exception.printStackTrace();
            return;
        }

    }

    public void saveResource(){

    }

}
