package pl.ktv.merchantry.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;
import pl.ktv.merchantry.Lang;
import pl.ktv.merchantry.unlock.Unlock;

// Komendy odblokowywane na zawsze: /craft, /anvil, /enderchest (/ec), /stonecutter...
public final class UnlockCommands {
    private UnlockCommands() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        for (Unlock unlock : Unlock.values()) {
            dispatcher.register(Commands.literal(unlock.id()).executes(context -> open(context, unlock)));
        }
        dispatcher.register(Commands.literal("ec").executes(context -> open(context, Unlock.ENDERCHEST)));
    }

    private static int open(CommandContext<CommandSourceStack> context, Unlock unlock) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        if (!unlock.has(player)) {
            player.sendSystemMessage(Lang.msg("unlock.locked"));
            return 0;
        }
        unlock.open(player);
        return 1;
    }
}
