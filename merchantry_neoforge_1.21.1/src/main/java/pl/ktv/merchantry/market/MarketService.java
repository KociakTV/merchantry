package pl.ktv.merchantry.market;

import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import pl.ktv.merchantry.Merchantry;
import pl.ktv.merchantry.Config;
import pl.ktv.merchantry.Lang;
import pl.ktv.merchantry.economy.Economy;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

// Logika rynku graczy: wystawianie, kupowanie, wycofywanie ofert i wypłaty dla graczy offline
public final class MarketService {
    private MarketService() {
    }

    public static MarketData data(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(MarketData.FACTORY, MarketData.NAME);
    }

    public static boolean isAvailable() {
        return Economy.isEnabled() && Config.ENABLE_MARKET.get();
    }

    // Sprawdza dostępność i wysyła powód, jeśli rynek jest wyłączony
    public static boolean checkAvailable(ServerPlayer player) {
        if (!Economy.isEnabled()) {
            player.sendSystemMessage(Lang.msg("currency.disabled"));
            return false;
        }
        if (!Config.ENABLE_MARKET.get()) {
            player.sendSystemMessage(Lang.msg("market.disabled"));
            return false;
        }
        return true;
    }

    public static long fee(long price) {
        return (long) Math.floor(price * Config.MARKET_FEE_PERCENT.get() / 100.0);
    }

    public static long countListings(MarketData data, UUID seller) {
        return data.listings.stream().filter(l -> l.seller().equals(seller)).count();
    }

    // Wystawia przedmiot z głównej ręki za podaną cenę (łącznie). amount = ile sztuk; 0 = wszystko, co jest w ręce.
    public static void createListing(ServerPlayer player, long price, int amount) {
        if (!checkAvailable(player)) {
            return;
        }
        ItemStack hand = player.getMainHandItem();
        if (hand.isEmpty()) {
            player.sendSystemMessage(Lang.msg("market.empty_hand"));
            return;
        }
        MarketData data = data(player.server);
        int max = Config.MAX_MARKET_LISTINGS.get();
        if (countListings(data, player.getUUID()) >= max) {
            player.sendSystemMessage(Lang.msg("market.limit", max));
            return;
        }
        int count = amount <= 0 ? hand.getCount() : amount;
        if (count > hand.getCount()) {
            player.sendSystemMessage(Lang.msg("market.not_enough_in_hand", hand.getCount(), hand.getHoverName()));
            return;
        }
        // Najpierw zabieramy przedmiot z ręki, potem tworzymy ofertę - bez ryzyka duplikacji
        ItemStack stack = hand.copyWithCount(count);
        hand.shrink(count);
        if (hand.isEmpty()) {
            player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        }
        data.listings.add(new MarketListing(UUID.randomUUID(), player.getUUID(), player.getGameProfile().getName(),
                stack, price, System.currentTimeMillis()));
        data.setDirty();
        player.sendSystemMessage(Lang.msg("market.listed", stack.getCount(), stack.getHoverName(), Economy.format(price)));
        Merchantry.LOGGER.info("{} wystawił na rynku {}x {} za {}", player.getGameProfile().getName(),
                stack.getCount(), stack.getItem(), price);
    }

