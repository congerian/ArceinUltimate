package ru.arcein.plugins.arceinultimate.core.module;

import java.net.URLClassLoader;

public class ModulesLoader extends URLClassLoader {
    ModulesHandler handler;

    public ModulesLoader(ModulesHandler handler){
        super(((URLClassLoader)handler.plugin.getClass().getClassLoader()).getURLs(), handler.plugin.getClass().getClassLoader());
        this.handler = handler;
    }
}
