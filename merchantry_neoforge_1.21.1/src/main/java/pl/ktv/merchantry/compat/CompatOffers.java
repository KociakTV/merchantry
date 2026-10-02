package pl.ktv.merchantry.compat;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.neoforged.fml.loading.FMLPaths;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import pl.ktv.merchantry.Merchantry;
import pl.ktv.merchantry.shop.ItemPrice;
import pl.ktv.merchantry.shop.OfferType;
import pl.ktv.merchantry.shop.ShopManager;
import pl.ktv.merchantry.shop.ShopOffer;
import pl.ktv.merchantry.shop.sell.SellManager;
import pl.ktv.merchantry.unlock.Unlock;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

// Domyślne oferty dla modów (kupno w sklepie i skup przez serwer). Paczka moda jest dodawana raz -
// przy pierwszym starcie serwera z tym modem, także do istniejących plików ofert. Dodane paczki są
// zapisane w config/merchantry_compat_offers.json, więc oferty usunięte przez admina nie wracają.
public final class CompatOffers {
    private static final String FILE_NAME = Merchantry.MOD_ID + "_compat_offers.json";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    // Kupno: ilość w ofercie, cena w $ i cena w przedmiotach (gdy waluta wyłączona)
    private record Buy(String item, int count, long money, Item priceItem, int priceCount) {
    }

    // Skup: serwer płaci price $ za sztukę
    private record Sell(String item, long price) {
    }

    private record Pack(String id, Compat mod, List<Buy> buy, List<Sell> sell, List<Unlock> unlocks) {
    }

    private static final List<Pack> PACKS = List.of(
            new Pack("create", Compat.CREATE, List.of(
                    new Buy("minecraft:andesite", 64, 40, Items.IRON_INGOT, 2),
                    new Buy("create:andesite_alloy", 16, 60, Items.IRON_INGOT, 3),
                    new Buy("create:zinc_ingot", 16, 100, Items.IRON_INGOT, 5),
                    new Buy("minecraft:copper_ingot", 16, 50, Items.IRON_INGOT, 3),
                    new Buy("create:brass_ingot", 16, 200, Items.DIAMOND, 2)
            ), List.of(
                    new Sell("create:andesite_alloy", 2),
                    new Sell("create:zinc_ingot", 4),
                    new Sell("create:brass_ingot", 8),
                    new Sell("create:polished_rose_quartz", 6)
            ), List.of()),
            new Pack("createaddition", Compat.CREATE_ADDITIONS, List.of(
                    new Buy("createaddition:electrum_ingot", 8, 120, Items.DIAMOND, 1)
            ), List.of(
                    new Sell("createaddition:electrum_ingot", 10)
            ), List.of()),
            new Pack("create_new_age", Compat.CREATE_NEW_AGE, List.of(
                    new Buy("create_new_age:magnetite_block", 16, 120, Items.DIAMOND, 1)
            ), List.of(
                    new Sell("create_new_age:magnetite_block", 5)
            ), List.of()),
            new Pack("create_more_additions", Compat.CREATE_MORE_ADDITIONS, List.of(
                    new Buy("create_more_additions:silver_ingot", 8, 100, Items.DIAMOND, 1)
            ), List.of(
                    new Sell("create_more_additions:silver_ingot", 8),
                    new Sell("create_more_additions:raw_silver", 4)
            ), List.of()),
            new Pack("ae2", Compat.AE2, List.of(
                    new Buy("ae2:certus_quartz_crystal", 16, 80, Items.IRON_INGOT, 4),
                    new Buy("ae2:charged_certus_quartz_crystal", 8, 80, Items.IRON_INGOT, 4),
                    new Buy("ae2:fluix_crystal", 16, 150, Items.DIAMOND, 2),
                    new Buy("ae2:silicon", 16, 60, Items.IRON_INGOT, 3),
                    new Buy("ae2:sky_stone_block", 16, 60, Items.IRON_INGOT, 3),
                    new Buy("ae2:blank_pattern", 16, 400, Items.DIAMOND, 4),
                    // Mysterious Cube - po rozbiciu daje prasy do Inscribera (zwykle trzeba szukać meteorytów)
                    new Buy("ae2:mysterious_cube", 1, 2500, Items.DIAMOND, 16)
            ), List.of(
                    new Sell("ae2:certus_quartz_crystal", 3),
                    new Sell("ae2:charged_certus_quartz_crystal", 5),
                    new Sell("ae2:fluix_crystal", 6),
                    new Sell("ae2:silicon", 2),
                    new Sell("ae2:sky_stone_block", 2)
            ), List.of()),
            new Pack("ftbultimine", Compat.FTB_ULTIMINE, List.of(), List.of(), List.of(Unlock.ULTIMINE))
    );

