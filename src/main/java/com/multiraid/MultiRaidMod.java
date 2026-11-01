package com.multiraid;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MultiRaidMod implements ModInitializer {
    public static final String MOD_ID = "multiraid";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        ServerTickEvents.END_SERVER_TICK.register(RaidManager::tick);
        ServerLifecycleEvents.SERVER_STOPPING.register(RaidManager::shutdown);
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> RaidManager.reset());
        LOGGER.info("MultiRaid geladen: mehrere gleichzeitige Raids sind aktiv.");
    }
}
