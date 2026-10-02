package pl.ktv.merchantry.compat.numismatics;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import pl.ktv.merchantry.Config;
import pl.ktv.merchantry.Lang;
import pl.ktv.merchantry.economy.Economy;
import pl.ktv.merchantry.exchange.Exchange;
import pl.ktv.merchantry.shop.ShopMenu;

import java.util.List;
import java.util.Locale;

// Okno kantoru (skrzynka 9x3):
//  rząd 1: informacje (saldo $, saldo w banku, kurs)
//  rząd 2: $ -> bank Numismatics (10, 100, 1000, wszystko) oraz monety z ekwipunku -> $
//  rząd 3: bank Numismatics -> $ (cog, crown, sun, wszystko) oraz powrót
public class ExchangeMenu extends ChestMenu {
    private static final int SLOT_INFO = 4;
    private static final int[] DOLLAR_SLOTS = {10, 11, 12, 13};
    private static final long[] DOLLAR_AMOUNTS = {10, 100, 1000, -1};
    private static final int SLOT_COINS = 16;
    private static final int[] SPUR_SLOTS = {19, 20, 21, 22};
    private static final long[] SPUR_AMOUNTS = {64, 512, 4096, -1};
    private static final int SLOT_BACK = 26;

    private final ServerPlayer player;
    private final SimpleContainer display;
    private final boolean returnToShop;

    private ExchangeMenu(int containerId, Inventory inventory, ServerPlayer player, SimpleContainer display,
                         boolean returnToShop) {
        super(MenuType.GENERIC_9x3, containerId, inventory, display, 3);
        this.player = player;
        this.display = display;
        this.returnToShop = returnToShop;
        render();
    }

    public static void open(ServerPlayer player, boolean returnToShop) {
        player.openMenu(new SimpleMenuProvider(
                (id, inventory, p) -> new ExchangeMenu(id, inventory, player, new SimpleContainer(27), returnToShop),
                Lang.msg("exchange.title")));
    }

    @Override
    public void clicked(int slotId, int button, ClickType clickType, Player clicker) {
        Lang.setContext(player);
        if (slotId >= 0 && slotId < 27 && (clickType == ClickType.PICKUP || clickType == ClickType.QUICK_MOVE)) {
            handleClick(slotId);
        }
        if (player.containerMenu == this) {
            sendAllDataToRemote();
        }
    }

    @Override
    public ItemStack quickMoveStack(Player clicker, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player clicker) {
        return true;
    }

    private void handleClick(int slot) {
        // Ustawienia mogły się zmienić w trakcie (np. admin wyłączył kantor)
        if (!Exchange.isAvailable()) {
            player.closeContainer();
            return;
        }
        for (int i = 0; i < DOLLAR_SLOTS.length; i++) {
            if (slot == DOLLAR_SLOTS[i]) {
                long amount = DOLLAR_AMOUNTS[i] < 0 ? Economy.getBalance(player) : DOLLAR_AMOUNTS[i];
                NumismaticsCompat.dollarsToBank(player, amount);
                afterExchange();
                return;
            }
        }
        for (int i = 0; i < SPUR_SLOTS.length; i++) {
            if (slot == SPUR_SLOTS[i]) {
                long amount = SPUR_AMOUNTS[i] < 0 ? NumismaticsCompat.bankBalance(player) : SPUR_AMOUNTS[i];
                NumismaticsCompat.bankToDollars(player, amount);
                afterExchange();
                return;
            }
        }
        if (slot == SLOT_COINS) {
            NumismaticsCompat.coinsToDollars(player);
            afterExchange();
        } else if (slot == SLOT_BACK) {
            if (returnToShop) {
                ShopMenu.open(player);
            } else {
                player.closeContainer();
            }
        }
    }

    private void afterExchange() {
        player.playNotifySound(SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.7F, 1.2F);
        render();
    }

    private void render() {
        display.clearContent();
        ShopMenu.fill(display, 0, 27);

        long balance = Economy.getBalance(player);
        int bank = NumismaticsCompat.bankBalance(player);
        display.setItem(SLOT_INFO, ShopMenu.named(Exchange.icon(), Lang.msg("exchange.title"), List.of(
                Lang.msg("exchange.info.dollars", Economy.format(balance)),
                Lang.msg("exchange.info.bank", NumismaticsCompat.formatSpurs(bank)),
                Component.empty(),
                Lang.msg("exchange.info.rate", formatRate()),
                Lang.msg("exchange.info.fee", formatNumber(Config.EXCHANGE_FEE_PERCENT.get())))));

        Item[] dollarIcons = {Items.GOLD_NUGGET, Items.GOLD_INGOT, Items.GOLD_BLOCK, Items.CHEST};
        for (int i = 0; i < DOLLAR_SLOTS.length; i++) {
            long amount = DOLLAR_AMOUNTS[i] < 0 ? balance : DOLLAR_AMOUNTS[i];
            Component name = DOLLAR_AMOUNTS[i] < 0
                    ? Lang.msg("exchange.to_bank_all")
                    : Lang.msg("exchange.to_bank", Economy.format(amount));
            display.setItem(DOLLAR_SLOTS[i], ShopMenu.named(new ItemStack(dollarIcons[i]), name, List.of(
                    Lang.msg("exchange.you_pay", Economy.format(amount)),
                    Lang.msg("exchange.you_get", NumismaticsCompat.formatSpurs(Exchange.dollarsToSpurs(amount))))));
        }

        for (int i = 0; i < SPUR_SLOTS.length; i++) {
            long amount = SPUR_AMOUNTS[i] < 0 ? bank : SPUR_AMOUNTS[i];
            Component name = SPUR_AMOUNTS[i] < 0
                    ? Lang.msg("exchange.from_bank_all")
                    : Lang.msg("exchange.from_bank", NumismaticsCompat.formatSpurs(amount));
            ItemStack icon = SPUR_AMOUNTS[i] < 0 ? new ItemStack(Items.ENDER_CHEST) : NumismaticsCompat.coinIcon((int) amount);
            display.setItem(SPUR_SLOTS[i], ShopMenu.named(icon, name, List.of(
                    Lang.msg("exchange.you_pay", NumismaticsCompat.formatSpurs(amount)),
                    Lang.msg("exchange.you_get", Economy.format(Exchange.spursToDollars(amount))))));
        }

        long coins = NumismaticsCompat.coinsInInventory(player);
        display.setItem(SLOT_COINS, ShopMenu.named(NumismaticsCompat.coinIcon(64), Lang.msg("exchange.coins"), List.of(
                Lang.msg("exchange.coins.have", NumismaticsCompat.formatSpurs(coins)),
                Lang.msg("exchange.you_get", Economy.format(Exchange.spursToDollars(coins))))));

        display.setItem(SLOT_BACK, ShopMenu.named(new ItemStack(returnToShop ? Items.ARROW : Items.BARRIER),
                Lang.msg(returnToShop ? "exchange.back" : "exchange.close"), List.of()));
    }

    private static String formatRate() {
        return "1 ¤ = " + formatNumber(Config.DOLLARS_PER_SPUR.get()) + " " + Config.CURRENCY_SYMBOL.get();
    }

    private static String formatNumber(double value) {
        return value == Math.rint(value) ? String.valueOf((long) value) : String.format(Locale.ROOT, "%.4f", value)
                .replaceAll("0+$", "");
    }
}
