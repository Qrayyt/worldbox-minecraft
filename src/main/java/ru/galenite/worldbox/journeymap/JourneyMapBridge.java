package ru.galenite.worldbox.journeymap;

import ru.galenite.worldbox.WorldBoxMod;

/**
 * Soft integration point.
 *
 * This class deliberately avoids importing JourneyMap API, so the mod compiles and runs
 * even when JourneyMap is missing. Next step: add JourneyMap API as modCompileOnly and
 * register chunk polygons here.
 */
public final class JourneyMapBridge {
    private JourneyMapBridge() {}

    public static void tryInit() {
        WorldBoxMod.LOGGER.info("JourneyMap detected. Soft bridge enabled; overlay adapter is ready for API implementation.");
    }
}