    private CompatOffers() {
    }

    // Wywoływane po wczytaniu ofert sklepu i skupu
    public static void apply() {
        Set<String> applied = loadApplied();
        boolean shopChanged = false;
        boolean sellChanged = false;
        boolean stateChanged = false;
        for (Pack pack : PACKS) {
            if (!pack.mod().isLoaded() || applied.contains(pack.id())) {
                continue;
            }
            for (Buy buy : pack.buy()) {
                shopChanged |= addBuy(pack, buy);
            }
            for (Sell sell : pack.sell()) {
                Item item = item(sell.item());
                if (item != null && SellManager.find(item) == null) {
                    SellManager.offers().add(new pl.ktv.merchantry.shop.sell.SellOffer(item, sell.price()));
                    sellChanged = true;
                }
            }
            for (Unlock unlock : pack.unlocks()) {
                shopChanged |= addUnlock(unlock);
            }
            applied.add(pack.id());
            stateChanged = true;
            Merchantry.LOGGER.info("Dodano domyślne oferty dla {}", pack.mod().modId());
        }
        if (shopChanged) {
            ShopManager.save();
        }
        if (sellChanged) {
            SellManager.save();
        }
        if (stateChanged) {
            saveApplied(applied);
        }
    }

    private static boolean addBuy(Pack pack, Buy buy) {
        Item item = item(buy.item());
        if (item == null) {
            return false;
        }
        String id = pack.id() + "_" + BuiltInRegistries.ITEM.getKey(item).getPath();
        if (ShopManager.find(id) != null) {
            return false;
        }
        ShopOffer offer = new ShopOffer(id, OfferType.ITEM);
        offer.item = new ItemStack(item, buy.count());
        offer.moneyPrice = buy.money();
        offer.itemPrice = new ItemPrice(buy.priceItem(), buy.priceCount());
        ShopManager.offers().add(offer);
        return true;
    }

    private static boolean addUnlock(Unlock unlock) {
        if (ShopManager.find(unlock.id()) != null) {
            return false;
        }
        ShopOffer offer = new ShopOffer(unlock.id(), OfferType.UNLOCK);
        offer.unlock = unlock.id();
        offer.name = "@default.unlock." + unlock.id();
        offer.description.add(unlock.defaultDescription());
        offer.moneyPrice = 5000L;
        offer.itemPrice = new ItemPrice(Items.DIAMOND, 32);
        ShopManager.offers().add(offer);
        return true;
    }

    // Przedmiot po ID; null, gdy go nie ma (np. inna wersja moda)
    private static Item item(String id) {
        Item item = BuiltInRegistries.ITEM.get(ResourceLocation.parse(id));
        if (item == Items.AIR) {
            Merchantry.LOGGER.warn("Brak przedmiotu {} - pomijam domyślną ofertę", id);
            return null;
        }
        return item;
    }

    private static Path file() {
        return FMLPaths.CONFIGDIR.get().resolve(FILE_NAME);
    }

    private static Set<String> loadApplied() {
        Set<String> applied = new LinkedHashSet<>();
        Path file = file();
        if (!Files.exists(file)) {
            return applied;
        }
        try {
            JsonObject json = JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8)).getAsJsonObject();
            for (JsonElement element : json.getAsJsonArray("applied")) {
                applied.add(element.getAsString());
            }
        } catch (Exception e) {
            Merchantry.LOGGER.error("Nie udało się wczytać {}", file, e);
        }
        return applied;
    }

    private static void saveApplied(Set<String> applied) {
        JsonObject json = new JsonObject();
        JsonArray array = new JsonArray();
        applied.forEach(array::add);
        json.add("applied", array);
        try {
            Files.createDirectories(file().getParent());
            Files.writeString(file(), GSON.toJson(json), StandardCharsets.UTF_8);
        } catch (Exception e) {
            Merchantry.LOGGER.error("Nie udało się zapisać {}", file(), e);
        }
    }
}
