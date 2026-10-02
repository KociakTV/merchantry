package pl.ktv.merchantry.event;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import pl.ktv.merchantry.keepinventory.KeepInventory;
import pl.ktv.merchantry.market.MarketService;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.AdvancementEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import pl.ktv.merchantry.Merchantry;
import pl.ktv.merchantry.Config;
import pl.ktv.merchantry.Lang;
import pl.ktv.merchantry.command.HomeCommands;
import pl.ktv.merchantry.data.ModAttachments;
import pl.ktv.merchantry.data.PlayerData;
import pl.ktv.merchantry.economy.Cheque;
import pl.ktv.merchantry.economy.Earnings;
import pl.ktv.merchantry.economy.Economy;
import pl.ktv.merchantry.economy.Sidebar;

// Zdarzenia graczy: saldo startowe, zarabianie, realizacja czeków
@EventBusSubscriber(modid = Merchantry.MOD_ID)
public final class PlayerEvents {
    private PlayerEvents() {
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        Lang.setContext(player);
        // Pieniądze i przedmioty z rynku, które czekały, gdy gracz był offline
        MarketService.deliverPending(player);
        if (!Economy.isEnabled()) {
            return;
        }
        PlayerData data = ModAttachments.get(player);
        if (!data.initialized) {
            data.initialized = true;
            data.balance += Config.STARTING_BALANCE.get();
            // Wymiar startowy nie daje nagrody
            Earnings.onDimension(player, player.level().dimension(), true);
        }
        Economy.syncScoreboard(player);
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        Earnings.clear(event.getEntity().getUUID());
        Sidebar.clear(event.getEntity().getUUID());
        HomeCommands.clear(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.tickCount % 20 != 0) {
            return;
        }
        Lang.setContext(player);
        if (Economy.isEnabled()) {
            Earnings.tick(player);
            Economy.syncPayoutScores(player);
        }
        Sidebar.update(player);
    }

    @SubscribeEvent
    public static void onAdvancement(AdvancementEvent.AdvancementEarnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && Economy.isEnabled()) {
            Lang.setContext(player);
            Earnings.onAdvancement(player, event.getAdvancement());
        }
    }

    @SubscribeEvent
    public static void onChangeDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && Economy.isEnabled()) {
            Lang.setContext(player);
            Earnings.onDimension(player, event.getTo(), false);
        }
    }

    // Najniższy priorytet: jeśli inny mod anuluje śmierć (np. totem), nie ruszamy ekwipunku
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            Lang.setContext(player);
            KeepInventory.onDeath(player);
        }
    }

    // Po skopiowaniu danych gracza (NeoForge robi to z najwyższym priorytetem) oddajemy ekwipunek
    @SubscribeEvent
    public static void onClone(PlayerEvent.Clone event) {
        if (event.isWasDeath() && event.getOriginal() instanceof ServerPlayer original
                && event.getEntity() instanceof ServerPlayer player) {
            KeepInventory.onRespawn(original, player);
        }
    }

    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !Cheque.isCheque(event.getItemStack())) {
            return;
        }
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
        Lang.setContext(player);
        if (!Economy.isEnabled()) {
            player.sendSystemMessage(Lang.msg("currency.disabled"));
            return;
        }
        Cheque.redeem(player, event.getItemStack());
    }
}
