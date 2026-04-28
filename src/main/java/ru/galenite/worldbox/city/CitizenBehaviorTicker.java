package ru.galenite.worldbox.city;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import ru.galenite.worldbox.building.BuildingPlanner;

public final class CitizenBehaviorTicker {
    private CitizenBehaviorTicker() {}

    public static void tick(ServerWorld world, City city) {
        for (VillagerEntity villager : world.getEntitiesByClass(VillagerEntity.class, new Box(city.center).expand(128),
                e -> e.getCommandTags().contains("wb_city_" + city.id))) {
            var speed = villager.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED);
            if (speed != null) speed.setBaseValue(0.5D);

            CitizenRole role = roleOf(villager);
            if (role == CitizenRole.BUILDER) BuildingPlanner.builderTick(world, city, villager);
            else if (role == CitizenRole.LUMBERJACK) gatherWood(world, city, villager);
            else if (role == CitizenRole.MINER) gatherStone(world, city, villager);
            else if (role == CitizenRole.FARMER) farmFood(world, city, villager);
            else if (role == CitizenRole.GUARD || role == CitizenRole.SOLDIER) patrol(world, city, villager);
        }
    }

    private static CitizenRole roleOf(VillagerEntity villager) {
        for (String tag : villager.getCommandTags()) {
            if (!tag.startsWith("wb_role_")) continue;
            try { return CitizenRole.valueOf(tag.substring("wb_role_".length()).toUpperCase()); }
            catch (IllegalArgumentException ignored) { return CitizenRole.IDLE; }
        }
        return CitizenRole.IDLE;
    }

    private static void gatherWood(ServerWorld world, City city, VillagerEntity v) {
        BlockPos target = findNear(world, v.getBlockPos(), 18, CitizenBehaviorTicker::isLog);
        if (target == null) {
            wanderNear(v, city.center, 0.55D);
            return;
        }
        v.getNavigation().startMovingTo(target.getX() + 0.5, target.getY(), target.getZ() + 0.5, 0.65D);
        if (v.squaredDistanceTo(target.getX() + 0.5, target.getY(), target.getZ() + 0.5) < 9) {
            world.setBlockState(target, Blocks.AIR.getDefaultState());
            city.wood += 4;
            world.playSound(null, target, SoundEvents.BLOCK_WOOD_BREAK, SoundCategory.BLOCKS, 0.7f, 1.0f);
        }
    }

    private static void gatherStone(ServerWorld world, City city, VillagerEntity v) {
        BlockPos target = findNear(world, v.getBlockPos().down(1), 12, s -> s.isOf(Blocks.STONE) || s.isOf(Blocks.COBBLESTONE));
        if (target == null) {
            wanderNear(v, city.center, 0.55D);
            return;
        }
        v.getNavigation().startMovingTo(target.getX() + 0.5, target.getY() + 1, target.getZ() + 0.5, 0.6D);
        if (v.squaredDistanceTo(target.getX() + 0.5, target.getY() + 1, target.getZ() + 0.5) < 10) {
            world.setBlockState(target, Blocks.COBBLESTONE.getDefaultState());
            city.stone += 2;
            world.playSound(null, target, SoundEvents.BLOCK_STONE_BREAK, SoundCategory.BLOCKS, 0.6f, 1.0f);
        }
    }

    private static void farmFood(ServerWorld world, City city, VillagerEntity v) {
        if (world.random.nextInt(3) == 0) city.food += 1;
        wanderNear(v, city.center, 0.5D);
    }

    private static void patrol(ServerWorld world, City city, VillagerEntity v) {
        if (world.random.nextInt(4) == 0) wanderNear(v, city.center, 0.62D);
    }

    private static void wanderNear(VillagerEntity v, BlockPos center, double speed) {
        if (!v.getNavigation().isIdle()) return;
        int x = center.getX() + v.getWorld().random.nextBetween(-18, 18);
        int z = center.getZ() + v.getWorld().random.nextBetween(-18, 18);
        int y = v.getWorld().getTopY(net.minecraft.world.Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, x, z);
        v.getNavigation().startMovingTo(x + 0.5, y, z + 0.5, speed);
    }

    private interface StateFilter { boolean test(BlockState state); }

    private static BlockPos findNear(ServerWorld world, BlockPos center, int radius, StateFilter filter) {
        for (int i = 0; i < 80; i++) {
            BlockPos p = center.add(world.random.nextBetween(-radius, radius), world.random.nextBetween(-3, 4), world.random.nextBetween(-radius, radius));
            if (filter.test(world.getBlockState(p))) return p;
        }
        return null;
    }

    private static boolean isLog(BlockState state) {
        return state.isOf(Blocks.OAK_LOG) || state.isOf(Blocks.BIRCH_LOG) || state.isOf(Blocks.SPRUCE_LOG) ||
                state.isOf(Blocks.JUNGLE_LOG) || state.isOf(Blocks.ACACIA_LOG) || state.isOf(Blocks.DARK_OAK_LOG) ||
                state.isOf(Blocks.MANGROVE_LOG) || state.isOf(Blocks.CHERRY_LOG);
    }
}
