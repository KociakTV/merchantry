package pl.ktv.merchantry.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.server.level.ServerPlayer;
import pl.ktv.merchantry.Lang;
import pl.ktv.merchantry.data.ModAttachments;

// /keepinv - pokazuje liczbę ładunków; /keepinv give|set <gracz> <ilość> dla admina
public final class KeepInventoryCommand {
    private KeepInventoryCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("keepinv")
                .executes(context -> {
                    ServerPlayer player = context.getSource().getPlayerOrException();
                    player.sendSystemMessage(Lang.msg("keepinv.charges",
                            ModAttachments.get(player).keepInventoryCharges));
                    return ModAttachments.get(player).keepInventoryCharges;
                })
                .then(Commands.literal("give").requires(source -> source.hasPermission(2))
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.argument("amount", IntegerArgumentType.integer(1))
                                        .executes(context -> change(context.getSource(),
                                                EntityArgument.getPlayer(context, "player"),
                                                IntegerArgumentType.getInteger(context, "amount"), true)))))
                .then(Commands.literal("set").requires(source -> source.hasPermission(2))
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.argument("amount", IntegerArgumentType.integer(0))
                                        .executes(context -> change(context.getSource(),
                                                EntityArgument.getPlayer(context, "player"),
                                                IntegerArgumentType.getInteger(context, "amount"), false))))));
    }

    private static int change(CommandSourceStack source, ServerPlayer target, int amount, boolean add) {
        var data = ModAttachments.get(target);
        data.keepInventoryCharges = add ? data.keepInventoryCharges + amount : amount;
        int charges = data.keepInventoryCharges;
        source.sendSuccess(() -> Lang.msg("keepinv.admin", target.getGameProfile().getName(), charges), true);
        return charges;
    }
}
