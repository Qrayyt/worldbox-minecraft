package ru.galenite.worldbox.building;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.Heightmap;
import ru.galenite.worldbox.city.City;
import ru.galenite.worldbox.city.CityStage;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class BuildingPlanner {
    private BuildingPlanner() {}

    public static void tickPlanning(ServerWorld world, City city) {
        if (!city.hasActiveBuild) startNextBuild(world, city);
    }

    public static void builderTick(ServerWorld world, City city, VillagerEntity builder) {
        tickPlanning(world, city);
        if (!city.hasActiveBuild) return;

        BlockPos build = city.buildPos();
        builder.getNavigation().startMovingTo(build.getX() + 3.5, build.getY(), build.getZ() + 3.5, 0.65D);
        if (builder.squaredDistanceTo(build.getX() + 3.5, build.getY(), build.getZ() + 3.5) > 49) return;

        int needWood = phaseWoodNeed(city.currentBuildPhase);
        int needStone = phaseStoneNeed(city.currentBuildPhase);
        if (city.deliveredWood < needWood && city.wood > 0) {
            city.wood--;
            city.deliveredWood++;
            return;
        }
        if (city.deliveredStone < needStone && city.stone > 0) {
            city.stone--;
            city.deliveredStone++;
            return;
        }
        if (city.deliveredWood < needWood || city.deliveredStone < needStone) return;

        placeHousePhase(world, build, city.currentBuildPhase);
        city.deliveredWood = 0;
        city.deliveredStone = 0;
        city.currentBuildPhase++;
        city.buildProgress++;
        world.playSound(null, build, SoundEvents.BLOCK_WOOD_PLACE, SoundCategory.BLOCKS, 0.8f, 0.9f);

        if (city.currentBuildPhase > 5) {
            city.currentBuildPhase = 0;
            city.completedBuildings++;
            city.nextPlotIndex++;
            city.hasActiveBuild = false;
            city.food += 2;
            updateStage(city);
            world.playSound(null, build, SoundEvents.ENTITY_VILLAGER_CELEBRATE, SoundCategory.NEUTRAL, 1.0f, 1.0f);
        }
    }

    private static int phaseWoodNeed(int phase) {
        return switch (phase) {
            case 0 -> 10;
            case 1 -> 12;
            case 2 -> 10;
            case 3 -> 14;
            case 4 -> 6;
            default -> 4;
        };
    }

    private static int phaseStoneNeed(int phase) {
        return phase == 0 ? 4 : 0;
    }

    private static void startNextBuild(ServerWorld world, City city) {
        BlockPos origin = getPlannedPlot(world, city, city.nextPlotIndex);
        if (origin == null) return;
        city.buildX = origin.getX();
        city.buildY = origin.getY();
        city.buildZ = origin.getZ();
        city.hasActiveBuild = true;
        city.currentBuildPhase = 0;
        city.deliveredWood = 0;
        city.deliveredStone = 0;
        placeRoadToPlot(world, city.center, origin);
    }

    private static void updateStage(City city) {
        if (city.completedBuildings >= 2 && city.stage == CityStage.CAMP) city.stage = CityStage.VILLAGE;
        if (city.completedBuildings >= 6 && city.stage == CityStage.VILLAGE) city.stage = CityStage.TOWN;
        if (city.completedBuildings >= 14 && city.stage == CityStage.TOWN) city.stage = CityStage.CITY;
    }

    private static BlockPos getPlannedPlot(ServerWorld world, City city, int index) {
        List<BlockPos> candidates = new ArrayList<>();
        int ring = 1 + index / 8;
        int radius = 13 + ring * 9;
        for (int x = -radius; x <= radius; x += 9) {
            for (int z = -radius; z <= radius; z += 9) {
                if (Math.abs(x) < 8 && Math.abs(z) < 8) continue;
                if (Math.abs(x) != radius && Math.abs(z) != radius) continue;
                BlockPos rough = city.center.add(x, 0, z);
                BlockPos surface = surface(world, rough);
                if (surface != null && isGoodPlot(world, surface)) candidates.add(surface);
            }
        }
        if (candidates.isEmpty()) return null;
        candidates.sort(Comparator.comparingDouble(p -> p.getSquaredDistance(city.center)));
        return candidates.get(Math.floorMod(index, candidates.size()));
    }

    private static BlockPos surface(ServerWorld world, BlockPos rough) {
        int y = world.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, rough.getX(), rough.getZ());
        if (y <= world.getBottomY() + 2) return null;
        return new BlockPos(rough.getX(), y, rough.getZ());
    }

    private static boolean isGoodPlot(ServerWorld world, BlockPos base) {
        int minY = base.getY();
        int maxY = base.getY();
        int solid = 0;
        for (int x = -2; x < 9; x++) for (int z = -2; z < 9; z++) {
            BlockPos p = surface(world, base.add(x, 0, z));
            if (p == null) return false;
            minY = Math.min(minY, p.getY());
            maxY = Math.max(maxY, p.getY());
            BlockState ground = world.getBlockState(p.down());
            BlockState top = world.getBlockState(p);
            if (ground.isOf(Blocks.WATER) || ground.isOf(Blocks.LAVA) || isTree(top) || isTree(ground)) return false;
            if (ground.isSolidBlock(world, p.down())) solid++;
        }
        return solid > 90 && maxY - minY <= 2;
    }

    private static void placeRoadToPlot(ServerWorld world, BlockPos cityCenter, BlockPos plot) {
        BlockPos start = surface(world, cityCenter);
        BlockPos end = surface(world, plot.add(3, 0, -2));
        if (start == null || end == null) return;
        int x = start.getX();
        int z = start.getZ();
        while (x != end.getX()) { x += Integer.compare(end.getX(), x); pave(world, new BlockPos(x, 0, z)); }
        while (z != end.getZ()) { z += Integer.compare(end.getZ(), z); pave(world, new BlockPos(x, 0, z)); }
    }

    private static void pave(ServerWorld world, BlockPos rough) {
        BlockPos s = surface(world, rough);
        if (s == null) return;
        BlockPos ground = s.down();
        BlockState above = world.getBlockState(s);
        if (isTree(above) || isTree(world.getBlockState(ground)) || !world.getBlockState(ground).isSolidBlock(world, ground)) return;
        Block groundBlock = world.getBlockState(ground).getBlock();
        if (groundBlock == Blocks.OAK_PLANKS || groundBlock == Blocks.COBBLESTONE || groundBlock == Blocks.OAK_LOG || groundBlock == Blocks.STRIPPED_OAK_LOG) return;
        world.setBlockState(ground, Blocks.DIRT_PATH.getDefaultState());
        if (!above.isAir() && !above.isSolidBlock(world, s)) world.setBlockState(s, Blocks.AIR.getDefaultState());
    }

    private static boolean isTree(BlockState state) {
        return state.isOf(Blocks.OAK_LEAVES) || state.isOf(Blocks.BIRCH_LEAVES) || state.isOf(Blocks.SPRUCE_LEAVES) ||
                state.isOf(Blocks.JUNGLE_LEAVES) || state.isOf(Blocks.ACACIA_LEAVES) || state.isOf(Blocks.DARK_OAK_LEAVES) ||
                state.isOf(Blocks.MANGROVE_LEAVES) || state.isOf(Blocks.CHERRY_LEAVES) ||
                state.isOf(Blocks.OAK_LOG) || state.isOf(Blocks.BIRCH_LOG) || state.isOf(Blocks.SPRUCE_LOG) ||
                state.isOf(Blocks.JUNGLE_LOG) || state.isOf(Blocks.ACACIA_LOG) || state.isOf(Blocks.DARK_OAK_LOG) ||
                state.isOf(Blocks.MANGROVE_LOG) || state.isOf(Blocks.CHERRY_LOG);
    }

    private static void placeHousePhase(ServerWorld world, BlockPos base, int phase) {
        if (phase == 0) {
            clearPlot(world, base);
            flattenFoundation(world, base);
            for (int x = 0; x < 7; x++) for (int z = 0; z < 7; z++) world.setBlockState(base.add(x, 0, z), Blocks.COBBLESTONE.getDefaultState());
        } else if (phase == 1) {
            for (int x = 0; x < 7; x++) for (int z = 0; z < 7; z++) if (x == 0 || x == 6 || z == 0 || z == 6) world.setBlockState(base.add(x, 1, z), Blocks.STRIPPED_OAK_LOG.getDefaultState());
            world.setBlockState(base.add(3, 1, 0), Blocks.AIR.getDefaultState());
        } else if (phase == 2) {
            for (int x = 0; x < 7; x++) for (int z = 0; z < 7; z++) if (x == 0 || x == 6 || z == 0 || z == 6) world.setBlockState(base.add(x, 2, z), Blocks.OAK_PLANKS.getDefaultState());
            world.setBlockState(base.add(1, 2, 0), Blocks.GLASS_PANE.getDefaultState());
            world.setBlockState(base.add(5, 2, 0), Blocks.GLASS_PANE.getDefaultState());
        } else if (phase == 3) {
            for (int x = -1; x < 8; x++) for (int z = -1; z < 8; z++) {
                Direction facing = z < 3 ? Direction.NORTH : Direction.SOUTH;
                world.setBlockState(base.add(x, 3, z), Blocks.OAK_STAIRS.getDefaultState().with(Properties.HORIZONTAL_FACING, facing));
            }
        } else if (phase == 4) {
            world.setBlockState(base.add(3, 1, 0), Blocks.OAK_DOOR.getDefaultState().with(Properties.HORIZONTAL_FACING, Direction.NORTH));
            world.setBlockState(base.add(3, 1, 3), Blocks.CHEST.getDefaultState());
            world.setBlockState(base.add(2, 1, 3), Blocks.CRAFTING_TABLE.getDefaultState());
            world.setBlockState(base.add(4, 1, 3), Blocks.BARREL.getDefaultState());
        } else {
            world.setBlockState(base.add(3, 1, 4), Blocks.YELLOW_BED.getDefaultState());
        }
    }

    private static void flattenFoundation(ServerWorld world, BlockPos base) {
        for (int x = -1; x < 8; x++) for (int z = -1; z < 8; z++) {
            BlockPos column = base.add(x, 0, z);
            for (int y = -3; y <= 0; y++) {
                BlockPos p = column.add(0, y, 0);
                if (world.getBlockState(p).isAir()) world.setBlockState(p, Blocks.DIRT.getDefaultState());
            }
        }
    }

    private static void clearPlot(ServerWorld world, BlockPos base) {
        for (int x = -2; x < 9; x++) for (int z = -2; z < 9; z++) for (int y = 1; y < 7; y++) {
            BlockState state = world.getBlockState(base.add(x, y, z));
            if (!isTree(state)) world.setBlockState(base.add(x, y, z), Blocks.AIR.getDefaultState());
        }
    }
}
