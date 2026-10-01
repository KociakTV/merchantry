package pl.ktv.merchantry.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;
import pl.ktv.merchantry.Lang;
import pl.ktv.merchantry.economy.Economy;
import pl.ktv.merchantry.shop.ConfirmMenu;
import pl.ktv.merchantry.shop.OfferType;
import pl.ktv.merchantry.shop.ShopManager;

// /repair i /repairall - otwierają okno potwierdzenia z wyliczoną ceną naprawy (tylko za walutę)
public final class RepairCommands {
    private RepairCommands() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("repair")
                .executes(context -> open(context.getSource(), OfferType.REPAIR)));
        dispatcher.register(Commands.literal("repairall")
                .executes(context -> open(context.getSource(), OfferType.REPAIR_ALL)));
    }

    private static int open(CommandSourceStack source, OfferType type) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        if (!Economy.isEnabled()) {
            source.sendFailure(Lang.msg("currency.disabled"));
            return 0;
        }
        ConfirmMenu.open(player, ShopManager.repairOffer(type), -1);
        return 1;
    }
}
