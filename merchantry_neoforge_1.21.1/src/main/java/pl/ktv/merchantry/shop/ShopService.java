package pl.ktv.merchantry.shop;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import pl.ktv.merchantry.Merchantry;
import pl.ktv.merchantry.Config;
import pl.ktv.merchantry.Lang;
import pl.ktv.merchantry.command.HomeCommands;
import pl.ktv.merchantry.data.ModAttachments;
import pl.ktv.merchantry.repair.Repair;
import pl.ktv.merchantry.unlock.Unlock;

// Logika zakupu oferty
public final class ShopService {
    private ShopService() {
    }

    // Stan oferty z punktu widzenia gracza - do opisu w oknie i sprawdzenia przed zakupem
    public enum Status {
        AVAILABLE, CANT_AFFORD, OWNED, MAX_REACHED, NOTHING_TO_REPAIR, UNAVAILABLE
    }

    // Ile razy można kupić ofertę w jednej transakcji - wybór ilości mają przedmioty i ładunki keepInventory
    public static int maxQuantity(ShopOffer offer) {
        return offer.type == OfferType.ITEM || offer.type == OfferType.KEEP_INVENTORY
                ? Config.MAX_PURCHASE_QUANTITY.get() : 1;
    }

    public static Status status(ServerPlayer player, ShopOffer offer, int quantity) {
        Price price = Payment.price(player, offer, quantity);
        if (price == null || !isValid(offer)) {
            return Status.UNAVAILABLE;
        }
        switch (offer.type) {
            case UNLOCK -> {
                if (ModAttachments.get(player).unlocks.contains(offer.unlock)) {
                    return Status.OWNED;
                }
            }
            case HOME_SLOT -> {
                if (HomeCommands.limit(player) >= Config.MAX_HOMES.get()) {
                    return Status.MAX_REACHED;
                }
            }
            case REPAIR, REPAIR_ALL -> {
                if (price.amount() <= 0) {
                    return Status.NOTHING_TO_REPAIR;
                }
            }
            case KEEP_INVENTORY -> {
                int max = Config.MAX_KEEP_INVENTORY_CHARGES.get();
                if (max > 0 && ModAttachments.get(player).keepInventoryCharges + quantity > max) {
                    return Status.MAX_REACHED;
                }
            }
            default -> {
            }
        }
        return Payment.canAfford(player, price) ? Status.AVAILABLE : Status.CANT_AFFORD;
    }

    public static boolean purchase(ServerPlayer player, ShopOffer offer, int quantity) {
        quantity = Math.max(1, Math.min(quantity, maxQuantity(offer)));
        Price price = Payment.price(player, offer, quantity);
        String error = switch (status(player, offer, quantity)) {
            case UNAVAILABLE -> "shop.not_available";
            case OWNED -> "shop.already_owned";
            case MAX_REACHED -> offer.type == OfferType.KEEP_INVENTORY ? "shop.max_keepinv" : "shop.max_homes";
            case NOTHING_TO_REPAIR -> "shop.nothing_to_repair";
            case CANT_AFFORD -> "shop.cant_afford";
            case AVAILABLE -> null;
        };
        if (error != null) {
            player.sendSystemMessage(Lang.msg(error, price != null ? price.describe() : ""));
            return false;
        }
        if (!Payment.charge(player, price)) {
            return false;
        }
        deliver(player, offer, quantity);

        Component name = quantity > 1
                ? Component.literal(quantity + "x ").append(offer.displayName())
                : offer.displayName();
        player.sendSystemMessage(Lang.msg("shop.bought", name, price.describe()));
        player.playNotifySound(SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 1.0F, 1.0F);
        Merchantry.LOGGER.info("{} kupił ofertę {} (x{}) za {}", player.getGameProfile().getName(), offer.id,
                quantity, price.describe().getString());
        return true;
    }

    private static boolean isValid(ShopOffer offer) {
        return switch (offer.type) {
            case ITEM -> !offer.item.isEmpty();
            case UNLOCK -> Unlock.byId(offer.unlock) != null;
            case HOME_SLOT, REPAIR, REPAIR_ALL, KEEP_INVENTORY -> true;
            case COMMAND -> offer.command != null && !offer.command.isBlank();
        };
    }

    private static void deliver(ServerPlayer player, ShopOffer offer, int quantity) {
        switch (offer.type) {
            case ITEM -> {
                for (int i = 0; i < quantity; i++) {
                    ItemHandlerHelper.giveItemToPlayer(player, offer.item.copy());
                }
            }
            case UNLOCK -> ModAttachments.get(player).unlocks.add(offer.unlock);
            case HOME_SLOT -> ModAttachments.get(player).extraHomes++;
            case KEEP_INVENTORY -> ModAttachments.get(player).keepInventoryCharges += quantity;
            case COMMAND -> {
                // Komendę wykonuje serwer z uprawnieniami admina - tylko operatorzy mogą ją ustawić
                String command = offer.command
                        .replace("%player%", player.getGameProfile().getName())
                        .replace("%uuid%", player.getStringUUID());
                player.server.getCommands().performPrefixedCommand(
                        player.server.createCommandSourceStack().withSuppressedOutput().withPermission(4), command);
            }
            case REPAIR, REPAIR_ALL -> {
                int repaired = Repair.repair(player, offer.type == OfferType.REPAIR_ALL);
                player.sendSystemMessage(Lang.msg("repair.done", repaired));
                player.playNotifySound(SoundEvents.ANVIL_USE, SoundSource.PLAYERS, 0.5F, 1.0F);
            }
        }
    }
}
