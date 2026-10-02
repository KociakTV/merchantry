package pl.ktv.merchantry.event;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import pl.ktv.merchantry.Config;
import pl.ktv.merchantry.compat.CompatOffers;
import pl.ktv.merchantry.command.HomeCommands;
import pl.ktv.merchantry.command.KeepInventoryCommand;
import pl.ktv.merchantry.command.MarketCommand;
import pl.ktv.merchantry.command.MoneyCommands;
import pl.ktv.merchantry.command.RepairCommands;
import pl.ktv.merchantry.command.ShopCommand;
import pl.ktv.merchantry.command.ShopConfigCommand;
import pl.ktv.merchantry.command.UnlockCommands;
import pl.ktv.merchantry.shop.ShopManager;
import pl.ktv.merchantry.shop.sell.SellManager;

// Zdarzenia serwera: rejestracja komend oraz wczytanie konfiguracji i ofert sklepu.
// Język komend ustawia mixin CommandsMixin (odpowiednik CommandEvent z NeoForge).
public final class ServerEvents {
    private ServerEvents() {
    }

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, buildContext, selection) -> {
            ShopCommand.register(dispatcher);
            ShopConfigCommand.register(dispatcher, buildContext);
            UnlockCommands.register(dispatcher);
            HomeCommands.register(dispatcher);
            MoneyCommands.register(dispatcher);
            RepairCommands.register(dispatcher);
            KeepInventoryCommand.register(dispatcher);
            MarketCommand.register(dispatcher);
        });

        // Konfiguracja czytana przy każdym starcie świata (zmiany w pliku bez restartu gry)
        ServerLifecycleEvents.SERVER_STARTING.register(server -> Config.load());
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            ShopManager.load(server);
            SellManager.load();
            // Domyślne oferty dla zainstalowanych modów (np. FTB Ultimine) - raz na mod
            CompatOffers.apply();
        });
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> ShopManager.unload());
    }
}
