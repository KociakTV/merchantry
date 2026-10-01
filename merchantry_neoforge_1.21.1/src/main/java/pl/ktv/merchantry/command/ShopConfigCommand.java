package pl.ktv.merchantry.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.item.ItemArgument;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import pl.ktv.merchantry.economy.Economy;
import pl.ktv.merchantry.shop.sell.SellManager;
import pl.ktv.merchantry.shop.editor.EditorListMenu;
import pl.ktv.merchantry.shop.editor.OfferEditMenu;
import net.minecraft.world.item.ItemStack;
import pl.ktv.merchantry.Lang;
import pl.ktv.merchantry.shop.ItemPrice;
import pl.ktv.merchantry.shop.OfferType;
import pl.ktv.merchantry.shop.ShopManager;
import pl.ktv.merchantry.shop.ShopOffer;
import pl.ktv.merchantry.unlock.Unlock;

import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

// /shopconfig - zarządzanie ofertami sklepu przez admina (poziom uprawnień 2)
public final class ShopConfigCommand {
    private static final SuggestionProvider<CommandSourceStack> OFFER_IDS = (context, builder) ->
            SharedSuggestionProvider.suggest(ShopManager.offers().stream().map(o -> o.id), builder);
    private static final SuggestionProvider<CommandSourceStack> UNLOCK_IDS = (context, builder) ->
            SharedSuggestionProvider.suggest(Arrays.stream(Unlock.values()).map(Unlock::id), builder);

