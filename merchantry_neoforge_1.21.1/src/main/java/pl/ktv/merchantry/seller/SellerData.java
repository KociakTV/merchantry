package pl.ktv.merchantry.seller;

import net.minecraft.core.GlobalPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

// Dane skrzynek sprzedających zapisywane w świecie (data/merchantry_sellers.dat):
// lista postawionych skrzynek (z energią) oraz rozliczenia właścicieli (dzienny limit i zarobek czekający na odbiór).
// Pieniądze trzymamy przy właścicielu, a nie w skrzynce, żeby zniszczenie skrzynki ich nie kasowało.
public class SellerData extends SavedData {
    public static final String NAME = "merchantry_sellers";
    public static final SavedData.Factory<SellerData> FACTORY = new SavedData.Factory<>(SellerData::new, SellerData::load, null);

    // Jedna postawiona skrzynka sprzedająca
    public static final class Crate {
        public final UUID owner;
        // Zgromadzona energia (FE) - zużywana przy sprzedaży
        public long energy;

        public Crate(UUID owner, long energy) {
            this.owner = owner;
            this.energy = energy;
        }
    }

    // Rozliczenie jednego gracza (wspólne dla wszystkich jego skrzynek)
    public static final class Ledger {
        // Dzień (liczony od 1970, czas serwera), którego dotyczy licznik sprzedaży
        public long day;
        // Ile sprzedaży (opróżnień skrzynki) wykonano w tym dniu
        public int sellsToday;
        // Dodatkowe dzienne sprzedaże dokupione w sklepie (na stałe, ponad limit z konfiguracji)
        public int extraSells;
        // Zarobek według dnia sprzedaży: dzień -> kwota. Dojrzewa po kilku dniach (patrz SellerService.collect)
        public final TreeMap<Long, Long> earnings = new TreeMap<>();
    }

    public final Map<GlobalPos, Crate> crates = new HashMap<>();
    public final Map<UUID, Ledger> ledgers = new HashMap<>();

    public static SellerData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(FACTORY, NAME);
    }

    // Rozliczenie gracza z wyzerowanym licznikiem, jeśli zaczął się nowy dzień
    public Ledger ledger(UUID owner, long today) {
        Ledger ledger = ledgers.computeIfAbsent(owner, id -> new Ledger());
        if (ledger.day != today) {
            ledger.day = today;
            ledger.sellsToday = 0;
        }
        return ledger;
    }

    public static SellerData load(CompoundTag tag, HolderLookup.Provider registries) {
        SellerData data = new SellerData();
        for (Tag entry : tag.getList("crates", Tag.TAG_COMPOUND)) {
            CompoundTag crate = (CompoundTag) entry;
            ResourceKey<Level> dimension = ResourceKey.create(Registries.DIMENSION,
                    ResourceLocation.parse(crate.getString("dim")));
            GlobalPos pos = GlobalPos.of(dimension,
                    new net.minecraft.core.BlockPos(crate.getInt("x"), crate.getInt("y"), crate.getInt("z")));
            data.crates.put(pos, new Crate(crate.getUUID("owner"), crate.getLong("energy")));
        }
        for (Tag entry : tag.getList("ledgers", Tag.TAG_COMPOUND)) {
            CompoundTag saved = (CompoundTag) entry;
            Ledger ledger = new Ledger();
            ledger.day = saved.getLong("day");
            ledger.sellsToday = saved.getInt("sells_today");
            ledger.extraSells = saved.getInt("extra_sells");
            for (Tag earning : saved.getList("earnings", Tag.TAG_COMPOUND)) {
                CompoundTag line = (CompoundTag) earning;
                ledger.earnings.put(line.getLong("day"), line.getLong("amount"));
            }
            data.ledgers.put(saved.getUUID("owner"), ledger);
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag crateList = new ListTag();
        for (Map.Entry<GlobalPos, Crate> entry : crates.entrySet()) {
            CompoundTag crate = new CompoundTag();
            crate.putString("dim", entry.getKey().dimension().location().toString());
            crate.putInt("x", entry.getKey().pos().getX());
            crate.putInt("y", entry.getKey().pos().getY());
            crate.putInt("z", entry.getKey().pos().getZ());
            crate.putUUID("owner", entry.getValue().owner);
            crate.putLong("energy", entry.getValue().energy);
            crateList.add(crate);
        }
        tag.put("crates", crateList);

        ListTag ledgerList = new ListTag();
        for (Map.Entry<UUID, Ledger> entry : ledgers.entrySet()) {
            CompoundTag saved = new CompoundTag();
            saved.putUUID("owner", entry.getKey());
            saved.putLong("day", entry.getValue().day);
            saved.putInt("sells_today", entry.getValue().sellsToday);
            saved.putInt("extra_sells", entry.getValue().extraSells);
            ListTag earnings = new ListTag();
            for (Map.Entry<Long, Long> earning : entry.getValue().earnings.entrySet()) {
                CompoundTag line = new CompoundTag();
                line.putLong("day", earning.getKey());
                line.putLong("amount", earning.getValue());
                earnings.add(line);
            }
            saved.put("earnings", earnings);
            ledgerList.add(saved);
        }
        tag.put("ledgers", ledgerList);
        return tag;
    }
}
