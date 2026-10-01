package pl.ktv.merchantry.shop.sell;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.fml.loading.FMLPaths;
import pl.ktv.merchantry.Merchantry;
import pl.ktv.merchantry.Lang;
import pl.ktv.merchantry.economy.Economy;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

// Skup przedmiotów przez serwer (zakładka "Sprzedaj"). Lista w config/merchantry_sell.json.
public final class SellManager {
    private static final Codec<List<SellOffer>> LIST_CODEC = SellOffer.CODEC.listOf();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static final List<SellOffer> OFFERS = new ArrayList<>();

    private SellManager() {
    }

    public static List<SellOffer> offers() {
        return OFFERS;
    }

    public static SellOffer find(Item item) {
        for (SellOffer offer : OFFERS) {
            if (offer.item() == item) {
                return offer;
            }
        }
        return null;
    }

    // Dodaje albo zmienia cenę skupu przedmiotu
    public static void set(Item item, long price) {
        SellOffer existing = find(item);
        SellOffer updated = new SellOffer(item, price);
        if (existing != null) {
            OFFERS.set(OFFERS.indexOf(existing), updated);
        } else {
            OFFERS.add(updated);
        }
        save();
    }

    public static boolean remove(Item item) {
        boolean removed = OFFERS.removeIf(offer -> offer.item() == item);
        if (removed) {
            save();
        }
        return removed;
    }

    public static void load() {
        OFFERS.clear();
        Path file = file();
        if (!Files.exists(file)) {
            OFFERS.addAll(defaults());
            save();
            return;
        }
        try {
            JsonElement json = JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8));
            LIST_CODEC.parse(JsonOps.INSTANCE, json)
                    .resultOrPartial(error -> Merchantry.LOGGER.error("Błąd w pliku skupu: {}", error))
                    .ifPresent(OFFERS::addAll);
        } catch (Exception e) {
            Merchantry.LOGGER.error("Nie udało się wczytać skupu z {}", file, e);
        }
    }

    public static void save() {
        Path file = file();
        LIST_CODEC.encodeStart(JsonOps.INSTANCE, OFFERS)
                .resultOrPartial(error -> Merchantry.LOGGER.error("Nie udało się zapisać skupu: {}", error))
                .ifPresent(json -> {
                    try {
                        Files.createDirectories(file.getParent());
                        Files.writeString(file, GSON.toJson(json), StandardCharsets.UTF_8);
                    } catch (Exception e) {
                        Merchantry.LOGGER.error("Nie udało się zapisać skupu do {}", file, e);
                    }
                });
    }

    private static Path file() {
        return FMLPaths.CONFIGDIR.get().resolve(Merchantry.MOD_ID + "_sell.json");
    }

    // Proste surowce na start - admin zmieni ceny przez /shopconfig sell
    private static List<SellOffer> defaults() {
        return new ArrayList<>(List.of(
                new SellOffer(Items.COAL, 2),
                new SellOffer(Items.COPPER_INGOT, 2),
                new SellOffer(Items.IRON_INGOT, 5),
                new SellOffer(Items.GOLD_INGOT, 8),
                new SellOffer(Items.REDSTONE, 1),
                new SellOffer(Items.LAPIS_LAZULI, 2),
                new SellOffer(Items.QUARTZ, 2),
                new SellOffer(Items.EMERALD, 10),
                new SellOffer(Items.DIAMOND, 20),
                new SellOffer(Items.NETHERITE_INGOT, 300),
                new SellOffer(Items.LEATHER, 2),
                new SellOffer(Items.ENDER_PEARL, 5),
                new SellOffer(Items.BLAZE_ROD, 6),
                new SellOffer(Items.WHEAT, 1)
        ));
    }

    // Ile sztuk gracz może sprzedać (zwykłe, nieuszkodzone, bez własnej nazwy i zaklęć)
    public static int count(ServerPlayer player, Item item) {
        return player.getInventory().clearOrCountMatchingItems(stack -> sellable(stack, item), 0,
                player.inventoryMenu.getCraftSlots());
    }

    // Sprzedaje amount sztuk (-1 = wszystkie). Zwraca true, jeśli coś sprzedano.
    public static boolean sell(ServerPlayer player, SellOffer offer, int amount) {
        if (!Economy.isEnabled()) {
            player.sendSystemMessage(Lang.msg("currency.disabled"));
            return false;
        }
        int have = count(player, offer.item());
        int toSell = amount < 0 ? have : Math.min(amount, have);
        if (toSell <= 0) {
            player.sendSystemMessage(Lang.msg("sell.none", offer.item().getDescription()));
            return false;
        }
        int removed = player.getInventory().clearOrCountMatchingItems(stack -> sellable(stack, offer.item()), toSell,
                player.inventoryMenu.getCraftSlots());
        player.inventoryMenu.broadcastChanges();
        long earned = removed * offer.price();
        Economy.deposit(player, earned);
        player.sendSystemMessage(Lang.msg("sell.done", removed, offer.item().getDescription(), Economy.format(earned)));
        Merchantry.LOGGER.info("{} sprzedał serwerowi {}x {} za {}", player.getGameProfile().getName(), removed,
                offer.item(), earned);
        return true;
    }

    private static boolean sellable(ItemStack stack, Item item) {
        return stack.is(item) && !stack.has(DataComponents.CUSTOM_NAME) && !stack.isEnchanted()
                && (!stack.isDamageableItem() || !stack.isDamaged());
    }
}
