package ru.galenite.worldbox;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.galenite.worldbox.command.WorldBoxCommands;
import ru.galenite.worldbox.city.CityTicker;

public class WorldBoxMod implements ModInitializer {
    public static final String MOD_ID = "worldbox";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> WorldBoxCommands.register(dispatcher));
        ServerTickEvents.END_SERVER_TICK.register(CityTicker::tick);
        LOGGER.info("WorldBox Fabric initialized");
    }
}
