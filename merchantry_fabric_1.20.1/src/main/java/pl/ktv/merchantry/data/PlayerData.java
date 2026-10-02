package pl.ktv.merchantry.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.nbt.CompoundTag;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

// Dane gracza zapisywane razem z nim (saldo, domy, odblokowania, postęp zarabiania, keepInventory)
public class PlayerData {
    private static final Codec<Set<String>> STRING_SET = Codec.STRING.listOf()
            .xmap(list -> new HashSet<>(list), set -> new ArrayList<>(set));

    public static final Codec<PlayerData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.BOOL.optionalFieldOf("initialized", false).forGetter(d -> d.initialized),
            Codec.LONG.optionalFieldOf("balance", 0L).forGetter(d -> d.balance),
            Codec.INT.optionalFieldOf("extra_homes", 0).forGetter(d -> d.extraHomes),
            Codec.unboundedMap(Codec.STRING, Home.CODEC).optionalFieldOf("homes", Map.of()).forGetter(d -> d.homes),
            STRING_SET.optionalFieldOf("unlocks", Set.of()).forGetter(d -> d.unlocks),
            STRING_SET.optionalFieldOf("discovered_biomes", Set.of()).forGetter(d -> d.discoveredBiomes),
            STRING_SET.optionalFieldOf("discovered_dimensions", Set.of()).forGetter(d -> d.discoveredDimensions),
            STRING_SET.optionalFieldOf("rewarded_advancements", Set.of()).forGetter(d -> d.rewardedAdvancements),
            Codec.INT.optionalFieldOf("active_ticks", 0).forGetter(d -> d.activeTicks),
            Codec.LONG.optionalFieldOf("playtime_day", 0L).forGetter(d -> d.playtimeDay),
            Codec.LONG.optionalFieldOf("playtime_earned_today", 0L).forGetter(d -> d.playtimeEarnedToday),
            Codec.INT.optionalFieldOf("keep_inventory_charges", 0).forGetter(d -> d.keepInventoryCharges),
            CompoundTag.CODEC.optionalFieldOf("pending_restore").forGetter(d -> Optional.ofNullable(d.pendingRestore)),
            Codec.BOOL.optionalFieldOf("sidebar_hidden", false).forGetter(d -> d.sidebarHidden)
    ).apply(instance, PlayerData::new));

    public boolean initialized;
    public long balance;
    public int extraHomes;
    public final Map<String, Home> homes;
    public final Set<String> unlocks;
    public final Set<String> discoveredBiomes;
    public final Set<String> discoveredDimensions;
    public final Set<String> rewardedAdvancements;
    public int activeTicks;
    public long playtimeDay;
    public long playtimeEarnedToday;
    public int keepInventoryCharges;
    // Ekwipunek i XP zapamiętane przy śmierci, oddawane po odrodzeniu (null = brak)
    public CompoundTag pendingRestore;
    // Gracz ukrył panel boczny komendą /sidebar
    public boolean sidebarHidden;

    public PlayerData() {
        this(false, 0L, 0, Map.of(), Set.of(), Set.of(), Set.of(), Set.of(), 0, 0L, 0L, 0, Optional.empty(), false);
    }

    private PlayerData(boolean initialized, long balance, int extraHomes, Map<String, Home> homes, Set<String> unlocks,
                       Set<String> discoveredBiomes, Set<String> discoveredDimensions, Set<String> rewardedAdvancements,
                       int activeTicks, long playtimeDay, long playtimeEarnedToday, int keepInventoryCharges,
                       Optional<CompoundTag> pendingRestore, boolean sidebarHidden) {
        this.initialized = initialized;
        this.balance = balance;
        this.extraHomes = extraHomes;
        this.homes = new LinkedHashMap<>(homes);
        this.unlocks = new HashSet<>(unlocks);
        this.discoveredBiomes = new HashSet<>(discoveredBiomes);
        this.discoveredDimensions = new HashSet<>(discoveredDimensions);
        this.rewardedAdvancements = new HashSet<>(rewardedAdvancements);
        this.activeTicks = activeTicks;
        this.playtimeDay = playtimeDay;
        this.playtimeEarnedToday = playtimeEarnedToday;
        this.keepInventoryCharges = keepInventoryCharges;
        this.pendingRestore = pendingRestore.orElse(null);
        this.sidebarHidden = sidebarHidden;
    }
}