    public static boolean buy(ServerPlayer buyer, UUID listingId) {
        if (!checkAvailable(buyer)) {
            return false;
        }
        MarketData data = data(buyer.server);
        MarketListing listing = data.find(listingId);
        if (listing == null) {
            buyer.sendSystemMessage(Lang.msg("market.gone"));
            return false;
        }
        if (listing.seller().equals(buyer.getUUID())) {
            return cancel(buyer, listingId);
        }
        if (!Economy.withdraw(buyer, listing.price())) {
            buyer.sendSystemMessage(Lang.msg("money.not_enough", Economy.format(Economy.getBalance(buyer))));
            return false;
        }
        data.listings.remove(listing);
        ItemHandlerHelper.giveItemToPlayer(buyer, listing.stack().copy());

        long payout = listing.price() - fee(listing.price());
        ServerPlayer seller = buyer.server.getPlayerList().getPlayer(listing.seller());
        if (seller != null) {
            Economy.deposit(seller, payout);
            seller.sendSystemMessage(Lang.msgFor(seller, "market.sold", listing.stack().getCount(),
                    listing.stack().getHoverName(), buyer.getGameProfile().getName(), Economy.format(payout)));
            seller.playNotifySound(SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 1.0F, 1.2F);
        } else {
            data.pendingMoney.merge(listing.seller(), payout, Long::sum);
        }
        data.setDirty();

        buyer.sendSystemMessage(Lang.msg("market.bought", listing.stack().getCount(), listing.stack().getHoverName(),
                Economy.format(listing.price()), listing.sellerName()));
        buyer.playNotifySound(SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 1.0F, 1.0F);
        Merchantry.LOGGER.info("{} kupił na rynku {}x {} od {} za {}", buyer.getGameProfile().getName(),
                listing.stack().getCount(), listing.stack().getItem(), listing.sellerName(), listing.price());
        return true;
    }

    // Wycofanie własnej oferty - przedmiot wraca do sprzedającego
    public static boolean cancel(ServerPlayer player, UUID listingId) {
        MarketData data = data(player.server);
        MarketListing listing = data.find(listingId);
        if (listing == null || !listing.seller().equals(player.getUUID())) {
            player.sendSystemMessage(Lang.msg("market.gone"));
            return false;
        }
        data.listings.remove(listing);
        data.setDirty();
        ItemHandlerHelper.giveItemToPlayer(player, listing.stack().copy());
        player.sendSystemMessage(Lang.msg("market.cancelled", listing.stack().getCount(), listing.stack().getHoverName()));
        return true;
    }

    // Usunięcie oferty przez admina - przedmiot wraca do sprzedającego (albo czeka, jeśli jest offline)
    public static void adminRemove(ServerPlayer admin, UUID listingId) {
        MarketData data = data(admin.server);
        MarketListing listing = data.find(listingId);
        if (listing == null) {
            return;
        }
        data.listings.remove(listing);
        ServerPlayer seller = admin.server.getPlayerList().getPlayer(listing.seller());
        if (seller != null) {
            ItemHandlerHelper.giveItemToPlayer(seller, listing.stack().copy());
            seller.sendSystemMessage(Lang.msgFor(seller, "market.removed_by_admin", listing.stack().getHoverName()));
        } else {
            data.mailbox.computeIfAbsent(listing.seller(), id -> new ArrayList<>()).add(listing.stack().copy());
        }
        data.setDirty();
        admin.sendSystemMessage(Lang.msg("market.admin_removed", listing.sellerName()));
        Merchantry.LOGGER.info("{} usunął ofertę rynku gracza {} ({}x {})", admin.getGameProfile().getName(),
                listing.sellerName(), listing.stack().getCount(), listing.stack().getItem());
    }

    // Przy logowaniu: pieniądze za sprzedane przedmioty i zwrócone przedmioty
    public static void deliverPending(ServerPlayer player) {
        MarketData data = data(player.server);
        Long money = data.pendingMoney.remove(player.getUUID());
        List<ItemStack> items = data.mailbox.remove(player.getUUID());
        if (money == null && items == null) {
            return;
        }
        if (money != null && money > 0) {
            Economy.deposit(player, money);
            player.sendSystemMessage(Lang.msg("market.offline_earnings", Economy.format(money)));
        }
        if (items != null) {
            for (ItemStack item : items) {
                ItemHandlerHelper.giveItemToPlayer(player, item);
            }
            player.sendSystemMessage(Lang.msg("market.mailbox", items.size()));
        }
        data.setDirty();
    }

    public static Component sellerName(MarketListing listing) {
        return Component.literal(listing.sellerName());
    }
}
