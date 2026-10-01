package pl.ktv.merchantry.event;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.CommandEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import pl.ktv.merchantry.Lang;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import pl.ktv.merchantry.Merchantry;
import pl.ktv.merchantry.command.ExchangeCommand;
import pl.ktv.merchantry.command.HomeCommands;
import pl.ktv.merchantry.command.KeepInventoryCommand;
import pl.ktv.merchantry.command.MoneyCommands;
import pl.ktv.merchantry.command.RepairCommands;
import pl.ktv.merchantry.command.ShopCommand;
import pl.ktv.merchantry.command.ShopConfigCommand;
import pl.ktv.merchantry.command.UnlockCommands;
import pl.ktv.merchantry.shop.ShopManager;

// Zdarzenia serwera: rejestracja komend oraz wczytanie ofert sklepu
@EventBusSubscriber(modid = Merchantry.MOD_ID)
public final class ServerEvents {
    private ServerEvents() {
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        ShopCommand.register(event.getDispatcher());
        ShopConfigCommand.register(event.getDispatcher(), event.getBuildContext());
        UnlockCommands.register(event.getDispatcher());
        HomeCommands.register(event.getDispatcher());
        MoneyCommands.register(event.getDispatcher());
        RepairCommands.register(event.getDispatcher());
        KeepInventoryCommand.register(event.getDispatcher());
        ExchangeCommand.register(event.getDispatcher());
        pl.ktv.merchantry.command.MarketCommand.register(event.getDispatcher());
    }

    // Każda komenda: komunikaty w języku gracza, który ją wpisał
    @SubscribeEvent
    public static void onCommand(CommandEvent event) {
        Lang.setContext(event.getParseResults().getContext().getSource().getPlayer());
    }

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        ShopManager.load(event.getServer());
        pl.ktv.merchantry.shop.sell.SellManager.load();
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        ShopManager.unload();
    }
}
