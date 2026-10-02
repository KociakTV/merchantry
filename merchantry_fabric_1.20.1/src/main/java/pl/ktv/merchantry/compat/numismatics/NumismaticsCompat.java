package pl.ktv.merchantry.compat.numismatics;

import dev.ithundxr.createnumismatics.Numismatics;
import dev.ithundxr.createnumismatics.content.backend.BankAccount;
import dev.ithundxr.createnumismatics.content.backend.Coin;
import dev.ithundxr.createnumismatics.content.coins.CoinItem;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import pl.ktv.merchantry.Merchantry;
import pl.ktv.merchantry.Lang;
import pl.ktv.merchantry.economy.Economy;
import pl.ktv.merchantry.exchange.Exchange;

import java.util.Locale;

// Jedyne miejsce (poza ExchangeMenu), które używa klas Create: Numismatics.
// Wywoływane wyłącznie, gdy Numismatics jest zainstalowany (sprawdza to Exchange).
public final class NumismaticsCompat {
    private NumismaticsCompat() {
    }

    public static void openExchange(ServerPlayer player, boolean returnToShop) {
        ExchangeMenu.open(player, returnToShop);
    }

    private static BankAccount account(ServerPlayer player) {
        return Numismatics.BANK.getAccount(player);
    }

    public static int bankBalance(ServerPlayer player) {
        return account(player).getBalance();
    }

    // Wartość (w spurach) wszystkich monet w ekwipunku
    public static long coinsInInventory(ServerPlayer player) {
        long total = 0;
        Inventory inventory = player.getInventory();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (stack.getItem() instanceof CoinItem coin) {
                total += (long) coin.coin.value * stack.getCount();
            }
        }
        return total;
    }

    public static ItemStack coinIcon(int spurs) {
        return Coin.closest(spurs).asStack();
    }

    public static String formatSpurs(long spurs) {
        return String.format(Locale.ROOT, "%,d", spurs).replace(',', ' ') + " ¤";
    }

    // $ -> konto w banku Numismatics
    public static void dollarsToBank(ServerPlayer player, long dollars) {
        long spurs = Math.min(Exchange.dollarsToSpurs(dollars), Integer.MAX_VALUE);
        if (dollars <= 0 || spurs <= 0) {
            player.sendSystemMessage(Lang.msg("exchange.too_small"));
            return;
        }
        if (!Economy.withdraw(player, dollars)) {
            player.sendSystemMessage(Lang.msg("money.not_enough", Economy.format(Economy.getBalance(player))));
            return;
        }
        account(player).deposit((int) spurs);
        done(player, Economy.format(dollars), formatSpurs(spurs));
    }

    // Konto w banku Numismatics -> $
    public static void bankToDollars(ServerPlayer player, long spurs) {
        long dollars = Exchange.spursToDollars(spurs);
        if (spurs <= 0 || dollars <= 0) {
            player.sendSystemMessage(Lang.msg("exchange.too_small"));
            return;
        }
        if (spurs > Integer.MAX_VALUE || !account(player).deduct((int) spurs, false)) {
            player.sendSystemMessage(Lang.msg("exchange.bank_not_enough", formatSpurs(bankBalance(player))));
            return;
        }
        Economy.deposit(player, dollars);
        done(player, formatSpurs(spurs), Economy.format(dollars));
    }

    // Wszystkie monety z ekwipunku -> $
    public static void coinsToDollars(ServerPlayer player) {
        long spurs = coinsInInventory(player);
        long dollars = Exchange.spursToDollars(spurs);
        if (spurs <= 0) {
            player.sendSystemMessage(Lang.msg("exchange.no_coins"));
            return;
        }
        if (dollars <= 0) {
            player.sendSystemMessage(Lang.msg("exchange.too_small"));
            return;
        }
        Inventory inventory = player.getInventory();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            if (inventory.getItem(i).getItem() instanceof CoinItem) {
                inventory.setItem(i, ItemStack.EMPTY);
            }
        }
        player.inventoryMenu.broadcastChanges();
        Economy.deposit(player, dollars);
        done(player, formatSpurs(spurs), Economy.format(dollars));
    }

    private static void done(ServerPlayer player, String from, String to) {
        player.sendSystemMessage(Lang.msg("exchange.done", from, to));
        Merchantry.LOGGER.info("{} wymienił w kantorze {} -> {}", player.getGameProfile().getName(), from, to);
    }
}
