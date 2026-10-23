package pl.ktv.merchantry.seller;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.BlockDropsEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import pl.ktv.merchantry.Config;
import pl.ktv.merchantry.Lang;
import pl.ktv.merchantry.Merchantry;

// Zdarzenia gry związane ze skrzynkami sprzedającymi: postawienie, zniszczenie i cykliczna sprzedaż
@EventBusSubscriber(modid = Merchantry.MOD_ID)
public final class SellerEvents {
    private SellerEvents() {
    }

    // Postawienie skrzynki. NeoForge przywraca przedmiot z jego danymi na czas tego zdarzenia,
    // więc w ręce gracza nadal widać znacznik skrzynki sprzedającej.
    @SubscribeEvent
    public static void onPlace(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level) || !event.getPlacedBlock().is(Blocks.CHEST)) {
            return;
        }
        ServerPlayer player = event.getEntity() instanceof ServerPlayer p ? p : null;
        boolean crateItem = player != null
                && (SellerService.isCrateItem(player.getMainHandItem()) || SellerService.isCrateItem(player.getOffhandItem()));
        BlockPos pos = event.getPos();
        BlockState placed = event.getPlacedBlock();
        SellerData data = SellerData.get(level.getServer());

        if (crateItem) {
            Lang.setContext(player);
            if (!Config.ENABLE_SELLER.get()) {
                event.setCanceled(true);
                player.sendSystemMessage(Lang.msg("seller.disabled"));
                return;
            }
            if (SellerService.countCrates(data, player.getUUID()) >= Config.SELLER_MAX_CRATES.get()) {
                event.setCanceled(true);
                player.sendSystemMessage(Lang.msg("seller.limit_crates", Config.SELLER_MAX_CRATES.get()));
                return;
            }
        }

        // Skrzynka sprzedająca zawsze jest pojedyncza: jeśli stawiana obok innej skrzynki zrobiłaby się podwójna,
        // rozdzielamy je (jak przy stawianiu z Shiftem). Dotyczy też zwykłej skrzynki stawianej obok sprzedającej.
        if (placed.hasProperty(ChestBlock.TYPE) && placed.getValue(ChestBlock.TYPE) != ChestType.SINGLE) {
            BlockPos other = pos.relative(ChestBlock.getConnectedDirection(placed));
            if (crateItem || data.crates.containsKey(SellerService.key(level, other))) {
                level.setBlock(pos, placed.setValue(ChestBlock.TYPE, ChestType.SINGLE), Block.UPDATE_ALL);
                BlockState otherState = level.getBlockState(other);
                if (otherState.is(Blocks.CHEST) && otherState.getValue(ChestBlock.TYPE) != ChestType.SINGLE) {
                    level.setBlock(other, otherState.setValue(ChestBlock.TYPE, ChestType.SINGLE), Block.UPDATE_ALL);
                }
            }
        }
        if (!crateItem) {
            return;
        }

        SellerService.register(level, pos, player.getUUID(), player.getGameProfile().getName());
        player.sendSystemMessage(Lang.msg("seller.placed"));
        Merchantry.LOGGER.info("{} postawił skrzynkę sprzedającą w {} {}", player.getGameProfile().getName(),
                level.dimension().location(), pos.toShortString());
    }

    // Zniszczenie skrzynki: zamiast zwykłej skrzynki wypada skrzynka sprzedająca (z danymi),
    // żeby można ją było postawić gdzie indziej. Energia przepada, zarobek jest bezpieczny u właściciela.
    @SubscribeEvent
    public static void onDrops(BlockDropsEvent event) {
        if (!event.getState().is(Blocks.CHEST)) {
            return;
        }
        ServerLevel level = event.getLevel();
        if (!SellerService.unregister(level, event.getPos())) {
            return;
        }
        ServerPlayer breaker = event.getBreaker() instanceof ServerPlayer p ? p : null;
        for (ItemEntity drop : event.getDrops()) {
            ItemStack stack = drop.getItem();
            if (stack.is(Items.CHEST) && !SellerService.isCrateItem(stack)) {
                // Język nazwy i opisu: język gracza, który zniszczył skrzynkę (albo domyślny)
                drop.setItem(Lang.with(breaker, SellerService::createItem));
                break;
            }
        }
    }

    // Po starcie serwera odświeża wyniki zarobku w scoreboardzie
    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        SellerService.syncAllEarnedScores(event.getServer());
    }

    // Co kilka sekund sprzedaje zawartość wszystkich skrzynek
    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        int interval = Config.SELLER_INTERVAL_SECONDS.get() * 20;
        if (event.getServer().getTickCount() % interval == 0) {
            SellerService.tick(event.getServer());
        }
    }
}
