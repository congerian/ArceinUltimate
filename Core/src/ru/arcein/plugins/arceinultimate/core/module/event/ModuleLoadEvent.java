package ru.arcein.plugins.arceinultimate.core.module.event;

import org.bukkit.event.HandlerList;

public class ModuleLoadEvent extends ModuleEvent{

    private String message;
    private static final HandlerList handlerList = new HandlerList();

    public ModuleLoadEvent(String message){
        super(message);
        this.message = message;
    }

    @Override
    public HandlerList getHandlers() {
        return handlerList;
    }

    public static HandlerList getHandlerList() {
        return handlerList;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

}
