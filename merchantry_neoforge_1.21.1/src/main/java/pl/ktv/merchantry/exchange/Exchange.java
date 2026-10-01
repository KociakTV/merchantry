package pl.ktv.merchantry.exchange;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.fml.ModList;
import pl.ktv.merchantry.Config;
import pl.ktv.merchantry.Lang;
import pl.ktv.merchantry.compat.numismatics.NumismaticsCompat;
import pl.ktv.merchantry.economy.Economy;

// Kantor $ <-> Create: Numismatics. Ta klasa nie odwołuje się do klas Numismatics,
// więc można jej bezpiecznie używać także wtedy, gdy mod nie jest zainstalowany.
public final class Exchange {
    public static final String NUMISMATICS_ID = "numismatics";

    private Exchange() {
    }

    public static boolean isNumismaticsLoaded() {
        return ModList.get().isLoaded(NUMISMATICS_ID);
    }

    public static boolean isAvailable() {
        return isNumismaticsLoaded() && Economy.isEnabled() && Config.ENABLE_EXCHANGE.get();
    }

    // Ikona kantoru: Bank Terminal z Numismatics (szukany po ID, bez odwołań do klas moda); w razie braku - szmaragd
    public static ItemStack icon() {
        Item terminal = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(NUMISMATICS_ID, "bank_terminal"));
        return new ItemStack(terminal == Items.AIR ? Items.EMERALD : terminal);
    }

    // Otwiera kantor albo wyjaśnia, dlaczego jest niedostępny. returnToShop = przycisk powrotu do sklepu.
    public static void open(ServerPlayer player, boolean returnToShop) {
        if (!isNumismaticsLoaded()) {
            player.sendSystemMessage(Lang.msg("exchange.no_numismatics"));
        } else if (!Economy.isEnabled()) {
            player.sendSystemMessage(Lang.msg("currency.disabled"));
        } else if (!Config.ENABLE_EXCHANGE.get()) {
            player.sendSystemMessage(Lang.msg("exchange.disabled"));
        } else {
            NumismaticsCompat.openExchange(player, returnToShop);
        }
    }

    private static double feeFactor() {
        return 1.0 - Config.EXCHANGE_FEE_PERCENT.get() / 100.0;
    }

    // Ile spurów dostanie gracz za podaną kwotę $ (po prowizji)
    public static long dollarsToSpurs(long dollars) {
        return (long) Math.floor(dollars * feeFactor() / Config.DOLLARS_PER_SPUR.get());
    }

    // Ile $ dostanie gracz za podaną liczbę spurów (po prowizji)
    public static long spursToDollars(long spurs) {
        return (long) Math.floor(spurs * Config.DOLLARS_PER_SPUR.get() * feeFactor());
    }
}
