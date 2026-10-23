package pl.ktv.merchantry.seller;

import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
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
        stack.set(DataComponents.LORE, new ItemLore(List.of(
                noItalic(Lang.msg("seller.item.lore.1")),
                noItalic(Lang.msg("seller.item.lore.2", Config.SELLER_TAX_PERCENT.get().intValue(),
                        Config.SELLER_DAILY_STACKS.get())),
                noItalic(Lang.msg("seller.item.lore.3", Config.SELLER_ENERGY_PER_STACK.get())),
                noItalic(Lang.msg("seller.item.lore.4"))
        )));
        return stack;
    }

    public static boolean isCrateItem(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        return stack.is(Items.CHEST) && data != null && data.copyTag().contains(TAG);
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
    public static void register(ServerLevel level, BlockPos pos, UUID owner) {
        SellerData data = SellerData.get(level.getServer());
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
                sell(data, entry.getValue(), chest, today);
            }
        }
    }

    // Jeden cykl sprzedaży jednej skrzynki
    private static void sell(SellerData data, SellerData.Crate crate, ChestBlockEntity chest, long today) {
        SellerData.Ledger ledger = data.ledger(crate.owner, today);

        // Ile stacków wolno jeszcze sprzedać w tym cyklu: limit cyklu, limit dzienny i energia
        double allowance = Math.min(Config.SELLER_STACKS_PER_CYCLE.get(),
                Config.SELLER_DAILY_STACKS.get() - ledger.stacksToday);
        int energyPerStack = Config.SELLER_ENERGY_PER_STACK.get();
        if (energyPerStack > 0) {
            allowance = Math.min(allowance, crate.energy / (double) energyPerStack);
        }
        if (allowance < 0.001) {
            return;
        }

        double keep = 1.0 - Config.SELLER_TAX_PERCENT.get() / 100.0;
        double soldStacks = 0;
        long earned = 0;
        for (int slot = 0; slot < chest.getContainerSize() && allowance - soldStacks > 0.001; slot++) {
            ItemStack stack = chest.getItem(slot);
            long price = SellManager.priceOf(stack);
            if (price <= 0) {
                continue;
            }
            int maxStack = stack.getMaxStackSize();
            // Tyle sztuk mieści się jeszcze w limicie (z drobną tolerancją na błędy zaokrągleń)
            int amount = (int) Math.min(stack.getCount(), Math.floor((allowance - soldStacks) * maxStack + 1e-6));
            if (amount <= 0) {
                continue;
            }
            chest.removeItem(slot, amount);
            earned += (long) Math.floor(amount * price * keep);
            soldStacks += amount / (double) maxStack;
        }
        if (soldStacks <= 0) {
            return;
        }

        ledger.stacksToday += soldStacks;
        ledger.earnings.merge(today, earned, Long::sum);
        crate.energy = Math.max(0, crate.energy - (long) Math.ceil(soldStacks * energyPerStack));
        data.setDirty();
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

    // Wystawia czek na wszystko, co dojrzało. Zwraca kwotę (0 = nic do odebrania).
    public static long collect(ServerPlayer player) {
        SellerData data = SellerData.get(player.server);
        SellerData.Ledger ledger = data.ledgers.get(player.getUUID());
        if (ledger == null) {
            return 0;
        }
        long amount = matured(ledger, LocalDate.now().toEpochDay(), true);
        if (amount <= 0) {
            return 0;
        }
        data.setDirty();
        ItemHandlerHelper.giveItemToPlayer(player, Cheque.create(player, amount));
        Merchantry.LOGGER.info("{} odebrał czek ze skrzynek sprzedających na {}", player.getGameProfile().getName(), amount);
        return amount;
    }

    // Podsumowanie dla /seller
    public static void sendStatus(ServerPlayer player) {
        SellerData data = SellerData.get(player.server);
        long today = LocalDate.now().toEpochDay();
        SellerData.Ledger ledger = data.ledger(player.getUUID(), today);

        long crates = 0;
        long energy = 0;
        for (SellerData.Crate crate : data.crates.values()) {
            if (crate.owner.equals(player.getUUID())) {
                crates++;
                energy += crate.energy;
            }
        }
        long ready = matured(ledger, today, false);
        long pending = 0;
        long soonest = Long.MAX_VALUE;
        long delay = Config.SELLER_PAYOUT_DELAY_DAYS.get();
        for (Map.Entry<Long, Long> earning : ledger.earnings.entrySet()) {
            if (earning.getKey() + delay > today) {
                pending += earning.getValue();
                soonest = Math.min(soonest, earning.getKey() + delay - today);
            }
        }

        player.sendSystemMessage(Lang.msg("seller.status.header"));
        if (!Economy.isEnabled() || !Config.ENABLE_SELLER.get()) {
            player.sendSystemMessage(Lang.msg("seller.disabled"));
        }
        player.sendSystemMessage(Lang.msg("seller.status.crates", crates, Config.SELLER_MAX_CRATES.get(),
                String.format(java.util.Locale.ROOT, "%,d", energy).replace(',', ' ')));
        player.sendSystemMessage(Lang.msg("seller.status.limit",
                String.format(java.util.Locale.ROOT, "%.1f", ledger.stacksToday), Config.SELLER_DAILY_STACKS.get()));
        player.sendSystemMessage(Lang.msg("seller.status.ready", Economy.format(ready)));
        if (pending > 0) {
            player.sendSystemMessage(Lang.msg("seller.status.pending", Economy.format(pending), soonest));
        }
    }
}
