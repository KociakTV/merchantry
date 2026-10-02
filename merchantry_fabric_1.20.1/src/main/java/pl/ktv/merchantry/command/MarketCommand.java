package pl.ktv.merchantry.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.LongArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import pl.ktv.merchantry.market.MarketMenu;
import pl.ktv.merchantry.market.MarketService;
import pl.ktv.merchantry.shop.ShopMenu;

// /market (/rynek) - rynek graczy; /market sell <cena> - wystawia przedmiot z ręki; /sell - skup serwera
public final class MarketCommand {
    private MarketCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        for (String name : new String[]{"market", "rynek"}) {
            dispatcher.register(Commands.literal(name)
                    .executes(context -> {
                        MarketMenu.open(context.getSource().getPlayerOrException(), MarketMenu.State.DEFAULT);
                        return 1;
                    })
                    // /market sell <cena> [ilość] - cena za całość; bez ilości wystawia wszystko z ręki
                    .then(Commands.literal("sell")
                            .then(Commands.argument("price", LongArgumentType.longArg(1))
                                    .executes(context -> {
                                        MarketService.createListing(context.getSource().getPlayerOrException(),
                                                LongArgumentType.getLong(context, "price"), 0);
                                        return 1;
                                    })
                                    .then(Commands.argument("amount", IntegerArgumentType.integer(1))
                                            .executes(context -> {
                                                MarketService.createListing(context.getSource().getPlayerOrException(),
                                                        LongArgumentType.getLong(context, "price"),
                                                        IntegerArgumentType.getInteger(context, "amount"));
                                                return 1;
                                            })))));
        }
        dispatcher.register(Commands.literal("sell")
                .executes(context -> {
                    ShopMenu.open(context.getSource().getPlayerOrException(), 0, ShopMenu.Tab.SELL);
                    return 1;
                }));
    }
}
