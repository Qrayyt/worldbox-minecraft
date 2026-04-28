package ru.galenite.worldbox.city;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

public class City {
    public final UUID id;
    public String name;
    public String nation;
    public BlockPos center;
    public CityStage stage = CityStage.CAMP;
    public int population;
    public int children;
    public int food;
    public int wood;
    public int stone;
    public int buildProgress;
    public int completedBuildings;
    public int currentBuildPhase;
    public boolean hasActiveBuild;
    public int buildX;
    public int buildY;
    public int buildZ;
    public int deliveredWood;
    public int deliveredStone;
    public int nextPlotIndex;
    public int birthCooldown;
    public int warCooldown;
    public final Set<Long> claimedChunks = new LinkedHashSet<>();

    public City(UUID id, String name, String nation, BlockPos center) {
        this.id = id;
        this.name = name;
        this.nation = nation;
        this.center = center;
        this.population = 4;
        this.children = 0;
        this.food = 18;
        this.wood = 20;
        this.stone = 8;
        this.birthCooldown = 20 * 60 * 3;
        this.claimedChunks.add(new ChunkPos(center).toLong());
    }

    public int targetCitizens() {
        return population + children;
    }

    public BlockPos buildPos() {
        return new BlockPos(buildX, buildY, buildZ);
    }

    public static City fromNbt(NbtCompound nbt) {
        City city = new City(
                nbt.getUuid("id"),
                nbt.getString("name"),
                nbt.getString("nation"),
                new BlockPos(nbt.getInt("x"), nbt.getInt("y"), nbt.getInt("z"))
        );
        city.stage = CityStage.valueOf(nbt.getString("stage"));
        city.population = nbt.getInt("population");
        city.children = nbt.getInt("children");
        city.food = nbt.getInt("food");
        city.wood = nbt.getInt("wood");
        city.stone = nbt.getInt("stone");
        city.buildProgress = nbt.getInt("buildProgress");
        city.completedBuildings = nbt.getInt("completedBuildings");
        city.currentBuildPhase = nbt.getInt("currentBuildPhase");
        city.hasActiveBuild = nbt.getBoolean("hasActiveBuild");
        city.buildX = nbt.getInt("buildX");
        city.buildY = nbt.getInt("buildY");
        city.buildZ = nbt.getInt("buildZ");
        city.deliveredWood = nbt.getInt("deliveredWood");
        city.deliveredStone = nbt.getInt("deliveredStone");
        city.nextPlotIndex = nbt.getInt("nextPlotIndex");
        city.birthCooldown = nbt.getInt("birthCooldown");
        city.warCooldown = nbt.getInt("warCooldown");
        city.claimedChunks.clear();
        NbtList chunks = nbt.getList("chunks", 8);
        for (int i = 0; i < chunks.size(); i++) {
            city.claimedChunks.add(Long.parseLong(chunks.getString(i)));
        }
        if (city.claimedChunks.isEmpty()) city.claimedChunks.add(new ChunkPos(city.center).toLong());
        return city;
    }

    public NbtCompound toNbt() {
        NbtCompound nbt = new NbtCompound();
        nbt.putUuid("id", id);
        nbt.putString("name", name);
        nbt.putString("nation", nation);
        nbt.putInt("x", center.getX());
        nbt.putInt("y", center.getY());
        nbt.putInt("z", center.getZ());
        nbt.putString("stage", stage.name());
        nbt.putInt("population", population);
        nbt.putInt("children", children);
        nbt.putInt("food", food);
        nbt.putInt("wood", wood);
        nbt.putInt("stone", stone);
        nbt.putInt("buildProgress", buildProgress);
        nbt.putInt("completedBuildings", completedBuildings);
        nbt.putInt("currentBuildPhase", currentBuildPhase);
        nbt.putBoolean("hasActiveBuild", hasActiveBuild);
        nbt.putInt("buildX", buildX);
        nbt.putInt("buildY", buildY);
        nbt.putInt("buildZ", buildZ);
        nbt.putInt("deliveredWood", deliveredWood);
        nbt.putInt("deliveredStone", deliveredStone);
        nbt.putInt("nextPlotIndex", nextPlotIndex);
        nbt.putInt("birthCooldown", birthCooldown);
        nbt.putInt("warCooldown", warCooldown);
        NbtList chunks = new NbtList();
        for (Long chunk : claimedChunks) chunks.add(NbtString.of(Long.toString(chunk)));
        nbt.put("chunks", chunks);
        return nbt;
    }
}
