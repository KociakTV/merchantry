package pl.ktv.merchantry.seller;

import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.ServerScoreboard;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.ScoreHolder;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import pl.ktv.merchantry.Config;
import pl.ktv.merchantry.Lang;
import pl.ktv.merchantry.Merchantry;
import pl.ktv.merchantry.economy.Cheque;
import pl.ktv.merchantry.economy.Economy;
import pl.ktv.merchantry.shop.sell.SellManager;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

// Logika skrzynek sprzedających. Skrzynka to zwykły przedmiot "minecraft:chest" ze znacznikiem w custom_data,
// więc mod nadal działa tylko po stronie serwera (gracze nie potrzebują żadnego moda).
// Co kilka sekund serwer sprzedaje zawartość każdej postawionej skrzynki po cenach skupu (z podatkiem),
// w granicach dziennego limitu i dostępnego prądu. Zarobek trafia do rozliczenia właściciela
// i po kilku dniach można go odebrać czekiem.
public final class SellerService {
    // Nazwa znacznika w custom_data przedmiotu
    private static final String TAG = "merchantry_seller";

    private SellerService() {
    }

    // ---------------------------------------------------------------- przedmiot

    // Nowa skrzynka sprzedająca (tekst w języku bieżącego odbiorcy - patrz Lang.setContext)
    public static ItemStack createItem() {
        CompoundTag root = new CompoundTag();
        root.putBoolean(TAG, true);

        ItemStack stack = new ItemStack(Items.CHEST);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(root));
        stack.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
        stack.set(DataComponents.CUSTOM_NAME, noItalic(Lang.msg("seller.item.name")));
        int energy = Config.SELLER_ENERGY_PER_SELL.get();
        stack.set(DataComponents.LORE, new ItemLore(List.of(
                noItalic(Lang.msg("seller.item.lore.1")),
                noItalic(Lang.msg("seller.item.lore.2", Config.SELLER_TAX_PERCENT.get().intValue(),
                        dailySellsConfigured())),
                noItalic(energy > 0 ? Lang.msg("seller.item.lore.3", energy) : Lang.msg("seller.item.lore.3.free")),
                noItalic(Lang.msg("seller.item.lore.4"))
        )));
        return stack;
    }

    public static boolean isCrateItem(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        return stack.is(Items.CHEST) && data != null && data.copyTag().contains(TAG);
    }

    // Limit z konfiguracji: 0 oznacza "tyle, co maksymalna liczba skrzynek" (podąża za nią automatycznie)
    public static int dailySellsConfigured() {
        int configured = Config.SELLER_DAILY_SELLS.get();
        return configured > 0 ? configured : Config.SELLER_MAX_CRATES.get();
    }

    // Ile sprzedaży dziennie ma ten gracz: limit z konfiguracji + dokupione w sklepie
    public static int dailySellLimit(SellerData.Ledger ledger) {
        return dailySellsConfigured() + ledger.extraSells;
    }

    private static Component noItalic(Component component) {
        return component.copy().withStyle(style -> style.withItalic(false));
    }

    // ---------------------------------------------------------------- rejestr skrzynek

    public static GlobalPos key(ServerLevel level, BlockPos pos) {
        return GlobalPos.of(level.dimension(), pos.immutable());
    }

    public static long countCrates(SellerData data, UUID owner) {
        return data.crates.values().stream().filter(crate -> crate.owner.equals(owner)).count();
    }

    // Zapisuje postawioną skrzynkę jako sprzedającą
    public static void register(ServerLevel level, BlockPos pos, UUID owner, String ownerName) {
        SellerData data = SellerData.get(level.getServer());
        data.ledger(owner, LocalDate.now().toEpochDay()).name = ownerName;
        data.crates.put(key(level, pos), new SellerData.Crate(owner, 0));
        data.setDirty();
        // Kable innych modów mogły zapamiętać "brak zasilania" - odświeżamy ich pamięć podręczną
        level.invalidateCapabilities(pos);
    }

    public static boolean unregister(ServerLevel level, BlockPos pos) {
        SellerData data = SellerData.get(level.getServer());
        boolean removed = data.crates.remove(key(level, pos)) != null;
        if (removed) {
            data.setDirty();
            level.invalidateCapabilities(pos);
        }
        return removed;
    }

    // ---------------------------------------------------------------- sprzedaż

    // Wywoływane co kilka sekund dla całego serwera
    public static void tick(MinecraftServer server) {
        if (!Config.ENABLE_SELLER.get() || !Economy.isEnabled()) {
            return;
        }
        SellerData data = SellerData.get(server);
        if (data.crates.isEmpty()) {
            return;
        }
        long today = LocalDate.now().toEpochDay();
        Iterator<Map.Entry<GlobalPos, SellerData.Crate>> iterator = data.crates.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<GlobalPos, SellerData.Crate> entry = iterator.next();
            ServerLevel level = server.getLevel(entry.getKey().dimension());
            BlockPos pos = entry.getKey().pos();
            // Skrzynki w niezaładowanych fragmentach świata czekają
            if (level == null || !level.isLoaded(pos)) {
                continue;
            }
            // Skrzynka zniknęła (np. komenda /setblock) - sprzątamy wpis
            if (!level.getBlockState(pos).is(Blocks.CHEST)) {
                iterator.remove();
                data.setDirty();
                level.invalidateCapabilities(pos);
                continue;
            }
            if (level.getBlockEntity(pos) instanceof ChestBlockEntity chest) {
                sell(server, data, entry.getValue(), chest, today);
            }
        }
    }

    // Jedno sprawdzenie skrzynki: jeśli może, sprzedaje CAŁĄ zawartość naraz (to jedna "sprzedaż")
    private static void sell(MinecraftServer server, SellerData data, SellerData.Crate crate,
                             ChestBlockEntity chest, long today) {
        SellerData.Ledger ledger = data.ledger(crate.owner, today);
        if (ledger.sellsToday >= dailySellLimit(ledger)) {
            return;
        }
        int energyPerSell = Config.SELLER_ENERGY_PER_SELL.get();
        if (energyPerSell > 0 && crate.energy < energyPerSell) {
            return;
        }
        // Domyślnie czekamy, aż skrzynka się zapełni, żeby nie marnować dziennych sprzedaży na drobne partie
        if (Config.SELLER_ONLY_WHEN_FULL.get()) {
            for (int slot = 0; slot < chest.getContainerSize(); slot++) {
                if (chest.getItem(slot).isEmpty()) {
                    return;
                }
            }
        }

        // Najpierw liczymy, ile dostaniemy, i zapamiętujemy sloty do opróżnienia
        double keep = 1.0 - Config.SELLER_TAX_PERCENT.get() / 100.0;
        long earned = 0;
        List<Integer> slots = new ArrayList<>();
        for (int slot = 0; slot < chest.getContainerSize(); slot++) {
            ItemStack stack = chest.getItem(slot);
            long price = SellManager.priceOf(stack);
            if (price > 0) {
                earned += (long) Math.floor(stack.getCount() * price * keep);
                slots.add(slot);
            }
        }
        // Nic do sprzedania (albo zarobek zaokrągla się do zera) - nie zużywamy sprzedaży ani prądu
        if (slots.isEmpty() || earned <= 0) {
            return;
        }

        for (int slot : slots) {
            chest.setItem(slot, ItemStack.EMPTY);
        }
        chest.setChanged();
        ledger.sellsToday++;
        ledger.earnings.merge(today, earned, Long::sum);
        ledger.totalEarned += earned;
        crate.energy = Math.max(0, crate.energy - energyPerSell);
        data.setDirty();
        syncEarnedScore(server, ledger);
    }

    // Łączny zarobek gracza jako wynik w scoreboardzie (dla modów z tablicami wyników).
    // Wpisujemy po nicku, więc działa też, gdy właściciel jest offline.
    public static void syncEarnedScore(MinecraftServer server, SellerData.Ledger ledger) {
        String name = Config.SELLER_EARNED_OBJECTIVE.get();
        if (name.isBlank() || ledger.name.isBlank()) {
            return;
        }
        ServerScoreboard scoreboard = server.getScoreboard();
        Objective objective = scoreboard.getObjective(name);
        if (objective == null) {
            objective = scoreboard.addObjective(name, ObjectiveCriteria.DUMMY,
                    Component.literal(Lang.strIn(Lang.defaultLanguage(), "scoreboard.seller_earned")),
                    ObjectiveCriteria.RenderType.INTEGER, true, null);
        }
        scoreboard.getOrCreatePlayerScore(ScoreHolder.forNameOnly(ledger.name), objective)
                .set((int) Math.min(ledger.totalEarned, Integer.MAX_VALUE));
    }

    // Po starcie serwera odświeża wyniki wszystkich graczy (np. gdy cel scoreboardu dopiero włączono)
    public static void syncAllEarnedScores(MinecraftServer server) {
        SellerData data = SellerData.get(server);
        for (SellerData.Ledger ledger : data.ledgers.values()) {
            syncEarnedScore(server, ledger);
        }
    }

    // ---------------------------------------------------------------- administracja

    // Zeruje dzienny licznik sprzedaży gracza (dokupione sprzedaże zostają)
    public static void resetDailyLimit(MinecraftServer server, UUID owner) {
        SellerData data = SellerData.get(server);
        data.ledger(owner, LocalDate.now().toEpochDay()).sellsToday = 0;
        data.setDirty();
    }

    // Skraca oczekiwanie na wypłatę: cały czekający zarobek staje się od razu gotowy do odbioru
    public static long resetPayoutCooldown(MinecraftServer server, UUID owner) {
        SellerData data = SellerData.get(server);
        SellerData.Ledger ledger = data.ledgers.get(owner);
        if (ledger == null || ledger.earnings.isEmpty()) {
            return 0;
        }
        long sum = ledger.earnings.values().stream().mapToLong(Long::longValue).sum();
        ledger.earnings.clear();
        ledger.earnings.put(LocalDate.now().toEpochDay() - Config.SELLER_PAYOUT_DELAY_DAYS.get(), sum);
        data.setDirty();
        return sum;
    }

    // Dodaje graczowi dodatkowe dzienne sprzedaże (na stałe); zwraca nowy łączny limit dzienny
    public static int addExtraSells(MinecraftServer server, UUID owner, int amount) {
        SellerData data = SellerData.get(server);
        SellerData.Ledger ledger = data.ledger(owner, LocalDate.now().toEpochDay());
        ledger.extraSells += amount;
        data.setDirty();
        return dailySellLimit(ledger);
    }

    // ---------------------------------------------------------------- zarobek

    // Suma zarobków, które już dojrzały (można je odebrać); jeśli remove, usuwa je z rozliczenia
    private static long matured(SellerData.Ledger ledger, long today, boolean remove) {
        long delay = Config.SELLER_PAYOUT_DELAY_DAYS.get();
        long sum = 0;
        Iterator<Map.Entry<Long, Long>> iterator = ledger.earnings.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Long, Long> earning = iterator.next();
            if (earning.getKey() + delay <= today) {
                sum += earning.getValue();
                if (remove) {
                    iterator.remove();
                }
            }
        }
        return sum;
    }

    // Ile zarobku jest teraz gotowe do odbioru
    public static long readyAmount(ServerPlayer player) {
        SellerData.Ledger ledger = SellerData.get(player.server).ledgers.get(player.getUUID());
        return ledger == null ? 0 : matured(ledger, LocalDate.now().toEpochDay(), false);
    }

    // Wystawia czek na podaną kwotę (requested <= 0 = wszystko, co dojrzało). Reszta zostaje w rozliczeniu.
    // Zwraca wypłaconą kwotę albo 0, gdy nie ma czego wypłacić albo prosimy o więcej, niż jest gotowe.
    public static long collect(ServerPlayer player, long requested) {
        SellerData data = SellerData.get(player.server);
        SellerData.Ledger ledger = data.ledgers.get(player.getUUID());
        if (ledger == null) {
            return 0;
        }
        long today = LocalDate.now().toEpochDay();
        long ready = matured(ledger, today, false);
        long amount = requested > 0 ? requested : ready;
        if (amount <= 0 || amount > ready) {
            return 0;
        }

        // Pobieramy od najstarszych dni; ostatni dzień może zostać rozliczony tylko częściowo
        long delay = Config.SELLER_PAYOUT_DELAY_DAYS.get();
        long left = amount;
        Iterator<Map.Entry<Long, Long>> iterator = ledger.earnings.entrySet().iterator();
        while (iterator.hasNext() && left > 0) {
            Map.Entry<Long, Long> earning = iterator.next();
            if (earning.getKey() + delay > today) {
                continue;
            }
            long take = Math.min(left, earning.getValue());
            left -= take;
            if (take == earning.getValue()) {
                iterator.remove();
            } else {
                earning.setValue(earning.getValue() - take);
            }
        }
        data.setDirty();
        ledger.name = player.getGameProfile().getName();
        ItemHandlerHelper.giveItemToPlayer(player, Cheque.create(player, amount));
        Merchantry.LOGGER.info("{} odebrał czek ze skrzynek sprzedających na {}", player.getGameProfile().getName(), amount);
        return amount;
    }

    // Podsumowanie dla /seller
    public static void sendStatus(ServerPlayer player) {
        SellerData data = SellerData.get(player.server);
        long today = LocalDate.now().toEpochDay();
        SellerData.Ledger ledger = data.ledger(player.getUUID(), today);
        ledger.name = player.getGameProfile().getName();

        long crates = 0;
        long energy = 0;
        for (SellerData.Crate crate : data.crates.values()) {
            if (crate.owner.equals(player.getUUID())) {
                crates++;
                energy += crate.energy;
            }
        }
        long delay = Config.SELLER_PAYOUT_DELAY_DAYS.get();
        long ready = matured(ledger, today, false);
        long pending = 0;
        for (Map.Entry<Long, Long> earning : ledger.earnings.entrySet()) {
            if (earning.getKey() + delay > today) {
                pending += earning.getValue();
            }
        }

        player.sendSystemMessage(Lang.msg("seller.status.header"));
        if (!Economy.isEnabled() || !Config.ENABLE_SELLER.get()) {
            player.sendSystemMessage(Lang.msg("seller.disabled"));
        }
        player.sendSystemMessage(Lang.msg("seller.status.crates", crates, Config.SELLER_MAX_CRATES.get(),
                String.format(java.util.Locale.ROOT, "%,d", energy).replace(',', ' ')));
        player.sendSystemMessage(Lang.msg("seller.status.limit", ledger.sellsToday, dailySellLimit(ledger)));
        if (ledger.extraSells > 0) {
            player.sendSystemMessage(Lang.msg("seller.status.extra", ledger.extraSells));
        }
        // Gotowe do odbioru teraz, a potem to, co dojrzewa (z podziałem na dni, w których będzie gotowe)
        player.sendSystemMessage(Lang.msg("seller.status.ready", Economy.format(ready)));
        if (pending > 0) {
            player.sendSystemMessage(Lang.msg("seller.status.pending", Economy.format(pending), delay));
            for (Map.Entry<Long, Long> earning : ledger.earnings.entrySet()) {
                long daysLeft = earning.getKey() + delay - today;
                if (daysLeft > 0) {
                    player.sendSystemMessage(Lang.msg("seller.status.pending.line", daysLeft,
                            Economy.format(earning.getValue())));
                }
            }
        }
        player.sendSystemMessage(Lang.msg("seller.status.total", Economy.format(ledger.totalEarned)));
    }
}
