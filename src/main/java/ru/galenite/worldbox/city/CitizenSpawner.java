package ru.galenite.worldbox.city;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.village.VillagerData;
import net.minecraft.village.VillagerProfession;
import net.minecraft.village.VillagerType;

public final class CitizenSpawner {
    private CitizenSpawner() {}

    public static void ensurePopulation(ServerWorld world, City city) {
        long nearby = world.getEntitiesByClass(VillagerEntity.class, new Box(city.center).expand(96),
                e -> e.getCommandTags().contains("wb_city_" + city.id)).size();
        if (nearby >= city.targetCitizens()) return;

        boolean spawnChild = nearby >= city.population;
        VillagerEntity villager = EntityType.VILLAGER.create(world);
        if (villager == null) return;
        BlockPos spawn = safeSpawnPos(world, city.center.add(world.random.nextBetween(-5, 5), 0, world.random.nextBetween(-5, 5)));
        villager.refreshPositionAndAngles(spawn, world.random.nextFloat() * 360f, 0f);

        CitizenRole role = spawnChild ? CitizenRole.CHILD : pickRole(city, (int) nearby);
        villager.addCommandTag("wb_city_" + city.id);
        villager.addCommandTag("wb_role_" + role.name().toLowerCase());
        VillagerProfession profession = switch (role) {
            case FARMER -> VillagerProfession.FARMER;
            case GUARD, SOLDIER -> VillagerProfession.WEAPONSMITH;
            case MINER -> VillagerProfession.TOOLSMITH;
            case LUMBERJACK -> VillagerProfession.FLETCHER;
            case BUILDER -> VillagerProfession.MASON;
            default -> VillagerProfession.NONE;
        };
        villager.setVillagerData(new VillagerData(VillagerType.PLAINS, profession, 1));
        if (spawnChild) villager.setBreedingAge(-24000);

        var attr = villager.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED);
        if (attr != null) attr.setBaseValue(spawnChild ? 0.095D : 0.115D);
        world.spawnEntity(villager);
    }

    private static BlockPos safeSpawnPos(ServerWorld world, BlockPos rough) {
        int y = world.getTopY(net.minecraft.world.Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, rough.getX(), rough.getZ());
        return new BlockPos(rough.getX(), y, rough.getZ());
    }

    private static CitizenRole pickRole(City city, int index) {
        if (index == 0 || index == 1) return CitizenRole.BUILDER;
        if (index % 7 == 0) return CitizenRole.GUARD;
        if (index % 5 == 0) return CitizenRole.MINER;
        if (index % 3 == 0) return CitizenRole.FARMER;
        return CitizenRole.LUMBERJACK;
    }
}
