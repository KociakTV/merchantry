package pl.ktv.merchantry.command;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import pl.ktv.merchantry.shop.ShopMenu;

// /shop - otwiera okno sklepu
public final class ShopCommand {
    private ShopCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("shop")
                .executes(context -> {
                    ShopMenu.open(context.getSource().getPlayerOrException());
                    return 1;
                }));
    }
}
