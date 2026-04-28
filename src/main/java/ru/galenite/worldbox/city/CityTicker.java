package ru.galenite.worldbox.city;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.ChunkPos;
import ru.galenite.worldbox.building.BuildingPlanner;
import ru.galenite.worldbox.war.WarManager;

public final class CityTicker {
    private static int ticks;
    private CityTicker() {}

    public static void tick(MinecraftServer server) {
        ticks++;
        if (ticks % 20 != 0) return;
        for (ServerWorld world : server.getWorlds()) {
            WorldBoxState state = WorldBoxState.get(world);
            boolean dirty = false;
            for (City city : state.cities()) {
                if (city.warCooldown > 0) city.warCooldown -= 20;
                if (city.birthCooldown > 0) city.birthCooldown -= 20;
                if (ticks % 100 == 0) {
                    CitizenBehaviorTicker.tick(world, city);
                    WarManager.tickArmies(world, city);
                }
                if (ticks % 200 == 0) {
                    // Ресурсы больше не появляются из воздуха: их добывают NPC в CitizenBehaviorTicker.
                    maybeFamilyGrowth(city);
                    CitizenSpawner.ensurePopulation(world, city);
                    expandClaims(city);
                    dirty = true;
                }
                BuildingPlanner.tickPlanning(world, city);
                dirty = true;
            }
            if (dirty) state.markDirty();
        }
    }

    private static void maybeFamilyGrowth(City city) {
        if (city.children > 0 && city.food > 35 && city.buildProgress % 1200 == 0) {
            city.children -= 1;
            city.population += 1;
            city.food -= 12;
        }
        int childLimit = Math.max(1, city.population / 5);
        if (city.birthCooldown <= 0 && city.population >= 4 && city.children < childLimit && city.food > 55) {
            city.food -= 28;
            city.children += 1;
            city.birthCooldown = 20 * 60 * 6;
        }
    }

    private static void expandClaims(City city) {
        ChunkPos center = new ChunkPos(city.center);
        int radius = Math.min(4, 1 + city.completedBuildings / 5);
        for (int x = -radius; x <= radius; x++) for (int z = -radius; z <= radius; z++) {
            if (Math.abs(x) + Math.abs(z) <= radius + 1) city.claimedChunks.add(new ChunkPos(center.x + x, center.z + z).toLong());
        }
    }
}
