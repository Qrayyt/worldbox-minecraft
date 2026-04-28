package ru.galenite.worldbox.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import ru.galenite.worldbox.city.City;
import ru.galenite.worldbox.city.WorldBoxState;
import ru.galenite.worldbox.war.WarManager;

import java.util.UUID;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public final class WorldBoxCommands {
    private WorldBoxCommands() {}

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(literal("worldbox")
                .requires(src -> src.hasPermissionLevel(2))
                .then(literal("create")
                        .then(argument("city", StringArgumentType.word())
                                .executes(ctx -> create(ctx.getSource(), StringArgumentType.getString(ctx, "city"), "neutral"))
                                .then(argument("nation", StringArgumentType.word())
                                        .executes(ctx -> create(ctx.getSource(), StringArgumentType.getString(ctx, "city"), StringArgumentType.getString(ctx, "nation"))))))
                .then(literal("info")
                        .then(argument("city", StringArgumentType.word())
                                .executes(ctx -> info(ctx.getSource(), StringArgumentType.getString(ctx, "city")))))
                .then(literal("list")
                        .executes(ctx -> list(ctx.getSource())))
                .then(literal("war")
                        .then(argument("attacker", StringArgumentType.word())
                                .then(argument("defender", StringArgumentType.word())
                                        .executes(ctx -> war(ctx.getSource(), StringArgumentType.getString(ctx, "attacker"), StringArgumentType.getString(ctx, "defender"))))))
        );
    }

    private static int create(ServerCommandSource source, String name, String nation) {
        BlockPos pos = BlockPos.ofFloored(source.getPosition());
        WorldBoxState state = WorldBoxState.get(source.getWorld());
        City city = new City(UUID.randomUUID(), name, nation, pos);
        state.add(city);
        source.sendFeedback(() -> Text.literal("Создан город " + name + " / нация: " + nation), true);
        return 1;
    }

    private static int info(ServerCommandSource source, String name) {
        City city = WorldBoxState.get(source.getWorld()).getByName(name);
        if (city == null) {
            source.sendError(Text.literal("Город не найден"));
            return 0;
        }
        source.sendFeedback(() -> Text.literal(city.name + " | adults: " + city.population + " | children: " + city.children + " | chunks: " + city.claimedChunks.size() + " | stage: " + city.stage + " | nation: " + city.nation + " | food/wood/stone: " + city.food + "/" + city.wood + "/" + city.stone + " | build: " + (city.hasActiveBuild ? city.currentBuildPhase + " @ " + city.buildX + "," + city.buildY + "," + city.buildZ + " delivered " + city.deliveredWood + "w/" + city.deliveredStone + "s" : "none")), false);
        return 1;
    }

    private static int list(ServerCommandSource source) {
        StringBuilder out = new StringBuilder("WorldBox cities: ");
        for (City city : WorldBoxState.get(source.getWorld()).cities()) {
            out.append(city.name).append("(").append(city.population).append(") ");
        }
        source.sendFeedback(() -> Text.literal(out.toString()), false);
        return 1;
    }

    private static int war(ServerCommandSource source, String attackerName, String defenderName) {
        WorldBoxState state = WorldBoxState.get(source.getWorld());
        City attacker = state.getByName(attackerName);
        City defender = state.getByName(defenderName);
        if (attacker == null || defender == null) {
            source.sendError(Text.literal("Один из городов не найден"));
            return 0;
        }
        WarManager.declareWar(source.getWorld(), attacker, defender);
        state.markDirty();
        source.sendFeedback(() -> Text.literal(attacker.name + " объявил войну " + defender.name), true);
        return 1;
    }
}