    private ShopConfigCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext buildContext) {
        dispatcher.register(Commands.literal("shopconfig")
                .requires(source -> source.hasPermission(2))
                // Bez argumentów: edytor graficzny
                .executes(context -> {
                    EditorListMenu.open(context.getSource().getPlayerOrException(), 0);
                    return 1;
                })
                .then(Commands.literal("edit")
                        .executes(context -> {
                            EditorListMenu.open(context.getSource().getPlayerOrException(), 0);
                            return 1;
                        })
                        .then(offerId().executes(context -> {
                            ShopOffer offer = findOrFail(context);
                            if (offer == null) {
                                return 0;
                            }
                            OfferEditMenu.open(context.getSource().getPlayerOrException(), offer);
                            return 1;
                        })))
                // Wysyłane przez opcjonalną część klienta po upuszczeniu przedmiotu z JEI/EMI na pole edytora
                .then(Commands.literal("drop").then(offerId()
                        .then(Commands.argument("field", StringArgumentType.word())
                                .suggests((c, b) -> SharedSuggestionProvider.suggest(List.of(OfferEditMenu.FIELD_PRODUCT,
                                        OfferEditMenu.FIELD_ICON, OfferEditMenu.FIELD_PRICE_ITEM), b))
                                .then(Commands.argument("item", ItemArgument.item(buildContext))
                                        .executes(ShopConfigCommand::drop)))))
                // Skup serwera (zakładka "Sprzedaj"): cena za 1 sztukę
                .then(Commands.literal("sell")
                        .then(Commands.literal("set")
                                .then(Commands.argument("item", ItemArgument.item(buildContext))
                                        .then(Commands.argument("price", LongArgumentType.longArg(1))
                                                .executes(context -> {
                                                    var item = ItemArgument.getItem(context, "item").getItem();
                                                    long price = LongArgumentType.getLong(context, "price");
                                                    SellManager.set(item, price);
                                                    context.getSource().sendSuccess(() -> Lang.msg("config.sell.set",
                                                            item.getDescription(), Economy.format(price)), true);
                                                    return 1;
                                                }))))
                        .then(Commands.literal("remove")
                                .then(Commands.argument("item", ItemArgument.item(buildContext))
                                        .executes(context -> {
                                            var item = ItemArgument.getItem(context, "item").getItem();
                                            if (!SellManager.remove(item)) {
                                                context.getSource().sendFailure(Lang.msg("config.sell.not_found", item.getDescription()));
                                                return 0;
                                            }
                                            context.getSource().sendSuccess(() -> Lang.msg("config.sell.removed",
                                                    item.getDescription()), true);
                                            return 1;
                                        })))
                        .then(Commands.literal("reload").executes(context -> {
                            SellManager.load();
                            context.getSource().sendSuccess(() -> Lang.msg("config.reloaded", SellManager.offers().size()), true);
                            return 1;
                        })))
                .then(Commands.literal("list").executes(ShopConfigCommand::list))
                .then(Commands.literal("reload").executes(ShopConfigCommand::reload))
                .then(Commands.literal("add")
                        .then(Commands.literal("item").then(newId().executes(ShopConfigCommand::addItem)))
                        .then(Commands.literal("unlock").then(newId()
                                .then(Commands.argument("unlock", StringArgumentType.word()).suggests(UNLOCK_IDS)
                                        .executes(ShopConfigCommand::addUnlock))))
                        .then(Commands.literal("homeslot").then(newId().executes(ShopConfigCommand::addHomeSlot)))
                        .then(Commands.literal("repair").then(newId()
                                .executes(context -> addRepair(context, OfferType.REPAIR))))
                        .then(Commands.literal("repairall").then(newId()
                                .executes(context -> addRepair(context, OfferType.REPAIR_ALL))))
                        .then(Commands.literal("keepinventory").then(newId()
                                .executes(context -> add(context, OfferType.KEEP_INVENTORY, o -> {
                                    ShopOffer defaults = ShopManager.keepInventory(o.id);
                                    o.name = defaults.name;
                                    o.description.addAll(defaults.description);
                                }))))
                        .then(Commands.literal("command").then(newId()
                                .then(Commands.argument("command", StringArgumentType.greedyString())
                                        .executes(ShopConfigCommand::addCommand)))))
                .then(Commands.literal("remove").then(offerId().executes(ShopConfigCommand::remove)))
                .then(Commands.literal("price").then(offerId()
                        .then(Commands.literal("money")
                                .then(Commands.argument("amount", LongArgumentType.longArg(0))
                                        .executes(context -> edit(context, o ->
                                                o.moneyPrice = LongArgumentType.getLong(context, "amount")))))
                        .then(Commands.literal("item")
                                .then(Commands.argument("item", ItemArgument.item(buildContext))
                                        .then(Commands.argument("count", IntegerArgumentType.integer(1, 1_000_000))
                                                .executes(context -> edit(context, o -> o.itemPrice = new ItemPrice(
                                                        ItemArgument.getItem(context, "item").getItem(),
                                                        IntegerArgumentType.getInteger(context, "count")))))))
                        .then(Commands.literal("clear")
                                .then(Commands.literal("money").executes(context -> edit(context, o -> o.moneyPrice = null)))
                                .then(Commands.literal("item").executes(context -> edit(context, o -> o.itemPrice = null))))))
                .then(Commands.literal("name").then(offerId()
                        .then(Commands.argument("name", StringArgumentType.greedyString())
                                .executes(context -> edit(context, o ->
                                        o.name = StringArgumentType.getString(context, "name"))))))
                .then(Commands.literal("desc").then(offerId()
                        .then(Commands.argument("text", StringArgumentType.greedyString())
                                .executes(context -> edit(context, o -> setDescription(o,
                                        StringArgumentType.getString(context, "text")))))))
                .then(Commands.literal("icon").then(offerId().executes(ShopConfigCommand::setIcon)))
                .then(Commands.literal("setitem").then(offerId().executes(ShopConfigCommand::setItem)))
                .then(Commands.literal("command").then(offerId()
                        .then(Commands.argument("command", StringArgumentType.greedyString())
                                .executes(context -> edit(context, o ->
                                        o.command = stripSlash(StringArgumentType.getString(context, "command")))))))
                .then(Commands.literal("move").then(offerId()
                        .then(Commands.argument("position", IntegerArgumentType.integer(1))
                                .executes(ShopConfigCommand::move)))));
    }

    private static RequiredArgumentBuilder<CommandSourceStack, String> newId() {
        return Commands.argument("id", StringArgumentType.word());
    }

    private static RequiredArgumentBuilder<CommandSourceStack, String> offerId() {
        return Commands.argument("id", StringArgumentType.word()).suggests(OFFER_IDS);
    }

    // Ustawia produkt / ikonę / przedmiot ceny i odświeża otwarty edytor (bez komunikatu - zmiana widać w oknie)
    private static int drop(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ShopOffer offer = findOrFail(context);
        if (offer == null) {
            return 0;
        }
        ItemStack stack = ItemArgument.getItem(context, "item").createItemStack(1, false);
        switch (StringArgumentType.getString(context, "field")) {
            case OfferEditMenu.FIELD_PRODUCT -> {
                if (offer.type != OfferType.ITEM) {
                    return 0;
                }
                offer.item = stack.copyWithCount(offer.item.isEmpty() ? 1 : Math.min(offer.item.getCount(), stack.getMaxStackSize()));
            }
            case OfferEditMenu.FIELD_ICON -> offer.icon = stack;
            case OfferEditMenu.FIELD_PRICE_ITEM -> {
                if (offer.type == OfferType.REPAIR || offer.type == OfferType.REPAIR_ALL) {
                    return 0;
                }
                offer.itemPrice = new ItemPrice(stack.getItem(), offer.itemPrice != null ? offer.itemPrice.count() : 1);
            }
            default -> {
                return 0;
            }
        }
        ShopManager.save();
        if (context.getSource().getPlayer() != null) {
            OfferEditMenu.refreshFor(context.getSource().getPlayer(), offer);
        }
        return 1;
    }

    private static int list(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        List<ShopOffer> offers = ShopManager.offers();
        source.sendSystemMessage(Lang.msg("config.list.header", offers.size()));
        for (int i = 0; i < offers.size(); i++) {
            ShopOffer o = offers.get(i);
            String money = o.moneyPrice != null ? String.valueOf(o.moneyPrice) : "-";
            Component item = o.itemPrice != null
                    ? Component.literal(o.itemPrice.count() + "x ").append(o.itemPrice.item().getDescription())
                    : Component.literal("-");
            source.sendSystemMessage(Lang.msg("config.list.entry", i + 1, o.id, o.type.getSerializedName(),
                    o.displayName(), money, item));
        }
        return offers.size();
    }

    private static int reload(CommandContext<CommandSourceStack> context) {
        ShopManager.load(context.getSource().getServer());
        context.getSource().sendSuccess(() -> Lang.msg("config.reloaded", ShopManager.offers().size()), true);
        return 1;
    }

    private static int addItem(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ItemStack hand = context.getSource().getPlayerOrException().getMainHandItem();
        if (hand.isEmpty()) {
            context.getSource().sendFailure(Lang.msg("config.empty_hand"));
            return 0;
        }
        return add(context, OfferType.ITEM, o -> o.item = hand.copy());
    }

    private static int addUnlock(CommandContext<CommandSourceStack> context) {
        String unlockId = StringArgumentType.getString(context, "unlock");
        Unlock unlock = Unlock.byId(unlockId);
        if (unlock == null) {
            context.getSource().sendFailure(Lang.msg("config.unknown_unlock", unlockId));
            return 0;
        }
        return add(context, OfferType.UNLOCK, o -> {
            o.unlock = unlock.id();
            o.name = "@default.unlock." + unlock.id();
            o.description.add("@default.unlock.desc:" + unlock.id());
        });
    }

    private static int addHomeSlot(CommandContext<CommandSourceStack> context) {
        return add(context, OfferType.HOME_SLOT, o -> {
            o.name = "@default.home_slot";
            o.description.add("@default.home_slot.desc");
        });
    }

    // Naprawy mają cenę liczoną automatycznie - nie trzeba ustawiać price
    private static int addRepair(CommandContext<CommandSourceStack> context, OfferType type) {
        return add(context, type, o -> {
            ShopOffer defaults = ShopManager.repair(type);
            o.name = defaults.name;
            o.description.addAll(defaults.description);
        });
    }

    private static int addCommand(CommandContext<CommandSourceStack> context) {
        return add(context, OfferType.COMMAND, o -> o.command = stripSlash(StringArgumentType.getString(context, "command")));
    }

    private static int add(CommandContext<CommandSourceStack> context, OfferType type, Consumer<ShopOffer> setup) {
        String id = StringArgumentType.getString(context, "id");
        if (ShopManager.find(id) != null) {
            context.getSource().sendFailure(Lang.msg("config.exists", id));
            return 0;
        }
        ShopOffer offer = new ShopOffer(id, type);
        setup.accept(offer);
        ShopManager.offers().add(offer);
        ShopManager.save();
        context.getSource().sendSuccess(() -> Lang.msg("config.added", id, id), true);
        return 1;
    }

    private static int remove(CommandContext<CommandSourceStack> context) {
        ShopOffer offer = findOrFail(context);
        if (offer == null) {
            return 0;
        }
        ShopManager.offers().remove(offer);
        ShopManager.save();
        context.getSource().sendSuccess(() -> Lang.msg("config.removed", offer.id), true);
        return 1;
    }

    private static int setIcon(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ItemStack hand = context.getSource().getPlayerOrException().getMainHandItem();
        if (hand.isEmpty()) {
            context.getSource().sendFailure(Lang.msg("config.empty_hand"));
            return 0;
        }
        return edit(context, o -> o.icon = hand.copyWithCount(1));
    }

    private static int setItem(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ItemStack hand = context.getSource().getPlayerOrException().getMainHandItem();
        if (hand.isEmpty()) {
            context.getSource().sendFailure(Lang.msg("config.empty_hand"));
            return 0;
        }
        return edit(context, o -> o.item = hand.copy());
    }

    private static int move(CommandContext<CommandSourceStack> context) {
        ShopOffer offer = findOrFail(context);
        if (offer == null) {
            return 0;
        }
        List<ShopOffer> offers = ShopManager.offers();
        int position = Math.min(IntegerArgumentType.getInteger(context, "position"), offers.size()) - 1;
        offers.remove(offer);
        offers.add(position, offer);
        ShopManager.save();
        context.getSource().sendSuccess(() -> Lang.msg("config.updated", offer.id), true);
        return 1;
    }

    private static int edit(CommandContext<CommandSourceStack> context, Consumer<ShopOffer> change) {
        ShopOffer offer = findOrFail(context);
        if (offer == null) {
            return 0;
        }
        change.accept(offer);
        ShopManager.save();
        // Klikalny odnośnik z powrotem do edytora tej oferty
        String command = "/shopconfig edit " + offer.id;
        context.getSource().sendSuccess(() -> Lang.msg("config.updated", offer.id).append(" ").append(
                Lang.msg("editor.reopen").withStyle(style -> style
                        .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, command))
                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal(command))))), true);
        return 1;
    }

    private static ShopOffer findOrFail(CommandContext<CommandSourceStack> context) {
        String id = StringArgumentType.getString(context, "id");
        ShopOffer offer = ShopManager.find(id);
        if (offer == null) {
            context.getSource().sendFailure(Lang.msg("config.not_found", id));
        }
        return offer;
    }

    // Linie opisu oddzielone znakiem |, a "-" czyści opis
    private static void setDescription(ShopOffer offer, String text) {
        offer.description.clear();
        if (!text.equals("-")) {
            offer.description.addAll(Arrays.asList(text.split("\\|")));
        }
    }

    private static String stripSlash(String command) {
        return command.startsWith("/") ? command.substring(1) : command;
    }
}
