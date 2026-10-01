package pl.ktv.merchantry.command;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import pl.ktv.merchantry.exchange.Exchange;

// /kantor (/exchange) - wymiana $ <-> Create: Numismatics
public final class ExchangeCommand {
    private ExchangeCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        for (String name : new String[]{"kantor", "exchange"}) {
            dispatcher.register(Commands.literal(name)
                    .executes(context -> {
                        Exchange.open(context.getSource().getPlayerOrException(), false);
                        return 1;
                    }));
        }
    }
}
