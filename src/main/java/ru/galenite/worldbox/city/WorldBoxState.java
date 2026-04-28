package ru.galenite.worldbox.city;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.PersistentState;
import net.minecraft.world.PersistentStateManager;
import ru.galenite.worldbox.WorldBoxMod;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public class WorldBoxState extends PersistentState {
    private final Map<UUID, City> cities = new LinkedHashMap<>();

    public static WorldBoxState get(ServerWorld world) {
        PersistentStateManager manager = world.getPersistentStateManager();
        return manager.getOrCreate(WorldBoxState::fromNbt, WorldBoxState::new, WorldBoxMod.MOD_ID + "_cities");
    }

    public static WorldBoxState fromNbt(NbtCompound nbt) {
        WorldBoxState state = new WorldBoxState();
        NbtList list = nbt.getList("cities", 10);
        for (int i = 0; i < list.size(); i++) {
            City city = City.fromNbt(list.getCompound(i));
            state.cities.put(city.id, city);
        }
        return state;
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt) {
        NbtList list = new NbtList();
        for (City city : cities.values()) list.add(city.toNbt());
        nbt.put("cities", list);
        return nbt;
    }

    public Collection<City> cities() {
        return cities.values();
    }

    public City getByName(String name) {
        return cities.values().stream().filter(c -> c.name.equalsIgnoreCase(name)).findFirst().orElse(null);
    }

    public City getById(UUID id) {
        return cities.get(id);
    }

    public void add(City city) {
        cities.put(city.id, city);
        markDirty();
    }
}
