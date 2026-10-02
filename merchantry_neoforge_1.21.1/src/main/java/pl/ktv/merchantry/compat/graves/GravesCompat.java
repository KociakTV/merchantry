package pl.ktv.merchantry.compat.graves;

import com.b1n_ry.yigd.events.YigdEvents;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import pl.ktv.merchantry.Config;
import pl.ktv.merchantry.compat.Compat;
import pl.ktv.merchantry.keepinventory.KeepInventory;

// Mody grobów: gdy śmierć uratował ładunek keepInventory, grób nie powstaje (ekwipunek i tak jest pusty,
// a pusty grób tylko myli gracza).
// Gravestone i Corpse (biblioteka henkelmaxa) oraz wiele innych modów grobów tworzy grób w LivingDropsEvent
// i pomija anulowane zdarzenie - anulujemy je więc wcześniej. YIGD ma własne zdarzenie.
public final class GravesCompat {
    private GravesCompat() {
    }

    public static void register() {
        NeoForge.EVENT_BUS.addListener(EventPriority.HIGH, GravesCompat::onDrops);
        if (Compat.YIGD.isLoaded()) {
            Yigd.register();
        }
    }

    private static boolean blockGrave(ServerPlayer player) {
        return Config.BLOCK_GRAVES.get() && KeepInventory.isDeathSaved(player);
    }

    // Lista przedmiotów jest pusta (ekwipunek zapamiętał ładunek), więc anulowanie niczego nie zabiera graczowi
    private static void onDrops(LivingDropsEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && blockGrave(player)) {
            event.setCanceled(true);
        }
    }

    // Osobna klasa, żeby klasy YIGD nie były ładowane, gdy mod nie jest zainstalowany
    private static final class Yigd {
        static void register() {
            NeoForge.EVENT_BUS.addListener(Yigd::onAllowGrave);
        }

        private static void onAllowGrave(YigdEvents.AllowGraveGenerationEvent event) {
            if (blockGrave(event.getDeathContext().player())) {
                event.setAllowGeneration(false);
            }
        }
    }
}
