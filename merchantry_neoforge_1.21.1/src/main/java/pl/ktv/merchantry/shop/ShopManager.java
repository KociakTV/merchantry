package pl.ktv.merchantry.shop;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.fml.loading.FMLPaths;
import pl.ktv.merchantry.Merchantry;
import pl.ktv.merchantry.Lang;
import pl.ktv.merchantry.unlock.Unlock;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

// Lista ofert sklepu zapisywana w config/merchantry_offers.json
public final class ShopManager {
    private static final String FILE_NAME = Merchantry.MOD_ID + "_offers.json";
    private static final Codec<List<ShopOffer>> LIST_CODEC = ShopOffer.CODEC.listOf();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static final List<ShopOffer> OFFERS = new ArrayList<>();
    private static MinecraftServer server;

    private ShopManager() {
    }

    public static List<ShopOffer> offers() {
        return OFFERS;
    }

    public static ShopOffer find(String id) {
        for (ShopOffer offer : OFFERS) {
            if (offer.id.equals(id)) {
                return offer;
            }
        }
        return null;
    }

    public static void load(MinecraftServer minecraftServer) {
        server = minecraftServer;
        OFFERS.clear();
        Path file = file();
        if (!Files.exists(file)) {
            // Starsze wersje trzymały oferty w folderze świata - przenosimy je do config/
            Path legacy = server.getWorldPath(LevelResource.ROOT).resolve("serverconfig").resolve(FILE_NAME);
            if (Files.exists(legacy)) {
                try {
                    Files.createDirectories(file.getParent());
                    Files.copy(legacy, file);
                    Merchantry.LOGGER.info("Przeniesiono oferty sklepu z {} do {}", legacy, file);
                } catch (Exception e) {
                    Merchantry.LOGGER.error("Nie udało się przenieść ofert z {}", legacy, e);
                }
            }
        }
        if (!Files.exists(file)) {
            OFFERS.addAll(defaults());
            save();
            return;
        }
        try {
            JsonElement json = JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8));
            LIST_CODEC.parse(ops(), json)
                    .resultOrPartial(error -> Merchantry.LOGGER.error("Błąd w pliku ofert: {}", error))
                    .ifPresent(OFFERS::addAll);
            if (migrateDefaultTexts()) {
                save();
            }
        } catch (Exception e) {
            Merchantry.LOGGER.error("Nie udało się wczytać ofert sklepu z {}", file, e);
        }
    }

    public static void save() {
        if (server == null) {
            return;
        }
        Path file = file();
        LIST_CODEC.encodeStart(ops(), OFFERS)
                .resultOrPartial(error -> Merchantry.LOGGER.error("Nie udało się zapisać ofert: {}", error))
                .ifPresent(json -> {
                    try {
                        Files.createDirectories(file.getParent());
                        Files.writeString(file, GSON.toJson(json), StandardCharsets.UTF_8);
                    } catch (Exception e) {
                        Merchantry.LOGGER.error("Nie udało się zapisać ofert sklepu do {}", file, e);
                    }
                });
    }

    public static void unload() {
        OFFERS.clear();
        server = null;
    }

    // Wspólny plik w config/ - obok merchantry-server.toml, jeden dla wszystkich światów
    private static Path file() {
        return FMLPaths.CONFIGDIR.get().resolve(FILE_NAME);
    }

    private static RegistryOps<JsonElement> ops() {
        return RegistryOps.create(JsonOps.INSTANCE, server.registryAccess());
    }

    // Oferty tworzone przy pierwszym uruchomieniu - admin może je zmienić przez /shopconfig
    private static List<ShopOffer> defaults() {
        List<ShopOffer> list = new ArrayList<>();
        list.add(unlock(Unlock.CRAFT, 500, 4));
        list.add(unlock(Unlock.STONECUTTER, 500, 4));
        list.add(unlock(Unlock.LOOM, 250, 2));
        list.add(unlock(Unlock.CARTOGRAPHY, 250, 2));
        list.add(unlock(Unlock.GRINDSTONE, 750, 6));
        list.add(unlock(Unlock.SMITHING, 1500, 10));
        list.add(unlock(Unlock.ANVIL, 2500, 16));
        list.add(unlock(Unlock.ENDERCHEST, 4000, 24));

        ShopOffer home = new ShopOffer("home_slot", OfferType.HOME_SLOT);
        home.name = "@default.home_slot";
        home.description.add("@default.home_slot.desc");
        home.moneyPrice = 1000L;
        home.itemPrice = new ItemPrice(Items.DIAMOND, 8);
        list.add(home);

        list.add(repair(OfferType.REPAIR));
        list.add(repair(OfferType.REPAIR_ALL));
        list.add(keepInventory("keep_inventory"));

        ShopOffer bread = new ShopOffer("bread", OfferType.ITEM);
        bread.item = new ItemStack(Items.BREAD, 16);
        bread.moneyPrice = 50L;
        bread.itemPrice = new ItemPrice(Items.IRON_INGOT, 4);
        list.add(bread);
        return list;
    }

    // Starsze wersje zapisywały domyślne nazwy/opisy jako gotowy tekst (po polsku lub angielsku).
    // Zamieniamy je na klucze "@default...", żeby każdy gracz widział je w swoim języku. Własne teksty zostają.
    private static boolean migrateDefaultTexts() {
        boolean changed = false;
        for (ShopOffer offer : OFFERS) {
            String nameKey = defaultKeyFor(offer.name, offer);
            if (nameKey != null) {
                offer.name = nameKey;
                changed = true;
            }
            for (int i = 0; i < offer.description.size(); i++) {
                String key = defaultKeyFor(offer.description.get(i), offer);
                if (key != null) {
                    offer.description.set(i, key);
                    changed = true;
                }
            }
        }
        return changed;
    }

    private static String defaultKeyFor(String text, ShopOffer offer) {
        if (text == null || text.isBlank() || text.startsWith("@")) {
            return null;
        }
        for (String language : new String[]{Lang.ENGLISH, Lang.POLISH}) {
            if (offer.unlock != null && text.equals(Lang.strIn(language, "default.unlock.desc", offer.unlock))) {
                return "@default.unlock.desc:" + offer.unlock;
            }
            for (var entry : Lang.entries(language).entrySet()) {
                if (entry.getKey().startsWith("default.") && entry.getValue().equals(text)) {
                    return "@" + entry.getKey();
                }
            }
        }
        return null;
    }

    // Nowa oferta z edytora: domyślna nazwa i opis, bez cen (poza keepInventory). carried = produkt dla przedmiotu.
    public static ShopOffer create(OfferType type, ItemStack carried) {
        ShopOffer offer = switch (type) {
            case ITEM -> {
                ShopOffer o = new ShopOffer("", type);
                o.item = carried.copy();
                yield o;
            }
            case UNLOCK -> {
                Unlock unlock = Unlock.CRAFT;
                for (Unlock candidate : Unlock.values()) {
                    if (OFFERS.stream().noneMatch(o -> candidate.id().equals(o.unlock))) {
                        unlock = candidate;
                        break;
                    }
                }
                ShopOffer o = new ShopOffer("", type);
                o.unlock = unlock.id();
                o.name = "@default.unlock." + unlock.id();
                o.description.add("@default.unlock.desc:" + unlock.id());
                yield o;
            }
            case HOME_SLOT -> {
                ShopOffer o = new ShopOffer("", type);
                o.name = "@default.home_slot";
                o.description.add("@default.home_slot.desc");
                yield o;
            }
            case REPAIR, REPAIR_ALL -> repair(type);
            case KEEP_INVENTORY -> keepInventory("");
            case COMMAND -> new ShopOffer("", type);
        };
        String base = type == OfferType.ITEM && !carried.isEmpty()
                ? net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(carried.getItem()).getPath()
                : type == OfferType.UNLOCK ? offer.unlock : type.getSerializedName();
        offer.id = uniqueId(base);
        return offer;
    }

    // Identyfikator bez kolizji: base, base_2, base_3...
    public static String uniqueId(String base) {
        String id = base;
        for (int i = 2; find(id) != null; i++) {
            id = base + "_" + i;
        }
        return id;
    }

    // Oferta naprawy dla /repair i /repairall: ta ze sklepu (z nazwą ustawioną przez admina) albo domyślna
    public static ShopOffer repairOffer(OfferType type) {
        for (ShopOffer offer : OFFERS) {
            if (offer.type == type) {
                return offer;
            }
        }
        return repair(type);
    }

    public static ShopOffer repair(OfferType type) {
        boolean all = type == OfferType.REPAIR_ALL;
        ShopOffer offer = new ShopOffer(all ? "repair_all" : "repair", type);
        offer.name = (all ? "@default.repair_all" : "@default.repair");
        offer.description.add((all ? "@default.repair_all.desc" : "@default.repair.desc"));
        return offer;
    }

    public static ShopOffer keepInventory(String id) {
        ShopOffer offer = new ShopOffer(id, OfferType.KEEP_INVENTORY);
        offer.name = "@default.keep_inventory";
        offer.description.add("@default.keep_inventory.desc");
        offer.moneyPrice = 300L;
        offer.itemPrice = new ItemPrice(Items.DIAMOND, 3);
        return offer;
    }

    private static ShopOffer unlock(Unlock unlock, long money, int diamonds) {
        ShopOffer offer = new ShopOffer(unlock.id(), OfferType.UNLOCK);
        offer.unlock = unlock.id();
        offer.name = "@default.unlock." + unlock.id();
        offer.description.add("@default.unlock.desc:" + unlock.id());
        offer.moneyPrice = money;
        offer.itemPrice = new ItemPrice(Items.DIAMOND, diamonds);
        return offer;
    }
}
