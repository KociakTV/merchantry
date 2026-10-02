package pl.ktv.merchantry.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import pl.ktv.merchantry.Config;
import pl.ktv.merchantry.Lang;
import pl.ktv.merchantry.data.Home;
import pl.ktv.merchantry.data.ModAttachments;
import pl.ktv.merchantry.data.PlayerData;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

// Domy: /sethome, /home, /delhome, /homes. Limit = domyślne + dokupione, maksymalnie maxHomes.
public final class HomeCommands {
    private static final String DEFAULT_NAME = "home";
    private static final Map<UUID, Long> LAST_TELEPORT = new HashMap<>();

    private static final SuggestionProvider<CommandSourceStack> HOMES = (context, builder) -> {
        ServerPlayer player = context.getSource().getPlayer();
        return player == null ? builder.buildFuture()
                : SharedSuggestionProvider.suggest(ModAttachments.get(player).homes.keySet(), builder);
    };

    private HomeCommands() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("sethome")
                .executes(context -> setHome(context.getSource().getPlayerOrException(), DEFAULT_NAME))
                .then(Commands.argument("name", StringArgumentType.word())
                        .executes(context -> setHome(context.getSource().getPlayerOrException(),
                                StringArgumentType.getString(context, "name")))));

        dispatcher.register(Commands.literal("home")
                .executes(context -> home(context.getSource().getPlayerOrException(), null))
                .then(Commands.argument("name", StringArgumentType.word()).suggests(HOMES)
                        .executes(context -> home(context.getSource().getPlayerOrException(),
                                StringArgumentType.getString(context, "name")))));

        dispatcher.register(Commands.literal("delhome")
                .then(Commands.argument("name", StringArgumentType.word()).suggests(HOMES)
                        .executes(context -> delHome(context.getSource().getPlayerOrException(),
                                StringArgumentType.getString(context, "name")))));

        dispatcher.register(Commands.literal("homes")
                .executes(context -> list(context.getSource().getPlayerOrException())));
    }

    public static int limit(ServerPlayer player) {
        return Math.min(Config.DEFAULT_HOMES.get() + ModAttachments.get(player).extraHomes, Config.MAX_HOMES.get());
    }

    public static void clear(UUID player) {
        LAST_TELEPORT.remove(player);
    }

    private static int setHome(ServerPlayer player, String rawName) {
        String name = rawName.toLowerCase(Locale.ROOT);
        PlayerData data = ModAttachments.get(player);
        if (!data.homes.containsKey(name) && data.homes.size() >= limit(player)) {
            player.sendSystemMessage(Lang.msg("home.limit", limit(player)));
            return 0;
        }
        data.homes.put(name, new Home(player.level().dimension(), player.getX(), player.getY(), player.getZ(),
                player.getYRot(), player.getXRot()));
        player.sendSystemMessage(Lang.msg("home.set", name));
        return 1;
    }

    private static int home(ServerPlayer player, String rawName) throws CommandSyntaxException {
        PlayerData data = ModAttachments.get(player);
        if (data.homes.isEmpty()) {
            player.sendSystemMessage(Lang.msg("home.none"));
            return 0;
        }
        String name;
        if (rawName != null) {
            name = rawName.toLowerCase(Locale.ROOT);
        } else if (data.homes.containsKey(DEFAULT_NAME)) {
            name = DEFAULT_NAME;
        } else if (data.homes.size() == 1) {
            name = data.homes.keySet().iterator().next();
        } else {
            player.sendSystemMessage(Lang.msg("home.choose", String.join(", ", data.homes.keySet())));
            return 0;
        }

        Home home = data.homes.get(name);
        if (home == null) {
            player.sendSystemMessage(Lang.msg("home.not_found", name));
            return 0;
        }

        long now = System.currentTimeMillis();
        long cooldownMs = Config.HOME_COOLDOWN_SECONDS.get() * 1000L;
        Long last = LAST_TELEPORT.get(player.getUUID());
        if (last != null && now - last < cooldownMs && !player.hasPermissions(2)) {
            player.sendSystemMessage(Lang.msg("home.cooldown", (cooldownMs - (now - last) + 999) / 1000));
            return 0;
        }

        ServerLevel level = player.server.getLevel(home.dimension());
        if (level == null) {
            player.sendSystemMessage(Lang.msg("home.world_missing"));
            return 0;
        }
        player.teleportTo(level, home.x(), home.y(), home.z(), home.yaw(), home.pitch());
        LAST_TELEPORT.put(player.getUUID(), now);
        player.sendSystemMessage(Lang.msg("home.teleported", name));
        return 1;
    }

    private static int delHome(ServerPlayer player, String rawName) {
        String name = rawName.toLowerCase(Locale.ROOT);
        if (ModAttachments.get(player).homes.remove(name) == null) {
            player.sendSystemMessage(Lang.msg("home.not_found", name));
            return 0;
        }
        player.sendSystemMessage(Lang.msg("home.deleted", name));
        return 1;
    }

    private static int list(ServerPlayer player) {
        PlayerData data = ModAttachments.get(player);
        List<String> names = List.copyOf(data.homes.keySet());
        player.sendSystemMessage(Lang.msg("home.list", names.size(), limit(player),
                names.isEmpty() ? "-" : String.join(", ", names)));
        return names.size();
    }
}
