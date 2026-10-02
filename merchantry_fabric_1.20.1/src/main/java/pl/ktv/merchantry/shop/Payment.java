package pl.ktv.merchantry.shop;

import pl.ktv.merchantry.util.Stacks;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import pl.ktv.merchantry.Config;
import pl.ktv.merchantry.Lang;
import pl.ktv.merchantry.data.ModAttachments;
import pl.ktv.merchantry.economy.Economy;
import pl.ktv.merchantry.repair.Repair;

// Płatność za oferty: w $ gdy waluta włączona, w przeciwnym razie w przedmiotach
public final class Payment {
    private Payment() {
    }

    // Cena oferty dla gracza przy danej ilości; null = oferta nie ma ceny w obecnym trybie
    public static Price price(ServerPlayer player, ShopOffer offer, int quantity) {
        if (offer.type == OfferType.REPAIR || offer.type == OfferType.REPAIR_ALL) {
            // Naprawy tylko za walutę, cena zależy od zużycia przedmiotów
            return Economy.isEnabled() ? Price.ofMoney(Repair.totalCost(player, offer.type == OfferType.REPAIR_ALL)) : null;
        }
        double multiplier = priceMultiplier(player, offer);
        if (Economy.isEnabled()) {
            return offer.moneyPrice == null ? null
                    : Price.ofMoney(Math.round(offer.moneyPrice * multiplier) * quantity);
        }
        return offer.itemPrice == null ? null
                : Price.ofItems(offer.itemPrice.item(), (long) Math.ceil(offer.itemPrice.count() * multiplier) * quantity);
    }

    public static Component describe(ServerPlayer player, ShopOffer offer, int quantity) {
        Price price = price(player, offer, quantity);
        return price == null ? Lang.msg("shop.price.none") : price.describe();
    }

    public static boolean canAfford(ServerPlayer player, Price price) {
        if (price.money()) {
            return Economy.getBalance(player) >= price.amount();
        }
        return countItems(player, price) >= price.amount();
    }

    public static boolean charge(ServerPlayer player, Price price) {
        if (!canAfford(player, price)) {
            return false;
        }
        if (price.money()) {
            return Economy.withdraw(player, price.amount());
        }
        player.getInventory().clearOrCountMatchingItems(stack -> matches(stack, price), (int) price.amount(),
                player.inventoryMenu.getCraftSlots());
        player.inventoryMenu.broadcastChanges();
        return true;
    }

    // Każdy kolejny dom jest droższy: mnożnik ^ liczba już kupionych domów
    private static double priceMultiplier(ServerPlayer player, ShopOffer offer) {
        if (offer.type != OfferType.HOME_SLOT) {
            return 1.0;
        }
        return Math.pow(Config.HOME_PRICE_MULTIPLIER.get(), ModAttachments.get(player).extraHomes);
    }

    private static long countItems(ServerPlayer player, Price price) {
        return player.getInventory().clearOrCountMatchingItems(stack -> matches(stack, price), 0,
                player.inventoryMenu.getCraftSlots());
    }

    // Zwykły przedmiot bez własnej nazwy - żeby nie zabrać przypadkiem np. nazwanego przedmiotu
    private static boolean matches(ItemStack stack, Price price) {
        return stack.is(price.item()) && !Stacks.hasCustomName(stack);
    }
}
