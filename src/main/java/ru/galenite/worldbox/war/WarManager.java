package ru.galenite.worldbox.war;

import net.minecraft.block.Blocks;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.PillagerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import ru.galenite.worldbox.city.City;
import ru.galenite.worldbox.city.WorldBoxState;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class WarManager {
    private static final Map<UUID, Integer> SIEGE_PROGRESS = new HashMap<>();

    private WarManager() {}

    public static void declareWar(ServerWorld world, City attacker, City defender) {
        if (attacker.id.equals(defender.id)) return;
        attacker.warCooldown = 20 * 60 * 5;
        defender.warCooldown = 20 * 60 * 5;
        spawnArmy(world, attacker, defender);
    }

    public static void tickArmies(ServerWorld world, City city) {
        for (PillagerEntity pillager : world.getEntitiesByClass(PillagerEntity.class, new Box(city.center).expand(220), WarManager::hasArmyTag)) {
            BlockPos target = readTarget(pillager);
            UUID attackerId = readAttacker(pillager);
            UUID defenderId = readDefender(pillager);
            if (target == null || attackerId == null || defenderId == null) continue;

            var speed = pillager.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED);
            if (speed != null) speed.setBaseValue(0.35D);
            pillager.getNavigation().startMovingTo(target.getX() + 0.5D, target.getY(), target.getZ() + 0.5D, 1.0D);

            if (pillager.getBlockPos().getSquaredDistance(target) < 28 * 28) {
                if (world.random.nextInt(12) == 0) setSiegeFire(world, target, 1);
                applySiegeTick(world, attackerId, defenderId, target);
            }
        }
    }

    private static void applySiegeTick(ServerWorld world, UUID attackerId, UUID defenderId, BlockPos target) {
        WorldBoxState state = WorldBoxState.get(world);
        City attacker = state.getById(attackerId);
        City defender = state.getById(defenderId);
        if (attacker == null || defender == null) return;
        if (attacker.nation.equals(defender.nation)) return;

        int nearSoldiers = world.getEntitiesByClass(PillagerEntity.class, new Box(defender.center).expand(32), e ->
                hasArmyTag(e) && e.getCommandTags().contains("wb_attacker_" + attacker.id) && e.getCommandTags().contains("wb_defender_" + defender.id)
        ).size();

        if (nearSoldiers < 2) return;
        int progress = SIEGE_PROGRESS.getOrDefault(defender.id, 0) + nearSoldiers;
        SIEGE_PROGRESS.put(defender.id, progress);

        // Захват: город переходит в нацию победителя, чанки объединяются, часть населения теряется.
        if (progress >= Math.max(80, defender.population * 20)) {
            defender.nation = attacker.nation;
            attacker.claimedChunks.addAll(defender.claimedChunks);
            defender.population = Math.max(2, defender.population - Math.max(1, nearSoldiers / 2));
            attacker.food += Math.max(8, defender.food / 3);
            attacker.wood += Math.max(6, defender.wood / 3);
            attacker.stone += Math.max(3, defender.stone / 3);
            defender.food = Math.max(10, defender.food / 2);
            defender.wood = Math.max(6, defender.wood / 2);
            defender.stone = Math.max(3, defender.stone / 2);
            SIEGE_PROGRESS.remove(defender.id);
            world.playSound(null, defender.center, SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundCategory.HOSTILE, 2.0f, 0.8f);
            cleanupArmy(world, attacker.id, defender.id);
            state.markDirty();
        } else if (world.random.nextInt(20) == 0) {
            world.playSound(null, target, SoundEvents.ENTITY_PILLAGER_CELEBRATE, SoundCategory.HOSTILE, 1.0f, 0.9f);
        }
    }

    private static void cleanupArmy(ServerWorld world, UUID attacker, UUID defender) {
        for (PillagerEntity pillager : world.getEntitiesByClass(PillagerEntity.class, new Box(BlockPos.ORIGIN).expand(30000000), e ->
                e.getCommandTags().contains("wb_attacker_" + attacker) && e.getCommandTags().contains("wb_defender_" + defender))) {
            pillager.discard();
        }
    }

    private static void spawnArmy(ServerWorld world, City attacker, City defender) {
        int soldiers = Math.max(4, Math.min(22, attacker.population / 2 + attacker.completedBuildings / 2));
        for (int i = 0; i < soldiers; i++) {
            PillagerEntity pillager = EntityType.PILLAGER.create(world);
            if (pillager == null) continue;
            BlockPos pos = safePos(world, attacker.center.add(world.random.nextBetween(-8, 8), 0, world.random.nextBetween(-8, 8)));
            pillager.refreshPositionAndAngles(pos, 0, 0);
            pillager.addCommandTag("wb_army");
            pillager.addCommandTag("wb_attacker_" + attacker.id);
            pillager.addCommandTag("wb_defender_" + defender.id);
            pillager.addCommandTag("wb_target_" + defender.center.getX() + "_" + defender.center.getY() + "_" + defender.center.getZ());
            var speed = pillager.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED);
            if (speed != null) speed.setBaseValue(0.35D);
            world.spawnEntity(pillager);
            pillager.getNavigation().startMovingTo(defender.center.getX() + 0.5D, defender.center.getY(), defender.center.getZ() + 0.5D, 1.0D);
        }
        world.playSound(null, attacker.center, SoundEvents.EVENT_RAID_HORN.value(), SoundCategory.HOSTILE, 3.0f, 1.0f);
    }

    private static boolean hasArmyTag(PillagerEntity e) {
        return e.getCommandTags().contains("wb_army");
    }

    private static UUID readAttacker(PillagerEntity e) {
        for (String tag : e.getCommandTags()) if (tag.startsWith("wb_attacker_")) return parseUuid(tag.substring("wb_attacker_".length()));
        return null;
    }

    private static UUID readDefender(PillagerEntity e) {
        for (String tag : e.getCommandTags()) if (tag.startsWith("wb_defender_")) return parseUuid(tag.substring("wb_defender_".length()));
        return null;
    }

    private static UUID parseUuid(String raw) {
        try { return UUID.fromString(raw); } catch (IllegalArgumentException ignored) { return null; }
    }

    private static BlockPos readTarget(PillagerEntity e) {
        for (String tag : e.getCommandTags()) {
            if (!tag.startsWith("wb_target_")) continue;
            String[] parts = tag.substring("wb_target_".length()).split("_");
            if (parts.length != 3) return null;
            try {
                return new BlockPos(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]), Integer.parseInt(parts[2]));
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private static void setSiegeFire(ServerWorld world, BlockPos center, int amount) {
        for (int i = 0; i < amount; i++) {
            BlockPos p = center.add(world.random.nextBetween(-14, 14), 0, world.random.nextBetween(-14, 14));
            int y = world.getTopY(net.minecraft.world.Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, p.getX(), p.getZ());
            BlockPos fire = new BlockPos(p.getX(), y, p.getZ());
            if (world.getBlockState(fire.down()).isSolidBlock(world, fire.down()) && world.getBlockState(fire).isAir()) {
                world.setBlockState(fire, Blocks.FIRE.getDefaultState());
            }
        }
    }

    private static BlockPos safePos(ServerWorld world, BlockPos rough) {
        int y = world.getTopY(net.minecraft.world.Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, rough.getX(), rough.getZ());
        return new BlockPos(rough.getX(), y, rough.getZ());
    }
}
