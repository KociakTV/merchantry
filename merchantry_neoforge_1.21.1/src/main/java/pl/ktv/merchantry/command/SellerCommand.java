package pl.ktv.merchantry.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import pl.ktv.merchantry.Lang;
import pl.ktv.merchantry.economy.Economy;
import pl.ktv.merchantry.seller.SellerData;
import pl.ktv.merchantry.seller.SellerService;

import java.util.UUID;

// /seller - podsumowanie skrzynek sprzedających gracza; /seller collect - odbiór dojrzałego zarobku czekiem.
// Dla admina: /seller give <gracz> oraz /seller admin add|remove|energy <pozycja> (przydatne też do testów).
public final class SellerCommand {
    private SellerCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("seller")
                .executes(context -> {
                    SellerService.sendStatus(context.getSource().getPlayerOrException());
                    return 1;
                })
                .then(Commands.literal("collect")
                        .executes(context -> collect(context, 0))
                        .then(Commands.argument("amount", LongArgumentType.longArg(1))
                                .executes(context -> collect(context, LongArgumentType.getLong(context, "amount")))))
                .then(Commands.literal("give").requires(source -> source.hasPermission(2))
                        .then(Commands.argument("player", EntityArgument.player()).executes(context -> {
                            ServerPlayer target = EntityArgument.getPlayer(context, "player");
                            // Tekst przedmiotu w języku odbiorcy, a nie admina, który wpisał komendę
                            ItemHandlerHelper.giveItemToPlayer(target, Lang.with(target, SellerService::createItem));
                            context.getSource().sendSuccess(() -> Lang.msg("seller.give",
                                    target.getGameProfile().getName()), true);
                            return 1;
                        })))
                .then(Commands.literal("admin").requires(source -> source.hasPermission(2))
                        .then(Commands.literal("add")
                                .then(Commands.argument("pos", BlockPosArgument.blockPos())
                                        .then(Commands.argument("owner", EntityArgument.player())
                                                .executes(SellerCommand::adminAdd))))
                        .then(Commands.literal("list").executes(SellerCommand::adminList))
                        .then(Commands.literal("remove")
                                .then(Commands.argument("pos", BlockPosArgument.blockPos())
                                        .executes(SellerCommand::adminRemove)))
                        .then(Commands.literal("reset")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(context -> reset(context, true, true))
                                        .then(Commands.literal("limit").executes(context -> reset(context, true, false)))
                                        .then(Commands.literal("payout").executes(context -> reset(context, false, true)))
                                        .then(Commands.literal("all").executes(context -> reset(context, true, true)))))
                        .then(Commands.literal("extra")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .then(Commands.argument("amount", IntegerArgumentType.integer(1, 1000))
                                                .executes(SellerCommand::extra))))
                        .then(Commands.literal("energy")
                                .then(Commands.argument("pos", BlockPosArgument.blockPos())
                                        .then(Commands.argument("amount", IntegerArgumentType.integer(0))
                                                .executes(SellerCommand::adminEnergy))))));
    }

    // /seller collect [kwota] - czek na całość albo na podaną kwotę; reszta zostaje w rozliczeniu
    private static int collect(CommandContext<CommandSourceStack> context, long requested) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        if (!Economy.isEnabled()) {
            player.sendSystemMessage(Lang.msg("currency.disabled"));
            return 0;
        }
        long ready = SellerService.readyAmount(player);
        if (requested > ready && ready > 0) {
            player.sendSystemMessage(Lang.msg("seller.collect.too_much", Economy.format(ready)));
            return 0;
        }
        long amount = SellerService.collect(player, requested);
        if (amount <= 0) {
            player.sendSystemMessage(Lang.msg("seller.collect.none"));
            return 0;
        }
        player.sendSystemMessage(Lang.msg("seller.collect.done", Economy.format(amount)));
        long left = ready - amount;
        if (left > 0) {
            player.sendSystemMessage(Lang.msg("seller.collect.left", Economy.format(left)));
        }
        return 1;
    }

    // /seller admin reset <gracz> [limit|payout|all] - zeruje dzienny limit sprzedaży i/albo oczekiwanie na wypłatę
    private static int reset(CommandContext<CommandSourceStack> context, boolean limit, boolean payout)
            throws CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(context, "player");
        if (limit) {
            SellerService.resetDailyLimit(target.server, target.getUUID());
        }
        long matured = payout ? SellerService.resetPayoutCooldown(target.server, target.getUUID()) : 0;
        String name = target.getGameProfile().getName();
        context.getSource().sendSuccess(() -> Lang.msg("seller.admin.reset", name,
                Lang.str(limit && payout ? "seller.admin.reset.all" : limit ? "seller.admin.reset.limit"
                        : "seller.admin.reset.payout"), Economy.format(matured)), true);
        return 1;
    }

    // /seller admin extra <gracz> <ilość> - dodatkowe dzienne sprzedaże (tego używa oferta w sklepie)
    private static int extra(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(context, "player");
        int total = SellerService.addExtraSells(target.server, target.getUUID(),
                IntegerArgumentType.getInteger(context, "amount"));
        target.sendSystemMessage(Lang.msgFor(target, "seller.extra.added", total));
        context.getSource().sendSuccess(() -> Lang.msg("seller.admin.extra", target.getGameProfile().getName(), total), true);
        return total;
    }

    private static int adminAdd(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerLevel level = context.getSource().getLevel();
        BlockPos pos = BlockPosArgument.getLoadedBlockPos(context, "pos");
        if (!level.getBlockState(pos).is(Blocks.CHEST)) {
            context.getSource().sendFailure(Lang.msg("seller.admin.no_chest"));
            return 0;
        }
        ServerPlayer owner = EntityArgument.getPlayer(context, "owner");
        SellerService.register(level, pos, owner.getUUID(), owner.getGameProfile().getName());
        context.getSource().sendSuccess(() -> Lang.msg("seller.admin.added", pos.toShortString(),
                owner.getGameProfile().getName()), true);
        return 1;
    }

    // /seller admin list - wszystkie zarejestrowane skrzynki: wymiar, pozycja, właściciel, energia
    private static int adminList(CommandContext<CommandSourceStack> context) {
        SellerData data = SellerData.get(context.getSource().getServer());
        if (data.crates.isEmpty()) {
            context.getSource().sendSuccess(() -> Lang.msg("seller.admin.list.none"), false);
            return 0;
        }
        context.getSource().sendSuccess(() -> Lang.msg("seller.admin.list.header", data.crates.size()), false);
        data.crates.forEach((pos, crate) -> {
            SellerData.Ledger ledger = data.ledgers.get(crate.owner);
            String owner = ledger != null && !ledger.name.isBlank() ? ledger.name : crate.owner.toString();
            context.getSource().sendSuccess(() -> Lang.msg("seller.admin.list.line", pos.dimension().location().toString(),
                    pos.pos().toShortString(), owner, crate.energy), false);
        });
        return data.crates.size();
    }

    private static int adminRemove(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        BlockPos pos = BlockPosArgument.getLoadedBlockPos(context, "pos");
        if (!SellerService.unregister(context.getSource().getLevel(), pos)) {
            context.getSource().sendFailure(Lang.msg("seller.admin.not_crate"));
            return 0;
        }
        context.getSource().sendSuccess(() -> Lang.msg("seller.admin.removed", pos.toShortString()), true);
        return 1;
    }

    private static int adminEnergy(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerLevel level = context.getSource().getLevel();
        BlockPos pos = BlockPosArgument.getLoadedBlockPos(context, "pos");
        SellerData data = SellerData.get(level.getServer());
        SellerData.Crate crate = data.crates.get(SellerService.key(level, pos));
        if (crate == null) {
            context.getSource().sendFailure(Lang.msg("seller.admin.not_crate"));
            return 0;
        }
        crate.energy = IntegerArgumentType.getInteger(context, "amount");
        data.setDirty();
        context.getSource().sendSuccess(() -> Lang.msg("seller.admin.energy", pos.toShortString(), crate.energy), true);
        return 1;
    }
}
