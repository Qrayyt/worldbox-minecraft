package ru.galenite.worldbox.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import ru.galenite.worldbox.WorldBoxMod;
import ru.galenite.worldbox.journeymap.JourneyMapBridge;

public class WorldBoxClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        if (FabricLoader.getInstance().isModLoaded("journeymap")) {
            JourneyMapBridge.tryInit();
        } else {
            WorldBoxMod.LOGGER.info("JourneyMap not found: WorldBox runs without map overlays.");
        }
    }
}
