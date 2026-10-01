package pl.ktv.merchantry.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import pl.ktv.merchantry.Merchantry;
import pl.ktv.merchantry.Lang;
import pl.ktv.merchantry.economy.Cheque;
import pl.ktv.merchantry.economy.Economy;

// Komendy waluty: /balance, /pay, /cheque (/czek) oraz /eco dla admina.
// Działają tylko, gdy enableCurrency = true.
public final class MoneyCommands {
    private MoneyCommands() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        for (String name : new String[]{"balance", "bal"}) {
            dispatcher.register(Commands.literal(name)
                    .executes(context -> balance(context, context.getSource().getPlayerOrException()))
                    .then(Commands.argument("player", EntityArgument.player())
                            .requires(source -> source.hasPermission(2))
                            .executes(context -> balance(context, EntityArgument.getPlayer(context, "player")))));
        }

        dispatcher.register(Commands.literal("pay")
                .then(Commands.argument("player", EntityArgument.player())
                        .then(Commands.argument("amount", LongArgumentType.longArg(1))
                                .executes(MoneyCommands::pay))));

        for (String name : new String[]{"cheque", "czek"}) {
            dispatcher.register(Commands.literal(name)
                    .then(Commands.argument("amount", LongArgumentType.longArg(1))
                            .executes(MoneyCommands::cheque)));
        }

        // Włącza/wyłącza własny panel boczny gracza
        dispatcher.register(Commands.literal("sidebar")
                .executes(context -> {
                    ServerPlayer player = context.getSource().getPlayerOrException();
                    var data = pl.ktv.merchantry.data.ModAttachments.get(player);
                    data.sidebarHidden = !data.sidebarHidden;
                    pl.ktv.merchantry.economy.Sidebar.update(player);
                    player.sendSystemMessage(Lang.msg(data.sidebarHidden ? "sidebar.hidden" : "sidebar.shown"));
                    return 1;
                }));

        dispatcher.register(Commands.literal("eco")
                .requires(source -> source.hasPermission(2))
                .then(ecoAction("give"))
                .then(ecoAction("take"))
                .then(ecoAction("set")));
    }

    private static boolean checkEnabled(CommandSourceStack source) {
        if (!Economy.isEnabled()) {
            source.sendFailure(Lang.msg("currency.disabled"));
            return false;
        }
        return true;
    }

    private static int balance(CommandContext<CommandSourceStack> context, ServerPlayer target) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        if (!checkEnabled(source)) {
            return 0;
        }
        String balance = Economy.format(Economy.getBalance(target));
        if (target == source.getPlayer()) {
            source.sendSystemMessage(Lang.msg("balance.self", balance));
        } else {
            source.sendSystemMessage(Lang.msg("balance.other", target.getGameProfile().getName(), balance));
        }
        return 1;
    }

    private static int pay(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        if (!checkEnabled(source)) {
            return 0;
        }
        ServerPlayer sender = source.getPlayerOrException();
        ServerPlayer target = EntityArgument.getPlayer(context, "player");
        long amount = LongArgumentType.getLong(context, "amount");
        if (sender == target) {
            source.sendFailure(Lang.msg("pay.self"));
            return 0;
        }
        if (!Economy.withdraw(sender, amount)) {
            source.sendFailure(Lang.msg("money.not_enough", Economy.format(Economy.getBalance(sender))));
            return 0;
        }
        Economy.deposit(target, amount);
        sender.sendSystemMessage(Lang.msg("pay.sent", Economy.format(amount), target.getGameProfile().getName()));
        target.sendSystemMessage(Lang.msgFor(target, "pay.received",Economy.format(amount), sender.getGameProfile().getName()));
        Merchantry.LOGGER.info("{} przelał {} graczowi {}", sender.getGameProfile().getName(), amount,
                target.getGameProfile().getName());
        return 1;
    }

    private static int cheque(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        if (!checkEnabled(source)) {
            return 0;
        }
        ServerPlayer player = source.getPlayerOrException();
        long amount = LongArgumentType.getLong(context, "amount");
        if (!Economy.withdraw(player, amount)) {
            source.sendFailure(Lang.msg("money.not_enough", Economy.format(Economy.getBalance(player))));
            return 0;
        }
        ItemHandlerHelper.giveItemToPlayer(player, Cheque.create(player, amount));
        player.sendSystemMessage(Lang.msg("cheque.created", Economy.format(amount)));
        Merchantry.LOGGER.info("{} wystawił czek na {}", player.getGameProfile().getName(), amount);
        return 1;
    }

    private static LiteralArgumentBuilder<CommandSourceStack> ecoAction(String action) {
        return Commands.literal(action)
                .then(Commands.argument("player", EntityArgument.player())
                        .then(Commands.argument("amount", LongArgumentType.longArg(0))
                                .executes(context -> eco(context, action))));
    }

    private static int eco(CommandContext<CommandSourceStack> context, String action) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        if (!checkEnabled(source)) {
            return 0;
        }
        ServerPlayer target = EntityArgument.getPlayer(context, "player");
        long amount = LongArgumentType.getLong(context, "amount");
        String name = target.getGameProfile().getName();
        switch (action) {
            case "give" -> Economy.deposit(target, amount);
            case "take" -> Economy.setBalance(target, Economy.getBalance(target) - Math.min(amount, Economy.getBalance(target)));
            default -> Economy.setBalance(target, amount);
        }
        String balance = Economy.format(Economy.getBalance(target));
        source.sendSuccess(() -> Lang.msg("eco." + action, Economy.format(amount), name, balance), true);
        return 1;
    }
}
