package pl.ktv.merchantry.event;

import net.fabricmc.fabric.api.entity.event.v1.ServerEntityWorldChangeEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import pl.ktv.merchantry.Config;
import pl.ktv.merchantry.Lang;
import pl.ktv.merchantry.command.HomeCommands;
import pl.ktv.merchantry.data.ModAttachments;
import pl.ktv.merchantry.data.PlayerData;
import pl.ktv.merchantry.economy.Cheque;
import pl.ktv.merchantry.economy.Earnings;
import pl.ktv.merchantry.economy.Economy;
import pl.ktv.merchantry.economy.Sidebar;
import pl.ktv.merchantry.keepinventory.KeepInventory;
import pl.ktv.merchantry.market.MarketService;

// Zdarzenia graczy: saldo startowe, zarabianie, realizacja czeków, keepInventory
public final class PlayerEvents {
    private PlayerEvents() {
    }

    public static void register() {
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> onLogin(handler.getPlayer()));
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> onLogout(handler.getPlayer()));
        ServerTickEvents.END_SERVER_TICK.register(PlayerEvents::onServerTick);
        ServerEntityWorldChangeEvents.AFTER_PLAYER_CHANGE_WORLD.register((player, origin, destination) ->
                onChangeDimension(player, destination));
        // Po odrodzeniu - dane gracza (z keepInventory) są już skopiowane na nową postać
        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
            if (!alive) {
                KeepInventory.onRespawn(oldPlayer, newPlayer);
            }
        });
        UseItemCallback.EVENT.register(PlayerEvents::onRightClickItem);
    }

    private static void onLogin(ServerPlayer player) {
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

    private static void onLogout(ServerPlayer player) {
        Earnings.clear(player.getUUID());
        Sidebar.clear(player.getUUID());
        HomeCommands.clear(player.getUUID());
    }

    // Fabric nie ma zdarzenia ticku gracza - raz na tick serwera przechodzimy po graczach
    private static void onServerTick(MinecraftServer server) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (player.tickCount % 20 != 0) {
                continue;
            }
            Lang.setContext(player);
            if (Economy.isEnabled()) {
                Earnings.tick(player);
                Economy.syncPayoutScores(player);
            }
            Sidebar.update(player);
        }
    }

    // Wywoływane z mixinu PlayerAdvancementsMixin, gdy gracz zdobędzie osiągnięcie
    public static void onAdvancement(ServerPlayer player, AdvancementHolder advancement) {
        if (Economy.isEnabled()) {
            Lang.setContext(player);
            Earnings.onAdvancement(player, advancement);
        }
    }

    private static void onChangeDimension(ServerPlayer player, net.minecraft.server.level.ServerLevel destination) {
        if (Economy.isEnabled()) {
            Lang.setContext(player);
            Earnings.onDimension(player, destination.dimension(), false);
        }
    }

    // Wywoływane z mixinu ServerPlayerMixin na początku śmierci gracza (po totemie, przed wypadnięciem przedmiotów)
    public static void onDeath(ServerPlayer player) {
        Lang.setContext(player);
        KeepInventory.onDeath(player);
    }

    private static InteractionResultHolder<ItemStack> onRightClickItem(Player entity, Level level, InteractionHand hand) {
        ItemStack stack = entity.getItemInHand(hand);
        if (!(entity instanceof ServerPlayer player) || !Cheque.isCheque(stack)) {
            return InteractionResultHolder.pass(stack);
        }
        Lang.setContext(player);
        if (!Economy.isEnabled()) {
            player.sendSystemMessage(Lang.msg("currency.disabled"));
        } else {
            Cheque.redeem(player, stack);
        }
        return InteractionResultHolder.success(player.getItemInHand(hand));
    }
}
